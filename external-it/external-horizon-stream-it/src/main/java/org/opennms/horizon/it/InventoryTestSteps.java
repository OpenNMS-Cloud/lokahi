package org.opennms.horizon.it;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.opennms.horizon.it.gqlmodels.CreateNodeData;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.querywrappers.CreateNodeResult;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllMinionsQueryResult;
import org.opennms.horizon.it.gqlmodels.MinionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InventoryTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    private static final Logger LOG = LoggerFactory.getLogger(InventoryTestSteps.class);

    // Operations to access data from other TestSteps
    private Supplier<String> userAccessTokenSupplier;
    private Supplier<String> ingressUrlSupplier;

    // Runtime Data
    private String minionLocation;
    private FindAllMinionsQueryResult findAllMinionsQueryResult;
    private List<MinionData> minionsAtLocation;
    private String lastMinionQueryResultBody;

//========================================
// Getters and Setters
//----------------------------------------

    public Supplier<String> getUserAccessTokenSupplier() {
        return userAccessTokenSupplier;
    }

    public void setUserAccessTokenSupplier(Supplier<String> userAccessTokenSupplier) {
        this.userAccessTokenSupplier = userAccessTokenSupplier;
    }

    public Supplier<String> getIngressUrlSupplier() {
        return ingressUrlSupplier;
    }

    public void setIngressUrlSupplier(Supplier<String> ingressUrlSupplier) {
        this.ingressUrlSupplier = ingressUrlSupplier;
    }

    public List<MinionData> getMinionsAtLocation() {
        return minionsAtLocation;
    }

//========================================
// Test Step Definitions
//----------------------------------------

    @Given("At least one Minion is running with location {string}")
    public void atLeastOneMinionIsRunningWithLocation(String location) {
        minionLocation = location;
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
        URL url = formatIngressUrl("/api/graphql");
        String accessToken = userAccessTokenSupplier.get();

        String query = GQLQueryConstants.CREATE_NODE_QUERY;

        CreateNodeData nodeVariable = new CreateNodeData();
        nodeVariable.setLabel(label);
        nodeVariable.setLocation(location);
        nodeVariable.setManagementIp(ipAddress);

        Map<String, Object> queryVariables = Map.of("node", nodeVariable);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);
        gqlQuery.setVariables(queryVariables);

        Response restAssuredResponse = executePost(url, accessToken, gqlQuery);

        LOG.debug("createNode response: payload={}", restAssuredResponse.getBody().asString());

        assertEquals("add-device query failed: status=" + restAssuredResponse.getStatusCode() + "; body=" + restAssuredResponse.getBody().asString(),
            200, restAssuredResponse.getStatusCode());

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
                .atMost(30, TimeUnit.SECONDS)
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
        URL url = formatIngressUrl("/api/graphql");
        String accessToken = userAccessTokenSupplier.get();

        String queryList = GQLQueryConstants.DELETE_NODE_BY_ID;

        int nodeId = getFirstNodeId();

        Map<String, Object> queryVariables = Map.of("id", nodeId);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(queryVariables);

        Response response = executePost(url, accessToken, gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap lhm = jsonPathEvaluator.get("data");
        Boolean done = (Boolean) lhm.get("deleteNode");
        System.out.println("node id is: " + nodeId);
        assertTrue(done);
    }

//========================================
// Internals
//----------------------------------------

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation("SSL"))
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private URL formatIngressUrl(String path) throws MalformedURLException {
        String baseUrl = ingressUrlSupplier.get();

        return new URL(new URL(baseUrl), path);
    }

    private String formatAuthorizationHeader(String token) {
        return "Bearer " + token;
    }

    private Response executePost(URL url, String accessToken, Object body) {
        RestAssuredConfig restAssuredConfig = createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig)
            ;

        Response restAssuredResponse =
            requestSpecification
                .header(HttpHeaders.AUTHORIZATION, formatAuthorizationHeader(accessToken))
                .header(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .body(body)
                .post(url)
                .thenReturn()
            ;

        return restAssuredResponse;
    }

    private boolean checkAtLeastOneMinionAtGivenLocation() throws MalformedURLException {
        FindAllMinionsQueryResult findAllMinionsQueryResult = commonQueryMinions();
        List<MinionData> filtered = commonFilterMinionsAtLocation(findAllMinionsQueryResult);

        LOG.debug("MINIONS for location: count={}; location={}", filtered.size(), minionLocation);

        return ( ! filtered.isEmpty() );
    }

    /** @noinspection rawtypes*/
    private FindAllMinionsQueryResult commonQueryMinions() throws MalformedURLException {
        String accessToken = userAccessTokenSupplier.get();

        URL url = formatIngressUrl("/api/graphql");

        Response restAssuredResponse = executePost(url, accessToken, GQLQueryConstants.LIST_MINIONS_QUERY);

        lastMinionQueryResultBody = restAssuredResponse.getBody().asString();

        Assert.assertEquals(200, restAssuredResponse.getStatusCode());

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
        URL url = formatIngressUrl("/api/graphql");
        String accessToken = userAccessTokenSupplier.get();

        String queryList = GQLQueryConstants.LIST_NODE_METRICS;

        int nodeId = getFirstNodeId();

        Map<String, Object> queryVariables = Map.of("id", nodeId);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(queryList);
        gqlQuery.setVariables(queryVariables);

        Response response = executePost(url, accessToken, gqlQuery);

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
        URL url = formatIngressUrl("/api/graphql");
        String accessToken = userAccessTokenSupplier.get();

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(GQLQueryConstants.GET_NODE_ID);

        Response response = executePost(url, accessToken, gqlQuery);

        JsonPath jsonPathEvaluator = response.jsonPath();
        LinkedHashMap lhm = jsonPathEvaluator.get("data");
        ArrayList map = (ArrayList) lhm.get("findAllNodes");
        LinkedHashMap nodesData = (LinkedHashMap) map.get(0);
        int id = (int) nodesData.get("id");

        return id;
    }

}
