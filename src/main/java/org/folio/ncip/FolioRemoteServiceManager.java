package org.folio.ncip;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.extensiblecatalog.ncip.v2.service.AcceptItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.CheckInItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.CheckOutItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.kie.api.runtime.KieContainer;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.time.LocalDateTime;

public class FolioRemoteServiceManager implements RemoteServiceManager {

	private static final Logger logger = Logger.getLogger(FolioRemoteServiceManager.class);
	private MultiMap okapiHeaders;
	private Properties ncipProperties;
	private KieContainer kieContainer;
	
	public FolioRemoteServiceManager() throws Exception {

	}

	public FolioRemoteServiceManager(Properties properties) {
		
	}

	public Properties getNcipProperties() {
		return ncipProperties;
	}

	public void setNcipProperties(Properties ncipProperties) {
		this.ncipProperties = ncipProperties;
	}
	
	public KieContainer getKieContainer() {
		return kieContainer;
	}

	public void setKieContainer(KieContainer kieContainer) {
		this.kieContainer = kieContainer;
	}

	public MultiMap getOkapiHeaders() {
		return okapiHeaders;
	}

	public void setOkapiHeaders(MultiMap multiMap) {
		this.okapiHeaders = multiMap;
	}

	long millisElapsedSince(long t0) {
		return (System.nanoTime() - t0) / (1000L * 1000L);
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

	public HttpResponse<String> callApiPost(String url, HttpClient client, JsonObject body)
			throws Exception, IOException, InterruptedException {
		HttpRequest r = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMinutes(1))
				.header(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
				.header(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN).version(Version.HTTP_1_1)
				.header(Constants.CONTENT_TYPE_TEXT, Constants.CONTENT_JSON)
				.header(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
				.header(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
				.POST(BodyPublishers.ofString(body.toString())).build();

		HttpResponse<String> response = client.send(r, BodyHandlers.ofString());

		logger.info("POST:");
		logger.info(body.toString());
		logger.info(url);
		logger.info(response.statusCode());
		logger.info(response.body().toString());

		if (response.statusCode() > 399) {
			String responseBody = processErrorResponse(response);
			throw new Exception(responseBody);
		}

		return response;

	}

	public HttpResponse<String> callApiDelete(String url, HttpClient client)
			throws Exception, IOException, InterruptedException {
		HttpRequest r = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMinutes(1))
				.header(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
				.header(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN).version(Version.HTTP_1_1)
				.header(Constants.CONTENT_TYPE_TEXT, Constants.CONTENT_JSON)
				.header(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
				.header(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
				.DELETE().build();

		HttpResponse<String> response = client.send(r, BodyHandlers.ofString());
		logger.info("DELETE:");
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
	

	public JsonObject checkIn(CheckInItemInitiationData initData, String agencyId) throws Exception {

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_FOR_CIRC);
		LocalDateTime now = LocalDateTime.now();
		String returnDate = dtf.format(now);

		String itemBarcode = initData.getItemId().getItemIdentifierValue();
		UUID id = UUID.randomUUID();
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);

		//IF REQUIRED CONFIG VALUES HAVE NOT BEEN INITIALIZED, THEN INIT THEM
		//THIS HAS TO HAPPEN HERE (AND NOT DURING VERT.X START) BECAUSE IT NEEDS
		//AN AUTHENTICATED USER TO MAKE THE API CAllS
		if (ncipProperties.get(agencyId + Constants.INITIALIZED_PROPERTY) == null)
			initProperties(agencyId, baseUrl);

		String servicePoint = ncipProperties.getProperty(agencyId + ".checkin.service.point.id");

		HttpClient client = HttpClient.newBuilder().build();
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("servicePointId", servicePoint);
		jsonObject.put("checkInDate", returnDate);
		jsonObject.put("itemBarcode", itemBarcode);
		jsonObject.put("id", id.toString());

		String url = baseUrl + Constants.CHECK_IN_BY_BARCODE;
		HttpResponse<String> checkInResponse = callApiPost(url, client, jsonObject);

		return new JsonObject(checkInResponse.body());
	}

	public JsonObject checkOut(CheckOutItemInitiationData initData, String agencyId) throws Exception {
		
		UUID id = UUID.randomUUID();
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		HttpClient client = HttpClient.newBuilder().build();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_FOR_CIRC);
		LocalDateTime now = LocalDateTime.now();
		String loanDate = dtf.format(now);
		String itemBarcode = initData.getItemId().getItemIdentifierValue();
		String userBarcode = initData.getUserId().getUserIdentifierValue();
				
		//LOOKUP USER & CHECK FOR BLOCKS
		//ADDED BECAUSE THE CHECKOUT API DOES NOT LOOK FOR 'BLOCKS'
		//**THIS CAN BE REMOVED WHEN UXPROD-1683 IS COMPLETED**
		JsonObject user = lookupPatronRecord(initData.getUserId());
		if (user == null)
			throw new FolioNcipException(Constants.USER_NOT_FOUND);
		
		user = gatherPatronData(user,user.getString("id"));
		
    	//DO MANUAL BLOCKS EXIST?
    	JsonArray blocks = user.getJsonArray("manualblocks");
    	Iterator  i = blocks.iterator();
    	while (i.hasNext()) {
    		JsonObject block = (JsonObject) i.next();
    		if (block.getBoolean(Constants.BORROWING_BLOCK)) throw new FolioNcipException(Constants.BLOCKED); 
    		if (block.getBoolean(Constants.REQUEST_BLOCK)) throw new FolioNcipException(Constants.BLOCKED); 
    	}
    	//IS THE PATRON ACTIVE?
    	if (!user.getBoolean(Constants.ACTIVE)) throw new FolioNcipException(Constants.BLOCKED); 
		///END CHECKING FOR BLOCK
				
				
		// IF REQUIRED CONFIG VALUES HAVE NOT BEEN INITIALIZED, THEN INIT THEM
		if (ncipProperties.get(agencyId + Constants.INITIALIZED_PROPERTY) == null)
			initProperties(agencyId, baseUrl);

		String servicePoint = ncipProperties.getProperty(agencyId + ".checkout.service.point.id");
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("itemBarcode", itemBarcode);
		jsonObject.put("userBarcode", userBarcode);
		jsonObject.put("id", id.toString());
		jsonObject.put("loanDate", loanDate);
		jsonObject.put("servicePointId", servicePoint);

		String url = baseUrl + Constants.CHECK_OUT_BY_BARCODE;

		HttpResponse<String> checkoutResponse = callApiPost(url, client, jsonObject);
		JsonObject checkoutResponseAsJson = new JsonObject(checkoutResponse.body());
		return checkoutResponseAsJson;
	}

	/**
	 * The acceptItem method: 1) creates an instance (bib) 2) creates a holding
	 * record 3) creates an item 4) places the item on hold
	 * 
	 * If anything fails, it attempts to go back and delete the already created
	 * Objects (e.g. Item ID already exists so item creation fails..it backs out the
	 * holding and instance record
	 **/
	public JsonObject acceptItem(AcceptItemInitiationData initData, UserId userId, String requesterAgencyId)
			throws Exception {

		JsonObject returnValues = new JsonObject();
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		HttpClient client = HttpClient.newBuilder().build();
		
		// LOOKUP THE USER
		JsonObject user = lookupPatronRecord(userId);

		// IF USER ID WAS NOT FOUND - RETURN AN ERROR:
		if (user == null)
			throw new FolioNcipException(Constants.USER_NOT_FOUND);

		// VALIDATE PICKUP LOCATION
		String pickUpLocationCode = initData.getPickupLocation().getValue();
		String pickuLocationUrl = baseUrl + "/service-points?query=(code==" + pickUpLocationCode
				+ "+AND+pickupLocation==true)";
		HttpResponse<String> servicePointResponse = callApiGet(pickuLocationUrl, client);
		JsonObject servicePoints = new JsonObject(servicePointResponse.body());
		if (servicePoints.getJsonArray("servicepoints").size() == 0)
			throw new FolioNcipException("pickup location code note found: " + pickUpLocationCode);

		// GENERATE UUIDS FOR OBJECTS
		UUID instanceUuid = UUID.randomUUID();
		UUID holdingsUuid = UUID.randomUUID();
		UUID itemUuid = UUID.randomUUID();

		// IF REQUIRED CONFIG VALUES HAVE NOT BEEN INITIALIZED, THEN INIT THEM
		if (ncipProperties.get(requesterAgencyId + Constants.INITIALIZED_PROPERTY) == null)
			initProperties(requesterAgencyId, baseUrl);

		// BUILD INSTANCE
		JsonObject instance = new JsonObject();
		instance.put("title", retreiveItemTitle(initData));
		instance.put("instanceTypeId", ncipProperties.get(requesterAgencyId + ".instance.type.id"));
		instance.put("id", instanceUuid.toString());
		instance.put("source", ncipProperties.get(requesterAgencyId + ".instance.source"));
		instance.put("discoverySuppress", true);
		JsonArray identifiersArray = new JsonArray();
		JsonObject identifier = new JsonObject();
		identifier.put("identifierTypeId", ncipProperties.get(requesterAgencyId + ".instance.custom.identifier.id"));
		identifier.put("value", initData.getRequestId().getRequestIdentifierValue());
		identifiersArray.add(identifier);
		instance.put("identifiers", identifiersArray);

		try {
			// CALL INSTANCE API:
			String url = baseUrl + Constants.INSTANCE_URL;
			HttpResponse<String> instanceResponse = callApiPost(url, client, instance);

			// CALL HOLDINGS API:
			JsonObject holdings = new JsonObject();
			String holdingsPermLocation = ncipProperties.getProperty(requesterAgencyId + ".holdings.perm.location.id");
			holdings.put("id", holdingsUuid.toString());
			holdings.put("instanceId", instanceUuid.toString());
			holdings.put("discoverySuppress", true);
			//REQUIRED, ELSE IT WILL NOT SHOW UP IN INVENTORY SEARCH BY LOCA.
			holdings.put("permanentLocationId", holdingsPermLocation);    
			url = baseUrl + Constants.HOLDINGS_URL;
			HttpResponse<String> holdingsResponse = callApiPost(url, client, holdings);

			// CALL ITEMS API
			String itemStatusName = ncipProperties.getProperty(requesterAgencyId + ".item.status.name");
			String itemLocation = ncipProperties.getProperty(requesterAgencyId + ".item.perm.location.id");
			ItemId itemId = initData.getItemId();
			JsonObject item = new JsonObject();
			item.put("id", itemUuid.toString());
			item.put("holdingsRecordId", holdingsUuid.toString());
			item.put("discoverySuppress", true);
			item.put("itemLevelCallNumber", itemId.getItemIdentifierValue());
			// PLACE HOLD DOES NOT WORK UNLESS THE ITEM HAS A PERM LOCATION
			JsonObject permLocation = new JsonObject();
			permLocation.put("id", itemLocation);
			JsonObject materialType = new JsonObject();
			materialType.put("id", ncipProperties.getProperty(requesterAgencyId + ".item.material.type.id"));
			JsonObject status = new JsonObject();
			status.put("name", itemStatusName);
			JsonObject permLoanType = new JsonObject();
			permLoanType.put("id", ncipProperties.getProperty(requesterAgencyId + ".item.perm.loan.type.id"));

			item.put("status", status);
			item.put("materialType", materialType);
			item.put("permanentLoanType", permLoanType);
			item.put("barcode", itemId.getItemIdentifierValue());
			item.put("permanentLocation", permLocation);

			url = baseUrl + Constants.ITEM_URL;
			HttpResponse<String> itemResponse = callApiPost(url, client, item);

			// PLACE REQUEST (HOLD)
			JsonObject request = new JsonObject();
			request.put("requestType", "Page");
			// FOR EXPLAINATION ABOUT HARDCODE FULFILLMENT
			//SEE NOTES.TXT
			request.put("fulfilmentPreference", "Hold Shelf");
			String uid = user.getString("id");
			request.put("requesterId", uid);
			request.put("itemId", itemUuid.toString());
			String sPointId = servicePoints.getJsonArray("servicepoints").getJsonObject(0).getString("id");
			request.put("pickupServicePointId", sPointId);
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_FOR_CIRC);
			LocalDateTime now = LocalDateTime.now();
			request.put("requestDate", dtf.format(now));

			url = baseUrl + Constants.REQUEST_URL;
			HttpResponse<String> requestRespone = callApiPost(url, client, request);

			returnValues.mergeIn(new JsonObject(requestRespone.body())).mergeIn(new JsonObject(itemResponse.body()))
					.mergeIn(new JsonObject(holdingsResponse.body()));
		} catch (Exception e) {
			// IF ANY OF THE ABOVE FAILED - ATTEMPT TO DELETE THE INSTANCE, HOLDINGS ITEM
			// THAT MAY HAVE BEEN CREATED ALONG THE WAY
			String deleteInstanceUrl = baseUrl + Constants.INSTANCE_URL + "/" + instanceUuid.toString();
			String deleteHoldingsUrl = baseUrl + Constants.HOLDINGS_URL + "/" + holdingsUuid.toString();
			String deleteItemUrl = baseUrl + Constants.ITEM_URL + "/" + itemUuid.toString();

			// TRY TO DELETE THE ITEM (IT MAY OR MAYNOT HAVE BEEN CREATED
			try {
				callApiDelete(deleteItemUrl, client);
			} catch (Exception itemError) {
				logger.error("unable to back out item: " + itemUuid.toString());
				logger.error(itemError.getMessage());
			}
			// TRY TO DELETE HOLDINGS
			try {
				callApiDelete(deleteHoldingsUrl, client);
			} catch (Exception holdingsError) {
				logger.error("unable to back out holdings record: " + holdingsUuid);
				logger.error(holdingsError.getMessage());
			}
			// TRY TO DELETE INSTANCE
			try {
				callApiDelete(deleteInstanceUrl, client);
			} catch (Exception instanceError) {
				logger.error("unable to back out instance record: " + instanceUuid);
				logger.error(instanceError.getMessage());
			}
			throw e;
		}
		return returnValues;
	}

	private String retreiveItemTitle(AcceptItemInitiationData initData) {
		String title = "";
		try {
			title = initData.getItemOptionalFields().getBibliographicDescription().getTitle();
		} catch (Exception e) {
			// do nothing since title is optional.
			// method will return an empty string
		}
		if (title == null)
			title = "";
		return title;
	}

	/**
	 * This method, used by the lookup user service, makes the 5 API calls in an
	 * asynchronous way
	 * @throws Exception 
	 * 
	 **/
	public JsonObject gatherPatronData(JsonObject user, String userId) throws Exception {

		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String groupId = user.getString("patronGroup");
		HttpClient client = HttpClient.newBuilder().build();
		final long LONG_DELAY_MS = 5000;

		List<String> apiCallsNeeded = Arrays.asList(baseUrl + "/circulation/loans?query=(userId=" + userId + "+and+status=open)",
				baseUrl + "/accounts?query=(userId==" + userId + ")", baseUrl + "/groups/" + groupId,
				baseUrl + "/manualblocks?query=(userId=" + userId + ")&limit=100",
				baseUrl + "/service-points-users?query=(userId==" + userId + ")");

		ExecutorService executor = Executors.newFixedThreadPool(6);
		CompletionService<HttpResponse> cs = new ExecutorCompletionService<>(executor);
		List<Future<HttpResponse>> completableList = new ArrayList<>();

		Iterator<String> iterator = apiCallsNeeded.iterator();
		while (iterator.hasNext()) {
			String url = iterator.next();
			completableList.add(cs.submit(() -> callApiGet(url, client)));
		}

		List<String> successes = new ArrayList<>();
		List<String> failures = new ArrayList<>();

		long startTime = System.nanoTime();
		while (completableList.size() > 0) {
			Future<HttpResponse> f = cs.poll();
			if (f != null) {
				completableList.remove(f);
				try {
					HttpResponse value = f.get();
					successes.add(value.body().toString());
					user.mergeIn(new JsonObject(value.body().toString()));
				} catch (Exception e) {
					failures.add(e.getMessage());
					logger.error("an api call failed");
					logger.error(e.toString());
		    		throw new Exception(" An API call failed.   Error when looking up patron details.  API calls to circ, accounts, manual blocks and service points.  " , e);
				}
			}
			if (millisElapsedSince(startTime) > LONG_DELAY_MS) {
				user.mergeIn(new JsonObject("{'error':'timedout'}"));
				break;
			}
		}
		executor.shutdown();

		// GET NAME OF SERVICE POINT -- TODO PATRON IS NOT TIED TO A LIBRARY, THEY ARE
		// TIED TO A PREFERRED SERVICE POINT
		// USING THAT INSTEAD - USEFUL OR REMOVE?
		// ALSO - SERVICE DESK FOR A PATRON IS NOT REQUIRED
		try {
			String defaultServicePointId = user.getJsonArray("servicePointsUsers").getJsonObject(0)
					.getString("defaultServicePointId");
			String url = baseUrl + "/service-points/" + defaultServicePointId;
			HttpResponse<String> response = callApiGet(url, client);
			JsonObject servicePoint = new JsonObject(response.body());
			user.mergeIn(servicePoint);
		} catch (Exception e) {
			// THIS IS FINE
			logger.info("patron does not have a preferred service point assigned");
		}
		return user;
	}

	//ONE TIME (PER AGENT) PROPERTY INITIALIZATION
	private void initProperties(String requesterAgencyId, String baseUrl) throws Exception {

		logger.info("=======> initializing properties");
		JSONParser parser = new JSONParser();
		try {

			InputStream inputStream =this.getClass().getResourceAsStream(Constants.INIT_PROP_FILE);
			JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(inputStream));
			JSONArray jsonArray = (JSONArray) obj.get("lookups");
			Iterator i = jsonArray.iterator();

			HttpClient client = HttpClient.newBuilder().build();
			HttpResponse<String> response = null;
			requesterAgencyId = requesterAgencyId.toLowerCase();
			int responseCode = 0;
			JsonObject jsonObject = null;

			while (i.hasNext()) {
				JSONObject setting = (JSONObject) i.next();
				String lookup = (String) setting.get("lookup");
				String id = (String) setting.get("id");
				String url = (String) setting.get("url");
				String returnArray = (String) setting.get("returnArray");
				String identifier = (String) setting.get("identifier");

				String lookupValue = ncipProperties.getProperty(requesterAgencyId + "." + lookup);

				logger.info("Initializing ");
				logger.info(lookup);
				logger.info(" using lookup value ");
				logger.info(lookupValue);

				url = url.replace("{lookup}", URLEncoder.encode(lookupValue));
				response = callApiGet(baseUrl + url.trim(), client);
				responseCode = response.statusCode();
				jsonObject = new JsonObject(response.body());
				if (responseCode > 200 || jsonObject.getJsonArray(returnArray).size() == 0)
					throw new Exception(
							"Your " + requesterAgencyId + ". " + lookup + " of " + lookupValue + " could not be found");
				
				ncipProperties.setProperty(requesterAgencyId + "." + id,
						jsonObject.getJsonArray(returnArray).getJsonObject(0).getString(identifier));

			}

			// IF WE MADE IT THIS FAR, ALL OF THE PROPERTIES HAVE BEEN INITIALIZED
			ncipProperties.setProperty(requesterAgencyId + Constants.INITIALIZED_PROPERTY, "true");

		} catch (Exception e) {
			logger.error("Failed attempting to initialize the properties needed to make the API calls");
			logger.error(e.getLocalizedMessage());
			throw new Exception("Initializing NCIP properties failed.  " + e.getLocalizedMessage());
		}

	}
	
	//LOOKUP PATRON METHOD SHARED BY
	//SEVERAL OF THE SERVICES
	public JsonObject lookupPatronRecord(UserId userid) throws Exception {
		String barcode = userid.getUserIdentifierValue();
		// LOOKUP THE PATRON
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		HttpClient client = HttpClient.newBuilder().build();
		String userApiUri = baseUrl + "/users?query=(barcode==" + barcode + ")&limit=1";
		HttpResponse<String> response = callApiGet(userApiUri, client);

		// WAS THE PATRON FOUND?
		JsonObject users = new JsonObject(response.body());
		if (users.getJsonArray("users").size() == 0)
			return null;
		
		JsonObject user = users.getJsonArray("users").getJsonObject(0);
		return user;
	}

	//METHOD THE LOOKUP USER SERVICE CALLS
	//GET USER AND USER DETAILS (E.G ACCOUNTS)
	public JsonObject lookupUser(UserId userid) throws Exception {
		JsonObject user = lookupPatronRecord(userid);
		if (user == null) return user;
		String id = user.getString("id");
		user = gatherPatronData(user, id);
		return user;
	}

}