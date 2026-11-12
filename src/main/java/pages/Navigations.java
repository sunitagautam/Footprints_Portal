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

    // ✅ There are TWO distinct href='onetime_charges' anchors in
    //    the DOM: "Apply OneTime Charges" (a Billing-section
    //    shortcut) and the actual top-level nav item, plain
    //    "OneTime Charges" — confirmed live. Matching by href alone
    //    (with or without the #navbar-second-toggle scope) grabbed
    //    the wrong/non-clickable one. Matching on the exact visible
    //    text disambiguates regardless of menu position.
    @FindBy(xpath = "//a[@href='onetime_charges' and normalize-space(text())='OneTime Charges']")
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

    // ═══════════════════════════════════════════════
    // SETTINGS → SMS VIEW
    // Navigates directly via URL — no sub-menu link
    // needed since the screen URL is known.
    // ═══════════════════════════════════════════════
    public void goToSmsView() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.SMS_VIEW_URL);
        System.out.println("✅ Navigated to: SMS View");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // SETTINGS → EMAIL VIEW
    // Navigates directly via URL — only the Rakesh user
    // has access to this screen (confirmed live, 2026-09-22).
    // ═══════════════════════════════════════════════
    public void goToEmailView() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.EMAIL_VIEW_URL);
        System.out.println("✅ Navigated to: Email View");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // SETTINGS → SYSTEM LOGS → DAILY OBSERVER DATA
    // ═══════════════════════════════════════════════
    public void goToDailyObserverData() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.DAILY_OBSERVER_DATA_URL);
        System.out.println("✅ Navigated to: Daily Observer Data");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → TIE UPS DETAILS
    // ═══════════════════════════════════════════════
    public void goToTieupDetails() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.CORPORATE_OFFERS_URL);
        System.out.println("✅ Navigated to: Tie Ups Details");
        Thread.sleep(1500);
    }

    public void goToAddCorporateOffer() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ADD_CORPORATE_OFFER_URL);
        System.out.println("✅ Navigated to: Add Corporate Offer");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → ADMISSION INVITES
    // ═══════════════════════════════════════════════
    public void goToAdmissionInvites() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ADMISSION_INVITES_URL);
        System.out.println("✅ Navigated to: Admission Invites");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → ADMISSION RECORDS
    // ═══════════════════════════════════════════════
    public void goToAdmissionRecords() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ADMISSION_RECORDS_URL);
        System.out.println("✅ Navigated to: Admission Records");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → CORPORATE ADMISSION COUNT
    // ═══════════════════════════════════════════════
    // ═══════════════════════════════════════════════
    // CHILDREN → UPDATES → PAYMENT / ATTENDANCE RETENTION
    // queryString: e.g. "filter=closed&from_date=2026-10-01" (null/empty = default view)
    // ═══════════════════════════════════════════════
    public void goToPaymentRetention(String queryString) throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.PAYMENT_RETENTION_URL
                + (queryString == null || queryString.isEmpty() ? "" : "?" + queryString));
        System.out.println("✅ Navigated to: Payment Retention");
        Thread.sleep(2000);
    }

    public void goToAttendanceRetention(String queryString) throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ATTENDANCE_RETENTION_URL
                + (queryString == null || queryString.isEmpty() ? "" : "?" + queryString));
        System.out.println("✅ Navigated to: Attendance Retention");
        Thread.sleep(2000);
    }

    public void goToPickupAuthorization(String queryString) throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.PICKUP_AUTHORIZATION_URL
                + (queryString == null || queryString.isEmpty() ? "" : "?" + queryString));
        System.out.println("✅ Navigated to: Pickup Authorization");
        Thread.sleep(2000);
    }

    public void goToAttendanceUpdates() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ATTENDANCE_UPDATES_URL);
        System.out.println("✅ Navigated to: Attendance Updates");
        Thread.sleep(2000);
    }

    public void goToCorporateAdmissionCount() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ADMISSION_COUNT_URL);
        System.out.println("✅ Navigated to: Corporate Admission Count");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → CORPORATE INVOICE
    // ═══════════════════════════════════════════════
    public void goToCorporateInvoice() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.CORPORATE_INVOICE_URL);
        System.out.println("✅ Navigated to: Corporate Invoice");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → PARENT COMPANY REPORT
    // ═══════════════════════════════════════════════
    public void goToParentCompanyReport() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.PARENT_COMPANY_REPORT_URL);
        System.out.println("✅ Navigated to: Parent Company Report");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → CORPORATE COMPANY
    // ═══════════════════════════════════════════════
    public void goToCorporateCompany() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.CORPORATE_COMPANY_URL);
        System.out.println("✅ Navigated to: Corporate Company");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // TEACHER PAYOUT REPORT — direct URL (not linked in any menu yet)
    // queryString: e.g. "filter_center_id=34&date_filter=Current+Quarter&..."
    //              (empty → the blank "Select a COCO centre" view)
    // ═══════════════════════════════════════════════
    public void goToTeacherPayoutReport(String queryString) throws InterruptedException {
        closeModalIfOpen();
        String url = utils.IAutoConstant.TEACHER_PAYOUT_REPORT_URL
                + (queryString == null || queryString.isEmpty() ? "" : "?" + queryString);
        driver.get(url);
        System.out.println("✅ Navigated to: Teacher Payout Report " + (queryString == null ? "" : queryString));
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → TIEUP PLANS AND PRICING
    // ═══════════════════════════════════════════════
    public void goToTieupPlansPricing() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.TIEUP_PLANS_PRICING_URL);
        System.out.println("✅ Navigated to: Tieup Plans and Pricing");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → PENDING CO-PAY SETUP
    // ═══════════════════════════════════════════════
    public void goToPendingCoPaySetup() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.PENDING_COPAY_SETUP_URL);
        System.out.println("✅ Navigated to: Pending Co-Pay Setup");
        Thread.sleep(2500);
    }

    public void goToCoPayMigrationData() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.COPAY_MIGRATION_DATA_URL);
        System.out.println("✅ Navigated to: Co-Pay Migration Data");
        Thread.sleep(2500);
    }

    public void goToTieupExpiryMigration() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.TIEUP_EXPIRY_MIGRATION_URL);
        System.out.println("✅ Navigated to: Tie-up Expiry Migration Admissions");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → CORPORATE CREDITS ISSUED
    // ═══════════════════════════════════════════════
    public void goToCorporateCredits() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.CORPORATE_CREDITS_URL);
        System.out.println("✅ Navigated to: Corporate Credits Issued");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // FRANCHISE → CORPORATE TIE-UPS → ADJUST CORPORATE CREDITS
    // ═══════════════════════════════════════════════
    public void goToAdjustCorporateCredits() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.ADJUST_CORPORATE_CREDITS_URL);
        System.out.println("✅ Navigated to: Adjust Corporate Credits");
        Thread.sleep(2500);
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → REFUND LIST
    // Navigates directly via URL — no sub-menu link
    // needed since the screen URL is known. Used by
    // Cancel Registration's SC_011_TC_004 to verify a
    // voided invoice appears for Finance to refund.
    // ═══════════════════════════════════════════════
    public void goToRefundList() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.REFUND_LIST_URL);
        System.out.println("✅ Navigated to: Refund List");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // SUPPORT → FINANCIAL UPDATES → CREDITS ISSUED
    // Navigates directly via URL, same as Email View /
    // Refund List.
    // ═══════════════════════════════════════════════
    public void goToCreditsIssued() throws InterruptedException {
        closeModalIfOpen();
        driver.get(utils.IAutoConstant.CREDITS_ISSUED_URL);
        System.out.println("✅ Navigated to: Credits Issued");
        Thread.sleep(1500);
    }
}
