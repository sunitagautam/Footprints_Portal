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
// REFUNDS ISSUED (Support → Financial Updates → Refunds Issued, /refund_list)
// Selectors confirmed live 2026-11-20 (as Rakesh) — see CLAUDE.md "Requirements — Support → Financial Updates".
//
// Filter form #dateform (posts back): Issued from/to #date_from / #date_to and Transfer from/to #transfer_date_from /
// #transfer_date_to (pickadate, "20 October, 2026"; default Issued = today − 1 month → today), Type #refund_type,
// Status multiselect #refund_status name refund_status[] (default Advice, Verified, Bank Details Awaited, Transfer Initiated),
// Admission Type #admission_type, Child ID #child_id (keyup > 3 chars: all statuses, dates cleared), Ticket ID #ticket_id,
// Submit input[name=filter_list], Reset #btn_reset. The list API (listRefundList) is capped at 500 rows.
// Table table.datatable-refunds-listing (client side, 10 per page, CSV "Download Report" Refund_Issued_Report_d_m_yyyy).
// DOM columns: 0 Child Name | 1 Type(raw, hidden) | 2 Type | 3 Description | 4 Amount | 5 Issued Date | 6 Verification Date |
// 7 Bank Details Received | 8 Transfer Date | 9 Closure/ Refunded Date | 10 Status | 11–16 bank details (hidden) |
// 17 Refund Id (hidden) | 18 Action. Row class tr_<refund id>.
// Add New Refund popup (add_new_refund) form #issued_credit_refund → assets/js/refund_proccess.js ValidateRefundForm():
// jQuery validate → keyword guard (kit / book) → confirm "Are you sure, You want to issue refund?" → add_new_refund API →
// existing-refund table ("I have verified existing refund and continue with new refund") → confirm again → "Detail Updated
// successfully" → reload. Verify / Verify Transfer popup (refund_details) → refund_details.js: confirm "Are you sure you want
// to mark as <status> this refund?" → refund_proccess_update → "Detail Updated successfully". Void / Return: markRefundVoid()
// confirm "Are you sure want to void this refund ?" → refund_void → flash in .call-update-message.
// ═══════════════════════════════════════════════════════════════
public class RefundsIssuedPage {

    WebDriver driver;
    WebDriverWait wait;

    public static final String TABLE = "table.datatable-refunds-listing";
    private static final String POP = "#popdown-dialog";
    public static final List<String> KEYS = Arrays.asList("Child Name", "Type Raw", "Type", "Description", "Amount", "Issued Date",
            "Verification Date", "Bank Details Received", "Transfer Date", "Closure/ Refunded Date", "Status", "Account Bank Name",
            "Account Number", "Account IFSC Code", "Account Holder Name", "Account Holder Email", "QuickBook Code", "Refund Id");
    public static final String[] ALL_STATUSES = {"Advice", "Verified", "Bank Details Awaited", "Transfer Pending", "Transfer Initiated", "Refunded", "Return", "Voided"};

    public RefundsIssuedPage(WebDriver driver) {
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
        return str("var s=(window.menuData||[]).filter(function(x){return x.url==='refund_list';})[0]; return s ? s.topMenu+' > '+s.subMenu+' > '+s.title : '';");
    }

