package org.opennms.horizon.it;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.junit.CucumberOptions;
import io.restassured.response.Response;
import org.opennms.horizon.it.gqlmodels.AlertTimeRange;
import org.opennms.horizon.it.gqlmodels.GQLQuery;
import org.opennms.horizon.it.helper.TestsExecutionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

@CucumberOptions(monochrome = true,
        features = "src/main/resources/org/opennms/horizon/it/alert-page.feature")
public class AlertPageTestSteps {
    private static final Logger LOG = LoggerFactory.getLogger(AlertPageTestSteps.class);
    private final TestsExecutionHelper helper;
    private Supplier<Stream<JsonElement>> allAlerts;
    private Supplier<Stream<JsonElement>> sortedAlerts;
    private Stream<JsonElement> alertsWithClearFilters;
    private String filterBy;
    private AlertTimeRange alertTimeRange = null;
    private long idCleared = -1;
    private long idAcknowledged = -1;

    public AlertPageTestSteps(TestsExecutionHelper helper) {
        this.helper = helper;
    }

    @Then("At least {int} alerts have been triggered")
    public void fetchAlerts(int count) {

        String request = """
                query {
                  findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, sortBy: "tenantId", sortAscending: true                                                   
                                ) {
                                nextPage
                                alerts {
                                    severity
                                    label
                                    nodeName
                                    lastUpdateTimeMs
                                }
                              }
                            }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();

        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        assertTrue("Less than " + count + " were fetched.", count <= alerts.size());

    }

    @Given("I am on the Alerts page")
    public void onAlertsPage() {
        String request = """
                query {
                  findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, sortBy: "tenantId", sortAscending: true                                                                   
                                ) {
                                nextPage
                                alerts {
                                    databaseId
                                    severity
                                    acknowledged
                                    label
                                    nodeName
                                    lastUpdateTimeMs
                                }
                              }
                            }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();
        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        allAlerts = () -> StreamSupport.stream(alerts.spliterator(), true);

    }

    @When("I view the Alert list")
    public void viewAlertList() {
        System.out.print(allAlerts);
    }

    @And("I sort the list by {string}")
    public void sortAlertListBy(String sortBy) {
        String request = """
                query {
                  findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, sortBy: \"""" + sortBy + "\", sortAscending: true\n" + """
                    ) {
                    nextPage
                    alerts {
                        severity
                        label
                        nodeName
                    }
                  }
                }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();

        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();
        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        sortedAlerts = () -> StreamSupport.stream(alerts.spliterator(), true);

    }

    @Then("I will see a list filtered by Alerts with an Active state ordered by {string}")
    public void listOrderedBy(String sortBy) {

        ArrayList<String> alertList = new ArrayList<>();
        sortedAlerts.get().map(element -> alertList.add(((JsonObject) element).get("severity").getAsString()));

        LOG.info("Alerts sorted by {}: {}", sortBy, alertList);
        String[] alertArray = alertList.toArray(new String[0]);
        Arrays.sort(alertArray);
        assertEquals("Alerts were not ordered by " + sortBy, alertList, Arrays.asList(alertArray));

    }

    @And("I pick a time window of {string}")
    public void pickATimeWindow(String timeRange) {
        LOG.info("pick a time window {}", timeRange);
        alertTimeRange = switch (timeRange) {
            case "ALL" -> AlertTimeRange.ALL;
            case "LAST_24_HOURS" -> AlertTimeRange.LAST_24_HOURS;
            case "SEVEN_DAYS" -> AlertTimeRange.SEVEN_DAYS;
            case "TODAY" -> AlertTimeRange.TODAY;
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        };
    }

    @Then("I will see a list filtered by Alerts that occurred in the time window")
    public void listFilteredByAlertsInTimeWindow() {
        String request = """
                query {
                  findAllAlerts(pageSize: 20, page: 0, timeRange: """ + alertTimeRange + ",\n" + """
                     sortBy: "tenantId", sortAscending: true                                                                   
                    ) {
                    nextPage
                    alerts {
                        severity
                        label
                        nodeName
                        lastUpdateTimeMs
                    }
                  }
                }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - alerts occurred in {} {} ", alertTimeRange, gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();

        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        Stream<JsonElement> alertStream = StreamSupport.stream(alerts.spliterator(), true);
        long count = alertStream.filter(element -> !isTimeInTimeRange(((JsonObject) element).get("lastUpdateTimeMs").getAsLong())).count();
        assertEquals("The event time is not in the range of " + alertTimeRange, count, 0);
    }

    private boolean isTimeInTimeRange(long eventTimeMillis) {
        Instant currentTime = Instant.ofEpochMilli(System.currentTimeMillis());
        Instant eventTime = Instant.ofEpochMilli(eventTimeMillis);
        Instant start = switch (alertTimeRange) {
            case TODAY -> currentTime.truncatedTo(ChronoUnit.DAYS);
            case LAST_24_HOURS -> currentTime.minus(24, ChronoUnit.HOURS);
            case SEVEN_DAYS -> currentTime.minus(7, ChronoUnit.DAYS);
            default -> throw new IllegalArgumentException("Invalid time range: " + alertTimeRange);
        };
        return !eventTime.isBefore(start);
    }

    @And("I have a filter toggled for alerts with severity {string}")
    public void listFilterBySeverity(String filterBy) {
        this.filterBy = filterBy;
        String request = """
                query {
                      findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, severities: [\"""" + filterBy + "\"], sortBy: \"severity\", sortAscending: true\n" + """            
                          ) {
                          nextPage
                          alerts {
                             databaseId
                             severity
                          }
                        }
                }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();
        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        Stream<JsonElement> alertStream = StreamSupport.stream(alerts.spliterator(), true);

        assertTrue("Alert severity verification failed.", alertStream.allMatch(element -> filterBy.equals(((JsonObject) element).get("severity").getAsString())));

    }

    @When("I choose to clear filters")
    public void clearFilters() {
        String request = """
                query {
                      findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, severities: [], sortBy: "severity", sortAscending: true            
                                ) {
                                nextPage
                                alerts {
                                   databaseId
                                   severity
                                }
                              }
                      }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();
        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        alertsWithClearFilters = StreamSupport.stream(alerts.spliterator(), true);

    }

    @Then("alerts from all severities will show")
    public void showAllAlerts() {
        assertTrue("Clear filters failed.", alertsWithClearFilters.anyMatch(element -> !filterBy.equals(((JsonObject) element).get("severity").getAsString())));
    }

    @And("I have an ACTIVE alert")
    public void selectAnActiveAlert() {

        Supplier<Stream<JsonElement>> filteredAlerts = () -> allAlerts.get().filter(element -> !"CLEARED".equals(((JsonObject) element).get("severity").getAsString()));
        if (filteredAlerts.get().findAny().isPresent()) {
            idCleared = ((JsonObject) filteredAlerts.get().findFirst().get()).get("databaseId").getAsLong();
        }
        assertTrue("Failed to find an active alert", filteredAlerts.get().findAny().isPresent());

    }

    @When("I choose to clear the alert")
    public void clearAlert() {

        String clearAlert = "mutation { clearAlert( ids:[" + idCleared + "]) {alertErrorList{databaseId error} alertList{databaseId severity} }}";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(clearAlert);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("clear alert failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
                200, response.getStatusCode());

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();

        JsonArray errors = jsonObject.getAsJsonObject("data").getAsJsonObject("clearAlert").getAsJsonArray("alertErrorList");

        assertEquals("Clear alert failed. " + errors, errors.size(), 0);

    }

    @Then("the status of the Alert will change to {string}")
    public void verifyAlertStatus(String status) {
        String request = """
                query {
                      findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, severities: [\"""" + status + "\"], sortBy: \"severity\", sortAscending: true\n" + """            
                          ) {
                          nextPage
                          alerts {
                             databaseId
                          }
                        }
                }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();

        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");
        Stream<JsonElement> alertStream = StreamSupport.stream(alerts.spliterator(), true);

        Stream<JsonElement> filteredAlerts = alertStream.filter(element -> idCleared == ((JsonObject) element).get("databaseId").getAsLong());
        assertTrue("The status of the alert with id: " + idCleared + " does not change to " + status, filteredAlerts.findAny().isPresent());
    }

    @And("I have an UNACKNOWLEDGED alert")
    public void selectAnUnacknoledgedAlert() {

        Supplier<Stream<JsonElement>> filteredAlerts = () -> allAlerts.get().filter(element ->
                !((JsonObject) element).get("acknowledged").getAsBoolean() && !"CLEARED".equals(((JsonObject) element).get("severity").getAsString()));
        if (filteredAlerts.get().findAny().isPresent()) {
            idAcknowledged = ((JsonObject) filteredAlerts.get().findAny().get()).get("databaseId").getAsLong();
        }
        assertTrue("Failed to find an UNACKNOWLEDGED alert", filteredAlerts.get().findAny().isPresent());

    }

    @When("I choose to acknowledge the alert")
    public void acknowledgeAlert() {

        String clearAlert = "mutation { acknowledgeAlert( ids:[" + idAcknowledged + "]) {alertErrorList{databaseId error} alertList{databaseId acknowledged severity} }}";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(clearAlert);

        Response response = helper.executePostQuery(gqlQuery);

        assertEquals("acknowledge alert failed: status=" + response.getStatusCode() + "; body=" + response.getBody().asString(),
                200, response.getStatusCode());

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();

        JsonArray errors = jsonObject.getAsJsonObject("data").getAsJsonObject("acknowledgeAlert").getAsJsonArray("alertErrorList");

        assertEquals("acknowledge alert failed. " + errors, errors.size(), 0);

    }

    @Then("the Alert is acknowledged")
    public void verifyAcknowledgement() {
        String request = """
                query {
                      findAllAlerts(pageSize: 20, page: 0, timeRange: ALL, sortBy: "severity", sortAscending: true         
                                ) {
                                nextPage
                                alerts {
                                   databaseId
                                   acknowledged
                                }
                              }
                      }""";

        GQLQuery gqlQuery = new GQLQuery();
        gqlQuery.setQuery(request);

        LOG.info("gqlQuery - all alerts {}", gqlQuery);
        Response response = helper.executePostQuery(gqlQuery);
        assertEquals(response.getStatusCode(), 200);

        String jsonResp = response.getBody().print();
        JsonObject jsonObject = new JsonParser().parse(jsonResp).getAsJsonObject();

        JsonArray alerts = jsonObject.getAsJsonObject("data").getAsJsonObject("findAllAlerts").getAsJsonArray("alerts");

        Stream<JsonElement> alertStream = StreamSupport.stream(alerts.spliterator(), true);

        Stream<JsonElement> filteredAlerts = alertStream.
                filter(element -> idAcknowledged == ((JsonObject) element).get("databaseId").getAsLong() && ((JsonObject) element).get("acknowledged").getAsBoolean());

        assertTrue("The alert with id: " + idAcknowledged + " was not acknowledged", filteredAlerts.findAny().isPresent());

    }
}

