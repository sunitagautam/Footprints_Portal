package pages.Support;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OneTimeChargesPage {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // MAIN PAGE
    // ═══════════════════════════════════════════════
    @FindBy(id = "btn_child_details")
    WebElement fetchChildDetailsBtn;

    // ═══════════════════════════════════════════════
    // APPLY ONE TIME CHARGE MODAL — FORM FIELDS
    // ═══════════════════════════════════════════════
    // ✅ Add One Time Charges button — top right
    @FindBy(xpath = "//button[contains(.,'Add One Time Charges')]")
    private WebElement addOneTimeChargesBtn;

    // ✅ Fetch Child Details button
    // ✅ Child ID input
    @FindBy(xpath = "//*[@id='charge_child_id']")
    private WebElement childIdInput;
    // ✅ Child Name — auto-filled after fetch
    @FindBy(id = "child_name")
    private WebElement childNameInput;

    // ✅ Charge Type — standard <select> dropdown
    // id="charge_type" name="charge_type"
    @FindBy(id = "charge_type")
    private WebElement chargeTypeDropdown;

    // ✅ Charge Amount
    @FindBy(id = "charge_amount")
    private WebElement chargeAmountInput;

    // ✅ Comment textarea
    @FindBy(id = "charge_comments")
    private WebElement chargeCommentsInput;

    // ✅ Submit form button — id="apply_charge"
    @FindBy(id = "apply_charge")
    private WebElement submitFormBtn;

    // ✅ Close modal — × blue circle
    @FindBy(css = "button.close-popdown[data-dismiss='modal']")
    private WebElement closeModalBtn;
    // ═══════════════════════════════════════════════
    // CONFIRM? POPUP
    // "This will immediately generate an invoice..."
    // ═══════════════════════════════════════════════

    // ✅ Confirm Submit — blue Submit button
    @FindBy(id = "submit_apply_charge")
    private WebElement confirmSubmitBtn;

    // ✅ Confirm Close — red Close button
    @FindBy(xpath = "//div[contains(@id,'confirm') or " +
            ".//h4[contains(.,'Confirm')]]" +
            "//button[contains(.,'Close')]")
    private WebElement confirmCloseBtn;

    // ✅ Confirm popup title
    @FindBy(xpath = "//h4[contains(.,'Confirm')]" +
            " | //h5[contains(.,'Confirm')]")
    private WebElement confirmPopupTitle;

    // ═══════════════════════════════════════════════
    // SUCCESS MESSAGE inside modal
    // "Charges applied successfully!"
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//*[contains(.,'applied successfully')]" +
            " | //*[contains(@class,'alert-success')]")
    private WebElement successMessage;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public OneTimeChargesPage(WebDriver driver) {
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
                    .elementToBeClickable(addOneTimeChargesBtn));
            System.out.println("✅ OneTime Charges page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Page not loaded: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK ADD ONE TIME CHARGES
    // ═══════════════════════════════════════════════
    public void clickAddOneTimeCharges()
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(addOneTimeChargesBtn));
        addOneTimeChargesBtn.click();
        System.out.println("▶ Add One Time Charges clicked");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // IS FORM MODAL VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isFormModalVisible() {
        try {
            wait.until(ExpectedConditions
                    .visibilityOf(childIdInput));
            return childIdInput.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // ENTER CHILD ID
    // ═══════════════════════════════════════════════
    public void enterChildId(String childId) throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(childIdInput));
        childIdInput.clear();
        childIdInput.sendKeys(childId);
        System.out.println("✅ Child ID entered: " + childId);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // ENTER CHILD ID — JS fallback for readonly/disabled
    // field
    // ✅ New (additive only) — confirmed live that
    //    charge_child_id can be readonly/disabled for
    //    some users (e.g. Nidhi Chaturvedi), where plain
    //    .clear()/.sendKeys() above throws
    //    InvalidElementStateException. Mirrors the same
    //    JS readonly-bypass pattern already used by
    //    enterChargeAmount()/enterChargeComments().
    // ═══════════════════════════════════════════════
    public void enterChildIdRobust(String childId) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(childIdInput));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].removeAttribute('disabled');" +
                        "arguments[0].value = arguments[1];",
                childIdInput, childId);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input'));" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                childIdInput);
        System.out.println("✅ Child ID entered (JS): " + childId);
        Thread.sleep(300);
    }

    // ═══════════════════════════════════════════════
    // CLICK FETCH CHILD DETAILS
    // ✅ Loads Child Name after entering Child ID
    // ═══════════════════════════════════════════════
    public void clickFetchChildDetails() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(fetchChildDetailsBtn));
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].click();", fetchChildDetailsBtn);
        System.out.println("▶ Fetch Child Details clicked");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // GET CHILD NAME — auto-filled after fetch
    // ═══════════════════════════════════════════════
    public String getChildName() {
        try {
            // ✅ Try as text element first
            String name = childNameInput.getText().trim();
            if (name.isEmpty()) {
                name = childNameInput
                        .getAttribute("value");
            }
            if (name == null) name = "";
            System.out.println("▶ Child Name: " + name);
            return name.trim();
        } catch (Exception e) {
            System.out.println("⚠ Child name not found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // SELECT CHARGE TYPE
    // ✅ Standard <select> — uses selectByVisibleText
    // @param chargeType exact visible text e.g. "Book Set"
    // ═══════════════════════════════════════════════
    public void selectChargeType(String chargeType)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .visibilityOf(chargeTypeDropdown));
        Select select = new Select(chargeTypeDropdown);
        select.selectByVisibleText(chargeType);
        System.out.println("✅ Charge Type: " + chargeType);
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // GET CHARGE AMOUNT — reads auto-filled value
    // ═══════════════════════════════════════════════
    public String getChargeAmount() {
        try {
            String val = chargeAmountInput
                    .getAttribute("value");
            if (val == null || val.isEmpty()) {
                val = chargeAmountInput.getText().trim();
            }
            System.out.println("▶ Charge Amount: " + val);
            return val == null ? "" : val.trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    //    - Auto-filled (readonly) → JS removes readonly, sets value
//    - Manual entry            → JS sets value directly
// ═══════════════════════════════════════════════
    public void enterChargeAmount(String amount)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .visibilityOf(chargeAmountInput));

        // ✅ Remove readonly/disabled — handles auto-filled fields
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].removeAttribute('disabled');" +
                        "arguments[0].value = arguments[1];",
                chargeAmountInput, amount);

        // ✅ Fire change event so app registers the new value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input'));" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                chargeAmountInput);

        System.out.println("✅ Charge Amount: " + amount);
        Thread.sleep(200);
    }

    // ═══════════════════════════════════════════════
