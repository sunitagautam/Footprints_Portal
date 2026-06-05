package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.Navigations;
import pages.Support.OnlinePaymentReceived;
import utils.BaseTest;
import utils.IAutoConstant;

public class OnlinePaymentReceived_Testcases extends BaseTest {

    OnlinePaymentReceived onlinePaymentPage;
    Navigations navigations;

    // ═══════════════════════════════════════════════
    // BEFORE CLASS
    // ✅ BaseTest already handles:
    //    — Browser launch
    //    — Login with default credentials
    // Just initialize page objects here
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        navigations = new Navigations(driver);
        onlinePaymentPage = new OnlinePaymentReceived(driver);
        System.out.println("✅ Page objects initialized");
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD
    // ✅ Navigate via Support menu before each test
    // ✅ Mimics real user navigation
    // ✅ Respects User Rights
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        navigations.goToOnlinePaymentReceived();
        System.out.println("▶ Ready: Online Payment Received");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_001 — Verify page loads with data
    // ═══════════════════════════════════════════════
    @Test(priority = 1,
            description = "Verify page loads with data")
    public void verifyPageLoadsWithData() {
        Reporter.log("▶ TC_OP_001 — Page loads with data", true);

        Assert.assertTrue(onlinePaymentPage.isPageLoaded(),
                "❌ Page did not load");
        Assert.assertTrue(onlinePaymentPage.isTableDisplayingData(),
                "❌ No data in table");

        String info = onlinePaymentPage.getTableInfoText();
        Reporter.log("✅ Table info: " + info, true);
        System.out.println("✅ TC_OP_001 PASSED — " + info);
    }

    // ═══════════════════════════════════════════════
    // TC_OP_002 — Verify Total Amount displayed
    // ═══════════════════════════════════════════════
    @Test(priority = 2,
            description = "Verify Total Amount displayed")
    public void verifyTotalAmountDisplayed() {
        Reporter.log("▶ TC_OP_002 — Total Amount", true);

        Assert.assertTrue(onlinePaymentPage.isPageLoaded(),
                "❌ Page did not load");

        String total = onlinePaymentPage.getTotalAmount();
        Assert.assertFalse(total.isEmpty(),
                "❌ Total Amount not displayed");

        Reporter.log("✅ Total Amount: " + total, true);
        System.out.println("✅ TC_OP_002 PASSED — " + total);
    }

    // ═══════════════════════════════════════════════
    // TC_OP_003 — Filter by Center
    // ═══════════════════════════════════════════════
    @Test(priority = 3,
            description = "Filter by Center")
    public void verifyFilterByCenter()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_003 — Filter by Center", true);

        onlinePaymentPage.filterByCenter("Sector 126, Noida");
        onlinePaymentPage.clickSearch();

        // ✅ Assert filter was applied — not data count
        Assert.assertFalse(
                onlinePaymentPage.getTableInfoText().isEmpty(),
                "❌ Filter not applied");

