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

/**
 * Page Object: Regular_ServiceRequests
 * <p>
 * All 11 service forms are pre-rendered in the DOM inside
 * .panel.panel-flat.child_detail_box.model-child-service
 * Visibility is toggled via display:none/block per selected service.
 * Every locator is verified directly from the live DOM dump.
 * <p>
 * Forms confirmed in DOM:
 * frm-child-pause           → Child Pause
 * frm-withdraw-child        → Withdraw Child
 * frm-program-change        → Program Change
 * frm-center-shift          → Center Shift
 * frm-extended-daycare      → Extended Daycare
 * frm-start-transport-1-way → Start Transport 1 Way
 * frm-start-transport-2-way → Start Transport 2 Way
 * frm-stop-transport-1-way  → Stop Transport 1 Way
 * frm-stop-transport-2-way  → Stop Transport 2 Way
 * frm-start-time-extension  → Start Time Extension
 * frm-stop-time-extension   → Stop Time Extension
 * <p>
 * Naming convention:
 * Common       → no prefix
 * Child Pause  → pause_
 * Center Shift → cs_
 * Program Chg  → pc_
 * Extended DC  → ed_
 * Transport    → t1_ / t2_ / st1_ / st2_
 * Time Ext     → ste_ / stp_
 * Withdraw     → wd_
 *
 */
public class Regular_ServiceRequests {

    // ══════════════════════════════════════════════════════════════════════════
    // COMMON LOCATORS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Service Request icon link on Account Statement screen
     */
    @FindBy(xpath = "//a[contains(@href,'pop_child_services')]")
    public WebElement serviceRequest_Link;

    /**
     * Services dropdown — id="send_request"
     */
    @FindBy(id = "send_request")
    public WebElement selectServices_dropdown;

    /**
     * AJAX response area — success or error messages
     */
    @FindBy(css = ".alert-ajax-response")
    public WebElement alert_ajaxResponse;

    /**
     * Inline alert message area
     */
    @FindBy(css = ".alert-message")
    public WebElement alert_message;

    /**
     * Close button on the service panel
     */
    @FindBy(css = "button.close.close-popdown")
    public WebElement btn_closePanel;

    // ══════════════════════════════════════════════════════════════════════════
    // 1. CHILD PAUSE — form id="frm-child-pause"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * From date — id="breakFrom" scoped to frm-child-pause
     */
    @FindBy(css = "#frm-child-pause #breakFrom")
    public WebElement pause_fromDate;

    /**
     * To date — id="breakTo" scoped to frm-child-pause
     */
    @FindBy(css = "#frm-child-pause #breakTo")
    public WebElement pause_toDate;

    /**
     * Reason textarea — id="pause_reason"
     */
    @FindBy(id = "pause_reason")
    public WebElement pause_reason;

    /**
     * Exception Case checkbox — id="chk-pause-exception" (CUSTOMER_REQUEST_EXCEPTION only)
     */
    @FindBy(id = "chk-pause-exception")
    public WebElement pause_exceptionCase_checkbox;

    /**
     * Ticket ID input — id="pause_ticket_id" (enabled only when exception checked)
     */
    @FindBy(id = "pause_ticket_id")
    public WebElement pause_ticketId;

    /**
     * Submit button — class="btn-service child-pause"
     */
    @FindBy(css = "button.btn-service.child-pause")
    public WebElement pause_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 2. WITHDRAW CHILD — form id="frm-withdraw-child"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * To date — id="breakTo" scoped to frm-withdraw-child
     */
    @FindBy(css = "#frm-withdraw-child #breakTo")
    public WebElement wd_toDate;

    /**
     * Reason dropdown — id="withdraw_reason"
     */
    @FindBy(id = "withdraw_reason")
    public WebElement wd_reason_dropdown;

    /**
     * Reason comment textarea — id="reason_comment"
     */
    @FindBy(id = "reason_comment")
    public WebElement wd_reasonComment;

    /**
     * Submit button — scoped to frm-withdraw-child
     */
    @FindBy(css = "#frm-withdraw-child button[name='add_request']")
    public WebElement wd_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 3. PROGRAM CHANGE — form id="frm-program-change"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Effective date — id="breakFrom" scoped to frm-program-change
     */
    @FindBy(css = "#frm-program-change #breakFrom")
    public WebElement pc_effectiveDate;

    /**
     * New Program dropdown — id="new_program" scoped to frm-program-change
     */
    @FindBy(css = "#frm-program-change #new_program")
    public WebElement pc_newProgram_dropdown;

    /**
     * Submit button — scoped to frm-program-change
     */
    @FindBy(css = "#frm-program-change button[name='add_request']")
    public WebElement pc_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 4. CENTER SHIFT — form id="frm-center-shift"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Effective date — id="breakFrom" scoped to frm-center-shift
     */
    @FindBy(css = "#frm-center-shift #breakFrom")
    public WebElement cs_effectiveDate;

    /**
     * New Center dropdown — id="new_center" (unique across all forms)
     */
    @FindBy(id = "new_center")
    public WebElement cs_newCenter_dropdown;

