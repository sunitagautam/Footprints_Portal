package pages.Support;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object: ApproveTransportPage — screen: process_child_transport
 * <p>
 * Landed on after clicking the Approve link for a Start Transport 1/2 Way row
 * on Recent Customer Requests:
 * https://test-franchise.footprintseducation.in/process_child_transport
 * ?request_id=&lt;id&gt;&request_type=Add%20One%20Way%20Transport&child_id=&lt;child_id&gt;
 * &amp;center_id=&lt;id&gt;&amp;assign_route=1&amp;show_addon=1
 * <p>
 * CONFIRMED live (user-supplied DOM, 2026-08-13) — the real form is a
 * progressive-disclosure sequence, much simpler than the spec's own "Transport
 * Type / Route / Trip / Location map with draggable bus icon" description
 * suggested (no separate "Trip" field exists). Confirmed field order:
 * <ol>
 * <li>transport_type (select, id="transport_type") — "--Choose--" / "Pick-Up" / "Drop"</li>
 * <li>addon (select, id="addon") — pricing plan, e.g. "One Way Transport - 1750"</li>
 * <li>pickup_route_id / drop_route_id (select) — route, options inside an &lt;optgroup label="Active"&gt;
 * ("drop_route_id" is assumed to exist analogously for Drop, by naming symmetry — unconfirmed)</li>
 * <li>ptid (readonly input, id="ptid", class="pickatime-format") — Pickup Time, a pickatime.js widget
 * (sibling of the pickadate.js widgets used elsewhere in this repo)</li>
 * <li>Location — Google Places autocomplete text input (id="map_canvas_route_autocomplete",
 * placeholder "Enter Address/Location") → pick a suggestion → double-click the map's bus-icon
 * marker → click the "Set &lt;Pickup/Drop&gt; Location" button (id="send_map_sms")</li>
 * <li>WEF Date — calendar-icon input, exact element id still UNCONFIRMED</li>
 * <li>"Approve" button (not "Submit")</li>
 * </ol>
 * The double-click-marker step is the least certain part of this page object — Google Maps
 * renders its marker as a plain positioned &lt;img&gt; (usemap="#gmimap&lt;N&gt;", N varies per map
 * instance), so the selector below is a best-effort match and may need a live correction pass.
 */
public class ApproveTransportPage {

    WebDriver driver;
    WebDriverWait wait;

