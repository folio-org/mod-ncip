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

import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.UserId;
import org.folio.ncip.services.FolioRequestItemService;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.*;



public class RequestItemServiceTests {

	

	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	

	}
	
	@Test
	public void testRetrieveUserId() throws Exception {
		FolioRequestItemService folioRequestItemService = new FolioRequestItemService();
		Method retrieveUserId = folioRequestItemService.getClass().getDeclaredMethod("retrieveUserId", RequestItemInitiationData.class);
		retrieveUserId.setAccessible(true);
		RequestItemInitiationData requestItemInitiationData = new RequestItemInitiationData();
		UserId userId = new UserId();
		userId.setUserIdentifierValue("abc123");
		requestItemInitiationData.setUserId(userId);
		UserId userIdResult = (UserId) retrieveUserId.invoke(folioRequestItemService, requestItemInitiationData);
		assertEquals(userIdResult.getUserIdentifierValue(),"abc123");
	}

}
