package org.folio.ncip;


import org.apache.log4j.Logger;
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

import static org.folio.ncip.Constants.SYS_PORT;
import static org.folio.ncip.Constants.DEFAULT_PORT;


public class MainVerticle extends AbstractVerticle {

	private static final Logger logger = Logger.getLogger(MainVerticle.class);

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
		router.route(HttpMethod.POST, "/ncip").handler(this::handleNcip);
		router.route(HttpMethod.GET, "/ncipconfigcheck").handler(this::ncipConfigCheck);
		router.route(HttpMethod.GET, "/admin/health").handler(this::healthCheck);
		final Promise<HttpServer> serverPromise = Promise.promise();
		vertx.createHttpServer().requestHandler(router).listen(port, serverPromise);

		helperPromise.future()
		.compose(x -> serverPromise.future())
		.<Void>mapEmpty()
		.onComplete(startPromise);
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
			.end("<Problem><message>problem processing NCIP request</message><exception>" + e.toString()+ "</exception></Problem>");
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

		vertx.executeBlocking(promise -> {
			try (InputStream responseMsgInputStream = folioNcipHelper.ncipProcess(ctx);
					Scanner scanner = new Scanner(responseMsgInputStream,"UTF-8").useDelimiter("\\A")) {
				String inputStreamString = scanner.next();
				promise.complete(inputStreamString);
			}
			catch (Exception e) {
				promise.fail(e);
			}
		}, res -> {
			if (res.failed()) {
				logger.error("error occured processing this request.  Unable to construct a proper NCIP response with problem element",
						res.cause());
				ctx.response()
				.setStatusCode(500)
				.putHeader(HttpHeaders.CONTENT_TYPE, "application/xml") //TODO CONSTANT
				//THIS REALLY SHOULD BE AN NCIP RESONSE THAT MIRRORS THE NCIP REQUEST TYPE (WITH PROBLEM ELEMENT) HOWEVER...
				//THAT IS NOT POSSIBLE IF WE'VE REACHED HERE BECAUSE ONLY THE MESSAGE HANDLER CAN CONSTRUCT A RESPONSE OBJECT
				//WE SHOULDN'T EVER GET HERE IF THE MODULE IS SET UP PROPERLY - FAMOUS LAST WORDS
				.end("<Problem><message>problem processing NCIP request</message><exception>"
						+ res.cause().getLocalizedMessage() + "</exception></Problem>");
				return;
			}

			System.out.println("The result is: " + res.result());
			ctx.response()
			.setStatusCode(200)
			.putHeader(HttpHeaders.CONTENT_TYPE, Constants.APP_XML)
			.end(res.result().toString());
		});

	}


}
