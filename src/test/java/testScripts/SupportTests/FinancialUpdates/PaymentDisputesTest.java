package testScripts.SupportTests.FinancialUpdates;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import pages.Support.FinancialUpdates.PaymentDisputesPage;
import utils.BaseTest;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

// ═══════════════════════════════════════════════════════════════
// PAYMENT DISPUTES (Support → Financial Updates → Payment Disputes, /payment_disputes)
// Source: "TC_Support_Financial_Updates_2026 (1).xlsx", sheet "TC_Payment Disputes" (23 TCs), updated to the live
// screen (2026-11-19) + new TCs → TC_Support_Financial_Updates.xlsx, sheet TC_PaymentDisputes.
// Runs as Jaydeep Kar (getUserForScreen("Payment Disputes")): Rakesh has no Manage_Payment_Dispute — no Resolve action
// and "Sorry! You are not authorise to access this screen." on Add Dispute.
//
// Saves (user, 2026-11-19 — "Yes, on own dispute", "Any Active child"): SC002_TC_001 adds a dispute for an Active child
// with no pending dispute (taken from the Completed + Active list; override -DpdChild), SC005_TC_001 adds a second one
// for the same child, SC004_TC_003 logs a call on it and SC004_TC_004 resolves the child's pending dispute(s), so the
// child ends with no pending dispute.
// ═══════════════════════════════════════════════════════════════
public class PaymentDisputesTest extends BaseTest {

    private static final String ADD_CONFIRM = "Are you sure want to add this in Dispute ?";
    private static final String CALL_LOG_CONFIRM = "Are you sure want to add this Call Log ?";
    private static final List<String> COLUMNS = Arrays.asList("Child Id", "Child Name", "Centre", "Centre Directress", "Last Call",
            "Comment", "Added On", "Added By", "Attrition Date", "Actions");
    private static final String TODAY = LocalDate.now().toString();

    Navigations navigations;
    PaymentDisputesPage page;
    String user;

    // Shared between the save tests
    private String childId = System.getProperty("pdChild", "");
    private String childName = "";
    private final List<String> moduleIds = new ArrayList<>();
    private final String comment = "QA Auto dispute " + System.currentTimeMillis() % 100000;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
        user = getUserForScreen("Payment Disputes");
        Assert.assertFalse(user.isEmpty(), "❌ No user for Payment Disputes in input_UserRights.xlsx");
        navigations.goToUserRights();
        new UserRightsPage(driver).switchUser(user);
        Reporter.log("✅ Switched to: " + user, true);
        Thread.sleep(2000);
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws Exception {
        ensureBrowserAlive();
        dismissAlertIfPresent();
        reopen();
    }

    // Chrome sometimes drops the session during a reload — relaunch, log in and switch user again
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

    // Opens the screen with the default filters (the server may keep the last posted filter)
    private void reopen() throws InterruptedException {
        navigations.goToPaymentDisputes();
        dismissAlertIfPresent();
        page = new PaymentDisputesPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Payment Disputes did not load");
        if (!page.getFilterText("status").equals("Pending") || !page.getFilterText("child_status").equals("Active")
                || !page.getFilterText("center_id").equals("All") || !page.getFilterText("admission_type").isEmpty()) {
            page.setFilter("center_id", "All");
            page.setFilter("status", "Pending");
            page.setFilter("child_status", "Active");
            page.setFilter("admission_type", "");
            page.clickSearch();
        }
    }

    private void filter(String status, String childStatus) throws InterruptedException {
        page.setFilter("status", status);
        page.setFilter("child_status", childStatus);
        page.clickSearch();
    }

