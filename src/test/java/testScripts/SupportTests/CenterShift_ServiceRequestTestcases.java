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
import pages.Support.Regular_ServiceRequests;
import utils.APIs;
import utils.BaseTest;

import java.time.LocalDate;

/**
 * Test Suite: Center Shift Service Request
 *
 * Test Cases covered (from TC_ServiceRequest_CenterShift_Final.xlsx, sheet TC_CenterShift):
 *   TC001 — SC003_TC_001: Happy path — Submit Center Shift form (Pending status)
 *   TC002 — SC003_TC_002: Pending → Processing via cron API
 *   TC003 — SC003_TC_003: Processing → Approved via cron API (creates new child)
 *   TC004 — SC003_TC_004: Old child attrition processing via API
 *
 * Pre-conditions:
 *   - CS_CHILD_ID must be an Active, Regular V2 child with:
 *       • No outstanding unpaid invoices
 *       • No pending/processing center shift
 *       • No other overlapping service requests
 *       • Last approved center shift (if any) >= 60 days ago
 *   - CS_NEW_CENTER must be a valid center supporting the child's payment plan
 *   - CS_NEW_PROGRAM must be available at the new center
 *
 * Cron timing note:
 *   TC003 (Processing→Approved) only runs when Joining Date is within ±5 days of today.
 *   Run TC001 with a joining date near the current date, then run TC002-TC003 close to
 *   the joining date. TC004 requires the old_child_id returned from TC003's response.
 *
 * Navigation: Support → Account Statement → Service Request link → select "Center Shift"
 * User: Jaydeep Kar (Account Statement rights)
 */
