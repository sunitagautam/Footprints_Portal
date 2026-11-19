package testScripts.SupportTests.FinancialUpdates;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.SystemLogs.EmailView_Page;
import pages.Support.FinancialUpdates.DelayPenaltyWaiversPage;
import utils.BaseTest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ═══════════════════════════════════════════════════════════════
// DELAY PENALTY WAIVERS (Support → Financial Updates → Delay Penalty Waivers, /waive-off_delay_penalty)
// Source: "TC_Support_Financial_Updates_2026 (1).xlsx", sheet "TC_DelayPenalty Waivers" (27 TCs),
// updated to the live screen (2026-11-19) + new TCs from exploration → TC_Support_Financial_Updates.xlsx,
// sheet TC_DelayPenaltyWaivers. Runs as Rakesh (no user switch, user 2026-11-18).
//
// Saves (approved by the user, 2026-11-18 — "send / resend waive-off links"): SC005_TC_001 sends one link
// to a fresh active child, SC005_TC_003 tries a second link for the same child, SC006_TC_003 resends it.
// Child: the first active child of DP_CENTER with no waive-off link yet (override -DdpChildId / -DdpCenter).
// ═══════════════════════════════════════════════════════════════
public class DelayPenaltyWaiversTest extends BaseTest {

    private static final String DP_CENTER = System.getProperty("dpCenter", "AECS Layout");
    private static final String EMAIL_SUBJECT = "Footprints - Delay penalty waived-off";
    private static final DateTimeFormatter ISSUE_ON = DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH);

    Navigations navigations;
    DelayPenaltyWaiversPage page;

    // Shared between the save tests
    private String sentChildId = System.getProperty("dpChildId", "");
    private String sentChildName = "";
    private String sentWaiveoffId = "";

    @BeforeClass(alwaysRun = true)
    public void setUp() throws InterruptedException {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        ensureBrowserAlive();
        dismissAlertIfPresent();
        reopen();
    }

    // Chrome sometimes drops the session during the reload after a save — relaunch and log in again
    private void ensureBrowserAlive() throws InterruptedException {
        try {
            driver.getTitle();
        } catch (Exception e) {
            Reporter.log("⚠ Browser session lost — relaunching (" + String.valueOf(e.getMessage()).split("\\n")[0] + ")", true);
            try { driver.quit(); } catch (Exception ignored) { }
            openBrowser("chrome");
            setUp();
        }
    }

    private void reopen() throws InterruptedException {
        navigations.goToDelayPenaltyWaivers();
        dismissAlertIfPresent();
        page = new DelayPenaltyWaiversPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Delay Penalty Waivers did not load");
    }

    private static double rupees(String s) {
        String v = s.replace("Rs.", "").replaceAll("[^0-9.]", "");
        return v.isEmpty() ? 0 : Double.parseDouble(v);
    }

    // The sort key DataTables uses for a string column, in the current order
    @SuppressWarnings("unchecked")
    private List<String> rawSortKeys(String header) {
        int i = page.getAllColumnHeaders().indexOf(header);
        return new ArrayList<>((List<String>) ((JavascriptExecutor) driver).executeScript(
                "return jQuery('.dt-delay-waiveoff').DataTable().column(arguments[0],{search:'applied',order:'applied'}).data().toArray()"
                        + ".map(function(v){return String(v).replace(/<.*?>/g,'').toLowerCase();});", i));
    }

    private static String childIdOf(String option) {
        return option.replaceAll("^.*- (\\d+):.*$", "$1");
    }

    private static String childNameOf(String option) {
        return option.substring(0, option.indexOf("- ")).trim();
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC_001 — Default view of Waive-off Delay Penalty")
    public void sc001_tc001_defaultView() {
        Reporter.log("▶ SC001_TC_001 — Default view", true);
        Assert.assertEquals(page.getMenuPath(), "Support > Financial Updates > Delay Penalty Waivers", "❌ Menu path");
        Assert.assertEquals(page.getHeadings(), Arrays.asList("Waive-off Delay Penalty", "Waive-off Links Issued"), "❌ Headings");
        List<String> labels = page.getFormLabels();
        for (String l : Arrays.asList("Center", "Child Status", "Child"))
            Assert.assertTrue(labels.contains(l), "❌ Label missing: " + l + " in " + labels);
        Assert.assertEquals(page.getCenterButtonText(), "No Filters", "❌ Center default");
        Assert.assertEquals(page.getChildStatusText(), "Active", "❌ Child Status default");
        Assert.assertEquals(page.getChildSelectionText(), "Select Child", "❌ Child default");
        Assert.assertTrue(page.isSendLinkVisible(), "❌ Send Link not visible");
        Assert.assertTrue(page.getSendLinkClass().contains("clx-btn-primary"), "❌ Send Link is not the primary (blue) button: " + page.getSendLinkClass());
        Assert.assertTrue(page.isChildHintVisible(), "❌ Hint not visible");
        Assert.assertEquals(page.getChildHintText(), "Please select a center to load children.", "❌ Hint text");
        Assert.assertEquals(page.getVisibleColumnHeaders(), Arrays.asList("Child Name", "Issue On", "Used On", "Waive-off Penalty", "Status", "Action"), "❌ Visible columns");
        Assert.assertTrue(page.isSearchVisible(), "❌ Search not visible");
        Assert.assertTrue(page.isPaginationVisible(), "❌ Pagination not visible");
        Assert.assertEquals(page.getPageLength(), 20, "❌ Page length");
        Assert.assertTrue(page.getInfoText().matches("Showing 1 to 20 of [\\d,]+ entries"), "❌ Info: " + page.getInfoText());
        Reporter.log("   " + page.getInfoText() + " | Download button present: " + page.isDownloadButtonPresent(), true);
        Reporter.log("✅ SC001_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — Center
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC002_TC_001 — Center list is alphabetical; selecting a centre loads its children")
    public void sc002_tc001_centerLoadsChildren() throws InterruptedException {
        Reporter.log("▶ SC002_TC_001 — Center loads children", true);
        List<String> centres = page.getCenterOptions();
        List<String> sorted = new ArrayList<>(centres);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        Reporter.log("   " + centres.size() + " centres, first: " + centres.subList(0, 5), true);
        Assert.assertEquals(centres, sorted, "❌ Centres are not alphabetical");

        page.selectCenters(DP_CENTER);
        Assert.assertEquals(page.getCenterButtonText(), DP_CENTER, "❌ Center button text");
        List<String> kids = page.getChildOptions();
        Reporter.log("   " + DP_CENTER + ": " + kids.size() + " active children, e.g. " + kids.subList(0, Math.min(3, kids.size())), true);
        Assert.assertFalse(kids.isEmpty(), "❌ No children loaded for " + DP_CENTER);
        for (String k : kids)
            Assert.assertTrue(k.matches(".+- \\d+: .*"), "❌ Child option format: " + k);
        Assert.assertFalse(page.isChildHintVisible(), "❌ Hint still visible after selecting a centre");
        Reporter.log("✅ SC002_TC_001 PASSED", true);
    }

    @Test(priority = 3, description = "SC002_TC_002 — Center default No Filters; Child = Select Child with hint")
    public void sc002_tc002_centerDefault() {
        Reporter.log("▶ SC002_TC_002 — Center default", true);
        Assert.assertEquals(page.getCenterButtonText(), "No Filters", "❌ Center default");
        Assert.assertEquals(page.getChildSelectionText(), "Select Child", "❌ Child default");
        Assert.assertTrue(page.getChildOptions().isEmpty(), "❌ Children listed without a centre");
        Assert.assertTrue(page.isChildHintVisible(), "❌ Hint not visible");
        Reporter.log("✅ SC002_TC_002 PASSED", true);
    }

    @Test(priority = 4, description = "SC002_TC_003 — Several centres load the children of all of them; clearing centres empties the list (new)")
    public void sc002_tc003_multipleCentres() throws InterruptedException {
        Reporter.log("▶ SC002_TC_003 — Several centres", true);
        page.selectCenters(DP_CENTER);
        Set<String> one = new HashSet<>(page.getChildOptionValues());
        page.selectCenters(DP_CENTER, "BTM Layout");
        Set<String> two = new HashSet<>(page.getChildOptionValues());
        Reporter.log("   " + DP_CENTER + " " + one.size() + " → + BTM Layout " + two.size() + " | button: " + page.getCenterButtonText(), true);
        Assert.assertEquals(page.getCenterButtonText(), DP_CENTER + ", BTM Layout", "❌ Center button text");
        Assert.assertTrue(two.containsAll(one) && two.size() > one.size(), "❌ Two centres do not list both centres' children");
        page.clearCenters();
        Assert.assertTrue(page.getChildOptions().isEmpty(), "❌ Children still listed after clearing the centres");
        Assert.assertTrue(page.isChildHintVisible(), "❌ Hint not back after clearing the centres");
        Reporter.log("✅ SC002_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 — Child Status
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC003_TC_001 — Child Status options Active / Inactive, default Active")
    public void sc003_tc001_childStatusOptions() throws InterruptedException {
        Reporter.log("▶ SC003_TC_001 — Child Status options", true);
        Assert.assertEquals(page.getChildStatusText(), "Active", "❌ Default");
        Assert.assertEquals(page.getChildStatusOptions(), Arrays.asList("Active", "Inactive"), "❌ Options");
        page.selectChildStatus("inactive");
        Assert.assertEquals(page.getChildStatusText(), "Inactive", "❌ Inactive not selected");
        Reporter.log("✅ SC003_TC_001 PASSED", true);
    }

    @Test(priority = 6, description = "SC003_TC_002 — Inactive loads only inactive children")
    public void sc003_tc002_inactiveChildren() throws InterruptedException {
        Reporter.log("▶ SC003_TC_002 — Active vs Inactive children", true);
        page.selectCenters(DP_CENTER);
        Set<String> active = new HashSet<>(page.getChildOptionValues());
        page.selectChildStatus("inactive");
        Set<String> inactive = new HashSet<>(page.getChildOptionValues());
        Reporter.log("   " + DP_CENTER + ": active " + active.size() + ", inactive " + inactive.size(), true);
        Assert.assertFalse(active.isEmpty(), "❌ No active children");
        Assert.assertFalse(inactive.isEmpty(), "❌ No inactive children");
        Set<String> both = new HashSet<>(active);
        both.retainAll(inactive);
        Assert.assertTrue(both.isEmpty(), "❌ Children in both lists: " + both);
        Reporter.log("✅ SC003_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Child depends on Center
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC004_TC_001 — Child needs a centre; selecting a child shows it and filters the table")
    public void sc004_tc001_childNeedsCentre() throws InterruptedException {
        Reporter.log("▶ SC004_TC_001 — Child needs a centre", true);
        Assert.assertTrue(page.getChildOptions().isEmpty() && page.isChildHintVisible(), "❌ Children without a centre");
        page.selectCenters(DP_CENTER);
        String opt = page.getChildOptions().get(0);
        page.selectChild(childIdOf(opt));
        Assert.assertEquals(page.getChildSelectionText(), opt, "❌ Selected child text");
        // ✅ The page searches the table by the child's name and filters on the child id
        Assert.assertEquals(page.getSearchValue(), childNameOf(opt), "❌ Search box not set to the child name");
        for (String id : page.getColumnValues("Child ID"))
            Assert.assertEquals(id, childIdOf(opt), "❌ Table shows another child after selecting " + opt);
        Reporter.log("   " + opt + " → " + page.getRowCount() + " link(s) | empty text: " + page.getEmptyTableMessage(), true);
        Reporter.log("✅ SC004_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005 / SC009 — Send Link
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC005_TC_002 / SC009_TC_002 / SC009_TC_003 — Send Link blocked without centre or child")
    public void sc005_tc002_sendLinkValidation() throws InterruptedException {
        Reporter.log("▶ SC005_TC_002 — Send Link validation", true);
        int before = page.getTotalRowCount();
        page.startCapture(false);
        page.clickSendLink();
        Assert.assertEquals(page.getChildValidationError(), "Please Select child!", "❌ No-centre validation");
        Assert.assertTrue(page.getCapturedConfirms().isEmpty(), "❌ Confirm shown without a child");

        page.selectCenters(DP_CENTER);
        page.startCapture(false);
        page.clickSendLink();
        Assert.assertEquals(page.getChildValidationError(), "Please Select child!", "❌ Centre-but-no-child validation");
        Assert.assertTrue(page.getCapturedConfirms().isEmpty(), "❌ Confirm shown without a child");

        reopen();
        Assert.assertEquals(page.getTotalRowCount(), before, "❌ A record was created without a child");
        Reporter.log("✅ SC005_TC_002 PASSED", true);
    }

    @Test(priority = 9, description = "SC005_TC_001 — Send Link creates an Open link (confirm, message, new row)")
    public void sc005_tc001_sendLink() throws InterruptedException {
        Reporter.log("▶ SC005_TC_001 — Send Link", true);
        page.selectCenters(DP_CENTER);
        if (sentChildId.isEmpty()) {
            for (String opt : page.getChildOptions())
                if (page.getWaiveoffIdsForChild(childIdOf(opt)).isEmpty()) {
                    sentChildId = childIdOf(opt);
                    break;
                }
        }
        if (sentChildId.isEmpty()) throw new SkipException("No active child in " + DP_CENTER + " without a waive-off link");
        String opt = page.getChildOptions().stream().filter(o -> childIdOf(o).equals(sentChildId)).findFirst()
                .orElseThrow(() -> new SkipException("Child " + sentChildId + " is not an active child of " + DP_CENTER));
        sentChildName = childNameOf(opt);
        List<String> before = page.getWaiveoffIdsForChild(sentChildId);
        Reporter.log("   Child " + opt + " | links before: " + before, true);
        page.selectChild(sentChildId);
        Assert.assertEquals(page.getRowCount(), before.size(), "❌ Table not filtered to the child");

        // ✅ Cancel on the confirm sends nothing
        page.startCapture(false);
        page.clickSendLink();
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList("Are you sure want to send delay penalty waived-off link?"), "❌ Confirm text");
        Thread.sleep(1500);
        Assert.assertEquals(page.getWaiveoffIdsForChild(sentChildId), before, "❌ Cancel still created a link");

        page.startCapture(true);
        page.clickSendLink();
        page.waitForReload();
        String msg = page.waitForFlashMessage();
        Reporter.log("   Message: " + msg, true);
        List<String> after = page.getWaiveoffIdsForChild(sentChildId);
        List<String> added = new ArrayList<>(after);
        added.removeAll(before);
        Reporter.log("   Links after: " + after, true);
        Assert.assertEquals(added.size(), 1, "❌ Expected one new link, got " + added);
        sentWaiveoffId = added.get(0);
        Map<String, String> row = page.getRow(sentWaiveoffId);
        Reporter.log("   New row: " + row, true);
        Assert.assertEquals(row.get("Child Name"), sentChildName, "❌ Child Name");
        Assert.assertTrue(row.get("Issue On").startsWith(LocalDate.now().format(ISSUE_ON)), "❌ Issue On not today: " + row.get("Issue On"));
        Assert.assertEquals(row.get("Used On"), "", "❌ Used On");
        Assert.assertEquals(row.get("Waive-off Penalty"), "Rs. 0.00", "❌ Waive-off Penalty");
        Assert.assertEquals(row.get("Status"), "Open", "❌ Status");
        Assert.assertEquals(row.get("Resend"), "yes", "❌ No Resend button");
        Assert.assertFalse(msg.isEmpty(), "❌ No success message after Send Link");
        Reporter.log("✅ SC005_TC_001 PASSED — waive-off " + sentWaiveoffId + " for child " + sentChildId, true);
    }

    @Test(priority = 10, dependsOnMethods = "sc005_tc001_sendLink", description = "SC005_TC_004 / SC010_TC_001-003 — Waive-off email: subject, recipients, body, link")
    public void sc005_tc004_waiveoffEmail() throws InterruptedException {
        Reporter.log("▶ SC005_TC_004 — Waive-off email", true);
        String b64 = Base64.getEncoder().encodeToString(sentChildId.getBytes(StandardCharsets.UTF_8));
        EmailView_Page email = new EmailView_Page(driver);
        String body = email.openEmailByBodyAndRecipient(EMAIL_SUBJECT, "ch=" + b64, "");
        Reporter.log("   Email: " + body.replaceAll("\\s+", " "), true);
        Assert.assertFalse(body.isEmpty(), "❌ No '" + EMAIL_SUBJECT + "' email with ch=" + b64);
        String flat = body.replaceAll("\\s+", " ");
        Assert.assertTrue(flat.contains(sentChildId + "_father@") || flat.toLowerCase().contains("father"), "❌ Father not a recipient");
        Assert.assertTrue(flat.contains(sentChildId + "_mother@") || flat.toLowerCase().contains("mother"), "❌ Mother not a recipient");
        Assert.assertTrue(flat.contains("Dear Parents"), "❌ Greeting");
        Assert.assertTrue(flat.contains("As per your request, your delay penalty is waived-off and below is the link you can use to pay your due invoices."), "❌ Waived-off line");
        Assert.assertTrue(flat.matches(".*onlinepayment\\?q=[0-9a-f]+&ch=" + java.util.regex.Pattern.quote(b64) + ".*"), "❌ Payment link with ch=" + b64);
        Assert.assertTrue(flat.contains("This is to inform you that this link is valid only for three days after receiving this email. It cannot be used by anyone else and expires after three days. Please make sure to pay your dues in this period to avoid any inconvenience."), "❌ Expiry notice");
        Assert.assertTrue(flat.contains("Regards, Team Footprints"), "❌ Sign-off");
        Reporter.log("✅ SC005_TC_004 PASSED", true);
    }

    @Test(priority = 11, dependsOnMethods = "sc005_tc001_sendLink", description = "SC005_TC_003 — A second link for a child with an Open link is blocked or replaces it")
    public void sc005_tc003_duplicateLink() throws InterruptedException {
        Reporter.log("▶ SC005_TC_003 — Duplicate link", true);
        page.selectCenters(DP_CENTER);
        page.selectChild(sentChildId);
        List<String> before = page.getWaiveoffIdsForChild(sentChildId);
        page.startCapture(true);
        page.clickSendLink();
        Thread.sleep(2500);
        String msg = page.getFlashMessage();
        reopen();
        List<String> after = page.getWaiveoffIdsForChild(sentChildId);
        String msg2 = page.getFlashMessage();
        long open = after.stream().filter(id -> "Open".equals(page.getRow(id).get("Status"))).count();
        Reporter.log("   Message: " + msg + " / after reload: " + msg2 + " | links " + before + " → " + after + " | Open: " + open, true);
        // ✅ (a) blocked — no new row, or (b) the old link expired — still only one Open link
        Assert.assertTrue(after.equals(before) || open == 1,
                "❌ Duplicate link created: child " + sentChildId + " now has " + open + " Open links " + after);
        Reporter.log("✅ SC005_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC006 — Waive-off Links Issued table
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 12, description = "SC006_TC_001 — Table default: 20 per page, columns, sorting arrows")
    public void sc006_tc001_tableDefault() {
        Reporter.log("▶ SC006_TC_001 — Table default", true);
        Assert.assertEquals(page.getPageRowCount(), 20, "❌ Rows on page 1");
        Assert.assertEquals(page.getVisibleColumnHeaders(), Arrays.asList("Child Name", "Issue On", "Used On", "Waive-off Penalty", "Status", "Action"), "❌ Columns");
        for (String h : Arrays.asList("Child Name", "Issue On", "Used On", "Waive-off Penalty", "Status"))
            Assert.assertTrue(page.getHeaderSortClass(h).contains("sorting"), "❌ No sort arrows on " + h + ": " + page.getHeaderSortClass(h));
        Reporter.log("   Action column sort class: " + page.getHeaderSortClass("Action"), true);
        // ✅ Newest first (hidden WaiveOff ID desc)
        List<String> ids = page.getPageColumnValues("WaiveOff ID");
        for (int i = 1; i < ids.size(); i++)
            Assert.assertTrue(Long.parseLong(ids.get(i - 1)) > Long.parseLong(ids.get(i)), "❌ Not newest first: " + ids);
        Reporter.log("✅ SC006_TC_001 PASSED", true);
    }

    @Test(priority = 13, description = "SC006_TC_002 / SC006_TC_005 — Status values; Resend only on Open; penalty Rs 0.00 unless Used")
    public void sc006_tc002_statusValues() {
        Reporter.log("▶ SC006_TC_002 — Status values", true);
        Map<String, Map<String, Long>> sum = page.getStatusSummary();
        Reporter.log("   " + sum, true);
        Assert.assertEquals(new TreeSet<>(sum.keySet()), new TreeSet<>(Arrays.asList("Expired", "Open", "Used")), "❌ Status values");
        sum.forEach((st, m) -> {
            if (st.equals("Open")) Assert.assertEquals(m.get("withResend"), m.get("count"), "❌ Open row without Resend");
            else Assert.assertEquals((long) m.get("withResend"), 0L, "❌ Resend on a " + st + " row");
            if (st.equals("Used")) {
                Assert.assertEquals(m.get("usedOnFilled"), m.get("count"), "❌ Used row without Used On");
                Assert.assertEquals(m.get("nonZeroPenalty"), m.get("count"), "❌ Used row with Rs 0.00 penalty");
            } else {
                Assert.assertEquals((long) m.get("usedOnFilled"), 0L, "❌ Used On on a " + st + " row");
                Assert.assertEquals((long) m.get("nonZeroPenalty"), 0L, "❌ Penalty on a " + st + " row");
            }
        });
        Reporter.log("✅ SC006_TC_002 PASSED", true);
    }

    @Test(priority = 14, dependsOnMethods = "sc005_tc001_sendLink", description = "SC006_TC_003 — Resend an Open link")
    public void sc006_tc003_resend() throws InterruptedException {
        Reporter.log("▶ SC006_TC_003 — Resend", true);
        String id = sentWaiveoffId;
        if (!"Open".equals(page.getRow(id).get("Status"))) id = page.getFirstWaiveoffIdWithStatus("Open");
        Assert.assertFalse(id.isEmpty(), "❌ No Open link to resend");
        Map<String, String> before = page.getRow(id);
        Reporter.log("   Resending waive-off " + id + ": " + before, true);

        page.startCapture(false);
        page.clickResend(id);
        Assert.assertEquals(page.getCapturedConfirms(), Collections.singletonList("Are you sure want to re-send delay penalty waived-off link?"), "❌ Confirm text");
        Assert.assertTrue(page.getFlashMessage().isEmpty(), "❌ Message after Cancel: " + page.getFlashMessage());

        page.startCapture(true);
        page.clickResend(id);
        String msg = page.waitForFlashMessage();
        Reporter.log("   Message: " + msg, true);
        Assert.assertFalse(msg.isEmpty(), "❌ No message after Resend");
        Assert.assertFalse(msg.toLowerCase().contains("error"), "❌ Resend failed: " + msg);

        reopen();
        Map<String, String> after = page.getRow(id);
        Reporter.log("   After: " + after + " | links for child: " + page.getWaiveoffIdsForChild(before.get("Child ID")), true);
        Assert.assertEquals(after.get("Status"), "Open", "❌ Status after Resend");
        Assert.assertEquals(after.get("Resend"), "yes", "❌ Resend button gone");
        Reporter.log("✅ SC006_TC_003 PASSED", true);
    }

    @Test(priority = 15, description = "SC006_TC_006 — Child Name opens Child Information (new)")
    public void sc006_tc006_childInformation() throws InterruptedException {
        Reporter.log("▶ SC006_TC_006 — Child Information", true);
        String id = page.getPageColumnValues("WaiveOff ID").get(0);
        Map<String, String> row = page.getRow(id);
        Assert.assertEquals(page.getChildInfoHref(id), "pop_child_info?pop=yes&child_id=" + row.get("Child ID"), "❌ Child link");
        Reporter.log("   " + row.get("Child Name") + " → " + page.getChildInfoHref(id), true);
        Reporter.log("✅ SC006_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC007 / SC008 — Search, pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "SC007_TC_001 — Search by child name")
    public void sc007_tc001_search() throws InterruptedException {
        Reporter.log("▶ SC007_TC_001 — Search", true);
        int total = page.getRowCount();
        String name = page.getPageColumnValues("Child Name").get(0);
        page.searchTable(name);
        List<String> names = page.getColumnValues("Child Name");
        Reporter.log("   '" + name + "' → " + names.size() + " rows", true);
        Assert.assertTrue(names.size() < total && !names.isEmpty(), "❌ Search did not narrow the list");
        for (String n : names) Assert.assertTrue(n.toLowerCase().contains(name.toLowerCase()), "❌ Non-matching row: " + n);
        page.searchTable("");
        Assert.assertEquals(page.getRowCount(), total, "❌ Clear search did not restore the rows");
        page.searchTable("zzqqxxnomatch");
        Assert.assertEquals(page.getRowCount(), 0, "❌ Rows for a non-existent name");
        Assert.assertEquals(page.getEmptyTableMessage(), "No matching records found", "❌ Empty message");
        Reporter.log("✅ SC007_TC_001 PASSED", true);
    }

    @Test(priority = 17, description = "SC008_TC_001 — Pagination (20 per page)")
    public void sc008_tc001_pagination() throws InterruptedException {
        Reporter.log("▶ SC008_TC_001 — Pagination", true);
        List<String> p1 = page.getPageColumnValues("WaiveOff ID");
        Assert.assertEquals(p1.size(), 20, "❌ Page 1 rows");
        page.clickPageNumber("2");
        Assert.assertEquals(page.getCurrentPage(), 2, "❌ Page 2 not shown");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 21 to 40 of"), "❌ Info on page 2: " + page.getInfoText());
        List<String> p2 = page.getPageColumnValues("WaiveOff ID");
        Assert.assertTrue(Collections.disjoint(p1, p2), "❌ Page 2 repeats page 1");
        page.clickNextPage();
        Assert.assertEquals(page.getCurrentPage(), 3, "❌ Next did not go to page 3");
        page.clickPreviousPage();
        page.clickPreviousPage();
        Assert.assertEquals(page.getCurrentPage(), 1, "❌ Previous did not return to page 1");
        Assert.assertTrue(page.getInfoText().startsWith("Showing 1 to 20 of"), "❌ Info on page 1: " + page.getInfoText());
        Reporter.log("✅ SC008_TC_001 PASSED", true);
    }

    @Test(priority = 18, description = "SC008_TC_002 — Column sorting")
    public void sc008_tc002_sorting() throws InterruptedException {
        Reporter.log("▶ SC008_TC_002 — Sorting", true);
        // DataTables sorts on the raw cell text (tags removed, lower case, NOT trimmed), so a name saved with a
        // leading space sorts first — compare with that same key
        page.clickColumnHeader("Child Name");
        List<String> asc = rawSortKeys("Child Name");
        List<String> s = new ArrayList<>(asc);
        Collections.sort(s);
        Assert.assertEquals(asc.subList(0, 50), s.subList(0, 50), "❌ Child Name A-Z");
        Reporter.log("   Names with leading/trailing spaces: " + asc.stream().filter(x -> !x.equals(x.trim())).count(), true);
        page.clickColumnHeader("Child Name");
        List<String> desc = rawSortKeys("Child Name");
        s.sort(Collections.reverseOrder());
        Assert.assertEquals(desc.subList(0, 50), s.subList(0, 50), "❌ Child Name Z-A");

        page.clickColumnHeader("Waive-off Penalty");
        page.clickColumnHeader("Waive-off Penalty");
        List<String> pen = page.getOrderedColumnValues("Waive-off Penalty");
        Reporter.log("   Penalty desc, top 3: " + pen.subList(0, 3), true);
        Assert.assertTrue(rupees(pen.get(0)) >= rupees(pen.get(pen.size() - 1)) && rupees(pen.get(0)) > 0, "❌ Penalty sort: " + pen.subList(0, 3));

        page.clickColumnHeader("Status");
        List<String> st = page.getOrderedColumnValues("Status");
        List<String> st2 = new ArrayList<>(st);
        Collections.sort(st2);
        Assert.assertEquals(st, st2, "❌ Status sort");

        page.clickColumnHeader("Issue On");
        Reporter.log("   Issue On sort class: " + page.getHeaderSortClass("Issue On") + " | first: " + page.getOrderedColumnValues("Issue On").subList(0, 2), true);
        Reporter.log("✅ SC008_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC011 — Issue On format (new)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "SC011_TC_001 — Issue On uses a valid 12-hour time (new)")
    public void sc011_tc001_issueOnFormat() {
        Reporter.log("▶ SC011_TC_001 — Issue On format", true);
        List<String> bad = new ArrayList<>();
        for (String v : page.getColumnValues("Issue On")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2}):\\d{2} (AM|PM)$").matcher(v);
            if (m.find() && Integer.parseInt(m.group(1)) > 12) bad.add(v);
        }
        Reporter.log("   " + bad.size() + " value(s) with a 24-hour hour + AM/PM, e.g. " + bad.subList(0, Math.min(3, bad.size())), true);
        Assert.assertTrue(bad.isEmpty(), "❌ Issue On shows a 24-hour time with AM/PM, e.g. " + bad.subList(0, Math.min(3, bad.size())));
        Reporter.log("✅ SC011_TC_001 PASSED", true);
    }
}
