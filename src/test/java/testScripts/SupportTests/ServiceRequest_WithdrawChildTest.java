package testScripts.SupportTests;

import io.restassured.response.Response;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.UserRightsPage;
import pages.Support.AccountStatementPage;
import pages.Support.RecentCustomerRequestsPage;
import pages.Support.Regular_ServiceRequests;
import utils.APIs;
import utils.BaseTest;
import utils.IAutoConstant;

import java.time.LocalDate;

/**
 * Withdraw Child — 8 Test Suite
 * Source: TC_ServiceRequests_Withdraw.xlsx (sheet TC_Withdraw).
 * <p>
 * Priority / Execution Order:
 * 1  tc001_fullFlowByDatePath     SC001_TC_001         Submit → getAllPendingRequests → Approve →
 *                                                      processChildApprovedRequest → Approved.
 *                                                      Data-driven by date path — future-dated only
 *                                                      for now; back-dated added next sprint.
 * 2  testWithdraw_Transfer        SC001_TC_002         Submit, reason=Transfer, verify Pending.
 * 2  testWithdraw_NotSatisfied    SC001_TC_003         Submit, reason=Not Satisfied with Services.
 * 2  testWithdraw_FormalSchool    SC001_TC_004         Submit, reason=Moving to formal schooling.
 * 2  testWithdraw_Others          SC001_TC_005         Submit, reason=Others.
 * 3  tc006_pendingStatusVerify    SC001_TC_006         Submit → verify Pending status on grid.
 * 4  tc007_supportApprove         SC002_TC_002         Approve → processChildApprovedRequest → Approved.
 * 5  tc008_supportReject          SC002_TC_003         Reject → request not processed.
 * <p>
 * SC004_TC_001 (access-right validation for 'Raise_Support_Request') intentionally
 * NOT automated in this round — needs a second user without that right, which
 * doesn't currently exist as a row in testData/input_UserRights.xlsx.
 * <p>
 * PRE-CONDITION — every *_CHILD_ID constant below (tc001/tc006/tc007/tc008)
 * needs a real, unused child ID before running. The 4 reason-variant tests
 * instead take childId_Transfer/childId_NotSatisfied/childId_FormalSchool/
 * childId_Others via TestNG &lt;parameter&gt; in WithdrawChildtestng.xml — set
 * those there, not in this file. Withdrawal is one-way (child status →
 * Attrition), so each scenario needs its OWN child. Tests skip gracefully if
 * left as TODO.
 * <p>
 * CONFIRMED live (child 67543): withdraw_reason dropdown options are exactly
 * "Transfer", "Not Satisfied with Services", "Moving to formal schooling"
 * (lowercase "formal schooling" — not "Formal School"/"Formal Schooling"),
 * "Others".
 * <p>
 * UNVERIFIED — confirm against the live app before trusting:
 * - ckey for getWithdrawChildPendingRequests (reused B47C56483AAE7373, the
 * Center Shift/Extended Daycare/Time Extension ckey for this same physical
 * endpoint — the spec's own example URL omits a ckey).
 * - ckey for processWithdrawChildRequest (reused 9414D96600C5, the only ckey
 * already confirmed working against parentapp/processChildApprovedRequest,
 * from Center Shift's old-child-attrition step — the spec's own example also
 * omits a ckey here).
 */
public class ServiceRequest_WithdrawChildTest extends BaseTest {

    // ── TEST DATA — replace every TODO_* below with a real, unused child ID ──
    private static final String FUTURE_DATED_CHILD_ID = "68676";
    // private static final String BACK_DATED_CHILD_ID = "69755"; // banked — next sprint

    private static final String PENDING_STATUS_CHILD_ID = "68710";
    private static final String APPROVE_CHILD_ID = "68733";
    private static final String REJECT_CHILD_ID = "68743";
    private static final String RETAIN_CHILD_ID = "69714";
    private static final String UPDATE_REQUEST_CHILD_ID = "69819";

    // Reason-variant child IDs (SC001_TC_002-005) — supplied via TestNG
    // <parameter> in WithdrawChildtestng.xml, not hardcoded, since these are
    // the ones swapped out most often when a child gets consumed by a run.
    private String childId_Transfer;
    private String childId_NotSatisfied;
    private String childId_FormalSchool;
    private String childId_Others;

