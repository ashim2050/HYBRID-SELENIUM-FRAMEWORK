package com.framework.utils;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridges JSON test-data files with TestNG {@code @DataProvider} methods.
 *
 * Workflow:
 * 1. FrameworkRunner converts module Excel → JSON and places it under
 *    output/testdata/<ModuleName>_testdata.json.
 * 2. Each test class calls DataProviderUtil.getTestData(moduleName, methodName)
 *    inside its @DataProvider method.
 * 3. Only rows whose ExecutionFlag is "yes" AND whose TestMethodName
 *    matches the calling method are returned.
 *
 * JSON row map keys (columns) match the Excel headers exactly – no mapping needed.
 */
public class DataProviderUtil {

    private static final Logger logger = LogManager.getLogger(DataProviderUtil.class);

    private DataProviderUtil() { }

    // -------------------------------
    // Public API
    // -------------------------------

    /**
     * Reads the module's JSON file and returns filtered rows as {@code Object[][]}.
     *
     * @param moduleName Logical module name (matches JSON file name prefix)
     * @param testMethodName Java method name to filter rows by TestMethodName column
     * @return TestNG-ready Object[][] where each row is a single
     *         Map<String, Object> containing all test-data columns
     */
    public static Object[][] getTestData(String moduleName, String testMethodName) {
        String jsonPath = buildJsonPath(moduleName);
        logger.info("Loading test data | module: {}, method: {}, file: {}", moduleName, testMethodName, jsonPath);

        List<Map<String, Object>> allRows = JsonConverter.readJson(jsonPath);

        List<Map<String, Object>> filtered = allRows.stream()
                .filter(row -> matchesMethod(row, testMethodName))
                .filter(DataProviderUtil::isExecutionEnabled)
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            logger.warn("No executable rows found for method '{}' in module '{}'.", testMethodName, moduleName);
        } else {
            logger.info("Found {} executable row(s) for '{}' in module '{}'.",
                    filtered.size(), testMethodName, moduleName);
        }

        Object[][] result = new Object[filtered.size()][1];
        for (int i = 0; i < filtered.size(); i++) {
            result[i][0] = filtered.get(i);
        }
        return result;
    }

    /**
     * Overload that accepts the JSON file path directly (useful for custom paths).
     *
     * @param jsonFilePath Path to JSON file
     * @param testMethodName Java method name to filter rows
     * @return TestNG-ready Object[][]
     */
    public static Object[][] getTestDataFromFile(String jsonFilePath, String testMethodName) {
        List<Map<String, Object>> allRows = JsonConverter.readJson(jsonFilePath);

        List<Map<String, Object>> filtered = allRows.stream()
                .filter(row -> matchesMethod(row, testMethodName))
                .filter(DataProviderUtil::isExecutionEnabled)
                .collect(Collectors.toList());

        Object[][] result = new Object[filtered.size()][1];
        for (int i = 0; i < filtered.size(); i++) {
            result[i][0] = filtered.get(i);
        }
        return result;
    }

    // -------------------------------
    // Helpers
    // -------------------------------

    private static String buildJsonPath(String moduleName) {
        String base = ConfigReader.getJsonOutputPath();
        if (!base.endsWith("/") && !base.endsWith("\\")) {
            base += "/";
        }
        return base + moduleName + "_testdata.json";
    }

    private static boolean matchesMethod(Map<String, Object> row, String testMethodName) {
        Object value = row.get("TestMethodName");
        return value != null && testMethodName.equalsIgnoreCase(String.valueOf(value).trim());
    }

    private static boolean isExecutionEnabled(Map<String, Object> row) {
        Object flag = row.get("ExecutionFlag");
        return flag != null && "yes".equalsIgnoreCase(String.valueOf(flag).trim());
    }
}
