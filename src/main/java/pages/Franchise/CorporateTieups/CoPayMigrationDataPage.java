package pages.Franchise.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Co-Pay Migration Data (/copay_migration_data).
 * Heading h5 "Co-Pay Migration Data". Filter form #frm_invoice_report (posts back): Center #center_selection (select2,
 * "All Center"), Status #migration_updated [All, Pending, Updated], hidden #exclude_inhouse (ticked), Filter #btn_submit.
 * Table #DataTables_Table_0 (.dt-admission-invite, 20 per page): Child Name | Length | Company Name | Breakup | Payment Date
 * | Monthly Fee | Existing Breakup (Monthly) | Updated Breakup (Monthly) | Update, plus 8 "never" columns used by the CSV
 * (copay_annual_fee_migration.csv). Update = copay_migration_setup?pop=yes&child_id=&m=&cm=&y=&cy= (popdown_xl_large):
 * form #frm-copay-migration-update, plan multiselects #parent_monthly_plan / #parent_yearly_plan / #corp_monthly_plan /
 * #corp_yearly_plan, Update #btn_pop_update_migration_plan → no confirm → POST api/corporate/updateCoPayNewPlan4Migration/
 * → message in .plan-error → page reload.
 */
public class CoPayMigrationDataPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_invoice_report";
    private static final String TABLE = "#DataTables_Table_0";
    private static final String POPUP = "#popdown-dialog";

    public CoPayMigrationDataPage(WebDriver driver) {
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
            wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getHeading() {
        return String.valueOf(js().executeScript("var h=[].find.call(document.querySelectorAll('h5.panel-title,h4,h2'), function(x){return /Co-Pay Migration/.test(x.textContent);});return h?h.textContent.trim():'';"));
    }

    // ════════════════ Filters ════════════════
    public boolean isCenterFilterVisible() { return isShown(By.cssSelector(FILTER + " .select2-container")); }
    public boolean isStatusFilterVisible() { return isShown(By.cssSelector(FILTER + " #migration_updated")); }
    public boolean isFilterButtonVisible() { return isShown(By.cssSelector(FILTER + " #btn_submit")); }
    public boolean isExcludeInhouseVisible() { return isShown(By.cssSelector(FILTER + " #exclude_inhouse")); }
    public boolean isSearchVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }
    public boolean isDownloadVisible() { return isShown(By.cssSelector(".buttons-csv")); }
    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    public String getSelectedText(String id) {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + FILTER + " #'+arguments[0]);"
                + "return s && s.selectedIndex>=0 ? s.options[s.selectedIndex].text.trim() : '';", id));
    }

    public List<String> getOptions(String id) {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " #'+arguments[0]+' option'), function(o){return o.text.trim();});", id));
    }

    public void selectByText(String id, String text) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #'+arguments[0]), t=arguments[1];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.val(o.first().val()).trigger('change'); return true;", id, text);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("'" + text + "' not in #" + id);
    }

    public void clickFilter() throws InterruptedException {
        js().executeScript("document.body.setAttribute('data-qa-old','1');");
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState==='complete' && !document.body.hasAttribute('data-qa-old');")));
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().columns().header().toArray().map(function(h){return h.textContent.trim();});"));
    }

    // Headers the user sees (the CSV-only columns have class "never")
    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().columns().header().toArray()"
                + ".filter(function(h){return !/\\bnever\\b/.test(h.className);}).map(function(h){return h.textContent.trim();});"));
    }

    private int headerIndex(String header) {
        List<String> all = getAllColumnHeaders();
        int i = all.indexOf(header);
        if (i < 0) throw new IllegalArgumentException("Column '" + header + "' not in " + all);
        return i;
    }

    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied', order:'applied'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=String(v).replace(/<br\\s*\\/?>/g,' '); return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{page:'current'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=String(v).replace(/<br\\s*\\/?>/g,' '); return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    // Child ID is a "never" column; first column is the Child Name
    public String getRowText(String childId) {
        return String.valueOf(js().executeScript("var t=jQuery('" + TABLE + "').DataTable(), c=t.columns().header().toArray().map(function(h){return h.textContent.trim();}).indexOf('Child ID');"
                + "var id=arguments[0], r=t.rows().data().toArray().find(function(r){return String(r[c]).replace(/<[^>]*>/g,'').trim()===id;});"
                + "return r ? Array.from(r).map(function(v){var d=document.createElement('div'); d.innerHTML=String(v).replace(/<br\\s*\\/?>/g,' '); return d.textContent.replace(/\\s+/g,' ').trim();}).join(' | ') : '';", childId));
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

    // href of a child's Update link (plans to set: m / cm / y / cy)
    public String getUpdateHref(String childId) {
        Object h = js().executeScript("var id=arguments[0], a=[].find.call(jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().map(function(r){return r.querySelector(\"a[href^='copay_migration_setup']\");}),"
                + " function(a){return a && /[?&]child_id=/.test(a.getAttribute('href')) && new URLSearchParams(a.getAttribute('href').split('?')[1]).get('child_id')===id;});"
                + "return a ? a.getAttribute('href') : '';", childId);
        return String.valueOf(h);
    }

    // ════════════════ Update popup ════════════════
    public void openUpdate(String childId) throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("jQuery('" + TABLE + "').DataTable().search(arguments[0]).draw();", childId);
        Thread.sleep(800);
        js().executeScript("document.querySelector(\"" + TABLE + " a[href^='copay_migration_setup?pop=yes&child_id=\" + arguments[0] + \"&']\").click();", childId);
        new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> !d.findElements(By.cssSelector(POPUP + " #btn_pop_update_migration_plan")).isEmpty());
        Thread.sleep(2000);
    }

    public String getPopupTitle() {
        return String.valueOf(js().executeScript("var t=document.querySelector('" + POPUP + " .modal-title');return t?t.textContent.trim():'';"));
    }

    public String getPopupText() {
        return String.valueOf(js().executeScript("var d=document.querySelector('" + POPUP + "');return d?d.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    // Value of a read-only detail ("Child Name", "Existing Monthly Plan (Parent)", …)
    public String getPopupDetail(String label) {
        return String.valueOf(js().executeScript("var t=arguments[0], l=[].find.call(document.querySelectorAll('" + POPUP + " label'), function(x){return x.textContent.replace(/\\s+/g,' ').trim().replace(/\\s*:$/,'')===t;});"
                + "return l && l.nextElementSibling ? l.nextElementSibling.textContent.replace(/\\s+/g,' ').trim() : '';", label));
    }

    public boolean isPopupFieldPresent(String id) {
        return !driver.findElements(By.cssSelector(POPUP + " #" + id)).isEmpty();
    }

    public boolean isPopupFieldShown(String id) {
        return Boolean.TRUE.equals(js().executeScript("var e=document.querySelector('" + POPUP + " #'+arguments[0]); if(!e) return false;"
                + "var b=e.nextElementSibling && e.nextElementSibling.classList.contains('btn-group') ? e.nextElementSibling : e; return !!b.offsetParent;", id));
    }

    public String getPlanValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + POPUP + " #'+arguments[0]);return e&&e.value!=null?String(e.value):'';", id));
    }

    // ✅ bootstrap-multiselect: pick a value (or "" = --- Select Plan ---) through the plugin
    public void selectPlan(String id, String value) {
        js().executeScript("var s=jQuery('" + POPUP + " #'+arguments[0]); if(!s.length) return;"
                + "if (arguments[1]==='') { try { s.multiselect('deselectAll', false); } catch(e) {} s.val(''); try { s.multiselect('refresh'); } catch(e) {} }"
                + "else { try { s.multiselect('select', arguments[1], true); } catch(e) {} if (s.val()!==arguments[1]) s.val(arguments[1]).trigger('change'); }", id, value);
    }

    // ✅ the API reply is kept in sessionStorage, because success reloads the page before the message can be read
    public void clickUpdate() {
        js().executeScript("window.__qaReload=1; sessionStorage.removeItem('qaCopayMigration');"
                + "if (!window.__qaHook) { window.__qaHook=1; jQuery(document).ajaxComplete(function(e,x,s){ if (/updateCoPayNewPlan4Migration/.test(s.url))"
                + " sessionStorage.setItem('qaCopayMigration', 'HTTP '+x.status+' '+x.responseText); }); }"
                + "document.querySelector('" + POPUP + " #btn_pop_update_migration_plan').click();");
    }

    public String getPlanMessage() {
        return String.valueOf(js().executeScript("var e=[].filter.call(document.querySelectorAll('" + POPUP + " .plan-error, " + POPUP + " .flash-msg .alert, " + POPUP + " .plan-error ~ .flash-msg'),"
                + " function(x){return x.offsetParent && x.innerText.trim();}); return e.length ? e.map(function(x){return x.innerText.replace(/\\s+/g,' ').trim();}).join(' | ') : '';"));
    }

    // ✅ API reply of the last Update ("HTTP 200 {"status":"ok","message":"Details successfully Updated"}"), or the message shown
    public String waitForUpdateResult() throws InterruptedException {
        String last = "";
        for (int i = 0; i < 150; i++) {
            try {
                Object reply = js().executeScript("return sessionStorage.getItem('qaCopayMigration');");
                if (reply != null) return String.valueOf(reply);
                String m = getPlanMessage();
                if (!m.isEmpty() && !m.contains("Please Wait")) last = m;
            } catch (Exception ignored) {
            }
            Thread.sleep(200);
        }
        return last;
    }
}
