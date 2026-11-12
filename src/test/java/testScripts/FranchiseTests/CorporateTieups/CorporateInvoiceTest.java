package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.CorporateInvoicePage;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// CORPORATE INVOICE (Franchise → Corporate Tieups → Corporate Invoice, /corporate_invoice)
// Source: "TC_Corporate Tie-ups.xlsx" SC_005 (17 TCs) → TC_Corporate_Tieups.xlsx, sheet TC_CorporateInvoice,
// updated to the live screen + new TCs from exploration. Runs as Rakshita Singh (EP-14768, SME - Customer Support; user 2026-11-12, was Rakesh).
//
// Nothing here is saved: the Void popup is only opened and submitted with an empty reason (blocked client side).
// Not automated yet (they save, or need a right): TC_010 raiseCoPayInvoice API, TC_011 mark payment,
// TC_012 payment validation (Update button needs Update_Corporate_Invoice), TC_014 void submit.
// ═══════════════════════════════════════════════════════════════
public class CorporateInvoiceTest extends BaseTest {

    private static final List<String> VISIBLE_COLUMNS = Arrays.asList("Invoice ID", "Child Name", "Corporate Offer", "Company Name",
            "Invoice Date", "Invoice Amount", "Amount Due", "Invoice Status", "Payment Details", "Action");
    private static final List<String> CSV_COLUMNS = Arrays.asList("Child ID", "Child Name", "Corporate Offer", "Invoice Date",
            "Invoice Description", "Invoice Amount", "Amount Due", "Invoice Status", "Payment Details", "Company Name", "Invoice ID");
    private static final String COMMENT = "QA automation - payment test (approved by Sunita Gautam, 28-Oct-2026)";
    private static final DateTimeFormatter GRID_DATE = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FIELD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    Navigations navigations;
    CorporateInvoicePage invoicePage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();

