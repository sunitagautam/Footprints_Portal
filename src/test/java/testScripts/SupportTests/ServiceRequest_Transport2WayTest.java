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
 * Transport Service Request — 2 Way — 3 Test Suite
 * Source: TC_Transport.xlsx (sheet TC_Transport).
 * <p>
 * 1  tc001_submitPending   SC005_TC_001   Submit Start Transport 2 Way (child 24309) → Pending.
 * 2  tc002_stopSubmitPending SC012_TC_001 Submit Stop Transport 2 Way (child 66914) → Pending.
 * 3  tc003_stopFullFlow    SC012_TC_002   Stop Transport 2 Way full flow: submit → getAllPendingRequests
 * → Approve → processChildApprovedRequest → Approved → addon removed.
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
}
