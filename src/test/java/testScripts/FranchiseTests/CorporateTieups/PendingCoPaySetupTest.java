package testScripts.FranchiseTests.CorporateTieups;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Franchise.CorporateTieups.PendingCoPaySetupPage;
import pages.Navigations;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// PENDING CO-PAY SETUP (Franchise → Corporate Tieups → Pending Co-Pay Setup, /pending_copay_setup)
// Source: TC_Corporate Tie-ups.xlsx SC_004_TC_001–008, updated to the live screen + new TCs →
// TC_Corporate_Tieups.xlsx, sheet TC_PendingCoPaySetup. Runs as Rakesh (Copay_Update_Access).
// Saves (user, 2026-11-03): TC_007 sets child 74169's Co-Pay plans per its invite breakup — corporate 8,998, parent
// 14,301 (= centre fee 23,299 − 8,998), billing from Dec 2026, registration fee 11,499 (centre V2 fee) paid by Parent.
// ═══════════════════════════════════════════════════════════════
public class PendingCoPaySetupTest extends BaseTest {

    private static final List<String> COLUMNS = Arrays.asList("Child Id", "Child Name", "Center", "Offer Title", "Program", "Plan",
            "Joining Date", "Actions");
    private static final String OFFER = System.getProperty("copayOffer", "ProEves Corporate offer - Long Term");
    private static final String SETUP_CHILD = System.getProperty("copaySetupChild", "74169");
    private static final String PARENT_MONTHLY = "14301", CORPORATE_MONTHLY = "8998", REGISTRATION_FEE = "11499";
    private static final String BILLING_MONTH = "2026-12-01";

