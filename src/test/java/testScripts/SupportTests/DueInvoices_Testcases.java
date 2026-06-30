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
import org.testng.annotations.*;
import pages.Navigations;
import pages.Settings.SMSView_Page;
import pages.Support.AccountStatementPage;
import pages.Support.CustomerPortal_PayDueInvoices;
import utils.APIs;
import utils.BaseTest;
import utils.IAutoConstant;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DueInvoices_Testcases extends BaseTest {

    // Test card credentials (ICICI sandbox)
    private static final String ICICI_CARD_NUMBER = "4111111111111111";
    private static final String ICICI_EXPIRY_MM   = "07";
    private static final String ICICI_EXPIRY_YYYY = "2029";
    private static final String ICICI_CARD_CVV    = "328";
    private static final String ICICI_OTP         = "123456";
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
        saveToJmeterCsv(childId, "UPI",  upiJson);
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
        try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}

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
                            + "firstname,email,phone,productinfo,udf1,udf2,udf3"
                            + System.lineSeparator());
                    System.out.println("✅ JMeter CSV created: "
                            + csvFile.getAbsolutePath());
                }

                fw.write(
                    json.optString("txnid")              + "," +
                    json.optString("txn_id")             + "," +
                    json.optString("payment_mode")       + "," +
                    json.optString("amount")             + "," +
                    json.optString("convenience_charge") + "," +
                    csv(json.optString("firstname"))     + "," +
                    json.optString("email")              + "," +
                    json.optString("phone")              + "," +
                    csv(json.optString("productinfo"))   + "," +
                    json.optString("udf1")               + "," +
                    json.opt("udf2")                     + "," +
                    json.optString("udf3")
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
}
