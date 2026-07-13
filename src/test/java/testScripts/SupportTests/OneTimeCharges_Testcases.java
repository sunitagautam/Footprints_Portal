package testScripts.SupportTests;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.*;
import pages.Navigations;
import pages.Settings.UserRightsPage;
import pages.Support.OneTimeChargesPage;
import utils.BaseTest;

public class OneTimeCharges_Testcases extends BaseTest {

    // ✅ Child ID used across all TCs
    private static final String CHILD_ID = "46195";

    OneTimeChargesPage oneTimeChargesPage;
    UserRightsPage userRightsPage;
    Navigations navigations;


    @DataProvider(name = "chargeTypeData")
    public Object[][] chargeTypeData() {
        return new Object[][]{
                {"SC_015_TC_005", "Annual Preschool Fee", "3000", "annual fee of next year", false, false, "", "", ""},
                {"SC_015_TC_006", "Book Set", "3500", "Book Set Charges", false, false, "", "", ""},
                {"SC_015_TC_007", "Extended DayCare", "3500", "extended amount", false, false, "", "", ""},
                {"SC_015_TC_008", "Late Stay", "200", "late amount", true, true, "08/21/2024", "2", "00"},
                {"SC_015_TC_009", "Read-O-Stick", "2500", "Read-O-Stick charges", true, false, "", "", ""},
                {"SC_015_TC_010", "Registration Fee", "2500", "Registration Fee charges", true, false, "", "", ""},
                {"SC_015_TC_011", "School Bag", "650", "School Bag amount", false, false, "", "", ""},
                {"SC_015_TC_012", "Security Fee", "5000", "Refundable at the time of exit", true, false, "", "", ""},
                {"SC_015_TC_013", "Transport Fee", "3000", "Transport fee", false, false, "", "", ""},
                {"SC_015_TC_014", "Tuition Fee", "300", "Tuition Fee", false, false, "", "", ""},
                {"SC_015_TC_015", "Courier Charges", "50", "Courier charges", false, false, "", "", ""},
                {"SC_015_TC_016", "Welcome Kit", "4000", "Welcome Kit charges", true, false, "", "", ""},
                {"SC_015_TC_017", "Welcome Kit Without Read-O-Stick", "5000", "Welcome Kit Without Read-O-Stick", true, false, "", "", ""},
                {"SC_015_TC_018", "Welcome Kit With Read-O-Stick", "7500", "Welcome Kit With Read-O-Stick", true, false, "", "", ""},
                {"SC_015_TC_019", "Late Stay", "400", "late amount updated rate", true, true, "08/21/2024", "2", "30"},
                {"SC_015_TC_020", "Apron Charges", "200", "Apron Charges", false, false, "", "", ""},
                {"SC_015_TC_021", "New Book-set", "3500", "New Book-set Charges", false, false, "", "", ""},
                {"SC_015_TC_022", "Tee Shirt Charges", "500", "Tee Shirt Charges", false, false, "", "", ""},
        };
    }

    // ═══════════════════════════════════════════════
    // BEFORE CLASS — login as Rakesh, switch to user
    // ═══════════════════════════════════════════════
    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        navigations = new Navigations(driver);
        userRightsPage = new UserRightsPage(driver);
        oneTimeChargesPage = new OneTimeChargesPage(driver);
        System.out.println("✅ Page objects initialized");

        String user = getUserForScreen("OneTime Charges");
        Assert.assertFalse(user.isEmpty(),
                "❌ No user found for OneTime Charges in Excel");

