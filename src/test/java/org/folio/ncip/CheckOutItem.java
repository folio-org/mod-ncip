package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CheckOutItem extends TestBase {

    private static final String ITEM_ID = "c7369b8e-a573-45e4-b2ab-9c39e7fe04a4";
    @Test
    public void callCheckOutItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-full.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_ID));
    }
}
