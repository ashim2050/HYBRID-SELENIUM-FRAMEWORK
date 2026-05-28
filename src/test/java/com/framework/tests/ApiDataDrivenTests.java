package com.framework.tests;

import com.framework.config.ConfigReader;
import com.framework.listeners.TestListener;
import com.framework.utils.DataProviderUtil;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Data-driven REST API test class.
 *
 * Test data is sourced from output/testdata/API_testdata.json
 * (generated at runtime from Input/API_Module.xlsx).
 *
 * Four test methods map to the four HTTP verbs.
 * The TestMethodName column in Excel controls which rows each DataProvider picks up.
 *
 * Target API: https://jsonplaceholder.typicode.com (free public REST API)
 */
@Listeners(TestListener.class)
public class ApiDataDrivenTests {

    private static final Logger logger = LogManager.getLogger(ApiDataDrivenTests.class);
    private static final String MODULE_NAME = "API";

    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;

    // -------------------------------
    // Setup
    // -------------------------------
    @BeforeClass(alwaysRun = true)
    public void setupApi() {
        String baseURI = ConfigReader.getApiBaseUrl();
        RestAssured.baseURI = baseURI;

        String proxyHost = ConfigReader.getApiProxyHost();
        int proxyPort = ConfigReader.getApiProxyPort();
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            RestAssured.proxy(proxyHost, proxyPort);
            logger.info("API proxy set: {}:{}", proxyHost, proxyPort);
        }

        RestAssured.useRelaxedHTTPSValidation();

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .log(LogDetail.STATUS)
                .build();

