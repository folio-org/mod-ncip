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
import org.folio.ncip.Constants;
import org.folio.ncip.FolioNcipException;
import org.folio.ncip.FolioRemoteServiceManager;

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
		try {
			JsonObject requestItemResponseDetails = ((FolioRemoteServiceManager)serviceManager)
					.requestItem(bibliographicId.getBibliographicRecordId().getBibliographicRecordIdentifier(), userId);
			String assignedRequestId = requestItemResponseDetails.getJsonObject("request").getString("id");
			String barcode = requestItemResponseDetails.getJsonObject("item").getString("barcode");
			String callNumber = requestItemResponseDetails.getJsonObject("item").getString("callNumber");
			String locationName = requestItemResponseDetails.getJsonObject("item").getJsonObject("effectiveLocation").getString("name");
			ncipRequestId.setRequestIdentifierValue(assignedRequestId);
			itemDescription.setCallNumber(callNumber);
			itemDescription.setCopyNumber(barcode);
			locationNameInstance.setLocationNameValue(locationName);
			itemId.setItemIdentifierValue(barcode);
		}
		catch(Exception exception) {
			logger.error("Failed to Page RequestItem " + exception.getLocalizedMessage());
			return setProblemInResponse(new Problem(new ProblemType(Constants.REQUEST_ITEM_PROBLEM),
					Constants.UNKNOWN_DATA_ELEMENT, Constants.REQUEST_ITEM_PROBLEM, exception.getMessage()));
		}
		ItemIdentifierType itemIdentifierType = new ItemIdentifierType(Constants.SCHEME, Constants.ITEM_BARCODE);
		RequestIdentifierType requestIdentifierType = new RequestIdentifierType(Constants.SCHEME,Constants.REQUEST_ID);
		Location location = new Location();
		LocationName locationName = new LocationName();
		locationName.setLocationNameInstances(List.of(locationNameInstance));
		location.setLocationName(locationName);
		ItemOptionalFields itemOptionalFields = new ItemOptionalFields();
		itemOptionalFields.setItemDescription(itemDescription);
		itemOptionalFields.setLocations(List.of(location));
		itemId.setItemIdentifierType(itemIdentifierType);
		ncipRequestId.setRequestIdentifierType(requestIdentifierType);
		RequestItemResponseData requestItemResponseData = new RequestItemResponseData();
		requestItemResponseData.setItemId(itemId);
		requestItemResponseData.setRequestId(ncipRequestId);
		requestItemResponseData.setUserId(userId);
		requestItemResponseData.setItemOptionalFields(itemOptionalFields);
		requestItemResponseData.setRequestType(new RequestType(Constants.SCHEME, Constants.PAGE));
		requestItemResponseData.setRequestScopeType(new RequestScopeType(Constants.SCHEME, Constants.ITEM));
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
