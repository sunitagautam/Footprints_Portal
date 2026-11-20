package testScripts.SupportTests.FinancialUpdates;

import org.openqa.selenium.WindowType;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.FinancialUpdates.BackendUpdateRequestsPage;
import pages.Support.FinancialUpdates.UpdatePaymentsPage;
import utils.BaseTest;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

// ═══════════════════════════════════════════════════════════════
// BACKEND UPDATE REQUESTS (Support → Financial Updates → Backend Update Requests, /backend_update_requests)
// Source: "TC_Support_Financial_Updates_2026 (1).xlsx", sheet "TC_Backend Update Requests" (24 TCs), updated to the
// live screen (2026-11-20) + new TCs → TC_Support_Financial_Updates.xlsx, sheet TC_BackendUpdateRequests.
// Runs as Rakesh (no user switch).
//
// Saves (user, 2026-11-20 — "Update own request", "Void Invoice flow", "Billing Setup Update save"):
//  - SC003_TC_002 adds a High request (Others, Requested To = Rakesh Singh) for child -DbuChild (default 50920);
//  - SC005_TC_001 updates that request (priority Medium + Add Child History);
//  - SC005_TC_006 adds a Void Invoice request for the cheapest fully-due invoice with a Void icon on Update Payments
//    (override -DbuVoidChild / -DbuVoidInvoice) and voids it with the $ icon;
//  - SC005_TC_007 re-saves the child's current Pause Flag in Billing Setup Update (value unchanged).
// ═══════════════════════════════════════════════════════════════
public class BackendUpdateRequestsTest extends BaseTest {

    private static final List<String> SHOWN_COLUMNS = Arrays.asList("Child Name", "Request Type", "Requested Action", "Due Date",
            "Date of Request", "Requested By", "Requested To", "Center Information", "Priority", "Status", "Action");
    private static final List<String> TYPES = Arrays.asList("Apply Discount", "Child Record Update", "Credits Removal", "Invoice Update",
            "Joining Updates", "Operation Not Supported", "Others", "Program Change", "Query or Concern", "Reactivation",
            "Subscription Update", "Transfer");
    private static final List<String> STAFF = Arrays.asList("--Select--", "Aparna Jain", "Ashmita Kullu", "Ayesha Khan", "Bushra Firoz",
            "Jaydeep Kar", "Rakesh Singh", "Rakshita Singh", "Ranjna Bhalla", "Shalu Vadhwa", "Shefali Sharma", "Sneha Choudhary",
            "Varsha Jha", "Vikrant Gaur");
    private static final String PINK = "#ffe7e7";
    private static final String VOID_CONFIRM = "Are you sure you want to process this Void Invoice?";
    private static final String ICONS_6 = "Update Backend Requests, Account Statement, Billing Plans, Discounts, Freshdesk Ticket Link, Billing Setup Update";
    private static final String ICONS_7 = "Update Backend Requests, Account Statement, Billing Plans, Discounts, Freshdesk Ticket Link, Void Invoice, Billing Setup Update";

    Navigations navigations;
    BackendUpdateRequestsPage page;

    private final String childId = System.getProperty("buChild", "50920");
    private final String stamp = String.valueOf(System.currentTimeMillis() % 1000000);
    private final String action = "QA Auto backend request " + stamp;
    private final String ticket = "375022";
    private String requestId = "";
    private String voidChild = System.getProperty("buVoidChild", "");
    private String voidInvoice = System.getProperty("buVoidInvoice", "");

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws Exception {
        ensureBrowserAlive();
        dismissAlertIfPresent();
        reopen();
    }

    // Chrome sometimes drops the session during a reload — relaunch and log in again
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

    // Opens the screen with the default filters (the server keeps the last posted filter in the session)
    private void reopen() throws InterruptedException {
        navigations.goToBackendUpdateRequests();
        dismissAlertIfPresent();
        page = new BackendUpdateRequestsPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Backend Update Requests did not load");
        if (!page.getSelectText("requestto").equals("Rakesh Singh") || !page.getSelectText("requestby").equals("--Select--")
                || !page.getStatusSelected().equals(Arrays.asList("Pending", "Reopened")) || !page.getSelectText("type").equals("--Select--")
                || !page.getSelectText("priority").equals("--Select--") || !page.getChildFilter().isEmpty()) {
            page.clickReset();
        }
        // Reset does not clear Child Name/ Child ID (resetForm() skips #sr_child_name and the server keeps it) — clear it by hand
        if (!page.getChildFilter().isEmpty()) {
            page.setChildFilter("");
            page.clickSearch();
        }
    }

    // All requests (Requested To blank, every status)
    private void showAll() throws InterruptedException {
        page.setSelect("requestto", "--Select--");
        page.setStatuses(BackendUpdateRequestsPage.ALL_STATUSES);
        page.clickSearch();
    }

    private Map<String, String> findOwnRequest() throws InterruptedException {
        if (requestId.isEmpty()) {
            // Look the request up by its unique Requested Action text
            for (Map<String, String> r : page.getRows())
                if (r.get("Requested Action Full").contains(action) || r.get("Requested Action").contains(action)) {
                    requestId = r.get("Request id");
                    return r;
                }
            return null;
        }
        return page.getRowByRequestId(requestId);
    }

