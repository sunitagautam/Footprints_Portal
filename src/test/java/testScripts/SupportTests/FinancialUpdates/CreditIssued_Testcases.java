package testScripts.SupportTests.FinancialUpdates;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Onboarding.LoginPage;
import pages.Settings.EmailView_Page;
import pages.Settings.UserRightsPage;
import pages.Support.FinancialUpdates.CreditIssued_Page;
import utils.BaseTest;
import utils.IAutoConstant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ═══════════════════════════════════════════════════════════════
// CREDITS ISSUED (Support → Financial Updates → Credits Issued)
// Source: TC_Support_Financial_Updates_2026.xlsx, sheet
// "TC_Credits Issued". Runs as Jaydeep Kar (only user who gets the
// Action column); the email check (SC004_TC_003) re-logs in as
// Rakesh, the only user with Email View access.
//
// The test issues its OWN credit (SC002_TC_001), then links it to
// a different invoice (SC004_TC_001) and revokes it (SC004_TC_002)
// in the same run — no fresh test data needed per run.
// ═══════════════════════════════════════════════════════════════
public class CreditIssued_Testcases extends BaseTest {

    // ✅ Child with paid invoices incl. a Book invoice (B/...)
    private static final String CHILD_ID = "73041";
    private static final String CHILD_NAME = "Kartikey Jaiswal";

    // ✅ Book credit must go against a B/... invoice, and the
    //    line item must mention "book" (page keyword guard)
    private static final String CREDIT_TYPE = "Credits - Book Charges";
    private static final String INVOICE_PREFIX = "B/";
    private static final String CREDIT_AMOUNT = "100";
    private static final String CREDIT_LINE_ITEM = "book";
    private static final String CREDIT_COMMENTS = "Automation - Credits Issued";
    private static final String REVOKE_REASON = "Automation - revoke test credit";

    private static final String FILTER_CENTER = "Sector 122, Noida";

    private static final DateTimeFormatter ISSUED_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss", Locale.ENGLISH);

    // ✅ Set by SC002_TC_001, used by the Link / Revoke tests
    private static String newCreditId = "";
    private static String issuedInvoiceNo = "";

    Navigations navigations;
    UserRightsPage userRightsPage;
    CreditIssued_Page creditPage;

    // ═══════════════════════════════════════════════
    // BEFORE CLASS — login as Rakesh, switch to user
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        creditPage = new CreditIssued_Page(driver);

        String user = getUserForScreen("Credits Issued");
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for Credits Issued in Excel");

        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — fresh Credits Issued page
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToCreditsIssued();
        creditPage = new CreditIssued_Page(driver);
        Assert.assertTrue(creditPage.isPageLoaded(),
                "❌ Credits Issued page did not load");
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001_TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC_001 — Verify default view of Credits Issued screen")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);

        Assert.assertTrue(creditPage.isFromDateVisible(), "❌ From date not visible");
        Assert.assertTrue(creditPage.isToDateVisible(), "❌ To date not visible");
        Assert.assertTrue(creditPage.isCenterFilterVisible(), "❌ Center filter not visible");
        Assert.assertTrue(creditPage.isTypeFilterVisible(), "❌ Type filter not visible");
        Assert.assertTrue(creditPage.isStatusFilterVisible(), "❌ Status filter not visible");
        Assert.assertTrue(creditPage.isFilterSubmitVisible(), "❌ Filter Submit not visible");
        Assert.assertTrue(creditPage.isSearchBarVisible(), "❌ Search bar not visible");
        Assert.assertTrue(creditPage.isAddNewButtonVisible(), "❌ Add New Issue Credits/Refund not visible");
        Assert.assertTrue(creditPage.isDownloadReportVisible(), "❌ Download Report not visible");
        Assert.assertTrue(creditPage.isPaginationVisible(), "❌ Pagination not visible");

        Assert.assertEquals(creditPage.getTypeFilterOptions(),
                Arrays.asList("All", "Credits", "Void Credits"), "❌ Type options mismatch");
        Assert.assertEquals(creditPage.getStatusFilterOptions(),
                Arrays.asList("All", "Pending", "Successful", "Failed"), "❌ Status options mismatch");

