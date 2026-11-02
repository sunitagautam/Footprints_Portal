package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.CorporateAdmissionCountPage;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ═══════════════════════════════════════════════════════════════
// CORPORATE ADMISSION COUNT (Franchise → Corporate Tieups → Corporate Admission Count, /tieup_admission_count)
// New screen — no source test cases; written from the requirement the user shared (2026-11-02) + live screen →
// TC_Corporate_Tieups.xlsx, sheet TC_CorporateAdmissionCount. Right View_Corporate_Admission_Count. Runs as Rakesh.
// Read-only: nothing is saved.
// ═══════════════════════════════════════════════════════════════
public class CorporateAdmissionCountTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Offer Title", "Company Name", "Offer Status", "Admission Count");
    private static final List<String> POPUP_COLUMNS = Arrays.asList("Admission ID", "Child Name", "Center", "Admission Date", "Status");
    private static final String CENTER = System.getProperty("countCenter", "Koramangala");

    Navigations navigations;
    CorporateAdmissionCountPage countPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToCorporateAdmissionCount();
        dismissAlertIfPresent();
        countPage = new CorporateAdmissionCountPage(driver);
        Assert.assertTrue(countPage.isPageLoaded(), "❌ Corporate Admission Count did not load");
    }

    private void reopen() throws InterruptedException {
        navigations.goToCorporateAdmissionCount();
        countPage = new CorporateAdmissionCountPage(driver);
        Assert.assertTrue(countPage.isPageLoaded(), "❌ Corporate Admission Count did not load");
    }

    // ✅ Summary rows with a count between min and max ("offer || company || status || count")
    private List<String[]> rowsWithCount(int min, int max, int limit) {
        List<String[]> out = new ArrayList<>();
        for (String r : countPage.getSummaryRows()) {
            String[] p = r.split(" \\|\\| ");
            int c = Integer.parseInt(p[3]);
            if (c >= min && c <= max) out.add(p);
            if (out.size() >= limit) break;
        }
        return out;
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "TC_001 — Default view")
    public void tc001_defaultView() throws InterruptedException {
        Reporter.log("▶ TC_001 — Default view", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='tieup_admission_count')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu: " + path + " | Heading: " + countPage.getHeading(), true);
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Corporate Admission Count", "❌ Menu path");
        Assert.assertEquals(countPage.getHeading(), "Tieup Admission Count", "❌ Heading");
        Assert.assertTrue(countPage.pageHasText("Corporate admissions by offer and company. Click an offer, company or count to see the admissions."), "❌ Help text");
        for (String id : Arrays.asList("offer_id", "company_id", "offer_status", "center_id", "btn_submit"))
            Assert.assertTrue(countPage.isFilterFieldVisible(id), "❌ Filter field " + id);
        Assert.assertEquals(countPage.getSelectedText("offer_id"), "--SELECT--", "❌ Default offer");
        Assert.assertEquals(countPage.getSelectedText("company_id"), "--SELECT--", "❌ Default company");
        Assert.assertEquals(countPage.getSelectOptions("offer_status"), Arrays.asList("--SELECT--", "Active", "All", "Expired"), "❌ Offer Status options");
        Assert.assertEquals(countPage.getSelectedText("offer_status"), "All", "❌ Default Offer Status");
        Assert.assertEquals(countPage.getSelectedText("center_id"), "All Centers", "❌ Default Center");
        List<String> visible = countPage.getVisibleColumnHeaders();
        Reporter.log("   Columns: " + visible + " | all: " + countPage.getAllColumnHeaders() + " | " + countPage.getInfoText()
                + " | per page " + countPage.getPageLength() + " | " + countPage.getDownloadButtonText(), true);
        Assert.assertEquals(visible, COLUMNS, "❌ Columns");
        Assert.assertEquals(countPage.getAllColumnHeaders(), COLUMNS, "❌ Hidden columns present");
        Assert.assertEquals(countPage.getPageLength(), 20, "❌ Rows per page");
        Assert.assertEquals(countPage.getDownloadButtonText(), "Download", "❌ Download button");
        Reporter.log("✅ TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Access right View_Corporate_Admission_Count (Rakesh has it → screen opens)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "TC_002 — Access right View_Corporate_Admission_Count")
    public void tc002_accessRight() throws InterruptedException {
        Reporter.log("▶ TC_002 — Access right", true);
        boolean has = new UserRightsPage(driver).hasRight("357", "View_Corporate_Admission_Count");
        reopen();
        Reporter.log("   Rakesh has View_Corporate_Admission_Count: " + has + " | screen: " + countPage.getHeading() + " | " + countPage.getInfoText(), true);
        Assert.assertTrue(has, "❌ Rakesh lacks View_Corporate_Admission_Count");
        Assert.assertTrue(countPage.getRowCount() > 0, "❌ No summary rows");
        Reporter.log("✅ TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003 — One row per Corporate Offer + Company; no child-count / center column
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "TC_003 — One summary row per offer and company")
    public void tc003_oneRowPerOfferCompany() {
        Reporter.log("▶ TC_003 — One row per offer + company", true);
        List<String> rows = countPage.getSummaryRows();
        Set<String> keys = new HashSet<>();
        List<String> dups = new ArrayList<>();
        long zero = 0;
        int sum = 0;
        for (String r : rows) {
            String[] p = r.split(" \\|\\| ");
            if (!keys.add(p[0] + " | " + p[1])) dups.add(p[0] + " | " + p[1]);
            int c = Integer.parseInt(p[3]);
            sum += c;
            if (c == 0) zero++;
        }
        Reporter.log("   " + rows.size() + " rows | total admissions " + sum + " | rows with count 0: " + zero + " (QueryLogs) | duplicates: " + dups, true);
        Assert.assertTrue(dups.isEmpty(), "❌ Duplicate offer + company rows: " + dups);
        Assert.assertFalse(countPage.getAllColumnHeaders().stream().anyMatch(h -> h.matches("(?i).*(center|branch|child).*")), "❌ Center / child column present");
        Reporter.log("✅ TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Offer, company and count links open the same popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "TC_004 — Offer, company and count are clickable and open the admissions popup")
    public void tc004_linksOpenPopup() throws InterruptedException {
        Reporter.log("▶ TC_004 — Links open popup", true);
        String[] row = rowsWithCount(1, 30, 1).get(0);
        List<String> links = countPage.getRowLinks(row[0], row[1]);
        Reporter.log("   Row " + row[0] + " / " + row[1] + " = " + row[3] + " | links " + links, true);
        Assert.assertEquals(links.size(), 3, "❌ Offer / company / count links");
        Assert.assertTrue(links.stream().allMatch(l -> l.contains("pop_tieup_admission_list") && l.equals(links.get(0))), "❌ The three links differ: " + links);
        String[] names = {"offer", "company", "count"};
        int[] cells = {0, 1, 3};
        for (int i = 0; i < 3; i++) {
            if (i > 0) reopen();
            countPage.clickRowLink(row[0], row[1], cells[i]);
            Assert.assertTrue(countPage.isPopupVisible(), "❌ Popup did not open from the " + names[i] + " link");
            Reporter.log("   " + names[i] + " link → '" + countPage.getPopupHeader() + "' | rows " + countPage.getPopupRowCount(), true);
            Assert.assertEquals(countPage.getPopupRowCount(), Integer.parseInt(row[3]), "❌ Popup rows from the " + names[i] + " link");
        }
        Reporter.log("✅ TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 — Popup: header, columns, one row per admission = summary count (several rows, all centers)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "TC_005 — Popup row count matches the summary count (all centers)")
    public void tc005_popupMatchesCount() throws InterruptedException {
        Reporter.log("▶ TC_005 — Popup count = summary count", true);
        List<String[]> sample = rowsWithCount(1, 60, 5);
        Assert.assertFalse(sample.isEmpty(), "❌ No summary row with admissions");
        for (int i = 0; i < sample.size(); i++) {
            String[] row = sample.get(i);
            if (i > 0) reopen();
            countPage.clickRowLink(row[0], row[1], 3);
            String header = countPage.getPopupHeader();
            int n = countPage.getPopupRowCount();
            List<String> ids = countPage.getPopupColumn("Admission ID");
            Reporter.log("   " + row[0] + " / " + row[1] + " = " + row[3] + " → '" + header + "' | rows " + n + " | distinct ids " + new HashSet<>(ids).size()
                    + " | centers " + new java.util.TreeSet<>(countPage.getPopupColumn("Center")) + " | statuses " + new java.util.TreeSet<>(countPage.getPopupColumn("Status")), true);
            Assert.assertEquals(countPage.getPopupHeaders(), POPUP_COLUMNS, "❌ Popup columns");
            Assert.assertTrue(header.startsWith(row[1] + " · " + row[3] + " admission(s)"), "❌ Popup header: " + header);
            Assert.assertTrue(header.contains(row[0]), "❌ Popup header offer: " + header);
            Assert.assertEquals(n, Integer.parseInt(row[3]), "❌ Popup rows vs summary count");
            Assert.assertEquals(new HashSet<>(ids).size(), n, "❌ Duplicate admissions in the popup");
        }
        Reporter.log("✅ TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_006 — Center filter is searchable
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "TC_006 — Searchable Center filter")
    public void tc006_centerSearchable() throws InterruptedException {
        Reporter.log("▶ TC_006 — Center filter searchable", true);
        Assert.assertTrue(countPage.isSelect2("center_id"), "❌ Center is not a searchable (select2) dropdown");
        List<String> res = countPage.searchSelect2("center_id", "Koram");
        Reporter.log("   'Koram' → " + res, true);
        Assert.assertFalse(res.isEmpty(), "❌ No results for 'Koram'");
        Assert.assertTrue(res.stream().allMatch(r -> r.toLowerCase().contains("koram")), "❌ Results not filtered: " + res);
        Reporter.log("✅ TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_007 — Center selected: totals for that center; popup honours the center and matches the count
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "TC_007 — Center filter: totals and popup for the selected center")
    public void tc007_centerTotals() throws InterruptedException {
        Reporter.log("▶ TC_007 — Center " + CENTER, true);
        java.util.Map<String, Integer> all = new java.util.HashMap<>();
        for (String r : countPage.getSummaryRows()) { String[] p = r.split(" \\|\\| "); all.put(p[0] + " | " + p[1], Integer.parseInt(p[3])); }
        countPage.selectByText("center_id", CENTER);
        countPage.clickFilter();
        Assert.assertEquals(countPage.getSelectedText("center_id"), CENTER, "❌ Center not kept after Filter");
        List<String[]> sample = new ArrayList<>();
        int sum = 0;
        for (String r : countPage.getSummaryRows()) {
            String[] p = r.split(" \\|\\| ");
            int c = Integer.parseInt(p[3]);
            sum += c;
            Integer a = all.get(p[0] + " | " + p[1]);
            Assert.assertTrue(a != null && c <= a, "❌ " + p[0] + " / " + p[1] + ": center count " + c + " > all-centers count " + a);
            if (c > 0 && sample.size() < 3) sample.add(p);
        }
        Reporter.log("   " + CENTER + ": " + countPage.getRowCount() + " rows, total " + sum + " | links carry center_id: "
                + (sample.isEmpty() ? "-" : countPage.getRowLinks(sample.get(0)[0], sample.get(0)[1]).get(0).replaceAll(".*(center_id=\\d+).*", "$1")), true);
        Assert.assertFalse(sample.isEmpty(), "❌ No admissions at " + CENTER);
        for (int i = 0; i < sample.size(); i++) {
            String[] row = sample.get(i);
            if (i > 0) { reopen(); countPage.selectByText("center_id", CENTER); countPage.clickFilter(); }
            countPage.clickRowLink(row[0], row[1], 3);
            List<String> centers = countPage.getPopupColumn("Center");
            Reporter.log("   " + row[0] + " / " + row[1] + " = " + row[3] + " (all centers " + all.get(row[0] + " | " + row[1]) + ") → popup rows "
                    + countPage.getPopupRowCount() + " | centers " + new HashSet<>(centers), true);
            Assert.assertEquals(countPage.getPopupRowCount(), Integer.parseInt(row[3]), "❌ Popup rows vs center count");
            Assert.assertTrue(centers.stream().allMatch(CENTER::equals), "❌ Popup lists other centers: " + new HashSet<>(centers));
        }
        Reporter.log("✅ TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 — Offer, Company and Offer Status filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "TC_008 — Offer, Company and Offer Status filters")
    public void tc008_filters() throws InterruptedException {
        Reporter.log("▶ TC_008 — Filters", true);
        String[] row = rowsWithCount(1, 1000, 1).get(0);
        countPage.selectByText("offer_id", row[0]);
        countPage.clickFilter();
        List<String> rows = countPage.getSummaryRows();
        Reporter.log("   Offer " + row[0] + " → " + rows, true);
        Assert.assertEquals(countPage.getSelectedText("offer_id"), row[0], "❌ Offer not kept");
        Assert.assertTrue(!rows.isEmpty() && rows.stream().allMatch(r -> r.startsWith(row[0] + " || ")), "❌ Offer filter");
        reopen();
        countPage.selectByText("company_id", row[1]);
        countPage.clickFilter();
        rows = countPage.getSummaryRows();
        Reporter.log("   Company " + row[1] + " → " + rows.size() + " rows", true);
        Assert.assertTrue(!rows.isEmpty() && rows.stream().allMatch(r -> r.split(" \\|\\| ")[1].equals(row[1])), "❌ Company filter");
        for (String st : Arrays.asList("Active", "Expired")) {
            reopen();
            countPage.selectByText("offer_status", st);
            countPage.clickFilter();
            rows = countPage.getSummaryRows();
            Reporter.log("   Offer Status " + st + " → " + rows.size() + " rows", true);
            Assert.assertTrue(!rows.isEmpty() && rows.stream().allMatch(r -> r.split(" \\|\\| ")[2].equals(st)), "❌ Offer Status " + st);
        }
        Reporter.log("✅ TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_009 — Search, pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "TC_009 — Search, pagination and sorting")
    public void tc009_searchPagingSorting() throws InterruptedException {
        Reporter.log("▶ TC_009 — Search / paging / sorting", true);
        List<String> first = countPage.getPageColumn(0);
        Assert.assertEquals(first.size(), 20, "❌ Rows per page");
        countPage.clickNextPage();
        Reporter.log("   Next → " + countPage.getInfoText(), true);
        Assert.assertTrue(countPage.getInfoText().contains("Showing 21 to 40"), "❌ Info after Next");
        countPage.clickPreviousPage();
        Assert.assertEquals(countPage.getPageColumn(0), first, "❌ Page 1 after Previous");
        for (String col : COLUMNS) {
            countPage.clickColumnHeader(col);
            String a = countPage.getHeaderSortClass(col);
            countPage.clickColumnHeader(col);
            String b = countPage.getHeaderSortClass(col);
            Reporter.log("   " + col + ": " + a + " → " + b, true);
            Assert.assertTrue(a.matches(".*sorting_(asc|desc).*") && b.matches(".*sorting_(asc|desc).*") && !a.equals(b), "❌ " + col + " sorting");
        }
        countPage.clickColumnHeader("Admission Count");
        boolean asc = countPage.getHeaderSortClass("Admission Count").contains("asc");
        List<Integer> counts = countPage.getPageColumn(3).stream().map(Integer::parseInt).toList();
        List<Integer> sorted = new ArrayList<>(counts);
        sorted.sort(asc ? java.util.Comparator.naturalOrder() : java.util.Comparator.reverseOrder());
        Reporter.log("   Admission Count " + (asc ? "asc" : "desc") + ": " + counts, true);
        Assert.assertEquals(counts, sorted, "❌ Admission Count numeric order");
        String offer = first.get(0);
        countPage.searchTable(offer);
        Assert.assertTrue(countPage.getPageColumn(0).contains(offer), "❌ Search");
        countPage.searchTable("zzqq-no-such-offer");
        Assert.assertEquals(countPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_010 — Download
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "TC_010 — Download")
    public void tc010_download() throws Exception {
        Reporter.log("▶ TC_010 — Download", true);
        int expected = countPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        countPage.clickDownload();
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
        Reporter.log("   File: " + csv.getName() + " | header " + header + " | rows " + rows + " (grid " + expected + ")", true);
        for (String c : COLUMNS) Assert.assertTrue(header.contains(c), "❌ CSV missing " + c);
        Assert.assertEquals(rows, expected, "❌ CSV rows vs grid");
        Reporter.log("✅ TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_011 — Popup search, download, masking
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "TC_011 — Popup search, download and data shown")
    public void tc011_popupTools() throws InterruptedException {
        Reporter.log("▶ TC_011 — Popup tools", true);
        String[] row = rowsWithCount(3, 60, 1).get(0);
        countPage.clickRowLink(row[0], row[1], 3);
        Assert.assertTrue(countPage.isPopupSearchVisible() && countPage.isPopupDownloadVisible(), "❌ Popup search / download");
        String id = countPage.getPopupColumn("Admission ID").get(0);
        countPage.searchPopup(id);
        Reporter.log("   Search " + id + " → " + countPage.getPopupInfo(), true);
        Assert.assertEquals(countPage.getPopupColumn("Admission ID"), List.of(id), "❌ Popup search");
        countPage.searchPopup("zzqq-no-such-child");
        Assert.assertEquals(countPage.getPopupEmptyMessage(), "No matching records found", "❌ Popup no-match message");
        Reporter.log("   Popup columns " + countPage.getPopupHeaders() + " — no phone / email shown (masking: QueryLogs)", true);
        Reporter.log("✅ TC_011 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_012 — A zero-count row is listed and its link opens an empty popup
    //  User, 2026-11-02 (Q-a): zero-count rows are displayed; the link opens with empty data.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "TC_012 — Zero-count row: link opens an empty popup")
    public void tc012_zeroCountRow() throws InterruptedException {
        Reporter.log("▶ TC_012 — Zero-count row", true);
        List<String[]> zero = rowsWithCount(0, 0, 1);
        Assert.assertFalse(zero.isEmpty(), "❌ No zero-count row");
        String[] row = zero.get(0);
        Assert.assertEquals(countPage.getRowLinks(row[0], row[1]).size(), 3, "❌ Zero-count row is not clickable");
        countPage.clickRowLink(row[0], row[1], 3);
        Reporter.log("   " + row[0] + " / " + row[1] + " = 0 → '" + countPage.getPopupHeader() + "' | rows " + countPage.getPopupRowCount()
                + " | " + countPage.getPopupEmptyMessage(), true);
        Assert.assertTrue(countPage.isPopupVisible(), "❌ Popup did not open");
        Assert.assertEquals(countPage.getPopupRowCount(), 0, "❌ Popup rows for a zero count");
        Assert.assertTrue(countPage.getPopupHeader().startsWith(row[1] + " · 0 admission(s)"), "❌ Popup header");
        Assert.assertEquals(countPage.getPopupEmptyMessage(), "No admissions found for this offer and company.", "❌ Empty-popup message");
        Reporter.log("✅ TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_014 — Popup Admission Date uses the module's format (dd Mon, yyyy). Fails until the Low bug
    //  Bug_AdmissionCount_DateFormat.md is fixed (popup shows yyyy-mm-dd).
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "TC_014 — Popup Admission Date format")
    public void tc014_popupDateFormat() throws InterruptedException {
        Reporter.log("▶ TC_014 — Popup date format", true);
        String[] row = rowsWithCount(1, 30, 1).get(0);
        countPage.clickRowLink(row[0], row[1], 3);
        List<String> dates = countPage.getPopupColumn("Admission Date");
        Reporter.log("   " + row[0] + " / " + row[1] + " → Admission Dates " + dates, true);
        Assert.assertFalse(dates.isEmpty(), "❌ No dates");
        for (String d : dates)
            Assert.assertTrue(d.matches("\\d{2} [A-Z][a-z]{2}, \\d{4}"), "❌ Admission Date '" + d + "' is not dd Mon, yyyy (Low bug)");
        Reporter.log("✅ TC_014 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_013 — User without View_Corporate_Admission_Count cannot open the screen (switches to Jaydeep Kar — runs last)
    //  User, 2026-11-02 (Q-f): use Jaydeep Kar.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 99, description = "TC_013 — User without the right cannot open the screen (Jaydeep Kar)")
    public void tc013_noRightUser() throws InterruptedException {
        Reporter.log("▶ TC_013 — No access without View_Corporate_Admission_Count (Jaydeep Kar)", true);
        navigations.goToUserRights();
        new UserRightsPage(driver).switchUser("Jaydeep Kar");
        Thread.sleep(3000);
        String inMenu = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; return m.some(function(x){return x.url==='tieup_admission_count';})+'';"));
        driver.get(utils.IAutoConstant.ADMISSION_COUNT_URL);
        Thread.sleep(6000);
        String body = String.valueOf(((JavascriptExecutor) driver).executeScript("return document.body.innerText;"));
        boolean table = Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("return !!document.getElementById('tieup_admission_count_table');"));
        String user = String.valueOf(((JavascriptExecutor) driver).executeScript("return (document.body.innerText.match(/Jaydeep Kar|Rakesh Singh/)||[''])[0];"));
        Reporter.log("   Logged in as " + user + " | in menu: " + inMenu + " | direct URL → table shown: " + table
                + " | message: " + (body.contains("not authorise") ? "Sorry! You are not authorise to access this screen." : "(none)"), true);
        Assert.assertEquals(user, "Jaydeep Kar", "❌ Switch to Jaydeep Kar failed");
        Assert.assertEquals(inMenu, "false", "❌ Corporate Admission Count is in Jaydeep's menu");
        Assert.assertFalse(table, "❌ Jaydeep can open the screen");
        Assert.assertTrue(body.contains("Sorry! You are not authorise to access this screen."), "❌ No 'not authorised' message");
        Reporter.log("✅ TC_013 PASSED", true);
    }
}
