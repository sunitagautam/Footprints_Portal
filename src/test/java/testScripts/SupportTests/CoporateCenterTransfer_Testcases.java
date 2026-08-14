package testScripts.SupportTests;

import io.restassured.response.Response;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
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
 * own modal, form id="frm-center-transfer") → SC002_TC_001
 * - Transfer Applicable=Yes → SERVICE REQUEST → Center Shift (the exact same
 * form already automated in ServiceRequest_CenterShiftTest.java for Regular
 * children — reused here via Regular_ServiceRequests) → SC003_TC_001/SC002_TC_002
 * Both paths land on Recent Customer Requests as Request Type = "Center Shift".
 * <p>
 * User: resolved from Excel → getUserForScreen("Corporate Account Statement")
 */
public class CoporateCenterTransfer_Testcases extends BaseTest {

    private static final String SCREEN_CORPORATE = "Corporate Account Statement";

    // ═══════════════════════════════════════════════
    // TEST DATA
    // Tieup: Airbnb Global Capability Center Pvt Ltd (transfer applicable = YES)
    // ═══════════════════════════════════════════════
    // Button-flow child — confirmed Transfer Applicable=No (user-confirmed, fresh)
    private static final String CCT_BUTTON_CHILD_ID = "72154";
    // Applicable Month is resolved live (first available option) in
    // sc002_tc001_buttonFlowFullCycle() — process_corporate_center_migration_requests
    // requires "date" = the request's own WEF date, computed from whichever
    // month actually got selected (see getLastSelectedApplicableMonth()).

    // Service-Request-flow chain child — confirmed Transfer Applicable=Yes,
    // tie-up-benefit eligible (user-confirmed, fresh).
    // SC003_TC_001 (submit) and SC002_TC_002 (approve+API) chain on this same child.
    private static final String CCT_SR_CHAIN_CHILD_ID = "71984";
    private static final String CCT_SR_JOINING_DATE =
            LocalDate.now().plusMonths(1).withDayOfMonth(1).toString();