    public ApproveTransportPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public boolean isFormVisible() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("transport_type")));
            return true;
        } catch (Exception e) {
            System.out.println("⚠ Approve Transport form not detected: " + e.getMessage());
            return false;
        }
    }

    /**
     * @param visibleText "Pick-Up" or "Drop" — confirmed exact option text.
     */
    public void selectTransportType(String visibleText) throws InterruptedException {
        WebElement dropdown = driver.findElement(By.id("transport_type"));
        wait.until(ExpectedConditions.visibilityOf(dropdown));
        new Select(dropdown).selectByVisibleText(visibleText);
        System.out.println("✅ Transport Type: " + visibleText);
        Thread.sleep(800);
    }

    /**
     * Selects a pricing plan from the "Available Plans" dropdown (id="addon"),
     * e.g. "One Way Transport - 1750". Falls back to the first non-placeholder
     * option if the exact plan name isn't found/known.
     */
    public String selectAddonPlan(String visibleText) throws InterruptedException {
        WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("addon")));
        Select select = new Select(dropdown);
        if (visibleText != null) {
            try {
                select.selectByVisibleText(visibleText);
                System.out.println("✅ Addon plan: " + visibleText);
                Thread.sleep(500);
                return visibleText;
            } catch (Exception ignored) {
                // fall through to first-available
            }
        }
        String chosen = selectFirstNonPlaceholder(select, "--Select--");
        System.out.println("✅ Addon plan (first available): " + chosen);
        Thread.sleep(500);
        return chosen;
    }

    /**
     * Selects a route in the dropdown that appears after choosing an addon
     * plan — id is "pickup_route_id" for Pick-Up, "drop_route_id" for Drop
     * (the latter by naming symmetry, unconfirmed). Falls back to the first
     * non-placeholder option if the exact route name isn't found/known.
     */
    public String selectRoute(String transportTypeValue, String visibleText) throws InterruptedException {
        String routeSelectId = transportTypeValue.equalsIgnoreCase("drop") ? "drop_route_id" : "pickup_route_id";
        WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(routeSelectId)));
        Select select = new Select(dropdown);
        if (visibleText != null) {
            try {
                select.selectByVisibleText(visibleText);
                System.out.println("✅ Route: " + visibleText);
                Thread.sleep(500);
                return visibleText;
            } catch (Exception ignored) {
                // fall through to first-available
            }
        }
        String chosen = selectFirstNonPlaceholder(select, "Select Route");
        System.out.println("✅ Route (first available): " + chosen);
        Thread.sleep(500);
        return chosen;
    }

    private String selectFirstNonPlaceholder(Select select, String placeholderText) {
        for (WebElement opt : select.getOptions()) {
            String txt = opt.getText().trim();
            if (!txt.isEmpty() && !txt.equalsIgnoreCase(placeholderText) && !txt.equalsIgnoreCase("--Choose--")) {
                select.selectByVisibleText(txt);
                return txt;
            }
        }
        throw new RuntimeException("No non-placeholder option available to select");
    }

    /**
     * Pickup Time — readonly pickatime.js widget (id="ptid"). Sets the raw
     * input value + fires change/input events via JS, same bypass technique
     * used for pickadate.js date fields elsewhere in this repo (readonly
     * widgets don't accept direct sendKeys()).
     *
     * @param time e.g. "09:00 AM"
     */
    public void setPickupTime(String time) throws InterruptedException {
        WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("ptid")));
        setReadonlyFieldByJs(field, time);
        System.out.println("✅ Pickup Time set: " + time);
    }

    /**
     * Types an address into the Google Places autocomplete field and selects
     * the first suggestion from the dropdown it opens.
     */
    public void enterLocationAddress(String address) throws InterruptedException {
        // The Google Places autocomplete input renders asynchronously after
        // the route is selected — confirmed live it can take a few seconds,
        // but reproducibly exceeded the original 30s wait twice in a row
        // under one session (Nidhi Chaturvedi) even on a route that loaded
        // reliably under another (Jaydeep Kar) — widened to 60s rather than
        // assume it's pure randomness.
        WebElement field = new WebDriverWait(driver, Duration.ofSeconds(60))
                .until(ExpectedConditions.visibilityOfElementLocated(
                        By.id("map_canvas_route_autocomplete")));
        field.clear();
        field.sendKeys(address);
        Thread.sleep(1500); // Google Places autocomplete debounce
        try {
            WebElement firstSuggestion = new WebDriverWait(driver, Duration.ofSeconds(8))
                    .until(ExpectedConditions.elementToBeClickable(By.cssSelector(".pac-container .pac-item")));
            firstSuggestion.click();
            System.out.println("✅ Location address entered + first suggestion selected: " + address);
        } catch (Exception e) {
            System.out.println("⚠ No .pac-item autocomplete suggestion appeared for: " + address
                    + " (" + e.getMessage() + ")");
        }
        Thread.sleep(800);
    }

    /**
     * Double-clicks the map's bus-icon marker to drop the pin at the
     * autocompleted location. Google Maps renders the marker as a plain
     * positioned &lt;img&gt; with a usemap attribute (usemap="#gmimap&lt;N&gt;",
     * N varies per map instance) — best-effort selector, most fragile part
     * of this page object.
     */
    public void doubleClickMapMarker() throws InterruptedException {
        List<WebElement> markers = driver.findElements(By.cssSelector("img[usemap^='#gmimap']"));
        if (markers.isEmpty()) {
            throw new RuntimeException("Map marker (bus icon) not found — selector needs live correction");
        }
        WebElement marker = markers.get(markers.size() - 1); // most recently rendered marker
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", marker);
        Thread.sleep(300);
        new Actions(driver).doubleClick(marker).perform();
        System.out.println("✅ Map marker double-clicked");
        Thread.sleep(500);
    }

    /**
     * Clicks the "Set Pickup/Drop Location" button (id="send_map_sms") to
     * confirm the location chosen via the map.
     */
    public void confirmMapLocation() throws InterruptedException {
        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(By.id("send_map_sms")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);
        System.out.println("✅ Set Location button clicked");
        Thread.sleep(800);
    }

    /**
     * WEF Date — CONFIRMED live id="transport_date", a pickadate.js widget
     * (class="picker__input", aria-owns="transport_date_root transport_date_hidden")
     * — the SAME widget type that, per CLAUDE.md's Withdraw Child notes,
     * silently corrupted the submitted record when set via raw JS value
     * injection (looked correct in the DOM but the backend received a broken
     * payload). So this drives the real widget via
     * Regular_ServiceRequests' proven openCalendarFor()/clickCalendarDay()
     * helpers instead of JS injection, and defaults to TODAY (not a future
     * date) so processChildApprovedRequest can actually process it afterward.
     */
    public void setWefDateToToday() throws InterruptedException {
        WebElement field = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("transport_date")));
        Regular_ServiceRequests calendarHelper = new Regular_ServiceRequests(driver);
        calendarHelper.openCalendarFor(field);
        int today = java.time.LocalDate.now().getDayOfMonth();
        boolean clicked = calendarHelper.clickCalendarDay(today);
        if (!clicked) {
            calendarHelper.closeCalendar();
            throw new RuntimeException("Could not click today's (" + today + ") day cell on WEF Date calendar");
        }
        System.out.println("✅ WEF Date set to today (" + today + ")");
    }

    /**
     * CONFIRMED live: id="approve_transport", class="approve btn btn-primary",
     * with request_id/request_type as plain HTML attributes:
     * &lt;button type="button" name="approve_transport" class="approve btn
     * btn-primary" id="approve_transport" request_id="166259" request_type=
     * "Add One Way Transport"&gt;Approve&lt;/button&gt;
     */
    public void approve() throws InterruptedException {
        WebElement approveBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("approve_transport")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", approveBtn);
        System.out.println("▶ Approve Transport form — Approve clicked");
        Thread.sleep(800);

        // CONFIRMED live (user-supplied screenshot): clicking Approve triggers a
        // native confirm() — "Changes will be applicable today onwards, sure
        // want to update status of transport request?" — must be accepted or
        // every subsequent WebDriver call throws UnhandledAlertException (same
        // hazard as the Withdraw Child approve/update native confirms).
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.alertIsPresent());
            String alertText = driver.switchTo().alert().getText();
            System.out.println("▶ Alert: " + alertText);
            driver.switchTo().alert().accept();
            System.out.println("✅ Alert accepted");
        } catch (Exception e) {
            System.out.println("⚠ No confirm() alert appeared after Approve: " + e.getMessage());
        }
        Thread.sleep(1000);
    }

    /**
     * Generic readonly-widget value setter (pickatime.js etc.) — removes
     * readonly, sets the value, fires change/input/blur so any attached
     * widget picks up the new value.
     */
    private void setReadonlyFieldByJs(WebElement field, String value) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0]; var v = arguments[1];" +
                        "el.removeAttribute('readonly');" +
                        "el.value = v;" +
                        "el.dispatchEvent(new Event('change', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('input', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('blur', {bubbles:true}));",
                field, value);
        Thread.sleep(500);
    }

    /**
     * Pickaday-style JS date setter — same technique as
     * Regular_ServiceRequests' private setDateByJs, duplicated here since
     * that one isn't accessible across page objects.
     */
    private void setDateByJs(WebElement field, String date) throws InterruptedException {
        ((JavascriptExecutor) driver).executeScript("arguments[0].removeAttribute('readonly');", field);
        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", field, date);
        ((JavascriptExecutor) driver).executeScript(
                "var el = arguments[0]; var dateStr = arguments[1]; var d = new Date(dateStr);" +
                        "if (el._picker && typeof el._picker.setDate === 'function') { el._picker.setDate(d, true); }" +
                        "else if (el._picker && typeof el._picker.set === 'function') { el._picker.set('select', d); }" +
                        "else if (typeof $ !== 'undefined' && $(el).data('pickadate')) { $(el).pickadate('picker').set('select', d); }" +
                        "else if (typeof $ !== 'undefined' && $(el).data('datepicker')) { $(el).datepicker('setDate', d); }" +
                        "el.dispatchEvent(new Event('change', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('input', {bubbles:true}));" +
                        "el.dispatchEvent(new Event('blur', {bubbles:true}));",
                field, date);
        System.out.println("✅ WEF Date set: " + date);
        Thread.sleep(600);
    }

    /**
     * Diagnostic dump — call on failure to inspect real field ids/names and
     * correct the selectors above, same pattern as OneTimeChargesPage's
     * dumpModalHtml().
     */
    public String dumpPageSource() {
        try {
            String html = driver.findElement(By.tagName("body")).getAttribute("innerHTML");
            System.out.println("═══ Approve Transport page source (first 5000 chars) ═══");
            System.out.println(html.substring(0, Math.min(5000, html.length())));
            return html;
        } catch (Exception e) {
            return "";
        }
    }
}
