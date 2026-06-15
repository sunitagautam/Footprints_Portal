package testScripts.SupportTests;

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
import pages.Support.Regular_ServiceRequests;
import utils.BaseTest;

public class ServiceRequest_ProgramChangeTest extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA — Child IDs
    // ═══════════════════════════════════════════════
    private static final String REGULAR_CHILD_ID = "69073"; // Active, Regular, V1
    private static final String CORPORATE_CHILD_ID = "68908"; // Corporate Tie-up (SC013)
    private static final String PAUSED_CHILD_ID = "65564"; // TODO: confirm paused child
    private static final String INACTIVE_CHILD_ID = "67348"; // TODO: confirm inactive child

    // ═══════════════════════════════════════════════
    // TEST DATA — New Program dropdown values
    // Confirmed from live UI (child #48019 screenshot)
    // ═══════════════════════════════════════════════
    private static final String PROG_FULL_DAY       = "Full Day (09:00 AM to 06:30 PM)";
    private static final String PROG_EXT_PRESCHOOL  = "Extended Preschool (09:00 AM to 03:30 PM)";
    private static final String PROG_AFTER_SCHOOL   = "After School (01:30 PM to 06:30 PM)";
    private static final String PROG_EVENING        = "Evening Program (03:00 PM to 06:30 PM)";
    private static final String PROG_SELECT         = "--Select--";

    // ═══════════════════════════════════════════════
    // TEST DATA — Service type & Effective dates
    // ═══════════════════════════════════════════════
    // ═══════════════════════════════════════════════
    // TEST DATA — Users
    // USER_EXCEPTION = Jaydeep Kar  → Support/Exception (Days 1–10 window)
    // USER_SUPPORT   = Nidhi Chaturvedi → Regular Support (Days 1–5 window)
    // ═══════════════════════════════════════════════
    private static final String USER_EXCEPTION = "Jaydeep Kar";
    private static final String USER_SUPPORT = "Nidhi Chaturvedi";

    private static final String SERVICE_TYPE = "Program Change";
    private static final String DATE_1ST_OF_MONTH = "2026-07-01";
    private static final String DATE_5TH_OF_MONTH = "2026-07-05";
    private static final String DATE_8TH_OF_MONTH = "2026-07-08";
    private static final String DATE_10TH_OF_MONTH = "2026-07-10";
    private static final String DATE_PAST = "2026-04-01"; // backdated

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

        // Start with Nidhi — she covers SC001 and SC002 (priorities 1–5).
        // Jaydeep takes over from SC003_TC001 (priority 6) onward.
        String nidhiUser = getUserForScreen("Program Change");
        Assert.assertFalse(nidhiUser.isEmpty(),
                "❌ No user found for 'Program Change' in Excel");

        navigations.goToUserRights();
        userRightsPage.switchUser(nidhiUser);
        System.out.println("✅ Switched to: " + nidhiUser);
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — navigate to Account Statement
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        // Safety: dismiss any lingering JS alert before navigation
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Lingering alert dismissed in @BeforeMethod");
        } catch (Exception ignored) {
        }

        // Safety: close service panel if still open (blocks menu click)
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
    // AFTER METHOD — close modal/panel
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void closeAfterTest() {
        // Step 1: Dismiss any open JS confirm() alert (prevents UnhandledAlertException)
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @AfterMethod");
        } catch (Exception ignored) {
        }

        // Step 2: Close the inline service panel
        try {
            serviceRequestPage.closeModalByJs();
        } catch (Exception ignored) {
        }

        // Step 3: Wait for panel to fully close before next test
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════
    // HELPER — open Service Request panel and load
    //          Program Change form for a given child
    // ═══════════════════════════════════════════════
    private void openProgramChangeForm(String childId)
            throws InterruptedException {
        accountStatementPage.generateAccountStatement(childId);
        Assert.assertTrue(accountStatementPage.isAccountSummaryVisible(),
                "❌ Account summary not visible for child: " + childId);
        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(),
                "❌ Service Request panel did not open");
        serviceRequestPage.selectServiceType(SERVICE_TYPE);
        Assert.assertTrue(serviceRequestPage.isProgramChangeFormVisible(),
                "❌ Program Change form did not load");
    }

    // ═══════════════════════════════════════════════
    // HELPER — switch to a named user mid-test
    // Navigates to User Rights, switches user,
    // then navigates back to Account Statement.
    // Call this at the START of any test that
    // requires a specific user role.
    // ═══════════════════════════════════════════════

    /**
     * Switches to a named user mid-test via User Rights page.
     * <p>
     * Handles all post-switch popups that appear after every user switch:
     * 1. Pending Tasks dialog  ("Remind Me Later")
     * 2. Policy notification bells  (same flow as @BeforeClass login)
     * 3. Notification dropdown  (blocks menu clicks if left open)
     * <p>
     * After switching, navigates to Account Statement so the next
     * openProgramChangeForm() call works without extra navigation.
     *
     * @param userName exact name as shown in User Rights search
     *                 e.g. "Jaydeep Kar" or "Nidhi Chaturvedi"
     */
    private void switchToUser(String userName)
            throws InterruptedException {
        System.out.println("▶ Switching to user: " + userName);

        // Step 1: Safety — dismiss any open JS alert
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }

        // Step 2: Close service panel if still open (prevents menu click block)
        try {
            serviceRequestPage.closeModalByJs();
            Thread.sleep(300);
        } catch (Exception ignored) {
        }

        // Step 3: If currently switched to a non-main user, click profile icon
        // then "Back to Main User" to restore Rakesh's session (Settings menu needed)
        try {
            org.openqa.selenium.WebElement profileIcon =
                    new org.openqa.selenium.support.ui.WebDriverWait(
                            driver, java.time.Duration.ofSeconds(5))
                            .until(org.openqa.selenium.support.ui.ExpectedConditions
                                    .elementToBeClickable(org.openqa.selenium.By.xpath(
                                            "//a[contains(@class,'dropdown-toggle') and .//i[contains(@class,'icon-user')]]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", profileIcon);
            Thread.sleep(500);

            org.openqa.selenium.WebElement backToMain =
                    new org.openqa.selenium.support.ui.WebDriverWait(
                            driver, java.time.Duration.ofSeconds(5))
                            .until(org.openqa.selenium.support.ui.ExpectedConditions
                                    .elementToBeClickable(org.openqa.selenium.By.xpath(
                                            "//a[.//i[contains(@class,'icon-exit2')]]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", backToMain);
            System.out.println("✅ Back to Main User clicked — returning to Rakesh");
            Thread.sleep(2000);
            acknowledgePolicyNotificationIfPresent();
            closeNotificationDropdownIfOpen();
        } catch (Exception e) {
            System.out.println("▶ Already on main user — skipping Back to Main User");
        }

        // Step 4: Navigate to User Rights (now as Rakesh, Settings menu is available)
        navigations.goToUserRights();
        userRightsPage.switchUser(userName);
        System.out.println("✅ Switched to: " + userName);
        Thread.sleep(2000);

        // Step 4: Handle Pending Tasks dialog if it appears after switch
        // (same dialog dismissed in your existing BaseTest login flow)
        try {
            org.openqa.selenium.support.ui.WebDriverWait shortWait =
                    new org.openqa.selenium.support.ui.WebDriverWait(
                            driver, java.time.Duration.ofSeconds(5));
            org.openqa.selenium.WebElement remindBtn = shortWait.until(
                    org.openqa.selenium.support.ui.ExpectedConditions
                            .elementToBeClickable(org.openqa.selenium.By.xpath(
                                    "//*[contains(text(),'Remind Me Later') or " +
                                            "contains(text(),'remind me later')]")));
            remindBtn.click();
            System.out.println("✅ Pending Tasks dismissed (Remind Me Later)");
            Thread.sleep(500);
        } catch (Exception ignored) {
            System.out.println("▶ No Pending Tasks dialog");
        }

        // Step 5: Handle policy notification bells (same as BaseTest login)
        // Reuses the method already defined in BaseTest
        acknowledgePolicyNotificationIfPresent();

        // Step 6: Close notification dropdown (prevents menu interception)
        closeNotificationDropdownIfOpen();

        // Step 7: Navigate to Account Statement ready for the test
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement (as " + userName + ")");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC001 — Parent Submission Window (Days 1–5 only)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC001_TC001 — Parent can submit within Day 1–5 window.
     * Run this test on a date between the 1st and 5th of the month.
     */
    @Test(priority = 1,
            description = "SC001_TC001 — Parent: Days 1–5 enabled, Days 6+ disabled in calendar")
    public void sc001_tc001_parentWindowDays1To5()
            throws Exception {
        Reporter.log("▶ SC001_TC001 — Parent calendar: Days 1–5 enabled, Days 6+ disabled", true);
        Reporter.log("   ℹ Run this test on a date between the 1st and 5th of the month", true);
        // Running as Nidhi Chaturvedi — switched once in @BeforeClass
        openProgramChangeForm(REGULAR_CHILD_ID);

        // Open calendar and inspect day availability
        serviceRequestPage.openCalendarFor(serviceRequestPage.pc_effectiveDate);
        String monthYear = serviceRequestPage.getCalendarMonthYear();
        Reporter.log("   Calendar shows: " + monthYear, true);

        // Days 1–5 must be enabled (infocus, no aria-disabled)
        for (int day = 1; day <= 5; day++) {
            boolean enabled = serviceRequestPage.isDayEnabledInCurrentMonth(day);
            Reporter.log("   Day " + day + " enabled=" + enabled, true);
            Assert.assertTrue(enabled,
                    "❌ Day " + day + " should be ENABLED for Parent. Calendar: " + monthYear);
        }

        // Days 6–10 must be disabled for Parent role (infocus + aria-disabled=true)
        for (int day = 6; day <= 10; day++) {
            boolean disabled = serviceRequestPage.isDayDisabledInCurrentMonth(day);
            Reporter.log("   Day " + day + " disabled=" + disabled, true);
            Assert.assertTrue(disabled,
                    "❌ Day " + day + " should be DISABLED for Parent. Calendar: " + monthYear);
        }

        // Boundary: last enabled day must be exactly 5
        int lastEnabled = serviceRequestPage.getLastEnabledDayInCurrentMonth();
        Reporter.log("   Last enabled day in current month: " + lastEnabled, true);
        Assert.assertEquals(lastEnabled, 5,
                "❌ Parent window should end on Day 5. Last enabled: " + lastEnabled);

        serviceRequestPage.closeCalendar();
        Reporter.log("✅ SC001_TC001 PASSED — Parent calendar correctly shows Days 1–5 only", true);
    }

    /**
     * SC001_TC002 — Parent blocked from submitting after Day 5.
     * Run on a date after the 5th of the month.
     */
    @Test(priority = 2,
            description = "SC001_TC002 — Parent: Days 6+ disabled, only next month 1st available")
    public void sc001_tc002_parentBlockedAfterDay5()
            throws Exception {
        Reporter.log("▶ SC001_TC002 — Parent calendar: Days 6+ disabled after window", true);
        Reporter.log("   ℹ Run this test on a date after the 5th of the month", true);
        // Running as Nidhi Chaturvedi — switched once in @BeforeClass
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.openCalendarFor(serviceRequestPage.pc_effectiveDate);
        String monthYear = serviceRequestPage.getCalendarMonthYear();
        Reporter.log("   Calendar shows: " + monthYear, true);

        // After Day 5: current month days 6–31 must all be disabled (aria-disabled=true)
        java.util.List<Integer> disabledDays = serviceRequestPage.getAllDisabledDaysInCurrentMonth();
        java.util.List<Integer> enabledDays = serviceRequestPage.getAllEnabledDaysInCurrentMonth();
        Reporter.log("   Disabled current-month days: " + disabledDays, true);
        Reporter.log("   Enabled current-month days : " + enabledDays, true);

        // No current-month days (6–31) should be enabled after the window
        for (int day = 6; day <= 31; day++) {
            if (serviceRequestPage.isDayEnabledInCurrentMonth(day)) {
                Assert.fail("❌ Day " + day
                        + " should be DISABLED for Parent after Day 5. Calendar: " + monthYear);
            }
        }

        // Next month 1st is an outfocus cell with NO aria-disabled — check directly
        // (Do NOT navigate — next month's 1st is already visible as outfocus in current view)
        boolean nextMonth1stEnabled = serviceRequestPage.isNextMonthDayEnabled(1);
        Reporter.log("   Next month 1st (outfocus) enabled=" + nextMonth1stEnabled, true);
        Assert.assertTrue(nextMonth1stEnabled,
                "❌ Next month 1st should be ENABLED for Parent (shown as outfocus cell).");
        String nextMonthYear = "(outfocus cell in current calendar view)";

        serviceRequestPage.closeCalendar();
        Reporter.log("✅ SC001_TC002 PASSED — Parent blocked after Day 5, next month 1st available", true);
    }

    /**
     * SC001_TC003 — Parent cannot backdate (past dates blocked).
     */
    @Test(priority = 3,
            description = "SC001_TC003 — Parent: past month dates disabled in calendar")
    public void sc001_tc003_parentCannotBackdate()
            throws Exception {
        Reporter.log("▶ SC001_TC003 — Parent calendar: past month dates disabled", true);
        // Running as Nidhi Chaturvedi — switched once in @BeforeClass
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.openCalendarFor(serviceRequestPage.pc_effectiveDate);
        String currentMonthYear = serviceRequestPage.getCalendarMonthYear();
        Reporter.log("   Current calendar: " + currentMonthYear, true);

        // Verify prev-month navigation arrow is disabled (no going back)
        java.util.List<org.openqa.selenium.WebElement> prevNavDisabled =
                driver.findElements(
                        By.cssSelector(".picker__nav--prev.picker__nav--disabled"));
        Assert.assertFalse(prevNavDisabled.isEmpty(),
                "❌ Previous month nav should be DISABLED for Parent (no backdating)." +
                        " Calendar: " + currentMonthYear);
        Reporter.log("   ✅ Previous month navigation arrow is correctly disabled", true);

        // Verify all prev-month outfocus cells (Jun 28-30) have aria-disabled="true"
        java.util.List<org.openqa.selenium.WebElement> prevMonthCells =
                driver.findElements(By.cssSelector(".picker__day--outfocus"));
        int prevMonthCellCount = 0;
        for (org.openqa.selenium.WebElement cell : prevMonthCells) {
            String dayTxt = cell.getText().trim();
            String ariaDisabled = cell.getAttribute("aria-disabled");
            Reporter.log("   Outfocus day " + dayTxt + " aria-disabled=" + ariaDisabled, true);
            // All previous-month outfocus cells must be disabled
            // (Note: next month's 1st outfocus cell is enabled - skip day "1" here
            //  since we cannot distinguish prev vs next month outfocus by day number alone)
            if (!dayTxt.equals("1")) {
                Assert.assertEquals("true", ariaDisabled,
                        "❌ Outfocus day " + dayTxt + " should have aria-disabled=true");
                prevMonthCellCount++;
            }
        }
        Reporter.log("   ✅ " + prevMonthCellCount + " outfocus cells verified as disabled", true);

        serviceRequestPage.closeCalendar();
        Reporter.log("✅ SC001_TC003 PASSED — Parent cannot navigate to or select past dates", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC002 — Support General User (Days 1–5 window)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC002_TC001 — Support user: Day 1–5 with backdate to 1st allowed.
     * Run within Day 1–5 with Override_Customer_Request_Policies user.
     */
    @Test(priority = 4,
            description = "SC002_TC001 — Support user: Day 1–5 with backdate to 1st")
    public void sc002_tc001_supportWindowDays1To5WithBackdate()
            throws Exception {
        Reporter.log("▶ SC002_TC001 — Support user Day 1–5, backdate to 1st", true);
        // Running as Nidhi Chaturvedi — switched once in @BeforeClass
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_1ST_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success")
                        || msg.toLowerCase().contains("request"),
                "❌ Support backdate to 1st should be allowed. Got: " + msg);

        Reporter.log("✅ SC002_TC001 PASSED — Support can set effective date to 1st", true);
    }

    /**
     * SC002_TC002 — Support general user blocked after Day 5.
     * Run on a date after the 5th with a non-exception support user.
     */
    @Test(priority = 5,
            description = "SC002_TC002 — Support general: Days 6+ disabled in calendar after Day 5")
    public void sc002_tc002_supportBlockedAfterDay5()
            throws Exception {
        Reporter.log("▶ SC002_TC002 — Support calendar: Days 6+ disabled after Day 5 window", true);
        Reporter.log("   ℹ Run on a date after the 5th with non-exception support user", true);
        // Running as Nidhi Chaturvedi — switched once in @BeforeClass
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.openCalendarFor(serviceRequestPage.pc_effectiveDate);
        String monthYear = serviceRequestPage.getCalendarMonthYear();
        Reporter.log("   Calendar shows: " + monthYear, true);

        // Days 6–31 must be disabled for general Support after Day 5
        java.util.List<Integer> enabledDays = serviceRequestPage.getAllEnabledDaysInCurrentMonth();
        Reporter.log("   Enabled current-month days: " + enabledDays, true);

        for (int day = 6; day <= 31; day++) {
            if (serviceRequestPage.isDayEnabledInCurrentMonth(day)) {
                Assert.fail("❌ Day " + day
                        + " should be DISABLED for Support (non-exception) after Day 5."
                        + " Calendar: " + monthYear);
            }
        }

        // Max enabled day should be ≤ 5
        int lastEnabled = serviceRequestPage.getLastEnabledDayInCurrentMonth();
        Reporter.log("   Last enabled day: " + lastEnabled, true);
        Assert.assertTrue(lastEnabled <= 5,
                "❌ Support general window should end at Day 5. Last enabled: " + lastEnabled);

        serviceRequestPage.closeCalendar();
        Reporter.log("✅ SC002_TC002 PASSED — Support blocked after Day 5 (calendar verified)", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC003 — Exception User (Days 1–10 window)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC003_TC001 — Exception user (Parent Support Head): Day 1–10 window.
     * Run between 6th–10th with Parent Support Head user.
     */
    @Test(priority = 6,
            description = "SC003_TC001 — Exception user: Day 1–10 window with backdate")
    public void sc003_tc001_exceptionUserDays1To10()
            throws Exception {
        Reporter.log("▶ SC003_TC001 — Exception user Day 1–10", true);

        // Single switch from Nidhi → Jaydeep here; SC003_TC003 and SC004_TC001
        // continue as Jaydeep without another switch.
        String jaydeepUser = getUserForScreen("Account Statement");
        Assert.assertFalse(jaydeepUser.isEmpty(), "❌ No user found for 'Account Statement' in Excel");
        Reporter.log("   Switching to exception user: " + jaydeepUser, true);
        switchToUser(jaydeepUser);
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_8TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success")
                        || msg.toLowerCase().contains("request"),
                "❌ Exception user should be allowed on Day 8. Got: " + msg);

        Reporter.log("✅ SC003_TC001 PASSED — Exception user allowed within Day 1–10", true);
    }

    /**
     * SC003_TC003 — After Day 10 only next month 1st selectable.
     * Run after the 10th of the month.
     */
    @Test(priority = 7,
            description = "SC003_TC003 — Exception user after Day 10: Days 11+ disabled, next month 1st enabled")
    public void sc003_tc003_afterDay10OnlyNextMonth1st()
            throws Exception {
        Reporter.log("▶ SC003_TC003 — Exception user calendar: after Day 10 window", true);
        Reporter.log("   ℹ Run after the 10th with exception user (Jaydeep — switched in SC003_TC001)", true);
        // Running as Jaydeep Kar — switched once in sc003_tc001_exceptionUserDays1To10
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.openCalendarFor(serviceRequestPage.pc_effectiveDate);
        String monthYear = serviceRequestPage.getCalendarMonthYear();
        Reporter.log("   Calendar shows: " + monthYear, true);

        // After Day 10: all current-month days must be disabled (infocus + aria-disabled=true)
        java.util.List<Integer> enabledDays = serviceRequestPage.getAllEnabledDaysInCurrentMonth();
        Reporter.log("   Enabled current-month days: " + enabledDays, true);

        Assert.assertTrue(enabledDays.isEmpty(),
                "❌ After Day 10, NO current-month days should be enabled for exception user."
                        + " Still enabled: " + enabledDays + " | Calendar: " + monthYear);

        // Next month 1st is visible as outfocus cell in current calendar view — check it directly
        // Aug 1 confirmed from DOM: picker__day--outfocus with NO aria-disabled
        boolean nextMonth1stEnabled = serviceRequestPage.isNextMonthDayEnabled(1);
        Reporter.log("   Next month 1st (outfocus) enabled=" + nextMonth1stEnabled, true);
        Assert.assertTrue(nextMonth1stEnabled,
                "❌ Next month 1st should be ENABLED for exception user (outfocus, no aria-disabled).");

        // Navigate to next month to check days 2–10 are disabled
        serviceRequestPage.calendarNextMonth();
        String nextMonthYear = serviceRequestPage.getCalendarMonthYear();
        Reporter.log("   Navigated to: " + nextMonthYear, true);

        // Days 2–10 of next month must be disabled
        for (int day = 2; day <= 10; day++) {
            boolean disabled = serviceRequestPage.isDayDisabledInCurrentMonth(day);
            Reporter.log("   " + nextMonthYear + " Day " + day + " disabled=" + disabled, true);
            Assert.assertTrue(disabled,
                    "❌ " + nextMonthYear + " Day " + day + " should be DISABLED. Got enabled.");
        }

        serviceRequestPage.closeCalendar();
        Reporter.log("✅ SC003_TC003 PASSED — only next month 1st available after Day 10 (calendar verified)", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC004 — Upgrade Scenarios
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC004_TC001 — Upgrade: Half Day → Full Day, effective 5th.
     * Child #66289 is currently on Half Day program.
     */
    @Test(priority = 8,
            description = "SC004_TC001 — Upgrade: Half Day to Full Day, effective 5th")
    public void sc004_tc001_upgradeHalfToFullDay()
            throws Exception {
        Reporter.log("▶ SC004_TC001 — Upgrade Half Day → Full Day", true);
        // Running as Jaydeep Kar — switched once in sc003_tc001_exceptionUserDays1To10
        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);

        // Verify selection before submit
        Assert.assertEquals(serviceRequestPage.getSelectedProgram(),
                PROG_FULL_DAY,
                "❌ New Program selection mismatch before submit");

        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success")
                        || msg.toLowerCase().contains("request"),
                "❌ Upgrade Half → Full Day failed. Got: " + msg);

        Reporter.log("✅ SC004_TC001 PASSED — Upgrade Half Day → Full Day submitted", true);
        Reporter.log("   ℹ Backend: verify 2 prorated invoices with program names in line items", true);
    }

    /**
     * SC004_TC002 — Upgrade: Effective 1st — single full-month invoice (no proration).
     */
    @Test(priority = 9,
            description = "SC004_TC002 — Upgrade effective 1st: single full-month invoice")
    public void sc004_tc002_upgradeEffective1stNoProration()
            throws InterruptedException {
        Reporter.log("▶ SC004_TC002 — Upgrade effective 1st, no proration", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_1ST_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success")
                        || msg.toLowerCase().contains("request"),
                "❌ Upgrade effective 1st failed. Got: " + msg);

        Reporter.log("✅ SC004_TC002 PASSED — Upgrade effective 1st submitted", true);
        Reporter.log("   ℹ Backend: verify single full-month invoice, no split", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC005 — Downgrade Scenarios
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC005_TC001 — Downgrade: Full Day → Extended Preschool, effective 10th.
     * Expect 2 prorated in
     * voices, original voided.
     */
    @Test(priority = 10,
            description = "SC005_TC001 — Downgrade: Full Day to Extended Preschool, effective 10th")
    public void sc005_tc001_downgradeFullToExtendedPreschool()
            throws InterruptedException {
        Reporter.log("▶ SC005_TC001 — Downgrade Full Day → Extended Preschool", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_10TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_EXT_PRESCHOOL);

        Assert.assertEquals(serviceRequestPage.getSelectedProgram(),
                PROG_EXT_PRESCHOOL,
                "❌ New Program selection mismatch before submit");

        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success")
                        || msg.toLowerCase().contains("request"),
                "❌ Downgrade Full → Extended Preschool failed. Got: " + msg);

        Reporter.log("✅ SC005_TC001 PASSED — Downgrade submitted", true);
        Reporter.log("   ℹ Backend: verify original invoice voided, 2 prorated invoices created", true);
    }

    /**
     * SC005_TC002 — Downgrade: Effective 1st — single full-month invoice.
     */
    @Test(priority = 11,
            description = "SC005_TC002 — Downgrade effective 1st: single full-month invoice")
    public void sc005_tc002_downgradeEffective1st()
            throws InterruptedException {
        Reporter.log("▶ SC005_TC002 — Downgrade effective 1st, no split", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_1ST_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_AFTER_SCHOOL);

        Assert.assertEquals(serviceRequestPage.getSelectedProgram(),
                PROG_AFTER_SCHOOL,
                "❌ New Program selection mismatch before submit");

        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertTrue(
                msg.toLowerCase().contains("success")
                        || msg.toLowerCase().contains("request"),
                "❌ Downgrade effective 1st failed. Got: " + msg);

        Reporter.log("✅ SC005_TC002 PASSED — Downgrade effective 1st submitted", true);
        Reporter.log("   ℹ Backend: verify single full-month invoice at new rate, no split", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC007 — Time-slot Only Change
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC007_TC001 — Time-slot only change: same plan name, different slot.
     * No invoice action expected on backend.
     * NOTE: This test validates the UI submission only.
     * Backend: verify no new invoice is created.
     */
    @Test(priority = 12,
            description = "SC007_TC001 — Time-slot only change: no invoice action")
    public void sc007_tc001_timeSlotOnlyChange()
            throws InterruptedException {
        Reporter.log("▶ SC007_TC001 — Time-slot only program change", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);

        // Select a different time-slot of the same program type
        // Full Day has different slot options — select whichever is available
        // If only one Full Day option exists, use Extended Preschool as same-rate slot
        serviceRequestPage.selectPCNewProgram(PROG_EXT_PRESCHOOL);

        Assert.assertEquals(serviceRequestPage.getSelectedProgram(),
                PROG_EXT_PRESCHOOL,
                "❌ Time-slot program selection mismatch");

        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Reporter.log("   ℹ Backend: if same plan name → no invoice regeneration expected", true);

        Reporter.log("✅ SC007_TC001 — Form submitted; backend: verify no new invoice for same-name program", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC009 — Child Status Validation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC009_TC001 — Paused child: Program Change option should be blocked.
     */
    @Test(priority = 13,
            description = "SC009_TC001 — Paused child cannot submit Program Change")
    public void sc009_tc001_pausedChildCannotSubmit()
            throws InterruptedException {
        Reporter.log("▶ SC009_TC001 — Paused child blocked from Program Change", true);

        accountStatementPage.generateAccountStatement(PAUSED_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isAccountSummaryVisible(),
                "❌ Account summary not visible for paused child: " + PAUSED_CHILD_ID);

        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(),
                "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType(SERVICE_TYPE);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Paused child should be blocked from Program Change. Got: " + msg);

        Reporter.log("✅ SC009_TC001 PASSED — Paused child blocked", true);
    }

    /**
     * SC009_TC002 — Inactive/Suspended child: Program Change blocked.
     */
    @Test(priority = 14,
            description = "SC009_TC002 — Inactive child cannot submit Program Change")
    public void sc009_tc002_inactiveChildCannotSubmit()
            throws InterruptedException {
        Reporter.log("▶ SC009_TC002 — Inactive child blocked from Program Change", true);

        accountStatementPage.generateAccountStatement(INACTIVE_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isAccountSummaryVisible(),
                "❌ Account summary not visible for inactive child: " + INACTIVE_CHILD_ID);

        serviceRequestPage.clickServiceRequestLink();
        Assert.assertTrue(serviceRequestPage.isModalVisible(),
                "❌ Service Request panel did not open");

        serviceRequestPage.selectServiceType(SERVICE_TYPE);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Inactive child should be blocked. Got: " + msg);

        Reporter.log("✅ SC009_TC002 PASSED — Inactive child blocked", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC012 — Negative / Edge Cases
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC012_TC001 — Same From and New Program: submission rejected.
     * Selects the first available option which matches the current program.
     */
    @Test(priority = 15,
            description = "SC012_TC001 — Same From Program and New Program: submission rejected")
    public void sc012_tc001_sameProgramRejected()
            throws InterruptedException {
        Reporter.log("▶ SC012_TC001 — Same From Program = New Program", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);

        // Get current program shown in dropdown (pre-selected default)
        String currentProgram = serviceRequestPage.getSelectedProgram();
        Reporter.log("   Current program in dropdown: " + currentProgram, true);

        // If dropdown defaults to --Select--, pick the same program child is enrolled in
        // Child #66289 is on Half Day — Extended Preschool is the equivalent same-type
        // Adjust this value to match the child's current enrolled program exactly
        String sameProgram = currentProgram.isEmpty()
                || currentProgram.equals(PROG_SELECT)
                ? PROG_EXT_PRESCHOOL   // same as child's current plan
                : currentProgram;

        serviceRequestPage.selectPCNewProgram(sameProgram);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Same program change should be rejected. Got: " + msg);

        Reporter.log("✅ SC012_TC001 PASSED — same program rejected", true);
    }

    /**
     * SC012_TC002 — No New Program selected (--Select-- left): mandatory validation.
     */
    @Test(priority = 16,
            description = "SC012_TC002 — No New Program selected: mandatory validation")
    public void sc012_tc002_newProgramNotSelected()
            throws InterruptedException {
        Reporter.log("▶ SC012_TC002 — Submit without selecting New Program", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);
        // Intentionally leave New Program as --Select--
        serviceRequestPage.submitProgramChange();

        // If a JS confirm() pops up here, it's a bug — form should not proceed
        if (serviceRequestPage.isAlertPresent()) {
            String alertText = serviceRequestPage.getAlertText();
            Reporter.log("   Unexpected alert: " + alertText, true);
            serviceRequestPage.dismissAlert();
            Assert.fail("❌ Confirmation popup shown without New Program selected — validation missing");
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Missing New Program should fail validation. Got: " + msg);

        Reporter.log("✅ SC012_TC002 PASSED — missing New Program blocked", true);
    }

    /**
     * SC012_TC003 — Cancel on JS confirm() popup: form stays open, no request created.
     */
    @Test(priority = 17,
            description = "SC012_TC003 — Cancel on popup: no submission")
    public void sc012_tc003_cancelOnConfirmationPopup()
            throws InterruptedException {
        Reporter.log("▶ SC012_TC003 — Cancel on Program Change confirmation popup", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange(); // triggers JS confirm()

        Assert.assertTrue(serviceRequestPage.isAlertPresent(),
                "❌ Confirmation popup not shown after submit");
        Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
        serviceRequestPage.dismissAlert(); // click Cancel

        // Form should still be visible — no navigation / success message
        Assert.assertTrue(serviceRequestPage.isProgramChangeFormVisible(),
                "❌ Program Change form should still be visible after Cancel");

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response after cancel: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ No success message expected after Cancel. Got: " + msg);

        Reporter.log("✅ SC012_TC003 PASSED — Cancel keeps form open, no submission", true);
    }

    /**
     * SC012_TC004 — Child with no invoice: program updated without invoice action.
     */
    @Test(priority = 18,
            description = "SC012_TC004 — No invoice for child: program updated without invoice action")
    public void sc012_tc004_noInvoiceChildProgramUpdated()
            throws InterruptedException {
        Reporter.log("▶ SC012_TC004 — Child with no invoice", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_5TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Reporter.log("   ℹ Backend: if no invoice exists, invoice block skipped, program updated directly", true);

        Reporter.log("✅ SC012_TC004 — Submitted; backend: verify no invoice error when no invoice exists", true);
    }

    /**
     * SC012_TC006 — Parent UI: no backdate or 10-day exception window accessible.
     */
    @Test(priority = 19,
            description = "SC012_TC006 — Parent UI has no backdate or 10-day exception window")
    public void sc012_tc006_parentUINoBackdateOrExceptionWindow()
            throws InterruptedException {
        Reporter.log("▶ SC012_TC006 — Parent UI date restriction", true);

        openProgramChangeForm(REGULAR_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_PAST); // April 1 — past
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);
        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.dismissAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Assert.assertFalse(
                msg.toLowerCase().contains("success"),
                "❌ Backdate should not be allowed for Parent. Got: " + msg);

        Reporter.log("✅ SC012_TC006 PASSED — Parent backdate blocked", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SC013 — Corporate Tie-up
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * SC013_TC003 — Corporate Tie-up: prorated invoice uses tie-up amounts.
     */
    @Test(priority = 20,
            description = "SC013_TC003 — Corporate Tie-up: prorated invoice uses tie-up rates")
    public void sc013_tc003_corporateTieupProratedInvoice()
            throws InterruptedException {
        Reporter.log("▶ SC013_TC003 — Corporate Tie-up Program Change", true);

        openProgramChangeForm(CORPORATE_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_8TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_FULL_DAY);

        Assert.assertEquals(serviceRequestPage.getSelectedProgram(),
                PROG_FULL_DAY,
                "❌ Corporate child New Program selection mismatch");

        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Reporter.log("   ℹ Backend: prorated invoice must use tie-up plan amounts, not standard rates", true);

        Reporter.log("✅ SC013_TC003 — Submitted; backend: verify tie-up rate on prorated invoices", true);
    }

    /**
     * SC013_TC004 — Corporate Tie-up: credit source guard NOT applicable.
     */
    @Test(priority = 21,
            description = "SC013_TC004 — Corporate Tie-up: credit source guard not applicable")
    public void sc013_tc004_corporateTieupCreditSourceGuardNotApplicable()
            throws InterruptedException {
        Reporter.log("▶ SC013_TC004 — Corporate Tie-up: credit source guard skipped", true);

        openProgramChangeForm(CORPORATE_CHILD_ID);
        serviceRequestPage.setPCEffectiveDate(DATE_10TH_OF_MONTH);
        serviceRequestPage.selectPCNewProgram(PROG_EXT_PRESCHOOL); // downgrade

        Assert.assertEquals(serviceRequestPage.getSelectedProgram(),
                PROG_EXT_PRESCHOOL,
                "❌ Corporate child downgrade selection mismatch");

        serviceRequestPage.submitProgramChange();

        if (serviceRequestPage.isAlertPresent()) {
            Reporter.log("   Alert: " + serviceRequestPage.getAlertText(), true);
            serviceRequestPage.acceptAlert();
        }

        String msg = serviceRequestPage.getResponseMessage();
        Reporter.log("   Response: " + msg, true);
        Reporter.log("   ℹ Backend: credit source guard applies only to regular admissions, not corporate", true);

        Reporter.log("✅ SC013_TC004 — Submitted; backend: verify no credit source block for corporate child", true);
    }
}
