package pages.Franchise.CorporateTieups;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════════════════════════
// ADMISSION INVITES (Franchise → Corporate Tieups → Admission Invites)
// Screen: /corporate_admission_invites — explored read-only 2026-10-27.
//
// ⚠ Two forms reuse the same ids (invite_email, invite_phone, invite_offer_id):
//   #frm_invite1          — "Send New Invitation"
//   #frm_invite_admission — "Corporate Admission Invite" filters (posts back)
//   Every locator is scoped to its form.
// Saves (Send Invite, row Send Invitation Email, Expire, Edit) all end in a
// page reload; the message is shown from the "flash-message" cookie.
// ═══════════════════════════════════════════════════════════════
public class AdmissionInvitesPage {

    WebDriver driver;
    WebDriverWait wait;

    private static final String SEND = "#frm_invite1";
    private static final String FILTER = "#frm_invite_admission";
    private static final String TABLE = "table.dt-admission-invite";

    public AdmissionInvitesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    private String text(WebElement e) {
        return e.getAttribute("textContent").replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }

    private boolean isShown(By by) {
        for (WebElement e : driver.findElements(by))
            try {
                if (e.isDisplayed()) return true;
            } catch (Exception ignored) {
            }
        return false;
    }

    private List<String> optionTexts(WebElement select) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(select).getOptions())
            out.add(o.getAttribute("textContent").trim());
        return out;
    }

    // ✅ UAT's fixed "API Debug (Test)" panel covers the bottom of the page
    public void hideApiDebugPanel() {
        js().executeScript("var p=document.getElementById('api-debug-panel'); if(p) p.style.display='none';");
    }

    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(TABLE)));
            hideApiDebugPanel();
            return driver.getCurrentUrl().contains("corporate_admission_invites");
        } catch (Exception e) {
            return false;
        }
    }

    public boolean pageHasText(String s) {
        return driver.findElement(By.tagName("body")).getText().contains(s);
    }

    // ═══════════════════════════════════════════════
    // SEND NEW INVITATION (#frm_invite1)
    // ═══════════════════════════════════════════════
    private WebElement send(String id) {
        return driver.findElement(By.cssSelector(SEND + " #" + id));
    }

    public boolean isSendFieldVisible(String id) { return isShown(By.cssSelector(SEND + " #" + id)); }

    public List<String> getSendOptions(String id) { return optionTexts(send(id)); }

    public boolean isBreakupReadonly() { return send("breakup").getAttribute("readonly") != null; }

    public void selectSendParentType(String type) {
        new Select(send("invite_parent_type")).selectByVisibleText(type);
    }

    public void enterSendField(String id, String value) {
        WebElement e = send(id);
        e.clear();
        e.sendKeys(value);
    }

    public String getSendFieldValue(String id) { return send(id).getAttribute("value"); }

    // ✅ select2 offer — select by exact text, fire change (fills the Center list)
    public void selectSendOffer(String offerTitle) throws InterruptedException {
        Object ok = js().executeScript("var t=arguments[0], s=jQuery('" + SEND + " #invite_offer_id');"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                + "if(!o.length) return false; s.val(o.first().val()).trigger('change'); return true;", offerTitle);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Offer not in Send form: " + offerTitle);
        Thread.sleep(600);
    }

    // ✅ First offer whose title starts with the prefix ("" when none)
    public String findSendOffer(String prefix) {
        for (String o : getSendOptions("invite_offer_id"))
            if (o.startsWith(prefix)) return o;
        return "";
    }

    // ✅ First Co-Pay offer (page JS list coPayTieups) → its title
    public String selectFirstCoPayOffer() throws InterruptedException {
        Object t = js().executeScript("var s=jQuery('" + SEND + " #invite_offer_id'); var o=s.find('option').filter(function(){"
                + "return jQuery.inArray(jQuery(this).val(), coPayTieups) != -1;}).first(); if(!o.length) return '';"
                + "s.val(o.val()).trigger('change'); return o.text().trim();");
        Thread.sleep(600);
        return String.valueOf(t);
    }

    public List<String> getSendCenterOptions() { return optionTexts(send("invite_center_id")); }

    public void selectSendCenter(String center) {
        new Select(send("invite_center_id")).selectByVisibleText(center);
    }

    // ✅ Records XHRs and native confirms; acceptConfirm=false makes every confirm answer Cancel.
    //    Confirms are also kept in sessionStorage — a successful save reloads the page.
    public void startCapture(boolean acceptConfirm) {
        js().executeScript("window.__reqs=[]; try{ sessionStorage.setItem('__confirms','[]'); }catch(e){}"
                + "var o=XMLHttpRequest.prototype.open; XMLHttpRequest.prototype.open=function(m,u){window.__reqs.push(m+' '+u); return o.apply(this,arguments);};"
                + "window.confirm=function(m){ try{ var c=JSON.parse(sessionStorage.getItem('__confirms')||'[]'); c.push(m);"
                + " sessionStorage.setItem('__confirms', JSON.stringify(c)); }catch(e){} return " + acceptConfirm + "; };");
    }

    @SuppressWarnings("unchecked")
    public List<String> getCapturedRequests() {
        Object r = js().executeScript("return window.__reqs || [];");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public List<String> getCapturedConfirms() {
        Object r = js().executeScript("try{ return JSON.parse(sessionStorage.getItem('__confirms')||'[]'); }catch(e){ return []; }");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    public void clickSendInvite() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", send("send_invite1"));
        System.out.println("▶ Send Invite clicked");
        Thread.sleep(2000);
    }

    // ✅ Visible validation messages of a form ("<field> => <message>"), incl. the Co-Pay breakup message
    @SuppressWarnings("unchecked")
    public List<String> getFormErrors(String formCss) {
        Object r = js().executeScript(
                "var f=document.querySelector(arguments[0]); if(!f) return [];"
                        + "var out=Array.from(f.querySelectorAll('label[id$=\"-error\"]'))"
                        + ".filter(function(e){return (e.offsetWidth||e.offsetHeight) && e.innerText.trim();})"
                        + ".map(function(e){return (e.getAttribute('for')||'') + ' => ' + e.innerText.trim();});"
                        + "Array.from(document.querySelectorAll('.breakup-error-msg')).forEach(function(e){"
                        + "if((e.offsetWidth||e.offsetHeight) && e.innerText.trim()) out.push('breakup => '+e.innerText.trim());});"
                        + "return out;", formCss);
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    public List<String> getSendErrors() { return getFormErrors(SEND); }

    // ✅ Page flash messages (shown after the reload from the "flash-message" cookie, or inline)
    @SuppressWarnings("unchecked")
    public List<String> getFlashMessages() {
        Object r = js().executeScript(
                "return Array.from(document.querySelectorAll('.flash-msg .alert, .alert-success, .alert-warning, .alert-danger'))"
                        + ".filter(function(e){return (e.offsetWidth||e.offsetHeight) && e.innerText.trim() && !e.closest('#api-debug-panel');})"
                        + ".map(function(e){return e.innerText.replace(/\\s+/g,' ').replace(/^×\\s*(Close)?\\s*/,'').trim();});");
        List<String> out = new ArrayList<>();
        if (r instanceof List) for (Object o : (List<?>) r) if (!out.contains(String.valueOf(o))) out.add(String.valueOf(o));
        return out;
    }

    // ✅ After a save the page reloads — wait for it, then read the flash
    public String waitForReloadAndFlash() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            Thread.sleep(500);
            try {
                if (driver.findElements(By.cssSelector(TABLE)).isEmpty()) continue;
                List<String> f = getFlashMessages();
                if (!f.isEmpty()) return String.join(" || ", f);
            } catch (Exception ignored) {
            }
        }
        return String.join(" || ", getFlashMessages());
    }

    // ═══════════════════════════════════════════════
    // CORPORATE ADMISSION INVITE — FILTERS (#frm_invite_admission)
    // ═══════════════════════════════════════════════
    private WebElement filter(String id) {
        return driver.findElement(By.cssSelector(FILTER + " #" + id));
    }

    public boolean isFilterFieldVisible(String id) { return isShown(By.cssSelector(FILTER + " #" + id)); }

    public List<String> getFilterOptions(String id) { return optionTexts(filter(id)); }

    public String getSelectedFilterStatus() {
        return new Select(filter("invite_status")).getFirstSelectedOption().getText().trim();
    }

    // ✅ Tie-Up Types multiselect — the checked values
    @SuppressWarnings("unchecked")
    public List<String> getSelectedTieupTypes() {
        Object r = js().executeScript("return Array.from(document.querySelector('" + FILTER + " #tieup_type').selectedOptions).map(function(o){return o.text.trim();});");
        return r instanceof List ? (List<String>) r : new ArrayList<>();
    }

    public void selectFilterCompany(String company) {
        new Select(filter("invite_company_id")).selectByVisibleText(company);
    }

    public void selectFilterOffer(String offerTitle) {
        Object ok = js().executeScript("var t=arguments[0], s=jQuery('" + FILTER + " #invite_offer_id');"
                + "var o=s.find('option').filter(function(){return jQuery(this).text().trim()===t;});"
                + "if(!o.length) return false; s.val(o.first().val()).trigger('change'); return true;", offerTitle);
        if (!Boolean.TRUE.equals(ok)) throw new IllegalArgumentException("Offer not in filter: " + offerTitle);
    }

    public void enterFilterInviteEmail(String email) {
        WebElement e = filter("invite_email");
        e.clear();
        e.sendKeys(email);
    }

    public void enterFilterInvitePhone(String phone) {
        WebElement e = filter("invite_phone");
        e.clear();
        e.sendKeys(phone);
    }

    // ✅ [All, Requested, Invited, Used, Expired]
    public void selectFilterInviteStatus(String status) {
        new Select(filter("invite_status")).selectByVisibleText(status);
    }

    // ✅ types = exact option texts, e.g. "Corporate Pay"; none = all
    public void selectTieupTypes(String... types) {
        js().executeScript("var s=jQuery('" + FILTER + " #tieup_type'); var t=arguments[0];"
                + "if(!t.length){ s.multiselect('selectAll', false); } else { s.multiselect('deselectAll', false); s.multiselect('select', t); }"
                + "s.multiselect('updateButtonText');", java.util.Arrays.asList(types));
    }

    public void clickSearch() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("arguments[0].click();", filter("btn_filter"));
        System.out.println("▶ Search clicked");
        Thread.sleep(3500);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(TABLE)));
        hideApiDebugPanel();
    }

    // ✅ Shortcut: every status + every tie-up type, one invite email
    public void searchAllByEmail(String email) throws InterruptedException {
        enterFilterInviteEmail(email);
        selectFilterInviteStatus("All");
        selectTieupTypes();
        clickSearch();
    }

    // ═══════════════════════════════════════════════
    // TABLE (DataTables, 20 per page, default order Invited Date desc)
    // ═══════════════════════════════════════════════
    public List<String> getVisibleColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : driver.findElements(By.cssSelector(TABLE + " thead th")))
            if (!th.getAttribute("class").contains("never")) out.add(text(th));
        return out;
    }

    public String getInfoText() {
        List<WebElement> i = driver.findElements(By.cssSelector(".dataTables_info"));
        return i.isEmpty() ? "" : i.get(0).getText().trim();
    }

    public int getTotalEntries() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("of ([\\d,]+) entries").matcher(getInfoText());
        return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : 0;
    }

    public boolean isSearchBoxVisible() { return isShown(By.cssSelector(".dataTables_filter input[type='search']")); }
    public boolean isDownloadVisible()  { return isShown(By.cssSelector("a.buttons-csv")); }
    public boolean isPaginationVisible() { return isShown(By.cssSelector(".dataTables_paginate")); }

    public String getEmptyTableMessage() {
        List<WebElement> e = driver.findElements(By.cssSelector(TABLE + " tbody td.dataTables_empty"));
        return e.isEmpty() ? "" : e.get(0).getText().trim();
    }

    private List<WebElement> rows() {
        return driver.findElements(By.cssSelector(TABLE + " tbody tr[data-id]"));
    }

    public int getRowCount() { return rows().size(); }

    private int headerIndex(String header) {
        List<WebElement> ths = driver.findElements(By.cssSelector(TABLE + " thead th"));
        for (int i = 0; i < ths.size(); i++)
            if (text(ths.get(i)).equalsIgnoreCase(header)) return i;
        throw new IllegalArgumentException("Column not found: " + header);
    }

    public List<String> getColumnValues(String header) {
        int idx = headerIndex(header);
        List<String> out = new ArrayList<>();
        for (WebElement r : rows()) {
            List<WebElement> tds = r.findElements(By.xpath("./td"));
            if (tds.size() > idx) out.add(text(tds.get(idx)));
        }
        return out;
    }

    // ✅ Row class = tie-up type ("Co-Pay odd", "Corporate Pay even" …)
    public List<String> getRowTieupTypes() {
        List<String> out = new ArrayList<>();
        for (WebElement r : rows())
            out.add(r.getAttribute("class").replaceAll("\\b(odd|even)\\b", "").trim());
        return out;
    }

    // ✅ Tie-up type of one invite (row class, e.g. "Co-Pay")
    public String getRowTieupTypeOf(String inviteId) {
        return row(inviteId).getAttribute("class").replaceAll("\\b(odd|even)\\b", "").trim();
    }

    // ✅ Each row's cells joined by " | " (hidden Invite ID first)
    public List<String> getRowTexts() {
        List<String> out = new ArrayList<>();
        for (WebElement row : rows()) {
            List<String> cells = new ArrayList<>();
            for (WebElement td : row.findElements(By.xpath("./td"))) cells.add(text(td));
            out.add(String.join(" | ", cells));
        }
        return out;
    }

    // ✅ Invite ids (data-id) of the listed rows
    public List<String> getInviteIds() {
        List<String> out = new ArrayList<>();
        for (WebElement r : rows()) out.add(r.getAttribute("data-id"));
        return out;
    }

    private WebElement row(String inviteId) {
        return driver.findElement(By.cssSelector(TABLE + " tbody tr[data-id='" + inviteId + "']"));
    }

    public boolean isInviteListed(String inviteId) {
        return !driver.findElements(By.cssSelector(TABLE + " tbody tr[data-id='" + inviteId + "']")).isEmpty();
    }

    public String getCell(String inviteId, String header) {
        return text(row(inviteId).findElements(By.xpath("./td")).get(headerIndex(header)));
    }

    // ✅ Inline row style — "color: red" (past 72 h) / orange (past 48 h)
    public String getRowStyle(String inviteId) {
        String s = row(inviteId).getAttribute("style");
        return s == null ? "" : s.trim();
    }

    // ✅ Titles of the action links of one invite (the hidden breakup link has no title)
    public List<String> getActionTitles(String inviteId) {
        List<String> out = new ArrayList<>();
        for (WebElement a : row(inviteId).findElements(By.cssSelector("td:last-child a[title]")))
            out.add(a.getAttribute("title").trim());
        return out;
    }

    // ✅ Co-Pay breakup stored on an invite — values of the Edit Invitation popup's fields (popup left open)
    public String getRowBreakup(String inviteId) throws InterruptedException {
        clickEditInvitation(inviteId);
        Object v = js().executeScript("var o=[]; document.querySelectorAll('" + EDIT + " textarea, " + EDIT + " input[type=text]').forEach(function(e){"
                + "o.push((e.id||e.name)+'='+e.value);}); return o.join(' | ');");
        return String.valueOf(v);
    }

    // ✅ Co-Pay Requested invite: the email icon opens "Co-Pay Tieup Breakup Details" (#view-breakup-modal)
    public boolean isCoPayBreakupPopupVisible() {
        return isShown(By.cssSelector("#view-breakup-modal #breakup_details"));
    }

    public void enterCoPayBreakup(String breakup) {
        WebElement t = driver.findElement(By.cssSelector("#view-breakup-modal #breakup_details"));
        t.clear();
        t.sendKeys(breakup);
    }

    public void clickCoPayBreakupConfirm() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.cssSelector("#view-breakup-modal .modal-footer .btn-primary")));
        System.out.println("▶ Co-Pay breakup Confirm clicked");
        Thread.sleep(2000);
    }

    public String getCoPayBreakupError() {
        Object e = js().executeScript("var m=document.querySelector('#view-breakup-modal .breakup-error-msg');"
                + " return m && (m.offsetWidth||m.offsetHeight) ? m.innerText.trim() : '';");
        return String.valueOf(e);
    }

    public String getLeadDetailsHref(String inviteId) {
        return row(inviteId).findElement(By.cssSelector("a[title='Lead Details']")).getAttribute("href");
    }

    private void clickAction(String inviteId, String title) throws InterruptedException {
        hideApiDebugPanel();
        WebElement a = row(inviteId).findElement(By.cssSelector("a[title='" + title + "']"));
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", a);
        js().executeScript("arguments[0].click();", a);
        System.out.println("▶ " + title + " clicked (invite " + inviteId + ")");
        Thread.sleep(1500);
    }

    // ✅ Waits until the popup's own script has bound the Update Invite click handler
    public void clickEditInvitation(String inviteId) throws InterruptedException {
        clickAction(inviteId, "Edit Invitation");
        for (int i = 0; i < 30; i++) {
            Object bound = js().executeScript("var b=document.querySelector('" + EDIT + " #update_invite');"
                    + "if(!b || !window.jQuery) return false; var ev=jQuery._data(b,'events'); return !!(ev && ev.click);");
            if (Boolean.TRUE.equals(bound)) break;
            Thread.sleep(500);
        }
        Thread.sleep(500);
    }
    public void clickExpireInvitation(String inviteId) throws InterruptedException { clickAction(inviteId, "Expire invitation"); }
    public void clickSendInvitationEmail(String inviteId) throws InterruptedException { clickAction(inviteId, "Send Invitation Email"); }

    // ✅ Any other row action by its title (e.g. a resend icon on an Invited row)
    public void clickActionByTitle(String inviteId, String title) throws InterruptedException { clickAction(inviteId, title); }

    public void searchTable(String keyword) throws InterruptedException {
        WebElement input = driver.findElement(By.cssSelector(".dataTables_filter input[type='search']"));
        input.clear();
        input.sendKeys(keyword);
        Thread.sleep(1200);
    }

    public void clickDownload() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.cssSelector("a.buttons-csv")));
        System.out.println("▶ Download clicked");
        Thread.sleep(3000);
    }

    public boolean isNextPageEnabled() {
        return !driver.findElement(By.cssSelector(".dataTables_paginate .next")).getAttribute("class").contains("disabled");
    }

    public void clickNextPage() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.cssSelector(".dataTables_paginate .next")));
        Thread.sleep(1200);
    }

    public void clickPreviousPage() throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElement(By.cssSelector(".dataTables_paginate .previous")));
        Thread.sleep(1200);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        js().executeScript("arguments[0].click();", driver.findElements(By.cssSelector(TABLE + " thead th")).get(headerIndex(header)));
        Thread.sleep(1000);
    }

    public String getHeaderSortClass(String header) {
        return driver.findElements(By.cssSelector(TABLE + " thead th")).get(headerIndex(header)).getAttribute("class");
    }

    // ═══════════════════════════════════════════════
    // EDIT INVITATION POPUP (#frm_edit_invite in #popdown-dialog)
    // ═══════════════════════════════════════════════
    private static final String EDIT = "#popdown-dialog #frm_edit_invite";

    public boolean isEditPopupVisible() { return isShown(By.cssSelector(EDIT)); }

    public String getEditPopupText() {
        List<WebElement> d = driver.findElements(By.id("popdown-dialog"));
        return d.isEmpty() ? "" : d.get(0).getText();
    }

    public String getEditFieldValue(String id) {
        return driver.findElement(By.cssSelector(EDIT + " #" + id)).getAttribute("value");
    }

    public String getEditSelectedText(String id) {
        return new Select(driver.findElement(By.cssSelector(EDIT + " #" + id))).getFirstSelectedOption().getText().trim();
    }

    public void selectEditParentType(String type) {
        new Select(driver.findElement(By.cssSelector(EDIT + " #invite_parent_type"))).selectByVisibleText(type);
    }

    public void enterEditField(String id, String value) {
        WebElement e = driver.findElement(By.cssSelector(EDIT + " #" + id));
        e.clear();
        e.sendKeys(value);
    }

    public void clickUpdateInvite() throws InterruptedException {
        hideApiDebugPanel();
        js().executeScript("document.querySelector('" + EDIT + " #update_invite').click();");
        System.out.println("▶ Update Invite clicked");
        Thread.sleep(2000);
    }

    public List<String> getEditErrors() { return getFormErrors(EDIT); }
}
