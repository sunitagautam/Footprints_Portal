package utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.*;
import pages.Onboarding.LoginPage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseTest {

    protected WebDriver driver;

    protected static AtomicInteger passCount = new AtomicInteger(0);
    protected static AtomicInteger failCount = new AtomicInteger(0);

    // ═══════════════════════════════════════════════
    // BEFORE SUITE — init folders
    // ═══════════════════════════════════════════════
    @BeforeSuite
    public void initFramework() {
        IAutoConstant.initFolders();
        System.out.println("▶ Framework initialised.");
    }

    // ═══════════════════════════════════════════════
    // BEFORE CLASS — launch browser
    // ═══════════════════════════════════════════════
    @BeforeClass
    @Parameters("browser")
    public void openBrowser(@Optional("chrome") String browser) {
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

        // ✅ Auto Login after browser launch
        driver.get(IAutoConstant.LOGIN_URL);
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginWithDefaultCredentials();
        System.out.println("✅ Logged in as: " + IAutoConstant.USERNAME);
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
            System.out.println("❌ Screenshot failed: " + e.getMessage());
        }
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
        Reporter.log("══════════════ TEST SUMMARY ══════════════", true);
        Reporter.log("✅ Passed : " + passCount.get(), true);
        Reporter.log("❌ Failed : " + failCount.get(), true);
        Reporter.log("   Total  : " +
                (passCount.get() + failCount.get()), true);
        Reporter.log("==========================================", true);
    }
}