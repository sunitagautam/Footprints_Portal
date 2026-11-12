package testScripts.FranchiseTests.CorporateTieups;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.TieupPlansPricingPage;
import pages.Franchise.CorporateTieups.Tieup_DetailsPage;
import pages.Navigations;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// TIEUP PLANS AND PRICING (Franchise → Corporate Tieups → Tieup Plans and Pricing, /tie_up_child_financial_info)
// Source: TC_Corporate Tie-ups.xlsx SC_007_TC_001–021, updated to the live screen + new TCs →
// TC_Corporate_Tieups.xlsx, sheet TC_TieupPlansPricing. Runs as Rakesh.
// Saves (user, 2026-11-03): TC_007–011 edit ONLY our own "QA Auto Tieup V2 27101611" (offer 275, stays Active) and
// put it back; TC_018 applies the new fee structure on that offer only (its children 75594, 75607, 75621).
// ═══════════════════════════════════════════════════════════════
public class TieupPlansPricingTest extends BaseTest {

    private static final String BIG_OFFER = System.getProperty("plansOffer", "ProEves Corporate offer - Long Term");
    private static final String QA_OFFER = "QA Auto Tieup V2 27101611";
    private static final String QA_OFFER_UPDATE_URL = "https://test-franchise.footprintseducation.in/add_corporate_offer?offer_id=Mjc1";
    private static final List<String> COLUMNS = Arrays.asList("Child Id", "Name", "Center", "Company", "Offer Title", "Monthly Plan",
            "Yearly Plan", "Tie-Up Monthly Plan", "Tie-Up Yearly Plan", "Coupons", "Tie-Up Coupons", "Program", "Joining Date",
            "Next Monthly Billing", "Next Yearly Billing", "Payment Plan", "Billing Frequency", "Current Status", "Monthly Payer",
            "Annual Payer", "Doubtful Reason", "Something_Wrong");
    // The QA tie-up's settings, restored after every edit
    private static final Map<String, String> QA_ORIGINAL = new LinkedHashMap<>();

    static {
        QA_ORIGINAL.put("fee_type", "Fixed Fee");
        QA_ORIGINAL.put("tieup_type", "Corporate Pay");
        QA_ORIGINAL.put("monthly_fee_payer", "Corporate");
        QA_ORIGINAL.put("annual_fee_payer", "Corporate");
        QA_ORIGINAL.put("offer_annual_chargebee_id", "5% Discount");
    }

