/**
 * 
 */
package org.folio.ncip;

import static org.junit.Assert.*;

import java.net.MalformedURLException;

import org.hamcrest.Matchers;


import org.junit.Before;
import org.junit.Test;

import io.restassured.matcher.ResponseAwareMatcher;
import io.restassured.response.Response;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.path.xml.XmlPath.CompatibilityMode;




/**
 * @author 
 *
 */
public class RequestItem extends TestBase {
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void callRequestItem() throws MalformedURLException {
		Response response = postData("src/test/resources/mockdata/ncip-requestitem.xml");
		System.out.println(response.getBody().prettyPrint());
	}

}
