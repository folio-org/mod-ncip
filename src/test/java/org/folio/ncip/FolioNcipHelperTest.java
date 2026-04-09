package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Test;

public class FolioNcipHelperTest {

    @Test
    public void initToolkitDoesNotLeakOverridesAcrossTenants() throws Exception {
        String okapiUrl = "http://okapi";
        String tenantA = "tenantA";
        String tenantB = "tenantB";

        String toolkitResponseA = new io.vertx.core.json.JsonObject()
                .put("items", new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "toolkit")
                                .put("value", new io.vertx.core.json.JsonObject()
                                        .put("test.shared.override", "one"))))
                .encode();

        String toolkitResponseB = new io.vertx.core.json.JsonObject()
                .put("items", new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "toolkit")
                                .put("value", new io.vertx.core.json.JsonObject()
                                        .put("test.shared.override", "two"))))
                .encode();

        MutableToolkitStubFolioNcipHelper helper = new MutableToolkitStubFolioNcipHelper(Promise.promise());

        RoutingContext contextA = buildContext(tenantA, okapiUrl);
        helper.initializeTenantToolkitState(tenantA);
        helper.setToolkitResponse(toolkitResponseA);
        helper.initToolkit(contextA);

        RoutingContext contextB = buildContext(tenantB, okapiUrl);
        helper.initializeTenantToolkitState(tenantB);
        helper.setToolkitResponse(toolkitResponseB);
        helper.initToolkit(contextB);

        Properties tenantAProps = (Properties) helper.toolkitProperties.get(tenantA);
        Properties tenantBProps = (Properties) helper.toolkitProperties.get(tenantB);
        Properties defaults = (Properties) helper.defaultToolkitObjects.get("toolkit");

        assertEquals("one", tenantAProps.getProperty("test.shared.override"));
        assertEquals("two", tenantBProps.getProperty("test.shared.override"));
        assertTrue(defaults.getProperty("test.shared.override") == null);
    }

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

    @Test
    public void getConfigValueReturnsFirstConfigurationValue() {
        String configResponse = new io.vertx.core.json.JsonObject()
                .put(Constants.CONFIGS, new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject().put(Constants.VALUE_KEY, "configured-value")))
                .encode();

        FolioNcipHelper helper = new ConfigLookupStubFolioNcipHelper(Promise.promise(), configResponse, false);
        MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(Constants.X_OKAPI_URL, "http://okapi");

        assertEquals("configured-value", helper.getConfigValue("my.code", headers));
    }

    @Test
    public void getConfigValueReturnsNullOnApiFailure() {
        FolioNcipHelper helper = new ConfigLookupStubFolioNcipHelper(Promise.promise(), "", true);
        MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(Constants.X_OKAPI_URL, "http://okapi");

        assertNull(helper.getConfigValue("my.code", headers));
    }

    @Test
    public void processErrorResponseParsesJsonErrors() {
        FolioNcipHelper helper = new StubFolioNcipHelper(Promise.promise(), "{}", "{}");
        String body = new io.vertx.core.json.JsonObject()
                .put("errors", new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject().put("message", "first"))
                        .add(new io.vertx.core.json.JsonObject().put("message", "second")))
                .encode();

        assertEquals("ERROR: firstsecond", helper.processErrorResponse(body));
    }

    @Test
    public void initToolkitKeepsDefaultsWhenNoSettingsItemsExist() throws Exception {
        String tenant = "diku";
        MutableToolkitStubFolioNcipHelper helper = new MutableToolkitStubFolioNcipHelper(Promise.promise());
        helper.initializeTenantToolkitState(tenant);
        helper.setToolkitResponse(new io.vertx.core.json.JsonObject().put("items", new io.vertx.core.json.JsonArray())
                .encode());

        helper.initToolkit(buildContext(tenant, "http://okapi"));

        Properties tenantProps = (Properties) helper.toolkitProperties.get(tenant);
        assertNotNull(tenantProps);
    }

    @Test
    public void initToolkitKeepsDefaultsWhenSettingsValueObjectMissing() throws Exception {
        String tenant = "diku";
        MutableToolkitStubFolioNcipHelper helper = new MutableToolkitStubFolioNcipHelper(Promise.promise());
        helper.initializeTenantToolkitState(tenant);
        helper.setToolkitResponse(new io.vertx.core.json.JsonObject()
                .put("items", new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject().put("key", "toolkit")))
                .encode());

        helper.initToolkit(buildContext(tenant, "http://okapi"));

        Properties tenantProps = (Properties) helper.toolkitProperties.get(tenant);
        assertNotNull(tenantProps);
    }

    @Test
    public void initNcipPropertiesSkipsBlankKeysMissingValuesAndNullFields() throws Exception {
        String tenant = "diku";
        String okapiUrl = "http://okapi";

        String settingsResponse = new io.vertx.core.json.JsonObject()
                .put("items", new io.vertx.core.json.JsonArray()
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "toolkit")
                                .put("value", new io.vertx.core.json.JsonObject().put("ignored", "value")))
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "")
                                .put("value", new io.vertx.core.json.JsonObject().put("ignored", "value")))
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "relais"))
                        .add(new io.vertx.core.json.JsonObject()
                                .put("key", "rapid")
                                .put("value", new io.vertx.core.json.JsonObject()
                                        .put("valid.code", "text")
                                        .putNull("null.code"))))
                .encode();

        String addressTypesResponse = new io.vertx.core.json.JsonObject()
                .put("addressTypes", new io.vertx.core.json.JsonArray())
                .encode();

        FolioNcipHelper helper = new StubFolioNcipHelper(Promise.promise(), settingsResponse, addressTypesResponse);
        helper.initNcipProperties(buildContext(tenant, okapiUrl));

        Properties loaded = (Properties) helper.ncipProperties.get(tenant);
        assertEquals("text", loaded.getProperty("rapid.valid.code"));
        assertNull(loaded.getProperty("rapid.null.code"));
        assertNull(loaded.getProperty("relais.valid.code"));
    }

    @Test
    public void setUpMappingExecutesWithoutError() throws Exception {
        FolioNcipHelper helper = new StubFolioNcipHelper(Promise.promise(), "{}", "{}");
        Method method = FolioNcipHelper.class.getDeclaredMethod("setUpMapping");
        method.setAccessible(true);

        method.invoke(helper);
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

    private static class MutableToolkitStubFolioNcipHelper extends FolioNcipHelper {
        private String toolkitResponse = new io.vertx.core.json.JsonObject()
                .put("items", new io.vertx.core.json.JsonArray())
                .encode();

        MutableToolkitStubFolioNcipHelper(Promise<Void> promise) {
            super(promise);
        }

        void setToolkitResponse(String toolkitResponse) {
            this.toolkitResponse = toolkitResponse;
        }

        @Override
        public String callApiGet(String uriString, MultiMap okapiHeaders) {
            if (uriString.contains(Constants.SETTINGS_URL)) {
                return toolkitResponse;
            }
            if (uriString.contains(Constants.ADDRESS_TYPES)) {
                return new io.vertx.core.json.JsonObject().put("addressTypes", new io.vertx.core.json.JsonArray())
                        .encode();
            }
            throw new IllegalArgumentException("Unexpected URL: " + uriString);
        }
    }

    private static class ConfigLookupStubFolioNcipHelper extends FolioNcipHelper {
        private final String response;
        private final boolean throwError;

        ConfigLookupStubFolioNcipHelper(Promise<Void> promise, String response, boolean throwError) {
            super(promise);
            this.response = response;
            this.throwError = throwError;
        }

        @Override
        public String callApiGet(String uriString, MultiMap okapiHeaders) throws Exception {
            if (throwError) {
                throw new Exception("mock failure");
            }
            return response;
        }
    }

    private RoutingContext buildContext(String tenant, String okapiUrl) {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap()
                .add(Constants.X_OKAPI_TENANT, tenant)
                .add(Constants.X_OKAPI_URL, okapiUrl);

        HttpServerRequest request = mock(HttpServerRequest.class);
        when(request.getHeader(Constants.X_OKAPI_TENANT)).thenReturn(tenant);
        when(request.getHeader(Constants.X_OKAPI_URL)).thenReturn(okapiUrl);
        when(request.headers()).thenReturn(headers);

        RoutingContext context = mock(RoutingContext.class);
        when(context.request()).thenReturn(request);
        return context;
    }
}
