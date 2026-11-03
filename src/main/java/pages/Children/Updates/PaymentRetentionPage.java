package pages.Children.Updates;

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

/**
 * Children → Updates → Payment Retention (/payment_retention_list), SDA-13426. Confirmed live 2026-11-03 as Rakesh.
 * <p>
 * Filter form (GET, name payment_retention_filters_form): Status select#filter [due_today = "Due / Current",
 * upcoming, escalated = "With Support", closed], Centre select#center_filter_list (select2), Child ID
 * input[name=admission_id] (number), From / To input[name=from_date|to_date] (type=date; default 1st of month → today),
 * Filter = submit button, Clear = a[href=payment_retention_list].
 * <p>
 * Table #table_payment_retention (client-side DataTable, 25 per page, search "Search child, centre…", empty text
 * "No cases for this filter."). Cells: Child (name link + .clx-meta "#id") | Centre | Attendance "N present · N absent" |
 * Amount ("Rs 8,249.00" + "Invoice Date01 Oct 2026") | Last outcome pill | Status pill | Due | Action
 * (History a[href*=pop_payment_retention_call_history], Log Call a[href*=pop_update_payment_retention_call], or
 * span "Logged").
 * <p>
 * Popups load into #popdown-dialog: History .pr_hist_panel, Log Call .pr_log_panel (radios pr_call_status /
 * pr_received_action / pr_call_with, textarea #pr_reason_notes, Save #pr_submit_btn → native confirm → message in
 * .update-message-notes → page reload after 1.5 s).
 */
public class PaymentRetentionPage {

    WebDriver driver;
    WebDriverWait wait;

    public static final String TABLE = "#table_payment_retention";
    private static final String FORM = "form[name=payment_retention_filters_form]";
    private static final String POPUP = "#popdown-dialog";

    public PaymentRetentionPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private Object run(String script, Object... args) {
        return js().executeScript(script, args);
    }

    private String str(String script, Object... args) {
        Object o = run(script, args);
        return o == null ? "" : String.valueOf(o).trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> strings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(x == null ? "" : String.valueOf(x).trim());
        return out;
    }

