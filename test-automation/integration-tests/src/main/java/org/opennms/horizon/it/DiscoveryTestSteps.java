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
package org.opennms.horizon.it;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import io.cucumber.java.en.Then;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.LocationData;
import org.opennms.horizon.it.gqlmodels.querywrappers.AddDiscoveryResult;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiscoveryTestSteps {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryTestSteps.class);

    private TestsExecutionHelper helper;
    private Map<String, GenericContainer> nodes = new ConcurrentHashMap<>();

    public DiscoveryTestSteps(TestsExecutionHelper helper) {
        this.helper = helper;
    }

//========================================
// Test Step Definitions
//----------------------------------------

    /**
     * This test step is to create a new discovery
     * @param name Name of the discovery
     * @param location Rather Default or behind the Minion
     * @param ipaddress Ip address or range of addresses separated by -
     * @param port port or array of ports separated by comma
     * @param communities Community string
     * @throws MalformedURLException
     */
    @Then("Add a new active discovery for the name {string} at location {string} with ip address {string} and port {int}, readCommunities {string}")
    public void addANewActiveDiscovery(String name, String location, String ipaddress, int port, String communities) throws MalformedURLException {
        LOG.info("Add a new discovery query execution steps");

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));
        String query = String.format(GQLQueryConstants.ADD_DISCOVERY_QUERY, name, locationId, ipaddress, communities, port);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
            200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + discoveryResult.getErrors(),
            ( discoveryResult.getErrors() == null ) || ( discoveryResult.getErrors().isEmpty() ));
   }

    @Then("Check the status of all {long} nodes with expected status {string}")
    public void checkTheStatusOfAllNodesTest(Long numNodes, String status) {
        LOG.info("Test to check the status of all nodes");

        try {
            Awaitility
                .await()
                .ignoreExceptions()
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> checkTheStatusOfAllNodes(numNodes.longValue(), status));
        } catch (Exception e) {
            LOG.info("Test check the status failed with the error: {}", e.getMessage());
            throw e;
        }
    }

    @Then("Node {string} is started")
    public void startNode(String nodeName) throws IOException {
        LOG.info("Starting node " + nodeName);

        GenericContainer<?> node = new GenericContainer<>(DockerImageName.parse(helper.getNodeImageNameSupplier().get()))
            .withNetworkAliases("nodes")
            .withNetwork(helper.getCommonNetworkSupplier().get())
            .withCopyFileToContainer(MountableFile.forClasspathResource("BOOT-INF/classes/snmpd/snmpd.conf"), "/etc/snmp/snmpd.conf")
            .withLabel("label", nodeName);

        node.waitingFor(Wait.forLogMessage(".*SNMPD Daemon started.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
        node.start();
        nodes.put(nodeName, node);
    }

    @Then("Node {string} is started with nondefault port and community")
    public void startNonDefaultNode(String nodeName) throws IOException {
        LOG.info("Starting non-default node " + nodeName);

        GenericContainer<?> node = new GenericContainer<>(DockerImageName.parse(helper.getNodeImageNameSupplier().get()))
            .withNetworkAliases("nodes")
            .withNetwork(helper.getCommonNetworkSupplier().get())
            .withCopyFileToContainer(MountableFile.forClasspathResource("BOOT-INF/classes/snmpd/snmpd_nondefaults.conf"), "/etc/snmp/snmpd.conf")
            .withLabel("label", nodeName);

        node.waitingFor(Wait.forLogMessage(".*SNMPD Daemon started.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
        node.start();
        nodes.put(nodeName, node);
    }

    @Then("Discover {string} for node {string}, location {string} is created to discover by IP")
    public void discoverSingleNodeWithDefaults(String discoveryName, String nodeName, String location) throws IOException {
        discoverSingleNode(discoveryName, nodeName, location, 161, "public");
    }

    @Then("Discover {string} for node {string}, location {string}, port {int}, community {string}")
    public void discoverSingleNode(String discoveryName, String nodeName, String location, int port, String community) throws MalformedURLException {
        GenericContainer<?> node = nodes.get(nodeName);
        if (node == null) {
            throw new RuntimeException("No node matching name " + nodeName);
        }

//        String ip = node.getContainerIpAddress();  -> Don't use this API - it always just returns "localhost"
        String ipaddress = getContainerIP(node);
        LOG.info("IP:" + ipaddress);

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        String query = String.format(GQLQueryConstants.ADD_DISCOVERY_QUERY, discoveryName, locationId, ipaddress, community, port);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
            200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + discoveryResult.getErrors(),
            (discoveryResult.getErrors() == null) || (discoveryResult.getErrors().isEmpty()));
    }

    private String getContainerIP(GenericContainer container) {
        NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();
        Map<String, ContainerNetwork> networksMap = networkSettings.getNetworks();
        return networksMap.values().iterator().next().getIpAddress();
    }

    private int convertStringIPToInt(String stringAddr) {
        try {
            InetAddress i = InetAddress.getByName(stringAddr);
            return ByteBuffer.wrap(i.getAddress()).getInt();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Test error converting string ip '" + stringAddr + "' to properly formatted inet addr");
        }
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

    private String getAllContainerIPs(Collection<GenericContainer> nodes) {
        Iterator<GenericContainer> nodeIterator = nodes.iterator();
        if (!nodeIterator.hasNext()) {
            throw new RuntimeException("Cannot get node IPs when there are no nodes");
        }
        String ips = getContainerIP(nodeIterator.next());

        while (nodeIterator.hasNext()) {
            ips += "," + getContainerIP(nodeIterator.next());
        }
        return ips;
    }

    @Then("Subnet discovery {string} for nodes using location {string} and IP range")
    public void discoveryByIPRange(String discoveryName, String location) throws IOException {
        GenericContainer node = nodes.values().iterator().next();
        if (node == null) {
            throw new RuntimeException("No nodes for subnet discovery");
        }

        String ipRange = calculateIPRanges(nodes.values());

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        String query = String.format(GQLQueryConstants.ADD_DISCOVERY_QUERY, discoveryName, locationId, ipRange, "public", 161);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
            200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + discoveryResult.getErrors(),
            (discoveryResult.getErrors() == null ) || (discoveryResult.getErrors().isEmpty()));
    }

    @Then("Subnet discovery {string} for nodes using location {string} and mask {long}")
    public void discoveryByMask(String discoveryName, String location, Long maskLong) throws IOException {
        long mask = maskLong.longValue();
        LOG.info("Subnet discovery with mask " + mask);
        GenericContainer node = nodes.values().iterator().next();
        if (node == null) {
            throw new RuntimeException("No nodes for subnet discovery");
        }
        if (mask < 24) {
            throw new RuntimeException("Tests only support discovery with masks at least 24");
        }

        String ipaddress = getContainerIP(node) + "/" + mask;
        LOG.info("IP:" + ipaddress);

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        String query = String.format(GQLQueryConstants.ADD_DISCOVERY_QUERY, discoveryName, locationId, ipaddress, "public", 161);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
            200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + discoveryResult.getErrors(),
            (discoveryResult.getErrors() == null ) || (discoveryResult.getErrors().isEmpty()));

    }

    public boolean checkTheStatusOfAllNodes(long numNodes, String expectedStatus) throws MalformedURLException {
        LOG.info("checkTheStatusOfAllNodes");

        String queryList = GQLQueryConstants.LIST_NODE_METRICS;

        ArrayList<Integer> nodeIds = getAllNodeIDs();

        if (nodeIds.size() != numNodes) {
            return false;
        }

        for (Iterator<Integer> iterator = nodeIds.iterator(); iterator.hasNext(); ) {
            Integer nextId = iterator.next();
            Map<String, Object> queryVariables = Map.of("id", nextId.intValue());

            GQLQuery gqlQuery = new GQLQuery();
            gqlQuery.setQuery(queryList);
            gqlQuery.setVariables(queryVariables);

            Response response = helper.executePostQuery(gqlQuery);

            JsonPath jsonPathEvaluator = response.jsonPath();
            LinkedHashMap lhm = jsonPathEvaluator.get("data");
            LinkedHashMap map = (LinkedHashMap) lhm.get("nodeStatus");
            String currentStatus = (String) map.get("status");
            LOG.info("Checking status " + currentStatus);
            if (!currentStatus.equals(expectedStatus)) {
                return false;
            }
        }

        return true;
    }

    public ArrayList<Integer> getAllNodeIDs() throws MalformedURLException {
        LOG.info("Getting the node IDs from the inventory");

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.GET_NODE_ID);

        Response response = helper.executePostQuery(gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap lhm = jsonPathEvaluator.get("data");
        ArrayList map = (ArrayList) lhm.get("findAllNodes");
        ArrayList<Integer> nodeIDs = new ArrayList<>();
        for (Iterator iterator = map.iterator(); iterator.hasNext(); ) {
            LinkedHashMap next = (LinkedHashMap) iterator.next();
            nodeIDs.add(Integer.valueOf((int) next.get("id")));
        }

        return nodeIDs;
    }

    @Then("Subnet discovery {string} for nodes using location {string} and IP list")
    public void subnetDiscoveryForNodesUsingLocationAndIPList(String discoveryName, String location) throws MalformedURLException {
        GenericContainer node = nodes.values().iterator().next();
        if (node == null) {
            throw new RuntimeException("No nodes for subnet discovery");
        }

        String ipList = getAllContainerIPs(nodes.values());

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        String query = String.format(GQLQueryConstants.ADD_DISCOVERY_QUERY, discoveryName, locationId, ipList, "public", 161);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
            200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + discoveryResult.getErrors(),
            (discoveryResult.getErrors() == null ) || (discoveryResult.getErrors().isEmpty()));
    }
}