    private void needOwnRequest() throws InterruptedException {
        if (requestId.isEmpty()) throw new SkipException("SC003_TC_002 did not create a request in this run");
        page.showAllRows();
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC_001 — Default view: filters with defaults, Notes, Add button, table")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);
        Assert.assertEquals(page.getMenuPath(), "Support > Financial Updates > Backend Update Requests", "❌ Menu path");
        Assert.assertEquals(page.getHeading(), "Backend Update Requests", "❌ Heading");
        Assert.assertTrue(page.isAddButtonVisible(), "❌ Add Backend Requests button missing");
        Assert.assertEquals(page.getFilterLabels(), Arrays.asList("Child Name/ Child ID", "Requested To", "Requested By", "Status", "Type", "Priority"), "❌ Filter labels");
        Assert.assertEquals(page.getChildFilter(), "", "❌ Child filter default");
        Assert.assertEquals(page.getSelectText("requestto"), "Rakesh Singh", "❌ Requested To default (logged-in user)");
        Assert.assertEquals(page.getSelectText("requestby"), "--Select--", "❌ Requested By default");
        Assert.assertEquals(page.getStatusSelected(), Arrays.asList("Pending", "Reopened"), "❌ Status default");
        Assert.assertEquals(page.getStatusButtonText(), "Pending, Reopened", "❌ Status button text");
        Assert.assertEquals(page.getSelectText("type"), "--Select--", "❌ Type default");
        Assert.assertEquals(page.getSelectText("priority"), "--Select--", "❌ Priority default");
        Assert.assertTrue(page.isSearchButtonVisible() && page.isResetButtonVisible(), "❌ Search / Reset missing");
        String notes = page.getNotesText();
        Reporter.log("   Notes: " + notes, true);
        Assert.assertTrue(notes.contains("High Priority"), "❌ Notes legend missing: " + notes);
        Assert.assertEquals(page.getShownColumnHeaders(), SHOWN_COLUMNS, "❌ Columns");
        Assert.assertTrue(page.isSearchBoxVisible(), "❌ Search box missing");
        Assert.assertTrue(page.isPaginationVisible(), "❌ Pagination missing");
        Assert.assertEquals(page.getDownloadButtonText(), "Download Report", "❌ Download button");
        Assert.assertEquals(page.getPageLength(), 10, "❌ Rows per page");
        Reporter.log("   " + page.getInfoText() + " | " + page.getAllColumnHeaders(), true);
        for (Map<String, String> r : page.getRows()) {
            Assert.assertEquals(r.get("Requested To"), "Rakesh Singh", "❌ Default list shows another Requested To: " + r);
            Assert.assertTrue(Arrays.asList("Pending", "Reopened").contains(r.get("Status")), "❌ Default list status: " + r);
        }
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — Filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC002_TC_002 — Requested To / Requested By options and filter")
    public void sc002_tc002_requestedToFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_002 — Requested To", true);
        Assert.assertEquals(page.getSelectOptions("requestto"), STAFF, "❌ Requested To options");
        Assert.assertEquals(page.getSelectOptions("requestby"), STAFF, "❌ Requested By options");
        page.setSelect("requestto", "Jaydeep Kar");
        page.setStatuses(BackendUpdateRequestsPage.ALL_STATUSES);
        page.clickSearch();
        Assert.assertEquals(page.getSelectText("requestto"), "Jaydeep Kar", "❌ Requested To not kept");
        List<String> to = new ArrayList<>(new TreeSet<>(page.getRows().stream().map(r -> r.get("Requested To")).collect(java.util.stream.Collectors.toList())));
        Reporter.log("   Requested To = Jaydeep Kar → " + page.getRowCount() + " rows " + to, true);
        Assert.assertTrue(page.getRowCount() > 0, "❌ No rows for Jaydeep Kar");
        Assert.assertEquals(to, Collections.singletonList("Jaydeep Kar"), "❌ Other Requested To values");
        page.setSelect("requestto", "--Select--");
        page.setSelect("requestby", "Rakesh Singh");
        page.clickSearch();
        List<String> by = new ArrayList<>(new TreeSet<>(page.getRows().stream().map(r -> r.get("Requested By")).collect(java.util.stream.Collectors.toList())));
        Reporter.log("   Requested By = Rakesh Singh → " + page.getRowCount() + " rows " + by, true);
        Assert.assertTrue(by.isEmpty() || by.equals(Collections.singletonList("Rakesh Singh")), "❌ Requested By filter: " + by);
        page.clickReset();
        Assert.assertEquals(page.getSelectText("requestto"), "Rakesh Singh", "❌ Reset: Requested To");
        Assert.assertEquals(page.getSelectText("requestby"), "--Select--", "❌ Reset: Requested By");
        Reporter.log("✅ SC002_TC_002 PASSED", true);
    }

