package pages.Franchise.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Tie-up Expiry Migration Admissions (/tieup_expiry_migrations).
 * Heading h2 "Tie-up Expiry Migration Admissions". Filter form #frm_invoice_report (posts back): Company
 * #sr_corporate_company (select2, "--All Companies--"), Filter #btn_submit, Clear link (a.clx-btn-link → tieup_expiry_migrations).
 * Table .dt-admission-invite (#DataTables_Table_0, 20 per page, lengths 10/20/50/100, order Offer Title desc; rows loaded
 * server-side from V2 api/corporatetieupplans/ListTieupExpiryMigrations/): Child Name | Center | Company | Offer Title |
 * Offer Expiry Date | Offer Status | Migration Status | Migration Date | Renewal Expiry Date, plus "never" Child ID / Child Name.
 * Download = client CSV Tieup_Expiry_Migration_Records.csv (columns 1–10). Search placeholder "Filter these entries".
 */
public class TieupExpiryMigrationPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_invoice_report";
    private static final String TABLE = "table.dt-admission-invite";

    public TieupExpiryMigrationPage(WebDriver driver) {
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

    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                    "return !!(window.jQuery && jQuery.fn.dataTable && document.querySelector('" + TABLE + "') && jQuery.fn.dataTable.isDataTable(document.querySelector('" + TABLE + "')));")));
            hideApiDebugPanel();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String dt() {
        return "jQuery(document.querySelector('" + TABLE + "')).DataTable()";
    }

    public String getHeading() {
        return String.valueOf(js().executeScript("var h=[].find.call(document.querySelectorAll('h2,h4,h5'), function(x){return /Expiry Migration/.test(x.textContent);});return h?h.textContent.trim():'';"));
    }

    // ════════════════ Filters ════════════════
    public boolean isCompanyFilterVisible() { return isShown(By.cssSelector(FILTER + " .select2-container")); }
    public boolean isFilterButtonVisible() { return isShown(By.cssSelector(FILTER + " #btn_submit")); }
    public boolean isClearLinkVisible() { return isShown(By.cssSelector(FILTER + " a.clx-btn-link")); }
    public boolean isSearchVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }
    public boolean isDownloadVisible() { return isShown(By.cssSelector(".buttons-csv")); }
    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }
    public boolean isPageLengthVisible() { return isShown(By.cssSelector(".dataTables_length select")); }

    public String getSearchPlaceholder() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_filter input');return i?(i.getAttribute('placeholder')||''):'';"));
    }

    public List<String> getPageLengthOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('.dataTables_length select option'), function(o){return o.text.trim();});"));
    }

    public String getSelectedCompany() {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + FILTER + " #sr_corporate_company');"
                + "return s && s.selectedIndex>=0 ? s.options[s.selectedIndex].text.trim() : '';"));
    }

    public List<String> getCompanyOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('" + FILTER + " #sr_corporate_company option'), function(o){return o.text.trim();});"));
    }

    public void selectCompany(String text) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #sr_corporate_company'), t=arguments[0];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.val(o.first().val()).trigger('change'); return true;", text);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("'" + text + "' not in #sr_corporate_company");
    }

    private void submitAndWait(String clickJs) {
        js().executeScript("document.body.setAttribute('data-qa-old','1');");
        js().executeScript(clickJs);
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState==='complete' && !document.body.hasAttribute('data-qa-old');")));
        isPageLoaded();
    }

    public void clickFilter() throws InterruptedException {
        submitAndWait("document.querySelector('" + FILTER + " #btn_submit').click();");
        Thread.sleep(500);
    }

    public void clickClear() throws InterruptedException {
        submitAndWait("document.querySelector('" + FILTER + " a.clx-btn-link').click();");
        Thread.sleep(500);
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return " + dt() + ".columns().header().toArray().map(function(h){return h.textContent.trim();});"));
    }

    // Headers the user sees (the CSV-only columns have class "never")
    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return " + dt() + ".columns().header().toArray()"
                + ".filter(function(h){return !/\\bnever\\b/.test(h.className);}).map(function(h){return h.textContent.trim();});"));
    }

    private int headerIndex(String header) {
        List<String> all = getAllColumnHeaders();
        int i = all.indexOf(header);
        if (i < 0) throw new IllegalArgumentException("Column '" + header + "' not in " + all);
        return i;
    }

    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("return " + dt() + ".column(arguments[0],{search:'applied', order:'applied'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=String(v).replace(/<br\\s*\\/?>/g,' '); return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return " + dt() + ".rows({search:'applied'}).count();")).intValue();
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return " + dt() + ".page.len();")).intValue();
    }

    // [[columnIndex, "asc"|"desc"], …]
    public String getOrder() {
        return String.valueOf(js().executeScript("return JSON.stringify(" + dt() + ".order());"));
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_info');return i?i.innerText.trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(".dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1000);
    }

    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('.buttons-csv').click();");
        Thread.sleep(1500);
    }

    public boolean isNextPageEnabled() {
        return Boolean.TRUE.equals(js().executeScript("var n=document.querySelector('.dataTables_paginate .next');return !!n && !/disabled/.test(n.className);"));
    }

    public boolean isPreviousPageEnabled() {
        return Boolean.TRUE.equals(js().executeScript("var n=document.querySelector('.dataTables_paginate .previous');return !!n && !/disabled/.test(n.className);"));
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("jQuery(" + dt() + ".column(arguments[0]).header()).click();", headerIndex(header));
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return String.valueOf(js().executeScript("return " + dt() + ".column(arguments[0]).header().className;", headerIndex(header)));
    }
}
