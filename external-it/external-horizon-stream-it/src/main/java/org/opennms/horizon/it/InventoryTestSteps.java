package org.opennms.horizon.it;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.opennms.horizon.it.gqlmodels.querywrappers.FindAllMinionsQueryResult;
import org.opennms.horizon.it.gqlmodels.MinionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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

}
