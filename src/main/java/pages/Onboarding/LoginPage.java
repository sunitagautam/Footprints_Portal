package pages.Onboarding;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;

public class LoginPage {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // PAGE ELEMENTS
    // ═══════════════════════════════════════════════
    @FindBy(name = "username")
    private WebElement username;

    @FindBy(name = "password")
    private WebElement pwd;

    @FindBy(id = "submit")
    private WebElement loginBtn;

    @FindBy(xpath = "//img[@src='assets/images/logo.png']")
    private WebElement footPrintsLogo;

    // ✅ Fixed — correct locator from your application
    @FindBy(css = "span.label.bg-danger")
    private WebElement errorMsg;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // LOGIN — with parameters
    // ═══════════════════════════════════════════════
    public void login(String un, String pw) {
        wait.until(ExpectedConditions.visibilityOf(username));
        username.clear();
        if (!un.trim().isEmpty()) username.sendKeys(un);
        System.out.println("✅ Username entered: [" + un + "]");
        pwd.clear();
        if (!pw.trim().isEmpty()) pwd.sendKeys(pw);
        System.out.println("✅ Password entered");
        loginBtn.click();
        System.out.println("▶ Login button clicked");
    }

    // ═══════════════════════════════════════════════
    // LOGIN — default credentials
    // ═══════════════════════════════════════════════
    public void loginWithDefaultCredentials() {
        login(IAutoConstant.USERNAME, IAutoConstant.PASSWORD);
    }

    // ═══════════════════════════════════════════════
    // IS LOGIN SUCCESSFUL
    // ═══════════════════════════════════════════════
    public boolean isLoginSuccessful() {
        try {
            wait.until(driver ->
                    !driver.getCurrentUrl().contains("login"));
            System.out.println("✅ Redirected to: " +
                    driver.getCurrentUrl());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // GET ERROR MESSAGE
    // ✅ Fixed — uses span.label.bg-danger locator
    // ═══════════════════════════════════════════════
    public String getErrorMessage() {
        try {
            wait.until(ExpectedConditions.visibilityOf(errorMsg));
            String msg = errorMsg.getText().trim();
            System.out.println("▶ Error message: " + msg);
            return msg;
        } catch (Exception e) {
            System.out.println("⚠ No error message found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // IS ERROR VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isErrorVisible() {
        try {
            return errorMsg.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // IS ON LOGIN PAGE
    // ═══════════════════════════════════════════════
    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("login");
    }

    // ═══════════════════════════════════════════════
    // VALIDATE LOGO
    // ═══════════════════════════════════════════════
    public boolean validateFootPrintsLogo() {
        try {
            return footPrintsLogo.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}