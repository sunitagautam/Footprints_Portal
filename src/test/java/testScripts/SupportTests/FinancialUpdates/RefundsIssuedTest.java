package testScripts.SupportTests.FinancialUpdates;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.FinancialUpdates.RefundsIssuedPage;
import utils.BaseTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════════
// REFUNDS ISSUED (Support → Financial Updates → Refunds Issued, /refund_list)
// Source: "TC_Support_Financial_Updates_2026 (1).xlsx", sheet "TC_Refunds Issued" (47 TCs), updated to the live screen
// (2026-11-20) + new TCs → TC_Support_Financial_Updates.xlsx, sheet TC_RefundsIssued. Runs as Rakesh.
//
// Saves (standing approval, CLAUDE.md 2026-11-18 — "the refund workflow (Add New Refund, Verify / Transfer / Return)"):
//  - SC002_TC_003 adds a ₹100 Registration Fee refund for attrition child -DrfChild (default 63478) with ticket 375022;
//    SC005_TC_006 verifies it (Advice → Verified) and SC005_TC_003 voids it, so it ends Voided;
//  - SC006_TC_007 marks one existing Transfer Initiated refund (not a 15-day case) as Return (-DrfReturnId);
//  - SC006_TC_005 does Verify Transfer on one 15-day Transfer Initiated refund (-DrfTransferId) → Refunded.
// Not saved: Upload Transfer Initiated / Upload Refunded Data (excluded), Incorrect Bank Details (validation only),
// Send Bank Details Email (confirm cancelled).
// Evidence screenshots for failures: ~/Desktop/QA_Documents/Bug Evidence/FinancialUpdates_RefundsIssued/.
// ═══════════════════════════════════════════════════════════════
public class RefundsIssuedTest extends BaseTest {

    private static final String ADD_CONFIRM = "Are you sure, You want to issue refund?";
    private static final String VOID_CONFIRM = "Are you sure want to void this refund ?";
    private static final List<String> DEFAULT_STATUSES = Arrays.asList("Advice", "Verified", "Bank Details Awaited", "Transfer Initiated");
    private static final List<String> COLUMNS = Arrays.asList("Child Name", "Type", "Description", "Amount", "Issued Date", "Verification Date",
            "Bank Details Received", "Transfer Date", "Closure/ Refunded Date", "Status", "Action");
    private static final File EVIDENCE = new File(System.getProperty("user.home"), "Desktop/QA_Documents/Bug Evidence/FinancialUpdates_RefundsIssued");

    Navigations navigations;
    RefundsIssuedPage page;

    private final String child = System.getProperty("rfChild", "63478");
    private final String activeChild = System.getProperty("rfActiveChild", "50920");
    private final String stamp = String.valueOf(System.currentTimeMillis() % 1000000);
    private final String desc = "QA Auto refund " + stamp;
    private final String ticket = "375022";
    private String refundId = "";
    private boolean ownVerifyMissing = false;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
        try { driver.manage().window().setSize(new org.openqa.selenium.Dimension(1680, 1050)); } catch (Exception ignored) { }
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws Exception {
        ensureBrowserAlive();
        dismissAlertIfPresent();
        reopen();
    }

    private void ensureBrowserAlive() throws Exception {
        try {
            driver.getTitle();
        } catch (Exception e) {
            Reporter.log("⚠ Browser session lost — relaunching (" + String.valueOf(e.getMessage()).split("\n")[0] + ")", true);
            try { driver.quit(); } catch (Exception ignored) { }
            openBrowser("chrome");
            setUp();
        }
    }

    // Opens the screen; Reset when the server kept a posted filter
    private void reopen() throws InterruptedException {
        navigations.goToRefundsIssued();
        dismissAlertIfPresent();
        page = new RefundsIssuedPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Refunds Issued did not load");
        if (!page.getStatusSelected().equals(DEFAULT_STATUSES) || !page.getValue("child_id").isEmpty() || !page.getValue("refund_type").equals("All")) {
            page.clickReset();
            if (!page.getStatusSelected().equals(DEFAULT_STATUSES)) {
                navigations.goToRefundsIssued();
                page.isPageLoaded();
            }
        }
    }

    // Wide filter: Issued 1 Jan of the year → today, every status
    private void wide() throws InterruptedException {
        page.setDate("date_from", LocalDate.now().withDayOfYear(1).toString());
        page.setDate("date_to", LocalDate.now().toString());
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
    }

    private void filterChild(String childId) throws InterruptedException {
        page.typeChildId(childId);
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
        page.showAllRows();
    }

