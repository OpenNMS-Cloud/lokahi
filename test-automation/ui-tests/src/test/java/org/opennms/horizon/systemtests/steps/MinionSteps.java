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
import org.opennms.horizon.systemtests.pages.LeftPanelPage;
import org.opennms.horizon.systemtests.pages.LocationsPage;
import org.openqa.selenium.By;
import org.testcontainers.containers.Network;
import testcontainers.MinionContainer;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class MinionSteps {
    private static final ElementsCollection locationMenus = $$(By.xpath("//div[@class='locations-card']//button[@data-test='more-options-btn']"));
    private static final SelenideElement firstMenu = $(By.xpath("//div[@class='locations-card']//button[@data-test='more-options-btn'][1]"));

    private static Map<String, MinionContainer> minions = new HashMap<>();


    public static void waitForMinionUp(String minionName) {
        SelenideElement minionStatus = $(By.xpath("//ul[@class='minions-list']/li[.//div[@data-test='header-name']/text()='" + minionName.toUpperCase() + "']//div[@class='status']//span[text()='UP']"));

        // Make sure we're on the locations page with the right location selected
        LeftPanelPage.clickOnPanelSection("locations");
        LocationsPage.selectLocation(LocationSteps.getLocationName());

        // The UI can take a very long time to reflect minion status updates. Might need to force refreshed to speed this up.
        minionStatus.should(exist, Duration.ofSeconds(120));
    }

    public static void addMinionForLocation(String minionName) {
        // Ensure the location exists
        LocationSteps.addOrEditLocation();
        MinionContainer minion = LocationSteps.addMinionFromLocationPane(minionName);

        minions.put(LocationSteps.getLocationName(), minion);
        waitForMinionUp(minionName);
    }

    public static MinionContainer startMinion(File bundle, String pwd, String minionId) {
        Network network = SetupSteps.getCommonNetwork();
        MinionContainer minion = new MinionContainer(minionId, "minion-" + minionId.toLowerCase(), network,
            bundle, pwd);

        minion.start();
        return minion;
    }
}
