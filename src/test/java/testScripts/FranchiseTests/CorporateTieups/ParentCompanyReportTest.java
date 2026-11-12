package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.ParentCompanyReportPage;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ═══════════════════════════════════════════════════════════════
// PARENT COMPANY REPORT (Franchise → Corporate Tieups → Parent Company Report, /existing_children_parent_company_report)
// New screen — no source test cases; written from the requirement the user shared (2026-11-02) + live screen →
// TC_Corporate_Tieups.xlsx, sheet TC_ParentCompanyReport (SC_015). Right View_Corporate_Admission_Count. Runs as Rakshita Singh (EP-14768, SME - Customer Support; user 2026-11-12, was Rakesh).
// Test data: on the test environment every parent company is "Dummy" (user). Read-only: nothing is saved.
// TC_010 (popup export), TC_011 (Counts tooltip), TC_012 (date format) assert the requirement and fail until the
// Low bugs are fixed.
// ═══════════════════════════════════════════════════════════════
public class ParentCompanyReportTest extends BaseTest {

    private static final String COMPANY = System.getProperty("parentCompany", "Dummy");
    private static final List<String> COLUMNS = Arrays.asList("Company Name", "Counts", "Action");
    private static final List<String> POPUP_COLUMNS = Arrays.asList("Child ID", "Child Name", "Father Name", "Father Company",
            "Mother Name", "Mother Company", "Center", "Program", "Admission Date", "Status");

    Navigations navigations;
    ParentCompanyReportPage reportPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();

