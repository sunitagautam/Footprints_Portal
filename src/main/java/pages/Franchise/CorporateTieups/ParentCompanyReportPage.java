package pages.Franchise.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Parent Company Report (/existing_children_parent_company_report),
 * right View_Corporate_Admission_Count. Heading "Existing Children Parent Company Report".
 * Filter form #frm_parent_company_report: City (city_filter_list[]) and Center (#center_selection) multiselects + Filter
 * (#btn_submit). Table #parent_company_report_table: Company Name | Counts | Action (eye → popup
 * pop_existing_company_children "Active Children Details (<company>)" listing the active children).
 */
public class ParentCompanyReportPage {

    WebDriver driver;

    private static final String FILTER = "#frm_parent_company_report";
    private static final String TABLE = "#parent_company_report_table";
    private static final String POPUP = "#popdown-dialog";
    public static final String CITY = "select[name='city_filter_list[]']";
    public static final String CENTER = "#center_selection";

    public ParentCompanyReportPage(WebDriver driver) {
        this.driver = driver;
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
            new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && jQuery.fn.dataTable.isDataTable('" + TABLE + "'));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getHeading() {
        return String.valueOf(js().executeScript("var h=[].filter.call(document.querySelectorAll('h1,h2,h3,h4,h5'),function(h){return h.offsetWidth>0 && /Parent Company/.test(h.textContent);})[0];"
                + "return h?h.textContent.trim():'';"));
    }

    // ════════════════ Filters ════════════════
    public boolean isFilterButtonVisible() { return isShown(By.cssSelector(FILTER + " #btn_submit")); }

    public String getMultiselectButtonText(String selectCss) {
        return String.valueOf(js().executeScript("return jQuery(arguments[0]).next('.btn-group').find('button.multiselect').text().trim();", selectCss));
    }

    public List<String> getMultiselectOptions(String selectCss) {
        return strings(js().executeScript("return jQuery(arguments[0]).find('option').map(function(){return jQuery(this).text().trim();}).get();", selectCss));
    }

    // ✅ Selects only this option (without the page's onChange, which calls the missing $.uniform)
    public void multiselectOnly(String selectCss, String optionText) {
        Object ok = js().executeScript("var s=jQuery(arguments[0]), t=arguments[1];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.multiselect('deselectAll', false); s.multiselect('select', o.first().val()); s.multiselect('refresh'); return true;", selectCss, optionText);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("'" + optionText + "' not in " + selectCss);
    }

    public List<String> getMultiselectSelected(String selectCss) {
        return strings(js().executeScript("return jQuery(arguments[0]).find('option:selected').map(function(){return jQuery(this).text().trim();}).get();", selectCss));
    }

    // ✅ Centre names listed under a city's optgroup in the Center dropdown
    public List<String> getCentersOfCity(String city) {
        return strings(js().executeScript("var C=arguments[0];"
                + "return jQuery('" + CENTER + " optgroup').filter(function(){return jQuery(this).attr('label')===C;})"
                + ".find('option').map(function(){return jQuery(this).text().trim();}).get();", city));
    }

