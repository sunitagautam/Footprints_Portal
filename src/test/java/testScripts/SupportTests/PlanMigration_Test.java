package testScripts.SupportTests;

import io.restassured.response.Response;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pages.Support.FinancialReports.AccountStatementPage;
import pages.Support.RecentCustomerRequestsPage;
import utils.APIs;
import utils.BaseTest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Test Suite: Payment Plan Migration (V1 ↔ V2)
 * <p>
 * V1 = Long Term Payment Plan, V2 = Short Term Payment Plan.
 * <p>
 * Screen: Account Statement → "Migrate in Long Term Payment Plan" (V2→V1) →
 * "Plan Migration Request - V2 to V1" modal → Recent Customer Requests.
 * Stays on the default Rakesh login (no user switch).
 * <p>
 * Source: TC_Payment_Plan_Migration_13Oct.xlsx (SC006) — built from SC_018 +
 * enhancement SDA-13134.
 */
public class PlanMigration_Test extends BaseTest {

    // ═══════════════════════════════════════════════
    // TEST DATA
    // ═══════════════════════════════════════════════
    // V2 child with no existing Plan Migration request. tc001–tc004 are read-only;
    // tc005 raises a real request and CONSUMES the child (49851 consumed 2026-10-13).
    private static final String V2_CHILD_ID = System.getProperty("v2ChildId", "49819"); // spare: 49795

    // Annual recurring date (option value of #next_yearly_billing_date); blank = first option.
    private static final String ANNUAL_RECURRING_DATE = System.getProperty("annualDate", "");

    private static final String BASE = "https://test-franchise.footprintseducation.in";

    private AccountStatementPage accountStatementPage;
    private RecentCustomerRequestsPage recentRequestsPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        accountStatementPage = new AccountStatementPage(driver);
        recentRequestsPage = new RecentCustomerRequestsPage(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        dismissAlertIfPresent();
        accountStatementPage.closeModalByJs();
    }

    private void openAccountStatement(String childId) throws InterruptedException {
        driver.get(BASE + "/account_statement?child_id=" + childId);
        Thread.sleep(4000);
        // Confirmed live: the URL can occasionally show the previously viewed child — verify the header,
        // and fall back to Admission ID + Generate Report.
        if (!isAccountStatementFor(childId)) {
            log("⚠ Account Statement URL showed another child — regenerating for #" + childId);
            accountStatementPage.generateAccountStatement(childId);
            Thread.sleep(4000);
        }
        Assert.assertTrue(isAccountStatementFor(childId), "Account Statement did not load child #" + childId);
    }

    private boolean isAccountStatementFor(String childId) {
        for (org.openqa.selenium.WebElement legend : driver.findElements(By.tagName("legend")))
            if (legend.getText().contains("#" + childId + " ")) return true;
        return false;
    }

    private void assertPopupOpened() {
        if (!accountStatementPage.isPlanMigrationModalVisible()) {
            Assert.fail("Plan migration popup did not open. Message: '"
                    + accountStatementPage.getPlanMigrationPageMessage() + "'");
        }
    }

    private void log(String msg) {
        System.out.println(msg);
        Reporter.log(msg);
    }

    // SC006_TC001 — V2 child sees "Migrate in Long Term Payment Plan", no V1→V2 button.
    @Test(priority = 1)
    public void tc001_v2ChildSeesMigrateToLongTermButton() throws InterruptedException {
        openAccountStatement(V2_CHILD_ID);
        String plan = accountStatementPage.getPaymentPlanLabel();
        log("Payment Plan of #" + V2_CHILD_ID + ": " + plan);
        Assert.assertEquals(plan, "V2", "Child must be on the V2 (Short Term) plan");
        Assert.assertTrue(accountStatementPage.isMigrateToLongTermPlanVisible(),
                "'Migrate in Long Term Payment Plan' button should be shown for a V2 child");
        Assert.assertFalse(accountStatementPage.isMigrateToShortTermPlanVisible(),
                "'Migrate in Short Term Payment Plan' should not be shown for a V2 child");
    }

    // SC006_TC002 — Cancel on the first confirm does nothing.
    @Test(priority = 2)
    public void tc002_cancelFirstConfirmKeepsPlan() throws InterruptedException {
        openAccountStatement(V2_CHILD_ID);
        String confirm = accountStatementPage.clickMigrateToLongTermPlan(false);
        log("Confirm 1: " + confirm);
        Assert.assertEquals(confirm, "Are you sure you want to migrate plan from V2 to V1?");
        Thread.sleep(1500);
        Assert.assertTrue(driver.findElements(By.id("next_yearly_billing_date")).stream().noneMatch(e -> e.isDisplayed()),
                "Popup should not open after Cancel");
        Assert.assertEquals(accountStatementPage.getPaymentPlanLabel(), "V2", "Plan should stay V2");
    }

    // SC006_TC003 — Popup default view (step 1 only loads plan details; nothing is saved).
    @Test(priority = 3)
    public void tc003_popupDefaultView() throws InterruptedException {
        openAccountStatement(V2_CHILD_ID);
        accountStatementPage.clickMigrateToLongTermPlan(true);
        assertPopupOpened();

        String title = accountStatementPage.getPlanMigrationModalTitle();
        String monthly = accountStatementPage.getPlanMigrationMonthlyAmount();
        String yearly = accountStatementPage.getPlanMigrationYearlyAmount();
        String note = accountStatementPage.getPlanMigrationNote();
        List<String> dates = accountStatementPage.getAnnualRecurringDateOptions();
        log("Title   : " + title);
        log("Monthly : " + monthly + " | Annual: " + yearly);
        log("Note    : " + note);
        log("Annual recurring date options: " + dates);

        Assert.assertEquals(title, "Plan Migration Request - V2 to V1");
        Assert.assertTrue(monthly.startsWith("₹"), "Monthly amount should be shown: " + monthly);
        Assert.assertTrue(yearly.startsWith("₹"), "Annual amount should be shown: " + yearly);
        Assert.assertTrue(note.startsWith("Monthly fee of ₹") && note.endsWith("will be charged from the next month."),
                "Unexpected note: " + note);
        Assert.assertEquals(dates.size(), 12, "Expected the 1st of the next 12 months");
        String firstExpected = LocalDate.now().withDayOfMonth(1).plusMonths(1).toString();
        Assert.assertEquals(dates.get(0), firstExpected, "First option should be the 1st of next month");
    }

