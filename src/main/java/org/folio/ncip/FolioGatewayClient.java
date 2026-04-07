package org.folio.ncip;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.io.entity.*;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class FolioGatewayClient {

    private static final Logger logger = Logger.getLogger(FolioGatewayClient.class);

    public static String get(String url, MultiMap headers) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet request = new HttpGet(url);
            applyTimeout(request);
            applyHeaders(request, headers);

            try (CloseableHttpResponse response = client.execute(request)) {

                String body = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity())
                        : "";

                int code = response.getCode();

                if (code > 399) {
                    logger.error("API error response body: " + body);
                    throw new Exception(processErrorResponse(body));
                }

                return body;
            }
        }
    }

    public static String post(String url, JsonObject body, MultiMap headers) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost request = new HttpPost(url);
            applyTimeout(request);
            applyHeaders(request, headers);

            request.addHeader("Content-Type", "application/json");

            if (body != null) {
                request.setEntity(new StringEntity(body.encode(), ContentType.APPLICATION_JSON));
            }

            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity())
                        : null;
                int code = response.getCode();
                if (code > 399) {
                    logger.error("API error response body: " + responseBody);
                    throw new Exception(processErrorResponse(responseBody));
                }
                return responseBody;
            }
        }
    }

    public static void put(String url, JsonObject body, MultiMap headers) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPut request = new HttpPut(url);
            applyTimeout(request);
            applyHeaders(request, headers);

            request.addHeader("Content-Type", "application/json");

            if (body != null) {
                request.setEntity(new StringEntity(body.encode(), ContentType.APPLICATION_JSON));
            }

            try (CloseableHttpResponse response = client.execute(request)) {
                String responseBody = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity())
                        : "";

                int code = response.getCode();
                if (code > 399) {
                    logger.error("API error response body: " + responseBody);
                    throw new Exception(processErrorResponse(responseBody));
                }
            }
        }
    }

    public static CloseableHttpResponse delete(String url, MultiMap headers) throws Exception {

        CloseableHttpClient client = HttpClients.createDefault();

        HttpDelete request = new HttpDelete(url);
        applyTimeout(request);
        applyHeaders(request, headers);

        return client.execute(request);
    }

    private static void applyTimeout(HttpUriRequestBase request) {
        final String timeoutString = System.getProperty(Constants.SERVICE_MGR_TIMEOUT, Constants.DEFAULT_TIMEOUT);
        int timeout = Integer.parseInt(timeoutString);
        logger.info("Using timeout: " + timeout);

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(timeout))
                .setResponseTimeout(Timeout.ofMilliseconds(timeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeout))
                .build();

        request.setConfig(config);
    }

    private static void applyHeaders(HttpUriRequestBase request, MultiMap headers) {

        if (headers == null) {
            return;
        }

        for (Map.Entry<String, String> header : headers.entries()) {

            String key = header.getKey();

            // 🚨 DO NOT forward unsafe transport headers
            if (key.equalsIgnoreCase("Content-Length")) {
                continue;
            }
            if (key.equalsIgnoreCase("Host")) {
                continue;
            }
            if (key.equalsIgnoreCase("Connection")) {
                continue;
            }
            if (key.equalsIgnoreCase("Transfer-Encoding")) {
                continue;
            }
            if (key.equalsIgnoreCase("Content-Type")) {
                continue;
            }

            request.addHeader(key, header.getValue());
        }
    }

    /**
     * The method deals with error messages that are returned by the API as plain
     * strings and messages returned as JSON
     *
     */
    public static String processErrorResponse(String responseBody) {
        // SOMETIMES ERRORS ARE RETURNED BY THE API AS PLAIN STRINGS
        // SOMETIMES ERRORS ARE RETURNED BY THE API AS JSON
        StringBuffer responseBuffer = new StringBuffer();
        try {
            JsonObject jsonObject = new JsonObject(responseBody);
            JsonArray errors = jsonObject.getJsonArray("errors");
            Iterator i = errors.iterator();
            responseBuffer.append("ERROR: ");
            while (i.hasNext()) {
                JsonObject errorMessage = (JsonObject) i.next();
                responseBuffer.append(errorMessage.getString("message"));
            }
        } catch (Exception exception) {
            // NOT A PROBLEM, ERROR WAS A STRING. UNABLE TO PARSE
            // AS JSON
            responseBuffer.append(responseBody);
        }
        return responseBuffer.toString();
    }
}