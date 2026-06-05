package pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;

public class Navigations {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // TOP NAVIGATION MENUS
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//a[contains(@class,'dropdown-toggle')" +
            " and contains(.,'Support')]")
    private WebElement supportMenu;

    @FindBy(xpath = "//a[contains(@class,'dropdown-toggle')" +
            " and contains(.,'Settings')]")
    private WebElement settingsMenu;

    @FindBy(xpath = "//a[contains(@class,'dropdown-toggle')" +
            " and contains(.,'Children')]")
    private WebElement childrenMenu;

    @FindBy(xpath = "//a[contains(@class,'dropdown-toggle')" +
            " and contains(.,'Employees')]")
    private WebElement employeesMenu;

    @FindBy(xpath = "//a[contains(@class,'dropdown-toggle')" +
            " and contains(.,'Franchise')]")
    private WebElement franchiseMenu;

    @FindBy(xpath = "//a[contains(@class,'dropdown-toggle')" +
            " and contains(.,'Sales')]")
    private WebElement salesMarketingMenu;

    // ═══════════════════════════════════════════════
    // SUPPORT SUB-MENU ITEMS
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//a[contains(.,'Online Payments Received')]")
    private WebElement onlinePaymentsReceivedLink;

    @FindBy(xpath = "//a[contains(.,'Account Statement')]")
    private WebElement accountStatementLink;

    @FindBy(xpath = "//a[contains(.,'Invoices')]")
    private WebElement invoicesLink;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public Navigations(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // CLOSE ANY OPEN MODAL
    // ✅ Called before every navigation
    // ✅ Prevents ElementClickInterceptedException
    // ═══════════════════════════════════════════════
    private void closeModalIfOpen() {
        try {
            // ✅ Also remove modal-backdrop via JS
            ((JavascriptExecutor) driver).executeScript(
                    "var backdrops = document.querySelectorAll(" +
                            "'.modal-backdrop, .modal-backdrop.fade');" +
                            "backdrops.forEach(function(el){ el.remove(); });" +
                            // ✅ Also close any alert warnings
                            "var alerts = document.querySelectorAll(" +
                            "'.alert.alert-warning');" +
                            "alerts.forEach(function(el){ el.remove(); });" +
                            // ✅ Also hide any open modals
                            "var modals = document.querySelectorAll('.modal');" +
                            "modals.forEach(function(el){" +
                            "  el.style.display='none';" +
                            "  el.classList.remove('in','show');" +
                            "});" +
                            // ✅ Remove modal-open class from body
                            "document.body.classList.remove('modal-open');"
            );
            Thread.sleep(500);
            System.out.println("✅ Cleaned up modals/alerts via JS");
        } catch (Exception e) {
            System.out.println("▶ No cleanup needed");
        }
    }

    // ═══════════════════════════════════════════════
    // HELPER — Click main menu then sub-menu
    // ✅ Closes modal + navigates
    // ═══════════════════════════════════════════════
    private void clickMenu(WebElement menu, WebElement subItem,
                           String screenName)
            throws InterruptedException {
        closeModalIfOpen();                          // ✅ Always close first
        wait.until(ExpectedConditions.elementToBeClickable(menu));
        menu.click();
        System.out.println("▶ Menu clicked");
        Thread.sleep(500);
        wait.until(ExpectedConditions.elementToBeClickable(subItem));
        subItem.click();
        System.out.println("✅ Navigated to: " + screenName);
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → ONLINE PAYMENTS RECEIVED
    // ═══════════════════════════════════════════════
    public void goToOnlinePaymentReceived()
            throws InterruptedException {
        clickMenu(supportMenu, onlinePaymentsReceivedLink,
                "Online Payments Received");
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → ACCOUNT STATEMENT
    // ═══════════════════════════════════════════════
    public void goToAccountStatement()
            throws InterruptedException {
        clickMenu(supportMenu, accountStatementLink,
                "Account Statement");
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → INVOICES
    // ═══════════════════════════════════════════════
    public void goToInvoices()
            throws InterruptedException {
        clickMenu(supportMenu, invoicesLink, "Invoices");
    }
}