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
import org.opennms.horizon.systemtests.pages.portal.EditInstanceNamePopup;

import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disappear;

public class EditInstanceNameSteps {
    @Then("set new name {string} in 'Instance Name' field")
    public void setNewInstanceName(String instanceName) {
        EditInstanceNamePopup.setNewInstanceName(instanceName);
    }

    @Then("click on 'UPDATE' button")
    public void clickUpdateBtn() {
        EditInstanceNamePopup.clickUpdateBtn();
    }

    @Then("click on 'CANCEL' button to close 'Edit Instance' popup")
    public void clickCancelBtn() {
        EditInstanceNamePopup.clickCancelBtn();
        EditInstanceNamePopup.waitNewState(disappear);
    }

    @Then("click on 'X' button to close 'Edit Instance' popup")
    public void clickCloseBtn() {
        EditInstanceNamePopup.clickCloseBtn();
        EditInstanceNamePopup.waitNewState(disappear);
    }

    @Then("'Edit Instance' popup appears")
    public void waitPopup() {
        EditInstanceNamePopup.waitNewState(appear);
    }

    @Then("'Edit Instance' popup disappear")
    public void waitPopupDisappear() {
        EditInstanceNamePopup.waitNewState(disappear);
    }

    @Then("'Edit Instance' popup shows an error message {string} for Instance name")
    public void verifyErrorMessage(String errorMessage) {
        EditInstanceNamePopup.verifyErrorMessage(errorMessage);
    }

    @And("has correct URL link in the clipboard that matches with the URL field")
    public void hasCorrectURLLinkInTheClipboard() {
        EditInstanceNamePopup.verifyClipboardValue();
    }
}
