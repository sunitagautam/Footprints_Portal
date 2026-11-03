package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.CorporateCompanyPage;
import pages.Navigations;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// CORPORATE COMPANY (Franchise → Corporate Tieups → Corporate Company, /corporate_company)
// Source: "TC_Corporate Tie-ups.xlsx" SC_006 (10 TCs) → TC_Corporate_Tieups.xlsx, sheet TC_CorporateCompany. Runs as Rakesh.
//
// Saves (user, 2026-10-28: one QA company, reused): TC_008 adds "QA Auto Company" (Noida, Uttar Pradesh, Active) only
// when it does not exist yet — the same name is never submitted twice; TC_010 edits its address each run.
// ═══════════════════════════════════════════════════════════════
public class CorporateCompanyTest extends BaseTest {

    private static final String RUN_ID = new java.text.SimpleDateFormat("ddMMHHmm").format(new java.util.Date());
    private static final String QA_COMPANY = "QA Auto Company";
    private static final List<String> VISIBLE_COLUMNS = Arrays.asList("Company Name", "Company City", "Company State", "Company Status",
            "Company Created", "Action");
    private static final List<String> CSV_COLUMNS = Arrays.asList("Company Name", "Company ID", "Company City", "Company State",
            "Company Status", "Company Created");

    Navigations navigations;
    CorporateCompanyPage companyPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToCorporateCompany();
        dismissAlertIfPresent();
        companyPage = new CorporateCompanyPage(driver);
        Assert.assertTrue(companyPage.isPageLoaded(), "❌ Corporate Company did not load");
    }

    private void reopen() throws InterruptedException {
        navigations.goToCorporateCompany();
        companyPage = new CorporateCompanyPage(driver);
        Assert.assertTrue(companyPage.isPageLoaded(), "❌ Corporate Company did not load");
    }

    private void assertAll(String header, java.util.function.Predicate<String> ok, String what) {
        List<String> v = companyPage.getColumnValues(header);
        Reporter.log("   " + header + " (" + v.size() + " rows): " + new java.util.TreeSet<>(v).stream().limit(8).toList(), true);
        Assert.assertFalse(v.isEmpty(), "❌ No rows for " + what);
        for (String s : v) Assert.assertTrue(ok.test(s), "❌ " + header + " '" + s + "' does not match " + what);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_006_TC_001 — Default view of Corporate Company")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_006_TC_001 — Default view", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='corporate_company')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu: " + path + " | Heading: " + companyPage.getHeading(), true);
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Corporate Company", "❌ Menu path");
        Assert.assertEquals(companyPage.getHeading(), "Corporate Company", "❌ Heading");
        Assert.assertTrue(companyPage.isAddButtonVisible(), "❌ Add Corporate Company button");
        for (String id : Arrays.asList("company_name", "company_city", "company_state", "company_status", "btn_submit"))
            Assert.assertTrue(companyPage.isFilterFieldVisible(id), "❌ Filter field " + id);
        Assert.assertEquals(companyPage.getSelectOptions("company_status"), Arrays.asList("All", "Active", "Inactive"), "❌ Status options");
        Assert.assertEquals(companyPage.getSelectOptions("company_city").get(0), "All", "❌ City default");
        Assert.assertEquals(companyPage.getSelectOptions("company_state").get(0), "All", "❌ State default");
        Reporter.log("   Cities: " + companyPage.getSelectOptions("company_city") + "\n   States: " + companyPage.getSelectOptions("company_state"), true);
        Assert.assertTrue(companyPage.isSearchBoxVisible() && companyPage.isPaginationVisible(), "❌ Search box / pagination");
        Assert.assertEquals(companyPage.getDownloadButtonText(), "Download", "❌ Download button");
        Assert.assertEquals(companyPage.getPageLength(), 10, "❌ Rows per page");
        List<String> visible = companyPage.getVisibleColumnHeaders();
        Reporter.log("   Visible columns: " + visible + " | hidden: Company ID | " + companyPage.getInfoText(), true);
        Assert.assertEquals(visible, VISIBLE_COLUMNS, "❌ Visible columns");
        Assert.assertTrue(companyPage.getAllColumnHeaders().contains("Company ID"), "❌ Company ID column");
        Reporter.log("✅ SC_006_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_002 — Filters / Search button
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_006_TC_002 — Filters")
    public void tc002_filters() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_002 — Filters", true);
        companyPage.selectFilter("company_status", "Inactive");
        companyPage.clickSearch();
        Assert.assertEquals(companyPage.getSelectedText("company_status"), "Inactive", "❌ Status not kept");
        if (companyPage.getRowCount() > 0) assertAll("Company Status", "Inactive"::equals, "status Inactive");
        else Reporter.log("   Inactive → " + companyPage.getEmptyTableMessage(), true);
        reopen();
        companyPage.selectFilter("company_status", "Active");
        companyPage.clickSearch();
        assertAll("Company Status", "Active"::equals, "status Active");
        reopen();
        companyPage.selectFilter("company_city", "Noida");
        companyPage.clickSearch();
        assertAll("Company City", "Noida"::equals, "city Noida");
        reopen();
        companyPage.selectFilter("company_state", "Karnataka");
        companyPage.clickSearch();
        assertAll("Company State", "Karnataka"::equals, "state Karnataka");
        reopen();
        String name = companyPage.getPageColumnValues("Company Name").get(0);
        companyPage.enterCompanyName(name);
        companyPage.clickSearch();
        Assert.assertEquals(companyPage.getCompanyNameFilter(), name, "❌ Company name not kept");
        assertAll("Company Name", n -> n.toLowerCase().contains(name.toLowerCase()), "name " + name);
        reopen();
        companyPage.enterCompanyName("zzqq-no-such-company");
        companyPage.clickSearch();
        Reporter.log("   No match → " + companyPage.getEmptyTableMessage(), true);
        Assert.assertEquals(companyPage.getEmptyTableMessage(), "No data available in table", "❌ No-data message");
        Reporter.log("✅ SC_006_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_003 — Search box
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_006_TC_003 — Search")
    public void tc003_search() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_003 — Search", true);
        String name = companyPage.getPageColumnValues("Company Name").get(1);
        companyPage.searchTable(name);
        Reporter.log("   '" + name + "' → " + companyPage.getInfoText(), true);
        Assert.assertTrue(companyPage.getPageColumnValues("Company Name").contains(name), "❌ Company not found");
        companyPage.searchTable("zzqq-no-such-company");
        Assert.assertEquals(companyPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_006_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_004 — Download
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_006_TC_004 — Download corporate_company.csv")
    public void tc004_download() throws Exception {
        Reporter.log("▶ SC_006_TC_004 — Download", true);
        int expected = companyPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        companyPage.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().startsWith("corporate_company") && n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified())) csv = f;
            if (csv == null) Thread.sleep(500);
        }
        Assert.assertNotNull(csv, "❌ No corporate_company*.csv downloaded");
        Thread.sleep(800);
        String header;
        int rows = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            header = r.readLine().replace("﻿", "");
            while (r.readLine() != null) rows++;
        }
        List<String> cols = new ArrayList<>();
        for (String c : header.split(",")) cols.add(c.replace("\"", "").trim());
        Reporter.log("   File: " + csv.getName() + " | rows " + rows + " (grid " + expected + ") | columns " + cols, true);
        Assert.assertEquals(csv.getName().replaceAll(" \\(\\d+\\)", ""), "corporate_company.csv", "❌ File name");
        Assert.assertEquals(cols, CSV_COLUMNS, "❌ CSV columns");
        Assert.assertEquals(rows, expected, "❌ CSV rows vs grid");
        Reporter.log("✅ SC_006_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_005 / 006 — Pagination, sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_006_TC_005 — Pagination (10 per page)")
    public void tc005_pagination() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_005 — Pagination", true);
        List<String> first = companyPage.getPageColumnValues("Company Name");
        Reporter.log("   " + companyPage.getInfoText() + " | rows on page 1: " + first.size(), true);
        Assert.assertEquals(first.size(), 10, "❌ Rows per page");
        companyPage.clickNextPage();
        Reporter.log("   Next → " + companyPage.getInfoText(), true);
        Assert.assertTrue(companyPage.getInfoText().contains("Showing 11 to 20"), "❌ Info after Next");
        Assert.assertNotEquals(companyPage.getPageColumnValues("Company Name"), first, "❌ Same rows after Next");
        companyPage.clickPreviousPage();
        Assert.assertEquals(companyPage.getPageColumnValues("Company Name"), first, "❌ Page 1 after Previous");
        Reporter.log("✅ SC_006_TC_005 PASSED", true);
    }

    @Test(priority = 6, description = "SC_006_TC_006 — Sorting")
    public void tc006_sorting() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_006 — Sorting", true);
        for (String col : VISIBLE_COLUMNS.subList(0, VISIBLE_COLUMNS.size() - 1)) {
            companyPage.clickColumnHeader(col);
            String a = companyPage.getHeaderSortClass(col);
            companyPage.clickColumnHeader(col);
            String b = companyPage.getHeaderSortClass(col);
            Reporter.log("   " + col + ": " + a + " → " + b, true);
            Assert.assertTrue(a.matches(".*sorting_(asc|desc).*") && b.matches(".*sorting_(asc|desc).*") && !a.equals(b),
                    "❌ " + col + " did not sort both ways");
        }
        Reporter.log("   Action: " + companyPage.getHeaderSortClass("Action") + " (not sortable)", true);
        Assert.assertTrue(companyPage.getHeaderSortClass("Action").contains("sorting_disabled"), "❌ Action column should not sort");
        companyPage.clickColumnHeader("Company Name");
        boolean asc = companyPage.getHeaderSortClass("Company Name").contains("asc");
        List<String> names = companyPage.getPageColumnValues("Company Name");
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        if (!asc) java.util.Collections.reverse(sorted);
        Reporter.log("   Company Name " + (asc ? "asc" : "desc") + ": " + names, true);
        Assert.assertEquals(names, sorted, "❌ Company Name order");
        Reporter.log("✅ SC_006_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_007 / 009 — Add popup default view and validation (nothing saved)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_006_TC_007 — Add Corporate Company popup default view")
    public void tc007_addPopupView() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_007 — Add popup view", true);
        companyPage.clickAddCompany();
        Assert.assertTrue(companyPage.isPopupVisible(), "❌ Add popup did not open");
        String text = companyPage.getPopupText();
        Reporter.log("   " + text.substring(0, Math.min(160, text.length())) + "… | title '" + companyPage.getPopupTitle() + "'", true);
        Assert.assertTrue(text.startsWith("Add Corporate Company") || companyPage.getPopupTitle().contains("Add Corporate Company"), "❌ Title");
        for (String l : Arrays.asList("Company Name*", "Company Address*", "Company City*", "Company State*", "Company Status*"))
            Assert.assertTrue(text.replace(" *", "*").contains(l), "❌ Label " + l);
        List<String> cities = companyPage.getPopupOptions("company_city_id"), states = companyPage.getPopupOptions("company_state_id");
        Reporter.log("   Cities: " + cities + "\n   States: " + states + " | Status: " + companyPage.getPopupOptions("new_company_status")
                + " (default " + companyPage.getPopupSelectedText("new_company_status") + ") | button " + companyPage.getPopupValue("btn_save_data"), true);
        Assert.assertEquals(cities.get(0), "--SELECT--", "❌ City placeholder");
        Assert.assertEquals(cities.get(cities.size() - 1), "OTHER", "❌ City OTHER option");
        Assert.assertEquals(states.get(states.size() - 1), "OTHER", "❌ State OTHER option");
        Assert.assertEquals(companyPage.getPopupOptions("new_company_status"), Arrays.asList("Active", "Inactive"), "❌ Status options");
        Assert.assertEquals(companyPage.getPopupSelectedText("new_company_status"), "Active", "❌ Default status");
        Assert.assertEquals(companyPage.getPopupValue("btn_save_data"), "Submit", "❌ Submit button");
        Assert.assertFalse(companyPage.isPopupFieldVisible("other_city"), "❌ Other city shown before OTHER is chosen");
        companyPage.selectPopup("company_city_id", "OTHER");
        companyPage.selectPopup("company_state_id", "OTHER");
        Reporter.log("   OTHER → other city field " + companyPage.isPopupFieldVisible("other_city") + " | other state field " + companyPage.isPopupFieldVisible("other_state"), true);
        Assert.assertTrue(companyPage.isPopupFieldVisible("other_city") && companyPage.isPopupFieldVisible("other_state"), "❌ OTHER does not show a text field");
        Reporter.log("✅ SC_006_TC_007 PASSED", true);
    }

    @Test(priority = 8, description = "SC_006_TC_009 — Add popup mandatory fields")
    public void tc009_addValidation() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_009 — Add popup validation", true);
        companyPage.clickAddCompany();
        companyPage.startCapture(false);
        companyPage.clickPopupSubmit();
        List<String> errors = companyPage.getPopupErrors();
        Reporter.log("   Empty → " + errors + " | confirms " + companyPage.getCapturedConfirms() + " | requests " + companyPage.getCapturedRequests(), true);
        Assert.assertEquals(errors, Arrays.asList("new_company_name => Company name is required.", "new_company_address => Company address is required.",
                "company_city_id => Company city is required.", "company_state_id => Company state is required."), "❌ Required messages");
        Assert.assertTrue(companyPage.getCapturedRequests().isEmpty(), "❌ A request was sent");
        Reporter.log("✅ SC_006_TC_009 PASSED (nothing saved)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_008 — Add Corporate Company (SAVES once: "QA Auto Company")
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC_006_TC_008 — Add Corporate Company")
    public void tc008_addCompany() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_008 — Add Corporate Company", true);
        String id = companyPage.findCompanyId(QA_COMPANY);
        if (!id.isEmpty()) {
            Reporter.log("   '" + QA_COMPANY + "' already exists (id " + id + ") — not submitted again (one reused QA company)", true);
            Reporter.log("✅ SC_006_TC_008 PASSED (company added on an earlier run)", true);
            return;
        }
        companyPage.clickAddCompany();
        companyPage.enterPopupField("new_company_name", QA_COMPANY);
        companyPage.enterPopupField("new_company_address", "Sector 126 Noida (QA automation)");
        companyPage.selectPopup("company_city_id", "Noida");
        companyPage.selectPopup("company_state_id", "Uttar Pradesh");
        companyPage.selectPopup("new_company_status", "Active");
        companyPage.startCapture(true);
        companyPage.clickPopupSubmit();
        String msg = companyPage.waitForMessage();
        Reporter.log("   Confirms " + companyPage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains("added successfully"), "❌ Message: " + msg);
        Thread.sleep(3000);
        reopen();
        id = companyPage.findCompanyId(QA_COMPANY);
        companyPage.searchTable(QA_COMPANY);
        Reporter.log("   Listed: id " + id + " | " + companyPage.getPageColumnValues("Company City") + " " + companyPage.getPageColumnValues("Company State")
                + " " + companyPage.getPageColumnValues("Company Status") + " " + companyPage.getPageColumnValues("Company Created"), true);
        Assert.assertFalse(id.isEmpty(), "❌ New company not listed");
        Assert.assertEquals(companyPage.getPageColumnValues("Company City"), List.of("Noida"), "❌ City");
        Assert.assertEquals(companyPage.getPageColumnValues("Company State"), List.of("Uttar Pradesh"), "❌ State");
        Assert.assertEquals(companyPage.getPageColumnValues("Company Status"), List.of("Active"), "❌ Status");
        Reporter.log("✅ SC_006_TC_008 PASSED — '" + QA_COMPANY + "' id " + id, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_006_TC_010 — Edit Corporate Company (SAVES: QA company's address)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 31, description = "SC_006_TC_010 — Edit Corporate Company", dependsOnMethods = "tc008_addCompany")
    public void tc010_editCompany() throws InterruptedException {
        Reporter.log("▶ SC_006_TC_010 — Edit Corporate Company", true);
        String id = companyPage.findCompanyId(QA_COMPANY);
        Assert.assertFalse(id.isEmpty(), "❌ '" + QA_COMPANY + "' not found");
        companyPage.clickEditCompany(id);
        String title = companyPage.getPopupText();
        Reporter.log("   Popup: " + title.substring(0, Math.min(60, title.length())) + "… | name " + companyPage.getPopupValue("new_company_name")
                + " | address " + companyPage.getPopupValue("new_company_address") + " | button " + companyPage.getPopupValue("btn_save_data"), true);
        Assert.assertTrue(title.startsWith("Update Corporate Company"), "❌ Popup title");
        Assert.assertEquals(companyPage.getPopupValue("new_company_name"), QA_COMPANY, "❌ Name pre-filled");
        Assert.assertEquals(companyPage.getPopupValue("btn_save_data"), "Update", "❌ Update button");
        String address = "Sector 126 Noida (QA automation " + RUN_ID + ")";
        companyPage.enterPopupField("new_company_address", address);
        companyPage.startCapture(true);
        companyPage.clickPopupSubmit();
        String msg = companyPage.waitForMessage();
        Reporter.log("   Confirms " + companyPage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains("updated successfully"), "❌ Message: " + msg);
        Thread.sleep(3000);
        reopen();
        companyPage.clickEditCompany(id);
        Reporter.log("   Saved address: " + companyPage.getPopupValue("new_company_address"), true);
        Assert.assertEquals(companyPage.getPopupValue("new_company_address"), address, "❌ Address not updated");
        Reporter.log("✅ SC_006_TC_010 PASSED", true);
    }
}
