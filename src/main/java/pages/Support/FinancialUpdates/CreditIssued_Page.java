package pages.Support.FinancialUpdates;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.IAutoConstant;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// ═══════════════════════════════════════════════════════════════
// CREDITS ISSUED (Support → Financial Updates → Credits Issued)
// Screen: /issue_credits — all selectors confirmed live
// (2026-10-05/06, see CLAUDE.md "Requirements — Credits Issued").
// The Action column (Revoke Credit / Link Invoice Reference) only
// renders for Jaydeep Kar, not Rakesh.
// ═══════════════════════════════════════════════════════════════
public class CreditIssued_Page {

    WebDriver driver;
    WebDriverWait wait;

    // ═══════════════════════════════════════════════
    // MAIN PAGE — FILTERS
    // ═══════════════════════════════════════════════
    // ✅ popdown_big link (not a button)
    @FindBy(xpath = "//a[contains(@href,'pop_issue_credits')]")
    private WebElement addNewBtn;

    // ✅ From / To share the duplicate id "select_date" —
    //    located by name instead. pickadate.js, readonly.
    @FindBy(name = "date_from")
    private WebElement fromDateInput;

    @FindBy(name = "date_to")
    private WebElement toDateInput;

    // ✅ select2-backed <select>
    @FindBy(id = "center_selection")
    private WebElement centerDropdown;

    // ✅ All | Credits | Void Credits
    @FindBy(id = "credit_type")
    private WebElement typeDropdown;

    // ✅ All | Pending | Successful | Failed
    @FindBy(id = "status")
    private WebElement statusDropdown;

    @FindBy(name = "submit_credit_date")
    private WebElement filterSubmitBtn;

    // ═══════════════════════════════════════════════
    // MAIN PAGE — TABLE
    // ═══════════════════════════════════════════════
    @FindBy(id = "DataTables_Table_0")
    private WebElement table;

    @FindBy(css = "#DataTables_Table_0_filter input[type='search']")
    private WebElement tableSearchInput;

    @FindBy(id = "DataTables_Table_0_info")
    private WebElement tableInfo;

    // ✅ DataTables client-side CSV export
    @FindBy(css = "a.buttons-csv")
    private WebElement downloadReportBtn;

    @FindBy(id = "DataTables_Table_0_paginate")
    private WebElement pagination;

    @FindBy(css = "#DataTables_Table_0_paginate a.next")
    private WebElement nextPageBtn;

    @FindBy(css = "#DataTables_Table_0_paginate a.previous")
    private WebElement previousPageBtn;

