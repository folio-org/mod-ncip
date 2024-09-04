package org.folio.ncip.services;


import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.AgencyId;
import org.extensiblecatalog.ncip.v2.service.AgencyUserPrivilegeType;
import org.extensiblecatalog.ncip.v2.service.AuthenticationInput;
import org.extensiblecatalog.ncip.v2.service.ElectronicAddress;
import org.extensiblecatalog.ncip.v2.service.ElectronicAddressType;
import org.extensiblecatalog.ncip.v2.service.LookupUserInitiationData;
import org.extensiblecatalog.ncip.v2.service.LookupUserResponseData;
import org.extensiblecatalog.ncip.v2.service.LookupUserService;
import org.extensiblecatalog.ncip.v2.service.NameInformation;
import org.extensiblecatalog.ncip.v2.service.PersonalNameInformation;
import org.extensiblecatalog.ncip.v2.service.PhysicalAddress;
import org.extensiblecatalog.ncip.v2.service.PhysicalAddressType;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.StructuredAddress;
import org.extensiblecatalog.ncip.v2.service.StructuredPersonalUserName;
import org.extensiblecatalog.ncip.v2.service.UserAddressInformation;
import org.extensiblecatalog.ncip.v2.service.UserAddressRoleType;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.extensiblecatalog.ncip.v2.service.UserIdentifierType;
import org.extensiblecatalog.ncip.v2.service.UserOptionalFields;
import org.extensiblecatalog.ncip.v2.service.UserPrivilege;
import org.extensiblecatalog.ncip.v2.service.UserPrivilegeStatus;
import org.extensiblecatalog.ncip.v2.service.UserPrivilegeStatusType;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioNcipException;
import org.folio.ncip.FolioRemoteServiceManager;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Iterator;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;




public class FolioLookupUserService  extends FolioNcipService  implements LookupUserService  {
	
	 private static final Logger logger = Logger.getLogger(FolioLookupUserService.class);
	 public long reqTimeoutMs;
	 public JsonObject obj;
	 private Properties ncipProperties;
	 
	 
     public LookupUserResponseData performService(LookupUserInitiationData initData,
			 ServiceContext serviceContext,
			 RemoteServiceManager serviceManager) {
		     logger.info("data passed into performService  : ");
		     logger.info(initData.toString());
			 LookupUserResponseData responseData = new LookupUserResponseData();
			 this.ncipProperties = ((FolioRemoteServiceManager)serviceManager).getNcipProperties();
			 
				//ATTEMPT TO DETERMINE AGENCY ID
				// INITIATION HEADER IS NOT REQUIRED
				String requesterAgencyId = null;
				try {
					requesterAgencyId = initData.getInitiationHeader().getFromAgencyId().getAgencyId().getValue();
					if (requesterAgencyId == null || requesterAgencyId.trim().equalsIgnoreCase(""))
						throw new Exception("Agency ID could not be determined");
				} catch (Exception e) {
					logger.error("Could not determine agency id from initiation header.");
					if (responseData.getProblems() == null) responseData.setProblems(new ArrayList<Problem>());
		        	Problem p = new Problem(new ProblemType(Constants.LOOKUP_USER_FAILED),Constants.AGENCY_ID,Constants.FROM_AGENCY_MISSING ,e.getMessage());
		        	responseData.getProblems().add(p);
		        	return responseData;
				}
			 
			UserId userId = retrieveUserId(initData,serviceManager);
			if (userId == null) {
				if (responseData.getProblems() == null) responseData.setProblems(new ArrayList<Problem>());
				Problem p = new Problem(new ProblemType(Constants.LOOKUP_USER_VALIDATION_PROBLEM),Constants.LOOKUP_USER_VALIDATION_PROBLEM,Constants.COULD_NOT_DETERMINE_USER,"");
				responseData.getProblems().add(p);
				return responseData;
			}
			try {
				validateUserId(userId);
			}
			catch(Exception exception) {
				if (responseData.getProblems() == null) responseData.setProblems(new ArrayList<Problem>());
				Problem p = new Problem(new ProblemType(Constants.LOOKUP_USER_VALIDATION_PROBLEM),Constants.LOOKUP_USER_VALIDATION_PROBLEM,exception.getMessage(),exception.getMessage());
				responseData.getProblems().add(p);
				return responseData;
			} 
			try {
				//THE SERVICE MANAGER CALLS THE OKAPI APIs
				 JsonObject patronDetailsAsJson = ((FolioRemoteServiceManager)serviceManager).lookupUser(userId);
				 if (patronDetailsAsJson == null) {
					  ProblemType problemType = new ProblemType("");
					  Problem p = new Problem(problemType,Constants.USERID,Constants.USER_NOT_FOUND);
			    	  responseData.setProblems(new ArrayList<Problem>());
			    	  responseData.getProblems().add(p);
			    	  return responseData;
				 }
				 responseData = constructResponse(initData,patronDetailsAsJson,requesterAgencyId);
				 logger.info("API LOOKUP RESULTS...");
				 logger.info(patronDetailsAsJson.toString());

				 checkPinIfNeeded(initData, (FolioRemoteServiceManager)serviceManager, patronDetailsAsJson.getString("id"));
			 } catch (Exception e) {
				 logger.error("error during performService:");
				 logger.error(e.toString());
				 ProblemType problemType = new ProblemType("");
				 Problem p = new Problem(problemType,Constants.GENERAL_ERROR, Constants.LOOKUP_USER_FAILED + ": " + e.toString());
		    	 responseData.setProblems(new ArrayList<Problem>());
		    	 responseData.getProblems().add(p);
		    	 return responseData;
			}
			 return responseData;
	 }

