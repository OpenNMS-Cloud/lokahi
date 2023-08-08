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
package org.opennms.horizon.systemtests.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.opennms.horizon.systemtests.steps.DiscoverySteps;
import org.openqa.selenium.By;

import java.time.Duration;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
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
    private static final SelenideElement ADD_DISCOVERY_BUTTON = $(By.xpath("//button[@data-test='addDiscoveryBtn']"));
    private static final SelenideElement SAVE_DISCOVERY_BUTTON = $(By.xpath("//button[@data-test='btn-submit']"));
    private static final SelenideElement SNMP_DISCOVERY_RADIO_BUTTON = $(By.xpath("//div[@data-test='discoveryICMP']"));
    private static final SelenideElement DISCOVERY_NAME_INPUT = $(By.xpath("//div[@data-test='discoveryNameInput']//input"));
    private static final SelenideElement LOCATION_NAME_INPUT = $(By.xpath("//input[@data-test='locationsInput']"));
    private static final SelenideElement IP_RANGE_INPUT = $(By.xpath("//div[@data-test='ipAddressInput']//div[@class='content-editable']"));
    private static final SelenideElement COMMUNITY_STRING_INPUT = $(By.xpath("//div[@data-test='communityInput']//div[@class='content-editable']"));
    private static final SelenideElement PORT_INPUT = $(By.xpath("//div[@data-test='portInput']//div[@class='content-editable']"));
    private static final SelenideElement VIEW_DETECTED_NODES_BUTTON = $(By.xpath("//button[@data-test='viewDetectedNodesButton']"));

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

    public static boolean newDiscoveryCheckForLocation(String locationName) {
        String search = "//div[@class='locations-select']//span[text()=' " + locationName +"']";
        // Sometimes a delay in the UI to populate the default selected location
        Selenide.sleep(1000);

        return $(By.xpath(search)).exists();
    }

    public static void performDiscovery(String discoveryName, String locationName, int port,
                                        String community, String ip) {
        LeftPanelPage.clickOnPanelSection("discovery");
        ADD_DISCOVERY_BUTTON.shouldBe(enabled).click();
        SNMP_DISCOVERY_RADIO_BUTTON.shouldBe(enabled).click();
        DISCOVERY_NAME_INPUT.shouldBe(enabled).sendKeys(discoveryName);

        if (!newDiscoveryCheckForLocation(locationName)) {
            // When only 1 location exists, it is automatically selected
            LOCATION_NAME_INPUT.shouldBe(enabled).sendKeys(locationName);
            LOCATION_NAME_INPUT.sendKeys("\n");

            String specificListItemSearch = "//ul[@aria-label='Select a location']/li[.//span/text()=' " + locationName + "']";
            SelenideElement locationPopupListItem = $(By.xpath(specificListItemSearch));
            locationPopupListItem.should(exist, Duration.ofSeconds(20)).shouldBe(enabled).click();
        }

        IP_RANGE_INPUT.shouldBe(enabled).sendKeys(ip);

        PORT_INPUT.shouldBe(enabled).clear();
        PORT_INPUT.sendKeys(Integer.toString(port));
        COMMUNITY_STRING_INPUT.shouldBe(enabled).clear();
        COMMUNITY_STRING_INPUT.sendKeys(community);

        SAVE_DISCOVERY_BUTTON.shouldBe(enabled).click();
        VIEW_DETECTED_NODES_BUTTON.should(exist).shouldBe(enabled).click();
    }

    public static void deleteAllDiscoveries() {
        // TODO: Delete all discoveries once it is supported
    }
}
