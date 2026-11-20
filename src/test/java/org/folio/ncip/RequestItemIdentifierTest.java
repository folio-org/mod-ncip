package org.folio.ncip;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.extensiblecatalog.ncip.v2.service.BibliographicId;
import org.extensiblecatalog.ncip.v2.service.BibliographicRecordId;
import org.extensiblecatalog.ncip.v2.service.ItemId;
import org.extensiblecatalog.ncip.v2.service.RequestId;
import org.extensiblecatalog.ncip.v2.service.RequestItemInitiationData;
import org.extensiblecatalog.ncip.v2.service.RequestScopeType;
import org.extensiblecatalog.ncip.v2.service.SchemeValueBehavior;
import org.extensiblecatalog.ncip.v2.service.SchemeValuePair;
import org.junit.BeforeClass;
import org.junit.Test;

public class RequestItemIdentifierTest {
    private static final String BASE_URL = "http://folio.example";

    @BeforeClass
    public static void configureScopeTypes() {
        SchemeValuePair.allowNullScheme(RequestScopeType.class.getName());
        SchemeValuePair.mapBehavior(RequestScopeType.class.getName(), SchemeValueBehavior.ALLOW_ANY);
    }

    @Test
    public void itemScopeAcceptsBarcodeWithoutBibliographicId() throws Exception {
        RequestItemInitiationData data = request("Item", "barcode-1", null);
        assertLookup(data, "/inventory/items", "barcode==\"barcode-1\"", false);
    }

    @Test
    public void barcodeTakesPrecedenceOverConflictingHrid() throws Exception {
        assertLookup(request("Item", "barcode-1", "different-item-hrid"),
                "/inventory/items", "barcode==\"barcode-1\"", false);
    }

    @Test
    public void scopeComparisonIsCaseInsensitive() throws Exception {
        assertLookup(request("iTeM", "barcode-1", null),
                "/inventory/items", "barcode==\"barcode-1\"", false);
    }

    @Test
    public void missingOrBlankBarcodeFallsBackToItemHrid() throws Exception {
        for (String barcode : Arrays.asList(null, "", " \t")) {
            assertLookup(request("Item", barcode, "item-hrid"),
                    "/inventory/items", "hrid==\"item-hrid\"", false);
        }
        RequestItemInitiationData data = request("Item", null, "item-hrid");
        data.setItemIds(Arrays.asList((ItemId) null));
        assertLookup(data, "/inventory/items", "hrid==\"item-hrid\"", false);
        data.setItemIds(List.of(new ItemId()));
        assertLookup(data, "/inventory/items", "hrid==\"item-hrid\"", false);
        data.setItemIds(List.of());
        assertLookup(data, "/inventory/items", "hrid==\"item-hrid\"", false);
    }

    @Test
    public void bibliographicScopesUseInstanceHridAndIgnoreItemId() throws Exception {
        for (String scope : Arrays.asList("Title", "Bibliographic Item", "tItLe", "bIbLiOgRaPhIc ItEm")) {
            for (String barcode : Arrays.asList(null, "ignored-barcode")) {
                assertLookup(request(scope, barcode, "instance-hrid"),
                        "/inventory/instances", "hrid==\"instance-hrid\"", true);
            }
        }
    }

    @Test
    public void otherScopesPreferBarcode() throws Exception {
        for (String scope : Arrays.asList("Item", "", null, "Other", "Not Title")) {
            assertLookup(request(scope, "barcode-1", "different-item-hrid"),
                    "/inventory/items", "barcode==\"barcode-1\"", false);
            assertLookup(request(scope, "barcode-1", null),
                    "/inventory/items", "barcode==\"barcode-1\"", false);
        }
    }

    @Test
    public void otherScopesFallBackToItemHridWhenBarcodeIsMissing() throws Exception {
        for (String scope : Arrays.asList("Item", "", null, "Other")) {
            assertLookup(request(scope, null, "item-hrid"),
                    "/inventory/items", "hrid==\"item-hrid\"", false);
            assertLookup(request(scope, " \t", "item-hrid"),
                    "/inventory/items", "hrid==\"item-hrid\"", false);
        }
    }

    @Test
    public void specialCharactersAreCqlEscapedAndUrlEncoded() throws Exception {
        assertLookup(request("Item", "A+B & \"C\"\\D/é", null),
                "/inventory/items", "barcode==\"A+B & \\\"C\\\"\\\\D/é\"", false);
    }

    @Test
    public void legacyRequestIdBarcodeIsUsedOnlyWhenItemIdAndHridAreMissingOrBlank() throws Exception {
        for (String scope : Arrays.asList("Item", "", null, "Other")) {
            for (String barcode : Arrays.asList(null, "", " \t")) {
                for (String hrid : Arrays.asList(null, "", " \t")) {
                    assertLookup(withRequestId(request(scope, barcode, hrid), "legacy-barcode"),
                            "/inventory/items", "barcode==\"legacy-barcode\"", false);
                }
            }
        }
    }