	 private LookupUserResponseData constructResponse(LookupUserInitiationData initData,JsonObject userDetails,String requesterAgencyId) throws Exception {
		 
		 LookupUserResponseData responseData = new LookupUserResponseData();
		 try {
			 
			  if (responseData.getUserOptionalFields()==null)
		        	responseData.setUserOptionalFields(new UserOptionalFields());
			  
			 if (initData.getNameInformationDesired()) {
				 responseData.getUserOptionalFields().setNameInformation(this.retrieveName(userDetails));
			 }
			 
			 if (initData.getUserIdDesired())
				 responseData.setUserId(this.retrieveBarcode(userDetails,requesterAgencyId));
			 
			  if (initData.getUserAddressInformationDesired())
		        	responseData.getUserOptionalFields().setUserAddressInformations(this.retrieveAddress(userDetails,requesterAgencyId));
			   
			  if (initData.getUserPrivilegeDesired()) {
		        	responseData.getUserOptionalFields().setUserPrivileges(this.retrievePrivileges(userDetails,requesterAgencyId));
			        responseData.getUserOptionalFields().getUserPrivileges().add(this.retrieveBorrowingPrvilege(userDetails,requesterAgencyId));
			  }
		 }
		 catch(Exception e) {
			 logger.error("error during constructing lookup user construct response:");
			 logger.error(e.toString());
			 throw e;
		 }
		 return responseData;
	 }
	 
	   private UserId retrieveBarcode(JsonObject jsonObject,String agencyId) throws Exception {
		   UserId userId = new UserId();
		   String barcode = jsonObject.getString("barcode");
		   userId.setUserIdentifierValue(barcode);
		   userId.setAgencyId(new AgencyId(agencyId));
		   userId.setUserIdentifierType(new UserIdentifierType("barcode"));
		   return userId;
	   }