    // ✅ UAT's fixed "API Debug (Test)" panel covers the bottom of the page
    public void hideApiDebugPanel() {
        run("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    // ════════════════════════════════════════════════════════
    //  LIST
    // ════════════════════════════════════════════════════════
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

    public String getMenuPath() {
        return str("var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='payment_retention_list')"
                + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';");
    }

    public String getHeading() {
        return str("var h=document.querySelector('.clx-page-head h2'); return h?h.textContent:'';");
    }

    // ✅ e.g. "7 Due / Current"
    public String getCountText() {
        return str("var c=document.querySelector('.clx-page-head .clx-count'); return c?c.textContent.replace(/\\s+/g,' '):'';");
    }

    public String getNoteText() {
        return str("var n=document.querySelector('.clx-note-box.clx-note-info'); return n?n.innerText.replace(/\\s+/g,' '):'';");
    }

    // ✅ "value=label" pairs of the Status select
    public List<String> getStatusOptions() {
        return strings(run("return [...document.querySelectorAll('" + FORM + " select#filter option')].map(o=>o.value+'='+o.text.trim());"));
    }

    public String getSelectedStatus() {
        return str("return document.querySelector('" + FORM + " select#filter').value;");
    }

    public String getFieldValue(String name) {
        return str("var e=document.querySelector('" + FORM + " [name=\"'+arguments[0]+'\"]'); return e?e.value:'';", name);
    }

    public List<String> getColumnHeaders() {
        return strings(run("return [...document.querySelectorAll('" + TABLE + " thead th')].map(h=>h.textContent.replace(/\\s+/g,' ').trim());"));
    }

    public String getInfoText() {
        return str("var i=document.querySelector('#table_payment_retention_wrapper .dataTables_info'); return i?i.textContent.replace(/\\s+/g,' '):'';");
    }

    public String getEmptyText() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty'); return e?e.textContent.trim():'';");
    }

    /**
     * ✅ Every row of the DataTable (all pages, current search applied), as a map:
     * child, childId, centre, attendance, amount, invoiceDate, lastOutcome, status, due, actions, historyTaskId,
     * logCallHref, rowText.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRows() {
        Object o = run("var dt=jQuery('" + TABLE + "').DataTable(); var out=[];"
                + "dt.rows({search:'applied'}).nodes().toArray().forEach(function(r){ var td=r.querySelectorAll('td'); if(td.length<8) return;"
                + " var t=function(i){return td[i].textContent.replace(/\\s+/g,' ').trim();};"
                + " var meta=td[3].querySelector('.clx-meta'); var amt=td[3].querySelector('.clx-task-title');"
                + " var h=td[7].querySelector('a[href*=pop_payment_retention_call_history]');"
                + " var l=td[7].querySelector('a[href*=pop_update_payment_retention_call]');"
                + " var acts=[...td[7].querySelectorAll('a,span,button')].map(e=>e.textContent.trim()).filter(x=>x);"
                + " out.push({child:(td[0].querySelector('a')||td[0]).textContent.trim(),"
                + "  childId:(td[0].querySelector('.clx-meta')||{textContent:''}).textContent.replace('#','').trim(),"
                + "  centre:t(1), attendance:t(2), amount:amt?amt.textContent.trim():'', invoiceDate:meta?meta.textContent.replace('Invoice Date','').trim():'',"
                + "  lastOutcome:t(4), status:t(5), due:t(6), actions:acts.join(' + '),"
                + "  historyTaskId:h?(h.getAttribute('href').match(/task_id=(\\d+)/)||['',''])[1]:'',"
                + "  logCallHref:l?l.getAttribute('href'):'', rowText:r.textContent.replace(/\\s+/g,' ').trim()}); });"
                + "return out;");
        List<Map<String, String>> rows = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) {
            Map<String, String> m = new LinkedHashMap<>();
            ((Map<String, Object>) x).forEach((k, v) -> m.put(k, v == null ? "" : String.valueOf(v)));
            rows.add(m);
        }
        return rows;
    }

    public String getRowCellHtml(String childId) {
        return str("var r=[...jQuery('" + TABLE + "').DataTable().rows().nodes().toArray()].find(r=>r.textContent.indexOf('#'+arguments[0])>=0);"
                + "return r?r.outerHTML.replace(/\\s+/g,' '):'';", childId);
    }

    // ── Filters ──
    public void selectStatus(String value) {
        run("var s=document.querySelector('" + FORM + " select#filter'); s.value=arguments[0]; s.dispatchEvent(new Event('change',{bubbles:true}));", value);
    }

    public void setChildId(String id) {
        run("var e=document.querySelector('" + FORM + " [name=admission_id]'); e.value=arguments[0];", id);
    }

    public void setDateRange(String fromIso, String toIso) {
        run("var f=document.querySelector('" + FORM + " [name=from_date]'), t=document.querySelector('" + FORM + " [name=to_date]');"
                + "f.value=arguments[0]; t.value=arguments[1];", fromIso, toIso);
    }

    // ✅ Centre select2 — select by option text; returns the option value ("" if not found)
    public String selectCentreByName(String centre) {
        String v = str("var opts=document.querySelectorAll('" + FORM + " #center_filter_list option');"
                + "for(var i=0;i<opts.length;i++){ if(opts[i].text.trim()===arguments[0]) return opts[i].value; } return '';", centre);
        if (!v.isEmpty()) run("jQuery('" + FORM + " #center_filter_list').val(arguments[0]).trigger('change');", v);
        return v;
    }

    public String getSelectedCentreText() {
        return str("var s=document.querySelector('" + FORM + " #center_filter_list'); return s.options[s.selectedIndex].text.trim();");
    }

    public void clickFilter() {
        WebElement b = driver.findElement(By.cssSelector(FORM + " button[type=submit]"));
        run("arguments[0].click();", b);
        sleep(2500);
        isPageLoaded();
    }

    public void clickClear() {
        run("document.querySelector('" + FORM + " a[href=\"payment_retention_list\"]').click();");
        sleep(2500);
        isPageLoaded();
    }

    // ✅ DataTable search box (client side)
    public void search(String text) {
        WebElement in = driver.findElement(By.cssSelector("#table_payment_retention_filter input[type=search]"));
        in.clear();
        if (!text.isEmpty()) in.sendKeys(text);
        else run("jQuery('" + TABLE + "').DataTable().search('').draw();");
        sleep(800);
    }

    public String getSearchPlaceholder() {
        return str("var i=document.querySelector('#table_payment_retention_filter input[type=search]'); return i?i.placeholder:'';");
    }

    // ════════════════════════════════════════════════════════
    //  POPUPS (History + Log Call)
    // ════════════════════════════════════════════════════════
    private void clickRowLink(String hrefPart, String taskId) {
        // the row may be on another DataTable page — show all rows first
        run("jQuery('" + TABLE + "').DataTable().page.len(-1).draw();");
        WebElement a = driver.findElement(By.cssSelector(TABLE + " a[href*='" + hrefPart + "'][href*='task_id=" + taskId + "&'], "
                + TABLE + " a[href*='" + hrefPart + "'][href$='task_id=" + taskId + "']"));
        run("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", a);
    }

    public boolean openHistory(String taskId) {
        clickRowLink("pop_payment_retention_call_history", taskId);
        return waitForPopup(".pr_hist_panel");
    }

    public boolean openLogCall(String taskId) {
        clickRowLink("pop_update_payment_retention_call", taskId);
        return waitForPopup(".pr_log_panel");
    }

    private boolean waitForPopup(String panel) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "var p=document.querySelector('" + POPUP + " " + panel + "'); return !!(p && p.offsetParent && window.jQuery);")));
            sleep(800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPopupOpen() {
        return Boolean.TRUE.equals(run("var p=document.querySelector('" + POPUP + "'); return !!(p && p.offsetParent && p.querySelector('.clx-sheet'));"));
    }

    public String getPopupTitle() {
        return str("var h=document.querySelector('" + POPUP + " .clx-sheet-head h3'); return h?h.textContent.replace(/\\s+/g,' '):'';");
    }

    public String getPopupEyebrow() {
        return str("var h=document.querySelector('" + POPUP + " .clx-eyebrow'); return h?h.textContent.replace(/\\s+/g,' '):'';");
    }

    public List<String> getPopupPills() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " .clx-sheet-head .clx-pills .clx-pill')].map(p=>p.textContent.trim());"));
    }

    public String getPopupText() {
        return str("var p=document.querySelector('" + POPUP + "'); return p?p.innerText.replace(/\\s+/g,' '):'';");
    }

    public void closePopup() {
        run("var b=document.querySelector('" + POPUP + " .close-popdown'); if(b) b.click();");
        sleep(1000);
    }

    // ── History ──
    public List<String> getHistoryHeaders() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " .pr_hist_panel table thead th')].map(h=>h.textContent.trim());"));
    }

    // ✅ "step || outcome || callWith || remarks || loggedAt"
    public List<String> getHistoryRows() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " .pr_hist_panel table tbody tr')].map(r=>"
                + "[...r.querySelectorAll('td')].map(c=>c.textContent.replace(/\\s+/g,' ').trim()).join(' || '));"));
    }

    public int countHistoryInputs() {
        return ((Number) run("return document.querySelectorAll('" + POPUP + " .pr_hist_panel table input, " + POPUP
                + " .pr_hist_panel table textarea, " + POPUP + " .pr_hist_panel table select').length;")).intValue();
    }

    public String getHistoryAddNoteRowText() {
        return str("var i=document.querySelector('" + POPUP + " .pr_hist_panel table input, " + POPUP + " .pr_hist_panel table textarea');"
                + "return i? i.closest('tr').textContent.replace(/\\s+/g,' ') : '';");
    }

    // ── Log Call ──
    // ✅ The context card: "dt=dd" pairs
    public Map<String, String> getLogCallContext() {
        Map<String, String> m = new LinkedHashMap<>();
        for (String s : strings(run("return [...document.querySelectorAll('" + POPUP + " .pr_log_panel dl.clx-kv > div')].map(d=>"
                + "(d.querySelector('dt')||{textContent:''}).textContent.trim()+'='+(d.querySelector('dd')||{textContent:''}).textContent.replace(/\\s+/g,' ').trim());"))) {
            int i = s.indexOf('=');
            m.put(s.substring(0, i), s.substring(i + 1));
        }
        return m;
    }

    public String getLogCallGuidance() {
        return str("var n=document.querySelector('" + POPUP + " .pr_log_panel .clx-note-warn'); return n?n.textContent.replace(/\\s+/g,' '):'';");
    }

    // ✅ Field labels in order, e.g. "Call status *", "Outcome *", "Call with *", "Remarks"
    public List<String> getLogCallLabels() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " #pr_log_form .clx-lbl')].map(l=>l.textContent.replace(/\\s+/g,' ').trim());"));
    }

    // ✅ Labels of the radio chips of one group (pr_call_status / pr_received_action / pr_call_with)
    public List<String> getChipLabels(String radioName) {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " input[name=\"'+arguments[0]+'\"]')].map(i=>i.closest('label').textContent.trim());", radioName));
    }

    public boolean isOutcomeShown() {
        return Boolean.TRUE.equals(run("var w=document.querySelector('" + POPUP + " .pr_outcome_wrap'); return !!(w && w.offsetParent);"));
    }

    // ✅ Clicks the chip (real click, so the page's change handlers run)
    public void chooseRadio(String radioName, String value) {
        WebElement r = driver.findElement(By.cssSelector(POPUP + " input[name='" + radioName + "'][value='" + value + "']"));
        WebElement chip = r.findElement(By.xpath("./ancestor::label[1]"));
        run("arguments[0].scrollIntoView({block:'center'});", chip);
        chip.click();
        sleep(300);
    }

    public void setRemarks(String text) {
        WebElement t = driver.findElement(By.cssSelector(POPUP + " #pr_reason_notes"));
        t.clear();
        t.sendKeys(text);
    }

    public String getRemarksPlaceholder() {
        return str("return document.querySelector('" + POPUP + " #pr_reason_notes').placeholder;");
    }

    public void clickSave() {
        WebElement b = driver.findElement(By.cssSelector(POPUP + " #pr_submit_btn"));
        run("arguments[0].scrollIntoView({block:'center'});", b);
        b.click();
    }

    public void clickCancel() {
        run("var b=[...document.querySelectorAll('" + POPUP + " .clx-sheet-foot .close-popdown')].find(x=>x.textContent.trim()==='Cancel'); if(b) b.click();");
        sleep(1000);
    }

    public List<String> getShownErrors() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " #pr_call_status_error, " + POPUP + " #pr_outcome_error, " + POPUP
                + " #pr_call_with_error')].filter(e=>!e.classList.contains('hide') && e.offsetParent).map(e=>e.textContent.trim());"));
    }

    // ✅ flash_message renders into .update-message-notes (or a sibling .flash-msg)
    public String getLogCallMessage() {
        return str("var p=document.querySelector('" + POPUP + "'); if(!p) return '';"
                + "var e=p.querySelector('.update-message-notes'); var t=e?e.textContent.trim():'';"
                + "if(!t){ var f=p.querySelector('.flash-msg, .alert'); t=f?f.textContent.trim():''; } return t.replace(/\\s+/g,' ');");
    }

    public String waitForLogCallMessage(int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            try {
                String m = getLogCallMessage();
                if (!m.isEmpty()) return m;
            } catch (Exception ignored) {
            }
            sleep(200);
        }
        return "";
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
