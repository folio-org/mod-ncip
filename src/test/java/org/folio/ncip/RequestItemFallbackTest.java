package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.restassured.response.Response;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestItemFallbackTest extends TestBase {

    private static MockServer mockServer;

    private static final String CREATED_REQUEST_ID = "de627598-d468-41e0-a849-450940478477";
    private static final String ITEM_IDENTIFIER = "<ns1:ItemIdentifierValue>0000005</ns1:ItemIdentifierValue>";
    private static final String CALL_NUMBER = "<ns1:CallNumber>58.95</ns1:CallNumber>";
    private static final String LOCATION = "Annex";
    private static final String REQUESTER_ID = "764fe3bf-e09b-4fcc-b4a9-c78aab6995f1";

    @BeforeClass
    public static void beforeClass() throws InterruptedException, ExecutionException, TimeoutException {
        mockServer = new MockServer(Utils.nextFreePort());
        mockServer.start();
    }

    @AfterClass
    public static void afterClass() {
        if (mockServer != null) {
            mockServer.close();
        }
    }

    @Test
    public void callRequestItemWithBarcodeFallbackWhenHridMissing() throws Exception {
        Response response = postData("src/test/resources/mockdata/ncip-requestitem-barcodeFallback.xml");
        String body = response.getBody().prettyPrint();

        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(CREATED_REQUEST_ID));
        assertTrue(body.contains(ITEM_IDENTIFIER));
        assertTrue(body.contains(CALL_NUMBER));
        assertTrue(body.contains(LOCATION));
        assertTrue(body.contains(REQUESTER_ID));
    }
}
