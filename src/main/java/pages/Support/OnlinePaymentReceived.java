package pages.Support;

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
import java.util.List;

public class OnlinePaymentReceived {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════
    @FindBy(id = "center_id")
    private WebElement centerDropdown;

    @FindBy(id = "select2-center_id-container")
    private WebElement centerSelect2Container;

    @FindBy(name = "filter_gateway[]")
    private WebElement gatewayMultiselect;

    @FindBy(name = "filter_type[]")
    private WebElement typeMultiselect;

    @FindBy(name = "filter_status[]")
    private WebElement statusMultiselect;

    @FindBy(id = "activity_date")
    private WebElement dateRangeInput;

    @FindBy(name = "daterangepicker_start")
    private WebElement dateRangeStart;

    @FindBy(name = "daterangepicker_end")
    private WebElement dateRangeEnd;

    @FindBy(css = "button.applyBtn")
    private WebElement dateApplyBtn;

    @FindBy(css = "button.cancelBtn")
    private WebElement dateCancelBtn;

    @FindBy(id = "filter_submit_btn")
    private WebElement searchBtn;

    // ═══════════════════════════════════════════════
    // TABLE SEARCH
    // ═══════════════════════════════════════════════
    @FindBy(css = "#DataTables_Table_0_filter input")
    private WebElement tableSearchBox;

    // ═══════════════════════════════════════════════
    // TABLE
    // ═══════════════════════════════════════════════
    @FindBy(id = "DataTables_Table_0")
    private WebElement dataTable;

    @FindBy(css = "#DataTables_Table_0 tbody tr")
    private List<WebElement> tableRows;

    @FindBy(css = ".dataTables_info")
    private WebElement tableInfo;

    @FindBy(css = "#DataTables_Table_0 tfoot td")
    private List<WebElement> tableFooterCells;

    // ═══════════════════════════════════════════════
    // PAGINATION
    // ═══════════════════════════════════════════════
    @FindBy(css = "button.btn-primary.notifyStatus")
    private WebElement nextPageBtn;

    @FindBy(css = "button.btn-info.notifyStatus")
    private WebElement prevPageBtn;

    // ═══════════════════════════════════════════════
    // MAIN BUTTONS
    // ═══════════════════════════════════════════════
    @FindBy(css = "button.payu-model")
    private WebElement updatePaymentBtn;

    @FindBy(id = "btn-upload-payment-not-received")
    private WebElement uploadPaymentsNotReceivedBtn;

    // ═══════════════════════════════════════════════
    // UPDATE PAYMENT MODAL
    // ═══════════════════════════════════════════════
    @FindBy(id = "payumoney")
    private WebElement radioPayUMoney;

    @FindBy(id = "hdfc")
    private WebElement radioHDFC;

    @FindBy(id = "icici")
    private WebElement radioICICI;

    @FindBy(id = "transaction_id")
    private WebElement transactionNumberInput;

    @FindBy(css = "button.btn-retrieve")
    private WebElement retrieveDetailsBtn;

    @FindBy(css = "button.btn-submit")
    private WebElement submitDetailsBtn;

    @FindBy(css = "button.btn-link")
    private WebElement closeUpdatePaymentBtn;

    // ✅ Transaction details textarea — id="payu_details"
    @FindBy(id = "payu_details")
    private WebElement payuDetailsTextarea;

    // ═══════════════════════════════════════════════
    // BULK UPLOAD MODAL
    // ═══════════════════════════════════════════════
    @FindBy(id = "pnr-file")
    private WebElement uploadCSVInput;

    @FindBy(id = "pnr-uploader-email")
    private WebElement uploaderEmailInput;

    @FindBy(id = "btn-pnr-upload")
    private WebElement uploadTransactionsBtn;