    public String getHeading() {
        return str("var h=[].filter.call(document.querySelectorAll('h5'),function(h){return h.offsetWidth>0 && /Refund/.test(h.textContent);})[0]; return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('#dateform label'),function(l){return l.offsetWidth>0 && l.textContent.trim() && !l.closest('.multiselect-container');})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    // ✅ Top buttons: Add New Refund / Upload Transfer Initiated / Upload Refunded Data
    public List<String> getTopButtons() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('a.popdown_big.btn'),function(a){return a.offsetWidth>0;}).map(function(a){return a.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    // ════════════════ Filters ════════════════
    public String getValue(String id) {
        return str("var e=document.querySelector('#dateform #'+arguments[0]); if(!e) return ''; return e.tagName==='SELECT'?(e.selectedIndex<0?'':e.options[e.selectedIndex].text.trim()):e.value;", id);
    }

    public List<String> getOptions(String id) {
        return strings(js().executeScript("return [].map.call(document.querySelector('#dateform #'+arguments[0]).options,function(o){return o.text.trim();});", id));
    }

    public List<String> getOptionValues(String id) {
        return strings(js().executeScript("return [].map.call(document.querySelector('#dateform #'+arguments[0]).options,function(o){return o.value;});", id));
    }

    public void selectByText(String id, String text) {
        Object ok = js().executeScript("var s=document.querySelector('#dateform #'+arguments[0]),T=arguments[1]; var o=[].filter.call(s.options,function(x){return x.text.trim()===T;})[0];"
                + "if(!o) return false; jQuery(s).val(o.value).trigger('change'); return true;", id, text);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No option '" + text + "' in #" + id);
        System.out.println("▶ " + id + " = " + text);
    }

    // ✅ pickadate filter date (yyyy-MM-dd); empty clears it
    public void setDate(String id, String iso) {
        js().executeScript("var p=jQuery('#dateform #'+arguments[0]).pickadate('picker'); if(!arguments[1]){p.clear(); return;} var d=arguments[1].split('-'); p.set('select',[+d[0],+d[1]-1,+d[2]]);", id, iso);
        System.out.println("▶ " + id + " = " + iso);
    }

    public List<String> getStatusSelected() {
        return strings(js().executeScript("return jQuery('#refund_status').val()||[];"));
    }

    public List<String> getStatusOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('#refund_status option'),function(o){return o.text.trim();});"));
    }

    public String getStatusButtonText() {
        return str("var b=document.querySelector('#dateform button.multiselect'); return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public void setStatuses(String... statuses) {
        js().executeScript("var s=jQuery('#refund_status'); s.multiselect('deselectAll',false); s.val(arguments[0]); s.multiselect('refresh');", Arrays.asList(statuses));
        System.out.println("▶ Status = " + Arrays.toString(statuses));
    }

    // ✅ Child ID typed with real keys (its keyup selects all statuses and clears the dates when > 3 characters)
    public void typeChildId(String childId) {
        WebElement e = driver.findElement(By.cssSelector("#dateform #child_id"));
        e.clear();
        e.sendKeys(childId);
    }

    public void typeTicketId(String ticket) {
        WebElement e = driver.findElement(By.cssSelector("#dateform #ticket_id"));
        e.clear();
        e.sendKeys(ticket);
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
    public List<String> getAllHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }

    public List<String> getDefinedHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return !/\\bnever\\b/.test(h.className);})"
                + ".map(function(h){return h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }

    // ✅ Rows (search applied): KEYS + childId (Account Statement link) + actions (titles) + rowClass + childHref/target
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRows() {
        Object o = js().executeScript("var K=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray().map(function(r){ var out={};"
                + " for(var i=0;i<K.length;i++) out[K[i]]=r.children[i]?r.children[i].textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';"
                + " var a=r.children[0].querySelector('a'); var m=a?(a.getAttribute('href')||'').match(/child_id=(\\d+)/):null; out.childId=m?m[1]:''; out.childHref=a?a.getAttribute('href')+'|'+(a.getAttribute('target')||''):'';"
                + " var mm=(r.className||'').match(/tr_(\\d+)/); out['Refund Id']=mm?mm[1]:out['Refund Id'];"
                + " var c=r.lastElementChild; out.actions=c?[].map.call(c.querySelectorAll('a'),function(x){return (x.getAttribute('title')||'').trim()+((x.className||'').indexOf('disabled')>=0?'(disabled)':'');}).join(', '):'';"
                + " out.actionText=c?[].map.call(c.querySelectorAll('a'),function(x){return x.textContent.replace(/\\s+/g,' ').trim();}).filter(function(t){return t;}).join(', '):'';"
                + " out.rowClass=r.className; out.popover=(r.children[2].querySelector('[data-content]')||{getAttribute:function(){return '';}}).getAttribute('data-content').replace(/<[^>]+>/g,' ').replace(/\\s+/g,' ').trim(); return out;});", KEYS);
        List<Map<String, String>> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(map(x));
        return out;
    }

