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
    // ✅ id="select2-user_picker-container"
    // ═══════════════════════════════════════════════
    public void selectUser(String userName)
            throws InterruptedException {

        // ✅ Step 1 — Click Select2 container to open
        wait.until(ExpectedConditions
                .elementToBeClickable(select2Container));
        select2Container.click();
        System.out.println("▶ Select2 dropdown opened");
        Thread.sleep(500);

        // ✅ Step 2 — Type in search box
        WebElement searchBox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(
                                ".select2-search__field")));
        searchBox.clear();
        searchBox.sendKeys(userName);
        System.out.println("▶ Searching: " + userName);
        Thread.sleep(1000);

        // ✅ Step 3 — Click matching result
        WebElement result = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//li[contains(@class," +
                                        "'select2-results__option')" +
                                        " and contains(.,'" +
                                        userName + "')]")));
        result.click();
        System.out.println("✅ User selected: " + userName);
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // CLICK VERIFY SCREEN ACCESS
    // ✅ input type="submit" id="submit"
    // ✅ JS click — bypasses Cloudflare handler
    // ═══════════════════════════════════════════════
    public void clickVerifyScreenAccess()
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(verifyScreenAccessBtn));
        ((JavascriptExecutor) driver)
                .executeScript(
                        "arguments[0].click();",
                        verifyScreenAccessBtn);
        System.out.println("▶ Verify Screen Access clicked");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // ACCEPT BROWSER ALERT — Click OK
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
    // ✅ Uses SHORT wait (5s) — not full 30s wait
    // ═══════════════════════════════════════════════
    public void handlePendingTasksIfPresent()
            throws InterruptedException {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions
                            .elementToBeClickable(
                                    remindMeLaterBtn));

            System.out.println(
                    "▶ Pending Tasks page detected");

            // ✅ JS click — button is type="submit"
            ((JavascriptExecutor) driver)
                    .executeScript(
                            "arguments[0].click();",
                            remindMeLaterBtn);

            System.out.println(
                    "✅ Remind Me Later clicked" +
                            " — pending tasks dismissed");
            Thread.sleep(1500);

        } catch (Exception e) {
            // ✅ No pending tasks — continue normally
            System.out.println(
                    "▶ No pending tasks — continuing");
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
        handlePendingTasksIfPresent(); // ✅ Added
        System.out.println("✅ Now logged in as: " + userName);
    }

    // ═══════════════════════════════════════════════
    // VERIFY USER HAS RIGHT TITLE
    // ✅ Checks Current Rights table
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
