package org.folio.ncip;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
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

	private Vertx vertx;

	@BeforeClass
	public static void beforeClass() {
		RestAssured.reset();
		RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
		RestAssured.port = 8081;
		RestAssured.requestSpecification = new RequestSpecBuilder()
				.addHeader("X-Okapi-Tenant", "diku")
				.addHeader("X-Okapi-Url", "http://localhost:8082")
				.build();
	}

	@Before
	public void before(TestContext ctx) {
		vertx = Vertx.vertx();
		vertx.exceptionHandler(ctx.exceptionHandler());
	}

	@Test
	public void health(TestContext ctx) {
		vertx.deployVerticle(new MainVerticle(), success(ctx, x -> {
			get("/admin/health").
			then().
			statusCode(200).
			body(is("OK"));
		}));
	}

	@Test
	public void ncipConfigCheck(TestContext ctx) {
		String configs = new JsonObject().put("configs", new JsonArray()
				.add(new JsonObject().put("code", "instance.type.name").put("value", "bar")))
				.encodePrettily();
		String instanceTypes = new JsonObject().put("instanceTypes", new JsonArray()
				.add(new JsonObject()))
				.encodePrettily();

		vertx.createHttpServer()
		.requestHandler(req -> {
			switch (req.path()) {
			case "/configurations/entries": req.response().setStatusCode(200).end(configs); break;
			case "/instance-types": req.response().setStatusCode(200).end(instanceTypes); break;
			default: req.response().setStatusCode(500).end("Bad path " + req.path()); break;
			}
		})
		.listen(8082, ctx.asyncAssertSuccess(x -> {
			vertx.deployVerticle(new MainVerticle(), success(ctx, y -> {
				get("/ncipconfigcheck").
				then().
				statusCode(200).
				body(is("OK"));
			}));
		}));
	}

	/**
	 * Like TestContext.asyncAssertSuccess, but use executeBlocking to run nextHandler
	 * so that the mock http server can respond.
	 */
	private <T> Handler<AsyncResult<T>> success(TestContext ctx, Handler<T> nextHandler) {
		return ctx.asyncAssertSuccess(value -> vertx.executeBlocking(promise -> {
			nextHandler.handle(value);
			promise.complete();
		}, ctx.asyncAssertSuccess()));
	}
}
