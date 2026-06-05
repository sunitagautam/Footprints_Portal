package utils;

import org.testng.annotations.DataProvider;

public class ExcelDataProvider {

    // ═══════════════════════════════════════════════
    // COLUMN COUNTS — must match Excel sheets exactly
    // ═══════════════════════════════════════════════

    // LoginValidations — 7 columns:
    // TestCaseID, TestScenario, Username, Password,
    // ExpectedResult, ExpectedErrorMsg, Remarks
    private static final int LOGIN_COL_COUNT = 7;


    // ═══════════════════════════════════════════════
    // DATA PROVIDER — Login Validations
    // ═══════════════════════════════════════════════
    @DataProvider(name = "loginData")
    public static Object[][] getLoginData() {
        String excelPath = IAutoConstant.INPUT_CREDENTIALS;
        String sheetName = IAutoConstant.SHEET_LOGIN;
        return loadExcelData(excelPath, sheetName, LOGIN_COL_COUNT);
    }


    // ═══════════════════════════════════════════════
    // SHARED HELPER — Reusable Excel loader
    // ═══════════════════════════════════════════════
    private static Object[][] loadExcelData(
            String excelPath,
            String sheetName,
            int expectedColCount) {

        // ✅ Check file exists
        java.io.File excelFile = new java.io.File(excelPath);
        if (!excelFile.exists()) {
            throw new RuntimeException(
                    "❌ Excel file not found at: " + excelPath +
                            "\n   Place file under <project-root>/testData/"
            );
        }

        int rowCount = FWUtils.getRowCount(excelPath, sheetName);
        int colCount = FWUtils.getColCount(excelPath, sheetName);

        System.out.println("══════════════════════════════════════════");
        System.out.println("📊 Excel Sheet  : " + sheetName);
        System.out.println("📋 Data Rows    : " + rowCount);
        System.out.println("📋 Columns      : " + colCount);
        System.out.println("══════════════════════════════════════════");

        // ✅ Validate column count
        if (colCount != expectedColCount) {
            throw new RuntimeException(
                    "❌ Column count mismatch! " +
                            "Excel has " + colCount +
                            " columns but expects " + expectedColCount +
                            "\n   Check sheet: " + sheetName
            );
        }

        // ✅ Load rows — skip empty ones
        java.util.List<Object[]> dataList = new java.util.ArrayList<>();

        for (int i = 1; i <= rowCount; i++) {

            Object[] rowData   = new Object[colCount];
            boolean isEmptyRow = true;

            for (int j = 0; j < colCount; j++) {
                String cellValue = FWUtils.readXLData(
                        excelPath, sheetName, i, j);
                rowData[j] = (cellValue == null) ? "" : cellValue.trim();
                if (!rowData[j].toString().isEmpty()) {
                    isEmptyRow = false;
                }
            }

            // ✅ Skip completely empty rows
            if (isEmptyRow) {
                System.out.println("⚠ Skipping empty row: " + i);
                continue;
            }

            dataList.add(rowData);
            System.out.println("✅ Row " + i + " loaded: " + rowData[0]);
        }

        System.out.println("══════════════════════════════════════════");
        System.out.println("▶ Total valid rows: " + dataList.size());
        System.out.println("══════════════════════════════════════════");

        return dataList.toArray(new Object[0][]);
    }
}
