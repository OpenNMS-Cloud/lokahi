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

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.junit.Assert;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selectors.*;

public class LocationsPage {

    private static final ElementsCollection locationNamesList = $$("[data-test='card']");
    private static final ElementsCollection minionsList =  $$("[class='minions-card-wrapper']");


    public static void clickOnLocation(String locationName) {
        locationNamesList.find(text(locationName)).shouldBe(visible, enabled).click();
    }

    public static void checkLocationExists(String locationName) {
        locationNamesList.find(text(locationName)).shouldBe(visible, enabled);
    }

    public static void checkMinionExists(String minionId) {
        minionsList.find(text(minionId)).shouldBe(visible, enabled);
    }

    public static void clickOnMinionDeleteButton(String minionId) {
        minionsList.find(text(minionId)).$("[data-test='context-menu']").shouldBe(visible, enabled).click();
        minionsList.find(text(minionId)).$(withText("Delete")).shouldBe(visible, enabled).click();
    }

    public static void checkMinionDoesntExist(String minionId) {
        Selenide.refresh();
        Assert.assertFalse("Expected to see no minion with ID: " + minionId, minionsList.find(text(minionId)).isDisplayed());
    }
}