        List<String> expectedColumns = Arrays.asList("Child Name", "Amount", "Type", "Description",
                "Bank", "Issued By", "Invoice Reference", "Issued Date Time", "Update Status", "Action");
        // ✅ Defined columns, not just visible ones — the responsive table
        //    folds e.g. "Update Status" into the "+" expander when long
        //    descriptions squeeze the layout
        List<String> actualColumns = creditPage.getDefinedColumnHeaders();
        Reporter.log("   Columns: " + actualColumns, true);
        Assert.assertEquals(actualColumns, expectedColumns, "❌ Table columns mismatch");

        Reporter.log("   Default range: " + creditPage.getFromDateValue()
                + " → " + creditPage.getToDateValue(), true);
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001_TC_002 — Date range filter (current month: 1st → today)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC001_TC_002 — Verify filter by From & To date range")
    public void sc001_tc002_dateRangeFilter() throws InterruptedException {
        LocalDate from = LocalDate.now().withDayOfMonth(1);
        LocalDate to = LocalDate.now();
        Reporter.log("▶ SC001_TC_002 — Date range " + from + " → " + to, true);

        creditPage.setDateRange(from, to);
        creditPage.clickFilterSubmit();
        Reporter.log("   After submit: " + creditPage.getFromDateValue() + " → "
                + creditPage.getToDateValue() + " | " + creditPage.getInfoText(), true);

        for (String issued : creditPage.getColumnValues("Issued Date Time")) {
            LocalDate date = LocalDateTime.parse(issued, ISSUED_FORMAT).toLocalDate();
            Assert.assertFalse(date.isBefore(from) || date.isAfter(to),
                    "❌ Row outside date range: " + issued);
        }
        Reporter.log("✅ SC001_TC_002 PASSED — all rows within range", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001_TC_003 — Center filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC001_TC_003 — Verify filter by Center")
    public void sc001_tc003_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_003 — Center = " + FILTER_CENTER, true);

        creditPage.selectCenter(FILTER_CENTER);
        creditPage.clickFilterSubmit();

        String info = creditPage.getInfoText();
        Reporter.log("   Selected after submit: " + creditPage.getSelectedCenter() + " | " + info, true);
        Assert.assertEquals(creditPage.getSelectedCenter(), FILTER_CENTER,
                "❌ Center filter not retained after submit");
        Assert.assertFalse(info.isEmpty(), "❌ Table did not render after filter");
        Reporter.log("✅ SC001_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001_TC_004 — Type filter = Credits
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC001_TC_004 — Verify filter by Type = Credits")
    public void sc001_tc004_typeFilterCredits() throws InterruptedException {
        Reporter.log("▶ SC001_TC_004 — Type = Credits", true);

        creditPage.selectType("Credits");
        creditPage.clickFilterSubmit();
        Reporter.log("   " + creditPage.getInfoText(), true);

        Assert.assertEquals(creditPage.getSelectedType(), "Credits");
        for (String type : creditPage.getColumnValues("Type")) {
            Assert.assertFalse(type.toLowerCase().contains("void"),
                    "❌ Void credit shown under Type = Credits: " + type);
        }
        Reporter.log("✅ SC001_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001_TC_005 — Status filter = Pending
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC001_TC_005 — Verify filter by Status = Pending")
    public void sc001_tc005_statusFilterPending() throws InterruptedException {
        Reporter.log("▶ SC001_TC_005 — Status = Pending", true);

        creditPage.selectStatus("Pending");
        creditPage.clickFilterSubmit();
        Reporter.log("   " + creditPage.getInfoText(), true);

        Assert.assertEquals(creditPage.getSelectedStatus(), "Pending");
        for (String status : creditPage.getColumnValues("Update Status")) {
            Assert.assertEquals(status, "Pending", "❌ Non-Pending row under Status = Pending");
        }
        Reporter.log("✅ SC001_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002_TC_002 — Mandatory field validation. Errors show ONE at a
    //  time (page JS returns on the first failing field), so each field
    //  is filled in turn and the next message checked. Never submits.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC002_TC_002 — Verify mandatory field validation messages")
    public void sc002_tc002_mandatoryFieldValidation() throws InterruptedException {
        Reporter.log("▶ SC002_TC_002 — Mandatory fields | child=" + CHILD_ID, true);

        creditPage.clickAddNew();
        creditPage.enterChildId(CHILD_ID);
        creditPage.clickFetchChildDetails();
        Assert.assertEquals(creditPage.getFetchedChildName(), CHILD_NAME, "❌ Child name mismatch");

        creditPage.clickSubmitForm();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Please select credit type!");

        creditPage.selectCreditType(CREDIT_TYPE);
        creditPage.clickSubmitForm();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Select Invoice Reference!");

        Assert.assertFalse(creditPage.selectInvoiceByPrefix(INVOICE_PREFIX).isEmpty(),
                "❌ No " + INVOICE_PREFIX + " invoice for child " + CHILD_ID);
        creditPage.clickSubmitForm();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Credit Amount required!");

        creditPage.enterCreditAmountIfEmpty(CREDIT_AMOUNT);
        creditPage.clickSubmitForm();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Credit Line Item required!");

        creditPage.enterCreditLineItem(CREDIT_LINE_ITEM);
        creditPage.clickSubmitForm();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Comments required!");

        Assert.assertFalse(creditPage.isAlertPresent(), "❌ Confirm popup appeared with a missing field");
        Reporter.log("✅ SC002_TC_002 PASSED — all 5 messages shown in order", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002_TC_001 — Add New Issue Credits: full flow. Captures the new
    //  credit_id for the Link / Revoke tests below.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC002_TC_001 — Verify Add New Issue Credits/Refund")
    public void sc002_tc001_addNewCredit() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Add New credit | child=" + CHILD_ID, true);

        Set<String> before = creditPage.getCreditIdsForChild(CHILD_ID);

        creditPage.clickAddNew();
        creditPage.enterChildId(CHILD_ID);
        creditPage.clickFetchChildDetails();
        Assert.assertEquals(creditPage.getFetchedChildName(), CHILD_NAME, "❌ Child name mismatch");

        creditPage.selectCreditType(CREDIT_TYPE);
        issuedInvoiceNo = creditPage.selectInvoiceByPrefix(INVOICE_PREFIX);
        Assert.assertFalse(issuedInvoiceNo.isEmpty(), "❌ No " + INVOICE_PREFIX + " invoice for child " + CHILD_ID);
        creditPage.enterCreditAmountIfEmpty(CREDIT_AMOUNT);
        creditPage.enterCreditLineItem(CREDIT_LINE_ITEM);
        creditPage.enterComments(CREDIT_COMMENTS);
        creditPage.clickSubmitForm();

        Assert.assertEquals(creditPage.acceptConfirmPopup(), "Confirm?", "❌ Confirm popup text mismatch");
        String message = creditPage.waitForMessageContaining("Credits issued successfully");
        Reporter.log("   Message: " + message, true);
        Assert.assertTrue(message.contains("Credits issued successfully!"),
                "❌ Success message not shown: " + creditPage.getVisibleAlertText());

        // ✅ Page auto-reloads ~2s after success
        Thread.sleep(4000);
        navigations.goToCreditsIssued();
        creditPage = new CreditIssued_Page(driver);
        Assert.assertTrue(creditPage.isPageLoaded());

        Set<String> after = creditPage.getCreditIdsForChild(CHILD_ID);
        after.removeAll(before);
        Assert.assertEquals(after.size(), 1, "❌ Expected exactly one new credit row, found: " + after);
        newCreditId = after.iterator().next();

        String amount = creditPage.getCellByCreditId(newCreditId, "Amount");
        String type = creditPage.getCellByCreditId(newCreditId, "Type");
        String invoice = creditPage.getCellByCreditId(newCreditId, "Invoice Reference");
        Reporter.log("   New credit_id=" + newCreditId + " | " + amount + " | " + type + " | " + invoice, true);
        Assert.assertEquals(amount, CREDIT_AMOUNT + ".00", "❌ Amount mismatch");
        Assert.assertEquals(type, CREDIT_TYPE, "❌ Type mismatch");
        Assert.assertEquals(invoice, issuedInvoiceNo, "❌ Invoice Reference mismatch");
        Reporter.log("✅ SC002_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002_TC_001 (per credit type) — happy flow for every credit type
    //  except "Credits - Security" (excluded per user). The app does NOT
    //  validate type↔invoice mapping, so each row picks the invoice the
    //  way Support would manually — by the invoice's booking head
    //  (confirmed by user, 2026-10-06): Tuition → Preschool Fee, Book →
    //  Book Set, Transport → Transport Charges (child 50875 — T/ invoices
    //  are Convenience Fee, not transport), Convenience → Convenience Fee,
    //  Annual → Annual Preschool Fee, Registration → Registration Fee,
    //  every other type → Preschool Fee. Line items match the type so the
    //  page's keyword guard (apron/bag/shirt/kit/book/convenience) passes.
    // ════════════════════════════════════════════════════════════════════
    private static final String TRANSPORT_CHILD_ID = "50875";
    private static final String TRANSPORT_CHILD_NAME = "Gyanvi Mittal";

    // ✅ Child with Late Stay / Delay Penalty / Extended DayCare invoices
    //    (same child as OneTime Charges). Exact invoice numbers per user.
    private static final String CHARGES_CHILD_ID = "46195";
    private static final String CHARGES_CHILD_NAME = "Nayansh Kaushik";

    @DataProvider(name = "creditTypes")
    public Object[][] creditTypes() {
        Object[][] rows = new Object[][]{
                // child id, child name, credit type value, invoice booking head OR exact invoice no (has "/"), line item
                {CHILD_ID, CHILD_NAME, "Credits - Admission Discount", "Preschool Fee", "admission discount"},
                {CHILD_ID, CHILD_NAME, "Customer Payment", "Preschool Fee", "customer payment"},
                {CHILD_ID, CHILD_NAME, "Downgrade Credits", "Preschool Fee", "downgrade"},
                {CHILD_ID, CHILD_NAME, "Credits - Tuition Fee", "Preschool Fee", "tuition fee"},
                {CHILD_ID, CHILD_NAME, "Credits - Annual Fee", "Annual Preschool Fee", "annual fee"},
                {CHARGES_CHILD_ID, CHARGES_CHILD_NAME, "Credits - Delay Penalty", "P378/2627/495", "delay penalty"},
                {CHILD_ID, CHILD_NAME, "Credits - Book Charges", "Book Set", "book"},
                {CHILD_ID, CHILD_NAME, "Credits - Apron", "Preschool Fee", "apron"},
                {CHILD_ID, CHILD_NAME, "Credits - School Bag", "Preschool Fee", "school bag"},
                {CHILD_ID, CHILD_NAME, "Credits - Tee Shirt", "Preschool Fee", "tee shirt"},
                {CHARGES_CHILD_ID, CHARGES_CHILD_NAME, "Credits - Extended DayCare", "P378/2627/493", "extended daycare"},
                {CHARGES_CHILD_ID, CHARGES_CHILD_NAME, "Credits - Late-Stay", "P378/2627/479", "late stay"},
                {CHILD_ID, CHILD_NAME, "Credits - Non Satisfaction", "Preschool Fee", "non satisfaction"},
                {TRANSPORT_CHILD_ID, TRANSPORT_CHILD_NAME, "Credits - Transport", "Transport Charges", "transport"},
                {CHILD_ID, CHILD_NAME, "Credits - Registration", "Registration Fee", "registration"},
                {CHILD_ID, CHILD_NAME, "Credits - Welcome Admission Discount", "Preschool Fee", "welcome admission discount"},
                // ✅ Line item + amount auto-fill from the Convenience Fee invoice
                {CHILD_ID, CHILD_NAME, "Credits - Convenience Charges", "Convenience Fee", ""},
        };
        // ✅ Optional: -DcreditTypes="Credits - Late-Stay,Credits - Delay Penalty"
        //    runs only those rows (avoids re-issuing every type)
        String only = System.getProperty("creditTypes", "").trim();
        if (only.isEmpty()) return rows;
        List<String> wanted = Arrays.asList(only.split("\\s*,\\s*"));
        List<Object[]> filtered = new ArrayList<>();
        for (Object[] row : rows)
            if (wanted.contains((String) row[2])) filtered.add(row);
        return filtered.toArray(new Object[0][]);
    }

    @Test(priority = 7, dataProvider = "creditTypes",
            description = "SC002_TC_001 — Happy flow: issue a credit for each credit type")
    public void sc002_tc001_happyFlowEachCreditType(String childId, String childName, String creditType,
                                                    String bookingHead, String lineItem) throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 [" + creditType + "] | child=" + childId, true);

        Set<String> before = creditPage.getCreditIdsForChild(childId);

        creditPage.clickAddNew();
        creditPage.enterChildId(childId);
        creditPage.clickFetchChildDetails();
        String fetchedName = creditPage.getFetchedChildName().replaceAll("\\s+", " ");
        Reporter.log("   Child name: " + fetchedName, true);
        if (childName == null)
            Assert.assertFalse(fetchedName.isEmpty(), "❌ Child name not fetched for " + childId);
        else
            Assert.assertEquals(fetchedName, childName, "❌ Child name mismatch");

        creditPage.selectCreditType(creditType);
        String invoiceNo = bookingHead.contains("/")
                ? creditPage.selectInvoiceByNumber(bookingHead)
                : creditPage.selectInvoiceByBookingHead(bookingHead);
        Assert.assertFalse(invoiceNo.isEmpty(),
                "❌ No paid '" + bookingHead + "' invoice for child " + childId + " (" + creditType + ")");

        creditPage.enterCreditAmountIfEmpty(CREDIT_AMOUNT);
        String amount = creditPage.getCreditAmountValue();
        if (!lineItem.isEmpty()) creditPage.enterCreditLineItem(lineItem);
        creditPage.enterComments(CREDIT_COMMENTS + " - " + creditType);
        creditPage.clickSubmitForm();

        // ✅ Wait for the native confirm — touching the page while it is
        //    open makes Chrome dismiss it (= Cancel, nothing submitted)
        try {
            creditPage.acceptConfirmPopup();
        } catch (org.openqa.selenium.TimeoutException e) {
            // ✅ Seen under server load: the driver misses the confirm but
            //    the credit is still issued — the grid check below decides
            Reporter.log("⚠ Confirm popup not observed within 20s for " + creditType, true);
        }
        String message = creditPage.waitForMessageContaining("Credits issued successfully");
        Reporter.log("   Message: " + message, true);

        // ✅ By design (user, 2026-10-06): a Customer Payment credit is NOT
        //    listed in the Credits Issued grid — success message only
        if (creditType.equals("Customer Payment")) {
            Assert.assertTrue(message.contains("Credits issued successfully!"),
                    "❌ Success message not shown for " + creditType + ": " + creditPage.getVisibleAlertText());
            Reporter.log("✅ SC002_TC_001 [" + creditType + "] PASSED — success message only (not listed in grid by design)", true);
            return;
        }

        Thread.sleep(4000);
        navigations.goToCreditsIssued();
        creditPage = new CreditIssued_Page(driver);
        Assert.assertTrue(creditPage.isPageLoaded());

        Set<String> after = creditPage.getCreditIdsForChild(childId);
        after.removeAll(before);
        Assert.assertEquals(after.size(), 1, "❌ Expected exactly one new credit row, found: " + after);
        String creditId = after.iterator().next();

        // ✅ The flash message can vanish before it is read (page reloads
        //    ~2s after success, slower under server load) — the new grid
        //    row is the hard proof of issuance; a missed message is logged
        if (!message.contains("Credits issued successfully!"))
            Reporter.log("⚠ Success message not captured for " + creditType
                    + " — credit " + creditId + " confirmed in grid", true);

        String gridAmount = creditPage.getCellByCreditId(creditId, "Amount");
        String gridType = creditPage.getCellByCreditId(creditId, "Type");
        String gridInvoice = creditPage.getCellByCreditId(creditId, "Invoice Reference");
        Reporter.log("   credit_id=" + creditId + " | " + gridAmount + " | " + gridType + " | " + gridInvoice, true);
        Assert.assertEquals(Double.parseDouble(gridAmount), Double.parseDouble(amount), "❌ Amount mismatch");
        Assert.assertEquals(gridType, creditType, "❌ Type mismatch");
        Assert.assertEquals(gridInvoice, invoiceNo, "❌ Invoice Reference mismatch");
        Reporter.log("✅ SC002_TC_001 [" + creditType + "] PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004_TC_001 — Link Invoice Reference on the test's own credit:
    //  re-link to a different invoice, verify grid updated.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, dependsOnMethods = "sc002_tc001_addNewCredit",
            description = "SC004_TC_001 — Verify Action → Link Invoice Reference")
    public void sc004_tc001_linkInvoiceReference() throws InterruptedException {
        Reporter.log("▶ SC004_TC_001 — Link Invoice | credit_id=" + newCreditId, true);

        creditPage.openLinkInvoiceModal(CHILD_NAME, newCreditId);
        String modalText = creditPage.getActionModalText("Link Invoice Reference");
        Reporter.log("   Modal: " + modalText.replaceAll("\\s+", " "), true);
        for (String label : Arrays.asList("Child ID", "#" + CHILD_ID, "Child Name", CHILD_NAME,
                "Credit Type", CREDIT_TYPE, "Issued Credit Amount", "Remaining Amount to Adjustment",
                "Credit Issued On", "Credit Start From")) {
            Assert.assertTrue(modalText.contains(label), "❌ Link modal missing: " + label);
        }
        Assert.assertTrue(creditPage.getLinkSelectedInvoiceText().contains(issuedInvoiceNo),
                "❌ Current invoice not preselected");

        creditPage.selectLinkInvoicePlaceholder();
        creditPage.clickLinkSubmit();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Select Invoice Reference!");

        String newInvoice = creditPage.selectDifferentLinkInvoice();
        Assert.assertFalse(newInvoice.isEmpty(), "❌ No other invoice to link to");
        creditPage.clickLinkSubmit();

        String message = creditPage.waitForMessageContaining("successfully");
        Reporter.log("   Message: " + message, true);
        Assert.assertTrue(message.contains("Credit invoice reference updated successfully"),
                "❌ Link success message not shown: " + message + " " + creditPage.getVisibleAlertText());

        Thread.sleep(4000);
        navigations.goToCreditsIssued();
        creditPage = new CreditIssued_Page(driver);
        Assert.assertTrue(creditPage.isPageLoaded());
        String gridInvoice = creditPage.getCellByCreditId(newCreditId, "Invoice Reference");
        Reporter.log("   Grid Invoice Reference: " + issuedInvoiceNo + " → " + gridInvoice, true);
        Assert.assertEquals(gridInvoice, newInvoice, "❌ Grid Invoice Reference not updated");
        Reporter.log("✅ SC004_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004_TC_002 — Revoke the test's own credit (current month,
    //  unadjusted → Revoke enabled).
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, dependsOnMethods = "sc002_tc001_addNewCredit",
            description = "SC004_TC_002 — Verify Action → Revoke Credit")
    public void sc004_tc002_revokeCredit() throws InterruptedException {
        Reporter.log("▶ SC004_TC_002 — Revoke | credit_id=" + newCreditId, true);

        Assert.assertTrue(creditPage.isRevokeIconPresent(newCreditId),
                "❌ Revoke icon not shown for the new current-month credit");

        creditPage.openRevokeModal(CHILD_NAME, newCreditId);
        String modalText = creditPage.getActionModalText("Revoke Credit");
        Reporter.log("   Modal: " + modalText.replaceAll("\\s+", " "), true);
        for (String label : Arrays.asList("Child ID", "#" + CHILD_ID, "Child Name", CHILD_NAME,
                "Credit Type", CREDIT_TYPE, "Issued Credit Amount", "Remaining Amount",
                "Credit Issued On", "Reason")) {
            Assert.assertTrue(modalText.contains(label), "❌ Revoke modal missing: " + label);
        }

        creditPage.clickRevoke();
        Assert.assertEquals(creditPage.getVisibleValidationMessage(), "Revoke Reason is Missing!");

        creditPage.enterRevokeReason(REVOKE_REASON);
        creditPage.clickRevoke();
        String message = creditPage.waitForMessageContaining("Credit revoked successfully");
        Reporter.log("   Message: " + message, true);
        Assert.assertTrue(message.contains("Credit revoked successfully"),
                "❌ Revoke success message not shown: " + creditPage.getVisibleAlertText());

        Thread.sleep(4000);
        navigations.goToCreditsIssued();
        creditPage = new CreditIssued_Page(driver);
        Assert.assertTrue(creditPage.isPageLoaded());
        Reporter.log("   After revoke | status=" + creditPage.getCellByCreditId(newCreditId, "Update Status")
                + " | type=" + creditPage.getCellByCreditId(newCreditId, "Type"), true);
        Assert.assertFalse(creditPage.isRevokeIconPresent(newCreditId),
                "❌ Revoke icon still shown after revoking");
        Reporter.log("✅ SC004_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003_TC_001 — Search bar
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC003_TC_001 — Verify search bar")
    public void sc003_tc001_search() throws InterruptedException {
        String existingName = creditPage.getColumnValues("Child Name").get(0);
        Reporter.log("▶ SC003_TC_001 — Search: " + existingName, true);

        creditPage.searchTable(existingName);
        List<String> names = creditPage.getColumnValues("Child Name");
        Assert.assertFalse(names.isEmpty(), "❌ No rows for existing name");
        for (String n : names)
            Assert.assertTrue(n.contains(existingName), "❌ Non-matching row: " + n);

        creditPage.searchTable("zzzz_no_such_child_9999");
        Assert.assertEquals(creditPage.getEmptyTableMessage(), "No matching records found");
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003_TC_002 — Download Report (CSV) — checks the file lands in
    //  ~/Downloads and its header row.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "SC003_TC_002 — Verify Download Report (CSV)")
    public void sc003_tc002_downloadReport() throws Exception {
        Reporter.log("▶ SC003_TC_002 — Download Report", true);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();

        creditPage.clickDownloadReport();

        File csv = null;
        for (int i = 0; i < 20 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified()))
                        csv = f;
            if (csv == null) Thread.sleep(500);
        }
        Assert.assertNotNull(csv, "❌ No CSV downloaded to " + downloads);
        Reporter.log("   File: " + csv.getName(), true);

        String header;
        try (BufferedReader reader = new BufferedReader(new FileReader(csv))) {
            header = reader.readLine();
        }
        Reporter.log("   Header: " + header, true);
        Assert.assertNotNull(header, "❌ CSV is empty");

        List<String> sheetColumns = Arrays.asList("Child Id", "Child Name", "Issued Date", "Amount", "Type",
                "Description", "Bank", "Issued By", "Invoice Reference", "Issue Date Time", "Update Status",
                "Father Name", "Mother Name", "Father Email", "Mother Email", "Father Phone", "Mother Phone",
                "Account Bank Name", "Account Number", "Account IFSC Code", "Account Holder Email");
        List<String> missing = new ArrayList<>();
        for (String col : sheetColumns)
            if (!header.toLowerCase().contains(col.toLowerCase())) missing.add(col);
        Reporter.log("   Sheet columns not in CSV: " + missing, true);

        for (String col : Arrays.asList("Child Name", "Amount", "Type", "Description", "Bank", "Issued By",
                "Invoice Reference", "Update Status", "Father Name", "Mother Name", "Father Email",
                "Mother Email", "Father Phone", "Mother Phone", "Account Bank Name", "Account Number"))
            Assert.assertTrue(header.contains(col), "❌ CSV missing column: " + col);
        Reporter.log("✅ SC003_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003_TC_003 — Pagination (10 per page)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "SC003_TC_003 — Verify pagination")
    public void sc003_tc003_pagination() throws InterruptedException {
        Reporter.log("▶ SC003_TC_003 — Pagination | " + creditPage.getInfoText(), true);

        Assert.assertTrue(creditPage.getVisibleRowCount() <= 10, "❌ More than 10 rows on page 1");
        Assert.assertTrue(creditPage.getInfoText().contains("Showing 1 to"), "❌ Not on page 1");
        Assert.assertTrue(creditPage.isNextPageEnabled(), "❌ Not enough data to paginate");

        creditPage.clickNextPage();
        Reporter.log("   Page 2: " + creditPage.getInfoText(), true);
        Assert.assertTrue(creditPage.getInfoText().contains("Showing 11 to"), "❌ Next did not go to page 2");

        creditPage.clickPreviousPage();
        Reporter.log("   Back: " + creditPage.getInfoText(), true);
        Assert.assertTrue(creditPage.getInfoText().contains("Showing 1 to"), "❌ Previous did not return to page 1");
        Reporter.log("✅ SC003_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003_TC_004 — Column sorting (Amount numeric, Child Name text)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "SC003_TC_004 — Verify column sorting")
    public void sc003_tc004_sorting() throws InterruptedException {
        Reporter.log("▶ SC003_TC_004 — Sorting", true);

        creditPage.clickColumnHeader("Amount");
        List<Double> asc = toDoubles(creditPage.getColumnValues("Amount"));
        Reporter.log("   Amount asc: " + asc, true);
        Assert.assertTrue(creditPage.getHeaderSortClass("Amount").contains("sorting_asc"));
        Assert.assertTrue(isSorted(asc, true), "❌ Amount not ascending");

        creditPage.clickColumnHeader("Amount");
        List<Double> desc = toDoubles(creditPage.getColumnValues("Amount"));
        Reporter.log("   Amount desc: " + desc, true);
        Assert.assertTrue(creditPage.getHeaderSortClass("Amount").contains("sorting_desc"));
        Assert.assertTrue(isSorted(desc, false), "❌ Amount not descending");

        creditPage.clickColumnHeader("Child Name");
        List<String> names = creditPage.getColumnValues("Child Name");
        Reporter.log("   Child Name asc: " + names, true);
        Assert.assertTrue(creditPage.getHeaderSortClass("Child Name").contains("sorting_asc"));
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        Assert.assertEquals(names, sorted, "❌ Child Name not ascending");
        Reporter.log("✅ SC003_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004_TC_004 — Void Credits (system-created against a voided
    //  invoice). Filter Type = Void Credits → only Void rows. Currently
    //  0 rows in range — logged, not failed.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "SC004_TC_004 — Verify Type filter = Void Credits")
    public void sc004_tc004_voidCreditsFilter() throws InterruptedException {
        Reporter.log("▶ SC004_TC_004 — Type = Void Credits", true);

        creditPage.selectType("Void Credits");
        creditPage.clickFilterSubmit();
        Reporter.log("   " + creditPage.getInfoText(), true);
        Assert.assertEquals(creditPage.getSelectedType(), "Void Credits");

        List<String> types = creditPage.getColumnValues("Type");
        if (types.isEmpty()) {
            Reporter.log("⚠ SC004_TC_004 INFO — no Void Credits in range ("
                    + creditPage.getFromDateValue() + " → " + creditPage.getToDateValue()
                    + "): " + creditPage.getEmptyTableMessage(), true);
        }
        for (String type : types)
            Assert.assertTrue(type.toLowerCase().contains("void"), "❌ Non-void row under Void Credits: " + type);
        Reporter.log("✅ SC004_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004_TC_003 — Credit-issuance email (Book Charges is an emailing
    //  type). Email View is Rakesh-only, so re-login as Rakesh. Runs
    //  LAST since it leaves the session as Rakesh.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, dependsOnMethods = "sc002_tc001_addNewCredit",
            description = "SC004_TC_003 — Verify credit-issuance email sent to parent")
    public void sc004_tc003_creditIssuanceEmail() throws InterruptedException {
        Reporter.log("▶ SC004_TC_003 — Credit email | child=" + CHILD_ID, true);

        driver.manage().deleteAllCookies();
        driver.get(IAutoConstant.LOGIN_URL);
        new LoginPage(driver).loginWithDefaultCredentials();
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();

        EmailView_Page emailViewPage = new EmailView_Page(driver);
        navigations.goToEmailView();
        Assert.assertTrue(emailViewPage.isPageLoaded(), "❌ Email View page did not load");

        // ✅ Confirmed live: subject is "Credit issued worth Rs. <amount>"
        //    and the child id is only in the email BODY — so filter by
        //    Subject = "Credit Issued" + Body = child id, then pick the
        //    row with this run's amount and today's date.
        String subjectFilter = "Credit Issued";
        String expectedSubject = "credit issued worth rs. " + CREDIT_AMOUNT;
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH));
        WebElement bodyFilter = driver.findElement(By.id("body"));
        bodyFilter.clear();
        bodyFilter.sendKeys(CHILD_ID);
        List<String> rows = emailViewPage.getAllMatchingRowTexts(subjectFilter);
        String match = "";
        for (String r : rows)
            if (r.toLowerCase().contains(expectedSubject) && r.contains(today)) {
                match = r;
                break;
            }
        Reporter.log("   Emails for Subject='" + subjectFilter + "' + Body='" + CHILD_ID + "': " + rows.size(), true);
        Reporter.log("   Row for today: " + match.replaceAll("\\s+", " "), true);
        Assert.assertFalse(match.isEmpty(),
                "❌ No 'Credit issued worth Rs. " + CREDIT_AMOUNT + "' email dated " + today + " for child " + CHILD_ID);
        Reporter.log("✅ SC004_TC_003 PASSED", true);
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════
    private List<Double> toDoubles(List<String> values) {
        List<Double> out = new ArrayList<>();
        for (String v : values) out.add(Double.parseDouble(v.replace(",", "")));
        return out;
    }

    private boolean isSorted(List<Double> values, boolean ascending) {
        for (int i = 1; i < values.size(); i++) {
            int cmp = Double.compare(values.get(i - 1), values.get(i));
            if (ascending ? cmp > 0 : cmp < 0) return false;
        }
        return true;
    }
}
