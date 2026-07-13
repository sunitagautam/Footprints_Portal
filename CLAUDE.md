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

## Requirements — Extended Daycare Service Request (`ServiceRequest_ExtendedDaycareTest.java`)

Source: `TC_Extended_Daycare_Final.xlsx` (sheet `TC_Extended Daycare`). 10 test cases selected for automation.

### SC002_TC_001 — Happy Path: Submit → Pending (Critical)
Screen: `recent_update_details?child_id=<child_id>`
Prerequisites: Active Regular child. Login as Support staff. No pending Extended Daycare request.
Test Data: Start Date: 2026-07-02 | End Date: 2026-07-30 | Admission ID: 68671

1. Login as Support → Account Statement → enter Admission ID → click SERVICE REQUEST. **Expect:** Service Request popup opens.
2. Select 'Extended Daycare'. Enter Start Date = 2026-07-02, End Date = 2026-07-30. Submit → OK. **Expect:** Toast 'Extended Daycare request submitted successfully'.
3. Navigate to Customer Request screen. **Expect:** screen loads.
4. Verify Request Type = 'Extended Daycare', Status = 'Pending', Approval Status = 'NA'.
5. Verify Center Name = child's center, WEF Date = Start Date, End Date = End Date selected.
6. Verify Created By = logged-in user name.
7. Verify CANCEL button (red) visible at Pending status.

### SC002_TC_002 — Happy Path: Pending → Approved via getAllPendingRequests API (Critical)
Screen: `recent_update_details?child_id=<child_id>`
Prerequisites: Extended Daycare request in Pending status.
API: `{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373`
Note: unlike Center Shift, ED goes directly Pending → Approved (no Processing step).

1. Confirm status = Pending on Customer Request screen.
2. Run `getAllPendingRequests` API. **Expect:** response status = ok.
3. Refresh Customer Request screen. **Expect:** Status = Approved.
4. Verify Approval Status = NA (stays NA even after Approved — unlike Center Shift).
5. Verify Actions column is EMPTY after Approved (no CANCEL / PROCESSING DETAILS).
6. Verify Support Executive column populated after approval.

### SC002_TC_003 — Happy Path: Child History "Extended Daycare Started" entry (Critical)
Screen: Support → Account Statement → Child History
Prerequisites: Extended Daycare request approved.

1. Navigate to Account Statement → CHILD HISTORY icon. **Expect:** Child Updates History popup opens.
2. Verify entry: 'Extended Daycare - Extended Daycare Started'.
3. Verify timestamp = approval time (when getAllPendingRequests API ran).
4. Verify child status remains ACTIVE and program unchanged.

### SC002_TC_004 — Happy Path: extendedDaycareCronJob on End Date → Completed (Critical)
Screen: Support → Account Statement → Child History
Prerequisites: Extended Daycare Approved. End Date = today.
API: `{{Base_URL}}parentapp/extendedDaycareCronJob?ckey=7A533862C14E`
Expected response: `status=ok`, message='Extended Daycare duration Completed', Total 1 Records Updated

1. On End Date, run cron API. **Expect:** API executes.
2. Verify response: status=ok, message contains 'Extended Daycare duration Completed', Total 1 Records Updated.
3. Verify response includes Child ID, Child Name, Start Date, End Date, Status=Completed.
4. Navigate to Child History. Verify entry: 'Extended Daycare - Extended Daycare duration Completed'.
5. Verify timestamp of Completed entry = End Date cron run time.
6. Verify child remains on ORIGINAL program (ED is an additional service, not a program change).
7. Verify child status still = ACTIVE after completion.

### SC003_TC_001 — Pricing: Per-day charge = round(6.67% × half-day fee) (Critical)
Screen: Support → Account Statement
Prerequisites: Child with half-day fee = Rs.11,999. Extended Daycare approved for 1 day.
Expected per-day charge: round(0.0667 × 11,999) = Rs.800

