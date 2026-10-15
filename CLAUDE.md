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
mvn test -Dtest=testScripts.SupportTests.FinancialUpdates.OneTimeCharges_Testcases
```

**Run with a different browser (default is `chrome`):**

```bash
mvn test -Dbrowser=firefox -Dsurefire.suiteXmlFiles=OneTimeChargestestng.xml
```

Screenshots on failure land in `screenshots/`. Test results are written to `result/report.xlsx`.

## Architecture

This is a **Selenium + TestNG Page Object Model (POM)** framework targeting the Footprints franchise management web app
at `https://test-franchise.footprintseducation.in`.

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

**BaseTest lifecycle** — Every test class extends `BaseTest`. `@BeforeClass` launches Chrome, navigates to the login
URL, logs in with the default user (`Rakesh` / `Dev@123` from `IAutoConstant`), then automatically cycles through and
acknowledges any pending policy notification popups (up to 10 bells), then hides the notification dropdown via JS to
prevent click interception.

**User switching** — Many screens require switching to a module-specific user before testing. Test classes read
`testData/input_UserRights.xlsx` (sheet `UserRights`, columns: UserName | ScreenName | RightTitle) to find which user
has rights to a given screen name, then call `UserRightsPage.switchUser(user)` in `@BeforeClass`.

**Navigation** — All menu navigation goes through `Navigations.java`. It re-initialises `PageFactory` before each click
to handle `StaleElementReferenceException` caused by full page reloads during user switches.

**Test data** — Login scenarios come from `testData/input_Credential.xlsx` (sheet `LoginValidations`). User rights come
from `testData/input_UserRights.xlsx`. Online payment data comes from `testData/payment_not_received.csv`. Test data is
loaded via `ExcelDataProvider` or inline `@DataProvider` methods.

**JS workarounds** — Several form fields are `readonly` or blocked by overlays. The page objects use
`JavascriptExecutor` to remove `readonly`/`disabled` attributes, fire `input`/`change` events, and force-hide modal
backdrops when the app's own close buttons are unreachable.

**Suite XML files** — Each feature area has its own TestNG XML at the project root (e.g. `OneTimeChargestestng.xml`).
The `browser` parameter defaults to `chrome` and is passed to `BaseTest.openBrowser()` via `@Parameters`. Maven Surefire
looks for `testng.xml` by default; use `-Dsurefire.suiteXmlFiles=` to target a specific suite.

## Requirements — Extended Daycare Service Request (`ServiceRequest_ExtendedDaycareTest.java`)

Source: `TC_Extended_Daycare_Final.xlsx` (sheet `TC_Extended Daycare`). 10 test cases selected for automation.

### SC002_TC_001 — Happy Path: Submit → Pending (Critical)

Screen: `recent_update_details?child_id=<child_id>`
Prerequisites: Active Regular child. Login as Support staff. No pending Extended Daycare request.
Test Data: Start Date: 2026-07-02 | End Date: 2026-07-30 | Admission ID: 68671

1. Login as Support → Account Statement → enter Admission ID → click SERVICE REQUEST. **Expect:** Service Request popup
   opens.
2. Select 'Extended Daycare'. Enter Start Date = 2026-07-02, End Date = 2026-07-30. Submit → OK. **Expect:** Toast '
   Extended Daycare request submitted successfully'.
3. Navigate to Customer Request screen. **Expect:** screen loads.
4. Verify Request Type = 'Extended Daycare', Status = 'Pending', Approval Status = 'NA'.
5. Verify Center Name = child's center, WEF Date = Start Date, End Date = End Date selected.
6. Verify Created By = logged-in user name.
7. Verify CANCEL button (red) visible at Pending status.

### SC002_TC_002 — Happy Path: Pending → Approved via getAllPendingRequests API (Critical)

Screen: `recent_update_details?child_id=<child_id>`
Prerequisites: Extended Daycare request in Pending status.
API:
`{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373`
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
Expected: Daycare Fee + Preschool Fee + SGST 9% + CGST 9% + Roundoff, Total: Rs.23,200. GST applies to Daycare Fee
component only.

1. Navigate to Account Statement → find Extended Daycare invoice (PI/XXXXXX). **Expect:** invoice visible with all line
   items.
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

- `pages/Support/Regular_ServiceRequests.java` — Extended Daycare form already wired: `isExtendedDaycareFormVisible()`,
  `setEDFromDate()`, `setEDToDate()`, `submitExtendedDaycare()`.
- `utils/APIs.java` — unused constants `ED_APPROVE_REQUEST` (= Center Shift's `getAllPendingRequests`, ckey
  `B47C56483AAE7373`) and `ED_CRON_JOB` (`parentapp/extendedDaycareCronJob`, ckey `7A533862C14E`). Need methods built on
  them (no `getExtendedDaycare...` methods exist yet).
- `pages/Support/AccountStatementPage.java` — has `isExtendedDaycareInvoiceVisible()`,
  `getExtendedDaycareInvoiceLineItems()`, `getExtendedDaycareInvoiceTotal()` for invoice checks (SC003_TC_002/003).
- Reference pattern to follow: `ServiceRequest_CenterShiftTest.java` + `CenterShifttestng.xml`.

### Open items to confirm before automating

- Concrete child IDs for: inactive/attrition child (SC008_TC_001), child with an existing pending ED request (
  SC008_TC_002), child with half-day fee = Rs.11,999 (SC003_TC_001/002/003).
- UI selectors on the Customer Request screen for Request Type, Status, Approval Status, Center Name, WEF Date, Created
  By, Support Executive, Actions — reuse/extend `RecentCustomerRequestsPage.java`.

### NEW enhancement: Extended Day Care Stop / Early Resume (requirement received 2026-09-22, NOT yet automated)

**Problem:** Extended Day Care currently runs for a fixed duration once approved — if a parent applies for 15 days and
only needs 7, neither the parent nor Support can end it early; it runs to the original end date and the parent is
charged the full duration. Child Pause already has an equivalent **Early Resume** action (shortens an in-progress pause,
recalculates duration, raises the differential/credit invoice, notifies the parent) — this enhancement brings the same
capability to Extended Day Care.

**Scope:** shortening an already-started/in-progress ED request, charge recalculation and adjustment, child history
entry, parent + centre notification.

**Acceptance criteria (as given):**

1. A Stop / Early Resume action appears only for a child whose ED request is **active/in-progress** — NOT shown for
   Pending (use existing cancel flow instead), Completed, or Cancelled requests.
2. Selecting the action lets the user pick a new end date; must be **on/after the ED start date and before the current
   end date** — anything outside that range is rejected with a clear message (spec's own example wording: *"Early stop
   date cannot be before the Extended Day Care start date"* — not yet confirmed live).
3. On submit, the ED request's end date updates to the new date and the revised day count is recalculated from the
   original start date.
4. Charge is adjusted for the unused period — parent not charged for days after the new end date. If already
   invoiced/collected, a credit/adjustment entry is raised for the unused days, naming the revised period.
5. A child/admission history entry records the early stop (who did it, old end date → new end date, revised duration,
   timestamp).
6. A confirmation email goes to the parent (CC centre head, per the existing ED email flow), template under
   `Common/EmailTemplates/`.
7. Repeating the action twice on the same request, or submitting with a date equal to the current end date, must NOT
   create a duplicate adjustment/credit entry.
8. Available to Support team users on the internal portal.

**Reference URL/navigation (per spec):** Franchise portal → Children → Child → Services popup (
`franchise/children/pop_child_services.php`, JS `franchise/assets/js/pop_child_services.js`) — same screen hosting
Pause/Resume + Extended Daycare request actions.

**Confirmed live via screenshots (2026-09-22), children 74030 and 74034 — do not re-derive without evidence:**

- The action is actually surfaced as a new blue **"STOP / EARLY RESUME"** button in the **Actions** column of the Recent
  Customer Requests grid (`recent_update_details?child_id=<id>`), shown for an **Approved** ED row — this column used to
  render EMPTY at Approved status per this file's original ED notes (SC002_TC_002); that note is now superseded/extended
  by this button's existence. Not yet confirmed whether the Services-popup surface named in the spec is a second,
  separate entry point or the spec's own description of the same underlying screen family.
- Clicking it opens modal **"Extended Daycare Stop / Early Resume Request"**: `Extended Daycare Start Date` (
  read-only) + `New End Date` (date picker) + a dynamic info banner, e.g. *"Extended Daycare will now run for 4 day(s),
  shortened by 5 day(s) from the original end date."*
- Submit triggers a **native `confirm()`**: *"Are you sure you want to resume early?"* — must be accepted via WebDriver
  `Alert` handling.
- On OK, an inline success banner appears in the same modal: *"Extended Daycare end date has been revised. The invoice
  has been updated to Rs. <amount> for the revised period."*
- **Two distinct settlement paths observed, confirmed by user (2026-09-22) to be governed by whether the original ED
  invoice was already paid:**
    - **Child 74030 (unpaid invoice) → void + reissue:** original ED invoice (PI/993883, ₹5,850 — Daycare Fee +
      Preschool Fee + SGST/CGST + Roundoff) is **voided** ("Voided on 22 Sep, 2026", row highlighted red on Account
      Statement), and a **new** invoice (PI/993884, due 26 Sep 2026) is raised for the shortened period at **₹2,600.00
      ** — matches the modal's own "updated to Rs. 2600" success text.
    - **Child 74034 (already-paid invoice) → credit note, invoice untouched:** a credit entry appears on Account
      Statement — *"Credits - Extended DayCare | Extended Daycare Early Stop Credit, Period - 2026-09-25 To
      2026-09-30"* — ₹5,500.00, adjusted against the original invoice (which stays intact, Invoice Voided = 0 for that
      child).

**Not yet confirmed live:**

