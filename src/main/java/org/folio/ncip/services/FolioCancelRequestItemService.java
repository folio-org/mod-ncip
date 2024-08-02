package org.folio.ncip.services;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.CancelRequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.CancelRequestItemResponseData;
import org.extensiblecatalog.ncip.v2.service.CancelRequestItemService;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.RequestId;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioRemoteServiceManager;

import java.util.ArrayList;

public class FolioCancelRequestItemService extends FolioNcipService implements CancelRequestItemService {

    private static final Logger LOGGER = Logger.getLogger(FolioCancelRequestItemService.class);

    @Override
    public CancelRequestItemResponseData performService(CancelRequestItemInitiationData cancelRequestItemInitiationData,
                                                        ServiceContext serviceContext,
                                                        RemoteServiceManager remoteServiceManager) throws ServiceException {
        CancelRequestItemResponseData responseData = new CancelRequestItemResponseData();
        RequestId requestId = cancelRequestItemInitiationData.getRequestId();
        UserId userId = cancelRequestItemInitiationData.getUserId();
        LOGGER.info("Cancel request " + requestId);

        try {
            validateUserId(userId);
            validateRequestIdIsPresent(requestId);
        } catch (Exception exception) {
            Problem problem = new Problem(new ProblemType(Constants.CANCEL_REQUEST_ITEM_PROBLEM), Constants.CANCEL_REQUEST_ITEM_PROBLEM,
                    exception.getMessage());
            LOGGER.error("Request validation failed");
            return addProblem(responseData, problem);
        }

        String requesterAgencyId;
        try {
            requesterAgencyId = cancelRequestItemInitiationData.getInitiationHeader().getFromAgencyId().getAgencyId().getValue();
            if (requesterAgencyId == null || requesterAgencyId.trim().equalsIgnoreCase("")) {
                throw new Exception("Agency ID could nto be determined");
            }
        } catch (Exception e) {
            LOGGER.error("Could not determine agency id from initiation header.");
            Problem problem = new Problem(new ProblemType(Constants.CANCEL_REQUEST_ITEM_PROBLEM), Constants.AGENCY_ID,
                    Constants.FROM_AGENCY_MISSING, e.getMessage());
            return addProblem(responseData, problem);
        }

        String itemIdString = null;
        try {
            JsonObject cancelResponse =((FolioRemoteServiceManager)remoteServiceManager)
                    .cancelRequestItem(requestId.getRequestIdentifierValue(), userId, requesterAgencyId.toLowerCase());
            JsonObject item = cancelResponse.getJsonObject("item");
            if (item != null) {
                itemIdString = item.getString("barcode");
            } else {
                itemIdString = cancelResponse.getString("itemId");
            }
        } catch(Exception  e) {
            Problem problem = new Problem(new ProblemType(Constants.CANCEL_REQUEST_ITEM_PROBLEM), Constants.UNKNOWN_DATA_ELEMENT,
                    Constants.CANCEL_REQUEST_ITEM_PROBLEM, e.getMessage());
            return addProblem(responseData, problem);
        }

        ItemId itemId = new ItemId();
        itemId.setItemIdentifierValue(itemIdString);
        responseData.setRequestId(requestId);
        responseData.setUserId(userId);
        responseData.setItemId(itemId);
        return responseData;
    }

    private CancelRequestItemResponseData addProblem(CancelRequestItemResponseData responseData, Problem problem){
        if (responseData.getProblems() == null) {
            responseData.setProblems(new ArrayList<>());
        }
        responseData.getProblems().add(problem);
        return responseData;
    }
}
