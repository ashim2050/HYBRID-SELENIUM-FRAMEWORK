package com.framework.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Converts Excel raw data (List of Maps) to JSON files and reads them back.
 * Each module's test-case data is persisted as a JSON array so that
 * test classes can consume it at runtime via DataProviderUtil.
 */
public class JsonConverter {

    private static final Logger logger = LogManager.getLogger(JsonConverter.class);

    // Jackson ObjectMapper with pretty-print enabled
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonConverter() { }

    // -------------------------------
    // Write JSON
    // -------------------------------

    /**
     * Serialises a list of row-maps (read from Excel) to a pretty-printed JSON file.
     *
     * @param data Rows from ExcelReader
     * @param outputFilePath Target JSON file path (parent directories are created if needed)
     */
    public static void writeJson(List<Map<String, String>> data, String outputFilePath) {
        try {
            File outputFile = new File(outputFilePath);
            // Ensure parent directories exist
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warn("Could not create directories for: {}", outputFilePath);
            }

            MAPPER.writeValue(outputFile, data);
            logger.info("JSON written to {} [{} record(s)]", outputFilePath, data.size());
        } catch (IOException e) {
            logger.error("Failed to write JSON to '{}': {}", outputFilePath, e.getMessage(), e);
            throw new RuntimeException("Failed to write JSON file: " + outputFilePath, e);
        }
    }

    // -------------------------------
    // Read JSON
    // -------------------------------

    /**
     * Reads a JSON file back as a list of string-keyed maps.
     * Used by DataProviderUtil to feed test data at runtime.
     *
     * @param jsonFilePath Path to JSON file
     * @return List of maps representing test data
     */
    public static List<Map<String, Object>> readJson(String jsonFilePath) {
        try {
            return MAPPER.readValue(
                new File(jsonFilePath),
                new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (IOException e) {
            logger.error("Failed to read JSON from '{}': {}", jsonFilePath, e.getMessage(), e);
            throw new RuntimeException("Failed to read JSON file: " + jsonFilePath, e);
        }
    }

    // -------------------------------
    // Utility
    // -------------------------------

    /**
     * Converts any object to a formatted JSON string (for logging purposes).
     *
     * @param obj Object to convert
     * @return JSON string representation
     */
    public static String toJsonString(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (IOException e) {
            return obj.toString();
        }
    }
}