    // ── PAGE OBJECTS ─────────────────────────────────────────────────────
    private Regular_ServiceRequests serviceRequestPage;
    private AccountStatementPage accountStatementPage;
    private RecentCustomerRequestsPage recentRequestsPage;
    private UserRightsPage userRightsPage;
    private Navigations navigations;

    // ── TABS — Account Statement and Customer Request stay open side by side
    // so tests switch tabs instead of re-navigating through the app each time.
    private String acctStatementTab;
    private String customerRequestTab;

    // ── LIFECYCLE ────────────────────────────────────────────────────────
    @Parameters({"childId_Transfer", "childId_NotSatisfied", "childId_FormalSchool", "childId_Others"})
    @BeforeClass(alwaysRun = true)
    public void setUp(@Optional("TODO_REASON_TRANSFER") String childId_Transfer,
                       @Optional("TODO_REASON_NOT_SATISFIED") String childId_NotSatisfied,
                       @Optional("TODO_REASON_FORMAL_SCHOOL") String childId_FormalSchool,
                       @Optional("TODO_REASON_OTHERS") String childId_Others) throws Exception {
        this.childId_Transfer = childId_Transfer;
        this.childId_NotSatisfied = childId_NotSatisfied;
        this.childId_FormalSchool = childId_FormalSchool;
        this.childId_Others = childId_Others;

        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        serviceRequestPage = new Regular_ServiceRequests(driver);
        recentRequestsPage = new RecentCustomerRequestsPage(driver);

        String user = getUserForScreen("Account Statement");
        Assert.assertFalse(user.isEmpty(), "No user found for 'Account Statement'");
        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();

        // Open Account Statement in this tab, then Customer Request grid in a
        // second tab — both stay open for the rest of the class so tests switch
        // tabs instead of re-navigating through the app/menu each time.
        navigations.goToAccountStatement();
        acctStatementTab = driver.getWindowHandle();

        ((JavascriptExecutor) driver).executeScript("window.open();");
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(acctStatementTab)) {
                customerRequestTab = handle;
                break;
            }
        }
        driver.switchTo().window(customerRequestTab);
        // Fresh window.open() tabs start on about:blank — navigateByChildId()
        // derives its base URL from the CURRENT url, so seed a real app page first.
        driver.get(IAutoConstant.LOGIN_URL);

        driver.switchTo().window(acctStatementTab);
    }

    private void switchToAccountStatementTab() {
        driver.switchTo().window(acctStatementTab);
    }

    private void switchToCustomerRequestTab() {
        driver.switchTo().window(customerRequestTab);
    }

    @BeforeMethod(alwaysRun = true)
    public void goToAccountStatement() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        switchToAccountStatementTab();
        try {
            serviceRequestPage.closeModalByJs();
            Thread.sleep(400);
        } catch (Exception ignored) {
        }
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupAfterTest() {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        try {
            switchToAccountStatementTab();
            serviceRequestPage.closeModalByJs();
        } catch (Exception ignored) {
        }
    }

    private boolean isSkipped(String childId) {
        if (childId.startsWith("TODO")) {
            System.out.println("   ⚠ SKIPPED — set a real child ID (" + childId + ")");
            Reporter.log("⚠ SKIPPED — child ID not set: " + childId, true);
            return true;
        }
        return false;
    }

    /**
     * Submit a Withdraw Child request via the Service Request form and return
     * the toast/response message. Shared by every test that creates a fresh
     * Withdraw Child request.
     */
    private String submitWithdrawRequest(String childId, String toDate, String reason) throws InterruptedException {
        switchToAccountStatementTab();
        accountStatementPage.generateAccountStatement(childId);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType("Withdraw Child");
        Assert.assertTrue(serviceRequestPage.isWithdrawFormVisible(), "❌ Withdraw Child form not visible");

        serviceRequestPage.setWithdrawToDate(toDate);
        serviceRequestPage.selectWithdrawReason(reason);
        // The reason_comment field only renders in the DOM when reason="Others"
        // (confirmed live — for the other 3 reasons it's absent, not just
        // hidden, so waiting on its visibility times out). "Others" submits
        // silently with no popup/toast unless it's filled.
        if ("Others".equals(reason)) {
            serviceRequestPage.enterWithdrawComment("Automated test — " + reason);
        }
        Thread.sleep(300);

        serviceRequestPage.submitWithdraw();
        Thread.sleep(800);
        String popup = serviceRequestPage.getAlertText();
        System.out.println("   [Popup] " + popup);
        Reporter.log("   Popup: " + popup, true);
        if (!popup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }

        String response = serviceRequestPage.getResponseMessage();
        System.out.println("   [Response] " + response);
        Reporter.log("   Response: " + response, true);
        return response;
    }

    /**
     * ISO yyyy-MM-dd, 30 days out. setWithdrawToDate() feeds this straight into
     * JS `new Date(dateStr)` — that reliably parses ISO but not "dd-MMM-yyyy",
     * so always compute relative to today rather than hardcoding either form.
     */
    private String futureDate() {
        return LocalDate.now().plusDays(30).toString();
    }

    /**
     * Asserts the grid shows Request Status = Pending for this child's
     * Withdraw Child request, THEN hits getAllPendingRequests (background job).
     * Order matters — getAllPendingRequests is a stateful trigger, not a plain
     * read (confirmed live): calling it flips the grid straight from Pending
     * to Processing, so checking Pending must happen before that call.
     */
    private void verifyPendingStatus(String childId) throws InterruptedException {
        switchToCustomerRequestTab();
        String requestStatus = recentRequestsPage.getWithdrawRequestStatus(childId);
        System.out.println("   [Request Status before background job] " + requestStatus);
        Reporter.log("   Request Status before background job: " + requestStatus, true);
        Assert.assertEquals(requestStatus, "Pending", "❌ Request Status should be Pending before background job runs");

        Response pending = APIs.getWithdrawChildPendingRequests(childId);
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC001 — SC001_TC_001 : Full flow — getAllPendingRequests → Approve → processChildApprovedRequest
    // ════════════════════════════════════════════════════════════════════
    @DataProvider(name = "withdrawDatePaths")
    public Object[][] withdrawDatePaths() {
        return new Object[][]{
                // {dateType, toDate, childId} — WEF = today, not a real future
                // date: processChildApprovedRequest only processes AS OF the
                // WEF date (confirmed live — a 30-days-out WEF returns
                // HTTP 200/null, a silent no-op), so today is required to
                // observe the terminal Approved state within this test run.
                {"future-dated", LocalDate.now().toString(), FUTURE_DATED_CHILD_ID}
                // Next sprint: {"back-dated", LocalDate.now().minusDays(10).toString(), BACK_DATED_CHILD_ID}
        };
    }

    @Test(dataProvider = "withdrawDatePaths", priority = 1,
            description = "SC001_TC_001 — Full flow: submit → getAllPendingRequests → Approve → processChildApprovedRequest → Approved")
    public void tc001_fullFlowByDatePath(String dateType, String toDate, String childId) throws InterruptedException {
        if (isSkipped(childId)) return;
        Reporter.log("▶ TC001 SC001_TC_001 | dateType=" + dateType + " | child=" + childId + " | toDate=" + toDate, true);

        String response = submitWithdrawRequest(childId, toDate, "Transfer");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);

        // Step 2 — getAllPendingRequests
        Response pending = APIs.getWithdrawChildPendingRequests(childId);
        Assert.assertTrue(pending.getStatusCode() >= 200 && pending.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + pending.getStatusCode());
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());

        switchToCustomerRequestTab();
        String requestStatus = recentRequestsPage.getWithdrawRequestStatus(childId);
        System.out.println("   [Request Status after submit+API] " + requestStatus);
        Reporter.log("   Request Status after submit+API: " + requestStatus, true);

        if ("future-dated".equals(dateType)) {
            // Step 3 — Manual Approve on Customer Request screen (confirmed
            // live: mandatory before processChildApprovedRequest does anything
            // — calling the API without this first returns HTTP 200/null, a
            // silent no-op, not an error).
            String requestId = recentRequestsPage.getFirstRetainAttritionRequestId(childId);
            Assert.assertFalse(requestId.isEmpty(), "❌ No pending Child Attrition request found");
            recentRequestsPage.clickApproveAttrition(requestId);
            recentRequestsPage.submitApproveAttrition("Automated test approval");

            // Step 4 — processChildApprovedRequest
            Response processResp = APIs.processWithdrawChildRequest(childId);
            Assert.assertTrue(processResp.getStatusCode() >= 200 && processResp.getStatusCode() < 300,
                    "❌ Expected 2xx. Got: " + processResp.getStatusCode());
            System.out.println("   [Process] " + processResp.getBody().asString());
            Reporter.log("   Process response: " + processResp.getBody().asString(), true);

            // Step 5 — verify Approved
            Thread.sleep(1000);
            String afterStatus = recentRequestsPage.getWithdrawRequestStatus(childId);
            System.out.println("   [After process] Request Status=" + afterStatus);
            Reporter.log("   After process: Request Status=" + afterStatus, true);
            Assert.assertEquals(afterStatus, "Approved", "❌ Request Status should be Approved after processing");
        } else {
            // Back-dated: auto-approved immediately by the getAllPendingRequests call itself.
            Assert.assertEquals(requestStatus, "Approved", "❌ Back-dated request should auto-approve immediately");
        }

        Reporter.log("✅ TC001 PASSED — Withdraw Child (" + dateType + ") full flow complete", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC002 — SC001_TC_002 : Submit Withdraw Child, reason = Transfer
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC001_TC_002 — Submit Withdraw Child, reason=Transfer")
    public void testWithdraw_Transfer() throws InterruptedException {
        if (isSkipped(childId_Transfer)) return;
        Reporter.log("▶ SC001_TC_002 | reason=Transfer | child=" + childId_Transfer, true);

        String response = submitWithdrawRequest(childId_Transfer, futureDate(), "Transfer");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);
        verifyPendingStatus(childId_Transfer);

        Reporter.log("✅ PASSED — Withdraw Child submitted with reason=Transfer", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC003 — SC001_TC_003 : Submit Withdraw Child, reason = Not Satisfied with Services
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC001_TC_003 — Submit Withdraw Child, reason=Not Satisfied with Services")
    public void testWithdraw_NotSatisfied() throws InterruptedException {
        if (isSkipped(childId_NotSatisfied)) return;
        Reporter.log("▶ SC001_TC_003 | reason=Not Satisfied with Services | child=" + childId_NotSatisfied, true);

        String response = submitWithdrawRequest(childId_NotSatisfied, futureDate(), "Not Satisfied with Services");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);
        verifyPendingStatus(childId_NotSatisfied);

        Reporter.log("✅ PASSED — Withdraw Child submitted with reason=Not Satisfied with Services", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC004 — SC001_TC_004 : Submit Withdraw Child, reason = Moving to formal schooling
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC001_TC_004 — Submit Withdraw Child, reason=Moving to formal schooling")
    public void testWithdraw_FormalSchool() throws InterruptedException {
        if (isSkipped(childId_FormalSchool)) return;
        Reporter.log("▶ SC001_TC_004 | reason=Moving to formal schooling | child=" + childId_FormalSchool, true);

        String response = submitWithdrawRequest(childId_FormalSchool, futureDate(), "Moving to formal schooling");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);
        verifyPendingStatus(childId_FormalSchool);

        Reporter.log("✅ PASSED — Withdraw Child submitted with reason=Moving to formal schooling", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC005 — SC001_TC_005 : Submit Withdraw Child, reason = Others
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC001_TC_005 — Submit Withdraw Child, reason=Others")
    public void testWithdraw_Others() throws InterruptedException {
        if (isSkipped(childId_Others)) return;
        Reporter.log("▶ SC001_TC_005 | reason=Others | child=" + childId_Others, true);

        String response = submitWithdrawRequest(childId_Others, futureDate(), "Others");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);
        verifyPendingStatus(childId_Others);

        Reporter.log("✅ PASSED — Withdraw Child submitted with reason=Others", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC006 — SC001_TC_006 : Verify Pending status on Customer Request screen
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC001_TC_006 — Submit Withdraw Child, verify Pending status")
    public void tc006_pendingStatusVerify() throws InterruptedException {
        if (isSkipped(PENDING_STATUS_CHILD_ID)) return;
        Reporter.log("▶ TC006 SC001_TC_006 | child=" + PENDING_STATUS_CHILD_ID, true);

        String response = submitWithdrawRequest(PENDING_STATUS_CHILD_ID, futureDate(), "Not Satisfied with Services");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);

        verifyPendingStatus(PENDING_STATUS_CHILD_ID);

        Reporter.log("✅ TC006 PASSED — Withdraw Child Pending status verified", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC007 — SC002_TC_002 : Support approves the Withdraw Child request
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC002_TC_002 — Support approves Withdraw Child request")
    public void tc007_supportApprove() throws InterruptedException {
        if (isSkipped(APPROVE_CHILD_ID)) return;
        Reporter.log("▶ TC007 SC002_TC_002 | child=" + APPROVE_CHILD_ID, true);

        // WEF = today (not futureDate()) — processChildApprovedRequest only
        // processes as of the WEF date, so today is needed to observe the
        // terminal Approved state within this test run.
        String response = submitWithdrawRequest(APPROVE_CHILD_ID, LocalDate.now().toString(), "Transfer");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);

        Response pending = APIs.getWithdrawChildPendingRequests(APPROVE_CHILD_ID);
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());

        // Manual Approve on Customer Request screen (confirmed live: mandatory
        // before processChildApprovedRequest does anything — without it the
        // API returns HTTP 200/null, a silent no-op, not an error).
        switchToCustomerRequestTab();
        String requestId = recentRequestsPage.getFirstRetainAttritionRequestId(APPROVE_CHILD_ID);
        Assert.assertFalse(requestId.isEmpty(), "❌ No pending Child Attrition request found");
        recentRequestsPage.clickApproveAttrition(requestId);
        recentRequestsPage.submitApproveAttrition("Automated test approval");

        Response processResp = APIs.processWithdrawChildRequest(APPROVE_CHILD_ID);
        Assert.assertTrue(processResp.getStatusCode() >= 200 && processResp.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + processResp.getStatusCode());

        Thread.sleep(1000);
        switchToCustomerRequestTab();
        String afterStatus = recentRequestsPage.getWithdrawRequestStatus(APPROVE_CHILD_ID);
        System.out.println("   [After Approve] Request Status=" + afterStatus);
        Reporter.log("   After Approve: Request Status=" + afterStatus, true);
        Assert.assertEquals(afterStatus, "Approved", "❌ Request Status should be Approved");

        Reporter.log("✅ TC007 PASSED — Withdraw Child approved by Support", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC008 — SC002_TC_003 : Support rejects the Withdraw Child request
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC002_TC_003 — Support rejects Withdraw Child request")
    public void tc008_supportReject() throws InterruptedException {
        if (isSkipped(REJECT_CHILD_ID)) return;
        Reporter.log("▶ TC008 SC002_TC_003 | child=" + REJECT_CHILD_ID, true);

        String response = submitWithdrawRequest(REJECT_CHILD_ID, futureDate(), "Others");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);

        Response pending = APIs.getWithdrawChildPendingRequests(REJECT_CHILD_ID);
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());

        switchToCustomerRequestTab();
        // "Reject" for a Child Attrition row is the RETAIN button (confirmed
        // live) — no generic button.reject exists for this request type.
        String requestId = recentRequestsPage.getFirstRetainAttritionRequestId(REJECT_CHILD_ID);
        Assert.assertFalse(requestId.isEmpty(), "❌ No RETAIN button found for Withdraw Child request");
        recentRequestsPage.clickRetainAttrition(requestId);
        recentRequestsPage.acceptActionAlert();
        String rejectResponse = recentRequestsPage.getActionResponseMessage();
        System.out.println("   [Retain/Reject response] " + rejectResponse);
        Reporter.log("   Retain/Reject response: " + rejectResponse, true);

        Thread.sleep(1000);
        String afterStatus = recentRequestsPage.getWithdrawRequestStatus(REJECT_CHILD_ID);
        System.out.println("   [After Reject] Request Status=" + afterStatus);
        Reporter.log("   After Reject: Request Status=" + afterStatus, true);
        Assert.assertNotEquals(afterStatus, "Approved", "❌ Rejected request must not show as Approved");

        Reporter.log("✅ TC008 PASSED — Withdraw Child rejected by Support", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC009 — SC003_TC_001 : Retain admission from Customer Request screen
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC003_TC_001 — Retain admission (cancel pending attrition) from Customer Request screen")
    public void tc009_retainAdmission() throws InterruptedException {
        if (isSkipped(RETAIN_CHILD_ID)) return;
        Reporter.log("▶ TC009 SC003_TC_001 | child=" + RETAIN_CHILD_ID, true);

        String response = submitWithdrawRequest(RETAIN_CHILD_ID, futureDate(), "Transfer");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);

        Response pending = APIs.getWithdrawChildPendingRequests(RETAIN_CHILD_ID);
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());

        // RETAIN remains available up until processChildApprovedRequest actually
        // runs (confirmed by user) — acting here, right after getAllPendingRequests
        // and before ever calling the process API, is within that window.
        switchToCustomerRequestTab();
        String requestId = recentRequestsPage.getFirstRetainAttritionRequestId(RETAIN_CHILD_ID);
        Assert.assertFalse(requestId.isEmpty(), "❌ No RETAIN button found for Withdraw Child request");
        recentRequestsPage.clickRetainAttrition(requestId);
        recentRequestsPage.acceptActionAlert();
        String retainResponse = recentRequestsPage.getActionResponseMessage();
        System.out.println("   [Retain response] " + retainResponse);
        Reporter.log("   Retain response: " + retainResponse, true);

        Thread.sleep(1000);
        String afterStatus = recentRequestsPage.getWithdrawRequestStatus(RETAIN_CHILD_ID);
        System.out.println("   [After Retain] Request Status=" + afterStatus);
        Reporter.log("   After Retain: Request Status=" + afterStatus, true);
        Assert.assertEquals(afterStatus, "Cancelled", "❌ Request Status should be Cancelled after retaining admission");

        Reporter.log("✅ TC009 PASSED — Admission retained, attrition request cancelled", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC010 — Update Attrition Request : change WEF date + reason
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "Update Attrition Request — change WEF date + reason from Customer Request screen")
    public void tc010_updateAttritionRequest() throws InterruptedException {
        if (isSkipped(UPDATE_REQUEST_CHILD_ID)) return;
        Reporter.log("▶ TC010 | child=" + UPDATE_REQUEST_CHILD_ID, true);

        String response = submitWithdrawRequest(UPDATE_REQUEST_CHILD_ID, futureDate(), "Transfer");
        Assert.assertTrue(response.toLowerCase().contains("success"), "❌ Expected success toast. Got: " + response);

        Response pending = APIs.getWithdrawChildPendingRequests(UPDATE_REQUEST_CHILD_ID);
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());

        // UPDATE REQUEST remains available while Approval Status = Pending
        // (confirmed by user) — same window as RETAIN, before the process API runs.
        switchToCustomerRequestTab();
        String wefDateBefore = recentRequestsPage.getWithdrawColumnValue(UPDATE_REQUEST_CHILD_ID, "WEF Date");
        System.out.println("   [WEF Date before] " + wefDateBefore);
        Reporter.log("   WEF Date before: " + wefDateBefore, true);

        String requestId = recentRequestsPage.getFirstRetainAttritionRequestId(UPDATE_REQUEST_CHILD_ID);
        Assert.assertFalse(requestId.isEmpty(), "❌ No pending Child Attrition request found");
        String newWefDate = LocalDate.now().plusDays(45).toString();
        recentRequestsPage.clickUpdateRequest(requestId);
        String updateResponse = recentRequestsPage.submitUpdateRequest(newWefDate, "Automated test — WEF date update");
        System.out.println("   [Update Request response] " + updateResponse);
        Reporter.log("   Update Request response: " + updateResponse, true);

        // Give the backend time to finish processing, then hard-refresh (not
        // just re-navigate) in case the grid simply isn't re-rendering the
        // AJAX result rather than the record actually being gone.
        Thread.sleep(3000);
        switchToCustomerRequestTab();
        driver.navigate().refresh();
        Thread.sleep(2000);
        int rowCount = recentRequestsPage.getRowCount();
        System.out.println("   [Row count after update + refresh] " + rowCount);
        Reporter.log("   Row count after update + refresh: " + rowCount, true);
        Assert.assertTrue(rowCount > 0, "❌ Child Attrition row disappeared from the grid after Update Request (even after refresh). Response was: " + updateResponse);

        String wefDateAfter = recentRequestsPage.getWithdrawColumnValue(UPDATE_REQUEST_CHILD_ID, "WEF Date");
        System.out.println("   [WEF Date after] " + wefDateAfter);
        Reporter.log("   WEF Date after: " + wefDateAfter, true);
        Assert.assertFalse(wefDateAfter.isEmpty(), "❌ WEF Date is empty after Update Request");
        Assert.assertNotEquals(wefDateAfter, wefDateBefore, "❌ WEF Date should change after Update Request");

        Reporter.log("✅ TC010 PASSED — Attrition request WEF date updated", true);
    }
}
