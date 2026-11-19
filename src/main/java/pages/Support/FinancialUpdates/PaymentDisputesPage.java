package pages.Support.FinancialUpdates;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// PAYMENT DISPUTES (Support → Financial Updates → Payment Disputes, /payment_disputes)
// Selectors confirmed live 2026-11-19 (as Jaydeep Kar — Rakesh has no Manage_Payment_Dispute) — see CLAUDE.md
// "Requirements — Support → Financial Updates".
//
// Filter form #frm_user_list (posts back): Center select2 #center_id ("" = All), Dispute Status #status
// [Completed, Pending*], Child Status #child_status [1=Active*, 0=Attrition], Admission Type #admission_type
// [""*, Corporate, Regular], Search #filter_submit_button.
// "Add New Disputes" = a[href^='mark_disputes?pop=yes&func=addDisputes'] (popdown) → form #mark_dispute_frm:
// #child_id + Fetch Child Details, #invoice_payment_dispute [Invoice, Offline Payment, Online Payment, Policy],
// #note_text, #add_dispute_button; confirm "Are you sure want to add this in Dispute ?" → api/customerfinancial/markDisputes.
// Table #datatable-pay-list (client side, 20 per page, Download Data CSV): Child Id | Child Name | Centre |
// Centre Directress | Last Call | Comment | Added On | Added By | Attrition Date | Actions. Pending rows are coloured
// (style color:red / orange). Actions: Call (modal #call_parents_<id>), History (pop_history_view), Update Call Log
// (comment_list…func=update_call_log), Reslove Dispute (comment_list…func=resolve_dispute); Completed rows: comment_list
// without func (call history). Call log / resolve form #user_note (#note_text, #add_notes_button), confirm
// "Are you sure want to add this Call Log ?" → api/customerfinancial/insertComments.
// ═══════════════════════════════════════════════════════════════
public class PaymentDisputesPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String TABLE = "#datatable-pay-list";
    private static final String POP = "#popdown-dialog";

    public PaymentDisputesPage(WebDriver driver) {
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

    public static String b64(String childId) {
        return Base64.getEncoder().encodeToString(childId.getBytes());
    }

    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(d -> !d.findElements(By.cssSelector("#frm_user_list #filter_submit_button")).isEmpty()
                    && Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMenuPath() {
        return str("var s=(window.menuData||[]).filter(function(x){return x.url==='payment_disputes';})[0];"
                + "return s ? s.topMenu+' > '+s.subMenu+' > '+s.title : '';");
    }

    public String getHeading() {
        return str("var h=[].filter.call(document.querySelectorAll('h5'),function(h){return h.offsetWidth>0 && /Payment Disputes/.test(h.textContent);})[0];"
                + "return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('#frm_user_list label'),function(l){return l.offsetWidth>0 && l.textContent.trim();})"
                + ".map(function(l){return l.textContent.replace(/\\s+/g,' ').trim();});"));
    }

    // ════════════════ Filters ════════════════
    // ✅ Selected option text of a filter select (center_id, status, child_status, admission_type)
    public String getFilterText(String id) {
        return str("var s=document.getElementById(arguments[0]); return s.options[s.selectedIndex].text.trim();", id);
    }

    public List<String> getFilterOptions(String id) {
        return strings(js().executeScript("return [].map.call(document.getElementById(arguments[0]).options,function(o){return o.text.trim();});", id));
    }

    // ✅ Sets a filter by option text (select2 for the centre is kept in step via trigger('change'))
    public void setFilter(String id, String optionText) {
        Object ok = js().executeScript("var s=document.getElementById(arguments[0]), T=arguments[1];"
                + "var o=[].filter.call(s.options,function(x){return x.text.trim()===T;})[0]; if(!o) return false;"
                + "jQuery(s).val(o.value).trigger('change'); return true;", id, optionText);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No option '" + optionText + "' in #" + id);
        System.out.println("▶ Filter " + id + " = " + optionText);
    }

    public void clickSearch() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.getElementById('filter_submit_button').click();");
        System.out.println("▶ Search clicked");
        Thread.sleep(2500);
        isPageLoaded();
    }

    public boolean isAddNewDisputesVisible() {
        return isShown(By.cssSelector("a[href^='mark_disputes?pop=yes&func=addDisputes']"));
    }

    // ════════════════ Table ════════════════
    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.offsetWidth>0;})"
                + ".map(function(h){return h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }

    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }

    private int colIndex(String header) {
        int i = getAllColumnHeaders().indexOf(header);
        if (i < 0) throw new IllegalArgumentException("No column " + header + " in " + getAllColumnHeaders());
        return i;
    }

    // ✅ Cell text of every (searched) row for a column, from the row nodes (the Actions cell holds modal markup)
    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("var i=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray()"
                + ".map(function(r){return r.children[i].textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public List<String> getOrderedColumnValues(String header) {
        return strings(js().executeScript("var i=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows({search:'applied',order:'applied'}).nodes().toArray()"
                + ".map(function(r){return r.children[i].textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    // ✅ Every row: the 9 data columns + "color" (red / orange / "") + "actions" (titles, comma-separated) + "module_id"
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRows() {
        Object o = js().executeScript("var hs=[].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.replace(/\\s+/g,' ').trim();});"
                + "return jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().map(function(r){ var out={};"
                + " for(var i=0;i<9;i++) out[hs[i]]=r.children[i].textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();"
                + " var m=(r.getAttribute('style')||'').match(/color:\\s*(\\w+)/); out.color=m?m[1]:'';"
                + " var a=r.children[9]; out.actions=[].map.call(a.querySelectorAll(':scope > a'),function(x){return (x.getAttribute('title')||x.getAttribute('data-original-title')||'').trim();}).join(', ');"
                + " var l=a.querySelector('a[href*=comment_module_id]'); var mm=l?(l.getAttribute('href').match(/comment_module_id=(\\d+)/)):null; out.module_id=mm?mm[1]:'';"
                + " return out;});");
        List<Map<String, String>> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(map(x));
        return out;
    }

    public List<Map<String, String>> getRowsForChild(String childId) {
        List<Map<String, String>> out = new ArrayList<>();
        for (Map<String, String> r : getRows()) if (childId.equals(r.get("Child Id"))) out.add(r);
        return out;
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
        return str("var i=document.querySelector('#datatable-pay-list_info');return i?i.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';");
    }

    public String getEmptyTableMessage() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';");
    }

    public boolean isSearchVisible() { return isShown(By.cssSelector("#datatable-pay-list_filter input")); }

    public boolean isPaginationVisible() { return isShown(By.cssSelector("#datatable-pay-list_paginate")); }

    public String getDownloadButtonText() {
        return str("var b=document.querySelector('#datatable-pay-list_wrapper .dt-buttons .dt-button');return b?b.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    // ✅ CSV file name configured on the DataTables button (without .csv), e.g. Customers_Paying_Offline_Disputes_20261119165008
    public String getDownloadFileName() {
        return str("var b=jQuery('" + TABLE + "').DataTable().settings()[0].oInit.buttons||[]; return b.length?(b[0].filename||b[0].title||''):'';");
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#datatable-pay-list_wrapper .dt-buttons .dt-button').click();");
        System.out.println("▶ Download Data clicked");
        Thread.sleep(1500);
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector("#datatable-pay-list_filter input"));
        box.clear();
        if (!keyword.isEmpty()) box.sendKeys(keyword);
        else js().executeScript("jQuery('" + TABLE + "').DataTable().search('').draw();");
        Thread.sleep(1200);
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('#datatable-pay-list_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('#datatable-pay-list_paginate .paginate_button.previous').click();");
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

    // ════════════════ Confirm capture ════════════════
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__confirms=[]; window.confirm=function(m){ window.__confirms.push(m); return " + acceptConfirm + "; };");
    }

    public List<String> getCapturedConfirms() {
        return strings(js().executeScript("return window.__confirms || [];"));
    }

    // ════════════════ Popdown (Add / Call log / Resolve / History) ════════════════
    private void clickLink(String css) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var a=document.querySelector(arguments[0]); if(!a) return false; a.scrollIntoView({block:'center'}); a.click(); return true;", css);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Link not found: " + css);
        Thread.sleep(1500);
    }

    // ✅ Waits for the popdown to show the given text; returns its visible text
    public String waitForPopup(String marker) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
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
                + "var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';"
                + "var p=document.querySelector('" + POP + "'); if(p) p.style.display='none';");
        Thread.sleep(700);
    }

    public void openAddDispute() throws InterruptedException {
        clickLink("a[href^='mark_disputes?pop=yes&func=addDisputes']");
        System.out.println("▶ Add New Disputes clicked");
        waitForPopup("Mark Dispute");
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #mark_dispute_frm #add_dispute_button")).isEmpty());
        Thread.sleep(500);
    }

    public void enterChildId(String childId) {
        WebElement e = driver.findElement(By.cssSelector(POP + " #mark_dispute_frm #child_id"));
        e.clear();
        if (!childId.isEmpty()) e.sendKeys(childId);
    }

    // ✅ "Fetch Child Details" → api/franchises/getChildDetails; waits for the AJAX to finish
    public void clickFetchChildDetails() throws InterruptedException {
        Object ok = js().executeScript("var b=[].filter.call(document.querySelectorAll('" + POP + " a, " + POP + " button'),function(x){return /Fetch Child Details/.test(x.textContent);})[0];"
                + "if(!b) return false; b.click(); return true;");
        if (!Boolean.TRUE.equals(ok)) throw new IllegalStateException("No Fetch Child Details button");
        System.out.println("▶ Fetch Child Details clicked");
        Thread.sleep(800);
        for (int i = 0; i < 30; i++) {
            if (!Boolean.TRUE.equals(js().executeScript("return (jQuery.active||0)>0;"))) break;
            Thread.sleep(500);
        }
        Thread.sleep(500);
    }

    // ✅ Child name shown after Fetch: the hidden #child_name value + the visible "Child Name :" text
    public String getFetchedChildName() {
        return str("return (jQuery('" + POP + " #mark_dispute_frm #child_name').val()||'').trim();");
    }

    public String getAddFormText() {
        return str("var f=document.querySelector('" + POP + " #mark_dispute_frm'); return f?f.innerText.replace(/\\s+/g,' ').trim():'';");
    }

    public List<String> getTypeOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + POP + " #invoice_payment_dispute option'),function(o){return o.text.trim();});"));
    }

    public void selectType(String type) {
        js().executeScript("var s=document.querySelector('" + POP + " #invoice_payment_dispute'); s.value=arguments[0]; jQuery(s).trigger('change');", type);
    }

    public void enterPopupComment(String text) {
        WebElement t = driver.findElement(By.cssSelector(POP + " #note_text"));
        t.clear();
        if (!text.isEmpty()) t.sendKeys(text);
    }

    public void clickAddDispute() throws InterruptedException {
        js().executeScript("document.querySelector('" + POP + " #add_dispute_button').click();");
        System.out.println("▶ Add Dispute clicked");
        Thread.sleep(900);
    }

    public void clickPopupSubmit() throws InterruptedException {
        js().executeScript("document.querySelector('" + POP + " #add_notes_button').click();");
        System.out.println("▶ Submit clicked");
        Thread.sleep(900);
    }

    // ✅ jQuery-validate errors in the popdown (field name → message)
    public Map<String, String> getPopupErrors() {
        return map(js().executeScript("var out={}; document.querySelectorAll('" + POP + " label.text-danger').forEach(function(l){"
                + "if(l.offsetWidth>0 && l.textContent.trim()) out[l.getAttribute('for')||l.id]=l.textContent.trim();}); return out;"));
    }

    // ✅ Flash message inside the popdown (.update-message-notes / .panel-container-form)
    public String getPopupMessage() {
        return str("var o=[]; document.querySelectorAll('" + POP + " .flash-msg .alert, " + POP + " .update-message-notes .alert, " + POP + " .alert').forEach(function(e){"
                + "var t=e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim(); if(e.offsetWidth>0 && t && o.indexOf(t)<0) o.push(t);}); return o.join(' | ');");
    }

    public String waitForPopupMessage() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            String m = getPopupMessage();
            if (!m.isEmpty()) return m;
            Thread.sleep(500);
        }
        return "";
    }

    // ════════════════ Row actions ════════════════
    private String rowAction(String childId, String hrefPart) {
        return TABLE + " tr#emp-row-" + childId + " td:last-child > a[href*='" + hrefPart + "']";
    }

    private void showRow(String childId) throws InterruptedException {
        js().executeScript("var dt=jQuery('" + TABLE + "').DataTable(); dt.search('').draw(); var I='emp-row-'+arguments[0];"
                + "var idx=dt.rows({search:'applied',order:'applied'}).nodes().toArray().findIndex(function(r){return r.id===I;});"
                + "if(idx>=0) dt.page(Math.floor(idx/dt.page.len())).draw('page');", childId);
        Thread.sleep(600);
    }

    // Shows the page that holds the row of a dispute (comment_module_id)
    private void showModuleRow(String moduleId) throws InterruptedException {
        js().executeScript("var dt=jQuery('" + TABLE + "').DataTable(); var M='comment_module_id='+arguments[0];"
                + "var idx=dt.rows({search:'applied',order:'applied'}).nodes().toArray().findIndex(function(r){return (r.innerHTML||'').indexOf(M)>=0;});"
                + "if(idx>=0) dt.page(Math.floor(idx/dt.page.len())).draw('page');", moduleId);
        Thread.sleep(600);
    }

    // ✅ Call → Parents Contact Details modal; returns its text
    public String openCallModal(String childId) throws InterruptedException {
        showRow(childId);
        clickLink(TABLE + " tr#emp-row-" + childId + " a[data-target='#call_parents_" + childId + "']");
        System.out.println("▶ Call clicked (child " + childId + ")");
        return str("var m=document.getElementById('call_parents_'+arguments[0]); return m && m.offsetWidth>0 ? m.innerText.replace(/\\s+/g,' ').trim() : '';", childId);
    }

    public List<String> getCallModalLinks(String childId) {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('#call_parents_'+arguments[0]+' a[href^=tel], #call_parents_'+arguments[0]+' a[href^=mailto]'),function(a){return a.getAttribute('href');})"
                + ".filter(function(v,i,s){return s.indexOf(v)===i;});", childId));
    }

    public void closeCallModal(String childId) throws InterruptedException {
        js().executeScript("jQuery('#call_parents_'+arguments[0]).modal('hide');", childId);
        Thread.sleep(700);
    }

    public String openHistory(String childId) throws InterruptedException {
        showRow(childId);
        clickLink(rowAction(childId, "pop_history_view"));
        System.out.println("▶ History clicked (child " + childId + ")");
        return waitForPopup("Child Updates History");
    }

    public void openUpdateCallLog(String childId) throws InterruptedException {
        showRow(childId);
        clickLink(rowAction(childId, "func=update_call_log"));
        System.out.println("▶ Update Call Log clicked (child " + childId + ")");
        waitForPopup("Update Call Log Details");
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #user_note #add_notes_button")).isEmpty());
    }

    public void openResolveDispute(String childId) throws InterruptedException {
        showRow(childId);
        clickLink(rowAction(childId, "func=resolve_dispute"));
        System.out.println("▶ Resolve Dispute clicked (child " + childId + ")");
        waitForPopup("Dispute");
        wait.until(d -> !d.findElements(By.cssSelector(POP + " #user_note #add_notes_button")).isEmpty());
    }

    // ✅ Completed rows: the call history link (comment_list without func)
    public String openCallHistory(String childId, String moduleId) throws InterruptedException {
        showRow(childId);
        String css = TABLE + " tr#emp-row-" + childId + " td:last-child > a[href^='comment_list']:not([href*='func='])";
        if (moduleId != null && !moduleId.isEmpty()) {
            showModuleRow(moduleId);
            css = TABLE + " td:last-child > a[href^='comment_list'][href$='comment_module_id=" + moduleId + "']:not([href*='func='])";
        }
        clickLink(css);
        System.out.println("▶ Call history clicked (child " + childId + ")");
        return waitForPopup("Comment List");
    }

    public String getPopupTitle() {
        return str("var h=document.querySelector('" + POP + " .panel-title, " + POP + " h5, " + POP + " h4, " + POP + " .modal-title'); return h?h.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    // ✅ Comments listed in the call log popup (newest first): "@<user> : <text> <date>"
    public List<String> getPopupNotes() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + POP + " .user-note-listing p'),function(p){return p.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});"));
    }
}
