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

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.portal.EditInstancePage;
import org.opennms.horizon.systemtests.utils.TestDataStorage;

public class EditInstanceSteps {

    @Then("the IT Administrator sees {string} as a single user for the instance")
    public void instanceHasASingleUser(String email) {
        EditInstancePage.verifyNumberOfUsers(1);
        String userEmail = TestDataStorage.mapUserToEmail(email);
        EditInstancePage.verifyUserEmailInTable(userEmail);
    }

    @Then("click on 'DELETE INSTANCE' button")
    public void clickDeleteInstanceBtn() {
        EditInstancePage.clickDeleteInstance();
    }

    @Then("the IT Administrator sees the 'Cloud Instance Details' page for the {string} instance")
    public void checkWeAreOnDetailsPage(String instanceName) {
        EditInstancePage.verifyPageTitle();
        EditInstancePage.verifyInstanceName(instanceName);
    }

    @Then("click on the instance 'URL' link")
    public void clickOnUrl() {
        EditInstancePage.clickOnInstanceUrl();
    }

    @Then("click on 'edit' for instance name")
    public void clickOnEditInstanceName() {
        EditInstancePage.clickEditNameBtn();
    }

    @Then("click on 'copy' button for URL")
    public void clickOnCopyURLButton() {
        EditInstancePage.clickCopyURLButton();
    }

    @And("search 'Search for user' input is empty")
    public void searchSearchForUserIsEmpty() {
        EditInstancePage.verifySearchFieldIsEmpty();
    }

    @Then("click to 'go back' button to return to the OpenNMS Cloud page")
    public void clickGoBackBtn() {
        EditInstancePage.clickGoBackButton();
    }
}
