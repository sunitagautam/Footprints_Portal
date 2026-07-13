package pages.Settings;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSView_Page {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // SMS TABLE
    // Most recent SMS is always the first row.
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//table//tbody//tr[1]")
    private WebElement firstRow;

    @FindBy(xpath = "//table//tbody//tr")
    private List<WebElement> allRows;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public SMSView_Page(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // IS PAGE LOADED
    // ═══════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//table//tbody//tr[1]")));
            System.out.println("✅ SMS View page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ SMS View page not loaded: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // GET LATEST OTP
    // Reads the first (most recent) row and returns
    // the first 4-digit code found in the SMS text.
    // Document: "copy the sms text columns 4 digits
    //            (mentioned in starting)"
    // ═══════════════════════════════════════════════
    public String getLatestOtp() throws InterruptedException {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//table//tbody//tr[1]")));
        Thread.sleep(1000);
        PageFactory.initElements(driver, this);

        String smsText = getSmsTextFromRow(firstRow);
        System.out.println("▶ Latest SMS: " + smsText);

        String otp = extractFirstFourDigits(smsText);
        if (!otp.isEmpty()) {
            System.out.println("✅ OTP extracted: " + otp);
        } else {
            System.out.println("❌ 4-digit OTP not found in SMS: "
                    + smsText);
        }
        return otp;
    }

    // ═══════════════════════════════════════════════
    // GET OTP FOR A SPECIFIC PHONE NUMBER
    // Scans all rows to match the phone number and
    // returns the OTP from that row's SMS text.
    // ═══════════════════════════════════════════════
    public String getOtpForPhone(String phone) throws InterruptedException {
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//table//tbody//tr[1]")));
        Thread.sleep(1000);
        PageFactory.initElements(driver, this);

        for (WebElement row : allRows) {
            if (row.getText().contains(phone)) {
                String smsText = getSmsTextFromRow(row);
                System.out.println("▶ Matched phone " + phone
                        + " | SMS: " + smsText);
                return extractFirstFourDigits(smsText);
            }
        }
        System.out.println("⚠ No SMS found for phone: " + phone);
        return "";
    }

    // ═══════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════

    /**
     * Gets the SMS message text from a table row.
     * Typical column layout: Sr | Phone | Status | Message | Date
     * Tries column index 3 first; falls back to full row text.
     */
    private String getSmsTextFromRow(WebElement row) {
        try {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (cells.size() > 3) {
                String text = cells.get(3).getText().trim();
                if (!text.isEmpty()) return text;
            }
            return row.getText().trim();
        } catch (Exception e) {
            System.out.println("⚠ getSmsTextFromRow: " + e.getMessage());
            return "";
        }
    }

    /**
     * Extracts the first 4-digit number from the SMS text.
     * E.g. "1234 is your OTP for Footprints" → "1234"
     */
    private String extractFirstFourDigits(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher m = Pattern.compile("\\b(\\d{4})\\b").matcher(text);
        if (m.find()) return m.group(1);
        Matcher m2 = Pattern.compile("(\\d{4})").matcher(text);
        if (m2.find()) return m2.group(1);
        return "";
    }
}
