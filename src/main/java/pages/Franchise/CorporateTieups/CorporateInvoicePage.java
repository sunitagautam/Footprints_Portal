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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Corporate Invoice (/corporate_invoice).
 * Filter form #frm_invoice_report (posts back); DataTable #datatable-invoice (client side, max 1000 rows; the
 * "Download All N Records" button exports every matching record). Row actions: download PDF, Void Invoice
 * (a.void_credits_button → #modal_void_invoice_pop), Update payment (.btn-update → #modal_form_update, needs the
 * Update_Corporate_Invoice right). On Status = Void the last column is "Voided On".
 */
public class CorporateInvoicePage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_invoice_report";
    private static final String TABLE = "#datatable-invoice";
    private static final String VOID = "#modal_void_invoice_pop";

    public CorporateInvoicePage(WebDriver driver) {
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
        return String.valueOf(js().executeScript("var h=document.querySelector('h2');return h?h.textContent.trim():'';"));
    }

    // ════════════════ Filters ════════════════
    public List<String> getFilterLabels() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " label'), function(l){return l.textContent.trim();})"
                + ".filter(function(t){return t && t.length<30 && !/^(Select all)$/.test(t);}).slice(0,8);"));
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

    public String findOption(String id, String prefix) {
        for (String o : getSelectOptions(id))
            if (o.startsWith(prefix)) return o;
        return "";
    }

    public void enterChildId(String childId) {
        WebElement e = driver.findElement(By.cssSelector(FILTER + " #sr_child_id"));
        e.clear();
        e.sendKeys(childId);
    }

    public String getFieldValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + FILTER + " #" + id + "');return e?e.value:'';"));
    }

    // ✅ pickadate field (sr_invoice_from / sr_invoice_to) — month is 1-based here
    public void setDate(String id, int year, int month, int day) {
        js().executeScript("jQuery('#" + id + "').pickadate('picker').set('select',[arguments[0],arguments[1],arguments[2]]);",
                year, month - 1, day);
    }

    // ✅ bootstrap-multiselect (#sr_center_id / #sr_ownership) — options selected without firing onChange
    public List<String> getMultiselectOptions(String id) {
        return strings(js().executeScript("return jQuery('#'+arguments[0]).find('option').map(function(){return jQuery(this).text().trim();}).get();", id));
    }

    public void multiselectOnly(String id, String optionText) {
        Object ok = js().executeScript("var s=jQuery('#'+arguments[0]), t=arguments[1];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.multiselect('deselectAll', false); s.multiselect('select', o.first().val()); s.multiselect('refresh'); return true;", id, optionText);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("'" + optionText + "' not in #" + id);
    }

    public List<String> getMultiselectSelected(String id) {
        return strings(js().executeScript("return jQuery('#'+arguments[0]).find('option:selected').map(function(){return jQuery(this).text().trim();}).get();", id));
    }

    public void clickFilter() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        System.out.println("▶ Filter clicked");
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    public void clickClear() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_reset').click();");
        System.out.println("▶ Clear clicked");
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

    // ✅ Every value of a column (all filtered rows) — HTML stripped
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

    public String getSearchPlaceholder() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_filter input');return i?i.placeholder:'';"));
    }

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

    // ✅ Last column: "Action" (with Update_Corporate_Invoice), "Download" (without), "Voided On" (Status = Void)
    public String getLastColumnHeader() {
        List<String> v = getVisibleColumnHeaders();
        return v.isEmpty() ? "" : v.get(v.size() - 1);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("var A=arguments[0]; [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0].click();", header);
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return String.valueOf(js().executeScript("var A=arguments[0]; var h=[].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim()===A && h.offsetWidth>0;})[0];"
                + "return h?h.className:'';", header));
    }

    // ✅ Action set per row over all filtered rows → {"download+void": n, ...}; download = PDF link, void = Void Invoice, update = .btn-update
    @SuppressWarnings("unchecked")
    public java.util.Map<String, Long> getActionSets() {
        Object o = js().executeScript("var out={}; jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray().forEach(function(r){"
                + "var c=r.lastElementChild, a=[];"
                + "if(c.querySelector('a[onclick*=PDFDownload]')) a.push('download');"
                + "if(c.querySelector('.btn-update')) a.push('update');"
                + "if(c.querySelector('.void_credits_button')) a.push('void');"
                + "if(c.querySelector('.adjust_credits_button')) a.push('adjust');"
                + "var k=a.join('+')||('text:'+(c.textContent.trim()?'date':'empty')); out[k]=(out[k]||0)+1;}); return out;");
        java.util.Map<String, Long> m = new java.util.TreeMap<>();
        if (o instanceof java.util.Map)
            ((java.util.Map<String, Object>) o).forEach((k, v) -> m.put(k, ((Number) v).longValue()));
        return m;
    }

    // ✅ PDF download URL of an invoice (from the row's PDFDownload(...) call)
    public String getPdfUrl(String invoiceId) {
        return String.valueOf(js().executeScript("var I=arguments[0];"
                + "var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(r){return r.children[1].textContent.trim()===I;})[0];"
                + "if(!r) return ''; var a=r.querySelector('a[onclick*=PDFDownload]'); if(!a) return '';"
                + "var m=a.getAttribute('onclick').match(/PDFDownload\\('([^']+)'/); return m?m[1].replace(/&amp;/g,'&'):'';", invoiceId));
    }

    // ✅ Clicks an invoice's download icon; the page fetches the PDF and saves it as invoice_C<invoiceId>.pdf
    public void clickPdfDownload(String invoiceId) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var a=document.querySelector('.td-action-btn-'+arguments[0]+' a[onclick*=PDFDownload]'); if(!a) return false;"
                + "a.scrollIntoView({block:'center'}); a.click(); return true;", invoiceId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No download icon for invoice " + invoiceId);
        System.out.println("▶ Invoice PDF download clicked (invoice " + invoiceId + ")");
    }

    // ✅ Fetches an invoice PDF the way the page's PDFDownload() does (same URL, XHR as blob) → "HTTP 200 | application/pdf | 14802 | %PDF-1.7"
    public String fetchPdfInfo(String invoiceId) {
        return String.valueOf(js().executeAsyncScript("var done=arguments[arguments.length-1], u=arguments[0];"
                + "var x=new XMLHttpRequest(); x.open('GET',u); x.responseType='blob';"
                + "x.onload=function(){var b=x.response, fr=new FileReader(); fr.onload=function(){done('HTTP '+x.status+' | '+b.type+' | '+b.size+' | '+String(fr.result).substring(0,8));};"
                + " fr.readAsText(b.slice(0,8));}; x.onerror=function(){done('network error');}; x.send();", getPdfUrl(invoiceId)));
    }

    // ════════════════ Update Invoice Payment popup (#modal_form_update, needs Update_Corporate_Invoice) ════════════════
    private static final String PAY = "#modal_form_update";

    // ✅ Opens the popup of the first row with an Update button → its invoice id
    public String openFirstUpdatePayment() throws InterruptedException {
        hideApiDebugPanel();
        Object id = js().executeScript("var b=document.querySelector('" + TABLE + " .btn-update'); if(!b) return '';"
                + "b.scrollIntoView({block:'center'}); b.click(); return b.closest('tr').children[1].textContent.trim();");
        System.out.println("▶ Update clicked (invoice " + id + ")");
        Thread.sleep(2000);
        return String.valueOf(id);
    }

    public void openUpdatePayment(String invoiceId) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var b=document.querySelector('.td-action-btn-'+arguments[0]+' .btn-update'); if(!b) return false;"
                + "b.scrollIntoView({block:'center'}); b.click(); return true;", invoiceId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No Update button for invoice " + invoiceId);
        System.out.println("▶ Update clicked (invoice " + invoiceId + ")");
        Thread.sleep(2000);
    }

    // ✅ Message shown after a payment submit (popup or page flash), "" when none within ~20 s
    public String waitForPaymentMessage() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Object m = js().executeScript("var o=[];document.querySelectorAll('" + PAY + " .error-msg, " + PAY + " .form-update-message, .flash-msg').forEach(function(e){"
                    + "if(e.offsetWidth>0 && e.textContent.trim()) o.push(e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim());}); return o.join(' | ');");
            if (m != null && !String.valueOf(m).isEmpty()) return String.valueOf(m);
            Thread.sleep(500);
        }
        return "";
    }

    public boolean isUpdatePaymentVisible() { return isShown(By.cssSelector(PAY + " #frm-update-payment")); }

    public String getUpdatePaymentText() {
        return String.valueOf(js().executeScript("var m=document.querySelector('" + PAY + "');return m?m.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getPaymentValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + PAY + " #'+arguments[0]);return e?e.value:'';", id));
    }

    public void setPaymentValue(String id, String value) {
        js().executeScript("jQuery('" + PAY + " #'+arguments[0]).val(arguments[1]);", id, value);
    }

    // ✅ pickadate Payment Date: null clears it, else selects [y, m(1-based), d]
    public void setPaymentDate(LocalDate date) {
        if (date == null) js().executeScript("jQuery('#payment_date').pickadate('picker').clear();");
        else js().executeScript("jQuery('#payment_date').pickadate('picker').set('select',[arguments[0],arguments[1],arguments[2]]);",
                date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    public void clickPaymentSubmit() throws InterruptedException {
        js().executeScript("document.querySelector('" + PAY + " button[onclick*=UpdatePayment]').click();");
        System.out.println("▶ Payment Submit clicked");
        Thread.sleep(1500);
    }

    // ✅ Visible validation messages ("<class> => <text>"), shown one at a time
    public List<String> getPaymentErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + PAY + " [class*=alert-]'), function(e){return e.offsetWidth>0 && e.textContent.trim();})"
                + ".map(function(e){return e.textContent.trim();});"));
    }

    public void closeUpdatePayment() throws InterruptedException {
        js().executeScript("jQuery('" + PAY + "').modal('hide');");
        Thread.sleep(800);
    }

    // ════════════════ Void Invoice popup ════════════════
    public void openVoidPopup(String invoiceId) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var a=document.querySelector('.void_credits_button_'+arguments[0]); if(!a) return false;"
                + "a.scrollIntoView({block:'center'}); a.click(); return true;", invoiceId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No Void icon for invoice " + invoiceId);
        System.out.println("▶ Void Invoice clicked (invoice " + invoiceId + ")");
        Thread.sleep(2000);
    }

    public boolean isVoidPopupVisible() { return isShown(By.cssSelector(VOID + " #frm-void-invoice")); }

    public String getVoidPopupText() {
        return String.valueOf(js().executeScript("var m=document.querySelector('" + VOID + "');return m?m.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getVoidHidden(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + VOID + " #'+arguments[0]);return e?e.value:'';", id));
    }

    public String getVoidReasonPlaceholder() {
        return driver.findElement(By.cssSelector(VOID + " #void_reason")).getAttribute("placeholder");
    }

    public void clickVoidSubmit() throws InterruptedException {
        js().executeScript("document.querySelector('" + VOID + " .btn-void-invoice').click();");
        System.out.println("▶ Void Submit clicked");
        Thread.sleep(1500);
    }

    public String getVoidReasonError() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + VOID + " .alert-void_reason');return e && e.offsetWidth>0 ? e.textContent.trim() : '';"));
    }

    public void enterVoidReason(String reason) {
        WebElement t = driver.findElement(By.cssSelector(VOID + " #void_reason"));
        t.clear();
        t.sendKeys(reason);
    }

    // ✅ Message shown in the Void popup after submit (success or error), "" when none within ~20 s
    public String waitForVoidMessage() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Object m = js().executeScript("var o=[];document.querySelectorAll('" + VOID + " .form-update-message, " + VOID + " .error-msg, .flash-msg').forEach(function(e){"
                    + "if(e.offsetWidth>0 && e.textContent.trim()) o.push(e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim());}); return o.join(' | ');");
            if (m != null && !String.valueOf(m).isEmpty()) return String.valueOf(m);
            Thread.sleep(500);
        }
        return "";
    }

    // ✅ Row ids (Invoice ID) on the current page that have a Void icon
    public List<String> getPageInvoiceIdsWithVoid() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({page:'current'}).nodes().toArray()"
                + ".filter(function(r){return r.querySelector('.void_credits_button');}).map(function(r){return r.children[1].textContent.trim();});"));
    }

    public void closeVoidPopup() throws InterruptedException {
        js().executeScript("jQuery('" + VOID + "').modal('hide');");
        Thread.sleep(800);
    }

    // ✅ Records XHRs and native confirms; acceptConfirm=false answers Cancel
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__reqs=[]; window.__confirms=[];"
                + "var o=XMLHttpRequest.prototype.open; XMLHttpRequest.prototype.open=function(m,u){window.__reqs.push(m+' '+u); return o.apply(this,arguments);};"
                + "window.confirm=function(m){ window.__confirms.push(m); return " + acceptConfirm + "; };");
    }

    public List<String> getCapturedRequests() { return strings(js().executeScript("return window.__reqs || [];")); }

    public List<String> getCapturedConfirms() { return strings(js().executeScript("return window.__confirms || [];")); }
}
