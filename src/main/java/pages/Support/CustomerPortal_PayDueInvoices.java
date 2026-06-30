package pages.Support;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CustomerPortal_PayDueInvoices {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // CUSTOMER PORTAL — BILLING PAGE (Tab 2)
    // URL: test.footprintseducation.in/billing?idk=...
    // ═══════════════════════════════════════════════
    @FindBy(css = "a.btn[href*='onlinepayment']")
    private WebElement payDueInvoiceLink;

    // ═══════════════════════════════════════════════
    // ONLINE PAYMENT PAGE (Tab 3)
    // URL: test.footprintseducation.in/onlinepayment/?child_id=...
    // ═══════════════════════════════════════════════

    // UPI / NetBanking — hidden input holding payment JSON
    @FindBy(id = "payment_json_icici_upi")
    private WebElement upiPaymentJsonInput;

    // Father radio button — id="otp_on_father", opacity:0 (Bootstrap custom-control-input)
    @FindBy(id = "otp_on_father")
    private WebElement fatherRadioBtn;

    // Phone OTP button — shown after selecting Father/Mother radio
    @FindBy(xpath = "//button[contains(normalize-space(.),'Phone OTP')]")
    private WebElement phoneOtpBtn;

    // Phone OTP input (appears after selecting Father radio)
    @FindBy(xpath = "//input[@id='otp' or @name='otp'" +
            " or contains(@placeholder,'OTP')" +
            " or contains(@placeholder,'otp')]")
    private WebElement otpInput;

    // Verify OTP button — id="btn_otp_submit", text="Continue"
    @FindBy(id = "btn_otp_submit")
    private WebElement verifyOtpBtn;

    // Pay via Credit / Debit Card button — id="btn_icici_ccdc"
    @FindBy(id = "btn_icici_ccdc")
    private WebElement payViaCreditDebitCardBtn;

    // ICICI gateway — cardholder name
    @FindBy(id = "nameOnCard")
    private WebElement cardHolderNameInput;

    // ICICI gateway — card number: id="cardno"
    @FindBy(id = "cardno")
    private WebElement cardNumberInput;

    // ICICI gateway — combined expiry input (fallback; actual gateway uses separate month/year selects)
    @FindBy(id = "expiry")
    private WebElement expiryDateInput;

    // ICICI gateway — expiry month dropdown: id="expMonth"
    @FindBy(id = "expMonth")
    private WebElement expiryMonthSelect;

    // ICICI gateway — expiry year dropdown: id="expYear"
    @FindBy(id = "expYear")
    private WebElement expiryYearSelect;

    // ICICI gateway — CVV: id="cvvno"
    @FindBy(id = "cvvno")
    private WebElement cvvInput;

    // ICICI gateway — Pay / Submit button
    @FindBy(xpath = "//input[@type='submit' or @type='button']" +
            " | //button[@type='submit'" +
            " or contains(normalize-space(.),'Pay')" +
            " or contains(normalize-space(.),'Proceed')]")
    private WebElement submitCardBtn;

    // Card OTP input (after submitting card details)
    @FindBy(xpath = "//input[@id='otp' or @name='otp'" +
            " or @id='authOTP' or contains(@placeholder,'OTP')]")
    private WebElement cardOtpInput;

    // Submit card OTP button
    @FindBy(xpath = "//button[contains(normalize-space(.),'Submit')" +
            " or contains(normalize-space(.),'Verify')" +
            " or contains(normalize-space(.),'Confirm')]")
    private WebElement submitCardOtpBtn;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public CustomerPortal_PayDueInvoices(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // TAB MANAGEMENT
    // ═══════════════════════════════════════════════

    /**
     * Waits for a new tab to appear (compared to handlesBefore)
     * and switches to it.
     * Call pattern:
     * Set<String> before = driver.getWindowHandles();
     * clickSomethingThatOpensNewTab();
     * page.waitAndSwitchToNewTab(before);
     */
    public void waitAndSwitchToNewTab(Set<String> handlesBefore)
            throws InterruptedException {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.getWindowHandles().size() > handlesBefore.size());
        for (String handle : driver.getWindowHandles()) {
            if (!handlesBefore.contains(handle)) {
                driver.switchTo().window(handle);
                Thread.sleep(2000);
                System.out.println("✅ Switched to new tab: "
                        + driver.getCurrentUrl());
                return;
            }
        }
    }

    /**
     * Closes all tabs except mainHandle and returns focus to it.
     */
    public void closeAllExtraTabsAndReturn(String mainHandle)
            throws InterruptedException {
        for (String handle : new ArrayList<>(driver.getWindowHandles())) {
            if (!handle.equals(mainHandle)) {
                driver.switchTo().window(handle);
                driver.close();
            }
        }
        driver.switchTo().window(mainHandle);
        Thread.sleep(500);
        System.out.println("✅ All extra tabs closed");
    }

    // ═══════════════════════════════════════════════
    // BILLING PAGE — Pay Due Invoice
    // ═══════════════════════════════════════════════

    public boolean isPayDueInvoiceBtnPresent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.visibilityOf(payDueInvoiceLink));
            System.out.println("✅ Pay Due Invoice button found");
            return true;
        } catch (Exception e) {
            System.out.println("⚠ Pay Due Invoice button not found: "
                    + e.getMessage());
            return false;
        }
    }

    public void clickPayDueInvoice() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(payDueInvoiceLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", payDueInvoiceLink);
        System.out.println("▶ Pay Due Invoice clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // UPI / NET BANKING FLOW
    // Extracts hidden input: id="payment_json_icici_upi"
    // ═══════════════════════════════════════════════

    public String extractUpiPaymentJson() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.id("payment_json_icici_upi")));
            Thread.sleep(1000);

            String json = (String) ((JavascriptExecutor) driver)
                    .executeScript(
                            "return document.getElementById(" +
                                    "'payment_json_icici_upi').value;");

            if (json != null && !json.isEmpty()) {
                System.out.println("✅ UPI JSON extracted (length="
                        + json.length() + ")");
            } else {
                System.out.println("⚠ UPI JSON value is empty");
            }
            return json != null ? json : "";
        } catch (Exception e) {
            System.out.println("❌ UPI JSON extraction failed: "
                    + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // CC/DC HIDDEN JSON — id="payment_json_icici_ccdc"
    // Extracts the CC/DC payment JSON hidden input,
    // present on the same payment page as the UPI one.
    // ═══════════════════════════════════════════════

    public String extractCcdcPaymentJson() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.id("payment_json_icici_ccdc")));
            Thread.sleep(500);

            String json = (String) ((JavascriptExecutor) driver)
                    .executeScript(
                            "return document.getElementById(" +
                                    "'payment_json_icici_ccdc').value;");

            if (json != null && !json.isEmpty()) {
                System.out.println("✅ CC/DC JSON extracted (length="
                        + json.length() + ")");
            } else {
                System.out.println("⚠ CC/DC JSON value is empty");
            }
            return json != null ? json : "";
        } catch (Exception e) {
            System.out.println("❌ CC/DC JSON extraction failed: "
                    + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // CREDIT / DEBIT CARD FLOW
    // ═══════════════════════════════════════════════

    public boolean isFatherRadioPresent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.id("otp_on_father")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // opacity:0 due to Bootstrap custom-control-input — use presenceOfElementLocated
    // then JS click to bypass the visibility/opacity check
    public void clickFatherRadio() throws InterruptedException {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("otp_on_father")));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", fatherRadioBtn);
        System.out.println("▶ Father radio selected");
        Thread.sleep(1000);
    }

    // Clicks the "Phone OTP" button to trigger SMS to the father's phone
    public void clickPhoneOtpButton() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(phoneOtpBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", phoneOtpBtn);
        System.out.println("▶ Phone OTP button clicked — SMS triggered");
        Thread.sleep(2000);
    }

    public void enterPhoneOtp(String otp) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(otpInput));
        otpInput.clear();
        otpInput.sendKeys(otp);
        System.out.println("✅ Phone OTP entered");
        Thread.sleep(500);
    }

    public void clickVerifyOtp() throws InterruptedException {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.id("btn_otp_submit")));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", verifyOtpBtn);
        System.out.println("▶ Verify OTP (Continue) clicked");
        Thread.sleep(2000);
    }

    public void clickPayViaCreditDebitCard() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(payViaCreditDebitCardBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();",
                        payViaCreditDebitCardBtn);
        System.out.println("▶ Pay via Credit/Debit Card clicked");
        Thread.sleep(3000);
    }

    /**
     * Fills card details on the payment gateway form.
     * If the form is inside an iframe, call switchToPaymentIframe() first.
     */
    public void fillCardDetails(String cardNumber, String expiry, String cvv)
            throws InterruptedException {
        try {
            wait.until(ExpectedConditions.visibilityOf(cardNumberInput));
        } catch (Exception e) {
            System.out.println("⚠ cardNumberInput not found — inputs on page:");
            driver.findElements(By.tagName("input")).forEach(i ->
                    System.out.println("   id=" + i.getAttribute("id")
                            + " name=" + i.getAttribute("name")
                            + " placeholder=" + i.getAttribute("placeholder")
                            + " type=" + i.getAttribute("type")));
            System.out.println("▶ iframes on page: "
                    + driver.findElements(By.tagName("iframe")).size());
            throw e;
        }
        cardNumberInput.clear();
        cardNumberInput.sendKeys(cardNumber);
        Thread.sleep(300);

        expiryDateInput.clear();
        expiryDateInput.sendKeys(expiry);
        Thread.sleep(300);

        cvvInput.clear();
        cvvInput.sendKeys(cvv);
        System.out.println("✅ Card details filled — "
                + cardNumber.substring(cardNumber.length() - 4));
        Thread.sleep(500);
    }

    public void submitCardPayment() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(submitCardBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", submitCardBtn);
        System.out.println("▶ Card payment submitted");
        Thread.sleep(4000);
    }

    public void enterCardOtp(String otp) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(cardOtpInput));
        cardOtpInput.clear();
        cardOtpInput.sendKeys(otp);
        System.out.println("✅ Card OTP entered");
        Thread.sleep(500);
    }

    public void submitCardOtp() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(submitCardOtpBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", submitCardOtpBtn);
        System.out.println("▶ Card OTP submitted");
        Thread.sleep(3000);
    }

    /**
     * Extracts data-post attribute value after card payment completes.
     * Switches back to default content before searching.
     */
    public String extractDataPostValue() {
        try {
            driver.switchTo().defaultContent();
        } catch (Exception ignored) {
        }
        try {
            WebElement el = new WebDriverWait(driver,
                    Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("[data-post]")));
            String raw = el.getAttribute("data-post");
            System.out.println("✅ data-post extracted (length="
                    + (raw != null ? raw.length() : 0) + ")");
            return raw != null ? raw : "";
        } catch (Exception e) {
            System.out.println("❌ data-post not found: " + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // IFRAME HELPER
    // Call before fillCardDetails() if gateway uses iframe
    // ═══════════════════════════════════════════════
    public void switchToPaymentIframe() {
        try {
            List<WebElement> iframes = driver.findElements(
                    By.tagName("iframe"));
            if (!iframes.isEmpty()) {
                driver.switchTo().frame(iframes.get(0));
                System.out.println("✅ Switched to payment iframe");
            } else {
                System.out.println("▶ No iframe — card form in main page");
            }
        } catch (Exception e) {
            System.out.println("⚠ Iframe switch: " + e.getMessage());
        }
    }

    public void switchToDefaultContent() {
        try {
            driver.switchTo().defaultContent();
            System.out.println("✅ Switched back to default content");
        } catch (Exception ignored) {
        }
    }
}
