/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.cucumber.steps;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;

public class MonitoringLocationStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;
    private MonitoringLocationDTO lastMonitoringLocation;
    private String lastLocation;
    private List<MonitoringLocationDTO> lastMonitoringLocations;
    private BoolValue lastDelete;
    private Exception lastException;

    public MonitoringLocationStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    @Given("[MonitoringLocation] Grpc location {string}")
    public void grpcLocation(String location) {
        // backgroundHelper.grpcLocationId(location);
    }

    @Given("[MonitoringLocation] External GRPC Port in system property {string}")
    public void monitoringLocationExternalGRPCPortInSystemProperty(String systemPropertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(systemPropertyName);
    }

    @Given("[MonitoringLocation] Kafka Bootstrap URL in system property {string}")
    public void monitoringLocationKafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[MonitoringLocation] Grpc TenantId {string}")
    public void monitoringLocationGrpcTenantId(String systemPropertyName) {
        backgroundHelper.grpcTenantId(systemPropertyName);
    }

    @Given("[MonitoringLocation] Create Grpc Connection for Inventory")
    public void monitoringLocationCreateGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    @When("[MonitoringLocation] Clean up Monitoring Location")
    public void monitoringLocationCleanUpMonitoringLocation() {
        backgroundHelper
                .getMonitoringLocationStub()
                .listLocations(Empty.newBuilder().build())
                .getLocationsList()
                .forEach(location ->
                        backgroundHelper.getMonitoringLocationStub().deleteLocation(Int64Value.of(location.getId())));
    }

    @Then("[MonitoringLocation] Monitoring Location is cleaned up")
    public void monitoringLocationMonitoringLocationIsEmpty() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub
                                .listLocations(Empty.newBuilder().build())
                                .getLocationsList()
                                .size(),
                        Matchers.is(0));
    }

    @Then("[MonitoringLocation] verify exception {string} thrown with message {string}")
    public void monitoringLocationVerifyException(String exceptionName, String message) {
        if (lastException == null) {
            fail("No exception caught");
        } else {
            Assertions.assertEquals(exceptionName, lastException.getClass().getSimpleName(), "Exception mismatch");
            Assertions.assertEquals(message, lastException.getMessage());
        }
    }

    @When("[MonitoringLocation] Create Monitoring Location with name {string}")
    public void monitoringLocationCreateMonitoringLocation(String location) {
        try {
            lastMonitoringLocation = backgroundHelper
                    .getMonitoringLocationStub()
                    .createLocation(MonitoringLocationCreateDTO.newBuilder()
                            .setLocation(location)
                            .setGeoLocation(
                                    GeoLocation.newBuilder().setLatitude(1.0).setLongitude(2.0))
                            .setTenantId(backgroundHelper.getTenantId())
                            .setAddress("address")
                            .build());
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("[MonitoringLocation] Monitoring Location is created")
    public void monitoringLocationMonitoringLocationIsCreated() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub
                                .listLocations(Empty.newBuilder().build())
                                .getLocationsList()
                                .size(),
                        Matchers.is(1));
    }

    @When("[MonitoringLocation] Get Monitoring Location by name {string}")
    public void monitoringLocationGetMonitoringLocation(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastLocation = location;
        lastMonitoringLocation = await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub.getLocationByName(StringValue.of(location)),
                        Matchers.notNullValue());
    }

    @When("[MonitoringLocation] Get Monitoring Location by id")
    public void monitoringLocationGetMonitoringLocation() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastMonitoringLocation = await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub.getLocationById(Int64Value.of(lastMonitoringLocation.getId())),
                        Matchers.notNullValue());
    }

    @Then("[MonitoringLocation] Monitoring Location is returned")
    public void monitoringLocationMonitoringLocationIsReturned() {
        assertEquals(backgroundHelper.getTenantId(), lastMonitoringLocation.getTenantId());
        assertEquals(lastLocation, lastMonitoringLocation.getLocation());
    }

    @When("[MonitoringLocation] Update Monitoring Location with name {string}")
    public void monitoringLocationUpdateMonitoringLocation(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastLocation = location;
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub.updateLocation(MonitoringLocationDTO.newBuilder()
                                .setId(lastMonitoringLocation.getId())
                                .setLocation(lastLocation)
                                .build()),
                        Matchers.notNullValue());
    }

    @Then("[MonitoringLocation] Monitoring Location is updated")
    public void monitoringLocationMonitoringLocationIsUpdated() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub.getLocationByName(StringValue.of(lastLocation)),
                        Matchers.notNullValue());
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub
                                .getLocationById(Int64Value.of(lastMonitoringLocation.getId()))
                                .getLocation(),
                        Matchers.equalTo(lastLocation));
    }

    @When("[MonitoringLocation] Delete Monitoring Location")
    public void monitoringLocationDeleteMonitoringLocation() {
        lastDelete = backgroundHelper
                .getMonitoringLocationStub()
                .deleteLocation(Int64Value.of(lastMonitoringLocation.getId()));
    }

    @Then("[MonitoringLocation] Monitoring Location is deleted")
    public void monitoringLocationMonitoringLocationIsDeleted() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        assertTrue(lastDelete.getValue());
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub
                                .listLocations(Empty.newBuilder().build())
                                .getLocationsList()
                                .size(),
                        Matchers.is(0));
    }

    @Then("[MonitoringLocation] Monitoring Location is not found")
    public void monitoringLocationMonitoringLocationIsNotFound() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
                .pollDelay(10L, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        monitoringLocationStub.getLocationById(Int64Value.of(lastMonitoringLocation.getId()));
                    } catch (StatusRuntimeException e) {
                        assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
                        assertEquals(
                                "NOT_FOUND: Location with id: " + lastMonitoringLocation.getId() + " doesn't exist.",
                                e.getMessage());
                    }
                });
        findByNameNotFound(
                monitoringLocationStub,
                lastMonitoringLocation.getLocation(),
                "NOT_FOUND: Location with name: " + lastMonitoringLocation.getLocation() + " doesn't exist");
    }

    @When("[MonitoringLocation] List Monitoring Location")
    public void monitoringLocationListMonitoringLocation() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastMonitoringLocations =
                monitoringLocationStub.listLocations(Empty.newBuilder().build()).getLocationsList();
    }

    @Then("[MonitoringLocation] Nothing is found")
    public void monitoringLocationNothingIsNotFound() {
        assertTrue(lastMonitoringLocations.isEmpty());
    }

    @Then("[MonitoringLocation] Get Monitoring Location by name {string} Not Found")
    public void monitoringLocationGetMonitoringLocationByNameNotFound(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        findByNameNotFound(
                monitoringLocationStub, location, "NOT_FOUND: Location with name: " + location + " doesn't exist");
    }

    private void findByNameNotFound(
            MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub monitoringLocationStub,
            String location,
            String lastMonitoringLocation1) {
        await().pollInterval(5, TimeUnit.SECONDS)
                .pollDelay(10L, TimeUnit.MILLISECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        monitoringLocationStub.getLocationByName(StringValue.of(location));
                    } catch (StatusRuntimeException e) {
                        assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
                        assertEquals(lastMonitoringLocation1, e.getMessage());
                    }
                });
    }
}
