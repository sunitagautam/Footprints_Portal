package testScripts.SupportTests.FinancialUpdates;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.FinancialUpdates.UpdatePaymentsPage;
import utils.BaseTest;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ═══════════════════════════════════════════════════════════════
// UPDATE PAYMENTS (Support → Financial Updates → Update Payments, /invoices?type=due)
// Source: "TC_Support_Financial_Updates_2026 (1).xlsx", sheet "TC_Update Payments" (19 TCs),
// updated to the live screen (2026-11-18) + new TCs from exploration → TC_Support_Financial_Updates.xlsx,
// sheet TC_UpdatePayments. Runs as Rakesh (no user switch, user 2026-11-18).
//
// Saves (approved by the user, 2026-11-18): SC005_TC_001 adjusts one invoice from credit balance,
// SC005_TC_002 voids one fully-due invoice, SC005_TC_004 adjusts one invoice from security deposit.
// ═══════════════════════════════════════════════════════════════
public class UpdatePaymentsTest extends BaseTest {

    private static final List<String> VISIBLE_COLUMNS = Arrays.asList("Invoice #", "Invoice Series", "Child Name", "Center",
            "Amount", "Paid Amount", "Status", "Invoice Date", "Admission Status", "Due Date", "Action");
    // Payment Date is a hidden column since the 19-Nov redesign
    private static final List<String> HIDDEN_COLUMNS = Arrays.asList("Child #", "Room", "Program", "Payment Date");
    private static final String VOID_REASON = "QA automation - void test (approved by Sunita Gautam, 18-Nov-2026)";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    Navigations navigations;
    UpdatePaymentsPage page;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws InterruptedException {
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
        navigations.goToUpdatePayments();
        dismissAlertIfPresent();
        page = new UpdatePaymentsPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Update Payments did not load");
        // The server keeps the last filter in the session (seen 19-Nov) — start every test from the defaults
        if (!page.getAdmissionId().isEmpty() || !"Due, Overdue".equals(page.getMultiselectButtonText("sr_invoice_status"))
                || !"Current Year".equals(page.getSelectedValue("filter_month"))
                || !"No Filters".equals(page.getMultiselectButtonText("sr_center_id"))
                || !"No Filters".equals(page.getMultiselectButtonText("sr_ownership"))
                || !"No Filters".equals(page.getMultiselectButtonText("sr_city_id"))) {
            page.clickReset();
            page = new UpdatePaymentsPage(driver);
        }
    }

    private static double amount(String s) {
        String v = s.replaceAll("[^0-9.\\-]", "");
        return v.isEmpty() ? 0 : Double.parseDouble(v);
    }

    private static LocalDate invoiceDate(String s) {
        return LocalDate.parse(s.substring(0, 10), ISO);
    }

