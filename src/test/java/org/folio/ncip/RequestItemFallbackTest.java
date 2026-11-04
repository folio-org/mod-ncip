package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.restassured.response.Response;
import io.restassured.RestAssured;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestItemFallbackTest extends TestBase {

    private static MockServer mockServer;

    private static final String CREATED_REQUEST_ID = "de627598-d468-41e0-a849-450940478477";

    @BeforeClass
    public static void beforeClass() throws InterruptedException, ExecutionException, TimeoutException {
        // Other test classes install global headers; these requests need this mock server's headers.
        RestAssured.reset();
        mockServer = new MockServer(Utils.nextFreePort());
        mockServer.start();
    }

    @AfterClass
    public static void afterClass() {
        RestAssured.reset();
        if (mockServer != null) {
            mockServer.close();
        }
    }

    @Test
    public void missingItemIdAndEmptyBibliographicIdUseLegacyRequestIdBarcode() throws Exception {
        Response response = postData("src/test/resources/mockdata/ncip-requestitem-barcodeFallback.xml");
        String body = response.getBody().prettyPrint();

        assertEquals(200, response.getStatusCode());
        assertFalse(body.contains("Problem"));
        assertTrue(body.contains(CREATED_REQUEST_ID));
        assertTrue(body.contains("<ns1:ItemIdentifierValue>0000005</ns1:ItemIdentifierValue>"));
        assertTrue(mockServer.getItemQueries().contains("barcode==\"0000005\""));
    }

    @Test
    public void productionStylePrefixedXmlUsesLegacyRequestIdBarcode() throws Exception {
        Response response = postData("src/test/resources/mockdata/ncip-requestitem-legacy-prefixed.xml");
        String body = response.getBody().asString();

        assertEquals(200, response.getStatusCode());
        assertFalse(body.contains("Problem"));
        assertTrue(body.contains(CREATED_REQUEST_ID));
        assertTrue(mockServer.getItemQueries().contains("barcode==\"TEST-ITEM-0001\""));
    }
}
