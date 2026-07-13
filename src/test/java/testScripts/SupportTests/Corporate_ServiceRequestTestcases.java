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
 * Test Suite: Corporate Service Requests
 * <p>
 * Features under test:
 * 1. Tieup Program Change  — child ID: 50947
 * 2. Corporate Transfer     — child ID: 68984
 * <p>
 * User: resolved from Excel → getUserForScreen("Corporate Account Statement")
 * Navigation: Support → Account Statement → generate for child → action link
 * <p>
 * KEY DESIGN:
 * Each test calls generateAccountStatement(childId) with its OWN child ID.
 * TieupPC uses 50947, CorporateTransfer uses 68984.
 * Page object locators are dynamic — no hardcoded child IDs.
 */
public class Corporate_ServiceRequestTestcases extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA — Child IDs (different per feature)
    // ═══════════════════════════════════════════════
    private static final String TIEUP_PC_CHILD_ID = "50947";
    private static final String CORPORATE_TRANSFER_CHILD_ID = "68984";

    // ═══════════════════════════════════════════════
    // TEST DATA — Tieup Program Change
    // Flow (PDF confirmed):
    //   Add modal  → processingDate → saved toast
    //   Approve modal → wefDate + fee fields → approved toast
    // ═══════════════════════════════════════════════
    private static final String TIEUP_PROGRAM_NAME = "Half Day";
    private static final String TIEUP_PROCESSING_DATE = "2026-06-23"; // ISO YYYY-MM-DD
    private static final String TIEUP_WEF_DATE = "2026-06-23"; // same as processing
    private static final String TIEUP_FEE_BREAKUP = "8000";
    private static final String TIEUP_PARENT_MONTHLY = "3000";
    private static final String TIEUP_CORPORATE_MONTHLY = "5000";

    // Toast text confirmed from PDF screenshots (uppercase match)
    private static final String TOAST_TIEUP_SAVED = "SAVED SUCCESSFULLY";
    private static final String TOAST_TIEUP_APPROVED = "APPROVED SUCCESSFULLY";

    // ═══════════════════════════════════════════════
    // TEST DATA — Corporate Transfer
    // ═══════════════════════════════════════════════
    private static final String CT_JOINING_MONTH = "June 2026";
    private static final String CT_OFFER_NAME = "ABP News - Sector 62 Offer";
    private static final String CT_CENTER_NAME = "Sector 122, Noida";
    private static final String CT_FEE_COMMENT = "8499";

    // ═══════════════════════════════════════════════
    // EXCEL KEY — screen name for getUserForScreen()
    // Add this row in your Excel user config sheet:
    //   Screen Name : Corporate Account Statement
    //   Right Title : Tieup_SPOC_Access
    //   User Name   : Varsha Jha  (or whoever has this role)
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
    // User resolved from Excel — not hardcoded
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        corporatePage = new Corporate_ServiceRequests(driver);
        System.out.println("✅ Page objects initialised");

        // Resolve user from Excel — Screen: "Corporate Account Statement"
        // Right Title: "Tieup_SPOC_Access" → returns Varsha Jha (or configured user)
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

        // Handle post-switch popups
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD
    // Dismiss alert + close modal + navigate to Account Statement
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        // Step 1: Dismiss any lingering alert
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @BeforeMethod");
        } catch (Exception ignored) {
        }

        // Step 2: Close any open modal/panel
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal-backdrop').forEach(el=>el.remove());" +
                            "document.querySelectorAll('.modal').forEach(el=>{" +
                            "  el.style.display='none'; el.classList.remove('in','show');});" +
                            "document.body.classList.remove('modal-open');");
            Thread.sleep(300);
        } catch (Exception ignored) {
        }

        // Step 3: Navigate to Account Statement
        Thread.sleep(2000);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        // Step 1: Dismiss alert first
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @AfterMethod");
        } catch (Exception ignored) {
        }

        // Step 2: Close any open modal
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
    // TC001 — Tieup Program Change
    //
    // Pre-condition: Child 50947 must have a valid Corporate Tieup
    // Steps:
    //   1. Generate Account Statement for child 50947
    //   2. Click Tieup Program Change link
    //   3. Select program, set processing date, submit
    //   4. Approve to Processing → fill fee breakup → approve to Approved
    //   5. Verify info rows printed (visual verification)
    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════
    // TC001 — Tieup Program Change
    //
    // Flow confirmed from PDF (Jun 16 2026):
    //   1. Generate Account Statement for child 50947
    //   2. Click "TIE UP PROGRAM CHANGE" (red button)
    //   3. Select program + set processing date → Add Program Change Request
    //   4. Assert toast: "SAVED SUCCESSFULLY"
    //   5. Click "APPROVE TIEUP PROGRAM CHANGE REQUEST" (green button)
    //   6. Fill WEF date + Fee Breakup + Monthly amounts → Approve Request
    //   7. Assert toast: "APPROVED SUCCESSFULLY"
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "TC001 — Tieup Program Change: Add → Save → Approve for child 50947")
    public void tc001_tieupProgramChange() throws InterruptedException {
        Reporter.log("▶ TC001 — Tieup Program Change | child: " + TIEUP_PC_CHILD_ID, true);

        // Step 1: Generate Account Statement for Tieup PC child (50947)
        // NOTE: Different child from Corporate Transfer (68984)
        corporatePage.generateAccountStatement(TIEUP_PC_CHILD_ID);
        Reporter.log("   Account Statement generated for child: " + TIEUP_PC_CHILD_ID, true);

        // Step 2-6: Full Tieup Program Change flow — returns approval toast
        String approvedToast = corporatePage.doTieupProgramChange(
                TIEUP_PROGRAM_NAME,
                TIEUP_PROCESSING_DATE,
                TIEUP_WEF_DATE,
                TIEUP_FEE_BREAKUP,
                TIEUP_PARENT_MONTHLY,
                TIEUP_CORPORATE_MONTHLY);

        Reporter.log("   Approval toast: " + approvedToast, true);

        // Step 7: Assert approval toast contains "APPROVED SUCCESSFULLY"
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

    // ═══════════════════════════════════════════════════════════════════════
    // TC002 — Corporate Transfer
    //
    // Pre-condition: Child 68984 must be a Corporate child
    //               with a valid offer available for transfer
    // Steps:
    //   1. Generate Account Statement for child 68984
    //   2. Click Corporate Transfer link
    //   3. Select month, offer, center → submit → accept alert
    //   4. Approve transfer → fill fee comment → approve
    //   5. Verify "Corporate Transfer Already Requested" message
    //   6. Open Customer Requests tab → verify table data
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "TC002 — Corporate Transfer for child 68984")
    public void tc002_corporateTransfer() throws InterruptedException {
        Reporter.log("▶ TC002 — Corporate Transfer | child: "
                + CORPORATE_TRANSFER_CHILD_ID, true);

        // Step 1: Generate Account Statement for TRANSFER child (68984)
        // NOTE: Different child ID from TC001
        corporatePage.generateAccountStatement(CORPORATE_TRANSFER_CHILD_ID);
        Reporter.log("   Account Statement generated for child: "
                + CORPORATE_TRANSFER_CHILD_ID, true);

        // Step 2: Run full Corporate Transfer flow
        corporatePage.doCorporateTransfer(
                CT_JOINING_MONTH,
                CT_OFFER_NAME,
                CT_CENTER_NAME,
                CT_FEE_COMMENT);

        // Step 3: Verify transfer already requested message
        String transferMsg = corporatePage.getTransferAlreadyRequestedMessage();
        Reporter.log("   Transfer message: " + transferMsg, true);
        Assert.assertFalse(transferMsg.isEmpty(),
                "❌ 'Corporate Transfer Already Requested' message not visible after approval");

        Reporter.log("✅ TC002 Step 1-3 PASSED — Transfer submitted and approved", true);

        // Step 4: Open Customer Requests in new tab
        String originalWindow = driver.getWindowHandle();
        Reporter.log("▶ Opening Customer Requests tab", true);

        String newTabUrl = corporatePage.openCustomerRequestsTab();
        Reporter.log("   Customer Requests URL: " + newTabUrl, true);

        Assert.assertTrue(
                newTabUrl.contains("customer") || newTabUrl.contains("recent"),
                "❌ Customer Requests URL unexpected: " + newTabUrl);

        // Step 5: Print and verify table data
        corporatePage.printTableData();

        // Step 6: Switch back to Account Statement
        driver.close(); // close new tab
        driver.switchTo().window(originalWindow);
        System.out.println("✅ Switched back to Account Statement window");

        Reporter.log("✅ TC002 PASSED — Corporate Transfer completed for child: "
                + CORPORATE_TRANSFER_CHILD_ID, true);
        Reporter.log("   ℹ Backend: verify transfer record for offer: "
                + CT_OFFER_NAME, true);
    }
}
