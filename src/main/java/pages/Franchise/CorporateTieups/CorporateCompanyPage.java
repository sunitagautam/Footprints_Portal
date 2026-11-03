package pages.Franchise.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Corporate Company (/corporate_company).
 * Filter form #corporate_company (posts back; Search button, no Clear); DataTable "table.dataTable" (client side,
 * 10 per page, Company ID hidden). Add / Update popup (#popdown-dialog, add_corporate_company?pop=yes[&company_id=])
 * with City / State dropdowns that offer OTHER → free text.
 */
public class CorporateCompanyPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#corporate_company";
    private static final String TABLE = "table.dataTable";
    private static final String POPUP = "#popdown-dialog";

    public CorporateCompanyPage(WebDriver driver) {
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

    // ✅ UAT's fixed "API Debug (Test)" panel covers the bottom of the page
    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(d -> !d.findElements(By.cssSelector(FILTER + " #btn_submit")).isEmpty()
                    && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getHeading() {
        return String.valueOf(js().executeScript("var h=[].filter.call(document.querySelectorAll('h5'),function(h){return h.offsetWidth>0;})"
                + ".map(function(h){return h.textContent.trim();}).filter(function(t){return /Company/.test(t);})[0];return h||'';"));
    }

    public boolean isAddButtonVisible() { return isShown(By.cssSelector("a[href='add_corporate_company?pop=yes']")); }

    // ════════════════ Filters ════════════════
    public boolean isFilterFieldVisible(String id) { return isShown(By.cssSelector(FILTER + " #" + id)); }

    public List<String> getSelectOptions(String id) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(driver.findElement(By.cssSelector(FILTER + " #" + id))).getOptions())
            out.add(o.getAttribute("textContent").trim());
        return out;
    }

    public String getSelectedText(String id) {
        return new Select(driver.findElement(By.cssSelector(FILTER + " #" + id))).getFirstSelectedOption().getAttribute("textContent").trim();
    }

    public void selectFilter(String id, String text) {
        new Select(driver.findElement(By.cssSelector(FILTER + " #" + id))).selectByVisibleText(text);
    }

    public void enterCompanyName(String name) {
        WebElement e = driver.findElement(By.cssSelector(FILTER + " #company_name"));
        e.clear();
        e.sendKeys(name);
    }

    public String getCompanyNameFilter() { return driver.findElement(By.cssSelector(FILTER + " #company_name")).getAttribute("value"); }

    public void clickSearch() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        System.out.println("▶ Search clicked");
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim();});"));
    }

    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.offsetWidth>0;})"
                + ".map(function(h){return h.textContent.trim();});"));
    }

    private int colIndex(String header) {
        int i = getAllColumnHeaders().indexOf(header);
        if (i < 0) throw new IllegalArgumentException("No column " + header);
        return i;
    }

    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable();"
                + "return dt.column(arguments[0],{search:'applied'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable();"
                + "return dt.column(arguments[0],{page:'current'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public boolean isSearchBoxVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }

    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    public String getDownloadButtonText() {
        return String.valueOf(js().executeScript("var b=document.querySelector('.dt-buttons .dt-button');return b?b.textContent.trim():'';"));
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(".dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1200);
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('.dt-buttons .dt-button').click();");
        System.out.println("▶ Download clicked");
        Thread.sleep(1000);
    }

    public boolean isNextPageEnabled() {
        return !driver.findElements(By.cssSelector(".dataTables_paginate .paginate_button.next:not(.disabled)")).isEmpty();
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .paginate_button.previous').click();");
        Thread.sleep(800);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("var A=arguments[0]; [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0].click();", header);
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return String.valueOf(js().executeScript("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0];"
                + "return h?h.className:'';", header));
    }

    // ✅ Company ID (hidden column) of a company name; "" when not listed
    public String findCompanyId(String name) {
        Object id = js().executeScript("var N=arguments[0], dt=jQuery('" + TABLE + "').DataTable(), out='';"
                + "dt.rows().every(function(){var d=this.data(); if(jQuery('<div>').html(d[0]).text().trim()===N) out=jQuery('<div>').html(d[1]).text().trim();}); return out;", name);
        return String.valueOf(id);
    }

    // ════════════════ Add / Update popup ════════════════
    public void clickAddCompany() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector(\"a[href='add_corporate_company?pop=yes']\").click();");
        System.out.println("▶ Add Corporate Company clicked");
        waitForPopup();
    }

    // ✅ Opens the Update popup of a company by its id (row's edit icon, href company_id=<base64 id>)
    public void clickEditCompany(String companyId) throws InterruptedException {
        hideApiDebugPanel();
        String b64 = java.util.Base64.getEncoder().encodeToString(companyId.getBytes());
        Object ok = js().executeScript("var B=arguments[0], dt=jQuery('" + TABLE + "').DataTable(), a=null;"
                + "dt.rows().nodes().toArray().forEach(function(r){var x=r.querySelector('a[href*=\"company_id='+B+'\"]'); if(x) a=x;});"
                + "if(!a) return false; dt.search('').draw(false);"
                + "var i=dt.rows({order:'applied'}).nodes().toArray().indexOf(a.closest('tr')); dt.page(Math.floor(i/dt.page.len())).draw(false);"
                + "a.scrollIntoView({block:'center'}); a.click(); return true;", b64);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No edit icon for company " + companyId);
        System.out.println("▶ Update clicked (company " + companyId + ")");
        waitForPopup();
    }

    private void waitForPopup() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Object ready = js().executeScript("var b=document.querySelector('" + POPUP + " #btn_save_data');"
                    + "if(!b || !window.jQuery) return false; var ev=jQuery._data(b,'events'); return !!(ev && ev.click) || !!b.onclick || !!b.getAttribute('onclick');");
            if (Boolean.TRUE.equals(ready)) break;
            Thread.sleep(500);
        }
        Thread.sleep(500);
    }

    public boolean isPopupVisible() { return isShown(By.cssSelector(POPUP + " #new_company_name")); }

    public String getPopupText() {
        return String.valueOf(js().executeScript("var p=document.querySelector('" + POPUP + "');return p?p.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getPopupTitle() {
        return String.valueOf(js().executeScript("var p=document.querySelector('" + POPUP + "');if(!p)return '';"
                + "var h=p.querySelector('.modal-title, h4, h5, h3');return h?h.textContent.trim():'';"));
    }

    public List<String> getPopupOptions(String id) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(driver.findElement(By.cssSelector(POPUP + " #" + id))).getOptions())
            out.add(o.getAttribute("textContent").trim());
        return out;
    }

    public String getPopupValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + POPUP + " #'+arguments[0]);return e?e.value:'';", id));
    }

    public String getPopupSelectedText(String id) {
        return new Select(driver.findElement(By.cssSelector(POPUP + " #" + id))).getFirstSelectedOption().getAttribute("textContent").trim();
    }

    public void enterPopupField(String id, String value) {
        WebElement e = driver.findElement(By.cssSelector(POPUP + " #" + id));
        e.clear();
        if (!value.isEmpty()) e.sendKeys(value);
    }

    public void selectPopup(String id, String text) {
        Select s = new Select(driver.findElement(By.cssSelector(POPUP + " #" + id)));
        s.selectByVisibleText(text);
        js().executeScript("jQuery('" + POPUP + " #'+arguments[0]).trigger('change');", id);
    }

    public boolean isPopupFieldVisible(String id) { return isShown(By.cssSelector(POPUP + " #" + id)); }

    // ✅ Records XHRs and native confirms (kept in sessionStorage across a reload); acceptConfirm=false answers Cancel
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__reqs=[]; try{ sessionStorage.setItem('__confirms','[]'); }catch(e){}"
                + "var o=XMLHttpRequest.prototype.open; XMLHttpRequest.prototype.open=function(m,u){window.__reqs.push(m+' '+u); return o.apply(this,arguments);};"
                + "window.confirm=function(m){ try{ var c=JSON.parse(sessionStorage.getItem('__confirms')||'[]'); c.push(m);"
                + " sessionStorage.setItem('__confirms', JSON.stringify(c)); }catch(e){} return " + acceptConfirm + "; };");
    }

    public List<String> getCapturedRequests() { return strings(js().executeScript("return window.__reqs || [];")); }

    public List<String> getCapturedConfirms() {
        return strings(js().executeScript("try{ return JSON.parse(sessionStorage.getItem('__confirms')||'[]'); }catch(e){ return []; }"));
    }

    public void clickPopupSubmit() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + POPUP + " #btn_save_data').click();");
        System.out.println("▶ Popup Submit / Update clicked");
        Thread.sleep(1500);
    }

    // ✅ Visible validation messages of the popup ("<field id> => <message>")
    public List<String> getPopupErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + POPUP + " label.text-danger[for], " + POPUP + " label.error'), function(l){"
                + "return l.offsetWidth>0 && l.textContent.trim() && l.textContent.trim()!=='*';}).map(function(l){return l.getAttribute('for')+' => '+l.textContent.trim();});"));
    }

    // ✅ Message after a save (popup or page flash — the page may reload), "" when none within ~20 s
    public String waitForMessage() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Object m = js().executeScript("var o=[];document.querySelectorAll('.flash-msg, .success-msg, " + POPUP + " .alert').forEach(function(e){"
                    + "if(e.offsetWidth>0 && e.textContent.trim()) o.push(e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim());}); return o.join(' | ');");
            if (m != null && !String.valueOf(m).isEmpty()) return String.valueOf(m);
            Thread.sleep(500);
        }
        return "";
    }
}