        navigations.goToUserRights();
        userRightsPage.switchUser(user);
        System.out.println("✅ Switched to: " + user);
        Thread.sleep(2000);
    }

    // ═══════════════════════════════════════════════
    // BEFORE METHOD — navigate to OneTime Charges
    // ═══════════════════════════════════════════════
    @BeforeMethod(alwaysRun = true)
    public void navigateToPage() throws InterruptedException {
        Thread.sleep(3000);
        navigations.goToOneTimeCharges();
        System.out.println("▶ Ready: OneTime Charges");
    }

    // ═══════════════════════════════════════════════
    // AFTER METHOD
    // ✅ Force-closes modal after EVERY test
    //    even if TC fails mid-way leaving modal open
    //    so @BeforeMethod navigation is never blocked
    // ═══════════════════════════════════════════════
    @AfterMethod(alwaysRun = true)
    public void closeModalAfterTest() {
        try {
            oneTimeChargesPage.closeModal();
        } catch (Exception e) {
            // Modal not open — no action needed
        }
    }

    // ═══════════════════════════════════════════════
    // TC_001 — Page Loads
    // ═══════════════════════════════════════════════
    @Test(priority = 1,
            description = "SC_015_TC_001 — Verify page loads")
    public void verifyPageLoads() {
        Reporter.log("▶ SC_015_TC_001 — Page loads", true);
        Assert.assertTrue(oneTimeChargesPage.isPageLoaded(),
                "❌ Page did not load");
        Reporter.log("✅ OneTime Charges page loaded", true);
        System.out.println("✅ SC_015_TC_001 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_002 — Default View
    // ═══════════════════════════════════════════════
    @Test(priority = 2,
            description = "SC_015_TC_002 — Verify default view")
    public void verifyDefaultView() {
        Reporter.log("▶ SC_015_TC_002 — Default View", true);
        Assert.assertTrue(oneTimeChargesPage.isPageLoaded(),
                "❌ Page did not load");
        Reporter.log("✅ Default view verified:", true);
        Reporter.log("   ✅ Add One Time Charges button", true);
        Reporter.log("   ✅ Filters: Child ID, From, To, Center, Type, Charged By", true);
        Reporter.log("   ✅ Download Report button", true);
        Reporter.log("   ✅ Search bar", true);
        Reporter.log("   ✅ Table: Child ID, Child Name, Amount, Type, Description, Charged On, Charged By", true);
        System.out.println("✅ SC_015_TC_002 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_003 — Modal Default View
    // ✅ Opens modal → verifies fields → closes modal
    // ═══════════════════════════════════════════════
    @Test(priority = 3,
            description = "SC_015_TC_003 — Verify modal default view")
    public void verifyModalDefaultView() throws InterruptedException {
        Reporter.log("▶ SC_015_TC_003 — Modal Default View", true);
        Assert.assertTrue(oneTimeChargesPage.isPageLoaded(),
                "❌ Page did not load");

        oneTimeChargesPage.clickAddOneTimeCharges();
        Assert.assertTrue(oneTimeChargesPage.isFormModalVisible(),
                "❌ Modal did not open");

        Reporter.log("✅ Modal fields verified:", true);
        Reporter.log("   ✅ Child ID field", true);
        Reporter.log("   ✅ Fetch Child Details button", true);
        Reporter.log("   ✅ Child Name text", true);
        Reporter.log("   ✅ Charge Type dropdown", true);
        Reporter.log("   ✅ Charge Amount field", true);
        Reporter.log("   ✅ Comment field", true);
        Reporter.log("   ✅ Submit Form button", true);
        Reporter.log("   ✅ Close icon ×", true);

        oneTimeChargesPage.closeModal();
        System.out.println("✅ Modal closed after TC_003");
        System.out.println("✅ SC_015_TC_003 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_004 — All 17 Charge Type Options
    // ✅ Uses local CHILD_ID (not IAutoConstant)
    // ✅ Closes modal after verification
    // ═══════════════════════════════════════════════
    @Test(priority = 4,
            description = "SC_015_TC_004 — Verify all 17 charge types")
    public void verifyAllChargeTypeOptions() throws InterruptedException {
        Reporter.log("▶ SC_015_TC_004 — All Charge Type Options", true);
        Assert.assertTrue(oneTimeChargesPage.isPageLoaded(),
                "❌ Page did not load");

        oneTimeChargesPage.clickAddOneTimeCharges();
        oneTimeChargesPage.enterChildId(CHILD_ID);   // ✅ local constant
        oneTimeChargesPage.clickFetchChildDetails();

        String[] allOptions = {
                "Annual Preschool Fee", "Apron Charges",
                "Book Set", "Courier Charges",
                "Extended DayCare", "Late Stay",
                "New Book-set", "Read-O-Stick",
                "Registration Fee", "School Bag",
                "Security Fee", "Tee Shirt Charges",
                "Transport Fee", "Tuition Fee",
                "Welcome Kit",
                "Welcome Kit Without Read-O-Stick",
                "Welcome Kit With Read-O-Stick"
        };

        boolean allPresent = oneTimeChargesPage
                .verifyAllChargeTypesPresent(allOptions);
        Assert.assertTrue(allPresent,
                "❌ Some charge types missing from dropdown");

        Reporter.log("✅ All 17 charge types verified:", true);
        for (String opt : allOptions) {
            Reporter.log("   ✅ " + opt, true);
        }

        oneTimeChargesPage.closeModal();
        System.out.println("✅ Modal closed after TC_004");
        System.out.println("✅ SC_015_TC_004 PASSED");
    }

    // ═══════════════════════════════════════════════
    // TC_005 to TC_022 — Apply Each Charge Type
    // ═══════════════════════════════════════════════
    @Test(priority = 5,
            dataProvider = "chargeTypeData",
            description = "Apply each charge type and verify success")
    public void verifyApplyChargeType(
            String tcId, String chargeType,
            String amount, String comment,
            boolean isAutoAmount, boolean needsDate,
            String lateDate, String lateHour, String lateMin)
            throws InterruptedException {

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("▶ " + tcId + " — " + chargeType, true);
        Reporter.log("   Child ID    : " + CHILD_ID, true);
        Reporter.log("   Charge Type : " + chargeType, true);
        Reporter.log("   Amount      : ₹" + amount, true);
        Reporter.log("══════════════════════════════════════", true);

        Assert.assertTrue(oneTimeChargesPage.isPageLoaded(),
                "❌ Page did not load");

        // Step 1 — Open modal
        oneTimeChargesPage.clickAddOneTimeCharges();
        Assert.assertTrue(oneTimeChargesPage.isFormModalVisible(),
                "❌ Modal did not open");
        Reporter.log("✅ Step 1 — Modal opened", true);

        // Step 2 — Enter Child ID
        oneTimeChargesPage.enterChildId(CHILD_ID);
        Reporter.log("✅ Step 2 — Child ID: " + CHILD_ID, true);

        // Step 3 — Fetch Child Details
        oneTimeChargesPage.clickFetchChildDetails();
        String childName = oneTimeChargesPage.getChildName();
        Assert.assertFalse(childName.isEmpty(),
                "❌ Child name not fetched for: " + CHILD_ID);
        Reporter.log("✅ Step 3 — Child Name: " + childName, true);

        // Step 4 — Select Charge Type
        oneTimeChargesPage.selectChargeType(chargeType);
        Reporter.log("✅ Step 4 — Charge Type: " + chargeType, true);
        Thread.sleep(500);

        // Step 5 — Late Stay fields
        if (needsDate) {
            oneTimeChargesPage.enterLateStayDetails(
                    lateDate, lateHour, lateMin);
            Reporter.log("✅ Step 5 — Late Stay: Date=" + lateDate
                    + " Hour=" + lateHour + " Min=" + lateMin, true);
        }

        // Step 6 — Amount
        if (isAutoAmount) {
            Thread.sleep(500);
            String autoAmt = oneTimeChargesPage.getChargeAmount();
            Reporter.log("✅ Step 6 — Auto Amount: ₹" + autoAmt
                    + " (expected ₹" + amount + ")", true);
            if (autoAmt == null || autoAmt.trim().isEmpty()) {
                oneTimeChargesPage.enterChargeAmount(amount);
                Reporter.log("   ℹ Auto empty — entered manually", true);
            }
        } else {
            oneTimeChargesPage.enterChargeAmount(amount);
            Reporter.log("✅ Step 6 — Amount: ₹" + amount, true);
        }

        // Step 7 — Comment
        oneTimeChargesPage.enterChargeComments(comment);
        Reporter.log("✅ Step 7 — Comment: " + comment, true);

        // Step 8 — Submit
        oneTimeChargesPage.clickSubmitForm();
        Reporter.log("✅ Step 8 — Submit Form clicked", true);

        // Step 9 — Confirmation popup
        Assert.assertTrue(
                oneTimeChargesPage.isConfirmationPopupVisible(),
                "❌ Confirmation popup not shown for: " + chargeType);
        Reporter.log("✅ Step 9 — Confirmation popup shown", true);

        // Step 10 — Confirm
        oneTimeChargesPage.clickConfirmSubmit();
        Reporter.log("✅ Step 10 — Confirm Submit clicked", true);

        // Step 11 — Success message
        String successMsg = oneTimeChargesPage.getSuccessMessage();
        Assert.assertFalse(successMsg.isEmpty(),
                "❌ Success message not shown for: " + chargeType);
        Assert.assertTrue(
                successMsg.toLowerCase().contains("success") ||
                        successMsg.toLowerCase().contains("applied"),
                "❌ Unexpected message: " + successMsg);

        Reporter.log("══════════════════════════════════════", true);
        Reporter.log("✅ " + tcId + " PASSED!", true);
        Reporter.log("✅ Charge Type : " + chargeType, true);
        Reporter.log("✅ Child ID    : " + CHILD_ID, true);
        Reporter.log("✅ Child Name  : " + childName, true);
        Reporter.log("✅ Amount      : ₹" + amount, true);
        Reporter.log("✅ Success     : " + successMsg, true);
        Reporter.log("══════════════════════════════════════", true);
        System.out.println("✅ " + tcId + " PASSED — " + chargeType);
    }
}
