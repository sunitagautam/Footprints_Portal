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

    /**
     * Additive fallback for getHistoryParagraphs() — confirmed live
     * (2026-09-22) that the Child History modal has been redesigned into a
     * card-based "Child Updates History (N) / Backend Requests (N)" tabbed
     * layout that no longer matches historyParagraphs' original
     * modal-body/pre-scrollable/p locator (which now finds 0 elements even
     * when entries are clearly visible). Dumps the whole visible modal's
     * text instead, for substring/regex matching — same fallback pattern
     * already used elsewhere in this file (getBillingCancelDateText(),
     * getTransferCaseBannerText()) after narrow element-based locators
     * proved fragile against real app markup.
     */
    public String getChildHistoryFullText() {
        try {
            WebElement modal = driver.findElement(By.xpath(
                    "//*[contains(@class,'modal') and .//*[contains(.,'Child Updates History')]]" +
                            " | //*[contains(.,'Child Updates History')]/ancestor::div[contains(@class,'modal')][1]"));
            String text = modal.getText();
            System.out.println("▶ Child History full text dump:\n" + text);
            return text;
        } catch (Exception e) {
            System.out.println("⚠ getChildHistoryFullText: " + e.getMessage());
            return "";
        }
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
    // TIME EXTENSION — ADDONS LINE
    // Confirmed DOM: <div class="col-md-12"><b>Addons :</b> Time Extension
    //   ( <i class="fa fa-inr"></i> 1500.00)</div>
    // Absent state shows "Addons : Not Available" instead.
    // ═══════════════════════════════════════════════
    public String getAddonsText() {
        try {
            WebElement el = driver.findElement(By.xpath(
                    "//div[contains(@class,'col-md-12')][b[contains(normalize-space(.),'Addons')]]"));
            String text = el.getText().trim();
            System.out.println("✅ Addons: " + text);
            return text;
        } catch (Exception e) {
            System.out.println("⚠ getAddonsText: " + e.getMessage());
            return "";
        }
    }

    public boolean isTimeExtensionAddonPresent() {
        return getAddonsText().toLowerCase().contains("time extension");
    }

    public boolean isTimeExtensionAddonAbsent() {
        String text = getAddonsText();
        return text.toLowerCase().contains("not available") || !text.toLowerCase().contains("time extension");
    }

    // ═══════════════════════════════════════════════
    // TIME EXTENSION — INVOICE LINE ITEMS
    // Same row structure as Extended Daycare (.col-md-3/7/2), but the booking
    // comment reads "Prorated Time Extension Charges - <Month>, <Year> (<N>
    // days)" instead of "Extended Daycare Charges". Confirmed DOM shows only
    // Daycare Fee/SGST/CGST rows (no separate Preschool Fee/Roundoff), but the
    // sibling-row scan below captures whatever rows are actually present.
    // ═══════════════════════════════════════════════
    public boolean isTimeExtensionInvoiceVisible() {
        try {
            return !driver.findElements(By.xpath(
                    "//div[contains(@class,'col-md-7')]"
                            + "[contains(normalize-space(.),'Prorated Time Extension Charges')]"))
                    .isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public java.util.Map<String, Double> getTimeExtensionInvoiceLineItems() {
        java.util.Map<String, Double> items = new java.util.LinkedHashMap<>();
        try {
            WebElement anchorComment = driver.findElement(By.xpath(
                    "//div[contains(@class,'col-md-7')]"
                            + "[contains(normalize-space(.),'Prorated Time Extension Charges')]"));
            WebElement anchorRow = anchorComment.findElement(By.xpath(".."));
            WebElement container = anchorRow.findElement(By.xpath(".."));
            List<WebElement> rows = container.findElements(By.xpath("./div[contains(@class,'row')]"));
            for (WebElement row : rows) {
                String label = row.findElement(By.cssSelector(".col-md-3")).getText().trim();
                String amtText = row.findElement(By.cssSelector(".col-md-2")).getText()
                        .replace(",", "").replace("₹", "").trim();
                items.put(label, Double.parseDouble(amtText));
            }
            System.out.println("✅ Time Extension invoice line items: " + items);
        } catch (Exception e) {
            System.out.println("⚠ getTimeExtensionInvoiceLineItems: " + e.getMessage());
        }
        return items;
    }

    public double getTimeExtensionInvoiceTotal() {
        return getTimeExtensionInvoiceLineItems().values().stream()
                .mapToDouble(Double::doubleValue).sum();
    }

    // ═══════════════════════════════════════════════
    // CANCEL REGISTRATION
    // Confirmed live via screenshots (2026-09-09):
    // Button: <a class="popdown_xl_large btn btn-xs text-muted has-text
    //   reg-padding" href="cancel_registration?pop=yes&child_id=<id>">
    //   <span class="btn btn-xs btn-danger">Cancel Registration</span></a>
    // Opens an AJAX modal titled "Cancel Registration" with a pre-filled
    // Child ID field + Reason textarea (placeholder "Enter your comments
    // here") + a "Cancel Registration" submit button. Submitting fires a
    // NATIVE confirm() — "Are you sure you want to cancel registration?"
    // — then shows an inline green success banner "Cancelling
    // Registration Processed" in the same modal. After the page reflects
    // the change: child status label becomes "(ATTRITION)", the button
    // is replaced by "REFUND WELCOME KIT", a "Billing Cancel Date : <date>"
    // line appears under both subscription panels, and at least one
    // ledger row gains "(Voided on <date>)".
    // Field/modal locators below are first-pass, built from screenshots
    // only (not yet confirmed against live DOM) — refine on first live
    // run, same as every other feature in this project.
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//a[contains(@href,'cancel_registration')]")
    private WebElement cancelRegistrationLink;

    @FindBy(id = "cancel_reason")
    private WebElement cancelRegistrationReasonInput;

    @FindBy(id = "cancel_registration")
    private WebElement cancelRegistrationSubmitBtn;

    @FindBy(xpath = "//*[contains(normalize-space(.),'Cancelling Registration Processed')]")
    private WebElement cancelRegistrationSuccessBanner;

    @FindBy(xpath = "//a[normalize-space(.)='REFUND WELCOME KIT']" +
            " | //span[normalize-space(.)='REFUND WELCOME KIT']")
    private WebElement refundWelcomeKitBtn;


    public boolean isCancelRegistrationButtonVisible() {
        try {
            return cancelRegistrationLink.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickCancelRegistration() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(cancelRegistrationLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", cancelRegistrationLink);
        System.out.println("▶ Cancel Registration clicked");
        Thread.sleep(1200);
    }

    public boolean isCancelRegistrationModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(cancelRegistrationReasonInput));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CANCEL REGISTRATION — DEFAULT VIEW (SC_011_TC_002)
    // Consolidated single check covering: title, close icon,
    // Child ID field, Reason field, submit button.
    // ═══════════════════════════════════════════════
    public boolean isCancelRegistrationDefaultViewCorrect() {
        try {
            boolean titleVisible = !driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.),'Cancel Registration')]")).isEmpty();
            boolean closeIconVisible = !driver.findElements(By.xpath(
                    "//button[contains(@class,'close')]" +
                            " | //span[@aria-hidden='true' and (text()='×' or text()='x')]" +
                            " | //i[contains(@class,'fa-times')]" +
                            " | //i[contains(@class,'fa-remove')]")).isEmpty();
            boolean childIdFieldVisible = !driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.),'Child ID')]")).isEmpty();
            boolean reasonFieldVisible = cancelRegistrationReasonInput.isDisplayed();
            boolean submitBtnVisible = cancelRegistrationSubmitBtn.isDisplayed();
            boolean allVisible = titleVisible && closeIconVisible && childIdFieldVisible
                    && reasonFieldVisible && submitBtnVisible;
            System.out.println("✅ Cancel Registration default view — title:" + titleVisible
                    + " closeIcon:" + closeIconVisible + " childIdField:" + childIdFieldVisible
                    + " reasonField:" + reasonFieldVisible + " submitBtn:" + submitBtnVisible);
            return allVisible;
        } catch (Exception e) {
            System.out.println("⚠ isCancelRegistrationDefaultViewCorrect: " + e.getMessage());
            return false;
        }
    }

    public void enterCancelRegistrationReason(String reason) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(cancelRegistrationReasonInput));
        cancelRegistrationReasonInput.clear();
        cancelRegistrationReasonInput.sendKeys(reason);
        System.out.println("✅ Cancel Registration reason entered: " + reason);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // SUBMIT CANCEL REGISTRATION
    // Accepts the native confirm() ("Are you sure you want to
    // cancel registration?") and returns its text.
    // ═══════════════════════════════════════════════
    public String submitCancelRegistration() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(cancelRegistrationSubmitBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", cancelRegistrationSubmitBtn);
        System.out.println("▶ Cancel Registration submit clicked");
        Thread.sleep(800);

        String alertText = "";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            org.openqa.selenium.Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            System.out.println("▶ Confirm popup: " + alertText);
            alert.accept();
            System.out.println("✅ Alert accepted — cancel registration submitted");
            Thread.sleep(1500);
        } catch (Exception e) {
            System.out.println("⚠ No confirm alert after Cancel Registration submit: " + e.getMessage());
        }
        return alertText;
    }

    public String getCancelRegistrationSuccessMessage() {
        try {
            WebElement banner = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(cancelRegistrationSuccessBanner));
            String msg = banner.getText().trim();
            System.out.println("✅ Cancel Registration success message: " + msg);
            return msg;
        } catch (Exception e) {
            System.out.println("⚠ getCancelRegistrationSuccessMessage: " + e.getMessage());
            return "";
        }
    }

    public boolean isRefundWelcomeKitButtonVisible() {
        try {
            return refundWelcomeKitBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CHILD STATUS LABEL — reads the parenthesised status next to
    // the child's name in the header line, e.g. "#73014 AADISHREE
    // YADAV   (ATTRITION)   [PAYMENT PLAN : V1]". A positional
    // xpath to the containing <legend> proved fragile (differs by
    // child/page state) — scans the full body text instead, same
    // fallback style already used by getMonthlyPlanAmount().
    // ═══════════════════════════════════════════════
    public String getChildStatusLabel() {
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "#\\d+\\s+[^()\\n]*\\(([^)]+)\\)\\s*\\[PAYMENT PLAN").matcher(bodyText);
            if (m.find()) {
                String status = m.group(1).trim();
                System.out.println("✅ Child status label: " + status);
                return status;
            }
            System.out.println("⚠ getChildStatusLabel: header line not found in body text");
        } catch (Exception e) {
            System.out.println("⚠ getChildStatusLabel: " + e.getMessage());
        }
        return "";
    }

    public boolean isChildStatusAttrition() {
        return getChildStatusLabel().toUpperCase().contains("ATTRITION");
    }

    // ═══════════════════════════════════════════════
    // BILLING CANCEL DATE — "Billing Cancel Date : 09 Sep, 2026" is
    // rendered as a label element plus a sibling text node, so an
    // xpath text() match only ever returns the label itself (same
    // trap documented for AdmissionMigrationRequest's banner text).
    // Scans the body text instead.
    // ═══════════════════════════════════════════════
    public String getBillingCancelDateText() {
        try {
            String bodyText = driver.findElement(By.tagName("body")).getText();
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "Billing Cancel Date\\s*:?\\s*([^\\n]+)").matcher(bodyText);
            if (m.find()) {
                String date = m.group(1).trim();
                System.out.println("✅ Billing Cancel Date: " + date);
                return date;
            }
            System.out.println("⚠ getBillingCancelDateText: not found in body text");
        } catch (Exception e) {
            System.out.println("⚠ getBillingCancelDateText: " + e.getMessage());
        }
        return "";
    }

    // ═══════════════════════════════════════════════
    // VOIDED INVOICE REFERENCES — scans the ledger table for
    // any row whose text contains "Voided on" and reads that
    // row's first cell (the Reference column), e.g. "PI/985388".
    // ═══════════════════════════════════════════════
    public List<String> getVoidedInvoiceReferences() {
        List<String> refs = new java.util.ArrayList<>();
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(500);
            List<WebElement> voidedRows = driver.findElements(By.xpath(
                    "//tr[.//*[contains(normalize-space(.),'Voided on')]]"));
            for (WebElement row : voidedRows) {
                try {
                    String ref = row.findElement(By.xpath("./td[1]")).getText().trim();
                    if (!ref.isEmpty()) {
                        refs.add(ref);
                    }
                } catch (Exception ignored) {
                }
            }
            System.out.println("✅ Voided invoice references: " + refs);
        } catch (Exception e) {
            System.out.println("⚠ getVoidedInvoiceReferences: " + e.getMessage());
        }
        return refs;
    }

    // ═══════════════════════════════════════════════
    // EXTENDED DAYCARE EARLY STOP CREDIT — additive, for the new Stop /
    // Early Resume enhancement's "already-paid invoice" settlement path
    // (a credit note is raised instead of voiding the original invoice).
    // Confirmed live text pattern: "Credits - Extended DayCare |
    // Extended Daycare Early Stop Credit, Period - <start> To <end>".
    // ═══════════════════════════════════════════════
    public String getExtendedDaycareEarlyStopCreditText() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(500);
            // Confirmed live: this ledger entry is a plain <div> stack, NOT a <tr> —
            // e.g. <div style="border-left:5px solid #FF5722;">Credit issued on ...
            // <div class="row">...Extended Daycare Early Stop Credit...</div></div>.
            // Find the innermost element carrying the marker text, then walk up to
            // the outer "Credit issued on" wrapper div for the full description.
            WebElement innermost = driver.findElement(By.xpath(
                    "//*[contains(normalize-space(.),'Extended Daycare Early Stop Credit')]" +
                            "[not(.//*[contains(normalize-space(.),'Extended Daycare Early Stop Credit')])]"));
            WebElement rowDiv;
            try {
                rowDiv = innermost.findElement(By.xpath("./ancestor::div[contains(.,'Credit issued on')][1]"));
            } catch (Exception e) {
                rowDiv = innermost; // fall back to whatever we found if the wrapper isn't there
            }
            String text = rowDiv.getText().trim();
            System.out.println("✅ Extended Daycare Early Stop Credit row: " + text);
            return text;
        } catch (Exception e) {
            System.out.println("▶ No Extended Daycare Early Stop Credit row found: " + e.getMessage());
            return "";
        }
    }

    public boolean isExtendedDaycareEarlyStopCreditVisible() {
        return !getExtendedDaycareEarlyStopCreditText().isEmpty();
    }

    /**
     * Counts how many distinct "Extended Daycare Early Stop Credit" ledger
     * entries exist for the currently-generated Account Statement — used to
     * confirm a duplicate/no-op Early Resume submission does NOT create a
     * second credit entry (AC #7).
     */
    public int countExtendedDaycareEarlyStopCreditEntries() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
            Thread.sleep(500);
            List<WebElement> matches = driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.),'Extended Daycare Early Stop Credit')]" +
                            "[not(.//*[contains(normalize-space(.),'Extended Daycare Early Stop Credit')])]"));
            System.out.println("✅ Extended Daycare Early Stop Credit entry count: " + matches.size());
            return matches.size();
        } catch (Exception e) {
            System.out.println("⚠ countExtendedDaycareEarlyStopCreditEntries: " + e.getMessage());
            return -1;
        }
    }

    // ═══════════════════════════════════════════════
    // REFUND LIST — SC_011_TC_004 (UI-verify only, no exact
    // refund-amount math). Checks that a given invoice/reference
    // text appears somewhere on the refund_list screen.
    // ═══════════════════════════════════════════════
    public boolean isReferenceVisibleOnRefundList(String reference) {
        try {
            return !driver.findElements(By.xpath(
                    "//*[contains(normalize-space(.),'" + reference + "')]")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void closeCancelRegistrationModal() throws InterruptedException {
        closeModalByJs();
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
