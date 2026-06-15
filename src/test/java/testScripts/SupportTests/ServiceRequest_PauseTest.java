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
import pages.Support.Regular_ServiceRequests;
import utils.BaseTest;
import utils.IAutoConstant;

public class ServiceRequest_PauseTest extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA
    // ═══════════════════════════════════════════════
    private static final String REGULAR_CHILD_ID = "49149"; // Active, Regular, V1
    // TODO: replace with a child that has a pause within last 60 days
    private static final String CHILD_60DAY_RULE = "TODO";
    // TODO: replace with an inactive / paused child
    private static final String INACTIVE_CHILD_ID = "TODO";

    private static final String SERVICE_TYPE = "Child Pause";

    // Pause date constants (ISO format — adjust if Pickaday locale differs)
    private static final String APR_01 = "2026-04-01";
    private static final String APR_15 = "2026-04-15";
    private static final String APR_30 = "2026-04-30";
    private static final String MAY_01 = "2026-05-01";
    private static final String MAY_02 = "2026-05-02";
    private static final String MAY_14 = "2026-05-14";
    private static final String MAY_20 = "2026-05-20";
    private static final String MAY_29 = "2026-05-29";
    private static final String MAY_31 = "2026-05-31";
    private static final String JUN_01 = "2026-06-01";
    private static final String APR_02 = "2026-04-02";
    private static final String JUN_29 = "2026-06-29";
    private static final String JUL_01 = "2026-07-01";
    private static final String MAY_02_2 = "2026-05-02";
    private static final String JUN_01_2 = "2026-06-01";

    Regular_ServiceRequests serviceRequestPage;
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
        serviceRequestPage = new Regular_ServiceRequests(driver);

        String user = getUserForScreen("Account Statement");
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for 'Regular Service Requests' in Excel");

        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — navigate to Account Statement
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        Thread.sleep(3000);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD — close modal/panel
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void closeAfterTest() {
        try {
            serviceRequestPage.closeModalByJs();
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════
    // HELPER — open service request panel for a child
    //          and select Child Pause type
    // ═══════════════════════════════════════════════
    private void openPauseForm(String childId)
            throws InterruptedException {
        accountStatementPage.generateAccountStatement(childId);
        Assert.assertTrue(accountStatementPage.isAccountSummaryVisible(),
                "❌ Account summary not visible for child: " + childId);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(),
                "❌ Service Request panel did not open");
        serviceRequestPage.selectServiceType(SERVICE_TYPE);
        Assert.assertTrue(serviceRequestPage.isPauseFormVisible(),
                "❌ Child Pause form did not load");
    }

    // ═══════════════════════════════════════════════
    // SC001_TC001 — 30-day same-month pause allowed
    // Data: From Apr 1 | To Apr 30 | Reason: Medical
    // ═══════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC001_TC001 — 30-day same-month pause accepted")
    public void sc001_tc001_valid30DaySameMonthPause()
            throws InterruptedException {
        Reporter.log("▶ SC001_TC001 — 30-day same-month pause", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(APR_30);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        String alertText = serviceRequestPage.getAlertText();
        Reporter.log("   Alert: " + alertText, true);
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected success toast. Got: " + msg);

        Reporter.log("✅ SC001_TC001 PASSED", true);
    }

    // ═══════════════════════════════════════════════
    // SC001_TC002 — 31-day full calendar month allowed (May 1–31)
    // ═══════════════════════════════════════════════
    @Test(priority = 2,
            description = "SC001_TC002 — 31-day full May pause accepted")
    public void sc001_tc002_valid31DayFullMonthMay()
            throws InterruptedException {
        Reporter.log("▶ SC001_TC002 — 31-day full May pause", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(MAY_01);
        serviceRequestPage.setPauseToDate(MAY_31);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected success. Got: " + msg);

        Reporter.log("✅ SC001_TC002 PASSED", true);
    }

    // ═══════════════════════════════════════════════
    // SC001_TC003 — 31-day pause NOT starting on 1st is BLOCKED
    // Data: May 2 – Jun 1 (31 days, invalid)
    // ═══════════════════════════════════════════════
    @Test(priority = 3,
            description = "SC001_TC003 — 31-day pause not from 1st is rejected")
    public void sc001_tc003_invalid31DayNotFromFirst()
            throws InterruptedException {
        Reporter.log("▶ SC001_TC003 — 31-day pause May 2–Jun 1 blocked", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(MAY_02_2);
        serviceRequestPage.setPauseToDate(JUN_01_2);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        if (serviceRequestPage.isAlertPresent()) {
            String alertText = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + alertText, true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ 31-day non-1st pause should be blocked, but got success");

        Reporter.log("✅ SC001_TC003 PASSED — request blocked as expected", true);
    }

    // ═══════════════════════════════════════════════
    // SC001_TC004 — Two-month 30-day pause allowed
    // Data: Apr 15 – May 14
    // ═══════════════════════════════════════════════
    @Test(priority = 4,
            description = "SC001_TC004 — Two-month 30-day pause accepted")
    public void sc001_tc004_twoMonth30DayPause()
            throws InterruptedException {
        Reporter.log("▶ SC001_TC004 — Two-month pause Apr 15–May 14", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(APR_15);
        serviceRequestPage.setPauseToDate(MAY_14);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected success. Got: " + msg);

        Reporter.log("✅ SC001_TC004 PASSED", true);
    }

    // ═══════════════════════════════════════════════
    // SC001_TC005 — Under 30 days treated as Leave
    // Data: May 1 – May 20 (20 days)
    // ═══════════════════════════════════════════════
    @Test(priority = 5,
            description = "SC001_TC005 — 20-day pause treated as Leave, submits")
    public void sc001_tc005_under30DayTreatedAsLeave()
            throws InterruptedException {
        Reporter.log("▶ SC001_TC005 — 20-day pause (Leave), no 60-day rule", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(MAY_01);
        serviceRequestPage.setPauseToDate(MAY_20);
        serviceRequestPage.enterPauseReason("Leave");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        // Leave (< 30 days) should submit successfully; no discount
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected submission. Got: " + msg);

        Reporter.log("✅ SC001_TC005 PASSED — 20-day leave submitted", true);
        Reporter.log("   ℹ Backend: no 50% discount expected (leave, not pause)", true);
    }

    // ═══════════════════════════════════════════════
    // SC001_TC006 — Exactly 29 days is a Leave
    // Data: May 1 – May 29
    // ═══════════════════════════════════════════════
    @Test(priority = 6,
            description = "SC001_TC006 — 29-day boundary treated as Leave")
    public void sc001_tc006_exactly29DayLeave()
            throws InterruptedException {
        Reporter.log("▶ SC001_TC006 — 29-day pause (Leave boundary)", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(MAY_01);
        serviceRequestPage.setPauseToDate(MAY_29);
        serviceRequestPage.enterPauseReason("Leave");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected submission. Got: " + msg);

        Reporter.log("✅ SC001_TC006 PASSED — 29-day leave submitted", true);
        Reporter.log("   ℹ Backend: no discount expected (< 30 days)", true);
    }

    // ═══════════════════════════════════════════════
    // SC008_TC001 — End date before start date: validation error
    // Data: From May 15 | To May 10
    // ═══════════════════════════════════════════════
    @Test(priority = 7,
            description = "SC008_TC001 — End before start shows validation error")
    public void sc008_tc001_endDateBeforeStart()
            throws InterruptedException {
        Reporter.log("▶ SC008_TC001 — End date before start date", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate("2026-05-15");
        serviceRequestPage.setPauseToDate("2026-05-10");
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        // Should NOT show a confirmation popup — form should block submit
        if (serviceRequestPage.isAlertPresent()) {
            String alertText = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + alertText, true);
            // If alert appeared, it must NOT be a success confirmation
            Assert.assertFalse(
                    alertText.toLowerCase().contains("do you want"),
                    "❌ Confirmation popup shown for invalid date range");
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Invalid date range should not succeed. Got: " + msg);

        Reporter.log("✅ SC008_TC001 PASSED — invalid range blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC008_TC002 — 1-day pause treated as Leave
    // Data: May 5 – May 5
    // ═══════════════════════════════════════════════
    @Test(priority = 8,
            description = "SC008_TC002 — 1-day pause treated as Leave, submits")
    public void sc008_tc002_oneDayPauseAsLeave()
            throws InterruptedException {
        Reporter.log("▶ SC008_TC002 — 1-day pause (Leave)", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate("2026-05-05");
        serviceRequestPage.setPauseToDate("2026-05-05");
        serviceRequestPage.enterPauseReason("Leave");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected submission. Got: " + msg);

        Reporter.log("✅ SC008_TC002 PASSED — 1-day leave submitted", true);
        Reporter.log("   ℹ Backend: no discount, no 60-day rule triggered", true);
    }

    // ═══════════════════════════════════════════════
    // SC008_TC003 — Cancel on confirmation popup: no request submitted
    // ═══════════════════════════════════════════════
    @Test(priority = 9,
            description = "SC008_TC003 — Cancel on popup: no submission")
    public void sc008_tc003_cancelOnConfirmationPopup()
            throws InterruptedException {
        Reporter.log("▶ SC008_TC003 — Cancel on confirmation popup", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(APR_30);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        String alertText = serviceRequestPage.getAlertText();
        Reporter.log("   Alert: " + alertText, true);
        serviceRequestPage.dismissAlert();

        // After cancel, form should still be open and no success message
        Assert.assertTrue(serviceRequestPage.isPauseFormVisible(),
                "❌ Pause form should still be visible after cancel");

        Reporter.log("✅ SC008_TC003 PASSED — cancel leaves form open, no submission", true);
    }

    // ═══════════════════════════════════════════════
    // SC008_TC004 — Reason field blank: mandatory validation
    // ═══════════════════════════════════════════════
    @Test(priority = 10,
            description = "SC008_TC004 — Blank reason blocked by validation")
    public void sc008_tc004_reasonBlankValidation()
            throws InterruptedException {
        Reporter.log("▶ SC008_TC004 — Reason field blank", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(APR_30);
        // Intentionally leave reason blank
        serviceRequestPage.submitPause();

        // Should NOT open confirmation popup if reason is mandatory
        if (serviceRequestPage.isAlertPresent()) {
            String alertText = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + alertText, true);
            serviceRequestPage.dismissAlert();
            Assert.fail("❌ Confirmation popup shown despite blank reason");
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Blank reason should not succeed. Got: " + msg);

        Reporter.log("✅ SC008_TC004 PASSED — blank reason blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC009_TC001 — Exception UI: checkbox, Ticket ID, Comments visible
    // ═══════════════════════════════════════════════
    @Test(priority = 11,
            description = "SC009_TC001 — Exception Case checkbox and Ticket ID fields visible")
    public void sc009_tc001_exceptionUIFieldsVisible()
            throws InterruptedException {
        Reporter.log("▶ SC009_TC001 — Exception UI fields check", true);

        openPauseForm(REGULAR_CHILD_ID);

        Assert.assertTrue(serviceRequestPage.isExceptionCaseCheckboxVisible(),
                "❌ Exception Case checkbox not visible");
        Assert.assertTrue(serviceRequestPage.isTicketIdFieldVisible(),
                "❌ Ticket ID field not visible");

        Reporter.log("✅ SC009_TC001 PASSED", true);
        Reporter.log("   ✅ Exception Case checkbox visible", true);
        Reporter.log("   ✅ Ticket ID field visible", true);
    }

    // ═══════════════════════════════════════════════
    // SC009_TC004 — Ticket ID is mandatory when Exception Case is checked
    // ═══════════════════════════════════════════════
    @Test(priority = 12,
            description = "SC009_TC004 — Ticket ID mandatory when Exception checked")
    public void sc009_tc004_ticketIdMandatoryWhenExceptionChecked()
            throws InterruptedException {
        Reporter.log("▶ SC009_TC004 — Exception checked, Ticket ID blank", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();

        Assert.assertTrue(serviceRequestPage.isTicketIdFieldEnabled(),
                "❌ Ticket ID field not enabled after checking exception");

        // Leave Ticket ID blank and try submit
        serviceRequestPage.setPauseFromDate("2026-04-02");
        serviceRequestPage.setPauseToDate("2026-05-02");
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        if (serviceRequestPage.isAlertPresent()) {
            String txt = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + txt, true);
            serviceRequestPage.dismissAlert();
            Assert.fail("❌ Popup shown despite missing Ticket ID");
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Should be blocked without Ticket ID. Got: " + msg);

        Reporter.log("✅ SC009_TC004 PASSED — blank Ticket ID blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC010_TC001 — Exception 90-day pause allowed
    // Data: Apr 1 – Jun 29 (90 days), Exception checked, Ticket ID: TKT-001
    // ═══════════════════════════════════════════════
    @Test(priority = 13,
            description = "SC010_TC001 — Exception 90-day pause accepted")
    public void sc010_tc001_exception90DayPauseAllowed()
            throws InterruptedException {
        Reporter.log("▶ SC010_TC001 — Exception 90-day pause (Apr 1–Jun 29)", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        serviceRequestPage.enterTicketId("TKT-001");
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(JUN_29);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown for 90-day exception pause");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected success for 90-day exception. Got: " + msg);

        Reporter.log("✅ SC010_TC001 PASSED — 90-day exception pause accepted", true);
        Reporter.log("   ℹ Backend: split into 3 × 30-day requests expected", true);
    }

    // ═══════════════════════════════════════════════
    // SC010_TC002 — Exception 91-day pause BLOCKED
    // Data: Apr 1 – Jul 1 (91 days)
    // ═══════════════════════════════════════════════
    @Test(priority = 14,
            description = "SC010_TC002 — Exception 91-day pause blocked")
    public void sc010_tc002_exception91DayBlocked()
            throws InterruptedException {
        Reporter.log("▶ SC010_TC002 — Exception 91-day pause (Apr 1–Jul 1) blocked", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        serviceRequestPage.enterTicketId("TKT-002");
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(JUL_01);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        if (serviceRequestPage.isAlertPresent()) {
            String txt = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + txt, true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ 91-day exception pause should be blocked. Got: " + msg);

        Reporter.log("✅ SC010_TC002 PASSED — 91-day blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC010_TC003 — Exception exactly 90-day boundary accepted
    // Data: Apr 1 – Jun 29 (90 days)
    // ═══════════════════════════════════════════════
    @Test(priority = 15,
            description = "SC010_TC003 — Exception 90-day boundary accepted")
    public void sc010_tc003_exception90DayBoundary()
            throws InterruptedException {
        Reporter.log("▶ SC010_TC003 — 90-day boundary (Apr 1–Jun 29)", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        serviceRequestPage.enterTicketId("TKT-003");
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(JUN_29);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown for 90-day boundary");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected success at 90-day boundary. Got: " + msg);

        Reporter.log("✅ SC010_TC003 PASSED — 90-day boundary accepted", true);
    }

    // ═══════════════════════════════════════════════
    // SC010_TC004 — Exception 31-day pause accepted
    // Data: Apr 2 – May 2 (31 days), Exception checked
    // ═══════════════════════════════════════════════
    @Test(priority = 16,
            description = "SC010_TC004 — Exception 31-day pause accepted")
    public void sc010_tc004_exception31DayAccepted()
            throws InterruptedException {
        Reporter.log("▶ SC010_TC004 — Exception 31-day pause (Apr 2–May 2)", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        serviceRequestPage.enterTicketId("TKT-004");
        serviceRequestPage.setPauseFromDate(APR_02);
        serviceRequestPage.setPauseToDate("2026-05-02");
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown for 31-day exception pause");
        serviceRequestPage.acceptAlert();

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success") ||
                        msg.toLowerCase().contains("submitted"),
                "❌ Expected success for 31-day exception. Got: " + msg);

        Reporter.log("✅ SC010_TC004 PASSED — 31-day exception accepted", true);
    }

    // ═══════════════════════════════════════════════
    // SC014_TC001 — Exception checked but Ticket ID blank: blocked
    // ═══════════════════════════════════════════════
    @Test(priority = 17,
            description = "SC014_TC001 — Exception checked, Ticket ID blank: blocked")
    public void sc014_tc001_exceptionCheckedTicketIdBlank()
            throws InterruptedException {
        Reporter.log("▶ SC014_TC001 — Exception checked, Ticket ID blank", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        // Intentionally leave Ticket ID blank
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate("2026-05-15");
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        if (serviceRequestPage.isAlertPresent()) {
            String txt = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + txt, true);
            serviceRequestPage.dismissAlert();
            Assert.fail("❌ Popup shown despite missing Ticket ID");
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Should be blocked. Got: " + msg);

        Reporter.log("✅ SC014_TC001 PASSED — missing Ticket ID blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC014_TC002 — No exception flag, 45 days: standard rule blocks
    // ═══════════════════════════════════════════════
    @Test(priority = 18,
            description = "SC014_TC002 — No exception flag, 45-day pause blocked")
    public void sc014_tc002_noExceptionFlag45DaysBlocked()
            throws InterruptedException {
        Reporter.log("▶ SC014_TC002 — Standard 45-day pause (no exception) blocked", true);

        openPauseForm(REGULAR_CHILD_ID);
        // Exception checkbox NOT checked
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate("2026-05-15"); // 45 days
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        if (serviceRequestPage.isAlertPresent()) {
            String txt = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + txt, true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ 45-day standard pause should be blocked. Got: " + msg);

        Reporter.log("✅ SC014_TC002 PASSED — 45-day non-exception blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC014_TC003 — Exception with invalid date range (end before start)
    // ═══════════════════════════════════════════════
    @Test(priority = 19,
            description = "SC014_TC003 — Exception pause with 0/negative days blocked")
    public void sc014_tc003_exceptionInvalidDateRange()
            throws InterruptedException {
        Reporter.log("▶ SC014_TC003 — Exception with end < start", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        serviceRequestPage.enterTicketId("TKT-009");
        serviceRequestPage.setPauseFromDate("2026-05-15");
        serviceRequestPage.setPauseToDate("2026-05-10");
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        if (serviceRequestPage.isAlertPresent()) {
            String txt = serviceRequestPage.getAlertText();
            Reporter.log("   Alert: " + txt, true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Invalid date range should be blocked. Got: " + msg);

        Reporter.log("✅ SC014_TC003 PASSED — invalid range blocked", true);
    }

    // ═══════════════════════════════════════════════
    // SC014_TC005 — Cancel on exception popup: no request submitted
    // ═══════════════════════════════════════════════
    @Test(priority = 20,
            description = "SC014_TC005 — Cancel exception popup: no submission")
    public void sc014_tc005_cancelOnExceptionPopup()
            throws InterruptedException {
        Reporter.log("▶ SC014_TC005 — Cancel on exception confirmation popup", true);

        openPauseForm(REGULAR_CHILD_ID);
        serviceRequestPage.checkExceptionCase();
        serviceRequestPage.enterTicketId("TKT-011");
        serviceRequestPage.setPauseFromDate(APR_01);
        serviceRequestPage.setPauseToDate(JUN_29);
        serviceRequestPage.enterPauseReason("Medical");
        serviceRequestPage.submitPause();

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown");
        String alertText = serviceRequestPage.getAlertText();
        Reporter.log("   Alert: " + alertText, true);
        serviceRequestPage.dismissAlert();

        Assert.assertTrue(serviceRequestPage.isPauseFormVisible(),
                "❌ Form should still be visible after cancel");

        Reporter.log("✅ SC014_TC005 PASSED — cancel leaves form open, no submission", true);
    }
}

