package org.folio.ncip.services;

import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.DeleteItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.DeleteItemResponseData;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.ValidationException;
import org.extensiblecatalog.ncip.v2.service.DeleteItemService;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioRemoteServiceManager;

import java.util.ArrayList;

public class FolioDeleteItemService extends FolioNcipService implements DeleteItemService {

    private static final Logger LOGGER = Logger.getLogger(FolioDeleteItemService.class);

    @Override
    public DeleteItemResponseData performService(DeleteItemInitiationData initData, ServiceContext context, RemoteServiceManager serviceManager) throws ServiceException, ValidationException {
        DeleteItemResponseData responseData = new DeleteItemResponseData();
        ItemId itemId = initData.getItemId();

        try {
            validateItemId(itemId);
        } catch (Exception exception) {
            Problem problem = new Problem(new ProblemType(Constants.DELETE_ITEM_PROBLEM), Constants.DELETE_ITEM_PROBLEM,
                    exception.getMessage());
            return addProblem(responseData, problem);
        }

        String requesterAgencyId;
        try {
            requesterAgencyId = initData.getInitiationHeader().getFromAgencyId().getAgencyId().getValue();
            if (requesterAgencyId == null || requesterAgencyId.trim().equalsIgnoreCase("")) {
                throw new Exception("Agency ID could nto be determined");
            }
        } catch (Exception e) {
            LOGGER.error("Could not determine agency id from initiation header.");
            Problem problem = new Problem(new ProblemType(Constants.DELETE_ITEM_PROBLEM), Constants.AGENCY_ID,
                    Constants.FROM_AGENCY_MISSING, e.getMessage());
            return addProblem(responseData, problem);
        }

        try {
            ((FolioRemoteServiceManager)serviceManager).deleteItem(itemId.getItemIdentifierValue(), requesterAgencyId.toLowerCase());
        } catch(Exception  e) {
            Problem problem = new Problem(new ProblemType(Constants.DELETE_ITEM_PROBLEM), Constants.UNKNOWN_DATA_ELEMENT,
                    Constants.DELETE_ITEM_PROBLEM, e.getMessage());
            return addProblem(responseData, problem);
        }

        responseData.setItemId(itemId);
        return responseData;
    }

    private DeleteItemResponseData addProblem(DeleteItemResponseData responseData, Problem problem){
        if (responseData.getProblems() == null) {
            responseData.setProblems(new ArrayList<>());
        }
        responseData.getProblems().add(problem);
        return responseData;
    }
}
