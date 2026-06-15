# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

**Build (compile only):**
```bash
mvn compile
```

**Run all tests (requires a `testng.xml` at project root — Surefire default):**
```bash
mvn test
```

**Run a specific TestNG suite XML directly:**
```bash
mvn test -Dsurefire.suiteXmlFiles=OneTimeChargestestng.xml
mvn test -Dsurefire.suiteXmlFiles=OnlinePaymentReceivedtestng.xml
```

**Run a single test class:**
```bash
mvn test -Dtest=testScripts.SupportTests.OneTimeCharges_Testcases
```

**Run with a different browser (default is `chrome`):**
```bash
mvn test -Dbrowser=firefox -Dsurefire.suiteXmlFiles=OneTimeChargestestng.xml
```

Screenshots on failure land in `screenshots/`. Test results are written to `result/report.xlsx`.

## Architecture

This is a **Selenium + TestNG Page Object Model (POM)** framework targeting the Footprints franchise management web app at `https://test-franchise.footprintseducation.in`.

### Layer overview

```
src/main/java/
  utils/
    IAutoConstant.java      — all constants: URLs, credentials, file paths, timeouts
    BaseTest.java           — TestNG lifecycle (@BeforeSuite/Class/AfterMethod/Suite):
                              browser init, auto-login as Rakesh, policy notification
                              acknowledgment, screenshot-on-failure, pass/fail summary
    FWUtils.java            — low-level Excel read/write via Apache POI
    ExcelDataProvider.java  — TestNG @DataProvider wrappers that load Excel sheets
  pages/
    Navigations.java        — single navigation hub for all top-nav menu → sub-menu clicks
    Onboarding/             — LoginPage
    Support/                — OneTimeChargesPage, OnlinePaymentReceived, AccountStatementPage,
                              Regular_ServiceRequests, Corporate_ServiceRequests, RecentCustomerRequestsPage
    Settings/               — UserRightsPage

src/test/java/
  testScripts/
    OnbardingTests/         — login_Testcases
    SupportTests/           — one file per Support screen
```

### Key design decisions

**BaseTest lifecycle** — Every test class extends `BaseTest`. `@BeforeClass` launches Chrome, navigates to the login URL, logs in with the default user (`Rakesh` / `Dev@123` from `IAutoConstant`), then automatically cycles through and acknowledges any pending policy notification popups (up to 10 bells), then hides the notification dropdown via JS to prevent click interception.

**User switching** — Many screens require switching to a module-specific user before testing. Test classes read `testData/input_UserRights.xlsx` (sheet `UserRights`, columns: UserName | ScreenName | RightTitle) to find which user has rights to a given screen name, then call `UserRightsPage.switchUser(user)` in `@BeforeClass`.

**Navigation** — All menu navigation goes through `Navigations.java`. It re-initialises `PageFactory` before each click to handle `StaleElementReferenceException` caused by full page reloads during user switches.

**Test data** — Login scenarios come from `testData/input_Credential.xlsx` (sheet `LoginValidations`). User rights come from `testData/input_UserRights.xlsx`. Online payment data comes from `testData/payment_not_received.csv`. Test data is loaded via `ExcelDataProvider` or inline `@DataProvider` methods.

**JS workarounds** — Several form fields are `readonly` or blocked by overlays. The page objects use `JavascriptExecutor` to remove `readonly`/`disabled` attributes, fire `input`/`change` events, and force-hide modal backdrops when the app's own close buttons are unreachable.

**Suite XML files** — Each feature area has its own TestNG XML at the project root (e.g. `OneTimeChargestestng.xml`). The `browser` parameter defaults to `chrome` and is passed to `BaseTest.openBrowser()` via `@Parameters`. Maven Surefire looks for `testng.xml` by default; use `-Dsurefire.suiteXmlFiles=` to target a specific suite.