    Navigations navigations;
    TieupPlansPricingPage plansPage;

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
        navigations.goToTieupPlansPricing();
        plansPage = new TieupPlansPricingPage(driver);
        Assert.assertTrue(plansPage.isPageLoaded(), "❌ Tieup Plans and Pricing did not load");
        plansPage.resetTableState();
    }

    private void openOffer(String offer, boolean doubtfulOnly) throws InterruptedException {
        plansPage.selectOffer(offer);
        plansPage.resetTableState();
        plansPage.setDoubtful(doubtfulOnly);
        Assert.assertEquals(plansPage.getSelectedOffer(), offer, "❌ Offer not kept after reload");
    }

    // ✅ Update our QA tie-up with the given field → option text values, then return the flash message
    private String editQaTieup(Map<String, String> values, String reason) throws InterruptedException {
        driver.get(QA_OFFER_UPDATE_URL);
        Thread.sleep(3000);
        Tieup_DetailsPage tieup = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieup.isFormLoaded(), "❌ Update form did not load");
        Assert.assertEquals(tieup.getFieldValue("offer_title_id"), QA_OFFER, "❌ Update form is for another tie-up");
        for (Map.Entry<String, String> e : values.entrySet()) tieup.selectByText(e.getKey(), e.getValue());
        tieup.enterUpdateReason(reason);
        tieup.clickUpdate();
        dismissAlertIfPresent();
        String msg = String.join(" || ", tieup.getFlashMessages());
        Reporter.log("   Tie-up updated " + values + " → " + msg, true);
        Assert.assertTrue(tieup.getVisibleErrors().isEmpty(), "❌ Validation errors: " + tieup.getVisibleErrors());
        Assert.assertTrue(msg.toLowerCase().contains("updated successfully"), "❌ Tie-up not updated: " + msg);
        return msg;
    }

    // ✅ After a tie-up change: reload the screen for the QA offer and check its records are listed
    private List<String> qaRecordsAfterChange(String column) throws InterruptedException {
        reopen();
        openOffer(QA_OFFER, false);
        int rows = plansPage.getRowCount();
        List<String> values = plansPage.getColumnValues(column);
        Reporter.log("   " + QA_OFFER + ": " + plansPage.getTopText() + " | rows " + rows + " | " + column + " " + values
                + " | doubtful " + plansPage.getColumnValues("Something_Wrong"), true);
        Assert.assertTrue(rows > 0, "❌ No records listed after the tie-up change");
        Assert.assertEquals(rows, plansPage.getAdmissionsCount(), "❌ Rows vs 'N admissions'");
        return values;
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 / TC_003 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_007_TC_001 — Default view")
    public void tc001_defaultView() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_001 — Default view", true);
        Reporter.log("   Heading: " + plansPage.getHeading() + " | top: " + plansPage.getTopText() + " | offer " + plansPage.getSelectedOffer()
                + " | doubtful checked " + plansPage.isDoubtfulChecked() + " | " + plansPage.getEmptyTableMessage(), true);
        Assert.assertEquals(plansPage.getHeading(), "Tie Up Admission Financial Information Report", "❌ Heading");
        Assert.assertTrue(plansPage.isOfferDropdownVisible(), "❌ Select Offer dropdown");
        Assert.assertEquals(plansPage.getSelectedOffer(), "--Select Offer--", "❌ Default offer");
        Assert.assertTrue(plansPage.isDoubtfulCheckboxPresent() && plansPage.isDoubtfulChecked(), "❌ Only Doubtful Records not checked by default");
        Assert.assertEquals(plansPage.getAdmissionsCount(), 0, "❌ '0 admissions' before an offer is chosen");
        Assert.assertFalse(plansPage.isApplyButtonVisible(), "❌ Apply New Fee Structure shown before an offer is chosen");
        openOffer(BIG_OFFER, true);
        List<String> all = plansPage.getAllColumnHeaders();
        Reporter.log("   After offer: " + plansPage.getTopText() + " | columns " + all + " | per page " + plansPage.getPageLength()
                + " " + plansPage.getPageLengthOptions() + " | search '" + plansPage.getSearchPlaceholder() + "'", true);
        Assert.assertEquals(all, COLUMNS, "❌ Columns");
        Assert.assertTrue(plansPage.isApplyButtonVisible(), "❌ Apply New Fee Structure");
        Assert.assertTrue(plansPage.isSearchVisible() && plansPage.isDownloadVisible() && plansPage.isPaginationVisible(),
                "❌ Search / Download Report / pagination");
        Assert.assertEquals(plansPage.getPageLength(), 10, "❌ Rows per page");
        Assert.assertEquals(plansPage.getPageLengthOptions(), Arrays.asList("10", "20", "50", "100"), "❌ Page length options");
        String body = String.valueOf(((org.openqa.selenium.JavascriptExecutor) driver).executeScript("return document.body.innerText;"));
        Reporter.log("   Notes (TC_003, remark only): " + (body.contains("indicate plan is not set") ? "shown" : "NOT shown on the screen"), true);
        Reporter.log("✅ SC_007_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Access
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_007_TC_002 — Screen access")
    public void tc002_access() {
        Reporter.log("▶ SC_007_TC_002 — Access", true);
        String inMenu = String.valueOf(((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var m=(window.menuData||[]).filter(function(x){return x.url==='tie_up_child_financial_info';});return m.length?m[0].topMenu+' > '+m[0].subMenu+' > '+m[0].title:'';"));
        Reporter.log("   Menu: " + inMenu + " | URL " + driver.getCurrentUrl(), true);
        Assert.assertEquals(inMenu, "Franchise > Corporate Tieups > Tieup Plans and Pricing", "❌ Menu entry");
        Assert.assertTrue(driver.getCurrentUrl().endsWith("/tie_up_child_financial_info"), "❌ URL");
        Reporter.log("✅ SC_007_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Select offer
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_007_TC_004 — Select offer")
    public void tc004_selectOffer() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_004 — Select offer " + BIG_OFFER, true);
        openOffer(BIG_OFFER, false);
        List<String> titles = plansPage.getColumnValues("Offer Title");
        Reporter.log("   " + plansPage.getTopText() + " | rows " + titles.size() + " | " + plansPage.getInfoText(), true);
        Assert.assertTrue(titles.size() > 0, "❌ No records for the offer");
        Assert.assertEquals(titles.size(), plansPage.getAdmissionsCount(), "❌ Rows vs 'N admissions'");
        for (String t : titles) Assert.assertEquals(t, BIG_OFFER, "❌ Row of another offer");
        Reporter.log("✅ SC_007_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 — Search in the offer dropdown
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_007_TC_005 — Search in the offer dropdown")
    public void tc005_offerDropdownSearch() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_005 — Offer dropdown search", true);
        List<String> found = plansPage.searchOfferDropdown("ProEves");
        Reporter.log("   'ProEves' → " + found, true);
        Assert.assertFalse(found.isEmpty(), "❌ No result");
        for (String o : found) Assert.assertTrue(o.toLowerCase().contains("proeves"), "❌ Unrelated option: " + o);
        List<String> none = plansPage.searchOfferDropdown("zzqq-no-offer");
        Reporter.log("   'zzqq-no-offer' → " + none, true);
        Assert.assertEquals(none, List.of("No results found"), "❌ No-match text");
        Reporter.log("✅ SC_007_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_006 / TC_012 — Only Doubtful Records
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_007_TC_006 / TC_012 — Only Doubtful Records")
    public void tc006_doubtfulCheckbox() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_006 / TC_012 — Only Doubtful Records (" + QA_OFFER + ")", true);
        openOffer(QA_OFFER, true);
        int doubtful = plansPage.getRowCount();
        List<String> flags = plansPage.getColumnValues("Something_Wrong");
        List<String> reasons = plansPage.getColumnValues("Doubtful Reason");
        Reporter.log("   Checked: " + doubtful + " rows | " + flags + " | reasons " + reasons + " | red names " + plansPage.getRedNameChildIds(), true);
        for (String f : flags) Assert.assertEquals(f, "Doubtful", "❌ Non-doubtful row while checked");
        for (String r : reasons) Assert.assertFalse(r.isEmpty(), "❌ Doubtful row with no reason");
        plansPage.setDoubtful(false);
        int all = plansPage.getRowCount();
        Reporter.log("   Unchecked: " + all + " rows (total " + plansPage.getTotalRowCount() + ")", true);
        Assert.assertFalse(plansPage.isDoubtfulChecked(), "❌ Did not uncheck");
        Assert.assertEquals(all, plansPage.getTotalRowCount(), "❌ Unchecked should list every record");
        plansPage.setDoubtful(true);
        Assert.assertTrue(plansPage.isDoubtfulChecked(), "❌ Did not check again");
        Assert.assertEquals(plansPage.getRowCount(), doubtful, "❌ Doubtful rows after re-check");
        Reporter.log("✅ SC_007_TC_006 / TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_013 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_007_TC_013 — Search")
    public void tc013_search() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_013 — Search", true);
        openOffer(BIG_OFFER, false);
        String center = plansPage.getPageColumnValues("Center").get(0);
        plansPage.searchTable(center);
        List<String> centers = plansPage.getColumnValues("Center");
        Reporter.log("   Search '" + center + "' → " + centers.size() + " rows", true);
        Assert.assertFalse(centers.isEmpty(), "❌ No result");
        plansPage.searchTable("zzqq-no-such-child");
        Assert.assertEquals(plansPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        plansPage.searchTable("");
        Reporter.log("✅ SC_007_TC_013 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_014 — Download Report
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_007_TC_014 — Download Report")
    public void tc014_download() throws Exception {
        Reporter.log("▶ SC_007_TC_014 — Download Report (" + QA_OFFER + ", all records)", true);
        openOffer(QA_OFFER, false);
        int expected = plansPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        plansPage.clickDownload();
        File csv = null;
        for (int i = 0; i < 40 && csv == null; i++) {
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
        String header = lines.isEmpty() ? "" : lines.get(0);
        Reporter.log("   File: " + csv.getName() + " | header " + header + " | data lines " + (lines.size() - 1) + " (grid " + expected + ")", true);
        Assert.assertTrue(csv.getName().startsWith("Tie_Up_Children_Financial_Information_Report_"), "❌ File name");
        for (String c : Arrays.asList("Child ID", "Name", "Center", "Company", "Offer Title", "Program"))
            Assert.assertTrue(header.toLowerCase().contains(c.toLowerCase()), "❌ CSV missing " + c);
        for (String id : plansPage.getColumnValues("Child Id"))
            Assert.assertTrue(lines.stream().anyMatch(l -> l.contains(id)), "❌ Child " + id + " not in the CSV");
        Reporter.log("✅ SC_007_TC_014 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_015 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_007_TC_015 — Pagination")
    public void tc015_pagination() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_015 — Pagination", true);
        openOffer(BIG_OFFER, false);
        int rows = plansPage.getRowCount();
        String first = plansPage.getInfoText();
        Assert.assertTrue(rows > 10, "❌ Need more than 10 records, got " + rows);
        Assert.assertFalse(plansPage.isPreviousPageEnabled(), "❌ Previous enabled on page 1");
        plansPage.clickNextPage();
        String second = plansPage.getInfoText();
        plansPage.clickPreviousPage();
        String back = plansPage.getInfoText();
        Reporter.log("   " + first + " → Next: " + second + " → Previous: " + back, true);
        Assert.assertTrue(first.startsWith("Showing 1–10 of " + rows), "❌ Page 1 info");
        Assert.assertTrue(second.startsWith("Showing 11–20 of " + rows), "❌ Page 2 info");
        Assert.assertEquals(back, first, "❌ Previous did not return to page 1");
        Reporter.log("✅ SC_007_TC_015 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_016 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC_007_TC_016 — Sorting")
    public void tc016_sorting() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_016 — Sorting", true);
        openOffer(BIG_OFFER, false);
        List<Long> asc = new ArrayList<>();
        for (String v : plansPage.getPageColumnValues("Child Id")) asc.add(Long.parseLong(v));
        plansPage.clickColumnHeader("Child Id");
        List<Long> desc = new ArrayList<>();
        for (String v : plansPage.getPageColumnValues("Child Id")) desc.add(Long.parseLong(v));
        String descClass = plansPage.getHeaderSortClass("Child Id");
        plansPage.clickColumnHeader("Name");
        List<String> names = plansPage.getPageColumnValues("Name");
        Reporter.log("   Child Id asc " + asc + " | " + descClass + " " + desc + " | Name " + names, true);
        List<Long> sorted = new ArrayList<>(asc);
        sorted.sort(Comparator.naturalOrder());
        Assert.assertEquals(asc, sorted, "❌ Child Id ascending (default)");
        sorted = new ArrayList<>(desc);
        sorted.sort(Comparator.reverseOrder());
        Assert.assertEquals(desc, sorted, "❌ Child Id descending");
        List<String> sortedNames = new ArrayList<>(names);
        sortedNames.sort(String.CASE_INSENSITIVE_ORDER);
        Assert.assertEquals(names, sortedNames, "❌ Name ascending");
        plansPage.resetTableState();
        Reporter.log("✅ SC_007_TC_016 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_017 — Expand / collapse
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_007_TC_017 — Expand / collapse a record")
    public void tc017_expandCollapse() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_017 — Expand / collapse", true);
        openOffer(BIG_OFFER, false);
        if (!plansPage.hasCollapsedColumns()) throw new SkipException("The window is wide enough to show every column — no '+' control");
        Assert.assertFalse(plansPage.isFirstRowExpanded(), "❌ Expanded by default");
        plansPage.clickFirstRowControl();
        String detail = plansPage.getFirstRowDetailText();
        Reporter.log("   Expanded: " + detail, true);
        Assert.assertTrue(plansPage.isFirstRowExpanded(), "❌ Did not expand");
        Assert.assertTrue(detail.contains("Something_Wrong") || detail.contains("Doubtful Reason"), "❌ Hidden columns not shown in the detail row");
        plansPage.clickFirstRowControl();
        Assert.assertFalse(plansPage.isFirstRowExpanded(), "❌ Did not collapse");
        Reporter.log("✅ SC_007_TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_007 — Fee type changed (Fixed Fee → Center Fee → back)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC_007_TC_007 — Records after the fee type changes (Center Fee not listed)")
    public void tc007_feeTypeChanged() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_007 — Fee type Fixed Fee → Center Fee → Fixed Fee (SAVES, QA tie-up)", true);
        try {
            editQaTieup(Map.of("fee_type", "Center Fee"), "QA auto: Plans and Pricing TC_007 fee type Center Fee");
            reopen();
            openOffer(QA_OFFER, false);
            Reporter.log("   Center Fee: " + plansPage.getTopText() + " | rows " + plansPage.getRowCount(), true);
            // ✅ By design (Sunita Gautam, 03-Nov-2026): Center Fee tie-ups are not listed on this report
            Assert.assertEquals(plansPage.getAdmissionsCount(), 0, "❌ Center Fee tie-up should not be listed");
        } finally {
            editQaTieup(Map.of("fee_type", "Fixed Fee"), "QA auto: Plans and Pricing TC_007 fee type back to Fixed Fee");
        }
        qaRecordsAfterChange("Tie-Up Monthly Plan");
        Reporter.log("✅ SC_007_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 / TC_009 — Parent pay ↔ Corporate pay
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 21, description = "SC_007_TC_008 / TC_009 — Records after the annual payer changes")
    public void tc008_payerChanged() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_008 / TC_009 — Annual Fee Payer Parent, then Corporate (SAVES, QA tie-up)", true);
        try {
            editQaTieup(Map.of("annual_fee_payer", "Parent"), "QA auto: Plans and Pricing TC_008 annual payer Parent");
            List<String> parent = qaRecordsAfterChange("Tie-Up Yearly Plan");
            for (String v : parent) Assert.assertTrue(v.contains("(Parent)"), "❌ Tie-Up Yearly Plan not Parent: " + v);
        } finally {
            editQaTieup(Map.of("annual_fee_payer", "Corporate"), "QA auto: Plans and Pricing TC_009 annual payer Corporate");
        }
        List<String> corporate = qaRecordsAfterChange("Tie-Up Yearly Plan");
        for (String v : corporate) Assert.assertTrue(v.contains("(Corporate)"), "❌ Tie-Up Yearly Plan not Corporate: " + v);
        Reporter.log("✅ SC_007_TC_008 / TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_010 — Onsite needs onsite centres only
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 22, description = "SC_007_TC_010 — Onsite tie-up type")
    public void tc010_onsite() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_010 — Tieup Type Onsite on a non-onsite centre (validation, nothing saved)", true);
        driver.get(QA_OFFER_UPDATE_URL);
        Thread.sleep(3000);
        Tieup_DetailsPage tieup = new Tieup_DetailsPage(driver);
        Assert.assertTrue(tieup.isFormLoaded(), "❌ Update form did not load");
        // ✅ never touch another tie-up: offer IDs change when the test DB is synced (offer 275 became a real tie-up, 2026-11-12)
        Assert.assertEquals(tieup.getFieldValue("offer_title_id"), QA_OFFER, "❌ Update form is for another tie-up");
        tieup.selectByText("tieup_type", "Onsite");
        tieup.enterUpdateReason("QA auto: Plans and Pricing TC_010 tie-up type Onsite");
        tieup.clickUpdate();
        dismissAlertIfPresent();
        String msg = String.join(" || ", tieup.getFlashMessages());
        Reporter.log("   " + msg, true);
        Assert.assertTrue(msg.contains("An Onsite tie-up can only be linked to onsite center(s). Please uncheck the following non-onsite center(s) before saving: Demo Center."),
                "❌ Onsite validation: " + msg);
        Assert.assertEquals(qaRecordsAfterChange("Offer Title").size(), 3, "❌ Records changed");
        Reporter.log("✅ SC_007_TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_011 — Coupon changed
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 23, description = "SC_007_TC_011 — Records after the tie-up coupon changes")
    public void tc011_couponChanged() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_011 — Annual Fee Offer 5% → 10% → 5% (SAVES, QA tie-up)", true);
        List<String> before = qaRecordsAfterChange("Tie-Up Coupons");
        try {
            editQaTieup(Map.of("offer_annual_chargebee_id", "10% Discount"), "QA auto: Plans and Pricing TC_011 annual coupon 10%");
            List<String> changed = qaRecordsAfterChange("Tie-Up Coupons");
            Assert.assertNotEquals(changed, before, "❌ Tie-Up Coupons did not change");
        } finally {
            editQaTieup(Map.of("offer_annual_chargebee_id", "5% Discount"), "QA auto: Plans and Pricing TC_011 annual coupon back to 5%");
        }
        Assert.assertEquals(qaRecordsAfterChange("Tie-Up Coupons"), before, "❌ Tie-Up Coupons not back");
        Reporter.log("✅ SC_007_TC_011 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_018 / TC_019 — Apply New Fee Structure (QA offer only)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC_007_TC_018 / TC_019 — Apply New Fee Structure")
    public void tc018_applyNewFeeStructure() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_018 / TC_019 — Apply New Fee Structure on " + QA_OFFER + " (SAVES)", true);
        editQaTieup(QA_ORIGINAL, "QA auto: Plans and Pricing - settings restored before Apply");
        reopen();
        openOffer(QA_OFFER, true);
        int before = plansPage.getRowCount();
        Reporter.log("   Doubtful before: " + before + " " + plansPage.getColumnValues("Doubtful Reason"), true);

        plansPage.clickApplyNewFeeStructure();
        String confirm = plansPage.readConfirm(false);
        Thread.sleep(1000);
        Reporter.log("   Confirm: " + confirm.replace("\n", " ") + " → Cancel", true);
        Assert.assertTrue(confirm.startsWith("The 'Co-Pay' tie-up fee structure will not be updated from here."), "❌ Confirm text");
        Assert.assertTrue(confirm.contains("Do you really want to make changes to your billing plans? It will go into effect from the next billing date."), "❌ Confirm text");
        Assert.assertEquals(plansPage.getRowCount(), before, "❌ Something changed after Cancel");

        plansPage.clickApplyNewFeeStructure();
        plansPage.readConfirm(true);
        String message = plansPage.waitForUpdateMessage();
        Thread.sleep(3000);
        reopen();
        openOffer(QA_OFFER, true);
        int after = plansPage.getRowCount();
        Reporter.log("   Message: " + message + " | doubtful after: " + after + " " + plansPage.getColumnValues("Doubtful Reason"), true);
        plansPage.setDoubtful(false);
        Reporter.log("   All records after Apply: Monthly " + plansPage.getColumnValues("Monthly Plan") + " | Tie-Up Monthly "
                + plansPage.getColumnValues("Tie-Up Monthly Plan") + " | Coupons " + plansPage.getColumnValues("Coupons"), true);
        Assert.assertTrue(message.toLowerCase().contains("contract plan updated"), "❌ Apply message: " + message);
        // ✅ Remaining doubtful rows are only tie-up configuration gaps (no plan on the tie-up for the child's program) —
        //    expected for the QA tie-up (Sunita Gautam, 03-Nov-2026)
        Assert.assertTrue(after <= before, "❌ More doubtful records after Apply");
        for (String r : plansPage.getColumnValues("Doubtful Reason"))
            if (!r.isEmpty()) Assert.assertTrue(r.matches("((Annual|Monthly) billing plan not setup on tie-up contractual term - \\d+ ?)+"),
                    "❌ Doubtful reason other than a tie-up configuration gap: " + r);
        Reporter.log("✅ SC_007_TC_018 / TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_020 — History after Apply (Child History of each child)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 31, description = "SC_007_TC_020 — Child History after Apply New Fee Structure")
    public void tc020_historyAfterApply() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_020 — Child History 'Contract Updated'", true);
        openOffer(QA_OFFER, false);
        List<String> children = plansPage.getColumnValues("Child Id");
        String today = java.time.LocalDate.now().toString();
        String entry = "Fee updated to the latest contractual terms under Contract:" + QA_OFFER;
        for (String id : children) {
            driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + id);
            Thread.sleep(6000);
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var a=[].find.call(document.querySelectorAll('a'), function(a){return /CHILD HISTORY/i.test(a.innerText);}); if(a) a.click();");
            Thread.sleep(5000);
            String history = String.valueOf(((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "var t=document.body.innerText; var i=t.indexOf('Child Updates History'); return i<0 ? '' : t.substring(i, i+1500).replace(/\\s+/g,' ');"));
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Contract Updated Rakesh Singh · " + today + " \\d{2}:\\d{2}:\\d{2} "
                    + java.util.regex.Pattern.quote(entry)).matcher(history);
            boolean found = m.find();
            Reporter.log("   " + id + ": " + (found ? m.group() : "not found → " + history.substring(0, Math.min(300, history.length()))), true);
            Assert.assertTrue(found, "❌ No 'Contract Updated' history today for " + id);
        }
        Reporter.log("✅ SC_007_TC_020 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_021 — Email after the tie-up is updated
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 32, description = "SC_007_TC_021 — 'Tie-up Details Updated' email")
    public void tc021_tieupUpdatedEmail() throws InterruptedException {
        Reporter.log("▶ SC_007_TC_021 — Email 'Tie-up Details Updated – " + QA_OFFER + "'", true);
        navigations.goToEmailView();
        pages.Settings.SystemLogs.EmailView_Page email = new pages.Settings.SystemLogs.EmailView_Page(driver);
        Assert.assertTrue(email.isPageLoaded(), "❌ Email View did not load");
        String subject = "Tie-up Details Updated – " + QA_OFFER;
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, yyyy", java.util.Locale.ENGLISH));
        List<String> rows = email.getAllMatchingRowTexts("Tie-up Details Updated");
        List<String> mine = new ArrayList<>();
        for (String r : rows) if (r.contains(subject) && r.contains(today)) mine.add(r.replaceAll("\\s+", " "));
        Reporter.log("   " + mine.size() + " email(s) today: " + (mine.isEmpty() ? rows : mine.subList(0, Math.min(2, mine.size()))), true);
        Assert.assertFalse(mine.isEmpty(), "❌ No '" + subject + "' email today");
        Reporter.log("✅ SC_007_TC_021 PASSED", true);
    }
}
