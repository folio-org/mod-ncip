package org.folio.ncip;

import io.restassured.http.Header;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.extensiblecatalog.ncip.v2.common.MappedMessageHandler;
import org.extensiblecatalog.ncip.v2.common.MessageHandlerFactory;
import org.extensiblecatalog.ncip.v2.common.Translator;
import org.extensiblecatalog.ncip.v2.service.NCIPInitiationData;
import org.extensiblecatalog.ncip.v2.service.NCIPResponseData;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;
import org.kie.api.runtime.KieContainer;

import static org.junit.Assert.fail;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class MockServer {
	
	
	  private Translator translator; //for toolkit
	  private Properties toolkitProperties;
	  private Properties ncipProperties;
	  private KieContainer kieContainer;
	  private  ServiceContext serviceContext;
	  private static FolioNcipHelper folioNcipHelper;
	
	 private static final Logger logger = LoggerFactory.getLogger(MockServer.class);
	 

	
	
	
	  static HashMap<String, Integer> params = new HashMap<>();

	  private final int port;
	  private final Vertx vertx;

	  MockServer(int port) {
	    this.port = port;
	    this.vertx = Vertx.vertx();
	  }
	  
	  
	  void start() throws InterruptedException, ExecutionException, TimeoutException {
		    // Setup Mock Server...
		  final Promise<Void> promise = Promise.promise();
		   folioNcipHelper  = new FolioNcipHelper(promise);
		   HttpServer server = vertx.createHttpServer();
		   params.put("port", this.port);
		    CompletableFuture<HttpServer> deploymentComplete = new CompletableFuture<>();
		    server.requestHandler(defineRoutes()::accept).listen(port, result -> {
		      if(result.succeeded()) {
		        deploymentComplete.complete(result.result());
		      }
		      else {
		        deploymentComplete.completeExceptionally(result.cause());
		      }
		    });
		    deploymentComplete.get(60, TimeUnit.SECONDS);
		  }

		  void close() {
		    vertx.close(res -> {
		      if (res.failed()) {
		        logger.error("Failed to shut down mock server", res.cause());
		        fail(res.cause().getMessage());
		      } else {
		        logger.info("Successfully shut down mock server");
		      }
		    });
		  }
		  
		  private Router defineRoutes() {
			    Router router = Router.router(vertx);
			    router.route().handler(BodyHandler.create());
			    router.post("/test").handler(this::test);
			    router.post("/ncip").handler(this::ncip);
			    router.get("/groups/:id").handler(this::groupLookup);
			    router.get("/users").handler(this::users);
			    router.get("/circulation/loans").handler(this::circulation);
			    router.get("/accounts").handler(this::accounts);
			    router.get("/service-points-users").handler(this::servicePointUsers);
			    router.get("/manualblocks").handler(this::manualBlocks);
			    return router;
		  }
		  
		  private void groupLookup(RoutingContext ctx) {
			  String mockFileName =  TestConstants.PATH_TO_MOCK_FILES + "groups-get.json";
			  String body = readLineByLine(mockFileName);
			  serverResponse(ctx,200,APPLICATION_JSON,body);
		  }
		  
		  private void manualBlocks(RoutingContext ctx) {
			  String query = ctx.request().getParam("query");
			  String mockFileName =  TestConstants.PATH_TO_MOCK_FILES + "manualBlocks-get-noblocks.json";
			  if (query.contains(TestConstants.BLOCKED_PATRON_ID)) mockFileName = TestConstants.PATH_TO_MOCK_FILES + "manualBlocks-get-blocked.json";
			  String body = readLineByLine(mockFileName);
			  serverResponse(ctx,200,APPLICATION_JSON,body);
		  }
		  
		  private void users(RoutingContext ctx) {
			  String query = ctx.request().getParam("query");
			  String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get.json";
			  if (query.contains(TestConstants.BLOCKED_PATRON_BARCODE)) mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get-blocked.json";
			  if (query.contains(TestConstants.PATRON_DOESNT_EXIST_BARCODE)) mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get-notFound.json";
			  if (query.contains(TestConstants.BLOCKED_PATRON_BARCODE_BY_RULES)) mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get-blocked-fine.json";
			  String body = readLineByLine(mockFileName);
			  serverResponse(ctx,200,APPLICATION_JSON,body);
		  }
		  
		  private void circulation(RoutingContext ctx) {
			  String mockFileName =  TestConstants.PATH_TO_MOCK_FILES + "loans-get.json";
			  String body = readLineByLine(mockFileName);
			  serverResponse(ctx,200,APPLICATION_JSON,body);
		  }
		  
		  private void accounts(RoutingContext ctx) {
			  String query = ctx.request().getParam("query");
			  String mockFileName =  TestConstants.PATH_TO_MOCK_FILES + "accounts-get.json";
			  if (query.contains(TestConstants.BLOCKED_PATRON_ID_BY_RULES)) mockFileName = TestConstants.PATH_TO_MOCK_FILES + "accounts-get-large-fine.json";
			  String body = readLineByLine(mockFileName);
			  serverResponse(ctx,200,APPLICATION_JSON,body);
		  }
		  
		  private void servicePointUsers(RoutingContext ctx) {
			  String mockFileName =  TestConstants.PATH_TO_MOCK_FILES + "servicePoints-get.json";
			  String body = readLineByLine(mockFileName);
			  serverResponse(ctx,200,APPLICATION_JSON,body);
		  }
		  
		  private void serverResponse(RoutingContext ctx, int statusCode, String contentType, String body) {
			    ctx.response()
			      .setStatusCode(statusCode)
			      .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
			      .end(body);
			  }
		  
		  
		  private void test(RoutingContext ctx) {
			  
			  
			    logger.info("gotx: " + ctx.getBodyAsString());
			    String id = UUID.randomUUID().toString();
			    JsonObject body = ctx.getBodyAsJson();
			    
		  }
		  
		  private static String readLineByLine(String filePath) {
		      StringBuilder contentBuilder = new StringBuilder();
		      try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8))
		      {
		          stream.forEach(s -> contentBuilder.append(s).append("\n"));
		      }
		      catch (IOException e)
		      {
		          e.printStackTrace();
		      }
		      return contentBuilder.toString();
		  }
		  
		  
		  
		  protected void ncip(RoutingContext ctx) {
				 
			   vertx.executeBlocking(promise -> {
				   InputStream responseMsgInputStream = null;
				   try {
				   	    //FolioNcipHelper folioNcipHelper = new FolioNcipHelper(ctx);
				   		responseMsgInputStream = folioNcipHelper.ncipProcess(ctx);
						}
						catch(Exception e) {
							logger.error("error occured processing this request.  Unable to construct a proper NCIP response with problem element");
							logger.error(e.toString());
							ctx.response()
							.setStatusCode(500)
							.putHeader(HttpHeaders.CONTENT_TYPE, "application/xml") //TODO CONSTANT
							//THIS REALLY SHOULD BE AN NCIP RESONSE THAT MIRRORS THE NCIP REQUEST TYPE (WITH PROBLEM ELEMENT) HOWEVER...
							//THAT IS NOT POSSIBLE IF WE'VE REACHED HERE BECAUSE ONLY THE MESSAGE HANDLER CAN CONSTRUCT A RESPONSE OBJECT
							//WE SHOULDN'T EVER GET HERE - FAMOUS LAST WORDS
							.end("<Problem><message>probem processing NCIP request</message><exception>" + e.getLocalizedMessage() + "</exception></Problem>");
						}
		
					  String inputStreamString = new Scanner(responseMsgInputStream,"UTF-8").useDelimiter("\\A").next();
				      promise.complete(inputStreamString);
				}, res -> {
				  System.out.println("The result is: " + res.result());
				  ctx.response()
			      .setStatusCode(200)
			      .putHeader(HttpHeaders.CONTENT_TYPE, "application/xml") //TODO CONSTANT
			      .end(res.result().toString());
				});
			    
		 }
		  

		  
		  
		  
}
