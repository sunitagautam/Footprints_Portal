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
 * Children → Updates → Attendance Retention (/attendance_retention_call_list), SDA-13172. Confirmed live 2026-11-03
 * as Rakesh.
 * <p>
 * Filter form (GET, form.ar-filters): Status select[name=filter] [due_today (default), upcoming, escalated, closed],
 * Centre select#center_filter_list (select2), From / To input[name=from_date|to_date] (pickadate, empty by default),
 * Apply = button.ar-go. A header badge reads "N DUE TODAY" / "N UPCOMING" / "N ESCALATED" / "N CLOSED".
 * <p>
 * Table table.attendance_retention_data (DataTable): Child (name link) | Centre | Step / Attempt ("Attempt 2 / 1",
 * "Escalation / 3") | Status pill | Trigger | Next Due | Action (History a[href*=pop_attendance_retention_call_history],
 * Log Call button — disabled with title "Not due until yyyy-MM-dd" before the due date).
 * <p>
 * History popup (#popdown-dialog .ar-hist): "Call History — <Child> (#id)", table # | Outcome | Action | Spoke With |
 * Notes | Logged At; inline .ar_bf_note + .ar_bf_save only on "No CD Action" rows; "Support Notes" section only when
 * Support has added notes.
 */
public class AttendanceRetentionPage {

    WebDriver driver;
    WebDriverWait wait;

    public static final String TABLE = "table.attendance_retention_data";
    private static final String FORM = "form.ar-filters";
    private static final String POPUP = "#popdown-dialog";

    public AttendanceRetentionPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
    }

    private Object run(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
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

    public void hideApiDebugPanel() {
        run("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    // ════════════════════════════════════════════════════════
    //  LIST
    // ════════════════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "var t=document.querySelector('" + TABLE + "');"
                            + "return !!(t && window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable(t));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMenuPath() {
        return str("var m=window.menuData||[]; for(var i=0;i<m.length;i++){ if(m[i].url==='attendance_retention_call_list')"
                + " return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';");
    }

    public String getHeading() {
        return str("var h=[...document.querySelectorAll('h5,h4,h3,h2')].find(e=>/Attendance Retention/i.test(e.textContent)); return h?h.textContent.replace(/\\s+/g,' '):'';");
    }

    // ✅ e.g. "1 UPCOMING" (textContent; the page may render it in CSS uppercase)
    public String getBadgeText() {
        return str("var w=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT); var n;"
                + "while(n=w.nextNode()){ var t=n.parentElement.closest('.ar-badge,.badge,.label,span,div'); var s=(t?t.textContent:n.textContent).replace(/\\s+/g,' ').trim();"
                + " if(/^\\d+\\s+(due today|upcoming|escalated|closed)$/i.test(s)) return s; } return '';");
    }

    public List<String> getStatusOptions() {
        return strings(run("return [...document.querySelectorAll('" + FORM + " select[name=filter] option')].map(o=>o.value+'='+o.text.trim());"));
    }

    public String getSelectedStatus() {
        return str("return document.querySelector('" + FORM + " select[name=filter]').value;");
    }

    public String getFieldValue(String name) {
        return str("var e=document.querySelector('" + FORM + " [name=\"'+arguments[0]+'\"]'); return e?e.value:'';", name);
    }

    public List<String> getColumnHeaders() {
        return strings(run("return [...document.querySelectorAll('" + TABLE + " thead th')].map(h=>h.textContent.replace(/\\s+/g,' ').trim());"));
    }

    public String getInfoText() {
        return str("var t=document.querySelector('" + TABLE + "'); var i=t?document.getElementById(t.id+'_info'):null; return i?i.textContent.replace(/\\s+/g,' '):'';");
    }

    public String getEmptyText() {
        return str("var e=document.querySelector('" + TABLE + " td.dataTables_empty'); return e?e.textContent.trim():'';");
    }

    /**
     * ✅ All rows (current search applied): child, childId, centre, stepAttempt, status, trigger, nextDue, actions,
     * historyTaskId, logCallDisabled, logCallTitle, logCallHtml.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getRows() {
        Object o = run("var dt=jQuery('" + TABLE + "').DataTable(); var out=[];"
                + "dt.rows({search:'applied'}).nodes().toArray().forEach(function(r){ var td=r.querySelectorAll('td'); if(td.length<7) return;"
                + " var t=function(i){return td[i].textContent.replace(/\\s+/g,' ').trim();};"
                + " var a=td[0].querySelector('a'); var h=td[6].querySelector('a[href*=pop_attendance_retention_call_history]');"
                + " var l=[...td[6].querySelectorAll('a,button')].find(e=>/log call/i.test(e.textContent));"
                + " out.push({child:a?a.textContent.trim():t(0), childId:a?((a.getAttribute('href')||'').match(/child_id=(\\d+)/)||['',''])[1]:'',"
                + "  centre:t(1), stepAttempt:t(2), status:t(3), trigger:t(4), nextDue:t(5),"
                + "  actions:[...td[6].querySelectorAll('a,button')].map(e=>(e.getAttribute('title')||'')+':'+e.textContent.trim()).join(' + '),"
                + "  historyTaskId:h?(h.getAttribute('href').match(/task_id=(\\d+)/)||['',''])[1]:'',"
                + "  logCallDisabled:l?String(!!(l.disabled||l.classList.contains('disabled'))):'',"
                + "  logCallTitle:l?(l.getAttribute('title')||''):'', logCallHtml:l?l.outerHTML.replace(/\\s+/g,' '):''}); });"
                + "return out;");
        List<Map<String, String>> rows = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) {
            Map<String, String> m = new LinkedHashMap<>();
            ((Map<String, Object>) x).forEach((k, v) -> m.put(k, v == null ? "" : String.valueOf(v)));
            rows.add(m);
        }
        return rows;
    }

    // ── Filters ──
    public void selectStatus(String value) {
        run("var s=document.querySelector('" + FORM + " select[name=filter]'); s.value=arguments[0]; s.dispatchEvent(new Event('change',{bubbles:true}));", value);
    }

    public String selectCentreByName(String centre) {
        String v = str("var opts=document.querySelectorAll('" + FORM + " #center_filter_list option');"
                + "for(var i=0;i<opts.length;i++){ if(opts[i].text.trim()===arguments[0]) return opts[i].value; } return '';", centre);
        if (!v.isEmpty()) run("jQuery('" + FORM + " #center_filter_list').val(arguments[0]).trigger('change');", v);
        return v;
    }

    public List<String> getCentreOptions() {
        return strings(run("return [...document.querySelectorAll('" + FORM + " #center_filter_list option')].map(o=>o.text.trim());"));
    }

    // ✅ From / To are pickadate widgets — set through the picker API (yyyy-MM-dd)
    public void setDateRange(String fromIso, String toIso) {
        run("var f=jQuery('" + FORM + " [name=from_date]'), t=jQuery('" + FORM + " [name=to_date]');"
                + "function put(el,v){ var p=el.pickadate('picker'); if(!v){ p.clear(); return; } var d=v.split('-'); p.set('select',[+d[0],+d[1]-1,+d[2]]); }"
                + "put(f,arguments[0]); put(t,arguments[1]);", fromIso, toIso);
    }

    public void clickApply() {
        WebElement b = driver.findElement(By.cssSelector(FORM + " button.ar-go"));
        run("arguments[0].click();", b);
        sleep(2500);
        isPageLoaded();
    }

    public void search(String text) {
        WebElement in = driver.findElement(By.cssSelector(".dataTables_filter input[type=search]"));
        in.clear();
        if (!text.isEmpty()) in.sendKeys(text);
        else run("jQuery('" + TABLE + "').DataTable().search('').draw();");
        sleep(800);
    }

    // ✅ Clicks the row's Log Call button (enabled or not) — returns true if a popup opened
    public boolean clickLogCall(String childId) {
        run("var r=[...jQuery('" + TABLE + "').DataTable().rows().nodes().toArray()].find(r=>r.innerHTML.indexOf('child_id='+arguments[0]+'\"')>=0);"
                + "var b=r?[...r.querySelectorAll('a,button')].find(e=>/log call/i.test(e.textContent)):null; if(b) b.click();", childId);
        sleep(3000);
        return isPopupOpen();
    }

    // ════════════════════════════════════════════════════════
    //  HISTORY POPUP
    // ════════════════════════════════════════════════════════
    public boolean openHistory(String taskId) {
        run("jQuery('" + TABLE + "').DataTable().page.len(-1).draw();");
        WebElement a = driver.findElement(By.cssSelector(TABLE + " a[href*='pop_attendance_retention_call_history'][href$='task_id=" + taskId + "']"));
        run("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", a);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(25)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "var p=document.querySelector('" + POPUP + " .ar-hist'); return !!(p && p.offsetParent);")));
            sleep(800);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPopupOpen() {
        return Boolean.TRUE.equals(run("var p=document.querySelector('" + POPUP + "'); return !!(p && p.offsetParent && p.textContent.trim().length>0);"));
    }

    public String getPopupTitle() {
        return str("var h=document.querySelector('" + POPUP + " .panel-title'); return h?h.textContent.replace(/\\s+/g,' '):'';");
    }

    public List<String> getHistoryHeaders() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " .ar-hist table thead th')].map(h=>h.textContent.trim());"));
    }

    // ✅ "# || outcome || action || spokeWith || notes || loggedAt"
    public List<String> getHistoryRows() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " .ar-hist table tbody tr')].map(r=>"
                + "[...r.querySelectorAll('td')].map(c=>c.textContent.replace(/\\s+/g,' ').trim()).join(' || '));"));
    }

    // ✅ Inputs in the outcome table other than the No-CD-Action backfill box
    public int countEditableHistoryControls() {
        return ((Number) run("return [...document.querySelectorAll('" + POPUP + " .ar-hist table input, " + POPUP + " .ar-hist table textarea, "
                + POPUP + " .ar-hist table select')].filter(e=>!e.classList.contains('ar_bf_note')).length;")).intValue();
    }

    public List<String> getBackfillRowTexts() {
        return strings(run("return [...document.querySelectorAll('" + POPUP + " .ar-hist .ar_bf_note')].map(i=>i.closest('tr').textContent.replace(/\\s+/g,' ').trim());"));
    }

    public boolean hasSupportNotesSection() {
        return Boolean.TRUE.equals(run("var p=document.querySelector('" + POPUP + " .ar-hist'); return !!p && [...p.querySelectorAll('div')].some(d=>d.children.length===0 && d.textContent.trim()==='Support Notes');"));
    }

    public List<String> getSupportNotes() {
        return strings(run("var p=document.querySelector('" + POPUP + " .ar-hist'); if(!p) return [];"
                + "return [...p.querySelectorAll('div')].filter(d=>d.children.length===0 && /^\\[.+·.+\\]/.test(d.textContent.trim())).map(d=>d.textContent.trim());"));
    }

    public void closePopup() {
        run("var b=document.querySelector('" + POPUP + " .close-popdown'); if(b) b.click();");
        sleep(1000);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
