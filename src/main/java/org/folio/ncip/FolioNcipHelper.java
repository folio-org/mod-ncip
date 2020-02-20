package org.folio.ncip;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.common.MappedMessageHandler;
import org.extensiblecatalog.ncip.v2.common.MessageHandlerFactory;
import org.extensiblecatalog.ncip.v2.common.ServiceValidatorFactory;
import org.extensiblecatalog.ncip.v2.common.Translator;
import org.extensiblecatalog.ncip.v2.common.TranslatorFactory;
import org.extensiblecatalog.ncip.v2.service.NCIPInitiationData;
import org.extensiblecatalog.ncip.v2.service.NCIPResponseData;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.RoutingContext;



public class FolioNcipHelper {

	private static final Logger logger = Logger.getLogger(FolioNcipHelper.class);

	//INSTANCES OF org.extensiblecatalog.ncip.v2.service.ServiceContext serviceContext PER TENANT
	protected Properties serviceContext = new Properties();  
	//INSTANCES OF org.extensiblecatalog.ncip.v2.common.Translator PER TENANT
	protected Properties translator = new Properties();
	//INSTANCES OF java.util.Properties.Properties PER TENANT
	protected Properties toolkitProperties = new Properties();
	//INSTANCES OF org.kie.api.runtime.KieContainer PER TENANT
	protected Properties kieContainer = new Properties();
	//INSTANCE OF java.util.Properties.Properties PER TENANT
	protected Properties ncipProperties = new Properties();
	


	public FolioNcipHelper(Promise<Void> promise) {
		//INITIALIZE THE PROPERTIES
		//FOR EACH TENANT
		//initNcipProperties();  //E.G. INSTANCE TYPE
		//initToolkit();             //XC NCIP TOOLKIT PROPERTIES FILES
		//initRules();               //INIT DROOLS RULES FOR PATRON CHECK
		
		 Future<Void> steps = initToolkit().compose(v -> initRules().compose(x ->initNcipProperties()));
	     steps.setHandler(ar -> {
	    	if (ar.succeeded()) {
	    		promise.complete();
	    	}
	    	else {
	    		promise.fail(ar.cause());
	    	}
	    });
	}

	public InputStream ncipProcess(RoutingContext context) throws Exception {
		
		
		logger.info("ncip process called...");
		logger.info("=====okapi headers================");
		for (Map.Entry<String, String> entry : context.request().headers().entries()) {
			logger.info(entry.getKey() + "-" + entry.getValue());
		}
		logger.info("==============================");
		logger.info("==========BODY===============");
		logger.info(context.getBodyAsString());
		
		
		String tenant = context.request().headers().get(Constants.X_OKAPI_TENANT);
		InputStream stream = new ByteArrayInputStream(context.getBodyAsString().getBytes(StandardCharsets.UTF_8));
		NCIPInitiationData initiationData = null;
		InputStream responseMsgInputStream = null;
		FolioRemoteServiceManager folioRemoteServiceManager = null;
		MappedMessageHandler messageHandler = null;
		try {
			//USE THIS TENANT'S TRANSLATOR TO CREATE THE OBJECTS WITH THE XML INPUT:
			initiationData = ((Translator)translator.get(tenant)).createInitiationData((org.extensiblecatalog.ncip.v2.service.ServiceContext )serviceContext.get(tenant), stream);
			
			folioRemoteServiceManager = new FolioRemoteServiceManager();
			folioRemoteServiceManager.setKieContainer((KieContainer)kieContainer.get(tenant));
			folioRemoteServiceManager.setOkapiHeaders(context.request().headers());
			folioRemoteServiceManager.setNcipProperties((Properties)ncipProperties.get(tenant));
			
			//MESSAGEHANDLER IS A DYNAMIC TYPE BASED ON INCOMING XML
			//E.G. INCOMING XML = LookupUserRequest, then messageHandler WILL BE FolioLookupUserService
			//THIS IS CONFIGURABLE IN THE toolkit.properties file
			messageHandler = (MappedMessageHandler) MessageHandlerFactory.buildMessageHandler(toolkitProperties.getProperty(tenant));
			messageHandler.setRemoteServiceManager(folioRemoteServiceManager);
			NCIPResponseData responseData = messageHandler.performService(initiationData, (org.extensiblecatalog.ncip.v2.service.ServiceContext)serviceContext.get(tenant));
			responseMsgInputStream =  ((Translator)translator.get(tenant)).createResponseMessageStream((org.extensiblecatalog.ncip.v2.service.ServiceContext)serviceContext.get(tenant), responseData);
		}
		catch(Exception e) {
			logger.error(e.toString());
			throw e;
		}
		return responseMsgInputStream;
	}


