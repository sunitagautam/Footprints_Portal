package testScripts.FranchiseTests.CorporateTieups;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.AdjustCorporateCreditsPage;
import pages.Franchise.CorporateTieups.CorporateInvoicePage;
import pages.Navigations;
import pages.Support.FinancialReports.AccountStatementPage;
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
// ADJUST CORPORATE CREDITS (Franchise → Corporate Tieups → Adjust Corporate Credits, /corporate_invoice?type=due)
// Source: "TC_Corporate Tie-ups.xlsx" SC_011_TC_019–037 → TC_Corporate_Tieups.xlsx, sheet TC_AdjustCorporateCredits.
// Runs as Rakesh. This class covers TC_019–033 (user, 2026-11-03).
//
// Saves (approved for corporate invoices, 28-Oct-2026): TC_031 marks one Payment Due invoice paid, TC_032 voids one —
// each takes an invoice with the Void icon.
// TC_034–037 (user, 03/04-Nov-2026): adjust the corporate credit balance against child 57826's invoice 66151, then revert
// it — the revert restores the invoice and the balance, so the same pair is reused every run (-DadjustChildId /
// -DadjustInvoiceId). The Revert icon needs the right Revert_Credit_Adjustment (given to Rakesh on 04-Nov-2026).
// ═══════════════════════════════════════════════════════════════
public class AdjustCorporateCreditsTest extends BaseTest {

