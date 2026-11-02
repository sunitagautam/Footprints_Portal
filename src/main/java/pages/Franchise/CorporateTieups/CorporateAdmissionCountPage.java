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
 * Franchise → Corporate Tieups → Corporate Admission Count (/tieup_admission_count), right View_Corporate_Admission_Count.
 * Filter form #frm_tieup_admission_count (posts back): Offer Title, Company, Offer Status, Center (select2).
 * Summary table #tieup_admission_count_table: Offer Title | Company Name | Offer Status | Admission Count — the offer,
 * company and count are links (pop_tieup_admission_list?pop=yes&offer_id=&company_id=&center_id=…) that open a
 * popup (#popdown-dialog) listing the admissions: Admission ID | Child Name | Center | Admission Date | Status.
 */
public class CorporateAdmissionCountPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_tieup_admission_count";
    private static final String TABLE = "#tieup_admission_count_table";
    private static final String POPUP = "#popdown-dialog";

    public CorporateAdmissionCountPage(WebDriver driver) {
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

    // ✅ The summary loads slowly (~5 s) — wait for the DataTable
    public boolean isPageLoaded() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
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

    public boolean pageHasText(String s) {
        return String.valueOf(js().executeScript("return document.body.innerText;")).contains(s);
    }

    // ════════════════ Filters ════════════════
    public boolean isFilterFieldVisible(String id) { return isShown(By.cssSelector(FILTER + " #" + id + ", " + FILTER + " #" + id + " + .select2")); }

    public List<String> getSelectOptions(String id) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(driver.findElement(By.cssSelector(FILTER + " #" + id))).getOptions())
            out.add(o.getAttribute("textContent").trim());
        return out;
    }

    public String getSelectedText(String id) {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + FILTER + " #" + id + "');"
                + "return s && s.selectedIndex>=0 ? s.options[s.selectedIndex].text.trim() : '';"));
    }

    public void selectByText(String id, String text) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #" + id + "'), t=arguments[0];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                + "if(!o.length) return false; s.val(o.first().val()).trigger('change'); return true;", text);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Option '" + text + "' not in #" + id);
    }

    public boolean isSelect2(String id) {
        return Boolean.TRUE.equals(js().executeScript("return jQuery('" + FILTER + " #" + id + "').hasClass('select2-hidden-accessible');"));
    }

    // ✅ Opens the select2 Center dropdown, types a keyword → visible result texts (searchable filter)
    public List<String> searchSelect2(String id, String keyword) throws InterruptedException {
        js().executeScript("jQuery('" + FILTER + " #" + id + "').select2('open');");
        Thread.sleep(600);
        WebElement box = driver.findElement(By.cssSelector(".select2-container--open .select2-search__field, .select2-drop-active input.select2-input"));
        box.sendKeys(keyword);
        Thread.sleep(1000);
        List<String> res = strings(js().executeScript("return [].map.call(document.querySelectorAll('.select2-results__option, .select2-result-label'),function(o){return o.textContent.trim();})"
                + ".filter(function(t){return t;});"));
        js().executeScript("jQuery('" + FILTER + " #" + id + "').select2('close');");
        return res;
    }

    public void clickFilter() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        System.out.println("▶ Filter clicked");
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Summary table ════════════════
    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.offsetWidth>0;})"
                + ".map(function(h){return h.textContent.trim();});"));
    }

    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim();});"));
    }

    // ✅ Every summary row (filtered) as "offer || company || status || count"
    public List<String> getSummaryRows() {
        return strings(js().executeScript("var t=function(v){return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();};"
                + "return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).data().toArray().map(function(d){return t(d[0])+' || '+t(d[1])+' || '+t(d[2])+' || '+t(d[3]);});"));
    }

    public List<String> getPageColumn(int index) {
        return strings(js().executeScript("var t=function(v){return jQuery('<div>').html(v).text().replace(/\\s+/g,' ').trim();};"
                + "return jQuery('" + TABLE + "').DataTable().column(arguments[0],{page:'current'}).data().toArray().map(t);", index));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('" + TABLE + "_info, .dataTables_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public String getDownloadButtonText() {
        return String.valueOf(js().executeScript("var b=document.querySelector('.dt-buttons [aria-controls=\"tieup_admission_count_table\"]');return b?b.textContent.trim():'';"));
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector("#tieup_admission_count_table_filter input, .dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1200);
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('.dt-buttons [aria-controls=\"tieup_admission_count_table\"]').click();");
        System.out.println("▶ Download clicked");
        Thread.sleep(1000);
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('#tieup_admission_count_table_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('#tieup_admission_count_table_paginate .paginate_button.previous').click();");
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

    // ✅ Link hrefs of a summary row (by offer + company) for its 3 linked cells: offer, company, count
    public List<String> getRowLinks(String offer, String company) {
        return strings(js().executeScript("var O=arguments[0], C=arguments[1], out=[];"
                + "jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().forEach(function(r){"
                + " if(r.children[0].textContent.trim()===O && r.children[1].textContent.trim()===C){"
                + "  [0,1,3].forEach(function(i){var a=r.children[i].querySelector('a'); out.push(a?a.getAttribute('href'):'');});}});"
                + "return out;", offer, company));
    }

    // ✅ Clicks cell 0 (offer), 1 (company) or 3 (count) of a summary row; pages the table to it first
    public void clickRowLink(String offer, String company, int cell) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var O=arguments[0], C=arguments[1], I=arguments[2], dt=jQuery('" + TABLE + "').DataTable(), row=null;"
                + "dt.rows({search:'applied'}).nodes().toArray().forEach(function(r){if(r.children[0].textContent.trim()===O && r.children[1].textContent.trim()===C) row=r;});"
                + "if(!row) return false; var i=dt.rows({search:'applied',order:'applied'}).nodes().toArray().indexOf(row); dt.page(Math.floor(i/dt.page.len())).draw(false);"
                + "var a=row.children[I].querySelector('a'); if(!a) return false; a.scrollIntoView({block:'center'}); a.click(); return true;", offer, company, cell);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No link in cell " + cell + " for " + offer + " / " + company);
        System.out.println("▶ Summary link clicked (cell " + cell + ") " + offer + " / " + company);
        waitForPopup();
    }

    // ════════════════ Admissions popup ════════════════
    private void waitForPopup() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Object ok = js().executeScript("var p=document.querySelector('" + POPUP + "');if(!p||!p.offsetWidth) return false;"
                    + "var t=p.querySelector('table'); return !!(t && window.jQuery && jQuery.fn.dataTable.isDataTable(t));");
            if (Boolean.TRUE.equals(ok)) break;
            Thread.sleep(500);
        }
        Thread.sleep(500);
    }

    public boolean isPopupVisible() { return isShown(By.cssSelector(POPUP + " table")); }

    // ✅ Popup header, e.g. "ABP News Network · 6 admission(s) ABP - Female Employees with child below 18 months"
    public String getPopupHeader() {
        return String.valueOf(js().executeScript("var p=document.querySelector('" + POPUP + "');if(!p)return '';"
                + "var t=p.innerText.replace(/\\s+/g,' '); var i=t.indexOf('×'); return (i>0?t.substring(0,i):t.substring(0,200)).trim();"));
    }

    public List<String> getPopupHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + POPUP + " table thead th'), function(h){return h.textContent.trim();});"));
    }

    public int getPopupRowCount() {
        return ((Number) js().executeScript("var t=document.querySelector('" + POPUP + " table');"
                + "return jQuery(t).DataTable().rows({search:'applied'}).count();")).intValue();
    }

    // ✅ All values of a popup column (all rows, not just the page)
    public List<String> getPopupColumn(String header) {
        return strings(js().executeScript("var t=document.querySelector('" + POPUP + " table'), H=[].map.call(t.querySelectorAll('thead th'),function(h){return h.textContent.trim();});"
                + "var i=H.indexOf(arguments[0]); if(i<0) return [];"
                + "return jQuery(t).DataTable().column(i,{search:'applied'}).data().toArray().map(function(v){return jQuery('<div>').html(v).text().replace(/\\s+/g,' ').trim();});", header));
    }

    public String getPopupInfo() {
        return String.valueOf(js().executeScript("var i=document.querySelector('" + POPUP + " .dataTables_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public boolean isPopupSearchVisible() { return isShown(By.cssSelector(POPUP + " .dataTables_filter input")); }

    public boolean isPopupDownloadVisible() { return isShown(By.cssSelector(POPUP + " .dt-buttons .dt-button")); }

    public void searchPopup(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(POPUP + " .dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1000);
    }

    public String getPopupEmptyMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + POPUP + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public void closePopup() throws InterruptedException {
        js().executeScript("var c=document.querySelector('" + POPUP + " .close, " + POPUP + " .close-popdown, " + POPUP + " [data-dismiss]'); if(c) c.click();"
                + "var p=document.querySelector('" + POPUP + "'); if(p) p.style.display='none'; var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';");
        Thread.sleep(800);
    }
}
