package org.folio.ncip;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.Promise;
import java.io.InputStream;
import java.util.Scanner;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import static org.folio.ncip.Constants.SYS_PORT;
import static org.folio.ncip.Constants.DEFAULT_PORT;

public class MainVerticle extends AbstractVerticle {

	private static final Logger logger = LogManager.getLogger(MainVerticle.class);

	private static FolioNcipHelper folioNcipHelper;

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		final String portStr = System.getProperty(SYS_PORT, DEFAULT_PORT);
		final int port = Integer.parseInt(portStr);
		logger.info("mod-ncip is using port: " + port);

		final Promise<Void> helperPromise = Promise.promise();
		folioNcipHelper = new FolioNcipHelper(helperPromise);

		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		router.route(HttpMethod.POST, "/_/tenant").handler(this::handleTenant);
		router.route(HttpMethod.GET, "/_/tenant/:id").handler(this::handleTenantGet);
		router.route(HttpMethod.DELETE, "/_/tenant/:id").handler(this::handleTenantDelete);
		router.route(HttpMethod.POST, "/ncip").handler(this::handleNcip);
		router.route(HttpMethod.GET, "/ncipconfigcheck").handler(this::ncipConfigCheck);
		router.route(HttpMethod.GET, "/admin/health").handler(this::healthCheck);
		final Promise<HttpServer> serverPromise = Promise.promise();
		vertx.createHttpServer()
				.requestHandler(router)
				.listen(port)
				.onSuccess(serverPromise::complete)
				.onFailure(serverPromise::fail);
		helperPromise.future()
				.compose(x -> serverPromise.future())
				.<Void>mapEmpty()
				.onComplete(startPromise);
	}

	protected void handleTenant(RoutingContext ctx) {
		ConfigToSettingsMigrationService migrationService = new ConfigToSettingsMigrationService();
		migrationService.process(ctx)
				.onSuccess(v -> ctx.response().setStatusCode(204).end())
				.onFailure(e -> {
					logger.error("Tenant initialization failed", e);
					ctx.fail(e);
				});
	}

	protected void handleTenantGet(RoutingContext ctx) {
  		ctx.response()
    	  .setStatusCode(200)
    	  .putHeader("Content-Type", "application/json")
    	  .end("{\"complete\": true}");
	}

	protected void handleTenantDelete(RoutingContext ctx) {
  		 ctx.response().setStatusCode(204).end();
	}

	protected void ncipConfigCheck(RoutingContext ctx) {
		final Promise<Void> promise = Promise.promise();
		NcipConfigCheck ncipConfigCheck = new NcipConfigCheck(promise);
		promise.future().compose(x -> {
			try {
				ncipConfigCheck.process(ctx);
			} catch (Exception e) {
				return Future.failedFuture(e);
			}
			ctx.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, Constants.TEXT_PLAIN_STRING)
					.end(Constants.OK);
			return Future.succeededFuture();
		}).onFailure(e -> {
			logger.error("***************");
			logger.error(e.getMessage(), e);
			ctx.response()
					.setStatusCode(500)
					.putHeader(HttpHeaders.CONTENT_TYPE, Constants.APP_XML)
					.end("<Problem><message>problem processing NCIP request</message><exception>" + e.toString()
							+ "</exception></Problem>");
		});
	}

	protected void healthCheck(RoutingContext ctx) {
		logger.info("healthcheck called");
		ctx.response()
				.setStatusCode(200)
				.putHeader(HttpHeaders.CONTENT_TYPE, Constants.TEXT_PLAIN_STRING)
				.end(Constants.OK);
	}

	protected void handleNcip(RoutingContext ctx) {
		String requestId = ctx.request().getHeader("X-Okapi-Request-Id");
		String userId = ctx.request().getHeader("X-Okapi-User-Id");
		String tenant = ctx.request().getHeader("X-Okapi-Tenant");

		logger.info("tenant=" + tenant);
		logger.info("requestId=" + requestId);
		logger.info("calling mod-settings...");

		ctx.put("requestId", requestId);
		ctx.put("moduleId", "mod-ncip");
		ctx.put("tenantId", tenant);
		ctx.put("userId", userId);
		vertx.<String>executeBlocking(() -> {
			try (InputStream responseMsgInputStream = folioNcipHelper.ncipProcess(ctx);
					Scanner scanner = new Scanner(responseMsgInputStream, "UTF-8").useDelimiter("\\A")) {

				return scanner.hasNext() ? scanner.next() : "";

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).onComplete(res -> {

			if (res.failed()) {
				logger.error(
						"error occurred processing this request. Unable to construct NCIP response",
						res.cause());

				ctx.response()
						.setStatusCode(500)
						.putHeader(HttpHeaders.CONTENT_TYPE, "application/xml")
						.end("<Problem><message>problem processing NCIP request</message><exception>"
								+ res.cause().getLocalizedMessage()
								+ "</exception></Problem>");
				return;
			}

			ctx.response()
					.setStatusCode(200)
					.putHeader(HttpHeaders.CONTENT_TYPE, Constants.APP_XML)
					.end(res.result());
		});

	}

}
