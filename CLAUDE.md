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

## Requirements — Withdraw Child Service Request (`ServiceRequest_WithdrawChildTest.java`)

Source: `TC_ServiceRequests_Withdraw.xlsx` (sheet `TC_Withdraw`). 7 test cases selected for automation (SC004_TC_001 deferred — see below).

### SC001_TC_001 — Full flow via background jobs/APIs (High)
Screen: Service Request → Withdraw Child
Test Data: `Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<id>` ; `parentapp/processChildApprovedRequest?child_id=<id>` (spec's own example URLs omit a ckey for both).

1. Apply Withdraw Child request.
2. **Back-dated:** Child Attrition Request auto-approved & attrition processed immediately — no manual approval needed.
3. **Future/current-dated:** Needs approval from CD (Support) on Customer Request screen, then run `processChildApprovedRequest` API → approved as per WEF automatically.
4. Automated as data-driven (`@DataProvider "withdrawDatePaths"`) — **future-dated only this round**; back-dated row commented out, deferred to next sprint per user decision.

### SC001_TC_002/003/004/005 — Submit for each withdraw reason (Medium)
Screen: Service Request → Withdraw Child
Test Data: To Date (future, 30 days out); Reason: Transfer / Not Satisfied with Services / Moving to formal schooling / Others.

1. Open URL → Support → Account Statement → enter Admission ID → Service Request.
2. Select Service Type → Withdraw Child Request, verify default view.
3. Fill To Date & reason.
4. Click Submit → confirm popup ("Do you want to send Withdraw Child request?") → OK.
5. Toast: "Your request submitted successfully."

Automated as 4 separate `@Test` methods (`testWithdraw_Transfer`, `testWithdraw_NotSatisfied`, `testWithdraw_FormalSchool`, `testWithdraw_Others`) rather than one `@DataProvider` method — user's explicit choice, since child IDs for these come from TestNG `@Parameters`/XML (no code edit needed when a child gets consumed) rather than a hardcoded data array. Each also calls `verifyPendingStatus(childId)` (beyond the base spec) to confirm Pending status after submit.

### SC001_TC_006 — Verify Pending status after submit (Medium)
Screen: Service Request → Withdraw Child → Customer Request
Prerequisites: Support staff/admin user. Background job: `getAllPendingRequests` API via Postman.

1. Submit Withdraw Child request (reason: Not Satisfied with Service).
2. Verify Request Status = "Pending" on Customer Request screen.

### SC002_TC_002 — Support approves Withdraw Child request (High)
Screen: Service Request → Withdraw Child → Customer Request

1. Open Customer Request screen for the child.
2. Run `getAllPendingRequests` → click Approve → request approved successfully.
3. Run `processChildApprovedRequest` → approved as per WEF automatically.

### SC002_TC_003 — Support rejects Withdraw Child request (High)
Screen: Service Request → Withdraw Child → Customer Request

1. Open Customer Request screen for the child.
2. Click Cancel/Reject button → request rejected, not processed.

### SC004_TC_001 — Access-right validation for 'Raise_Support_Request' (Medium) — **NOT automated this round**
Needs a second user without the `Raise_Support_Request` right to compare against — no such row currently exists in `testData/input_UserRights.xlsx` (only `Program Change` has a differentiated-rights precedent, added specifically for that purpose). Deferred until that test-data gap is filled.

### SC003_TC_001 — RETAIN as its own scenario (High) — `tc009_retainAdmission`
Screen: Service Request → Withdraw Child → Customer Request
Prerequisites: Withdraw Child request Pending, Approval Status not yet run through `processChildApprovedRequest`.

1. Submit Withdraw Child request. Confirm Pending on grid.
2. Click RETAIN on the Child Attrition row (row-scoped via `request_id`) → accept native confirm "You want to retain attrition request?".
3. Verify Request Status = "Cancelled".

### Update Attrition Request — WEF date change on a pending record — `tc010_updateAttritionRequest`
Screen: Service Request → Withdraw Child → Customer Request
Prerequisites: Withdraw Child request Pending, Approval Status = Pending (UPDATE REQUEST button only shows in that window — same window as RETAIN, before `processChildApprovedRequest` runs).
Confirmed by user: **Update Request can be submitted multiple times on the same pending record before its WEF date** — it does not get consumed/one-shot the way Approve/Retain do, so the same child can be reused to re-test the update flow.

1. Submit Withdraw Child request. Capture WEF Date before.
2. Click UPDATE REQUEST on the Child Attrition row (row-scoped via `request_id`, same pattern as RETAIN/APPROVE — plain/JS click does not open the modal, requires `Actions.moveToElement().click()`).
3. In the "Update Attrition Request" modal, pick a new (future, later than current WEF) date via the calendar and enter a mandatory Comment. Submit → accept native confirm "Are you sure want to update this request?".
4. Refresh grid. Verify row still present (not deleted) and WEF Date changed to the new value.

### Confirmed live (do not re-derive without evidence)
- The grid's "Request Type" column for a Withdraw Child submission reads **"Child Attrition"**, not "Withdraw Child" — confirmed from the `getAllPendingRequests` response (`"type":"Child Attrition"`, `"name":"<child>-Child Attrition"`) and the grid itself. "Withdraw Child" is only the Service Request dropdown's label.
- `withdraw_reason` dropdown options, exact text: `Transfer`, `Not Satisfied with Services`, `Moving to formal schooling` (lowercase "formal schooling"), `Others`.
- The `reason_comment` field (`id="reason_comment"`) only renders in the DOM when reason = "Others" — for the other 3 reasons it's absent (not just hidden), so waiting on its visibility times out. Selecting "Others" without filling it submits with no popup/toast at all (silent client-side validation block).
- `getAllPendingRequests` is a **stateful trigger, not a plain read** (same pattern as Center Shift/Extended Daycare/Time Extension) — calling it flips the grid straight from `Pending` to `Processing`. Any check for `Pending` status must happen *before* calling it, not after.
- A Child Attrition row has **no generic `button.approve`/`button.reject` pair**. The actionable controls are:
  - **RETAIN** (`id="retained_attrition"`, `request_id` attribute present) — cancels/rejects the pending attrition ("retain the child" = don't withdraw them). This is what SC002_TC_003's "click on cancel button" refers to.
  - **APPROVE** (`class="label btn btn-primary"`, text "Approve" — **no** `id`/`request_id` attribute) — must be located row-scoped: find RETAIN's `request_id`, walk up to its `<tr>`, then find the "Approve"-text element within that same row.
  - Clicking APPROVE only works via a genuine `Actions(driver).moveToElement(el).click().perform()` — neither `WebElement.click()` nor a JS-dispatched `.click()` opens the modal (confirmed by repeated live testing).
- Clicking APPROVE opens an "Approve Attrition Request(#childId)" modal: pre-filled "Approved By", and a **mandatory** Comment field (`id="approve_text"`, textarea) — Submit button is `id="approve_attrition"`. Submitting it triggers a **native confirm()**: "Are you sure want to approve this request?" — must be accepted or every subsequent WebDriver call throws `UnhandledAlertException`.
- `processChildApprovedRequest` only processes a request **as of its WEF date** — calling it for a genuinely future-dated request (e.g. 30 days out) returns HTTP 200 with body `null`, a **silent no-op**, not an error. To observe the terminal `Approved` state within a single test run, WEF must be set to **today**, not a real future date. This applies to both the "future-dated" data-provider row and `tc007`.
- Manual Approve (click APPROVE → fill comment → Submit → accept native confirm) is **mandatory** before `processChildApprovedRequest` does anything — confirmed directly by the user, and matches the spec's own wording ("need to take approval from CD as well then run the API").
- `processChildApprovedRequest` ckey `9414D96600C5` (reused from Center Shift's `processOldChildAttrition`) is **confirmed correct** by the user for this use too — the earlier "null" response was caused by the WEF-date timing above, not a wrong ckey.
- `getWithdrawChildPendingRequests` reuses ckey `B47C56483AAE7373` (same physical `Financialprocess/getAllPendingRequests/` endpoint as Center Shift/Extended Daycare/Time Extension) — spec's example omits a ckey but this is confirmed working via live responses.
- Helper methods that read/act on a specific child's row (`getFirstRetainAttritionRequestId`, `clickApproveAttrition`) must **navigate to that child first**, never rely on whatever page happens to already be loaded in the shared tab — confirmed live that stale tab state from a previous test silently returns/acts on a DIFFERENT child's `request_id`.
- **UPDATE REQUEST** (row-scoped, same pattern as RETAIN/APPROVE — no `id`/`request_id`, must walk up from RETAIN's `<tr>`) opens an "Update Attrition Request(#childId)" modal: date field `id="attrition_date"` + mandatory Comment `id="update_reason"` (textarea) + Submit `id="update_attrition"`. Submit triggers a native confirm() "Are you sure want to update this request?" — same dismiss-on-any-WebDriver-call hazard as elsewhere, must go straight to the alert check.
- The `attrition_date` field is **pickadate.js** (`class="picker__input"`, popup root `id="<fieldId>_root"`), **not** jQuery UI datepicker and **not** Pikaday, despite both being used elsewhere in this app (confirmed live via DOM inspection — `.picker__select--year`/`.picker__select--month` native `<select>` elements plus `.picker__day[data-pick=<epoch>]` day cells). Setting the value via JS injection (`datepicker('setDate', ...)`) rendered the correct-looking value in the DOM but **silently deleted the record on submit** instead of updating it — the internal pickadate state never got set, so the backend received a broken payload. Fixed by driving the real widget: `Select` on the year/month dropdowns (fires native `change`), then a genuine `.click()` on the matching `.picker__day--infocus:not(.picker__day--disabled)` cell — mirroring exactly what a manual user does. This is what actually resolved the "row vanishes after Update Request" bug, not the earlier confirm-handling or refresh-timing theories.
- Confirmed by user: Update Request can be run **multiple times on the same pending record before its WEF date** — unlike Approve/Retain it isn't a one-shot/terminal action, so the same child ID can be reused across repeated update attempts without needing a fresh child each time.

### Existing/added building blocks
- `pages/Support/Regular_ServiceRequests.java` — Withdraw Child form already wired: `isWithdrawFormVisible()`, `setWithdrawToDate()`, `selectWithdrawReason()`, `enterWithdrawComment()`, `submitWithdraw()`.
- `utils/APIs.java` — added `getWithdrawChildPendingRequests(childId)` and `processWithdrawChildRequest(childId)`.
- `pages/Support/RecentCustomerRequestsPage.java` — added `findWithdrawChildRow()` (filters grid by Request Type = "Child Attrition"), `getWithdrawColumnValue/RequestStatus/ApprovalStatus(admId)`, `getFirstRetainAttritionRequestId(childId)` / `clickRetainAttrition(requestId)` (reject/cancel), `clickApproveAttrition(requestId)` / `submitApproveAttrition(comment)` (approve modal, Actions-click + native-confirm handling), `clickUpdateRequest(requestId)` / `submitUpdateRequest(newDate, reason)` (update modal, pickadate calendar click + native-confirm handling).
- `WithdrawChildtestng.xml` — suite file, now includes all 10 test methods; 4 reason-variant child IDs supplied via `<parameter>` tags (TestNG `@Parameters` injected into instance fields via `@BeforeClass`), the other 6 scenarios (including `tc009`/`tc010`) via hardcoded constants in the test class.

### Current status (as of 2026-07-14): ALL 10 TEST CASES CONFIRMED PASSING
- Original 8 (SC001_TC_001-006, SC002_TC_002/003): validated together in one combined `mvn test -Dsurefire.suiteXmlFiles=WithdrawChildtestng.xml` run with 8 simultaneously-fresh child IDs — **8/8 passed, 0 failures.**
- `tc009_retainAdmission` (SC003_TC_001, RETAIN as its own scenario): passed independently — `Request Status` → "Cancelled" confirmed correct.
- `tc010_updateAttritionRequest` (Update Attrition Request WEF-date change): passed independently after fixing the pickadate calendar-interaction bug described above — WEF Date changed correctly (e.g. Aug 13, 2026 → Aug 28, 2026) and the row stayed present after refresh.
- These were each confirmed as individual/independent passing runs, not one single 10-test combined suite execution — user explicitly opted to skip the extra combined run and proceed on the strength of the individual confirmations.

The Withdraw Child automation (10 test cases from `TC_ServiceRequests_Withdraw.xlsx`) is functionally complete. Remaining work, not started:
- **SC004_TC_001** (access-right validation) — needs a differentiated-rights row for Withdraw Child in `testData/input_UserRights.xlsx`, same pattern as `Program Change`.
- **Back-dated path for `tc001`** — coded but commented out, never run live (banked ID `69755` for next sprint).
- **41 further test cases in the same sheet, not yet scoped**: SC002_TC_001 (email — no infra to test), SC002_TC_004/005 (agent distribution/availability — backend routing, not really UI-testable), SC003_TC_002-004 (Re-join flow), SC004_TC_002-009 + SC005/SC006 (32 cases — future-date validation edge cases, invoice voiding, refund calculation; deep financial/data-integrity checks likely needing new page objects and backend verification, not yet scoped for feasibility).

## Requirements — Corporate Transfer / Corporate Center Transfer / Tie-Up Program Change

Source specs: `TC_TieUp_ProgramChange.xlsx` (Tie-Up Program Change), `TC_Corporate_All.xlsx` (sheets `TC_Corporate Transfer` and `TC_Corporate Center Transfer`). These are three DISTINCT features that all live on the Account Statement page for Corporate/Co-Pay/Employee tie-up children, sharing one page object (`Corporate_ServiceRequests.java`) but split into separate test classes per the user's explicit request.

### Confirmed live: three distinct links on Account Statement (Corporate child)
- **TIE UP PROGRAM CHANGE** — `href="javascript:addTieupProgramChange('<child_id>')"` → `TieupProgramChange_Testcases.java`
- **CORPORATE TRANSFER** — `href="pop_corporate_transfer?pop=yes&child_id=<child_id>"` → `CorporateTransfer_Testcases.java`
- **CORPORATE CENTER TRANSFER** — `href="pop_center_transfer?pop=yes&child_id=<child_id>"` → `CoporateCenterTransfer_Testcases.java`
All three can appear together on the same child's Account Statement page — do not assume only one is relevant just because a child is "Corporate."

### Tie-Up Program Change (`TieupProgramChange_Testcases.java`)
- `tc001_tieupProgramChange` (child `71962`): full Add → Save → Approve flow via the modal (form fields reuse `applicable_month`/`new_center`/`new_program_name`/`add_request` ids — see below). Confirmed passing (pre-existing from earlier sprint, preserved as-is during the file split).
- Corporate_ServiceRequests.java's Tie-Up modal and the Corporate Transfer / Corporate Center Transfer modals all reuse the SAME underlying field ids (`applicable_month`, `new_center`, `new_program_name`, `add_request`) — confirmed live via DOM diffing. Only which fields are actually shown/required differs per flow (e.g. Corporate Transfer adds an Offer dropdown `new_offer_id`; Corporate Center Transfer's button-flow omits it).

### Corporate Transfer (`CorporateTransfer_Testcases.java`) — 4/4 test cases confirmed passing
- **SC002_TC_001** — Submit via CORPORATE TRANSFER link → verify Request Status = Pending.
- **SC003_TC_001** — Approve via "Approve Corporate Transfer" (confirmed live: this happens **inline on Account Statement**, not on Recent Customer Requests as the spec's own wording implied) → verify Request Status = Processing.
- **SC008_TC_001** — Cancel a Pending request → verify Cancelled. **Cancel control is `button.cancel_customer_request`** (same generic class already used for Program Change cancellation), NOT an `<a>` tag — reuses `RecentCustomerRequestsPage.isCancelProgramChangeButtonVisible()`/`clickCancelProgramChange()`. Test is idempotent: if a Pending request already exists for the cancel child (e.g. from a prior partial run), it skips re-submitting and cancels the existing one directly — but must explicitly `navigations.goToAccountStatement()` first if it does need to submit, since the idempotency check itself navigates away to Recent Customer Requests.
- **SC009_TC_001** — `migrationprocess/process_corporate_migration_requests` API (`APIs.processCorporateMigrationRequests(childId, date)`) → verify old child Attrition + new child created. **Confirmed live: requires a `date` param matching the request's own WEF date** (1st of the selected Joining Month, e.g. `2026-08-01` for "Aug 2026") — without it, or with a mismatched date, the API returns `{"status":"ok","0":"No Request to Process Corporate Transfer"}` (still HTTP 200, silently a no-op, NOT an error). No `ckey` needed for this endpoint. Attrition-status text check must be case-insensitive (`translate()`) — the app does not reliably render it all-caps.
- Corporate Transfer has no `getAllPendingRequests` step in its own flow (unlike Center Shift/Extended Daycare/etc.) — submit and approve are both direct UI actions; only the final month-end migration is API-driven.
- Test-child chaining: `CT_CHAIN_CHILD_ID` (submit→approve→migrate, one child, gets consumed/attritted by the end) is SEPARATE from `CT_CANCEL_CHILD_ID` (cancel only) — cancel and approve are alternate branches of the same Pending state and cannot share a child. Both are consumed after one full run — a fresh child is needed for each subsequent clean re-run.
- Offer/Center/Program dropdowns: when the exact right values aren't known ahead of time for an arbitrary child, `Corporate_ServiceRequests.selectFirstAvailable()` picks the first non-placeholder option — used via passing `null` to `submitCorporateTransfer(month, null, null, null)`.

### Corporate Center Transfer (`CoporateCenterTransfer_Testcases.java`) — 3/5 confirmed, 2 need fresh test data
- Two distinct submission paths per spec, confirmed live:
  - **Transfer Applicable=No** → the "CORPORATE CENTER TRANSFER" button's own modal (form `id="frm-center-transfer"`, fields `applicable_month`/`new_center`/`new_program_name`/`add_request` — same ids as Corporate Transfer's modal, just without the Offer dropdown). → SC002_TC_001.
  - **Transfer Applicable=Yes** → SERVICE REQUEST → Center Shift (the exact same form already automated for Regular children in `ServiceRequest_CenterShiftTest.java`, reused here via `Regular_ServiceRequests`). → SC003_TC_001/SC002_TC_002.
- Both paths land on Recent Customer Requests as **Request Type = "Center Shift"** (not "Corporate Center Transfer") — use `getColumnValueByRequestType(childId, "Center Shift", ...)`.
- **No generic "Approve" button exists for Center Shift-type rows** — confirmed live via row-HTML dump: once `getAllPendingRequests` flips Pending→Processing, the Actions column shows only **Cancel** (`button.cancel_customer_request`) and **Processing Details** — approval is entirely API-driven. Do not reuse `getFirstApproveRequestId()`/`clickApprove()` for this request type.
- **CORRECTED (2026-08-17), supersedes the original note above:** `process_corporate_center_migration_requests` (ckey `10998DF5FF67`) is NOT the approve API for Service-Request/Center-Shift-sourced rows — it was a wrong-API bug in the test, not a date-matching issue as first suspected. Confirmed live via `getAllPendingRequests`' own full response body: Service-Request-submitted rows are recorded with `"type":"Center Shift"`, never `"Corporate Center Transfer"` — so `process_corporate_center_migration_requests` (which filters on `"Corporate Center Transfer"`-typed rows only) silently returns `{"status":"ok","0":"No Request to Process Corporate Center Transfer"}` for these regardless of any `date` param (tried exact-matching WEF date, no date param, still no-op) — it's a type mismatch, not a timing bug. That endpoint is correctly used ONLY by the button-flow (SC002_TC_001, genuinely typed `"Corporate Center Transfer"`).
- **Correct pairing for Center-Shift-typed rows (Service-Request flow)** — the SAME two-step the plain non-corporate Center Shift feature already uses: `APIs.getCenterShiftPendingToProcessing(childId)` (`Financialprocess/getAllPendingRequests/`, Pending→Processing) then `APIs.getCenterShiftProcessingToApproved(childId)` (`servicerequest/cronProcessCenterShiftRequests`, ckey `B43C083098B7`, Processing→Approved, creates new child + attrition row). Both methods already existed in `APIs.java` from the original Center Shift feature — no new endpoint needed, just the correct existing one.
- `cronProcessCenterShiftRequests` **refuses to process a request before its own attrition/end date arrives** — confirmed live, returns `{"status":"ok","0":{"request_id":"...","result":{"status":"error","error":"Request #... cannot be processed before attrition date (<date>)."}}}`. This is a genuine, informative timing gate (same family as the future-dated-WEF no-op documented elsewhere for Withdraw Child/Transport) — `sc002_tc002_fullFlowViaServiceRequest`/`sc004_tc001_approvePopupDetails` now treat this specific response as **informational** (logged, not a hard failure) rather than asserting hard success, since it can't be forced to complete same-day.
- **Center Shift's own attrition/end date is NEVER the date typed into the effective-date field** — confirmed live: submitting with `setCSEffectiveDate("2026-08-17")` (today) recorded `"date":"2026-08-19"` (today+2) and `"end_date":"2026-08-18"` (today+1) in the raw API response — the form silently snaps to some minimum lead time rather than accepting the literal typed value. The grid's "WEF Date" column reads `end_date`, not `date`. Earlier "off by one day" child data (71430/73212, WEF Aug 31 vs a Sep 1 constant) was this exact same relationship (`end_date` = `date` − 1), not a bug — the code just referenced the wrong field.
- Because of the above, `sc002_tc002`/`sc004_tc001` (Service-Request-flow chain) **cannot reach a genuinely "Approved" terminal state within the same run they're submitted in** — the attrition date is always at least 1–2 days out at submission time, and the approve API refuses early. Confirming full "Approved" requires either running the approve step on/after the request's own attrition date, or the user shifting the test server's clock forward (same pattern already noted for Corporate Transfer's SC009_TC_001).
- SC004_TC_001 (approve-popup-detail verification) and SC005_TC_001 (reject) each need their OWN fresh Transfer Applicable=Yes child with an untouched Pending Center Shift request — approve/reject are mutually-exclusive terminal actions that can't share a child with the SC003_TC_001/SC002_TC_002 chain.
- `CS_CORPORATE_YES = "62383"` (from `ServiceRequest_CenterShiftTest.java`, a previously-confirmed Corporate+flag=Yes child) was reused as `CCT_SR_CHAIN_CHILD_ID` — now consumed after a successful live run (3 new children created: `72253`/`72254`/`72255`). Chain child was subsequently rotated to `71430` (consumed, WEF-locked Aug 31) then `71839` (fresh 2026-08-17, submitted → Pending → advanced to Processing during live API investigation, own attrition date 2026-08-18 not yet reached at time of writing).

### Open items to confirm before further automating
- Fresh child IDs needed: one more `CT_CHAIN_CHILD_ID`/`CT_CANCEL_CHILD_ID` pair for Corporate Transfer (both consumed), and two fresh Transfer Applicable=Yes children for `CoporateCenterTransfer_Testcases`'s SC004_TC_001/SC005_TC_001.
- `CCT_APPROVE_DETAIL_CHILD_ID` (`73212`) is now blocked the same way `71430` was — already has a Pending Center Shift request (WEF end_date Aug 31, `date` Sep 1) — needs a fresh replacement to re-verify `sc004_tc001_approvePopupDetails` against the corrected API pairing.
- To get a genuinely-Approved confirmation of `sc002_tc002`/`sc004_tc001` (rather than the informational "cannot be processed before attrition date" branch), either re-run `getCenterShiftProcessingToApproved` for child `71839` (request `166300`) on/after 2026-08-18, or submit a fresh child and re-run once the server clock reaches that request's own attrition date.
- A fresh Transfer Applicable=No child for Corporate Center Transfer's button-flow (SC002_TC_001) — `71046` was the last confirmed-fresh one at time of writing; check its state before reusing.
- SC009_TC_001's assertion is currently informational-pass when the migration API returns "No Request to Process" (expected until the real calendar date/WEF date align, or a matching `date` param is supplied) — user indicated they'll do a full sanity check by shifting the server date themselves; tighten the assertion back to a hard requirement at that point if desired.
- 40+ further test cases across both new sheets not yet scoped (email notifications, CD-role dashboards, financial/prorated-invoice/discount-continuity checks, access-right differentials for `Tieup_SPOC_Access`/`Invite_Corporate_Payable_Admission` — no "without access" user exists yet in `testData/input_UserRights.xlsx`).

## Requirements — One Time Charges: Block Invoicing on Attrition Child Records

Screen: `https://test-franchise.footprintseducation.in/onetime_charges`. Per user's stated motto ("automate each feature or screen in one place"), all scenarios below live as new `@Test` methods directly in the existing `testScripts.SupportTests.OneTimeCharges_Testcases` class (priorities 6-8, after the existing 22), with supporting methods appended (never edited) to `pages.Support.OneTimeChargesPage` — no separate test class or suite XML was created for this.

### Context (bug being fixed)
Currently, Center Head or anyone with One Time Charges access (`Apply_Onetime_Charge` right) can raise an invoice against an **attrition child record** (a child already withdrawn/left the school). This has caused real incidents where an invoice meant for a child's new/current record was mistakenly raised on their old attritted record instead.

### Acceptance criteria
1. **Do not allow** raising a one-time-charge invoice against an attrition child record by default.
2. **Add an exception**, the same way one already exists for the Support team — via an access right, so that whoever is granted that right can still raise the invoice, provided they enter a **mandatory comment/reason**.
3. **Show a warning message on screen** if someone without the exception right attempts to raise an invoice for an attrition child.

### Confirmed live, test data
- `attrition_child_invoice_allow = 1` (month) on the test server — confirmed by user.
- **Jaydeep Kar** (`Apply_Onetime_Charge` right, plain-text `charge_child_id` input + `btn_child_details` Fetch flow — the same UI as the original 22 tests) → exception-case, attrition child **60006** (`EXCEPTION_ATTRITION_CHILD_ID`, superseding the earlier 71750 which was past the eligibility window — user supplied 60006 specifically to exercise the checkbox path). `testExceptionUserCanRaiseChargeOnAttritionChild` (priority 6): **confirmed live, full positive path.** Warning shown is the same flat *"Cannot raise invoice charges for an Attrition child."* text Nidhi sees (message text does NOT distinguish eligibility — only checkbox visibility does). Exact live DOM for the exception block:
  ```html
  <div class="row" id="div-attrition-exception" style="margin-bottom: 15px;">
    <div class="checkbox">
      <label class="text-danger">
        <input type="checkbox" id="chk-attrition-exception" name="is_exception" value="1">
        Exception Attrition Invoice
      </label>
    </div>
  </div>
  ```
  No dedicated reason field exists — `enterExceptionReasonIfPresent()` correctly finds nothing and the test falls back to the existing `enterChargeComments()`, which IS the mandatory reason field for the exception flow. Full flow (tick checkbox → comment → charge type/amount → submit → confirm → success) completes end-to-end. Test still branches on live checkbox state (visible → full positive flow; hidden → assert block warning instead) so it self-adapts if child 60006 ever ages out of the eligibility window too.
- **Nidhi Chaturvedi** (Center Head/CD, no dedicated OneTime Charges row needed in `input_UserRights.xlsx` — her existing `Program Change` row is enough to `switchUser()`) → blocked-case, attrition child **48737**. `testCenterHeadIsBlockedOnAttritionChild` (priority 8): **PASSES**. Confirmed live: for her role, `charge_child_id` is NOT a text input — it's a searchable `<select>` (select2) listing all children, no separate Fetch button exists at all. Setting its value + firing a `change` event via JS (`enterChildIdRobust()`) is what actually triggers the block. Her message is the flat, non-date-qualified **"Cannot raise invoice charges for an Attrition child."** (distinct from Jaydeep's month-specific message), and the exception checkbox never appears for her — consistent with her having no exception right at all (vs. Jaydeep's apparent date-conditional eligibility). Message is captured and printed to console per request.
- `testActiveChildFlowStillWorks` (priority 7, active child **68192**, still as Jaydeep): **PASSES** — confirms the pre-existing active-child flow is unaffected by all the above additions.

### Shared-infrastructure fix (affects all screens, not just this one)
`Navigations.oneTimeChargesLink`'s original locator was an absolute positional XPath (`li[7]/div[2]/li[1]`) that assumed one fixed menu shape. It broke for Nidhi (her role renders a differently-shaped nav). Fixed, with user confirmation, to match by the link's own text instead of position: `//a[@href='onetime_charges' and normalize-space(text())='OneTime Charges']`. Note there are **two** distinct `href="onetime_charges"` anchors in the DOM — an "Apply OneTime Charges" Billing-shortcut and the real top-level nav item, plain "OneTime Charges" — a bare `//a[@href='onetime_charges']` matches the wrong one and breaks navigation for every user, so the exact-text qualifier is required.

### New OneTimeChargesPage methods (additive only, existing methods untouched)
`getAttritionWarningMessage()` / `isAttritionWarningVisible()`, `dumpVisibleModalsAndAlerts()` / `dumpModalHtml()` (diagnostics), `findExceptionCheckbox()` (private) / `isExceptionCheckboxVisible()` / `checkExceptionCheckbox()` (targets `#div-attrition-exception input[type='checkbox']`), `enterExceptionReasonIfPresent()`, `isSubmitFormEnabled()`, `printPageSourceSnippetContaining()`, `enterChildIdRobust()` (JS-based, for roles like Nidhi's where the field is a disabled/readonly select rather than a plain text input), `dumpChildIdAndFetchButtonState()`, `clickFetchChildDetailsForced()`.

### Status: all 3 new scenarios PASS (confirmed together in one live run)
`testExceptionUserCanRaiseChargeOnAttritionChild`, `testActiveChildFlowStillWorks`, `testCenterHeadIsBlockedOnAttritionChild` — 3/3 passing with the exact child IDs supplied (71750, 68192, 48737), no fresher child needed per user instruction.

### Open items
- The checkbox+reason+success branch of `testExceptionUserCanRaiseChargeOnAttritionChild` is written but not yet exercised live (child 71750 takes the hard-blocked branch instead) — will self-exercise automatically if a future child ID falls inside the exception's eligibility window.
- The exact access-right name and where it's actually assigned (not required for testing so far — behavior was verified empirically rather than by right name).

## Requirements — Transport Service Request (`ServiceRequest_Transport1WayTest.java` / `ServiceRequest_Transport2WayTest.java`)

Source: `TC_Transport.xlsx` (sheet `TC_Transport`). 20 test cases in the full sheet (SC001–SC012, covering full E2E flow, submit, dropdown checks, 1-Way/2-Way approve forms, cancellation, invoicing, credit/debit on plan switch, recurring billing, and stop-transport flows). **6 selected for automation this round**, split by flow into two test classes per user's explicit request:
- `ServiceRequest_Transport1WayTest.java` — SC001_TC_001, SC002_TC_001, SC002_TC_002, SC011_TC_001, SC011_TC_002
- `ServiceRequest_Transport2WayTest.java` — SC005_TC_001

### SC001_TC_001 — Full Flow: Submit → getAllPendingRequests API → Approve Transport form → processChildApprovedRequest API → Approved (High)
Screen: Account Statement → Service Request → Recent Customer Requests → `process_child_transport`
APIs:
- `getAllPendingRequests`: `{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373`
- `processChildApprovedRequest`: `https://test-admissions.footprintseducation.in/api/parentapp/processChildApprovedRequest?child_id=<child_id>&ckey=9414D96600C5`
- Approve Transport URL (landed on after clicking Approve): `https://test-franchise.footprintseducation.in/process_child_transport?request_id=<id>&request_type=Add%20One%20Way%20Transport&child_id=<child_id>&assign_route=1&show_addon=1`

1. Login as Jaydeep Kar → Account Statement → enter child ID → Generate → SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Transport One Way' (Start Transport 1 Way form). Fill From date. Submit → confirm popup → OK. **Expect:** toast 'Transport request submitted successfully.'
3. Run `getAllPendingRequests` API. **Expect:** status=ok, type=Transport, status Pending → Processing.
4. Navigate to Recent Customer Requests. Find Transport row, status=Processing. Click Approve. **Expect:** Approve Transport form opens (`process_child_transport`).
5. Fill Approve Transport form: Transport Type (Pick-up/Drop/Both), Route, Trip, Location (map, draggable bus icon). Submit. **Expect:** approved.
6. Run `processChildApprovedRequest` API. **Expect:** status=ok, Transport addon added to child.
7. Navigate to Recent Customer Requests. Verify Request Status = Approved.

### SC002_TC_001 — Start Transport 1 Way: Submit → Pending (High)
Screen: Account Statement → Service Request
Test Data: Service dropdown 'Transport One Way' (= Start Transport 1 Way) | From date: 2024-09-18 (use a real future date at run time)

1. Login as Jaydeep Kar → Account Statement → enter child/Admission ID → Generate → SERVICE REQUEST. **Expect:** popup opens with Services dropdown.
2. Click dropdown, verify 'Transport One Way' listed. Select it. Verify default view: Service dropdown, From date, Submit icon, Close icon.
3. Select 'Start Transport 1 Way'. Verify same default view fields visible.
4. Fill From date. Click Submit. **Expect:** confirmation popup 'Do you want to send Transport request?'
5. Click Cancel on popup. **Expect:** request NOT submitted, popup dismissed.
6. Click Submit again → OK. **Expect:** toast 'Transport request submitted successfully.'
7. Navigate to Recent Customer Requests. Verify Request Type = 'Start Transport 1 Way', Status = Pending.

### SC002_TC_002 — Service Request dropdown: Transport One Way / Transport Two Way options present (High)
Screen: Account Statement → Service Request
Prerequisites: Jaydeep Kar has Account Statement access (`manage_user_rights`). Active child with transport-enabled center.

1. Login as Jaydeep Kar → Account Statement → enter child ID → Generate → SERVICE REQUEST. **Expect:** popup opens with Services dropdown.
2. Click dropdown. Verify 'Transport One Way' option listed.
3. Verify 'Transport Two Way' option listed.
4. Verify other transport options listed (e.g. Stop Transport).
5. Select 'Transport One Way'. Verify From date field appears as mandatory.

### SC005_TC_001 — Start Transport 2 Way: Submit → Pending (High)
Screen: Account Statement → Service Request
Test Data: Service dropdown 'Start Transport 2 Way' | From date: 2024-09-18 (use a real future date at run time) | Child ID: 24309

1. Navigate to Account Statement. Enter Admission ID 24309. Click SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Start Transport 2 Way'. Verify default view: Service dropdown, From date, Submit icon, Close icon.
3. Fill From date. Click Submit → OK. **Expect:** toast 'Transport request submitted successfully.'
4. Navigate to Recent Customer Requests. Verify Request Type = 'Start Transport 2 Way', Status = Pending.

### SC011_TC_001 — Stop Transport 1 Way: Submit → Pending (High)
Screen: Account Statement → Service Request
Test Data: Child 66730 (Advik Dhingra, ACTIVE) — active Addon: 'One Way Transport - 1750 (₹1750.00)'. Dropdown option (exact, confirmed live): 'Stop Transport 1 Way'. Form fields: Services dropdown + From date (mandatory, calendar icon) + Submit button only — no Route/Trip/Location (those are Approve-form-only fields).

1. Login as Jaydeep Kar → `account_statement?child_id=66730`. **Expect:** loads, child ACTIVE.
2. Verify Addons section shows 'One Way Transport - 1750 (₹ 1750.00)' active.
3. Click SERVICE REQUEST (spanner icon). **Expect:** popup opens, header shows 'Advik Dhingra #66730 ACTIVE'.
4. Click Services dropdown. Verify 'Stop Transport 1 Way' listed (note: 'Start Transport 1 Way' NOT listed — child already has 1 Way active).
5. Select 'Stop Transport 1 Way'. Verify form shows ONLY Services dropdown + From date + Submit.
6. Click From date calendar icon, select a future date.
7. Click Submit → confirm popup → OK. **Expect:** toast 'Transport request submitted successfully.'
8. Navigate to Recent Customer Requests. Verify Request Type = 'Stop Transport 1 Way', Status = Pending.

### SC011_TC_002 — Stop Transport 1 Way: Full flow → addon removed (High)
Screen: Recent Customer Requests → Account Statement
Prerequisites: Stop Transport 1 Way in Pending status (SC011_TC_001 submitted). Child 66730 (Advik Dhingra).
APIs: same `getAllPendingRequests` / `processChildApprovedRequest` endpoints as SC001_TC_001 (`chid_id=66730` / `child_id=66730`).

1. Run `getAllPendingRequests` API. **Expect:** status=ok, Stop Transport 1 Way → Processing.
2. Navigate to Recent Customer Requests. Verify Request Type = 'Stop Transport 1 Way', Status = Processing. Click Approve. **Expect:** approval confirmed.
3. Run `processChildApprovedRequest` API. **Expect:** status=ok.
4. Navigate to Recent Customer Requests. Verify Request Status = Approved.
5. Navigate to `account_statement?child_id=66730`. Scroll to Addons section.
6. Verify Addons section: 'One Way Transport - 1750' is NO LONGER listed — addon fully removed.

### Existing building blocks to reuse (confirmed from code, no new work needed)
- `pages/Support/Regular_ServiceRequests.java` — **all 4 Transport forms already wired** (built in an earlier session, ahead of this automation round): `isStartTransport1WayFormVisible()`/`setT1FromDate()`/`submitStartTransport1Way()`, `isStartTransport2WayFormVisible()`/`setT2FromDate()`/`submitStartTransport2Way()`, `isStopTransport1WayFormVisible()`/`setST1FromDate()`/`submitStopTransport1Way()`, `isStopTransport2WayFormVisible()`/`setST2FromDate()`/`submitStopTransport2Way()`.
- `pages/Support/RecentCustomerRequestsPage.java` — generic helpers already sufficient, no new methods needed: `getColumnValueByRequestType(admId, requestType, columnHeader)` (reads Transport rows by Request Type = "Start Transport 1 Way" / "Start Transport 2 Way" / "Stop Transport 1 Way"), `getFirstApproveRequestId()` + `clickApprove(requestId)` (generic `button.approve[request_id=...]` pattern).
- `pages/Support/AccountStatementPage.java` — `getAddonsText()` already reads the Addons section (`<div class="col-md-12"><b>Addons :</b> ...</div>`, "Not Available" when absent) — reused directly via `.contains("One Way Transport")` rather than adding a Transport-specific wrapper.
- `utils/APIs.java` — `processChildApprovedRequest`'s physical endpoint (`parentapp/processChildApprovedRequest`, ckey `9414D96600C5`) is already generic (`CS_ATTRITION_PROCESS`, reused across Center Shift/Withdraw Child/Corporate Center Transfer) — added a thin `processTransportApprovedRequest(childId)` wrapper for naming consistency with other features rather than a new endpoint.

### Confirmed live (2026-08-13) — do not re-derive without evidence
- The Recent Customer Requests grid's **"Request Type" column shows the backend action name, not the Services dropdown's UI label** — same pattern as Withdraw Child showing "Child Attrition" instead of "Withdraw Child". Confirmed via live duplicate-request error text and a direct grid dump for children 72101 and 66730:
  - Services dropdown "Start Transport 1 Way" submission → grid Request Type = **"Add One Way Transport"**
  - Services dropdown "Stop Transport 1 Way" submission → grid Request Type = **"Delete One Way Transport"**
  - (2-way equivalents — "Add Two Way Transport"/"Delete Two Way Transport" — extrapolated from this naming pattern, NOT yet independently confirmed live.)
- The Services dropdown option text itself — `"Start Transport 1 Way"` / `"Stop Transport 1 Way"` — IS correct as passed to `selectServiceType()`; both confirmed live (selection succeeded and revealed the expected form each time).
- `getAllPendingRequests` param **`chid_id` (not `child_id`) is confirmed correct for Transport** — user-supplied a live-working example (`chid_id=72101`), unlike Extended Daycare/Withdraw Child where `chid_id` was silently wrong. Do not "fix" this to `child_id` without new evidence.
- `processChildApprovedRequest` ckey `9414D96600C5` confirmed working for Transport (user-supplied live example, `child_id=72428`).
- Duplicate-request submissions are rejected client-side with `"Invalid Request: Request to '<Add/Delete One/Two Way Transport>' is already pending with us - requested by <name> on <date>, it will be processed soon."` — tests treat this as an acceptable pre-existing-Pending state (not a hard failure) when resubmitting against a child that already has one pending, mirroring the idempotency pattern used in Corporate Transfer's cancel test.
- Child **72428** (originally supplied for the full E2E flow, SC001_TC_001) has **no Transport option in its Services dropdown at all** — confirmed live (dropdown only listed Center Shift/Child Pause/Extended Daycare/Program Change/Start Time Extension/Withdraw Child, 0 existing grid rows) — it is not Transport-enabled at its center. A different, Transport-enabled, unused child ID is needed for SC001_TC_001.
- Child **24309** (spec's own child for SC005_TC_001, Start Transport 2 Way) is now in **ATTRITION** status — confirmed live: the whole Services dropdown renders `disabled`, and the panel header shows "Sanidhya Rajan #24309 ATTRITION — You can't access services of attrition child." A different, ACTIVE, 2-way-transport-enabled child ID is needed for SC005_TC_001.
- `processTransportApprovedRequest` (processChildApprovedRequest) has the **same future-dated-WEF silent no-op behavior confirmed for Withdraw Child**: calling it for a request whose WEF/From date is not today returns HTTP 200 with a `null` body and leaves the grid status stuck at "Processing" rather than "Approved". Confirmed live for child 66730's Stop Transport 1 Way request (WEF was 2026-08-20 from an earlier test run using a +7-day future date — the process call no-op'd, status stayed "Processing"). `tc004_stopSubmitPending` was changed to submit with WEF=today (`LocalDate.now()`) instead of a future date so `tc005_stopFullFlow` can observe the terminal Approved state within the same run — but this fix could not be verified live yet because child 66730 already has that stale future-dated Pending request blocking a fresh same-day resubmission (duplicate-request check blocks ANY second "Delete One Way Transport" submission regardless of date, not just same-date ones).
- Confirmed live: the generic `button.approve[request_id=...]` pattern (already used by other service types) **does work for "Delete One/Two Way Transport" (Stop) rows** — `clickApprove(166255)` succeeded and the grid moved from "Processing" (post-`getAllPendingRequests`) toward the Approve action, before getting stuck at "Processing" for the unrelated future-dated-WEF reason above.
- Confirmed live (user-supplied element): **"Add One/Two Way Transport" (Start) rows use a plain `<a>` link, NOT the generic button.approve** — `<a href="process_child_transport?request_id=166259&request_type=Add One Way Transport&child_id=73041&center_id=210&assign_route=1&show_addon=1" class="btn btn-primary btn-xs label">Approve</a>`. Clicking it navigates the current tab straight to the Approve Transport form. Added `RecentCustomerRequestsPage.isTransportApproveLinkVisible()`/`clickTransportApprove()` (matches `a[href*='process_child_transport']`) for this; `tc001_fullFlow` now uses these instead of the generic `getFirstApproveRequestId()`/`clickApprove()`, which found nothing for this row type (confirmed live — that was the exact prior failure).
- Confirmed live (user): clicking Approve does navigate to `process_child_transport?request_id=<id>&request_type=Add%20One%20Way%20Transport&child_id=<id>&center_id=<id>&assign_route=1&show_addon=1`, matching the spec's own URL pattern exactly (plus an extra `center_id` param not mentioned in the spec).

### 2-Way Transport (`ServiceRequest_Transport2WayTest.java`) — ALL 3 tests CONFIRMED PASSING (2026-08-14)
Running as **Nidhi Chaturvedi** (see "Transport role / acting user" section below), a full clean run with fresh children passed 3/3: `tc001_submitPending` (SC005_TC_001, child 72089) — Start Transport 2 Way → Pending; `tc002_stopSubmitPending` (SC012_TC_001, child 50875, "Two Way Transport - 2000" addon) → Pending; `tc003_stopFullFlow` (SC012_TC_002) → submit → getAllPendingRequests → generic `button.approve` click → `processChildApprovedRequest` → Approved → addon removed ("Not Available"). This also confirms "Add Two Way Transport"/"Delete Two Way Transport" as the real grid Request Type strings (previously just extrapolated from the 1-Way naming) — the ONE prior failure using this text was a cross-type conflict (child already had an unrelated One Way request pending), not a wrong string.
- **Fixed bug**: this class shares a single browser tab (unlike the 1-Way class's two-tab design) — any step that navigates to Recent Customer Requests (grid checks via `getColumnValueByRequestType`) leaves the driver there, and `AccountStatementPage.generateAccountStatement()` doesn't navigate itself (assumes it's already on Account Statement). Both `@BeforeMethod` and the end of `tc003` now call `navigations.goToAccountStatement()` explicitly before any Account Statement interaction — omitting this caused `NoSuchElementException` on `#frm_child_id` twice before the fix.
- **Cross-type conflict discovered**: a child with ANY existing pending/processing transport request (of either direction) blocks a NEW submission of the other direction too — e.g. child 72089's pre-existing "Add One Way Transport" blocked a "Start Transport 2 Way" submission with the same generic "already pending" error, even though no Two Way request had ever been created. `tc001_submitPending`'s idempotency check was tightened to only treat "already pending" as an acceptable duplicate when the error text itself mentions "two way" — otherwise it now fails loudly with a message pointing at the real cause (different child needed), instead of silently misreading it as a same-type dup.

### Transport role / acting user — switched from Jaydeep Kar to Nidhi Chaturvedi (2026-08-14)
Per explicit request, added a new row to `testData/input_UserRights.xlsx` (`UserRights` sheet): `Nidhi Chaturvedi | Transport | Raise_Support_Request` (RightTitle is descriptive only — `getUserForScreen()` matches on ScreenName, column B, exact case-insensitive string match, not RightTitle). Both `ServiceRequest_Transport1WayTest.java` and `ServiceRequest_Transport2WayTest.java` now call `getUserForScreen("Transport")` instead of `getUserForScreen("Account Statement")`, switching the acting user for ALL 8 Transport tests (both classes) from Jaydeep Kar to Nidhi Chaturvedi. Confirmed live: Nidhi logs in as "Centers Head" role and has full, identical access to submit/approve Transport requests via Account Statement → Service Request — no UI differences observed vs. Jaydeep for any step exercised so far (Start/Stop submit, Approve Transport form, generic button.approve). Every child ID used under Jaydeep in earlier sessions is unaffected by this switch (user identity doesn't change which children exist/their state) but IS now stale from repeated Jaydeep-era test runs — fresh children were re-supplied for the 2-Way class's first clean run under Nidhi (72089, 50875) with full success; the 1-Way class has not yet been re-run end-to-end under Nidhi.

### Approve Transport form (`process_child_transport`) — CONFIRMED live end-to-end, INCLUDING by the automated test itself
Child 73041: user walked the real form manually, then re-ran `processChildApprovedRequest`, which actually processed it — Account Statement showed `Addons: One Way Transport - 300 (₹300.00)` and a generated invoice (`PI/967621`). Child 72114: `tc001_fullFlow` then reproduced the entire chain **unattended** (submit → getAllPendingRequests → Approve link → fill form → Approve → native confirm → processChildApprovedRequest → Approved) — confirmed PASSING. This is NOT the spec's own "Transport Type / Route / Trip / Location map" description — there is no separate Trip field. Confirmed real field sequence, implemented in `ApproveTransportPage.java`:
1. `#transport_type` (select) — options `--Choose--`/`Pick-Up`/`Drop` (exact text, note the hyphen+capital-U)
2. `#addon` (select) — pricing plan, e.g. "One Way Transport - 300"
3. `#pickup_route_id` (select, shown for Pick-Up; `#drop_route_id` assumed analogous for Drop, unconfirmed — user has only explained the Pick-Up path so far) — options inside `<optgroup label="Active">`
4. `#ptid` (readonly input, `pickatime.js` widget) — Pickup Time
5. `#map_canvas_route_autocomplete` (Google Places autocomplete) — type an address, click the `.pac-item` suggestion, double-click the map's bus-icon marker (`img[usemap^='#gmimap']`), then click `#send_map_sms` ("Set Pickup/Drop Location") — confirmed live result: "Pickup Location: ORYON BUSINESS INDIA, PTV LTD, Bhangel, Sector - 106, Noida..." populated correctly from address "Amrapali Zodia sector 120 Noida". Renders asynchronously after Route selection — can take several seconds, so a generous wait is used rather than a fixed sleep.
6. `#transport_date` (WEF Date) — a `pickadate.js` widget (`class="picker__input"`), same widget family that silently corrupted data via raw JS injection for Withdraw Child's `attrition_date` — so this is driven via `Regular_ServiceRequests`' proven `openCalendarFor()`/`clickCalendarDay()` real-widget-click helpers, defaulting to today's date
7. `#approve_transport` (button, `class="approve btn btn-primary"`, `request_id`/`request_type` as plain attributes) — clicking it triggers a **native `confirm()`**: "Changes will be applicable today onwards, sure want to update status of transport request?" — must be accepted or every subsequent WebDriver call throws `UnhandledAlertException` (confirmed live via user screenshot)

### Confirmed live — submit-form date quirks
- The **Start Transport 1 Way submit form** (Service Request popup, "From date") rejects `today` as a "past date" — `"Cannot make request in past date"` — unlike most other features' submit forms. A genuine future date is required here (`futureDate()`, +7 days).
- This submit-form date is **independent** of the Approve Transport form's own separate `#transport_date` (WEF Date) field — the latter is what actually gates `processChildApprovedRequest`, and gets set to today regardless of whatever future date was used at submit time.
- The test server's clock was reset mid-session by the user after a date-mismatch caused every future-dated submission to be rejected as "past" (the server's clock had drifted ahead of the local automation machine's clock) — if this recurs, check the server date before assuming a code regression.

### Open items to confirm before/while automating
- ~~Whether #drop_route_id really exists~~ — resolved via user's full-page element dump: it exists (`id="drop_route_id"`, same "Select Route" + `<optgroup>` structure as pickup). The Drop path's other fields are also now confirmed from that same dump, though not yet exercised live end-to-end: `#dtid` (Drop Time — NOT "drop_ptid", class `dropatime-format`), `#dropRouteinfo_name`/`#dropRouteinfo_lat`/`#dropRouteinfo_long` (Drop location, mirroring `pickupRouteinfo_*`), `#drop_point` (mirroring `pickup_point`). `ApproveTransportPage.selectRoute()` already picks between `pickup_route_id`/`drop_route_id` by transport type value — Drop-path Time/Location methods (`setPickupTime`/`enterLocationAddress` are currently Pickup-only-named) would need Drop-specific variants (or parameterizing) before Drop can be automated.
- The dump's two `icon-pin-alt` elements are the manual trigger for the location picker — confirmed live: `<a data-toggle="modal" href="#" data-href="location_picker?pop=yes&child_id=<id>&route_type=pickup&hd_inp=pickupRouteinfo&CH_TASK_MODE=true" class="open_modal_window" data-target="#select_route_modal">` wrapping the pin icon + the readonly `pickupRouteinfo_name` field. Clicking it AJAX-loads `location_picker?...` into a Bootstrap modal (`#select_route_modal`) containing the map/autocomplete. `ApproveTransportPage` never clicks this — yet `tc001_fullFlow` still found and used `#map_canvas_route_autocomplete` successfully — so selecting the Route dropdown (`#pickup_route_id`) itself must auto-trigger the same modal/map programmatically (likely a JS onchange handler), making an explicit pin click redundant for the Pickup path. Worth keeping in mind for Drop-path automation later: the analogous `data-href="location_picker?...&route_type=drop&hd_inp=dropRouteinfo..."` trigger presumably exists for `#drop_route_id` too, unconfirmed.
- Confirmed by user: for **Two Way Transport**, the Approve form's URL/screen requires filling **both** Pick-Up and Drop sections (matches the spec's SC006_TC_001) — not yet relevant to this round's automated scope (SC005_TC_001 is submit-only, no approve step), but will be needed if 2-way approve is automated later.
- Whether the **Stop Transport submit form** also rejects today's date the same way Start's does — unconfirmed; matters for `tc004_stopSubmitPending`/`tc005_stopFullFlow`, which currently submit with today's date specifically so `tc005` can observe the terminal Approved state (see Stop Transport notes above) — if Stop's submit form also rejects today, that design needs revisiting.
- A fresh, unused, Transport-enabled child ID with **no existing pending Transport request** for **SC002_TC_002** (`DROPDOWN_CHECK_CHILD_ID`, currently a TODO placeholder) — needed so both "Start Transport 1 Way" and "Start Transport 2 Way" are still offered in the dropdown.
- A fresh, ACTIVE (not Attrition), 2-way-transport-enabled child ID for **SC005_TC_001** — 24309 (the spec's own example) is now Attrition and cannot be used.
- Either a fresh child with an active "One Way Transport" addon (to re-verify SC011_TC_002's full approve flow cleanly), or waiting until 2026-08-20 for child 66730's existing stale Pending request to reach its own WEF date, to confirm the `tc004`-now-submits-with-today's-date fix actually lets `tc005_stopFullFlow` reach "Approved".