        // ✅ switch to the screen's user from testData/input_UserRights.xlsx (Rakshita Singh, user 2026-11-12)
        String user = getUserForScreen("Parent Company Report");
        Assert.assertFalse(user.isEmpty(), "❌ No user found for Parent Company Report in Excel");
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
        navigations.goToParentCompanyReport();
        dismissAlertIfPresent();
        reportPage = new ParentCompanyReportPage(driver);
        Assert.assertTrue(reportPage.isPageLoaded(), "❌ Parent Company Report did not load");
    }

    private int countOf(String company) {
        for (String r : reportPage.getRows()) {
            String[] p = r.split(" \\|\\| ");
            if (p[0].equals(company)) return Integer.parseInt(p[1]);
        }
        return -1;
    }

    private void filter(String selectCss, String option) throws InterruptedException {
        reportPage.multiselectOnly(selectCss, option);
        reportPage.clickFilter();
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "TC_001 — Default view")
    public void tc001_defaultView() {
        Reporter.log("▶ TC_001 — Default view", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='existing_children_parent_company_report')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu: " + path + " | Heading: " + reportPage.getHeading() + " | City '" + reportPage.getMultiselectButtonText(ParentCompanyReportPage.CITY)
                + "' | Center '" + reportPage.getMultiselectButtonText(ParentCompanyReportPage.CENTER) + "' | " + reportPage.getInfoText(), true);
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Parent Company Report", "❌ Menu path");
        Assert.assertEquals(reportPage.getHeading(), "Existing Children Parent Company Report", "❌ Heading");
        Assert.assertEquals(reportPage.getMultiselectButtonText(ParentCompanyReportPage.CITY), "No Filters", "❌ City default");
        Assert.assertEquals(reportPage.getMultiselectButtonText(ParentCompanyReportPage.CENTER), "No Filters", "❌ Center default");
        Assert.assertTrue(reportPage.getMultiselectOptions(ParentCompanyReportPage.CITY).size() > 10, "❌ City options");
        Assert.assertTrue(reportPage.getMultiselectOptions(ParentCompanyReportPage.CENTER).size() > 100, "❌ Center options");
        Assert.assertTrue(reportPage.isFilterButtonVisible(), "❌ Filter button");
        Assert.assertEquals(reportPage.getColumnHeaders(), COLUMNS, "❌ Columns");
        Assert.assertEquals(reportPage.getDownloadButtonText(), "Download", "❌ Download button");
        Assert.assertEquals(reportPage.getPageLength(), 20, "❌ Rows per page");
        Assert.assertTrue(countOf(COMPANY) > 0, "❌ No '" + COMPANY + "' row");
        Reporter.log("✅ TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Access right
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "TC_002 — Access right View_Corporate_Admission_Count")
    public void tc002_accessRight() throws InterruptedException {
        Reporter.log("▶ TC_002 — Access right", true);
        boolean has = new UserRightsPage(driver).hasRight("357", "View_Corporate_Admission_Count");
        navigations.goToParentCompanyReport();
        reportPage = new ParentCompanyReportPage(driver);
        boolean opens = reportPage.isPageLoaded();
        Reporter.log("   Rakesh has View_Corporate_Admission_Count: " + has + " | screen opens: " + opens, true);
        Assert.assertTrue(has && opens, "❌ Access");
        Reporter.log("✅ TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003 — Counts = popup rows; only active children
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "TC_003 — Count matches the popup; only active children")
    public void tc003_countAndActiveOnly() throws InterruptedException {
        Reporter.log("▶ TC_003 — Count / active only", true);
        int count = countOf(COMPANY);
        String href = reportPage.getActionHref(COMPANY);
        reportPage.openChildren(COMPANY);
        int rows = reportPage.getPopupRowCount();
        Set<String> statuses = new HashSet<>(reportPage.getPopupColumn("Status"));
        List<String> ids = reportPage.getPopupColumn("Child ID");
        Reporter.log("   " + COMPANY + " = " + count + " | eye " + href + "\n   Popup '" + reportPage.getPopupTitle() + "' rows " + rows
                + " | distinct ids " + new HashSet<>(ids).size() + " | statuses " + statuses, true);
        Assert.assertTrue(href.contains("pop_existing_company_children") && href.endsWith("Active Children Details (" + COMPANY + ")"), "❌ Eye link / title");
        Assert.assertTrue(reportPage.getPopupTitle().contains("Active Children Details (" + COMPANY + ")"), "❌ Popup title");
        Assert.assertEquals(rows, count, "❌ Popup rows vs Counts");
        Assert.assertEquals(new HashSet<>(ids).size(), rows, "❌ Duplicate children in the popup");
        Assert.assertEquals(statuses, Set.of("Active"), "❌ Non-active children listed");
        Reporter.log("✅ TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Popup columns and parent company
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "TC_004 — Popup columns")
    public void tc004_popupColumns() throws InterruptedException {
        Reporter.log("▶ TC_004 — Popup columns", true);
        reportPage.openChildren(COMPANY);
        Reporter.log("   " + reportPage.getPopupHeaders() + " | per page " + reportPage.getPopupPageLength(), true);
        Assert.assertEquals(reportPage.getPopupHeaders(), POPUP_COLUMNS, "❌ Popup columns");
        Set<String> companies = new HashSet<>(reportPage.getPopupColumn("Father Company"));
        companies.addAll(reportPage.getPopupColumn("Mother Company"));
        Reporter.log("   Father / Mother companies: " + companies.stream().limit(5).toList(), true);
        Assert.assertTrue(companies.contains(COMPANY), "❌ " + COMPANY + " not among the parent companies");
        Reporter.log("✅ TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 / TC_006 — City and Center filters (count = popup rows; popup limited to the filter)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "TC_005 — City filter")
    public void tc005_cityFilter() throws InterruptedException {
        Reporter.log("▶ TC_005 — City filter", true);
        int all = countOf(COMPANY);
        String city = System.getProperty("parentCity", "Hyderabad");
        List<String> cityCenters = reportPage.getCentersOfCity(city);
        filter(ParentCompanyReportPage.CITY, city);
        int n = countOf(COMPANY);
        Assert.assertEquals(reportPage.getMultiselectSelected(ParentCompanyReportPage.CITY), List.of(city), "❌ City not kept");
        reportPage.openChildren(COMPANY);
        Set<String> centers = new HashSet<>(reportPage.getPopupColumn("Center"));
        Reporter.log("   " + city + ": " + COMPANY + " = " + n + " (all " + all + ") | popup rows " + reportPage.getPopupRowCount() + " | centers " + centers.size(), true);
        Assert.assertTrue(n > 0 && n < all, "❌ City count");
        Assert.assertEquals(reportPage.getPopupRowCount(), n, "❌ Popup rows vs city count");
        Assert.assertTrue(cityCenters.containsAll(centers), "❌ Popup lists centres outside " + city + ": " + centers);
        Reporter.log("✅ TC_005 PASSED", true);
    }

    @Test(priority = 6, description = "TC_006 — Center filter")
    public void tc006_centerFilter() throws InterruptedException {
        Reporter.log("▶ TC_006 — Center filter", true);
        String center = System.getProperty("parentCenter", "Koramangala");
        filter(ParentCompanyReportPage.CENTER, center);
        int n = countOf(COMPANY);
        Assert.assertEquals(reportPage.getMultiselectSelected(ParentCompanyReportPage.CENTER), List.of(center), "❌ Center not kept");
        reportPage.openChildren(COMPANY);
        Set<String> centers = new HashSet<>(reportPage.getPopupColumn("Center"));
        Reporter.log("   " + center + ": " + COMPANY + " = " + n + " | popup rows " + reportPage.getPopupRowCount() + " | centers " + centers, true);
        Assert.assertTrue(n > 0, "❌ No children at " + center);
        Assert.assertEquals(reportPage.getPopupRowCount(), n, "❌ Popup rows vs center count");
        Assert.assertEquals(centers, Set.of(center), "❌ Popup lists other centres");
        Reporter.log("✅ TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_007 — Demo Center (center 100) and test children are excluded
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "TC_007 — Demo Center excluded")
    public void tc007_demoCenterExcluded() throws InterruptedException {
        Reporter.log("▶ TC_007 — Demo Center excluded", true);
        filter(ParentCompanyReportPage.CENTER, "Demo Center");
        Reporter.log("   Demo Center → " + reportPage.getEmptyTableMessage() + " | " + reportPage.getInfoText(), true);
        Assert.assertEquals(reportPage.getEmptyTableMessage(), "No data available in table", "❌ Demo Center children listed");
        Reporter.log("✅ TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 — Search (table and popup)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "TC_008 — Search")
    public void tc008_search() throws InterruptedException {
        Reporter.log("▶ TC_008 — Search", true);
        Assert.assertEquals(reportPage.getSearchPlaceholder(), "", "❌ Unexpected placeholder");
        reportPage.searchTable(COMPANY);
        Assert.assertTrue(reportPage.getRows().get(0).startsWith(COMPANY + " || "), "❌ Table search");
        reportPage.searchTable("zzqq-no-such-company");
        Assert.assertEquals(reportPage.getEmptyTableMessage(), "No matching records found", "❌ Table no-match message");
        navigations.goToParentCompanyReport();
        reportPage = new ParentCompanyReportPage(driver);
        reportPage.isPageLoaded();
        reportPage.openChildren(COMPANY);
        Assert.assertTrue(reportPage.isPopupSearchVisible(), "❌ Popup search box");
        String id = reportPage.getPopupColumn("Child ID").get(0);
        reportPage.searchPopup(id);
        Reporter.log("   Popup search " + id + " → " + reportPage.getPopupInfo(), true);
        Assert.assertTrue(reportPage.getPopupColumn("Child ID").contains(id), "❌ Popup search");
        reportPage.searchPopup("zzqq-no-such-child");
        Assert.assertEquals(reportPage.getPopupEmptyMessage(), "No matching records found", "❌ Popup no-match message");
        Reporter.log("✅ TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_009 — Export (main table)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "TC_009 — Download")
    public void tc009_download() throws Exception {
        Reporter.log("▶ TC_009 — Download", true);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        reportPage.clickDownload();
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
        String header, line1;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            header = r.readLine().replace("﻿", "");
            line1 = r.readLine();
        }
        Reporter.log("   File: " + csv.getName() + " | " + header + " | " + line1, true);
        Assert.assertTrue(header.contains("Company Name") && header.contains("Counts"), "❌ CSV columns");
        Assert.assertTrue(line1 != null && line1.contains(COMPANY), "❌ CSV data");
        Reporter.log("✅ TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_010 — Popup export (requirement: "Supports search and export"; Low bug — no Download in the popup)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "TC_010 — Popup export")
    public void tc010_popupExport() throws InterruptedException {
        Reporter.log("▶ TC_010 — Popup export", true);
        reportPage.openChildren(COMPANY);
        boolean dl = reportPage.isPopupDownloadVisible();
        Reporter.log("   Popup Download button: " + dl, true);
        Assert.assertTrue(dl, "❌ No Download / export in the Active Children Details popup (Low bug)");
        Reporter.log("✅ TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_011 — Counts tooltip (requirement: "Counts (Tool tip Information)"; Low bug — none shown)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "TC_011 — Counts tooltip")
    public void tc011_countsTooltip() throws InterruptedException {
        Reporter.log("▶ TC_011 — Counts tooltip", true);
        String tip = reportPage.getCountsTooltip(COMPANY);
        Reporter.log("   Hover Counts of " + COMPANY + " → '" + tip + "'", true);
        Assert.assertFalse(tip.isEmpty(), "❌ No tooltip on Counts (Low bug)");
        Reporter.log("✅ TC_011 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_012 — Popup Admission Date format (dd Mon, yyyy; Low bug — shows yyyy-mm-dd)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "TC_012 — Popup Admission Date format")
    public void tc012_popupDateFormat() throws InterruptedException {
        Reporter.log("▶ TC_012 — Popup date format", true);
        reportPage.openChildren(COMPANY);
        List<String> dates = reportPage.getPopupColumn("Admission Date").subList(0, 5);
        Reporter.log("   Admission Dates " + dates, true);
        for (String d : dates)
            Assert.assertTrue(d.matches("\\d{2} [A-Z][a-z]{2}, \\d{4}"), "❌ Admission Date '" + d + "' is not dd Mon, yyyy (Low bug)");
        Reporter.log("✅ TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_013 — Popup pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "TC_013 — Popup pagination")
    public void tc013_popupPaging() throws InterruptedException {
        Reporter.log("▶ TC_013 — Popup pagination", true);
        reportPage.openChildren(COMPANY);
        String first = reportPage.getPopupInfo();
        reportPage.clickPopupNextPage();
        Reporter.log("   " + first + " → " + reportPage.getPopupInfo(), true);
        Assert.assertTrue(first.startsWith("Showing 1 to 10 of"), "❌ Popup page size");
        Assert.assertTrue(reportPage.getPopupInfo().startsWith("Showing 11 to 20 of"), "❌ Popup Next");
        Reporter.log("✅ TC_013 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_014 — User without the right (Jaydeep Kar, user 2026-11-02) — switches user, runs last
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 99, description = "TC_014 — User without the right cannot open the screen (Jaydeep Kar)")
    public void tc014_noRightUser() throws InterruptedException {
        Reporter.log("▶ TC_014 — No access (Jaydeep Kar)", true);
        navigations.goToUserRights();
        new UserRightsPage(driver).switchUser("Jaydeep Kar");
        Thread.sleep(3000);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String inMenu = String.valueOf(js.executeScript("var m=window.menuData||[]; return m.some(function(x){return x.url==='existing_children_parent_company_report';})+'';"));
        driver.get(utils.IAutoConstant.PARENT_COMPANY_REPORT_URL);
        Thread.sleep(6000);
        String body = String.valueOf(js.executeScript("return document.body.innerText;"));
        boolean table = Boolean.TRUE.equals(js.executeScript("return !!document.getElementById('parent_company_report_table');"));
        String user = String.valueOf(js.executeScript("return (document.body.innerText.match(/Jaydeep Kar|Rakesh Singh/)||[''])[0];"));
        Reporter.log("   Logged in as " + user + " | in menu: " + inMenu + " | table shown: " + table, true);
        Assert.assertEquals(user, "Jaydeep Kar", "❌ Switch failed");
        Assert.assertEquals(inMenu, "false", "❌ Parent Company Report in Jaydeep's menu");
        Assert.assertFalse(table, "❌ Jaydeep can open the screen");
        Assert.assertTrue(body.contains("Sorry! You are not authorise to access this screen."), "❌ No 'not authorised' message");
        Reporter.log("✅ TC_014 PASSED", true);
    }
}
