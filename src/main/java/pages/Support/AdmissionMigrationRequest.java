package pages.Support;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Page Object: Admission Migration (Regular ↔ Corporate)
 * <p>
 * Screen: Account Statement — "MIGRATE FROM REGULAR TO CORPORATE" /
 * "MIGRATE FROM CORPORATE TO REGULAR" buttons open the same "Admission
 * Migration" modal, with a different field set per direction:
 * - Regular→Corporate Add modal: Corporate Tie Up, Center, Program,
 * Employee Code, Employee Email, Employer.
 * - Corporate→Regular Add modal: Center, Program only.
 * <p>
 * Both directions share the same underlying JS handlers observed live
 * (migrateAdmissionRegularCorporate(childId) / approveMigrateAdmissionRegularCorporate(childId)).
 * <p>
 * NOTE: modal field locators below are LABEL-PROXIMITY based (not ids) —
 * no confirmed DOM ids were available at write time (only screenshots of
 * the rendered form). This mirrors the label-proximity approach used
 * elsewhere in this codebase when ids are unknown; verify/tighten these
 * against the real DOM on first live run, same as every other feature's
 * "confirmed live" pass.
 */
public class AdmissionMigrationRequest {

    WebDriver driver;
    WebDriverWait wait;

    // ══════════════════════════════════════════════════════════════════════
    // ACCOUNT STATEMENT — shared locators (self-contained, mirrors
    // Corporate_ServiceRequests.java's own copy of these)
    // ══════════════════════════════════════════════════════════════════════

    @FindBy(id = "frm_child_id")
    WebElement admissionIdInput;

    @FindBy(name = "statement_bnt")
    WebElement generateBtn;

    // ══════════════════════════════════════════════════════════════════════
    // ENTRY BUTTONS — all-caps anchor text (confirmed live text, per
    // AccountStatementPage.java's existing visibility-only locators)
    // ══════════════════════════════════════════════════════════════════════

    private static final String UPPER_XLATE =
            "translate(.,'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";

    @FindBy(xpath = "//a[contains(" + UPPER_XLATE + ",'MIGRATE FROM REGULAR TO CORPORATE')]")
    WebElement migrateRegularToCorporateBtn;

    @FindBy(xpath = "//a[contains(" + UPPER_XLATE + ",'MIGRATE FROM CORPORATE TO REGULAR')]")
    WebElement migrateCorporateToRegularBtn;

    @FindBy(xpath = "//a[contains(" + UPPER_XLATE + ",'APPROVE MIGRATION FROM REGULAR TO CORPORATE')]")
    WebElement approveMigrationRegularToCorporateBtn;

    @FindBy(xpath = "//a[contains(" + UPPER_XLATE + ",'APPROVE MIGRATION FROM CORPORATE TO REGULAR')]")
    WebElement approveMigrationCorporateToRegularBtn;

    // ══════════════════════════════════════════════════════════════════════
    // "ADMISSION MIGRATION" MODAL — shared by Add + Approve, both directions
    // ══════════════════════════════════════════════════════════════════════

    // Confirmed live via dumpModalHtml() — modal id="modal_form_admission_migration"
    @FindBy(xpath = "//h5[contains(@class,'modal-title')][normalize-space(.)='Admission Migration']")
    WebElement modalHeader;

    @FindBy(id = "offer_id")
    WebElement corporateTieUpDropdown;

    @FindBy(id = "center_id")
    WebElement centerDropdown;

    @FindBy(id = "program")
    WebElement programDropdown;

    @FindBy(id = "employee_code")
    WebElement employeeCodeInput;

    @FindBy(id = "employee_email")
    WebElement employeeEmailInput;

    @FindBy(id = "employer")
    WebElement employerInput;

    @FindBy(xpath = "//button[normalize-space(.)='Add Migration Request']")
    WebElement addMigrationRequestBtn;

    @FindBy(xpath = "//button[normalize-space(.)='Approve Migration Request']")
    WebElement approveMigrationRequestBtn;

    @FindBy(xpath = "//button[normalize-space(.)='Close']")
    WebElement modalCloseBtn;

