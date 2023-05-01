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
package org.opennms.horizon.systemtests.pages.cloud;

import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class AppliancePage {

    private static final SelenideElement minionRemoveButton = $("[data-test='minion-item-delete-btn']");
    private static final SelenideElement minionLatency = $("[data-test='minion-item-latency']");
    private static final SelenideElement minionStatus = $("[data-test='minion-item-status']");

    private static final SelenideElement addDeviceButton = $("[data-test='add-node-btn']");
    private static final SelenideElement deviceNameInput = $("[data-test='name-input'] input");
    private static final SelenideElement deviceIpInput = $("[data-test='ip-input'] input");

    private static final SelenideElement saveButton = $("[data-test='save-btn']");
    private static final SelenideElement cancelButton = $("[data-test='cancel-btn']");

    public static void setDeviceNameInput(String name) {
        deviceNameInput.shouldBe(enabled).sendKeys(name);
    }

    public static void setDeviceIpInput(String ipAddress) {
        deviceIpInput.shouldBe(enabled).sendKeys(ipAddress);
    }

    public static void clickSaveButton() {
        saveButton.shouldBe(enabled).click();
    }

    public static void clickCancelButton() {
        cancelButton.shouldBe(enabled).click();
    }

    public static void clickAddDeviceButton() {
        addDeviceButton.shouldBe(enabled).click();
    }

    public static String getMinionStatus() {
        return minionStatus.getText();
    }

    public static void waitMinionStatus(String status) {
        minionStatus.shouldHave(text(status), Duration.ofSeconds(31));
    }

    public static void getDeviceStatusWithName(String status, String name) {
        SelenideElement deviceStatus = $x(String.format("//div[@data-test='node-item' and contains(., '%s')]/div[@data-test='node-item-status']/div", name));
        deviceStatus.shouldHave(text(status));
    }

    public static String getMinionLatency() {
        return minionLatency.getText();
    }

    public static void clickRemoveMinion() {
        minionRemoveButton.shouldBe(enabled).click();
    }

    public static void checkIsRemoveButtonShown(boolean condition) {
        minionRemoveButton.shouldBe(condition ? enabled : hidden);
    }

    public static void checkIsAddDeviceButtonVisible() {
        addDeviceButton.shouldBe(enabled);
    }

}
