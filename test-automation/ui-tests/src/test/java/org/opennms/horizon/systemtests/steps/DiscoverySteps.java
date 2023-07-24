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

import com.codeborne.selenide.SelenideElement;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.systemtests.pages.LeftPanelPage;
import org.opennms.horizon.systemtests.pages.InventoryPage;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.Assert.assertNotNull;

public class DiscoverySteps {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoverySteps.class);

    private static final SelenideElement ADD_DISCOVERY_BUTTON = $(By.xpath("//div[@class='add-btn']/button"));
    private static final SelenideElement SAVE_DISCOVERY_BUTTON = $(By.xpath("//button[text()='Save discovery']"));
    private static final SelenideElement VIEW_DETECTED_NODES_BUTTON = $(By.xpath("//button[text()=' View Detected Nodes']"));
    private static final SelenideElement SNMP_DISCOVERY_RADIO_BUTTON = $(By.xpath("//div[@class='feather-radio'][./span/text()='ICMP/SNMP']"));
    private static final SelenideElement DISCOVERY_NAME_INPUT = $(By.xpath("//div[contains(@class, 'name-input')]//input"));
    private static final SelenideElement LOCATION_NAME_INPUT = $(By.xpath("//input[@placeholder='Search Locations']"));
    private static final SelenideElement IP_RANGE_INPUT = $(By.xpath("//div[@class='ip-input']//div[@class='content-editable']"));
    private static final SelenideElement COMMUNITY_STRING_INPUT = $(By.xpath("//div[@class='community-input']//div[@class='content-editable']"));
    private static final SelenideElement PORT_INPUT = $(By.xpath("//div[@class='udp-port-input']//div[@class='content-editable']"));
    private static final String SNMP_NODE_IMAGE_NAME = "polinux/snmpd:alpine";

    private static Map<String, GenericContainer> nodes = new HashMap<>();

    @Given("Start snmp node {string}")
    public void startNode(String nodeName) throws IOException {
        LOG.info("Starting node " + nodeName);

        GenericContainer<?> node = new GenericContainer<>(DockerImageName.parse(SNMP_NODE_IMAGE_NAME))
            .withNetworkAliases("nodes")
            .withNetwork(SetupSteps.getCommonNetwork())
            .withCopyFileToContainer(MountableFile.forClasspathResource("snmpd/snmpd.conf"), "/etc/snmp/snmpd.conf")
            .withLabel("label", nodeName);

        node.waitingFor(Wait.forLogMessage(".*SNMPD Daemon started.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
        node.start();
        nodes.put(nodeName, node);
    }

    @When("Discovery {string} for node {string} is created to discover by IP")
    public void discoveryForNodeIsCreatedToDiscoverByIP(String discoveryName, String nodeName) {
        discoverSingleNode(discoveryName, nodeName, LocationSteps.getLocationName(), 161, "public");
    }

    private static String getContainerIP(GenericContainer<?> container) {
        NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();
        Map<String, ContainerNetwork> networksMap = networkSettings.getNetworks();
        return networksMap.values().iterator().next().getIpAddress();
    }

    public boolean newDiscoveryCheckForLocation(String locationName) {
        String search = "//div[@class='locations-select']//span[text()=' " + locationName + "']";
        return $(By.xpath(search)).exists();
    }

    private void discoverSingleNode(String discoveryName, String nodeName, String locationName, int port, String community) {
        String ipaddress = getIpaddress(nodeName);

        LeftPanelPage.clickOnPanelSection("discovery");
        ADD_DISCOVERY_BUTTON.shouldBe(enabled).click();
        SNMP_DISCOVERY_RADIO_BUTTON.shouldBe(enabled).click();
        DISCOVERY_NAME_INPUT.shouldBe(enabled).sendKeys(discoveryName);

        if (!newDiscoveryCheckForLocation(locationName)) {
            // When only 1 location exists, it is automatically selected
            LOCATION_NAME_INPUT.shouldBe(enabled).sendKeys(locationName);
            LOCATION_NAME_INPUT.sendKeys("\n");

            String specificListItemSearch = "//ul[@aria-label='Select a location']/li[//span/text()=' " + locationName + "']";
            SelenideElement locationPopupListItem = $(By.xpath(specificListItemSearch));
            locationPopupListItem.should(exist, Duration.ofSeconds(20)).shouldBe(enabled).click();
        }

        IP_RANGE_INPUT.shouldBe(enabled).sendKeys(ipaddress);

        PORT_INPUT.shouldBe(enabled).clear();
        PORT_INPUT.sendKeys(Integer.toString(port));
        COMMUNITY_STRING_INPUT.shouldBe(enabled).clear();
        COMMUNITY_STRING_INPUT.sendKeys(community);

        SAVE_DISCOVERY_BUTTON.shouldBe(enabled).click();
        VIEW_DETECTED_NODES_BUTTON.should(exist).shouldBe(enabled).click();
    }

    public static String getIpaddress(String nodeName) {
        String ipaddress;
        GenericContainer<?> node = nodes.get(nodeName);
        assertNotNull("Cannot find node with name " + nodeName, node);

        return getContainerIP(node);
    }

    @Then("Status of {string} should be {string}")
    public void statusOfShouldBe(String nodeName, String requestedStatus) {
        InventoryPage.Status status;
        if (requestedStatus.toLowerCase().equals("up")) {
            status = InventoryPage.Status.UP;
        } else {
            status = InventoryPage.Status.DOWN;
        }
        String ipaddress = getContainerIP(nodes.get(nodeName));

        InventoryPage.verifyNodeStatus(status, ipaddress);
    }

    @Then("delete node {string}")
    public void deleteNode(String nodeName) {
        GenericContainer<?> node = nodes.get(nodeName);
        assertNotNull("Cannot find node to delete with name " + nodeName, node);

        String ipaddress = getContainerIP(node);

        LeftPanelPage.clickOnPanelSection("inventory");
        InventoryPage.deleteNode(ipaddress);
    }
}
