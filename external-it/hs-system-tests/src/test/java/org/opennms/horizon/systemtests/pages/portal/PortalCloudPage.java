/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/


package org.opennms.horizon.systemtests.pages.portal;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.List;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exactValue;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.not;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$x;

public class PortalCloudPage {

    private static final SelenideElement headerTxt = $("#header");
    private static final SelenideElement addInstanceBtn = $("#cloud-add-instance-button");
    private static final SelenideElement instanceTable = $("table.condensed  tbody");
    private static final ElementsCollection names = $$x("//tbody/tr/td[position()=1]");
    private static final SelenideElement instanceNameTitle = $("th[data-ref-id='feather-sort-header']");
    private static final SelenideElement logInBtn = $("#cloud-instances-row-login-0");
    private static final SelenideElement detailsBtn = $("#cloud-instances-row-details-0");
    private static final SelenideElement clearSearchBtn = $("#cloud-instances-nodata-clear-search");
    private static final SelenideElement addInstanceNoDataBtn = $("#cloud-instances-nodata-add-instance");
    private static final SelenideElement searchInstanceInp = $("#cloud-instances-search");
    private static final SelenideElement shadedBackground = $("div.backdrop");
    private static final SelenideElement spinner = $(".spinner");
    private static final SelenideElement noDataTxt = $("h3.empty-view");
    private static final SelenideElement noDataSubTxt = $("p.empty-view-sub");
    private static final SelenideElement rowToHide = $("tr.data-table-leave-to");

    private static final SelenideElement firstPageBtn = $("[data-ref-id='feather-pagination-first-button']");
    private static final SelenideElement previousPageBtn = $("[data-ref-id='feather-pagination-previous-button']");
    private static final SelenideElement nextPageBtn = $("[data-ref-id='feather-pagination-next-button']");
    private static final SelenideElement lastPageBtn = $("[data-ref-id='feather-pagination-last-button']");

    public static void verifyMainPageHeader() {
        headerTxt.shouldBe(visible, Duration.ofMinutes(1)).shouldHave(text("OpenNMS Cloud"));
    }

    public static void clickAddInstance() {
        addInstanceBtn.shouldBe(enabled, Duration.ofSeconds(20)).click();
    }

    public static void clickAddInstanceWhenNoData() {
        addInstanceNoDataBtn.shouldBe(enabled, Duration.ofSeconds(20)).click();
    }

    public static void instantShouldBePresentedInTable(String instanceName) {
        instanceTable.find(byText(instanceName)).shouldBe(visible);
    }

    public static void searchShowsNothingFound() {
        noDataTxt.shouldBe(visible, text("No results found."));
        noDataSubTxt.shouldHave(text("Clear search or check your search input."));
        clearSearchBtn.shouldBe(enabled);
        addInstanceNoDataBtn.shouldBe(hidden);
    }

    public static void clickOnClearSearchBtn() {
        clearSearchBtn.shouldBe(enabled).click();
    }

    public static void clickLogInForFirstInstance() {
        logInBtn.shouldBe(enabled).click();
    }

    public static void clickLogInForSpecificInstance(String instanceName) {
        $(byText(instanceName)).ancestor("tr").find("button").shouldBe(enabled).click();
    }

    public static void clickDetailsForSpecificInstance(String instanceName) {
        $(byText(instanceName)).ancestor("tr").find(By.xpath(".//button[position()=2]")).shouldBe(enabled).click();
    }

    public static void clickDetailsForFirstInstance() {
        detailsBtn.shouldBe(enabled).click();
    }

    public static void setFilter(String pattern) {
        searchInstanceInp.shouldBe(enabled, Duration.ofSeconds(20)).setValue("").sendKeys(pattern);
        spinner.shouldBe(appear).shouldBe(disappear, Duration.ofMinutes(1));
        Selenide.sleep(1_000);
        if (instanceTable.isDisplayed()) {
            rowToHide.shouldBe(disappear);
        }
    }

    public static void addTextToFilter(String pattern) {
        searchInstanceInp.shouldBe(enabled).sendKeys(pattern);
        spinner.shouldBe(appear).shouldBe(disappear, Duration.ofMinutes(1));
        Selenide.sleep(1_000);
        if (instanceTable.isDisplayed()) {
            rowToHide.shouldBe(disappear);
        }
    }

    public static void mainPageIsNotCoveredByPopups() {
        shadedBackground.shouldBe(hidden);
    }

    // the instances name are sorted
    public static void compareInstanceNamesInTheTable(List<String> instanceName) {
        names.shouldHave(CollectionCondition.exactTexts(instanceName));
    }

    public static void verifySearchField() {
        searchInstanceInp.shouldHave(exactText(""), exactValue(""));
    }

    public static void verifyNoInstances() {
        noDataTxt.shouldBe(visible, text("No instances available."));
        noDataSubTxt.shouldHave(text("Get started by selecting Add Instance."));
        addInstanceNoDataBtn.shouldBe(enabled);
        clearSearchBtn.shouldBe(hidden);
    }

    public static void clickInstanceNameTitle() {
        instanceNameTitle.click();
    }

    public static void verifyInstanceNameSorting(String sortingType) {
        instanceNameTitle.shouldHave(attribute("aria-sort", sortingType));
    }

    public static void verifyPaginationForCurrentPage(
        boolean firstDisabled, boolean previousDisabled, boolean nextDisabled, boolean lastDisabled
    ) {
        firstPageBtn.shouldHave(firstDisabled ? attribute("aria-disabled") : not(attribute("aria-disabled")));
        previousPageBtn.shouldHave(previousDisabled ? attribute("aria-disabled") : not(attribute("aria-disabled")));
        nextPageBtn.shouldHave(nextDisabled ? attribute("aria-disabled") : not(attribute("aria-disabled")));
        lastPageBtn.shouldHave(lastDisabled ? attribute("aria-disabled") : not(attribute("aria-disabled")));
    }

    public static void clickNextPageBtn() {
        nextPageBtn.click();
        spinner.shouldBe(appear).shouldBe(disappear, Duration.ofMinutes(1));
        Selenide.sleep(1_000);
    }

    public static void clickPreviousPageBtn() {
        previousPageBtn.click();
        spinner.shouldBe(appear).shouldBe(disappear, Duration.ofMinutes(1));
        Selenide.sleep(1_000);
    }
}
