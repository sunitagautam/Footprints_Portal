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
 * Franchise → Corporate Tieups → Pending Co-Pay Setup (/pending_copay_setup), right Copay_Update_Access.
 * Heading "Pending Co-Pay Setup Admissions". Filter form #frm_invoice_report (posts back): Offer #sr_offer_id, Center
 * #sr_center_id (select2, "All"), Filter #btn_submit. Table #datatable-offers-list (10 per page): Child Id | Child Name |
 * Center | Offer Title | Program | Plan | Joining Date | Actions ("Update Subscription Plan" →
 * copay_billing_update?pop=yes&child_id=, popdown_xl_large). Download = client-side CSV.
 * Popup form #apply_billing_update: registration_fee (+payer, fresh admissions only), parent / corporate monthly plan
 * (bootstrap-multiselect, "add_new_plan" = Create New Plan + *_plan_amt) and billing month, yearly plans (V1 only),
 * monthly / yearly coupon, sent_mail; Update #btn_submit_5 → validateUpdateDetails() → POST api/billing/updateCopayBillingSetup
 * → PNotify "Co-Pay Plan Subscription" message.
 */
public class PendingCoPaySetupPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String FILTER = "#frm_invoice_report";
    private static final String TABLE = "#datatable-offers-list";
    private static final String POPUP = "#popdown-dialog";

    public PendingCoPaySetupPage(WebDriver driver) {
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
        return String.valueOf(js().executeScript("var h=[].find.call(document.querySelectorAll('h2,h4,h5'), function(x){return /Pending Co-Pay/.test(x.textContent);});return h?h.textContent.trim():'';"));
    }

    // ════════════════ Filters ════════════════
    public boolean isOfferFilterVisible() { return isShown(By.cssSelector(FILTER + " #sr_offer_id")); }
    public boolean isCenterFilterVisible() { return isShown(By.cssSelector(FILTER + " .select2-container")); }
    public boolean isFilterButtonVisible() { return isShown(By.cssSelector(FILTER + " #btn_submit")); }
    public boolean isSearchVisible() { return isShown(By.cssSelector(".dataTables_filter input")); }
    public boolean isDownloadVisible() { return isShown(By.cssSelector(".buttons-csv")); }
    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    public String getSelectedText(String id) {
        return String.valueOf(js().executeScript("var s=document.querySelector('" + FILTER + " #'+arguments[0]);"
                + "return s && s.selectedIndex>=0 ? s.options[s.selectedIndex].text.trim() : '';", id));
    }

    public void selectByText(String id, String text) {
        Object ok = js().executeScript("var s=jQuery('" + FILTER + " #'+arguments[0]), t=arguments[1];"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;}); if(!o.length) return false;"
                + "s.val(o.first().val()).trigger('change'); return true;", id, text);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("'" + text + "' not in #" + id);
    }

    public void clickFilter() throws InterruptedException {
        js().executeScript("document.body.setAttribute('data-qa-old','1');");
        js().executeScript("document.querySelector('" + FILTER + " #btn_submit').click();");
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d)
                .executeScript("return document.readyState==='complete' && !document.body.hasAttribute('data-qa-old');")));
        isPageLoaded();
        Thread.sleep(500);
    }

    // ════════════════ Table ════════════════
    public List<String> getAllColumnHeaders() {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().columns().header().toArray().map(function(h){return h.textContent.trim();});"));
    }

    private int headerIndex(String header) {
        List<String> all = getAllColumnHeaders();
        int i = all.indexOf(header);
        if (i < 0) throw new IllegalArgumentException("Column '" + header + "' not in " + all);
        return i;
    }

    public List<String> getColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{search:'applied'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=v; return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public List<String> getPageColumnValues(String header) {
        return strings(js().executeScript("return jQuery('" + TABLE + "').DataTable().column(arguments[0],{page:'current'}).data().toArray()"
                + ".map(function(v){var d=document.createElement('div'); d.innerHTML=v; return d.textContent.replace(/\\s+/g,' ').trim();});", headerIndex(header)));
    }

    public int getRowCount() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).count();")).intValue();
    }

    public int getPageLength() {
        return ((Number) js().executeScript("return jQuery('" + TABLE + "').DataTable().page.len();")).intValue();
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

    public List<String> getActionTitles() {
        return strings(js().executeScript("var s={}; jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().forEach(function(r){"
                + "[].forEach.call(r.cells[r.cells.length-1].querySelectorAll('a[title]'), function(a){s[a.title]=1;});}); return Object.keys(s);"));
    }

    // ✅ Popup text of every listed child, fetched in the page (read-only): "<id> :: <text>"
    public List<String> fetchAllPopupTexts() throws InterruptedException {
        js().executeScript("window.__qaPop=null; var ids=jQuery('" + TABLE + "').DataTable().column(0).data().toArray().map(function(v){var d=document.createElement('div');d.innerHTML=v;return d.textContent.trim();});"
                + "(async function(){var o=[]; for (const id of ids){ var h=await (await fetch('copay_billing_update?pop=yes&child_id='+id,{credentials:'same-origin'})).text();"
                + " var d=new DOMParser().parseFromString(h,'text/html'); o.push(id+' :: '+d.body.innerText.replace(/\\s+/g,' ').trim().substring(0,120)); } window.__qaPop=o; })();");
        for (int i = 0; i < 90; i++) {
            Object r = js().executeScript("return window.__qaPop;");
            if (r != null) return strings(r);
            Thread.sleep(1000);
        }
        return new ArrayList<>();
    }

    // ════════════════ Update Subscription Plan popup ════════════════
    public void openUpdatePlan(String childId) throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("jQuery('" + TABLE + "').DataTable().search(arguments[0]).draw();", childId);
        Thread.sleep(800);
        js().executeScript("document.querySelector(\"" + TABLE + " a[href$='copay_billing_update?pop=yes&child_id=\" + arguments[0] + \"']\").click();", childId);
        new WebDriverWait(driver, Duration.ofSeconds(40)).until(d -> !d.findElements(By.cssSelector(POPUP + " #apply_billing_update")).isEmpty());
        Thread.sleep(2000);
    }

    public String getPopupText() {
        return String.valueOf(js().executeScript("var d=document.querySelector('" + POPUP + "');return d?d.innerText.replace(/\\s+/g,' ').trim():'';"));
    }

    public void clickUpdate() {
        js().executeScript("document.querySelector('" + POPUP + " #btn_submit_5').click();");
    }

    // Visible validation labels ("error-<field>: text")
    public List<String> getVisibleErrors() {
        return strings(js().executeScript("return [].filter.call(document.querySelectorAll(\"" + POPUP + " [class^='error-']\"), function(e){return e.offsetParent && e.textContent.trim();})"
                + ".map(function(e){return e.className.split(' ')[0].replace('error-','')+': '+e.textContent.trim();});"));
    }

    public void setPopupValue(String id, String value) {
        js().executeScript("var e=document.querySelector('" + POPUP + " #'+arguments[0]); e.value=arguments[1]; jQuery(e).trigger('change');", id, value);
    }

    // ✅ bootstrap-multiselect plan dropdown: select the value through the plugin, firing its onChange
    public void selectPlan(String id, String value) {
        js().executeScript("var s=jQuery('" + POPUP + " #'+arguments[0]); try { s.multiselect('select', arguments[1], true); } catch(e) {} "
                + "if (s.val() !== arguments[1]) { s.val(arguments[1]).trigger('change'); }", id, value);
    }

    public String getPopupValue(String id) {
        return String.valueOf(js().executeScript("var e=document.querySelector('" + POPUP + " #'+arguments[0]);return e?String(e.value):'';", id));
    }

    // ✅ PNotify / block message after Update
    public String waitForUpdateMessage() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            Object m = js().executeScript("var e=[].filter.call(document.querySelectorAll('.ui-pnotify-text, .blockMsg, " + POPUP + " .alert'), function(x){return x.offsetParent && x.innerText.trim();});"
                    + "return e.length ? e.map(function(x){return x.innerText.replace(/\\s+/g,' ').trim();}).join(' | ') : '';");
            // ✅ skip the "Updating Details. Please Wait..." overlay — wait for the result text
            if (m != null && !String.valueOf(m).isEmpty() && !String.valueOf(m).contains("Please Wait")) return String.valueOf(m);
            Thread.sleep(200);
        }
        return "";
    }
}
