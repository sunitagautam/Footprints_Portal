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
 * Franchise → Corporate Tieups → Admission Records (/corporate_admissions).
 * Filter form #frm_invoice_report (posts back to the page); DataTable "table.dataTable" (client side, max 500 rows;
 * the "Download All N Records" button exports every matching record). Most columns are hidden ("never") and are
 * read through DataTables, by header text.
 */
public class AdmissionRecordsPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_invoice_report";
    private static final String EDIT = "#popdown-dialog #frm_edit_invite";

    public AdmissionRecordsPage(WebDriver driver) {
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
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('table.dataTable'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getHeading() {
        return String.valueOf(js().executeScript("var h=document.querySelector('h2');return h?h.textContent.trim():'';"));
    }

    // ════════════════ Filters ════════════════
    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " > div > label, "
                + FILTER + " div > label.control-label, " + FILTER + " label'), function(l){return l.textContent.trim();})"
                + ".filter(function(t){return t && t.length<40;});"));
    }

    public boolean isFilterFieldVisible(String id) { return isShown(By.cssSelector(FILTER + " #" + id)); }

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

    // ✅ Plain or select2 select — by exact option text, fires change
    public void selectByText(String id, String text) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #" + id + "'), t=arguments[0];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                + "if(!o.length) return false; s.val(o.first().val()).trigger('change'); return true;", text);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Option '" + text + "' not in #" + id);
    }

    // ✅ First option of a select that starts with the prefix ("" when none)
    public String findOption(String id, String prefix) {
        for (String o : getSelectOptions(id))
            if (o.startsWith(prefix)) return o;
        return "";
    }

    // ✅ pickadate field (sr_joining_from / _to, sr_billing_from / _to) — month is 1-based here
    public void setDate(String id, int year, int month, int day) {
        js().executeScript("jQuery('#" + id + "').pickadate('picker').set('select',[arguments[0],arguments[1],arguments[2]]);",
                year, month - 1, day);
    }

    public String getFieldValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + FILTER + " #" + id + "');return e?e.value:'';"));
    }

    public boolean isChecked(String id) {
        return Boolean.TRUE.equals(js().executeScript("var e=document.getElementById(arguments[0]);return !!(e&&e.checked);", id));
    }

    public void setChecked(String id, boolean on) {
        js().executeScript("var e=jQuery('#'+arguments[0]); e.prop('checked', arguments[1]); if(jQuery.uniform) jQuery.uniform.update(e);", id, on);
    }

    // ✅ bootstrap-multiselect (City city_filter_list[] / Center #center_selection). The page's onChange handler calls
    //    $.uniform.update, which is undefined on this page, so options are selected without firing it
    public List<String> getMultiselectOptions(String selectCss) {
        return strings(js().executeScript("return jQuery(arguments[0]).find('option').map(function(){return jQuery(this).text().trim();}).get();", selectCss));
    }

    public void multiselectOnly(String selectCss, String optionText) {
        Object ok = js().executeScript("var s=jQuery(arguments[0]), t=arguments[1];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.multiselect('deselectAll', false); s.multiselect('select', o.first().val()); s.multiselect('refresh'); return true;",
                selectCss, optionText);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("'" + optionText + "' not in " + selectCss);
    }

    public List<String> getMultiselectSelected(String selectCss) {
        return strings(js().executeScript("return jQuery(arguments[0]).find('option:selected').map(function(){return jQuery(this).text().trim();}).get();", selectCss));
    }

    public void clickFilter() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        System.out.println("▶ Filter clicked");
        Thread.sleep(2000);
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Table ════════════════
    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('table.dataTable thead th'), function(h){return h.offsetWidth>0;})"
                + ".map(function(h){return h.textContent.trim();});"));
    }

    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('table.dataTable thead th'), function(h){return h.textContent.trim();});"));
    }

    public boolean isColumnVisible(String header) {
        return Boolean.TRUE.equals(js().executeScript("var A=arguments[0]; return [].some.call(document.querySelectorAll('table.dataTable thead th'), function(h){"
                + "return h.textContent.trim()===A && h.offsetWidth>0;});", header));
    }

    private int colIndex(String header) {
        int i = getAllColumnHeaders().indexOf(header);
        if (i < 0) throw new IllegalArgumentException("No column " + header);
        return i;
    }

    // ✅ Every value of a column (all filtered rows, not just the page) — HTML stripped
    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('table.dataTable').DataTable();"
                + "return dt.column(arguments[0],{search:'applied'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    // ✅ Values of the rows on the current page only, in display order
    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('table.dataTable').DataTable();"
                + "return dt.column(arguments[0],{page:'current'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('table.dataTable').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('table.dataTable td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('table.dataTable').DataTable().page.len();")).intValue();
    }

    public String getSearchPlaceholder() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_filter input');return i?i.placeholder:'';"));
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
        js().executeScript("var A=arguments[0]; [].filter.call(document.querySelectorAll('table.dataTable thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0].click();", header);
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return String.valueOf(js().executeScript("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('table.dataTable thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0];"
                + "return h?h.className:'';", header));
    }

    // ✅ Child ID link (hidden first cell) of the first row on the page → its href
    public String getFirstChildIdHref() {
        return String.valueOf(js().executeScript("var a=document.querySelector('table.dataTable tbody tr td:first-child a[href*=account_statement]');"
                + "return a?a.getAttribute('href')+' | '+a.getAttribute('target')+' | '+a.textContent.trim():'';"));
    }

    // ✅ Clicks the first row's Child Name → "Child Information" popup
    public String openFirstChildInfo() throws InterruptedException {
        String name = String.valueOf(js().executeScript("var a=document.querySelector('table.dataTable tbody a[title=\"Child Information\"]');"
                + "if(!a) return ''; a.click(); return a.textContent.trim();"));
        Thread.sleep(3000);
        return name;
    }

    public String getPopupText() {
        return String.valueOf(js().executeScript("var p=document.querySelector('#popdown-dialog');return p&&p.offsetWidth>0?p.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    // ════════════════ Edit Invite Details ════════════════
    // ✅ Row index (page order) and invite id of rows whose Employment Details are filled, offer not starting with skipPrefix
    @SuppressWarnings("unchecked")
    public List<String> findRowsWithEmploymentDetails(String skipPrefix) {
        Object r = js().executeScript("var SKIP=arguments[0], dt=jQuery('table.dataTable').DataTable(), H=[].map.call(document.querySelectorAll('table.dataTable thead th'),function(h){return h.textContent.trim();});"
                + "var iC=H.indexOf('Employee Code'), iM=H.indexOf('Employer'), iO=H.indexOf('Offer Title'), iId=H.indexOf('Child ID'), out=[];"
                + "dt.rows({search:'applied'}).every(function(){var d=this.data(), n=this.node(); var t=function(v){return jQuery('<div>').html(v).text().trim();};"
                + " var a=n.querySelector('a[href*=admission_invite_details]'); if(!a) return;"
                + " if(t(d[iC]) && t(d[iM]) && t(d[iO]).indexOf(SKIP)!==0) out.push(t(d[iId])+'|'+a.getAttribute('href').replace(/.*invite_id=/,''));});"
                + "return out;", skipPrefix);
        return strings(r);
    }

    // ✅ Opens the Edit Invite Details popup of a child (searches the grid for the child id first)
    public void openEditInviteDetails(String childId) throws InterruptedException {
        js().executeScript("jQuery('table.dataTable').DataTable().search(arguments[0]).draw();", childId);
        Thread.sleep(800);
        hideApiDebugPanel();
        Object ok = js().executeScript("var C=arguments[0]; var n=jQuery('table.dataTable').DataTable().rows({search:'applied'}).nodes().toArray().filter(function(n){"
                + "return n.querySelector('a[href=\"account_statement?child_id='+C+'\"]');})[0];"
                + "if(!n) return false; var a=n.querySelector('a[href*=admission_invite_details]'); a.scrollIntoView({block:'center'}); a.click(); return true;", childId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Child " + childId + " not in the grid");
        System.out.println("▶ Edit Invite Details clicked (child " + childId + ")");
        for (int i = 0; i < 30; i++) {
            Object bound = js().executeScript("var b=document.querySelector('" + EDIT + " #update_invite');"
                    + "if(!b || !window.jQuery) return false; var ev=jQuery._data(b,'events'); return !!(ev && ev.click);");
            if (Boolean.TRUE.equals(bound)) break;
            Thread.sleep(500);
        }
        Thread.sleep(500);
    }

    public boolean isEditPopupVisible() { return isShown(By.cssSelector(EDIT)); }

    public String getEditPopupTitle() {
        return String.valueOf(js().executeScript("var t=document.querySelector('" + EDIT + " .modal-title');return t?t.textContent.trim():'';"));
    }

    public List<String> getEditLabels() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + EDIT + " label'), function(l){return l.textContent.replace(/\\s+/g,' ').replace(/\\s*:\\s*$/,'').trim();})"
                + ".filter(function(t){return t && t!=='\\u00a0';});"));
    }

    public String getEditValue(String id) {
        return driver.findElement(By.cssSelector(EDIT + " #" + id)).getAttribute("value");
    }

    public void enterEditField(String id, String value) {
        WebElement e = driver.findElement(By.cssSelector(EDIT + " #" + id));
        e.clear();
        if (!value.isEmpty()) e.sendKeys(value);
    }

    // ✅ Records native confirms (also across the reload) — acceptConfirm=false answers Cancel
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__reqs=[]; try{ sessionStorage.setItem('__confirms','[]'); }catch(e){}"
                + "var o=XMLHttpRequest.prototype.open; XMLHttpRequest.prototype.open=function(m,u){window.__reqs.push(m+' '+u); return o.apply(this,arguments);};"
                + "window.confirm=function(m){ try{ var c=JSON.parse(sessionStorage.getItem('__confirms')||'[]'); c.push(m);"
                + " sessionStorage.setItem('__confirms', JSON.stringify(c)); }catch(e){} return " + acceptConfirm + "; };");
    }

    public List<String> getCapturedConfirms() {
        return strings(js().executeScript("try{ return JSON.parse(sessionStorage.getItem('__confirms')||'[]'); }catch(e){ return []; }"));
    }

    public List<String> getCapturedRequests() {
        return strings(js().executeScript("return window.__reqs || [];"));
    }

    public void clickUpdateInvite() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("arguments[0].click();", driver.findElement(By.cssSelector(EDIT + " #update_invite")));
        System.out.println("▶ Update Invite clicked");
        Thread.sleep(1500);
    }

    // ✅ Visible validation messages of the edit popup ("<field> => <message>")
    public List<String> getEditErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + EDIT + " label.text-danger'), function(l){return l.offsetWidth>0 && l.textContent.trim();})"
                + ".map(function(l){return (l.getAttribute('for')||l.id)+' => '+l.textContent.trim();});"));
    }

    // ✅ After a save the page reloads; the flash message comes from a cookie
    public String waitForReloadAndFlash() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Object m = js().executeScript("var f=[].filter.call(document.querySelectorAll('.flash-msg, .alert'), function(e){return e.offsetWidth>0 && e.textContent.trim();});"
                    + "return f.length? f.map(function(e){return e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim();}).join(' | ') : '';");
            if (m != null && !String.valueOf(m).isEmpty()) return String.valueOf(m);
            Thread.sleep(500);
        }
        return "";
    }
}
