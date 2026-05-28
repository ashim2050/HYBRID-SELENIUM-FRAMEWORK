package com.framework.base;

import com.framework.config.ConfigReader;
import com.framework.drivers.DriverManager;
import com.framework.listeners.TestListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.util.Map;

/**
 * Abstract base class for all test classes.
 *
 * <p>Lifecycle:
 * <ul>
 * <li>{@code setup(Object[])} - called before every test method;
 * reads the "Browser" key from the test-data map (first DataProvider param)
 * or falls back to config.properties.</li>
 * <li>{@code tearDown()} - quits the driver after every test method.</li>
 * </ul>
 *
 * <p>Thread safety: each thread gets its own driver via {@link DriverManager}.
 * The {@link TestListener} is registered here so Extent Reports are active for
 * both IDE runs and FrameworkRunner-generated TestNG XML executions.
 */
@Listeners(TestListener.class)
public abstract class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);

    // --- Setup / Teardown ---
    /**
     * TestNG injects the DataProvider parameters as {@code params[0]} when
     * {@code alwaysRun=true} and the method signature accepts {@code Object[]}.
     */
    @BeforeMethod(alwaysRun = true)
    public void setup(Object[] params) {
        String browser = ConfigReader.getBrowser();

        // If a DataProvider row is present, honour its "Browser" column
        if (params != null && params.length > 0 && params[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> testData = (Map<String, Object>) params[0];
            Object browserVal = testData.get("Browser");
            if (browserVal != null && !String.valueOf(browserVal).trim().isEmpty()) {
                browser = String.valueOf(browserVal).trim();
            }
        }

        try {
            DriverManager.initDriver(browser);
        } catch (Throwable e) {
            logger.error("DRIVER INIT FAILED | Browser: {} | error: {}", browser, e.getMessage());
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
        }

        logger.info("**** Test started | Browser: {} | Thread: {} ****",
                browser, Thread.currentThread().getName());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        logger.info("**** Test finished | cleaning up driver ****");
        DriverManager.quitDriver();
    }

    // --- Convenience accessor ---
    /**
     * Returns the WebDriver for the current thread.
     */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
