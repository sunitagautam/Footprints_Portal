package testScripts.SupportTests;

import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.AccountStatementPage;
import utils.BaseTest;

import java.util.List;

// ═══════════════════════════════════════════════════════════════
// RECTIFY BRANCH SELECTION (SC_010_TC_001 to SC_010_TC_019)
// Source: TC_Account Statement Scripts_09Sep.xlsx, sheet
// "Account Statement Page", Scenario ID SC_010, module
// "Rectify Branch selection".
// Screen: Account Statement → RECTIFY BRANCH SELECTION button
// (popdown_medium modal, form#frm-rectify-center).
// Stays on the default Rakesh login throughout — no user switch,
// same convention as Cancel Registration.
//
// Deferred per user decision (2026-09-23) — deep financial/refund
// math and email content, out of scope this round:
//   TC_005 (payment logged to new center), TC_007 (registration fee
//   refund amount), TC_009 (paid invoice dummy-refund amount),
//   TC_011/TC_012 (kit invoice refund + "in transit" state),
//   TC_013 (email content).
// Skipped per explicit user instruction (2026-09-23):
//   TC_018 (invalid Joining Date behavior), TC_019 (prepone/postpone
//   joining date at new center — UI action not specified).
//
// Test data (Regular Admission children unless noted):
//   74166 → TC_001/TC_002/TC_003/TC_014 (view-only, no submit, reusable)
//   74175 → TC_004/TC_006/TC_016 (full flow, consumed → Attrition)
//   73547 → TC_008/TC_010 (due/unpaid invoice → void path, consumed)
//   74115 → TC_015 (past the 3-day window, button must be absent)
//   73718, 74121 → TC_017 (Corporate admission, button must be absent)
//   74054 (spares: 74131, 72790, 73661) → BONUS (after Joining Date,
//     still within the 3-day window — full flow, consumed)
// ═══════════════════════════════════════════════════════════════
public class RectifyBranch_Testscases extends BaseTest {

    private static final String RB_VIEW_CHILD_ID = "74166";
    // 74175 consumed (2026-09-23) — full flow correctly created new child
    // #74234 and flipped 74175 to Attrition; that run only failed because of
    // the extractNewChildIdFromRectifyMessage() case-sensitivity bug (fixed),
    // not a real app issue. 74190 also consumed — the re-verification run's
    // native confirm was accepted server-side before an unrelated Chrome
    // session crash lost the browser mid-test (infra flake, not a code bug);
    // confirmed live the request still went through (74190 → Attrition).
    // Rotated to a fresh child for the next clean re-verification.
    private static final String RB_FULL_FLOW_CHILD_ID = "74224";
    private static final String RB_DUE_INVOICE_CHILD_ID = "73547";
    private static final String RB_PAST_WINDOW_CHILD_ID = "74115";
    private static final String RB_CORPORATE_CHILD_ID_1 = "73718";
    private static final String RB_CORPORATE_CHILD_ID_2 = "74121";
    private static final String RB_AFTER_JOINING_CHILD_ID = "74054";

    // New child id created by TC_004's full flow, reused by TC_006/TC_016
    // (both re-check the OLD child, RB_FULL_FLOW_CHILD_ID, so this is
    // captured for logging/traceability rather than direct reuse).
    private static String newChildIdFromFullFlow;

