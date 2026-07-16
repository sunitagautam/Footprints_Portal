package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.UserRightsPage;
import pages.Support.AccountStatementPage;
import pages.Support.Corporate_ServiceRequests;
import utils.BaseTest;

/**
 * Test Suite: Tie-Up Program Change (Corporate/Co-Pay/Employee tie-up children)
 * <p>
 * Screen: Account Statement → "TIE UP PROGRAM CHANGE" button
 * href pattern: javascript:addTieupProgramChange('&lt;child_id&gt;')
 * <p>
 * Child ID: 71962
 * User: resolved from Excel → getUserForScreen("Corporate Account Statement")
 * <p>
 * Split out of Corporate_ServiceRequestTestcases.java — page object
 * (Corporate_ServiceRequests.java) stays shared across all Corporate
 * service-request test classes.
 */
public class TieupProgramChange_Testcases extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA — Tieup Program Change
    // Flow (PDF confirmed):
    //   Add modal  → processingDate → saved toast
    //   Approve modal → wefDate + fee fields → approved toast
    // ═══════════════════════════════════════════════
    private static final String TIEUP_PC_CHILD_ID = "71962";
    private static final String TIEUP_PROGRAM_NAME = "Half Day";
    private static final String TIEUP_PROCESSING_DATE = "2026-07-23"; // ISO YYYY-MM-DD
    private static final String TIEUP_WEF_DATE = "2026-07-23"; // same as processing
    private static final String TIEUP_FEE_BREAKUP = "8000";
    private static final String TIEUP_PARENT_MONTHLY = "3000";
    private static final String TIEUP_CORPORATE_MONTHLY = "5000";

    // Toast text confirmed from PDF screenshots (uppercase match)
    private static final String TOAST_TIEUP_APPROVED = "APPROVED SUCCESSFULLY";

    // ═══════════════════════════════════════════════
    // EXCEL KEY — screen name for getUserForScreen()
    //   Screen Name : Corporate Account Statement
    //   Right Title : Tieup_SPOC_Access
    //   User Name   : Varsha Jha
    // ═══════════════════════════════════════════════
    private static final String SCREEN_CORPORATE = "Corporate Account Statement";

    // ═══════════════════════════════════════════════
    // PAGE OBJECTS
    // ═══════════════════════════════════════════════
    Corporate_ServiceRequests corporatePage;
    AccountStatementPage accountStatementPage;
    UserRightsPage userRightsPage;
    Navigations navigations;

    // ═══════════════════════════════════════════════
    // BEFORE CLASS
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        corporatePage = new Corporate_ServiceRequests(driver);
        System.out.println("✅ Page objects initialised");

        String user = getUserForScreen(SCREEN_CORPORATE);
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for screen '" + SCREEN_CORPORATE + "' in Excel. "
                        + "Add row: Screen Name=Corporate Account Statement | "
                        + "Right Title=Tieup_SPOC_Access | User Name=Varsha Jha");

        System.out.println("▶ Switching to Corporate user: " + user);
        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);

        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @BeforeMethod");
        } catch (Exception ignored) {
        }

        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal-backdrop').forEach(el=>el.remove());" +
                            "document.querySelectorAll('.modal').forEach(el=>{" +
                            "  el.style.display='none'; el.classList.remove('in','show');});" +
                            "document.body.classList.remove('modal-open');");
            Thread.sleep(300);
        } catch (Exception ignored) {
        }

        Thread.sleep(2000);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @AfterMethod");
        } catch (Exception ignored) {
        }

        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal-backdrop').forEach(el=>el.remove());" +
                            "document.querySelectorAll('.modal').forEach(el=>{" +
                            "  el.style.display='none'; el.classList.remove('in','show');});" +
                            "document.body.classList.remove('modal-open');");
        } catch (Exception ignored) {
        }

        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC001 — Tieup Program Change: Add → Save → Approve
    //
    // Flow confirmed from PDF (Jun 16 2026):
    //   1. Generate Account Statement for child 71962
    //   2. Click "TIE UP PROGRAM CHANGE" (red button)
    //   3. Select program + set processing date → Add Program Change Request
    //   4. Assert toast: "SAVED SUCCESSFULLY"
    //   5. Click "APPROVE TIEUP PROGRAM CHANGE REQUEST" (green button)
    //   6. Fill WEF date + Fee Breakup + Monthly amounts → Approve Request
    //   7. Assert toast: "APPROVED SUCCESSFULLY"
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "TC001 — Tieup Program Change: Add → Save → Approve for child 71962")
    public void tc001_tieupProgramChange() throws InterruptedException {
        Reporter.log("▶ TC001 — Tieup Program Change | child: " + TIEUP_PC_CHILD_ID, true);

        corporatePage.generateAccountStatement(TIEUP_PC_CHILD_ID);
        Reporter.log("   Account Statement generated for child: " + TIEUP_PC_CHILD_ID, true);

        String approvedToast = corporatePage.doTieupProgramChange(
                TIEUP_PROGRAM_NAME,
                TIEUP_PROCESSING_DATE,
                TIEUP_WEF_DATE,
                TIEUP_FEE_BREAKUP,
                TIEUP_PARENT_MONTHLY,
                TIEUP_CORPORATE_MONTHLY);

        Reporter.log("   Approval toast: " + approvedToast, true);

        Assert.assertTrue(
                approvedToast.toUpperCase().contains(TOAST_TIEUP_APPROVED),
                "❌ Expected approval toast containing '" + TOAST_TIEUP_APPROVED
                        + "'. Got: " + approvedToast);

        Reporter.log("✅ TC001 PASSED — Tieup Program Change approved for child: "
                + TIEUP_PC_CHILD_ID, true);
        Reporter.log("   Program: " + TIEUP_PROGRAM_NAME
                + " | WEF: " + TIEUP_WEF_DATE, true);
        Reporter.log("   ℹ Backend: verify program=" + TIEUP_PROGRAM_NAME
                + ", status=Approved, wef_date=" + TIEUP_WEF_DATE, true);
    }
}
