package testScripts.ChildrenTests.Updates;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Children.Updates.AttendanceRetentionPage;
import pages.Navigations;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// ATTENDANCE RETENTION (Children → Updates → Attendance Retention, /attendance_retention_call_list), SDA-13172
// Source: TC_Attendance_Retention_Trigger.xlsx (sheet TC_Attendance_Retention). Phase 1 = screen + History popup.
// Runs as Rakesh (user, 2026-11-03). Read-only: nothing is saved.
//
// On 03-Nov-2026 there was no Due Today / Escalated case, so the Log Retention Call popup (SC005/SC006) cannot be
// reached from the screen — those cases need a case opened by detectNewTriggers / advanceLadder (Phase 2).
// Tests adapt to the live data and SKIP when a tab has no case.
// ═══════════════════════════════════════════════════════════════
public class AttendanceRetentionTest extends BaseTest {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final List<String> STATUS_OPTIONS = Arrays.asList(
            "due_today=Due Today", "upcoming=Upcoming", "escalated=Escalated", "closed=Closed");
    private static final List<String> COLUMNS = Arrays.asList("CHILD", "CENTRE", "STEP / ATTEMPT", "STATUS", "TRIGGER", "NEXT DUE", "ACTION");
    private static final List<String> CLOSED_STATUSES = Arrays.asList("Closed By CD", "Support Resolved", "Child Status Changed");

    Navigations navigations;
    AttendanceRetentionPage page;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void ensureBrowserAlive() throws Exception {
        try {
            driver.getTitle();
        } catch (Exception e) {
            Reporter.log("⚠ Browser session lost — relaunching", true);
            try { driver.quit(); } catch (Exception ignored) { }
            openBrowser("chrome");
            setUp();
        }
        dismissAlertIfPresent();
    }

    // ════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════
    private void open(String query) throws InterruptedException {
        navigations.goToAttendanceRetention(query);
        page = new AttendanceRetentionPage(driver);
        if (!page.isPageLoaded()) { // UAT is slow at times — one reload
            Reporter.log("   ⚠ List slow to load — reloading once", true);
            navigations.goToAttendanceRetention(query);
            page = new AttendanceRetentionPage(driver);
        }
        Assert.assertTrue(page.isPageLoaded(), "❌ Attendance Retention list did not load");
    }

    private static LocalDate date(String dmy) {
        return LocalDate.parse(dmy.trim(), DMY);
    }

    private static String upper(String s) {
        return s.toUpperCase(Locale.ENGLISH).replaceAll("\\s+", " ").trim();
    }

