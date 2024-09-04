package org.folio.ncip;

import io.restassured.response.Response;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreateUserFiscalTransaction extends TestBase {

    private static final String USER_ID = "d4a845bd-a326-4916-9cb4-6a5ac1a86155";
    private static final String FEE_ID = "628e0831-30b0-415f-8b21-944950624926";
    private static final String PROBLEM = "Problem";

    @Test
    public void callCreateUserFiscalTransaction() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-createUserFiscalTransaction.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(USER_ID));
        assertTrue(body.contains(FEE_ID));
    }

    @Test
    public void callCreateUserFiscalTransactionNoTransaction() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-createUserFiscalTransactionNoTrans.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(PROBLEM));
    }

    @Test
    public void callCreateUserFiscalTransactionBlocked() throws MalformedURLException {
        Response response = postData("src/test/resources/mockdata/ncip-createUserFiscalTransactionBlocked.xml");
        String body = response.getBody().prettyPrint();
        assertEquals(200, response.getStatusCode());
        assertTrue(body.contains(PROBLEM));
    }
}
