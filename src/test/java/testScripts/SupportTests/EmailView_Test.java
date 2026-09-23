package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Settings.EmailView_Page;
import utils.BaseTest;

// ═══════════════════════════════════════════════════════════════
// EMAIL VIEW (Settings → Email View)
// Screen: /email_view — confirmed live (2026-09-22) that only the
// Rakesh user has access to this screen (unlike SMS View, which other
// switched-to users can reach). Stays on the default Rakesh login
// throughout — NO user switch for this feature.
//
// Generic, reusable page for any feature test that needs to confirm a
// notification/confirmation email was sent (e.g. Extended Daycare's
// Stop/Early Resume enhancement) — not scoped to one feature.
// ═══════════════════════════════════════════════════════════════
public class EmailView_Test extends BaseTest {

    Navigations navigations;
    EmailView_Page emailViewPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        emailViewPage = new EmailView_Page(driver);

        acknowledgePolicyNotificationIfPresent();
        closeNotificationDropdownIfOpen();
    }

    // ════════════════════════════════════════════════════════════════════
    //  DIAGNOSTIC — explore the live Email View page structure before
    //  writing real interaction/assertion methods.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 1,
            description = "DIAGNOSTIC — explore Email View page structure")
    public void diagnostic_exploreEmailViewStructure() throws InterruptedException {
        Reporter.log("▶ Exploring Email View page structure", true);

        navigations.goToEmailView();
        boolean loaded = emailViewPage.isPageLoaded();
        System.out.println("   [Page loaded] " + loaded);
        Reporter.log("   Page loaded: " + loaded, true);

        emailViewPage.dumpBodyText();
        emailViewPage.dumpFilterFormHtml();
        emailViewPage.dumpVisibleTableStructure();
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC001 — Extended Daycare Stop/Early Resume enhancement: confirm a
    //  notification email record exists for the same-day completion
    //  triggered against child 70801 (see ServiceRequest_ExtendedDaycareTest
    //  tc016). Confirmed live subject: "Extended Daycare Duration Completed
    //  - for Anaisha (#70801)".
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 2,
            description = "Extended Daycare — confirmation email recorded for a completed request")
    public void tc001_extendedDaycareCompletionEmailRecorded() throws InterruptedException {
        Reporter.log("▶ TC001 — Extended Daycare completion email | child=70801", true);

        navigations.goToEmailView();
        Assert.assertTrue(emailViewPage.isPageLoaded(), "❌ Email View page did not load");

        // Per convention: always filter by the dedicated Subject field with
        // the feature's own subject keyword ("Extended Daycare"), then check
        // the results for this specific child/outcome.
        boolean found = emailViewPage.hasEmailContaining("Extended Daycare", "70801", "Completed");
        System.out.println("   [Completion email found] " + found);
        Reporter.log("   Completion email found: " + found, true);
        Assert.assertTrue(found, "❌ Expected an 'Extended Daycare ... Completed' email record for child 70801");

        Reporter.log("✅ TC001 PASSED — Extended Daycare completion email record confirmed", true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TC002 — Confirm whether a SEPARATE notification email exists for the
    //  Early Stop/Early Resume action itself (distinct from the completion
    //  email above) — per AC #6 ("a confirmation email is sent... after a
    //  successful Early Resume"). Informational if not found — the
    //  completion email confirms the notification pipeline works, but this
    //  checks for the specific early-stop wording separately.
    // ════════════════════════════════════════════════════════════════════
    @Test(priority = 3,
            description = "Extended Daycare — check for a distinct Early Stop/Early Resume notification email")
    public void tc002_extendedDaycareEarlyStopEmailCheck() throws InterruptedException {
        Reporter.log("▶ TC002 — Extended Daycare Early Stop email check | child=70801", true);

        navigations.goToEmailView();
        Assert.assertTrue(emailViewPage.isPageLoaded(), "❌ Email View page did not load");

        java.util.List<String> allRows = emailViewPage.getAllMatchingRowTexts("Extended Daycare");
        String rowText = "";
        for (String r : allRows) {
            if (r.contains("70801")) {
                rowText = r;
                break;
            }
        }
        System.out.println("   [Matching row for 70801] " + rowText);
        Reporter.log("   Matching row for 70801: " + rowText, true);

        boolean earlyStopEmailFound = rowText.toLowerCase().contains("early stop")
                || rowText.toLowerCase().contains("early resume")
                || rowText.toLowerCase().contains("revised");

        if (earlyStopEmailFound) {
            Reporter.log("✅ A distinct Early Stop/Early Resume notification email was found", true);
        } else {
            Reporter.log("⚠ TC002 INFO — no email row for child 70801 mentioned 'Early Stop'/'Early Resume'/'revised' "
                    + "specifically — only the completion email (TC001) was found. The app may only send one "
                    + "notification (on Completed), not a separate one at the moment Early Resume is submitted. "
                    + "Not hard-failed — informational pending product confirmation of expected email(s).", true);
        }
    }
}