    // Inline confirmation text shown in the Approve modal itself (NOT a
    // native browser confirm) — e.g. "Are you sure you want to approve
    // Admission Migration?"
    @FindBy(xpath = "//*[contains(text(),'Are you sure you want to approve Admission Migration')]")
    WebElement approveInlineConfirmText;

    // Generic toast/alert region — mirrors Corporate_ServiceRequests'
    // tieupToastMessage pattern, keyed on "MIGRATION" text instead of "TIE UP".
    @FindBy(xpath = "//div[contains(@class,'alert') or contains(@class,'toast')"
            + " or contains(@class,'success')]"
            + "[contains(translate(normalize-space(.),"
            + "'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'MIGRATION')]")
    WebElement migrationToastMessage;

    // "Admission Migration Already Requested" — red text on Account Statement
    @FindBy(xpath = "//*[contains(text(),'Admission Migration Already Requested')]")
    WebElement alreadyRequestedText;

    // "TIE-UP : <name>" banner on a Corporate child's Account Statement
    // Confirmed live: "ACCOUNT STATEMENT (...)", "TRANSFER CASE FROM #<id>"
    // and "TIE-UP : <name>" can all be adjacent TEXT NODES within the SAME
    // <legend> element (no separate wrapping tag per line). XPath's
    // contains(text(),...) only inspects the FIRST direct text-node child,
    // so it silently misses later sibling text nodes — confirmed live via
    // a direct diagnostic dump. Read the whole legend and extract with a
    // Java regex instead of relying on a narrow XPath text() match.
    @FindBy(css = "legend")
    WebElement legendElement;

    // "Billing Cancel Date : <date>" red text shown after approval —
    // confirmed live this is NOT inside <legend>, and contains(text(),...)
    // does correctly match it (its own dedicated element, only text node).
    @FindBy(xpath = "//*[contains(text(),'Billing Cancel Date')]")
    WebElement billingCancelDateText;

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════

    public AdmissionMigrationRequest(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        PageFactory.initElements(driver, this);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACCOUNT STATEMENT NAVIGATION
    // ══════════════════════════════════════════════════════════════════════

    public void generateAccountStatement(String childId) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(admissionIdInput));
        admissionIdInput.clear();
        admissionIdInput.sendKeys(childId);
        System.out.println("✅ Admission ID entered: " + childId);
        Thread.sleep(300);

