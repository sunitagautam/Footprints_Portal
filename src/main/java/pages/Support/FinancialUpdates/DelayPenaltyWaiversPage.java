package pages.Support.FinancialUpdates;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// DELAY PENALTY WAIVERS (Support → Financial Updates → Delay Penalty Waivers, /waive-off_delay_penalty)
// All selectors confirmed live 2026-11-19 (after the "clx" redesign) — see CLAUDE.md
// "Requirements — Support → Financial Updates".
//
// Top form #frm_delay_penalty: Center (bootstrap-multiselect #waiveoff_center_id), Child Status
// (#waiveoff_child_status active|inactive), Child (select2 #waiveoff_child_id, loaded by AJAX
// api/admission/pickerChildrenByStatus/ 400 ms after a centre / status change), Send Link (#send_link).
// Send Link → jQuery-validate "Please Select child!" → native confirm "Are you sure want to send delay
// penalty waived-off link?" → POST api/financials/updateWaiveOffDelayData/ → flash cookie + page reload.
// Resend (button#btn_resend_<waiveoffId>, Open rows only) → confirm "Are you sure want to re-send delay
// penalty waived-off link?" → same API with {waiveoff_id, email:'only'} → flash_message, no reload.
// Table .dt-delay-waiveoff (#DataTables_Table_0), client side, 20 per page, order WaiveOff ID desc:
// Child ID (hidden) | WaiveOff ID (hidden) | Child Name | Issue On | Used On | Waive-off Penalty | Status | Action.
// ═══════════════════════════════════════════════════════════════
public class DelayPenaltyWaiversPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FORM = "#frm_delay_penalty";
    private static final String TABLE = ".dt-delay-waiveoff";

    public DelayPenaltyWaiversPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

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

    private boolean isShown(By by) {
        for (WebElement e : driver.findElements(by))
            try {
                if (e.isDisplayed()) return true;
            } catch (Exception ignored) {
            }
        return false;
    }

    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(d -> !d.findElements(By.cssSelector(FORM + " #send_link")).isEmpty()
                    && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMenuPath() {
        return str("var s=(window.menuData||[]).filter(function(x){return x.url==='waive-off_delay_penalty';})[0];"
                + "return s ? s.topMenu+' > '+s.subMenu+' > '+s.title : '';");
    }

    // ✅ h3 headings: "Waive-off Delay Penalty", "Waive-off Links Issued"
    public List<String> getHeadings() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('h3'),function(h){return h.offsetWidth>0 && /Waive/.test(h.textContent);})"
                + ".map(function(h){return h.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    public List<String> getFormLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + FORM + " label.clx-lbl'),function(l){return l.offsetWidth>0;})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    // ════════════════ Center (bootstrap-multiselect) ════════════════
    public String getCenterButtonText() {
        return str("var b=jQuery('#waiveoff_center_id').next('.btn-group').find('button.multiselect');"
                + "return b.length ? b.text().replace(/\\s+/g,' ').trim() : '';");
    }

    public List<String> getCenterOptions() {
        return strings(js().executeScript("return jQuery('#waiveoff_center_id option').map(function(){return jQuery(this).text().trim();}).get();"));
    }

    // ✅ Selects exactly these centres and fires change (the page loads the children 400 ms later)
    public void selectCenters(String... names) throws InterruptedException {
        Object missing = js().executeScript("var s=jQuery('#waiveoff_center_id'), want=arguments[0], miss=[];"
                + "s.multiselect('deselectAll', false);"
                + "want.forEach(function(t){var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                + " if(!o.length) miss.push(t); else s.multiselect('select', o.first().val());});"
                + "s.multiselect('refresh'); s.trigger('change'); return miss.join(',');", java.util.Arrays.asList(names));
        if (missing != null && !String.valueOf(missing).isEmpty())
            throw new IllegalArgumentException("Centre not found: " + missing);
        System.out.println("▶ Centre(s) selected: " + java.util.Arrays.toString(names));
        waitForChildrenLoaded();
    }

    public void clearCenters() throws InterruptedException {
        js().executeScript("var s=jQuery('#waiveoff_center_id'); s.multiselect('deselectAll', false); s.multiselect('refresh'); s.trigger('change');");
        Thread.sleep(1500);
    }

    // ════════════════ Child Status / Child ════════════════
    public String getChildStatusText() {
        return str("var s=document.getElementById('waiveoff_child_status'); return s.options[s.selectedIndex].text.trim();");
    }

    public List<String> getChildStatusOptions() {
        return strings(js().executeScript("return [].map.call(document.getElementById('waiveoff_child_status').options,function(o){return o.text.trim();});"));
    }

    public void selectChildStatus(String value) throws InterruptedException {
        js().executeScript("jQuery('#waiveoff_child_status').val(arguments[0]).trigger('change');", value);
        System.out.println("▶ Child Status = " + value);
        waitForChildrenLoaded();
    }

    // ✅ Waits for the 400 ms debounce + the picker AJAX (the loading block clears when done)
    public void waitForChildrenLoaded() throws InterruptedException {
        Thread.sleep(900);
        for (int i = 0; i < 40; i++) {
            Boolean busy = (Boolean) js().executeScript("return jQuery('.loading-block .blockUI').length>0 || (jQuery.active||0)>0;");
            if (!Boolean.TRUE.equals(busy)) break;
            Thread.sleep(500);
        }
        Thread.sleep(300);
    }

    // ✅ Child dropdown options except the "Select Child" placeholder, e.g. "Aarav Pillai- 63660: Abhijeet Pillai, Preeti Nair"
    public List<String> getChildOptions() {
        return strings(js().executeScript("return jQuery('#waiveoff_child_id option').filter(function(){return this.value!=='';}).map(function(){return jQuery(this).text().trim();}).get();"));
    }

    public List<String> getChildOptionValues() {
        return strings(js().executeScript("return jQuery('#waiveoff_child_id option').filter(function(){return this.value!=='';}).map(function(){return this.value;}).get();"));
    }

    public String getChildSelectionText() {
        return str("var c=jQuery('#waiveoff_child_id').next('.select2').find('.select2-selection__rendered'); return c.length?c.text().trim():'';");
    }

    public boolean isChildHintVisible() {
        return Boolean.TRUE.equals(js().executeScript("var h=document.getElementById('waiveoff_child_hint'); return !!(h && h.offsetWidth>0);"));
    }

    public String getChildHintText() {
        return str("var h=document.getElementById('waiveoff_child_hint'); return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    // ✅ Picks a child by id through select2 (fires the page's change handler, which filters the table)
    public void selectChild(String childId) throws InterruptedException {
        Object ok = js().executeScript("var s=jQuery('#waiveoff_child_id'); if(!s.find('option[value=\"'+arguments[0]+'\"]').length) return false;"
                + "s.val(arguments[0]).trigger('change'); return true;", childId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Child " + childId + " not in the dropdown");
        System.out.println("▶ Child selected: " + childId);
        Thread.sleep(800);
    }

    public String getChildValidationError() {
        return str("var e=document.getElementById('waiveoff_child_id-error'); return e && e.offsetWidth>0 ? e.textContent.trim() : '';");
    }

    // ════════════════ Send Link / Resend ════════════════
    public boolean isSendLinkVisible() {
        return isShown(By.cssSelector(FORM + " #send_link"));
    }

    public String getSendLinkClass() {
        return str("return document.getElementById('send_link').className;");
    }

    public void clickSendLink() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.getElementById('send_link').click();");
        System.out.println("▶ Send Link clicked");
        Thread.sleep(1000);
    }

    public void clickResend(String waiveoffId) throws InterruptedException {
        goToRowPage(waiveoffId);
        Object ok = js().executeScript("var b=document.getElementById('btn_resend_'+arguments[0]); if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;", waiveoffId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No Resend button for waive-off " + waiveoffId);
        System.out.println("▶ Resend clicked (waive-off " + waiveoffId + ")");
        Thread.sleep(1000);
    }

    // ✅ Waits for the reload after Send Link (page reloads on success) — returns once the table is back
    public void waitForReload() throws InterruptedException {
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    // ✅ Visible flash message(s) (Send Link result after reload, Resend result, errors)
    public String getFlashMessage() {
        return str("var o=[]; document.querySelectorAll('.flash-msg .alert, .flash-msg').forEach(function(e){"
                + "var t=e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim(); if(e.offsetWidth>0 && t && o.indexOf(t)<0) o.push(t);}); return o.join(' | ');");
    }

    public String waitForFlashMessage() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            String m = getFlashMessage();
            if (!m.isEmpty()) return m;
            Thread.sleep(500);
        }
        return "";
    }

    // ════════════════ Confirm / request capture ════════════════
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__confirms=[]; window.confirm=function(m){ window.__confirms.push(m); return " + acceptConfirm + "; };");
    }

    public List<String> getCapturedConfirms() {
        return strings(js().executeScript("return window.__confirms || [];"));
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\u00a0/g,' ').trim();});"));
    }

    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.offsetWidth>0;})"
                + ".map(function(h){return h.textContent.replace(/\\u00a0/g,' ').trim();});"));
    }

    private int colIndex(String header) {
        int i = getAllColumnHeaders().indexOf(header);
        if (i < 0) throw new IllegalArgumentException("No column " + header);
        return i;
    }

    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public List<String> getOrderedColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied',order:'applied'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{page:'current'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    // ✅ One row (header → text) by waive-off id; empty map if absent
    @SuppressWarnings("unchecked")
    public Map<String, String> getRow(String waiveoffId) {
        Object o = js().executeScript("var I=arguments[0], hs=[].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.replace(/\\u00a0/g,' ').trim();});"
                + "var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){return r.getAttribute('data-id')===I;})[0];"
                + "if(!r) return null; var out={}; [].forEach.call(r.children,function(td,i){ out[hs[i]||('col'+i)]=td.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"
                + "out['Resend']=r.querySelector('[id^=btn_resend_]')?'yes':'no'; return out;", waiveoffId);
        Map<String, String> m = new LinkedHashMap<>();
        if (o instanceof Map) ((Map<String, Object>) o).forEach((k, v) -> m.put(k, String.valueOf(v)));
        return m;
    }

    // ✅ All waive-off ids for a child, newest first
    public List<String> getWaiveoffIdsForChild(String childId) {
        return strings(js().executeScript("var C=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows().nodes().toArray()"
                + ".filter(function(r){return r.children[0].textContent.trim()===C;})"
                + ".map(function(r){return r.getAttribute('data-id');}).sort(function(a,b){return b-a;});", childId));
    }

    // ✅ Status → {count, withResend, usedOnFilled, nonZeroPenalty} over every row
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Long>> getStatusSummary() {
        Object o = js().executeScript("var out={}, hs=[].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.trim();});"
                + "var si=hs.indexOf('Status'), ui=hs.indexOf('Used On'), pi=hs.indexOf('Waive-off Penalty');"
                + "jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().forEach(function(r){var s=r.children[si].textContent.trim();"
                + " var x=out[s]||(out[s]={count:0,withResend:0,usedOnFilled:0,nonZeroPenalty:0}); x.count++;"
                + " if(r.querySelector('[id^=btn_resend_]')) x.withResend++; if(r.children[ui].textContent.trim()) x.usedOnFilled++;"
                + " if(parseFloat(r.children[pi].textContent.replace('Rs.','').replace(/[^0-9.]/g,''))>0) x.nonZeroPenalty++;}); return out;");
        Map<String, Map<String, Long>> m = new LinkedHashMap<>();
        if (o instanceof Map) ((Map<String, Object>) o).forEach((k, v) -> {
            Map<String, Long> in = new LinkedHashMap<>();
            ((Map<String, Object>) v).forEach((a, b) -> in.put(a, ((Number) b).longValue()));
            m.put(k, in);
        });
        return m;
    }

    // ✅ First row id with the given status
    public String getFirstWaiveoffIdWithStatus(String status) {
        return str("var S=arguments[0], hs=[].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.trim();}), si=hs.indexOf('Status');"
                + "var r=jQuery('" + TABLE + "').DataTable().rows({order:'applied'}).nodes().toArray().filter(function(r){return r.children[si].textContent.trim()===S;})[0];"
                + "return r ? r.getAttribute('data-id') : '';", status);
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getTotalRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows().count();")).intValue();
    }

    public int getPageRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({page:'current'}).count();")).intValue();
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public String getInfoText() {
        return str("var i=document.querySelector('#DataTables_Table_0_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public String getEmptyTableMessage() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';");
    }

    public String getSearchValue() {
        return str("var e=document.querySelector('#DataTables_Table_0_filter input'); return e?e.value:'';");
    }

    public boolean isSearchVisible() { return isShown(By.cssSelector("#DataTables_Table_0_filter input")); }

    public boolean isPaginationVisible() { return isShown(By.cssSelector("#DataTables_Table_0_paginate")); }

    public boolean isDownloadButtonPresent() {
        return !driver.findElements(By.cssSelector("#DataTables_Table_0_wrapper .dt-buttons")).isEmpty();
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector("#DataTables_Table_0_filter input"));
        box.clear();
        if (!keyword.isEmpty()) box.sendKeys(keyword);
        else js().executeScript("jQuery('" + TABLE + "').DataTable().search('').draw();");
        Thread.sleep(1200);
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('#DataTables_Table_0_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('#DataTables_Table_0_paginate .paginate_button.previous').click();");
        Thread.sleep(800);
    }

    public void clickPageNumber(String n) throws InterruptedException {
        js().executeScript("var N=arguments[0]; [].filter.call(document.querySelectorAll('#DataTables_Table_0_paginate .paginate_button'),function(a){return a.textContent.trim()===N;})[0].click();", n);
        Thread.sleep(800);
    }

    public int getCurrentPage() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.info().page;")).intValue() + 1;
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("var A=arguments[0]; [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0].click();", header);
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return str("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0];"
                + "return h?h.className:'';", header);
    }

    public void goToRowPage(String waiveoffId) throws InterruptedException {
        js().executeScript("var I=arguments[0], dt=jQuery('" + TABLE + "').DataTable();"
                + "var idx=dt.rows({search:'applied',order:'applied'}).nodes().toArray().findIndex(function(r){return r.getAttribute('data-id')===I;});"
                + "if(idx>=0){ dt.page(Math.floor(idx/dt.page.len())).draw('page'); }", waiveoffId);
        Thread.sleep(600);
    }

    public String getChildInfoHref(String waiveoffId) {
        return str("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){return r.getAttribute('data-id')===I;})[0];"
                + "if(!r) return ''; var a=r.querySelector('a[href*=pop_child_info]'); return a?a.getAttribute('href'):'';", waiveoffId);
    }
}