    public Map<String, String> getRowByRefundId(String refundId) {
        for (Map<String, String> r : getRows()) if (refundId.equals(r.get("Refund Id"))) return r;
        return null;
    }

    // ✅ href|target of an action by title, for a refund
    public String getActionHref(String refundId, String title) {
        return str("var R=arguments[0],T=arguments[1]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){return r.classList.contains('tr_'+R);})[0];"
                + "if(!r) return ''; var a=[].filter.call(r.lastElementChild.querySelectorAll('a'),function(x){return (x.getAttribute('title')||'').trim()===T;})[0];"
                + "return a?(a.getAttribute('href')+'|'+(a.getAttribute('target')||'')):'';", refundId, title);
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getTotalRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows().count();")).intValue();
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

    public String getDownloadFileName() {
        return str("var b=jQuery('" + TABLE + "').DataTable().settings()[0].oInit.buttons||[]; if(!b.length) return ''; var x=b[0]; var f=x.filename||x.title||''; return typeof f==='function'?f():f;");
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

    public void showAllRows() throws InterruptedException {
        js().executeScript("jQuery('" + TABLE + "').DataTable().page.len(-1).draw();");
        Thread.sleep(800);
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .paginate_button.previous').click();");
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

    // ✅ Values of a column in the drawn order, using DataTables' own sort data (orthogonal 'sort')
    public List<String> getSortData(int col) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable(), i=arguments[0]; return dt.rows({search:'applied',order:'applied'}).indexes().toArray().map(function(r){return String(dt.cell(r,i).render('sort')).replace(/<.*?>/g,'');});", col));
    }

    // ════════════════ Confirm / alert capture ════════════════
    public void startCapture(boolean accept) {
        js().executeScript("window.__confirms=[]; window.__alerts=[]; window.confirm=function(m){window.__confirms.push(m); return " + accept + ";}; window.alert=function(m){window.__alerts.push(m);};");
    }

    public List<String> getCapturedConfirms() { return strings(js().executeScript("return window.__confirms||[];")); }

    // ✅ Main-page flash (.call-update-message / any visible alert outside popups)
    public String getPageMessage() {
        return str("var a=[].filter.call(document.querySelectorAll('.flash-msg .alert, .call-update-message .alert'),function(x){return x.offsetWidth>0 && !x.closest('#popdown-dialog') && !x.closest('#api-debug-panel');});"
                + "return a.map(function(x){return x.textContent.replace(/\\s+/g,' ').replace(/^×\\s*/,'').trim();}).join(' | ');");
    }

    public String waitForPageMessage() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            String m = getPageMessage();
            if (!m.isEmpty()) return m;
            Thread.sleep(250);
        }
        return "";
    }

    // ════════════════ Row actions ════════════════
    private void clickRowAction(String refundId, String title) throws InterruptedException {
        hideApiDebugPanel();
        Object ok2 = js().executeScript("var R=arguments[0],T=arguments[1]; var r=document.querySelector('" + TABLE + " tr.tr_'+R); if(!r) return false;"
                + "var a=[].filter.call(r.querySelectorAll('a'),function(x){return (x.getAttribute('title')||'').trim()===T;})[0]; if(!a) return false; a.scrollIntoView({block:'center'}); a.click(); return true;", refundId, title);
        if (!Boolean.TRUE.equals(ok2)) throw new IllegalStateException("No '" + title + "' action on refund " + refundId);
        System.out.println("▶ " + title + " clicked (refund " + refundId + ")");
        Thread.sleep(1200);
    }

    public void clickVoid(String refundId) throws InterruptedException { clickRowAction(refundId, "Void Refund"); }

    public void clickReturn(String refundId) throws InterruptedException { clickRowAction(refundId, "Return Refund"); }

