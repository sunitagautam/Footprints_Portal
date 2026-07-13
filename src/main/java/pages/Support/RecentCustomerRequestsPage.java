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

import java.time.Duration;
import java.util.List;

/**
 * Page Object: RecentCustomerRequestsPage
 * <p>
 * Navigation:
 * Method 1: Support → Account Statement → Enter Admission ID
 * → Generate → Customer Requests link (top-right icon)
 * Method 2: Children → Recent Customer Requests sub-tab
 * Direct URL: /recent_update_details?child_id=<ID>
 * <p>
 * ALL LOCATORS CONFIRMED FROM LIVE DOM (console dump Jun 16, 2026):
 * <p>
 * Admission ID  → id="typeahead_child_id"  name="child_id"  type=number
 * From date     → name="date_from"         (duplicate id, use name)
 * To date       → name="date_to"           (duplicate id, use name)
 * Center        → id="center_selection"
 * Request Type  → id="type"  (multiselect — NOT a simple <select>)
 * Status        → id="status"              name="request_approval_status"
 * Admission Type→ id="admission_type"
 * Support Exec  → id="request_owner"
 * Submit        → name="submit_date"  type=submit  (INPUT not BUTTON)
 * Extend date   → id="update_date_to"  (modal input, always in DOM)
 * Extend Confirm→ class="btn-model-extension-cancel"
 * Cancel Request→ class="btn-model-cancel"
 */
public class RecentCustomerRequestsPage {

    WebDriver driver;
    WebDriverWait wait;

    // ══════════════════════════════════════════════════════════════════════
    // FILTER SECTION — all confirmed from live DOM
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Admission ID — typeahead number input
     * id="typeahead_child_id"  name="child_id"  type=number
     */
    @FindBy(id = "typeahead_child_id")
    WebElement admissionIdInput;

    /**
     * From date — jQuery datepicker input
     * Two inputs share id="select_date" — must scope by name="date_from"
     */
    @FindBy(name = "date_from")
    WebElement fromDateInput;

    /**
     * To date — jQuery datepicker input
     * name="date_to"
     */
    @FindBy(name = "date_to")
    WebElement toDateInput;

    /**
     * Center dropdown — id="center_selection"
     */
    @FindBy(id = "center_selection")
    WebElement centerDropdown;

    /**
     * Request Type — id="type"  name="type[]"  type=select-multiple
     * This is a Bootstrap Multiselect — rendered as checkboxes in a dropdown.
     * For single value selection use the underlying <select> directly via JS.
     */
    @FindBy(id = "type")
    WebElement requestTypeSelect;

    /**
     * Status dropdown — id="status"  name="request_approval_status"
     * Options: All | Pending | Processing | Approved etc.
     */
    @FindBy(id = "status")
    WebElement statusDropdown;

    /**
     * Admission Type dropdown — id="admission_type"
     * Options: Regular | Corporate
     */
    @FindBy(id = "admission_type")
    WebElement admissionTypeDropdown;

    /**
     * Support Executive dropdown — id="request_owner"
     */
    @FindBy(id = "request_owner")
    WebElement supportExecutiveDropdown;

    /**
     * Submit button — INPUT type=submit, name="submit_date"
     * class="btn btn-primary btn-lg"  NO id attribute
     */
    @FindBy(name = "submit_date")
    WebElement submitButton;

    // ══════════════════════════════════════════════════════════════════════
    // RESULTS TABLE — DataTables
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Download Data button — DataTables CSV export button.
     * Confirmed from live DOM: <a class="dt-button buttons-csv buttons-html5">
     * <span>Download Data</span></a>
     * Rendered by DataTables after results load — not present on initial page load.
     */
    @FindBy(css = "a.buttons-csv.buttons-html5")
    WebElement downloadButton;

    /**
     * DataTables info: "Showing X to Y of Z entries"
     */
    @FindBy(xpath = "//div[contains(@class,'dataTables_info')]")
    WebElement showingEntriesText;

    /**
     * DataTables inline search input
     */
    @FindBy(xpath = "//div[contains(@class,'dataTables_filter')]//input[@type='search']")
    WebElement tableSearchInput;

    /**
     * All result rows — excludes empty "No data" rows
     */
    @FindBy(xpath = "//table[contains(@class,'dataTable')]/tbody/tr"
            + "[not(contains(@class,'dataTables_empty'))]")
    List<WebElement> tableRows;

    /**
     * All column headers
     */
    @FindBy(xpath = "//table[contains(@class,'dataTable')]/thead/tr/th")
    List<WebElement> tableHeaders;

