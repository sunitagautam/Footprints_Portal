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
import java.util.List;

public class AccountStatementPage {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // GENERATE REPORT FORM
    // ═══════════════════════════════════════════════
    @FindBy(id = "frm_child_id")
    private WebElement admissionIdInput;

    @FindBy(xpath = "//select[@name='month_from']")
    private WebElement fromMonthSelect;

    @FindBy(xpath = "//select[@name='month_to']")
    private WebElement toMonthSelect;

    @FindBy(name = "statement_bnt")
    private WebElement generateBtn;

    // ═══════════════════════════════════════════════
    // DEFAULT MESSAGE — before generate
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//*[contains(text(),'Account statement not retrieved')]")
    private WebElement noDataMessage;

    // ═══════════════════════════════════════════════
    // PDF DOWNLOAD — id="download_statement"
    // ═══════════════════════════════════════════════
    @FindBy(id = "download_statement")
    private WebElement pdfDownloadIcon;

    // ═══════════════════════════════════════════════
    // ACCOUNT SUMMARY
    // ═══════════════════════════════════════════════
    @FindBy(id = "account_balance")
    private WebElement accountBalanceSpan;

    @FindBy(id = "credit_balance")
    private WebElement creditBalanceSpan;

    // ═══════════════════════════════════════════════
    // CHILD NAME LINK (appears after generate)
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//html//body//div[5]//div//div//div[1]//div[2]//div[3]" +
            "//legend//div[1]/a[@title='Child Information']")
    private WebElement childNameLink;

    // ═══════════════════════════════════════════════
    // ACTION LINKS — span text is mixed-case inside anchor
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//div[3]/legend/div[2]/div/div[1]/a[1]/span[text()='Add Charges']")
    private WebElement addChargesLink;

    @FindBy(xpath = "//div[3]//legend//div[2]//div//div[1]//a[2]//span[text()='Child Plan']")
    private WebElement childPlanLink;

    @FindBy(xpath = "//a[.//span[normalize-space()='Center Plan']]")
    private WebElement centerPlanLink;

    @FindBy(xpath = "//div[3]//legend//div[2]//div//div[1]//a[4]//span[text()='Diary Notes']")
    private WebElement diaryNotesLink;

    @FindBy(xpath = "//a[.//span[normalize-space()='Child History']]")
    private WebElement childHistoryLink;

    @FindBy(xpath = "//div[3]//legend//div[2]//div//div[1]//a[6]//span[text()='Child Info']")
    private WebElement childInfoLink;

    @FindBy(xpath = "//div[3]//legend//div[2]//div//div[1]//a[7]//span[text()='Service Request']")
    private WebElement serviceRequestLink;

    @FindBy(xpath = "//a[.//span[normalize-space()='Customer Portal']]")
    private WebElement customerPortalLink;

    @FindBy(xpath = "//div[3]//legend//div[2]//div//div[1]//a[9]//span[text()='Customer Requests']")
    private WebElement customerRequestLink;

    // ═══════════════════════════════════════════════
    // MIGRATION BUTTONS (all-caps anchor text)
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//a[normalize-space(.)='MIGRATE FROM REGULAR TO CORPORATE']")
    private WebElement migrateRegularToCorporateBtn;

    @FindBy(xpath = "//a[normalize-space(.)='MIGRATE FROM CORPORATE TO REGULAR']")
    private WebElement migrateCorporateToRegularBtn;

    @FindBy(xpath = "//a[contains(.,'CO-PAY') and contains(.,'CHILD PLAN UPDATE')]")
    private WebElement coPayChildPlanUpdateBtn;

    @FindBy(xpath = "//a[contains(.,'RECTIFY BRANCH')" +
            " or contains(.,'Rectify Branch')]")
    private WebElement rectifyBranchBtn;

    // ═══════════════════════════════════════════════
    // ADD CHARGES MODAL  (reuses OneTimeChargesPage elements)
    // ═══════════════════════════════════════════════
    @FindBy(id = "charge_type")
    private WebElement chargeTypeDropdown;

    @FindBy(id = "apply_charge")
    private WebElement submitFormBtn;

    @FindBy(id = "submit_apply_charge")
    private WebElement confirmSubmitBtn;

    // ═══════════════════════════════════════════════
    // CHILD PLAN MODAL   — id="popdown-dialog"
    // ═══════════════════════════════════════════════
    @FindBy(id = "selected_effective_date")
    private WebElement yearDropdown;

    @FindBy(xpath = "//*[@id='frm_apply_fee_card']//div[3]//div//input")
    private WebElement feeCardCheckBtn;

    @FindBy(xpath = "//*[@id='frm_apply_fee_card']//div[4]//div//input")
    private WebElement updateFeeCardBtn;

    // ═══════════════════════════════════════════════
    // SHARED MODAL CLOSE — id="popdown-dialog"
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//*[@id='popdown-dialog']//div//div//div//div//div[3]//button[text()='Close']")
    private WebElement popdownCloseBtn;

    // ═══════════════════════════════════════════════
    // DIARY NOTES MODAL
    // ═══════════════════════════════════════════════
    @FindBy(id = "note_text")
    private WebElement diaryCommentInput;

    @FindBy(id = "add_notes_button")
    private WebElement diarySubmitBtn;

    @FindBy(xpath = "//*[contains(text(),'No Comments Yet')]")
    private WebElement noCommentsYetMsg;

    // ═══════════════════════════════════════════════
    // CHILD HISTORY MODAL
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//div[@class='modal-body text-left']" +
            "//div[@class='pre-scrollable']//p")
    private List<WebElement> historyParagraphs;

    // ═══════════════════════════════════════════════
    // CHILD INFO MODAL — TABS
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//a[contains(.,'Basic Information')]" +
            " | //li[contains(.,'Basic Information')]//a")
    private WebElement basicInfoTab;

    @FindBy(xpath = "//a[contains(.,'Home Address')]" +
            " | //li[contains(.,'Home Address')]//a")
    private WebElement homeAddressTab;

    @FindBy(xpath = "//a[contains(.,'Billing Information')]" +
            " | //li[contains(.,'Billing Information')]//a")
    private WebElement billingInfoTab;

    @FindBy(xpath = "//a[contains(.,'Admission Payment')]" +
            " | //li[contains(.,'Admission Payment')]//a")
    private WebElement admissionPaymentTab;

    // ═══════════════════════════════════════════════
    // SERVICE REQUEST MODAL
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//select[@id='service_type'" +
            " or @name='service_type'" +
            " or @id='request_type']")
    private WebElement serviceTypeDropdown;

    // ═══════════════════════════════════════════════
    // TOAST / SUCCESS MESSAGE
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//*[contains(@class,'alert-success')" +
            " or contains(@class,'toast')" +
            " or contains(@id,'toast')]")
    private WebElement toastMessage;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public AccountStatementPage(WebDriver driver) {
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
            wait.until(ExpectedConditions.visibilityOf(admissionIdInput));
            System.out.println("✅ Account Statement page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Page not loaded: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // DEFAULT MESSAGE VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isDefaultMessageVisible() {
        try {
            return noDataMessage.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // ENTER ADMISSION ID
    // ✅ id="frm_child_id"  type="number"
    // ═══════════════════════════════════════════════
    public void enterAdmissionId(String admissionId)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(admissionIdInput));
        admissionIdInput.clear();
        admissionIdInput.sendKeys(admissionId);
        System.out.println("✅ Admission ID entered: " + admissionId);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // SELECT FROM MONTH  — SELECT name="month_from"
    // ═══════════════════════════════════════════════
    public void selectFromMonth(String monthYear)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(fromMonthSelect));
        new Select(fromMonthSelect).selectByVisibleText(monthYear);
        System.out.println("✅ From Month: " + monthYear);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // SELECT TO MONTH  — SELECT name="month_to"
    // ═══════════════════════════════════════════════
    public void selectToMonth(String monthYear)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(toMonthSelect));
        new Select(toMonthSelect).selectByVisibleText(monthYear);
        System.out.println("✅ To Month: " + monthYear);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // SELECT FIRST AVAILABLE OPTION FROM DROPDOWN
    // Picks the first non-empty option (index 0 or 1)
    // ═══════════════════════════════════════════════
    private void selectFirstOption(WebElement selectEl, String label)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(selectEl));
        Select sel = new Select(selectEl);
        List<WebElement> options = sel.getOptions();
        for (WebElement opt : options) {
            String text = opt.getText().trim();
            // Skip blank placeholders like "--Select--", "-- Select --"
            if (!text.isEmpty() && !text.startsWith("-")) {
                sel.selectByVisibleText(text);
                System.out.println("✅ " + label + ": " + text);
                break;
            }
        }
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // CLICK GENERATE  — input name="statement_bnt"
    // ═══════════════════════════════════════════════
    public void clickGenerate() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(generateBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", generateBtn);
        System.out.println("▶ Generate clicked");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // GENERATE — selects top month from both dropdowns
    // ═══════════════════════════════════════════════
    public void generateAccountStatement(String admissionId)
            throws InterruptedException {
        enterAdmissionId(admissionId);
        selectFirstOption(fromMonthSelect, "From Month");
        selectFirstOption(toMonthSelect, "To Month");
        clickGenerate();
    }

    // ═══════════════════════════════════════════════
    // GENERATE — with explicit month range
    // ═══════════════════════════════════════════════
    public void generateAccountStatement(String admissionId,
                                         String fromMonth, String toMonth)
            throws InterruptedException {
        enterAdmissionId(admissionId);
        selectFromMonth(fromMonth);
        selectToMonth(toMonth);
        clickGenerate();
    }

    // ═══════════════════════════════════════════════
    // IS ACCOUNT SUMMARY VISIBLE (after generate)
    // ═══════════════════════════════════════════════
    public boolean isAccountSummaryVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(accountBalanceSpan));
            return true;
        } catch (Exception e) {
            try {
                return driver.findElement(By.id("lead_data")).isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    // ═══════════════════════════════════════════════
    // MIGRATION BUTTON VISIBILITY CHECKS
    // ═══════════════════════════════════════════════
    public boolean isMigrateRegularToCorporateVisible() {
        try {
            return migrateRegularToCorporateBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isMigrateCorporateToRegularVisible() {
        try {
            return migrateCorporateToRegularBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isCoPayChildPlanUpdateVisible() {
        try {
            return coPayChildPlanUpdateBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRectifyBranchBtnVisible() {
        try {
            new WebDriverWait(driver,
                    Duration.ofSeconds(IAutoConstant.SHORT_WAIT))
                    .until(ExpectedConditions.visibilityOf(rectifyBranchBtn));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK PDF DOWNLOAD — id="download_statement"
    // ═══════════════════════════════════════════════
    public void clickPdfDownload() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(pdfDownloadIcon));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", pdfDownloadIcon);
        System.out.println("▶ PDF Download clicked");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // GET TOAST / SUCCESS MESSAGE
    // ═══════════════════════════════════════════════
    public String getToastMessage() {
        try {
            WebElement toast = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(toastMessage));
            String msg = toast.getText().trim();
            System.out.println("✅ Toast: " + msg);
            return msg;
        } catch (Exception e) {
            System.out.println("⚠ Toast not found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // ACCEPT CONFIRMATION ALERT
    // ═══════════════════════════════════════════════
    public void acceptConfirmationAlert() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
            System.out.println("✅ Alert accepted");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("⚠ No alert: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // SHARED POPDOWN CLOSE
    // ═══════════════════════════════════════════════
    public void closePopdown() throws InterruptedException {
        try {
            wait.until(ExpectedConditions
                    .elementToBeClickable(popdownCloseBtn));
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", popdownCloseBtn);
            System.out.println("✅ Popdown closed");
            Thread.sleep(500);
        } catch (Exception e) {
            closeModalByJs();
        }
    }

    // ═══════════════════════════════════════════════
    // ADD CHARGES
    // ═══════════════════════════════════════════════
    public void clickAddCharges() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(addChargesLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", addChargesLink);
        System.out.println("▶ Add Charges clicked");
        Thread.sleep(1000);
    }

    public boolean isAddChargesModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(chargeTypeDropdown));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CHILD PLAN MODAL
    // ═══════════════════════════════════════════════
    public void clickChildPlan() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(childPlanLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", childPlanLink);
        System.out.println("▶ Child Plan clicked");
        Thread.sleep(1000);
    }

    public boolean isChildPlanModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(yearDropdown));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void selectChildPlanYear(String year) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(yearDropdown));
        new Select(yearDropdown).selectByVisibleText(year);
        System.out.println("✅ Year selected: " + year);
        Thread.sleep(500);
    }

    public void clickUpdateFeeCard() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(updateFeeCardBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", updateFeeCardBtn);
        System.out.println("▶ Update Fee Card clicked");
        Thread.sleep(1000);
    }

    public void clickChildPlanUpdate() throws InterruptedException {
        clickUpdateFeeCard();
    }

    public void closeChildPlanModal() throws InterruptedException {
        closePopdown();
    }

    // ═══════════════════════════════════════════════
    // CENTER PLAN MODAL
    // ═══════════════════════════════════════════════
    public void clickCenterPlan() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(centerPlanLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", centerPlanLink);
        System.out.println("▶ Center Plan clicked");
        Thread.sleep(1000);
    }

    public boolean isCenterPlanModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(popdownCloseBtn));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void closeCenterPlanModal() throws InterruptedException {
        closePopdown();
    }

    // ═══════════════════════════════════════════════
    // CENTER PLAN — HALF DAY FEE (SHORT TERM / V2)
    // Confirmed DOM (Center Fee Plan popup table):
    // <tr><td>Half Day</td><td class="amt">V1 amount</td><td class="amt">V2 amount</td></tr>
    // Columns are positional: td[1]=Program label, td[2]=Long Term Fee (V1),
    // td[3]=Short Term Fee (V2) — this is the value used for the Extended
    // Daycare per-day pricing formula.
    // ═══════════════════════════════════════════════
    public double getHalfDayFeeV2FromCenterPlan() {
        try {
            WebElement cell = driver.findElement(By.xpath(
                    "//tr[td[1][normalize-space(.)='Half Day']]/td[3]"));
            String cleaned = cell.getText().replace("₹", "").replace(",", "").trim();
            double amount = Double.parseDouble(cleaned);
            System.out.println("✅ Half Day fee V2 (Center Plan): " + amount);
            return amount;
        } catch (Exception e) {
            System.out.println("⚠ getHalfDayFeeV2FromCenterPlan: " + e.getMessage());
        }
        return -1;
    }

    // ═══════════════════════════════════════════════
    // DIARY NOTES MODAL  — id="note_text", "add_notes_button"
    // ═══════════════════════════════════════════════
    public void clickDiaryNotes() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(diaryNotesLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", diaryNotesLink);
        System.out.println("▶ Diary Notes clicked");
        Thread.sleep(1000);
    }

    public boolean isDiaryNotesModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(diaryCommentInput));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isNoCommentsYetVisible() {
        try {
            return noCommentsYetMsg.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void enterDiaryComment(String comment) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(diaryCommentInput));
        diaryCommentInput.clear();
        diaryCommentInput.sendKeys(comment);
        System.out.println("✅ Diary comment entered");
        Thread.sleep(300);
    }

    public void clickDiarySubmit() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(diarySubmitBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", diarySubmitBtn);
        System.out.println("▶ Diary Submit clicked");
        Thread.sleep(1000);
    }

    public void closeDiaryNotesModal() throws InterruptedException {
        closePopdown();
    }

    // ═══════════════════════════════════════════════
    // CHILD HISTORY MODAL
    // ═══════════════════════════════════════════════
    public void clickChildHistory() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(childHistoryLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", childHistoryLink);
        System.out.println("▶ Child History clicked");
        Thread.sleep(1000);
    }

    public boolean isChildHistoryModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(popdownCloseBtn));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<WebElement> getHistoryParagraphs() {
        return historyParagraphs;
    }

    public void closeChildHistoryModal() throws InterruptedException {
        closePopdown();
    }

    // ═══════════════════════════════════════════════
    // CHILD INFO MODAL
    // ═══════════════════════════════════════════════
    public void clickChildInfo() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(childInfoLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", childInfoLink);
        System.out.println("▶ Child Info clicked");
        Thread.sleep(1000);
    }

    public boolean isChildInfoModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(basicInfoTab));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void clickBasicInfoTab() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(basicInfoTab));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", basicInfoTab);
        System.out.println("▶ Basic Info tab clicked");
        Thread.sleep(500);
    }

    public void clickHomeAddressTab() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(homeAddressTab));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", homeAddressTab);
        System.out.println("▶ Home Address tab clicked");
        Thread.sleep(500);
    }

    public void clickBillingInfoTab() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(billingInfoTab));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", billingInfoTab);
        System.out.println("▶ Billing Info tab clicked");
        Thread.sleep(500);
    }

    public void clickAdmissionPaymentTab() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(admissionPaymentTab));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", admissionPaymentTab);
        System.out.println("▶ Admission Payment tab clicked");
        Thread.sleep(500);
    }

    public void closeChildInfoModal() throws InterruptedException {
        closePopdown();
    }

    // ═══════════════════════════════════════════════
    // SERVICE REQUEST MODAL
    // ═══════════════════════════════════════════════
    public void clickServiceRequest() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(serviceRequestLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", serviceRequestLink);
        System.out.println("▶ Service Request clicked");
        Thread.sleep(1000);
    }

    public boolean isServiceRequestModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(serviceTypeDropdown));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void selectServiceType(String serviceType)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(serviceTypeDropdown));
        new Select(serviceTypeDropdown).selectByVisibleText(serviceType);
        System.out.println("✅ Service Type: " + serviceType);
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // CUSTOMER REQUEST
    // ═══════════════════════════════════════════════
    public void clickCustomerRequest() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(customerRequestLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", customerRequestLink);
        System.out.println("▶ Customer Request clicked");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // CUSTOMER PORTAL
    // ═══════════════════════════════════════════════
    public void clickCustomerPortal() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(customerPortalLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", customerPortalLink);
        System.out.println("▶ Customer Portal clicked");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // RECTIFY BRANCH
    // ═══════════════════════════════════════════════
    public void clickRectifyBranch() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(rectifyBranchBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", rectifyBranchBtn);
        System.out.println("▶ Rectify Branch clicked");
        Thread.sleep(1000);
    }

    public boolean isRectifyBranchModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(popdownCloseBtn));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void closeRectifyBranchModal() throws InterruptedException {
        closePopdown();
    }

    // ═══════════════════════════════════════════════
    // TAB / WINDOW HELPERS
    // ═══════════════════════════════════════════════
    public boolean isNewTabOpened(int originalTabCount) {
        return driver.getWindowHandles().size() > originalTabCount;
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public void switchToNewTab() {
        String current = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(current)) {
                driver.switchTo().window(handle);
                System.out.println("✅ Switched to: " + driver.getCurrentUrl());
                break;
            }
        }
    }

    public void closeNewTabAndReturn(String mainHandle) {
        try {
            driver.close();
            driver.switchTo().window(mainHandle);
            System.out.println("✅ Returned to main tab");
        } catch (Exception e) {
            System.out.println("⚠ Close tab: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // MONTHLY SUBSCRIPTION — PLAN AMOUNT
    // Half-day/full-day plan fee varies by center, so tests must read the
    // actual displayed amount rather than assume a fixed value.
    // Confirmed DOM: <div class="col-md-12"><b>Plan Amount :</b>
    //   <i class="fa fa-inr"></i> 12999.00 (Monthly)</div>
    // The Monthly Subscription box renders before the Yearly Subscription
    // box, so the first matching div is the monthly one.
    // ═══════════════════════════════════════════════
    public double getMonthlyPlanAmount() {
        try {
            List<WebElement> planAmountDivs = driver.findElements(By.xpath(
                    "//div[contains(@class,'col-md-12')][b[contains(normalize-space(.),'Plan Amount')]]"));
            if (!planAmountDivs.isEmpty()) {
                String text = planAmountDivs.get(0).getText();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\d,]+(?:\\.\\d+)?)").matcher(text);
                if (m.find()) {
                    double amount = Double.parseDouble(m.group(1).replace(",", ""));
                    System.out.println("✅ Monthly Plan Amount: " + amount);
                    return amount;
                }
            }
            System.out.println("⚠ getMonthlyPlanAmount: 'Plan Amount' div not found — falling back to text scan");

            // Fallback: scan visible page text between the subscription headers
            String bodyText = driver.findElement(By.tagName("body")).getText();
            int start = bodyText.indexOf("Monthly Subscription");
            if (start == -1) return -1;
            int end = bodyText.indexOf("Yearly Subscription", start);
            String section = end > start ? bodyText.substring(start, end) : bodyText.substring(start);

            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(
                    "Plan Amount\\s*:?\\s*[₹Rs.]*\\s*([\\d,]+(?:\\.\\d+)?)").matcher(section);
            if (m2.find()) {
                double amount = Double.parseDouble(m2.group(1).replace(",", ""));
                System.out.println("✅ Monthly Plan Amount (fallback): " + amount);
                return amount;
            }
            System.out.println("⚠ getMonthlyPlanAmount: 'Plan Amount' not found in Monthly Subscription section");
        } catch (Exception e) {
            System.out.println("⚠ getMonthlyPlanAmount: " + e.getMessage());
        }
        return -1;
    }

    // ═══════════════════════════════════════════════
    // EXTENDED DAYCARE — INVOICE LINE ITEMS
    // Rendered as sibling .row divs, always visible under the invoice
    // (no expand click needed): Daycare Fee, Preschool Fee, SGST, CGST,
    // Roundoff. Each row: .col-md-3 = label, .col-md-7 = booking comment,
    // .col-md-2 = amount (fa-inr icon glyph has no text content).
    // ═══════════════════════════════════════════════
    public boolean isExtendedDaycareInvoiceVisible() {
        try {
            return !driver.findElements(By.xpath(
                    "//div[contains(@class,'row')]"
                            + "[.//div[contains(@class,'col-md-3')][normalize-space(.)='Daycare Fee']]"
                            + "//div[contains(@class,'col-md-7')][contains(normalize-space(.),'Extended Daycare Charges')]"))
                    .isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public java.util.Map<String, Double> getExtendedDaycareInvoiceLineItems() {
        java.util.Map<String, Double> items = new java.util.LinkedHashMap<>();
        try {
            WebElement daycareFeeRow = driver.findElement(By.xpath(
                    "//div[contains(@class,'row')]"
                            + "[.//div[contains(@class,'col-md-3')][normalize-space(.)='Daycare Fee']]"));
            WebElement container = daycareFeeRow.findElement(By.xpath(".."));
            List<WebElement> rows = container.findElements(By.xpath("./div[contains(@class,'row')]"));
            for (WebElement row : rows) {
                String label = row.findElement(By.cssSelector(".col-md-3")).getText().trim();
                String amtText = row.findElement(By.cssSelector(".col-md-2")).getText()
                        .replace(",", "").replace("₹", "").trim();
                items.put(label, Double.parseDouble(amtText));
            }
            System.out.println("✅ Extended Daycare invoice line items: " + items);
        } catch (Exception e) {
            System.out.println("⚠ getExtendedDaycareInvoiceLineItems: " + e.getMessage());
        }
        return items;
    }

    public double getExtendedDaycareInvoiceTotal() {
        return getExtendedDaycareInvoiceLineItems().values().stream()
                .mapToDouble(Double::doubleValue).sum();
    }

    // ═══════════════════════════════════════════════
    // FORCE-CLOSE ALL MODALS VIA JS — used in @AfterMethod
    // ═══════════════════════════════════════════════
    public void closeModalByJs() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal-backdrop')" +
                            ".forEach(el => el.remove());" +
                            "document.querySelectorAll('.modal')" +
                            ".forEach(el => {" +
                            "  el.style.display='none';" +
                            "  el.classList.remove('in','show');" +
                            "});" +
                            "document.body.classList.remove('modal-open');"
            );
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("⚠ JS close: " + e.getMessage());
        }
    }
}
