package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class FolioRemoteServiceManagerTest {

    @Test
    public void lookupPatronRecordByAcceptsMixedCaseBarcodeType() throws Exception {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        FolioRemoteServiceManager manager = new FolioRemoteServiceManager() {
            @Override
            public String callApiGet(String uriString) {
                requestedUrl.set(uriString);
                return "{\"users\":[{\"id\":\"user-1\"}]}";
            }
        };

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(Constants.X_OKAPI_URL, "http://localhost:8082");
        manager.setOkapiHeaders(headers);

        JsonObject user = manager.lookupPatronRecordBy("Barcode", "abc123");
        String decodedUrl = URLDecoder.decode(requestedUrl.get(), StandardCharsets.UTF_8);

        assertNotNull(user);
        assertEquals("user-1", user.getString(Constants.ID));
        assertTrue(decodedUrl.contains("barcode=="));
        assertTrue(!decodedUrl.contains("Barcode=="));
    }

    @Test
    public void lookupPatronRecordByAcceptsMixedCaseExternalSystemIdType() throws Exception {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        FolioRemoteServiceManager manager = new FolioRemoteServiceManager() {
            @Override
            public String callApiGet(String uriString) {
                requestedUrl.set(uriString);
                return "{\"users\":[{\"id\":\"user-2\"}]}";
            }
        };

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(Constants.X_OKAPI_URL, "http://localhost:8082");
        manager.setOkapiHeaders(headers);

        JsonObject user = manager.lookupPatronRecordBy("ExternalSystemId", "ext-123");
        String decodedUrl = URLDecoder.decode(requestedUrl.get(), StandardCharsets.UTF_8);

        assertNotNull(user);
        assertEquals("user-2", user.getString(Constants.ID));
        assertTrue(decodedUrl.contains("externalSystemId=="));
        assertTrue(!decodedUrl.contains("ExternalSystemId=="));
    }

    @Test
    public void deleteItemEncodesSpecialCharactersInBarcodeLookupQuery() throws Exception {
        AtomicReference<String> requestedUrl = new AtomicReference<>();
        FolioRemoteServiceManager manager = new FolioRemoteServiceManager() {
            @Override
            public String callApiGet(String uriString) {
                requestedUrl.set(uriString);
                return "{\"items\":[],\"totalRecords\":0}";
            }
        };

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add(Constants.X_OKAPI_URL, "http://localhost:8082");
        manager.setOkapiHeaders(headers);

        manager.deleteItem("abc+123", "diku");

        String rawUrl = requestedUrl.get();
        String decodedUrl = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);

        assertTrue(rawUrl.contains("%2B"));
        assertTrue(decodedUrl.contains("barcode==\"abc+123\""));
    }

    @Test
    public void requestItemBarcodeFallbackBuildsEncodedLookupUrlForPlusSign() throws Exception {
        FolioRemoteServiceManager manager = new FolioRemoteServiceManager();

        String rawUrl = manager.buildItemSearchByBarcodeUrl("http://localhost:8082", "abc+123");
        String decodedUrl = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8);

        assertTrue(rawUrl.contains("%2B"));
        assertTrue(decodedUrl.contains("barcode==\"abc+123\""));
    }
}