package org.folio.ncip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class ConfigToSettingsMigrationServiceTest {

    @Test
    public void processSkipsBlankConfigNameAndWritesValidGroup() throws Exception {
        JsonArray legacyConfigs = new JsonArray()
                .add(new JsonObject()
                        .put(Constants.ID, "legacy-1")
                        .put(Constants.CONFIG_KEY, "relais")
                        .put(Constants.CODE_KEY, "checkout.service.point.id")
                        .put(Constants.VALUE_KEY, "sp-1"))
                .add(new JsonObject()
                        .put(Constants.ID, "legacy-2")
                        .put(Constants.CONFIG_KEY, "")
                        .put(Constants.CODE_KEY, "bad.code")
                        .put(Constants.VALUE_KEY, "ignored"));

        StubApi stub = new StubApi(legacyConfigs, 201);
        try {
            stub.start();

            ConfigToSettingsMigrationService service = new ConfigToSettingsMigrationService();
            service.process(mockContext(stub.baseUrl()))
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);

            assertEquals(1, stub.postCount.get());
            assertEquals(0, stub.putCount.get());
            assertEquals(2, stub.deleteCount.get());
            assertEquals(1, stub.settingsById.size());

            JsonObject onlySetting = stub.settingsById.values().iterator().next();
            assertEquals("relais", onlySetting.getString(Constants.KEY));
            assertEquals("sp-1", onlySetting.getJsonObject(Constants.VALUE_KEY)
                    .getString("checkout.service.point.id"));
        } finally {
            stub.close();
        }
    }

    @Test
    public void processUsesPutFallbackOnPostConflict() throws Exception {
        JsonArray legacyConfigs = new JsonArray()
                .add(new JsonObject()
                        .put(Constants.ID, "legacy-1")
                        .put(Constants.CONFIG_KEY, "relais")
                        .put(Constants.CODE_KEY, "checkout.service.point.id")
                        .put(Constants.VALUE_KEY, "sp-1"));

        StubApi stub = new StubApi(legacyConfigs, 409);
        try {
            stub.start();

            ConfigToSettingsMigrationService service = new ConfigToSettingsMigrationService();
            service.process(mockContext(stub.baseUrl()))
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(10, TimeUnit.SECONDS);

            assertEquals(1, stub.postCount.get());
            assertEquals(1, stub.putCount.get());
            assertEquals(1, stub.settingsById.size());
        } finally {
            stub.close();
        }
    }

    @Test
    public void processFailsOnNonConflictPostError() throws Exception {
        JsonArray legacyConfigs = new JsonArray()
                .add(new JsonObject()
                        .put(Constants.ID, "legacy-1")
                        .put(Constants.CONFIG_KEY, "relais")
                        .put(Constants.CODE_KEY, "checkout.service.point.id")
                        .put(Constants.VALUE_KEY, "sp-1"));

        StubApi stub = new StubApi(legacyConfigs, 500);
        try {
            stub.start();

            ConfigToSettingsMigrationService service = new ConfigToSettingsMigrationService();
            CompletableFuture<Void> result = service.process(mockContext(stub.baseUrl()))
                    .toCompletionStage()
                    .toCompletableFuture();

            boolean failedAsExpected = false;
            try {
                result.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                failedAsExpected = true;
            }

            assertTrue(failedAsExpected);
            assertEquals(1, stub.postCount.get());
            assertEquals(0, stub.putCount.get());
            assertEquals(0, stub.deleteCount.get());
        } finally {
            stub.close();
        }
    }

    private RoutingContext mockContext(String baseUrl) {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        MultiMap headers = MultiMap.caseInsensitiveMultiMap().add(Constants.X_OKAPI_URL, baseUrl);

        when(context.request()).thenReturn(request);
        when(request.headers()).thenReturn(headers);
        return context;
    }

    private static class StubApi {
        private final Vertx vertx = Vertx.vertx();
        private final JsonArray legacyConfigs;
        private final int postStatus;

        private HttpServer server;
        private int port;

        private final AtomicInteger postCount = new AtomicInteger();
        private final AtomicInteger putCount = new AtomicInteger();
        private final AtomicInteger deleteCount = new AtomicInteger();
        private final Map<String, JsonObject> settingsById = new ConcurrentHashMap<>();

        StubApi(JsonArray legacyConfigs, int postStatus) {
            this.legacyConfigs = legacyConfigs;
            this.postStatus = postStatus;
        }

        void start() throws Exception {
            port = Utils.nextFreePort();
            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());

            router.get("/configurations/entries").handler(ctx -> {
                JsonObject response = new JsonObject().put(Constants.CONFIGS, legacyConfigs);
                ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json")
                        .end(response.encode());
            });

            router.post(Constants.SETTINGS_URL).handler(ctx -> {
                postCount.incrementAndGet();
                JsonObject setting = ctx.body().asJsonObject();

                if (postStatus == 201) {
                    settingsById.put(setting.getString(Constants.ID), setting);
                    ctx.response().setStatusCode(201).putHeader("Content-Type", "application/json")
                            .end(setting.encode());
                    return;
                }

                if (postStatus == 409) {
                    JsonObject error = new JsonObject().put("errors", new JsonArray()
                            .add(new JsonObject().put("message", "already exists")));
                    ctx.response().setStatusCode(409).putHeader("Content-Type", "application/json")
                            .end(error.encode());
                    return;
                }

                JsonObject error = new JsonObject().put("errors", new JsonArray()
                        .add(new JsonObject().put("message", "unexpected failure")));
                ctx.response().setStatusCode(500).putHeader("Content-Type", "application/json")
                        .end(error.encode());
            });

            router.put(Constants.SETTINGS_URL + "/:id").handler(ctx -> {
                putCount.incrementAndGet();
                JsonObject setting = ctx.body().asJsonObject();
                settingsById.put(ctx.pathParam("id"), setting);
                ctx.response().setStatusCode(204).end();
            });

            router.get(Constants.SETTINGS_URL + "/:id").handler(ctx -> {
                JsonObject setting = settingsById.get(ctx.pathParam("id"));
                if (setting == null) {
                    ctx.response().setStatusCode(404).end();
                } else {
                    ctx.response().setStatusCode(200).putHeader("Content-Type", "application/json")
                            .end(setting.encode());
                }
            });

            router.delete("/configurations/entries/:id").handler(ctx -> {
                deleteCount.incrementAndGet();
                ctx.response().setStatusCode(204).end();
            });

            server = vertx.createHttpServer();
            CompletableFuture<HttpServer> started = new CompletableFuture<>();
            server.requestHandler(router)
                    .listen(port)
                    .onSuccess(started::complete)
                    .onFailure(started::completeExceptionally);
            started.get(10, TimeUnit.SECONDS);
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        void close() throws Exception {
            if (server != null) {
                server.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
            }
            vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        }
    }
}