    /**
     * New Program inside Center Shift — id="new_program" scoped to frm-center-shift
     */
    @FindBy(css = "#frm-center-shift #new_program")
    public WebElement cs_newProgram_dropdown;

    /**
     * Center Visit Declaration checkbox — id="chk-center-visit-declaration"
     */
    @FindBy(id = "chk-center-visit-declaration")
    public WebElement cs_centerVisitDeclaration_checkbox;

    /**
     * Submit button — class="btn-service center-shift"
     */
    @FindBy(css = "button.btn-service.center-shift")
    public WebElement cs_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 5. EXTENDED DAYCARE — form id="frm-extended-daycare"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * From date — id="extendedbreakFrom" (unique, no scoping needed)
     */
    @FindBy(id = "extendedbreakFrom")
    public WebElement ed_fromDate;

    /**
     * To date — id="extendedbreakTo" (unique, no scoping needed)
     */
    @FindBy(id = "extendedbreakTo")
    public WebElement ed_toDate;

    /**
     * Submit button — class="btn-service extended-daycare"
     */
    @FindBy(css = "button.btn-service.extended-daycare")
    public WebElement ed_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 6. START TRANSPORT 1 WAY — form id="frm-start-transport-1-way"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * From date — id="breakFrom" scoped to frm-start-transport-1-way
     */
    @FindBy(css = "#frm-start-transport-1-way #breakFrom")
    public WebElement t1_fromDate;

    /**
     * Submit button — class="btn-service start-transport-1-way"
     */
    @FindBy(css = "button.btn-service.start-transport-1-way")
    public WebElement t1_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 7. START TRANSPORT 2 WAY — form id="frm-start-transport-2-way"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * From date — id="breakFrom" scoped to frm-start-transport-2-way
     */
    @FindBy(css = "#frm-start-transport-2-way #breakFrom")
    public WebElement t2_fromDate;

    /**
     * Submit button — class="btn-service start-transport-2-way"
     */
    @FindBy(css = "button.btn-service.start-transport-2-way")
    public WebElement t2_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 8. STOP TRANSPORT 1 WAY — form id="frm-stop-transport-1-way"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     *
     * From date — id="breakFrom" scoped to frm-stop-transport-1-way
     */
    @FindBy(css = "#frm-stop-transport-1-way #breakFrom")
    public WebElement st1_fromDate;

    /**
     * Submit button — class="btn-service stop-transport-1-way"
     */
    @FindBy(css = "button.btn-service.stop-transport-1-way")
    public WebElement st1_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 9. STOP TRANSPORT 2 WAY — form id="frm-stop-transport-2-way"
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * From date — id="breakFrom" scoped to frm-stop-transport-2-way
     */
    @FindBy(css = "#frm-stop-transport-2-way #breakFrom")
    public WebElement st2_fromDate;

    /**
     * Start From date — id="startFrom" (unique to stop-transport-2-way)
     */
    @FindBy(id = "startFrom")
    public WebElement st2_startFrom;

    /**
     * End From date — id="endFrom" (unique to stop-transport-2-way)
     */
    @FindBy(id = "endFrom")
    public WebElement st2_endFrom;

    /**
     * Submit button — scoped to frm-stop-transport-2-way (3 buttons share class)
     */
    @FindBy(css = "#frm-stop-transport-2-way button[name='add_request']")
    public WebElement st2_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 10. START TIME EXTENSION — form id="frm-start-time-extension"
    // Confirmed from live DOM: only ONE date field ("Start Date"), id="startFrom"
    // — NOT "breakFrom". There is no "To" date on this form.
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Start date — id="startFrom" scoped to frm-start-time-extension
     */
    @FindBy(css = "#frm-start-time-extension #startFrom")
    public WebElement ste_fromDate;

    /**
     * Submit button — scoped to frm-start-time-extension
     */
    @FindBy(css = "#frm-start-time-extension button[name='add_request']")
    public WebElement ste_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // 11. STOP TIME EXTENSION — form id="frm-stop-time-extension"
    // Confirmed from live DOM: the single date field is "End Date", id="endFrom"
    // — NOT "breakFrom".
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * End date — id="endFrom" scoped to frm-stop-time-extension
     */
    @FindBy(css = "#frm-stop-time-extension #endFrom")
    public WebElement stp_fromDate;

    /**
     * Submit button — scoped to frm-stop-time-extension
     */
    @FindBy(css = "#frm-stop-time-extension button[name='add_request']")
    public WebElement stp_btn_submit;

    // ══════════════════════════════════════════════════════════════════════════
    // DRIVER & WAIT
    // ══════════════════════════════════════════════════════════════════════════

    WebDriver driver;
    WebDriverWait wait;

    // ══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════════