    private void logRows(String tab, List<Map<String, String>> rows) {
        for (Map<String, String> r : rows)
            Reporter.log("   " + tab + ": " + r.get("child") + " #" + r.get("childId") + " | " + r.get("centre") + " | " + r.get("stepAttempt")
                    + " | " + r.get("status") + " | " + r.get("trigger") + " | " + r.get("nextDue") + " | " + r.get("actions"), true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — List screen
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC004_TC001 — Due Today loads by default with the correct columns and count")
    public void sc004_tc001_defaultView() throws InterruptedException {
        Reporter.log("▶ SC004_TC001 — Default view", true);
        open(null);
        Assert.assertEquals(page.getMenuPath(), "Children > Updates > Attendance Retention", "❌ Menu path");
        Assert.assertTrue(page.getHeading().contains("Attendance Retention Call List"), "❌ Heading: " + page.getHeading());
        Assert.assertEquals(page.getStatusOptions(), STATUS_OPTIONS, "❌ Status options");
        Assert.assertEquals(page.getSelectedStatus(), "due_today", "❌ Default status");
        Assert.assertEquals(page.getFieldValue("from_date"), "", "❌ From date should be empty by default");
        Assert.assertEquals(page.getFieldValue("to_date"), "", "❌ To date should be empty by default");
        List<String> cols = page.getColumnHeaders();
        Assert.assertEquals(cols.stream().map(AttendanceRetentionTest::upper).collect(java.util.stream.Collectors.toList()), COLUMNS, "❌ Columns");
        List<Map<String, String>> rows = page.getRows();
        Assert.assertEquals(upper(page.getBadgeText()), rows.size() + " DUE TODAY", "❌ Badge");
        LocalDate today = LocalDate.now();
        for (Map<String, String> r : rows) Assert.assertEquals(date(r.get("nextDue")), today, "❌ Due Today lists a case not due today: " + r);
        logRows("Due Today", rows);
        if (rows.isEmpty()) Reporter.log("   ℹ No case due today — empty text '" + page.getEmptyText() + "'", true);
        Reporter.log("✅ SC004_TC001 passed (badge '" + page.getBadgeText() + "')", true);
    }

    @Test(priority = 2, description = "SC004_TC002 — Status filter: Upcoming / Escalated / Closed")
    public void sc004_tc002_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC004_TC002 — Status filter", true);
        LocalDate today = LocalDate.now();
        String[][] tabs = {{"upcoming", "UPCOMING"}, {"escalated", "ESCALATED"}, {"closed", "CLOSED"}};
        for (String[] t : tabs) {
            open(null);
            page.selectStatus(t[0]);
            page.clickApply();
            Assert.assertEquals(page.getSelectedStatus(), t[0], "❌ Status not kept after Apply");
            List<Map<String, String>> rows = page.getRows();
            Assert.assertEquals(upper(page.getBadgeText()), rows.size() + " " + t[1], "❌ Badge on " + t[0]);
            logRows(t[0], rows);
            Reporter.log("   " + t[0] + " columns: " + page.getColumnHeaders(), true);
            for (Map<String, String> r : rows) {
                switch (t[0]) {
                    case "upcoming":
                        Assert.assertTrue(date(r.get("nextDue")).isAfter(today), "❌ Upcoming case not in the future: " + r);
                        break;
                    case "escalated":
                        Assert.assertEquals(r.get("status"), "Escalated", "❌ Escalated status: " + r);
                        break;
                    default:
                        Assert.assertTrue(CLOSED_STATUSES.contains(r.get("status")), "❌ Closed status value: " + r.get("status"));
                        date(r.get("nextDue"));
                }
            }
            if (t[0].equals("closed")) {
                List<String> cols = page.getColumnHeaders();
                if (cols.stream().noneMatch(c -> upper(c).equals("RESOLVED")))
                    Reporter.log("   ℹ Closed tab still shows '" + cols.get(5) + "' (sheet: Next Due replaced by Resolved) — query", true);
            }
        }
        Reporter.log("✅ SC004_TC002 passed", true);
    }

    @Test(priority = 3, description = "SC004_TC003 — Centre filter and Closed-tab From/To date range")
    public void sc004_tc003_centreAndDateFilters() throws InterruptedException {
        Reporter.log("▶ SC004_TC003 — Centre and date filters", true);
        open("filter=closed");
        List<Map<String, String>> closed = page.getRows();
        open("filter=upcoming");
        List<Map<String, String>> upcoming = page.getRows();
        Map<String, String> sample = !upcoming.isEmpty() ? upcoming.get(0) : closed.isEmpty() ? null : closed.get(0);
        if (sample == null) throw new SkipException("No Upcoming or Closed case to filter on");
        String tab = !upcoming.isEmpty() ? "upcoming" : "closed";

        // Centre — the sample's centre keeps the case; another centre drops it
        open("filter=" + tab);
        Assert.assertTrue(page.getCentreOptions().get(0).equals("--All Centres--"), "❌ First centre option");
        Assert.assertFalse(page.selectCentreByName(sample.get("centre")).isEmpty(), "❌ Centre '" + sample.get("centre") + "' not in the filter");
        page.clickApply();
        List<Map<String, String>> byCentre = page.getRows();
        Assert.assertTrue(byCentre.stream().anyMatch(r -> r.get("childId").equals(sample.get("childId"))), "❌ Case missing for its own centre");
        for (Map<String, String> r : byCentre) Assert.assertEquals(r.get("centre"), sample.get("centre"), "❌ Centre filter");
        String other = page.getCentreOptions().stream().filter(c -> !c.equals(sample.get("centre")) && !c.startsWith("--")).findFirst().orElse("");
        open("filter=" + tab);
        page.selectCentreByName(other);
        page.clickApply();
        Assert.assertTrue(page.getRows().stream().noneMatch(r -> r.get("childId").equals(sample.get("childId"))), "❌ Case shown for another centre (" + other + ")");
        Reporter.log("   Centre '" + sample.get("centre") + "' → " + byCentre.size() + " row(s); '" + other + "' → " + page.getRows().size(), true);

        // Closed tab — From/To on the resolved date
        if (closed.isEmpty()) {
            Reporter.log("   ℹ No closed case — date range not checked", true);
        } else {
            Map<String, String> c = closed.get(0);
            LocalDate resolved = date(c.get("nextDue"));
            open("filter=closed");
            page.setDateRange(resolved.toString(), resolved.toString());
            page.clickApply();
            Assert.assertTrue(page.getRows().stream().anyMatch(r -> r.get("childId").equals(c.get("childId"))), "❌ Closed case missing for its resolved date " + resolved);
            open("filter=closed");
            page.setDateRange(resolved.plusDays(1).toString(), resolved.plusDays(30).toString());
            page.clickApply();
            Assert.assertTrue(page.getRows().stream().noneMatch(r -> r.get("childId").equals(c.get("childId"))
                    && r.get("nextDue").equals(c.get("nextDue"))), "❌ Closed case shown outside its resolved date");
            Reporter.log("   Closed '" + c.get("child") + "' resolved " + resolved + " → in range / out of range OK", true);
        }
        Reporter.log("✅ SC004_TC003 passed", true);
    }

    @Test(priority = 4, description = "SC004_TC004 — Search box filters the list by child name")
    public void sc004_tc004_search() throws InterruptedException {
        Reporter.log("▶ SC004_TC004 — Search", true);
        String tab = null;
        List<Map<String, String>> rows = null;
        for (String t : new String[]{"due_today", "upcoming", "escalated", "closed"}) {
            open("filter=" + t);
            rows = page.getRows();
            if (!rows.isEmpty()) { tab = t; break; }
        }
        if (tab == null) throw new SkipException("No case on any tab");
        String child = rows.get(0).get("child");
        page.search(child);
        List<Map<String, String>> found = page.getRows();
        Assert.assertFalse(found.isEmpty(), "❌ Search '" + child + "' found nothing");
        for (Map<String, String> r : found) Assert.assertTrue(r.get("child").toLowerCase().contains(child.toLowerCase()) || r.get("centre").toLowerCase().contains(child.toLowerCase()), "❌ " + r);
        page.search("zzqa-no-such-child");
        Assert.assertTrue(page.getRows().isEmpty(), "❌ Search for a missing child still lists rows");
        Assert.assertEquals(page.getEmptyText(), "No matching records found", "❌ No-match text");
        page.search("");
        Assert.assertEquals(page.getRows().size(), rows.size(), "❌ Clearing the search did not bring back all rows");
        Reporter.log("✅ SC004_TC004 passed (" + tab + ", '" + child + "')", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005 — Log Call availability
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC005_TC004 — Log Call is disabled for a case that is not yet due")
    public void sc005_tc004_logCallDisabledWhenNotDue() throws InterruptedException {
        Reporter.log("▶ SC005_TC004 — Log Call disabled on Upcoming", true);
        open("filter=upcoming");
        List<Map<String, String>> rows = page.getRows();
        if (rows.isEmpty()) throw new SkipException("No Upcoming case");
        for (Map<String, String> r : rows) {
            Assert.assertEquals(r.get("logCallDisabled"), "true", "❌ Log Call enabled before due: " + r);
            String expected = "Not due until " + date(r.get("nextDue"));
            Assert.assertEquals(r.get("logCallTitle"), expected, "❌ Tooltip");
        }
        Map<String, String> r = rows.get(0);
        Assert.assertFalse(page.clickLogCall(r.get("childId")), "❌ Disabled Log Call opened a popup");
        logRows("Upcoming", rows);
        Reporter.log("✅ SC005_TC004 passed", true);
    }

    @Test(priority = 11, description = "SC007_TC002 / SC010 — Escalated and Closed cases show History only (no Log Call)")
    public void sc007_tc002_historyOnlyOnEscalatedAndClosed() throws InterruptedException {
        Reporter.log("▶ SC007_TC002 — History only on Escalated / Closed", true);
        int n = 0;
        for (String t : new String[]{"escalated", "closed"}) {
            open("filter=" + t);
            for (Map<String, String> r : page.getRows()) {
                Assert.assertFalse(r.get("historyTaskId").isEmpty(), "❌ History missing: " + r);
                if (t.equals("closed"))
                    Assert.assertEquals(r.get("logCallHtml"), "", "❌ Log Call shown on a closed case: " + r);
                else if (!r.get("logCallHtml").isEmpty())
                    Reporter.log("   ℹ Escalated case shows Log Call to Rakesh (" + r.get("logCallHtml") + ") — Rakesh is not the CD", true);
                n++;
            }
        }
        if (n == 0) throw new SkipException("No Escalated or Closed case");
        Reporter.log("✅ SC007_TC002 passed (" + n + " rows)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC011 — Call History popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC011_TC001 — History popup: read-only log of all attempts + Support Notes")
    public void sc011_tc001_historyPopup() throws InterruptedException {
        Reporter.log("▶ SC011_TC001 — History popup", true);
        int checked = 0;
        boolean sawSupportNotes = false, sawNoSupportNotes = false;
        for (String t : new String[]{"due_today", "upcoming", "escalated", "closed"}) {
            open("filter=" + t);
            for (Map<String, String> r : page.getRows()) {
                Assert.assertTrue(page.openHistory(r.get("historyTaskId")), "❌ History did not open: " + r);
                Assert.assertEquals(page.getPopupTitle(), ("Call History — " + r.get("child") + " (#" + r.get("childId") + ")").replaceAll("\\s+", " "), "❌ History title");
                Assert.assertEquals(page.getHistoryHeaders(), Arrays.asList("#", "Outcome", "Action", "Spoke With", "Notes", "Logged At"), "❌ History columns");
                List<String> hist = page.getHistoryRows();
                Reporter.log("   " + t + " — " + r.get("child") + ": " + hist + " | Support notes: " + page.getSupportNotes(), true);
                for (String h : hist) {
                    String[] c = h.split(" \\|\\| ", -1);
                    Assert.assertEquals(c.length, 6, "❌ History row cells: " + h);
                    Assert.assertTrue(Arrays.asList("Not Picked", "Received", "No CD Action").contains(c[1]), "❌ Outcome value: " + c[1]);
                    Assert.assertTrue(c[5].matches("\\d{2} \\w{3} \\d{4}, \\d{2}:\\d{2} [AP]M"), "❌ Logged At format: " + c[5]);
                }
                Assert.assertEquals(page.countEditableHistoryControls(), 0, "❌ Outcome table has edit controls");
                for (String b : page.getBackfillRowTexts())
                    Assert.assertTrue(b.contains("No CD Action"), "❌ Add note shown on a non-'No CD Action' row: " + b);
                if (page.hasSupportNotesSection()) {
                    sawSupportNotes = true;
                    Assert.assertFalse(page.getSupportNotes().isEmpty(), "❌ Support Notes section with no notes");
                    for (String sn : page.getSupportNotes())
                        Assert.assertTrue(sn.matches("\\[\\d{2} \\w{3} \\d{4}, \\d{2}:\\d{2} [AP]M · .+] .+"), "❌ Support note format: " + sn);
                } else sawNoSupportNotes = true;
                page.closePopup();
                checked++;
            }
        }
        if (checked == 0) throw new SkipException("No case on any tab");
        Reporter.log("   Support Notes shown on some case: " + sawSupportNotes + "; absent on some case: " + sawNoSupportNotes, true);
        Reporter.log("✅ SC011_TC001 passed (" + checked + " cases)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005 / SC006 — Log Retention Call popup (needs a Due Today case)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC005_TC001 — Log Call opens on a Due Today case (needs a due case — Phase 2)")
    public void sc005_tc001_logCallOnDueCase() throws InterruptedException {
        Reporter.log("▶ SC005_TC001 — Log Call on a Due Today case", true);
        open(null);
        Map<String, String> r = page.getRows().stream().filter(x -> "false".equals(x.get("logCallDisabled"))).findFirst()
                .orElseThrow(() -> new SkipException("No Due Today case with an enabled Log Call — open one via detectNewTriggers (Phase 2)"));
        Assert.assertTrue(page.clickLogCall(r.get("childId")), "❌ Log Call popup did not open for " + r);
        Reporter.log("   Popup: " + page.getPopupTitle(), true);
        Assert.assertTrue(page.getPopupTitle().startsWith("Log Retention Call") && page.getPopupTitle().contains(r.get("child")), "❌ Popup title");
        page.closePopup();
        Reporter.log("✅ SC005_TC001 passed", true);
    }
}
