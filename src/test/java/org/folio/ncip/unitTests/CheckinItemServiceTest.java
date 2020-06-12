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

import org.extensiblecatalog.ncip.v2.service.BibliographicDescription;
import org.extensiblecatalog.ncip.v2.service.NameInformation;
import org.extensiblecatalog.ncip.v2.service.UserAddressInformation;
import org.folio.ncip.FolioNcipException;
import org.folio.ncip.services.FolioAcceptItemService;
import org.folio.ncip.services.FolioCheckInItemService;
import org.folio.ncip.services.FolioLookupUserService;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

import static org.junit.Assert.*;



public class CheckinItemServiceTest {

	

	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	

	}
	

	@Test
	public void testRetrieveBiblioDescription() throws Exception {
		FolioCheckInItemService folioCheckinItemService = new FolioCheckInItemService();
		Method retrieveBiblioDescriptionMethod = folioCheckinItemService.getClass().getDeclaredMethod("retrieveBiblioDescription", JsonObject.class);
		retrieveBiblioDescriptionMethod.setAccessible(true);
		JsonObject jsonObject = new JsonObject();
		JsonObject item = new JsonObject();
		item.put("title", "My Test Title");
		item.put("primaryContributor","Jane Doe");
		jsonObject.put("item", item);
		BibliographicDescription bibDescription = (BibliographicDescription) retrieveBiblioDescriptionMethod.invoke(folioCheckinItemService, jsonObject);
		assertEquals(bibDescription.getAuthor(),"Jane Doe");
		assertEquals(bibDescription.getTitle(),"My Test Title");
		

	}
	
	
	

}