1. Submit Extended Daycare for 1 day (Start Date = End Date). Run Approval API. **Expect:** approved.
2. Navigate to Account Statement → find Extended Daycare invoice.
3. Verify Daycare Fee = Rs.800.
4. Verify calculation: 0.0667 × 11,999 = 799.93 → rounded = Rs.800.

### SC003_TC_002 — Pricing: Invoice line items (Critical)
Screen: Support → Account Statement
Prerequisites: Extended Daycare approved. Invoice visible.
Expected: Daycare Fee + Preschool Fee + SGST 9% + CGST 9% + Roundoff, Total: Rs.23,200. GST applies to Daycare Fee component only.

1. Navigate to Account Statement → find Extended Daycare invoice (PI/XXXXXX). **Expect:** invoice visible with all line items.
2. Verify line item: Daycare Fee | Booking Head = 'Extended Daycare Charges'.
3. Verify line item: Preschool Fee | Booking Comment = 'Extended Daycare Charges'.
4. Verify SGST = 9% of Daycare Fee component (e.g. Rs.4,522.03 × 9% = Rs.406.99).
5. Verify CGST = 9% of Daycare Fee component (same as SGST).
6. Verify Roundoff line item present (e.g. -0.01).
7. Verify invoice total = Daycare Fee + Preschool Fee + SGST + CGST + Roundoff.
8. Verify invoice raised immediately on approval (not on End Date).
9. Verify invoice due date = Start Date + 4 days (observed: due date 06-Jul for 02-Jul invoice).

### SC003_TC_003 — Pricing: credit_debit_amount matches invoice total (High)
Screen: Support → Account Statement
Expected: API response credit_debit_amount = 23200 matches Invoice total Rs.23,200.

1. Submit Extended Daycare. Note credit_debit_amount from getAllPendingRequests API response.
2. Navigate to Account Statement. Note Extended Daycare invoice total.
3. Verify credit_debit_amount (23200) matches invoice total (Rs.23,200).

### SC008_TC_001 — Negative: Inactive child cannot submit Extended Daycare (High)
Screen: Support → Account Statement → Service Request → Extended Daycare
Prerequisites: Inactive or Attrition child.

1. Open Account Statement for Inactive/Attrition child → SERVICE REQUEST. **Expect:** popup opens.
2. Verify Extended Daycare not available in dropdown OR blocked if selected.

### SC008_TC_002 — Negative: Duplicate Extended Daycare blocked (High)
Screen: Support → Account Statement → Service Request → Extended Daycare
Prerequisites: Extended Daycare already Pending/Approved for child.

1. Submit Extended Daycare. Confirm status = Pending.
2. Attempt to submit another Extended Daycare for same child with overlapping dates. **Expect:** system blocks.
3. Verify error: 'Extended Daycare request already exists for this period.'
4. Verify no duplicate record on Customer Request screen.

### SC008_TC_003 — Negative: extendedDaycareCronJob before End Date → no premature completion (Medium)
Screen: Support → Account Statement → Child History
Prerequisites: Extended Daycare Approved. End Date not yet reached.
API: `{{Base_URL}}parentapp/extendedDaycareCronJob?ckey=7A533862C14E`

1. Confirm End Date is in future.
2. Run cron API before End Date. **Expect:** cron runs.
3. Verify request NOT marked Completed (End Date not reached).
4. Verify no 'Completed' history entry in Child History.
5. On actual End Date, run cron. Verify Completed correctly.