	    private UserPrivilege retrieveBorrowingPrvilege(JsonObject jsonObject,String agencyId) throws Exception{
	    	UserPrivilege up = new UserPrivilege();
	    	up.setUserPrivilegeDescription("User Status");
	    	up.setAgencyId( new AgencyId(agencyId));
	    	up.setAgencyUserPrivilegeType(new AgencyUserPrivilegeType("","STATUS"));
	    	UserPrivilegeStatus ups = new UserPrivilegeStatus();
	    	String patronBorrowingStatus = checkForBlocks(jsonObject,agencyId);
	    	ups.setUserPrivilegeStatusType(new UserPrivilegeStatusType("",patronBorrowingStatus));
	    	up.setUserPrivilegeStatus(ups);
	    	return up;
	    }
	    
	    
	    private String checkForBlocks(JsonObject jsonObject,String agencyId) throws Exception {
	    	
	    	
	    	String okMessage = getOkMessage(agencyId);
	    	String blockedMessage = getBlockedMessage(agencyId);

	    	try {

	    		//NOTE: - CHECKING FOR BOTH BORROWING BLOCK ~AND~ REQUEST BLOCK
	    		//BECAUSE IF THE PATRON ENDS UP REQUESTING AN ITEM TO BORROW,
	    		//A 'HOLD' WILL BE INVOVLED IN THAT TRANSACTION...AND IF THERE
	    		//IS A REQUEST BLOCK...IT WILL FAIL
		    	JsonArray blocks = jsonObject.getJsonArray("manualblocks");
		    	Iterator  i = blocks.iterator();
		    	while (i.hasNext()) {
		    		JsonObject block = (JsonObject) i.next();
		    		if (block.getBoolean(Constants.BORROWING_BLOCK)!= null && block.getBoolean(Constants.BORROWING_BLOCK)) return blockedMessage;
		    		if (block.getBoolean(Constants.REQUEST_BLOCK) != null && block.getBoolean(Constants.REQUEST_BLOCK)) return blockedMessage;
		    	}
		    	
		    	JsonArray automatedPatronBlocks = jsonObject.getJsonArray("automatedPatronBlocks");
		    	Iterator  automatedPatronBlocksIterator = automatedPatronBlocks.iterator();
		    	while (automatedPatronBlocksIterator.hasNext()) {
		    		JsonObject block = (JsonObject) automatedPatronBlocksIterator.next();
		    		if (block.getBoolean(Constants.AUTOMATED_BORROWING_BLOCK)!= null && block.getBoolean(Constants.AUTOMATED_BORROWING_BLOCK)) return blockedMessage;
		    		if (block.getBoolean(Constants.AUTOMATED_REQUEST_BLOCK) != null && block.getBoolean(Constants.AUTOMATED_REQUEST_BLOCK)) return blockedMessage;
		    	}
		    	
		    	//IS THE PATRON ACTIVE?
		    	if (!jsonObject.getBoolean("active")) return blockedMessage;
		    	
		    	//NO BLOCKS FOUND - RETURN THE okMessage
		    	return okMessage;
			     
	    	}
	    	catch(Exception e) {
	    		logger.error("error during checkRules");
	    		logger.error(e.getLocalizedMessage());
	    		throw new Exception("  Error during checkRules.  Looking at blocks.  " , e);
	    	}
	    }
	    
	    

	 
	    private ArrayList<UserPrivilege> retrievePrivileges(JsonObject jsonObject,String agencyId) {
	    	String patronType = jsonObject.getString("group");
	    	String patronHomeLibrary = jsonObject.getString("code");
	    	ArrayList<UserPrivilege> list = new ArrayList<UserPrivilege>();;
	    	list.add(this.retrievePrivilegeFor(patronType, "User Profile", "PROFILE",agencyId));
	    	list.add(this.retrievePrivilegeFor(patronHomeLibrary,"User Library","LIBRARY",agencyId));
	    	return list;
	    }
	    
	    private UserPrivilege retrievePrivilegeFor(String userPrivilegeStatusTypeString, String descriptionString, String agencyUserPrivilegeTypeString,String agencyId) {
	    	
	    	UserPrivilege up = new UserPrivilege();
	    	up.setUserPrivilegeDescription(descriptionString);
	    	up.setAgencyId( new AgencyId(agencyId));
	    	new AgencyUserPrivilegeType(agencyId, agencyId);
	    	up.setAgencyUserPrivilegeType(new AgencyUserPrivilegeType("",agencyUserPrivilegeTypeString));
	    	UserPrivilegeStatus ups = new UserPrivilegeStatus();
	    	ups.setUserPrivilegeStatusType(new UserPrivilegeStatusType("",userPrivilegeStatusTypeString));
	    	up.setUserPrivilegeStatus(ups);
	    	return up;
	    }
	  
