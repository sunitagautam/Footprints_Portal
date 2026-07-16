package utils;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Index of API methods implemented in this class (grouped by feature):
 * <p>
 * PAYMENT EVENTS
 * postPaymentEvent(json)                          — POST Eventlistener/iciciPaymentEvents (unified UPI/Card)
 * postUpiPaymentEvent(upiJson)                     — POST Eventlistener/iciciPaymentEvents (UPI/NetBanking)
 * postCardPaymentEvent(dataPost)                   — POST create/icici_payment (Credit/Debit Card)
 * getCheckAndProcessData()                         — GET middleware/checkAndProcessData
 * <p>
 * CENTER SHIFT
 * getCenterShiftPendingToProcessing(childId)       — GET Financialprocess/getAllPendingRequests/ (Pending → Processing)
 * getCenterShiftProcessingToApproved(childId)      — GET servicerequest/cronProcessCenterShiftRequests (Processing → Approved)
 * processOldChildAttrition(oldChildId)             — GET parentapp/processChildApprovedRequest (old child → Attrition)
 * <p>
 * EXTENDED DAYCARE
 * getExtendedDaycarePendingToApproved(childId)     — GET Financialprocess/getAllPendingRequests/ (Pending → Approved)
 * NOTE: not actually scoped by chid_id — approves whatever
 * Extended Daycare request is currently pending system-wide.
 * runExtendedDaycareCronJob()                      — GET parentapp/extendedDaycareCronJob (End Date → Completed)
 * <p>
 * TIME EXTENSION
 * getTimeExtensionPendingRequests(childId)         — GET Financialprocess/getAllPendingRequests/ (chid_id scopes correctly here)
 * processTimeExtensionRequest(childId)             — GET childservices/processTimeExtentionRequest (child_id, after Approve)
 * <p>
 * WITHDRAW CHILD
 * getWithdrawChildPendingRequests(childId)         — GET Financialprocess/getAllPendingRequests/ (child_id)
 * processWithdrawChildRequest(childId)             — GET parentapp/processChildApprovedRequest (child_id, after Approve;
 * future/current-dated only — back-dated auto-approves)
 * <p>
 * CORPORATE TRANSFER
 * processCorporateMigrationRequests()              — GET migrationprocess/process_corporate_migration_requests/
 * (month-end cron, no child_id param — processes all approved/processing requests)
 * <p>
 * CORPORATE CENTER TRANSFER
 * getCorporateCenterTransferPendingRequests(childId) — GET Financialprocess/getAllPendingRequests/ (child_id)
 * processCorporateCenterMigrationRequest(childId)  — GET migrationprocess/process_corporate_center_migration_requests (child_id, no ckey per spec)
 * processCorporateCenterTransferApprovedRequest(childId) — GET parentapp/processChildApprovedRequest (child_id, after Approve;
 * Service-Request/Center-Shift path only)
 * <p>
 * HELPERS
 * convertSingleQuotesToDouble(json)                — {'k':'v'} → {"k":"v"}
 * decodeHtmlEntities(raw)                          — &quot;/&amp;/&#39;/&lt;/&gt; → literal chars
 */
public class APIs {

    // ═══════════════════════════════════════════════
    // BASE URLs
    // ═══════════════════════════════════════════════
    public static final String ADMISSIONS_BASE_URL =
            "https://test-admissions.footprintseducation.in/api/";

    public static final String EVENTS_BASE_URL =
            "https://test-events.footprintseducation.in/";

    // ═══════════════════════════════════════════════
    // ENDPOINT PATHS
    // ═══════════════════════════════════════════════
    private static final String UPI_PAYMENT_EVENT =
            "Eventlistener/iciciPaymentEvents";

    private static final String CARD_PAYMENT_EVENT =
            "create/icici_payment";

    private static final String CHECK_AND_PROCESS =
            "middleware/checkAndProcessData";

    // ═══════════════════════════════════════════════
    // CENTER SHIFT CRON ENDPOINTS
    // ═══════════════════════════════════════════════
    private static final String CS_PENDING_TO_PROCESSING =
            "Financialprocess/getAllPendingRequests/";

    private static final String CS_PROCESSING_TO_APPROVED =
            "servicerequest/cronProcessCenterShiftRequests";

    private static final String CS_ATTRITION_PROCESS =
            "parentapp/processChildApprovedRequest";

    // ═══════════════════════════════════════════════
    // EXTENDED DAYCARE ENDPOINTS
    // ═══════════════════════════════════════════════
    // Same physical endpoint as CS_PENDING_TO_PROCESSING — Extended Daycare
    // goes Pending → Approved directly on this call (no Processing step).
    private static final String ED_APPROVE_REQUEST =
            "Financialprocess/getAllPendingRequests/";

