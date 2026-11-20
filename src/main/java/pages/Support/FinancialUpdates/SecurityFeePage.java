package pages.Support.FinancialUpdates;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// SECURITY FEE (Support → Financial Updates → Security Fee, /security_deposit) — read-only report
// Selectors confirmed live 2026-11-20 (as Rakesh) — see CLAUDE.md "Requirements — Support → Financial Updates".
//
// Heading h5 "Security Deposit Fee". Filter form #dateform (posts back): Date From #date_from / Date To #date_to (pickadate,
// "20 October, 2026"; default today − 1 month → today), Status multiselect #record_status name record_status[]
// (value=label: Received=Deposited, Transferred, Partially Refunded, Refunded; "-- All --" when none ticked), Center
// #center_selection (select2, "All Center"), Submit input[name=filter_list], Reset #btn_reset.
// Table table.datatable-security-listing (client side, 10 per page, order Received Date desc, CSV "Download Report"):
// 0 Child Name (account_statement link) | 1 Child Id (never) | 2 Description | 3 Amount Deposit | 4 Received Date |
// 5 Status | 6 Remaining Amount | 7 Transfer Date | 8 Refund Details ("Date: <d>" + amount) | 9 Adjusted Amount.
// List API financialsupport/listSecurityFeeList (limit 5000).
// ═══════════════════════════════════════════════════════════════
public class SecurityFeePage {

    WebDriver driver;
    WebDriverWait wait;

    public static final String TABLE = "table.datatable-security-listing";
    public static final List<String> KEYS = Arrays.asList("Child Name", "Child Id", "Description", "Amount Deposit", "Received Date",
            "Status", "Remaining Amount", "Transfer Date", "Refund Details", "Adjusted Amount");

    public SecurityFeePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
    }

    private JavascriptExecutor js() { return (JavascriptExecutor) driver; }

    private String str(String script, Object... args) {
        Object o = js().executeScript(script, args);
        return o == null ? "" : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(String.valueOf(x));
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> map(Object o) {
        Map<String, String> m = new LinkedHashMap<>();
        if (o instanceof Map) ((Map<String, Object>) o).forEach((k, v) -> m.put(k, String.valueOf(v)));
        return m;
    }

    private boolean isShown(By by) {
        for (WebElement e : driver.findElements(by))
            try { if (e.isDisplayed()) return true; } catch (Exception ignored) { }
        return false;
    }

    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(d -> !d.findElements(By.cssSelector("#dateform #btn_reset")).isEmpty()
                    && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMenuPath() {
        return str("var s=(window.menuData||[]).filter(function(x){return x.url==='security_deposit';})[0]; return s ? s.topMenu+' > '+s.subMenu+' > '+s.title : '';");
    }

    public String getHeading() {
        return str("var h=[].filter.call(document.querySelectorAll('h5'),function(h){return h.offsetWidth>0 && /Security/.test(h.textContent);})[0]; return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('#dateform label'),function(l){return l.offsetWidth>0 && l.textContent.trim() && !l.closest('.multiselect-container');})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    // ════════════════ Filters ════════════════
    public String getValue(String id) {
        return str("var e=document.querySelector('#dateform #'+arguments[0]); if(!e) return ''; return e.tagName==='SELECT'?(e.selectedIndex<0?'':e.options[e.selectedIndex].text.trim()):e.value;", id);
    }

    public void setDate(String id, String iso) {
        js().executeScript("var p=jQuery('#dateform #'+arguments[0]).pickadate('picker'); if(!arguments[1]){p.clear(); return;} var d=arguments[1].split('-'); p.set('select',[+d[0],+d[1]-1,+d[2]]);", id, iso);
        System.out.println("▶ " + id + " = " + iso);
    }

    // ✅ Status options as "label=value"
    public List<String> getStatusOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('#record_status option'),function(o){return o.text.trim()+'='+o.value;});"));
    }

    public List<String> getStatusSelected() {
        return strings(js().executeScript("return jQuery('#record_status').val()||[];"));
    }

    public String getStatusButtonText() {
        return str("var b=document.querySelector('#dateform button.multiselect'); return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    // ✅ Status by option VALUE (Received / Transferred / Partially Refunded / Refunded); none = all
    public void setStatuses(String... values) {
        js().executeScript("var s=jQuery('#record_status'); s.multiselect('deselectAll',false); s.val(arguments[0]); s.multiselect('refresh');", Arrays.asList(values));
        System.out.println("▶ Status = " + Arrays.toString(values));
    }

    public List<String> getCenterOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('#center_selection option'),function(o){return o.text.trim()+'='+o.value;});"));
    }

    public void setCenter(String value) {
        js().executeScript("jQuery('#center_selection').val(arguments[0]).trigger('change');", value);
        System.out.println("▶ Center = " + value);
    }

    public void clickSubmit() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#dateform input[name=filter_list]').click();");
        System.out.println("▶ Submit clicked");
        Thread.sleep(2500);
        isPageLoaded();
    }

    public void clickReset() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#dateform #btn_reset').click();");
        System.out.println("▶ Reset clicked");
        Thread.sleep(2500);
        isPageLoaded();
    }

    // ════════════════ Table ════════════════
    public List<String> getDefinedHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return !/\\bnever\\b/.test(h.className);})"
                + ".map(function(h){return h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRows() {
        Object o = js().executeScript("var K=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray().map(function(r){ var out={};"
                + " for(var i=0;i<K.length;i++) out[K[i]]=r.children[i]?r.children[i].innerText.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';"
                + " var a=r.children[0].querySelector('a'); out.childHref=a?a.getAttribute('href')+'|'+(a.getAttribute('target')||''):''; return out;});", KEYS);
        List<Map<String, String>> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(map(x));
        return out;
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public String getInfoText() {
        return str("var i=document.querySelector('.dataTables_info');return i?i.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';");
    }

    public String getEmptyMessage() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';");
    }

    public String getOrder() {
        return str("return JSON.stringify(jQuery('" + TABLE + "').DataTable().order());");
    }

    public boolean isSearchBoxVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }

    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    public String getDownloadButtonText() {
        return str("var b=document.querySelector('.dt-buttons .dt-button');return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('.dt-buttons .dt-button').click();");
        System.out.println("▶ Download Report clicked");
        Thread.sleep(1500);
    }

    public void searchTable(String keyword) throws InterruptedException {
        js().executeScript("jQuery('" + TABLE + "').DataTable().search(arguments[0]).draw(); var i=document.querySelector('.dataTables_filter input'); if(i) i.value=arguments[0];", keyword);
        Thread.sleep(1000);
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public int getCurrentPage() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.info().page;")).intValue() + 1;
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("var A=arguments[0]; [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\s+/g,' ').trim()===A && h.offsetWidth>0;})[0].click();", header);
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return str("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\s+/g,' ').trim()===A;})[0]; return h?h.className:'';", header);
    }

    // ✅ Column values in the drawn order, from DataTables' sort data
    public List<String> getSortData(int col) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable(), i=arguments[0]; return dt.rows({search:'applied',order:'applied'}).indexes().toArray().map(function(r){return String(dt.cell(r,i).render('sort')).replace(/<.*?>/g,'').trim();});", col));
    }
}
