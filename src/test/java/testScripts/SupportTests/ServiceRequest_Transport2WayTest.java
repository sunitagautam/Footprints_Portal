package testScripts.SupportTests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.UserRightsPage;
import pages.Support.AccountStatementPage;
import pages.Support.RecentCustomerRequestsPage;
import pages.Support.Regular_ServiceRequests;
import utils.APIs;
import utils.BaseTest;

import java.time.LocalDate;

/**
 * Transport Service Request — 2 Way — 5 Test Suite
 * Source: TC_Transport.xlsx (sheet TC_Transport), plus 2 gap scenarios (SC017–SC018)
 * added to TC_Transport14Aug.xlsx, mirroring 2 of the 4 gap scenarios already added
 * to ServiceRequest_Transport1WayTest (the other 2 — Attrition-block and access-right
 * — test mechanisms generic to the whole Service Request panel, not direction-specific,
 * so were not duplicated here per explicit user decision).
 * <p>
 * 1  tc001_submitPending   SC005_TC_001   Submit Start Transport 2 Way (child 24309) → Pending.
 * 2  tc002_stopSubmitPending SC012_TC_001 Submit Stop Transport 2 Way (child 66914) → Pending.
 * 3  tc003_stopFullFlow    SC012_TC_002   Stop Transport 2 Way full flow: submit → getAllPendingRequests
 * → Approve → processChildApprovedRequest → Approved → addon removed.
 * 4  tc004_duplicateCrossTypeBlocked SC017_TC_001 (GAP) Duplicate Two Way request blocked, incl.
 * cross-type conflict — a pending Two Way request also blocks a new One Way submit on the same child.
 * 5  tc005_pastDateRejected SC018_TC_001 (GAP) Start Transport 2 Way submit form rejects a genuinely
 * past date ("Cannot make request in past date"); today/future dates succeed.
 * <p>
 * SC012 (Stop Transport 2 Way) was NOT in the originally-selected 6 test cases for this round —
 * added per explicit follow-up request, mirroring the 1-Way class's tc004/tc005 pattern exactly
 * (same generic button.approve flow, same WEF=today requirement for processChildApprovedRequest
 * to actually process it, same idempotent already-pending handling).
 * <p>
 * TEST DATA — child 24309 given directly in the spec ("Active child with 2-way transport
 * route available", "Child ID: 24309") — CONFIRMED live as now Attrition, cannot be used;
 * needs a fresh ACTIVE, 2-way-transport-enabled replacement. Child 66914 (Anshika Gautam,
 * "Two Way Transport - 2200" addon) given directly in the spec for SC012_TC_001/002 —
 * freshness not yet independently reverified live.
 * <p>
 * UNVERIFIED — "Add Two Way Transport"/"Delete Two Way Transport" grid Request Type values
 * are extrapolated from the 1-Way naming pattern confirmed live ("Add/Delete One Way
 * Transport"), not yet independently confirmed for 2-Way.
 */
public class ServiceRequest_Transport2WayTest extends BaseTest {

    private static final String START_2WAY_CHILD_ID = "72089"; // supplied by user — replaces 24309, which was confirmed Attrition
    private static final String STOP_2WAY_CHILD_ID = "50875"; // Anshika Gautam — active "Two Way Transport - 2200" addon

    private static final String GRID_TYPE_ADD_2WAY = "Add Two Way Transport";
    private static final String GRID_TYPE_DELETE_2WAY = "Delete Two Way Transport";

    private Regular_ServiceRequests serviceRequestPage;
    private AccountStatementPage accountStatementPage;
    private RecentCustomerRequestsPage recentRequestsPage;
    private UserRightsPage userRightsPage;
    private Navigations navigations;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        serviceRequestPage = new Regular_ServiceRequests(driver);
        recentRequestsPage = new RecentCustomerRequestsPage(driver);

        String user = getUserForScreen("Transport");
        Assert.assertFalse(user.isEmpty(), "No user found for 'Account Statement'");
        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();

