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

import io.cucumber.java.en.But;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.portal.DeleteInstancePopup;

public class DeleteInstanceSteps {

    @Then("'Instance Delete Confirmation' popup appears")
    public void popupIdDisplayed() {
        DeleteInstancePopup.waitPopupIsDisplayed(true);
    }

    @Then("the IT Administrator enters {string} as an 'Instance Name'")
    public void setInstnanceName(String instanceName) {
        DeleteInstancePopup.setInstanceNameToConfirmationInput(instanceName);
    }

    @Then("click on 'DELETE' button to confirm deletion")
    public void clickDeleteBtn() {
        DeleteInstancePopup.clickOnDeleteBtn();
    }

    @Then("click on 'CANCEL' button to close the popup")
    public void clickCancelBtn() {
        DeleteInstancePopup.clickOnCancelBtn();
        DeleteInstancePopup.waitPopupIsDisplayed(false);
    }

    @Then("click on 'X' button to close the popup")
    public void clickCloseBtn() {
        DeleteInstancePopup.clickOnCloseBtn();
        DeleteInstancePopup.waitPopupIsDisplayed(false);
    }

    @But("sees error message {string} for 'Instance Name' field")
    public void verifyErrorMessage(String errorMessage) {
        DeleteInstancePopup.verifyErrorText(errorMessage);
    }

    @Then("error message for 'Instance Name' field disappears")
    public void verifyNoErrorMessage() {
        DeleteInstancePopup.verifyNoError();
    }
}
