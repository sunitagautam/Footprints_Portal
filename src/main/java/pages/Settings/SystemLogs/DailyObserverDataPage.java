package pages.Settings.SystemLogs;

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
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// DAILY OBSERVER DATA (Settings → System Logs → Daily Observer Data)
// Screen: /daily_observer_data — selectors confirmed live
// (2026-10-21, see CLAUDE.md "Requirements — Daily Observer Data").
//
// ⚠ The Add/Update modal reuses ids of the page's filter form
//   (observer_type, observer_data_type) — every modal locator is
//   scoped to the popdown container #popdown-dialog.
// ═══════════════════════════════════════════════════════════════
public class DailyObserverDataPage {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // HEADER BUTTONS
    // ═══════════════════════════════════════════════
    // ✅ popdown_medium link → pop_daily_observer_data?pop=yes
    @FindBy(xpath = "//a[contains(@href,'pop_daily_observer_data') and normalize-space(.)='Add Observer Data']")
    private WebElement addObserverBtn;

    @FindBy(css = "button.run-cron-btn[data-type='billing']")
    private WebElement runBillingCronBtn;

    @FindBy(css = "button.run-cron-btn[data-type='non_billing']")
    private WebElement runNonBillingCronBtn;

    // ═══════════════════════════════════════════════
    // FILTER FORM (#daily_observer)
    // ═══════════════════════════════════════════════
    @FindBy(css = "#daily_observer #observer_type")
    private WebElement observerTypeFilter;

    @FindBy(css = "#daily_observer #query_search")
    private WebElement querySearchFilter;

    @FindBy(css = "#daily_observer #observer_data_type")
    private WebElement dataTypeFilter;

    @FindBy(css = "#daily_observer #observer_category")
    private WebElement categoryFilter;

    @FindBy(css = "#daily_observer #observer_occur_on")
    private WebElement frequencyFilter;

    @FindBy(css = "#daily_observer #daily_status")
    private WebElement statusFilter;

    @FindBy(css = "#daily_observer #executed_status")
    private WebElement execStatusFilter;

    @FindBy(css = "#daily_observer #btn_submit")
    private WebElement filterBtn;

    // ═══════════════════════════════════════════════
    // TABLE (DataTables, 20 per page, default order = hidden
    // Observer ID column desc → newest first)
    // ═══════════════════════════════════════════════
    @FindBy(id = "DataTables_Table_0")
    private WebElement table;

    @FindBy(css = "#DataTables_Table_0_filter input[type='search']")
    private WebElement tableSearchInput;

    @FindBy(id = "DataTables_Table_0_info")
    private WebElement tableInfo;

    @FindBy(css = "a.buttons-csv")
    private WebElement downloadBtn;

    @FindBy(id = "DataTables_Table_0_paginate")
    private WebElement pagination;

    @FindBy(id = "DataTables_Table_0_next")
    private WebElement nextPageBtn;