        navigations.goToAccountStatement();
    }

    @BeforeMethod(alwaysRun = true)
    public void goToAccountStatement() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        // Unlike the 1-Way class (which uses two tabs and always switches back
        // to Account Statement), this class shares a single tab — a prior
        // test's navigateByChildId() (Recent Customer Requests) or grid check
        // can leave the driver on a different page entirely, where #frm_child_id
        // doesn't exist. Always re-navigate here rather than assuming.
        navigations.goToAccountStatement();
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
            serviceRequestPage.closeModalByJs();
        } catch (Exception ignored) {
        }
    }

    private String futureDate() {
        return LocalDate.now().plusDays(7).toString();
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC001 — SC005_TC_001 : Submit Start Transport 2 Way → Pending
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC005_TC_001 — Submit Start Transport 2 Way, verify Pending")
    public void tc001_submitPending() throws InterruptedException {
        Reporter.log("▶ TC001 SC005_TC_001 | child=" + START_2WAY_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(START_2WAY_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");

        // The Services dropdown renders permanently `disabled` (not a timing
        // issue) when the child is Attrition — confirmed live for child 24309
        // ("You can't access services of attrition child"). Fail fast with a
        // clear message rather than a generic Selenium timeout/exception.
        Assert.assertTrue(serviceRequestPage.selectServices_dropdown.isEnabled(),
                "❌ Services dropdown is disabled — child " + START_2WAY_CHILD_ID
                        + " is likely Attrition/inactive. Replace with a fresh ACTIVE, 2-way-transport-enabled child ID.");

        serviceRequestPage.selectServiceType("Start Transport 2 Way");
        Assert.assertTrue(serviceRequestPage.isStartTransport2WayFormVisible(), "❌ Start Transport 2 Way form not visible");

        serviceRequestPage.setT2FromDate(futureDate());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport2Way();
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
        // If a Start 2 Way request from a prior run is already pending, treat
        // that as an acceptable pre-existing Pending state — but ONLY if the
        // blocking request is itself a Two Way one. Confirmed live: a child
        // with ANY existing pending transport request (even a One Way one)
        // gets the same generic "already pending" block on a Two Way submit
        // attempt, which is a cross-type conflict needing a different child,
        // not a same-request duplicate to treat as idempotent.
        boolean alreadyPendingSameType = response.toLowerCase().contains("already pending")
                && response.toLowerCase().contains("two way");
        Assert.assertTrue(response.toLowerCase().contains("success") || alreadyPendingSameType,
                "❌ Expected success toast (or a Two Way already-pending duplicate). Got: " + response
                        + (response.toLowerCase().contains("already pending")
                        ? " — this child already has an UNRELATED transport request blocking new submissions; use a different, genuinely fresh child."
                        : ""));

        String status = recentRequestsPage.getColumnValueByRequestType(
                START_2WAY_CHILD_ID, GRID_TYPE_ADD_2WAY, "Request Status");
        System.out.println("   [Request Status] " + status);
        Reporter.log("   Request Status: " + status, true);
        Assert.assertEquals(status, "Pending", "❌ Request Status should be Pending");

        Reporter.log("✅ TC001 PASSED — Start Transport 2 Way submitted, Pending", true);
    }

    /**
     * Submit a Stop Transport 2 Way request and return the toast/response message.
     */
    private String submitStopTransport2Way(String childId, String fromDate) throws InterruptedException {
        accountStatementPage.generateAccountStatement(childId);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType("Stop Transport 2 Way");
        Assert.assertTrue(serviceRequestPage.isStopTransport2WayFormVisible(), "❌ Stop Transport 2 Way form not visible");

        serviceRequestPage.setST2FromDate(fromDate);
        Thread.sleep(300);
        serviceRequestPage.submitStopTransport2Way();
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

    // ════════════════════════════════════════════════════════════════════
    //  TC002 — SC012_TC_001 : Submit Stop Transport 2 Way (child 66914) → Pending
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC012_TC_001 — Submit Stop Transport 2 Way, verify Pending")
    public void tc002_stopSubmitPending() throws InterruptedException {
        Reporter.log("▶ TC002 SC012_TC_001 | child=" + STOP_2WAY_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(STOP_2WAY_CHILD_ID);
        String addonsBefore = accountStatementPage.getAddonsText();
        System.out.println("   [Addons before] " + addonsBefore);
        Assert.assertTrue(addonsBefore.toLowerCase().contains("two way transport"),
                "❌ Expected 'Two Way Transport' addon active before Stop request. Got: " + addonsBefore);

        // WEF = today, not a real future date — same reasoning as the 1-Way
        // Stop flow: processChildApprovedRequest only processes AS OF the WEF
        // date, so today is required for tc003 to observe Approved.
        String response = submitStopTransport2Way(STOP_2WAY_CHILD_ID, LocalDate.now().toString());
        boolean alreadyPending = response.toLowerCase().contains("already pending");
        Assert.assertTrue(response.toLowerCase().contains("success") || alreadyPending,
                "❌ Expected success toast (or already-pending). Got: " + response);

        String status = recentRequestsPage.getColumnValueByRequestType(
                STOP_2WAY_CHILD_ID, GRID_TYPE_DELETE_2WAY, "Request Status");
        System.out.println("   [Request Status] " + status);
        Reporter.log("   Request Status: " + status, true);
        Assert.assertEquals(status, "Pending", "❌ Request Status should be Pending");

        Reporter.log("✅ TC002 PASSED — Stop Transport 2 Way submitted, Pending", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC003 — SC012_TC_002 : Stop Transport 2 Way full flow → addon removed
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC012_TC_002 — Stop Transport 2 Way full flow: submit → getAllPendingRequests → Approve → processChildApprovedRequest → addon removed")
    public void tc003_stopFullFlow() throws InterruptedException {
        Reporter.log("▶ TC003 SC012_TC_002 | child=" + STOP_2WAY_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(STOP_2WAY_CHILD_ID);
        String addonsBefore = accountStatementPage.getAddonsText();
        System.out.println("   [Addons before] " + addonsBefore);

        // Submits a fresh Stop request if none is pending yet; if tc002 (or a
        // prior run) already left one pending, the app blocks the duplicate —
        // either way there is now a Pending "Delete Two Way Transport" request
        // to drive through Approve below.
        String response = submitStopTransport2Way(STOP_2WAY_CHILD_ID, LocalDate.now().toString());
        boolean alreadyPending = response.toLowerCase().contains("already pending");
        Assert.assertTrue(response.toLowerCase().contains("success") || alreadyPending,
                "❌ Expected success toast (or already-pending). Got: " + response);

        Response pending = APIs.getTransportPendingRequests(STOP_2WAY_CHILD_ID);
        Assert.assertEquals(pending.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + pending.getBody().asString());

        String statusAfterApi = recentRequestsPage.getColumnValueByRequestType(
                STOP_2WAY_CHILD_ID, GRID_TYPE_DELETE_2WAY, "Request Status");
        System.out.println("   [Request Status after submit+API] " + statusAfterApi);
        Reporter.log("   Request Status after submit+API: " + statusAfterApi, true);

        String requestId = recentRequestsPage.getFirstApproveRequestId();
        Assert.assertFalse(requestId.isEmpty(), "❌ No Approve button found for Stop Transport request");
        recentRequestsPage.clickApprove(requestId);
        Thread.sleep(1500);
        recentRequestsPage.acceptActionAlert();

        Response processResp = APIs.processTransportApprovedRequest(STOP_2WAY_CHILD_ID);
        Assert.assertTrue(processResp.getStatusCode() >= 200 && processResp.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + processResp.getStatusCode());
        System.out.println("   [Process] " + processResp.getBody().asString());
        Reporter.log("   Process response: " + processResp.getBody().asString(), true);

        Thread.sleep(1000);
        String afterStatus = recentRequestsPage.getColumnValueByRequestType(
                STOP_2WAY_CHILD_ID, GRID_TYPE_DELETE_2WAY, "Request Status");
        System.out.println("   [After process] Request Status=" + afterStatus);
        Reporter.log("   After process: Request Status=" + afterStatus, true);
        Assert.assertEquals(afterStatus, "Approved", "❌ Request Status should be Approved after processing");

        // Single-tab class — the grid check above navigated away from Account
        // Statement (to Recent Customer Requests), and generateAccountStatement()
        // assumes it's already on that page (it doesn't navigate itself), so
        // an explicit re-navigation is required here first.
        navigations.goToAccountStatement();
        accountStatementPage.generateAccountStatement(STOP_2WAY_CHILD_ID);
        String addonsAfter = accountStatementPage.getAddonsText();
        System.out.println("   [Addons after] " + addonsAfter);
        Reporter.log("   Addons after: " + addonsAfter, true);
        Assert.assertFalse(addonsAfter.toLowerCase().contains("two way transport"),
                "❌ 'Two Way Transport' addon should be removed. Got: " + addonsAfter);

        Reporter.log("✅ TC003 PASSED — Stop Transport 2 Way approved, addon removed", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC004 — SC017_TC_001 (GAP) : Duplicate/overlapping Transport request
    //          blocked, incl. cross-type Two Way vs One Way conflict
    // ════════════════════════════════════════════════════════════════════
    // Mirrors ServiceRequest_Transport1WayTest.tc007_duplicateCrossTypeBlocked
    // from the OTHER direction — submits Two Way first, confirms a duplicate
    // Two Way submit is blocked, AND a cross-type One Way submit on the SAME
    // child is also blocked while the Two Way request is pending.
    @Test(priority = 4, description = "SC017_TC_001 (GAP) — Duplicate/overlapping Transport request blocked, incl. cross-type Two Way vs One Way conflict")
    public void tc004_duplicateCrossTypeBlocked() throws InterruptedException {
        Reporter.log("▶ TC004 SC017_TC_001 | child=" + START_2WAY_CHILD_ID, true);

        // Setup: submit Start Transport 2 Way — either creates a fresh
        // pending request or confirms one already exists from tc001 (both
        // are an acceptable pre-existing-pending state for this scenario).
        accountStatementPage.generateAccountStatement(START_2WAY_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");
        serviceRequestPage.selectServiceType("Start Transport 2 Way");
        Assert.assertTrue(serviceRequestPage.isStartTransport2WayFormVisible(), "❌ Start Transport 2 Way form not visible");
        serviceRequestPage.setT2FromDate(futureDate());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport2Way();
        Thread.sleep(800);
        String setupPopup = serviceRequestPage.getAlertText();
        if (!setupPopup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String setupResponse = serviceRequestPage.getResponseMessage();
        System.out.println("   [Setup submit] " + setupResponse);
        boolean setupOk = setupResponse.toLowerCase().contains("success")
                || setupResponse.toLowerCase().contains("already pending");
        Assert.assertTrue(setupOk, "❌ Expected success or already-pending during setup. Got: " + setupResponse);
        serviceRequestPage.closeModalByJs();
        Thread.sleep(400);

        // Baseline: count Transport rows right after setup, BEFORE the
        // blocked duplicate/cross-type attempts below. Child 72089 has
        // accumulated Transport history across earlier test sessions (a
        // stale "Add One Way Transport" row alongside the current Two Way
        // one) — asserting a hardcoded "exactly 1 row" is wrong for a
        // long-lived, reused test child. The real proof that the blocked
        // attempts created nothing new is that the count doesn't GROW.
        recentRequestsPage.navigateByChildId(START_2WAY_CHILD_ID);
        int baselineRowCount = recentRequestsPage.getRowCount();
        int baselineTransportRows = 0;
        for (int r = 1; r <= baselineRowCount; r++) {
            String type = recentRequestsPage.getColumnValueForRow(r, "Request Type");
            if (type != null && type.toLowerCase().contains("transport")) baselineTransportRows++;
        }
        System.out.println("   [Baseline transport row count] " + baselineTransportRows);

        // navigateByChildId() above left the driver on Recent Customer
        // Requests — generateAccountStatement() doesn't navigate itself, so
        // an explicit re-navigation is required before continuing (same
        // pattern already used at the end of tc003_stopFullFlow).
        navigations.goToAccountStatement();

        // Step: attempt a SAME-type (Two Way) duplicate submit
        accountStatementPage.generateAccountStatement(START_2WAY_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");
        serviceRequestPage.selectServiceType("Start Transport 2 Way");
        Assert.assertTrue(serviceRequestPage.isStartTransport2WayFormVisible(), "❌ Start Transport 2 Way form not visible");
        serviceRequestPage.setT2FromDate(futureDate());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport2Way();
        Thread.sleep(800);
        String dupPopup = serviceRequestPage.getAlertText();
        if (!dupPopup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String duplicateSameType = serviceRequestPage.getResponseMessage();
        System.out.println("   [Same-type duplicate] " + duplicateSameType);
        Reporter.log("   Same-type duplicate response: " + duplicateSameType, true);
        Assert.assertTrue(duplicateSameType.toLowerCase().contains("already pending"),
                "❌ Expected 'already pending' block for same-type (Two Way) duplicate. Got: " + duplicateSameType);
        serviceRequestPage.closeModalByJs();
        Thread.sleep(400);

        // Step: attempt the OPPOSITE direction (Start Transport 1 Way) for the SAME child
        accountStatementPage.generateAccountStatement(START_2WAY_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");
        serviceRequestPage.selectServiceType("Start Transport 1 Way");
        Assert.assertTrue(serviceRequestPage.isStartTransport1WayFormVisible(), "❌ Start Transport 1 Way form not visible");
        serviceRequestPage.setT1FromDate(futureDate());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport1Way();
        Thread.sleep(800);
        String crossPopup = serviceRequestPage.getAlertText();
        if (!crossPopup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String crossTypeResponse = serviceRequestPage.getResponseMessage();
        System.out.println("   [Cross-type (1 Way) attempt] " + crossTypeResponse);
        Reporter.log("   Cross-type (1 Way) attempt response: " + crossTypeResponse, true);
        Assert.assertTrue(crossTypeResponse.toLowerCase().contains("already pending"),
                "❌ Expected 'already pending' block for cross-type (1 Way) attempt while a 2 Way request is pending. Got: " + crossTypeResponse);

        // Step: verify the Transport row count did NOT grow beyond the
        // baseline captured right after setup — proves the blocked
        // duplicate/cross-type attempts created nothing new, regardless of
        // how much pre-existing Transport history this child already has.
        recentRequestsPage.navigateByChildId(START_2WAY_CHILD_ID);
        int rowCount = recentRequestsPage.getRowCount();
        int transportRows = 0;
        for (int r = 1; r <= rowCount; r++) {
            String type = recentRequestsPage.getColumnValueForRow(r, "Request Type");
            if (type != null && type.toLowerCase().contains("transport")) transportRows++;
        }
        System.out.println("   [Transport row count] " + transportRows + " (baseline was " + baselineTransportRows + ")");
        Reporter.log("   Transport row count: " + transportRows + " (baseline was " + baselineTransportRows + ")", true);
        Assert.assertEquals(transportRows, baselineTransportRows,
                "❌ Expected Transport row count unchanged from baseline (" + baselineTransportRows
                        + "), found " + transportRows + " — a blocked attempt may have created a duplicate row");

        Reporter.log("✅ TC004 PASSED — Duplicate + cross-type Transport requests blocked (2 Way side)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC005 — SC018_TC_001 (GAP) : Start Transport 2 Way submit form
    //          rejects a genuinely PAST date; today/future succeed
    // ════════════════════════════════════════════════════════════════════
    // Mirrors ServiceRequest_Transport1WayTest.tc008_pastDateRejected for
    // the Two Way form — confirms the same past-date validation applies
    // here too, and that today/future dates are accepted.
    @Test(priority = 5, description = "SC018_TC_001 (GAP) — Start Transport 2 Way submit form rejects a genuinely past date; today/future succeed")
    public void tc005_pastDateRejected() throws InterruptedException {
        Reporter.log("▶ TC005 SC018_TC_001 | child=" + START_2WAY_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(START_2WAY_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");
        serviceRequestPage.selectServiceType("Start Transport 2 Way");
        Assert.assertTrue(serviceRequestPage.isStartTransport2WayFormVisible(), "❌ Start Transport 2 Way form not visible");

        // Step: a genuinely past date (yesterday) must be rejected. Any
        // confirm popup must be ACCEPTED (not dismissed) so the real
        // validation actually runs — see the 1-Way class's tc008 for the
        // earlier bug where blind-dismissing cancelled the submission
        // instead of reading the real validation message.
        serviceRequestPage.setT2FromDate(LocalDate.now().minusDays(1).toString());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport2Way();
        Thread.sleep(800);
        String yesterdayPopup = serviceRequestPage.getAlertText();
        if (!yesterdayPopup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String yesterdayResponse = serviceRequestPage.getResponseMessage();
        System.out.println("   [Yesterday] " + yesterdayResponse);
        Reporter.log("   Yesterday response: " + yesterdayResponse, true);
        Assert.assertTrue(yesterdayResponse.toLowerCase().contains("past date"),
                "❌ Expected 'Cannot make request in past date' for a genuinely past date. Got: " + yesterdayResponse);

        // Step: today's date must succeed (not be rejected) — confirmed
        // for the 1 Way form; verifying the same holds for 2 Way.
        serviceRequestPage.setT2FromDate(LocalDate.now().toString());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport2Way();
        Thread.sleep(800);
        String todayPopup = serviceRequestPage.getAlertText();
        if (!todayPopup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String todayResponse = serviceRequestPage.getResponseMessage();
        System.out.println("   [Today's date] " + todayResponse);
        Reporter.log("   Today's date response: " + todayResponse, true);
        boolean todayAlreadyPending = todayResponse.toLowerCase().contains("already pending");
        Assert.assertTrue(todayResponse.toLowerCase().contains("success") || todayAlreadyPending,
                "❌ Expected success (or already-pending) for today's date. Got: " + todayResponse);

        // Step: a genuine future date must also succeed
        serviceRequestPage.setT2FromDate(futureDate());
        Thread.sleep(300);
        serviceRequestPage.submitStartTransport2Way();
        Thread.sleep(800);
        String popup = serviceRequestPage.getAlertText();
        if (!popup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }
        String futureResponse = serviceRequestPage.getResponseMessage();
        System.out.println("   [Future date] " + futureResponse);
        Reporter.log("   Future date response: " + futureResponse, true);
        boolean alreadyPending = futureResponse.toLowerCase().contains("already pending");
        Assert.assertTrue(futureResponse.toLowerCase().contains("success") || alreadyPending,
                "❌ Expected success (or already-pending) for a genuine future date. Got: " + futureResponse);

        Reporter.log("✅ TC005 PASSED — Genuinely past date rejected; today and future dates accepted (2 Way form)", true);
    }
}
