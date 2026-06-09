package com.framework.listeners;

import com.framework.config.ConfigReader;
import com.framework.utils.ExcelReader;
import com.framework.utils.JsonConverter;
import com.framework.utils.TestNGXmlGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dynamically builds the TestNG suite from MasterConfig.xlsx before any test runs.
 *
 * <p>This listener fires BEFORE tests start, reads the ExecutionFlag column in the
 * master sheet, generates JSON test data for each active module, and replaces the
 * static {@code <test>} entries in testng.xml with the correct active set.
 *
 * <p>This means testng.xml never needs to be edited manually – just flip
 * ExecutionFlag in MasterConfig.xlsx and re-run.
 */
public class DynamicSuiteListener implements IAlterSuiteListener {
    private static final Logger logger = LogManager.getLogger(DynamicSuiteListener.class);

    @Override
    public void alter(List<XmlSuite> suites) {
        logger.info("--- DynamicSuiteListener: building suite from MasterConfig.xlsx ---");

        // Only proceed when the master config exists on disk; otherwise nothing to do.
        String masterPath = ConfigReader.getMasterFilePath();
        File masterFile = new File(masterPath);
        if (!masterFile.exists()) {
            logger.warn("Master config not found at '{}' - skipping dynamic suite generation.", masterPath);
            return;
        }

        for (XmlSuite suite : suites) {
            try {
                processSuite(suite);
            } catch (Exception e) {
                logger.error("Failed to process suite '{}' due to error: {}", suite.getName(), e.getMessage(), e);
            }
        }

        logger.info("--- DynamicSuiteListener: alter() complete ---");
    }

    private void processSuite(XmlSuite suite) {
        logger.info("Processing suite: {}", suite.getName());

        // 1. Read master config
        String masterPath = ConfigReader.getMasterFilePath();
        logger.info("Master config: {}", masterPath);

        List<Map<String, String>> masterRows = ExcelReader.readFirstSheet(masterPath);
        logger.info("Total modules in master: {}", masterRows.size());

        // 2. Filter active modules
        List<Map<String, String>> activeModules = masterRows.stream()
            .filter(row -> "yes".equalsIgnoreCase(
                row.getOrDefault("ExecutionFlag", "no").trim()))
            .collect(Collectors.toList());

        if (activeModules.isEmpty()) {
            logger.warn("No modules with ExecutionFlag=Yes found. Suite will be empty.");
            return;
        }

        logger.info("Active modules ({}):", activeModules.size());
        activeModules.forEach(m -> logger.info("{}", m.get("ModuleName")));

        // 3. Generate JSON test data for each active module
        new File(ConfigReader.getJsonOutputPath()).mkdirs();

        for (Map<String, String> module : activeModules) {
            String moduleName = module.getOrDefault("ModuleName", "Unknown").trim();
            String moduleFilePath = module.getOrDefault("ModuleFilePath", "Unknown").trim();
            String sheetName = module.getOrDefault("SheetName", ConfigReader.getModuleDefaultSheet()).trim();

            File moduleFile = new File(moduleFilePath);
            if (!moduleFile.exists()) {
                logger.warn("Module file not found: {}", moduleFilePath);
                continue;
            }

            List<Map<String, String>> testData = ExcelReader.readSheet(moduleFile.getAbsolutePath(), sheetName);
            String jsonOutputPath = ConfigReader.getJsonOutputPath();
            if (!jsonOutputPath.endsWith("/") && !jsonOutputPath.endsWith("\\")) {
                jsonOutputPath += "/";
            }
            JsonConverter.writeJson(testData, jsonOutputPath + moduleName + "_testdata.json");

          }

       // 4. Rebuild XmlTest entries for this suite
suite.getTests().clear();

for (Map<String, String> module : activeModules) {
    String moduleName = module.getOrDefault("ModuleName", "Unknown").trim();
    String testClass = module.getOrDefault("TestClass", "").trim();

    if (testClass.isEmpty()) {
        logger.warn("Module '{}' has no TestClass column - skipping.", moduleName);
        continue;
    }

    XmlTest xmlTest = new XmlTest(suite);
    xmlTest.setName(moduleName);
    xmlTest.setClasses(Collections.singletonList(new XmlClass(testClass)));

    logger.info("Suite test added: [{}] -> {}", moduleName, testClass);
}

logger.info("Suite '{}' built with {} test(s)", suite.getName(), activeModules.size());

// 5. Persist the resolved suite to testng-dynamic.xml
// This keeps the file in sync with what actually ran, making it easy
// to inspect the last execution plan or re-run via the standalone runner.
String dynamicXmlPath = ConfigReader.getTestNgOutputPath();
new File(dynamicXmlPath).getParentFile().mkdirs();
TestNGXmlGenerator.generate(activeModules, dynamicXmlPath);
logger.info("testng-dynamic.xml updated: {}", dynamicXmlPath);

    }
}
