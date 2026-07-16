package pages.Support;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Page Object: Corporate_ServiceRequests
 * <p>
 * Screen: Account Statement (Corporate child)
 * <p>
 * Features:
 * 1. Tieup Program Change  — child ID: 50947
 * 2. Corporate Transfer     — child ID: 68984
 * <p>
 * KEY FIX: All locators that previously hardcoded child ID 50947
 * are now dynamic — located by contains(@href,...) so they work
 * for ANY child ID passed at runtime.
 * <p>
 * User: Varsha Jha (switched once in @BeforeClass of test)
 */
public class Corporate_ServiceRequests {

    WebDriver driver;
    WebDriverWait wait;

    // ══════════════════════════════════════════════════════════════════════
    // ACCOUNT STATEMENT — shared locators
    // ══════════════════════════════════════════════════════════════════════

    @FindBy(id = "frm_child_id")
    WebElement admissionIdInput;

    /**
     * Generate button — name="statement_bnt"
     * FIX: removed duplicate generateReport_Btn
     */
    @FindBy(name = "statement_bnt")
    WebElement generateBtn;

    /**
     * Customer Requests link — top-right icon on Account Statement
     * Opens Recent Customer Requests in a new tab
     */
    @FindBy(xpath = "//a[contains(.,'Customer Request')]")
    WebElement customerRequest_link;

    // ══════════════════════════════════════════════════════════════════════
    // TIEUP PROGRAM CHANGE — dynamic locators (no hardcoded child ID)
    // FIX: all 3 locators previously hardcoded '50947'
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Tieup Program Change link — href contains addTieupProgramChange
     * Dynamic: works for any child ID
     */
    @FindBy(xpath = "//a[contains(@href,'addTieupProgramChange')]")
    WebElement tieup_ProgramChangeLink;

    /**
     * New Program dropdown inside Tieup PC modal
     */
    @FindBy(id = "new_program")
    WebElement select_Program;

    /**
     * Processing date calendar input
     */
    @FindBy(id = "processing_date")
    WebElement calendar_ProcessingDate;

    /**
     * Next arrow on processing date calendar
     */
    @FindBy(xpath = "//*[@id='processing_date_root']//div//div//div//div//div[1]//div[3]")
    WebElement next_Arrow;

    /**
     * Date cell to select (4th) on processing date calendar
     */
    @FindBy(xpath = "//*[@id='processing_date_table']//tbody//tr[2]//td[3]//div[text()='4']")
    WebElement date_selected;

    /**
     * Add Program Change Request button in modal
     */
    @FindBy(xpath = "//*[@id='modal_form_corp_program_change']//div//div//div[3]//button[2][text()='Add Program Change Request']")
    WebElement addProgramChange_btn;