    @Test(priority = 3, description = "SC002_TC_003 — Status filter (multiselect)")
    public void sc002_tc003_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_003 — Status", true);
        Assert.assertEquals(page.getStatusOptions(), Arrays.asList(BackendUpdateRequestsPage.ALL_STATUSES), "❌ Status options");
        page.setSelect("requestto", "--Select--");
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String s : BackendUpdateRequestsPage.ALL_STATUSES) {
            page.setSelect("requestto", "--Select--");
            page.setStatuses(s);
            page.clickSearch();
            Set<String> got = new TreeSet<>(page.getRows().stream().map(r -> r.get("Status")).collect(java.util.stream.Collectors.toList()));
            counts.put(s, page.getRowCount());
            Assert.assertTrue(got.isEmpty() || got.equals(Collections.singleton(s)), "❌ Status " + s + " shows " + got);
        }
        showAll();
        int all = page.getRowCount();
        Reporter.log("   " + counts + " | all statuses " + all, true);
        Assert.assertEquals(all, counts.values().stream().mapToInt(Integer::intValue).sum(), "❌ All statuses ≠ sum of each status");
        Reporter.log("✅ SC002_TC_003 PASSED", true);
    }

    @Test(priority = 4, description = "SC002_TC_004 — Type filter (12 types; Void Invoice is not in the filter)")
    public void sc002_tc004_typeFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_004 — Type", true);
        List<String> opts = page.getSelectOptions("type");
        Assert.assertEquals(opts.subList(1, opts.size()), TYPES, "❌ Type options");
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String t : Arrays.asList("Invoice Update", "Child Record Update", "Program Change")) {
            page.setSelect("requestto", "--Select--");
            page.setStatuses(BackendUpdateRequestsPage.ALL_STATUSES);
            page.setSelect("type", t);
            page.clickSearch();
            Set<String> got = new TreeSet<>(page.getRows().stream().map(r -> r.get("Request Type")).collect(java.util.stream.Collectors.toList()));
            counts.put(t, page.getRowCount());
            Assert.assertTrue(got.isEmpty() || got.equals(Collections.singleton(t)), "❌ Type " + t + " shows " + got);
        }
        Reporter.log("   " + counts, true);
        page.clickReset();
        Assert.assertEquals(page.getSelectText("type"), "--Select--", "❌ Reset: Type");
        Reporter.log("✅ SC002_TC_004 PASSED", true);
    }

    @Test(priority = 5, description = "SC002_TC_005 — Priority filter")
    public void sc002_tc005_priorityFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_005 — Priority", true);
        Assert.assertEquals(page.getSelectOptions("priority"), Arrays.asList("--Select--", "High", "Low", "Medium"), "❌ Priority options");
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String p : Arrays.asList("High", "Medium", "Low")) {
            page.setSelect("requestto", "--Select--");
            page.setStatuses(BackendUpdateRequestsPage.ALL_STATUSES);
            page.setSelect("priority", p);
            page.clickSearch();
            List<Map<String, String>> rows = page.getRows();
            counts.put(p, rows.size());
            for (Map<String, String> r : rows) {
                Assert.assertEquals(r.get("Priority"), p, "❌ Priority " + p + ": " + r.get("Request id"));
                // SC004_TC_002: High rows pink, others not
                Assert.assertEquals(r.get("background").toLowerCase().contains(PINK), p.equals("High"), "❌ Row colour for " + p + ": " + r.get("Request id") + " " + r.get("background"));
            }
        }
        Reporter.log("   " + counts, true);
        Reporter.log("✅ SC002_TC_005 PASSED", true);
    }

    @Test(priority = 6, description = "SC002_TC_006 — Combined filters + Reset restores the defaults")
    public void sc002_tc006_combinedAndReset() throws InterruptedException {
        Reporter.log("▶ SC002_TC_006 — Combined + Reset", true);
        page.setSelect("requestto", "--Select--");
        page.setStatuses("Completed");
        page.setSelect("type", "Invoice Update");
        page.setSelect("priority", "High");
        page.clickSearch();
        for (Map<String, String> r : page.getRows()) {
            Assert.assertEquals(r.get("Status"), "Completed", "❌ Status");
            Assert.assertEquals(r.get("Request Type"), "Invoice Update", "❌ Type");
            Assert.assertEquals(r.get("Priority"), "High", "❌ Priority");
        }
        Reporter.log("   Completed + Invoice Update + High → " + page.getRowCount() + " rows", true);
        Assert.assertEquals(page.getStatusSelected(), Collections.singletonList("Completed"), "❌ Status not kept after Search");
        page.clickReset();
        Assert.assertEquals(page.getSelectText("requestto"), "Rakesh Singh", "❌ Reset: Requested To");
        Assert.assertEquals(page.getStatusSelected(), Arrays.asList("Pending", "Reopened"), "❌ Reset: Status");
        Assert.assertEquals(page.getSelectText("type"), "--Select--", "❌ Reset: Type");
        Assert.assertEquals(page.getSelectText("priority"), "--Select--", "❌ Reset: Priority");
        Reporter.log("✅ SC002_TC_006 PASSED", true);
    }

    @Test(priority = 7, description = "SC002_TC_001 — Child Name / Child ID filter")
    public void sc002_tc001_childFilter() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Child Name / Child ID", true);
        showAll();
        Map<String, String> sample = page.getRows().get(0);
        String name = sample.get("Child Name"), id = sample.get("Child Id");
        page.setChildFilter(name);
        page.clickSearch();
        Assert.assertTrue(page.getRowCount() > 0, "❌ No rows for " + name);
        List<String> names = page.getRows().stream().map(r -> r.get("Child Name")).collect(java.util.stream.Collectors.toList());
        List<String> other = names.stream().filter(n -> !n.toLowerCase().contains(name.toLowerCase())).distinct().collect(java.util.stream.Collectors.toList());
        Reporter.log("   '" + name + "' → " + names.size() + " rows; not matching the full name: " + other, true);
        page.setChildFilter(id);
        page.clickSearch();
        Set<String> ids = new TreeSet<>(page.getRows().stream().map(r -> r.get("Child Id")).collect(java.util.stream.Collectors.toList()));
        Reporter.log("   '" + id + "' → " + page.getRowCount() + " rows " + ids, true);
        Assert.assertEquals(ids, Collections.singleton(id), "❌ Child ID filter");
        Assert.assertEquals(page.getChildFilter(), id, "❌ Child filter not kept");
        Assert.assertTrue(other.isEmpty(), "❌ Name filter '" + name + "' also returned " + other + " (bug)");
        page.clickReset();
        String kept = page.getChildFilter();
        Reporter.log("   after Reset the child filter is '" + kept + "'", true);
        Assert.assertEquals(kept, "", "❌ Reset did not clear Child Name/ Child ID (bug)");
        Reporter.log("✅ SC002_TC_001 PASSED", true);
    }

    @Test(priority = 8, description = "SC008_TC_002 — Filter with no result: empty state, Reset restores")
    public void sc008_tc002_emptyState() throws InterruptedException {
        Reporter.log("▶ SC008_TC_002 — Empty state", true);
        page.setSelect("requestto", "--Select--");
        page.setStatuses(BackendUpdateRequestsPage.ALL_STATUSES);
        page.setChildFilter("QA No Such Child " + stamp);
        page.clickSearch();
        Assert.assertEquals(page.getRowCount(), 0, "❌ Rows for a non-existent child");
        Reporter.log("   " + page.getEmptyTableMessage() + " | " + page.getInfoText(), true);
        Assert.assertEquals(page.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 0 to 0 of 0"), "❌ Info: " + page.getInfoText());
        page.clickReset();
        String kept = page.getChildFilter();
        page.setChildFilter("");
        page.clickSearch();
        Reporter.log("   after Reset the child filter is '" + kept + "'; cleared by hand → " + page.getInfoText(), true);
        Assert.assertEquals(kept, "", "❌ Reset did not clear Child Name/ Child ID (bug)");
        Reporter.log("✅ SC008_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC006 / SC007 — Search box, download, pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC006_TC_002 — Search box")
    public void sc006_tc002_searchBox() throws InterruptedException {
        Reporter.log("▶ SC006_TC_002 — Search box", true);
        showAll();
        int all = page.getRowCount();
        String name = page.getRows().get(page.getRows().size() / 2).get("Child Name");
        page.searchTable(name);
        Assert.assertTrue(page.getRowCount() > 0 && page.getRowCount() < all, "❌ Search did not narrow: " + page.getRowCount());
        for (Map<String, String> r : page.getRows())
            Assert.assertTrue(String.join(" ", r.values()).toLowerCase().contains(name.toLowerCase()), "❌ Row without '" + name + "'");
        Reporter.log("   '" + name + "' → " + page.getRowCount() + " of " + all, true);
        page.searchTable("");
        Assert.assertEquals(page.getRowCount(), all, "❌ Clearing the search");
        page.searchTable("zzqq" + stamp);
        Assert.assertEquals(page.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC006_TC_002 PASSED", true);
    }

    @Test(priority = 10, description = "SC006_TC_001 — Download Report (CSV of the current filter)")
    public void sc006_tc001_download() throws Exception {
        Reporter.log("▶ SC006_TC_001 — Download Report", true);
        showAll();
        page.setStatuses("Revoked");
        page.setSelect("requestto", "--Select--");
        page.clickSearch();
        int rows = page.getRowCount();
        String name = page.getDownloadFileName();
        Reporter.log("   file name setting '" + name + "', rows " + rows, true);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        page.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] f = downloads.listFiles((d, n) -> n.endsWith(".csv"));
            if (f != null) for (File x : f) if (x.lastModified() >= start - 2000 && (csv == null || x.lastModified() > csv.lastModified())) csv = x;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No CSV downloaded");
        List<String> lines = Files.readAllLines(csv.toPath());
        Reporter.log("   " + csv.getName() + " | " + lines.size() + " lines | header " + lines.get(0), true);
        Assert.assertTrue(csv.getName().startsWith("Backend Update Requests") || csv.getName().startsWith("Backend_Update_Requests"), "❌ File name " + csv.getName());
        Assert.assertTrue(lines.get(0).contains("Child Name") && lines.get(0).contains("Request Type"), "❌ Header " + lines.get(0));
        Assert.assertFalse(lines.get(0).contains("\"Action\""), "❌ Action column exported");
        long data = lines.stream().skip(1).filter(l -> l.trim().startsWith("\"")).count();
        Assert.assertTrue(data >= rows, "❌ CSV rows " + data + " < grid rows " + rows);
        Reporter.log("✅ SC006_TC_001 PASSED", true);
    }

    @Test(priority = 11, description = "SC007_TC_001 — Pagination and sorting")
    public void sc007_tc001_paginationSorting() throws InterruptedException {
        Reporter.log("▶ SC007_TC_001 — Pagination / sorting", true);
        showAll();
        Assert.assertTrue(page.getInfoText().startsWith("Showing 1 to 10 of"), "❌ Info: " + page.getInfoText());
        page.clickNextPage();
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Next page");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 11 to"), "❌ Info page 2: " + page.getInfoText());
        Assert.assertTrue(page.getHeaderSortClass("Child Name").contains("sorting_asc"), "❌ Default order Child Name asc");
        List<String> asc = page.getSortKeys(1), sortedAsc = new ArrayList<>(asc);
        Collections.sort(sortedAsc);
        Assert.assertEquals(asc, sortedAsc, "❌ Child Name A–Z");
        page.clickColumnHeader("Child Name");
        List<String> desc = page.getSortKeys(1), sortedDesc = new ArrayList<>(desc);
        sortedDesc.sort(Collections.reverseOrder());
        Assert.assertEquals(desc, sortedDesc, "❌ Child Name Z–A");
        page.clickColumnHeader("Status");
        List<String> st = page.getSortKeys(13), sortedSt = new ArrayList<>(st);
        Collections.sort(sortedSt);
        Assert.assertEquals(st, sortedSt, "❌ Status sort");
        page.clickColumnHeader("Due Date");
        Reporter.log("   Due Date sort class: " + page.getHeaderSortClass("Due Date") + " | first " + page.getSortKeys(6).subList(0, 3), true);
        Assert.assertTrue(page.getHeaderSortClass("Due Date").contains("sorting_asc"), "❌ Due Date not sorted");
        Reporter.log("✅ SC007_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Action icons
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "SC004_TC_001 / SC005_TC_002–005 — Action icons: 6, or 7 for Void Invoice; links")
    public void sc004_tc001_actionIcons() throws InterruptedException {
        Reporter.log("▶ SC004_TC_001 — Action icons", true);
        showAll();
        page.showAllRows();
        Map<String, Integer> sets = new TreeMap<>();
        Map<String, String> normal = null, voidRow = null;
        for (Map<String, String> r : page.getRows()) {
            sets.merge(r.get("actions"), 1, Integer::sum);
            if (r.get("Request Type").equals("Void Invoice")) {
                Assert.assertEquals(r.get("actions"), ICONS_7, "❌ Void Invoice icons: " + r.get("Request id"));
                voidRow = r;
            } else {
                Assert.assertFalse(r.get("actions").contains("Void Invoice"), "❌ $ icon on a " + r.get("Request Type") + " row");
                if (normal == null && r.get("actions").equals(ICONS_6)) normal = r;
            }
        }
        Reporter.log("   icon sets " + sets, true);
        Assert.assertNotNull(normal, "❌ No row with the 6 icons");
        String id = normal.get("Request id"), child = normal.get("Child Id");
        Assert.assertEquals(page.getActionHref(id, "Account Statement"), "account_statement?child_id=" + child + "|_blank", "❌ Account Statement link");
        Assert.assertTrue(page.getActionHref(id, "Billing Plans").matches("child_financial_info\\?user_center_filter=\\d+\\|_blank"), "❌ Billing Plans: " + page.getActionHref(id, "Billing Plans"));
        Assert.assertTrue(page.getActionHref(id, "Discounts").matches("discount_usage\\?sr_centre=\\d+\\|_blank"), "❌ Discounts: " + page.getActionHref(id, "Discounts"));
        Assert.assertEquals(page.getActionHref(id, "Billing Setup Update"), "backend_billing_update?pop=yes&child_id=" + child + "|", "❌ Billing Setup link");
        String fd = page.getActionHref(id, "Freshdesk Ticket Link");
        Reporter.log("   request " + id + " Freshdesk " + fd, true);
        Assert.assertTrue(fd.startsWith("https://footprints.freshdesk.com/a/tickets/"), "❌ Freshdesk link " + fd);
        if (voidRow != null) {
            Map<String, String> d = page.getVoidButtonData(voidRow.get("Request id"));
            Reporter.log("   $ icon data " + d, true);
            Assert.assertEquals(d.get("child-id"), voidRow.get("Child Id"), "❌ $ icon child");
        }
        // Account Statement opens in a new tab, the list stays
        String list = driver.getWindowHandle();
        driver.switchTo().newWindow(WindowType.TAB);
        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + child);
        Thread.sleep(4000);
        String body = driver.findElement(org.openqa.selenium.By.tagName("body")).getText();
        Assert.assertTrue(body.contains("#" + child), "❌ Account Statement of " + child + " not shown");
        driver.close();
        driver.switchTo().window(list);
        Assert.assertTrue(page.isPageLoaded(), "❌ List tab lost");
        Reporter.log("✅ SC004_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 / SC008 — Add Backend Requests
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC003_TC_001 — Add Backend Requests popup default view")
    public void sc003_tc001_addPopupView() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Add popup", true);
        page.openAddPopup();
        Reporter.log("   title '" + page.getPopupTitle() + "' | labels " + page.getFormLabels(), true);
        Assert.assertTrue(page.getPopupText().contains("Add Backend Requests"), "❌ Title");
        List<String> types = page.getFieldOptions("request_type");
        List<String> expTypes = new ArrayList<>(TYPES);
        expTypes.add("Void Invoice");
        Assert.assertEquals(types.subList(1, types.size()), expTypes, "❌ Request Type options");
        Assert.assertEquals(page.getFieldOptions("requested_to"), STAFF, "❌ Request To options");
        Assert.assertEquals(page.getFieldValue("requested_to"), "Jaydeep Kar", "❌ Request To default");
        Assert.assertEquals(page.getFieldValue("requested_by"), "Rakesh Singh", "❌ Request By default");
        Assert.assertEquals(page.getPriority(), "Low", "❌ Priority default");
        Assert.assertEquals(page.getFieldValue("request_status"), "Pending", "❌ Request Status default");
        Assert.assertEquals(page.getFieldOptions("request_status"), Arrays.asList("--Select--", "Acknowledged", "Attrition", "Completed", "Declined", "Pending", "Reopened", "Revoked"), "❌ Status options");
        Assert.assertEquals(page.getFieldState("request_complete_before").get("readonly"), "true", "❌ Due Date not a picker");
        Assert.assertFalse(page.isFieldShown("invoice_number"), "❌ Void fields shown for no type");
        for (String l : Arrays.asList("Child ID *", "Request Type*", "Request To*", "Request By*", "Due Date", "Priority", "Action Requested *", "Request Status*", "Ticket Reference", "Comment"))
            Assert.assertTrue(page.getFormLabels().contains(l), "❌ Label missing: " + l);
        page.selectField("request_type", "Void Invoice");
        Assert.assertTrue(page.isFieldShown("invoice_number") && page.isFieldShown("invoice_type") && page.isFieldShown("invoice_amount"), "❌ Void fields not shown for Void Invoice");
        Assert.assertEquals(page.getFieldOptions("invoice_type"), Arrays.asList("--Select--", "Corporate", "Regular"), "❌ Invoice Type options");
        page.selectField("request_type", "Others");
        Assert.assertFalse(page.isFieldShown("invoice_number"), "❌ Void fields still shown");
        Assert.assertTrue(page.clickPopupCloseButton(), "❌ Close did not close the popup");
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    @Test(priority = 21, description = "SC003_TC_003 / SC008_TC_001 — Mandatory field validation; Close saves nothing")
    public void sc008_tc001_mandatoryValidation() throws InterruptedException {
        Reporter.log("▶ SC008_TC_001 — Validation", true);
        int before = page.getTotalRowCount();
        page.openAddPopup();
        page.selectField("requested_to", "--Select--");
        page.selectField("requested_by", "--Select--");
        page.selectField("request_status", "--Select--");
        page.clickSubmit();
        List<String> e1 = page.getValidationErrors();
        Reporter.log("   empty → " + e1, true);
        Assert.assertEquals(e1, Arrays.asList("Child Id is required.", "Request Type is required.", "Requested To is required.",
                "Requested By is required.", "Action Requested is required.", "Request Status is required."), "❌ Empty submit messages");
        Assert.assertTrue(page.isPopupOpen(), "❌ Popup closed on an invalid submit");
        page.setField("request_admission_id", childId);
        page.clickSubmit();
        List<String> e2 = page.getValidationErrors();
        Reporter.log("   only Child ID → " + e2, true);
        Assert.assertFalse(e2.contains("Child Id is required."), "❌ Child Id error remains");
        Assert.assertTrue(e2.contains("Action Requested is required."), "❌ Action Requested error missing");
        page.setField("request_admission_id", "");
        page.setField("request_action_requested", "QA only action");
        page.clickSubmit();
        List<String> e3 = page.getValidationErrors();
        Reporter.log("   only Action Requested → " + e3, true);
        Assert.assertTrue(e3.contains("Child Id is required."), "❌ Child Id error missing");
        Assert.assertFalse(e3.contains("Action Requested is required."), "❌ Action Requested error remains");
        // Void Invoice needs the invoice fields
        page.selectField("request_type", "Void Invoice");
        page.clickSubmit();
        List<String> e4 = page.getValidationErrors();
        Reporter.log("   Void Invoice → " + e4, true);
        Assert.assertTrue(e4.containsAll(Arrays.asList("Invoice Type is required.", "Invoice Number is required.", "Invoice Amount is required.")), "❌ Void field errors: " + e4);
        // Child ID / invoice number take digits only
        page.setField("request_admission_id", "12ab3");
        page.setField("invoice_number", "PI/88x");
        Reporter.log("   Child ID '12ab3' → '" + page.getFieldValue("request_admission_id") + "', invoice 'PI/88x' → '" + page.getFieldValue("invoice_number") + "'", true);
        Assert.assertEquals(page.getFieldValue("invoice_number"), "88", "❌ Invoice number not digits only");
        // Due date range: the picker allows server today … today + 6 months only
        String[] range = page.getDueDateRange();
        Reporter.log("   Due Date picker min " + range[0] + " max " + range[1], true);
        Assert.assertEquals(LocalDate.parse(range[1]), LocalDate.parse(range[0]).plusMonths(6), "❌ Due Date max ≠ min + 6 months");
        Assert.assertFalse(LocalDate.parse(range[0]).isBefore(LocalDate.now().minusDays(1)), "❌ Due Date min in the past: " + range[0]);
        Assert.assertTrue(page.clickPopupCloseButton(), "❌ Close did not close the popup");
        reopen();
        Assert.assertEquals(page.getTotalRowCount(), before, "❌ A request was created");
        Reporter.log("✅ SC008_TC_001 PASSED", true);
    }

    @Test(priority = 22, description = "SC003_TC_002 / SC004_TC_002 — Add a High request (own); new row is pink")
    public void sc003_tc002_addRequest() throws InterruptedException {
        Reporter.log("▶ SC003_TC_002 — Add request for child " + childId, true);
        page.openAddPopup();
        page.setField("request_admission_id", childId);
        page.selectField("request_type", "Others");
        page.selectField("requested_to", "Rakesh Singh");
        page.setDueDate(LocalDate.now().plusDays(7).toString());
        page.setPriority("High");
        page.setField("request_action_requested", action);
        page.setField("request_ticket_reference", ticket);
        page.setField("request_comment", "QA automation comment " + stamp);
        page.clickSubmit();
        Assert.assertTrue(page.getValidationErrors().isEmpty(), "❌ Validation errors: " + page.getValidationErrors());
        String msg = page.waitForPostBack();
        Reporter.log("   message: " + msg, true);
        page.showAllRows();
        Map<String, String> r = findOwnRequest();
        Assert.assertNotNull(r, "❌ New request not in the default list (Requested To Rakesh, Pending)");
        Reporter.log("   new request " + r, true);
        Assert.assertEquals(r.get("Child Id"), childId, "❌ Child");
        Assert.assertEquals(r.get("Request Type"), "Others", "❌ Type");
        Assert.assertEquals(r.get("Status"), "Pending", "❌ Status");
        Assert.assertEquals(r.get("Requested By"), "Rakesh Singh", "❌ Requested By");
        Assert.assertEquals(r.get("Requested To"), "Rakesh Singh", "❌ Requested To");
        Assert.assertEquals(r.get("Priority"), "High", "❌ Priority");
        Assert.assertTrue(r.get("background").toLowerCase().contains(PINK), "❌ High row not pink: " + r.get("background"));
        Assert.assertEquals(page.getActionHref(requestId, "Freshdesk Ticket Link"), "https://footprints.freshdesk.com/a/tickets/" + ticket + "|_blank", "❌ Freshdesk link");
        Reporter.log("✅ SC003_TC_002 PASSED — request " + requestId, true);
    }

    @Test(priority = 23, description = "SC005_TC_001 — Update own request: pre-filled, Date of Request read-only, Close, Update + Add Child History")
    public void sc005_tc001_updateRequest() throws InterruptedException {
        Reporter.log("▶ SC005_TC_001 — Update request " + requestId, true);
        needOwnRequest();
        page.openUpdatePopup(requestId);
        Reporter.log("   labels " + page.getFormLabels(), true);
        Assert.assertEquals(page.getFieldValue("request_admission_id"), childId, "❌ Child pre-fill");
        Assert.assertEquals(page.getFieldValue("request_type"), "Others", "❌ Type pre-fill");
        Assert.assertEquals(page.getFieldValue("requested_to"), "Rakesh Singh", "❌ Request To pre-fill");
        Assert.assertEquals(page.getPriority(), "High", "❌ Priority pre-fill");
        Assert.assertEquals(page.getFieldValue("request_action_requested"), action, "❌ Action pre-fill");
        Assert.assertEquals(page.getFieldValue("request_ticket_reference"), ticket, "❌ Ticket pre-fill");
        Assert.assertEquals(page.getFieldState("request_date").get("disabled"), "true", "❌ Date of Request not read-only");
        Reporter.log("   Date of Request '" + page.getFieldValue("request_date") + "' | " + page.getCommentHistoryText(), true);
        Assert.assertTrue(page.isFieldShown("add_child_history"), "❌ Add Child History checkbox missing");
        // Close keeps everything
        page.setPriority("Medium");
        Assert.assertTrue(page.clickPopupCloseButton(), "❌ Close did not close");
        reopen();
        page.showAllRows();
        Assert.assertEquals(page.getRowByRequestId(requestId).get("Priority"), "High", "❌ Close saved a change");
        // Update with Add Child History
        page.openUpdatePopup(requestId);
        page.setPriority("Medium");
        page.tickAddChildHistory(true);
        page.clickSubmit();
        Assert.assertTrue(page.getValidationErrors().contains("Admission History Notes is required."), "❌ History notes not required: " + page.getValidationErrors());
        page.setField("child_history_notes", "QA automation history note " + stamp);
        page.setField("request_comment", "QA automation update " + stamp);
        page.clickSubmit();
        String msg = page.waitForPostBack();
        Reporter.log("   message: " + msg, true);
        page.showAllRows();
        Map<String, String> r = page.getRowByRequestId(requestId);
        Assert.assertNotNull(r, "❌ Request missing after update");
        Reporter.log("   after update " + r, true);
        Assert.assertEquals(r.get("Priority"), "Medium", "❌ Priority not updated");
        Assert.assertFalse(r.get("background").toLowerCase().contains(PINK), "❌ Medium row still pink");
        String child = page.getChildHistoryText(childId);
        int h = child.indexOf("QA automation history note " + stamp);
        Reporter.log("   Child History: " + (h < 0 ? "not found" : child.substring(Math.max(0, h - 160), h + 40)), true);
        Assert.assertTrue(h >= 0, "❌ Add Child History note not in the child's history");
        page.openUpdatePopup(requestId);
        String hist = page.getCommentHistoryText();
        Reporter.log("   Comment History: " + hist, true);
        Assert.assertTrue(hist.contains("QA automation update " + stamp), "❌ Update comment not in Comment History");
        page.closePopup();
        Reporter.log("✅ SC005_TC_001 PASSED", true);
    }

    @Test(priority = 24, description = "SC005_TC_007 — Billing Setup Update: popup for the child, re-save Pause Flag (unchanged)")
    public void sc005_tc007_billingSetupUpdate() throws InterruptedException {
        Reporter.log("▶ SC005_TC_007 — Billing Setup Update", true);
        needOwnRequest();
        page.openBillingSetup(requestId);
        String text = page.getPopupText();
        Reporter.log("   " + text.substring(0, Math.min(250, text.length())), true);
        Assert.assertTrue(text.contains("Billing Setup Update (#" + childId + ")"), "❌ Popup title / child");
        String pause = page.getBillingValue("child_pause_flag");
        Reporter.log("   Pause Flag = " + pause + " | Child Status = " + page.getBillingValue("child_current_status"), true);
        Assert.assertFalse(pause.startsWith("-Select"), "❌ No current Pause Flag");
        String msg = page.clickBillingUpdate("pause-flag");
        Reporter.log("   message: " + msg, true);
        Assert.assertFalse(msg.isEmpty(), "❌ No message after Update");
        Assert.assertFalse(msg.toLowerCase().contains("error") || msg.toLowerCase().contains("fail"), "❌ Update failed: " + msg);
        page.closePopup();
        reopen();
        page.showAllRows();
        page.openBillingSetup(requestId);
        Assert.assertEquals(page.getBillingValue("child_pause_flag"), pause, "❌ Pause Flag changed");
        page.closePopup();
        Reporter.log("✅ SC005_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005_TC_006 — Void Invoice request + $ icon
    // ════════════════════════════════════════════════════════════════════
    private void pickVoidInvoice() throws InterruptedException {
        if (!voidChild.isEmpty() && !voidInvoice.isEmpty()) return;
        navigations.goToUpdatePayments();
        UpdatePaymentsPage up = new UpdatePaymentsPage(driver);
        Assert.assertTrue(up.isPageLoaded(), "❌ Update Payments did not load");
        List<List<String>> rows = up.getRowsWithAction("void");
        rows.removeIf(r -> !r.get(3).replace(",", "").equals("0.00"));
        if (rows.isEmpty()) throw new SkipException("No fully-due invoice with a Void icon on Update Payments");
        rows.sort(Comparator.comparingDouble(r -> Double.parseDouble(r.get(2).replace(",", ""))));
        voidInvoice = rows.get(0).get(0);
        voidChild = rows.get(0).get(1);
        Reporter.log("   picked invoice " + voidInvoice + " child " + voidChild + " amount " + rows.get(0).get(2) + " " + rows.get(0).get(4), true);
    }

    @Test(priority = 30, description = "SC005_TC_006 — Void Invoice request: amount lookup, $ icon voids the invoice")
    public void sc005_tc006_voidInvoice() throws InterruptedException {
        Reporter.log("▶ SC005_TC_006 — Void Invoice", true);
        pickVoidInvoice();
        reopen();
        String lookup = page.lookupInvoice(voidChild, voidInvoice, "Regular");
        Reporter.log("   lookup before: " + lookup, true);
        Assert.assertTrue(lookup.contains("success"), "❌ Invoice " + voidInvoice + " not found by the lookup");
        String amount = lookup.replaceAll(".*invoice_amount\\\\?\"\\s*:\\s*\\\\?\"([0-9.]+).*", "$1");
        // Wrong invoice number → message, no amount
        page.openAddPopup();
        page.setField("request_admission_id", voidChild);
        page.selectField("request_type", "Void Invoice");
        page.selectField("invoice_type", "Regular");
        page.setField("invoice_number", "1");
        page.blurInvoiceNumberAndWait();
        String wrongMsg = page.getInvoiceAmountError();
        Reporter.log("   wrong number on a fresh form → message '" + wrongMsg + "' (error element exists: " + page.invoiceAmountErrorExists() + ")", true);
        Assert.assertEquals(page.getFieldValue("invoice_amount"), "", "❌ Amount for a wrong invoice");
        // After a submit jQuery-validate has created #invoice_amount-error, so the lookup message can show
        page.clickSubmit();
        page.setField("invoice_number", "1");
        page.blurInvoiceNumberAndWait();
        String wrongMsg2 = page.getInvoiceAmountError();
        Reporter.log("   wrong number after a submit → '" + wrongMsg2 + "'", true);
        Assert.assertEquals(wrongMsg2, "No Invoice Amount found for this Invoice Number, Invoice Type and Child Id", "❌ Wrong-invoice message");
        // Right number → amount filled
        page.setField("invoice_number", voidInvoice);
        page.blurInvoiceNumberAndWait();
        Assert.assertEquals(page.getFieldValue("invoice_amount"), amount, "❌ Amount not auto-filled");
        page.selectField("requested_to", "Rakesh Singh");
        page.setField("request_action_requested", "QA Auto void invoice " + voidInvoice + " " + stamp);
        page.setField("request_comment", "QA automation void " + stamp);
        page.clickSubmit();
        Assert.assertTrue(page.getValidationErrors().isEmpty(), "❌ Validation errors: " + page.getValidationErrors());
        Reporter.log("   message: " + page.waitForPostBack(), true);
        page.showAllRows();
        Map<String, String> vr = null;
        for (Map<String, String> r : page.getRows())
            if (r.get("Requested Action Full").contains("QA Auto void invoice " + voidInvoice + " " + stamp)) vr = r;
        Assert.assertNotNull(vr, "❌ Void Invoice request not listed");
        String vid = vr.get("Request id");
        Reporter.log("   void request " + vr, true);
        Assert.assertEquals(vr.get("actions"), ICONS_7, "❌ Icons of the Void Invoice row");
        Map<String, String> d = page.getVoidButtonData(vid);
        Reporter.log("   $ data " + d, true);
        Assert.assertEquals(d.get("invoice-number"), voidInvoice, "❌ $ invoice number");
        Assert.assertEquals(d.get("invoice-type"), "Regular", "❌ $ invoice type");
        // Cancel on the confirm keeps the invoice
        page.startCapture(false);
        page.clickVoidInvoice(vid);
        Thread.sleep(1500);
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(VOID_CONFIRM), "❌ Confirm text");
        Assert.assertTrue(page.lookupInvoice(voidChild, voidInvoice, "Regular").contains("success"), "❌ Cancel voided the invoice");
        // OK voids it
        page.startCapture(true);
        page.clickVoidInvoice(vid);
        String msg = page.waitForVoidMessage();
        Reporter.log("   void message: '" + msg + "' alerts " + page.getCapturedAlerts(), true);
        Assert.assertEquals(msg, "Invoice Voided Successfully", "❌ Void message");
        Thread.sleep(4000);
        String after = page.lookupInvoice(voidChild, voidInvoice, "Regular");
        Reporter.log("   lookup after the void: " + after, true);
        reopen();
        showAll();
        page.showAllRows();
        Map<String, String> v2 = page.getRowByRequestId(vid);
        Reporter.log("   request after void: " + (v2 == null ? "not listed" : v2.get("Status") + " | " + v2.get("actions")), true);
        // Update Payments no longer lists the invoice as due
        navigations.goToUpdatePayments();
        UpdatePaymentsPage up = new UpdatePaymentsPage(driver);
        Assert.assertTrue(up.isPageLoaded(), "❌ Update Payments did not load");
        Assert.assertTrue(up.getRow(voidInvoice).isEmpty(), "❌ Voided invoice " + voidInvoice + " still listed as due");
        // Account Statement: the invoice is voided, with its void date
        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + voidChild);
        Thread.sleep(5000);
        String as = driver.findElement(org.openqa.selenium.By.tagName("body")).getText().replaceAll("\\s+", " ");
        int at = as.indexOf("PI/" + voidInvoice);
        String line = at < 0 ? "" : as.substring(at, Math.min(as.length(), at + 140));
        Reporter.log("   Account Statement: " + line, true);
        Assert.assertTrue(line.contains("(Voided"), "❌ Invoice " + voidInvoice + " not voided on the Account Statement");
        List<String> bugs = new ArrayList<>();
        if (wrongMsg.isEmpty()) bugs.add("wrong invoice number on a fresh form shows no message");
        if (!line.matches(".*\\(Voided on \\d{2} \\w{3}, \\d{4}\\).*")) bugs.add("no void date: '" + line.replaceAll(".*(\\(Voided[^)]*\\)).*", "$1") + "'");
        if (after.contains("success\\\":true") || after.contains("success\":true")) bugs.add("the invoice lookup still returns the voided invoice");
        Assert.assertTrue(bugs.isEmpty(), "❌ Invoice " + voidInvoice + " was voided, but: " + bugs + " (bugs)");
        Reporter.log("✅ SC005_TC_006 PASSED — invoice " + voidInvoice + " (child " + voidChild + ") voided via request " + vid, true);
    }
}
