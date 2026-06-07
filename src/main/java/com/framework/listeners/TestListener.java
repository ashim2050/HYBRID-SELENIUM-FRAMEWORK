package com.framework.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.framework.config.ConfigReader;
import com.framework.drivers.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * TestNG listener that integrates with Extent Reports for rich HTML reports.
 *
 * <p>Reports are written to {@code output/reports/ExtentReport_<timestamp>.html}.
 * The listener is registered in the dynamically generated testng.xml.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger logger = LogManager.getLogger(TestListener.class);

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    // --- Suite lifecycle ---
    @Override
    public void onStart(ISuite suite) {
        if (extentReports != null) {
            logger.info("ExtentReports already initialised – reusing for suite: {}", suite.getName());
            return;
        }

        String reportFileName = ConfigReader.getReportsFileName();
        String reportPath = ConfigReader.getReportsOutputPath() + reportFileName;

        java.io.File reportsDir = new java.io.File(ConfigReader.getReportsOutputPath());
        reportsDir.mkdirs();

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setDocumentTitle("Hybrid Framework Execution Report");
        sparkReporter.config().setReportName("Suite: " + suite.getName());
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setEncoding("UTF-8");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Framework", "Hybrid Selenium Framework");
        extentReports.setSystemInfo("Browser", ConfigReader.getBrowser());
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("Env", ConfigReader.get("env", "QA"));

        logger.info("ExtentReports initialised -> {}", reportPath);
    }

    @Override
    public void onFinish(ISuite suite) {
        if (extentReports != null) {
            extentReports.flush();
            logger.info("ExtentReports flushed for suite: {}", suite.getName());
        }
    }

    // --- Test lifecycle ---
    @Override
    public void onTestStart(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        Object[] params = result.getParameters();
        ExtentTest test;

        if (params != null && params.length > 0 && params[0] instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) params[0];
            Object tcId = data.get("TestCaseID");
            Object desc = data.get("Description");
            Object browser = data.get("Browser");

            if (tcId != null && !tcId.toString().isEmpty()) {
                testName = desc != null && !desc.toString().isEmpty()
                        ? tcId + " - " + desc
                        : tcId.toString();
            }

            test = extentReports.createTest(testName, result.getMethod().getDescription());
            if (tcId != null) test.info("<b>Test Case ID:</b> " + tcId);
            if (desc != null) test.info("<b>Description:</b> " + desc);
            if (browser != null) test.info("<b>Browser:</b> " + browser);
        } else {
            test = extentReports.createTest(testName, result.getMethod().getDescription());
        }

        extentTest.set(test);
        logger.info("START = {}", testName);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().log(Status.PASS, "Test PASSED");
        logger.info("PASS = {}", result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = extentTest.get();
        test.log(Status.FAIL, "Test FAILED : " + result.getThrowable().getMessage());
        test.fail(result.getThrowable());

        try {
            WebDriver driver = DriverManager.getDriver();
            if (driver != null) {
                String base64Screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                test.addScreenCaptureFromBase64String("data:image/png;base64," + base64Screenshot,
                        "Screenshot captured for failed test: " + result.getMethod().getMethodName());
            }
        } catch (IllegalStateException e) {
            logger.debug("No WebDriver available for non-UI test: {}", result.getMethod().getMethodName());
        } catch (Exception e) {
            logger.warn("Could not capture screenshot for: {}", result.getMethod().getMethodName(), e);
        }

        logger.error("FAIL - {} | {}", result.getMethod().getMethodName(), result.getThrowable().getMessage());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        extentTest.get().log(Status.SKIP, "Test SKIPPED");
        logger.warn("SKIP - {}", result.getMethod().getMethodName());
    }

    // Static accessor for tests that want to log steps
    public static ExtentTest getTest() {
        return extentTest.get();
    }
}
