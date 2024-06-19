package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CancelRequestItem extends TestBase {

    private static final String REQUEST_ID = "5fc504cb-9042-4bfe-a54f-287c56cd7a11";

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void callCancelRequestItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-cancelRequestItem.xml");
        String body = response.getBody().prettyPrint();
        System.out.println(body);
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(REQUEST_ID));
    }
}
