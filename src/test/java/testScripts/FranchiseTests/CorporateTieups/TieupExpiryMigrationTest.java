package testScripts.FranchiseTests.CorporateTieups;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.TieupExpiryMigrationPage;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// TIE-UP EXPIRY MIGRATION ADMISSIONS (Franchise → Corporate Tieups → Tie-up Expiry Migration Admissions, /tieup_expiry_migrations)
// Source: TC_Corporate Tie-ups.xlsx SC_009_TC_001–040 → TC_Corporate_Tieups.xlsx, sheet TC_TieupExpiryMigration. Runs as the Excel user for this screen (user 2026-12-07; was Rakesh).
// Read-only only (user, 2026-12-07): on test, sendExpiryReminder creates no expiry-migration rows, so the grid is empty and
// processTieupExpiryMigration answers "No data Found". The reminder → migration → restore flow (TC_007–040) is Not executed
// until dev explains what creates the rows. The checks below adapt to an empty grid and tighten when rows exist.
// ═══════════════════════════════════════════════════════════════
public class TieupExpiryMigrationTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Child Name", "Center", "Company", "Offer Title", "Offer Expiry Date",
            "Offer Status", "Migration Status", "Migration Date", "Renewal Expiry Date");
    private static final List<String> CSV_COLUMNS = Arrays.asList("Child ID", "Child Name", "Center", "Company", "Offer Title",
            "Offer Expiry Date", "Offer Status", "Migration Status", "Migration Date", "Renewal Expiry Date");
    private static final String COMPANY = System.getProperty("expiryCompany", "Tata Consultancy Services Limited(TCS)");

    Navigations navigations;
    TieupExpiryMigrationPage expiryPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();

        // ✅ switch to the screen's user from testData/input_UserRights.xlsx (user 2026-12-07: Rakshita Singh, or Shivali Gupta)
        String user = getUserForScreen("Tie-up Expiry Migration");
        Assert.assertFalse(user.isEmpty(), "❌ No user found for Tie-up Expiry Migration in Excel");
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
        reopen();
    }

    private void reopen() throws InterruptedException {
        navigations.goToTieupExpiryMigration();
        expiryPage = new TieupExpiryMigrationPage(driver);
        Assert.assertTrue(expiryPage.isPageLoaded(), "❌ Tie-up Expiry Migration Admissions did not load");
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_009_TC_001 — Default view")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_009_TC_001 — Default view", true);
        List<String> cols = expiryPage.getVisibleColumnHeaders();
        Reporter.log("   Heading: " + expiryPage.getHeading() + " | company " + expiryPage.getSelectedCompany() + " | columns " + cols
                + " | " + expiryPage.getInfoText() + " | per page " + expiryPage.getPageLength() + " | rows " + expiryPage.getRowCount(), true);
        Assert.assertEquals(expiryPage.getHeading(), "Tie-up Expiry Migration Admissions", "❌ Heading");
        Assert.assertTrue(expiryPage.isCompanyFilterVisible() && expiryPage.isFilterButtonVisible() && expiryPage.isClearLinkVisible(), "❌ Filters");
        Assert.assertEquals(expiryPage.getSelectedCompany(), "--All Companies--", "❌ Default company");
        Assert.assertTrue(expiryPage.getCompanyOptions().size() > 1, "❌ Company list is empty");
        Assert.assertTrue(expiryPage.isSearchVisible() && expiryPage.isDownloadVisible() && expiryPage.isPaginationVisible(), "❌ Search / Download / pagination");
        Assert.assertEquals(expiryPage.getSearchPlaceholder(), "Filter these entries", "❌ Search placeholder");
        Assert.assertEquals(cols, COLUMNS, "❌ Columns");
        Reporter.log("✅ SC_009_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Company filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_009_TC_002 — Company filter")
    public void tc002_companyFilter() throws InterruptedException {
        Reporter.log("▶ SC_009_TC_002 — Company filter (" + COMPANY + ")", true);
        int all = expiryPage.getRowCount();
        expiryPage.selectCompany(COMPANY);
        expiryPage.clickFilter();
        List<String> companies = expiryPage.getColumnValues("Company");
        Reporter.log("   All " + all + " rows | '" + COMPANY + "': " + companies.size() + " rows | " + expiryPage.getEmptyTableMessage(), true);
        Assert.assertEquals(expiryPage.getSelectedCompany(), COMPANY, "❌ Company not kept");
        for (String c : companies) Assert.assertEquals(c, COMPANY, "❌ Row of another company");
        if (companies.isEmpty())
            Assert.assertEquals(expiryPage.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        Reporter.log("✅ SC_009_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_009_TC_003 — Search")
    public void tc003_search() throws InterruptedException {
        Reporter.log("▶ SC_009_TC_003 — Search", true);
        int rows = expiryPage.getRowCount();
        if (rows > 0) {
            String name = expiryPage.getColumnValues("Child Name").get(0);
            expiryPage.searchTable(name);
            List<String> names = expiryPage.getColumnValues("Child Name");
            Reporter.log("   '" + name + "' → " + names, true);
            for (String n : names) Assert.assertTrue(n.contains(name), "❌ Unrelated row: " + n);
            expiryPage.searchTable("zzqq-no-such-child");
            Assert.assertEquals(expiryPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        } else {
            // with no data DataTables keeps "No data available in table" for any search
            expiryPage.searchTable("zzqq-no-such-child");
            Reporter.log("   Grid is empty — search shows: " + expiryPage.getEmptyTableMessage(), true);
            Assert.assertEquals(expiryPage.getRowCount(), 0, "❌ Rows appeared");
            Assert.assertFalse(expiryPage.getEmptyTableMessage().isEmpty(), "❌ No empty-table message");
        }
        Reporter.log("✅ SC_009_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Download
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_009_TC_004 — Download")
    public void tc004_download() throws Exception {
        Reporter.log("▶ SC_009_TC_004 — Download", true);
        int expected = expiryPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        expiryPage.clickDownload();
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
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            String l;
            while ((l = r.readLine()) != null) if (!l.replace("﻿", "").trim().isEmpty()) lines.add(l.replace("﻿", ""));
        }
        List<String> header = new ArrayList<>();
        for (String h : lines.get(0).split(",")) header.add(h.replace("\"", "").trim());
        Reporter.log("   File: " + csv.getName() + " | header " + header + " | rows " + (lines.size() - 1) + " (grid " + expected + ")", true);
        Assert.assertTrue(csv.getName().startsWith("Tieup_Expiry_Migration_Records"), "❌ File name " + csv.getName());
        Assert.assertEquals(header, CSV_COLUMNS, "❌ CSV columns");
        Assert.assertEquals(lines.size() - 1, expected, "❌ CSV rows vs grid");
        Reporter.log("✅ SC_009_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_009_TC_005 — Pagination")
    public void tc005_pagination() {
        Reporter.log("▶ SC_009_TC_005 — Pagination", true);
        int rows = expiryPage.getRowCount(), len = expiryPage.getPageLength();
        Reporter.log("   " + expiryPage.getInfoText() + " | per page " + len + " | options " + expiryPage.getPageLengthOptions(), true);
        Assert.assertEquals(len, 20, "❌ Rows per page");
        Assert.assertTrue(expiryPage.isPageLengthVisible(), "❌ Rows-per-page dropdown");
        Assert.assertEquals(expiryPage.getPageLengthOptions(), Arrays.asList("10", "20", "50", "100"), "❌ Rows-per-page options");
        Assert.assertFalse(expiryPage.isPreviousPageEnabled(), "❌ Previous enabled on page 1");
        Assert.assertEquals(expiryPage.isNextPageEnabled(), rows > len, "❌ Next state for " + rows + " rows");
        Reporter.log("✅ SC_009_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_006 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_009_TC_006 — Sorting")
    public void tc006_sorting() throws InterruptedException {
        Reporter.log("▶ SC_009_TC_006 — Sorting", true);
        Reporter.log("   Default order " + expiryPage.getOrder(), true);
        Assert.assertTrue(expiryPage.getHeaderSortClass("Offer Title").contains("sorting_desc"), "❌ Default order is not Offer Title descending");
        for (String c : COLUMNS) {
            expiryPage.clickColumnHeader(c);
            Assert.assertTrue(expiryPage.getHeaderSortClass(c).matches(".*sorting_(asc|desc).*"), "❌ '" + c + "' did not sort");
        }
        if (expiryPage.getRowCount() > 1) {
            expiryPage.clickColumnHeader("Child Name");
            List<String> names = expiryPage.getColumnValues("Child Name");
            List<String> sorted = new ArrayList<>(names);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            List<String> rev = new ArrayList<>(sorted);
            java.util.Collections.reverse(rev);
            Assert.assertTrue(names.equals(sorted) || names.equals(rev), "❌ Child Name not sorted: " + names);
        } else Reporter.log("   Grid has " + expiryPage.getRowCount() + " row(s) — row order not checked", true);
        Reporter.log("✅ SC_009_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_041 (new) — Clear link
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 41, description = "SC_009_TC_041 — Clear resets the filter")
    public void tc041_clear() throws InterruptedException {
        Reporter.log("▶ SC_009_TC_041 — Clear", true);
        expiryPage.selectCompany(COMPANY);
        expiryPage.clickFilter();
        Assert.assertEquals(expiryPage.getSelectedCompany(), COMPANY, "❌ Company not kept");
        expiryPage.clickClear();
        Reporter.log("   After Clear: " + expiryPage.getSelectedCompany() + " | " + driver.getCurrentUrl(), true);
        Assert.assertEquals(expiryPage.getSelectedCompany(), "--All Companies--", "❌ Clear did not reset the company");
        Reporter.log("✅ SC_009_TC_041 PASSED", true);
    }
}
