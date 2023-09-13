package org.folio.ncip.services;


import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.RequestItemResponseData;
import org.extensiblecatalog.ncip.v2.service.RequestItemService;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;




public class FolioRequestItemService extends FolioNcipService implements RequestItemService {

	private static final Logger logger = Logger.getLogger(FolioRequestItemService.class);
	

	@Override
	public RequestItemResponseData performService(RequestItemInitiationData initData, ServiceContext serviceContext,
			RemoteServiceManager serviceManager) {

		logger.info("RequestItemService called");
		RequestItemResponseData requestItemResponseData = new RequestItemResponseData();
		return requestItemResponseData;
	}


}
