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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class DiscoveryPage {

    private static final SelenideElement addDiscoveryButton = $("[class='btn hover focus btn-primary has-icon']");
    private static final SelenideElement SNMPRadioButton = $("[class='radio hover focus']");
    private static final SelenideElement discoveryNameInputField = $("[class='feather-input']");
    private static final SelenideElement discoveryLocationNameInputField = $$("[class='feather-autocomplete-input']").get(1);
    private static final SelenideElement discoveryLocationNamesDropdown = $("[data-ref-id='feather-autocomplete-menu-container-dropdown']");
    private static final SelenideElement discoveryIPInputField = $("[id='contentEditable_1']");
    private static final SelenideElement saveDiscoveryButton = $("[type='submit']");
    private static final SelenideElement addAnotherDiscoveryButton = $("[class='btn hover focus btn-text has-icon']");

    public static void selectICMP_SNMP() {
        SNMPRadioButton.shouldBe(Condition.visible, Condition.enabled).click();
    }

    public static void clickAddDiscovery() {
        addDiscoveryButton.shouldBe(Condition.visible, Condition.enabled).click();
    }

    public static void createNewSnmpDiscovery(String discoveryName, String locationName, String ip) {
        discoveryNameInputField.shouldBe(Condition.visible, Condition.enabled).sendKeys(discoveryName);
        discoveryLocationNameInputField.shouldBe(Condition.visible, Condition.enabled).sendKeys(locationName.substring(0, 3));
        discoveryLocationNamesDropdown.$(withText(locationName)).click();
        discoveryIPInputField.shouldBe(Condition.visible, Condition.enabled).sendKeys(ip);
        saveDiscoveryButton.shouldBe(Condition.visible, Condition.enabled).click();
        addAnotherDiscoveryButton.shouldBe(Condition.visible, Condition.enabled).click();
    }

}
