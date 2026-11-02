package testScripts;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Teacher_Payout_report_detailPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ═══════════════════════════════════════════════════════════════
// TEACHER PAYOUT REPORT — /teacher_payout_report_details
// Direct URL only (not linked in any menu yet, 2026-11-02). Explored
// live 2026-11-02 — no test sheet supplied; cases are in
// ~/Desktop/QA_Documents/Testcases Scripts/TC_Teacher_Payout_Report.xlsx
// (SC001_TC001 … SC009_TC001). Stays on the default Rakesh login.
//
// Read-only report: nothing is saved. Live figures change daily, so
// the tests check the report against itself (KPIs = roll-up = Children
// tab = CSV) and against calendar maths, not against fixed numbers.
// Fixed past ranges are used for stable data: Oct 2026 (Sector 93,
// no attrition) and Jul–Sep 2026 (attrition + varied Presence %).
// Override the centre with -DtprCentreId=<id> -DtprCentre="<name>".
// ═══════════════════════════════════════════════════════════════
public class Teacher_Payout_report_detailTest extends BaseTest {

    private static final String CENTRE_ID = System.getProperty("tprCentreId", "34");
    private static final String CENTRE = System.getProperty("tprCentre", "Sector 93, Noida");

    // ✅ The URL supplied by the user (2026-11-02)
    private static final String DEFAULT_QUERY = "filter_center_id=" + CENTRE_ID + "&date_filter=Current+Quarter"
            + "&date_from=2026-10-01&date_from_submit=2026-10-01&date_to=2026-12-31&date_to_submit=2026-11-01"
            + "&teacher=all&room=all&threshold=85&saturday=off&not_marked=absent";

    private static final String OCT_FROM = "2026-10-01", OCT_TO = "2026-10-31";
    private static final String LQ_FROM = "2026-07-01", LQ_TO = "2026-09-30";

