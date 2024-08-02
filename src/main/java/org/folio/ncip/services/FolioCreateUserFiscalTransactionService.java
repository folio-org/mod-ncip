package org.folio.ncip.services;

import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.CreateUserFiscalTransactionInitiationData;
import org.extensiblecatalog.ncip.v2.service.CreateUserFiscalTransactionResponseData;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.extensiblecatalog.ncip.v2.service.ValidationException;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioRemoteServiceManager;

import java.util.ArrayList;

public class FolioCreateUserFiscalTransactionService extends FolioNcipService implements CreateUserFiscalTransactionService {
    private static final Logger LOGGER = Logger.getLogger(FolioCreateUserFiscalTransactionService.class);

    @Override
    public CreateUserFiscalTransactionResponseData performService(CreateUserFiscalTransactionInitiationData initData, ServiceContext serviceContext, RemoteServiceManager remoteServiceManager) throws ServiceException, ValidationException {
        CreateUserFiscalTransactionResponseData responseData = new CreateUserFiscalTransactionResponseData();
        UserId userId = initData.getUserId();
        try {
            validateUserId(userId);
            initData.getFiscalTransactionInformation().getFiscalActionType();
        } catch (Exception exception) {
            Problem problem = new Problem(new ProblemType(Constants.CREATE_USER_FISCAL_TRANSACTION_PROBLEM), Constants.CREATE_USER_FISCAL_TRANSACTION_PROBLEM,
                    exception.getMessage());
            LOGGER.error("Request validation failed");
            return addProblem(responseData, problem);
        }

        try {
            ((FolioRemoteServiceManager)remoteServiceManager).createUserFiscalTransaction(userId, initData.getFiscalTransactionInformation());
        } catch(Exception  e) {
            Problem problem = new Problem(new ProblemType(Constants.CREATE_USER_FISCAL_TRANSACTION_PROBLEM), Constants.UNKNOWN_DATA_ELEMENT,
                    Constants.CREATE_USER_FISCAL_TRANSACTION_PROBLEM, e.getMessage());
            return addProblem(responseData, problem);
        }
        responseData.setUserId(userId);
        return responseData;
    }

    private CreateUserFiscalTransactionResponseData addProblem(CreateUserFiscalTransactionResponseData responseData, Problem problem){
        if (responseData.getProblems() == null) {
            responseData.setProblems(new ArrayList<>());
        }
        responseData.getProblems().add(problem);
        return responseData;
    }
}
