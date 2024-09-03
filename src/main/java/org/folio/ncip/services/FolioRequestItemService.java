package org.folio.ncip.services;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.BibliographicId;
import org.extensiblecatalog.ncip.v2.service.ItemDescription;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.ItemIdentifierType;
import org.extensiblecatalog.ncip.v2.service.ItemOptionalFields;
import org.extensiblecatalog.ncip.v2.service.Location;
import org.extensiblecatalog.ncip.v2.service.LocationName;
import org.extensiblecatalog.ncip.v2.service.LocationNameInstance;
import org.extensiblecatalog.ncip.v2.service.LocationType;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RequestId;
import org.extensiblecatalog.ncip.v2.service.RequestIdentifierType;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.RequestItemResponseData;
import org.extensiblecatalog.ncip.v2.service.RequestItemService;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.RequestScopeType;
import org.extensiblecatalog.ncip.v2.service.RequestType;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.extensiblecatalog.ncip.v2.service.UserIdentifierType;
import org.extensiblecatalog.ncip.v2.service.UserOptionalFields;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioNcipException;
import org.folio.ncip.FolioRemoteServiceManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FolioRequestItemService extends FolioNcipService implements RequestItemService {

	private static final Logger logger = Logger.getLogger(FolioRequestItemService.class);

	@Override
	public RequestItemResponseData performService(RequestItemInitiationData initData, ServiceContext serviceContext,
			RemoteServiceManager serviceManager) {
		logger.info("RequestItemService");

		BibliographicId bibliographicId = initData.getBibliographicId(0);
		UserId userId = initData.getUserId();
		logger.info("accept bibliographic id : " + bibliographicId);

		try {
			validateUserId(userId);
			validateBibliographicIdIsPresent(bibliographicId);
			initData.getRequestType().getValue();
			validateRequestIdIsPresent(initData.getRequestId());
		}
		catch(Exception exception) {
			logger.error("Failed validating userId and itemId. " + exception.getLocalizedMessage());
			return setProblemInResponse(new Problem(new ProblemType(Constants.REQUEST_ITEM_PROBLEM), Constants.REQUEST_ITEM_INPUT_PROBLEM,
					exception.getMessage(),exception.getMessage()));
		}

		ItemId itemId = new ItemId();
		RequestId ncipRequestId = new RequestId();
		ItemDescription itemDescription = new ItemDescription();
		LocationNameInstance locationNameInstance = new LocationNameInstance();
		LocationNameInstance libraryNameInstance = new LocationNameInstance();
		UserId optionalUserId = new UserId();
		try {
			JsonObject requestItemResponseDetails = ((FolioRemoteServiceManager)serviceManager).requestItem(initData);
			String assignedRequestId = requestItemResponseDetails.getString("id");
			String requesterId = requestItemResponseDetails.getString("requesterId");
			String barcode = null;
			String callNumber = null;
			String locationName = null;
			String libraryName = null;
			if(requestItemResponseDetails.getJsonObject("item") != null) {
				JsonObject item = requestItemResponseDetails.getJsonObject("item");
				barcode = item.getString("barcode");
				callNumber = item.getString("callNumber");
				if (item.getJsonObject("location") != null) {
					locationName = item.getJsonObject("location").getString("name");
				    libraryName = item.getJsonObject("location").getString("libraryName");
				}
			}
			ncipRequestId.setRequestIdentifierValue(assignedRequestId);
			itemDescription.setCallNumber(callNumber);
			locationNameInstance.setLocationNameValue(locationName);
			libraryNameInstance.setLocationNameValue(libraryName);
			itemId.setItemIdentifierValue(barcode);
			optionalUserId.setUserIdentifierValue(requesterId);
		}
		catch(Exception exception) {
			logger.error("Failed to Page RequestItem " + exception.getLocalizedMessage());
			return setProblemInResponse(new Problem(new ProblemType(Constants.REQUEST_ITEM_PROBLEM),
					Constants.UNKNOWN_DATA_ELEMENT, Constants.REQUEST_ITEM_PROBLEM, exception.getMessage()));
		}
		ItemIdentifierType itemIdentifierType = new ItemIdentifierType(Constants.SCHEME, Constants.ITEM_BARCODE);
		RequestIdentifierType requestIdentifierType = new RequestIdentifierType(Constants.SCHEME,Constants.REQUEST_ID);
		Location location = new Location();
		LocationType locationType = new LocationType(Constants.SCHEME, Constants.ITEM);
		LocationName locationName = new LocationName();
		locationNameInstance.setLocationNameLevel(new BigDecimal(4));
		libraryNameInstance.setLocationNameLevel(new BigDecimal(3));
		locationName.setLocationNameInstances(List.of(libraryNameInstance, locationNameInstance));
		location.setLocationName(locationName);
		location.setLocationType(locationType);
		ItemOptionalFields itemOptionalFields = new ItemOptionalFields();
		itemOptionalFields.setItemDescription(itemDescription);
		itemOptionalFields.setLocations(List.of(location));
		itemId.setItemIdentifierType(itemIdentifierType);
		ncipRequestId.setRequestIdentifierType(requestIdentifierType);

		optionalUserId.setUserIdentifierType(new UserIdentifierType(Constants.SCHEME,"uuid"));
		UserOptionalFields userOptionalFields = new UserOptionalFields();
		userOptionalFields.setUserIds(List.of(optionalUserId));

		RequestItemResponseData requestItemResponseData = new RequestItemResponseData();
		requestItemResponseData.setItemId(itemId);
		requestItemResponseData.setRequestId(ncipRequestId);
		requestItemResponseData.setUserId(userId);
		requestItemResponseData.setItemOptionalFields(itemOptionalFields);
		requestItemResponseData.setRequestType(new RequestType(Constants.SCHEME, Constants.PAGE));
		requestItemResponseData.setRequestScopeType(new RequestScopeType(Constants.SCHEME, Constants.ITEM));
		requestItemResponseData.setUserOptionalFields(userOptionalFields);
		return requestItemResponseData;
	}

	private RequestItemResponseData setProblemInResponse(Problem problem){
		RequestItemResponseData responseData = new RequestItemResponseData();
		if (responseData.getProblems() == null) responseData.setProblems(new ArrayList<>());
		responseData.getProblems().add(problem);
		return responseData;
	}

	protected void validateBibliographicIdIsPresent(BibliographicId bibliographicId) throws FolioNcipException {

		if (bibliographicId == null || bibliographicId.getBibliographicRecordId() == null ||
				bibliographicId.getBibliographicRecordId().getBibliographicRecordIdentifier() == null) {
			FolioNcipException exception = new FolioNcipException("Item id missing");
			throw exception;
		}

	}
}
