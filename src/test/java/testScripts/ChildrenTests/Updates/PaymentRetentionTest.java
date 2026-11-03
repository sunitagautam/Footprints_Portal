package testScripts.ChildrenTests.Updates;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Children.Updates.PaymentRetentionPage;
import pages.Navigations;
import utils.BaseTest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// ═══════════════════════════════════════════════════════════════
// PAYMENT RETENTION (Children → Updates → Payment Retention, /payment_retention_list), SDA-13426
// Source: TC_Payment_Retention.xlsx (sheet "Payment Retention"). Phase 1 = screen + popups (see CLAUDE.md).
// Runs as Rakesh (user, 2026-11-03). Cases live in Oct 2026, so lists are opened with from_date=2026-01-01
// (-DprFromDate=yyyy-MM-dd to change).
//
// The save tests log real outcomes on Due / Current cases whose Last outcome is blank (user OK, 2026-11-03):
// one outcome per case, so each run uses up those cases. They SKIP when no such case is left.
// The cron/API cases (SC005–SC012) are Phase 2.
// ═══════════════════════════════════════════════════════════════
public class PaymentRetentionTest extends BaseTest {

    private static final String FROM = System.getProperty("prFromDate", "2026-01-01");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final List<String> STATUS_OPTIONS = Arrays.asList(
            "due_today=Due / Current", "upcoming=Upcoming", "escalated=With Support", "closed=Closed");
    private static final Set<String> OUTCOMES = new HashSet<>(Arrays.asList("", "Resolved", "Unresolved", "No Answer", "No action"));
    private static final String CONFIRM_TEXT = "Save this call outcome? Only one outcome can be logged per task.";
    private static final String SAVED_TEXT = "Call outcome saved.";

    Navigations navigations;
    PaymentRetentionPage page;

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
    private void open(String filter) throws InterruptedException {
        open(filter, "");
    }

    private void open(String filter, String extra) throws InterruptedException {
        navigations.goToPaymentRetention("filter=" + filter + "&from_date=" + FROM + "&to_date=" + LocalDate.now() + extra);
        page = new PaymentRetentionPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ Payment Retention list did not load");
    }

    private static LocalDate date(String dmy) {
        return LocalDate.parse(dmy.trim(), DMY);
    }

    private List<Map<String, String>> rowsOrSkip(String filter) throws InterruptedException {
        open(filter);
        List<Map<String, String>> rows = page.getRows();
        if (rows.isEmpty()) throw new SkipException("No cases on the '" + filter + "' tab (from " + FROM + ")");
        return rows;
    }