    private void assertDatesWithin(List<String> invoiceDates, LocalDate from, LocalDate to, String what) {
        Assert.assertFalse(invoiceDates.isEmpty(), "❌ No rows for " + what);
        for (String d : invoiceDates) {
            LocalDate x = invoiceDate(d);
            Assert.assertFalse(x.isBefore(from) || x.isAfter(to), "❌ " + what + ": invoice date " + d + " outside " + from + " → " + to);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001_TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC_001 — Default view of Update Payments")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);

        Assert.assertEquals(page.getMenuPath(), "Support > Financial Updates > Update Payments", "❌ Menu path");

        String dateOption = page.getSelectedText("filter_month");
        Assert.assertTrue(dateOption.startsWith("Current Year"), "❌ Invoice Date default: " + dateOption);
        Assert.assertEquals(page.getHeading(), "Due Payments " + dateOption, "❌ Heading");

        String notes = page.getNotes();
        Reporter.log("   Notes: " + notes + " | count: " + page.getInvoiceCountText(), true);
        Assert.assertTrue(notes.contains("Void Invoice: Invoice can only be voided if it is entirely due."), "❌ Note Void: " + notes);
        Assert.assertTrue(notes.contains("Adjust Invoice: Amount will be adjusted from credit balance for a child(If any)"), "❌ Note Adjust: " + notes);
        Assert.assertTrue(notes.contains("Adjust Excess Payment: Amount will be adjusted from excess payment balance for a child(If any)"), "❌ Note Excess: " + notes);
        Assert.assertTrue(page.getInvoiceCountText().matches("[\\d,]+ invoices?"), "❌ Invoice count under heading: " + page.getInvoiceCountText());
        Assert.assertTrue(page.getAllMenuPaths().contains("Children > Billing > Due Payments"), "❌ Second menu entry: " + page.getAllMenuPaths());

        List<String> labels = page.getFilterLabels();
        Reporter.log("   Filter labels: " + labels, true);
        for (String l : Arrays.asList("Admission ID", "Center", "Ownership", "City", "Invoice Status", "Invoice Date"))
            Assert.assertTrue(labels.contains(l), "❌ Filter label missing: " + l + " in " + labels);

        Assert.assertEquals(page.getMultiselectButtonText("sr_center_id"), "No Filters", "❌ Center default");
        Assert.assertEquals(page.getMultiselectButtonText("sr_ownership"), "No Filters", "❌ Ownership default");
        Assert.assertEquals(page.getMultiselectButtonText("sr_city_id"), "No Filters", "❌ City default");
        Assert.assertEquals(page.getMultiselectButtonText("sr_invoice_status"), "Due, Overdue", "❌ Invoice Status default");
        Assert.assertEquals(page.getAdmissionId(), "", "❌ Admission ID should be empty");
        Assert.assertFalse(page.isMonthRangeVisible(), "❌ Month range should be hidden by default");

        Assert.assertEquals(page.getVisibleColumnHeaders().subList(1, page.getVisibleColumnHeaders().size()), VISIBLE_COLUMNS, "❌ Visible columns");
        Assert.assertTrue(page.getAllColumnHeaders().containsAll(HIDDEN_COLUMNS), "❌ Hidden columns: " + page.getAllColumnHeaders());
        Assert.assertTrue(page.isSelectAllVisible(), "❌ Select-all checkbox not visible");
        Assert.assertEquals(page.getDownloadReportText(), "Download Report", "❌ Download Report button");
        Assert.assertTrue(page.isSearchVisible(), "❌ Search not visible");
        Assert.assertTrue(page.isPaginationVisible(), "❌ Pagination not visible");
        Assert.assertEquals(page.getPageLength(), 20, "❌ Page length");
        Assert.assertTrue(page.getInfoText().matches("Showing 1 to 20 of [\\d,]+ entries"), "❌ Info: " + page.getInfoText());
        Reporter.log("   Info: " + page.getInfoText() + " | note: " + page.getMaxRowsNote(), true);
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — Filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC002_TC_001 — Admission ID filter")
    public void sc002_tc001_admissionIdFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Admission ID filter", true);
        String childId = page.getColumnValues("Child #").get(0);

        page.enterAdmissionId(childId);
        page.clickFilter();
        Assert.assertEquals(page.getAdmissionId(), childId, "❌ Admission ID not kept after Filter");
        List<String> ids = page.getColumnValues("Child #");
        Reporter.log("   Child " + childId + " → " + ids.size() + " invoice(s)", true);
        Assert.assertFalse(ids.isEmpty(), "❌ No rows for child " + childId);
        Assert.assertTrue(ids.stream().allMatch(childId::equals), "❌ Other children listed: " + new TreeSet<>(ids));

        page.enterAdmissionId("99999999");
        page.clickFilter();
        Assert.assertEquals(page.getRowCount(), 0, "❌ Rows for a non-existent Admission ID");
        Assert.assertEquals(page.getEmptyTableMessage(), "No data available in table", "❌ Empty message");

        page.clickReset();
        Assert.assertEquals(page.getAdmissionId(), "", "❌ Reset did not clear Admission ID");
        Assert.assertTrue(page.getRowCount() > 1, "❌ Reset did not restore the rows");
        Reporter.log("✅ SC002_TC_001 PASSED", true);
    }

    @Test(priority = 3, description = "SC002_TC_002 — Center filter (one and several centres)")
    public void sc002_tc002_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_002 — Center filter", true);
        List<String> centres = new ArrayList<>(new LinkedHashSet<>(page.getColumnValues("Center")));
        Assert.assertTrue(centres.size() >= 2, "❌ Need 2 centres in the default list: " + centres);
        Assert.assertTrue(page.getMultiselectOptions("sr_center_id").size() > 100, "❌ Center list too short");

        String c1 = centres.get(0), c2 = centres.get(1);
        page.multiselectOnly("sr_center_id", c1);
        page.clickFilter();
        Assert.assertEquals(page.getMultiselectSelected("sr_center_id"), Collections.singletonList(c1), "❌ Center not kept");
        Set<String> got = new TreeSet<>(page.getColumnValues("Center"));
        Reporter.log("   " + c1 + " → " + got, true);
        Assert.assertEquals(got, Collections.singleton(c1), "❌ One-centre filter");

        page.multiselectOnly("sr_center_id", c1, c2);
        page.clickFilter();
        got = new TreeSet<>(page.getColumnValues("Center"));
        Reporter.log("   " + c1 + " + " + c2 + " → " + got, true);
        Assert.assertEquals(got, new TreeSet<>(Arrays.asList(c1, c2)), "❌ Two-centre filter");

        page.clickReset();
        Assert.assertEquals(page.getMultiselectButtonText("sr_center_id"), "No Filters", "❌ Reset Center");
        Reporter.log("✅ SC002_TC_002 PASSED", true);
    }

    @Test(priority = 4, description = "SC002_TC_003 — Ownership filter (COCO / Franchise / Onsite)")
    public void sc002_tc003_ownershipFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_003 — Ownership filter", true);
        Assert.assertEquals(page.getMultiselectOptions("sr_ownership"), Arrays.asList("COCO", "Franchise", "Onsite"), "❌ Ownership options");

