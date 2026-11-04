package pages.Children.Updates;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// CHILDREN → UPDATES → PICKUP AUTHORIZATION (/pickup_authorization), SDA-13180 / SDA-13817
// Confirmed live 2026-11-04 as Nidhi Chaturvedi (Center Head, centre 210). See CLAUDE.md for the full DOM notes.
//  - Filters: #pickup_date (input type=date, max=today), #filter_card_type, #filter_status, #btn_submit.
//  - One box per child: .pickup-child[data-child-id]; cards .pickup-card[data-status][data-card-type].
//  - Popups (popdown, #popdown-dialog): pop_authorise_pickup, pop_handover_pickup, pop_pickup_audit_trail.
//    Messages render in #popdown-dialog .alert-ajax-response.
// ═══════════════════════════════════════════════════════════════
public class PickupAuthorizationPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public PickupAuthorizationPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    private Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    private String text(WebElement el) {
        return String.valueOf(js("return arguments[0].innerText;", el)).replaceAll("\\s+", " ").trim();
    }

    /** DOM text (not CSS-transformed): pills render uppercase on screen but are "Permanent", "Valid" in the DOM. */
    private String raw(WebElement el) {
        return String.valueOf(js("return arguments[0].textContent;", el)).replaceAll("\\s+", " ").trim();
    }

    // ════════════════════════════════════════════════════════════════════
    //  PAGE + FILTERS
    // ════════════════════════════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("frm-pickup-authorization")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Body text from the page heading "Pickup Authorization" up to the filter form. */
    public String getHeaderText() {
        return String.valueOf(js(
                "var t=document.body.innerText; var i=t.indexOf('Pickup Authorization\\n'); if(i<0) i=t.lastIndexOf('Pickup Authorization');"
                        + " var j=t.indexOf('CHOOSE DATE', i); if(j<0) j=t.indexOf('Choose Date', i); return t.substring(i, j<0? i+200 : j);"))
                .replaceAll("\\s+", " ").trim();
    }

    public String getDateValue() {
        return driver.findElement(By.id("pickup_date")).getAttribute("value");
    }

    public String getDateMax() {
        return driver.findElement(By.id("pickup_date")).getAttribute("max");
    }

    public String getDateInputType() {
        return driver.findElement(By.id("pickup_date")).getAttribute("type");
    }

    public List<String> getOptions(String selectId) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(driver.findElement(By.id(selectId))).getOptions()) {
            out.add(o.getAttribute("value") + "=" + o.getText().trim());
        }
        return out;
    }

    public String getSelectedText(String selectId) {
        return new Select(driver.findElement(By.id(selectId))).getFirstSelectedOption().getText().trim();
    }

    public boolean isCenterFilterPresent() {
        return !driver.findElements(By.cssSelector("#frm-pickup-authorization select[name*='center']")).isEmpty();
    }

    /** Sets date (yyyy-MM-dd; null = keep), card type value and status value (null = keep), then Filter. */
    public void filter(String isoDate, String cardTypeValue, String statusValue) throws InterruptedException {
        if (isoDate != null) {
            js("var d=document.getElementById('pickup_date'); d.value=arguments[0]; d.dispatchEvent(new Event('change',{bubbles:true}));", isoDate);
        }
        if (cardTypeValue != null) new Select(driver.findElement(By.id("filter_card_type"))).selectByValue(cardTypeValue);
        if (statusValue != null) new Select(driver.findElement(By.id("filter_status"))).selectByValue(statusValue);
        WebElement btn = driver.findElement(By.id("btn_submit"));
        js("arguments[0].click();", btn);
        Thread.sleep(3000);
        isPageLoaded();
    }

    // ════════════════════════════════════════════════════════════════════
    //  CHILD BOXES + CARDS
    // ════════════════════════════════════════════════════════════════════
    public List<String> getChildIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement b : driver.findElements(By.cssSelector("#pickup_request_cards .pickup-child"))) {
            ids.add(b.getAttribute("data-child-id"));
        }
        return ids;
    }

    public boolean isChildShown(String childId) {
        return !driver.findElements(childBox(childId)).isEmpty();
    }

    private By childBox(String childId) {
        return By.cssSelector("#pickup_request_cards .pickup-child[data-child-id='" + childId + "']");
    }

    public String getChildHeading(String childId) {
        return text(driver.findElement(childBox(childId)).findElement(By.cssSelector(".clx-card-head h3")));
    }

    public String getChildNotes(String childId) {
        List<WebElement> n = driver.findElement(childBox(childId)).findElements(By.cssSelector(".clx-pa-child-notes"));
        return n.isEmpty() ? "" : text(n.get(0));
    }

    public String getChildBoxText(String childId) {
        return text(driver.findElement(childBox(childId)));
    }

    /** One map per card in the child's box: type, status, name, relation, meta, actions, statusPill. */
    public List<Map<String, String>> getCards(String childId) {
        List<Map<String, String>> cards = new ArrayList<>();
        for (WebElement c : driver.findElement(childBox(childId)).findElements(By.cssSelector(".pickup-card"))) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("type", c.getAttribute("data-card-type"));
            m.put("status", c.getAttribute("data-status"));
            List<WebElement> pills = c.findElements(By.cssSelector(":scope > .clx-pill"));
            m.put("typePill", pills.size() > 0 ? raw(pills.get(0)) : "");
            m.put("statusPill", pills.size() > 1 ? raw(pills.get(1)) : "");
            m.put("cardClass", c.getAttribute("class"));
            m.put("name", raw(c.findElement(By.cssSelector(".clx-pa-person h4"))));
            List<WebElement> tag = c.findElements(By.cssSelector(".clx-pa-person .clx-tag"));
            m.put("relation", tag.isEmpty() ? "" : raw(tag.get(0)));
            List<WebElement> meta = c.findElements(By.cssSelector(".clx-pa-meta"));
            m.put("meta", meta.isEmpty() ? "" : raw(meta.get(0)));
            List<WebElement> act = c.findElements(By.cssSelector(".clx-pa-actions"));
            m.put("actions", act.isEmpty() ? "" : raw(act.get(0)));
            m.put("photo", c.findElements(By.cssSelector(".clx-pa-person img")).isEmpty() ? ""
                    : c.findElement(By.cssSelector(".clx-pa-person img")).getAttribute("src"));
            cards.add(m);
        }
        return cards;
    }

    public Map<String, String> getCardByName(String childId, String visitorName) {
        for (Map<String, String> c : getCards(childId)) {
            if (c.get("name").equalsIgnoreCase(visitorName)) return c;
        }
        return null;
    }

    private WebElement cardEl(String childId, String visitorName) {
        for (WebElement c : driver.findElement(childBox(childId)).findElements(By.cssSelector(".pickup-card"))) {
            if (raw(c.findElement(By.cssSelector(".clx-pa-person h4"))).equalsIgnoreCase(visitorName)) return c;
        }
        throw new IllegalStateException("No card '" + visitorName + "' for child " + childId);
    }

    /** The card's action link (Authorise / Mark Hand Over), or null. */
    private WebElement actionLink(String childId, String visitorName, String hrefPrefix) {
        List<WebElement> a = cardEl(childId, visitorName).findElements(By.cssSelector(".clx-pa-actions a[href^='" + hrefPrefix + "']"));
        return a.isEmpty() ? null : a.get(0);
    }

    public boolean isAuthoriseShown(String childId, String visitorName) {
        return actionLink(childId, visitorName, "pop_authorise_pickup") != null;
    }

    public boolean isHandOverShown(String childId, String visitorName) {
        return actionLink(childId, visitorName, "pop_handover_pickup") != null;
    }

    /** A faded (unusable) action is rendered without a link, or with a disabled class — report what is there. */
    public String getActionHtml(String childId, String visitorName) {
        List<WebElement> act = cardEl(childId, visitorName).findElements(By.cssSelector(".clx-pa-actions"));
        return act.isEmpty() ? "" : String.valueOf(js("return arguments[0].innerHTML;", act.get(0))).replaceAll("\\s+", " ").trim();
    }

    public String getAuthoriseHref(String childId, String visitorName) {
        WebElement a = actionLink(childId, visitorName, "pop_authorise_pickup");
        return a == null ? "" : a.getAttribute("href");
    }

    public String getHandOverHref(String childId, String visitorName) {
        WebElement a = actionLink(childId, visitorName, "pop_handover_pickup");
        return a == null ? "" : a.getAttribute("href");
    }

    public boolean isAuditTrailButtonShown(String childId) {
        return !driver.findElement(childBox(childId)).findElements(By.cssSelector("a[href^='pop_pickup_audit_trail']")).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════
    //  POPUPS (popdown)
    // ════════════════════════════════════════════════════════════════════
    private void clickAndWaitPopup(WebElement link, String rootCss) {
        js("arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", link);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#popdown-dialog " + rootCss)));
    }

    public void openAuthorise(String childId, String visitorName) {
        clickAndWaitPopup(actionLink(childId, visitorName, "pop_authorise_pickup"), ".authorise-pickup");
    }

    public void openHandOver(String childId, String visitorName) {
        clickAndWaitPopup(actionLink(childId, visitorName, "pop_handover_pickup"), ".handover-pickup");
    }

    public void openAuditTrail(String childId) {
        clickAndWaitPopup(driver.findElement(childBox(childId)).findElement(By.cssSelector("a[href^='pop_pickup_audit_trail']")),
                ".pickup-audit-trail");
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#pickup_audit_events_info")));
        } catch (Exception ignored) {
        }
    }

    /** Opens a popup URL directly (relative, e.g. a copied/tampered link) inside the page's popdown. */
    public void openPopupUrl(String relativeUrl, String rootCss) {
        // point an existing popdown link (View Audit Trail) at the copied URL; an injected anchor is not wired up
        WebElement a = driver.findElement(By.cssSelector("#pickup_request_cards a[href^='pop_pickup_audit_trail']"));
        js("arguments[0].setAttribute('href', arguments[1]); arguments[0].scrollIntoView({block:'center'});", a, relativeUrl);
        a.click();
        // a refused link may render only a message, so wait for the popup itself, not a specific body
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("popdown-dialog")));
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !getPopupText().isEmpty());
            Thread.sleep(1500);
        } catch (Exception ignored) {
        }
    }

    public boolean isPopupOpen() {
        List<WebElement> p = driver.findElements(By.id("popdown-dialog"));
        return !p.isEmpty() && p.get(0).isDisplayed();
    }

    public String getPopupText() {
        return text(driver.findElement(By.id("popdown-dialog")));
    }

    public String getPopupTitle() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog .clx-sheet-head h3")));
    }

    /** flash_message renders a sibling <div class="flash-msg"><div class="alert …"> after .alert-ajax-response; read the newest one. */
    public String getPopupMessage() {
        List<WebElement> m = driver.findElements(By.cssSelector("#popdown-dialog .flash-msg .alert, #popdown-dialog .alert-ajax-response .alert"));
        for (int i = m.size() - 1; i >= 0; i--) {
            String t = text(m.get(i));
            if (!t.isEmpty()) return t.replaceFirst("^×\\s*", "");
        }
        List<WebElement> a = driver.findElements(By.cssSelector("#popdown-dialog .alert-ajax-response"));
        return a.isEmpty() ? "" : text(a.get(0));
    }

    /** Waits for a non-empty popup message, or for the page to reload (popup gone), and returns the last message seen. */
    public String waitForPopupMessage(int seconds) throws InterruptedException {
        String last = "";
        for (int i = 0; i < seconds * 2; i++) {
            try {
                String m = getPopupMessage();
                if (!m.isEmpty()) return m;
            } catch (Exception ignored) {
            }
            Thread.sleep(500);
        }
        return last;
    }

    public void closePopup() throws InterruptedException {
        List<WebElement> x = driver.findElements(By.cssSelector("#popdown-dialog .clx-x.close-popdown"));
        if (!x.isEmpty()) js("arguments[0].click();", x.get(0));
        Thread.sleep(1500);
        js("var o=document.getElementById('popdown-opacity'); if(o) o.style.display='none';");
    }

    /** After a successful action the page reloads by itself after 2 s; waits for the reloaded page. */
    public void waitForReload() throws InterruptedException {
        Thread.sleep(4000);
        isPageLoaded();
    }

    // ── Hand Over popup
    public boolean isIdentityTickPresent() {
        return !driver.findElements(By.cssSelector("#popdown-dialog #identity_verified")).isEmpty();
    }

    public String getIdentityTickLabel() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog #identity_verified")).findElement(By.xpath("./ancestor::label[1]")));
    }

    public boolean isIdentityTicked() {
        return driver.findElement(By.cssSelector("#popdown-dialog #identity_verified")).isSelected();
    }

    public void tickIdentity() {
        WebElement c = driver.findElement(By.cssSelector("#popdown-dialog #identity_verified"));
        if (!c.isSelected()) c.click();
    }

    public void untickIdentity() {
        WebElement c = driver.findElement(By.cssSelector("#popdown-dialog #identity_verified"));
        if (c.isSelected()) c.click();
    }

    public boolean isHandOverButtonEnabled() {
        return driver.findElement(By.cssSelector("#popdown-dialog #btn_handover")).isEnabled();
    }

    public void clickHandOverButton() {
        WebElement b = driver.findElement(By.cssSelector("#popdown-dialog #btn_handover"));
        js("arguments[0].scrollIntoView({block:'center'});", b);
        b.click();
    }

    /** Clicks Mark Hand Over even while it is disabled (to check the client-side guard message). */
    public void forceClickHandOverButton() {
        js("var b=arguments[0]; b.disabled=false; b.click();", driver.findElement(By.cssSelector("#popdown-dialog #btn_handover")));
    }

    // ── Authorise popup
    public List<String> getRecipientOptions() {
        List<String> out = new ArrayList<>();
        for (WebElement r : driver.findElements(By.cssSelector("#popdown-dialog .recipient-type"))) {
            out.add(r.getAttribute("value") + " " + text(r.findElement(By.xpath("./ancestor::label[1]"))));
        }
        return out;
    }

    public int countFreeTextInputsInRecipients() {
        return driver.findElements(By.cssSelector("#popdown-dialog .clx-pa-recipients input:not([type='radio'])")).size();
    }

    public void selectRecipient(String parentType) {
        js("arguments[0].click();", driver.findElement(By.cssSelector("#popdown-dialog .recipient-type[value='" + parentType + "']")));
    }

    public boolean isSendOtpEnabled() {
        return driver.findElement(By.cssSelector("#popdown-dialog #btn_send_otp")).isEnabled();
    }

    public void clickSendOtp() {
        js("arguments[0].click();", driver.findElement(By.cssSelector("#popdown-dialog #btn_send_otp")));
    }

    public boolean isOtpSectionVisible() {
        List<WebElement> s = driver.findElements(By.cssSelector("#popdown-dialog .otp-section"));
        return !s.isEmpty() && s.get(0).isDisplayed();
    }

    public int getOtpBoxCount() {
        return driver.findElements(By.cssSelector("#popdown-dialog .otp-digit")).size();
    }

    public String getExpiresIn() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog #otp_expires_in")));
    }

    public String getExpiresInClass() {
        return driver.findElement(By.cssSelector("#popdown-dialog #otp_expires_in")).getAttribute("class");
    }

    public String getAttemptsLeft() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog #otp_attempts_left")));
    }

    public String getResendText() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog #btn_resend_otp")));
    }

    public boolean isResendEnabled() {
        return driver.findElement(By.cssSelector("#popdown-dialog #btn_resend_otp")).isEnabled();
    }

    public void clickResend() {
        js("arguments[0].click();", driver.findElement(By.cssSelector("#popdown-dialog #btn_resend_otp")));
    }

    /** Types the code one digit per box (real keystrokes, so the page's keyup handler moves focus and enables Verify). */
    public void enterOtp(String code) {
        List<WebElement> boxes = driver.findElements(By.cssSelector("#popdown-dialog .otp-digit"));
        for (WebElement b : boxes) {
            b.clear();
        }
        for (int i = 0; i < boxes.size() && i < code.length(); i++) {
            boxes.get(i).sendKeys(String.valueOf(code.charAt(i)));
        }
    }

    public String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (WebElement b : driver.findElements(By.cssSelector("#popdown-dialog .otp-digit"))) sb.append(b.getAttribute("value"));
        return sb.toString();
    }

    /** Clicks Verify & Authorise and waits (≤ 20 s) for a result message that differs from the one shown before. */
    public String verifyAndWaitForResult() throws InterruptedException {
        String before = "";
        try { before = getPopupMessage(); } catch (Exception ignored) { }
        String attemptsBefore = "";
        try { attemptsBefore = getAttemptsLeft(); } catch (Exception ignored) { }
        clickVerify();
        for (int i = 0; i < 100; i++) {
            Thread.sleep(200);
            String m = "";
            try {
                m = getPopupMessage();
            } catch (Exception e) {
                continue; // message element being replaced, or the page is reloading
            }
            if (!m.isEmpty() && !m.equals(before)) return m;
            try {
                String a = getAttemptsLeft();
                if (!a.equals(attemptsBefore) && i > 10) {
                    return "(message faded) attempts left " + a;
                }
            } catch (Exception ignored) {
            }
        }
        try { return getPopupMessage(); } catch (Exception e) { return ""; }
    }

    public boolean isVerifyEnabled() {
        return driver.findElement(By.cssSelector("#popdown-dialog #btn_verify_otp")).isEnabled();
    }

    public void clickVerify() {
        js("arguments[0].click();", driver.findElement(By.cssSelector("#popdown-dialog #btn_verify_otp")));
    }

    // ── Audit Trail popup
    public String getAuditHeaderText() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog .clx-pa-head")));
    }

    public Map<String, String> getAuditSummary() {
        Map<String, String> m = new LinkedHashMap<>();
        for (WebElement d : driver.findElements(By.cssSelector("#popdown-dialog dl.clx-pa-summary > div"))) {
            m.put(raw(d.findElement(By.tagName("dt"))).toUpperCase(), raw(d.findElement(By.tagName("dd"))));
        }
        return m;
    }

    public List<String> getAuditColumns() {
        List<String> cols = new ArrayList<>();
        for (WebElement th : driver.findElements(By.cssSelector("#popdown-dialog #pickup_audit_events thead th"))) {
            cols.add(String.valueOf(js("return arguments[0].textContent;", th)).trim());
        }
        return cols;
    }

    /** All audit rows (every page) as maps column → text, via the DataTable API. */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getAuditRows() {
        Object rows = js("try{ $('#pickup_audit_events').DataTable().page.len(-1).draw(); }catch(e){}"
                + " var cols=[]; document.querySelectorAll('#popdown-dialog #pickup_audit_events thead th').forEach(function(th){cols.push(th.textContent.trim());});"
                + " var out=[]; document.querySelectorAll('#popdown-dialog #pickup_audit_events tbody tr').forEach(function(tr){ var td=tr.querySelectorAll('td');"
                + " if(td.length < cols.length) return; var r={}; for(var i=0;i<cols.length;i++){ r[cols[i]]=td[i].textContent.replace(/\\s+/g,' ').trim(); } out.push(r); });"
                + " return out;");
        List<Map<String, String>> out = new ArrayList<>();
        for (Object o : (List<Object>) rows) {
            Map<String, String> m = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) m.put(e.getKey(), String.valueOf(e.getValue()));
            out.add(m);
        }
        return out;
    }

    public String getAuditInfo() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog #pickup_audit_events_info")));
    }

    public void searchAudit(String text) throws InterruptedException {
        WebElement s = driver.findElement(By.cssSelector("#popdown-dialog #pickup_audit_events_filter input"));
        s.clear();
        s.sendKeys(text);
        Thread.sleep(1000);
    }

    public int getAuditVisibleRowCount() {
        return driver.findElements(By.cssSelector("#popdown-dialog #pickup_audit_events tbody tr")).size();
    }

    public String getAuditFirstVisibleRowText() {
        List<WebElement> r = driver.findElements(By.cssSelector("#popdown-dialog #pickup_audit_events tbody tr"));
        return r.isEmpty() ? "" : text(r.get(0));
    }

    public boolean isAuditDownloadShown() {
        return !driver.findElements(By.cssSelector("#popdown-dialog a.buttons-csv")).isEmpty();
    }

    public String getAuditDownloadText() {
        return text(driver.findElement(By.cssSelector("#popdown-dialog a.buttons-csv")));
    }

    public void clickAuditDownload() {
        js("arguments[0].click();", driver.findElement(By.cssSelector("#popdown-dialog a.buttons-csv")));
    }

    /** Whether any audit cell (Event etc.) offers an edit / delete control. */
    public int countAuditEditControls() {
        return driver.findElements(By.cssSelector("#popdown-dialog #pickup_audit_events tbody a, "
                + "#popdown-dialog #pickup_audit_events tbody button, #popdown-dialog #pickup_audit_events tbody input")).size();
    }
}