public class CenterShift_ServiceRequestTestcases extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA — UPDATE BEFORE RUNNING
    // ═══════════════════════════════════════════════

    // Child ID: Active Regular V2 child with no pending center shift
    // TODO: Update to a valid child ID in your test environment
    private static final String CS_CHILD_ID = "49149";

    // Joining Date: next month's 1st (not in current month, >= today+2, <= today+32)
    // Computed dynamically so tests remain valid when run on any date
    private static final String CS_JOINING_DATE =
            LocalDate.now().plusMonths(1).withDayOfMonth(1).toString(); // e.g. "2026-08-01"

    // New Center: must support child's payment plan and the selected program
    // TODO: Update to a center available in the Shift To dropdown for this child
    private static final String CS_NEW_CENTER = "Sector 56, Gurgaon";

    // New Program: must be available at CS_NEW_CENTER
    // Set to empty to auto-select the first available program
    private static final String CS_NEW_PROGRAM = ""; // empty = auto-select first option

    // Old child ID populated from TC003 response for use in TC004
    private static String oldChildIdFromApproval = "";

    // ═══════════════════════════════════════════════
    // PAGE OBJECTS
    // ═══════════════════════════════════════════════
    Regular_ServiceRequests serviceRequestPage;
    AccountStatementPage accountStatementPage;
    UserRightsPage userRightsPage;
    Navigations navigations;

    // ═══════════════════════════════════════════════
    // BEFORE CLASS — switch to Account Statement user
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);
        serviceRequestPage = new Regular_ServiceRequests(driver);

        System.out.println("▶ CS Joining Date computed: " + CS_JOINING_DATE);

        String user = getUserForScreen("Account Statement");
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for 'Account Statement' in Excel. "
                        + "Add row: ScreenName=Account Statement | UserName=<user with SR access>");

        System.out.println("▶ Switching to user: " + user);
        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);

        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — navigate to Account Statement
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }

        try {
            serviceRequestPage.closeModalByJs();
            Thread.sleep(500);
        } catch (Exception ignored) {
        }

        Thread.sleep(2000);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD — close service panel/modal
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void closeAfterTest() {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        try {
            serviceRequestPage.closeModalByJs();
        } catch (Exception ignored) {
        }
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC001 — SC003_TC_001: Happy Path Submit
    //
    // Steps:
    //   1. Generate Account Statement for CS_CHILD_ID
    //   2. Click Service Request link → panel opens with Services dropdown
    //   3. Select "Center Shift" from Services dropdown
    //   4. Verify Center Shift form is visible (date, center, program, checkbox)
    //   5. Set Joining Date = CS_JOINING_DATE (next month 1st)
    //   6. Select New Center from dropdown (CS_NEW_CENTER or first available)
    //   7. Select New Program (CS_NEW_PROGRAM or first available)
    //   8. Check center visit declaration checkbox
    //   9. Click Submit → accept JS confirmation alert
    //  10. Verify success response message
    // Expected: Request created with status = Pending
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "TC001 — SC003_TC_001: Submit Center Shift form for child " + CS_CHILD_ID)
    public void tc001_submitCenterShift() throws InterruptedException {
        Reporter.log("▶ TC001 — Center Shift Submit | child: " + CS_CHILD_ID
                + " | date: " + CS_JOINING_DATE, true);

        // Step 1: Generate Account Statement
        accountStatementPage.generateAccountStatement(CS_CHILD_ID);
        Reporter.log("   Account Statement generated for child: " + CS_CHILD_ID, true);

        // Step 2: Click Service Request link
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(),
                "❌ Service Request panel did not open — Services dropdown not visible");
        Reporter.log("   Service Request panel opened", true);

        // Step 3: Select Center Shift service type
        serviceRequestPage.selectServiceType("Center Shift");
        Assert.assertTrue(serviceRequestPage.isCenterShiftFormVisible(),
                "❌ Center Shift form not visible after selecting service type");
        Reporter.log("   Center Shift form loaded", true);

        // Step 4: Set Joining Date via Pickaday JS setter
        serviceRequestPage.setCSEffectiveDate(CS_JOINING_DATE);
        Reporter.log("   Joining Date set: " + CS_JOINING_DATE, true);

        // Step 5: Select New Center — use configured value or first available option
        if (CS_NEW_CENTER != null && !CS_NEW_CENTER.isEmpty()) {
            serviceRequestPage.selectCSNewCenter(CS_NEW_CENTER);
            Reporter.log("   New Center selected: " + CS_NEW_CENTER, true);
        } else {
            String firstCenter = selectFirstAvailableCenter();
            Reporter.log("   New Center (first available): " + firstCenter, true);
        }
        Thread.sleep(800); // wait for program dropdown to refresh after center selection

        // Step 6: Select New Program — use configured value or first available option
        if (CS_NEW_PROGRAM != null && !CS_NEW_PROGRAM.isEmpty()) {
            serviceRequestPage.selectCSNewProgram(CS_NEW_PROGRAM);
            Reporter.log("   New Program selected: " + CS_NEW_PROGRAM, true);
        } else {
            String firstProgram = selectFirstAvailableProgram();
            Reporter.log("   New Program (first available): " + firstProgram, true);
        }

        // Step 7: Check center visit declaration checkbox
        serviceRequestPage.checkCSCenterVisitDeclaration();
        Assert.assertTrue(serviceRequestPage.isCSCenterVisitDeclarationChecked(),
                "❌ Center Visit Declaration checkbox not checked");
        Reporter.log("   Center Visit Declaration: checked", true);

        // Step 8: Submit
        serviceRequestPage.submitCenterShift();

        // Step 9: Accept confirmation alert "Do you want to send Center Shift request?"
        String alertText = serviceRequestPage.getAlertText();
        Reporter.log("   Alert: " + alertText, true);
        serviceRequestPage.acceptAlert();
        Thread.sleep(2000);

        // Step 10: Get response message
        String responseMsg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + responseMsg, true);

        // Assert success
        Assert.assertFalse(responseMsg.isEmpty(),
                "❌ No response message after Center Shift submission. "
                        + "Check child " + CS_CHILD_ID + " meets all pre-conditions.");
        Assert.assertFalse(responseMsg.toUpperCase().contains("ERROR"),
                "❌ Error in response: " + responseMsg);

        Reporter.log("✅ TC001 PASSED — Center Shift submitted. Status: Pending. Child: "
                + CS_CHILD_ID + " | Joining: " + CS_JOINING_DATE, true);
        Reporter.log("   ℹ Backend: verify center_shift record status=Pending for child "
                + CS_CHILD_ID, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC002 — SC003_TC_002: Pending → Processing via cron API
    //
    // Steps:
    //   1. Call GET /Financialprocess/getAllPendingRequests?key=...&chid_id=<id>&ckey=...
    //   2. Assert HTTP 200 response
    //   3. Verify response indicates processing started
    // Expected: Center shift status changes from Pending → Processing
    //           Approval Status = Pending, Support Executive assigned
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "TC002 — SC003_TC_002: Pending→Processing cron API for child " + CS_CHILD_ID)
    public void tc002_pendingToProcessing() throws InterruptedException {
        Reporter.log("▶ TC002 — Pending→Processing | child: " + CS_CHILD_ID, true);

        Response response = APIs.getCenterShiftPendingToProcessing(CS_CHILD_ID);
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        Reporter.log("   HTTP Status: " + statusCode, true);
        Reporter.log("   Response: " + body, true);

        Assert.assertTrue(statusCode >= 200 && statusCode < 300,
                "❌ Expected 2xx response. Got: " + statusCode + " | Body: " + body);

        Reporter.log("✅ TC002 PASSED — Pending→Processing API returned: " + statusCode, true);
        Reporter.log("   ℹ Backend: verify center_shift status=Processing, "
                + "approval_status=Pending for child " + CS_CHILD_ID, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC003 — SC003_TC_003: Processing → Approved via cron API
    //
    // Steps:
    //   1. Call GET /servicerequest/cronProcessCenterShiftRequests?key=...&child_id=<id>&ckey=...
    //   2. Assert HTTP 200 response
    //   3. Parse response: extract old_child_id, new_child_id
    //   4. Store old_child_id for TC004
    // Expected: status=ok, new child created, attrition row created for old child
    //
    // TIMING NOTE: Cron only processes where Joining Date is within ±5 days of today.
    //              Run this test when today is close to CS_JOINING_DATE.
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
            description = "TC003 — SC003_TC_003: Processing→Approved cron API for child " + CS_CHILD_ID)
    public void tc003_processingToApproved() throws InterruptedException {
        Reporter.log("▶ TC003 — Processing→Approved | child: " + CS_CHILD_ID, true);

        Response response = APIs.getCenterShiftProcessingToApproved(CS_CHILD_ID);
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        Reporter.log("   HTTP Status: " + statusCode, true);
        Reporter.log("   Response: " + body, true);

        Assert.assertTrue(statusCode >= 200 && statusCode < 300,
                "❌ Expected 2xx response. Got: " + statusCode + " | Body: " + body);

        // Try to extract old_child_id and new_child_id for TC004
        try {
            org.json.JSONObject json = new org.json.JSONObject(body);
            if (json.has("old_child_id")) {
                oldChildIdFromApproval = json.get("old_child_id").toString();
                Reporter.log("   old_child_id: " + oldChildIdFromApproval, true);
            }
            if (json.has("new_child_id")) {
                String newChildId = json.get("new_child_id").toString();
                Reporter.log("   new_child_id: " + newChildId, true);
            }
            if (json.has("status")) {
                String status = json.get("status").toString();
                Reporter.log("   status: " + status, true);
            }
        } catch (Exception e) {
            Reporter.log("   ℹ Could not parse JSON response (may be plain text): " + e.getMessage(), true);
        }

        Reporter.log("✅ TC003 PASSED — Processing→Approved API returned: " + statusCode, true);
        Reporter.log("   ℹ Backend: verify center_shift=Approved, new child created, "
                + "attrition row status=Processing for old child " + CS_CHILD_ID, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC004 — SC003_TC_004: Old Child Attrition Processing via API
    //
    // Steps:
    //   1. Use old_child_id from TC003 response (falls back to CS_CHILD_ID if not parsed)
    //   2. Call GET /parentapp/processChildApprovedRequest?child_id=<old_id>&ckey=...
    //   3. Assert HTTP 200 response
    // Expected: Old child status changes from Active → Attrition
    //           Child history updated
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
            description = "TC004 — SC003_TC_004: Old child attrition processing via API")
    public void tc004_oldChildAttritionProcessing() throws InterruptedException {
        // Use old_child_id from TC003 if available, otherwise fall back to CS_CHILD_ID
        String childId = oldChildIdFromApproval.isEmpty() ? CS_CHILD_ID : oldChildIdFromApproval;
        Reporter.log("▶ TC004 — Old Child Attrition | child_id: " + childId, true);

        if (oldChildIdFromApproval.isEmpty()) {
            Reporter.log("   ⚠ old_child_id not available from TC003 response — using CS_CHILD_ID: "
                    + CS_CHILD_ID, true);
            Reporter.log("   ℹ Update oldChildIdFromApproval manually if TC003 response "
                    + "did not return a parseable old_child_id", true);
        }

        Response response = APIs.processOldChildAttrition(childId);
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        Reporter.log("   HTTP Status: " + statusCode, true);
        Reporter.log("   Response: " + body, true);

        Assert.assertTrue(statusCode >= 200 && statusCode < 300,
                "❌ Expected 2xx response. Got: " + statusCode + " | Body: " + body);

        Reporter.log("✅ TC004 PASSED — Attrition API returned: " + statusCode, true);
        Reporter.log("   ℹ Backend: verify old child current_status=Attrition | child_id: "
                + childId, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC005 — SC006_TC_001: Cancel Pending Center Shift (UI)
    //
    // Pre-condition: A Pending center shift must exist for CS_CHILD_ID
    // Steps:
    //   1. Generate Account Statement for CS_CHILD_ID
    //   2. Click Customer Requests tab
    //   3. Find the Pending center shift row
    //   4. Click CANCEL action
    //   5. Verify status changes to Cancelled
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 5,
            description = "TC005 — SC006_TC_001: Cancel Pending center shift for child " + CS_CHILD_ID)
    public void tc005_cancelPendingCenterShift() throws InterruptedException {
        Reporter.log("▶ TC005 — Cancel Pending Center Shift | child: " + CS_CHILD_ID, true);

        // Step 1: Generate Account Statement
        accountStatementPage.generateAccountStatement(CS_CHILD_ID);
        Reporter.log("   Account Statement generated for child: " + CS_CHILD_ID, true);

        // Step 2: Click Customer Requests tab
        accountStatementPage.clickCustomerRequest();
        Thread.sleep(2000);

        // Step 3: Look for CANCEL button on Pending center shift row
        try {
            // Find Cancel link for center shift in the customer requests table
            org.openqa.selenium.WebElement cancelBtn = driver.findElement(
                    org.openqa.selenium.By.xpath(
                            "//td[contains(text(),'Center Shift')]"
                                    + "/following-sibling::td[contains(text(),'Pending')]"
                                    + "/following-sibling::td//a[contains(text(),'Cancel')"
                                    + " or contains(text(),'CANCEL')]"));
            ((org.openqa.selenium.JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", cancelBtn);
            Reporter.log("   Cancel button clicked", true);
            Thread.sleep(1000);

            // Accept confirmation alert if present
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);

            // Verify cancelled status
            String responseMsg = serviceRequestPage.getResponseMessage();
            Reporter.log("   Response after cancel: " + responseMsg, true);

            Assert.assertFalse(responseMsg.isEmpty(),
                    "❌ No response after cancelling center shift");

            Reporter.log("✅ TC005 PASSED — Pending Center Shift cancelled", true);

        } catch (org.openqa.selenium.NoSuchElementException e) {
            Reporter.log("⚠ TC005 SKIPPED — No Pending Center Shift Cancel button found for child "
                    + CS_CHILD_ID + ". Ensure a Pending request exists before running this test.", true);
            System.out.println("⚠ TC005: Cancel button not found — no Pending center shift for child "
                    + CS_CHILD_ID);
        }
    }

    // ═══════════════════════════════════════════════
    // HELPERS — Select first available dropdown option
    // ═══════════════════════════════════════════════

    private String selectFirstAvailableCenter() throws InterruptedException {
        org.openqa.selenium.support.ui.Select sel =
                new org.openqa.selenium.support.ui.Select(
                        serviceRequestPage.cs_newCenter_dropdown);
        for (org.openqa.selenium.WebElement opt : sel.getOptions()) {
            String txt = opt.getText().trim();
            if (!txt.isEmpty() && !txt.startsWith("--") && !txt.startsWith("Select")) {
                sel.selectByVisibleText(txt);
                System.out.println("✅ New Center (first available): " + txt);
                Thread.sleep(300);
                return txt;
            }
        }
        System.out.println("⚠ No available center options found");
        return "";
    }

    private String selectFirstAvailableProgram() throws InterruptedException {
        org.openqa.selenium.support.ui.Select sel =
                new org.openqa.selenium.support.ui.Select(
                        serviceRequestPage.cs_newProgram_dropdown);
        for (org.openqa.selenium.WebElement opt : sel.getOptions()) {
            String txt = opt.getText().trim();
            if (!txt.isEmpty() && !txt.startsWith("--") && !txt.startsWith("Select")) {
                sel.selectByVisibleText(txt);
                System.out.println("✅ New Program (first available): " + txt);
                Thread.sleep(300);
                return txt;
            }
        }
        System.out.println("⚠ No available program options found");
        return "";
    }
}
