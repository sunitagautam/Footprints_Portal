package testScripts.SettingsTests.SystemLogs;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.SystemLogs.DailyObserverDataPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// DAILY OBSERVER DATA (Settings → System Logs → Daily Observer Data)
// Screen: /daily_observer_data. Explored live 2026-10-21 (no test
// sheet supplied — scenarios derived from the live screen, see
// CLAUDE.md "Requirements — Daily Observer Data").
// Stays on the default Rakesh login (no user switch).
//
// The test adds its OWN observer (TC_018, "QA Auto Observer <n>",
// SELECT 1 AS x, Schedule Active = No so the daily cron skips it),
// then updates it (TC_019: Category Alert, Frequency Weekly, still Active)
// and executes it (TC_020).
//
// Run Billing / Non-Billing Cron run EVERY active observer and email
// the results — TC_022 runs the Non-Billing cron only when
// -DrunObserverCron=true; by default it only checks the confirm.
// ═══════════════════════════════════════════════════════════════
public class DailyObserverDataTest extends BaseTest {

    private static final String QUERY = "SELECT accept,COUNT(*) FROM billing_policy_acceptance GROUP BY accept;";
    private static final String QUERY_SEARCH_TEXT = "wp_pods_admission_forms";

    private static final List<String> COLUMNS = Arrays.asList("Observer Type", "Query", "Data Type", "Category",
            "Frequency", "Status", "Created", "Last Executed", "Next Execution", "Exec Status", "Action");
    private static final List<String> FREQUENCIES = Arrays.asList("All", "Daily", "Alternate", "Mon–Fri", "Weekly",
            "Monthly", "Quarterly", "Random", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
            "Saturday", "Custom Date", "Custom Range");

    // ✅ Set by TC_018, used by TC_019 / TC_020
    //    (or supplied: -DobserverId=<id> -DobserverType="<type>" to reuse an existing own record)
    private static String newObserverType = System.getProperty("observerType", "");
    private static String newObserverId = System.getProperty("observerId", "");

