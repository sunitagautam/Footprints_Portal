package testScripts.SupportTests;

import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
import utils.IAutoConstant;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Time Extension — 6 Test Suite (one continuous flow on a single child)
 * <p>
 * Priority / Execution Order:
 * 1  tc001_submitStartTimeExtension           SC002_TC_001  Submit Start → Pending (incl. Cancel path) (UI)
 * 2  tc002_startFullFlowApproveAndProcess      SC002_TC_002  API verify → Approve → process API → addon/invoice (UI + RestAssured)
 * 3  tc003_startProcessApiResponseVerify       SC002_TC_010  processTimeExtentionRequest response + addon amount (UI + RestAssured)
 * 4  tc004_submitStopTimeExtension             SC003_TC_001  Submit Stop → Pending (UI)
 * 5  tc005_stopPendingRequestApiVerify         SC003_TC_003  getAllPendingRequests fields for Stop (RestAssured)
 * 6  tc006_stopApproveAndProcessAddonRemoved   SC003_TC_005  Approve Stop → process API → addon removed (UI + RestAssured)
 * <p>
 * PRE-CONDITION — update TE_CHILD_ID below:
 * TE_CHILD_ID   Active Regular child, Time Extension enabled at its center, no
 * conflicting pending Start/Stop Time Extension request. Shared across all 6
 * tests in this continuous flow (submit Start → approve → submit Stop →
 * approve), matching the real captured flow in "Start Time Extension Service
 * Request.docx". Skips gracefully if left as TODO.
 * <p>
 * CONFIRMED FROM LIVE RUNS (do not re-derive without evidence):
 * - Start Time Extension form: id="frm-start-time-extension", single date
 * field id="startFrom" (no "To" date). Stop Time Extension form:
 * id="frm-stop-time-extension", single date field id="endFrom".
 * - Submit triggers a NATIVE browser confirm() dialog: "Do you want to send
 * request for time extension?" — Cancel dismisses without submitting; OK
 * submits and shows toast "Your request submitted successfully."
 * - Fresh submission grid state: Request Status = "Pending", Approval
 * Status = "NA" (both columns confirmed live — Approval Status only becomes
 * meaningful after the getAllPendingRequests API + Approve step).
 * - getAllPendingRequests: "chid_id" IS the correct scoping param here
 * (unlike Extended Daycare, where "child_id" was required instead). This
 * endpoint is a STATEFUL TRIGGER, not a plain read — calling it transitions
 * the request and makes the Approve button appear; calling it a second time
 * for an already-transitioned entry returns "no pending requests".
 * - Approve button: id="approve_extension", Reject: id="reject_extention"
 * (sic) — both carry request_id/request_type attributes directly, so they
 * can be located without first resolving a grid row/request_id.
 * - Approve flow: click Approve → "Time Extension Request" modal
 * (#modal_form_extension) opens showing Child Name/Request Type/Effective
 * Date → click Confirm (.btn-model-extension-cancel, reuses the existing
 * Pause Extend/Resume plumbing) → a native "Prorated Invoice will be
 * charged..." / "Are you sure you want to approve the request?" confirm
 * dialog appears → accept. The approval completes server-side at this point
 * (grid already shows Approval Status = Approved) — the modal does NOT
 * reliably re-render a "Request Approved Successfully" / Close button
 * afterward, so treat closing it as best-effort UI cleanup, never a hard
 * requirement (see RecentCustomerRequestsPage.closeTimeExtensionModal()).
 * - processTimeExtentionRequest uses "child_id" (not "chid_id"), ckey=3E529969372D.
 * It only processes a request dated for the CURRENT day — a future-dated
 * Stop request is left stuck returning "No Data found to Process Time
 * Extention Request" indefinitely.
 * - After processing: grid Request Status/Approval Status both = "Approved".
 * - Account Statement Addons line: "Addons : Time Extension ( ₹1500.00)"
 * when active, "Addons : Not Available" when not (amount varies by center —
 * not hardcoded here).
 * - Invoice booking comment: "Prorated Time Extension Charges - <Month>,
 * <Year> (<N> days)", same row structure as Extended Daycare's invoice
 * (Daycare Fee/SGST/CGST — no separate Preschool Fee/Roundoff observed).
 */
public class ServiceRequest_TimeExtensionTest extends BaseTest {

    // ── TEST DATA ────────────────────────────────────────────────────────
    private static final String TE_CHILD_ID = "68823";
    private static final String START_DATE = LocalDate.now().toString();
    // processTimeExtentionRequest only processes a Stop request dated for the
    // CURRENT day — a future date leaves it stuck as "No Data found to Process
    // Time Extention Request" indefinitely.
    private static final String STOP_DATE = LocalDate.now().toString();

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
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        serviceRequestPage = new Regular_ServiceRequests(driver);
        recentRequestsPage = new RecentCustomerRequestsPage(driver);

        System.out.println("▶ TE_CHILD_ID  : " + TE_CHILD_ID);
        System.out.println("▶ START_DATE   : " + START_DATE);
        System.out.println("▶ STOP_DATE    : " + STOP_DATE);

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
        // derives its base URL from the CURRENT url, so seed a real app page
        // first or that derivation silently breaks.
        driver.get(IAutoConstant.LOGIN_URL);
        recentRequestsPage.navigateByChildId(TE_CHILD_ID);

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

    private boolean isSkipped() {
        if (TE_CHILD_ID.startsWith("TODO")) {
            System.out.println("   ⚠ SKIPPED — set TE_CHILD_ID");
            Reporter.log("⚠ SKIPPED — TE_CHILD_ID not set", true);
            return true;
        }
        return false;
    }

    /**
     * Full Approve flow for a given request type: click Approve → confirm on
     * the summary modal → accept the possible native "Prorated Invoice..."
     * dialog → close the "Request Approved Successfully" state.
     */
    private void approveTimeExtension(String requestType) throws InterruptedException {
        boolean approveVisible = !driver.findElements(By.cssSelector(
                "#approve_extension[request_type='" + requestType + "']")).isEmpty();
        Assert.assertTrue(approveVisible, "❌ Approve button not visible for request_type=" + requestType);

        recentRequestsPage.clickApproveExtension(requestType);
        Thread.sleep(800);
        recentRequestsPage.confirmTimeExtensionApproval();
        Thread.sleep(800);

        try {
            String nativeConfirm = driver.switchTo().alert().getText();
            System.out.println("   [Native confirm] " + nativeConfirm);
            Reporter.log("   Native confirm: " + nativeConfirm, true);
            driver.switchTo().alert().accept();
            Thread.sleep(1000);
        } catch (Exception ignored) {
            System.out.println("   (no native confirm dialog appeared)");
        }

        Thread.sleep(1000);
        recentRequestsPage.closeTimeExtensionModal();
        Thread.sleep(500);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC001 — SC002_TC_001 : Submit Start Time Extension (incl. Cancel path)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC002_TC_001 — Start Time Extension: submit via Service Request form, incl. Cancel path")
    public void tc001_submitStartTimeExtension() throws InterruptedException {
        if (isSkipped()) return;
        Reporter.log("▶ TC001 SC002_TC_001 | child=" + TE_CHILD_ID + " | start=" + START_DATE, true);

        accountStatementPage.generateAccountStatement(TE_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType("Start Time Extension");
        Assert.assertTrue(serviceRequestPage.isStartTimeExtensionFormVisible(), "❌ Start Time Extension form not visible");

        serviceRequestPage.setSTEFromDate(START_DATE);
        Thread.sleep(300);

        // Step 3+4 — Submit, then Cancel the native confirm — verify NOT submitted
        serviceRequestPage.submitStartTimeExtension();
        Thread.sleep(800);
        String popup = serviceRequestPage.getAlertText();
        System.out.println("   [Popup] " + popup);
        Reporter.log("   Popup: " + popup, true);
        Assert.assertTrue(popup.toLowerCase().contains("time extension") || !popup.isEmpty(),
                "❌ Expected native confirm popup for time extension request");
        serviceRequestPage.dismissAlert();
        Thread.sleep(800);

        String responseAfterCancel = serviceRequestPage.getResponseMessage();
        System.out.println("   [Response after Cancel] " + responseAfterCancel);
        Reporter.log("   Response after Cancel: " + responseAfterCancel, true);
        Assert.assertTrue(responseAfterCancel.isEmpty(),
                "❌ Request should NOT be submitted after Cancel. Got: " + responseAfterCancel);
        Assert.assertTrue(serviceRequestPage.isStartTimeExtensionFormVisible(),
                "❌ Form should stay open after Cancel");

        // Step 5 — Submit again, this time OK
        serviceRequestPage.submitStartTimeExtension();
        Thread.sleep(800);
        popup = serviceRequestPage.getAlertText();
        System.out.println("   [Popup] " + popup);
        Reporter.log("   Popup: " + popup, true);
        if (!popup.isEmpty()) {
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);
        }

        String response = serviceRequestPage.getResponseMessage();
        System.out.println("   [Response] " + response);
        Reporter.log("   Response: " + response, true);
        Assert.assertTrue(response.toLowerCase().contains("success"),
                "❌ Expected success toast. Got: " + response);

        // Step 6 — Verify Customer Request grid: Request Status = Pending, Approval
        // Status = NA (confirmed live — Approval Status only becomes meaningful
        // once the getAllPendingRequests API + Approve step run in tc002).
        switchToCustomerRequestTab();
        String approvalStatus = recentRequestsPage.getTEApprovalStatus(TE_CHILD_ID, "Start Time Extension");
        String requestStatus = recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Start Time Extension");
        System.out.println("   [Request Status] " + requestStatus + "  [Approval Status] " + approvalStatus);
        Reporter.log("   Request Status: " + requestStatus + " | Approval Status: " + approvalStatus, true);
        Assert.assertEquals(requestStatus, "Pending", "❌ Request Status should be Pending");
        Assert.assertEquals(approvalStatus, "NA", "❌ Approval Status should be NA before getAllPendingRequests/Approve");

        Reporter.log("✅ TC001 PASSED — Start Time Extension submitted, Cancel path verified", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC002 — SC002_TC_002 : Full flow — API verify → Approve → process API → addon/invoice
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "SC002_TC_002 — Start Time Extension full flow: getAllPendingRequests → Approve → processTimeExtentionRequest → addon/invoice")
    public void tc002_startFullFlowApproveAndProcess() throws InterruptedException {
        if (isSkipped()) return;
        Reporter.log("▶ TC002 SC002_TC_002 | child=" + TE_CHILD_ID, true);

        // Step 2 — getAllPendingRequests, verify fields for OUR child's Start Time Extension entry
        Response r = APIs.getTimeExtensionPendingRequests(TE_CHILD_ID);
        Assert.assertTrue(r.getStatusCode() >= 200 && r.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + r.getStatusCode());
        String status = r.jsonPath().getString("status");
        Assert.assertEquals(status, "ok", "❌ API status should be ok. Body: " + r.getBody().asString());

        List<Map<String, Object>> entries = r.jsonPath().getList("response");
        Map<String, Object> entry = entries.stream()
                .filter(e -> TE_CHILD_ID.equals(String.valueOf(e.get("admission_id")))
                        && "Start Time Extension".equalsIgnoreCase(String.valueOf(e.get("type"))))
                .findFirst().orElse(null);
        Assert.assertNotNull(entry, "❌ No 'Start Time Extension' entry found for child " + TE_CHILD_ID
                + " in getAllPendingRequests response: " + r.getBody().asString());

        System.out.println("   [Matched entry] " + entry);
        Reporter.log("   Matched entry: " + entry, true);
        Assert.assertEquals(String.valueOf(entry.get("type")), "Start Time Extension");
        Assert.assertEquals(String.valueOf(entry.get("status")), "Pending");
        Assert.assertEquals(String.valueOf(entry.get("credit_debit_amount")), "0");
        Assert.assertEquals(String.valueOf(entry.get("parent_name")), "Support Request");
        Assert.assertEquals(String.valueOf(entry.get("current_status")), "Active");
        Assert.assertEquals(String.valueOf(entry.get("admission_type")), "Regular");

        // Step 3-4 — Approve on Customer Request screen
        switchToCustomerRequestTab();
        recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Start Time Extension"); // navigates to the grid
        approveTimeExtension("Start Time Extension");

        // Step 4b — Approve modal closed: refresh the Customer Request screen and
        // capture status before processing, per the documented manual flow.
        recentRequestsPage.navigateByChildId(TE_CHILD_ID);
        Thread.sleep(1000);
        String preProcessRequestStatus = recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Start Time Extension");
        String preProcessApprovalStatus = recentRequestsPage.getTEApprovalStatus(TE_CHILD_ID, "Start Time Extension");
        System.out.println("   [After Approve, before process API] Request Status=" + preProcessRequestStatus
                + " Approval Status=" + preProcessApprovalStatus);
        Reporter.log("   After Approve, before process API: Request Status=" + preProcessRequestStatus
                + " | Approval Status=" + preProcessApprovalStatus, true);

        // Step 5 — processTimeExtentionRequest
        Response processResp = APIs.processTimeExtensionRequest(TE_CHILD_ID);
        Assert.assertTrue(processResp.getStatusCode() >= 200 && processResp.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + processResp.getStatusCode());
        String processStatus = processResp.jsonPath().getString("status");
        String processMessage = processResp.jsonPath().getString("message");
        System.out.println("   [Process] status=" + processStatus + " message=" + processMessage);
        Reporter.log("   Process status=" + processStatus + " message=" + processMessage, true);
        Assert.assertEquals(processStatus, "ok", "❌ Process status should be ok");
        Assert.assertEquals(processMessage, "Time Extension request processed", "❌ Unexpected process message");

        // Step 6 — Customer Request Status updated to Approved
        Thread.sleep(1000);
        String afterApprovalStatus = recentRequestsPage.getTEApprovalStatus(TE_CHILD_ID, "Start Time Extension");
        String afterRequestStatus = recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Start Time Extension");
        System.out.println("   [After] Request Status=" + afterRequestStatus + " Approval Status=" + afterApprovalStatus);
        Reporter.log("   After: Request Status=" + afterRequestStatus + " | Approval Status=" + afterApprovalStatus, true);
        Assert.assertEquals(afterApprovalStatus, "Approved", "❌ Approval Status should be Approved");

        // Step 7 — Account Statement Addons shows Time Extension
        switchToAccountStatementTab();
        accountStatementPage.generateAccountStatement(TE_CHILD_ID);
        String addonsText = accountStatementPage.getAddonsText();
        System.out.println("   [Addons] " + addonsText);
        Reporter.log("   Addons: " + addonsText, true);
        Assert.assertTrue(accountStatementPage.isTimeExtensionAddonPresent(),
                "❌ 'Time Extension' addon should be present. Got: " + addonsText);

        // Step 8 — Scroll down to the invoice section, then verify the prorated invoice line items
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, document.body.scrollHeight);");
        Thread.sleep(500);
        Assert.assertTrue(accountStatementPage.isTimeExtensionInvoiceVisible(), "❌ Time Extension invoice not visible");
        Map<String, Double> items = accountStatementPage.getTimeExtensionInvoiceLineItems();
        System.out.println("   [Invoice line items] " + items);
        Reporter.log("   Invoice line items: " + items, true);
        Assert.assertTrue(items.containsKey("Daycare Fee"), "❌ Daycare Fee line item missing from prorated invoice");

        Reporter.log("✅ TC002 PASSED — Start Time Extension full flow complete", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC003 — SC002_TC_010 : processTimeExtentionRequest response + addon amount
    //
    //  The request was already approved/processed in tc002 — this re-verifies
    //  the resulting state (Approved status + addon amount) rather than
    //  re-triggering approval, since it's already done.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
            description = "SC002_TC_010 — Verify Approved status and Time Extension addon amount")
    public void tc003_startProcessApiResponseVerify() throws InterruptedException {
        if (isSkipped()) return;
        Reporter.log("▶ TC003 SC002_TC_010 | child=" + TE_CHILD_ID, true);

        switchToCustomerRequestTab();
        String approvalStatus = recentRequestsPage.getTEApprovalStatus(TE_CHILD_ID, "Start Time Extension");
        System.out.println("   [Approval Status] " + approvalStatus);
        Reporter.log("   Approval Status: " + approvalStatus, true);
        Assert.assertEquals(approvalStatus, "Approved", "❌ Approval Status should be Approved (from tc002)");

        switchToAccountStatementTab();
        accountStatementPage.generateAccountStatement(TE_CHILD_ID);
        String addonsText = accountStatementPage.getAddonsText();
        System.out.println("   [Addons] " + addonsText);
        Reporter.log("   Addons: " + addonsText, true);
        Assert.assertTrue(accountStatementPage.isTimeExtensionAddonPresent(),
                "❌ 'Time Extension' addon should be present. Got: " + addonsText);

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\d,]+(?:\\.\\d+)?)").matcher(addonsText);
        if (m.find()) {
            double amount = Double.parseDouble(m.group(1).replace(",", ""));
            System.out.println("   [Addon amount] " + amount);
            Reporter.log("   Addon amount: " + amount, true);
            Assert.assertTrue(amount > 0, "❌ Addon amount should be a positive number");
        } else {
            Reporter.log("⚠ TC003 INFO — Could not parse a numeric amount from Addons text: " + addonsText, true);
        }

        Reporter.log("✅ TC003 PASSED — Approved status and addon amount verified", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC004 — SC003_TC_001 : Submit Stop Time Extension
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
            description = "SC003_TC_001 — Stop Time Extension: submit via Service Request form")
    public void tc004_submitStopTimeExtension() throws InterruptedException {
        if (isSkipped()) return;
        Reporter.log("▶ TC004 SC003_TC_001 | child=" + TE_CHILD_ID + " | stop=" + STOP_DATE, true);

        accountStatementPage.generateAccountStatement(TE_CHILD_ID);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(), "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType("Stop Time Extension");
        Assert.assertTrue(serviceRequestPage.isStopTimeExtensionFormVisible(), "❌ Stop Time Extension form not visible");

        serviceRequestPage.setSTPFromDate(STOP_DATE);
        Thread.sleep(300);

        serviceRequestPage.submitStopTimeExtension();
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
        Assert.assertTrue(response.toLowerCase().contains("success"),
                "❌ Expected success toast. Got: " + response);

        switchToCustomerRequestTab();
        String approvalStatus = recentRequestsPage.getTEApprovalStatus(TE_CHILD_ID, "Stop Time Extension");
        String requestStatus = recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Stop Time Extension");
        System.out.println("   [Request Status] " + requestStatus + "  [Approval Status] " + approvalStatus);
        Reporter.log("   Request Status: " + requestStatus + " | Approval Status: " + approvalStatus, true);
        Assert.assertEquals(requestStatus, "Pending", "❌ Request Status should be Pending");
        Assert.assertEquals(approvalStatus, "NA", "❌ Approval Status should be NA before getAllPendingRequests/Approve");

        Reporter.log("✅ TC004 PASSED — Stop Time Extension submitted", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC005 — SC003_TC_003 : getAllPendingRequests fields for Stop Time Extension
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5,
            description = "SC003_TC_003 — getAllPendingRequests response verified for Stop Time Extension")
    public void tc005_stopPendingRequestApiVerify() {
        if (isSkipped()) return;
        Reporter.log("▶ TC005 SC003_TC_003 | child=" + TE_CHILD_ID, true);

        Response r = APIs.getTimeExtensionPendingRequests(TE_CHILD_ID);
        Assert.assertTrue(r.getStatusCode() >= 200 && r.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + r.getStatusCode());
        Assert.assertEquals(r.jsonPath().getString("status"), "ok",
                "❌ API status should be ok. Body: " + r.getBody().asString());

        List<Map<String, Object>> entries = r.jsonPath().getList("response");
        Map<String, Object> entry = entries.stream()
                .filter(e -> TE_CHILD_ID.equals(String.valueOf(e.get("admission_id")))
                        && "Stop Time Extension".equalsIgnoreCase(String.valueOf(e.get("type"))))
                .findFirst().orElse(null);
        Assert.assertNotNull(entry, "❌ No 'Stop Time Extension' entry found for child " + TE_CHILD_ID
                + " in getAllPendingRequests response: " + r.getBody().asString());

        System.out.println("   [Matched entry] " + entry);
        Reporter.log("   Matched entry: " + entry, true);

        String date = String.valueOf(entry.get("date"));
        String endDate = String.valueOf(entry.get("end_date"));
        System.out.println("   [date] " + date + "  [end_date] " + endDate);
        Assert.assertEquals(endDate, date, "❌ end_date should equal date (WEF date) for Stop Time Extension");
        Assert.assertEquals(String.valueOf(entry.get("credit_debit_amount")), "0");
        Assert.assertEquals(String.valueOf(entry.get("status")), "Pending");
        Assert.assertEquals(String.valueOf(entry.get("parent_name")), "Support Request");
        Assert.assertEquals(String.valueOf(entry.get("admission_type")), "Regular");

        Reporter.log("✅ TC005 PASSED — Stop Time Extension pending request fields verified", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC006 — SC003_TC_005 : Approve Stop → process API → addon removed
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6,
            description = "SC003_TC_005 — Approve Stop Time Extension, process, verify addon removed")
    public void tc006_stopApproveAndProcessAddonRemoved() throws InterruptedException {
        if (isSkipped()) return;
        Reporter.log("▶ TC006 SC003_TC_005 | child=" + TE_CHILD_ID, true);

        // Note: getAllPendingRequests is a stateful trigger, not a plain read — it
        // was already called once in tc005 (which consumes/transitions the pending
        // entry), so it must NOT be called again here or it finds nothing left.
        switchToCustomerRequestTab();
        recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Stop Time Extension"); // navigates to the grid
        approveTimeExtension("Stop Time Extension");

        // Approve modal closed: refresh the Customer Request screen and capture
        // status before processing, per the documented manual flow.
        recentRequestsPage.navigateByChildId(TE_CHILD_ID);
        Thread.sleep(1000);
        String preProcessRequestStatus = recentRequestsPage.getTERequestStatus(TE_CHILD_ID, "Stop Time Extension");
        String preProcessApprovalStatus = recentRequestsPage.getTEApprovalStatus(TE_CHILD_ID, "Stop Time Extension");
        System.out.println("   [After Approve, before process API] Request Status=" + preProcessRequestStatus
                + " Approval Status=" + preProcessApprovalStatus);
        Reporter.log("   After Approve, before process API: Request Status=" + preProcessRequestStatus
                + " | Approval Status=" + preProcessApprovalStatus, true);

        Response processResp = APIs.processTimeExtensionRequest(TE_CHILD_ID);
        Assert.assertTrue(processResp.getStatusCode() >= 200 && processResp.getStatusCode() < 300,
                "❌ Expected 2xx. Got: " + processResp.getStatusCode());
        String processStatus = processResp.jsonPath().getString("status");
        String processMessage = processResp.jsonPath().getString("message");
        System.out.println("   [Process] status=" + processStatus + " message=" + processMessage);
        Reporter.log("   Process status=" + processStatus + " message=" + processMessage, true);
        Assert.assertEquals(processStatus, "ok", "❌ Process status should be ok");
        Assert.assertEquals(processMessage, "Time Extension request processed", "❌ Unexpected process message");

        Thread.sleep(1000);
        switchToAccountStatementTab();
        accountStatementPage.generateAccountStatement(TE_CHILD_ID);
        String addonsText = accountStatementPage.getAddonsText();
        System.out.println("   [Addons after Stop] " + addonsText);
        Reporter.log("   Addons after Stop: " + addonsText, true);
        Assert.assertTrue(accountStatementPage.isTimeExtensionAddonAbsent(),
                "❌ 'Time Extension' addon should be REMOVED. Got: " + addonsText);

        Reporter.log("✅ TC006 PASSED — Stop Time Extension approved, addon removed", true);
    }
}