    private void evidence(String name) {
        try {
            EVIDENCE.mkdirs();
            page.hideApiDebugPanel();
            File png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File out = new File(EVIDENCE, name + ".png");
            Files.copy(png.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Reporter.log("   📷 " + out, true);
        } catch (Exception e) {
            Reporter.log("   (screenshot failed: " + e.getMessage() + ")", true);
        }
    }

    private static final List<DateTimeFormatter> FORMATS = Arrays.asList(
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH), DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH), DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH), DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));

    static LocalDate parse(String s) {
        String t = s.trim().replaceAll("\\s+\\d{1,2}:\\d{2}.*$", "");
        for (DateTimeFormatter f : FORMATS)
            try { return LocalDate.parse(t, f); } catch (Exception ignored) { }
        throw new IllegalArgumentException("Unknown date format: '" + s + "'");
    }

    private List<String> col(String key) {
        return page.getRows().stream().map(r -> r.get(key)).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001 — Default view and filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC_001 — Default view: filters with defaults, buttons, table")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);
        Assert.assertEquals(page.getMenuPath(), "Support > Financial Updates > Refunds Issued", "❌ Menu path");
        Reporter.log("   heading '" + page.getHeading() + "' | labels " + page.getFilterLabels() + " | buttons " + page.getTopButtons(), true);
        Assert.assertEquals(page.getTopButtons(), Arrays.asList("Add New Refund", "Upload Transfer Initiated", "Upload Refunded Data"), "❌ Top buttons");
        LocalDate today = LocalDate.now();
        Assert.assertEquals(parse(page.getValue("date_from")), today.minusMonths(1), "❌ Issued From default");
        Assert.assertEquals(parse(page.getValue("date_to")), today, "❌ Issued To default");
        Assert.assertEquals(page.getValue("transfer_date_from"), "", "❌ Transfer From default");
        Assert.assertEquals(page.getValue("transfer_date_to"), "", "❌ Transfer To default");
        Assert.assertEquals(page.getValue("refund_type"), "All", "❌ Type default");
        Assert.assertEquals(page.getStatusSelected(), DEFAULT_STATUSES, "❌ Status default");
        Assert.assertEquals(page.getStatusOptions(), Arrays.asList(RefundsIssuedPage.ALL_STATUSES), "❌ Status options");
        Assert.assertEquals(page.getValue("admission_type"), "--Select--", "❌ Admission Type default");
        Assert.assertEquals(page.getOptions("admission_type"), Arrays.asList("--Select--", "Regular", "Corporate", "Activity Camp"), "❌ Admission Type options");
        Assert.assertEquals(page.getDefinedHeaders(), COLUMNS, "❌ Columns");
        Assert.assertTrue(page.isSearchBoxVisible() && page.isPaginationVisible(), "❌ Search / pagination missing");
        Assert.assertEquals(page.getDownloadButtonText(), "Download Report", "❌ Download button");
        Assert.assertEquals(page.getPageLength(), 10, "❌ Rows per page");
        for (Map<String, String> r : page.getRows())
            Assert.assertTrue(DEFAULT_STATUSES.contains(r.get("Status")), "❌ Default list has status " + r.get("Status"));
        Reporter.log("   " + page.getInfoText() + " | order " + page.getOrder(), true);
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    @Test(priority = 2, description = "SC001_TC_002 — Issued date range")
    public void sc001_tc002_issuedDateRange() throws InterruptedException {
        Reporter.log("▶ SC001_TC_002 — Issued date range", true);
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(2), to = LocalDate.now().withDayOfMonth(1).minusDays(1);
        page.setDate("date_from", from.toString());
        page.setDate("date_to", to.toString());
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
        List<String> issued = col("Issued Date");
        Reporter.log("   " + from + " → " + to + " : " + issued.size() + " rows, e.g. " + issued.subList(0, Math.min(3, issued.size())), true);
        Assert.assertTrue(issued.size() > 0, "❌ No refunds in " + from + " → " + to);
        Assert.assertEquals(parse(page.getValue("date_from")), from, "❌ From not kept");
        List<String> out = issued.stream().filter(d -> d.isEmpty() || parse(d).isBefore(from) || parse(d).isAfter(to)).collect(Collectors.toList());
        Assert.assertTrue(out.isEmpty(), "❌ Issued dates outside the range: " + out);
        Reporter.log("✅ SC001_TC_002 PASSED", true);
    }

    @Test(priority = 3, description = "SC001_TC_003 — Transfer date range")
    public void sc001_tc003_transferDateRange() throws InterruptedException {
        Reporter.log("▶ SC001_TC_003 — Transfer date range", true);
        LocalDate from = LocalDate.now().minusMonths(3), to = LocalDate.now();
        page.setDate("date_from", "");
        page.setDate("date_to", "");
        page.setDate("transfer_date_from", from.toString());
        page.setDate("transfer_date_to", to.toString());
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
        List<String> tr = col("Transfer Date");
        Reporter.log("   " + from + " → " + to + " : " + tr.size() + " rows, e.g. " + tr.subList(0, Math.min(3, tr.size())), true);
        Assert.assertTrue(tr.size() > 0, "❌ No transferred refunds in the range");
        List<String> out = tr.stream().filter(d -> d.isEmpty() || parse(d).isBefore(from) || parse(d).isAfter(to)).collect(Collectors.toList());
        Assert.assertTrue(out.isEmpty(), "❌ Transfer dates outside the range: " + out);
        Reporter.log("✅ SC001_TC_003 PASSED", true);
    }

    @Test(priority = 4, description = "SC001_TC_004 — Type filter; options vs refund types in the data")
    public void sc001_tc004_typeFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_004 — Type", true);
        List<String> opts = page.getOptions("refund_type");
        Reporter.log("   options " + opts + " values " + page.getOptionValues("refund_type"), true);
        wide();
        Set<String> inData = new TreeSet<>(col("Type"));
        Reporter.log("   types in the data (this year, all statuses): " + inData, true);
        page.selectByText("refund_type", "Refund - Annual Fee");
        page.clickSubmit();
        Set<String> got = new TreeSet<>(col("Type"));
        Reporter.log("   Refund - Annual Fee → " + page.getRowCount() + " rows " + got, true);
        Assert.assertTrue(got.isEmpty() || got.equals(Collections.singleton("Refund - Annual Fee")), "❌ Type filter: " + got);
        // Every refund type in the data should be offered by the filter
        Set<String> offered = new HashSet<>(page.getOptionValues("refund_type"));
        List<String> missing = inData.stream().filter(t -> !offered.contains(t)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            wide();
            evidence("01_Type_filter_misses_types_in_data");
        }
        Assert.assertTrue(missing.isEmpty(), "❌ Type filter has no option for " + missing + " (bug)");
        Reporter.log("✅ SC001_TC_004 PASSED", true);
    }

    @Test(priority = 5, description = "SC001_TC_005 — Status filter")
    public void sc001_tc005_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_005 — Status", true);
        Assert.assertEquals(page.getStatusButtonText(), "4 selected", "❌ Status button text");
        page.setDate("date_from", LocalDate.now().withDayOfYear(1).toString());
        page.setStatuses("Transfer Pending", "Return");
        page.clickSubmit();
        Set<String> got = new TreeSet<>(col("Status"));
        Reporter.log("   Transfer Pending + Return → " + page.getRowCount() + " rows " + got, true);
        Assert.assertTrue(Arrays.asList("Transfer Pending", "Return").containsAll(got), "❌ Status filter: " + got);
        Assert.assertEquals(page.getStatusSelected(), Arrays.asList("Transfer Pending", "Return"), "❌ Status not kept");
        page.setStatuses("Refunded");
        page.clickSubmit();
        got = new TreeSet<>(col("Status"));
        Reporter.log("   Refunded → " + page.getRowCount() + " rows " + got, true);
        Assert.assertTrue(got.isEmpty() || got.equals(Collections.singleton("Refunded")), "❌ Refunded filter: " + got);
        Reporter.log("✅ SC001_TC_005 PASSED", true);
    }

    @Test(priority = 6, description = "SC001_TC_006 — Admission Type filter")
    public void sc001_tc006_admissionType() throws InterruptedException {
        Reporter.log("▶ SC001_TC_006 — Admission Type", true);
        // Default one-month range with every status (a wide range hits the 500-row cap)
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
        int all = page.getRowCount();
        Assert.assertTrue(all < 500, "❌ Range too wide for a count check: " + all);
        Map<String, Integer> c = new LinkedHashMap<>();
        for (String t : Arrays.asList("Regular", "Corporate", "Activity Camp")) {
            page.selectByText("admission_type", t);
            page.clickSubmit();
            c.put(t, page.getRowCount());
            Assert.assertEquals(page.getValue("admission_type"), t, "❌ Admission Type not kept");
        }
        Reporter.log("   all " + all + " | " + c, true);
        int sum = c.values().stream().mapToInt(Integer::intValue).sum();
        Assert.assertTrue(c.get("Regular") > 0 && c.get("Regular") <= all, "❌ Regular count");
        Assert.assertEquals(sum, all, "❌ Regular + Corporate + Activity Camp ≠ all (" + c + ", all " + all + ")");
        Reporter.log("✅ SC001_TC_006 PASSED", true);
    }

    @Test(priority = 7, description = "SC001_TC_007 / new — Child ID and Ticket ID filters")
    public void sc001_tc007_childAndTicket() throws InterruptedException {
        Reporter.log("▶ SC001_TC_007 — Child ID / Ticket ID", true);
        wide();
        String id = page.getRows().get(0).get("childId");
        filterChild(id);
        Set<String> ids = new TreeSet<>(col("childId"));
        Reporter.log("   Child ID " + id + " → " + page.getRowCount() + " rows " + ids + " | statuses " + new TreeSet<>(col("Status")), true);
        Assert.assertEquals(ids, Collections.singleton(id), "❌ Child ID filter");
        Assert.assertEquals(page.getValue("child_id"), id, "❌ Child ID not kept");
        reopen();
        wide();
        String tk = page.getRows().stream().map(r -> r.get("popover").replaceAll(".*Ticket ID : (\\S+).*", "$1")).filter(t -> t.matches("\\d+")).findFirst().orElse("");
        if (tk.isEmpty()) throw new SkipException("No refund with a ticket id this year");
        page.typeTicketId(tk);
        page.clickSubmit();
        List<String> tks = page.getRows().stream().map(r -> r.get("popover").replaceAll(".*Ticket ID : (\\S+).*", "$1")).distinct().collect(Collectors.toList());
        Reporter.log("   Ticket ID " + tk + " → " + page.getRowCount() + " rows " + tks, true);
        Assert.assertEquals(tks, Collections.singletonList(tk), "❌ Ticket ID filter");
        Reporter.log("✅ SC001_TC_007 PASSED", true);
    }

    @Test(priority = 8, description = "SC001_TC_008 — Reset restores the defaults")
    public void sc001_tc008_reset() throws InterruptedException {
        Reporter.log("▶ SC001_TC_008 — Reset", true);
        page.setDate("date_from", LocalDate.now().withDayOfYear(1).toString());
        page.selectByText("refund_type", "Refund - Annual Fee");
        page.selectByText("admission_type", "Regular");
        page.setStatuses("Refunded");
        page.clickSubmit();
        page.clickReset();
        Map<String, String> after = new LinkedHashMap<>();
        for (String f : Arrays.asList("date_from", "date_to", "transfer_date_from", "refund_type", "admission_type", "child_id")) after.put(f, page.getValue(f));
        after.put("status", String.valueOf(page.getStatusSelected()));
        Reporter.log("   after Reset " + after + " | " + page.getInfoText(), true);
        boolean ok = !after.get("date_from").isEmpty() && parse(after.get("date_from")).equals(LocalDate.now().minusMonths(1))
                && after.get("refund_type").equals("All") && after.get("admission_type").equals("--Select--")
                && page.getStatusSelected().equals(DEFAULT_STATUSES);
        if (!ok) evidence("02_After_Reset_filters_not_default");
        Assert.assertTrue(ok, "❌ Reset did not restore the defaults: " + after);
        Reporter.log("✅ SC001_TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 / SC005 — Search, download, pagination, sorting, links, 500 cap
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC003_TC_001 — Search box")
    public void sc003_tc001_search() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Search", true);
        int all = page.getRowCount();
        String name = page.getRows().get(page.getRowCount() / 2).get("Child Name");
        page.searchTable(name);
        Assert.assertTrue(page.getRowCount() > 0 && page.getRowCount() <= all, "❌ Search");
        for (Map<String, String> r : page.getRows()) Assert.assertTrue(String.join(" ", r.values()).toLowerCase().contains(name.toLowerCase()), "❌ Row without " + name);
        page.searchTable("zzqq" + stamp);
        Assert.assertEquals(page.getEmptyMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("   '" + name + "' ok; no-match ok", true);
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    @Test(priority = 11, description = "SC003_TC_002 — Download Report")
    public void sc003_tc002_download() throws Exception {
        Reporter.log("▶ SC003_TC_002 — Download", true);
        int rows = page.getRowCount();
        String name = page.getDownloadFileName();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        page.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] f = downloads.listFiles((d, n) -> n.startsWith("Refund_Issued_Report") && n.endsWith(".csv"));
            if (f != null) for (File x : f) if (x.lastModified() >= start - 2000 && (csv == null || x.lastModified() > csv.lastModified())) csv = x;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No Refund_Issued_Report*.csv downloaded");
        List<String> lines = Files.readAllLines(csv.toPath());
        Reporter.log("   " + csv.getName() + " (setting '" + name + "') | " + lines.size() + " lines | header " + lines.get(0), true);
        LocalDate t = LocalDate.now();
        Assert.assertTrue(csv.getName().startsWith("Refund_Issued_Report_" + t.getDayOfMonth() + "_" + t.getMonthValue() + "_" + t.getYear()), "❌ File name " + csv.getName());
        for (String h : Arrays.asList("Child Name", "Type", "Description", "Amount", "Issued Date", "Status", "Account Bank Name", "Account Number"))
            Assert.assertTrue(lines.get(0).contains(h), "❌ CSV header missing " + h);
        Assert.assertFalse(lines.get(0).contains("\"Action\""), "❌ Action exported");
        Reporter.log("   grid rows " + rows, true);
        Reporter.log("✅ SC003_TC_002 PASSED", true);
    }

    @Test(priority = 12, description = "SC003_TC_003 / SC003_TC_004 — Pagination and sorting")
    public void sc003_tc003_paginationSorting() throws InterruptedException {
        Reporter.log("▶ SC003_TC_003 — Pagination / sorting", true);
        wide();
        Assert.assertTrue(page.getInfoText().startsWith("Showing 1 to 10 of"), "❌ " + page.getInfoText());
        page.clickNextPage();
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Next");
        page.clickPreviousPage();
        Assert.assertEquals(page.getCurrentPage(), 1, "❌ Previous");
        for (String h : Arrays.asList("Child Name", "Amount", "Issued Date", "Status")) {
            page.clickColumnHeader(h);
            String cls = page.getHeaderSortClass(h);
            Reporter.log("   " + h + " → " + cls, true);
            Assert.assertTrue(cls.contains("sorting_asc") || cls.contains("sorting_desc"), "❌ " + h + " not sortable");
        }
        page.clickColumnHeader("Child Name");
        if (!page.getHeaderSortClass("Child Name").contains("sorting_asc")) page.clickColumnHeader("Child Name");
        List<String> v = page.getSortData(0).stream().map(String::toLowerCase).collect(Collectors.toList()), s = new ArrayList<>(v);
        Collections.sort(s);
        Assert.assertEquals(v, s, "❌ Child Name A–Z");
        Reporter.log("✅ SC003_TC_003 PASSED", true);
    }

    @Test(priority = 13, description = "SC005_TC_001 / SC005_TC_002 / new — Child link, 500-row cap, refund info popover")
    public void sc005_tc001_childLinkAndCap() throws InterruptedException {
        Reporter.log("▶ SC005_TC_001 — Child link / 500 cap", true);
        Map<String, String> r = page.getRows().get(0);
        Reporter.log("   " + r.get("Child Name") + " → " + r.get("childHref") + " | popover: " + r.get("popover"), true);
        Assert.assertEquals(r.get("childHref"), "account_statement?child_id=" + r.get("childId") + "|_blank", "❌ Child Name link");
        Assert.assertTrue(r.get("popover").contains("Ticket ID") && r.get("popover").contains("Refund Issued By"), "❌ Type popover");
        page.setDate("date_from", LocalDate.now().minusYears(2).toString());
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
        Reporter.log("   2 years, all statuses → " + page.getTotalRowCount() + " rows", true);
        Assert.assertTrue(page.getTotalRowCount() <= 500, "❌ More than 500 rows");
        Assert.assertEquals(page.getTotalRowCount(), 500, "❌ Expected the 500 cap for a 2-year range");
        Reporter.log("✅ SC005_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Upload popups (view only; uploads are not run)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "SC004_TC_001 / SC004_TC_003 — Upload popups default view (no upload)")
    public void sc004_tc001_uploadPopups() throws InterruptedException {
        Reporter.log("▶ SC004_TC_001 — Upload popups", true);
        page.openTopPopup("Upload Transfer Initiated", "Upload");
        String t1 = page.getPopupText();
        Reporter.log("   Transfer Initiated: " + t1, true);
        Assert.assertTrue(t1.contains(".csv") && t1.toLowerCase().contains("click here"), "❌ Upload Transfer Initiated notes: " + t1);
        page.closePopup();
        reopen();
        page.openTopPopup("Upload Refunded Data", "Upload");
        String t2 = page.getPopupText();
        Reporter.log("   Refunded: " + t2, true);
        Assert.assertTrue(t2.contains(".csv"), "❌ Upload Refunded notes: " + t2);
        page.closePopup();
        Reporter.log("✅ SC004_TC_001 PASSED (no file uploaded)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — Add New Refund
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC002_TC_001 / SC002_TC_002 — Add New Refund popup; child search fills name and centre")
    public void sc002_tc001_addPopup() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Add popup", true);
        page.openAddRefund();
        Reporter.log("   labels " + page.getPopupLabels(), true);
        Reporter.log("   refund types " + page.getPopupOptions("refund_type"), true);
        List<String> types = page.getPopupOptions("refund_type").stream().map(o -> o.split("=")[0]).collect(Collectors.toList());
        Assert.assertEquals(types, Arrays.asList("Select Refund", "15 Days Withdrawal", "Second Year Annual Fee", "Tuition Fee", "Registration Fee",
                "Security Deposit", "Duplicate Payment", "Transport", "Welcome kit", "Book Charges"), "❌ Refund Type options");
        Assert.assertEquals(page.getPopupField("input[name=issue_refund_date]"), LocalDate.now().toString(), "❌ Issue Date default");
        page.searchChild(child);
        Reporter.log("   child " + child + " → '" + page.getChildName() + "' / '" + page.getChildCenter() + "' (hidden id " + page.getPickedChildId() + ")", true);
        Assert.assertFalse(page.getChildName().isEmpty(), "❌ Child name not filled");
        Assert.assertFalse(page.getChildCenter().isEmpty(), "❌ Centre not filled");
        page.searchChild("9999999");
        Reporter.log("   unknown child → message '" + page.getPopupMessage() + "'", true);
        page.closePopup();
        Reporter.log("✅ SC002_TC_001 PASSED", true);
    }

    @Test(priority = 21, description = "SC002_TC_004 / new — Mandatory fields, invoice reference, keyword guard")
    public void sc002_tc004_validation() throws InterruptedException {
        Reporter.log("▶ SC002_TC_004 — Validation", true);
        page.openAddRefund();
        page.startCapture(false);
        page.clickAddSubmit();
        List<String> e1 = page.getPopupErrors();
        Reporter.log("   empty → " + e1, true);
        Assert.assertTrue(e1.containsAll(Arrays.asList("Please specify refund amount.", "Please specify some details.", "Please specify refund type.")), "❌ Messages: " + e1);
        page.searchChild(child);
        page.selectRefundType("Refund - Registration Fee");
        List<String> inv = page.getPopupOptions("invoice_list");
        Reporter.log("   Registration Fee invoices " + inv, true);
        Assert.assertTrue(inv.size() > 1, "❌ No invoice reference for the child");
        page.setPopupField("#actual_payment_received", "100");
        page.setPopupField("#refund_description", "QA book refund check");
        page.clickAddSubmit();
        Reporter.log("   errors " + page.getPopupErrors(), true);
        Assert.assertTrue(page.getPopupErrors().contains("Please select invoice."), "❌ Invoice required: " + page.getPopupErrors());
        page.selectInvoice(inv.get(1).split("=")[1]);
        page.clickAddSubmit();
        String guard = page.getPopupMessage();
        Reporter.log("   'book' in description with Registration Fee → '" + guard + "' confirms " + page.getCapturedConfirms(), true);
        Assert.assertTrue(guard.contains("Refund type must be set to 'Book Charges'"), "❌ Keyword guard: " + guard);
        Assert.assertTrue(page.getCapturedConfirms().isEmpty(), "❌ Confirm shown despite the guard");
        // Security Deposit lists a fake "NOT-AVAILABLE" invoice
        page.selectRefundType("Refund - Security Deposit");
        List<String> sd = page.getPopupOptions("invoice_list");
        Reporter.log("   Security Deposit invoices " + sd, true);
        boolean fake = sd.stream().anyMatch(o -> o.contains("NOT-AVAILABLE"));
        if (fake) evidence("03_Security_Deposit_invoice_NOT-AVAILABLE");
        page.closePopup();
        Assert.assertFalse(fake, "❌ Security Deposit offers a 'NOT-AVAILABLE' invoice reference (bug)");
        Reporter.log("✅ SC002_TC_004 PASSED", true);
    }

    @Test(priority = 22, description = "SC002_TC_006 — Active child: 15 Days Withdrawal refund is blocked")
    public void sc002_tc006_activeChildBlocked() throws InterruptedException {
        Reporter.log("▶ SC002_TC_006 — Active child " + activeChild, true);
        page.openAddRefund();
        page.searchChild(activeChild);
        page.selectRefundType("Refund - Tuition Fee");
        List<String> inv = page.getPopupOptions("invoice_list");
        Reporter.log("   invoices " + inv + " | popup msg '" + page.getPopupMessage() + "'", true);
        page.setPopupField("#actual_payment_received", "100");
        page.setPopupField("#refund_description", "QA active child check " + stamp);
        if (inv.size() > 1) page.selectInvoice(inv.get(1).split("=")[1]);
        page.startCapture(true);
        page.clickAddSubmit();
        String msg = page.waitForPopupMessage();
        Reporter.log("   confirms " + page.getCapturedConfirms() + " → '" + msg + "'", true);
        // Sheet: "… are not allowed for active child"; live: "Since the child is in the Active stage, the system does not allow refund for …"
        boolean blocked = msg.toLowerCase().contains("active stage") && msg.toLowerCase().contains("does not allow");
        if (!blocked) evidence("04_Active_child_15_days_refund");
        page.closePopup();
        Assert.assertTrue(blocked, "❌ Active child not blocked: " + msg);
        Reporter.log("✅ SC002_TC_006 PASSED", true);
    }

    @Test(priority = 23, description = "SC002_TC_003 — Add New Refund (existing refunds table, then save)")
    public void sc002_tc003_addRefund() throws InterruptedException {
        Reporter.log("▶ SC002_TC_003 — Add refund for child " + child, true);
        page.openAddRefund();
        page.searchChild(child);
        page.setPopupField("#actual_payment_received", "100");
        page.setPopupField("#ticket_id", ticket);
        page.setPopupField("#refund_description", desc);
        page.selectRefundType("Refund - Registration Fee");
        List<String> inv = page.getPopupOptions("invoice_list");
        Assert.assertTrue(inv.size() > 1, "❌ No invoice");
        page.selectInvoice(inv.get(1).split("=")[1]);
        page.startCapture(true);
        page.clickAddSubmit();
        String table = page.getExistingRefundTable();
        Reporter.log("   confirms " + page.getCapturedConfirms() + " | existing: " + table.substring(0, Math.min(300, table.length())) + " | button '" + page.getAddButtonText() + "'", true);
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(ADD_CONFIRM), "❌ Confirm");
        if (!table.isEmpty()) {
            Assert.assertTrue(table.contains("Please check & verify existing refund before process for new refund"), "❌ Existing-refund note");
            Assert.assertEquals(page.getAddButtonText(), "I have verified existing refund and continue with new refund", "❌ Continue button");
            page.startCapture(true);
            page.clickAddSubmit();
        }
        String msg = page.waitForPopupMessage();
        Reporter.log("   message '" + msg + "'", true);
        Assert.assertTrue(msg.contains("Detail Updated successfully"), "❌ Save message: " + msg);
        Thread.sleep(3000);
        reopen();
        filterChild(child);
        Map<String, String> r = page.getRows().stream().filter(x -> x.get("Description").contains(desc)).findFirst().orElse(null);
        Assert.assertNotNull(r, "❌ New refund not listed");
        refundId = r.get("Refund Id");
        Reporter.log("   new refund " + refundId + " | " + r, true);
        Assert.assertEquals(r.get("Status"), "Advice", "❌ Status");
        Assert.assertEquals(r.get("Type"), "Refund - Registration Fee", "❌ Type");
        Assert.assertTrue(r.get("Amount").replace(",", "").contains("100"), "❌ Amount " + r.get("Amount"));
        // Maker-checker (QueryLogs): the issuer gets Void / Freshdesk / Bank Detail, no Verify
        Assert.assertTrue(r.get("actions").contains("Void Refund"), "❌ Advice actions " + r.get("actions"));
        Reporter.log("   own Advice refund actions: " + r.get("actions") + (r.get("actions").contains("Verify") ? "" : " (no Verify for the issuer)"), true);
        // SC006_TC_001 — Freshdesk link of the ticket
        String fd = page.getActionHref(refundId, "Freshdesk Ticket Link");
        Reporter.log("   Freshdesk " + fd, true);
        Assert.assertEquals(fd, "https://footprints.freshdesk.com/a/tickets/" + ticket + "|_blank", "❌ Freshdesk link");
        Reporter.log("✅ SC002_TC_003 PASSED — refund " + refundId, true);
    }

    // ✅ Row of a refund looked up through its child (all statuses, no dates — avoids the 500-row cap)
    private Map<String, String> rowViaChild(String childId, String id) throws InterruptedException {
        reopen();
        page.setDate("date_from", "");
        page.setDate("date_to", "");
        filterChild(childId);
        return page.getRowByRefundId(id);
    }

    // Own refund: this run's, else the newest "QA Auto refund" of the child that is still Advice / Verified
    private void needOwnRefund(String... statuses) throws InterruptedException {
        filterChild(child);
        if (!refundId.isEmpty() && page.getRowByRefundId(refundId) != null) return;
        refundId = page.getRows().stream().filter(r -> r.get("Description").startsWith("QA Auto refund") && Arrays.asList(statuses).contains(r.get("Status")))
                .map(r -> r.get("Refund Id")).max(Comparator.comparingLong(Long::parseLong)).orElse("");
        if (refundId.isEmpty()) throw new SkipException("No own refund (" + Arrays.toString(statuses) + ") for child " + child);
        Reporter.log("   using own refund " + refundId, true);
    }

    @Test(priority = 24, description = "SC005_TC_006 (new) — Verify own refund: Advice → Verified")
    public void sc005_tc006_verifyRefund() throws InterruptedException {
        Reporter.log("▶ SC005_TC_006 — Verify refund", true);
        needOwnRefund("Advice");
        String ownActions = page.getRowByRefundId(refundId).get("actions");
        Reporter.log("   own refund " + refundId + " actions: " + ownActions, true);
        String verifyId = refundId;
        if (!ownActions.contains("Verify")) {
            // Maker-checker: the user who issued a refund gets no Verify action — verify one issued by someone else
            ownVerifyMissing = true;
            page.setStatuses("Advice");
            page.typeChildId("");
            page.clickSubmit();
            page.showAllRows();
            verifyId = page.getRows().stream().filter(r -> r.get("actions").contains("Verify") && !r.get("popover").contains("Refund Issued By : Rakesh Singh"))
                    .map(r -> r.get("Refund Id")).findFirst().orElse("");
            if (verifyId.isEmpty()) throw new SkipException("No Advice refund with Verify");
            Map<String, String> v = page.getRowByRefundId(verifyId);
            Reporter.log("   verifying refund " + verifyId + " instead (" + v.get("Child Name") + ", " + v.get("Type") + ", " + v.get("Amount") + ")", true);
        }
        final String vid = verifyId;
        String vChild = page.getRowByRefundId(vid).get("childId");
        page.openDetails(vid, "Verify");
        String txt = page.getPopupText();
        Reporter.log("   " + txt.substring(0, Math.min(400, txt.length())) + " | button '" + page.getProcessButtonText() + "'", true);
        Assert.assertTrue(txt.contains("#" + vChild) && txt.toLowerCase().contains("advice"), "❌ Popup header");
        page.startCapture(true);
        page.clickProcessButton();
        String msg = page.waitForPopupMessage();
        Reporter.log("   confirms " + page.getCapturedConfirms() + " → '" + msg + "'", true);
        Assert.assertTrue(page.getCapturedConfirms().get(0).startsWith("Are you sure you want to mark as "), "❌ Confirm");
        Assert.assertTrue(msg.contains("Detail Updated successfully"), "❌ Verify message: " + msg);
        Thread.sleep(3000);
        reopen();
        filterChild(vChild);
        Map<String, String> r = page.getRowByRefundId(vid);
        Reporter.log("   after verify " + r.get("Status") + " | " + r.get("Verification Date") + " | actions " + r.get("actions") + " / " + r.get("actionText"), true);
        Assert.assertEquals(r.get("Status"), "Verified", "❌ Status after Verify");
        if (r.get("actionText").contains("Vefified")) evidence("05_Verified_label_typo_Vefified");
        Assert.assertFalse(r.get("actionText").contains("Vefified"), "❌ Label typo 'Vefified' (bug)");
        Reporter.log("✅ SC005_TC_006 PASSED — refund " + vid + (ownVerifyMissing ? " (own refund " + refundId + " had no Verify action)" : ""), true);
    }

    @Test(priority = 25, description = "SC005_TC_003 — Void own refund")
    public void sc005_tc003_voidRefund() throws InterruptedException {
        Reporter.log("▶ SC005_TC_003 — Void refund", true);
        needOwnRefund("Verified", "Advice");
        page.startCapture(false);
        page.clickVoid(refundId);
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(VOID_CONFIRM), "❌ Void confirm");
        String st = page.getRowByRefundId(refundId).get("Status");
        Assert.assertTrue(st.equals("Verified") || st.equals("Advice"), "❌ Cancel changed the status: " + st);
        page.startCapture(true);
        page.clickVoid(refundId);
        String msg = page.waitForPageMessage();
        Reporter.log("   message '" + msg + "'", true);
        Assert.assertTrue(msg.toLowerCase().contains("void"), "❌ Void message: " + msg);
        reopen();
        filterChild(child);
        Map<String, String> r = page.getRowByRefundId(refundId);
        Reporter.log("   after void " + (r == null ? "not listed" : r.get("Status") + " | " + r.get("actions")), true);
        Assert.assertNotNull(r, "❌ Voided refund not listed with all statuses");
        Assert.assertEquals(r.get("Status"), "Voided", "❌ Status after Void");
        Reporter.log("✅ SC005_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC006 — Bank details, Return, Verify Transfer
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC006_TC_002 / SC006_TC_004 — Bank details links, Send Email confirm, Incorrect Bank Details validation (no save)")
    public void sc006_tc002_bankDetails() throws InterruptedException {
        Reporter.log("▶ SC006_TC_002 — Bank details", true);
        page.setStatuses(RefundsIssuedPage.ALL_STATUSES);
        page.clickSubmit();
        page.showAllRows();
        Map<String, String> awaited = page.getRows().stream().filter(r -> r.get("Status").equals("Bank Details Awaited")).findFirst().orElse(null);
        if (awaited != null) {
            String id = awaited.get("Refund Id");
            String href = page.getActionHref(id, "Update Bank Details");
            Reporter.log("   Bank Details Awaited " + id + " → " + href, true);
            Assert.assertTrue(href.startsWith("https://test.footprintseducation.in/update-bank-details?id=") && href.endsWith("|_blank"), "❌ Update Bank Details link");
            page.startCapture(false);
            page.clickSendBankEmail(id);
            Reporter.log("   Send Email confirm " + page.getCapturedConfirms() + " (cancelled)", true);
            Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList("Send Bank Details email to the parent for this refund?"), "❌ Send Email confirm");
        }
        Map<String, String> ti = page.getRows().stream().filter(r -> r.get("actions").contains("Incorrect Bank Details")).findFirst().orElse(null);
        if (ti == null) throw new SkipException("No refund with Incorrect Bank Details");
        page.clickIncorrectBank(ti.get("Refund Id"));
        Thread.sleep(800);
        Reporter.log("   modal: " + page.getIncorrectBankModalText(), true);
        Assert.assertTrue(page.isIncorrectBankModalShown(), "❌ Mark Invalid Bank Details modal");
        page.startCapture(false);
        page.submitIncorrectBank();
        Reporter.log("   empty submit → " + page.getIncorrectBankErrors() + " confirms " + page.getCapturedConfirms(), true);
        Assert.assertTrue(page.getCapturedConfirms().isEmpty(), "❌ Confirm without a comment");
        page.closeIncorrectBankModal();
        Reporter.log("✅ SC006_TC_002 PASSED (nothing saved)", true);
    }

    @Test(priority = 31, description = "SC006_TC_007 — Return a Transfer Initiated refund")
    public void sc006_tc007_return() throws InterruptedException {
        Reporter.log("▶ SC006_TC_007 — Return", true);
        page.setStatuses("Transfer Initiated");
        page.setDate("date_from", LocalDate.now().minusMonths(6).toString());
        page.clickSubmit();
        page.showAllRows();
        String id = System.getProperty("rfReturnId", "");
        if (id.isEmpty()) id = page.getRows().stream().filter(r -> r.get("actions").contains("Return Refund") && r.get("actions").contains("Manual processing"))
                .map(r -> r.get("Refund Id")).findFirst().orElse("");
        if (id.isEmpty()) throw new SkipException("No Transfer Initiated refund to return");
        Map<String, String> before = page.getRowByRefundId(id);
        if (before == null) {
            String rc = System.getProperty("rfReturnChild", "");
            Map<String, String> done = rc.isEmpty() ? null : rowViaChild(rc, id);
            Reporter.log("   refund " + id + " is no longer Transfer Initiated: " + (done == null ? "not found (pass -DrfReturnChild)" : done.get("Status") + " | class " + done.get("rowClass")), true);
            Assert.assertNotNull(done, "❌ Refund " + id + " not found");
            Assert.assertEquals(done.get("Status"), "Return", "❌ Status of returned refund " + id);
            throw new SkipException("Refund " + id + " was already returned in an earlier run (state checked, nothing saved); the confirm-wording bug stays open");
        }
        Reporter.log("   returning refund " + id + " | " + before.get("Child Name") + " " + before.get("Type") + " " + before.get("Amount"), true);
        page.startCapture(true);
        page.clickReturn(id);
        String confirm = page.getCapturedConfirms().isEmpty() ? "" : page.getCapturedConfirms().get(0);
        String msg = page.waitForPageMessage();
        Thread.sleep(1500);
        String cls = page.getRowByRefundId(id) == null ? "" : page.getRowByRefundId(id).get("rowClass");
        Reporter.log("   confirm '" + confirm + "' → '" + msg + "' | row class " + cls, true);
        evidence("06_Return_confirm_says_void");
        Assert.assertTrue(msg.toLowerCase().contains("return"), "❌ Return message: " + msg);
        Map<String, String> r = rowViaChild(before.get("childId"), id);
        Assert.assertNotNull(r, "❌ Refund " + id + " not listed");
        Assert.assertEquals(r.get("Status"), "Return", "❌ Status after Return");
        Reporter.log("   after: " + r.get("Status") + " | class " + r.get("rowClass"), true);
        Assert.assertEquals(confirm, "Are you sure want to return this refund ?", "❌ Return confirm wording (it says 'void') — refund " + id + " was returned (bug)");
        Reporter.log("✅ SC006_TC_007 PASSED — refund " + id, true);
    }

    @Test(priority = 32, description = "SC006_TC_005 — Verify Transfer (15-day withdrawal) → Refunded")
    public void sc006_tc005_verifyTransfer() throws InterruptedException {
        Reporter.log("▶ SC006_TC_005 — Verify Transfer", true);
        page.setStatuses("Transfer Initiated");
        page.setDate("date_from", LocalDate.now().minusMonths(6).toString());
        page.clickSubmit();
        page.showAllRows();
        String id = System.getProperty("rfTransferId", "");
        if (id.isEmpty()) id = page.getRows().stream().filter(r -> r.get("actions").contains("Verify Transfer") && !r.get("actions").contains("Manual processing"))
                .map(r -> r.get("Refund Id")).findFirst().orElse("");
        if (id.isEmpty()) throw new SkipException("No 15-day Transfer Initiated refund");
        Map<String, String> target = page.getRowByRefundId(id);
        if (target == null) {
            String tc = System.getProperty("rfTransferChild", "");
            Map<String, String> done = tc.isEmpty() ? null : rowViaChild(tc, id);
            Reporter.log("   refund " + id + " is no longer Transfer Initiated: " + (done == null ? "not found (pass -DrfTransferChild)" : done.get("Status") + " | Closure " + done.get("Closure/ Refunded Date")), true);
            Assert.assertNotNull(done, "❌ Refund " + id + " not found");
            Assert.assertEquals(done.get("Status"), "Refunded", "❌ Status of transferred refund " + id);
            throw new SkipException("Refund " + id + " was already marked Refunded in an earlier run (state checked, nothing saved)");
        }
        String tChild = target.get("childId");
        Map<String, String> other = page.getRows().stream().filter(r -> r.get("actions").contains("Manual processing")).findFirst().orElse(null);
        Reporter.log("   disabled Verify Transfer example: " + (other == null ? "none" : other.get("Refund Id") + " " + other.get("Type")), true);
        page.openDetails(id, "Verify Transfer");
        String pt = page.getPopupText();
        Reporter.log("   " + pt.substring(0, Math.min(300, pt.length())) + " | button '" + page.getProcessButtonText() + "'", true);
        page.startCapture(false);
        page.clickProcessButton();
        Reporter.log("   empty Transaction Reference → " + page.getPopupErrors(), true);
        Assert.assertTrue(page.getCapturedConfirms().isEmpty(), "❌ Confirm without Transaction Reference");
        page.setPopupField("#transaction_reference", "QATXN" + stamp);
        page.startCapture(true);
        page.clickProcessButton();
        String msg = page.waitForPopupMessage();
        Reporter.log("   confirms " + page.getCapturedConfirms() + " → '" + msg + "'", true);
        Assert.assertTrue(msg.contains("Detail Updated successfully"), "❌ Verify Transfer message: " + msg);
        Thread.sleep(3000);
        Map<String, String> r = rowViaChild(tChild, id);
        Assert.assertNotNull(r, "❌ Refund " + id + " not listed");
        Reporter.log("   after: " + r.get("Status") + " | Closure " + r.get("Closure/ Refunded Date") + " | actions " + r.get("actions"), true);
        Assert.assertEquals(r.get("Status"), "Refunded", "❌ Status after Verify Transfer");
        Reporter.log("✅ SC006_TC_005 PASSED — refund " + id, true);
    }
}
