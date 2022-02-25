package org.folio.ncip;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
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
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class FolioRemoteServiceManager implements RemoteServiceManager {

	private static final Logger logger = Logger.getLogger(FolioRemoteServiceManager.class);
	private MultiMap okapiHeaders;
	private Properties ncipProperties;

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

	public MultiMap getOkapiHeaders() {
		return okapiHeaders;
	}

	public void setOkapiHeaders(MultiMap multiMap) {
		this.okapiHeaders = multiMap;
	}

	long millisElapsedSince(long t0) {
		return (System.nanoTime() - t0) / (1000L * 1000L);
	}

	public String callApiGet(String uriString) throws Exception, IOException, InterruptedException {
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
		
		HttpResponse response = null;
		HttpEntity entity = null;
		String responseString = null;
		int responseCode = 0;
		try {
			response = client.execute(request);
			entity = response.getEntity();
			responseString = EntityUtils.toString(entity, "UTF-8");
			responseCode = response.getStatusLine().getStatusCode();
		}
		catch(Exception e) {
			logger.fatal("getApiGet failed");
			logger.fatal(uriString);
			throw e;
		}
		finally {
			client.close();
		}

		logger.info("GET:");
		logger.info(uriString);
		logger.info(responseCode);
		logger.info(responseString);

		if (responseCode > 399) {
			String responseBody = processErrorResponse(responseString);
			throw new Exception(responseBody);
		}

		return responseString;

	}

	public String callApiPost(String uriString, JsonObject body) 
		throws Exception, IOException, InterruptedException {
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
			responseString = EntityUtils.toString(entity, "UTF-8");
			responseCode = response.getStatusLine().getStatusCode();
		}
		catch(Exception e) {
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
			throw new Exception(responseBody);
		}

		return responseString;

	}

	public HttpResponse callApiDelete(String uriString) throws Exception, IOException, InterruptedException {

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
			throw new Exception("Response code from delete: " + responseCode);
		}

		return response;

	}

	/**
	 * The method deals with error messages that are returned by the API as plain
	 * strings and messages returned as JSON
	 *
	 */
	public String processErrorResponse(String responseBody) {
		// SOMETIMES ERRORS ARE RETURNED BY THE API AS PLAIN STRINGS
		// SOMETIMES ERRORS ARE RETURNED BY THE API AS JSON
		try {
			JsonObject jsonObject = new JsonObject(responseBody);
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

		// BEFORE ANYTHING ELSE - MAKE SURE THE NCIP PROPERTIES HAVE
		// BEEN SET IN MOD-CONFIGURATION
		// CAN'T DO ANYTHING WITHOUT THEM
		if (ncipProperties == null) {
			logger.fatal(
					"NCIP Properties have not been initialized.  These properties (e.g. checkin.service.point.code) have to be set so the Checkin Item service can be called");
			throw new Exception(
					"NCIP Properties have not been initialized.  These properties (e.g. checkin.service.point.code) have to be set in mod-configuration so the Checkin Item service service can be called.");
		}

		DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
		ZonedDateTime now = ZonedDateTime.now();
		String returnDate = dtf.format(now);

		String itemBarcode = initData.getItemId().getItemIdentifierValue();
		UUID id = UUID.randomUUID();
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);

		initProperties(agencyId, baseUrl);

		String servicePoint = ncipProperties.getProperty(agencyId + ".checkin.service.point.id");

		JsonObject jsonObject = new JsonObject();
		jsonObject.put("servicePointId", servicePoint);
		jsonObject.put("checkInDate", returnDate);
		jsonObject.put("itemBarcode", itemBarcode);
		jsonObject.put("id", id.toString());

		String url = baseUrl + Constants.CHECK_IN_BY_BARCODE;
		String checkInResponse = callApiPost(url, jsonObject);

		return new JsonObject(checkInResponse);
	}

	public JsonObject checkOut(CheckOutItemInitiationData initData, String agencyId) throws Exception {

		// BEFORE ANYTHING ELSE - MAKE SURE THE NCIP PROPERTIES HAVE
		// BEEN SET IN MOD-CONFIGURATION
		// CAN'T DO ANYTHING WITHOUT THEM
		if (ncipProperties == null) {
			logger.fatal(
					"NCIP Properties have not been initialized.  These properties (e.g. checkout.service.point.code) have to be set so the Checkout item service can be called");
			throw new Exception(
					"NCIP Properties have not been initialized.  These properties (e.g. checkout.service.point.code) have to be set  in mod-configuration so the Checkout item service can be called.");
		}

		UUID id = UUID.randomUUID();
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_FOR_CIRC);
		LocalDateTime now = LocalDateTime.now();
		String loanDate = dtf.format(now);
		String itemBarcode = initData.getItemId().getItemIdentifierValue();
		String userBarcode = initData.getUserId().getUserIdentifierValue();

		// LOOKUP USER & CHECK FOR BLOCKS
		// ADDED BECAUSE THE CHECKOUT API DOES NOT LOOK FOR 'BLOCKS'
		// **THIS CAN BE REMOVED WHEN UXPROD-1683 IS COMPLETED**
		JsonObject user = lookupPatronRecord(initData.getUserId());
		if (user == null)
			throw new FolioNcipException(Constants.USER_NOT_FOUND);

		user = gatherPatronData(user, user.getString("id"));

		// DO MANUAL BLOCKS EXIST?
		JsonArray blocks = user.getJsonArray("manualblocks");
		Iterator i = blocks.iterator();
		while (i.hasNext()) {
			JsonObject block = (JsonObject) i.next();
			if (block.getBoolean(Constants.BORROWING_BLOCK))
				throw new FolioNcipException(Constants.BLOCKED);
		}
		// DO AUTOMATED BLOCKS EXIST?
    		JsonArray automatedPatronBlocks = user.getJsonArray("automatedPatronBlocks");
    		Iterator  automatedPatronBlocksIterator = automatedPatronBlocks.iterator();
    		while (automatedPatronBlocksIterator.hasNext()) {
    			JsonObject block = (JsonObject) automatedPatronBlocksIterator.next();
    			if (block.getBoolean(Constants.AUTOMATED_BORROWING_BLOCK)!= null && block.getBoolean(Constants.AUTOMATED_BORROWING_BLOCK)) throw new FolioNcipException(Constants.BLOCKED);
    		}
		// IS THE PATRON ACTIVE?
		if (!user.getBoolean("active"))
			throw new FolioNcipException(Constants.BLOCKED);
		/// END CHECKING FOR BLOCK

		initProperties(agencyId, baseUrl);

		String servicePoint = ncipProperties.getProperty(agencyId + ".checkout.service.point.id");
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("itemBarcode", itemBarcode);
		jsonObject.put("userBarcode", userBarcode);
		jsonObject.put("id", id.toString());
		//jsonObject.put("loanDate", loanDate); //use default - current date/time
		jsonObject.put("servicePointId", servicePoint);

		String url = baseUrl + Constants.CHECK_OUT_BY_BARCODE;
		try {
			String checkoutResponse = callApiPost(url, jsonObject);
			JsonObject checkoutResponseAsJson = new JsonObject(checkoutResponse);
			return checkoutResponseAsJson;
		}
		catch(Exception e) {
			//CHECKOUT FAILED - TRY TO CALL CHECKIN TRANSACTION
			//SO THE CHECKOUT CAN BE ATTEMPTED AGAIN.
			//WE'VE SEEN THE CHECKOUT TIMEOUT WHEN THE ENTIRE SYSTEM
			//IS SLOW - AND THE ITEM ENDS UP CHECKED OUT...SO JUST ATTEMPTING
			//A CHECKIN FOR CONVENIENCE
			logger.error("exception occured during checkout item");
			logger.error("attempting a checkin...in case the checkout actually worked");
			String returnDate = dtf.format(now);
			jsonObject.put("checkInDate", returnDate);
			Thread.sleep(4000);
			try {
				url = baseUrl + Constants.CHECK_IN_BY_BARCODE;
				callApiPost(url, jsonObject);
			} catch (Exception checkinError) {
				logger.error("unable to checkin item: " + itemBarcode);
				logger.error(checkinError.getMessage());
			}
			throw e;
		}
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

		// BEFORE ANYTHING ELSE - MAKE SURE THE NCIP PROPERTIES HAVE
		// BEEN SET IN MOD-CONFIGURATION
		// CAN'T DO ANYTHING WITHOUT THEM
		if (ncipProperties == null) {
			logger.fatal(
					"NCIP Properties have not been initialized.  These properties (e.g. instance.type.name) have to be set so the AcceptItem service can be called");
			throw new Exception(
					"NCIP Properties have not been initialized.  These properties (e.g. instance.type.name) have to be set in mod-configuration so the AcceptItem service can be called.");
		}

		// LOOKUP THE USER
		JsonObject user = lookupPatronRecord(userId);

		// IF USER ID WAS NOT FOUND - RETURN AN ERROR:
		if (user == null)
			throw new FolioNcipException(Constants.USER_NOT_FOUND);

		if (initData.getRequestId() == null) {
			throw new FolioNcipException(Constants.REQUEST_ID_MISSING);
		}

		// VALIDATE PICKUP LOCATION
		String pickUpLocationCode = initData.getPickupLocation().getValue();
		String pickuLocationUrl = baseUrl + "/service-points?query=(code==" + URLEncoder.encode(pickUpLocationCode)
				+ "+AND+pickupLocation==true)";
		String servicePointResponse = callApiGet(pickuLocationUrl);
		JsonObject servicePoints = new JsonObject(servicePointResponse);
		if (servicePoints.getJsonArray("servicepoints").size() == 0)
			throw new FolioNcipException("pickup location code note found: " + pickUpLocationCode);

		// GENERATE UUIDS FOR OBJECTS
		UUID instanceUuid = UUID.randomUUID();
		UUID holdingsUuid = UUID.randomUUID();
		UUID itemUuid = UUID.randomUUID();

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
			String instanceResponse = callApiPost(url, instance);

			// CALL HOLDINGS API:
			JsonObject holdings = new JsonObject();
			String holdingsPermLocation = ncipProperties.getProperty(requesterAgencyId + ".holdings.perm.location.id");
			holdings.put("id", holdingsUuid.toString());
			holdings.put("instanceId", instanceUuid.toString());
			holdings.put("discoverySuppress", true);
			// REQUIRED, ELSE IT WILL NOT SHOW UP IN INVENTORY SEARCH BY LOCA.
			holdings.put("permanentLocationId", holdingsPermLocation);
			url = baseUrl + Constants.HOLDINGS_URL;
			String holdingsResponse = callApiPost(url, holdings);

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
			String itemResponse = callApiPost(url, item);

			// PLACE REQUEST (HOLD)
			JsonObject request = new JsonObject();
			request.put("requestType", "Page");
			// FOR EXPLAINATION ABOUT HARDCODE FULFILLMENT
			// SEE NOTES.TXT
			request.put("fulfilmentPreference", "Hold Shelf");
			String uid = user.getString("id");
			request.put("requesterId", uid);
			request.put("itemId", itemUuid.toString());
			request.put("instanceId", holdings.getString("instanceId"));
			request.put("requestLevel", "Item");
			request.put("holdingsRecordId", holdingsUuid.toString());
			String sPointId = servicePoints.getJsonArray("servicepoints").getJsonObject(0).getString("id");
			request.put("pickupServicePointId", sPointId);
			DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
			ZonedDateTime now = ZonedDateTime.now();
			request.put("requestDate", dtf.format(now));

			url = baseUrl + Constants.REQUEST_URL;
			String requestRespone = callApiPost(url, request);

			returnValues.mergeIn(new JsonObject(requestRespone)).mergeIn(new JsonObject(itemResponse))
					.mergeIn(new JsonObject(holdingsResponse));
		} catch (Exception e) {
			// IF ANY OF THE ABOVE FAILED - ATTEMPT TO DELETE THE INSTANCE, HOLDINGS ITEM
			// THAT MAY HAVE BEEN CREATED ALONG THE WAY
			String deleteInstanceUrl = baseUrl + Constants.INSTANCE_URL + "/" + instanceUuid.toString();
			String deleteHoldingsUrl = baseUrl + Constants.HOLDINGS_URL + "/" + holdingsUuid.toString();
			String deleteItemUrl = baseUrl + Constants.ITEM_URL + "/" + itemUuid.toString();

			// TRY TO DELETE THE ITEM (IT MAY OR MAYNOT HAVE BEEN CREATED
			try {
				callApiDelete(deleteItemUrl);
			} catch (Exception itemError) {
				logger.error("unable to back out item: " + itemUuid.toString());
				logger.error(itemError.getMessage());
			}
			// TRY TO DELETE HOLDINGS
			try {
				callApiDelete(deleteHoldingsUrl);
			} catch (Exception holdingsError) {
				logger.error("unable to back out holdings record: " + holdingsUuid);
				logger.error(holdingsError.getMessage());
			}
			// TRY TO DELETE INSTANCE
			try {
				callApiDelete(deleteInstanceUrl);
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
	 * 
	 * @throws Exception
	 * 
	 **/
	public JsonObject gatherPatronData(JsonObject user, String userId) throws Exception {

		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String groupId = user.getString("patronGroup");
		// HttpClient client = HttpClient.newBuilder().build();
		final long LONG_DELAY_MS = 10000;

		List<String> apiCallsNeeded = Arrays.asList(
				baseUrl + "/manualblocks?query=(userId=" + userId + ")&limit=100",
				baseUrl + "/automated-patron-blocks/" + userId,
				baseUrl + "/groups/" + groupId,
				baseUrl + "/service-points-users?query=(userId==" + userId + ")&limit=700");

		ExecutorService executor = Executors.newFixedThreadPool(6);
		CompletionService<String> cs = new ExecutorCompletionService<>(executor);
		List<Future<String>> completableList = new ArrayList<>();

		Iterator<String> iterator = apiCallsNeeded.iterator();
		while (iterator.hasNext()) {
			String url = iterator.next();
			completableList.add(cs.submit(() -> callApiGet(url)));
		}

		List<String> successes = new ArrayList<>();
		List<String> failures = new ArrayList<>();

		long startTime = System.nanoTime();
		while (completableList.size() > 0) {
			Future<String> f = cs.poll();
			if (f != null) {
				completableList.remove(f);
				try {
					String value = f.get();
					successes.add(value);
					user.mergeIn(new JsonObject(value));
				} catch (Exception e) {
					failures.add(e.getMessage());
					logger.error("an api call failed");
					logger.error(e.toString());
					throw new Exception(
							" An API call failed.   Error when looking up patron details.  "
							+ "API manual and automated blocks and service points.  ", e);
				}
			}
			if (millisElapsedSince(startTime) > LONG_DELAY_MS) {
				JsonObject timeOutMessage = new JsonObject();
				timeOutMessage.put("error", "Request for user data took too long too respond.");
				user.mergeIn(timeOutMessage);
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
			if (defaultServicePointId == null) return user;
			String url = baseUrl + "/service-points/" + defaultServicePointId;
			String response = callApiGet(url);
			JsonObject servicePoint = new JsonObject(response);
			user.mergeIn(servicePoint);
		} catch (Exception e) {
			//IF THIS IS AN ISSUE WITH PERMISSIONS - THROW AN ERROR:
			if (e.getLocalizedMessage() != null && e.getLocalizedMessage().contains("permission")) {
				throw e;
			}
			logger.info("patron does not have a preferred service point assigned");
		}
		return user;
	}

	/**
	 * This method is called one time (per agent in the ncip.properties file) to
	 * initialize & store property values
	 *
	 */
	private void initProperties(String requesterAgencyId, String baseUrl) throws Exception {

		logger.info("=======> initializing properties");
		JSONParser parser = new JSONParser();
		CloseableHttpClient client = HttpClients.custom().build();
		try {
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(Constants.INIT_PROP_FILE);
			JSONObject obj = (JSONObject) parser.parse(new InputStreamReader(inputStream));
			JSONArray jsonArray = (JSONArray) obj.get("lookups");
			Iterator i = jsonArray.iterator();

			HttpResponse response = null;
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
				
				if (lookupValue == null) throw new Exception("configuration value missing for " + requesterAgencyId + "." + lookup);

				logger.info("Initializing ");
				logger.info(lookup);
				logger.info(" using lookup value ");
				logger.info(lookupValue);
				
				//TODO - BETTER WAY?
				//IF LOCATION CODES CONTAINS '/' WHICH MANY OF THEM DO
				//LOOKUP LOCATION DOESN'T WORK UNLESS THE LOCATION CODE
				//IS SURROUNDED BY QUOTES 
				if (lookupValue.contains("/")) lookupValue = '"' + lookupValue + '"';
				url = url.replace("{lookup}", URLEncoder.encode(lookupValue));
				
				final String timeoutString = System.getProperty(Constants.SERVICE_MGR_TIMEOUT,Constants.DEFAULT_TIMEOUT);
				int timeout = Integer.parseInt(timeoutString);
				logger.info("Using timeout: " + timeout);
				RequestConfig config = RequestConfig.custom()
					.setConnectTimeout(timeout)
					.setSocketTimeout(timeout)
					.build();

				HttpUriRequest request = RequestBuilder.get().setUri(baseUrl + url.trim())
					.setConfig(config)
					.setHeader(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
					.setHeader(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN) // do i need version here?
					.setHeader(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
					.setHeader(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN)).build();

				response = client.execute(request);
				HttpEntity entity = response.getEntity();
				String responseString = EntityUtils.toString(entity, "UTF-8");
				responseCode = response.getStatusLine().getStatusCode();

				if (responseCode > 400) {
					throw new Exception(responseString);
				}

				jsonObject = new JsonObject(responseString);
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
			throw new Exception(
					"Initializing NCIP properties failed.  Are you sure you have NCIP properties set for this AgencyId: "
							+ requesterAgencyId + ".  DETAILS: " + e.getLocalizedMessage());
		}
		finally {
			client.close();
		}

	}

	/**
	 * Lookup patron method shared by several of the services
	 *
	 */
	public JsonObject lookupPatronRecord(UserId userid) throws Exception {
		String barcode = userid.getUserIdentifierValue();
		// LOOKUP THE PATRON
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String userApiUri = baseUrl + "/users?query=(barcode==" + barcode + ")&limit=1";
		String response = callApiGet(userApiUri);

		// WAS THE PATRON FOUND?
		JsonObject users = new JsonObject(response);
		if (users.getJsonArray("users").size() == 0)
			return null;

		JsonObject user = users.getJsonArray("users").getJsonObject(0);
		return user;
	}

	/**
	 * Lookup patron method used by LookupUser service when AuthenticationInput is
	 * used instead of UserId
	 *
	 */
	public JsonObject lookupPatronRecordBy(String type, String value) throws Exception {
		// LOOKUP THE PATRON
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String userApiUri = baseUrl + "/users?query=(" + type + "==" + value + ")&limit=1";
		String response = callApiGet(userApiUri);

		// WAS THE PATRON FOUND?
		JsonObject users = new JsonObject(response);
		if (users.getJsonArray("users").size() == 0)
			return null;

		JsonObject user = users.getJsonArray("users").getJsonObject(0);
		return user;
	}

	/**
	 * Method the LookupUser service calls. Pulls together user and user details
	 * (e.g. accounts)
	 *
	 */
	public JsonObject lookupUser(UserId userid) throws Exception {
		JsonObject user = lookupPatronRecord(userid);
		if (user == null)
			return user;
		String id = user.getString("id");
		user = gatherPatronData(user, id);
		return user;
	}

}
