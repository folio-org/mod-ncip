package org.folio.ncip;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


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
		// TODO Auto-generated constructor stub
	}

	private MultiMap okapiHeaders;
	

	public void process(RoutingContext routingContext) throws Exception {
		logger.info("ncip config check...");
	    okapiHeaders = routingContext.request().headers(); 
		String baseUrl = routingContext.request().headers().get(Constants.X_OKAPI_URL);
		logger.info("BaseEndpoint: " + baseUrl);
		JSONParser parser = new JSONParser();
		//InputStream inputStream =this.getClass().getResourceAsStream(Constants.INIT_PROP_FILE);
		InputStream inputStream =this.getClass().getClassLoader().getResourceAsStream(Constants.INIT_PROP_FILE);
		JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(inputStream));
		JSONArray jsonArray = (JSONArray) obj.get("lookups");
		
		
		String okapiBaseEndpoint = routingContext.request().getHeader(Constants.X_OKAPI_URL);
		String tenant = routingContext.request().getHeader(Constants.X_OKAPI_TENANT);
		String conifgQuery = URLEncoder.encode("(module==NCIP and configName<>rules and configName<>toolkit)");
		String configEndpoint = okapiBaseEndpoint + "/configurations/entries?query=query=" + conifgQuery + "&limit=200";
		//String encodedUrl = URLEncoder.encode(configEndpoint, "UTF-8");
		String response = callApiGet(configEndpoint, routingContext.request().headers());
		JsonObject jsonObject = new JsonObject(response);
		JsonArray configs = jsonObject.getJsonArray(Constants.CONFIGS);
		if (configs.size() < 1) {
			logger.info("No toolkit configurations found.  Using defaults.  QUERY:" + configEndpoint);
			throw new Exception(
					"No NCP configurations found in mod-configuration");
		}
		
		Iterator configsIterator = configs.iterator();
		
		while (configsIterator.hasNext()) {
			JsonObject config = (JsonObject) configsIterator.next();
			String code = config.getString(Constants.CODE_KEY);
			String value = config.getString(Constants.VALUE_KEY);
			JSONObject setting = returnSearch(jsonArray,code);
			if (setting == null) continue;
			String lookup = (String) setting.get("lookup");
			String id = (String) setting.get("id");
			String url = (String) setting.get("url");
			String returnArray = (String) setting.get("returnArray");
			String identifier = (String) setting.get("identifier");
			Enumeration<String> properties = (Enumeration<String>) ncipProperties.propertyNames();
			CloseableHttpClient client = HttpClients.createDefault();
			HttpResponse lookupResponse = null;
			
			
		
				
			  logger.info("Initializing ");
			  logger.info(lookup);
			  logger.info(" using lookup value ");
			  logger.info(lookup);

			 url = url.replace("{lookup}", URLEncoder.encode(value));
			 logger.info("WILL LOOKUP " + lookup + " WITH URL " + url + " USING VALUE " + value);
			 

			 
			 
			HttpUriRequest request = RequestBuilder.get()
					.setUri(baseUrl + url.trim())
					.setHeader(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
					.setHeader(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN) //do i need version here?
					.setHeader(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
					.setHeader(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
					.build();
					
			lookupResponse = client.execute(request);
			 HttpEntity entity = lookupResponse.getEntity();
			 String responseString = EntityUtils.toString(entity, "UTF-8");
			 int responseCode = lookupResponse.getStatusLine().getStatusCode();
			 
			

			logger.info("GET:");
			logger.info(baseUrl + url.trim());
			logger.info(lookupResponse.getStatusLine().getStatusCode());
			logger.info(responseString);


			
			if (responseCode> 399) {
				String responseBody = processErrorResponse(responseString);
				throw new Exception(responseBody);
			}

			 jsonObject = new JsonObject(responseString);
			 if (responseCode > 200 || jsonObject.getJsonArray(returnArray).size() == 0)
					throw new Exception(
							"The lookup of " + value + " could not be found for " + code);
	      
	    }
	}
	
	
	  public JSONObject returnSearch(JSONArray a, String searchValue){
		  
		  for(Object o: a){
			    if ( o instanceof JSONObject ) {
			        String config =(String) ((JSONObject) o).get("lookup");
			       // String actualValue = searchValue.substring(searchValue.indexOf('.')+1);
			        System.out.println("=====>" + config + " vs " + searchValue);
			        if (config.equalsIgnoreCase(searchValue)) return (JSONObject) o;
			    }
			}
		  return null;
	  }
	  
}
