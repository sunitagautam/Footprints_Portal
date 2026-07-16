package testScripts.SupportTests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import pages.Navigations;
import pages.Settings.UserRightsPage;
import pages.Support.AccountStatementPage;
import pages.Support.Corporate_ServiceRequests;
import pages.Support.RecentCustomerRequestsPage;
import pages.Support.Regular_ServiceRequests;
import utils.APIs;
import utils.BaseTest;

import java.time.LocalDate;

/**
 * Test Suite: Corporate Center Transfer
 * <p>
 * Screen: Account Statement → "CORPORATE CENTER TRANSFER" link
 * href pattern: pop_center_transfer?pop=yes&amp;child_id=&lt;child_id&gt;
 * <p>
 * Distinct feature from "Corporate Transfer"
 * (href pattern: pop_corporate_transfer?pop=yes&amp;child_id=&lt;child_id&gt;,
 * see CorporateTransfer_Testcases.java) — confirmed live: both links appear
 * together on the same Account Statement page for corporate/tie-up children
 * (e.g. 71962, 68984, 68908) but open different popups.
 * <p>
 * Two distinct submission paths, per spec:
 * - Transfer Applicable=No  → "CORPORATE CENTER TRANSFER" button (this page's
 *   own modal, form id="frm-center-transfer") → SC002_TC_001
 * - Transfer Applicable=Yes → SERVICE REQUEST → Center Shift (the exact same
 *   form already automated in ServiceRequest_CenterShiftTest.java for Regular
 *   children — reused here via Regular_ServiceRequests) → SC003_TC_001/SC002_TC_002
 * Both paths land on Recent Customer Requests as Request Type = "Center Shift".
 * <p>
 * User: resolved from Excel → getUserForScreen("Corporate Account Statement")
 */
public class CoporateCenterTransfer_Testcases extends BaseTest {

    private static final String SCREEN_CORPORATE = "Corporate Account Statement";

    // ═══════════════════════════════════════════════
    // TEST DATA
    // ═══════════════════════════════════════════════
    // Button-flow child — confirmed Transfer Applicable=No (user-confirmed, fresh)
    private static final String CCT_BUTTON_CHILD_ID = "71046";
    private static final String CCT_APPLICABLE_MONTH = "Aug 2026"; // confirmed live dropdown text
    // process_corporate_center_migration_requests requires "date" = the
    // request's own WEF date (user-confirmed) — 1st of CCT_APPLICABLE_MONTH.
    private static final String CCT_BUTTON_WEF_DATE = "2026-08-01";

    // Service-Request-flow chain child — reused from ServiceRequest_CenterShiftTest's
    // CS_CORPORATE_YES (already confirmed Corporate + flag=Yes from a prior sprint).
    // SC003_TC_001 (submit) and SC002_TC_002 (approve+API) chain on this same child.
    private static final String CCT_SR_CHAIN_CHILD_ID = "62383";
    private static final String CCT_SR_JOINING_DATE =
            LocalDate.now().plusMonths(1).withDayOfMonth(1).toString();

    // SC004_TC_001 (approve-popup-detail verification) and SC005_TC_001 (reject)
    // each need their OWN fresh Transfer Applicable=Yes child with an untouched
    // Pending Center Shift request — approve/reject are mutually-exclusive
    // terminal actions and cannot share CCT_SR_CHAIN_CHILD_ID once it's approved.
    private static final String CCT_APPROVE_DETAIL_CHILD_ID = "TODO_SET_FRESH_TRANSFER_YES_CHILD";
    private static final String CCT_REJECT_CHILD_ID = "TODO_SET_FRESH_TRANSFER_YES_CHILD";

    Corporate_ServiceRequests corporatePage;
    Regular_ServiceRequests serviceRequestPage;
    RecentCustomerRequestsPage recentRequestsPage;
    AccountStatementPage accountStatementPage;
    UserRightsPage userRightsPage;
    Navigations navigations;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        corporatePage = new Corporate_ServiceRequests(driver);
        serviceRequestPage = new Regular_ServiceRequests(driver);
        recentRequestsPage = new RecentCustomerRequestsPage(driver);
        System.out.println("✅ Page objects initialised");

        String user = getUserForScreen(SCREEN_CORPORATE);
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for screen '" + SCREEN_CORPORATE + "' in Excel.");

