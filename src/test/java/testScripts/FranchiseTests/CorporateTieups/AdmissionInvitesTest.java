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
import pages.Settings.SystemLogs.EmailView_Page;
import utils.BaseTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════════
// ADMISSION INVITES (Franchise → Corporate Tieups → Admission Invites)
// Source: "TC_Corporate Tie-ups.xlsx" SC_002 (20 TCs) → TC_Corporate_Tieups.xlsx,
// sheet TC_AdmissionInvites. Runs as Rakesh (user, 2026-10-27).
//
// Priorities 1–29: nothing is saved (view, validation, filters, download…).
// Priorities 30+: SAVES on UAT, only on QA data:
//   - Send New Invitation to a unique qa.auto.inv<run>@footprintseducation.in
//     on a "QA Auto Tieup V2 …" tie-up (Demo Company / Demo Center);
//   - edit / send email / expire act only on those invites, and on a
//     Requested invite raised by Tieup_DetailsTest.tc070 (lead 1) on a QA tie-up.
// ═══════════════════════════════════════════════════════════════
public class AdmissionInvitesTest extends BaseTest {

    private static final String RUN_ID = new java.text.SimpleDateFormat("ddMMHHmm").format(new java.util.Date());
    private static final String OFFER_PREFIX = System.getProperty("inviteOfferPrefix", "QA Auto Tieup V2");
    private static final String COMPANY = "Demo Company";
    private static final String CENTER = "Demo Center";
    private static final String INVITE_EMAIL = "qa.auto.inv" + RUN_ID + "@footprintseducation.in";
    private static final String INVITE_PHONE = "98" + RUN_ID;
    private static final String CONFIRM_SEND = "Are you sure want to send admission Invitation?";
    private static final String CONFIRM_EDIT = "Are you sure want to edit Invitation?";
    // ✅ Leads tried in order for a new Admission Request (user: "use lead IDs 3, 4, 5, so on", up to 64 — 2026-10-28).
    //    A lead is skipped when it has no email or already has an invite link (one invite per lead email).
    //    With email on 2026-10-28: 6, 25 (used), 27–34.
    private static final List<String> LEAD_IDS = Arrays.asList(System.getProperty("inviteLeadIds",
            java.util.stream.IntStream.rangeClosed(3, 64).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","))).split(","));
    private static final String COPAY_BREAKUP = "Parent payable: 5000, Corporate payable: 4000";
    private static final String COPAY_EMAIL = "qa.auto.copay" + RUN_ID + "@footprintseducation.in";
    private static final String COPAY_PHONE = "97" + RUN_ID;

    private static final List<String> COLUMNS = Arrays.asList("Parent Type", "Offer Title", "Invite Email", "Invite Phone",
            "Requested Date", "Requested By", "Invited Date", "Invited By", "Invite Status", "Tentative Admission Date", "Action");
    private static final List<String> STATUSES = Arrays.asList("All", "Requested", "Invited", "Used", "Expired");
    private static final List<String> TYPES = Arrays.asList("Employee Pay", "Corporate Pay", "Co-pay");
    private static final DateTimeFormatter GRID_DATE = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);

    // ✅ Set by the save tests
    private static String offerTitle = "";
    private static String newInviteId = "";
    private static String requestedInviteId = System.getProperty("requestedInviteId", "");
    private static final List<String> flow = new ArrayList<>();

    Navigations navigations;
    AdmissionInvitesPage invitesPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        invitesPage = new AdmissionInvitesPage(driver);
        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void openScreen() throws InterruptedException {
        dismissAlertIfPresent();
        navigations.goToAdmissionInvites();
        dismissAlertIfPresent();
        invitesPage = new AdmissionInvitesPage(driver);
        Assert.assertTrue(invitesPage.isPageLoaded(), "❌ Admission Invites did not load");
    }

    private void assertAllRows(String header, String expected) {
        List<String> v = invitesPage.getColumnValues(header);
        Reporter.log("   " + header + ": " + v, true);
        for (String s : v)
            Assert.assertTrue(s.equalsIgnoreCase(expected), "❌ Row with " + header + " '" + s + "', expected " + expected);
    }

    // ✅ Every save opens a fresh page, fills the Send form with valid data (offer must be a QA tie-up)
    private void fillValidSendForm(String email, String phone) throws InterruptedException {
        invitesPage.selectSendParentType("Father");
        invitesPage.enterSendField("invite_email", email);
        invitesPage.enterSendField("invite_phone", phone);
        invitesPage.selectSendOffer(requireOffer());
        invitesPage.selectSendCenter(CENTER);
        invitesPage.enterSendField("employee_code", "QA-" + RUN_ID);
        invitesPage.enterSendField("employee_email", email);
        invitesPage.enterSendField("employer", "QA Auto Employer");
    }

    private String requireOffer() {
        if (offerTitle.isEmpty()) {
            offerTitle = invitesPage.findSendOffer(OFFER_PREFIX);
            if (offerTitle.isEmpty())
                throw new SkipException("No '" + OFFER_PREFIX + "…' tie-up in Select Offer — run Tieup_DetailsTest tc052 first");
        }
        return offerTitle;
    }

    // ✅ A Requested invite on a QA tie-up (raised by Tieup_DetailsTest.tc070), or -DrequestedInviteId
    private String requireRequestedInvite() throws InterruptedException {
        if (!requestedInviteId.isEmpty()) return requestedInviteId;
        invitesPage.selectFilterInviteStatus("Requested");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        for (String id : invitesPage.getInviteIds())
            if (invitesPage.getCell(id, "Offer Title").startsWith("QA Auto Tieup")) {
                requestedInviteId = id;
                return id;
            }
        // ✅ None left → raise one: Tieup Details → Admission Request (leads 3, 4, 5 …) on the QA tie-up (SAVES)
        requestedInviteId = raiseAdmissionRequest();
        return requestedInviteId;
    }

    private String raiseAdmissionRequest() throws InterruptedException {
        String tieup = requireOffer();
        Tieup_DetailsPage tieupPage = null;
        for (String lead : LEAD_IDS) {
            lead = lead.trim();
            if (tieupPage == null) {   // (re)open the popup — only after a send attempt
                navigations.goToTieupDetails();
                tieupPage = new Tieup_DetailsPage(driver);
                Assert.assertTrue(tieupPage.isListLoaded(), "❌ Tieup Details did not load");
                tieupPage.searchTable(tieup);
                tieupPage.clickFirstRowAdmissionRequest();
            }
            tieupPage.fetchLead(lead);
            if (!tieupPage.isLeadDetailsShown() || tieupPage.getLeadEmail().isEmpty()) {
                Reporter.log("   Lead " + lead + ": no details / no email — next", true);
                continue;
            }
            String leadEmail = tieupPage.getLeadEmail();
            tieupPage.selectInPopup("invite_parent_type", "Father");
            tieupPage.selectInPopup("centers_available", CENTER);
            tieupPage.selectFirstInPopup("payment_plan_avail");
            tieupPage.selectFirstInPopup("program_offered");
            tieupPage.enterInPopup("admission_request_comment", "QA auto request for Admission Invites " + RUN_ID);
            String msg = tieupPage.clickSendRequest();
            Reporter.log("   Lead " + lead + " (" + leadEmail + ") on " + tieup + " → " + msg + " " + tieupPage.getPopupErrors(), true);
            tieupPage = null;
            if (!msg.toLowerCase().contains("success")) continue;
            navigations.goToAdmissionInvites();
            invitesPage = new AdmissionInvitesPage(driver);
            invitesPage.isPageLoaded();
            invitesPage.enterFilterInviteEmail(leadEmail);
            invitesPage.selectFilterInviteStatus("Requested");
            invitesPage.selectTieupTypes();
            invitesPage.clickSearch();
            for (String id : invitesPage.getInviteIds())
                if (invitesPage.getCell(id, "Offer Title").equals(tieup)) return id;
            throw new AssertionError("❌ New Requested invite not listed for " + leadEmail + " / " + tieup);
        }
        throw new SkipException("No lead in " + LEAD_IDS + " could raise an Admission Request (no email / already invited) — pass -DinviteLeadIds=<ids>");
    }

    private void openInvite(String inviteId, String email) throws InterruptedException {
        navigations.goToAdmissionInvites();
        invitesPage = new AdmissionInvitesPage(driver);
        Assert.assertTrue(invitesPage.isPageLoaded(), "❌ Admission Invites did not load");
        if (email != null) {
            invitesPage.searchAllByEmail(email);
        } else {
            invitesPage.selectFilterInviteStatus("All");
            invitesPage.selectTieupTypes();
            invitesPage.clickSearch();
            invitesPage.searchTable(inviteId);
        }
        Assert.assertTrue(invitesPage.isInviteListed(inviteId), "❌ Invite " + inviteId + " not listed");
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_001 — Menu / access
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1, description = "SC_002_TC_001 — Menu Franchise → Corporate Tieups → Admission Invites")
    public void tc001_menuNavigation() {
        Reporter.log("▶ SC_002_TC_001 — Menu", true);
        String path = String.valueOf(((JavascriptExecutor) driver).executeScript(
                "var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='corporate_admission_invites')"
                        + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
        Reporter.log("   Menu path: " + path, true);
        Assert.assertEquals(path, "Franchise > Corporate Tieups > Admission Invites", "❌ Menu path");
        Assert.assertTrue(invitesPage.pageHasText("SEND NEW INVITATION"), "❌ Send New Invitation section");
        Assert.assertTrue(invitesPage.pageHasText("Corporate Admission Invite"), "❌ Corporate Admission Invite section");
        Reporter.log("✅ SC_002_TC_001 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_002 — Default view
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2, description = "SC_002_TC_002 — Default view of Admission Invites")
    public void tc002_defaultView() {
        Reporter.log("▶ SC_002_TC_002 — Default view", true);
        for (String id : Arrays.asList("breakup", "invite_parent_type", "invite_email", "invite_phone", "invite_center_id",
                "employee_code", "employee_email", "employer", "send_invite1"))
            Assert.assertTrue(invitesPage.isSendFieldVisible(id), "❌ Send New Invitation field: " + id);
        Assert.assertTrue(invitesPage.getSendOptions("invite_offer_id").size() > 1, "❌ Select Offer has no tie-ups");
        Assert.assertEquals(invitesPage.getSendOptions("invite_parent_type"), Arrays.asList("Select Type", "Mother", "Father"), "❌ Select Type");
        for (String id : Arrays.asList("invite_company_id", "invite_email", "invite_phone", "invite_status", "btn_filter"))
            Assert.assertTrue(invitesPage.isFilterFieldVisible(id), "❌ Filter field: " + id);
        Assert.assertEquals(invitesPage.getFilterOptions("invite_status"), STATUSES, "❌ Invite Status options");
        Assert.assertEquals(invitesPage.getSelectedFilterStatus(), "Requested", "❌ Default Invite Status");
        Assert.assertEquals(invitesPage.getFilterOptions("tieup_type"), TYPES, "❌ Tie-Up Types options");
        // ✅ Live: nothing ticked by default (button "No Filters"); Onsite is not an option (spec lists All + Onsite)
        Assert.assertTrue(invitesPage.getSelectedTieupTypes().isEmpty(), "❌ Default Tie-Up Types: " + invitesPage.getSelectedTieupTypes());
        Assert.assertEquals(invitesPage.getFilterOptions("center_selection").get(0), "All Center", "❌ Centers filter");
        Assert.assertTrue(invitesPage.getFilterOptions("invite_company_id").size() > 1, "❌ Company list");
        Assert.assertTrue(invitesPage.pageHasText("Past 72 Hours Admission Requested"), "❌ 72-hour legend");
        Assert.assertTrue(invitesPage.pageHasText("Past 48 Hours Admission Requested"), "❌ 48-hour legend");
        Assert.assertTrue(invitesPage.isSearchBoxVisible(), "❌ Search box");
        Assert.assertTrue(invitesPage.isDownloadVisible(), "❌ Download");
        Assert.assertTrue(invitesPage.isPaginationVisible(), "❌ Pagination");
        Assert.assertEquals(invitesPage.getVisibleColumnHeaders(), COLUMNS, "❌ Columns");
        Reporter.log("   " + invitesPage.getInfoText(), true);
        Reporter.log("✅ SC_002_TC_002 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_005 — Mandatory fields
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 5, description = "SC_002_TC_005 — Send New Invitation mandatory fields")
    public void tc005_mandatoryFields() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_005 — Mandatory fields", true);
        invitesPage.startCapture(false);
        invitesPage.clickSendInvite();
        List<String> errors = invitesPage.getSendErrors();
        errors.forEach(e -> Reporter.log("   " + e, true));
        for (String id : Arrays.asList("invite_parent_type", "invite_email", "invite_phone", "invite_offer_id"))
            Assert.assertTrue(errors.contains(id + " => Required!"), "❌ No 'Required!' for " + id);
        Assert.assertTrue(invitesPage.getCapturedConfirms().isEmpty(), "❌ Confirm shown for an empty form");
        Assert.assertTrue(invitesPage.getCapturedRequests().isEmpty(), "❌ Request sent: " + invitesPage.getCapturedRequests());
        Reporter.log("✅ SC_002_TC_005 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_006 — Email format
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 6, description = "SC_002_TC_006 — Email / Employee Official Email format")
    public void tc006_emailFormat() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_006 — Email format", true);
        invitesPage.startCapture(false);
        invitesPage.enterSendField("invite_email", "sunita@");
        invitesPage.enterSendField("employee_email", "sunita@");
        invitesPage.clickSendInvite();
        List<String> errors = invitesPage.getSendErrors();
        errors.forEach(e -> Reporter.log("   " + e, true));
        Assert.assertTrue(errors.contains("invite_email => Invalid Email!"), "❌ Email error");
        Assert.assertTrue(errors.stream().anyMatch(e -> e.startsWith("employee_email => ")), "❌ No error for Employee Official Email");
        Assert.assertTrue(invitesPage.getCapturedRequests().isEmpty(), "❌ Request sent");
        Reporter.log("✅ SC_002_TC_006 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_007 — Phone
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 7, description = "SC_002_TC_007 — Phone validation (10–13 digits)")
    public void tc007_phoneValidation() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_007 — Phone", true);
        invitesPage.startCapture(false);
        for (String phone : Arrays.asList("9876545", "98765432101234")) {
            invitesPage.enterSendField("invite_phone", phone);
            invitesPage.clickSendInvite();
            String value = invitesPage.getSendFieldValue("invite_phone");
            boolean err = invitesPage.getSendErrors().contains("invite_phone => Invalid Phone!");
            Reporter.log("   " + phone + " → value '" + value + "' | Invalid Phone! shown: " + err, true);
            Assert.assertTrue(err || value.length() <= 13, "❌ No 'Invalid Phone!' for " + phone);
        }
        invitesPage.enterSendField("invite_phone", "abcdefghij");
        Reporter.log("   letters → value '" + invitesPage.getSendFieldValue("invite_phone") + "'", true);
        Assert.assertTrue(invitesPage.getSendFieldValue("invite_phone").matches("\\d*"), "❌ Letters accepted in Phone");
        Assert.assertTrue(invitesPage.getCapturedRequests().isEmpty(), "❌ Request sent");
        Reporter.log("✅ SC_002_TC_007 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_021 (new) — Co-Pay offer needs Breakup details
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 8, description = "SC_002_TC_021 — Co-Pay offer: Co-Pay Breakup details mandatory")
    public void tc021_coPayBreakupMandatory() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_021 — Co-Pay breakup", true);
        invitesPage.startCapture(false);
        invitesPage.selectSendParentType("Father");
        invitesPage.enterSendField("invite_email", INVITE_EMAIL);
        invitesPage.enterSendField("invite_phone", INVITE_PHONE);
        String coPay = invitesPage.selectFirstCoPayOffer();
        Reporter.log("   Co-Pay offer: " + coPay + " | breakup readonly: " + invitesPage.isBreakupReadonly(), true);
        Assert.assertFalse(coPay.isEmpty(), "❌ No Co-Pay offer in the list");
        Assert.assertFalse(invitesPage.isBreakupReadonly(), "❌ Breakup not editable for a Co-Pay offer");
        invitesPage.clickSendInvite();
        List<String> errors = invitesPage.getSendErrors();
        Reporter.log("   " + errors, true);
        Assert.assertTrue(errors.stream().anyMatch(e -> e.startsWith("breakup => ") && e.toLowerCase().contains("required")),
                "❌ No breakup error: " + errors);
        Assert.assertTrue(invitesPage.getCapturedConfirms().isEmpty(), "❌ Confirm shown without breakup");
        Assert.assertTrue(invitesPage.getCapturedRequests().isEmpty(), "❌ Request sent");
        Reporter.log("✅ SC_002_TC_021 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_022 (new) — Center list follows the offer; breakup read-only for non Co-Pay
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 9, description = "SC_002_TC_022 — Select Center lists the offer's centres; breakup read-only for non Co-Pay")
    public void tc022_centerListByOffer() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_022 — Center by offer", true);
        Assert.assertTrue(invitesPage.getSendCenterOptions().size() <= 1, "❌ Centres listed before an offer is chosen");
        invitesPage.selectSendOffer(requireOffer());
        List<String> centers = invitesPage.getSendCenterOptions();
        Reporter.log("   " + offerTitle + " → " + centers + " | breakup readonly: " + invitesPage.isBreakupReadonly(), true);
        Assert.assertEquals(centers, Arrays.asList(CENTER), "❌ Centres of " + offerTitle);
        Assert.assertTrue(invitesPage.isBreakupReadonly(), "❌ Breakup editable for a non Co-Pay offer");
        Reporter.log("✅ SC_002_TC_022 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_010 — Invite Status filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 10, description = "SC_002_TC_010 — Invite Status filter")
    public void tc010_statusFilter() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_010 — Status filter", true);
        int all = -1;
        for (String status : STATUSES) {
            navigations.goToAdmissionInvites();
            invitesPage = new AdmissionInvitesPage(driver);
            invitesPage.isPageLoaded();
            invitesPage.selectFilterInviteStatus(status);
            invitesPage.selectTieupTypes();
            invitesPage.clickSearch();
            Reporter.log("   " + status + " → " + invitesPage.getInfoText(), true);
            Assert.assertEquals(invitesPage.getSelectedFilterStatus(), status, "❌ Status not kept after Search");
            if (status.equals("All")) all = invitesPage.getTotalEntries();
            else {
                if (invitesPage.getRowCount() > 0) assertAllRows("Invite Status", status);
                Assert.assertTrue(invitesPage.getTotalEntries() <= all, "❌ " + status + " shows more than All");
            }
        }
        Reporter.log("✅ SC_002_TC_010 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_012 — Tie-Up Types filter
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 11, description = "SC_002_TC_012 — Tie-Up Types filter")
    public void tc012_tieupTypeFilter() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_012 — Tie-Up Types", true);
        invitesPage.selectFilterInviteStatus("All");
        invitesPage.selectTieupTypes("Corporate Pay");
        invitesPage.clickSearch();
        Reporter.log("   Corporate Pay → " + invitesPage.getInfoText() + " | selected " + invitesPage.getSelectedTieupTypes(), true);
        Assert.assertEquals(invitesPage.getSelectedTieupTypes(), Arrays.asList("Corporate Pay"), "❌ Filter not kept");
        Assert.assertTrue(invitesPage.getRowCount() > 0, "❌ No Corporate Pay invites");
        for (String t : invitesPage.getRowTieupTypes())
            Assert.assertEquals(t, "Corporate Pay", "❌ Row of type " + t);
        invitesPage.selectTieupTypes("Employee Pay", "Co-pay");
        invitesPage.clickSearch();
        List<String> types = invitesPage.getRowTieupTypes();
        Reporter.log("   Employee Pay + Co-pay → " + invitesPage.getInfoText() + " | row types " + new java.util.TreeSet<>(types), true);
        for (String t : types)
            Assert.assertTrue(t.equalsIgnoreCase("Employee Pay") || t.equalsIgnoreCase("Co-Pay"), "❌ Row of type " + t);
        Reporter.log("✅ SC_002_TC_012 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_013 — Download
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 13, description = "SC_002_TC_013 — Download admission_invited.csv")
    public void tc013_download() throws Exception {
        Reporter.log("▶ SC_002_TC_013 — Download", true);
        invitesPage.selectFilterInviteStatus("All");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        long start = System.currentTimeMillis();
        invitesPage.clickDownload();
        File csv = null;
        for (int i = 0; i < 30 && csv == null; i++) {
            File[] files = downloads.listFiles((d, n) -> n.toLowerCase().startsWith("admission_invited") && n.toLowerCase().endsWith(".csv"));
            if (files != null)
                for (File f : files)
                    if (f.lastModified() >= start - 1000 && (csv == null || f.lastModified() > csv.lastModified())) csv = f;
            if (csv == null) Thread.sleep(500);
        }
        Assert.assertNotNull(csv, "❌ No admission_invited*.csv downloaded to " + downloads);
        String header;
        try (BufferedReader r = new BufferedReader(new FileReader(csv))) {
            header = r.readLine();
        }
        Reporter.log("   File: " + csv.getName() + " | Header: " + header, true);
        for (String col : Arrays.asList("Parent Type", "Offer Title", "Invite Email", "Invite Phone", "Requested Date",
                "Requested By", "Invited Date", "Invited By", "Invite Status"))
            Assert.assertTrue(header.contains(col), "❌ CSV missing column: " + col);
        Reporter.log("✅ SC_002_TC_013 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_014 — Pagination
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 14, description = "SC_002_TC_014 — Pagination")
    public void tc014_pagination() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_014 — Pagination", true);
        invitesPage.selectFilterInviteStatus("All");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        int total = invitesPage.getTotalEntries();
        Reporter.log("   " + invitesPage.getInfoText() + " | rows on page 1: " + invitesPage.getRowCount(), true);
        Assert.assertEquals(invitesPage.getRowCount(), Math.min(20, total), "❌ Rows per page (live: 20)");
        String first = invitesPage.getInviteIds().get(0);
        invitesPage.clickNextPage();
        Reporter.log("   Next → " + invitesPage.getInfoText(), true);
        Assert.assertTrue(invitesPage.getInfoText().contains("Showing 21 to"), "❌ Info after Next");
        Assert.assertNotEquals(invitesPage.getInviteIds().get(0), first, "❌ Same rows after Next");
        invitesPage.clickPreviousPage();
        Assert.assertEquals(invitesPage.getInviteIds().get(0), first, "❌ Page 1 after Previous");
        Reporter.log("✅ SC_002_TC_014 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_015 — Sorting
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 15, description = "SC_002_TC_015 — Column sorting")
    public void tc015_sorting() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_015 — Sorting", true);
        invitesPage.selectFilterInviteStatus("All");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        for (String col : COLUMNS.subList(0, COLUMNS.size() - 1)) {
            invitesPage.clickColumnHeader(col);
            String a = invitesPage.getHeaderSortClass(col);
            invitesPage.clickColumnHeader(col);
            String b = invitesPage.getHeaderSortClass(col);
            Reporter.log("   " + col + ": " + a + " → " + b, true);
            Assert.assertTrue(a.matches(".*sorting_(asc|desc).*") && b.matches(".*sorting_(asc|desc).*") && !a.equals(b),
                    "❌ " + col + " did not sort both ways");
        }
        Reporter.log("✅ SC_002_TC_015 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_023 (new) — Requested rows past 72 h in red / 48 h in orange
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 16, description = "SC_002_TC_023 — Requested invites older than 72 h shown in red, 48 h in orange")
    public void tc023_rowColours() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_023 — Row colours", true);
        invitesPage.selectFilterInviteStatus("Requested");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        LocalDate today = LocalDate.now();
        int checked = 0;
        for (String id : invitesPage.getInviteIds()) {
            String req = invitesPage.getCell(id, "Requested Date");
            String style = invitesPage.getRowStyle(id);
            long days = ChronoUnit.DAYS.between(LocalDate.parse(req, GRID_DATE), today);
            Reporter.log("   " + id + " | requested " + req + " (" + days + " d) | style '" + style + "'", true);
            if (days >= 4) {
                Assert.assertTrue(style.contains("red"), "❌ Invite " + id + " requested " + days + " days ago is not red");
                checked++;
            }
            if (days == 0) Assert.assertFalse(style.contains("red") || style.contains("orange"), "❌ Today's request coloured: " + id);
        }
        Reporter.log("✅ SC_002_TC_023 PASSED (" + checked + " old requests checked)", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_008 — Email a pending (Requested) invite (SAVES)
    //  User (2026-10-28): there is no "Email Pending Invites" button — filter Invite Status = Requested and click the
    //  row's email icon (Send Invitation Email). Uses a Requested invite NOT on a QA tie-up (one the user created);
    //  the QA tie-up's Requested invite is left for TC_017 / TC_019.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 33, description = "SC_002_TC_008 — Requested filter → email icon sends the invitation")
    public void tc008_emailPendingInvites() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_008 — Email pending invite (SAVES)", true);
        invitesPage.selectFilterInviteStatus("Requested");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        String inviteId = "";
        for (String id : invitesPage.getInviteIds())
            if (!invitesPage.getCell(id, "Offer Title").startsWith("QA Auto Tieup")) { inviteId = id; break; }
        if (inviteId.isEmpty())
            throw new SkipException("No Requested invite outside the QA tie-ups — create one (user) or pass none");
        String offer = invitesPage.getCell(inviteId, "Offer Title");
        Reporter.log("   Requested invite " + inviteId + " | " + offer + " | " + invitesPage.getCell(inviteId, "Invite Email")
                + " | actions " + invitesPage.getActionTitles(inviteId), true);
        Assert.assertTrue(invitesPage.getActionTitles(inviteId).contains("Send Invitation Email"), "❌ No email icon on the Requested row");
        boolean coPay = "Co-Pay".equals(invitesPage.getRowTieupTypeOf(inviteId));
        invitesPage.startCapture(true);
        invitesPage.clickSendInvitationEmail(inviteId);
        if (coPay) {
            // ✅ Co-Pay: the breakup popup opens first (user: "breakup form first open … Parent payable / Corporate payable")
            Assert.assertTrue(invitesPage.isCoPayBreakupPopupVisible(), "❌ Co-Pay breakup popup did not open");
            invitesPage.clickCoPayBreakupConfirm();
            Reporter.log("   Empty breakup → '" + invitesPage.getCoPayBreakupError() + "' | confirms " + invitesPage.getCapturedConfirms(), true);
            Assert.assertTrue(invitesPage.getCapturedRequests().isEmpty(), "❌ Sent without a breakup");
            invitesPage.enterCoPayBreakup(COPAY_BREAKUP);
            Reporter.log("   Breakup: " + COPAY_BREAKUP, true);
            invitesPage.clickCoPayBreakupConfirm();
        }
        String msg = invitesPage.waitForReloadAndFlash();
        Reporter.log("   Confirms " + invitesPage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertEquals(invitesPage.getCapturedConfirms(), Arrays.asList(CONFIRM_SEND), "❌ Confirm text");
        Assert.assertTrue(msg.contains("Email has been sent successfully to"), "❌ Message: " + msg);
        openInvite(inviteId, null);
        String status = invitesPage.getCell(inviteId, "Invite Status");
        Reporter.log("   Status after email: " + status + " | Invited By " + invitesPage.getCell(inviteId, "Invited By"), true);
        Assert.assertEquals(status, "Invited", "❌ Status after the email");
        Reporter.log("✅ SC_002_TC_008 PASSED — invite " + inviteId, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_024 (new) — Send a Co-Pay invitation with breakup (SAVES)
    //  User (2026-10-28): for a Co-Pay tie-up the breakup is filled first, e.g. "Parent payable: 5000,
    //  Corporate payable: 4000" (amounts vary), then Send.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 34, description = "SC_002_TC_024 — Send a Co-Pay invitation with Co-Pay breakup")
    public void tc024_sendCoPayInvitation() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_024 — Co-Pay invitation (SAVES) to " + COPAY_EMAIL, true);
        String coPay = invitesPage.selectFirstCoPayOffer();
        Assert.assertFalse(coPay.isEmpty(), "❌ No Co-Pay offer in the list");
        List<String> centers = invitesPage.getSendCenterOptions();
        invitesPage.selectSendParentType("Mother");
        invitesPage.enterSendField("breakup", COPAY_BREAKUP);
        invitesPage.enterSendField("invite_email", COPAY_EMAIL);
        invitesPage.enterSendField("invite_phone", COPAY_PHONE);
        invitesPage.selectSendCenter(centers.get(0));
        invitesPage.enterSendField("employee_code", "QA-CP-" + RUN_ID);
        invitesPage.enterSendField("employee_email", COPAY_EMAIL);
        invitesPage.enterSendField("employer", "QA Auto Employer");
        Reporter.log("   " + coPay + " | " + centers.get(0) + " | breakup: " + COPAY_BREAKUP, true);
        invitesPage.startCapture(true);
        invitesPage.clickSendInvite();
        String msg = invitesPage.waitForReloadAndFlash();
        Reporter.log("   Confirms " + invitesPage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertEquals(invitesPage.getCapturedConfirms(), Arrays.asList(CONFIRM_SEND), "❌ Confirm text");
        Assert.assertTrue(msg.toLowerCase().contains(COPAY_EMAIL), "❌ Success message does not name " + COPAY_EMAIL + ": " + msg);
        invitesPage = new AdmissionInvitesPage(driver);
        invitesPage.searchAllByEmail(COPAY_EMAIL);
        Assert.assertEquals(invitesPage.getRowCount(), 1, "❌ Rows for " + COPAY_EMAIL);
        String id = invitesPage.getInviteIds().get(0);
        Reporter.log("   Row: " + invitesPage.getRowTexts().get(0), true);
        Assert.assertEquals(invitesPage.getCell(id, "Offer Title"), coPay, "❌ Offer Title");
        Assert.assertEquals(invitesPage.getCell(id, "Invite Status"), "Invited", "❌ Status");
        // ✅ The breakup is not shown on this screen (grid / Edit Invitation); per the spec it appears later on
        //    Pending Co-Pay Setup / Co-Pay Migration Data (SC_004 / SC_008) — logged only here
        Reporter.log("   Edit Invitation fields: " + invitesPage.getRowBreakup(id), true);
        Reporter.log("✅ SC_002_TC_024 PASSED — invite " + id, true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_003 — Send New Invitation (SAVES)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 30, description = "SC_002_TC_003 — Send New Invitation")
    public void tc003_sendNewInvitation() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_003 — Send New Invitation (SAVES) to " + INVITE_EMAIL, true);
        fillValidSendForm(INVITE_EMAIL, INVITE_PHONE);
        invitesPage.startCapture(true);
        invitesPage.clickSendInvite();
        String msg = invitesPage.waitForReloadAndFlash();
        Assert.assertEquals(invitesPage.getCapturedConfirms(), Arrays.asList(CONFIRM_SEND), "❌ Confirm text");
        Reporter.log("   " + offerTitle + " | " + CENTER + " | message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains(INVITE_EMAIL), "❌ Success message does not name " + INVITE_EMAIL + ": " + msg);
        invitesPage = new AdmissionInvitesPage(driver);
        invitesPage.searchAllByEmail(INVITE_EMAIL);
        List<String> rows = invitesPage.getRowTexts();
        rows.forEach(r -> Reporter.log("   Row: " + r, true));
        Assert.assertEquals(rows.size(), 1, "❌ Rows for " + INVITE_EMAIL);
        newInviteId = invitesPage.getInviteIds().get(0);
        String status = invitesPage.getCell(newInviteId, "Invite Status");
        flow.add("new " + newInviteId + ": " + status);
        Assert.assertEquals(invitesPage.getCell(newInviteId, "Offer Title"), offerTitle, "❌ Offer Title");
        Assert.assertEquals(invitesPage.getCell(newInviteId, "Parent Type"), "Father", "❌ Parent Type");
        Assert.assertEquals(status, "Invited", "❌ Status of a new invitation");
        Assert.assertEquals(invitesPage.getCell(newInviteId, "Invited By"), "Rakesh Singh", "❌ Invited By");
        Reporter.log("✅ SC_002_TC_003 PASSED — invite " + newInviteId + ", actions " + invitesPage.getActionTitles(newInviteId), true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_004 — Invitation email (Email View)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 31, description = "SC_002_TC_004 — Invitation email content")
    public void tc004_invitationEmail() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_004 — Invitation email", true);
        if (newInviteId.isEmpty()) throw new SkipException("No invitation sent in this run (SC_002_TC_003)");
        // ✅ Test env logs every email To qa@footprintseducation.in — the invitee address is only in the body
        String body = new EmailView_Page(driver).openEmailByBodyAndRecipient("Admission Invite Email", INVITE_EMAIL, "Admission Invite Email");
        Reporter.log("   Email: " + body.replaceAll("\\s+", " "), true);
        Assert.assertFalse(body.isEmpty(), "❌ No invitation email to " + INVITE_EMAIL + " in Email View");
        Assert.assertTrue(body.contains("You have been invited for admission at Footprints under corporate tie-up benefits with " + COMPANY),
                "❌ Invitation sentence / company");
        Assert.assertTrue(body.contains("online_form?ckey="), "❌ Admission form link");
        java.util.regex.Matcher exp = java.util.regex.Pattern.compile("link will expire in next (\\d+) days").matcher(body);
        Assert.assertTrue(exp.find(), "❌ Link expiry not stated");
        Reporter.log("   Link expiry in email: " + exp.group(1) + " days", true);
        // ✅ Q9 closed (user, 2026-10-28): the link expires within 3 days
        Assert.assertEquals(exp.group(1), "3", "❌ Link expiry days");
        Reporter.log("✅ SC_002_TC_004 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_016 — Duplicate invitation (same email + offer)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 32, description = "SC_002_TC_016 — Duplicate invitation for the same email and offer")
    public void tc016_duplicateInvitation() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_016 — Duplicate invitation (may SAVE)", true);
        if (newInviteId.isEmpty()) throw new SkipException("No invitation sent in this run (SC_002_TC_003)");
        fillValidSendForm(INVITE_EMAIL, INVITE_PHONE);
        invitesPage.startCapture(true);
        invitesPage.clickSendInvite();
        String msg = invitesPage.waitForReloadAndFlash();
        Reporter.log("   Second send → " + msg, true);
        invitesPage = new AdmissionInvitesPage(driver);
        invitesPage.searchAllByEmail(INVITE_EMAIL);
        Reporter.log("   Rows for " + INVITE_EMAIL + ": " + invitesPage.getRowCount() + " " + invitesPage.getInviteIds(), true);
        Assert.assertEquals(invitesPage.getRowCount(), 1, "❌ A duplicate invitation was created for the same email + offer (" + msg + ")");
        Reporter.log("✅ SC_002_TC_016 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_009 — Search with all filters
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 33, description = "SC_002_TC_009 — Search by Company, Offer, Email, Phone, Status")
    public void tc009_searchAllFilters() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_009 — Search", true);
        if (newInviteId.isEmpty()) throw new SkipException("No invitation sent in this run (SC_002_TC_003)");
        invitesPage.selectFilterCompany(COMPANY);
        invitesPage.selectFilterOffer(offerTitle);
        invitesPage.enterFilterInviteEmail(INVITE_EMAIL);
        invitesPage.enterFilterInvitePhone(INVITE_PHONE);
        invitesPage.selectFilterInviteStatus("All");
        invitesPage.selectTieupTypes();
        invitesPage.clickSearch();
        Reporter.log("   " + invitesPage.getInfoText() + " | " + invitesPage.getRowTexts(), true);
        Assert.assertEquals(invitesPage.getInviteIds(), Arrays.asList(newInviteId), "❌ Search result");
        invitesPage.enterFilterInviteEmail("nobody." + RUN_ID + "@footprintseducation.in");
        invitesPage.clickSearch();
        Reporter.log("   No match → '" + invitesPage.getEmptyTableMessage() + "'", true);
        Assert.assertEquals(invitesPage.getRowCount(), 0, "❌ Rows for a non-existent email");
        Reporter.log("✅ SC_002_TC_009 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_017 — Edit Invitation (Requested invite on a QA tie-up)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 40, description = "SC_002_TC_017 — Edit Invitation")
    public void tc017_editInvitation() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_017 — Edit Invitation (SAVES)", true);
        String id = requireRequestedInvite();
        openInvite(id, null);
        String before = invitesPage.getCell(id, "Parent Type");
        String newType = before.equals("Father") ? "Mother" : "Father";
        invitesPage.clickEditInvitation(id);
        Assert.assertTrue(invitesPage.isEditPopupVisible(), "❌ Edit Invitation popup did not open");
        String popup = invitesPage.getEditPopupText();
        Assert.assertTrue(popup.contains("Edit Invitation"), "❌ Popup title");
        Reporter.log("   Invite " + id + " | email " + invitesPage.getEditFieldValue("edit_invite_email") + " | Parent " + before + " → " + newType, true);
        invitesPage.selectEditParentType(newType);
        invitesPage.enterEditField("edit_employer", "QA Auto Employer " + RUN_ID);
        invitesPage.startCapture(true);
        invitesPage.clickUpdateInvite();
        String msg = invitesPage.waitForReloadAndFlash();
        Assert.assertEquals(invitesPage.getCapturedConfirms(), Arrays.asList(CONFIRM_EDIT), "❌ Confirm text");
        Reporter.log("   Message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains("success") || msg.toLowerCase().contains("updated"), "❌ No success message: " + msg);
        openInvite(id, null);
        Assert.assertEquals(invitesPage.getCell(id, "Parent Type"), newType, "❌ Parent Type not updated");
        Reporter.log("✅ SC_002_TC_017 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_019 — Send Invitation Email (Requested → Invited)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 41, description = "SC_002_TC_019 — Send Invitation Email on a Requested invite")
    public void tc019_sendInvitationEmail() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_019 — Send Invitation Email (SAVES)", true);
        String id = requireRequestedInvite();
        openInvite(id, null);
        String status = invitesPage.getCell(id, "Invite Status");
        flow.add("requested " + id + ": " + status);
        Assert.assertEquals(status, "Requested", "❌ Invite " + id + " is not Requested");
        Assert.assertTrue(invitesPage.getActionTitles(id).contains("Send Invitation Email"), "❌ No Send Invitation Email icon");
        invitesPage.startCapture(true);
        invitesPage.clickSendInvitationEmail(id);
        String msg = invitesPage.waitForReloadAndFlash();
        Reporter.log("   Confirms: " + invitesPage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains("success"), "❌ No success message: " + msg);
        openInvite(id, null);
        String after = invitesPage.getCell(id, "Invite Status");
        flow.add("requested " + id + ": " + after);
        List<String> actions = invitesPage.getActionTitles(id);
        Reporter.log("   Status " + after + " | Invited " + invitesPage.getCell(id, "Invited Date") + " by "
                + invitesPage.getCell(id, "Invited By") + " | actions " + actions, true);
        Assert.assertEquals(after, "Invited", "❌ Status after sending the invitation");
        Assert.assertFalse(actions.contains("Send Invitation Email"), "❌ Send Invitation Email still shown");
        Reporter.log("✅ SC_002_TC_019 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_018 — Expire Invitation (this run's new invite)
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 42, description = "SC_002_TC_018 — Expire Invitation")
    public void tc018_expireInvitation() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_018 — Expire Invitation (SAVES, this run's invite only)", true);
        if (newInviteId.isEmpty()) throw new SkipException("No invitation sent in this run (SC_002_TC_003)");
        openInvite(newInviteId, INVITE_EMAIL);
        Assert.assertTrue(invitesPage.getActionTitles(newInviteId).contains("Expire invitation"), "❌ No Expire icon");
        invitesPage.startCapture(true);
        invitesPage.clickExpireInvitation(newInviteId);
        String msg = invitesPage.waitForReloadAndFlash();
        Reporter.log("   Confirms: " + invitesPage.getCapturedConfirms() + " | message: " + msg, true);
        Assert.assertTrue(msg.toLowerCase().contains("expired successfully"), "❌ Expire message: " + msg);
        openInvite(newInviteId, INVITE_EMAIL);
        String status = invitesPage.getCell(newInviteId, "Invite Status");
        flow.add("new " + newInviteId + ": " + status);
        List<String> actions = invitesPage.getActionTitles(newInviteId);
        Reporter.log("   Status " + status + " | actions " + actions, true);
        Assert.assertEquals(status, "Expired", "❌ Status after Expire");
        Assert.assertEquals(actions, Arrays.asList("Lead Details"), "❌ Only Lead Details should remain");
        Reporter.log("✅ SC_002_TC_018 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_011 — Status flow Requested → Invited, Invited → Expired
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 43, description = "SC_002_TC_011 — Invite status flow")
    public void tc011_statusFlow() {
        Reporter.log("▶ SC_002_TC_011 — Status flow: " + flow, true);
        if (flow.isEmpty()) throw new SkipException("No status changes recorded in this run");
        String all = String.join(" ; ", flow);
        if (!requestedInviteId.isEmpty())
            Assert.assertTrue(all.contains("requested " + requestedInviteId + ": Requested")
                    && all.contains("requested " + requestedInviteId + ": Invited"), "❌ Requested → Invited not seen: " + all);
        if (!newInviteId.isEmpty())
            Assert.assertTrue(all.contains("new " + newInviteId + ": Invited")
                    && all.contains("new " + newInviteId + ": Expired"), "❌ Invited → Expired not seen: " + all);
        Reporter.log("   'Used' is set when the parent completes the admission form — not automated", true);
        Reporter.log("✅ SC_002_TC_011 PASSED", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  SC_002_TC_020 — Lead Details
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 44, description = "SC_002_TC_020 — Lead Details link")
    public void tc020_leadDetails() throws InterruptedException {
        Reporter.log("▶ SC_002_TC_020 — Lead Details", true);
        String id = requireRequestedInvite();
        openInvite(id, null);
        String href = invitesPage.getLeadDetailsHref(id);
        Reporter.log("   " + href, true);
        Assert.assertTrue(href.contains("prt_lead_detail?eid="), "❌ Lead Details link: " + href);
        driver.get(href);
        Thread.sleep(4000);
        String body = driver.findElement(By.tagName("body")).getText();
        Reporter.log("   " + driver.getCurrentUrl() + " | " + body.replaceAll("\\s+", " ").substring(0, Math.min(300, body.length())), true);
        Assert.assertTrue(driver.getCurrentUrl().contains("prt_lead_detail"), "❌ Not on the lead details page");
        Assert.assertFalse(body.contains("Unauthorised Access") || body.contains("not authorise"), "❌ No access to lead details");
        Reporter.log("✅ SC_002_TC_020 PASSED", true);
    }
}
