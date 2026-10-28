package pages.Franchise.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
// TIE-UPS DETAILS (Franchise → Corporate Tie-ups → Tie Ups Details)
// Screens: /corporate_offers (list) and /add_corporate_offer
// (Add, or Update with ?offer_id=<base64 id>). Selectors confirmed
// live 2026-10-26 as Rakesh (see CLAUDE.md "Requirements —
// Franchise → Corporate Tie-ups").
//
// ⚠ The list table has hidden "never" duplicate columns (Tie-up
//   Title, Centers List, Contact Details) and export-only columns —
//   header lookups take the FIRST match, which is the visible one.
// ═══════════════════════════════════════════════════════════════
public class Tieup_DetailsPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String TABLE = "#datatable-offers-list";

    // ═══════════════════════════════════════════════
    // LIST — HEADER / FILTERS
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//button[contains(@onclick,'add_corporate_offer') and contains(normalize-space(.),'Add New Corporate Offer')]")
    private WebElement addNewOfferBtn;

    // ✅ select2 — set via jQuery
    @FindBy(id = "sr_center_id")
    private WebElement centerFilter;

    // ✅ [--All Status--, Active, Expired], default Active
    @FindBy(id = "sr_status")
    private WebElement statusFilter;

    @FindBy(id = "btn_submit")
    private WebElement filterBtn;

    @FindBy(xpath = "//a[@href='corporate_offers' and normalize-space(.)='Clear']")
    private WebElement clearLink;

    // ═══════════════════════════════════════════════
    // LIST — TABLE (DataTables, 10 per page)
    // ═══════════════════════════════════════════════
    @FindBy(css = "#datatable-offers-list_filter input[type='search']")
    private WebElement tableSearchInput;

    @FindBy(css = "select[name='datatable-offers-list_length']")
    private WebElement pageLengthSelect;

    @FindBy(css = "a.buttons-csv[aria-controls='datatable-offers-list']")
    private WebElement downloadBtn;

    @FindBy(id = "datatable-offers-list_paginate")
    private WebElement pagination;

    // ═══════════════════════════════════════════════
    // ADD / UPDATE FORM (#offer_form)
    // ═══════════════════════════════════════════════
    @FindBy(id = "create_offer")
    private WebElement submitBtn;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public Tieup_DetailsPage(WebDriver driver) {
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

    private boolean isShown(By by) {
        for (WebElement e : driver.findElements(by))
            if (isShown(e)) return true;
        return false;
    }

    private List<String> optionTexts(WebElement select) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(select).getOptions())
            out.add(o.getAttribute("textContent").trim());
        return out;
    }

    private String text(WebElement e) {
        return e.getAttribute("textContent").replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
    }

    // ═══════════════════════════════════════════════
    // LIST PAGE
    // ═══════════════════════════════════════════════
    public boolean isListLoaded() {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("datatable-offers-list_info")));
            return driver.getCurrentUrl().contains("corporate_offers");
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ "Corporate Tie-ups" heading
    public boolean isHeadingVisible() {
        return isShown(By.xpath("//*[normalize-space(text())='Corporate Tie-ups']"));
    }

    // ✅ "<N> tie-ups" count next to the heading (-1 when not found)
    public int getTieupsCount() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) tie-ups")
                .matcher(driver.findElement(By.tagName("body")).getText());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public boolean isAddNewOfferVisible()  { return isShown(addNewOfferBtn); }
    public boolean isCenterFilterPresent() { return !driver.findElements(By.id("sr_center_id")).isEmpty(); }
    public boolean isFilterButtonVisible() { return isShown(filterBtn); }
    public boolean isClearLinkVisible()    { return isShown(clearLink); }
    public boolean isSearchBarVisible()    { return isShown(tableSearchInput); }
    public boolean isDownloadVisible()     { return isShown(downloadBtn); }
    public boolean isPaginationVisible()   { return isShown(pagination); }

    public String getSearchPlaceholder()   { return tableSearchInput.getAttribute("placeholder"); }
    public List<String> getStatusOptions() { return optionTexts(statusFilter); }
    public List<String> getPageLengthOptions() { return optionTexts(pageLengthSelect); }

    public String getSelectedStatus() {
        return new Select(statusFilter).getFirstSelectedOption().getText().trim();
    }

    public String getSelectedCenterText() {
        return String.valueOf(js().executeScript(
                "var s=document.getElementById('sr_center_id'); return s.selectedIndex<0?'':s.options[s.selectedIndex].text.trim();"));
    }

    public String getFirstCenterOption() {
        return new Select(centerFilter).getOptions().get(0).getAttribute("textContent").trim();
    }

    public void selectStatus(String text) {
        new Select(statusFilter).selectByVisibleText(text);
    }

    // ✅ select2 — select by visible text and fire change
    public void selectCenter(String centerName) {
        Object ok = js().executeScript(
                "var t=arguments[0], s=jQuery('#sr_center_id');"
                        + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                        + "if(!o.length) return false; s.val(o.first().val()).trigger('change'); return true;", centerName);
        if (!Boolean.TRUE.equals(ok))
            throw new IllegalArgumentException("Center not in filter: " + centerName);
        System.out.println("▶ Center selected: " + centerName);
    }

    public void clickFilter() throws InterruptedException {
        js().executeScript("arguments[0].click();", filterBtn);
        System.out.println("▶ Filter clicked");
        waitForTable();
    }

    public void clickClear() throws InterruptedException {
        js().executeScript("arguments[0].click();", clearLink);
        System.out.println("▶ Clear clicked");
        waitForTable();
    }

    private void waitForTable() throws InterruptedException {
        Thread.sleep(2500);
        PageFactory.initElements(driver, this);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("datatable-offers-list_info")));
    }

    // ═══════════════════════════════════════════════
    // TABLE READING
    // ═══════════════════════════════════════════════
    // ✅ Visible headers only (excludes class "never")
    public List<String> getVisibleColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : driver.findElements(By.cssSelector(TABLE + " thead th")))
            if (!th.getAttribute("class").contains("never"))
                out.add(th.getAttribute("textContent").trim());
        return out;
    }

    // ✅ Export-only headers (class "never")
    public List<String> getHiddenColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : driver.findElements(By.cssSelector(TABLE + " thead th")))
            if (th.getAttribute("class").contains("never"))
                out.add(th.getAttribute("textContent").replace(' ', ' ').trim());
        return out;
    }

    // ✅ "Showing 1–10 of 125 entries"
    public String getInfoText() {
        return driver.findElement(By.id("datatable-offers-list_info")).getText().trim();
    }

    public int getTotalEntries() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("of (\\d+) entries")
                .matcher(getInfoText().replace(",", ""));
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    public boolean isTableEmpty() {
        return !driver.findElements(By.cssSelector(TABLE + " tbody td.dataTables_empty")).isEmpty();
    }

    public String getEmptyTableMessage() {
        List<WebElement> e = driver.findElements(By.cssSelector(TABLE + " tbody td.dataTables_empty"));
        return e.isEmpty() ? "" : e.get(0).getText().trim();
    }

    private List<WebElement> rows() {
        if (isTableEmpty()) return new ArrayList<>();
        return driver.findElements(By.cssSelector(TABLE + " tbody tr[role='row']"));
    }

    public int getVisibleRowCount() {
        return rows().size();
    }

    // ✅ First header with this text (= the visible one)
    private int headerIndex(String header) {
        List<WebElement> ths = driver.findElements(By.cssSelector(TABLE + " thead th"));
        for (int i = 0; i < ths.size(); i++)
            if (ths.get(i).getAttribute("textContent").replace(' ', ' ').trim().equalsIgnoreCase(header)) return i;
        throw new IllegalArgumentException("Column not found: " + header);
    }

    // ✅ The n-th (0-based) header with this text — e.g. 1 = the hidden full "Centers List"
    private int headerIndex(String header, int occurrence) {
        List<WebElement> ths = driver.findElements(By.cssSelector(TABLE + " thead th"));
        int seen = 0;
        for (int i = 0; i < ths.size(); i++)
            if (ths.get(i).getAttribute("textContent").replace(' ', ' ').trim().equalsIgnoreCase(header) && seen++ == occurrence)
                return i;
        throw new IllegalArgumentException("Column not found: " + header + " #" + occurrence);
    }

    public List<String> getColumnValues(String header) {
        return getColumnValues(header, 0);
    }

    public List<String> getColumnValues(String header, int occurrence) {
        int idx = headerIndex(header, occurrence);
        List<String> out = new ArrayList<>();
        for (WebElement row : rows()) {
            List<WebElement> tds = row.findElements(By.xpath("./td"));
            if (tds.size() > idx) out.add(text(tds.get(idx)));
        }
        return out;
    }

    // ✅ Every row's full centre list: the "+N more" link's data-centers
    //    JSON, or the cell text when the tie-up has a single centre / "All Centers…"
    public List<String> getRowsFullCenters() {
        List<String> out = new ArrayList<>();
        int idx = headerIndex("Centers List");
        for (WebElement row : rows()) {
            WebElement td = row.findElements(By.xpath("./td")).get(idx);
            List<WebElement> more = td.findElements(By.cssSelector("a.open-centers-modal"));
            out.add(more.isEmpty() ? text(td) : more.get(0).getAttribute("data-centers"));
        }
        return out;
    }

    public void searchTable(String keyword) throws InterruptedException {
        tableSearchInput.clear();
        tableSearchInput.sendKeys(keyword);
        System.out.println("▶ Searched: " + keyword);
        Thread.sleep(1200);
    }

    public void clearSearch() throws InterruptedException {
        js().executeScript("jQuery('" + TABLE + "').DataTable().search('').draw();");
        tableSearchInput.clear();
        Thread.sleep(800);
    }

    public void selectPageLength(String value) throws InterruptedException {
        new Select(pageLengthSelect).selectByValue(value);
        System.out.println("▶ Per page: " + value);
        Thread.sleep(1200);
    }

    public void clickDownload() throws InterruptedException {
        js().executeScript("arguments[0].click();", downloadBtn);
        System.out.println("▶ Download clicked");
        Thread.sleep(3000);
    }

    public boolean isNextPageEnabled() {
        return !driver.findElement(By.id("datatable-offers-list_next")).getAttribute("class").contains("disabled");
    }

    public boolean isPreviousPageEnabled() {
        return !driver.findElement(By.id("datatable-offers-list_previous")).getAttribute("class").contains("disabled");
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.id("datatable-offers-list_next")));
        System.out.println("▶ Next page clicked");
        Thread.sleep(1000);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.id("datatable-offers-list_previous")));
        System.out.println("▶ Previous page clicked");
        Thread.sleep(1000);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        WebElement th = driver.findElements(By.cssSelector(TABLE + " thead th")).get(headerIndex(header));
        js().executeScript("arguments[0].click();", th);
        System.out.println("▶ Sorted by: " + header);
        Thread.sleep(1000);
    }

    // ✅ "sorting_asc" / "sorting_desc" / "sorting"
    public String getHeaderSortClass(String header) {
        return driver.findElements(By.cssSelector(TABLE + " thead th")).get(headerIndex(header)).getAttribute("class");
    }

    // ═══════════════════════════════════════════════
    // ROW ACTIONS (first row)
    // ═══════════════════════════════════════════════
    private WebElement firstRow() {
        return driver.findElement(By.cssSelector(TABLE + " tbody tr[role='row']"));
    }

    // ✅ Icons present in the first row's Actions cell
    public List<String> getFirstRowActionTitles() {
        List<String> out = new ArrayList<>();
        WebElement row = firstRow();
        if (!row.findElements(By.cssSelector("a[href^='add_corporate_offer?offer_id=']")).isEmpty()) out.add("Update");
        if (!row.findElements(By.cssSelector("a[href*='view_history']")).isEmpty()) out.add("Tieup History Log");
        if (!row.findElements(By.cssSelector("a[href*='view_corporate_offer']")).isEmpty()) out.add("View Offer");
        if (!row.findElements(By.cssSelector("a[href*='admission_request']")).isEmpty()) out.add("Admission Request");
        return out;
    }

    public String getFirstRowTitle() {
        return text(firstRow().findElement(By.xpath("./td[1]")));
    }

    // ✅ Update link of the first row → add_corporate_offer?offer_id=<base64>
    public String getFirstRowUpdateHref() {
        return firstRow().findElement(By.cssSelector("a[href^='add_corporate_offer?offer_id=']")).getAttribute("href");
    }

    private void clickInFirstRow(String css) throws InterruptedException {
        js().executeScript("arguments[0].click();", firstRow().findElement(By.cssSelector(css)));
        Thread.sleep(3000);
    }

    public void clickFirstRowViewOffer() throws InterruptedException        { clickInFirstRow("a[href*='view_corporate_offer']"); }
    public void clickFirstRowHistoryLog() throws InterruptedException       { clickInFirstRow("a[href*='view_history']"); }
    public void clickFirstRowAdmissionRequest() throws InterruptedException { clickInFirstRow("a[href*='admission_request']"); }

    // ✅ (i) icon next to the title — hover popover, content in data-content
    public String getFirstRowInfoContent() {
        WebElement info = firstRow().findElement(By.cssSelector("label[data-popup]"));
        String html = info.getAttribute("data-content");
        return html == null ? "" : html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    public boolean hoverFirstRowInfoIcon() throws InterruptedException {
        WebElement info = firstRow().findElement(By.cssSelector("label[data-popup]"));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", info);
        new Actions(driver).moveToElement(info).perform();
        Thread.sleep(1200);
        return isShown(By.cssSelector(".popover"));
    }

    // ═══════════════════════════════════════════════
    // "+N MORE" CENTRES MODAL (#modal-centers-list)
    // ═══════════════════════════════════════════════
    // ✅ First "+N more" link on the page → N (-1 when none). The Centers
    //    List column can be folded by the responsive layout, so no
    //    visibility check — the link is clicked via JS.
    public int getFirstMoreCentersCount() {
        List<WebElement> links = driver.findElements(By.cssSelector(TABLE + " a.open-centers-modal"));
        for (WebElement a : links) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\+(\\d+) more").matcher(text(a));
            if (m.find()) return Integer.parseInt(m.group(1));
        }
        return -1;
    }

    public void clickFirstMoreCenters() throws InterruptedException {
        for (WebElement a : driver.findElements(By.cssSelector(TABLE + " a.open-centers-modal"))) {
            if (text(a).matches(".*\\+\\d+ more.*")) {
                js().executeScript("arguments[0].click();", a);
                Thread.sleep(1500);
                return;
            }
        }
        throw new IllegalStateException("No '+N more' link on this page");
    }

    public boolean isCentersModalVisible() { return isShown(By.id("modal-centers-list")); }

    public String getCentersModalTitle() {
        return text(driver.findElement(By.cssSelector("#modal-centers-list .modal-title")));
    }

    public int getCentersModalCount() {
        return Integer.parseInt(text(driver.findElement(By.id("modal-centers-count"))));
    }

    public int getCentersModalListSize() {
        return driver.findElements(By.cssSelector("#centers-modal-list li")).size();
    }

    public void closeCentersModal() throws InterruptedException {
        js().executeScript("arguments[0].click();",
                driver.findElement(By.cssSelector("#modal-centers-list button.close")));
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // POPDOWN POPUPS (View Offer / History Log / Admission Request)
    // ═══════════════════════════════════════════════
    public boolean isPopdownVisible() { return isShown(By.id("popdown-dialog")); }

    public String getPopdownText() {
        List<WebElement> d = driver.findElements(By.id("popdown-dialog"));
        return d.isEmpty() ? "" : d.get(0).getText();
    }

    public String getPopdownTitle() {
        List<WebElement> t = driver.findElements(By.cssSelector("#popdown-dialog .modal-title, #popdown-dialog h4, #popdown-dialog h5"));
        for (WebElement e : t)
            if (!text(e).isEmpty()) return text(e);
        return "";
    }

    // ✅ History Log table headers
    public List<String> getHistoryHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : driver.findElements(By.cssSelector("#popdown-dialog table thead th")))
            out.add(text(th));
        return out;
    }

    public int getHistoryRowCount() {
        return driver.findElements(By.cssSelector("#popdown-dialog table tbody tr")).size();
    }

    // ✅ Update Details icon of the first history row — "Log Details" text in data-content
    public String getFirstHistoryLogDetails() {
        List<WebElement> s = driver.findElements(By.cssSelector("#popdown-dialog table tbody tr span[data-content]"));
        return s.isEmpty() ? "" : s.get(0).getAttribute("data-content").replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    public List<String> getHistoryDates() {
        List<String> out = new ArrayList<>();
        for (WebElement tr : driver.findElements(By.cssSelector("#popdown-dialog table tbody tr")))
            out.add(text(tr.findElement(By.xpath("./td[1]"))));
        return out;
    }

    public boolean isPopdownFieldVisible(String id) {
        return isShown(By.cssSelector("#popdown-dialog #" + id));
    }

    public void closePopdown() throws InterruptedException {
        js().executeScript(
                "var b=document.querySelector('#popdown-dialog .close-popdown, #popdown-dialog [data-dismiss=modal], #popdown-dialog .close');"
                        + "if(b) b.click();");
        Thread.sleep(1000);
        js().executeScript("var d=document.getElementById('popdown-dialog'); if(d) d.remove();"
                + "var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';");
    }

    // ═══════════════════════════════════════════════
    // ADD / UPDATE FORM
    // ═══════════════════════════════════════════════
    public void clickAddNewOffer() throws InterruptedException {
        js().executeScript("arguments[0].click();", addNewOfferBtn);
        System.out.println("▶ Add New Corporate Offer clicked");
        Thread.sleep(2500);
    }

    public boolean isFormLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("offer_form")));
            return driver.getCurrentUrl().contains("add_corporate_offer");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isFieldVisible(String id) { return isShown(By.id(id)); }

    public boolean isFieldDisabled(String id) {
        return driver.findElement(By.id(id)).getAttribute("disabled") != null;
    }

    public List<String> getOptions(String id) { return optionTexts(driver.findElement(By.id(id))); }

    public String getSelectedText(String id) {
        return new Select(driver.findElement(By.id(id))).getFirstSelectedOption().getText().trim();
    }

    public String getFieldValue(String id) { return driver.findElement(By.id(id)).getAttribute("value"); }

    // ✅ Selects by visible text and fires the page's change handler
    public void selectByText(String id, String text) throws InterruptedException {
        WebElement s = driver.findElement(By.id(id));
        new Select(s).selectByVisibleText(text);
        js().executeScript("jQuery(arguments[0]).trigger('change');", s);
        Thread.sleep(800);
    }

    public void enterField(String id, String value) {
        WebElement e = driver.findElement(By.id(id));
        e.clear();
        e.sendKeys(value);
    }

    // ✅ Number of program checkboxes (chk_<program>_<n>) and centre checkboxes (chk_center_<id>)
    public int getProgramCheckboxCount() {
        return driver.findElements(By.cssSelector("input[type='checkbox'][id^='chk_']:not([id^='chk_center_']):not([id='chk_all_center'])")).size();
    }

    public int getCenterCheckboxCount() {
        return driver.findElements(By.cssSelector("input[type='checkbox'][id^='chk_center_']")).size();
    }

    // ✅ The page text contains a label (e.g. "Max Age Allowed")
    public boolean pageHasText(String label) {
        return driver.findElement(By.tagName("body")).getText().contains(label);
    }

    // ✅ Records every XHR so a test can prove Submit sent nothing
    public void startAjaxCapture() {
        js().executeScript("window.__reqs=[]; var o=XMLHttpRequest.prototype.open;"
                + "XMLHttpRequest.prototype.open=function(m,u){window.__reqs.push(m+' '+u); return o.apply(this,arguments);};");
    }

    @SuppressWarnings("unchecked")
    public List<String> getCapturedAjax() {
        Object r = js().executeScript("return window.__reqs || [];");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    // ✅ UAT's fixed "API Debug (Test)" panel (#api-debug-panel) covers the
    //    bottom of the page and intercepts clicks on SUBMIT
    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public void clickSubmit() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", submitBtn);
        Thread.sleep(500);
        try {
            submitBtn.click();
        } catch (org.openqa.selenium.ElementClickInterceptedException e) {
            // ✅ A panel can overlap SUBMIT while the page settles — a JS click
            //    still fires the form's submit + validation
            js().executeScript("arguments[0].click();", submitBtn);
        }
        System.out.println("▶ Submit clicked");
        Thread.sleep(2500);
    }

    // ✅ Visible validation messages as "<field id> => <message>" —
    //    live markup: <label id="<field>-error" class="text-danger" for="<field>">
    @SuppressWarnings("unchecked")
    public List<String> getVisibleErrors() {
        Object r = js().executeScript(
                "return Array.from(document.querySelectorAll('#offer_form label[id$=\"-error\"]'))"
                        + ".filter(function(e){return (e.offsetWidth||e.offsetHeight) && e.innerText.trim();})"
                        + ".map(function(e){return (e.getAttribute('for')||e.id||'') + ' => ' + e.innerText.trim();});");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    public void clickBack() throws InterruptedException {
        WebElement back = driver.findElement(By.xpath(
                "//*[(self::a or self::button) and normalize-space(.)='BACK'] | //input[@value='BACK']"));
        js().executeScript("arguments[0].click();", back);
        System.out.println("▶ BACK clicked");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // ADD / UPDATE — SAVING (confirmed live 2026-10-27; logic in
    // assets/js/corporateoffer.js — jQuery validate, then a plain form POST)
    // ═══════════════════════════════════════════════
    // ✅ Selects by option value and fires the page's change handler
    public void selectByValue(String id, String value) throws InterruptedException {
        WebElement s = driver.findElement(By.id(id));
        new Select(s).selectByValue(value);
        js().executeScript("jQuery(arguments[0]).trigger('change');", s);
        Thread.sleep(500);
    }

    // ✅ Program checkbox chk_<program>_<n>; feeText e.g. "Rs. 7000.00" picks the
    //    matching sel_<program>_<n> fee (only shown when a plan is chosen); null = no flat fee
    public void checkProgram(String checkboxId, String feeText) throws InterruptedException {
        WebElement chk = driver.findElement(By.id(checkboxId));
        if (!chk.isSelected()) js().executeScript("jQuery(arguments[0]).click();", chk);
        Thread.sleep(400);
        if (feeText != null)
            selectByText(checkboxId.replaceFirst("^chk_", "sel_"), feeText);
    }

    // ✅ Centre checkbox chk_center_<centerId> (e.g. 100 = Demo Center)
    public void checkCenter(String centerId) {
        WebElement chk = driver.findElement(By.id("chk_center_" + centerId));
        if (!chk.isSelected()) js().executeScript("arguments[0].click();", chk);
    }

    // ✅ pickadate — set through the widget's own API (keeps start_date_submit in sync)
    public void setStartDateToday() {
        js().executeScript("jQuery('#start_date').pickadate('picker').set('select', new Date());");
    }

    public String getStartDateSubmitValue() {
        return driver.findElement(By.id("start_date_hidden")).getAttribute("value");
    }

    // ✅ #document_url (bootstrap-fileinput). A valid pdf/doc/docx/pptx ≤ 8 MB is
    //    uploaded to AWS on change; the stored path lands in #doc_type_file
    public void chooseContractFile(String absolutePath) throws InterruptedException {
        hideApiDebugPanel();
        driver.findElement(By.id("document_url")).sendKeys(absolutePath);
        System.out.println("▶ Contract file chosen: " + absolutePath);
        Thread.sleep(1500);
    }

    // ✅ Waits for the AWS upload to fill #doc_type_file ("" when it never does)
    public String waitForContractUpload(int seconds) throws InterruptedException {
        for (int i = 0; i < seconds * 2; i++) {
            String v = getFieldValue("doc_type_file");
            if (v != null && !v.isEmpty()) return v;
            Thread.sleep(500);
        }
        return "";
    }

    // ✅ Client-side file check message (#contract_file_error)
    public String getContractFileError() {
        WebElement e = driver.findElement(By.id("contract_file_error"));
        return isShown(e) ? text(e) : "";
    }

    public String getContractFileCaption() {
        List<WebElement> c = driver.findElements(By.cssSelector(".file-caption-name"));
        return c.isEmpty() ? "" : text(c.get(0));
    }

    // ✅ fileinput "Remove" button (Clear selected files)
    public void clickRemoveContractFile() throws InterruptedException {
        js().executeScript("arguments[0].click();",
                driver.findElement(By.cssSelector("button.fileinput-remove-button")));
        System.out.println("▶ Remove (contract file) clicked");
        Thread.sleep(1000);
    }

    // ✅ Update form: "Reason for Change*" + UPDATE button (.update_offer)
    public void enterUpdateReason(String reason) {
        enterField("update_reason", reason);
    }

    public void clickUpdate() throws InterruptedException {
        hideApiDebugPanel();
        WebElement btn = driver.findElement(By.cssSelector("#update_offer, .update_offer"));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        Thread.sleep(500);
        try {
            btn.click();
        } catch (org.openqa.selenium.ElementClickInterceptedException e) {
            js().executeScript("arguments[0].click();", btn);
        }
        System.out.println("▶ UPDATE clicked");
        Thread.sleep(3500);
    }

    // ✅ Visible success / warning flash texts on the page after a save
    @SuppressWarnings("unchecked")
    public List<String> getFlashMessages() {
        Object r = js().executeScript(
                "return Array.from(document.querySelectorAll('.alert, .flash-msg, .toast, .jGrowl-message, .pnotify-text'))"
                        + ".filter(function(e){return (e.offsetWidth||e.offsetHeight) && e.innerText.trim();})"
                        + ".map(function(e){return e.innerText.replace(/\\s+/g,' ').trim();});");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    // ✅ List: search a title and return the first matching row's Update href ("" if none)
    public String findUpdateHrefByTitle(String title) throws InterruptedException {
        searchTable(title);
        for (WebElement row : rows())
            if (text(row.findElement(By.xpath("./td[1]"))).startsWith(title)) {
                List<WebElement> a = row.findElements(By.cssSelector("a[href^='add_corporate_offer?offer_id=']"));
                return a.isEmpty() ? "" : a.get(0).getAttribute("href");
            }
        return "";
    }

    // ✅ Full text of the first row whose title starts with the given title (after searching)
    public String getRowTextByTitle(String title) throws InterruptedException {
        searchTable(title);
        for (WebElement row : rows())
            if (text(row.findElement(By.xpath("./td[1]"))).startsWith(title)) {
                List<String> cells = new ArrayList<>();
                for (WebElement td : row.findElements(By.xpath("./td"))) cells.add(text(td));
                return String.join(" | ", cells);
            }
        return "";
    }

    // ✅ Picks the option at index (0 = placeholder) when the select is visible
    public void selectByIndexIfPresent(String id, int index) throws InterruptedException {
        if (!isFieldVisible(id)) return;
        WebElement s = driver.findElement(By.id(id));
        new Select(s).selectByIndex(index);
        js().executeScript("jQuery(arguments[0]).trigger('change');", s);
        Thread.sleep(400);
    }

    // ✅ The "Log Data" popover AS RENDERED for the first history row (hover; the
    //    page's own popover('show') as fallback) — data-content holds more than is displayed
    public String getFirstHistoryPopoverRenderedText() throws InterruptedException {
        List<WebElement> s = driver.findElements(By.cssSelector("#popdown-dialog table tbody tr span[data-content]"));
        if (s.isEmpty()) return "";
        hideApiDebugPanel();
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", s.get(0));
        new Actions(driver).moveToElement(s.get(0)).perform();
        Thread.sleep(1200);
        if (!isShown(By.cssSelector(".popover .popover-content"))) {
            js().executeScript("jQuery(arguments[0]).popover('show');", s.get(0));
            Thread.sleep(1000);
        }
        List<WebElement> c = driver.findElements(By.cssSelector(".popover .popover-content"));
        String out = c.isEmpty() ? "" : c.get(0).getText().replaceAll("\\s+", " ").trim();
        js().executeScript("jQuery(arguments[0]).popover('hide');", s.get(0));
        return out;
    }

    // ═══════════════════════════════════════════════
    // ADMISSION REQUEST POPUP (#frm_admission_request in #popdown-dialog)
    // Fetch → test-crm …/api/lead_records/getLeadDetails; Send Request →
    // api/Corporateinvitation/setAdmissionInvitationRequest, then reload
    // ═══════════════════════════════════════════════
    private WebElement inPopup(String id) {
        return driver.findElement(By.cssSelector("#popdown-dialog #" + id));
    }

    public void fetchLead(String leadId) throws InterruptedException {
        WebElement lead = inPopup("lead_id");
        lead.clear();
        lead.sendKeys(leadId);
        js().executeScript("document.querySelector('#popdown-dialog [name=btn_lead_details]').click();");
        System.out.println("▶ Fetch Details: lead " + leadId);
        Thread.sleep(3000);
    }

    public boolean isLeadDetailsShown() { return isShown(By.cssSelector("#popdown-dialog .lead_id_data")); }

    public String getLeadName()  { return text(inPopup("lbl_lead_name")); }
    public String getLeadEmail() { return text(inPopup("lbl_lead_email")); }
    public String getLeadPhone() { return text(inPopup("lbl_lead_phoneno")); }

    public List<String> getPopupOptions(String id) { return optionTexts(inPopup(id)); }

    public void selectInPopup(String id, String visibleText) {
        WebElement s = inPopup(id);
        new Select(s).selectByVisibleText(visibleText);
        js().executeScript("jQuery(arguments[0]).trigger('change');", s);
    }

    // ✅ First real option (index 1) of a popup select → returns its text
    public String selectFirstInPopup(String id) {
        WebElement s = inPopup(id);
        new Select(s).selectByIndex(1);
        js().executeScript("jQuery(arguments[0]).trigger('change');", s);
        return new Select(s).getFirstSelectedOption().getText().trim();
    }

    public void enterInPopup(String id, String value) {
        WebElement e = inPopup(id);
        e.clear();
        e.sendKeys(value);
    }

    // ✅ Tentative Admission Date (pickadate) — set through the widget's API
    public void setTentativeAdmissionDate(int daysFromToday) {
        js().executeScript("var d=new Date(); d.setDate(d.getDate()+arguments[0]);"
                + "jQuery('#popdown-dialog #admission_request_date').pickadate('picker').set('select', d);", daysFromToday);
    }

    // ✅ Send Request → returns the popup's flash text (read before the 1.5 s reload)
    public String clickSendRequest() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#popdown-dialog #send_request').click();");
        System.out.println("▶ Send Request clicked");
        for (int i = 0; i < 12; i++) {
            Thread.sleep(250);
            try {
                Object t = js().executeScript("var a=document.querySelector('#popdown-dialog .flash-msg .alert');"
                        + "return a && (a.offsetWidth||a.offsetHeight) ? a.innerText : '';");
                if (t != null && !String.valueOf(t).trim().isEmpty()) return String.valueOf(t).replaceAll("\\s+", " ").trim();
            } catch (Exception reloaded) {
                return "";
            }
        }
        return "";
    }

    // ✅ Visible validation messages inside the popup ("<field> => <message>")
    @SuppressWarnings("unchecked")
    public List<String> getPopupErrors() {
        Object r = js().executeScript(
                "return Array.from(document.querySelectorAll('#popdown-dialog label[id$=\"-error\"]'))"
                        + ".filter(function(e){return (e.offsetWidth||e.offsetHeight) && e.innerText.trim();})"
                        + ".map(function(e){return (e.getAttribute('for')||'') + ' => ' + e.innerText.trim();});");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }
}