    public void clickFilter() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        System.out.println("▶ Filter clicked");
        Thread.sleep(2500);
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Table ════════════════
    public List<String> getColumnHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + TABLE + " thead th'), function(h){return h.textContent.trim();});"));
    }

    // ✅ Rows as "company || count"
    public List<String> getRows() {
        return strings(js().executeScript("var t=function(v){return jQuery('<div>').html(v).text().replace(/\\s+/g,' ').trim();};"
                + "return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).data().toArray().map(function(d){return t(d[0])+' || '+t(d[1]);});"));
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('" + TABLE + "_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public String getSearchPlaceholder() {
        return String.valueOf(js().executeScript("var i=document.querySelector('" + TABLE + "_filter input');return i?i.placeholder:'';"));
    }

    public String getDownloadButtonText() {
        return String.valueOf(js().executeScript("var b=document.querySelector('.dt-buttons [aria-controls=\"parent_company_report_table\"]');return b?b.textContent.trim():'';"));
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(TABLE + "_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1000);
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('.dt-buttons [aria-controls=\"parent_company_report_table\"]').click();");
        System.out.println("▶ Download clicked");
        Thread.sleep(1000);
    }

    public String getActionHref(String company) {
        return String.valueOf(js().executeScript("var C=arguments[0], out='';"
                + "jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().forEach(function(r){if(r.children[0].textContent.trim()===C){var a=r.querySelector('a'); out=a?a.getAttribute('href')+' | '+a.getAttribute('title'):'';}});"
                + "return out;", company));
    }

    // ✅ Hovers the Counts cell of a company → visible tooltip / popover text ("" when none) + any title attribute
    public String getCountsTooltip(String company) throws InterruptedException {
        WebElement cell = (WebElement) js().executeScript("var C=arguments[0], c=null;"
                + "jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().forEach(function(r){if(r.children[0].textContent.trim()===C) c=r.children[1];}); return c;", company);
        new Actions(driver).moveToElement(cell).perform();
        Thread.sleep(1200);
        return String.valueOf(js().executeScript("var t=[].filter.call(document.querySelectorAll('.tooltip, .popover, [role=tooltip]'),function(e){return e.offsetWidth>0;})"
                + ".map(function(e){return e.innerText.replace(/\\s+/g,' ').trim();}).join(' | ');"
                + "var a=arguments[0]; var titles=[a.getAttribute('title'),a.getAttribute('data-original-title')].concat([].map.call(a.querySelectorAll('[title],[data-original-title]'),function(x){return x.getAttribute('title')||x.getAttribute('data-original-title');})).filter(Boolean);"
                + "return (t+' '+titles.join(' ')).trim();", cell));
    }

    public void openChildren(String company) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = js().executeScript("var C=arguments[0], a=null;"
                + "jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().forEach(function(r){if(r.children[0].textContent.trim()===C) a=r.querySelector('a');});"
                + "if(!a) return false; a.click(); return true;", company);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No eye icon for " + company);
        System.out.println("▶ Active Children Details opened (" + company + ")");
        for (int i = 0; i < 90; i++) {
            Object ready = js().executeScript("var t=document.querySelector('" + POPUP + " table'); return !!(t && window.jQuery && jQuery.fn.dataTable.isDataTable(t));");
            if (Boolean.TRUE.equals(ready)) break;
            Thread.sleep(500);
        }
        Thread.sleep(800);
    }

    // ════════════════ Active Children Details popup ════════════════
    public boolean isPopupVisible() { return isShown(By.cssSelector(POPUP + " table")); }

    public String getPopupTitle() {
        return String.valueOf(js().executeScript("var p=document.querySelector('" + POPUP + "');if(!p) return '';"
                + "var h=p.querySelector('.modal-title, h4, h5, h3, .panel-title'); return h?h.textContent.replace(/\\s+/g,' ').trim():p.innerText.split('\\n')[0].trim();"));
    }

    public List<String> getPopupHeaders() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + POPUP + " table thead th'), function(h){return h.textContent.trim();});"));
    }

    public int getPopupRowCount() {
        return ((Number) js().executeScript("return jQuery(document.querySelector('" + POPUP + " table')).DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public List<String> getPopupColumn(String header) {
        return strings(js().executeScript("var t=document.querySelector('" + POPUP + " table'), H=[].map.call(t.querySelectorAll('thead th'),function(h){return h.textContent.trim();});"
                + "var i=H.indexOf(arguments[0]); if(i<0) return [];"
                + "return jQuery(t).DataTable().column(i,{search:'applied'}).data().toArray().map(function(v){return jQuery('<div>').html(v).text().replace(/\\s+/g,' ').trim();});", header));
    }

    public int getPopupPageLength() {
        return ((Number) js().executeScript("return jQuery(document.querySelector('" + POPUP + " table')).DataTable().page.len();")).intValue();
    }

    public String getPopupInfo() {
        return String.valueOf(js().executeScript("var i=document.querySelector('" + POPUP + " .dataTables_info');return i?i.textContent.replace(/\\s+/g,' ').trim():'';"));
    }

    public boolean isPopupSearchVisible() { return isShown(By.cssSelector(POPUP + " .dataTables_filter input")); }

    public boolean isPopupDownloadVisible() { return isShown(By.cssSelector(POPUP + " .dt-buttons .dt-button, " + POPUP + " a[href*=download], " + POPUP + " button[id*=download]")); }

    public void searchPopup(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(POPUP + " .dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1500);
    }

    public String getPopupEmptyMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + POPUP + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public void clickPopupNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('" + POPUP + " .paginate_button.next').click();");
        Thread.sleep(800);
    }
}
