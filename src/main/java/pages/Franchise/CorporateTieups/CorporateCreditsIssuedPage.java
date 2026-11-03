package pages.Franchise.CorporateTieups;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Corporate Credits Issued (/issue_corporate_credits). Screen access View_Corporate_Invoice;
 * Override_Corporate_Payer shows "Issue Credits" and the Action column (Revoke Credit / Link Invoice Reference).
 * Filter form #dateform (posts back): From / To (pickadate, name date_from / date_to — both id select_date), Center
 * #center_selection (select2), Status #status, Filter input[name=submit_credit_date], Clear link. More than 31 days →
 * "Cannot select more than 31 days data" and the default range comes back.
 * Table #DataTables_Table_0, 10 per page, Download Report = client-side CSV Credits_Issued_Report_dd_MM_yyyy.csv.
 * Popups render in #popdown-dialog: Add (pop_issue_corporate_credits), Link (corporate_invoice_reference?credit_id=),
 * Revoke (revoke_corporate_credit?credit_id=). Every save shows a flash message and reloads the page after 2 s.
 */
public class CorporateCreditsIssuedPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#dateform";
    private static final String TABLE = "#DataTables_Table_0";
    private static final String POPUP = "#popdown-dialog";

    public CorporateCreditsIssuedPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private boolean isShown(By by) {
        for (WebElement e : driver.findElements(by))
            try {
                if (e.isDisplayed()) return true;
            } catch (Exception ignored) {
            }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(String.valueOf(x));
        return out;
    }

    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(d -> !d.findElements(By.name("submit_credit_date")).isEmpty()
                    && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getHeading() {
        return String.valueOf(js().executeScript("var h=document.querySelector('h2');return h?h.textContent.trim():'';"));
    }

    public String getBodyText() {
        return String.valueOf(js().executeScript("return document.body.innerText;"));
    }

    // ════════════════ Filters ════════════════
    public boolean isFromDateVisible() { return isShown(By.cssSelector(FILTER + " input[name=date_from]")); }
    public boolean isToDateVisible() { return isShown(By.cssSelector(FILTER + " input[name=date_to]")); }
    public boolean isCenterFilterVisible() { return isShown(By.cssSelector(FILTER + " .select2-container, " + FILTER + " #center_selection")); }
    public boolean isStatusFilterVisible() { return isShown(By.cssSelector(FILTER + " #status")); }
    public boolean isFilterButtonVisible() { return isShown(By.name("submit_credit_date")); }
    public boolean isClearLinkVisible() { return isShown(By.cssSelector("a[href='issue_corporate_credits']")); }
    public boolean isIssueCreditsVisible() { return isShown(By.cssSelector("a[title='Issue Credits']")); }
    public boolean isSearchVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }
    public boolean isDownloadVisible() { return isShown(By.cssSelector(".buttons-csv")); }
    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    public List<String> getStatusOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " #status option'), function(o){return o.text.trim();});"));
    }

    public String getSelectedStatus() {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + FILTER + " #status');return s.options[s.selectedIndex].text.trim();"));
    }

    public String getSelectedCenter() {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + FILTER + " #center_selection');return s.options[s.selectedIndex].text.trim();"));
    }

    public List<String> getCenterOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " #center_selection option'), function(o){return o.text.trim();});"));
    }

    // "<date_from> | <date_to>" as shown (d MMMM yyyy)
    public String getDateValues() {
        return String.valueOf(js().executeScript("return document.querySelector('" + FILTER + " input[name=date_from]').value+' | '+document.querySelector('" + FILTER + " input[name=date_to]').value;"));
    }

    // ✅ The filter the server applied (yyyy-MM-dd), written into the page as window.pageDownloadFilters
    public String getAppliedFilters() {
        return String.valueOf(js().executeScript("return JSON.stringify(window.pageDownloadFilters||{});"));
    }

    public void setDateRange(LocalDate from, LocalDate to) {
        js().executeScript("var f=jQuery('" + FILTER + " input[name=date_from]').pickadate('picker'), t=jQuery('" + FILTER + " input[name=date_to]').pickadate('picker');"
                        + "f.set('select',[arguments[0],arguments[1],arguments[2]]); t.set('select',[arguments[3],arguments[4],arguments[5]]);",
                from.getYear(), from.getMonthValue() - 1, from.getDayOfMonth(), to.getYear(), to.getMonthValue() - 1, to.getDayOfMonth());
    }

    public void selectStatus(String status) {
        js().executeScript("var s=jQuery('" + FILTER + " #status'), t=arguments[0];"
                + "s.val(s.find('option').filter(function(){return jQuery(this).text().trim()===t;}).val()).trigger('change');", status);
    }

    public void selectCenter(String center) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #center_selection'), t=arguments[0];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.val(o.first().val()).trigger('change'); return true;", center);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Center '" + center + "' not in the list");
    }

    public void clickFilter() throws InterruptedException {
        js().executeScript("document.body.setAttribute('data-qa-old','1');");
        driver.findElement(By.name("submit_credit_date")).click();
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState==='complete' && !document.body.hasAttribute('data-qa-old');")));
        isPageLoaded();
        Thread.sleep(500);
    }

    public void clickClear() throws InterruptedException {
        js().executeScript("document.body.setAttribute('data-qa-old','1');");
        js().executeScript("document.querySelector(\"a[href='issue_corporate_credits']\").click();");
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState==='complete' && !document.body.hasAttribute('data-qa-old');")));
        isPageLoaded();
    }

    // ✅ Red banner shown after a rejected filter (e.g. "Cannot select more than 31 days data")
    public String getPageAlert() {
        return String.valueOf(js().executeScript("var e=[].filter.call(document.querySelectorAll('.alert-danger'), function(x){return x.offsetParent;});"
                + "return e.length ? e[0].innerText.replace(/×|Close/g,'').replace(/\\s+/g,' ').trim() : '';"));
    }

    // ════════════════ Table ════════════════
    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(t){return t;})"
                + ".filter(function(t){return t.offsetParent;}).map(function(t){return t.textContent.trim();});"));
    }

    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().columns().header().toArray().map(function(h){return h.textContent.trim();});"));
    }

    private int headerIndex(String header) {
        List<String> all = getAllColumnHeaders();
        int i = all.indexOf(header);
        if (i < 0) throw new IllegalArgumentException("Column '" + header + "' not in " + all);
        return i;
    }

    // ✅ Every row's value (all pages, current search), as text
    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=v; return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{page:'current'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=v; return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_info');return i?i.innerText.trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public String getSearchPlaceholder() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_filter input');return i?i.placeholder:'';"));
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(".dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1000);
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('.buttons-csv').click();");
        Thread.sleep(1500);
    }

    public boolean isNextPageEnabled() {
        return Boolean.TRUE.equals(js().executeScript("var n=document.querySelector('.dataTables_paginate .next');return !!n && !/disabled/.test(n.className);"));
    }

    public boolean isPreviousPageEnabled() {
        return Boolean.TRUE.equals(js().executeScript("var n=document.querySelector('.dataTables_paginate .previous');return !!n && !/disabled/.test(n.className);"));
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("jQuery(jQuery('" + TABLE + "').DataTable().column(arguments[0]).header()).click();", headerIndex(header));
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return String.valueOf(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0]).header().className;", headerIndex(header)));
    }

    // ✅ Credit ids on the grid (from the action links), in table order
    public List<String> getCreditIds() {
        return strings(js().executeScript("var s={}; return [].map.call(document.querySelectorAll(\"" + TABLE + " a[href*='credit_id=']\"),"
                + "function(a){return a.getAttribute('href').split('credit_id=')[1];}).filter(function(i){return s[i]?false:(s[i]=1);});"));
    }

    public List<String> getRowActionTitles(String creditId) {
        return strings(js().executeScript("var a=document.querySelector(\"" + TABLE + " a[href$='credit_id=\"+arguments[0]+\"']\"); if(!a) return [];"
                + "return [].map.call(a.closest('tr').querySelectorAll('a[title]'), function(x){return x.title;});", creditId));
    }

    // "Child Name | Amount | Type | Description | Issued By | Invoice Reference | Issued Date Time | Update Status" for one credit
    public String getRowText(String creditId) {
        return String.valueOf(js().executeScript("var a=document.querySelector(\"" + TABLE + " a[href$='credit_id=\"+arguments[0]+\"']\"); if(!a) return '';"
                + "return [].map.call(a.closest('tr').cells, function(c){return c.innerText.replace(/\\s+/g,' ').trim();}).filter(function(t){return t;}).join(' | ');", creditId));
    }

    // ════════════════ Popups ════════════════
    private void openPopup(String selector) throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector(arguments[0]).click();", selector);
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> !d.findElements(By.cssSelector(POPUP + " form")).isEmpty());
        Thread.sleep(1000);
    }

    public boolean isPopupOpen() { return isShown(By.cssSelector(POPUP + " form")); }

    public String getPopupText() {
        return String.valueOf(js().executeScript("var d=document.querySelector('" + POPUP + "');return d?d.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getPopupTitle() {
        return String.valueOf(js().executeScript("var h=document.querySelector('" + POPUP + " .modal-title, " + POPUP + " h4, " + POPUP + " h5');return h?h.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public void openIssueCredits() throws InterruptedException {
        openPopup("a[title='Issue Credits']");
    }

    public void openLinkInvoice(String creditId) throws InterruptedException {
        openPopup(TABLE + " a[href$='corporate_invoice_reference?pop=yes&credit_id=" + creditId + "']");
    }

    public void openRevokeCredit(String creditId) throws InterruptedException {
        openPopup(TABLE + " a[href$='revoke_corporate_credit?pop=yes&credit_id=" + creditId + "']");
    }

    public void enterChildIdAndFetch(String childId) throws InterruptedException {
        WebElement id = driver.findElement(By.cssSelector(POPUP + " #child_id"));
        id.clear();
        id.sendKeys(childId);
        driver.findElement(By.cssSelector(POPUP + " #btn_child_details")).click();
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                "var n=document.querySelector('" + POPUP + " #child_name'); return !!n && n.value!=='';")));
        Thread.sleep(1500);
    }

    public String getFetchedChildName() {
        return String.valueOf(js().executeScript("var n=document.querySelector('" + POPUP + " #child_name');return n?n.value:'';"));
    }

    public List<String> getPopupSelectOptions(String id) {
        return strings(js().executeScript("var s=document.querySelector('" + POPUP + " #'+arguments[0]); if(!s) return [];"
                + "return [].map.call(s.options, function(o){return o.text.trim();});", id));
    }

    public String getPopupSelectedText(String id) {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + POPUP + " #'+arguments[0]);"
                + "return s && s.selectedIndex>=0 ? s.options[s.selectedIndex].text.trim() : '';", id));
    }

    // ✅ Option whose text contains the given text → selected (fires change); returns the option text
    public String selectPopupOptionContaining(String id, String text) {
        Object picked = js().executeScript("var s=document.querySelector('" + POPUP + " #'+arguments[0]), want=arguments[1];"
                + "for (var i=0;i<s.options.length;i++){ if(s.options[i].value!=='' && s.options[i].text.indexOf(want)>=0){"
                + " s.selectedIndex=i; jQuery(s).trigger('change'); return s.options[i].text.trim(); } } return '';", id, text);
        String result = String.valueOf(picked);
        if (result.isEmpty()) throw new IllegalArgumentException("No option containing '" + text + "' in #" + id);
        return result;
    }

    public void clearPopupSelect(String id) {
        js().executeScript("var s=document.querySelector('" + POPUP + " #'+arguments[0]); s.value=''; jQuery(s).trigger('change');", id);
    }

    public void typeInPopup(String id, String value) {
        WebElement e = driver.findElement(By.cssSelector(POPUP + " #" + id));
        e.clear();
        e.sendKeys(value);
    }

    public String getPopupFieldValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + POPUP + " #'+arguments[0]);return e?e.value:'';", id));
    }

    public void clickPopupButton(String id) {
        driver.findElement(By.cssSelector(POPUP + " #" + id)).click();
    }

    // ✅ Validation messages are shown one at a time (p/span .error-<field>)
    public String getVisibleValidationMessage() {
        return String.valueOf(js().executeScript("var e=[].filter.call(document.querySelectorAll(\"" + POPUP + " [class^='error-']\"),"
                + "function(x){return x.offsetParent && x.textContent.trim();}); return e.length ? e[0].textContent.trim() : '';"));
    }

    // ✅ Native confirm ("Are you sure you want to Issue Credit?") — wait for it; returns its text
    public String acceptConfirm() {
        Alert a = new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.alertIsPresent());
        String text = a.getText();
        a.accept();
        return text;
    }

    public String dismissConfirm() {
        Alert a = new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.alertIsPresent());
        String text = a.getText();
        a.dismiss();
        return text;
    }

    // ✅ flash_message after a save (page reloads ~2 s later) — polled quickly
    public String waitForFlashMessage() throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            Object m = js().executeScript("var e=[].filter.call(document.querySelectorAll('.alert-success, .alert-warning'),"
                    + "function(x){return x.offsetParent && x.innerText.trim();}); return e.length ? e[0].innerText.replace(/×|Close/g,'').replace(/\\s+/g,' ').trim() : '';");
            if (m != null && !String.valueOf(m).isEmpty()) return String.valueOf(m);
            Thread.sleep(200);
        }
        return "";
    }
}