    private static final String ED_CRON_JOB =
            "parentapp/extendedDaycareCronJob";

    // ═══════════════════════════════════════════════
    // TIME EXTENSION ENDPOINTS
    // ═══════════════════════════════════════════════
    // Same physical endpoint as CS_PENDING_TO_PROCESSING/ED_APPROVE_REQUEST —
    // confirmed via real captured examples that "chid_id" correctly scopes here.
    private static final String TE_PENDING_REQUESTS =
            "Financialprocess/getAllPendingRequests/";

    private static final String TE_PROCESS_REQUEST =
            "childservices/processTimeExtentionRequest";

    // ═══════════════════════════════════════════════
    // WITHDRAW CHILD ENDPOINTS
    // ═══════════════════════════════════════════════
    // Same physical endpoint as CS_PENDING_TO_PROCESSING/ED_APPROVE_REQUEST/
    // TE_PENDING_REQUESTS — the test spec's own example URL confirms "child_id"
    // (not "chid_id") is the correct scoping param here.
    private static final String WD_PENDING_REQUESTS =
            "Financialprocess/getAllPendingRequests/";

    // ═══════════════════════════════════════════════
    // CORPORATE TRANSFER ENDPOINTS
    // ═══════════════════════════════════════════════
    // Month-end cron — spec's own example URL has no child_id param at all;
    // processes all approved/processing Corporate Transfer requests globally.
    private static final String CT_MIGRATION_CRON =
            "migrationprocess/process_corporate_migration_requests/";

    // ═══════════════════════════════════════════════
    // CORPORATE CENTER TRANSFER ENDPOINTS
    // ═══════════════════════════════════════════════
    // Same physical endpoint as CS_PENDING_TO_PROCESSING/ED_APPROVE_REQUEST/
    // TE_PENDING_REQUESTS/WD_PENDING_REQUESTS — uses "child_id" per this
    // feature's own spec examples.
    private static final String CCT_PENDING_REQUESTS =
            "Financialprocess/getAllPendingRequests/";

    // Spec's own example URL has no ckey param for this one.
    private static final String CCT_MIGRATION_REQUEST =
            "migrationprocess/process_corporate_center_migration_requests";