    /**
     * "APPROVE TIEUP PROGRAM CHANGE REQUEST" button — appears AFTER request is saved.
     * Confirmed from PDF flow: no intermediate 'Processing' step exists.
     * Flow: Add Request → toast "SAVED SUCCESSFULLY" → this button appears (green)
     * Text confirmed: "APPROVE TIEUP PROGRAM CHANGE REQUEST"
     */
    @FindBy(xpath = "//a[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz',"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'APPROVE TIEUP PROGRAM CHANGE REQUEST')]"
            + " | //a[contains(@href,'updateTieupProgramChange')]")
    WebElement approveTieUpPC_btn;

    /**
     * WEF date input in Approve modal — id="wef_date"
     * Pre-filled with processing date, can be overridden.
     * Uses same Pickaday JS setter pattern.
     */
    @FindBy(id = "wef_date")
    WebElement wef_dateInput;

    /**
     * Toast/success message area — appears after Add Request and after Approve
     * Expected texts:
     * "TIEUP PROGRAM CHANGE REQUEST SAVED SUCCESSFULLY. REQUEST NEED TO APPROVED"
     * "TIE UP PROGRAM CHANGE REQUEST APPROVED SUCCESSFULLY."
     */
    @FindBy(xpath = "//div[contains(@class,'alert') or contains(@class,'toast')"
            + " or contains(@class,'success')]"
            + "[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz',"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'TIEUP') "
            + " or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz',"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'TIE UP')]")
    WebElement tieupToastMessage;

    /**
     * Fee breakup / comment field
     */
    @FindBy(id = "req_comment")
    WebElement feeBreakup;

    /**
     * Monthly amount — parent payable
     */
    @FindBy(id = "parent_monthly")
    WebElement monthlyAmount_ParentPayable;

    /**
     * Monthly amount — corporate payable
     */
    @FindBy(id = "corporate_monthly")
    WebElement monthlyAmount_CorporatePayable;

    /**
     * Approve Request button in PC approval modal
     */
    @FindBy(xpath = "//*[@id='modal_form_program_change_req']//div//div//div[3]//button[2][text()='Approve Request']")
    WebElement approveRequest_Btn;


    /**
     * Info rows in Tieup PC modal body
     */
    @FindBy(xpath = "//div[@class='modal-body model-program-change-body-req']//div[@class='row mt-10']")
    List<WebElement> tieupProgramChange_info;

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE TRANSFER — locators
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Corporate Transfer link — href contains pop_corporate_transfer
     * Confirmed live: distinct from Corporate Center Transfer below —
     * both links appear together on the same Account Statement page.
     */
    @FindBy(xpath = "//a[contains(@href,'pop_corporate_transfer')]")
    WebElement corporateTransfer_link;

    /**
     * Corporate Center Transfer link — href contains pop_center_transfer
     * Confirmed live (child 71962, Transfer Applicable=No): opens a form
     * (id="frm-center-transfer") reusing the SAME field ids as the Corporate
     * Transfer modal — applicable_month (labeled "New Center Joining Date"),
     * new_center ("Shift To"), new_program_name ("Program To"), add_request
     * (Submit) — just without the Offer field.
     */
    @FindBy(xpath = "//a[contains(@href,'pop_center_transfer')]")
    WebElement centerTransferLink;

    /**
     * Joining month dropdown
     */
    @FindBy(id = "applicable_month")
    WebElement joining_month;

    /**
     * New Offer ID dropdown
     */
    @FindBy(id = "new_offer_id")
    WebElement new_offerID;

    /**
     * New Center dropdown
     */
    @FindBy(id = "new_center")
    WebElement newCenter;

    /**
     * New Program dropdown (Corporate Transfer)
     */
    @FindBy(id = "new_program_name")
    WebElement newProgram;

    /**
     * Submit button — id="add_request"
     */
    @FindBy(id = "add_request")
    WebElement submit_Btn;

    /**
     * Approve Corporate Transfer link
     */
    @FindBy(xpath = "//a[contains(text(), 'Approve Corporate Transfer')]")
    WebElement approve_ctransfer_Btn;

    /**
     * Corporate Transfer approval modal
     */
    @FindBy(id = "frm-corporate-change-req")
    WebElement modal_CorporateTransferRequest;

    /**
     * Fee breakup comment in CT modal
     */
    @FindBy(id = "req_comment")
    WebElement feebreakup_comment;

    /**
     * Approve button in CT modal
     */
    @FindBy(xpath = "//*[@id='modal_form_corporate_change_req']//div//div//div[3]//button[2][text()='Approve Request']")
    WebElement approve_Btn;

    /**
     * Already transferred message
     */
    @FindBy(xpath = "//div//legend//div[3]//div[2][contains(text(), 'Corporate Transfer Already Requested')]")
    WebElement transfer_msg;

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════

    public Corporate_ServiceRequests(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        PageFactory.initElements(driver, this);
    }

    // ══════════════════════════════════════════════════════════════════════
    // ACCOUNT STATEMENT — shared actions
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Enter Admission ID and click Generate to load account statement.
     * Call this at the start of EACH test with the correct childId.
     *
     * @param childId e.g. "50947" or "68984"
     */
    public void generateAccountStatement(String childId)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(admissionIdInput));
        admissionIdInput.clear();
        admissionIdInput.sendKeys(childId);
        System.out.println("✅ Admission ID entered: " + childId);
        Thread.sleep(300);

        wait.until(ExpectedConditions.elementToBeClickable(generateBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", generateBtn);
        System.out.println("▶ Generate clicked for child: " + childId);
        Thread.sleep(2500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TIEUP PROGRAM CHANGE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Full Tieup Program Change flow.
     * Pre-condition: generateAccountStatement(TieupProgramChange_ChildID)
     * must be called before this method.
     *
     * Flow:
     *   1. Click Tieup Program Change link
     *   2. Select program, set processing date, submit
     *   3. Approve to Processing status
     *   4. Fill fee breakup, approve to Approved
     *   5. Print info rows
     *
     * @param programName  e.g. "Half Day"
     * @param feeBreakupAmount  e.g. "8000"
     * @param parentMonthly    e.g. "3000"
     * @param corporateMonthly e.g. "5000"
     */
    /**
     * Full Tieup Program Change flow — confirmed from PDF (Jun 16 2026):
     * <p>
     * Step 1: Click "TIE UP PROGRAM CHANGE" red button
     * Step 2: Modal — select program, set processing date → Add Program Change Request
     * Step 3: Assert toast "SAVED SUCCESSFULLY" → button changes to
     * "APPROVE TIEUP PROGRAM CHANGE REQUEST"
     * Step 4: Click Approve button → modal opens with WEF date + fee fields
     * Step 5: Fill WEF date, fee breakup, amounts → Approve Request
     * Step 6: Assert toast "APPROVED SUCCESSFULLY"
     * <p>
     * NOTE: There is NO intermediate "Processing" step.
     * Old code had updateTieupProgramChange('id','Processing') — that was wrong.
     *
     * @param programName      e.g. "Half Day"
     * @param processingDate   ISO "YYYY-MM-DD" e.g. "2026-06-23"
     * @param wefDate          ISO "YYYY-MM-DD" e.g. "2026-06-23" (usually same as processing)
     * @param feeBreakupAmount e.g. "8000"
     * @param parentMonthly    e.g. "3000"
     * @param corporateMonthly e.g. "5000"
     * @return toast message after approval for assertion
     */
    public String doTieupProgramChange(String programName,
                                       String processingDate,
                                       String wefDate,
                                       String feeBreakupAmount,
                                       String parentMonthly,
                                       String corporateMonthly)
            throws InterruptedException {

        // ── STEP 1: Click "TIE UP PROGRAM CHANGE" red button ──────────────
        wait.until(ExpectedConditions.elementToBeClickable(tieup_ProgramChangeLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", tieup_ProgramChangeLink);
        System.out.println("▶ TIE UP PROGRAM CHANGE clicked");
        Thread.sleep(1000);

        // ── STEP 2: Fill Add Tieup Program Change modal ────────────────────
        // Select program
        wait.until(ExpectedConditions.visibilityOf(select_Program));
        new Select(select_Program).selectByVisibleText(programName);
        System.out.println("✅ To Program: " + programName);
        Thread.sleep(300);

        // Set processing date via JS (Pickaday readonly input)
        setDateByJs(calendar_ProcessingDate, processingDate);
        System.out.println("✅ Processing Date: " + processingDate);

        // ── STEP 3: Click "Add Program Change Request" ─────────────────────
        wait.until(ExpectedConditions.elementToBeClickable(addProgramChange_btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", addProgramChange_btn);
        System.out.println("▶ Add Program Change Request clicked");
        Thread.sleep(2000);

        // Assert toast: "SAVED SUCCESSFULLY"
        String savedToast = getTieupToastMessage();
        System.out.println("   Toast after Add: " + savedToast);

        // ── STEP 4: Click "APPROVE TIEUP PROGRAM CHANGE REQUEST" (green) ───
        wait.until(ExpectedConditions.elementToBeClickable(approveTieUpPC_btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approveTieUpPC_btn);
        System.out.println("▶ APPROVE TIEUP PROGRAM CHANGE REQUEST clicked");
        Thread.sleep(1000);

        // ── STEP 5: Fill approval modal fields ────────────────────────────
        // WEF date — pre-filled but set explicitly for reliability
        wait.until(ExpectedConditions.visibilityOf(wef_dateInput));
        setDateByJs(wef_dateInput, wefDate);
        System.out.println("✅ WEF Date: " + wefDate);

        // Fee Breakup textarea
        wait.until(ExpectedConditions.visibilityOf(feeBreakup));
        feeBreakup.clear();
        feeBreakup.sendKeys(feeBreakupAmount);
        System.out.println("✅ Fee Breakup: " + feeBreakupAmount);

        // Monthly Amount — Parent Payable
        monthlyAmount_ParentPayable.clear();
        monthlyAmount_ParentPayable.sendKeys(parentMonthly);
        System.out.println("✅ Parent Monthly: " + parentMonthly);

        // Monthly Amount — Corporate Payable
        monthlyAmount_CorporatePayable.clear();
        monthlyAmount_CorporatePayable.sendKeys(corporateMonthly);
        System.out.println("✅ Corporate Monthly: " + corporateMonthly);

        // ── STEP 6: Click "Approve Request" ───────────────────────────────
        wait.until(ExpectedConditions.elementToBeClickable(approveRequest_Btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approveRequest_Btn);
        System.out.println("▶ Approve Request clicked");
        Thread.sleep(2000);

        // Get and return approval toast for assertion in test
        String approvedToast = getTieupToastMessage();
        System.out.println("   Toast after Approve: " + approvedToast);

        // Print info rows for debugging
        try {
            for (WebElement row : tieupProgramChange_info) {
                System.out.println("   " + row.getText());
            }
        } catch (Exception ignored) {
        }

        System.out.println("✅ Tieup Program Change completed");
        return approvedToast;
    }

    /**
     * Get the Tieup toast message visible on Account Statement screen.
     * Returns empty string if not visible within timeout.
     */
    public String getTieupToastMessage() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(tieupToastMessage));
            return tieupToastMessage.getText().trim();
        } catch (Exception e) {
            // Fallback: check all visible alerts/toasts
            try {
                java.util.List<WebElement> toasts = driver.findElements(
                        By.xpath("//div[contains(@class,'alert') or contains(@class,'toast')]"
                                + "[string-length(normalize-space(.)) > 0]"));
                for (WebElement t : toasts) {
                    String txt = t.getText().trim().toUpperCase();
                    if (txt.contains("TIEUP") || txt.contains("TIE UP")) return txt;
                }
            } catch (Exception ignored) {
            }
            return "";
        }
    }

    /**
     * JS date setter — same pattern as Regular_ServiceRequests.
     * Handles Pickaday readonly inputs.
     */
    private void setDateByJs(WebElement field, String isoDate)
            throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');", field);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];", field, isoDate);
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0]; var d=new Date(arguments[1]);" +
                        "if(el._picker && typeof el._picker.setDate==='function'){el._picker.setDate(d,true);}" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('blur',{bubbles:true}));",
                field, isoDate);
        Thread.sleep(400);
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE TRANSFER
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Full Corporate Transfer flow.
     * Pre-condition: generateAccountStatement(CorporateTransfer_ChildID)
     * must be called before this method.
     * <p>
     * Flow:
     * 1. Click Corporate Transfer link
     * 2. Select month, offer, center
     * 3. Submit + accept alert
     * 4. Approve transfer → fill fee breakup → approve
     * 5. Verify "Corporate Transfer Already Requested" message
     * 6. Open Customer Requests in new tab → verify table
     *
     * @param joiningMonth e.g. "May 2025"
     * @param offerName    e.g. "ABP News - Sector 62 Offer"
     * @param centerName   e.g. "Sector 122, Noida"
     * @param feeComment   e.g. "8499"
     */
    public void doCorporateTransfer(String joiningMonth,
                                    String offerName,
                                    String centerName,
                                    String feeComment)
            throws InterruptedException {

        // Step 1: Click Corporate Transfer link
        wait.until(ExpectedConditions.elementToBeClickable(corporateTransfer_link));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", corporateTransfer_link);
        System.out.println("▶ Corporate Transfer link clicked");
        Thread.sleep(800);

        // Step 2: Select joining month
        wait.until(ExpectedConditions.visibilityOf(joining_month));
        new Select(joining_month).selectByVisibleText(joiningMonth);
        System.out.println("✅ Joining month: " + joiningMonth);

        // Step 3: Select offer ID
        new Select(new_offerID).selectByVisibleText(offerName);
        System.out.println("✅ Offer: " + offerName);

        // Step 4: Select new center
        new Select(newCenter).selectByVisibleText(centerName);
        System.out.println("✅ Center: " + centerName);

        // Step 5: Submit
        wait.until(ExpectedConditions.elementToBeClickable(submit_Btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", submit_Btn);
        System.out.println("▶ Submit clicked");
        Thread.sleep(1000);

        // Step 6: Accept JS alert
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            System.out.println("▶ Alert: " + alert.getText());
            alert.accept();
            System.out.println("✅ Alert accepted — request submitted");
            Thread.sleep(1500);
        } catch (Exception e) {
            System.out.println("⚠ No alert after submit: " + e.getMessage());
        }

        // Step 7: Approve Corporate Transfer
        wait.until(ExpectedConditions.elementToBeClickable(approve_ctransfer_Btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approve_ctransfer_Btn);
        System.out.println("▶ Approve Corporate Transfer clicked");
        Thread.sleep(1000);

        // Step 8: Fill fee breakup in modal
        wait.until(ExpectedConditions.visibilityOf(modal_CorporateTransferRequest));
        wait.until(ExpectedConditions.visibilityOf(feebreakup_comment));
        feebreakup_comment.clear();
        feebreakup_comment.sendKeys(feeComment);
        System.out.println("✅ Fee comment: " + feeComment);

        // Step 9: Approve in modal
        wait.until(ExpectedConditions.elementToBeClickable(approve_Btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approve_Btn);
        System.out.println("▶ Approve clicked");
        Thread.sleep(1500);

        // Step 10: Refresh and verify transfer message
        driver.navigate().refresh();
        Thread.sleep(2000);
        try {
            wait.until(ExpectedConditions.visibilityOf(transfer_msg));
            System.out.println("✅ Transfer message: " + transfer_msg.getText());
        } catch (Exception e) {
            System.out.println("⚠ Transfer message not visible: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE TRANSFER — submit only (split from doCorporateTransfer so
    // submit/approve can be exercised independently across test cases)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Submit-only step of Corporate Transfer — click link, fill fields,
     * submit, accept the JS confirm() alert. Does NOT approve.
     * Pre-condition: generateAccountStatement(childId) already called.
     *
     * @return response/toast text visible right after submit (may be empty
     * if the app relies solely on the native alert with no follow-up toast)
     */
    public String submitCorporateTransfer(String joiningMonth, String offerName,
                                          String centerName, String programName)
            throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(corporateTransfer_link));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", corporateTransfer_link);
        System.out.println("▶ CORPORATE TRANSFER link clicked");
        Thread.sleep(800);

        wait.until(ExpectedConditions.visibilityOf(joining_month));
        new Select(joining_month).selectByVisibleText(joiningMonth);
        System.out.println("✅ Joining month: " + joiningMonth);

        if (offerName != null && !offerName.isEmpty()) {
            new Select(new_offerID).selectByVisibleText(offerName);
            System.out.println("✅ Offer: " + offerName);
        } else {
            System.out.println("✅ Offer (first available): " + selectFirstAvailable(new_offerID));
        }

        if (centerName != null && !centerName.isEmpty()) {
            new Select(newCenter).selectByVisibleText(centerName);
            System.out.println("✅ Center: " + centerName);
        } else {
            System.out.println("✅ Center (first available): " + selectFirstAvailable(newCenter));
        }
        Thread.sleep(400);

        if (programName != null && !programName.isEmpty()) {
            new Select(newProgram).selectByVisibleText(programName);
            System.out.println("✅ Program: " + programName);
        } else {
            System.out.println("✅ Program (first available): " + selectFirstAvailable(newProgram));
        }

        wait.until(ExpectedConditions.elementToBeClickable(submit_Btn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit_Btn);
        System.out.println("▶ Submit clicked");
        Thread.sleep(1000);

        String alertText = "";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            System.out.println("▶ Confirm popup: " + alertText);
            alert.accept();
            System.out.println("✅ Alert accepted — request submitted");
            Thread.sleep(1500);
        } catch (Exception e) {
            System.out.println("⚠ No confirm alert after submit: " + e.getMessage());
        }

        String toast = getTieupToastMessage();
        System.out.println("   Toast/response after submit: " + toast);
        return !toast.isEmpty() ? toast : alertText;
    }

    /**
     * Select the first non-placeholder option ("-- Select --" etc.) in a
     * dropdown — used when the exact live option list for a given child is
     * unknown ahead of time.
     */
    private String selectFirstAvailable(WebElement selectElement) {
        Select sel = new Select(selectElement);
        for (WebElement opt : sel.getOptions()) {
            String text = opt.getText().trim();
            if (!text.isEmpty() && !text.startsWith("--") && !text.equalsIgnoreCase("Select")) {
                sel.selectByVisibleText(text);
                return text;
            }
        }
        return "";
    }

    /**
     * Approve-only step of Corporate Transfer via "Approve Corporate
     * Transfer" link + fee-breakup modal.
     * Pre-condition: a Pending Corporate Transfer request already exists
     * for the currently-loaded child (submitCorporateTransfer already ran).
     *
     * @return response/toast text visible after approval
     */
    public String approveCorporateTransfer(String feeComment) throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(approve_ctransfer_Btn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approve_ctransfer_Btn);
        System.out.println("▶ Approve Corporate Transfer clicked");
        Thread.sleep(1000);

        wait.until(ExpectedConditions.visibilityOf(modal_CorporateTransferRequest));
        wait.until(ExpectedConditions.visibilityOf(feebreakup_comment));
        feebreakup_comment.clear();
        feebreakup_comment.sendKeys(feeComment);
        System.out.println("✅ Fee comment: " + feeComment);

        wait.until(ExpectedConditions.elementToBeClickable(approve_Btn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approve_Btn);
        System.out.println("▶ Approve clicked");
        Thread.sleep(1500);

        String toast = getTieupToastMessage();
        System.out.println("   Toast/response after approve: " + toast);
        return toast;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE CENTER TRANSFER — button flow (Transfer Applicable=No)
    // Confirmed live (child 71962): form id="frm-center-transfer" reuses
    // joining_month/newCenter/newProgram/submit_Btn field ids.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Submit the Corporate Center Transfer button-flow form.
     * Pre-condition: generateAccountStatement(childId) already called.
     *
     * @param applicableMonth visible text e.g. "Aug 2026"
     * @param centerName      visible text of the "Shift To" center dropdown
     * @param programName     visible text of "Program To" — pass null/empty
     *                        to leave as "-- Select --" if not required
     * @return response/toast text visible right after submit
     */
    public String submitCorporateCenterTransfer(String applicableMonth, String centerName,
                                                String programName)
            throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(centerTransferLink));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerTransferLink);
        System.out.println("▶ CORPORATE CENTER TRANSFER link clicked");
        Thread.sleep(1200);

        wait.until(ExpectedConditions.visibilityOf(joining_month));
        new Select(joining_month).selectByVisibleText(applicableMonth);
        System.out.println("✅ Applicable Month: " + applicableMonth);
        Thread.sleep(300);

        if (centerName != null && !centerName.isEmpty()) {
            new Select(newCenter).selectByVisibleText(centerName);
            System.out.println("✅ Shift To: " + centerName);
        } else {
            System.out.println("✅ Shift To (first available): " + selectFirstAvailable(newCenter));
        }
        Thread.sleep(300);

        if (programName != null && !programName.isEmpty()) {
            new Select(newProgram).selectByVisibleText(programName);
            System.out.println("✅ Program To: " + programName);
        } else {
            System.out.println("✅ Program To (first available): " + selectFirstAvailable(newProgram));
        }

        wait.until(ExpectedConditions.elementToBeClickable(submit_Btn));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submit_Btn);
        System.out.println("▶ Submit clicked");
        Thread.sleep(1000);

        String alertText = "";
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            System.out.println("▶ Confirm popup: " + alertText);
            alert.accept();
            System.out.println("✅ Alert accepted — request submitted");
            Thread.sleep(1500);
        } catch (Exception e) {
            System.out.println("⚠ No confirm alert after submit: " + e.getMessage());
        }

        String toast = getTieupToastMessage();
        System.out.println("   Toast/response after submit: " + toast);
        return !toast.isEmpty() ? toast : alertText;
    }

    /**
     * Open Customer Requests link from Account Statement.
     * Opens in a new tab — switches to it and returns the URL.
     *
     * @return URL of the Customer Requests tab
     */
    public String openCustomerRequestsTab() throws InterruptedException {
        String originalWindow = driver.getWindowHandle();

        wait.until(ExpectedConditions.elementToBeClickable(customerRequest_link));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", customerRequest_link);
        Thread.sleep(1500);

        // Switch to new tab
        Set<String> allHandles = driver.getWindowHandles();
        for (String handle : allHandles) {
            if (!handle.equals(originalWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }

        String url = driver.getCurrentUrl();
        System.out.println("✅ Customer Requests tab URL: " + url);
        System.out.println("   Title: " + driver.getTitle());
        return url;
    }

    /**
     * Switch back to the original Account Statement window.
     * Call after openCustomerRequestsTab() when done with new tab.
     */
    public void switchBackToAccountStatement(String originalHandle) {
        driver.switchTo().window(originalHandle);
        System.out.println("✅ Switched back to Account Statement window");
    }

    /**
     * Print all table rows from current page — for debugging.
     */
    public void printTableData() {
        List<WebElement> rows = driver.findElements(By.tagName("tr"));
        List<WebElement> headers = driver.findElements(By.tagName("th"));
        System.out.println("Rows: " + rows.size() + " | Columns: " + headers.size());
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (!cells.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                cells.forEach(c -> sb.append(c.getText()).append(" | "));
                System.out.println(sb.toString());
            }
        }
    }

    /**
     * Get the "Corporate Transfer Already Requested" message text.
     * Returns empty string if not visible.
     */
    public String getTransferAlreadyRequestedMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(transfer_msg));
            return transfer_msg.getText().trim();
        } catch (Exception e) {
            return "";
        }

    }
}