        Map<String, Set<String>> centresBy = new LinkedHashMap<>();
        for (String o : Arrays.asList("COCO", "Franchise", "Onsite")) {
            page.multiselectOnly("sr_ownership", o);
            page.clickFilter();
            Assert.assertEquals(page.getMultiselectSelected("sr_ownership"), Collections.singletonList(o), "❌ Ownership not kept: " + o);
            Set<String> c = new TreeSet<>(page.getColumnValues("Center"));
            centresBy.put(o, c);
            Reporter.log("   " + o + " → " + page.getRowCount() + " rows, " + c.size() + " centres", true);
            reopen();
        }
        // ✅ A centre belongs to one ownership type, so the three lists must not overlap
        for (String a : centresBy.keySet())
            for (String b : centresBy.keySet())
                if (a.compareTo(b) < 0) {
                    Set<String> both = new TreeSet<>(centresBy.get(a));
                    both.retainAll(centresBy.get(b));
                    Assert.assertTrue(both.isEmpty(), "❌ Centres in both " + a + " and " + b + ": " + both);
                }
        Assert.assertFalse(centresBy.get("COCO").isEmpty(), "❌ No COCO invoices");
        Assert.assertFalse(centresBy.get("Franchise").isEmpty(), "❌ No Franchise invoices");
        Reporter.log("   Onsite centres: " + centresBy.get("Onsite"), true);
        Reporter.log("✅ SC002_TC_003 PASSED", true);
    }

    @Test(priority = 5, description = "SC002_TC_004 — Invoice Status filter")
    public void sc002_tc004_invoiceStatusFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_004 — Invoice Status filter", true);
        Assert.assertEquals(page.getMultiselectOptions("sr_invoice_status"), Arrays.asList("Due", "Overdue", "Paid", "Voided"), "❌ Status options");
        Assert.assertEquals(page.getMultiselectSelected("sr_invoice_status"), Arrays.asList("Due", "Overdue"), "❌ Status default");
        Set<String> def = new TreeSet<>(page.getColumnValues("Status"));
        Reporter.log("   Due, Overdue → " + def, true);
        Assert.assertTrue(Arrays.asList("Payment Due", "Payment Overdue").containsAll(def), "❌ Default statuses: " + def);

        Map<String, Set<String>> seen = new LinkedHashMap<>();
        for (String s : Arrays.asList("Paid", "Voided")) {
            page.multiselectOnly("sr_invoice_status", s);
            page.clickFilter();
            Set<String> st = new TreeSet<>(page.getColumnValues("Status"));
            seen.put(s, st);
            Reporter.log("   " + s + " → " + page.getRowCount() + " rows, statuses " + st, true);
            Assert.assertFalse(st.isEmpty(), "❌ No " + s + " invoices this year");
            Assert.assertEquals(st.size(), 1, "❌ Mixed statuses for " + s + ": " + st);
        }
        Assert.assertNotEquals(seen.get("Paid"), seen.get("Voided"), "❌ Paid and Voided show the same status");

        page.multiselectOnly("sr_invoice_status", "Due", "Overdue", "Paid", "Voided");
        page.clickFilter();
        Set<String> all = new TreeSet<>(page.getColumnValues("Status"));
        Reporter.log("   All statuses → " + all, true);
        Assert.assertTrue(all.size() >= 2, "❌ All statuses should list several: " + all);
        Reporter.log("✅ SC002_TC_004 PASSED", true);
    }

    @Test(priority = 6, description = "SC002_TC_005 — Invoice Date filter (presets, Custom month range, Select All)")
    public void sc002_tc005_invoiceDateFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_005 — Invoice Date filter", true);
        List<String> opts = page.getSelectOptions("filter_month");
        Reporter.log("   Options: " + opts, true);
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today), lastMonth = thisMonth.minusMonths(1);
        String mmm = "MMM-yyyy";
        Assert.assertEquals(opts.get(0), "Select All", "❌ First option");
        Assert.assertTrue(opts.contains("Current Month (" + thisMonth.format(DateTimeFormatter.ofPattern(mmm, Locale.ENGLISH)) + ")"), "❌ Current Month option: " + opts);
        Assert.assertTrue(opts.contains("Last Month (" + lastMonth.format(DateTimeFormatter.ofPattern(mmm, Locale.ENGLISH)) + ")"), "❌ Last Month option: " + opts);
        for (String p : Arrays.asList("Current Year", "Current Quarter", "Last Quarter", "Last 3 Months", "Last 6 Months", "Last Year", "Custom Date Range"))
            Assert.assertTrue(opts.stream().anyMatch(o -> o.startsWith(p)), "❌ Option missing: " + p);

        // Last Month
        page.selectByValue("filter_month", "Last Month");
        page.clickFilter();
        Assert.assertTrue(page.getHeading().contains("Last Month"), "❌ Heading after Last Month: " + page.getHeading());
        assertDatesWithin(page.getColumnValues("Invoice Date"), lastMonth.atDay(1), lastMonth.atEndOfMonth(), "Last Month");

        // Custom Date Range → month selects
        page.selectByValue("filter_month", "Custom Date Range");
        Assert.assertTrue(page.isMonthRangeVisible(), "❌ Start / End Month not shown for Custom Date Range");
        YearMonth from = thisMonth.minusMonths(3), to = thisMonth.minusMonths(2);
        page.selectByValue("filter_month_range_1", from.atEndOfMonth().format(ISO));
        page.selectByValue("filter_month_range_2", to.atEndOfMonth().format(ISO));
        page.clickFilter();
        Reporter.log("   Custom " + from + " → " + to + ": " + page.getRowCount() + " rows, heading " + page.getHeading(), true);
        assertDatesWithin(page.getColumnValues("Invoice Date"), from.atDay(1), to.atEndOfMonth(), "Custom range");

        // Select All → older invoices appear too
        page.selectByValue("filter_month", "");
        page.clickFilter();
        List<String> dates = page.getColumnValues("Invoice Date");
        LocalDate oldest = dates.stream().map(UpdatePaymentsTest::invoiceDate).min(LocalDate::compareTo).orElse(today);
        Reporter.log("   Select All → " + page.getRowCount() + " rows, oldest " + oldest + " | " + page.getMaxRowsNote(), true);
        Assert.assertTrue(oldest.getYear() < today.getYear(), "❌ Select All shows nothing before this year (oldest " + oldest + ")");
        Reporter.log("✅ SC002_TC_005 PASSED", true);
    }

    @Test(priority = 7, description = "SC002_TC_006 — Combined filters, then Reset")
    public void sc002_tc006_filterAndReset() throws InterruptedException {
        Reporter.log("▶ SC002_TC_006 — Combined filters + Reset", true);
        String centre = page.getColumnValues("Center").get(0);
        page.multiselectOnly("sr_center_id", centre);
        page.multiselectOnly("sr_invoice_status", "Paid");
        page.selectByValue("filter_month", "Current Year");
        page.clickFilter();
        Reporter.log("   " + centre + " + Paid + Current Year → " + page.getRowCount() + " rows", true);
        Assert.assertTrue(page.getColumnValues("Center").stream().allMatch(centre::equals), "❌ Centre not applied");
        Set<String> st = new TreeSet<>(page.getColumnValues("Status"));
        Assert.assertTrue(st.size() <= 1 && !st.contains("Payment Due") && !st.contains("Payment Overdue"), "❌ Status not applied: " + st);
        assertDatesWithin(page.getColumnValues("Invoice Date"), LocalDate.now().withDayOfYear(1), LocalDate.now().withMonth(12).withDayOfMonth(31), "Current Year");

        page.clickReset();
        Assert.assertEquals(page.getMultiselectButtonText("sr_center_id"), "No Filters", "❌ Center not reset");
        Assert.assertEquals(page.getMultiselectButtonText("sr_ownership"), "No Filters", "❌ Ownership not reset");
        Assert.assertEquals(page.getMultiselectButtonText("sr_city_id"), "No Filters", "❌ City not reset");
        Assert.assertEquals(page.getMultiselectButtonText("sr_invoice_status"), "Due, Overdue", "❌ Status not reset");
        Assert.assertTrue(page.getSelectedText("filter_month").startsWith("Current Year"), "❌ Invoice Date not reset: " + page.getSelectedText("filter_month"));
        Reporter.log("✅ SC002_TC_006 PASSED", true);
    }

    @Test(priority = 8, description = "SC002_TC_007 — City filter (new)")
    public void sc002_tc007_cityFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_007 — City filter", true);
        List<String> cities = page.getMultiselectOptions("sr_city_id");
        Assert.assertTrue(cities.contains("Bengaluru") && cities.contains("Gurgaon"), "❌ City list: " + cities.size());
        page.multiselectOnly("sr_city_id", "Bengaluru");
        page.clickFilter();
        Set<String> blr = new TreeSet<>(page.getColumnValues("Center"));
        Reporter.log("   Bengaluru → " + page.getRowCount() + " rows, " + blr.size() + " centres", true);
        Assert.assertFalse(blr.isEmpty(), "❌ No Bengaluru invoices");
        reopen();
        page.multiselectOnly("sr_city_id", "Gurgaon");
        page.clickFilter();
        Set<String> ggn = new TreeSet<>(page.getColumnValues("Center"));
        Reporter.log("   Gurgaon → " + page.getRowCount() + " rows, " + ggn.size() + " centres", true);
        Set<String> both = new TreeSet<>(blr);
        both.retainAll(ggn);
        Assert.assertTrue(both.isEmpty(), "❌ Centres listed for both cities: " + both);
        Reporter.log("✅ SC002_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 — Max 1,000 rows + Download All (replaces the summary amounts)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC003_TC_001 — Max 1,000 invoices note + Download All Invoice")
    public void sc003_tc001_maxRowsNote() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Max-rows note", true);
        String note = page.getMaxRowsNote();
        Reporter.log("   Note: " + note, true);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Total (\\d+) invoices found but max 1000 invoices are shown\\. Please restrict your search criteria").matcher(note);
        Assert.assertTrue(m.find(), "❌ Max-rows note: " + note);
        Assert.assertTrue(Integer.parseInt(m.group(1)) > 1000, "❌ Total should be over 1000: " + m.group(1));
        Assert.assertEquals(page.getRowCount(), 1000, "❌ Grid should hold 1000 rows");
        Assert.assertTrue(note.contains("Download All Invoice"), "❌ Download All Invoice link missing");
        String href = page.getDownloadAllHref();
        Assert.assertTrue(href.contains("func=downloadInvoiceList") && href.contains("payment_status%5B0%5D=payment_due")
                && href.contains("payment_status%5B1%5D=payment_overdue") && href.contains("date_from=" + LocalDate.now().getYear() + "-01-01"), "❌ Download All href: " + href);

        // ✅ A small result has no note
        page.enterAdmissionId(page.getColumnValues("Child #").get(0));
        page.clickFilter();
        Assert.assertEquals(page.getMaxRowsNote(), "", "❌ Note shown for a small result");
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Column data + Invoice Details
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC004_TC_001 — Column data formats")
    public void sc004_tc001_columnData() throws Exception {
        Reporter.log("▶ SC004_TC_001 — Column data", true);
        List<String> inv = page.getColumnValues("Invoice #"), series = page.getColumnValues("Invoice Series");
        for (int i = 0; i < inv.size(); i++) {
            Assert.assertTrue(inv.get(i).matches("\\d+"), "❌ Invoice #: " + inv.get(i));
            Assert.assertTrue(series.get(i).matches("[A-Z0-9]+(/[A-Z0-9]+)+"), "❌ Invoice Series: " + series.get(i));
        }
        for (String c : page.getColumnValues("Child #")) Assert.assertTrue(c.matches("\\d+"), "❌ Child #: " + c);
        List<String> amt = page.getColumnValues("Amount"), paid = page.getColumnValues("Paid Amount");
        for (int i = 0; i < amt.size(); i++) {
            Assert.assertTrue(amt.get(i).matches("[\\d,]+\\.\\d{2}") && paid.get(i).matches("[\\d,]+\\.\\d{2}"), "❌ Amounts: " + amt.get(i) + " / " + paid.get(i));
            Assert.assertTrue(amount(paid.get(i)) < amount(amt.get(i)), "❌ A due invoice is fully paid: " + amt.get(i) + " / " + paid.get(i));
        }
        for (String d : page.getColumnValues("Invoice Date")) Assert.assertTrue(d.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), "❌ Invoice Date: " + d);
        for (String d : page.getColumnValues("Due Date")) Assert.assertTrue(d.matches("\\d{4}-\\d{2}-\\d{2}"), "❌ Due Date: " + d);
        Set<String> adm = new TreeSet<>(page.getColumnValues("Admission Status"));
        Reporter.log("   Admission Status values: " + adm, true);
        Assert.assertTrue(adm.contains("Active") || adm.contains("Attrition"), "❌ Admission Status: " + adm);
        Reporter.log("   Action sets: " + page.getActionSets(), true);
        Assert.assertTrue(page.getActionSets().keySet().stream().allMatch(k -> k.startsWith("details")), "❌ A row has no Invoice Details icon");

        // ✅ Invoice # badge links to the invoice PDF on the billing server
        String id = inv.get(0);
        String href = page.getPdfHref(id);
        Assert.assertTrue(href.contains("test-billing.footprintseducation.in/api/invoice/downloadpdf?q="), "❌ PDF link: " + href);
        HttpURLConnection c = (HttpURLConnection) new URL(href).openConnection();
        c.setInstanceFollowRedirects(true);
        int code = c.getResponseCode();
        String type = String.valueOf(c.getContentType());
        Reporter.log("   PDF " + id + " → HTTP " + code + " " + type, true);
        Assert.assertEquals(code, 200, "❌ PDF HTTP status");
        Assert.assertTrue(type.contains("pdf"), "❌ PDF content type: " + type);
        Reporter.log("✅ SC004_TC_001 PASSED", true);
    }

    @Test(priority = 11, description = "SC004_TC_002 — Invoice Details popover (new)")
    public void sc004_tc002_invoiceDetailsPopover() throws InterruptedException {
        Reporter.log("▶ SC004_TC_002 — Invoice Details popover", true);
        String id = page.getColumnValues("Invoice #").get(1);
        Map<String, String> row = page.getRow(id);
        String text = page.openInvoiceDetailsPopover(id);
        Reporter.log("   " + id + ": " + text, true);
        // The 19-Nov redesign shows the popover title in capitals (CSS) — compare case-insensitively
        Assert.assertTrue(text.toUpperCase().contains("INVOICE DETAILS (" + id + ")"), "❌ Popover title: " + text);
        for (String f : Arrays.asList("Child Name", "Center", "Invoice Status", "Amount", "Paid Amount", "Pending Amount", "Due Date", "Total Reminder Sent"))
            Assert.assertTrue(text.contains(f), "❌ Popover field missing: " + f);
        Assert.assertTrue(text.toUpperCase().contains(row.get("Child Name").toUpperCase()), "❌ Child name");
        Assert.assertTrue(text.contains(row.get("Center")), "❌ Centre");
        Assert.assertTrue(text.contains(row.get("Status")), "❌ Status");
        double pending = amount(row.get("Amount")) - amount(row.get("Paid Amount"));
        Assert.assertTrue(text.replace(",", "").contains(String.format("Pending Amount %.2f", pending)), "❌ Pending Amount ≠ " + pending + ": " + text);
        Reporter.log("✅ SC004_TC_002 PASSED", true);
    }

    @Test(priority = 12, description = "SC004_TC_003 — Child Name opens Child Information (new)")
    public void sc004_tc003_childInformation() throws InterruptedException {
        Reporter.log("▶ SC004_TC_003 — Child Information popup", true);
        // An Attrition child's popup shows "No Details Found!" (seen 19-Nov, query Q3) — use an Active child
        String id = page.getFirstInvoiceWithAdmissionStatus("Active");
        Assert.assertFalse(id.isEmpty(), "❌ No invoice of an Active child");
        Map<String, String> row = page.getRow(id);
        Assert.assertEquals(page.getChildInfoHref(id), "pop_child_info?pop=yes&child_id=" + row.get("Child #"), "❌ Child link");
        page.clickChildName(id);
        String popup = page.getOpenPopupText();
        Reporter.log("   Popup: " + popup.substring(0, Math.min(200, popup.length())), true);
        Assert.assertTrue(popup.contains("#" + row.get("Child #")), "❌ Child Information popup does not show " + row.get("Child #"));
        Assert.assertTrue(popup.toUpperCase().contains(row.get("Child Name").toUpperCase()), "❌ Child name in popup");
        Reporter.log("✅ SC004_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005 — Actions (save on UAT)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "SC005_TC_001 — Adjust invoice from credit balance")
    public void sc005_tc001_adjustFromCreditBalance() throws InterruptedException {
        Reporter.log("▶ SC005_TC_001 — Adjust from credit balance", true);
        List<List<String>> rows = page.getRowsWithAction("credit");
        Reporter.log("   " + rows.size() + " row(s) with Adjust from Credit Balance", true);
        if (rows.isEmpty()) throw new SkipException("No invoice with a credit balance to adjust");
        List<String> r = rows.get(0);
        String id = r.get(0), child = r.get(1);
        String tip = page.getActionTooltip(id, ".adjust_credits_button[data-type=credit_adjust]");
        Reporter.log("   Invoice " + id + " child " + child + " | " + tip + " | amount " + r.get(2) + " paid " + r.get(3), true);
        Assert.assertTrue(tip.matches("Adjust Invoice #" + id + " From Credit Balance Due Amount - Rs\\.[\\d,.]+ Credit Balance - Rs\\. [\\d,.]+"), "❌ Tooltip: " + tip);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Due Amount - Rs\\.([\\d,.]+) Credit Balance - Rs\\. ([\\d,.]+)").matcher(tip);
        Assert.assertTrue(m.find());
        double due = amount(m.group(1)), credit = amount(m.group(2));
        Assert.assertEquals(due, amount(r.get(2)) - amount(r.get(3)), 0.01, "❌ Tooltip Due Amount ≠ Amount − Paid");

        page.startCapture(true);
        String result = page.clickAdjust(id, "credit_adjust");
        Reporter.log("   Confirms: " + page.getCapturedConfirms() + " | result: " + result + " | message: " + page.getPageMessage(), true);
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(
                "This will adjust invoice if any credits lying in customer account, Are you sure you want to do it?"), "❌ Confirm text");
        // (The request capture no longer sees this XHR after the 19-Nov redesign; the icon text is the server's reply)
        Assert.assertEquals(result, "Credits Adjusted", "❌ Adjust result: " + result + " " + page.getPageMessage());

        // ✅ After reload (the Invoice Status filter ignores Paid — Q1): a full adjust removes the invoice from
        //    Due/Overdue; a part adjust raises its Paid Amount by the credit used
        assertAfterAdjust(id, child, r, Math.min(due, credit), credit >= due);
        Reporter.log("✅ SC005_TC_001 PASSED", true);
    }

    private void assertAfterAdjust(String id, String child, List<String> before, double used, boolean full) throws InterruptedException {
        reopen();
        page.enterAdmissionId(child);
        page.selectByValue("filter_month", "");
        page.clickFilter();
        Map<String, String> after = page.getRow(id);
        Reporter.log("   After: " + after, true);
        if (full) {
            Assert.assertTrue(after.isEmpty(), "❌ Fully adjusted invoice " + id + " still Due/Overdue: " + after);
        } else {
            Assert.assertFalse(after.isEmpty(), "❌ Invoice " + id + " not found after a part adjust");
            Assert.assertEquals(amount(after.get("Paid Amount")), amount(before.get(3)) + used, 0.01, "❌ Paid Amount after adjust");
        }
        page.clickReset();
        page = new UpdatePaymentsPage(driver);
    }

    @Test(priority = 14, description = "SC005_TC_002 — Void a fully-due invoice")
    public void sc005_tc002_voidInvoice() throws InterruptedException {
        Reporter.log("▶ SC005_TC_002 — Void invoice", true);
        List<List<String>> rows = page.getRowsWithAction("void");
        Assert.assertFalse(rows.isEmpty(), "❌ No invoice with a Void icon");
        for (List<String> r : rows) Assert.assertEquals(amount(r.get(3)), 0.0, 0.001, "❌ Void icon on a partly-paid invoice " + r);
        // ✅ Smallest invoice
        rows.sort(Comparator.comparingDouble(r -> amount(r.get(2))));
        List<String> r = rows.get(0);
        String id = r.get(0), child = r.get(1);
        Reporter.log("   Voiding invoice " + id + " (child " + child + ", Rs " + r.get(2) + ", " + r.get(4) + ")", true);

        page.openVoidPopup(id);
        Assert.assertTrue(page.isVoidPopupVisible(), "❌ Void popup not open");
        String popup = page.getVoidPopupText();
        Assert.assertTrue(popup.toUpperCase().contains("VOID INVOICE REASON") && popup.toUpperCase().contains("REASON"), "❌ Void popup: " + popup);
        Assert.assertEquals(page.getVoidHidden("invoice_id"), id, "❌ Popup invoice id");
        Assert.assertEquals(page.getVoidHidden("child_id"), child, "❌ Popup child id");

        page.clickVoidSubmit();
        Assert.assertEquals(page.getVoidReasonError(), "Reason required", "❌ Empty reason");

        page.startCapture(false);
        page.enterVoidReason(VOID_REASON);
        page.clickVoidSubmit();
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList("This will void invoice, Are you sure you want to do it?"), "❌ Confirm text");
        Thread.sleep(1500);
        Assert.assertTrue(page.waitForVoidMessageQuick().isEmpty(), "❌ Cancel on the confirm still voided the invoice");

        page.startCapture(true);
        page.clickVoidSubmit();
        String msg = page.waitForVoidMessage();
        Reporter.log("   Message: " + msg, true);
        Assert.assertTrue(msg.contains("Invoice void successfully"), "❌ Void message: " + msg);
        Thread.sleep(4000);
        Assert.assertFalse(page.isRowPresent(id), "❌ Voided row still in the grid");

        // ✅ After reload the invoice is no longer Due/Overdue for the child (the Voided status filter
        //    is ignored by the server — Q1 — so it cannot be listed under Voided here)
        reopen();
        page.enterAdmissionId(child);
        page.selectByValue("filter_month", "");
        page.clickFilter();
        Map<String, String> after = page.getRow(id);
        Reporter.log("   After (Due/Overdue for child " + child + "): " + after, true);
        Assert.assertTrue(after.isEmpty(), "❌ Voided invoice " + id + " still listed as Due/Overdue");
        page.clickReset();
        Reporter.log("✅ SC005_TC_002 PASSED — invoice " + id + " voided", true);
    }

    @Test(priority = 15, description = "SC005_TC_004 — Adjust invoice from security deposit (new; SC005_TC_003 Adjust Excess Payment has no icon live)")
    public void sc005_tc004_adjustFromSecurityDeposit() throws InterruptedException {
        Reporter.log("▶ SC005_TC_004 — Adjust from security deposit", true);
        Reporter.log("   Excess-payment rows: " + page.getRowsWithAction("excess").size() + " (none expected — the icon is not rendered)", true);
        List<List<String>> rows = page.getRowsWithAction("security");
        Reporter.log("   " + rows.size() + " row(s) with Adjust from Security Deposit", true);
        if (rows.isEmpty()) throw new SkipException("No invoice with a security deposit to adjust");
        rows.sort(Comparator.comparingDouble(r -> amount(r.get(2)) - amount(r.get(3))));
        List<String> r = rows.get(0);
        String id = r.get(0), child = r.get(1);
        String tip = page.getActionTooltip(id, ".adjust_credits_button[data-type=security_payment_adjust]");
        Reporter.log("   Invoice " + id + " child " + child + " | " + tip, true);
        Assert.assertTrue(tip.matches("Adjust Invoice #" + id + " From Security Deposite Due Amount - Rs\\.[\\d,.]+ Security Deposite - Rs\\. [\\d,.]+"), "❌ Tooltip: " + tip);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Due Amount - Rs\\.([\\d,.]+) Security Deposite - Rs\\. ([\\d,.]+)").matcher(tip);
        Assert.assertTrue(m.find());
        double due = amount(m.group(1)), deposit = amount(m.group(2));

        page.startCapture(true);
        String result = page.clickAdjust(id, "security_payment_adjust");
        Reporter.log("   Confirms: " + page.getCapturedConfirms() + " | result: " + result + " | message: " + page.getPageMessage(), true);
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(
                "This will adjust invoice if any security credits lying in customer account, Are you sure you want to do it?"), "❌ Confirm text");
        Assert.assertFalse(result.isEmpty() || result.contains("[ERROR]"), "❌ Adjust failed: " + result + " " + page.getPageMessage());
        Reporter.log("   Result: " + result, true);
        assertAfterAdjust(id, child, r, Math.min(due, deposit), deposit >= due);
        Reporter.log("✅ SC005_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC006 — Download, bulk selection, search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "SC006_TC_001 — Download Report (CSV of the filtered rows)")
    public void sc006_tc001_downloadReport() throws Exception {
        Reporter.log("▶ SC006_TC_001 — Download Report", true);
        String child = page.getColumnValues("Child #").get(0);
        page.enterAdmissionId(child);
        page.clickFilter();
        List<String> ids = page.getColumnValues("Invoice #");

        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        page.clickDownloadReport();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] f = downloads.listFiles((d, n) -> n.startsWith("Invoices_Report") && n.endsWith(".csv"));
            if (f != null) for (File x : f) if (x.lastModified() >= start - 2000 && (csv == null || x.lastModified() > csv.lastModified())) csv = x;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No Invoices_Report*.csv downloaded");
        List<String> lines = Files.readAllLines(csv.toPath());
        lines.removeIf(String::isEmpty);
        Reporter.log("   " + csv.getName() + " | " + lines.size() + " lines | header " + lines.get(0), true);
        // The redesign writes &nbsp; into headers ("Invoice&nbsp;#") and the file name ("…__nbspCurrent_Year…", Q4)
        Reporter.log("   File name contains 'nbsp': " + csv.getName().contains("nbsp"), true);
        String header = lines.get(0).replace('\u00a0', ' ');
        for (String h : Arrays.asList("Invoice #", "Invoice Series", "Child #", "Child Name", "Center", "Room", "Program", "Amount",
                "Paid Amount", "Status", "Invoice Date", "Payment Date", "Admission Status", "Due Date"))
            Assert.assertTrue(header.contains("\"" + h + "\""), "❌ CSV column missing: " + h);
        Assert.assertFalse(header.contains("Action"), "❌ CSV has the Action column");
        Assert.assertEquals(lines.size() - 1, ids.size(), "❌ CSV rows ≠ grid rows");
        for (String id : ids) Assert.assertTrue(lines.stream().anyMatch(l -> l.startsWith("\"" + id + "\"")), "❌ Invoice " + id + " missing in CSV");
        Reporter.log("✅ SC006_TC_001 PASSED", true);
    }

    @Test(priority = 17, description = "SC006_TC_002 — Invoice checkboxes, Download ZIP count, Clear Selection")
    public void sc006_tc002_bulkSelection() throws InterruptedException {
        Reporter.log("▶ SC006_TC_002 — Bulk selection", true);
        Reporter.log("   Bulk toolbar (Clear Selection / Download ZIP) visible: " + page.isBulkToolbarVisible(), true);
        Assert.assertEquals(page.getBulkSelectedCount(), "0", "❌ Initial count");
        Assert.assertTrue(page.isZipButtonDisabled(), "❌ Download ZIP enabled with nothing selected");
        page.tickInvoiceCheckboxes(1);
        Assert.assertEquals(page.getBulkSelectedCount(), "1", "❌ Count after 1");
        Assert.assertFalse(page.isZipButtonDisabled(), "❌ Download ZIP still disabled");
        page.tickInvoiceCheckboxes(3);
        Assert.assertEquals(page.getBulkSelectedCount(), "3", "❌ Count after 3");
        page.tickSelectAllOnPage();
        Assert.assertEquals(page.getCheckedCount(), page.getPageRowCount(), "❌ Select all on page");
        Assert.assertEquals(page.getBulkSelectedCount(), String.valueOf(page.getPageRowCount()), "❌ Count after select all");
        page.clickClearSelection();
        Assert.assertEquals(page.getCheckedCount(), 0, "❌ Clear Selection left boxes ticked");
        Assert.assertEquals(page.getBulkSelectedCount(), "0", "❌ Count after Clear Selection");
        // Informational until answered (Q2): the toolbar stays display:none, so Download ZIP is not reachable
        Reporter.log("   ⚠ Bulk toolbar visible after ticking: " + page.isBulkToolbarVisible() + " (Q2)", true);
        Reporter.log("✅ SC006_TC_002 PASSED", true);
    }

    @Test(priority = 18, description = "SC006_TC_003 — Search by child name and invoice number")
    public void sc006_tc003_search() throws InterruptedException {
        Reporter.log("▶ SC006_TC_003 — Search", true);
        int total = page.getRowCount();
        String id = page.getColumnValues("Invoice #").get(3);
        Map<String, String> row = page.getRow(id);

        page.searchTable(row.get("Child Name"));
        List<String> names = page.getColumnValues("Child Name");
        Reporter.log("   '" + row.get("Child Name") + "' → " + names.size() + " rows", true);
        Assert.assertTrue(names.size() < total, "❌ Name search did not narrow the list");
        Assert.assertTrue(page.getColumnValues("Invoice #").contains(id), "❌ Name search lost invoice " + id);

        page.searchTable(row.get("Invoice Series"));
        Assert.assertTrue(page.getColumnValues("Invoice Series").contains(row.get("Invoice Series")), "❌ Invoice number search");
        Assert.assertTrue(page.getRowCount() <= 3, "❌ Invoice number search too broad: " + page.getRowCount());

        page.searchTable("");
        Assert.assertEquals(page.getRowCount(), total, "❌ Clearing the search did not restore all rows");

        page.searchTable("zzqq-no-such-invoice");
        Assert.assertEquals(page.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC006_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC007 — Pagination + sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "SC007_TC_001 — Pagination (20 per page)")
    public void sc007_tc001_pagination() throws InterruptedException {
        Reporter.log("▶ SC007_TC_001 — Pagination", true);
        Assert.assertEquals(page.getPageRowCount(), 20, "❌ Rows on page 1");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 1 to 20 of"), "❌ Info: " + page.getInfoText());
        List<String> p1 = page.getPageColumnValues("Invoice #");
        page.clickPageNumber("2");
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Page 2");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 21 to 40 of"), "❌ Info page 2: " + page.getInfoText());
        List<String> p2 = page.getPageColumnValues("Invoice #");
        Assert.assertTrue(Collections.disjoint(p1, p2), "❌ Page 2 repeats page 1 rows");
        page.clickNextPage();
        Assert.assertEquals(page.getCurrentPage(), 3, "❌ Next → page 3");
        page.clickPreviousPage();
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Previous → page 2");
        Reporter.log("✅ SC007_TC_001 PASSED", true);
    }

    @Test(priority = 20, description = "SC007_TC_002 — Column sorting")
    public void sc007_tc002_sorting() throws InterruptedException {
        Reporter.log("▶ SC007_TC_002 — Sorting", true);
        Assert.assertTrue(page.getHeaderSortClass("Invoice Date").contains("sorting_desc"), "❌ Default sort should be Invoice Date desc: " + page.getHeaderSortClass("Invoice Date"));

        page.clickColumnHeader("Invoice #");
        List<Long> asc = new ArrayList<>();
        for (String v : page.getOrderedColumnValues("Invoice #")) asc.add(Long.parseLong(v));
        List<Long> s = new ArrayList<>(asc);
        Collections.sort(s);
        Assert.assertEquals(asc, s, "❌ Invoice # ascending");
        page.clickColumnHeader("Invoice #");
        List<Long> desc = new ArrayList<>();
        for (String v : page.getOrderedColumnValues("Invoice #")) desc.add(Long.parseLong(v));
        s.sort(Collections.reverseOrder());
        Assert.assertEquals(desc, s, "❌ Invoice # descending");

        page.clickColumnHeader("Amount");
        List<Double> am = new ArrayList<>();
        for (String v : page.getOrderedColumnValues("Amount")) am.add(amount(v));
        List<Double> sa = new ArrayList<>(am);
        Collections.sort(sa);
        Assert.assertEquals(am, sa, "❌ Amount ascending (numeric)");

        for (String col : Arrays.asList("Invoice Date", "Due Date")) {
            page.clickColumnHeader(col);
            List<String> d = page.getOrderedColumnValues(col);
            List<String> sd = new ArrayList<>(d);
            if (page.getHeaderSortClass(col).contains("sorting_desc")) sd.sort(Collections.reverseOrder());
            else Collections.sort(sd);
            Assert.assertEquals(d, sd, "❌ " + col + " sort (" + page.getHeaderSortClass(col) + ")");
        }
        Reporter.log("✅ SC007_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC008 — Negative
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 21, description = "SC008_TC_001 — Partly-paid invoices cannot be voided")
    public void sc008_tc001_partlyPaidNoVoid() {
        Reporter.log("▶ SC008_TC_001 — Partly-paid invoices have no Void icon", true);
        List<String> paid = page.getColumnValues("Paid Amount");
        long partly = paid.stream().filter(p -> amount(p) > 0).count();
        Reporter.log("   Partly-paid rows: " + partly, true);
        if (partly == 0) throw new SkipException("No partly-paid invoice in the default list");
        for (List<String> r : page.getRowsWithAction("void"))
            Assert.assertEquals(amount(r.get(3)), 0.0, 0.001, "❌ Void offered on a partly-paid invoice " + r);
        Reporter.log("✅ SC008_TC_001 PASSED — no partly-paid invoice offers Void", true);
    }

    @Test(priority = 22, description = "SC008_TC_002 — No results")
    public void sc008_tc002_noResults() throws InterruptedException {
        Reporter.log("▶ SC008_TC_002 — No results", true);
        page.enterAdmissionId("99999999");
        page.clickFilter();
        Assert.assertEquals(page.getRowCount(), 0, "❌ Rows found");
        Assert.assertEquals(page.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        Assert.assertEquals(page.getInfoText(), "Showing 0 to 0 of 0 entries", "❌ Info");
        Assert.assertEquals(page.getMaxRowsNote(), "", "❌ Max-rows note on an empty result");
        Reporter.log("✅ SC008_TC_002 PASSED", true);
    }
}