    @FindBy(id = "DataTables_Table_0_previous")
    private WebElement previousPageBtn;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public DailyObserverDataPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private boolean isShown(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // PAGE
    // ═══════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOf(tableInfo));
            return driver.getCurrentUrl().contains("daily_observer_data");
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Page heading text "Daily Observer Data"
    public boolean isHeadingVisible() {
        return !driver.findElements(By.xpath(
                "//*[self::h1 or self::h2 or self::h3 or self::h4 or self::h5 or self::span or self::div]"
                        + "[normalize-space(text())='Daily Observer Data']")).isEmpty();
    }

    public boolean isAddObserverVisible()     { return isShown(addObserverBtn); }
    public boolean isRunBillingCronVisible()  { return isShown(runBillingCronBtn); }
    public boolean isRunNonBillingCronVisible() { return isShown(runNonBillingCronBtn); }
    public boolean isObserverTypeFilterVisible() { return isShown(observerTypeFilter); }
    public boolean isQuerySearchFilterVisible()  { return isShown(querySearchFilter); }
    public boolean isFilterButtonVisible()    { return isShown(filterBtn); }
    public boolean isSearchBarVisible()       { return isShown(tableSearchInput); }
    public boolean isDownloadVisible()        { return isShown(downloadBtn); }
    public boolean isPaginationVisible()      { return isShown(pagination); }

    public List<String> getDataTypeOptions()   { return optionTexts(dataTypeFilter); }
    public List<String> getCategoryOptions()   { return optionTexts(categoryFilter); }
    public List<String> getFrequencyOptions()  { return optionTexts(frequencyFilter); }
    public List<String> getStatusOptions()     { return optionTexts(statusFilter); }
    public List<String> getExecStatusOptions() { return optionTexts(execStatusFilter); }

    private List<String> optionTexts(WebElement select) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(select).getOptions())
            out.add(o.getAttribute("textContent").trim());
        return out;
    }

    // ✅ All user-facing columns — excludes the hidden
    //    "Observer ID" column (class "never")
    public List<String> getDefinedColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : table.findElements(By.cssSelector("thead th"))) {
            if (!th.getAttribute("class").contains("never"))
                out.add(th.getAttribute("textContent").trim());
        }
        return out;
    }

    // ═══════════════════════════════════════════════
    // FILTERS — the form posts back to the same page
    // ═══════════════════════════════════════════════
    public void enterObserverTypeFilter(String text) {
        observerTypeFilter.clear();
        observerTypeFilter.sendKeys(text);
    }

    public void enterQuerySearchFilter(String text) {
        querySearchFilter.clear();
        querySearchFilter.sendKeys(text);
    }

    // ✅ By visible text, e.g. "Billing", "Alert", "Daily", "Inactive", "Success"
    public void selectDataType(String text)   { new Select(dataTypeFilter).selectByVisibleText(text); }
    public void selectCategory(String text)   { new Select(categoryFilter).selectByVisibleText(text); }
    public void selectFrequency(String text)  { new Select(frequencyFilter).selectByVisibleText(text); }
    public void selectStatus(String text)     { new Select(statusFilter).selectByVisibleText(text); }
    public void selectExecStatus(String text) { new Select(execStatusFilter).selectByVisibleText(text); }

    public String getSelectedDataType()   { return new Select(dataTypeFilter).getFirstSelectedOption().getText().trim(); }
    public String getSelectedCategory()   { return new Select(categoryFilter).getFirstSelectedOption().getText().trim(); }
    public String getSelectedFrequency()  { return new Select(frequencyFilter).getFirstSelectedOption().getText().trim(); }
    public String getSelectedStatus()     { return new Select(statusFilter).getFirstSelectedOption().getText().trim(); }
    public String getSelectedExecStatus() { return new Select(execStatusFilter).getFirstSelectedOption().getText().trim(); }
    public String getObserverTypeFilterValue() { return observerTypeFilter.getAttribute("value"); }

    public void clickFilter() throws InterruptedException {
        js().executeScript("arguments[0].click();", filterBtn);
        System.out.println("▶ Filter clicked");
        Thread.sleep(1500);
        PageFactory.initElements(driver, this);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("DataTables_Table_0_info")));
    }

    // ═══════════════════════════════════════════════
    // TABLE READING
    // ═══════════════════════════════════════════════
    public String getInfoText() {
        return driver.findElement(By.id("DataTables_Table_0_info")).getText().trim();
    }

    // ✅ Total from "Showing 1 to 20 of 186 entries" (0 when empty)
    public int getTotalEntries() {
        String info = getInfoText().replace(",", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("of (\\d+) entries").matcher(info);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    public boolean isTableEmpty() {
        return !driver.findElements(By.cssSelector("#DataTables_Table_0 tbody td.dataTables_empty")).isEmpty();
    }

    public String getEmptyTableMessage() {
        List<WebElement> e = driver.findElements(By.cssSelector("#DataTables_Table_0 tbody td.dataTables_empty"));
        return e.isEmpty() ? "" : e.get(0).getText().trim();
    }

    public int getVisibleRowCount() {
        if (isTableEmpty()) return 0;
        return driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr[role='row']")).size();
    }

    // ✅ Reads cell text by header — uses textContent so cells
    //    folded by the responsive layout are still read
    public List<String> getColumnValues(String header) {
        int idx = headerIndex(header);
        List<String> out = new ArrayList<>();
        if (isTableEmpty()) return out;
        for (WebElement row : driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr[role='row']"))) {
            List<WebElement> tds = row.findElements(By.xpath("./td"));
            if (tds.size() > idx)
                out.add(tds.get(idx).getAttribute("textContent").replaceAll("\\s+", " ").trim());
        }
        return out;
    }

    private int headerIndex(String header) {
        List<WebElement> ths = driver.findElements(By.cssSelector("#DataTables_Table_0 thead th"));
        for (int i = 0; i < ths.size(); i++)
            if (ths.get(i).getAttribute("textContent").trim().equalsIgnoreCase(header)) return i;
        throw new IllegalArgumentException("Column not found: " + header);
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement input = driver.findElement(By.cssSelector("#DataTables_Table_0_filter input[type='search']"));
        input.clear();
        input.sendKeys(keyword);
        System.out.println("▶ Searched: " + keyword);
        Thread.sleep(1000);
    }

    public void clearSearch() throws InterruptedException {
        js().executeScript("$('#DataTables_Table_0').DataTable().search('').draw();");
        driver.findElement(By.cssSelector("#DataTables_Table_0_filter input[type='search']")).clear();
        Thread.sleep(800);
    }

    // ✅ Row id = the "tr-<observerId>" class
    public String getObserverIdOfFirstRowMatching(String observerType) {
        for (WebElement row : driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr[role='row']"))) {
            String type = row.findElement(By.xpath("./td[1]")).getAttribute("textContent").trim();
            if (type.equals(observerType)) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("tr-(\\d+)").matcher(row.getAttribute("class"));
                if (m.find()) return m.group(1);
            }
        }
        return "";
    }

    // ✅ Full text of one observer's row (by observer id), each cell " | "-joined
    public String getRowTextById(String observerId) {
        List<WebElement> rows = driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr.tr-" + observerId));
        if (rows.isEmpty()) return "";
        List<String> cells = new ArrayList<>();
        for (WebElement td : rows.get(0).findElements(By.xpath("./td")))
            cells.add(td.getAttribute("textContent").replaceAll("\\s+", " ").trim());
        return String.join(" | ", cells);
    }

    public String getCellById(String observerId, String header) {
        List<WebElement> rows = driver.findElements(By.cssSelector("#DataTables_Table_0 tbody tr.tr-" + observerId));
        if (rows.isEmpty()) return "";
        return rows.get(0).findElements(By.xpath("./td")).get(headerIndex(header))
                .getAttribute("textContent").replaceAll("\\s+", " ").trim();
    }

    // ═══════════════════════════════════════════════
    // DOWNLOAD / PAGINATION / SORTING
    // ═══════════════════════════════════════════════
    public void clickDownload() throws InterruptedException {
        js().executeScript("arguments[0].click();", downloadBtn);
        System.out.println("▶ Download clicked");
        Thread.sleep(3000);
    }

    public boolean isNextPageEnabled() {
        return !driver.findElement(By.id("DataTables_Table_0_next")).getAttribute("class").contains("disabled");
    }

    public boolean isPreviousPageEnabled() {
        return !driver.findElement(By.id("DataTables_Table_0_previous")).getAttribute("class").contains("disabled");
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.id("DataTables_Table_0_next")));
        System.out.println("▶ Next page clicked");
        Thread.sleep(1000);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.id("DataTables_Table_0_previous")));
        System.out.println("▶ Previous page clicked");
        Thread.sleep(1000);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        WebElement th = driver.findElements(By.cssSelector("#DataTables_Table_0 thead th")).get(headerIndex(header));
        js().executeScript("arguments[0].click();", th);
        System.out.println("▶ Sorted by: " + header);
        Thread.sleep(1000);
    }

    // ✅ "sorting_asc" / "sorting_desc" / "sorting"
    public String getHeaderSortClass(String header) {
        return driver.findElements(By.cssSelector("#DataTables_Table_0 thead th"))
                .get(headerIndex(header)).getAttribute("class");
    }

    // ═══════════════════════════════════════════════
    // ADD / UPDATE OBSERVER MODAL (#popdown-dialog)
    // ═══════════════════════════════════════════════
    private WebElement modal() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("popdown-dialog")));
    }

    private WebElement inModal(String id) {
        return modal().findElement(By.id(id));
    }

    public void clickAddObserver() {
        wait.until(ExpectedConditions.elementToBeClickable(addObserverBtn));
        js().executeScript("arguments[0].click();", addObserverBtn);
        waitForModalTitle("Add Observer Data");
        System.out.println("▶ Add Observer Data modal opened");
    }

    // ✅ Update = pencil icon, href pop_daily_observer_data?pop=yes&observer_id=<base64 id>
    public void clickUpdate(String observerId) {
        WebElement link = driver.findElement(By.cssSelector(
                "#DataTables_Table_0 tbody tr.tr-" + observerId + " a[title='Update']"));
        js().executeScript("arguments[0].click();", link);
        waitForModalTitle("Update Observer Data");
        System.out.println("▶ Update Observer Data modal opened for #" + observerId);
    }

    private void waitForModalTitle(String title) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//div[@id='popdown-dialog']//h4[contains(@class,'modal-title')][normalize-space(.)='" + title + "']")));
        // ✅ Validator is bound on document.ready of the loaded popup
        try { Thread.sleep(1000); } catch (InterruptedException ignored) { }
    }

    public String getModalTitle() {
        return modal().findElement(By.cssSelector("h4.modal-title")).getText().trim();
    }

    public boolean isModalCloseIconVisible() {
        return isShown(modal().findElement(By.cssSelector("button.close-popdown")));
    }

    public boolean isModalFieldVisible(String id) {
        try {
            return inModal(id).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Placeholders hint under Query
    public String getQueryPlaceholderHint() {
        return modal().findElement(By.xpath(".//textarea[@id='query_original']/following-sibling::small"))
                .getText().trim();
    }

    public String getModalFieldValue(String id) {
        return inModal(id).getAttribute("value");
    }

    public String getModalSelectedText(String id) {
        return new Select(inModal(id)).getFirstSelectedOption().getAttribute("textContent").trim();
    }

    public List<String> getModalOptions(String id) {
        return optionTexts(inModal(id));
    }

    public String getModalSubmitLabel() {
        return inModal("btn_save_data").getAttribute("value");
    }

    public void enterModalObserverType(String text) {
        WebElement el = inModal("observer_type");
        el.clear();
        el.sendKeys(text);
    }

    public void enterModalQuery(String query) {
        WebElement el = inModal("query_original");
        el.clear();
        el.sendKeys(query);
    }

    // ✅ By option value: data type billing|non_billing, category Alert|Log,
    //    frequency Daily|...|Custom_Date|Custom_Range, status Active|Inactive,
    //    schedule active 1|0
    public void selectModalDataType(String value)      { new Select(inModal("observer_data_type")).selectByValue(value); }
    public void selectModalCategory(String value)      { new Select(inModal("data_category")).selectByValue(value); }
    public void selectModalFrequency(String value)     { new Select(inModal("observer_occur_on_opt")).selectByValue(value); }
    public void selectModalStatus(String value)        { new Select(inModal("data_status")).selectByValue(value); }
    public void selectModalScheduleActive(String value) { new Select(inModal("observer_is_active")).selectByValue(value); }

    public boolean isCustomScheduleVisible() {
        return isShown(inModal("custom_schedule_row"));
    }

    public String getCustomScheduleHint() {
        return inModal("custom_schedule_hint").getText().trim();
    }

    public void enterCustomSchedule(String json) {
        WebElement el = inModal("observer_custom_schedule");
        el.clear();
        el.sendKeys(json);
    }

    // ✅ Installs an ajaxComplete hook so a test can tell whether
    //    Submit actually sent the save request
    public void startAjaxCapture() {
        js().executeScript("window.__qaAjax=[]; $(document).off('ajaxComplete.qa').on('ajaxComplete.qa',"
                + "function(e,x,o){window.__qaAjax.push(o.url+' => '+x.status+' '+(x.responseText||''));});");
    }

    @SuppressWarnings("unchecked")
    public List<String> getCapturedAjax() {
        Object r = js().executeScript("return window.__qaAjax || [];");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    public void clickModalSubmit() {
        js().executeScript("arguments[0].click();", inModal("btn_save_data"));
        System.out.println("▶ Modal Submit clicked");
    }

    // ✅ jQuery-validate labels (label.text-danger[for=<field>]),
    //    "field => message" for the VISIBLE ones only
    public List<String> getVisibleModalErrors() {
        List<String> out = new ArrayList<>();
        for (WebElement e : modal().findElements(By.cssSelector("label.text-danger")))
            if (e.isDisplayed() && !e.getText().trim().isEmpty())
                out.add(e.getAttribute("for") + " => " + e.getText().trim());
        return out;
    }

    // ✅ Every validation label, shown or hidden
    public List<String> getAllModalErrors() {
        List<String> out = new ArrayList<>();
        for (WebElement e : modal().findElements(By.cssSelector("label.text-danger"))) {
            String t = e.getAttribute("textContent").trim();
            if (!t.isEmpty()) out.add(e.getAttribute("for") + " => " + t + (e.isDisplayed() ? "" : " (hidden)"));
        }
        return out;
    }

    // ✅ flash_message renders into .success-msg; page reloads 1.5s after ok
    public String waitForModalMessage(int seconds) throws InterruptedException {
        for (int i = 0; i < seconds * 10; i++) {
            try {
                // ✅ flash_message may render into a sibling .flash-msg, not inside .success-msg
                for (WebElement m : driver.findElements(By.cssSelector(
                        "#popdown-dialog .success-msg, #popdown-dialog .flash-msg, .flash-msg .alert"))) {
                    String t = m.getText().trim();
                    if (!t.isEmpty()) return t;
                }
            } catch (Exception ignored) { }
            Thread.sleep(100);
        }
        return "";
    }

    public void closeModal() throws InterruptedException {
        try {
            js().executeScript("arguments[0].click();",
                    driver.findElement(By.cssSelector("#popdown-dialog button.close-popdown")));
        } catch (Exception ignored) { }
        Thread.sleep(800);
        js().executeScript("var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';");
    }

    // ═══════════════════════════════════════════════
    // EXECUTE / RUN CRON — native confirm() + blockUI overlay message
    // ═══════════════════════════════════════════════
    public void clickExecute(String observerId) {
        WebElement link = driver.findElement(By.xpath(
                "//table[@id='DataTables_Table_0']//tr[contains(concat(' ',@class,' '),' tr-" + observerId
                        + " ')]//i[@title='Execute']/parent::a"));
        js().executeScript("arguments[0].click();", link);
        System.out.println("▶ Execute clicked for #" + observerId);
    }

    public void clickRunBillingCron() {
        js().executeScript("arguments[0].click();", runBillingCronBtn);
        System.out.println("▶ Run Billing Cron clicked");
    }

    public void clickRunNonBillingCron() {
        js().executeScript("arguments[0].click();", runNonBillingCronBtn);
        System.out.println("▶ Run Non-Billing Cron clicked");
    }

    public String acceptConfirm() {
        Alert a = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.alertIsPresent());
        String t = a.getText();
        a.accept();
        return t;
    }

    public String dismissConfirm() {
        Alert a = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.alertIsPresent());
        String t = a.getText();
        a.dismiss();
        return t;
    }

    // ✅ First overlay message that is not the "Please Wait" one
    public String waitForOverlayResult(int seconds) throws InterruptedException {
        for (int i = 0; i < seconds * 5; i++) {
            try {
                List<WebElement> b = driver.findElements(By.cssSelector(".blockMsg"));
                if (!b.isEmpty()) {
                    String t = b.get(0).getText().trim();
                    if (!t.isEmpty() && !t.contains("Please Wait")) return t;
                }
            } catch (Exception ignored) { }
            Thread.sleep(200);
        }
        return "";
    }

    public boolean isOverlayShown() {
        return !driver.findElements(By.cssSelector(".blockMsg")).isEmpty();
    }

    // ✅ Re-open the screen fresh (after the page's own reload)
    public void reload() throws InterruptedException {
        driver.get(IAutoConstant.DAILY_OBSERVER_DATA_URL);
        Thread.sleep(2000);
        PageFactory.initElements(driver, this);
        isPageLoaded();
    }
}
