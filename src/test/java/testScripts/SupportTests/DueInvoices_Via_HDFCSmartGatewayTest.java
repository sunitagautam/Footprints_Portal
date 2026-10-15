package testScripts.SupportTests;

import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.*;
import pages.Navigations;
import pages.Support.CustomerPortal_PayDueInvoices;
import pages.Support.FinancialReports.AccountStatementPage;
import utils.APIs;
import utils.BaseTest;
import utils.IAutoConstant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Due Invoices payment via HDFC SmartGateway.
 *
 * Built the same way as the ICICI flow in {@link DueInvoices_Testcases}, but
 * for the newer HDFC SmartGateway integration that has replaced the ICICI
 * "Pay via Credit/Debit Card" / UPI buttons on the online payment page with a
 * single "Pay via HDFC SmartGateway" button.
 *
 * Primary (reliable) settlement path — mirrors ICICI's TC_001:
 *   Account Statement → Customer Portal → Pay Due Invoice (online payment page)
 *   → extract hidden id="payment_json_smartgateway"
 *   → POST {{Base_URL}}Eventlistener/processPayments/
 *   → invoice settled.
 *
 * Child IDs come from the same testData/Invoices_Due.csv (Status = "Payment
 * Due"), filterable with -DchildId=XXXX, exactly like the ICICI class.
 *
 * The browser-driven JusPay-simulator path (clickPayViaHdfcSmartGateway →
 * hosted page → simulator CHARGED) is wired on the page object but NOT used as
 * the default assertion yet — its hosted-page/simulator locators are
 * [LIVE-TBD] (see CustomerPortal_PayDueInvoices) and need one live run to
 * lock before being promoted to a hard-asserted test.
 */
public class DueInvoices_Via_HDFCSmartGatewayTest extends BaseTest {

    private WebDriverWait wait;
    private AccountStatementPage accountStatementPage;
    private CustomerPortal_PayDueInvoices portalPage;
    private Navigations navigations;
    private String mainWindowHandle;

    // ═══════════════════════════════════════════════
    // DATA PROVIDER — unique Child IDs from CSV
    // (same source/filter as DueInvoices_Testcases)
    // ═══════════════════════════════════════════════
    @DataProvider(name = "childIds")
    public Object[][] getChildIds() throws IOException {
        List<String> ids = readUniqueChildIds();
        Assert.assertFalse(ids.isEmpty(),
                "❌ No child IDs loaded from: " + IAutoConstant.INVOICE_REPORT_CSV);

        String filter = System.getProperty("childId", "").trim();
        if (!filter.isEmpty()) {
            ids = ids.contains(filter)
                    ? Collections.singletonList(filter)
                    : Collections.emptyList();
            Assert.assertFalse(ids.isEmpty(),
                    "❌ childId=" + filter + " not found in CSV");
            Reporter.log("▶ DataProvider: running for childId=" + filter, true);
        } else {
            Reporter.log("▶ DataProvider: " + ids.size()
                    + " unique child IDs loaded", true);
        }

        Object[][] data = new Object[ids.size()][1];
        for (int i = 0; i < ids.size(); i++) {
            data[i][0] = ids.get(i);
        }
        return data;
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        accountStatementPage = new AccountStatementPage(driver);
        portalPage = new CustomerPortal_PayDueInvoices(driver);
        navigations = new Navigations(driver);
        mainWindowHandle = driver.getWindowHandle();
        System.out.println("✅ Page objects initialized");
    }

    @BeforeMethod(alwaysRun = true)
    public void navigateToAccountStatement() throws InterruptedException {
        closeAllExtraTabsSafely();
        Thread.sleep(500);
        navigations.goToAccountStatement();
        System.out.println("▶ Ready: Account Statement");
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUpAfterTest() throws InterruptedException {
        closeAllExtraTabsSafely();
    }

    // ═══════════════════════════════════════════════════════════
    // TC_SG_001 — Extract SmartGateway JSON + POST processPayments
    //
    // Reliable, DOM-confirmed path (mirrors ICICI TC_001).
    // ═══════════════════════════════════════════════════════════
    @Test(priority = 1,
            dataProvider = "childIds",
            description = "SC_SG_TC_001 — Extract HDFC SmartGateway payment JSON and POST processPayments")
    public void extractSmartGatewayJsonAndPostApi(String childId)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ SC_SG_TC_001 — SmartGateway JSON via API | Child: "
                + childId, true);

        // Step 1 — Generate Account Statement for child
        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);
        Reporter.log("✅ Step 1 — Account Statement generated", true);

        // Step 2 — Customer Portal (billing tab)
        Set<String> tabsBefore = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(tabsBefore);
        Reporter.log("✅ Step 2 — Customer Portal (billing) opened", true);

        // Step 3 — Any due invoices?
        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            Reporter.log("⚠ Child " + childId
                    + " — No due invoices found, skipping", true);
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            throw new org.testng.SkipException(
                    "No due invoices for child " + childId);
        }

        // Step 4 — Pay Due Invoice (online payment tab)
        Set<String> tabsBefore2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(tabsBefore2);
        Reporter.log("✅ Step 4 — Online payment page opened", true);

        // Step 5 — Extract hidden SmartGateway JSON
        String sgJson = portalPage.extractSmartGatewayPaymentJson();
        Assert.assertFalse(sgJson.isEmpty(),
                "❌ SmartGateway JSON empty for child: " + childId);
        Assert.assertTrue(sgJson.toLowerCase().contains("hdfc smart"),
                "❌ JSON does not look like a SmartGateway payload: " + sgJson);
        Reporter.log("✅ Step 5 — SmartGateway JSON extracted: " + sgJson, true);

        // Step 6 — POST to Eventlistener/processPayments/
        Response resp = APIs.postProcessPayments(sgJson);
        Assert.assertTrue(
                resp.getStatusCode() >= 200 && resp.getStatusCode() < 300,
                "❌ processPayments failed — HTTP " + resp.getStatusCode()
                        + " | " + resp.getBody().asString());
        Reporter.log("✅ Step 6 — processPayments: HTTP "
                + resp.getStatusCode(), true);

        // Step 7 — Mark child as Paid in CSV
        markChildAsPaid(childId);
        Reporter.log("✅ Step 7 — CSV updated: Child " + childId + " → Paid", true);

        Reporter.log("✅ Child       : " + childId, true);
        Reporter.log("✅ SmartGW JSON: " + sgJson, true);
        Reporter.log("✅ API Status  : " + resp.getStatusCode(), true);
        Reporter.log("✅ API Response: " + resp.getBody().asString(), true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ SC_SG_TC_001 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════════════════
    // TC_SG_002 — Full browser flow via JusPay UAT simulator (CHARGED)
    //
    // [LIVE-TBD] The hosted-page method-selection and simulator
    // dropdown locators are provisional (see CustomerPortal_PayDueInvoices).
    // This test is enabled=false until a live exploration run locks them.
    // ═══════════════════════════════════════════════════════════
    @Test(priority = 2,
            enabled = false,
            dataProvider = "childIds",
            description = "SC_SG_TC_002 — Pay via HDFC SmartGateway through JusPay simulator (CHARGED)")
    public void payViaSmartGatewaySimulator(String childId)
            throws InterruptedException {

        Reporter.log("▶ SC_SG_TC_002 — SmartGateway simulator | Child: "
                + childId, true);

        accountStatementPage.generateAccountStatement(childId);
        Thread.sleep(1000);

        Set<String> tabsBefore = driver.getWindowHandles();
        accountStatementPage.clickCustomerPortal();
        portalPage.waitAndSwitchToNewTab(tabsBefore);

        if (!portalPage.isPayDueInvoiceBtnPresent()) {
            portalPage.closeAllExtraTabsAndReturn(mainWindowHandle);
            throw new org.testng.SkipException(
                    "No due invoices for child " + childId);
        }

        Set<String> tabsBefore2 = driver.getWindowHandles();
        portalPage.clickPayDueInvoice();
        portalPage.waitAndSwitchToNewTab(tabsBefore2);

        // Click "Pay via HDFC SmartGateway" → hosted page
        portalPage.clickPayViaHdfcSmartGateway();
        Assert.assertTrue(portalPage.isHdfcHostedPageLoaded(),
                "❌ HDFC SmartGateway hosted page did not load");
        String orderId = portalPage.getHdfcOrderId();
        Reporter.log("✅ Hosted page — Order ID: " + orderId, true);

        // Reach + drive the UAT simulator (CHARGED)
        Assert.assertTrue(portalPage.waitForJuspaySimulator(),
                "❌ JusPay UAT simulator not reached");
        portalPage.simulateTransactionState("CHARGED");
        Assert.assertTrue(portalPage.isSimulatorStatusCaptured("CHARGED"),
                "❌ Simulator did not confirm CHARGED");

        // Return to Footprints
        Assert.assertTrue(portalPage.waitForReturnToFootprints(),
                "❌ Did not redirect back to Footprints after payment");

        markChildAsPaid(childId);
        Reporter.log("✅ SC_SG_TC_002 — Child " + childId
                + " paid via simulator (Order " + orderId + ")", true);
        System.out.println("✅ SC_SG_TC_002 PASSED — Child: " + childId);
    }

    // ═══════════════════════════════════════════════
    // HELPERS (copied from DueInvoices_Testcases — per
    // the project convention of per-class duplication
    // rather than editing the existing shared class)
    // ═══════════════════════════════════════════════
    private List<String> readUniqueChildIds() throws IOException {
        List<String> result = new ArrayList<>();
        File csv = new File(IAutoConstant.INVOICE_REPORT_CSV);
        System.out.println("▶ Reading from: " + csv.getAbsolutePath());
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length < 2) continue;
                String childId = parts[0].trim();
                String status = parts[1].trim();
                if (!childId.isEmpty() && "Payment Due".equalsIgnoreCase(status)) {
                    result.add(childId);
                }
            }
        }
        System.out.println("✅ " + result.size()
                + " children with Payment Due status loaded");
        return result;
    }

    private void markChildAsPaid(String childId) {
        File csv = new File(IAutoConstant.INVOICE_REPORT_CSV);
        List<String> lines = new ArrayList<>();
        boolean updated = false;
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2
                        && parts[0].trim().equals(childId)
                        && "Payment Due".equalsIgnoreCase(parts[1].trim())) {
                    lines.add(parts[0].trim() + ",Paid");
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("⚠ markChildAsPaid — read error: " + e.getMessage());
            return;
        }
        if (!updated) {
            System.out.println("⚠ markChildAsPaid — child " + childId
                    + " not found or already Paid");
            return;
        }
        try (java.io.FileWriter fw = new java.io.FileWriter(csv, false)) {
            for (String l : lines) {
                fw.write(l + System.lineSeparator());
            }
            System.out.println("✅ CSV updated: Child " + childId + " → Paid");
        } catch (IOException e) {
            System.out.println("⚠ markChildAsPaid — write error: " + e.getMessage());
        }
    }

    private void closeAllExtraTabsSafely() throws InterruptedException {
        try {
            if (mainWindowHandle == null) return;
            for (String handle : new ArrayList<>(driver.getWindowHandles())) {
                if (!handle.equals(mainWindowHandle)) {
                    driver.switchTo().window(handle);
                    driver.close();
                }
            }
            driver.switchTo().window(mainWindowHandle);
        } catch (Exception e) {
            System.out.println("⚠ Tab cleanup: " + e.getMessage());
        }
        Thread.sleep(300);
    }
}
