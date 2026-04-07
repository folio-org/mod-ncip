package org.folio.ncip;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.util.PercentCodec;

/*
 * 
 * Validates the values 
 * in the 'ncip.properties' configuration file
 * This is used just for testing during setup of NCIP
 * 
 */
public class NcipConfigCheck extends FolioNcipHelper {

	private static final Logger logger = Logger.getLogger(NcipConfigCheck.class);

	public NcipConfigCheck(Promise<Void> promise) {
		super(promise);
	}

	public void process(RoutingContext routingContext) throws Exception {

		logger.info("ncip config check...");

		MultiMap okapiHeaders = routingContext.request().headers();

		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String tenant = okapiHeaders.get(Constants.X_OKAPI_TENANT);
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new Exception("Missing required header: " + Constants.X_OKAPI_URL);
		}

		logger.info("BaseEndpoint: " + baseUrl);

		JSONParser parser = new JSONParser();
		JSONArray jsonArray;
		try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Constants.INIT_PROP_FILE)) {
			if (inputStream == null) {
				throw new Exception("Unable to load " + Constants.INIT_PROP_FILE + " from classpath");
			}

			JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			jsonArray = (JSONArray) obj.get("lookups");
		}

		if (jsonArray == null) {
			throw new Exception("Missing 'lookups' array in " + Constants.INIT_PROP_FILE);
		}

		Map<String, JSONObject> lookupByCode = buildLookupIndex(jsonArray);

		String settingsQuery = "scope==" + Constants.SETTING_SCOPE;
		String settingsEndpoint = baseUrl + Constants.SETTINGS_URL + "?query="
				+ PercentCodec.encode(settingsQuery)
				+ "&limit=1000";

		String response = FolioGatewayClient.get(settingsEndpoint, okapiHeaders);

		JsonObject jsonObject = new JsonObject(response);
		JsonArray items = jsonObject.getJsonArray("items");

		if (items == null || items.isEmpty()) {
			throw new Exception("No NCIP agency settings found in mod-settings for tenant " + tenant);
		}

		for (Object o : items) {

			JsonObject item = (JsonObject) o;
			String agency = item.getString(Constants.KEY);
			if ("toolkit".equalsIgnoreCase(agency)) {
				continue;
			}
			JsonObject values = item.getJsonObject(Constants.VALUE_KEY);

			if (values == null || values.isEmpty()) {
				logger.warn("Skipping agency '" + agency + "' because it has no settings value object");
				continue;
			}

			for (String code : values.fieldNames()) {
				String value = values.getString(code);
				validateLookup(lookupByCode, baseUrl, okapiHeaders, code, value, agency, tenant);
			}
		}
	}

	private void validateLookup(Map<String, JSONObject> lookupByCode, String baseUrl, MultiMap okapiHeaders,
			String code, String value, String agency, String tenant) throws Exception {

		if (value == null || value.isBlank()) {
			return;
		}

		JSONObject setting = lookupByCode.get(code.toLowerCase());
		if (setting == null) {
			return;
		}

		String lookup = (String) setting.get("lookup");
		String url = (String) setting.get("url");
		String returnArray = (String) setting.get("returnArray");

		logger.info("Validating '" + lookup + "' for agency '" + agency + "' using value '" + value + "'");

		String lookupValue = value;
		if (lookupValue.contains("/")) {
			lookupValue = '"' + lookupValue + '"';
		}

		String fullUrl = baseUrl + url.replace("{lookup}", PercentCodec.encode(lookupValue)).trim();
		String lookupResponse = FolioGatewayClient.get(fullUrl, okapiHeaders);
		JsonObject lookupJson = new JsonObject(lookupResponse);
		JsonArray resultArray = lookupJson.getJsonArray(returnArray);

		if (resultArray == null || resultArray.isEmpty()) {
			throw new Exception("Lookup value '" + value + "' not found for setting '" + code
					+ "' (agency='" + agency + "', tenant='" + tenant + "', url='" + fullUrl + "')");
		}
	}

	private Map<String, JSONObject> buildLookupIndex(JSONArray lookups) {
		Map<String, JSONObject> lookupByCode = new HashMap<>();
		for (Object o : lookups) {
			if (o instanceof JSONObject) {
				JSONObject lookup = (JSONObject) o;
				String code = (String) lookup.get("lookup");
				if (code != null) {
					lookupByCode.put(code.toLowerCase(), lookup);
				}
			}
		}
		return lookupByCode;
	}
}
