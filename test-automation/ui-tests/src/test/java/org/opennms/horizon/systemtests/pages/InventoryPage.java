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

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class InventoryPage {
    public enum Status {
        UP,
        DOWN
    }

    private static final SelenideElement monitoredNodesTab = $$("[data-ref-id='feather-tab']").get(0);
    private static final ElementsCollection nodesList = $$("[class='cards']");

    public static void clickOnMonitoredNodesTab() {
        monitoredNodesTab.shouldBe(Condition.visible, enabled).click();
    }

    public static boolean checkIfMonitoredNodeExist(String nodeSysName) {
        return nodesList.findBy(Condition.text(nodeSysName)).isDisplayed();
    }

    public static void verifyNodeStatus(Status status, String nodename) {
        // The inventory page doesn't refresh on its own. Need to periodically check and force a refresh
        String itemStatusSearch = "//ul[@class='cards']/li[//li[@data-test='management-ip']/span/text()='" + nodename + "']//div[@title='Status']//span[text()='" + status + "']";
        SelenideElement statusCheck = $(By.xpath(itemStatusSearch));
        int iterations = 10;
        while (!statusCheck.exists() && iterations > 0) {
            --iterations;
            Selenide.refresh();
            LeftPanelPage.clickOnPanelSection("inventory");

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignore and busy-loop it
            }
        }
        statusCheck.should(exist);
    }

    public static void deleteNode(String node) {
        String deleteNodeButtonSearch = "//ul[@class='cards']/li[//li[@data-test='management-ip']/span/text()='" + node + "']//li[@data-test='delete']";
        $(By.xpath(deleteNodeButtonSearch)).should(exist).shouldBe(enabled).click();

        String deleteConfirm = "//button[@data-testid='save-btn']";
        $(By.xpath(deleteConfirm)).should(exist).shouldBe(enabled).click();
    }

}
