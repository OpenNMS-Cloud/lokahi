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

package org.opennms.horizon.systemtests.steps;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.opennms.horizon.systemtests.pages.LeftPanelPage;
import org.opennms.horizon.systemtests.pages.LocationsPage;
import org.opennms.horizon.systemtests.utils.FileDownloadManager;
import org.openqa.selenium.By;
import testcontainers.MinionContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.junit.Assert.fail;

public class LocationSteps {
    public static final String DEFAULT_LOCATION_NAME = "default";
    private static final ElementsCollection locationMenus = $$(By.xpath("//div[@class='locations-card']//button[@data-test='more-options-btn']"));
    private static final SelenideElement firstMenu = $(By.xpath("//div[@class='locations-card']//button[@data-test='more-options-btn'][1]"));
    private static final SelenideElement addLocationButton = $(By.xpath("//button[@data-test='add-location-btn']"));
    private static final SelenideElement locationNameInput = $(By.xpath("//input[@data-test='input-name']"));
    private static final SelenideElement saveLocationButton = $(By.xpath("//button[@data-test='save-button']"));
    private static final SelenideElement downloadCertButton = $(By.xpath("//button[@data-test='download-btn']"));
    private static final SelenideElement dockerCmd = $(By.xpath("//div[@class='instructions']/pre"));

    private static String locationName = "not_default";



    public static void addOrEditLocation() {
        LeftPanelPage.clickOnPanelSection("locations");

        if (!LocationsPage.selectLocationEditMenu(locationName)) {
            // Add the location
            addLocationButton.shouldBe(enabled).click();
            locationNameInput.shouldBe(editable).sendKeys(locationName);
            saveLocationButton.shouldBe(enabled).click();
        }
    }

    public static MinionContainer addMinionFromLocationPane(String minionName) {
        File bundle = null;
        try {
            bundle = FileDownloadManager.downloadCertificate(downloadCertButton);
            if (bundle != null) {
                // Parse out the pwd for the bundle
                String dockerText = dockerCmd.shouldBe(visible).getText();
                Pattern pattern = Pattern.compile("GRPC_CLIENT_KEYSTORE_PASSWORD='([a-z,0-9,-]*)'");
                Matcher matcher = pattern.matcher(dockerText);

                if (matcher.find()) {
                    MinionContainer minion = MinionSteps.startMinion(bundle, matcher.group(1), minionName, LocationSteps.getLocationName());
                    // Minion startup and connect is slow - need a specific timeout here
                    return minion;
                }
                fail("Unable to parse p12 password from docker string: " + dockerText);
            } else {
                fail("Failure downloading file");
            }
        } catch (IOException e) {
            fail("File not found after attempting to download p12 bundle");
        }
        fail("Failure downloading p12 bundle file");
        return null;
    }

    public static String getLocationName() {
        return locationName;
    }

    public static boolean isLocationNameDefault() {
        return (DEFAULT_LOCATION_NAME.equals(locationName));
    }

    @Then("delete all locations")
    public void deleteAllLocations() {
        LeftPanelPage.clickOnPanelSection("locations");
        while (true) {
            if (!firstMenu.exists()) {
                // No locations left to delete
                return;
            }
            int numLocations = locationMenus.size();
            firstMenu.click();

            SelenideElement deleteItem = $(By.xpath("//div[@data-test='context-menu']/div[@class='feather-menu-dropdown']//a[span/text()='Delete'][1]"));
            deleteItem.should(exist).click();
            LocationsPage.selectConfirmDeleteButton();
            locationMenus.shouldHave(size(numLocations - 1));
        }
    }

    @Then("delete used locations")
    public void deleteUsedLocations() {
        LeftPanelPage.clickOnPanelSection("locations");
        LocationsPage.selectLocationDeleteMenu(locationName);
        LocationsPage.selectConfirmDeleteButton();
    }

    @Given("Location name {string}")
    public void locationName(String newName) {
        locationName = newName;
    }
}
