package testScripts.SupportTests.FinancialReports;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import pages.Support.FinancialReports.AccountStatementPage;
import utils.BaseTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Account Statement (Support → Financial Reports → Account Statement) — redesigned UI.
 * Source: TC_Account Statement Scripts.xlsx, sheet "Account Statement Page" (SC_001–SC_010, SC_021),
 * updated to the redesigned screen (2026-11-19) plus new cases for what the redesign added.
 * Runs as the "Account Statement" user from input_UserRights.xlsx (Jaydeep Kar).
 * Read-only except SC_005_TC_003 (adds one Diary Note per run) — user-approved 2026-11-19.
 */
public class AccountStatement_Testcases extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA (checked live 2026-11-19; override with -D…)
    // ═══════════════════════════════════════════════
    private static final String REGULAR_CHILD_ID = System.getProperty("asRegularChild", "49149");   // Dhruvam Sen Verma — Active, Regular, V1, Extended Preschool
    private static final String REGULAR_CHILD_NAME = System.getProperty("asRegularChildName", "Dhruvam Sen Verma");
    private static final String CORPORATE_CHILD_ID = System.getProperty("asCorporateChild", "49190"); // Atharva Pratap Singh — Corporate (Inhouse Admissions), Co-Pay, V2
    private static final String COPAY_CHILD_ID = System.getProperty("asCoPayChild", "49190");         // same child: shows CO-PAY CHILD PLAN UPDATE
    private static final String ATTRITION_CHILD_ID = System.getProperty("asAttritionChild", "49194"); // Miral Aris — ATTRITION since the DB sync
    // Admission types (user, 2026-11-19). Emergency Daycare: pass -DasEmergencyChild=<id> when available.
    private static final String AT_REGULAR = System.getProperty("asTypeRegular", "53113");      // Kartik Katyal — V1
    private static final String AT_CORPORATE = System.getProperty("asTypeCorporate", "75508");  // Niranjan Kuruvath — Wipro Offsite Centers, V2
    private static final String AT_SUMMER_CAMP = System.getProperty("asTypeSummerCamp", "76191"); // Summer Camp — no payment plan
    private static final String AT_EMERGENCY = System.getProperty("asEmergencyChild", "53394"); // Aira Bhardwaj — Emergency Daycare (not active)
    private static final String HISTORY_CHILD_ID = System.getProperty("asHistoryChild", "70801");     // has Child History entries

    private static final List<String> HEADER_ACTIONS = Arrays.asList(
            "Add Charges", "Child Plan", "Center Plan", "Diary Notes", "Child History",
            "Child Info", "Service Request", "Customer Portal", "Add Credits", "Customer Requests");
    private static final List<String> SUMMARY_LABELS = Arrays.asList(
            "Invoice Raised", "Invoice Voided", "Credits Issued", "Referral Credits Issued",
            "Promotional Credits Issued", "Promotional Credits Adjusted", "Discount Issued",
            "Security Deposit", "Refund", "Payment Received", "Credit Balance", "Dues", "Account Balance");
    private static final List<String> TRANSACTION_HEADERS = Arrays.asList(
            "Reference (Invoice/Credit/Refund)", "Booking Head", "Booking Comments", "Amount", "Credit", "Debit");

    AccountStatementPage accountStatementPage;
    UserRightsPage userRightsPage;
    Navigations navigations;
    String actingUser;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        accountStatementPage = new AccountStatementPage(driver);

        actingUser = getUserForScreen("Account Statement");
        Assert.assertFalse(actingUser.isEmpty(), "❌ No user found for Account Statement in Excel");
        navigations.goToUserRights();
        userRightsPage.switchUser(actingUser);
        System.out.println("✅ Switched to: " + actingUser);
        Thread.sleep(2000);
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        accountStatementPage.closeModalByJs();
        navigations.goToAccountStatement();
        accountStatementPage.hideApiDebugPanel();
        System.out.println("▶ Ready: Account Statement");
    }

    @AfterMethod(alwaysRun = true)
    public void closeModalsAfterTest() {
        try {
            closeExtraTabs();
            accountStatementPage.closeModalByJs();
        } catch (Exception ignored) {
        }
    }

    // ─── helpers ───

    private void log(String msg) {
        Reporter.log(msg, true);
    }

    private void open(String childId) throws InterruptedException {
        accountStatementPage.openAccountStatementFor(childId);
        Assert.assertTrue(accountStatementPage.getStatementHeaderText().contains("#" + childId),
                "❌ Account Statement did not load for #" + childId + ": " + accountStatementPage.getStatementHeaderText());
    }

    private static String norm(String s) {
        return s.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private void assertContainsAll(String where, String text, List<String> expected) {
        List<String> missing = new ArrayList<>();
        for (String e : expected) if (!text.toLowerCase().contains(e.toLowerCase())) missing.add(e);
        Assert.assertTrue(missing.isEmpty(), "❌ " + where + " is missing " + missing + "\n--- text ---\n" + text);
    }

    private void openPopup(String hrefPart, String titlePart) throws InterruptedException {
        accountStatementPage.clickHeaderLink(hrefPart);
        Assert.assertTrue(accountStatementPage.waitForPopupTitle(titlePart, 15),
                "❌ Popup '" + titlePart + "' did not open (title now: '" + accountStatementPage.getPopupTitle() + "')");
        log("   ✅ Popup opened: " + accountStatementPage.getPopupTitle());
    }

    private String switchToNewTab(int tabsBefore) throws InterruptedException {
        long end = System.currentTimeMillis() + 15000;
        while (driver.getWindowHandles().size() <= tabsBefore && System.currentTimeMillis() < end) Thread.sleep(300);
        Assert.assertTrue(driver.getWindowHandles().size() > tabsBefore, "❌ No new tab opened");
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(handles.get(handles.size() - 1));
        Thread.sleep(4000);
        return driver.getCurrentUrl();
    }

    private void closeExtraTabs() {
        List<String> handles = new ArrayList<>(driver.getWindowHandles());
        for (int i = handles.size() - 1; i > 0; i--) {
            driver.switchTo().window(handles.get(i));
            driver.close();
        }
        driver.switchTo().window(handles.get(0));
    }

    // ═══════════════════════════════════════════════
    // SC_001 — DEFAULT VIEW / GENERATE
    // ═══════════════════════════════════════════════

    @Test(priority = 1, description = "SC_001_TC_001 — Default view of Account Statement screen (before Generate)")
    public void sc001_tc001_verifyDefaultView() {
        log("▶ SC_001_TC_001 — Default view");
        Assert.assertTrue(accountStatementPage.isPageLoaded(), "❌ Account Statement page did not load");
        Assert.assertEquals(accountStatementPage.getPageHeading(), "Account Statement", "❌ Page heading");
        Assert.assertEquals(accountStatementPage.getDurationSelections(), new String[]{"--Select--", "--Select--"},
                "❌ Duration From/To should default to --Select--");
        List<String> months = accountStatementPage.getDurationOptions(true);
        Assert.assertTrue(months.size() > 12 && months.get(1).matches("[A-Z][a-z]{2} \\d{4}"),
                "❌ Duration options should be months (e.g. 'Nov 2026'): " + months.subList(0, Math.min(3, months.size())));
        Assert.assertTrue(accountStatementPage.isGenerateButtonVisible(), "❌ Generate Report button missing");
        // Booking Head / Booking Comments / Amount share one header cell, so check the joined header text
        assertContainsAll("Transactions header", String.join(" | ", accountStatementPage.getTransactionHeaders()), TRANSACTION_HEADERS);
        Assert.assertTrue(accountStatementPage.isDefaultMessageVisible(),
                "❌ 'Account statement not retrieved for this child!' not shown");
        Assert.assertFalse(accountStatementPage.isPdfIconVisible(), "❌ PDF icon should appear only after Generate");
        log("✅ SC_001_TC_001 PASSED — Admission ID, Duration (" + months.get(1) + " …), Generate Report, 6 columns, empty message");
    }

    @Test(priority = 2, description = "SC_001_TC_002 — Generate for a Regular admission: header, actions, sections")
    public void sc001_tc002_generateRegularAdmission() throws InterruptedException {
        log("▶ SC_001_TC_002 — Regular admission " + REGULAR_CHILD_ID);
        accountStatementPage.enterAdmissionId(REGULAR_CHILD_ID);
        accountStatementPage.clickGenerate();
        accountStatementPage.hideApiDebugPanel();

        Assert.assertEquals(accountStatementPage.getPageHeading(), "Account Statement (#" + REGULAR_CHILD_ID + ")", "❌ Page heading");
        String header = accountStatementPage.getStatementHeaderText();
        log("   Header: " + header);
        assertContainsAll("Header", header, Arrays.asList("#" + REGULAR_CHILD_ID, REGULAR_CHILD_NAME.toUpperCase(), "(ACTIVE)", "[PAYMENT PLAN : V1]"));
        Assert.assertTrue(accountStatementPage.getStatementPeriod().endsWith("TO TILL DATE"),
                "❌ Default period should be '<joining month> TO TILL DATE': " + accountStatementPage.getStatementPeriod());

        Map<String, String> actions = accountStatementPage.getHeaderActionLinks();
        List<String> got = new ArrayList<>();
        for (String k : actions.keySet()) got.add(norm(k));
        for (String a : HEADER_ACTIONS) Assert.assertTrue(got.contains(norm(a)), "❌ Header action missing: " + a + " in " + actions.keySet());
        Assert.assertTrue(accountStatementPage.isMigrateRegularToCorporateVisible(), "❌ 'Migrate From Regular to Corporate' button missing");
        Assert.assertTrue(accountStatementPage.isPdfIconVisible(), "❌ PDF icon (Download Account Summary) missing");

        Assert.assertEquals(accountStatementPage.getSectionHeadings().subList(
                accountStatementPage.getSectionHeadings().size() - 3, accountStatementPage.getSectionHeadings().size()),
                Arrays.asList("Subscriptions", "Transactions", "Account Summary"), "❌ Sections");
        for (String panel : Arrays.asList("Monthly Subscription", "Yearly Subscription")) {
            assertContainsAll(panel, accountStatementPage.getSubscriptionPanelText(panel),
                    Arrays.asList("Plan Name", "Plan Amount", "Next Billing Date", "Coupons", "Addons"));
        }
        Assert.assertTrue(accountStatementPage.getTransactionRowCount() > 0, "❌ No transaction rows");
        assertContainsAll("Account Summary", accountStatementPage.getAccountSummaryText(), SUMMARY_LABELS);
        log("✅ SC_001_TC_002 PASSED — " + actions.size() + " actions, 3 sections, " + accountStatementPage.getTransactionRowCount() + " transaction rows");
    }

    @Test(priority = 3, description = "SC_001_TC_003 — Generate for a Corporate admission: tie-up and corporate actions")
    public void sc001_tc003_generateCorporateAdmission() throws InterruptedException {
        log("▶ SC_001_TC_003 — Corporate admission " + CORPORATE_CHILD_ID);
        open(CORPORATE_CHILD_ID);
        String header = accountStatementPage.getStatementHeaderText();
        log("   Header: " + header);
        Assert.assertTrue(header.contains("TIE-UP :"), "❌ Tie-up name missing in header");
        Assert.assertTrue(accountStatementPage.isMigrateCorporateToRegularVisible(), "❌ 'Migrate From Corporate to Regular' missing");
        Assert.assertFalse(accountStatementPage.isMigrateRegularToCorporateVisible(), "❌ 'Migrate From Regular to Corporate' must not show for a corporate child");
        Assert.assertTrue(accountStatementPage.isHeaderLinkVisible("javascript:addTieupProgramChange"), "❌ 'Tie Up Program Change' missing");
        Assert.assertTrue(accountStatementPage.isHeaderLinkVisible("pop_corporate_transfer"), "❌ 'Corporate Transfer' missing");
        Assert.assertTrue(accountStatementPage.isHeaderLinkVisible("pop_center_transfer"), "❌ 'Corporate Center Transfer' missing");
        Assert.assertTrue(accountStatementPage.getSectionHeadings().contains("Account Summary"), "❌ Account Summary section missing");
        assertContainsAll("Account Summary (corporate)", accountStatementPage.getAccountSummaryText(),
                Arrays.asList("Parent Account Summary", "Corporate Account Summary", "Invoice Raised", "Corporate Invoice Raised",
                        "Payment Received", "Corporate Payment Received"));
        log("✅ SC_001_TC_003 PASSED — Tie-up header, Migrate Corporate→Regular, Tie Up Program Change, Corporate (Center) Transfer");
    }

    @Test(priority = 4, description = "SC_001_TC_004 — Co-Pay admission: Co-Pay Child Plan Update popup (read-only)")
    public void sc001_tc004_generateCoPayAdmission() throws InterruptedException {
        log("▶ SC_001_TC_004 — Co-Pay admission " + COPAY_CHILD_ID);
        open(COPAY_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isCoPayChildPlanUpdateVisible(), "❌ 'Co-Pay Child Plan Update' missing");
        openPopup("copay_billing_update", "Co-Pay Plan Subscription Setup Update (#" + COPAY_CHILD_ID + ")");
        assertContainsAll("Co-Pay popup", accountStatementPage.getPopupText(), Arrays.asList(
                "Child Name", "Center Name", "Program", "Offer Title", "Payment Plan", "Breakup Details",
                "FOR PARENT PAYER", "FOR CORPORATE PAYER", "DISCOUNT", "Monthly Discount", "Refresh Data"));
        Assert.assertNotNull(accountStatementPage.findInPopup("button[id^='btn_submit']"), "❌ Update button missing");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Popup did not close");
        log("✅ SC_001_TC_004 PASSED — popup opened and closed, nothing updated");
    }

    @Test(priority = 5, description = "SC_001_TC_005 — PDF icon opens the account summary PDF in a new tab")
    public void sc001_tc005_verifyPdfDownload() throws InterruptedException {
        log("▶ SC_001_TC_005 — PDF");
        open(REGULAR_CHILD_ID);
        int tabs = driver.getWindowHandles().size();
        accountStatementPage.clickPdfDownload();
        String url = switchToNewTab(tabs);
        log("   PDF tab: " + url);
        Assert.assertTrue(url.contains("AccountStatement_" + REGULAR_CHILD_ID) && url.endsWith(".pdf"),
                "❌ Expected a PDF named AccountStatement_" + REGULAR_CHILD_ID + "_…pdf, got " + url);
        log("✅ SC_001_TC_005 PASSED");
    }

    @Test(priority = 6, description = "SC_001_TC_006 — Duration filter limits the statement to the chosen months")
    public void sc001_tc006_durationFilter() throws InterruptedException {
        log("▶ SC_001_TC_006 — Duration filter");
        List<String> months = accountStatementPage.getDurationOptions(true);
        String to = months.get(1), from = months.get(2);   // last month → current month
        accountStatementPage.generateAccountStatement(REGULAR_CHILD_ID, from, to);
        accountStatementPage.hideApiDebugPanel();
        String period = accountStatementPage.getStatementPeriod();
        log("   Period: " + period + " | Duration now: " + Arrays.toString(accountStatementPage.getDurationSelections()));
        String expFrom = from.substring(0, 3).toUpperCase() + "-" + from.substring(4);
        String expTo = to.substring(0, 3).toUpperCase() + "-" + to.substring(4);
        Assert.assertTrue(period.contains(expFrom) && period.contains(expTo),
                "❌ Header period should be " + expFrom + " TO " + expTo + ", got: " + period);
        log("✅ SC_001_TC_006 PASSED");
    }

    @Test(priority = 7, description = "SC_001_TC_007 — Each header action's tooltip names that action")
    public void sc001_tc007_actionTooltips() throws InterruptedException {
        log("▶ SC_001_TC_007 — Action tooltips");
        open(REGULAR_CHILD_ID);
        LinkedHashMap<String, String> actions = accountStatementPage.getHeaderActionLinks();
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, String> e : actions.entrySet()) {
            String title = e.getValue().trim();
            log("   " + e.getKey() + " → tooltip '" + title + "'");
            // a tooltip that names a DIFFERENT header action is wrong
            for (String other : HEADER_ACTIONS) {
                if (!norm(other).equals(norm(e.getKey())) && norm(title).equals(norm(other))) wrong.add(e.getKey() + " → '" + title + "'");
            }
        }
        Assert.assertTrue(wrong.isEmpty(), "❌ Tooltip names another action: " + wrong);
        log("✅ SC_001_TC_007 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_002 — ADD CHARGES / ADD CREDITS
    // ═══════════════════════════════════════════════

    @Test(priority = 8, description = "SC_002_TC_001 — Add Charges popup default view + empty Submit validation")
    public void sc002_tc001_verifyAddChargesModal() throws InterruptedException {
        log("▶ SC_002_TC_001 — Add Charges");
        open(REGULAR_CHILD_ID);
        openPopup("add_onetime_charge", "Apply One Time Charges");
        Assert.assertTrue(accountStatementPage.isAddChargesModalVisible(), "❌ Charge Type dropdown missing");
        assertContainsAll("Add Charges popup", accountStatementPage.getPopupText(), Arrays.asList(
                "Child ID", "Child Name", REGULAR_CHILD_NAME, "Charge Type", "Charge Amount", "Comment",
                "Same text will be sent in invoice to customer right away", "Submit form"));
        Assert.assertEquals(accountStatementPage.findInPopup("#child_id").getAttribute("value"), REGULAR_CHILD_ID, "❌ Child ID not prefilled");
        Assert.assertFalse(accountStatementPage.findInPopup("#child_id").isEnabled(), "❌ Child ID should be read-only");
        List<String> types = accountStatementPage.getPopupSelectOptions("#charge_type");
        assertContainsAll("Charge types", String.join("|", types), Arrays.asList("Book Set", "Tuition Fee", "Transport Fee", "Registration Fee"));
        accountStatementPage.clickInPopup("#apply_charge");
        String msg = accountStatementPage.getPopupValidationMessage();
        log("   Empty Submit → " + msg);
        Assert.assertTrue(msg.contains("Please specify amount"), "❌ Empty Submit message: '" + msg + "'");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Popup did not close");
        log("✅ SC_002_TC_001 PASSED — " + (types.size() - 1) + " charge types");
    }

    @Test(priority = 9, description = "SC_002_TC_002 — Add Credits popup default view + empty Submit validation")
    public void sc002_tc002_verifyAddCreditsModal() throws InterruptedException {
        log("▶ SC_002_TC_002 — Add Credits");
        open(REGULAR_CHILD_ID);
        openPopup("pop_issue_credits", "Add New Issue Credits/Refund");
        assertContainsAll("Add Credits popup", accountStatementPage.getPopupText().toUpperCase(), Arrays.asList(
                "CHILD ID", "CHILD NAME", "SELECT CREDITS", "SELECT INVOICE REFERENCE", "CREDIT AMOUNT",
                "CREDIT LINE ITEM", "COMMENTS", "SUBMIT FORM"));
        Assert.assertEquals(accountStatementPage.findInPopup("#child_id").getAttribute("value"), REGULAR_CHILD_ID, "❌ Child ID not prefilled");
        List<String> refs = accountStatementPage.getPopupSelectOptions("#credit_invoice_ref");
        Assert.assertTrue(refs.size() > 1, "❌ No invoice references for the child");
        accountStatementPage.clickInPopup("#apply_credit");
        String msg = accountStatementPage.getPopupValidationMessage();
        log("   Empty Submit → " + msg);
        Assert.assertEquals(msg, "Please select credit type!", "❌ Empty Submit message");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Popup did not close");
        log("✅ SC_002_TC_002 PASSED — " + (refs.size() - 1) + " invoice references");
    }

    // ═══════════════════════════════════════════════
    // SC_003 — CHILD PLAN (view-only since the redesign)
    // ═══════════════════════════════════════════════

    @Test(priority = 10, description = "SC_003_TC_001 — Child Plan popup default view")
    public void sc003_tc001_verifyChildPlanModalView() throws InterruptedException {
        log("▶ SC_003_TC_001 — Child Plan");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildPlan();
        Assert.assertTrue(accountStatementPage.isChildPlanModalVisible(), "❌ Child Plan popup did not open");
        String text = accountStatementPage.getPopupText();
        assertContainsAll("Child Plan popup", text.toUpperCase(), Arrays.asList(
                "CURRENT ADMISSION PLAN", "LONG TERM (V1)", "PROGRAM", "LONG TERM FEE (V1)", "SHORT TERM FEE (V2)", "CURRENT PLAN",
                "ANNUAL", "REGISTRATION FEE", "HALF DAY", "FULL DAY", "EXTENDED PRESCHOOL", "AFTER SCHOOL", "EVENING PROGRAM"));
        log("✅ SC_003_TC_001 PASSED");
    }

    @Test(priority = 11, description = "SC_003_TC_003 — Close the Child Plan popup")
    public void sc003_tc003_closeChildPlanModal() throws InterruptedException {
        log("▶ SC_003_TC_003 — Close Child Plan");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildPlan();
        Assert.assertTrue(accountStatementPage.isChildPlanModalVisible(), "❌ Child Plan popup did not open");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Child Plan popup did not close");
        log("✅ SC_003_TC_003 PASSED");
    }

    @Test(priority = 12, description = "SC_003_TC_004 — Child Plan fee of the child's program = Monthly Subscription plan amount")
    public void sc003_tc004_childPlanMatchesSubscription() throws InterruptedException {
        log("▶ SC_003_TC_004 — Child Plan vs Monthly Subscription");
        open(REGULAR_CHILD_ID);
        String monthly = accountStatementPage.getSubscriptionPanelText("Monthly Subscription");
        java.util.regex.Matcher pm = java.util.regex.Pattern.compile("Tuition Fee - (.+?) Program").matcher(monthly);
        java.util.regex.Matcher am = java.util.regex.Pattern.compile("Plan Amount\\s*:\\s*₹?\\s*([\\d,]+\\.\\d{2})").matcher(monthly);
        Assert.assertTrue(pm.find() && am.find(), "❌ Could not read the monthly plan: " + monthly);
        String program = pm.group(1).trim();
        double planAmount = Double.parseDouble(am.group(1).replace(",", ""));
        boolean v1 = accountStatementPage.getStatementHeaderText().contains("[PAYMENT PLAN : V1]");

        accountStatementPage.clickChildPlan();
        Assert.assertTrue(accountStatementPage.isChildPlanModalVisible(), "❌ Child Plan popup did not open");
        java.util.regex.Matcher rm = java.util.regex.Pattern
                .compile("(?m)^" + java.util.regex.Pattern.quote(program) + "\\s+₹\\s*([\\d,]+\\.\\d{2})\\s+₹\\s*([\\d,]+\\.\\d{2})")
                .matcher(accountStatementPage.getPopupText());
        Assert.assertTrue(rm.find(), "❌ Program row '" + program + "' not found in Child Plan");
        double fee = Double.parseDouble((v1 ? rm.group(1) : rm.group(2)).replace(",", ""));
        log("   " + program + " " + (v1 ? "V1" : "V2") + " fee " + fee + " vs Monthly Plan Amount " + planAmount);
        Assert.assertEquals(fee, planAmount, 0.001, "❌ Child Plan fee differs from the Monthly Subscription plan amount");
        log("✅ SC_003_TC_004 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_004 — CENTER PLAN
    // ═══════════════════════════════════════════════

    @Test(priority = 13, description = "SC_004_TC_001 — Center Plan popup default view")
    public void sc004_tc001_verifyCenterPlanModalView() throws InterruptedException {
        log("▶ SC_004_TC_001 — Center Plan");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickCenterPlan();
        Assert.assertTrue(accountStatementPage.isCenterPlanModalVisible(), "❌ Center Plan popup did not open");
        assertContainsAll("Center Plan popup", accountStatementPage.getPopupText(), Arrays.asList(
                "Program", "Long Term Fee (V1)", "Short Term Fee (V2)", "Annual", "Registration Fee", "Security Fee",
                "Full Day", "Half Day", "After School", "Extended Preschool", "Evening Program"));
        log("✅ SC_004_TC_001 PASSED");
    }

    @Test(priority = 14, description = "SC_004_TC_002 — Close the Center Plan popup")
    public void sc004_tc002_closeCenterPlanModal() throws InterruptedException {
        log("▶ SC_004_TC_002 — Close Center Plan");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickCenterPlan();
        Assert.assertTrue(accountStatementPage.isCenterPlanModalVisible(), "❌ Center Plan popup did not open");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Center Plan popup did not close");
        log("✅ SC_004_TC_002 PASSED");
    }

    @Test(priority = 15, description = "SC_004_TC_003 — Corporate child's Center Plan shows the Tie Up Details button")
    public void sc004_tc003_corporateCenterPlanTieUpDetails() throws InterruptedException {
        log("▶ SC_004_TC_003 — Corporate Center Plan");
        open(CORPORATE_CHILD_ID);
        accountStatementPage.clickCenterPlan();
        Assert.assertTrue(accountStatementPage.isCenterPlanModalVisible(), "❌ Center Plan popup did not open");
        Assert.assertTrue(accountStatementPage.getPopupText().contains("Tie Up Details"), "❌ 'Tie Up Details' button missing");
        Assert.assertTrue(accountStatementPage.isHeaderLinkVisible("pop_center_plan_view") , "❌ Center Plan link missing");
        log("✅ SC_004_TC_003 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_005 — DIARY NOTES
    // ═══════════════════════════════════════════════

    @Test(priority = 16, description = "SC_005_TC_001 — Diary Notes popup default view")
    public void sc005_tc001_verifyDiaryNotesModalView() throws InterruptedException {
        log("▶ SC_005_TC_001 — Diary Notes");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickDiaryNotes();
        Assert.assertTrue(accountStatementPage.isDiaryNotesModalVisible(), "❌ Diary Notes popup did not open");
        Assert.assertTrue(accountStatementPage.getPopupTitle().startsWith("Notes"), "❌ Title: " + accountStatementPage.getPopupTitle());
        assertContainsAll("Diary Notes popup", accountStatementPage.getPopupText(), Arrays.asList("Updated By", "Comment", "Submit"));
        org.openqa.selenium.WebElement updatedBy = accountStatementPage.findInPopup("input[type='text'][disabled]");
        Assert.assertNotNull(updatedBy, "❌ Updated By field missing");
        Assert.assertEquals(updatedBy.getAttribute("value"), actingUser, "❌ Updated By should be the logged-in user");
        Assert.assertEquals(accountStatementPage.findInPopup("#note_text").getAttribute("placeholder"), "Type your comment", "❌ Comment placeholder");
        String notes = accountStatementPage.getDiaryNotesListText();
        Assert.assertTrue(notes.contains("@") || accountStatementPage.isNoCommentsYetVisible(), "❌ Neither notes nor 'No Comments Yet' shown");
        log("✅ SC_005_TC_001 PASSED — notes: " + notes.split("\n")[0]);
    }

    @Test(priority = 17, description = "SC_005_TC_002 — Close the Diary Notes popup")
    public void sc005_tc002_closeDiaryNotesModal() throws InterruptedException {
        log("▶ SC_005_TC_002 — Close Diary Notes");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickDiaryNotes();
        Assert.assertTrue(accountStatementPage.isDiaryNotesModalVisible(), "❌ Diary Notes popup did not open");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Diary Notes popup did not close");
        log("✅ SC_005_TC_002 PASSED");
    }

    @Test(priority = 18, description = "SC_005_TC_004 — Diary Notes: empty Submit shows a validation message")
    public void sc005_tc004_diaryNoteEmptySubmit() throws InterruptedException {
        log("▶ SC_005_TC_004 — Diary Notes empty Submit");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickDiaryNotes();
        Assert.assertTrue(accountStatementPage.isDiaryNotesModalVisible(), "❌ Diary Notes popup did not open");
        accountStatementPage.clickDiarySubmit();
        String msg = accountStatementPage.getPopupValidationMessage();
        log("   Empty Submit → " + msg);
        Assert.assertEquals(msg, "Please enter some text!", "❌ Empty Submit message");
        log("✅ SC_005_TC_004 PASSED");
    }

    @Test(priority = 19, description = "SC_005_TC_003 — Submit a Diary Note (adds one note on UAT)")
    public void sc005_tc003_submitDiaryNote() throws InterruptedException {
        String note = "QA automation note SC_005_TC_003 " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMHHmmss"));
        log("▶ SC_005_TC_003 — Submit Diary Note: " + note);
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickDiaryNotes();
        Assert.assertTrue(accountStatementPage.isDiaryNotesModalVisible(), "❌ Diary Notes popup did not open");
        accountStatementPage.enterDiaryComment(note);
        accountStatementPage.clickDiarySubmit();
        accountStatementPage.acceptConfirmationAlert();
        long end = System.currentTimeMillis() + 10000;
        while (!accountStatementPage.getPopupText().contains(note) && System.currentTimeMillis() < end) Thread.sleep(500);
        if (!accountStatementPage.getPopupText().contains(note)) {   // list may refresh only on reopen
            accountStatementPage.closeModalByJs();
            open(REGULAR_CHILD_ID);
            accountStatementPage.clickDiaryNotes();
            accountStatementPage.isDiaryNotesModalVisible();
        }
        String list = accountStatementPage.getDiaryNotesListText();
        Assert.assertTrue(list.contains(note), "❌ New note not listed:\n" + list);
        Assert.assertTrue(list.contains("@" + actingUser), "❌ Note author should be @" + actingUser);
        log("✅ SC_005_TC_003 PASSED — note listed by @" + actingUser);
    }

    // ═══════════════════════════════════════════════
    // SC_006 — CHILD HISTORY / CHILD INFO
    // ═══════════════════════════════════════════════

    @Test(priority = 20, description = "SC_006_TC_001 — Child History popup default view")
    public void sc006_tc001_verifyChildHistoryModalView() throws InterruptedException {
        log("▶ SC_006_TC_001 — Child History (" + HISTORY_CHILD_ID + ")");
        open(HISTORY_CHILD_ID);
        accountStatementPage.clickChildHistory();
        Assert.assertTrue(accountStatementPage.isChildHistoryModalVisible(), "❌ Child History popup did not open");
        String text = accountStatementPage.getPopupText();
        log("   " + text.replace("\n", " | "));
        Assert.assertTrue(text.matches("(?s).*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*") || text.contains("No History Record(s) Retrieved"),
                "❌ Neither history entries nor 'No History Record(s) Retrieved' shown");
        log("✅ SC_006_TC_001 PASSED");
    }

    @Test(priority = 21, description = "SC_006_TC_002 — Close the Child History popup")
    public void sc006_tc002_closeChildHistoryModal() throws InterruptedException {
        log("▶ SC_006_TC_002 — Close Child History");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildHistory();
        Assert.assertTrue(accountStatementPage.isChildHistoryModalVisible(), "❌ Child History popup did not open");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Child History popup did not close");
        log("✅ SC_006_TC_002 PASSED");
    }

    @Test(priority = 22, description = "SC_006_TC_004 — Child Info popup default view (Basic Information)")
    public void sc006_tc004_verifyChildInfoModalView() throws InterruptedException {
        log("▶ SC_006_TC_004 — Child Info");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildInfo();
        Assert.assertTrue(accountStatementPage.isChildInfoModalVisible(), "❌ Child Info popup did not open");
        String text = accountStatementPage.getPopupText();
        assertContainsAll("Child Info popup", text, Arrays.asList(
                REGULAR_CHILD_NAME + " #" + REGULAR_CHILD_ID, "Basic Information", "Home Address", "Billing Information", "Admission Payment",
                "Admission Date", "Joining Date", "Center", "Admission Type", "First Name", "Last Name", "DOB", "Gender",
                "Blood Group", "Allergies", "Program", "Supermam", "Class", "Class Assignment", "PARENTS DETAILS",
                "Father Name", "Mother Name", "Email", "Phone", "Share parent-app details"));
        for (String css : new String[]{"a[title='History']", "a[title='Go to Customer Portal']", "a[title='Child Information PDF']"}) {
            Assert.assertNotNull(accountStatementPage.findInPopup(css), "❌ Header icon missing: " + css);
        }
        log("✅ SC_006_TC_004 PASSED");
    }

    @Test(priority = 23, description = "SC_006_TC_007 — Basic Information: names editable, parent email/phone read-only")
    public void sc006_tc007_verifyBasicInfoTab() throws InterruptedException {
        log("▶ SC_006_TC_007 — Basic Information");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildInfo();
        Assert.assertTrue(accountStatementPage.isChildInfoModalVisible(), "❌ Child Info popup did not open");
        accountStatementPage.clickBasicInfoTab();
        String first = accountStatementPage.findInPopup("#first_name_change").getAttribute("value");
        String last = accountStatementPage.findInPopup("#last_name_change").getAttribute("value");
        Assert.assertEquals((first + " " + last).trim(), REGULAR_CHILD_NAME, "❌ First + Last name");
        Assert.assertTrue(accountStatementPage.findInPopup("#first_name_change").isEnabled(), "❌ First Name should be editable");
        for (String id : new String[]{"#father_email", "#mother_email", "#father_phone", "#mother_phone"}) {
            Assert.assertFalse(accountStatementPage.findInPopup(id).isEnabled(), "❌ " + id + " should be read-only");
        }
        log("✅ SC_006_TC_007 PASSED — " + first + " " + last);
    }

    @Test(priority = 24, description = "SC_006_TC_012 — Child Info → Home Address default view")
    public void sc006_tc012_verifyHomeAddressTab() throws InterruptedException {
        log("▶ SC_006_TC_012 — Home Address");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildInfo();
        Assert.assertTrue(accountStatementPage.isChildInfoModalVisible(), "❌ Child Info popup did not open");
        accountStatementPage.clickHomeAddressTab();
        assertContainsAll("Home Address", accountStatementPage.getChildInfoTabText("#tab-home-address"),
                Arrays.asList("House No / Flat No / Society Name", "Landmark", "City", "State", "UPDATE"));
        Assert.assertNotNull(accountStatementPage.findInPopup("#pincode"), "❌ Pincode field missing");
        log("✅ SC_006_TC_012 PASSED");
    }

    @Test(priority = 25, description = "SC_006_TC_014 — Child Info → Billing Information default view")
    public void sc006_tc014_verifyBillingInfoTab() throws InterruptedException {
        log("▶ SC_006_TC_014 — Billing Information");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildInfo();
        Assert.assertTrue(accountStatementPage.isChildInfoModalVisible(), "❌ Child Info popup did not open");
        accountStatementPage.clickBillingInfoTab();
        assertContainsAll("Billing Information", accountStatementPage.getChildInfoTabText("#tab-billing-information"),
                Arrays.asList("Invoiced By", "Excess Payment", "Total Dues", "Credit Balance", "Monthly Plan", "Yearly Plan",
                        "Monthly Addons", "Yearly Addons", "Monthly Coupons", "Yearly Coupons", "Customer Services",
                        "Send Fee Certificate", "Mail To Parents"));
        log("✅ SC_006_TC_014 PASSED");
    }

    @Test(priority = 26, description = "SC_006_TC_016 — Child Info → Admission Payment default view")
    public void sc006_tc016_verifyAdmissionPaymentTab() throws InterruptedException {
        log("▶ SC_006_TC_016 — Admission Payment");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildInfo();
        Assert.assertTrue(accountStatementPage.isChildInfoModalVisible(), "❌ Child Info popup did not open");
        accountStatementPage.clickAdmissionPaymentTab();
        String text = accountStatementPage.getChildInfoTabText("#tab-admission-payment");
        assertContainsAll("Admission Payment", text, Arrays.asList(
                "Payment Approved?", "Joining Approved?", "Payment Date", "Payment Type", "Registration Fee", "Amount Payable", "Download"));
        log("✅ SC_006_TC_016 PASSED");
    }

    @Test(priority = 27, description = "SC_006_TC_018 — Close the Child Info popup")
    public void sc006_tc018_closeChildInfoModal() throws InterruptedException {
        log("▶ SC_006_TC_018 — Close Child Info");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickChildInfo();
        Assert.assertTrue(accountStatementPage.isChildInfoModalVisible(), "❌ Child Info popup did not open");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Child Info popup did not close");
        log("✅ SC_006_TC_018 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_007 — SERVICE REQUEST (forms opened, never submitted)
    // ═══════════════════════════════════════════════

    @Test(priority = 28, description = "SC_007_TC_001 — Service Request popup: each service opens its form")
    public void sc007_tc001_verifyServiceRequestModal() throws InterruptedException {
        log("▶ SC_007_TC_001 — Service Request");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickServiceRequest();
        Assert.assertTrue(accountStatementPage.isServiceRequestModalVisible(), "❌ Service Request popup did not open");
        Assert.assertTrue(accountStatementPage.getPopupText().contains("#" + REGULAR_CHILD_ID + " ACTIVE"), "❌ Child header in popup");
        Map<String, List<String>> forms = new LinkedHashMap<>();
        forms.put("Center Shift", Arrays.asList("breakFrom", "new_center", "new_program"));
        forms.put("Child Pause", Arrays.asList("breakFrom", "breakTo", "pause_reason"));
        forms.put("Extended Daycare", Arrays.asList("extendedbreakFrom", "extendedbreakTo"));
        forms.put("Program Change", Arrays.asList("breakFrom", "new_program"));
        forms.put("Start Time Extension", Arrays.asList("startFrom"));
        forms.put("Withdraw Child", Arrays.asList("breakTo", "withdraw_reason"));
        Assert.assertTrue(accountStatementPage.getServiceTypeOptions().containsAll(forms.keySet()),
                "❌ Services list: " + accountStatementPage.getServiceTypeOptions());
        for (Map.Entry<String, List<String>> f : forms.entrySet()) {
            accountStatementPage.selectServiceType(f.getKey());
            List<String> fields = accountStatementPage.getVisiblePopupFieldIds();
            log("   " + f.getKey() + " → " + fields);
            Assert.assertTrue(fields.containsAll(f.getValue()), "❌ " + f.getKey() + " form fields: " + fields);
            Assert.assertNotNull(accountStatementPage.findInPopup("#submit_request, button[name='add_request']"), "❌ Submit missing for " + f.getKey());
        }
        log("✅ SC_007_TC_001 PASSED");
    }

    @Test(priority = 29, description = "SC_007_TC_002 — Center Shift form default view (not submitted)")
    public void sc007_tc002_verifyCenterShiftRequest() throws InterruptedException {
        log("▶ SC_007_TC_002 — Center Shift form");
        open(REGULAR_CHILD_ID);
        accountStatementPage.clickServiceRequest();
        Assert.assertTrue(accountStatementPage.isServiceRequestModalVisible(), "❌ Service Request popup did not open");
        accountStatementPage.selectServiceType("Center Shift");
        assertContainsAll("Center Shift form", accountStatementPage.getPopupText(), Arrays.asList("New Center Joining Date", "Shift To", "Submit"));
        Assert.assertTrue(accountStatementPage.getPopupSelectOptions("#new_center").size() > 1, "❌ 'Shift To' has no centres");
        log("✅ SC_007_TC_002 PASSED — form shown; submit is out of scope (read-only run)");
    }

    @Test(priority = 30, description = "SC_007_TC_003 — Attrition child: services are blocked")
    public void sc007_tc003_attritionChildServicesBlocked() throws InterruptedException {
        log("▶ SC_007_TC_003 — Attrition child " + ATTRITION_CHILD_ID);
        open(ATTRITION_CHILD_ID);
        Assert.assertTrue(accountStatementPage.getStatementHeaderText().contains("(ATTRITION)"), "❌ Child should be ATTRITION");
        Assert.assertTrue(accountStatementPage.isRefundWelcomeKitButtonVisible(), "❌ 'Refund Welcome Kit' missing for the attrition child");
        accountStatementPage.clickServiceRequest();
        Assert.assertTrue(accountStatementPage.waitForPopupTitle("ATTRITION", 15), "❌ Service Request popup did not open");
        Assert.assertTrue(accountStatementPage.getPopupText().contains("You can't access services of attrition child"), "❌ Block message missing");
        Assert.assertFalse(accountStatementPage.isServiceTypeDropdownEnabled(), "❌ Services dropdown should be disabled");
        log("✅ SC_007_TC_003 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_008 / SC_009 — CUSTOMER PORTAL / CUSTOMER REQUESTS (new tabs)
    // ═══════════════════════════════════════════════

    @Test(priority = 31, description = "SC_008_TC_001 — Customer Portal opens the child's billing page in a new tab")
    public void sc008_tc001_verifyCustomerPortal() throws InterruptedException {
        log("▶ SC_008_TC_001 — Customer Portal");
        open(REGULAR_CHILD_ID);
        int tabs = driver.getWindowHandles().size();
        accountStatementPage.clickCustomerPortal();
        String url = switchToNewTab(tabs);
        log("   " + url);
        Assert.assertTrue(url.contains("billing?idk="), "❌ Unexpected Customer Portal URL: " + url);
        String body = driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
        assertContainsAll("Customer Portal", body, Arrays.asList("Account Information (#" + REGULAR_CHILD_ID + ")", "Update Address",
                "Get Fee Certificate", "Customer Request Archive", "Subscription #1", "Subscription #2"));
        log("✅ SC_008_TC_001 PASSED");
    }

    @Test(priority = 32, description = "SC_009_TC_001 — Customer Requests opens Recent Customer Requests in a new tab")
    public void sc009_tc001_verifyCustomerRequestRedirect() throws InterruptedException {
        log("▶ SC_009_TC_001 — Customer Requests");
        open(REGULAR_CHILD_ID);
        int tabs = driver.getWindowHandles().size();
        accountStatementPage.clickCustomerRequest();
        String url = switchToNewTab(tabs);
        log("   " + url);
        Assert.assertTrue(url.contains("recent_update_details?child_id=" + REGULAR_CHILD_ID), "❌ Unexpected URL: " + url);
        log("✅ SC_009_TC_001 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_010 — RECTIFY BRANCH (only inside its joining-date window)
    // ═══════════════════════════════════════════════

    @Test(priority = 33, description = "SC_010_TC_001 — Rectify Branch Selection button visibility")
    public void sc010_tc001_verifyRectifyBranchVisibility() throws InterruptedException {
        log("▶ SC_010_TC_001 — Rectify Branch visibility");
        open(REGULAR_CHILD_ID);
        boolean visible = accountStatementPage.isRectifyBranchBtnVisible();
        log("   Rectify Branch visible: " + visible + " (shown only within 3 days of the joining date; " + REGULAR_CHILD_ID + " joined Sep 2024)");
        Assert.assertFalse(visible, "❌ Rectify Branch must not show long after the joining date");
        log("✅ SC_010_TC_001 PASSED");
    }

    @Test(priority = 34, description = "SC_010_TC_003 — Rectify Branch popup default view (when a child in the window is given)")
    public void sc010_tc003_verifyRectifyBranchModal() throws InterruptedException {
        String child = System.getProperty("asRectifyChild", "");
        if (child.isEmpty()) throw new org.testng.SkipException("Needs a child within 3 days of joining: -DasRectifyChild=<id>");
        log("▶ SC_010_TC_003 — Rectify Branch popup (" + child + ")");
        open(child);
        Assert.assertTrue(accountStatementPage.isRectifyBranchBtnVisible(), "❌ Rectify Branch button not shown for " + child);
        accountStatementPage.clickRectifyBranch();
        Assert.assertTrue(accountStatementPage.isRectifyBranchModalVisible(), "❌ Rectify Branch popup did not open");
        Assert.assertTrue(accountStatementPage.isRectifyBranchDefaultViewCorrect(), "❌ Rectify Branch default view");
        log("✅ SC_010_TC_003 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_021 — APPLY DAYCARE ADDON (popup only)
    // ═══════════════════════════════════════════════

    @Test(priority = 35, description = "SC_021_TC_002 — Apply Daycare Addon button visible for an Extended Preschool child")
    public void sc021_tc002_applyDaycareAddonVisible() throws InterruptedException {
        log("▶ SC_021_TC_002 — Apply Daycare Addon visible");
        open(REGULAR_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isApplyDaycareAddonVisible(), "❌ 'Apply Daycare Addon' missing for " + actingUser);
        log("✅ SC_021_TC_002 PASSED");
    }

    @Test(priority = 36, description = "SC_021_TC_018 — Apply Daycare Addon not offered to a corporate tie-up child")
    public void sc021_tc018_applyDaycareAddonHiddenForCorporate() throws InterruptedException {
        log("▶ SC_021_TC_018 — Apply Daycare Addon hidden for corporate");
        open(CORPORATE_CHILD_ID);
        Assert.assertFalse(accountStatementPage.isApplyDaycareAddonVisible(), "❌ 'Apply Daycare Addon' shown for a corporate child");
        log("✅ SC_021_TC_018 PASSED");
    }

    @Test(priority = 37, description = "SC_021_TC_026 — Apply Daycare Addon popup default view (not submitted)")
    public void sc021_tc026_applyDaycareAddonPopup() throws InterruptedException {
        log("▶ SC_021_TC_026 — Apply Daycare Addon popup");
        open(REGULAR_CHILD_ID);
        openPopup("pop_extended_addon", "#" + REGULAR_CHILD_ID);
        assertContainsAll("Daycare Addon popup", accountStatementPage.getPopupText(), Arrays.asList(
                "Request will be processed at month end if applicable from next month",
                "This will transfer child into Half Day Program with Daycare add-on",
                "Are you sure you want to continue?", "Applicable From", "Submit"));
        Assert.assertEquals(accountStatementPage.getPopupSelectOptions("#applicable_from_next"), Arrays.asList("Current Month", "Next Month"),
                "❌ Applicable From options");
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Popup did not close");
        log("✅ SC_021_TC_026 PASSED");
    }

    // ═══════════════════════════════════════════════
    // SC_001_TC_008 — DEFAULT VIEW PER ADMISSION TYPE (user, 2026-11-19)
    // ═══════════════════════════════════════════════

    @org.testng.annotations.DataProvider(name = "admissionTypes")
    public Object[][] admissionTypes() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Regular", AT_REGULAR});
        rows.add(new Object[]{"Corporate", AT_CORPORATE});
        rows.add(new Object[]{"Summer Camp", AT_SUMMER_CAMP});
        rows.add(new Object[]{"Emergency Daycare", AT_EMERGENCY});
        return rows.toArray(new Object[0][]);
    }

    @Test(priority = 38, dataProvider = "admissionTypes",
            description = "SC_001_TC_008 — Default view of the generated statement for each admission type")
    public void sc001_tc008_defaultViewPerAdmissionType(String type, String childId) throws InterruptedException {
        if (childId.isEmpty()) throw new org.testng.SkipException(type + ": no child id yet (pass -DasEmergencyChild=<id>)");
        log("▶ SC_001_TC_008 — " + type + " #" + childId);
        accountStatementPage.enterAdmissionId(childId);
        accountStatementPage.clickGenerate();
        accountStatementPage.hideApiDebugPanel();

        // common to every type
        Assert.assertEquals(accountStatementPage.getPageHeading(), "Account Statement (#" + childId + ")", "❌ Page heading");
        String header = accountStatementPage.getStatementHeaderText();
        log("   Header: " + header);
        if (type.equals("Emergency Daycare")) {
            verifyEmergencyDaycareView(childId, header);
            return;
        }
        Assert.assertTrue(header.contains("#" + childId) && header.matches(".*\\((ACTIVE|PAUSE|ATTRITION|JOINING[^)]*)\\).*"),
                "❌ Header should show #id and status: " + header);
        Assert.assertTrue(accountStatementPage.getStatementPeriod().matches("[A-Z]{3}-\\d{4} TO TILL DATE"),
                "❌ Period: " + accountStatementPage.getStatementPeriod());
        List<String> got = new ArrayList<>();
        for (String k : accountStatementPage.getHeaderActionLinks().keySet()) got.add(norm(k));
        for (String a : HEADER_ACTIONS) Assert.assertTrue(got.contains(norm(a)), "❌ " + type + ": action missing: " + a);
        Assert.assertTrue(accountStatementPage.isPdfIconVisible(), "❌ PDF icon missing");
        List<String> sections = accountStatementPage.getSectionHeadings();
        Assert.assertTrue(sections.containsAll(Arrays.asList("Subscriptions", "Transactions", "Account Summary")), "❌ Sections: " + sections);
        for (String panel : Arrays.asList("Monthly Subscription", "Yearly Subscription")) {
            String txt = accountStatementPage.getSubscriptionPanelText(panel);
            assertContainsAll(type + " " + panel, txt, Arrays.asList("Plan Name", "Plan Amount", "Next Billing Date", "Coupons", "Addons"));
            log("   " + txt.replace("\n", " | "));
        }
        assertContainsAll(type + " Transactions header", String.join(" | ", accountStatementPage.getTransactionHeaders()), TRANSACTION_HEADERS);
        List<String> common = new ArrayList<>(SUMMARY_LABELS);
        if (type.equals("Corporate")) common.removeAll(Arrays.asList("Promotional Credits Issued", "Promotional Credits Adjusted"));
        assertContainsAll(type + " Account Summary", accountStatementPage.getAccountSummaryText(), common);
        log("   Summary: " + accountStatementPage.getAccountSummary());

        // type-specific
        switch (type) {
            case "Regular":
                Assert.assertTrue(header.matches(".*\\[PAYMENT PLAN : V[12]\\].*"), "❌ Payment plan tag missing");
                Assert.assertFalse(header.contains("TIE-UP"), "❌ Regular child shows a tie-up");
                Assert.assertTrue(accountStatementPage.isMigrateRegularToCorporateVisible(), "❌ Migrate Regular→Corporate missing");
                break;
            case "Corporate":
                Assert.assertTrue(header.contains("TIE-UP :"), "❌ Tie-up name missing");
                Assert.assertTrue(accountStatementPage.isMigrateCorporateToRegularVisible(), "❌ Migrate Corporate→Regular missing");
                Assert.assertFalse(accountStatementPage.isMigrateRegularToCorporateVisible(), "❌ Migrate Regular→Corporate shown for corporate");
                assertContainsAll("Corporate summary", accountStatementPage.getAccountSummaryText(), Arrays.asList("Corporate Account Summary"));
                break;
            case "Summer Camp":
                Assert.assertFalse(header.contains("TIE-UP"), "❌ Summer Camp child shows a tie-up");
                Assert.assertFalse(accountStatementPage.isMigrateRegularToCorporateVisible() || accountStatementPage.isMigrateCorporateToRegularVisible(),
                        "❌ Admission migration offered to a Summer Camp child");
                break;
            default:
                break;
        }
        log("✅ SC_001_TC_008 PASSED — " + type);
    }

    /**
     * Emergency Daycare (confirmed live 19-Nov-2026, child 53394): reduced view — status shown as "(EMERGENCY DAYCARE)",
     * one header action "Details" (pop_emergency_daycare, labelled with the child name) instead of the ten standard icons;
     * Subscriptions "Not Available"; same Transactions / Account Summary.
     */
    private void verifyEmergencyDaycareView(String childId, String header) throws InterruptedException {
        Assert.assertTrue(header.contains("(EMERGENCY DAYCARE)"), "❌ Header should show (EMERGENCY DAYCARE): " + header);
        Assert.assertTrue(accountStatementPage.getStatementPeriod().matches("[A-Z]{3}-\\d{4} TO TILL DATE"), "❌ Period");
        Assert.assertTrue(accountStatementPage.isPdfIconVisible(), "❌ PDF icon missing");
        Assert.assertTrue(accountStatementPage.getSectionHeadings().containsAll(Arrays.asList("Subscriptions", "Transactions", "Account Summary")),
                "❌ Sections: " + accountStatementPage.getSectionHeadings());
        assertContainsAll("Emergency Transactions header", String.join(" | ", accountStatementPage.getTransactionHeaders()), TRANSACTION_HEADERS);
        assertContainsAll("Emergency Account Summary", accountStatementPage.getAccountSummaryText(), SUMMARY_LABELS);
        log("   Actions: " + accountStatementPage.getHeaderActionLinks());
        Assert.assertTrue(accountStatementPage.isHeaderLinkVisible("pop_emergency_daycare"), "❌ Emergency Daycare 'Details' action missing");
        Assert.assertFalse(accountStatementPage.isHeaderLinkVisible("pop_child_services"), "❌ Service Request shown for an Emergency Daycare child");
        accountStatementPage.clickHeaderLink("pop_emergency_daycare");
        Assert.assertTrue(accountStatementPage.waitForPopupTitle("#" + childId + " EMERGENCY DAYCARE", 15), "❌ Details popup did not open");
        assertContainsAll("Emergency Details popup", accountStatementPage.getPopupText(), Arrays.asList(
                "Child Information", "Parent Information", "PERSONAL DETAILS", "Name", "Last Name", "Gender", "DOB",
                "ADDRESS DETAILS", "Address1", "Address2", "City", "State", "PinCode", "Update", "Close"));
        Assert.assertTrue(accountStatementPage.closePopupByIcon(), "❌ Details popup did not close");
        log("✅ SC_001_TC_008 PASSED — Emergency Daycare (reduced view, Details popup)");
    }
}
