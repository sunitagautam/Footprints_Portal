package utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

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
