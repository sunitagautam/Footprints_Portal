package pages.Franchise.CorporateTieups;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * Franchise → Corporate Tieups → Adjust Corporate Credits (/corporate_invoice?type=due).
 * The same page as Corporate Invoice (filters #frm_invoice_report, DataTable #datatable-invoice, Update / Void), so the
 * filter, table and popup methods come from CorporateInvoicePage. Adds the Adjust icon
 * (a.adjust_credits_button[data-invoice], shown only when the child has a corporate credit balance; confirm →
 * POST api/chargebeelistner/adjustCorporateCredits/) and the Revert icon (.revert_credit_adjustment).
 */
public class AdjustCorporateCreditsPage extends CorporateInvoicePage {

    private static final String TABLE = "#datatable-invoice";

    public AdjustCorporateCreditsPage(WebDriver driver) {
        super(driver);
    }

    private JavascriptExecutor jsx() {
        return (JavascriptExecutor) driver;
    }

    @SuppressWarnings("unchecked")
    private List<String> list(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List) for (Object x : (List<Object>) o) out.add(String.valueOf(x));
        return out;
    }

    // ✅ "Franchise > Corporate Tieups > Adjust Corporate Credits" from the page's menuData
    public String getMenuPath() {
        return String.valueOf(jsx().executeScript("var m=window.menuData||[]; for(var i=0;i<m.length;i++){"
                + " if(m[i].url==='corporate_invoice?type=due') return m[i].topMenu+' > '+m[i].subMenu+' > '+m[i].title; } return '';"));
    }

    // ✅ The applied filter the page keeps for its download (window.pageDownloadFilters)
    public String getDownloadFilter(String key) {
        return String.valueOf(jsx().executeScript("var f=window.pageDownloadFilters||{}; return f[arguments[0]]==null?'':String(f[arguments[0]]);", key));
    }

    // ✅ Invoice IDs (all filtered rows) that show the Adjust icon
    public List<String> getInvoiceIdsWithAdjust() {
        return list(jsx().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray()"
                + ".filter(function(r){return r.querySelector('.adjust_credits_button');}).map(function(r){return r.children[1].textContent.trim();});"));
    }

    // ✅ Invoice IDs (all filtered rows) that show the Revert icon
    public List<String> getInvoiceIdsWithRevert() {
        return list(jsx().executeScript("return jQuery('" + TABLE + "').DataTable().rows({search:'applied'}).nodes().toArray()"
                + ".filter(function(r){return r.querySelector('.revert_credit_adjustment');}).map(function(r){return r.children[1].textContent.trim();});"));
    }

    // ✅ Adjust icon tooltip as text: "Adjust Invoice #66151 From Credit Balance Due Amount - Rs.16,645.00 Credit Balance - Rs. 2,564.00"
    public String getAdjustTooltip(String invoiceId) {
        return String.valueOf(jsx().executeScript("var a=document.querySelector('.adjust_credits_button[data-invoice=\"'+arguments[0]+'\"]'); if(!a) return '';"
                + "var t=a.getAttribute('data-original-title')||a.getAttribute('title')||''; return jQuery('<div>').html(t).text().replace(/\\s+/g,' ').trim();", invoiceId));
    }

    // ✅ Revert icon tooltip: "Revert Corporate Credit Adjustment for Unpaid Invoice #66151"
    public String getRevertTooltip(String invoiceId) {
        return String.valueOf(jsx().executeScript("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(x){return x.children[1].textContent.trim()===I;})[0];"
                + "var a=r?r.querySelector('.revert_credit_adjustment'):null; if(!a) return '';"
                + "return (a.getAttribute('data-original-title')||a.getAttribute('title')||'').trim();", invoiceId));
    }

    // ✅ Icons in an invoice's Action cell: download / update / void / adjust / revert
    public List<String> getRowActions(String invoiceId) {
        return list(jsx().executeScript("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(x){return x.children[1].textContent.trim()===I;})[0];"
                + "if(!r) return []; var c=r.lastElementChild, a=[];"
                + "if(c.querySelector('a[onclick*=PDFDownload]')) a.push('download'); if(c.querySelector('.btn-update')) a.push('update');"
                + "if(c.querySelector('.void_credits_button')) a.push('void'); if(c.querySelector('.adjust_credits_button')) a.push('adjust');"
                + "if(c.querySelector('.revert_credit_adjustment')) a.push('revert'); return a;", invoiceId));
    }

    // ✅ Records the page's AJAX responses ("<url> => <status> <body>") — the adjust / revert results are not shown as a toast
    public void startAjaxCapture() {
        jsx().executeScript("window.__ajax=[]; jQuery(document).ajaxComplete(function(e,x,s){window.__ajax.push(s.url+' => '+x.status+' '+(x.responseText||'').substring(0,400));});");
    }

    public List<String> getAjaxResponses() { return list(jsx().executeScript("return window.__ajax || [];")); }

    // ✅ Clicks the Adjust icon (call startCapture(true) first to answer the native confirm), then waits for the response
    public String clickAdjust(String invoiceId) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = jsx().executeScript("var a=document.querySelector('.adjust_credits_button[data-invoice=\"'+arguments[0]+'\"]'); if(!a) return false;"
                + "a.scrollIntoView({block:'center'}); a.click(); return true;", invoiceId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No Adjust icon for invoice " + invoiceId);
        System.out.println("▶ Adjust clicked (invoice " + invoiceId + ")");
        return waitForAjax("adjustCorporateCredits");
    }

    // ✅ Clicks the Revert icon (call startCapture(true) first), then waits for the response
    public String clickRevert(String invoiceId) throws InterruptedException {
        hideApiDebugPanel();
        Object ok = jsx().executeScript("var I=arguments[0]; var r=jQuery('" + TABLE + "').DataTable().rows().nodes().toArray().filter(function(x){return x.children[1].textContent.trim()===I;})[0];"
                + "var a=r?r.querySelector('.revert_credit_adjustment'):null; if(!a) return false; a.scrollIntoView({block:'center'}); jQuery(a).click(); return true;", invoiceId);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("No Revert icon for invoice " + invoiceId);
        System.out.println("▶ Revert clicked (invoice " + invoiceId + ")");
        return waitForAjax("revertCorporateCreditAdjustment");
    }

    private String waitForAjax(String urlPart) throws InterruptedException {
        for (int i = 0; i < 60; i++) {
            for (String r : getAjaxResponses()) if (r.contains(urlPart)) return r;
            Thread.sleep(500);
        }
        return "";
    }

    // ✅ Text the Adjust icon is replaced with after the call ("Corporate Credits Adjusted")
    public String getAdjustIconText(String invoiceId) {
        return String.valueOf(jsx().executeScript("var a=document.querySelector('.adjust_credits_button[data-invoice=\"'+arguments[0]+'\"]');return a?a.textContent.trim():'';", invoiceId));
    }

    public String getUpdateMessage() {
        return String.valueOf(jsx().executeScript("var o=[];document.querySelectorAll('.form-update-message, .flash-msg').forEach(function(e){if(e.textContent.trim()) o.push(e.textContent.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim());}); return o.join(' | ');"));
    }
}
