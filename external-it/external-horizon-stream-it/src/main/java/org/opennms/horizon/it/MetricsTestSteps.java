package org.opennms.horizon.it;

import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.awaitility.Awaitility;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.MinionData;
import org.opennms.horizon.it.gqlmodels.querywrappers.MetricQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class MetricsTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    private static final Logger LOG = LoggerFactory.getLogger(MetricsTestSteps.class);

    // Operations to access data from other TestSteps
    private Supplier<String> userAccessTokenSupplier;
    private Supplier<String> ingressUrlSupplier;
    private Supplier<List<MinionData>> minionsAtLocationSupplier;

//========================================
// Getters and Setters
//----------------------------------------

    public Supplier<String> getUserAccessTokenSupplier() {
        return userAccessTokenSupplier;
    }

    public void setUserAccessTokenSupplier(Supplier<String> userAccessTokenSupplier) {
        this.userAccessTokenSupplier = userAccessTokenSupplier;
    }

    public Supplier<List<MinionData>> getMinionsAtLocationSupplier() {
        return minionsAtLocationSupplier;
    }

    public void setMinionsAtLocationSupplier(Supplier<List<MinionData>> minionsAtLocationSupplier) {
        this.minionsAtLocationSupplier = minionsAtLocationSupplier;
    }

    public Supplier<String> getIngressUrlSupplier() {
        return ingressUrlSupplier;
    }

    public void setIngressUrlSupplier(Supplier<String> ingressUrlSupplier) {
        this.ingressUrlSupplier = ingressUrlSupplier;
    }

//========================================
// Test Step Definitions
//----------------------------------------

    @Then("Read the {string} from Prometheus with label {string} set to the Minion System ID for each Minion found with timeout {int}ms")
    public void readTheFromPrometheusWithLabelSetToTheMinionSystemID(String metricName, String labelName, long timeout) throws MalformedURLException {
        Awaitility
            .await()
            .ignoreExceptions()
            .atMost(timeout, TimeUnit.MILLISECONDS)
            .until(() -> this.commonCheckMinionMetrics(metricName, labelName))
            ;
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

    private List<Pair<MinionData, MetricQueryResult>> commonQueryMetrics(String metricName, String labelName) throws MalformedURLException {
        String accessToken = userAccessTokenSupplier.get();

        URL url = formatIngressUrl("/api/graphql");

        List<MinionData> minionsAtLocation = minionsAtLocationSupplier.get();
        List<Pair<MinionData, MetricQueryResult>> results = new LinkedList<>();
        for (MinionData oneMinion : minionsAtLocation) {
            String query =
                String.format(GQLQueryConstants.GET_LABELED_METRICS_QUERY, metricName, labelName, oneMinion.getSystemId());

            GQLQuery gqlQuery = new GQLQuery();
            gqlQuery.setQuery(query);

            Response restAssuredResponse = executePost(url, accessToken, gqlQuery);

            assertEquals(200, restAssuredResponse.getStatusCode());

            MetricQueryResult metricQueryResult = restAssuredResponse.getBody().as(MetricQueryResult.class);

            results.add(Pair.of(oneMinion, metricQueryResult));
        }

        return results;
    }

    private boolean commonCheckMinionMetrics(String metricName, String labelName) throws MalformedURLException {
        List<Pair<MinionData, MetricQueryResult>> minionMetrics = commonQueryMetrics(metricName, labelName);
        for (Pair<MinionData, MetricQueryResult> oneMinionMetric : minionMetrics) {
            MetricQueryResult metricQueryResult = oneMinionMetric.getRight();

            LOG.info("METRIC FOR MINION: metric-name={}; monitor={}; value={}",
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("__name__"),
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("monitor"),
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getValue().get(0)
            );

            assertEquals(1, metricQueryResult.getData().getMetric().getData().getResult().size());
            assertEquals("response_time_msec", metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("__name__"));
        }

        return true;
    }
}
