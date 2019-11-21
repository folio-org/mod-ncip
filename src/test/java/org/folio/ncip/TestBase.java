package org.folio.ncip;

import static io.restassured.RestAssured.given;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient.Version;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * When not run from StorageTestSuite then this class invokes StorageTestSuite.before() and
 * StorageTestSuite.after() to allow to run a single test class, for example from within an
 * IDE during development.
 */
public abstract class TestBase {

  Response postData(String fileName) throws MalformedURLException {
	  String endpoint = "http://localhost:" + MockServer.params.get("port");
	  
	  File file = new File(fileName);
	  String absolutePath = file.getAbsolutePath();
	  String body = readLineByLine(file.getAbsolutePath());
      return given()
		.header("x-okapi-tenant", "lu")
		.header("Accept", "application/json,text/plain")
		.header("X-Okapi-Url", endpoint)
		.header("X-Okapi-Token", "na")
      .accept(ContentType.XML)
      .contentType(ContentType.XML)
      .body(body)
      .post(new URL(endpoint + "/ncip"));
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
}