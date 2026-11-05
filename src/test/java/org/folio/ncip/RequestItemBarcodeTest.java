package org.folio.ncip;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;

import java.util.Arrays;

import io.restassured.http.ContentType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Exercises XML translation, service validation, lookup, and response generation together. */
public class RequestItemBarcodeTest {
    private static MockServer server;
    private static String endpoint;

    @BeforeClass
    public static void startServer() throws Exception {
        int port = Utils.nextFreePort();
        server = new MockServer(port);
        server.start();
        endpoint = "http://localhost:" + port;
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    public void itemIdBarcodeWorksWithoutBibliographicId() {
        String body = post("Item", "<ItemId><ItemIdentifierValue>0000005</ItemIdentifierValue></ItemId>",
                "ill-request-123");
        assertSuccess(body);
        assertTrue(body.contains("<ns1:ItemIdentifierValue>0000005</ns1:ItemIdentifierValue>"));
        assertTrue(body.contains("de627598-d468-41e0-a849-450940478477"));
        assertEquals("barcode==\"0000005\"", lastItemQuery());
        assertEquals("Item", lastPostedRequest().getString("requestLevel"));
        assertNotNull(lastPostedRequest().getString("itemId"));
    }

    @Test
    public void legacyItemHridStillWorksWithoutItemId() {
        assertSuccess(post("Item", bib("itemHrid1"), "ill-request-123"));
        assertEquals("hrid==\"itemHrid1\"", lastItemQuery());
    }

    @Test
    public void otherScopesUseBarcodeFirstOnTheWire() {
        for (String scope : Arrays.asList("", null, "Other")) {
            assertSuccess(post(scope, "<ItemId><ItemIdentifierValue>0000005</ItemIdentifierValue></ItemId>"
                    + bib("different-item-hrid"), "ill-request-123"));
            assertEquals("barcode==\"0000005\"", lastItemQuery());
        }
    }

    @Test
    public void otherScopesRetainHridFallbackOnTheWire() {
        for (String scope : Arrays.asList("", null, "Other")) {
            assertSuccess(post(scope, bib("itemHrid1"), "ill-request-123"));
            assertEquals("hrid==\"itemHrid1\"", lastItemQuery());
        }
    }

    @Test
    public void bibliographicScopesCreateTitleRequestsOnTheWire() {
        for (String scope : Arrays.asList("Title", "Bibliographic Item", "tItLe", "bIbLiOgRaPhIc ItEm")) {
            for (String itemId : Arrays.asList("",
                    "<ItemId><ItemIdentifierValue>ignored-barcode</ItemIdentifierValue></ItemId>")) {
                int itemQueriesBefore = server.getItemQueries().size();
                int instanceQueriesBefore = server.getInstanceQueries().size();
                assertSuccess(post(scope, itemId + bib("instanceHrid1"), "ill-request-123"));
                assertEquals(itemQueriesBefore, server.getItemQueries().size());
                assertEquals(instanceQueriesBefore + 1, server.getInstanceQueries().size());
                assertEquals("hrid==\"instanceHrid1\"", server.getInstanceQueries().get(instanceQueriesBefore));
                assertEquals("Title", lastPostedRequest().getString("requestLevel"));
                assertNotNull(lastPostedRequest().getString("instanceId"));
                assertFalse(lastPostedRequest().containsKey("itemId"));
                assertFalse(lastPostedRequest().containsKey("holdingsRecordId"));
            }
        }
    }

    @Test
    public void barcodeTakesPrecedenceOverHridOnTheWire() {
        assertSuccess(post("Item", "<ItemId><ItemIdentifierValue>A+B &amp; C</ItemIdentifierValue></ItemId>"
                + bib("different-item-hrid"), "ill-request-123"));
        assertEquals("barcode==\"A+B & C\"", lastItemQuery());
    }

    @Test
    public void unknownBarcodeDoesNotFallBackToHrid() {
        int before = server.getItemQueries().size();
        String body = post("Item", "<ItemId><ItemIdentifierValue>missing-requestitem-barcode</ItemIdentifierValue></ItemId>"
                + bib("itemHrid1"), "ill-request-123");
        assertTrue(body.contains("Problem"));
        assertEquals(before + 1, server.getItemQueries().size());
        assertEquals("barcode==\"missing-requestitem-barcode\"", lastItemQuery());
    }

    @Test
    public void legacyRequestIdBarcodeWorksWithMissingOrBlankItemIdentifiers() {
        for (String scope : Arrays.asList("Item", "", null, "Other")) {
            for (String itemId : Arrays.asList("", "<ItemId/>",
                    "<ItemId><ItemIdentifierValue> </ItemIdentifierValue></ItemId>")) {
                for (String bibliographicId : Arrays.asList("", bib(""), bib(" "))) {
                    String body = post(scope, itemId + bibliographicId, "0000005");
                    assertSuccess(body);
                    assertTrue(body.contains("de627598-d468-41e0-a849-450940478477"));
                    assertEquals("barcode==\"0000005\"", lastItemQuery());
                    assertEquals("Item", lastPostedRequest().getString("requestLevel"));
                }
            }
        }
    }

    @Test
    public void unknownHridDoesNotFallBackToRequestId() {
        int before = server.getItemQueries().size();
        int requestsBefore = server.getPostedRequests().size();
        String body = post("Item", bib("missing-requestitem-hrid"), "0000005");
        assertTrue(body.contains("Problem"));
        assertEquals(before + 1, server.getItemQueries().size());
        assertEquals("hrid==\"missing-requestitem-hrid\"", lastItemQuery());
        assertEquals(requestsBefore, server.getPostedRequests().size());
    }

    @Test
    public void entirelyAbsentIdentifiersReturnNcipProblem() {
        String body = post("Item", "", null);
        assertTrue(body.contains("Problem"));
        assertTrue(body.contains("Item id missing"));
    }

    @Test
    public void titleScopesCannotUseLegacyRequestIdBarcodeOnTheWire() {
        for (String scope : Arrays.asList("Title", "Bibliographic Item")) {
            int itemsBefore = server.getItemQueries().size();
            int instancesBefore = server.getInstanceQueries().size();
            int requestsBefore = server.getPostedRequests().size();
            String body = post(scope, bib(""), "0000005");
            assertTrue(body.contains("Bibliographic id missing"));
            assertEquals(itemsBefore, server.getItemQueries().size());
            assertEquals(instancesBefore, server.getInstanceQueries().size());
            assertEquals(requestsBefore, server.getPostedRequests().size());
        }
    }

    @Test
    public void bibliographicScopesCannotUseItemIdInPlaceOfInstanceHrid() {
        for (String scope : Arrays.asList("Title", "Bibliographic Item")) {
            int itemQueriesBefore = server.getItemQueries().size();
            int instanceQueriesBefore = server.getInstanceQueries().size();
            int requestsBefore = server.getPostedRequests().size();
            String body = post(scope, "<ItemId><ItemIdentifierValue>0000005</ItemIdentifierValue></ItemId>",
                    "ill-request-123");
            assertTrue(body.contains("Problem"));
            assertTrue(body.contains("Bibliographic id missing"));
            assertEquals(itemQueriesBefore, server.getItemQueries().size());
            assertEquals(instanceQueriesBefore, server.getInstanceQueries().size());
            assertEquals(requestsBefore, server.getPostedRequests().size());
        }
    }

    @Test
    public void requestIdIsStillRequiredWithBarcode() {
        String body = post("Item", "<ItemId><ItemIdentifierValue>0000005</ItemIdentifierValue></ItemId>", null);
        assertTrue(body.contains("Problem"));
        assertTrue(body.contains("Request ID Missing"));
    }

    private String post(String scope, String identifiers, String requestId) {
        String xml = """
                <NCIPMessage version="http://www.niso.org/schemas/ncip/v2_02/ncip_v2_02.xsd">
                  <RequestItem>
                    <InitiationHeader>
                      <FromAgencyId><AgencyId>Relais</AgencyId></FromAgencyId>
                      <ToAgencyId><AgencyId>Lehigh University</AgencyId></ToAgencyId>
                    </InitiationHeader>
                    <UserId><UserIdentifierValue>8377630</UserIdentifierValue></UserId>
                    %s
                    %s
                    <RequestType>Page</RequestType>
                    %s
                  </RequestItem>
                </NCIPMessage>
                """.formatted(requestId == null ? "" : "<RequestId><RequestIdentifierValue>" + requestId
                        + "</RequestIdentifierValue></RequestId>", identifiers,
                        scope == null ? "" : "<RequestScopeType>" + scope + "</RequestScopeType>");
        return given().header("X-Okapi-Tenant", "lu").header("X-Okapi-Url", endpoint)
                .header("X-Okapi-Token", "na").contentType(ContentType.XML).body(xml)
                .post(endpoint + "/ncip").then().statusCode(200).extract().asString();
    }

    private String bib(String hrid) {
        return "<BibliographicId><BibliographicRecordId><BibliographicRecordIdentifier>" + hrid
                + "</BibliographicRecordIdentifier></BibliographicRecordId></BibliographicId>";
    }

    private String lastItemQuery() {
        return server.getItemQueries().get(server.getItemQueries().size() - 1);
    }

    private io.vertx.core.json.JsonObject lastPostedRequest() {
        return server.getPostedRequests().get(server.getPostedRequests().size() - 1);
    }

    private void assertSuccess(String body) {
        assertFalse(body, body.contains("Problem"));
        assertTrue(body, body.contains("RequestItemResponse"));
    }
}
