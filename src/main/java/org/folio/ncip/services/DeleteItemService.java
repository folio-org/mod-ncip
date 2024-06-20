package org.folio.ncip.services;

import org.extensiblecatalog.ncip.v2.service.DeleteItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.DeleteItemResponseData;
import org.extensiblecatalog.ncip.v2.service.NCIPService;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.ServiceException;
import org.extensiblecatalog.ncip.v2.service.ValidationException;

public interface DeleteItemService extends NCIPService<DeleteItemInitiationData, DeleteItemResponseData> {
    DeleteItemResponseData performService(DeleteItemInitiationData initData, ServiceContext context, RemoteServiceManager serviceManager) throws ServiceException, ValidationException;
}