// ✅ Same fix — comments field also readonly in some cases
// ═══════════════════════════════════════════════
    public void enterChargeComments(String comments)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .visibilityOf(chargeCommentsInput));

        // ✅ Remove readonly/disabled — then set value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].removeAttribute('readonly');" +
                        "arguments[0].removeAttribute('disabled');" +
                        "arguments[0].value = arguments[1];",
                chargeCommentsInput, comments);

        // ✅ Fire change event so app registers the new value
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new Event('input'));" +
                        "arguments[0].dispatchEvent(new Event('change'));",
                chargeCommentsInput);

        System.out.println("✅ Comments: " + comments);
        Thread.sleep(200);
    }

    // ═══════════════════════════════════════════════
    // ENTER LATE STAY DETAILS
    // ═══════════════════════════════════════════════

    // ✅ Fix — use JS to set date value
    public void enterLateStayDetails(String date, String hour, String minute)
            throws InterruptedException {

        // ✅ Date — JS set (datepicker is readonly to direct typing)
        try {
            WebElement dateField = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("date")));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].removeAttribute('readonly');" +
                            "arguments[0].value = arguments[1];",
                    dateField, date);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new Event('change'));" +
                            "arguments[0].dispatchEvent(new Event('input'));",
                    dateField);
            System.out.println("✅ Late Stay Date: " + date);
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("⚠ Date field: " + e.getMessage());
        }

        // ✅ Select Hour
        try {
            WebElement hourSelect = driver.findElement(By.id("hour"));
            new Select(hourSelect).selectByValue(hour);
            System.out.println("✅ Late Stay Hour: " + hour);
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("⚠ Hour field: " + e.getMessage());
        }

        // ✅ Select Minute — use "00" or "30" as value
        try {
            WebElement minSelect = driver.findElement(By.id("minute"));
            new Select(minSelect).selectByValue(minute);
            System.out.println("✅ Late Stay Min: " + minute);
            Thread.sleep(300);
        } catch (Exception e) {
            // ✅ Try index if value fails
            try {
                WebElement minSelect = driver.findElement(By.id("minute"));
                new Select(minSelect).selectByVisibleText(minute);
            } catch (Exception e2) {
                System.out.println("⚠ Minute field: " + e.getMessage());
            }
        }

        // ✅ Click Calculate
        try {
            WebElement calcBtn = driver.findElement(By.id("calculate_charge"));
            calcBtn.click();
            System.out.println("▶ Calculate clicked");
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("⚠ Calculate: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK SUBMIT FORM
    // ✅ Opens Confirm? popup
    // ═══════════════════════════════════════════════
    public void clickSubmitForm() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(submitFormBtn));
        submitFormBtn.click();
        System.out.println("▶ Submit Form clicked");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // IS CONFIRMATION POPUP VISIBLE
    // "This will immediately generate an invoice..."
    // ═══════════════════════════════════════════════
    public boolean isConfirmationPopupVisible() {
        try {
            // ✅ Check by submit button or text
            new WebDriverWait(driver,
                    Duration.ofSeconds(IAutoConstant.SHORT_WAIT))
                    .until(ExpectedConditions
                            .elementToBeClickable(confirmSubmitBtn));
            System.out.println("✅ Confirmation popup visible");
            return true;
        } catch (Exception e) {
            try {
                WebElement popup = driver.findElement(
                        By.xpath("//*[contains(.," +
                                "'generate an invoice')]"));
                return popup.isDisplayed();
            } catch (Exception e2) {
                return false;
            }
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK CONFIRM SUBMIT — Blue Submit button
    // ═══════════════════════════════════════════════
    public void clickConfirmSubmit()
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(confirmSubmitBtn));
        confirmSubmitBtn.click();
        System.out.println("▶ Confirm Submit clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // CLICK CONFIRM CLOSE — Red Close button
    // ═══════════════════════════════════════════════
    public void clickConfirmClose()
            throws InterruptedException {
        try {
            wait.until(ExpectedConditions
                    .elementToBeClickable(confirmCloseBtn));
            confirmCloseBtn.click();
        } catch (Exception e) {
            // ✅ Fallback by text
            WebElement btn = driver.findElement(
                    By.xpath("//button[contains(.,'Close')]" +
                            "[not(contains(@class,'modal-close'))]"));
            btn.click();
        }
        System.out.println("▶ Confirm Close clicked");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // GET SUCCESS MESSAGE
    // "Charges applied successfully!"
    // ═══════════════════════════════════════════════
    public String getSuccessMessage() {
        try {
            WebElement msgEl = new WebDriverWait(driver,
                    Duration.ofSeconds(10))
                    .until(ExpectedConditions
                            .visibilityOfElementLocated(
                                    By.xpath(
                                            "//*[contains(.," +
                                                    "'applied successfully')]" +
                                                    " | //*[contains(@class," +
                                                    "'alert-success')]")));
            String msg = msgEl.getText().trim();
            System.out.println("✅ Success: " + msg);
            return msg;
        } catch (Exception e) {
            System.out.println("⚠ Success msg not found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // CLOSE MODAL — × blue button
    // ═══════════════════════════════════════════════
    public void closeModal() throws InterruptedException {
        try {
            wait.until(ExpectedConditions
                    .elementToBeClickable(closeModalBtn));
            ((JavascriptExecutor) driver)
                    .executeScript(
                            "arguments[0].click();", closeModalBtn);
            System.out.println("✅ Modal closed via × button");
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("▶ Modal already closed or not present");
        }
    }

    // ═══════════════════════════════════════════════
    // VERIFY ALL CHARGE TYPES PRESENT IN DROPDOWN
    // ✅ Used by SC_015_TC_004
    // ═══════════════════════════════════════════════
    public boolean verifyAllChargeTypesPresent(
            String[] expectedTypes) {
        try {
            Select select = new Select(chargeTypeDropdown);
            List<WebElement> options = select.getOptions();
            List<String> optionTexts = new ArrayList<>();
            for (WebElement opt : options) {
                optionTexts.add(opt.getText().trim());
            }
            System.out.println("▶ Options found: "
                    + optionTexts);

            boolean allFound = true;
            for (String expected : expectedTypes) {
                if (!optionTexts.contains(expected)) {
                    System.out.println("❌ Missing: " + expected);
                    allFound = false;
                } else {
                    System.out.println("✅ Found: " + expected);
                }
            }
            return allFound;
        } catch (Exception e) {
            System.out.println("⚠ verifyAllChargeTypes: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // ATTRITION CHILD — WARNING MESSAGE
    // ✅ New (additive only) — used by attrition-invoice
    //    enhancement tests. Does not touch any existing
    //    method above.
    // ✅ Fires as soon as the attrition child is fetched
    //    on the Apply Charge modal.
    // ═══════════════════════════════════════════════
    public String getAttritionWarningMessage() {
        // ✅ Confirmed live: the actual banner is
        //    "<div class="alert alert-info alert-styled-left">
        //     Cannot raise invoice charges for this child as
        //     attrition date is beyond 1 months</div>" —
        //    class is alert-info (not warning/danger), and the
        //    text lives on a descendant/text node, so XPath
        //    must use "." (string-value) not "text()".
        By[] candidates = {
                By.cssSelector(".alert-info.alert-styled-left"),
                By.cssSelector(".alert-warning"),
                By.cssSelector(".alert-danger"),
                By.cssSelector(".alert-info"),
                By.xpath("//*[contains(translate(.," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz')," +
                        "'cannot raise invoice charges')]"),
                By.xpath("//*[contains(translate(.," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz')," +
                        "'attrition')]"),
                By.xpath("//*[contains(@class,'text-danger') " +
                        "and contains(translate(.," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz')," +
                        "'attrition')]")
        };
        for (By by : candidates) {
            try {
                WebElement el = new WebDriverWait(driver,
                        Duration.ofSeconds(IAutoConstant.SHORT_WAIT))
                        .until(ExpectedConditions
                                .visibilityOfElementLocated(by));
                String msg = el.getText().trim();
                if (!msg.isEmpty()) {
                    System.out.println("⚠ Attrition warning message: "
                            + msg);
                    return msg;
                }
            } catch (Exception ignored) {
            }
        }
        System.out.println("▶ No attrition warning message found");
        return "";
    }

    public boolean isAttritionWarningVisible() {
        return !getAttritionWarningMessage().isEmpty();
    }

    // ═══════════════════════════════════════════════
    // DEBUG — DUMP MODAL HTML
    // ✅ New (additive only) — for locating live
    //    selectors on the Apply Charge modal.
    // ═══════════════════════════════════════════════
    public String dumpModalHtml() {
        try {
            WebElement modal = driver.findElement(By.cssSelector(
                    ".modal.show, .modal.in, " +
                            ".modal[style*='display: block'], .modal-content"));
            String html = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].outerHTML;",
                            modal);
            System.out.println("▶ MODAL HTML DUMP:\n" + html);
            return html;
        } catch (Exception e) {
            System.out.println("⚠ Could not dump modal HTML: "
                    + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // DEBUG — DUMP ALL VISIBLE MODALS/ALERTS
    // ✅ New (additive only) — dumpModalHtml() above can
    //    grab the wrong stacked .modal-content (several
    //    exist in the DOM, most hidden). This scans every
    //    element and keeps only ones actually rendered on
    //    screen (offsetParent !== null), which is more
    //    reliable for locating a dynamically-injected
    //    warning/checkbox after Fetch Child Details.
    // ═══════════════════════════════════════════════
    public String dumpVisibleModalsAndAlerts() {
        try {
            String html = (String) ((JavascriptExecutor) driver).executeScript(
                    "var sel = '.modal, .modal-content, .alert, .toast, " +
                            "[class*=\"alert\"], [id*=\"warning\"], " +
                            "[class*=\"exception\"], [id*=\"exception\"]';" +
                            "var els = document.querySelectorAll(sel);" +
                            "var out = [];" +
                            "els.forEach(function(el){" +
                            "  if (el.offsetParent !== null) {" +
                            "    out.push(el.outerHTML);" +
                            "  }" +
                            "});" +
                            "return out.join('\\n----------\\n');");
            System.out.println("▶ VISIBLE MODALS/ALERTS DUMP:\n" + html);
            return html == null ? "" : html;
        } catch (Exception e) {
            System.out.println("⚠ Could not dump visible modals/alerts: "
                    + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // EXCEPTION CASE CHECKBOX — attrition-invoice
    // exception flow
    // ✅ New (additive only)
    // ═══════════════════════════════════════════════
    private WebElement findExceptionCheckbox() {
        // ✅ Confirmed live: the exception block sits in
        //    <div id="div-attrition-exception" style="display:none">
        //    — hidden unless the app decides to reveal it (attrition
        //    age within some eligible window + user has the right).
        By[] candidates = {
                By.cssSelector("#div-attrition-exception input[type='checkbox']"),
                By.id("exception_case"),
                By.id("is_exception"),
                By.id("exception_checkbox"),
                By.xpath("//input[@type='checkbox' and " +
                        "contains(translate(@id," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz'),'exception')]"),
                By.xpath("//label[contains(translate(.," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz'),'exception')]" +
                        "//input[@type='checkbox']"),
                By.xpath("//*[contains(translate(text()," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz'),'exception')]" +
                        "/preceding::input[@type='checkbox'][1]")
        };
        for (By by : candidates) {
            try {
                WebElement el = driver.findElement(by);
                if (el.isDisplayed()) {
                    System.out.println(
                            "✅ Exception checkbox found via: " + by);
                    return el;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public boolean isExceptionCheckboxVisible() {
        return findExceptionCheckbox() != null;
    }

    public void checkExceptionCheckbox() {
        WebElement cb = findExceptionCheckbox();
        if (cb == null) {
            throw new RuntimeException(
                    "❌ Exception checkbox not found on modal");
        }
        try {
            cb.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver)
                    .executeScript("arguments[0].click();", cb);
        }
        System.out.println("✅ Exception checkbox ticked");
    }

    // ═══════════════════════════════════════════════
    // EXCEPTION REASON — mandatory comment/reason field
    // that unlocks after ticking the exception checkbox
    // ✅ New (additive only) — falls back to the existing
    //    Comment field (enterChargeComments) if no
    //    dedicated exception-reason field is found.
    // ═══════════════════════════════════════════════
    public boolean enterExceptionReasonIfPresent(String reason) {
        By[] candidates = {
                By.id("exception_reason"),
                By.id("exception_comment"),
                By.id("attrition_reason"),
                By.xpath("//textarea[contains(translate(@id," +
                        "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                        "'abcdefghijklmnopqrstuvwxyz'),'exception')]")
        };
        for (By by : candidates) {
            try {
                WebElement el = driver.findElement(by);
                if (el.isDisplayed()) {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].removeAttribute('readonly');" +
                                    "arguments[0].removeAttribute('disabled');" +
                                    "arguments[0].value = arguments[1];",
                            el, reason);
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].dispatchEvent(new Event('input'));" +
                                    "arguments[0].dispatchEvent(new Event('change'));",
                            el);
                    System.out.println(
                            "✅ Exception reason entered via: " + by);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        System.out.println(
                "▶ No dedicated exception-reason field found");
        return false;
    }

    // ═══════════════════════════════════════════════
    // DEBUG — DUMP CHILD-ID INPUT + FETCH BUTTON STATE
    // ✅ New (additive only)
    // ═══════════════════════════════════════════════
    public void dumpChildIdAndFetchButtonState() {
        try {
            String info = (String) ((JavascriptExecutor) driver).executeScript(
                    "var ci = document.getElementById('charge_child_id');" +
                            "var fb = document.getElementById('btn_child_details');" +
                            "function d(el){ return el ? el.outerHTML + ' | disabled=' + el.disabled + " +
                            "' | readOnly=' + el.readOnly + ' | offsetParent=' + (el.offsetParent !== null) : 'NOT FOUND'; }" +
                            "return 'childIdInput: ' + d(ci) + '\\n\\nfetchBtn: ' + d(fb);");
            System.out.println("▶ CHILD ID / FETCH BUTTON STATE:\n" + info);
        } catch (Exception e) {
            System.out.println("⚠ dumpChildIdAndFetchButtonState: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK FETCH CHILD DETAILS — JS-only, no pre-wait
    // ✅ New (additive only) — for sessions where the
    //    button is disabled/not "clickable" per Selenium's
    //    definition but a JS click still triggers the
    //    underlying handler.
    // ═══════════════════════════════════════════════
    public void clickFetchChildDetailsForced() throws InterruptedException {
        try {
            WebElement btn = driver.findElement(By.id("btn_child_details"));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].removeAttribute('disabled');" +
                            "arguments[0].click();", btn);
            System.out.println("▶ Fetch Child Details (forced) clicked");
        } catch (Exception e) {
            System.out.println("⚠ clickFetchChildDetailsForced: " + e.getMessage());
        }
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // IS SUBMIT FORM BUTTON ENABLED
    // ✅ New (additive only) — used for the blocked
    //    (no-exception) negative case.
    // ═══════════════════════════════════════════════
    public boolean isSubmitFormEnabled() {
        try {
            return submitFormBtn.isDisplayed()
                    && submitFormBtn.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // DEBUG — PRINT PAGE-SOURCE SNIPPET CONTAINING A
    // KEYWORD (case-insensitive)
    // ✅ New (additive only) — catches text injected
    //    anywhere on the page, not only inside a
    //    specific modal container.
    // ═══════════════════════════════════════════════
    public void printPageSourceSnippetContaining(String keyword) {
        try {
            String src = driver.getPageSource();
            String lowerSrc = src.toLowerCase();
            String lowerKeyword = keyword.toLowerCase();
            int idx = lowerSrc.indexOf(lowerKeyword);
            if (idx == -1) {
                System.out.println("▶ Keyword '" + keyword
                        + "' not found anywhere in page source");
                return;
            }
            int start = Math.max(0, idx - 200);
            int end = Math.min(src.length(), idx + 200);
            System.out.println("▶ Page source snippet around '" + keyword
                    + "':\n" + src.substring(start, end));
        } catch (Exception e) {
            System.out.println("⚠ printPageSourceSnippetContaining: "
                    + e.getMessage());
        }
    }
}
