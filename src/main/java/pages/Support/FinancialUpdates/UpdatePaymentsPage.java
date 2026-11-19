package pages.Support.FinancialUpdates;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// UPDATE PAYMENTS (Support → Financial Updates → Update Payments, /invoices?type=due)
// All selectors confirmed live 2026-11-18 — see CLAUDE.md
// "Requirements — Support → Financial Updates".
// Filters post the form back to the page; the table is a client-side
// DataTable (#dt_invoice_list) holding at most 1,000 invoices.
// ═══════════════════════════════════════════════════════════════
public class UpdatePaymentsPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_invoice_report";
    private static final String TABLE = "#dt_invoice_list";
    private static final String VOID = "#modal_void_invoice_pop";

    public UpdatePaymentsPage(WebDriver driver) {
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

    // ✅ UAT's fixed "API Debug (Test)" panel covers the bottom half of the page
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

    // ✅ "Support > Financial Updates > Update Payments" from the page's menuData
    public String getMenuPath() {
        // Two menu items share this URL (Children > Billing > Due Payments too) — take the Support one
        return str("var m=(window.menuData||[]).filter(function(x){return x.url==='invoices?type=due';});"
                + "var s=m.filter(function(x){return x.topMenu==='Support';})[0]||m[0];"
                + "return s ? s.topMenu+' > '+s.subMenu+' > '+s.title : '';");
    }

    // ✅ Every menu path that opens this URL
    public List<String> getAllMenuPaths() {
        return strings(js().executeScript("return (window.menuData||[]).filter(function(x){return x.url==='invoices?type=due';})"
                + ".map(function(s){return s.topMenu+' > '+s.subMenu+' > '+s.title;});"));
    }

    // ✅ h2 "Due Payments Current Year (Jan 2026 - Nov 2026)" — follows the Invoice Date filter (h5 before the 19-Nov redesign)
    public String getHeading() {
        return str("var h=[].filter.call(document.querySelectorAll('h2,h5'),function(e){return /^\\s*Due Payments/.test(e.textContent) && e.offsetWidth>0;})[0];"
                + "return h?h.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim():'';");
    }

    // ✅ Count shown under the heading, e.g. "5454 invoices" / "1 invoice" (new in the 19-Nov redesign)
    public String getInvoiceCountText() {
        return str("var m=document.body.innerText.match(/Due Payments[^\\n]*\\n+\\s*(\\d[\\d,]* invoices?)/); return m?m[1]:'';");
    }

    // ✅ The blue info note box (Void / Adjust Invoice / Adjust Excess Payment); since the 19-Nov
    //    redesign it is one .clx-note-info box without the 1)–3) numbering
    public String getNotes() {
        return str("var b=document.querySelector('.clx-note-box.clx-note-info'); return b && b.offsetWidth>0 ? b.textContent.replace(/\\s+/g,' ').trim() : '';");
    }

    // ════════════════ Filters ════════════════
    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " label'), function(l){return l.textContent.replace(/\\s+/g,' ').replace(/:$/,'').trim();})"
                + ".filter(function(t,i,a){return t && t.length<30 && !/^(Select all)$/.test(t) && a.indexOf(t)===i;});"));
    }

    public boolean isFieldVisible(String id) {
        return isShown(By.cssSelector(FILTER + " #" + id)) || isShown(By.cssSelector(FILTER + " #" + id + " + .btn-group button"));
    }

    // ✅ bootstrap-multiselect button text, e.g. "No Filters" / "Due, Overdue"
    public String getMultiselectButtonText(String id) {
        return str("var b=jQuery('#'+arguments[0]).next('.btn-group').find('button.multiselect');"
                + "return b.length ? b.text().replace(/\\s+/g,' ').trim() : '';", id);
    }

    public List<String> getMultiselectOptions(String id) {
        return strings(js().executeScript("return jQuery('#'+arguments[0]).find('option').map(function(){return jQuery(this).text().trim();}).get();", id));
    }

    public List<String> getMultiselectSelected(String id) {
        return strings(js().executeScript("return jQuery('#'+arguments[0]).find('option:selected').map(function(){return jQuery(this).text().trim();}).get();", id));
    }

    // ✅ Selects exactly these options (by text) in a bootstrap-multiselect, without firing onChange
    public void multiselectOnly(String id, String... optionTexts) {
        Object missing = js().executeScript("var s=jQuery('#'+arguments[0]), want=arguments[1], miss=[];"
                + "s.multiselect('deselectAll', false);"
                + "want.forEach(function(t){var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                + " if(!o.length) miss.push(t); else s.multiselect('select', o.first().val());});"
                + "s.multiselect('refresh'); return miss.join(',');", id, java.util.Arrays.asList(optionTexts));
        if (missing != null && !String.valueOf(missing).isEmpty())
            throw new IllegalArgumentException("Not in #" + id + ": " + missing);
    }

    public List<String> getSelectOptions(String id) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(driver.findElement(By.cssSelector(FILTER + " #" + id))).getOptions())
            out.add(o.getAttribute("textContent").replace(' ', ' ').trim());
        return out;
    }

    public String getSelectedText(String id) {
        return str("var s=document.querySelector('" + FILTER + " #" + id + "');"
                + "return s && s.selectedIndex>=0 ? s.options[s.selectedIndex].text.trim() : '';");
    }

    public String getSelectedValue(String id) {
        return str("var s=document.querySelector('" + FILTER + " #" + id + "'); return s ? s.value : '';");
    }

    // ✅ Plain select by value; fires change (Invoice Date shows / hides the month range on change)
    public void selectByValue(String id, String value) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #" + id + "'), V=arguments[0];"
                + "if(!s.find('option').filter(function(){return this.value===V;}).length) return false;"
                + "s.val(V).trigger('change'); return true;", value);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Value '" + value + "' not in #" + id);
    }

    public boolean isMonthRangeVisible() {
        return isShown(By.cssSelector(FILTER + " #filter_month_range_1")) && isShown(By.cssSelector(FILTER + " #filter_month_range_2"));
    }

    public void enterAdmissionId(String childId) {
        WebElement e = driver.findElement(By.cssSelector(FILTER + " #sr_child_id"));
        e.clear();
        e.sendKeys(childId);
    }

    public String getAdmissionId() {
        return str("var e=document.querySelector('" + FILTER + " #sr_child_id'); return e?e.value:'';");
    }

    public void clickFilter() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        System.out.println("▶ Filter clicked");
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    public void clickReset() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_reset').click();");
        System.out.println("▶ Reset clicked");
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Max-rows note + Download All ════════════════
    // ✅ "* Total 5390 invoices found but max 1000 invoices are shown. Please restrict your search criteria"
    public String getMaxRowsNote() {
        return str("var a=document.getElementById('download_all_invoice_list'); if(!a) return '';"
                + "var p=a.parentElement; return p.offsetWidth>0 ? p.textContent.replace(/\\s+/g,' ').trim() : '';");
    }

    public String getDownloadAllHref() {
        return str("var a=document.getElementById('download_all_invoice_list'); return a ? a.href : '';");
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

    // ✅ Every value of a column (all filtered rows), HTML stripped
    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable();"
                + "return dt.column(arguments[0],{search:'applied'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    // ✅ Values in the order currently drawn (after sorting), all pages
    public List<String> getOrderedColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable();"
                + "return dt.column(arguments[0],{search:'applied',order:'applied'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("var dt=jQuery('" + TABLE + "').DataTable();"
                + "return dt.column(arguments[0],{page:'current'}).data().toArray().map(function(v){"
                + "return jQuery('<div>').html(v).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", colIndex(header)));
    }

    // ✅ One row's cells by header → text, located by invoice id
    @SuppressWarnings("unchecked")
    public Map<String, String> getRow(String invoiceId) {
        Object o = js().executeScript("var I=arguments[0], dt=jQuery('" + TABLE + "').DataTable(), hs=[].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.replace(/\\u00a0/g,' ').trim();});"
                + "var r=dt.rows().nodes().toArray().filter(function(r){var b=r.querySelector('.invoice_id'); return b && b.textContent.trim()===I;})[0];"
                + "if(!r) return null; var out={}; [].forEach.call(r.children,function(td,i){ out[hs[i]||('col'+i)]=td.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();}); return out;", invoiceId);
        Map<String, String> m = new LinkedHashMap<>();
        if (o instanceof Map) ((Map<String, Object>) o).forEach((k, v) -> m.put(k, String.valueOf(v)));
        return m;
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
        return str("var i=document.querySelector('#dt_invoice_list_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';");
    }

    public String getEmptyTableMessage() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';");
    }

    public boolean isSearchVisible() { return isShown(By.cssSelector("#dt_invoice_list_filter input")); }

    public boolean isPaginationVisible() { return isShown(By.cssSelector("#dt_invoice_list_paginate")); }

    public boolean isSelectAllVisible() { return isShown(By.id("select_all_page")); }

    public String getDownloadReportText() {
        return str("var b=document.querySelector('#dt_invoice_list_wrapper .dt-buttons .dt-button');return b?b.textContent.trim():'';");
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector("#dt_invoice_list_filter input"));
        box.clear();
        if (!keyword.isEmpty()) box.sendKeys(keyword);
        else js().executeScript("jQuery('" + TABLE + "').DataTable().search('').draw();");
        Thread.sleep(1200);
    }

    public void clickDownloadReport() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#dt_invoice_list_wrapper .dt-buttons .dt-button').click();");
        System.out.println("▶ Download Report clicked");
        Thread.sleep(1500);
    }

    public boolean isNextPageEnabled() {
        return !driver.findElements(By.cssSelector("#dt_invoice_list_paginate .paginate_button.next:not(.disabled)")).isEmpty();
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('#dt_invoice_list_paginate .paginate_button.next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('#dt_invoice_list_paginate .paginate_button.previous').click();");
        Thread.sleep(800);
    }

    public void clickPageNumber(String n) throws InterruptedException {
        js().executeScript("var N=arguments[0]; [].filter.call(document.querySelectorAll('#dt_invoice_list_paginate .paginate_button'),function(a){return a.textContent.trim()===N;})[0].click();", n);
        Thread.sleep(800);
    }

    public int getCurrentPage() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.info().page;")).intValue() + 1;
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("var A=arguments[0]; [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\u00a0/g,' ').trim()===A && h.offsetWidth>0;})[0].click();", header);
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return str("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.replace(/\\u00a0/g,' ').trim()===A && h.offsetWidth>0;})[0];"
                + "return h?h.className:'';", header);
    }

    // ════════════════ Row actions ════════════════
    // ✅ Action set per row over all filtered rows → {"details+credit+void": n, ...}
    @SuppressWarnings("unchecked")
    public Map<String, Long> getActionSets() {
        Object o = js().executeScript("var out={}; jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray().forEach(function(r){"
                + "var c=r.lastElementChild, a=[];"
                + "if(c.querySelector('.invoice_pop_details')) a.push('details');"
                + "if(c.querySelector('.adjust_credits_button[data-type=credit_adjust]')) a.push('credit');"
                + "if(c.querySelector('.adjust_credits_button[data-type=security_payment_adjust]')) a.push('security');"
                + "if(c.querySelector('.adjust_credits_button[data-type=excess_payment_adjust]')) a.push('excess');"
                + "if(c.querySelector('.void_credits_button')) a.push('void');"
                + "if(c.querySelector('.revert_credit_adjustment')) a.push('revert');"
                + "var k=a.join('+')||'none'; out[k]=(out[k]||0)+1;}); return out;");
        Map<String, Long> m = new java.util.TreeMap<>();
        if (o instanceof Map) ((Map<String, Object>) o).forEach((k, v) -> m.put(k, ((Number) v).longValue()));
        return m;
    }

    // ✅ Rows having an action: [invoiceId, childId, amount, paid, status] — action = details|credit|security|excess|void
    @SuppressWarnings("unchecked")
    public List<List<String>> getRowsWithAction(String action) {
        String sel = action.equals("void") ? ".void_credits_button"
                : action.equals("details") ? ".invoice_pop_details"
                : ".adjust_credits_button[data-type=" + (action.equals("credit") ? "credit_adjust"
                : action.equals("security") ? "security_payment_adjust" : "excess_payment_adjust") + "]";
        Object o = js().executeScript("var S=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray()"
                + ".filter(function(r){return r.lastElementChild.querySelector(S);}).map(function(r){var c=r.children;"
                + " function t(i){return c[i].textContent.replace(/\\s+/g,' ').trim();} return [t(1),t(3),t(8),t(9),t(10)];});", sel);
        List<List<String>> out = new ArrayList<>();
        if (o instanceof List) for (Object r : (List<Object>) o) out.add(strings(r));
        return out;
    }

    // ✅ Tooltip of an action icon (Adjust shows Due Amount / Credit Balance), HTML stripped
    public String getActionTooltip(String invoiceId, String cssInCell) {
        return str("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){var b=r.querySelector('.invoice_id'); return b && b.textContent.trim()===I;})[0];"
                + "if(!r) return ''; var a=r.lastElementChild.querySelector(arguments[1]); if(!a) return '';"
                + "return jQuery('<div>').html((a.getAttribute('data-original-title')||a.getAttribute('title')||'').replace(/<[^>]+>/g,' ')).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();", invoiceId, cssInCell);
    }

    // ✅ Invoice Details popover content (data-content), HTML stripped
    public String getInvoiceDetails(String invoiceId) {
        return str("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){var b=r.querySelector('.invoice_id'); return b && b.textContent.trim()===I;})[0];"
                + "if(!r) return ''; var a=r.querySelector('.invoice_pop_details');"
                + "return a ? jQuery('<div>').html(a.getAttribute('data-content')).text().replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim() : '';", invoiceId);
    }

    // ✅ Opens the Invoice Details popover and returns its visible text
    public String openInvoiceDetailsPopover(String invoiceId) throws InterruptedException {
        goToRowPage(invoiceId);
        js().executeScript("var I=arguments[0]; var b=[].filter.call(document.querySelectorAll('" + TABLE + " .invoice_id'),function(e){return e.textContent.trim()===I;})[0];"
                + "var a=b.closest('tr').querySelector('.invoice_pop_details'); a.scrollIntoView({block:'center'}); jQuery(a).popover('show');", invoiceId);
        Thread.sleep(1000);
        return str("var p=document.querySelector('.popover'); return p && p.offsetWidth>0 ? p.innerText.replace(/\\s+/g,' ').trim() : '';");
    }

    // ✅ Draws the DataTable page that holds the invoice, so its row is in the DOM / visible
    public void goToRowPage(String invoiceId) throws InterruptedException {
        js().executeScript("var I=arguments[0], dt=jQuery('" + TABLE + "').DataTable();"
                + "var idx=dt.rows({search:'applied',order:'applied'}).nodes().toArray().findIndex(function(r){var b=r.querySelector('.invoice_id'); return b && b.textContent.trim()===I;});"
                + "if(idx>=0){ dt.page(Math.floor(idx/dt.page.len())).draw('page'); }", invoiceId);
        Thread.sleep(600);
    }

    public String getPdfHref(String invoiceId) {
        return str("var I=arguments[0]; var b=[].filter.call(jQuery('" + TABLE + "').DataTable().rows().nodes().toArray(),function(r){var e=r.querySelector('.invoice_id'); return e && e.textContent.trim()===I;})[0];"
                + "if(!b) return ''; var a=b.querySelector('.invoice_id').closest('a'); return a ? a.href : '';", invoiceId);
    }

    // ✅ First invoice (all filtered rows) whose Admission Status is the given value, e.g. "Active"
    public String getFirstInvoiceWithAdmissionStatus(String status) {
        return str("var S=arguments[0], i=" + "[].map.call(document.querySelectorAll('" + TABLE + " thead th'),function(h){return h.textContent.trim();}).indexOf('Admission Status');"
                + "var r=jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray().filter(function(r){return r.children[i].textContent.trim()===S;})[0];"
                + "return r ? r.querySelector('.invoice_id').textContent.trim() : '';", status);
    }

    public String getChildInfoHref(String invoiceId) {
        return str("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){var e=r.querySelector('.invoice_id'); return e && e.textContent.trim()===I;})[0];"
                + "if(!r) return ''; var a=r.querySelector('a[href*=pop_child_info]'); return a ? a.getAttribute('href') : '';", invoiceId);
    }

    public void clickChildName(String invoiceId) throws InterruptedException {
        goToRowPage(invoiceId);
        js().executeScript("var I=arguments[0]; var b=[].filter.call(document.querySelectorAll('" + TABLE + " .invoice_id'),function(e){return e.textContent.trim()===I;})[0];"
                + "var a=b.closest('tr').querySelector('a[href*=pop_child_info]'); a.scrollIntoView({block:'center'}); a.click();", invoiceId);
        Thread.sleep(3000);
    }

    public String getOpenPopupText() {
        return str("var o=''; document.querySelectorAll('#popdown-dialog, .modal').forEach(function(m){ if(m.offsetWidth>0) o+=' '+m.innerText;}); return o.replace(/\\s+/g,' ').trim();");
    }

    // ════════════════ Adjust (credit balance / security deposit) ════════════════
    // ✅ Clicks the adjust icon; the native confirm is answered by startCapture(true).
    //    Returns the icon cell's text after the POST (the page writes data.Response into it).
    public String clickAdjust(String invoiceId, String dataType) throws InterruptedException {
        goToRowPage(invoiceId);
        Object ok = js().executeScript("var I=arguments[0], T=arguments[1]; var b=[].filter.call(document.querySelectorAll('" + TABLE + " .invoice_id'),function(e){return e.textContent.trim()===I;})[0];"
                + "if(!b) return false; var a=b.closest('tr').querySelector('.adjust_credits_button[data-type='+T+']'); if(!a) return false;"
                + "a.setAttribute('data-qa-adjust','1'); a.scrollIntoView({block:'center'}); a.click(); return true;", invoiceId, dataType);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No " + dataType + " icon for invoice " + invoiceId);
        System.out.println("▶ Adjust (" + dataType + ") clicked for invoice " + invoiceId);
        for (int i = 0; i < 60; i++) {
            Thread.sleep(500);
            String t = str("var a=document.querySelector('[data-qa-adjust]'); if(!a) return '';"
                    + "if(a.querySelector('.spinner')) return ''; return a.textContent.replace(/\\s+/g,' ').trim() + (a.querySelector('.fa-exclamation-circle')?'[ERROR]':'');");
            if (!t.isEmpty()) return t;
        }
        return "";
    }

    // ✅ Message flashed in .form-update-message (adjust errors, revert)
    public String getPageMessage() {
        return str("var o=[]; document.querySelectorAll('.form-update-message, .flash-msg').forEach(function(e){"
                + "if(e.offsetWidth>0 && e.textContent.trim() && !e.closest('" + VOID + "')) o.push(e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim());}); return o.join(' | ');");
    }

    // ════════════════ Void Invoice popup ════════════════
    public void openVoidPopup(String invoiceId) throws InterruptedException {
        goToRowPage(invoiceId);
        Object ok = js().executeScript("var a=document.querySelector('.void_credits_button_'+arguments[0]); if(!a) return false;"
                + "a.scrollIntoView({block:'center'}); a.click(); return true;", invoiceId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No Void icon for invoice " + invoiceId);
        System.out.println("▶ Void Invoice clicked (invoice " + invoiceId + ")");
        Thread.sleep(1500);
    }

    public boolean isVoidPopupVisible() { return isShown(By.cssSelector(VOID + " #void_reason")); }

    public String getVoidPopupText() {
        return str("var m=document.querySelector('" + VOID + "');return m?m.innerText.replace(/\\s+/g,' ').trim():'';");
    }

    public String getVoidHidden(String id) {
        return str("var e=document.querySelector('" + VOID + " #'+arguments[0]);return e?e.value:'';", id);
    }

    public void enterVoidReason(String reason) {
        WebElement t = driver.findElement(By.cssSelector(VOID + " #void_reason"));
        t.clear();
        t.sendKeys(reason);
    }

    public void clickVoidSubmit() throws InterruptedException {
        js().executeScript("[].filter.call(document.querySelectorAll('" + VOID + " .modal-footer button'),function(b){return b.textContent.trim()==='Submit';})[0].click();");
        System.out.println("▶ Void Submit clicked");
        Thread.sleep(1000);
    }

    public String getVoidReasonError() {
        return str("var e=document.querySelector('" + VOID + " .alert-void_reason');return e && e.offsetWidth>0 ? e.textContent.trim() : '';");
    }

    public String waitForVoidMessage() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            String m = str("var o=[];document.querySelectorAll('" + VOID + " .form-update-message, " + VOID + " .flash-msg').forEach(function(e){"
                    + "if(e.offsetWidth>0 && e.textContent.trim()) o.push(e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim());}); return o.join(' | ');");
            if (!m.isEmpty()) return m;
            Thread.sleep(500);
        }
        return "";
    }

    // ✅ Void message without the long wait (used after Cancel on the confirm)
    public String waitForVoidMessageQuick() {
        return str("var o=[];document.querySelectorAll('" + VOID + " .form-update-message, " + VOID + " .flash-msg').forEach(function(e){"
                + "if(e.offsetWidth>0 && e.textContent.trim()) o.push(e.textContent.trim());}); return o.join(' | ');");
    }

    public boolean isRowPresent(String invoiceId) {
        return Boolean.TRUE.equals(js().executeScript("var I=arguments[0]; return jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().some(function(r){var b=r.querySelector('.invoice_id'); return b && b.textContent.trim()===I && document.body.contains(r);});", invoiceId));
    }

    public void closeVoidPopup() throws InterruptedException {
        js().executeScript("jQuery('" + VOID + "').modal('hide');");
        Thread.sleep(800);
    }

    // ════════════════ Bulk ZIP selection ════════════════
    public boolean isBulkToolbarVisible() { return isShown(By.id("bulk_download_toolbar")); }

    public void tickInvoiceCheckboxes(int n) throws InterruptedException {
        js().executeScript("var bs=document.querySelectorAll('" + TABLE + " tbody .invoice_checkbox'); for(var i=0;i<arguments[0]&&i<bs.length;i++){ if(!bs[i].checked) bs[i].click(); }", n);
        Thread.sleep(500);
    }

    public void tickSelectAllOnPage() throws InterruptedException {
        js().executeScript("var b=document.getElementById('select_all_page'); if(!b.checked) b.click();");
        Thread.sleep(500);
    }

    public int getCheckedCount() {
        return ((Number) js().executeScript("return document.querySelectorAll('" + TABLE + " .invoice_checkbox:checked').length;")).intValue();
    }

    public String getBulkSelectedCount() {
        return str("var e=document.getElementById('bulk_selected_count'); return e?e.textContent.trim():'';");
    }

    public boolean isZipButtonDisabled() {
        return Boolean.TRUE.equals(js().executeScript("var b=document.getElementById('btn_bulk_download_zip'); return !b || b.disabled;"));
    }

    // ✅ Clicks Clear Selection through its handler (the toolbar is hidden, so not via a real click)
    public void clickClearSelection() throws InterruptedException {
        js().executeScript("jQuery('#btn_clear_selection').trigger('click');");
        Thread.sleep(500);
    }

    // ════════════════ Confirm / request capture ════════════════
    // ✅ Records native confirms (answered with acceptConfirm) and XHR URLs
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__reqs=[]; window.__confirms=[];"
                + "var o=XMLHttpRequest.prototype.open; XMLHttpRequest.prototype.open=function(m,u){window.__reqs.push(m+' '+u); return o.apply(this,arguments);};"
                + "window.confirm=function(m){ window.__confirms.push(m); return " + acceptConfirm + "; };");
    }

    public List<String> getCapturedRequests() { return strings(js().executeScript("return window.__reqs || [];")); }

    public List<String> getCapturedConfirms() { return strings(js().executeScript("return window.__confirms || [];")); }
}
