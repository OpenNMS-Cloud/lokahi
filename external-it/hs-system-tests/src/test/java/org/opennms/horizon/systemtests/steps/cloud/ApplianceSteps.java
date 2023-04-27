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

package org.opennms.horizon.systemtests.steps.cloud;

import com.codeborne.selenide.Selenide;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.cloud.AppliancePage;
import org.opennms.horizon.systemtests.utils.TestDataStorage;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

public class ApplianceSteps {

    @Then("check {string} minion in the list")
    public static void waitWhenMinionAppear(String minionName) {
        minionName = TestDataStorage.getMinionName(minionName);

        for (int i = 0; i < 2; i++) {
            // since the minion doesn't show up we need to wait and refresh the page
            if (!$(byText(minionName.toUpperCase())).isDisplayed()) {
                waitHeartbeat(1);
            }
        }
    }

    @Then("wait for {long} heartbeats")
    public static void waitHeartbeat(long count) {
        Selenide.sleep(count * 30_000);
        Selenide.refresh();
    }

    @Then("Is the 'delete' Minion button displayed? {string}")
    public void checkIsRemoveMinionButtonShown(String isDisplayed) {
        boolean condition = TestDataStorage.stringToBoolean(isDisplayed);
        AppliancePage.checkIsRemoveButtonShown(condition);
    }

    @Then("check the status of the minion is {string}")
    public void checkTheStatusOfTheMinionIs(String status) {
        AppliancePage.waitMinionStatus(status);
    }

    @Then("click on the 'delete' button for minion")
    public void removeMinionFromTheList() {
        AppliancePage.clickRemoveMinion();
    }

    @Then("check 'Add Device' button is accessible and visible")
    public void checkAddDeviceButtonIsAccessibleAndVisible() {
        AppliancePage.checkIsAddDeviceButtonVisible();
    }

    @Then("click on 'Add Device' button to open a pop up window")
    public void clickOnAddDeviceButtonToOpenAPopUpWindow() {
        AppliancePage.clickAddDeviceButton();
    }

    @Then("check the status of the device with name {string} as status {string}")
    public void checkTheStatusOfTheAddedDeviceAsStatus(String name, String status) {
        AppliancePage.getDeviceStatusWithName(status, name);
    }

    @Then("Add device name {string}")
    public void addDeviceName(String deviceName) {
        AppliancePage.setDeviceNameInput(deviceName);
    }

    @Then("Add device IP address {string}")
    public void addDeviceIPAddress(String deviceIpAddress) {
        AppliancePage.setDeviceIpInput(deviceIpAddress);
    }

    @Then("Click on save button")
    public void clickOnSaveButton() {
        AppliancePage.clickSaveButton();
    }
}
