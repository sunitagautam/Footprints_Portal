package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.CoPayMigrationDataPage;
import pages.Navigations;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ═══════════════════════════════════════════════════════════════
// CO-PAY MIGRATION DATA (Franchise → Corporate Tieups → Co-Pay Migration Data, /copay_migration_data)
// Source: TC_Corporate Tie-ups.xlsx SC_008_TC_001–007, updated to the live screen + new TCs →
// TC_Corporate_Tieups.xlsx, sheet TC_CoPayMigrationData. Runs as Rakesh.
// Saves (user, 2026-11-03): TC_007 clicks Update on child 56670 (Aadvik Chandna) with the popup's pre-filled plans —
// parent 7,399 + corporate 7,400 monthly, yearly 16,000 for both.
// ═══════════════════════════════════════════════════════════════
public class CoPayMigrationDataTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Child Name", "Length", "Company Name", "Breakup", "Payment Date",
            "Monthly Fee", "Existing Breakup (Monthly)", "Updated Breakup (Monthly)", "Update");
    private static final List<String> CSV_COLUMNS = Arrays.asList("Child ID", "Child Name", "Length", "Payment Date", "Company Name",
            "Breakup", "Discount Monthly", "Discount Yearly", "Parent Payer Monthly Fee", "Corporate Payer Monthly Fee",
            "Fee Amount of Center (Monthly)", "Diff Monthly Fee", "Admission Type");
    private static final String UPDATE_CHILD = System.getProperty("copayMigrationChild", "56670");
    private static final String UPDATE_CHILD_CENTER = System.getProperty("copayMigrationCenter", "Sector 1, Greater Noida");
    private static final String EMPTY_CENTER = System.getProperty("copayEmptyCenter", "Sector 122, Noida");
    private static final DateTimeFormatter PAYMENT_DATE = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);

    Navigations navigations;
    CoPayMigrationDataPage migrationPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        reopen();
    }

    private void reopen() throws InterruptedException {
        navigations.goToCoPayMigrationData();
        migrationPage = new CoPayMigrationDataPage(driver);
        Assert.assertTrue(migrationPage.isPageLoaded(), "❌ Co-Pay Migration Data did not load");
    }

    // Plans to set, from the Update link (m = parent monthly, cm = corporate monthly, y = parent yearly, cy = corporate yearly)
    private Map<String, String> planParams(String href) {
        Map<String, String> p = new LinkedHashMap<>();
        for (String kv : href.substring(href.indexOf('?') + 1).replace("&amp;", "&").split("&")) {
            String[] x = kv.split("=", 2);
            p.put(x[0], x.length > 1 ? x[1] : "");
        }
        return p;
    }

    private static String amountOf(String planId) {
        Matcher m = Pattern.compile("-(\\d+)$").matcher(planId);
        return m.find() ? m.group(1) + ".00" : "";
    }

    // "Parent" / "Corporate" plan amount of the Monthly or Yearly Subscription on the child's Account Statement
    private String accountStatementPlan(String body, String subscription, String payer) {
        int s = body.indexOf(subscription + " Subscription");
        int e = subscription.equals("Monthly") ? body.indexOf("Yearly Subscription", s) : body.indexOf("Transactions", s);
        String block = body.substring(s, e < 0 ? body.length() : e);
        Matcher m = Pattern.compile(payer + " Payable Details:\\s*Plan Name :[^\\n]*\\n\\s*Plan Amount :\\s*([^\\n(]+)").matcher(block);
        return m.find() ? m.group(1).trim() : "";
    }

    private String openAccountStatementText(String childId) throws InterruptedException {
        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + childId);
        for (int i = 0; i < 40; i++) {
            String body = String.valueOf(((JavascriptExecutor) driver).executeScript("return document.body.innerText;"));
            if (body.contains("#" + childId + " ") && body.contains("Yearly Subscription")) return body;
            Thread.sleep(500);
        }
        return String.valueOf(((JavascriptExecutor) driver).executeScript("return document.body.innerText;"));
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_008_TC_001 — Default view")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_008_TC_001 — Default view", true);
        List<String> cols = migrationPage.getVisibleColumnHeaders();
        Reporter.log("   Heading: " + migrationPage.getHeading() + " | center " + migrationPage.getSelectedText("center_selection") + " | status "
                + migrationPage.getSelectedText("migration_updated") + " | columns " + cols + " | " + migrationPage.getInfoText()
                + " | per page " + migrationPage.getPageLength(), true);
        Assert.assertEquals(migrationPage.getHeading(), "Co-Pay Migration Data", "❌ Heading");
        Assert.assertTrue(migrationPage.isCenterFilterVisible() && migrationPage.isStatusFilterVisible() && migrationPage.isFilterButtonVisible(), "❌ Filters");
        Assert.assertEquals(migrationPage.getSelectedText("center_selection"), "All Center", "❌ Default center");
        Assert.assertTrue(migrationPage.isSearchVisible() && migrationPage.isDownloadVisible() && migrationPage.isPaginationVisible(), "❌ Search / Download / pagination");
        Assert.assertEquals(cols, COLUMNS, "❌ Columns");
        Assert.assertTrue(migrationPage.getRowCount() > 0, "❌ No rows");
        for (String u : migrationPage.getColumnValues("Update")) Assert.assertTrue(u.startsWith("Update"), "❌ Row without Update button: " + u);
        Reporter.log("✅ SC_008_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Center filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_008_TC_002 — Center filter")
    public void tc002_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_002 — Center filter", true);
        int all = migrationPage.getRowCount();
        migrationPage.selectByText("center_selection", UPDATE_CHILD_CENTER);
        migrationPage.clickFilter();
        List<String> ids = migrationPage.getColumnValues("Child ID");
        Reporter.log("   Center '" + UPDATE_CHILD_CENTER + "': " + ids + " of " + all + " rows", true);
        Assert.assertEquals(migrationPage.getSelectedText("center_selection"), UPDATE_CHILD_CENTER, "❌ Center not kept");
        Assert.assertTrue(ids.contains(UPDATE_CHILD), "❌ " + UPDATE_CHILD + " not listed for its centre");
        Assert.assertTrue(ids.size() < all, "❌ Center filter did not narrow the list");

        reopen();
        migrationPage.selectByText("center_selection", EMPTY_CENTER);
        migrationPage.clickFilter();
        Reporter.log("   " + EMPTY_CENTER + ": " + migrationPage.getRowCount() + " rows " + migrationPage.getColumnValues("Child ID") + " | " + migrationPage.getEmptyTableMessage(), true);
        Assert.assertEquals(migrationPage.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        Reporter.log("✅ SC_008_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_008_TC_003 — Search")
    public void tc003_search() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_003 — Search", true);
        String name = migrationPage.getPageColumnValues("Child Name").get(0);
        migrationPage.searchTable(name);
        List<String> names = migrationPage.getColumnValues("Child Name");
        Reporter.log("   '" + name + "' → " + names, true);
        Assert.assertFalse(names.isEmpty(), "❌ No result");
        for (String n : names) Assert.assertTrue(n.contains(name), "❌ Unrelated row: " + n);

        String company = migrationPage.getPageColumnValues("Company Name").get(0);
        migrationPage.searchTable(company);
        List<String> companies = migrationPage.getColumnValues("Company Name");
        Reporter.log("   '" + company + "' → " + companies.size() + " rows", true);
        for (String c : companies) Assert.assertTrue(c.contains(company), "❌ Unrelated row: " + c);

        migrationPage.searchTable("zzqq-no-such-child");
        Assert.assertEquals(migrationPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_008_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Download
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_008_TC_004 — Download")
    public void tc004_download() throws Exception {
        Reporter.log("▶ SC_008_TC_004 — Download", true);
        int expected = migrationPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        migrationPage.clickDownload();
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
        Assert.assertTrue(csv.getName().startsWith("copay_annual_fee_migration"), "❌ File name " + csv.getName());
        Assert.assertEquals(header, CSV_COLUMNS, "❌ CSV columns");
        Assert.assertEquals(lines.size() - 1, expected, "❌ CSV rows vs grid");
        Assert.assertTrue(lines.stream().anyMatch(l -> l.startsWith("\"" + UPDATE_CHILD + "\"")), "❌ " + UPDATE_CHILD + " not in the CSV");
        Reporter.log("✅ SC_008_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_008_TC_005 — Pagination")
    public void tc005_pagination() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_005 — Pagination", true);
        int rows = migrationPage.getRowCount(), len = migrationPage.getPageLength();
        Reporter.log("   " + migrationPage.getInfoText() + " | per page " + len, true);
        Assert.assertEquals(len, 20, "❌ Rows per page");
        Assert.assertFalse(migrationPage.isPreviousPageEnabled(), "❌ Previous enabled on page 1");
        Assert.assertTrue(migrationPage.getInfoText().contains("Showing 1 to " + Math.min(rows, len) + " of " + rows), "❌ Page 1 info");
        Assert.assertEquals(migrationPage.isNextPageEnabled(), rows > len, "❌ Next state for " + rows + " rows");
        Reporter.log("✅ SC_008_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_006 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_008_TC_006 — Sorting")
    public void tc006_sorting() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_006 — Sorting", true);
        Assert.assertTrue(migrationPage.getHeaderSortClass("Child Name").contains("sorting_asc"), "❌ Default order is not Child Name ascending");
        List<String> names = migrationPage.getColumnValues("Child Name");
        List<String> asc = new ArrayList<>(names);
        asc.sort(String.CASE_INSENSITIVE_ORDER);
        Assert.assertEquals(names, asc, "❌ Child Name not ascending");
        migrationPage.clickColumnHeader("Child Name");
        List<String> desc = new ArrayList<>(asc);
        java.util.Collections.reverse(desc);
        Assert.assertEquals(migrationPage.getColumnValues("Child Name"), desc, "❌ Child Name not descending");

        migrationPage.clickColumnHeader("Payment Date");
        List<LocalDate> dates = new ArrayList<>();
        for (String d : migrationPage.getColumnValues("Payment Date")) dates.add(LocalDate.parse(d, PAYMENT_DATE));
        Reporter.log("   Child Name desc " + desc.subList(0, Math.min(3, desc.size())) + " | Payment Date " + dates, true);
        List<LocalDate> sorted = new ArrayList<>(dates);
        java.util.Collections.sort(sorted);
        List<LocalDate> rev = new ArrayList<>(sorted);
        java.util.Collections.reverse(rev);
        Assert.assertTrue(dates.equals(sorted) || dates.equals(rev), "❌ Payment Date not sorted as dates: " + dates);
        Reporter.log("✅ SC_008_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 (new) — Status filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_008_TC_008 — Status filter")
    public void tc008_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_008 — Status filter", true);
        Assert.assertEquals(migrationPage.getOptions("migration_updated"), Arrays.asList("All", "Pending", "Updated"), "❌ Status options");
        Assert.assertEquals(migrationPage.getSelectedText("migration_updated"), "All", "❌ Default status");
        Assert.assertFalse(migrationPage.isExcludeInhouseVisible(), "❌ Exclude Inhouse should be hidden");
        int all = migrationPage.getRowCount();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String status : new String[]{"Pending", "Updated"}) {
            reopen();
            migrationPage.selectByText("migration_updated", status);
            migrationPage.clickFilter();
            Assert.assertEquals(migrationPage.getSelectedText("migration_updated"), status, "❌ Status not kept");
            List<String> updated = migrationPage.getColumnValues("Updated Breakup (Monthly)");
            counts.put(status, updated.size());
            for (String u : updated)
                Assert.assertEquals(u.isEmpty(), status.equals("Pending"), "❌ " + status + " row with Updated Breakup '" + u + "'");
        }
        Reporter.log("   All " + all + " | " + counts, true);
        Assert.assertEquals(counts.get("Pending") + counts.get("Updated"), all, "❌ Pending + Updated ≠ All");
        Reporter.log("✅ SC_008_TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_009 (new) — Update popup default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC_008_TC_009 — Update popup default view")
    public void tc009_updatePopupDefaultView() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_009 — Update popup default view (" + UPDATE_CHILD + ")", true);
        String href = migrationPage.getUpdateHref(UPDATE_CHILD);
        Assert.assertFalse(href.isEmpty(), "❌ No Update link for " + UPDATE_CHILD);
        Map<String, String> p = planParams(href);
        String row = migrationPage.getRowText(UPDATE_CHILD);
        migrationPage.openUpdate(UPDATE_CHILD);
        Reporter.log("   " + href + "\n   Popup: " + migrationPage.getPopupText(), true);
        Assert.assertEquals(migrationPage.getPopupTitle(), "Co-Pay Plan Subscription Update (#" + UPDATE_CHILD + ")", "❌ Title");
        for (String l : new String[]{"Child Name", "Center Name", "Program", "Offer Title", "Payment Plan", "Child Status",
                "Existing Monthly Plan (Parent)", "Existing Yearly Plan (Parent)", "Existing Monthly Plan (Corporate)", "Existing Yearly Plan (Corporate)"})
            Assert.assertFalse(migrationPage.getPopupDetail(l).isEmpty(), "❌ Popup detail '" + l + "' missing");
        Assert.assertTrue(row.startsWith(migrationPage.getPopupDetail("Child Name")), "❌ Child Name vs grid");
        Assert.assertEquals(migrationPage.getPopupDetail("Center Name"), UPDATE_CHILD_CENTER, "❌ Center Name");
        String text = migrationPage.getPopupText();
        Assert.assertTrue(text.contains("FOR PARENT PAYER") && text.contains("FOR CORPORATE PAYER"), "❌ Payer sections");
        // the popup is pre-filled with the plans of the Update link
        Assert.assertEquals(migrationPage.getPlanValue("parent_monthly_plan"), p.get("m"), "❌ Parent monthly plan");
        Assert.assertEquals(migrationPage.getPlanValue("corp_monthly_plan"), p.get("cm"), "❌ Corporate monthly plan");
        if ("V1".equals(migrationPage.getPopupDetail("Payment Plan"))) {
            Assert.assertEquals(migrationPage.getPlanValue("parent_yearly_plan"), p.getOrDefault("y", ""), "❌ Parent yearly plan");
            Assert.assertEquals(migrationPage.getPlanValue("corp_yearly_plan"), p.getOrDefault("cy", ""), "❌ Corporate yearly plan");
        }
        Reporter.log("✅ SC_008_TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_010 (new) — Update with no plan selected
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_008_TC_010 — Update with no plan selected")
    public void tc010_updateWithoutPlan() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_010 — Update with no plan selected (" + UPDATE_CHILD + ", nothing saved)", true);
        migrationPage.openUpdate(UPDATE_CHILD);
        for (String id : new String[]{"parent_monthly_plan", "parent_yearly_plan", "corp_monthly_plan", "corp_yearly_plan"})
            migrationPage.selectPlan(id, "");
        for (String id : new String[]{"parent_monthly_plan", "parent_yearly_plan", "corp_monthly_plan", "corp_yearly_plan"})
            Assert.assertTrue(migrationPage.getPlanValue(id).isEmpty(), "❌ #" + id + " not cleared");
        migrationPage.clickUpdate();
        Thread.sleep(1500);
        String msg = migrationPage.getPlanMessage();
        Reporter.log("   Message: " + msg, true);
        Assert.assertTrue(msg.contains("Select at least one plan from list"), "❌ Message: " + msg);
        Reporter.log("✅ SC_008_TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_007 — Update (SAVES: child 56670's Co-Pay plans)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 50, description = "SC_008_TC_007 — Update the Co-Pay plans")
    public void tc007_updatePlans() throws InterruptedException {
        Reporter.log("▶ SC_008_TC_007 — Update the Co-Pay plans of " + UPDATE_CHILD, true);
        String href = migrationPage.getUpdateHref(UPDATE_CHILD);
        Assert.assertFalse(href.isEmpty(), "❌ No Update link for " + UPDATE_CHILD);
        Map<String, String> p = planParams(href);
        Reporter.log("   Before: " + migrationPage.getRowText(UPDATE_CHILD) + "\n   Plans to set: " + p, true);
        String before = openAccountStatementText(UPDATE_CHILD);
        Reporter.log("   Account Statement before: monthly parent " + accountStatementPlan(before, "Monthly", "Parent") + " / corporate "
                + accountStatementPlan(before, "Monthly", "Corporate") + " | yearly parent " + accountStatementPlan(before, "Yearly", "Parent")
                + " / corporate " + accountStatementPlan(before, "Yearly", "Corporate"), true);

        reopen();
        migrationPage.openUpdate(UPDATE_CHILD);
        migrationPage.clickUpdate();
        String result = migrationPage.waitForUpdateResult();
        Reporter.log("   Update result: " + result, true);
        Assert.assertTrue(result.contains("\"status\":\"ok\"") && result.contains("Details successfully Updated"), "❌ Update reply: " + result);

        String after = openAccountStatementText(UPDATE_CHILD);
        String pm = accountStatementPlan(after, "Monthly", "Parent"), cm = accountStatementPlan(after, "Monthly", "Corporate");
        String py = accountStatementPlan(after, "Yearly", "Parent"), cy = accountStatementPlan(after, "Yearly", "Corporate");
        Reporter.log("   Account Statement after: monthly parent " + pm + " / corporate " + cm + " | yearly parent " + py + " / corporate " + cy, true);
        Assert.assertEquals(pm, amountOf(p.get("m")), "❌ Parent monthly plan");
        Assert.assertEquals(cm, amountOf(p.get("cm")), "❌ Corporate monthly plan");
        if (!p.getOrDefault("y", "").isEmpty()) Assert.assertEquals(py, amountOf(p.get("y")), "❌ Parent yearly plan");
        if (!p.getOrDefault("cy", "").isEmpty()) Assert.assertEquals(cy, amountOf(p.get("cy")), "❌ Corporate yearly plan");

        reopen();
        Reporter.log("   Grid after: " + migrationPage.getRowText(UPDATE_CHILD), true);
        Reporter.log("✅ SC_008_TC_007 PASSED", true);
    }
}
