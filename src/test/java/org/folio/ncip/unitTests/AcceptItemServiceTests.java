package org.folio.ncip.unitTests;


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
import org.folio.ncip.FolioNcipException;
import org.folio.ncip.services.FolioAcceptItemService;
import org.folio.ncip.services.FolioLookupUserService;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.*;



public class AcceptItemServiceTests {

	

	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	

	}
	

	@Test
	public void testValidatePickupLocation() throws Exception {
		FolioAcceptItemService folioAcceptItemService = new FolioAcceptItemService();
		Method validatePickupLocation = folioAcceptItemService.getClass().getDeclaredMethod("validatePickupLocation", String.class);
		validatePickupLocation.setAccessible(true);
		try {
			validatePickupLocation.invoke(folioAcceptItemService, "");
			 fail("expected exception was not occured.");
		}
		catch(Exception e) {
			//EXPECTED AN EXCEPTION
		}
		

	}
	
	
	

}
