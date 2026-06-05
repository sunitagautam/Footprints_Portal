package utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

public class FWUtils {

    // ═══════════════════════════════════════════════
    // READ EXCEL DATA
    // ═══════════════════════════════════════════════
    public static String readXLData(String filePath, String sheetName,
                                    int rowNum, int colNum) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.out.println("❌ Sheet not found: " + sheetName);
                return "";
            }

            Row row = sheet.getRow(rowNum);
            if (row == null) return "";

            Cell cell = row.getCell(colNum);
            if (cell == null) return "";

            DataFormatter formatter = new DataFormatter();
            return formatter.formatCellValue(cell).trim();

        } catch (Exception e) {
            System.out.println("❌ readXLData error: " + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // GET ROW COUNT
    // ═══════════════════════════════════════════════
    public static int getRowCount(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.out.println("❌ Sheet not found: " + sheetName);
                return 0;
            }
            return sheet.getLastRowNum();

        } catch (Exception e) {
            System.out.println("❌ getRowCount error: " + e.getMessage());
            return 0;
        }
    }

    // ═══════════════════════════════════════════════
    // GET COLUMN COUNT
    // ═══════════════════════════════════════════════
    public static int getColCount(String filePath, String sheetName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                System.out.println("❌ Sheet not found: " + sheetName);
                return 0;
            }
            Row row = sheet.getRow(0);
            return (row == null) ? 0 : row.getLastCellNum();

        } catch (Exception e) {
            System.out.println("❌ getColCount error: " + e.getMessage());
            return 0;
        }
    }

    // ═══════════════════════════════════════════════
    // WRITE EXCEL DATA
    // ═══════════════════════════════════════════════
    public static void setXLData(String filePath, String sheetName,
                                 int rowNum, int colNum, Object value) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) sheet = workbook.createSheet(sheetName);

            Row row = sheet.getRow(rowNum);
            if (row == null) row = sheet.createRow(rowNum);

            Cell cell = row.getCell(colNum);
            if (cell == null) cell = row.createCell(colNum);

            if (value instanceof Integer) {
                cell.setCellValue((Integer) value);
            } else {
                cell.setCellValue(value.toString());
            }

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
            System.out.println("✅ Report updated — row:" + rowNum +
                    " col:" + colNum + " value:" + value);

        } catch (Exception e) {
            System.out.println("❌ setXLData error: " + e.getMessage());
        }
    }
}