        wait.until(ExpectedConditions.elementToBeClickable(generateBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", generateBtn);
        System.out.println("▶ Generate clicked for child: " + childId);
        Thread.sleep(2500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ENTRY BUTTON VISIBILITY / CLICKS
    // ══════════════════════════════════════════════════════════════════════

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

    public void clickMigrateRegularToCorporate() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(migrateRegularToCorporateBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", migrateRegularToCorporateBtn);
        System.out.println("▶ MIGRATE FROM REGULAR TO CORPORATE clicked");
        Thread.sleep(800);
    }

    public void clickMigrateCorporateToRegular() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(migrateCorporateToRegularBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", migrateCorporateToRegularBtn);
        System.out.println("▶ MIGRATE FROM CORPORATE TO REGULAR clicked");
        Thread.sleep(800);
    }

    public boolean isApproveMigrationRegularToCorporateVisible() {
        try {
            return approveMigrationRegularToCorporateBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isApproveMigrationCorporateToRegularVisible() {
        try {
            return approveMigrationCorporateToRegularBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void clickApproveMigrationRegularToCorporate() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(approveMigrationRegularToCorporateBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approveMigrationRegularToCorporateBtn);
        System.out.println("▶ APPROVE MIGRATION FROM REGULAR TO CORPORATE clicked");
        Thread.sleep(800);
    }

    public void clickApproveMigrationCorporateToRegular() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(approveMigrationCorporateToRegularBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approveMigrationCorporateToRegularBtn);
        System.out.println("▶ APPROVE MIGRATION FROM CORPORATE TO REGULAR clicked");
        Thread.sleep(800);
    }

    // ══════════════════════════════════════════════════════════════════════
    // "ADMISSION MIGRATION" MODAL — Add Migration Request
    // ══════════════════════════════════════════════════════════════════════

    public boolean isModalVisible() {
        try {
            return modalHeader.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void selectCorporateTieUp(String tieUpName) {
        wait.until(ExpectedConditions.visibilityOf(corporateTieUpDropdown));
        new Select(corporateTieUpDropdown).selectByVisibleText(tieUpName);
        System.out.println("✅ Corporate Tie Up: " + tieUpName);
    }

    public void selectCenter(String centerName) {
        wait.until(ExpectedConditions.visibilityOf(centerDropdown));
        new Select(centerDropdown).selectByVisibleText(centerName);
        System.out.println("✅ Center: " + centerName);
    }

    public void selectProgram(String programName) {
        wait.until(ExpectedConditions.visibilityOf(programDropdown));
        new Select(programDropdown).selectByVisibleText(programName);
        System.out.println("✅ Program: " + programName);
    }

    /**
     * Used for the Corporate→Regular Add modal, where Center has no single
     * pre-filled option but a full list to choose from (confirmed live on
     * children 66969/66970 — unlike the usual single-preselected-option
     * Center dropdown seen elsewhere).
     */
    public String selectFirstAvailableCenter() {
        wait.until(ExpectedConditions.visibilityOf(centerDropdown));
        Select sel = new Select(centerDropdown);
        for (WebElement opt : sel.getOptions()) {
            String text = opt.getText().trim();
            if (!text.isEmpty() && !text.equalsIgnoreCase("Select")) {
                sel.selectByVisibleText(text);
                System.out.println("✅ Center (first available): " + text);
                return text;
            }
        }
        return "";
    }

    /**
     * Program options populate dynamically (via JS onchange) only after
     * Corporate Tie Up is selected — confirmed live, dropdown starts with
     * only a "Please Select" placeholder. Selects the first real option
     * once available; no-ops (returns empty string) if still empty after
     * waiting, since some tie-ups may leave Program unchanged/pre-set.
     */
    public String selectFirstAvailableProgram() throws InterruptedException {
        Thread.sleep(1000);
        Select sel = new Select(programDropdown);
        for (int attempt = 0; attempt < 5; attempt++) {
            for (WebElement opt : sel.getOptions()) {
                String text = opt.getText().trim();
                if (!text.isEmpty() && !text.equalsIgnoreCase("Please Select")) {
                    sel.selectByVisibleText(text);
                    System.out.println("✅ Program (first available): " + text);
                    return text;
                }
            }
            Thread.sleep(500);
        }
        System.out.println("⚠ Program dropdown still empty after waiting — leaving unset");
        return "";
    }

    public void enterEmployeeCode(String code) {
        wait.until(ExpectedConditions.visibilityOf(employeeCodeInput));
        employeeCodeInput.clear();
        employeeCodeInput.sendKeys(code);
        System.out.println("✅ Employee Code: " + code);
    }

    public void enterEmployeeEmail(String email) {
        wait.until(ExpectedConditions.visibilityOf(employeeEmailInput));
        employeeEmailInput.clear();
        employeeEmailInput.sendKeys(email);
        System.out.println("✅ Employee Email: " + email);
    }

    public void enterEmployer(String employer) {
        wait.until(ExpectedConditions.visibilityOf(employerInput));
        employerInput.clear();
        employerInput.sendKeys(employer);
        System.out.println("✅ Employer: " + employer);
    }

    /**
     * Click "Add Migration Request" and accept the native browser confirm()
     * ("Are you sure you want to submit the migration request?") — confirmed
     * live via screenshot as a genuine window.confirm(), not an inline modal
     * text (unlike the Approve step below).
     *
     * @return toast/response text visible right after submit
     */
    public String clickAddMigrationRequest() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(addMigrationRequestBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addMigrationRequestBtn);
        System.out.println("▶ Add Migration Request clicked");
        Thread.sleep(800);

        String alertText = "";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            System.out.println("▶ Confirm popup: " + alertText);
            alert.accept();
            System.out.println("✅ Alert accepted — migration request submitted");
            Thread.sleep(1500);
        } catch (Exception e) {
            System.out.println("⚠ No confirm alert after Add Migration Request: " + e.getMessage());
        }

        String toast = getMigrationToastMessage();
        System.out.println("   Toast/response after submit: " + toast);
        return !toast.isEmpty() ? toast : alertText;
    }

    /**
     * Click "Add Migration Request" and report whether submission was
     * blocked client-side (no native confirm() fired) — used for mandatory
     * field validation, where the browser's own HTML5 "required" tooltip is
     * not readable as DOM text via Selenium, but its blocking effect (no
     * confirm dialog, modal stays open) is directly observable.
     *
     * @return true if no confirm() appeared (submission blocked), false if
     * the confirm() fired (submission proceeded — fields were valid)
     */
    public boolean isAddMigrationRequestBlocked() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(addMigrationRequestBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", addMigrationRequestBtn);
        System.out.println("▶ Add Migration Request clicked (validation check)");
        Thread.sleep(800);

        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Confirm alert appeared — submission was NOT blocked");
            return false;
        } catch (Exception e) {
            System.out.println("✅ No confirm alert — submission blocked by validation");
            return true;
        }
    }

    /**
     * Click "Approve Migration Request". Per screenshots, the Approve modal's
     * "Are you sure...?" text is INLINE in the modal (not a native confirm),
     * so no alert-handling is needed here — just the click.
     *
     * @return toast/response text visible right after approval
     */
    public String clickApproveMigrationRequest() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(approveMigrationRequestBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approveMigrationRequestBtn);
        System.out.println("▶ Approve Migration Request clicked");
        Thread.sleep(1500);

        String toast = getMigrationToastMessage();
        System.out.println("   Toast/response after approve: " + toast);
        return toast;
    }

    public void closeModal() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(modalCloseBtn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", modalCloseBtn);
        System.out.println("▶ Modal closed");
        Thread.sleep(500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // MESSAGES / STATUS TEXT
    // ══════════════════════════════════════════════════════════════════════

    public String getMigrationToastMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(migrationToastMessage));
            return migrationToastMessage.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Reads any visible message on screen matching the given substring —
     * used for the various validation/blocking messages (dues clearance,
     * mandatory-field errors, duplicate/attrition-conflict toast, center
     * restriction) whose exact container isn't confirmed yet.
     */
    public String getVisibleMessageContaining(String substring) {
        try {
            WebElement el = driver.findElement(By.xpath(
                    "//*[contains(text(),'" + substring + "')]"));
            return el.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isAlreadyRequestedTextVisible() {
        try {
            return alreadyRequestedText.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    private String getLegendText() {
        try {
            wait.until(ExpectedConditions.visibilityOf(legendElement));
            return legendElement.getText();
        } catch (Exception e) {
            return "";
        }
    }

    public String getTieUpBannerText() {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("TIE-UP\\s*:[^\\n]*")
                .matcher(getLegendText());
        return m.find() ? m.group().trim() : "";
    }

    public String getTransferCaseBannerText() {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("TRANSFER CASE FROM #\\d+")
                .matcher(getLegendText());
        return m.find() ? m.group().trim() : "";
    }

    public String getBillingCancelDateText() {
        try {
            wait.until(ExpectedConditions.visibilityOf(billingCancelDateText));
            return billingCancelDateText.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // DIAGNOSTIC — dump the modal's real HTML so field locators above can be
    // corrected against the actual DOM on first live run (same "confirm
    // live" methodology used throughout this codebase for other features).
    // ══════════════════════════════════════════════════════════════════════

    public void dumpModalHtml() {
        try {
            List<WebElement> modals = driver.findElements(By.cssSelector(".modal.show, .modal.in, .modal[style*='display: block']"));
            List<String> htmls = new ArrayList<>();
            for (WebElement m : modals) {
                htmls.add(m.getAttribute("outerHTML"));
            }
            System.out.println("▶ Visible modal(s) HTML: " + htmls);
        } catch (Exception e) {
            System.out.println("⚠ Could not dump modal HTML: " + e.getMessage());
        }
    }
}
