package com.framework.setup;

import com.framework.config.ConfigReader;
import com.framework.utils.ExcelReader;
import com.framework.utils.JsonConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.BeforeSuite;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TestNG suite-level setup: reads MasterConfig.xlsx and generates
 * per-module JSON test-data files before any DataProvider or test runs.
 *
 * <p>Include this class in your static {@code testng.xml} so that
 * {@code mvn test} works without needing to run FrameworkRunner first.
 */
public class SuiteSetup {

    private static final Logger logger = LogManager.getLogger(SuiteSetup.class);

    @BeforeSuite(alwaysRun = true)
    public void generateTestData() {
        logger.info("== SuiteSetup: generating JSON test data from Excel ==");

        // Read master config
        String masterPath = ConfigReader.getMasterFilePath();
        logger.info("Reading master config: {}", masterPath);

        List<Map<String, String>> masterRows = ExcelReader.readFirstSheet(masterPath);

        // Filter active modules
        List<Map<String, String>> activeModules = masterRows.stream()
                .filter(row -> "yes".equalsIgnoreCase(
                        row.getOrDefault("ExecutionFlag", "no").trim()))
                .collect(Collectors.toList());

        if (activeModules.isEmpty()) {
            logger.warn("No active modules found in MasterConfig - no JSON generated.");
            return;
        }

        // Process each active module
        for (Map<String, String> module : activeModules) {
            String moduleName = module.getOrDefault("ModuleName", "Unknown").trim();
            String moduleFile = module.getOrDefault("ModuleFilePath", "").trim();
            String sheetName = module.getOrDefault("SheetName", ConfigReader.getModuleDefaultSheet()).trim();

            if (moduleFile.isEmpty()) {
                logger.warn("Module '{}' has no ModuleFilePath - skipping.", moduleName);
                continue;
            }

            logger.info("Processing module: {} | file: {} | sheet: {}", moduleName, moduleFile, sheetName);

            List<Map<String, String>> testCases = ExcelReader.readSheet(moduleFile, sheetName);
            String jsonPath = ConfigReader.getJsonOutputPath() + moduleName + "_testdata.json";
            JsonConverter.writeJson(testCases, jsonPath);

            logger.info("JSON generated: {} ({} rows)", jsonPath, testCases.size());
        }

        logger.info("=== SuiteSetup complete ===");
    }
}