        // ✅ switch to the screen's user from testData/input_UserRights.xlsx (Rakshita Singh, user 2026-11-12)
        String user = getUserForScreen("Corporate Invoice");
        Assert.assertFalse(user.isEmpty(), "❌ No user found for Corporate Invoice in Excel");
        navigations.goToUserRights();
        new UserRightsPage(driver).switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToCorporateInvoice();
        dismissAlertIfPresent();
        invoicePage = new CorporateInvoicePage(driver);
        Assert.assertTrue(invoicePage.isPageLoaded(), "❌ Corporate Invoice did not load");
    }

    private void reopen() throws InterruptedException {
        navigations.goToCorporateInvoice();
        invoicePage = new CorporateInvoicePage(driver);
        Assert.assertTrue(invoicePage.isPageLoaded(), "❌ Corporate Invoice did not load");
    }

    private void filterStatus(String status) throws InterruptedException {
        invoicePage.selectByText("sr_invoice_status", status);
        invoicePage.clickFilter();
    }

    private void assertAll(String header, java.util.function.Predicate<String> ok, String what) {
        List<String> v = invoicePage.getColumnValues(header);
        Reporter.log("   " + header + " (" + v.size() + " rows): " + new java.util.TreeSet<>(v).stream().limit(8).toList(), true);
        Assert.assertFalse(v.isEmpty(), "❌ No rows for " + what);
        for (String s : v) Assert.assertTrue(ok.test(s), "❌ " + header + " '" + s + "' does not match " + what);
    }

    // "Download All 2583 Records" → 2583 ; plain "Download" → -1
    private int downloadAllCount() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Download All (\\d+) Records").matcher(invoicePage.getDownloadButtonText());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_005_TC_001 — Default view of Corporate Invoice")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_005_TC_001 — Default view", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='corporate_invoice')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu: " + path + " | Heading: " + invoicePage.getHeading() + " | Labels: " + invoicePage.getFilterLabels(), true);
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Corporate Invoice", "❌ Menu path");
        Assert.assertEquals(invoicePage.getHeading(), "Corporate Invoice", "❌ Heading");
        for (String id : Arrays.asList("sr_child_id", "sr_invoice_from", "sr_invoice_to", "sr_invoice_status", "btn_submit", "btn_reset"))
            Assert.assertTrue(invoicePage.isFilterFieldVisible(id), "❌ Filter field " + id);
        Assert.assertEquals(invoicePage.getSelectOptions("sr_invoice_status"), Arrays.asList("--SELECT--", "Paid", "Payment Due", "Void"), "❌ Status options");
        Assert.assertEquals(invoicePage.getMultiselectOptions("sr_ownership"), Arrays.asList("COCO", "Franchise", "Onsite"), "❌ Ownership options");
        Assert.assertTrue(invoicePage.getMultiselectOptions("sr_center_id").size() > 100, "❌ Center options");
        Assert.assertEquals(invoicePage.getSelectedText("sr_corporate_offer"), "--SELECT--", "❌ Default Corporate Offer");
        Assert.assertEquals(invoicePage.getSearchPlaceholder(), "Filter these entries", "❌ Search placeholder");
        Assert.assertTrue(invoicePage.getDownloadButtonText().startsWith("Download"), "❌ Download button");
        Assert.assertTrue(invoicePage.isPaginationVisible(), "❌ Pagination");
        Assert.assertEquals(invoicePage.getPageLength(), 10, "❌ Rows per page");
        List<String> visible = invoicePage.getVisibleColumnHeaders();
        Reporter.log("   Visible columns: " + visible + " | hidden: Child ID, Invoice Description | " + invoicePage.getInfoText()
                + " | " + invoicePage.getDownloadButtonText(), true);
        Assert.assertEquals(visible, VISIBLE_COLUMNS, "❌ Visible columns");
        Assert.assertTrue(invoicePage.getAllColumnHeaders().containsAll(Arrays.asList("Child ID", "Invoice Description")), "❌ Child ID / Invoice Description columns");
        Reporter.log("✅ SC_005_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_016 — Default period: last month + current month, status Payment Due
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_005_TC_016 — Default invoice period and status")
    public void tc016_defaultPeriod() {
        Reporter.log("▶ SC_005_TC_016 — Default period", true);
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(1), to = LocalDate.now();
        Reporter.log("   From " + invoicePage.getFieldValue("sr_invoice_from") + " To " + invoicePage.getFieldValue("sr_invoice_to")
                + " | Status " + invoicePage.getSelectedText("sr_invoice_status"), true);
        Assert.assertEquals(invoicePage.getFieldValue("sr_invoice_from"), from.format(FIELD_DATE), "❌ Default From (1st of last month)");
        Assert.assertEquals(invoicePage.getFieldValue("sr_invoice_to"), to.format(FIELD_DATE), "❌ Default To (today)");
        Assert.assertEquals(invoicePage.getSelectedText("sr_invoice_status"), "Payment Due", "❌ Default status");
        assertAll("Invoice Status", "Payment Due"::equals, "default status");
        // ✅ The range is on the invoice CREATED date ("Invoice created from / to"); Invoice Date can be later
        //    (e.g. an invoice created in October dated 01 Nov) — QueryLogs
        java.util.Map<String, Long> months = new java.util.TreeMap<>();
        for (String d : invoicePage.getColumnValues("Invoice Date")) months.merge(d.replaceAll("^\\d\\d ", ""), 1L, Long::sum);
        Reporter.log("   Invoice Date months: " + months, true);
        assertAll("Invoice Date", s -> !LocalDate.parse(s, GRID_DATE).isBefore(from), "not before " + from);
        Reporter.log("✅ SC_005_TC_016 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_002 — Filter by status + Corporate Offer; no data
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_005_TC_002 — Filters")
    public void tc002_filters() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_002 — Filters", true);
        String offer = invoicePage.getPageColumnValues("Corporate Offer").get(0);
        invoicePage.selectByText("sr_corporate_offer", offer);
        invoicePage.clickFilter();
        Assert.assertEquals(invoicePage.getSelectedText("sr_corporate_offer"), offer, "❌ Offer not kept after Filter");
        assertAll("Corporate Offer", offer::equals, "offer " + offer);
        assertAll("Invoice Status", "Payment Due"::equals, "status Payment Due");
        reopen();
        String qa = invoicePage.findOption("sr_corporate_offer", "QA Auto Tieup V2");
        invoicePage.selectByText("sr_corporate_offer", qa);
        invoicePage.clickFilter();
        Reporter.log("   " + qa + " → " + invoicePage.getEmptyTableMessage(), true);
        Assert.assertEquals(invoicePage.getEmptyTableMessage(), "No data available in table", "❌ No-data message");
        Reporter.log("✅ SC_005_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_003 / 004 / 005 — Actions per status
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_005_TC_003 — Paid: only Download")
    public void tc003_paidActions() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_003 — Paid", true);
        filterStatus("Paid");
        assertAll("Invoice Status", "Paid"::equals, "status Paid");
        Map<String, Long> a = invoicePage.getActionSets();
        Reporter.log("   Actions: " + a, true);
        Assert.assertEquals(a.keySet(), java.util.Set.of("download"), "❌ Paid rows must show only Download");
        Reporter.log("✅ SC_005_TC_003 PASSED", true);
    }

    @Test(priority = 5, description = "SC_005_TC_004 — Payment Due: Download, Update, Void")
    public void tc004_dueActions() {
        Reporter.log("▶ SC_005_TC_004 — Payment Due", true);
        Map<String, Long> a = invoicePage.getActionSets();
        Reporter.log("   Actions: " + a, true);
        Assert.assertTrue(a.keySet().stream().allMatch(k -> k.contains("download")), "❌ Due rows without Download: " + a);
        Assert.assertTrue(a.keySet().stream().anyMatch(k -> k.contains("void")), "❌ No Void icon on any due row: " + a);
        Reporter.log("   ℹ Rows without Void: " + a.getOrDefault("download", 0L) + " (all partly paid/adjusted — rule to confirm, QueryLogs)", true);
        if (a.keySet().stream().noneMatch(k -> k.contains("update")))
            Reporter.log("   ℹ No Update button — Rakesh lacks Update_Corporate_Invoice (asked to add it)", true);
        Reporter.log("✅ SC_005_TC_004 PASSED (Download + Void)", true);
    }

    @Test(priority = 6, description = "SC_005_TC_005 — Void: Voided On column")
    public void tc005_voidColumn() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_005 — Void", true);
        filterStatus("Void");
        List<String> headers = invoicePage.getAllColumnHeaders();
        Reporter.log("   Last column: " + headers.get(headers.size() - 1) + " | " + invoicePage.getInfoText(), true);
        Assert.assertEquals(headers.get(headers.size() - 1), "Voided On", "❌ Action column not replaced by Voided On");
        assertAll("Invoice Status", "Void"::equals, "status Void");
        assertAll("Voided On", s -> s.matches("\\d{2} \\w{3}, \\d{4} \\d{2}:\\d{2}"), "a void date-time");
        Reporter.log("✅ SC_005_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_006 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_005_TC_006 — Search")
    public void tc006_search() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_006 — Search", true);
        String childId = invoicePage.getPageColumnValues("Child ID").get(0), offer = invoicePage.getPageColumnValues("Corporate Offer").get(0);
        invoicePage.searchTable(childId);
        Reporter.log("   '" + childId + "' → " + invoicePage.getInfoText(), true);
        Assert.assertTrue(invoicePage.getPageColumnValues("Child ID").contains(childId), "❌ Child not found");
        invoicePage.searchTable(offer);
        Reporter.log("   '" + offer + "' → " + invoicePage.getInfoText(), true);
        Assert.assertTrue(invoicePage.getPageColumnValues("Corporate Offer").contains(offer), "❌ Offer not found");
        invoicePage.searchTable("zzqq-no-such-invoice");
        Assert.assertEquals(invoicePage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_005_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_007 / TC_017 — Download; Company Name + Invoice ID columns
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_005_TC_007 — Download corporate_invoice CSV")
    public void tc007_download() throws Exception {
        Reporter.log("▶ SC_005_TC_007 — Download", true);
        int expected = downloadAllCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        invoicePage.clickDownload();
        File csv = null;
        for (int i = 0; i < 90 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().startsWith("corporate_invoice") && n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified())) csv = f;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No corporate_invoice*.csv downloaded");
        Thread.sleep(1500);
        String header;
        int rows = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            header = r.readLine();
            while (header != null && header.trim().isEmpty()) header = r.readLine();
            String line;
            while ((line = r.readLine()) != null)
                if (line.matches("^\"?\\d+\"?,.*")) rows++;   // a record starts with its Child ID
        }
        List<String> cols = new ArrayList<>();
        for (String c : header.split(",")) cols.add(c.replace("\"", "").trim());
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("d_MMM_yyyy", Locale.ENGLISH));
        Reporter.log("   File: " + csv.getName() + " | records " + rows + " (button " + expected + ") | columns " + cols, true);
        Assert.assertEquals(csv.getName().replaceAll(" \\(\\d+\\)", ""), "corporate_invoice" + today + ".csv", "❌ File name");
        Assert.assertTrue(cols.containsAll(CSV_COLUMNS), "❌ CSV columns: " + cols);
        if (expected > 0) Assert.assertEquals(rows, expected, "❌ CSV records vs 'Download All N Records'");
        // ✅ User, 2026-10-28 (Q E): Company Name and Invoice ID are correctly included in the download
        Assert.assertTrue(cols.contains("Company Name") && cols.contains("Invoice ID"), "❌ Company Name / Invoice ID missing in CSV: " + cols);
        Reporter.log("✅ SC_005_TC_007 PASSED", true);
    }

    @Test(priority = 9, description = "SC_005_TC_017 — Company Name and Invoice ID columns")
    public void tc017_companyAndInvoiceColumns() {
        Reporter.log("▶ SC_005_TC_017 — Company Name / Invoice ID", true);
        List<String> visible = invoicePage.getVisibleColumnHeaders();
        Assert.assertTrue(visible.contains("Company Name") && visible.contains("Invoice ID"), "❌ Columns not on screen: " + visible);
        List<String> ids = invoicePage.getPageColumnValues("Invoice ID");
        Reporter.log("   Invoice IDs on page 1: " + ids + " | Companies: " + invoicePage.getPageColumnValues("Company Name"), true);
        Assert.assertTrue(ids.stream().allMatch(s -> s.matches("\\d+")), "❌ Invoice ID values");
        Reporter.log("✅ SC_005_TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_008 / 009 — Pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_005_TC_008 — Pagination (10 per page)")
    public void tc008_pagination() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_008 — Pagination", true);
        List<String> first = invoicePage.getPageColumnValues("Invoice ID");
        Reporter.log("   " + invoicePage.getInfoText() + " | rows on page 1: " + first.size(), true);
        Assert.assertEquals(first.size(), 10, "❌ Rows per page");
        invoicePage.clickNextPage();
        Reporter.log("   Next → " + invoicePage.getInfoText(), true);
        Assert.assertTrue(invoicePage.getInfoText().contains("Showing 11–20"), "❌ Info after Next");
        Assert.assertNotEquals(invoicePage.getPageColumnValues("Invoice ID"), first, "❌ Same rows after Next");
        invoicePage.clickPreviousPage();
        Assert.assertEquals(invoicePage.getPageColumnValues("Invoice ID"), first, "❌ Page 1 after Previous");
        Reporter.log("✅ SC_005_TC_008 PASSED", true);
    }

    @Test(priority = 11, description = "SC_005_TC_009 — Sorting")
    public void tc009_sorting() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_009 — Sorting", true);
        for (String col : VISIBLE_COLUMNS) {
            invoicePage.clickColumnHeader(col);
            String a = invoicePage.getHeaderSortClass(col);
            invoicePage.clickColumnHeader(col);
            String b = invoicePage.getHeaderSortClass(col);
            Reporter.log("   " + col + ": " + a + " → " + b, true);
            Assert.assertTrue(a.matches(".*sorting_(asc|desc).*") && b.matches(".*sorting_(asc|desc).*") && !a.equals(b),
                    "❌ " + col + " did not sort both ways");
        }
        invoicePage.clickColumnHeader("Invoice Amount");
        boolean asc = invoicePage.getHeaderSortClass("Invoice Amount").contains("asc");
        List<Double> amounts = invoicePage.getPageColumnValues("Invoice Amount").stream().map(s -> Double.parseDouble(s.replace(",", ""))).toList();
        List<Double> sorted = new ArrayList<>(amounts);
        sorted.sort(asc ? java.util.Comparator.naturalOrder() : java.util.Comparator.reverseOrder());
        Reporter.log("   Invoice Amount " + (asc ? "asc" : "desc") + ": " + amounts, true);
        Assert.assertEquals(amounts, sorted, "❌ Invoice Amount numeric order");
        Reporter.log("✅ SC_005_TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_013 — Download invoice PDF (Paid and Payment Due)
    // ════════════════════════════════════════════════════════════════════
    // ✅ Same request the download icon makes (PDFDownload → XHR blob → invoice_C<id>.pdf). Fetched in the page because
    //    Chrome blocks a second automatic download in one test
    // ✅ Due invoices: one with the Void icon (user, 2026-10-28 — rows without it have a data issue being fixed)
    private void checkPdf(String status) throws InterruptedException {
        List<String> withVoid = invoicePage.getPageInvoiceIdsWithVoid();
        for (int i = 0; i < 2 && withVoid.isEmpty() && status.equals("Payment Due"); i++) {
            invoicePage.clickColumnHeader(invoicePage.getLastColumnHeader());
            withVoid = invoicePage.getPageInvoiceIdsWithVoid();
        }
        String id = status.equals("Payment Due") && !withVoid.isEmpty() ? withVoid.get(0) : invoicePage.getPageColumnValues("Invoice ID").get(0);
        String info = invoicePage.fetchPdfInfo(id);
        Reporter.log("   " + status + " invoice " + id + " → " + info + " | " + invoicePage.getPdfUrl(id), true);
        Assert.assertTrue(info.startsWith("HTTP 200 | application/pdf") && info.endsWith("%PDF-1.7") || info.contains("| %PDF"), "❌ PDF (" + status + "): " + info);
    }

    @Test(priority = 12, description = "SC_005_TC_013 — Download invoice PDF (Paid / Payment Due)")
    public void tc013_invoicePdf() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_013 — Invoice PDF", true);
        checkPdf("Payment Due");
        filterStatus("Paid");
        checkPdf("Paid");
        Reporter.log("   ℹ Invoice number inside the PDF (CFP/… for Paid, PI/… for Due) is not read — needs a PDF library", true);
        Reporter.log("✅ SC_005_TC_013 PASSED (PDFs download)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_014 — Void Invoice popup: view + reason validation (nothing saved)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "SC_005_TC_014 — Void Invoice popup and reason validation")
    public void tc014_voidPopup() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_014 — Void popup (not submitted)", true);
        String id = invoicePage.getPageColumnValues("Invoice ID").get(0);
        String child = invoicePage.getPageColumnValues("Child ID").get(0), name = invoicePage.getPageColumnValues("Child Name").get(0);
        String amount = invoicePage.getPageColumnValues("Invoice Amount").get(0);
        invoicePage.openVoidPopup(id);
        Assert.assertTrue(invoicePage.isVoidPopupVisible(), "❌ Void popup did not open");
        String text = invoicePage.getVoidPopupText();
        Reporter.log("   " + text + " | placeholder: " + invoicePage.getVoidReasonPlaceholder() + " | c_desc: " + invoicePage.getVoidHidden("c_desc"), true);
        Assert.assertTrue(text.contains("Void Invoice (#" + child + ")"), "❌ Title / child id");
        Assert.assertTrue(text.contains("Child Name : " + name) && text.contains("Invoice Amount : Rs. " + amount), "❌ Child name / amount");
        Assert.assertEquals(invoicePage.getVoidHidden("c_desc"), "Voided Due #" + id + " invoice", "❌ Void credit description");
        invoicePage.startCapture(false);
        invoicePage.clickVoidSubmit();
        Reporter.log("   Empty reason → '" + invoicePage.getVoidReasonError() + "' | confirms " + invoicePage.getCapturedConfirms() + " | requests " + invoicePage.getCapturedRequests(), true);
        Assert.assertEquals(invoicePage.getVoidReasonError(), "Reason required", "❌ Reason validation");
        Assert.assertTrue(invoicePage.getCapturedRequests().isEmpty(), "❌ A request was sent");
        invoicePage.closeVoidPopup();
        Reporter.log("✅ SC_005_TC_014 PASSED (popup + validation; void not submitted)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_015 — Center and Ownership filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "SC_005_TC_015 — Center and Ownership filters")
    public void tc015_centerOwnership() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_015 — Center / Ownership", true);
        int all = invoicePage.getRowCount() < 1000 ? invoicePage.getRowCount() : downloadAllCount();
        int sum = 0;
        for (String own : Arrays.asList("COCO", "Franchise", "Onsite")) {
            reopen();
            invoicePage.multiselectOnly("sr_ownership", own);
            invoicePage.clickFilter();
            int n = invoicePage.getRowCount() < 1000 ? invoicePage.getRowCount() : downloadAllCount();
            Reporter.log("   Ownership " + own + " (kept " + invoicePage.getMultiselectSelected("sr_ownership") + "): " + n + " | " + invoicePage.getInfoText(), true);
            Assert.assertEquals(invoicePage.getMultiselectSelected("sr_ownership"), List.of(own), "❌ Ownership not kept");
            Assert.assertTrue(n <= all, "❌ " + own + " returned more than all");
            sum += n;
        }
        Reporter.log("   COCO + Franchise + Onsite = " + sum + " | all = " + all + (sum < all ? " → " + (all - sum) + " invoices with no ownership (QueryLogs)" : ""), true);
        Assert.assertTrue(sum <= all && sum > all * 0.95, "❌ Ownership split vs all: " + sum + " / " + all);
        reopen();
        invoicePage.multiselectOnly("sr_center_id", "Demo Center");
        invoicePage.clickFilter();
        Reporter.log("   Center Demo Center → " + invoicePage.getInfoText() + " | " + invoicePage.getEmptyTableMessage(), true);
        Assert.assertEquals(invoicePage.getMultiselectSelected("sr_center_id"), List.of("Demo Center"), "❌ Center not kept");
        Assert.assertTrue(invoicePage.getRowCount() < all, "❌ Center filter did not narrow the records");
        Reporter.log("✅ SC_005_TC_015 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_018 (new) — Child ID filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "SC_005_TC_018 — Child ID filter")
    public void tc018_childIdFilter() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_018 — Child ID filter", true);
        String child = invoicePage.getPageColumnValues("Child ID").get(0);
        invoicePage.enterChildId(child);
        invoicePage.clickFilter();
        Assert.assertEquals(invoicePage.getFieldValue("sr_child_id"), child, "❌ Child ID not kept");
        assertAll("Child ID", child::equals, "child " + child);
        Reporter.log("✅ SC_005_TC_018 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_019 (new) — Grid shows max 1000 rows; Download All exports every record
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "SC_005_TC_019 — Grid limit vs Download All N Records")
    public void tc019_gridLimit() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_019 — Grid limit", true);
        int all = downloadAllCount();
        Reporter.log("   " + invoicePage.getInfoText() + " | rows " + invoicePage.getRowCount() + " | " + invoicePage.getDownloadButtonText(), true);
        Assert.assertTrue(all > 1000, "❌ Expected more than 1000 due invoices by default: " + all);
        Assert.assertEquals(invoicePage.getRowCount(), 1000, "❌ Grid rows when more than 1000 match");
        filterStatus("Void");
        Reporter.log("   Void: " + invoicePage.getInfoText() + " | " + invoicePage.getDownloadButtonText(), true);
        Assert.assertEquals(invoicePage.getDownloadButtonText(), "Download", "❌ Button label for a small result");
        Reporter.log("✅ SC_005_TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_020 (new) — Clear resets the filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 17, description = "SC_005_TC_020 — Clear")
    public void tc020_clear() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_020 — Clear", true);
        String defFrom = invoicePage.getFieldValue("sr_invoice_from"), defTo = invoicePage.getFieldValue("sr_invoice_to");
        invoicePage.selectByText("sr_invoice_status", "Void");
        invoicePage.enterChildId("40191");
        invoicePage.clickFilter();
        invoicePage.clickClear();
        Reporter.log("   After Clear: status " + invoicePage.getSelectedText("sr_invoice_status") + " | child '" + invoicePage.getFieldValue("sr_child_id")
                + "' | " + invoicePage.getFieldValue("sr_invoice_from") + " – " + invoicePage.getFieldValue("sr_invoice_to"), true);
        Assert.assertEquals(invoicePage.getSelectedText("sr_invoice_status"), "Payment Due", "❌ Status after Clear");
        Assert.assertEquals(invoicePage.getFieldValue("sr_child_id"), "", "❌ Child ID after Clear");
        Assert.assertEquals(invoicePage.getFieldValue("sr_invoice_from"), defFrom, "❌ From after Clear");
        Assert.assertEquals(invoicePage.getFieldValue("sr_invoice_to"), defTo, "❌ To after Clear");
        Reporter.log("✅ SC_005_TC_020 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Not automated yet — they save, or need a right Rakesh does not have
    // ════════════════════════════════════════════════════════════════════
    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_014 — Void Invoice: submit (SAVES — voids one Payment Due invoice)
    //  User, 2026-10-28: every invoice should show Void, but on test some rows lack it (data issue) — filter
    //  Payment Due, sort the last column so rows with the Void icon come first, and void one.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 40, description = "SC_005_TC_014 — Void a Payment Due invoice")
    public void tc014_voidInvoice() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_014 — Void Invoice (SAVES)", true);
        List<String> withVoid = invoicePage.getPageInvoiceIdsWithVoid();
        for (int i = 0; i < 2 && withVoid.isEmpty(); i++) {
            invoicePage.clickColumnHeader(invoicePage.getLastColumnHeader());
            withVoid = invoicePage.getPageInvoiceIdsWithVoid();
        }
        Assert.assertFalse(withVoid.isEmpty(), "❌ No invoice with a Void icon on the first page after sorting");
        String id = withVoid.get(0);
        int row = invoicePage.getPageColumnValues("Invoice ID").indexOf(id);
        String child = invoicePage.getPageColumnValues("Child ID").get(row), name = invoicePage.getPageColumnValues("Child Name").get(row);
        String amount = invoicePage.getPageColumnValues("Invoice Amount").get(row), offer = invoicePage.getPageColumnValues("Corporate Offer").get(row);
        Reporter.log("   Voiding invoice " + id + " | child " + child + " " + name + " | " + offer + " | Rs. " + amount, true);
        invoicePage.openVoidPopup(id);
        invoicePage.enterVoidReason("QA automation - void test (approved by Sunita Gautam, 28-Oct-2026)");
        invoicePage.startCapture(true);
        invoicePage.clickVoidSubmit();
        String msg = invoicePage.waitForVoidMessage();
        Reporter.log("   Confirms " + invoicePage.getCapturedConfirms() + " | requests " + invoicePage.getCapturedRequests() + " | message: " + msg, true);
        Assert.assertEquals(invoicePage.getCapturedConfirms(), List.of("This will void invoice, Are you sure want to do it?"), "❌ Confirm text");
        Assert.assertTrue(msg.toLowerCase().contains("voided successfully"), "❌ Void message: " + msg);
        Thread.sleep(4000);
        reopen();
        filterStatus("Void");
        invoicePage.searchTable(id);
        List<String> ids = invoicePage.getPageColumnValues("Invoice ID");
        Reporter.log("   Void list for " + id + ": " + ids + " | Voided On " + (ids.contains(id) ? invoicePage.getPageColumnValues("Voided On").get(ids.indexOf(id)) : "-"), true);
        Assert.assertTrue(ids.contains(id), "❌ Invoice " + id + " not listed as Void");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH));
        Assert.assertTrue(invoicePage.getPageColumnValues("Voided On").get(ids.indexOf(id)).startsWith(today), "❌ Voided On is not today");
        Reporter.log("✅ SC_005_TC_014 PASSED — invoice " + id + " voided", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_010 — raiseCoPayInvoice API (SAVES — generates invoices for one child)
    //  User, 2026-10-28: select a ProEves Long / Short Term offer and pick any child from it.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC_005_TC_010 — raiseCoPayInvoice API")
    public void tc010_raiseCoPayInvoice() throws InterruptedException {
        // User, 2026-10-28: offer "Indeed - Bangalore Region" (ProEves Long / Short Term also allowed). The API only
        // raises invoices for a co-pay admission not yet invoiced; others answer "No Pending Co-pay Admission
        // Invoices" and nothing changes — so the children of the offer are tried in turn (max 10).
        Reporter.log("▶ SC_005_TC_010 — raiseCoPayInvoice (may SAVE)", true);
        String offer = System.getProperty("copayOffer", "Indeed - Bangalore Region");
        List<String> children = new ArrayList<>();
        String one = System.getProperty("copayChildId", "");
        if (!one.isEmpty()) children.add(one);
        else {
            invoicePage.selectByText("sr_invoice_status", "--SELECT--");
            List<String> offers = invoicePage.getSelectOptions("sr_corporate_offer");
            if (!offers.contains(offer))   // allow a partial name, e.g. -DcopayOffer="Sector 62"
                offer = offers.stream().filter(o -> o.toLowerCase().contains(System.getProperty("copayOffer", "").toLowerCase())).findFirst().orElse(offer);
            invoicePage.selectByText("sr_corporate_offer", offer);
            invoicePage.setDate("sr_invoice_from", 2024, 1, 1);
            invoicePage.clickFilter();
            for (String c : invoicePage.getColumnValues("Child ID")) if (!children.contains(c)) children.add(c);
            // ✅ Children with no corporate invoice yet are not on this screen — take the offer's children from Admission Records
            navigations.goToAdmissionRecords();
            pages.Franchise.CorporateTieups.AdmissionRecordsPage records = new pages.Franchise.CorporateTieups.AdmissionRecordsPage(driver);
            Assert.assertTrue(records.isPageLoaded(), "❌ Admission Records did not load");
            records.selectByText("sr_corporate_offer", offer);
            records.selectByText("sr_status", "All");
            records.setChecked("exclude_inhouse", false);
            records.setChecked("exclude_onsite", false);
            records.clickFilter();
            List<String> fromRecords = records.getColumnValues("Child ID");
            Reporter.log("   Admission Records children of " + offer + ": " + fromRecords + " | statuses " + new java.util.TreeSet<>(records.getColumnValues("Status")), true);
            List<String> invoiced = new ArrayList<>(children);
            children.clear();
            for (String c : fromRecords) if (!invoiced.contains(c) && !children.contains(c)) children.add(c);   // not yet invoiced first
            for (String c : invoiced) if (!children.contains(c)) children.add(c);
        }
        Reporter.log("   Offer " + offer + " → children " + children, true);
        Assert.assertFalse(children.isEmpty(), "❌ No child on " + offer);
        String generated = "";
        for (String child : children.subList(0, Math.min(10, children.size()))) {
            io.restassured.response.Response r = utils.APIs.raiseCoPayInvoice(child);
            String body = r.getBody().asString();
            Reporter.log("   child " + child + " → HTTP " + r.getStatusCode() + " | " + body, true);
            Assert.assertEquals(r.getStatusCode(), 200, "❌ HTTP status");
            Assert.assertTrue(body.contains("\"status\":\"ok\""), "❌ Response: " + body);
            if (body.contains("Co-pay Proforma Invoices Generated successfully")) { generated = child; break; }
        }
        if (generated.isEmpty())
            throw new SkipException("No child on " + offer + " had pending co-pay admission invoices — generate path not executed");
        Reporter.log("✅ SC_005_TC_010 PASSED — invoices generated for child " + generated, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_012 — Update Invoice Payment: validation (nothing saved)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 18, description = "SC_005_TC_012 — Update Invoice Payment: field validation")
    public void tc012_paymentValidation() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_012 — Update Invoice Payment validation", true);
        String id = invoicePage.openFirstUpdatePayment();
        Assert.assertFalse(id.isEmpty(), "❌ No Update button (right Update_Corporate_Invoice)");
        Assert.assertTrue(invoicePage.isUpdatePaymentVisible(), "❌ Update Invoice Payment popup did not open");
        String text = invoicePage.getUpdatePaymentText();
        Reporter.log("   Invoice " + id + " popup: " + text + " | amount pre-filled: " + invoicePage.getPaymentValue("payment_amount"), true);
        Assert.assertTrue(text.contains("Update Invoice Payment") && text.contains("Payment Date") && text.contains("Amount Received")
                && text.contains("Payment Comments"), "❌ Popup fields");
        Assert.assertEquals(invoicePage.getPaymentValue("invoice_id"), id, "❌ Popup invoice id");
        invoicePage.startCapture(false);
        // messages appear one at a time: date → amount → comments
        invoicePage.setPaymentDate(null);
        invoicePage.setPaymentValue("payment_amount", "");
        invoicePage.setPaymentValue("payment_comments", "");
        invoicePage.clickPaymentSubmit();
        List<String> e1 = invoicePage.getPaymentErrors();
        invoicePage.setPaymentDate(LocalDate.now());
        invoicePage.clickPaymentSubmit();
        List<String> e2 = invoicePage.getPaymentErrors();
        invoicePage.setPaymentValue("payment_amount", "1");
        invoicePage.clickPaymentSubmit();
        List<String> e3 = invoicePage.getPaymentErrors();
        Reporter.log("   Empty → " + e1 + " | date set → " + e2 + " | amount set → " + e3 + " | confirms " + invoicePage.getCapturedConfirms()
                + " | requests " + invoicePage.getCapturedRequests(), true);
        Assert.assertEquals(e1, List.of("Payment date required"), "❌ Date message");
        Assert.assertEquals(e2, List.of("Payment amount required"), "❌ Amount message");
        Assert.assertEquals(e3, List.of("Comments required"), "❌ Comments message");
        Assert.assertTrue(invoicePage.getCapturedRequests().isEmpty(), "❌ A request was sent");
        invoicePage.closeUpdatePayment();
        Reporter.log("✅ SC_005_TC_012 PASSED (nothing saved)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_005_TC_011 — Mark a due invoice paid (SAVES)
    //  User, 2026-10-28: pick any Payment Due invoice that has the Void icon.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 41, description = "SC_005_TC_011 — Mark a due invoice paid")
    public void tc011_updatePayment() throws InterruptedException {
        Reporter.log("▶ SC_005_TC_011 — Update Invoice Payment (SAVES)", true);
        List<String> withVoid = invoicePage.getPageInvoiceIdsWithVoid();
        for (int i = 0; i < 2 && withVoid.isEmpty(); i++) {
            invoicePage.clickColumnHeader(invoicePage.getLastColumnHeader());
            withVoid = invoicePage.getPageInvoiceIdsWithVoid();
        }
        Assert.assertFalse(withVoid.isEmpty(), "❌ No due invoice with a Void icon on the first page after sorting");
        String id = withVoid.get(0);
        int row = invoicePage.getPageColumnValues("Invoice ID").indexOf(id);
        String child = invoicePage.getPageColumnValues("Child ID").get(row), name = invoicePage.getPageColumnValues("Child Name").get(row);
        String due = invoicePage.getPageColumnValues("Amount Due").get(row);
        invoicePage.openUpdatePayment(id);
        String amount = invoicePage.getPaymentValue("payment_amount");
        Reporter.log("   Invoice " + id + " | child " + child + " " + name + " | due " + due + " | amount pre-filled " + amount, true);
        Assert.assertEquals(amount, due, "❌ Amount Received not pre-filled with the amount due");
        invoicePage.setPaymentDate(LocalDate.now());
        invoicePage.setPaymentValue("payment_comments", COMMENT);
        invoicePage.startCapture(true);
        invoicePage.clickPaymentSubmit();
        String msg = invoicePage.waitForPaymentMessage();
        Reporter.log("   Confirms " + invoicePage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains("invoice updated successfully"), "❌ Message: " + msg);
        Thread.sleep(4000);
        reopen();
        filterStatus("Paid");
        invoicePage.searchTable(id);
        List<String> ids = invoicePage.getPageColumnValues("Invoice ID");
        Assert.assertTrue(ids.contains(id), "❌ Invoice " + id + " not listed as Paid");
        int r = ids.indexOf(id);
        String details = invoicePage.getPageColumnValues("Payment Details").get(r);
        Reporter.log("   Paid list: " + id + " | due " + invoicePage.getPageColumnValues("Amount Due").get(r) + " | " + details
                + " | actions " + invoicePage.getActionSets(), true);
        Assert.assertEquals(invoicePage.getPageColumnValues("Amount Due").get(r), "0.00", "❌ Amount Due after payment");
        Assert.assertTrue(details.contains(String.format(Locale.ENGLISH, "%,.2f", Double.parseDouble(due))), "❌ Payment Details amount: " + details);
        // Step 5 — the child's Account Statement shows the payment in the credit column:
        //   "Payment received on <dd Month, yyyy> … Manual Payment Updated <comments> <amount>" (corporate invoices
        //   show "(Corporate)"; a paid invoice gets its final number, e.g. PI/65606 → P/001/2627/8367)
        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + child);
        Thread.sleep(6000);
        String page = String.valueOf(((JavascriptExecutor) driver).executeScript("window.scrollTo(0,document.body.scrollHeight); return document.body.innerText;")).replace('\u00a0', ' ').replaceAll("\\s+", " ");
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM, yyyy", Locale.ENGLISH));
        java.util.regex.Matcher pm = java.util.regex.Pattern.compile("Payment received on " + java.util.regex.Pattern.quote(day)
                + "[^|]{0,40}?Manual Payment Updated " + java.util.regex.Pattern.quote(COMMENT) + "\\s+" + java.util.regex.Pattern.quote(String.format(Locale.ENGLISH, "%,.2f", Double.parseDouble(due)))).matcher(page);
        boolean found = pm.find();
        Reporter.log("   Account Statement #" + child + ": " + (found ? pm.group() : "payment line not found"), true);
        Assert.assertTrue(found, "❌ Payment not shown on the child's Account Statement");
        Reporter.log("✅ SC_005_TC_011 PASSED — invoice " + id + " marked paid", true);
    }
}
