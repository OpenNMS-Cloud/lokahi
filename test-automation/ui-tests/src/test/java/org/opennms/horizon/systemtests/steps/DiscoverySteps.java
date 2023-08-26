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

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.systemtests.pages.DiscoveryPage;
import org.opennms.horizon.systemtests.pages.InventoryPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;

import static com.codeborne.selenide.Selenide.$;
import static org.junit.Assert.*;

public class DiscoverySteps {
    private static final Logger LOG = LoggerFactory.getLogger(DiscoverySteps.class);

    private static final String SNMP_NODE_IMAGE_NAME = "opennms/lokahi-snmpd-udpgen:latest";

    private static Map<String, GenericContainer> nodes = new HashMap<>();

    public static void cleanup() {
        InventoryPage.deleteAllNodes();

        for (GenericContainer nodeContainer : nodes.values()) {
            nodeContainer.stop();
        }
        nodes.clear();

        DiscoveryPage.deleteAllDiscoveries();
    }

    @Given("Start snmp node {string}")
    public void startNode(String nodeName) throws IOException {
        startSnmpNode(nodeName, "");
    }

    private static void startSnmpNode(String nodeName, String snmpConfigFile) {
        LOG.info("Starting node " + nodeName);

        GenericContainer<?> node = new GenericContainer<>(DockerImageName.parse(SNMP_NODE_IMAGE_NAME))
            .withNetworkAliases("nodes")
            .withNetwork(SetupSteps.getCommonNetwork())
            .withLabel("label", nodeName);

        if (snmpConfigFile != null && !snmpConfigFile.isBlank()) {
            node.withCopyFileToContainer(MountableFile.forClasspathResource(snmpConfigFile), "/etc/snmp/snmpd.conf");
        }

        node.waitingFor(Wait.forLogMessage(".*NET-SNMP version.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
        node.start();
        nodes.put(nodeName, node);
    }

    @Given("Start snmp node {string} with additional snmpd configuration")
    public void startSnmpNodeWithAdditionalSnmpdConfiguration(String nodeName) {
        startSnmpNode(nodeName, "snmpd/snmpd.conf");
    }

    @When("Discovery {string} for node {string} is created to discover by IP")
    public void discoveryForNodeIsCreatedToDiscoverByIP(String discoveryName, String nodeName) {
        discoverSingleNode(discoveryName, nodeName, LocationSteps.getLocationName(), 161, "public");
    }

    @Then("Discover {string} for node {string}, port {int}, community {string}")
    public void discoverForNodePortCommunity(String discoveryName, String nodeName, int port, String community) {
        discoverSingleNode(discoveryName, nodeName, LocationSteps.getLocationName(), port, community);
    }

    private void discoverSingleNode(String discoveryName, String nodeName, String locationName, int port, String community) {
        String ipaddress = getIpaddress(nodeName);
        DiscoveryPage.performDiscovery(discoveryName, locationName, port, community, ipaddress);
    }

    @Then("Subnet mask discovery {string} for nodes with mask {int}, port {int}, community {string}")
    public void subnetDiscoveryWithMask(String discoverName, int mask, int port, String community) {
        GenericContainer node = nodes.values().iterator().next();
        if (node == null) {
            throw new RuntimeException("No nodes for subnet discovery");
        }
        if (mask < 24) {
            throw new RuntimeException("Tests only support discovery with masks at least 24");
        }

        String ipaddress = getContainerIP(node) + "/" + mask;
        DiscoveryPage.performDiscovery(discoverName, LocationSteps.getLocationName(), port, community, ipaddress);
    }

    @Then("Subnet range discovery {string} for nodes with port {int}, community {string}")
    public void subnetDiscoveryWithIPRange(String discoverName, int port, String community) {
        Collection<GenericContainer> nodeContainers = nodes.values();

        if (nodeContainers.size() < 2) {
            fail("Need at least 2 nodes for an IP range");
        }
        String ranges = calculateIPRanges(nodeContainers);

        DiscoveryPage.performDiscovery(discoverName, LocationSteps.getLocationName(), port, community, ranges);
    }

    @Then("IP list discovery {string} for nodes {string} with port {int}, community {string}")
    public void ipListDiscoveryForNodesWithMaskPortCommunity(String discoveryName, String nodeNames, int port, String community) {
        String ipList = getContainerIPs(nodeNames);

        DiscoveryPage.performDiscovery(discoveryName, LocationSteps.getLocationName(), port, community, ipList);

    }

    private String getContainerIPs(String nodeNames) {
        StringBuilder ips = new StringBuilder();
        String[] nodeNameArray = nodeNames.split(",");
        for (String nodeNameFromArray : nodeNameArray) {
            String nodeName = nodeNameFromArray.trim();
            GenericContainer container = nodes.get(nodeName);
            assertNotNull("Must have a container for node " + nodeName, container);
            if (!ips.isEmpty()) {
                ips.append(", ");
            }
            ips.append(getContainerIP(container));
        }
        return ips.toString();
    }

    private String calculateIPRanges(Collection<GenericContainer> nodes) {
        int firstAddr = 0;
        String firstAddrString = "";
        int secondAddr = 0;
        String secondAddrString = "";

        Iterator<GenericContainer> nodeIterator = nodes.iterator();
        if (!nodeIterator.hasNext()) {
            throw new RuntimeException("Cannot calculate IP range when there are no nodes");
        }
        GenericContainer node = nodeIterator.next();

        firstAddrString = getContainerIP(node);
        firstAddr = convertStringIPToInt(firstAddrString);

        if (!nodeIterator.hasNext()) {
            throw new RuntimeException("Cannot calculate IP range where there is only 1 node");
        }
        node = nodeIterator.next();
        secondAddrString = getContainerIP(node);
        secondAddr = convertStringIPToInt(secondAddrString);

        if (firstAddr > secondAddr) {
            String ipStr = secondAddrString;
            secondAddrString = firstAddrString;
            firstAddrString = ipStr;

            int ipInt = secondAddr;
            secondAddr = firstAddr;
            firstAddr = ipInt;
        }

        while (nodeIterator.hasNext()) {
            node = nodeIterator.next();
            String ipStr = getContainerIP(node);
            int ipInt = convertStringIPToInt(ipStr);
            if (ipInt < firstAddr) {
                firstAddrString = ipStr;
                firstAddr = ipInt;
            } else if (ipInt > secondAddr) {
                secondAddrString = ipStr;
                secondAddr = ipInt;
            }
        }
        return firstAddrString + "-" + secondAddrString;
    }

    private int convertStringIPToInt(String stringAddr) {
        try {
            InetAddress i = InetAddress.getByName(stringAddr);
            return ByteBuffer.wrap(i.getAddress()).getInt();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Test error converting string ip '" + stringAddr + "' to properly formatted inet addr");
        }
    }

    private static String getContainerIP(GenericContainer<?> container) {
        NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();
        Map<String, ContainerNetwork> networksMap = networkSettings.getNetworks();
        return networksMap.values().iterator().next().getIpAddress();
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
        GenericContainer<?> nodeContainer = nodes.get(nodeName);
        assertNotNull("Cannot find node to delete with name " + nodeName, nodeContainer);

        InventoryPage.deleteNode(getContainerIP(nodeContainer));
    }

    @Then("check snmp interfaces exist for node {string}")
    public void checkSnmpInterfacesExistForNode(String nodeName) {
        String ipaddress = getContainerIP(nodes.get(nodeName));

        InventoryPage.verifyNodeContainsSnmpInterfaces(ipaddress);
    }

    @Then("Node {string} should not exist")
    public void nodeShouldNotExist(String nodeName) {
        String ipaddress = getContainerIP(nodes.get(nodeName));

        InventoryPage.verifyNodeDoesNotExist(ipaddress);
    }
}
