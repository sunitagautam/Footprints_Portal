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
// BACKEND UPDATE REQUESTS (Support → Financial Updates → Backend Update Requests, /backend_update_requests)
// Selectors confirmed live 2026-11-19/20 (as Rakesh) — see CLAUDE.md "Requirements — Support → Financial Updates".
//
// Filter form #frm-search (posts back): #sr_child_name, #requestto (default = logged-in user), #requestby,
// Status multiselect #admission_status name status[] (default Pending, Reopened), #type (12 types, no "Void Invoice"),
// #priority [High, Low, Medium]; #sr_submit Search, #sr_reset Reset.
// Table #datatable-task (client side, 10 per page, order Child Name asc). DOM columns: 0 Child Id(hidden) | 1 Child Name |
// 2 Request id(hidden) | 3 Request Type | 4 Requested Action | 5 Requested Action(hidden) | 6 Due Date | 7 Date of Request |
// 8 Requested By | 9 Requested To | 10 Comments(hidden) | 11 Center Information | 12 Priority(hidden) | 13 Status | 14 Action.
// High rows: style="background: #ffe7e7". CSV button "Download Report".
// Add / Update popup (popdown_big) form #frm_update posts back to the page; jQuery validate (label.text-danger).
// Void Invoice icon .void-invoice-btn → confirm "Are you sure you want to process this Void Invoice?" →
// api/backenderpupdates/updateInvoiceVoid → #backend_update_mesg "Invoice Voided Successfully" → reload after 2 s.
// Billing Setup Update popup backend_billing_update?pop=yes&child_id= — each section's Update button calls
// validateUpdateDetails(this, '<ctype>', '<fields>') → POST api/billing/updateBillingSetup?ctype=<ctype> (no confirm).
// ═══════════════════════════════════════════════════════════════
public class BackendUpdateRequestsPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String TABLE = "#datatable-task";
    private static final String POP = "#popdown-dialog";
    public static final List<String> COLUMN_KEYS = Arrays.asList("Child Id", "Child Name", "Request id", "Request Type",
            "Requested Action", "Requested Action Full", "Due Date", "Date of Request", "Requested By", "Requested To",
            "Comments", "Center Information", "Priority", "Status");

    public BackendUpdateRequestsPage(WebDriver driver) {
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

    @SuppressWarnings("unchecked")
    private Map<String, String> map(Object o) {
        Map<String, String> m = new LinkedHashMap<>();
        if (o instanceof Map) ((Map<String, Object>) o).forEach((k, v) -> m.put(k, String.valueOf(v)));
        return m;
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
            wait.until(d -> !d.findElements(By.cssSelector("#frm-search #sr_submit")).isEmpty()
                    && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMenuPath() {
        return str("var s=(window.menuData||[]).filter(function(x){return x.url==='backend_update_requests';})[0];"
                + "return s ? s.topMenu+' > '+s.subMenu+' > '+s.title : '';");
    }

    public String getHeading() {
        return str("var h=[].filter.call(document.querySelectorAll('h5'),function(h){return h.offsetWidth>0 && /Backend Update Requests/.test(h.textContent);})[0];"
                + "return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('#frm-search label.control-label'),function(l){return l.offsetWidth>0 && l.textContent.trim();})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    public boolean isAddButtonVisible() {
        return isShown(By.cssSelector("a[href^='add_backend_update_requests?pop=yes&action=add']"));
    }

    public boolean isSearchButtonVisible() { return isShown(By.cssSelector("#frm-search #sr_submit")); }

    public boolean isResetButtonVisible() { return isShown(By.cssSelector("#frm-search #sr_reset")); }

    // ✅ Notes legend text ("Notes: High Priority")
    public String getNotesText() {
        return str("var n=[].filter.call(document.querySelectorAll('*'),function(e){return e.children.length<6 && e.offsetWidth>0 && /^\\s*Notes\\s*:/.test(e.textContent) && /High Priority/.test(e.textContent);});"
                + "n.sort(function(a,b){return a.textContent.length-b.textContent.length;}); return n.length?n[0].textContent.replace(/\\s+/g,' ').trim():'';");
    }

    // ════════════════ Filters ════════════════
    public String getSelectText(String id) {
        return str("var s=document.querySelector('#frm-search #'+arguments[0]); return s.selectedIndex<0?'':s.options[s.selectedIndex].text.trim();", id);
    }

    public List<String> getSelectOptions(String id) {
        return strings(js().executeScript("return [].map.call(document.querySelector('#frm-search #'+arguments[0]).options,function(o){return o.text.trim();});", id));
    }

    public void setSelect(String id, String optionText) {
        Object ok = js().executeScript("var s=document.querySelector('#frm-search #'+arguments[0]), T=arguments[1];"
                + "var o=[].filter.call(s.options,function(x){return x.text.trim()===T;})[0]; if(!o) return false;"
                + "jQuery(s).val(o.value).trigger('change'); return true;", id, optionText);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No option '" + optionText + "' in #" + id);
        System.out.println("▶ Filter " + id + " = " + optionText);
    }

    public String getChildFilter() {
        return str("return document.querySelector('#frm-search #sr_child_name').value;");
    }

    public void setChildFilter(String text) {
        WebElement e = driver.findElement(By.cssSelector("#frm-search #sr_child_name"));
        e.clear();
        if (!text.isEmpty()) e.sendKeys(text);
        System.out.println("▶ Child Name/ Child ID = " + text);
    }

    public List<String> getStatusSelected() {
        return strings(js().executeScript("return jQuery('#admission_status').val()||[];"));
    }

    public List<String> getStatusOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('#admission_status option'),function(o){return o.text.trim();});"));
    }

    public String getStatusButtonText() {
        return str("var b=document.querySelector('#frm-search button.multiselect'); return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public void setStatuses(String... statuses) {
        js().executeScript("jQuery('#admission_status').val(arguments[0]).multiselect('refresh');", Arrays.asList(statuses));
        System.out.println("▶ Status = " + Arrays.toString(statuses));
    }

    public static final String[] ALL_STATUSES = {"Pending", "Completed", "Acknowledged", "Reopened", "Revoked", "Declined", "Attrition"};

    public void clickSearch() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#frm-search #sr_submit').click();");
        System.out.println("▶ Search clicked");
        Thread.sleep(2500);
        isPageLoaded();
    }

    public void clickReset() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#frm-search #sr_reset').click();");
        System.out.println("▶ Reset clicked");
        Thread.sleep(2500);
        isPageLoaded();
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }

    // ✅ Defined columns (th without the responsive "never" class); Date of Request / Priority may be folded into the + expander
    public List<String> getShownColumnHeaders() {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable(), C=dt.settings()[0].aoColumns; return [].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h,i){"
                + "return {t:h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim(), v:dt.column(i).visible() && !/\\bnever\\b/.test(h.className) && !/\\bnever\\b/.test(C[i].className||'')};})"
                + ".filter(function(x){return x.v;}).map(function(x){return x.t;});"));
    }

    // ✅ Every row (search applied): COLUMN_KEYS + "background" + "actions" (titles) + "iconCount" + "html"
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRows() {
        Object o = js().executeScript("var K=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray().map(function(r){ var out={};"
                + " for(var i=0;i<K.length;i++) out[K[i]]=r.children[i]?r.children[i].textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';"
                + " var m=(r.getAttribute('style')||'').match(/background:\\s*([^;]+)/); out.background=m?m[1].trim():'';"
                + " var a=r.children[14]; var ic=a?[].slice.call(a.querySelectorAll(':scope > a')):[];"
                + " out.actions=ic.map(function(x){return x.classList.contains('void-invoice-btn')?'Void Invoice':(x.getAttribute('title')||'').trim();}).join(', ');"
                + " out.iconCount=String(ic.length); return out;});", COLUMN_KEYS);
        List<Map<String, String>> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(map(x));
        return out;
    }

    public Map<String, String> getRowByRequestId(String requestId) {
        for (Map<String, String> r : getRows()) if (requestId.equals(r.get("Request id"))) return r;
        return null;
    }

    // ✅ href of an action icon of a request (by title; "Void Invoice" = the .void-invoice-btn)
    public String getActionHref(String requestId, String title) {
        return str("var I=arguments[0],T=arguments[1]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){return r.children[2].textContent.trim()===I;})[0];"
                + "if(!r) return ''; var a=[].filter.call(r.children[14].querySelectorAll('a'),function(x){return (x.getAttribute('title')||'').trim()===T;})[0];"
                + "return a?(a.getAttribute('href')+'|'+(a.getAttribute('target')||'')):'';", requestId, title);
    }

    // ✅ data-* attributes of the Void Invoice icon of a request
    public Map<String, String> getVoidButtonData(String requestId) {
        return map(js().executeScript("var I=arguments[0]; var b=document.querySelector('.void-invoice-btn[data-request-id=\"'+I+'\"]'); if(!b) return {};"
                + "var o={}; [].forEach.call(b.attributes,function(a){ if(a.name.indexOf('data-')===0) o[a.name.substring(5)]=a.value;}); return o;", requestId));
    }

    public int getTotalRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows().count();")).intValue();
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getPageRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({page:'current'}).count();")).intValue();
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public String getInfoText() {
        return str("var i=document.querySelector('#datatable-task_info');return i?i.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';");
    }

    public String getEmptyTableMessage() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';");
    }

    public boolean isSearchBoxVisible() { return isShown(By.cssSelector("#datatable-task_filter input")); }

    public boolean isPaginationVisible() { return isShown(By.cssSelector("#datatable-task_paginate")); }

    public String getDownloadButtonText() {
        return str("var b=document.querySelector('#datatable-task_wrapper .dt-buttons .dt-button');return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public String getDownloadFileName() {
        return str("var b=jQuery('" + TABLE + "').DataTable().settings()[0].oInit.buttons||[]; if(!b.length) return ''; var x=b[0]; var f=x.filename||x.title||'';"
                + "return typeof f==='function'?f():f;");
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#datatable-task_wrapper .dt-buttons .dt-button').click();");
        System.out.println("▶ Download Report clicked");
        Thread.sleep(1500);
    }

    public void searchTable(String keyword) throws InterruptedException {
        js().executeScript("jQuery('" + TABLE + "').DataTable().search(arguments[0]).draw();"
                + "var i=document.querySelector('#datatable-task_filter input'); if(i) i.value=arguments[0];", keyword);
        Thread.sleep(1000);
    }

    // ✅ Shows every row on one page (rows found by request id must be in the DOM for clicks)
    public void showAllRows() throws InterruptedException {
        js().executeScript("jQuery('" + TABLE + "').DataTable().page.len(-1).draw();");
        Thread.sleep(800);
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('#datatable-task_paginate .paginate_button.next').click();");
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
        return str("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\s+/g,' ').trim()===A && h.offsetWidth>0;})[0];"
                + "return h?h.className:'';", header);
    }

    // ✅ Column values in the drawn order, as DataTables sorts them (tags stripped, lower case)
    public List<String> getSortKeys(int domIndex) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied',order:'applied'}).data().toArray()"
                + ".map(function(v){return String(v).replace(/<.*?>/g,'').toLowerCase();});", domIndex));
    }

    // ════════════════ Messages after a post-back ════════════════
    // ✅ Visible flash / alert text on the page (outside the popup)
    public String getPageMessage() {
        return str("var a=[].filter.call(document.querySelectorAll('.flash-msg .alert, .alert-success, .alert-danger, .alert-warning, .alert-info'),"
                + "function(x){return x.offsetWidth>0 && !x.closest('#popdown-dialog') && !x.closest('#api-debug-panel');});"
                + "return a.map(function(x){return x.textContent.replace(/\\s+/g,' ').replace(/^×\\s*/,'').trim();}).filter(function(t){return t;}).join(' | ');");
    }

    // ════════════════ Confirm capture ════════════════
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__confirms=[]; window.__alerts=[]; window.confirm=function(m){ window.__confirms.push(m); return " + acceptConfirm + "; };"
                + "window.alert=function(m){ window.__alerts.push(m); };");
    }

    public List<String> getCapturedConfirms() {
        return strings(js().executeScript("return window.__confirms || [];"));
    }

    public List<String> getCapturedAlerts() {
        return strings(js().executeScript("return window.__alerts || [];"));
    }

    // ════════════════ Popdown ════════════════
    private void clickLink(String css) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var a=document.querySelector(arguments[0]); if(!a) return false; a.scrollIntoView({block:'center'}); a.click(); return true;", css);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Link not found: " + css);
        Thread.sleep(1500);
    }

    public String waitForPopup(String marker) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            String t = getPopupText();
            if (t.contains(marker)) return t;
            Thread.sleep(500);
        }
        return getPopupText();
    }

    public String getPopupText() {
        return str("var p=document.querySelector('" + POP + "'); return p && p.offsetWidth>0 ? p.innerText.replace(/\\s+/g,' ').trim() : '';");
    }

    public boolean isPopupOpen() {
        return isShown(By.cssSelector(POP + " #frm_update"));
    }

    public void closePopup() throws InterruptedException {
        js().executeScript("var b=document.querySelector('" + POP + " .close-popdown'); if(b) b.click();"
                + "var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';"
                + "var p=document.querySelector('" + POP + "'); if(p) p.style.display='none';");
        Thread.sleep(700);
    }

    // ✅ Clicks the red Close button of the request popup (a real click; returns whether the popup closed)
    public boolean clickPopupCloseButton() throws InterruptedException {
        js().executeScript("var b=document.querySelector('" + POP + " #frm_update .close-popdown'); if(b) b.click();");
        Thread.sleep(1000);
        return !isPopupOpen();
    }

    private void waitForForm() throws InterruptedException {
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #frm_update #btn_submit")).isEmpty());
        wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                "var f=jQuery('" + POP + " #frm_update'); return f.length>0 && !!f.data('validator');")));
        Thread.sleep(500);
    }

    public void openAddPopup() throws InterruptedException {
        clickLink("a[href^='add_backend_update_requests?pop=yes&action=add']");
        System.out.println("▶ Add Backend Requests clicked");
        waitForPopup("Add Backend Requests");
        waitForForm();
    }

    public void openUpdatePopup(String requestId) throws InterruptedException {
        String b64 = java.util.Base64.getEncoder().encodeToString(requestId.getBytes());
        clickLink("a[href^='add_backend_update_requests?pop=yes&action=update&id=" + b64 + "&']");
        System.out.println("▶ Update Backend Requests clicked for request " + requestId);
        waitForPopup("Update Backend Requests");
        waitForForm();
    }

    public String getPopupTitle() {
        return str("var p=document.querySelector('" + POP + "'); if(!p) return ''; var h=[].filter.call(p.querySelectorAll('h1,h2,h3,h4,h5,h6,legend,.panel-title'),function(x){return x.offsetWidth>0 && x.textContent.trim();})[0];"
                + "return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    // ✅ Visible labels of the request form
    public List<String> getFormLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + POP + " #frm_update label'),function(l){return l.offsetWidth>0 && !l.classList.contains('text-danger') && l.textContent.trim();})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    public String getFieldValue(String id) {
        return str("var e=document.querySelector('" + POP + " #frm_update #'+arguments[0]); if(!e) return '';"
                + "return e.tagName==='SELECT' ? (e.selectedIndex<0?'':e.options[e.selectedIndex].text.trim()) : e.value;", id);
    }

    public Map<String, String> getFieldState(String id) {
        return map(js().executeScript("var e=document.querySelector('" + POP + " #frm_update #'+arguments[0]); if(!e) return {};"
                + "return {readonly:String(e.readOnly), disabled:String(e.disabled), shown:String(e.offsetWidth>0)};", id));
    }

    public List<String> getFieldOptions(String id) {
        return strings(js().executeScript("return [].map.call(document.querySelector('" + POP + " #frm_update #'+arguments[0]).options,function(o){return o.text.trim();});", id));
    }

    public boolean isFieldShown(String id) {
        return isShown(By.cssSelector(POP + " #frm_update #" + id));
    }

    public String getPriority() {
        return str("var r=document.querySelector('" + POP + " #frm_update input[name=request_priority]:checked'); return r?r.value:'';");
    }

    public void setPriority(String value) {
        js().executeScript("var r=document.querySelector('" + POP + " #frm_update input[name=request_priority][value=\"'+arguments[0]+'\"]'); r.checked=true; jQuery(r).trigger('change');"
                + "if(jQuery.uniform) try{jQuery.uniform.update(r);}catch(e){}", value);
        System.out.println("▶ Priority = " + value);
    }

    public void setField(String id, String text) {
        WebElement e = driver.findElement(By.cssSelector(POP + " #frm_update #" + id));
        e.clear();
        if (!text.isEmpty()) e.sendKeys(text);
    }

    // ✅ Selects by option text and fires change (Request Type shows the void fields; Invoice Type re-fetches the amount)
    public void selectField(String id, String optionText) {
        Object ok = js().executeScript("var s=document.querySelector('" + POP + " #frm_update #'+arguments[0]), T=arguments[1];"
                + "var o=[].filter.call(s.options,function(x){return x.text.trim()===T;})[0]; if(!o) return false;"
                + "jQuery(s).val(o.value).trigger('change'); return true;", id, optionText);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No option '" + optionText + "' in #" + id);
        System.out.println("▶ " + id + " = " + optionText);
    }

    // ✅ Due Date through pickadate's own API (yyyy-MM-dd)
    public void setDueDate(String isoDate) {
        js().executeScript("var p=jQuery('" + POP + " #frm_update #request_complete_before').pickadate('picker'); var d=arguments[0].split('-');"
                + "p.set('select',[+d[0],+d[1]-1,+d[2]]);", isoDate);
        System.out.println("▶ Due Date = " + isoDate);
    }

    // ✅ Due Date picker range [min, max] as yyyy-MM-dd
    public String[] getDueDateRange() {
        return str("var p=jQuery('" + POP + " #frm_update #request_complete_before').pickadate('picker');"
                + "function f(o){var d=o.obj; return d.getFullYear()+'-'+('0'+(d.getMonth()+1)).slice(-2)+'-'+('0'+d.getDate()).slice(-2);}"
                + "return f(p.get('min'))+'|'+f(p.get('max'));").split("\\|");
    }

    // ✅ Writes a raw value into the readonly due date (to test the validator's range rules)
    public void forceDueDateText(String isoDate) {
        js().executeScript("jQuery('" + POP + " #frm_update #request_complete_before').val(arguments[0]);", isoDate);
    }

    public void tickAddChildHistory(boolean on) {
        js().executeScript("var c=document.querySelector('" + POP + " #frm_update #add_child_history'); if(c.checked!==arguments[0]) c.click();", on);
    }

    // ✅ Leaves the invoice field (blur) and waits for the invoice lookup AJAX
    public void blurInvoiceNumberAndWait() throws InterruptedException {
        js().executeScript("jQuery('" + POP + " #frm_update #invoice_number').trigger('blur');");
        waitAjax();
    }

    public void waitAjax() throws InterruptedException {
        Thread.sleep(700);
        for (int i = 0; i < 40; i++) {
            if (!Boolean.TRUE.equals(js().executeScript("return (jQuery.active||0)>0;"))) break;
            Thread.sleep(500);
        }
        Thread.sleep(300);
    }

    public boolean invoiceAmountErrorExists() {
        return Boolean.TRUE.equals(js().executeScript("return !!document.querySelector('" + POP + " #invoice_amount-error');"));
    }

    public String getInvoiceAmountError() {
        return str("var e=document.querySelector('" + POP + " #invoice_amount-error'); return e && e.offsetWidth>0 ? e.textContent.trim() : '';");
    }

    // ✅ Submit: jQuery-validate runs on submit; a valid form posts back and the page reloads
    public void clickSubmit() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + POP + " #frm_update #btn_submit').click();");
        System.out.println("▶ Submit clicked");
        Thread.sleep(1500);
    }

    // ✅ Visible validation messages of the form, in DOM order
    public List<String> getValidationErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + POP + " #frm_update label.text-danger, " + POP + " #frm_update .error'),"
                + "function(l){return l.offsetWidth>0 && l.textContent.trim();}).map(function(l){return l.textContent.trim();})"
                + ".filter(function(t,i,a){return a.indexOf(t)===i;});"));
    }

    // ✅ Waits for the post-back to reload the list; returns the page message
    public String waitForPostBack() throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            Thread.sleep(1000);
            try {
                if (!isPopupOpen() && Boolean.TRUE.equals(js().executeScript("return document.readyState==='complete' && !!window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "');")))
                    break;
            } catch (Exception ignored) {
            }
        }
        isPageLoaded();
        String m = getPageMessage();
        System.out.println("   page message: " + m);
        return m;
    }

    // ✅ Comment History list of the Update popup
    public String getCommentHistoryText() {
        return str("var p=document.querySelector('" + POP + "'); if(!p) return ''; var t=p.innerText; var i=t.indexOf('Comment History'); return i<0?'':t.substring(i).replace(/\\s+/g,' ').trim();");
    }

    // ════════════════ Void Invoice icon ════════════════
    public void clickVoidInvoice(String requestId) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var b=document.querySelector('.void-invoice-btn[data-request-id=\"'+arguments[0]+'\"]'); if(!b) return false; b.click(); return true;", requestId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalStateException("No Void Invoice icon for request " + requestId);
        System.out.println("▶ Void Invoice clicked for request " + requestId);
    }

    // ✅ #backend_update_mesg text (shown before the 2 s reload)
    public String waitForVoidMessage() throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            String m = str("var e=document.getElementById('backend_update_mesg'); return e && e.offsetWidth>0 ? e.textContent.trim() : '';");
            if (!m.isEmpty()) return m;
            Thread.sleep(250);
        }
        return "";
    }

    // ✅ Read-only invoice lookup the Add popup uses → raw response
    public String lookupInvoice(String childId, String invoiceNumber, String invoiceType) {
        return str("var x=new XMLHttpRequest(); x.open('POST', API_SERVER_URL+'/api/backenderpupdates/getBillingInvoicesFilter', false);"
                + "x.setRequestHeader('Content-Type','application/x-www-form-urlencoded');"
                + "x.send('action=get_invoice_details&invoice_number='+encodeURIComponent(arguments[1])+'&child_id='+encodeURIComponent(arguments[0])+'&invoice_type='+encodeURIComponent(arguments[2]));"
                + "return x.responseText;", childId, invoiceNumber, invoiceType);
    }

    // ════════════════ Billing Setup Update popup ════════════════
    public void openBillingSetup(String requestId) throws InterruptedException {
        String href = getActionHref(requestId, "Billing Setup Update").split("\\|")[0];
        clickLink("a[href='" + href + "']");
        System.out.println("▶ Billing Setup Update clicked (" + href + ")");
        waitForPopup("Billing Setup Update");
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #child_pause_flag")).isEmpty());
        Thread.sleep(1500);
    }

    public String getBillingValue(String id) {
        return str("var e=document.querySelector('" + POP + " #'+arguments[0]); if(!e) return '';"
                + "return e.tagName==='SELECT' ? (e.selectedIndex<0?'':e.options[e.selectedIndex].text.trim()) : e.value;", id);
    }

    public String getBillingSectionText(String ctype) {
        return str("var d=document.querySelector('" + POP + " .div-'+arguments[0]); return d?d.innerText.replace(/\\s+/g,' ').trim():'';", ctype);
    }

    // ✅ Clicks a section's Update button (onclick validateUpdateDetails(this,'<ctype>',…)); waits for the flash message
    public String clickBillingUpdate(String ctype) throws InterruptedException {
        hideApiDebugPanel();
        wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript("return !!window.__cfRLUnblockHandlers || !document.querySelector('script[src*=rocket-loader]');")));
        Object ok = js().executeScript("var C=arguments[0]; var b=[].filter.call(document.querySelectorAll('" + POP + " button[onclick]'),function(x){return x.getAttribute('onclick').indexOf(\"'\"+C+\"'\")>0;})[0];"
                + "if(!b) return false; b.click(); return true;", ctype);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalStateException("No Update button for " + ctype);
        System.out.println("▶ Billing Setup Update: " + ctype);
        waitAjax();
        for (int i = 0; i < 20; i++) {
            String m = str("var d=document.querySelector('" + POP + " .div-'+arguments[0]); if(!d) return '';"
                    + "var a=[].filter.call(d.parentNode.querySelectorAll('.alert'),function(x){return x.offsetWidth>0;}); return a.map(function(x){return x.textContent.replace(/\\s+/g,' ').replace(/^×\\s*/,'').trim();}).join(' | ');", ctype);
            if (!m.isEmpty()) return m;
            Thread.sleep(500);
        }
        return "";
    }

    // ✅ Child Updates History popup text (pop_history_view, read with a GET; includes the "Backend Requests" tab)
    public String getChildHistoryText(String childId) {
        return str("var x=new XMLHttpRequest(); x.open('GET','pop_history_view?pop=yes&child_id='+arguments[0],false); x.send();"
                + "var d=document.createElement('div'); d.innerHTML=x.responseText; [].forEach.call(d.querySelectorAll('style,script'),function(e){e.remove();});"
                + "return d.textContent.replace(/\\s+/g,' ').trim();", childId);
    }
}
