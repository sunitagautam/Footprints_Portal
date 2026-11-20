package testScripts.SupportTests.FinancialUpdates;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.FinancialUpdates.SecurityFeePage;
import utils.BaseTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════════
// SECURITY FEE (Support → Financial Updates → Security Fee, /security_deposit) — read-only report
// No source test sheet: cases written from the live screen (2026-11-20) → TC_Support_Financial_Updates.xlsx, sheet
// TC_SecurityFee. Runs as Rakesh. Nothing is saved (the screen has no actions).
// Evidence screenshots for failures: ~/Desktop/QA_Documents/Bug Evidence/FinancialUpdates_SecurityFee/.
// ═══════════════════════════════════════════════════════════════
public class SecurityFeeTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Child Name", "Description", "Amount Deposit", "Received Date", "Status",
            "Remaining Amount", "Transfer Date", "Refund Details", "Adjusted Amount");
    private static final File EVIDENCE = new File(System.getProperty("user.home"), "Desktop/QA_Documents/Bug Evidence/FinancialUpdates_SecurityFee");
    private static final DateTimeFormatter ROW_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FILTER_DATE = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH);
    private static final LocalDate WIDE_FROM = LocalDate.of(2024, 1, 1);

    Navigations navigations;
    SecurityFeePage page;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
        try { driver.manage().window().setSize(new org.openqa.selenium.Dimension(1680, 1050)); } catch (Exception ignored) { }
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws Exception {
        try {
            driver.getTitle();
        } catch (Exception e) {
            try { driver.quit(); } catch (Exception ignored) { }
            openBrowser("chrome");
            setUp();
        }
        dismissAlertIfPresent();
        reopen();
    }

    private void reopen() throws InterruptedException {
        navigations.goToSecurityFee();
        dismissAlertIfPresent();
        page = new SecurityFeePage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Security Fee did not load");
        if (!page.getStatusSelected().isEmpty() || !page.getValue("center_selection").equals("All Center")
                || !LocalDate.parse(page.getValue("date_from"), FILTER_DATE).equals(LocalDate.now().minusMonths(1))) {
            page.clickReset();
        }
    }

    private void wide() throws InterruptedException {
        page.setDate("date_from", WIDE_FROM.toString());
        page.setDate("date_to", LocalDate.now().toString());
        page.clickSubmit();
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

    private List<String> col(String key) {
        return page.getRows().stream().map(r -> r.get(key)).collect(Collectors.toList());
    }

    @Test(priority = 1, description = "SC001_TC_001 — Default view")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);
        Assert.assertEquals(page.getMenuPath(), "Support > Financial Updates > Security Fee", "❌ Menu path");
        Assert.assertEquals(page.getHeading(), "Security Deposit Fee", "❌ Heading");
        Assert.assertEquals(page.getFilterLabels(), Arrays.asList("Date From", "Date To", "Status", "Center"), "❌ Filter labels");
        Assert.assertEquals(LocalDate.parse(page.getValue("date_from"), FILTER_DATE), LocalDate.now().minusMonths(1), "❌ Date From default");
        Assert.assertEquals(LocalDate.parse(page.getValue("date_to"), FILTER_DATE), LocalDate.now(), "❌ Date To default");
        Assert.assertEquals(page.getStatusButtonText(), "-- All --", "❌ Status default");
        Assert.assertEquals(page.getStatusOptions(), Arrays.asList("Deposited=Received", "Transferred=Transferred", "Partially Refunded=Partially Refunded", "Refunded=Refunded"), "❌ Status options");
        Assert.assertEquals(page.getValue("center_selection"), "All Center", "❌ Center default");
        Assert.assertTrue(page.getCenterOptions().size() > 100, "❌ Centre list");
        Assert.assertEquals(page.getDefinedHeaders(), COLUMNS, "❌ Columns");
        Assert.assertTrue(page.isSearchBoxVisible() && page.isPaginationVisible(), "❌ Search / pagination");
        Assert.assertEquals(page.getDownloadButtonText(), "Download Report", "❌ Download button");
        Assert.assertEquals(page.getPageLength(), 10, "❌ Rows per page");
        Assert.assertEquals(page.getOrder(), "[[4,\"desc\"]]", "❌ Default order (Received Date desc)");
        Reporter.log("   " + page.getInfoText() + (page.getRowCount() == 0 ? " | empty: '" + page.getEmptyMessage() + "'" : ""), true);
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    @Test(priority = 2, description = "SC001_TC_002 — Date range filter (Received Date)")
    public void sc001_tc002_dateRange() throws InterruptedException {
        Reporter.log("▶ SC001_TC_002 — Date range", true);
        wide();
        List<String> d = col("Received Date");
        Reporter.log("   " + WIDE_FROM + " → today: " + d.size() + " rows", true);
        Assert.assertTrue(d.size() > 0, "❌ No data");
        List<String> out = d.stream().filter(x -> { LocalDate v = LocalDate.parse(x, ROW_DATE); return v.isBefore(WIDE_FROM) || v.isAfter(LocalDate.now()); }).collect(Collectors.toList());
        Assert.assertTrue(out.isEmpty(), "❌ Received dates outside the range: " + out);
        Assert.assertEquals(LocalDate.parse(page.getValue("date_from"), FILTER_DATE), WIDE_FROM, "❌ Date From not kept");
        // From after To → nothing (no validation message on screen)
        page.setDate("date_from", LocalDate.now().toString());
        page.setDate("date_to", LocalDate.now().minusMonths(1).toString());
        page.clickSubmit();
        Reporter.log("   From > To → " + page.getRowCount() + " rows, '" + page.getEmptyMessage() + "'", true);
        Assert.assertEquals(page.getRowCount(), 0, "❌ Rows for From > To");
        Reporter.log("✅ SC001_TC_002 PASSED", true);
    }

    @Test(priority = 3, description = "SC001_TC_003 — Status filter; option labels match the table")
    public void sc001_tc003_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_003 — Status", true);
        Map<String, Integer> c = new LinkedHashMap<>();
        page.setDate("date_from", WIDE_FROM.toString());
        for (String v : Arrays.asList("Received", "Transferred", "Partially Refunded", "Refunded")) {
            page.setStatuses(v);
            page.clickSubmit();
            Set<String> got = new TreeSet<>(col("Status"));
            c.put(v, page.getRowCount());
            List<LocalDate> rd = col("Received Date").stream().map(x -> LocalDate.parse(x, ROW_DATE)).sorted().collect(Collectors.toList());
            if (!rd.isEmpty()) Reporter.log("   " + v + ": " + rd.size() + " rows, Received Date " + rd.get(0) + " → " + rd.get(rd.size() - 1), true);
            if (v.equals("Refunded") && page.getRowCount() > 141) evidence("03_Status_Refunded_ignores_date_range");
            Assert.assertTrue(got.isEmpty() || got.equals(Collections.singleton(v)), "❌ Status " + v + " shows " + got);
            Assert.assertEquals(page.getStatusSelected(), Collections.singletonList(v), "❌ Status not kept");
        }
        // The filter calls "Received" deposits "Deposited" but the table shows "Received"
        page.setStatuses("Received");
        page.clickSubmit();
        boolean same = page.getStatusButtonText().equals(page.getRows().isEmpty() ? page.getStatusButtonText() : page.getRows().get(0).get("Status"));
        Reporter.log("   filter shows '" + page.getStatusButtonText() + "', table shows '" + (page.getRows().isEmpty() ? "" : page.getRows().get(0).get("Status")) + "'", true);
        if (!same) evidence("01_Status_Deposited_vs_Received");
        boolean labelOk = same;
        page.setStatuses();
        page.clickSubmit();
        int all = page.getRowCount();
        Reporter.log("   " + c + " | all " + all, true);
        int sum = c.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != all) evidence("03b_Status_All_count");
        Assert.assertTrue(labelOk, "❌ Status label differs: filter 'Deposited' vs table 'Received' (bug)");
        Assert.assertEquals(sum, all, "❌ All statuses (" + all + ") ≠ sum of each status " + c + " — the date range is applied differently per status (bug?)");
        Reporter.log("✅ SC001_TC_003 PASSED", true);
    }

    @Test(priority = 4, description = "SC001_TC_004 — Center filter")
    public void sc001_tc004_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_004 — Center", true);
        wide();
        Set<String> all = new HashSet<>(col("Child Id"));
        List<String> centers = page.getCenterOptions();
        String found = "";
        int n = 0;
        for (String o : centers.subList(1, Math.min(centers.size(), 40))) {
            page.setCenter(o.substring(o.lastIndexOf('=') + 1));
            page.clickSubmit();
            if (page.getRowCount() > 0) { found = o; n = page.getRowCount(); break; }
        }
        Assert.assertFalse(found.isEmpty(), "❌ No centre with security deposits among the first 39");
        Set<String> ids = new HashSet<>(col("Child Id"));
        Reporter.log("   " + found + " → " + n + " rows (of " + all.size() + " children in all centres)", true);
        Assert.assertTrue(all.containsAll(ids), "❌ Centre rows not in the all-centre list");
        Assert.assertTrue(n < all.size(), "❌ Centre filter did not narrow");
        Assert.assertEquals(page.getValue("center_selection"), found.substring(0, found.lastIndexOf('=')), "❌ Centre not kept");
        Reporter.log("✅ SC001_TC_004 PASSED", true);
    }

    @Test(priority = 5, description = "SC001_TC_005 — Reset")
    public void sc001_tc005_reset() throws InterruptedException {
        Reporter.log("▶ SC001_TC_005 — Reset", true);
        page.setDate("date_from", WIDE_FROM.toString());
        page.setStatuses("Refunded");
        page.clickSubmit();
        page.clickReset();
        Reporter.log("   after Reset: " + page.getValue("date_from") + " → " + page.getValue("date_to") + " | " + page.getStatusButtonText() + " | " + page.getValue("center_selection"), true);
        Assert.assertEquals(LocalDate.parse(page.getValue("date_from"), FILTER_DATE), LocalDate.now().minusMonths(1), "❌ Date From");
        Assert.assertEquals(page.getStatusButtonText(), "-- All --", "❌ Status");
        Assert.assertEquals(page.getValue("center_selection"), "All Center", "❌ Center");
        Reporter.log("✅ SC001_TC_005 PASSED", true);
    }

    @Test(priority = 6, description = "SC002_TC_001 — Data per status (remaining, transfer, refund details)")
    public void sc002_tc001_dataPerStatus() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Data per status", true);
        wide();
        List<String> bad = new ArrayList<>();
        Map<String, Map<String, String>> sample = new LinkedHashMap<>();
        for (Map<String, String> r : page.getRows()) {
            String st = r.get("Status");
            sample.putIfAbsent(st, r);
            String amt = r.get("Amount Deposit");
            if (!amt.matches("Rs\\. [\\d,]+\\.\\d{2}")) bad.add(r.get("Child Id") + " amount '" + amt + "'");
            switch (st) {
                case "Received":
                    if (!r.get("Remaining Amount").equals(amt)) bad.add(r.get("Child Id") + " Received but remaining '" + r.get("Remaining Amount") + "' ≠ " + amt);
                    break;
                case "Refunded":
                    if (!r.get("Refund Details").matches("Date: \\w{3} \\d{1,2}, \\d{4}\\s*Rs\\. [\\d,]+\\.\\d{2}")) bad.add(r.get("Child Id") + " Refunded without refund details '" + r.get("Refund Details") + "'");
                    break;
                case "Transferred":
                    if (r.get("Transfer Date").isEmpty() && r.get("Adjusted Amount").isEmpty()) bad.add(r.get("Child Id") + " Transferred without transfer date / adjusted amount");
                    break;
                default:
            }
        }
        sample.forEach((k, v) -> Reporter.log("   " + k + ": " + v, true));
        if (!bad.isEmpty()) {
            page.setStatuses("Transferred");
            page.clickSubmit();
            page.setStatuses();
            wide();
            page.searchTable("Transferred");
            evidence("04_Transferred_rows_without_transfer_date");
            page.searchTable("51434");
            evidence("05_Received_row_51434_without_remaining_amount");
            page.searchTable("");
        }
        Reporter.log("   problems: " + bad, true);
        Assert.assertTrue(bad.isEmpty(), "❌ Data problems: " + bad);
        Reporter.log("✅ SC002_TC_001 PASSED", true);
    }

    @Test(priority = 7, description = "SC003_TC_001 — Search box")
    public void sc003_tc001_search() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Search", true);
        wide();
        int all = page.getRowCount();
        String name = page.getRows().get(all / 2).get("Child Name");
        page.searchTable(name);
        Assert.assertTrue(page.getRowCount() > 0 && page.getRowCount() < all, "❌ Search");
        page.searchTable("zzqq");
        Assert.assertEquals(page.getEmptyMessage(), "No matching records found", "❌ No-match");
        Reporter.log("   '" + name + "' ok; no match ok", true);
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    @Test(priority = 8, description = "SC003_TC_002 — Download Report")
    public void sc003_tc002_download() throws Exception {
        Reporter.log("▶ SC003_TC_002 — Download", true);
        wide();
        int rows = page.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        page.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] f = downloads.listFiles((d, nm) -> nm.endsWith(".csv"));
            if (f != null) for (File x : f) if (x.lastModified() >= start - 2000 && (csv == null || x.lastModified() > csv.lastModified())) csv = x;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No CSV downloaded");
        List<String> lines = Files.readAllLines(csv.toPath());
        long data = lines.stream().skip(1).filter(l -> l.startsWith("\"")).count();
        Reporter.log("   " + csv.getName() + " | " + data + " data rows (grid " + rows + ") | header " + lines.get(0), true);
        Assert.assertTrue(lines.get(0).contains("Child Name") && lines.get(0).contains("Amount Deposit"), "❌ Header");
        List<String> missing = COLUMNS.stream().filter(h -> !lines.get(0).contains("\"" + h + "\"")).collect(Collectors.toList());
        Reporter.log("   grid columns missing in the CSV: " + missing, true);
        Assert.assertEquals(data, rows, "❌ CSV rows ≠ grid rows");
        if (!csv.getName().startsWith("Security")) {
            driver.switchTo().newWindow(org.openqa.selenium.WindowType.TAB);
            driver.get("chrome://downloads");
            Thread.sleep(2000);
            evidence("02_Security_Fee_CSV_named_Refund_Issued_Report");
            driver.close();
            driver.switchTo().window(driver.getWindowHandles().iterator().next());
        }
        Assert.assertTrue(csv.getName().startsWith("Security"), "❌ CSV of Security Fee is named " + csv.getName() + " (bug)");
        Assert.assertTrue(missing.isEmpty(), "❌ CSV misses grid columns " + missing + " (bug)");
        Reporter.log("✅ SC003_TC_002 PASSED", true);
    }

    @Test(priority = 9, description = "SC003_TC_003 — Pagination, sorting, child link")
    public void sc003_tc003_paginationSortingLink() throws InterruptedException {
        Reporter.log("▶ SC003_TC_003 — Pagination / sorting / link", true);
        wide();
        Assert.assertTrue(page.getInfoText().startsWith("Showing 1 to 10 of"), "❌ " + page.getInfoText());
        page.clickNextPage();
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Next");
        List<String> d = page.getSortData(4);
        Reporter.log("   default order first: " + d.subList(0, 3), true);
        for (String h : Arrays.asList("Child Name", "Amount Deposit", "Status")) {
            page.clickColumnHeader(h);
            Assert.assertTrue(page.getHeaderSortClass(h).matches(".*sorting_(asc|desc).*"), "❌ " + h + " not sortable");
        }
        Map<String, String> r = page.getRows().get(0);
        Assert.assertEquals(r.get("childHref"), "account_statement?child_id=" + r.get("Child Id") + "|_blank", "❌ Child link");
        Reporter.log("✅ SC003_TC_003 PASSED", true);
    }
}
