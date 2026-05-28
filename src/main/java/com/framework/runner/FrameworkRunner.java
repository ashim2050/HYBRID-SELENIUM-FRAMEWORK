package com.framework.runner;

// Importing required classes
import com.framework.config.ConfigReader;
import com.framework.utils.ExcelReader;
import com.framework.utils.JsonConverter;
import com.framework.utils.TestNGXmlGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.TestNG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entry point for the Hybrid Selenium Framework.
 *
 * Execution flow:
 * 1. Read MasterConfig.xlsx — every row represents one test module.
 * 2. Filter rows where ExecutionFlag == "Yes".
 * 3. For each active module, read its module Excel file and convert
 *    the sheet into test data files inside output/testdata/.
 * 4. Generate a dynamic testng.xml in output/ listing only active test classes.
 * 5. Programmatically invoke TestNG with the generated XML.
 */
public class FrameworkRunner {

    // Logger instance for logging framework execution details
    private static final Logger logger = LogManager.getLogger(FrameworkRunner.class);

    public static void main(String[] args) {

        // Banner logs
        logger.info("===============================================");
        logger.info("Hybrid Selenium Framework Runner Start");
        logger.info("===============================================");

        // Step 1: Read master config file
        String masterPath = ConfigReader.getMasterFilePath();
        logger.info("Reading master config: {}", masterPath);

        List<Map<String, String>> masterRows = ExcelReader.readFirstSheet(masterPath);
        logger.info("Total modules in master: {}", masterRows.size());

        // Step 2: Filter active modules (ExecutionFlag = Yes)
        List<Map<String, String>> activeModules = masterRows.stream()
                .filter(row -> "yes".equalsIgnoreCase(
                        row.getOrDefault("ExecutionFlag", "no").trim()))
                .collect(Collectors.toList());

        if (activeModules.isEmpty()) {
            logger.info("No modules marked ExecutionFlag=Yes. Nothing to run. Exiting.");
            return;
        }

        logger.info("Active modules marked for execution: {}", activeModules.size());
        activeModules.forEach(m -> logger.info(" - {}", m.get("ModuleName")));

        // Step 3: Convert Excel test cases to JSON for each active module
        for (Map<String, String> module : activeModules) {
            String moduleName = module.getOrDefault("ModuleName", "unknown").trim();
            String moduleFile = module.getOrDefault("ModuleFilePath", "").trim();
            String sheetName = module.getOrDefault("SheetName", "").trim();

            if (moduleFile.isEmpty()) {
                logger.error("Module '{}' has no ModuleFilePath, skipping JSON conversion.", moduleName);
                continue;
            }

            logger.info("Processing module: {} | file: {} | sheet: {}", moduleName, moduleFile, sheetName);

            // Read test cases from Excel
            List<Map<String, String>> testCases = ExcelReader.readSheet(moduleFile, sheetName);

            // Write test cases to JSON
            String jsonOutputPath = ConfigReader.getJsonOutputPath() + moduleName + "_testdata.json";
            JsonConverter.writeJson(testCases, jsonOutputPath);

            logger.info("JSON generated: {} | {} test case(s)", jsonOutputPath, testCases.size());
        }

        // Step 4: Generate dynamic testng.xml
        String testNGXmlPath = ConfigReader.getTestNGOutputPath();
        TestNGXmlGenerator.generate(activeModules, testNGXmlPath);

        // Step 5: Execute TestNG with generated XML
        logger.info("Launching TestNG with: {}", testNGXmlPath);

        TestNG testng = new TestNG();
        testng.setUseDefaultListeners(false);
        List<String> suites = new ArrayList<>();
        suites.add(new java.io.File(testNGXmlPath).getAbsolutePath());
        testng.setTestSuites(suites);
        testng.run();

        // Final completion log
        logger.info("Hybrid framework Execution Complete");
    }
}