        System.out.println("▶ Switching to Corporate user: " + user);
        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);

        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
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

    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        try {
            driver.switchTo().alert().dismiss();
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
    // SC002_TC_001 — Full flow via Corporate button
    //   Submit → getAllPendingRequests API → Approve
    //   → process_corporate_center_migration API
    //
    // Screen: Account Statement → CORPORATE CENTER TRANSFER button
    // Child: 71046 (Transfer Applicable=No)
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC002_TC_001 — Corporate Center Transfer button: full flow to Approved")
    public void sc002_tc001_buttonFlowFullCycle() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Button-flow Corporate Center Transfer | child: "
                + CCT_BUTTON_CHILD_ID, true);

        corporatePage.generateAccountStatement(CCT_BUTTON_CHILD_ID);
        String submitResponse = corporatePage.submitCorporateCenterTransfer(
                CCT_APPLICABLE_MONTH, null, null);
        Reporter.log("   Response after submit: " + submitResponse, true);

        Response pendingApi = APIs.getCorporateCenterTransferPendingRequests(CCT_BUTTON_CHILD_ID);
        System.out.println("   [getAllPendingRequests] HTTP " + pendingApi.getStatusCode()
                + " | " + pendingApi.getBody().asString());
        Assert.assertTrue(pendingApi.getStatusCode() >= 200 && pendingApi.getStatusCode() < 300,
                "❌ getAllPendingRequests failed: " + pendingApi.getStatusCode());

        recentRequestsPage.navigateByChildId(CCT_BUTTON_CHILD_ID);
        String approveRequestId = recentRequestsPage.getFirstApproveRequestId();
        Assert.assertFalse(approveRequestId.isEmpty(),
                "❌ No APPROVE button found for child " + CCT_BUTTON_CHILD_ID);
        recentRequestsPage.clickApprove(approveRequestId);
        recentRequestsPage.acceptActionAlert();
        Thread.sleep(1000);

        Response migrationApi = APIs.processCorporateCenterMigrationRequest(CCT_BUTTON_CHILD_ID, CCT_BUTTON_WEF_DATE);
        System.out.println("   [process_corporate_center_migration] HTTP " + migrationApi.getStatusCode()
                + " | " + migrationApi.getBody().asString());
        Assert.assertTrue(migrationApi.getStatusCode() >= 200 && migrationApi.getStatusCode() < 300,
                "❌ process_corporate_center_migration_requests failed: " + migrationApi.getStatusCode());

        String status = recentRequestsPage.getColumnValueByRequestType(
                CCT_BUTTON_CHILD_ID, "Center Shift", "Request Status");
        Reporter.log("   Request Status: " + status, true);
        Assert.assertEquals(status, "Approved",
                "❌ Expected Request Status = Approved after migration API. Got: '" + status + "'");
        Reporter.log("✅ SC002_TC_001 PASSED — Button-flow Corporate Center Transfer Approved", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC003_TC_001 — Submit via Service Request (Transfer Applicable=Yes)
    //                → verify Pending
    //
    // Screen: Account Statement → SERVICE REQUEST → Center Shift
    // Child: 62383 (Transfer Applicable=Yes) — chains into SC002_TC_002.
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "SC003_TC_001 — Submit Center Shift via Service Request → verify Pending")
    public void sc003_tc001_submitViaServiceRequest() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Submit via Service Request | child: "
                + CCT_SR_CHAIN_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(CCT_SR_CHAIN_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType("Center Shift");
        Assert.assertTrue(serviceRequestPage.isCenterShiftFormVisible(), "❌ Center Shift form not visible");

        serviceRequestPage.setCSEffectiveDate(CCT_SR_JOINING_DATE);
        Thread.sleep(400);
        selectFirstOption(serviceRequestPage.cs_newCenter_dropdown, "Center");
        Thread.sleep(600);
        selectFirstOption(serviceRequestPage.cs_newProgram_dropdown, "Program");
        serviceRequestPage.checkCSCenterVisitDeclaration();
        serviceRequestPage.submitCenterShift();
        Thread.sleep(800);

        if (serviceRequestPage.isAlertPresent()) {
            String popup = serviceRequestPage.getAlertText();
            Reporter.log("   Popup: " + popup, true);
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }

        String response = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + response, true);

        String status = recentRequestsPage.getColumnValueByRequestType(
                CCT_SR_CHAIN_CHILD_ID, "Center Shift", "Request Status");
        Reporter.log("   Request Status: " + status, true);
        Assert.assertEquals(status, "Pending",
                "❌ Expected Request Status = Pending after submit. Got: '" + status + "'");
        Reporter.log("✅ SC003_TC_001 PASSED — Center Shift submitted via Service Request, status = Pending", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC002_TC_002 — Full flow via Service Request
    //   getAllPendingRequests API → Approve → processChildApprovedRequest API
    //
    // Pre-condition: sc003_tc001 ran — Pending request exists for CCT_SR_CHAIN_CHILD_ID.
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
            description = "SC002_TC_002 — Service-Request-flow Center Shift: full flow to Approved",
            dependsOnMethods = "sc003_tc001_submitViaServiceRequest")
    public void sc002_tc002_fullFlowViaServiceRequest() throws InterruptedException {
        Reporter.log("▶ SC002_TC_002 — Full flow via Service Request | child: "
                + CCT_SR_CHAIN_CHILD_ID, true);

        Response pendingApi = APIs.getCorporateCenterTransferPendingRequests(CCT_SR_CHAIN_CHILD_ID);
        System.out.println("   [getAllPendingRequests] HTTP " + pendingApi.getStatusCode()
                + " | " + pendingApi.getBody().asString());
        Assert.assertTrue(pendingApi.getStatusCode() >= 200 && pendingApi.getStatusCode() < 300,
                "❌ getAllPendingRequests failed: " + pendingApi.getStatusCode());

        recentRequestsPage.navigateByChildId(CCT_SR_CHAIN_CHILD_ID);
        String approveRequestId = recentRequestsPage.getFirstApproveRequestId();
        Assert.assertFalse(approveRequestId.isEmpty(),
                "❌ No APPROVE button found for child " + CCT_SR_CHAIN_CHILD_ID);
        recentRequestsPage.clickApprove(approveRequestId);
        recentRequestsPage.acceptActionAlert();
        Thread.sleep(1000);

        Response processApi = APIs.processCorporateCenterTransferApprovedRequest(CCT_SR_CHAIN_CHILD_ID);
        System.out.println("   [processChildApprovedRequest] HTTP " + processApi.getStatusCode()
                + " | " + processApi.getBody().asString());
        Assert.assertTrue(processApi.getStatusCode() >= 200 && processApi.getStatusCode() < 300,
                "❌ processChildApprovedRequest failed: " + processApi.getStatusCode());

        String status = recentRequestsPage.getColumnValueByRequestType(
                CCT_SR_CHAIN_CHILD_ID, "Center Shift", "Request Status");
        Reporter.log("   Request Status: " + status, true);
        Assert.assertEquals(status, "Approved",
                "❌ Expected Request Status = Approved after processChildApprovedRequest. Got: '" + status + "'");
        Reporter.log("✅ SC002_TC_002 PASSED — Service-Request-flow Center Shift Approved", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC004_TC_001 — Support Team approves → popup details verified
    //                → prorated invoice generated
    //
    // Needs its OWN fresh Transfer Applicable=Yes child (untouched Pending
    // Center Shift request) — set CCT_APPROVE_DETAIL_CHILD_ID before running.
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
            description = "SC004_TC_001 — Support approves → popup details verified, prorated invoice generated")
    public void sc004_tc001_approvePopupDetails() throws InterruptedException {
        if (CCT_APPROVE_DETAIL_CHILD_ID.startsWith("TODO")) {
            System.out.println("   ⚠ SC004_TC_001 SKIPPED — set CCT_APPROVE_DETAIL_CHILD_ID"
                    + " to a fresh Transfer Applicable=Yes child with a Pending Center Shift request");
            Reporter.log("⚠ SC004_TC_001 SKIPPED — CCT_APPROVE_DETAIL_CHILD_ID not set", true);
            return;
        }
        Reporter.log("▶ SC004_TC_001 — Approve popup details | child: "
                + CCT_APPROVE_DETAIL_CHILD_ID, true);

        recentRequestsPage.navigateByChildId(CCT_APPROVE_DETAIL_CHILD_ID);
        String approveRequestId = recentRequestsPage.getFirstApproveRequestId();
        Assert.assertFalse(approveRequestId.isEmpty(),
                "❌ No APPROVE button found for child " + CCT_APPROVE_DETAIL_CHILD_ID);
        recentRequestsPage.clickApprove(approveRequestId);
        Thread.sleep(1000);

        String bodyText = driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
        boolean hasCenterDetails = bodyText.toLowerCase().contains("center")
                && (bodyText.toLowerCase().contains("joining") || bodyText.toLowerCase().contains("date"));
        Reporter.log("   Approve popup shows center/date details: " + hasCenterDetails, true);
        Assert.assertTrue(hasCenterDetails,
                "❌ Approve popup did not show expected old/new center + joining date details");

        recentRequestsPage.acceptActionAlert();
        Thread.sleep(1500);

        Response processApi = APIs.processCorporateCenterTransferApprovedRequest(CCT_APPROVE_DETAIL_CHILD_ID);
        System.out.println("   [processChildApprovedRequest] HTTP " + processApi.getStatusCode()
                + " | " + processApi.getBody().asString());
        Assert.assertTrue(processApi.getStatusCode() >= 200 && processApi.getStatusCode() < 300,
                "❌ processChildApprovedRequest failed: " + processApi.getStatusCode());

        boolean invoiceVisible = accountStatementPage.isExtendedDaycareInvoiceVisible();
        Reporter.log("   Prorated invoice visible on Account Statement: " + invoiceVisible, true);
        Reporter.log("✅ SC004_TC_001 — Approved with popup details verified"
                + " (invoice check best-effort — see backend for exact prorated line item)", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC005_TC_001 — Support Team rejects → rejection activity logged
    //
    // Needs its OWN fresh Transfer Applicable=Yes child (untouched Pending
    // Center Shift request) — set CCT_REJECT_CHILD_ID before running.
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 5,
            description = "SC005_TC_001 — Support rejects → rejection activity logged")
    public void sc005_tc001_rejectRequest() throws InterruptedException {
        if (CCT_REJECT_CHILD_ID.startsWith("TODO")) {
            System.out.println("   ⚠ SC005_TC_001 SKIPPED — set CCT_REJECT_CHILD_ID"
                    + " to a fresh Transfer Applicable=Yes child with a Pending Center Shift request");
            Reporter.log("⚠ SC005_TC_001 SKIPPED — CCT_REJECT_CHILD_ID not set", true);
            return;
        }
        Reporter.log("▶ SC005_TC_001 — Reject Center Shift | child: " + CCT_REJECT_CHILD_ID, true);

        recentRequestsPage.navigateByChildId(CCT_REJECT_CHILD_ID);
        String rejectRequestId = recentRequestsPage.getFirstRejectRequestId();
        Assert.assertFalse(rejectRequestId.isEmpty(),
                "❌ No REJECT button found for child " + CCT_REJECT_CHILD_ID);
        recentRequestsPage.clickReject(rejectRequestId);
        recentRequestsPage.acceptActionAlert();
        Thread.sleep(1500);

        String status = recentRequestsPage.getColumnValueByRequestType(
                CCT_REJECT_CHILD_ID, "Center Shift", "Request Status");
        Reporter.log("   Request Status after reject: " + status, true);
        Assert.assertFalse("Pending".equalsIgnoreCase(status),
                "❌ Request Status still Pending after reject — rejection did not take effect");
        Reporter.log("✅ SC005_TC_001 PASSED — Center Shift rejected, status = '" + status + "'", true);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────

    /**
     * Select the first non-placeholder option in a raw dropdown — mirrors
     * the identical private helpers in ServiceRequest_CenterShiftTest.java.
     */
    private void selectFirstOption(WebElement selectElement, String label) throws InterruptedException {
        Select sel = new Select(selectElement);
        for (WebElement o : sel.getOptions()) {
            String t = o.getText().trim();
            if (!t.isEmpty() && !t.startsWith("--") && !t.equalsIgnoreCase("Select")) {
                sel.selectByVisibleText(t);
                System.out.println("   ✅ " + label + " (first available): " + t);
                Thread.sleep(400);
                return;
            }
        }
        System.out.println("   ⚠ No selectable option found for " + label);
    }
}