- Exact rejection message text for an out-of-range date (before start / on-or-after current end date).
- Idempotency behavior for a duplicate/no-op submission (AC #7).
- Where/how the child history entry actually renders (Account Statement → Child History, by analogy with the existing "
  Extended Daycare Started"/"...duration Completed" entries — SC002_TC_003/004 above).
- The email template file/name under `Common/EmailTemplates/` and its content — not itself independently verifiable via
  UI automation, likely logged as informational per this project's usual pattern for email-only ACs (c.f. Withdraw
  Child's SC002_TC_001, deferred as "no infra to test").
- Exact access-right name for this action (verified empirically elsewhere in this project rather than by name, per
  convention).
- Whether "Stop" and "Early Resume" are the same single action (the button/modal title uses both terms interchangeably,
  and the confirm/success text only ever says "resume early") or two distinct behaviors — current evidence points to one
  unified action.

**Existing building blocks to reuse:**

- `pages/Support/RecentCustomerRequestsPage.java` — will need a new STOP/EARLY RESUME button locator + modal-driving
  methods (date picker, confirm-handling, success-banner read) — same file already hosts the ED grid-reading helpers.
- `pages/Support/AccountStatementPage.java` — already has `isExtendedDaycareInvoiceVisible()`/
  `getExtendedDaycareInvoiceLineItems()`/`getExtendedDaycareInvoiceTotal()`; will need extending (additive only) to also
  read a voided-invoice row (mirroring Cancel Registration's `getVoidedInvoiceReferences()` pattern) and a credit-note
  line item.
- Per this project's "one file per screen" convention: new scenarios go as new `@Test` methods in the existing
  `ServiceRequest_ExtendedDaycareTest.java`, no new test class.

**Draft scenario list (pending user confirmation before automating — not yet built):**

1. STOP/EARLY RESUME button visible for an Approved/in-progress ED request.
2. Button NOT shown for a Pending ED request.
3. Button NOT shown for a Completed ED request.
4. Button NOT shown for a Cancelled ED request.
5. Valid early-stop date (≥ start date, < current end date) → submit → success banner, end date + day count revised
   correctly.
6. Date before ED start date → rejected with clear message.
7. Date on/after current end date → rejected (not a valid "early" stop).
8. Unpaid-invoice path → original invoice voided, new reduced invoice raised for revised period (child 74030 pattern).
9. Already-paid-invoice path → credit note raised for unused days, original invoice untouched (child 74034 pattern).
10. Child/admission history entry recorded (old→new end date, who, timestamp).
11. Parent + centre-head notification email sent — likely informational/logged only, not hard-asserted (no email infra
    to verify).
12. Repeat submission on the same request (or a same-as-current-end-date submission) does not create a duplicate
    credit/adjustment entry.

**Status (2026-09-22): automation started, unpaid-invoice (void+reissue) path CONFIRMED PASSING end-to-end.**

### Confirmed live (2026-09-22), children 74054 and 74131 — do not re-derive without evidence

- The action's real anchor markup:
  `<a class="popdown_medium btn btn-primary bg-blue btn-xs label" href="pop_update_request?pop=yes&extended_daycare_early_resume=yes&child_id=<id>&req_id=<id>&resume_date=<current end date>&pause_start_date=<ED start date>" title="Child Information">Stop / Early Resume</a>` —
  a `popdown_medium` AJAX modal, same family as Cancel Registration's `popdown_xl_large`. The URL param name
  `pause_start_date` (not e.g. `ed_start_date`) confirms this reuses Child Pause's own Early Resume handler, matching
  the requirement's framing.
- Modal header: `<h5 class="panel-title">Extended Daycare Stop / Early Resume Request</h5>`.
- New End Date field is a **pickadate.js** widget: `id="early_resume_date"`, popup root `id="early_resume_date_root"`.
  Unlike Withdraw Child's `attrition_date` / Tie-Up's `processing_date` pickadate widgets, this one's month header (
  `picker__month`/`picker__year`) is **plain text, not a `<select>`**, and both prev/next nav arrows are
  `picker__nav--disabled` — the valid date range never spans a month boundary for this flow, so no month navigation is
  needed, just a direct day-cell click.
- Info banner: `<label id="resume_days-alert" class="alert alert-primary" for="early_resume_date">` — dynamic text e.g.
  *"Extended Daycare will now run for 7 day(s), shortened by 2 day(s) from the original end date."* Confirmed the
  day-count math is correct (22–30 Sep shortened to 22–28 Sep = 7 days, 2 shortened).
- Submit button: `<button id="submit_early_resume" type="button" class="btn btn-primary">Submit</button>`. No native
  `confirm()` was observed on submit for either child (unlike many other service-request flows in this app).
- Success/result message renders as a generic `.alert-success`-family element: *"Extended Daycare end date has been
  revised. The invoice has been updated to Rs. &lt;amount&gt; for the revised period."* Amounts were proportional to the
  day-count reduction (child 74054: 6300 → 4900 for 9→7 days; child 74131: 7803 → 6069 for 9→7 days — both ≈ 7/9 of the
  original credit_debit_amount, consistent with a linear per-day proration).
- **Unpaid-invoice settlement path (void + reissue) CONFIRMED for both children:** original ED invoice voided (74054:
  `PI/993887`; 74131: `PI/993888`) and a new invoice raised for the revised period — read via the existing
  `AccountStatementPage.getVoidedInvoiceReferences()` (already built for Cancel Registration, reused as-is).
- **The credit-note ledger entry is NOT a `<tr>` — it's a plain `<div>` stack** (
  `<div style="border-left:5px solid #FF5722;">Credit issued on ... <div class="row">...Extended Daycare Early Stop Credit...</div></div>`),
  confirmed via exact live markup supplied by the user. `AccountStatementPage.getExtendedDaycareEarlyStopCreditText()`
  was written directly against this div structure (find innermost element containing the marker text, then walk up to
  the ancestor div containing "Credit issued on") — never assumed to be `<tr>`-based like
  `getVoidedInvoiceReferences()`.
- After Early Resume submit, the `popdown_medium` modal/backdrop can be left in the DOM blocking the top-nav menu (same
  class of issue already fixed for Cancel Registration) — worked around with `serviceRequestPage.closeModalByJs()`
  before navigating away, not a new shared-code fix.

### Sep-21 boundary — RESOLVED, NOT a bug (confirmed live by user, 2026-09-22)

The calendar visually allows selecting Sep 21 (one day before the Sep 22 ED start date) — this part was confirmed via
automated DOM dump. However, the user manually tested submitting that exact date on a different live child (68827,
req_id 168112, old_end_date 2026-09-30) and confirmed the app **correctly blocks it** with a clear inline error banner:
**"Early stop date cannot be before the Extended Day Care start date."** — this is the exact TBD rejection message from
AC #2, now confirmed live. So this is cosmetic calendar leniency only, not a validation bypass — no JIRA needed.
Confirmed live selectors: error banner renders in `<div class="flash-msg content-group-lg div-error mr-15 ml-15">` near
the top of the modal (separate element from the `#resume_days-alert` day-count info label, which still renders alongside
it showing "0 day(s), shortened by 9 day(s)" — clamped at 0, not negative).

### Case 2 (already-paid invoice → credit note) — CONFIRMED end-to-end, 2026-09-22, child 67647

Full automated chain succeeded: submit ED (22–30 Sep) → approve via API → Customer Portal → Pay Due Invoice → real UPI
payment via `APIs.postUpiPaymentEvent()` (HTTP 200, `{"status":"success","message":"Transaction received: ..."}`) →
Early Resume with new end date 28 Sep → **distinct success message confirms the credit path**: *"Extended Daycare end
date has been revised. A credit of Rs. 1734 has been issued to the account for the unused period."* — different wording
from the unpaid/void+reissue case's *"...invoice has been updated to Rs. &lt;amount&gt;..."* message, confirming the app
itself distinguishes the two settlement paths in its own response text, not just in the ledger.

**Bug found and fixed in a separate, pre-existing feature this reuses:**
`CustomerPortal_PayDueInvoices.extractUpiPaymentJson()` (built for the unrelated `DueInvoices_Testcases.java` feature)
was hardcoded to look for hidden field `id="payment_json_icici_upi"` — the user supplied the exact live DOM showing the
real id is **`payment_json_icici_up`** (no trailing "i"). Fixed to try both ids (real one first, original as fallback)
rather than assuming either was simply a typo. This means `DueInvoices_Testcases.java`'s own tests were likely failing
on this same root cause before this fix — worth a full re-run of that suite separately to confirm, since it's an
unrelated feature not otherwise touched here.

**Ledger re-check CONFIRMED (2026-09-22), read-only re-run** — the earlier `frm_child_id` timeout was a one-off
navigation glitch, not a real issue. Clean re-run for child 67647 confirmed: `Status=Approved`, `End Date=Sep 28, 2026`,
`Voided invoices=[]` (original paid invoice untouched), and the credit row: *"Credit issued on 22 September, 2026 01:55
PM against invoice P557/2627/333 (Adjusted against invoice P557/2627/333)"*. Case 2 is now fully confirmed end-to-end —
settlement path = CREDIT NOTE, no void, matching the paid-invoice rule exactly.

Children **68338** and **68419** were NOT consumed — the loop moved on to 68338 when the transient error above was hit,
but was stopped before submitting anything for it. Both remain fresh/available for future use.

### Open items

- Clean read-only re-verification of child 67647's ledger (Credit row present, no voided invoice) — pending, safe to do
  without resubmitting anything.
- Re-run `DueInvoicestestng.xml` to confirm whether the `payment_json_icici_up` field-id fix also resolves failures
  there (separate, unrelated feature — not part of this ED work's scope, but flagged since the same bug was found while
  reusing its page object).
- Scenarios 1–4 (button visibility rules: shown only for Approved/in-progress, hidden for Pending/Completed/Cancelled)
  not yet automated.
- Scenario 6 (invalid date before start rejected with clear message) — CONFIRMED (see above). Still need to confirm the
  on-or-after-current-end-date rejection case.
- Scenario 10 (child/admission history entry) and Scenario 11 (email notification) not yet checked.
- Scenario 12 (duplicate/no-op submission does not create a duplicate credit) not yet automated — children 74054/74131 (
  both now at End Date = Sep 28) are actually well-positioned to test this next, since a second Early Resume attempt
  with the same date would directly exercise AC #7.
- Child 74130 (originally supplied as a spare for the Sep-21 test) is **confirmed invalid/not usable** by the user — do
  not reuse.
- **Converted to permanent tests (2026-09-22), BOTH CONFIRMED PASSING:** `tc011_earlyResumeUnpaidInvoiceVoidAndReissue`
  and `tc012_earlyResumePaidInvoiceCreditNote` in `ServiceRequest_ExtendedDaycareTest.java` (priorities 11–12),
  replacing the temporary `diagnostic_*` methods (removed).
    - `tc011` — clean full run confirmed on child **68338**: original invoice `PI/993895` voided, new invoice raised for
      the shortened period. `EARLY_RESUME_UNPAID_CHILD_ID` now set to 68338 (consumed).
    - `tc012` — clean full run confirmed on child **70256**: credit of Rs. 2200 issued, original invoice `P605/2627/764`
      left untouched. `EARLY_RESUME_PAID_CHILD_ID` now set to 70256 (consumed).
    - Constants: `EARLY_RESUME_START_DATE`/`EARLY_RESUME_END_DATE`/`EARLY_RESUME_NEW_END_DAY` = `2026-09-22` /
      `2026-09-30` / `"28"`.
    - **Fixed a real navigation-timing bug in `tc012`** while chasing this down: going straight from the just-closed
      Early Resume modal to a top-nav menu click (to reach Account Statement) raced with leftover DOM state and timed
      out on `frm_child_id` — happened twice (children 67647 and 70243). Fixed by adding a direct URL navigation (
      `recentRequestsPage.getEDEndDate(...)`) before `navigations.goToAccountStatement()`, matching `tc011`'s
      already-working sequence.
    - **Server-date drift caused two false "child invalid" scares mid-session** — the test server's clock briefly
      advanced to Oct 1 (confirmed via the portal header clock), silently breaking ED submission for children 68419 and
      70243 since the hardcoded Sep 22/Sep 30 dates had become past dates (same "past date" rejection class as
      Transport's submit-form issue elsewhere in this file). Neither child was actually invalid. User reset the server
      date back to Sep 22 and confirmed it — dates reverted to the original Sep values, not the Oct workaround.
    - **AC #7 (duplicate/no-op submission) incidentally observed, not yet a dedicated test:** re-running Early Resume
      against a child already at the target end date showed the info banner correctly reporting "shortened by 0 day(s)",
      and the Submit button never became clickable — the app appears to block a genuine no-op resubmission client-side.
      Worth a dedicated scenario 12 test later, but not yet written as one.
    - All child IDs supplied for this feature across the session (74054, 74131, 67647, 68338, 70243, 70256) are now
      consumed — fresh Regular children (no existing ED request; the paid-invoice one also needs a payable due invoice
      reachable via Customer Portal) are needed for the next clean re-run of either test.

### Button-visibility scenarios (2026-09-22) — 3 of 4 states CONFIRMED, 1 real bug found

`tc013_stopEarlyResumeHiddenPendingVisibleApproved`, `tc014_stopEarlyResumeHiddenAfterCompleted` (superseded by tc016
below), `tc015_stopEarlyResumeHiddenAfterCancelled`, `tc016_historyEntryAndHiddenAfterGenuineCompletion`,
`tc017_duplicateSubmissionNoDuplicateLedgerEntry` — priorities 13–17.

- **Pending → hidden, Approved → visible: CONFIRMED PASSING** (child 70801).
- **Cancelled → hidden: CONFIRMED PASSING** (child 70779) — reuses the generic `button.cancel_customer_request`/
  `confirmCancelRequest()` cancel flow already wired for Program Change/Corporate Transfer; final grid status reads "
  Cancelled".
- **Duplicate/no-op resubmission → no duplicate ledger entry: CONFIRMED PASSING** (child 70256, tc017) — credit entry
  count stayed at exactly 1 before and after a same-date resubmission attempt; Submit button never became clickable for
  the no-op.
- **🐛 BUG CONFIRMED — Completed → button does NOT hide (child 70801).** Engineered a genuine same-day completion by
  using Early Resume itself to shorten an Approved request's end date to *today* (its original end date was tomorrow, so
  this was the only day the picker allowed) — confirmed via the `extendedDaycareCronJob` API response explicitly naming
  this child ("Child ID 70801 ... Status: Completed") and a Child History entry "Extended Daycare duration Completed"
  dated today. Despite genuine completion, **STOP/EARLY RESUME remained visible** — confirmed twice (once during the
  original run, once via a clean read-only recheck with a screenshot). This directly contradicts the stated
  requirement ("not shown for requests that are... already Completed"). `tc016` asserts the spec'd (correct) behavior,
  so it will keep failing until this is fixed — **flagged for JIRA**, not a test-code bug.
- **Real, additive bug fix to the shared Child History reader**: `AccountStatementPage.getHistoryParagraphs()` (its
  `modal-body text-left > pre-scrollable > p` locator) returns **0 elements** against the app's current Child History
  modal — confirmed via screenshot that the modal has been redesigned into a card-based, tabbed "Child Updates History (
  N) / Backend Requests (N)" layout that no longer matches that old locator, even though entries clearly exist (13 shown
  for child 70801). Added `getChildHistoryFullText()` (additive, dumps the whole modal's text for substring matching) as
  the fix — `historyParagraphs` itself left untouched per this project's "never edit existing methods" convention, since
  `tc004`/`tc007` (the original 10-test suite) still reference it and haven't been re-verified against this
  apparently-redesigned modal this session.
- **Child History entry for the early-stop action CONFIRMED with exact live text**: *"Extended Daycare Early Stop: end
  date revised from 2026-09-23 to 2026-09-22, revised duration 1 day(s) (by Jaydeep Kar)"* — richer than the spec asked
  for (names the acting user, both old and new dates, and the revised duration explicitly).
- Child 70801 is now genuinely Completed (consumed) and also exhibits the button-visibility bug above — not suitable for
  reuse in a clean future run of tc013/tc014/tc016. Children 70485 and 70383 (supplied same batch) remain unused spares.

### Email notification check (2026-09-22) — CONFIRMED, new reusable infra built

Corrected an earlier assumption that no email-testing infra exists in this project — the user confirmed a live **Email
View** screen: Settings → Email View, direct URL `https://test-franchise.footprintseducation.in/email_view`, *
*accessible only to the Rakesh user** (not Jaydeep Kar, who `ServiceRequest_ExtendedDaycareTest` switches to). Built as
a standalone, reusable pair — **not scoped to Extended Daycare** — for any feature's tests to use going forward:

- `pages/Settings/EmailView_Page.java` — mirrors `SMSView_Page.java`'s structure. Confirmed live: a standard DataTables
  grid (`id="DataTables_Table_0"`), columns Email Id | Email Subject | Email Header | Email Failed Reason | Created
  Date | From Email | Email Status | Action. **Row order is NOT reliably "most recent first"** — the default view's top
  row was a future-dated (01 Oct 2026) scheduled entry ahead of same-day entries.
- **Convention (per explicit user instruction, 2026-09-22): always filter by the dedicated Subject field, not the
  generic DataTables search box.** The real advanced filter form is `id="frm-search"` (Email | Subject | Body | From
  Email | Email Status | Submit) — confirmed live field ids: Subject input `id="subject"`, Submit button
  `id="btn_submit"`. `filterBySubject(subjectKeyword)` drives this form;
  `hasEmailContaining(subjectKeyword, ...requiredSubstrings)`, `getFirstMatchingRowText(subjectKeyword)`, and
  `getAllMatchingRowTexts(subjectKeyword)` all filter by Subject first, then scan the resulting rows for
  feature/child-specific text. Each feature passes its own relevant subject text (e.g. `"Extended Daycare"` for this
  feature) — `searchTable()` (the generic DataTables box) is kept only for ad-hoc diagnostic use, not feature lookups.
- `pages/Navigations.java` gained `goToEmailView()` (direct URL, mirrors `goToSmsView()`); `utils/IAutoConstant.java`
  gained `EMAIL_VIEW_URL`.
- `testScripts.SettingsTests.EmailView_Test` — stays on the default Rakesh login throughout (no user switch), same
  convention as `CancelRegistration_testcases`.
- **`tc001_extendedDaycareCompletionEmailRecorded` — CONFIRMED PASSING**: found the exact live email record *"Extended
  Daycare Duration Completed - for Anaisha (#70801)"*, timestamped 22 Sep 2026 16:05 PM — matching the exact same-day
  completion engineered in `ServiceRequest_ExtendedDaycareTest.tc016`. All emails in this test environment show
  `Email Status = Pending` (not "Success") — the environment appears to queue/record rather than actually deliver, which
  is fine for confirming the notification trigger fired correctly.
- **`tc002_extendedDaycareEarlyStopEmailCheck` — informational, not hard-failed**: no email row for child 70801
  mentioned "Early Stop"/"Early Resume"/"revised" specifically — only the Completion email (above) was found. Suggests
  the app may only send one notification (on Completed), not a separate one at the moment Early Resume is submitted —
  needs product confirmation before hardening this into a real assertion either way.

This completes all four previously-open items for the Extended Day Care Stop / Early Resume enhancement (
button-hidden-after-Completed, child history entry, email notification, duplicate-submission ledger check) — see the "
Button-visibility scenarios" section above for the full status of each, including the one confirmed real bug (button
stays visible after genuine Completion).

## Requirements — Online Payment Received: HDFC SmartGateway Integration

Screen: Support → Online Payments Received → Update Payment modal. New radio button added to the existing
`payment_type[]` group: `<input type="radio" name="payment_type[]" value="smartgateway" id="smartgateway">` — a newer
HDFC integration sitting **alongside** the pre-existing `hdfc` radio (already automated as TC_OP_014), not a replacement
for it. Automated as 2 new `@Test` methods appended to the existing `OnlinePaymentReceived_Testcases` class (priorities
21–22), per this project's "one file per screen, new methods only" convention — no new test class, no edits to existing
methods.

### Confirmed live (do not re-derive without evidence)

- **Real "Filter by Gateway" label is "HDFC Smart"** — NOT "HDFC SmartGateway" as initially assumed from the radio's
  `id`/`value`. Confirmed by dumping the live multiselect options:
  `[ICICI, ICICI UPI, ICICI CC/DC, HDFC, HDFC Smart, Footprints, Payumoney, ...]`.
- August 2026 had **zero** "HDFC Smart" transactions; searching with no date filter found exactly **one** live record at
  the time of testing (2026-09-01, child #72026 Viraj Kumar Mahajan, Invoice Payment, Success, ₹11621.00).
- Update Payment modal flow for this radio works end-to-end exactly like PayUMoney/HDFC/ICICI: select radio (
  `id="smartgateway"`, JS click) → enter transaction ID → Retrieve Details → either a valid transaction (textarea
  `#payu_details` populated → Submit Details) or an alert (`.alert-warning`/`.alert-danger`) for an
  invalid/already-processed one.
- Two supplied transaction IDs (`43bb624cfda3dc2cdbce`, `9eb4ca8ca04fde151cef`) both returned **"Warning: Transaction
  already received! This Transaction ID has already been processed."** — handled by the existing invalid-transaction
  branch (same pattern as TC_OP_013b's rejected-transaction test), not a bug.
- A third, fresh transaction ID (`4886243ffaecfa62a4f4`) confirmed the **full valid path live**: Retrieve Details →
  `isTransactionValid()` true → Submit Details clicked successfully.

### New OnlinePaymentReceived.java methods (additive only, existing methods untouched)

- `selectSmartGatewayPaymentType()` — dedicated method (not a new case in `selectPaymentType()`'s existing switch) that
  JS-clicks the `#smartgateway` radio.
- `getGatewayFilterOptions()` (diagnostic) — opens the "Filter by Gateway" multiselect and returns every option's real
  label text; this is what surfaced the "HDFC Smart" vs "HDFC SmartGateway" discrepancy.
- `dumpVisibleRows()` (diagnostic) — dumps the text of every currently visible table row, used to inspect the one live "
  HDFC Smart" record found during exploration.

### Status: both new tests CONFIRMED PASSING

`verifyUpdatePaymentHDFCSmartGateway` (TC_OP_021) and `verifyFilterByGatewaySmartGateway` (TC_OP_022) — run individually
and together, 2/2 passing. Full 22-test suite (`mvn test -Dsurefire.suiteXmlFiles=OnlinePaymentReceivedtestng.xml`)
confirmed 22/22 passing after the fixes below.

### Bug found & fixed while refreshing stale test data (TC_OP_008/009)

`verifySearchByChildName`/`verifySearchByChildID` had stale hardcoded data ("Ranjeeta"/"69126", date range
06/01/2026–06/02/2026) that no longer matches live data. While refreshing these with real child Name "Kiyansh"/ID "
53115" (per user's ask), found two separate genuine bugs, not data staleness:

- **`filterByDateRange()` (existing method, sendKeys-based) silently corrupts the date-range picker's state and always
  returns 0 entries** — confirmed live: applying it with ANY range, including one that spans today (which independently
  has real data), returns "Showing 0 to 0 of 0 entries". Root-caused by dumping the picker's live DOM: it's a
  `daterangepicker` (dangrossman jQuery plugin) whose calendar already defaults to today→today (visible via its
  `active start-date active end-date` class on today's cell) — clicking Apply **without touching the text inputs at all
  ** correctly returns real data (8 entries). Typing into `daterangepicker_start`/`_end` via `sendKeys` breaks the
  widget's internal moment.js state — same class of bug as the pickadate.js issue already documented for Withdraw
  Child's `attrition_date` field elsewhere in this file. This also means the pre-existing `TC_OP_007` (Filter by Date
  Range) has likely been passing on a technicality all along — its assertion only checks the info text is non-empty,
  and "Showing 0 to 0 of 0 entries" satisfies that. Not touched per this project's "never edit existing methods"
  convention — flagged here for awareness.
- **Fix, additive only:** added `filterByToday()` — opens the picker and clicks Apply immediately, relying on its
  default today→today selection, rather than typing dates. `TC_OP_008` now uses this + a live-reconfirmed real child, "
  Dharya Sharma" (child ID 69135, found on 2026-09-01's data — "Kiyansh"/"53115" did not appear in any reachable window
  and were not used).
- **The Child ID column is a hidden `<td style="display:none">`** (not shown in the UI at all — confirmed via
  cell-by-cell DOM dump) — every `<td>`'s content up to and including this hidden one is present in the DOM, so the
  existing `isChildIDInTable()`'s xpath match succeeds, but its `row.isDisplayed()` check then always returns `false`
  for this specific column, so the assertion can never pass regardless of data freshness. Per explicit user decision, *
  *TC_OP_009 (Search by Child ID) was removed entirely** rather than fixing `isChildIDInTable()` — only Child Name
  search is kept going forward. `isChildIDInTable()` itself was left in `OnlinePaymentReceived.java`, unused, per "never
  edit/remove existing methods" (it may still be useful if a future screen ever needs to check a genuinely visible
  ID-like column).
- **Note for future re-runs:** since this is live report data, both the "HDFC Smart" transaction IDs and the "Dharya
  Sharma"/69135 pair used above will age off as the report window moves — re-supply fresh values (or re-derive them the
  same way: dump today's default table view) when they stop matching.

## Requirements — Time Extension Service Request (`ServiceRequest_TimeExtensionTest.java`)

Source: `TC_Time_Extension_Updated.xlsx` (sheet `TC_Time Extension`). 6 test cases selected for automation.

### SC002_TC_001 — Start Time Extension: Submit request via Service Request form (High)

Screen: Support → Account Statement → Service Request → Start/Stop Time Extension
Prerequisites: Active Regular child. Time Extension enabled at center.
Test Data: Admission ID: 46085 | Service Type: Start Time Extension
Note: Parents can extend services by 90 min — drop off 8:15 AM, pick up by 7:15 PM.

1. Login as Support → Account Statement → enter Admission ID → click SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Start Time Extension' from Services dropdown. **Expect:** form loads.
3. Enter WEF date. Click Submit. **Expect:** confirmation popup 'Do you want to send request for time extension?' with
   OK & Cancel.
4. Click Cancel on popup. **Expect:** request NOT submitted, form stays open.
5. Click Submit again → OK. **Expect:** toast 'Your request submitted successfully.'
6. Navigate to Customer Request screen. Verify Request Type = 'Start Time Extension', Status = Pending.

### SC002_TC_002 — Start Time Extension: Full flow Submit → API → Approve → Process API (High)

Screen: `recent_update_details?child_id=<child_id>`
Prerequisites: Active Regular child. Admission ID: 70800.
APIs:

-

`{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373` (
**Note:** per the Extended Daycare finding, the real param is `child_id`, not `chid_id` — verify which one actually
scopes correctly here too before trusting results.)

-

`http://test-admissions.footprintseducation.in/api/childservices/processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D`

1. Submit Start Time Extension request. **Expect:** toast success, Status = Pending.
2. Run `getAllPendingRequests` API. **Expect:** status=ok, type='Start Time Extension', status='Pending', admission_id
   correct, credit_debit_amount='0', parent_name='Support Request', current_status='Active', admission_type='Regular'.
3. Navigate to Customer Request screen. Verify Approve button displayed.
4. Click Approve button → confirm. **Expect:** request approved on Customer Request screen.
5. Run `processTimeExtentionRequest` API. **Expect:** `{"status":"ok","message":"Time Extension request processed"}`.
6. Navigate to Customer Request screen. Verify Status = Approved.
7. Navigate to Account Statement. Verify Addons section shows 'Time Extension'.
8. Verify prorated invoice generated: 'Prorated Time Extension Charges - <Month>, <Year> (<N> days)'.

### SC002_TC_010 — Start Time Extension: processTimeExtentionRequest API response (Critical)

Screen: API / Postman
Prerequisites: Start Time Extension approved on Customer Request screen.
API:
`http://test-admissions.footprintseducation.in/api/childservices/processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D`
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
API:
`{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373`
Expected: type='Stop Time Extension', date=WEF date, end_date=same as date, credit_debit_amount='0', status='Pending',
parent_name='Support Request', admission_type='Regular'

1. Submit Stop Time Extension request. Note WEF date selected.
2. Run `getAllPendingRequests` API.
3. Verify response status=ok, type='Stop Time Extension'.
4. Verify date = WEF date selected, end_date = same as date.
5. Verify credit_debit_amount = '0' (no immediate charge on stop request).
6. Verify status = 'Pending', parent_name = 'Support Request'.

### SC003_TC_005 — Stop Time Extension: processTimeExtentionRequest API response (Critical)

Screen: API / Postman
Prerequisites: Stop Time Extension approved on Customer Request screen.
API:
`http://test-admissions.footprintseducation.in/api/childservices/processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D`
Expected: `{"status":"ok","message":"Time Extension request processed"}`

1. Approve Stop Time Extension on Customer Request screen.
2. Run `processTimeExtentionRequest` API.
3. Verify response status = 'ok'.
4. Verify response message = 'Time Extension request processed'.
5. Navigate to Account Statement. Verify Time Extension addon REMOVED.

### Existing building blocks to reuse

- `pages/Support/Regular_ServiceRequests.java` — Start/Stop Time Extension forms already wired:
  `isStartTimeExtensionFormVisible()`, `setSTEFromDate()`, `setSTEToDate()`, `submitStartTimeExtension()`,
  `isStopTimeExtensionFormVisible()`, `setSTPFromDate()`, `submitStopTimeExtension()`.
- `utils/APIs.java` — `getExtendedDaycarePendingToApproved(childId)` hits the same physical `getAllPendingRequests`
  endpoint with the same key/ckey — need an equivalent Time Extension method (or a shared generic one) plus a new method
  for `processTimeExtentionRequest` (different ckey: `3E529969372D`).
- `pages/Support/RecentCustomerRequestsPage.java` — has the generic grid-reading pattern (`getEDColumnValue`-style,
  filtering by Request Type) to mirror for Time Extension rows; also has `clickApprove(requestId)` already for the
  Approve button.
- `pages/Support/AccountStatementPage.java` — will need an Addons-section reader (not yet built) to verify 'Time
  Extension' addon appears/disappears, distinct from the existing Extended Daycare invoice-line-item reader.

### Open items to confirm before automating

- Confirm whether `getAllPendingRequests` actually needs `chid_id` or `child_id` for Time Extension (the Extended
  Daycare investigation found `child_id` was the real, correctly-scoping param — `chid_id` was silently ignored).
- Concrete child IDs: Admission ID 46085 (submit tests) and 70800 (full-flow test) are given in the sheet — need to
  confirm these are currently in the right state (Active, Time Extension enabled at center, no conflicting pending
  request) before each run.
- UI locator for the Addons section on Account Statement (to verify 'Time Extension' addon add/remove) — not yet
  explored.
- Whether Time Extension is enabled at the relevant center for the given admission IDs (SC001_TC_001's config step is a
  prerequisite, not in our automated scope).

## Requirements — Withdraw Child Service Request (`ServiceRequest_WithdrawChildTest.java`)

Source: `TC_ServiceRequests_Withdraw.xlsx` (sheet `TC_Withdraw`). 7 test cases selected for automation (SC004_TC_001
deferred — see below).

### SC001_TC_001 — Full flow via background jobs/APIs (High)

Screen: Service Request → Withdraw Child
Test Data: `Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<id>` ;
`parentapp/processChildApprovedRequest?child_id=<id>` (spec's own example URLs omit a ckey for both).

1. Apply Withdraw Child request.
2. **Back-dated:** Child Attrition Request auto-approved & attrition processed immediately — no manual approval needed.
3. **Future/current-dated:** Needs approval from CD (Support) on Customer Request screen, then run
   `processChildApprovedRequest` API → approved as per WEF automatically.
4. Automated as data-driven (`@DataProvider "withdrawDatePaths"`) — **future-dated only this round**; back-dated row
   commented out, deferred to next sprint per user decision.

### SC001_TC_002/003/004/005 — Submit for each withdraw reason (Medium)

Screen: Service Request → Withdraw Child
Test Data: To Date (future, 30 days out); Reason: Transfer / Not Satisfied with Services / Moving to formal schooling /
Others.

1. Open URL → Support → Account Statement → enter Admission ID → Service Request.
2. Select Service Type → Withdraw Child Request, verify default view.
3. Fill To Date & reason.
4. Click Submit → confirm popup ("Do you want to send Withdraw Child request?") → OK.
5. Toast: "Your request submitted successfully."

Automated as 4 separate `@Test` methods (`testWithdraw_Transfer`, `testWithdraw_NotSatisfied`,
`testWithdraw_FormalSchool`, `testWithdraw_Others`) rather than one `@DataProvider` method — user's explicit choice,
since child IDs for these come from TestNG `@Parameters`/XML (no code edit needed when a child gets consumed) rather
than a hardcoded data array. Each also calls `verifyPendingStatus(childId)` (beyond the base spec) to confirm Pending
status after submit.

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

Needs a second user without the `Raise_Support_Request` right to compare against — no such row currently exists in
`testData/input_UserRights.xlsx` (only `Program Change` has a differentiated-rights precedent, added specifically for
that purpose). Deferred until that test-data gap is filled.

### SC003_TC_001 — RETAIN as its own scenario (High) — `tc009_retainAdmission`

Screen: Service Request → Withdraw Child → Customer Request
Prerequisites: Withdraw Child request Pending, Approval Status not yet run through `processChildApprovedRequest`.

1. Submit Withdraw Child request. Confirm Pending on grid.
2. Click RETAIN on the Child Attrition row (row-scoped via `request_id`) → accept native confirm "You want to retain
   attrition request?".
3. Verify Request Status = "Cancelled".

### Update Attrition Request — WEF date change on a pending record — `tc010_updateAttritionRequest`

Screen: Service Request → Withdraw Child → Customer Request
Prerequisites: Withdraw Child request Pending, Approval Status = Pending (UPDATE REQUEST button only shows in that
window — same window as RETAIN, before `processChildApprovedRequest` runs).
Confirmed by user: **Update Request can be submitted multiple times on the same pending record before its WEF date** —
it does not get consumed/one-shot the way Approve/Retain do, so the same child can be reused to re-test the update flow.

1. Submit Withdraw Child request. Capture WEF Date before.
2. Click UPDATE REQUEST on the Child Attrition row (row-scoped via `request_id`, same pattern as RETAIN/APPROVE —
   plain/JS click does not open the modal, requires `Actions.moveToElement().click()`).
3. In the "Update Attrition Request" modal, pick a new (future, later than current WEF) date via the calendar and enter
   a mandatory Comment. Submit → accept native confirm "Are you sure want to update this request?".
4. Refresh grid. Verify row still present (not deleted) and WEF Date changed to the new value.

### Confirmed live (do not re-derive without evidence)

- The grid's "Request Type" column for a Withdraw Child submission reads **"Child Attrition"**, not "Withdraw Child" —
  confirmed from the `getAllPendingRequests` response (`"type":"Child Attrition"`, `"name":"<child>-Child Attrition"`)
  and the grid itself. "Withdraw Child" is only the Service Request dropdown's label.
- `withdraw_reason` dropdown options, exact text: `Transfer`, `Not Satisfied with Services`,
  `Moving to formal schooling` (lowercase "formal schooling"), `Others`.
- The `reason_comment` field (`id="reason_comment"`) only renders in the DOM when reason = "Others" — for the other 3
  reasons it's absent (not just hidden), so waiting on its visibility times out. Selecting "Others" without filling it
  submits with no popup/toast at all (silent client-side validation block).
- `getAllPendingRequests` is a **stateful trigger, not a plain read** (same pattern as Center Shift/Extended
  Daycare/Time Extension) — calling it flips the grid straight from `Pending` to `Processing`. Any check for `Pending`
  status must happen *before* calling it, not after.
- A Child Attrition row has **no generic `button.approve`/`button.reject` pair**. The actionable controls are:
    - **RETAIN** (`id="retained_attrition"`, `request_id` attribute present) — cancels/rejects the pending attrition ("
      retain the child" = don't withdraw them). This is what SC002_TC_003's "click on cancel button" refers to.
    - **APPROVE** (`class="label btn btn-primary"`, text "Approve" — **no** `id`/`request_id` attribute) — must be
      located row-scoped: find RETAIN's `request_id`, walk up to its `<tr>`, then find the "Approve"-text element within
      that same row.
    - Clicking APPROVE only works via a genuine `Actions(driver).moveToElement(el).click().perform()` — neither
      `WebElement.click()` nor a JS-dispatched `.click()` opens the modal (confirmed by repeated live testing).
- Clicking APPROVE opens an "Approve Attrition Request(#childId)" modal: pre-filled "Approved By", and a **mandatory**
  Comment field (`id="approve_text"`, textarea) — Submit button is `id="approve_attrition"`. Submitting it triggers a *
  *native confirm()**: "Are you sure want to approve this request?" — must be accepted or every subsequent WebDriver
  call throws `UnhandledAlertException`.
- `processChildApprovedRequest` only processes a request **as of its WEF date** — calling it for a genuinely
  future-dated request (e.g. 30 days out) returns HTTP 200 with body `null`, a **silent no-op**, not an error. To
  observe the terminal `Approved` state within a single test run, WEF must be set to **today**, not a real future date.
  This applies to both the "future-dated" data-provider row and `tc007`.
- Manual Approve (click APPROVE → fill comment → Submit → accept native confirm) is **mandatory** before
  `processChildApprovedRequest` does anything — confirmed directly by the user, and matches the spec's own wording ("
  need to take approval from CD as well then run the API").
- `processChildApprovedRequest` ckey `9414D96600C5` (reused from Center Shift's `processOldChildAttrition`) is *
  *confirmed correct** by the user for this use too — the earlier "null" response was caused by the WEF-date timing
  above, not a wrong ckey.
- `getWithdrawChildPendingRequests` reuses ckey `B47C56483AAE7373` (same physical
  `Financialprocess/getAllPendingRequests/` endpoint as Center Shift/Extended Daycare/Time Extension) — spec's example
  omits a ckey but this is confirmed working via live responses.
- Helper methods that read/act on a specific child's row (`getFirstRetainAttritionRequestId`, `clickApproveAttrition`)
  must **navigate to that child first**, never rely on whatever page happens to already be loaded in the shared tab —
  confirmed live that stale tab state from a previous test silently returns/acts on a DIFFERENT child's `request_id`.
- **UPDATE REQUEST** (row-scoped, same pattern as RETAIN/APPROVE — no `id`/`request_id`, must walk up from RETAIN's
  `<tr>`) opens an "Update Attrition Request(#childId)" modal: date field `id="attrition_date"` + mandatory Comment
  `id="update_reason"` (textarea) + Submit `id="update_attrition"`. Submit triggers a native confirm() "Are you sure
  want to update this request?" — same dismiss-on-any-WebDriver-call hazard as elsewhere, must go straight to the alert
  check.
- The `attrition_date` field is **pickadate.js** (`class="picker__input"`, popup root `id="<fieldId>_root"`), **not**
  jQuery UI datepicker and **not** Pikaday, despite both being used elsewhere in this app (confirmed live via DOM
  inspection — `.picker__select--year`/`.picker__select--month` native `<select>` elements plus
  `.picker__day[data-pick=<epoch>]` day cells). Setting the value via JS injection (`datepicker('setDate', ...)`)
  rendered the correct-looking value in the DOM but **silently deleted the record on submit** instead of updating it —
  the internal pickadate state never got set, so the backend received a broken payload. Fixed by driving the real
  widget: `Select` on the year/month dropdowns (fires native `change`), then a genuine `.click()` on the matching
  `.picker__day--infocus:not(.picker__day--disabled)` cell — mirroring exactly what a manual user does. This is what
  actually resolved the "row vanishes after Update Request" bug, not the earlier confirm-handling or refresh-timing
  theories.
- Confirmed by user: Update Request can be run **multiple times on the same pending record before its WEF date** —
  unlike Approve/Retain it isn't a one-shot/terminal action, so the same child ID can be reused across repeated update
  attempts without needing a fresh child each time.

### Existing/added building blocks

- `pages/Support/Regular_ServiceRequests.java` — Withdraw Child form already wired: `isWithdrawFormVisible()`,
  `setWithdrawToDate()`, `selectWithdrawReason()`, `enterWithdrawComment()`, `submitWithdraw()`.
- `utils/APIs.java` — added `getWithdrawChildPendingRequests(childId)` and `processWithdrawChildRequest(childId)`.
- `pages/Support/RecentCustomerRequestsPage.java` — added `findWithdrawChildRow()` (filters grid by Request Type = "
  Child Attrition"), `getWithdrawColumnValue/RequestStatus/ApprovalStatus(admId)`,
  `getFirstRetainAttritionRequestId(childId)` / `clickRetainAttrition(requestId)` (reject/cancel),
  `clickApproveAttrition(requestId)` / `submitApproveAttrition(comment)` (approve modal, Actions-click + native-confirm
  handling), `clickUpdateRequest(requestId)` / `submitUpdateRequest(newDate, reason)` (update modal, pickadate calendar
  click + native-confirm handling).
- `WithdrawChildtestng.xml` — suite file, now includes all 10 test methods; 4 reason-variant child IDs supplied via
  `<parameter>` tags (TestNG `@Parameters` injected into instance fields via `@BeforeClass`), the other 6 scenarios (
  including `tc009`/`tc010`) via hardcoded constants in the test class.

### Current status (as of 2026-07-14): ALL 10 TEST CASES CONFIRMED PASSING

- Original 8 (SC001_TC_001-006, SC002_TC_002/003): validated together in one combined
  `mvn test -Dsurefire.suiteXmlFiles=WithdrawChildtestng.xml` run with 8 simultaneously-fresh child IDs — **8/8 passed,
  0 failures.**
- `tc009_retainAdmission` (SC003_TC_001, RETAIN as its own scenario): passed independently — `Request Status` → "
  Cancelled" confirmed correct.
- `tc010_updateAttritionRequest` (Update Attrition Request WEF-date change): passed independently after fixing the
  pickadate calendar-interaction bug described above — WEF Date changed correctly (e.g. Aug 13, 2026 → Aug 28, 2026) and
  the row stayed present after refresh.
- These were each confirmed as individual/independent passing runs, not one single 10-test combined suite execution —
  user explicitly opted to skip the extra combined run and proceed on the strength of the individual confirmations.

The Withdraw Child automation (10 test cases from `TC_ServiceRequests_Withdraw.xlsx`) is functionally complete.
Remaining work, not started:

- **SC004_TC_001** (access-right validation) — needs a differentiated-rights row for Withdraw Child in
  `testData/input_UserRights.xlsx`, same pattern as `Program Change`.
- **Back-dated path for `tc001`** — coded but commented out, never run live (banked ID `69755` for next sprint).
- **41 further test cases in the same sheet, not yet scoped**: SC002_TC_001 (email — no infra to test),
  SC002_TC_004/005 (agent distribution/availability — backend routing, not really UI-testable), SC003_TC_002-004 (
  Re-join flow), SC004_TC_002-009 + SC005/SC006 (32 cases — future-date validation edge cases, invoice voiding, refund
  calculation; deep financial/data-integrity checks likely needing new page objects and backend verification, not yet
  scoped for feasibility).

### User-data change (2026-09-10): "Corporate Account Statement" screen user renamed

`testData/input_UserRights.xlsx` (sheet `UserRights`) row `Varsha Jha | Corporate Account Statement | Tieup_SPOC_Access`
updated to **`Shivali Gupta | Corporate Account Statement | Tieup_SPOC_Access`** —
`getUserForScreen("Corporate Account Statement")` now switches to Shivali Gupta instead of Varsha Jha for every feature
that uses this screen key (Admission Migration, Corporate Transfer, Corporate Center Transfer, Tie-Up Program Change).
Historical notes below that mention "Varsha Jha" reflect the state at the time they were written — the current live user
for this screen key is Shivali Gupta.

## Requirements — Corporate Transfer / Corporate Center Transfer / Tie-Up Program Change

Source specs: `TC_TieUp_ProgramChange.xlsx` (Tie-Up Program Change), `TC_Corporate_All.xlsx` (sheets
`TC_Corporate Transfer` and `TC_Corporate Center Transfer`). These are three DISTINCT features that all live on the
Account Statement page for Corporate/Co-Pay/Employee tie-up children, sharing one page object (
`Corporate_ServiceRequests.java`) but split into separate test classes per the user's explicit request.

### Confirmed live: three distinct links on Account Statement (Corporate child)

- **TIE UP PROGRAM CHANGE** — `href="javascript:addTieupProgramChange('<child_id>')"` →
  `TieupProgramChange_Testcases.java`
- **CORPORATE TRANSFER** — `href="pop_corporate_transfer?pop=yes&child_id=<child_id>"` →
  `CorporateTransfer_Testcases.java`
- **CORPORATE CENTER TRANSFER** — `href="pop_center_transfer?pop=yes&child_id=<child_id>"` →
  `CoporateCenterTransfer_Testcases.java`
  All three can appear together on the same child's Account Statement page — do not assume only one is relevant just
  because a child is "Corporate."

### Tie-Up Program Change (`TieupProgramChange_Testcases.java`)

- `tc001_tieupProgramChange` (child `71962`): full Add → Save → Approve flow via the modal (form fields reuse
  `applicable_month`/`new_center`/`new_program_name`/`add_request` ids — see below). Confirmed passing (pre-existing
  from earlier sprint, preserved as-is during the file split).
- Corporate_ServiceRequests.java's Tie-Up modal and the Corporate Transfer / Corporate Center Transfer modals all reuse
  the SAME underlying field ids (`applicable_month`, `new_center`, `new_program_name`, `add_request`) — confirmed live
  via DOM diffing. Only which fields are actually shown/required differs per flow (e.g. Corporate Transfer adds an Offer
  dropdown `new_offer_id`; Corporate Center Transfer's button-flow omits it).

### Tie-Up Program Change — NEW enhancement: effective-date rule changes (requirement received 2026-09-10, NOT yet automated)

Screen: Account Statement → Tieup Program Change Request. New rules for which effective date a user is allowed to pick,
replacing/extending the current validation logic.

**IMPORTANT — user will change the test server's date manually before each date-dependent test runs.** Do NOT run any
test tied to a specific date window until the user explicitly confirms the server date has been changed to match that
scenario. Wait for that signal every time, per test.

**Acceptance criteria:**

1. **Last 2 days of the month** (window varies by month length, e.g. Aug 30–31, Sep 29–30) → effective-date picker must
   still allow selecting the **1st of the upcoming month** (not blocked/pushed further out).
2. **1st–4th of the current month** → picker must allow selecting the **1st of the current month**. Selecting it must *
   *void the existing invoice for that month and generate a new invoice** reflecting the co-pay program change.
3. **Both cases above** must show an **alert/info message** stating the request must be approved before the processing
   date, or the program change will not take effect. Message copy must be consistent across all three date-based paths (
   exact copy is currently **TBD** — not yet confirmed, do not guess/hardcode it into an assertion until confirmed
   live).
4. **Outside these windows** — existing behavior applies, no regression.
5. **If not approved before the processing date** — program change does NOT take effect, no invoice voided/created.

**Technical notes (from spec, for whoever implements/tests this):**

- "Last 2 days of month" must be computed dynamically (days-in-month based: 28/29/30/31), not a fixed calendar date.
- Reuse/extend the existing invoice void + regenerate flow (same family as other void/regenerate flows already automated
  elsewhere, e.g. Withdraw Child) for the 1st–4th case — confirm no double-invoicing/duplicate co-pay charges.
- Confirm whether date comparisons use **server date or client date** (timezone edge cases) — not yet confirmed.

**Decisions confirmed by user (2026-09-10):**

- Alert/info message copy: no text supplied in advance — will be read live off the actual modal once the server date is
  set to a matching window, then confirmed correct by the user before locking it into an assertion.
- No separate test-case sheet for this enhancement — scenarios are derived directly from the 5 acceptance criteria
  above (draft list below), not sourced from an Excel file like other features.

**Draft scenario list (derived from acceptance criteria, pending user confirmation before automating):**

1. Effective-date picker allows the 1st of the upcoming month during the last-2-days-of-month window (e.g. server date =
   Aug 30 or Aug 31) — AC #1.
2. Effective-date picker allows the 1st of the current month during the 1st–4th-of-month window — AC #2.
3. Selecting the 1st of the current month (1st–4th case) voids the existing invoice for that month and generates a new
   invoice reflecting the co-pay program change — AC #2/#3 (reuse the voided-invoice-reading pattern already built for
   Cancel Registration).
4. Alert/info message appears for both date-rule paths (#1 and #2 above) — AC #3.
5. Outside these windows, existing effective-date behavior is unchanged (regression check, e.g. mid-month date) — AC #4.
6. Un-approved request past its processing date does NOT take effect (no invoice voided/created) — AC #5. Flagged as the
   hardest to automate (needs a request left deliberately unapproved until its own processing date arrives, similar to
   timing-gate limitations already documented for Corporate Center Transfer/Withdraw Child elsewhere in this file) — may
   need to be confirmed as informational/manual rather than a hard automated assertion.

**Status (2026-09-10): PAUSED — core pre-existing regression found, unrelated to the new enhancement code.**

Built so far (additive only): `Corporate_ServiceRequests.java` gained `getAnyVisibleAlertOrInfoMessage()` (generic,
non-"TIEUP"-keyword-scoped alert/info reader, for the new enhancement's yet-unconfirmed message copy) and a diagnostic
dump inside `doTieupProgramChange()` (fires only when the Add-step toast is empty — dumps any visible alert/info text
plus the modal's full HTML). Also fixed a real bug in the existing private `selectFirstAvailable()` helper: it didn't
recognize "Please Select" as a placeholder (only "--"-prefixed text or exact "Select"), so it could select a placeholder
as if it were a real dropdown choice — now also excludes "Please Select" (case-insensitive). Added a null-check to
`doTieupProgramChange(programName, ...)`: passing `null` now picks the first available real program option (same pattern
as Corporate Transfer's null-param convention), for children whose live program list is unknown ahead of time.
`TieupProgramChange_Testcases.java` gained a new `tc002_effectiveDateRegressionOutsideWindows` test (Scenario 5) and
bumped the pre-existing `TIEUP_PROCESSING_DATE`/`TIEUP_WEF_DATE` constants from a now-stale `2026-07-23` to
`2026-09-17` (data-only fix, TC001's logic untouched).

**Core finding: the entire Tie-Up Program Change submission flow (`doTieupProgramChange()`) is currently broken, not
just the new scenarios.** Confirmed via the pre-existing, previously-passing `tc001_tieupProgramChange` (child 73462) —
even with a genuinely valid, fresh future date (2026-09-17, confirmed via the picker's own minimum-lead-time default)
and no other changes, the "Add Program Change Request" step produces **no toast**, and the modal stays open (
`style="display: block;"`) — the "APPROVE TIEUP PROGRAM CHANGE REQUEST" link never appears because the Add request never
actually succeeded.

Root cause (from a live modal HTML dump captured mid-diagnostic): the `processing_date` Pickaday widget's own internal
calendar state does NOT reflect what `setDateByJs()` injects. `setDateByJs()` sets the raw `<input>` `.value` via JS and
fires `change`/`input`/`blur` events, and conditionally calls `el._picker.setDate(...)` if that property exists — but
the widget's visible calendar (year dropdown, highlighted day) stayed on its own default (e.g. showed "August 2027" for
two different children — 73554 and 52641 — regardless of what ISO date was injected, in one investigation branch; showed
a sane "September 2026, min-lead ~7 days" default in another, unrelated to the injected value either way). This means
whatever value the app's own JS actually reads from the widget at submit time doesn't match the injected date, and the
server silently rejects the request (there's a `<div class="program-change-error-msg" style="display:none;">` in the
DOM — likely holds the real validation reason, but never becomes visible per our checks).

**This is the same class of bug already fixed once before in this project** — see the Withdraw Child section above:
`attrition_date`'s own pickadate.js widget was fixed by driving the REAL widget (year/month `<select>` change events + a
genuine `.click()` on the target day cell) instead of JS-injecting the value, because JS injection updated the DOM but
never the widget's actual internal state, silently corrupting the backend payload. `setDateByJs()` in
`Corporate_ServiceRequests.java` is very likely hitting the identical failure mode for `processing_date`/`wef_date`, and
may always have needed the same fix — `tc001`'s "confirmed passing" status may be stale from before something changed (
library version, server config, or this was never actually deeply verified beyond a screenshot-driven happy path).

**User decision (2026-09-10): PAUSE.** User manually checked the live site and confirmed this is a genuine app-side
bug — **reported to the dev team**. Automation of the effective-date enhancement (and any further Tie-Up Program Change
work) is **blocked until the dev team fixes it**. Do not modify `setDateByJs()`, `doTieupProgramChange()`'s core flow,
or run further live tests against this feature until the user confirms the fix has landed.

**Server-date-check reminder (added 2026-09-10, applies going forward for this feature and likely others):** before
running any test — especially date-sensitive ones — check the ACTUAL server date/time, not assumed real-world date. It's
visible on the portal header near the profile icon, in body text as a pattern like
`<center name?> | <bell count> | <Mon DD, HH:MM AM/PM> | <count> | <username>` (confirmed live:
`"Test | 2 | Sep 10, 04:29 PM | 4 | Shivali Gupta"`). The user manually changes this server date for date-window testing
and it does NOT always match real-world "today" — confirm it directly rather than assuming.

**Status (2026-09-11): dev fix confirmed deployed, automation resumed, Scenario 1 CONFIRMED PASSING end-to-end.**

### Fixes made to get the core flow (and Scenario 1) working

1. **Real-widget date picker** — `setDateByJs()`'s value-injection never actually updated the Pickaday widget's internal
   state (confirmed live: always fell back to its own default, e.g. "today+7 days", regardless of injected value). Added
   `setDateByRealPicker(WebElement inputField, String rootId, String isoDate)` — drives the actual widget: clicks
   `picker__nav--prev`/`picker__nav--next` to reach the target month/year (this widget's year `<select>` is `disabled`,
   unlike Withdraw Child's enabled selects, so arrow-navigation is used instead of selecting an option), then a real
   `.click()` on the target day cell. `doTieupProgramChange()`'s two `setDateByJs(...)` calls (processing_date,
   wef_date) now call this instead. `setDateByJs()` itself left in place, unused.
2. **Native confirm() after "Add Program Change Request"** — confirmed live: clicking Add triggers a native
   `confirm()` ("Are you sure you want to submit the program change request?") that was never being accepted, causing
   every subsequent check to silently fail (`unexpected alert open` errors swallowed by broad try/catch, masking the
   real cause for a long time). Added `acceptNativeConfirmIfPresent()`, called after every Add/Approve button click.
3. **Approve button locator** — for a Long Term tie-up child (73462) the Approve control renders as a `<button>` with
   text "APPROVE TIE UP PROGRAM CHANGE REQUEST" (space between TIE/UP); for a Short Term tie-up child (67087) it renders
   as an `<a href="javascript:updateTieupProgramChange('<id>','Processing')">` with mixed-case text "Approve Tie Up
   Program Change Request". `approveTieUpPC_btn`'s locator now matches both forms (button-or-anchor by text,
   case-insensitive, plus the href fallback) — which form a given child gets is not fully understood (Long vs Short Term
   tie-up, or something else).
4. **Toast-reading fallback** — confirmed on both 73462 and 67087: even on a **genuinely successful** approval (page
   badge changes to "TIE UP PROGRAM CHANGE REQUEST(APPROVED)"), `getTieupToastMessage()` comes back **empty** — meaning
   the pre-existing TC001 assertion (`approvedToast.contains("APPROVED SUCCESSFULLY")`) would falsely fail even when the
   feature works correctly. Added `getApprovedStateFallback()`: when the toast is empty, checks the page body for the "(
   APPROVED)" badge text and returns a normalized string containing "APPROVED SUCCESSFULLY" so existing assertions still
   pass. Wired into both `doTieupProgramChange()` and `approveExistingTieupRequest()`.
5. **New helper `approveExistingTieupRequest(wefDate, feeBreakupAmount, parentMonthly, corporateMonthly)`** — for a
   child that already has a pending/processing request (no "TIE UP PROGRAM CHANGE" entry button at all in that state —
   only the Approve button), duplicating `doTieupProgramChange()`'s steps 4-6 rather than refactoring the existing
   method, per this file's established per-flow-duplication convention.

### Confirmed live — the exact TBD alert/info message copy

`"This request must be approved before the processing date (<date>), or the program change will not take effect."` —
appears as `<div class="program-change-error-msg alert alert-info">` inside the Add modal (previously always empty/
`display:none`). Matches AC #3. **Only appears when the chosen processing date needs auto-correction** (e.g. picking "
2026-09-17" got corrected to "2026-10-01" with this warning shown, and the "To Program" dropdown reset to "Please
Select", requiring a re-select + second Add click to actually proceed) — it did **NOT** appear when directly selecting
the already-correct date (2026-10-01) for either 73462 or 67087. This contradicts AC #3's literal wording ("alert must
show for both rule paths") — live behavior is treated as the source of truth, logged via `getLastEffectiveDateWarning()`
rather than hard-asserted.

### Scenario 1 (last-2-days-of-month) — CONFIRMED PASSING end-to-end (2026-09-11)

User set the server date to **2026-09-29** and supplied child **67087**.
`tc003_effectiveDateLastTwoDaysAllowsUpcomingMonth` selects **2026-10-01** (1st of the upcoming month) as processing/WEF
date — full Add → native confirm → Approve → native confirm flow completes, ending in a genuine "TIE UP PROGRAM CHANGE
REQUEST(APPROVED)" badge state (confirmed via live page-text dump, not just toast). Child 73462 (TC001's original child)
was used for the initial fix/investigation and is also now fully Approved (consumed) as a side effect.

### Consumed test data

- **73462** (TC001's child) — now Approved (consumed) from the investigation. TC001 will need a fresh child for its next
  clean run, or can stay as a "known-approved" reference.
- **67087** (Scenario 1 child) — now Approved (consumed).
- Fresh children needed for: a clean TC001 re-run, and Scenario 2/3/4/6 (1st-4th-of-month window, invoice
  void/regenerate check, un-approved-past-processing-date check).

### API endpoint added (2026-09-11): `processTieupProgramChangeRequest(childId)`

`{{Base_URL}}migrationprocess/processTieupProgramChangeRequests?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<child_id>&ckey=2107B04D9668` —
per user, run this **on the request's own WEF date**; a positive response is only expected on/after it (same timing-gate
family as other process/cron endpoints in this project). Wired into `APIs.java` and called from every effective-date
scenario test after the UI flow, per user's instruction ("in each case run the API too").

### Additional fixes/additions (2026-09-11, continued)

- **Disabled-day-cell fallback** in `setDateByRealPicker()`: confirmed live that the 1st of the current month (Scenario
  2/3's target date) can still render as `picker__day--disabled` in the calendar UI even though the effective-date rule
  should allow selecting it — the strict "non-disabled" xpath match threw `NoSuchElementException`. Now falls back to
  matching the day cell regardless of disabled state and clicking it via JS when the strict match fails.
- **`acceptNativeConfirmIfPresent()` wait extended from 5s to 10s**, plus a second defensive call added right before the
  Approve button click — confirmed live the native confirm() can appear later than expected (possibly
  server-load-related), causing "unexpected alert open" on the next WebDriver call if not accepted in time.
- **`addTieupProgramChangeOnly(programName, processingDate)`** — new method for Scenario 6 (AC #6: un-approved request
  must not take effect): duplicates the Add-only portion of `doTieupProgramChange()` (steps 1-3 + warning handling),
  deliberately never clicking Approve.
- **Test server had a live outage mid-session (2026-09-11)** — manifested as login timeouts/"waiting for username field"
  and "no such window" browser crashes; confirmed by user, resolved after they checked. Not a code issue — if these
  exact symptoms recur, check server status before assuming a regression.

### Final scenario → child ID mapping (confirmed by user, 2026-09-11)

- Scenario 1 (last-2-days-of-month): child **67087** — CONFIRMED PASSING.
- Scenario 2/3 (1st-4th-of-month, invoice void/regenerate): child **72765** — attempted 2026-09-11, form silently reset
  selecting day "1" with no warning message. Root cause: **the server's custom test date (Sep 2) appears to have reset
  back toward real-world date after the live outage/restart that day** (confirmed via a later screenshot showing the
  header at "Sep 9" for a different child, not the Sep 2 the user had set) — meaning "1st of current month" was no
  longer within the 1st-4th window at the time of that attempt. Needs the server date re-set to a genuine 1st-4th window
  before retrying.
- Scenario 5 (regression, outside windows): child **73799** attempted 2026-09-11 — **has no "TIE UP PROGRAM CHANGE"
  option at all** (its action row only shows Corporate Center Transfer/Corporate Transfer/Migrate to Regular). Confirmed
  via screenshot, not a bug — this child's tie-up type ("OPTUM BY PROEVES... HALF DAY LONG TERM") apparently doesn't
  support Program Change. Needs a replacement child that has the option available.
- Scenario 6 (un-approved request, no effect): child **73519** — **CONFIRMED PASSING (2026-09-14)**. Submitted via
  `addTieupProgramChangeOnly()` (left unapproved) → `processTieupProgramChangeRequest` API correctly returned
  `{"status":"error","error":"There are no pending tie up program change requests!"}` (HTTP 200) → confirmed no invoice
  voided. Validates AC #6 end-to-end.

### Confirmed live (2026-09-14): API's own "no effect" response text

For an un-approved request, `processTieupProgramChangeRequest` responds
`{"status":"error","error":"There are no pending tie up program change requests!"}` — this is the negative-case
counterpart to the positive "processed as per WEF date" response expected for Scenarios 1/2-3/5.

**Open items (as of 2026-09-11):**

- Scenario 1 (last-2-days-of-month) is DONE. Remaining: Scenario 2/3, Scenario 4 (alert message assertion — partially
  covered, see the "only appears on auto-correction" note above), Scenario 5, Scenario 6 — all coded and ready, awaiting
  live runs with the assigned children above.
- Still unconfirmed: why 73554/52641 originally showed a broken "Aug 2027" picker default while 73462/67087 worked
  correctly once the real fixes were in place — never fully root-caused; may simply have been masked by the
  alert-not-handled bug rather than being a separate issue.
- Which factor determines button-vs-anchor for the Approve control (Long Term vs Short Term tie-up, or something else)
  is not confirmed — only two data points so far.

### Corporate Transfer (`CorporateTransfer_Testcases.java`) — 4/4 test cases confirmed passing

- **SC002_TC_001** — Submit via CORPORATE TRANSFER link → verify Request Status = Pending.
- **SC003_TC_001** — Approve via "Approve Corporate Transfer" (confirmed live: this happens **inline on Account
  Statement**, not on Recent Customer Requests as the spec's own wording implied) → verify Request Status = Processing.
- **SC008_TC_001** — Cancel a Pending request → verify Cancelled. **Cancel control is `button.cancel_customer_request`
  ** (same generic class already used for Program Change cancellation), NOT an `<a>` tag — reuses
  `RecentCustomerRequestsPage.isCancelProgramChangeButtonVisible()`/`clickCancelProgramChange()`. Test is idempotent: if
  a Pending request already exists for the cancel child (e.g. from a prior partial run), it skips re-submitting and
  cancels the existing one directly — but must explicitly `navigations.goToAccountStatement()` first if it does need to
  submit, since the idempotency check itself navigates away to Recent Customer Requests.
- **SC009_TC_001** — `migrationprocess/process_corporate_migration_requests` API (
  `APIs.processCorporateMigrationRequests(childId, date)`) → verify old child Attrition + new child created. **Confirmed
  live: requires a `date` param matching the request's own WEF date** (1st of the selected Joining Month, e.g.
  `2026-08-01` for "Aug 2026") — without it, or with a mismatched date, the API returns
  `{"status":"ok","0":"No Request to Process Corporate Transfer"}` (still HTTP 200, silently a no-op, NOT an error). No
  `ckey` needed for this endpoint. Attrition-status text check must be case-insensitive (`translate()`) — the app does
  not reliably render it all-caps.
- Corporate Transfer has no `getAllPendingRequests` step in its own flow (unlike Center Shift/Extended Daycare/etc.) —
  submit and approve are both direct UI actions; only the final month-end migration is API-driven.
- Test-child chaining: `CT_CHAIN_CHILD_ID` (submit→approve→migrate, one child, gets consumed/attritted by the end) is
  SEPARATE from `CT_CANCEL_CHILD_ID` (cancel only) — cancel and approve are alternate branches of the same Pending state
  and cannot share a child. Both are consumed after one full run — a fresh child is needed for each subsequent clean
  re-run.
- Offer/Center/Program dropdowns: when the exact right values aren't known ahead of time for an arbitrary child,
  `Corporate_ServiceRequests.selectFirstAvailable()` picks the first non-placeholder option — used via passing `null` to
  `submitCorporateTransfer(month, null, null, null)`.
- **`CT_JOINING_MONTH` must always be a FUTURE month — the Joining Month dropdown excludes the current calendar month (
  confirmed live).** It goes stale every time the calendar rolls into whatever month it was set to (e.g. it was "Sep
  2026" and broke with `NoSuchElementException: Cannot locate option with text: Sep 2026` once today became
  2026-09-02) — bump it forward, and keep `CT_MIGRATION_DATE` matching as the 1st of that same month.
- Re-confirmed 2026-09-02 with `CT_JOINING_MONTH="Oct 2026"`/`CT_MIGRATION_DATE="2026-10-01"` and fresh children
  `CT_CHAIN_CHILD_ID=73410`/`CT_CANCEL_CHILD_ID=73326`: 4/4 passing. Migration API created new child **73706** from
  73410 (now Attrition). Both child IDs are now consumed — need a fresh pair for the next clean re-run.

### Corporate Center Transfer (`CoporateCenterTransfer_Testcases.java`) — 3/5 confirmed, 2 need fresh test data

- Two distinct submission paths per spec, confirmed live:
    - **Transfer Applicable=No** → the "CORPORATE CENTER TRANSFER" button's own modal (form `id="frm-center-transfer"`,
      fields `applicable_month`/`new_center`/`new_program_name`/`add_request` — same ids as Corporate Transfer's modal,
      just without the Offer dropdown). → SC002_TC_001.
    - **Transfer Applicable=Yes** → SERVICE REQUEST → Center Shift (the exact same form already automated for Regular
      children in `ServiceRequest_CenterShiftTest.java`, reused here via `Regular_ServiceRequests`). →
      SC003_TC_001/SC002_TC_002.
- Both paths land on Recent Customer Requests as **Request Type = "Center Shift"** (not "Corporate Center Transfer") —
  use `getColumnValueByRequestType(childId, "Center Shift", ...)`.
- **No generic "Approve" button exists for Center Shift-type rows** — confirmed live via row-HTML dump: once
  `getAllPendingRequests` flips Pending→Processing, the Actions column shows only **Cancel** (
  `button.cancel_customer_request`) and **Processing Details** — approval is entirely API-driven. Do not reuse
  `getFirstApproveRequestId()`/`clickApprove()` for this request type.
- **CORRECTED (2026-08-17), supersedes the original note above:** `process_corporate_center_migration_requests` (ckey
  `10998DF5FF67`) is NOT the approve API for Service-Request/Center-Shift-sourced rows — it was a wrong-API bug in the
  test, not a date-matching issue as first suspected. Confirmed live via `getAllPendingRequests`' own full response
  body: Service-Request-submitted rows are recorded with `"type":"Center Shift"`, never `"Corporate Center Transfer"` —
  so `process_corporate_center_migration_requests` (which filters on `"Corporate Center Transfer"`-typed rows only)
  silently returns `{"status":"ok","0":"No Request to Process Corporate Center Transfer"}` for these regardless of any
  `date` param (tried exact-matching WEF date, no date param, still no-op) — it's a type mismatch, not a timing bug.
  That endpoint is correctly used ONLY by the button-flow (SC002_TC_001, genuinely typed `"Corporate Center Transfer"`).
- **Correct pairing for Center-Shift-typed rows (Service-Request flow)** — the SAME two-step the plain non-corporate
  Center Shift feature already uses: `APIs.getCenterShiftPendingToProcessing(childId)` (
  `Financialprocess/getAllPendingRequests/`, Pending→Processing) then
  `APIs.getCenterShiftProcessingToApproved(childId)` (`servicerequest/cronProcessCenterShiftRequests`, ckey
  `B43C083098B7`, Processing→Approved, creates new child + attrition row). Both methods already existed in `APIs.java`
  from the original Center Shift feature — no new endpoint needed, just the correct existing one.
- `cronProcessCenterShiftRequests` **refuses to process a request before its own attrition/end date arrives** —
  confirmed live, returns
  `{"status":"ok","0":{"request_id":"...","result":{"status":"error","error":"Request #... cannot be processed before attrition date (<date>)."}}}`.
  This is a genuine, informative timing gate (same family as the future-dated-WEF no-op documented elsewhere for
  Withdraw Child/Transport) — `sc002_tc002_fullFlowViaServiceRequest`/`sc004_tc001_approvePopupDetails` now treat this
  specific response as **informational** (logged, not a hard failure) rather than asserting hard success, since it can't
  be forced to complete same-day.
- **Center Shift's own attrition/end date is NEVER the date typed into the effective-date field** — confirmed live:
  submitting with `setCSEffectiveDate("2026-08-17")` (today) recorded `"date":"2026-08-19"` (today+2) and
  `"end_date":"2026-08-18"` (today+1) in the raw API response — the form silently snaps to some minimum lead time rather
  than accepting the literal typed value. The grid's "WEF Date" column reads `end_date`, not `date`. Earlier "off by one
  day" child data (71430/73212, WEF Aug 31 vs a Sep 1 constant) was this exact same relationship (`end_date` = `date` −
  1), not a bug — the code just referenced the wrong field.
- Because of the above, `sc002_tc002`/`sc004_tc001` (Service-Request-flow chain) **cannot reach a genuinely "Approved"
  terminal state within the same run they're submitted in** — the attrition date is always at least 1–2 days out at
  submission time, and the approve API refuses early. Confirming full "Approved" requires either running the approve
  step on/after the request's own attrition date, or the user shifting the test server's clock forward (same pattern
  already noted for Corporate Transfer's SC009_TC_001).
- SC004_TC_001 (approve-popup-detail verification) and SC005_TC_001 (reject) each need their OWN fresh Transfer
  Applicable=Yes child with an untouched Pending Center Shift request — approve/reject are mutually-exclusive terminal
  actions that can't share a child with the SC003_TC_001/SC002_TC_002 chain.
- `CS_CORPORATE_YES = "62383"` (from `ServiceRequest_CenterShiftTest.java`, a previously-confirmed Corporate+flag=Yes
  child) was reused as `CCT_SR_CHAIN_CHILD_ID` — now consumed after a successful live run (3 new children created:
  `72253`/`72254`/`72255`). Chain child was subsequently rotated to `71430` (consumed, WEF-locked Aug 31) then `71839` (
  fresh 2026-08-17, submitted → Pending → advanced to Processing during live API investigation, own attrition date
  2026-08-18 not yet reached at time of writing).

### Open items to confirm before further automating

- Fresh child IDs needed: one more `CT_CHAIN_CHILD_ID`/`CT_CANCEL_CHILD_ID` pair for Corporate Transfer (both consumed),
  and two fresh Transfer Applicable=Yes children for `CoporateCenterTransfer_Testcases`'s SC004_TC_001/SC005_TC_001.
- `CCT_APPROVE_DETAIL_CHILD_ID` (`73212`) is now blocked the same way `71430` was — already has a Pending Center Shift
  request (WEF end_date Aug 31, `date` Sep 1) — needs a fresh replacement to re-verify `sc004_tc001_approvePopupDetails`
  against the corrected API pairing.
- To get a genuinely-Approved confirmation of `sc002_tc002`/`sc004_tc001` (rather than the informational "cannot be
  processed before attrition date" branch), either re-run `getCenterShiftProcessingToApproved` for child `71839` (
  request `166300`) on/after 2026-08-18, or submit a fresh child and re-run once the server clock reaches that request's
  own attrition date.
- A fresh Transfer Applicable=No child for Corporate Center Transfer's button-flow (SC002_TC_001) — `71046` was the last
  confirmed-fresh one at time of writing; check its state before reusing.
- SC009_TC_001's assertion is currently informational-pass when the migration API returns "No Request to Process" (
  expected until the real calendar date/WEF date align, or a matching `date` param is supplied) — user indicated they'll
  do a full sanity check by shifting the server date themselves; tighten the assertion back to a hard requirement at
  that point if desired.
- 40+ further test cases across both new sheets not yet scoped (email notifications, CD-role dashboards,
  financial/prorated-invoice/discount-continuity checks, access-right differentials for `Tieup_SPOC_Access`/
  `Invite_Corporate_Payable_Admission` — no "without access" user exists yet in `testData/input_UserRights.xlsx`).

## Requirements — One Time Charges: Block Invoicing on Attrition Child Records

Screen: `https://test-franchise.footprintseducation.in/onetime_charges`. Per user's stated motto ("automate each feature
or screen in one place"), all scenarios below live as new `@Test` methods directly in the existing
`testScripts.SupportTests.FinancialUpdates.OneTimeCharges_Testcases` class (priorities 6-8, after the existing 22), with
supporting
methods appended (never edited) to `pages.Support.Financial_Updates.OneTimeChargesPage` — no separate test class or
suite XML was created
for this.

### Context (bug being fixed)

Currently, Center Head or anyone with One Time Charges access (`Apply_Onetime_Charge` right) can raise an invoice
against an **attrition child record** (a child already withdrawn/left the school). This has caused real incidents where
an invoice meant for a child's new/current record was mistakenly raised on their old attritted record instead.

### Acceptance criteria

1. **Do not allow** raising a one-time-charge invoice against an attrition child record by default.
2. **Add an exception**, the same way one already exists for the Support team — via an access right, so that whoever is
   granted that right can still raise the invoice, provided they enter a **mandatory comment/reason**.
3. **Show a warning message on screen** if someone without the exception right attempts to raise an invoice for an
   attrition child.

### Confirmed live, test data

- `attrition_child_invoice_allow = 1` (month) on the test server — confirmed by user.
- **Jaydeep Kar** (`Apply_Onetime_Charge` right, plain-text `charge_child_id` input + `btn_child_details` Fetch flow —
  the same UI as the original 22 tests) → exception-case, attrition child **60006** (`EXCEPTION_ATTRITION_CHILD_ID`,
  superseding the earlier 71750 which was past the eligibility window — user supplied 60006 specifically to exercise the
  checkbox path). `testExceptionUserCanRaiseChargeOnAttritionChild` (priority 6): **confirmed live, full positive path.
  ** Warning shown is the same flat *"Cannot raise invoice charges for an Attrition child."* text Nidhi sees (message
  text does NOT distinguish eligibility — only checkbox visibility does). Exact live DOM for the exception block:
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
  No dedicated reason field exists — `enterExceptionReasonIfPresent()` correctly finds nothing and the test falls back
  to the existing `enterChargeComments()`, which IS the mandatory reason field for the exception flow. Full flow (tick
  checkbox → comment → charge type/amount → submit → confirm → success) completes end-to-end. Test still branches on
  live checkbox state (visible → full positive flow; hidden → assert block warning instead) so it self-adapts if child
  60006 ever ages out of the eligibility window too.
- **Nidhi Chaturvedi** (Center Head/CD, no dedicated OneTime Charges row needed in `input_UserRights.xlsx` — her
  existing `Program Change` row is enough to `switchUser()`) → blocked-case, attrition child **48737**.
  `testCenterHeadIsBlockedOnAttritionChild` (priority 8): **PASSES**. Confirmed live: for her role, `charge_child_id` is
  NOT a text input — it's a searchable `<select>` (select2) listing all children, no separate Fetch button exists at
  all. Setting its value + firing a `change` event via JS (`enterChildIdRobust()`) is what actually triggers the block.
  Her message is the flat, non-date-qualified **"Cannot raise invoice charges for an Attrition child."** (distinct from
  Jaydeep's month-specific message), and the exception checkbox never appears for her — consistent with her having no
  exception right at all (vs. Jaydeep's apparent date-conditional eligibility). Message is captured and printed to
  console per request.
- `testActiveChildFlowStillWorks` (priority 7, active child **68192**, still as Jaydeep): **PASSES** — confirms the
  pre-existing active-child flow is unaffected by all the above additions.

### Shared-infrastructure fix (affects all screens, not just this one)

`Navigations.oneTimeChargesLink`'s original locator was an absolute positional XPath (`li[7]/div[2]/li[1]`) that assumed
one fixed menu shape. It broke for Nidhi (her role renders a differently-shaped nav). Fixed, with user confirmation, to
match by the link's own text instead of position:
`//a[@href='onetime_charges' and normalize-space(text())='OneTime Charges']`. Note there are **two** distinct
`href="onetime_charges"` anchors in the DOM — an "Apply OneTime Charges" Billing-shortcut and the real top-level nav
item, plain "OneTime Charges" — a bare `//a[@href='onetime_charges']` matches the wrong one and breaks navigation for
every user, so the exact-text qualifier is required.

### New OneTimeChargesPage methods (additive only, existing methods untouched)

`getAttritionWarningMessage()` / `isAttritionWarningVisible()`, `dumpVisibleModalsAndAlerts()` / `dumpModalHtml()` (
diagnostics), `findExceptionCheckbox()` (private) / `isExceptionCheckboxVisible()` / `checkExceptionCheckbox()` (targets
`#div-attrition-exception input[type='checkbox']`), `enterExceptionReasonIfPresent()`, `isSubmitFormEnabled()`,
`printPageSourceSnippetContaining()`, `enterChildIdRobust()` (JS-based, for roles like Nidhi's where the field is a
disabled/readonly select rather than a plain text input), `dumpChildIdAndFetchButtonState()`,
`clickFetchChildDetailsForced()`.

### Status: all 3 new scenarios PASS (confirmed together in one live run)

`testExceptionUserCanRaiseChargeOnAttritionChild`, `testActiveChildFlowStillWorks`,
`testCenterHeadIsBlockedOnAttritionChild` — 3/3 passing with the exact child IDs supplied (71750, 68192, 48737), no
fresher child needed per user instruction.

### Open items

- The checkbox+reason+success branch of `testExceptionUserCanRaiseChargeOnAttritionChild` is written but not yet
  exercised live (child 71750 takes the hard-blocked branch instead) — will self-exercise automatically if a future
  child ID falls inside the exception's eligibility window.
- The exact access-right name and where it's actually assigned (not required for testing so far — behavior was verified
  empirically rather than by right name).

## Requirements — Transport Service Request (`ServiceRequest_Transport1WayTest.java` /

`ServiceRequest_Transport2WayTest.java`)

Source: `TC_Transport.xlsx` (sheet `TC_Transport`). 20 test cases in the full sheet (SC001–SC012, covering full E2E
flow, submit, dropdown checks, 1-Way/2-Way approve forms, cancellation, invoicing, credit/debit on plan switch,
recurring billing, and stop-transport flows). **6 selected for automation this round**, split by flow into two test
classes per user's explicit request:

- `ServiceRequest_Transport1WayTest.java` — SC001_TC_001, SC002_TC_001, SC002_TC_002, SC011_TC_001, SC011_TC_002
- `ServiceRequest_Transport2WayTest.java` — SC005_TC_001

### SC001_TC_001 — Full Flow: Submit → getAllPendingRequests API → Approve Transport form → processChildApprovedRequest API → Approved (High)

Screen: Account Statement → Service Request → Recent Customer Requests → `process_child_transport`
APIs:

- `getAllPendingRequests`:
  `{{Base_URL}}Financialprocess/getAllPendingRequests/?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373`
- `processChildApprovedRequest`:
  `https://test-admissions.footprintseducation.in/api/parentapp/processChildApprovedRequest?child_id=<child_id>&ckey=9414D96600C5`
- Approve Transport URL (landed on after clicking Approve):
  `https://test-franchise.footprintseducation.in/process_child_transport?request_id=<id>&request_type=Add%20One%20Way%20Transport&child_id=<child_id>&assign_route=1&show_addon=1`

1. Login as Jaydeep Kar → Account Statement → enter child ID → Generate → SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Transport One Way' (Start Transport 1 Way form). Fill From date. Submit → confirm popup → OK. **Expect:**
   toast 'Transport request submitted successfully.'
3. Run `getAllPendingRequests` API. **Expect:** status=ok, type=Transport, status Pending → Processing.
4. Navigate to Recent Customer Requests. Find Transport row, status=Processing. Click Approve. **Expect:** Approve
   Transport form opens (`process_child_transport`).
5. Fill Approve Transport form: Transport Type (Pick-up/Drop/Both), Route, Trip, Location (map, draggable bus icon).
   Submit. **Expect:** approved.
6. Run `processChildApprovedRequest` API. **Expect:** status=ok, Transport addon added to child.
7. Navigate to Recent Customer Requests. Verify Request Status = Approved.

### SC002_TC_001 — Start Transport 1 Way: Submit → Pending (High)

Screen: Account Statement → Service Request
Test Data: Service dropdown 'Transport One Way' (= Start Transport 1 Way) | From date: 2024-09-18 (use a real future
date at run time)

1. Login as Jaydeep Kar → Account Statement → enter child/Admission ID → Generate → SERVICE REQUEST. **Expect:** popup
   opens with Services dropdown.
2. Click dropdown, verify 'Transport One Way' listed. Select it. Verify default view: Service dropdown, From date,
   Submit icon, Close icon.
3. Select 'Start Transport 1 Way'. Verify same default view fields visible.
4. Fill From date. Click Submit. **Expect:** confirmation popup 'Do you want to send Transport request?'
5. Click Cancel on popup. **Expect:** request NOT submitted, popup dismissed.
6. Click Submit again → OK. **Expect:** toast 'Transport request submitted successfully.'
7. Navigate to Recent Customer Requests. Verify Request Type = 'Start Transport 1 Way', Status = Pending.

### SC002_TC_002 — Service Request dropdown: Transport One Way / Transport Two Way options present (High)

Screen: Account Statement → Service Request
Prerequisites: Jaydeep Kar has Account Statement access (`manage_user_rights`). Active child with transport-enabled
center.

1. Login as Jaydeep Kar → Account Statement → enter child ID → Generate → SERVICE REQUEST. **Expect:** popup opens with
   Services dropdown.
2. Click dropdown. Verify 'Transport One Way' option listed.
3. Verify 'Transport Two Way' option listed.
4. Verify other transport options listed (e.g. Stop Transport).
5. Select 'Transport One Way'. Verify From date field appears as mandatory.

### SC005_TC_001 — Start Transport 2 Way: Submit → Pending (High)

Screen: Account Statement → Service Request
Test Data: Service dropdown 'Start Transport 2 Way' | From date: 2024-09-18 (use a real future date at run time) | Child
ID: 24309

1. Navigate to Account Statement. Enter Admission ID 24309. Click SERVICE REQUEST. **Expect:** popup opens.
2. Select 'Start Transport 2 Way'. Verify default view: Service dropdown, From date, Submit icon, Close icon.
3. Fill From date. Click Submit → OK. **Expect:** toast 'Transport request submitted successfully.'
4. Navigate to Recent Customer Requests. Verify Request Type = 'Start Transport 2 Way', Status = Pending.

### SC011_TC_001 — Stop Transport 1 Way: Submit → Pending (High)

Screen: Account Statement → Service Request
Test Data: Child 66730 (Advik Dhingra, ACTIVE) — active Addon: 'One Way Transport - 1750 (₹1750.00)'. Dropdown option (
exact, confirmed live): 'Stop Transport 1 Way'. Form fields: Services dropdown + From date (mandatory, calendar icon) +
Submit button only — no Route/Trip/Location (those are Approve-form-only fields).

1. Login as Jaydeep Kar → `account_statement?child_id=66730`. **Expect:** loads, child ACTIVE.
2. Verify Addons section shows 'One Way Transport - 1750 (₹ 1750.00)' active.
3. Click SERVICE REQUEST (spanner icon). **Expect:** popup opens, header shows 'Advik Dhingra #66730 ACTIVE'.
4. Click Services dropdown. Verify 'Stop Transport 1 Way' listed (note: 'Start Transport 1 Way' NOT listed — child
   already has 1 Way active).
5. Select 'Stop Transport 1 Way'. Verify form shows ONLY Services dropdown + From date + Submit.
6. Click From date calendar icon, select a future date.
7. Click Submit → confirm popup → OK. **Expect:** toast 'Transport request submitted successfully.'
8. Navigate to Recent Customer Requests. Verify Request Type = 'Stop Transport 1 Way', Status = Pending.

### SC011_TC_002 — Stop Transport 1 Way: Full flow → addon removed (High)

Screen: Recent Customer Requests → Account Statement
Prerequisites: Stop Transport 1 Way in Pending status (SC011_TC_001 submitted). Child 66730 (Advik Dhingra).
APIs: same `getAllPendingRequests` / `processChildApprovedRequest` endpoints as SC001_TC_001 (`chid_id=66730` /
`child_id=66730`).

1. Run `getAllPendingRequests` API. **Expect:** status=ok, Stop Transport 1 Way → Processing.
2. Navigate to Recent Customer Requests. Verify Request Type = 'Stop Transport 1 Way', Status = Processing. Click
   Approve. **Expect:** approval confirmed.
3. Run `processChildApprovedRequest` API. **Expect:** status=ok.
4. Navigate to Recent Customer Requests. Verify Request Status = Approved.
5. Navigate to `account_statement?child_id=66730`. Scroll to Addons section.
6. Verify Addons section: 'One Way Transport - 1750' is NO LONGER listed — addon fully removed.

### Existing building blocks to reuse (confirmed from code, no new work needed)

- `pages/Support/Regular_ServiceRequests.java` — **all 4 Transport forms already wired** (built in an earlier session,
  ahead of this automation round): `isStartTransport1WayFormVisible()`/`setT1FromDate()`/`submitStartTransport1Way()`,
  `isStartTransport2WayFormVisible()`/`setT2FromDate()`/`submitStartTransport2Way()`,
  `isStopTransport1WayFormVisible()`/`setST1FromDate()`/`submitStopTransport1Way()`, `isStopTransport2WayFormVisible()`/
  `setST2FromDate()`/`submitStopTransport2Way()`.
- `pages/Support/RecentCustomerRequestsPage.java` — generic helpers already sufficient, no new methods needed:
  `getColumnValueByRequestType(admId, requestType, columnHeader)` (reads Transport rows by Request Type = "Start
  Transport 1 Way" / "Start Transport 2 Way" / "Stop Transport 1 Way"), `getFirstApproveRequestId()` +
  `clickApprove(requestId)` (generic `button.approve[request_id=...]` pattern).
- `pages/Support/AccountStatementPage.java` — `getAddonsText()` already reads the Addons section (
  `<div class="col-md-12"><b>Addons :</b> ...</div>`, "Not Available" when absent) — reused directly via
  `.contains("One Way Transport")` rather than adding a Transport-specific wrapper.
- `utils/APIs.java` — `processChildApprovedRequest`'s physical endpoint (`parentapp/processChildApprovedRequest`, ckey
  `9414D96600C5`) is already generic (`CS_ATTRITION_PROCESS`, reused across Center Shift/Withdraw Child/Corporate Center
  Transfer) — added a thin `processTransportApprovedRequest(childId)` wrapper for naming consistency with other features
  rather than a new endpoint.

### Confirmed live (2026-08-13) — do not re-derive without evidence

- The Recent Customer Requests grid's **"Request Type" column shows the backend action name, not the Services dropdown's
  UI label** — same pattern as Withdraw Child showing "Child Attrition" instead of "Withdraw Child". Confirmed via live
  duplicate-request error text and a direct grid dump for children 72101 and 66730:
    - Services dropdown "Start Transport 1 Way" submission → grid Request Type = **"Add One Way Transport"**
    - Services dropdown "Stop Transport 1 Way" submission → grid Request Type = **"Delete One Way Transport"**
    - (2-way equivalents — "Add Two Way Transport"/"Delete Two Way Transport" — extrapolated from this naming pattern,
      NOT yet independently confirmed live.)
- The Services dropdown option text itself — `"Start Transport 1 Way"` / `"Stop Transport 1 Way"` — IS correct as passed
  to `selectServiceType()`; both confirmed live (selection succeeded and revealed the expected form each time).
- `getAllPendingRequests` param **`chid_id` (not `child_id`) is confirmed correct for Transport** — user-supplied a
  live-working example (`chid_id=72101`), unlike Extended Daycare/Withdraw Child where `chid_id` was silently wrong. Do
  not "fix" this to `child_id` without new evidence.
- `processChildApprovedRequest` ckey `9414D96600C5` confirmed working for Transport (user-supplied live example,
  `child_id=72428`).
- Duplicate-request submissions are rejected client-side with
  `"Invalid Request: Request to '<Add/Delete One/Two Way Transport>' is already pending with us - requested by <name> on <date>, it will be processed soon."` —
  tests treat this as an acceptable pre-existing-Pending state (not a hard failure) when resubmitting against a child
  that already has one pending, mirroring the idempotency pattern used in Corporate Transfer's cancel test.
- Child **72428** (originally supplied for the full E2E flow, SC001_TC_001) has **no Transport option in its Services
  dropdown at all** — confirmed live (dropdown only listed Center Shift/Child Pause/Extended Daycare/Program
  Change/Start Time Extension/Withdraw Child, 0 existing grid rows) — it is not Transport-enabled at its center. A
  different, Transport-enabled, unused child ID is needed for SC001_TC_001.
- Child **24309** (spec's own child for SC005_TC_001, Start Transport 2 Way) is now in **ATTRITION** status — confirmed
  live: the whole Services dropdown renders `disabled`, and the panel header shows "Sanidhya Rajan #24309 ATTRITION —
  You can't access services of attrition child." A different, ACTIVE, 2-way-transport-enabled child ID is needed for
  SC005_TC_001.
- `processTransportApprovedRequest` (processChildApprovedRequest) has the **same future-dated-WEF silent no-op behavior
  confirmed for Withdraw Child**: calling it for a request whose WEF/From date is not today returns HTTP 200 with a
  `null` body and leaves the grid status stuck at "Processing" rather than "Approved". Confirmed live for child 66730's
  Stop Transport 1 Way request (WEF was 2026-08-20 from an earlier test run using a +7-day future date — the process
  call no-op'd, status stayed "Processing"). `tc004_stopSubmitPending` was changed to submit with WEF=today (
  `LocalDate.now()`) instead of a future date so `tc005_stopFullFlow` can observe the terminal Approved state within the
  same run — but this fix could not be verified live yet because child 66730 already has that stale future-dated Pending
  request blocking a fresh same-day resubmission (duplicate-request check blocks ANY second "Delete One Way Transport"
  submission regardless of date, not just same-date ones).
- Confirmed live: the generic `button.approve[request_id=...]` pattern (already used by other service types) **does work
  for "Delete One/Two Way Transport" (Stop) rows** — `clickApprove(166255)` succeeded and the grid moved from "
  Processing" (post-`getAllPendingRequests`) toward the Approve action, before getting stuck at "Processing" for the
  unrelated future-dated-WEF reason above.
- Confirmed live (user-supplied element): **"Add One/Two Way Transport" (Start) rows use a plain `<a>` link, NOT the
  generic button.approve** —
  `<a href="process_child_transport?request_id=166259&request_type=Add One Way Transport&child_id=73041&center_id=210&assign_route=1&show_addon=1" class="btn btn-primary btn-xs label">Approve</a>`.
  Clicking it navigates the current tab straight to the Approve Transport form. Added
  `RecentCustomerRequestsPage.isTransportApproveLinkVisible()`/`clickTransportApprove()` (matches
  `a[href*='process_child_transport']`) for this; `tc001_fullFlow` now uses these instead of the generic
  `getFirstApproveRequestId()`/`clickApprove()`, which found nothing for this row type (confirmed live — that was the
  exact prior failure).
- Confirmed live (user): clicking Approve does navigate to
  `process_child_transport?request_id=<id>&request_type=Add%20One%20Way%20Transport&child_id=<id>&center_id=<id>&assign_route=1&show_addon=1`,
  matching the spec's own URL pattern exactly (plus an extra `center_id` param not mentioned in the spec).

### 2-Way Transport (`ServiceRequest_Transport2WayTest.java`) — ALL 3 tests CONFIRMED PASSING (2026-08-14)

Running as **Nidhi Chaturvedi** (see "Transport role / acting user" section below), a full clean run with fresh children
passed 3/3: `tc001_submitPending` (SC005_TC_001, child 72089) — Start Transport 2 Way → Pending;
`tc002_stopSubmitPending` (SC012_TC_001, child 50875, "Two Way Transport - 2000" addon) → Pending;
`tc003_stopFullFlow` (SC012_TC_002) → submit → getAllPendingRequests → generic `button.approve` click →
`processChildApprovedRequest` → Approved → addon removed ("Not Available"). This also confirms "Add Two Way Transport"/"
Delete Two Way Transport" as the real grid Request Type strings (previously just extrapolated from the 1-Way naming) —
the ONE prior failure using this text was a cross-type conflict (child already had an unrelated One Way request
pending), not a wrong string.

- **Fixed bug**: this class shares a single browser tab (unlike the 1-Way class's two-tab design) — any step that
  navigates to Recent Customer Requests (grid checks via `getColumnValueByRequestType`) leaves the driver there, and
  `AccountStatementPage.generateAccountStatement()` doesn't navigate itself (assumes it's already on Account Statement).
  Both `@BeforeMethod` and the end of `tc003` now call `navigations.goToAccountStatement()` explicitly before any
  Account Statement interaction — omitting this caused `NoSuchElementException` on `#frm_child_id` twice before the fix.
- **Cross-type conflict discovered**: a child with ANY existing pending/processing transport request (of either
  direction) blocks a NEW submission of the other direction too — e.g. child 72089's pre-existing "Add One Way
  Transport" blocked a "Start Transport 2 Way" submission with the same generic "already pending" error, even though no
  Two Way request had ever been created. `tc001_submitPending`'s idempotency check was tightened to only treat "already
  pending" as an acceptable duplicate when the error text itself mentions "two way" — otherwise it now fails loudly with
  a message pointing at the real cause (different child needed), instead of silently misreading it as a same-type dup.

### Transport role / acting user — switched from Jaydeep Kar to Nidhi Chaturvedi (2026-08-14)

Per explicit request, added a new row to `testData/input_UserRights.xlsx` (`UserRights` sheet):
`Nidhi Chaturvedi | Transport | Raise_Support_Request` (RightTitle is descriptive only — `getUserForScreen()` matches on
ScreenName, column B, exact case-insensitive string match, not RightTitle). Both `ServiceRequest_Transport1WayTest.java`
and `ServiceRequest_Transport2WayTest.java` now call `getUserForScreen("Transport")` instead of
`getUserForScreen("Account Statement")`, switching the acting user for ALL 8 Transport tests (both classes) from Jaydeep
Kar to Nidhi Chaturvedi. Confirmed live: Nidhi logs in as "Centers Head" role and has full, identical access to
submit/approve Transport requests via Account Statement → Service Request — no UI differences observed vs. Jaydeep for
any step exercised so far (Start/Stop submit, Approve Transport form, generic button.approve). Every child ID used under
Jaydeep in earlier sessions is unaffected by this switch (user identity doesn't change which children exist/their state)
but IS now stale from repeated Jaydeep-era test runs — fresh children were re-supplied for the 2-Way class's first clean
run under Nidhi (72089, 50875) with full success; the 1-Way class has not yet been re-run end-to-end under Nidhi.

### Approve Transport form (

`process_child_transport`) — CONFIRMED live end-to-end, INCLUDING by the automated test itself

Child 73041: user walked the real form manually, then re-ran `processChildApprovedRequest`, which actually processed
it — Account Statement showed `Addons: One Way Transport - 300 (₹300.00)` and a generated invoice (`PI/967621`). Child
72114: `tc001_fullFlow` then reproduced the entire chain **unattended** (submit → getAllPendingRequests → Approve link →
fill form → Approve → native confirm → processChildApprovedRequest → Approved) — confirmed PASSING. This is NOT the
spec's own "Transport Type / Route / Trip / Location map" description — there is no separate Trip field. Confirmed real
field sequence, implemented in `ApproveTransportPage.java`:

1. `#transport_type` (select) — options `--Choose--`/`Pick-Up`/`Drop` (exact text, note the hyphen+capital-U)
2. `#addon` (select) — pricing plan, e.g. "One Way Transport - 300"
3. `#pickup_route_id` (select, shown for Pick-Up; `#drop_route_id` assumed analogous for Drop, unconfirmed — user has
   only explained the Pick-Up path so far) — options inside `<optgroup label="Active">`
4. `#ptid` (readonly input, `pickatime.js` widget) — Pickup Time
5. `#map_canvas_route_autocomplete` (Google Places autocomplete) — type an address, click the `.pac-item` suggestion,
   double-click the map's bus-icon marker (`img[usemap^='#gmimap']`), then click `#send_map_sms` ("Set Pickup/Drop
   Location") — confirmed live result: "Pickup Location: ORYON BUSINESS INDIA, PTV LTD, Bhangel, Sector - 106, Noida..."
   populated correctly from address "Amrapali Zodia sector 120 Noida". Renders asynchronously after Route selection —
   can take several seconds, so a generous wait is used rather than a fixed sleep.
6. `#transport_date` (WEF Date) — a `pickadate.js` widget (`class="picker__input"`), same widget family that silently
   corrupted data via raw JS injection for Withdraw Child's `attrition_date` — so this is driven via
   `Regular_ServiceRequests`' proven `openCalendarFor()`/`clickCalendarDay()` real-widget-click helpers, defaulting to
   today's date
7. `#approve_transport` (button, `class="approve btn btn-primary"`, `request_id`/`request_type` as plain attributes) —
   clicking it triggers a **native `confirm()`**: "Changes will be applicable today onwards, sure want to update status
   of transport request?" — must be accepted or every subsequent WebDriver call throws `UnhandledAlertException` (
   confirmed live via user screenshot)

### Confirmed live — submit-form date quirks

- The **Start Transport 1 Way submit form** (Service Request popup, "From date") rejects `today` as a "past date" —
  `"Cannot make request in past date"` — unlike most other features' submit forms. A genuine future date is required
  here (`futureDate()`, +7 days).
- This submit-form date is **independent** of the Approve Transport form's own separate `#transport_date` (WEF Date)
  field — the latter is what actually gates `processChildApprovedRequest`, and gets set to today regardless of whatever
  future date was used at submit time.
- The test server's clock was reset mid-session by the user after a date-mismatch caused every future-dated submission
  to be rejected as "past" (the server's clock had drifted ahead of the local automation machine's clock) — if this
  recurs, check the server date before assuming a code regression.

### Open items to confirm before/while automating

- ~~Whether #drop_route_id really exists~~ — resolved via user's full-page element dump: it exists (
  `id="drop_route_id"`, same "Select Route" + `<optgroup>` structure as pickup). The Drop path's other fields are also
  now confirmed from that same dump, though not yet exercised live end-to-end: `#dtid` (Drop Time — NOT "drop_ptid",
  class `dropatime-format`), `#dropRouteinfo_name`/`#dropRouteinfo_lat`/`#dropRouteinfo_long` (Drop location, mirroring
  `pickupRouteinfo_*`), `#drop_point` (mirroring `pickup_point`). `ApproveTransportPage.selectRoute()` already picks
  between `pickup_route_id`/`drop_route_id` by transport type value — Drop-path Time/Location methods (`setPickupTime`/
  `enterLocationAddress` are currently Pickup-only-named) would need Drop-specific variants (or parameterizing) before
  Drop can be automated.
- The dump's two `icon-pin-alt` elements are the manual trigger for the location picker — confirmed live:
  `<a data-toggle="modal" href="#" data-href="location_picker?pop=yes&child_id=<id>&route_type=pickup&hd_inp=pickupRouteinfo&CH_TASK_MODE=true" class="open_modal_window" data-target="#select_route_modal">`
  wrapping the pin icon + the readonly `pickupRouteinfo_name` field. Clicking it AJAX-loads `location_picker?...` into a
  Bootstrap modal (`#select_route_modal`) containing the map/autocomplete. `ApproveTransportPage` never clicks this —
  yet `tc001_fullFlow` still found and used `#map_canvas_route_autocomplete` successfully — so selecting the Route
  dropdown (`#pickup_route_id`) itself must auto-trigger the same modal/map programmatically (likely a JS onchange
  handler), making an explicit pin click redundant for the Pickup path. Worth keeping in mind for Drop-path automation
  later: the analogous `data-href="location_picker?...&route_type=drop&hd_inp=dropRouteinfo..."` trigger presumably
  exists for `#drop_route_id` too, unconfirmed.
- Confirmed by user: for **Two Way Transport**, the Approve form's URL/screen requires filling **both** Pick-Up and Drop
  sections (matches the spec's SC006_TC_001) — not yet relevant to this round's automated scope (SC005_TC_001 is
  submit-only, no approve step), but will be needed if 2-way approve is automated later.
- Whether the **Stop Transport submit form** also rejects today's date the same way Start's does — unconfirmed; matters
  for `tc004_stopSubmitPending`/`tc005_stopFullFlow`, which currently submit with today's date specifically so `tc005`
  can observe the terminal Approved state (see Stop Transport notes above) — if Stop's submit form also rejects today,
  that design needs revisiting.
- A fresh, unused, Transport-enabled child ID with **no existing pending Transport request** for **SC002_TC_002** (
  `DROPDOWN_CHECK_CHILD_ID`, currently a TODO placeholder) — needed so both "Start Transport 1 Way" and "Start Transport
  2 Way" are still offered in the dropdown.
- A fresh, ACTIVE (not Attrition), 2-way-transport-enabled child ID for **SC005_TC_001** — 24309 (the spec's own
  example) is now Attrition and cannot be used.
- Either a fresh child with an active "One Way Transport" addon (to re-verify SC011_TC_002's full approve flow cleanly),
  or waiting until 2026-08-20 for child 66730's existing stale Pending request to reach its own WEF date, to confirm the
  `tc004`-now-submits-with-today's-date fix actually lets `tc005_stopFullFlow` reach "Approved".

## Requirements — Cancel Registration (`CancelRegistration_testcases.java`)

Screen: Account Statement (`account_statement?child_id=<id>`), CANCEL REGISTRATION button. Stays on default Rakesh login
throughout — **no user switch** for this feature (per explicit user instruction). Page-object methods go into the
existing `AccountStatementPage.java` (additive only). Tests go into a new `CancelRegistration_testcases.java` class.

### Confirmed live (from screenshots, 2026-09-09) — do not re-derive without evidence

- Button (visible for an Active child):
  `<a class="popdown_xl_large btn btn-xs text-muted has-text reg-padding" href="cancel_registration?pop=yes&child_id=<id>" title=""><span class="btn btn-xs btn-danger" href="#">Cancel Registration</span></a>`.
- Clicking it opens a **modal** ("Cancel Registration" header, blue banner) with:
    - `Child ID :` — pre-filled input (e.g. `73014`), appears read-only/reference-only.
    - `Reason :` — textarea, placeholder "Enter your comments here".
    - Submit button: "Cancel Registration →".
- Clicking submit triggers a **genuine native `window.confirm()`**: *"Are you sure you want to cancel registration?"* (
  Cancel/OK) — must be accepted via WebDriver `Alert` handling, not inline modal text.
- On OK, the SAME modal shows an inline green success banner (checkmark icon): **"Cancelling Registration Processed"** —
  form fields (Child ID/Reason) remain visible underneath, Reason cleared back to placeholder.
- After closing the modal and the Account Statement page reflects the change:
    - Child status label next to name/ID changes to **`(ATTRITION)`** in red (was plain, e.g. `(JOINING PIPELINE)`
      before, for a not-yet-active-billing child).
    - The red **CANCEL REGISTRATION** button is **replaced by a new red "REFUND WELCOME KIT"** button.
    - **`Billing Cancel Date : <today's date>`** (e.g. "09 Sep, 2026") appears as a new red line under BOTH the Monthly
      Subscription and Yearly Subscription panels.
    - In the ledger/invoice table, at least one invoice line switches to **voided**: its header row gains
      `(Voided on <today's date>)` in red and the row is highlighted (light red/pink background) — confirmed on an
      invoice dated 26 August, 2026 ("Read-O-Stick"/"Welcome Kit" line items). An older invoice (31 July, 2026, "
      Registration Fee") was **NOT** voided — so voiding appears selective (which invoices get voided is not yet fully
      understood — needs more investigation/confirmation, don't assume "all invoices voided").

### Test cases (source: `TC_Account Statement Scripts_09Sep.xlsx`, sheet `Account Statement Page`, Scenario ID

`SC_011`, Module "Cancel Registration")

- **SC_011_TC_001** (Medium) — Verify the option for "Cancel Registration" **before joining** (to close account) is
  available. Steps: open Account Statement URL → click Cancel Registration button. (No explicit expected-result text in
  sheet — implies button is visible/clickable and opens the popup for a not-yet-joined/"Joining Pipeline" child,
  matching the pre-cancellation screenshot's `(JOINING PIPELINE)` label.)
- **SC_011_TC_002** (Medium) — Verify Cancel Registration popup modal **default view**: title "Cancel Registration",
  Close icon, Child ID field, Reason input field, Cancel Registration button. Write as ONE consolidated step
  per [[feedback_default_view_testcase_format]].
- **SC_011_TC_003** (Medium) — Full submit flow: fill Child ID & Reason → Cancel Registration button → native confirm
  popup ("Are you sure you want to cancel registration?", OK/Cancel) → OK → success toast "Cancelling Registration
  Processed successfully" (sheet's wording — screenshot showed inline banner "Cancelling Registration Processed", no
  separate toast observed; confirm which is real when automating). Expected result per sheet: child becomes *
  *`(Attrition - Closed)`** with **Attrition Reason = "Registration Cancelled"** (screenshot instead showed plain
  `(ATTRITION)` — the "- Closed" qualifier and the Attrition Reason field/location are not yet confirmed live, need to
  check Child Info/Child History for where "Attrition Reason" actually renders).
- **SC_011_TC_004** (Medium) — Verify refund of Registration Fee on the **Finance side**: open `refund_list`, verify the
  voided invoice(s) appear there for refund, "Finance will refund the amount" (manual finance step, not itself
  automatable — automate only the verification that the entry appears on `refund_list`).
- **SC_011_TC_005** (Medium) — Cancel registration **BEFORE Joining Date** → automatic refund. Prerequisite: Cancel
  Registration button stays visible until the day after Joining Date. Expected: Partial payment at admission → refund of
  ₹5000 (Registration Fee) only; Full payment at admission → refund of Registration Fee + Annual Fee (v1) + Tuition Fee.
- **SC_011_TC_006** (Medium) — Cancel registration **AFTER Joining Date** (still within the "next day" grace window).
  Same steps/expected-result text as TC_005 in the sheet (appears to be a copy in the source doc — the real
  distinguishing behavior between before/after Joining Date is not yet spelled out beyond the button-visibility window;
  confirm with user if the refund amounts genuinely differ for the "after" case, or if this needs re-deriving live).

### Open items to confirm before automating

- Exact rule for which invoices get voided vs. left alone (all unpaid? only future-dated/subscription invoices? specific
  booking heads like Welcome Kit?) — only one before/after example seen so far (relevant to TC_004/005/006's
  refund-amount checks).
- Whether "Reason" is actually mandatory (server-side) or just a UI hint — not yet tested with an empty Reason.
- What happens if Cancel Registration is attempted on a child that's already Attrition/has no such button (expect:
  button absent, mirrors the "no button rendered" pattern already seen in Admission Migration for
  paused/onsite-restricted children).
- Where "Attrition Reason" and the "- Closed" qualifier (TC_003's expected result) actually render — not yet seen live (
  screenshot walkthrough only showed plain `(ATTRITION)`, no "- Closed"/reason visible on Account Statement itself).
- TC_005/TC_006's exact partial-vs-full-payment refund-amount verification depth — these are deep financial checks (
  refund amounts by payment history); need to confirm how much of this is UI-verifiable on Account Statement/refund_list
  vs. requiring finance-side/API confirmation, similar to other deferred financial checks in this project (e.g.
  Corporate Transfer's invoice proration).
- Concrete fresh child ID(s), not yet mapped to specific scenarios above: **73705, 73831, 74174, 73859**. User will
  explicitly state which child maps to which scenario (before/after Joining Date, partial/full payment) — do not
  guess/derive this mapping from live state. 73014 (used for the walkthrough) is now consumed.

### Final plan — confirmed by user, ready to automate (2026-09-09)

- Child assignment (Regular Admission): **73705** → TC_001 + TC_002 (view-only, no submit, reusable); **73831** →
  TC_003 (full submit → Attrition); **74174** → TC_005 (before Joining Date); **73859** → TC_006 (after Joining Date).
  TC_004 (`refund_list`) just checks whichever child got voided in the above.
- After each test that performs a cancellation, **print** the following to console/log:
  `Current Child Status: Attrition`, `Billing Cancel Date: <date>` (e.g. "09 Sep, 2026"), `Voided Invoice IDs: <ref>` (
  e.g. "PI/985388").
- Starting automation now with Regular Admission cases (TC_001–006); Corporate and Summer Camp admission-type coverage
  for this same feature to be added later as new methods once those child IDs are supplied.

### Scope decision (confirmed by user, 2026-09-09)

- **TC_004 (refund_list) and TC_005/TC_006 (refund amounts)**: UI-verify only — confirm the voided invoice/refund entry
  appears on `refund_list` and that Account Statement shows the expected refund line item(s). Do NOT attempt to validate
  exact partial-vs-full-payment refund math (₹5000-only vs. registration+annual+tuition) — that level of financial
  verification is out of scope this round.
- **Child ID → scenario mapping**: user will explicitly state which of the 4 supplied child IDs (73705, 73831, 74174,
    73859) maps to which scenario/state (pre-Joining-Date, post-Joining-Date, partial payment, full payment) — do not
           infer this from live child state.
- **TC_006 (after Joining Date) child IDs updated (2026-09-09)**: user supplied 74173 (primary), 73735, 73748 (
  backups) — supersedes the earlier 73859 assignment for this scenario.
- **Confirmed live DOM (2026-09-09)** — Reason field:
  `<textarea rows="1" id="cancel_reason" cols="4" name="cancel_reason" class="form-control required" placeholder="Enter your comments here"></textarea>`.
- **Confirmed live DOM (2026-09-09)** — Submit button:
  `<button type="button" class="btn btn-primary" id="cancel_registration" data-loading-text="<i class='fa fa-spinner fa-spin '></i> Processing" value="cancel_registration">Cancel Registration <i class="icon-arrow-right14 position-right"></i></button>`.

### Status (2026-09-09): automation built, TC_001/TC_002 confirmed passing; TC_003/005/006 need fresh children for a clean re-verify

- Code lives in `AccountStatementPage.java` (additive methods only) + new `CancelRegistration_testcases.java` +
  `CancelRegistrationtestng.xml`, following this file's usual iterate-against-live-app process.
- **A stray `<div id="popdown-opacity">` overlay** (the popdown_xl_large modal family's own overlay — distinct from
  bootstrap's `.modal-backdrop` already handled elsewhere) can be left behind after opening/closing the Cancel
  Registration popup, blocking top-nav menu clicks. Fixed with a targeted JS hide in this test class's own
  `@BeforeMethod`/`@AfterMethod` (not in shared `Navigations.java`, to avoid touching code other tests depend on).
- **`getChildStatusLabel()`/`getBillingCancelDateText()` fixed**: an absolute-position xpath to the containing
  `<legend>` proved fragile (didn't match for at least one child/page state), and a `contains(text(),...)` match on "
  Billing Cancel Date" only ever returns the label's own text node, missing the sibling text node with the actual date (
  same trap already documented for `AdmissionMigrationRequest`'s banner text). Both now scan the full page body text
  with regex instead — matches this file's existing fallback style in `getMonthlyPlanAmount()`.
- **REFUND WELCOME KIT button and voided-invoice rows are data-dependent, not guaranteed on every cancellation** —
  confirmed live on child 73831 (a Joining-Pipeline child with no invoices/Welcome-Kit charge yet): after successful
  cancellation it went to ATTRITION correctly, but showed NO Refund Welcome Kit button and NO voided invoice row,
  because there was nothing to void/refund. The test now treats the refund-button check as informational only (logged,
  not asserted) — only `isChildStatusAttrition()` is a hard requirement, consistent with the "UI-verify only" scope
  decision.
- **Voided-invoice reading now scrolls to the bottom of the page first** (`getVoidedInvoiceReferences()`) per user's
  request, since the ledger table can render below the fold.
- **First full run (before the above fixes) actually succeeded in submitting real cancellations** for children **73831
  ** (TC_003), **74174** (TC_005), and **74173** (TC_006) — all three are now genuinely consumed (Attrition), even
  though the run initially reported failures (the failures were in the verification code, not the submission).

### Final status (2026-09-09): all 6 scenarios (SC_011_TC_001–006) CONFIRMED PASSING

Re-verified individually with fresh children after the fixes above:

- TC_001 + TC_002 — child **73705** (view-only, not consumed).
- TC_003 (full flow) — child **73705** reused for the actual submit (consumed → Attrition). ⚠ Note:
  `CR_FULL_FLOW_CHILD_ID` and `CR_VIEW_CHILD_ID` currently both point at 73705 in code — a fresh view-only child is
  needed before TC_001/TC_002 can be cleanly re-run again.
- TC_004 (refund_list) — passed, depends on TC_003's captured voided invoice ref.
- TC_005 (before Joining Date) — child **73832** ("JOINING - CLASS APPROVAL" status confirmed live) — passed; no
  invoices/Welcome Kit existed yet, so voided-refs list was empty and refund button absent (expected/informational per
  the data-dependent note above).
- TC_006 (after Joining Date) — child **73860** (user-supplied) had NO Cancel Registration button at all (likely outside
  the "next day of Joining Date" eligibility window) — fell back to backup **73735** (from the earlier-supplied
  74173/73735/73748 set), which passed with a full real example: 4 invoices voided (PI/987441, PI/987442, PI/987443,
  PI/987444).
- All children used in this final pass are now consumed (Attrition): 73705, 73832, 73735. Fresh Regular Admission
  children needed for any future clean re-run.

### Bonus — mixed admission-type verification (2026-09-10)

Two `@Test` methods added (`bonus_cancelRegistrationMixedAdmissionType1`/`2`, priorities 7-8) reusing the same shared
flow, per user's request to confirm Cancel Registration behaves identically across admission types (
Regular/Corporate/Summer Camp/etc.) — no new page-object code needed since the feature is type-agnostic.

- Child **71141** ("SIA BISHT", JOINING - CLASS APPROVAL): **PASSED** — full flow, Attrition confirmed, Billing Cancel
  Date "10 Sep, 2026". Now consumed.
- Child **71990** ("ADVIK S", also JOINING - CLASS APPROVAL): Cancel Registration button **genuinely absent** —
  confirmed via full body-text dump (not a locator bug). **Root cause confirmed by user (2026-09-10): this child's real
  Joining Date is 07 Sep — already past the eligibility window**, so the button correctly disappears. Not
  admission-type-related. (The "JUL-2027 TO JUL-2027" Account Statement date-range default seen during investigation was
  an unrelated red herring — not the child's actual Joining Date.)
- **Confirmed exact eligibility window (2026-09-10): the Cancel Registration button is displayed from 3 days BEFORE the
  child's Joining Date through 1 day AFTER it** — supersedes the SC_011_TC_005/TC_006 prerequisite's vaguer "till next
  day of Joining Date" wording with the precise window.
- `performCancelRegistrationAndVerify()` (the shared flow used by every `@Test` in this class) now checks button
  visibility first: if absent for any child, it prints/logs this exact eligibility-window message and returns
  gracefully (skip) instead of throwing a timeout exception — so a missing button reads as "outside eligibility window"
  rather than a false failure, for any test case, not just the mixed-admission-type bonus ones.

### Full-suite run (2026-09-10): 6/8 passed — the 2 failures are stale test data, not code bugs

`mvn test -Dsurefire.suiteXmlFiles=CancelRegistrationtestng.xml` (all 8 tests together): TC_003, TC_004, TC_005, TC_006,
and both bonus tests passed (TC_003/005/006 gracefully skipped via the new eligibility-window message, since their
children were already consumed from prior individual runs). **TC_001 and TC_002 failed** because `CR_VIEW_CHILD_ID` and
`CR_FULL_FLOW_CHILD_ID` both pointed at the same id (73705) — TC_003's real submission consumed it, so TC_001/TC_002's
hard assertion that the button IS visible correctly failed (they test the happy path, so they can't use the same
graceful-skip pattern). Fixed the constants to be clearly documented as needing to stay separate.

- **For one fully clean full-suite run, 5 fresh Regular/mixed-type children are needed** (6 if also exercising bonus2
  instead of letting it skip): one each for `CR_VIEW_CHILD_ID` (TC_001/002, stays reusable — never submitted),
  `CR_FULL_FLOW_CHILD_ID` (TC_003, consumed), `CR_BEFORE_JOINING_CHILD_ID` (TC_005, consumed),
  `CR_AFTER_JOINING_CHILD_ID` (TC_006, consumed), `CR_MIXED_ADMISSION_CHILD_ID_1` (bonus1, non-Regular, consumed), and
  optionally `CR_MIXED_ADMISSION_CHILD_ID_2` (bonus2, non-Regular, consumed) — all must be within the eligibility
  window (3 days before to 1 day after Joining Date) to actually exercise the flow rather than skip.

### Full-suite re-run (2026-09-22): 8/8 passed — first genuinely clean combined run

`CR_VIEW_CHILD_ID`/`CR_FULL_FLOW_CHILD_ID` (73705), `CR_BEFORE_JOINING_CHILD_ID` (73832), `CR_AFTER_JOINING_CHILD_ID` (

73735) were all consumed from the 2026-09-10 run — refreshed to fresh children **74167** (view + full-flow), **74227** (
       before Joining Date), **74189** (after Joining Date). Note: `CR_VIEW_CHILD_ID`/`CR_FULL_FLOW_CHILD_ID` still both
       point
       at the same id (74167) — same caveat as before, a dedicated fresh view-only child is needed to cleanly re-run
       TC_001/TC_002 again without re-consuming the full-flow child. All 8 tests (SC_011_TC_001–006 + both bonus
       mixed-admission-type tests) passed in one `mvn test -Dsurefire.suiteXmlFiles=CancelRegistrationtestng.xml` run.
       Children
       74167/74227/74189 are now consumed (Attrition) — fresh ones needed for the next clean run.

- User clarified (2026-09-09): each of the 4 children will be in one of two admission statuses — **"Joining Pipeline"**
  or **"Joining Class Approval"** — not yet fully Active. **Do NOT track/verify a child's pre-cancellation status as a
  test precondition** — the only status assertion needed is post-cancellation: child status changes to **Attrition**.
- The 4 supplied IDs (73705, 73831, 74174, 73859) are all **Regular Admission** children. User will separately supply *
  *Corporate** and **Summer Camp** admission child IDs later, for additional coverage of this same feature across
  admission types.

### NEW enhancement: access right `Cancel_Registration` (received 2026-10-15, NOT yet automated)

A new user right **`Cancel_Registration`** ("Cancel child registration") now controls the Cancel Registration action
on Account Statement. Until now the feature had no right check, and the tests ran as Rakesh with no user switch.

**Rule (user, 2026-10-15):** only a user who has `Cancel_Registration` can see the Cancel Registration button on
Account Statement. A user without the right does not see the button at all.

**Status (2026-10-15, server date Oct 15):**

- **Jaydeep Kar has the right** (user). Excel row added: `Jaydeep Kar | Cancel Registration | Cancel_Registration`.
- Rakesh (user_id 357) had NO right. Then `tc008`'s first version **assigned `Cancel_Registration` to Rakesh**, so he has
  it now. Remove it before rerunning `tc007`.
- Assign UI (`manage_user_rights?user_id=<id>`, user-supplied DOM): search `#assign-search` → checkbox
  `input.right-checkbox[value=Cancel_Registration]` → `#right_updated_comment` (Request Reference, required) →
  `#update_right` "Assign Right". No alert was observed. Helpers: `UserRightsPage.assignRight()` / `hasRight()`.
- `sc011_tc007_buttonHiddenWithoutCancelRegistrationRight` (Rakesh, child **73856**) passed, but it is INCONCLUSIVE:
  73856's Joining Date is Oct 5, 2026, so it is outside the eligibility window at Oct 15. The button also stayed hidden
  after Rakesh got the right.
- **`sc011_tc008_userWithCancelRegistrationRightCanCancel` PASSED (2026-10-15)** as Jaydeep on child **74181** (Joining
  Date Oct 14, 2026, inside the window):
  - The button was visible, and the confirm read "Are you sure you want to cancel registration?".
  - Status → **ATTRITION**; voided invoice **PI/1002474**.
  - "Billing Cancel Date" was not found on the page this time (logged blank, not asserted).
  - 74181 is now CONSUMED. The default for `-DcrRightChildId` is 74181; pass a fresh in-window child for the next run.

- **🐛 BUG (2026-10-15, reported to dev by the user): the right is NOT enforced.** `sc011_tc007` as Rakesh on child
  **74246** (JOINING PIPELINE, Joining Date Oct 14 — inside the window) FAILS: the button is visible although Rakesh has
  no `Cancel_Registration` right.
  - The right was removed via Current Rights "×" (`button.previlige_right_id` in `#assigned_rights`; confirm "Are you
    sure you want to Delete Right ?") and by the user. The `#rights-search` check shows "No matching records found".
  - Reproduced on fresh logins; the user also sees it manually.
  - Write-up: `~/Desktop/QA_Documents/Bug Evidence/Bug_CancelRegistration_RightNotEnforced.md` + screenshots in
    `CancelRegistration_RightNotEnforced/`.
  - `tc007` asserts the spec'd behaviour and keeps failing until fixed. 74246 is read-only there (not consumed).
  - `tc007` removes the right from Rakesh first (`UserRightsPage.removeRight()`) if he has it.

- **Test cases (2026-10-15):** SC_011_TC_013–020 added to
  `~/Desktop/QA_Documents/Testcases Scripts/TC_CancelRegistration.xlsx`; the format was verified unchanged.
  - Iteration 1: TC_013 Pass, TC_014 Fail (the bug), TC_016 Fail (same cause), TC_018 Pass, TC_020 Pass.
  - TC_015 (direct URL), TC_017 step 2 and TC_019 (Days validity) are Not executed.
  - The Document History entry and QueryLogs Q1 were added.

- **FIXED by dev — retest PASSED (2026-10-15):**
  - `sc011_tc007` (Rakesh, no right, 74246): the button is now hidden.
  - Direct URL `cancel_registration?pop=yes&child_id=74246` as Rakesh without the right shows **"Sorry! You are not
    authorise to access this screen."** with no form.
  - Assign the right to Rakesh, then a fresh login: the button is visible. The right was removed again afterwards, so
    Rakesh is back to no right.
  - Iteration 2 is filled in the workbook, and QueryLogs Q1 is Closed.

Not yet confirmed (ask the user / verify live before automating):

- Whether Rakesh has the right by default. The existing 8 tests depend on this.
- Whether the server also blocks a direct `cancel_registration?pop=yes&child_id=<id>` URL for a user without the right.

## Requirements — Admission Migration (Regular ↔ Corporate) (`AdmissionMigration_Testcases.java`)

Source: `Admission Migration.docx` (flow/screenshots) + `TC_Account Statement Scripts.xlsx` (sheet
`Account Statement Page`, `SC_016_TC_001-020` = Regular→Corporate, `SC_017_TC_001-023` = Corporate→Regular). 12 of the
43 cases automated this round, plus 2 bonus scenarios discovered while sourcing test data (14 total) —
modal-default-view checks (`TC_001` both directions) explicitly excluded per user; the remainder are
backend/financial/data-integrity checks (invoice proration, security-fee refunds, invite-table rows, transport-data
copying, email notifications) not UI-testable this round, same category deferred for other features.

Screen: Account Statement → "MIGRATE FROM REGULAR TO CORPORATE" / "MIGRATE FROM CORPORATE TO REGULAR" buttons, both
opening the same "Admission Migration" modal with a direction-dependent field set. User: Varsha Jha (
`getUserForScreen("Corporate Account Statement")` — same screen key as Corporate Transfer; confirmed live she performs
both directions).

### Confirmed live — modal structure (do not re-derive without evidence)

- Modal container: `id="modal_form_admission_migration"`, header `<h5 class="modal-title">Admission Migration</h5>` (an
  h5, not h4).
- **Regular→Corporate Add modal fields**: Corporate Tie Up = `select#offer_id`, Center = `select#center_id` (
  pre-filled/effectively read-only — only one option), Program = `select#program` (starts with only a "Please Select"
  placeholder — **populates dynamically only after Corporate Tie Up is selected**, via its `onchange`), Employee Code =
  `input#employee_code`, Employee Email = `input#employee_email`, Employer = `input#employer`. A hidden `#breakup`
  textarea (class `copay-breakup`) appears only for Co-Pay tie-ups.
- **Corporate→Regular Add modal fields**: just Center (`select#center_id`) + Program (`select#program`) — no Tie
  Up/Employee fields, confirmed via live DOM dump.
- Submit button: `button.btn-model-migrate` (text "Add Migration Request") — clicking it fires a **genuine
  native `window.confirm()`** ("Are you sure you want to submit the migration request?"), unlike the Approve step below.
- Approve button (text "Approve Migration Request") — the modal's own "Are you sure you want to approve Admission
  Migration?" is **inline modal text, NOT a native confirm()** — no alert-handling needed, just the click.
- Both directions' entry/approve buttons funnel through the same JS handlers regardless of direction:
  `migrateAdmissionRegularCorporate(childId)` (Add) / `approveMigrateAdmissionRegularCorporate(childId)` (Approve) —
  confirmed via anchor `href` dumps.
- Exact live success toasts (both directions use the same wording): *"ADMISSION MIGRATION REQUEST SAVED SUCCESSFULLY.
  REQUEST WILL BE PROCESSED AT MONTH END ONCE APPROVED"* (after Add) and *"ADMISSION MIGRATION REQUEST APPROVED
  SUCCESSFULLY. REQUEST WILL BE PROCESSED AT MONTH END"* (after Approve).

### Confirmed live — two real bugs found and fixed while building this

- **XPath `contains(text(), ...)` only inspects an element's FIRST direct text-node child** — "TRANSFER CASE FROM #<
  old_id>" and "TIE-UP : <name>" are later sibling text nodes within the same `<legend>` as "ACCOUNT STATEMENT (...)",
  so a naive `//*[contains(text(),'TRANSFER CASE FROM')]` silently never matches even though the text is genuinely on
  the page. Fixed by reading the whole `<legend>` text (`legendElement`, `css=legend`) and extracting with a Java
  regex (`getTransferCaseBannerText()`/`getTieUpBannerText()`) instead of a narrow XPath text-node match.
  `getBillingCancelDateText()`'s original `contains(text(),...)` locator is unaffected — confirmed live that text lives
  in its own dedicated element outside `<legend>`, not sharing text nodes with anything else.
- **"Admission Migration Already Requested" is state-dependent, not a persistent marker** — confirmed live it shows only
  mid-flight (Pending/Processing); once a child's migration actually completes (old child → Attrition), the banner
  disappears entirely, replaced by the Attrition status itself. All "duplicate blocked" / "already requested" assertions
  now check the Migrate button's absence (`isMigrateRegularToCorporateVisible()`/`isMigrateCorporateToRegularVisible()`
  returning false) instead, since that holds true across every post-submission state (Pending, Processing, and
  Attrition-completed).
- **The migration API's `date` param must be the request's own real WEF date, not a guessed "1st of current month"** —
  per explicit user instruction. Confirmed live: using a guessed date returned
  `{"status":"ok","0":"No Request to process Migration"}` (silent no-op, same class of timing gate documented for
  Corporate Transfer/Center Shift/etc.); reading the actual "WEF Date" column off the Recent Customer Requests grid (
  format `"MMM d, yyyy"`, e.g. "Oct 1, 2026") and parsing it to ISO before calling the API correctly processed the
  request and returned a real new child id both times it was tried.

### API endpoints (added to `utils/APIs.java`)

- `processAdmissionMigrationRequests(childId, date)` —
  `migrationprocess/process_admission_migration_requests?child_id=<id>&date=<yyyy-MM-dd>&ckey=EF0E0A75C6C2` — same
  endpoint/ckey for BOTH directions. Response on success:
  `{"status":"ok","0":["Request Processed successfully with new child id <id>"]}`.
- `processAdmissionPipeline(newChildId)` —
  `admissionpipeline/processAdmissionPipeline?ckey=EF7061CCE0C7&child_id=<new_id>&show=error` — run against the new
  child id returned above.

### Test data — outcomes discovered while sourcing children (confirmed live, do not re-derive)

- Child **64301** (Corporate) has **outstanding Dues** — blocks migration with the exact spec message *"Please clear
  Dues before raising migration request"*. Repurposed into its own test, `sc017_tc002_duesClearanceValidation` (maps to
  `SC_017_TC_002`/`SC_016_TC_003`).
- Child **64719** (Corporate) is in **PAUSE** status (not Active) — the Migrate button doesn't render **at all** for a
  paused child (confirmed via anchor dump — none of the migration-related anchors on the page match it). Not an official
  Excel TC id; added as a bonus scenario, `bonus_migrateButtonHiddenForPausedChild`.
- Happy-path children are **one-shot/consumed**: a successful full run takes the child all the way to Attrition (new
  child created), so the same id can't be reused for a second clean run. Consumed so far: Regular 70000, 70247, 71172 (→
  new child 73713 on the final clean run); Corporate 72454 (→ new child 73714 on the final clean run).

### Status: 14/14 automated cases CONFIRMED PASSING — feature complete

`sc016_tc002_mandatoryFieldValidation`, `sc016_tc004_fullFlowRegularToCorporate` (covers `SC_016_TC_004`+`TC_005`),
`sc016_tc011_centerProgramUnchanged`, `sc016_tc017_duplicateRequestBlocked`,
`sc016_tc018_customerRequestStatusAndWefDate`, `sc016_tc019_billingCancelDate`,
`sc017_tc003_fullFlowCorporateToRegular` (covers `SC_017_TC_003`+`TC_004`), `sc017_tc009_centerProgramUnchanged`,
`sc017_tc018_customerRequestStatusAndWefDate`, `sc017_tc020_billingCancelDate`, `sc017_tc002_duesClearanceValidation`,
`bonus_migrateButtonHiddenForPausedChild`, `sc017_tc023_blockedByPendingAttritionRequest`,
`sc017_tc022_centerRestrictedToCorporateOnly`.

- **`sc017_tc023_blockedByPendingAttritionRequest` confirmed live on child 67004** (user pre-verified before supplying
  it): the native submit confirm() DID fire, and the request was rejected server-side with the exact spec toast — *"
  OOPS! YOU CAN NOT APPLY ADMISSION MIGRATION REQUEST. YOUR ADMISSION MIGRATION REQUEST PRIOR/AFTER WITH CHILD ATTRITION
  REQUEST, TO PUT ADMISSION MIGRATION REQUEST THE CHILD ATTRITION REQUEST MUST BE CANCELLED."* — confirming the block
  happens after the confirm(), not before.
- **`sc017_tc022_centerRestrictedToCorporateOnly` confirmed live on child 71846** (user pre-verified) — **the
  restriction is ONLY for children at ONSITE centers**, and it manifests as the Migrate button not rendering at all (
  same absent-button pattern as the Paused-child bonus check, different underlying cause). Rewrote the test from "look
  for a message after clicking" to `assertFalse(isMigrateCorporateToRegularVisible())`, since the earlier "message on
  submit" hypothesis (from two unconfirmed candidates, 66969/66970 — both non-onsite, turned out unrestricted, and got
  consumed by the exploratory submit) was wrong for this specific restriction. **Note for finding a replacement child**:
  it must be at an onsite center specifically — a non-onsite Corporate center does NOT have this restriction.

### Open items

- Fresh Regular + Corporate children needed for the next clean full-suite re-run (both consumed by the last
  confirmed-passing chain run) — backups **71341** (Regular) and **66865** (Corporate) were supplied but not yet used.
- Child **71846** (onsite, center-restricted) is NOT consumed by this test — it's a pure read-only visibility check (no
  submit), so it's safely reusable for repeat runs.

## Requirements — Credits Issued (`CreditIssued_Testcases.java`)

Source: `TC_Support_Financial_Updates_2026.xlsx` (sheet `TC_Credits Issued`). 13 test cases. Screen: Support → Financial
Updates → Credits Issued (`https://test-franchise.footprintseducation.in/issue_credits`). Access right: `Issue_Credits`.
No existing page object/test class for this screen (confirmed via grep, 2026-10-05) — needs new
`pages/Support/CreditsIssuedPage.java` + `testScripts/SupportTests/CreditsIssued_Testcases.java` +
`CreditsIssuedtestng.xml`.

### SC001 — Default view & filters

- **SC001_TC_001** (Low) — Default view (ONE consolidated step): Filters — From & To Date (calendar), Center (dropdown),
  Type (dropdown: Credits, Void Credits), Status (dropdown: All, Pending, Successful, Failed), Submit button; Search
  bar; "Add New Issue Credits/Refund" button; Download Report button; Pagination; Table columns: Child Name, Amount,
  Type, Description, Bank, Issued By, Invoice Reference, Issue Date Time, Update Status, Action (Revoke Credit, Link
  Invoice).
- **SC001_TC_002** (Medium) — Date range filter (sheet data: 4 Nov 2024 → 4 Dec 2024) → Submit → table shows data within
  range.
- **SC001_TC_003** (Medium) — Center filter (sheet data: Telecom Layout, HBR Layout) → Submit → filtered data.
- **SC001_TC_004** (Medium) — Type filter (Credits) → Submit → filtered data.
- **SC001_TC_005** (Medium) — Status filter (Pending) → Submit → filtered data.

### SC002 — Add New Issue Credits/Refund

- **SC002_TC_001** (High) — Click Add New → modal → enter Child ID → Fetch Child Details → Child Name shown → fill
  Credit Type (dropdown), Invoice Reference, Credit Amount, Credit Line Item, Comments → Submit Form → confirmation
  popup "confirm ?" with OK & Cancel (native `confirm()` vs. in-page modal NOT yet confirmed) → OK → toast "Credits
  issued successfully!" → new row in table.
- **SC002_TC_002** (Medium) — Same up to Fetch, then Submit empty → validation messages: "Please select credit type!", "
  Select Invoice Reference!", "Credit Amount required!", "Credit Line Item required!", "Comments required!".

### SC003 — Search, download, pagination, sorting

- **SC003_TC_001** (Low) — Search by child name / support executive → matching rows; non-existent → "No matching records
  found".
- **SC003_TC_002** (Medium) — Download Report → CSV `Credits_Issued_Report_<dd_mm_yyyy>.csv` with columns: Child Id,
  Child Name, Issued Date, Amount, Type, Description, Bank, Issued By, Invoice Reference, Issue Date Time, Update
  Status, Father Name, Mother Name, Father Email, Mother Email, Father Phone, Mother Phone, Account Bank Name, Account
  Number, Account IFSC Code, Account Holder Email.
- **SC003_TC_003** (Low) — Pagination prev/next; 10 records per page.
- **SC003_TC_004** (Low) — Column sorting.

### SC004 — Actions & business rules

- **SC004_TC_001** (Medium) — Link Invoice Reference icon → modal shows Child ID, Child Name, Credit Type, Issued Credit
  Amount, Remaining Amount to Adjustment, Credit Issued On, Credit Start From → select Invoice Reference → Submit →
  toast "Credit invoice reference updated successfully".
- **SC004_TC_002** (Medium) — Revoke Credit: enabled ONLY if credit issued in current month AND no amount adjusted yet (
  partially adjusted credit cannot be revoked). Modal shows Child ID, Child Name, Credit Type, Issued Credit Amount,
  Remaining Amount, Credit Issued On, Credit Start From, Reason → enter reason → Revoke → toast "Credit revoked
  successfully".
- **SC004_TC_003** (Medium) — Credit-issuance email to parent (CC CH) sent ONLY for credit types: Downgrade Credits,
  Admission Discount, Credits - Non Satisfaction, Credits - Annual Fee, Credits - Book Charges, Credits - Delay Penalty,
  Credits - Extended Daycare, Credits - Late Stay, Credits - Transfer Fee, Credits - Transport, Credits - Tuition Fee.
  Skip email if amount is negative. Sheet has no steps for this case — how to verify is TBD (ask user).
- **SC004_TC_004** (Medium) — Void Credits: "Credits" are for customer invoices only; "Void Credits" apply to both
  customer and corporate invoices and are added only by the system against a voided invoice, while the void invoice is
  processed Sheet has no steps for this case — how to verify is TBD (ask user).

### Confirmed live (2026-10-05, Rakesh, read-only exploration with child 69912 — nothing submitted)

- **Main screen:** "Add New Issue Credits/Refund" =
  `<a class="popdown_big btn btn-primary pull-right" href="pop_issue_credits?pop=yes&child_id=" title="Issue Credits">` —
  a `popdown_big` modal (same family as Cancel Registration's popdown; watch for the leftover `#popdown-opacity`
  overlay).
- **Filters:**
    - From `input[name=date_from]` and To `input[name=date_to]`. **Both share the duplicate id `select_date`**, so
      locate them by `name`. They are readonly pickadate.js widgets (`picker__input`); defaults are From = today − 1
      month, To = today.
    - Center `select#center_selection` (select2; first option "All Center"; includes "Telecom Layout, HBR Layout").
    - Type `select#credit_type` = [All | Credits | Void Credits]. Status
      `select#status` = [All | Pending | Successful | Failed].
    - Submit = `input[name=submit_credit_date]`.
- **Table:** DataTables `#DataTables_Table_0` (`datatable-credits-listing`), "Showing 1 to 10 of 115 entries" by
  default, default sort Issued By desc. Search = `#DataTables_Table_0_filter input[type=search]`. Download Report =
  `a.buttons-csv` (client-side DataTables CSV export, which includes hidden columns). Pagination =
  `#DataTables_Table_0_paginate`.
- **Visible columns:** Child Name (link to `account_statement?child_id=<id>`) | Amount | Type | Description | Bank |
  Issued By | Invoice Reference | Issued Date Time | Update Status.
    - **Hidden (`display:none`, CSV-only):** Invoice Line Item, Father/Mother Name, Father/Mother Email, Father/Mother
      Phone, + 4 blank (likely the Account Bank Name/Number/IFSC/Holder Email columns).
    - **No Action column, and no Revoke / Link Invoice control anywhere on the page** (Rakesh, default filters). **Per
      user (2026-10-05): this is because every credit currently listed was added successfully (Update Status = "
      Successful"), so Revoke is not offered for any of them** — matches the sheet's rule that Revoke is only enabled
      for a current-month credit with nothing adjusted yet. Whether Link Invoice is hidden for the same reason is not
      yet confirmed.
    - No separate "Child Id" / "Issued Date" column seen, unlike the sheet's CSV list.
- **Add modal:**
    - Child ID `input#child_id` (number, min=20) → Fetch `a#btn_child_details` (`getChildDetails()` →
      `api/franchises/getChildDetails`) → Child Name shown in `.div-child-name` / hidden `#child_name` (69912 = "Eleanor
      Mariam Jerin").
    - It then calls `api/financials/fetchInvoiceReferences` → `select#credit_invoice_ref`. Option `value` = internal
      invoice id, `data-amount` = invoice amount, text =
      `"<dd Mon, yyyy> -> Rs. <amt> (paid|payment_due) [<invoice no>]"`. Both paid and due invoices are listed; the
      placeholder is `<option value="">Select Invoice</option>`.
    - Credit type `select#credit_type_item`: placeholder "Select Credit" + 18 types in
      `<optgroup label="Credit types">`. Option `value` ≠ label for one entry: value "Credits - Admission Discount" has
      label "Admission Discount".
    - Credit Amount
      `<input id="credit_amount" type="number" class="form-control required" placeholder="Credit Amount" min="20">` — *
      *per user (2026-10-05): sometimes auto-filled from the selected invoice, sometimes must be entered manually.** The
      page JS only auto-fills it (amount = invoice `data-amount`, line item = "Convenience Fee") when credit type = "
      Credits - Convenience Charges"; whether other cases also auto-fill is unconfirmed. Tests must check the field
      after invoice selection and type a value only when it's empty. `min="20"` is an HTML attribute only — the submit
      JS never checks it (button is type=button, no native form validation), so a value below 20 is probably not
      blocked; unconfirmed. Credit Line Item `textarea#invoice_line_items` (free text). Comments
      `textarea#credit_comments`. Submit `button#apply_credit` ("Submit Form").
    - **Child with no invoices** → promotional mode: "Invoices not found. You can add promotional credits only."; the
      type list is reduced to "Promotion Credits"; the button becomes `#apply_promotional_credit` ("Add Promotional
      Credit"); Invoice Reference is not required.
    - **"Credits - Convenience Charges"** filters invoices to Convenience-Fee ones only; picking an invoice auto-fills
      the amount and the line item "Convenience Fee".
    - **Keyword guard:** a line item containing apron/bag/shirt/kit/book/convenience with a mismatched credit type
      blocks submit with "As per description, credit is for '<kw>'. Credit type must be set to '<type>'."
- **Validation is SEQUENTIAL, one message at a time** (`p.error-<field>`), in this order: "Child ID required!" → "Please
  select credit type!" → "Select Invoice Reference!" → "Credit Amount required!" → "Credit Line Item required!" → "
  Comments required!". An empty submit shows only "Please select credit type!".
- **Submit:** native `confirm("Confirm?")` → POST `api/financials/issueCredits` → `flash_message(data.message)` in
  `.panel-container-form` → **page auto-reloads after 2s**. Exact success text not yet seen live (sheet: "Credits issued
  successfully!").

- **Child 70095 = "Pavamaana Datta Daram"** (user-supplied, confirmed live 2026-10-05 via read-only Fetch): 10 invoices,
  all **paid** — P512/2627/6, K/2627/4069, B/2627/7335, P512/2627/17, P512/2627/18, T/2627/33 (Rs. 29.50,
  Convenience-type), K/2627/4191, K/2627/4190, K/2627/4189, T/2627/8371 (Rs. 29.50). Which test case(s) this child is
  for is NOT yet confirmed by the user.

### Confirmed live as Jaydeep Kar (2026-10-05, read-only — modals opened, nothing submitted)

- **Action column** (21st `th`, text "Action") appears only for Jaydeep; the table showed 116 entries.
- **Link Invoice Reference:** shown on every row, including "Successful" ones.
    - Icon:
      `<a class="popdown_medium btn btn-link btn-float has-text" href="invoice_reference?pop=yes&credit_id=<id>" title="Link Invoice Reference"><i class="icon-hyperlink text-warning"></i></a>`.
    - Modal title "Link Invoice Reference". It shows Child ID (#id), Child Name, Credit Type, Issued Credit Amount,
      Remaining Amount to Adjustment, Credit Issued On, Credit Start From (all match the sheet).
    - `select#credit_invoice_ref`: placeholder "Select Invoice Reference"; option text is
      `"<date> -> Rs. <amt> (<status>) [<invoice no>] || <booking head>"`, e.g. "|| Tuition", "|| Recurring -> Monthly".
    - **The dropdown opens with the credit's CURRENT invoice already selected** (`selected="selected"`; e.g. credit
      151993 → T/2627/3442, the same value as the grid's Invoice Reference column). Confirmed via user-pasted DOM. So
      the "Select Invoice Reference!" error only appears if the user picks the placeholder back, and a test should pick
      a DIFFERENT invoice to prove the link changed.
    - Modal title (user-confirmed):
      `<h4 class="modal-title"><i class="icon-hyperlink text-warning"></i> &nbsp;Link Invoice Reference</h4>`.
    - Submit `button#btn_submit_reference`. Empty submit → "Select Invoice Reference!".
    - **No confirm()** — goes straight to POST `api/financials/updateCreditInvoiceReference` → `flash_message` → reload
      after 2s.
- **Revoke Credit:** shown only on an eligible credit.
    - Confirmed on child **69003 "Chancy Sethi"**: credit_id 152174, Rs. 600.00, Credits - Book Charges, invoice
      B/2627/5235, issued Oct 5, 2026 12:42:44 by Rakesh Singh, Update Status = **"Successful"**. So "Successful" does
      NOT hide Revoke; current month plus unadjusted is what matters, consistent with the sheet's rule.
    - Found by searching the child name in the DataTables search box (the user says sorting the column also works).
    - Icon (user-confirmed):
      `<a class="popdown_medium btn btn-link btn-float has-text" href="revoke_credit?pop=yes&credit_id=<id>" title="Revoke Credit"><i class="icon-database-remove text-orange"></i></a>`.
    - Modal title `<h4 class="modal-title">… &nbsp;Revoke Credit</h4>` (note: it reuses the `icon-hyperlink` icon).
    - Modal shows Child ID, Child Name, Credit Type, Issued Credit Amount, **Remaining Amount** (Rs. 600.00), Credit
      Issued On. **No "Credit Start From"** field, unlike the sheet's list.
    - User screenshot (2026-10-05) matches the dump: the row's Action cell shows the Revoke icon (orange
      `icon-database-remove`) stacked above the Link icon; rows without Revoke show only the Link icon.
    - Reason `textarea#credit_revoke_reason`, Submit `button#btn_submit_reason` ("Revoke"). Empty submit → "Revoke
      Reason is Missing!".
    - **No confirm()** → POST `api/financials/revoke_credit` → `flash_message` → reload after 2s. Success message
      location (user-supplied, 2026-10-05): absolute XPath
      `/html/body/div[7]/div/div/div/div/form/div/div[1]/div/div[1]` (first block inside the Revoke modal's form, i.e.
      where `flash_message` renders in `.panel-container-form`). Do NOT use the positional XPath as-is; locate it
      relative to the form `#frm_revoke_credit`. **Exact success text confirmed by user: "Credit revoked successfully"
      ** (matches the sheet).
- **"Void Credit" action** (mentioned by user): not seen on any row on page 1 or on 69003's row — likely only on
  Void-Credit-type rows; unconfirmed.

### Automation built (2026-10-06) — first full run: 14/15 PASSED
Files: `pages/Support/FinancialUpdates/CreditIssued_Page.java` (new), `testScripts/SupportTests/FinancialUpdates/CreditIssued_Testcases.java` (new, 15 tests), `CreditIssuedtestng.xml`; additive `IAutoConstant.CREDITS_ISSUED_URL` + `Navigations.goToCreditsIssued()`. Runs as Jaydeep via `getUserForScreen("Credits Issued")`.
- Order: SC001_TC_001–005 → SC002_TC_002 (validation) → SC002_TC_001 (issues credit, captures new credit_id by diffing the child's credit_ids before/after) → SC004_TC_001 (link) → SC004_TC_002 (revoke) → SC003_TC_001–004 → SC004_TC_004 (void filter) → SC004_TC_003 (email, re-logs in as Rakesh, runs last).
- SC001_TC_002 uses **current month (1st → today)** — the user's last answer was "current month date range"; whether that was meant for this test or for Void Credits is NOT yet confirmed. Dates are set through pickadate's own API (`picker.set('select', …)`), and every row's Issued Date Time is asserted to be in range.
- Run 1 evidence: credit_id **152177** issued on 70095 (Rs 100, Book Charges, B/2627/7335) → toast "Credits issued successfully!" → re-linked to T/2627/8371, toast **"Credit invoice reference updated successfully."** (trailing period) → revoked, toast "Credit revoked successfully".
- **A revoked credit disappears from the grid entirely** (its row is gone after reload, not just the icon).
- **Link modal shows only invoices relevant to the credit type, plus paid ones** — for the Book credit, the first non-current option was T/2627/8371 (Convenience). The app accepted linking a Book credit to a T/ invoice.
- CSV confirmed: `Credits_Issued_Report_06_10_2026.csv` (dd_MM_yyyy), header = Child Id, Child Name, Issued Date, Amount, Type, Description, Invoice Line Item, Bank, Issued By, Invoice Reference, Issued Date Time, Update Status, Father/Mother Name/Email/Phone, Account Bank Name, Account Number, Account Ifsc Code, … (matches the sheet plus an extra "Invoice Line Item").
- Pagination: 10/page confirmed ("Showing 1 to 10 of 50 entries" → "Showing 11 to"). Sorting: Amount asc/desc numeric + Child Name asc confirmed.
- SC004_TC_004: 0 Void Credits in the default range → logged as informational, not failed.
- **SC004_TC_003 (email) — fixed (2026-10-06), confirmed live via a read-only check; not yet re-run inside the full suite.**
  - The subject is **"Credit issued worth Rs. <amount>"**.
  - The child id/name appears ONLY in the email body, never in the row text — that's why the first run failed.
  - Per user instruction, the test now filters Subject = **"Credit Issued"** and Body = child id (`#body` field on Email View's `#frm-search`), then matches the row containing "credit issued worth rs. <amount>" + today's date (`dd MMM, yyyy`).
  - Live row: `qa@footprintseducation.in | Credit issued worth Rs. 100 | cc: center head | ["error",401,"Invalid_Key","Invalid API key"] | 06 Oct, 2026 12:38 PM | Pending`. The 401 is the test environment's mail provider, not a test issue.
  - The email is still recorded even when the credit is revoked right after.
  - The grid's "Issued Date Time" (07:08 AM) vs the email time (12:38 PM) suggests the grid shows UTC, not IST.

### FINAL STATUS (2026-10-06): Credits Issued automation COMPLETE — 32/32 passing
- Complete suite run: 30/32. The 2 misses (Link/Revoke) were caused by child 73041 having 40+ credits, so the new credit wasn't on search page 1. Fixed in `openActionModal()`: it now runs `DataTable().page.len(-1).draw()` after searching. Re-run of add+link+revoke: 3/3 (credit 152238, link B/2627/6208 → PI/993959, revoked).
- Robustness fixes after a slow-server run (26/32, all test-side; the app had issued every credit correctly):
  - Default view now checks the table's DEFINED columns via `getDefinedColumnHeaders()` — the responsive DataTable folds "Update Status" into the "+" expander when long descriptions squeeze it.
  - Confirm wait is now 20s.
  - In the per-type test, the new grid row is the hard proof of issuance; a missed flash message is only logged. Customer Payment (not listed, by design) keeps the message as a hard check.
- Chain credit **152220** (Book, 73041) from the complete run was revoked manually by the user (2026-10-08). Per-type credits are intentionally left in place.
- OneTime Charges regression (same day, child 72620): **25/25 passed**.

### Per-credit-type happy flow (2026-10-06) — `sc002_tc001_happyFlowEachCreditType` (DataProvider `creditTypes`, same pattern as OneTime Charges' `chargeTypeData`)
- 17 types (every dropdown type **except "Credits - Security"**, excluded per user). Optional `-DcreditTypes="<type>,<type>"` runs only those rows.
- **The app does NOT validate the credit type ↔ invoice mapping** (user, 2026-10-06), but Support maps them manually. So each row picks the invoice the way Support would: by the invoice's **booking head** (read from the modal's cached `arrCreditInvoices.data[id].booking_heads`, exact `|`-split match, PAID only — `CreditIssued_Page.selectInvoiceByBookingHead()`), or by an exact invoice number (`selectInvoiceByNumber()`).
  - **`T/` invoices are Convenience Fee (head "Convenience Fee|IGST", Rs 29.50), NOT Transport.** Transport is billed on `P378/...` invoices with head **"Transport Charges"**.
- Mapping (user-confirmed):
  - Tuition → Preschool Fee
  - Book → Book Set (B/)
  - Transport → Transport Charges (child **50875 Gyanvi Mittal**)
  - Convenience → Convenience Fee (amount/line item auto-fill)
  - Annual → Annual Preschool Fee
  - Registration → Registration Fee
  - Delay Penalty → **P378/2627/495**, Extended DayCare → **P378/2627/493**, Late-Stay → **P378/2627/479** (all on child **46195 Nayansh Kaushik**, same child as OneTime Charges)
  - every other type → Preschool Fee
  - Main child: **73041 Kartikey Jaiswal** (has B/, K/, P378 and T/ paid invoices). Other children checked: 73875 has only due PI/ invoices (unusable); 73007 has no T/.
- **Results:**
  - 16/17 confirmed passing: 13 on 73041, Transport on 50875 (credit 152195), Delay Penalty/Extended DayCare/Late-Stay on 46195 (credits 152199/152200/152201).
  - **"Customer Payment" FAILED — investigated read-only (2026-10-06):** the server accepted it ("Credits issued successfully!", credit_id 152183 consumed), but that id appears in the Credits Issued grid under NONE of Type All / Credits / Void Credits, and "Customer Payment" text is not on child 73041's Account Statement either. Every other type shows up immediately. **Confirmed BY DESIGN (user, 2026-10-06): Customer Payment credits are not listed on Credits Issued** — the test now checks only the success message for that row.
  - **Correction:** the failed per-type runs 2/3 DID issue some credits (contrary to the first assumption that a dismissed confirm submitted nothing). Extra credits on 73041: 152179 (Delay Penalty), 152180 (Book), 152187 (Delay Penalty), 152193 (Late-Stay).
  - Fresh children are NOT needed between runs — issuing a credit doesn't consume the child. User supplied spares 53603, 64087 (unused, invoice heads not checked).
- Bug fixed in the first per-type run: checking `isAlertPresent()` 500ms after Submit returned false; touching the DOM while the native confirm was open made Chrome DISMISS it (= Cancel, nothing submitted). Now waits for the alert via `acceptConfirmPopup()` (10s).
- Full-suite run 2 (2026-10-06): all 15 original tests PASSED, incl. SC004_TC_003 email with Subject "Credit Issued" + Body child id (chain credit 152178 on 73041, re-linked + revoked).

**Status (2026-10-05, end of day): exploration done, NO automation code written yet. Resume here.** Pending user
answers:

1. Was credit 152174 (child 69003) actually revoked? Proposed: the test issues its own credit, then revokes it (
   self-supplying data).
2. Link Invoice: which credit, and which (different) invoice to re-link to — can it be the test's own new credit?
3. Void Credit action: which row shows it, and what to verify.
4. Add a `Jaydeep Kar | Credits Issued | Issue_Credits` row to `input_UserRights.xlsx`?
5. Add New: child 70095? Credit type + amount? Exact success text (sheet: "Credits issued successfully!")?
6. SC002_TC_002: OK to check the sequential validation messages one at a time?
7. Optional checks: below-20 amount, SC004_TC_003 email, SC004_TC_004 Void Credits — in scope?

**Decisions confirmed by user (2026-10-06):**

- Revoke data: the **test issues its own credit**, then revokes that same credit in the same run (no per-run fresh child
  needed).
- Add New (SC002_TC_001): child **70095**, credit type **Credits - Book Charges** (against a Book invoice; amount typed
  manually; line item must mention "book" to pass the keyword guard).
- Link Invoice: re-link the **test's own new credit** to a different invoice of the same child, then verify the grid's
  Invoice Reference changed.
- Optional-check scope (user, 2026-10-06): **(a) SC002_TC_002 sequential validation — YES**; **(b) SC004_TC_003 email
  check — YES, via Email View logged in as Rakesh**; **(c) SC004_TC_004 Void Credits Type filter — YES**; **(d) below-20
  amount negative test — NO, leave it out.**
- Void Credit action: per user, it appears in the Action column (on which rows is not yet confirmed live).
    - Live check (2026-10-06, Jaydeep, read-only): Type = "Void Credits" with the default range (05 Sep 2026 → 06 Oct
        2026) → **"No data available in table" (0 entries)**. Across the default "All" view, the only action titles
              present are "Link Invoice Reference" (`invoice_reference`) and "Revoke Credit" (`revoke_credit`, on 2
              rows) — **no
              Void Credit action seen on any row**. Need from user: a date range/child that has Void Credits, and which
              row
              shows the Void Credit action.
- `testData/input_UserRights.xlsx` row added (2026-10-06): `Jaydeep Kar | Credits Issued | Issue_Credits` → use
  `getUserForScreen("Credits Issued")`.
- **Invoice-matching rule for issuing a credit (user, 2026-10-06):** pick an invoice whose number prefix matches the
  credit type — Book → `B/...`, Kit → `K/...`, Transport → `T/...`; any other type can use any invoice. Credits can be
  issued against any PAID invoice.
- Date-range filter (SC001_TC_002): use the **default last-month range** (From = today − 1 month, To = today), not the
  sheet's Nov–Dec 2024 (user, 2026-10-06).
- Add New success text confirmed: **"Credits issued successfully!"**. Amount: 100 (proposed, not objected to).

### Open items to confirm before automating

- ~~Which user~~ — RESOLVED, **SUPERSEDED (user, 2026-10-05): switch to Jaydeep Kar.** Rakesh sees no Action column at
  all; only Jaydeep gets the Action column (Revoke Credit, Link Invoice Reference, Void Credit etc.). Switch via
  `navigations.goToUserRights()` + `userRightsPage.switchUser("Jaydeep Kar")`. Jaydeep's existing Excel rows are under
  `OneTime Charges` / `Account Statement`; a `Credits Issued` row is not added yet, so either reuse one of those keys or
  add a new row (ask user).
- Test child IDs: one for Add New (with an open invoice for Invoice Reference), one with a current-month unadjusted
  credit (Revoke), one with a credit lacking an invoice link (Link Invoice).
- Sheet's date-range data (Nov–Dec 2024) may be stale — confirm a window that actually has data.
- Whether Credit Amount accepts negatives (relevant to SC004_TC_003's "skip email if negative" rule).

## Requirements — Payment Plan Migration (V1 ↔ V2) (requirement received 2026-10-13, NOT yet automated)

Screen: Support → Account Statement → new button (user's description: "Migrate to short term or long term plan", or a
mail notification sent to the parent). Clicking it opens a popup showing the calculation details. The spec's screenshots
were not included in the pasted text, so the button label, popup fields and DOM are **not yet confirmed live**. No
existing page object or test covers this (grep, 2026-10-13).

### Business rules (as given)

- Until now, V1→V2 was allowed only at renewal, when the annual invoice is due (the "Payment Plan Migration Promotion").
  From now on, parents cannot change payment terms at any time during the tenure.
- **Support (Account Statement):** only V2→V1 is done by Support. It is processed immediately, and the system adds a
  customer request with **Approved** status to track the change history. **All invoices must be paid** before a plan
  can be migrated.
- Parents can revert a plan after it is migrated by adding a new request. This is not blocked yet (parent-app work is
  pending).
- Requests are taken **for next month only**. "Case 2 (current month)" is marked *Not applicable* for both directions.
- **There is no migration benefit in the first year.** V1→V2 is offered only from year 2, at each annual renewal, and it
  is fully automatic: no Support or centre action is needed.
- **Email flow (cron-driven, V1→V2 renewal offer):**
  - About 7 days before renewal, an offer email goes to the parent with the new monthly fee, the credit/charge
    calculation and the next bill.
  - If there is no response, a reminder goes out about 3 days before renewal.
  - The parent replies Accept or Decline through a secure one-time link (same style as the joining-date-change email).
  - **Accept:** about 2 days before renewal, ahead of that cycle's invoice, the system auto-adds a month-end migration
    request. The month-end cron then processes it.
  - **Decline or no response:** nothing changes, and the offer returns the following year.
  - If the current cycle's annual fee is already paid, it is non-refundable, and the switch applies from the next
    renewal.

### V1 → V2

- The subscription changes to the V2 card (+2000 monthly fee difference), and the yearly plan is cancelled.
- A credit against the latest annual fee invoice applies only for a mid-month migration, which is currently not taken
  (next month only).
- Formula: `Annual Fee − ((V2 monthly − V1 monthly) × Months Of Service)`, where Months Of Service = service days till
  date / 30. Sample: 18000 − (1500 × 90/30) = 8500 (note: sample uses a 1500 difference, not 2000).
- Credit description: **"Annual Fee Credits Against V1 to V2 Migration"**.
- Case 1 (next month): issue the monthly fee invoice per fee card + discounts, update the admission record and monthly
  billing plan (+2000), and add a history item and logs.

### V2 → V1

- The subscription changes to the V1 card (−2000), and a yearly plan is created with billing date = Joining Month + 11
  months.
- The difference is charged as the Annual Fee: `Annual Fee − ((V2 monthly − V1 monthly) × Months Of Service)`. Sample
  8500.
- Invoice line item: **"Annual Fee Against V2 to V1 Migration"**. The breakup follows the program like other invoices,
  and the invoice must be treated as the last annual invoice.
- Case 1 (next month): issue monthly fee credits per fee card + discounts, raise the annual invoice and set the annual
  plan, update the admission record and monthly billing plan (−2000), and add a history item and logs.

### Confirmation email (both directions)

Subject **"Confirmation of payment plan migration"**, To: Parents, Cc: CD. The body confirms the change for <Child
Name> and the new monthly invoice amount (example: 13999) from next month. It then has **either** "Invoice of prorated
fee difference … is charged" **or** "Credits of prorated fee difference … are issued". A calculation block follows:
Annual & Registration Fee Difference, Monthly Fee Difference for service period (example `5625 (1500* 3.75 Months) /
2000`), and Final Fee Difference. The spec's example numbers are internally inconsistent (15000 − 5625 ≠ 7325), so do
not hard-code them. This can be verifiable via Email View (Rakesh, filter Subject).

### Open items (to confirm before automating)

- Exact button label(s) and visibility rules on Account Statement. Is it one button with a direction choice, or separate
  V1→V2 / V2→V1 buttons? Which plan is "short term" vs "long term"? The spec says Support does V2→V1 only, yet it
  details V1→V2 too.
- Popup fields/DOM, the "applicable month" selector, and whether a native confirm() fires.
- Test children: V2 child (all invoices paid), V1 child, a child with unpaid invoices (negative), a first-year child
  (negative).
- The user to switch to (needs a `input_UserRights.xlsx` row) or Rakesh.
- Whether the renewal-offer cron/email + Accept/Decline link is in scope (cron endpoint/ckey not supplied).
- Where history/logs render (Child History modal vs Recent Customer Requests "Approved" row).

### Test cases — `TC_Account Statement Scripts.xlsx`, sheet `Account Statement Page`, SC_018_TC_001–043 (studied 2026-10-13)

Note from the sheet: **V1 = Long Term Payment Plan, V2 = Short Term Payment Plan.** The access right is
`Payment_Plan_Migration`. The sheet predates the 2026-10-13 requirement; enhancements to it are still to be supplied by
the user.

**A. Account Statement button flow (TC_001–010)**

- TC_001 / TC_003: "Migrate in Short Term Payment Plan" opens the modal "Plan Migration Calculation" (default view +
  calculated values). Example: Annual Fee V1 Rs 15,000; Monthly Fee Difference (2000) Rs 3,460 (2,000 × 1.73 months);
  Final Fee Difference Rs 11,540; "Credit of Rs. 11,540.00 will be issued". Buttons: Close icon/button and Confirm.
- TC_002 (V1→V2, months 1–8, child 47562):
  - Confirm triggers "Are you sure you want to migrate plan?" (OK/Cancel).
  - Toast: "Plan migrated successfully from V1 to V2. Credit of Rs. 11,540.00 raised/issued".
  - The button then reads "Migrate in Long Term Payment Plan".
- TC_004 (V1→V2, months 9–12, child 42585):
  - An "Applicable From*" dropdown offers Current/Next Month; the test picks Current Month.
  - Toast: "Plan migrated successfully from V1 to V2. Monthly Plan of Rs. 12,999.00 updated for next month".
  - (The sheet notes Footprints generally disallows this case.)
- TC_005 (V2→V1, months 1–8, child 46596):
  - Monthly Fee Difference Rs 9,540 (2,000 × 4.77 months of "V2 plan extra paid period"); Final Rs 5,460.
  - Toast: "Plan migrated successfully from V2 to V1. Invoice of Rs. 5,460.00 raised/issued".
  - The button then reads "Migrate in Short Term Payment Plan".
- TC_006 (V2→V1, months 9–12, child 39621):
  - Applicable From = Current Month.
  - Toast: "...Monthly Plan of Rs. 21,499.00 updated for next month". The annual invoice is Rs 15,000 and monthly
    credits of 2000 are issued.
- TC_007: verify the invoice created after migration (no detail in the sheet).
- TC_008: email "Confirmation of payment plan migration" sent to the parent.
- TC_009: due invoice blocks migration — "Invoice payment is due. Invoice must be paid for migration plan !!!"
- TC_010: a pending attrition request blocks migration — "Oops! You can not apply Plan Migration request. Your Plan
  Migration request prior/after with Child Attrition request, To put Plan Migration request the Child Attrition request
  must be cancelled".

**B. Background jobs, V1→V2 renewal offer (TC_011–015)**

- TC_011: `{{Base_URL}}financialprocess/saveMigrationPromotionalData` saves renewal-due children (example child 42098)
  to Support → Financial Updates → Payment Plan Migration.
- TC_012: the same endpoint with `?send_promotional_email=yes` sends the first email and sets status "Informed". Single
  parents are excluded because they pay no annual fee.
- TC_013: `financialprocess/sendMigrationPromotionalReminderEmail?days=3` sends the reminder and sets status "Reminder".
- TC_014: the parent website link "Offer To Migrate On New Payment Plan - Zero Annual Fee!" → Confirm → OTP → status
  becomes "Confirmed", with the parent confirmation date and phone recorded.
- TC_015: `financialprocess/MigrateDataInNewPlan` sets status "Completed", completion email "Sent", and the migration
  date. The parent email and an internal email are both sent.
- None of these endpoints has a ckey in the sheet.

**C. Support → Financial Updates → Payment Plan Migration screen (TC_016–029)** — a NEW screen with no page object.

- TC_016 default view:
  - Filters: Migration Status multi-select with options Pending / Informed / Denied / Reminder / Confirmed / Completed
    / Cancelled; Migration Month; Center (All Center); Filter button.
  - Also: Search and Pagination.
  - Columns: Child ID, Child Name, Fee/Addons/Coupons Details, Completion Email Status, Confirmation Date(Parent),
    Confirmed By(Parent), Migration Status, Migration Date, Migration Comments, Action(Update).
- TC_017–020: filter ("No data available in table"), search ("No matching records found"), pagination at 10 per page,
  and sorting.
- TC_021 / 022 / 025 — Update modal "New Payment Plan Migration Request" (status Confirmed/Cancelled + comments):
  - Confirmed toast: "Thanks, you will be migrate on new payment plan on <date>". The row gains a "Migrate to V2/V1"
    button.
  - Cancelled toast: "Thanks, Request cancelled". The Update button is removed.
- TC_023 (child 26316): "Migrate to V2" shows the toast "Hurray! Migration has been completed successfully of <name>
  (#id)" and status becomes Completed.
  - If the annual invoice was paid, a credit is issued; if it is due, the invoice is voided.
- TC_024: the parent picks Not Interested → status "Denied".
- TC_026–029: date-window variants (confirm before the 29th cycle / 31st evening / after 1st-of-month invoicing / after
  annual paid → auto Completed). These depend on the server date.

**D. Parent website, discounts and edge cases (TC_030–043)**

- TC_030–032: the website page and its options (confirm / reminder / not interested); execution 1 day before renewal.
- TC_033: a pending annual invoice from the last 30 days is voided.
- TC_034–039: staff / sibling / single-parent / inauguration discounts and add-ons continue after migration. Siblings get
  separate emails and separate executions, and both must be active.
- TC_040: the link expires on the last day of the month at 8 PM; the cron runs at 9 PM (SDA-9396).
- TC_041–042: Support can confirm or reject only after the invoice is generated (from the 1st).
- TC_043: a V1→V2 migration at exactly 7.5 months calculates correctly and its approve button works (SDA-9655).

**Conflicts with the 2026-10-13 requirement, to resolve with the user's enhancements:**

- The requirement says requests are for next month only and Case 2 (current month) is not applicable. TC_004/TC_006
  select Current Month.
- The requirement says Support does V2→V1 only. TC_001–004 test V1→V2 from Account Statement.
- The requirement's emails go 7 days before renewal and auto-request 2 days before; the sheet uses the 23rd/24th, 28th/
  29th and 30th.

### Enhancement — First-year eligibility + automatic renewal plan-switch email (received 2026-10-13, NOT yet automated)

**Problem:** today a V1 parent is offered V1→V2 with a benefit even in the child's first year. This stops: the benefit
applies only from year 2, with the first year counted from the admission/joining date. The system has no "first-year
eligibility" concept yet, so this is new logic.

**Acceptance criteria (as given):**

1. A V1 child within their first year (per the yearly recurring date) is NOT offered or given the V2 migration benefit.
2. From year 2, at the time of the recurring annual charge, an email is sent automatically offering the plan switch
   (V1→V2 or V2→V1, whichever applies). This email already exists; its content is being updated.
3. The email states clearly that an already-paid annual fee is non-refundable.
4. The email has Accept/Decline actions using the same mechanism as the existing joining-date-change flow.
5. An Accepted switch is locked in and applied from the next renewal cycle, before that cycle's recurring invoice is
   generated.
6. Declined or no response → the child stays on the current plan, and the upcoming invoice is unchanged.
7. The renewal email triggers automatically for V1→V2, with no centre-team action.

**Technical notes:**

- Reuse the joining-date-change Accept/Decline mechanism; don't build a parallel flow.
- First-year is likely derived from the admission/joining date. Confirm no separate V1-start-date field exists.
- Identify which admissionapi cron/billing job fires the renewal email today. Per SC_018_TC_011–013 it is likely
  `saveMigrationPromotionalData` / `sendMigrationPromotionalReminderEmail`.
- The switch-lock deadline must be enforced against the real invoice-generation job, not a soft cutoff, to avoid a late
  Accept arriving after billing has run.
- Update the SOP/wiki: no first-year benefit; the process is now automatic.

**Impact on SC_018 (QA mapping, pending user confirmation):**

- AC1 → NEW negative cases:
  - A first-year V1 child is not picked up by `saveMigrationPromotionalData` (no row on the Payment Plan Migration
    screen, no email).
  - The Account Statement "Migrate in Short Term Payment Plan" button is hidden or blocked for a first-year child —
    UNCONFIRMED whether AC1 also covers this manual button.
  - Boundary: the day before vs the day of the 1-year anniversary.
- AC2/3/4 → update SC_018_TC_012/013 (email content, non-refundable text, Accept/Decline link) and TC_014/TC_024
  (accept/decline via the joining-date-change-style link instead of the old website-OTP page — UNCONFIRMED).
- AC5 → TC_015/TC_026–029/TC_040–042 (lock deadline vs invoicing). Plus a NEW race case: Accept after the
  invoice-generation job has run must NOT switch the current cycle.
- AC6 → TC_024 (Declined) + a NEW no-response case: plan and upcoming invoice unchanged.
- AC7 → TC_011/012 run by cron, no manual trigger. In QA they are still triggered by API.
- The SC_018 rows that use 2024 dates/children (e.g. TC_014's November 2024 example) need fresh data.

### QA-member test cases for the enhancement — `SDA-13134_plan payment.csv` (reviewed 2026-10-13)

There are only 4 cases, all on Account Statement:

- TC-001: a V1 child sees NO V1→V2 option or button.
- TC-002: a V2 child sees the V2→V1 option.
- TC-003: V1→V2 cannot be forced via a direct URL or a manipulated request.
- TC-004: the V2→V1 button shows only for an eligible V2 child.

These imply that on Account Statement **Support can only do V2→V1, and the V1→V2 button is removed entirely**. That
supersedes SC_018_TC_001–004, which ran V1→V2 from Account Statement. Not yet confirmed live.

**Gaps vs the requirement + enhancement ACs (not covered by these 4):**

- First-year eligibility (AC1), plus the anniversary boundary.
- The automatic renewal email: its content, non-refundable wording, and Accept/Decline (AC2–4, AC7).
- Accept is locked before invoicing, including the late-Accept race (AC5); Decline/no-response leaves the plan
  unchanged (AC6).
- The V2→V1 execution itself: calculation popup, Applicable From = next month only, confirm, toast, Approved customer
  request, annual invoice "Annual Fee Against V2 to V1 Migration", monthly plan −2000, history, confirmation email.
- Blocks for "all invoices must be paid" and for a pending attrition request.
- TC-004's "eligible" is undefined.

### APIs — `Payment Plan Migration.postman_collection.json` (shared 2026-10-13)

`{{Base_URL}}` = `https://test-admissions.footprintseducation.in/api/` (`APIs.ADMISSIONS_BASE_URL`), and **no ckey is
needed for any of the 4** (both confirmed by the user, 2026-10-13). Added to `APIs.java` (compiles, not yet run
live): `sendPlanMigrationPromotionalEmail(childId)`, `sendPlanMigrationPromotionalEmailForDate(date)`,
`sendPlanMigrationReminderEmail(days)` and `processPlanMigrationRequests(childId, date)`.
User decisions (2026-10-13): trigger the offer email via the API (not wait for the cron), and **explore live first,
then automate**.

1. `GET financialprocess/saveMigrationPromotionalData?send_promotional_email=yes&date=2027-01-25` saves renewal-due
   children to the Payment Plan Migration screen and sends the offer email. The `date` param presumably simulates the
   run date (SC_018_TC_011/012).
2. `GET financialprocess/saveMigrationPromotionalData/?send_promotional_email=yes&child_id=69912` is the same, scoped to
   one child: the first email to the parent (23rd/24th of each month per the old schedule).
3. `GET financialprocess/sendMigrationPromotionalReminderEmail?days=3` sends the reminder 3 days before annual billing
   (28th/29th) (SC_018_TC_013).
4. `GET planmigration/processMigrationRequests?child_id=49820&date=2026-09-30` is the month-end cron that executes
   accepted migrations.
   - Documented error: `"Plan migration cron is allowed only on the last day of the month between 11:00 PM and 11:59
     PM"` — a hard time gate.
   - Testing it needs the server clock set inside that window. Whether the `date` param bypasses the gate is not
     confirmed.
   - This replaces the sheet's older `financialprocess/MigrateDataInNewPlan` (SC_018_TC_015). It is the "lock against
     invoicing" point for AC5.

### Confirmed live (2026-10-13, read-only exploration as Rakesh; nothing submitted)

**Account Statement → V2→V1 ("Migrate in Long Term Payment Plan").** V2 children 49851 / 49819 / 49795 were supplied by
the user.

- Header shows `#49851 ANAIKA SINGHAL (ACTIVE) [PAYMENT PLAN : V2]`. Monthly 16,499, Yearly Subscription "Not
  Available". **49851 has Dues 16,499** (invoice due 05 Oct) — likely blocked by the "all invoices paid" rule.
- Button markup:
  `<a class="btn btn-xs btn-primary" href="javascript:migratePlanV1V2newPlan('49851', false);">Migrate in Long Term Payment Plan</a>`.
- **Step 1:** native `confirm("Are you sure you want to migrate plan from V2 to V1?")`. Then
  `POST api/planmigration/migratePlanV1V2newplan` (child_id, user_name, user_id — no `confirm`; "Loading plan details").
  - On ok, it opens modal `#modal_form_migration_request` showing `#display-monthly-amount` / `#display-yearly-amount`,
    with info text in `#frm-plan-migration-request .invoice-text`.
  - On error, the message goes to `.message-plan-migration`.
- **Step 2 (modal confirm):**
  - It requires `#next_yearly_billing_date` (else "Please select annual recurring date").
  - Then native `confirm("Are you sure you want to migrate plan from V2 to V1? The new plan will be effective from next
    month")` → same POST with `confirm=YES` and `next_yearly_billing_date`.
  - The success message goes to `.migration-error-msg`, then the page reloads after 3 s.
- **WebDriver note:** `unhandledPromptBehavior = dismiss and notify`, so an unexpected confirm is auto-dismissed
  (= Cancel). Exploration run 1 hit this; 49851 was verified still V2 afterwards.
- The legacy `migratePlanV1V2()` (`api/billing/migratePlanV1V2`, "Are you sure you want to migrate plan?", Applicable
  From) is still in the JS but was not used by the V2 child's button.

**Support → Payment Plan Migration** (`https://test-franchise.footprintseducation.in/payment_plan_migration`)

- **Filters** (form `#frm_invoice_report`):
  - Migration Status: multiselect `#sr_migration_status` (checkbox values Pending/Informed/Denied/Reminder/Confirmed/
    Completed/Cancelled; button text "No Filters").
  - Migration Month: `#sr_migration_month` ("Aug - 2027" … "Dec - 2019").
  - Center: `#center_selection` (select2, "All Center").
  - Filter button: `#btn_submit`.
- **Table:** DataTable `#dt_migration_list`, 20 per page (NOT 10), 224 entries. Status counts: 217 Pending,
  4 Completed, 1 Informed, 1 Reminder, 1 Confirmed.
  - Columns: Child ID | Child Name (link `pop_child_info`) | Fee/Addons/Coupons Details | Completion Email Status |
    Confirmation Date (Parent) | Confirmed By (Parent) | Migration Status | Migration Date | Migration Comments | Action.
  - Fee cell shows "Existing Annual" (V1 only), "Existing Monthly", "New Monthly" and "Addons".
  - **V2 rows are listed too, with New Monthly = Existing − 2000.** 49851/49819/49795 are all Pending here.
- **The Action column is EMPTY on all 224 rows for Rakesh.** The Update button is `.btn-cancel[data-migration=<base64
  JSON: migration_id, admission_id, diff_amount, annual_billing_date, child_name, migration_status>]`, rendered
  server-side. What renders it is UNKNOWN (right? date window per SC_018_TC_041?).
- **Update modal `#modal_form_cancel`** — "New Payment Plan Migration Request":
  - It shows Child Name and "Annual billing Date"; status `select#action` (Confirmed/Cancelled; the current status
    and Completed are removed); `textarea#migration_comments`.
  - Validation: "Migration status required", "Migration comments required".
  - Note: "* Once the migration status is "Confirmed," it will be processed immediately for the V2 migration."
  - Confirmed → native confirm "If the annual invoice is not paid, it will be marked as void. We will add credits if
    the invoice is paid.\nFor this month, there is a Rs. <diff> rupee charge for upgrade plan." →
    `POST financialprocess/updateCustomerMigrationResponse/` with `action_2_perform=Migration` and
    `request_source=payment_plan_migration` (skips the parent link-expiry + OTP checks).
  - The message goes to `.modal-header`, then reload after 5 s.

**Emails (Email View, Rakesh)**

- Offer: **"New Payment Plan: Zero Annual Fee"** (To father+mother, Cc CD). Reminder: **"Reminder - New Payment Plan:
  Zero Annual Fee"**.
- Body: "your annual invoice will be raised on <date> for your ward <name> (#id)…". Then Existing Plan (Annual Fee Rs
  16,000.00 Due on <date>, Monthly) vs Optional New Plan (Annual Fee: None, Monthly +2,000).
  - "…please confirm by Clicking Here before <date>…"
  - **AC3 text present:** "Please note that once your annual fee for the current cycle has been charged, it is
    non-refundable - this new plan will apply from your next renewal, not the current one."
  - "Click Here in case you do not want to consider this option…"
- Both links point to `https://test.footprintseducation.in//plan_migration.php?q=<hash>`.
- Completion: **"Footprints: Payment plan upgraded for <child>"** ("…migrated to new payment plan as per your
  confirmation", New Plan Annual None / Monthly).
- OTP: **"OTP for Plan Migration"** — **test-env OTP = 1234**.
- Internal: "[Info] New Payment Plan Migration Records Saved in DB (<date>)".
- Opening a body: the row's envelope link `pop_email_view_data_modal?ajax=yes&pop=yes&eid=<id>&db_name=<db>` (direct
  `driver.get` works).

**Parent page** `plan_migration.php?q=<hash>` (viewed for child 62097, not clicked)

- "Offer To Migrate On New Payment Plan - Zero Annual Fee!", "Annual Renewal of 16,000 is Due for <name> (#id) on
  <date>", Existing vs New plan cards.
- Buttons: `#btn_confirm` "Confirm", `#btn_denied` "Not Interested". Footer: "*New fee will be applicable for all
  payable on or after <date>."
- Hidden fields: `#migration_id`, `#admission_id`, `#action`. OTP card: "Please select contact details for OTP" (radios
  call `sendOTP(...)`); `#otp` (4 digits); buttons `#btn_get_submit` → `#btn_otp_submit` (`updateMigrationResponse()`).

### Test-case document (2026-10-13)

`~/Desktop/QA_Documents/Testcases Scripts/TC_Payment_Plan_Migration_13Oct.xlsx` was built from the user's template
`TC_Template_Enanched_13Oct.xlsx`: 43 TCs / 84 step rows, IDs `SC001_TC001`–`SC006_TC008`.

| Scenario | Area |
|---|---|
| SC001 | Payment Plan Migration screen: default view, filters, grid |
| SC002 | `saveMigrationPromotionalData` + offer / reminder emails, incl. AC1 first-year |
| SC003 | Parent page Confirm / Not Interested + OTP |
| SC004 | `processMigrationRequests` cron + AC5 lock / race |
| SC005 | Support Update modal |
| SC006 | Account Statement V2→V1 |

Q1–Q7 are logged in the QueryLogs sheet. The template's wrong list validation on Expected Result (K86:K301) was
dropped in the copy.

`PlanMigration_Test.java` currently holds only TEMPORARY read-only `diagnostic_*` methods; remove them before the real
tests.

**Email verification convention for this feature (user, 2026-10-13):** verify the emails in Email View by filtering
Subject/Body = **"Payment plan"**. Open the email body and **click its links to drive the complete flow** (email link →
parent page → Confirm/Not Interested → OTP).

**V2→V1 popup — confirmed live (2026-10-13, child 49851, step 1 only, nothing saved; DOM also supplied by the user).**
Step 1 opened the popup even though 49851 has an unpaid 16,499 invoice — **no dues block at step 1**. Whether the
confirm step blocks it is not yet known.

- Container: `#modal_form_migration_request` > `.modal-content.modal-loading-content`. Title
  `<h4 class="modal-title">Plan Migration Request - V2 to V1</h4>`.
- Error/success area: `.migration-error-msg`. Form: `#frm-plan-migration-request`.
- Info alert:
  - "You are migrating from V2 (Monthly) to V1 (Annual + Monthly) payment plan. Please review the new plan details
    below."
  - In red: "Once you have applied for a plan migration, you must cancel the existing plan migration request before
    raising any other new request."
- Card "New V1 Plan Pricing": `#display-monthly-amount` "₹ 14,499.00" (= V2 16,499 − 2,000) and
  `#display-yearly-amount` "₹ 16,000.00".
- `select#next_yearly_billing_date` ("When should the new V1 annual plan recurring date?"): placeholder "-- Select the
  1st of month --", then the 1st of the next 12 months (values `2026-11-01` … `2027-10-01`, text "1st of November
  2026"…). Help text: "The new plan will become active from the next month. The selected month will be set for annual
  billing date."
- `.invoice-text`: "Monthly fee of ₹14,499.00 and Annual fee of ₹16,000.00 will be charged from the next month."
- Buttons: Cancel (`data-dismiss`), and **"Confirm Migration"** `button.btn-model-migrate` →
  `migratePlanV1V2newPlan(49851, true)`.

**V2→V1 full flow — EXECUTED live (2026-10-13, child 49851, user-approved, annual date "1st of November 2026" =
`2026-11-01`). 49851 is now CONSUMED (has a pending Plan Migration request).**

- Confirm 1: "Are you sure you want to migrate plan from V2 to V1?" Then select `2026-11-01` → "Confirm Migration".
- Confirm 2: "Are you sure you want to migrate plan from V2 to V1? The new plan will be effective from next month".
  Both were accepted.
- The success message in `.migration-error-msg` was NOT captured: the page reloads ~3 s later and my 9 s poll got
  nothing. The exact success text is still unknown, so the next run must read it right after the alert.
- **Unpaid dues (16,499) did NOT block the request.** This contradicts the "all invoices must be paid" rule; a candidate
  bug/query for dev.
- **Result:**
  - **Recent Customer Requests** (`recent_update_details?child_id=49851`, also the CUSTOMER REQUESTS icon on Account
    Statement): Request Type **"Plan Migration"**, Request Status **Pending**, Approval Status NA, Center Nirvana
    Country, **WEF Date Oct 31, 2026**.
  - The requirement said "immediately processed … customer request with Approved status", but live it is **Pending**,
    presumably processed by the month-end `processMigrationRequests` cron.
- **Account Statement right after:** still `[PAYMENT PLAN : V2]`; monthly 16,499 (Next Billing 01 Nov 2026); Yearly
  "Not Available". The "MIGRATE IN LONG TERM PAYMENT PLAN" button is STILL shown, although the popup says an existing
  request must be cancelled before a new one.
- Email View (Subject "Payment plan" + Body 49851) found only the internal "[Info] New Payment Plan Migration Records
  Saved in DB" email. No parent confirmation email yet, presumably sent when the request is processed.
- Also found in Email View: a separate V2-direction offer subject **"New Payment Plan: Annual Fee Applies"** (+
  "Reminder - …").
- User instruction for the test: print the `.invoice-text` note ("Monthly fee of ₹14,499.00 and Annual fee of
  ₹16,000.00 will be charged from the next month.") to the console/report. The flow is Apply → then open Customer
  Request.
- **`processMigrationRequests` confirmed (2026-10-13):** a call with `child_id=49851&date=2026-10-31` (the request's
  WEF) returned HTTP 200 `{"status":"error","error":"Plan migration cron is allowed only on the last day of the month
  between 11:00 PM and 11:59 PM"}`. **The `date` param does NOT bypass the gate**; the server clock must be set to the
  last day of the month, 11:00–11:59 PM, to process. Tests treat this response as informational (same pattern as other
  timing-gated APIs).

**V2→V1 processed — CONFIRMED end-to-end (2026-10-31 server time, child 49851).** The user set the server clock to Oct
31 11 PM and ran `processMigrationRequests?child_id=49851&date=2026-10-31` →
`{"status":"ok","message":"Processed 1 migration requests","processed":1,"approved":1,"failed":0,"email_sent":"Yes","date":"2026-10-31"}`.
An immediate re-run → `"No migration requests to process for today"` (no double-processing).

Checks to run after EVERY migration (user instruction), all in `PlanMigration_Test.verifyProcessedV2ToV1Migration()`:

1. **Customer Requests:** Plan Migration → **Approved** (Approval Status NA, WEF Oct 31, 2026).
2. **Account Statement:**
   - header `<legend>…<span class="">[Payment Plan : V1]</span>`.
   - Monthly Subscription: "Tuition Fee - Half Day Program", 14499.00 (Monthly) = V2 16,499 − 2,000.
   - Yearly Subscription: "Annual Fee - Half Day Program", **16000.00 (Yearly)**, **Next Billing Date 01 Nov, 2026** (=
     the annual recurring date chosen).
   - The "Migrate in Long Term Payment Plan" button is gone (and there is no V1→V2 button either).
3. **Child History** (popup "Child Updates History"):
   - **"Plan Migration Completed"** (2026-10-31 23:00:05) — "Migration: V2 -> V1 | Length: Half Day | Effective Date:
     2026-11-01 | Monthly Plan ID: ALINC-monthly-fee-14499 | Annual Plan ID: annual-fee-16000".
   - Plus "Plan Migration Request" (Rakesh Singh, 2026-10-13) — "Plan Migration Request Created : Migration: V2 -> V1 |
     Length: Half Day | Annual Recurring Date: 2026-11-01".
   - The existing `getChildHistoryFullText()` XPath matches `.modal-title` only (header text). The new
     `waitForChildHistoryText()` reads the page text from "Child Updates History" onward.
4. **Email View:** Subject **"Plan Migration Report V2 -> V1 - All Centers - 31 Oct 2026"** (internal, To Rakesh).
   - Its table: # | Center Name | Child Name | Child ID | Program | Effective Date | Monthly Plan ID | Annual Plan ID |
     Yearly Subscription ID.
   - The row is "Nirvana Country | Anaika Singhal | 49851 | Half Day | 01 Nov 2026 | ALINC-monthly-fee-14499 |
     annual-fee-16000 | 49851_annual_103126"; "Total Migrations Processed: 1".
   - The subject date = the request's WEF date.

**Open (query for dev):** no PARENT confirmation email was found for 49851. Neither "Confirmation of payment plan
migration" (spec subject) nor "Footprints: Payment plan updated for Anaika Singhal" (the subject seen for another V2→V1
child, Ariv Kudale, 01 Oct) exists, despite `email_sent: Yes`.

**Tests:**

- `tc001`–`tc003` passed on 49819 (read-only).
- `tc004` (date-required validation) has not been run since its fix.
- `tc005` (full flow, CONSUMES the child; skips verification as informational if the cron gate blocks) has not been run.
- `tc006` (read-only verification; `-DprocessedChildId/-DprocessedAnnualDate/-DprocessedMonthlyAmount`, defaults
  49851 / 2026-11-01 / 14499.00) **PASSED**.
- Observed: 49819's popup monthly = ₹13,319 (16,799 − 2,000 = 14,799, less its 10% inaugural discount), so the
  discount carries over.

**Run 2026-10-31 (server time), child 49819 — tc004 + tc005 + tc006.** 49819 is now CONSUMED (V1).

- `tc005`:
  - The popup's first annual-date option was **2026-12-01** (server date Oct 31). That was selected.
  - The request went Pending (WEF Oct 31, 2026) → cron `processed:1, approved:1, email_sent:Yes` → **Approved**.
  - Result: V1; monthly **14799.00** (= undiscounted V2 16,799 − 2,000); yearly 16000.00.
  - Child History: "Plan Migration Completed … Length: Full Day | Effective Date: 2026-11-01 | Monthly Plan ID:
    ALINC-monthly-fee-14799 | Annual Plan ID: annual-fee-16000".
  - Report email "Plan Migration Report V2 -> V1 - All Centers - 31 Oct 2026" lists 49819 (Gokhale Vihar Marg,
    Butler Colony | Hetansh Kumar | Full Day | 01 Nov 2026 | … | 49819_annual_103126).
- **🐛 Likely BUG — the chosen annual recurring date is ignored.**
  - The request entry records "Annual Recurring Date: 2026-12-01", but the processed Yearly Subscription shows **Next
    Billing Date : 01 Nov, 2026**.
  - That is the effective date, not the chosen date, contrary to the popup's own help text "The selected month will be
    set for annual billing date."
  - It was hidden on 49851 because 2026-11-01 was chosen there. The shared check asserts the spec'd behaviour and fails
    on 49819 until fixed. Report to dev/JIRA.
- **Popup monthly ≠ plan amount.** The popup shows the fee AFTER discounts (₹13,319 = 14,799 less a 10% inaugural
  discount); Account Statement's Plan Amount is undiscounted (14,799). `tc005` now expects (monthly before − 2,000) from
  Account Statement.
- **flash_message locator fix:** messages render in a sibling `<div class="flash-msg"><div class="alert …">` after
  `.migration-error-msg` / `.message-plan-migration`, not inside it. `tc004` now **PASSES** on 69923 ("Please select
  annual recurring date").
- History "Effective Date" = WEF + 1 day (the plan start), not the annual date. The check now asserts "Annual Recurring
  Date: <chosen>" on the request entry instead.
- New V2 child supplied: **69923** (only used read-only by tc004 so far — still fresh).
- **User check (2026-10-13):** 49819's Yearly Next Billing Date shows 01 Nov, 2026 — confirmed. The user suspects the
  server-time change may be the cause: 49819's request was raised AND processed the same night with the server clock at
  Oct 31 11 PM, which also made the first option 1st of Dec.
- **Status: SUSPECTED bug, pending a clean retest.** Raise V2→V1 for **69923** at a normal server date, choosing a later
  month (e.g. 2027-01-01). Move the clock to month-end 11 PM only for the cron. Then verify the Yearly Next Billing Date
  = 01 Jan, 2027. Raise the JIRA only if it still shows the plan start date.
- **Retest step 1 done (2026-10-13, normal server date):** `tc005` on **69923** with `-DannualDate=2026-12-01`.
  - Success message (now captured): **"Plan migration request saved successfully! Request will be processed on 31 Oct,
    2026"**.
  - Customer Request: Plan Migration Pending, WEF Oct 31, 2026.
  - Monthly before 12,999 → expected 10,999; the popup showed ₹9,899 (discounted). Cron gate → informational.
  - Next: the user sets the server to Oct 31 11 PM → run `tc006 -DprocessedChildId=69923 -DprocessedAnnualDate=2026-12-01
    -DprocessedMonthlyAmount=10999.00` after the cron.
- **Expected behaviour clarified by the user (2026-10-13):** if the annual recurring date selected = 1 Dec, then **WEF
  should be 30 Nov** and the cron processes the request on 30 Nov. That makes the plan effective / the yearly Next
  Billing Date 01 Dec.
  - **Actual:** WEF is always the last day of the CURRENT month (Oct 31), whatever date is selected. Seen on 69923
    (raised at a normal server date: "Request will be processed on 31 Oct, 2026"; Customer Request WEF Oct 31, 2026;
    history "Annual Recurring Date: 2026-12-01") and 49819 (processed Oct 31 → effective 2026-11-01, yearly Next
    Billing 01 Nov, 2026).
  - **BUG CONFIRMED.** The user is logging it in JIRA.
  - `tc005` now asserts WEF = selected annual date − 1 day.
  - 69923 is Pending with the wrong WEF (Oct 31) — do not reuse for a clean run until the fix.
- **Reproduced again (2026-10-13, normal server date) on 62853:**
  - Selected 2026-12-01 → "Plan migration request saved successfully! Request will be processed on 31 Oct, 2026".
  - Customer Request Pending, **WEF Oct 31, 2026** (expected Nov 30, 2026).
  - `tc005` fails exactly here: `expected [2026-11-30] but found [2026-10-31]`.
  - Monthly before 11,799; popup ₹9,799 (no discount). 62853 is now Pending with the wrong WEF.
- **Bug evidence screenshots (2026-10-14):** saved in `~/Desktop/QA_Documents/Bug Evidence/PlanMigration_WEF_AnnualDate/`
  (01–06: 62853 Customer Request WEF Oct 31; 62853 history "Annual Recurring Date: 2026-12-01"; 49819 Customer Request;
  49819 Yearly Next Billing 01 Nov 2026; 49819 history Effective Date 2026-11-01 vs Annual Recurring Date 2026-12-01;
  report email). The temporary `evidence_*` test method was removed.
- **Parent-email bug evidence (2026-10-14, read-only):** Email View for 49851 and 49819, searching Subject "Payment
  plan" / "Confirmation of payment plan" / "Plan Migration" × Body = child ID / child first name.
  - Only the internal "Plan Migration Report V2 -> V1 - All Centers - 31 Oct 2026" (To rakesh.singh@…) and an old
    "[Info] … Records Saved in DB (17 September, 2026)" appear. There is **no parent-addressed email**, although the cron
    returned `email_sent: Yes`.
  - Screenshots: `~/Desktop/QA_Documents/Bug Evidence/PlanMigration_ParentEmail/`.
- **Bug reports written (2026-10-14)**, for the user to log in JIRA; all in `~/Desktop/QA_Documents/Bug Evidence/`:
  - Bug 1 (WEF/annual date ignored) — text in chat, screenshots `PlanMigration_WEF_AnnualDate/`.
  - Bug 2 (no parent email) — `Bug2_PlanMigration_ParentEmail.md`, screenshots `PlanMigration_ParentEmail/`.
  - Bug 3 (dues not blocking) — `Bug3_PlanMigration_DuesNotBlocking.md`, screenshots
    `PlanMigration_DuesNotBlocking/`. 49851 still has unpaid PI/999905 (₹16,499, due 05 Oct 2026, Dues ₹16,499) yet
    was migrated to V1.
- **All 3 bugs logged in JIRA by the user (2026-10-14)** — JIRA IDs not yet supplied. Until they're fixed:
  - `tc005` fails on WEF (Bug 1).
  - `tc006` fails for any child whose chosen annual date ≠ the 1st of next month (Bug 1).
  - No automated assertion exists yet for the parent email (Bug 2) or the dues block (Bug 3).

### V2→V1 migration via parent EMAIL (renewal offer) — started 2026-10-14

The user asked to leave Bugs 1–3 until dev fixes them. The flow:

1. The offer email is sent ~one month before the child completes 1 year, based on the Joining Date (cron 23rd/24th,
   ~7 days before renewal).
2. The parent clicks the link and chooses Confirm / Not Interested with an OTP.
3. The request is processed by the month-end cron.

- **Example child 62654 (Raghavi Nelkudri), read-only 2026-10-14 (server Oct 14 12:30 PM):**
  - V2; Tuition Fee - After School Program 10,299 (Monthly), next billing 01 Nov 2026; Yearly "Not Available"; **Dues
    10,299**.
  - Child info popup (`pop_child_info?&child_id=62654`, link
    `<a class="btn-link text-primary" href="pop_child_info?&child_id=…" title="Child Information">`) shows "November 21,
    2025" and **"November 24, 2025" = Joining Date** (`div.col-xs-8.col-sm-7.mb-10`).
  - Not yet on the Payment Plan Migration screen. Only internal "[Info] … Records Saved in DB" emails mention it.
- **V2 offer email = "New Payment Plan: Annual Fee Applies"** (+ "Reminder - New Payment Plan: Annual Fee Applies"). To
  father+mother, Cc CD. Body (child 49820 example):
  - "your ward <Name> (#id)'s current payment plan does not carry an annual fee. From your next renewal on <1st Month,
    Year>, Footprints is offering the option to switch to our standard plan, which carries a lower monthly fee alongside
    an annual fee."
  - Existing Payment Plan: Annual Fee None, Monthly X. Optional New Payment Plan: Annual Fee Rs. 16,000.00, Monthly X −
    2,000.
  - "If you switch, you would pay Rs. 2,000.00 less per month, and an annual fee of Rs. 16,000.00 would apply from
    <date> onward."
  - "please confirm by Clicking Here before <date>"; "Click Here in case you do not want to consider this option…".
  - Both links → `test.footprintseducation.in//plan_migration.php?q=<token>`.
- **Plan:**
  - The user sets the server date to 24 Oct 2026, then call
    `saveMigrationPromotionalData/?send_promotional_email=yes&child_id=62654`.
  - Check the PPM row = Informed, the email content, then the link → Confirm → OTP 1234 → Confirmed.
  - Cron on Oct 31 11 PM, then `verifyProcessedV2ToV1Migration`.
  - Decline path on another child → Denied.
- **Offer sent for 62654 (server Oct 24 2026 06:30 PM):** `saveMigrationPromotionalData/?send_promotional_email=yes&child_id=62654`
  → HTTP 200 `{"status":"ok","message":"Promotional email sent successfully to 1 Child"}`. **`tc007_v2OfferEmailSent`
  PASSED.**
  - **Email** "New Payment Plan: Annual Fee Applies" (24 Oct 2026 18:30): To d_62654_father/mother, CC
    Archana.Kadiwal@center (CD).
    - "From your next renewal on **1st November, 2026**…"; Existing: Annual None / Monthly 10,299.00; New: Annual Rs.
      16,000.00 / Monthly 8,299.00; "you would pay Rs. 2,000.00 less per month".
    - "please confirm by Clicking Here before **30th October, 2026**".
    - **NEW 3rd option:** "Click Here if you want to think about it and want us to reminder you later. Or Click Here in
      case you do not want to consider this option…" — typo "reminder you later".
    - All links → `plan_migration.php?q=7715373f7e42b992e9360e3521dc8c26`.
    - The V2 email has **no non-refundable sentence** (that is in the V1 "Zero Annual Fee" email only).
  - **Payment Plan Migration grid defaults Migration Month to the FIRST option ("Aug - 2027")**, so a test must select
    the renewal month ("Nov - 2026") + Filter before searching.
  - Row: "62654 | Raghavi Nelkudri | Existing Monthly 10,299 / New Monthly 8,299 | Pending | Informed | **Update**". The
    Update action IS shown for this Informed row, which partially answers open query Q2.
- **Email View convention (user, 2026-10-14):** filter by Subject (e.g. "New Payment Plan: Annual Fee Applies"), then
  **sort the "Created Date" column descending** so the newest email is first (+ Body = child ID as an extra filter).
  Implemented in `PlanMigration_Test.sortEmailViewByCreatedDateDesc()` (clicks the header until
  `aria-sort="descending"`), used by `openEmailBySubjectAndBody()`. `tc007` re-PASSED with it.
- **`tc007` extended (PASSED):** a full 13-line email content check (values computed: existing monthly from Account
  Statement, new = −2,000, renewal date from the email, confirm-by = renewal − 2 days). Child name read via
  `textContent` (Account Statement renders it CSS-uppercased).
  - **3 blue links** (rgb 47,111,208), ALL the same URL `plan_migration.php?q=<token>`:
    1. "Clicking Here" = confirm/reject
    2. "Click Here" = remind me later
    3. "Click Here" = not interested
  - Content issues to raise: typo "want us to **reminder** you later"; CD name spelt "Kadiwal" (CC) vs "Kadival"
    (signature).
- **Parent page for 62654 (read-only, 2026-10-14):**
  - "Offer To Migrate On New Payment Plan", "Dear Santhosh R Nelkudri & Channamma S", "A New Payment Plan is available
    for Raghavi Nelkudri (#62654) from November 01, 2026". Existing (Annual None / Monthly ₹10,299) vs New (Annual
    ₹16,000 / Monthly ₹8,299).
  - **3 buttons:** `#btn_confirm` "Confirm", `#btn_reminder` "Remind Me Later" (new), `#btn_denied` "Not Interested"
    (all `.btn-migrate`). Footer "*New fee will be applicable for all payable on or after November 01, 2026."
  - Popup (a 2nd element with id `fee-modal`, title "Payment Plan"):
    - Message (static HTML seen: "Are you sure want to confirm to migration in new payment plan? You annual plan will
      not be charged on November 01, 2026." — V1→V2 wording; verify live after clicking Confirm before calling it a
      bug).
    - → `#btn_get_submit` "Continue" (`showContactDetails()`) → "Please select contact details for OTP": radios
      Father/Mother (`onchange=sendOTP(<id>,'<phone>','F'|'M')`), masked email/phone.
    - → `#otp` (4 digits, "OTP is valid for 10 Minutes", Resend) → `#btn_otp_submit` "Continue"
      (`updateMigrationResponse()`).
    - Hidden fields: `#migration_id` (9050), `#admission_id`, `#action`, `#verified_by`, `#otp_password`,
      `#user_id`=Parent.
  - Handlers live in an external script (no inline code).
- **Parent responses — `tc008_parentRespondsToOffer` (`-DofferChildId -DparentAction=confirm|reminder|denied`), server Oct
  24 2026:**
  - **Rocket Loader:** the page's onclick handlers are guarded by `if (!window.__cfRLUnblockHandlers) return false`, so
    early clicks do nothing. The test waits for `__cfRLUnblockHandlers` + jQuery and retries the click.
  - **62654 Confirm — PASSED.**
    - Popup "Payment Plan": "Your annual plan will not be invoiced on November 01, 2026" → Continue → "Please select
      contact details for OTP" → Father (`#customRadioInline2`) → "OTP has been sent successfully" → OTP 1234 →
      Continue.
    - Result: "Thanks, your new payment plan will be applied from next month."
    - Row: Confirmation Date (Parent) 24 Oct, 2026 | Confirmed By (Parent) 1377446720 | **Confirmed** | Update.
  - **62671 Not Interested — PASSED.**
    - Popup: "Your Annual Fee will be invoiced on November 01, 2026" → OTP flow.
    - Result: "Thanks, your annual fee will be invoiced on November 01,2026" (no space after the comma).
    - Row: 24 Oct, 2026 | 1016639668 | **Denied**.
  - **62664 Remind Me Later — WORKS.**
    - Popup: "We will send you reminder for this again" → Continue → **NO OTP step**. Page alert "Thanks, your
      reminder request registered successfully"; the Remind Me Later button becomes disabled (a 2nd test run therefore
      times out on clicking it — expected).
    - Row: **Reminder** (no confirmation date/by).
  - **🐛 Content issue (V2→V1 popups use V1→V2 wording):**
    - Confirm says "Your annual plan will **not** be invoiced on November 01, 2026", but for V2→V1 the ₹16,000 annual
      fee WILL start.
    - Not Interested says "Your Annual Fee **will** be invoiced on November 01, 2026" / "Thanks, your annual fee will
      be invoiced…", but a V2 child who declines stays on V2 with NO annual fee.
    - The two messages are inverted for the V2 direction. Candidate Bug 4.
  - **Test data now:** 62654 Confirmed (awaiting cron Oct 31 11 PM); 62671 Denied; 62664 Reminder (awaiting reminder
    email API).
- **Reminder email for 62664 (2026-10-14):** `sendMigrationPromotionalReminderEmail?days=3&child_id=62664` → HTTP 200,
  plain text **"Reminder Send Successfullynull"** (not JSON; stray "null").
  - Only 62664's reminder was created in that run, so `child_id` appears to be respected.
  - Email **"Reminder - New Payment Plan: Annual Fee Applies"** (To d_62664_father/mother, CC renuka.s@center =
    Renuka Sarkar): the same content as the offer, but the "remind me later" option is dropped — only "Clicking Here"
    (confirm) + "Click Here in case you do not want to consider…".
  - Created **14 Oct, 2026 13:26** — the server clock was back at the real date by then (offer emails were 24 Oct).
  - **`tc009_v2ReminderEmailSent` PASSED** (13 content checks + asserts no "remind" text).
- **Cron for the parent-email path (server Oct 31 2026 11 PM):**
  `processMigrationRequests?child_id=62654&date=2026-10-31` →
  `{"status":"ok","message":"Processed 1 migration requests","processed":1,"approved":1,"failed":0,"email_sent":"No","date":"2026-10-31"}`.
  **`tc010_verifyParentEmailMigrationProcessed` PASSED** for 62654:
  - Customer Request "Plan Migration" Approved (WEF Oct 31, 2026). [Payment Plan : V1]. Monthly 8,299.00. Yearly
    16,000.00 with **Next Billing Date 01 Nov, 2027**.
  - **The annual invoice is raised immediately at processing:** ledger "Invoice charged on 31 October, 2026 11:00 PM (Due
    date 04 Nov, 2026)" with line "**Annual Fee Invoice Amount Against V2 to V1 Migration**" (the spec said "Annual Fee
    Against V2 to V1 Migration"). The yearly next billing therefore moves to next year.
  - Child History entries are titled "**Plan Migration**" (NOT "Plan Migration Completed" as in the Support path):
    - "Auto: on 31 October, 2026 11:00 PM, the New Payment Plan (V1) was migrated via parent email confirmation. The
      request was approved by the Father, and on 24 October, 2026 06:46 PM confirmation was completed."
    - "Auto: Request for new payment plan migration confirmed by Father on 24 October, 2026 06:46 PM - queued for
      month-end processing".
  - **Parent email IS sent here** (despite `email_sent: No`): "**Footprints: Payment plan updated for Raghavi
    Nelkudri**" 31 Oct 23:00, To d_62654_father/mother, CC CD — "This is to inform that you have been migrated to new
    payment plan as per your confirmation. New Payment Plan Annual Fee: Rs. 16,000.00 Monthly Fee: Rs. 8,299.00".
  - **No internal "Plan Migration Report V2 -> V1" email** for this run (only the earlier 49851/49819 reports).
  - Open questions for dev:
    - The Support path did NOT raise an immediate annual invoice for 49851/49819 (yearly next billing 01 Nov 2026) —
      which is right?
    - Is the missing report email for the parent-email path intended?
- **`tc011_verifyDeniedChildUnchanged` PASSED** for 62671 (after the cron): the row still Denied; V2; monthly 12,499.00
  unchanged; yearly "Not Available"; no "Against V2 to V1 Migration" invoice.
- `tc007`–`tc011` cover the parent-email V2→V1 flow end-to-end.
- **User decision (2026-10-14):** leave the small content issues — no bug reports for them: the "reminder you later"
  typo, Kadiwal/Kadival, `email_sent: No` vs the sent parent email, and "Reminder Send Successfullynull". Bug 4 (the
  reversed V2 popup messages) was written up for JIRA.
- **Bug 4 screenshots** copied to `~/Desktop/QA_Documents/Bug Evidence/PlanMigration_V2PopupWording/`:
  01 62654 Confirm popup "Your annual plan will not be invoiced…"; 02 Confirm result; 03 62671 Not Interested popup "Your
  Annual Fee will be invoiced…"; 04 Not Interested result "Thanks, your annual fee will be invoiced on November 01,2026".
- **Link expiry CONFIRMED (2026-10-14, server at Oct 31 11 PM):** opening 62664's reminder-email link
  (`plan_migration.php?q=73cd55771cacbf63b59548b7a646332a` — different token from the offer email) shows only a yellow
  alert: "**This link has expired. Please contact your Centre Directress if you'd still like to change your payment
  plan.**" No Confirm/Remind/Not Interested buttons. The email's deadline was "before 30th October, 2026".
  Screenshot: `screenshots/plan_migration/tc008_62664_link_expired_Oct31.png`. Covers SC003_TC006 / SC_018_TC_040.
  62664 is still in Reminder status; confirming it needs the server date set back before Oct 30.
  `tc008` now accepts `-DofferSubject` (e.g. "Reminder - New Payment Plan: Annual Fee Applies").
- **Terminology (user, 2026-10-14):** "Account Statement path" = Support/Rakesh migrates from the Account Statement
  button ("Migrate in Long Term Payment Plan") → cron. "Parent-email path" = the parent confirms from the offer email →
  cron. Do not call it the "Support path".
- **Annual invoice — parent-email vs Account Statement path (read-only, 2026-10-14).** Screenshots:
  `~/Desktop/QA_Documents/Bug Evidence/PlanMigration_AnnualInvoice_EmailVsAccountStatement/`.
  - **62654 (parent email):** **PI/1002267**, "Invoice charged on 31 October, 2026 11:00 PM (Due date 04 Nov, 2026)",
    ₹16,000.00 = Daycare Fee "Annual Fee Invoice Amount Against V2 to V1 Migration" 13,559.32 + SGST 9% 1,220.34 +
    CGST 9% 1,220.34. Yearly Next Billing 01 Nov, 2027.
  - **49851 / 49819 (Account Statement):** NO migration annual invoice. The last invoices are the Oct monthly ones
    (PI/999905 01 Oct 06:44 PM for 49851; 01 Oct 06:49 PM for 49819). Yearly Next Billing 01 Nov, 2026.
  - Question for dev: which behaviour is correct?
- **Test fix:** `openAccountStatement()` now verifies the `<legend>` header shows "#<childId> " and falls back to
  Admission ID + Generate Report. Confirmed live once: `account_statement?child_id=49851` rendered #49819 (the
  previously viewed child) — intermittent.
- **Rechecked (2026-10-14), every ledger invoice row containing "annual" (case-insensitive):**
  - 62654: only PI/1002267 "Annual Fee Invoice Amount Against V2 to V1 Migration" ₹16,000.00 (31 Oct 2026).
  - **49851 and 49819: none at all.** Their Oct invoices are monthly Tuition/Preschool Fee. So the Account Statement
    path raised no annual invoice — confirmed. Identify migration annual invoices by the text "Annual Fee Invoice Amount
    Against V2 to V1 Migration" (user).
- **BY DESIGN (user, 2026-10-14) — the dev question is closed:**
  - **Account Statement path** ("Migrate in Long Term Payment Plan" button): NO annual invoice at processing; the
    Yearly Subscription only shows the next annual billing date (the annual fee is billed then).
  - **Parent-email path:** the annual invoice ("Annual Fee Invoice Amount Against V2 to V1 Migration") is raised
    IMMEDIATELY at processing, and the yearly Next Billing Date is set to next year automatically.
  - Asserted in `verifyProcessedV2ToV1Migration` (no such invoice) and `tc010` (the invoice is present + next billing
    = renewal + 1 year).
- **Bug 2 CLOSED — by design (user, 2026-10-14):** the Account Statement path sends NO parent email after migration.
  Only the parent-email path sends "Footprints: Payment plan updated for <child>". `Bug2_PlanMigration_ParentEmail.md`
  is marked CLOSED; the user should close the JIRA.
  `verifyProcessedV2ToV1Migration` now asserts no such parent email exists. Report-email lookup now uses Body = child ID
  (several "…All Centers - 31 Oct 2026" reports exist). `tc006` re-PASSED for 49851.
- **V2→V1 parent-email recheck children (user, 2026-10-14), read-only pre-check at server Oct 31 11:5x PM.** All are V2,
  with Nov 2026 Payment Plan Migration rows in **Pending** (saved earlier, no offer sent):

  | Child | Name | Monthly | Joining | Dues |
  |---|---|---|---|---|
  | 62700 | Sadiq Abbas | 10,999 Half Day | 24 Nov 2025 | 10,999 |
  | 62694 | Advita Giri | Account Statement **17,799 Full Day**, but the PPM row says **12,499→10,499 (stale — plan changed after the row was saved; check the offer email amount)** | 24 Nov 2025 | 17,799 |
  | 62806 | Yatika Thakur | 16,499 Half Day | 28 Nov 2025 | 32,998 |
  | 62730 | Yashika Dubey | 8,999 Half Day | 24/26 Nov 2025 | 8,999 |

  - 62806 is a transfer case from #61172.
  - **Name lookup fix:** the legend has a 2nd `pop_child_info` link ("Transfer Case From #61172"). The name is now read
    from the link whose href has `child_id=<id>`.
- **V1 candidates for later V1→V2** (PPM Nov - 2026, V1, Pending): 34643, 39951, 41206/41207 (siblings), 41640, 41644/
  41646, 41667, 43870, 48982, 49314, 49386, 49603, 49733, 49849, 49937, 49950, 50040/50041, 50115/50117, 50119, 50135,
  50140, 50176. Joining dates are not yet checked for first-year eligibility.
- **Recheck run (server Oct 24 2026):** offers sent for 62700/62694/62730/62806 with
  `…&child_id=<id>&date=2026-10-24` → all "Promotional email sent successfully to 1 Child".
  - `tc007` PASSED for 62700, 62730, 62806 (13/13 lines, Informed). The 62700 first attempt hit a one-off Account
    Statement load timeout; the rerun passed.
  - `tc008`:
    - 62700 Confirm → **Confirmed** (24 Oct, 1879501037)
    - 62730 Remind Me Later → **Reminder**
    - 62806 Not Interested → **Denied** (24 Oct, 1894143316)
- **🐛 Bug 5 (candidate) — 62694 (Advita Giri): a stale migration record means the parent's Confirm is silently not
  saved.**
  - The record was saved earlier with Existing Monthly 12,499 → New 10,499. The child's plan has since changed (Account
    Statement: Tuition Fee - Full Day 17,799).
  - The offer email was sent with the CURRENT amounts (Existing 17,799 → New 15,799), but the record was NOT updated:
    still 12,499/10,499, status stays **Pending** (never Informed).
  - Parent Confirm + OTP 1234 showed "Thanks, your new payment plan will be applied from next month.", but the row
    stays **Pending** with no Confirmation Date/Confirmed By.
  - Only one row exists for 62694 across all 2026–2027 months.
  - Evidence: `~/Desktop/QA_Documents/Bug Evidence/PlanMigration_StaleRecord_ConfirmNotSaved/` (00 Account Statement
    17,799; 01 offer email 17,799→15,799; 02 popup; 03 OTP; 04 success message; 05 row still Pending).
  - `getPaymentPlanMigrationRow()` now returns all matching rows joined by " ||| " and warns when there is more than
    one.
- **62730 reminder path (server Oct 28 2026 06:31 PM):**
  - The reminder API → "Reminder Send Successfullynull". `tc009` PASSED (reminder email 28 Oct 18:31, 13/13).
  - `tc008` Confirm from the REMINDER email link (`q=fa45f831…`, its own token) → OTP 1234 → "Thanks, your new payment
    plan will be applied from next month." → row **Confirmed** (28 Oct, 2026 | 1366353774).
- **Email View gotcha:** sorting by "Created Date" is NOT chronological ("29 Sep, 2026" sorted above "28 Oct, 2026" —
  a text sort on "dd Mon, yyyy"). Always also filter Body = child ID (as `openEmailBySubjectAndBody` does); don't rely on
  sort order alone.
- **Recheck state before the cron:** 62700 Confirmed | 62730 Confirmed (via reminder) | 62806 Denied | 62694 Pending
  (Bug 5 — confirm not saved). Next: server Oct 31 11 PM → cron for all 4 → `tc010` for 62700/62730, `tc011` for
  62806 and 62694 (expect it unchanged, V2).

**Dev-confirmed enhancement (shared by the user 2026-10-14) — `setAnnualBillingDateAfterPlanMigration` in
`planmigration.php`:**

- This is the cron on the **1st of each month** that updates `next_yearly_billing_date` after a plan migration.
- **New validation:** only update a child if the billing DB table `billing_customer_invoice` has a row for the child
  with `subscription_type = 'Yearly'` AND `invoice_start = <first day of the month, YYYY-MM-01>`. Otherwise skip the
  child (`next_yearly_billing_date` unchanged).
- **Scope:** `wp_pods_customer_request` rows with `type = 'Plan Migration'`, `status = 'Approved'`, `effective_date =
  last day of the month` (+ the function's existing date logic).
- **Ties in with the BY-DESIGN Account Statement path:**
  - No annual invoice at processing (Oct 31). The regular billing raises the Yearly invoice on Nov 1.
  - This cron then moves the yearly Next Billing Date (01 Nov 2026 → 01 Nov 2027) — but only if that Yearly invoice
    exists.
- **Endpoint / trigger** for this cron and for the 1st-of-month billing run: NOT yet supplied.
- **Server-time gotcha (2026-10-14):** the user set "31 Oct 11 PM", but the portal header showed **Nov 1, 04:30 AM**.
  The clock was set in UTC; the portal and the cron use IST (+5:30). The cron gate kept refusing.
  - Always confirm via the portal header that it shows "Oct 31, 11:xx PM" (IST) before running
    `processMigrationRequests`.
  - Risk: while the server sat on Nov 1, 1st-of-month billing jobs may have run. Check the ledgers for 1 Nov invoices.

### V1→V2 — children supplied by the user (2026-10-14), read-only pre-check (server Nov 1 04:30 AM)

| Child | Name | Plan | Monthly | Yearly (Next Billing) | Annual invoice(s) | Joining | Dues | PPM row |
|---|---|---|---|---|---|---|---|---|
| 67748 | Tejasv Jain | V1 | 7,749 Half Day | 16,000, 01 Apr 2027 | FP/P067/2025-26/1936, 31 Mar 2026, "Annual fee for Preschool April, 2026 - March, 2027" 15,000 | 31 Mar / 2 Apr 2026 | 4,749 | none |
| 68078 | Manas Mishra | V1 | 6,399 After School | 16,000, 01 Aug 2027 | P476/2627/129, 01 Aug 2026, 16,000 | 8 Apr 2026 | 6,399 | none |
| **68557** | Shreshth Tiwari | **V2 (not V1)** | 12,749 | none | P397/2627/366 prorated annual – Center Shift 3,112 | 21 Apr 2026 | 12,749 | none |
| 68891 | Daivik Bhola | V1 | 9,999 Half Day | 16,000, 01 Jul 2027 | P491/2627/27 prorated (Center Shift) 2,718 + P491/2627/82, 01 Jul 2026, 16,000 | 30 Apr 2026 | 9,999 | none |

- **All four joined Mar–Apr 2026, so they are FIRST-YEAR children** (as of Oct/Nov 2026). Per AC1 they must NOT get
  the V1→V2 offer, which makes them suited to the AC1 negative test, not the positive flow.
- Their annual renewals are Apr/Jul/Aug 2027.
- For positive V1→V2: use 2nd-year V1 children with a Nov 2026 renewal (PPM Nov - 2026 V1 rows, e.g. 34643, 39951,
  41667, 43870, 49733, 50176) — pending user confirmation.
- **Cron for the recheck (portal header Oct 31 11:05 PM IST):**
  - 62700 / 62730 / 62694 → "Processed 1 migration requests … approved 1, email_sent No".
  - 62806 → "No migration requests to process for today" (Denied).
  - **`tc010` PASSED** for 62700 (8,999), 62730 (6,999) and **62694 (15,799 = current 17,799 − 2,000, NOT the stale
    10,499)**: Approved, V1, yearly 16,000 next 01 Nov 2027, annual invoice "Annual Fee Invoice Amount Against V2 to V1
    Migration" 31 Oct 2026 11:00 PM.
  - **`tc011` PASSED** for 62806 (V2 16,499 unchanged, no yearly).
- **After the cron, Payment Plan Migration rows:**
  - 62700 and 62730: "Sent | <confirm date> | <phone> | **Completed** | 31 Oct, 2026".
  - **62694: STILL "12,499 / 10,499 | Pending | Pending | Update"**, although its migration was processed and
    approved.
- **Bug 5 REVISED:** the parent's Confirm and the migration both work and use current amounts. The defect is that the
  Payment Plan Migration screen record for a child whose plan changed after the record was saved is never updated:
  - stale amounts;
  - status stays Pending through Confirm and Completed;
  - no confirmation date/by, no completion email status/migration date;
  - the Update action is still offered (Support could act on an already-migrated child).
- **No 1 Nov 2026 invoices** on any of the 4 children, so the 1st-of-month billing did not run during the clock
  changes.

- **Stale-record scan (stopped early by the user, 2026-10-14):** 154 of 373 Nov 2026 rows were checked. The only
  mismatch was 49352 (already migrated/Completed — expected), so no other Bug 5 cases were found in that sample.
- **V1→V2 candidates checked (read-only):** 34643 Sriyan Singh (11,099), 39951 Avyukt Verma (6,599), 41667 Eshaan Singh
  (14,399), 43870 Nurvi Wadikar (11,499), 49733 Aditya H Bhat (20,399), 50176 Abirami Arun (10,999).
  - All V1, 2nd year+, yearly Next Billing 01 Nov 2026, Nov 2026 rows Pending, all with dues.
  - Proposed: 34643 Confirm; 39951 Remind → Confirm; 41667 Not Interested; 43870 spare.
- **New tests:**
  - `tc012_v1OfferEmailSent` — "New Payment Plan: Zero Annual Fee" content incl. the AC3 non-refundable line, links,
    Informed row.
  - `tc013_verifyV1ToV2MigrationProcessed` — Approved, V2, monthly +2000, yearly removed, history, parent email
    "Footprints: Payment plan upgraded for <child>".
  - `tc014_firstYearV1ChildNotOffered` — runs the offer API, then expects no row and no email.
  - `tc008` is reused with `-DofferSubject="New Payment Plan: Zero Annual Fee"`.
- **`tc014` PASSED for 67748, 68078, 68891** — API "Promotional email sent successfully to 0 Child", no rows, no email.
  - Caveat: their renewals are Apr/Jul/Aug 2027, so "0 Child" may be due to renewal timing, not the first-year rule.
  - A strong AC1 test needs a first-year V1 child whose yearly renewal is upcoming now.
- **V1→V2 run (server Oct 24 2026):** offers for 34643/39951/41667 → "sent successfully to 1 Child".
  - **`tc012` PASSED ×3** (10/10 content lines incl. AC3 non-refundable; 3 links with the same token; row Informed).
  - The V1 email: "your annual invoice will be raised on 1st November, 2026 for your ward …"; Existing "Annual Fee:
    Rs. 16,000.00 Due on 1st November, 2026" + Monthly X; New "Annual Fee: None" + Monthly X+2,000; "So, instead of
    paying Rs. 16,000.00 in November, you could just pay additional Rs. 2,000.00 per month."; confirm before 30th
    October; the same 3-option line (with the "reminder you later" typo).
  - `tc008` (with `-DofferSubject="New Payment Plan: Zero Annual Fee"`, renewal regex now accepts "will be raised
    on"):
    - 34643 Confirm → **Confirmed** (24 Oct, 1148141008)
    - 39951 Remind Me Later → **Reminder**
    - 41667 Not Interested → **Denied** (24 Oct, 1133190870)
  - **Bug 4 corroborated:** the V1 children get the SAME popup texts ("Your annual plan will not be invoiced…" on
    Confirm; "Your Annual Fee will be invoiced…" on Not Interested). These are correct for V1→V2 but were shown
    unchanged to V2 children, so the text is not direction-aware.
- **39951 V1 reminder path (server Oct 28):**
  - The reminder API → "Reminder Send Successfullynull".
  - **`tc015_v1ReminderEmailSent` PASSED** ("Reminder - New Payment Plan: Zero Annual Fee", 28 Oct 2026 20:00; 10/10
    lines; no "remind" option).
  - `tc008` Confirm from the reminder → **Confirmed** (28 Oct, 1854200941).
  - The V1 reminder link reuses the SAME token as the offer (`q=6ab886…`), unlike the V2 reminder earlier, which had a
    new token.
- **State before the cron:** 34643 Confirmed, 39951 Confirmed (via reminder), 41667 Denied.
- **V1→V2 cron (Oct 31 11:05 PM IST):**
  - 34643 and 39951 → "Processed 1 … approved 1, email_sent No". 41667 → "No migration requests to process" (Denied).
- **`tc013_verifyV1ToV2MigrationProcessed` PASSED** for both:
  - 34643: Approved, V2, monthly 13,099 (= 11,099 + 2,000).
  - 39951: Approved, V2, monthly 8,599 (= 6,599 + 2,000).
  - Both: Yearly Subscription "Not Available" (removed); Invoice Voided 0.
  - Child History "Auto: on 31 October, 2026 11:00 PM, the New Payment Plan (V2) was migrated via parent email
    confirmation…" + "…confirmed by Father … queued for month-end processing".
  - Parent email "Footprints: Payment plan upgraded for <child>" (31 Oct 23:00): "This is to inform that you have been
    migrated to new payment plan as per your confirmation. New Payment Plan Annual Fee: None Monthly Fee: Rs. <new>".
- **`tc011` (now `-DdeniedPlan=V1`) PASSED** for 41667: V1, monthly 14,399, yearly 16,000 kept, row Denied.
- **Test fix:** parent-email lookups now filter Subject WITHOUT the child name ("Footprints: Payment plan
  upgraded/updated for") + Body = child ID. Names can contain double spaces (e.g. "Anaika  Singhal"), which made the
  exact-subject filter miss 39951's email.
- Side note (pre-existing, unrelated to migration): 39951 has no Oct 2026 invoice (last 01 Sep 2026), so its monthly
  Next Billing still shows 01 Oct 2026.
- **V1→V2 parent-email flow covered end-to-end:** `tc012` (offer), `tc008` (Confirm / Remind / Not Interested),
  `tc015` (reminder), `tc013` (processed), `tc011` (declined), `tc014` (first-year, basic).
- **Bug 5 written up (2026-10-14):** `~/Desktop/QA_Documents/Bug Evidence/Bug5_PlanMigration_StaleRecord.md` +
  screenshots 00–08 in `PlanMigration_StaleRecord/` (the folder was renamed from `…_ConfirmNotSaved`). After the cron,
  06 shows 62694 still Pending with Update, 07 shows 62700 Completed for comparison, and 08 shows 62694 Account
  Statement V1 15,799.
- **Bug 5 reported to the dev team by the user (2026-10-14).**
- **Bug 5 cause (user, 2026-10-14):** an EARLIER request/record existed for the same month (created in the past),
  example child **62621**.
  - Read-only check of 62621 (Rudra Pratap): V2 9,749; one PPM row Nov 2026 "9,749 → 7,749 | Pending" (the amount
    matches the current fee); no Customer Requests; no plan-migration history.
  - The earlier record is not visible on screen — likely from the bulk saveMigrationPromotionalData runs ("[Info] New
    Payment Plan Migration Records Saved in DB" 15 Sep / 05 Oct 2026).
  - Proposed clean repro (pending the user's OK + server Oct 24): send the offer to 62621 and Confirm. If the row stays
    Pending, the earlier same-month record alone triggers Bug 5 (no fee change needed).
- **Bug 5 CLOSED — not a real case (developer, 2026-10-14):** caused by an earlier same-month record created while the
  test server date was moved back and forth. Production cannot reach this state. The write-up file is marked CLOSED;
  no 62621 repro needed.
  - **Lesson for future runs:** prefer children with NO existing Payment Plan Migration row for the target month (or
    accept that rows pre-created by earlier date-shifted runs may not update on screen).
- **Bug 5 root cause confirmed (DB screenshot from the user):** `ftprints_footprint.payment_plan_migrations` has 2
  rows for admission 62694, both V2_TO_V1 with `annual_billing_date` 2026-11-01:
  - `migration_id` 9053: existing_monthly_fee 12499 → new 10499 (older — the one shown on screen, stays Pending).
  - 9055: 17799 → 15799 (used by the offer/Confirm/cron).
  - Other columns: `migration_direction` ENUM, `existing_annual_fee`, `new_annual_fee`,
    `new_monthly_fee_onward_april`.
  - The screen shows the older row by default. Created by date-shifted test runs, not a production case. Screenshot:
    `Bug Evidence/PlanMigration_StaleRecord/09_DB_payment_plan_migrations_two_rows_62694.png`.
- **Full test-case document (2026-10-15):** `~/Desktop/QA_Documents/Testcases Scripts/TC_Payment_Plan_Migration.xlsx`,
  from `TC_Template_Enanched_13Oct.xlsx`. The template format was verified unchanged (sheets, data validations, merged
  cells, column widths, conditional formats, freeze panes).
  - 88 TCs / 136 step rows:

    | Scenario | Area | TCs |
    |---|---|---|
    | SC001 | PPM screen | 14 |
    | SC002 | Account Statement V2→V1 | 16 |
    | SC003 | V2→V1 email | 18 |
    | SC004 | V1→V2 email | 13 |
    | SC005 | Eligibility / billing cron | 7 |
    | SC006 | Manual & non-functional | 20 |

  - Iteration 1 is filled from automation runs: 50 PASS, 6 FAIL (Bug 1 ×2, Bug 3, Bug 4 ×2, minor content),
    32 NOT EXECUTED.
  - The Test Summary is filled; QueryLogs has Q1–Q6. The superseded 13 Oct draft `TC_Payment_Plan_Migration_13Oct.xlsx`
    is kept.
  - The user plans to update the automation after the dev fixes Bugs 1/3/4 (and possible approved enhancements).