    // SC006_TC004 — Annual recurring date is mandatory (client-side check, nothing saved).
    @Test(priority = 4)
    public void tc004_annualRecurringDateRequired() throws InterruptedException {
        openAccountStatement(V2_CHILD_ID);
        accountStatementPage.clickMigrateToLongTermPlan(true);
        assertPopupOpened();
        String msg = accountStatementPage.confirmPlanMigrationWithoutDate();
        log("Validation: " + msg);
        Assert.assertTrue(msg.contains("Please select annual recurring date"), "Unexpected validation: " + msg);
    }

    // SC006_TC005 — Full V2→V1 flow: apply → open Customer Requests → run month-end cron.
    // CONSUMES V2_CHILD_ID.
    @Test(priority = 5)
    public void tc005_v2ToV1MigrationFullFlow() throws InterruptedException {
        openAccountStatement(V2_CHILD_ID);
        double monthlyBefore = monthlyPlanAmount(driver.findElement(By.tagName("body")).getText());
        log("Monthly plan before: " + monthlyBefore);
        accountStatementPage.clickMigrateToLongTermPlan(true);
        assertPopupOpened();

        log("Note: " + accountStatementPage.getPlanMigrationNote());
        // Plan Amount on Account Statement is the undiscounted fee (the popup shows it after discounts).
        String expectedMonthly = String.format("%.2f", monthlyBefore - 2000);
        String annualDate = ANNUAL_RECURRING_DATE.isEmpty()
                ? accountStatementPage.getAnnualRecurringDateOptions().get(0) : ANNUAL_RECURRING_DATE;
        accountStatementPage.selectAnnualRecurringDate(annualDate);
        log("Annual recurring date: " + annualDate);

        String[] result = accountStatementPage.confirmPlanMigration();
        log("Confirm 2: " + result[0]);
        log("Result   : " + result[1]);
        Assert.assertEquals(result[0],
                "Are you sure you want to migrate plan from V2 to V1? The new plan will be effective from next month");
        Thread.sleep(5000); // page reloads ~3 s after save

        // Customer Requests — Pending, WEF = last day of the (server's) month
        String status = recentRequestsPage.getColumnValueByRequestType(V2_CHILD_ID, "Plan Migration", "Request Status");
        String wef = recentRequestsPage.getColumnValueByRequestType(V2_CHILD_ID, "Plan Migration", "WEF Date");
        log("Customer Request → Plan Migration | Status: " + status + " | WEF Date: " + wef);
        Assert.assertEquals(status, "Pending", "Plan Migration request should be Pending");
        LocalDate wefDate = LocalDate.parse(wef, GRID_DATE);
        // Expected (user, 2026-10-13): WEF = day before the selected annual recurring date (1 Dec → 30 Nov);
        // the cron processes the request on that day.
        Assert.assertEquals(wefDate, LocalDate.parse(annualDate).minusDays(1),
                "WEF Date should be the day before the selected annual recurring date");

        // Month-end cron — only allowed on the last day of the month, 11:00–11:59 PM (server time).
        Response cron = APIs.processPlanMigrationRequests(V2_CHILD_ID, wefDate.toString());
        String body = cron.getBody().asString();
        Assert.assertEquals(cron.getStatusCode(), 200);
        if (body.contains("allowed only on the last day of the month")) {
            log("ℹ Cron not processed (server time outside last day 11:00–11:59 PM) — informational: " + body);
            return;
        }
        Assert.assertTrue(body.contains("\"status\":\"ok\""), "Cron failed: " + body);
        verifyProcessedV2ToV1Migration(V2_CHILD_ID, annualDate, expectedMonthly);
    }

    // SC006_TC005 (verification part) — read-only check of a child whose V2→V1 request was already
    // processed by the month-end cron. Defaults = child 49851 (processed 2026-10-31, annual date
    // 2026-11-01, new monthly 14499.00).
    @Test(priority = 6)
    public void tc006_verifyProcessedV2ToV1Migration() throws InterruptedException {
        verifyProcessedV2ToV1Migration(
                System.getProperty("processedChildId", "49851"),
                System.getProperty("processedAnnualDate", "2026-11-01"),
                System.getProperty("processedMonthlyAmount", "14499.00"));
    }

    // SC002 — V2→V1 renewal offer email (sent by saveMigrationPromotionalData ~1 month before the
    // 1-year anniversary of the Joining Date). Read-only: run after the offer API for -DofferChildId.
    @Test(priority = 7)
    public void tc007_v2OfferEmailSent() throws InterruptedException {
        String childId = System.getProperty("offerChildId", "62654");
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        openAccountStatement(childId);
        offerMonthlyBefore = monthlyPlanAmount(driver.findElement(By.tagName("body")).getText());
        offerChildName = (String) js.executeScript(
                "var a=[...document.querySelectorAll(\"legend a[href*='pop_child_info']\")].find(x=>new RegExp('child_id='+arguments[0]+'(\\\\D|$)').test(x.getAttribute('href')));return a?a.textContent.trim().replace(/\\s+/g,' '):'';", childId); // raw text, not CSS-uppercased
        log("Account Statement: " + offerChildName + " | monthly " + offerMonthlyBefore);

        // Email View → "New Payment Plan: Annual Fee Applies" for the child
        String body = openEmailBySubjectAndBody("New Payment Plan: Annual Fee Applies", childId);
        log("Offer email:\n" + body);
        Assert.assertFalse(body.isEmpty(), "Offer email 'New Payment Plan: Annual Fee Applies' should exist for " + childId);
        verifyV2OfferEmailContent(body, childId);
        // 3 blue links: "Clicking Here" (confirm), "Click Here" (remind me later), "Click Here" (not interested)
        @SuppressWarnings("unchecked")
        List<String> links = (List<String>) js.executeScript(
                "return [...document.querySelectorAll('a')].filter(x=>/plan_migration\\.php\\?q=/.test(x.href))"
                        + ".map(x=>x.textContent.trim()+' => '+x.href+' | color: '+getComputedStyle(x).color);");
        for (String l : links) log("Offer link: " + l);
        Assert.assertEquals(links.size(), 3, "Email should have 3 plan_migration links (confirm / remind later / not interested)");
        Assert.assertTrue(links.get(0).startsWith("Clicking Here"), "1st link should be 'Clicking Here' (confirm)");
        Assert.assertTrue(links.get(1).startsWith("Click Here") && links.get(2).startsWith("Click Here"),
                "2nd/3rd links should be 'Click Here' (remind later / not interested)");
        String link = links.get(0).split(" => ")[1].split(" \\| ")[0];

        // Payment Plan Migration screen (filtered to the renewal month) → row for the child, status Informed
        java.util.regex.Matcher rm = java.util.regex.Pattern.compile("next renewal on (\\d+)\\w{2} (\\w+), (\\d{4})").matcher(body);
        Assert.assertTrue(rm.find(), "Renewal date not found in the email");
        String month = rm.group(2).substring(0, 3) + " - " + rm.group(3);   // e.g. "Nov - 2026"
        log("Renewal: " + rm.group(0) + " → Migration Month filter '" + month + "'");
        String row = getPaymentPlanMigrationRow(childId, month);
        log("Payment Plan Migration row: " + row);
        Assert.assertTrue(row.startsWith(childId), "Child should be listed on Payment Plan Migration: " + row);
        Assert.assertTrue(row.contains("Informed"), "Migration Status should be Informed: " + row);
    }

