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
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class InventoryPage {
    public enum Status {
        UP,
        DOWN
    }

    private static final SelenideElement firstSnmpInterfaceInTable = $(By.xpath("//table[@data-test='SNMPInterfacesTable']/tbody/tr[1]"));

    private static final SelenideElement firstNodeDeleteButton = $(By.xpath("//li[@data-test='MONITORED'][1]//li[@data-test='delete']"));
    private static final SelenideElement firstNodeManagementIP = $(By.xpath("//li[@data-test='MONITORED'][1]//li[@data-test='management-ip']/span"));
    private static final SelenideElement deleteConfirmButton = $(By.xpath("//button[@data-testid='save-btn']"));
    private static final ElementsCollection monitoredInventoryCards = $$(By.xpath("//li[@data-test='MONITORED']"));

    public static void verifyNodeStatus(Status status, String nodeManagementIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        // The inventory page doesn't refresh on its own. Need to periodically check and force a refresh
        String itemStatusSearch = "//li[@data-test='MONITORED' and .//li[@data-test='management-ip']/span/text()='" + nodeManagementIp + "']//span[text()='" + status + "']";
        SelenideElement statusCheck = $(By.xpath(itemStatusSearch));
        int iterations = 20;
        boolean exists = false;

        while (!exists && iterations > 0) {
            try {
                statusCheck.should(exist, Duration.ofSeconds(4));
                iterations = 0;
            } catch (com.codeborne.selenide.ex.ElementNotFound e) {
                --iterations;
                Selenide.refresh();
            }
        }

        statusCheck.should(exist);
    }

    public static void verifyNodeDoesNotExist(String nodeManagementIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        // The inventory page doesn't refresh on its own. Need to periodically check and force a refresh
        String itemStatusSearch = "//li[@data-test='MONITORED' and .//li[@data-test='management-ip']/span/text()='" + nodeManagementIp + "']";

        SelenideElement nodeCheck = $(By.xpath(itemStatusSearch));
        nodeCheck.should(Condition.not(exist));
    }

    public static void deleteNode(String nodeManagementIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        String deleteNodeButtonSearch = "//li[@data-test='MONITORED' and .//li[@data-test='management-ip']/span/text()='" + nodeManagementIp + "']//li[@data-test='delete']";
        $(By.xpath(deleteNodeButtonSearch)).should(exist).shouldBe(enabled).click();

        deleteConfirmButton.should(exist).shouldBe(enabled).click();
    }

    public static void deleteAllNodes() {
        LeftPanelPage.clickOnPanelSection("inventory");

        int nodeCount = monitoredInventoryCards.size();
        while (nodeCount > 0) {
            --nodeCount;
            // Get specific reference with IP for first card, delete, then ensure it is gone before continuing
            String mgmtIp = firstNodeManagementIP.should(exist).getText();
            SelenideElement searchMgmt = $(By.xpath("//li[@data-test='MONITORED'][1]//li[@data-test='management-ip']/span[text()='" + mgmtIp + "']"));

            firstNodeDeleteButton.shouldBe(enabled).hover().click();
            deleteConfirmButton.should(exist).shouldBe(enabled).click();

            searchMgmt.should(not(exist));
        }
    }

    public static void verifyNodeContainsSnmpInterfaces(String nodeIp) {
        String nodeEventsAlarmsButton = "//li[@data-test='MONITORED' and .//li[@data-test='management-ip']/span/text()='" + nodeIp + "']//li[@data-test='warning']";
        $(By.xpath(nodeEventsAlarmsButton)).should(exist).shouldBe(enabled).click();

        firstSnmpInterfaceInTable.should(exist);
    }
}
