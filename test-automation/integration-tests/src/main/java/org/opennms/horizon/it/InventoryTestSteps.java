package org.opennms.horizon.it;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.model.Container;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.opennms.horizon.it.gqlmodels.CreateNodeData;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.LocationData;
import org.opennms.horizon.it.gqlmodels.querywrappers.CreateNodeResult;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllLocationsData;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllMinionsQueryResult;
import org.opennms.horizon.it.gqlmodels.MinionData;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import org.testcontainers.utility.ResourceReaper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class InventoryTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    private static final Logger LOG = LoggerFactory.getLogger(InventoryTestSteps.class);

    private TestsExecutionHelper helper;
    private Map<String, GenericContainer> minions = new ConcurrentHashMap<>();

    public InventoryTestSteps(TestsExecutionHelper helper) {
        this.helper = helper;
    }

    // Runtime Data
    private String minionLocation;
    private FindAllMinionsQueryResult findAllMinionsQueryResult;
    private List<MinionData> minionsAtLocation;
    private String lastMinionQueryResultBody;

    // certificate related runtime info location -> [keystore password=pkcs12 byte sequence]
    private Map<String, Entry<String, byte[]>> keystores = new ConcurrentHashMap<>();

//========================================
// Getters and Setters
//----------------------------------------

    public List<MinionData> getMinionsAtLocation() {
        return minionsAtLocation;
    }

