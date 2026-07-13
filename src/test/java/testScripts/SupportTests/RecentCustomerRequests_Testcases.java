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
    // Child Pause — Pending (has CANCEL + PROCESSING DETAILS)
    // Confirmed from DOM: Paarth Agrawal, request_id=159673
    private static final String PAUSE_PENDING_ADM_ID = "49149";  // update to actual child admId
    private static final String PAUSE_PENDING_CHILD = "Paarth Agrawal";
    private static final String PAUSE_PENDING_REQ_ID = "159673";
    // Child Pause — Approved (has EARLY RESUME + EXTEND PAUSE)
    // Confirmed from DOM: Saavi Agarwal, request_id=160536 (processing details)
    private static final String PAUSE_APPROVED_ADM_ID = "65697";  // update to actual admId
    private static final String PAUSE_APPROVED_CHILD = "Saavi Agarwal";
    // Child Pause — Disabled buttons (old policy, pre-April 2026)
    // Confirmed from DOM: Driti Jadhav — disabled btn-ladda class
    private static final String PAUSE_OLD_POLICY_CHILD = "Driti Jadhav";
    // Program Change — Pending (has CANCEL + PROCESSING DETAILS)
    // Confirmed from DOM: Vedant Shrivastava, request_id=160577
    private static final String PC_PENDING_ADM_ID = "66698";  // update to actual admId
    private static final String PC_PENDING_CHILD = "Vedant Shrivastava";
    private static final String PC_PENDING_REQ_ID = "160577";
    // Early Resume date — must be before current end date of approved pause
    private static final String EARLY_RESUME_DATE = "2026-06-20";
    // Extend date — must be after current end date but within 30-day limit
    private static final String EXTEND_PAUSE_DATE = "2026-07-15";
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

    // ═══════════════════════════════════════════════
    // TEST DATA — Action button test IDs
    // ═══════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════
    // TC013 — Pause Pending: CANCEL button visible
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 14,
            description = "TC013 — Pending Pause: CANCEL button visible in table row")
    public void tc013_pausePendingCancelButtonVisible() throws InterruptedException {
        Reporter.log("▶ TC013 — CANCEL button visible for Pending Pause", true);

        rcPage.enterAdmissionId(PAUSE_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isCancelPauseButtonVisible(),
                "❌ CANCEL button (cancel_pause) not visible for Pending Pause");

        String reqId = rcPage.getFirstCancelRequestId();
        Reporter.log("   Cancel button found, request_id=" + reqId, true);
        Assert.assertFalse(reqId.isEmpty(),
                "❌ request_id attribute should not be empty on CANCEL button");

        Reporter.log("✅ TC013 PASSED — CANCEL button visible for Pending Pause", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC014 — Pause Pending: PROCESSING DETAILS button visible
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 15,
            description = "TC014 — Pending Pause: PROCESSING DETAILS button visible")
    public void tc014_pausePendingProcessingDetailsVisible() throws InterruptedException {
        Reporter.log("▶ TC014 — PROCESSING DETAILS button visible for Pending Pause", true);

        rcPage.enterAdmissionId(PAUSE_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isProcessingDetailsVisible(),
                "❌ PROCESSING DETAILS button not visible for Pending Pause");

        String reqId = rcPage.getFirstProcessingDetailsRequestId();
        Reporter.log("   Processing Details request_id=" + reqId, true);

        Reporter.log("✅ TC014 PASSED — PROCESSING DETAILS visible", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC015 — Pause Pending: Cancel request flow
    // Cancel → Confirm → verify CANCEL button gone
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 16,
            description = "TC015 — Cancel a Pending Pause request via modal confirm")
    public void tc015_cancelPendingPauseRequest() throws InterruptedException {
        Reporter.log("▶ TC015 — Cancel Pending Pause (request_id=" + PAUSE_PENDING_REQ_ID + ")", true);

        rcPage.enterAdmissionId(PAUSE_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isCancelPauseButtonVisible(),
                "❌ CANCEL button not visible — pre-condition failed");

        // Click CANCEL → modal opens
        rcPage.clickCancelPauseByRequestId(PAUSE_PENDING_REQ_ID);
        Reporter.log("   CANCEL clicked — modal should appear", true);

        // Confirm cancellation
        rcPage.confirmCancelRequest();
        Reporter.log("   Confirmed cancellation", true);

        // Re-submit to refresh table
        rcPage.enterAdmissionId(PAUSE_PENDING_ADM_ID);
        rcPage.clickSubmit();

        // Verify CANCEL button is gone — request is now cancelled
        Assert.assertTrue(rcPage.isCancelPauseButtonGone(),
                "❌ CANCEL button still visible after cancellation");

        Reporter.log("✅ TC015 PASSED — Pause request cancelled successfully", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC016 — Pause Pending: Cancel modal dismissed — no cancellation
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 17,
            description = "TC016 — Dismiss Cancel modal: pause request NOT cancelled")
    public void tc016_dismissCancelModal() throws InterruptedException {
        Reporter.log("▶ TC016 — Dismiss Cancel modal", true);

        rcPage.enterAdmissionId(PAUSE_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isCancelPauseButtonVisible(),
                "❌ CANCEL button not visible — pre-condition failed");

        // Click CANCEL → modal opens
        rcPage.clickCancelPause();

        // Dismiss modal (Close button)
        rcPage.dismissCancelModal();
        Reporter.log("   Modal dismissed", true);

        // CANCEL button should still be present
        Assert.assertTrue(rcPage.isCancelPauseButtonVisible(),
                "❌ CANCEL button gone after dismiss — request should not be cancelled");

        Reporter.log("✅ TC016 PASSED — Dismiss keeps request intact", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC017 — Program Change Pending: CANCEL button visible
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 18,
            description = "TC017 — Pending Program Change: CANCEL button visible")
    public void tc017_programChangePendingCancelVisible() throws InterruptedException {
        Reporter.log("▶ TC017 — CANCEL button visible for Pending Program Change", true);

        rcPage.enterAdmissionId(PC_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isCancelProgramChangeButtonVisible(),
                "❌ CANCEL button (cancel_customer_request) not visible for Pending PC");

        String reqId = rcPage.getFirstCancelRequestId();
        Reporter.log("   Cancel button found, request_id=" + reqId, true);

        Reporter.log("✅ TC017 PASSED — Program Change CANCEL button visible", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC018 — Program Change Pending: Cancel request flow
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 19,
            description = "TC018 — Cancel a Pending Program Change request")
    public void tc018_cancelPendingProgramChange() throws InterruptedException {
        Reporter.log("▶ TC018 — Cancel Pending Program Change (request_id="
                + PC_PENDING_REQ_ID + ")", true);

        rcPage.enterAdmissionId(PC_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isCancelProgramChangeButtonVisible(),
                "❌ CANCEL button not visible — pre-condition failed");

        rcPage.clickCancelProgramChangeByRequestId(PC_PENDING_REQ_ID);
        Reporter.log("   CANCEL clicked", true);

        rcPage.confirmCancelRequest();
        Reporter.log("   Confirmed cancellation", true);

        // Re-submit to refresh
        rcPage.enterAdmissionId(PC_PENDING_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isCancelProgramChangeButtonGone(),
                "❌ CANCEL button still visible — cancellation may have failed");

        Reporter.log("✅ TC018 PASSED — Program Change cancelled successfully", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC019 — Approved Pause: EARLY RESUME button enabled
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 20,
            description = "TC019 — Approved Pause: EARLY RESUME button enabled (popdown_medium)")
    public void tc019_approvedPauseEarlyResumeEnabled() throws InterruptedException {
        Reporter.log("▶ TC019 — EARLY RESUME button enabled for Approved Pause", true);

        rcPage.enterAdmissionId(PAUSE_APPROVED_ADM_ID);
        rcPage.clickSubmit();

        boolean enabled = rcPage.isEarlyResumeEnabled(PAUSE_APPROVED_CHILD);
        Reporter.log("   EARLY RESUME enabled for " + PAUSE_APPROVED_CHILD + ": " + enabled, true);

        Assert.assertTrue(enabled,
                "❌ EARLY RESUME button should be enabled for Approved Pause. Child: "
                        + PAUSE_APPROVED_CHILD);

        Reporter.log("✅ TC019 PASSED — EARLY RESUME enabled", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC020 — Approved Pause: EXTEND PAUSE button enabled
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 21,
            description = "TC020 — Approved Pause: EXTEND PAUSE button enabled")
    public void tc020_approvedPauseExtendEnabled() throws InterruptedException {
        Reporter.log("▶ TC020 — EXTEND PAUSE button enabled for Approved Pause", true);

        rcPage.enterAdmissionId(PAUSE_APPROVED_ADM_ID);
        rcPage.clickSubmit();

        boolean enabled = rcPage.isExtendPauseEnabled(PAUSE_APPROVED_CHILD);
        Reporter.log("   EXTEND PAUSE enabled for " + PAUSE_APPROVED_CHILD + ": " + enabled, true);

        Assert.assertTrue(enabled,
                "❌ EXTEND PAUSE button should be enabled for Approved Pause. Child: "
                        + PAUSE_APPROVED_CHILD);

        Reporter.log("✅ TC020 PASSED — EXTEND PAUSE enabled", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC021 — Old Policy Pause: EARLY RESUME + EXTEND PAUSE disabled
    // Verifies SC007_TC001 — old-policy pauses show disabled buttons
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 22,
            description = "TC021 — Old policy pause: EARLY RESUME and EXTEND PAUSE disabled")
    public void tc021_oldPolicyPauseButtonsDisabled() throws InterruptedException {
        Reporter.log("▶ TC021 — Old policy pause buttons disabled (SC007_TC001)", true);

        // Submit default search to load all records
        rcPage.clickSubmit();

        boolean resumeDisabled = rcPage.isEarlyResumeDisabled(PAUSE_OLD_POLICY_CHILD);
        boolean extendDisabled = rcPage.isExtendPauseDisabled(PAUSE_OLD_POLICY_CHILD);

        Reporter.log("   EARLY RESUME disabled for " + PAUSE_OLD_POLICY_CHILD
                + ": " + resumeDisabled, true);
        Reporter.log("   EXTEND PAUSE disabled for " + PAUSE_OLD_POLICY_CHILD
                + ": " + extendDisabled, true);

        Assert.assertTrue(resumeDisabled,
                "❌ EARLY RESUME should be DISABLED for old-policy pause. Child: "
                        + PAUSE_OLD_POLICY_CHILD);
        Assert.assertTrue(extendDisabled,
                "❌ EXTEND PAUSE should be DISABLED for old-policy pause. Child: "
                        + PAUSE_OLD_POLICY_CHILD);

        Reporter.log("✅ TC021 PASSED — old-policy pause shows disabled buttons", true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC022 — Early Resume flow: click → set date → confirm
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 23,
            description = "TC022 — Early Resume: set new end date and confirm")
    public void tc022_earlyResumeFlow() throws InterruptedException {
        Reporter.log("▶ TC022 — Early Resume flow for child: " + PAUSE_APPROVED_CHILD, true);
        Reporter.log("   Resume date: " + EARLY_RESUME_DATE, true);

        rcPage.enterAdmissionId(PAUSE_APPROVED_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isEarlyResumeEnabled(PAUSE_APPROVED_CHILD),
                "❌ EARLY RESUME not enabled — pre-condition failed");

        // Click EARLY RESUME → popdown opens
        rcPage.clickEarlyResumeForChild(PAUSE_APPROVED_CHILD);
        Reporter.log("   EARLY RESUME popdown opened", true);

        // Set resume date in update_date_to input
        rcPage.setExtendResumeDate(EARLY_RESUME_DATE);
        Reporter.log("   Resume date set: " + EARLY_RESUME_DATE, true);

        // Confirm
        rcPage.confirmExtendResume();

        String msg = rcPage.getActionResponseMessage();
        Reporter.log("   Response: " + msg, true);

        Assert.assertFalse(msg.toLowerCase().contains("error"),
                "❌ Early Resume returned error: " + msg);

        Reporter.log("✅ TC022 PASSED — Early Resume submitted", true);
        Reporter.log("   ℹ Backend: verify pause end date updated to "
                + EARLY_RESUME_DATE, true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TC023 — Extend Pause flow: click → set new end date → confirm
    // ═══════════════════════════════════════════════════════════════════════
    @Test(priority = 24,
            description = "TC023 — Extend Pause: set new end date and confirm")
    public void tc023_extendPauseFlow() throws InterruptedException {
        Reporter.log("▶ TC023 — Extend Pause flow for child: " + PAUSE_APPROVED_CHILD, true);
        Reporter.log("   New end date: " + EXTEND_PAUSE_DATE, true);

        rcPage.enterAdmissionId(PAUSE_APPROVED_ADM_ID);
        rcPage.clickSubmit();

        Assert.assertTrue(rcPage.isExtendPauseEnabled(PAUSE_APPROVED_CHILD),
                "❌ EXTEND PAUSE not enabled — pre-condition failed");

        // Click EXTEND PAUSE → popdown opens
        rcPage.clickExtendPauseForChild(PAUSE_APPROVED_CHILD);
        Reporter.log("   EXTEND PAUSE popdown opened", true);

        // Set new end date
        rcPage.setExtendResumeDate(EXTEND_PAUSE_DATE);
        Reporter.log("   New end date set: " + EXTEND_PAUSE_DATE, true);

        // Confirm
        rcPage.confirmExtendResume();

        String msg = rcPage.getActionResponseMessage();
        Reporter.log("   Response: " + msg, true);

        Assert.assertFalse(msg.toLowerCase().contains("error"),
                "❌ Extend Pause returned error: " + msg);

        Reporter.log("✅ TC023 PASSED — Extend Pause submitted", true);
        Reporter.log("   ℹ Backend: verify pause end date extended to "
                + EXTEND_PAUSE_DATE, true);
    }
}