    // SC003 — Parent answers the V2→V1 offer from the email link: Confirm / Remind Me Later / Not Interested.
    // -DofferChildId=<id> -DparentAction=confirm|reminder|denied. CHANGES the child's migration request.
    @Test(priority = 8)
    public void tc008_parentRespondsToOffer() throws Exception {
        String childId = System.getProperty("offerChildId", "62654");
        String action = System.getProperty("parentAction", "confirm");
        String buttonId = Map.of("confirm", "btn_confirm", "reminder", "btn_reminder", "denied", "btn_denied").get(action);
        String expectedStatus = Map.of("confirm", "Confirmed", "reminder", "Reminder", "denied", "Denied").get(action);
        Assert.assertNotNull(buttonId, "parentAction must be confirm, reminder or denied");
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        org.openqa.selenium.support.ui.WebDriverWait wait =
                new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(20));

        // Link + renewal month from the offer email
        // -DofferSubject="Reminder - New Payment Plan: Annual Fee Applies" to answer from the reminder email
        String subject = System.getProperty("offerSubject", "New Payment Plan: Annual Fee Applies");
        String body = openEmailBySubjectAndBody(subject, childId);
        Assert.assertFalse(body.isEmpty(), "Email '" + subject + "' not found for " + childId);
        String link = (String) js.executeScript(
                "var a=[...document.querySelectorAll('a')].find(x=>/plan_migration\\.php\\?q=/.test(x.href));return a?a.href:'';");
        java.util.regex.Matcher rm = java.util.regex.Pattern.compile("(?:next renewal on|will be raised on) \\d+\\w{2} (\\w+), (\\d{4})").matcher(body);
        Assert.assertTrue(rm.find(), "Renewal date not found in the email");   // V2 offer: "next renewal on"; V1 offer: "will be raised on"
        String month = rm.group(1).substring(0, 3) + " - " + rm.group(2);
        log("Offer link: " + link + " | Migration Month: " + month);

