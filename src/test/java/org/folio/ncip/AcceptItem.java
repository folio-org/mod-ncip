package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AcceptItem extends TestBase {

    private static final String REQUEST_ID = "de627598-d468-41e0-a849-450940478477";
    private static final String ITEM_UUID = "7212ba6a-8dcf-45a1-be9a-ffaa847c4423";
    @Test
    public void acceptItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-acceptItem.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(REQUEST_ID));
        assertTrue(body.contains(ITEM_UUID));
    }

    @Test
    public void acceptItemFailFee() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-acceptItem-blocked.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains("Problem performing AcceptItem"));
    }

}