    private static long ageDays(String addedOn) {
        return ChronoUnit.DAYS.between(LocalDate.parse(addedOn), LocalDate.now());
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001 — Default view and filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC_001 / SC005_TC_002 / SC005_TC_003 — Default view: Pending disputes of Active children")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);
        Assert.assertEquals(page.getMenuPath(), "Support > Financial Updates > Payment Disputes", "❌ Menu path");
        Assert.assertEquals(page.getHeading(), "Payment Disputes", "❌ Heading");
        Assert.assertEquals(page.getFilterLabels(), Arrays.asList("Center", "Dispute Status", "Child Status", "Admission Type"), "❌ Filter labels");
        Assert.assertEquals(page.getFilterText("center_id"), "All", "❌ Center default");
        Assert.assertEquals(page.getFilterOptions("status"), Arrays.asList("Completed", "Pending"), "❌ Dispute Status options");
        Assert.assertEquals(page.getFilterText("status"), "Pending", "❌ Dispute Status default");
        Assert.assertEquals(page.getFilterOptions("child_status"), Arrays.asList("Active", "Attrition"), "❌ Child Status options");
        Assert.assertEquals(page.getFilterText("child_status"), "Active", "❌ Child Status default");
        Assert.assertEquals(page.getFilterOptions("admission_type"), Arrays.asList("", "Corporate", "Regular"), "❌ Admission Type options");
        Assert.assertTrue(page.isAddNewDisputesVisible(), "❌ Add New Disputes button missing");
        Assert.assertTrue(page.isSearchVisible(), "❌ Search box missing");
        Assert.assertTrue(page.isPaginationVisible(), "❌ Pagination missing");
        Assert.assertEquals(page.getDownloadButtonText(), "Download Data", "❌ Download button");
        Assert.assertEquals(page.getVisibleColumnHeaders(), COLUMNS, "❌ Columns");
        Assert.assertEquals(page.getPageLength(), 20, "❌ Rows per page");
        List<Map<String, String>> rows = page.getRows();
        Reporter.log("   " + page.getInfoText() + " | rows " + rows.size(), true);
        // SC005_TC_002 / TC_003: only pending disputes (coloured, with Update Call Log) of Active children (no Attrition Date)
        for (Map<String, String> r : rows) {
            Assert.assertTrue(r.get("actions").contains("Update Call Log"), "❌ Not a pending row: " + r);
            Assert.assertTrue(r.get("Attrition Date").isEmpty() || r.get("Attrition Date").startsWith("0000"), "❌ Attrition child in the default list: " + r);
        }
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    @Test(priority = 2, description = "SC001_TC_002 — Center filter")
    public void sc001_tc002_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_002 — Center filter", true);
        String centre = page.getColumnValues("Centre").get(0);
        int all = page.getTotalRowCount();
        page.setFilter("center_id", centre);
        page.clickSearch();
        Assert.assertEquals(page.getFilterText("center_id"), centre, "❌ Centre not kept");
        List<String> centres = page.getColumnValues("Centre");
        Reporter.log("   All " + all + " → " + centre + " " + centres.size(), true);
        Assert.assertFalse(centres.isEmpty(), "❌ No rows for " + centre);
        for (String c : centres) Assert.assertEquals(c, centre, "❌ Row of another centre");
        Reporter.log("✅ SC001_TC_002 PASSED", true);
    }

