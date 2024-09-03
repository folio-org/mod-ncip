package org.folio.ncip.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.extensiblecatalog.ncip.v2.service.AgencyId;
import org.extensiblecatalog.ncip.v2.service.BibliographicDescription;
import org.extensiblecatalog.ncip.v2.service.CheckInItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.CheckInItemResponseData;
import org.extensiblecatalog.ncip.v2.service.CheckInItemService;
import org.extensiblecatalog.ncip.v2.service.ItemDescription;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.ItemIdentifierType;
import org.extensiblecatalog.ncip.v2.service.ItemOptionalFields;
import org.extensiblecatalog.ncip.v2.service.Problem;
import org.extensiblecatalog.ncip.v2.service.ProblemType;
import org.extensiblecatalog.ncip.v2.service.RemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.extensiblecatalog.ncip.v2.service.UserIdentifierType;
import org.extensiblecatalog.ncip.v2.service.UserOptionalFields;
import org.extensiblecatalog.ncip.v2.service.Version2ItemIdentifierType;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioRemoteServiceManager;

import io.vertx.core.json.JsonObject;

public class FolioCheckInItemService extends FolioNcipService implements CheckInItemService {

	private static final Logger logger = Logger.getLogger(FolioCheckInItemService.class);
	

	@Override
	public CheckInItemResponseData performService(CheckInItemInitiationData initData, ServiceContext serviceContext,
			RemoteServiceManager serviceManager) {

		CheckInItemResponseData checkInItemResponseData = new CheckInItemResponseData();
		ItemId itemId = initData.getItemId();
		logger.info("checking in " + itemId);

		try {
			validateItemId(itemId);
		} catch (Exception exception) {
			if (checkInItemResponseData.getProblems() == null)
				checkInItemResponseData.setProblems(new ArrayList<Problem>());
			Problem p = new Problem(new ProblemType(Constants.CHECK_IN_PROBLEM), Constants.CHECK_IN_PROBLEM,
					exception.getMessage(), Constants.CHECK_IN_PROBLEM);
			checkInItemResponseData.getProblems().add(p);
			logger.error("item validation failed");
			return checkInItemResponseData;
		}

		//ATTEMPT TO DETERMINE AGENCY ID
		// INITIATION HEADER IS NOT REQUIRED
		String requesterAgencyId = null;
		try {
			requesterAgencyId = initData.getInitiationHeader().getFromAgencyId().getAgencyId().getValue();
			if (requesterAgencyId == null || requesterAgencyId.trim().equalsIgnoreCase(""))
				throw new Exception("Agency ID could nto be determined");
		} catch (Exception e) {
			logger.error("Could not determine agency id from initiation header.");
			if (checkInItemResponseData.getProblems() == null) checkInItemResponseData.setProblems(new ArrayList<Problem>());
        	Problem p = new Problem(new ProblemType(Constants.CHECK_IN_PROBLEM),Constants.AGENCY_ID,Constants.FROM_AGENCY_MISSING ,e.getMessage());
        	checkInItemResponseData.getProblems().add(p);
        	return checkInItemResponseData;
		}
		

		try {
			//THE SERVICE MANAGER CALLS THE OKAPI APIs
			JsonObject checkInResponseDetails = ((FolioRemoteServiceManager) serviceManager).checkIn(initData,
					requesterAgencyId.toLowerCase());
			// construct ItemId
			ItemIdentifierType idemIdentiferType = new ItemIdentifierType("Scheme", "Item Barcode");
			ItemId iId = new ItemId();
			iId.setAgencyId(new AgencyId(requesterAgencyId));
			iId.setItemIdentifierType(idemIdentiferType);
			iId.setItemIdentifierValue(itemId.getItemIdentifierValue());

			//CONSTRUCT "User Id" ELEMENT
			//IF THIS LOAN WAS ALREADY CHECKED IN, THE API DOES
			//NOT RETURN A "loan" AS PART OF THE RESPONSE
			if (checkInResponseDetails.getJsonObject("loan") != null) {
					
				UserIdentifierType userIdentiferType = new UserIdentifierType("Scheme", "User Barcode");
				UserId uId = new UserId();
				uId.setAgencyId(new AgencyId(requesterAgencyId));
				uId.setUserIdentifierType(userIdentiferType);
				uId.setUserIdentifierValue(
						checkInResponseDetails.getJsonObject("loan").getJsonObject("borrower").getString("barcode"));
				checkInItemResponseData.setUserId(uId);
				String loanUuid = checkInResponseDetails.getJsonObject("loan").getString("id");
				String userUuid = checkInResponseDetails.getJsonObject("loan").getString("userId");

				ItemId loanId = new ItemId();
				loanId.setItemIdentifierValue(loanUuid);
				loanId.setItemIdentifierType(Version2ItemIdentifierType.UUID);
				checkInItemResponseData.setLoanUuid(loanId);

				UserId userUuidObject = new UserId();
				userUuidObject.setUserIdentifierValue(userUuid);
				userUuidObject.setUserIdentifierType(new UserIdentifierType(Constants.SCHEME, "userUuid"));
				checkInItemResponseData.setUserUuid(userUuidObject);
			}
			
			checkInItemResponseData.setItemId(iId);
			
			if (checkInItemResponseData.getItemOptionalFields() == null)
				checkInItemResponseData.setItemOptionalFields(new ItemOptionalFields());
			
			if (initData.getBibliographicDescriptionDesired())
				checkInItemResponseData.getItemOptionalFields()
						.setBibliographicDescription(this.retrieveBiblioDescription(checkInResponseDetails));
			
			if (initData.getItemDescriptionDesired())
				checkInItemResponseData.getItemOptionalFields()
						.setItemDescription(this.retreiveItemDescription(checkInResponseDetails));

		} catch (Exception exception) {
			if (checkInItemResponseData.getProblems() == null)
				checkInItemResponseData.setProblems(new ArrayList<Problem>());
			Problem p = new Problem(new ProblemType(Constants.CHECK_IN_PROBLEM), Constants.UNKNOWN_DATA_ELEMENT,
					Constants.CHECK_IN_PROBLEM, exception.getMessage());
			checkInItemResponseData.getProblems().add(p);
			return checkInItemResponseData;
		}
		return checkInItemResponseData;
	}

	// TODO is call number necessary? - i don't see it in the FOLIO check in response
	//WOULD INVOVLE ANOTHER API CALL
	private ItemDescription retreiveItemDescription(JsonObject checkInTrans) {
		ItemDescription itemDescription = new ItemDescription();
		// itemDescription.setCallNumber(checkInTrans.getCallNumber());
		return itemDescription;
	}

	private BibliographicDescription retrieveBiblioDescription(JsonObject checkInTrans) {
		BibliographicDescription biblioDescription = new BibliographicDescription();
		biblioDescription.setAuthor(checkInTrans.getJsonObject("item").getString("primaryContributor"));
		biblioDescription.setTitle(checkInTrans.getJsonObject("item").getString("title"));
		return biblioDescription;
	}

}