    // ✅ Due cases that still offer Log Call and have no user outcome yet — Last outcome blank, or "No action"
    //    (a system row from an earlier step). Save tests use these.
    private List<Map<String, String>> freshDueCases() throws InterruptedException {
        open("due_today");
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : page.getRows())
            if (!r.get("logCallHref").isEmpty() && (r.get("lastOutcome").isEmpty() || r.get("lastOutcome").equals("No action"))) out.add(r);
        Reporter.log("   Fresh due cases (Log Call, no user outcome): " + out.size(), true);
        return out;
    }

    private static String taskId(Map<String, String> r) {
        return r.get("historyTaskId");
    }

    private Map<String, String> findRow(String taskId) {
        for (Map<String, String> r : page.getRows()) if (taskId.equals(r.get("historyTaskId"))) return r;
        return null;
    }

    // ✅ Native confirm after Save — returns its text; accept or dismiss
    private String answerConfirm(boolean accept) {
        Alert a = new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.alertIsPresent());
        String t = a.getText();
        if (accept) a.accept();
        else a.dismiss();
        return t;
    }

    // ✅ Fills and saves one outcome on an open Log Call popup; returns the message shown
    private String saveOutcome(String callStatus, String outcome, String callWith, String remarks) {
        page.chooseRadio("pr_call_status", callStatus);
        if (outcome != null) page.chooseRadio("pr_received_action", outcome);
        page.chooseRadio("pr_call_with", callWith);
        if (remarks != null) page.setRemarks(remarks);
        page.clickSave();
        String confirm = answerConfirm(true);
        Assert.assertEquals(confirm, CONFIRM_TEXT, "❌ Confirm text");
        return page.waitForLogCallMessage(20);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC001 — Access & navigation
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC001_TC001 — Payment Retention list opens from the menu")
    public void sc001_tc001_listOpensFromMenu() throws InterruptedException {
        Reporter.log("▶ SC001_TC001 — Payment Retention list opens from the menu", true);
        navigations.goToPaymentRetention(null);
        page = new PaymentRetentionPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ List did not load");
        Assert.assertEquals(page.getMenuPath(), "Children > Updates > Payment Retention", "❌ Menu path");
        Assert.assertEquals(page.getHeading(), "Payment Retention", "❌ Heading");
        Assert.assertEquals(page.getStatusOptions(), STATUS_OPTIONS, "❌ Status options");
        Assert.assertEquals(page.getSelectedStatus(), "due_today", "❌ Default status");
        Assert.assertTrue(page.getCountText().matches("\\d+ Due / Current"), "❌ Count text: " + page.getCountText());
        Assert.assertEquals(page.getFieldValue("from_date"), LocalDate.now().withDayOfMonth(1).toString(), "❌ Default From date");
        Assert.assertEquals(page.getFieldValue("to_date"), LocalDate.now().toString(), "❌ Default To date");
        Assert.assertEquals(page.getSearchPlaceholder(), "Search child, centre…", "❌ Search placeholder");
        String note = page.getNoteText();
        for (String s : new String[]{"Due / Current:", "Upcoming:", "With Support:", "Closed tab: closed date", "one outcome per step"})
            Assert.assertTrue(note.contains(s), "❌ Help note missing '" + s + "': " + note);
        Reporter.log("   Count: " + page.getCountText() + " | Columns: " + page.getColumnHeaders() + " | Note: " + note, true);
        Reporter.log("✅ SC001_TC001 passed", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC002 — List columns, tabs, filters, actions
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC002_TC001 — List columns display correct data")
    public void sc002_tc001_columns() throws InterruptedException {
        Reporter.log("▶ SC002_TC001 — List columns display correct data", true);
        List<Map<String, String>> rows = rowsOrSkip("due_today");
        Reporter.log("   Headers: " + page.getColumnHeaders(), true);
        for (String h : new String[]{"CHILD", "CENTRE", "ATTENDANCE (DAY 1–5)", "LAST OUTCOME", "STATUS", "DUE", "ACTION"})
            Assert.assertTrue(page.getColumnHeaders().stream().anyMatch(x -> x.equalsIgnoreCase(h)), "❌ Missing column " + h);
        for (Map<String, String> r : rows) {
            Reporter.log("   " + r.get("child") + " #" + r.get("childId") + " | " + r.get("centre") + " | " + r.get("attendance") + " | "
                    + r.get("amount") + " (" + r.get("invoiceDate") + ") | " + r.get("lastOutcome") + " | " + r.get("status") + " | "
                    + r.get("due") + " | " + r.get("actions"), true);
            Assert.assertTrue(r.get("childId").matches("\\d+"), "❌ Child ID under the name: " + r);
            Assert.assertFalse(r.get("centre").isEmpty(), "❌ Centre empty: " + r);
            Assert.assertTrue(r.get("attendance").matches("\\d+ present · \\d+ absent"), "❌ Attendance format: " + r.get("attendance"));
            Assert.assertTrue(r.get("amount").matches("Rs [\\d,]+\\.\\d{2}"), "❌ Amount format: " + r.get("amount"));
            date(r.get("invoiceDate"));
            Assert.assertTrue(OUTCOMES.contains(r.get("lastOutcome")), "❌ Last outcome value: " + r.get("lastOutcome"));
            Assert.assertTrue(r.get("status").equals("Open (CD)") || r.get("status").equals("With Support"), "❌ Status: " + r.get("status"));
            Assert.assertFalse(r.get("rowText").matches(".*\\b(PI|P\\d+|[A-Z])/\\d+.*"), "❌ Invoice number shown on the row: " + r.get("rowText"));
        }
        Reporter.log("✅ SC002_TC001 passed (" + rows.size() + " rows)", true);
    }

    @Test(priority = 3, description = "SC002_TC002 — Due / Current lists cases due today or earlier; Upcoming lists later ones")
    public void sc002_tc002_dueVsUpcoming() throws InterruptedException {
        Reporter.log("▶ SC002_TC002 — Due / Current vs Upcoming", true);
        LocalDate today = LocalDate.now();
        open("due_today");
        for (Map<String, String> r : page.getRows())
            Assert.assertFalse(date(r.get("due")).isAfter(today), "❌ Due tab lists a future case: " + r);
        Reporter.log("   Due / Current: " + page.getRows().size(), true);
        open("upcoming");
        List<Map<String, String>> up = page.getRows();
        for (Map<String, String> r : up) {
            Assert.assertTrue(date(r.get("due")).isAfter(today), "❌ Upcoming lists a case due today or earlier: " + r);
            Assert.assertTrue(r.get("logCallHref").isEmpty(), "❌ Upcoming case offers Log Call: " + r);
        }
        if (up.isEmpty()) Reporter.log("   ℹ Upcoming tab is empty — empty text: '" + page.getEmptyText() + "'", true);
        else Reporter.log("   Upcoming: " + up.size(), true);
        Reporter.log("✅ SC002_TC002 passed", true);
    }

    @Test(priority = 4, description = "SC002_TC003 — With Support (escalated) and Closed tabs")
    public void sc002_tc003_escalatedAndClosed() throws InterruptedException {
        Reporter.log("▶ SC002_TC003 — With Support and Closed tabs", true);
        open("escalated");
        List<Map<String, String>> esc = page.getRows();
        for (Map<String, String> r : esc) {
            Reporter.log("   With Support: " + r.get("child") + " | " + r.get("status") + " | " + r.get("due") + " | " + r.get("actions"), true);
            Assert.assertEquals(r.get("status"), "With Support", "❌ Escalated status");
        }
        open("closed");
        List<Map<String, String>> closed = page.getRows();
        for (Map<String, String> r : closed) {
            Reporter.log("   Closed: " + r.get("child") + " | " + r.get("status") + " | " + r.get("due") + " | " + r.get("actions"), true);
            Assert.assertFalse(r.get("status").startsWith("Open") || r.get("status").equals("With Support"), "❌ Closed tab shows an open case: " + r);
            Assert.assertEquals(r.get("actions"), "History", "❌ Closed case actions: " + r.get("actions"));
            date(r.get("due"));
        }
        if (esc.isEmpty() && closed.isEmpty()) throw new SkipException("No With Support or Closed cases");
        Reporter.log("✅ SC002_TC003 passed (With Support " + esc.size() + ", Closed " + closed.size() + ")", true);
    }

    @Test(priority = 5, description = "SC002_TC004 — Centre, Child ID and date range filters")
    public void sc002_tc004_filters() throws InterruptedException {
        Reporter.log("▶ SC002_TC004 — Centre, Child ID and date range filters", true);
        List<Map<String, String>> all = rowsOrSkip("due_today");
        Map<String, String> r0 = all.get(0);

        // Centre
        String v = page.selectCentreByName(r0.get("centre"));
        Assert.assertFalse(v.isEmpty(), "❌ Centre '" + r0.get("centre") + "' not in the Centre filter");
        page.clickFilter();
        Assert.assertEquals(page.getSelectedCentreText(), r0.get("centre"), "❌ Centre filter not kept after Filter");
        for (Map<String, String> r : page.getRows()) Assert.assertEquals(r.get("centre"), r0.get("centre"), "❌ Centre filter");
        Reporter.log("   Centre '" + r0.get("centre") + "' → " + page.getRows().size() + " rows", true);

        // Child ID — exact
        open("due_today", "&admission_id=" + r0.get("childId"));
        List<Map<String, String>> byId = page.getRows();
        Assert.assertFalse(byId.isEmpty(), "❌ No row for Child ID " + r0.get("childId"));
        for (Map<String, String> r : byId) Assert.assertEquals(r.get("childId"), r0.get("childId"), "❌ Child ID filter");

        // Child ID — partial / non-existing
        String partial = r0.get("childId").substring(0, r0.get("childId").length() - 1);
        open("due_today", "&admission_id=" + partial);
        Assert.assertTrue(page.getRows().isEmpty(), "❌ Partial Child ID " + partial + " matched rows (exact match expected)");
        Assert.assertEquals(page.getEmptyText(), "No cases for this filter.", "❌ Empty text");
        open("due_today", "&admission_id=999999");
        Assert.assertTrue(page.getRows().isEmpty(), "❌ Non-existing Child ID matched rows");

        // From after To — no crash, no rows
        navigations.goToPaymentRetention("filter=due_today&from_date=" + LocalDate.now() + "&to_date=" + LocalDate.now().minusMonths(2));
        page = new PaymentRetentionPage(driver);
        Assert.assertTrue(page.isPageLoaded(), "❌ List did not load with From > To");
        Reporter.log("   From > To → rows " + page.getRows().size() + ", empty text '" + page.getEmptyText() + "'", true);
        Assert.assertTrue(page.getRows().isEmpty(), "❌ From > To still lists cases");

        // Closed tab — filtered on closed date (Due/Closed column)
        open("closed");
        for (Map<String, String> r : page.getRows())
            Assert.assertFalse(date(r.get("due")).isBefore(LocalDate.parse(FROM)), "❌ Closed date before From: " + r);

        // Clear resets
        page.clickClear();
        Assert.assertEquals(page.getSelectedStatus(), "due_today", "❌ Clear: status");
        Assert.assertEquals(page.getFieldValue("admission_id"), "", "❌ Clear: Child ID");
        Assert.assertEquals(page.getFieldValue("from_date"), LocalDate.now().withDayOfMonth(1).toString(), "❌ Clear: From date");
        Reporter.log("✅ SC002_TC004 passed", true);
    }

    @Test(priority = 6, description = "SC002_TC005 — DataTable search box")
    public void sc002_tc005_search() throws InterruptedException {
        Reporter.log("▶ SC002_TC005 — Search box", true);
        List<Map<String, String>> all = rowsOrSkip("due_today");
        String child = all.get(0).get("child");
        page.search(child);
        List<Map<String, String>> found = page.getRows();
        Assert.assertFalse(found.isEmpty(), "❌ Search '" + child + "' found nothing");
        for (Map<String, String> r : found) Assert.assertTrue(r.get("rowText").toLowerCase().contains(child.toLowerCase()), "❌ Search result: " + r);
        page.search("zzqa-no-such-child");
        Assert.assertTrue(page.getRows().isEmpty(), "❌ Search for a missing child still lists rows");
        page.search("");
        Assert.assertEquals(page.getRows().size(), all.size(), "❌ Clearing the search did not bring back all rows");
        Reporter.log("✅ SC002_TC005 passed", true);
    }

    @Test(priority = 7, description = "SC002_TC006 — Action column states")
    public void sc002_tc006_actionStates() throws InterruptedException {
        Reporter.log("▶ SC002_TC006 — Action column states", true);
        open("due_today");
        int logCall = 0, logged = 0;
        for (Map<String, String> r : page.getRows()) {
            Assert.assertTrue(r.get("actions").contains("History"), "❌ History missing: " + r);
            if (!r.get("logCallHref").isEmpty()) {
                logCall++;
                Assert.assertFalse(r.get("actions").contains("Logged"), "❌ Both Log Call and Logged: " + r);
                if (!r.get("lastOutcome").isEmpty())
                    Reporter.log("   ℹ " + r.get("child") + " offers Log Call although Last outcome = " + r.get("lastOutcome")
                            + " (outcome is from an earlier step?)", true);
            } else {
                logged++;
                Assert.assertTrue(r.get("actions").contains("Logged"), "❌ Due case without Log Call should show Logged: " + r);
            }
        }
        Reporter.log("   Due / Current: Log Call " + logCall + ", Logged " + logged, true);
        open("closed");
        for (Map<String, String> r : page.getRows()) Assert.assertEquals(r.get("actions"), "History", "❌ Closed case actions");
        Reporter.log("✅ SC002_TC006 passed", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC003 — Log Payment Call popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC003_TC001 — Log Payment Call popup fields and read-only context")
    public void sc003_tc001_popupFields() throws InterruptedException {
        Reporter.log("▶ SC003_TC001 — Popup fields and context", true);
        open("due_today");
        Map<String, String> r = page.getRows().stream().filter(x -> !x.get("logCallHref").isEmpty()).findFirst()
                .orElseThrow(() -> new SkipException("No due case with Log Call"));
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open");
        Assert.assertEquals(page.getPopupTitle(), ("Log call — " + r.get("child")).replaceAll("\\s+", " "), "❌ Popup title");
        Assert.assertTrue(page.getPopupEyebrow().contains(r.get("centre")) && page.getPopupEyebrow().contains("#" + r.get("childId")), "❌ Eyebrow: " + page.getPopupEyebrow());
        List<String> pills = page.getPopupPills();
        Reporter.log("   Pills: " + pills, true);
        Assert.assertTrue(pills.size() >= 2 && pills.get(0).matches("Day \\d+ — .+"), "❌ Step pill: " + pills);

        Map<String, String> ctx = page.getLogCallContext();
        Reporter.log("   Context: " + ctx, true);
        Assert.assertEquals(new ArrayList<>(ctx.keySet()), Arrays.asList("Attendance (Day 1–5)", "Due", "Invoice date", "Amount due", "Invoice due date"), "❌ Context fields");
        Assert.assertEquals(ctx.get("Attendance (Day 1–5)"), r.get("attendance"), "❌ Attendance vs row");
        Assert.assertEquals(ctx.get("Due"), r.get("due"), "❌ Due vs row");
        Assert.assertEquals(ctx.get("Invoice date"), r.get("invoiceDate"), "❌ Invoice date vs row");
        Assert.assertEquals(ctx.get("Amount due").replace("Rs.", "Rs"), r.get("amount"), "❌ Amount vs row");
        Assert.assertFalse(page.getPopupText().matches(".*\\b(PI|P\\d+)/\\d+.*"), "❌ Invoice number shown in the popup");

        if (pills.get(0).startsWith("Day 5"))
            Assert.assertTrue(page.getLogCallGuidance().startsWith("This call is about " + r.get("child") + "'s attendance only. Don't raise payment."),
                    "❌ Day-5 guidance: " + page.getLogCallGuidance());
        Reporter.log("   Guidance: " + page.getLogCallGuidance(), true);

        Assert.assertEquals(page.getLogCallLabels(), Arrays.asList("Call status *", "Outcome *", "Call with *", "Remarks"), "❌ Field labels");
        Assert.assertEquals(page.getChipLabels("pr_call_status"), Arrays.asList("Answered", "No Answer"), "❌ Call status options");
        Assert.assertEquals(page.getChipLabels("pr_received_action"), Arrays.asList("Resolved", "Unresolved"), "❌ Outcome options");
        Assert.assertEquals(page.getChipLabels("pr_call_with"), Arrays.asList("Father", "Mother", "Guardian", "Other"), "❌ Call with options");
        Assert.assertFalse(page.isOutcomeShown(), "❌ Outcome shown before Answered");
        page.chooseRadio("pr_call_status", "Received");
        Assert.assertTrue(page.isOutcomeShown(), "❌ Outcome not shown after Answered");
        page.chooseRadio("pr_call_status", "Not_Picked");
        Assert.assertFalse(page.isOutcomeShown(), "❌ Outcome still shown after No Answer");
        if (!page.getPopupText().contains("Updated By"))
            Reporter.log("   ℹ No 'Updated By' field in the popup (sheet expects one) — query", true);
        page.clickCancel();
        Reporter.log("✅ SC003_TC001 passed", true);
    }

    @Test(priority = 11, description = "SC003_TC005 — Mandatory field validations")
    public void sc003_tc005_validations() throws InterruptedException {
        Reporter.log("▶ SC003_TC005 — Mandatory field validations", true);
        open("due_today");
        Map<String, String> r = page.getRows().stream().filter(x -> !x.get("logCallHref").isEmpty()).findFirst()
                .orElseThrow(() -> new SkipException("No due case with Log Call"));
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open");
        page.clickSave();
        Assert.assertEquals(page.getShownErrors(), Arrays.asList("Please select the call status."), "❌ Empty save");
        page.chooseRadio("pr_call_status", "Received");
        page.clickSave();
        Assert.assertEquals(page.getShownErrors(), Arrays.asList("Please select the outcome."), "❌ Answered without outcome");
        page.chooseRadio("pr_call_status", "Not_Picked");
        page.clickSave();
        Assert.assertEquals(page.getShownErrors(), Arrays.asList("Please select who you spoke with."), "❌ No Answer without Call with");
        Assert.assertTrue(page.isPopupOpen(), "❌ Popup closed on a validation error");
        Assert.assertEquals(page.getRemarksPlaceholder(), "What was discussed (recommended for Unresolved)", "❌ Remarks placeholder");
        page.clickCancel();
        open("due_today");
        Assert.assertFalse(findRow(taskId(r)).get("logCallHref").isEmpty(), "❌ Something was saved by the validation checks");
        Reporter.log("✅ SC003_TC005 passed", true);
    }

    @Test(priority = 12, description = "SC003_TC006 — Cancel on the confirmation dialog")
    public void sc003_tc006_cancelConfirm() throws InterruptedException {
        Reporter.log("▶ SC003_TC006 — Cancel on the confirmation dialog", true);
        open("due_today");
        Map<String, String> r = page.getRows().stream().filter(x -> !x.get("logCallHref").isEmpty()).findFirst()
                .orElseThrow(() -> new SkipException("No due case with Log Call"));
        String before = r.get("lastOutcome");
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open");
        page.chooseRadio("pr_call_status", "Received");
        page.chooseRadio("pr_received_action", "Closed");
        page.chooseRadio("pr_call_with", "Mother");
        page.clickSave();
        Assert.assertEquals(answerConfirm(false), CONFIRM_TEXT, "❌ Confirm text");
        Thread.sleep(1500);
        Assert.assertTrue(page.isPopupOpen(), "❌ Popup closed after Cancel");
        Assert.assertEquals(page.getLogCallMessage(), "", "❌ A message was shown after Cancel");
        page.clickCancel();
        open("due_today");
        Map<String, String> after = findRow(taskId(r));
        Assert.assertFalse(after.get("logCallHref").isEmpty(), "❌ Log Call gone after Cancel");
        Assert.assertEquals(after.get("lastOutcome"), before, "❌ Last outcome changed after Cancel");
        Reporter.log("✅ SC003_TC006 passed", true);
    }

    @Test(priority = 13, description = "SC003_TC002 — Log Answered – Resolved (saves; step and status unchanged)")
    public void sc003_tc002_logResolved() throws InterruptedException {
        Reporter.log("▶ SC003_TC002 — Log Answered – Resolved", true);
        List<Map<String, String>> fresh = freshDueCases();
        if (fresh.isEmpty()) throw new SkipException("No due case without an outcome left");
        Map<String, String> r = fresh.get(0);
        Reporter.log("   Case: " + r.get("child") + " #" + r.get("childId") + " task " + taskId(r) + " | " + r.get("status"), true);
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open");
        String step = page.getPopupPills().get(0);
        String remarks = "QA auto: parent will send child (" + LocalDate.now() + ")";
        String msg = saveOutcome("Received", "Closed", "Mother", remarks);
        Assert.assertEquals(msg, SAVED_TEXT, "❌ Save message");

        open("due_today");
        Map<String, String> after = findRow(taskId(r));
        Assert.assertNotNull(after, "❌ Case left the Due / Current tab after a Resolved outcome (case must NOT close)");
        Assert.assertEquals(after.get("lastOutcome"), "Resolved", "❌ Last outcome");
        Assert.assertTrue(after.get("logCallHref").isEmpty() && after.get("actions").contains("Logged"), "❌ Action should be Logged: " + after.get("actions"));
        Assert.assertEquals(after.get("status"), r.get("status"), "❌ Status changed");
        Assert.assertTrue(page.openHistory(taskId(r)), "❌ History did not open");
        Assert.assertEquals(page.getPopupPills().get(0), step, "❌ Step changed after the outcome");
        Assert.assertTrue(page.getHistoryRows().stream().anyMatch(h -> h.contains("Answered — Resolved") && h.contains("Mother") && h.contains(remarks)),
                "❌ History row missing: " + page.getHistoryRows());
        page.closePopup();
        Reporter.log("✅ SC003_TC002 passed", true);
    }

    @Test(priority = 14, description = "SC003_TC004 + SC003_TC011 — Log No Answer; remarks with HTML / Hindi saved as text (emoji logged only)")
    public void sc003_tc004_logNoAnswerAndRemarksInput() throws InterruptedException {
        Reporter.log("▶ SC003_TC004 / TC011 — No Answer + remarks input", true);
        List<Map<String, String>> fresh = freshDueCases();
        if (fresh.isEmpty()) throw new SkipException("No due case without an outcome left");
        Map<String, String> r = fresh.get(0);
        Reporter.log("   Case: " + r.get("child") + " #" + r.get("childId") + " task " + taskId(r), true);
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open");
        String remarks = "<b>QA</b> <i>auto</i> नमस्ते 😀 \"quotes\" & <img src=x onerror=alert(1)>";
        String msg = saveOutcome("Not_Picked", null, "Guardian", remarks);
        Assert.assertEquals(msg, SAVED_TEXT, "❌ Save message");

        open("due_today");
        Map<String, String> after = findRow(taskId(r));
        Assert.assertNotNull(after, "❌ Case left the Due / Current tab");
        Assert.assertEquals(after.get("lastOutcome"), "No Answer", "❌ Last outcome");
        Assert.assertTrue(page.openHistory(taskId(r)), "❌ History did not open");
        Thread.sleep(1000);
        try {
            driver.switchTo().alert();
            Assert.fail("❌ Remarks script ran (alert shown) in History");
        } catch (org.openqa.selenium.NoAlertPresentException ok) {
            // expected
        }
        List<String> hist = page.getHistoryRows();
        Reporter.log("   History: " + hist, true);
        // Emoji are stored as "????" (not a bug, user 2026-11-03) — only HTML and Hindi are checked.
        Assert.assertTrue(hist.stream().anyMatch(h -> h.contains("No Answer") && h.contains("Guardian") && h.contains("नमस्ते")
                && h.contains("<b>QA</b>")), "❌ Remarks not shown as plain text: " + hist);
        Reporter.log("   Emoji shown as: " + (hist.stream().anyMatch(h -> h.contains("😀")) ? "😀" : "replaced (expected, e.g. ????)"), true);
        page.closePopup();
        Reporter.log("✅ SC003_TC004 / TC011 passed", true);
    }

    @Test(priority = 15, description = "SC003_TC007 + SC003_TC003 — One log per task: second window rejected (Unresolved saved first)")
    public void sc003_tc007_duplicateRejected() throws InterruptedException {
        Reporter.log("▶ SC003_TC007 / TC003 — Duplicate outcome from a second window", true);
        List<Map<String, String>> fresh = freshDueCases();
        if (fresh.isEmpty()) throw new SkipException("No due case without an outcome left");
        Map<String, String> r = fresh.get(0);
        Reporter.log("   Case: " + r.get("child") + " #" + r.get("childId") + " task " + taskId(r), true);
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open (window 1)");
        String first = driver.getWindowHandle();

        driver.switchTo().newWindow(WindowType.TAB);
        open("due_today");
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open (window 2)");
        String msg2 = saveOutcome("Received", "Unresolved", "Father", "QA auto: payment next week");
        Assert.assertEquals(msg2, SAVED_TEXT, "❌ Window 2 save message");
        driver.close();
        driver.switchTo().window(first);
        page = new PaymentRetentionPage(driver);

        String msg1 = saveOutcome("Not_Picked", null, "Father", "QA auto: second window");
        Reporter.log("   Window 1 message: " + msg1, true);
        Assert.assertNotEquals(msg1, SAVED_TEXT, "❌ Second outcome for the same task was saved");
        Assert.assertTrue(msg1.toLowerCase().contains("already"), "❌ Expected an 'already logged / updated' message, got: " + msg1);

        open("due_today");
        Map<String, String> after = findRow(taskId(r));
        Assert.assertEquals(after.get("lastOutcome"), "Unresolved", "❌ Last outcome should stay Unresolved");
        Assert.assertTrue(page.openHistory(taskId(r)), "❌ History did not open");
        long n = page.getHistoryRows().stream().filter(h -> h.contains("QA auto: second window")).count();
        Assert.assertEquals(n, 0, "❌ The rejected outcome was written to History");
        page.closePopup();
        Reporter.log("✅ SC003_TC007 / TC003 passed", true);
    }

    @Test(priority = 16, description = "SC003_TC007 — Second outcome on a step that already has one is saved and added to History")
    public void sc003_tc007b_logCallOfferedAfterOutcome() throws InterruptedException {
        Reporter.log("▶ SC003_TC007b — Log Call still offered after an outcome", true);
        open("due_today");
        Map<String, String> r = page.getRows().stream().filter(x -> !x.get("logCallHref").isEmpty()
                        && OUTCOMES.contains(x.get("lastOutcome")) && !x.get("lastOutcome").isEmpty() && !x.get("lastOutcome").equals("No action"))
                .findFirst().orElseThrow(() -> new SkipException("No due case offers Log Call after a user outcome"));
        Assert.assertTrue(page.openHistory(taskId(r)), "❌ History did not open");
        String step = page.getPopupPills().get(0);
        List<String> before = page.getHistoryRows();
        page.closePopup();
        Reporter.log("   Case: " + r.get("child") + " #" + r.get("childId") + " task " + taskId(r) + " | Log Call " + r.get("logCallHref")
                + " | Last outcome " + r.get("lastOutcome") + " | step " + step + " | History " + before, true);
        Assert.assertTrue(page.openLogCall(taskId(r)), "❌ Log Call popup did not open");
        String msg = saveOutcome("Not_Picked", null, "Other", "QA auto: second outcome check");
        Reporter.log("   Message: " + msg, true);
        // A second outcome on the same step is accepted (not a bug, user 2026-11-03).
        Assert.assertEquals(msg, SAVED_TEXT, "❌ Second outcome save message");
        open("due_today");
        Assert.assertTrue(page.openHistory(taskId(r)), "❌ History did not open after the second outcome");
        List<String> after = page.getHistoryRows();
        page.closePopup();
        Reporter.log("   History after: " + after, true);
        Assert.assertEquals(after.size(), before.size() + 1, "❌ The second outcome did not add one History row");
        Assert.assertTrue(after.stream().anyMatch(h -> h.contains("No Answer") && h.contains("Other") && h.contains("QA auto: second outcome check")),
                "❌ Second outcome missing from History: " + after);
        Reporter.log("✅ SC003_TC007b passed", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC004 — Call History popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC004_TC001 — History shows all call logs (open and closed cases)")
    public void sc004_tc001_history() throws InterruptedException {
        Reporter.log("▶ SC004_TC001 — History popup", true);
        int checked = 0;
        for (String tab : new String[]{"escalated", "closed"}) {
            open(tab);
            List<Map<String, String>> rows = page.getRows();
            if (rows.isEmpty()) continue;
            Map<String, String> r = rows.get(0);
            Assert.assertTrue(page.openHistory(taskId(r)), "❌ History did not open (" + tab + ")");
            Assert.assertEquals(page.getPopupTitle(), ("Call history — " + r.get("child")).replaceAll("\\s+", " "), "❌ History title");
            Assert.assertTrue(page.getPopupEyebrow().contains("#" + r.get("childId")), "❌ Eyebrow: " + page.getPopupEyebrow());
            Assert.assertEquals(page.getHistoryHeaders(), Arrays.asList("Step", "Outcome", "Call with", "Remarks / note", "Logged at"), "❌ History columns");
            List<String> hist = page.getHistoryRows();
            Reporter.log("   " + tab + " — " + r.get("child") + " [" + page.getPopupPills() + "]: " + hist, true);
            Assert.assertFalse(hist.isEmpty(), "❌ History is empty for a " + tab + " case");
            for (String h : hist) {
                String[] c = h.split(" \\|\\| ", -1);
                Assert.assertEquals(c.length, 5, "❌ History row cells: " + h);
                Assert.assertTrue(c[4].matches("\\d{2} \\w{3} \\d{4}, \\d{2}:\\d{2} [AP]M"), "❌ Logged at format: " + c[4]);
            }
            if (tab.equals("closed"))
                Assert.assertTrue(hist.stream().anyMatch(h -> h.contains("Auto-closed") || h.contains("Closed")), "❌ Closed case has no close entry: " + hist);
            Assert.assertEquals(page.countHistoryInputs() == 0 || !page.getHistoryAddNoteRowText().isEmpty(), true, "❌ Unexpected input in History");
            page.closePopup();
            checked++;
        }
        if (checked == 0) throw new SkipException("No With Support / Closed case to open History on");
        Reporter.log("✅ SC004_TC001 passed", true);
    }

    @Test(priority = 21, description = "SC004_TC002 — 'Add note' only on 'No action' rows")
    public void sc004_tc002_addNoteOnlyOnNoActionRows() throws InterruptedException {
        Reporter.log("▶ SC004_TC002 — Add note only on No action rows", true);
        int seen = 0;
        for (String tab : new String[]{"due_today", "escalated", "closed"}) {
            open(tab);
            for (Map<String, String> r : page.getRows()) {
                if (!page.openHistory(taskId(r))) continue;
                for (String h : page.getHistoryRows())
                    if (h.contains("No action")) Reporter.log("   No action row (" + r.get("child") + "): " + h, true);
                if (page.countHistoryInputs() > 0) {
                    String row = page.getHistoryAddNoteRowText();
                    Reporter.log("   Add-note row: " + row, true);
                    Assert.assertTrue(row.contains("No action") || row.contains("No outcome logged"), "❌ Add note on a normal row: " + row);
                    seen++;
                }
                page.closePopup();
                if (seen > 0) break;
            }
            if (seen > 0) break;
        }
        if (seen == 0) throw new SkipException("No 'No action' row with Add note found — needs a No_CD_Action case (Phase 2 cron)");
        Reporter.log("✅ SC004_TC002 passed", true);
    }
}
