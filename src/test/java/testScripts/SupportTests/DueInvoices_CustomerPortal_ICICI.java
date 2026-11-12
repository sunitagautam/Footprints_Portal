package testScripts.SupportTests;

import io.restassured.response.Response;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.*;
import pages.Navigations;
import pages.Settings.SystemLogs.SMSView_Page;
import pages.Support.CustomerPortal_PayDueInvoices;
import pages.Support.FinancialReports.AccountStatementPage;
import utils.APIs;
import utils.BaseTest;
import utils.IAutoConstant;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DueInvoices_CustomerPortal_ICICI extends BaseTest {

    // Test card credentials (ICICI sandbox)
    private static final String ICICI_CARD_NUMBER = "4111111111111111";
    private static final String ICICI_EXPIRY_MM = "07";
    private static final String ICICI_EXPIRY_YYYY = "2029";
    private static final String ICICI_CARD_CVV = "328";
    private static final String ICICI_OTP = "123456";
    private static final String ICICI_CARD_HOLDER = "Test User";
    private WebDriverWait wait;
    private AccountStatementPage accountStatementPage;
    private CustomerPortal_PayDueInvoices portalPage;
    private Navigations navigations;
    private SMSView_Page smsViewPage;
    private String mainWindowHandle;

    // ═══════════════════════════════════════════════
    // DATA PROVIDER — unique Child IDs from CSV
    // ═══════════════════════════════════════════════
    @DataProvider(name = "childIds")
    public Object[][] getChildIds() throws IOException {
        List<String> ids = readUniqueChildIds();
        Assert.assertFalse(ids.isEmpty(),
                "❌ No child IDs loaded from: "
                        + IAutoConstant.INVOICE_REPORT_CSV);

        // Filter to a single child when -DchildId=XXXX is passed
        String filter = System.getProperty("childId", "").trim();
        if (!filter.isEmpty()) {
            ids = ids.contains(filter)
                    ? Collections.singletonList(filter)
                    : Collections.emptyList();
            Assert.assertFalse(ids.isEmpty(),
                    "❌ childId=" + filter + " not found in CSV");
            Reporter.log("▶ DataProvider: running for childId=" + filter,
                    true);
        } else {
            Reporter.log("▶ DataProvider: "
                    + ids.size() + " unique child IDs loaded", true);
        }

        Object[][] data = new Object[ids.size()][1];
        for (int i = 0; i < ids.size(); i++) {
            data[i][0] = ids.get(i);
        }
        return data;
    }

    // ═══════════════════════════════════════════════
    // BEFORE CLASS — init page objects
    // Parent BaseTest.openBrowser() runs first so
    // driver is already initialised here.
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws InterruptedException {
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        accountStatementPage = new AccountStatementPage(driver);
        portalPage = new CustomerPortal_PayDueInvoices(driver);
        navigations = new Navigations(driver);
        smsViewPage = new SMSView_Page(driver);
        mainWindowHandle = driver.getWindowHandle();
        System.out.println("✅ Page objects initialized");
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — close stale tabs + navigate
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToAccountStatement() throws InterruptedException {
        closeAllExtraTabsSafely();
        Thread.sleep(500);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD — clean up extra tabs
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void cleanUpAfterTest() throws InterruptedException {
        closeAllExtraTabsSafely();
    }

    // ═══════════════════════════════════════════════
    // TC_001 — Extract UPI / NetBanking payment JSON
    // Reads hidden element id="payment_json_icici_upi"
    // for every child from the invoice report CSV.
    // ═══════════════════════════════════════════════
    @Test(priority = 1,
            dataProvider = "childIds",
            description = "SC_DI_TC_001 — Extract UPI hidden payment JSON for each child")
    public void extractUpiPaymentJson(String childId)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ SC_DI_TC_001 — UPI JSON | Child: " + childId, true);

        // Step 1 — Generate Account Statement for child
        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);
        Reporter.log("✅ Step 1 — Account Statement generated", true);

        // Step 2 — Click Customer Portal (opens billing tab)
        Set<String> tabsBefore = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(tabsBefore);    // Tab 2: billing
        Reporter.log("✅ Step 2 — Customer Portal (billing) opened", true);

        // Step 3 — Check for due invoices
        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            Reporter.log("⚠ Child " + childId
                    + " — No due invoices found, skipping", true);
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            return;
        }

        // Step 4 — Click Pay Due Invoice (opens payment tab)
        Set<String> tabsBefore2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(tabsBefore2);   // Tab 3: payment
        Reporter.log("✅ Step 4 — Payment page opened", true);

        // Step 5 — Extract UPI hidden payment JSON
        String upiJson = portalPage.extractUpiPaymentJson();
        Assert.assertFalse(upiJson.isEmpty(),
                "❌ UPI JSON empty for child: " + childId);
        Reporter.log("✅ Step 5 — UPI JSON extracted", true);

        // Step 5b — Extract CC/DC hidden payment JSON and save both to JMeter CSV
        String ccdcJson = portalPage.extractCcdcPaymentJson();
        saveToJmeterCsv(childId, "UPI", upiJson);
        saveToJmeterCsv(childId, "CARD", ccdcJson);
        Reporter.log("✅ Step 5b — Both JSONs saved to JMeter CSV", true);

        // Step 6 — POST UPI payment event to backend
        Response upiResponse = APIs.postUpiPaymentEvent(upiJson);
        Assert.assertTrue(
                upiResponse.getStatusCode() >= 200
                        && upiResponse.getStatusCode() < 300,
                "❌ UPI POST failed — HTTP " + upiResponse.getStatusCode()
                        + " | " + upiResponse.getBody().asString());
        Reporter.log("✅ Step 6 — UPI POST: HTTP "
                + upiResponse.getStatusCode(), true);

        // Step 7 — Mark child as Paid in CSV
        markChildAsPaid(childId);
        Reporter.log("✅ Step 7 — CSV updated: Child " + childId + " → Paid", true);

        Reporter.log("✅ Child       : " + childId, true);
        Reporter.log("✅ UPI JSON    : " + upiJson, true);
        Reporter.log("✅ API Status  : " + upiResponse.getStatusCode(), true);
        Reporter.log("✅ API Response: " + upiResponse.getBody().asString(), true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ SC_DI_TC_001 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════
    // TC_003 — Capture UPI JSON → JMeter CSV
    // Data-capture only: extracts id="payment_json_icici_upi"
    // and appends one row to jmeter_payment_data.csv.
    // Does NOT call any payment API or mark child as Paid.
    // Use this to build the JMeter dataset without
    // processing real payments.
    // ═══════════════════════════════════════════════
    @Test(priority = 3,
            dataProvider = "childIds",
            description = "SC_DI_TC_003 — Capture UPI JSON and save to JMeter CSV")
    public void captureUpiJsonForJmeter(String childId)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ SC_DI_TC_003 — UPI JSON Capture | Child: " + childId, true);

        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);

        Set<String> t1 = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(t1);

        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            Reporter.log("⚠ Child " + childId + " — No due invoices, skipping", true);
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            return;
        }

        Set<String> t2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(t2);

        String upiJson = portalPage.extractUpiPaymentJson();
        Assert.assertFalse(upiJson.isEmpty(),
                "❌ UPI JSON empty for child: " + childId);

        saveToJmeterCsv(childId, "UPI", upiJson);
        Reporter.log("✅ UPI JSON saved to JMeter CSV — child: " + childId, true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ SC_DI_TC_003 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════
    // TC_004 — Capture CC/DC JSON → JMeter CSV
    // Data-capture only: extracts id="payment_json_icici_ccdc"
    // and appends one row to jmeter_payment_data.csv.
    // Does NOT call any payment API or mark child as Paid.
    // ═══════════════════════════════════════════════
    @Test(priority = 4,
            dataProvider = "childIds",
            description = "SC_DI_TC_004 — Capture CC/DC JSON and save to JMeter CSV")
    public void captureCcdcJsonForJmeter(String childId)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ SC_DI_TC_004 — CC/DC JSON Capture | Child: " + childId, true);

        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);

        Set<String> t1 = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(t1);

        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            Reporter.log("⚠ Child " + childId + " — No due invoices, skipping", true);
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            return;
        }

        Set<String> t2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(t2);

        String ccdcJson = portalPage.extractCcdcPaymentJson();
        Assert.assertFalse(ccdcJson.isEmpty(),
                "❌ CC/DC JSON empty for child: " + childId);

        saveToJmeterCsv(childId, "CARD", ccdcJson);
        Reporter.log("✅ CC/DC JSON saved to JMeter CSV — child: " + childId, true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ SC_DI_TC_004 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════
    // TC_005 — Extract CC/DC JSON → POST to payment API
    //
    // Mirror of TC_001 but for the card hidden JSON:
    //   id="payment_json_icici_ccdc"
    // Posts directly to {{Base_URL}}Eventlistener/iciciPaymentEvents
    // — no ICICI gateway browser flow required.
    // Child is marked Paid in CSV after a successful API response.
    // ═══════════════════════════════════════════════
    @Test(priority = 5,
            dataProvider = "childIds",
            description = "SC_DI_TC_005 — Extract CC/DC JSON and POST to payment event API")
    public void extractCcdcJsonAndPostApi(String childId)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ SC_DI_TC_005 — CC/DC JSON via API | Child: " + childId, true);

        // Step 1 — Generate Account Statement for child
        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);
        Reporter.log("✅ Step 1 — Account Statement generated", true);

        // Step 2 — Click Customer Portal (opens billing tab)
        Set<String> tabsBefore = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(tabsBefore);
        Reporter.log("✅ Step 2 — Customer Portal (billing) opened", true);

        // Step 3 — Check for due invoices
        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            Reporter.log("⚠ Child " + childId + " — No due invoices, skipping", true);
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            return;
        }

        // Step 4 — Click Pay Due Invoice (opens payment tab)
        Set<String> tabsBefore2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(tabsBefore2);
        Reporter.log("✅ Step 4 — Payment page opened", true);

        // Step 5 — Extract CC/DC hidden payment JSON
        String ccdcJson = portalPage.extractCcdcPaymentJson();
        Assert.assertFalse(ccdcJson.isEmpty(),
                "❌ CC/DC JSON empty for child: " + childId);
        Reporter.log("✅ Step 5 — CC/DC JSON extracted", true);

        // Step 6 — POST to Eventlistener/iciciPaymentEvents
        Response apiResponse = APIs.postPaymentEvent(ccdcJson);
        Assert.assertTrue(
                apiResponse.getStatusCode() >= 200
                        && apiResponse.getStatusCode() < 300,
                "❌ CC/DC POST failed — HTTP " + apiResponse.getStatusCode()
                        + " | " + apiResponse.getBody().asString());
        Reporter.log("✅ Step 6 — CC/DC POST: HTTP "
                + apiResponse.getStatusCode(), true);

        // Step 7 — Mark child as Paid in CSV
        markChildAsPaid(childId);
        Reporter.log("✅ Step 7 — CSV updated: Child " + childId + " → Paid", true);

        Reporter.log("✅ Child       : " + childId, true);
        Reporter.log("✅ CC/DC JSON  : " + ccdcJson, true);
        Reporter.log("✅ API Status  : " + apiResponse.getStatusCode(), true);
        Reporter.log("✅ API Response: " + apiResponse.getBody().asString(), true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ SC_DI_TC_005 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════
    // TC_002 — Complete Credit / Debit Card payment
    //
    // NOTE: This test submits a real test-env payment.
    //       Each child's invoice is marked paid after
    //       a successful run — do not repeat for the
    //       same child without resetting invoice data.
    //
    // Phone OTP: pass at runtime with -DsmsOtp=XXXX
    // ═══════════════════════════════════════════════
    @Test(priority = 2,
            dataProvider = "childIds",
            description = "SC_DI_TC_002 — Complete Credit/Debit card payment for each child")
    public void completeCardPaymentFlow(String childId)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ SC_DI_TC_002 — Card Payment | Child: " + childId,
                true);

        // Step 1 — Generate Account Statement for child
        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);
        Reporter.log("✅ Step 1 — Account Statement generated", true);

        // Step 2 — Click Customer Portal (opens billing tab)
        Set<String> tabsBefore = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(tabsBefore);    // Tab 2: billing
        Reporter.log("✅ Step 2 — Customer Portal (billing) opened", true);

        // Step 3 — Check for due invoices
        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            Reporter.log("⚠ Child " + childId
                    + " — No due invoices found, skipping", true);
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            return;
        }

        // Step 4 — Click Pay Due Invoice (opens payment tab)
        Set<String> tabsBefore2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(tabsBefore2);   // Tab 3: payment
        Reporter.log("✅ Step 4 — Payment page opened", true);

        // Step 5 — Select Father radio + OTP (only if OTP section is present)
        String paymentTabHandle = driver.getWindowHandle();
        if (portalPage.isFatherRadioPresent()) {
            // Step 5a — Select Father radio (JS click — opacity:0 element)
            portalPage.clickFatherRadio();
            Reporter.log("✅ Step 5a — Father radio selected", true);

            // Step 5b — Click Phone OTP button to trigger SMS
            portalPage.clickPhoneOtpButton();
            Reporter.log("✅ Step 5b — Phone OTP button clicked", true);

            // Step 6 — Fetch OTP from SMS View, enter and verify
            String smsOtp = fetchOtpFromSmsView(paymentTabHandle);
            Assert.assertFalse(smsOtp.isEmpty(),
                    "❌ Phone OTP not found on SMS View screen");
            portalPage.enterPhoneOtp(smsOtp);
            portalPage.clickVerifyOtp();
            Reporter.log("✅ Step 6 — Phone OTP verified: " + smsOtp, true);
        } else {
            Reporter.log("⚠ Step 5/6 — OTP section not present, skipping", true);
            System.out.println("⚠ No OTP section for child " + childId
                    + " — proceeding directly to card payment");
        }

        // Steps 7-12 — ICICI CC/DD: gateway form → PayPhi OTP → APIs
        performICICI_CCDD_Payment(ICICI_CARD_HOLDER, "Test", "User");
        Reporter.log("✅ Steps 7-12 — ICICI CC/DD payment complete", true);

        // Step 13 — Mark child as Paid in CSV
        markChildAsPaid(childId);
        Reporter.log("✅ Step 13 — CSV updated: Child " + childId + " → Paid", true);

        Reporter.log("✅ Child: " + childId, true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ SC_DI_TC_002 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════════════
    // ICICI CC/DD PAYMENT
    // Clicks Pay via ICICI CC/DD → expands Cards → fills card
    // details → Pay Now → PayPhi simulator OTP → back to
    // Footprints → captures icici_post_data → runs ICICI APIs
    // ═══════════════════════════════════════════════════════
    private void performICICI_CCDD_Payment(String fName,
                                           String firstName, String lastName)
            throws InterruptedException {

        // ── Click Pay via Credit / Debit Card ────────────────
        WebElement ccddBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("btn_icici_ccdc")));
        ccddBtn.click();
        System.out.println("▶ Clicked: Pay via Credit / Debit Card");

        // ── Wait for ICICI hosted page ────────────────────────
        wait.until(ExpectedConditions.urlContains("pgpayuat.icici.bank.in"));
        System.out.println("▶ Redirected to ICICI hosted page — URL: " + driver.getCurrentUrl());
        Thread.sleep(1500);

        // ── Expand Cards section ──────────────────────────────
        WebElement cardsSection = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cardAccordionBtn")));
        cardsSection.click();
        System.out.println("▶ Cards section expanded");
        Thread.sleep(1000);

        // ── Name on Card ──────────────────────────────────────
        WebElement nameField = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("nameOnCard")));
        nameField.sendKeys(fName);
        System.out.println("✅ Name on card: " + fName);

        // ── Card Number ───────────────────────────────────────
        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cardNoMasked"))).sendKeys(ICICI_CARD_NUMBER);
        System.out.println("✅ Card number entered");
        Thread.sleep(1000);

        // ── Expiry Month (select, id="month") ─────────────────
        WebElement monthEl = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("month")));
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0]; el.style.cssText='display:block !important;" +
                        "visibility:visible !important;opacity:1 !important;" +
                        "height:30px !important;width:80px !important;';", monthEl);
        Thread.sleep(300);
        new Select(monthEl).selectByValue(ICICI_EXPIRY_MM);
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0];" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "if(window.jQuery){jQuery(el).trigger('change').trigger('input');}",
                monthEl);
        Object mmVal = ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('month').value;");
        System.out.println("✅ Expiry month set — DOM value: " + mmVal);
        Thread.sleep(500);

        // ── Expiry Year (select, id="year") ───────────────────
        WebElement yearEl = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("year")));
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0]; el.style.cssText='display:block !important;" +
                        "visibility:visible !important;opacity:1 !important;" +
                        "height:30px !important;width:80px !important;';", yearEl);
        Thread.sleep(300);
        // Year options load asynchronously after month selection — wait until populated
        wait.until(d -> new Select(d.findElement(By.id("year"))).getOptions().size() > 1);
        Select yearSelect = new Select(yearEl);
        System.out.println("▶ Year dropdown options: " +
                yearSelect.getOptions().stream()
                        .map(o -> "'" + o.getAttribute("value") + "'")
                        .collect(java.util.stream.Collectors.joining(", ")));
        // Gateway may use 4-digit ("2029") or 2-digit ("29") option values
        try {
            yearSelect.selectByValue(ICICI_EXPIRY_YYYY);
        } catch (Exception e) {
            String twoDigit = ICICI_EXPIRY_YYYY.substring(2);
            System.out.println("⚠ Value '" + ICICI_EXPIRY_YYYY + "' not found, trying '" + twoDigit + "'");
            yearSelect.selectByValue(twoDigit);
        }
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0];" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "if(window.jQuery){jQuery(el).trigger('change').trigger('input');}",
                yearEl);
        Object yyyyVal = ((JavascriptExecutor) driver).executeScript(
                "return document.getElementById('year').value;");
        System.out.println("✅ Expiry year set  — DOM value: " + yyyyVal);
        Thread.sleep(500);

        // ── CVV (input type=password, id="cvv") ───────────────
        wait.until(ExpectedConditions.elementToBeClickable(
                By.id("cvv"))).sendKeys(ICICI_CARD_CVV);
        System.out.println("✅ CVV entered");

        // ── Pay Now (paytBtn2 → submits card form) ─────────────
        WebElement payNow = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("paytBtn2")));
        payNow.click();
        System.out.println("▶ Clicked: Pay Now");

        // ── Wait for PayPhi OTP page ───────────────────────────
        System.out.println("▶ Waiting for PayPhi OTP page...");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("otp")));
        System.out.println("▶ PayPhi OTP page loaded — URL: " + driver.getCurrentUrl());

        // ── Enter OTP ─────────────────────────────────────────
        driver.findElement(By.id("otp")).sendKeys(ICICI_OTP);
        System.out.println("✅ OTP entered: " + ICICI_OTP);

        // ── Click Verify OTP ──────────────────────────────────
        WebElement verifyBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(text(),'Verify OTP')] | //input[@value='Verify OTP']")));
        verifyBtn.click();
        System.out.println("▶ Clicked: Verify OTP");

        // ── Dismiss "Transaction Success" JS alert ────────────
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            System.out.println("▶ Alert: " + alert.getText());
            alert.accept();
            System.out.println("▶ Alert dismissed");
        } catch (Exception e) {
            System.out.println("▶ No alert after Verify OTP");
        }

        // ── Wait for redirect back to Footprints ──────────────
        System.out.println("▶ Waiting for redirect back to Footprints...");
        try {
            wait.until(ExpectedConditions.urlContains("footprintseducation.in"));
            System.out.println("▶ Back on Footprints — URL: " + driver.getCurrentUrl());
        } catch (Exception e) {
            System.out.println("⚠ Redirect wait timed out — current URL: "
                    + driver.getCurrentUrl());
        }
        Thread.sleep(2000);

        // ── Switch back to default content (exit any iframe) ──
        try {
            driver.switchTo().defaultContent();
        } catch (Exception ignored) {
        }

        // ── Extract icici_post_data (data-post attribute) ─────
        String iciciPostData = "";
        try {
            WebElement postEl = wait.until(ExpectedConditions
                    .presenceOfElementLocated(By.cssSelector("[data-post]")));
            iciciPostData = postEl.getAttribute("data-post");
            System.out.println("✅ icici_post_data extracted (length="
                    + (iciciPostData != null ? iciciPostData.length() : 0) + ")");
        } catch (Exception e) {
            System.out.println("❌ icici_post_data not found: " + e.getMessage());
        }
        Assert.assertFalse(iciciPostData.isEmpty(),
                "❌ icici_post_data (data-post) is empty after ICICI CC/DD payment");

        // ── POST card payment event ────────────────────────────
        Response cardResp = APIs.postCardPaymentEvent(iciciPostData);
        System.out.println("✅ Card POST — HTTP " + cardResp.getStatusCode()
                + " | " + cardResp.getBody().asString());
        Assert.assertTrue(
                cardResp.getStatusCode() >= 200 && cardResp.getStatusCode() < 300,
                "❌ Card POST failed — HTTP " + cardResp.getStatusCode()
                        + " | " + cardResp.getBody().asString());

        // ── GET check and process data ─────────────────────────
        Response processResp = APIs.getCheckAndProcessData();
        System.out.println("✅ Check & Process — HTTP " + processResp.getStatusCode()
                + " | " + processResp.getBody().asString());
        Assert.assertTrue(
                processResp.getStatusCode() >= 200 && processResp.getStatusCode() < 300,
                "❌ checkAndProcessData failed — HTTP " + processResp.getStatusCode()
                        + " | " + processResp.getBody().asString());
    }

    // ═══════════════════════════════════════════════
    // HELPER — Save payment JSON row to JMeter CSV
    //
    // Output file : testData/jmeter_payment_data.csv
    // Columns     : txnid, txn_id, payment_mode, amount,
    //               convenience_charge, firstname, email,
    //               phone, productinfo, udf1, udf2, udf3
    //
    // Each field becomes a JMeter variable (${txnid},
    // ${amount}, etc.) for use in HTTP Request bodies.
    // File is created with headers on first write and
    // appended to on subsequent runs.
    // ═══════════════════════════════════════════════
    private void saveToJmeterCsv(String childId,
                                 String paymentMode,
                                 String rawJson) {
        if (rawJson == null || rawJson.isEmpty()) {
            System.out.println("⚠ saveToJmeterCsv — empty JSON for child "
                    + childId + " / " + paymentMode + ", skipping");
            return;
        }

        // Convert single→double quotes to get valid JSON
        String validJson = APIs.convertSingleQuotesToDouble(rawJson);

        try {
            org.json.JSONObject json = new org.json.JSONObject(validJson);

            File csvFile = new File(IAutoConstant.JMETER_PAYMENTS_CSV);
            boolean writeHeader = !csvFile.exists() || csvFile.length() == 0;

            try (FileWriter fw = new FileWriter(csvFile, true)) {
                if (writeHeader) {
                    fw.write("txnid,txn_id,payment_mode,amount,convenience_charge,"
                            + "firstname,email,phone,productinfo,udf1,udf2,udf3,"
                            + "json_payload"
                            + System.lineSeparator());
                    System.out.println("✅ JMeter CSV created: "
                            + csvFile.getAbsolutePath());
                }

                fw.write(
                        json.optString("txnid") + "," +
                                json.optString("txn_id") + "," +
                                json.optString("payment_mode") + "," +
                                json.optString("amount") + "," +
                                json.optString("convenience_charge") + "," +
                                csv(json.optString("firstname")) + "," +
                                json.optString("email") + "," +
                                json.optString("phone") + "," +
                                csv(json.optString("productinfo")) + "," +
                                json.optString("udf1") + "," +
                                json.opt("udf2") + "," +
                                json.optString("udf3") + "," +
                                csv(validJson)                        // full JSON for API-only runs
                                + System.lineSeparator()
                );

                System.out.println("✅ JMeter CSV row saved — child=" + childId
                        + " mode=" + json.optString("payment_mode")
                        + " txn=" + json.optString("txn_id")
                        + " amount=" + json.optString("amount"));
            }
        } catch (Exception e) {
            System.out.println("⚠ saveToJmeterCsv — error for child "
                    + childId + ": " + e.getMessage());
        }
    }

    // Wraps a CSV field in double-quotes if it contains a comma or quote.
    private String csv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ═══════════════════════════════════════════════
    // HELPER — Read Child IDs whose Status = "Payment Due"
    // CSV format: Child #,Status  (two columns, no quotes)
    // ═══════════════════════════════════════════════
    private List<String> readUniqueChildIds() throws IOException {
        List<String> result = new ArrayList<>();
        File csv = new File(IAutoConstant.INVOICE_REPORT_CSV);
        System.out.println("▶ Reading from: " + csv.getAbsolutePath());

        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length < 2) continue;
                String childId = parts[0].trim();
                String status = parts[1].trim();
                if (!childId.isEmpty() && "Payment Due".equalsIgnoreCase(status)) {
                    result.add(childId);
                }
            }
        }
        System.out.println("✅ " + result.size()
                + " children with Payment Due status loaded");
        return result;
    }

    // ═══════════════════════════════════════════════
    // HELPER — Update child's Status to "Paid" in CSV
    // Rewrites the CSV in-place, matching by Child #.
    // ═══════════════════════════════════════════════
    private void markChildAsPaid(String childId) {
        File csv = new File(IAutoConstant.INVOICE_REPORT_CSV);
        List<String> lines = new ArrayList<>();
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2
                        && parts[0].trim().equals(childId)
                        && "Payment Due".equalsIgnoreCase(parts[1].trim())) {
                    lines.add(parts[0].trim() + ",Paid");
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ markChildAsPaid — read error: " + e.getMessage());
            return;
        }

        if (!updated) {
            System.out.println("⚠ markChildAsPaid — child " + childId
                    + " not found or already Paid");
            return;
        }

        try (FileWriter fw = new FileWriter(csv, false)) {
            for (String l : lines) {
                fw.write(l + System.lineSeparator());
            }
            System.out.println("✅ CSV updated: Child " + childId
                    + " → Paid");
        } catch (IOException e) {
            System.out.println("⚠ markChildAsPaid — write error: "
                    + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // HELPER — Fetch phone OTP from Settings → SMS View
    // Switches to main app tab, navigates to sms_view,
    // reads the latest 4-digit OTP, then switches back
    // to the payment tab.
    // ═══════════════════════════════════════════════
    private String fetchOtpFromSmsView(String paymentTabHandle)
            throws InterruptedException {
        // Go to main app tab
        driver.switchTo().window(mainWindowHandle);
        navigations.goToSmsView();

        String otp = smsViewPage.getLatestOtp();
        System.out.println("✅ OTP from SMS View: " + otp);

        // Return to payment tab
        driver.switchTo().window(paymentTabHandle);
        Thread.sleep(500);
        return otp;
    }

    // ═══════════════════════════════════════════════
    // HELPER — Close all extra tabs, return to main
    // ═══════════════════════════════════════════════
    private void closeAllExtraTabsSafely() throws InterruptedException {
        try {
            if (mainWindowHandle == null) return;
            for (String handle :
                    new ArrayList<>(driver.getWindowHandles())) {
                if (!handle.equals(mainWindowHandle)) {
                    driver.switchTo().window(handle);
                    driver.close();
                }
            }
            driver.switchTo().window(mainWindowHandle);
        } catch (Exception e) {
            System.out.println("⚠ Tab cleanup: " + e.getMessage());
        }
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════════════════════
    // ICICI FOUR-BUTTON PAYMENT PAGE (2026-11-12)
    // The payment page now offers UPI / Credit and Debit Card / Net
    // Banking / Amex + International and corporate cards, each with its
    // own hidden JSON (payment_json_icici_up / _cc / _nb / _dc).
    // Child IDs come from Invoices → "Download All Invoice" (Due + Overdue,
    // Active or Attrition). One child per test; a child whose payment page
    // says "Due invoices not found" is skipped. -DiciciChildId=<id> forces
    // a child for a single run.
    // ═══════════════════════════════════════════════════════════════
    private static List<String> iciciChildPool;
    private static final Set<String> iciciUsedChildren = new java.util.LinkedHashSet<>();

    @Test(priority = 10, description = "SC_DI_TC_010 — ICICI UPI: JSON → iciciPaymentEvents → invoices settled")
    public void tc010_iciciUpiJson() throws Exception {
        payViaIciciJson("UPI", "up", "UPI", "SC_DI_TC_010");
    }

    @Test(priority = 11, description = "SC_DI_TC_011 — ICICI Credit and Debit Card: JSON → iciciPaymentEvents → invoices settled")
    public void tc011_iciciCreditDebitCardJson() throws Exception {
        payViaIciciJson("Credit and Debit Card", "cc", "CARD", "SC_DI_TC_011");
    }

    @Test(priority = 12, description = "SC_DI_TC_012 — ICICI Net Banking: JSON → iciciPaymentEvents → invoices settled")
    public void tc012_iciciNetBankingJson() throws Exception {
        payViaIciciJson("Net Banking", "nb", "NB", "SC_DI_TC_012");
    }

    @Test(priority = 13, description = "SC_DI_TC_013 — ICICI Amex/International/Corporate cards: JSON → iciciPaymentEvents → invoices settled")
    public void tc013_iciciAmexJson() throws Exception {
        payViaIciciJson("Amex + International and corporate cards", "dc", "CARD", "SC_DI_TC_013");
    }

    @Test(priority = 14, description = "SC_DI_TC_014 — ICICI Credit and Debit Card through the hosted gateway page")
    public void tc014_iciciCardViaGateway() throws Exception {
        Reporter.log("▶ SC_DI_TC_014 — ICICI card via gateway", true);
        String childId = openPaymentPageForNextIciciChild();
        String paymentTab = driver.getWindowHandle();
        portalPage.selectIciciPaymentMethod("Credit and Debit Card");
        Reporter.log("▶ Child " + childId + " — " + portalPage.getIciciPayButtonText(), true);
        portalPage.clickIciciPayButton();

        wait.until(d -> !d.getCurrentUrl().contains("footprintseducation.in"));
        Reporter.log("▶ Gateway page: " + driver.getCurrentUrl(), true);
        dumpVisibleInputs();
        performIciciHostedCardPayment();

        Assert.assertTrue(portalPage.waitForReturnToFootprints(),
                "❌ Gateway did not return to Footprints — URL: " + driver.getCurrentUrl());
        Reporter.log("▶ Returned to: " + driver.getCurrentUrl() + " | "
                + ((String) ((JavascriptExecutor) driver).executeScript(
                "return document.body.innerText;")).replaceAll("\\s+", " ").trim(), true);
        // The return page says "It will be processed soon": settlement runs through
        // the middleware pipeline (middleware/checkAndProcessData), as in the old TC_002.
        Response process = APIs.getCheckAndProcessData();
        String processBody = process.getBody().asString();
        Reporter.log("▶ checkAndProcessData → HTTP " + process.getStatusCode() + " "
                + processBody.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim(), true);
        Assert.assertFalse(processBody.contains("database error"),
                "❌ checkAndProcessData returned a database error: "
                        + processBody.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim());
        driver.switchTo().window(paymentTab);
        assertDuesSettled(childId);
        System.out.println("✅ SC_DI_TC_014 PASSED — Child: " + childId);
    }

    private void payViaIciciJson(String method, String suffix, String expectedMode, String tc)
            throws Exception {
        Reporter.log("▶ " + tc + " — ICICI " + method, true);
        String childId = openPaymentPageForNextIciciChild();

        portalPage.selectIciciPaymentMethod(method);
        String active = portalPage.getActiveIciciPaymentMethod();
        Reporter.log("▶ Active method: " + active, true);
        Assert.assertTrue(active.startsWith(method), "❌ Method button not active: " + active);
        String payText = portalPage.getIciciPayButtonText();

        String raw = portalPage.extractIciciPaymentJson(suffix);
        Assert.assertFalse(raw.isEmpty(), "❌ payment_json_icici_" + suffix + " empty for child " + childId);
        org.json.JSONObject json = new org.json.JSONObject(APIs.convertSingleQuotesToDouble(raw));
        Reporter.log("▶ JSON: " + json, true);
        Assert.assertEquals(json.optString("payment_mode"), expectedMode, "❌ payment_mode");
        Assert.assertEquals(json.optString("status"), "success", "❌ status");
        String payAmount = payText.replaceAll("[^0-9.]", "");
        Assert.assertEquals(new java.math.BigDecimal(json.optString("amount")),
                new java.math.BigDecimal(payAmount),
                "❌ JSON amount vs '" + payText + "'");

        Response resp = APIs.postPaymentEvent(raw);
        String body = resp.getBody().asString();
        Reporter.log("▶ iciciPaymentEvents → HTTP " + resp.getStatusCode() + " " + body, true);
        Assert.assertEquals(resp.getStatusCode(), 200, "❌ HTTP");
        Assert.assertTrue(body.contains("\"success\"") && body.contains(json.optString("txnid")),
                "❌ Unexpected response: " + body);

        assertDuesSettled(childId);
        System.out.println("✅ " + tc + " PASSED — Child: " + childId + " (" + method + ", " + payText + ")");
    }

    /** Reloads the payment page until it shows "Due invoices not found" (30 s). */
    private void assertDuesSettled(String childId) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            driver.navigate().refresh();
            Thread.sleep(3000);
            if (!portalPage.hasDueInvoicesOnPaymentPage()) {
                Reporter.log("✅ Child " + childId + " — payment page: Due invoices not found", true);
                return;
            }
        }
        Assert.fail("❌ Child " + childId + " still has due invoices after payment");
    }

    /**
     * Opens Account Statement → Customer Portal → Pay Due Invoice for the next
     * unused child of the Invoices download, skipping children with no dues.
     * Leaves the driver on the payment tab.
     */
    private String openPaymentPageForNextIciciChild() throws Exception {
        List<String> candidates = new ArrayList<>();
        String forced = System.getProperty("iciciChildId", "").trim();
        if (!forced.isEmpty() && !iciciUsedChildren.contains(forced)) {
            candidates.add(forced);
        } else {
            if (iciciChildPool == null) iciciChildPool = downloadDueOverdueChildIds();
            for (String c : iciciChildPool) if (!iciciUsedChildren.contains(c)) candidates.add(c);
        }
        int tries = 0;
        for (String childId : candidates) {
            if (++tries > 15) break;
            iciciUsedChildren.add(childId);
            try {
                closeAllExtraTabsSafely();
                navigations.goToAccountStatement();
                accountStatementPage.generateAccountStatement(childId);
                Thread.sleep(1000);
                Set<String> before = driver.getWindowHandles();
                accountStatementPage.clickCustomerPortal();
                portalPage.waitAndSwitchToNewTab(before);
                if (!portalPage.isPayDueInvoiceBtnPresent()) continue;
                Set<String> before2 = driver.getWindowHandles();
                portalPage.clickPayDueInvoice();
                portalPage.waitAndSwitchToNewTab(before2);
                Thread.sleep(2000);
                if (portalPage.hasDueInvoicesOnPaymentPage()) {
                    Reporter.log("✅ Child " + childId + " — payment page open: " + driver.getCurrentUrl(), true);
                    return childId;
                }
                Reporter.log("⚠ Child " + childId + " — no due invoices on payment page, next", true);
            } catch (Exception e) {
                Reporter.log("⚠ Child " + childId + " — could not open payment page: "
                        + e.getMessage().split("\n")[0] + ", next", true);
            }
        }
        throw new SkipException("No child with due invoices found (tried " + (tries - 1) + ")");
    }

    /**
     * Invoices screen → "Download All Invoice" (default filter Due + Overdue,
     * current year) → Invoice_Report_<date>.csv in ~/Downloads → unique Child Ids
     * whose Invoice Status is Payment Due / Payment Overdue.
     */
    private List<String> downloadDueOverdueChildIds() throws Exception {
        String mainTab = driver.getWindowHandle();
        driver.get(IAutoConstant.INVOICES_URL);
        WebElement dl = new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.presenceOfElementLocated(By.id("download_all_invoice_list")));
        String statuses = (String) ((JavascriptExecutor) driver).executeScript(
                "return [...document.getElementById('sr_invoice_status').selectedOptions].map(o=>o.text).join(',');");
        Reporter.log("▶ Invoices filter Invoice Status = " + statuses, true);
        Assert.assertEquals(statuses, "Due,Overdue", "❌ Invoices default status filter changed");

        File dir = new File(System.getProperty("user.home"), "Downloads");
        long t0 = System.currentTimeMillis();
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", dl);
        File report = null;
        for (int i = 0; i < 90 && report == null; i++) {
            Thread.sleep(1000);
            File[] files = dir.listFiles((d, n) -> n.startsWith("Invoice_Report_") && n.endsWith(".csv"));
            if (files != null) for (File f : files) if (f.lastModified() >= t0 - 2000) report = f;
        }
        Assert.assertNotNull(report, "❌ Invoice_Report_*.csv not downloaded to " + dir);
        Thread.sleep(1500);
        driver.switchTo().window(mainTab);

        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        List<String> lines = java.nio.file.Files.readAllLines(report.toPath());
        int childCol = -1, statusCol = -1;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            List<String> c = parseCsvLine(line);
            if (childCol < 0) {
                childCol = c.indexOf("Child Id");
                statusCol = c.indexOf("Invoice Status");
                Assert.assertTrue(childCol >= 0 && statusCol >= 0, "❌ Report header: " + c);
                continue;
            }
            if (c.size() <= Math.max(childCol, statusCol)) continue;
            String st = c.get(statusCol);
            if (("Payment Due".equals(st) || "Payment Overdue".equals(st)) && c.get(childCol).matches("\\d+")) {
                ids.add(c.get(childCol));
            }
        }
        Reporter.log("✅ " + report.getName() + " — " + ids.size() + " children with Due/Overdue invoices", true);
        Assert.assertFalse(ids.isEmpty(), "❌ No Due/Overdue children in " + report.getName());
        return new ArrayList<>(ids);
    }

    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (q) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else if (ch == '"') q = false;
                else sb.append(ch);
            } else if (ch == '"') q = true;
            else if (ch == ',') { out.add(sb.toString().trim()); sb.setLength(0); }
            else sb.append(ch);
        }
        out.add(sb.toString().trim());
        if (!out.isEmpty()) out.set(0, out.get(0).replace("﻿", ""));
        return out;
    }

    private void dumpVisibleInputs() {
        Object r = ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('input,select,button')].filter(e=>e.offsetParent)"
                        + ".map(e=>e.tagName+'#'+e.id+' name='+e.name+' '+(e.innerText||e.placeholder||'').trim().substring(0,40)).join(' | ');");
        Reporter.log("▶ Gateway controls: " + r, true);
    }

    /** ICICI hosted page card steps (same fields as performICICI_CCDD_Payment, without the old #btn_icici_ccdc click). */
    private void performIciciHostedCardPayment() throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        wait.until(ExpectedConditions.elementToBeClickable(By.id("cardAccordionBtn"))).click();
        Thread.sleep(1000);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("nameOnCard"))).sendKeys(ICICI_CARD_HOLDER);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("cardNoMasked"))).sendKeys(ICICI_CARD_NUMBER);
        Thread.sleep(1000);
        for (String[] sel : new String[][]{{"month", ICICI_EXPIRY_MM}, {"year", ICICI_EXPIRY_YYYY}}) {
            WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(By.id(sel[0])));
            js.executeScript("arguments[0].style.cssText='display:block !important;visibility:visible !important;"
                    + "opacity:1 !important;height:30px !important;width:80px !important;';", el);
            wait.until(d -> new Select(d.findElement(By.id(sel[0]))).getOptions().size() > 1);
            Select s = new Select(el);
            try { s.selectByValue(sel[1]); } catch (Exception e) { s.selectByValue(sel[1].substring(2)); }
            js.executeScript("var el=arguments[0];el.dispatchEvent(new Event('change',{bubbles:true}));"
                    + "if(window.jQuery){jQuery(el).trigger('change');}", el);
            Thread.sleep(500);
        }
        wait.until(ExpectedConditions.elementToBeClickable(By.id("cvv"))).sendKeys(ICICI_CARD_CVV);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("paytBtn2"))).click();
        Reporter.log("▶ Pay Now clicked", true);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("otp"))).sendKeys(ICICI_OTP);
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                "//button[contains(text(),'Verify OTP')] | //input[@value='Verify OTP']"))).click();
        Reporter.log("▶ OTP " + ICICI_OTP + " verified", true);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.alertIsPresent());
            Alert a = driver.switchTo().alert();
            Reporter.log("▶ Alert: " + a.getText(), true);
            a.accept();
        } catch (Exception ignored) {
        }
    }
}
