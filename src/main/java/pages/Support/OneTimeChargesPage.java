package pages.Support;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OneTimeChargesPage {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // MAIN PAGE
    // ═══════════════════════════════════════════════
    @FindBy(id = "btn_child_details")
    WebElement fetchChildDetailsBtn;

    // ═══════════════════════════════════════════════
    // APPLY ONE TIME CHARGE MODAL — FORM FIELDS
    // ═══════════════════════════════════════════════
    // ✅ Add One Time Charges button — top right
    @FindBy(xpath = "//button[contains(.,'Add One Time Charges')]")
    private WebElement addOneTimeChargesBtn;

    // ✅ Fetch Child Details button
    // ✅ Child ID input
    @FindBy(xpath = "//*[@id='charge_child_id']")
    private WebElement childIdInput;
    // ✅ Child Name — auto-filled after fetch
    @FindBy(id = "child_name")
    private WebElement childNameInput;

    // ✅ Charge Type — standard <select> dropdown
    // id="charge_type" name="charge_type"
    @FindBy(id = "charge_type")
    private WebElement chargeTypeDropdown;

    // ✅ Charge Amount
    @FindBy(id = "charge_amount")
    private WebElement chargeAmountInput;

    // ✅ Comment textarea
    @FindBy(id = "charge_comments")
    private WebElement chargeCommentsInput;

    // ✅ Submit form button — id="apply_charge"
    @FindBy(id = "apply_charge")
    private WebElement submitFormBtn;

    // ✅ Close modal — × blue circle
    @FindBy(css = "button.close-popdown[data-dismiss='modal']")
    private WebElement closeModalBtn;
    // ═══════════════════════════════════════════════
    // CONFIRM? POPUP
    // "This will immediately generate an invoice..."
    // ═══════════════════════════════════════════════

    // ✅ Confirm Submit — blue Submit button
    @FindBy(id = "submit_apply_charge")
    private WebElement confirmSubmitBtn;

    // ✅ Confirm Close — red Close button
    @FindBy(xpath = "//div[contains(@id,'confirm') or " +
            ".//h4[contains(.,'Confirm')]]" +
            "//button[contains(.,'Close')]")
    private WebElement confirmCloseBtn;

    // ✅ Confirm popup title
    @FindBy(xpath = "//h4[contains(.,'Confirm')]" +
            " | //h5[contains(.,'Confirm')]")
    private WebElement confirmPopupTitle;

    // ═══════════════════════════════════════════════
    // SUCCESS MESSAGE inside modal
    // "Charges applied successfully!"
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//*[contains(.,'applied successfully')]" +
            " | //*[contains(@class,'alert-success')]")
    private WebElement successMessage;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public OneTimeChargesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // IS PAGE LOADED
    // ═══════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions
                    .elementToBeClickable(addOneTimeChargesBtn));
            System.out.println("✅ OneTime Charges page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Page not loaded: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK ADD ONE TIME CHARGES
    // ═══════════════════════════════════════════════
    public void clickAddOneTimeCharges()
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(addOneTimeChargesBtn));
        addOneTimeChargesBtn.click();
        System.out.println("▶ Add One Time Charges clicked");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // IS FORM MODAL VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isFormModalVisible() {
        try {
            wait.until(ExpectedConditions
                    .visibilityOf(childIdInput));
            return childIdInput.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // ENTER CHILD ID
    // ═══════════════════════════════════════════════
    public void enterChildId(String childId) throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(childIdInput));
        childIdInput.clear();
        childIdInput.sendKeys(childId);
        System.out.println("✅ Child ID entered: " + childId);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // CLICK FETCH CHILD DETAILS
    // ✅ Loads Child Name after entering Child ID
    // ═══════════════════════════════════════════════
    public void clickFetchChildDetails() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(fetchChildDetailsBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", fetchChildDetailsBtn);
        System.out.println("▶ Fetch Child Details clicked");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // GET CHILD NAME — auto-filled after fetch
    // ═══════════════════════════════════════════════
    public String getChildName() {
        try {
            // ✅ Try as text element first
            String name = childNameInput.getText().trim();
            if (name.isEmpty()) {
                name = childNameInput
                        .getAttribute("value");
            }
            if (name == null) name = "";
            System.out.println("▶ Child Name: " + name);
            return name.trim();
        } catch (Exception e) {
            System.out.println("⚠ Child name not found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // SELECT CHARGE TYPE
    // ✅ Standard <select> — uses selectByVisibleText
    // @param chargeType exact visible text e.g. "Book Set"
    // ═══════════════════════════════════════════════
    public void selectChargeType(String chargeType)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .visibilityOf(chargeTypeDropdown));
        Select select = new Select(chargeTypeDropdown);
        select.selectByVisibleText(chargeType);
        System.out.println("✅ Charge Type: " + chargeType);
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // GET CHARGE AMOUNT — reads auto-filled value
    // ═══════════════════════════════════════════════
    public String getChargeAmount() {
        try {
            String val = chargeAmountInput
                    .getAttribute("value");
            if (val == null || val.isEmpty()) {
                val = chargeAmountInput.getText().trim();
            }
            System.out.println("▶ Charge Amount: " + val);
            return val == null ? "" : val.trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    //    - Auto-filled (readonly) → JS removes readonly, sets value
//    - Manual entry            → JS sets value directly
// ═══════════════════════════════════════════════
    public void enterChargeAmount(String amount)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .visibilityOf(chargeAmountInput));

        // ✅ Remove readonly/disabled — handles auto-filled fields
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].removeAttribute('disabled');" +
                        "arguments[0].value = arguments[1];",
                chargeAmountInput, amount);

        // ✅ Fire change event so app registers the new value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input'));" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                chargeAmountInput);

        System.out.println("✅ Charge Amount: " + amount);
        Thread.sleep(200);
    }

    // ═══════════════════════════════════════════════
// ✅ Same fix — comments field also readonly in some cases
// ═══════════════════════════════════════════════
    public void enterChargeComments(String comments)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .visibilityOf(chargeCommentsInput));

        // ✅ Remove readonly/disabled — then set value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].removeAttribute('disabled');" +
                        "arguments[0].value = arguments[1];",
                chargeCommentsInput, comments);

        // ✅ Fire change event so app registers the new value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input'));" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                chargeCommentsInput);

        System.out.println("✅ Comments: " + comments);
        Thread.sleep(200);
    }

    // ═══════════════════════════════════════════════
    // ENTER LATE STAY DETAILS
    // ═══════════════════════════════════════════════

    // ✅ Fix — use JS to set date value
    public void enterLateStayDetails(String date, String hour, String minute)
            throws InterruptedException {

        // ✅ Date — JS set (datepicker is readonly to direct typing)
        try {
            WebElement dateField = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("date")));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].removeAttribute('readonly');" +
                            "arguments[0].value = arguments[1];",
                    dateField, date);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('change'));" +
                            "arguments[0].dispatchEvent(new Event('input'));",
                    dateField);
            System.out.println("✅ Late Stay Date: " + date);
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("⚠ Date field: " + e.getMessage());
        }

        // ✅ Select Hour
        try {
            WebElement hourSelect = driver.findElement(By.id("hour"));
            new Select(hourSelect).selectByValue(hour);
            System.out.println("✅ Late Stay Hour: " + hour);
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("⚠ Hour field: " + e.getMessage());
        }

        // ✅ Select Minute — use "00" or "30" as value
        try {
            WebElement minSelect = driver.findElement(By.id("minute"));
            new Select(minSelect).selectByValue(minute);
            System.out.println("✅ Late Stay Min: " + minute);
            Thread.sleep(300);
        } catch (Exception e) {
            // ✅ Try index if value fails
            try {
                WebElement minSelect = driver.findElement(By.id("minute"));
                new Select(minSelect).selectByVisibleText(minute);
            } catch (Exception e2) {
                System.out.println("⚠ Minute field: " + e.getMessage());
            }
        }

        // ✅ Click Calculate
        try {
            WebElement calcBtn = driver.findElement(By.id("calculate_charge"));
            calcBtn.click();
            System.out.println("▶ Calculate clicked");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("⚠ Calculate: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK SUBMIT FORM
    // ✅ Opens Confirm? popup
    // ═══════════════════════════════════════════════
    public void clickSubmitForm() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(submitFormBtn));
        submitFormBtn.click();
        System.out.println("▶ Submit Form clicked");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // IS CONFIRMATION POPUP VISIBLE
    // "This will immediately generate an invoice..."
    // ═══════════════════════════════════════════════
    public boolean isConfirmationPopupVisible() {
        try {
            // ✅ Check by submit button or text
            new WebDriverWait(driver,
                    Duration.ofSeconds(IAutoConstant.SHORT_WAIT))
                    .until(ExpectedConditions
                            .elementToBeClickable(confirmSubmitBtn));
            System.out.println("✅ Confirmation popup visible");
            return true;
        } catch (Exception e) {
            try {
                WebElement popup = driver.findElement(
                        By.xpath("//*[contains(.," +
                                "'generate an invoice')]"));
                return popup.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK CONFIRM SUBMIT — Blue Submit button
    // ═══════════════════════════════════════════════
    public void clickConfirmSubmit()
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(confirmSubmitBtn));
        confirmSubmitBtn.click();
        System.out.println("▶ Confirm Submit clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // CLICK CONFIRM CLOSE — Red Close button
    // ═══════════════════════════════════════════════
    public void clickConfirmClose()
            throws InterruptedException {
        try {
            wait.until(ExpectedConditions
                    .elementToBeClickable(confirmCloseBtn));
            confirmCloseBtn.click();
        } catch (Exception e) {
            // ✅ Fallback by text
            WebElement btn = driver.findElement(
                    By.xpath("//button[contains(.,'Close')]" +
                            "[not(contains(@class,'modal-close'))]"));
            btn.click();
        }
        System.out.println("▶ Confirm Close clicked");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // GET SUCCESS MESSAGE
    // "Charges applied successfully!"
    // ═══════════════════════════════════════════════
    public String getSuccessMessage() {
        try {
            WebElement msgEl = new WebDriverWait(driver,
                    Duration.ofSeconds(10))
                    .until(ExpectedConditions
                            .visibilityOfElementLocated(
                                    By.xpath(
                                            "//*[contains(.," +
                                                    "'applied successfully')]" +
                                                    " | //*[contains(@class," +
                                                    "'alert-success')]")));
            String msg = msgEl.getText().trim();
            System.out.println("✅ Success: " + msg);
            return msg;
        } catch (Exception e) {
            System.out.println("⚠ Success msg not found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // CLOSE MODAL — × blue button
    // ═══════════════════════════════════════════════
    public void closeModal() throws InterruptedException {
        try {
            wait.until(ExpectedConditions
                    .elementToBeClickable(closeModalBtn));
            ((JavascriptExecutor) driver)
                    .executeScript(
                            "arguments[0].click();", closeModalBtn);
            System.out.println("✅ Modal closed via × button");
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("▶ Modal already closed or not present");
        }
    }

    // ═══════════════════════════════════════════════
    // VERIFY ALL CHARGE TYPES PRESENT IN DROPDOWN
    // ✅ Used by SC_015_TC_004
    // ═══════════════════════════════════════════════
    public boolean verifyAllChargeTypesPresent(
            String[] expectedTypes) {
        try {
            Select select = new Select(chargeTypeDropdown);
            List<WebElement> options = select.getOptions();
            List<String> optionTexts = new ArrayList<>();
            for (WebElement opt : options) {
                optionTexts.add(opt.getText().trim());
            }
            System.out.println("▶ Options found: "
                    + optionTexts);

            boolean allFound = true;
            for (String expected : expectedTypes) {
                if (!optionTexts.contains(expected)) {
                    System.out.println("❌ Missing: " + expected);
                    allFound = false;
                } else {
                    System.out.println("✅ Found: " + expected);
                }
            }
            return allFound;
        } catch (Exception e) {
            System.out.println("⚠ verifyAllChargeTypes: "
                    + e.getMessage());
            return false;
        }
    }


}
