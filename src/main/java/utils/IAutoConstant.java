package utils;

import java.io.File;

public final class IAutoConstant {

    // ═══════════════════════════════════════════════
    // BASE PATH — works on Mac & Windows
    // ═══════════════════════════════════════════════
    public static final String BASE_PATH =
            System.getProperty("user.dir");

    // ═══════════════════════════════════════════════
    // APPLICATION URLs
    // ═══════════════════════════════════════════════
    public static final String LOGIN_URL =
            "https://test-franchise.footprintseducation.in/login";


    // ═══════════════════════════════════════════════
    // LOGIN CREDENTIALS
    // ═══════════════════════════════════════════════
    public static final String USERNAME = "Rakesh";
    public static final String PASSWORD = "Dev@123";

    // ═══════════════════════════════════════════════
    // EXCEL INPUT FILES
    // ═══════════════════════════════════════════════
    public static final String INPUT_CREDENTIALS =
            BASE_PATH + File.separator + "testData" +
                    File.separator + "input_Credential.xlsx";

    // ═══════════════════════════════════════════════
    // OUTPUT PATHS
    // ═══════════════════════════════════════════════
    public static final String REPORT_PATH =
            BASE_PATH + File.separator + "result" +
                    File.separator + "report.xlsx";

    public static final String SCREENSHOT_PATH =
            BASE_PATH + File.separator + "screenshots" +
                    File.separator;

    // ═══════════════════════════════════════════════
    // SHEET NAMES
    // ═══════════════════════════════════════════════
    public static final String SHEET_LOGIN = "LoginValidations";
    public static final String SHEET_USER_RIGHTS = "UserRights";
    // ═══════════════════════════════════════════════

    // CSV FILES : OnlinePayment Received page
// ═══════════════════════════════════════════════
    public static final String PAYMENT_NOT_RECEIVED_CSV =
            BASE_PATH + File.separator + "testData" +
                    File.separator + "payment_not_received.csv";
    // ═══════════════════════════════════════════════
    // TIMEOUTS (seconds)
    // ═══════════════════════════════════════════════
    public static final int EXPLICIT_WAIT = 30;
    public static final int PAGE_LOAD_WAIT = 60;
    public static final int SHORT_WAIT = 5;

    // Prevent instantiation
    private IAutoConstant() {
    }

    // ═══════════════════════════════════════════════
    // FOLDER INITIALISER — called once in @BeforeSuite
    // ═══════════════════════════════════════════════
    public static void initFolders() {
        new File(BASE_PATH + File.separator + "result").mkdirs();
        new File(SCREENSHOT_PATH).mkdirs();
        new File(BASE_PATH + File.separator + "testData").mkdirs();
        System.out.println("▶ Framework folders initialised.");
        System.out.println("   Base path : " + BASE_PATH);
    }
}
