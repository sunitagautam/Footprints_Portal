package testScripts.FranchiseTests.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.AdmissionInvitesPage;
import pages.Franchise.CorporateTieups.Tieup_DetailsPage;
import pages.Navigations;
import pages.Settings.SystemLogs.UserRightsPage;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// TIE-UPS DETAILS (Franchise → Corporate Tie-ups → Tie Ups Details)
// Source: TC_Corporate_Tieups.xlsx, SC_001 (converted from
// "TC_Corporate Tie-ups.xlsx" + 10 new cases found while exploring).
// Runs as Rakesh (user, 2026-10-26 — the Shivali Gupta switch fails).
//
// Priorities 1–42: cases that SAVE NOTHING (list, filters, search,
// download, pagination, sorting, popups, form views, validation).
// Priorities 50+: cases that CREATE / UPDATE tie-ups on UAT (user's OK,
// 2026-10-27). Every tie-up they create is titled "QA Auto Tieup … <run id>",
// uses Demo Company + Demo Center only, and is left ACTIVE (user, 2026-10-27:
// test environment — never expire the created tie-ups).
// SC_001_TC_041 switches to Jaydeep Kar, so it runs last.
// ═══════════════════════════════════════════════════════════════
public class Tieup_DetailsTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Tie-up Title", "Centers List", "Expiry Date",
            "Status", "Last Updated", "Contact Details", "Actions");
    private static final List<String> EXPORT_COLUMNS = Arrays.asList("Company Name", "Fee Payer",
            "Monthly Guarantee Type", "Monthly Guarantee Amount", "Monthly Guarantee Admission");
    private static final List<String> ALL_ACTIONS = Arrays.asList("Update", "Tieup History Log", "View Offer",
            "Admission Request");
    private static final List<String> PAYER_OPTIONS = Arrays.asList("Corporate", "Parent", "Waive-off");
    private static final String[] PAYER_IDS = {"registration_fee_payer", "transport_payer", "kit_payer",
            "monthly_fee_payer", "annual_fee_payer"};
    private static final String[] ANNUAL_IDS = {"sel_annual", "offer_annual_chargebee_id", "annual_fee_payer"};
    private static final String REQUIRED = "This field is required.";
    private static final String LEAD_ID = System.getProperty("tieupLeadId", "1"); // lead 1 exists in test CRM (user, 2026-10-27); 179508 does not

    // ✅ Save-test data — Demo Company / Demo Center only (user: OK to create on UAT)
    private static final String RUN_ID = new java.text.SimpleDateFormat("ddMMHHmm").format(new java.util.Date());
    private static final String COMPANY = "Demo Company";
    private static final String CENTER_ID = "100";            // Demo Center (Noida)
    private static final String FULL_DAY = "chk_full_day_3";  // Full Day 09:00 AM to 06:30 PM
    private static final String EVENING = "chk_evening_program_6";
    private static final String CONTACT_EMAIL = "sunita.gautam@footprintseducation.in";
    private static final String ADDED_MSG = "added successfully";
    private static final String UPDATED_MSG = "updated successfully";
    private static String v1Title = "";
    private static String v2Title = "";
    private static String noPlanTitle = "";

    Navigations navigations;
    Tieup_DetailsPage tieupPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        tieupPage = new Tieup_DetailsPage(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToTieupDetails();
        dismissAlertIfPresent();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isListLoaded(), "❌ Tie Ups Details screen did not load");
    }

    private void openAddForm() throws InterruptedException {
        navigations.goToAddCorporateOffer();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isFormLoaded(), "❌ Add Corporate Offer form did not load");
    }

    // ✅ Opens the first listed tie-up's Update form — nothing is saved
    private void openUpdateForm() throws InterruptedException {
        String href = tieupPage.getFirstRowUpdateHref();
        driver.get(href);
        Thread.sleep(2500);
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isFormLoaded(), "❌ Update form did not load: " + href);
        Reporter.log("   Update form: " + href, true);
    }

    private void assertStatusAll(String expected) {
        List<String> statuses = tieupPage.getColumnValues("Status");
        Reporter.log("   Status column: " + statuses, true);
        Assert.assertFalse(statuses.isEmpty(), "❌ No rows to check");
        for (String s : statuses)
            Assert.assertTrue(s.equalsIgnoreCase(expected), "❌ Row with Status '" + s + "', expected " + expected);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_001 — Menu navigation
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_001_TC_001 — Menu Franchise → Corporate Tie-ups → Tie Ups Details")
    public void tc001_menuNavigation() {
        Reporter.log("▶ SC_001_TC_001 — Menu navigation", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='corporate_offers')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu path: " + path, true);
        Assert.assertFalse(driver.findElements(By.cssSelector("a[href='corporate_offers']")).isEmpty(),
                "❌ No menu link to corporate_offers");
        // ✅ Live labels (2026-10-26): "Corporate Tieups" / "Tieup Details" (spec: "Tie Ups Details")
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Tieup Details", "❌ Menu path");
        Assert.assertTrue(tieupPage.getTieupsCount() > 0, "❌ '<N> tie-ups' count not shown");
        Assert.assertTrue(tieupPage.getVisibleRowCount() > 0, "❌ No corporate offers listed");
        Reporter.log("✅ SC_001_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_002 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_001_TC_002 — Default view of Tie Ups Details")
    public void tc002_defaultView() {
        Reporter.log("▶ SC_001_TC_002 — Default view", true);
        Assert.assertTrue(tieupPage.isHeadingVisible(), "❌ Heading 'Corporate Tie-ups'");
        Reporter.log("   Count: " + tieupPage.getTieupsCount() + " tie-ups | " + tieupPage.getInfoText(), true);
        Assert.assertTrue(tieupPage.getTieupsCount() > 0, "❌ '<N> tie-ups' count");
        Assert.assertTrue(tieupPage.isAddNewOfferVisible(), "❌ Add New Corporate Offer button");
        Assert.assertTrue(tieupPage.isCenterFilterPresent(), "❌ Center filter");
        Assert.assertEquals(tieupPage.getSelectedCenterText(), "All", "❌ Center default");
        Assert.assertEquals(tieupPage.getStatusOptions(), Arrays.asList("--All Status--", "Active", "Expired"), "❌ Status options");
        Assert.assertEquals(tieupPage.getSelectedStatus(), "Active", "❌ Status default");
        Assert.assertTrue(tieupPage.isFilterButtonVisible(), "❌ Filter button");
        Assert.assertTrue(tieupPage.isClearLinkVisible(), "❌ Clear link");
        Assert.assertTrue(tieupPage.isSearchBarVisible(), "❌ Search box");
        Assert.assertEquals(tieupPage.getPageLengthOptions(), Arrays.asList("10", "20", "50", "100"), "❌ Per-page options");
        Assert.assertTrue(tieupPage.isDownloadVisible(), "❌ Download button");
        Assert.assertTrue(tieupPage.isPaginationVisible(), "❌ Pagination");
        Assert.assertTrue(tieupPage.getInfoText().matches("Showing 1.10 of \\d+ entries"), "❌ Info: " + tieupPage.getInfoText());
        Assert.assertEquals(tieupPage.getVisibleColumnHeaders(), COLUMNS, "❌ Columns");
        Assert.assertEquals(tieupPage.getFirstRowActionTitles(), ALL_ACTIONS, "❌ Action icons for Rakesh");
        Reporter.log("✅ SC_001_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_003 — Filter by Center and Status
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_001_TC_003 — Filter by Center and Status")
    public void tc003_filterCenterAndStatus() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_003 — Filter by Center and Status", true);
        String center = "";
        for (String c : tieupPage.getColumnValues("Centers List"))
            if (!c.startsWith("All Centers")) {
                center = c.replaceAll("\\s*\\+\\d+ more$", "").trim();
                break;
            }
        if (center.isEmpty()) throw new SkipException("Only 'All Centers' tie-ups on page 1");
        Reporter.log("   Center: " + center, true);
        tieupPage.selectCenter(center);
        tieupPage.selectStatus("Active");
        tieupPage.clickFilter();
        Reporter.log("   " + tieupPage.getInfoText(), true);
        assertStatusAll("Active");
        List<String> fullCentres = tieupPage.getRowsFullCenters();
        for (int i = 0; i < fullCentres.size(); i++)
            Assert.assertTrue(fullCentres.get(i).contains(center) || fullCentres.get(i).contains("All Centers"),
                    "❌ Row " + (i + 1) + " does not include centre " + center + ": " + fullCentres.get(i));
        Assert.assertEquals(tieupPage.getSelectedCenterText(), center, "❌ Center not kept after Filter");

        tieupPage.selectCenter(tieupPage.getFirstCenterOption());
        tieupPage.selectStatus("Expired");
        tieupPage.clickFilter();
        Reporter.log("   Expired: " + tieupPage.getInfoText(), true);
        assertStatusAll("Expired");
        Reporter.log("✅ SC_001_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_004 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_001_TC_004 — Search by tie-up title")
    public void tc004_search() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_004 — Search", true);
        String title = tieupPage.getFirstRowTitle();
        String keyword = title.length() > 20 ? title.substring(0, 20) : title;
        tieupPage.searchTable(keyword);
        Reporter.log("   '" + keyword + "' → " + tieupPage.getInfoText(), true);
        Assert.assertTrue(tieupPage.getVisibleRowCount() > 0, "❌ No match for an existing title");
        for (String t : tieupPage.getColumnValues("Tie-up Title"))
            Assert.assertTrue(t.toLowerCase().contains(keyword.toLowerCase()), "❌ Non-matching row: " + t);

        tieupPage.searchTable("xyz123");
        Assert.assertEquals(tieupPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        tieupPage.clearSearch();
        Reporter.log("✅ SC_001_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_005 — Download (client-side DataTables CSV)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_001_TC_005 — Download report")
    public void tc005_download() throws Exception {
        Reporter.log("▶ SC_001_TC_005 — Download", true);
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        tieupPage.clickDownload();
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
        Reporter.log("   File: " + csv.getName() + " | data lines: " + lines + " | Header: " + header, true);
        Assert.assertNotNull(header, "❌ CSV is empty");
        for (String col : Arrays.asList("Tie-up Title", "Centers List", "Expiry Date", "Status", "Contact Details"))
            Assert.assertTrue(header.contains(col), "❌ CSV missing column: " + col);
        for (String col : EXPORT_COLUMNS)
            Assert.assertTrue(header.contains(col), "❌ CSV missing export column: " + col);
        Assert.assertTrue(lines >= tieupPage.getTotalEntries(), "❌ CSV has fewer rows than the grid");
        Reporter.log("✅ SC_001_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_006 — Pagination and page size
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_001_TC_006 — Pagination and page size")
    public void tc006_pagination() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_006 — Pagination", true);
        Assert.assertEquals(tieupPage.getVisibleRowCount(), Math.min(10, tieupPage.getTotalEntries()), "❌ Default 10 rows");
        Assert.assertFalse(tieupPage.isPreviousPageEnabled(), "❌ Previous enabled on page 1");
        String firstTitle = tieupPage.getFirstRowTitle();
        tieupPage.clickNextPage();
        Reporter.log("   Next → " + tieupPage.getInfoText(), true);
        Assert.assertTrue(tieupPage.getInfoText().startsWith("Showing 11"), "❌ Info after Next: " + tieupPage.getInfoText());
        Assert.assertNotEquals(tieupPage.getFirstRowTitle(), firstTitle, "❌ Same rows after Next");
        tieupPage.clickPreviousPage();
        Assert.assertTrue(tieupPage.getInfoText().startsWith("Showing 1"), "❌ Info after Previous");
        Assert.assertEquals(tieupPage.getFirstRowTitle(), firstTitle, "❌ Page 1 rows after Previous");

        int total = tieupPage.getTotalEntries();
        for (String len : new String[]{"20", "50", "100"}) {
            tieupPage.selectPageLength(len);
            int rows = tieupPage.getVisibleRowCount();
            Reporter.log("   " + len + " per page → " + rows + " rows | " + tieupPage.getInfoText(), true);
            Assert.assertEquals(rows, Math.min(Integer.parseInt(len), total), "❌ Rows for " + len + " per page");
        }
        Reporter.log("✅ SC_001_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_007 — Column sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_001_TC_007 — Column sorting")
    public void tc007_sorting() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_007 — Sorting", true);
        for (String col : Arrays.asList("Tie-up Title", "Centers List", "Expiry Date", "Status", "Last Updated", "Contact Details")) {
            tieupPage.clickColumnHeader(col);
            String first = tieupPage.getHeaderSortClass(col);
            List<String> asc = tieupPage.getColumnValues(col);
            tieupPage.clickColumnHeader(col);
            String second = tieupPage.getHeaderSortClass(col);
            List<String> desc = tieupPage.getColumnValues(col);
            Reporter.log("   " + col + ": " + first + " → " + second, true);
            Assert.assertTrue(first.contains("sorting_asc") || first.contains("sorting_desc"), "❌ " + col + " not sorted: " + first);
            Assert.assertTrue(second.contains("sorting_asc") || second.contains("sorting_desc"), "❌ " + col + " not sorted: " + second);
            Assert.assertNotEquals(first, second, "❌ " + col + " did not toggle direction");
            if (!new ArrayList<>(asc).equals(desc) && asc.size() > 1)
                Reporter.log("     first row " + asc.get(0) + " ↔ " + desc.get(0), true);
        }
        Reporter.log("✅ SC_001_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_008 — Tie-up Title info icon (hover popover)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_001_TC_008 — Tie-up Title information icon")
    public void tc008_infoIcon() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_008 — Info icon", true);
        String content = tieupPage.getFirstRowInfoContent();
        Reporter.log("   " + (content.length() > 300 ? content.substring(0, 300) + "…" : content), true);
        for (String label : Arrays.asList("Fee Type", "Tieup Type", "Monthly Guarantee Type", "Monthly Guarantee Amount",
                "Monthly Guarantee Admission", "Pause Credits Applicable", "Refund Applicable", "Transfer Applicable",
                "Program Change", "User Comment", "Contract Type", "Authorisation", "Plans", "Programs Available",
                "Centers Applicable", "Fee Components"))
            Assert.assertTrue(content.contains(label), "❌ Info popover missing: " + label);
        Reporter.log("   Popover shown on hover: " + tieupPage.hoverFirstRowInfoIcon(), true);
        Reporter.log("✅ SC_001_TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_009 — "+N more" centres popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC_001_TC_009 — '+N more' centres link")
    public void tc009_moreCenters() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_009 — +N more", true);
        int more = tieupPage.getFirstMoreCentersCount();
        if (more < 0) throw new SkipException("No tie-up with '+N more' on page 1");
        tieupPage.clickFirstMoreCenters();
        Assert.assertTrue(tieupPage.isCentersModalVisible(), "❌ Centres popup did not open");
        int count = tieupPage.getCentersModalCount();
        Reporter.log("   +" + more + " more → " + tieupPage.getCentersModalTitle() + " | list items: "
                + tieupPage.getCentersModalListSize(), true);
        Assert.assertTrue(tieupPage.getCentersModalTitle().matches("Applicable Centers \\(\\d+\\)"), "❌ Popup title");
        Assert.assertEquals(tieupPage.getCentersModalListSize(), count, "❌ Listed centres ≠ count in title");
        Assert.assertTrue(count > more, "❌ Total centres not more than the '+N more' number");
        tieupPage.closeCentersModal();
        Assert.assertFalse(tieupPage.isCentersModalVisible(), "❌ Popup did not close with ×");
        Reporter.log("✅ SC_001_TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_010 — Add New Corporate Offer: default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_001_TC_010 — Add New Corporate Offer default view")
    public void tc010_addFormDefaultView() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_010 — Add form default view", true);
        tieupPage.clickAddNewOffer();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isFormLoaded(), "❌ Add Corporate Offer form did not open");
        for (String id : Arrays.asList("offer_title_id", "company_id", "corporate_payment_plan", "sel_registration_fee",
                "registration_fee_payer", "transport_payer", "kit_payer", "kit_price_override", "offer_monthly_chargebee_id",
                "monthly_fee_payer", "user_comments", "start_date", "end_date", "offer_status", "fee_type", "tieup_type",
                "mg_type", "pause_applicable", "refund_applicable", "transfer_applicable", "program_change",
                "contact_name", "contact_phone", "contact_email", "authorisation", "key_account_manager",
                "business_development_manager", "max_age", "create_offer"))
            Assert.assertTrue(tieupPage.isFieldVisible(id) || !driver.findElements(By.id(id)).isEmpty(), "❌ Field missing: " + id);
        Assert.assertEquals(tieupPage.getOptions("corporate_payment_plan"), Arrays.asList("--Select--", "V1", "V2"), "❌ Payment Plan options");
        for (String payer : PAYER_IDS)
            Assert.assertTrue(tieupPage.getOptions(payer).containsAll(PAYER_OPTIONS), "❌ " + payer + ": " + tieupPage.getOptions(payer));
        Assert.assertTrue(tieupPage.getOptions("offer_status").containsAll(Arrays.asList("Active", "Expired")), "❌ Status options");
        Assert.assertTrue(tieupPage.getOptions("fee_type").containsAll(Arrays.asList("Center Fee", "Fixed Fee")), "❌ Fee Type options");
        Assert.assertTrue(tieupPage.getOptions("tieup_type").containsAll(Arrays.asList("Co-Pay", "Corporate Pay", "Employee Pay", "Onsite")), "❌ Tieup Type options");
        Assert.assertTrue(tieupPage.getOptions("mg_type").containsAll(Arrays.asList("Annual", "Monthly")), "❌ Monthly Guarantee Type options");
        Assert.assertTrue(tieupPage.getOptions("authorisation").containsAll(Arrays.asList("By Employer", "Employment Proof")), "❌ Authorisation options");
        Reporter.log("   Programs: " + tieupPage.getProgramCheckboxCount() + " | Centres: " + tieupPage.getCenterCheckboxCount(), true);
        Assert.assertTrue(tieupPage.getProgramCheckboxCount() > 0, "❌ Program checkboxes");
        Assert.assertTrue(tieupPage.getCenterCheckboxCount() > 0, "❌ Centre checkboxes");
        for (String label : Arrays.asList("Max Age Allowed", "Contract File", "Kit Price"))
            Assert.assertTrue(tieupPage.pageHasText(label), "❌ Label missing: " + label);
        Reporter.log("✅ SC_001_TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_011 / TC_012 (step 2 only) — Plan V1 / V2 annual section
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "SC_001_TC_011 — Payment Plan V1 shows the Annual Fee section (step 2, no save)")
    public void tc011_planV1ShowsAnnual() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_011 — Plan V1 (no save)", true);
        openAddForm();
        tieupPage.selectByText("corporate_payment_plan", "V1");
        for (String id : ANNUAL_IDS)
            Assert.assertTrue(tieupPage.isFieldVisible(id), "❌ V1: " + id + " not shown");
        Reporter.log("✅ SC_001_TC_011 (step 2) PASSED", true);
    }

    @Test(priority = 12, description = "SC_001_TC_012 — Payment Plan V2 hides the Annual Fee section (step 2, no save)")
    public void tc012_planV2HidesAnnual() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_012 — Plan V2 (no save)", true);
        openAddForm();
        tieupPage.selectByText("corporate_payment_plan", "V2");
        for (String id : ANNUAL_IDS)
            Assert.assertFalse(tieupPage.isFieldVisible(id), "❌ V2: " + id + " still shown");
        Reporter.log("✅ SC_001_TC_012 (step 2) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_014 (step 2) — No plan: Annual section (QueryLogs Q1)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "SC_001_TC_014 — No Payment Plan: Annual section shown and required (as per requirement)")
    public void tc014_noPlanAnnualSection() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_014 — No plan: Annual section", true);
        openAddForm();
        Assert.assertEquals(tieupPage.getSelectedText("corporate_payment_plan"), "--Select--", "❌ Plan default");
        // ✅ As per requirement (user, 2026-10-27 — Q1 closed): with no plan the Annual section stays visible
        for (String id : ANNUAL_IDS)
            Assert.assertTrue(tieupPage.isFieldVisible(id), "❌ No plan: " + id + " not shown");
        Assert.assertTrue(tieupPage.isFieldVisible("annual_fee_payer"), "❌ Annual Fee Payer not shown");
        Reporter.log("✅ SC_001_TC_014 (step 2) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_015 / TC_038 — Mandatory field validation
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "SC_001_TC_015 — Mandatory field validation (empty Submit, nothing saved)")
    public void tc015_mandatoryValidation() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_015 — Mandatory validation", true);
        openAddForm();
        tieupPage.startAjaxCapture();
        tieupPage.clickSubmit();
        List<String> errors = tieupPage.getVisibleErrors();
        errors.forEach(e -> Reporter.log("   " + e, true));
        String all = String.join("\n", errors);
        for (String id : Arrays.asList("offer_title_id", "company_id", "registration_fee_payer", "transport_payer",
                "kit_payer", "monthly_fee_payer", "annual_fee_payer", "centers_id[]", "start_date", "fee_type",
                "tieup_type", "contact_name", "contact_phone", "contact_email", "authorisation", "max_age"))
            Assert.assertTrue(all.contains(id + " => " + REQUIRED), "❌ No '" + REQUIRED + "' for " + id);
        for (String msg : Arrays.asList("Please select at least one program.", "Please select offer status", "Contract file is required."))
            Assert.assertTrue(all.contains(msg), "❌ Missing message: " + msg);
        Assert.assertTrue(driver.getCurrentUrl().endsWith("add_corporate_offer"), "❌ Page left after empty Submit");
        Assert.assertTrue(tieupPage.getCapturedAjax().isEmpty(), "❌ Request sent: " + tieupPage.getCapturedAjax());
        Reporter.log("✅ SC_001_TC_015 / TC_038 (step 2) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_019 — Contact Phone length
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 19, description = "SC_001_TC_019 — Contact Phone length validation")
    public void tc019_phoneLength() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_019 — Phone length", true);
        openAddForm();
        tieupPage.enterField("contact_phone", "9876543");
        tieupPage.clickSubmit();
        String all = String.join("\n", tieupPage.getVisibleErrors());
        Assert.assertTrue(all.contains("contact_phone => Please enter at least 10 characters."), "❌ Min-length error:\n" + all);

        tieupPage.enterField("contact_phone", "987654332123456");
        tieupPage.clickSubmit();
        all = String.join("\n", tieupPage.getVisibleErrors());
        Reporter.log("   15 digits → value '" + tieupPage.getFieldValue("contact_phone") + "' | max-length error shown: "
                + all.contains("no more than 13") + " (QueryLogs Q3)", true);
        Assert.assertTrue(all.contains("contact_phone => Please enter no more than 13 characters.")
                || tieupPage.getFieldValue("contact_phone").length() <= 13, "❌ 15-digit phone accepted with no error");
        Reporter.log("✅ SC_001_TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_020 — Contact Email format
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC_001_TC_020 — Contact Email format validation")
    public void tc020_emailFormat() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_020 — Email format", true);
        openAddForm();
        tieupPage.enterField("contact_email", "test@");
        tieupPage.clickSubmit();
        String all = String.join("\n", tieupPage.getVisibleErrors());
        Assert.assertTrue(all.contains("contact_email => Please enter a valid email address."), "❌ Email error:\n" + all);
        Reporter.log("✅ SC_001_TC_020 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_021 — BACK button
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 21, description = "SC_001_TC_021 — BACK returns to the list")
    public void tc021_backButton() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_021 — BACK", true);
        int before = tieupPage.getTieupsCount();
        openAddForm();
        tieupPage.enterField("offer_title_id", "QA Auto BACK check");
        tieupPage.clickBack();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isListLoaded(), "❌ BACK did not return to the list: " + driver.getCurrentUrl());
        Assert.assertEquals(tieupPage.getTieupsCount(), before, "❌ Tie-up count changed after BACK");
        Reporter.log("✅ SC_001_TC_021 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_025 — Update form: Waive-off disables discount (no save)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 25, description = "SC_001_TC_025 — Update form: Waive-off disables discount (no save)")
    public void tc025_updateWaiveOff() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_025 — Update Waive-off (no save)", true);
        openUpdateForm();
        assertWaiveOffToggles();
        Reporter.log("✅ SC_001_TC_025 (steps 1–4, not saved) PASSED", true);
    }

    // ✅ Shared by TC_025 (Update) and TC_040 (Add)
    private void assertWaiveOffToggles() throws InterruptedException {
        if (!tieupPage.isFieldVisible("offer_annual_chargebee_id"))
            tieupPage.selectByText("corporate_payment_plan", "V1");
        tieupPage.selectByText("monthly_fee_payer", "Waive-off");
        Assert.assertTrue(tieupPage.isFieldDisabled("offer_monthly_chargebee_id"), "❌ Monthly Fee Offer not disabled");
        tieupPage.selectByText("annual_fee_payer", "Waive-off");
        Assert.assertTrue(tieupPage.isFieldDisabled("offer_annual_chargebee_id"), "❌ Annual Fee Offer not disabled");
        tieupPage.selectByText("monthly_fee_payer", "Corporate");
        tieupPage.selectByText("annual_fee_payer", "Corporate");
        Assert.assertFalse(tieupPage.isFieldDisabled("offer_monthly_chargebee_id"), "❌ Monthly Fee Offer still disabled");
        Assert.assertFalse(tieupPage.isFieldDisabled("offer_annual_chargebee_id"), "❌ Annual Fee Offer still disabled");
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_026 (step 2) — Payer options per fee component
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 26, description = "SC_001_TC_026 — Update form: payer options per fee component (step 2, no save)")
    public void tc026_payerOptions() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_026 — Payer options (no save)", true);
        openUpdateForm();
        Assert.assertTrue(tieupPage.isFieldVisible("update_reason"), "❌ Reason for Change field missing on Update");
        for (String payer : PAYER_IDS) {
            List<String> opts = tieupPage.getOptions(payer);
            Reporter.log("   " + payer + ": " + opts, true);
            Assert.assertTrue(opts.containsAll(PAYER_OPTIONS), "❌ " + payer + " options: " + opts);
        }
        Reporter.log("✅ SC_001_TC_026 (step 2) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_027 — View Offer popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 27, description = "SC_001_TC_027 — View Offer popup")
    public void tc027_viewOffer() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_027 — View Offer", true);
        String title = tieupPage.getFirstRowTitle();
        tieupPage.clickFirstRowViewOffer();
        Assert.assertTrue(tieupPage.isPopdownVisible(), "❌ View Offer popup did not open");
        String text = tieupPage.getPopdownText();
        for (String s : Arrays.asList("Corporate Tie-up Details", "Basic Info", "Tie-up Name", "Enabled Date", "Valid Till",
                "Status", "Applicable Centers", "Contract & Fee", "Contract Type", "Monthly Guarantee Type", "Fee Type",
                "Authorisation", "Applicability", "Plans", "Programs Available", "Pause Credits", "Refund", "Transfer",
                "Program Change", "Fee Components", "Calculate Fee", "Contact & Files", "SPOC", "Comments"))
            Assert.assertTrue(text.contains(s), "❌ View Offer missing: " + s);
        Assert.assertTrue(text.contains(title.trim()), "❌ View Offer is not for '" + title + "'");
        tieupPage.closePopdown();
        Reporter.log("✅ SC_001_TC_027 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_028 — Admission Request popup default view (no send)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 28, description = "SC_001_TC_028 — Admission Request popup default view (no send)")
    public void tc028_admissionRequestView() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_028 — Admission Request popup", true);
        tieupPage.clickFirstRowAdmissionRequest();
        Assert.assertTrue(tieupPage.isPopdownVisible(), "❌ Admission Request popup did not open");
        String text = tieupPage.getPopdownText();
        Assert.assertTrue(text.contains("Add New Admission Request"), "❌ Popup title");
        Assert.assertTrue(text.contains("Lead Id") && text.contains("Fetch Details"), "❌ Lead Id / Fetch Details");
        Assert.assertTrue(tieupPage.isPopdownFieldVisible("lead_id"), "❌ Lead Id field");
        // Step 3 — Fetch Details is a read (test CRM getLeadDetails)
        tieupPage.fetchLead(LEAD_ID);
        Assert.assertTrue(tieupPage.isLeadDetailsShown(), "❌ No lead details for lead " + LEAD_ID + ": " + tieupPage.getPopdownText());
        Reporter.log("   Lead " + LEAD_ID + ": " + tieupPage.getLeadName() + " | " + tieupPage.getLeadEmail() + " | " + tieupPage.getLeadPhone(), true);
        Assert.assertFalse(tieupPage.getLeadName().isEmpty(), "❌ Lead name");
        Assert.assertFalse(tieupPage.getLeadEmail().isEmpty(), "❌ Lead email");
        Assert.assertFalse(tieupPage.getLeadPhone().isEmpty(), "❌ Lead phone");
        Assert.assertEquals(tieupPage.getPopupOptions("invite_parent_type"), Arrays.asList("--SELECT--", "Mother", "Father"), "❌ Lead Details of");
        for (String id : Arrays.asList("centers_available", "payment_plan_avail", "program_offered", "admission_request_date",
                "admission_request_comment", "employee_code", "employee_email", "employer", "send_request"))
            Assert.assertTrue(tieupPage.isPopdownFieldVisible(id), "❌ Field missing after Fetch: " + id);
        String popup = tieupPage.getPopdownText();
        for (String label : Arrays.asList("Corporate Discounts", "Fee Paid By", "Tentative Admission Date"))
            Assert.assertTrue(popup.contains(label), "❌ Missing: " + label);
        tieupPage.closePopdown();
        Reporter.log("✅ SC_001_TC_028 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_033 — Clear filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 33, description = "SC_001_TC_033 — Clear resets the filters")
    public void tc033_clearFilters() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_033 — Clear", true);
        int activeTotal = tieupPage.getTotalEntries();
        tieupPage.selectStatus("Expired");
        tieupPage.clickFilter();
        assertStatusAll("Expired");
        tieupPage.clickClear();
        Assert.assertEquals(tieupPage.getSelectedStatus(), "Active", "❌ Status not reset");
        Assert.assertEquals(tieupPage.getSelectedCenterText(), "All", "❌ Center not reset");
        Assert.assertEquals(tieupPage.getTotalEntries(), activeTotal, "❌ Default list not shown after Clear");
        Reporter.log("✅ SC_001_TC_033 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_034 — Default Status filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 34, description = "SC_001_TC_034 — Default Status Active; --All Status-- shows all")
    public void tc034_defaultStatus() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_034 — Default Status", true);
        Assert.assertEquals(tieupPage.getSelectedStatus(), "Active", "❌ Default Status");
        assertStatusAll("Active");
        int active = tieupPage.getTotalEntries();
        tieupPage.selectStatus("--All Status--");
        tieupPage.clickFilter();
        int all = tieupPage.getTotalEntries();
        Reporter.log("   Active " + active + " → All " + all, true);
        Assert.assertTrue(all > active, "❌ --All Status-- did not show more tie-ups");
        Reporter.log("✅ SC_001_TC_034 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_035 — Tieup History Log popup
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 35, description = "SC_001_TC_035 — Tieup History Log popup")
    public void tc035_historyLog() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_035 — History Log", true);
        String title = tieupPage.getFirstRowTitle();
        tieupPage.clickFirstRowHistoryLog();
        Assert.assertTrue(tieupPage.isPopdownVisible(), "❌ History Log popup did not open");
        String text = tieupPage.getPopdownText();
        Assert.assertTrue(text.contains("Tieup History Log"), "❌ Popup title");
        Assert.assertTrue(text.contains(title.trim()), "❌ Title does not name '" + title + "'");
        Assert.assertEquals(tieupPage.getHistoryHeaders(), Arrays.asList("Updated On", "Updated By", "Update Details"), "❌ Columns");
        Reporter.log("   Entries: " + tieupPage.getHistoryRowCount(), true);
        if (tieupPage.getHistoryRowCount() > 0) {
            List<String> dates = tieupPage.getHistoryDates();
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd MMM, yyyy", java.util.Locale.ENGLISH);
            for (int i = 1; i < dates.size(); i++)
                Assert.assertFalse(java.time.LocalDate.parse(dates.get(i), f).isAfter(java.time.LocalDate.parse(dates.get(i - 1), f)),
                        "❌ Not newest first: " + dates.get(i - 1) + " then " + dates.get(i));
            String log = tieupPage.getFirstHistoryLogDetails();
            Reporter.log("   " + log, true);
            for (String s : Arrays.asList("Log Details", "Changed By", "Date", "Reason for Change", "Changes"))
                Assert.assertTrue(log.contains(s), "❌ Log Details missing: " + s);
            // ✅ Step 3 is about what the user SEES on hover, not the data-content attribute
            String shown = tieupPage.getFirstHistoryPopoverRenderedText();
            Reporter.log("   Rendered popover: " + shown, true);
            java.util.regex.Matcher by = java.util.regex.Pattern.compile("Changed By: (.+?) Date:").matcher(log);
            java.util.regex.Matcher why = java.util.regex.Pattern.compile("Reason for Change: (.+?) Changes:").matcher(log);
            if (by.find()) Assert.assertTrue(shown.contains(by.group(1).trim()), "❌ Popover does not show Changed By '" + by.group(1).trim() + "': " + shown);
            if (why.find()) Assert.assertTrue(shown.contains(why.group(1).trim()), "❌ Popover does not show the Reason '" + why.group(1).trim() + "': " + shown);
        }
        Assert.assertTrue(text.contains("Close"), "❌ Close button");
        tieupPage.closePopdown();
        Reporter.log("✅ SC_001_TC_035 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_037 (steps 1–2) — Monthly Guarantee fields
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 37, description = "SC_001_TC_037 — Monthly Guarantee Type shows Amount / Admission (steps 1–2, no save)")
    public void tc037_monthlyGuaranteeFields() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_037 — Monthly Guarantee (no save)", true);
        openAddForm();
        Reporter.log("   Default: mg_amount shown=" + tieupPage.isFieldVisible("mg_amount")
                + " | mg_sheet shown=" + tieupPage.isFieldVisible("mg_sheet"), true);
        Assert.assertFalse(tieupPage.isFieldVisible("mg_amount"), "❌ Amount shown before a type is chosen");
        for (String type : Arrays.asList("Annual", "Monthly")) {
            tieupPage.selectByText("mg_type", type);
            Assert.assertTrue(tieupPage.isFieldVisible("mg_amount"), "❌ " + type + ": Monthly Guarantee Amount not shown");
            Assert.assertTrue(tieupPage.isFieldVisible("mg_sheet"), "❌ " + type + ": Monthly Guarantee Admission not shown");
        }
        Reporter.log("✅ SC_001_TC_037 (steps 1–2) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_040 — Add form: Waive-off disables discount
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 40, description = "SC_001_TC_040 — Add form: Waive-off disables discount")
    public void tc040_addWaiveOff() throws InterruptedException {
        Reporter.log("▶ SC_001_TC_040 — Add Waive-off", true);
        openAddForm();
        assertWaiveOffToggles();
        Reporter.log("✅ SC_001_TC_040 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_042 — Contact Details column
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 42, description = "SC_001_TC_042 — Contact Details column shows the SPOC")
    public void tc042_contactDetails() {
        Reporter.log("▶ SC_001_TC_042 — Contact Details", true);
        List<String> contacts = tieupPage.getColumnValues("Contact Details");
        contacts.forEach(c -> Reporter.log("   " + c, true));
        Assert.assertFalse(contacts.isEmpty(), "❌ No rows");
        Assert.assertTrue(contacts.stream().anyMatch(c -> c.contains("Tieup SPOC")), "❌ No 'Tieup SPOC' contact shown");
        Reporter.log("✅ SC_001_TC_042 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SAVE HELPERS
    // ════════════════════════════════════════════════════════════════════
    private static final String MINIMAL_PDF = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/MediaBox[0 0 200 200]/Parent 2 0 R>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n";

    // ✅ Test files are generated per run (nothing binary in the repo)
    private String makeFile(String name, byte[] content) throws Exception {
        Path dir = Files.createDirectories(new File(System.getProperty("java.io.tmpdir"), "tieup_files").toPath());
        Path f = dir.resolve(name);
        Files.write(f, content);
        return f.toAbsolutePath().toString();
    }

    private String contractPdf() throws Exception {
        return makeFile("Testnote.pdf", MINIMAL_PDF.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    // ✅ Fills every mandatory field. plan = "V1" / "V2" / null (no plan)
    private void fillNewOffer(String title, String plan, String feeType) throws Exception {
        tieupPage.enterField("offer_title_id", title);
        tieupPage.selectByText("company_id", COMPANY);
        if (plan != null) tieupPage.selectByText("corporate_payment_plan", plan);
        tieupPage.enterField("sel_registration_fee", "5000");
        tieupPage.selectByText("registration_fee_payer", "Corporate");
        tieupPage.selectByText("transport_payer", "Parent");
        tieupPage.selectByText("kit_payer", "Parent");
        tieupPage.selectByText("monthly_fee_payer", "Corporate");
        if (tieupPage.isFieldVisible("annual_fee_payer")) tieupPage.selectByText("annual_fee_payer", "Corporate");
        tieupPage.checkCenter(CENTER_ID);
        tieupPage.setStartDateToday();
        tieupPage.selectByText("offer_status", "Active");
        tieupPage.selectByText("fee_type", feeType);
        tieupPage.selectByText("tieup_type", "Corporate Pay");
        tieupPage.enterField("contact_name", "QA Auto SPOC");
        tieupPage.enterField("contact_phone", "9876543210");
        tieupPage.enterField("contact_email", CONTACT_EMAIL);
        tieupPage.selectByText("authorisation", "By Employer");
        tieupPage.enterField("max_age", "6");
        tieupPage.enterField("user_comments", "Created by automation (Tieup_DetailsTest) — safe to ignore.");
    }

    private void uploadContract() throws Exception {
        tieupPage.chooseContractFile(contractPdf());
        String path = tieupPage.waitForContractUpload(30);
        Reporter.log("   Contract uploaded: " + path + " | caption: " + tieupPage.getContractFileCaption(), true);
        Assert.assertFalse(path.isEmpty(), "❌ Contract file did not upload: " + tieupPage.getContractFileError());
    }

    // ✅ Submit → page reload → returns the visible flash texts (joined)
    private String submitAndReadMessage() throws InterruptedException {
        tieupPage.clickSubmit();
        Thread.sleep(2500);
        dismissAlertIfPresent();
        String msg = String.join(" || ", tieupPage.getFlashMessages());
        Reporter.log("   After Submit: " + driver.getCurrentUrl() + " | " + msg, true);
        List<String> errors = tieupPage.getVisibleErrors();
        Assert.assertTrue(errors.isEmpty(), "❌ Validation errors on Submit: " + errors);
        return msg;
    }

    private void assertListed(String title) throws InterruptedException {
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isListLoaded(), "❌ List did not load");
        String row = tieupPage.getRowTextByTitle(title);
        Reporter.log("   Listed: " + row, true);
        Assert.assertFalse(row.isEmpty(), "❌ '" + title + "' is not listed");
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_011 (step 3) / TC_016 / TC_037 (step 3) — Create V1 tie-up
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 50, description = "SC_001_TC_011 — Create a Payment Plan V1 tie-up (+TC_016 contract upload, TC_037 Monthly Guarantee)")
    public void tc050_createV1Tieup() throws Exception {
        Reporter.log("▶ SC_001_TC_011 / TC_016 / TC_037 — Create V1 (SAVES)", true);
        v1Title = "QA Auto Tieup V1 " + RUN_ID;
        openAddForm();
        fillNewOffer(v1Title, "V1", "Fixed Fee");
        tieupPage.checkProgram(FULL_DAY, "Rs. 7000.00");
        tieupPage.checkProgram(EVENING, "Rs. 1499.00");
        tieupPage.selectByIndexIfPresent("sel_annual", 1);
        tieupPage.selectByIndexIfPresent("offer_annual_chargebee_id", 1);
        tieupPage.selectByText("mg_type", "Annual");
        tieupPage.enterField("mg_amount", "2500");
        tieupPage.enterField("mg_sheet", "5");
        uploadContract();
        String msg = submitAndReadMessage();
        Assert.assertTrue(msg.toLowerCase().contains(ADDED_MSG), "❌ No success message: " + msg);
        assertListed(v1Title);
        Reporter.log("✅ SC_001_TC_011 (step 3) PASSED — " + v1Title, true);
    }

    private void requireCreated(String title, String what) {
        if (title.isEmpty()) throw new SkipException(what + " was not created in this run");
    }

    // ✅ Opens the Update form of a tie-up created by this run
    private void openUpdateFor(String title) throws InterruptedException {
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isListLoaded(), "❌ List did not load");
        String href = tieupPage.findUpdateHrefByTitle(title);
        Assert.assertFalse(href.isEmpty(), "❌ No Update link for '" + title + "'");
        driver.get(href);
        Thread.sleep(2500);
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isFormLoaded(), "❌ Update form did not load: " + href);
        Assert.assertEquals(tieupPage.getFieldValue("offer_title_id"), title, "❌ Update form is for another tie-up");
    }

    // ✅ UPDATE with a reason → returns the flash texts
    private String updateWithReason(String reason) throws InterruptedException {
        tieupPage.enterUpdateReason(reason);
        tieupPage.clickUpdate();
        dismissAlertIfPresent();
        String msg = String.join(" || ", tieupPage.getFlashMessages());
        Reporter.log("   After UPDATE: " + driver.getCurrentUrl() + " | " + msg, true);
        Assert.assertTrue(tieupPage.getVisibleErrors().isEmpty(), "❌ Validation errors on UPDATE: " + tieupPage.getVisibleErrors());
        Assert.assertTrue(msg.toLowerCase().contains(UPDATED_MSG), "❌ No update success message: " + msg);
        return msg;
    }

    // ✅ View Offer popup text of a tie-up (searched by title)
    private String viewOfferText(String title) throws InterruptedException {
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        tieupPage.searchTable(title);
        tieupPage.clickFirstRowViewOffer();
        Assert.assertTrue(tieupPage.isPopdownVisible(), "❌ View Offer did not open");
        String text = tieupPage.getPopdownText();
        tieupPage.closePopdown();
        return text;
    }

    // ✅ Newest Tieup History Log entry (Log Details text) of a tie-up
    private String latestHistory(String title) throws InterruptedException {
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        tieupPage.searchTable(title);
        tieupPage.clickFirstRowHistoryLog();
        Assert.assertTrue(tieupPage.isPopdownVisible(), "❌ History Log did not open");
        String log = tieupPage.getFirstHistoryLogDetails();
        tieupPage.closePopdown();
        Reporter.log("   Latest history: " + log, true);
        return log;
    }

    private String today() {
        return new java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.ENGLISH).format(new java.util.Date());
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_016 (step 3) / TC_037 (step 3) — Created V1 in View Offer
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 51, description = "SC_001_TC_016 / TC_037 — View Offer of the created V1 tie-up: contract + Monthly Guarantee")
    public void tc051_viewCreatedV1() throws Exception {
        Reporter.log("▶ SC_001_TC_016 / TC_037 — View created V1", true);
        requireCreated(v1Title, "V1 tie-up");
        String text = viewOfferText(v1Title);
        Assert.assertTrue(text.contains(v1Title), "❌ View Offer not for " + v1Title);
        Assert.assertTrue(text.contains("Download Contract"), "❌ Download Contract missing");
        Assert.assertTrue(text.contains("Monthly Guarantee Type: Annual"), "❌ MG Type in View Offer");
        Assert.assertTrue(text.contains("2,500.00") || text.contains("2500"), "❌ MG Amount in View Offer");
        Assert.assertTrue(text.contains("Monthly Guarantee Admission: 5"), "❌ MG Admission in View Offer");
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        tieupPage.searchTable(v1Title);
        String info = tieupPage.getFirstRowInfoContent();
        Assert.assertTrue(info.contains("Monthly Guarantee Type : Annual") && info.contains("Monthly Guarantee Amount : 2500"),
                "❌ MG in info popover: " + info);
        Reporter.log("✅ SC_001_TC_016 / TC_037 (step 3) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_012 (step 3) + TC_013 — Create V2 tie-up, one program without fee
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 52, description = "SC_001_TC_012 / TC_013 — Create a V2 tie-up; Evening Program left without a flat fee")
    public void tc052_createV2Tieup() throws Exception {
        Reporter.log("▶ SC_001_TC_012 / TC_013 — Create V2 (SAVES)", true);
        v2Title = "QA Auto Tieup V2 " + RUN_ID;
        openAddForm();
        fillNewOffer(v2Title, "V2", "Fixed Fee");
        for (String id : ANNUAL_IDS)
            Assert.assertFalse(tieupPage.isFieldVisible(id), "❌ V2: " + id + " shown");
        tieupPage.checkProgram(FULL_DAY, "Rs. 7000.00");
        tieupPage.checkProgram(EVENING, null);
        uploadContract();
        String msg = submitAndReadMessage();
        Assert.assertTrue(msg.toLowerCase().contains(ADDED_MSG), "❌ No success message: " + msg);
        assertListed(v2Title);
        String text = viewOfferText(v2Title);
        Reporter.log("   View Offer fee lines: " + text.replaceAll("(?s).*Fee Components", "Fee Components").replaceAll("(?s)Contact & Files.*", ""), true);
        Assert.assertTrue(text.contains("Full Day"), "❌ Full Day missing");
        Assert.assertTrue(text.contains("Evening Program"), "❌ Evening Program missing");
        Reporter.log("✅ SC_001_TC_012 / TC_013 PASSED — " + v2Title, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_014 (step 3) — Create a tie-up with no Payment Plan
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 53, description = "SC_001_TC_014 — Create a tie-up with no Payment Plan (Fee Type Center Fee)")
    public void tc053_createNoPlanTieup() throws Exception {
        Reporter.log("▶ SC_001_TC_014 — Create no plan (SAVES)", true);
        noPlanTitle = "QA Auto Tieup NoPlan " + RUN_ID;
        openAddForm();
        fillNewOffer(noPlanTitle, null, "Center Fee");
        tieupPage.checkProgram(FULL_DAY, null);
        Reporter.log("   Full Day fee dropdown shown with no plan: " + tieupPage.isFieldVisible("sel_full_day_3"), true);
        Assert.assertFalse(tieupPage.isFieldVisible("sel_full_day_3"), "❌ Flat fee dropdown shown with no plan");
        uploadContract();
        String msg = submitAndReadMessage();
        Assert.assertTrue(msg.toLowerCase().contains(ADDED_MSG), "❌ No success message: " + msg);
        assertListed(noPlanTitle);
        Reporter.log("✅ SC_001_TC_014 (step 3) PASSED — " + noPlanTitle, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_030 — Several offers for the same company
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 54, description = "SC_001_TC_030 — Several offers for the same company are listed")
    public void tc054_multipleOffersSameCompany() throws Exception {
        Reporter.log("▶ SC_001_TC_030 — Same company", true);
        requireCreated(v1Title, "V1 tie-up");
        requireCreated(v2Title, "V2 tie-up");
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        for (String t : Arrays.asList(v1Title, v2Title)) {
            String row = tieupPage.getRowTextByTitle(t);
            Assert.assertTrue(row.contains("| " + COMPANY + " |"), "❌ " + t + " not listed for " + COMPANY + ": " + row);
        }
        Reporter.log("✅ SC_001_TC_030 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_017 — Remove an uploaded contract file (no save)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 55, description = "SC_001_TC_017 — Remove the uploaded contract file")
    public void tc055_removeContractFile() throws Exception {
        Reporter.log("▶ SC_001_TC_017 — Remove contract file (no save)", true);
        openAddForm();
        tieupPage.selectByText("company_id", COMPANY);
        uploadContract();
        tieupPage.clickRemoveContractFile();
        Reporter.log("   After Remove: caption '" + tieupPage.getContractFileCaption() + "' | doc_type_file '"
                + tieupPage.getFieldValue("doc_type_file") + "'", true);
        tieupPage.clickSubmit();
        String all = String.join("\n", tieupPage.getVisibleErrors());
        Assert.assertTrue(driver.getCurrentUrl().endsWith("add_corporate_offer"), "❌ Form was submitted");
        Assert.assertTrue(all.contains("doc_type_file => Contract file is required."),
                "❌ No 'Contract file is required.' after Remove — doc_type_file still '" + tieupPage.getFieldValue("doc_type_file") + "'");
        Reporter.log("✅ SC_001_TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_018 — Contract file validation (no save)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 56, description = "SC_001_TC_018 — Contract file: wrong format, over 8 MB, missing")
    public void tc056_contractFileValidation() throws Exception {
        Reporter.log("▶ SC_001_TC_018 — Contract file validation (no save)", true);
        openAddForm();
        tieupPage.chooseContractFile(makeFile("test.png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}));
        String wrongType = tieupPage.getContractFileError();
        Reporter.log("   .png → '" + wrongType + "'", true);
        Assert.assertEquals(wrongType, "Please select correct file type", "❌ Wrong-format message");

        openAddForm();
        tieupPage.chooseContractFile(makeFile("big_contract.pdf", new byte[9 * 1000 * 1000]));
        String tooBig = tieupPage.getContractFileError();
        Reporter.log("   9 MB pdf → '" + tooBig + "'", true);
        Assert.assertEquals(tooBig, "File should be less than 8 MB", "❌ Over-size message");
        Assert.assertEquals(tieupPage.getFieldValue("doc_type_file"), "", "❌ Over-size file was uploaded");

        openAddForm();
        tieupPage.clickSubmit();
        Assert.assertTrue(String.join("\n", tieupPage.getVisibleErrors()).contains("doc_type_file => Contract file is required."),
                "❌ Missing-file message");
        Reporter.log("✅ SC_001_TC_018 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_039 — Kit Price override saved (blank = standard price)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 57, description = "SC_001_TC_039 — Kit Price override: a value is saved, blank stays blank")
    public void tc057_kitPriceOverride() throws Exception {
        Reporter.log("▶ SC_001_TC_039 — Kit Price override (SAVES)", true);
        String kitTitle = "QA Auto Tieup Kit " + RUN_ID;
        openAddForm();
        // ✅ Live label: "Kit Price" + <small>(blank = standard price)</small> — no space between them
        Assert.assertTrue(tieupPage.pageHasText("Kit Price") && tieupPage.pageHasText("(blank = standard price)"), "❌ Kit Price label");
        fillNewOffer(kitTitle, "V2", "Fixed Fee");
        tieupPage.checkProgram(FULL_DAY, "Rs. 7000.00");
        tieupPage.enterField("kit_price_override", "2500");
        uploadContract();
        String msg = submitAndReadMessage();
        Assert.assertTrue(msg.toLowerCase().contains(ADDED_MSG), "❌ No success message: " + msg);
        openUpdateFor(kitTitle);
        Reporter.log("   Saved Kit Price: '" + tieupPage.getFieldValue("kit_price_override") + "'", true);
        Assert.assertEquals(Double.parseDouble(tieupPage.getFieldValue("kit_price_override")), 2500.0, "❌ Kit Price not saved");
        // ✅ A tie-up created with Kit Price blank (this run's V1, or -DblankKitTieup=<title>)
        String blankKit = v1Title.isEmpty() ? System.getProperty("blankKitTieup", "") : v1Title;
        if (!blankKit.isEmpty()) {
            String v1Title = blankKit;
            openUpdateFor(v1Title);
            Reporter.log("   Kit Price of " + v1Title + " (created blank): '" + tieupPage.getFieldValue("kit_price_override") + "'", true);
            // ✅ As per requirement (user, 2026-10-27 — Q7 closed): a blank Kit Price is stored as 0.00 = standard price
            String kit = tieupPage.getFieldValue("kit_price_override");
            Assert.assertTrue(kit.isEmpty() || Double.parseDouble(kit) == 0.0, "❌ Blank Kit Price saved as " + kit);
        }
        Reporter.log("✅ SC_001_TC_039 (step 2: save) PASSED — " + kitTitle, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_036 + TC_022 — Reason for Change mandatory, logged, Last Updated
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 60, description = "SC_001_TC_036 / TC_022 — Update needs a Reason for Change; change is logged")
    public void tc060_updateReasonMandatoryAndLogged() throws Exception {
        Reporter.log("▶ SC_001_TC_036 / TC_022 — Reason for Change (SAVES)", true);
        requireCreated(v1Title, "V1 tie-up");
        openUpdateFor(v1Title);
        tieupPage.enterField("max_age", "7");
        tieupPage.clickUpdate();
        String all = String.join("\n", tieupPage.getVisibleErrors());
        Reporter.log("   Empty reason → " + all, true);
        Assert.assertTrue(all.contains("update_reason => Reason for change is required when updating tie-up details."),
                "❌ Reason-required message:\n" + all);
        String reason = "QA auto: Max Age 6 to 7 " + RUN_ID;
        updateWithReason(reason);
        String row = tieupPage.getRowTextByTitle(v1Title);
        Assert.assertTrue(row.contains(today()), "❌ Last Updated is not today (" + today() + "): " + row);
        String log = latestHistory(v1Title);
        Assert.assertTrue(log.contains(reason), "❌ Reason not in history: " + log);
        Assert.assertTrue(log.contains("Rakesh Singh"), "❌ Changed By not in history");
        Assert.assertTrue(log.contains("6") && log.contains("7"), "❌ Old → new value not in history");
        Reporter.log("✅ SC_001_TC_036 / TC_022 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_032 — KAM and BDM
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 61, description = "SC_001_TC_032 — KAM and BDM are saved")
    public void tc061_updateKamBdm() throws Exception {
        Reporter.log("▶ SC_001_TC_032 — KAM / BDM (SAVES)", true);
        requireCreated(v1Title, "V1 tie-up");
        openUpdateFor(v1Title);
        List<String> kams = tieupPage.getOptions("key_account_manager");
        String kam = kams.get(1), bdm = tieupPage.getOptions("business_development_manager").get(2);
        tieupPage.selectByText("key_account_manager", kam);
        tieupPage.selectByText("business_development_manager", bdm);
        updateWithReason("QA auto: KAM " + kam + ", BDM " + bdm);
        openUpdateFor(v1Title);
        Assert.assertEquals(tieupPage.getSelectedText("key_account_manager"), kam, "❌ KAM not kept");
        Assert.assertEquals(tieupPage.getSelectedText("business_development_manager"), bdm, "❌ BDM not kept");
        String log = latestHistory(v1Title);
        Assert.assertTrue(log.contains("QA auto: KAM"), "❌ KAM/BDM change not in history: " + log);
        Reporter.log("✅ SC_001_TC_032 PASSED — KAM " + kam + ", BDM " + bdm, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_026 (step 3) — Change payers, View Offer shows them
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 62, description = "SC_001_TC_026 — Changed payers are saved and shown in View Offer")
    public void tc062_updatePayers() throws Exception {
        Reporter.log("▶ SC_001_TC_026 — Payers (SAVES)", true);
        requireCreated(v1Title, "V1 tie-up");
        openUpdateFor(v1Title);
        tieupPage.selectByText("transport_payer", "Corporate");
        tieupPage.selectByText("kit_payer", "Waive-off");
        updateWithReason("QA auto: Transport Corporate, Kit Waive-off");
        String text = viewOfferText(v1Title).toUpperCase();
        Reporter.log("   " + text.replaceAll("(?s).*FEE COMPONENTS", "FEE COMPONENTS").replaceAll("(?s)CONTACT & FILES.*", "").replace('\n', ' '), true);
        Assert.assertTrue(text.matches("(?s).*TRANSPORT FEE\\s+CORPORATE.*"), "❌ Transport payer not Corporate in View Offer");
        Assert.assertTrue(text.matches("(?s).*KIT / STUDY MATERIAL\\s+WAIVE-?OFF.*"), "❌ Kit payer not Waive-off in View Offer");
        Reporter.log("✅ SC_001_TC_026 (step 3) PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_024 — Update: V1 → V2 hides the annual section
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 63, description = "SC_001_TC_024 — Update: Plan V2 hides the annual discount")
    public void tc063_updateToV2() throws Exception {
        Reporter.log("▶ SC_001_TC_024 — Update V1 → V2 (SAVES)", true);
        requireCreated(v1Title, "V1 tie-up");
        openUpdateFor(v1Title);
        tieupPage.selectByText("corporate_payment_plan", "V2");
        for (String id : ANNUAL_IDS)
            Assert.assertFalse(tieupPage.isFieldVisible(id), "❌ V2: " + id + " shown");
        tieupPage.selectByText("sel_full_day_3", "Rs. 7000.00");
        updateWithReason("QA auto: plan V1 to V2");
        openUpdateFor(v1Title);
        Assert.assertEquals(tieupPage.getSelectedText("corporate_payment_plan"), "V2", "❌ Plan not saved as V2");
        Reporter.log("✅ SC_001_TC_024 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_023 — Update: V2 → V1 needs Annual Fee + discount
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 64, description = "SC_001_TC_023 — Update: Plan V1 shows Annual Fee + discount, saved")
    public void tc064_updateToV1() throws Exception {
        Reporter.log("▶ SC_001_TC_023 — Update V2 → V1 (SAVES)", true);
        requireCreated(v2Title, "V2 tie-up");
        openUpdateFor(v2Title);
        tieupPage.selectByText("corporate_payment_plan", "V1");
        for (String id : ANNUAL_IDS)
            Assert.assertTrue(tieupPage.isFieldVisible(id), "❌ V1: " + id + " not shown");
        tieupPage.selectByText("sel_full_day_3", "Rs. 7000.00");
        tieupPage.selectByIndexIfPresent("sel_annual", 1);
        String annualOffer = tieupPage.getOptions("offer_annual_chargebee_id").get(1);
        tieupPage.selectByText("offer_annual_chargebee_id", annualOffer);
        tieupPage.selectByText("annual_fee_payer", "Corporate");
        updateWithReason("QA auto: plan V2 to V1 with " + annualOffer);
        openUpdateFor(v2Title);
        Assert.assertEquals(tieupPage.getSelectedText("corporate_payment_plan"), "V1", "❌ Plan not saved as V1");
        Assert.assertEquals(tieupPage.getSelectedText("offer_annual_chargebee_id"), annualOffer, "❌ Annual offer not saved");
        Reporter.log("✅ SC_001_TC_023 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_029 — Send an admission request for a lead
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 70, description = "SC_001_TC_029 — Admission Request: send a request for a lead, listed on Admission Invites")
    public void tc070_sendAdmissionRequest() throws Exception {
        Reporter.log("▶ SC_001_TC_029 — Admission Request, lead " + LEAD_ID + " (SAVES)", true);
        requireCreated(v2Title, "V2 tie-up");
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        tieupPage.searchTable(v2Title);
        Assert.assertTrue(tieupPage.getFirstRowTitle().startsWith(v2Title), "❌ Tie-up not found: " + v2Title);
        tieupPage.clickFirstRowAdmissionRequest();
        Assert.assertTrue(tieupPage.isPopdownVisible(), "❌ Admission Request popup did not open");
        tieupPage.fetchLead(LEAD_ID);
        Assert.assertTrue(tieupPage.isLeadDetailsShown(), "❌ No lead details for lead " + LEAD_ID);
        String leadEmail = tieupPage.getLeadEmail();
        tieupPage.selectInPopup("invite_parent_type", "Father");
        tieupPage.selectInPopup("centers_available", "Demo Center");
        String plan = tieupPage.selectFirstInPopup("payment_plan_avail");
        String program = tieupPage.selectFirstInPopup("program_offered");
        tieupPage.setTentativeAdmissionDate(15);
        tieupPage.enterInPopup("admission_request_comment", "QA auto admission request " + RUN_ID);
        tieupPage.enterInPopup("employee_code", "FP-101");
        tieupPage.enterInPopup("employee_email", CONTACT_EMAIL);
        tieupPage.enterInPopup("employer", "QA Auto Employer");
        Reporter.log("   Lead " + LEAD_ID + " (" + tieupPage.getLeadName() + ", " + leadEmail + ") | Father | Demo Center | "
                + plan + " | " + program, true);
        String msg = tieupPage.clickSendRequest();
        Reporter.log("   After Send Request: " + msg, true);
        Assert.assertTrue(tieupPage.getPopupErrors().isEmpty(), "❌ Validation errors: " + tieupPage.getPopupErrors());
        Assert.assertTrue(msg.toLowerCase().contains("success"), "❌ No success message: " + msg);

        // Step 4 — the request is listed on Admission Invites
        navigations.goToAdmissionInvites();
        AdmissionInvitesPage invites = new AdmissionInvitesPage(driver);
        Assert.assertTrue(invites.isPageLoaded(), "❌ Admission Invites did not load");
        invites.enterFilterInviteEmail(leadEmail);
        invites.selectFilterInviteStatus("All");
        invites.clickSearch();
        List<String> rows = invites.getRowTexts();
        rows.forEach(r -> Reporter.log("   Invite: " + r, true));
        Assert.assertTrue(rows.stream().anyMatch(r -> r.contains(v2Title) && r.contains("Father")),
                "❌ No Admission Invites row for " + v2Title + " / " + leadEmail);
        Reporter.log("✅ SC_001_TC_029 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_001_TC_041 — Action icons by user rights (switches user → runs last)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 99, description = "SC_001_TC_041 — Action icons by user rights (Rakesh vs Jaydeep Kar)")
    public void tc041_actionIconsByRights() throws Exception {
        Reporter.log("▶ SC_001_TC_041 — Action icons by rights", true);
        Assert.assertEquals(tieupPage.getFirstRowActionTitles(), ALL_ACTIONS, "❌ Rakesh's icons");
        navigations.goToUserRights();
        new UserRightsPage(driver).switchUser("Jaydeep Kar");
        Thread.sleep(2000);
        navigations.goToTieupDetails();
        tieupPage = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieupPage.isListLoaded(), "❌ List did not load as Jaydeep Kar");
        List<String> icons = tieupPage.getFirstRowActionTitles();
        Reporter.log("   Jaydeep Kar: " + icons, true);
        Assert.assertEquals(icons, Arrays.asList("View Offer", "Admission Request"), "❌ Jaydeep's icons");
        Reporter.log("✅ SC_001_TC_041 PASSED", true);
    }
}
