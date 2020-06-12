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

import org.extensiblecatalog.ncip.v2.service.NameInformation;
import org.extensiblecatalog.ncip.v2.service.UserAddressInformation;
import org.folio.ncip.services.FolioLookupUserService;
import org.junit.Before;
import org.junit.Test;

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
		Method retrievePhysicalAddressMethod = folioLookupUserService.getClass().getDeclaredMethod("retrievePhysicalAddress", JsonObject.class);
		retrievePhysicalAddressMethod.setAccessible(true);
		Field ncipProperties = folioLookupUserService.getClass().getDeclaredField("ncipProperties");
		ncipProperties.setAccessible(true);
		ncipProperties.set(folioLookupUserService, p);
		

		JsonObject jsonObject = new JsonObject();
		jsonObject.put("addressLine1", "1 Main Street");
		jsonObject.put("region", "PA");
		jsonObject.put("postalCode", "18038");
		jsonObject.put("addressTypeId", "key-to-home-address");
	    UserAddressInformation userAddressInformation = (UserAddressInformation) retrievePhysicalAddressMethod.invoke(folioLookupUserService, jsonObject);
	    assertEquals(userAddressInformation.getPhysicalAddress().getStructuredAddress().getLine1(),"1 Main Street");
	    assertEquals(userAddressInformation.getPhysicalAddress().getStructuredAddress().getLocality(),null);
		

	}
	
	
	@Test
	public void testRetrieveTelephoneNumber() throws Exception {
		FolioLookupUserService folioLookupUserService = new FolioLookupUserService();
		Method retrieveTelephoneNumber = folioLookupUserService.getClass().getDeclaredMethod("retrieveTelephoneNumber", JsonObject.class,String.class);
		retrieveTelephoneNumber.setAccessible(true);
		JsonObject personal = new JsonObject();
		JsonObject telephone = new JsonObject();
		telephone.put("home", "5551212");
		personal.put("personal", telephone);
		UserAddressInformation telephoneInfo = (UserAddressInformation) retrieveTelephoneNumber.invoke(folioLookupUserService, personal,"home");
		assertEquals(telephoneInfo.getElectronicAddress().getElectronicAddressData(),"5551212");
		
	
	}
	
	@Test
	public void testRetrieveName() throws Exception {
		FolioLookupUserService folioLookupUserService = new FolioLookupUserService();
		Method retrieveNameMethod = folioLookupUserService.getClass().getDeclaredMethod("retrieveName", JsonObject.class);
		retrieveNameMethod.setAccessible(true);
		JsonObject personal = new JsonObject();
		JsonObject name = new JsonObject();
		name.put("firstName", "John");
		name.put("lastName", "Doe");
		personal.put("personal", name);
		NameInformation nameInfo = (NameInformation) retrieveNameMethod.invoke(folioLookupUserService, personal);
		assertEquals(nameInfo.getPersonalNameInformation().getStructuredPersonalUserName().getGivenName(),"John");
		assertEquals(nameInfo.getPersonalNameInformation().getStructuredPersonalUserName().getSurname(),"Doe");
		
	
	}

}
