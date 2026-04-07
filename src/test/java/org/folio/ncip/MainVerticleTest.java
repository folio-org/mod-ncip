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
import java.io.IOException;
import java.net.ServerSocket;
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
}