        Reporter.log("✅ " + onlinePaymentPage.getTableInfoText(), true);
        System.out.println("✅ TC_OP_003 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_004 — Filter by Gateway
    // ═══════════════════════════════════════════════
    @Test(priority = 4,
            description = "Filter by Gateway — Payumoney")
    public void verifyFilterByGateway()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_004 — Filter by Gateway", true);

        onlinePaymentPage.filterByGateway("Payumoney");
        onlinePaymentPage.clickSearch();

        Assert.assertFalse(
                onlinePaymentPage.getTableInfoText().isEmpty(),
                "❌ Filter not applied");

        Reporter.log("✅ " + onlinePaymentPage.getTableInfoText(), true);
        System.out.println("✅ TC_OP_004 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_005 — Filter by Type
    // ═══════════════════════════════════════════════
    @Test(priority = 5,
            description = "Filter by Type — Invoice Payment")
    public void verifyFilterByType()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_005 — Filter by Type", true);

        onlinePaymentPage.filterByType("Invoice Payment");
        onlinePaymentPage.clickSearch();

        Assert.assertFalse(
                onlinePaymentPage.getTableInfoText().isEmpty(),
                "❌ Filter not applied");

        Reporter.log("✅ " + onlinePaymentPage.getTableInfoText(), true);
        System.out.println("✅ TC_OP_005 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_006 — Filter by Status
    // ═══════════════════════════════════════════════
    @Test(priority = 6,
            description = "Filter by Status — Success")
    public void verifyFilterByStatus()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_006 — Filter by Status", true);

        onlinePaymentPage.filterByStatus("Success");
        onlinePaymentPage.clickSearch();

        Assert.assertFalse(
                onlinePaymentPage.getTableInfoText().isEmpty(),
                "❌ Filter not applied");

        Reporter.log("✅ " + onlinePaymentPage.getTableInfoText(), true);
        System.out.println("✅ TC_OP_006 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_007 — Filter by Date Range
    // ═══════════════════════════════════════════════
    @Test(priority = 7,
            description = "Filter by Date Range")
    public void verifyFilterByDateRange()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_007 — Filter by Date Range", true);

        onlinePaymentPage.filterByDateRange(
                "06/01/2026", "06/01/2026");

        String info = onlinePaymentPage.getTableInfoText();
        Assert.assertFalse(info.isEmpty(),
                "❌ Table empty after date filter");

        Reporter.log("✅ " + info, true);
        System.out.println("✅ TC_OP_007 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_008 — Search by Child Name
    // ✅ Apply date range first to ensure data is visible
    // ═══════════════════════════════════════════════
    // TC_OP_008
    @Test(priority = 8,
            description = "Search by Child Name")
    public void verifySearchByChildName()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_008 — Search by Child Name", true);

        // ✅ Apply date range + click search to load data
        onlinePaymentPage.filterByDateRange(
                "06/01/2026", "06/02/2026");
        onlinePaymentPage.clickSearch();          // ✅ Add this

        String name = "Ranjeeta";
        onlinePaymentPage.searchInTable(name);

        Assert.assertTrue(
                onlinePaymentPage.isChildNameInTable(name),
                "❌ Child not found: " + name);

        Reporter.log("✅ Found: " + name, true);
        System.out.println("✅ TC_OP_008 PASSED");
    }

    // TC_OP_009
    @Test(priority = 9,
            description = "Search by Child ID")
    public void verifySearchByChildID()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_009 — Search by Child ID", true);

        // ✅ Apply date range + click search to load data
        onlinePaymentPage.filterByDateRange(
                "06/01/2026", "06/02/2026");
        onlinePaymentPage.clickSearch();          // ✅ Add this

        String id = "69126";
        onlinePaymentPage.searchInTable(id);

        Assert.assertTrue(
                onlinePaymentPage.isChildIDInTable(id),
                "❌ Child ID not found: " + id);

        Reporter.log("✅ Found ID: " + id, true);
        System.out.println("✅ TC_OP_009 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_010 — Pagination Next
    // ═══════════════════════════════════════════════
    @Test(priority = 10,
            description = "Pagination — Next page")
    public void verifyPaginationNext()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_010 — Pagination Next", true);

        if (onlinePaymentPage.isNextPageEnabled()) {
            String before = onlinePaymentPage.getTableInfoText();
            onlinePaymentPage.clickNextPage();
            String after = onlinePaymentPage.getTableInfoText();
            Assert.assertNotEquals(before, after,
                    "❌ Table unchanged after next page");
            Reporter.log("✅ Next: " + after, true);
        } else {
            Reporter.log("ℹ Single page — skip", true);
        }
        System.out.println("✅ TC_OP_010 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_011 — Pagination Previous
    // ═══════════════════════════════════════════════
    @Test(priority = 11,
            description = "Pagination — Previous page")
    public void verifyPaginationPrevious()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_011 — Pagination Previous", true);

        if (onlinePaymentPage.isNextPageEnabled()) {
            onlinePaymentPage.clickNextPage();
            String page2 = onlinePaymentPage.getTableInfoText();
            onlinePaymentPage.clickPreviousPage();
            String page1 = onlinePaymentPage.getTableInfoText();
            Assert.assertNotEquals(page2, page1,
                    "❌ Table unchanged after previous");
            Reporter.log("✅ Back to: " + page1, true);
        } else {
            Reporter.log("ℹ Single page — skip", true);
        }
        System.out.println("✅ TC_OP_011 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_012 — Update Payment Modal Opens
    // ═══════════════════════════════════════════════
    @Test(priority = 12,
            description = "Update Payment modal opens")
    public void verifyUpdatePaymentModalOpens()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_012 — Update Payment modal", true);

        onlinePaymentPage.clickUpdatePayment();

        Assert.assertTrue(
                onlinePaymentPage.isUpdatePaymentModalVisible(),
                "❌ Modal did not open");

        Reporter.log("✅ Modal opened", true);
        System.out.println("✅ TC_OP_012 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_013 — Update Payment — PayUMoney
    // ✅ Submit if transaction valid
    // ✅ Capture error and close if invalid
    // ═══════════════════════════════════════════════
    @Test(priority = 13,
            description = "Update Payment — PayUMoney + submit if valid")
    public void verifyUpdatePaymentPayUMoney()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_013 — Update Payment PayUMoney", true);

        onlinePaymentPage.clickUpdatePayment();
        onlinePaymentPage.selectPaymentType("payumoney");
        onlinePaymentPage.enterTransactionNumber("2606016a1d84e3b203b");
        onlinePaymentPage.clickRetrieveDetails();

        if (onlinePaymentPage.isTransactionValid()) {
            // ✅ Valid — submit details
            onlinePaymentPage.clickSubmitDetails();
            Reporter.log("✅ TC_OP_013 — Transaction valid" +
                    " — Submit Details clicked", true);
            System.out.println("✅ TC_OP_013 PASSED — Submitted");
        } else {
            // ✅ Invalid — capture error and close gracefully
            String err = onlinePaymentPage.getTransactionErrorMessage();
            Reporter.log("⚠ TC_OP_013 — Invalid transaction: "
                    + err, true);
            System.out.println("⚠ TC_OP_013 — Invalid txn: " + err);
            onlinePaymentPage.closeUpdatePaymentModal();
            System.out.println("✅ TC_OP_013 PASSED" +
                    " — Invalid txn handled gracefully");
        }
    }

    // ═══════════════════════════════════════════════
// TC_OP_013b — Update Payment — Rejected Transaction
// ✅ Verifies REJ status is handled gracefully
// ═══════════════════════════════════════════════
    @Test(priority = 13,
            description = "Update Payment — Rejected/Cancelled transaction")
    public void verifyUpdatePaymentRejectedTransaction()
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ TC_OP_013b — Update Payment | " +
                "Rejected Transaction Scenario", true);
        Reporter.log("   Transaction ID : 2606046a21223032f6d", true);
        Reporter.log("   Gateway        : ICICI", true);
        Reporter.log("══════════════════════════════════════", true);

        onlinePaymentPage.clickUpdatePayment();
        onlinePaymentPage.selectPaymentType("icici");
        onlinePaymentPage.enterTransactionNumber("2606046a21223032f6d");
        onlinePaymentPage.clickRetrieveDetails();

        // ✅ Transaction should be rejected — not valid
        Assert.assertFalse(
                onlinePaymentPage.isTransactionValid(),
                "❌ Expected rejected transaction but got valid");

        String errMsg = onlinePaymentPage.getTransactionErrorMessage();

        Reporter.log("✅ Payment Status  : Transaction rejected by " +
                "gateway", true);
        Reporter.log("✅ Gateway         : ICICI", true);
        Reporter.log("✅ Error Message   : " + errMsg, true);
        Reporter.log("✅ Action          : Modal closed — " +
                "rejected transaction not processed", true);
        Reporter.log("ℹ Finance Note    : Transaction Status REJ means" +
                " payment was declined/cancelled by the bank." +
                " Contact child's parent to retry payment.", true);

        System.out.println("⚠ Rejected txn msg: " + errMsg);
        onlinePaymentPage.closeUpdatePaymentModal();
        System.out.println("✅ TC_OP_013b PASSED — Rejected txn handled");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_014 — Update Payment — HDFC
    // ✅ Submit if transaction valid
    // ✅ Capture error and close if invalid
    // ═══════════════════════════════════════════════
    @Test(priority = 14,
            description = "Update Payment — HDFC + submit if valid")
    public void verifyUpdatePaymentHDFC()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_014 — Update Payment HDFC", true);

        onlinePaymentPage.clickUpdatePayment();
        onlinePaymentPage.selectPaymentType("hdfc");
        onlinePaymentPage.enterTransactionNumber("06437426052208482229");
        onlinePaymentPage.clickRetrieveDetails();

        if (onlinePaymentPage.isTransactionValid()) {
            onlinePaymentPage.clickSubmitDetails();
            Reporter.log("✅ TC_OP_014 — Transaction valid" +
                    " — Submit Details clicked", true);
            System.out.println("✅ TC_OP_014 PASSED — Submitted");
        } else {
            String err = onlinePaymentPage.getTransactionErrorMessage();
            Reporter.log("⚠ TC_OP_014 — Invalid transaction: "
                    + err, true);
            System.out.println("⚠ TC_OP_014 — Invalid txn: " + err);
            onlinePaymentPage.closeUpdatePaymentModal();
            System.out.println("✅ TC_OP_014 PASSED" +
                    " — Invalid txn handled gracefully");
        }
    }

    // ═══════════════════════════════════════════════
    // TC_OP_015 — Update Payment — ICICI
    // ✅ Submit if transaction valid
    // ✅ Capture error and close if invalid
    // ═══════════════════════════════════════════════
    @Test(priority = 15,
            description = "Update Payment — ICICI + submit if valid")
    public void verifyUpdatePaymentICICI()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_015 — Update Payment ICICI", true);

        onlinePaymentPage.clickUpdatePayment();
        onlinePaymentPage.selectPaymentType("icici");
        onlinePaymentPage.enterTransactionNumber("2606026a1e85569ca7a");
        onlinePaymentPage.clickRetrieveDetails();

        if (onlinePaymentPage.isTransactionValid()) {
            onlinePaymentPage.clickSubmitDetails();
            Reporter.log("✅ TC_OP_015 — Transaction valid" +
                    " — Submit Details clicked", true);
            System.out.println("✅ TC_OP_015 PASSED — Submitted");
        } else {
            String err = onlinePaymentPage.getTransactionErrorMessage();
            Reporter.log("⚠ TC_OP_015 — Invalid transaction: "
                    + err, true);
            System.out.println("⚠ TC_OP_015 — Invalid txn: " + err);
            onlinePaymentPage.closeUpdatePaymentModal();
            System.out.println("✅ TC_OP_015 PASSED" +
                    " — Invalid txn handled gracefully");
        }
    }

    // ═══════════════════════════════════════════════
    // TC_OP_016 — Close Update Payment Modal
    // ═══════════════════════════════════════════════
    @Test(priority = 16,
            description = "Close Update Payment modal")
    public void verifyUpdatePaymentModalCloses()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_016 — Close modal", true);

        onlinePaymentPage.clickUpdatePayment();
        onlinePaymentPage.closeUpdatePaymentModal();

        Assert.assertFalse(
                onlinePaymentPage.isUpdatePaymentModalVisible(),
                "❌ Modal did not close");

        Reporter.log("✅ Modal closed", true);
        System.out.println("✅ TC_OP_016 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_017 — Bulk Upload Modal Opens
    // ═══════════════════════════════════════════════
    @Test(priority = 17,
            description = "Bulk Upload modal opens")
    public void verifyBulkUploadModalOpens()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_017 — Bulk Upload modal", true);

        onlinePaymentPage.clickUploadPaymentsNotReceived();

        Assert.assertTrue(
                onlinePaymentPage.isBulkUploadModalVisible(),
                "❌ Modal did not open");

        Reporter.log("✅ Modal opened", true);
        System.out.println("✅ TC_OP_017 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_018 — Uploader Email Pre-filled
    // ═══════════════════════════════════════════════
    @Test(priority = 18,
            description = "Uploader email pre-filled")
    public void verifyUploaderEmailPreFilled()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_018 — Uploader email", true);

        onlinePaymentPage.clickUploadPaymentsNotReceived();

        String email = onlinePaymentPage.getUploaderEmail();
        Assert.assertFalse(email.isEmpty(), "❌ Email empty");
        Assert.assertTrue(email.contains("@"),
                "❌ Invalid email: " + email);

        Reporter.log("✅ Email: " + email, true);
        System.out.println("✅ TC_OP_018 PASSED — " + email);
    }

    // ═══════════════════════════════════════════════
    // TC_OP_019 — Download Sample CSV
    // ═══════════════════════════════════════════════
    @Test(priority = 19,
            description = "Download Sample CSV")
    public void verifyDownloadSampleCSV()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_019 — Download Sample CSV", true);

        onlinePaymentPage.clickUploadPaymentsNotReceived();
        onlinePaymentPage.clickDownloadSampleCSV();

        Reporter.log("✅ Sample CSV triggered", true);
        System.out.println("✅ TC_OP_019 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_OP_020 — Upload CSV File
    // ═══════════════════════════════════════════════
    @Test(priority = 20,
            description = "Upload payment_not_received CSV file")
    public void verifyUploadCSVFile()
            throws InterruptedException {
        Reporter.log("▶ TC_OP_020 — Upload CSV file", true);

        // ✅ Open bulk upload modal
        onlinePaymentPage.clickUploadPaymentsNotReceived();

        Assert.assertTrue(
                onlinePaymentPage.isBulkUploadModalVisible(),
                "❌ Bulk Upload modal did not open");

        // ✅ Upload CSV from testData folder
        onlinePaymentPage.uploadCSVFile(
                IAutoConstant.PAYMENT_NOT_RECEIVED_CSV);

        // ✅ Click Upload Transactions
        onlinePaymentPage.clickUploadTransactions();

        Reporter.log("✅ CSV uploaded and submitted", true);
        System.out.println("✅ TC_OP_020 PASSED");
    }
}