### Existing building blocks to reuse
- `pages/Support/Regular_ServiceRequests.java` — Extended Daycare form already wired: `isExtendedDaycareFormVisible()`, `setEDFromDate()`, `setEDToDate()`, `submitExtendedDaycare()`.
- `utils/APIs.java` — unused constants `ED_APPROVE_REQUEST` (= Center Shift's `getAllPendingRequests`, ckey `B47C56483AAE7373`) and `ED_CRON_JOB` (`parentapp/extendedDaycareCronJob`, ckey `7A533862C14E`). Need methods built on them (no `getExtendedDaycare...` methods exist yet).
- `pages/Support/AccountStatementPage.java` — has `isExtendedDaycareInvoiceVisible()`, `getExtendedDaycareInvoiceLineItems()`, `getExtendedDaycareInvoiceTotal()` for invoice checks (SC003_TC_002/003).
- Reference pattern to follow: `ServiceRequest_CenterShiftTest.java` + `CenterShifttestng.xml`.

### Open items to confirm before automating
- Concrete child IDs for: inactive/attrition child (SC008_TC_001), child with an existing pending ED request (SC008_TC_002), child with half-day fee = Rs.11,999 (SC003_TC_001/002/003).
- UI selectors on the Customer Request screen for Request Type, Status, Approval Status, Center Name, WEF Date, Created By, Support Executive, Actions — reuse/extend `RecentCustomerRequestsPage.java`.

## Requirements — Time Extension Service Request (`ServiceRequest_TimeExtensionTest.java`)

Source: `TC_Time_Extension_Updated.xlsx` (sheet `TC_Time Extension`). 6 test cases selected for automation.

### SC002_TC_001 — Start Time Extension: Submit request via Service Request form (High)
Screen: Support → Account Statement → Service Request → Start/Stop Time Extension
Prerequisites: Active Regular child. Time Extension enabled at center.
Test Data: Admission ID: 46085 | Service Type: Start Time Extension
Note: Parents can extend services by 90 min — drop off 8:15 AM, pick up by 7:15 PM.

1. Login as Support → Account Statement → enter Admission ID → click SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Start Time Extension' from Services dropdown. **Expect:** form loads.
3. Enter WEF date. Click Submit. **Expect:** confirmation popup 'Do you want to send request for time extension?' with OK & Cancel.
4. Click Cancel on popup. **Expect:** request NOT submitted, form stays open.
5. Click Submit again → OK. **Expect:** toast 'Your request submitted successfully.'
6. Navigate to Customer Request screen. Verify Request Type = 'Start Time Extension', Status = Pending.

### SC002_TC_002 — Start Time Extension: Full flow Submit → API → Approve → Process API (High)
Screen: `recent_update_details?child_id=<child_id>`
Prerequisites: Active Regular child. Admission ID: 70800.
APIs:
- `{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373` (**Note:** per the Extended Daycare finding, the real param is `child_id`, not `chid_id` — verify which one actually scopes correctly here too before trusting results.)
- `http://test-admissions.footprintseducation.in/api/childservices/processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D`

1. Submit Start Time Extension request. **Expect:** toast success, Status = Pending.
2. Run `getAllPendingRequests` API. **Expect:** status=ok, type='Start Time Extension', status='Pending', admission_id correct, credit_debit_amount='0', parent_name='Support Request', current_status='Active', admission_type='Regular'.
3. Navigate to Customer Request screen. Verify Approve button displayed.
4. Click Approve button → confirm. **Expect:** request approved on Customer Request screen.
5. Run `processTimeExtentionRequest` API. **Expect:** `{"status":"ok","message":"Time Extension request processed"}`.
6. Navigate to Customer Request screen. Verify Status = Approved.
7. Navigate to Account Statement. Verify Addons section shows 'Time Extension'.
8. Verify prorated invoice generated: 'Prorated Time Extension Charges - <Month>, <Year> (<N> days)'.

### SC002_TC_010 — Start Time Extension: processTimeExtentionRequest API response (Critical)
Screen: API / Postman
Prerequisites: Start Time Extension approved on Customer Request screen.
API: `http://test-admissions.footprintseducation.in/api/childservices/processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D`
Expected: `{"status":"ok","message":"Time Extension request processed"}`

1. Approve Start Time Extension on Customer Request screen.
2. Run `processTimeExtentionRequest` API.
3. Verify response status = 'ok'.
4. Verify response message = 'Time Extension request processed'.
5. Navigate to Customer Request screen. Verify Status = Approved.
6. Navigate to Account Statement. Verify Addons: Time Extension (₹1,500.00) shown.

### SC003_TC_001 — Stop Time Extension: Submit request via Service Request form (High)
Screen: Support → Account Statement → Service Request → Start/Stop Time Extension
Prerequisites: Active child with Time Extension active.
Test Data: Admission ID: 46085 | Service Type: Stop Time Extension

1. Login as Support → Account Statement → enter Admission ID → click SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Stop Time Extension' from Services dropdown. **Expect:** form loads.
3. Enter WEF date. Click Submit. **Expect:** confirmation popup 'Do you want to send request for time extension?'
4. Click OK on popup. **Expect:** toast 'Your request submitted successfully.'
5. Navigate to Customer Request screen. Verify Request Type = 'Stop Time Extension', Status = Pending.

### SC003_TC_003 — Stop Time Extension: getAllPendingRequests API response verified (High)
Screen: API / Postman
Prerequisites: Stop Time Extension submitted.
API: `{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373`
Expected: type='Stop Time Extension', date=WEF date, end_date=same as date, credit_debit_amount='0', status='Pending', parent_name='Support Request', admission_type='Regular'

1. Submit Stop Time Extension request. Note WEF date selected.
2. Run `getAllPendingRequests` API.
3. Verify response status=ok, type='Stop Time Extension'.
4. Verify date = WEF date selected, end_date = same as date.
5. Verify credit_debit_amount = '0' (no immediate charge on stop request).
6. Verify status = 'Pending', parent_name = 'Support Request'.

### SC003_TC_005 — Stop Time Extension: processTimeExtentionRequest API response (Critical)
Screen: API / Postman
Prerequisites: Stop Time Extension approved on Customer Request screen.
API: `http://test-admissions.footprintseducation.in/api/childservices/processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D`
Expected: `{"status":"ok","message":"Time Extension request processed"}`

1. Approve Stop Time Extension on Customer Request screen.
2. Run `processTimeExtentionRequest` API.
3. Verify response status = 'ok'.
4. Verify response message = 'Time Extension request processed'.
5. Navigate to Account Statement. Verify Time Extension addon REMOVED.

### Existing building blocks to reuse
- `pages/Support/Regular_ServiceRequests.java` — Start/Stop Time Extension forms already wired: `isStartTimeExtensionFormVisible()`, `setSTEFromDate()`, `setSTEToDate()`, `submitStartTimeExtension()`, `isStopTimeExtensionFormVisible()`, `setSTPFromDate()`, `submitStopTimeExtension()`.
- `utils/APIs.java` — `getExtendedDaycarePendingToApproved(childId)` hits the same physical `getAllPendingRequests` endpoint with the same key/ckey — need an equivalent Time Extension method (or a shared generic one) plus a new method for `processTimeExtentionRequest` (different ckey: `3E529969372D`).
- `pages/Support/RecentCustomerRequestsPage.java` — has the generic grid-reading pattern (`getEDColumnValue`-style, filtering by Request Type) to mirror for Time Extension rows; also has `clickApprove(requestId)` already for the Approve button.
- `pages/Support/AccountStatementPage.java` — will need an Addons-section reader (not yet built) to verify 'Time Extension' addon appears/disappears, distinct from the existing Extended Daycare invoice-line-item reader.

### Open items to confirm before automating
- Confirm whether `getAllPendingRequests` actually needs `chid_id` or `child_id` for Time Extension (the Extended Daycare investigation found `child_id` was the real, correctly-scoping param — `chid_id` was silently ignored).
- Concrete child IDs: Admission ID 46085 (submit tests) and 70800 (full-flow test) are given in the sheet — need to confirm these are currently in the right state (Active, Time Extension enabled at center, no conflicting pending request) before each run.
- UI locator for the Addons section on Account Statement (to verify 'Time Extension' addon add/remove) — not yet explored.
- Whether Time Extension is enabled at the relevant center for the given admission IDs (SC001_TC_001's config step is a prerequisite, not in our automated scope).
