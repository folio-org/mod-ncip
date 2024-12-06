package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.*;

public class CheckOutItem extends TestBase {

    private static final String ITEM_ID = "c7369b8e-a573-45e4-b2ab-9c39e7fe04a4";
    @Test
    public void callCheckOutItem() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-full.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_ID));
    }

    @Test
    public void callCheckOutItemWithoutExternalReference() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-null-request-id-external-reference.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_ID));
    }

    @Test
    public void callCheckOutItemWithoutExternalReferenceType() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-null-external-reference-type.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_ID));
    }

    @Test
    public void callCheckOutItemEmptyExternalReferenceValue() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-null-external-reference-value.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(ITEM_ID));
    }

    @Test
    public void callCheckOutItem_GivenNoRequestId() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-null-request-id.xml");
        assertNull(response);
    }

    @Test
    public void callCheckOutItem_GivenNullRequestIdentifierValue() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-checkout-null-request-identifier-value.xml");
        assertNull(response);
    }
}
