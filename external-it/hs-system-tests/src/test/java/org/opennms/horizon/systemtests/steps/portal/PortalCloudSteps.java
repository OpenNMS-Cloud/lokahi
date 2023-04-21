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

    @Then("the IT Administrator opens 'Details' for the instance")
    public void openDetailsForTheInstance() {
        PortalCloudPage.clickDetailsForFirstInstance();
    }

    @Then("the IT Administrator is brought back to the OpenNMS Cloud page")
    public void thePageIsNotCoveredByAnyPopup() {
        PortalCloudPage.mainPageIsNotCoveredByPopups();
    }

    @Then("click on 'Log in' button for the instance")
    public void clickOnLogInBtn() {
        PortalCloudPage.clickLogInForFirstInstance(); // it opens a new tab
        Selenide.closeWindow();
        Selenide.switchTo().window(0);
    }

    @Given("prepare {string} instance")
    public void prepareInstance(String instanceName) {
        PortalCloudPage.clickAddInstance();
        AddNewInstancePopup.setInstanceName(instanceName);
        AddNewInstancePopup.clickSubmitBtn();
        PortalCloudPage.mainPageIsNotCoveredByPopups();
        PortalCloudPage.setFilter(instanceName);
    }
}
