package org.opennms.horizon.it;

import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.awaitility.Awaitility;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.gqlmodels.MinionData;
import org.opennms.horizon.it.gqlmodels.querywrappers.MetricQueryResult;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class MetricsTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    private static final Logger LOG = LoggerFactory.getLogger(MetricsTestSteps.class);

    private TestsExecutionHelper helper;

    public MetricsTestSteps(TestsExecutionHelper helper) {
        this.helper = helper;
    }

    // Operations to access data from other TestSteps
    private Supplier<List<MinionData>> minionsAtLocationSupplier;

//========================================
// Getters and Setters
//----------------------------------------

    public void setMinionsAtLocationSupplier(Supplier<List<MinionData>> minionsAtLocationSupplier) {
        this.minionsAtLocationSupplier = minionsAtLocationSupplier;
    }


//========================================
// Test Step Definitions
//----------------------------------------

    @Then("Read the {string} metrics with label {string} set to the Minion System ID for each Minion found with timeout {int}ms")
    public void readTheMetricsWithLabelSetToTheMinionSystemID(String metricName, String labelName, long timeout) throws MalformedURLException {
        Awaitility
            .await()
            .ignoreExceptions()
            .atMost(timeout, TimeUnit.MILLISECONDS)
            .until(() -> this.commonCheckMinionMetrics(metricName, labelName))
            ;
    }


    @Then("Read the {string} metrics with label {string} set to {string} with timeout {int}ms")
    public void readTheMetricsWithLabelSetToWithTimeoutMs(String metricName, String labelName, String labelValue, int timeout) {
        Awaitility
            .await()
            .ignoreExceptions()
            .atMost(timeout, TimeUnit.MILLISECONDS)
            .until(() -> this.commonCheckDeviceMetrics(metricName, labelName, labelValue))
        ;
    }


//========================================
// Internals
//----------------------------------------

    private List<Pair<MinionData, MetricQueryResult>> commonQueryMinionMetrics(String metricName, String labelName) throws MalformedURLException {

        List<MinionData> minionsAtLocation = minionsAtLocationSupplier.get();
        List<Pair<MinionData, MetricQueryResult>> results = new LinkedList<>();
        for (MinionData oneMinion : minionsAtLocation) {
            String query =
                String.format(GQLQueryConstants.GET_LABELED_METRICS_QUERY, metricName, labelName, oneMinion.getSystemId());

            GQLQuery gqlQuery = new GQLQuery();
            gqlQuery.setQuery(query);

            Response restAssuredResponse = helper.executePostQuery(gqlQuery);

            assertEquals(200, restAssuredResponse.getStatusCode());

            MetricQueryResult metricQueryResult = restAssuredResponse.getBody().as(MetricQueryResult.class);

            results.add(Pair.of(oneMinion, metricQueryResult));
        }

        return results;
    }

    private boolean commonCheckMinionMetrics(String metricName, String labelName) throws MalformedURLException {
        List<Pair<MinionData, MetricQueryResult>> minionMetrics = commonQueryMinionMetrics(metricName, labelName);
        for (Pair<MinionData, MetricQueryResult> oneMinionMetric : minionMetrics) {
            MetricQueryResult metricQueryResult = oneMinionMetric.getRight();

            LOG.info("METRIC FOR MINION: metric-name={}; monitor={}; systemId={}; value={}; size={}",
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("__name__"),
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("monitor"),
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("system_id"),
                metricQueryResult.getData().getMetric().getData().getResult().get(0).getValue().get(1),
                metricQueryResult.getData().getMetric().getData().getResult().size()
            );

            assertEquals(1, metricQueryResult.getData().getMetric().getData().getResult().size());
            assertEquals("response_time_msec", metricQueryResult.getData().getMetric().getData().getResult().get(0).getMetric().get("__name__"));
        }

        return true;
    }

    private MetricQueryResult commonQueryDeviceMetrics(String metricName, String labelName, String labelValue) throws MalformedURLException {
        String query = String.format(GQLQueryConstants.GET_LABELED_METRICS_QUERY, metricName, labelName, labelValue);

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(query);

        Response restAssuredResponse = helper.executePostQuery(gqlQuery);

        assertEquals(200, restAssuredResponse.getStatusCode());

        MetricQueryResult metricQueryResult = restAssuredResponse.getBody().as(MetricQueryResult.class);

        return metricQueryResult;
    }

    private boolean commonCheckDeviceMetrics(String metricName, String labelName, String labelValue) throws MalformedURLException {
        MetricQueryResult metricQueryResult = commonQueryDeviceMetrics(metricName, labelName, labelValue);

        assertEquals(1, metricQueryResult.getData().getMetric().getData().getResult().size());

        LOG.info("READ metric for device: metric-name={}; label-name={}; label-value={}; metric-value={}",
            metricName, labelName, labelValue,
            metricQueryResult.getData().getMetric().getData().getResult().get(0).getValue().get(1));

        return true;
    }
}
