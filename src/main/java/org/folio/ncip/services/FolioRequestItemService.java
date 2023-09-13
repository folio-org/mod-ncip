package org.folio.ncip.services;


import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.RequestItemResponseData;
import org.extensiblecatalog.ncip.v2.service.RequestItemService;
import org.extensiblecatalog.ncip.v2.service.AuthenticationInput;
import org.extensiblecatalog.ncip.v2.service.CheckOutItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioRemoteServiceManager;

import io.vertx.core.json.JsonObject;




public class FolioRequestItemService extends FolioNcipService implements RequestItemService {

	private static final Logger logger = Logger.getLogger(FolioRequestItemService.class);
	

	@Override
	public RequestItemResponseData performService(RequestItemInitiationData initData, ServiceContext serviceContext,
			RemoteServiceManager serviceManager) {
		UserId userId = retrieveUserId(initData);
        try {
        	validateUserId(userId);
        }
        catch(Exception exception) {
        	//JUST A PLACEHOLDER SERVICE FOR NOW
        } 
		logger.info("RequestItemService");
		RequestItemResponseData requestItemResponseData = new RequestItemResponseData();
		return requestItemResponseData;
	}
	
	 private String retrieveAuthenticationInputTypeOf(String type,RequestItemInitiationData initData) {
	    	if (initData.getAuthenticationInputs() == null) return null;
	    	String authenticationID = null;
	    	for (AuthenticationInput authenticationInput : initData.getAuthenticationInputs()) {
	    		if (authenticationInput.getAuthenticationInputType().getValue().equalsIgnoreCase(type)) {
	    			authenticationID = authenticationInput.getAuthenticationInputData();
	    			break;
	    		}
	    	}
	    	if (authenticationID != null && authenticationID.equalsIgnoreCase("")) authenticationID = null;
	    	return authenticationID;
	 }
	 
	private UserId retrieveUserId(RequestItemInitiationData initData) {
	    	UserId uid = null;
	    	String uidString = null;
	    	if (initData.getUserId() != null) {
	    		uid = initData.getUserId();
	    	}
	    	else {
	    		//TRY Barcode Id
	    		uidString = this.retrieveAuthenticationInputTypeOf(Constants.AUTH_BARCODE,initData);
	    		//TRY User Id
	    		if (uidString == null) {
	    			uidString = this.retrieveAuthenticationInputTypeOf(Constants.AUTH_UID, initData);
	    		}
	    	}
	    	if (uidString != null) {
	    		uid = new UserId();
	    		uid.setUserIdentifierValue(uidString);
	    	}
	    	return uid;
	}


}
