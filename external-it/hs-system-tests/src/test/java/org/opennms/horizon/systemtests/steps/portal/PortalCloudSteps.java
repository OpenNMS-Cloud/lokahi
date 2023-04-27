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


package org.opennms.horizon.systemtests.steps.portal;

import com.codeborne.selenide.Selenide;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.portal.AddNewInstancePopup;
import org.opennms.horizon.systemtests.pages.portal.PortalCloudPage;

import java.util.List;

import static org.opennms.horizon.systemtests.CucumberHooks.INSTANCES;

public class PortalCloudSteps {

    @Then("Verify that user logged in to Portal successfully")
    public void verifyThatUserLoggedInToPortal() {
        PortalCloudPage.verifyMainPageHeader();
    }

    @Then("a IT Administrator clicks on '+ADD INSTANCE' button")
    public void clickOnAddInstanceButton() {
        PortalCloudPage.clickAddInstance();
        AddNewInstancePopup.waitPopupIsDisplayed(true);
    }

    @Then("the IT Administrator sees an instance {string} in the list")
    public void findInstanceNameInTheTable(String instanceName) {
        if (instanceName.startsWith("random")) {
            instanceName = INSTANCES.get(0);
        }
        PortalCloudPage.setFilter(instanceName);
        PortalCloudPage.instantShouldBePresentedInTable(instanceName);
    }

    @Then("the IT Administrator sees a list of instances in the list")
    public void checkVisibilityOfInstances(List<String> instanceName) {
        PortalCloudPage.compareInstanceNamesInTheTable(instanceName);
    }

    @Then("set {string} in the 'Search Instance Name' field")
    public void setSearchFilter(String pattern) {
        PortalCloudPage.setFilter(pattern);
    }

    @Then("add {string} to the 'Search Instance Name' field")
    public void addTextToSearchFilter(String pattern) {
        PortalCloudPage.addTextToFilter(pattern);
    }

    @Then("the IT Administrator doesn't see an instance {string} in the list")
    public void checkThatInstanceIsAbsent(String instanceName) {
        if (instanceName.startsWith("random")) {
            instanceName = INSTANCES.get(0);
        }
        PortalCloudPage.setFilter(instanceName);
        PortalCloudPage.searchShowsNothingFound();
    }

    @Then("sees 'No results found'")
    public void verifyInstancesNoFound() {
        PortalCloudPage.searchShowsNothingFound();
    }

    @Then("sees 'No instances available.'")
    public void verifyNoInstances() {
        PortalCloudPage.verifyNoInstances();
    }

    @Then("click on 'CLEAR SEARCH' button")
    public void clickClearSearchButton() {
        PortalCloudPage.clickOnClearSearchBtn();
    }

    @Then("click on 'ADD INSTANCE' button")
    public void clickAddInstanceNoButtonButton() {
        PortalCloudPage.clickAddInstanceWhenNoData();
    }

    @Then("'Search Instance Name' field is empty")
    public void verifySearchField() {
        PortalCloudPage.verifySearchField();
    }

    @Then("the IT Administrator opens 'Details' for the instance")
    public void openDetailsForTheInstance() {
        PortalCloudPage.clickDetailsForFirstInstance();
    }

    @Then("the IT Administrator is brought back to the OpenNMS Cloud page")
    public void thePageIsNotCoveredByAnyPopup() {
        PortalCloudPage.mainPageIsNotCoveredByPopups();
        PortalCloudPage.verifyMainPageHeader();
    }

    @Then("click on 'Log in' button for the instance")
    public void clickOnLogInBtn() {
        PortalCloudPage.clickLogInForFirstInstance(); // it opens a new tab
        Selenide.closeWindow();
        Selenide.switchTo().window(0);
    }

    @Then("click on 'Log in' button for {string} instance")
    public void clickOnLogInBtnForInstance(String instanceName) {
        PortalCloudPage.clickLogInForSpecificInstance(instanceName); // it opens a new tab
        Selenide.switchTo().window(1);
    }

    @Then("click on 'Details' button for {string} instance")
    public void clickOnDetailsBtnForInstance(String instanceName) {
        PortalCloudPage.clickDetailsForSpecificInstance(instanceName); // it opens a new tab
    }

    @Given("prepare {string} instance")
    public void prepareInstance(String instanceName) {
        PortalCloudPage.clickAddInstance();
        AddNewInstancePopup.setInstanceName(instanceName);
        AddNewInstancePopup.clickSubmitBtn();
        PortalCloudPage.mainPageIsNotCoveredByPopups();
        PortalCloudPage.setFilter(instanceName);
    }

    @Then("click on 'Instance Name' title to change sorting")
    public void clickOnInstanceNameTitleToChangeSorting() {
        PortalCloudPage.clickInstanceNameTitle();
    }

    @Then("see {string} sorting icon for 'Instance name'")
    public void checkSortingForInstanceName(String sortingType) {
        PortalCloudPage.verifyInstanceNameSorting(sortingType); // none, ascending, descending
    }

    @Then("close the instance page")
    public void closeCurrentTab() {
        Selenide.closeWindow();
        Selenide.switchTo().window(0);
    }

    @Then("user sees the pagination control panel for the {string} page")
    public void userSeesThePaginationControlPanelForTheFirstPage(String page) {
        if (page.equals("single")) {
            PortalCloudPage.verifyPaginationForCurrentPage(true, true, true, true);
        } else if (page.equals("first")) {
            PortalCloudPage.verifyPaginationForCurrentPage(true, true, false, false);
        } else if (page.equals("last")) {
            PortalCloudPage.verifyPaginationForCurrentPage(false, false, true, true);
        } else {
            PortalCloudPage.verifyPaginationForCurrentPage(false, false, false, false);
        }
    }

    @Then("click on 'next page' button")
    public void clickNextPageButton() {
        PortalCloudPage.clickNextPageBtn();
    }

    @Then("click on 'previous page' button")
    public void clickPreviousPageButton() {
        PortalCloudPage.clickPreviousPageBtn();
    }

    @Then("debug")
    public void debug() {
        System.out.println();
    }
}
