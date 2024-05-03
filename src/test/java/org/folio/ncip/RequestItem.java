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
	}

}
