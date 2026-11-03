package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void requestIdCannotSubstituteForMissingItemIdentifiers() throws Exception {
        Response response = postData("src/test/resources/mockdata/ncip-requestitem-barcodeFallback.xml");
        String body = response.getBody().prettyPrint();

        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains("Problem"));
        assertTrue(body.contains("Item id missing"));
        assertFalse(body.contains(CREATED_REQUEST_ID));
    }
}
