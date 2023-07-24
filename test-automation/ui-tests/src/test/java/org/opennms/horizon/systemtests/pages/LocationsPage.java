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

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.junit.Assert;
import org.opennms.horizon.systemtests.utils.MinionStarter;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selectors.*;

public class LocationsPage {

    private static final ElementsCollection locationNamesList = $$("[data-test='card']");
    private static final SelenideElement addLocationButton = $("[data-test='add-location-btn']");
    private static final SelenideElement locationNameInputField = $$("[data-test='input-name']").get(1);
    private static final SelenideElement addLocationSaveButton = $("[data-test='save-button']");
    private static final SelenideElement downloadCertificateButton = $("[data-test='download-btn']");
    private static final SelenideElement dockerRunCLTextField = $(withText("GRPC_CLIENT_KEYSTORE_PASSWORD"));
    private static final ElementsCollection minionsList =  $$("[class='minions-card-wrapper']");
    private static final SelenideElement confirmDeleteButton = $(By.xpath("//button[@data-test='delete-btn']"));

    public static void clickOnLocation(String locationName) {
        locationNamesList.find(text(locationName)).shouldBe(visible, enabled).click();
    }

    public static void checkLocationExists(String locationName) {
        locationNamesList.find(text(locationName)).isDisplayed();
    }

    public static void checkMinionExists(String minionId) {
        minionsList.find(text(minionId)).shouldBe(visible, enabled);
    }

    public static void deleteMinion(String minionId) {
        minionsList.find(text(minionId)).$("[data-test='context-menu']").shouldBe(visible, enabled).click();
        minionsList.find(text(minionId)).$(withText("Delete")).shouldBe(visible, enabled).click();
        confirmDeleteButton.should(exist).shouldBe(visible, enabled).click();
    }

    public static void checkMinionDoesntExist(String minionId) {
        Selenide.refresh();
        Assert.assertFalse("Expected to see no minion with ID: " + minionId, minionsList.find(text(minionId)).isDisplayed());
    }

    public static void addNewLocation(String locationName) {
        addLocationButton.shouldBe(visible, enabled).click();
        locationNameInputField.shouldBe(visible).sendKeys(locationName);
        addLocationSaveButton.shouldBe(visible, enabled).click();
    }

    public static void downloadCertificateAndStartMinion(String minionId, String dockerComposeFile) {
        MinionStarter.downloadCertificateAndStartMinion(minionId, dockerComposeFile, downloadCertificateButton, dockerRunCLTextField);
    }

    public static void selectConfirmDeleteButton() {
        confirmDeleteButton.shouldBe(enabled).click();
    }

    public static boolean selectLocationMenuItem(String menuItemName, String locationName) {
        String specificLocationSearch = "//div[@class='locations-card' or @class='locations-card selected'][.//div[@class='name']//span/text()='" + locationName + "']//button[@data-test='more-options-btn']";
        SelenideElement locationMenu = $(By.xpath(specificLocationSearch));
        if (locationMenu.exists()) {
            // Location exists, time to hit edit
            locationMenu.click();
            SelenideElement menuItem = $(By.xpath("//div[@data-test='context-menu']/div[@class='feather-menu-dropdown']//a[span/text()='" + menuItemName + "'][1]"));
            menuItem.should(exist).click();
            return true;
        }
        return false;
    }

    public static boolean selectLocationEditMenu(String locationName) {
        return selectLocationMenuItem("Edit", locationName);
    }

    public static boolean selectLocationDeleteMenu(String locationName) {
        return selectLocationMenuItem("Delete", locationName);
    }

    public static boolean selectLocation(String locationName) {
        String specificLocationSearch = "//div[@class='locations-card' or @class='locations-card selected']//div[@class='name']//span[text()='" + locationName + "']";
        SelenideElement location = $(By.xpath(specificLocationSearch));
        if (location.exists()) {
            // Location exists, time to hit edit
            location.click();
            return true;
        }
        return false;
    }
}