	   private NameInformation retrieveName(JsonObject jsonObject) {
		    JsonObject personal = jsonObject.getJsonObject("personal"); //TODO constants
	    	String firstName = personal.getString("firstName");
	    	String lastName = personal.getString("lastName");
	    	if (personal.getString(Constants.PREFERRED_FIRST_NAME) != null && !personal.getString(Constants.PREFERRED_FIRST_NAME).isEmpty()) 
	    		firstName = personal.getString(Constants.PREFERRED_FIRST_NAME);
	    	NameInformation nameInformation = new NameInformation();
	    	PersonalNameInformation personalNameInformation = new PersonalNameInformation();
	    	StructuredPersonalUserName structuredPersonalUserName = new StructuredPersonalUserName();
	    	structuredPersonalUserName.setGivenName(firstName);
	    	structuredPersonalUserName.setSurname(lastName);
	    	personalNameInformation.setUnstructuredPersonalUserName(firstName + " " + lastName);
	    	personalNameInformation.setStructuredPersonalUserName(structuredPersonalUserName);
	    	nameInformation.setPersonalNameInformation(personalNameInformation);
	    	return nameInformation;
	    }
	  
	  
	   private ArrayList<UserAddressInformation> retrieveAddress(JsonObject jsonObject,String agencyId) {
	    	ArrayList<UserAddressInformation> list = new ArrayList<UserAddressInformation>();
	    	list.add(retrieveEmail(jsonObject,agencyId.toLowerCase()));
	    	list.add(retrieveTelephoneNumber(jsonObject,"phone"));
	    	list.add(retrieveTelephoneNumber(jsonObject,"mobilePhone"));
	    	JsonObject personal = jsonObject.getJsonObject(Constants.PERSONAL);
	    	//DON'T RETURN THE PATRON'S PHYSICAL ADDRESS UNLESS CONFIGURED TO DO SO:
	    	String includePhyscialAddressSetting = "false";
	    	if (ncipProperties != null) {
	    		includePhyscialAddressSetting = ncipProperties.getProperty(agencyId.toLowerCase() + "." + Constants.RESPONSE_INCLUDES_PHYSICAL_ADDRESS);
	    		if (includePhyscialAddressSetting == null) includePhyscialAddressSetting = ncipProperties.getProperty(Constants.RESPONSE_INCLUDES_PHYSICAL_ADDRESS);
	   		}
	    	if (includePhyscialAddressSetting == null || includePhyscialAddressSetting.equalsIgnoreCase("false")) return list;
	    	JsonArray arrayOfAddresses = personal.getJsonArray("addresses");
	    	if (arrayOfAddresses == null || arrayOfAddresses.isEmpty()) return list;
	    	for(int i = 0; i < arrayOfAddresses.size(); i++) {
	    		   JsonObject jsonAddress = arrayOfAddresses.getJsonObject(i);
	    		   list.add(retrievePhysicalAddress(jsonAddress));
	    	}
	    	return list;
	    }
	   
	  
	    private UserAddressInformation retrieveTelephoneNumber(JsonObject jsonObject,String phoneType) {
	    	
		    JsonObject personal = jsonObject.getJsonObject(Constants.PERSONAL); 
	    	String phoneNumber = personal.getString(phoneType);
	    	if (phoneNumber != null) {
	    		ElectronicAddress phone = new ElectronicAddress();
	    		phone.setElectronicAddressData(phoneNumber);
	    		phone.setElectronicAddressType(new ElectronicAddressType("TEL")); 
	    		UserAddressInformation uai = new UserAddressInformation();
	    		uai.setUserAddressRoleType(new UserAddressRoleType(Constants.OTHER));
	    		uai.setElectronicAddress(phone);
	    		return uai;
	    	}
	    	else return null;
	    }
	    
	    private UserAddressInformation retrieveEmail(JsonObject jsonObject, String agencyId) {
	 
	    	String emailString = "electronic mail address";
	    	if (ncipProperties != null) {
	    		String emailStringConfigValue = ncipProperties.getProperty(agencyId.toLowerCase() + "." + Constants.EMAIL_STRING);
	    		if (emailStringConfigValue != null) emailString = emailStringConfigValue;
	   	}

		JsonObject personal = jsonObject.getJsonObject(Constants.PERSONAL); 
	    	String emailAddress = personal.getString("email"); //TODO constants
	    	ElectronicAddress email = new ElectronicAddress();
	    	email.setElectronicAddressData(emailAddress);
	    	email.setElectronicAddressType(new ElectronicAddressType(emailString)); //TODO CONSTANT
	    	UserAddressInformation uai = new UserAddressInformation();
	    	uai.setUserAddressRoleType(new UserAddressRoleType("OTH")); //TODO CONSTANT
	    	uai.setElectronicAddress(email);
	    	return uai;
	    }
	    
