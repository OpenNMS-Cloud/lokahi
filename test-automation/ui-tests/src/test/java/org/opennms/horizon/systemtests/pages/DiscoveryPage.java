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
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import java.time.Duration;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.withText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.$x;

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
    private static final SelenideElement SAVE_DISCOVERY_BUTTON = $(By.xpath("//button[text()[contains(., 'Save')]]"));
    private static final SelenideElement SNMP_DISCOVERY_BUTTON = $(By.xpath("//div[@class='type-selector'][contains(.//*/text() , 'ICMP/SNMP')]"));
    private static final SelenideElement DISCOVERY_NAME_INPUT = $(By.xpath("//div[./div[@class='feather-input-border']][contains(./div/div/label/text(), 'Discovery Name')]/div/input"));
    private static final SelenideElement LOCATION_NAME_INPUT = $(By.xpath("//input[@placeholder='Choose Location']"));
    private static final SelenideElement LOCATION_DROPDOWN_ICON = $(By.xpath("//input[@placeholder='Choose Location']/../../div[@class='post']"));
    private static final SelenideElement IP_RANGE_INPUT = $(By.xpath("//div[./div[@class='feather-input-border']][contains(./div/div/label/text(), 'Enter IP Ranges')]/div/textarea"));
    private static final SelenideElement COMMUNITY_STRING_INPUT = $(By.xpath("//div[./div[@class='feather-input-border']][contains(./div/div/label/text(), 'Community String')]/div/textarea"));
    private static final SelenideElement PORT_INPUT = $(By.xpath("//div[./div[@class='feather-input-border']][contains(./div/div/label/text(), 'Enter UDP Port')]/div/textarea"));
    private static final SelenideElement VIEW_DETECTED_NODES_BUTTON = $(By.xpath("//button[./*/text()[contains(., 'Go To Inventory')]]"));
    private static final ElementsCollection ACTIVE_DISCOVERY_CARDS = $$(By.xpath("//div[@class='card-my-discoveries']/div[@class='list']/div"));
    private static final SelenideElement DISCOVERY_DELETE_BUTTON = $x("//span[@class='btn-content'][text() = 'Delete']");
    private static final SelenideElement DELETE_DISCOVERY_CONFIRM_YES = $x("//div[@data-ref-id='feather-dialog-footer']//button[text()='Yes']");
    private static final SelenideElement POPUP_LOCATION_LIST = $x("//div[@class='list visible']/div[@class='list-item'][1]");
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

        return $(By.xpath(search)).exists();
    }

    public static void performDiscovery(String discoveryName, String locationName, int port,
                                        String community, String ip) {
        LeftPanelPage.clickOnPanelSection("discovery");
        ADD_DISCOVERY_BUTTON.shouldBe(enabled).click();
        SNMP_DISCOVERY_BUTTON.should(exist).click();

        // Sometimes a small delay for the UI to fill in the default values. Make sure we wait on it
        PORT_INPUT.shouldHave(attribute("value", "161"));

        DISCOVERY_NAME_INPUT.shouldBe(editable).sendKeys(discoveryName);

        // When only 1 location exists, it is automatically selected and we don't need to add it
        if (!newDiscoveryCheckForLocation(locationName)) {
            // For the location selector to work, we need to click in it first as this shows the dropdown
            // selections. Even still, it sometimes doesn't show up even after 20s, so clear it and try again
            int tries = 5;
            while ((tries > 0) && (! POPUP_LOCATION_LIST.exists())) {
                LOCATION_DROPDOWN_ICON.shouldBe(enabled).click();
                Selenide.sleep(5000);
                --tries;
            }
            POPUP_LOCATION_LIST.should(exist);

            String specificListItemSearch = "//div[@label='" + locationName + "']";
            SelenideElement locationPopupListItem = $(By.xpath(specificListItemSearch));
            locationPopupListItem.should(exist, Duration.ofSeconds(20)).shouldBe(enabled).click();
        }

        IP_RANGE_INPUT.shouldBe(enabled).sendKeys(ip);

        // Clear seems to be buggy with the current drivers. Using backspaces instead
        PORT_INPUT.shouldBe(enabled).sendKeys("\b\b\b");
        PORT_INPUT.sendKeys(Integer.toString(port));

        COMMUNITY_STRING_INPUT.shouldBe(enabled).click();
        COMMUNITY_STRING_INPUT.sendKeys("\b\b\b\b\b\b");
        COMMUNITY_STRING_INPUT.sendKeys(community);

        SAVE_DISCOVERY_BUTTON.shouldBe(enabled).click();
        VIEW_DETECTED_NODES_BUTTON.should(exist).shouldBe(enabled).click();
    }

    public static void deleteAllDiscoveries() {
        LeftPanelPage.clickOnPanelSection("discovery");

        int discoveries = ACTIVE_DISCOVERY_CARDS.size();
        while (discoveries > 0) {
            deleteIndexedDiscovery(discoveries);

            // Wait to make sure the old element disappears
            $x("//div[@class='card-my-discoveries']/div[@class='list']/div[" + discoveries + "]").shouldNot(exist);

            --discoveries;
        }
    }

    private static void deleteIndexedDiscovery(int index) {
        SelenideElement discoveryCard = $x("//div[@class='card-my-discoveries']/div[@class='list']/div[" + index + "]/div");
        discoveryCard.isEnabled();
        discoveryCard.click();

        // If the delete button is clicked too fast (before all the fields are populated), it doesn't work
        Selenide.sleep(3000);
        DISCOVERY_DELETE_BUTTON.should(exist).click();
        DELETE_DISCOVERY_CONFIRM_YES.should(exist).click();
    }
}
