package org.folio.ncip;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.UUID;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.folio.util.StringUtil;
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
public class NcipConfigMigrate extends FolioNcipHelper {
	
	private static final Logger logger = Logger.getLogger(NcipConfigCheck.class);
	
	public NcipConfigMigrate(Promise<Void> promise) {
		super(promise);
	}

	private MultiMap okapiHeaders;
	

	public void process(RoutingContext routingContext) throws Exception {
		logger.info("migrating ncip configurations...");
	    okapiHeaders = routingContext.request().headers(); 
		String baseUrl = routingContext.request().headers().get(Constants.X_OKAPI_URL);
		logger.info("BaseEndpoint: " + baseUrl);
		
		//TESTING - GET CONFIGS FROM MOD-CONFIGURATION
		String okapiBaseEndpoint = routingContext.request().getHeader(Constants.X_OKAPI_URL);
		String configQuery = "(module==NCIP)";
		String configEndpoint = okapiBaseEndpoint + "/configurations/entries?query=" + PercentCodec.encode(configQuery) + "&limit=10000";
		String response = callApiGet(configEndpoint, routingContext.request().headers());
		JsonObject jsonObject = new JsonObject(response);
		JsonArray configs = jsonObject.getJsonArray(Constants.CONFIGS);
		if (configs.size() < 1) {
			logger.info("No NCIP configurations found to migrate.  QUERY:" + configEndpoint);
			throw new Exception(
					"No NCIP configurations found in mod-configuration");
		}
		Iterator configsIterator = configs.iterator();
		String url = baseUrl + Constants.SETTINGS_URL;
		while (configsIterator.hasNext()) {
			JsonObject config = (JsonObject) configsIterator.next();
			String code = config.getString(Constants.CODE_KEY);
			String configName = config.getString(Constants.CONFIG_KEY);
			String value = config.getString(Constants.VALUE_KEY);
			String combinedKey = configName + "::" + code ;
			String configUuid = config.getString(Constants.ID);
			
			//CONSTRUCT mod-settings
			JsonObject setting = new JsonObject();
			setting.put("id", configUuid);
			setting.put("scope", Constants.SETTING_SCOPE);
			setting.put(Constants.KEY, combinedKey);
			setting.put("value", value);
			System.out.println(setting.toString());
			//INSERT mod-settings
			String settingsResponse = callApiPost(url, setting);
			
			//VERIFY THE CONFIGURATION HAS BEEN MIGRATED TO MOD-SETTINGS
			String getConfigEndpoint = okapiBaseEndpoint + "/configurations/entries/" + configUuid;
			String verifySettingResponse = callApiGet(getConfigEndpoint , okapiHeaders);
			System.out.println(getConfigEndpoint);
			System.out.println(verifySettingResponse);
			JsonObject oldConfig = new JsonObject(verifySettingResponse);
			
			
			//VERIFY THE CONFIGURATION HAS BEEN MIGRATED TO MOD-SETTINGS
			String getSettingsEndpoint = okapiBaseEndpoint + "/settings/entries/" + configUuid;
			String newSettingsResult = callApiGet(getConfigEndpoint , okapiHeaders);
			JsonObject newSetting = new JsonObject(newSettingsResult);
			newSetting.getString("configName");
			newSetting.getString("code");
			if (combinedKey.equalsIgnoreCase(newSetting.getString("configName") + "::" + newSetting.getString("code"))) {
				System.out.println("MATCHED - CONFIRMED CONFIG HAS BEEN MIGRATED TO SETTING");
			}
			if (newSetting.getString("value").equalsIgnoreCase(value)) {
				System.out.println("MATCHED - CONFIRMED CONFIG HAS BEEN MIGRATED TO SETTING - COMPARED VALUE");
			}
			System.out.println(setting);
			System.out.println(oldConfig);
			
			
			
			
		}
		
	}
	
	
	
	public String callApiPost(String uriString, JsonObject body) 
			throws FolioNcipException, IOException {
			final String timeoutString = System.getProperty(Constants.SERVICE_MGR_TIMEOUT,Constants.DEFAULT_TIMEOUT);
			int timeout = Integer.parseInt(timeoutString);
			logger.info("Using timeout: " + timeout);
			RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();
			CloseableHttpClient client = HttpClients.custom().build();
			HttpUriRequest request = RequestBuilder.post()
				.setConfig(config)
				.setUri(uriString)
				.setEntity(new StringEntity(body.toString(),"UTF-8"))
				.setHeader(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
				.setHeader(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN).setVersion(HttpVersion.HTTP_1_1)
				.setHeader(Constants.CONTENT_TYPE_TEXT, Constants.CONTENT_JSON)
				.setHeader(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
				.setHeader(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
				.build();

			HttpResponse response = null;
			HttpEntity entity = null;
			String responseString = null;
			int responseCode = 0;
			try {
				response = client.execute(request);
				entity = response.getEntity();
				responseString = entity != null
					    ? EntityUtils.toString(entity, StandardCharsets.UTF_8)
					    : "";
				responseCode = response.getStatusLine().getStatusCode();
			}
			catch(IOException e) {
				String responseBody = e.getLocalizedMessage();
				logger.fatal("callApiPost failed");
				logger.fatal(uriString);
				logger.fatal(body);
				logger.fatal(e.getMessage());
				throw e;
			}
			finally {
				client.close();
			}

			logger.info("POST:");
			logger.info(body.toString());
			logger.info(uriString);
			logger.info(responseCode);
			logger.info(responseString);

			if (responseCode > 399) {
				String responseBody = processErrorResponse(responseString);
				throw new FolioNcipException(responseBody);
			}

			return responseString;

		}
	
	
	public HttpResponse callApiDelete(String uriString) throws Exception {

		final String timeoutString = System.getProperty(Constants.SERVICE_MGR_TIMEOUT,Constants.DEFAULT_TIMEOUT);
		int timeout = Integer.parseInt(timeoutString);
		logger.info("Using timeout: " + timeout);
		RequestConfig config = RequestConfig.custom()
			.setConnectTimeout(timeout)
			.setSocketTimeout(timeout)
			.build();
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.delete() // set timeout?
			.setUri(uriString)
			.setConfig(config)
			.setHeader(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
			.setHeader(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN) // do i need version here?
			.setHeader(Constants.CONTENT_TYPE_TEXT, Constants.CONTENT_JSON)
			.setHeader(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
			.setHeader(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
			.build();

		HttpResponse response = null;
		int responseCode = 0;
		try {
			response = client.execute(request);
			responseCode = response.getStatusLine().getStatusCode();
		}
		catch(Exception e) {
			logger.fatal("callApiDelete failed");
			logger.fatal(uriString);
			logger.fatal(e.getMessage());
			throw e;
		}
		finally {
			client.close();
		}
		
		logger.info("DELETE:");
		logger.info(uriString);
		logger.info(responseCode);

		if (responseCode > 399) {
			throw new FolioNcipException("Response code from delete: " + responseCode);
		}

		return response;

	}
	
	

}
