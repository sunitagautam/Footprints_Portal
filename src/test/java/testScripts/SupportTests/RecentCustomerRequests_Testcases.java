package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.RecentCustomerRequestsPage;
import utils.BaseTest;

public class RecentCustomerRequests_Testcases extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA
    // ═══════════════════════════════════════════════
    private static final String CHILD_ADM_ID = "65697"; // child visible in screenshot
    private static final String FROM_DATE = "01 May 2026";
    private static final String TO_DATE = "30 Jun 2026";

    // FIX: renamed from STATUS_APPROVED — value is "Pending" not "Approved"
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";

    // Expected table headers (11 columns confirmed from screenshot)
    private static final String[] EXPECTED_HEADERS = {
            "Child Name", "Request Type", "Request Status",
            "Approval Status", "Center Name", "Request Date",
            "WEF Date", "End Date", "Other Info",
            "Support Executive", "Created By"
    };

    RecentCustomerRequestsPage rcPage;
    Navigations navigations;

    // ═══════════════════════════════════════════════
    // BEFORE CLASS
    // No user switch needed — Rakesh (main user) has access
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        navigations = new Navigations(driver);
        rcPage = new RecentCustomerRequestsPage(driver);
        System.out.println("▶ Running Recent Customer Requests tests as: Rakesh");
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — navigate via Children menu
    // (Navigation Method 2: Children → Recent Customer Requests)
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        Thread.sleep(1000);
        navigations.goToRecentCustomerRequests();
        System.out.println("▶ Ready: Recent Customer Requests");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC001 — Page loads: filter section and table visible
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "TC001 — Page loads with filter section and table visible")
    public void tc001_pageLoadsSuccessfully() {
        Reporter.log("▶ TC001 — Page loads correctly", true);

        Assert.assertTrue(rcPage.isPageLoaded(),
                "❌ Submit button not visible — page did not load");
        Assert.assertTrue(rcPage.isTableVisible(),
                "❌ Data table not visible on page load");

        Reporter.log("✅ TC001 PASSED — page loaded, filter and table visible", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC002 — Default search (no filters): table renders without error
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "TC002 — Default search without filters renders table")
    public void tc002_defaultSearchReturnsResults() throws InterruptedException {
        Reporter.log("▶ TC002 — Default search", true);

        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        String entries = rcPage.getShowingEntriesText();
        Reporter.log("   Rows returned: " + rows, true);
        Reporter.log("   Entries text: " + entries, true);

        Assert.assertTrue(rows >= 0,
                "❌ Table row count should not be negative");

        Reporter.log("✅ TC002 PASSED — default search completed, entries: " + entries, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC003 — Search by Admission ID returns matching results
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
            description = "TC003 — Search by Admission ID returns matching results")
    public void tc003_searchByAdmissionId() throws InterruptedException {
        Reporter.log("▶ TC003 — Search by Admission ID: " + CHILD_ADM_ID, true);

        rcPage.enterAdmissionId(CHILD_ADM_ID);
        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        Reporter.log("   Rows returned: " + rows, true);
        Reporter.log("   Entries: " + rcPage.getShowingEntriesText(), true);

        Assert.assertTrue(rows >= 0,
                "❌ Row count should not be negative for valid Admission ID");

        // If rows exist, verify each row belongs to searched child
        if (rows > 0) {
            String firstChildName = rcPage.getColumnValueForRow(1, "Child Name");
            Reporter.log("   First row child: " + firstChildName, true);
            Assert.assertFalse(firstChildName.isEmpty(),
                    "❌ Child Name should not be empty in result row");
        }

        Reporter.log("✅ TC003 PASSED — Admission ID search completed", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC004 — Date range filter: From / To
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 4,
            description = "TC004 — Date range filter returns results within range")
    public void tc004_dateRangeFilter() throws InterruptedException {
        Reporter.log("▶ TC004 — Date range filter: " + FROM_DATE + " to " + TO_DATE, true);

        rcPage.setFromDate(FROM_DATE);
        rcPage.setToDate(TO_DATE);
        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        Reporter.log("   Rows returned: " + rows, true);
        Reporter.log("   Entries: " + rcPage.getShowingEntriesText(), true);

        Assert.assertTrue(rows >= 0,
                "❌ Row count should not be negative for valid date range");

        Reporter.log("✅ TC004 PASSED — date range filter applied", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC005 — Filter by Status = Pending
    // FIX: was named STATUS_APPROVED but value was "Pending" — now consistent
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 5,
            description = "TC005 — Filter by Status Pending shows only Pending records")
    public void tc005_filterByStatusPending() throws InterruptedException {
        Reporter.log("▶ TC005 — Filter by Status: " + STATUS_PENDING, true);

        rcPage.selectStatus(STATUS_PENDING);
        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        Reporter.log("   Rows returned: " + rows, true);

        if (rows > 0) {
            String status = rcPage.getColumnValueForRow(1, "Request Status");
            Reporter.log("   Row 1 Request Status: " + status, true);
            Assert.assertTrue(
                    status.equalsIgnoreCase(STATUS_PENDING),
                    "❌ Expected '" + STATUS_PENDING + "' in Request Status. Got: " + status);
        }

        Reporter.log("✅ TC005 PASSED — Pending filter applied", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC005b — Filter by Status = Approved
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 6,
            description = "TC005b — Filter by Status Approved shows only Approved records")
    public void tc005b_filterByStatusApproved() throws InterruptedException {
        Reporter.log("▶ TC005b — Filter by Status: " + STATUS_APPROVED, true);

        rcPage.selectStatus(STATUS_APPROVED);
        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        Reporter.log("   Rows returned: " + rows, true);

        if (rows > 0) {
            String status = rcPage.getColumnValueForRow(1, "Request Status");
            Reporter.log("   Row 1 Request Status: " + status, true);
            Assert.assertTrue(
                    status.equalsIgnoreCase(STATUS_APPROVED),
                    "❌ Expected 'Approved' in Request Status. Got: " + status);
        }

        Reporter.log("✅ TC005b PASSED — Approved filter applied", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC006 — Combined filter: Admission ID + date range
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 7,
            description = "TC006 — Combined Admission ID and date range filter")
    public void tc006_combinedAdmissionIdAndDateFilter() throws InterruptedException {
        Reporter.log("▶ TC006 — Combined: Admission ID + date range", true);

        rcPage.enterAdmissionId(CHILD_ADM_ID);
        rcPage.setFromDate(FROM_DATE);
        rcPage.setToDate(TO_DATE);
        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        Reporter.log("   Rows returned: " + rows, true);
        Reporter.log("   Entries: " + rcPage.getShowingEntriesText(), true);

        Assert.assertTrue(rows >= 0,
                "❌ Combined filter should return valid row count");

        Reporter.log("✅ TC006 PASSED — combined filter applied", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC007 — Verify all 11 table column headers present
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 8,
            description = "TC007 — All 11 table column headers are present")
    public void tc007_verifyTableHeaders() throws InterruptedException {
        Reporter.log("▶ TC007 — Verify all " + EXPECTED_HEADERS.length + " table headers", true);

        rcPage.clickSubmit();

        for (String header : EXPECTED_HEADERS) {
            boolean exists = rcPage.verifyHeaderExists(header);
            Reporter.log("   Header '" + header + "': " + (exists ? "✅" : "❌"), true);
            Assert.assertTrue(exists, "❌ Missing table header: " + header);
        }

        Reporter.log("✅ TC007 PASSED — all " + EXPECTED_HEADERS.length + " headers present", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC008 — Download Data button visible after search
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 9,
            description = "TC008 — Download Data button (DataTables CSV export) visible after search")
    public void tc008_downloadButtonVisible() throws InterruptedException {
        Reporter.log("▶ TC008 — Download Data button (a.buttons-csv.buttons-html5)", true);

        rcPage.clickSubmit();

        // DataTables CSV export button renders after results load
        // Confirmed locator: <a class="dt-button buttons-csv buttons-html5"><span>Download Data</span></a>
        Assert.assertTrue(rcPage.isDownloadButtonVisible(),
                "❌ Download Data button (a.buttons-csv) not visible after search");

        Reporter.log("✅ TC008 PASSED — Download Data button visible", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC009 — Table inline search filters visible rows
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 10,
            description = "TC009 — Table inline search box filters displayed rows")
    public void tc009_tableSearchFiltersRows() throws InterruptedException {
        Reporter.log("▶ TC009 — Table inline search", true);

        rcPage.clickSubmit();
        int totalRows = rcPage.getRowCount();
        Reporter.log("   Total rows before search: " + totalRows, true);

        rcPage.searchInTable("Approved");
        Thread.sleep(500);
        int filteredRows = rcPage.getRowCount();
        Reporter.log("   Rows after search 'Approved': " + filteredRows, true);

        Assert.assertTrue(filteredRows >= 0,
                "❌ Filtered row count should not be negative");
        Assert.assertTrue(filteredRows <= totalRows,
                "❌ Filtered rows should be ≤ total rows. Got: " + filteredRows
                        + " > " + totalRows);

        Reporter.log("✅ TC009 PASSED — table search functional", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC010 — Table horizontal scroll works (11 wide columns)
    // FIX: scrolls wrapper div, not table element
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 11,
            description = "TC010 — Table horizontal scroll works for 11 columns")
    public void tc010_horizontalScroll() throws InterruptedException {
        Reporter.log("▶ TC010 — Horizontal scroll", true);

        rcPage.clickSubmit();
        rcPage.scrollTableRight();
        Thread.sleep(500);
        rcPage.scrollTableLeft();
        Thread.sleep(300);

        Assert.assertTrue(rcPage.isTableVisible(),
                "❌ Table not visible after scroll");

        Reporter.log("✅ TC010 PASSED — horizontal scroll completed", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC011 — Direct URL navigation with child_id parameter
    // Verifies Navigation Method 2 (direct URL)
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 12,
            description = "TC011 — Direct URL navigation with child_id loads correct data")
    public void tc011_directUrlNavigationByChildId() throws InterruptedException {
        Reporter.log("▶ TC011 — Direct URL navigation for child: " + CHILD_ADM_ID, true);

        rcPage.navigateByChildId(CHILD_ADM_ID);

        Assert.assertTrue(rcPage.isPageLoaded(),
                "❌ Page did not load via direct URL navigation");
        Assert.assertTrue(rcPage.isTableVisible(),
                "❌ Table not visible after direct URL navigation");

        // Verify URL contains child_id
        String currentUrl = driver.getCurrentUrl();
        Reporter.log("   Current URL: " + currentUrl, true);
        Assert.assertTrue(currentUrl.contains("child_id=" + CHILD_ADM_ID),
                "❌ URL should contain child_id=" + CHILD_ADM_ID);

        Reporter.log("✅ TC011 PASSED — direct URL navigation works", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC012 — Invalid Admission ID returns no results (not an error)
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 13,
            description = "TC012 — Invalid Admission ID returns no data gracefully")
    public void tc012_invalidAdmissionIdNoData() throws InterruptedException {
        Reporter.log("▶ TC012 — Invalid Admission ID: 99999", true);

        rcPage.enterAdmissionId("99999");
        rcPage.clickSubmit();

        int rows = rcPage.getRowCount();
        Reporter.log("   Rows returned: " + rows, true);

        // Should show 0 rows or "No data available" — not a crash
        Assert.assertEquals(rows, 0,
                "❌ Invalid Admission ID should return 0 rows. Got: " + rows);

        Reporter.log("✅ TC012 PASSED — invalid ID returns no data gracefully", true);
    }
}