    /**
     * The DataTable element
     */
    @FindBy(xpath = "//table[contains(@class,'dataTable')]")
    WebElement dataTable;

    // ══════════════════════════════════════════════════════════════════════
    // ROW-LEVEL ACTION BUTTONS — confirmed from live DOM
    //
    // Pause rows show two buttons per row:
    //   CANCEL            → class="cancel_pause"       (Pending/Approved rows)
    //   PROCESSING DETAILS→ class="processing-details" (Pending/Approved rows)
    //
    // Early Resume / Extend buttons appear only AFTER pause is Approved.
    // All buttons carry request_id attribute for row identification.
    // ══════════════════════════════════════════════════════════════════════
    /**
     * Extend/Resume date input — id="update_date_to"  name="update_date_to"
     * Used in both Early Resume and Extend modals
     */
    @FindBy(id = "update_date_to")
    WebElement extendResumeDateInput;
    /**
     * Extend/Resume Confirm button
     * class="btn btn-primary btn-model-extension-cancel"
     * (Misleading class name — this IS the Confirm action button)
     */
    @FindBy(css = ".btn-model-extension-cancel")
    WebElement btnExtendConfirm;
    /**
     * Cancel Request modal Confirm button
     * class="btn btn-primary btn-model-cancel"
     */
    @FindBy(css = ".btn-model-cancel")
    WebElement btnCancelConfirm;
    /**
     * Close/Reject modal button — class="btn btn-danger"  text="Close"
     */
    @FindBy(xpath = "//button[contains(@class,'btn-danger') and normalize-space(text())='Close']")
    WebElement btnModalClose;