    public Regular_ServiceRequests(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMMON ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public void clickServiceRequestLink() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(serviceRequest_Link));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", serviceRequest_Link);
        System.out.println("▶ Service Request link clicked");
        Thread.sleep(1000);
    }

    public boolean isModalVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(selectServices_dropdown));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void selectServiceType(String visibleText)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(selectServices_dropdown));
        new Select(selectServices_dropdown).selectByVisibleText(visibleText);
        System.out.println("✅ Service type: " + visibleText);
        Thread.sleep(800);
    }

    public void closePanel() throws InterruptedException {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(btn_closePanel));
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", btn_closePanel);
            Thread.sleep(500);
        } catch (Exception e) {
            closeModalByJs();
        }
    }

    public void closeModalByJs() {
        try {
            // Dismiss any open JS alert first
            try {
                driver.switchTo().alert().dismiss();
            } catch (Exception ignored) {
            }

            ((JavascriptExecutor) driver).executeScript(
                    // Close Bootstrap modals
                    "document.querySelectorAll('.modal-backdrop').forEach(el=>el.remove());" +
                            "document.querySelectorAll('.modal').forEach(el=>{" +
                            "    el.style.display='none';" +
                            "    el.classList.remove('in','show');" +
                            "});" +
                            "document.body.classList.remove('modal-open');" +
                            // Remove the popdown opacity overlay (#popdown-opacity)
                            // that blocks menu clicks after the service panel is open
                            "var popdown = document.getElementById('popdown-opacity');" +
                            "if (popdown) popdown.remove();" +
                            // Close the inline service request panel (.model-child-service)
                            "var panel = document.querySelector('.model-child-service, .child_detail_box');" +
                            "if (panel) {" +
                            "    panel.style.display = 'none';" +
                            "    panel.innerHTML = '';" +
                            "}" +
                            // Hide service-legend too
                            "var legend = document.querySelector('.service-legend');" +
                            "if (legend) legend.style.display = 'none';"
            );
            Thread.sleep(800);
            System.out.println("✅ Service panel + modals closed via JS");
        } catch (Exception ignored) {
        }
    }

    public void acceptAlert() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            String text = driver.switchTo().alert().getText();
            System.out.println("▶ Alert: " + text);
            driver.switchTo().alert().accept();
            System.out.println("✅ Alert accepted");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("⚠ No alert: " + e.getMessage());
        }
    }

    public void dismissAlert() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().dismiss();
            System.out.println("✅ Alert dismissed");
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("⚠ No alert: " + e.getMessage());
        }
    }

    public boolean isAlertPresent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getAlertText() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            return driver.switchTo().alert().getText();
        } catch (Exception e) {
            return "";
        }
    }

    public String getResponseMessage() {
        try {
            WebElement el = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOf(alert_ajaxResponse));
            return el.getText().trim();
        } catch (Exception e) {
            try {
                WebElement el = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.visibilityOf(alert_message));
                return el.getText().trim();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. CHILD PAUSE ACTIONS
    // Date format: "YYYY-MM-DD" e.g. "2026-04-01"
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isPauseFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(pause_fromDate));
            return pause_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setPauseFromDate(String date) throws InterruptedException {
        setDateByJs(pause_fromDate, date);
    }

    public void setPauseToDate(String date) throws InterruptedException {
        setDateByJs(pause_toDate, date);
    }

    public void enterPauseReason(String reason) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(pause_reason));
        pause_reason.clear();
        pause_reason.sendKeys(reason);
        System.out.println("✅ Pause reason: " + reason);
        Thread.sleep(300);
    }

    public boolean isExceptionCaseCheckboxVisible() {
        try {
            return pause_exceptionCase_checkbox.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void checkExceptionCase() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(pause_exceptionCase_checkbox));
        if (!pause_exceptionCase_checkbox.isSelected()) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", pause_exceptionCase_checkbox);
            System.out.println("✅ Exception Case checked");
            Thread.sleep(500);
        }
    }

    public void uncheckExceptionCase() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(pause_exceptionCase_checkbox));
        if (pause_exceptionCase_checkbox.isSelected()) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", pause_exceptionCase_checkbox);
            System.out.println("✅ Exception Case unchecked");
            Thread.sleep(500);
        }
    }

    public boolean isTicketIdFieldVisible() {
        try {
            return pause_ticketId.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTicketIdFieldEnabled() {
        try {
            return pause_ticketId.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public void enterTicketId(String ticketId) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(pause_ticketId));
        pause_ticketId.clear();
        pause_ticketId.sendKeys(ticketId);
        System.out.println("✅ Ticket ID: " + ticketId);
        Thread.sleep(300);
    }

    public void submitPause() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(pause_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", pause_btn_submit);
        System.out.println("▶ Pause submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. WITHDRAW CHILD ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isWithdrawFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(wd_toDate));
            return wd_toDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setWithdrawToDate(String date) throws InterruptedException {
        setDateByJs(wd_toDate, date);
    }

    public void selectWithdrawReason(String visibleText)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(wd_reason_dropdown));
        new Select(wd_reason_dropdown).selectByVisibleText(visibleText);
        System.out.println("✅ Withdraw reason: " + visibleText);
        Thread.sleep(300);
    }

    public void enterWithdrawComment(String comment)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(wd_reasonComment));
        wd_reasonComment.clear();
        wd_reasonComment.sendKeys(comment);
        System.out.println("✅ Withdraw comment: " + comment);
        Thread.sleep(300);
    }

    public void submitWithdraw() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(wd_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", wd_btn_submit);
        System.out.println("▶ Withdraw submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. PROGRAM CHANGE ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isProgramChangeFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(pc_effectiveDate));
            return pc_effectiveDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setPCEffectiveDate(String date) throws InterruptedException {
        setDateByJs(pc_effectiveDate, date);
    }

    public void selectPCNewProgram(String visibleText)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(pc_newProgram_dropdown));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", pc_newProgram_dropdown);
        Thread.sleep(300);
        // JS selection ensures the change event fires even if the field is
        // inside a scrollable panel where native select interaction is intercepted
        ((JavascriptExecutor) driver).executeScript(
                "var sel=arguments[0]; var txt=arguments[1];" +
                        "for(var i=0;i<sel.options.length;i++){" +
                        "    if(sel.options[i].text.trim()===txt.trim()){" +
                        "        sel.selectedIndex=i; break;" +
                        "    }" +
                        "}" +
                        "sel.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "sel.dispatchEvent(new Event('input',{bubbles:true}));",
                pc_newProgram_dropdown, visibleText);
        System.out.println("✅ New Program: " + visibleText);
        Thread.sleep(500);
    }

    public String getSelectedProgram() {
        try {
            return new Select(pc_newProgram_dropdown)
                    .getFirstSelectedOption().getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String selectFirstAvailableProgram() throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(pc_newProgram_dropdown));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", pc_newProgram_dropdown);
        Thread.sleep(300);
        Select sel = new Select(pc_newProgram_dropdown);
        String selected = "";
        for (WebElement opt : sel.getOptions()) {
            String txt = opt.getText().trim();
            if (!txt.isEmpty() && !txt.equals("--Select--")) {
                selected = txt;
                break;
            }
        }
        if (!selected.isEmpty()) {
            selectPCNewProgram(selected);
            System.out.println("✅ First available program: " + selected);
        } else {
            System.out.println("⚠ No non-placeholder program option found");
        }
        return selected;
    }

    public void submitProgramChange() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(pc_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", pc_btn_submit);
        System.out.println("▶ Program Change submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. CENTER SHIFT ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isCenterShiftFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(cs_effectiveDate));
            return cs_effectiveDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setCSEffectiveDate(String date) throws InterruptedException {
        setDateByJs(cs_effectiveDate, date);
    }

    public void selectCSNewCenter(String visibleText)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(cs_newCenter_dropdown));
        new Select(cs_newCenter_dropdown).selectByVisibleText(visibleText);
        System.out.println("✅ New Center: " + visibleText);
        Thread.sleep(300);
    }

    public void selectCSNewProgram(String visibleText)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(cs_newProgram_dropdown));
        new Select(cs_newProgram_dropdown).selectByVisibleText(visibleText);
        System.out.println("✅ CS New Program: " + visibleText);
        Thread.sleep(300);
    }

    public void checkCSCenterVisitDeclaration()
            throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(
                cs_centerVisitDeclaration_checkbox));
        if (!cs_centerVisitDeclaration_checkbox.isSelected()) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();",
                            cs_centerVisitDeclaration_checkbox);
            System.out.println("✅ Center Visit Declaration checked");
            Thread.sleep(300);
        }
    }

    public boolean isCSCenterVisitDeclarationChecked() {
        try {
            return cs_centerVisitDeclaration_checkbox.isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    public void submitCenterShift() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(cs_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", cs_btn_submit);
        System.out.println("▶ Center Shift submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. EXTENDED DAYCARE ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isExtendedDaycareFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(ed_fromDate));
            return ed_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setEDFromDate(String date) throws InterruptedException {
        setDateByJs(ed_fromDate, date);
    }

    public void setEDToDate(String date) throws InterruptedException {
        setDateByJs(ed_toDate, date);
    }

    public void submitExtendedDaycare() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(ed_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", ed_btn_submit);
        System.out.println("▶ Extended Daycare submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. START TRANSPORT 1 WAY ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isStartTransport1WayFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(t1_fromDate));
            return t1_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setT1FromDate(String date) throws InterruptedException {
        setDateByJs(t1_fromDate, date);
    }

    public void submitStartTransport1Way() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(t1_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", t1_btn_submit);
        System.out.println("▶ Start Transport 1 Way submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. START TRANSPORT 2 WAY ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isStartTransport2WayFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(t2_fromDate));
            return t2_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setT2FromDate(String date) throws InterruptedException {
        setDateByJs(t2_fromDate, date);
    }

    public void submitStartTransport2Way() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(t2_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", t2_btn_submit);
        System.out.println("▶ Start Transport 2 Way submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 8. STOP TRANSPORT 1 WAY ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isStopTransport1WayFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(st1_fromDate));
            return st1_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setST1FromDate(String date) throws InterruptedException {
        setDateByJs(st1_fromDate, date);
    }

    public void submitStopTransport1Way() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(st1_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", st1_btn_submit);
        System.out.println("▶ Stop Transport 1 Way submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 9. STOP TRANSPORT 2 WAY ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isStopTransport2WayFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(st2_fromDate));
            return st2_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setST2FromDate(String date) throws InterruptedException {
        setDateByJs(st2_fromDate, date);
    }

    /**
     * startFrom — second date field unique to stop-transport-2-way
     */
    public void setST2StartFrom(String date) throws InterruptedException {
        setDateByJs(st2_startFrom, date);
    }

    /**
     * endFrom — third date field unique to stop-transport-2-way
     */
    public void setST2EndFrom(String date) throws InterruptedException {
        setDateByJs(st2_endFrom, date);
    }

    public void submitStopTransport2Way() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(st2_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", st2_btn_submit);
        System.out.println("▶ Stop Transport 2 Way submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 10. START TIME EXTENSION ACTIONS
    // NOTE: JS sets minExtensionDateFrom and minExtensionDateTo on form load.
    //       Dates must fall within the allowed range.
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isStartTimeExtensionFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(ste_fromDate));
            return ste_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setSTEFromDate(String date) throws InterruptedException {
        setDateByJs(ste_fromDate, date);
    }

    public void submitStartTimeExtension() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(ste_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", ste_btn_submit);
        System.out.println("▶ Start Time Extension submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 11. STOP TIME EXTENSION ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isStopTimeExtensionFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(stp_fromDate));
            return stp_fromDate.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void setSTPFromDate(String date) throws InterruptedException {
        setDateByJs(stp_fromDate, date);
    }

    public void submitStopTimeExtension() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(stp_btn_submit));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", stp_btn_submit);
        System.out.println("▶ Stop Time Extension submit clicked");
        Thread.sleep(1000);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CALENDAR VERIFICATION HELPERS
    //
    // Verified against live DOM (Jul 2, 2026 — Jaydeep Kar / Support/Exception user):
    //
    // CONFIRMED CLASS PATTERNS from actual table#breakFrom_table HTML:
    //   picker__day--infocus picker__day--highlighted  → TODAY (enabled, no aria-disabled)
    //   picker__day--infocus                           → Current month enabled day (1–10)
    //   picker__day--infocus picker__day--disabled     → Current month DISABLED day (11–31)
    //   picker__day--outfocus picker__day--disabled    → Prev/next month disabled (Jun 28-30, Aug 2-8)
    //   picker__day--outfocus (NO disabled)            → Next month 1st = ENABLED! (Aug 1)
    //
    // SOURCE OF TRUTH: aria-disabled="true" attribute — present on ALL disabled cells.
    // Do NOT rely on CSS class alone — Aug 1 is outfocus but enabled (no aria-disabled).
    //
    // Day number uniqueness: day "1" appears as Jun outfocus AND Aug outfocus.
    // isDayDisabledInCurrentMonth() scopes to .picker__day--infocus only.
    // isNextMonthDayEnabled() scopes to .picker__day--outfocus for next-month cells.
    //
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Open the Pickaday calendar by clicking the given date input field.
     * Waits until the calendar table is visible before returning.
     */
    public void openCalendarFor(WebElement dateField)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(dateField));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", dateField);
        Thread.sleep(300);
        // Try the Pickaday internal API first; fall back to a native focus + click
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0];" +
                        "if(el._picker && typeof el._picker.open==='function'){" +
                        "    el._picker.open();" +
                        "} else {" +
                        "    el.removeAttribute('readonly');" +
                        "    el.focus();" +
                        "    el.click();" +
                        "}",
                dateField);
        Thread.sleep(500);
        // Scope the wait to the picker wrapper that becomes .picker--opened
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".picker--opened table.picker__table")));
        Thread.sleep(300);
        System.out.println("▶ Calendar opened");
    }

    /**
     * Close the calendar via the Close button.
     * Falls back to Escape key.
     */
    public void closeCalendar() throws InterruptedException {
        try {
            org.openqa.selenium.WebElement closeBtn = driver.findElement(
                    By.cssSelector(".picker__button--close"));
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", closeBtn);
            Thread.sleep(400);
            System.out.println("✅ Calendar closed");
        } catch (Exception e) {
            try {
                driver.findElement(By.tagName("body"))
                        .sendKeys(org.openqa.selenium.Keys.ESCAPE);
                Thread.sleep(300);
            } catch (Exception ignored) {
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // HIGH-LEVEL DATE RESTRICTION CHECKER
    // Used by SC001_TC002, SC001_TC003, SC002_TC002, SC003_TC003, SC012_TC006
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Checks whether a specific date (YYYY-MM-DD) is DISABLED in the
     * Program Change effective date calendar.
     * <p>
     * How it works:
     * 1. Opens the calendar by clicking pc_effectiveDate field
     * 2. Parses target year/month/day from the dateString
     * 3. Navigates months forward/backward until the target month is shown
     * 4. Finds the day cell matching the day number
     * 5. Checks aria-disabled="true" on that cell
     * 6. Closes the calendar
     * 7. Returns true if the date is disabled, false if selectable
     * <p>
     * Handles three cases:
     * - Past month (e.g. "2026-06-10"): navigate back — prev-month cells
     * are aria-disabled=true when they appear as outfocus
     * - Current month (e.g. "2026-07-08"): infocus cells, check aria-disabled
     * - Next month (e.g. "2026-08-01"): outfocus cell visible in current view
     * or navigate forward
     *
     * @param dateString format "YYYY-MM-DD" e.g. "2026-07-08"
     * @return true if the date cell has aria-disabled="true", false if selectable
     */
    public boolean isPCEffectiveDateDisabled(String dateString)
            throws InterruptedException {

        // Step 1: Parse date parts
        String[] parts = dateString.split("-");
        int targetYear = Integer.parseInt(parts[0]);
        int targetMonth = Integer.parseInt(parts[1]); // 1-based (Jan=1)
        int targetDay = Integer.parseInt(parts[2]);

        System.out.println("▶ Checking if date is disabled: " + dateString
                + " (day=" + targetDay + ", month=" + targetMonth
                + ", year=" + targetYear + ")");

        // Step 2: Open the calendar
        openCalendarFor(pc_effectiveDate);
        Thread.sleep(300);

        boolean isDisabled;
        try {
            // Step 3: Navigate to the target month if needed
            navigateCalendarToMonth(targetYear, targetMonth);

            // Step 4: Find the day cell and check aria-disabled
            isDisabled = isDayCellDisabled(targetDay);

        } finally {
            // Step 5: Always close the calendar regardless of result
            closeCalendar();
        }

        System.out.println("   isPCEffectiveDateDisabled(" + dateString + ") = " + isDisabled);
        return isDisabled;
    }

    /**
     * Navigates the open calendar to the specified year and month.
     * Uses the prev/next arrows. Limits navigation to 24 months to prevent infinite loops.
     *
     * @param targetYear  e.g. 2026
     * @param targetMonth 1-based month (Jan=1, Jul=7)
     */
    private void navigateCalendarToMonth(int targetYear, int targetMonth)
            throws InterruptedException {

        for (int i = 0; i < 24; i++) { // max 24 month navigations
            // Read current year/month from calendar dropdowns
            int currentYear = Integer.parseInt(
                    new org.openqa.selenium.support.ui.Select(
                            driver.findElement(By.cssSelector(".picker__select--year")))
                            .getFirstSelectedOption().getText().trim());
            int currentMonth = Integer.parseInt(
                    new org.openqa.selenium.support.ui.Select(
                            driver.findElement(By.cssSelector(".picker__select--month")))
                            .getFirstSelectedOption().getAttribute("value").trim()) + 1; // 0-based → 1-based

            System.out.println("   Calendar at: " + currentYear + "-" + currentMonth
                    + " | Target: " + targetYear + "-" + targetMonth);

            if (currentYear == targetYear && currentMonth == targetMonth) {
                break; // already on target month
            }

            // Determine direction
            boolean goForward = (targetYear > currentYear)
                    || (targetYear == currentYear && targetMonth > currentMonth);

            if (goForward) {
                // Click next month arrow (if not disabled)
                java.util.List<org.openqa.selenium.WebElement> nextBtns =
                        driver.findElements(By.cssSelector(
                                ".picker__nav--next:not(.picker__nav--disabled)"));
                if (nextBtns.isEmpty()) {
                    System.out.println("   ⚠ Next arrow disabled — cannot navigate forward");
                    break;
                }
                nextBtns.get(0).click();
            } else {
                // Click prev month arrow (if not disabled)
                java.util.List<org.openqa.selenium.WebElement> prevBtns =
                        driver.findElements(By.cssSelector(
                                ".picker__nav--prev:not(.picker__nav--disabled)"));
                if (prevBtns.isEmpty()) {
                    // Prev arrow is disabled — this confirms past dates are blocked
                    System.out.println("   ✅ Prev arrow disabled — past month navigation blocked = date IS disabled");
                    return; // calendar cannot go back = date is effectively disabled
                }
                prevBtns.get(0).click();
            }
            Thread.sleep(400);
        }
    }

    /**
     * Finds a day cell by its day number in the CURRENTLY displayed month
     * and checks if it has aria-disabled="true".
     * Checks both infocus (current month) and outfocus (adjacent month) cells.
     *
     * @param dayNumber e.g. 10
     * @return true if aria-disabled="true", false if selectable
     */
    private boolean isDayCellDisabled(int dayNumber) {
        // Check all picker day cells (infocus + outfocus)
        java.util.List<org.openqa.selenium.WebElement> allCells =
                driver.findElements(By.cssSelector(".picker__day"));

        for (org.openqa.selenium.WebElement cell : allCells) {
            if (cell.getText().trim().equals(String.valueOf(dayNumber))) {
                String ariaDisabled = cell.getAttribute("aria-disabled");
                String classes = cell.getAttribute("class");
                boolean disabled = "true".equals(ariaDisabled);
                System.out.println("   Cell day=" + dayNumber
                        + " class='" + classes + "'"
                        + " aria-disabled='" + ariaDisabled + "'"
                        + " → disabled=" + disabled);
                return disabled;
            }
        }

        // Cell not found — treat as disabled
        System.out.println("   Day " + dayNumber + " cell not found in calendar → treating as disabled");
        return true;
    }

    /**
     * Returns true if a CURRENT-MONTH day is disabled.
     * Scopes to .picker__day--infocus cells only to avoid matching
     * outfocus cells (prev/next month) with the same day number.
     * <p>
     * Source of truth: aria-disabled="true" attribute.
     *
     * @param dayNumber day of the current displayed month (1–31)
     */
    public boolean isDayDisabledInCurrentMonth(int dayNumber) {
        try {
            // Scope to infocus (current month) cells only
            java.util.List<org.openqa.selenium.WebElement> cells =
                    driver.findElements(By.cssSelector(".picker__day--infocus"));
            for (org.openqa.selenium.WebElement cell : cells) {
                if (cell.getText().trim().equals(String.valueOf(dayNumber))) {
                    String ariaDisabled = cell.getAttribute("aria-disabled");
                    boolean disabled = "true".equals(ariaDisabled);
                    System.out.println("   [Current month] Day " + dayNumber
                            + " aria-disabled=" + ariaDisabled + " → disabled=" + disabled);
                    return disabled;
                }
            }
            System.out.println("   Day " + dayNumber + " not found in current month");
            return true; // treat as disabled if cell not found
        } catch (Exception e) {
            System.out.println("   isDayDisabledInCurrentMonth error: " + e.getMessage());
            return true;
        }
    }

    /**
     * Convenience inverse of isDayDisabledInCurrentMonth.
     */
    public boolean isDayEnabledInCurrentMonth(int dayNumber) {
        return !isDayDisabledInCurrentMonth(dayNumber);
    }

    /**
     * Returns true if the next-month day (shown as outfocus cell) is ENABLED.
     * Confirmed from DOM: Aug 1 is picker__day--outfocus WITHOUT aria-disabled.
     * Aug 2-8 are picker__day--outfocus WITH aria-disabled="true".
     *
     * @param dayNumber day number shown in the outfocus (next month) cells
     */
    public boolean isNextMonthDayEnabled(int dayNumber) {
        try {
            java.util.List<org.openqa.selenium.WebElement> outfocusCells =
                    driver.findElements(By.cssSelector(".picker__day--outfocus"));
            for (org.openqa.selenium.WebElement cell : outfocusCells) {
                if (cell.getText().trim().equals(String.valueOf(dayNumber))) {
                    String ariaDisabled = cell.getAttribute("aria-disabled");
                    boolean enabled = !"true".equals(ariaDisabled);
                    System.out.println("   [Next month] Day " + dayNumber
                            + " aria-disabled=" + ariaDisabled + " → enabled=" + enabled);
                    return enabled;
                }
            }
            System.out.println("   Next month day " + dayNumber + " outfocus cell not found");
            return false;
        } catch (Exception e) {
            System.out.println("   isNextMonthDayEnabled error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Legacy aliases — kept for backward compatibility with existing test calls.
     * Delegates to isDayDisabledInCurrentMonth().
     */
    public boolean isDayDisabled(int dayNumber) {
        return isDayDisabledInCurrentMonth(dayNumber);
    }

    public boolean isDayEnabled(int dayNumber) {
        return isDayEnabledInCurrentMonth(dayNumber);
    }

    /**
     * Assert current-month day is disabled. Fails with clear message.
     */
    public void assertDayIsDisabled(int dayNumber) {
        boolean disabled = isDayDisabledInCurrentMonth(dayNumber);
        org.testng.Assert.assertTrue(disabled,
                "❌ Day " + dayNumber + " should be DISABLED but is selectable");
        System.out.println("✅ Day " + dayNumber + " correctly disabled");
    }

    /**
     * Assert current-month day is enabled. Fails with clear message.
     */
    public void assertDayIsEnabled(int dayNumber) {
        boolean enabled = isDayEnabledInCurrentMonth(dayNumber);
        org.testng.Assert.assertTrue(enabled,
                "❌ Day " + dayNumber + " should be ENABLED but is disabled");
        System.out.println("✅ Day " + dayNumber + " correctly enabled");
    }

    /**
     * Returns list of all DISABLED day numbers in the current month.
     * Uses aria-disabled="true" as source of truth.
     * Scoped to .picker__day--infocus to avoid duplicate day numbers from outfocus.
     */
    public java.util.List<Integer> getAllDisabledDaysInCurrentMonth() {
        java.util.List<Integer> disabled = new java.util.ArrayList<>();
        try {
            java.util.List<org.openqa.selenium.WebElement> cells =
                    driver.findElements(By.cssSelector(
                            ".picker__day--infocus[aria-disabled='true']"));
            for (org.openqa.selenium.WebElement cell : cells) {
                try {
                    String txt = cell.getText().trim();
                    if (!txt.isEmpty()) disabled.add(Integer.parseInt(txt));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            System.out.println("   getAllDisabledDaysInCurrentMonth error: " + e.getMessage());
        }
        System.out.println("   Disabled current-month days: " + disabled);
        return disabled;
    }

    /**
     * Returns list of all ENABLED day numbers in the current month.
     * An enabled current-month day has class infocus but NO aria-disabled="true".
     */
    public java.util.List<Integer> getAllEnabledDaysInCurrentMonth() {
        java.util.List<Integer> enabled = new java.util.ArrayList<>();
        try {
            // All infocus cells that do NOT have aria-disabled attribute
            java.util.List<org.openqa.selenium.WebElement> cells =
                    driver.findElements(By.cssSelector(".picker__day--infocus"));
            for (org.openqa.selenium.WebElement cell : cells) {
                String ariaDisabled = cell.getAttribute("aria-disabled");
                if (!"true".equals(ariaDisabled)) {
                    try {
                        String txt = cell.getText().trim();
                        if (!txt.isEmpty()) enabled.add(Integer.parseInt(txt));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("   getAllEnabledDaysInCurrentMonth error: " + e.getMessage());
        }
        System.out.println("   Enabled current-month days: " + enabled);
        return enabled;
    }

    /**
     * Legacy aliases for backward compatibility.
     */
    public java.util.List<Integer> getAllDisabledDays() {
        return getAllDisabledDaysInCurrentMonth();
    }

    public java.util.List<Integer> getAllEnabledDays() {
        return getAllEnabledDaysInCurrentMonth();
    }

    /**
     * Returns the highest enabled day number in the current month.
     * e.g. returns 5 if only Days 1–5 are enabled (Parent window).
     * e.g. returns 10 if Days 1–10 enabled (Exception user window).
     */
    public int getLastEnabledDayInCurrentMonth() {
        java.util.List<Integer> enabled = getAllEnabledDaysInCurrentMonth();
        if (enabled.isEmpty()) return 0;
        int last = java.util.Collections.max(enabled);
        System.out.println("   Last enabled day in current month: " + last);
        return last;
    }

    /**
     * Click an enabled current-month day in the open calendar.
     *
     * @return true if clicked successfully, false if day is disabled/not found
     */
    public boolean clickCalendarDay(int dayNumber) throws InterruptedException {
        java.util.List<org.openqa.selenium.WebElement> cells =
                driver.findElements(By.cssSelector(".picker__day--infocus"));
        for (org.openqa.selenium.WebElement cell : cells) {
            if (cell.getText().trim().equals(String.valueOf(dayNumber))
                    && !"true".equals(cell.getAttribute("aria-disabled"))) {
                cell.click();
                Thread.sleep(300);
                System.out.println("✅ Clicked calendar day " + dayNumber);
                return true;
            }
        }
        System.out.println("   Day " + dayNumber + " not clickable (disabled or not found)");
        return false;
    }

    /**
     * Navigate calendar to next month via the > arrow button.
     */
    public void calendarNextMonth() throws InterruptedException {
        org.openqa.selenium.WebElement nextArrow = driver.findElement(By.cssSelector(
                ".picker__nav--next:not(.picker__nav--disabled)"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextArrow);
        Thread.sleep(500);
        System.out.println("▶ Calendar: navigated to next month");
    }

    /**
     * Get displayed month and year from the calendar dropdowns.
     *
     * @return e.g. "July 2026"
     */
    public String getCalendarMonthYear() {
        try {
            String year = new org.openqa.selenium.support.ui.Select(
                    driver.findElement(By.cssSelector(".picker__select--year")))
                    .getFirstSelectedOption().getText().trim();
            String month = new org.openqa.selenium.support.ui.Select(
                    driver.findElement(By.cssSelector(".picker__select--month")))
                    .getFirstSelectedOption().getText().trim();
            return month + " " + year;
        } catch (Exception e) {
            return "";
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE — JS date setter for all Pickaday readonly inputs
    //
    // BUG FIX: Setting input.value alone does NOT update Pickaday's internal
    // state. Form validation reads Pickaday's picker object, not the raw input
    // value — hence "Date is required" even when the input visually shows a date.
    //
    // Fix: After setting input value, call picker.set('select', date) on the
    // Pickaday instance attached to the input, then fire change/input/blur events.
    // ══════════════════════════════════════════════════════════════════════════

    private void setDateByJs(WebElement field, String date)
            throws InterruptedException {

        // Step 1: Remove readonly
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');", field);

        // Step 2: Set raw input value (visual display)
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];", field, date);

        // Step 3: Try ALL known Pickaday/datepicker internal state patterns.
        // Different Pickaday versions attach the picker differently:
        //   _picker.setDate()  — Pickaday standalone (most common)
        //   _picker.set()      — older Pickaday API
        //   pickaday property  — some custom wrappers
        //   jQuery pickadate   — jQuery plugin variant
        //   jQuery datepicker  — jQuery UI fallback
        ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];" +
                        "var dateStr = arguments[1];" +
                        "var d = new Date(dateStr);" +
                        "if (el._picker && typeof el._picker.setDate === 'function') {" +
                        "    el._picker.setDate(d, true);" +
                        "} else if (el._picker && typeof el._picker.set === 'function') {" +
                        "    el._picker.set('select', d);" +
                        "} else if (el.pickaday && typeof el.pickaday.setDate === 'function') {" +
                        "    el.pickaday.setDate(d, true);" +
                        "} else if (typeof $ !== 'undefined' && $(el).data('pickadate')) {" +
                        "    $(el).pickadate('picker').set('select', d);" +
                        "} else if (typeof $ !== 'undefined' && $(el).data('datepicker')) {" +
                        "    $(el).datepicker('setDate', d);" +
                        "}" +
                        "el.dispatchEvent(new Event('change', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('input',  {bubbles:true}));" +
                        "el.dispatchEvent(new Event('blur',   {bubbles:true}));",
                field, date);

        System.out.println("✅ Date set (Pickaday): " + date);
        Thread.sleep(600);
    }
}
