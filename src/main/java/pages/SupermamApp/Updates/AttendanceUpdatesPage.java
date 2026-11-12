package pages.SupermamApp.Updates;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ═══════════════════════════════════════════════════════════════
// SUPERMAM APP → UPDATES → ATTENDANCE (/attendance_updates)
// Confirmed live (2026-11-04, as Nidhi Chaturvedi):
//  - Room select[name=set_room_id] (select2; -1 = "All Rooms" → "Please select a room!"), GO submits the form.
//  - One row per child: buttons button.attendance_button.present / .checkout / .absent, all with
//    id="attendance_button_<childId>", inside #td_<childId>. A used button becomes disabled.
//  - CHECK IN → native confirm "Are you sure want to mark child Present?" → row text
//    "Check In [November 04, 2026 13:34 PM]" in #checkin_attendance_<childId>.
//  - Check Out / Absent text in #checkout_attendance_<childId> / #absent_attendance_<childId>.
// ═══════════════════════════════════════════════════════════════
public class AttendanceUpdatesPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public AttendanceUpdatesPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    private Object js(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("select[name='set_room_id']")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Room id → room name, without "All Rooms". */
    public Map<String, String> getRooms() {
        Map<String, String> rooms = new LinkedHashMap<>();
        for (WebElement o : driver.findElements(By.cssSelector("select[name='set_room_id'] option"))) {
            String v = o.getAttribute("value");
            if (v != null && !v.equals("-1")) rooms.put(v, o.getAttribute("textContent").trim());
        }
        return rooms;
    }

    /** Selects a room and submits (GO). The select is select2, so the value is set on the hidden select. */
    public void openRoom(String roomId) throws InterruptedException {
        js("var s=document.querySelector(\"select[name='set_room_id']\"); s.value=arguments[0]; s.form.submit();", roomId);
        Thread.sleep(3000);
        isPageLoaded();
    }

    /** Children listed in the current room. */
    public List<String> getChildIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement b : driver.findElements(By.cssSelector("button.attendance_button.present"))) {
            ids.add(b.getAttribute("id").replace("attendance_button_", ""));
        }
        return ids;
    }

    /** Children with no attendance today (CHECK IN still enabled). */
    public List<String> getUnmarkedChildIds() {
        List<String> ids = new ArrayList<>();
        for (WebElement b : driver.findElements(By.cssSelector("button.attendance_button.present"))) {
            if (b.isEnabled()) ids.add(b.getAttribute("id").replace("attendance_button_", ""));
        }
        return ids;
    }

    /** Children marked Present today and still in the centre (Check In, no Check Out, not Absent). */
    public List<String> getPresentChildIds() {
        List<String> ids = new ArrayList<>();
        for (String id : getChildIds()) {
            if (!getCheckInText(id).isEmpty() && getCheckOutText(id).isEmpty() && getAbsentText(id).isEmpty()) ids.add(id);
        }
        return ids;
    }

    public boolean isChildListed(String childId) {
        return !driver.findElements(By.id("td_" + childId)).isEmpty();
    }

    public String getChildName(String childId) {
        List<WebElement> el = driver.findElements(By.id("child_name_" + childId));
        return el.isEmpty() ? "" : el.get(0).getAttribute("value").trim();
    }

    private WebElement button(String childId, String cls) {
        return driver.findElement(By.cssSelector("#td_" + childId + " button.attendance_button." + cls));
    }

    public boolean isCheckInEnabled(String childId) {
        return button(childId, "present").isEnabled();
    }

    public boolean isCheckOutEnabled(String childId) {
        return button(childId, "checkout").isEnabled();
    }

    public boolean isAbsentEnabled(String childId) {
        return button(childId, "absent").isEnabled();
    }

    /** CHECK IN + accept the native confirm. Returns the confirm text (blank if none appeared). */
    public String checkIn(String childId) throws InterruptedException {
        js("arguments[0].click();", button(childId, "present"));
        String text = "";
        try {
            Alert a = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.alertIsPresent());
            text = a.getText();
            a.accept();
        } catch (Exception ignored) {
        }
        for (int i = 0; i < 20 && getCheckInText(childId).isEmpty(); i++) Thread.sleep(500);
        return text;
    }

    private String spanText(String id) {
        List<WebElement> el = driver.findElements(By.id(id));
        return el.isEmpty() ? "" : String.valueOf(js("return arguments[0].textContent;", el.get(0))).trim();
    }

    public String getCheckInText(String childId) {
        return spanText("checkin_attendance_" + childId);
    }

    public String getCheckOutText(String childId) {
        return spanText("checkout_attendance_" + childId);
    }

    public String getAbsentText(String childId) {
        return spanText("absent_attendance_" + childId);
    }
}
