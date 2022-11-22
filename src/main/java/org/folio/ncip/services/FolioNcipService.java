package org.folio.ncip.services;

import java.util.regex.Pattern;

import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.ncip.Constants;
import org.folio.ncip.FolioNcipException;

public class FolioNcipService {

	protected void validateUserId(UserId userId) throws FolioNcipException {

		this.validateUserIdIsPresent(userId);
		//REMOVED 11/22/2022
		//this.validateUserIdIsValid(userId);

	}

	protected void validateItemId(ItemId itemId) throws Exception {

		this.validateItemIdIsPresent(itemId);
		this.validateItemIdIsValid(itemId);
	}

	protected void validateUserIdIsPresent(UserId userId) throws FolioNcipException {

		if (userId == null || userId.getUserIdentifierValue() == null) {
			FolioNcipException exception = new FolioNcipException(Constants.USER_ID_MISSING);
			throw exception;
		}

	}

	protected void validateUserIdIsValid(UserId userId) throws FolioNcipException {

		final Pattern idPattern = Pattern.compile("^[a-zA-Z0-9\\s\\.\\-_]+$");
		if (!idPattern.matcher(userId.getUserIdentifierValue()).matches()
				|| userId.getUserIdentifierValue().length() > 100) {
			FolioNcipException exception = new FolioNcipException("User id is invalid");
			throw exception;
		}
	}

	protected void validateItemIdIsPresent(ItemId itemId) throws FolioNcipException {

		if (itemId == null || itemId.getItemIdentifierValue() == null) {
			FolioNcipException exception = new FolioNcipException("Item id missing");
			throw exception;
		}

	}

	protected void validateItemIdIsValid(ItemId itemId) throws FolioNcipException {

		final Pattern idPattern = Pattern.compile("^[a-zA-Z0-9\\s\\.\\-_]+$");
		if (!idPattern.matcher(itemId.getItemIdentifierValue()).matches()
				|| itemId.getItemIdentifierValue().length() > 100) {
			FolioNcipException exception = new FolioNcipException("Item id is invalid");
			throw exception;
		}
	}
}
