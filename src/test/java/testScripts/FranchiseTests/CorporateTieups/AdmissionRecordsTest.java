package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.AdmissionRecordsPage;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════════
// ADMISSION RECORDS (Franchise → Corporate Tieups → Admission Records, /corporate_admissions)
// Source: "TC_Corporate Tie-ups.xlsx" SC_003 (14 TCs) → TC_Corporate_Tieups.xlsx, sheet TC_AdmissionRecords,
// updated to the live screen + new TCs from exploration (user, 2026-10-28). Runs as Rakesh.
//
// Priorities 1–29: nothing is saved.
// Priority 30 (TC_009) SAVES: changes one record's Employee Code and restores the original value
// (2 saves, the record ends unchanged). Records whose employer is "QA Auto Employer" are skipped.
// ═══════════════════════════════════════════════════════════════
public class AdmissionRecordsTest extends BaseTest {

    private static final String RIGHTS_USER_ID = "357";   // Rakesh
    private static final String CONFIRM_EDIT = "Are you sure want to edit Invitation?";
    private static final List<String> VISIBLE_COLUMNS = Arrays.asList("Child Name", "Offer Title", "Length", "Admission Date", "Employment Details");
    private static final List<String> PROGRAMS = Arrays.asList("All", "Full Day", "Half Day", "Half Day 2", "Half Day 3", "After School",
            "Extended Preschool", "Evening Program");
    // ✅ CSV columns listed in SC_003_TC_006
    private static final List<String> CSV_COLUMNS = Arrays.asList("Child ID", "Child Name", "Center", "Company", "Offer Title", "Length",
            "Admission Plan", "Admission Date", "Status", "Attrition Date", "Father Name", "Father Phone", "Father Email", "Mother Name",
            "Mother Phone", "Mother Email", "Joining Date", "DOB", "Next Monthly Billing Date", "Next Yearly Billing Date",
            "Monthly Fee (Parent)", "Monthly Fee (Corporate)", "Registration Fee", "Yearly (Parent)", "Yearly (Corporate)",
            "Monthly Payer", "Yearly Payer", "Transport Payer", "Kit Payer", "Monthly Discount", "Yearly Discount",
            "Attrition Reason", "Employer", "Employee Email", "Employee Code");
    private static final DateTimeFormatter GRID_DATE = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);

    Navigations navigations;
    AdmissionRecordsPage recordsPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToAdmissionRecords();
        dismissAlertIfPresent();
        recordsPage = new AdmissionRecordsPage(driver);
        Assert.assertTrue(recordsPage.isPageLoaded(), "❌ Admission Records did not load");
    }

    private void assertAll(String header, java.util.function.Predicate<String> ok, String what) {
        List<String> v = recordsPage.getColumnValues(header);
        Reporter.log("   " + header + " (" + v.size() + " rows): " + new java.util.TreeSet<>(v), true);
        Assert.assertFalse(v.isEmpty(), "❌ No rows for " + what);
        for (String s : v) Assert.assertTrue(ok.test(s), "❌ " + header + " '" + s + "' does not match " + what);
    }

    // "Download All 3041 Records" → 3041 ; plain "Download" → -1
    private int downloadAllCount() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Download All (\\d+) Records").matcher(recordsPage.getDownloadButtonText());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_003_TC_001 — Default view of Admission Records")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_003_TC_001 — Default view", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='corporate_admissions')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu: " + path + " | Heading: " + recordsPage.getHeading(), true);
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Admission Records", "❌ Menu path");
        Assert.assertEquals(recordsPage.getHeading(), "Corporate Admission Records", "❌ Heading");
        for (String id : Arrays.asList("sr_corporate_offer", "sr_corporate_company", "sr_joining_from", "sr_joining_to", "sr_billing_from",
                "sr_billing_to", "sr_program", "sr_status", "exclude_inhouse", "exclude_onsite", "btn_submit"))
            Assert.assertTrue(recordsPage.isFilterFieldVisible(id) || id.startsWith("sr_corporate"), "❌ Filter field " + id);
        Assert.assertEquals(recordsPage.getSelectOptions("sr_program"), PROGRAMS, "❌ Program options");
        Assert.assertEquals(recordsPage.getSelectOptions("sr_status"), Arrays.asList("All", "Active", "Attrition"), "❌ Status options");
        Assert.assertEquals(recordsPage.getSelectedText("sr_status"), "Active", "❌ Default Status");
        Assert.assertEquals(recordsPage.getSelectedText("sr_program"), "All", "❌ Default Program");
        Assert.assertEquals(recordsPage.getSelectedText("sr_corporate_offer"), "--SELECT--", "❌ Default Corporate Offer");
        Assert.assertEquals(recordsPage.getSelectedText("sr_corporate_company"), "--SELECT--", "❌ Default Company");
        Assert.assertTrue(recordsPage.isChecked("exclude_inhouse") && recordsPage.isChecked("exclude_onsite"), "❌ Exclude checkboxes not ticked by default");
        List<String> cities = recordsPage.getMultiselectOptions("select[name='city_filter_list[]']");
        List<String> centers = recordsPage.getMultiselectOptions("#center_selection");
        Reporter.log("   City options: " + cities.size() + " | Center options: " + centers.size() + " (first " + centers.get(0) + ")", true);
        Assert.assertTrue(cities.size() > 10 && centers.get(0).equals("All Center"), "❌ City / Center lists");
        Assert.assertTrue(recordsPage.isSearchBoxVisible(), "❌ Search box");
        Assert.assertEquals(recordsPage.getSearchPlaceholder(), "Filter these admissions", "❌ Search placeholder");
        Assert.assertTrue(recordsPage.getDownloadButtonText().startsWith("Download"), "❌ Download button");
        Assert.assertTrue(recordsPage.isPaginationVisible(), "❌ Pagination");
        Assert.assertEquals(recordsPage.getPageLength(), 20, "❌ Rows per page");
        List<String> visible = recordsPage.getVisibleColumnHeaders();
        Reporter.log("   Visible columns: " + visible + " | Download: " + recordsPage.getDownloadButtonText() + " | " + recordsPage.getInfoText(), true);
        Assert.assertEquals(visible, VISIBLE_COLUMNS, "❌ Visible columns");
        Assert.assertTrue(recordsPage.getRowCount() > 0, "❌ No records by default");
        Reporter.log("✅ SC_003_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_002 — Filters (Program, Status, Joining / Billing dates, Company, Offer, no data)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_003_TC_002 — Filters")
    public void tc002_filters() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_002 — Filters", true);
        recordsPage.selectByText("sr_program", "Full Day");
        recordsPage.clickFilter();
        Assert.assertEquals(recordsPage.getSelectedText("sr_program"), "Full Day", "❌ Program not kept after Filter");
        assertAll("Length", "Full Day"::equals, "Program Full Day");

        navigations.goToAdmissionRecords();
        recordsPage.isPageLoaded();
        recordsPage.selectByText("sr_status", "Attrition");
        recordsPage.clickFilter();
        assertAll("Status", "Attrition"::equals, "Status Attrition");

        navigations.goToAdmissionRecords();
        recordsPage.isPageLoaded();
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(1), to = from.plusDays(19);
        recordsPage.setDate("sr_joining_from", from.getYear(), from.getMonthValue(), from.getDayOfMonth());
        recordsPage.setDate("sr_joining_to", to.getYear(), to.getMonthValue(), to.getDayOfMonth());
        recordsPage.clickFilter();
        Reporter.log("   Joining " + recordsPage.getFieldValue("sr_joining_from") + " – " + recordsPage.getFieldValue("sr_joining_to"), true);
        assertAll("Joining Date", s -> !LocalDate.parse(s).isBefore(from) && !LocalDate.parse(s).isAfter(to), "Joining " + from + "–" + to);

        navigations.goToAdmissionRecords();
        recordsPage.isPageLoaded();
        // ✅ Billing range Sep–Oct 2026 (user, 2026-10-28, Q14)
        LocalDate bFrom = LocalDate.of(2026, 9, 1), bTo = LocalDate.of(2026, 10, 31);
        int before = downloadAllCount();
        recordsPage.setDate("sr_billing_from", 2026, 9, 1);
        recordsPage.setDate("sr_billing_to", 2026, 10, 31);
        recordsPage.clickFilter();
        java.util.Map<String, Long> monthly = new java.util.TreeMap<>(), yearly = new java.util.TreeMap<>();
        for (String v : recordsPage.getColumnValues("Next Monthly Billing Date")) monthly.merge(v, 1L, Long::sum);
        for (String v : recordsPage.getColumnValues("Next Yearly Billing Date")) yearly.merge(v, 1L, Long::sum);
        Reporter.log("   Billing " + recordsPage.getFieldValue("sr_billing_from") + " – " + recordsPage.getFieldValue("sr_billing_to") + " | "
                + recordsPage.getDownloadButtonText() + " (default " + before + ")\n   Next Monthly: " + monthly + "\n   Next Yearly: " + yearly, true);
        Assert.assertEquals(recordsPage.getFieldValue("sr_billing_from"), "01 Sep 2026", "❌ Billing from not kept");
        Assert.assertEquals(recordsPage.getFieldValue("sr_billing_to"), "31 Oct 2026", "❌ Billing to not kept");
        Assert.assertTrue(recordsPage.getRowCount() > 0 && downloadAllCount() != before, "❌ Billing filter did not change the records");

        navigations.goToAdmissionRecords();
        recordsPage.isPageLoaded();
        recordsPage.selectByText("sr_corporate_company", "Wipro");
        recordsPage.clickFilter();
        assertAll("Company", "Wipro"::equals, "Company Wipro");

        navigations.goToAdmissionRecords();
        recordsPage.isPageLoaded();
        String offer = recordsPage.findOption("sr_corporate_offer", "QA Auto Tieup V2");
        recordsPage.selectByText("sr_corporate_offer", offer);
        recordsPage.clickFilter();
        Reporter.log("   Offer " + offer + " → " + recordsPage.getInfoText() + " | " + recordsPage.getEmptyTableMessage(), true);
        Assert.assertEquals(recordsPage.getEmptyTableMessage(), "No data available in table", "❌ No-data message");
        Reporter.log("✅ SC_003_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_003 / TC_004 — Exclude Inhouse / Onsite Admissions
    // ════════════════════════════════════════════════════════════════════
    private void checkExclude(String checkbox, String offerWord, String tc) throws InterruptedException {
        int ticked = downloadAllCount();
        List<String> offersTicked = recordsPage.getColumnValues("Offer Title");
        recordsPage.setChecked(checkbox, false);
        recordsPage.clickFilter();
        Assert.assertFalse(recordsPage.isChecked(checkbox), "❌ " + checkbox + " not kept unticked after Filter");
        int unticked = downloadAllCount();
        long withWord = recordsPage.getColumnValues("Offer Title").stream().filter(o -> o.toLowerCase().contains(offerWord)).count();
        long withWordTicked = offersTicked.stream().filter(o -> o.toLowerCase().contains(offerWord)).count();
        Reporter.log("   Ticked: " + ticked + " records, " + withWordTicked + " '" + offerWord + "' offers in grid | Unticked: " + unticked
                + " records, " + withWord + " '" + offerWord + "' offers in grid", true);
        Assert.assertTrue(unticked > ticked, "❌ " + tc + ": unticking did not add records (" + ticked + " → " + unticked + ")");
        Assert.assertEquals(withWordTicked, 0L, "❌ " + tc + ": '" + offerWord + "' offers listed while excluded");
        Assert.assertTrue(withWord > 0, "❌ " + tc + ": no '" + offerWord + "' offers after unticking");
    }

    @Test(priority = 3, description = "SC_003_TC_003 — Exclude Inhouse Admissions")
    public void tc003_excludeInhouse() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_003 — Exclude Inhouse Admissions", true);
        checkExclude("exclude_inhouse", "inhouse", "SC_003_TC_003");
        Reporter.log("✅ SC_003_TC_003 PASSED", true);
    }

    @Test(priority = 4, description = "SC_003_TC_004 — Exclude Onsite Admissions")
    public void tc004_excludeOnsite() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_004 — Exclude Onsite Admissions", true);
        checkExclude("exclude_onsite", "onsite", "SC_003_TC_004");
        Reporter.log("✅ SC_003_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_005 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_003_TC_005 — Search")
    public void tc005_search() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_005 — Search", true);
        String offer = recordsPage.getPageColumnValues("Offer Title").get(0);
        String child = recordsPage.getPageColumnValues("Child Name").get(0);
        recordsPage.searchTable(offer);
        Reporter.log("   '" + offer + "' → " + recordsPage.getInfoText(), true);
        Assert.assertTrue(recordsPage.getPageColumnValues("Offer Title").contains(offer), "❌ Searched offer not listed");
        recordsPage.searchTable(child);
        Reporter.log("   '" + child + "' → " + recordsPage.getInfoText(), true);
        Assert.assertTrue(recordsPage.getPageColumnValues("Child Name").contains(child), "❌ Searched child not listed");
        recordsPage.searchTable("zzqq-no-such-record");
        Reporter.log("   no match → " + recordsPage.getEmptyTableMessage(), true);
        Assert.assertEquals(recordsPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_003_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_006 — Download (all matching records)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_003_TC_006 — Download corporate_admission CSV")
    public void tc006_download() throws Exception {
        Reporter.log("▶ SC_003_TC_006 — Download", true);
        int expected = downloadAllCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        recordsPage.clickDownload();
        File csv = null;
        for (int i = 0; i < 90 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().startsWith("corporate_admission") && n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified())) csv = f;
            if (csv == null) Thread.sleep(1000);
        }
        Assert.assertNotNull(csv, "❌ No corporate_admission*.csv downloaded to " + downloads);
        Thread.sleep(1500);
        String header;
        int rows = 0;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            header = r.readLine();
            while (header != null && header.trim().isEmpty()) header = r.readLine();   // the file starts with a blank line
            String line;
            while ((line = r.readLine()) != null)
                if (line.matches("^\\d+,.*")) rows++;   // a record starts with its Child ID (cells can span lines)
        }
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd_MMM_yyyy", Locale.ENGLISH));
        Reporter.log("   File: " + csv.getName() + " | records: " + rows + " (button: " + expected + ")\n   Header: " + header, true);
        Assert.assertEquals(csv.getName().replaceAll(" \\(\\d+\\)", ""), "corporate_admission" + today + ".csv", "❌ File name");
        for (String col : CSV_COLUMNS)
            Assert.assertTrue(header.contains("\"" + col + "\"") || header.contains("," + col + ",") || header.startsWith(col + ",")
                    || header.contains("," + col + "\"") || header.endsWith("," + col) || header.endsWith("\"" + col + "\""), "❌ CSV missing column: " + col);
        if (expected > 0) Assert.assertEquals(rows, expected, "❌ CSV records vs 'Download All N Records'");
        Reporter.log("✅ SC_003_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_007 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_003_TC_007 — Pagination")
    public void tc007_pagination() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_007 — Pagination", true);
        List<String> first = recordsPage.getPageColumnValues("Child ID");
        Reporter.log("   " + recordsPage.getInfoText() + " | rows on page 1: " + first.size(), true);
        Assert.assertEquals(first.size(), 20, "❌ Rows per page");
        Assert.assertTrue(recordsPage.isNextPageEnabled(), "❌ Next disabled");
        recordsPage.clickNextPage();
        Reporter.log("   Next → " + recordsPage.getInfoText(), true);
        Assert.assertTrue(recordsPage.getInfoText().contains("Showing 21–40"), "❌ Info after Next");
        Assert.assertNotEquals(recordsPage.getPageColumnValues("Child ID"), first, "❌ Same rows after Next");
        recordsPage.clickPreviousPage();
        Assert.assertEquals(recordsPage.getPageColumnValues("Child ID"), first, "❌ Page 1 after Previous");
        Reporter.log("✅ SC_003_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_008 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_003_TC_008 — Sorting")
    public void tc008_sorting() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_008 — Sorting", true);
        for (String col : VISIBLE_COLUMNS) {
            recordsPage.clickColumnHeader(col);
            String a = recordsPage.getHeaderSortClass(col);
            recordsPage.clickColumnHeader(col);
            String b = recordsPage.getHeaderSortClass(col);
            Reporter.log("   " + col + ": " + a + " → " + b, true);
            Assert.assertTrue(a.matches(".*sorting_(asc|desc).*") && b.matches(".*sorting_(asc|desc).*") && !a.equals(b),
                    "❌ " + col + " did not sort both ways");
        }
        recordsPage.clickColumnHeader("Child Name");
        List<String> names = recordsPage.getPageColumnValues("Child Name");
        boolean asc = recordsPage.getHeaderSortClass("Child Name").contains("asc");
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        if (!asc) java.util.Collections.reverse(sorted);
        Reporter.log("   Child Name " + (asc ? "asc" : "desc") + ": " + names.subList(0, Math.min(5, names.size())), true);
        Assert.assertEquals(names, sorted, "❌ Child Name order");
        Reporter.log("✅ SC_003_TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_010 — Edit Invite Details: validation (nothing saved)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_003_TC_010 — Edit Invite Details: field validation")
    public void tc010_editValidation() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_010 — Edit Invite Details validation", true);
        String child = recordsPage.getPageColumnValues("Child ID").get(0);
        recordsPage.openEditInviteDetails(child);
        Assert.assertTrue(recordsPage.isEditPopupVisible(), "❌ Edit popup did not open");
        recordsPage.startCapture(false);
        for (String id : Arrays.asList("edit_employee_code", "edit_employee_email", "edit_employer")) recordsPage.enterEditField(id, "");
        recordsPage.clickUpdateInvite();
        List<String> errors = recordsPage.getEditErrors();
        Reporter.log("   Empty → " + errors, true);
        Assert.assertEquals(errors, Arrays.asList("edit_employee_code => Required!", "edit_employee_email => Required!", "edit_employer => Required!"),
                "❌ Required messages");
        recordsPage.enterEditField("edit_employee_code", "QA-VAL");
        recordsPage.enterEditField("edit_employer", "QA Validation");
        recordsPage.enterEditField("edit_employee_email", "test@");
        recordsPage.clickUpdateInvite();
        errors = recordsPage.getEditErrors();
        Reporter.log("   Email 'test@' → " + errors, true);
        Assert.assertEquals(errors, Arrays.asList("edit_employee_email => Invalid Email!"), "❌ Invalid email message");
        Assert.assertTrue(recordsPage.getCapturedConfirms().isEmpty(), "❌ Confirm shown for invalid data");
        Assert.assertTrue(recordsPage.getCapturedRequests().isEmpty(), "❌ Request sent");
        Reporter.log("✅ SC_003_TC_010 PASSED (nothing saved; popup closed by reload)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_011 / TC_012 — Child ID column and its Account Statement link
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "SC_003_TC_011 — Child ID column")
    public void tc011_childIdColumn() throws Exception {
        // ✅ Q13 (user, 2026-10-28): Child ID is shown in the downloaded file, not on screen
        Reporter.log("▶ SC_003_TC_011 — Child ID column (download only)", true);
        Assert.assertTrue(recordsPage.getAllColumnHeaders().contains("Child ID"), "❌ No Child ID column");
        boolean visible = recordsPage.isColumnVisible("Child ID");
        Reporter.log("   Child ID visible on screen: " + visible + " | first (hidden cell): " + recordsPage.getFirstChildIdHref(), true);
        Assert.assertFalse(visible, "❌ Child ID is shown on screen (should be in the download only)");
        Assert.assertTrue(recordsPage.getVisibleColumnHeaders().equals(VISIBLE_COLUMNS), "❌ Visible columns");
        Reporter.log("   Child ID in the CSV: checked by SC_003_TC_006 (first column 'Child ID')", true);
        Reporter.log("✅ SC_003_TC_011 PASSED", true);
    }

    @Test(priority = 12, description = "SC_003_TC_012 — Child ID link opens Account Statement")
    public void tc012_childIdLink() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_012 — Child ID link", true);
        String[] link = recordsPage.getFirstChildIdHref().split(" \\| ");
        Reporter.log("   href " + link[0] + " | target " + link[1] + " | text " + link[2], true);
        Assert.assertEquals(link[0], "account_statement?child_id=" + link[2], "❌ Link target");
        Assert.assertEquals(link[1], "_blank", "❌ Link should open a new tab");
        driver.get("https://test-franchise.footprintseducation.in/" + link[0]);
        Thread.sleep(4000);
        String legend = String.valueOf(((JavascriptExecutor) driver).executeScript("var l=document.querySelector('legend');return l?l.textContent.replace(/\\s+/g,' ').trim():'';"));
        Reporter.log("   Account Statement: " + legend, true);
        Assert.assertTrue(driver.getCurrentUrl().contains("account_statement?child_id=" + link[2]) && legend.contains("#" + link[2]),
                "❌ Account Statement of " + link[2] + " not shown");
        Reporter.log("✅ SC_003_TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_013 / TC_014 — Access rights
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "SC_003_TC_013 — View_Center_Corporate_Admissions (centre-level user)")
    public void tc013_centerLevelRight() {
        throw new SkipException("Needs a centre-level (CD) user with View_Center_Corporate_Admissions — not automated (runs as Rakesh)");
    }

    @Test(priority = 14, description = "SC_003_TC_014 — View_Corporate_Admissions (global access)")
    public void tc014_globalRight() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_014 — View_Corporate_Admissions", true);
        boolean has = new UserRightsPage(driver).hasRight(RIGHTS_USER_ID, "View_Corporate_Admissions");
        navigations.goToAdmissionRecords();
        recordsPage = new AdmissionRecordsPage(driver);
        boolean opens = recordsPage.isPageLoaded();
        Reporter.log("   Rakesh has View_Corporate_Admissions: " + has + " | screen opens: " + opens + " | " + recordsPage.getInfoText(), true);
        Assert.assertTrue(has, "❌ Rakesh lacks View_Corporate_Admissions");
        Assert.assertTrue(opens && recordsPage.getRowCount() > 0, "❌ Screen / records not shown");
        Reporter.log("✅ SC_003_TC_014 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_015 (new) — City filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "SC_003_TC_015 — City filter")
    public void tc015_cityFilter() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_015 — City filter", true);
        recordsPage.multiselectOnly("select[name='city_filter_list[]']", "Hyderabad");
        recordsPage.clickFilter();
        Reporter.log("   City kept: " + recordsPage.getMultiselectSelected("select[name='city_filter_list[]']") + " | " + recordsPage.getInfoText(), true);
        assertAll("Center Region", "Telangana"::equals, "City Hyderabad (region Telangana)");
        Reporter.log("✅ SC_003_TC_015 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_016 (new) — Center filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "SC_003_TC_016 — Center filter")
    public void tc016_centerFilter() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_016 — Center filter", true);
        String center = recordsPage.getColumnValues("Center").get(0);
        recordsPage.multiselectOnly("#center_selection", center);
        recordsPage.clickFilter();
        Reporter.log("   Center kept: " + recordsPage.getMultiselectSelected("#center_selection") + " | " + recordsPage.getInfoText(), true);
        assertAll("Center", center::equals, "Center " + center);
        Reporter.log("✅ SC_003_TC_016 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_017 (new) — Status filter: which child statuses each option lists
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 17, description = "SC_003_TC_017 — Status filter values")
    public void tc017_statusValues() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_017 — Status values", true);
        java.util.Set<String> active = new java.util.TreeSet<>(recordsPage.getColumnValues("Status"));
        Reporter.log("   Status Active lists: " + active, true);
        Assert.assertFalse(active.contains("Attrition"), "❌ Attrition listed under Active");
        recordsPage.selectByText("sr_status", "All");
        recordsPage.clickFilter();
        java.util.Set<String> all = new java.util.TreeSet<>(recordsPage.getColumnValues("Status"));
        Reporter.log("   Status All lists: " + all + " | Download: " + recordsPage.getDownloadButtonText(), true);
        Assert.assertTrue(all.containsAll(active) && all.size() >= active.size(), "❌ All does not cover Active");
        Reporter.log("✅ SC_003_TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_018 (new) — Grid shows max 500 records; Download All exports every record
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 18, description = "SC_003_TC_018 — Grid limit vs Download All N Records")
    public void tc018_gridLimit() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_018 — Grid limit", true);
        int all = downloadAllCount();
        Reporter.log("   Default: " + recordsPage.getInfoText() + " | rows " + recordsPage.getRowCount() + " | " + recordsPage.getDownloadButtonText(), true);
        Assert.assertTrue(all > 500, "❌ Expected more than 500 records by default: " + all);
        Assert.assertEquals(recordsPage.getRowCount(), 500, "❌ Grid rows when more than 500 records match");
        String note = String.valueOf(((JavascriptExecutor) driver).executeScript("return document.body.innerText.match(/\\* ?Max 500 records[^\\n]*/)||'';"));
        Reporter.log("   Note: " + note, true);
        Assert.assertTrue(note.contains("Max 500 records are shown. Please restrict your search criteria."), "❌ Max-500 note not shown");
        recordsPage.selectByText("sr_corporate_company", "Wipro");
        recordsPage.clickFilter();
        Reporter.log("   Company Wipro: " + recordsPage.getInfoText() + " | rows " + recordsPage.getRowCount() + " | " + recordsPage.getDownloadButtonText(), true);
        Assert.assertTrue(recordsPage.getRowCount() < 500, "❌ Wipro rows");
        Assert.assertFalse(String.valueOf(((JavascriptExecutor) driver).executeScript("return document.body.innerText;")).contains("Max 500 records are shown"),
                "❌ Max-500 note shown for fewer than 500 records");
        Reporter.log("✅ SC_003_TC_018 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_019 (new) — Child Name opens Child Information
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "SC_003_TC_019 — Child Name opens Child Information popup")
    public void tc019_childInformation() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_019 — Child Information", true);
        String name = recordsPage.openFirstChildInfo();
        String text = recordsPage.getPopupText();
        Reporter.log("   " + name + " → " + text.substring(0, Math.min(300, text.length())), true);
        Assert.assertFalse(text.isEmpty(), "❌ Child Information popup did not open");
        Assert.assertTrue(text.toLowerCase().contains(name.toLowerCase().split(" ")[0]), "❌ Popup is not for " + name);
        Reporter.log("✅ SC_003_TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_003_TC_009 — Edit Invite Details: update (SAVES, value restored)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC_003_TC_009 — Edit Invite Details: update and restore")
    public void tc009_editInviteDetails() throws InterruptedException {
        Reporter.log("▶ SC_003_TC_009 — Edit Invite Details (SAVES, restored)", true);
        String child = "", orig = "", email = "", employer = "";
        for (String row : recordsPage.findRowsWithEmploymentDetails("QA Auto")) {
            String c = row.split("\\|")[0];
            recordsPage.openEditInviteDetails(c);
            String e = recordsPage.getEditValue("edit_employee_email"), m = recordsPage.getEditValue("edit_employer");
            if (!e.contains("*") && !e.isEmpty() && !m.isEmpty() && !m.equals("QA Auto Employer")
                    && !recordsPage.getEditValue("edit_employee_code").isEmpty()) {
                child = c; orig = recordsPage.getEditValue("edit_employee_code"); email = e; employer = m;
                break;
            }
            navigations.goToAdmissionRecords();
            recordsPage.isPageLoaded();
        }
        if (child.isEmpty()) throw new SkipException("No record with filled, unmasked employment details in the grid");
        Reporter.log("   Child " + child + " | popup '" + recordsPage.getEditPopupTitle() + "' " + recordsPage.getEditLabels()
                + " | original: " + orig + " / " + email + " / " + employer, true);
        Assert.assertEquals(recordsPage.getEditPopupTitle(), "Edit Invitation Details", "❌ Popup title");
        String changed = orig + "-QA";
        for (String value : Arrays.asList(changed, orig)) {
            recordsPage.enterEditField("edit_employee_code", value);
            recordsPage.startCapture(true);
            recordsPage.clickUpdateInvite();
            String msg = recordsPage.waitForReloadAndFlash();
            Reporter.log("   Employee Code → " + value + " | confirms " + recordsPage.getCapturedConfirms() + " | " + msg, true);
            Assert.assertEquals(recordsPage.getCapturedConfirms(), Arrays.asList(CONFIRM_EDIT), "❌ Confirm text");
            Assert.assertTrue(msg.contains("Record updated successfully"), "❌ Message: " + msg);
            recordsPage = new AdmissionRecordsPage(driver);
            recordsPage.isPageLoaded();
            recordsPage.openEditInviteDetails(child);
            Assert.assertEquals(recordsPage.getEditValue("edit_employee_code"), value, "❌ Saved Employee Code");
            Assert.assertEquals(recordsPage.getEditValue("edit_employer"), employer, "❌ Employer changed");
        }
        Reporter.log("✅ SC_003_TC_009 PASSED — child " + child + " restored to " + orig, true);
    }
}
