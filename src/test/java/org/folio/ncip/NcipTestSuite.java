package org.folio.ncip;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import io.vertx.core.Vertx;

import io.restassured.RestAssured;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  LookupUser.class, RequestItem.class, CancelRequestItem.class, DeleteItem.class, AcceptItem.class, CreateUserFiscalTransaction.class, CheckOutItem.class
})
public class NcipTestSuite {

	  private static final int okapiPort = Utils.nextFreePort();
	  static final int mockPort = Utils.nextFreePort();
	  private static Vertx vertx;
	  private static MockServer mockServer;
	  private static boolean initialised;

	  @BeforeClass
	  public static void before() throws InterruptedException, ExecutionException, TimeoutException {
	    if (vertx == null) {
	      vertx = Vertx.vertx();
	    }

	    mockServer = new MockServer(mockPort);
	    mockServer.start();

	    RestAssured.reset();
	    RestAssured.baseURI = "http://localhost:" + okapiPort;
	    RestAssured.port = okapiPort;
	    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
	  }

	  @AfterClass
	  public static void after() {
	    vertx.close();
	    mockServer.close();
	    initialised = false;
	  }



}