	    private UserAddressInformation retrievePhysicalAddress(JsonObject jsonObject) {
	    	
	    	//TODO constants
	    	String streetAddresss = jsonObject.getString("addressLine1");
	    	String city = jsonObject.getString("city");
	    	String addressType = jsonObject.getString("addressTypeId");
	    	String region = jsonObject.getString("region");
	    	String postalCode = jsonObject.getString("postalCode");
	        UserAddressInformation uai = new UserAddressInformation();
	        PhysicalAddress pa = new PhysicalAddress();
	        StructuredAddress sa = new StructuredAddress();
	        sa.setLine1(streetAddresss);
	        sa.setLocality(city);
	        sa.setRegion(region);
	        sa.setPostalCode(postalCode);
	    	pa.setStructuredAddress(sa);
	    	uai.setUserAddressRoleType(new UserAddressRoleType(ncipProperties.getProperty(addressType))); 
	    	pa.setPhysicalAddressType(new PhysicalAddressType(null,"Postal Address")); //TODO
	    	uai.setPhysicalAddress(pa);
	    	return uai;
	    }
	    
		 private String retrieveAuthenticationInputTypeOf(LookupUserInitiationData initData,RemoteServiceManager serviceManager) {
		    	if (initData.getAuthenticationInputs() == null) return null;
		    	String barcode = null;
		    	for (AuthenticationInput authenticationInput : initData.getAuthenticationInputs()) {
		    		
		    		String authType = authenticationInput.getAuthenticationInputType().getValue();
		    		String authValue = authenticationInput.getAuthenticationInputData();
		    		
		    		try {
						JsonObject patronDetailsAsJson = ((FolioRemoteServiceManager)serviceManager).lookupPatronRecordBy(authType,authValue);
						barcode = patronDetailsAsJson.getString("barcode");
						if (barcode != null && !barcode.equalsIgnoreCase("")) return barcode;
					} catch (Exception e) {
						logger.error("unable to get barcode value from input");
					}
		    		
		    	}
		    	if (barcode != null && barcode.equalsIgnoreCase("")) barcode = null;
		    	return barcode;
		 }
		 
		private UserId retrieveUserId(LookupUserInitiationData initData,RemoteServiceManager serviceManager) {
		    	UserId uid = null;
		    	String uidString = null;
		    	if (initData.getUserId() != null) {
		    		uid = initData.getUserId();
		    	}
		    	else {
		    		//try AuthenticationInput:
		    		uidString = this.retrieveAuthenticationInputTypeOf(initData, serviceManager);
		    	}
		    	if (uidString != null) {
		    		uid = new UserId();
		    		uid.setUserIdentifierValue(uidString);
		    	}
		    	return uid;
		}
		
		private String getBlockedMessage(String agencyId) {
				if (ncipProperties != null && ncipProperties.getProperty(agencyId.toLowerCase() + "." + Constants.BLOCKED_CONFIG) != null) return ncipProperties.getProperty(agencyId.toLowerCase() + "." + Constants.BLOCKED_CONFIG);
				if (ncipProperties != null && ncipProperties.getProperty(Constants.BLOCKED_CONFIG) != null) return ncipProperties.getProperty(Constants.BLOCKED_CONFIG);
				return Constants.BLOCKED;
		}
		
		private String getOkMessage(String agencyId) {
			if (ncipProperties != null && ncipProperties.getProperty(agencyId.toLowerCase() + "." + Constants.OK_CONFIG) != null) return ncipProperties.getProperty(agencyId.toLowerCase() + "." + Constants.OK_CONFIG);
			if (ncipProperties != null && ncipProperties.getProperty(Constants.OK_CONFIG) != null) return ncipProperties.getProperty(Constants.OK_CONFIG);
			return Constants.ACTIVE;
		}

		private void checkPinIfNeeded(LookupUserInitiationData initData, FolioRemoteServiceManager serviceManager,
									  String userId) throws FolioNcipException {
			if (initData.getAuthenticationInputs() != null) {
				for (AuthenticationInput authenticationInput : initData.getAuthenticationInputs()) {
					String authType = authenticationInput.getAuthenticationInputType().getValue();
					if (Constants.AUTH_TYPE_PIN.equalsIgnoreCase(authType)) {
						String authValue = authenticationInput.getAuthenticationInputData();
						serviceManager.checkUserPin(userId, authValue);
					}
				}
			}
		}
}
