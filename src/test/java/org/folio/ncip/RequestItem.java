/**
 * 
 */
package org.folio.ncip;

import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;

import io.restassured.response.Response;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;


/**
 * @author 
 *
 */
public class RequestItem extends TestBase {
	private static final String CREATED_REQUEST_ID = "de627598-d468-41e0-a849-450940478477";
	private static final String ITEM_IDENTIFIER = "<ns1:ItemIdentifierValue>0000005</ns1:ItemIdentifierValue>";
	private static final String CALL_NUMBER = "<ns1:CallNumber>58.95</ns1:CallNumber>";
	private static final String LOCATION = "Annex : Datalogisk Institut";
	private static final String REQUESTER_ID = "764fe3bf-e09b-4fcc-b4a9-c78aab6995f1";
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void callRequestItem() throws MalformedURLException {
		Response response = postData("src/test/resources/mockdata/ncip-requestitem.xml");
		String body = response.getBody().prettyPrint();
		System.out.println(body);
		assertEquals(200, response.getStatusCode());
		assertTrue(body.contains(CREATED_REQUEST_ID));
		assertTrue(body.contains(ITEM_IDENTIFIER));
		assertTrue(body.contains(CALL_NUMBER));
		assertTrue(body.contains(LOCATION));
		assertTrue(body.contains(REQUESTER_ID));
	}



	@Test
	public void callRequestItemTitle() throws MalformedURLException {
		Response response = postData("src/test/resources/mockdata/ncip-requestitem-title.xml");
		String body = response.getBody().prettyPrint();
		System.out.println(body);
		assertEquals(200, response.getStatusCode());
		assertTrue(body.contains(CREATED_REQUEST_ID));
		assertTrue(body.contains(ITEM_IDENTIFIER));
		assertTrue(body.contains(CALL_NUMBER));
		assertTrue(body.contains(LOCATION));
		assertTrue(body.contains(REQUESTER_ID));
	}
}