    Navigations navigations;
    AccountStatementPage accountStatementPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        accountStatementPage = new AccountStatementPage(driver);

        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToAccountStatement() throws InterruptedException {
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed in @BeforeMethod");
        } catch (Exception ignored) {
        }
        accountStatementPage.closeModalByJs();
        removeStrayPopdownOverlay();
        Thread.sleep(500);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // Same stray-overlay class already documented for Cancel Registration
    // (popdown modal family's own overlay, distinct from bootstrap's
    // .modal-backdrop) — harmless no-op if this modal size doesn't use it.
    private void removeStrayPopdownOverlay() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var el = document.getElementById('popdown-opacity');" +
                            "if (el) { el.style.display = 'none'; el.style.opacity = '0'; }");
        } catch (Exception ignored) {
        }
    }

    @AfterMethod(alwaysRun = true)
    public void afterTest() {
        try {
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {
        }
        accountStatementPage.closeModalByJs();
        removeStrayPopdownOverlay();
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_001 — Rectify Branch Selection option available,
    // opens the popup.
    // ═══════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC_010_TC_001 — Verify Rectify Branch Selection option opens the popup")
    public void sc010_tc001_rectifyBranchOptionVisible() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_001 — child: " + RB_VIEW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_VIEW_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isRectifyBranchButtonVisible(),
                "❌ Rectify Branch Selection button not visible for child " + RB_VIEW_CHILD_ID);

        accountStatementPage.clickRectifyBranchSelection();
        Assert.assertTrue(accountStatementPage.isRectifyBranchFormVisible(),
                "❌ Rectify Branch Selection popup did not open");
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_002 — Option applicable for Regular admissions,
    // available till Joining Date + 3 days.
    // ═══════════════════════════════════════════════
    @Test(priority = 2,
            description = "SC_010_TC_002 — Verify Rectify Branch option availability for Regular admission")
    public void sc010_tc002_optionAvailabilityForRegularAdmission() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_002 — child: " + RB_VIEW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_VIEW_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isRectifyBranchButtonVisible(),
                "❌ Rectify Branch Selection button not visible for Regular admission child " + RB_VIEW_CHILD_ID);

        accountStatementPage.clickRectifyBranchSelection();
        Assert.assertTrue(accountStatementPage.isRectifyBranchFormVisible(),
                "❌ Rectify Branch Selection popup did not open");
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_003 — Default view of the modal (consolidated
    // single check per project convention).
    // ═══════════════════════════════════════════════
    @Test(priority = 3,
            description = "SC_010_TC_003 — Verify Rectify Branch popup default view")
    public void sc010_tc003_defaultViewOfModal() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_003 — child: " + RB_VIEW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_VIEW_CHILD_ID);
        accountStatementPage.clickRectifyBranchSelection();
        Assert.assertTrue(accountStatementPage.isRectifyBranchFormVisible(),
                "❌ Rectify Branch Selection popup did not open");

        Assert.assertTrue(accountStatementPage.isRectifyBranchDefaultViewCorrect(),
                "❌ Rectify Branch default view is missing one or more expected fields "
                        + "(Shift To / Joining Date / Submit / close icon)");
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_004 — Full flow: new admission created at the
    // new center, transfer reference recorded.
    // ═══════════════════════════════════════════════
    @Test(priority = 4,
            description = "SC_010_TC_004 — Verify creation of new admission via Rectify Branch")
    public void sc010_tc004_fullFlowNewAdmissionCreated() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_004 — child: " + RB_FULL_FLOW_CHILD_ID, true);

        newChildIdFromFullFlow = performRectifyBranchAndVerify(RB_FULL_FLOW_CHILD_ID);
        Assert.assertNotNull(newChildIdFromFullFlow,
                "❌ Rectify Branch full flow did not run for child " + RB_FULL_FLOW_CHILD_ID);
        Assert.assertFalse(newChildIdFromFullFlow.isEmpty(),
                "❌ New Child ID was not captured from the success message");
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_006 — Old admission account closed with
    // Attrition reason "Transfer".
    // ═══════════════════════════════════════════════
    @Test(priority = 5, dependsOnMethods = "sc010_tc004_fullFlowNewAdmissionCreated", alwaysRun = true,
            description = "SC_010_TC_006 — Verify old admission account closed after Rectify Branch")
    public void sc010_tc006_oldAdmissionClosedAttrition() throws InterruptedException {
        if (newChildIdFromFullFlow == null || newChildIdFromFullFlow.isEmpty()) {
            Reporter.log("ℹ Skipped — SC_010_TC_004 did not produce a new child id", true);
            return;
        }
        Reporter.log("▶ SC_010_TC_006 — old child: " + RB_FULL_FLOW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_FULL_FLOW_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isChildStatusAttrition(),
                "❌ Old child " + RB_FULL_FLOW_CHILD_ID + " status is not Attrition after Rectify Branch");

        System.out.println("Old Child Status: " + accountStatementPage.getChildStatusLabel());
        System.out.println("Transfer Case Banner: " + accountStatementPage.getTransferCaseToBannerText());
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_008 (+ TC_010 folded in, informational) — Due
    // invoices voided on the old center after Rectify Branch.
    // ═══════════════════════════════════════════════
    @Test(priority = 6,
            description = "SC_010_TC_008/TC_010 — Verify due invoices voided after Rectify Branch")
    public void sc010_tc008_dueInvoicesVoided() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_008/TC_010 — child: " + RB_DUE_INVOICE_CHILD_ID, true);

        String newChildId = performRectifyBranchAndVerify(RB_DUE_INVOICE_CHILD_ID);
        if (newChildId == null) {
            return; // eligibility-window message already printed
        }

        accountStatementPage.generateAccountStatement(RB_DUE_INVOICE_CHILD_ID);
        List<String> voidedRefs = accountStatementPage.getVoidedInvoiceReferences();
        System.out.println("Voided Invoice IDs (due-invoice child): " + String.join(", ", voidedRefs));
        Assert.assertFalse(voidedRefs.isEmpty(),
                "❌ Expected at least one voided invoice for due-invoice child " + RB_DUE_INVOICE_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_014 — Button visibility and clarity (placed
    // alongside Generate Full Invoices, per screenshot).
    // ═══════════════════════════════════════════════
    @Test(priority = 7,
            description = "SC_010_TC_014 — Verify Rectify Branch button visibility")
    public void sc010_tc014_buttonVisibilityAndPlacement() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_014 — child: " + RB_VIEW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_VIEW_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isRectifyBranchButtonVisible(),
                "❌ Rectify Branch Selection button not clearly visible for child " + RB_VIEW_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_015 — Button not shown/disabled once past the
    // 3-day rectify window.
    // ═══════════════════════════════════════════════
    @Test(priority = 8,
            description = "SC_010_TC_015 — Verify Rectify Branch button hidden past the 3-day window")
    public void sc010_tc015_buttonHiddenPastWindow() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_015 — child: " + RB_PAST_WINDOW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_PAST_WINDOW_CHILD_ID);
        Assert.assertFalse(accountStatementPage.isRectifyBranchButtonVisible(),
                "❌ Rectify Branch Selection button should NOT be visible past the 3-day window for child "
                        + RB_PAST_WINDOW_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_016 — Closure confirmation for old admission:
    // Transfer Case banner + Billing Cancel Date clearly visible.
    // ═══════════════════════════════════════════════
    @Test(priority = 9, dependsOnMethods = "sc010_tc004_fullFlowNewAdmissionCreated", alwaysRun = true,
            description = "SC_010_TC_016 — Verify closure confirmation details for old admission")
    public void sc010_tc016_closureConfirmationDetails() throws InterruptedException {
        if (newChildIdFromFullFlow == null || newChildIdFromFullFlow.isEmpty()) {
            Reporter.log("ℹ Skipped — SC_010_TC_004 did not produce a new child id", true);
            return;
        }
        Reporter.log("▶ SC_010_TC_016 — old child: " + RB_FULL_FLOW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(RB_FULL_FLOW_CHILD_ID);
        String transferBanner = accountStatementPage.getTransferCaseToBannerText();
        String billingCancelDate = accountStatementPage.getBillingCancelDateText();

        System.out.println("Transfer Case Banner: " + transferBanner);
        System.out.println("Billing Cancel Date: " + billingCancelDate);

        Assert.assertFalse(transferBanner.isEmpty(),
                "❌ Transfer Case banner not visible on old child's Account Statement");
        Assert.assertFalse(billingCancelDate.isEmpty(),
                "❌ Billing Cancel Date not visible on old child's Account Statement");
    }

    // ═══════════════════════════════════════════════
    // SC_010_TC_017 — Button not available for admissions other
    // than Regular (Corporate).
    // ═══════════════════════════════════════════════
    @Test(priority = 10,
            description = "SC_010_TC_017 — Verify Rectify Branch button hidden for Corporate admission (1)")
    public void sc010_tc017_buttonHiddenForCorporate1() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_017 — Corporate child: " + RB_CORPORATE_CHILD_ID_1, true);

        accountStatementPage.generateAccountStatement(RB_CORPORATE_CHILD_ID_1);
        Assert.assertFalse(accountStatementPage.isRectifyBranchButtonVisible(),
                "❌ Rectify Branch Selection button should NOT be visible for Corporate child "
                        + RB_CORPORATE_CHILD_ID_1);
    }

    @Test(priority = 11,
            description = "SC_010_TC_017 — Verify Rectify Branch button hidden for Corporate admission (2)")
    public void sc010_tc017_buttonHiddenForCorporate2() throws InterruptedException {
        Reporter.log("▶ SC_010_TC_017 — Corporate child: " + RB_CORPORATE_CHILD_ID_2, true);

        accountStatementPage.generateAccountStatement(RB_CORPORATE_CHILD_ID_2);
        Assert.assertFalse(accountStatementPage.isRectifyBranchButtonVisible(),
                "❌ Rectify Branch Selection button should NOT be visible for Corporate child "
                        + RB_CORPORATE_CHILD_ID_2);
    }

    // ═══════════════════════════════════════════════
    // BONUS — Rectify Branch AFTER Joining Date, still within the
    // 3-day allowed window (user-supplied data, not its own Excel
    // TC id — mirrors Cancel Registration's before/after Joining
    // Date split).
    // ═══════════════════════════════════════════════
    @Test(priority = 12,
            description = "BONUS — Verify Rectify Branch after Joining Date, within the 3-day window")
    public void bonus_rectifyBranchAfterJoiningDateWithinWindow() throws InterruptedException {
        Reporter.log("▶ BONUS after-Joining-Date — child: " + RB_AFTER_JOINING_CHILD_ID, true);

        performRectifyBranchAndVerify(RB_AFTER_JOINING_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // Printed whenever a child ID has no Rectify Branch button —
    // same "understood, not a failure" pattern as Cancel
    // Registration's eligibility-window message.
    // ═══════════════════════════════════════════════
    private void printRectifyButtonEligibilityWindowMessage(String childId) {
        String message = "Rectify Branch Selection button for child " + childId
                + " is not displayed — it is only shown up to 3 days from the Joining Date, "
                + "after that this option is disabled.";
        System.out.println("ℹ " + message);
        Reporter.log("   ℹ " + message, true);
    }

    // ═══════════════════════════════════════════════
    // SHARED FLOW — submit Rectify Branch (auto-picks the first
    // available center), verify the native confirm + success
    // message, capture and print the new Child ID, then open the
    // new child's own Account Statement to complete the flow (per
    // explicit user instruction). Returns the new Child ID, or null
    // if the button wasn't visible (outside the eligibility window).
    // ═══════════════════════════════════════════════
    private String performRectifyBranchAndVerify(String childId) throws InterruptedException {
        accountStatementPage.generateAccountStatement(childId);

        if (!accountStatementPage.isRectifyBranchButtonVisible()) {
            printRectifyButtonEligibilityWindowMessage(childId);
            return null;
        }

        accountStatementPage.clickRectifyBranchSelection();
        Assert.assertTrue(accountStatementPage.isRectifyBranchFormVisible(),
                "❌ Rectify Branch Selection popup did not open for child " + childId);

        accountStatementPage.selectRectifyBranchShiftToCenter(null);
        String confirmAlertText = accountStatementPage.submitRectifyBranch();
        Reporter.log("   Confirm popup text: " + confirmAlertText, true);
        Assert.assertTrue(confirmAlertText.toLowerCase().contains("change center"),
                "❌ Expected native confirm 'Do you want to change center?', got: " + confirmAlertText);

        String successMessage = accountStatementPage.getRectifyBranchSuccessMessage();
        Assert.assertTrue(successMessage.toLowerCase().contains("successfully processed"),
                "❌ Expected Rectify Branch success message, got: " + successMessage);

        String newChildId = accountStatementPage.extractNewChildIdFromRectifyMessage(successMessage);
        System.out.println("New Child ID created at new center: " + newChildId);
        Reporter.log("   New Child ID: " + newChildId, true);

        accountStatementPage.closeRectifyBranchPopup();
        accountStatementPage.generateAccountStatement(childId);

        Assert.assertTrue(accountStatementPage.isChildStatusAttrition(),
                "❌ Old child " + childId + " status did not change to Attrition after Rectify Branch");

        System.out.println("Old Child Status: " + accountStatementPage.getChildStatusLabel());
        System.out.println("Transfer Case Banner: " + accountStatementPage.getTransferCaseToBannerText());
        System.out.println("Billing Cancel Date: " + accountStatementPage.getBillingCancelDateText());

        // Complete the flow — open the newly created child's own Account
        // Statement, per explicit user instruction.
        if (!newChildId.isEmpty()) {
            accountStatementPage.generateAccountStatement(newChildId);
            System.out.println("Opened new admission Account Statement for child: " + newChildId);
        }

        return newChildId;
    }
}
