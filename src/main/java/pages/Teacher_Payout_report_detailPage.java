package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// TEACHER PAYOUT REPORT — /teacher_payout_report_details
// Direct URL only: not linked in any menu yet (2026-11-02), so the
// page sits in the root "pages" package. Selectors confirmed live
// (2026-11-02, Rakesh, see CLAUDE.md "Requirements — Teacher Payout Report").
//
// Report data comes from api/teacherattendancetrends/teacherAttendanceTrendsReport/.
// Teacher, Room and Threshold apply instantly (client side, URL is
// rewritten); Centre, Date Range, Saturdays and Not marked days reload.
// ⚠ The UAT "API Debug (Test)" panel (#api-debug-panel) sits over the
//   lower page and intercepts clicks — hideApiDebugPanel() before clicking.
// ═══════════════════════════════════════════════════════════════
public class Teacher_Payout_report_detailPage {

    WebDriver driver;

    // ═══════════════════════════════════════════════
    // FILTER FORM (#frm_tpr, method GET)
    // ═══════════════════════════════════════════════
    @FindBy(id = "filter_center_id")
    private WebElement centreDropdown;

    @FindBy(id = "date_filter")
    private WebElement dateRangeDropdown;

    // ✅ pickadate, readonly — shown only for "Custom Date Range"
    @FindBy(id = "date_from")
    private WebElement dateFromField;

    @FindBy(id = "date_to")
    private WebElement dateToField;

    @FindBy(id = "fTeacher")
    private WebElement teacherDropdown;

    @FindBy(id = "fRoom")
    private WebElement roomDropdown;

    @FindBy(id = "btn_tpr_submit")
    private WebElement goBtn;

    // ✅ SETTINGS row: number 1–100 (step 0.1), applies instantly
    @FindBy(id = "fThr")
    private WebElement thresholdField;

    // ✅ js-autosubmit: [off|Not a working day, on|Working day]
    @FindBy(css = "#frm_tpr select[name='saturday']")
    private WebElement saturdaysDropdown;

    // ✅ js-autosubmit: [absent|Count as absent, skip|Leave out (not an absence)]
    @FindBy(css = "#frm_tpr select[name='not_marked']")
    private WebElement notMarkedDropdown;

    // ═══════════════════════════════════════════════
    // CHILDREN TAB
    // ═══════════════════════════════════════════════
    @FindBy(id = "fSearch")
    private WebElement childSearchField;

    // ✅ [pa|Presence % low to high, pd|… high to low, n|Child A to Z, t|Teacher]
    @FindBy(id = "fSort")
    private WebElement childSortDropdown;

    @FindBy(id = "bCsv")
    private WebElement downloadCsvBtn;

