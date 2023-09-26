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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.commands.IsDisplayed;

import java.time.Duration;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$x;

public class HomePage {
    private static final SelenideElement flowsCard = $x("//div[ @class='card' and .//div[text()='Top 10 Applications'] ]");
    private static final SelenideElement pieChart = $("canvas#pieChartApplications");
    private static final SelenideElement emptyFlowCard = $x("//div[ @class='card' and .//div[text()='Top 10 Applications'] ]//div[@class='empty']");
    private static final SelenideElement flowsCardSubtitle = $x("//div[ @class='card' and .//div[text()='Top 10 Applications']]//div[@class='subtitle']");
    private static final SelenideElement flowsLink = $x("//div[text()='Flows']");

    public static void verifyNoDataTop10ApplicationsState(boolean isVisible) {
        emptyFlowCard.shouldBe(isVisible ? visible : hidden);
    }

    public static void verifyTop10ApplicationsSubtitle(String subtitleTxt) {
        flowsCardSubtitle.shouldBe(text(subtitleTxt));
    }

    public static void clickOnFlowsLink() {
        flowsLink.shouldBe(enabled).click();
        FlowsPage.waitPageLoaded();
    }

    public static void waitOnTop10Applications() {
        RefreshMonitor.waitForElement(pieChart, visible, 120, true);
        flowsCard.find("canvas#pieChartApplications").shouldBe(visible);
    }
}
