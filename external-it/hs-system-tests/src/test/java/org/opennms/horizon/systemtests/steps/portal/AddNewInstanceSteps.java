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

import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.portal.AddNewInstancePopup;
import org.opennms.horizon.systemtests.utils.TestDataStorage;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.opennms.horizon.systemtests.CucumberHooks.INSTANCES;

public class AddNewInstanceSteps {

    @Then("the IT Administrator fills {string} in 'Instance name'")
    public void setInstanceName(String instanceName) {
        if (instanceName.startsWith("random")) {
            instanceName = "Instance_" + RandomStringUtils.randomAlphabetic(10);
            INSTANCES.add(instanceName);
        }
        AddNewInstancePopup.setInstanceName(instanceName);
    }

    @Then("and selects 'Me' as option for 'Assign this instance to:'")
    public void selectMeAsAssignedUser() {
        AddNewInstancePopup.selectMeOption();
    }

    @Then("and selects 'Someone else' as option for 'Assign this instance to:'")
    public void selectSomeoneElseAsAssignedUser() {
        AddNewInstancePopup.selectSomeoneElseOption();
    }

    @Then("set assigned user email as {string}")
    public void setEmailAddressForAssignedUser(String email) {
        String userEmail = TestDataStorage.mapUserToEmail(email);
        AddNewInstancePopup.setAssignedUserEmail(userEmail);
        AddNewInstancePopup.confirmEmailInDropdown(userEmail);
    }

    @Then("the IT Administrator clicks on 'ADD INSTANCE' button")
    public void clickOnAddInstanceBtn() {
        AddNewInstancePopup.clickSubmitBtn();
    }

    @Then("the IT Administrator clicks on 'X' button to close popup")
    public void clickOnCancelBtn() {
        AddNewInstancePopup.clickCloseBtn();
    }

    @Then("an error message {string} appears for 'Instance name' field")
    public void verifyErrorMessageForInstanceName(String errorMessage) {
        AddNewInstancePopup.verifyErrorMessageForInstanceName(errorMessage);
    }

    @Then("the 'error message' for 'Instance name' is no longer displayed")
    public void verifyNoErrorMessageForInstanceName() {
        AddNewInstancePopup.verifyNoErrorMessageForInstanceName();
    }

    @Then("an error message {string} appears for 'Email address' field")
    public void verifyErrorMessageForEmailAddress(String errorMessage) {
        AddNewInstancePopup.verifyErrorMessageForEmailAddress(errorMessage);
    }

    @Then("the 'error message' for 'Email address' is no longer displayed")
    public void verifyNoErrorMessageForEmailAddress() {
        AddNewInstancePopup.verifyNoErrorMessageForEmailAddress();
    }
}