    public Teacher_Payout_report_detailPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    // ═══════════════════════════════════════════════
    // GENERAL
    // ═══════════════════════════════════════════════
    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    private void click(WebElement el) throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        Thread.sleep(300);
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            js().executeScript("arguments[0].click();", el);
        }
    }

    // ✅ Waits for a reload (Go / autosubmit) to finish: KPI tiles or an error / prompt shown
    public void waitForReport() throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            Object ready = js().executeScript("return document.readyState==='complete' && "
                    + "(document.querySelector('#kpis .tpr-tile')!==null || document.querySelector('.alert-danger')!==null"
                    + " || document.body.innerText.indexOf('then press Go')>=0);");
            if (Boolean.TRUE.equals(ready)) break;
            Thread.sleep(500);
        }
        Thread.sleep(800);
        hideApiDebugPanel();
    }

    public String getHeading() {
        return str(js().executeScript("var h=document.querySelector('#frm_tpr .panel-title');return h?h.innerText.split('\\n')[0].trim():'';"));
    }

    // ✅ "Presence per child and per teacher for the 85% attendance bonus, <centre>, <from> to <to>, <N> children"
    public String getSubtitle() {
        return str(js().executeScript("var c=document.getElementById('cov'); var p=c?c.parentElement:null;"
                + "return p?p.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public boolean isReportShown() {
        return !driver.findElements(By.cssSelector("#kpis .tpr-tile")).isEmpty();
    }

    // ✅ e.g. "The report could not be loaded. From date cannot be after To date."
    public String getErrorAlertText() {
        return str(js().executeScript("var e=document.querySelector('.alert.alert-danger');"
                + "return e&&e.offsetParent!==null?e.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public boolean isTextOnPage(String text) {
        return str(js().executeScript("return document.body.innerText;")).contains(text);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // ✅ Inline filter message, e.g. "Select both From and To dates for a custom date range."
    public String getFilterErrorText() {
        return str(js().executeScript("var e=[].filter.call(document.querySelectorAll('.filter-error'),function(x){return x.offsetParent!==null;})[0];"
                + "return e?e.innerText.trim():'';"));
    }

    public void clearCustomDates() throws InterruptedException {
        js().executeScript("jQuery('#date_from').pickadate('picker').clear(); jQuery('#date_to').pickadate('picker').clear();");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════
    private List<String> optionTexts(WebElement select) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(select).getOptions()) out.add(o.getText().trim());
        return out;
    }

    public List<String> getCentreOptions() { return optionTexts(centreDropdown); }

    public List<String> getDateRangeOptions() { return optionTexts(dateRangeDropdown); }

    public List<String> getTeacherOptions() { return optionTexts(teacherDropdown); }

    public List<String> getRoomOptions() { return optionTexts(roomDropdown); }

    public List<String> getSaturdaysOptions() { return optionTexts(saturdaysDropdown); }

    public List<String> getNotMarkedOptions() { return optionTexts(notMarkedDropdown); }

    public String getSelectedCentre() { return new Select(centreDropdown).getFirstSelectedOption().getText().trim(); }

    public String getSelectedDateRange() { return new Select(dateRangeDropdown).getFirstSelectedOption().getText().trim(); }

    public String getSelectedTeacher() { return new Select(teacherDropdown).getFirstSelectedOption().getText().trim(); }

    public String getSelectedRoom() { return new Select(roomDropdown).getFirstSelectedOption().getText().trim(); }

    public String getSelectedSaturdays() { return new Select(saturdaysDropdown).getFirstSelectedOption().getText().trim(); }

    public String getSelectedNotMarked() { return new Select(notMarkedDropdown).getFirstSelectedOption().getText().trim(); }

    public void selectCentre(String centre) {
        new Select(centreDropdown).selectByVisibleText(centre);
    }

    public void selectNoCentre() {
        new Select(centreDropdown).selectByIndex(0);
    }

    // ✅ Option text starts with the range name, e.g. "Last Month" → "Last Month (Oct-2026)"
    public void selectDateRange(String rangeName) {
        Select s = new Select(dateRangeDropdown);
        for (WebElement o : s.getOptions())
            if (o.getText().trim().startsWith(rangeName)) {
                s.selectByVisibleText(o.getText().trim());
                return;
            }
        throw new IllegalArgumentException("Date Range option not found: " + rangeName);
    }

    public boolean isCustomDateFieldsVisible() {
        return dateFromField.isDisplayed() && dateToField.isDisplayed();
    }

    public String getDateFromValue() { return dateFromField.getAttribute("value"); }

    public String getDateToValue() { return dateToField.getAttribute("value"); }

    // ✅ Sets From / To through pickadate's own API (yyyy-MM-dd) — the inputs are readonly
    public void setCustomDates(String fromIso, String toIso) throws InterruptedException {
        js().executeScript("function d(s){var p=s.split('-');return new Date(+p[0],+p[1]-1,+p[2]);}"
                + "jQuery('#date_from').pickadate('picker').set('select', d(arguments[0]));"
                + "jQuery('#date_to').pickadate('picker').set('select', d(arguments[1]));", fromIso, toIso);
        Thread.sleep(500);
    }

    public void clickGo() throws InterruptedException {
        click(goBtn);
        waitForReport();
    }

    // ✅ Instant filter (no reload)
    public void selectTeacher(String teacher) throws InterruptedException {
        new Select(teacherDropdown).selectByVisibleText(teacher);
        Thread.sleep(2000);
    }

    // ✅ Instant filter (no reload)
    public void selectRoom(String room) throws InterruptedException {
        new Select(roomDropdown).selectByVisibleText(room);
        Thread.sleep(2000);
    }

    // ✅ Instant: sets the value and fires input/change like typing does
    public void setThreshold(String value) throws InterruptedException {
        js().executeScript("var e=document.getElementById('fThr'); e.value=arguments[0];"
                + "e.dispatchEvent(new Event('input',{bubbles:true})); e.dispatchEvent(new Event('change',{bubbles:true}));", value);
        Thread.sleep(2000);
    }

    public String getThresholdValue() { return thresholdField.getAttribute("value"); }

    // ✅ Red text under SETTINGS, e.g. "Threshold must be a number between 1 and 100."
    public String getThresholdValidationText() {
        return str(js().executeScript("var x=document.evaluate(\"//*[contains(text(),'Threshold must be a number')]\",document,null,9,null).singleNodeValue;"
                + "return x&&x.offsetParent!==null?x.innerText.trim():'';"));
    }

    // ✅ js-autosubmit → page reloads
    public void selectSaturdays(String option) throws InterruptedException {
        new Select(saturdaysDropdown).selectByVisibleText(option);
        Thread.sleep(1500);
        waitForReport();
    }

    // ✅ js-autosubmit → page reloads
    public void selectNotMarked(String option) throws InterruptedException {
        new Select(notMarkedDropdown).selectByVisibleText(option);
        Thread.sleep(1500);
        waitForReport();
    }

    public String getSettingsInfoText() {
        return str(js().executeScript("var x=document.evaluate(\"//*[contains(text(),'apply instantly')]\",document,null,9,null).singleNodeValue;"
                + "return x?x.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    // ═══════════════════════════════════════════════
    // TABS (Report | Charts | Children <N> | How it is calculated)
    // ═══════════════════════════════════════════════
    public List<String> getTabNames() {
        List<String> out = new ArrayList<>();
        for (WebElement a : driver.findElements(By.cssSelector("ul.nav-tabs a[data-toggle='tab']")))
            out.add(a.getAttribute("textContent").replaceAll("\\s+", " ").trim());
        return out;
    }

    // ✅ paneId: p-report | p-charts | p-children | p-how
    public void openTab(String paneId) throws InterruptedException {
        click(driver.findElement(By.cssSelector("ul.nav-tabs a[href='#" + paneId + "']")));
        Thread.sleep(1500);
    }

    public boolean isTabActive(String paneId) {
        return str(driver.findElement(By.id(paneId)).getAttribute("class")).contains("active");
    }

    public String getChildrenTabBadge() {
        return str(js().executeScript("return document.getElementById('kidsCount').textContent.trim();"));
    }

    public void clickHowItIsCalculatedLink() throws InterruptedException {
        click(driver.findElement(By.cssSelector(".tpr-formula-line a.js-open-how")));
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // REPORT TAB — notice, KPI tiles, working days, holidays
    // ═══════════════════════════════════════════════
    public String getNoticeText() {
        return str(js().executeScript("var n=document.getElementById('notice');return n?n.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    // ✅ Tile labels: "Children in the period", "Qualifying for payout", "Below 85%", "Unexcused absent days"
    @SuppressWarnings("unchecked")
    public Map<String, String[]> getKpiTiles() {
        List<List<String>> rows = (List<List<String>>) js().executeScript(
                "return [].map.call(document.querySelectorAll('#kpis .tpr-tile'),function(t){"
                        + "return [t.querySelector('.tpr-tile-label').textContent.trim(), t.querySelector('.tpr-tile-value').textContent.trim(),"
                        + " t.querySelector('.tpr-tile-sub').textContent.replace(/\\s+/g,' ').trim()];});");
        Map<String, String[]> out = new LinkedHashMap<>();
        for (List<String> r : rows) out.put(r.get(0), new String[]{r.get(1), r.get(2)});
        return out;
    }

    public int getKpiValue(String labelStart) {
        for (Map.Entry<String, String[]> e : getKpiTiles().entrySet())
            if (e.getKey().toLowerCase().startsWith(labelStart.toLowerCase()))
                return Integer.parseInt(e.getValue()[0].replaceAll("[^0-9]", ""));
        throw new IllegalArgumentException("KPI tile not found: " + labelStart);
    }

    public String getKpiSub(String labelStart) {
        for (Map.Entry<String, String[]> e : getKpiTiles().entrySet())
            if (e.getKey().toLowerCase().startsWith(labelStart.toLowerCase())) return e.getValue()[1];
        throw new IllegalArgumentException("KPI tile not found: " + labelStart);
    }

    public List<String> getKpiLabels() {
        return new ArrayList<>(getKpiTiles().keySet());
    }

    // ✅ "Working days considered": label → number, e.g. {"calendar days (1 Oct – 31 Oct)":31, "Sundays":4,
    //    "Saturdays off":5, "declared holidays":3, "working days":19}
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getWorkingDaysEquation() {
        List<List<String>> parts = (List<List<String>>) js().executeScript(
                "return [].map.call(document.querySelectorAll('#eq .t'),function(t){"
                        + "return [t.querySelector('.l').textContent.trim(), t.querySelector('.n').textContent.trim()];});");
        Map<String, Integer> out = new LinkedHashMap<>();
        for (List<String> p : parts) out.put(p.get(0), Integer.parseInt(p.get(1)));
        return out;
    }

    public int getEquationValue(String labelStart) {
        for (Map.Entry<String, Integer> e : getWorkingDaysEquation().entrySet())
            if (e.getKey().startsWith(labelStart)) return e.getValue();
        throw new IllegalArgumentException("Working-days part not found: " + labelStart);
    }

    // ✅ "Declared holidays: 2 Oct (Gandhi Jayanti), … Taken from the centre's regional holiday list and centre overrides."
    public String getHolidaysLine() {
        return str(js().executeScript("var h=document.getElementById('hol');return h?h.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getFormulaLine() {
        return str(js().executeScript("var f=document.querySelector('.tpr-formula-line');return f?f.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    // ═══════════════════════════════════════════════
    // TEACHER PAYOUT ROLL-UP (#tTeacher)
    // ═══════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    public List<String> getTeacherTableHeaders() {
        return (List<String>) js().executeScript("return [].map.call(document.querySelectorAll('#tTeacher thead th'),function(h){return h.textContent.trim();});");
    }

    // ✅ One map per teacher row: header → cell text (whitespace collapsed).
    //    "Qualify for payout" reads "2 of 11", "Attendance %" reads "50.0% 2 ÷ 4".
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getTeacherRows() {
        List<String> headers = getTeacherTableHeaders();
        List<List<String>> rows = (List<List<String>>) js().executeScript(
                "return [].filter.call(document.querySelectorAll('#tTeacher tbody tr'),function(r){return r.cells.length>1;})"
                        + ".map(function(r){return [].map.call(r.cells,function(c){return c.innerText.replace(/\\s+/g,' ').trim();});});");
        List<Map<String, String>> out = new ArrayList<>();
        for (List<String> r : rows) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < r.size(); i++) m.put(headers.get(i), r.get(i));
            out.add(m);
        }
        return out;
    }

    public String getTeacherTableText() {
        return str(js().executeScript("var t=document.getElementById('tTeacher');return t?t.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    // ✅ Drill-down number buttons: data-m = n (Children) | ok (Qualify) | no (Below) | p (Present) |
    //    a (Absent) | pz (Paused/leave) | nm (Not marked) | nr (No record)
    public void clickTeacherDrill(String teacher, String metric) throws InterruptedException {
        click(driver.findElement(By.cssSelector("#tTeacher button.lnk[data-t='" + teacher + "'][data-m='" + metric + "']")));
        Thread.sleep(1500);
    }

    public boolean isDrillButtonPressed(String teacher, String metric) {
        return "true".equals(driver.findElement(By.cssSelector("#tTeacher button.lnk[data-t='" + teacher + "'][data-m='" + metric + "']"))
                .getAttribute("aria-pressed"));
    }

    // ✅ #drill panel, e.g. "Shobha Badola: Children who qualify for payout … Total, 2 children …"
    public String getDrillText() {
        return str(js().executeScript("var d=document.getElementById('drill');return d?d.innerText.replace(/[ \\t]+/g,' ').trim():'';"));
    }

    public String getDrillTitle() {
        return str(js().executeScript("var d=document.querySelector('#drill .panel-title, #drill h6');return d?d.innerText.replace(/\\s+/g,' ').replace(/Close$/,'').trim():'';"));
    }

    // ✅ Child ids listed in the drill table
    @SuppressWarnings("unchecked")
    public List<String> getDrillChildIds() {
        return (List<String>) js().executeScript("return [].map.call(document.querySelectorAll('#drill tbody tr'),function(r){return r.cells.length>1?r.cells[1].innerText.trim():'';})"
                + ".filter(function(v){return /^\\d+$/.test(v);});");
    }

    // ✅ Selecting a child row in the drill list opens that child's day-by-day detail below it
    public String clickDrillChildRow(String childId) throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("var id=arguments[0]; var r=[].filter.call(document.querySelectorAll('#drill tbody tr'),function(r){return r.cells.length>1&&r.cells[1].innerText.trim()===id;})[0];"
                + "r.scrollIntoView({block:'center'}); r.click();", childId);
        Thread.sleep(1500);
        return str(js().executeScript("var id=arguments[0]; var r=[].filter.call(document.querySelectorAll('#drill tbody tr'),function(r){return r.cells.length>1&&r.cells[1].innerText.trim()===id;})[0];"
                + "var n=r?r.nextElementSibling:null; return n&&!(n.cells.length>1&&/^\\d+$/.test(n.cells[1].innerText.trim()))?n.innerText.replace(/[ \\t]+/g,' ').trim():'';", childId));
    }

    public void closeDrill() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("var b=[].filter.call(document.querySelectorAll('#drill button, #drill a'),function(b){return b.textContent.trim()==='Close';})[0]; if(b) b.click();");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // ATTRITION REASONS BY TEACHER (#tMatrix)
    // ═══════════════════════════════════════════════
    public String getAttritionMatrixText() {
        return str(js().executeScript("var t=document.getElementById('tMatrix');return t?t.innerText.replace(/[ \\t]+/g,' ').trim():'';"));
    }

    // ✅ Header row: Reason | <teachers…> | Total (empty list when "No attrition in this period.")
    @SuppressWarnings("unchecked")
    public List<String> getAttritionMatrixHeaders() {
        return (List<String>) js().executeScript("return [].map.call(document.querySelectorAll('#tMatrix thead th'),function(h){return h.textContent.trim();});");
    }

    // ✅ Total of the matrix's "Total" row / "Total" column (bottom-right cell)
    public int getAttritionGrandTotal() {
        Object v = js().executeScript("var r=document.querySelectorAll('#tMatrix tbody tr'); if(!r.length) return '0'; var last=r[r.length-1];"
                + "return last.cells[last.cells.length-1].innerText.trim();");
        String s = str(v).replaceAll("[^0-9]", "");
        return s.isEmpty() ? 0 : Integer.parseInt(s);
    }

    public void clickFirstAttritionCount() throws InterruptedException {
        click(driver.findElement(By.cssSelector("#tMatrix button.lnk[data-m='left']")));
        Thread.sleep(1500);
    }

    public String getFirstAttritionButtonInfo() {
        WebElement b = driver.findElement(By.cssSelector("#tMatrix button.lnk[data-m='left']"));
        return b.getAttribute("data-t") + " | " + b.getAttribute("data-r");
    }

    // ═══════════════════════════════════════════════
    // CHARTS TAB
    // ═══════════════════════════════════════════════
    public String getChartPanelTitles() {
        return str(js().executeScript("return [].map.call(document.querySelectorAll('#p-charts .panel-title'),function(h){return h.innerText.replace(/\\s+/g,' ').trim();}).join(' | ');"));
    }

    // ✅ chTrend | chTeacher | chKids — rendered when the chart div holds an svg/canvas with a height
    public boolean isChartRendered(String chartId) {
        return Boolean.TRUE.equals(js().executeScript("var e=document.getElementById(arguments[0]);"
                + "return !!e && e.querySelectorAll('svg,canvas').length>0 && e.offsetHeight>0;", chartId));
    }

    public String getChartText(String chartId) {
        return str(js().executeScript("var e=document.getElementById(arguments[0]);return e?e.textContent.replace(/\\s+/g,' ').trim():'';", chartId));
    }

    // ═══════════════════════════════════════════════
    // CHILDREN TAB (#tKids)
    // ═══════════════════════════════════════════════
    @SuppressWarnings("unchecked")
    public List<String> getChildTableHeaders() {
        return (List<String>) js().executeScript("return [].map.call(document.querySelectorAll('#tKids thead th'),function(h){return h.textContent.trim();});");
    }

    // ✅ Child rows not hidden by search / filters: header → cell text, read from textContent
    //    (the Result cell is CSS-uppercased on screen: "MEETS 85%"). The "Child" cell is
    //    "<name> <tag>" (tag: Full period | Joined <d Mon> | Left <d Mon>), "Presence %" is "100.0% 1 ÷ 1" or "–".
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getVisibleChildRows() {
        List<String> headers = getChildTableHeaders();
        List<List<String>> rows = (List<List<String>>) js().executeScript(
                "function t(c){return [].map.call(c.childNodes,function(x){return x.textContent;}).join(' ').replace(/\\s+/g,' ').trim();}"
                        + "return [].filter.call(document.querySelectorAll('#tKids tr.krow'),function(r){return r.style.display!=='none';})"
                        + ".map(function(r){return [].map.call(r.cells,t);});");
        List<Map<String, String>> out = new ArrayList<>();
        for (List<String> r : rows) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < r.size(); i++) m.put(headers.get(i), r.get(i));
            out.add(m);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<String> getVisibleChildIds() {
        return (List<String>) js().executeScript("return [].filter.call(document.querySelectorAll('#tKids tr.krow'),function(r){return r.style.display!=='none';})"
                + ".map(function(r){return r.dataset.id;});");
    }

    // ✅ Text of the table body (e.g. "No children match the selected filters.")
    public String getChildTableBodyText() {
        return str(js().executeScript("return document.querySelector('#tKids tbody').innerText.replace(/\\s+/g,' ').trim();"));
    }

    public void searchChildren(String text) throws InterruptedException {
        childSearchField.clear();
        if (!text.isEmpty()) childSearchField.sendKeys(text);
        js().executeScript("arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", childSearchField);
        Thread.sleep(1200);
    }

    public List<String> getSortOptions() { return optionTexts(childSortDropdown); }

    public void sortChildren(String option) throws InterruptedException {
        new Select(childSortDropdown).selectByVisibleText(option);
        Thread.sleep(1200);
    }

    // ✅ Clicking a row (not the Child ID link) opens a day-by-day detail row right below it
    public void clickChildRow(String childId) throws InterruptedException {
        click(driver.findElement(By.cssSelector("#tKids tr.krow[data-id='" + childId + "'] td:nth-child(3)")));
        Thread.sleep(1500);
    }

    public String getChildDetailText(String childId) {
        return str(js().executeScript("var r=document.querySelector(\"#tKids tr.krow[data-id='\"+arguments[0]+\"']\");"
                + "var n=r?r.nextElementSibling:null; return n&&!n.classList.contains('krow')?n.innerText.replace(/[ \\t]+/g,' ').trim():'';", childId));
    }

    // ✅ Child ID link → pop_child_info popup ("Child Information")
    public void clickChildIdLink(String childId) throws InterruptedException {
        click(driver.findElement(By.cssSelector("#tKids a.tpr-child-link[href*='child_id=" + childId + "']")));
        Thread.sleep(3000);
    }

    public String getChildInfoPopupText() {
        return str(js().executeScript("var m=document.querySelector('.modal.in, .modal.show, #popdown-dialog');"
                + "return m&&m.offsetWidth>0?m.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public void closePopups() throws InterruptedException {
        js().executeScript("document.querySelectorAll('.modal.in [data-dismiss=modal], #popdown-dialog .close').forEach(function(b){b.click();});"
                + "var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';");
        Thread.sleep(1000);
    }

    // ✅ Result label class per result text, e.g. "Meets 85%" → "label label-success"
    @SuppressWarnings("unchecked")
    public Map<String, String> getResultLabelClasses() {
        List<List<String>> rows = (List<List<String>>) js().executeScript("return [].map.call(document.querySelectorAll('#tKids tr.krow'),function(r){"
                + "var s=r.cells[r.cells.length-1].querySelector('.label'); return s?[s.textContent.trim().replace(/[\\d.]+%$/,'X%'), s.className]:['',''];});");
        Map<String, String> out = new LinkedHashMap<>();
        for (List<String> r : rows) out.put(r.get(0), r.get(1));
        return out;
    }

    public void clickDownloadCsv() throws InterruptedException {
        click(downloadCsvBtn);
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // HOW IT IS CALCULATED TAB
    // ═══════════════════════════════════════════════
    public String getHowTabText() {
        return str(js().executeScript("return document.getElementById('p-how').innerText.replace(/[ \\t]+/g,' ');"));
    }

    // ✅ "If a child joins in the middle of the period" calculator: Absent days #wAbs, Paused days #wPz → #wOut
    public void setCalculatorInputs(String absentDays, String pausedDays) throws InterruptedException {
        js().executeScript("function s(id,v){var e=document.getElementById(id); e.value=v;"
                + " e.dispatchEvent(new Event('input',{bubbles:true})); e.dispatchEvent(new Event('change',{bubbles:true}));}"
                + "s('wAbs',arguments[0]); s('wPz',arguments[1]);", absentDays, pausedDays);
        Thread.sleep(800);
    }

    public String getCalculatorJoiningDate() {
        return str(js().executeScript("return document.getElementById('wJoin').value;"));
    }

    // ✅ Result lines: label → value, e.g. "Working days in window"→"19", "Presence %"→"17 ÷ 19 = 89.5%"
    @SuppressWarnings("unchecked")
    public Map<String, String> getCalculatorResult() {
        String text = str(js().executeScript("return document.getElementById('wOut').innerText;"));
        Map<String, String> out = new LinkedHashMap<>();
        String[] lines = text.split("\\n");
        for (int i = 0; i + 1 < lines.length; i += 2) out.put(lines[i].trim(), lines[i + 1].trim());
        return out;
    }

    // ✅ "Worked examples from this centre" block
    public String getWorkedExamplesText() {
        return str(js().executeScript("var e=document.getElementById('examples');return e?e.innerText.replace(/[ \\t]+/g,' ').trim():'';"));
    }
}