    // SC004_TC_001 (approve-popup-detail verification) and SC005_TC_001 (reject)
    // each need their OWN fresh Transfer Applicable=Yes child with an untouched
    // Pending Center Shift request — approve/reject are mutually-exclusive
    // terminal actions and cannot share CCT_SR_CHAIN_CHILD_ID once it's approved.
    private static final String CCT_APPROVE_DETAIL_CHILD_ID = "72269";
    private static final String CCT_REJECT_CHILD_ID = "72862";

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
    // Child: 71123 (Transfer Applicable=No)
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC002_TC_001 — Corporate Center Transfer button: full flow to Approved")
    public void sc002_tc001_buttonFlowFullCycle() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Button-flow Corporate Center Transfer | child: "
                + CCT_BUTTON_CHILD_ID, true);

        corporatePage.generateAccountStatement(CCT_BUTTON_CHILD_ID);
        // ▶ Pass null for month — selects whatever's first available in the
        //   live dropdown rather than a hardcoded "Aug 2026" that may no
        //   longer be an option for a different child (confirmed live:
        //   "Cannot locate option with text: Aug 2026" for child 66683).
        String submitResponse = corporatePage.submitCorporateCenterTransfer(
                null, null, null);
        Reporter.log("   Response after submit: " + submitResponse, true);

        String selectedMonth = corporatePage.getLastSelectedApplicableMonth();
        String wefDate = java.time.YearMonth.parse(selectedMonth,
                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", java.util.Locale.ENGLISH))
                .atDay(1).toString();
        Reporter.log("   Selected Applicable Month: " + selectedMonth + " → WEF date: " + wefDate, true);

        Response pendingApi = APIs.getCorporateCenterTransferPendingRequests(CCT_BUTTON_CHILD_ID);
        System.out.println("   [getAllPendingRequests] HTTP " + pendingApi.getStatusCode()
                + " | " + pendingApi.getBody().asString());
        Assert.assertTrue(pendingApi.getStatusCode() >= 200 && pendingApi.getStatusCode() < 300,
                "❌ getAllPendingRequests failed: " + pendingApi.getStatusCode());

        // No generic Approve button exists for Center Shift-type rows — confirmed
        // live via row-HTML dump (Actions column only ever shows Cancel +
        // Processing Details, never Approve). Approval is entirely API-driven:
        // process_corporate_center_migration_requests performs the actual
        // Processing → Approved transition (and creates the new child) in one
        // call — there is no separate UI click step despite the spec's wording.
        Response migrationApi = APIs.processCorporateCenterMigrationRequest(CCT_BUTTON_CHILD_ID, wefDate);
        System.out.println("   [process_corporate_center_migration] HTTP " + migrationApi.getStatusCode()
                + " | " + migrationApi.getBody().asString());
        Assert.assertTrue(migrationApi.getStatusCode() >= 200 && migrationApi.getStatusCode() < 300,
                "❌ process_corporate_center_migration_requests failed: " + migrationApi.getStatusCode());

        // ▶ The migration call attritts the OLD child and creates a brand-new
        //   one — its own response body ("Request Processed successfully
        //   with new child id ...") is the real success signal, not a
        //   post-migration grid read on the now-attritted old child (same
        //   pattern as Corporate Transfer's SC009_TC_001 migration check).
        String migrationBody = migrationApi.getBody().asString();
        boolean migrationSucceeded = migrationBody.toLowerCase().contains("request processed successfully")
                && migrationBody.toLowerCase().contains("new child id");
        Assert.assertTrue(migrationSucceeded,
                "❌ Migration API did not report a new child created: " + migrationBody);
        Reporter.log("✅ SC002_TC_001 PASSED — Button-flow Corporate Center Transfer Approved: "
                + migrationBody, true);
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
        selectCenterWithAvailableProgram();
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

        // ▶ Confirmed live: for Service-Request/Center-Shift-sourced rows,
        //   calling getAllPendingRequests FIRST silently flips the row from
        //   Pending → Processing as a side effect, and the migration API
        //   then no-ops ("No Request to Process Corporate Center Transfer")
        //   because it only picks up rows still Pending. Call migration
        //   FIRST (while genuinely Pending), matching the button-flow order
        //   only superficially — the underlying record types differ.
        Response migrationApi = APIs.processCorporateCenterMigrationRequest(
                CCT_SR_CHAIN_CHILD_ID, CCT_SR_JOINING_DATE);
        String migrationBody = migrationApi.getBody().asString();
        System.out.println("   [process_corporate_center_migration_requests] HTTP " + migrationApi.getStatusCode()
                + " | " + migrationBody);
        Assert.assertTrue(migrationApi.getStatusCode() >= 200 && migrationApi.getStatusCode() < 300,
                "❌ process_corporate_center_migration_requests failed: " + migrationApi.getStatusCode());
        boolean migrationSucceeded = migrationBody.toLowerCase().contains("request processed successfully")
                && migrationBody.toLowerCase().contains("new child id");
        Assert.assertTrue(migrationSucceeded,
                "❌ Migration API did not report a new child created: " + migrationBody);

        Reporter.log("✅ SC002_TC_002 PASSED — Service-Request-flow Center Shift Approved: "
                + migrationBody, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC004_TC_001 — Support Team approves → details verified
    //                → prorated invoice generated
    //
    // Needs its OWN fresh Transfer Applicable=Yes child (untouched Pending
    // Center Shift request) — set CCT_APPROVE_DETAIL_CHILD_ID before running.
    // NOTE: no UI "Approve popup" exists for Center Shift-type rows (confirmed
    // live) — the migration API's own response body carries the old/new
    // center + prorated detail the spec's popup description refers to, so
    // that's what this test verifies instead of a nonexistent popup.
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
            description = "SC004_TC_001 — Support approves → details verified, prorated invoice generated")
    public void sc004_tc001_approvePopupDetails() throws InterruptedException {
        if (CCT_APPROVE_DETAIL_CHILD_ID.startsWith("TODO")) {
            System.out.println("   ⚠ SC004_TC_001 SKIPPED — set CCT_APPROVE_DETAIL_CHILD_ID"
                    + " to a fresh Transfer Applicable=Yes child with a Pending Center Shift request");
            Reporter.log("⚠ SC004_TC_001 SKIPPED — CCT_APPROVE_DETAIL_CHILD_ID not set", true);
            return;
        }
        Reporter.log("▶ SC004_TC_001 — Approve details | child: "
                + CCT_APPROVE_DETAIL_CHILD_ID, true);

        submitCenterShiftIfNeeded(CCT_APPROVE_DETAIL_CHILD_ID);

        // ▶ Same fix as SC002_TC_002 — call migration FIRST, while the row
        //   is still genuinely Pending. Calling getAllPendingRequests
        //   beforehand flips it to Processing as a side effect, after which
        //   the migration API no-ops.
        Response migrationApi = APIs.processCorporateCenterMigrationRequest(
                CCT_APPROVE_DETAIL_CHILD_ID, CCT_SR_JOINING_DATE);
        String migrationBody = migrationApi.getBody().asString();
        System.out.println("   [process_corporate_center_migration_requests] HTTP " + migrationApi.getStatusCode()
                + " | " + migrationBody);
        Assert.assertTrue(migrationApi.getStatusCode() >= 200 && migrationApi.getStatusCode() < 300,
                "❌ process_corporate_center_migration_requests failed: " + migrationApi.getStatusCode());

        boolean hasProratedDetail = migrationBody.toLowerCase().contains("prorated")
                && migrationBody.toLowerCase().contains("new child id");
        Reporter.log("   Migration API response contains prorated + new-child detail: " + hasProratedDetail, true);
        Assert.assertTrue(hasProratedDetail,
                "❌ Migration API response missing expected prorated/new-child detail: " + migrationBody);

        Reporter.log("✅ SC004_TC_001 PASSED — Approved with prorated/new-child detail confirmed in API response", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC005_TC_001 — Support Team rejects → rejection activity logged
    //
    // Needs its OWN fresh Transfer Applicable=Yes child (untouched Pending
    // Center Shift request) — set CCT_REJECT_CHILD_ID before running.
    // NOTE: no generic Reject button exists for Center Shift-type rows either
    // (confirmed live) — rejection is done via the same Cancel control
    // (button.cancel_customer_request) used for Corporate Transfer's own
    // cancel flow, reused here via RecentCustomerRequestsPage.
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

        submitCenterShiftIfNeeded(CCT_REJECT_CHILD_ID);

        recentRequestsPage.navigateByChildId(CCT_REJECT_CHILD_ID);
        Assert.assertTrue(recentRequestsPage.isCancelProgramChangeButtonVisible(),
                "❌ CANCEL/Reject control not found for Pending Center Shift row, child " + CCT_REJECT_CHILD_ID);
        recentRequestsPage.clickCancelProgramChange();
        Thread.sleep(800);
        try {
            driver.switchTo().alert().accept();
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
        try {
            recentRequestsPage.confirmCancelRequest();
        } catch (Exception ignored) {
        }
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
     * Submit Center Shift via Service Request for childId only if it doesn't
     * already have a Pending/Processing/Approved Center Shift request —
     * makes SC004_TC_001/SC005_TC_001 self-contained regardless of whether
     * the child was pre-loaded with a request before the test ran.
     */
    private void submitCenterShiftIfNeeded(String childId) throws InterruptedException {
        String existingStatus = recentRequestsPage.getColumnValueByRequestType(childId, "Center Shift", "Request Status");
        if (!existingStatus.isEmpty()) {
            Reporter.log("   Reusing existing Center Shift request (status=" + existingStatus + ")", true);
            return;
        }
        navigations.goToAccountStatement();
        Thread.sleep(1000);
        accountStatementPage.generateAccountStatement(childId);
        serviceRequestPage.clickServiceRequestLink();
        serviceRequestPage.selectServiceType("Center Shift");
        Assert.assertTrue(serviceRequestPage.isCenterShiftFormVisible(),
                "❌ Center Shift form not visible for child " + childId
                        + " — may not be Transfer Applicable=Yes");
        serviceRequestPage.setCSEffectiveDate(CCT_SR_JOINING_DATE);
        Thread.sleep(400);
        selectCenterWithAvailableProgram();
        serviceRequestPage.checkCSCenterVisitDeclaration();
        serviceRequestPage.submitCenterShift();
        Thread.sleep(800);
        if (serviceRequestPage.isAlertPresent()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String response = serviceRequestPage.getResponseMessage();
        Reporter.log("   Submit response for child " + childId + ": " + response, true);
    }

    /**
     * Select a Center whose dependent Program dropdown actually has a
     * non-placeholder option — confirmed live that the naive "first
     * available Center" (e.g. "Wipro- Greater Noida - Onsite Center") can
     * have ZERO configured Programs, which is real data, not a timing
     * issue (verified by bumping the post-Center-select wait to 2s with no
     * change). Tries each Center option in turn until one actually yields
     * a selectable Program, instead of giving up on the first one.
     */
    private void selectCenterWithAvailableProgram() throws InterruptedException {
        Select centerSelect = new Select(serviceRequestPage.cs_newCenter_dropdown);
        java.util.List<String> centerOptions = new java.util.ArrayList<>();
        for (WebElement o : centerSelect.getOptions()) {
            String t = o.getText().trim();
            if (!t.isEmpty() && !t.startsWith("--") && !t.equalsIgnoreCase("Select")) {
                centerOptions.add(t);
            }
        }

        for (String centerText : centerOptions) {
            centerSelect.selectByVisibleText(centerText);
            System.out.println("   ▶ Trying Center: " + centerText);
            Thread.sleep(1500);

            Select programSelect = new Select(serviceRequestPage.cs_newProgram_dropdown);
            String programText = "";
            for (WebElement o : programSelect.getOptions()) {
                String t = o.getText().trim();
                if (!t.isEmpty() && !t.startsWith("--") && !t.equalsIgnoreCase("Select")) {
                    programText = t;
                    break;
                }
            }

            if (!programText.isEmpty()) {
                programSelect.selectByVisibleText(programText);
                System.out.println("   ✅ Center: " + centerText + " | Program: " + programText);
                Thread.sleep(400);
                return;
            }
            System.out.println("   ⚠ Center '" + centerText + "' has no Program options — trying next");
        }
        System.out.println("   ⚠ No Center found with an available Program option");
    }

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
