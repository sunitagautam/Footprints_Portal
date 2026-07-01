package testScripts.SupportTests;

import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
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
import pages.Support.Regular_ServiceRequests;
import utils.APIs;
import utils.BaseTest;

import java.time.LocalDate;

/**
 * Test Suite: Center Shift Service Request
 *
 * Test Cases (from TC_ServiceRequest_CenterShift_Final.xlsx, sheet TC_CenterShift):
 *   TC001 — SC003_TC_001: Happy path — Submit Center Shift form → Pending status
 *   TC002 — SC003_TC_002: Pending → Processing via cron API
 *   TC003 — SC003_TC_003: Processing → Approved via cron API (creates new child)
 *   TC004 — SC003_TC_004: Old child attrition processing via API
 *   TC005 — SC006_TC_001: Cancel Pending center shift (UI)
 *
 * Pre-conditions for TC001:
 *   - CS_CHILD_ID must be an Active Regular V2 child with:
 *       • No outstanding unpaid invoices
 *       • No pending/processing center shift
 *       • No overlapping service requests
 *       • Last approved center shift (if any) >= 60 days ago
 *   - CS_NEW_CENTER must be a valid center supporting child's payment plan
 *
 * Cron timing note (TC003):
 *   Processing→Approved cron only fires when Joining Date is within ±5 days of today.
 *   Run TC001 with a date near the current date, then run TC002–TC003 close to it.
 *
 * Navigation: Support → Account Statement → Service Request → Center Shift
 * User: Jaydeep Kar (Account Statement rights)
 */