        logger.info("---- API data-driven tests starting | baseURI: {} ----", baseURI);
    }

    // -------------------------------
    // DataProviders
    // -------------------------------
    @DataProvider(name = "getRequestData", parallel = false)
    public Object[][] getRequestData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testGetRequest");
    }

    @DataProvider(name = "postRequestData", parallel = false)
    public Object[][] postRequestData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testPostRequest");
    }

    @DataProvider(name = "putRequestData", parallel = false)
    public Object[][] putRequestData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testPutRequest");
    }

    @DataProvider(name = "deleteRequestData", parallel = false)
    public Object[][] deleteRequestData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testDeleteRequest");
    }

    // -------------------------------
    // Test Methods
    // -------------------------------

    @Test(dataProvider = "getRequestData",
          description = "Data-driven GET request - endpoint and assertions from Excel")
    public void testGetRequest(Map<String, Object> testData) {
        String tcId = str(testData, "TestCaseID");
        String endpoint = str(testData, "Endpoint");
        int expectedStatus = Integer.parseInt(str(testData, "ExpectedStatusCode"));
        String validatePath = str(testData, "ValidateJsonPath");
        String expectedValue = str(testData, "ExpectedValue");
        String baseUrl = getBaseUrl(testData);

        RestAssured.baseURI = baseUrl;
        logger.info("TEST [{}]: GET {} | baseUrl: {} | expected status: {}", tcId, endpoint, baseUrl, expectedStatus);

        Response response = given()
                .spec(requestSpec)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .statusCode(expectedStatus)
                .extract().response();

        validateField(response, validatePath, expectedValue, tcId);
        logger.info("PASS [{}]: GET {} -> {}", tcId, endpoint, expectedStatus);
    }

    @Test(dataProvider = "postRequestData",
          description = "Data-driven POST request - endpoint and body from Excel")
    public void testPostRequest(Map<String, Object> testData) {
        String tcId = str(testData, "TestCaseID");
        String endpoint = str(testData, "Endpoint");
        String requestBody = str(testData, "RequestBody");
        int expectedStatus = Integer.parseInt(str(testData, "ExpectedStatusCode"));
        String validatePath = str(testData, "ValidateJsonPath");
        String expectedValue = str(testData, "ExpectedValue");
        String baseUrl = getBaseUrl(testData);

        RestAssured.baseURI = baseUrl;
        logger.info("TEST [{}]: POST {} | baseUrl: {} | expected status: {}", tcId, endpoint, baseUrl, expectedStatus);

        Response response = given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .post(endpoint)
                .then()
                .spec(responseSpec)
                .statusCode(expectedStatus)
                .extract().response();

        validateField(response, validatePath, expectedValue, tcId);
        logger.info("PASS [{}]: POST {} -> {}", tcId, endpoint, expectedStatus);
    }

    @Test(dataProvider = "putRequestData",
          description = "Data-driven PUT request - endpoint and body from Excel")
    public void testPutRequest(Map<String, Object> testData) {
        String tcId = str(testData, "TestCaseID");
        String endpoint = str(testData, "Endpoint");
        String requestBody = str(testData, "RequestBody");
        int expectedStatus = Integer.parseInt(str(testData, "ExpectedStatusCode"));
        String validatePath = str(testData, "ValidateJsonPath");
        String expectedValue = str(testData, "ExpectedValue");
        String baseUrl = getBaseUrl(testData);

        RestAssured.baseURI = baseUrl;
        logger.info("TEST [{}]: PUT {} | baseUrl: {} | expected status: {}", tcId, endpoint, baseUrl, expectedStatus);

        Response response = given()
                .spec(requestSpec)
                .body(requestBody)
                .when()
                .put(endpoint)
                .then()
                .spec(responseSpec)
                .statusCode(expectedStatus)
                .extract().response();

        validateField(response, validatePath, expectedValue, tcId);
        logger.info("PASS [{}]: PUT {} -> {}", tcId, endpoint, expectedStatus);
    }

    @Test(dataProvider = "deleteRequestData",
          description = "Data-driven DELETE request - endpoint from Excel")
    public void testDeleteRequest(Map<String, Object> testData) {
        String tcId = str(testData, "TestCaseID");
        String endpoint = str(testData, "Endpoint");
        int expectedStatus = Integer.parseInt(str(testData, "ExpectedStatusCode"));
        String baseUrl = getBaseUrl(testData);

        RestAssured.baseURI = baseUrl;
        logger.info("TEST [{}]: DELETE {} | baseUrl: {} | expected status: {}", tcId, endpoint, baseUrl, expectedStatus);

        given()
                .spec(requestSpec)
                .when()
                .delete(endpoint)
                .then()
                .spec(responseSpec)
                .statusCode(expectedStatus);

        logger.info("PASS [{}]: DELETE {} -> {}", tcId, endpoint, expectedStatus);
    }

    // -------------------------------
    // Helpers
    // -------------------------------

    private String getBaseUrl(Map<String, Object> testData) {
        String baseUrl = str(testData, "BaseUrl");
        return baseUrl.isEmpty() ? ConfigReader.getApiBaseUrl() : baseUrl;
    }

    private void validateField(Response response, String jsonPath,
                               String expectedValue, String tcId) {
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            return; // no field validation requested
        }
        Object actual = response.jsonPath().get(jsonPath.trim());

        // Normalize list results: use first element for scalar assertions
        Object scalarActual = actual;
        if (actual instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) actual;
            if (list.isEmpty()) {
                scalarActual = null;
            } else {
                scalarActual = list.get(0);
            }
        }

        if ("not-null".equalsIgnoreCase(expectedValue) || "non-null".equalsIgnoreCase(expectedValue)) {
            Assert.assertNotNull(actual,
                    tcId + ": Field [" + jsonPath + "] should not be null");
        } else if ("not-empty".equalsIgnoreCase(expectedValue)) {
            Assert.assertNotNull(actual,
                    tcId + ": Field [" + jsonPath + "] should not be null");
            if (actual instanceof java.util.Collection) {
                Assert.assertFalse(((java.util.Collection<?>) actual).isEmpty(),
                        tcId + ": Field [" + jsonPath + "] should not be empty");
            } else {
                Assert.assertFalse(String.valueOf(actual).trim().isEmpty(),
                        tcId + ": Field [" + jsonPath + "] should not be empty");
            }
        } else {
            try {
                int expected = Integer.parseInt(expectedValue.trim());
                if (scalarActual == null) {
                    Assert.fail(tcId + ": Field [" + jsonPath + "] is null");
                }
                int actualInt = (scalarActual instanceof Number)
                        ? ((Number) scalarActual).intValue()
                        : Integer.parseInt(String.valueOf(scalarActual));
                Assert.assertEquals(actualInt, expected,
                        tcId + ": Field [" + jsonPath + "] mismatch");
            } catch (NumberFormatException e) {
                Assert.assertEquals(String.valueOf(scalarActual), expectedValue.trim(),
                        tcId + ": Field [" + jsonPath + "] mismatch");
            }
        }
    }

    private static String str(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val).trim() : "";
    }
}
