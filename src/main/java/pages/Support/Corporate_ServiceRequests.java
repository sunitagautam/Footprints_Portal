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

    // Captures the effective-date rule warning text (if any) seen during
    // the most recent doTieupProgramChange() call — empty string if none.
    private String lastEffectiveDateWarning = "";

    public String getLastEffectiveDateWarning() {
        return lastEffectiveDateWarning;
    }

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
     * Effective-date rule warning banner inside the Add Tieup Program
     * Change modal (added 2026-09-11). Confirmed live text: "This request
     * must be approved before the processing date (<date>), or the
     * program change will not take effect." Empty/hidden when the chosen
     * processing date doesn't trigger the rule.
     */
    @FindBy(className = "program-change-error-msg")
    WebElement programChangeWarningBanner;

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
     * Flow: Add Request → toast "SAVED SUCCESSFULLY" → this button appears (blue)
     * Confirmed live 2026-09-11 (post dev-fix): for a Long Term tie-up
     * child (73462) this renders as a &lt;button&gt; with text "APPROVE
     * TIE UP PROGRAM CHANGE REQUEST" (space between TIE/UP). For a Short
     * Term tie-up child (67087) it instead renders as an &lt;a
     * href="javascript:updateTieupProgramChange('&lt;id&gt;','Processing')"&gt;
     * with mixed-case text "Approve Tie Up Program Change Request" — both
     * forms matched here (case-insensitive) since which one a given
     * child gets is not yet understood (Long Term vs Short Term tie-up,
     * or something else).
     */
    @FindBy(xpath = "//*[self::a or self::button]"
            + "[contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz',"
            + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'),'APPROVE TIE UP PROGRAM CHANGE REQUEST')"
            + " or contains(translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz',"
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
    /**
     * Submit the Corporate Center Transfer button-flow form.
     * Pre-condition: generateAccountStatement(childId) already called.
     *
     * @param applicableMonth visible text e.g. "Aug 2026"
     * @param centerName      visible text of the "Shift To" center dropdown
     * @param programName     visible text of "Program To" — pass null/empty
     * to leave as "-- Select --" if not required
     * @return response/toast text visible right after submit
     */
    // ✅ Set by submitCorporateCenterTransfer() — the actual Applicable
    // Month visible text used (whether passed explicitly or resolved via
    // "first available"), so callers can derive the request's WEF date
    // (1st of this month) for the migration API's required "date" param.
    private String lastSelectedApplicableMonth;

    // ══════════════════════════════════════════════════════════════════════
    // ACCOUNT STATEMENT — shared actions
    // ══════════════════════════════════════════════════════════════════════

    public Corporate_ServiceRequests(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        PageFactory.initElements(driver, this);
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
        // Select program — pass null to pick the first available option
        // when the exact live option list for a given child is unknown
        // ahead of time (same pattern as Corporate Transfer's null params).
        wait.until(ExpectedConditions.visibilityOf(select_Program));
        if (programName == null) {
            String picked = selectFirstAvailable(select_Program);
            System.out.println("✅ Program (first available): " + picked);
        } else {
            new Select(select_Program).selectByVisibleText(programName);
            System.out.println("✅ To Program: " + programName);
        }
        Thread.sleep(300);

        // Set processing date via JS (Pickaday readonly input)
        setDateByRealPicker(calendar_ProcessingDate, "processing_date_root", processingDate);
        System.out.println("✅ Processing Date: " + processingDate);

        // ── STEP 3: Click "Add Program Change Request" ─────────────────────
        wait.until(ExpectedConditions.elementToBeClickable(addProgramChange_btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", addProgramChange_btn);
        System.out.println("▶ Add Program Change Request clicked");
        acceptNativeConfirmIfPresent();
        Thread.sleep(2000);

        // ── STEP 3b: Effective-date rule warning — two-step confirm ────────
        // Confirmed live (2026-09-11): when the chosen processing date
        // triggers the new effective-date rule, the modal shows a warning
        // banner ("This request must be approved before the processing
        // date (<date>)...") and RESETS the "To Program" dropdown back to
        // "Please Select" instead of completing the Add step. Re-selecting
        // the program and clicking Add again is required to actually
        // proceed.
        lastEffectiveDateWarning = "";
        try {
            List<WebElement> banners = driver.findElements(By.className("program-change-error-msg"));
            String bannerText = "";
            for (WebElement b : banners) {
                if (b.isDisplayed() && !b.getText().trim().isEmpty()) {
                    bannerText = b.getText().trim();
                    break;
                }
            }
            if (!bannerText.isEmpty()) {
                lastEffectiveDateWarning = bannerText;
                System.out.println("ℹ Effective-date warning shown: " + lastEffectiveDateWarning);

                if (programName == null) {
                    String picked = selectFirstAvailable(select_Program);
                    System.out.println("✅ Re-selected program (first available): " + picked);
                } else {
                    new Select(select_Program).selectByVisibleText(programName);
                    System.out.println("✅ Re-selected program: " + programName);
                }
                Thread.sleep(300);

                wait.until(ExpectedConditions.elementToBeClickable(addProgramChange_btn));
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", addProgramChange_btn);
                System.out.println("▶ Add Program Change Request clicked again (post-warning confirm)");
                acceptNativeConfirmIfPresent();
                Thread.sleep(2000);
            } else {
                System.out.println("ℹ No effective-date warning banner found/visible after first Add click");
            }
        } catch (Exception e) {
            System.out.println("⚠ Warning-banner check threw: " + e.getMessage());
        }

        // Assert toast: "SAVED SUCCESSFULLY"
        String savedToast = getTieupToastMessage();
        System.out.println("   Toast after Add: " + savedToast);
        if (savedToast.isEmpty()) {
            System.out.println("   DIAG any alert/info after Add: [" + getAnyVisibleAlertOrInfoMessage() + "]");
            try {
                List<WebElement> modals = driver.findElements(By.cssSelector(".modal.show, .modal.in, .modal[style*='display: block']"));
                for (WebElement m : modals) {
                    System.out.println("   DIAG visible modal HTML: " + m.getAttribute("outerHTML"));
                }
            } catch (Exception e) {
                System.out.println("   DIAG modal dump failed: " + e.getMessage());
            }
        }

        // ── STEP 4: Click "APPROVE TIEUP PROGRAM CHANGE REQUEST" (green) ───
        // Defensive re-check: a slow-appearing confirm() from the previous
        // Add step can still be open at this point (confirmed live — the
        // 5s wait there sometimes wasn't enough), which would otherwise
        // throw "unexpected alert open" on the very next WebDriver call.
        acceptNativeConfirmIfPresent();
        wait.until(ExpectedConditions.elementToBeClickable(approveTieUpPC_btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approveTieUpPC_btn);
        System.out.println("▶ APPROVE TIEUP PROGRAM CHANGE REQUEST clicked");
        acceptNativeConfirmIfPresent();
        Thread.sleep(1000);
        logTieupProgramChangeInlineMessageIfPresent();

        // ── STEP 5: Fill approval modal fields ────────────────────────────
        // WEF date — pre-filled but set explicitly for reliability
        wait.until(ExpectedConditions.visibilityOf(wef_dateInput));
        setDateByRealPicker(wef_dateInput, "wef_date_root", wefDate);
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
        approvedToast = approvedToast.isEmpty() ? getApprovedStateFallback() : approvedToast;

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

    // ══════════════════════════════════════════════════════════════════════
    // ADD-ONLY (added 2026-09-11) — for AC #6 (Scenario 6): a request left
    // deliberately unapproved must NOT take effect once its processing date
    // arrives. Duplicates doTieupProgramChange()'s steps 1-3 (+3b warning
    // handling) without ever clicking Approve, matching this file's
    // established per-flow-duplication convention.
    // ══════════════════════════════════════════════════════════════════════
    public String addTieupProgramChangeOnly(String programName, String processingDate)
            throws InterruptedException {

        wait.until(ExpectedConditions.elementToBeClickable(tieup_ProgramChangeLink));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", tieup_ProgramChangeLink);
        System.out.println("▶ TIE UP PROGRAM CHANGE clicked");
        Thread.sleep(1000);

        wait.until(ExpectedConditions.visibilityOf(select_Program));
        if (programName == null) {
            String picked = selectFirstAvailable(select_Program);
            System.out.println("✅ Program (first available): " + picked);
        } else {
            new Select(select_Program).selectByVisibleText(programName);
            System.out.println("✅ To Program: " + programName);
        }
        Thread.sleep(300);

        setDateByRealPicker(calendar_ProcessingDate, "processing_date_root", processingDate);
        System.out.println("✅ Processing Date: " + processingDate);

        wait.until(ExpectedConditions.elementToBeClickable(addProgramChange_btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", addProgramChange_btn);
        System.out.println("▶ Add Program Change Request clicked");
        acceptNativeConfirmIfPresent();
        Thread.sleep(2000);

        lastEffectiveDateWarning = "";
        try {
            List<WebElement> banners = driver.findElements(By.className("program-change-error-msg"));
            String bannerText = "";
            for (WebElement b : banners) {
                if (b.isDisplayed() && !b.getText().trim().isEmpty()) {
                    bannerText = b.getText().trim();
                    break;
                }
            }
            if (!bannerText.isEmpty()) {
                lastEffectiveDateWarning = bannerText;
                System.out.println("ℹ Effective-date warning shown: " + lastEffectiveDateWarning);

                if (programName == null) {
                    String picked = selectFirstAvailable(select_Program);
                    System.out.println("✅ Re-selected program (first available): " + picked);
                } else {
                    new Select(select_Program).selectByVisibleText(programName);
                    System.out.println("✅ Re-selected program: " + programName);
                }
                Thread.sleep(300);

                wait.until(ExpectedConditions.elementToBeClickable(addProgramChange_btn));
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", addProgramChange_btn);
                System.out.println("▶ Add Program Change Request clicked again (post-warning confirm)");
                acceptNativeConfirmIfPresent();
                Thread.sleep(2000);
            } else {
                System.out.println("ℹ No effective-date warning banner found/visible after first Add click");
            }
        } catch (Exception e) {
            System.out.println("⚠ Warning-banner check threw: " + e.getMessage());
        }

        String savedToast = getTieupToastMessage();
        System.out.println("   Toast after Add (add-only): " + savedToast);
        System.out.println("✅ Tieup Program Change Add-only completed (left unapproved)");
        return savedToast;
    }

    // ══════════════════════════════════════════════════════════════════════
    // TOAST FALLBACK (added 2026-09-11): confirmed live on two separate
    // children (73462, 67087) that the Approve step's success toast comes
    // back empty via getTieupToastMessage() even though the request
    // genuinely reaches Approved status — the page's own action button
    // changes to "TIE UP PROGRAM CHANGE REQUEST(APPROVED)". Without this
    // fallback, a genuinely successful approval would still fail the
    // existing "APPROVED SUCCESSFULLY" toast assertion in TC001/callers.
    // Returns a string containing "APPROVED SUCCESSFULLY" (matching the
    // existing assertion) when the badge is found, else empty.
    // ══════════════════════════════════════════════════════════════════════
    private String getApprovedStateFallback() {
        try {
            Thread.sleep(1000);
            String bodyText = driver.findElement(By.tagName("body")).getText().toUpperCase();
            if (bodyText.contains("TIE UP PROGRAM CHANGE REQUEST(APPROVED)")
                    || bodyText.contains("TIEUP PROGRAM CHANGE REQUEST(APPROVED)")) {
                System.out.println("✅ Approval confirmed via page badge state (toast was empty)");
                return "TIEUP PROGRAM CHANGE REQUEST APPROVED SUCCESSFULLY (via page badge)";
            }
        } catch (Exception e) {
            System.out.println("⚠ getApprovedStateFallback: " + e.getMessage());
        }
        return "";
    }

    // ══════════════════════════════════════════════════════════════════════
    // APPROVE-ONLY — for a child that already has a pending Tieup Program
    // Change request (added 2026-09-11). A child with a pending request has
    // no "TIE UP PROGRAM CHANGE" entry button at all (confirmed live) — only
    // the "APPROVE TIE UP PROGRAM CHANGE REQUEST" button — so this skips
    // doTieupProgramChange()'s Add steps (1-3) and reuses its Approve
    // steps (4-6) directly. Duplicated rather than refactored into a shared
    // private helper, matching this file's existing per-flow duplication
    // convention (see setDateByJs's own "same pattern as..." comment).
    // ══════════════════════════════════════════════════════════════════════
    public String approveExistingTieupRequest(String wefDate,
                                               String feeBreakupAmount,
                                               String parentMonthly,
                                               String corporateMonthly)
            throws InterruptedException {

        wait.until(ExpectedConditions.elementToBeClickable(approveTieUpPC_btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approveTieUpPC_btn);
        System.out.println("▶ APPROVE TIEUP PROGRAM CHANGE REQUEST clicked");
        acceptNativeConfirmIfPresent();
        Thread.sleep(1000);
        logTieupProgramChangeInlineMessageIfPresent();

        wait.until(ExpectedConditions.visibilityOf(wef_dateInput));
        setDateByRealPicker(wef_dateInput, "wef_date_root", wefDate);
        System.out.println("✅ WEF Date: " + wefDate);

        wait.until(ExpectedConditions.visibilityOf(feeBreakup));
        feeBreakup.clear();
        feeBreakup.sendKeys(feeBreakupAmount);
        System.out.println("✅ Fee Breakup: " + feeBreakupAmount);

        monthlyAmount_ParentPayable.clear();
        monthlyAmount_ParentPayable.sendKeys(parentMonthly);
        System.out.println("✅ Parent Monthly: " + parentMonthly);

        monthlyAmount_CorporatePayable.clear();
        monthlyAmount_CorporatePayable.sendKeys(corporateMonthly);
        System.out.println("✅ Corporate Monthly: " + corporateMonthly);

        wait.until(ExpectedConditions.elementToBeClickable(approveRequest_Btn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", approveRequest_Btn);
        System.out.println("▶ Approve Request clicked");
        acceptNativeConfirmIfPresent();
        Thread.sleep(2000);

        String approvedToast = getTieupToastMessage();
        System.out.println("   Toast after Approve: " + approvedToast);
        approvedToast = approvedToast.isEmpty() ? getApprovedStateFallback() : approvedToast;
        System.out.println("✅ Tieup Program Change (approve-only) completed");
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

    // ══════════════════════════════════════════════════════════════════════
    // EFFECTIVE-DATE RULE ENHANCEMENT (added 2026-09-10)
    // New acceptance criteria for the Tieup Program Change effective-date
    // picker: last-2-days-of-month allows the 1st of the upcoming month,
    // 1st-4th-of-month allows the 1st of the current month (voiding +
    // regenerating that month's invoice), and both cases must show an
    // alert/info message about approving before the processing date.
    // Locator below is intentionally generic (NOT scoped to the "TIEUP"
    // keyword like getTieupToastMessage()) since the new info/alert
    // message's exact copy is not yet confirmed.
    // ══════════════════════════════════════════════════════════════════════
    @FindBy(xpath = "//div[contains(@class,'alert') or contains(@class,'info') or contains(@class,'toast')]"
            + "[string-length(normalize-space(.)) > 0]")
    List<WebElement> anyVisibleAlertOrInfoElements;

    /**
     * Returns the text of any visible alert/info/toast element on the page,
     * regardless of keyword. Used to capture the (currently unconfirmed)
     * effective-date approval-deadline message copy.
     */
    public String getAnyVisibleAlertOrInfoMessage() {
        try {
            for (WebElement el : anyVisibleAlertOrInfoElements) {
                if (el.isDisplayed()) {
                    String text = el.getText().trim();
                    if (!text.isEmpty()) {
                        System.out.println("✅ Alert/info message: " + text);
                        return text;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠ getAnyVisibleAlertOrInfoMessage: " + e.getMessage());
        }
        return "";
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE TRANSFER
    // ══════════════════════════════════════════════════════════════════════

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
    // REAL-WIDGET DATE SETTER for Tieup Program Change's processing_date/
    // wef_date Pickaday fields (added 2026-09-11).
    // Confirmed live: setDateByJs()'s value-injection + change/input/blur
    // events do NOT update this widget's actual internal state — the
    // calendar always falls back to its own default (observed: "today +
    // 7 days" highlighted, regardless of what was injected), so the
    // backend silently rejects the submission. Same failure class as the
    // Withdraw Child attrition_date bug fixed elsewhere in this project —
    // fixed the same way: drive the real widget instead of injecting the
    // value. This widget's year <select> is disabled (unlike attrition_date's
    // enabled year/month selects), so month/year navigation uses the
    // picker__nav--prev/next arrows instead of selecting an option.
    // ══════════════════════════════════════════════════════════════════════
    private void setDateByRealPicker(WebElement inputField, String rootId, String isoDate)
            throws InterruptedException {
        java.time.LocalDate target = java.time.LocalDate.parse(isoDate);
        java.time.YearMonth targetYm = java.time.YearMonth.from(target);

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');", inputField);
        wait.until(ExpectedConditions.elementToBeClickable(inputField));
        inputField.click();
        Thread.sleep(500);

        WebElement root = driver.findElement(By.id(rootId));
        wait.until(ExpectedConditions.visibilityOf(root));

        for (int i = 0; i < 60; i++) {
            String monthText = root.findElement(By.cssSelector(".picker__month")).getText().trim();
            String yearText = root.findElement(By.cssSelector(".picker__select--year option[selected]"))
                    .getText().trim();
            java.time.YearMonth shown = java.time.YearMonth.of(
                    Integer.parseInt(yearText), java.time.Month.valueOf(monthText.toUpperCase()));

            if (shown.equals(targetYm)) {
                break;
            }
            String navClass = shown.isBefore(targetYm) ? "picker__nav--next" : "picker__nav--prev";
            root.findElement(By.cssSelector("." + navClass)).click();
            Thread.sleep(400);
            root = driver.findElement(By.id(rootId));
        }

        WebElement dayCell;
        try {
            dayCell = root.findElement(By.xpath(
                    ".//div[contains(@class,'picker__day--infocus') and not(contains(@class,'picker__day--disabled'))]"
                            + "[normalize-space(text())='" + target.getDayOfMonth() + "']"));
        } catch (Exception e) {
            // Fallback (added 2026-09-11): the effective-date rule enhancement
            // allows certain otherwise-in-the-past days (e.g. the 1st of the
            // current month during the 1st-4th-of-month window) as a real,
            // backend-honored selection even though the calendar widget still
            // visually marks that day cell "disabled" — confirmed live the
            // strict non-disabled match throws NoSuchElementException for
            // exactly this case. Retry allowing a disabled-looking cell and
            // click it via JS (a plain .click() on a "disabled" cell may be
            // blocked by the widget's own click-guard).
            System.out.println("⚠ Day cell not found as non-disabled — retrying allowing a disabled-looking cell");
            dayCell = root.findElement(By.xpath(
                    ".//div[contains(@class,'picker__day--infocus')]"
                            + "[normalize-space(text())='" + target.getDayOfMonth() + "']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", dayCell);
            System.out.println("✅ Real picker date set (via disabled-cell fallback): " + isoDate);
            Thread.sleep(500);
            return;
        }
        dayCell.click();
        System.out.println("✅ Real picker date set: " + isoDate);
        Thread.sleep(500);
    }

    // ══════════════════════════════════════════════════════════════════════
    // NATIVE CONFIRM — "Are you sure you want to submit the program change
    // request?" (discovered live 2026-09-11, part of the effective-date
    // rule fix). Must be accepted (OK) or every subsequent WebDriver call
    // throws UnhandledAlertException/"unexpected alert open" — confirmed
    // that exact failure mode live before adding this handler.
    // ══════════════════════════════════════════════════════════════════════
    // ══════════════════════════════════════════════════════════════════════
    // Inline message div `.message-tieup-program-change` sits right next
    // to the Approve button (confirmed live 2026-09-11, child 67087 —
    // a Short Term tie-up child whose request status is "Processing", not
    // "Pending"). Logged as a diagnostic since it's not yet confirmed
    // whether this div is where a "not yet approvable"/status-specific
    // message appears instead of the normal WEF-date modal opening.
    // ══════════════════════════════════════════════════════════════════════
    private void logTieupProgramChangeInlineMessageIfPresent() {
        try {
            List<WebElement> msgDivs = driver.findElements(By.className("message-tieup-program-change"));
            for (WebElement m : msgDivs) {
                String text = m.getText().trim();
                if (!text.isEmpty()) {
                    System.out.println("ℹ message-tieup-program-change: " + text);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void acceptNativeConfirmIfPresent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            System.out.println("▶ Native confirm: " + alert.getText());
            alert.accept();
            System.out.println("✅ Native confirm accepted (OK)");
        } catch (Exception e) {
            // No alert present — proceed as normal.
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE TRANSFER — submit only (split from doCorporateTransfer so
    // submit/approve can be exercised independently across test cases)
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
            if (!text.isEmpty() && !text.startsWith("--")
                    && !text.equalsIgnoreCase("Select")
                    && !text.equalsIgnoreCase("Please Select")) {
                sel.selectByVisibleText(text);
                return text;
            }
        }
        return "";
    }

    // ══════════════════════════════════════════════════════════════════════
    // CORPORATE CENTER TRANSFER — button flow (Transfer Applicable=No)
    // Confirmed live (child 71962): form id="frm-center-transfer" reuses
    // joining_month/newCenter/newProgram/submit_Btn field ids.
    // ══════════════════════════════════════════════════════════════════════

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

    public String getLastSelectedApplicableMonth() {
        return lastSelectedApplicableMonth;
    }

    public String submitCorporateCenterTransfer(String applicableMonth, String centerName,
                                                String programName)
            throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(centerTransferLink));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", centerTransferLink);
        System.out.println("▶ CORPORATE CENTER TRANSFER link clicked");
        Thread.sleep(1200);

        wait.until(ExpectedConditions.visibilityOf(joining_month));
        if (applicableMonth != null && !applicableMonth.isEmpty()) {
            new Select(joining_month).selectByVisibleText(applicableMonth);
            lastSelectedApplicableMonth = applicableMonth;
            System.out.println("✅ Applicable Month: " + applicableMonth);
        } else {
            lastSelectedApplicableMonth = selectFirstAvailable(joining_month);
            System.out.println("✅ Applicable Month (first available): " + lastSelectedApplicableMonth);
        }
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
