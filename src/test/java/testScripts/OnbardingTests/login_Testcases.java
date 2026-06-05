package testScripts.OnbardingTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.Onboarding.LoginPage;
import utils.BaseTest;
import utils.FWUtils;
import utils.IAutoConstant;

public class login_Testcases extends BaseTest {

    LoginPage loginPage;

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — navigate to login page
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void openLoginPage() {
        driver.manage().deleteAllCookies();
        driver.get(IAutoConstant.LOGIN_URL);
        loginPage = new LoginPage(driver);
        System.out.println("▶ Navigated to: " + IAutoConstant.LOGIN_URL);
    }

    // ═══════════════════════════════════════════════
    // DATA PROVIDER — reads from Excel
    // ═══════════════════════════════════════════════
    @DataProvider(name = "loginData")
    public Object[][] getLoginData() {

        String filePath  = IAutoConstant.INPUT_CREDENTIALS;
        String sheetName = "LoginValidations";

        int rowCount = FWUtils.getRowCount(filePath, sheetName);
        int colCount = FWUtils.getColCount(filePath, sheetName);

        System.out.println("══════════════════════════════════════════");
        System.out.println("📊 Sheet        : " + sheetName);
        System.out.println("📋 Data Rows    : " + rowCount);
        System.out.println("📋 Columns      : " + colCount);
        System.out.println("══════════════════════════════════════════");

        // ✅ Columns: TestCaseID, TestScenario, Username,
        //             Password, ExpectedResult, ExpectedErrorMsg, Remarks
        java.util.List<Object[]> dataList = new java.util.ArrayList<>();

        for (int i = 1; i <= rowCount; i++) {
            String testCaseID    = FWUtils.readXLData(filePath, sheetName, i, 0);
            String testScenario  = FWUtils.readXLData(filePath, sheetName, i, 1);
            String un            = FWUtils.readXLData(filePath, sheetName, i, 2);
            String pw            = FWUtils.readXLData(filePath, sheetName, i, 3);
            String expectedResult= FWUtils.readXLData(filePath, sheetName, i, 4);
            String expectedErrMsg= FWUtils.readXLData(filePath, sheetName, i, 5);
            String remarks       = FWUtils.readXLData(filePath, sheetName, i, 6);

            // ✅ Skip empty rows
            if (testCaseID.isEmpty()) {
                System.out.println("⚠ Skipping empty row: " + i);
                continue;
            }

            dataList.add(new Object[]{
                    testCaseID, testScenario, un, pw,
                    expectedResult, expectedErrMsg, remarks
            });
        }

        System.out.println("▶ Total test rows loaded: " + dataList.size());
        System.out.println("══════════════════════════════════════════");

        return dataList.toArray(new Object[0][]);
    }

    // ═══════════════════════════════════════════════
    // TEST — Login Validation
    // ═══════════════════════════════════════════════
    @Test(
            dataProvider = "loginData",
            description  = "Login screen validation — valid & invalid credentials"
    )
    public void validateLogin(
            String testCaseID,
            String testScenario,
            String username,
            String password,
            String expectedResult,
            String expectedErrMsg,
            String remarks
    ) {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("▶ TC ID     : " + testCaseID);
        System.out.println("▶ Scenario  : " + testScenario);
        System.out.println("▶ Username  : [" + username + "]");
        System.out.println("▶ Expected  : " + expectedResult);
        System.out.println("══════════════════════════════════════════");

        // ── Perform Login ─────────────────────────────────────────────────
        loginPage.login(username, password);

        // ── Validate based on Expected Result ────────────────────────────
        if (expectedResult.equalsIgnoreCase("PASS")) {

            // ✅ Valid credentials — should redirect away from login
            boolean loginSuccess = loginPage.isLoginSuccessful();

            Reporter.log("▶ TC: " + testCaseID + " | " + testScenario, true);
            Reporter.log("   Expected : Redirect to dashboard", true);
            Reporter.log("   Actual   : " + (loginSuccess ?
                    "✅ Redirected successfully" :
                    "❌ Still on login page"), true);

            Assert.assertTrue(loginSuccess,
                    "❌ [" + testCaseID + "] Login should succeed but failed." +
                            " URL: " + driver.getCurrentUrl());

            System.out.println("✅ PASSED — " + testCaseID +
                    " | Login successful");

        } else {

            // ✅ Invalid credentials — should show error message
            String actualErrMsg = loginPage.getErrorMessage();

            Reporter.log("▶ TC: " + testCaseID + " | " + testScenario, true);
            Reporter.log("   Expected Error : " + expectedErrMsg, true);
            Reporter.log("   Actual Error   : " + actualErrMsg, true);

            // ✅ Assert still on login page
            Assert.assertTrue(loginPage.isOnLoginPage(),
                    "❌ [" + testCaseID + "] Should stay on login page " +
                            "but redirected to: " + driver.getCurrentUrl());

            // ✅ Assert error message shown
            Assert.assertFalse(actualErrMsg.isEmpty(),
                    "❌ [" + testCaseID + "] Error message should be " +
                            "visible but was empty.");

            // ✅ Assert error message matches expected (if provided)
            if (!expectedErrMsg.isEmpty()) {
                Assert.assertTrue(
                        actualErrMsg.toLowerCase()
                                .contains(expectedErrMsg.toLowerCase()) ||
                                expectedErrMsg.toLowerCase()
                                        .contains(actualErrMsg.toLowerCase()),
                        "❌ [" + testCaseID + "] Error message mismatch!" +
                                "\n   Expected : " + expectedErrMsg +
                                "\n   Actual   : " + actualErrMsg
                );
            }

            System.out.println("✅ PASSED — " + testCaseID +
                    " | Error validated: " + actualErrMsg);
        }
    }

    // ═══════════════════════════════════════════════
    // TEST — Validate Logo on Login Page
    // ═══════════════════════════════════════════════
    @Test(description = "Validate FootPrints logo is visible on login page")
    public void validateLoginPageLogo() {
        boolean logoVisible = loginPage.validateFootPrintsLogo();
        Reporter.log("▶ FootPrints Logo visible: " + logoVisible, true);
        Assert.assertTrue(logoVisible,
                "❌ FootPrints logo not visible on login page");
        System.out.println("✅ PASSED — Logo is visible on login page");
    }
}
