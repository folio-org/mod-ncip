package org.folio.ncip.unitTests;


import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import org.extensiblecatalog.ncip.v2.service.AuthenticationDataFormatType;
import  org.extensiblecatalog.ncip.v2.service.AuthenticationInput;
import org.extensiblecatalog.ncip.v2.service.AuthenticationInputType;
import org.extensiblecatalog.ncip.v2.service.BibliographicDescription;
import org.extensiblecatalog.ncip.v2.service.CheckOutItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.InitiationHeader;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.ncip.services.FolioCheckOutItemService;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.*;



public class CheckoutItemServiceTest {

	//retrieveAuthenticationInputTypeOf
	//retrieveUserId

	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	

	}
	

	@Test
	public void testRetrieveAuthenticationInputTypeOf() throws Exception {
		FolioCheckOutItemService checkOutItemService = new FolioCheckOutItemService();
		Method retrieveAuthenticationInputTypeOf = checkOutItemService.getClass().getDeclaredMethod("retrieveAuthenticationInputTypeOf", String.class,CheckOutItemInitiationData.class);
		retrieveAuthenticationInputTypeOf.setAccessible(true);
		CheckOutItemInitiationData checkOutItemInitiationData = new CheckOutItemInitiationData();
		AuthenticationDataFormatType dataFormatType = new AuthenticationDataFormatType(null,"text");
		AuthenticationInputType authenticationInputType = new AuthenticationInputType(null,"username");
		AuthenticationInput authInput = new AuthenticationInput();
		authInput.setAuthenticationDataFormatType(dataFormatType);
		authInput.setAuthenticationInputData("tst101");
		authInput.setAuthenticationInputType(authenticationInputType);
		List<AuthenticationInput> inputs = new ArrayList<AuthenticationInput>();
		inputs.add(authInput);
		checkOutItemInitiationData.setAuthenticationInputs(inputs);
		String typeString = "username";
		String userid = (String) retrieveAuthenticationInputTypeOf.invoke(checkOutItemService, typeString , checkOutItemInitiationData);
		assertEquals(userid,"tst101");
	}
	
	

	@Test
	public void testRetrieveAuthViaUserId() throws Exception {
		FolioCheckOutItemService checkOutItemService = new FolioCheckOutItemService();
		Method retrieveUserId = checkOutItemService.getClass().getDeclaredMethod("retrieveUserId", CheckOutItemInitiationData.class);
		retrieveUserId.setAccessible(true);
		CheckOutItemInitiationData checkOutItemInitiationData = new CheckOutItemInitiationData();
		AuthenticationDataFormatType dataFormatType = new AuthenticationDataFormatType(null,"text");
		AuthenticationInputType authenticationInputType = new AuthenticationInputType(null,"username");
		AuthenticationInput authInput = new AuthenticationInput();
		authInput.setAuthenticationDataFormatType(dataFormatType);
		authInput.setAuthenticationInputData("tst101");
		authInput.setAuthenticationInputType(authenticationInputType);
		List<AuthenticationInput> inputs = new ArrayList<AuthenticationInput>();
		inputs.add(authInput);
		checkOutItemInitiationData.setAuthenticationInputs(inputs);
		String typeString = "username";
		UserId userid = (UserId) retrieveUserId.invoke(checkOutItemService, checkOutItemInitiationData);
		assertEquals(userid.getUserIdentifierValue(),"tst101");
	}
	
	@Test
	public void testRetrieveUserId() throws Exception {
		FolioCheckOutItemService checkOutItemService = new FolioCheckOutItemService();
		Method retrieveUserId = checkOutItemService.getClass().getDeclaredMethod("retrieveUserId", CheckOutItemInitiationData.class);
		retrieveUserId.setAccessible(true);
		CheckOutItemInitiationData checkOutItemInitiationData = new CheckOutItemInitiationData();
		UserId userId = new UserId();
		userId.setUserIdentifierValue("abc123");
		checkOutItemInitiationData.setUserId(userId);
		UserId userIdResult = (UserId) retrieveUserId.invoke(checkOutItemService, checkOutItemInitiationData);
		assertEquals(userIdResult.getUserIdentifierValue(),"abc123");
	}
	
	

}
