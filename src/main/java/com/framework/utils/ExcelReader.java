package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Reads .xlsx files produced by Apache POI and returns data as
 * {@code List<Map<String,String>>} where each map represents one data row
 * with column-header keys.
 *
 * Both the Master config file and module-level Excel files are read
 * through this single utility.
 */
public class ExcelReader {

    private static final Logger logger = LogManager.getLogger(ExcelReader.class);

    private ExcelReader() { }

    /**
     * Reads a named sheet from the given Excel file.
     *
     * @param filePath Absolute or project-relative path to the .xlsx file
     * @param sheetName Name of the sheet to read
     * @return List of row maps (header -> cell-value); never {@code null}
     */
    public static List<Map<String, String>> readSheet(String filePath, String sheetName) {
        List<Map<String, String>> data = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                Sheet fallback = workbook.getSheetAt(0);
                if (fallback == null) {
                    logger.error("Sheet '{}' not found in '{}' and workbook has no sheets.", sheetName, filePath);
                    throw new RuntimeException("Sheet '" + sheetName + "' not found in file: " + filePath);
                }
                logger.warn("Sheet '{}' not found in '{}'; falling back to first sheet '{}'.",
                        sheetName, filePath, fallback.getSheetName());
                sheet = fallback;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                logger.warn("Sheet '{}' has no header row returning empty list.", sheetName);
                return data;
            }

            List<String> headers = extractHeaders(headerRow);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                    Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(colIdx), getCellValue(cell));
                }
                data.add(rowData);
            }

            logger.info("Read {} data row(s) from sheet '{}' in '{}'.",
                    data.size(), sheetName, filePath);

        } catch (IOException e) {
            logger.error("IO error reading '{}': {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Failed to read Excel file: " + filePath, e);
        }

        return data;
    }

    /**
     * Reads the first sheet of the given Excel file.
     */
    public static List<Map<String, String>> readFirstSheet(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            String sheetName = workbook.getSheetAt(0).getSheetName();
            return readSheet(filePath, sheetName);

        } catch (IOException e) {
            throw new RuntimeException("Failed to open Excel file: " + filePath, e);
        }
    }

    /**
     * Reads ALL sheets from an Excel file.
     *
     * @return Map of sheetName -> list of row maps
     */
    public static Map<String, List<Map<String, String>>> readAllSheets(String filePath) {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String sheetName = workbook.getSheetAt(i).getSheetName();
                result.put(sheetName, readSheet(filePath, sheetName));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read all sheets from: " + filePath, e);
        }

        return result;
    }

    // Private helpers

    private static List<String> extractHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            String header = getCellValue(cell);
            headers.add(header.isEmpty() ? "COL_" + cell.getColumnIndex() : header);
        }
        return headers;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private static boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !getCellValue(cell).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
