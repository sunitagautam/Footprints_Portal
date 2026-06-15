package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.*;
import pages.Onboarding.LoginPage;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseTest {

    protected static AtomicInteger passCount = new AtomicInteger(0);
    protected static AtomicInteger failCount = new AtomicInteger(0);
    protected WebDriver driver;

    // ═══════════════════════════════════════════════
    // BEFORE SUITE — init folders
    // ═══════════════════════════════════════════════
    @BeforeSuite
    public void initFramework() {
        IAutoConstant.initFolders();
        System.out.println("▶ Framework initialised.");
    }

    // ═══════════════════════════════════════════════
    // BEFORE CLASS — launch browser + login
    // ═══════════════════════════════════════════════
    @BeforeClass
    @Parameters("browser")
    public void openBrowser(@Optional("chrome") String browser)
            throws InterruptedException {
        System.out.println("▶ Launching browser: " + browser);

        if (browser.equalsIgnoreCase("chrome")) {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-notifications");
            driver = new ChromeDriver(options);

        } else if (browser.equalsIgnoreCase("firefox")) {
            WebDriverManager.firefoxdriver().setup();
            driver = new FirefoxDriver();

        } else {
            throw new IllegalArgumentException(
                    "❌ Browser not supported: " + browser);
        }

        driver.manage().window().maximize();

        // ══════════════════════════════════════════
        // STEP 1 — Login as Rakesh
        // ══════════════════════════════════════════
        driver.get(IAutoConstant.LOGIN_URL);
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginWithDefaultCredentials();
        System.out.println("✅ Logged in as: "
                + IAutoConstant.USERNAME);
        Thread.sleep(1500);

        // ══════════════════════════════════════════
        // STEP 2 — Acknowledge all policy notifications
        // ✅ Uses Next button to go through all bells
        // ══════════════════════════════════════════
        acknowledgePolicyNotificationIfPresent();

        // ══════════════════════════════════════════
        // STEP 3 — Close notification dropdown
        // ✅ Prevents menu click interception
        // ══════════════════════════════════════════
        closeNotificationDropdownIfOpen();
    }

    // ═══════════════════════════════════════════════
    // ACKNOWLEDGE POLICY NOTIFICATION POPUP
    //
    // ✅ Flow:
    //    Bell 1 → Acknowledge → OK alert
    //           → Next button → Bell 2 loads
    //    Bell 2 → Acknowledge → OK alert
    //           → Next button → Bell 3 loads
    //    ...
    //    Last bell → Acknowledge → OK alert
    //              → No Next button → done ✅
    // ═══════════════════════════════════════════════
    protected void acknowledgePolicyNotificationIfPresent()
            throws InterruptedException {
        try {
            // ✅ Check if popup present (wait 5s)
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions
                            .presenceOfElementLocated(
                                    By.cssSelector(
                                            ".step-indicator")));

            System.out.println(
                    "▶ Policy notification popup detected");

            int acknowledged = 0;

            for (int bell = 1; bell <= 10; bell++) {

                // ✅ Safety — dismiss any lingering alert
                dismissAlertIfPresent();

                // ✅ Wait for Acknowledge button
                WebElement ackBtn;
                try {
                    ackBtn = new WebDriverWait(driver,
                            Duration.ofSeconds(5))
                            .until(ExpectedConditions
                                    .elementToBeClickable(
                                            By.cssSelector(
                                                    "button.notifyStatus" +
                                                            "[data-notification-status" +
                                                            "='Acknowledgement']")));
                } catch (Exception e) {
                    System.out.println(
                            "✅ No more notifications to acknowledge");
                    break;
                }

                // ✅ Click Acknowledge via JS
                ((JavascriptExecutor) driver)
                        .executeScript(
                                "arguments[0].click();", ackBtn);
                System.out.println(
                        "▶ Acknowledge clicked — bell " + bell);
                Thread.sleep(500);

                // ✅ Accept browser alert
                // "Are You Sure To Acknowledgement This Notification?"
                try {
                    new WebDriverWait(driver,
                            Duration.ofSeconds(5))
                            .until(ExpectedConditions
                                    .alertIsPresent());
                    driver.switchTo().alert().accept();
                    acknowledged++;
                    System.out.println(
                            "✅ Bell " + bell
                                    + " acknowledged permanently");
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println(
                            "⚠ Alert not found: "
                                    + e.getMessage());
                    break;
                }

                // ✅ Click Next button to go to next bell
                // data-notification-status="Next"
                try {
                    WebElement nextBtn = new WebDriverWait(
                            driver, Duration.ofSeconds(3))
                            .until(ExpectedConditions
                                    .elementToBeClickable(
                                            By.cssSelector(
                                                    "button.notifyStatus" +
                                                            "[data-notification-status" +
                                                            "='Next']")));

                    ((JavascriptExecutor) driver)
                            .executeScript(
                                    "arguments[0].click();",
                                    nextBtn);
                    System.out.println(
                            "▶ Next → loading bell "
                                    + (bell + 1));
                    Thread.sleep(1000);

                } catch (Exception e) {
                    // ✅ No Next button = last bell = all done
                    System.out.println(
                            "✅ Last bell done — no Next button");
                    break;
                }
            }

            System.out.println(
                    "✅ Policy acknowledged: " + acknowledged
                            + " notification(s) permanently");

        } catch (Exception e) {
            // ✅ No popup — continue normally
            System.out.println(
                    "▶ No policy notification — continuing");
        }
    }

    // ═══════════════════════════════════════════════
    // CLOSE NOTIFICATION DROPDOWN
    // ✅ Removes popdown panel blocking menu clicks
    // ✅ class="popdown-mynotify-outter p-15 pt-5"
    // ═══════════════════════════════════════════════
    protected void closeNotificationDropdownIfOpen()
            throws InterruptedException {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll(" +
                            "'[class*=\"popdown-mynotify\"]')" +
                            ".forEach(function(el){" +
                            "  el.style.display='none';" +
                            "});" +
                            "document.body.click();"
            );
            Thread.sleep(500);
            System.out.println(
                    "✅ Notification dropdown closed");
        } catch (Exception e) {
            System.out.println(
                    "▶ No notification dropdown");
        }
    }

    // ═══════════════════════════════════════════════
    // DISMISS ALERT IF PRESENT — safety helper
    // ═══════════════════════════════════════════════
    protected void dismissAlertIfPresent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().accept();
            System.out.println("▶ Lingering alert dismissed");
        } catch (Exception ignored) {
        }
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD — screenshot on failure
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void handleTestResult(ITestResult result) {
        String testName = result.getName();

        if (result.getStatus() == ITestResult.SUCCESS) {
            passCount.incrementAndGet();
            Reporter.log("✅ PASSED: " + testName, true);

        } else if (result.getStatus() == ITestResult.FAILURE) {
            failCount.incrementAndGet();
            Reporter.log("❌ FAILED: " + testName, true);
            Reporter.log("   Cause: " +
                    result.getThrowable().getMessage(), true);
            takeScreenshot(testName);

        } else if (result.getStatus() == ITestResult.SKIP) {
            Reporter.log("⚠ SKIPPED: " + testName, true);
        }
    }

    // ═══════════════════════════════════════════════
    // SCREENSHOT
    // ═══════════════════════════════════════════════
    public void takeScreenshot(String testName) {
        if (driver == null) return;
        try {
            File src = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.FILE);
            String path = IAutoConstant.SCREENSHOT_PATH +
                    testName + "_" +
                    System.currentTimeMillis() + ".png";
            FileUtils.copyFile(src, new File(path));
            Reporter.log("📸 Screenshot: " + path, true);
        } catch (IOException e) {
            System.out.println("❌ Screenshot failed: "
                    + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // SHARED HELPER — look up user for a screen
    //                 from input_UserRights.xlsx
    // ═══════════════════════════════════════════════
    protected String getUserForScreen(String screenName) throws Exception {
        FileInputStream fis = new FileInputStream(
                IAutoConstant.USER_RIGHTS_EXCEL);
        Workbook wb = new XSSFWorkbook(fis);
        Sheet sheet = wb.getSheet("UserRights");
        String user = "";
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            Cell screenCell = row.getCell(1);
            if (screenCell == null) continue;
            if (screenCell.getStringCellValue().trim()
                    .equalsIgnoreCase(screenName)) {
                Cell userCell = row.getCell(0);
                if (userCell != null) {
                    user = userCell.getStringCellValue().trim();
                    break;
                }
            }
        }
        wb.close();
        fis.close();
        System.out.println("▶ User for [" + screenName + "]: " + user);
        return user;
    }

    // ═══════════════════════════════════════════════
    // AFTER CLASS — close browser
    // ═══════════════════════════════════════════════
    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
            System.out.println("▶ Browser closed.");
        }
    }

    // ═══════════════════════════════════════════════
    // AFTER SUITE — print summary
    // ═══════════════════════════════════════════════
    @AfterSuite(alwaysRun = true)
    public void printReport() {
        Reporter.log("", true);
        Reporter.log(
                "══════════════ TEST SUMMARY ══════════════",
                true);
        Reporter.log("✅ Passed : " + passCount.get(), true);
        Reporter.log("❌ Failed : " + failCount.get(), true);
        Reporter.log("   Total  : " +
                (passCount.get() + failCount.get()), true);
        Reporter.log(
                "==========================================",
                true);
    }
}
