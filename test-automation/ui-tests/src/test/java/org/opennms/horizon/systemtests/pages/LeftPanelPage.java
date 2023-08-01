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

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import java.time.Duration;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.actions;

public class LeftPanelPage {
    private static final SelenideElement leftPanel = $(".app-aside");
    private static final SelenideElement topRightBorder = $(By.xpath("//div[@class='right center-horiz'][1]"));
    private static final SelenideElement leftPanelWide = $(By.xpath("//div[@class='feather-app-rail']"));
    private static final SelenideElement leftPanelSmall = $(By.xpath("//div[@class='feather-app-rail narrow']"));

    public static void verifyLeftPanelIsDisplayed() {
        leftPanel.shouldBe(visible);
    }

    public static void clickOnPanelSection(String section) {
        // "home" is special and is the root url
        String url;
        if (section.equals("home")) {
            url = "[href='/']";
        } else {
            url = String.format("[href='/%s']", section);
        }
        $(url).shouldBe(enabled).hover().click();

        switch (section) {
            case "home" -> $(By.xpath("//div[@data-test='page-headline'][text()='Insights Dashboard']")).should(exist);
            case "locations" -> $(By.xpath("//div[@class='locations-card-items']")).should(exist);
            case "discovery" -> $(By.xpath("//div[@class='card-my-discoveries']")).should(exist);
            case "inventory" ->
                $(By.xpath("//div[@data-test='page-header'][contains(./text(), 'Network Inventory')]")).should(exist);
        }

        // Have to wait for the left border animation to fully stop before we can clear it.
        int width = 0;
        int previous = leftPanelWide.getSize().getWidth();
        while (width != previous) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            width = previous;
            previous = leftPanelWide.getSize().getWidth();
        }

        topRightBorder.hover().click();

        try {
            leftPanelSmall.should(exist, Duration.ofSeconds(4));
        } catch (com.codeborne.selenide.ex.ElementNotFound e) {

        }

        // The panel doesn't always want to resize properly. Occasionally it will ignore the clicks
        // on the main parts of the page and not resize. In these cases, the mouse needs to go back
        // into the panel and then back to the page again.
        int maxRetries = 5;
        while (! leftPanelSmall.exists() && maxRetries > 0) {
            --maxRetries;
            $(url).shouldBe(enabled).hover().click();
            topRightBorder.hover().click();
            try {
                leftPanelSmall.should(exist, Duration.ofSeconds(4));
            } catch (com.codeborne.selenide.ex.ElementNotFound e) {
            }
        }

        width = 0;
        previous = leftPanelSmall.getSize().getWidth();
        while (width != previous) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            width = previous;
            previous = leftPanelSmall.getSize().getWidth();
        }
    }
}