    Navigations navigations;
    PendingCoPaySetupPage copayPage;

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
        navigations.goToPendingCoPaySetup();
        copayPage = new PendingCoPaySetupPage(driver);
        Assert.assertTrue(copayPage.isPageLoaded(), "❌ Pending Co-Pay Setup did not load");
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_001 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_004_TC_001 — Default view")
    public void tc001_defaultView() {
        Reporter.log("▶ SC_004_TC_001 — Default view", true);
        List<String> cols = copayPage.getAllColumnHeaders();
        Reporter.log("   Heading: " + copayPage.getHeading() + " | offer " + copayPage.getSelectedText("sr_offer_id") + " | center "
                + copayPage.getSelectedText("sr_center_id") + " | columns " + cols + " | " + copayPage.getInfoText() + " | per page " + copayPage.getPageLength()
                + " | actions " + copayPage.getActionTitles(), true);
        Assert.assertEquals(copayPage.getHeading(), "Pending Co-Pay Setup Admissions", "❌ Heading");
        Assert.assertTrue(copayPage.isOfferFilterVisible() && copayPage.isCenterFilterVisible() && copayPage.isFilterButtonVisible(), "❌ Filters");
        Assert.assertEquals(copayPage.getSelectedText("sr_offer_id"), "--Select--", "❌ Default offer");
        Assert.assertEquals(copayPage.getSelectedText("sr_center_id"), "All", "❌ Default center");
        Assert.assertTrue(copayPage.isSearchVisible() && copayPage.isDownloadVisible() && copayPage.isPaginationVisible(), "❌ Search / Download / pagination");
        Assert.assertEquals(cols, COLUMNS, "❌ Columns");
        Assert.assertEquals(copayPage.getPageLength(), 10, "❌ Rows per page");
        Assert.assertEquals(copayPage.getActionTitles(), List.of("Update Subscription Plan"), "❌ Actions");
        Reporter.log("✅ SC_004_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_002 — Filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_004_TC_002 — Offer and Center filters")
    public void tc002_filters() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_002 — Filters", true);
        int all = copayPage.getRowCount();
        copayPage.selectByText("sr_offer_id", OFFER);
        copayPage.clickFilter();
        List<String> offers = copayPage.getColumnValues("Offer Title");
        Reporter.log("   Offer '" + OFFER + "': " + offers.size() + " of " + all + " rows", true);
        Assert.assertEquals(copayPage.getSelectedText("sr_offer_id"), OFFER, "❌ Offer not kept");
        Assert.assertFalse(offers.isEmpty(), "❌ No rows for the offer");
        for (String o : offers) Assert.assertEquals(o, OFFER, "❌ Row of another offer");

        String center = copayPage.getColumnValues("Center").get(0);
        reopen();
        copayPage.selectByText("sr_center_id", center);
        copayPage.clickFilter();
        List<String> centers = copayPage.getColumnValues("Center");
        Reporter.log("   Center '" + center + "': " + centers.size() + " rows", true);
        Assert.assertEquals(copayPage.getSelectedText("sr_center_id"), center, "❌ Center not kept");
        for (String c : centers) Assert.assertEquals(c, center, "❌ Row of another centre");

        reopen();
        copayPage.selectByText("sr_center_id", "Demo Center");
        copayPage.clickFilter();
        Reporter.log("   Demo Center: " + copayPage.getRowCount() + " rows | " + copayPage.getEmptyTableMessage(), true);
        Assert.assertEquals(copayPage.getEmptyTableMessage(), "No data available in table", "❌ Empty message");
        Reporter.log("✅ SC_004_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_003 — Search
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3, description = "SC_004_TC_003 — Search")
    public void tc003_search() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_003 — Search", true);
        String center = copayPage.getPageColumnValues("Center").get(0);
        copayPage.searchTable(center);
        List<String> centers = copayPage.getColumnValues("Center");
        Reporter.log("   '" + center + "' → " + centers.size() + " rows", true);
        Assert.assertFalse(centers.isEmpty(), "❌ No result");
        for (String c : centers) Assert.assertTrue(c.contains(center), "❌ Unrelated row: " + c);
        copayPage.searchTable("zzqq-no-such-child");
        Assert.assertEquals(copayPage.getEmptyTableMessage(), "No matching records found", "❌ No-match message");
        Reporter.log("✅ SC_004_TC_003 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_004 — Download
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 4, description = "SC_004_TC_004 — Download")
    public void tc004_download() throws Exception {
        Reporter.log("▶ SC_004_TC_004 — Download", true);
        int expected = copayPage.getRowCount();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        copayPage.clickDownload();
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
        String header = lines.get(0);
        Reporter.log("   File: " + csv.getName() + " | header " + header + " | rows " + (lines.size() - 1) + " (grid " + expected + ")", true);
        for (String c : COLUMNS.subList(0, 7)) Assert.assertTrue(header.contains(c), "❌ CSV missing " + c);
        Assert.assertEquals(lines.size() - 1, expected, "❌ CSV rows vs grid");
        Reporter.log("✅ SC_004_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_005 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_004_TC_005 — Pagination")
    public void tc005_pagination() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_005 — Pagination", true);
        int rows = copayPage.getRowCount();
        if (rows <= 10) throw new SkipException("Only " + rows + " rows — no second page");
        String first = copayPage.getInfoText();
        Assert.assertFalse(copayPage.isPreviousPageEnabled(), "❌ Previous enabled on page 1");
        copayPage.clickNextPage();
        String second = copayPage.getInfoText();
        copayPage.clickPreviousPage();
        Reporter.log("   " + first + " → " + second + " → " + copayPage.getInfoText(), true);
        Assert.assertTrue(first.contains("Showing 1 to 10 of " + rows), "❌ Page 1 info");
        Assert.assertTrue(second.contains("Showing 11 to"), "❌ Page 2 info");
        Assert.assertEquals(copayPage.getInfoText(), first, "❌ Previous");
        Reporter.log("✅ SC_004_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_006 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_004_TC_006 — Sorting")
    public void tc006_sorting() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_006 — Sorting", true);
        List<Long> asc = new ArrayList<>();
        for (String v : copayPage.getPageColumnValues("Child Id")) asc.add(Long.parseLong(v));
        copayPage.clickColumnHeader("Child Id");
        List<Long> desc = new ArrayList<>();
        for (String v : copayPage.getPageColumnValues("Child Id")) desc.add(Long.parseLong(v));
        copayPage.clickColumnHeader("Joining Date");
        List<String> dates = copayPage.getPageColumnValues("Joining Date");
        Reporter.log("   Child Id asc " + asc + " | desc " + desc + " | Joining Date " + dates, true);
        List<Long> s = new ArrayList<>(asc);
        s.sort(Comparator.naturalOrder());
        Assert.assertEquals(asc, s, "❌ Child Id ascending (default)");
        s = new ArrayList<>(desc);
        s.sort(Comparator.reverseOrder());
        Assert.assertEquals(desc, s, "❌ Child Id descending");
        List<String> d = new ArrayList<>(dates);
        d.sort(Comparator.naturalOrder());
        Assert.assertEquals(dates, d, "❌ Joining Date ascending (yyyy-mm-dd)");
        Reporter.log("✅ SC_004_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_008 — Update popup validation (nothing saved)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_004_TC_008 — Update popup validation")
    public void tc008_popupValidation() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_008 — Validation on " + SETUP_CHILD, true);
        copayPage.openUpdatePlan(SETUP_CHILD);
        String text = copayPage.getPopupText();
        Reporter.log("   " + text.substring(0, Math.min(700, text.length())), true);
        Assert.assertTrue(text.startsWith("Co-Pay Plan Subscription Setup Update (#" + SETUP_CHILD + ")"), "❌ Popup title");
        for (String f : Arrays.asList("Next Billing will be impacted after next month of enrollment or next month whichever is later.",
                "Registration Fee will be applicable only for fresh admissions.", "Child Name :", "Center Name :", "Program :", "Offer Title :",
                "Payment Plan :", "Center Monthly Fee :", "Child Status :", "Breakup Details :", "For Parent Payer", "For Corporate Payer"))
            Assert.assertTrue(text.toLowerCase().contains(f.toLowerCase()), "❌ Popup missing '" + f + "'");
        // ✅ The child's current parent plan (Rs 1) comes preselected; with no billing month / registration fee nothing can be saved
        Reporter.log("   Preselected: parent " + copayPage.getPopupValue("parent_monthly_plan") + " | corporate '" + copayPage.getPopupValue("corp_monthly_plan")
                + "' | month '" + copayPage.getPopupValue("monthly_billing_date") + "' | registration fee '" + copayPage.getPopupValue("registration_fee") + "'", true);
        // ✅ The registration fee (centre fee) is pre-filled; billing month and payer are empty, so Update cannot save
        Assert.assertTrue(copayPage.getPopupValue("monthly_billing_date").isEmpty() && copayPage.getPopupValue("registration_fee_payer").isEmpty(),
                "❌ Billing month / payer pre-filled — Update could save");
        copayPage.clickUpdate();
        Thread.sleep(1000);
        List<String> errors = copayPage.getVisibleErrors();
        Reporter.log("   Empty Update → " + errors, true);
        for (String f : Arrays.asList("monthly_billing_date", "registration_fee_payer"))
            Assert.assertTrue(errors.stream().anyMatch(e -> e.startsWith(f + ":")), "❌ No message for " + f);
        Reporter.log("✅ SC_004_TC_008 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_009 (new) — Every listed child is a Co-Pay child
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_004_TC_009 — Every listed child can be set up")
    public void tc009_onlyCoPayChildren() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_009 — Update popup of every listed child", true);
        List<String> popups = copayPage.fetchAllPopupTexts();
        List<String> notCoPay = new ArrayList<>(), notProcessed = new ArrayList<>();
        for (String p : popups) {
            if (p.contains("Child should be of Corporate Co-Pay tieup")) notCoPay.add(p.split(" :: ")[0]);
            if (p.contains("Child admission not processed yet")) notProcessed.add(p.split(" :: ")[0]);
        }
        Reporter.log("   " + popups.size() + " children | 'Child should be of Corporate Co-Pay tieup': " + notCoPay
                + " | 'Child admission not processed yet' (expected, pending admission): " + notProcessed, true);
        Assert.assertEquals(popups.size(), copayPage.getRowCount(), "❌ Not every popup was read");
        Assert.assertTrue(notCoPay.isEmpty(), "❌ Listed child(ren) whose Update says Child should be of Corporate Co-Pay tieup: " + notCoPay);
        Reporter.log("✅ SC_004_TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC_007 — Set the Co-Pay plans (SAVES, child 74169)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 20, description = "SC_004_TC_007 — Set the Co-Pay child plans")
    public void tc007_setCoPayPlans() throws InterruptedException {
        Reporter.log("▶ SC_004_TC_007 — Set plans for " + SETUP_CHILD + " (SAVES)", true);
        if (!copayPage.getColumnValues("Child Id").contains(SETUP_CHILD))
            throw new SkipException("Child " + SETUP_CHILD + " is no longer pending (already set up)");
        copayPage.openUpdatePlan(SETUP_CHILD);
        copayPage.setPopupValue("registration_fee", REGISTRATION_FEE);
        copayPage.setPopupValue("registration_fee_payer", "Parent");
        copayPage.selectPlan("parent_monthly_plan", "add_new_plan");
        // the parent plan dropdown starts on the child's current Rs 1 plan
        copayPage.setPopupValue("parent_monthly_plan_amt", PARENT_MONTHLY);
        copayPage.setPopupValue("monthly_billing_date", BILLING_MONTH);
        copayPage.selectPlan("corp_monthly_plan", "add_new_plan");
        copayPage.setPopupValue("corp_monthly_plan_amt", CORPORATE_MONTHLY);
        copayPage.setPopupValue("corp_monthly_billing_date", BILLING_MONTH);
        String filled = "reg " + copayPage.getPopupValue("registration_fee") + "/" + copayPage.getPopupValue("registration_fee_payer")
                + " | parent " + copayPage.getPopupValue("parent_monthly_plan") + " " + copayPage.getPopupValue("parent_monthly_plan_amt")
                + " " + copayPage.getPopupValue("monthly_billing_date") + " | corporate " + copayPage.getPopupValue("corp_monthly_plan") + " "
                + copayPage.getPopupValue("corp_monthly_plan_amt") + " " + copayPage.getPopupValue("corp_monthly_billing_date")
                + " | coupons " + copayPage.getPopupValue("child_monthly_coupon") + "/" + copayPage.getPopupValue("child_yearly_coupon");
        Reporter.log("   Filled: " + filled, true);
        Assert.assertEquals(filled, "reg " + REGISTRATION_FEE + "/Parent | parent add_new_plan " + PARENT_MONTHLY + " " + BILLING_MONTH
                + " | corporate add_new_plan " + CORPORATE_MONTHLY + " " + BILLING_MONTH + " | coupons remove_discount/remove_discount", "❌ Form not filled as planned");
        copayPage.clickUpdate();
        String message = copayPage.waitForUpdateMessage();
        Reporter.log("   Message: " + message + " | errors " + copayPage.getVisibleErrors(), true);
        Thread.sleep(6000);

        driver.get("https://test-franchise.footprintseducation.in/account_statement?child_id=" + SETUP_CHILD);
        Thread.sleep(7000);
        String body = String.valueOf(((org.openqa.selenium.JavascriptExecutor) driver).executeScript("return document.body.innerText;"));
        String monthly = body.substring(body.indexOf("Monthly Subscription"), body.indexOf("Yearly Subscription")).replaceAll("\\s+", " ");
        Reporter.log("   Account Statement: " + monthly, true);
        reopen();
        boolean stillListed = copayPage.getColumnValues("Child Id").contains(SETUP_CHILD);
        Reporter.log("   Still on Pending Co-Pay Setup: " + stillListed, true);
        Assert.assertFalse(message.isEmpty(), "❌ No message after Update");
        Assert.assertTrue(monthly.contains("14,301.00") || monthly.contains("14301.00"), "❌ Parent monthly 14,301 not set: " + monthly);
        Assert.assertTrue(monthly.contains("8,998.00") || monthly.contains("8998.00"), "❌ Corporate monthly 8,998 not set: " + monthly);
        Assert.assertFalse(stillListed, "❌ Child still pending after the plans were set");
        Reporter.log("✅ SC_004_TC_007 PASSED", true);
    }
}
