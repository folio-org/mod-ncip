package org.folio.ncip.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import org.extensiblecatalog.ncip.v2.service.AcceptItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.AcceptItemResponseData;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.PickupLocation;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.junit.Test;

public class AcceptItemServiceTests {

	@Test
	public void testValidatePickupLocation() throws Exception {
		FolioAcceptItemService folioAcceptItemService = new FolioAcceptItemService();
		Method validatePickupLocation = folioAcceptItemService.getClass()
				.getDeclaredMethod("validatePickupLocation", String.class);
		validatePickupLocation.setAccessible(true);
		try {
			validatePickupLocation.invoke(folioAcceptItemService, "");
			fail("expected exception was not occured.");
		} catch (Exception e) {
			// expected exception
		}
	}

	@Test
	public void performServiceReturnsProblemWhenPickupLocationMissing() {
		FolioAcceptItemService service = new FolioAcceptItemService();
		AcceptItemInitiationData initData = buildAcceptItemInitiationData("item-1", "user-1", "");

		AcceptItemResponseData response = service.performService(initData, null, null);

		assertNotNull(response.getProblems());
		assertTrue(!response.getProblems().isEmpty());
	}

	@Test
	public void performServiceReturnsProblemWhenAgencyIdCannotBeDetermined() {
		FolioAcceptItemService service = new FolioAcceptItemService();
		AcceptItemInitiationData initData = buildAcceptItemInitiationData("item-1", "user-1", "pickup");
		initData.setInitiationHeader(null);
		initData.setRequestId(null);

		AcceptItemResponseData response = service.performService(initData, null, null);

		assertNotNull(response.getProblems());
		assertTrue(!response.getProblems().isEmpty());
	}

	private AcceptItemInitiationData buildAcceptItemInitiationData(String itemIdentifier, String userIdentifier,
			String pickupLocationValue) {
		AcceptItemInitiationData initData = new AcceptItemInitiationData();

		ItemId itemId = new ItemId();
		itemId.setItemIdentifierValue(itemIdentifier);
		initData.setItemId(itemId);

		UserId userId = new UserId();
		userId.setUserIdentifierValue(userIdentifier);
		initData.setUserId(userId);

		PickupLocation pickupLocation = new PickupLocation(pickupLocationValue);
		initData.setPickupLocation(pickupLocation);

		return initData;
	}
}
