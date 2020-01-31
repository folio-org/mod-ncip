package org.folio.ncip;

import static io.restassured.RestAssured.given;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

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