    private static final DateTimeFormatter D_MMM_YYYY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US);
    private static final DateTimeFormatter D_MMM = DateTimeFormatter.ofPattern("d MMM", Locale.US);
    private static final DateTimeFormatter MMM_YYYY = DateTimeFormatter.ofPattern("MMM yyyy", Locale.US);
    private static final DateTimeFormatter MMM_DASH_YYYY = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.US);

    private static final List<String> TEACHER_COLUMNS = Arrays.asList("Teacher (current)", "Rooms", "Children",
            "Qualify for payout", "Below 85%", "Attendance %", "Present", "Absent", "Paused / leave", "Not marked",
            "No record", "Left in period", "Attrition reasons");
    private static final List<String> CHILD_COLUMNS = Arrays.asList("Child", "Child ID", "Teacher", "Room", "Programme",
            "Counted from", "Counted to", "Holidays", "Working days", "Paused / leave", "Expected", "Present", "Absent",
            "Not marked", "No record", "Presence %", "Result");

    Navigations navigations;
    Teacher_Payout_report_detailPage reportPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        navigations = new Navigations(driver);
        reportPage = new Teacher_Payout_report_detailPage(driver);
    }

    // ✅ Chrome on this machine drops the session after ~45 report loads ("invalid session id",
    //    seen 2026-10-27 and 2026-11-02) — relaunch + log in again instead of failing the rest
    @BeforeMethod(alwaysRun = true)
    public void ensureBrowserAlive() throws InterruptedException {
        try {
            driver.getTitle();
        } catch (Exception e) {
            Reporter.log("⚠ Browser session lost — relaunching (" + e.getMessage().split("\n")[0] + ")", true);
            try { driver.quit(); } catch (Exception ignored) { }
            openBrowser("chrome");
            setUp();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════
    private void open(String query) throws InterruptedException {
        navigations.goToTeacherPayoutReport(query);
        reportPage.waitForReport();
    }

    private static String customQuery(String from, String to, String extra) {
        return "filter_center_id=" + CENTRE_ID + "&date_filter=Custom+Date+Range&date_from=" + from + "&date_from_submit=" + from
                + "&date_to=" + to + "&date_to_submit=" + to + "&teacher=all&room=all&threshold=85&saturday=off&not_marked=absent"
                + (extra == null ? "" : extra);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static int num(String s) {
        Matcher m = Pattern.compile("-?\\d+").matcher(s == null ? "" : s);
        return m.find() ? Integer.parseInt(m.group()) : 0;
    }

    // ✅ "87.1% 27 ÷ 31" → 87.1 ; "–" → -1
    private static double pct(String s) {
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)%").matcher(s == null ? "" : s);
        return m.find() ? Double.parseDouble(m.group(1)) : -1;
    }

    private static int countDays(LocalDate from, LocalDate to, DayOfWeek dow) {
        int n = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) if (d.getDayOfWeek() == dow) n++;
        return n;
    }

    // ✅ Checks "Working days considered" against the calendar: calendar days, Sundays, Saturdays, result
    private void assertWorkingDays(LocalDate from, LocalDate to, boolean saturdayWorking) {
        Map<String, Integer> eq = reportPage.getWorkingDaysEquation();
        Reporter.log("   Working days: " + eq, true);
        int calendar = (int) (to.toEpochDay() - from.toEpochDay()) + 1;
        String calLabel = "calendar days (" + from.format(D_MMM) + " – " + to.format(D_MMM) + ")";
        Assert.assertEquals(eq.get(calLabel), Integer.valueOf(calendar), "❌ Calendar days / label wrong: " + eq);
        Assert.assertEquals(reportPage.getEquationValue("Sundays"), countDays(from, to, DayOfWeek.SUNDAY), "❌ Sundays");
        int saturdays = saturdayWorking ? 0 : countDays(from, to, DayOfWeek.SATURDAY);
        Assert.assertEquals(reportPage.getEquationValue("Saturdays off"), saturdays, "❌ Saturdays off");
        int holidays = reportPage.getEquationValue("declared holidays");
        Assert.assertEquals(reportPage.getEquationValue("working days"),
                calendar - countDays(from, to, DayOfWeek.SUNDAY) - saturdays - holidays, "❌ Working days ≠ calendar − Sundays − Saturdays − holidays");
    }

    private Map<String, String> teacherRow(String teacher) {
        for (Map<String, String> r : reportPage.getTeacherRows())
            if (r.get("Teacher (current)").equals(teacher)) return r;
        throw new AssertionError("Teacher row not found: " + teacher);
    }

    private static String belowHeader(List<String> headers) {
        for (String h : headers) if (h.startsWith("Below ")) return h;
        throw new AssertionError("No 'Below X%' column in " + headers);
    }

    // ✅ KPIs = teacher roll-up totals = Children tab; per-teacher Attendance % arithmetic
    private void assertReportConsistent(String context) throws InterruptedException {
        int kids = reportPage.getKpiValue("Children in the period");
        int qualify = reportPage.getKpiValue("Qualifying for payout");
        int below = reportPage.getKpiValue("Below ");
        int absent = reportPage.getKpiValue("Unexcused absent days");

        List<Map<String, String>> teachers = reportPage.getTeacherRows();
        String belowCol = belowHeader(reportPage.getTeacherTableHeaders());
        int sKids = 0, sQualify = 0, sBelow = 0, sAbsent = 0;
        for (Map<String, String> t : teachers) {
            int children = num(t.get("Children"));
            sKids += children;
            sQualify += num(t.get("Qualify for payout"));
            sBelow += num(t.get(belowCol));
            sAbsent += num(t.get("Absent"));
            Assert.assertTrue(t.get("Qualify for payout").endsWith("of " + children),
                    "❌ " + t.get("Teacher (current)") + ": 'Qualify for payout' not 'x of " + children + "': " + t.get("Qualify for payout"));
            int present = num(t.get("Present")), abs = num(t.get("Absent")), nm = num(t.get("Not marked"));
            String att = t.get("Attendance %");
            if (present + abs + nm == 0) {
                Assert.assertEquals(att, "–", "❌ " + t.get("Teacher (current)") + ": no expected days but Attendance % = " + att);
            } else {
                // ✅ Not marked counts as absent by default (not_marked=absent)
                String expected = String.format(Locale.US, "%.1f%% %d ÷ %d", 100.0 * present / (present + abs + nm), present, present + abs + nm);
                Assert.assertEquals(att, expected, "❌ " + t.get("Teacher (current)") + ": Attendance %");
            }
        }
        Reporter.log("   [" + context + "] KPIs: children " + kids + ", qualify " + qualify + ", below " + below + ", absent " + absent
                + " | roll-up: " + sKids + "/" + sQualify + "/" + sBelow + "/" + sAbsent, true);
        Assert.assertEquals(sKids, kids, "❌ Σ teacher Children ≠ KPI");
        Assert.assertEquals(sQualify, qualify, "❌ Σ Qualify ≠ KPI");
        Assert.assertEquals(sBelow, below, "❌ Σ Below ≠ KPI");
        Assert.assertEquals(sAbsent, absent, "❌ Σ Absent ≠ KPI");

        reportPage.openTab("p-children");
        reportPage.searchChildren("");
        List<Map<String, String>> rows = reportPage.getVisibleChildRows();
        int meets = 0, belowRows = 0, absRows = 0;
        for (Map<String, String> r : rows) {
            if (r.get("Result").startsWith("Meets")) meets++;
            if (r.get("Result").startsWith("Below")) belowRows++;
            absRows += num(r.get("Absent"));
        }
        Assert.assertEquals(rows.size(), kids, "❌ Children tab rows ≠ KPI");
        Assert.assertEquals(meets, qualify, "❌ 'Meets' rows ≠ Qualifying KPI");
        Assert.assertEquals(belowRows, below, "❌ 'Below' rows ≠ Below KPI");
        Assert.assertEquals(absRows, absent, "❌ Σ child Absent ≠ KPI");
        Assert.assertEquals(reportPage.getChildrenTabBadge(), String.valueOf(kids), "❌ Children tab badge");
        reportPage.openTab("p-report");
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001 — Access & default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC001 — Report opens via the direct URL")
    public void sc001_tc001_openViaDirectUrl() throws Exception {
        Reporter.log("▶ SC001_TC001 — Open via direct URL", true);
        open(DEFAULT_QUERY);
        Assert.assertEquals(reportPage.getHeading(), "Teacher Payout Report", "❌ Heading");
        String sub = reportPage.getSubtitle();
        Reporter.log("   Subtitle: " + sub, true);
        Assert.assertTrue(sub.startsWith("Presence per child and per teacher for the 85% attendance bonus, " + CENTRE + ", "), "❌ Subtitle: " + sub);
        Assert.assertTrue(sub.matches(".*, \\d+ children$"), "❌ Subtitle should end with '<N> children': " + sub);
        Assert.assertTrue(reportPage.isReportShown(), "❌ KPI tiles not shown");
        Assert.assertEquals(reportPage.getSelectedCentre(), CENTRE);
        Assert.assertTrue(reportPage.getSelectedDateRange().startsWith("Current Quarter"), "❌ Date Range: " + reportPage.getSelectedDateRange());
        Assert.assertEquals(reportPage.getErrorAlertText(), "", "❌ Unexpected error");
        Reporter.log("✅ SC001_TC001 PASSED", true);
    }

    @Test(priority = 2, description = "SC001_TC002 — Without parameters only the filters + prompt are shown")
    public void sc001_tc002_noParameters() throws Exception {
        Reporter.log("▶ SC001_TC002 — No parameters", true);
        open("");
        Assert.assertFalse(reportPage.isReportShown(), "❌ Report shown without a centre");
        Assert.assertTrue(reportPage.isTextOnPage("Select a COCO centre and a date range, then press Go."), "❌ Prompt missing");
        Assert.assertEquals(reportPage.getSelectedCentre(), "-- Select COCO Centre --");
        Assert.assertEquals(reportPage.getSubtitle(), "Presence per child and per teacher for the 85% attendance bonus",
                "❌ Subtitle should have no centre / period part");
        Reporter.log("✅ SC001_TC002 PASSED", true);
    }

    @Test(priority = 3, description = "SC001_TC003 — Default view")
    public void sc001_tc003_defaultView() throws Exception {
        Reporter.log("▶ SC001_TC003 — Default view", true);
        open(DEFAULT_QUERY);
        Assert.assertEquals(reportPage.getTeacherOptions().get(0), "All teachers");
        Assert.assertEquals(reportPage.getRoomOptions().get(0), "All rooms");
        Assert.assertEquals(reportPage.getThresholdValue(), "85", "❌ Bonus threshold default");
        Assert.assertEquals(reportPage.getSaturdaysOptions(), Arrays.asList("Not a working day", "Working day"));
        Assert.assertEquals(reportPage.getSelectedSaturdays(), "Not a working day");
        Assert.assertEquals(reportPage.getNotMarkedOptions(), Arrays.asList("Count as absent", "Leave out (not an absence)"));
        Assert.assertEquals(reportPage.getSelectedNotMarked(), "Count as absent");
        Assert.assertEquals(reportPage.getSettingsInfoText(), "Teacher, Room and Threshold apply instantly; the others reload the report.");
        Assert.assertFalse(reportPage.isCustomDateFieldsVisible(), "❌ From / To shown for a preset range");
        List<String> tabs = reportPage.getTabNames();
        Reporter.log("   Tabs: " + tabs, true);
        Assert.assertEquals(tabs.size(), 4);
        Assert.assertEquals(tabs.get(0), "Report");
        Assert.assertEquals(tabs.get(1), "Charts");
        Assert.assertTrue(tabs.get(2).matches("Children \\d+"), "❌ Children tab: " + tabs.get(2));
        Assert.assertEquals(tabs.get(3), "How it is calculated");
        Assert.assertTrue(reportPage.isTabActive("p-report"), "❌ Report tab not active by default");
        Assert.assertEquals(reportPage.getKpiLabels(), Arrays.asList("Children in the period", "Qualifying for payout", "Below 85%", "Unexcused absent days"));
        Assert.assertEquals(reportPage.getTeacherTableHeaders(), TEACHER_COLUMNS);
        Reporter.log("✅ SC001_TC003 PASSED", true);
    }

    @Test(priority = 4, description = "SC001_TC004 — Centre list has COCO centres only; others are not loaded")
    public void sc001_tc004_cocoCentresOnly() throws Exception {
        Reporter.log("▶ SC001_TC004 — COCO centres only", true);
        open(DEFAULT_QUERY);
        List<String> centres = reportPage.getCentreOptions();
        Reporter.log("   Centres (" + (centres.size() - 1) + "): " + centres, true);
        Assert.assertEquals(centres.get(0), "-- Select COCO Centre --");
        Assert.assertTrue(centres.contains(CENTRE), "❌ " + CENTRE + " missing");
        Assert.assertFalse(centres.contains("Demo Center"), "❌ Non-COCO 'Demo Center' listed");
        for (String id : new String[]{"100", "99999"}) {
            open(DEFAULT_QUERY.replace("filter_center_id=" + CENTRE_ID, "filter_center_id=" + id));
            Assert.assertFalse(reportPage.isReportShown(), "❌ Report loaded for centre id " + id);
            Assert.assertTrue(reportPage.isTextOnPage("Select a COCO centre and a date range, then press Go."), "❌ Prompt missing for centre id " + id);
            Reporter.log("   Centre id " + id + " → not loaded, prompt shown", true);
        }
        Reporter.log("✅ SC001_TC004 PASSED", true);
    }

    @Test(priority = 5, description = "SC001_TC005 — Go without a centre")
    public void sc001_tc005_goWithoutCentre() throws Exception {
        Reporter.log("▶ SC001_TC005 — Go without a centre", true);
        open(DEFAULT_QUERY);
        String urlBefore = reportPage.getCurrentUrl();
        reportPage.selectNoCentre();
        reportPage.clickGo();
        Assert.assertTrue(reportPage.isTextOnPage("Select a centre."), "❌ 'Select a centre.' not shown");
        Assert.assertEquals(reportPage.getCurrentUrl(), urlBefore, "❌ The form was submitted without a centre");
        Reporter.log("✅ SC001_TC005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — Date range
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC002_TC001 — Date Range options are relative to today")
    public void sc002_tc001_dateRangeOptions() throws Exception {
        Reporter.log("▶ SC002_TC001 — Date Range options", true);
        open(DEFAULT_QUERY);
        LocalDate today = LocalDate.now();
        LocalDate qStart = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1);
        LocalDate lqStart = qStart.minusMonths(3);
        LocalDate lastMonth = today.minusMonths(1);
        List<String> expected = Arrays.asList(
                "Current Quarter (" + qStart.format(MMM_YYYY) + " - " + qStart.plusMonths(2).format(MMM_YYYY) + ")",
                "Current Month (" + today.format(MMM_DASH_YYYY) + ")",
                "Last Month (" + lastMonth.format(MMM_DASH_YYYY) + ")",
                "Last Quarter (" + lqStart.format(MMM_YYYY) + " - " + lqStart.plusMonths(2).format(MMM_YYYY) + ")",
                "Last 3 Months (" + today.minusMonths(3).format(MMM_DASH_YYYY) + " to " + lastMonth.format(MMM_DASH_YYYY) + ")",
                "Custom Date Range");
        Reporter.log("   Options: " + reportPage.getDateRangeOptions(), true);
        Assert.assertEquals(reportPage.getDateRangeOptions(), expected);
        Reporter.log("✅ SC002_TC001 PASSED", true);
    }

    @Test(priority = 11, description = "SC002_TC002 — Current Quarter is counted up to yesterday")
    public void sc002_tc002_currentQuarterInProgress() throws Exception {
        Reporter.log("▶ SC002_TC002 — Current Quarter", true);
        open(DEFAULT_QUERY);
        LocalDate today = LocalDate.now(), yesterday = today.minusDays(1);
        LocalDate qStart = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1);
        LocalDate qEnd = qStart.plusMonths(3).minusDays(1);
        String notice = reportPage.getNoticeText();
        Reporter.log("   Notice: " + notice, true);
        Assert.assertEquals(notice, "Period in progress: counted " + qStart.format(D_MMM_YYYY) + " to " + yesterday.format(D_MMM_YYYY)
                + " (up to yesterday). Figures will change until it ends on " + qEnd.format(D_MMM_YYYY) + ".");
        Assert.assertTrue(reportPage.getSubtitle().contains(qStart.format(D_MMM_YYYY) + " to " + yesterday.format(D_MMM_YYYY)), "❌ Subtitle period");
        assertWorkingDays(qStart, yesterday, false);
        Reporter.log("✅ SC002_TC002 PASSED", true);
    }

    @Test(priority = 12, description = "SC002_TC003 — Last Month")
    public void sc002_tc003_lastMonth() throws Exception {
        Reporter.log("▶ SC002_TC003 — Last Month", true);
        open(DEFAULT_QUERY);
        reportPage.selectDateRange("Last Month");
        reportPage.clickGo();
        LocalDate from = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        Assert.assertTrue(reportPage.getCurrentUrl().contains("date_filter=Last+Month&date_from=" + from + "&date_from_submit=" + from + "&date_to=" + to),
                "❌ URL: " + reportPage.getCurrentUrl());
        Assert.assertTrue(reportPage.getSelectedDateRange().startsWith("Last Month"));
        Assert.assertTrue(reportPage.getSubtitle().contains(from.format(D_MMM_YYYY) + " to " + to.format(D_MMM_YYYY)), "❌ Subtitle: " + reportPage.getSubtitle());
        Assert.assertEquals(reportPage.getNoticeText(), "", "❌ A completed month should not show 'Period in progress'");
        assertWorkingDays(from, to, false);
        Reporter.log("✅ SC002_TC003 PASSED", true);
    }

    @Test(priority = 13, description = "SC002_TC004 — Last Quarter and Last 3 Months")
    public void sc002_tc004_lastQuarterAndLast3Months() throws Exception {
        Reporter.log("▶ SC002_TC004 — Last Quarter / Last 3 Months", true);
        LocalDate today = LocalDate.now();
        LocalDate lqStart = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1).minusMonths(3);
        LocalDate l3Start = today.minusMonths(3).withDayOfMonth(1);
        LocalDate[][] ranges = {{lqStart, lqStart.plusMonths(3).minusDays(1)}, {l3Start, today.withDayOfMonth(1).minusDays(1)}};
        String[] names = {"Last Quarter", "Last 3 Months"};
        for (int i = 0; i < 2; i++) {
            open(DEFAULT_QUERY);
            reportPage.selectDateRange(names[i]);
            reportPage.clickGo();
            Reporter.log("   " + names[i] + ": " + reportPage.getSubtitle(), true);
            Assert.assertTrue(reportPage.getSubtitle().contains(ranges[i][0].format(D_MMM_YYYY) + " to " + ranges[i][1].format(D_MMM_YYYY)), "❌ " + names[i] + " subtitle");
            assertWorkingDays(ranges[i][0], ranges[i][1], false);
        }
        Reporter.log("✅ SC002_TC004 PASSED", true);
    }

    @Test(priority = 14, description = "SC002_TC005 — Current Month")
    public void sc002_tc005_currentMonth() throws Exception {
        Reporter.log("▶ SC002_TC005 — Current Month", true);
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() == 1) throw new SkipException("Today is the 1st: no past day in the current month");
        open(DEFAULT_QUERY);
        reportPage.selectDateRange("Current Month");
        reportPage.clickGo();
        LocalDate from = today.withDayOfMonth(1), yesterday = today.minusDays(1);
        Reporter.log("   " + reportPage.getSubtitle() + " | " + reportPage.getNoticeText(), true);
        Assert.assertTrue(reportPage.getSubtitle().contains(from.format(D_MMM_YYYY) + " to " + yesterday.format(D_MMM_YYYY)), "❌ Subtitle");
        Assert.assertTrue(reportPage.getNoticeText().startsWith("Period in progress"), "❌ Notice");
        assertWorkingDays(from, yesterday, false);
        if (reportPage.getEquationValue("working days") == 0)
            for (Map<String, String> t : reportPage.getTeacherRows())
                Assert.assertEquals(t.get("Attendance %"), "–", "❌ 0 working days but Attendance % for " + t.get("Teacher (current)"));
        Reporter.log("✅ SC002_TC005 PASSED", true);
    }

    @Test(priority = 15, description = "SC002_TC006 — Custom Date Range")
    public void sc002_tc006_customDateRange() throws Exception {
        Reporter.log("▶ SC002_TC006 — Custom Date Range", true);
        open(DEFAULT_QUERY);
        reportPage.selectDateRange("Custom Date Range");
        Assert.assertTrue(reportPage.isCustomDateFieldsVisible(), "❌ From / To not shown for Custom Date Range");
        reportPage.setCustomDates("2026-10-05", "2026-10-16");
        reportPage.clickGo();
        Assert.assertTrue(reportPage.getCurrentUrl().contains("date_filter=Custom+Date+Range&date_from=2026-10-05&date_from_submit=2026-10-05&date_to=2026-10-16"),
                "❌ URL: " + reportPage.getCurrentUrl());
        Assert.assertEquals(reportPage.getSelectedDateRange(), "Custom Date Range");
        Assert.assertEquals(reportPage.getDateFromValue(), "2026-10-05");
        Assert.assertEquals(reportPage.getDateToValue(), "2026-10-16");
        Assert.assertTrue(reportPage.getSubtitle().contains("5 Oct 2026 to 16 Oct 2026"), "❌ Subtitle: " + reportPage.getSubtitle());
        assertWorkingDays(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-16"), false);
        Reporter.log("✅ SC002_TC006 PASSED", true);
    }

    @Test(priority = 16, description = "SC002_TC007 — From date after To date")
    public void sc002_tc007_fromAfterTo() throws Exception {
        Reporter.log("▶ SC002_TC007 — From after To", true);
        open(customQuery("2026-10-20", "2026-10-10", null));
        Assert.assertEquals(reportPage.getErrorAlertText(), "The report could not be loaded. From date cannot be after To date.");
        Assert.assertFalse(reportPage.isReportShown());
        Reporter.log("✅ SC002_TC007 PASSED", true);
    }

    @Test(priority = 17, description = "SC002_TC008 — Today / future dates are not allowed")
    public void sc002_tc008_todayAndFuture() throws Exception {
        Reporter.log("▶ SC002_TC008 — Today / future", true);
        String today = LocalDate.now().toString(), nextMonth = LocalDate.now().plusMonths(2).withDayOfMonth(1).toString();
        String[][] cases = {{today, today}, {nextMonth, LocalDate.parse(nextMonth).plusDays(30).toString()}};
        for (String[] c : cases) {
            open(customQuery(c[0], c[1], null));
            Reporter.log("   " + c[0] + " → " + c[1] + ": " + reportPage.getErrorAlertText(), true);
            Assert.assertEquals(reportPage.getErrorAlertText(), "The report could not be loaded. Select dates before today. Today and future dates are not allowed.");
            Assert.assertFalse(reportPage.isReportShown());
        }
        Reporter.log("✅ SC002_TC008 PASSED", true);
    }

    @Test(priority = 18, description = "SC002_TC009 — Single-day custom range")
    public void sc002_tc009_singleDay() throws Exception {
        Reporter.log("▶ SC002_TC009 — Single day", true);
        open(customQuery("2026-10-26", "2026-10-26", null));
        Assert.assertTrue(reportPage.getSubtitle().contains("26 Oct 2026 to 26 Oct 2026"), "❌ Subtitle: " + reportPage.getSubtitle());
        assertWorkingDays(LocalDate.parse("2026-10-26"), LocalDate.parse("2026-10-26"), false);
        assertReportConsistent("26 Oct 2026");
        Reporter.log("✅ SC002_TC009 PASSED", true);
    }

    @Test(priority = 19, description = "SC002_TC010 — Custom Date Range without dates")
    public void sc002_tc010_customWithoutDates() throws Exception {
        Reporter.log("▶ SC002_TC010 — Custom Date Range without dates", true);
        open(DEFAULT_QUERY);
        String urlBefore = reportPage.getCurrentUrl();
        reportPage.selectDateRange("Custom Date Range");
        reportPage.clearCustomDates();
        reportPage.clickGo();
        Reporter.log("   UI: " + reportPage.getFilterErrorText(), true);
        Assert.assertEquals(reportPage.getFilterErrorText(), "Select both From and To dates for a custom date range.");
        Assert.assertEquals(reportPage.getCurrentUrl(), urlBefore, "❌ The form was submitted without dates");
        open(customQuery("", "2026-10-10", null));
        Reporter.log("   URL without From: " + reportPage.getErrorAlertText(), true);
        Assert.assertEquals(reportPage.getErrorAlertText(), "The report could not be loaded. Select a valid From and To date for the custom date range.");
        Assert.assertFalse(reportPage.isReportShown());
        Reporter.log("✅ SC002_TC010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 — KPI tiles & working days
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC003_TC001 — KPI tiles")
    public void sc003_tc001_kpiTiles() throws Exception {
        Reporter.log("▶ SC003_TC001 — KPI tiles", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        int kids = reportPage.getKpiValue("Children in the period");
        int qualify = reportPage.getKpiValue("Qualifying for payout");
        for (Map.Entry<String, String[]> e : reportPage.getKpiTiles().entrySet())
            Reporter.log("   " + e.getKey() + " = " + e.getValue()[0] + " (" + e.getValue()[1] + ")", true);
        Assert.assertTrue(reportPage.getKpiSub("Children in the period").matches("\\d+ left · \\d+ joined"), "❌ " + reportPage.getKpiSub("Children"));
        Assert.assertEquals(reportPage.getKpiSub("Qualifying for payout"),
                "of " + kids + " children (" + Math.round(100.0 * qualify / kids) + "%)", "❌ Qualifying sub-line");
        Assert.assertTrue(reportPage.getKpiSub("Below 85%").matches("\\d+ have unmarked or missing days"), "❌ Below sub-line");
        Assert.assertTrue(reportPage.getKpiSub("Unexcused absent days").matches("\\d+ paused / approved leave days left out"), "❌ Absent sub-line");
        Assert.assertTrue(reportPage.getSubtitle().endsWith(", " + kids + " children"), "❌ Subtitle children count ≠ KPI");
        Reporter.log("✅ SC003_TC001 PASSED", true);
    }

    @Test(priority = 21, description = "SC003_TC002 — Working days considered + declared holidays")
    public void sc003_tc002_workingDays() throws Exception {
        Reporter.log("▶ SC003_TC002 — Working days", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        assertWorkingDays(LocalDate.parse(OCT_FROM), LocalDate.parse(OCT_TO), false);
        String hol = reportPage.getHolidaysLine();
        Reporter.log("   " + hol, true);
        int holidays = reportPage.getEquationValue("declared holidays");
        Assert.assertTrue(hol.startsWith("Declared holidays: "), "❌ Holidays line");
        Assert.assertTrue(hol.endsWith("Taken from the centre's regional holiday list and centre overrides."), "❌ Holidays line source text");
        Matcher m = Pattern.compile("\\d{1,2} [A-Z][a-z]{2} \\(").matcher(hol);
        int listed = 0;
        while (m.find()) listed++;
        Assert.assertEquals(listed, holidays, "❌ Listed holidays ≠ declared holidays count");
        Reporter.log("✅ SC003_TC002 PASSED", true);
    }

    @Test(priority = 22, description = "SC003_TC003 — KPIs match the roll-up and the Children tab")
    public void sc003_tc003_kpisConsistent() throws Exception {
        Reporter.log("▶ SC003_TC003 — KPI consistency", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        assertReportConsistent("Oct 2026");
        open(customQuery(LQ_FROM, LQ_TO, null));
        assertReportConsistent("Jul–Sep 2026");
        // ✅ "x left · y joined" = children tagged Left / Joined
        String sub = reportPage.getKpiSub("Children in the period");
        reportPage.openTab("p-children");
        int left = 0, joined = 0;
        for (Map<String, String> r : reportPage.getVisibleChildRows()) {
            if (r.get("Child").contains(" Left ")) left++;
            if (r.get("Child").contains(" Joined ")) joined++;
        }
        Reporter.log("   Jul–Sep: '" + sub + "' | tags: left " + left + ", joined " + joined, true);
        Assert.assertEquals(sub, left + " left · " + joined + " joined", "❌ Left / joined tags ≠ KPI");
        Reporter.log("✅ SC003_TC003 PASSED", true);
    }

    @Test(priority = 23, description = "SC003_TC004 — Formula line and 'How it is calculated' link")
    public void sc003_tc004_formulaLine() throws Exception {
        Reporter.log("▶ SC003_TC004 — Formula line", true);
        open(DEFAULT_QUERY);
        String f = reportPage.getFormulaLine();
        Reporter.log("   " + f, true);
        Assert.assertEquals(f, "Presence % = Present ÷ Expected × 100, per child. A child at 85% or more earns the payout for their current teacher. How it is calculated");
        reportPage.clickHowItIsCalculatedLink();
        Assert.assertTrue(reportPage.isTabActive("p-how"), "❌ 'How it is calculated' tab not opened");
        Reporter.log("✅ SC003_TC004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Teacher payout roll-up, drill-down, attrition
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC004_TC001 — Roll-up columns and Rooms cell")
    public void sc004_tc001_rollupColumns() throws Exception {
        Reporter.log("▶ SC004_TC001 — Roll-up columns", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        Assert.assertEquals(reportPage.getTeacherTableHeaders(), TEACHER_COLUMNS);
        List<Map<String, String>> rows = reportPage.getTeacherRows();
        Assert.assertFalse(rows.isEmpty(), "❌ No teacher rows");
        for (Map<String, String> r : rows) {
            Assert.assertTrue(r.get("Rooms").startsWith("Primary:") && r.get("Rooms").contains("Children in:"),
                    "❌ Rooms cell of " + r.get("Teacher (current)") + ": " + r.get("Rooms"));
            Assert.assertTrue(reportPage.getTeacherOptions().contains(r.get("Teacher (current)")), "❌ Teacher not in the Teacher filter: " + r.get("Teacher (current)"));
        }
        Reporter.log("   Teachers: " + rows.size(), true);
        Reporter.log("✅ SC004_TC001 PASSED", true);
    }

    @Test(priority = 31, description = "SC004_TC002 — Roll-up totals and Attendance %")
    public void sc004_tc002_rollupTotals() throws Exception {
        Reporter.log("▶ SC004_TC002 — Roll-up totals", true);
        open(DEFAULT_QUERY);
        assertReportConsistent("Current Quarter");
        Reporter.log("✅ SC004_TC002 PASSED", true);
    }

    @Test(priority = 32, description = "SC004_TC003 — Drill-down: children who qualify")
    public void sc004_tc003_drillQualify() throws Exception {
        Reporter.log("▶ SC004_TC003 — Drill-down Qualify", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        Map<String, String> t = null;
        for (Map<String, String> r : reportPage.getTeacherRows()) if (num(r.get("Qualify for payout")) > 0) { t = r; break; }
        if (t == null) throw new SkipException("No teacher with qualifying children in Jul–Sep 2026");
        String teacher = t.get("Teacher (current)");
        reportPage.clickTeacherDrill(teacher, "ok");
        String drill = reportPage.getDrillText();
        Reporter.log("   " + drill.split("\n")[0] + " | ids " + reportPage.getDrillChildIds(), true);
        Assert.assertTrue(drill.startsWith(teacher + ": Children who qualify for payout"), "❌ Drill title: " + drill);
        Assert.assertTrue(reportPage.isDrillButtonPressed(teacher, "ok"), "❌ Button not shown as pressed");
        int q = num(t.get("Qualify for payout"));
        Assert.assertEquals(reportPage.getDrillChildIds().size(), q, "❌ Drill rows ≠ Qualify count");
        Assert.assertTrue(drill.contains("Total, " + q + " children"), "❌ Total line");
        Assert.assertTrue(drill.contains(q + " of " + q + " qualify"), "❌ 'x of x qualify'");
        // ✅ Selecting a child in the list shows that child's attendance day by day
        String childId = reportPage.getDrillChildIds().get(0);
        String day = reportPage.clickDrillChildRow(childId);
        Reporter.log("   Drill child " + childId + ": " + day.replace("\n", " | ").substring(0, Math.min(200, day.length())), true);
        Assert.assertTrue(day.contains("Present dates (") && day.contains("Presence %"), "❌ Day-by-day detail not shown for " + childId);
        // ✅ Same number again closes the drill
        reportPage.clickTeacherDrill(teacher, "ok");
        Assert.assertEquals(reportPage.getDrillText(), "", "❌ Second click did not close the drill");
        // ✅ Close button
        reportPage.clickTeacherDrill(teacher, "n");
        Assert.assertTrue(reportPage.getDrillText().contains("Total, " + num(t.get("Children")) + " children"), "❌ Children drill total");
        reportPage.closeDrill();
        Assert.assertEquals(reportPage.getDrillText(), "", "❌ Close did not close the drill");
        Reporter.log("✅ SC004_TC003 PASSED", true);
    }

    @Test(priority = 33, description = "SC004_TC004 — Drill-down: Below / Absent / No record")
    public void sc004_tc004_drillOtherMetrics() throws Exception {
        Reporter.log("▶ SC004_TC004 — Drill-down Below / Absent / No record", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        Map<String, String> t = null;
        for (Map<String, String> r : reportPage.getTeacherRows()) if (num(r.get("Below 85%")) > 0) { t = r; break; }
        if (t == null) throw new SkipException("No teacher with children below 85% in Jul–Sep 2026");
        String teacher = t.get("Teacher (current)");
        reportPage.clickTeacherDrill(teacher, "no");
        String below = reportPage.getDrillText();
        Reporter.log("   " + below.split("\n")[0], true);
        Assert.assertEquals(reportPage.getDrillChildIds().size(), num(t.get("Below 85%")), "❌ Below drill rows");
        Assert.assertTrue(below.contains("BELOW 85%") || below.contains("Below 85%"), "❌ Below drill results");
        reportPage.clickTeacherDrill(teacher, "a");
        String absent = reportPage.getDrillText();
        Reporter.log("   " + absent.split("\n")[0], true);
        Assert.assertTrue(absent.startsWith(teacher + ":"), "❌ Absent drill title");
        reportPage.clickTeacherDrill(teacher, "nr");
        String nr = reportPage.getDrillText();
        Reporter.log("   " + nr.split("\n")[0], true);
        Assert.assertTrue(nr.startsWith(teacher + ": Days with no attendance row"), "❌ No record drill title: " + nr);
        Reporter.log("✅ SC004_TC004 PASSED", true);
    }

    @Test(priority = 34, description = "SC004_TC005 — Attrition reasons by teacher")
    public void sc004_tc005_attritionMatrix() throws Exception {
        Reporter.log("▶ SC004_TC005 — Attrition reasons", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        if (reportPage.getKpiSub("Children in the period").startsWith("0 left"))
            Assert.assertEquals(reportPage.getAttritionMatrixText(), "No attrition in this period.", "❌ Empty matrix text");
        open(customQuery(LQ_FROM, LQ_TO, null));
        int left = num(reportPage.getKpiSub("Children in the period"));
        if (left == 0) throw new SkipException("No attrition in Jul–Sep 2026 for " + CENTRE);
        List<String> h = reportPage.getAttritionMatrixHeaders();
        Reporter.log("   Matrix: " + reportPage.getAttritionMatrixText().replace("\n", " | "), true);
        Assert.assertEquals(h.get(0), "Reason");
        Assert.assertEquals(h.get(h.size() - 1), "Total");
        Assert.assertEquals(reportPage.getAttritionGrandTotal(), left, "❌ Matrix total ≠ 'x left'");
        int sumLeft = 0;
        for (Map<String, String> r : reportPage.getTeacherRows()) sumLeft += num(r.get("Left in period"));
        Assert.assertEquals(sumLeft, left, "❌ Σ 'Left in period' ≠ 'x left'");
        String info = reportPage.getFirstAttritionButtonInfo();
        reportPage.clickFirstAttritionCount();
        String drill = reportPage.getDrillText();
        Reporter.log("   " + info + " → " + drill.replace("\n", " | "), true);
        String teacher = info.split(" \\| ")[0], reason = info.split(" \\| ")[1];
        Assert.assertTrue(drill.startsWith(teacher + ": children who left (" + reason + ")"), "❌ Attrition drill title");
        Assert.assertTrue(drill.contains("Attrition date") && drill.contains(reason), "❌ Attrition drill columns / reason");
        Reporter.log("✅ SC004_TC005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC005 — Instant filters & settings
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 40, description = "SC005_TC001 — Teacher filter applies instantly")
    public void sc005_tc001_teacherFilter() throws Exception {
        Reporter.log("▶ SC005_TC001 — Teacher filter", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        int total = reportPage.getKpiValue("Children in the period");
        Map<String, String> first = reportPage.getTeacherRows().get(0);
        String teacher = first.get("Teacher (current)");
        int kids = num(first.get("Children"));
        reportPage.selectTeacher(teacher);
        Reporter.log("   " + teacher + ": " + reportPage.getSubtitle(), true);
        Assert.assertTrue(reportPage.getCurrentUrl().contains("teacher=" + enc(teacher)), "❌ URL not updated: " + reportPage.getCurrentUrl());
        Assert.assertEquals(reportPage.getKpiValue("Children in the period"), kids, "❌ KPI after teacher filter");
        Assert.assertTrue(reportPage.getSubtitle().endsWith(kids + " of " + total + " children (filtered)"), "❌ Subtitle");
        Assert.assertEquals(reportPage.getTeacherRows().size(), 1, "❌ Roll-up should show one teacher");
        Assert.assertEquals(reportPage.getChildrenTabBadge(), String.valueOf(kids));
        reportPage.openTab("p-children");
        for (Map<String, String> r : reportPage.getVisibleChildRows())
            Assert.assertEquals(r.get("Teacher"), teacher, "❌ Child of another teacher shown: " + r.get("Child ID"));
        Reporter.log("✅ SC005_TC001 PASSED", true);
    }

    @Test(priority = 41, description = "SC005_TC002 — Room filter applies instantly")
    public void sc005_tc002_roomFilter() throws Exception {
        Reporter.log("▶ SC005_TC002 — Room filter", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        String room = reportPage.getRoomOptions().get(1);
        reportPage.selectRoom(room);
        int kids = reportPage.getKpiValue("Children in the period");
        Reporter.log("   " + room + ": " + reportPage.getSubtitle(), true);
        Assert.assertTrue(reportPage.getCurrentUrl().contains("room=" + enc(room)), "❌ URL not updated");
        reportPage.openTab("p-children");
        List<Map<String, String>> rows = reportPage.getVisibleChildRows();
        Assert.assertEquals(rows.size(), kids, "❌ Rows ≠ KPI");
        for (Map<String, String> r : rows) Assert.assertEquals(r.get("Room"), room, "❌ Other room shown: " + r.get("Child ID"));
        reportPage.openTab("p-report");
        assertReportConsistent("room " + room);
        Reporter.log("✅ SC005_TC002 PASSED", true);
    }

    @Test(priority = 42, description = "SC005_TC003 — Teacher + Room together; no-match message")
    public void sc005_tc003_teacherAndRoom() throws Exception {
        Reporter.log("▶ SC005_TC003 — Teacher + Room", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        reportPage.openTab("p-children");
        Map<String, String> child = reportPage.getVisibleChildRows().get(0);
        String teacher = child.get("Teacher"), room = child.get("Room");
        reportPage.openTab("p-report");
        reportPage.selectTeacher(teacher);
        reportPage.selectRoom(room);
        reportPage.openTab("p-children");
        List<Map<String, String>> rows = reportPage.getVisibleChildRows();
        Reporter.log("   " + teacher + " + " + room + " → " + rows.size() + " children", true);
        Assert.assertFalse(rows.isEmpty());
        for (Map<String, String> r : rows) {
            Assert.assertEquals(r.get("Teacher"), teacher);
            Assert.assertEquals(r.get("Room"), room);
        }
        Assert.assertEquals(reportPage.getKpiValue("Children in the period"), rows.size(), "❌ KPI ≠ rows");
        // ✅ A room this teacher has no children in → no-match message
        String emptyRoom = null;
        for (String r : reportPage.getRoomOptions().subList(1, reportPage.getRoomOptions().size())) {
            reportPage.selectRoom(r);
            if (reportPage.getKpiValue("Children in the period") == 0) { emptyRoom = r; break; }
        }
        if (emptyRoom == null) {
            Reporter.log("   ℹ " + teacher + " has children in every room — no-match check not possible", true);
        } else {
            Reporter.log("   " + teacher + " + " + emptyRoom + " → " + reportPage.getChildTableBodyText(), true);
            Assert.assertEquals(reportPage.getChildTableBodyText(), "No children match the selected filters.");
            Assert.assertTrue(reportPage.getTeacherTableText().contains("No children match the selected filters."), "❌ Roll-up no-match text");
            Assert.assertTrue(reportPage.getSubtitle().contains(" 0 of "), "❌ Subtitle should say '0 of N children (filtered)'");
        }
        Reporter.log("✅ SC005_TC003 PASSED", true);
    }

    @Test(priority = 43, description = "SC005_TC004 — Teacher / Room / Threshold from the URL")
    public void sc005_tc004_filtersFromUrl() throws Exception {
        Reporter.log("▶ SC005_TC004 — Filters from URL", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        String teacher = reportPage.getTeacherOptions().get(1), room = reportPage.getRoomOptions().get(1);
        open(customQuery(OCT_FROM, OCT_TO, null).replace("teacher=all", "teacher=" + enc(teacher)).replace("threshold=85", "threshold=90"));
        Assert.assertEquals(reportPage.getSelectedTeacher(), teacher);
        Assert.assertEquals(reportPage.getThresholdValue(), "90");
        Assert.assertTrue(reportPage.getKpiLabels().contains("Below 90%"), "❌ Threshold from URL not applied");
        Assert.assertTrue(reportPage.getSubtitle().contains("(filtered)"), "❌ Teacher from URL not applied");
        open(customQuery(OCT_FROM, OCT_TO, null).replace("room=all", "room=" + enc(room)));
        Assert.assertEquals(reportPage.getSelectedRoom(), room);
        Assert.assertTrue(reportPage.getSubtitle().contains("(filtered)"), "❌ Room from URL not applied");
        Reporter.log("✅ SC005_TC004 PASSED", true);
    }

    @Test(priority = 44, description = "SC005_TC005 — Bonus threshold re-classifies children instantly")
    public void sc005_tc005_thresholdReclassifies() throws Exception {
        Reporter.log("▶ SC005_TC005 — Threshold", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        int kids = reportPage.getKpiValue("Children in the period");
        for (String thr : new String[]{"70", "80", "90", "85.5"}) {
            reportPage.setThreshold(thr);
            double t = Double.parseDouble(thr);
            int q = reportPage.getKpiValue("Qualifying for payout"), b = reportPage.getKpiValue("Below ");
            reportPage.openTab("p-children");
            int expQ = 0, expB = 0, noDays = 0;
            for (Map<String, String> r : reportPage.getVisibleChildRows()) {
                double p = pct(r.get("Presence %"));
                String res = r.get("Result");
                if (p < 0) { noDays++; Assert.assertEquals(res, "No days"); continue; }
                if (p >= t) { expQ++; Assert.assertEquals(res, "Meets " + thr + "%", "❌ " + r.get("Child ID") + " " + p); }
                else { expB++; Assert.assertEquals(res, "Below " + thr + "%", "❌ " + r.get("Child ID") + " " + p); }
            }
            reportPage.openTab("p-report");
            Reporter.log("   " + thr + "%: qualify " + q + ", below " + b + ", no days " + noDays, true);
            Assert.assertTrue(reportPage.getCurrentUrl().contains("threshold=" + thr), "❌ URL threshold");
            Assert.assertEquals(q, expQ, "❌ Qualifying at " + thr);
            Assert.assertEquals(b, expB, "❌ Below at " + thr);
            Assert.assertEquals(q + b + noDays, kids, "❌ Qualify + Below + No days ≠ children");
            Assert.assertTrue(reportPage.getKpiLabels().contains("Below " + thr + "%"), "❌ KPI label");
            Assert.assertTrue(reportPage.getTeacherTableHeaders().contains("Below " + thr + "%"), "❌ Roll-up header");
            Assert.assertTrue(reportPage.getSubtitle().contains("for the " + thr + "% attendance bonus"), "❌ Subtitle threshold");
        }
        Reporter.log("✅ SC005_TC005 PASSED", true);
    }

    @Test(priority = 45, description = "SC005_TC006 — Threshold outside 1–100 on screen")
    public void sc005_tc006_thresholdValidation() throws Exception {
        Reporter.log("▶ SC005_TC006 — Threshold validation", true);
        open(DEFAULT_QUERY);
        for (String v : new String[]{"0", "101", "-5"}) {
            reportPage.setThreshold(v);
            Reporter.log("   " + v + " → '" + reportPage.getThresholdValidationText() + "' | KPI labels " + reportPage.getKpiLabels(), true);
            Assert.assertEquals(reportPage.getThresholdValidationText(), "Threshold must be a number between 1 and 100.", "❌ Message for " + v);
            Assert.assertTrue(reportPage.getKpiLabels().contains("Below 85%"), "❌ Invalid value " + v + " was applied");
        }
        reportPage.setThreshold("85.5");
        Assert.assertEquals(reportPage.getThresholdValidationText(), "", "❌ Message shown for a valid decimal");
        Assert.assertTrue(reportPage.getKpiLabels().contains("Below 85.5%"));
        Reporter.log("✅ SC005_TC006 PASSED", true);
    }

    @Test(priority = 46, description = "SC005_TC007 — Threshold outside 1–100 in the URL")
    public void sc005_tc007_thresholdInUrl() throws Exception {
        Reporter.log("▶ SC005_TC007 — Threshold in URL", true);
        open(DEFAULT_QUERY.replace("threshold=85", "threshold=150"));
        Assert.assertEquals(reportPage.getErrorAlertText(), "The report could not be loaded. Threshold must be a number between 1 and 100. \"150\" is not allowed.");
        Assert.assertFalse(reportPage.isReportShown());
        Reporter.log("✅ SC005_TC007 PASSED", true);
    }

    @Test(priority = 47, description = "SC005_TC008 — Saturdays = Working day reloads the report")
    public void sc005_tc008_saturdaysWorking() throws Exception {
        Reporter.log("▶ SC005_TC008 — Saturdays", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        int wdOff = reportPage.getEquationValue("working days");
        reportPage.selectSaturdays("Working day");
        Assert.assertTrue(reportPage.getCurrentUrl().contains("saturday=on"), "❌ URL: " + reportPage.getCurrentUrl());
        Assert.assertEquals(reportPage.getSelectedSaturdays(), "Working day");
        assertWorkingDays(LocalDate.parse(OCT_FROM), LocalDate.parse(OCT_TO), true);
        int wdOn = reportPage.getEquationValue("working days");
        Reporter.log("   Working days: off " + wdOff + " → on " + wdOn, true);
        Assert.assertEquals(wdOn - wdOff, countDays(LocalDate.parse(OCT_FROM), LocalDate.parse(OCT_TO), DayOfWeek.SATURDAY), "❌ Saturdays not added");
        Reporter.log("✅ SC005_TC008 PASSED", true);
    }

    @Test(priority = 48, description = "SC005_TC009 — Not marked days: Count as absent vs Leave out")
    public void sc005_tc009_notMarkedDays() throws Exception {
        Reporter.log("▶ SC005_TC009 — Not marked days", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        reportPage.openTab("p-children");
        for (Map<String, String> r : reportPage.getVisibleChildRows())
            Assert.assertEquals(num(r.get("Expected")), num(r.get("Present")) + num(r.get("Absent")) + num(r.get("Not marked")),
                    "❌ Count as absent: Expected ≠ P + A + Not marked for " + r.get("Child ID"));
        reportPage.openTab("p-report");
        reportPage.selectNotMarked("Leave out (not an absence)");
        Assert.assertTrue(reportPage.getCurrentUrl().contains("not_marked=skip"), "❌ URL: " + reportPage.getCurrentUrl());
        Assert.assertEquals(reportPage.getSelectedNotMarked(), "Leave out (not an absence)");
        reportPage.openTab("p-children");
        int nmDays = 0;
        for (Map<String, String> r : reportPage.getVisibleChildRows()) {
            nmDays += num(r.get("Not marked"));
            Assert.assertEquals(num(r.get("Expected")), num(r.get("Present")) + num(r.get("Absent")),
                    "❌ Leave out: Expected ≠ P + A for " + r.get("Child ID"));
        }
        Reporter.log("   Not marked days in range: " + nmDays + (nmDays == 0 ? " (no data to show the difference)" : ""), true);
        Reporter.log("✅ SC005_TC009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC006 — Children tab
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 50, description = "SC006_TC001 — Children tab columns and per-child arithmetic")
    public void sc006_tc001_childColumnsAndArithmetic() throws Exception {
        Reporter.log("▶ SC006_TC001 — Children columns", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        reportPage.openTab("p-children");
        Assert.assertEquals(reportPage.getChildTableHeaders(), CHILD_COLUMNS);
        for (Map<String, String> r : reportPage.getVisibleChildRows()) {
            String id = r.get("Child ID");
            int wd = num(r.get("Working days")), pz = num(r.get("Paused / leave")), exp = num(r.get("Expected"));
            int p = num(r.get("Present")), nr = num(r.get("No record"));
            Assert.assertEquals(wd, pz + exp + nr, "❌ " + id + ": Working days ≠ Paused + Expected + No record");
            if (exp == 0) {
                Assert.assertEquals(r.get("Presence %"), "–", "❌ " + id);
                Assert.assertEquals(r.get("Result"), "No days", "❌ " + id);
            } else {
                Assert.assertEquals(r.get("Presence %"), String.format(Locale.US, "%.1f%% %d ÷ %d", 100.0 * p / exp, p, exp), "❌ " + id + ": Presence %");
            }
            Assert.assertTrue(r.get("Child").matches(".+ (Full period|Joined \\d{1,2} [A-Z][a-z]{2}|Left \\d{1,2} [A-Z][a-z]{2}).*"), "❌ " + id + " tag: " + r.get("Child"));
            if (pz > 0) Assert.assertTrue(r.get("Child").contains("Paused " + pz + " d"), "❌ " + id + ": paused days but no 'Paused " + pz + " d' tag: " + r.get("Child"));
            else Assert.assertFalse(r.get("Child").contains("Paused"), "❌ " + id + ": 'Paused' tag without paused days");
        }
        Reporter.log("   " + reportPage.getVisibleChildRows().size() + " children checked", true);
        Map<String, String> labels = reportPage.getResultLabelClasses();
        Reporter.log("   Result labels: " + labels, true);
        Assert.assertEquals(labels.get("Meets X%"), "label label-success", "❌ Meets label colour (green)");
        Assert.assertEquals(labels.get("Below X%"), "label label-danger", "❌ Below label colour (red)");
        Assert.assertEquals(labels.get("No days"), "label label-default", "❌ No days label colour (grey)");
        Reporter.log("✅ SC006_TC001 PASSED", true);
    }

    @Test(priority = 51, description = "SC006_TC002 — Search")
    public void sc006_tc002_search() throws Exception {
        Reporter.log("▶ SC006_TC002 — Search", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        reportPage.openTab("p-children");
        List<Map<String, String>> all = reportPage.getVisibleChildRows();
        int total = all.size();
        Map<String, String> c = all.get(0);
        String name = c.get("Child").replaceAll(" (Full period|Joined .*|Left .*)$", "");
        reportPage.searchChildren(name.toLowerCase());
        Assert.assertTrue(reportPage.getVisibleChildIds().contains(c.get("Child ID")), "❌ Name search (lower case)");
        Assert.assertEquals(reportPage.getChildrenTabBadge(), reportPage.getVisibleChildIds().size() + " of " + total, "❌ Badge during search");
        reportPage.searchChildren(c.get("Child ID"));
        Assert.assertEquals(reportPage.getVisibleChildIds(), Arrays.asList(c.get("Child ID")), "❌ Child ID search");
        reportPage.searchChildren(c.get("Teacher"));
        for (Map<String, String> r : reportPage.getVisibleChildRows()) Assert.assertEquals(r.get("Teacher"), c.get("Teacher"), "❌ Teacher search");
        reportPage.searchChildren("zzzzqa");
        Assert.assertTrue(reportPage.getVisibleChildIds().isEmpty());
        Assert.assertEquals(reportPage.getChildTableBodyText(), "No children match the selected filters.");
        Assert.assertEquals(reportPage.getChildrenTabBadge(), "0 of " + total);
        reportPage.searchChildren("");
        Assert.assertEquals(reportPage.getVisibleChildIds().size(), total, "❌ Clearing search");
        Reporter.log("   '" + name + "', " + c.get("Child ID") + ", " + c.get("Teacher") + " found; no-match OK", true);
        Reporter.log("✅ SC006_TC002 PASSED", true);
    }

    @Test(priority = 52, description = "SC006_TC003 — Sort by")
    public void sc006_tc003_sort() throws Exception {
        Reporter.log("▶ SC006_TC003 — Sort", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        reportPage.openTab("p-children");
        Assert.assertEquals(reportPage.getSortOptions(), Arrays.asList("Presence % low to high", "Presence % high to low", "Child A to Z", "Teacher"));
        for (String opt : reportPage.getSortOptions()) {
            reportPage.sortChildren(opt);
            List<Map<String, String>> rows = reportPage.getVisibleChildRows();
            for (int i = 1; i < rows.size(); i++) {
                Map<String, String> a = rows.get(i - 1), b = rows.get(i);
                double pa = pct(a.get("Presence %")), pb = pct(b.get("Presence %"));
                switch (opt) {
                    case "Presence % low to high":
                        if (pa >= 0 && pb >= 0) Assert.assertTrue(pa <= pb, "❌ " + opt + ": " + pa + " before " + pb);
                        if (pa < 0) Assert.assertTrue(pb < 0, "❌ " + opt + ": '–' rows should be last");
                        break;
                    case "Presence % high to low":
                        if (pa >= 0 && pb >= 0) Assert.assertTrue(pa >= pb, "❌ " + opt + ": " + pa + " before " + pb);
                        if (pa < 0) Assert.assertTrue(pb < 0, "❌ " + opt + ": '–' rows should be last");
                        break;
                    case "Child A to Z":
                        Assert.assertTrue(a.get("Child").compareToIgnoreCase(b.get("Child")) <= 0, "❌ " + opt + ": " + a.get("Child") + " before " + b.get("Child"));
                        break;
                    default:
                        Assert.assertTrue(a.get("Teacher").compareToIgnoreCase(b.get("Teacher")) <= 0 || "Unassigned".equals(a.get("Teacher")) || "Unassigned".equals(b.get("Teacher")),
                                "❌ " + opt + ": " + a.get("Teacher") + " before " + b.get("Teacher"));
                }
            }
            Reporter.log("   " + opt + " OK (" + rows.get(0).get("Child") + " first)", true);
        }
        Reporter.log("✅ SC006_TC003 PASSED", true);
    }

    @Test(priority = 53, description = "SC006_TC004 — Row click shows the day-by-day detail")
    public void sc006_tc004_rowDetail() throws Exception {
        Reporter.log("▶ SC006_TC004 — Row detail", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        reportPage.openTab("p-children");
        Map<String, String> c = null;
        for (Map<String, String> r : reportPage.getVisibleChildRows()) if (num(r.get("Present")) > 0 && num(r.get("Absent")) > 0) { c = r; break; }
        if (c == null) throw new SkipException("No child with present and absent days");
        String id = c.get("Child ID");
        reportPage.clickChildRow(id);
        String detail = reportPage.getChildDetailText(id);
        Reporter.log("   " + id + ": " + detail.replace("\n", " | ").substring(0, Math.min(400, detail.length())), true);
        Assert.assertTrue(detail.contains("Working days in that window\n" + c.get("Working days")), "❌ Working days in detail");
        Assert.assertTrue(detail.contains("Present dates (" + num(c.get("Present")) + ")"), "❌ 'Present dates (n)'");
        Assert.assertTrue(detail.contains("Absent dates (" + num(c.get("Absent")) + ")"), "❌ 'Absent dates (n)'");
        Assert.assertTrue(detail.contains("Presence %\n" + c.get("Presence %").replaceAll("^([\\d.]+%) (\\d+) ÷ (\\d+)$", "$2 ÷ $3 = $1")), "❌ Presence % line");
        Reporter.log("✅ SC006_TC004 PASSED", true);
    }

    @Test(priority = 54, description = "SC006_TC005 — Child ID opens Child Information")
    public void sc006_tc005_childInfoPopup() throws Exception {
        Reporter.log("▶ SC006_TC005 — Child Information popup", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        reportPage.openTab("p-children");
        Map<String, String> c = reportPage.getVisibleChildRows().get(0);
        String id = c.get("Child ID");
        reportPage.clickChildIdLink(id);
        String popup = reportPage.getChildInfoPopupText();
        Reporter.log("   " + popup.substring(0, Math.min(200, popup.length())), true);
        Assert.assertTrue(popup.contains("#" + id), "❌ Popup is not for child " + id);
        Assert.assertTrue(popup.contains("Basic Information") && popup.contains(CENTRE), "❌ Popup content");
        reportPage.closePopups();
        Reporter.log("✅ SC006_TC005 PASSED", true);
    }

    @Test(priority = 55, description = "SC006_TC006 — Download CSV")
    public void sc006_tc006_downloadCsv() throws Exception {
        Reporter.log("▶ SC006_TC006 — Download CSV", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        int kids = reportPage.getKpiValue("Children in the period");
        reportPage.openTab("p-children");
        // ✅ User guide: the CSV ignores the Search box (follows Teacher / Room only)
        reportPage.searchChildren(reportPage.getVisibleChildIds().get(0));
        Assert.assertEquals(reportPage.getVisibleChildIds().size(), 1, "❌ Search did not narrow the list");
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        reportPage.clickDownloadCsv();
        File csv = null;
        for (int i = 0; i < 20 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.startsWith("Teacher_Payout_Report_") && n.endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified())) csv = f;
            if (csv == null) Thread.sleep(500);
        }
        Assert.assertNotNull(csv, "❌ No Teacher_Payout_Report_*.csv downloaded to " + downloads);
        String expectedName = "Teacher_Payout_Report_" + CENTRE.replaceAll("[^A-Za-z0-9]+", "_") + "_" + OCT_FROM + "_to_" + OCT_TO;
        Reporter.log("   File: " + csv.getName(), true);
        Assert.assertTrue(csv.getName().startsWith(expectedName), "❌ File name, expected " + expectedName + "*.csv");
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(csv), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) if (!l.trim().isEmpty()) lines.add(l);
        }
        String header = lines.get(0).replace("﻿", "");
        Reporter.log("   Header: " + header + " | data rows: " + (lines.size() - 1), true);
        Assert.assertEquals(header, "\"Child\",\"Child ID\",\"Teacher\",\"Room\",\"Attrition date\",\"Attrition reason\",\"Programme\",\"Counted from\",\"Counted to\","
                + "\"Declared holidays\",\"Working days\",\"Paused / leave\",\"Expected\",\"Present\",\"Absent\",\"Not marked\",\"No record\",\"Presence %\",\"Result\"");
        Assert.assertEquals(lines.size() - 1, kids, "❌ CSV rows ≠ all children (the CSV should ignore the search)");
        Reporter.log("✅ SC006_TC006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC007 — Charts
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 60, description = "SC007_TC001 — Charts tab")
    public void sc007_tc001_charts() throws Exception {
        Reporter.log("▶ SC007_TC001 — Charts", true);
        open(customQuery(LQ_FROM, LQ_TO, null));
        reportPage.openTab("p-charts");
        Thread.sleep(1500);
        String titles = reportPage.getChartPanelTitles();
        Reporter.log("   " + titles, true);
        Assert.assertEquals(titles, "Weekly presence trend all children in view | Payout status by teacher children per teacher | Presence % by child dashed line = bonus threshold");
        for (String id : new String[]{"chTrend", "chTeacher", "chKids"})
            Assert.assertTrue(reportPage.isChartRendered(id), "❌ Chart not rendered: " + id);
        Reporter.log("✅ SC007_TC001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC008 — How it is calculated
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 70, description = "SC008_TC001 — Rule and steps")
    public void sc008_tc001_ruleAndSteps() throws Exception {
        Reporter.log("▶ SC008_TC001 — Rule and steps", true);
        open(DEFAULT_QUERY);
        reportPage.openTab("p-how");
        String how = reportPage.getHowTabText();
        for (String s : Arrays.asList("The rule in one line", "Presence % = Present days ÷ Expected days × 100", "Steps applied to every child",
                "Who is counted. Regular admissions only. Trial admissions are not counted.", "Period.", "Working days.", "Joining.",
                "Attrition.", "Excluded days.", "Expected.", "Child result.", "Payout.", "If a child joins in the middle of the period",
                "Worked examples from this centre"))
            Assert.assertTrue(how.contains(s), "❌ Missing text: " + s);
        Reporter.log("✅ SC008_TC001 PASSED", true);
    }

    @Test(priority = 71, description = "SC008_TC002 — Joining-date calculator")
    public void sc008_tc002_calculator() throws Exception {
        Reporter.log("▶ SC008_TC002 — Calculator", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        int wd = reportPage.getEquationValue("working days");
        reportPage.openTab("p-how");
        Assert.assertEquals(reportPage.getCalculatorJoiningDate(), OCT_FROM, "❌ Joining date default");
        int[][] cases = {{2, 0}, {0, 0}, {5, 4}};
        for (int[] c : cases) {
            reportPage.setCalculatorInputs(String.valueOf(c[0]), String.valueOf(c[1]));
            Map<String, String> out = reportPage.getCalculatorResult();
            Reporter.log("   Absent " + c[0] + ", paused " + c[1] + " → " + out, true);
            int expected = wd - c[1], present = expected - c[0];
            Assert.assertEquals(out.get("Working days in window"), String.valueOf(wd));
            Assert.assertEquals(out.get("= Expected days"), String.valueOf(expected));
            Assert.assertEquals(out.get("Present (Expected − Absent)"), String.valueOf(present));
            Assert.assertEquals(out.get("Presence %"), String.format(Locale.US, "%d ÷ %d = %.1f%%", present, expected, 100.0 * present / expected));
        }
        Reporter.log("✅ SC008_TC002 PASSED", true);
    }

    @Test(priority = 72, description = "SC008_TC003 — Calculator with absent days above working days / negative (Q4)")
    public void sc008_tc003_calculatorEdgeValues() throws Exception {
        Reporter.log("▶ SC008_TC003 — Calculator edge values (informational, QueryLogs Q4)", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        reportPage.openTab("p-how");
        for (String abs : new String[]{"25", "-1"}) {
            reportPage.setCalculatorInputs(abs, "0");
            Map<String, String> out = reportPage.getCalculatorResult();
            Reporter.log("   Absent " + abs + " → Present " + out.get("Present (Expected − Absent)") + ", " + out.get("Presence %"), true);
            Assert.assertFalse(out.get("Present (Expected − Absent)").startsWith("-"), "❌ Negative Present shown");
        }
        Reporter.log("✅ SC008_TC003 PASSED", true);
    }

    @Test(priority = 73, description = "SC008_TC004 — Worked example matches the child's row")
    public void sc008_tc004_workedExample() throws Exception {
        Reporter.log("▶ SC008_TC004 — Worked example", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        reportPage.openTab("p-how");
        String ex = reportPage.getWorkedExamplesText();
        Reporter.log("   " + ex.replace("\n", " | "), true);
        reportPage.openTab("p-children");
        Map<String, String> row = null;
        for (Map<String, String> r : reportPage.getVisibleChildRows()) {
            String name = r.get("Child").replaceAll(" (Full period|Joined .*|Left .*)$", "");
            if (ex.startsWith(name + " ")) { row = r; break; }
        }
        Assert.assertNotNull(row, "❌ Worked example child not found in the Children tab");
        Assert.assertTrue(ex.contains("Room (today's room)\n" + row.get("Room")), "❌ Room");
        Assert.assertTrue(ex.contains("Working days in that window\n" + row.get("Working days")), "❌ Working days");
        Assert.assertTrue(ex.contains("Present\n" + row.get("Present")), "❌ Present");
        Assert.assertTrue(ex.contains("No attendance row (left out, see data check)\n" + row.get("No record")), "❌ No record");
        Assert.assertTrue(ex.contains("Expected days (Present + Absent + Not marked)\n" + row.get("Expected")), "❌ Expected");
        Reporter.log("✅ SC008_TC004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC009 — Other COCO centres
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 80, description = "SC009_TC001 — Every COCO centre loads without an error")
    public void sc009_tc001_otherCentres() throws Exception {
        Reporter.log("▶ SC009_TC001 — Other COCO centres", true);
        open(customQuery(OCT_FROM, OCT_TO, null));
        List<String> centres = reportPage.getCentreOptions();
        List<String> failed = new ArrayList<>();
        for (String centre : centres.subList(1, centres.size())) {
            ensureBrowserAlive();
            try {
                open(customQuery(OCT_FROM, OCT_TO, null));
            } catch (org.openqa.selenium.WebDriverException e) {
                ensureBrowserAlive();
                open(customQuery(OCT_FROM, OCT_TO, null));
            }
            reportPage.selectCentre(centre);
            reportPage.clickGo();
            String err = reportPage.getErrorAlertText();
            boolean ok = err.isEmpty() && reportPage.isReportShown() && reportPage.getSubtitle().contains(", " + centre + ", ");
            Reporter.log("   " + (ok ? "✅ " : "❌ ") + centre + ": " + (ok ? reportPage.getKpiValue("Children in the period") + " children" : err), true);
            if (!ok) failed.add(centre + " (" + err + ")");
        }
        Assert.assertTrue(failed.isEmpty(), "❌ Centres not loading: " + failed);
        Reporter.log("✅ SC009_TC001 PASSED", true);
    }
}
