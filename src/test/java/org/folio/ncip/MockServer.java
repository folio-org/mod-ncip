package org.folio.ncip;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.extensiblecatalog.ncip.v2.common.Translator;
import org.extensiblecatalog.ncip.v2.service.ServiceContext;

import static org.junit.Assert.fail;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import org.folio.okapi.common.XOkapiHeaders;
import org.folio.okapi.common.logging.FolioLoggingContext;

import javax.ws.rs.core.MediaType;

public class MockServer {


    private Translator translator; //for toolkit
    private Properties toolkitProperties;
    private Properties ncipProperties;
    private ServiceContext serviceContext;
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
        folioNcipHelper = new FolioNcipHelper(promise);  //TODO????
        //WebContext context = new WebContext(routingContext);
        HttpServer server = vertx.createHttpServer();
        params.put("port", this.port);
        CompletableFuture<HttpServer> deploymentComplete = new CompletableFuture<>();
        server.requestHandler(defineRoutes()).listen(port, result -> {
            if (result.succeeded()) {
                deploymentComplete.complete(result.result());
            } else {
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
        router.get("/service-points-users").handler(this::servicePointUsers);
        router.get("/service-points").handler(this::servicePointUsers);
        router.get("/manualblocks").handler(this::manualBlocks);
        router.get("/automated-patron-blocks/:id").handler(this::automatedBlocks);
        //router.get("/configurations/entries/maxloancount").handler(this::getMaxLoanCount);
        //router.get("/configurations/entries/maxfineamount").handler(this::getMaxFineAmount);
        router.get("/configurations/entries/toolkit").handler(this::getToolkitCofigs);
        router.get("/configurations/entries").handler(this::getNcipConfigs);
        router.get("/inventory/items").handler(this::items);
        router.post("/inventory/items").handler(this::itemsPost);
        router.get("/inventory/instances").handler(this::instances);
        router.post("/inventory/instances").handler(this::instancePost);
        router.get("/holdings-storage/holdings/:id").handler(this::holdingsById);
        router.post("/holdings-storage/holdings").handler(this::holdingsById);
        router.post("/circulation/requests").handler(this::requestsPost);
        router.get("/addresstypes").handler(this::addressTypes);
        router.get("/instance-types").handler(this::instanceTypes);
        router.get("/identifier-types").handler(this::identifierTypes);
        router.get("/material-types").handler(this::materialTypes);
        router.get("/loan-types").handler(this::loanTypes);
        router.get("/locations").handler(this::locations);
        router.get("/holdings-sources").handler(this::holdingsSources);
        router.get("/cancellation-reason-storage/cancellation-reasons").handler(this::cancellationReason);
        router.get("/circulation/requests/:id").handler(this::getCirculationRequestById);
        router.get("/circulation/requests").handler(this::getCirculationRequestList);
        router.put("/circulation/requests/:id").handler(this::putCirculationRequestById);
        router.post("/patron-pin/verify").handler(this::verifyPing);
        router.get("/owners").handler(this::getFeeOwner);
        router.get("/feefines").handler(this::findFee);
        router.post("/accounts").handler(this::postAccounts);
        router.get("/note-types").handler(this::getNoteTypes);
        router.post("/circulation/check-out-by-barcode").handler(this::postLoan);

        return router;
    }

    private void getNcipConfigs(RoutingContext ctx) {

        MultiMap params = ctx.request().params();
        List<String> param = params.getAll("query");
        String toolkit = "configName=toolkit";


        param.contains(toolkit);
        if (param.contains(toolkit)) {
            ctx.reroute("/configurations/entries/toolkit");
        }


        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "ncip-configs.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void getToolkitCofigs(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "toolkit-configs.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void groupLookup(RoutingContext ctx) {
        String mockFileName;
        if (ctx.request().path().contains("dac3aab4-7a86-422b-b5fc-6fbf7afd7aea")) {
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "groups-getStaff.json";
        } else {
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "groups-get.json";
        }
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void manualBlocks(RoutingContext ctx) {
        String query = ctx.request().getParam("query");
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "manualBlocks-get-noblocks.json";
        if (query.contains(TestConstants.BLOCKED_PATRON_ID))
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "manualBlocks-get-blocked.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void automatedBlocks(RoutingContext ctx) {
        String query = ctx.request().getParam("id");
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "automatedBlocks-get-noblocks.json";
        if (query.contains(TestConstants.BLOCKED_PATRON_ID_BY_AUTOMATED))
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "automatedBlocks-get-blocked.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void users(RoutingContext ctx) {
        String query = ctx.request().getParam("query");
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get.json";
        if (query.contains(TestConstants.BLOCKED_PATRON_BARCODE))
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get-blocked.json";
        if (query.contains(TestConstants.PATRON_DOESNT_EXIST_BARCODE))
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get-notFound.json";
        if (query.contains(TestConstants.BLOCKED_PATRON_BARCODE_BY_AUTOMATED))
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "usersByBarcode-get-blocked-fine.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void items(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "itemsByHrid-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void itemsPost(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "items-post.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void instances(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "instancesByHrid-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void instancePost(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "instances-post.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void holdingsById(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "holdingsById-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void addressTypes(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "addressTypes-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void requestsPost(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "requests-post.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void servicePointUsers(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "servicePoints-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void instanceTypes(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "instanceTypes-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void identifierTypes(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "identifierTypes-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void materialTypes(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "mTypes-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void loanTypes(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "loanTypes-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void locations(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "locations-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void holdingsSources(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "holdingsSources-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void cancellationReason(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "cancellationReason-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void getCirculationRequestById(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "circulationRequest-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void getCirculationRequestList(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "circulationRequestList-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void putCirculationRequestById(RoutingContext ctx) {
        ctx.response().setStatusCode(204).end();
    }

    private void verifyPing(RoutingContext ctx) {
        String body = ctx.body().asString();
        ctx.response()
                .setStatusCode(body.contains("5678") ? 422 : 200)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .end();
    }

    private void getFeeOwner(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "feeOwners-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void findFee(RoutingContext ctx) {
        String query = ctx.request().getParam("query");
        String mockFileName;
        if (query.contains("staff")) {
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "feefines-getEmpty.json";
        } else {
            mockFileName = TestConstants.PATH_TO_MOCK_FILES + "feefines-get.json";
        }
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void postAccounts(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "accounts-post.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void getNoteTypes(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "noteTypes-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
    }

    private void postLoan(RoutingContext ctx) {
        String mockFileName = TestConstants.PATH_TO_MOCK_FILES + "loan-get.json";
        String body = readLineByLine(mockFileName);
        serverResponse(ctx, 200, APPLICATION_JSON, body);
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
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }


    protected void ncip(RoutingContext ctx) {

        String requestId = ctx.request().headers().get(XOkapiHeaders.REQUEST_ID);
        String userId = ctx.request().headers().get(XOkapiHeaders.USER_ID);
        String tenant = ctx.request().headers().get(XOkapiHeaders.TENANT);
        FolioLoggingContext.put(FolioLoggingContext.REQUEST_ID_LOGGING_VAR_NAME, requestId);
        FolioLoggingContext.put(FolioLoggingContext.MODULE_ID_LOGGING_VAR_NAME, "mod-ncip");
        FolioLoggingContext.put(FolioLoggingContext.TENANT_ID_LOGGING_VAR_NAME, tenant);
        FolioLoggingContext.put(FolioLoggingContext.USER_ID_LOGGING_VAR_NAME, userId);

        vertx.executeBlocking(promise -> {
            InputStream responseMsgInputStream = null;
            try {
                //FolioNcipHelper folioNcipHelper = new FolioNcipHelper(ctx);
                responseMsgInputStream = folioNcipHelper.ncipProcess(ctx);
            } catch (Exception e) {
                logger.error("error occured processing this request.  Unable to construct a proper NCIP response with problem element");
                logger.error(e.toString());
                ctx.response()
                        .setStatusCode(500)
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/xml") //TODO CONSTANT
                        //THIS REALLY SHOULD BE AN NCIP RESONSE THAT MIRRORS THE NCIP REQUEST TYPE (WITH PROBLEM ELEMENT) HOWEVER...
                        //THAT IS NOT POSSIBLE IF WE'VE REACHED HERE BECAUSE ONLY THE MESSAGE HANDLER CAN CONSTRUCT A RESPONSE OBJECT
                        //WE SHOULDN'T EVER GET HERE - FAMOUS LAST WORDS
                        .end("<Problem><message>problem processing NCIP request</message><exception>" + e.getLocalizedMessage() + "</exception></Problem>");
            }

            String inputStreamString = new Scanner(responseMsgInputStream, "UTF-8").useDelimiter("\\A").next();
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
