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

package org.opennms.horizon.inventory.cucumber.steps;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.hamcrest.Matchers;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.AddressCreateDTO;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MonitoringLocationStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;
    private MonitoringLocationDTO lastMonitoringLocation;
    private String lastLocation;
    private List<MonitoringLocationDTO> lastMonitoringLocations;
    private BoolValue lastDelete;

    public MonitoringLocationStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
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
        backgroundHelper.getMonitoringLocationStub().listLocations(Empty.newBuilder().build()).getLocationsList()
            .forEach(location ->
                backgroundHelper.getMonitoringLocationStub().deleteLocation(Int64Value.of(location.getId())));
    }

    @Then("[MonitoringLocation] Monitoring Location is cleaned up")
    public void monitoringLocationMonitoringLocationIsEmpty() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.listLocations(Empty.newBuilder().build()).getLocationsList().size(),
                Matchers.is(0));
    }

    @When("[MonitoringLocation] Create Monitoring Location with name {string}")
    public void monitoringLocationCreateMonitoringLocation(String location) {
        lastMonitoringLocation = backgroundHelper.getMonitoringLocationStub().createLocation(MonitoringLocationCreateDTO.newBuilder().setLocation(location).setGeoLocation(GeoLocation.newBuilder().setLatitude(1.0).setLongitude(2.0).build()).setTenantId(backgroundHelper.getTenantId()).setAddress(AddressDTO.newBuilder().setId(1L).build()).build());
    }

    @Then("[MonitoringLocation] Monitoring Location is created")
    public void monitoringLocationMonitoringLocationIsCreated() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.listLocations(Empty.newBuilder().build()).getLocationsList().size(),
                Matchers.is(1));
    }

    @When("[MonitoringLocation] Get Monitoring Location by name {string}")
    public void monitoringLocationGetMonitoringLocation(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastLocation = location;
        lastMonitoringLocation = await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.getLocationByName(StringValue.of(location)),
                Matchers.notNullValue());
    }

    @When("[MonitoringLocation] Get Monitoring Location by id")
    public void monitoringLocationGetMonitoringLocation() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastMonitoringLocation = await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.getLocationById(Int64Value.of(lastMonitoringLocation.getId())),
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
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.updateLocation(MonitoringLocationDTO.newBuilder().setId(lastMonitoringLocation.getId()).setLocation(lastLocation).build()),
                Matchers.notNullValue());
    }

    @Then("[MonitoringLocation] Monitoring Location is updated")
    public void monitoringLocationMonitoringLocationIsUpdated() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.getLocationByName(StringValue.of(lastLocation)),
                Matchers.notNullValue());
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.getLocationById(Int64Value.of(lastMonitoringLocation.getId())).getLocation(),
                Matchers.equalTo(lastLocation));
    }

    @When("[MonitoringLocation] Delete Monitoring Location")
    public void monitoringLocationDeleteMonitoringLocation() {
        lastDelete = backgroundHelper.getMonitoringLocationStub().deleteLocation(Int64Value.of(lastMonitoringLocation.getId()));
    }

    @Then("[MonitoringLocation] Monitoring Location is deleted")
    public void monitoringLocationMonitoringLocationIsDeleted() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        assertTrue(lastDelete.getValue());
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.listLocations(Empty.newBuilder().build()).getLocationsList().size(),
                Matchers.is(0));
    }

    @Then("[MonitoringLocation] Monitoring Location is not found")
    public void monitoringLocationMonitoringLocationIsNotFound() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() -> {
                    try {
                        monitoringLocationStub.getLocationById(Int64Value.of(lastMonitoringLocation.getId()));
                    } catch (StatusRuntimeException e) {
                        assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
                        assertEquals("NOT_FOUND: Location with id: " + lastMonitoringLocation.getId() + " doesn't exist.", e.getMessage());
                    }
                }
            );
        findByNameNotFound(monitoringLocationStub, lastMonitoringLocation.getLocation(), "NOT_FOUND: Location with name: " + lastMonitoringLocation.getLocation() + " doesn't exist");
    }

    @When("[MonitoringLocation] List Monitoring Location")
    public void monitoringLocationListMonitoringLocation() {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        lastMonitoringLocations = monitoringLocationStub.listLocations(Empty.newBuilder().build()).getLocationsList();
    }

    @Then("[MonitoringLocation] Nothing is found")
    public void monitoringLocationNothingIsNotFound() {
        assertTrue(lastMonitoringLocations.isEmpty());
    }

    @Then("[MonitoringLocation] Get Monitoring Location by name {string} Not Found")
    public void monitoringLocationGetMonitoringLocationByNameNotFound(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        findByNameNotFound(monitoringLocationStub, location, "NOT_FOUND: Location with name: " + location + " doesn't exist");
    }

    private void findByNameNotFound(MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub monitoringLocationStub, String location, String lastMonitoringLocation1) {
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() -> {
                try {
                    monitoringLocationStub.getLocationByName(StringValue.of(location));
                } catch (StatusRuntimeException e) {
                    assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
                    assertEquals(lastMonitoringLocation1, e.getMessage());
                }
            });
    }

    @Given("[MonitoringLocation] Create address")
    public void monitoringLocationCreateAddress() {
        var address = AddressCreateDTO.newBuilder().setCity("city").setState("state").setCountry("country").setAddressLine1("street").setAddressLine2("").setPostalCode("zipCode").build();
        var addressCreated = backgroundHelper.getAddressServiceBlockingStub().createAddress(address);
        assertNotNull(addressCreated);
    }
}
