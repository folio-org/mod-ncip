package org.folio.ncip;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.Promise;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {
	@Rule
	public Timeout timeout = Timeout.seconds(10);

	@Rule
	public RunTestOnContext runTestOnContext = new RunTestOnContext();

	private Vertx vertx;

	@BeforeClass
	public static void beforeClass() {
		RestAssured.reset();
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	}

	@Before
	public void before(TestContext ctx) {
		vertx = runTestOnContext.vertx();
		vertx.exceptionHandler(ctx.exceptionHandler());
	}

	@After
	public void after() {
		System.clearProperty(Constants.SYS_PORT);
	}

	@Test
	public void health(TestContext ctx) {
		int appPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.deployVerticle(new MainVerticle())
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, null).get("/admin/health").then().statusCode(200).body(is("OK"));
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void ncipConfigCheck(TestContext ctx) {
		int appPort = findFreePort();
		int okapiPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		String settings = new JsonObject().put("items", new JsonArray()
				.add(new JsonObject()
						.put("key", "rapid")
						.put("value", new JsonObject().put("instance.type.name", "bar"))))
				.encodePrettily();
		String instanceTypes = new JsonObject().put("instanceTypes", new JsonArray()
				.add(new JsonObject()))
				.encodePrettily();

		vertx.createHttpServer()
				.requestHandler(req -> {
					switch (req.path()) {
						case "/settings/entries":
							req.response().setStatusCode(200).end(settings);
							break;
						case "/instance-types":
							req.response().setStatusCode(200).end(instanceTypes);
							break;
						default:
							req.response().setStatusCode(500).end("Bad path " + req.path());
							break;
					}
				})
				.listen(okapiPort)
				.compose(x -> vertx.deployVerticle(new MainVerticle()))
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, okapiPort).get("/ncipconfigcheck").then().statusCode(200).body(is("OK"));
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void ncipConfigCheckMultipleAgencies(TestContext ctx) {
		int appPort = findFreePort();
		int okapiPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		String settings = new JsonObject().put("items", new JsonArray()
				.add(new JsonObject()
						.put("key", "relais")
						.put("value", new JsonObject().put("instance.type.name", "bar")))
				.add(new JsonObject()
						.put("key", "test")
						.put("value", new JsonObject().put("instance.type.name", "bar")))
				.add(new JsonObject()
						.put("key", "toolkit")
						.put("value", new JsonObject().put("some.toolkit.value", "x"))))
				.encodePrettily();
		String instanceTypes = new JsonObject().put("instanceTypes", new JsonArray()
				.add(new JsonObject()))
				.encodePrettily();

		vertx.createHttpServer()
				.requestHandler(req -> {
					switch (req.path()) {
						case "/settings/entries":
							req.response().setStatusCode(200).end(settings);
							break;
						case "/instance-types":
							req.response().setStatusCode(200).end(instanceTypes);
							break;
						default:
							req.response().setStatusCode(500).end("Bad path " + req.path());
							break;
					}
				})
				.listen(okapiPort)
				.compose(x -> vertx.deployVerticle(new MainVerticle()))
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, okapiPort).get("/ncipconfigcheck").then().statusCode(200).body(is("OK"));
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void ncipConfigCheckFailure(TestContext ctx) {
		int appPort = findFreePort();
		int okapiPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.deployVerticle(new MainVerticle())
				.onComplete(ctx.asyncAssertSuccess(y -> {
					vertx.executeBlocking(() -> {
						request(appPort, okapiPort).get("/ncipconfigcheck").then().statusCode(500)
								.body(containsString("problem processing NCIP request"));
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void tenantInitMigratesAndReturnsNoContent(TestContext ctx) {
		int appPort = findFreePort();
		int okapiPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		Map<String, JsonObject> settingsById = new ConcurrentHashMap<>();
		String configsResponse = new JsonObject().put(Constants.CONFIGS, new JsonArray()
				.add(new JsonObject()
						.put(Constants.ID, "legacy-1")
						.put(Constants.CONFIG_KEY, "relais")
						.put(Constants.CODE_KEY, "checkout.service.point.id")
						.put(Constants.VALUE_KEY, "sp-1")))
				.encodePrettily();

		vertx.createHttpServer()
				.requestHandler(req -> {
					if (req.method().name().equals("GET") && req.path().equals("/configurations/entries")) {
						req.response().setStatusCode(200).end(configsResponse);
						return;
					}

					if (req.method().name().equals("POST") && req.path().equals(Constants.SETTINGS_URL)) {
						req.bodyHandler(body -> {
							JsonObject payload = body.toJsonObject();
							settingsById.put(payload.getString(Constants.ID), payload);
							req.response().setStatusCode(201).end(payload.encode());
						});
						return;
					}

					if (req.method().name().equals("GET") && req.path().startsWith(Constants.SETTINGS_URL + "/")) {
						String id = req.path().substring((Constants.SETTINGS_URL + "/").length());
						JsonObject payload = settingsById.get(id);
						if (payload == null) {
							req.response().setStatusCode(404).end();
						} else {
							req.response().setStatusCode(200).end(payload.encode());
						}
						return;
					}

					if (req.method().name().equals("DELETE") && req.path().startsWith("/configurations/entries/")) {
						req.response().setStatusCode(204).end();
						return;
					}

					req.response().setStatusCode(500).end("Bad path " + req.method() + " " + req.path());
				})
				.listen(okapiPort)
				.compose(x -> vertx.deployVerticle(new MainVerticle()))
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, okapiPort)
								.post("/_/tenant")
								.then()
								.statusCode(204);
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void tenantInitFailureReturnsServerError(TestContext ctx) {
		int appPort = findFreePort();
		int okapiPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.createHttpServer()
				.requestHandler(req -> req.response().setStatusCode(500).end("fail"))
				.listen(okapiPort)
				.compose(x -> vertx.deployVerticle(new MainVerticle()))
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, okapiPort)
								.post("/_/tenant")
								.then()
								.statusCode(500);
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void tenantGetReturnsCompleteTrue(TestContext ctx) {
		int appPort = findFreePort();

		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.deployVerticle(new MainVerticle())
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, null)
								.get("/_/tenant/test-job-id")
								.then()
								.statusCode(200)
								.body("complete", is(true));

						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void tenantDeleteReturnsNoContent(TestContext ctx) {
		int appPort = findFreePort();

		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.deployVerticle(new MainVerticle())
				.onComplete(ctx.asyncAssertSuccess(x -> {
					vertx.executeBlocking(() -> {
						request(appPort, null)
								.delete("/_/tenant")
								.then()
								.statusCode(204);

						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void ncipReturnsSuccessWhenHelperSucceeds(TestContext ctx) {
		int appPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.deployVerticle(new MainVerticle())
				.onComplete(ctx.asyncAssertSuccess(x -> {
					setHelperForTest(new StubMainVerticleHelper(false));
					vertx.executeBlocking(() -> {
						request(appPort, null)
								.header("X-Okapi-Request-Id", "req-1")
								.header("X-Okapi-User-Id", "user-1")
								.body("<request/>")
								.post("/ncip")
								.then()
								.statusCode(200)
								.body(is("<mock-ncip-response/>"));
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	@Test
	public void ncipReturnsProblemWhenHelperFails(TestContext ctx) {
		int appPort = findFreePort();
		System.setProperty(Constants.SYS_PORT, String.valueOf(appPort));

		vertx.deployVerticle(new MainVerticle())
				.onComplete(ctx.asyncAssertSuccess(x -> {
					setHelperForTest(new StubMainVerticleHelper(true));
					vertx.executeBlocking(() -> {
						request(appPort, null)
								.header("X-Okapi-Request-Id", "req-2")
								.header("X-Okapi-User-Id", "user-2")
								.body("<request/>")
								.post("/ncip")
								.then()
								.statusCode(500)
								.body(containsString("problem processing NCIP request"));
						return null;
					}).onComplete(ctx.asyncAssertSuccess());
				}));
	}

	private RequestSpecification request(int appPort, Integer okapiPort) {
		RequestSpecification request = RestAssured.given()
				.port(appPort)
				.header("X-Okapi-Tenant", "diku");

		if (okapiPort != null) {
			request.header("X-Okapi-Url", "http://localhost:" + okapiPort);
		}

		return request;
	}

	private int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to allocate test port", e);
		}
	}

	private void setHelperForTest(FolioNcipHelper helper) {
		try {
			Field field = MainVerticle.class.getDeclaredField("folioNcipHelper");
			field.setAccessible(true);
			field.set(null, helper);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to replace FolioNcipHelper for test", e);
		}
	}

	private static class StubMainVerticleHelper extends FolioNcipHelper {
		private final boolean fail;

		StubMainVerticleHelper(boolean fail) {
			super(Promise.promise());
			this.fail = fail;
		}

		@Override
		public InputStream ncipProcess(RoutingContext context) throws Exception {
			if (fail) {
				throw new Exception("mock failure");
			}
			return new ByteArrayInputStream("<mock-ncip-response/>".getBytes(StandardCharsets.UTF_8));
		}
	}
}
