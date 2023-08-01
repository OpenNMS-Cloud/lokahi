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

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class InventoryPage {
    public enum Status {
        UP,
        DOWN
    }

    private static final SelenideElement firstSnmpInterfaceInTable = $(By.xpath("//table[@aria-label='SNMP Interfaces Table']/tbody/tr[1]"));

    private static final ElementsCollection nodesList = $$("[class='cards']");


    public static boolean checkIfMonitoredNodeExist(String nodeSysName) {
        return nodesList.findBy(Condition.text(nodeSysName)).isDisplayed();
    }

    public static void verifyNodeStatus(Status status, String nodeManagementIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        // The inventory page doesn't refresh on its own. Need to periodically check and force a refresh
        String itemStatusSearch = "//ul[@class='cards']/li[.//li[@data-test='management-ip']/span/text()='" + nodeManagementIp + "']//div[@title='Status']//span[text()='" + status + "']";
        SelenideElement statusCheck = $(By.xpath(itemStatusSearch));
        int iterations = 20;
        while (!statusCheck.exists() && iterations > 0) {
            --iterations;
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // Ignore and busy-loop it
            }

            Selenide.refresh();

            LeftPanelPage.clickOnPanelSection("inventory");
        }
        statusCheck.should(exist);
    }

    public static void verifyNodeDoesNotExist(String nodeManagementIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        // The inventory page doesn't refresh on its own. Need to periodically check and force a refresh
        String itemStatusSearch = "//ul[@class='cards']/li[.//li[@data-test='management-ip']/span/text()='" + nodeManagementIp + "']";

        SelenideElement nodeCheck = $(By.xpath(itemStatusSearch));
        nodeCheck.should(Condition.not(exist));
    }

    public static void deleteNode(String nodeManagementIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        String deleteNodeButtonSearch = "//ul[@class='cards']/li[.//li[@data-test='management-ip']/span/text()='" + nodeManagementIp + "']//li[@data-test='delete']";
        $(By.xpath(deleteNodeButtonSearch)).should(exist).shouldBe(enabled).click();

        String deleteConfirm = "//button[@data-testid='save-btn']";
        $(By.xpath(deleteConfirm)).should(exist).shouldBe(enabled).click();
    }

    public static void verifyNodeContainsSnmpInterfaces(String nodeIp) {
        LeftPanelPage.clickOnPanelSection("inventory");

        String nodeEventsAlarmsButton = "//ul[@class='cards']/li[.//li[@data-test='management-ip']/span/text()='" + nodeIp + "']//li[@data-test='warning']";
        $(By.xpath(nodeEventsAlarmsButton)).should(exist).shouldBe(enabled).click();

        firstSnmpInterfaceInTable.should(exist);
    }

}
