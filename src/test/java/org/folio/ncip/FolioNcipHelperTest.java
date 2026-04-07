package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Test;

public class FolioNcipHelperTest {

    @Test
    public void initNcipPropertiesKeepsGlobalDefaultsWhenAgencyMissingProperty() throws Exception {
        String tenant = "diku";
        String okapiUrl = "http://okapi";

        MultiMap headers = MultiMap.caseInsensitiveMultiMap()
                .add(Constants.X_OKAPI_TENANT, tenant)
                .add(Constants.X_OKAPI_URL, okapiUrl);

        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getHeader(Constants.X_OKAPI_TENANT)).thenReturn(tenant);
        when(request.getHeader(Constants.X_OKAPI_URL)).thenReturn(okapiUrl);
        when(request.headers()).thenReturn(headers);

        RoutingContext context = mock(RoutingContext.class);
        when(context.request()).thenReturn(request);

        String settingsResponse = new io.vertx.core.json.JsonObject()
                .put("items", new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "relais")
                                .put("value", new io.vertx.core.json.JsonObject()
                                        .put("instance.type.name", "book")
                                        .put("response.includes.physical.address", true)))
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "test")
                                .put("value", new io.vertx.core.json.JsonObject()
                                        .put("instance.type.name", "book"))))
                .encode();

        String addressTypesResponse = new io.vertx.core.json.JsonObject()
                .put("addressTypes", new io.vertx.core.json.JsonArray())
                .encode();

        FolioNcipHelper helper = new StubFolioNcipHelper(Promise.promise(), settingsResponse, addressTypesResponse);
        helper.initNcipProperties(context);

        Properties loaded = (Properties) helper.ncipProperties.get(tenant);
        assertNotNull(loaded);

        // Verify global defaults remain available even when only one agency overrides a
        // setting.
        assertEquals("other", loaded.getProperty("cancel.request.reason.name"));
        assertEquals("other", loaded.getProperty("cancel.request.reason.patron.name"));
        assertEquals("General note", loaded.getProperty("request.note.name"));
        assertEquals("false", loaded.getProperty("response.includes.physical.address"));

        // Agency-specific override should be loaded where provided.
        assertEquals("true", loaded.getProperty("relais.response.includes.physical.address"));

        // Missing agency-specific value should not block fallback to the global default
        // key.
        assertNull(loaded.getProperty("test.response.includes.physical.address"));
    }

    @Test
    public void initNcipPropertiesLoadsGroupedSettingsFromMockFile() throws Exception {
        String tenant = "diku";
        String okapiUrl = "http://okapi";

        MultiMap headers = MultiMap.caseInsensitiveMultiMap()
                .add(Constants.X_OKAPI_TENANT, tenant)
                .add(Constants.X_OKAPI_URL, okapiUrl);

        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getHeader(Constants.X_OKAPI_TENANT)).thenReturn(tenant);
        when(request.getHeader(Constants.X_OKAPI_URL)).thenReturn(okapiUrl);
        when(request.headers()).thenReturn(headers);

        RoutingContext context = mock(RoutingContext.class);
        when(context.request()).thenReturn(request);

        String settingsResponse = Files.readString(Paths.get(TestConstants.PATH_TO_MOCK_FILES + "ncip-settings.json"));
        String addressTypesResponse = new io.vertx.core.json.JsonObject()
                .put("addressTypes", new io.vertx.core.json.JsonArray())
                .encode();

        FolioNcipHelper helper = new StubFolioNcipHelper(Promise.promise(), settingsResponse, addressTypesResponse);
        helper.initNcipProperties(context);

        Properties loaded = (Properties) helper.ncipProperties.get(tenant);
        assertNotNull(loaded);

        // Proves grouped per-agency mod-settings file is consumed and mapped.
        assertEquals("text", loaded.getProperty("rapid.instance.type.name"));
        assertEquals("text", loaded.getProperty("rapido.instance.type.name"));

        // Defaults from ncip.properties should still be present.
        assertEquals("other", loaded.getProperty("cancel.request.reason.name"));
    }

    private static class StubFolioNcipHelper extends FolioNcipHelper {
        private final String settingsResponse;
        private final String addressTypesResponse;

        StubFolioNcipHelper(Promise<Void> promise, String settingsResponse, String addressTypesResponse) {
            super(promise);
            this.settingsResponse = settingsResponse;
            this.addressTypesResponse = addressTypesResponse;
        }

        @Override
        public String callApiGet(String uriString, MultiMap okapiHeaders) {
            if (uriString.contains(Constants.SETTINGS_URL)) {
                return settingsResponse;
            }
            if (uriString.contains(Constants.ADDRESS_TYPES)) {
                return addressTypesResponse;
            }
            throw new IllegalArgumentException("Unexpected URL: " + uriString);
        }
    }
}