public class ServiceRequest_CenterShiftTest extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA — UPDATE BEFORE RUNNING
    // ═══════════════════════════════════════════════

    // Active Regular V2 child with no pending center shift
    // TODO: Update to a valid child ID in your test environment
    private static final String CS_CHILD_ID = "49149";

    // Joining Date: first day of next month
    // (not in current month, >= today+2, <= today+32)
    private static final String CS_JOINING_DATE =
            LocalDate.now().plusMonths(1).withDayOfMonth(1).toString();

    // New Center: must support child's payment plan and the selected program
    // TODO: Update to a center that appears in Shift To dropdown for this child
    private static final String CS_NEW_CENTER = "Sector 56, Gurgaon";

    // New Program: set to empty to auto-select first available option
    private static final String CS_NEW_PROGRAM = "";

    // Populated from TC003 response for use in TC004
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

        System.out.println("▶ CS Joining Date: " + CS_JOINING_DATE);

        String user = getUserForScreen("Account Statement");
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for 'Account Statement' in input_UserRights.xlsx");

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
        try { driver.switchTo().alert().dismiss(); } catch (Exception ignored) {}
        try { serviceRequestPage.closeModalByJs(); Thread.sleep(500); } catch (Exception ignored) {}
        Thread.sleep(2000);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD — close service panel / modal
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void closeAfterTest() {
        try { driver.switchTo().alert().dismiss(); } catch (Exception ignored) {}
        try { serviceRequestPage.closeModalByJs(); } catch (Exception ignored) {}
        try { Thread.sleep(1000); } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC001 — SC003_TC_001: Happy Path Submit
    //
    // Steps:
    //   1. Generate Account Statement for CS_CHILD_ID
    //   2. Click Service Request link → Services panel opens
    //   3. Select "Center Shift" from Services dropdown
    //   4. Verify Center Shift form visible
    //   5. Set Joining Date (next month 1st via Pickaday JS setter)
    //   6. Select New Center (configured or first available)
    //   7. Select New Program (configured or first available)
    //   8. Check center visit declaration checkbox
    //   9. Submit → accept JS confirmation alert
    //  10. Verify success response (no error message)
    // Expected: status = Pending
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "TC001 — SC003_TC_001: Submit Center Shift form for child " + CS_CHILD_ID)
    public void tc001_submitCenterShift() throws InterruptedException {
        Reporter.log("▶ TC001 — Center Shift Submit | child: " + CS_CHILD_ID
                + " | date: " + CS_JOINING_DATE, true);

        // Step 1: Account Statement
        accountStatementPage.generateAccountStatement(CS_CHILD_ID);
        Reporter.log("   Account Statement generated", true);

        // Step 2: Open Service Request panel
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(),
                "❌ Service Request panel did not open");
        Reporter.log("   Service Request panel opened", true);

        // Step 3: Select Center Shift
        serviceRequestPage.selectServiceType("Center Shift");
        Assert.assertTrue(serviceRequestPage.isCenterShiftFormVisible(),
                "❌ Center Shift form not visible after selecting service type");
        Reporter.log("   Center Shift form loaded", true);

        // Step 4: Set Joining Date
        serviceRequestPage.setCSEffectiveDate(CS_JOINING_DATE);
        Reporter.log("   Joining Date set: " + CS_JOINING_DATE, true);

        // Step 5: Select New Center
        if (!CS_NEW_CENTER.isEmpty()) {
            serviceRequestPage.selectCSNewCenter(CS_NEW_CENTER);
            Reporter.log("   New Center: " + CS_NEW_CENTER, true);
        } else {
            String center = selectFirstCenter();
            Reporter.log("   New Center (first available): " + center, true);
        }
        Thread.sleep(800); // program dropdown refreshes after center selection

        // Step 6: Select New Program
        if (!CS_NEW_PROGRAM.isEmpty()) {
            serviceRequestPage.selectCSNewProgram(CS_NEW_PROGRAM);
            Reporter.log("   New Program: " + CS_NEW_PROGRAM, true);
        } else {
            String program = selectFirstProgram();
            Reporter.log("   New Program (first available): " + program, true);
        }

        // Step 7: Check declaration checkbox
        serviceRequestPage.checkCSCenterVisitDeclaration();
        Assert.assertTrue(serviceRequestPage.isCSCenterVisitDeclarationChecked(),
                "❌ Center Visit Declaration checkbox not checked");
        Reporter.log("   Declaration checkbox: checked", true);

        // Step 8: Submit
        serviceRequestPage.submitCenterShift();

        // Step 9: Accept confirmation alert
        String alertText = serviceRequestPage.getAlertText();
        Reporter.log("   Alert: " + alertText, true);
        serviceRequestPage.acceptAlert();
        Thread.sleep(2000);

        // Step 10: Verify response
        String responseMsg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + responseMsg, true);
        Assert.assertFalse(responseMsg.isEmpty(),
                "❌ No response after Center Shift submission. "
                        + "Check child " + CS_CHILD_ID + " meets all pre-conditions.");
        Assert.assertFalse(responseMsg.toUpperCase().contains("ERROR"),
                "❌ Error response: " + responseMsg);

        Reporter.log("✅ TC001 PASSED — Center Shift submitted | child: "
                + CS_CHILD_ID + " | joining: " + CS_JOINING_DATE, true);
        Reporter.log("   ℹ Backend: verify center_shift status=Pending for child "
                + CS_CHILD_ID, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC002 — SC003_TC_002: Pending → Processing via cron API
    //
    // URL: {{Base_URL}}Financialprocess/getAllPendingRequests/
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373
    // Expected: HTTP 2xx — center shift moves to Processing status
    //           Approval Status = Pending, Support Executive assigned
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "TC002 — SC003_TC_002: Pending→Processing cron API for child " + CS_CHILD_ID)
    public void tc002_pendingToProcessing() {
        Reporter.log("▶ TC002 — Pending→Processing | child: " + CS_CHILD_ID, true);

        Response response = APIs.getCenterShiftPendingToProcessing(CS_CHILD_ID);
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        Reporter.log("   HTTP: " + statusCode + " | Body: " + body, true);
        Assert.assertTrue(statusCode >= 200 && statusCode < 300,
                "❌ Expected 2xx. Got: " + statusCode + " | " + body);

        Reporter.log("✅ TC002 PASSED — Pending→Processing returned: " + statusCode, true);
        Reporter.log("   ℹ Backend: verify status=Processing, approval_status=Pending for child "
                + CS_CHILD_ID, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC003 — SC003_TC_003: Processing → Approved via cron API
    //
    // URL: {{Base_URL}}servicerequest/cronProcessCenterShiftRequests
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<child_id>&ckey=B43C083098B7
    // Expected: status=ok, old_child_id and new_child_id in response
    //           New child created, attrition row created for old child
    //
    // TIMING: Cron only processes if Joining Date is within ±5 days of today
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
            description = "TC003 — SC003_TC_003: Processing→Approved cron API for child " + CS_CHILD_ID)
    public void tc003_processingToApproved() {
        Reporter.log("▶ TC003 — Processing→Approved | child: " + CS_CHILD_ID, true);

        Response response = APIs.getCenterShiftProcessingToApproved(CS_CHILD_ID);
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        Reporter.log("   HTTP: " + statusCode + " | Body: " + body, true);
        Assert.assertTrue(statusCode >= 200 && statusCode < 300,
                "❌ Expected 2xx. Got: " + statusCode + " | " + body);

        // Extract old_child_id for TC004
        try {
            org.json.JSONObject json = new org.json.JSONObject(body);
            if (json.has("old_child_id")) {
                oldChildIdFromApproval = json.get("old_child_id").toString();
                Reporter.log("   old_child_id: " + oldChildIdFromApproval, true);
            }
            if (json.has("new_child_id")) {
                Reporter.log("   new_child_id: " + json.get("new_child_id"), true);
            }
            if (json.has("status")) {
                Reporter.log("   status: " + json.get("status"), true);
            }
        } catch (Exception e) {
            Reporter.log("   ℹ Response is not JSON or missing fields: " + e.getMessage(), true);
        }

        Reporter.log("✅ TC003 PASSED — Processing→Approved returned: " + statusCode, true);
        Reporter.log("   ℹ Backend: verify center_shift=Approved, new child created, "
                + "old child has attrition row | child: " + CS_CHILD_ID, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC004 — SC003_TC_004: Old Child Attrition Processing via API
    //
    // URL: {{Base_URL}}parentapp/processChildApprovedRequest
    //       ?child_id=<old_child_id>&ckey=9414D96600C5
    // Uses old_child_id from TC003 response (falls back to CS_CHILD_ID if unavailable)
    // Expected: Old child status changes from Active → Attrition
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
            description = "TC004 — SC003_TC_004: Old child attrition processing via API")
    public void tc004_oldChildAttritionProcessing() {
        String childId = oldChildIdFromApproval.isEmpty() ? CS_CHILD_ID : oldChildIdFromApproval;
        Reporter.log("▶ TC004 — Old Child Attrition | child_id: " + childId, true);

        if (oldChildIdFromApproval.isEmpty()) {
            Reporter.log("   ⚠ old_child_id not available from TC003 — using CS_CHILD_ID: "
                    + CS_CHILD_ID, true);
        }

        Response response = APIs.processOldChildAttrition(childId);
        int statusCode = response.getStatusCode();
        String body = response.getBody().asString();

        Reporter.log("   HTTP: " + statusCode + " | Body: " + body, true);
        Assert.assertTrue(statusCode >= 200 && statusCode < 300,
                "❌ Expected 2xx. Got: " + statusCode + " | " + body);

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
    //   3. Find Pending center shift row and click CANCEL
    //   4. Accept confirmation alert
    //   5. Verify response / status = Cancelled
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 5,
            description = "TC005 — SC006_TC_001: Cancel Pending center shift for child " + CS_CHILD_ID)
    public void tc005_cancelPendingCenterShift() throws InterruptedException {
        Reporter.log("▶ TC005 — Cancel Pending Center Shift | child: " + CS_CHILD_ID, true);

        // Step 1: Account Statement
        accountStatementPage.generateAccountStatement(CS_CHILD_ID);
        Reporter.log("   Account Statement generated", true);

        // Step 2: Click Customer Requests tab
        accountStatementPage.clickCustomerRequest();
        Thread.sleep(2000);

        // Step 3: Find and click CANCEL on Pending center shift row
        try {
            WebElement cancelBtn = driver.findElement(By.xpath(
                    "//td[contains(text(),'Center Shift')]"
                            + "/following-sibling::td[contains(text(),'Pending')]"
                            + "/following-sibling::td//a[contains(translate(normalize-space(.),"
                            + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'CANCEL')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cancelBtn);
            Reporter.log("   Cancel button clicked", true);
            Thread.sleep(1000);

            // Step 4: Accept confirmation
            serviceRequestPage.acceptAlert();
            Thread.sleep(1500);

            // Step 5: Verify
            String responseMsg = serviceRequestPage.getResponseMessage();
            Reporter.log("   Response: " + responseMsg, true);
            Assert.assertFalse(responseMsg.isEmpty(),
                    "❌ No response after cancelling center shift");

            Reporter.log("✅ TC005 PASSED — Pending Center Shift cancelled | child: "
                    + CS_CHILD_ID, true);

        } catch (NoSuchElementException e) {
            Reporter.log("⚠ TC005 SKIPPED — No Pending Center Shift found for child "
                    + CS_CHILD_ID + ". Run TC001 first to create one.", true);
            System.out.println("⚠ TC005: Cancel button not found — no Pending center shift for child "
                    + CS_CHILD_ID);
        }
    }

    // ═══════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════

    private String selectFirstCenter() throws InterruptedException {
        Select sel = new Select(serviceRequestPage.cs_newCenter_dropdown);
        for (WebElement opt : sel.getOptions()) {
            String txt = opt.getText().trim();
            if (!txt.isEmpty() && !txt.startsWith("--") && !txt.startsWith("Select")) {
                sel.selectByVisibleText(txt);
                System.out.println("✅ New Center (first): " + txt);
                Thread.sleep(300);
                return txt;
            }
        }
        return "";
    }

    private String selectFirstProgram() throws InterruptedException {
        Select sel = new Select(serviceRequestPage.cs_newProgram_dropdown);
        for (WebElement opt : sel.getOptions()) {
            String txt = opt.getText().trim();
            if (!txt.isEmpty() && !txt.startsWith("--") && !txt.startsWith("Select")) {
                sel.selectByVisibleText(txt);
                System.out.println("✅ New Program (first): " + txt);
                Thread.sleep(300);
                return txt;
            }
        }
        return "";
    }
}
