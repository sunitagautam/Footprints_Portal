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
    // EARLY RESUME MODAL (SC004)
    // Confirmed from DOM: id="update_date_to"
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

    // ══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════

    public RecentCustomerRequestsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        PageFactory.initElements(driver, this);
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
