package org.folio.ncip;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.extensiblecatalog.ncip.v2.service.FiscalTransactionInformation;
import org.extensiblecatalog.ncip.v2.service.Location;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.RequestId;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.util.StringUtil;
import org.folio.util.PercentCodec;
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

		
	private static final Logger logger = LogManager.getLogger(FolioRemoteServiceManager.class);
	private static final Map<String, String> REQUEST_TYPE = Map.of("page", "Page",
			"hold", "Hold",
			"recall", "Recall");
	
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
			responseString = EntityUtils.toString(entity, "UTF-8");
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

	public void callApiPut(String uriString, JsonObject body) throws Exception{
		final String timeoutString = System.getProperty(Constants.SERVICE_MGR_TIMEOUT,Constants.DEFAULT_TIMEOUT);
		int timeout = Integer.parseInt(timeoutString);
		logger.info("Using timeout: {}", timeout);
		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.put()
				.setConfig(config)
				.setUri(uriString)
				.setEntity(new StringEntity(body.toString(), ContentType.APPLICATION_JSON))
				.setHeader(Constants.X_OKAPI_TENANT, okapiHeaders.get(Constants.X_OKAPI_TENANT))
				.setHeader(Constants.ACCEPT_TEXT, Constants.CONTENT_JSON_AND_PLAIN).setVersion(HttpVersion.HTTP_1_1)
				.setHeader(Constants.CONTENT_TYPE_TEXT, Constants.CONTENT_JSON)
				.setHeader(Constants.X_OKAPI_URL, okapiHeaders.get(Constants.X_OKAPI_URL))
				.setHeader(Constants.X_OKAPI_TOKEN, okapiHeaders.get(Constants.X_OKAPI_TOKEN))
				.build();

		HttpResponse response;
		HttpEntity entity;
		int responseCode;
		try {
			response = client.execute(request);
			entity = response.getEntity();
			responseCode = response.getStatusLine().getStatusCode();
		}
		catch(Exception e) {
			logger.fatal("callApiPut failed");
			logger.fatal(uriString);
			logger.fatal(body);
			logger.fatal(e.getMessage());
			throw e;
		}
		finally {
			client.close();
		}

		logger.info("PUT:");
		logger.info(body);
		logger.info(uriString);
		logger.info(responseCode);

		if (responseCode > 399) {
			if (entity != null) {
				String responseBody = processErrorResponse(EntityUtils.toString(entity, "UTF-8"));
				throw new Exception(responseBody);
			} else {
				throw new Exception("Failed to update record");
			}
		}
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

		String servicePoint = getProperty(agencyId,"checkin.service.point.id");

		JsonObject jsonObject = new JsonObject();
		jsonObject.put("servicePointId", servicePoint);
		jsonObject.put("checkInDate", returnDate);
		jsonObject.put("itemBarcode", itemBarcode);
		jsonObject.put(Constants.ID, id.toString());

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
		String itemBarcode = initData.getItemId().getItemIdentifierValue();

		// LOOKUP USER & CHECK FOR BLOCKS
		// ADDED BECAUSE THE CHECKOUT API DOES NOT LOOK FOR 'BLOCKS'
		// **THIS CAN BE REMOVED WHEN UXPROD-1683 IS COMPLETED**
		JsonObject user = lookupPatronRecord(initData.getUserId());
		if (user == null)
			throw new FolioNcipException(Constants.USER_NOT_FOUND);

		user = gatherPatronData(user, user.getString(Constants.ID));

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

		String servicePoint = getProperty(agencyId, "checkout.service.point.id");
		JsonObject jsonObject = new JsonObject();
		jsonObject.put("itemBarcode", itemBarcode);
		jsonObject.put("userBarcode", user.getString("barcode"));
		jsonObject.put(Constants.ID, id.toString());
		//jsonObject.put("loanDate", loanDate); //use default - current date/time
		jsonObject.put("servicePointId", servicePoint);

		String url = baseUrl + Constants.CHECK_OUT_BY_BARCODE;
		try {
			String checkoutResponse = callApiPost(url, jsonObject);
			JsonObject checkoutResponseAsJson = new JsonObject(checkoutResponse);
			addStaffInfoIfNeeded(agencyId, initData.getRequestId(), checkoutResponseAsJson.getString(Constants.ID), baseUrl);
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
			String returnDate = getDateTimeNowString();
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

	private void addStaffInfoIfNeeded(String agencyId, RequestId requestId, String loanUuid, String baseUrl){
		String noteEnabled = getProperty(agencyId, "request.note.enabled");
		if (Constants.BOOLEAN_TRUE.equalsIgnoreCase(noteEnabled) && requestId != null &&
				requestId.getRequestIdentifierValue() != null) {
			JsonObject staffInfo = new JsonObject();
			staffInfo.put("action", getProperty(agencyId, "checkout.loan.info.type"));
			staffInfo.put("actionComment", String.format(Constants.NOTE_TITLE_TEMPLATE, requestId.getRequestIdentifierValue()));
			try {
				callApiPost(baseUrl + String.format(Constants.ADD_STAFF_INFO_URL, loanUuid), staffInfo);
			} catch (Exception e) {
				logger.error("Unable to add staff info to loan: {}", loanUuid);
				logger.error(e.getMessage());
			}
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

		String callNumber = null;
		if (initData.getItemOptionalFields() != null && initData.getItemOptionalFields().getItemDescription() != null) {
			callNumber = initData.getItemOptionalFields().getItemDescription().getCallNumber();
		}

		// VALIDATE PICKUP LOCATION
		String pickUpLocationCode = initData.getPickupLocation().getValue();
		String sPointId = getServicePointId(pickUpLocationCode, baseUrl);

		// GENERATE UUIDS FOR OBJECTS
		UUID instanceUuid = UUID.randomUUID();
		UUID holdingsUuid = UUID.randomUUID();
		UUID itemUuid = UUID.randomUUID();

		initProperties(requesterAgencyId, baseUrl);

		// BUILD INSTANCE
		JsonObject instance = new JsonObject();
		instance.put(Constants.TITLE, retreiveItemTitle(initData));
		instance.put("instanceTypeId", getProperty(requesterAgencyId,"instance.type.id"));
		instance.put(Constants.ID, instanceUuid.toString());
		instance.put("source", getProperty(requesterAgencyId,"instance.source"));
		instance.put("discoverySuppress", true);
		JsonArray identifiersArray = new JsonArray();
		JsonObject identifier = new JsonObject();
		identifier.put("identifierTypeId", getProperty(requesterAgencyId, "instance.custom.identifier.id"));
		identifier.put("value", initData.getRequestId().getRequestIdentifierValue());
		identifiersArray.add(identifier);
		instance.put("identifiers", identifiersArray);

		try {
			// CALL INSTANCE API:
			String url = baseUrl + Constants.INSTANCE_URL;
			String instanceResponse = callApiPost(url, instance);

			// CALL HOLDINGS API:
			JsonObject holdings = new JsonObject();
			String holdingsPermLocation = getProperty(requesterAgencyId,"holdings.perm.location.id");
			holdings.put(Constants.ID, holdingsUuid.toString());
			holdings.put("sourceId", getProperty(requesterAgencyId, "holdings.source.id"));
			holdings.put("instanceId", instanceUuid.toString());
			holdings.put("discoverySuppress", true);
			// REQUIRED, ELSE IT WILL NOT SHOW UP IN INVENTORY SEARCH BY LOCA.
			holdings.put("permanentLocationId", holdingsPermLocation);
			url = baseUrl + Constants.HOLDINGS_URL;
			String holdingsResponse = callApiPost(url, holdings);

			// CALL ITEMS API
			String itemStatusName = getProperty(requesterAgencyId, "item.status.name");
			String itemLocation = getProperty(requesterAgencyId, "item.perm.location.id");
			ItemId itemId = initData.getItemId();
			JsonObject item = new JsonObject();
			item.put(Constants.ID, itemUuid.toString());
			item.put(Constants.HOLDINGS_RECORD_ID, holdingsUuid.toString());
			item.put("discoverySuppress", true);

			item.put("itemLevelCallNumber", StringUtils.isNotBlank(callNumber) ? callNumber : itemId.getItemIdentifierValue());
			// PLACE HOLD DOES NOT WORK UNLESS THE ITEM HAS A PERM LOCATION
			JsonObject permLocation = new JsonObject();
			permLocation.put(Constants.ID, itemLocation);
			JsonObject materialType = new JsonObject();
			materialType.put(Constants.ID, getProperty(requesterAgencyId, "item.material.type.id"));
			JsonObject status = new JsonObject();
			status.put("name", itemStatusName);
			JsonObject permLoanType = new JsonObject();
			permLoanType.put(Constants.ID, getProperty(requesterAgencyId, "item.perm.loan.type.id"));

			item.put(Constants.STATUS, status);
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
			request.put("fulfillmentPreference", getProperty(requesterAgencyId, "request.accept.fulfillment_preference"));
			String uid = user.getString(Constants.ID);
			request.put("requesterId", uid);
			request.put("itemId", itemUuid.toString());
			request.put("instanceId", holdings.getString("instanceId"));
			request.put(Constants.REQUEST_LEVEL, "Item");
			request.put(Constants.HOLDINGS_RECORD_ID, holdingsUuid.toString());
			request.put("pickupServicePointId", sPointId);
			DateTimeFormatter dtf = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
			ZonedDateTime now = ZonedDateTime.now();
			request.put("requestDate", dtf.format(now));

			url = baseUrl + Constants.REQUEST_URL;
			String requestRespone = callApiPost(url, request);

			returnValues.mergeIn(new JsonObject(requestRespone)).put("item", new JsonObject(itemResponse))
					.put("holdings", new JsonObject(holdingsResponse));

			addDefaultPatronFee(initData.getFiscalTransactionInformation(), user.getString(Constants.ID), user.getString(Constants.PATRON_GROUP), baseUrl);
			addNoteIfNeeded(requesterAgencyId, returnValues.getString(Constants.ID), initData.getRequestId().getRequestIdentifierValue(), baseUrl);
		} catch (Exception e) {
			// IF ANY OF THE ABOVE FAILED - ATTEMPT TO DELETE THE INSTANCE, HOLDINGS ITEM
			// THAT MAY HAVE BEEN CREATED ALONG THE WAY
			deleteItemAndRelatedRecords(baseUrl, instanceUuid.toString(), holdingsUuid.toString(), itemUuid.toString());
			throw e;
		}
		return returnValues;
	}

	protected JsonObject addDefaultPatronFee(FiscalTransactionInformation fiscalTransactionInformation, String userId, String patronGroupId, String baseUrl) throws Exception {
		JsonObject result = new JsonObject();
		if (fiscalTransactionInformation != null && fiscalTransactionInformation.getFiscalActionType() != null &&
				Constants.CHARGE_DEFAULT_PATRON_FEE.equalsIgnoreCase(fiscalTransactionInformation.getFiscalActionType().getValue())) {
			try {
				JsonObject owner = new JsonObject(callApiGet(baseUrl + Constants.FEE_OWNER_URL));
				JsonArray ownersArray = owner.getJsonArray("owners");
				if (ownersArray.isEmpty()) {
					throw new FolioNcipException("Failed to find fee owner Reshare-ILL");
				}
				String ownerId = ownersArray.getJsonObject(0).getString(Constants.ID);
				String patronGroup = new JsonObject(callApiGet(baseUrl +  Constants.PATRON_GROUP_BY_ID + patronGroupId))
						.getString("group");
				if (patronGroup == null) {
					throw new FolioNcipException("Failed to find patron group " + patronGroupId);
				}
				JsonObject fees = new JsonObject(callApiGet(baseUrl + Constants.FEE_FINE_BY_OWNER_AND_TYPE
						.replace("$ownerId$", ownerId).replace("$feeType$", StringUtil.urlEncode(patronGroup))));
				if (fees.getJsonArray("feefines").isEmpty()) {
					throw new FolioNcipException("Failed to find fee type " + patronGroup);
				}
				JsonObject fee = fees.getJsonArray("feefines").getJsonObject(0);
				JsonObject charge = new JsonObject();
				charge.put("ownerId", ownerId);
				charge.put("feeFineId", fee.getString(Constants.ID));
				charge.put("amount", fee.getValue("defaultAmount"));
				JsonObject paymentStatus = new JsonObject();
				paymentStatus.put("name", Constants.DEFAULT_PAYMENT_STATUS);
				charge.put("paymentStatus", paymentStatus);
				JsonObject status = new JsonObject();
				status.put("name", Constants.DEFAULT_FEE_STATUS);
				charge.put(Constants.STATUS, status);
				charge.put("remaining", fee.getValue("defaultAmount"));
				charge.put("feeFineType", fee.getString("feeFineType"));
				charge.put("feeFineOwner", ownersArray.getJsonObject(0).getString("owner"));
				charge.put("userId", userId);
				charge.put(Constants.ID, UUID.randomUUID().toString());
				if (fiscalTransactionInformation.getItemDetails() != null && fiscalTransactionInformation.getItemDetails().getItemId() != null &&
						fiscalTransactionInformation.getItemDetails().getItemId().getItemIdentifierValue() != null) {
					charge.put("barcode", fiscalTransactionInformation.getItemDetails().getItemId().getItemIdentifierValue());
				}
				return new JsonObject(callApiPost(baseUrl + Constants.ACCOUNT_URL, charge));
			} catch (Exception e) {
				logger.error("Failed to add default patron fee", e);
				throw e;
			}
		}
		return result;
	}

	private void deleteItemAndRelatedRecords(String baseUrl, String instanceUuid, String holdingsUuid, String itemUuid){
		String deleteInstanceUrl = baseUrl + Constants.INSTANCE_URL + "/" + instanceUuid;
		String deleteHoldingsUrl = baseUrl + Constants.HOLDINGS_URL + "/" + holdingsUuid;
		String deleteItemUrl = baseUrl + Constants.ITEM_URL + "/" + itemUuid;

		// Try to delete the item ( it may or may not have been created)
		try {
			callApiDelete(deleteItemUrl);
		} catch (Exception itemError) {
			logger.error("Unable to back out item: {}", itemUuid);
			logger.error(itemError.getMessage());
		}
		// Try to delete holdings
		try {
			callApiDelete(deleteHoldingsUrl);
		} catch (Exception holdingsError) {
			logger.error("Unable to back out holdings record: {}", holdingsUuid);
			logger.error(holdingsError.getMessage());
		}
		// Try to delete instance
		try {
			callApiDelete(deleteInstanceUrl);
		} catch (Exception instanceError) {
			logger.error("Unable to back out instance record: {}", instanceUuid);
			logger.error(instanceError.getMessage());
		}
	}

	private String getServicePointId(String pickUpLocationCode, String baseUrl) throws Exception {
		String query = "code==" + StringUtil.cqlEncode(pickUpLocationCode) + " AND pickupLocation==true";
		String pickupLocationUrl = baseUrl + "/service-points?query=" + PercentCodec.encode(query);
		String servicePointResponse = callApiGet(pickupLocationUrl);
		JsonObject servicePoints = new JsonObject(servicePointResponse);
		if (servicePoints.getJsonArray("servicepoints").size() == 0)
			throw new FolioNcipException("pickup location code note found: " + pickUpLocationCode);
		return servicePoints.getJsonArray("servicepoints").getJsonObject(0).getString(Constants.ID);
	}
	public JsonObject requestItem(RequestItemInitiationData initData) throws Exception {
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String agencyId = initData.getInitiationHeader().getFromAgencyId().getAgencyId().getValue();
		agencyId = agencyId == null ? agencyId : agencyId.toLowerCase();
		initProperties(agencyId, baseUrl);
		String hrid = initData.getBibliographicId(0).getBibliographicRecordId().getBibliographicRecordIdentifier();
		final boolean titleRequest = initData.getRequestScopeType() != null && initData.getRequestScopeType().getValue().toLowerCase().contains(Constants.TITLE);
		final String requestType = REQUEST_TYPE.getOrDefault(initData.getRequestType().getValue().toLowerCase(), "Page");
		String pickUpLocationCode = null;
		if (initData.getPickupLocation() != null && StringUtils.isNotBlank(initData.getPickupLocation().getValue())) {
			pickUpLocationCode = initData.getPickupLocation().getValue();
		}
		String locationCode = null;
		if (initData.getItemOptionalFields() != null && initData.getItemOptionalFields().getLocations() != null &&
				!initData.getItemOptionalFields().getLocations().isEmpty()) {
			Location location = initData.getItemOptionalFields().getLocation(0);
			if (location.getLocationName() != null && location.getLocationName().getLocationNameInstances() != null &&
					!location.getLocationName().getLocationNameInstances().isEmpty()) {
				locationCode = location.getLocationName().getLocationNameInstance(0).getLocationNameValue();
			}
		}

		JsonObject returnValues = new JsonObject();
		JsonObject user = lookupPatronRecord(initData.getUserId());
		if (user == null)
			throw new FolioNcipException(Constants.USER_NOT_FOUND);
		try {

			String searchUrl =  baseUrl + (titleRequest ? Constants.INSTANCE_SEARCH_URL : Constants.ITEM_SEARCH_URL)
					.replace("$hrid$", hrid);
			String responseString = callApiGet(searchUrl);
			JsonObject response = new JsonObject(responseString);

			Integer totalRecords = response.getInteger(Constants.TOTAL_RECORDS);
			if (totalRecords == 1) {

				JsonObject request = new JsonObject();
				if (titleRequest) {
					JsonObject instanceObject = response.getJsonArray("instances").getJsonObject(0);
					request.put("instanceId", instanceObject.getString(Constants.ID));
					request.put(Constants.REQUEST_LEVEL, "Title");
				} else {
					JsonObject itemObject = response.getJsonArray("items").getJsonObject(0);
					String holdingsUrl = baseUrl + Constants.HOLDINGS_URL + "/" + itemObject.getString(Constants.HOLDINGS_RECORD_ID);
					String holdingResponseString = callApiGet(holdingsUrl);
					JsonObject holdingResponse = new JsonObject(holdingResponseString);
					request.put("itemId", itemObject.getString(Constants.ID));
					request.put("instanceId", holdingResponse.getString("instanceId"));
					request.put(Constants.HOLDINGS_RECORD_ID, holdingResponse.getString(Constants.ID));
					request.put(Constants.REQUEST_LEVEL, "Item");
				}
				request.put("requestType", requestType);
				request.put("fulfillmentPreference", getProperty(agencyId, "request.fulfillment_preference"));
				request.put("requesterId", user.getString(Constants.ID));
				request.put("requestDate", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now()));
				String servicePointId;
				if (pickUpLocationCode != null) {
					servicePointId = getServicePointId(pickUpLocationCode, baseUrl);
				} else {
					servicePointId = getProperty(agencyId, "checkout.service.point.id");
				}
				request.put("pickupServicePointId", servicePointId);
				request.put("itemLocationCode", locationCode);

				String requestUrl = baseUrl + Constants.REQUEST_URL;
				String requestResponse = callApiPost(requestUrl, request);

				returnValues.mergeIn(new JsonObject(requestResponse));
				addNoteIfNeeded(agencyId, returnValues.getString(Constants.ID),
						initData.getRequestId().getRequestIdentifierValue(), baseUrl);
			} else {
				logger.error("Found total of {} items by hrid {}", totalRecords, hrid);
				throw new FolioNcipException(Constants.REQUEST_ITEM_MISSING_PROBLEM);
			}
		} catch (Exception exception) {
			logger.error("Failed to Page request", exception);
			throw  exception;
		}

		return returnValues;
	}

	private void addNoteIfNeeded(String agencyId, String requestUuid, String illRequestId, String baseUrl) {
		try {
			String noteEnabled = getProperty(agencyId, "request.note.enabled");
			if (Constants.BOOLEAN_TRUE.equalsIgnoreCase(noteEnabled)) {
				JsonObject note = new JsonObject();
				note.put("domain", Constants.NOTE_DOMAIN_REQUESTS);
				note.put("typeId", getProperty(agencyId, "request.note.id"));
				note.put(Constants.TITLE, String.format(Constants.NOTE_TITLE_TEMPLATE, illRequestId));
				JsonArray links = new JsonArray();
				JsonObject link = new JsonObject();
				link.put("type", Constants.NOTE_LINK_TYPE_REQUEST);
				link.put(Constants.ID, requestUuid);
				links.add(link);
				note.put("links", links);
				callApiPost(baseUrl + Constants.NOTES_URL, note);
			}
		} catch (Exception e) {
			logger.error("Failed to add request note to request: {}", requestUuid);
			logger.error(e.getMessage());
		}
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
		String groupId = user.getString(Constants.PATRON_GROUP);
		final String userIdQuery = "query=(" + PercentCodec.encode("userId==" + StringUtil.cqlEncode(userId) + ")");
		final long LONG_DELAY_MS = 10000;

		List<String> apiCallsNeeded = Arrays.asList(
				baseUrl + "/manualblocks?query=" + userIdQuery + "&limit=100",
				baseUrl + "/automated-patron-blocks/" + userId,
				baseUrl + "/groups/" + groupId,
				baseUrl + "/service-points-users?query=" + userIdQuery + "&limit=700");

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
				String id = (String) setting.get(Constants.ID);
				String url = (String) setting.get("url");
				String returnArray = (String) setting.get("returnArray");
				String identifier = (String) setting.get("identifier");

				String lookupValue = getProperty(requesterAgencyId, lookup);
				
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
				url = url.replace("{lookup}", PercentCodec.encode(lookupValue));

				
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
		String userIdentifier = userid.getUserIdentifierValue();
		// LOOKUP THE PATRON
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		userIdentifier = StringUtil.cqlEncode(userIdentifier);
		StringBuilder query = new StringBuilder()
		          .append("(barcode==")
		          .append(userIdentifier)
		          .append(" or externalSystemId==")
		          .append(userIdentifier)
		          .append(" or username==")
		          .append(userIdentifier)
		          .append(')');
		String userApiUri = baseUrl + "/users?query="  + PercentCodec.encode(query.toString());
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
		List<String> validTypes = Arrays.asList("barcode","externalsystemid","username");
		if (!validTypes.contains(type)) {
			throw new Exception("invalid patron lookup type provided: " + type);
		}

		if (type != null && type.equalsIgnoreCase("externalSystemId")) type = "externalSystemId";
		value = StringUtil.cqlEncode(value);
		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String query = "(" + type + "==" + value + ")";
		String userApiUri = baseUrl + "/users?query=" + PercentCodec.encode(query.toString()) + "&limit=1";

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
		String id = user.getString(Constants.ID);
		user = gatherPatronData(user, id);
		user.put("userUuid", id);
		return user;
	}

	public void checkUserPin(String userId, String pin) throws FolioNcipException {
		try {
			String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
			String pinApiUri = baseUrl + Constants.PATRON_PIN_VERIFY;
			JsonObject request = new JsonObject();
			request.put(Constants.ID, userId);
			request.put("pin", pin);
			callApiPost(pinApiUri, request);
		} catch (IOException | FolioNcipException e) {
			logger.error(new ParameterizedMessage("Could not check user {} pin", userId), e);
			throw new FolioNcipException("PIN check failed");
		}
	}

	public JsonObject cancelRequestItem(String requestId, UserId userId, String agencyId) throws Exception {

		if (ncipProperties == null) {
			throw new FolioNcipException("NCIP Properties have not been initialized.");
		}

		JsonObject user = lookupPatronRecord(userId);
		if (user == null) {
			throw new FolioNcipException(Constants.USER_NOT_FOUND);
		}

		String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
		String date = getDateTimeNowString();

		initProperties(agencyId, baseUrl);

		String reasonId = getProperty(agencyId,"cancel.request.reason.id");

		String url = baseUrl + Constants.REQUEST_URL + "/" + requestId;
		try {
			JsonObject requestResponse = new JsonObject(callApiGet(url));
			requestResponse.put(Constants.STATUS, Constants.REQUEST_CANCELLED_STATUS);
			requestResponse.put("cancelledByUserId", user.getString(Constants.ID));
			requestResponse.put("cancellationReasonId", reasonId);
			requestResponse.put("cancellationAdditionalInformation", Constants.REQUEST_CANCEL_ADDITIONAL_INFO);
			requestResponse.put("cancelledDate", date);
			callApiPut(url, requestResponse);

			return requestResponse;
		} catch (Exception e) {
			logger.error("Exception occurred during cancel request item");
			throw e;
		}
	}

	private String getDateTimeNowString(){
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT_FOR_CIRC);
		LocalDateTime now = LocalDateTime.now();
		return dtf.format(now);
	}

	public void deleteItem(String itemId, String agencyId) throws Exception {
		try {
			// Find item to delete
			String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
			String searchUrl = baseUrl + Constants.ITEM_SEARCH_BY_BARCODE_URL.replace("$barcode$", itemId);
			String itemResponseString = callApiGet(searchUrl);

			JsonObject itemResponse = new JsonObject(itemResponseString);
			Integer totalRecords = itemResponse.getInteger(Constants.TOTAL_RECORDS);
			if (totalRecords == 1) { // There is item
				JsonObject itemObject = itemResponse.getJsonArray("items").getJsonObject(0);
				String itemUuid = itemObject.getString(Constants.ID);
				String holdingsRecordId = itemObject.getString(Constants.HOLDINGS_RECORD_ID);

				if (ncipProperties == null) {
					throw new FolioNcipException("NCIP Properties have not been initialized.");
				}
				initProperties(agencyId, baseUrl);

				// Search open requests
				String requestResponseString = callApiGet(baseUrl + Constants.OPEN_REQUEST_BY_ITEM_ID_URL + itemUuid);
				JsonObject requestResponse = new JsonObject(requestResponseString);
				if (requestResponse.getInteger(Constants.TOTAL_RECORDS) > 0) {
					// Need to close open requests
					String reasonId = getProperty(agencyId, "cancel.request.reason.patron.id");

					JsonArray requests = requestResponse.getJsonArray("requests");
					requests.forEach(r -> {
						JsonObject requestObject = (JsonObject) r;
						try {
							String url = baseUrl + Constants.REQUEST_URL + "/" + requestObject.getString(Constants.ID);
							requestObject.put(Constants.STATUS, Constants.REQUEST_CANCELLED_STATUS);
							requestObject.put("cancellationReasonId", reasonId);
							requestObject.put("cancellationAdditionalInformation", Constants.REQUEST_CANCEL_PATRON_ADDITIONAL_INFO);
							requestObject.put("cancelledDate", getDateTimeNowString());
							callApiPut(url, requestObject);
						} catch (Exception e){
							logger.error("Could not cancel request {}", requestObject.getString(Constants.ID));
						}
					});
				}

				String softDeleteEnabled = getProperty(agencyId, "item.soft.delete");
				if (Constants.BOOLEAN_TRUE.equalsIgnoreCase(softDeleteEnabled)) {
					// Update item to Unavailable
					itemObject.getJsonObject(Constants.STATUS).put("name", Constants.ITEM_STATUS_UNAVAILABLE);
					callApiPut(baseUrl + Constants.ITEM_URL + "/" + itemUuid, itemObject);
				} else {
					String holdingsUrl = baseUrl + Constants.HOLDINGS_URL + "/" + itemObject.getString(Constants.HOLDINGS_RECORD_ID);
					String holdingResponseString = callApiGet(holdingsUrl);
					JsonObject holdingResponse = new JsonObject(holdingResponseString);

					deleteItemAndRelatedRecords(baseUrl, holdingResponse.getString("instanceId"), holdingsRecordId, itemUuid);
				}
			}
		} catch (Exception exception) {
			logger.error("Exception occurred during delete item");
			throw exception;
		}
	}

	private String getProperty(String agencyId, String key){
		String value = ncipProperties.getProperty(agencyId + "." + key);
		if (value == null) {
			value = ncipProperties.getProperty(key);
		}
		return value;
	}

	public JsonObject createUserFiscalTransaction(UserId userId, FiscalTransactionInformation fiscalTransactionInformation) throws Exception {
		try {
			String baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);
			JsonObject user = lookupPatronRecord(userId);
			return addDefaultPatronFee(fiscalTransactionInformation, user.getString(Constants.ID), user.getString(Constants.PATRON_GROUP), baseUrl);
		} catch (Exception exception) {
			logger.error("Exception occurred during CreateUserFiscalTransaction");
			throw exception;
		}
	}
}