    @Test
    public void explicitIdentifiersTakePrecedenceOverLegacyRequestId() throws Exception {
        for (String scope : Arrays.asList("Item", "", null, "Other")) {
            assertLookup(withRequestId(request(scope, "barcode-1", "item-hrid"), "legacy-barcode"),
                    "/inventory/items", "barcode==\"barcode-1\"", false);
            for (String barcode : Arrays.asList(null, "", " \t")) {
                assertLookup(withRequestId(request(scope, barcode, "item-hrid"), "legacy-barcode"),
                        "/inventory/items", "hrid==\"item-hrid\"", false);
            }
        }
    }

    @Test
    public void legacyRequestIdBarcodeIsCqlEscapedAndUrlEncoded() throws Exception {
        assertLookup(withRequestId(request("Item", "", ""), "A+B & \"C\"\\D/é"),
                "/inventory/items", "barcode==\"A+B & \\\"C\\\"\\\\D/é\"", false);
    }

    @Test
    public void missingOrBlankRequestIdCannotSupplyLegacyBarcode() {
        for (String requestId : Arrays.asList(null, "", " \t")) {
            RequestItemInitiationData data = withRequestId(request("Item", "", ""), requestId);
            FolioNcipException exception = assertThrows(FolioNcipException.class,
                    () -> RequestItemIdentifier.resolve(data));
            assertEquals("Item id missing", exception.getMessage());
        }
    }

    @Test
    public void requestIdCannotSubstituteForInstanceHrid() {
        for (String scope : Arrays.asList("Title", "Bibliographic Item", "tItLe", "bIbLiOgRaPhIc ItEm")) {
            for (String hrid : Arrays.asList(null, "", " \t")) {
                RequestItemInitiationData data = withRequestId(request(scope, null, hrid), "legacy-barcode");
                FolioNcipException exception = assertThrows(FolioNcipException.class,
                        () -> RequestItemIdentifier.resolve(data));
                assertEquals("Bibliographic id missing", exception.getMessage());
            }
        }
    }

    private RequestItemInitiationData withRequestId(RequestItemInitiationData data, String value) {
        RequestId requestId = new RequestId();
        requestId.setRequestIdentifierValue(value);
        data.setRequestId(requestId);
        return data;
    }

    @Test
    public void bibliographicScopesRequireBibliographicIdEvenWhenItemIdIsPresent() {
        for (String scope : Arrays.asList("Title", "Bibliographic Item")) {
            for (String hrid : Arrays.asList(null, "", " \t")) {
                FolioNcipException exception = assertThrows(FolioNcipException.class,
                        () -> RequestItemIdentifier.resolve(request(scope, "barcode-1", hrid)));
                assertEquals("Bibliographic id missing", exception.getMessage());
            }
        }
    }

    @Test
    public void incompleteBibliographicIdsAreRejectedWithoutNullPointerExceptions() {
        RequestItemInitiationData data = request("Item", null, null);
        for (List<BibliographicId> ids : Arrays.asList(List.<BibliographicId>of(),
                Arrays.asList((BibliographicId) null), List.of(new BibliographicId()))) {
            data.setBibliographicIds(ids);
            assertThrows(FolioNcipException.class, () -> RequestItemIdentifier.resolve(data));
        }
    }

    private void assertLookup(RequestItemInitiationData data, String path, String query, boolean title)
            throws Exception {
        RequestItemIdentifier identifier = RequestItemIdentifier.resolve(data);
        URI uri = URI.create(identifier.searchUrl(BASE_URL));
        assertEquals(path, uri.getPath());
        assertEquals("limit=1&query=" + query,
                URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8));
        assertEquals(title, identifier.isTitleRequest());
    }

    private RequestItemInitiationData request(String scope, String barcode, String hrid) {
        RequestItemInitiationData data = new RequestItemInitiationData();
        if (scope != null) {
            data.setRequestScopeType(new RequestScopeType(null, scope));
        }
        if (barcode != null) {
            ItemId itemId = new ItemId();
            itemId.setItemIdentifierValue(barcode);
            data.setItemIds(List.of(itemId));
        }
        if (hrid != null) {
            BibliographicRecordId recordId = new BibliographicRecordId();
            recordId.setBibliographicRecordIdentifier(hrid);
            BibliographicId bibliographicId = new BibliographicId();
            bibliographicId.setBibliographicRecordId(recordId);
            data.setBibliographicIds(List.of(bibliographicId));
        }
        return data;
    }
}