    // ═══════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════
    public CreditIssued_Page(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver,
                Duration.ofSeconds(IAutoConstant.EXPLICIT_WAIT));
        PageFactory.initElements(driver, this);
    }

    private JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    // ✅ Add/Link/Revoke modals reuse ids (child_id,
    //    credit_invoice_ref) — always act on the visible one
    private WebElement visible(By by) {
        for (WebElement el : driver.findElements(by)) {
            try {
                if (el.isDisplayed()) return el;
            } catch (Exception ignored) {
            }
        }
        throw new org.openqa.selenium.NoSuchElementException(
                "No visible element for " + by);
    }

    private WebElement waitVisible(By by) {
        return wait.until(d -> {
            try {
                return visible(by);
            } catch (Exception e) {
                return null;
            }
        });
    }

    // ═══════════════════════════════════════════════
    // IS PAGE LOADED
    // ═══════════════════════════════════════════════
    public boolean isPageLoaded() {
        try {
            PageFactory.initElements(driver, this);
            wait.until(ExpectedConditions.elementToBeClickable(addNewBtn));
            wait.until(ExpectedConditions.visibilityOf(tableInfo));
            System.out.println("✅ Credits Issued page loaded");
            return true;
        } catch (Exception e) {
            System.out.println("❌ Credits Issued page not loaded: "
                    + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // DEFAULT VIEW — element visibility
    // ═══════════════════════════════════════════════
    public boolean isAddNewButtonVisible()   { return isShown(addNewBtn); }
    public boolean isFromDateVisible()       { return isShown(fromDateInput); }
    public boolean isToDateVisible()         { return isShown(toDateInput); }
    public boolean isTypeFilterVisible()     { return isShown(typeDropdown); }
    public boolean isStatusFilterVisible()   { return isShown(statusDropdown); }
    public boolean isFilterSubmitVisible()   { return isShown(filterSubmitBtn); }
    public boolean isSearchBarVisible()      { return isShown(tableSearchInput); }
    public boolean isDownloadReportVisible() { return isShown(downloadReportBtn); }
    public boolean isPaginationVisible()     { return isShown(pagination); }

    // ✅ The <select> itself is hidden by select2 —
    //    check the select2 container next to it
    public boolean isCenterFilterVisible() {
        try {
            return (Boolean) js().executeScript(
                    "var s=document.getElementById('center_selection');" +
                            "var c=s.nextElementSibling;" +
                            "var el=(c && c.className.indexOf('select2')>=0)?c:s;" +
                            "return !!(el.offsetWidth||el.offsetHeight);");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isShown(WebElement el) {
        try {
            return el.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getTypeFilterOptions() {
        return optionTexts(typeDropdown);
    }

    public List<String> getStatusFilterOptions() {
        return optionTexts(statusDropdown);
    }

    public String getFromDateValue() {
        return fromDateInput.getAttribute("value");
    }

    public String getToDateValue() {
        return toDateInput.getAttribute("value");
    }

    private List<String> optionTexts(WebElement select) {
        List<String> out = new ArrayList<>();
        for (WebElement o : new Select(select).getOptions())
            out.add(o.getText().trim());
        return out;
    }

    // ✅ Only the visible headers (hidden CSV-only
    //    columns are display:none)
    public List<String> getVisibleColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : table.findElements(By.cssSelector("thead th"))) {
            if (th.isDisplayed()) out.add(th.getText().trim());
        }
        return out;
    }

    // ✅ All user-facing columns, whether or not the
    //    responsive layout has folded them into the "+"
    //    row expander — excludes only the CSV-only
    //    columns (class "never")
    public List<String> getDefinedColumnHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : table.findElements(By.cssSelector("thead th"))) {
            if (!th.getAttribute("class").contains("never"))
                out.add(th.getAttribute("textContent").trim());
        }
        return out;
    }

    // ═══════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════
    // ✅ Uses pickadate's own API (picker.set('select'))
    //    so the widget's internal state is updated —
    //    NOT raw value injection (see Withdraw Child's
    //    attrition_date bug in CLAUDE.md).
    public void setDateRange(LocalDate from, LocalDate to)
            throws InterruptedException {
        setPickadate("date_from", from);
        setPickadate("date_to", to);
        System.out.println("▶ Date range set: " + getFromDateValue()
                + " → " + getToDateValue());
    }

    private void setPickadate(String name, LocalDate date)
            throws InterruptedException {
        js().executeScript(
                "var p=$('input[name=\"'+arguments[0]+'\"]').pickadate('picker');" +
                        "p.set('select',[arguments[1],arguments[2],arguments[3]]);" +
                        "p.close();",
                name, date.getYear(), date.getMonthValue() - 1,
                date.getDayOfMonth());
        Thread.sleep(500);
    }

    // ✅ select2 — set the underlying <select> by
    //    visible text and fire change
    public void selectCenter(String centerName) throws InterruptedException {
        Boolean found = (Boolean) js().executeScript(
                "var s=document.getElementById('center_selection');" +
                        "for(var i=0;i<s.options.length;i++){" +
                        " if(s.options[i].text.trim()===arguments[0]){" +
                        "  $(s).val(s.options[i].value).trigger('change'); return true;}}" +
                        "return false;", centerName);
        if (!Boolean.TRUE.equals(found))
            throw new IllegalArgumentException(
                    "Center not found in dropdown: " + centerName);
        System.out.println("▶ Center selected: " + centerName);
        Thread.sleep(500);
    }

    public String getSelectedCenter() {
        return new Select(centerDropdown).getFirstSelectedOption()
                .getText().trim();
    }

    public void selectType(String type) {
        new Select(typeDropdown).selectByVisibleText(type);
        System.out.println("▶ Type selected: " + type);
    }

    public String getSelectedType() {
        return new Select(typeDropdown).getFirstSelectedOption()
                .getText().trim();
    }

    public void selectStatus(String status) {
        new Select(statusDropdown).selectByVisibleText(status);
        System.out.println("▶ Status selected: " + status);
    }

    public String getSelectedStatus() {
        return new Select(statusDropdown).getFirstSelectedOption()
                .getText().trim();
    }

    // ✅ Submit is a real form post — waits for the
    //    old table to go stale, then for the new one
    public void clickFilterSubmit() throws InterruptedException {
        WebElement oldTable = table;
        filterSubmitBtn.click();
        System.out.println("▶ Filter Submit clicked");
        try {
            wait.until(ExpectedConditions.stalenessOf(oldTable));
        } catch (Exception ignored) {
        }
        PageFactory.initElements(driver, this);
        wait.until(ExpectedConditions.visibilityOf(tableInfo));
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // TABLE READERS
    // ═══════════════════════════════════════════════
    public String getInfoText() {
        return tableInfo.getText().trim();
    }

    public boolean isTableEmpty() {
        return !table.findElements(
                By.cssSelector("tbody td.dataTables_empty")).isEmpty();
    }

    public String getEmptyTableMessage() {
        List<WebElement> empty = table.findElements(
                By.cssSelector("tbody td.dataTables_empty"));
        return empty.isEmpty() ? "" : empty.get(0).getText().trim();
    }

    public int getVisibleRowCount() {
        if (isTableEmpty()) return 0;
        return table.findElements(By.cssSelector("tbody tr")).size();
    }

    // ✅ Header index is taken across ALL th (hidden ones
    //    included) so it lines up with the td index
    private int headerIndex(String header) {
        List<WebElement> ths = table.findElements(By.cssSelector("thead th"));
        for (int i = 0; i < ths.size(); i++) {
            if (ths.get(i).getAttribute("textContent").trim()
                    .equalsIgnoreCase(header)) return i;
        }
        throw new IllegalArgumentException("Column not found: " + header);
    }

    // ✅ Values of one column on the CURRENT page
    public List<String> getColumnValues(String header) {
        List<String> out = new ArrayList<>();
        if (isTableEmpty()) return out;
        int idx = headerIndex(header);
        for (WebElement row : table.findElements(By.cssSelector("tbody tr"))) {
            List<WebElement> tds = row.findElements(By.tagName("td"));
            if (tds.size() > idx)
                out.add(tds.get(idx).getAttribute("textContent").trim());
        }
        return out;
    }

    public void searchTable(String keyword) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(tableSearchInput));
        tableSearchInput.clear();
        tableSearchInput.sendKeys(keyword);
        Thread.sleep(1500);
        System.out.println("▶ Table search: " + keyword);
    }

    public void clearSearch() throws InterruptedException {
        tableSearchInput.clear();
        tableSearchInput.sendKeys(" ");
        tableSearchInput.clear();
        js().executeScript("$('#DataTables_Table_0').DataTable().search('').draw();");
        Thread.sleep(1000);
    }

    // ═══════════════════════════════════════════════
    // DOWNLOAD / PAGINATION / SORTING
    // ═══════════════════════════════════════════════
    public void clickDownloadReport() throws InterruptedException {
        wait.until(ExpectedConditions.elementToBeClickable(downloadReportBtn));
        downloadReportBtn.click();
        System.out.println("▶ Download Report clicked");
        Thread.sleep(3000);
    }

    public boolean isNextPageEnabled() {
        return !nextPageBtn.getAttribute("class").contains("disabled");
    }

    public boolean isPreviousPageEnabled() {
        return !previousPageBtn.getAttribute("class").contains("disabled");
    }

    public void clickNextPage() throws InterruptedException {
        nextPageBtn.click();
        System.out.println("▶ Next page clicked");
        Thread.sleep(1000);
    }

    public void clickPreviousPage() throws InterruptedException {
        previousPageBtn.click();
        System.out.println("▶ Previous page clicked");
        Thread.sleep(1000);
    }

    public void clickColumnHeader(String header) throws InterruptedException {
        List<WebElement> ths = table.findElements(By.cssSelector("thead th"));
        ths.get(headerIndex(header)).click();
        System.out.println("▶ Sorted by: " + header);
        Thread.sleep(1000);
    }

    // ✅ "sorting_asc" / "sorting_desc" / "sorting"
    public String getHeaderSortClass(String header) {
        List<WebElement> ths = table.findElements(By.cssSelector("thead th"));
        return ths.get(headerIndex(header)).getAttribute("class");
    }

    // ═══════════════════════════════════════════════
    // ADD NEW ISSUE CREDITS/REFUND MODAL
    // ═══════════════════════════════════════════════
    public void clickAddNew() {
        wait.until(ExpectedConditions.elementToBeClickable(addNewBtn));
        js().executeScript("arguments[0].click();", addNewBtn);
        waitVisible(By.id("child_id"));
        System.out.println("▶ Add New Issue Credits/Refund modal opened");
    }

    public void enterChildId(String childId) {
        WebElement input = visible(By.id("child_id"));
        input.clear();
        input.sendKeys(childId);
        System.out.println("▶ Child ID entered: " + childId);
    }

    // ✅ Waits until the invoice dropdown is populated
    //    (or the promotional-only fallback renders)
    public void clickFetchChildDetails() throws InterruptedException {
        visible(By.id("btn_child_details")).click();
        System.out.println("▶ Fetch Child Details clicked");
        wait.until(d -> {
            try {
                return visible(By.id("credit_invoice_ref"))
                        .findElements(By.tagName("option")).size() > 1
                        || !getVisibleValidationMessage().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
        Thread.sleep(500);
    }

    // ✅ Shown in .div-child-name (hidden #child_name
    //    holds the same value)
    public String getFetchedChildName() {
        try {
            return visible(By.cssSelector(".div-child-name")).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ✅ By option VALUE, e.g. "Credits - Book Charges"
    //    (label differs only for "Admission Discount")
    public void selectCreditType(String value) {
        new Select(visible(By.id("credit_type_item"))).selectByValue(value);
        System.out.println("▶ Credit type selected: " + value);
    }

    public List<String> getCreditTypeOptions() {
        return optionTexts(visible(By.id("credit_type_item")));
    }

    // ✅ Picks the first invoice whose number starts with
    //    the given prefix, e.g. "B/" → "[B/2627/7335]".
    //    Returns the invoice number, or "" if none.
    public String selectInvoiceByPrefix(String prefix) {
        Select select = new Select(visible(By.id("credit_invoice_ref")));
        for (WebElement o : select.getOptions()) {
            String text = o.getText();
            if (text.contains("[" + prefix)) {
                select.selectByValue(o.getAttribute("value"));
                String invoiceNo = text.substring(text.indexOf('[') + 1,
                        text.indexOf(']'));
                System.out.println("▶ Invoice selected: " + text.trim());
                return invoiceNo;
            }
        }
        System.out.println("⚠ No invoice with prefix " + prefix);
        return "";
    }

    // ✅ First PAID invoice — credits can be issued
    //    against any paid invoice. Returns invoice no.
    public String selectFirstPaidInvoice() {
        Select select = new Select(visible(By.id("credit_invoice_ref")));
        for (WebElement o : select.getOptions()) {
            String text = o.getText();
            if (text.contains("(paid)")) {
                select.selectByValue(o.getAttribute("value"));
                System.out.println("▶ Invoice selected: " + text.trim());
                return text.substring(text.indexOf('[') + 1, text.indexOf(']'));
            }
        }
        System.out.println("⚠ No paid invoice found");
        return "";
    }

    // ✅ Selects the first PAID invoice whose booking heads include
    //    the given head EXACTLY (e.g. "Preschool Fee" must not match
    //    "Annual Preschool Fee"). Heads come from the modal's own
    //    cached fetchInvoiceReferences payload (arrCreditInvoices).
    //    The app does not validate type↔invoice mapping — this mirrors
    //    how Support maps credits manually. Returns invoice no or "".
    public String selectInvoiceByBookingHead(String bookingHead) {
        Object value = js().executeScript(
                "var head=arguments[0], sel=arguments[1];" +
                        "if(!window.arrCreditInvoices || !arrCreditInvoices.data) return '';" +
                        "var d=arrCreditInvoices.data;" +
                        "for(var i=0;i<sel.options.length;i++){" +
                        " var v=sel.options[i].value; if(!v || !d[v]) continue;" +
                        " if(d[v].label.indexOf('(paid)')<0) continue;" +
                        " var heads=(d[v].booking_heads||'').split('|');" +
                        " if(heads.indexOf(head)>=0) return v; }" +
                        "return '';",
                bookingHead, visible(By.id("credit_invoice_ref")));
        if (value == null || value.toString().isEmpty()) {
            System.out.println("⚠ No paid invoice with booking head: " + bookingHead);
            return "";
        }
        Select select = new Select(visible(By.id("credit_invoice_ref")));
        select.selectByValue(value.toString());
        String text = select.getFirstSelectedOption().getText();
        System.out.println("▶ Invoice selected [" + bookingHead + "]: " + text.trim());
        return text.substring(text.indexOf('[') + 1, text.indexOf(']'));
    }

    // ✅ Selects the invoice with this exact number, e.g.
    //    "P378/2627/495". Returns the number, or "" if absent.
    public String selectInvoiceByNumber(String invoiceNo) {
        Select select = new Select(visible(By.id("credit_invoice_ref")));
        for (WebElement o : select.getOptions()) {
            if (o.getText().contains("[" + invoiceNo + "]")) {
                select.selectByValue(o.getAttribute("value"));
                System.out.println("▶ Invoice selected: " + o.getText().trim());
                return invoiceNo;
            }
        }
        System.out.println("⚠ Invoice not found: " + invoiceNo);
        return "";
    }

    public String getCreditAmountValue() {
        return visible(By.id("credit_amount")).getAttribute("value");
    }

    // ✅ Auto-filled from the invoice for some credit
    //    types (Convenience Charges) — only type when empty
    public void enterCreditAmountIfEmpty(String amount) {
        WebElement input = visible(By.id("credit_amount"));
        String current = input.getAttribute("value");
        if (current == null || current.trim().isEmpty()) {
            input.sendKeys(amount);
            System.out.println("▶ Credit amount entered: " + amount);
        } else {
            System.out.println("▶ Credit amount auto-filled: " + current);
        }
    }

    public void enterCreditLineItem(String lineItem) {
        WebElement input = visible(By.id("invoice_line_items"));
        input.clear();
        input.sendKeys(lineItem);
        System.out.println("▶ Credit line item entered: " + lineItem);
    }

    public void enterComments(String comments) {
        WebElement input = visible(By.id("credit_comments"));
        input.clear();
        input.sendKeys(comments);
        System.out.println("▶ Comments entered: " + comments);
    }

    public void clickSubmitForm() throws InterruptedException {
        visible(By.id("apply_credit")).click();
        System.out.println("▶ Submit Form clicked");
        Thread.sleep(500);
    }

    // ✅ The visible p.error-<field> message in the open
    //    modal ("" if none). Validation shows ONE at a time.
    public String getVisibleValidationMessage() {
        for (WebElement p : driver.findElements(
                By.cssSelector("p[class^='error-']"))) {
            try {
                if (p.isDisplayed() && !p.getText().trim().isEmpty())
                    return p.getText().trim();
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    // ═══════════════════════════════════════════════
    // NATIVE confirm("Confirm?") — Add New only
    // ═══════════════════════════════════════════════
    public String acceptConfirmPopup() {
        Alert alert = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        alert.accept();
        System.out.println("✅ Confirm popup accepted: " + text);
        return text;
    }

    public String dismissConfirmPopup() {
        Alert alert = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        alert.dismiss();
        System.out.println("▶ Confirm popup dismissed: " + text);
        return text;
    }

    public boolean isAlertPresent() {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    // ═══════════════════════════════════════════════
    // FLASH MESSAGE — rendered by flash_message() inside
    // the modal's form; page auto-reloads ~2s later, so
    // this polls quickly. Returns the matching text, or
    // "" on timeout.
    // ═══════════════════════════════════════════════
    public String waitForMessageContaining(String fragment) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(20),
                    Duration.ofMillis(200)).until(d -> {
                for (WebElement el : d.findElements(By.xpath(
                        "//*[text()[contains(.,\"" + fragment + "\")]]"))) {
                    try {
                        if (el.isDisplayed()) return el.getText().trim();
                    } catch (Exception ignored) {
                    }
                }
                return null;
            });
        } catch (Exception e) {
            System.out.println("⚠ Message not found: " + fragment
                    + " | visible alerts: " + getVisibleAlertText());
            return "";
        }
    }

    // ✅ Any visible alert/flash text — for logging a
    //    failure (e.g. a warning from the API)
    public String getVisibleAlertText() {
        try {
            return (String) js().executeScript(
                    "var out=[];document.querySelectorAll('.alert').forEach(function(a){" +
                            "if(a.offsetWidth||a.offsetHeight) out.push(a.innerText.trim());});" +
                            "return out.join(' | ');");
        } catch (Exception e) {
            return "";
        }
    }

    // ═══════════════════════════════════════════════
    // ROW LOOKUP BY CREDIT ID — via the DataTables API
    // so rows on other pages are included
    // ═══════════════════════════════════════════════
    public Set<String> getCreditIdsForChild(String childId) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) js().executeScript(
                "var cid=arguments[0], out=[];" +
                        "$('#DataTables_Table_0').DataTable().rows().nodes().to$().each(function(){" +
                        " var row=$(this);" +
                        " if(row.find(\"a[href='account_statement?child_id=\"+cid+\"']\").length===0) return;" +
                        " var a=row.find(\"a[href*='credit_id=']\").first();" +
                        " if(a.length) out.push(a.attr('href').split('credit_id=')[1]);" +
                        "}); return out;",
                childId);
        return new LinkedHashSet<>(ids);
    }

    // ✅ Text of one column for the row holding the
    //    given credit_id ("" if not found)
    public String getCellByCreditId(String creditId, String header) {
        int idx = headerIndex(header);
        Object result = js().executeScript(
                "var id=arguments[0], idx=arguments[1], out='';" +
                        "$('#DataTables_Table_0').DataTable().rows().nodes().to$().each(function(){" +
                        " var row=this, match=false;" +
                        " $(row).find('a[href]').each(function(){" +
                        "   var h=this.getAttribute('href');" +
                        "   if(h.indexOf('credit_id=')>=0 && h.split('credit_id=')[1]===id) match=true; });" +
                        " if(match) out=$(row).children('td').eq(idx).text().trim();" +
                        "}); return out;",
                creditId, idx);
        return result == null ? "" : result.toString();
    }

    public boolean isActionPresent(String creditId, String hrefPage) {
        Object result = js().executeScript(
                "var id=arguments[0], page=arguments[1], found=false;" +
                        "$('#DataTables_Table_0').DataTable().rows().nodes().to$().find('a[href]').each(function(){" +
                        " var h=this.getAttribute('href');" +
                        " if(h.indexOf(page+'?')===0 && h.split('credit_id=')[1]===id) found=true; });" +
                        "return found;",
                creditId, hrefPage);
        return Boolean.TRUE.equals(result);
    }

    public boolean isRevokeIconPresent(String creditId) {
        return isActionPresent(creditId, "revoke_credit");
    }

    public boolean isLinkInvoiceIconPresent(String creditId) {
        return isActionPresent(creditId, "invoice_reference");
    }

    // ✅ Search by child first so the row is on the
    //    current page, then click its action icon
    private void openActionModal(String childName, String creditId,
                                 String hrefPage, String modalTitle)
            throws InterruptedException {
        searchTable(childName);
        // ✅ A child with many credits spans several pages — show all
        //    matching rows so the target credit is in the DOM
        js().executeScript("$('#DataTables_Table_0').DataTable().page.len(-1).draw();");
        Thread.sleep(1000);
        WebElement icon = driver.findElement(By.cssSelector(
                "#DataTables_Table_0 a[href^='" + hrefPage
                        + "?'][href$='credit_id=" + creditId + "']"));
        js().executeScript("arguments[0].click();", icon);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//h4[contains(@class,'modal-title')][contains(normalize-space(.),'"
                        + modalTitle + "')]")));
        Thread.sleep(1000);
        System.out.println("▶ " + modalTitle + " modal opened | credit_id="
                + creditId);
    }

    // ✅ Full text of the open Link/Revoke modal — walks
    //    up from its title until the details are included
    public String getActionModalText(String modalTitle) {
        Object text = js().executeScript(
                "var hs=document.querySelectorAll('h4.modal-title'), h=null;" +
                        "for(var i=0;i<hs.length;i++){ if(hs[i].innerText.indexOf(arguments[0])>=0" +
                        "   && (hs[i].offsetWidth||hs[i].offsetHeight)) h=hs[i]; }" +
                        "if(!h) return '';" +
                        "var c=h; while(c.parentElement && c.innerText.indexOf('Child ID')<0) c=c.parentElement;" +
                        "return c.innerText;", modalTitle);
        return text == null ? "" : text.toString();
    }

    // ═══════════════════════════════════════════════
    // LINK INVOICE REFERENCE MODAL
    // No confirm() — Submit posts directly.
    // ═══════════════════════════════════════════════
    public void openLinkInvoiceModal(String childName, String creditId)
            throws InterruptedException {
        openActionModal(childName, creditId, "invoice_reference",
                "Link Invoice Reference");
    }

    // ✅ Opens with the credit's CURRENT invoice selected
    public String getLinkSelectedInvoiceText() {
        return new Select(visible(By.id("credit_invoice_ref")))
                .getFirstSelectedOption().getText().trim();
    }

    // ✅ Selects the first real invoice that is NOT the
    //    currently-linked one. Returns its invoice number.
    public String selectDifferentLinkInvoice() {
        Select select = new Select(visible(By.id("credit_invoice_ref")));
        String current = select.getFirstSelectedOption().getAttribute("value");
        for (WebElement o : select.getOptions()) {
            String value = o.getAttribute("value");
            if (value.isEmpty() || value.equals(current)) continue;
            select.selectByValue(value);
            String text = o.getText();
            System.out.println("▶ Link invoice selected: " + text.trim());
            return text.substring(text.indexOf('[') + 1, text.indexOf(']'));
        }
        return "";
    }

    public void selectLinkInvoicePlaceholder() {
        new Select(visible(By.id("credit_invoice_ref"))).selectByValue("");
    }

    public void clickLinkSubmit() throws InterruptedException {
        visible(By.id("btn_submit_reference")).click();
        System.out.println("▶ Link Invoice Submit clicked");
        Thread.sleep(500);
    }

    // ═══════════════════════════════════════════════
    // REVOKE CREDIT MODAL
    // No confirm() — Revoke posts directly.
    // ═══════════════════════════════════════════════
    public void openRevokeModal(String childName, String creditId)
            throws InterruptedException {
        openActionModal(childName, creditId, "revoke_credit",
                "Revoke Credit");
    }

    public void enterRevokeReason(String reason) {
        WebElement input = visible(By.id("credit_revoke_reason"));
        input.clear();
        input.sendKeys(reason);
        System.out.println("▶ Revoke reason entered: " + reason);
    }

    public void clickRevoke() throws InterruptedException {
        visible(By.id("btn_submit_reason")).click();
        System.out.println("▶ Revoke clicked");
        Thread.sleep(500);
    }
}