    public void clickSendBankEmail(String refundId) throws InterruptedException { clickRowAction(refundId, "Send Bank Details Email"); }

    public void clickIncorrectBank(String refundId) throws InterruptedException { clickRowAction(refundId, "Incorrect Bank Details"); }

    public void openDetails(String refundId, String title) throws InterruptedException {
        clickRowAction(refundId, title);
        waitForPopup("Update Refund Details");
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #issued_credit_refund #refund_process_btn")).isEmpty());
        Thread.sleep(1000);
    }

    // ════════════════ Popups ════════════════
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

    public void closePopup() throws InterruptedException {
        js().executeScript("var b=document.querySelector('" + POP + " .close-popdown'); if(b) b.click();"
                + "var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none'; var p=document.querySelector('" + POP + "'); if(p) p.style.display='none';");
        Thread.sleep(700);
    }

    public void openTopPopup(String title, String marker) throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("var T=arguments[0]; var a=[].filter.call(document.querySelectorAll('a.popdown_big.btn'),function(a){return (a.getAttribute('title')||'')===T;})[0]; a.click();", title);
        System.out.println("▶ " + title + " clicked");
        waitForPopup(marker);
        Thread.sleep(1200);
    }

    public void openAddRefund() throws InterruptedException {
        openTopPopup("Add New Refund", "Add New Issue Refund");
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #issued_credit_refund #refund_add_btn")).isEmpty());
        wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript("return !!jQuery('" + POP + " #issued_credit_refund').data('validator') || typeof ValidateRefundForm==='function';")));
        Thread.sleep(800);
    }

    public List<String> getPopupLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + POP + " #issued_credit_refund label'),function(l){return l.offsetWidth>0 && !l.classList.contains('text-danger') && l.textContent.trim();})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    public List<String> getPopupOptions(String name) {
        return strings(js().executeScript("var s=document.querySelector('" + POP + " #issued_credit_refund select[name='+arguments[0]+']'); return s?[].map.call(s.options,function(o){return o.text.trim()+'='+o.value;}):[];", name));
    }

    public String getPopupField(String css) {
        return str("var e=document.querySelector('" + POP + " #issued_credit_refund '+arguments[0]); if(!e) return ''; return e.tagName==='SELECT'?(e.selectedIndex<0?'':e.options[e.selectedIndex].text.trim()):e.value;", css);
    }

    public Map<String, String> getPopupFieldState(String css) {
        return map(js().executeScript("var e=document.querySelector('" + POP + " #issued_credit_refund '+arguments[0]); if(!e) return {}; return {readonly:String(e.readOnly), disabled:String(e.disabled), shown:String(e.offsetWidth>0)};", css));
    }

    public void setPopupField(String css, String value) {
        WebElement e = driver.findElement(By.cssSelector(POP + " #issued_credit_refund " + css));
        e.clear();
        if (!value.isEmpty()) e.sendKeys(value);
    }

    // ✅ Child picker: type the id into the visible number input and click "Search Child By ID"; waits for the name
    public void searchChild(String childId) throws InterruptedException {
        WebElement in = driver.findElement(By.cssSelector(POP + " #issued_credit_refund input[name=Child_id_fetch]"));
        in.clear();
        in.sendKeys(childId);
        js().executeScript("var b=[].filter.call(document.querySelectorAll('" + POP + " #issued_credit_refund [data-input-id]'),function(x){return true;})[0]; if(b) b.click();");
        System.out.println("▶ Search Child By ID " + childId);
        waitAjax();
        for (int i = 0; i < 20 && getChildName().isEmpty(); i++) Thread.sleep(500);
    }

    public String getChildName() {
        return str("var e=document.querySelector('" + POP + " #issued_credit_refund input[id^=ChildName_]'); return e?e.value:'';");
    }

    public String getChildCenter() {
        return str("var e=document.querySelector('" + POP + " #issued_credit_refund input[id^=ChildCenter_]'); return e?e.value:'';");
    }

    public String getPickedChildId() {
        return str("var e=document.querySelector('" + POP + " #issued_credit_refund input[name=refund_child_id]'); return e?e.value:'';");
    }

    // ✅ Refund Type by value (e.g. "Refund - Registration Fee"); fires the change that loads the invoice references
    public void selectRefundType(String value) throws InterruptedException {
        js().executeScript("jQuery('" + POP + " #issued_credit_refund select[name=refund_type]').val(arguments[0]).trigger('change');", value);
        System.out.println("▶ Refund Type = " + value);
        waitAjax();
    }

    public void selectInvoice(String invoiceValue) {
        js().executeScript("jQuery('" + POP + " #issued_credit_refund #invoice_list').val(arguments[0]).trigger('change');", invoiceValue);
        System.out.println("▶ Invoice = " + invoiceValue);
    }

    public void waitAjax() throws InterruptedException {
        Thread.sleep(700);
        for (int i = 0; i < 40; i++) {
            if (!Boolean.TRUE.equals(js().executeScript("return (jQuery.active||0)>0;"))) break;
            Thread.sleep(500);
        }
        Thread.sleep(300);
    }

    public void clickAddSubmit() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + POP + " #issued_credit_refund #refund_add_btn').click();");
        System.out.println("▶ Add refund Submit clicked");
        waitAjax();
    }

    public String getAddButtonText() {
        return str("var b=document.querySelector('" + POP + " #refund_add_btn'); return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public void clickProcessButton() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + POP + " #issued_credit_refund #refund_process_btn').click();");
        System.out.println("▶ " + getProcessButtonText() + " clicked");
        waitAjax();
    }

    public String getProcessButtonText() {
        return str("var b=document.querySelector('" + POP + " #refund_process_btn'); return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public List<String> getPopupErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + POP + " label.text-danger, " + POP + " label.error'),function(l){return l.offsetWidth>0 && l.textContent.trim();})"
                + ".map(function(l){return l.textContent.trim();}).filter(function(t,i,a){return a.indexOf(t)===i;});"));
    }

    // ✅ Message in the popup (.form-update-message)
    public String getPopupMessage() {
        return str("var p=document.querySelector('" + POP + "'); if(!p) return ''; var a=[].filter.call(p.querySelectorAll('.alert'),function(x){return x.offsetWidth>0;});"
                + "return a.map(function(x){return x.textContent.replace(/\\s+/g,' ').replace(/^×\\s*/,'').trim();}).filter(function(t){return t;}).join(' | ');");
    }

    public String waitForPopupMessage() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            String m = getPopupMessage();
            if (!m.isEmpty()) return m;
            Thread.sleep(250);
        }
        return "";
    }

    public String getExistingRefundTable() {
        return str("var d=document.querySelector('" + POP + " .div-container-existing-data'); return d?d.innerText.replace(/\\s+/g,' ').trim():'';");
    }

    // ════════════════ Incorrect Bank Details modal ════════════════
    public boolean isIncorrectBankModalShown() { return isShown(By.cssSelector("#modal_frm_incorrect_bank_details #bank_comments")); }

    public String getIncorrectBankModalText() {
        return str("var m=document.querySelector('#modal_frm_incorrect_bank_details'); return m?m.innerText.replace(/\\s+/g,' ').trim():'';");
    }

    public void submitIncorrectBank() throws InterruptedException {
        js().executeScript("document.querySelector('#modal_frm_incorrect_bank_details .btn-model-cancel').click();");
        Thread.sleep(800);
    }

    public List<String> getIncorrectBankErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('#modal_frm_incorrect_bank_details [class*=alert-]'),function(e){return e.offsetWidth>0 && e.textContent.trim();}).map(function(e){return e.textContent.trim();});"));
    }

    public void closeIncorrectBankModal() throws InterruptedException {
        js().executeScript("jQuery('#modal_frm_incorrect_bank_details').modal('hide');");
        Thread.sleep(800);
    }
}
