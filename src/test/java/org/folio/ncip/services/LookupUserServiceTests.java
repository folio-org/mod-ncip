package org.folio.ncip.services;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import org.extensiblecatalog.ncip.v2.service.AuthenticationInput;
import org.extensiblecatalog.ncip.v2.service.AuthenticationInputType;
import org.extensiblecatalog.ncip.v2.service.FromAgencyId;
import org.extensiblecatalog.ncip.v2.service.InitiationHeader;
import org.extensiblecatalog.ncip.v2.service.LookupUserInitiationData;
import org.extensiblecatalog.ncip.v2.service.LookupUserResponseData;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.ncip.FolioRemoteServiceManager;
import org.extensiblecatalog.ncip.v2.service.NameInformation;
import org.extensiblecatalog.ncip.v2.service.UserAddressInformation;
import org.folio.ncip.services.FolioLookupUserService;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.*;

public class LookupUserServiceTests {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

	}

	@Test
	public void testRetrievePhysicalAddress() throws Exception {

		FolioLookupUserService folioLookupUserService = new FolioLookupUserService();

		Properties p = new Properties();
		p.setProperty("key-to-home-address", "Home");
		p.setProperty("key-to-campus-address", "Campus");
		Method retrievePhysicalAddressMethod = folioLookupUserService.getClass()
				.getDeclaredMethod("retrievePhysicalAddress", JsonObject.class);
		retrievePhysicalAddressMethod.setAccessible(true);
		Field ncipProperties = folioLookupUserService.getClass().getDeclaredField("ncipProperties");
		ncipProperties.setAccessible(true);
		ncipProperties.set(folioLookupUserService, p);

		JsonObject jsonObject = new JsonObject();
		jsonObject.put("addressLine1", "1 Main Street");
		jsonObject.put("region", "PA");
		jsonObject.put("postalCode", "18038");
		jsonObject.put("addressTypeId", "key-to-home-address");
		UserAddressInformation userAddressInformation = (UserAddressInformation) retrievePhysicalAddressMethod
				.invoke(folioLookupUserService, jsonObject);
		assertEquals(userAddressInformation.getPhysicalAddress().getStructuredAddress().getLine1(), "1 Main Street");
		assertEquals(userAddressInformation.getPhysicalAddress().getStructuredAddress().getLocality(), null);

	}

	@Test
	public void testRetrieveTelephoneNumber() throws Exception {
		FolioLookupUserService folioLookupUserService = new FolioLookupUserService();
		Method retrieveTelephoneNumber = folioLookupUserService.getClass().getDeclaredMethod("retrieveTelephoneNumber",
				JsonObject.class, String.class);
		retrieveTelephoneNumber.setAccessible(true);
		JsonObject personal = new JsonObject();
		JsonObject telephone = new JsonObject();
		telephone.put("home", "5551212");
		personal.put("personal", telephone);
		UserAddressInformation telephoneInfo = (UserAddressInformation) retrieveTelephoneNumber
				.invoke(folioLookupUserService, personal, "home");
		assertEquals(telephoneInfo.getElectronicAddress().getElectronicAddressData(), "5551212");

	}

	@Test
	public void testRetrieveName() throws Exception {
		FolioLookupUserService folioLookupUserService = new FolioLookupUserService();
		Method retrieveNameMethod = folioLookupUserService.getClass().getDeclaredMethod("retrieveName",
				JsonObject.class);
		retrieveNameMethod.setAccessible(true);
		JsonObject personal = new JsonObject();
		JsonObject name = new JsonObject();
		name.put("firstName", "John");
		name.put("lastName", "Doe");
		personal.put("personal", name);
		NameInformation nameInfo = (NameInformation) retrieveNameMethod.invoke(folioLookupUserService, personal);
		assertEquals(nameInfo.getPersonalNameInformation().getStructuredPersonalUserName().getGivenName(), "John");
		assertEquals(nameInfo.getPersonalNameInformation().getStructuredPersonalUserName().getSurname(), "Doe");

	}

	@Test
	public void retrieveAuthenticationInputTypeOfReturnsBarcodeFromPatronLookup() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		Method method = service.getClass().getDeclaredMethod("retrieveAuthenticationInputTypeOf",
				LookupUserInitiationData.class,
				org.extensiblecatalog.ncip.v2.service.RemoteServiceManager.class);
		method.setAccessible(true);

		LookupUserInitiationData initData = new LookupUserInitiationData();
		AuthenticationInput input = new AuthenticationInput();
		input.setAuthenticationInputType(new AuthenticationInputType(null, "barcode"));
		input.setAuthenticationInputData("value-1");
		initData.setAuthenticationInputs(java.util.List.of(input));

		FolioRemoteServiceManager serviceManager = new FolioRemoteServiceManager() {
			@Override
			public JsonObject lookupPatronRecordBy(String type, String value) {
				return new JsonObject().put("barcode", "123456");
			}
		};

		String resolved = (String) method.invoke(service, initData, serviceManager);
		assertEquals("123456", resolved);
	}

	@Test
	public void retrieveAuthenticationInputTypeOfReturnsNullWhenNoInputsPresent() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		Method method = service.getClass().getDeclaredMethod("retrieveAuthenticationInputTypeOf",
				LookupUserInitiationData.class,
				org.extensiblecatalog.ncip.v2.service.RemoteServiceManager.class);
		method.setAccessible(true);

		LookupUserInitiationData initData = new LookupUserInitiationData();
		String resolved = (String) method.invoke(service, initData, new FolioRemoteServiceManager());

		assertNull(resolved);
	}

	@Test
	public void performServiceReturnsProblemWhenAgencyHeaderMissing() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		LookupUserInitiationData initData = mock(LookupUserInitiationData.class);
		when(initData.toString()).thenReturn("lookup-request");

		FolioRemoteServiceManager manager = mock(FolioRemoteServiceManager.class);
		when(manager.getNcipProperties()).thenReturn(new Properties());

		LookupUserResponseData response = service.performService(initData, null, manager);
		assertNotNull(response.getProblems());
		assertTrue(!response.getProblems().isEmpty());
	}

	@Test
	public void performServiceReturnsProblemWhenUserCannotBeDetermined() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		LookupUserInitiationData initData = initDataWithAgency("relais");
		when(initData.getUserId()).thenReturn(null);
		when(initData.getAuthenticationInputs()).thenReturn(null);

		FolioRemoteServiceManager manager = mock(FolioRemoteServiceManager.class);
		when(manager.getNcipProperties()).thenReturn(new Properties());

		LookupUserResponseData response = service.performService(initData, null, manager);
		assertNotNull(response.getProblems());
		assertTrue(!response.getProblems().isEmpty());
	}

	@Test
	public void performServiceReturnsNotFoundProblemWhenManagerReturnsNullUser() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		LookupUserInitiationData initData = initDataWithAgency("relais");
		UserId userId = new UserId();
		userId.setUserIdentifierValue("user-1");
		when(initData.getUserId()).thenReturn(userId);

		FolioRemoteServiceManager manager = mock(FolioRemoteServiceManager.class);
		when(manager.getNcipProperties()).thenReturn(new Properties());
		when(manager.lookupUser(userId)).thenReturn(null);

		LookupUserResponseData response = service.performService(initData, null, manager);
		assertNotNull(response.getProblems());
		assertTrue(!response.getProblems().isEmpty());
	}

	@Test
	public void performServiceReturnsResponseWhenManagerReturnsPatronDetails() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		LookupUserInitiationData initData = initDataWithAgency("relais");
		UserId userId = new UserId();
		userId.setUserIdentifierValue("user-1");
		when(initData.getUserId()).thenReturn(userId);

		FolioRemoteServiceManager manager = mock(FolioRemoteServiceManager.class);
		when(manager.getNcipProperties()).thenReturn(new Properties());
		when(manager.lookupUser(userId)).thenReturn(new JsonObject()
				.put("id", "uuid-1")
				.put("userUuid", "uuid-1")
				.put("personal", new JsonObject().put("firstName", "First").put("lastName", "Last"))
				.put("manualblocks", new io.vertx.core.json.JsonArray())
				.put("automatedPatronBlocks", new io.vertx.core.json.JsonArray())
				.put("active", true));

		LookupUserResponseData response = service.performService(initData, null, manager);
		assertNotNull(response);
		assertNull(response.getProblems());
	}

	@Test
	public void checkForBlocksReturnsBlockedWhenManualBlockPresent() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		Method checkForBlocks = service.getClass().getDeclaredMethod("checkForBlocks", JsonObject.class,
				String.class);
		checkForBlocks.setAccessible(true);

		JsonObject user = new JsonObject()
				.put("manualblocks", new io.vertx.core.json.JsonArray()
						.add(new JsonObject().put("borrowing", true).put("requests", false)))
				.put("automatedPatronBlocks", new io.vertx.core.json.JsonArray())
				.put("active", true);

		String status = (String) checkForBlocks.invoke(service, user, "relais");
		assertEquals("BLOCKED", status);
	}

	@Test
	public void checkForBlocksReturnsActiveWhenNoBlocksAndActiveUser() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		Method checkForBlocks = service.getClass().getDeclaredMethod("checkForBlocks", JsonObject.class,
				String.class);
		checkForBlocks.setAccessible(true);

		JsonObject user = new JsonObject()
				.put("manualblocks", new io.vertx.core.json.JsonArray())
				.put("automatedPatronBlocks", new io.vertx.core.json.JsonArray())
				.put("active", true);

		String status = (String) checkForBlocks.invoke(service, user, "relais");
		assertEquals("ACTIVE", status);
	}

	@Test
	public void retrieveAddressIncludesPhysicalWhenConfiguredTrue() throws Exception {
		FolioLookupUserService service = new FolioLookupUserService();
		Field ncipProperties = service.getClass().getDeclaredField("ncipProperties");
		ncipProperties.setAccessible(true);
		Properties p = new Properties();
		p.setProperty("relais.response.includes.physical.address", "true");
		p.setProperty("home", "Home");
		ncipProperties.set(service, p);

		Method retrieveAddress = service.getClass().getDeclaredMethod("retrieveAddress", JsonObject.class,
				String.class);
		retrieveAddress.setAccessible(true);

		JsonObject personal = new JsonObject()
				.put("email", "x@y.org")
				.put("phone", "5551111")
				.put("mobilePhone", "5552222")
				.put("addresses", new io.vertx.core.json.JsonArray()
						.add(new JsonObject()
								.put("addressLine1", "1 Main Street")
								.put("city", "Bethlehem")
								.put("region", "PA")
								.put("postalCode", "18015")
								.put("addressTypeId", "home")));
		JsonObject user = new JsonObject().put("personal", personal);

		@SuppressWarnings("unchecked")
		ArrayList<UserAddressInformation> addresses = (ArrayList<UserAddressInformation>) retrieveAddress.invoke(
				service,
				user,
				"relais");

		assertTrue(addresses.size() >= 4);
	}

	private LookupUserInitiationData initDataWithAgency(String agency) {
		LookupUserInitiationData initData = mock(LookupUserInitiationData.class);
		when(initData.toString()).thenReturn("lookup-request");

		InitiationHeader initiationHeader = mock(InitiationHeader.class);
		FromAgencyId fromAgencyId = mock(FromAgencyId.class);
		org.extensiblecatalog.ncip.v2.service.AgencyId agencyId = mock(
				org.extensiblecatalog.ncip.v2.service.AgencyId.class);

		when(agencyId.getValue()).thenReturn(agency);
		when(fromAgencyId.getAgencyId()).thenReturn(agencyId);
		when(initiationHeader.getFromAgencyId()).thenReturn(fromAgencyId);
		when(initData.getInitiationHeader()).thenReturn(initiationHeader);
		return initData;
	}

}
