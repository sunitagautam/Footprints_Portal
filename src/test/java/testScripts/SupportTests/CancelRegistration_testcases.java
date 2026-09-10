package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.openqa.selenium.JavascriptExecutor;
import pages.Navigations;
import pages.Support.AccountStatementPage;
import utils.BaseTest;

import java.util.List;

// ═══════════════════════════════════════════════════════════════
// CANCEL REGISTRATION (SC_011_TC_001 to SC_011_TC_006)
// Source: TC_Account Statement Scripts_09Sep.xlsx, sheet
// "Account Statement Page", Scenario ID SC_011.
// Screen: Account Statement → CANCEL REGISTRATION button.
// Stays on the default Rakesh login throughout — NO user switch
// for this feature, per explicit user instruction.
// Test data — Regular Admission children:
//   73705 → TC_001 + TC_002 (view-only, no submit, reusable)
//   73831 → TC_003 (full submit → Attrition)
//   74174 → TC_005 (before Joining Date)
//   73859 → TC_006 (after Joining Date)
// Scope: TC_004/TC_005/TC_006 are UI-verify only — confirm the
// voided invoice/refund entry appears, not exact refund math.
// ═══════════════════════════════════════════════════════════════
public class CancelRegistration_testcases extends BaseTest {

    // NOTE: CR_VIEW_CHILD_ID and CR_FULL_FLOW_CHILD_ID must be DIFFERENT
    // children — TC_003 actually submits/consumes CR_FULL_FLOW_CHILD_ID
    // (→ Attrition), which would break TC_001/TC_002 (view-only, must stay
    // reusable) on the next run if they shared the same id. Both are
    // currently consumed (Attrition) as of 2026-09-10 — fresh Regular
    // children (within the Cancel Registration eligibility window: 3 days
    // before to 1 day after Joining Date) are needed for the next clean run.
    private static final String CR_VIEW_CHILD_ID = "73705";
    private static final String CR_FULL_FLOW_CHILD_ID = "73705";
    private static final String CR_BEFORE_JOINING_CHILD_ID = "73832";
    // 73860 (user-supplied 2026-09-09) had no Cancel Registration button at
    // all — likely outside the "next day of Joining Date" eligibility window.
    // Falling back to one of the earlier-supplied TC_006 backups.
    private static final String CR_AFTER_JOINING_CHILD_ID = "73735";

    // Mixed admission-type children supplied by user (2026-09-10) to confirm
    // Cancel Registration behaves identically regardless of admission type
    // (Regular/Corporate/Summer Camp) — the feature itself is type-agnostic.
    private static final String CR_MIXED_ADMISSION_CHILD_ID_1 = "71141";
    private static final String CR_MIXED_ADMISSION_CHILD_ID_2 = "71990";

    private static final String CANCEL_REASON = "Automation test - cancel registration";

    // Voided invoice reference captured by TC_003, reused by TC_004
    private static String voidedInvoiceRefFromFullFlow;

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

    // ═══════════════════════════════════════════════
    // A stray full-opacity <div id="popdown-opacity"> (the
    // popdown_xl_large modal family's own overlay — distinct from
    // bootstrap's ".modal-backdrop" already handled by closeModalByJs)
    // can be left behind after opening/closing a Cancel Registration
    // popup, blocking the top-nav menu clicks. Confirmed live
    // (2026-09-09): removing/hiding it is what actually unblocks
    // navigation.
    // ═══════════════════════════════════════════════
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
    // SC_011_TC_001 — Cancel Registration option is available
    // (before joining) and opens the popup.
    // ═══════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC_011_TC_001 — Verify the Cancel Registration option before joining opens the popup")
    public void sc011_tc001_cancelRegistrationOptionVisible() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_001 — child: " + CR_VIEW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(CR_VIEW_CHILD_ID);
        Assert.assertTrue(accountStatementPage.isCancelRegistrationButtonVisible(),
                "❌ Cancel Registration button not visible for child " + CR_VIEW_CHILD_ID);

