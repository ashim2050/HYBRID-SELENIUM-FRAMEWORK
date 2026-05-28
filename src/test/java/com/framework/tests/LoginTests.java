package com.framework.tests;

import com.framework.base.BaseTest;
import com.framework.listeners.TestListener;
import com.framework.pages.LoginPage;
import com.framework.pages.SecureAreaPage;
import com.framework.utils.DataProviderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Login module tests.
 *
 * Test data is sourced from output/testdata/Login_testdata.json
 * (generated at runtime from testdata/Login_Module.xlsx by FrameworkRunner).
 *
 * Only rows whose ExecutionFlag=Yes and whose TestMethodName
 * matches the calling method are fed in via the DataProvider.
 *
 * Target application: https://the-internet.herokuapp.com/login
 */
public class LoginTests extends BaseTest {

    private static final Logger logger = LogManager.getLogger(LoginTests.class);
    private static final String MODULE_NAME = "Login";

    // -------------------------------
    // DataProviders
    // -------------------------------
    @DataProvider(name = "validLoginData", parallel = false)
    public Object[][] validLoginData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testValidLogin");
    }

    @DataProvider(name = "invalidLoginData", parallel = false)
    public Object[][] invalidLoginData() {
        return DataProviderUtil.getTestData(MODULE_NAME, "testInvalidLogin");
    }

    // -------------------------------
    // Tests
    // -------------------------------

    /**
     * TC_LGN_001 - Verifies that a valid user can log in and land on the Secure Area.
     */
    @Test(dataProvider = "validLoginData",
          description = "Verify login with valid credentials")
    public void testValidLogin(Map<String, Object> testData) {
        String url = str(testData, "URL");
        String username = str(testData, "Username");
        String password = str(testData, "Password");
        String expectedTitle = str(testData, "ExpectedTitle");

        TestListener.getTest().info("TC: " + str(testData, "TestCaseID") + " | User: " + username);
        logger.info("testValidLogin 🟢 user: {}", username);

        LoginPage loginPage = new LoginPage(getDriver());
        SecureAreaPage securePage = loginPage.open(url).loginAs(username, password);

        // Verify successful login via flash message
        Assert.assertTrue(securePage.getFlashMessageText().contains("You logged into a secure area!"),
                "Login success message not found");

        // Optional title check
        if (!expectedTitle.isEmpty()) {
            Assert.assertTrue(securePage.getTitle().contains(expectedTitle),
                    "Page title mismatch. Expected to contain: " + expectedTitle);
        }

        TestListener.getTest().pass("testValidLogin passed for user: " + username);
        logger.info("testValidLogin PASSED 🟢 user: {}", username);
    }

    /**
     * TC_LGN_002, TC_LGN_003 - Verifies that invalid credentials show an error.
     */
    @Test(dataProvider = "invalidLoginData",
          description = "Verify login with invalid credentials shows error")
    public void testInvalidLogin(Map<String, Object> testData) {
        String url = str(testData, "URL");
        String username = str(testData, "Username");
        String password = str(testData, "Password");
        String expectedError = str(testData, "ExpectedError");

        TestListener.getTest().info("TC: " + str(testData, "TestCaseID") + " | User: " + username);
        logger.info("testInvalidLogin 🔴 user: {}", username);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.open(url).loginWithInvalidCredentials(username, password);

        String flashText = loginPage.getFlashMessageText();
        Assert.assertTrue(flashText.contains(expectedError),
                "Expected error: [" + expectedError + "] not found in: [" + flashText + "]");

        TestListener.getTest().pass("testInvalidLogin passed | error correctly shown.");
        logger.info("testInvalidLogin PASSED 🔴 user: {}", username);
    }

    // -------------------------------
    // Helpers
    // -------------------------------
    private static String str(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val != null ? String.valueOf(val).trim() : "";
    }
}
