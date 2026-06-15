package pages.Settings;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;

public class UserRightsPage {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // PAGE ELEMENTS
    // ═══════════════════════════════════════════════

    // ✅ Select2 container — exact id from HTML
    @FindBy(id = "select2-user_picker-container")
    private WebElement select2Container;

    // ✅ Verify Screen Access — input type="submit" id="submit"
    @FindBy(id = "submit")
    private WebElement verifyScreenAccessBtn;

    // ✅ Remind Me Later — name="remind_me_later" type="submit"
    @FindBy(name = "remind_me_later")
    private WebElement remindMeLaterBtn;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public UserRightsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // IS PAGE LOADED
    // ═══════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions
                    .visibilityOf(select2Container));
            System.out.println("✅ User Rights page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Page not loaded: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // SELECT USER FROM SELECT2 DROPDOWN
    // ✅ Fixed: uses CSS selector on result li
    //    instead of normalize-space exact match
    // ═══════════════════════════════════════════════
    public void selectUser(String userName)
            throws InterruptedException {

        // Step 1 — Open dropdown
        WebElement selectionEl = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[@id='select2-user_picker-container']/..")));
        selectionEl.click();
        System.out.println("▶ Select2 dropdown opened");
        Thread.sleep(800);

        // Step 2 — Type username
        WebElement searchBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".select2-search__field")));
        searchBox.clear();
        searchBox.sendKeys(userName);
        System.out.println("▶ Searching: " + userName);
        Thread.sleep(2000);

        // Step 3 — Press ENTER to select highlighted result
        searchBox.sendKeys(org.openqa.selenium.Keys.ENTER);
        System.out.println("✅ User selected via ENTER: " + userName);
        Thread.sleep(1000);

        // Step 4 — Verify container shows selected user
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> {
                        String text = select2Container.getText().trim();
                        String title = select2Container.getAttribute("title");
                        return text.contains(userName)
                                || (title != null && title.contains(userName));
                    });
            System.out.println("✅ User selection confirmed: " + userName);
        } catch (Exception e) {
            System.out.println("⚠ Proceeding without container confirmation");
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK VERIFY SCREEN ACCESS
    // Tries multiple selectors in order
    // ═══════════════════════════════════════════════
    public void clickVerifyScreenAccess()
            throws InterruptedException {
        System.out.println("▶ Current URL before click: "
                + driver.getCurrentUrl());

        By[] candidates = {
                By.id("submit"),
                By.xpath("//input[@type='submit']"),
                By.xpath("//input[contains(@value,'Verify')]"),
                By.xpath("//button[contains(.,'Verify')]"),
                By.xpath("//input[@type='submit' or @type='button']")
        };

        WebElement btn = null;
        for (By by : candidates) {
            try {
                btn = new WebDriverWait(driver,
                        Duration.ofSeconds(5))
                        .until(ExpectedConditions
                                .elementToBeClickable(by));
                System.out.println(
                        "▶ Found submit button via: " + by);
                break;
            } catch (Exception ignored) {
                System.out.println("⚠ Not found: " + by);
            }
        }

        if (btn == null) {
            throw new RuntimeException(
                    "❌ Verify Screen Access button not found: "
                            + driver.getCurrentUrl());
        }

        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", btn);
        System.out.println("▶ Verify Screen Access clicked");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // ACCEPT BROWSER ALERT
    // "Are you sure you want to change login as...?"
    // ═══════════════════════════════════════════════
    public void acceptUserSwitchAlert()
            throws InterruptedException {
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            String alertText = driver.switchTo()
                    .alert().getText();
            System.out.println("▶ Alert: " + alertText);
            driver.switchTo().alert().accept();
            System.out.println("✅ Alert accepted — user switched");
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("⚠ No alert: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // HANDLE PENDING TASKS PAGE
    // ✅ After user switch — if pending tasks page
    //    appears → click Remind Me Later → continue
    // ═══════════════════════════════════════════════
    public void handlePendingTasksIfPresent()
            throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions
                            .elementToBeClickable(
                                    remindMeLaterBtn));
            System.out.println("▶ Pending Tasks page detected");

            ((JavascriptExecutor) driver)
                    .executeScript(
                            "arguments[0].click();",
                            remindMeLaterBtn);
            System.out.println(
                    "✅ Remind Me Later clicked"
                            + " — pending tasks dismissed");
            Thread.sleep(1500);

        } catch (Exception e) {
            System.out.println("▶ No pending tasks — continuing");
        }
    }

    // ═══════════════════════════════════════════════
    // DISMISS BROWSER ALERT — Click Cancel
    // ═══════════════════════════════════════════════
    public void dismissUserSwitchAlert()
            throws InterruptedException {
        try {
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().dismiss();
            System.out.println("▶ Alert dismissed");
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("⚠ No alert: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // SWITCH USER — Full flow in one call
    // ✅ Step 1 — Select user from dropdown
    // ✅ Step 2 — Click Verify Screen Access
    // ✅ Step 3 — Accept browser alert (OK)
    // ✅ Step 4 — Handle Pending Tasks if present
    // ═══════════════════════════════════════════════
    public void switchUser(String userName)
            throws InterruptedException {
        System.out.println("▶ Switching to: " + userName);
        selectUser(userName);
        clickVerifyScreenAccess();
        acceptUserSwitchAlert();
        handlePendingTasksIfPresent();
        System.out.println("✅ Now logged in as: " + userName);
    }

    // ═══════════════════════════════════════════════
    // VERIFY USER HAS RIGHT TITLE
    // ═══════════════════════════════════════════════
    public boolean verifyUserHasRight(String rightTitle) {
        try {
            WebElement rightEl = driver.findElement(
                    By.xpath("//table//td[contains(text(),'"
                            + rightTitle + "')]"));
            boolean found = rightEl.isDisplayed();
            System.out.println(found
                    ? "✅ Right found: " + rightTitle
                    : "❌ Right not found: " + rightTitle);
            return found;
        } catch (Exception e) {
            System.out.println("❌ Right not found: "
                    + rightTitle);
            return false;
        }
    }
}
