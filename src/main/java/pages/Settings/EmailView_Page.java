package pages.Settings;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.util.List;

/**
 * Page Object: EmailView_Page
 * <p>
 * Navigation: Settings → Email View, or directly via
 * {@code Navigations.goToEmailView()} (URL: /email_view).
 * <p>
 * CONFIRMED LIVE (2026-09-22): only the "Rakesh" user has access to this
 * screen — a test using this page object must stay on (or switch back to)
 * Rakesh, not a switched-to user like Jaydeep Kar.
 * <p>
 * Generic, reusable across any feature that needs to verify a confirmation/
 * notification email was sent — mirrors {@link SMSView_Page}'s structure.
 */
public class EmailView_Page {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // EMAIL TABLE — confirmed live (2026-09-22): standard DataTables grid,
    // id="DataTables_Table_0". Columns: Email Id | Email Subject |
    // Email Header | Email Failed Reason | Created Date | From Email |
    // Email Status | Action. Row order is NOT reliably "most recent
    // first" — the default view's first row was a future-dated (01 Oct
    // 2026) scheduled entry ahead of same-day 22 Sep entries. Use
    // searchTable(keyword) + row scan instead of assuming row position.
    // ═══════════════════════════════════════════════
    @FindBy(xpath = "//table//tbody//tr[1]")
    private WebElement firstRow;

    @FindBy(xpath = "//table//tbody//tr")
    private List<WebElement> allRows;

    // Global DataTables search box — same locator convention already used
    // in RecentCustomerRequestsPage.searchInTable().
    @FindBy(xpath = "//div[contains(@class,'dataTables_filter')]//input[@type='search']")
    private WebElement tableSearchInput;

    // ═══════════════════════════════════════════════
    // ADVANCED FILTER FORM — confirmed live (2026-09-22):
    // id="frm-search" — Email | Subject | Body | From Email | Email Status
    // | Submit. Per user instruction: always filter by the dedicated
    // Subject field (id="subject"), not the generic DataTables search box,
    // when looking for a feature's emails.
    // ═══════════════════════════════════════════════
    @FindBy(id = "subject")
    private WebElement subjectFilterInput;

    @FindBy(id = "btn_submit")
    private WebElement filterSubmitBtn;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public EmailView_Page(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    // ═══════════════════════════════════════════════
    // IS PAGE LOADED
    // ═══════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//table//tbody//tr[1]")));
            System.out.println("✅ Email View page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Email View page not loaded: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // DIAGNOSTIC — dump the table's header + first few rows so real
    // locators/column layout can be confirmed before writing real
    // interaction/assertion methods.
    // ═══════════════════════════════════════════════
    public String dumpFilterFormHtml() {
        try {
            String html = (String) ((JavascriptExecutor) driver).executeScript(
                    "var forms = document.querySelectorAll('form');" +
                            "for (var i=0;i<forms.length;i++){" +
                            "  if (forms[i].outerHTML.toLowerCase().indexOf('subject') !== -1) {" +
                            "    return forms[i].outerHTML.substring(0, 5000);" +
                            "  }" +
                            "}" +
                            "return 'NO FORM CONTAINING \"subject\" FOUND (total forms: ' + forms.length + ')';");
            System.out.println("▶ EMAIL VIEW FILTER FORM DUMP:\n" + html);
            return html;
        } catch (Exception e) {
            System.out.println("⚠ dumpFilterFormHtml: " + e.getMessage());
            return "";
        }
    }

    public String dumpVisibleTableStructure() {
        try {
            String html = (String) ((JavascriptExecutor) driver).executeScript(
                    "var t = document.querySelector('table');" +
                            "return t ? t.outerHTML.substring(0, 6000) : 'NO TABLE FOUND ON PAGE';");
            System.out.println("▶ EMAIL VIEW TABLE DUMP:\n" + html);
            return html;
        } catch (Exception e) {
            System.out.println("⚠ dumpVisibleTableStructure: " + e.getMessage());
            return "";
        }
    }

    public String dumpBodyText() {
        try {
            String text = driver.findElement(By.tagName("body")).getText();
            String snippet = text.length() > 3000 ? text.substring(0, 3000) : text;
            System.out.println("▶ EMAIL VIEW BODY TEXT (first 3000 chars):\n" + snippet);
            return text;
        } catch (Exception e) {
            System.out.println("⚠ dumpBodyText: " + e.getMessage());
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // SEARCH — global DataTables search box (kept for ad-hoc/diagnostic
    // use only). For feature-scoped lookups, prefer filterBySubject()
    // below, per explicit user instruction.
    // ═══════════════════════════════════════════════
    public void searchTable(String keyword) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(tableSearchInput));
        tableSearchInput.clear();
        tableSearchInput.sendKeys(keyword);
        Thread.sleep(1000);
        System.out.println("▶ Email View table search: " + keyword);
    }

    /**
     * Filters the grid using the dedicated Subject field (id="subject")
     * of the advanced filter form (id="frm-search") and submits it —
     * confirmed live selector, 2026-09-22. Always use this (not the
     * generic search box) to look up a feature's emails, e.g.
     * filterBySubject("Extended Daycare").
     */
    public void filterBySubject(String subjectKeyword) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(subjectFilterInput));
        subjectFilterInput.clear();
        subjectFilterInput.sendKeys(subjectKeyword);
        filterSubmitBtn.click();
        Thread.sleep(1500);
        System.out.println("▶ Email View Subject filter: " + subjectKeyword);
    }

