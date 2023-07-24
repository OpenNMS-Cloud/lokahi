package org.opennms.horizon.systemtests.pages;

import com.codeborne.selenide.SelenideElement;
import org.junit.Assert;
import java.time.Duration;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class FlowsPage {
    private static final SelenideElement headerTxt = $("[data-test='flows-page-header']");
    private static final SelenideElement todayOption = $("#TODAY");
    private static final SelenideElement last24HoursOption = $("#LAST_24_HOURS");
    private static final SelenideElement last7DaysOption = $("#SEVEN_DAYS");
    private static final SelenideElement noDataTxt = $x("//div[@class='subtitle'][contains(text(), 'No applications data')]");
    private static final SelenideElement trafficChart = $("div.table-chart-container");
    private static final SelenideElement exporterInp = $("[data-ref-id='feather-autocomplete-input']");

    public static void verifyNoDataTitle() {
        noDataTxt.should(exist);
    }

    public static void verifyChartVisibility() {
        trafficChart.shouldBe(visible);
    }

    public static void clickOnExporterInput() {
        exporterInp.shouldBe(enabled).click();
    }

    public static void setValueInExporterFilter(String pattern) {
        exporterInp.setValue("").sendKeys(pattern);
    }

    public static void checkDropdown(String exporterName) {
        setValueInExporterFilter(exporterName.substring(0, 3));
        SelenideElement dropdown = $("[class='feather-list-item hover focus result-item hover']").shouldBe(visible);
        Assert.assertTrue(dropdown.getText().toLowerCase().contains(exporterName.toLowerCase()));
    }

    public static void waitPageLoaded() {
        headerTxt.shouldBe(visible, Duration.ofSeconds(10));
    }

}