    public RecentCustomerRequestsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        PageFactory.initElements(driver, this);
    }

    /**
     * Click CANCEL button for a specific child's pause request.
     * Finds the row matching the Admission ID, then clicks cancel_pause button.
     * After click, a confirmation modal appears — call confirmCancelRequest() next.
     * <p>
     * Flow: filterByAdmId → find cancel_pause button in that row → click
     *
     * @param admId Admission ID to search e.g. "49149"
     */
    public void clickCancelPause(String admId) throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();

        // Find the cancel button in the row containing this child's record
        WebElement cancelBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.cancel_pause")));
        String requestId = cancelBtn.getAttribute("request_id");
        System.out.println("▶ Clicking CANCEL for admId=" + admId
                + " request_id=" + requestId);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", cancelBtn);
        Thread.sleep(800);
    }


    /**
     * Click PROCESSING DETAILS button for a specific child's pause request.
     * Opens a modal or section showing processing status.
     *
     * @param admId Admission ID to search
     */
    public void clickProcessingDetails(String admId) throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();

        WebElement detailsBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.processing-details")));
        String requestId = detailsBtn.getAttribute("request_id");
        System.out.println("▶ Clicking PROCESSING DETAILS for admId=" + admId
                + " request_id=" + requestId);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", detailsBtn);
        Thread.sleep(1000);
    }

    // ──────────────────────────────────────────────────────────────────────
    // PAUSE — CANCEL + PROCESSING DETAILS
    // Confirmed: Pending/Approved pause rows
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Click PROCESSING DETAILS button for a specific request_id.
     */
    public void clickProcessingDetailsByRequestId(String requestId)
            throws InterruptedException {
        WebElement detailsBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.processing-details[request_id='" + requestId + "']")));
        System.out.println("▶ Clicking PROCESSING DETAILS for request_id=" + requestId);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", detailsBtn);
        Thread.sleep(1000);
    }

    /**
     * Returns true if CANCEL button is present for a given Admission ID.
     * Indicates the pause is in Pending/Approved status (cancellable).
     */
    public boolean isCancelButtonVisible(String admId) throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        return !driver.findElements(By.cssSelector("button.cancel_pause")).isEmpty();
    }

    // ── CANCEL — Child Pause (cancel_pause) ──────────────────────────────

    /**
     * Returns true if PROCESSING DETAILS button is present for a given Admission ID.
     */
    public boolean isProcessingDetailsButtonVisible(String admId)
            throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        return !driver.findElements(By.cssSelector("button.processing-details")).isEmpty();
    }


    // ── CANCEL — Program Change (cancel_customer_request) ────────────────

    /**
     * Dismiss the cancel modal — class="btn-danger"  text="Close"
     */
    public void dismissCancelModal() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(@class,'btn-danger')"
                        + " and normalize-space(text())='Close']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(500);
        System.out.println("✅ Cancel modal dismissed");
    }

    /**
     * Returns true if CANCEL button visible for Pause (Pending status)
     */
    public boolean isCancelPauseButtonVisible() {
        return !driver.findElements(By.cssSelector("button.cancel_pause")).isEmpty();
    }

    /**
     * Returns true if Pause CANCEL button gone (cancelled successfully)
     */
    public boolean isCancelPauseButtonGone() {
        return driver.findElements(By.cssSelector("button.cancel_pause")).isEmpty();
    }

    /**
     * Click CANCEL for a Pending Pause request
     */
    public void clickCancelPause() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.cancel_pause")));
        System.out.println("▶ CANCEL Pause clicked, request_id="
                + btn.getAttribute("request_id"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
    }

    // ── CANCEL — Generic (works for both Pause and Program Change) ────────

    /**
     * Click CANCEL for a specific Pause request_id
     */
    public void clickCancelPauseByRequestId(String requestId) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.cancel_pause[request_id='" + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ CANCEL Pause clicked, request_id=" + requestId);
    }

    /**
     * Returns true if CANCEL button visible for Program Change (Pending status)
     */
    public boolean isCancelProgramChangeButtonVisible() {
        return !driver.findElements(
                By.cssSelector("button.cancel_customer_request")).isEmpty();
    }

    /**
     * Returns true if Program Change CANCEL button gone
     */
    public boolean isCancelProgramChangeButtonGone() {
        return driver.findElements(
                By.cssSelector("button.cancel_customer_request")).isEmpty();
    }

    /**
     * Click CANCEL for a Pending Program Change request
     */
    public void clickCancelProgramChange() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.cancel_customer_request")));
        System.out.println("▶ CANCEL Program Change clicked, request_id="
                + btn.getAttribute("request_id"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
    }

    /**
     * Click CANCEL Program Change for a specific request_id
     */
    public void clickCancelProgramChangeByRequestId(String requestId)
            throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.cancel_customer_request[request_id='"
                        + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ CANCEL Program Change clicked, request_id=" + requestId);
    }

    // ──────────────────────────────────────────────────────────────────────
    // PAUSE — EARLY RESUME + EXTEND
    // Only appear when Approval Status = Approved.
    // TODO: Run DOM dump after filtering Pause + Approved to confirm class names:
    //   document.querySelectorAll('button').forEach(b=>
    //     console.log(b.className,'|',b.getAttribute('request_id'),'|',b.innerText?.trim()))
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if ANY cancel button visible (pause or program change)
     */
    public boolean isCancelButtonVisible() {
        return !driver.findElements(By.cssSelector(
                "button.cancel_pause, button.cancel_customer_request")).isEmpty();
    }

    /**
     * Returns true if ALL cancel buttons gone
     */
    public boolean isCancelButtonGone() {
        return driver.findElements(By.cssSelector(
                "button.cancel_pause, button.cancel_customer_request")).isEmpty();
    }

    /**
     * Returns true if PROCESSING DETAILS button visible
     */
    public boolean isProcessingDetailsVisible() {
        return !driver.findElements(By.cssSelector("button.processing-details")).isEmpty();
    }

    /**
     * Get request_id of first CANCEL button — use for chaining
     */
    public String getFirstCancelRequestId() {
        try {
            return driver.findElement(By.cssSelector("button.cancel_pause"))
                    .getAttribute("request_id");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get request_id of first PROCESSING DETAILS button
     */
    public String getFirstProcessingDetailsRequestId() {
        try {
            return driver.findElement(By.cssSelector("button.processing-details"))
                    .getAttribute("request_id");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Click EARLY RESUME for a specific child row.
     * Confirmed from live DOM (50-page scan):
     * Enabled  → <a class="popdown_medium btn btn-primary bg-blue btn-xs label">EARLY RESUME</a>
     * Disabled → <a class="disabled btn bg-slate-400 btn-ladda btn-xs label">EARLY RESUME</a>
     * Opens a popdown with update_date_to input + Confirm button.
     *
     * @param childName exact child name as shown in Child Name column
     */
    public void clickEarlyResumeForChild(String childName) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//tr[.//a[contains(@class,'popdown_big')"
                        + " and normalize-space(text())='" + childName + "']]"
                        + "//a[contains(@class,'popdown_medium')"
                        + " and normalize-space(text())='EARLY RESUME']")));
        System.out.println("▶ Clicking EARLY RESUME for: " + childName);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(1000);
    }

    /**
     * Click EARLY RESUME — first enabled button in table.
     * Use after filtering by Admission ID so only one row is visible.
     */
    public void clickEarlyResume() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'popdown_medium')"
                        + " and contains(@class,'bg-blue')"
                        + " and normalize-space(text())='EARLY RESUME']")));
        System.out.println("▶ Clicking EARLY RESUME");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(1000);
    }

    /**
     * Click EXTEND PAUSE for a specific child row.
     * Same DOM pattern as Early Resume — popdown_medium class when enabled.
     *
     * @param childName exact child name as shown in Child Name column
     */
    public void clickExtendPauseForChild(String childName) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//tr[.//a[contains(@class,'popdown_big')"
                        + " and normalize-space(text())='" + childName + "']]"
                        + "//a[contains(@class,'popdown_medium')"
                        + " and normalize-space(text())='EXTEND PAUSE']")));
        System.out.println("▶ Clicking EXTEND PAUSE for: " + childName);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(1000);
    }

    /**
     * Click EXTEND PAUSE — first enabled button in table.
     */
    public void clickExtendPause() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//a[contains(@class,'popdown_medium')"
                        + " and contains(@class,'bg-blue')"
                        + " and normalize-space(text())='EXTEND PAUSE']")));
        System.out.println("▶ Clicking EXTEND PAUSE");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(1000);
    }

    /**
     * Returns true if EARLY RESUME button is ENABLED for a child.
     * Enabled = popdown_medium + bg-blue class.
     */
    public boolean isEarlyResumeEnabled(String childName) {
        return !driver.findElements(By.xpath(
                "//tr[.//a[contains(@class,'popdown_big')"
                        + " and normalize-space(text())='" + childName + "']]"
                        + "//a[contains(@class,'popdown_medium') and contains(@class,'bg-blue')"
                        + " and normalize-space(text())='EARLY RESUME']")).isEmpty();
    }

    /**
     * Returns true if EARLY RESUME button is DISABLED for a child.
     * Disabled = class contains 'disabled' + 'btn-ladda' (old-policy pause).
     * Verifies SC007_TC001 — old-policy pauses must show disabled buttons.
     */
    public boolean isEarlyResumeDisabled(String childName) {
        return !driver.findElements(By.xpath(
                "//tr[.//a[contains(@class,'popdown_big')"
                        + " and normalize-space(text())='" + childName + "']]"
                        + "//a[contains(@class,'disabled') and contains(@class,'btn-ladda')"
                        + " and normalize-space(text())='EARLY RESUME']")).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // CHILD ATTRITION — RETAIN, PRORATED INVOICE, APPROVE, UPDATE REQUEST
    // Confirmed from live DOM
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Returns true if EXTEND PAUSE button is ENABLED for a child.
     */
    public boolean isExtendPauseEnabled(String childName) {
        return !driver.findElements(By.xpath(
                "//tr[.//a[contains(@class,'popdown_big')"
                        + " and normalize-space(text())='" + childName + "']]"
                        + "//a[contains(@class,'popdown_medium') and contains(@class,'bg-blue')"
                        + " and normalize-space(text())='EXTEND PAUSE']")).isEmpty();
    }

    /**
     * Returns true if EXTEND PAUSE button is DISABLED for a child.
     */
    public boolean isExtendPauseDisabled(String childName) {
        return !driver.findElements(By.xpath(
                "//tr[.//a[contains(@class,'popdown_big')"
                        + " and normalize-space(text())='" + childName + "']]"
                        + "//a[contains(@class,'disabled') and contains(@class,'btn-ladda')"
                        + " and normalize-space(text())='EXTEND PAUSE']")).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────
    // TRANSPORT — APPROVE, REJECT
    // Confirmed from live DOM
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Full Early Resume flow:
     * filter by admId → click EARLY RESUME → set date → confirm
     *
     * @param admId      Admission ID to filter
     * @param resumeDate ISO date "YYYY-MM-DD"
     */
    public void doEarlyResume(String admId, String resumeDate)
            throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        clickEarlyResume();
        Thread.sleep(500);
        setExtendResumeDate(resumeDate);
        confirmExtendResume();
    }

    /**
     * Full Extend Pause flow:
     * filter by admId → click EXTEND PAUSE → set new end date → confirm
     *
     * @param admId      Admission ID to filter
     * @param newEndDate ISO date "YYYY-MM-DD"
     */
    public void doExtendPause(String admId, String newEndDate)
            throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        clickExtendPause();
        Thread.sleep(500);
        setExtendResumeDate(newEndDate);
        confirmExtendResume();
    }

    // ──────────────────────────────────────────────────────────────────────
    // GENERIC HELPERS — response message after any action
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Click RETAIN button — class="retained_attrition"  text="RETAIN"
     */
    public void clickRetain(String requestId) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.retained_attrition[request_id='" + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ RETAIN clicked, request_id=" + requestId);
    }

    /**
     * Click PRORATED MONTHLY INVOICE — class="prorated-invoice"
     */
    public void clickProratedMonthlyInvoice(String requestId) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.prorated-invoice[request_id='" + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ PRORATED MONTHLY INVOICE clicked, request_id=" + requestId);
    }

    // ══════════════════════════════════════════════════════════════════════
    // EARLY RESUME MODAL (SC004)
    // Confirmed from DOM: id="update_date_to"
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Click PRORATED ANNUAL INVOICE — class="prorated-annual-invoice"
     */
    public void clickProratedAnnualInvoice(String requestId) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.prorated-annual-invoice[request_id='" + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ PRORATED ANNUAL INVOICE clicked, request_id=" + requestId);
    }

    /**
     * Click APPROVE button — class="approve"  text="APPROVE"
     */
    public void clickApprove(String requestId) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.approve[request_id='" + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ APPROVE clicked, request_id=" + requestId);
    }

    /**
     * Click REJECT button — class="reject"  text="REJECT"
     */
    public void clickReject(String requestId) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.reject[request_id='" + requestId + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ REJECT clicked, request_id=" + requestId);
    }

    /**
     * Get response message after any button action (alert or toast).
     */
    public String getActionResponseMessage() {
        // Check JS alert first
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4))
                    .until(ExpectedConditions.alertIsPresent());
            return driver.switchTo().alert().getText().trim();
        } catch (Exception ignored) {
        }
        // Check toast/ajax message
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(6))
                    .until(ExpectedConditions.visibilityOfElementLocated(
                            By.cssSelector(".alert-ajax-response,.alert-message,.toast")))
                    .getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════

    public void acceptActionAlert() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Navigate directly using child_id URL parameter.
     * Used in Pause tests to verify a specific child's request record.
     */
    public void navigateByChildId(String childId) throws InterruptedException {
        String base = driver.getCurrentUrl().replaceAll("(https?://[^/]+).*", "$1");
        driver.get(base + "/recent_update_details?child_id=" + childId);
        System.out.println("▶ Navigated to Recent Customer Requests for child: " + childId);
        Thread.sleep(1500);
        wait.until(ExpectedConditions.visibilityOf(submitButton));
    }

    // ══════════════════════════════════════════════════════════════════════
    // FILTER ACTIONS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Enter Admission ID in the typeahead number input.
     * Clears first, then sends keys.
     */
    public void enterAdmissionId(String admId) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(admissionIdInput));
        admissionIdInput.clear();
        admissionIdInput.sendKeys(admId);
        Thread.sleep(300);
        System.out.println("✅ Admission ID entered: " + admId);
    }

    /**
     * Set From date using jQuery datepicker.
     * Uses JS to set value + trigger datepicker's onSelect.
     * Format: "DD MMM YYYY" e.g. "01 May 2026"
     * OR ISO "YYYY-MM-DD" e.g. "2026-05-01"
     */
    public void setFromDate(String date) throws InterruptedException {
        setDatePickerValue(fromDateInput, "date_from_submit", date);
        System.out.println("✅ From date: " + date);
    }

    /**
     * Set To date using jQuery datepicker.
     */
    public void setToDate(String date) throws InterruptedException {
        setDatePickerValue(toDateInput, "date_to_submit", date);
        System.out.println("✅ To date: " + date);
    }

    /**
     * Select center by visible text.
     */
    public void selectCenter(String center) {
        new Select(centerDropdown).selectByVisibleText(center);
        System.out.println("✅ Center: " + center);
    }

    /**
     * Select Request Type using the underlying multiselect <select> element.
     * The Bootstrap Multiselect renders checkboxes but the underlying
     * <select id="type"> can be manipulated directly via JS.
     */
    public void selectRequestType(String visibleText) {
        ((JavascriptExecutor) driver).executeScript(
                "var sel = arguments[0];" +
                        "for (var i=0; i<sel.options.length; i++) {" +
                        "   sel.options[i].selected = sel.options[i].text.trim() === arguments[1];" +
                        "}" +
                        "$(sel).multiselect('refresh');",
                requestTypeSelect, visibleText);
        System.out.println("✅ Request Type: " + visibleText);
    }

    /**
     * Select Status by visible text.
     * id="status"  options: All | Pending | Processing | Approved | Rejected
     */
    public void selectStatus(String status) {
        new Select(statusDropdown).selectByVisibleText(status);
        System.out.println("✅ Status: " + status);
    }

    /**
     * Select Admission Type by visible text.
     * id="admission_type"  options: Regular | Corporate
     */
    public void selectAdmissionType(String type) {
        new Select(admissionTypeDropdown).selectByVisibleText(type);
        System.out.println("✅ Admission Type: " + type);
    }

    /**
     * Select Support Executive by visible text.
     * id="request_owner"
     */
    public void selectSupportExecutive(String executive) {
        new Select(supportExecutiveDropdown).selectByVisibleText(executive);
        System.out.println("✅ Support Executive: " + executive);
    }

    /**
     * Click the Submit button (INPUT type=submit, name="submit_date").
     */
    public void clickSubmit() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", submitButton);
        Thread.sleep(1500);
        System.out.println("▶ Submit clicked");
    }

    // ══════════════════════════════════════════════════════════════════════
    // RESULTS ACTIONS
    // ══════════════════════════════════════════════════════════════════════

    public void clickDownload() {
        wait.until(ExpectedConditions.elementToBeClickable(downloadButton));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", downloadButton);
        System.out.println("▶ Download Data clicked");
    }

    public void searchInTable(String keyword) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(tableSearchInput));
        tableSearchInput.clear();
        tableSearchInput.sendKeys(keyword);
        Thread.sleep(800);
        System.out.println("▶ Table search: " + keyword);
    }

    public void scrollTableRight() {
        ((JavascriptExecutor) driver).executeScript(
                "var w = document.querySelector(" +
                        "'.dataTables_scrollBody,.table-responsive,.dataTables_wrapper');" +
                        "if(w) w.scrollLeft=1000;");
    }

    public void scrollTableLeft() {
        ((JavascriptExecutor) driver).executeScript(
                "var w = document.querySelector(" +
                        "'.dataTables_scrollBody,.table-responsive,.dataTables_wrapper');" +
                        "if(w) w.scrollLeft=0;");
    }

    // ══════════════════════════════════════════════════════════════════════
    // EARLY RESUME / EXTEND MODAL ACTIONS (SC003, SC004, SC005)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Set the date in the Extend/Resume modal date input.
     * id="update_date_to"
     *
     * @param isoDate "YYYY-MM-DD" e.g. "2026-04-20"
     */
    public void setExtendResumeDate(String isoDate) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(extendResumeDateInput));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].value = arguments[1];",
                extendResumeDateInput, isoDate);
        ((JavascriptExecutor) driver).executeScript(
                "var el=arguments[0]; var d=new Date(arguments[1]);" +
                        "if($(el).datepicker) $(el).datepicker('setDate', d);" +
                        "el.dispatchEvent(new Event('change',{bubbles:true}));" +
                        "el.dispatchEvent(new Event('input',{bubbles:true}));",
                extendResumeDateInput, isoDate);
        Thread.sleep(400);
        System.out.println("✅ Extend/Resume date set: " + isoDate);
    }

    /**
     * Confirm the Extend/Resume action in the modal.
     * class="btn-model-extension-cancel" (Confirm button — misleading class name)
     */
    public void confirmExtendResume() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(btnExtendConfirm));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", btnExtendConfirm);
        Thread.sleep(1000);
        System.out.println("✅ Extend/Resume confirmed");
    }

    /**
     * Confirm a Cancel Request action.
     * class="btn-model-cancel"
     */
    public void confirmCancelRequest() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(btnCancelConfirm));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", btnCancelConfirm);
        Thread.sleep(1000);
        System.out.println("✅ Cancel request confirmed");
    }

    /**
     * Close the modal without confirming.
     */
    public void closeModal() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(btnModalClose));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", btnModalClose);
        Thread.sleep(500);
        System.out.println("✅ Modal closed");
    }

    // ══════════════════════════════════════════════════════════════════════
    // ASSERTIONS / QUERIES
    // ══════════════════════════════════════════════════════════════════════

    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOf(submitButton));
            return submitButton.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDownloadButtonVisible() {
        try {
            // DataTables CSV button renders after results load — use short wait
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.visibilityOf(downloadButton));
            return downloadButton.isDisplayed();
        } catch (Exception e) {
            System.out.println("   Download Data button (a.buttons-csv) not visible");
            return false;
        }
    }

    public boolean isTableVisible() {
        try {
            return dataTable.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public int getRowCount() {
        try {
            List<WebElement> rows = driver.findElements(By.xpath(
                    "//table[contains(@class,'dataTable')]/tbody/tr" +
                            "[not(contains(@class,'dataTables_empty'))]"));
            if (rows.size() == 1) {
                String t = rows.get(0).getText().trim().toLowerCase();
                if (t.contains("no data") || t.contains("no records") || t.isEmpty())
                    return 0;
            }
            return rows.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public String getShowingEntriesText() {
        try {
            return showingEntriesText.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public String getCellValue(int row, int col) {
        try {
            return driver.findElement(By.xpath(
                    "//table[contains(@class,'dataTable')]/tbody/tr["
                            + row + "]/td[" + col + "]")).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean verifyHeaderExists(String headerText) {
        return !driver.findElements(By.xpath(
                "//table[contains(@class,'dataTable')]" +
                        "/thead/tr/th[normalize-space(.)='" + headerText + "']")).isEmpty();
    }

    public String getColumnValueForRow(int row, String columnHeader) {
        try {
            List<WebElement> headers = driver.findElements(By.xpath(
                    "//table[contains(@class,'dataTable')]/thead/tr/th"));
            int col = -1;
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).getText().trim().equals(columnHeader)) {
                    col = i + 1;
                    break;
                }
            }
            return col == -1 ? "" : getCellValue(row, col);
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isNoDataMessageVisible() {
        return !driver.findElements(By.xpath(
                        "//table/tbody/tr/td[contains(.,'No data') or contains(.,'No records')]"))
                .isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════
    // PAUSE-SPECIFIC HELPERS (called from ServiceRequest_PauseTest)
    // ══════════════════════════════════════════════════════════════════════

    public boolean isPauseRequestVisible(String admId, String requestType)
            throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        if (getRowCount() == 0) return false;
        List<WebElement> rows = driver.findElements(By.xpath(
                "//table[contains(@class,'dataTable')]/tbody/tr"));
        for (WebElement row : rows) {
            if (row.getText().toLowerCase().contains(requestType.toLowerCase()))
                return true;
        }
        return false;
    }

    public String getPauseRequestStatus(String admId) throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        if (getRowCount() == 0) return "";
        return getColumnValueForRow(1, "Request Status");
    }

    public String getPauseWEFDate(String admId) throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        if (getRowCount() == 0) return "";
        return getColumnValueForRow(1, "WEF Date");
    }

    public String getPauseEndDate(String admId) throws InterruptedException {
        enterAdmissionId(admId);
        clickSubmit();
        if (getRowCount() == 0) return "";
        return getColumnValueForRow(1, "End Date");
    }

    // ══════════════════════════════════════════════════════════════════════
    // EXTENDED DAYCARE-SPECIFIC HELPERS (called from ServiceRequest_ExtendedDaycareTest)
    // Filters by Admission ID via direct URL, then finds the row whose
    // "Request Type" column is "Extended Daycare" — a child's most recent
    // Customer Request row is not necessarily an Extended Daycare one, so
    // reading row 1 blindly can silently pick up an unrelated request type.
    // ══════════════════════════════════════════════════════════════════════

    private int findExtendedDaycareRow() {
        int rows = getRowCount();
        for (int row = 1; row <= rows; row++) {
            if ("Extended Daycare".equalsIgnoreCase(getColumnValueForRow(row, "Request Type"))) {
                return row;
            }
        }
        return -1;
    }

    public String getEDColumnValue(String admId, String columnHeader)
            throws InterruptedException {
        navigateByChildId(admId);
        int row = findExtendedDaycareRow();
        if (row == -1) return "";
        return getColumnValueForRow(row, columnHeader);
    }

    public String getEDRequestStatus(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "Request Status");
    }

    public String getEDApprovalStatus(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "Approval Status");
    }

    public String getEDCenterName(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "Center Name");
    }

    public String getEDWEFDate(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "WEF Date");
    }

    public String getEDEndDate(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "End Date");
    }

    public String getEDCreatedBy(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "Created By");
    }

    public String getEDSupportExecutive(String admId) throws InterruptedException {
        return getEDColumnValue(admId, "Support Executive");
    }

    /**
     * Returns true if the Actions column for this child's Extended Daycare
     * row contains a CANCEL control (Pending status — cancellable).
     */
    public boolean isEDCancelVisible(String admId) throws InterruptedException {
        String actions = getEDColumnValue(admId, "Actions");
        return actions != null && actions.toUpperCase().contains("CANCEL");
    }

    /**
     * Returns true if the Actions column for this child's Extended Daycare
     * row is empty (Approved/Completed status — no actions available).
     */
    public boolean isEDActionsEmpty(String admId) throws InterruptedException {
        String actions = getEDColumnValue(admId, "Actions");
        return actions == null || actions.trim().isEmpty();
    }

    // ══════════════════════════════════════════════════════════════════════
    // TIME EXTENSION-SPECIFIC HELPERS (called from ServiceRequest_TimeExtensionTest)
    // Filters by Admission ID via direct URL, then finds the row whose
    // "Request Type" column matches "Start Time Extension" or "Stop Time
    // Extension" — a child progresses through both request types in sequence,
    // so reading row 1 blindly could pick up the wrong one.
    //
    // Confirmed from live DOM: Approve/Reject buttons carry request_type as a
    // plain HTML attribute (id="approve_extension"/"reject_extention"), so they
    // can be located directly without first resolving a row/request_id — e.g.
    // #approve_extension[request_type='Start Time Extension'].
    // ══════════════════════════════════════════════════════════════════════

    private int findTimeExtensionRow(String requestType) {
        int rows = getRowCount();
        for (int row = 1; row <= rows; row++) {
            if (requestType.equalsIgnoreCase(getColumnValueForRow(row, "Request Type"))) {
                return row;
            }
        }
        return -1;
    }

    public String getTEColumnValue(String admId, String requestType, String columnHeader)
            throws InterruptedException {
        navigateByChildId(admId);
        int row = findTimeExtensionRow(requestType);
        if (row == -1) return "";
        return getColumnValueForRow(row, columnHeader);
    }

    public String getTERequestStatus(String admId, String requestType) throws InterruptedException {
        return getTEColumnValue(admId, requestType, "Request Status");
    }

    public String getTEApprovalStatus(String admId, String requestType) throws InterruptedException {
        return getTEColumnValue(admId, requestType, "Approval Status");
    }

    /**
     * Click the APPROVE button for a given request type ("Start Time Extension"
     * or "Stop Time Extension"). Opens the "Time Extension Request" summary
     * modal — call confirmTimeExtensionApproval() next.
     */
    public void clickApproveExtension(String requestType) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#approve_extension[request_type='" + requestType + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ APPROVE (Time Extension) clicked for request_type=" + requestType);
    }

    /**
     * Click the REJECT button for a given request type.
     */
    public void clickRejectExtension(String requestType) throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("#reject_extention[request_type='" + requestType + "']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        Thread.sleep(800);
        System.out.println("▶ REJECT (Time Extension) clicked for request_type=" + requestType);
    }

    /**
     * Confirm the approval on the "Time Extension Request" summary modal —
     * class="btn-model-extension-cancel" (misleading name — this IS Confirm).
     * Reuses the same generic modal already wired for Pause Extend/Resume.
     */
    public void confirmTimeExtensionApproval() throws InterruptedException {
        confirmExtendResume();
    }

    /**
     * Close the "Request Approved Successfully" state of the same modal
     * (its Confirm button is disabled once approved — Close is the only action).
     * The approval itself already completed via confirmTimeExtensionApproval()
     * + the native "Prorated Invoice..." confirm — observed live that the modal
     * does not reliably re-render with a Close button afterward, so this step
     * is best-effort cleanup only and must never fail the calling test.
     */
    public void closeTimeExtensionModal() throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(btnModalClose));
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", btnModalClose);
            Thread.sleep(500);
            System.out.println("✅ Time Extension modal closed");
        } catch (Exception e) {
            System.out.println("⚠ Close button not found within 8s — force-hiding modal via JS (approval already completed)");
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll('.modal.show, .modal[style*=\"display: block\"]')" +
                            ".forEach(function(m){ m.classList.remove('show'); m.style.display='none'; });" +
                            "document.querySelectorAll('.modal-backdrop').forEach(function(b){ b.remove(); });" +
                            "document.body.classList.remove('modal-open');");
            Thread.sleep(300);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE — jQuery datepicker value setter
    // This page uses jQuery UI Datepicker (hasDatepicker class), NOT Pickaday
    // ══════════════════════════════════════════════════════════════════════

    private void setDatePickerValue(WebElement field, String hiddenName, String date)
            throws InterruptedException {
        // Set visible input value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].value = arguments[1];",
                field, date);
        // Update jQuery datepicker internal state
        ((JavascriptExecutor) driver).executeScript(
                "try { $(arguments[0]).datepicker('setDate', arguments[1]); } catch(e) {}" +
                        "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));" +
                        "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));",
                field, date);
        // Also set the hidden submit field which is what the server reads
        ((JavascriptExecutor) driver).executeScript(
                "var h = document.querySelector(\"input[name='" + hiddenName + "']\");" +
                        "if(h) h.value = arguments[0];",
                date);
        Thread.sleep(300);
    }
}