	/**
	 * XC NCIP Toolkit properties file
	 * initialized for each tenant
	 *
	*/
	private Future<Void> initToolkit() {

		Promise<Void> promise = Promise.promise();
		try {
			//get property folder for each tenant
			logger.info("initializing toolkit.properties");
			final String propertyFolder = System.getProperty("prop_files");
			List<String> tenantPropertyFolders = findFoldersInDirectory(propertyFolder + "/tenants");
			Iterator<String> i = tenantPropertyFolders.iterator();
			//ServiceContext sContext = ServiceValidatorFactory.buildServiceValidator().getInitialServiceContext();
			while (i.hasNext()) {
				Properties properties = new Properties();
				String tenant = (String) i.next();
				logger.info("initializing toolkit.properties for tenant: " + tenant);
				String toolKitPropertyFileName = System.getProperty("prop_files") + "/tenants/" + tenant + "/toolkit.properties"; 
				InputStream input = new FileInputStream(toolKitPropertyFileName);
				properties.load(input);
				toolkitProperties.put(tenant, properties);
				serviceContext.put(tenant,ServiceValidatorFactory.buildServiceValidator(properties).getInitialServiceContext());
				translator.put(tenant,TranslatorFactory.buildTranslator(null,properties));
			}
			promise.complete();
		}
		catch(Exception e) {
			logger.fatal("Unable to initialize toolkit.properties file.");
			logger.fatal(e.getLocalizedMessage());
			promise.fail("Unable to initialize toolkit.properties file.");
		}
		return promise.future();
	}

	/**
	 * Initializing property values needed for several of the services.
	 * e.g. materialTypes for instances created in AcceptItem
	 * These values are initialized for each tenant
	 * In the future, properties could be configured in settings?
	 * They don't typically change frequently...so maybe the property 
	 * file is fine.
	 *
	*/
	private Future<Void> initNcipProperties() {
		Promise<Void> promise = Promise.promise();
		try {
			String propertyFolder = System.getProperty("prop_files");
			logger.info("initializing ncip.properties");
			List<String> tenantPropertyFolders = findFoldersInDirectory(propertyFolder + "/tenants");
			Iterator<String> i = tenantPropertyFolders.iterator();
			while (i.hasNext()) {
				Properties properties = new Properties();
				String tenant = (String) i.next();
				logger.info("initializing ncip.properties for tenant: " + tenant);
				String filePath = System.getProperty("prop_files") + "/tenants/" + tenant + "/" + "ncip.properties";

				InputStream input = new FileInputStream(filePath);
				properties.load(input);
				ncipProperties.put(tenant, properties);
			}
			promise.complete();
		}
		catch(Exception e) {
			logger.fatal("unable to initialize ncip.properties file");
			logger.fatal(e.getLocalizedMessage());
			promise.fail("unable to initialize ncip.properties file");
		}
		return promise.future();
	}

	//THESE RULES ARE USED TO DETERMINE IF A *PATRON* CAN BORROW
	//THESE DON'T HAVE TO BE USED
	//CODE ALSO CHECKS ACTIVE INDICATOR ON USER RECORD
	//INITIALIZED FOR EACH TENANT
	private Future<Void> initRules() {
		Promise<Void> promise = Promise.promise();
		try {

			//get property folder
			logger.info("initializing rules.drl");
			final String propertyFolder = System.getProperty("prop_files");
			List<String> tenantPropertyFolders = findFoldersInDirectory(propertyFolder + "/tenants");
			Iterator<String> i = tenantPropertyFolders.iterator();
			while (i.hasNext()) {

				String tenant = (String) i.next();
				logger.info("initializing rules for tenant: " + tenant);
				KieServices kieServices = KieServices.Factory.get();
				KieFileSystem kfs = kieServices.newKieFileSystem();
				String rulesFilePath = System.getProperty("prop_files") + "/tenants/" + tenant + "/rules.drl"; 

				File file = new File(rulesFilePath);
				org.kie.api.io.Resource resource = kieServices.getResources().newFileSystemResource(file).setResourceType(ResourceType.DRL);
				kfs.write(resource);

				KieBuilder Kiebuilder = kieServices.newKieBuilder(kfs);
				Kiebuilder.buildAll();
				kieContainer.put(tenant, kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId())) ;
			}
			promise.complete();
		}
		catch(Exception e) {
			logger.fatal("unable to initialize rules.drl");
			logger.fatal(e.getLocalizedMessage());
			promise.fail("unable to initialize rules.drl");
		}
		return promise.future();
	}


	public List<String> findFoldersInDirectory(String directoryPath) {
		File directory = new File(directoryPath);

		FileFilter directoryFileFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory();
			}
		};

		File[] directoryListAsFile = directory.listFiles(directoryFileFilter);
		List<String> foldersInDirectory = new ArrayList<String>(directoryListAsFile.length);
		for (File directoryAsFile : directoryListAsFile) {
			foldersInDirectory.add(directoryAsFile.getName());
		}

		return foldersInDirectory;
	}
	
	
	


}