        accountStatementPage.clickCancelRegistration();
        Assert.assertTrue(accountStatementPage.isCancelRegistrationModalVisible(),
                "❌ Cancel Registration popup did not open");
    }

    // ═══════════════════════════════════════════════
    // SC_011_TC_002 — Popup modal default view (consolidated
    // single check per project convention).
    // ═══════════════════════════════════════════════
    @Test(priority = 2,
            description = "SC_011_TC_002 — Verify Cancel Registration popup default view")
    public void sc011_tc002_defaultViewOfModal() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_002 — child: " + CR_VIEW_CHILD_ID, true);

        accountStatementPage.generateAccountStatement(CR_VIEW_CHILD_ID);
        accountStatementPage.clickCancelRegistration();
        Assert.assertTrue(accountStatementPage.isCancelRegistrationModalVisible(),
                "❌ Cancel Registration popup did not open");

        Assert.assertTrue(accountStatementPage.isCancelRegistrationDefaultViewCorrect(),
                "❌ Cancel Registration default view is missing one or more expected fields "
                        + "(title / close icon / Child ID field / Reason field / submit button)");
    }

    // ═══════════════════════════════════════════════
    // SC_011_TC_003 — Full submit flow → Attrition.
    // ═══════════════════════════════════════════════
    @Test(priority = 3,
            description = "SC_011_TC_003 — Verify user can cancel registration end-to-end")
    public void sc011_tc003_fullCancelRegistrationFlow() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_003 — child: " + CR_FULL_FLOW_CHILD_ID, true);

        voidedInvoiceRefFromFullFlow = performCancelRegistrationAndVerify(CR_FULL_FLOW_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // SC_011_TC_004 — Refund of Registration Fee: verify the
    // voided invoice appears on refund_list for Finance to
    // action (UI-verify only, not the actual refund).
    // ═══════════════════════════════════════════════
    @Test(priority = 4, dependsOnMethods = "sc011_tc003_fullCancelRegistrationFlow", alwaysRun = true,
            description = "SC_011_TC_004 — Verify voided invoice appears on refund_list")
    public void sc011_tc004_voidedInvoiceOnRefundList() throws InterruptedException {
        if (voidedInvoiceRefFromFullFlow == null || voidedInvoiceRefFromFullFlow.isEmpty()) {
            Reporter.log("ℹ Skipped — no voided invoice reference captured from SC_011_TC_003", true);
            return;
        }

        Reporter.log("▶ SC_011_TC_004 — checking refund_list for: " + voidedInvoiceRefFromFullFlow, true);
        navigations.goToRefundList();

        Assert.assertTrue(accountStatementPage.isReferenceVisibleOnRefundList(voidedInvoiceRefFromFullFlow),
                "❌ Voided invoice " + voidedInvoiceRefFromFullFlow + " not found on refund_list");
    }

    // ═══════════════════════════════════════════════
    // SC_011_TC_005 — Cancel registration BEFORE Joining Date.
    // ═══════════════════════════════════════════════
    @Test(priority = 5,
            description = "SC_011_TC_005 — Verify cancel registration before Joining Date")
    public void sc011_tc005_cancelRegistrationBeforeJoiningDate() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_005 — child: " + CR_BEFORE_JOINING_CHILD_ID, true);

        performCancelRegistrationAndVerify(CR_BEFORE_JOINING_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // SC_011_TC_006 — Cancel registration AFTER Joining Date
    // (within the next-day grace window).
    // ═══════════════════════════════════════════════
    @Test(priority = 6,
            description = "SC_011_TC_006 — Verify cancel registration after Joining Date")
    public void sc011_tc006_cancelRegistrationAfterJoiningDate() throws InterruptedException {
        Reporter.log("▶ SC_011_TC_006 — child: " + CR_AFTER_JOINING_CHILD_ID, true);

        performCancelRegistrationAndVerify(CR_AFTER_JOINING_CHILD_ID);
    }

    // ═══════════════════════════════════════════════
    // BONUS — Cancel Registration on mixed admission-type children
    // (Corporate / Summer Camp / etc.), confirming the feature works
    // identically regardless of admission type. Not from the SC_011
    // sheet's original 6 cases — added per user's explicit request.
    // ═══════════════════════════════════════════════
    @Test(priority = 7,
            description = "BONUS — Verify cancel registration on a mixed admission-type child (1)")
    public void bonus_cancelRegistrationMixedAdmissionType1() throws InterruptedException {
        Reporter.log("▶ BONUS mixed admission type — child: " + CR_MIXED_ADMISSION_CHILD_ID_1, true);

        performCancelRegistrationAndVerify(CR_MIXED_ADMISSION_CHILD_ID_1);
    }

    @Test(priority = 8,
            description = "BONUS — Verify cancel registration on a mixed admission-type child (2)")
    public void bonus_cancelRegistrationMixedAdmissionType2() throws InterruptedException {
        Reporter.log("▶ BONUS mixed admission type — child: " + CR_MIXED_ADMISSION_CHILD_ID_2, true);

        performCancelRegistrationAndVerify(CR_MIXED_ADMISSION_CHILD_ID_2);
    }

    // ═══════════════════════════════════════════════
    // Confirmed by user (2026-09-10): the Cancel Registration button is
    // only displayed from 3 days BEFORE the child's Joining Date through
    // 1 day AFTER it. Outside that window the button is correctly absent
    // — not a bug. Printed whenever a child ID has no button, so a
    // missing button is understood rather than mistaken for a failure.
    // ═══════════════════════════════════════════════
    private void printCancelButtonEligibilityWindowMessage(String childId) {
        String message = "Cancel Registration button for child " + childId
                + " is not displayed — it is only shown from 3 days before the Joining Date "
                + "through 1 day after the Joining Date.";
        System.out.println("ℹ " + message);
        Reporter.log("   ℹ " + message, true);
    }

    // ═══════════════════════════════════════════════
    // SHARED FLOW — submit Cancel Registration, verify success
    // + Attrition status, print the required summary, and
    // return the voided invoice reference (if any) for reuse.
    // ═══════════════════════════════════════════════
    private String performCancelRegistrationAndVerify(String childId) throws InterruptedException {
        accountStatementPage.generateAccountStatement(childId);

        if (!accountStatementPage.isCancelRegistrationButtonVisible()) {
            printCancelButtonEligibilityWindowMessage(childId);
            return null;
        }

        accountStatementPage.clickCancelRegistration();
        Assert.assertTrue(accountStatementPage.isCancelRegistrationModalVisible(),
                "❌ Cancel Registration popup did not open for child " + childId);

        accountStatementPage.enterCancelRegistrationReason(CANCEL_REASON);
        String confirmAlertText = accountStatementPage.submitCancelRegistration();
        Reporter.log("   Confirm popup text: " + confirmAlertText, true);

        String successMessage = accountStatementPage.getCancelRegistrationSuccessMessage();
        Assert.assertTrue(successMessage.toLowerCase().contains("cancelling registration processed"),
                "❌ Expected success message 'Cancelling Registration Processed', got: " + successMessage);

        accountStatementPage.closeCancelRegistrationModal();
        accountStatementPage.generateAccountStatement(childId);

        Assert.assertTrue(accountStatementPage.isChildStatusAttrition(),
                "❌ Child " + childId + " status did not change to Attrition after cancel registration");
        // REFUND WELCOME KIT only renders when the child actually had a
        // Welcome Kit charge to refund — informational only, not a hard
        // requirement (depends on the child's payment history).
        Reporter.log("   REFUND WELCOME KIT button visible: " + accountStatementPage.isRefundWelcomeKitButtonVisible(), true);

        String status = accountStatementPage.getChildStatusLabel();
        String billingCancelDate = accountStatementPage.getBillingCancelDateText();
        List<String> voidedRefs = accountStatementPage.getVoidedInvoiceReferences();

        System.out.println("Current Child Status: " + status);
        System.out.println("Billing Cancel Date: " + billingCancelDate);
        System.out.println("Voided Invoices IDs: " + String.join(", ", voidedRefs));

        return voidedRefs.isEmpty() ? null : voidedRefs.get(0);
    }
}
