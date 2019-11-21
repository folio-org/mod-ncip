package org.folio.ncip;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

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
 * 	VALIDATES THE VALUES
 * IN THE ncip.properties CONFIGURATION FILE
 *	JUST FOR TESTING SETTINGS DURING SETUP
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
		JSONParser parser = new JSONParser();
		InputStream inputStream =this.getClass().getResourceAsStream(Constants.INIT_PROP_FILE);
		JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(inputStream));
		JSONArray jsonArray = (JSONArray) obj.get("lookups");
		
		HttpClient client = HttpClient.newBuilder().build();
		HttpResponse<String> response = null;
		
		Enumeration<String> properties = (Enumeration<String>) ncipProperties.propertyNames();
	    while (properties.hasMoreElements()) {
	     String key = properties.nextElement();
	     Properties values = (Properties) ncipProperties.get(key);
	     Enumeration<String> innerProperties = (Enumeration<String>) values.propertyNames();
	     int responseCode = 0;
		 JsonObject jsonObject = null;
	      while (innerProperties.hasMoreElements()) {
	    	  String  innerKey = innerProperties.nextElement();
	    	  String  innerValues = values.getProperty(innerKey);
	    	  System.out.println(innerKey + " : "  + innerValues);
	    	  JSONObject setting = returnSearch(jsonArray,innerKey);
	    	  if (setting == null) continue;
			  String lookup = (String) setting.get("lookup");
			  String id = (String) setting.get("id");
			  String url = (String) setting.get("url");
			  String returnArray = (String) setting.get("returnArray");
			  String identifier = (String) setting.get("identifier");
				
				
				logger.info("Initializing ");
				logger.info(lookup);
				logger.info(" using lookup value ");
				logger.info(lookup);

				url = url.replace("{lookup}", URLEncoder.encode(innerValues));
				System.out.println("WILL LOOKUP " + lookup + " WITH URL " + url + " USING VALUE " + innerValues);
				response = callApiGet(baseUrl + url.trim(), client);
				responseCode = response.statusCode();
				jsonObject = new JsonObject(response.body());
				if (responseCode > 200 || jsonObject.getJsonArray(returnArray).size() == 0)
					throw new Exception(
							"The lookup of " + innerValues + " could not be found for " + innerKey);
				
				
				
	      }
	    }
	}
	
	
	  public JSONObject returnSearch(JSONArray a, String searchValue){
		  
		  for(Object o: a){
			    if ( o instanceof JSONObject ) {
			        String config =(String) ((JSONObject) o).get("lookup");
			        String actualValue = searchValue.substring(searchValue.indexOf('.')+1);
			        //System.out.println("=====>" + config + " vs " + actualValue);
			        if (config.equalsIgnoreCase(actualValue)) return (JSONObject) o;
			    }
			}
		  return null;
	  }
	  
	  
		public HttpResponse<String> callApiGet(String url, HttpClient client)
				throws Exception, IOException, InterruptedException {
			HttpRequest r = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMinutes(1))
					.header(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
					.header(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN).version(Version.HTTP_1_1)
					.header(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
					.header(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
					.GET().build();

			HttpResponse<String> response = client.send(r, BodyHandlers.ofString());

			logger.info("GET:");
			logger.info(url);
			logger.info(response.statusCode());
			logger.info(response.body().toString());

			if (response.statusCode() > 399) {
				String responseBody = processErrorResponse(response);
				throw new Exception(responseBody);
			}

			return response;

		}
		
		
		public String processErrorResponse(HttpResponse<String> response) {
			// SOMETIMES ERRORS ARE RETURNED BY THE API AS PLAIN STRINGS
			String responseBody = response.body().toString();
			// SOMETIMES ERRORS ARE RETURNED BY THE API AS JSON
			try {
				JsonObject jsonObject = new JsonObject(response.body());
				JsonArray errors = jsonObject.getJsonArray("errors");
				Iterator i = errors.iterator();
				responseBody = "ERROR: ";
				while (i.hasNext()) {
					JsonObject errorMessage = (JsonObject) i.next();
					responseBody += errorMessage.getString("message");
				}
			} catch (Exception exception) {
				// NOT A PROBLEM, ERROR WAS A STRING. UNABLE TO PARSE
				// AS JSON
			}
			return responseBody;
		}

}
