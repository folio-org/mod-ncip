package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeleteItem extends TestBase {
    private static final String ITEM_BARCODE = "at-013";

    @Test
    public void callDeleteItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-deleteItem.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_BARCODE));
    }
}