        // Parent page → button → popup message → Continue
        driver.get(link);
        // Cloudflare Rocket Loader defers the page scripts: clicks are ignored until __cfRLUnblockHandlers is set.
        wait.until(d -> Boolean.TRUE.equals(((org.openqa.selenium.JavascriptExecutor) d).executeScript(
                "return document.readyState==='complete' && !!window.__cfRLUnblockHandlers && !!window.jQuery;")));
        org.openqa.selenium.WebElement next = null;
        for (int attempt = 1; attempt <= 3 && next == null; attempt++) {
            wait.until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(By.id(buttonId))).click();
            try {
                next = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(8))
                        .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(By.id("btn_get_submit")));
            } catch (org.openqa.selenium.TimeoutException e) {
                log("Popup not shown after click " + attempt + "; retrying");
            }
        }
        Assert.assertNotNull(next, "Popup did not open after clicking " + buttonId);
        Thread.sleep(800);
        log("Popup message (" + action + "): " + visibleModalText());
        evidence("tc008_" + childId + "_" + action + "_1_popup");
        next.click();

        if (action.equals("reminder")) {
            // Remind Me Later needs no OTP — the page shows a success alert straight away.
            String msg = wait.until(d -> {
                String m = pageMessages();
                return m.contains("reminder request registered") ? m : null;
            });
            log("Result (reminder): " + msg);
            evidence("tc008_" + childId + "_reminder_3_result");
            Assert.assertTrue(msg.contains("Thanks, your reminder request registered successfully"), msg);
        } else {
            submitParentOtp(childId, action, wait, js);
        }

        // Payment Plan Migration row → status
        String row = getPaymentPlanMigrationRow(childId, month);
        log("Payment Plan Migration row: " + row);
        Assert.assertTrue(row.contains(expectedStatus), "Migration Status should be " + expectedStatus + ": " + row);
    }

    private void submitParentOtp(String childId, String action, org.openqa.selenium.support.ui.WebDriverWait wait,
                                 org.openqa.selenium.JavascriptExecutor js) throws Exception {
        // Contact for OTP → Father → OTP 1234 → Continue
        wait.until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(normalize-space(.),'Please select contact details for OTP')]")));
        org.openqa.selenium.WebElement father = driver.findElement(By.id("customRadioInline2"));
        js.executeScript("arguments[0].click();", father);
        org.openqa.selenium.WebElement otp = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(By.id("otp")));
        Thread.sleep(1500);
        log("After selecting Father: " + visibleModalText());
        otp.clear();
        otp.sendKeys(System.getProperty("parentOtp", "1234"));
        evidence("tc008_" + childId + "_" + action + "_2_otp");
        org.openqa.selenium.WebElement submit = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(By.id("btn_otp_submit")));
        submit.click();
        Thread.sleep(4000);
        String result = visibleModalText() + " || PAGE: " + pageMessages();
        log("Result after OTP (" + action + "): " + result);
        evidence("tc008_" + childId + "_" + action + "_3_result");
        String expected = action.equals("confirm")
                ? "Thanks, your new payment plan will be applied from next month."
                : "Thanks, your annual fee will be invoiced on";
        Assert.assertTrue(result.contains(expected), "Unexpected result: " + result);
    }

    private String visibleModalText() {
        Object t = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('.modal')].filter(m=>getComputedStyle(m).display!=='none')"
                        + ".map(m=>m.innerText.replace(/\\s+/g,' ').trim()).join(' || ');");
        return t == null ? "" : t.toString();
    }

    private String pageMessages() {
        Object t = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('.alert,.toast,.flash-msg,.swal2-popup,.jq-toast-single,.text-success,.text-danger')]"
                        + ".filter(e=>e.offsetParent!==null&&e.innerText.trim()).map(e=>e.innerText.replace(/\\s+/g,' ').trim()).join(' | ');");
        return t == null ? "" : t.toString();
    }

    private void evidence(String name) throws Exception {
        java.nio.file.Path dir = java.nio.file.Paths.get("screenshots", "plan_migration");
        java.nio.file.Files.createDirectories(dir);
        java.io.File f = ((org.openqa.selenium.TakesScreenshot) driver).getScreenshotAs(org.openqa.selenium.OutputType.FILE);
        java.nio.file.Files.copy(f.toPath(), dir.resolve(name + ".png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /** Payment Plan Migration grid filtered to {@code month} (e.g. "Nov - 2026") and searched for the child. */
    private String getPaymentPlanMigrationRow(String childId, String month) throws InterruptedException {
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        driver.get(BASE + "/payment_plan_migration");
        Thread.sleep(5000);
        new org.openqa.selenium.support.ui.Select(driver.findElement(By.id("sr_migration_month"))).selectByVisibleText(month);
        driver.findElement(By.id("btn_submit")).click();
        Thread.sleep(5000);
        js.executeScript("$('#dt_migration_list').DataTable().search(arguments[0]).draw();", childId);
        Thread.sleep(1500);
        String all = (String) js.executeScript("return [...document.querySelectorAll('#dt_migration_list tbody tr')]"
                + ".map(r=>r.innerText.replace(/\\s+/g,' ')).join(' ||| ');");
        if (all.contains(" ||| ")) log("⚠ More than one Payment Plan Migration row for " + childId + ": " + all);
        return all;
    }

    // SC002 — Reminder email after the parent chose "Remind Me Later" (sendMigrationPromotionalReminderEmail).
    // Read-only: run after the reminder API for -DreminderChildId.
    @Test(priority = 9)
    public void tc009_v2ReminderEmailSent() throws InterruptedException {
        String childId = System.getProperty("reminderChildId", "62664");
        openAccountStatement(childId);
        offerMonthlyBefore = monthlyPlanAmount(driver.findElement(By.tagName("body")).getText());
        offerChildName = (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var a=[...document.querySelectorAll(\"legend a[href*='pop_child_info']\")].find(x=>new RegExp('child_id='+arguments[0]+'(\\\\D|$)').test(x.getAttribute('href')));return a?a.textContent.trim().replace(/\\s+/g,' '):'';", childId);

        // All reminder emails, newest first — shows whether other children were emailed in the same run
        driver.get(utils.IAutoConstant.EMAIL_VIEW_URL);
        Thread.sleep(4000);
        driver.findElement(By.id("subject")).sendKeys("Reminder - New Payment Plan");
        driver.findElement(By.id("btn_submit")).click();
        Thread.sleep(5000);
        sortEmailViewByCreatedDateDesc();
        List<org.openqa.selenium.WebElement> rows = driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr"));
        for (int i = 0; i < Math.min(6, rows.size()); i++) log("Latest reminder emails: " + rows.get(i).getText().replaceAll("\\s+", " "));

        String body = openEmailBySubjectAndBody("Reminder - New Payment Plan: Annual Fee Applies", childId);
        log("Reminder email:\n" + body);
        Assert.assertFalse(body.isEmpty(), "Reminder email should exist for " + childId);
        verifyV2OfferEmailContent(body, childId, true);
    }

    // SC004 — V2→V1 via parent email, after the month-end cron. Read-only.
    // -DmigratedChildId (62654) -DrenewalDate (2026-11-01) -DmigratedMonthlyAmount (8299.00) -DprocessedDate (2026-10-31)
    @Test(priority = 10)
    public void tc010_verifyParentEmailMigrationProcessed() throws InterruptedException {
        String childId = System.getProperty("migratedChildId", "62654");
        LocalDate renewal = LocalDate.parse(System.getProperty("renewalDate", "2026-11-01"));
        String monthly = System.getProperty("migratedMonthlyAmount", "8299.00");
        LocalDate processed = LocalDate.parse(System.getProperty("processedDate", "2026-10-31"));
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();

        String status = recentRequestsPage.getColumnValueByRequestType(childId, "Plan Migration", "Request Status");
        log("Customer Request → Plan Migration: " + status);
        soft.assertEquals(status, "Approved", "Plan Migration request should be Approved");

        openAccountStatement(childId);
        String page = driver.findElement(By.tagName("body")).getText();
        String yearly = section(page, "Yearly Subscription", "Billing Cancel Date|Addons|Reference|Dues");
        String monthlySec = section(page, "Monthly Subscription", "Yearly Subscription");
        log("Payment Plan: " + accountStatementPage.getPaymentPlanLabel() + "\nMonthly:\n" + monthlySec + "\nYearly:\n" + yearly);
        soft.assertEquals(accountStatementPage.getPaymentPlanLabel(), "V1", "Header should show [Payment Plan : V1]");
        soft.assertTrue(monthlySec.contains("Plan Amount : " + monthly + " (Monthly)"), "Monthly plan: " + monthlySec);
        soft.assertTrue(yearly.contains("Plan Amount : 16000.00 (Yearly)"), "Yearly amount: " + yearly);
        String nextYear = renewal.plusYears(1).format(DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH));
        soft.assertTrue(yearly.contains("Next Billing Date : " + nextYear),
                "Yearly Next Billing Date should be " + nextYear + " (this year's annual fee is invoiced at migration): " + yearly);
        String invoiceDay = processed.format(DateTimeFormatter.ofPattern("dd MMMM, yyyy", Locale.ENGLISH));
        log("Annual invoice lines:" + lines(page, "Against V2 to V1 Migration") + lines(page, "Invoice charged on " + invoiceDay));
        soft.assertTrue(page.contains("Annual Fee Invoice Amount Against V2 to V1 Migration"), "Annual fee invoice line missing");
        soft.assertTrue(page.contains("Invoice charged on " + invoiceDay), "Annual invoice should be charged on " + invoiceDay);

        accountStatementPage.clickChildHistory();
        String history = accountStatementPage.waitForChildHistoryText("migrated via parent email confirmation", 15);
        log("Child History (Plan Migration):" + lines(history, "Auto:"));
        soft.assertTrue(history.contains("the New Payment Plan (V1) was migrated via parent email confirmation"),
                "History should record the migration via parent email confirmation");
        soft.assertTrue(history.contains("queued for month-end processing"), "History should record the parent's confirmation");
        accountStatementPage.closeModalByJs();

        String childName = offerChildNameFor(childId);
        String email = openEmailBySubjectAndBody("Footprints: Payment plan updated for", childId);
        log("Parent email:\n" + email);
        soft.assertFalse(email.isEmpty(), "Parent email 'Footprints: Payment plan updated for " + childName + "' should exist");
        soft.assertTrue(email.contains("d_" + childId + "_father@gmail.com"), "Email should go to the parents");
        soft.assertTrue(email.contains("migrated to new payment plan"), "Email should confirm the migration");
        soft.assertTrue(email.contains("Annual Fee: Rs. 16,000.00"), "Email should state the annual fee");
        soft.assertAll();
    }

    // SC003 — Parent chose Not Interested: after the month-end cron the child is unchanged. Read-only.
    // -DdeniedChildId (62671) -DdeniedMonthlyAmount (12499.00) -DdeniedMonth ("Nov - 2026")
    @Test(priority = 11)
    public void tc011_verifyDeniedChildUnchanged() throws InterruptedException {
        String childId = System.getProperty("deniedChildId", "62671");
        String monthly = System.getProperty("deniedMonthlyAmount", "12499.00");
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();

        String row = getPaymentPlanMigrationRow(childId, System.getProperty("deniedMonth", "Nov - 2026"));
        log("Payment Plan Migration row: " + row);
        soft.assertTrue(row.contains("Denied"), "Migration Status should stay Denied: " + row);

        openAccountStatement(childId);
        String page = driver.findElement(By.tagName("body")).getText();
        String yearly = section(page, "Yearly Subscription", "Billing Cancel Date|Addons|Reference|Dues");
        String monthlySec = section(page, "Monthly Subscription", "Yearly Subscription");
        log("Payment Plan: " + accountStatementPage.getPaymentPlanLabel() + "\nMonthly:\n" + monthlySec + "\nYearly:\n" + yearly);
        String plan = System.getProperty("deniedPlan", "V2");   // V1 for a declined V1→V2 offer
        soft.assertEquals(accountStatementPage.getPaymentPlanLabel(), plan, "Child should stay on " + plan);
        soft.assertTrue(monthlySec.contains("Plan Amount : " + monthly + " (Monthly)"), "Monthly plan should be unchanged: " + monthlySec);
        if (plan.equals("V2")) {
            soft.assertTrue(yearly.contains("Plan Amount : Not Available"), "No yearly plan should be created: " + yearly);
            soft.assertFalse(page.contains("Against V2 to V1 Migration"), "No migration annual invoice should be raised");
        } else {
            soft.assertTrue(yearly.contains("Plan Amount : 16000.00 (Yearly)"), "Yearly plan should be kept: " + yearly);
        }
        soft.assertAll();
    }

    private String offerChildNameFor(String childId) throws InterruptedException {
        openAccountStatement(childId);
        return (String) ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var a=[...document.querySelectorAll(\"legend a[href*='pop_child_info']\")].find(x=>new RegExp('child_id='+arguments[0]+'(\\\\D|$)').test(x.getAttribute('href')));return a?a.textContent.trim().replace(/\\s+/g,' '):'';", childId);
    }

    // ═══════════════════════════════════════════════
    // V1 → V2 (Long Term → Short Term) via parent email — "New Payment Plan: Zero Annual Fee"
    // ═══════════════════════════════════════════════

    // SC002 — V1→V2 offer email + Informed row. Read-only: run after the offer API for -DofferChildId.
    @Test(priority = 12)
    public void tc012_v1OfferEmailSent() throws InterruptedException {
        String childId = System.getProperty("offerChildId", "34643");
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
        openAccountStatement(childId);
        String page = driver.findElement(By.tagName("body")).getText();
        double monthly = monthlyPlanAmount(page);
        String name = offerChildNameFor(childId);
        log("Account Statement: " + name + " | " + accountStatementPage.getPaymentPlanLabel() + " | monthly " + monthly);

        String body = openEmailBySubjectAndBody("New Payment Plan: Zero Annual Fee", childId);
        log("Offer email:\n" + body);
        Assert.assertFalse(body.isEmpty(), "Offer email 'New Payment Plan: Zero Annual Fee' should exist for " + childId);
        @SuppressWarnings("unchecked")
        List<String> links = (List<String>) js.executeScript(
                "return [...document.querySelectorAll('a')].filter(x=>/plan_migration\\.php\\?q=/.test(x.href))"
                        + ".map(x=>x.textContent.trim()+' => '+x.href);");
        for (String l : links) log("Offer link: " + l);
        verifyV1OfferEmailContent(body, childId, name, monthly, false);
        Assert.assertEquals(links.size(), 3, "Email should have 3 plan_migration links (confirm / remind later / not interested)");

        java.util.regex.Matcher rm = java.util.regex.Pattern.compile("raised on \\d+\\w{2} (\\w+), (\\d{4})").matcher(body);
        Assert.assertTrue(rm.find(), "Renewal date not found in the email");
        String row = getPaymentPlanMigrationRow(childId, rm.group(1).substring(0, 3) + " - " + rm.group(2));
        log("Payment Plan Migration row: " + row);
        Assert.assertTrue(row.contains("Informed"), "Migration Status should be Informed: " + row);
    }

    /** Content check of the V1→V2 offer ("New Payment Plan: Zero Annual Fee"); {@code reminder} = the "Reminder - …" email. */
    private void verifyV1OfferEmailContent(String body, String childId, String name, double monthly, boolean reminder) {
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();
        java.util.regex.Matcher rm = java.util.regex.Pattern.compile("raised on (\\d+\\w{2} (\\w+), \\d{4})").matcher(body);
        Assert.assertTrue(rm.find(), "Renewal date not found in the email");
        String renewal = rm.group(1), renewalMonth = rm.group(2);
        LocalDate renewalDate = LocalDate.parse(renewal.replaceAll("(\\d+)\\w{2} ", "$1 "),
                DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH));
        java.text.DecimalFormat inr = new java.text.DecimalFormat("#,##0.00");
        String[] expected = {
                "To: d_" + childId + "_father@gmail.com, d_" + childId + "_mother@gmail.com CC:",
                "Dear Parent,",
                "This is to inform you that your annual invoice will be raised on " + renewal + " for your ward " + name + " (#" + childId + ")",
                "Existing Payment Plan\nAnnual Fee: Rs. 16,000.00 Due on " + renewal + "\nMonthly Fee: Rs. " + inr.format(monthly),
                "Optional New Payment Plan\nAnnual Fee: None\nMonthly Fee: Rs. " + inr.format(monthly + 2000),
                "So, instead of paying Rs. 16,000.00 in " + renewalMonth + ", you could just pay additional Rs. 2,000.00 per month.",
                "please confirm by Clicking Here before " + ordinal(renewalDate.minusDays(2))
                        + ". This new fee will be applicable for all payable on or after " + renewal + ".",
                // AC3
                "Please note that once your annual fee for the current cycle has been charged, it is non-refundable - this new plan will apply from your next renewal, not the current one.",
                "Click Here in case you do not want to consider this option or receive any emails for it.",
                "Please note that the above plan cannot be opted after your annual invoice have been raised.",
        };
        String normalized = body.replaceAll("[ \\t]+", " ").replaceAll(" ?\\n ?", "\n");
        for (String line : expected) {
            boolean ok = normalized.contains(line);
            log((ok ? "  ✔ " : "  ✘ ") + line.replace("\n", " | "));
            soft.assertTrue(ok, "Email line missing: " + line.replace("\n", " | "));
        }
        if (reminder) soft.assertFalse(normalized.contains("remind"), "Reminder email should not offer 'remind me later' again");
        soft.assertAll();
    }

    // SC002 — V1→V2 reminder email ("Reminder - New Payment Plan: Zero Annual Fee"). Read-only, after the reminder API.
    @Test(priority = 15)
    public void tc015_v1ReminderEmailSent() throws InterruptedException {
        String childId = System.getProperty("reminderChildId", "39951");
        openAccountStatement(childId);
        double monthly = monthlyPlanAmount(driver.findElement(By.tagName("body")).getText());
        String name = offerChildNameFor(childId);
        String body = openEmailBySubjectAndBody("Reminder - New Payment Plan: Zero Annual Fee", childId);
        log("Reminder email:\n" + body);
        Assert.assertFalse(body.isEmpty(), "Reminder email should exist for " + childId);
        verifyV1OfferEmailContent(body, childId, name, monthly, true);
    }

    // SC004 — V1→V2 via parent email, after the month-end cron. Read-only.
    // -DmigratedChildId -DmonthlyBefore (V1 monthly before migration) -DprocessedDate (2026-10-31)
    @Test(priority = 13)
    public void tc013_verifyV1ToV2MigrationProcessed() throws InterruptedException {
        String childId = System.getProperty("migratedChildId", "34643");
        double before = Double.parseDouble(System.getProperty("monthlyBefore", "11099"));
        String expectedMonthly = String.format("%.2f", before + 2000);
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();

        String status = recentRequestsPage.getColumnValueByRequestType(childId, "Plan Migration", "Request Status");
        log("Customer Request → Plan Migration: " + status);
        soft.assertEquals(status, "Approved", "Plan Migration request should be Approved");

        openAccountStatement(childId);
        String page = driver.findElement(By.tagName("body")).getText();
        String monthlySec = section(page, "Monthly Subscription", "Yearly Subscription");
        String yearly = section(page, "Yearly Subscription", "Billing Cancel Date|Addons|Reference|Dues");
        log("Payment Plan: " + accountStatementPage.getPaymentPlanLabel() + "\nMonthly:\n" + monthlySec + "\nYearly:\n" + yearly);
        log("Migration-related ledger lines:" + lines(page, "Migration") + lines(page, "Void"));
        soft.assertEquals(accountStatementPage.getPaymentPlanLabel(), "V2", "Header should show [Payment Plan : V2]");
        soft.assertTrue(monthlySec.contains("Plan Amount : " + expectedMonthly + " (Monthly)"), "Monthly plan should be +2000: " + monthlySec);
        soft.assertTrue(yearly.contains("Plan Amount : Not Available"), "Yearly plan should be removed: " + yearly);

        accountStatementPage.clickChildHistory();
        String history = accountStatementPage.waitForChildHistoryText("Plan Migration", 15);
        log("Child History (Plan Migration):" + lines(history, "Auto:") + lines(history, "Migration:"));
        soft.assertTrue(history.contains("Plan Migration"), "Child History should record the plan migration");
        accountStatementPage.closeModalByJs();

        String name = offerChildNameFor(childId);
        // Subject filtered without the name (names can contain double spaces); Body = child ID picks the child.
        String email = openEmailBySubjectAndBody("Footprints: Payment plan upgraded for", childId);
        log("Parent email:\n" + email);
        soft.assertFalse(email.isEmpty(), "Parent email 'Footprints: Payment plan upgraded for " + name + "' should exist");
        soft.assertTrue(email.contains("Annual Fee: None"), "Email should state 'Annual Fee: None'");
        soft.assertAll();
    }

    // SC002 / AC1 — a first-year V1 child must NOT get the V1→V2 offer. Calls the offer API for -DfirstYearChildId.
    @Test(priority = 14)
    public void tc014_firstYearV1ChildNotOffered() throws InterruptedException {
        String childId = System.getProperty("firstYearChildId", "67748");
        Response r = APIs.sendPlanMigrationPromotionalEmail(childId);
        String body = r.getBody().asString();
        log("Offer API for first-year child " + childId + ": " + body);
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();
        soft.assertFalse(body.contains("sent successfully to 1 Child"), "First-year child should not be sent the offer: " + body);
        for (String m : new String[]{"Nov - 2026", "Dec - 2026"}) {
            String row = getPaymentPlanMigrationRow(childId, m);
            log("PPM " + m + ": " + row);
            soft.assertTrue(row.contains("No matching records"), "First-year child should have no Payment Plan Migration row in " + m);
        }
        String email = openEmailBySubjectAndBody("New Payment Plan: Zero Annual Fee", childId);
        soft.assertTrue(email.isEmpty(), "First-year child should get no 'Zero Annual Fee' offer email");
        soft.assertAll();
    }

    private double offerMonthlyBefore;
    private String offerChildName;

    /** Full content check of the V2→V1 offer email "New Payment Plan: Annual Fee Applies". */
    private void verifyV2OfferEmailContent(String body, String childId) {
        verifyV2OfferEmailContent(body, childId, false);
    }

    /** {@code reminder} = the "Reminder - …" email, which drops the "remind me later" option. */
    private void verifyV2OfferEmailContent(String body, String childId, boolean reminder) {
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();
        java.util.regex.Matcher rm = java.util.regex.Pattern.compile("next renewal on (\\d+\\w{2} \\w+, \\d{4})").matcher(body);
        Assert.assertTrue(rm.find(), "Renewal date not found in the email");
        String renewal = rm.group(1);                                   // "1st November, 2026"
        LocalDate renewalDate = LocalDate.parse(renewal.replaceAll("(\\d+)\\w{2} ", "$1 "),
                DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH));
        String confirmBy = ordinal(renewalDate.minusDays(2));           // "30th October, 2026"
        java.text.DecimalFormat inr = new java.text.DecimalFormat("#,##0.00");
        String existing = inr.format(offerMonthlyBefore), proposed = inr.format(offerMonthlyBefore - 2000);
        String name = offerChildName == null ? "" : offerChildName;

        String[] expected = {
                "To: d_" + childId + "_father@gmail.com, d_" + childId + "_mother@gmail.com CC:",
                "Dear Parent,",
                "Greetings of the day from Footprints! Your Parenting Partner!",
                "This is to inform you that your ward " + name + " (#" + childId + ")'s current payment plan does not carry an annual fee. "
                        + "From your next renewal on " + renewal + ", Footprints is offering the option to switch to our standard plan, "
                        + "which carries a lower monthly fee alongside an annual fee.",
                "You can also opt for this payment plan, below are the details for your reference and perusal.",
                "Existing Payment Plan\nAnnual Fee: None\nMonthly Fee: Rs. " + existing,
                "Optional New Payment Plan\nAnnual Fee: Rs. 16,000.00\nMonthly Fee: Rs. " + proposed,
                "If you switch, you would pay Rs. 2,000.00 less per month, and an annual fee of Rs. 16,000.00 would apply from "
                        + renewal + " onward. This is just an option for you - you may choose to continue on your current plan.",
                "If you want to change your payment plan, please confirm by Clicking Here before " + confirmBy
                        + ". This new fee will be applicable for all payable on or after " + renewal + ".",
                reminder
                        ? ".\nClick Here in case you do not want to consider this option or receive any emails for it."
                        : "Click Here if you want to think about it and want us to reminder you later. "
                        + "Or Click Here in case you do not want to consider this option or receive any emails for it.",
                "Please note that the above plan cannot be opted after your annual invoice have been raised.",
                "For any queries, please talk to your Centre Directress",
                "Thanks & Warm Regards,\nFootprints Childcare\nYour Parenting Partner!",
        };
        String normalized = body.replaceAll("[ \\t]+", " ").replaceAll(" ?\\n ?", "\n");
        if (reminder) soft.assertFalse(normalized.contains("remind"),
                "Reminder email should not offer 'remind me later' again");
        for (String line : expected) {
            boolean ok = normalized.contains(line);
            log((ok ? "  ✔ " : "  ✘ ") + line.replace("\n", " | "));
            soft.assertTrue(ok, "Email line missing: " + line.replace("\n", " | "));
        }
        soft.assertAll();
    }

    private static String ordinal(LocalDate d) {
        int n = d.getDayOfMonth();
        String suf = (n % 100 >= 11 && n % 100 <= 13) ? "th" : new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"}[n % 10];
        return n + suf + " " + d.format(DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.ENGLISH));
    }

    /** Clicks the "Created Date" header until it is sorted descending (newest email first). */
    private void sortEmailViewByCreatedDateDesc() throws InterruptedException {
        By header = By.xpath("//table[@id='DataTables_Table_0']//th[normalize-space()='Created Date']");
        for (int i = 0; i < 2; i++) {
            if ("descending".equals(driver.findElement(header).getAttribute("aria-sort"))) break;
            driver.findElement(header).click();
            Thread.sleep(1500);
        }
        log("Email View sorted by Created Date: " + driver.findElement(header).getAttribute("aria-sort"));
    }

    /** Email View: filter by Subject + Body, open the first row whose subject starts with {@code subject}. */
    private String openEmailBySubjectAndBody(String subject, String bodyFilter) throws InterruptedException {
        driver.get(utils.IAutoConstant.EMAIL_VIEW_URL);
        Thread.sleep(4000);
        driver.findElement(By.id("subject")).sendKeys(subject);
        driver.findElement(By.id("body")).sendKeys(bodyFilter);
        driver.findElement(By.id("btn_submit")).click();
        Thread.sleep(5000);
        sortEmailViewByCreatedDateDesc();
        for (org.openqa.selenium.WebElement r : driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr"))) {
            String t = r.getText();
            log("Email row: " + t.replaceAll("\\s+", " "));
            if (!t.contains(subject) || (!subject.startsWith("Reminder - ") && t.contains("Reminder - "))) continue;
            driver.get(r.findElement(By.cssSelector("a[href*='pop_email_view_data_modal']")).getAttribute("href"));
            Thread.sleep(2500);
            return driver.findElement(By.tagName("body")).getText();
        }
        return "";
    }

    // ═══════════════════════════════════════════════
    // Checks run after every processed V2→V1 migration
    // ═══════════════════════════════════════════════
    private void verifyProcessedV2ToV1Migration(String childId, String annualDateIso, String expectedMonthly)
            throws InterruptedException {
        org.testng.asserts.SoftAssert soft = new org.testng.asserts.SoftAssert();
        // 1. Customer Request → Approved
        String status = recentRequestsPage.getColumnValueByRequestType(childId, "Plan Migration", "Request Status");
        String wef = recentRequestsPage.getColumnValueByRequestType(childId, "Plan Migration", "WEF Date");
        log("Customer Request → Plan Migration | Status: " + status + " | WEF: " + wef);
        soft.assertEquals(status, "Approved", "Plan Migration request should be Approved after the cron");
        if (wef.isEmpty()) {   // e.g. parent-email path without a Customer Request row → use the cron date
            wef = LocalDate.parse(System.getProperty("processedDate", "2026-10-31")).format(GRID_DATE);
            log("No Plan Migration Customer Request row — using processedDate " + wef);
        }

        // 2. Account Statement → [Payment Plan : V1], monthly / yearly subscriptions
        openAccountStatement(childId);
        String page = driver.findElement(By.tagName("body")).getText();
        String plan = accountStatementPage.getPaymentPlanLabel();
        String monthly = section(page, "Monthly Subscription", "Yearly Subscription");
        String yearly = section(page, "Yearly Subscription", "Billing Cancel Date|Addons|Reference|Dues");
        log("Payment Plan: " + plan);
        log("Monthly:\n" + monthly);
        log("Yearly:\n" + yearly);
        soft.assertEquals(plan, "V1", "Header should show [Payment Plan : V1]");
        soft.assertTrue(monthly.contains("Plan Amount : " + expectedMonthly + " (Monthly)"), "Monthly plan: " + monthly);
        soft.assertTrue(yearly.contains("Plan Amount : 16000.00 (Yearly)"), "Yearly plan amount: " + yearly);
        String expectedAnnual = LocalDate.parse(annualDateIso).format(DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH));
        soft.assertTrue(yearly.contains("Next Billing Date : " + expectedAnnual),
                "Yearly Next Billing Date should be " + expectedAnnual + ": " + yearly);
        soft.assertFalse(accountStatementPage.isMigrateToLongTermPlanVisible(),
                "'Migrate in Long Term Payment Plan' should be gone after migration");
        // By design (user, 2026-10-14): the Account Statement path raises NO annual invoice at processing —
        // the annual fee is billed on the yearly Next Billing Date. (The parent-email path raises it immediately.)
        soft.assertFalse(page.contains("Annual Fee Invoice Amount Against V2 to V1 Migration"),
                "Account Statement path should not raise the migration annual invoice immediately");

        // 3. Child History → "Plan Migration Completed"
        accountStatementPage.clickChildHistory();
        String history = accountStatementPage.waitForChildHistoryText("Plan Migration Completed", 15);
        log("Child History (Plan Migration lines):" + lines(history, "Plan Migration"));
        soft.assertTrue(history.contains("Plan Migration Completed"),
                "Child History should contain 'Plan Migration Completed'");
        soft.assertTrue(history.contains("Migration: V2 -> V1"), "History should record 'Migration: V2 -> V1'");
        soft.assertTrue(history.contains("Annual Recurring Date: " + annualDateIso),
                "Request entry should record 'Annual Recurring Date: " + annualDateIso + "'");
        String effective = LocalDate.parse(wef, GRID_DATE).plusDays(1).toString();
        soft.assertTrue(history.contains("Effective Date: " + effective),
                "Completed entry should record 'Effective Date: " + effective + "'");
        accountStatementPage.closeChildHistoryModal();

        // 4. Email View (Rakesh) → "Plan Migration Report V2 -> V1 - All Centers - <dd Mon yyyy>" lists the child
        String reportSubject = "Plan Migration Report V2 -> V1 - All Centers - "
                + LocalDate.parse(wef, GRID_DATE).format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        String report = openEmailBySubjectAndBody(reportSubject, childId); // several reports can share a date
        log("Report email '" + reportSubject + "':\n" + report);
        soft.assertFalse(report.isEmpty(), "Email '" + reportSubject + "' should be in Email View");
        soft.assertTrue(report.contains(childId), "Report email should list child " + childId);

        // 5. By design (user, 2026-10-14): the Account Statement path sends NO parent email
        //    (the parent-email path sends "Footprints: Payment plan updated for <child>").
        String parentEmail = openEmailBySubjectAndBody("Footprints: Payment plan updated for", childId);
        log("Parent 'Payment plan updated' email: " + (parentEmail.isEmpty() ? "(none — expected)" : "FOUND"));
        soft.assertTrue(parentEmail.isEmpty(), "Account Statement path should not send the parent 'Payment plan updated' email");
        soft.assertAll();
    }

    private static final DateTimeFormatter GRID_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    /** Filters Email View by Subject, opens the first row whose subject matches exactly, returns its body ("" if none). */
    private String openEmailBySubject(String subject) throws InterruptedException {
        driver.get(utils.IAutoConstant.EMAIL_VIEW_URL);
        Thread.sleep(4000);
        driver.findElement(By.id("subject")).sendKeys(subject);
        driver.findElement(By.id("btn_submit")).click();
        Thread.sleep(5000);
        for (org.openqa.selenium.WebElement row : driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr"))) {
            if (!row.getText().contains(subject)) continue;
            driver.get(row.findElement(By.cssSelector("a[href*='pop_email_view_data_modal']")).getAttribute("href"));
            Thread.sleep(2500);
            return driver.findElement(By.tagName("body")).getText();
        }
        return "";
    }

    private static double monthlyPlanAmount(String page) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Plan Amount\\s*:\\s*₹?\\s*([\\d,.]+)\\s*\\(Monthly\\)").matcher(page);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : 0;
    }

    private static String section(String page, String start, String endRegex) {
        int i = page.indexOf(start);
        if (i < 0) return "";
        String rest = page.substring(i);
        String[] parts = rest.split("(?m)^(?=(" + endRegex + "))", 2);
        return parts[0].length() > 800 ? parts[0].substring(0, 800) : parts[0];
    }

    private static String lines(String page, String keyword) {
        StringBuilder sb = new StringBuilder();
        for (String l : page.split("\n")) if (l.contains(keyword)) sb.append("\n  ").append(l.trim());
        return sb.toString();
    }
}
