package utils;

import io.restassured.RestAssured;
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
 *                                                     NOTE: not actually scoped by chid_id — approves whatever
 *                                                     Extended Daycare request is currently pending system-wide.
 * runExtendedDaycareCronJob()                      — GET parentapp/extendedDaycareCronJob (End Date → Completed)
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
                .replace("&#39;",  "'")
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">");
    }
}