    @Test(priority = 3, description = "SC001_TC_003 — Dispute Status Completed: resolved disputes, not coloured, call history only")
    public void sc001_tc003_disputeStatusFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_003 — Dispute Status filter", true);
        filter("Completed", "Active");
        List<Map<String, String>> rows = page.getRows();
        Reporter.log("   Completed + Active: " + rows.size() + " rows; first " + (rows.isEmpty() ? "-" : rows.get(0)), true);
        Assert.assertFalse(rows.isEmpty(), "❌ No completed disputes");
        Set<String> actions = new TreeSet<>();
        for (Map<String, String> r : rows) {
            Assert.assertEquals(r.get("color"), "", "❌ Completed row is coloured: " + r);
            Assert.assertFalse(r.get("actions").contains("Update Call Log"), "❌ Update Call Log on a completed row: " + r);
            actions.add(r.get("actions"));
        }
        Reporter.log("   Action sets on completed rows: " + actions, true);
        Reporter.log("✅ SC001_TC_003 PASSED", true);
    }

    @Test(priority = 4, description = "SC001_TC_004 — Child Status Attrition")
    public void sc001_tc004_childStatusFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_004 — Child Status filter", true);
        filter("Pending", "Attrition");
        List<String> dates = page.getColumnValues("Attrition Date");
        Reporter.log("   Pending + Attrition: " + dates.size() + " rows; Attrition Date sample " + dates.subList(0, Math.min(3, dates.size())), true);
        Assert.assertFalse(dates.isEmpty(), "❌ No attrition rows");
        long withDate = dates.stream().filter(d -> d.matches("\\d{4}-\\d{2}-\\d{2}")).count();
        Reporter.log("   Rows with an Attrition Date: " + withDate + " / " + dates.size(), true);
        Assert.assertTrue(withDate > 0, "❌ No Attrition Date on attrition rows");
        Reporter.log("✅ SC001_TC_004 PASSED", true);
    }

    @Test(priority = 5, description = "SC001_TC_005 — Admission Type Corporate / Regular")
    public void sc001_tc005_admissionTypeFilter() throws InterruptedException {
        Reporter.log("▶ SC001_TC_005 — Admission Type filter", true);
        int all = page.getTotalRowCount();
        page.setFilter("admission_type", "Corporate");
        page.clickSearch();
        int corp = page.getTotalRowCount();
        page.setFilter("admission_type", "Regular");
        page.clickSearch();
        int reg = page.getTotalRowCount();
        Assert.assertEquals(page.getFilterText("admission_type"), "Regular", "❌ Admission Type not kept");
        Reporter.log("   All " + all + " = Corporate " + corp + " + Regular " + reg, true);
        Assert.assertTrue(corp > 0 && reg > 0, "❌ A type returned no rows");
        Assert.assertEquals(corp + reg, all, "❌ Corporate + Regular ≠ all");
        Reporter.log("✅ SC001_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — Add New Disputes
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC002_TC_002 — Add Dispute popup: default view and mandatory fields")
    public void sc002_tc002_addValidation() throws InterruptedException {
        Reporter.log("▶ SC002_TC_002 — Add validation", true);
        page.openAddDispute();
        Reporter.log("   Popup: " + page.getAddFormText(), true);
        // Type and Comment appear only after Fetch Child Details
        page.startCapture(false);
        page.clickAddDispute();
        Map<String, String> errors = page.getPopupErrors();
        Reporter.log("   Empty → " + errors, true);
        Assert.assertEquals(errors.get("child_id"), "Please select child.", "❌ Child error");
        pickChildForValidation();
        page.enterChildId(validationChild);
        page.clickFetchChildDetails();
        Reporter.log("   After fetch: " + page.getAddFormText(), true);
        Assert.assertEquals(page.getTypeOptions(), Arrays.asList("Select Type", "Invoice", "Offline Payment", "Online Payment", "Policy"), "❌ Type options");
        page.clickAddDispute();
        errors = page.getPopupErrors();
        Reporter.log("   Fetched, empty Type / Comment → " + errors, true);
        Assert.assertEquals(errors.get("invoice_payment_dispute"), "Type is required.", "❌ Type error");
        Assert.assertEquals(String.valueOf(errors.get("note_text")).replaceAll("\\s+", " "), "Comment is required.", "❌ Comment error");
        Reporter.log("   Comment message raw: [" + errors.get("note_text") + "]", true);
        Assert.assertTrue(page.getCapturedConfirms().isEmpty(), "❌ Confirm shown for an empty form");
        page.closePopup();
        Reporter.log("✅ SC002_TC_002 PASSED", true);
    }

    @Test(priority = 7, description = "SC002_TC_003 — Fetch Child Details with an unknown Child ID (new)")
    public void sc002_tc003_fetchUnknownChild() throws InterruptedException {
        Reporter.log("▶ SC002_TC_003 — Fetch unknown child", true);
        page.openAddDispute();
        page.enterChildId("9999999");
        page.clickFetchChildDetails();
        String msg = page.getPopupMessage();
        Reporter.log("   Name [" + page.getFetchedChildName() + "] message [" + msg + "] form: " + page.getAddFormText(), true);
        Assert.assertTrue(page.getFetchedChildName().isEmpty(), "❌ A name was fetched for an unknown child");
        page.closePopup();
        Reporter.log("✅ SC002_TC_003 PASSED", true);
    }

    // Any listed child, for the validation step (nothing is saved)
    private String validationChild = "";

    private void pickChildForValidation() {
        validationChild = childId.isEmpty() ? page.getColumnValues("Child Id").get(0) : childId;
    }

    private void pickChild() throws InterruptedException {
        if (!childId.isEmpty()) return;
        Set<String> pending = new HashSet<>(page.getColumnValues("Child Id"));
        filter("Completed", "Active");
        for (Map<String, String> r : page.getRows())
            if (!pending.contains(r.get("Child Id"))) { childId = r.get("Child Id"); break; }
        Assert.assertFalse(childId.isEmpty(), "❌ No Active child without a pending dispute");
        reopen();
    }

    private String addDispute(String type, String text) throws InterruptedException {
        page.openAddDispute();
        page.enterChildId(childId);
        page.clickFetchChildDetails();
        childName = page.getFetchedChildName();
        Reporter.log("   Fetched: " + childId + " → [" + childName + "] | " + page.getAddFormText(), true);
        Assert.assertFalse(childName.isEmpty(), "❌ Child name not fetched for " + childId);
        page.selectType(type);
        page.enterPopupComment(text);
        page.startCapture(true);
        page.clickAddDispute();
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(ADD_CONFIRM), "❌ Add confirm");
        String msg = page.waitForPopupMessage();
        Reporter.log("   Add (" + type + ") → [" + msg + "]", true);
        Thread.sleep(2000);
        return msg;
    }

    @Test(priority = 8, description = "SC002_TC_001 — Add a new dispute")
    public void sc002_tc001_addDispute() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Add dispute", true);
        pickChild();
        Assert.assertTrue(page.getRowsForChild(childId).isEmpty(), "❌ Child " + childId + " already has a pending dispute");
        String msg = addDispute("Invoice", comment);
        Assert.assertFalse(msg.isEmpty(), "❌ No message after Add Dispute");
        reopen();
        List<Map<String, String>> rows = page.getRowsForChild(childId);
        Reporter.log("   Rows for " + childId + ": " + rows, true);
        Assert.assertEquals(rows.size(), 1, "❌ Pending rows for the child");
        Map<String, String> r = rows.get(0);
        Assert.assertEquals(r.get("Comment"), comment, "❌ Comment");
        Assert.assertEquals(r.get("Added On"), TODAY, "❌ Added On");
        Assert.assertEquals(r.get("Added By"), user, "❌ Added By");
        Assert.assertEquals(r.get("color"), "", "❌ A new dispute is coloured");
        Assert.assertTrue(r.get("actions").contains("Update Call Log"), "❌ Update Call Log missing: " + r.get("actions"));
        moduleIds.add(r.get("module_id"));
        Reporter.log("✅ SC002_TC_001 PASSED — dispute for " + childId + " " + childName, true);
    }

    @Test(priority = 9, description = "SC005_TC_001 — Multiple disputes for the same child")
    public void sc005_tc001_multipleEntries() throws InterruptedException {
        Reporter.log("▶ SC005_TC_001 — Multiple entries", true);
        if (childName.isEmpty()) throw new SkipException("No dispute from SC002_TC_001");
        int before = page.getRowsForChild(childId).size();
        String msg = addDispute("Online Payment", comment + " (2nd)");
        reopen();
        List<Map<String, String>> rows = page.getRowsForChild(childId);
        Reporter.log("   Before " + before + " → after " + rows.size() + " | message [" + msg + "] | rows " + rows, true);
        Assert.assertEquals(rows.size(), before + 1, "❌ A second dispute for the same child was not listed");
        Reporter.log("✅ SC005_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Actions
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC004_TC_001 — Call: Parents Contact Details")
    public void sc004_tc001_call() throws InterruptedException {
        Reporter.log("▶ SC004_TC_001 — Call", true);
        String id = childName.isEmpty() ? page.getColumnValues("Child Id").get(0) : childId;
        String text = page.openCallModal(id);
        List<String> links = page.getCallModalLinks(id);
        Reporter.log("   Modal: " + text + " | links " + links, true);
        Assert.assertTrue(text.contains("Parents Contact Details"), "❌ Modal title: " + text);
        Assert.assertTrue(links.stream().anyMatch(l -> l.startsWith("tel:")), "❌ No phone link");
        Assert.assertTrue(links.stream().anyMatch(l -> l.startsWith("mailto:") && l.contains("father")), "❌ No father email");
        Assert.assertTrue(links.stream().anyMatch(l -> l.startsWith("mailto:") && l.contains("mother")), "❌ No mother email");
        page.closeCallModal(id);
        Reporter.log("✅ SC004_TC_001 PASSED", true);
    }

    @Test(priority = 11, description = "SC004_TC_002 — History: Child Updates History lists the added dispute")
    public void sc004_tc002_history() throws InterruptedException {
        Reporter.log("▶ SC004_TC_002 — History", true);
        String id = childName.isEmpty() ? page.getColumnValues("Child Id").get(0) : childId;
        String text = page.openHistory(id);
        Reporter.log("   " + text.substring(0, Math.min(500, text.length())), true);
        Assert.assertTrue(text.contains("Child Updates History"), "❌ History popup");
        if (!childName.isEmpty())
            Assert.assertTrue(text.contains("Dispute added by " + user) && text.contains(comment), "❌ The new dispute is not in the history");
        page.closePopup();
        Reporter.log("✅ SC004_TC_002 PASSED", true);
    }

    @Test(priority = 12, description = "SC004_TC_003 — Update Call Log")
    public void sc004_tc003_updateCallLog() throws InterruptedException {
        Reporter.log("▶ SC004_TC_003 — Update Call Log", true);
        if (childName.isEmpty()) throw new SkipException("No dispute from SC002_TC_001");
        page.openUpdateCallLog(childId);
        Reporter.log("   Popup: " + page.getPopupText(), true);
        Assert.assertTrue(page.getPopupText().contains("Child Name: " + childName) || page.getPopupText().contains(childName), "❌ Child name in the popup");
        page.startCapture(false);
        page.clickPopupSubmit();
        Assert.assertEquals(String.valueOf(page.getPopupErrors().get("note_text")).replaceAll("\\s+", " "), "Comment is required.", "❌ Comment error");
        page.enterPopupComment("QA Auto call log");
        page.clickPopupSubmit();
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(CALL_LOG_CONFIRM), "❌ Call log confirm");
        Thread.sleep(1500);
        Assert.assertFalse(page.getPopupNotes().stream().anyMatch(n -> n.contains("QA Auto call log")), "❌ Saved after Cancel");
        page.startCapture(true);
        page.clickPopupSubmit();
        String msg = page.waitForPopupMessage();
        List<String> notes = page.getPopupNotes();
        Reporter.log("   Message [" + msg + "] | notes " + notes, true);
        Assert.assertTrue(notes.stream().anyMatch(n -> n.contains("QA Auto call log") && n.contains(user)), "❌ Call log not listed");
        page.closePopup();
        reopen();
        Map<String, String> r = page.getRowsForChild(childId).get(0);
        Reporter.log("   Row after call log: " + r, true);
        Reporter.log("✅ SC004_TC_003 PASSED", true);
    }

    @Test(priority = 13, description = "SC004_TC_004 — Resolve Dispute")
    public void sc004_tc004_resolveDispute() throws InterruptedException {
        Reporter.log("▶ SC004_TC_004 — Resolve Dispute", true);
        if (childName.isEmpty()) throw new SkipException("No dispute from SC002_TC_001");
        int pending = page.getRowsForChild(childId).size();
        for (int i = 0; i < pending; i++) {
            page.openResolveDispute(childId);
            Reporter.log("   Popup: " + page.getPopupText(), true);
            page.enterPopupComment("QA Auto resolved");
            page.startCapture(true);
            page.clickPopupSubmit();
            String msg = page.waitForPopupMessage();
            Reporter.log("   Resolve " + (i + 1) + "/" + pending + " → confirms " + page.getCapturedConfirms() + " | [" + msg + "]", true);
            Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList(CALL_LOG_CONFIRM), "❌ Resolve confirm");
            page.closePopup();
            reopen();
            if (page.getRowsForChild(childId).isEmpty()) break;
        }
        Assert.assertTrue(page.getRowsForChild(childId).isEmpty(), "❌ Child still has a pending dispute: " + page.getRowsForChild(childId));
        filter("Completed", "Active");
        List<Map<String, String>> done = page.getRowsForChild(childId);
        Reporter.log("   Completed rows for " + childId + ": " + done, true);
        Assert.assertFalse(done.isEmpty(), "❌ Resolved dispute not in Completed");
        Reporter.log("✅ SC004_TC_004 PASSED", true);
    }

    @Test(priority = 14, description = "SC004_TC_005 — Call history of a completed dispute")
    public void sc004_tc005_callHistory() throws InterruptedException {
        Reporter.log("▶ SC004_TC_005 — Call history", true);
        filter("Completed", "Active");
        boolean own = !childName.isEmpty() && !moduleIds.isEmpty() && !page.getRowsForChild(childId).isEmpty();
        String id = own ? childId : page.getColumnValues("Child Id").get(0);
        String text = page.openCallHistory(id, own ? moduleIds.get(0) : "");
        List<String> notes = page.getPopupNotes();
        Reporter.log("   Child " + id + " | " + notes, true);
        Assert.assertTrue(text.contains("Comment List"), "❌ Call history popup: " + text);
        Assert.assertFalse(notes.isEmpty(), "❌ No comments listed");
        if (own) {
            Assert.assertTrue(notes.stream().anyMatch(n -> n.contains("QA Auto resolved")), "❌ Resolve comment missing");
            Assert.assertTrue(notes.stream().anyMatch(n -> n.contains("QA Auto call log")), "❌ Call log missing");
        }
        page.closePopup();
        Reporter.log("✅ SC004_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 — Search, download, pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "SC003_TC_001 — Search by child name and Added By")
    public void sc003_tc001_search() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Search", true);
        String name = page.getColumnValues("Child Name").get(0);
        page.searchTable(name);
        for (String n : page.getColumnValues("Child Name")) Assert.assertTrue(n.toLowerCase().contains(name.toLowerCase().trim()), "❌ " + n);
        String by = page.getColumnValues("Added By").get(0);
        page.searchTable(by);
        Assert.assertTrue(page.getRowCount() > 0, "❌ No rows for " + by);
        page.searchTable("zzqq-no-such-child");
        Assert.assertEquals(page.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        page.searchTable("");
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    @Test(priority = 16, description = "SC003_TC_002 — Download Data (CSV)")
    public void sc003_tc002_download() throws Exception {
        Reporter.log("▶ SC003_TC_002 — Download Data", true);
        String name = page.getDownloadFileName();
        Reporter.log("   File name: " + name, true);
        Assert.assertTrue(name.matches("Customers_Paying_Offline_Disputes_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "\\d{6}"), "❌ CSV name: " + name);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        page.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] f = downloads.listFiles((d, n) -> n.startsWith("Customers_Paying_Offline_Disputes") && n.endsWith(".csv"));
            if (f != null) for (File x : f) if (x.lastModified() >= start - 2000 && (csv == null || x.lastModified() > csv.lastModified())) csv = x;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No Customers_Paying_Offline_Disputes*.csv downloaded");
        List<String> lines = Files.readAllLines(csv.toPath());
        lines.removeIf(String::isEmpty);
        Reporter.log("   " + csv.getName() + " | " + lines.size() + " lines | header " + lines.get(0), true);
        for (String h : Arrays.asList("Child Id", "Child Name", "Centre", "Centre Directress", "Last Call", "Comment", "Added On", "Added By"))
            Assert.assertTrue(lines.get(0).contains("\"" + h + "\""), "❌ CSV column missing: " + h);
        Assert.assertFalse(lines.get(0).contains("Actions"), "❌ CSV has the Actions column");
        Reporter.log("   CSV data lines: " + (lines.size() - 1) + " | grid rows: " + page.getTotalRowCount(), true);
        Reporter.log("✅ SC003_TC_002 PASSED", true);
    }

    @Test(priority = 17, description = "SC003_TC_003 — Pagination (20 per page)")
    public void sc003_tc003_pagination() throws InterruptedException {
        Reporter.log("▶ SC003_TC_003 — Pagination", true);
        int total = page.getTotalRowCount();
        if (total <= 20) throw new SkipException("Only " + total + " rows");
        Assert.assertEquals(page.getPageRowCount(), 20, "❌ Rows on page 1");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 1 to 20 of " + total), "❌ Info: " + page.getInfoText());
        page.clickNextPage();
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Next");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 21 to"), "❌ Info page 2: " + page.getInfoText());
        page.clickPreviousPage();
        Assert.assertEquals(page.getCurrentPage(), 1, "❌ Previous");
        Reporter.log("✅ SC003_TC_003 PASSED", true);
    }

    @Test(priority = 18, description = "SC003_TC_004 — Column sorting")
    public void sc003_tc004_sorting() throws InterruptedException {
        Reporter.log("▶ SC003_TC_004 — Sorting", true);
        Assert.assertTrue(page.getHeaderSortClass("Child Id").contains("sorting_asc"), "❌ Default sort is not Child Id asc");
        for (String col : Arrays.asList("Child Id", "Child Name", "Added On")) {
            page.clickColumnHeader(col);
            String cls = page.getHeaderSortClass(col);
            boolean asc = cls.contains("sorting_asc");
            Assert.assertTrue(asc || cls.contains("sorting_desc"), "❌ " + col + " not sorted: " + cls);
            List<String> v = page.getOrderedColumnValues(col);
            for (int i = 1; i < v.size(); i++) {
                int c = col.equals("Child Id") ? Long.compare(Long.parseLong(v.get(i - 1)), Long.parseLong(v.get(i))) : v.get(i - 1).compareToIgnoreCase(v.get(i));
                Assert.assertTrue(asc ? c <= 0 : c >= 0, "❌ " + col + " order at " + i + ": " + v.get(i - 1) + " / " + v.get(i));
            }
            Reporter.log("   " + col + " " + (asc ? "asc" : "desc") + " OK", true);
        }
        Reporter.log("✅ SC003_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005 — Business rules
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "SC005_TC_006 / SC005_TC_007 — Pending rows coloured orange (7–15 days) and red (> 15 days)")
    public void sc005_tc006_rowColours() {
        Reporter.log("▶ SC005_TC_006 / TC_007 — Row colours", true);
        Map<String, Integer> seen = new TreeMap<>();
        List<String> wrong = new ArrayList<>();
        for (Map<String, String> r : page.getRows()) {
            long d = ageDays(r.get("Added On"));
            String c = r.get("color");
            seen.merge(d + "d:" + (c.isEmpty() ? "none" : c), 1, Integer::sum);
            String expected = d > 15 ? "red" : d > 7 && d < 15 ? "orange" : d < 7 ? "" : null;   // 7 and 15 days: boundary, logged only
            if (expected != null && !expected.equals(c)) wrong.add(r.get("Child Id") + " " + r.get("Added On") + " (" + d + "d) " + (c.isEmpty() ? "none" : c));
        }
        Reporter.log("   Age → colour: " + seen, true);
        Assert.assertTrue(wrong.isEmpty(), "❌ Wrong colour: " + wrong);
        Reporter.log("✅ SC005_TC_006 / TC_007 PASSED", true);
    }

    @Test(priority = 20, description = "SC005_TC_004 — A dispute is valid for 60 days")
    public void sc005_tc004_validSixtyDays() throws InterruptedException {
        Reporter.log("▶ SC005_TC_004 — 60-day validity", true);
        List<String> old = new ArrayList<>();
        for (Map<String, String> r : page.getRows())
            if (ageDays(r.get("Added On")) > 60) old.add(r.get("Child Id") + " " + r.get("Added On"));
        Reporter.log("   Pending Active disputes older than 60 days: " + old.size() + " " + old, true);
        Assert.assertTrue(old.isEmpty(), "❌ Disputes older than 60 days are still pending: " + old);
        Reporter.log("✅ SC005_TC_004 PASSED", true);
    }
}
