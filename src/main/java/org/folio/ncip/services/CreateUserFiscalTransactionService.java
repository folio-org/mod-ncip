package org.folio.ncip.services;

import org.extensiblecatalog.ncip.v2.service.CreateUserFiscalTransactionInitiationData;
import org.extensiblecatalog.ncip.v2.service.CreateUserFiscalTransactionResponseData;
import org.extensiblecatalog.ncip.v2.service.NCIPService;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.ValidationException;

public interface CreateUserFiscalTransactionService extends NCIPService<CreateUserFiscalTransactionInitiationData, CreateUserFiscalTransactionResponseData> {
    CreateUserFiscalTransactionResponseData performService(CreateUserFiscalTransactionInitiationData initData, ServiceContext serviceContext, RemoteServiceManager remoteServiceManager) throws ServiceException, ValidationException;
}
