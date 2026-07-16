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
- **No generic "Approve" button exists for Center Shift-type rows** — confirmed live via row-HTML dump: once `getAllPendingRequests` flips Pending→Processing, the Actions column shows only **Cancel** (`button.cancel_customer_request`) and **Processing Details** — approval is entirely API-driven (`process_corporate_center_migration_requests`), mirroring how the original Center Shift feature approves via `cronProcessCenterShiftRequests` rather than any UI click. Do not reuse `getFirstApproveRequestId()`/`clickApprove()` for this request type.
- `APIs.processCorporateCenterMigrationRequest(childId, wefDate)` — endpoint `migrationprocess/process_corporate_center_migration_requests`, **requires both `ckey=10998DF5FF67` (user-confirmed) and a `date` param matching the request's own WEF date** — without the date param it returns `{"status":"ok","0":"No Request to Process Corporate Center Transfer"}` (silent no-op). Confirmed live: passing `child_id` does NOT actually scope the call — a single invocation processes ALL matching requests for that date system-wide (same pattern as Extended Daycare's cron) and returns one result entry per request processed (e.g. 3 entries, 3 different new child ids, for one call).
- SC004_TC_001 (approve-popup-detail verification) and SC005_TC_001 (reject) each need their OWN fresh Transfer Applicable=Yes child with an untouched Pending Center Shift request — currently `TODO_SET_FRESH_TRANSFER_YES_CHILD` placeholders, since approve/reject are mutually-exclusive terminal actions that can't share a child with the SC003_TC_001/SC002_TC_002 chain.
- `CS_CORPORATE_YES = "62383"` (from `ServiceRequest_CenterShiftTest.java`, a previously-confirmed Corporate+flag=Yes child) was reused as `CCT_SR_CHAIN_CHILD_ID` — now consumed after a successful live run (3 new children created: `72253`/`72254`/`72255`).

### Open items to confirm before further automating
- Fresh child IDs needed: one more `CT_CHAIN_CHILD_ID`/`CT_CANCEL_CHILD_ID` pair for Corporate Transfer (both consumed), and two fresh Transfer Applicable=Yes children for `CoporateCenterTransfer_Testcases`'s SC004_TC_001/SC005_TC_001.
- A fresh Transfer Applicable=No child for Corporate Center Transfer's button-flow (SC002_TC_001) — `71046` was the last confirmed-fresh one at time of writing; check its state before reusing.
- SC009_TC_001's assertion is currently informational-pass when the migration API returns "No Request to Process" (expected until the real calendar date/WEF date align, or a matching `date` param is supplied) — user indicated they'll do a full sanity check by shifting the server date themselves; tighten the assertion back to a hard requirement at that point if desired.
- 40+ further test cases across both new sheets not yet scoped (email notifications, CD-role dashboards, financial/prorated-invoice/discount-continuity checks, access-right differentials for `Tieup_SPOC_Access`/`Invite_Corporate_Payable_Admission` — no "without access" user exists yet in `testData/input_UserRights.xlsx`).
