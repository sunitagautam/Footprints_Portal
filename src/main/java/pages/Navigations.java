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

    @FindBy(xpath = "//a[contains(.,'Recent Customer')]")
    private WebElement recentCustomerRequestsLink;

    @FindBy(xpath = "//a[contains(.,'Invoices')]")
    private WebElement invoicesLink;

    // ✅ EXACT XPath from browser inspector
    @FindBy(xpath = "//*[@id='navbar-second-toggle']" +
            "//ul//li[7]//div//div[1]//div//div[2]" +
            "//ul//li[1]//a[@href='onetime_charges']")
    private WebElement oneTimeChargesLink;

    // ═══════════════════════════════════════════════
    // SETTINGS SUB-MENU ITEMS
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//a[contains(.,'User Rights')]")
    private WebElement userRightsLink;

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
    // CLOSE ANY OPEN MODAL + NOTIFICATION DROPDOWN
    // ═══════════════════════════════════════════════
    private void closeModalIfOpen() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll(" +
                            "'.modal-backdrop,.modal-backdrop.fade')" +
                            ".forEach(el => el.remove());" +
                            "document.querySelectorAll(" +
                            "'.alert.alert-warning')" +
                            ".forEach(el => el.remove());" +
                            "document.querySelectorAll('.modal')" +
                            ".forEach(el => {" +
                            "  el.style.display='none';" +
                            "  el.classList.remove('in','show');" +
                            "});" +
                            "document.body.classList.remove('modal-open');" +
                            "document.querySelectorAll(" +
                            "'[class*=\"popdown-mynotify\"]')" +
                            ".forEach(el => el.style.display='none');"
            );
            Thread.sleep(500);
            System.out.println("✅ Cleaned up modals/alerts via JS");
        } catch (Exception e) {
            System.out.println("▶ No cleanup needed");
        }
    }

    // ═══════════════════════════════════════════════
    // HELPER — Click main menu then sub-menu
    // ✅ Re-initialises PageFactory before each click
    //    to handle StaleElementReferenceException
    //    after user switch causes full page reload
    // ═══════════════════════════════════════════════
    private void clickMenu(WebElement menu, WebElement subItem,
                           String screenName)
            throws InterruptedException {
        closeModalIfOpen();

        // ✅ Re-init — fixes stale elements after
        //    user switch causes full page reload
        PageFactory.initElements(driver, this);

        wait.until(ExpectedConditions.elementToBeClickable(menu));
        ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", menu);
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
    // CHILDREN → RECENT CUSTOMER REQUESTS
    // Sub-tab is under Children menu, not Support
    // ═══════════════════════════════════════════════
    public void goToRecentCustomerRequests()
            throws InterruptedException {
        clickMenu(childrenMenu, recentCustomerRequestsLink,
                "Recent Customer Requests");
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → INVOICES
    // ═══════════════════════════════════════════════
    public void goToInvoices()
            throws InterruptedException {
        clickMenu(supportMenu, invoicesLink, "Invoices");
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → ONETIME CHARGES
    // ✅ Exact XPath from browser inspector
    // ═══════════════════════════════════════════════
    public void goToOneTimeCharges()
            throws InterruptedException {
        clickMenu(supportMenu, oneTimeChargesLink,
                "OneTime Charges");
    }

    // ═══════════════════════════════════════════════
    // SETTINGS → USER RIGHTS
    // ═══════════════════════════════════════════════
    public void goToUserRights()
            throws InterruptedException {
        clickMenu(settingsMenu, userRightsLink,
                "User Rights");
    }
}