    private static final List<String> VISIBLE_COLUMNS = Arrays.asList("Invoice ID", "Child Name", "Corporate Offer", "Company Name",
            "Invoice Date", "Invoice Amount", "Amount Due", "Invoice Status", "Payment Details", "Action");
    private static final List<String> CSV_COLUMNS = Arrays.asList("Child ID", "Child Name", "Corporate Offer", "Invoice Date",
            "Invoice Description", "Invoice Amount", "Amount Due", "Invoice Status", "Payment Details");
    private static final String COMMENT = "QA automation - Adjust Corporate Credits screen (approved by Sunita Gautam, 03-Nov-2026)";
    private static final DateTimeFormatter GRID_DATE = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);

    private static final String ADJUST_CHILD = System.getProperty("adjustChildId", "57826");
    private static final String ADJUST_INVOICE = System.getProperty("adjustInvoiceId", "66151");

    Navigations navigations;
    AdjustCorporateCreditsPage page;

    // State shared by the TC_034 → TC_037 chain
    private double dueBefore, balanceBefore, corpDuesBefore, adjusted;
    private boolean adjustDone;

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
        navigations.goToAdjustCorporateCredits();
        dismissAlertIfPresent();
        page = new AdjustCorporateCreditsPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Adjust Corporate Credits did not load");
    }

    private void filterStatus(String status) throws InterruptedException {
        page.selectByText("sr_invoice_status", status);
        page.clickFilter();
    }

    private void assertAll(String header, java.util.function.Predicate<String> ok, String what) {
        List<String> v = page.getColumnValues(header);
        Reporter.log("   " + header + " (" + v.size() + " rows): " + new java.util.TreeSet<>(v).stream().limit(8).toList(), true);
        Assert.assertFalse(v.isEmpty(), "❌ No rows for " + what);
        for (String s : v) Assert.assertTrue(ok.test(s), "❌ " + header + " '" + s + "' does not match " + what);
    }

    private int downloadAllCount() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Download All (\\d+) Records").matcher(page.getDownloadButtonText());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    // ✅ First invoice on page 1 with the Void icon (sorts the Action column when none is on page 1)
    private String firstInvoiceWithVoid() throws InterruptedException {
        List<String> withVoid = page.getPageInvoiceIdsWithVoid();
        for (int i = 0; i < 2 && withVoid.isEmpty(); i++) {
            page.clickColumnHeader(page.getLastColumnHeader());
            withVoid = page.getPageInvoiceIdsWithVoid();
        }
        if (withVoid.isEmpty()) throw new SkipException("No Payment Due invoice with the Void icon on the first page");
        return withVoid.get(0);
    }

    // ✅ This screen lists Payment Due only, so a paid / voided invoice is looked up on Corporate Invoice
    private List<String> corporateInvoiceRow(String invoiceId, String status) throws InterruptedException {
        navigations.goToCorporateInvoice();
        CorporateInvoicePage inv = new CorporateInvoicePage(driver);
        Assert.assertTrue(inv.isPageLoaded(), "❌ Corporate Invoice did not load");
        inv.selectByText("sr_invoice_status", status);
        inv.clickFilter();
        inv.searchTable(invoiceId);
        int i = inv.getPageColumnValues("Invoice ID").indexOf(invoiceId);
        return i < 0 ? List.of() : List.of(inv.getPageColumnValues("Invoice Status").get(i), inv.getPageColumnValues("Amount Due").get(i),
                inv.getPageColumnValues(inv.getLastColumnHeader()).get(i));
    }

    private void assertGoneFromDueList(String invoiceId) throws InterruptedException {
        reopen();
        page.searchTable(invoiceId);
        Assert.assertFalse(page.getPageColumnValues("Invoice ID").contains(invoiceId), "❌ Invoice " + invoiceId + " still listed as due");
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_019 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_011_TC_019 — Default view of Adjust Corporate Credits")
    public void tc019_defaultView() {
        Reporter.log("▶ SC_011_TC_019 — Default view", true);
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        Reporter.log("   Menu: " + page.getMenuPath() + " | Heading: " + page.getHeading() + " | Labels: " + page.getFilterLabels()
                + " | From " + page.getFieldValue("sr_invoice_from") + " To " + page.getFieldValue("sr_invoice_to")
                + " | Status " + page.getSelectedText("sr_invoice_status") + " | credit_adjust_data_required=" + page.getDownloadFilter("credit_adjust_data_required"), true);
        Assert.assertEquals(page.getMenuPath(), "Franchise > Corporate Tieups > Adjust Corporate Credits", "❌ Menu path");
        Assert.assertEquals(page.getHeading(), "Adjust Corporate Credits", "❌ Heading");
        for (String id : Arrays.asList("sr_child_id", "sr_corporate_offer", "sr_invoice_from", "sr_invoice_to", "sr_invoice_status", "btn_submit", "btn_reset"))
            Assert.assertTrue(page.isFilterFieldVisible(id), "❌ Filter field " + id);
        Assert.assertEquals(page.getSelectOptions("sr_invoice_status"), Arrays.asList("--SELECT--", "Paid", "Payment Due", "Void"), "❌ Status options");
        Assert.assertEquals(page.getSelectedText("sr_invoice_status"), "Payment Due", "❌ Default status");
        Assert.assertEquals(page.getFieldValue("sr_invoice_from"), from.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)), "❌ Default From");
        Assert.assertEquals(page.getDownloadFilter("credit_adjust_data_required"), "YES", "❌ Page is not in credit-adjust mode");
        Assert.assertTrue(page.getDownloadButtonText().startsWith("Download"), "❌ Download button");
        Assert.assertTrue(page.isPaginationVisible(), "❌ Pagination");
        Assert.assertEquals(page.getSearchPlaceholder(), "Filter these entries", "❌ Search box");
        List<String> visible = page.getVisibleColumnHeaders();
        Reporter.log("   Visible columns: " + visible + " | " + page.getInfoText() + " | actions " + page.getActionSets(), true);
        Assert.assertEquals(visible, VISIBLE_COLUMNS, "❌ Visible columns");
        Assert.assertTrue(page.getAllColumnHeaders().containsAll(Arrays.asList("Child ID", "Invoice Description")), "❌ Hidden Child ID / Invoice Description columns");
        assertAll("Invoice Status", "Payment Due"::equals, "default status");
        Reporter.log("✅ SC_011_TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_020 / 021 / 022 / 023 — Filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_011_TC_020 — Child ID filter")
    public void tc020_childFilter() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_020 — Child ID filter", true);
        String child = page.getPageColumnValues("Child ID").get(0);
        page.enterChildId(child);
        page.clickFilter();
        Assert.assertEquals(page.getFieldValue("sr_child_id"), child, "❌ Child ID not kept");
        assertAll("Child ID", child::equals, "child " + child);
        Reporter.log("✅ SC_011_TC_020 PASSED", true);
    }

    @Test(priority = 3, description = "SC_011_TC_021 — Corporate Offer filter")
    public void tc021_offerFilter() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_021 — Corporate Offer filter", true);
        String offer = page.getPageColumnValues("Corporate Offer").get(0);
        page.selectByText("sr_corporate_offer", offer);
        page.clickFilter();
        Assert.assertEquals(page.getSelectedText("sr_corporate_offer"), offer, "❌ Offer not kept after Filter");
        assertAll("Corporate Offer", offer::equals, "offer " + offer);
        reopen();
        page.selectByText("sr_corporate_offer", page.findOption("sr_corporate_offer", "QA Auto Tieup V2"));
        page.clickFilter();
        Reporter.log("   QA Auto Tieup V2 → " + page.getEmptyTableMessage(), true);
        Assert.assertEquals(page.getEmptyTableMessage(), "No data available in table", "❌ No-data message");
        Reporter.log("✅ SC_011_TC_021 PASSED", true);
    }

    @Test(priority = 4, description = "SC_011_TC_022 — Invoice Created date range")
    public void tc022_dateRange() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_022 — Date range", true);
        LocalDate from = LocalDate.now().withDayOfMonth(1), to = LocalDate.now();
        page.setDate("sr_invoice_from", from.getYear(), from.getMonthValue(), 1);
        page.setDate("sr_invoice_to", to.getYear(), to.getMonthValue(), to.getDayOfMonth());
        page.clickFilter();
        Reporter.log("   " + page.getFieldValue("sr_invoice_from") + " – " + page.getFieldValue("sr_invoice_to") + " → " + page.getInfoText()
                + " | invoice_date_from=" + page.getDownloadFilter("invoice_date_from"), true);
        Assert.assertEquals(page.getDownloadFilter("invoice_date_from"), from.toString(), "❌ From not applied");
        // ✅ The range is on the invoice CREATED date; Invoice Date can be later (same as Corporate Invoice)
        if (page.getRowCount() > 0)
            assertAll("Invoice Date", s -> !LocalDate.parse(s, GRID_DATE).isBefore(from), "not before " + from);
        else Reporter.log("   ℹ " + page.getEmptyTableMessage(), true);
        Reporter.log("✅ SC_011_TC_022 PASSED", true);
    }

    @Test(priority = 5, description = "SC_011_TC_023 — Invoice Status filter")
    public void tc023_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_023 — Invoice Status", true);
        filterStatus("Payment Due");
        Assert.assertEquals(page.getSelectedText("sr_invoice_status"), "Payment Due", "❌ Status not kept");
        assertAll("Invoice Status", "Payment Due"::equals, "Payment Due");
        // ✅ Live (03-Nov): this screen always lists Payment Due — choosing Paid or Void is reset to Payment Due on
        //    Filter (window.pageDownloadFilters.invoice_status stays payment_due). The dropdown still offers them (QueryLogs)
        for (String st : Arrays.asList("Paid", "Void")) {
            reopen();
            filterStatus(st);
            Reporter.log("   " + st + " → kept '" + page.getSelectedText("sr_invoice_status") + "' | " + page.getInfoText(), true);
            Assert.assertEquals(page.getDownloadFilter("invoice_status"), "payment_due", "❌ Screen did not stay on Payment Due for " + st);
            assertAll("Invoice Status", "Payment Due"::equals, "Payment Due only (" + st + " chosen)");
        }
        Reporter.log("✅ SC_011_TC_023 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_024 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_011_TC_024 — Search")
    public void tc024_search() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_024 — Search", true);
        String name = page.getPageColumnValues("Child Name").get(0), offer = page.getPageColumnValues("Corporate Offer").get(0);
        page.searchTable(name);
        Reporter.log("   '" + name + "' → " + page.getInfoText(), true);
        Assert.assertTrue(page.getPageColumnValues("Child Name").contains(name), "❌ Child name not found");
        page.searchTable(offer);
        Assert.assertTrue(page.getPageColumnValues("Corporate Offer").contains(offer), "❌ Offer not found");
        page.searchTable("zzqq-no-such-invoice");
        Assert.assertEquals(page.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_011_TC_024 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_025 / 027 — Download (all matching records)
    // ════════════════════════════════════════════════════════════════════
    private File download() throws InterruptedException {
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        page.clickDownload();
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
        return csv;
    }

    // → [columns..., "#records"]
    private List<String> readCsv(File csv) throws Exception {
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
        for (String c : header.split(",")) cols.add(c.replace("\"", "").replace("\uFEFF", "").trim());
        cols.add("#" + rows);
        return cols;
    }

    @Test(priority = 7, description = "SC_011_TC_025 — Download report")
    public void tc025_download() throws Exception {
        Reporter.log("▶ SC_011_TC_025 — Download", true);
        int grid = page.getRowCount();
        File csv = download();
        List<String> cols = readCsv(csv);
        int rows = Integer.parseInt(cols.remove(cols.size() - 1).substring(1));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("d_MMM_yyyy", Locale.ENGLISH));
        Reporter.log("   File: " + csv.getName() + " | records " + rows + " (grid " + grid + ") | columns " + cols, true);
        // ✅ Live (03-Nov): with up to 1000 rows the grid's own CSV export is used → "corporate_invoice.csv" (no date);
        //    "Download All N Records" (more than 1000) gives corporate_invoice<dd_Mon_yyyy>.csv (QueryLogs)
        Assert.assertTrue(csv.getName().replaceAll(" \\(\\d+\\)", "").matches("corporate_invoice(" + today + ")?\\.csv"), "❌ File name " + csv.getName());
        Assert.assertTrue(cols.containsAll(CSV_COLUMNS), "❌ CSV columns: " + cols);
        Assert.assertEquals(rows, grid, "❌ CSV records vs grid");
        Reporter.log("✅ SC_011_TC_025 PASSED", true);
    }

    @Test(priority = 8, description = "SC_011_TC_026 / TC_027 — Grid max 1000 rows; download has all records")
    public void tc026_gridLimitAndFullDownload() throws Exception {
        Reporter.log("▶ SC_011_TC_026 / 027 — 1000-row grid, full download", true);
        page.setDate("sr_invoice_from", 2026, 1, 1);
        page.clickFilter();
        int all = downloadAllCount();
        Reporter.log("   Due since 01 Jan 2026: " + page.getInfoText() + " | rows " + page.getRowCount() + " | " + page.getDownloadButtonText(), true);
        Assert.assertTrue(all > 1000, "❌ Expected more than 1000 due invoices: " + page.getDownloadButtonText());
        Assert.assertEquals(page.getRowCount(), 1000, "❌ Grid rows when more than 1000 match");
        File csv = download();
        List<String> cols = readCsv(csv);
        int rows = Integer.parseInt(cols.get(cols.size() - 1).substring(1));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("d_MMM_yyyy", Locale.ENGLISH));
        Reporter.log("   " + csv.getName() + " → " + rows + " records (button " + all + ")", true);
        Assert.assertEquals(csv.getName().replaceAll(" \\(\\d+\\)", ""), "corporate_invoice" + today + ".csv", "❌ File name");
        Assert.assertEquals(rows, all, "❌ Download does not have every record");
        Reporter.log("✅ SC_011_TC_026 / 027 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_028 / 029 — Pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC_011_TC_028 — Pagination (10 per page)")
    public void tc028_pagination() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_028 — Pagination", true);
        List<String> first = page.getPageColumnValues("Invoice ID");
        Reporter.log("   " + page.getInfoText() + " | rows on page 1: " + first.size(), true);
        Assert.assertEquals(page.getPageLength(), 10, "❌ Rows per page");
        Assert.assertEquals(first.size(), 10, "❌ Rows on page 1");
        page.clickNextPage();
        Reporter.log("   Next → " + page.getInfoText(), true);
        Assert.assertTrue(page.getInfoText().contains("Showing 11–20"), "❌ Info after Next");
        Assert.assertNotEquals(page.getPageColumnValues("Invoice ID"), first, "❌ Same rows after Next");
        page.clickPreviousPage();
        Assert.assertEquals(page.getPageColumnValues("Invoice ID"), first, "❌ Page 1 after Previous");
        Reporter.log("✅ SC_011_TC_028 PASSED", true);
    }

    @Test(priority = 10, description = "SC_011_TC_029 — Sorting")
    public void tc029_sorting() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_029 — Sorting", true);
        for (String col : VISIBLE_COLUMNS) {
            page.clickColumnHeader(col);
            String a = page.getHeaderSortClass(col);
            page.clickColumnHeader(col);
            String b = page.getHeaderSortClass(col);
            Reporter.log("   " + col + ": " + a + " → " + b, true);
            Assert.assertTrue(a.matches(".*sorting_(asc|desc).*") && b.matches(".*sorting_(asc|desc).*") && !a.equals(b),
                    "❌ " + col + " did not sort both ways");
        }
        page.clickColumnHeader("Amount Due");
        boolean asc = page.getHeaderSortClass("Amount Due").contains("asc");
        List<Double> amounts = page.getPageColumnValues("Amount Due").stream().map(s -> Double.parseDouble(s.replace(",", ""))).toList();
        List<Double> sorted = new ArrayList<>(amounts);
        sorted.sort(asc ? java.util.Comparator.naturalOrder() : java.util.Comparator.reverseOrder());
        Reporter.log("   Amount Due " + (asc ? "asc" : "desc") + ": " + amounts, true);
        Assert.assertEquals(amounts, sorted, "❌ Amount Due numeric order");
        Reporter.log("✅ SC_011_TC_029 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_030 — Download invoice PDF
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "SC_011_TC_030 — Download invoice PDF")
    public void tc030_invoicePdf() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_030 — Invoice PDF", true);
        String id = firstInvoiceWithVoid();
        String info = page.fetchPdfInfo(id);
        Reporter.log("   Invoice " + id + " → " + info + " | " + page.getPdfUrl(id), true);
        Assert.assertTrue(info.startsWith("HTTP 200 | application/pdf") && info.contains("%PDF"), "❌ PDF: " + info);
        Reporter.log("✅ SC_011_TC_030 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_033 — Adjust icon only on due invoices of children with a corporate credit balance
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "SC_011_TC_033 — Adjust credit icon on due invoices")
    public void tc033_adjustIcon() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_033 — Adjust icon", true);
        page.setDate("sr_invoice_from", 2026, 1, 1);
        page.clickFilter();
        Map<String, Long> a = page.getActionSets();
        List<String> adjust = page.getInvoiceIdsWithAdjust();
        Reporter.log("   Due since 01 Jan 2026: " + page.getInfoText() + " | actions " + a + " | Adjust on " + adjust, true);
        assertAll("Invoice Status", "Payment Due"::equals, "Payment Due");
        for (String id : adjust.subList(0, Math.min(3, adjust.size()))) {
            page.searchTable(id);   // tooltips are set up only on rendered rows
            String tip = page.getAdjustTooltip(id);
            Reporter.log("   " + id + " tooltip: " + tip, true);
            Assert.assertTrue(tip.matches("Adjust Invoice #" + id + " From Credit Balance Due Amount - Rs\\.[\\d,.]+ Credit Balance - Rs\\. [\\d,.]+"),
                    "❌ Tooltip: " + tip);
        }
        if (adjust.isEmpty())
            throw new SkipException("No due invoice of a child with a corporate credit balance on test now (57826 / 66151 was adjusted on 03-Nov)");
        Reporter.log("✅ SC_011_TC_033 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_031 — Update Invoice Payment (SAVES — marks one due invoice paid)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 40, description = "SC_011_TC_031 — Update Invoice Payment")
    public void tc031_updatePayment() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_031 — Update Invoice Payment (SAVES)", true);
        String id = firstInvoiceWithVoid();
        int row = page.getPageColumnValues("Invoice ID").indexOf(id);
        String child = page.getPageColumnValues("Child ID").get(row), due = page.getPageColumnValues("Amount Due").get(row);
        page.openUpdatePayment(id);
        Assert.assertTrue(page.isUpdatePaymentVisible(), "❌ Update Invoice Payment popup did not open");
        Reporter.log("   Invoice " + id + " | child " + child + " | due " + due + " | popup: " + page.getUpdatePaymentText(), true);
        page.setPaymentDate(LocalDate.now());
        if (page.getPaymentValue("payment_amount").isEmpty()) page.setPaymentValue("payment_amount", due);
        page.setPaymentValue("payment_comments", COMMENT);
        page.clickPaymentSubmit();
        String msg = page.waitForPaymentMessage();
        Reporter.log("   Message: " + msg, true);
        Assert.assertTrue(msg.contains("Invoice Updated Successfully"), "❌ Payment message: " + msg);
        Thread.sleep(3000);
        assertGoneFromDueList(id);
        List<String> saved = corporateInvoiceRow(id, "Paid");
        Reporter.log("   Corporate Invoice (Paid) " + id + ": " + saved, true);
        Assert.assertFalse(saved.isEmpty(), "❌ Invoice " + id + " not listed as Paid on Corporate Invoice");
        Assert.assertEquals(saved.get(1), "0.00", "❌ Amount Due after payment");
        Reporter.log("✅ SC_011_TC_031 PASSED — invoice " + id + " marked paid", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_032 — Void Invoice (SAVES — voids one due invoice)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 41, description = "SC_011_TC_032 — Void Invoice")
    public void tc032_voidInvoice() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_032 — Void Invoice (SAVES)", true);
        String id = firstInvoiceWithVoid();
        int row = page.getPageColumnValues("Invoice ID").indexOf(id);
        String child = page.getPageColumnValues("Child ID").get(row);
        page.openVoidPopup(id);
        Assert.assertTrue(page.isVoidPopupVisible(), "❌ Void popup did not open");
        String text = page.getVoidPopupText();
        Reporter.log("   Invoice " + id + " | popup: " + text, true);
        Assert.assertTrue(text.contains("Void Invoice (#" + child + ")"), "❌ Void popup title");
        page.enterVoidReason(COMMENT);
        page.startCapture(true);
        page.clickVoidSubmit();
        String msg = page.waitForVoidMessage();
        Reporter.log("   Confirms " + page.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertEquals(page.getCapturedConfirms(), List.of("This will void invoice, Are you sure want to do it?"), "❌ Confirm text");
        Assert.assertTrue(msg.toLowerCase().contains("voided successfully"), "❌ Void message: " + msg);
        Thread.sleep(4000);
        assertGoneFromDueList(id);
        List<String> saved = corporateInvoiceRow(id, "Void");
        Reporter.log("   Corporate Invoice (Void) " + id + ": " + saved, true);
        Assert.assertFalse(saved.isEmpty(), "❌ Invoice " + id + " not listed as Void on Corporate Invoice");
        Reporter.log("✅ SC_011_TC_032 PASSED — invoice " + id + " voided", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_011_TC_034 – TC_037 — Adjust the credit balance against an invoice, then revert it (SAVES, self-restoring)
    // ════════════════════════════════════════════════════════════════════
    private void openAdjustInvoice() throws InterruptedException {
        reopen();
        page.enterChildId(ADJUST_CHILD);
        page.clickFilter();
        Assert.assertTrue(page.getColumnValues("Invoice ID").contains(ADJUST_INVOICE), "❌ Invoice " + ADJUST_INVOICE + " not listed as due for child " + ADJUST_CHILD);
    }

    private double amountDue() {
        int i = page.getColumnValues("Invoice ID").indexOf(ADJUST_INVOICE);
        return Double.parseDouble(page.getColumnValues("Amount Due").get(i).replace(",", ""));
    }

    private AccountStatementPage openAccountStatement() throws InterruptedException {
        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + ADJUST_CHILD);
        Thread.sleep(6000);
        dismissAlertIfPresent();
        return new AccountStatementPage(driver);
    }

    @Test(priority = 50, description = "SC_011_TC_034 — Adjust invoice from the corporate credit balance")
    public void tc034_adjustInvoice() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_034 — Adjust invoice " + ADJUST_INVOICE + " (child " + ADJUST_CHILD + ")", true);
        openAdjustInvoice();
        List<String> actions = page.getRowActions(ADJUST_INVOICE);
        if (actions.contains("revert"))
            throw new SkipException("Invoice " + ADJUST_INVOICE + " is already adjusted (Revert icon shown) — revert it first");
        Assert.assertTrue(actions.contains("adjust"), "❌ No Adjust icon on " + ADJUST_INVOICE + " — child has no corporate credit balance? " + actions);
        page.searchTable(ADJUST_INVOICE);
        String tip = page.getAdjustTooltip(ADJUST_INVOICE);
        dueBefore = amountDue();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Credit Balance - Rs\\. ([\\d,.]+)").matcher(tip);
        Assert.assertTrue(m.find(), "❌ Tooltip: " + tip);
        double tipBalance = Double.parseDouble(m.group(1).replace(",", ""));
        AccountStatementPage acct = openAccountStatement();
        balanceBefore = acct.getCorporateSummaryAmount("Corporate Credit Balance");
        corpDuesBefore = acct.getCorporateSummaryAmount("Corporate Dues");
        Reporter.log("   Before: due " + dueBefore + " | tooltip '" + tip + "' | Account Statement: Corporate Credit Balance " + balanceBefore
                + ", Corporate Dues " + corpDuesBefore, true);
        Assert.assertEquals(balanceBefore, tipBalance, 0.01, "❌ Tooltip balance vs Account Statement");
        openAdjustInvoice();
        page.startCapture(true);
        page.startAjaxCapture();
        String response = page.clickAdjust(ADJUST_INVOICE);
        adjustDone = response.contains("\"status\":\"ok\"");
        Reporter.log("   Confirms " + page.getCapturedConfirms() + " | response " + response + " | icon text '" + page.getAdjustIconText(ADJUST_INVOICE)
                + "' | message '" + page.getUpdateMessage() + "'", true);
        Assert.assertEquals(page.getCapturedConfirms(), List.of("This will adjust invoice if any credits lying in corporate account, Are you sure you want to do it?"), "❌ Confirm text");
        Assert.assertTrue(response.contains("\"status\":\"ok\"") && response.contains("Corporate Credits Adjusted"), "❌ Adjust response: " + response);
        Assert.assertEquals(page.getAdjustIconText(ADJUST_INVOICE), "Corporate Credits Adjusted", "❌ Result shown in the Action column");
        adjusted = Math.min(balanceBefore, dueBefore);
        Reporter.log("✅ SC_011_TC_034 PASSED — Rs. " + adjusted + " adjusted", true);
    }

    @Test(priority = 51, dependsOnMethods = "tc034_adjustInvoice", description = "SC_011_TC_035 — Due amount and credit balance after adjustment")
    public void tc035_dueAfterAdjustment() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_035 — Due = due − credit adjusted", true);
        openAdjustInvoice();
        double due = amountDue();
        AccountStatementPage acct = openAccountStatement();
        double balance = acct.getCorporateSummaryAmount("Corporate Credit Balance"), corpDues = acct.getCorporateSummaryAmount("Corporate Dues");
        Reporter.log("   Amount Due " + dueBefore + " → " + due + " | Corporate Credit Balance " + balanceBefore + " → " + balance
                + " | Corporate Dues " + corpDuesBefore + " → " + corpDues, true);
        Assert.assertEquals(due, dueBefore - adjusted, 0.01, "❌ Amount Due after adjustment");
        Assert.assertEquals(balance, balanceBefore - adjusted, 0.01, "❌ Corporate Credit Balance after adjustment");
        Assert.assertEquals(corpDues, corpDuesBefore - adjusted, 0.01, "❌ Corporate Dues after adjustment");
        Reporter.log("✅ SC_011_TC_035 PASSED", true);
    }

    @Test(priority = 52, dependsOnMethods = "tc034_adjustInvoice", description = "SC_011_TC_036 — Adjusted invoice: regenerated PDF, Revert icon")
    public void tc036_adjustedInvoiceRow() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_036 — Adjusted invoice row", true);
        openAdjustInvoice();
        List<String> actions = page.getRowActions(ADJUST_INVOICE);
        String pdf = page.getPdfUrl(ADJUST_INVOICE), tip = page.getRevertTooltip(ADJUST_INVOICE), info = page.fetchPdfInfo(ADJUST_INVOICE);
        Reporter.log("   Actions " + actions + " | revert tooltip '" + tip + "' | PDF " + pdf + " → " + info, true);
        Assert.assertEquals(actions, List.of("download", "update", "revert"), "❌ Actions on an adjusted invoice (no Void / Adjust, Revert shown)");
        Assert.assertEquals(tip, "Revert Corporate Credit Adjustment for Unpaid Invoice #" + ADJUST_INVOICE, "❌ Revert tooltip");
        Assert.assertTrue(pdf.contains("irg=r"), "❌ PDF link is not the regenerated invoice (irg=r): " + pdf);
        Assert.assertTrue(info.startsWith("HTTP 200 | application/pdf"), "❌ Regenerated PDF: " + info);
        Reporter.log("   ℹ Credit line items in the invoice are not read (needs a PDF library); the Account Statement ledger still shows the original amount until payment", true);
        Reporter.log("✅ SC_011_TC_036 PASSED", true);
    }

    @Test(priority = 53, dependsOnMethods = "tc034_adjustInvoice", alwaysRun = true, description = "SC_011_TC_037 — Revert adjusted credit")
    public void tc037_revertAdjustedCredit() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_037 — Revert adjusted credit (needs Revert_Credit_Adjustment)", true);
        if (!adjustDone) throw new SkipException("TC_034 did not adjust the invoice — nothing to revert");
        openAdjustInvoice();
        Assert.assertTrue(page.getRowActions(ADJUST_INVOICE).contains("revert"), "❌ No Revert icon on " + ADJUST_INVOICE + " — right Revert_Credit_Adjustment missing?");
        page.startCapture(true);
        page.startAjaxCapture();
        String response = page.clickRevert(ADJUST_INVOICE);
        Reporter.log("   Confirms " + page.getCapturedConfirms() + " | response " + response + " | message '" + page.getUpdateMessage() + "'", true);
        Assert.assertEquals(page.getCapturedConfirms(), List.of("Are you sure want to revert corporate credit adjustment from invoice?"), "❌ Confirm text");
        Assert.assertTrue(response.contains("\"status\":\"ok\"") && response.contains("Corporate Credit reversal successfully completed"), "❌ Revert response: " + response);
        openAdjustInvoice();
        double due = amountDue();
        List<String> actions = page.getRowActions(ADJUST_INVOICE);
        AccountStatementPage acct = openAccountStatement();
        double balance = acct.getCorporateSummaryAmount("Corporate Credit Balance"), corpDues = acct.getCorporateSummaryAmount("Corporate Dues");
        Reporter.log("   After revert: Amount Due " + due + " | actions " + actions + " | Corporate Credit Balance " + balance + " | Corporate Dues " + corpDues, true);
        Assert.assertEquals(due, dueBefore, 0.01, "❌ Amount Due not restored");
        Assert.assertEquals(actions, List.of("download", "update", "void", "adjust"), "❌ Actions after revert");
        Assert.assertEquals(balance, balanceBefore, 0.01, "❌ Corporate Credit Balance not restored");
        Assert.assertEquals(corpDues, corpDuesBefore, 0.01, "❌ Corporate Dues not restored");
        Reporter.log("✅ SC_011_TC_037 PASSED — invoice " + ADJUST_INVOICE + " back to Rs. " + due + ", balance Rs. " + balance, true);
    }
}
