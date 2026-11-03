package pages.Franchise.CorporateTieups;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Tieup Plans and Pricing (/tie_up_child_financial_info), heading "Tie Up Admission
 * Financial Information Report". Select Offer #offer_filter (select2) posts the page back (form.submit); the page then
 * shows "<N> admissions" and the "Apply New Fee Structure" button (#apply_fee_structure).
 * "Only Doubtful Records" #doubtful_row (checked by default) is a client-side DataTables filter on the last column
 * (Something_Wrong = "Doubtful"). Table #datatable_child_financial_info (responsive, 10 per page); Download Report
 * (#download_emp_list_s) posts #dwd_plan_pricing → Tie_Up_Children_Financial_Information_Report_<d_MMMM_yyyy>.csv.
 * Apply: native confirm → POST api/franchisesupport/applyTieUpLatestPlan/ → flash in .update-message → reload.
 */
public class TieupPlansPricingPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String TABLE = "#datatable_child_financial_info";

    public TieupPlansPricingPage(WebDriver driver) {
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

    // Text above the offer dropdown, e.g. "Tie Up Admission Financial Information Report 329 admissions Apply New Fee Structure"
    public String getTopText() {
        return String.valueOf(js().executeScript("var b=document.querySelector('.report-page-outer-box')||document.body;"
                + "return b.innerText.split('--Select Offer--')[0].replace(/\\s+/g,' ').trim();"));
    }

    // "<N> admissions" → N
    public int getAdmissionsCount() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+) admissions").matcher(getTopText());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    public boolean isApplyButtonVisible() { return isShown(By.cssSelector("#apply_fee_structure")); }
    public boolean isOfferDropdownVisible() { return isShown(By.cssSelector(".select2-container")); }
    public boolean isDoubtfulCheckboxPresent() { return !driver.findElements(By.id("doubtful_row")).isEmpty(); }
    public boolean isSearchVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }
    public boolean isDownloadVisible() { return isShown(By.id("download_emp_list_s")); }
    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    // ════════════════ Offer ════════════════
    public List<String> getOfferOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelector('#offer_filter').options, function(o){return o.text.trim();});"));
    }

    public String getSelectedOffer() {
        return String.valueOf(js().executeScript("var s=document.querySelector('#offer_filter');return s.options[s.selectedIndex].text.trim();"));
    }

    public String getSelectedOfferId() {
        return String.valueOf(js().executeScript("return document.querySelector('#offer_filter').value;"));
    }

    // ✅ The dropdown's own onchange is this.form.submit() — set the value and submit the same way, then wait for reload
    public void selectOffer(String offerTitle) throws InterruptedException {
        js().executeScript("document.body.setAttribute('data-qa-old','1');");
        Object ok = js().executeScript("var s=document.querySelector('#offer_filter'), t=arguments[0];"
                + "for (var i=0;i<s.options.length;i++){ if(s.options[i].text.trim()===t){ s.selectedIndex=i; s.form.submit(); return true; } } return false;", offerTitle);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Offer '" + offerTitle + "' not in the list");
        new WebDriverWait(driver, Duration.ofSeconds(90)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState==='complete' && !document.body.hasAttribute('data-qa-old');")));
        isPageLoaded();
        Thread.sleep(500);
    }

    // ✅ select2 search box: open, type, read the matching options, close
    public List<String> searchOfferDropdown(String keyword) throws InterruptedException {
        hideApiDebugPanel();
        driver.findElement(By.cssSelector(".select2-container .select2-selection")).click();
        Thread.sleep(500);
        WebElement box = driver.findElement(By.cssSelector(".select2-container--open .select2-search__field"));
        box.sendKeys(keyword);
        Thread.sleep(1000);
        List<String> out = strings(js().executeScript("return [].map.call(document.querySelectorAll('.select2-results__option'), function(o){return o.textContent.trim();});"));
        box.sendKeys(org.openqa.selenium.Keys.ESCAPE);
        Thread.sleep(300);
        return out;
    }

    // ════════════════ Doubtful ════════════════
    public boolean isDoubtfulChecked() {
        return Boolean.TRUE.equals(js().executeScript("return document.querySelector('#doubtful_row').checked;"));
    }

    // ✅ The styled (uniform) checkbox hides the input — click it via JS, which fires the change handler
    public void setDoubtful(boolean checked) throws InterruptedException {
        if (isDoubtfulChecked() != checked) js().executeScript("document.querySelector('#doubtful_row').click();");
        Thread.sleep(1000);
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().columns().header().toArray().map(function(h){return h.textContent.trim();});"));
    }

    public List<String> getVisibleColumnHeaders() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll('" + TABLE + " thead th'), function(t){return t.offsetParent && getComputedStyle(t).display!=='none';})"
                + ".map(function(t){return t.textContent.trim();});"));
    }

    private int headerIndex(String header) {
        List<String> all = getAllColumnHeaders();
        int i = all.indexOf(header);
        if (i < 0) throw new IllegalArgumentException("Column '" + header + "' not in " + all);
        return i;
    }

    // ✅ Values of rows passing the current filters (doubtful + search), cell HTML → text
    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=String(v).replace(/<br\\s*\\/?>|<\\/li>/g,' '); return d.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{page:'current'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=String(v); return d.textContent.replace(/\\u00a0/g,' ').replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getTotalRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows().count();")).intValue();
    }

    // Child ids whose name link is red (text-danger) among the rows passing the filters
    public List<String> getRedNameChildIds() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).data().toArray()"
                + ".filter(function(r){return /text-danger/.test(String(r[1]));}).map(function(r){return String(r[0]).replace(/<[^>]+>/g,'').trim();});"));
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
    }

    public List<String> getPageLengthOptions() {
        return strings(js().executeScript("return [].map.call(document.querySelectorAll('select[name=datatable_child_financial_info_length] option'), function(o){return o.text.trim();});"));
    }

    public String getInfoText() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_info');return i?i.innerText.trim():'';"));
    }

    public String getEmptyTableMessage() {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + TABLE + " td.dataTables_empty');return e?e.textContent.trim():'';"));
    }

    public String getSearchPlaceholder() {
        return String.valueOf(js().executeScript("var i=document.querySelector('.dataTables_filter input');return i?i.placeholder:'';"));
    }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement box = driver.findElement(By.cssSelector(".dataTables_filter input"));
        box.clear();
        box.sendKeys(keyword);
        Thread.sleep(1000);
    }

    // ✅ stateSave keeps the last search / page / order across reloads — reset it
    public void resetTableState() throws InterruptedException {
        js().executeScript("var dt=jQuery('" + TABLE + "').DataTable(); dt.search('').order([0,'asc']).page.len(10).draw(); dt.page(0).draw('page');");
        Thread.sleep(500);
    }

    public boolean isNextPageEnabled() {
        return Boolean.TRUE.equals(js().executeScript("var n=document.querySelector('.dataTables_paginate .next');return !!n && !/disabled/.test(n.className);"));
    }

    public boolean isPreviousPageEnabled() {
        return Boolean.TRUE.equals(js().executeScript("var n=document.querySelector('.dataTables_paginate .previous');return !!n && !/disabled/.test(n.className);"));
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .next').click();");
        Thread.sleep(800);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("document.querySelector('.dataTables_paginate .previous').click();");
        Thread.sleep(800);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("jQuery(jQuery('" + TABLE + "').DataTable().column(arguments[0]).header()).click();", headerIndex(header));
        Thread.sleep(800);
    }

    public String getHeaderSortClass(String header) {
        return String.valueOf(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0]).header().className;", headerIndex(header)));
    }

    // ════════════════ Expand / collapse (responsive "+") ════════════════
    public void clickFirstRowControl() throws InterruptedException {
        js().executeScript("document.querySelector('" + TABLE + " tbody tr td').click();");
        Thread.sleep(1000);
    }

    // "parent" when expanded; text of the detail row when shown
    public boolean isFirstRowExpanded() {
        return Boolean.TRUE.equals(js().executeScript("var r=document.querySelector('" + TABLE + " tbody tr');"
                + "return /parent/.test(r.className) && !!r.nextElementSibling && /child/.test(r.nextElementSibling.className);"));
    }

    public String getFirstRowDetailText() {
        return String.valueOf(js().executeScript("var r=document.querySelector('" + TABLE + " tbody tr').nextElementSibling;"
                + "return r && /child/.test(r.className) ? r.innerText.replace(/\\s+/g,' ').trim() : '';"));
    }

    public boolean hasCollapsedColumns() {
        return Boolean.TRUE.equals(js().executeScript("return document.querySelector('" + TABLE + "').classList.contains('collapsed');"));
    }

    // ════════════════ Download / Apply ════════════════
    public void clickDownload() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('#download_emp_list_s').click();");
        Thread.sleep(2000);
    }

    public void clickApplyNewFeeStructure() {
        hideApiDebugPanel();
        js().executeScript("jQuery('#apply_fee_structure').trigger('click');");
    }

    public String readConfirm(boolean accept) {
        Alert a = new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.alertIsPresent());
        String text = a.getText();
        if (accept) a.accept();
        else a.dismiss();
        return text;
    }

    // ✅ flash_message in .update-message (page reloads 1.5 s after success) — polled quickly
    public String waitForUpdateMessage() throws InterruptedException {
        for (int i = 0; i < 150; i++) {
            Object m = js().executeScript("var e=[].filter.call(document.querySelectorAll('.update-message .alert, .alert-success, .alert-warning'),"
                    + "function(x){return x.offsetParent && x.innerText.trim();}); return e.length ? e[0].innerText.replace(/×|Close/g,'').replace(/\\s+/g,' ').trim() : '';");
            if (m != null && !String.valueOf(m).isEmpty()) return String.valueOf(m);
            Thread.sleep(200);
        }
        return "";
    }
}