    @FindBy(id = "btn-pnr-sample")
    private WebElement downloadSampleCSVBtn;
    // Add to @FindBy locators section
    @FindBy(css = ".alert.alert-warning")
    private WebElement transactionWarningAlert;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public OnlinePaymentReceived(WebDriver driver) {
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
            wait.until(ExpectedConditions.visibilityOf(dataTable));
            System.out.println("✅ Online Payment Received page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Page not loaded: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // IS TABLE DISPLAYING DATA
    // ═══════════════════════════════════════════════
    public boolean isTableDisplayingData() {
        try {
            wait.until(ExpectedConditions.visibilityOf(dataTable));
            int rowCount = tableRows.size();
            System.out.println("▶ Table rows found: " + rowCount);
            return rowCount > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // GET TABLE ROW COUNT
    // ═══════════════════════════════════════════════
    public int getTableRowCount() {
        try {
            return tableRows.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // ═══════════════════════════════════════════════
    // GET TABLE INFO TEXT
    // ═══════════════════════════════════════════════
    public String getTableInfoText() {
        try {
            String info = tableInfo.getText().trim();
            System.out.println("▶ Table info: " + info);
            return info;
        } catch (Exception e) {
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // GET TOTAL AMOUNT
    // ═══════════════════════════════════════════════
    public String getTotalAmount() {
        try {
            for (WebElement cell : tableFooterCells) {
                String text = cell.getText().trim();
                if (text.matches(".*\\d+\\.\\d{2}.*")) {
                    System.out.println("▶ Total Amount: " + text);
                    return text;
                }
            }
            return "";
        } catch (Exception e) {
            System.out.println("⚠ Total amount not found");
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // FILTER BY CENTER — Select2
    // ═══════════════════════════════════════════════
    public void filterByCenter(String centerName)
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(centerSelect2Container));
        centerSelect2Container.click();
        Thread.sleep(500);

        WebElement searchInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".select2-search__field")));
        searchInput.sendKeys(centerName);
        Thread.sleep(500);

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//li[contains(@class,'select2-results__option')" +
                        " and contains(text(),'" + centerName + "')]"))
        ).click();
        System.out.println("✅ Center selected: " + centerName);
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // FILTER BY GATEWAY — Multiselect
    // ═══════════════════════════════════════════════
    public void filterByGateway(String gatewayValue)
            throws InterruptedException {
        selectMultiselectOption("filter_gateway[]", gatewayValue);
        System.out.println("✅ Gateway selected: " + gatewayValue);
    }

    // ═══════════════════════════════════════════════
    // FILTER BY TYPE — Multiselect
    // ═══════════════════════════════════════════════
    public void filterByType(String typeValue)
            throws InterruptedException {
        selectMultiselectOption("filter_type[]", typeValue);
        System.out.println("✅ Type selected: " + typeValue);
    }

    // ═══════════════════════════════════════════════
    // FILTER BY STATUS — Multiselect
    // ═══════════════════════════════════════════════
    public void filterByStatus(String statusValue)
            throws InterruptedException {
        selectMultiselectOption("filter_status[]", statusValue);
        System.out.println("✅ Status selected: " + statusValue);
    }

    // ═══════════════════════════════════════════════
    // FILTER BY DATE RANGE
    // @param startDate format: MM/DD/YYYY
    // @param endDate   format: MM/DD/YYYY
    // ═══════════════════════════════════════════════
    public void filterByDateRange(String startDate, String endDate)
            throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(dateRangeInput));
        dateRangeInput.click();
        Thread.sleep(500);

        wait.until(ExpectedConditions.visibilityOf(dateRangeStart));
        dateRangeStart.clear();
        dateRangeStart.sendKeys(startDate);

        dateRangeEnd.clear();
        dateRangeEnd.sendKeys(endDate);

        wait.until(ExpectedConditions.elementToBeClickable(dateApplyBtn));
        dateApplyBtn.click();
        System.out.println("✅ Date range: " + startDate + " to " + endDate);
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // CLICK SEARCH BUTTON
    // ═══════════════════════════════════════════════
    public void clickSearch() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(searchBtn));
        searchBtn.click();
        System.out.println("▶ Search button clicked");
        Thread.sleep(1500);
    }

