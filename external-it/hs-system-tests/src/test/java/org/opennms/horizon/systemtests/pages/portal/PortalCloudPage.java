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


package org.opennms.horizon.systemtests.pages.portal;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import java.time.Duration;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

public class PortalCloudPage {

    private static final SelenideElement profileIcon = $("button.profile-circle");
    private static final SelenideElement addInstanceBtn = $("#cloud-add-instance-button");
    private static final SelenideElement instanceTable = $("table.condensed  tbody");
    private static final SelenideElement logInBtn = $("#cloud-instances-row-login-0");
    private static final SelenideElement detailsBtn = $("#cloud-instances-row-details-0");
    private static final SelenideElement searchInstanceInp = $("#cloud-instances-search");
    private static final SelenideElement shadedBackground = $("div.backdrop");
    private static final SelenideElement spinner = $(".spinner");

    public static void verifyThatUserLoggedIn() {
        profileIcon.shouldBe(Condition.visible, Duration.ofMinutes(1));
    }

    public static void clickAddInstance() {
        addInstanceBtn.shouldBe(Condition.enabled, Duration.ofSeconds(20)).click();
    }

    public static void instantShouldBePresentedInTable(String instanceName) {
        instanceTable.find(byText(instanceName)).shouldBe(Condition.visible);
    }

    public static void clickLogInForFirstInstance() {
        logInBtn.shouldBe(Condition.enabled).click();
    }


    public static void clickDetailsForFirstInstance() {
        detailsBtn.shouldBe(Condition.enabled).click();
    }

    public static void setFilter(String pattern) {
        searchInstanceInp.shouldBe(Condition.enabled).setValue(pattern);
        spinner.shouldBe(Condition.appear).shouldBe(Condition.disappear, Duration.ofMinutes(1));
    }

    public static void mainPageIsNotCoveredByPopups() {
        shadedBackground.shouldBe(Condition.hidden);
    }
}
