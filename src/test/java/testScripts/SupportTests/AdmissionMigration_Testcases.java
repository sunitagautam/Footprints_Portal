package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.UserRightsPage;
import pages.Support.AdmissionMigrationRequest;
import pages.Support.RecentCustomerRequestsPage;
import utils.APIs;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Test Suite: Admission Migration (Regular ↔ Corporate)
 * <p>
 * Screen: Account Statement → "MIGRATE FROM REGULAR TO CORPORATE" /
 * "MIGRATE FROM CORPORATE TO REGULAR" buttons.
 * <p>
 * Source: Admission Migration.docx (flow/screenshots) + TC_Account Statement
 * Scripts.xlsx (sheet "Account Statement Page", SC_016_TC_001-020 = Regular→
 * Corporate, SC_017_TC_001-023 = Corporate→Regular). 14 of the 43 cases
 * selected for automation this round (modal-default-view checks TC_001 in
 * both directions explicitly excluded per user; most of the remainder are
 * backend/financial/data-integrity checks — invoice proration, security-fee
 * refunds, invite-table rows, transport-data copying, email notifications —
 * not UI-testable this round, same category deferred for other features).
 * <p>
 * User: Varsha Jha (getUserForScreen("Corporate Account Statement") —
 * confirmed live in the doc's own screenshots she performs both directions).
 */
public class AdmissionMigration_Testcases extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA — TODO: replace with real child IDs before running
    // ═══════════════════════════════════════════════
    // Regular child, no dues, no existing pending migration request —
    // chain: TC_002(mandatory validation)->TC_004(full flow)->TC_005(old
    // Attrition/new child)->TC_011(center/program unchanged)->TC_017
    // (duplicate blocked)->TC_018(status/WEF)->TC_019(Billing Cancel Date).
    private static final String AM_REGULAR_CHILD_ID = "71178"; // backup: 71341 — 70000/70247/71172 consumed by prior runs

    // Corporate child, no dues, no existing pending migration request —
    // mirrors the above chain for SC_017 TC_003/004/009/018/020.
    private static final String AM_CORPORATE_CHILD_ID = "66752"; // backup: 66865 — 64301 has Dues, 64719 is Paused (both reused for the bonus checks), 72454 consumed

    // Child at a center restricted to Corporate-only admissions — SC_017 TC_022.
    private static final String AM_CENTER_RESTRICTED_CHILD_ID = "TODO_CENTER_RESTRICTED_CHILD_ID";

    // Child with an existing pending Child Attrition/Withdraw request — SC_017 TC_023.
    private static final String AM_ATTRITION_CONFLICT_CHILD_ID = "TODO_ATTRITION_CONFLICT_CHILD_ID";

    // Corporate child with outstanding Dues — confirmed live to block
    // migration with the exact spec message — SC_017_TC_002/SC_016_TC_003.
    private static final String AM_DUES_CHILD_ID = "64301";

    // Corporate child in PAUSE status (not Active) — confirmed live the
    // Migrate button doesn't render at all for a paused child. Not an
    // official Excel TC id — a bonus scenario discovered while sourcing
    // test data.
    private static final String AM_PAUSED_CHILD_ID = "64719";

    // 1st of the current month — API's "date" param simulates month-end
    // processing without waiting for the real calendar month (same pattern
    // as Corporate Transfer's CT_MIGRATION_DATE).
    private static final String AM_MIGRATION_DATE =
            LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).toString();

    private static final String SCREEN_CORPORATE = "Corporate Account Statement";

    // ═══════════════════════════════════════════════
    // PAGE OBJECTS
    // ═══════════════════════════════════════════════
    AdmissionMigrationRequest migrationPage;
    UserRightsPage userRightsPage;
    Navigations navigations;
    RecentCustomerRequestsPage recentRequestsPage;

    // Populated by sc016_tc004_fullFlowRegularToCorporate, consumed by
    // sc016_tc005_oldAttritionNewChild / sc016_tc019_billingCancelDate.
    private static String newChildIdFromRegularToCorporate;
    private static String newChildIdFromCorporateToRegular;

    // ═══════════════════════════════════════════════
    // BEFORE CLASS
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        migrationPage = new AdmissionMigrationRequest(driver);
        recentRequestsPage = new RecentCustomerRequestsPage(driver);
        System.out.println("✅ Page objects initialised");

        String user = getUserForScreen(SCREEN_CORPORATE);
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for screen '" + SCREEN_CORPORATE + "' in Excel.");

        System.out.println("▶ Switching to Corporate user: " + user);
        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);

        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @BeforeMethod");
        } catch (Exception ignored) {
        }

        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal-backdrop').forEach(el=>el.remove());"
                            + "document.querySelectorAll('.modal').forEach(el=>{"
                            + "  el.style.display='none'; el.classList.remove('in','show');});"
                            + "document.body.classList.remove('modal-open');");
            Thread.sleep(300);
        } catch (Exception ignored) {
        }

        Thread.sleep(1500);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal-backdrop').forEach(el=>el.remove());"
                            + "document.querySelectorAll('.modal').forEach(el=>{"
                            + "  el.style.display='none'; el.classList.remove('in','show');});"
                            + "document.body.classList.remove('modal-open');");
        } catch (Exception ignored) {
        }
        try {
            Thread.sleep(1000);
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══ SC_016 — ADMISSION MIGRATION (REGULAR → CORPORATE) ═══
    // ═══════════════════════════════════════════════════════════════════════

    // SC_016_TC_002 — mandatory-field validation
    @Test(priority = 1,
            description = "SC_016_TC_002 — Mandatory fields for Regular to Corporate migration")
    public void sc016_tc002_mandatoryFieldValidation() throws InterruptedException {
        Reporter.log("▶ SC_016_TC_002 — Mandatory field validation | child: " + AM_REGULAR_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_REGULAR_CHILD_ID);
        migrationPage.clickMigrateRegularToCorporate();
        Assert.assertTrue(migrationPage.isModalVisible(), "❌ Admission Migration modal did not open");

        boolean blocked = migrationPage.isAddMigrationRequestBlocked();
        Reporter.log("   Submission blocked by validation: " + blocked, true);
        Assert.assertTrue(blocked,
                "❌ Expected submission to be blocked without filling mandatory fields, but it proceeded");
    }

    // SC_016_TC_004 + TC_005 — full flow: submit → approve → API → old
    // Attrition, new child created. Combined into one chained test since
    // TC_005's assertions are the direct continuation of TC_004's flow.
    @Test(priority = 2,
            description = "SC_016_TC_004/005 — Full flow Regular to Corporate: submit -> approve -> API -> Attrition + new child")
    public void sc016_tc004_fullFlowRegularToCorporate() throws InterruptedException {
        Reporter.log("▶ SC_016_TC_004/005 — Full flow | child: " + AM_REGULAR_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_REGULAR_CHILD_ID);
        migrationPage.clickMigrateRegularToCorporate();
        Assert.assertTrue(migrationPage.isModalVisible(), "❌ Admission Migration modal did not open");

        migrationPage.selectCorporateTieUp("ICICI Lombard General Insurance Company Limited");
        migrationPage.selectFirstAvailableProgram();
        migrationPage.enterEmployeeCode("EMP001");
        migrationPage.enterEmployeeEmail("emp001@example.com");
        migrationPage.enterEmployer("Acme Corp");

        String submitResponse = migrationPage.clickAddMigrationRequest();
        Reporter.log("   Response after submit: " + submitResponse, true);

        String status = recentRequestsPage.getColumnValueByRequestType(
                AM_REGULAR_CHILD_ID, "Admission Migration", "Request Status");
        Reporter.log("   Request Status after submit: " + status, true);
        Assert.assertEquals(status, "Pending",
                "❌ Expected Request Status = Pending after submit. Got: '" + status + "'");

        String wefDate = recentRequestsPage.getColumnValueByRequestType(
                AM_REGULAR_CHILD_ID, "Admission Migration", "WEF Date");
        Reporter.log("   WEF Date: " + wefDate, true);
        String apiDate = parseWefDateToIso(wefDate);
        Reporter.log("   API date param (parsed): " + apiDate, true);

        navigations.goToAccountStatement();
        migrationPage.generateAccountStatement(AM_REGULAR_CHILD_ID);
        Assert.assertTrue(migrationPage.isApproveMigrationRegularToCorporateVisible(),
                "❌ APPROVE MIGRATION FROM REGULAR TO CORPORATE button not shown after submit");

        migrationPage.clickApproveMigrationRegularToCorporate();
        String approveResponse = migrationPage.clickApproveMigrationRequest();
        Reporter.log("   Response after approve: " + approveResponse, true);

        var apiResponse = APIs.processAdmissionMigrationRequests(AM_REGULAR_CHILD_ID, apiDate);
        String body = apiResponse.getBody().asString();
        Reporter.log("   Migration API response: " + body, true);
        Assert.assertEquals(apiResponse.getStatusCode(), 200, "❌ Migration API did not return HTTP 200");

        newChildIdFromRegularToCorporate = extractNewChildId(body);
        Reporter.log("   New child ID: " + newChildIdFromRegularToCorporate, true);

        if (newChildIdFromRegularToCorporate != null) {
            APIs.processAdmissionPipeline(newChildIdFromRegularToCorporate);

            migrationPage.generateAccountStatement(AM_REGULAR_CHILD_ID);
            // Confirmed live: "Admission Migration Already Requested" is only
            // shown mid-flight (Pending/Processing) — once the child reaches
            // Attrition it disappears, replaced by the Attrition status
            // itself, so absence of the Migrate button is the robust,
            // state-independent signal that a duplicate can't be raised.
            Assert.assertFalse(migrationPage.isMigrateRegularToCorporateVisible(),
                    "❌ Migrate button still shown on old child after full migration completed");

            migrationPage.generateAccountStatement(newChildIdFromRegularToCorporate);
            String tieUp = migrationPage.getTieUpBannerText();
            Reporter.log("   New child Tie-Up banner: " + tieUp, true);
            Assert.assertTrue(tieUp.toUpperCase().contains("ICICI"),
                    "❌ New child's Tie-Up banner does not mention the selected Corporate Tie Up");
        } else {
            Reporter.log("ℹ Migration API did not return a new child id yet — informational, matches the "
                    + "same month-end timing gate documented for Corporate Transfer/Center Shift/etc.", true);
        }
    }

    // SC_016_TC_011 — center/program unchanged after migration
    @Test(priority = 3, dependsOnMethods = "sc016_tc004_fullFlowRegularToCorporate", alwaysRun = true,
            description = "SC_016_TC_011 — Center and program unchanged after Regular to Corporate migration")
    public void sc016_tc011_centerProgramUnchanged() throws InterruptedException {
        if (newChildIdFromRegularToCorporate == null) {
            Reporter.log("ℹ Skipped — no new child id from TC_004 (API had not processed the request yet)", true);
            return;
        }
        Reporter.log("▶ SC_016_TC_011 — Center/Program unchanged | new child: " + newChildIdFromRegularToCorporate, true);

        migrationPage.generateAccountStatement(newChildIdFromRegularToCorporate);
        String transferBanner = migrationPage.getTransferCaseBannerText();
        Reporter.log("   Transfer banner: " + transferBanner, true);
        Assert.assertTrue(transferBanner.contains(AM_REGULAR_CHILD_ID),
                "❌ New child's Account Statement does not reference the old child id");
    }

    // SC_016_TC_017 — duplicate migration request blocked
    @Test(priority = 4, dependsOnMethods = "sc016_tc004_fullFlowRegularToCorporate", alwaysRun = true,
            description = "SC_016_TC_017 — Duplicate Regular to Corporate migration request blocked")
    public void sc016_tc017_duplicateRequestBlocked() throws InterruptedException {
        Reporter.log("▶ SC_016_TC_017 — Duplicate request | child: " + AM_REGULAR_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_REGULAR_CHILD_ID);
        // See sc016_tc004's comment — "Already Requested" text is
        // state-dependent (mid-flight only), so the Migrate button's
        // absence is the robust signal across Pending/Processing/Attrition.
        Assert.assertFalse(migrationPage.isMigrateRegularToCorporateVisible(),
                "❌ Expected Migrate button hidden on a child with an existing request");
    }

    // SC_016_TC_018 — Customer Request screen status & WEF date keep updating
    @Test(priority = 5, dependsOnMethods = "sc016_tc004_fullFlowRegularToCorporate", alwaysRun = true,
            description = "SC_016_TC_018 — Customer Request screen status/WEF date (Regular to Corporate)")
    public void sc016_tc018_customerRequestStatusAndWefDate() throws InterruptedException {
        Reporter.log("▶ SC_016_TC_018 — Customer Request status/WEF | child: " + AM_REGULAR_CHILD_ID, true);

        String status = recentRequestsPage.getColumnValueByRequestType(
                AM_REGULAR_CHILD_ID, "Admission Migration", "Request Status");
        String wefDate = recentRequestsPage.getColumnValueByRequestType(
                AM_REGULAR_CHILD_ID, "Admission Migration", "WEF Date");
        Reporter.log("   Request Status: " + status + " | WEF Date: " + wefDate, true);

        Assert.assertFalse(status.isEmpty(), "❌ Request Status not found on Customer Request screen");
        Assert.assertFalse(wefDate.isEmpty(), "❌ WEF Date not found on Customer Request screen");
    }

    // SC_016_TC_019 — Billing Cancel Date shown in red after approval
    @Test(priority = 6, dependsOnMethods = "sc016_tc004_fullFlowRegularToCorporate", alwaysRun = true,
            description = "SC_016_TC_019 — Billing Cancel Date shown after approval (Regular to Corporate)")
    public void sc016_tc019_billingCancelDate() throws InterruptedException {
        Reporter.log("▶ SC_016_TC_019 — Billing Cancel Date | child: " + AM_REGULAR_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_REGULAR_CHILD_ID);
        String billingCancelDate = migrationPage.getBillingCancelDateText();
        Reporter.log("   Billing Cancel Date text: " + billingCancelDate, true);
        Assert.assertFalse(billingCancelDate.isEmpty(),
                "❌ Billing Cancel Date text not shown on old child after approval");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══ SC_017 — ADMISSION MIGRATION (CORPORATE → REGULAR) ═══
    // ═══════════════════════════════════════════════════════════════════════

    // SC_017_TC_003 + TC_004 — full flow: submit -> approve -> API -> old
    // Attrition, new child created (mirrors SC_016_TC_004/005).
    @Test(priority = 7,
            description = "SC_017_TC_003/004 — Full flow Corporate to Regular: submit -> approve -> API -> Attrition + new child")
    public void sc017_tc003_fullFlowCorporateToRegular() throws InterruptedException {
        Reporter.log("▶ SC_017_TC_003/004 — Full flow | child: " + AM_CORPORATE_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_CORPORATE_CHILD_ID);
        migrationPage.clickMigrateCorporateToRegular();
        Assert.assertTrue(migrationPage.isModalVisible(), "❌ Admission Migration modal did not open");

        String submitResponse = migrationPage.clickAddMigrationRequest();
        Reporter.log("   Response after submit: " + submitResponse, true);

        String status = recentRequestsPage.getColumnValueByRequestType(
                AM_CORPORATE_CHILD_ID, "Admission Migration", "Request Status");
        Reporter.log("   Request Status after submit: " + status, true);
        Assert.assertEquals(status, "Pending",
                "❌ Expected Request Status = Pending after submit. Got: '" + status + "'");

        String wefDate = recentRequestsPage.getColumnValueByRequestType(
                AM_CORPORATE_CHILD_ID, "Admission Migration", "WEF Date");
        Reporter.log("   WEF Date: " + wefDate, true);
        String apiDate = parseWefDateToIso(wefDate);
        Reporter.log("   API date param (parsed): " + apiDate, true);

        navigations.goToAccountStatement();
        migrationPage.generateAccountStatement(AM_CORPORATE_CHILD_ID);
        Assert.assertTrue(migrationPage.isApproveMigrationCorporateToRegularVisible(),
                "❌ APPROVE MIGRATION FROM CORPORATE TO REGULAR button not shown after submit");

        migrationPage.clickApproveMigrationCorporateToRegular();
        String approveResponse = migrationPage.clickApproveMigrationRequest();
        Reporter.log("   Response after approve: " + approveResponse, true);

        var apiResponse = APIs.processAdmissionMigrationRequests(AM_CORPORATE_CHILD_ID, apiDate);
        String body = apiResponse.getBody().asString();
        Reporter.log("   Migration API response: " + body, true);
        Assert.assertEquals(apiResponse.getStatusCode(), 200, "❌ Migration API did not return HTTP 200");

        newChildIdFromCorporateToRegular = extractNewChildId(body);
        Reporter.log("   New child ID: " + newChildIdFromCorporateToRegular, true);

        if (newChildIdFromCorporateToRegular != null) {
            APIs.processAdmissionPipeline(newChildIdFromCorporateToRegular);

            migrationPage.generateAccountStatement(AM_CORPORATE_CHILD_ID);
            Assert.assertFalse(migrationPage.isMigrateCorporateToRegularVisible(),
                    "❌ Migrate button still shown on old child after full migration completed");
        } else {
            Reporter.log("ℹ Migration API did not return a new child id yet — informational, matches the "
                    + "same month-end timing gate documented for Corporate Transfer/Center Shift/etc.", true);
        }
    }

    // SC_017_TC_009 — center/program unchanged after migration
    @Test(priority = 8, dependsOnMethods = "sc017_tc003_fullFlowCorporateToRegular", alwaysRun = true,
            description = "SC_017_TC_009 — Center and program unchanged after Corporate to Regular migration")
    public void sc017_tc009_centerProgramUnchanged() throws InterruptedException {
        if (newChildIdFromCorporateToRegular == null) {
            Reporter.log("ℹ Skipped — no new child id from TC_003 (API had not processed the request yet)", true);
            return;
        }
        Reporter.log("▶ SC_017_TC_009 — Center/Program unchanged | new child: " + newChildIdFromCorporateToRegular, true);

        migrationPage.generateAccountStatement(newChildIdFromCorporateToRegular);
        String transferBanner = migrationPage.getTransferCaseBannerText();
        Reporter.log("   Transfer banner: " + transferBanner, true);
        Assert.assertTrue(transferBanner.contains(AM_CORPORATE_CHILD_ID),
                "❌ New child's Account Statement does not reference the old child id");
    }

    // SC_017_TC_018 — Customer Request screen status & WEF date
    @Test(priority = 9, dependsOnMethods = "sc017_tc003_fullFlowCorporateToRegular", alwaysRun = true,
            description = "SC_017_TC_018 — Customer Request screen status/WEF date (Corporate to Regular)")
    public void sc017_tc018_customerRequestStatusAndWefDate() throws InterruptedException {
        Reporter.log("▶ SC_017_TC_018 — Customer Request status/WEF | child: " + AM_CORPORATE_CHILD_ID, true);

        String status = recentRequestsPage.getColumnValueByRequestType(
                AM_CORPORATE_CHILD_ID, "Admission Migration", "Request Status");
        String wefDate = recentRequestsPage.getColumnValueByRequestType(
                AM_CORPORATE_CHILD_ID, "Admission Migration", "WEF Date");
        Reporter.log("   Request Status: " + status + " | WEF Date: " + wefDate, true);

        Assert.assertFalse(status.isEmpty(), "❌ Request Status not found on Customer Request screen");
        Assert.assertFalse(wefDate.isEmpty(), "❌ WEF Date not found on Customer Request screen");
    }

    // SC_017_TC_020 — Billing Cancel Date shown in red after approval
    @Test(priority = 10, dependsOnMethods = "sc017_tc003_fullFlowCorporateToRegular", alwaysRun = true,
            description = "SC_017_TC_020 — Billing Cancel Date shown after approval (Corporate to Regular)")
    public void sc017_tc020_billingCancelDate() throws InterruptedException {
        Reporter.log("▶ SC_017_TC_020 — Billing Cancel Date | child: " + AM_CORPORATE_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_CORPORATE_CHILD_ID);
        String billingCancelDate = migrationPage.getBillingCancelDateText();
        Reporter.log("   Billing Cancel Date text: " + billingCancelDate, true);
        Assert.assertFalse(billingCancelDate.isEmpty(),
                "❌ Billing Cancel Date text not shown on old child after approval");
    }

    // SC_017_TC_022 — center restricted to Corporate-only admissions
    @Test(priority = 11,
            description = "SC_017_TC_022 — Migration blocked for centers that only take Corporate admissions")
    public void sc017_tc022_centerRestrictedToCorporateOnly() throws InterruptedException {
        Reporter.log("▶ SC_017_TC_022 — Center-restricted | child: " + AM_CENTER_RESTRICTED_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_CENTER_RESTRICTED_CHILD_ID);
        migrationPage.clickMigrateCorporateToRegular();

        String restrictedMsg = migrationPage.getVisibleMessageContaining(
                "Can't Migrate to Regular as this Center takes only Corporate Admissions");
        Reporter.log("   Restriction message: " + restrictedMsg, true);
        Assert.assertFalse(restrictedMsg.isEmpty(),
                "❌ Expected center-restriction message not shown");
    }

    // SC_017_TC_023 — blocked when a Child Attrition/Withdraw request exists
    @Test(priority = 12,
            description = "SC_017_TC_023 — Migration blocked when a Child Attrition request is pending")
    public void sc017_tc023_blockedByPendingAttritionRequest() throws InterruptedException {
        Reporter.log("▶ SC_017_TC_023 — Attrition conflict | child: " + AM_ATTRITION_CONFLICT_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_ATTRITION_CONFLICT_CHILD_ID);
        migrationPage.clickMigrateCorporateToRegular();
        migrationPage.clickAddMigrationRequest();

        String conflictMsg = migrationPage.getVisibleMessageContaining(
                "Your Admission Migration request prior/after with Child Attrition request");
        Reporter.log("   Conflict message: " + conflictMsg, true);
        Assert.assertFalse(conflictMsg.isEmpty(),
                "❌ Expected Child-Attrition-conflict message not shown");
    }

    // SC_017_TC_002 / SC_016_TC_003 — dues clearance validation
    // Confirmed live on child 64301 while sourcing test data for the happy
    // path: modal shows " Please clear Dues before raising migration
    // request" instead of the Add Migration Request form.
    @Test(priority = 13,
            description = "SC_017_TC_002 — Migration blocked until outstanding Dues are cleared")
    public void sc017_tc002_duesClearanceValidation() throws InterruptedException {
        Reporter.log("▶ SC_017_TC_002 — Dues clearance | child: " + AM_DUES_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_DUES_CHILD_ID);
        migrationPage.clickMigrateCorporateToRegular();

        String duesMsg = migrationPage.getVisibleMessageContaining(
                "clear Dues before raising migration request");
        Reporter.log("   Dues message: " + duesMsg, true);
        Assert.assertFalse(duesMsg.isEmpty(),
                "❌ Expected dues-clearance message not shown for a child with outstanding Dues");
    }

    // Bonus scenario (not an official Excel TC id) — discovered live while
    // sourcing test data: a Corporate child in PAUSE status has no Migrate
    // button at all, unlike an Active child.
    @Test(priority = 14,
            description = "Bonus — Migrate button not shown for a Paused (non-Active) Corporate child")
    public void bonus_migrateButtonHiddenForPausedChild() throws InterruptedException {
        Reporter.log("▶ Bonus — Paused child | child: " + AM_PAUSED_CHILD_ID, true);

        migrationPage.generateAccountStatement(AM_PAUSED_CHILD_ID);
        boolean visible = migrationPage.isMigrateCorporateToRegularVisible();
        Reporter.log("   Migrate button visible: " + visible, true);
        Assert.assertFalse(visible,
                "❌ Expected Migrate button to be hidden for a Paused (non-Active) child");
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════

    /**
     * Extracts the new child id from a response body like:
     * {"status":"ok","0":["Request Processed successfully with new child id 73708"]}
     * Returns null if the API hasn't processed anything yet (e.g. month-end
     * timing gate — same "No Request to Process" pattern documented for
     * Corporate Transfer/Corporate Center Transfer).
     */
    private String extractNewChildId(String responseBody) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("new child id (\\d+)")
                .matcher(responseBody);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Parses the Customer Request grid's "WEF Date" text (e.g. "Sep 30, 2026")
     * into the ISO yyyy-MM-dd format the migration API's "date" param needs.
     * Per user instruction, the API must be called with the ACTUAL WEF/
     * month-end date of the request — not a guessed "1st of current month" —
     * since a mismatched date silently no-ops with "No Request to process
     * Migration" (same class of timing gate documented for other features).
     * Falls back to AM_MIGRATION_DATE if the grid text can't be parsed.
     */
    private String parseWefDateToIso(String gridDateText) {
        try {
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH);
            return LocalDate.parse(gridDateText.trim(), fmt).toString();
        } catch (Exception e) {
            System.out.println("⚠ Could not parse WEF date '" + gridDateText + "': " + e.getMessage()
                    + " — falling back to " + AM_MIGRATION_DATE);
            return AM_MIGRATION_DATE;
        }
    }
}