    // ═══════════════════════════════════════════════
    // SEARCH IN TABLE — DataTables search box
    // ═══════════════════════════════════════════════
    public void searchInTable(String keyword)
            throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(tableSearchBox));
        tableSearchBox.clear();
        tableSearchBox.sendKeys(keyword);
        System.out.println("▶ Searching table for: " + keyword);
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // CLICK NEXT PAGE
    // ═══════════════════════════════════════════════
    public void clickNextPage() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(nextPageBtn));
        nextPageBtn.click();
        System.out.println("▶ Next page clicked");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // CLICK PREVIOUS PAGE
    // ═══════════════════════════════════════════════
    public void clickPreviousPage() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(prevPageBtn));
        prevPageBtn.click();
        System.out.println("▶ Previous page clicked");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // IS NEXT PAGE ENABLED
    // ═══════════════════════════════════════════════
    public boolean isNextPageEnabled() {
        try {
            return nextPageBtn.isEnabled() && nextPageBtn.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // OPEN UPDATE PAYMENT MODAL
    // ═══════════════════════════════════════════════
    public void clickUpdatePayment() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(updatePaymentBtn));
        updatePaymentBtn.click();
        System.out.println("▶ Update Payment modal opened");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // SELECT PAYMENT TYPE
    // @param type: "payumoney" | "hdfc" | "icici"
    // ═══════════════════════════════════════════════
    public void selectPaymentType(String type) {
        switch (type.toLowerCase()) {
            case "payumoney":
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", radioPayUMoney);
                System.out.println("✅ Payment type: PayUMoney");
                break;
            case "hdfc":
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", radioHDFC);
                System.out.println("✅ Payment type: HDFC");
                break;
            case "icici":
                ((JavascriptExecutor) driver)
                        .executeScript("arguments[0].click();", radioICICI);
                System.out.println("✅ Payment type: ICICI");
                break;
            default:
                throw new IllegalArgumentException(
                        "❌ Invalid payment type: " + type);
        }
    }

    // ═══════════════════════════════════════════════
    // IS UPDATE PAYMENT MODAL VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isUpdatePaymentModalVisible() {
        try {
            return transactionNumberInput.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // ENTER TRANSACTION NUMBER
    // ═══════════════════════════════════════════════
    public void enterTransactionNumber(String txnNumber) {
        wait.until(ExpectedConditions.visibilityOf(transactionNumberInput));
        transactionNumberInput.clear();
        transactionNumberInput.sendKeys(txnNumber);
        System.out.println("✅ Transaction number: " + txnNumber);
    }

    // ═══════════════════════════════════════════════
    // CLICK RETRIEVE DETAILS
    // ═══════════════════════════════════════════════
    public void clickRetrieveDetails() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(retrieveDetailsBtn));
        retrieveDetailsBtn.click();
        System.out.println("▶ Retrieve Details clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // IS TRANSACTION VALID
    // ✅ Checks textarea#payu_details for real content
    // ✅ Returns true  — submit details
    // ✅ Returns false — capture error and close modal
    // ═══════════════════════════════════════════════
    public boolean isTransactionValid() {
        try {
            // ✅ First check if rejected/error alert is showing
            try {
                WebElement warningAlert = driver.findElement(
                        By.cssSelector(".alert.alert-warning," +
                                " .alert.alert-danger"));
                if (warningAlert.isDisplayed() &&
                        !warningAlert.getText().trim().isEmpty()) {
                    System.out.println("⚠ Transaction alert: "
                            + warningAlert.getText().trim());
                    return false; // ✅ Failed/rejected transaction
                }
            } catch (Exception ignored) {
            }

            // ✅ Then check textarea content
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.id("payu_details")));

            String content = payuDetailsTextarea.getAttribute("value");
            if (content == null || content.trim().isEmpty()) {
                content = payuDetailsTextarea.getText();
            }
            content = content.trim();

            if (content.isEmpty()) {
                System.out.println("⚠ Transaction details empty");
                return false;
            }

            // ✅ Check if all values are null (invalid transaction)
            String stripped = content
                    .replace("null", "")
                    .replace("{", "").replace("}", "")
                    .replace("\"", "").replace(",", "")
                    .replace(":", "").replace("\n", "")
                    .replace("\r", "").replace(" ", "")
                    .trim();

            boolean isValid = !stripped.isEmpty();
            System.out.println("▶ Transaction valid: " + isValid);
            return isValid;

        } catch (Exception e) {
            System.out.println("⚠ Could not check transaction: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // GET TRANSACTION ERROR MESSAGE
    // ═══════════════════════════════════════════════
    public String getTransactionErrorMessage() {
        try {
            String[] errorSelectors = {
                    ".alert.alert-warning",    // ✅ REJ/failed status
                    ".alert.alert-danger",     // ✅ Error alerts
                    "span.label.bg-danger",    // ✅ Inline errors
                    ".error-message",
                    "[class*='error']"
            };
            for (String selector : errorSelectors) {
                try {
                    WebElement el = driver.findElement(
                            By.cssSelector(selector));
                    if (el.isDisplayed() &&
                            !el.getText().trim().isEmpty()) {
                        String msg = el.getText().trim();
                        System.out.println("▶ Transaction error: " + msg);
                        return msg;
                    }
                } catch (Exception ignored) {
                }
            }
            return "Transaction ID invalid or not found";
        } catch (Exception e) {
            return "Transaction ID invalid or not found";
        }
    }

    // ═══════════════════════════════════════════════
    // IS SUBMIT DETAILS BUTTON VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isSubmitDetailsBtnVisible() {
        try {
            new WebDriverWait(driver,
                    Duration.ofSeconds(IAutoConstant.SHORT_WAIT))
                    .until(ExpectedConditions
                            .visibilityOf(submitDetailsBtn));
            return submitDetailsBtn.isDisplayed() &&
                    submitDetailsBtn.isEnabled();
        } catch (Exception e) {
            System.out.println("⚠ Submit button not visible");
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // CLICK SUBMIT DETAILS
    // ═══════════════════════════════════════════════
    public void clickSubmitDetails() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(submitDetailsBtn));
        submitDetailsBtn.click();
        System.out.println("▶ Submit Details clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // CLOSE UPDATE PAYMENT MODAL
    // ═══════════════════════════════════════════════
    public void closeUpdatePaymentModal() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(closeUpdatePaymentBtn));
        closeUpdatePaymentBtn.click();
        System.out.println("▶ Update Payment modal closed");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // OPEN BULK UPLOAD MODAL
    // ═══════════════════════════════════════════════
    public void clickUploadPaymentsNotReceived()
            throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(uploadPaymentsNotReceivedBtn));
        uploadPaymentsNotReceivedBtn.click();
        System.out.println("▶ Bulk Upload modal opened");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // IS BULK UPLOAD MODAL VISIBLE
    // ═══════════════════════════════════════════════
    public boolean isBulkUploadModalVisible() {
        try {
            return uploadCSVInput.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // GET UPLOADER EMAIL
    // ═══════════════════════════════════════════════
    public String getUploaderEmail() {
        try {
            return uploaderEmailInput.getAttribute("value").trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // UPLOAD CSV FILE
    // ═══════════════════════════════════════════════
    public void uploadCSVFile(String filePath) {
        // ✅ Verify file exists first
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            throw new RuntimeException(
                    "❌ CSV file not found: " + filePath);
        }
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("pnr-file")));

        // ✅ Make hidden input visible via JS
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].style.display='block';" +
                        "arguments[0].style.visibility='visible';" +
                        "arguments[0].style.opacity='1';",
                uploadCSVInput);

        uploadCSVInput.sendKeys(filePath);
        System.out.println("✅ CSV file uploaded: " + filePath);
    }

    // ═══════════════════════════════════════════════
    // CLICK UPLOAD TRANSACTIONS
    // ═══════════════════════════════════════════════
    public void clickUploadTransactions() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(uploadTransactionsBtn));
        uploadTransactionsBtn.click();
        System.out.println("▶ Upload Transactions clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // CLICK DOWNLOAD SAMPLE CSV
    // ═══════════════════════════════════════════════
    public void clickDownloadSampleCSV() throws InterruptedException {
        wait.until(ExpectedConditions
                .elementToBeClickable(downloadSampleCSVBtn));
        downloadSampleCSVBtn.click();
        System.out.println("▶ Download Sample CSV clicked");
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // IS CHILD NAME IN TABLE
    // ═══════════════════════════════════════════════
    public boolean isChildNameInTable(String childName) {
        try {
            WebElement row = driver.findElement(By.xpath(
                    "//table[@id='DataTables_Table_0']" +
                            "//td[contains(text(),'" + childName + "')]"));
            System.out.println("✅ Child found: " + childName);
            return row.isDisplayed();
        } catch (Exception e) {
            System.out.println("❌ Child not found: " + childName);
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // IS CHILD ID IN TABLE
    // ═══════════════════════════════════════════════
    public boolean isChildIDInTable(String childID) {
        try {
            WebElement row = driver.findElement(By.xpath(
                    "//table[@id='DataTables_Table_0']" +
                            "//td[contains(text(),'" + childID + "')]"));
            System.out.println("✅ Child ID found: " + childID);
            return row.isDisplayed();
        } catch (Exception e) {
            System.out.println("❌ Child ID not found: " + childID);
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // HELPER — Select multiselect option by name
    // ═══════════════════════════════════════════════
    private void selectMultiselectOption(
            String selectName, String optionText)
            throws InterruptedException {

        // ✅ Click toggle button
        WebElement toggleBtn = driver.findElement(By.xpath(
                "//select[@name='" + selectName + "']" +
                        "/following-sibling::div//button" +
                        "[contains(@class,'multiselect')]"));
        toggleBtn.click();
        Thread.sleep(300);

        // ✅ Click option label
        WebElement option = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//ul[contains(@class,'multiselect-container')]" +
                                "//label[contains(.,'" + optionText + "')]")));
        option.click();
        Thread.sleep(300);

        // ✅ Close dropdown
        toggleBtn.click();
        Thread.sleep(300);
    }
}
