package org.folio.ncip;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.extensiblecatalog.ncip.v2.common.MappedMessageHandler;
import org.extensiblecatalog.ncip.v2.common.MessageHandlerFactory;
import org.extensiblecatalog.ncip.v2.common.ServiceValidatorFactory;
import org.extensiblecatalog.ncip.v2.common.Translator;
import org.extensiblecatalog.ncip.v2.common.TranslatorFactory;
import org.extensiblecatalog.ncip.v2.service.BibliographicRecordIdentifierCode;
import org.extensiblecatalog.ncip.v2.service.CurrencyCode;
import org.extensiblecatalog.ncip.v2.service.FiscalActionType;
import org.extensiblecatalog.ncip.v2.service.FiscalTransactionType;
import org.extensiblecatalog.ncip.v2.service.LocationType;
import org.extensiblecatalog.ncip.v2.service.NCIPInitiationData;
import org.extensiblecatalog.ncip.v2.service.NCIPResponseData;
import org.extensiblecatalog.ncip.v2.service.PickupLocation;
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
		// setUpMapping();
		initToolkitDefaults().onComplete(promise);
	}

	private void setUpMapping() {
		SchemeValuePair.allowNullScheme(RequestType.class.getName(), RequestScopeType.class.getName(),
				BibliographicRecordIdentifierCode.class.getName(), LocationType.class.getName(),
				PickupLocation.class.getName(),
				FiscalActionType.class.getName(), FiscalTransactionType.class.getName(), CurrencyCode.class.getName());
		SchemeValuePair.mapBehavior(RequestType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(RequestScopeType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(BibliographicRecordIdentifierCode.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(LocationType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(PickupLocation.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(FiscalActionType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(FiscalTransactionType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
		SchemeValuePair.mapBehavior(CurrencyCode.class.getName(), SchemeValueBehavior.ALLOW_ANY);
	}

	/*
	 *
	 * When the module starts, default config. values for the NCIP toolkit are
	 * loaded. When
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

	public InputStream ncipProcess(RoutingContext context) throws Exception {

		logger.info("ncip process called...");
		// logger.info("=====okapi headers================");
		// for (Map.Entry<String, String> entry : context.request().headers().entries())
		// {
		// logger.info(entry.getKey() + "-" + entry.getValue());
		// }
		// logger.info("==============================");
		String body = context.body().asString();
		logger.info("==========BODY===============");
		logger.info(body);

		String tenant = context.request().headers().get(Constants.X_OKAPI_TENANT);

		// INITIALIZE THIS TENANTS TOOLKIT PROPERTIES WITH THE DEFAULT VALUES:
		initializeTenantToolkitState(tenant);
		// HAVE THEY OVERWRITTEN ANY OF THESE VALUES IN MOD-SETTINGS?
		try {
			initToolkit(context);
		} catch (Exception e) {
			logger.info(e.getLocalizedMessage());
			logger.info("Unable to initialize custom toolkit properties.  Using default");
			logger.info(e.getLocalizedMessage());
		}
		rebuildTenantToolkitObjects(tenant);

		try {
			initNcipProperties(context);
		} catch (Exception e) {
			logger.error("Unable to initialize NCIP properties with mod-settings", e);
			throw e;
		}

		InputStream stream = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
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
			folioRemoteServiceManager.setNcipProperties((Properties) ncipProperties.get(tenant));

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

	protected void initializeTenantToolkitState(String tenant) throws Exception {
		Properties defaultToolkit = (Properties) defaultToolkitObjects.get("toolkit");
		Properties tenantToolkit = new Properties();
		if (defaultToolkit != null) {
			tenantToolkit.putAll(defaultToolkit);
		}

		toolkitProperties.put(tenant, tenantToolkit);
	}

	protected void rebuildTenantToolkitObjects(String tenant) throws Exception {
		Properties properties = (Properties) toolkitProperties.get(tenant);
		if (properties == null) {
			throw new Exception("Toolkit properties are missing for tenant " + tenant);
		}

		serviceContext.put(tenant,
				ServiceValidatorFactory.buildServiceValidator(properties).getInitialServiceContext());
		translator.put(tenant, TranslatorFactory.buildTranslator(null, properties));
	}

	/**
	 * XC NCIP Toolkit properties initialized for each tenant using mod-settings.
	 * Reads the single "toolkit" settings entry (scope=mod-ncip, key=toolkit)
	 * and applies each field in its value object as a toolkit property override.
	 *
	 * @throws Exception
	 *
	 */
	public void initToolkit(RoutingContext context) throws Exception {

		try {
			String okapiBaseEndpoint = context.request().getHeader(Constants.X_OKAPI_URL);
			String tenant = context.request().getHeader(Constants.X_OKAPI_TENANT);
			String settingsQuery = "scope==" + Constants.SETTING_SCOPE + " and key==toolkit";
			String settingsEndpoint = okapiBaseEndpoint + Constants.SETTINGS_URL + "?query="
					+ PercentCodec.encode(settingsQuery) + "&limit=1";

			Properties properties = (Properties) toolkitProperties.get(tenant);

			String response = callApiGet(settingsEndpoint, context.request().headers());
			JsonObject jsonObject = new JsonObject(response);
			JsonArray items = jsonObject.getJsonArray("items");

			if (items == null || items.isEmpty()) {
				logger.info("No toolkit settings found in mod-settings. Using defaults.");
				return;
			}

			JsonObject toolkitValueObj = items.getJsonObject(0).getJsonObject("value");
			if (toolkitValueObj == null) {
				logger.info("Toolkit settings entry has no value object. Using defaults.");
				return;
			}

			// Apply each field in the value object as a toolkit property override
			for (String key : toolkitValueObj.fieldNames()) {
				Object rawValue = toolkitValueObj.getValue(key);
				if (rawValue == null) {
					logger.warn("Toolkit setting '{}' has null value, skipping", key);
					continue;
				}
				String value = String.valueOf(rawValue);
				logger.info("Overriding toolkit property: {} = {}", key, value);
				properties.setProperty(key, value);
			}
		} catch (Exception e) {
			logger.fatal("Unable to initialize toolkit properties from mod-settings.");
			logger.fatal(e.getLocalizedMessage());
			throw new Exception("Unable to initialize toolkit properties using mod-settings. EXCEPTION: "
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
	/**
	 * Initializing property values needed for several of the services.
	 * Reads all agency settings from mod-settings (scope=mod-ncip, excluding
	 * the "toolkit" entry) in a single API call. Each setting entry represents
	 * one agency, with properties stored as agencyId.propertyCode.
	 *
	 * @throws Exception
	 *
	 */
	public void initNcipProperties(RoutingContext context) throws Exception {

		String tenant = context.request().getHeader(Constants.X_OKAPI_TENANT);
		String okapiBaseEndpoint = context.request().getHeader(Constants.X_OKAPI_URL);

		Properties properties = new Properties();

		// APPLY DEFAULTS FROM ncip.properties FOR ANY PROPERTY NOT SET IN MOD-SETTINGS
		try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Constants.NCIP_PROP_FILE);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("=", 2);
				if (parts.length == 2 && !parts[1].isBlank()) {
					properties.setProperty(parts[0].trim(), parts[1].trim());
				}
			}
		}

		// FETCH ALL AGENCY SETTINGS FROM MOD-SETTINGS IN ONE CALL
		try {
			String settingsQuery = "scope==" + Constants.SETTING_SCOPE;
			String settingsEndpoint = okapiBaseEndpoint + Constants.SETTINGS_URL + "?query="
					+ PercentCodec.encode(settingsQuery) + "&limit=1000";

			logger.info("Fetching NCIP agency settings from mod-settings: {}", settingsEndpoint);
			String response = callApiGet(settingsEndpoint, context.request().headers());
			JsonObject jsonObject = new JsonObject(response);
			JsonArray items = jsonObject.getJsonArray("items");

			if (items == null || items.isEmpty()) {
				logger.error("No NCIP agency settings found in mod-settings. QUERY: {}", settingsEndpoint);
				ncipProperties.remove(tenant);
				throw new Exception("No NCIP agency settings found in mod-settings");
			}

			// EACH ITEM IS ONE AGENCY - APPLY ITS PROPERTIES AS agencyId.propertyCode
			for (int i = 0; i < items.size(); i++) {
				JsonObject item = items.getJsonObject(i);
				String key = item.getString(Constants.KEY);
				if ("toolkit".equalsIgnoreCase(key)) {
					continue;
				}

				if (key == null || key.isBlank()) {
					logger.warn("Skipping settings entry with blank key");
					continue;
				}

				String agencyId = key.toLowerCase();
				JsonObject agencyValues = item.getJsonObject("value");

				if (agencyValues == null) {
					logger.warn("Agency settings entry '{}' has no value object, skipping", agencyId);
					continue;
				}

				logger.info("Loading {} properties for agency '{}'", agencyValues.size(), agencyId);
				for (String code : agencyValues.fieldNames()) {
					Object rawValue = agencyValues.getValue(code);
					if (rawValue == null) {
						logger.warn("Agency '{}' setting '{}' has null value, skipping", agencyId, code);
						continue;
					}
					properties.setProperty((agencyId + "." + code).toLowerCase(), String.valueOf(rawValue));
				}
			}
		} catch (Exception e) {
			logger.fatal("Unable to initialize NCIP properties from mod-settings.");
			logger.error("NCIP init failure", e);
			ncipProperties.remove(tenant);
			throw e;
		}

		// FETCH ADDRESS TYPES - UNCHANGED
		try {
			logger.info("=======> initializing address types");
			String addressTypesEndpoint = okapiBaseEndpoint + Constants.ADDRESS_TYPES;
			String addressTypesResponse = callApiGet(addressTypesEndpoint, context.request().headers());
			JsonObject addressesTypesAsJson = new JsonObject(addressTypesResponse);
			JsonArray addressTypeArray = addressesTypesAsJson.getJsonArray("addressTypes");
			Iterator addressTypeIterator = addressTypeArray.iterator();
			while (addressTypeIterator.hasNext()) {
				JsonObject addressType = (JsonObject) addressTypeIterator.next();
				properties.setProperty(addressType.getString("id"), addressType.getString("addressType"));
			}
		} catch (Exception e) {
			// THIS IS FINE...CAN CONTINUE
			logger.fatal("Unable to initialize address types.");
			logger.fatal(e.getLocalizedMessage());
		}

		ncipProperties.put(tenant, properties);
		logger.info("Has tenant properties {}", (ncipProperties.get(tenant) != null));
	}

	public String getConfigValue(String code, MultiMap okapiHeaders) {
		String returnValue = null;
		String okapiBaseEndpoint = okapiHeaders.get(Constants.X_OKAPI_URL);
		// String configEndpoint = okapiBaseEndpoint +
		// "/configurations/entries?query=(code==" + URLEncoder.encode(code) + ")";
		String configEndpoint = okapiBaseEndpoint + "/configurations/entries?query=";

		try {
			String query = "code==" + StringUtil.cqlEncode(code);
			String url = configEndpoint + PercentCodec.encode(query);
			String response = callApiGet(url, okapiHeaders);
			JsonObject jsonObject = new JsonObject(response);
			JsonArray configs = jsonObject.getJsonArray(Constants.CONFIGS);
			if (configs == null || configs.isEmpty()) {
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
			throws Exception {
		return FolioGatewayClient.get(uriString, okapiHeaders);

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
			responseBuffer.append("ERROR: ");
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
