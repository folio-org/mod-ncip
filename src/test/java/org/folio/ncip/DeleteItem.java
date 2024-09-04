package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeleteItem extends TestBase {
    private static final String ITEM_BARCODE = "at-013";
    private static final String PROBLEM = "Problem performing DeleteItem";

    @Test
    public void callDeleteItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-deleteItem.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_BARCODE));
    }

    @Test
    public void callDeleteItemMissingItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-deleteItem-missingItem.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(PROBLEM));
    }

    @Test
    public void callDeleteItemMissingAgency() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-deleteItem-missingAgency.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(PROBLEM));
    }

    @Test
    public void callDeleteItemError() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-deleteItem-error.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(PROBLEM));
    }
}