    Navigations navigations;
    DailyObserverDataPage observerPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        observerPage = new DailyObserverDataPage(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToDailyObserverData();
        observerPage = new DailyObserverDataPage(driver);
        Assert.assertTrue(observerPage.isPageLoaded(), "❌ Daily Observer Data screen did not load");
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Menu: Settings → System Logs → Daily Observer Data
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "TC_001 — Menu link Settings → System Logs → Daily Observer Data")
    public void tc001_menuLink() {
        Reporter.log("▶ TC_001 — Menu link", true);
        List<WebElement> links = driver.findElements(By.xpath(
                "//a[@href='daily_observer_data' and normalize-space(.)='Daily Observer Data']"));
        Assert.assertFalse(links.isEmpty(), "❌ 'Daily Observer Data' menu link not found");
        String subMenu = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='daily_observer_data')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu path: " + subMenu, true);
        Assert.assertEquals(subMenu, "Settings > System Logs > Daily Observer Data", "❌ Menu path");
        Reporter.log("✅ TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "TC_002 — Default view of Daily Observer Data")
    public void tc002_defaultView() {
        Reporter.log("▶ TC_002 — Default view", true);
        Assert.assertTrue(observerPage.isHeadingVisible(), "❌ Heading 'Daily Observer Data'");
        Assert.assertTrue(observerPage.isAddObserverVisible(), "❌ Add Observer Data button");
        Assert.assertTrue(observerPage.isRunBillingCronVisible(), "❌ Run Billing Cron button");
        Assert.assertTrue(observerPage.isRunNonBillingCronVisible(), "❌ Run Non-Billing Cron button");
        Assert.assertTrue(observerPage.isObserverTypeFilterVisible(), "❌ Observer Type filter");
        Assert.assertTrue(observerPage.isQuerySearchFilterVisible(), "❌ Query Search filter");
        Assert.assertEquals(observerPage.getDataTypeOptions(), Arrays.asList("All", "Billing", "Non-Billing"), "❌ Data Type options");
        Assert.assertEquals(observerPage.getCategoryOptions(), Arrays.asList("All", "Alert", "Log"), "❌ Category options");
        Assert.assertEquals(observerPage.getFrequencyOptions(), FREQUENCIES, "❌ Frequency options");
        Assert.assertEquals(observerPage.getStatusOptions(), Arrays.asList("All", "Active", "Inactive"), "❌ Status options");
        Assert.assertEquals(observerPage.getExecStatusOptions(), Arrays.asList("All", "Success", "Failed"), "❌ Exec Status options");
        Assert.assertTrue(observerPage.isFilterButtonVisible(), "❌ Filter button");
        Assert.assertTrue(observerPage.isSearchBarVisible(), "❌ Search bar");
        Assert.assertTrue(observerPage.isDownloadVisible(), "❌ Download button");
        Assert.assertTrue(observerPage.isPaginationVisible(), "❌ Pagination");
        Assert.assertEquals(observerPage.getDefinedColumnHeaders(), COLUMNS, "❌ Table columns");
        Reporter.log("   " + observerPage.getInfoText(), true);
        Assert.assertTrue(observerPage.getInfoText().matches(".*Showing 1 to \\d+ of \\d+ entries.*"), "❌ Info text");
        Reporter.log("✅ TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003–TC_007 — Dropdown filters: every row matches the filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "TC_003 — Filter by Data Type = Billing")
    public void tc003_filterDataType() throws InterruptedException {
        Reporter.log("▶ TC_003 — Filter Data Type = Billing", true);
        observerPage.selectDataType("Billing");
        observerPage.clickFilter();
        assertColumnAll("Data Type", "Billing");
        Assert.assertEquals(observerPage.getSelectedDataType(), "Billing", "❌ Filter value not kept after Filter");
        Reporter.log("✅ TC_003 PASSED", true);
    }

    @Test(priority = 4, description = "TC_004 — Filter by Category = Alert")
    public void tc004_filterCategory() throws InterruptedException {
        Reporter.log("▶ TC_004 — Filter Category = Alert", true);
        observerPage.selectCategory("Alert");
        observerPage.clickFilter();
        assertColumnAll("Category", "Alert");
        Reporter.log("✅ TC_004 PASSED", true);
    }

    @Test(priority = 5, description = "TC_005 — Filter by Frequency = Daily")
    public void tc005_filterFrequency() throws InterruptedException {
        Reporter.log("▶ TC_005 — Filter Frequency = Daily", true);
        observerPage.selectFrequency("Daily");
        observerPage.clickFilter();
        assertColumnAll("Frequency", "Daily");
        Reporter.log("✅ TC_005 PASSED", true);
    }

    @Test(priority = 6, description = "TC_006 — Filter by Status = Inactive")
    public void tc006_filterStatus() throws InterruptedException {
        Reporter.log("▶ TC_006 — Filter Status = Inactive", true);
        observerPage.selectStatus("Inactive");
        observerPage.clickFilter();
        assertColumnAll("Status", "Inactive");
        Reporter.log("✅ TC_006 PASSED", true);
    }

    @Test(priority = 7, description = "TC_007 — Filter by Exec Status = Success")
    public void tc007_filterExecStatus() throws InterruptedException {
        Reporter.log("▶ TC_007 — Filter Exec Status = Success", true);
        observerPage.selectExecStatus("Success");
        observerPage.clickFilter();
        assertColumnAll("Exec Status", "Success");
        Reporter.log("✅ TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 — Observer Type text filter (value taken from the live grid)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "TC_008 — Filter by Observer Type")
    public void tc008_filterObserverType() throws InterruptedException {
        Reporter.log("▶ TC_008 — Filter Observer Type", true);
        String type = observerPage.getColumnValues("Observer Type").get(0);
        Reporter.log("   Observer Type: " + type, true);
        observerPage.enterObserverTypeFilter(type);
        observerPage.clickFilter();
        List<String> types = observerPage.getColumnValues("Observer Type");
        Reporter.log("   " + observerPage.getInfoText(), true);
        Assert.assertFalse(types.isEmpty(), "❌ No rows for Observer Type '" + type + "'");
        for (String t : types)
            Assert.assertTrue(t.toLowerCase().contains(type.toLowerCase()), "❌ Row not matching: " + t);
        Assert.assertEquals(observerPage.getObserverTypeFilterValue(), type, "❌ Filter value not kept after Filter");
        Reporter.log("✅ TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_009 — Query Search text filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "TC_009 — Filter by Query Search")
    public void tc009_filterQuerySearch() throws InterruptedException {
        Reporter.log("▶ TC_009 — Filter Query Search = " + QUERY_SEARCH_TEXT, true);
        observerPage.enterQuerySearchFilter(QUERY_SEARCH_TEXT);
        observerPage.clickFilter();
        List<String> queries = observerPage.getColumnValues("Query");
        Reporter.log("   " + observerPage.getInfoText(), true);
        Assert.assertFalse(queries.isEmpty(), "❌ No rows for Query Search");
        for (String q : queries)
            Assert.assertTrue(q.toLowerCase().contains(QUERY_SEARCH_TEXT), "❌ Row not matching: " + q);
        Reporter.log("✅ TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_010 — Filter with no matching data
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "TC_010 — Filter with no matching data")
    public void tc010_filterNoData() throws InterruptedException {
        Reporter.log("▶ TC_010 — Filter with no matching data", true);
        observerPage.enterObserverTypeFilter("zzQA_no_such_observer_" + System.currentTimeMillis());
        observerPage.clickFilter();
        Reporter.log("   Empty message: " + observerPage.getEmptyTableMessage(), true);
        Assert.assertTrue(observerPage.isTableEmpty(), "❌ Table should be empty");
        Assert.assertEquals(observerPage.getTotalEntries(), 0, "❌ Entries should be 0");
        Reporter.log("✅ TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_011 — Search box (match + no match)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "TC_011 — Search box")
    public void tc011_search() throws InterruptedException {
        Reporter.log("▶ TC_011 — Search", true);
        String type = observerPage.getColumnValues("Observer Type").get(0);
        observerPage.searchTable(type);
        List<String> types = observerPage.getColumnValues("Observer Type");
        Assert.assertFalse(types.isEmpty(), "❌ No rows for search '" + type + "'");
        Assert.assertTrue(types.contains(type), "❌ Searched row missing: " + type);
        observerPage.searchTable("zzQA_no_such_observer");
        Reporter.log("   No-match message: " + observerPage.getEmptyTableMessage(), true);
        Assert.assertEquals(observerPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        observerPage.clearSearch();
        Reporter.log("✅ TC_011 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_012 — Pagination (20 per page)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "TC_012 — Pagination, 20 records per page")
    public void tc012_pagination() throws InterruptedException {
        Reporter.log("▶ TC_012 — Pagination", true);
        int total = observerPage.getTotalEntries();
        Reporter.log("   Total entries: " + total, true);
        if (total <= 20) throw new SkipException("Only " + total + " entries — pagination not testable");
        Assert.assertEquals(observerPage.getVisibleRowCount(), 20, "❌ Page 1 should show 20 rows");
        Assert.assertFalse(observerPage.isPreviousPageEnabled(), "❌ Previous should be disabled on page 1");
        observerPage.clickNextPage();
        Reporter.log("   " + observerPage.getInfoText(), true);
        Assert.assertTrue(observerPage.getInfoText().contains("Showing 21 to"), "❌ Page 2 info");
        Assert.assertTrue(observerPage.isPreviousPageEnabled(), "❌ Previous should be enabled on page 2");
        observerPage.clickPreviousPage();
        Assert.assertTrue(observerPage.getInfoText().contains("Showing 1 to 20"), "❌ Back to page 1");
        Reporter.log("✅ TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_013 — Sorting (Observer Type asc/desc)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "TC_013 — Column sorting")
    public void tc013_sorting() throws InterruptedException {
        Reporter.log("▶ TC_013 — Sorting by Observer Type", true);
        observerPage.clickColumnHeader("Observer Type");
        Assert.assertTrue(observerPage.getHeaderSortClass("Observer Type").contains("sorting_asc"), "❌ Not ascending");
        assertSorted(observerPage.getColumnValues("Observer Type"), true);
        observerPage.clickColumnHeader("Observer Type");
        Assert.assertTrue(observerPage.getHeaderSortClass("Observer Type").contains("sorting_desc"), "❌ Not descending");
        assertSorted(observerPage.getColumnValues("Observer Type"), false);
        Reporter.log("✅ TC_013 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_014 — Download (client-side DataTables CSV)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "TC_014 — Download CSV")
    public void tc014_download() throws Exception {
        Reporter.log("▶ TC_014 — Download", true);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        observerPage.clickDownload();
        File csv = null;
        for (int i = 0; i < 20 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified()))
                        csv = f;
            if (csv == null) Thread.sleep(500);
        }
        Assert.assertNotNull(csv, "❌ No CSV downloaded to " + downloads);
        String header;
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(csv))) {
            header = reader.readLine();
            while (reader.readLine() != null) lines++;
        }
        Reporter.log("   File: " + csv.getName() + " | Header: " + header + " | data lines: " + lines, true);
        Assert.assertNotNull(header, "❌ CSV is empty");
        for (String col : Arrays.asList("Observer Type", "Query", "Data Type", "Category", "Frequency", "Status"))
            Assert.assertTrue(header.contains(col), "❌ CSV missing column: " + col);
        Reporter.log("✅ TC_014 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_015 — Add Observer Data modal: default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "TC_015 — Add Observer Data modal default view")
    public void tc015_addModalDefaultView() throws InterruptedException {
        Reporter.log("▶ TC_015 — Add modal default view", true);
        observerPage.clickAddObserver();
        Assert.assertEquals(observerPage.getModalTitle(), "Add Observer Data", "❌ Modal title");
        Assert.assertTrue(observerPage.isModalCloseIconVisible(), "❌ Close icon");
        for (String id : Arrays.asList("observer_type", "query_original", "observer_data_type", "data_category",
                "observer_occur_on_opt", "data_status", "observer_is_active", "btn_save_data"))
            Assert.assertTrue(observerPage.isModalFieldVisible(id), "❌ Field not visible: " + id);
        Assert.assertFalse(observerPage.isCustomScheduleVisible(), "❌ Custom Schedule should be hidden by default");
        Assert.assertEquals(observerPage.getQueryPlaceholderHint(),
                "Placeholders: __START_DATE__, __END_DATE__, __START_DATETIME__, __END_DATETIME__, __BILLING_DB__",
                "❌ Query placeholders hint");
        Assert.assertEquals(observerPage.getModalSelectedText("observer_data_type"), "Non-Billing", "❌ Default Data Type");
        Assert.assertEquals(observerPage.getModalSelectedText("data_category"), "Alert", "❌ Default Category");
        Assert.assertEquals(observerPage.getModalSelectedText("observer_occur_on_opt"), "Daily", "❌ Default Frequency");
        Assert.assertEquals(observerPage.getModalSelectedText("data_status"), "Active", "❌ Default Status");
        Assert.assertEquals(observerPage.getModalSelectedText("observer_is_active"), "Yes – include in cron run", "❌ Default Schedule Active");
        Assert.assertEquals(observerPage.getModalOptions("observer_is_active"),
                Arrays.asList("Yes – include in cron run", "No – skip even if Active"), "❌ Schedule Active options");
        Assert.assertEquals(observerPage.getModalOptions("observer_occur_on_opt").size(), 16, "❌ Frequency option count");
        Assert.assertEquals(observerPage.getModalSubmitLabel(), "Submit", "❌ Submit label");
        observerPage.closeModal();
        Reporter.log("✅ TC_015 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_016 — Add modal: mandatory-field validation
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "TC_016 — Add modal mandatory validation")
    public void tc016_addModalValidation() throws InterruptedException {
        Reporter.log("▶ TC_016 — Add modal validation", true);
        observerPage.clickAddObserver();
        observerPage.startAjaxCapture();
        observerPage.clickModalSubmit();
        Thread.sleep(800);
        List<String> errors = observerPage.getVisibleModalErrors();
        Reporter.log("   Errors: " + errors, true);
        Assert.assertTrue(errors.contains("observer_type => Observer type is required."), "❌ Observer type message");
        Assert.assertTrue(errors.contains("query_original => Query is required."), "❌ Query message");
        Assert.assertTrue(observerPage.getCapturedAjax().isEmpty(), "❌ Save request sent despite errors");
        observerPage.closeModal();
        Reporter.log("✅ TC_016 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_017 — Custom Date / Custom Range schedule
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 17, description = "TC_017 — Custom Date / Custom Range schedule field + JSON validation")
    public void tc017_customSchedule() throws InterruptedException {
        Reporter.log("▶ TC_017 — Custom schedule", true);
        observerPage.clickAddObserver();
        observerPage.enterModalObserverType("QA validation only");
        observerPage.enterModalQuery(QUERY);

        observerPage.selectModalFrequency("Custom_Date");
        Assert.assertTrue(observerPage.isCustomScheduleVisible(), "❌ Custom Schedule not shown for Custom Date");
        Assert.assertEquals(observerPage.getCustomScheduleHint(),
                "JSON format: {\"dates\":[\"2026-06-01\",\"2026-09-01\"]}", "❌ Custom Date hint");

        observerPage.selectModalFrequency("Custom_Range");
        Assert.assertEquals(observerPage.getCustomScheduleHint(),
                "JSON format: {\"start_date\":\"2026-06-01\",\"end_date\":\"2026-12-31\",\"interval_days\":7}",
                "❌ Custom Range hint");

        // Empty schedule → required
        observerPage.startAjaxCapture();
        observerPage.clickModalSubmit();
        Thread.sleep(800);
        List<String> errors = observerPage.getVisibleModalErrors();
        Reporter.log("   Empty schedule: " + errors, true);
        Assert.assertTrue(errors.contains("observer_custom_schedule => Custom schedule JSON is required."),
                "❌ Custom schedule required message");

        // Invalid JSON → message should carry the example for the chosen frequency
        observerPage.enterCustomSchedule("not json");
        observerPage.clickModalSubmit();
        Thread.sleep(800);
        errors = observerPage.getVisibleModalErrors();
        Reporter.log("   Invalid JSON: " + errors, true);
        String invalid = errors.stream().filter(e -> e.startsWith("observer_custom_schedule")).findFirst().orElse("");
        Assert.assertTrue(invalid.contains("Invalid JSON. Example:"), "❌ Invalid JSON message");
        Assert.assertTrue(observerPage.getCapturedAjax().isEmpty(), "❌ Save request sent despite invalid JSON");

        // Back to Daily → field hidden
        observerPage.selectModalFrequency("Daily");
        Assert.assertFalse(observerPage.isCustomScheduleVisible(), "❌ Custom Schedule should hide for Daily");

        // 🐛 Found 2026-10-21: the example is "undefined" — the message is built once,
        //    when the page loads with Frequency = Daily
        Assert.assertTrue(invalid.contains("{\"start_date\""),
                "❌ Invalid JSON message shows no usable example: '" + invalid + "'");
        observerPage.closeModal();
        Reporter.log("✅ TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_018 — Add a new observer (own record)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 18, description = "TC_018 — Add new Observer Data")
    public void tc018_addObserver() throws InterruptedException {
        newObserverType = "QA Auto Observer " + (System.currentTimeMillis() % 1000000);
        Reporter.log("▶ TC_018 — Add observer: " + newObserverType, true);
        observerPage.clickAddObserver();
        observerPage.startAjaxCapture();
        observerPage.enterModalObserverType(newObserverType);
        observerPage.enterModalQuery(QUERY);
        observerPage.selectModalDataType("non_billing");
        observerPage.selectModalCategory("Log");
        observerPage.selectModalFrequency("Daily");
        observerPage.selectModalStatus("Active");
        observerPage.selectModalScheduleActive("0");
        observerPage.clickModalSubmit();
        String msg = observerPage.waitForModalMessage(10);
        Reporter.log("   Message: '" + msg + "'", true);
        List<String> ajax = observerPage.getCapturedAjax();
        Reporter.log("   Save request: " + ajax, true);

        Thread.sleep(2500);
        observerPage.reload();
        observerPage.searchTable(newObserverType);
        newObserverId = observerPage.getObserverIdOfFirstRowMatching(newObserverType);
        Reporter.log("   New observer id: " + newObserverId + " | " + observerPage.getRowTextById(newObserverId), true);
        Assert.assertFalse(newObserverId.isEmpty(), "❌ New observer not in the grid");
        Assert.assertEquals(normalizeQuery(observerPage.getCellById(newObserverId, "Query")), normalizeQuery(QUERY), "❌ Query");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Data Type"), "Non-Billing", "❌ Data Type");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Category"), "Log", "❌ Category");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Frequency"), "Daily", "❌ Frequency");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Status"), "Active", "❌ Status");
        // ✅ Checked last so TC_019/TC_020 can still use the saved record
        Assert.assertEquals(msg, "New observer Query added successfully.", "❌ Add message — save request: " + ajax);
        Reporter.log("✅ TC_018 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_019 — Update modal pre-filled + update the own observer
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "TC_019 — Update Observer Data")
    public void tc019_updateObserver() throws InterruptedException {
        Reporter.log("▶ TC_019 — Update observer", true);
        String id = newObserverId;
        String type = newObserverType;
        if (id.isEmpty()) {
            // ✅ No own record (TC_018 failed) — check the pre-fill on the newest row only, no save
            id = driver.findElement(By.cssSelector("#DataTables_Table_0 tbody tr[role='row']"))
                    .getAttribute("class").replaceAll(".*tr-(\\d+).*", "$1");
            type = observerPage.getCellById(id, "Observer Type");
        } else {
            observerPage.searchTable(type);
        }
        String query = observerPage.getCellById(id, "Query");
        String status = observerPage.getCellById(id, "Status");
        observerPage.clickUpdate(id);
        Assert.assertEquals(observerPage.getModalTitle(), "Update Observer Data", "❌ Modal title");
        Assert.assertEquals(observerPage.getModalFieldValue("observerID"), id, "❌ Hidden observer id");
        Assert.assertEquals(observerPage.getModalFieldValue("observer_type").trim(), type, "❌ Observer Type pre-fill");
        Assert.assertEquals(normalizeQuery(observerPage.getModalFieldValue("query_original")), normalizeQuery(query), "❌ Query pre-fill");
        Assert.assertEquals(observerPage.getModalSelectedText("data_status"), status, "❌ Status pre-fill");
        Assert.assertEquals(observerPage.getModalSubmitLabel(), "Update", "❌ Button label");
        Reporter.log("   Pre-fill OK for #" + id, true);

        if (newObserverId.isEmpty()) {
            observerPage.closeModal();
            Assert.fail("❌ Update save not tested — TC_018 did not create an observer");
        }

        observerPage.startAjaxCapture();
        // ✅ Stays Active so TC_020 can execute it
        observerPage.selectModalStatus("Active");
        observerPage.selectModalCategory("Alert");
        observerPage.selectModalFrequency("Weekly");
        observerPage.clickModalSubmit();
        String msg = observerPage.waitForModalMessage(10);
        List<String> ajax = observerPage.getCapturedAjax();
        Reporter.log("   Message: '" + msg + "' | Save request: " + ajax, true);
        Thread.sleep(2500);
        observerPage.reload();
        observerPage.searchTable(newObserverType);
        Reporter.log("   Row: " + observerPage.getRowTextById(newObserverId), true);
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Status"), "Active", "❌ Status");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Category"), "Alert", "❌ Category not updated");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Frequency"), "Weekly", "❌ Frequency not updated");
        Assert.assertEquals(msg, "Observer Data updated successfully.", "❌ Update message — save request: " + ajax);
        Reporter.log("✅ TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_020 — Execute the own observer (SELECT 1 AS x)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "TC_020 — Execute an observer query")
    public void tc020_executeObserver() throws InterruptedException {
        Reporter.log("▶ TC_020 — Execute", true);
        if (newObserverId.isEmpty())
            Assert.fail("❌ Execute not tested — TC_018 did not create an observer");
        observerPage.searchTable(newObserverType);
        observerPage.clickExecute(newObserverId);
        String confirm = observerPage.acceptConfirm();
        Reporter.log("   Confirm: " + confirm, true);
        Assert.assertEquals(confirm, "Are you sure want to execute this Query?", "❌ Confirm text");
        String result = observerPage.waitForOverlayResult(30);
        Reporter.log("   Result: " + result, true);
        Thread.sleep(3000);
        observerPage.reload();
        observerPage.searchTable(newObserverType);
        Reporter.log("   Row: " + observerPage.getRowTextById(newObserverId), true);
        Assert.assertFalse(observerPage.getCellById(newObserverId, "Last Executed").isEmpty(), "❌ Last Executed not set");
        Assert.assertEquals(observerPage.getCellById(newObserverId, "Exec Status"), "Success", "❌ Exec Status");
        Assert.assertEquals(result, "Query was executed successfully.", "❌ Execute result message");
        Reporter.log("✅ TC_020 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_021 — Execute: Cancel on confirm does nothing
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 21, description = "TC_021 — Execute → Cancel on confirm")
    public void tc021_executeCancel() throws InterruptedException {
        Reporter.log("▶ TC_021 — Execute → Cancel", true);
        String id = driver.findElement(By.cssSelector("#DataTables_Table_0 tbody tr[role='row']"))
                .getAttribute("class").replaceAll(".*tr-(\\d+).*", "$1");
        String before = observerPage.getRowTextById(id);
        observerPage.startAjaxCapture();
        observerPage.clickExecute(id);
        String confirm = observerPage.dismissConfirm();
        Assert.assertEquals(confirm, "Are you sure want to execute this Query?", "❌ Confirm text");
        Thread.sleep(1500);
        Assert.assertTrue(observerPage.getCapturedAjax().isEmpty(), "❌ Request sent after Cancel");
        Assert.assertFalse(observerPage.isOverlayShown(), "❌ Overlay shown after Cancel");
        Assert.assertEquals(observerPage.getRowTextById(id), before, "❌ Row changed after Cancel");
        Reporter.log("✅ TC_021 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_022 — Run Billing / Non-Billing Cron
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 22, description = "TC_022 — Run Billing / Non-Billing Cron")
    public void tc022_runCron() throws InterruptedException {
        Reporter.log("▶ TC_022 — Run Cron", true);
        observerPage.startAjaxCapture();
        observerPage.clickRunBillingCron();
        Assert.assertEquals(observerPage.dismissConfirm(), "Run Billing Observer Cron now?", "❌ Billing confirm");
        observerPage.clickRunNonBillingCron();
        Assert.assertEquals(observerPage.dismissConfirm(), "Run Non-Billing Observer Cron now?", "❌ Non-Billing confirm");
        Thread.sleep(1000);
        Assert.assertTrue(observerPage.getCapturedAjax().isEmpty(), "❌ Cron request sent after Cancel");
        Reporter.log("   Confirm texts OK, Cancel sends nothing", true);

        if (!Boolean.parseBoolean(System.getProperty("runObserverCron", "false"))) {
            Reporter.log("   ℹ Cron run itself skipped (pass -DrunObserverCron=true to run the Non-Billing cron)", true);
            Reporter.log("✅ TC_022 PASSED", true);
            return;
        }
        observerPage.clickRunNonBillingCron();
        observerPage.acceptConfirm();
        String result = observerPage.waitForOverlayResult(120);
        Reporter.log("   Result: " + result, true);
        Assert.assertFalse(result.isEmpty(), "❌ No result message");
        Assert.assertFalse(result.startsWith("Request failed") || result.startsWith("Error"), "❌ Cron failed: " + result);
        Reporter.log("✅ TC_022 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_023 — Stale invalid JSON in the hidden Custom Schedule field
    //  must not silently block Submit after switching back to Daily
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 23, description = "TC_023 — Hidden Custom Schedule JSON does not silently block Submit")
    public void tc023_hiddenScheduleDoesNotBlockSubmit() throws InterruptedException {
        Reporter.log("▶ TC_023 — Hidden invalid JSON after switching back to Daily", true);
        // ✅ All mandatory fields filled; Inactive + Schedule Active = No, so a saved record never runs in the cron
        String type = "QA Auto Hidden Schedule " + (System.currentTimeMillis() % 1000000);
        observerPage.clickAddObserver();
        observerPage.enterModalObserverType(type);
        observerPage.enterModalQuery("SELECT 1");
        observerPage.selectModalDataType("non_billing");
        observerPage.selectModalCategory("Log");
        observerPage.selectModalStatus("Inactive");
        observerPage.selectModalScheduleActive("0");
        observerPage.selectModalFrequency("Custom_Date");
        observerPage.enterCustomSchedule("not json");
        observerPage.selectModalFrequency("Daily");
        Assert.assertFalse(observerPage.isCustomScheduleVisible(), "❌ Custom Schedule should hide for Daily");
        observerPage.startAjaxCapture();
        observerPage.clickModalSubmit();
        String msg = observerPage.waitForModalMessage(8);
        List<String> visible = observerPage.getVisibleModalErrors();
        List<String> ajax = observerPage.getCapturedAjax();
        Reporter.log("   Message: '" + msg + "' | Visible errors: " + visible + " | All errors: "
                + observerPage.getAllModalErrors() + " | Save request: " + ajax, true);
        // 🐛 Found 2026-10-21, re-confirmed 2026-10-22: no save request, no message, no visible error
        Assert.assertTrue(!ajax.isEmpty() || !visible.isEmpty(),
                "❌ Submit does nothing — hidden Custom Schedule still fails validation for Frequency = Daily and no error is visible");
        Reporter.log("✅ TC_023 PASSED", true);
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════
    // ✅ The grid adds a space after commas in the Query cell — compare queries without spacing differences
    private String normalizeQuery(String query) {
        return query.replaceAll("\\s*,\\s*", ",").replaceAll("\\s+", " ").trim();
    }

    private void assertColumnAll(String header, String expected) {
        List<String> values = observerPage.getColumnValues(header);
        Reporter.log("   " + observerPage.getInfoText() + " | " + header + " values: " + new java.util.TreeSet<>(values), true);
        Assert.assertFalse(values.isEmpty(), "❌ No rows for " + header + " = " + expected);
        for (String v : values)
            Assert.assertTrue(v.equalsIgnoreCase(expected), "❌ " + header + " row value '" + v + "' ≠ " + expected);
    }

    private void assertSorted(List<String> values, boolean asc) {
        List<String> sorted = new ArrayList<>(values);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        if (!asc) java.util.Collections.reverse(sorted);
        Reporter.log("   " + (asc ? "Asc" : "Desc") + ": " + values, true);
        Assert.assertEquals(values, sorted, "❌ Not sorted " + (asc ? "ascending" : "descending"));
    }
}