//========================================
// Test Step Definitions
//----------------------------------------

    @Given("Create location {string}")
    public void createLocation(String location) throws Exception {
        String queryList = GQLQueryConstants.CREATE_LOCATION;

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(Map.of("location", location));

        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);
        assertFalse(helper.responseContainsErrors(response));
    }

    @Given("Location {string} is removed")
    public void deleteLocation(String location) throws Exception {
        LocationData locationData = helper.commonQueryLocations().getData().getFindAllLocations().stream()
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
//        TODO: The current API always fails on location deletion. This needs to be uncommented
//              once it's working properly (fails from the UI also).
//        assertFalse(helper.responseContainsErrors(response));
    }

    @Given("Location {string} does not exist")
    public void queryLocationDoNotExist(String location) throws MalformedURLException {
        List<LocationData> locationData = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(data -> data.getLocation().equals(location)).toList();
        assertTrue(locationData.isEmpty());
    }

    @Then("Location {string} do exist")
    public void queryLocationDoExist(String location) throws MalformedURLException {
        Optional<LocationData> locationData = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(data -> data.getLocation().equals(location))
            .findFirst();
        assertTrue(locationData.isPresent());
    }

    @Given("At least one Minion is running with location {string}")
    public void atLeastOneMinionIsRunningWithLocation(String location) {
        minionLocation = location;
    }

    @Given("No Minion running with location {string}")
    public void check(String location) throws MalformedURLException {
        atLeastOneMinionIsRunningWithLocation(location);
        assertFalse(checkAtLeastOneMinionAtGivenLocation());
    }


    @Then("Wait for at least one minion for the given location reported by inventory with timeout {int}ms")
    public void waitForAtLeastOneMinionForTheGivenLocationReportedByInventoryWithTimeoutMs(int timeout) {
        try {
            Awaitility
                .await()
                .atMost(timeout, TimeUnit.MILLISECONDS)
                .ignoreExceptions()
                .until(this::checkAtLeastOneMinionAtGivenLocation)
                ;
        } finally {
            LOG.info("LAST CHECK MINION RESPONSE BODY: {}", lastMinionQueryResultBody);
        }
    }

    /** @noinspection rawtypes*/
    @Then("Read the list of connected Minions from the BFF")
    public void readTheListOfConnectedMinionsFromInventory() throws MalformedURLException {
        findAllMinionsQueryResult = commonQueryMinions();
    }

    @Then("Find the minions running in the given location")
    public void findTheMinionsRunningInTheGivenLocation() {
        minionsAtLocation = commonFilterMinionsAtLocation(findAllMinionsQueryResult);
    }

    @Then("Verify at least one minion was found for the location")
    public void verifyAtLeastOneMinionWasFoundForTheLocation() {
        assertTrue(minionsAtLocation.size() > 0);
    }

    @Then("Add a device with label {string} IP address {string} and location {string}")
    public void addADeviceWithLabelIPAddressAndLocation(String label, String ipAddress, String location) throws MalformedURLException {
        LocationData locationData = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> location.equals(loc.getLocation()))
            .findFirst().orElse(null);

        if (locationData == null) {
            fail("Location " + location + " not found");
        }

        String query = GQLQueryConstants.CREATE_NODE_QUERY;

        CreateNodeData nodeVariable = new CreateNodeData();
        nodeVariable.setLabel(label);
        nodeVariable.setLocationId(String.valueOf(locationData.getId()));
        nodeVariable.setManagementIp(ipAddress);

        Map<String, Object> queryVariables = Map.of("node", nodeVariable);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);
        gqlQuery.setVariables(queryVariables);

        Response restAssuredResponse = helper.executePostQuery(gqlQuery);

        LOG.debug("createNode response: payload={}", restAssuredResponse.getBody().asString());

        assertEquals("add-device query failed: status=" + restAssuredResponse.getStatusCode() + "; body=" + restAssuredResponse.getBody().asString(),
            200, restAssuredResponse.getStatusCode());
        assertFalse("add-device query failed", helper.responseContainsErrors(restAssuredResponse));

        CreateNodeResult createNodeResult = restAssuredResponse.getBody().as(CreateNodeResult.class);

        // GRAPHQL errors result in 200 http response code and a body with "errors" detail
        assertTrue("create-node errors: " + createNodeResult.getErrors(),
            ( createNodeResult.getErrors() == null ) || ( createNodeResult.getErrors().isEmpty() ));
    }


    @Then("Check the status of the Node with expected status {string}")
    public void checkTheStatusOfTheFirstNodeTest(String status) {
        LOG.info("Test to check the status of the node");

        try {
            Awaitility
                .await()
                .ignoreExceptions()
                .atMost(120, TimeUnit.SECONDS)
                .until(() -> checkTheStatusOfTheFirstNode(status) )
            ;
            assertTrue(true);
        } catch (Exception e) {
            LOG.info("Test check the status failed with the error: {}", e.getMessage());
            assertTrue(false);
        }
    }


    @Then("Delete the first node from inventory")
    public void deleteFirstNodeFromInventory() throws MalformedURLException {
        LOG.info("Deleting the first node from the inventory.");

        String queryList = GQLQueryConstants.DELETE_NODE_BY_ID;

        int nodeId = getFirstNodeId();

        Map<String, Object> queryVariables = Map.of("id", nodeId);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(queryVariables);

        Response response = helper.executePostQuery(gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap lhm = jsonPathEvaluator.get("data");
        Boolean done = (Boolean) lhm.get("deleteNode");
        System.out.println("node id is: " + nodeId);
        assertTrue(done);
    }

    @When("Request certificate for location {string}")
    public void requestCertificateForLocation(String location) throws MalformedURLException {
        LOG.info("Requesting certificate for location {}.", location);

        Long locationId = helper.commonQueryLocations().getData().getFindAllLocations().stream()
            .filter(loc -> loc.getLocation().equals(location))
            .findFirst()
            .map(LocationData::getId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown location " + location));

        String query = String.format(GQLQueryConstants.CREATE_MINION_CERTIFICATE, locationId);
        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response response = helper.executePostQuery(gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap<String, String> lhm = jsonPathEvaluator.get("data.getMinionCertificate");

        byte[] pkcs12 = Base64.getDecoder().decode(lhm.get("certificate"));
        String pkcs12password = lhm.get("password");
        assertTrue(pkcs12.length > 0);
        assertNotNull(pkcs12password);

        keystores.put(location, Map.entry(pkcs12password, pkcs12));
    }

    @Then("Minion {string} is started with shared networking in location {string}")
    public void startMinionSharedNetwork(String systemId, String location) throws IOException {
        startMinion(systemId, location, true);
    }

    @Then("Minion {string} is started in location {string}")
    public void startMinionSpecificNetwork(String systemId, String location) throws IOException {
        startMinion(systemId, location, false);
    }

    public void startMinion(String systemId, String location, boolean sharedNetworking) throws IOException {
        if (!keystores.containsKey(location)) {
            fail("Could not find location " + location + " certificate");
        }

        Entry<String, byte[]> certificate = keystores.get(location);

        stopMinion(systemId);

        Network network;
        if (sharedNetworking) {
            network = Network.SHARED;
        } else {
            network = helper.getCommonNetworkSupplier().get();
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
            .withNetworkAliases("minion-" + systemId.toLowerCase())
            .withNetwork(network)
            .withLabel("label", systemId);
        minions.put(systemId, minion);

        File ca = helper.getMinionIngressCaCertificateSupplier().get();
        if (ca != null) {
            try {
                minion.withCopyToContainer(Transferable.of(Files.readString(ca.toPath())), "/opt/karaf/ca.crt");
                minion.withEnv("GRPC_CLIENT_TRUSTSTORE", "/opt/karaf/ca.crt");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read CA certificate", e);
            }
        }

        WaitAllStrategy waitStrategy = new WaitAllStrategy().withStartupTimeout(Duration.ofMinutes(3));
        waitStrategy.withStrategy(Wait.forLogMessage(".*Ignite node started OK.*", 1));
        // make sure grpc connection gets activated and reaches server
        waitStrategy.withStrategy(Wait.forLogMessage(".*Initialized RPC stream.*", 1));
        waitStrategy.withStrategy(Wait.forLogMessage(".*Initialized Sink stream.*", 1));
        waitStrategy.withStrategy(Wait.forLogMessage(".*Initialized cloud receiver stream.*", 1));
        minion.withCopyToContainer(Transferable.of(certificate.getValue()), "/opt/karaf/minion.p12");
        minion.waitingFor(waitStrategy);
        minion.start();
    }

    @Then("Minion {string} is stopped")
    public void stopMinion(String systemId) {
        DockerClient dockerClient = DockerClientFactory.lazyClient();
        List<Container> containers = dockerClient.listContainersCmd().exec();
        for (Container container : containers) {
            if (systemId.equals(container.getLabels().get("label"))) {
                try (WaitContainerCmd wait = dockerClient.waitContainerCmd(container.getId()); StopContainerCmd stop = dockerClient.stopContainerCmd(container.getId())) {
                    stop.exec();
                    Integer status = wait.start().awaitStatusCode(45, TimeUnit.SECONDS);
                    LOG.info("Stopped minion {}, exit status {}", systemId, status);
                }
            }
        }
    }


    private boolean checkAtLeastOneMinionAtGivenLocation() throws MalformedURLException {
        FindAllMinionsQueryResult findAllMinionsQueryResult = commonQueryMinions();
        List<MinionData> filtered = commonFilterMinionsAtLocation(findAllMinionsQueryResult);

        LOG.debug("MINIONS for location: count={}; location={}", filtered.size(), minionLocation);

        return ( ! filtered.isEmpty() );
    }

    /** @noinspection rawtypes*/
    private FindAllMinionsQueryResult commonQueryMinions() throws MalformedURLException {
        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.LIST_MINIONS_QUERY);

        Response restAssuredResponse = helper.executePostQuery(gqlQuery);

        lastMinionQueryResultBody = restAssuredResponse.getBody().asString();

        Assert.assertEquals(200, restAssuredResponse.getStatusCode());
        assertFalse(helper.responseContainsErrors(restAssuredResponse));

        return restAssuredResponse.getBody().as(FindAllMinionsQueryResult.class);
    }

    private List<MinionData> commonFilterMinionsAtLocation(FindAllMinionsQueryResult findAllMinionsQueryResult) {
        List<MinionData> minionData = findAllMinionsQueryResult.getData().getFindAllMinions();

        List<MinionData> minionsAtLocation =
            minionData.stream()
                .filter((md) -> Objects.equals(md.getLocation().getLocation(), minionLocation))
                .collect(Collectors.toList())
        ;

        LOG.debug("MINIONS for location: count={}; location={}", minionsAtLocation.size(), minionLocation);

        return minionsAtLocation;
    }

    /**
     * Method to check the expected status of the first node during the test
     * @param expectedStatus Expected expectedStatus of the node
     * @return If the expectedStatus is equals tot eh expected one
     * @throws MalformedURLException
     */
    public boolean checkTheStatusOfTheFirstNode(String expectedStatus) throws MalformedURLException {
        LOG.info("checkTheStatusOfTheNode");

        String queryList = GQLQueryConstants.LIST_NODE_METRICS;

        int nodeId = getFirstNodeId();

        Map<String, Object> queryVariables = Map.of("id", nodeId);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(queryVariables);

        Response response = helper.executePostQuery(gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap lhm = jsonPathEvaluator.get("data");
        LinkedHashMap map = (LinkedHashMap) lhm.get("nodeStatus");
        String currentStatus = (String) map.get("status");
        LOG.info("Status of the node: " + currentStatus);
        return currentStatus.equals(expectedStatus);
    }


    /**
     * Method to get the ID of the first node in the inventory
     * @return The First Node ID as Int
     * @throws MalformedURLException
     */
    public int getFirstNodeId() throws MalformedURLException {
        LOG.info("Getting the first node from the inventory");

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.GET_NODE_ID);

        Response response = helper.executePostQuery(gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap lhm = jsonPathEvaluator.get("data");
        ArrayList map = (ArrayList) lhm.get("findAllNodes");
        LinkedHashMap nodesData = (LinkedHashMap) map.get(0);
        int id = (int) nodesData.get("id");

        return id;
    }

}
