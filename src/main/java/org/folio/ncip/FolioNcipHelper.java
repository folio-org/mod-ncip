package org.folio.ncip;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extensiblecatalog.ncip.v2.common.MappedMessageHandler;
import org.extensiblecatalog.ncip.v2.common.MessageHandlerFactory;
import org.extensiblecatalog.ncip.v2.common.ServiceValidatorFactory;
import org.extensiblecatalog.ncip.v2.common.Translator;
import org.extensiblecatalog.ncip.v2.common.TranslatorFactory;
import org.extensiblecatalog.ncip.v2.service.BibliographicRecordIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.LocationType;
import org.extensiblecatalog.ncip.v2.service.NCIPInitiationData;
import org.extensiblecatalog.ncip.v2.service.NCIPResponseData;
import org.extensiblecatalog.ncip.v2.service.RequestScopeType;
import org.extensiblecatalog.ncip.v2.service.RequestType;
import org.extensiblecatalog.ncip.v2.service.SchemeValueBehavior;
import org.extensiblecatalog.ncip.v2.service.SchemeValuePair;
import org.folio.util.StringUtil;
import org.folio.util.PercentCodec;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class FolioNcipHelper {

	private static final Logger logger = LogManager.getLogger(FolioNcipHelper.class);
	
	// INSTANCES OF org.extensiblecatalog.ncip.v2.service.ServiceContext
	// serviceContext PER TENANT
	protected Properties serviceContext = new Properties();
	// INSTANCES OF org.extensiblecatalog.ncip.v2.common.Translator PER TENANT
	protected Properties translator = new Properties();
	// INSTANCES OF java.util.Properties.Properties PER TENANT
	protected Properties toolkitProperties = new Properties();
	// INSTANCE OF java.util.Properties.Properties PER TENANT
	protected Properties ncipProperties = new Properties();

	// USE THESE TOOLKIT PROPERTIES AS DEFAULTS:
	protected Properties defaultToolkitObjects = new Properties();

	public FolioNcipHelper(Promise<Void> promise) {
		setUpMapping();
		initToolkitDefaults().onComplete(promise);
	}

	/*
	 *
	 * When the module starts, default config. values for the NCIP toolkit are loaded. When
	 * ncip requests are made, the module will check for updated
	 * configurations using mod-configuration.
	 *
	 */
	private Future<Void> initToolkitDefaults() {

		try {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Constants.TOOLKIT_PROP_FILE);
			Properties properties = new Properties();
			logger.info("initializing the XC NCIP Toolkit default properties...");
			properties.load(inputStream);

			if (properties.isEmpty()) {
				return Future.failedFuture("Unable to initialize the default toolkit properties.");
			}

			defaultToolkitObjects.put("toolkit", properties);
			defaultToolkitObjects.put("servicecontext",
					ServiceValidatorFactory.buildServiceValidator(properties).getInitialServiceContext());
			defaultToolkitObjects.put("translator", TranslatorFactory.buildTranslator(null, properties));
			return Future.succeededFuture();
		} catch (Exception e) {
			logger.fatal(Constants.UNABLE_TO_INIT_TOOLKIT);
			logger.fatal(e.getLocalizedMessage());
			return Future.failedFuture(Constants.UNABLE_TO_INIT_TOOLKIT);
		}
	}

	private void setUpMapping(){
		SchemeValuePair.allowNullScheme(RequestType.class.getName(), RequestScopeType.class.getName(),
				BibliographicRecordIdentifierCode.class.getName(), LocationType.class.getName());
		SchemeValuePair.mapBehavior(RequestType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(RequestScopeType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(BibliographicRecordIdentifierCode.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(LocationType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
	}

	public InputStream ncipProcess(RoutingContext context) throws Exception {

		logger.info("ncip process called...");
		//logger.info("=====okapi headers================");
		//for (Map.Entry<String, String> entry : context.request().headers().entries()) {
		//	logger.info(entry.getKey() + "-" + entry.getValue());
		//}
		//logger.info("==============================");
		logger.info("==========BODY===============");
		logger.info(context.getBodyAsString());

		String tenant = context.request().headers().get(Constants.X_OKAPI_TENANT);


			// INITIALIZE THIS TENANTS TOOLKIT PROPERTIES WITH THE DEFAULT VALUES:
			toolkitProperties.put(tenant, defaultToolkitObjects.get("toolkit"));
			serviceContext.put(tenant, defaultToolkitObjects.get("servicecontext"));
			translator.put(tenant, defaultToolkitObjects.get("translator"));
			// HAVE THEY OVERWRITTEN ANY OF THESE VALUES IN MOD-CONFIGURATION?
			try {
				initToolkit(context);
			} catch (Exception e) {
				logger.info(e.getLocalizedMessage());
				logger.info("Unable to initialize custom toolkit properties.  Using default");
				logger.info(e.getLocalizedMessage());
			}

			try {
				initNcipProperties(context);
			} catch (Exception e) {
				logger.info("Unable to initialize NCIP properties with mod-configuration.");
				logger.info(e.getLocalizedMessage());
			}

		InputStream stream = new ByteArrayInputStream(context.getBodyAsString().getBytes(StandardCharsets.UTF_8));
		NCIPInitiationData initiationData = null;
		InputStream responseMsgInputStream = null;
		FolioRemoteServiceManager folioRemoteServiceManager = null;
		MappedMessageHandler messageHandler = null;
		try {
			// USE THIS TENANT'S TRANSLATOR TO CREATE THE OBJECTS WITH THE XML INPUT:
			initiationData = ((Translator) translator.get(tenant)).createInitiationData(
					(org.extensiblecatalog.ncip.v2.service.ServiceContext) serviceContext.get(tenant), stream);

			folioRemoteServiceManager = new FolioRemoteServiceManager();
			folioRemoteServiceManager.setOkapiHeaders(context.request().headers());
			Properties tenantProperties = (Properties) ncipProperties.get(tenant);
			printProperties(tenantProperties, tenant);
			folioRemoteServiceManager.setNcipProperties(tenantProperties);

			// MESSAGEHANDLER IS A DYNAMIC TYPE BASED ON INCOMING XML
			// E.G. INCOMING XML = LookupUserRequest, then messageHandler WILL BE
			// FolioLookupUserService
			// THIS IS CONFIGURABLE IN THE toolkit.properties file
			messageHandler = (MappedMessageHandler) MessageHandlerFactory
					.buildMessageHandler(toolkitProperties.getProperty(tenant));
			messageHandler.setRemoteServiceManager(folioRemoteServiceManager);
			NCIPResponseData responseData = messageHandler.performService(initiationData,
					(org.extensiblecatalog.ncip.v2.service.ServiceContext) serviceContext.get(tenant));
			responseMsgInputStream = ((Translator) translator.get(tenant)).createResponseMessageStream(
					(org.extensiblecatalog.ncip.v2.service.ServiceContext) serviceContext.get(tenant), responseData);
		} catch (Exception e) {
			logger.error(e.toString());
			throw e;
		}
		return responseMsgInputStream;
	}

	/**
	 * XC NCIP Toolkit properties initialized for each tenant using
	 * mod-configuration
	 *
	 * @throws Exception
	 *
	 */
	public void initToolkit(RoutingContext context) throws Exception {

		try {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Constants.TOOLKIT_PROP_FILE);
			// DO THE TOOLKIT PROPERTIES EXIST IN MOD-CONFIGURATION?
			String okapiBaseEndpoint = context.request().getHeader(Constants.X_OKAPI_URL);
			String tenant = context.request().getHeader(Constants.X_OKAPI_TENANT);
			String configEndpoint = okapiBaseEndpoint + "/configurations/entries" + "?query=" + PercentCodec.encode("configName=toolkit") + "&limit=200";
			// GET THE EXISTING PROPERTIES FOR THE TOOLKIT
			// AND JUST OVERWRITE ANY THAT HAVE BEEN SET IN MOD-CONFIGURATION
			Properties properties = (Properties) toolkitProperties.get(tenant);

			String response = callApiGet(configEndpoint, context.request().headers());
			JsonObject jsonObject = new JsonObject(response);
			JsonArray configs = jsonObject.getJsonArray(Constants.CONFIGS);
			if (configs.size() < 1) {
				logger.info("No toolkit configurations found.  Using defaults.  QUERY:" + configEndpoint);
				return;
			}

			Iterator configsIterator = configs.iterator();
			while (configsIterator.hasNext()) {
				JsonObject config = (JsonObject) configsIterator.next();
				String code = config.getString(Constants.CODE_KEY);
				String value = config.getString(Constants.VALUE_KEY);
				properties.setProperty(code, value);
			}
			toolkitProperties.put(tenant, properties);
			serviceContext.put(tenant,
					ServiceValidatorFactory.buildServiceValidator(properties).getInitialServiceContext());
			translator.put(tenant, TranslatorFactory.buildTranslator(null, properties));
		} catch (Exception e) {
			logger.fatal("Unable to initialize toolkit.properties file.");
			logger.fatal(e.getLocalizedMessage());
			throw new Exception("Unable to initialize toolkit properties using mod-configuration.  EXCEPTION: "
					+ e.getLocalizedMessage());
		}

	}

	/**
	 * Initializing property values needed for several of the services. e.g.
	 * materialTypes for instances created in AcceptItem These values are
	 * initialized for each tenant In the future, properties could be configured in
	 * settings? They don't typically change frequently...so maybe mod-configuration
	 * file is fine.
	 *
	 * @throws Exception
	 *
	 */
	public void initNcipProperties(RoutingContext context) throws Exception {

			String tenant = context.request().getHeader(Constants.X_OKAPI_TENANT);

			// THE NCIP PROPERTY FILE CONTAINS A LIST OF PROPERTIES
			// THAT NEED TO BE INITIALIZED
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Constants.NCIP_PROP_FILE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String okapiBaseEndpoint = context.request().getHeader(Constants.X_OKAPI_URL);
			String configEndpoint = okapiBaseEndpoint + "/configurations/entries?query=";

			Properties properties = new Properties();
			//DEFAULT VALUES
			properties.setProperty("holdings.source.name", "FOLIO");
			properties.setProperty("response.includes.physical.address","false");
			properties.setProperty("user.priv.ok.status","ACTIVE");
			properties.setProperty("user.priv.blocked.status","BLOCKED");
			// LOOK FOR EACH PROPERTY:
			while (reader.ready()) {
				String line = reader.readLine();
				logger.info(line);
				if (line.contains("#"))
					continue; // ignore comments in the file
				String[] parts = line.split("=");
				// IF THE CONFIG EXISTS IN MOD-CONFIGURATION, ADD TO PROPERTIES VAR
				// IF THE CONFIG DOES NOT EXIST IN MOD-CONFIGURATION,
				// THROW AN EXCEPTION.
				try {
					String configCode = parts[0];
					String query = "code==" + StringUtil.cqlEncode(configCode);
					String url = configEndpoint + PercentCodec.encode(query);
					String response = callApiGet(url + "&limit=200", context.request().headers());
					JsonObject jsonObject = new JsonObject(response);
					JsonArray configs = jsonObject.getJsonArray(Constants.CONFIGS);
					if (configs.size() < 1 && parts.length == 2) {
						//NO CONFIG SETTING - USE THE DEFAULT
						properties.setProperty(parts[0], parts[1]);
						continue;
					}
					if (configs.size() < 1) {
						throw new Exception("No ncip properties found in mod-configuration for property: " + line);
					}
					// SAVE EACH PROPERTY found
					// THERE COULD BE MULTIPLE - A DIFFERENT PROPERTY FOR
					// EACH AGENCY ID
					Iterator configsIterator = configs.iterator();
					while (configsIterator.hasNext()) {
						JsonObject config = (JsonObject) configsIterator.next();
						String code = config.getString(Constants.CODE_KEY);
						String configName = config.getString("configName");
						String value = config.getString(Constants.VALUE_KEY);
						// CONFIG NAME CONTAINS THE AGENCY ID FOR THIS VALUE
						// THERE COULD BE MULTIPLE VALUES FOR DIFFERENT AGENCY IDS
						properties.setProperty((configName + "." + code).toLowerCase(), value);
					}
				} catch (Exception e) {
					// UNABLE TO GET PROPERTY VALUE FROM MOD-CONFIGURATION
					logger.fatal("Unable to initialize ncip properties using mod-configuration.");
					logger.fatal(e.getLocalizedMessage());
					ncipProperties.remove(tenant);
					throw new Exception(
							"Unable to initialize NCIP properties using mod-configuration." + e.getLocalizedMessage());
				}
			}

			try {
				logger.info("=======> initializing address types");
				String addressTypesEndpoint =  okapiBaseEndpoint + Constants.ADDRESS_TYPES ;
				String addressTypesResponse = callApiGet(addressTypesEndpoint,context.request().headers());
				JsonObject addressesTypesAsJson = new JsonObject(addressTypesResponse);
				JsonArray addressTypeArray = addressesTypesAsJson.getJsonArray("addressTypes");
				Iterator addressTypeIterator = addressTypeArray.iterator();
				while (addressTypeIterator.hasNext()) {
					JsonObject addressType = (JsonObject) addressTypeIterator.next();
					properties.setProperty(addressType.getString("id"), addressType.getString("addressType"));
				}
			}
			catch(Exception e) {
				//THIS IS FINE...CAN CONTINUE
				logger.fatal("Unable to initialize address types.");
				logger.fatal(e.getLocalizedMessage());
			}

			printProperties(properties, tenant);

			ncipProperties.put(tenant, properties);

			logger.info("Has tenant properties " + (ncipProperties.get(tenant) != null));
	}

	private void printProperties(Properties properties, String tenant) {
		if(properties != null) {
			StringWriter writer = new StringWriter();
			properties.list(new PrintWriter(writer));
			logger.info("Tenant: " + tenant + " properties are " + writer.getBuffer().toString());
		} else {
			logger.info("Tenant: " + tenant + " does not have properties");
		}
	}


	public String getConfigValue(String code, MultiMap okapiHeaders) {
		String returnValue = null;
		String okapiBaseEndpoint = okapiHeaders.get(Constants.X_OKAPI_URL);
		//String configEndpoint = okapiBaseEndpoint + "/configurations/entries?query=(code==" + URLEncoder.encode(code) + ")";
		String configEndpoint = okapiBaseEndpoint + "/configurations/entries?query=";

		try {
			String query = "code==" + StringUtil.cqlEncode(code);
			String url = configEndpoint + PercentCodec.encode(query);
			String response = callApiGet(url, okapiHeaders);
			JsonObject jsonObject = new JsonObject(response);
			JsonArray configs = jsonObject.getJsonArray(Constants.CONFIGS);
			if (configs.size() < 1) {
				return null;
			}
			returnValue = configs.getJsonObject(0).getString(Constants.VALUE_KEY);
		} catch (Exception e) {
			logger.error("unable to get config value for code: " + code);
			logger.error(e.getMessage());
			return null;
		}

		return returnValue;
	}

	public String callApiGet(String uriString, MultiMap okapiHeaders)
			throws Exception  {
		CloseableHttpClient client = HttpClients.custom().build();
		final String timeoutString = System.getProperty(Constants.SERVICE_MGR_TIMEOUT,Constants.DEFAULT_TIMEOUT);
		int timeout = Integer.parseInt(timeoutString);
		logger.info("Using timeout: " + timeout);
		RequestConfig config = RequestConfig.custom()
			.setConnectTimeout(timeout)
			.setSocketTimeout(timeout)
			.build();
		HttpUriRequest request = RequestBuilder.get().setUri(uriString)
			.setConfig(config)
			.setHeader(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
			.setHeader(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN) // do i need version here?
			.setHeader(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
			.setHeader(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN)).build();
		String responseString = "";
		try {
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			responseString = EntityUtils.toString(entity, "UTF-8");
			int responseCode = response.getStatusLine().getStatusCode();

			logger.info("GET:");
			logger.info(uriString);
			logger.info(responseCode);
			//logger.info(responseString);

			if (responseCode > 399) {
				String responseBody = processErrorResponse(responseString);
				throw new Exception(responseBody);
			}
		}
		catch(Exception e) {
			logger.fatal("callApiGet failed");
			logger.fatal(uriString);
			throw e;
		}
		finally {
			client.close();
		}

		return responseString;

	}

	/**
	 * The method deals with error messages that are returned by the API as plain
	 * strings and messages returned as JSON
	 *
	 */
	public String processErrorResponse(String responseBody) {
		// SOMETIMES ERRORS ARE RETURNED BY THE API AS PLAIN STRINGS
		// SOMETIMES ERRORS ARE RETURNED BY THE API AS JSON
		StringBuffer responseBuffer = new StringBuffer();
		try {
			JsonObject jsonObject = new JsonObject(responseBody);
			JsonArray errors = jsonObject.getJsonArray("errors");
			Iterator i = errors.iterator();
			responseBuffer.append("ERROR: " );
			while (i.hasNext()) {
				JsonObject errorMessage = (JsonObject) i.next();
				responseBuffer.append(errorMessage.getString("message"));
			}
		} catch (Exception exception) {
			// NOT A PROBLEM, ERROR WAS A STRING. UNABLE TO PARSE
			// AS JSON
		}
		return responseBuffer.toString();
	}

}
