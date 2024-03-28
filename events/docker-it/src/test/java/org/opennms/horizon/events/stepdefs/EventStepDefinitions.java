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
package org.opennms.horizon.events.stepdefs;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.Empty;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.grpc.StatusRuntimeException;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.horizon.events.EventsBackgroundHelper;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;

@RequiredArgsConstructor
public class EventStepDefinitions {
    private final EventsBackgroundHelper backgroundHelper;

    private NodeDTO node;

    @Given("[Event] External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[Event] Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[Event] Create Grpc Connection for Events")
    public void createGrpcConnectionForEvents() {
        backgroundHelper.createGrpcConnectionForEvents();
    }

    @Then("verify there are {int} events")
    public void verifyThereAreEvents(int arg0) {
        if (arg0 < 0) {
            fail("Events can't be in negative");
        } else {
            assertEquals(arg0, backgroundHelper.getEventCount());
        }
    }

    @When("Send Trap Data to Kafka Listener via Producer with TenantId {string} and Location {string}")
    public void sendTrapDataToKafkaListenerViaProducerWithTenantIdAndLocationId(String tenantId, String location) {
        var locationServiceBlockingStub = backgroundHelper.getMonitoringLocationStub();
        String locationId = "";
        try {
            locationId =
                    locationServiceBlockingStub.listLocations(Empty.newBuilder().build()).getLocationsList().stream()
                            .filter(loc -> location.equals(loc.getLocation()))
                            .findFirst()
                            .map(MonitoringLocationDTO::getId)
                            .map(String::valueOf)
                            .orElseThrow(() -> new IllegalArgumentException("Location " + location + " not found"));
            ;
        } catch (StatusRuntimeException e) {
            // catch duplicate location
        }
        TenantLocationSpecificTrapLogDTO tenantLocationSpecificTrapLogDTO =
                TenantLocationSpecificTrapLogDTO.newBuilder()
                        .setLocationId(locationId)
                        .setTenantId(tenantId)
                        .build();
        var producerRecord = new ProducerRecord<String, byte[]>(
                backgroundHelper.getTopic(), tenantLocationSpecificTrapLogDTO.toByteArray());

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getBootstrapServer());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            kafkaProducer.send(producerRecord);
        }
    }

    @Then("Check If There are {int} Events with Location {string}")
    public void checkIfThereAreEventsWithLocation(int eventsCount, String location) {
        backgroundHelper.searchEventWithLocation(eventsCount, Integer.getInteger(node.getId() + ""), location);
    }

    @Given("Initialize Trap Producer With Topic {string} and BootstrapServer {string}")
    public void initializeTrapProducerWithTopicAndBootstrapServer(String topic, String bootstrapServer) {
        backgroundHelper.initializeTrapProducer(topic, bootstrapServer);
    }

    @Given("[Common] Create {string} Location")
    public void createLocation(String location) {
        var locationServiceBlockingStub = backgroundHelper.getMonitoringLocationStub();
        try {
            var locationDto = locationServiceBlockingStub.createLocation(MonitoringLocationCreateDTO.newBuilder()
                    .setLocation(location)
                    .build());
            assertNotNull(locationDto);
        } catch (StatusRuntimeException e) {
            // catch duplicate location
        }
    }

    @When("Add a device with IP address = {string} with label {string} and location {string}")
    public void addADeviceWithIPAddressWithLabelAndLocation(String ipAddress, String label, String location) {
        var locationServiceBlockingStub = backgroundHelper.getMonitoringLocationStub();
        String locationId = "";
        try {
            locationId =
                    locationServiceBlockingStub.listLocations(Empty.newBuilder().build()).getLocationsList().stream()
                            .filter(loc -> location.equals(loc.getLocation()))
                            .findFirst()
                            .map(MonitoringLocationDTO::getId)
                            .map(String::valueOf)
                            .orElseThrow(() -> new IllegalArgumentException("Location " + location + " not found"));
            ;
        } catch (StatusRuntimeException e) {
            // catch duplicate location
        }
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder()
                .setLabel(label)
                .setLocationId(locationId)
                .setManagementIp(ipAddress)
                .build());
        assertNotNull(node);
    }
}
