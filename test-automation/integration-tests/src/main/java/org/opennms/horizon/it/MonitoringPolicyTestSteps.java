package org.opennms.horizon.it;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.NetworkSettings;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.junit.CucumberOptions;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.opennms.horizon.it.gqlmodels.MonitorPolicyInputData;
import org.opennms.horizon.it.gqlmodels.PolicyRuleData;
import org.opennms.horizon.it.gqlmodels.AlertCondition;
import org.opennms.horizon.it.gqlmodels.ManagedObjectType;
import org.opennms.horizon.it.gqlmodels.AlertEventDefinitionInput;
import org.opennms.horizon.it.gqlmodels.EventType;
import org.opennms.horizon.it.gqlmodels.LocationData;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.MinionData;
import org.opennms.horizon.it.gqlmodels.querywrappers.AddDiscoveryResult;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllLocationsData;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllMinionsQueryResult;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@CucumberOptions(monochrome = true,
        features = "src/main/resources/org/opennms/horizon/it/monitoring-policy.feature")
public class MonitoringPolicyTestSteps {
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringPolicyTestSteps.class);
    private final TestsExecutionHelper helper;

    /* minions, containers ... */
    private final Map<String, GenericContainer<?>> minions = new ConcurrentHashMap<>();

    // Runtime Data
    private String minionLocation;
    private String lastMinionQueryResultBody;

    private final Map<String, Map.Entry<String, byte[]>> keystores = new ConcurrentHashMap<>();

    private final MonitorPolicyInputData monitorPolicyInputData = new MonitorPolicyInputData();
    private final PolicyRuleData policyRuleData = new PolicyRuleData();
    private final List<AlertCondition> alertConditions = new ArrayList<>();

    private GenericContainer<?> snmpContainer;
    private String minionIpaddress;
    private String snmpNodeIp;
    private int policyId;

    public MonitoringPolicyTestSteps(TestsExecutionHelper helper) {
        this.helper = helper;
    }

    @Given("Monitor policy name {string} and memo {string}")
    public void monitor_policy_name_and_memo(String name, String memo) {
        monitorPolicyInputData.setName(name);
        monitorPolicyInputData.setMemo(memo);
    }

    @Given("Notify by email {string}")
    public void notify_by_email(String notifyByEmail) {
        monitorPolicyInputData.setNotifyByEmail(Boolean.parseBoolean(notifyByEmail));
    }

    @Given("Policy rule name {string}, component type {string} and event type {string}")
    public void policyRule(String name, String componentType, String eventType) {
        policyRuleData.setName(name);
        ManagedObjectType managedObjectType = switch (componentType) {
            case "ANY" -> ManagedObjectType.ANY;
            case "NODE" -> ManagedObjectType.NODE;
            case "SNMP_INTERFACE" -> ManagedObjectType.SNMP_INTERFACE;
            case "SNMP_INTERFACE_LINK" -> ManagedObjectType.SNMP_INTERFACE_LINK;
            case "UNDEFINED" -> ManagedObjectType.UNDEFINED;
            case "UNRECOGNIZED" -> ManagedObjectType.UNRECOGNIZED;
            default -> null;
        };
        policyRuleData.setComponentType(managedObjectType);
        policyRuleData.setEventType(EventType.valueOf(eventType));

    }

    @Given("Alert conditions data")
    public void alertConditionsData(io.cucumber.datatable.DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            AlertCondition alertCondition = new AlertCondition();
            AlertEventDefinitionInput alertEventDefinitionInput = new AlertEventDefinitionInput();
            alertEventDefinitionInput.setId(Integer.parseInt(row.get("trigger event id")));
            alertEventDefinitionInput.setName(row.get("trigger event name"));
            alertEventDefinitionInput.setEventType(EventType.valueOf(row.get("event type")));
            alertCondition.setTriggerEvent(alertEventDefinitionInput);

            alertCondition.setCount(Integer.parseInt(row.get("count")));
            alertCondition.setOvertime(Integer.parseInt(row.get("overtime")));
            alertCondition.setOvertimeUnit(row.get("overtime unit"));
            alertCondition.setSeverity(row.get("severity"));
            alertConditions.add(alertCondition);
        }

        policyRuleData.setAlertConditions(alertConditions);
        monitorPolicyInputData.setRules(List.of(policyRuleData));
    }

    @When("The user saves the monitoring policy with name {string} and tag {string}")
    public void createAMonitoringPolicy(String name, String tag) {
        String queryList = "mutation createMonitorPolicy($policy: MonitorPolicyInput!) { createMonitorPolicy(policy: $policy) { id } }";

        monitorPolicyInputData.setName(name);
        List<String> tags = new ArrayList<>();
        tags.add(tag);
        monitorPolicyInputData.setTags(tags);
        Map<String, Object> queryVariables = Map.of("policy", monitorPolicyInputData);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(queryVariables);

        LOG.info("gqlQuery {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);

        String jsonResp = response.getBody().print();

        LOG.info("response.getBody().print() {}", jsonResp);
        JsonObject jsonObject = JsonParser.parseString(jsonResp).getAsJsonObject();

        policyId = jsonObject.getAsJsonObject("data").getAsJsonObject("createMonitorPolicy").get("id").getAsInt();
        LOG.info("policyId {}", policyId);

        assertEquals(response.getStatusCode(), 200);
    }

    @Then("There is a monitoring policy with name {string} and tag {string}")
    public void aMonitoringPolicyExists(String name, String tag) {
        String request = "query { findMonitorPolicyById(id: " + policyId + ") { name, tags } }";
        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();

        JsonObject jsonObject = JsonParser.parseString(jsonResp).getAsJsonObject();

        JsonObject policy = jsonObject.getAsJsonObject("data").getAsJsonObject("findMonitorPolicyById");

        if (null != policy && name.equals(policy.get("name").getAsString())) {
            JsonArray tags = policy.get("tags").getAsJsonArray();
            for (JsonElement element : tags) {
                if (tag.equals(element.getAsString())) {
                    LOG.info("Monitoring policy with name {} and tag {} exists.", name, tag);
                    return;
                }
            }
        }
        fail("Monitoring policy with name " + name + " and tag " + tag + " does not exist.");
    }

    @Given("Location {string} is created.")
    public void createLocation(String location) {
        String queryList = GQLQueryConstants.CREATE_LOCATION;

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(Map.of("location", location));

        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);
    }

    @Given("Location {string} is removed.")
    public void deleteLocation(String location) {
        LocationData locationData = commonQueryLocations().getData().getFindAllLocations().stream()
                .filter(loc -> loc.getLocation().equals(location))
                .findFirst().orElse(null);

        if (locationData == null) {
            fail("Location " + location + " not found");
        }

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.DELETE_LOCATION);
        gqlQuery.setVariables(Map.of("id", locationData.getId()));

        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);
    }

    @Then("Location {string} should exist")
    public void queryLocationDoExist(String location) {
        Optional<LocationData> locationData = commonQueryLocations().getData().getFindAllLocations().stream()
                .filter(data -> data.getLocation().equals(location))
                .findFirst();
        assertTrue(locationData.isPresent());
    }

    @Given("At least one Minion is running with location {string}.")
    public void atLeastOneMinionIsRunningWithLocation(String location) {
        minionLocation = location;
    }

    @Given("No minion running with location {string}")
    public void check(String location) throws MalformedURLException {
        atLeastOneMinionIsRunningWithLocation(location);
        assertFalse(checkAtLeastOneMinionAtGivenLocation());
    }

    @Then("Wait for at least one minion for the given location reported by inventory with timeout {int}ms.")
    public void waitForAtLeastOneMinionForTheGivenLocationReportedByInventoryWithTimeoutMs(int timeout) {
        try {
            Awaitility
                    .await()
                    .atMost(timeout, TimeUnit.MILLISECONDS)
                    .ignoreExceptions()
                    .until(this::checkAtLeastOneMinionAtGivenLocation);
        } finally {
            LOG.info("LAST CHECK MINION RESPONSE BODY: {}", lastMinionQueryResultBody);
        }
    }

    @Then("User can retrieve a certificate for location {string}")
    public void requestCertificateForLocation(String location) {
        LOG.info("Requesting certificate for location {}.", location);

        Long locationId = commonQueryLocations().getData().getFindAllLocations().stream()
                .filter(loc -> location.equals(loc.getLocation()))
                .findFirst()
                .map(LocationData::getId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(String.format(GQLQueryConstants.CREATE_MINION_CERTIFICATE, locationId));

        JsonPath jsonPathEvaluator = helper.executePostQuery(gqlQuery).jsonPath();
        LinkedHashMap<String, String> lhm = jsonPathEvaluator.get("data.getMinionCertificate");

        byte[] pkcs12 = Base64.getDecoder().decode(lhm.get("certificate"));
        String pkcs12password = lhm.get("password");
        assertTrue(pkcs12.length > 0);
        assertNotNull(pkcs12password);
        keystores.put(location, Map.entry(pkcs12password, pkcs12));
    }

    @Then("Minion {string} is started at location {string}")
    public void startMinion(String systemId, String location) {
        if (!keystores.containsKey(location)) {
            fail("Could not find location " + location + " certificate");
        }

        Map.Entry<String, byte[]> certificate = keystores.get(location);
        minions.compute(systemId, (key, existing) -> {
            if (existing != null && existing.isRunning()) {
                existing.stop();
            }
            GenericContainer<?> minion = new GenericContainer<>(DockerImageName.parse(helper.getMinionImageNameSupplier().get()))
                    .withEnv("MINION_GATEWAY_HOST", helper.getMinionIngressSupplier().get())
                    .withEnv("MINION_GATEWAY_PORT", String.valueOf(helper.getMinionIngressPortSupplier().get()))
                    .withEnv("MINION_GATEWAY_TLS", String.valueOf(helper.getMinionIngressTlsEnabledSupplier().get()))
                    .withEnv("MINION_ID", systemId)
                    .withEnv("USE_KUBERNETES", "false")
                    .withEnv("GRPC_CLIENT_KEYSTORE", "/opt/karaf/minion.p12")
                    .withEnv("GRPC_CLIENT_KEYSTORE_PASSWORD", certificate.getKey())
                    .withEnv("GRPC_CLIENT_OVERRIDE_AUTHORITY", helper.getMinionIngressOverrideAuthority().get())
                    .withEnv("IGNITE_SERVER_ADDRESSES", "localhost")
                    .withNetworkAliases("minion")
                    .withNetwork(Network.SHARED)
                    .withLabel("label", key);

            File ca = helper.getMinionIngressCaCertificateSupplier().get();
            if (ca != null) {
                try {
                    minion.withCopyToContainer(Transferable.of(Files.readString(ca.toPath())), "/opt/karaf/ca.crt");
                    minion.withEnv("GRPC_CLIENT_TRUSTSTORE", "/opt/karaf/ca.crt");
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read CA certificate", e);
                }
            }

            minion.withCopyToContainer(Transferable.of(certificate.getValue()), "/opt/karaf/minion.p12");
            minion.waitingFor(Wait.forLogMessage(".* Listening on \\[all interfaces\\]:1162.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
            minion.start();

            return minion;
        });
    }

    @And("SNMP node {string} is started in the network of minion {string}")
    public void startSNMPNode(String nodeLabel, String systemId) {

        LOG.info("Starting node with systemId: " + systemId);

        GenericContainer<?> minion = minions.get(systemId);

        snmpContainer = new GenericContainer<>(DockerImageName.parse(helper.getNodeImageNameSupplier().get()))
                .withNetworkAliases("nodes")
                .withNetwork(minion.getNetwork())
                .withCopyFileToContainer(MountableFile.forClasspathResource("BOOT-INF/classes/snmpd/snmpd.conf"), "/etc/snmp/snmpd.conf")
                .withLabel("label", nodeLabel);

        snmpContainer.waitingFor(Wait.forLogMessage(".*SNMPD Daemon started.*", 1).withStartupTimeout(Duration.ofMinutes(3)));
        snmpContainer.start();
        NetworkSettings networkSettings = minion.getContainerInfo().getNetworkSettings();
        Map<String, ContainerNetwork> networksMap = networkSettings.getNetworks();
        minionIpaddress = networksMap.values().iterator().next().getIpAddress();

        LOG.info("MINION ip={}", minionIpaddress);

    }

    @Then("Discover {string} for snmp node {string}, location {string} is created to discover by IP with policy tag {string}")
    public void discoverSingleNodeWithDefaults(String discoveryName, String nodeName, String location, String policyTag) throws MalformedURLException {
        discoverSNMPNode(discoveryName, nodeName, location, 161, "public", policyTag);
    }

    public void discoverSNMPNode(String discoveryName, String nodeName, String location, int port, String community, String policyTag) throws MalformedURLException {
        GenericContainer<?> node = snmpContainer;
        if (node == null) {
            throw new RuntimeException("No node matching name " + nodeName);
        }

        NetworkSettings networkSettings = snmpContainer.getContainerInfo().getNetworkSettings();
        Map<String, ContainerNetwork> networksMap = networkSettings.getNetworks();

        snmpNodeIp = networksMap.values().iterator().next().getIpAddress();

        LOG.info("SNMP node ip={}", snmpNodeIp);

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
                .filter(loc -> location.equals(loc.getLocation()))
                .findFirst()
                .map(LocationData::getId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        String ADD_DISCOVERY_QUERY_WITH_TAG =
                "mutation { createIcmpActiveDiscovery( request: { name: \"%s\", locationId: \"%s\", ipAddresses: [\"%s\"], snmpConfig: { readCommunities: [\"%s\"], ports: [%d]\n" +
                        " }, tags: [ { name: \"%s\" } ] } ) {id, name, ipAddresses, locationId, snmpConfig { readCommunities, ports } } }";

        String query = String.format(ADD_DISCOVERY_QUERY_WITH_TAG, discoveryName, locationId, snmpNodeIp, community, port, policyTag);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("add-discovery query failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
                200, response.getStatusCode());

        AddDiscoveryResult discoveryResult = response.getBody().as(AddDiscoveryResult.class);

        long snmpNodeId = discoveryResult.getData().getCreateIcmpActiveDiscovery().getId();
        LOG.info("SNMP node id={}", snmpNodeId);

        assertTrue("create-node errors: " + discoveryResult.getErrors(),
                (discoveryResult.getErrors() == null) || (discoveryResult.getErrors().isEmpty()));
    }

    @When("The snmp node sends a {string} to Minion")
    public void sendTrap(String triggerEventType) throws Exception {

        switch (triggerEventType) {
            case "SNMP Cold Start" -> sendTrapToMinion("1.3.6.1.6.3.1.1.5.1");
            case "SNMP Warm Start" -> sendTrapToMinion("1.3.6.1.6.3.1.1.5.2");
            case "SNMP Authen Failure" -> sendTrapToMinion("1.3.6.1.6.3.1.1.5.5");
            case "SNMP Link Down" -> sendTrapToMinion("1.3.6.1.6.3.1.1.5.3");
            case "SNMP Link Up" -> sendTrapToMinion("1.3.6.1.6.3.1.1.5.4");
            case "SNMP EGP Down" -> sendTrapToMinion("1.3.6.1.6.3.1.1.5.6");
            default -> LOG.error("trigger event type not supported: " + triggerEventType);
        }
    }

    private void sendTrapToMinion(String oid) throws Exception {
        String community = "public";
        String trapReceiver = minionIpaddress + ":1162";
        String[] command = {
                "snmptrap",
                "-v", "2c",
                "-c", community,
                trapReceiver,
                "", // empty string for the trap specific arguments
                oid
        };

        Container.ExecResult sendTrapResult = snmpContainer.execInContainer(command);
        String stdout = sendTrapResult.getStdout();
        int exitCode = sendTrapResult.getExitCode();
        LOG.info("Docker sendTrapResult stdout={}, exitCode={}, sendTrapResult={}", stdout, exitCode, sendTrapResult.getStderr());

    }

    @Then("An alert with {string} should be triggered with severity {string}")
    public void verifyAlertSeverity(String triggerEventName, String severity) {
        Awaitility
                .await()
                .atMost(20, TimeUnit.SECONDS)
                .pollDelay(5, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> waitForAlerts(triggerEventName));

        String nodeLabel = getNodeLabel();
        JsonArray alerts = getAlerts(nodeLabel);

        String alertSeverity = null;
        for (JsonElement element : alerts) {
            JsonObject alert = (JsonObject) element;
            String nodeName = alert.get("nodeName").getAsString();
            LOG.info(nodeName + " : " + nodeLabel);
            String eventUei = alert.get("uei").getAsString();
            if (nodeName.equals(nodeLabel) && eventUei.endsWith(triggerEventName.replaceAll(" ", "_"))) {
                alertSeverity = alert.get("severity").getAsString();
                break;
            }
        }
        assertEquals("Severity: " + severity + " was expected but got " + alertSeverity + " instead.", severity, alertSeverity);
    }

    private boolean waitForAlerts(String triggerEventName) {
        String nodeLabel = getNodeLabel();
        JsonArray alerts = getAlerts(nodeLabel);

        for (JsonElement element : alerts) {
            JsonObject alert = (JsonObject) element;
            String nodeName = alert.get("nodeName").getAsString();
            LOG.info(nodeName + " : " + nodeLabel);
            String eventUei = alert.get("uei").getAsString();
            if (nodeName.equals(nodeLabel) && eventUei.endsWith(triggerEventName.replaceAll(" ", "_"))) {
                return true;
            }
        }
        return false;
    }

    public String getNodeLabel() {

        String request = """
                query NodesTableParts { findAllNodes { id nodeLabel
                      ipInterfaces {
                        id
                        ipAddress
                      }
                      }}""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();

        JsonObject jsonObject = JsonParser.parseString(jsonResp).getAsJsonObject();

        JsonArray nodes = jsonObject.getAsJsonObject("data").getAsJsonArray("findAllNodes");
        for (JsonElement nodeElement : nodes) {
            JsonObject node = (JsonObject) nodeElement;
            JsonArray ipInterfaces = node.getAsJsonArray("ipInterfaces");
            for (JsonElement element : ipInterfaces) {
                JsonObject ipInterface = (JsonObject) element;
                String ipAddress = ipInterface.get("ipAddress").getAsString();
                LOG.info(ipAddress + " : " + snmpNodeIp);
                if (ipAddress.equals(snmpNodeIp)) {
                    return node.get("nodeLabel").getAsString();
                }
            }
        }
        return null;
    }

    public JsonArray getAlerts(String nodeLabel) {

        String request = """
                query {
                  findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, sortBy: "tenantId", sortAscending: true, nodeLabel:""" + " \"" + nodeLabel + "\"\n" + """
                    ) {
                    nextPage
                    alerts {
                        severity
                        label
                        nodeName
                        uei
                    }
                  }
                }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();

        JsonObject jsonObject = JsonParser.parseString(jsonResp).getAsJsonObject();

        return jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
    }

    @Then("Minion {string} is stopped.")
    public void stopMinion(String systemId) {

        GenericContainer<?> minion = minions.get(systemId);

        if (minion != null && minion.isRunning()) {
            minion.stop();
        }
    }

    private boolean checkAtLeastOneMinionAtGivenLocation() {
        FindAllMinionsQueryResult findAllMinionsQueryResult = commonQueryMinions();
        List<MinionData> filtered = commonFilterMinionsAtLocation(findAllMinionsQueryResult);

        LOG.debug("MINIONS for location: count={}; location={}", filtered.size(), minionLocation);

        return (!filtered.isEmpty());
    }

    private FindAllLocationsData commonQueryLocations() {
        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.LIST_LOCATIONS_QUERY);
        Response restAssuredResponse = helper.executePostQuery(gqlQuery);
        assertEquals(200, restAssuredResponse.getStatusCode());
        LOG.info("FindAllLocationsData {}", restAssuredResponse.getBody().print());
        return restAssuredResponse.getBody().as(FindAllLocationsData.class);
    }

    private FindAllMinionsQueryResult commonQueryMinions() {
        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.LIST_MINIONS_QUERY);

        Response restAssuredResponse = helper.executePostQuery(gqlQuery);
        lastMinionQueryResultBody = restAssuredResponse.getBody().asString();
        assertEquals(200, restAssuredResponse.getStatusCode());
        return restAssuredResponse.getBody().as(FindAllMinionsQueryResult.class);
    }

    private List<MinionData> commonFilterMinionsAtLocation(FindAllMinionsQueryResult findAllMinionsQueryResult) {
        List<MinionData> minionData = findAllMinionsQueryResult.getData().getFindAllMinions();

        List<MinionData> minionsAtLocation =
                minionData.stream()
                        .filter((md) -> Objects.equals(md.getLocation().getLocation(), minionLocation))
                        .collect(Collectors.toList());

        LOG.debug("MINIONS for location: count={}; location={}", minionsAtLocation.size(), minionLocation);

        return minionsAtLocation;
    }
}
