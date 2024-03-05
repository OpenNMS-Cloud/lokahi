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
package org.opennms.horizon.alertservice.stepdefs;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.assertj.core.groups.Tuple;
import org.junit.platform.commons.util.StringUtils;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertConditionProto;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionProto;
import org.opennms.horizon.alerts.proto.EventDefsByVendorRequest;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsRequest;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.MonitorPolicyList;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.OverTimeUnit;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@Slf4j
@RequiredArgsConstructor
public class MonitorPolicySteps {
    private final TenantSteps tenantSteps;
    private final KafkaTestHelper kafkaTestHelper;
    private final BackgroundSteps background;
    private final AlertGrpcClientUtils grpcClient;

    private MonitorPolicyProto.Builder policyBuilder;
    private PolicyRuleProto.Builder ruleBuilder;

    private List<AlertConditionProto.Builder> triggerBuilders = new ArrayList<>();

    private Map<String, AlertEventDefinitionProto> snmpTrapDefinitions;

    private MonitorPolicyProto lastCreatedPolicy;
    private MonitorPolicyProto defaultPolicy;
    private Exception lastException;

    @Given("A monitoring policy named {string} with tag {string}, notifying by email")
    public void defineNewPolicyNotifyViaEmail(String name, String tag) {
        defineNewPolicy(name, tag);
        policyBuilder.setNotifyByEmail(true);
    }

    @Given("A monitoring policy named {string} with tag {string}")
    public void defineNewPolicy(String name, String tag) {
        defineNewPolicy(name);
        policyBuilder.addTags(tag);

        ruleBuilder = PolicyRuleProto.newBuilder();
        triggerBuilders = new ArrayList<>();
    }

    @Given("A monitoring policy named {string}")
    public void defineNewPolicy(String name) {
        policyBuilder = MonitorPolicyProto.newBuilder().setName(name);

        ruleBuilder = PolicyRuleProto.newBuilder();
        triggerBuilders = new ArrayList<>();
    }

    @Given("The policy has a rule named {string} with component type {string} and trap definitions")
    public void setPolicyRules(String ruleName, String type, DataTable alertConditions) {
        ruleBuilder = PolicyRuleProto.newBuilder()
                .setName(ruleName)
                .setComponentType(ManagedObjectType.valueOf(type.toUpperCase()));

        snmpTrapDefinitions = this.loadSnmpTrapDefinitions();

        alertConditions.asMaps().stream()
                .map(map -> {
                    AlertConditionProto.Builder eventBuilder = AlertConditionProto.newBuilder()
                            .setTriggerEvent(snmpTrapDefinitions.get(map.get("trigger_event_name")))
                            .setCount(Integer.parseInt(map.get("count")))
                            .setOvertime(Integer.parseInt(map.get("overtime")))
                            .setOvertimeUnit(OverTimeUnit.valueOf(
                                    map.get("overtime_unit").toUpperCase()))
                            .setSeverity(Severity.valueOf(map.get("severity").toUpperCase()));
                    String clearEventType = map.get("clear_event_name");
                    if (StringUtils.isNotBlank(clearEventType)) {
                        eventBuilder.setClearEvent(snmpTrapDefinitions.get(clearEventType));
                    }
                    return eventBuilder;
                })
                .forEach(triggerBuilders::add);
    }

    @Given("The policy has a simple rule named {string} with component type {string}")
    public void addPolicyRules(String ruleName, String type) {
        policyBuilder.addRules(PolicyRuleProto.newBuilder()
                .setName(ruleName)
                .setComponentType(ManagedObjectType.valueOf(type.toUpperCase()))
                .build());
    }

    private Map<String, AlertEventDefinitionProto> loadSnmpTrapDefinitions() {
        ListAlertEventDefinitionsRequest request = ListAlertEventDefinitionsRequest.newBuilder()
                .setEventType(EventType.SNMP_TRAP)
                .build();
        List<AlertEventDefinitionProto> eventDefinitionsList = this.grpcClient
                .getAlertEventDefinitionStub()
                .listAlertEventDefinitions(request)
                .getAlertEventDefinitionsList();
        return eventDefinitionsList.stream()
                .filter(proto -> Strings.isNotBlank(proto.getName()))
                .collect(Collectors.toMap(AlertEventDefinitionProto::getName, Function.identity()));
    }