    /**
     * Returns true if, after filtering by Subject, at least one row
     * contains ALL of the given required substrings (case-insensitive) —
     * e.g. a child id alongside the subject text already filtered on.
     */
    public boolean hasEmailContaining(String subjectKeyword, String... requiredSubstrings) throws InterruptedException {
        filterBySubject(subjectKeyword);
        PageFactory.initElements(driver, this);
        for (WebElement row : allRows) {
            String rowText = row.getText();
            if (rowText == null || rowText.trim().isEmpty()) continue;
            String lower = rowText.toLowerCase();
            boolean allMatch = true;
            for (String req : requiredSubstrings) {
                if (!lower.contains(req.toLowerCase())) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                System.out.println("✅ Matching email row: " + rowText.replaceAll("\\s+", " ").trim());
                return true;
            }
        }
        System.out.println("⚠ No email row matched Subject='" + subjectKeyword + "' with all of: "
                + String.join(", ", requiredSubstrings));
        return false;
    }

    /**
     * Returns the text of the first row after filtering by Subject, or
     * empty string if no rows match.
     */
    public String getFirstMatchingRowText(String subjectKeyword) throws InterruptedException {
        filterBySubject(subjectKeyword);
        PageFactory.initElements(driver, this);
        for (WebElement row : allRows) {
            String rowText = row.getText();
            if (rowText != null && !rowText.trim().isEmpty()
                    && !rowText.toLowerCase().contains("no matching records")) {
                return rowText.trim();
            }
        }
        return "";
    }

    /**
     * Returns ALL rows' text after filtering by Subject — useful when
     * scanning multiple matches (e.g. to check for a distinct variant
     * email among several results with the same subject family).
     */
    public List<String> getAllMatchingRowTexts(String subjectKeyword) throws InterruptedException {
        filterBySubject(subjectKeyword);
        PageFactory.initElements(driver, this);
        List<String> result = new java.util.ArrayList<>();
        for (WebElement row : allRows) {
            String rowText = row.getText();
            if (rowText != null && !rowText.trim().isEmpty()
                    && !rowText.toLowerCase().contains("no matching records")) {
                result.add(rowText.trim());
            }
        }
        return result;
    }

    public String getShowingEntriesText() {
        try {
            return driver.findElement(By.xpath("//div[contains(@class,'dataTables_info')]")).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