    // ═══════════════════════════════════════════════
    // API 1 — POST Payment Event (UPI or Card JSON)
    //
    // URL  : {{Base_URL}}Eventlistener/iciciPaymentEvents
    // Body : hidden JSON — id="payment_json_icici_upi"
    //        or id="payment_json_icici_ccdc"
    // Use  : Single unified endpoint for both payment modes.
    //        Converts single→double quotes automatically.
    // ═══════════════════════════════════════════════
    public static Response postPaymentEvent(String json) {
        String validJson = convertSingleQuotesToDouble(json);
        System.out.println("▶ POST Payment Event");
        System.out.println("   URL  : " + ADMISSIONS_BASE_URL + UPI_PAYMENT_EVENT);
        System.out.println("   Body : " + validJson);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .contentType("application/json")
                .body(validJson)
                .when()
                .post(UPI_PAYMENT_EVENT)
                .then()
                .extract()
                .response();

        System.out.println("✅ Payment Event — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // API 1a — POST UPI / NetBanking Payment Event
    //
    // URL  : {{Base_URL}}Eventlistener/iciciPaymentEvents
    // Body : UPI hidden JSON (single quotes → double quotes)
    // Use  : After extracting id="payment_json_icici_upi" value
    // ═══════════════════════════════════════════════
    public static Response postUpiPaymentEvent(String upiJson) {
        String validJson = convertSingleQuotesToDouble(upiJson);
        System.out.println("▶ POST UPI Payment Event");
        System.out.println("   URL  : " + ADMISSIONS_BASE_URL + UPI_PAYMENT_EVENT);
        System.out.println("   Body : " + validJson);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .contentType("application/json")
                .body(validJson)
                .when()
                .post(UPI_PAYMENT_EVENT)
                .then()
                .extract()
                .response();

        System.out.println("✅ UPI Event — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // API 2 — POST Credit / Debit Card Payment Event
    //
    // URL  : https://test-events.footprintseducation.in/create/icici_payment
    // Body : data-post attribute value (&quot; → ")
    // Use  : After card payment completes and data-post is extracted
    // ═══════════════════════════════════════════════
    public static Response postCardPaymentEvent(String dataPost) {
        String validJson = decodeHtmlEntities(dataPost);
        System.out.println("▶ POST Card Payment Event");
        System.out.println("   URL  : " + EVENTS_BASE_URL + CARD_PAYMENT_EVENT);
        System.out.println("   Body : " + validJson);

        Response response = given()
                .baseUri(EVENTS_BASE_URL)
                .contentType("application/json")
                .body(validJson)
                .when()
                .post(CARD_PAYMENT_EVENT)
                .then()
                .extract()
                .response();

        System.out.println("✅ Card Event — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // API 3 — GET Check and Process Data
    //
    // URL  : https://test-admissions.footprintseducation.in/api/middleware/checkAndProcessData
    // Use  : After posting the card payment event
    // ═══════════════════════════════════════════════
    public static Response getCheckAndProcessData() {
        System.out.println("▶ GET Check and Process Data");
        System.out.println("   URL  : " + ADMISSIONS_BASE_URL + CHECK_AND_PROCESS);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(CHECK_AND_PROCESS)
                .then()
                .extract()
                .response();

        System.out.println("✅ Check & Process — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CENTER SHIFT — Pending → Processing
    //
    // URL : {{Base_URL}}Financialprocess/getAllPendingRequests/
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373
    // Use : Move center shift from Pending to Processing status
    // Note: "chid_id" (not child_id) — matches API spec exactly
    // ═══════════════════════════════════════════════
    public static Response getCenterShiftPendingToProcessing(String childId) {
        String endpoint = CS_PENDING_TO_PROCESSING
                + "?key=F@@tpr!nt$ChargeBeeUpdate$"
                + "&chid_id=" + childId
                + "&ckey=B47C56483AAE7373";
        System.out.println("▶ Center Shift: Pending → Processing");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Pending→Processing — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CENTER SHIFT — Processing → Approved
    //
    // URL : {{Base_URL}}servicerequest/cronProcessCenterShiftRequests
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<child_id>&ckey=B43C083098B7
    // Use : Approve center shift, creates new child + attrition row
    //       Response JSON: { status:"ok", old_child_id:..., new_child_id:... }
    // Note: Cron only processes requests where Joining Date is within ±5 days of today
    // ═══════════════════════════════════════════════
    public static Response getCenterShiftProcessingToApproved(String childId) {
        String endpoint = CS_PROCESSING_TO_APPROVED
                + "?key=F@@tpr!nt$ChargeBeeUpdate$"
                + "&child_id=" + childId
                + "&ckey=B43C083098B7";
        System.out.println("▶ Center Shift: Processing → Approved");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Processing→Approved — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CENTER SHIFT — Old Child Attrition Processing
    //
    // URL : {{Base_URL}}parentapp/processChildApprovedRequest
    //       ?child_id=<old_child_id>&ckey=9414D96600C5
    // Use : Change old child status from Active to Attrition
    //       Use old_child_id returned from getCenterShiftProcessingToApproved()
    // ═══════════════════════════════════════════════
    public static Response processOldChildAttrition(String oldChildId) {
        String endpoint = CS_ATTRITION_PROCESS
                + "?child_id=" + oldChildId
                + "&ckey=9414D96600C5";
        System.out.println("▶ Center Shift: Old Child Attrition Processing");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Attrition Processing — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // EXTENDED DAYCARE — Pending → Approved
    //
    // URL : {{Base_URL}}Financialprocess/getAllPendingRequests/
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<child_id>&ckey=B47C56483AAE7373
    // Use : Same physical endpoint as Center Shift's Pending→Processing call.
    //       For Extended Daycare it moves the request directly to Approved
    //       (no Processing status in between).
    // Note: uses "child_id" (unlike Center Shift's "chid_id" on the same path) —
    //       confirmed the correct param; "chid_id" was silently ignored, causing
    //       the endpoint to process whatever was pending system-wide instead of
    //       the intended child.
    // ═══════════════════════════════════════════════
    public static Response getExtendedDaycarePendingToApproved(String childId) {
        String endpoint = ED_APPROVE_REQUEST
                + "?key=F@@tpr!nt$ChargeBeeUpdate$"
                + "&child_id=" + childId
                + "&ckey=B47C56483AAE7373";
        System.out.println("▶ Extended Daycare: Pending → Approved");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Pending→Approved — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // EXTENDED DAYCARE — Cron Job (End Date → Completed)
    //
    // URL : {{Base_URL}}parentapp/extendedDaycareCronJob?ckey=7A533862C14E
    // Use : Run on/after End Date to mark the request Completed.
    //       Response JSON: { status:"ok", message:"...Completed...", ... }
    //       Running before End Date must NOT complete the request.
    // ═══════════════════════════════════════════════
    public static Response runExtendedDaycareCronJob() {
        String endpoint = ED_CRON_JOB + "?ckey=7A533862C14E";
        System.out.println("▶ Extended Daycare: Cron Job");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Cron Job — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // TIME EXTENSION — getAllPendingRequests
    //
    // URL : {{Base_URL}}Financialprocess/getAllPendingRequests/
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&chid_id=<child_id>&ckey=B47C56483AAE7373
    // Use : Same physical endpoint as Center Shift/Extended Daycare. Confirmed via
    //       real captured examples (doc: "Start Time Extension Service Request")
    //       that "chid_id" IS the correct param for this endpoint in the Time
    //       Extension context (unlike Extended Daycare, where "child_id" was
    //       needed instead) — do not assume the two behave identically.
    // ═══════════════════════════════════════════════
    public static Response getTimeExtensionPendingRequests(String childId) {
        String endpoint = TE_PENDING_REQUESTS
                + "?key=F@@tpr!nt$ChargeBeeUpdate$"
                + "&chid_id=" + childId
                + "&ckey=B47C56483AAE7373";
        System.out.println("▶ Time Extension: getAllPendingRequests");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ getAllPendingRequests — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // TIME EXTENSION — Process Request (Approve → Processed)
    //
    // URL : http://test-admissions.footprintseducation.in/api/childservices/
    //       processTimeExtentionRequest?child_id=<child_id>&ckey=3E529969372D
    // Use : Run after clicking Approve on the Customer Request screen.
    //       Response JSON: {"status":"ok","message":"Time Extension request processed"}
    //       Confirmed "child_id" (not "chid_id") from the doc's real examples.
    // ═══════════════════════════════════════════════
    public static Response processTimeExtensionRequest(String childId) {
        String endpoint = TE_PROCESS_REQUEST
                + "?child_id=" + childId
                + "&ckey=3E529969372D";
        System.out.println("▶ Time Extension: Process Request");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Process Request — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // WITHDRAW CHILD — getAllPendingRequests
    //
    // URL : {{Base_URL}}Financialprocess/getAllPendingRequests/
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<child_id>&ckey=B47C56483AAE7373
    // Use : Same physical endpoint as Center Shift/Extended Daycare/Time
    //       Extension. Spec's own example omits a ckey — reusing the ckey
    //       already confirmed working on this endpoint (B47C56483AAE7373);
    //       verify against a live response before trusting it further.
    // ═══════════════════════════════════════════════
    public static Response getWithdrawChildPendingRequests(String childId) {
        String endpoint = WD_PENDING_REQUESTS
                + "?key=F@@tpr!nt$ChargeBeeUpdate$"
                + "&child_id=" + childId
                + "&ckey=B47C56483AAE7373";
        System.out.println("▶ Withdraw Child: getAllPendingRequests");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ getAllPendingRequests — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // WITHDRAW CHILD — Process Approved Request
    //
    // URL : {{Base_URL}}parentapp/processChildApprovedRequest?child_id=<child_id>&ckey=9414D96600C5
    // Use : Run after clicking Approve on the Customer Request screen (future/
    //       current-dated withdrawals only — back-dated ones auto-approve).
    //       Spec's own example omits a ckey — reusing the only ckey already
    //       confirmed working against this endpoint (processOldChildAttrition's
    //       9414D96600C5, from Center Shift's attrition step); verify against a
    //       live response before trusting it further.
    // ═══════════════════════════════════════════════
    public static Response processWithdrawChildRequest(String childId) {
        String endpoint = CS_ATTRITION_PROCESS
                + "?child_id=" + childId
                + "&ckey=9414D96600C5";
        System.out.println("▶ Withdraw Child: Process Approved Request");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Process Approved Request — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CORPORATE TRANSFER — Month-end migration cron
    //
    // URL : {{Base_URL}}migrationprocess/process_corporate_migration_requests/
    // Use : Run at month end to process all approved Corporate Transfer
    //       requests — marks old admission Attrition (reason=Transfer) and
    //       creates new admission at the new center/TieUp/program.
    //       No child_id param per spec's own example — global cron.
    // ═══════════════════════════════════════════════
    public static Response processCorporateMigrationRequests() {
        System.out.println("▶ Corporate Transfer: Month-end Migration Cron");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + CT_MIGRATION_CRON);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(CT_MIGRATION_CRON)
                .then()
                .extract()
                .response();

        System.out.println("✅ Migration Cron — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    /**
     * Scoped/simulated variant — accepts child_id + date so a specific
     * request's month-end can be forced/verified without waiting for the
     * real calendar month to arrive (confirmed working param pair).
     *
     * URL : {{Base_URL}}migrationprocess/process_corporate_migration_requests?child_id=<child_id>&date=<yyyy-MM-dd>
     */
    public static Response processCorporateMigrationRequests(String childId, String date) {
        String endpoint = CT_MIGRATION_CRON + "?child_id=" + childId + "&date=" + date;
        System.out.println("▶ Corporate Transfer: Month-end Migration Cron (scoped)");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Migration Cron (scoped) — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CORPORATE CENTER TRANSFER — getAllPendingRequests
    //
    // URL : {{Base_URL}}Financialprocess/getAllPendingRequests/
    //       ?key=F@@tpr!nt$ChargeBeeUpdate$&child_id=<child_id>&ckey=B47C56483AAE7373
    // Use : Same physical endpoint as Center Shift/Extended Daycare/Time
    //       Extension/Withdraw Child. Uses "child_id" per this feature's
    //       own spec examples.
    // ═══════════════════════════════════════════════
    public static Response getCorporateCenterTransferPendingRequests(String childId) {
        String endpoint = CCT_PENDING_REQUESTS
                + "?key=F@@tpr!nt$ChargeBeeUpdate$"
                + "&child_id=" + childId
                + "&ckey=B47C56483AAE7373";
        System.out.println("▶ Corporate Center Transfer: getAllPendingRequests");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ getAllPendingRequests — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CORPORATE CENTER TRANSFER — Process Migration Request (button-flow approval)
    //
    // URL : {{Base_URL}}migrationprocess/process_corporate_center_migration_requests?child_id=<child_id>&ckey=10998DF5FF67
    // Use : Run after Approve on Recent Customer Requests, for requests
    //       submitted via the "CORPORATE CENTER TRANSFER" button.
    //       User-confirmed ckey=10998DF5FF67 (spec's own example omitted it).
    //       Confirmed live: without a "date" param matching the request's own
    //       WEF date, this returns "No Request to Process..." — user-confirmed
    //       "date is as per request WEF date". Also confirmed live: passing
    //       child_id does NOT actually scope the call — it processes ALL
    //       matching requests for that date system-wide (same pattern as
    //       Extended Daycare's cron), so treat childId here as documentation
    //       of intent, not a real scope guarantee.
    // ═══════════════════════════════════════════════
    public static Response processCorporateCenterMigrationRequest(String childId, String wefDate) {
        String endpoint = CCT_MIGRATION_REQUEST
                + "?child_id=" + childId
                + "&date=" + wefDate
                + "&ckey=10998DF5FF67";
        System.out.println("▶ Corporate Center Transfer: Process Migration Request");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Process Migration Request — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // CORPORATE CENTER TRANSFER — Process Approved Request (Service-Request/Center-Shift flow)
    //
    // URL : {{Base_URL}}parentapp/processChildApprovedRequest?child_id=<child_id>&ckey=9414D96600C5
    // Use : Run after Approve, for requests submitted via SERVICE REQUEST →
    //       Center Shift (Transfer Applicable=Yes children). Same physical
    //       endpoint/ckey as Center Shift's processOldChildAttrition and
    //       Withdraw Child's processWithdrawChildRequest.
    // ═══════════════════════════════════════════════
    public static Response processCorporateCenterTransferApprovedRequest(String childId) {
        String endpoint = CS_ATTRITION_PROCESS
                + "?child_id=" + childId
                + "&ckey=9414D96600C5";
        System.out.println("▶ Corporate Center Transfer: Process Approved Request");
        System.out.println("   URL: " + ADMISSIONS_BASE_URL + endpoint);

        Response response = given()
                .baseUri(ADMISSIONS_BASE_URL)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();

        System.out.println("✅ Process Approved Request — Status: " + response.getStatusCode());
        System.out.println("   Response: " + response.getBody().asString());
        return response;
    }

    // ═══════════════════════════════════════════════
    // HELPER — Convert single quotes to double quotes
    // UPI JSON comes as {'key':'value'} from the app.
    // Must be {"key":"value"} for a valid JSON POST body.
    // ═══════════════════════════════════════════════
    public static String convertSingleQuotesToDouble(String json) {
        if (json == null || json.isEmpty()) return json;
        return json.replace("'", "\"");
    }

    // ═══════════════════════════════════════════════
    // HELPER — Decode HTML entities in data-post value
    // data-post contains: &quot; &amp; &#39; etc.
    // Must be decoded to valid JSON before posting.
    // ═══════════════════════════════════════════════
    public static String decodeHtmlEntities(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        return raw
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