    @And("The policy is created in the tenant")
    public void thePolicyIsCreatedInANewTenant() {
        triggerBuilders.stream().map(AlertConditionProto.Builder::build).forEach(ruleBuilder::addSnmpEvents);

        MonitorPolicyProto policy = policyBuilder.addRules(ruleBuilder.build()).build();

        try {
            log.info("Creating policy {}", policy);
            lastCreatedPolicy = grpcClient.getPolicyStub().createPolicy(policy);
            assertThat(lastCreatedPolicy).isNotNull();
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("Verify the new policy has been created")
    public void verifyTheNewPolicyHasBeenCreated() {
        AlertEventDefinitionProto triggerEventDefinition =
                triggerBuilders.stream().findFirst().orElseThrow().getTriggerEvent();

        AlertConditionProto.Builder triggerBuilder = triggerBuilders.stream()
                .filter(b -> b.getTriggerEvent().equals(triggerEventDefinition))
                .findFirst()
                .orElseThrow();
        MonitorPolicyProto policy = grpcClient.getPolicyStub().getPolicyById(Int64Value.of(lastCreatedPolicy.getId()));
        assertThat(policy)
                .isNotNull()
                .extracting("name", "memo", "tagsList")
                .containsExactly(policyBuilder.getName(), policyBuilder.getMemo(), policyBuilder.getTagsList());
        assertThat(policy.getRulesList())
                .asList()
                .hasSize(1)
                .extracting("name", "componentType")
                .containsExactly(Tuple.tuple(ruleBuilder.getName(), ruleBuilder.getComponentType()));
        assertThat(policy.getRulesList().get(0).getSnmpEventsList())
                .asList()
                .hasSize(1)
                .extracting("triggerEvent", "count", "overtime", "overtimeUnit", "severity", "clearEvent")
                .containsExactly(Tuple.tuple(
                        triggerBuilder.getTriggerEvent(),
                        triggerBuilder.getCount(),
                        triggerBuilder.getOvertime(),
                        triggerBuilder.getOvertimeUnit(),
                        triggerBuilder.getSeverity(),
                        triggerBuilder.getClearEvent()));
    }

    @Then("List policy should contain {int}")
    public void listPolicyShouldContain(int count) {
        MonitorPolicyList list = grpcClient.getPolicyStub().listPolicies(Empty.getDefaultInstance());
        assertThat(list)
                .isNotNull()
                .extracting(MonitorPolicyList::getPoliciesList)
                .asList()
                .hasSize(count);
    }

    @Then("The default monitoring policy exist with name {string} and all notification enabled")
    public void theDefaultMonitoringPolicyExistWithNameAndTag(String policyName) {
        defaultPolicy = grpcClient.getPolicyStub().getDefaultPolicy(Empty.getDefaultInstance());
        assertThat(defaultPolicy)
                .isNotNull()
                .extracting("name", "notifyByEmail", "notifyByPagerDuty", "notifyByWebhooks")
                .containsExactly(policyName, true, true, true);
    }

    @Then("Verify the default monitoring policy has the following data")
    public void verifyTheDefaultMonitoringPolicyHasTheFollowingData(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        List<AlertConditionProto> events = defaultPolicy.getRulesList().get(0).getSnmpEventsList();
        assertThat(events).asList().hasSize(rows.size());
        for (int i = 0; i < events.size(); i++) {
            assertThat(events.get(i))
                    .extracting(e -> e.getTriggerEvent().getName(), e -> e.getSeverity()
                            .name())
                    .containsExactly(
                            rows.get(i).get("triggerEventName"), rows.get(i).get("severity"));
        }
    }

    @Then("Verify the default policy rule has name {string} and component type {string}")
    public void verifyTheDefaultPolicyRuleHasNameAndComponentType(String name, String type) {
        assertThat(defaultPolicy.getRulesList()).asList().hasSize(1);
        PolicyRuleProto rule = defaultPolicy.getRulesList().get(0);
        assertThat(rule)
                .extracting(PolicyRuleProto::getName, r -> r.getComponentType().name())
                .containsExactly(name, type);
    }

    @Then("Verify monitoring policy for tenant {string} is sent to Kafka")
    public void verifyMonitoringPolicyTopic(String tenant) {
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MonitorPolicyProto> messages =
                    kafkaTestHelper.getConsumedMessages(background.getMonitoringPolicyTopic()).stream()
                            .map(messageBytes -> {
                                try {
                                    return MonitorPolicyProto.parseFrom(messageBytes.value());
                                } catch (InvalidProtocolBufferException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .filter(proto -> proto.getTenantId().equals(tenant))
                            .toList();
            assertTrue(messages.size() >= 1);
        });
    }

    @Then("Verify valid monitoring policy ID is set in alert for the tenant")
    public void checkMonitoringPolicyIdSet() {
        checkMonitoringPolicyIdSet(tenantSteps.getTenantId());
    }

    @Then("Delete policy named {string}")
    public void deletePolicyNamed(String policyName) {
        Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var policies =
                    grpcClient.getPolicyStub().listPolicies(Empty.newBuilder().build()).getPoliciesList().stream()
                            .filter(p -> policyName.equals(p.getName()))
                            .toList();

            assertEquals(1, policies.size());
            assertTrue(grpcClient
                    .getPolicyStub()
                    .deletePolicyById(Int64Value.of(policies.get(0).getId()))
                    .getValue());
        });
    }

    @Then("Delete policy rule named {string} under policy named {string}")
    public void deletePolicyRuleNamedUnderPolicyNamed(String ruleName, String policyName) {
        Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<PolicyRuleProto> rulesMatched = new ArrayList<>();
            grpcClient
                    .getPolicyStub()
                    .listPolicies(Empty.newBuilder().build())
                    .getPoliciesList()
                    .forEach(p -> {
                        if (!policyName.equals(p.getName())) {
                            return;
                        }
                        var filtered = p.getRulesList().stream()
                                .filter(r -> ruleName.equals(r.getName()))
                                .toList();
                        rulesMatched.addAll(filtered);
                    });

            assertEquals(1, rulesMatched.size());
            assertTrue(grpcClient
                    .getPolicyStub()
                    .deleteRuleById(Int64Value.of(rulesMatched.get(0).getId()))
                    .getValue());
        });
    }

    @Then("Verify valid monitoring policy ID is set in alert for tenant {string}")
    public void checkMonitoringPolicyIdSet(String tenantId) {
        Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = filterMessagesForTenant(tenantId, alert -> alert.getMonitoringPolicyIdCount() >= 1);
            assertEquals(1, alerts.size());

            assertNotNull(grpcClient
                    .getPolicyStub()
                    .getPolicyById(Int64Value.of(alerts.get(0).getMonitoringPolicyId(0))));
        });
    }

    public List<Alert> filterMessagesForTenant(String tenant, Predicate<Alert> predicate) {
        return kafkaTestHelper.getConsumedMessages(background.getAlertTopic()).stream()
                .map(b -> {
                    try {
                        return Alert.parseFrom(b.value());
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(predicate)
                .toList();
    }

    @Then("Verify exception {string} thrown with message {string}")
    public void monitoringLocationVerifyException(String exceptionName, String message) {
        if (lastException == null) {
            fail("No exception caught");
        } else {
            assertThat(lastException.getClass().getSimpleName()).isEqualTo(exceptionName);
            assertThat(lastException.getMessage()).isEqualTo(message);
        }
    }

    @Then("Validate whether we have loaded all event definitions of size greater than or equal to {int}")
    public void validateWhetherWeHaveLoadedAllEventDefinitions(int count) {

        ListAlertEventDefinitionsRequest request = ListAlertEventDefinitionsRequest.newBuilder()
                .setEventType(EventType.SNMP_TRAP)
                .build();
        List<AlertEventDefinitionProto> eventDefinitionsList = this.grpcClient
                .getAlertEventDefinitionStub()
                .listAlertEventDefinitions(request)
                .getAlertEventDefinitionsList();
        assertThat(eventDefinitionsList.size()).isGreaterThanOrEqualTo(count);
    }

    @Then("Validate whether we can load vendors of size greater than or equal to {int}")
    public void validateWhetherWeCanLoadVendorsOfSizeGreaterThanOrEqualTo(int vendorCount) {

        var vendorList = this.grpcClient.getAlertEventDefinitionStub().listVendors(Empty.getDefaultInstance());

        System.out.println("Vendor list size " + vendorList.getVendorList().size());
        assertThat(vendorList.getVendorList().size()).isGreaterThanOrEqualTo(vendorCount);
    }

    @Then("Fetch event defs for vendor {string} and verify size is greater than or equal to {int}")
    public void fetchEventDefsForVendorAndVerifySizeIsGreaterThanOrEqualTo(String vendor, int size) {
        EventDefsByVendorRequest request = EventDefsByVendorRequest.newBuilder()
                .setEventType(EventType.SNMP_TRAP)
                .setVendor(vendor)
                .build();
        var eventDefinitionsByVendor =
                this.grpcClient.getAlertEventDefinitionStub().listAlertEventDefinitionsByVendor(request);
        assertThat(eventDefinitionsByVendor.getEventDefinitionList().size()).isGreaterThanOrEqualTo(size);
    }

    @Then("Fetch event defs for event type {string} and verify size is greater than or equal to {int}")
    public void fetchEventDefsForEventTypeAndVerifySizeIsGreaterThanOrEqualTo(String eventType, int size) {
        EventDefsByVendorRequest request = EventDefsByVendorRequest.newBuilder()
                .setEventType(EventType.valueOf(eventType))
                .build();
        var eventDefsByType = this.grpcClient.getAlertEventDefinitionStub().listAlertEventDefinitionsByVendor(request);
        assertThat(eventDefsByType.getEventDefinitionCount()).isGreaterThanOrEqualTo(size);
    }
}
