package testScripts.FranchiseTests.CorporateTieups;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.CorporateCreditsIssuedPage;
import pages.Navigations;
import pages.Support.FinancialReports.AccountStatementPage;
import pages.Support.RecentCustomerRequestsPage;
import pages.Support.Regular_ServiceRequests;
import utils.APIs;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════════
// CORPORATE CREDITS ISSUED (Franchise → Corporate Tieups → Corporate Credits Issued, /issue_corporate_credits)
// Source: TC_Corporate Tie-ups.xlsx SC_011_TC_001–018, updated to the live screen + new TCs →
// TC_Corporate_Tieups.xlsx, sheet TC_CorporateCreditsIssued. Runs as Rakesh (View_Corporate_Invoice +
// Override_Corporate_Payer).
// Saves (user, 2026-11-03): issues a Rs 100 corporate credit on child 37261, re-links it to another invoice of the
// same child, then revokes it — all in one run, so nothing is left behind.
// ═══════════════════════════════════════════════════════════════
public class CorporateCreditsIssuedTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Child Name", "Amount", "Type", "Description", "Issued By",
            "Invoice Reference", "Issued Date Time", "Update Status", "Action");
    private static final List<String> CSV_COLUMNS = Arrays.asList("Child Id", "Child Name", "Issued Date", "Amount", "Type",
            "Description", "Invoice Line Item", "Issued By", "Invoice Reference", "Issued Date Time", "Update Status",
            "Father Name", "Mother Name", "Father Email", "Mother Email", "Father Phone", "Mother Phone");
    private static final List<String> CREDIT_TYPES = Arrays.asList("Downgrade Credits", "Credits - Tuition Fee", "Credits - Annual Fee");
    private static final String CHILD_ID = System.getProperty("corpCreditChildId", "37261");
    private static final String NON_CORPORATE_CHILD_ID = System.getProperty("regularChildId", "68192");
    private static final String AMOUNT = "100";
    private static final String CENTER = System.getProperty("corpCreditCenter", "Koramangala");
    // TC_013 step 3 — center shift (user, 2026-11-04)
    private static final String CS_CHILD_ID = System.getProperty("csCreditChildId", "62383");
    private static final String CS_CHILD_NAME = System.getProperty("csCreditChildName", "Abeer Malik");
    private static final String CS_CENTER = System.getProperty("csCreditCenter", "Sector 122, Noida"), CS_PROGRAM = "Full Day";
    private static final DateTimeFormatter GRID_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    Navigations navigations;
    CorporateCreditsIssuedPage creditsPage;

    // Set by TC_005, used by TC_018 / TC_011 / TC_012
    private String newCreditId;
    private String issuedInvoice;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        reopen();
    }

    private void reopen() throws InterruptedException {
        navigations.goToCorporateCredits();
        creditsPage = new CorporateCreditsIssuedPage(driver);
        Assert.assertTrue(creditsPage.isPageLoaded(), "❌ Corporate Credits Issued did not load");
    }

    // "Nov 3, 2026 06:51:26" → date
    private LocalDate gridDate(String dateTime) {
        return LocalDate.parse(dateTime.replaceAll("\\s+\\d{2}:\\d{2}:\\d{2}$", "").trim(), GRID_DATE);
    }

    // "[PI/56445]" from an invoice option text
    private String invoiceNo(String optionText) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[([^\\]]+)]").matcher(optionText);
        return m.find() ? m.group(1) : "";
    }

    private void requireNewCredit() {
        if (newCreditId == null) throw new SkipException("TC_005 did not issue a credit in this run");
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_011_TC_001 — Default view")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_011_TC_001 — Default view", true);
        String heading = creditsPage.getHeading();
        List<String> visible = creditsPage.getVisibleColumnHeaders();
        List<String> all = creditsPage.getAllColumnHeaders();
        Reporter.log("   Heading: " + heading + " | dates " + creditsPage.getDateValues() + " | center " + creditsPage.getSelectedCenter()
                + " | status " + creditsPage.getStatusOptions() + " (" + creditsPage.getSelectedStatus() + ")", true);
        Reporter.log("   Columns: " + visible + " | all: " + all + " | " + creditsPage.getInfoText() + " | per page " + creditsPage.getPageLength(), true);
        Assert.assertEquals(heading, "Corporate Credits Issued", "❌ Heading");
        Assert.assertTrue(creditsPage.isFromDateVisible() && creditsPage.isToDateVisible(), "❌ From / To date");
        Assert.assertTrue(creditsPage.isCenterFilterVisible(), "❌ Center filter");
        Assert.assertEquals(creditsPage.getSelectedCenter(), "All Center", "❌ Default center");
        Assert.assertTrue(creditsPage.isStatusFilterVisible(), "❌ Status filter");
        Assert.assertEquals(creditsPage.getStatusOptions(), Arrays.asList("All", "Pending", "Successful", "Failed"), "❌ Status options");
        Assert.assertEquals(creditsPage.getSelectedStatus(), "All", "❌ Default status");
        Assert.assertTrue(creditsPage.isFilterButtonVisible() && creditsPage.isClearLinkVisible(), "❌ Filter / Clear");
        Assert.assertTrue(creditsPage.isIssueCreditsVisible(), "❌ Issue Credits button");
        Assert.assertTrue(creditsPage.isSearchVisible() && creditsPage.isDownloadVisible() && creditsPage.isPaginationVisible(),
                "❌ Search / Download Report / pagination");
        for (String c : COLUMNS) Assert.assertTrue(all.contains(c), "❌ Column " + c);
        for (String hidden : Arrays.asList("Invoice Line Item", "Father Name", "Mother Phone"))
            Assert.assertFalse(visible.contains(hidden), "❌ " + hidden + " should be hidden (CSV only)");
        LocalDate today = LocalDate.now();
        String applied = creditsPage.getAppliedFilters();
        Assert.assertTrue(applied.contains(today.minusMonths(1).toString()) && applied.contains(today.toString()),
                "❌ Default range should be last month → today: " + applied);
        Reporter.log("✅ SC_011_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Date range filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_011_TC_002 — Date range filter")
    public void tc002_dateRangeFilter() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_002 — Date range filter", true);
        LocalDate from = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        creditsPage.setDateRange(from, to);
        creditsPage.clickFilter();
        String applied = creditsPage.getAppliedFilters();
        List<String> dates = creditsPage.getColumnValues("Issued Date Time");
        Reporter.log("   " + from + " → " + to + " | applied " + applied + " | shown " + creditsPage.getDateValues()
                + " | " + creditsPage.getInfoText() + " | dates " + dates, true);
        Assert.assertTrue(applied.contains("\"date_from\":\"" + from + "\"") && applied.contains("\"date_to\":\"" + to + "\""),
                "❌ Range not applied: " + applied);
        // ✅ Grid time is UTC (IST − 5:30), so allow one day either side
        for (String d : dates) {
            LocalDate day = gridDate(d);
            Assert.assertFalse(day.isBefore(from.minusDays(1)) || day.isAfter(to.plusDays(1)), "❌ Out of range: " + d);
        }
        Reporter.log("✅ SC_011_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003 — Center filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_011_TC_003 — Center filter")
    public void tc003_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_003 — Center filter (" + CENTER + ")", true);
        int all = creditsPage.getRowCount();
        Assert.assertTrue(creditsPage.getCenterOptions().size() > 1, "❌ Center list empty");
        creditsPage.selectCenter(CENTER);
        creditsPage.clickFilter();
        int filtered = creditsPage.getRowCount();
        Reporter.log("   All centres: " + all + " rows | " + CENTER + ": " + filtered + " rows | kept: " + creditsPage.getSelectedCenter()
                + (filtered == 0 ? " | " + creditsPage.getEmptyTableMessage() : ""), true);
        Assert.assertEquals(creditsPage.getSelectedCenter(), CENTER, "❌ Center not kept after Filter");
        Assert.assertTrue(filtered <= all, "❌ Center filter returned more rows than All");
        if (filtered == 0) Assert.assertEquals(creditsPage.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        Reporter.log("✅ SC_011_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Status filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_011_TC_004 — Status filter")
    public void tc004_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_004 — Status filter", true);
        for (String status : Arrays.asList("Pending", "Successful", "Failed")) {
            creditsPage.selectStatus(status);
            creditsPage.clickFilter();
            List<String> values = creditsPage.getColumnValues("Update Status");
            Reporter.log("   " + status + ": " + values.size() + " rows" + (values.isEmpty() ? " (" + creditsPage.getEmptyTableMessage() + ")" : ""), true);
            Assert.assertEquals(creditsPage.getSelectedStatus(), status, "❌ Status not kept");
            for (String v : values) Assert.assertEquals(v, status, "❌ Row with status " + v);
            if (values.isEmpty()) Assert.assertEquals(creditsPage.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        }
        Reporter.log("✅ SC_011_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_006 — Mandatory fields (sequential messages)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_011_TC_006 — Mandatory fields")
    public void tc006_mandatoryFields() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_006 — Mandatory fields", true);
        creditsPage.openIssueCredits();
        Assert.assertEquals(creditsPage.getPopupTitle(), "Add Corporate Credits", "❌ Popup title");
        creditsPage.enterChildIdAndFetch(CHILD_ID);
        String name = creditsPage.getFetchedChildName();
        Reporter.log("   Child " + CHILD_ID + " → " + name, true);
        Assert.assertFalse(name.isEmpty(), "❌ Child name not fetched");

        List<String> seen = new ArrayList<>();
        creditsPage.clickPopupButton("apply_credit");
        seen.add(creditsPage.getVisibleValidationMessage());
        creditsPage.selectPopupOptionContaining("credit_type_item", "Credits - Tuition Fee");
        creditsPage.clickPopupButton("apply_credit");
        seen.add(creditsPage.getVisibleValidationMessage());
        creditsPage.selectPopupOptionContaining("credit_invoice_ref", "payment_due");
        creditsPage.clickPopupButton("apply_credit");
        seen.add(creditsPage.getVisibleValidationMessage());
        creditsPage.typeInPopup("credit_amount", AMOUNT);
        creditsPage.clickPopupButton("apply_credit");
        seen.add(creditsPage.getVisibleValidationMessage());
        creditsPage.typeInPopup("invoice_line_items", "Tuition fee");
        creditsPage.clickPopupButton("apply_credit");
        seen.add(creditsPage.getVisibleValidationMessage());
        Reporter.log("   Messages in order: " + seen, true);
        Assert.assertEquals(seen, Arrays.asList("Please select credit type!", "Select Invoice Reference!", "Credit Amount required!",
                "Credit Line Item required!", "Comments required!"), "❌ Validation messages");

        creditsPage.typeInPopup("child_id", "");
        creditsPage.clickPopupButton("apply_credit");
        String childMsg = creditsPage.getVisibleValidationMessage();
        Reporter.log("   Empty Child ID: " + childMsg, true);
        Assert.assertEquals(childMsg, "Child ID required!", "❌ Child ID message");
        Reporter.log("✅ SC_011_TC_006 PASSED", true);
    }

    private Object js(String script) {
        return ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(script);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 — Issue a corporate credit (Cancel, then OK)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_011_TC_005 — Issue a corporate credit")
    public void tc005_issueCredit() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_005 — Issue a Rs " + AMOUNT + " corporate credit on " + CHILD_ID, true);
        List<String> before = creditsPage.getCreditIds();
        creditsPage.openIssueCredits();
        creditsPage.enterChildIdAndFetch(CHILD_ID);
        String childName = creditsPage.getFetchedChildName();
        creditsPage.selectPopupOptionContaining("credit_type_item", "Credits - Tuition Fee");
        String invoiceOption = creditsPage.selectPopupOptionContaining("credit_invoice_ref", "payment_due");
        issuedInvoice = invoiceNo(invoiceOption);
        creditsPage.typeInPopup("credit_amount", AMOUNT);
        creditsPage.typeInPopup("invoice_line_items", "Tuition fee");
        creditsPage.typeInPopup("credit_comments", "QA automation corporate credit");

        creditsPage.clickPopupButton("apply_credit");
        String cancelText = creditsPage.dismissConfirm();
        Thread.sleep(1500);
        Reporter.log("   Confirm: " + cancelText + " → Cancel | popup still open: " + creditsPage.isPopupOpen(), true);
        Assert.assertEquals(cancelText, "Are you sure you want to Issue Credit?", "❌ Confirm text");
        Assert.assertTrue(creditsPage.isPopupOpen(), "❌ Popup closed after Cancel");

        creditsPage.clickPopupButton("apply_credit");
        creditsPage.acceptConfirm();
        String message = creditsPage.waitForFlashMessage();
        Reporter.log("   " + childName + " | " + invoiceOption + " | message: " + message, true);
        Thread.sleep(3000);
        reopen();
        List<String> after = creditsPage.getCreditIds();
        after.removeAll(before);
        Assert.assertEquals(after.size(), 1, "❌ Expected exactly one new credit, got " + after);
        newCreditId = after.get(0);
        String row = creditsPage.getRowText(newCreditId);
        Reporter.log("   New credit " + newCreditId + ": " + row, true);
        Assert.assertTrue(message.toLowerCase().contains("success"), "❌ Success message: " + message);
        Assert.assertTrue(row.startsWith(childName + " | " + AMOUNT + " | Credits - Tuition Fee"), "❌ Row: " + row);
        Assert.assertTrue(row.contains("Rakesh Singh") && row.contains(issuedInvoice) && row.contains(" | Successful"), "❌ Row: " + row);
        Reporter.log("✅ SC_011_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_018 — Corporate credit on Account Statement
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_011_TC_018 — Credit shown on Account Statement")
    public void tc018_creditOnAccountStatement() throws InterruptedException {
        requireNewCredit();
        Reporter.log("▶ SC_011_TC_018 — Credit " + newCreditId + " on Account Statement", true);
        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + CHILD_ID);
        Thread.sleep(6000);
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy", Locale.ENGLISH));
        String expected = "Credit issued on " + today;
        String body = String.valueOf(js("return document.body.innerText;"));
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Credit issued on " + today + "[^\\n]*against invoice " + java.util.regex.Pattern.quote(issuedInvoice)
                + "[\\s\\S]{0,200}?Credits - Tuition Fee").matcher(body);
        boolean found = m.find();
        Reporter.log("   Looking for \"" + expected + " … against invoice " + issuedInvoice + " … Credits - Tuition Fee\": " + found
                + (found ? " → " + m.group().replaceAll("\\s+", " ") : ""), true);
        Assert.assertTrue(found, "❌ Corporate credit not on Account Statement");
        Reporter.log("✅ SC_011_TC_018 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_011 — Link Invoice Reference
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_011_TC_011 — Link Invoice Reference")
    public void tc011_linkInvoice() throws InterruptedException {
        requireNewCredit();
        Reporter.log("▶ SC_011_TC_011 — Link Invoice Reference on credit " + newCreditId, true);
        creditsPage.openLinkInvoice(newCreditId);
        String text = creditsPage.getPopupText();
        String preselected = creditsPage.getPopupSelectedText("credit_invoice_ref");
        Reporter.log("   " + creditsPage.getPopupTitle() + " | " + text.substring(0, Math.min(330, text.length())) + " | preselected " + preselected, true);
        Assert.assertEquals(creditsPage.getPopupTitle(), "Link Invoice Reference", "❌ Popup title");
        for (String f : Arrays.asList("Child ID : #" + CHILD_ID, "Child Name :", "Credit Type : Credits - Tuition Fee",
                "Issued Credit Amount : Rs. 100.00", "Remaining Amount to Adjustment: Rs. 100.00", "Credit Issued On :", "Credit Start From :"))
            Assert.assertTrue(text.contains(f), "❌ Popup missing '" + f + "'");
        Assert.assertEquals(invoiceNo(preselected), issuedInvoice, "❌ Current invoice should be preselected");

        creditsPage.clearPopupSelect("credit_invoice_ref");
        creditsPage.clickPopupButton("btn_submit_reference");
        Thread.sleep(800);
        Assert.assertEquals(creditsPage.getVisibleValidationMessage(), "Select Invoice Reference!", "❌ Empty invoice message");

        String target = "";
        for (String o : creditsPage.getPopupSelectOptions("credit_invoice_ref"))
            if (o.contains("payment_due") && !invoiceNo(o).isEmpty() && !invoiceNo(o).equals(issuedInvoice)) {
                target = invoiceNo(o);
                break;
            }
        Assert.assertFalse(target.isEmpty(), "❌ No other due invoice to link to");
        creditsPage.selectPopupOptionContaining("credit_invoice_ref", "[" + target + "]");
        creditsPage.clickPopupButton("btn_submit_reference");
        String message = creditsPage.waitForFlashMessage();
        Thread.sleep(3000);
        reopen();
        String row = creditsPage.getRowText(newCreditId);
        Reporter.log("   Linked " + issuedInvoice + " → " + target + " | message: " + message + " | row: " + row, true);
        Assert.assertEquals(message, "Corporate Credit invoice reference updated successfully.", "❌ Link message");
        Assert.assertTrue(row.contains(target) && !row.contains(issuedInvoice), "❌ Invoice Reference not changed: " + row);
        issuedInvoice = target;
        Reporter.log("✅ SC_011_TC_011 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_012 / TC_014 — Revoke a current-month credit
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC_011_TC_012 — Revoke Credit")
    public void tc012_revokeCredit() throws InterruptedException {
        requireNewCredit();
        Reporter.log("▶ SC_011_TC_012 — Revoke credit " + newCreditId, true);
        List<String> actions = creditsPage.getRowActionTitles(newCreditId);
        Reporter.log("   Actions on the new credit: " + actions, true);
        Assert.assertTrue(actions.contains("Revoke Credit"), "❌ Revoke not offered on a current-month, unadjusted credit");
        creditsPage.openRevokeCredit(newCreditId);
        String text = creditsPage.getPopupText();
        Reporter.log("   " + creditsPage.getPopupTitle() + " | " + text, true);
        Assert.assertEquals(creditsPage.getPopupTitle(), "Revoke Credit", "❌ Popup title");
        for (String f : Arrays.asList("Child ID : #" + CHILD_ID, "Child Name :", "Credit Type : Credits - Tuition Fee",
                "Issued Credit Amount : Rs. 100.00", "Remaining Amount: Rs. 100.00", "Credit Issued On :", "Reason :"))
            Assert.assertTrue(text.contains(f), "❌ Popup missing '" + f + "'");

        creditsPage.clickPopupButton("btn_submit_reason");
        Thread.sleep(800);
        Assert.assertEquals(creditsPage.getVisibleValidationMessage(), "Revoke Reason is Missing!", "❌ Empty reason message");
        creditsPage.typeInPopup("credit_revoke_reason", "QA automation - revoke test credit");
        creditsPage.clickPopupButton("btn_submit_reason");
        String message = creditsPage.waitForFlashMessage();
        Thread.sleep(3000);
        reopen();
        Reporter.log("   Message: " + message + " | credit still listed: " + creditsPage.getCreditIds().contains(newCreditId), true);
        Assert.assertEquals(message, "Corporate Credit revoked successfully", "❌ Revoke message");
        Assert.assertFalse(creditsPage.getCreditIds().contains(newCreditId), "❌ Revoked credit still listed");
        Reporter.log("✅ SC_011_TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_013 — No Revoke on credits from earlier months
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_011_TC_013 — No Revoke on earlier-month credits")
    public void tc013_noRevokeOnOldCredits() {
        Reporter.log("▶ SC_011_TC_013 — Revoke only for current-month credits", true);
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        int old = 0;
        for (String id : creditsPage.getCreditIds()) {
            String row = creditsPage.getRowText(id);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[A-Z][a-z]{2} \\d{1,2}, \\d{4} \\d{2}:\\d{2}:\\d{2}").matcher(row);
            if (!m.find()) continue;
            LocalDate issued = gridDate(m.group());
            List<String> actions = creditsPage.getRowActionTitles(id);
            Reporter.log("   " + id + " issued " + issued + " → " + actions, true);
            // ✅ Grid time is UTC; a credit within the first day could still be this month in IST
            if (issued.isBefore(monthStart.minusDays(1))) {
                old++;
                Assert.assertFalse(actions.contains("Revoke Credit"), "❌ Revoke offered on an earlier-month credit " + id);
                Assert.assertTrue(actions.contains("Link Invoice Reference"), "❌ Link Invoice missing on " + id);
            }
        }
        if (old == 0) throw new SkipException("No earlier-month credit in the default range");
        Reporter.log("✅ SC_011_TC_013 PASSED (" + old + " earlier-month credits checked)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_013 step 3 — No Revoke on a center shift credit (SAVES: center shift on child 62383)
    //  User (2026-11-05): shift 62383 to Sector 122, Noida, Full Day; submit today, process later (45924 is blocked:
    //  "This facility is not applicable for tie-up benefit admission"). Each run moves the request one
    //  step on (submit → getAllPendingRequests → cronProcessCenterShiftRequests) and skips until the shift is processed.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC_011_TC_013 — No Revoke on a center shift credit")
    public void tc013b_centerShiftCreditNotRevocable() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_013 step 3 — center shift of " + CS_CHILD_ID + " to " + CS_CENTER + ", " + CS_PROGRAM, true);
        RecentCustomerRequestsPage requestsPage = new RecentCustomerRequestsPage(driver);
        String status = requestsPage.getColumnValueByRequestType(CS_CHILD_ID, "Center Shift", "Request Status");
        Reporter.log("   Center Shift request status: '" + status + "'", true);

        if (status.isEmpty()) {
            String joining = LocalDate.now().plusMonths(1).withDayOfMonth(1).toString();
            Regular_ServiceRequests serviceRequestPage = new Regular_ServiceRequests(driver);
            navigations.goToAccountStatement();
            new AccountStatementPage(driver).generateAccountStatement(CS_CHILD_ID);
            serviceRequestPage.clickServiceRequestLink();
            serviceRequestPage.selectServiceType("Center Shift");
            Assert.assertTrue(serviceRequestPage.isCenterShiftFormVisible(), "❌ Center Shift form not visible");
            serviceRequestPage.setCSEffectiveDate(joining);
            serviceRequestPage.selectCSNewCenter(CS_CENTER);
            Thread.sleep(1500);
            String program = null;
            for (org.openqa.selenium.WebElement o : new org.openqa.selenium.support.ui.Select(serviceRequestPage.cs_newProgram_dropdown).getOptions())
                if (o.getText().trim().startsWith(CS_PROGRAM)) program = o.getText().trim();
            Assert.assertNotNull(program, "❌ " + CS_PROGRAM + " not offered at " + CS_CENTER);
            serviceRequestPage.selectCSNewProgram(program);
            serviceRequestPage.checkCSCenterVisitDeclaration();
            serviceRequestPage.submitCenterShift();
            Thread.sleep(800);
            String confirm = serviceRequestPage.getAlertText();
            serviceRequestPage.acceptAlert();
            Thread.sleep(2000);
            String response = serviceRequestPage.getResponseMessage();
            Reporter.log("   Submitted: joining " + joining + " | " + program + " | confirm '" + confirm + "' | " + response, true);
            Assert.assertFalse(response.isEmpty() || response.toUpperCase().contains("ERROR"), "❌ Center Shift not submitted: " + response);
            // the new row can take a few seconds to show on the grid
            for (int i = 0; i < 5 && status.isEmpty(); i++) {
                Thread.sleep(3000);
                status = requestsPage.getColumnValueByRequestType(CS_CHILD_ID, "Center Shift", "Request Status");
            }
            Reporter.log("   Request status after submit: " + status, true);
            Assert.assertEquals(status, "Pending", "❌ Center Shift not Pending after submit");
        }
        if (status.equalsIgnoreCase("Pending")) {
            String body = APIs.getCenterShiftPendingToProcessing(CS_CHILD_ID).getBody().asString();
            status = requestsPage.getColumnValueByRequestType(CS_CHILD_ID, "Center Shift", "Request Status");
            Reporter.log("   getAllPendingRequests → " + body.substring(0, Math.min(300, body.length())) + " | status " + status, true);
        }
        if (status.equalsIgnoreCase("Processing")) {
            String body = APIs.getCenterShiftProcessingToApproved(CS_CHILD_ID).getBody().asString();
            Reporter.log("   cronProcessCenterShiftRequests → " + body, true);
            if (body.contains("cannot be processed before"))
                throw new SkipException("Center Shift of " + CS_CHILD_ID + " is Processing; it cannot be processed before its attrition date — rerun then");
            status = requestsPage.getColumnValueByRequestType(CS_CHILD_ID, "Center Shift", "Request Status");
            Reporter.log("   Status after processing: " + status, true);
        }
        if (!status.equalsIgnoreCase("Approved"))
            throw new SkipException("Center Shift of " + CS_CHILD_ID + " is '" + status + "' — not processed yet");

        reopen();
        creditsPage.searchTable(CS_CHILD_NAME);
        List<String> ids = creditsPage.getCreditIds();
        for (String id : ids) Reporter.log("   " + id + ": " + creditsPage.getRowText(id) + " → " + creditsPage.getRowActionTitles(id), true);
        if (ids.isEmpty()) throw new SkipException("Center Shift Approved but no corporate credit for " + CS_CHILD_NAME + " in the default range");
        for (String id : ids)
            Assert.assertFalse(creditsPage.getRowActionTitles(id).contains("Revoke Credit"), "❌ Revoke offered on center shift credit " + id);
        Reporter.log("✅ SC_011_TC_013 step 3 PASSED (" + ids.size() + " center shift credit(s), no Revoke)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_007 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "SC_011_TC_007 — Search")
    public void tc007_search() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_007 — Search", true);
        Assert.assertTrue(creditsPage.getRowCount() > 0, "❌ No credits in the default range");
        String child = creditsPage.getPageColumnValues("Child Name").get(0);
        creditsPage.searchTable(child);
        List<String> names = creditsPage.getColumnValues("Child Name");
        Reporter.log("   Search '" + child + "' → " + names.size() + " rows | placeholder '" + creditsPage.getSearchPlaceholder() + "'", true);
        for (String n : names) Assert.assertTrue(n.contains(child), "❌ Unrelated row: " + n);
        creditsPage.searchTable("zzqq-no-such-credit");
        Assert.assertEquals(creditsPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_011_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 — Download Report
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "SC_011_TC_008 — Download Report")
    public void tc008_download() throws Exception {
        Reporter.log("▶ SC_011_TC_008 — Download Report", true);
        int expected = creditsPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        creditsPage.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified())) csv = f;
            if (csv == null) Thread.sleep(500);
        }
        Assert.assertNotNull(csv, "❌ No CSV downloaded");
        Thread.sleep(800);
        String header;
        int rows = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            header = r.readLine().replace("﻿", "");
            while (r.readLine() != null) rows++;
        }
        String expectedName = "Credits_Issued_Report_" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy")) + ".csv";
        Reporter.log("   File: " + csv.getName() + " | header " + header + " | rows " + rows + " (grid " + expected + ")", true);
        Assert.assertTrue(csv.getName().startsWith(expectedName.replace(".csv", "")), "❌ File name, expected " + expectedName);
        List<String> cols = new ArrayList<>();
        for (String c : header.split("\",\"")) cols.add(c.replace("\"", "").trim());
        Assert.assertEquals(cols, CSV_COLUMNS, "❌ CSV columns");
        Assert.assertEquals(rows, expected, "❌ CSV rows vs grid");
        Reporter.log("✅ SC_011_TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_009 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "SC_011_TC_009 — Pagination")
    public void tc009_pagination() {
        Reporter.log("▶ SC_011_TC_009 — Pagination", true);
        int rows = creditsPage.getRowCount();
        String info = creditsPage.getInfoText();
        Reporter.log("   " + info + " | per page " + creditsPage.getPageLength() + " | next enabled " + creditsPage.isNextPageEnabled()
                + " | previous enabled " + creditsPage.isPreviousPageEnabled(), true);
        Assert.assertEquals(creditsPage.getPageLength(), 10, "❌ Rows per page");
        Assert.assertTrue(creditsPage.isPaginationVisible(), "❌ Pagination");
        Assert.assertFalse(creditsPage.isPreviousPageEnabled(), "❌ Previous enabled on page 1");
        Assert.assertEquals(creditsPage.isNextPageEnabled(), rows > 10, "❌ Next state for " + rows + " rows");
        Assert.assertTrue(info.matches("Showing 1.\\d+ of " + rows + " entries") || rows == 0, "❌ Info text: " + info);
        Reporter.log("✅ SC_011_TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_010 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "SC_011_TC_010 — Sorting")
    public void tc010_sorting() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_010 — Sorting", true);
        creditsPage.clickColumnHeader("Amount");
        List<Double> asc = new ArrayList<>();
        for (String v : creditsPage.getPageColumnValues("Amount")) asc.add(Double.parseDouble(v.replace(",", "")));
        String ascClass = creditsPage.getHeaderSortClass("Amount");
        creditsPage.clickColumnHeader("Amount");
        List<Double> desc = new ArrayList<>();
        for (String v : creditsPage.getPageColumnValues("Amount")) desc.add(Double.parseDouble(v.replace(",", "")));
        String descClass = creditsPage.getHeaderSortClass("Amount");
        creditsPage.clickColumnHeader("Child Name");
        List<String> names = creditsPage.getPageColumnValues("Child Name");
        Reporter.log("   Amount " + ascClass + " " + asc + " | " + descClass + " " + desc + " | Child Name " + names, true);
        List<Double> sorted = new ArrayList<>(asc);
        sorted.sort(Comparator.naturalOrder());
        Assert.assertEquals(asc, sorted, "❌ Amount ascending");
        sorted.sort(Comparator.reverseOrder());
        Assert.assertEquals(desc, sorted, "❌ Amount descending");
        List<String> sortedNames = new ArrayList<>(names);
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
        Assert.assertEquals(names, sortedNames, "❌ Child Name ascending");
        Reporter.log("✅ SC_011_TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_016 — Credits only for corporate admissions
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "SC_011_TC_016 — Credits only for corporate admissions")
    public void tc016_nonCorporateChild() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_016 — Regular child " + NON_CORPORATE_CHILD_ID + " has no corporate invoices", true);
        creditsPage.openIssueCredits();
        creditsPage.enterChildIdAndFetch(NON_CORPORATE_CHILD_ID);
        String text = creditsPage.getPopupText();
        List<String> invoices = creditsPage.getPopupSelectOptions("credit_invoice_ref");
        Reporter.log("   " + creditsPage.getFetchedChildName() + " | invoices " + invoices + " | " + text, true);
        Assert.assertTrue(text.contains("Invoices not found."), "❌ 'Invoices not found.' not shown");
        Assert.assertEquals(invoices, List.of("Invoice Not found!"), "❌ Invoice list should be empty");
        creditsPage.selectPopupOptionContaining("credit_type_item", "Credits - Tuition Fee");
        creditsPage.clickPopupButton("apply_credit");
        Assert.assertEquals(creditsPage.getVisibleValidationMessage(), "Select Invoice Reference!", "❌ Submit not blocked");
        Reporter.log("✅ SC_011_TC_016 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_017 — Only corporate invoices offered
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "SC_011_TC_017 — Only the child's corporate invoices offered")
    public void tc017_corporateInvoicesOnly() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_017 — Invoice lists for child " + CHILD_ID, true);
        creditsPage.openIssueCredits();
        creditsPage.enterChildIdAndFetch(CHILD_ID);
        String childName = creditsPage.getFetchedChildName();
        List<String> addInvoices = creditsPage.getPopupSelectOptions("credit_invoice_ref");
        Reporter.log("   Issue Credits invoices: " + addInvoices, true);
        Assert.assertTrue(addInvoices.size() > 1, "❌ No invoices offered");
        for (String o : addInvoices.subList(1, addInvoices.size()))
            Assert.assertTrue(o.contains("(payment_due)") && o.matches(".*\\[PI/\\d+].*"), "❌ Non-due / non-corporate invoice offered: " + o);
        reopen();
        String anyCredit = null;
        for (String id : creditsPage.getCreditIds())
            if (creditsPage.getRowText(id).startsWith(childName + " |")) anyCredit = id;
        if (anyCredit == null) throw new SkipException("No credit of child " + CHILD_ID + " in the default range for the Link check");
        creditsPage.openLinkInvoice(anyCredit);
        List<String> linkInvoices = creditsPage.getPopupSelectOptions("credit_invoice_ref");
        Reporter.log("   Link Invoice Reference invoices (credit " + anyCredit + "): " + linkInvoices, true);
        for (String o : addInvoices.subList(1, addInvoices.size()))
            Assert.assertTrue(linkInvoices.stream().anyMatch(l -> l.contains("[" + invoiceNo(o) + "]")), "❌ Link list missing " + o);
        Reporter.log("✅ SC_011_TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_038 (new) — Date range over 31 days
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 17, description = "SC_011_TC_038 — Date range over 31 days")
    public void tc038_dateRangeOver31Days() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_038 — 31-day limit", true);
        LocalDate to = LocalDate.now().minusDays(1);
        creditsPage.setDateRange(to.minusDays(31), to);
        creditsPage.clickFilter();
        String okApplied = creditsPage.getAppliedFilters();
        String okAlert = creditsPage.getPageAlert();
        Reporter.log("   31 days " + to.minusDays(31) + " → " + to + " | applied " + okApplied + " | alert '" + okAlert + "'", true);
        Assert.assertTrue(okApplied.contains(to.minusDays(31).toString()) && okAlert.isEmpty(), "❌ 31-day range rejected");

        creditsPage.setDateRange(to.minusDays(32), to);
        creditsPage.clickFilter();
        String applied = creditsPage.getAppliedFilters();
        String alert = creditsPage.getPageAlert();
        Reporter.log("   32 days " + to.minusDays(32) + " → " + to + " | alert '" + alert + "' | applied " + applied + " | shown " + creditsPage.getDateValues(), true);
        Assert.assertEquals(alert, "Cannot select more than 31 days data", "❌ 32-day message");
        LocalDate today = LocalDate.now();
        Assert.assertTrue(applied.contains(today.minusMonths(1).toString()) && applied.contains(today.toString()),
                "❌ Default range should be restored: " + applied);
        Reporter.log("✅ SC_011_TC_038 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_039 (new) — Clear
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 18, description = "SC_011_TC_039 — Clear resets the filters")
    public void tc039_clear() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_039 — Clear", true);
        String defaults = creditsPage.getAppliedFilters();
        creditsPage.selectStatus("Successful");
        creditsPage.selectCenter(CENTER);
        creditsPage.clickFilter();
        Assert.assertEquals(creditsPage.getSelectedStatus(), "Successful", "❌ Status not applied");
        creditsPage.clickClear();
        Reporter.log("   After Clear: center " + creditsPage.getSelectedCenter() + " | status " + creditsPage.getSelectedStatus()
                + " | applied " + creditsPage.getAppliedFilters(), true);
        Assert.assertEquals(creditsPage.getSelectedCenter(), "All Center", "❌ Center not reset");
        Assert.assertEquals(creditsPage.getSelectedStatus(), "All", "❌ Status not reset");
        Assert.assertEquals(creditsPage.getAppliedFilters(), defaults, "❌ Dates not reset");
        Reporter.log("✅ SC_011_TC_039 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_040 (new) — Credit types
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "SC_011_TC_040 — Credit types offered")
    public void tc040_creditTypes() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_040 — Credit types", true);
        creditsPage.openIssueCredits();
        creditsPage.enterChildIdAndFetch(CHILD_ID);
        List<String> types = creditsPage.getPopupSelectOptions("credit_type_item");
        Reporter.log("   " + types, true);
        Assert.assertEquals(types.subList(1, types.size()), CREDIT_TYPES, "❌ Credit types");
        Assert.assertEquals(types.get(0), "Select Credit", "❌ Placeholder");
        Reporter.log("✅ SC_011_TC_040 PASSED", true);
    }
}
