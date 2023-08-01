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

package org.opennms.horizon.alertservice.stepdefs;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.groups.Tuple;
import org.junit.platform.commons.util.StringUtils;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertConditionProto;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionProto;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.*;

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
        policyBuilder = MonitorPolicyProto.newBuilder()
            .setName(name);

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
                    .setOvertimeUnit(OverTimeUnit.valueOf(map.get("overtime_unit").toUpperCase()))
                    .setSeverity(Severity.valueOf(map.get("severity").toUpperCase()));
                String clearEventType = map.get("clear_event_name");
                if (StringUtils.isNotBlank(clearEventType)) {
                    eventBuilder.setClearEvent(snmpTrapDefinitions.get(clearEventType));
                }
                return eventBuilder;
            })
            .forEach(triggerBuilders::add);
    }

    private Map<String, AlertEventDefinitionProto> loadSnmpTrapDefinitions() {
        ListAlertEventDefinitionsRequest request = ListAlertEventDefinitionsRequest.newBuilder()
            .setEventType(EventType.SNMP_TRAP)
            .build();
        List<AlertEventDefinitionProto> eventDefinitionsList = this.grpcClient.getAlertEventDefinitionStub()
            .listAlertEventDefinitions(request)
            .getAlertEventDefinitionsList();
        return eventDefinitionsList.stream()
            .collect(Collectors.toMap(AlertEventDefinitionProto::getName, Function.identity()));
    }

    @And("The policy is created in the tenant")
    public void thePolicyIsCreatedInANewTenant() {
        triggerBuilders.stream()
            .map(AlertConditionProto.Builder::build)
            .forEach(ruleBuilder::addSnmpEvents);

        MonitorPolicyProto policy = policyBuilder
            .addRules(ruleBuilder.build())
            .build();

        log.info("Creating policy {}", policy);
        lastCreatedPolicy = grpcClient.getPolicyStub().createPolicy(policy);
        assertThat(lastCreatedPolicy).isNotNull();
    }

    @Then("Verify the new policy has been created")
    public void verifyTheNewPolicyHasBeenCreated() {
        AlertEventDefinitionProto triggerEventDefinition = triggerBuilders.stream()
            .findFirst()
            .orElseThrow()
            .getTriggerEvent();

        AlertConditionProto.Builder triggerBuilder = triggerBuilders.stream()
            .filter(b -> b.getTriggerEvent().equals(triggerEventDefinition))
            .findFirst().orElseThrow();
        MonitorPolicyProto policy = grpcClient.getPolicyStub().getPolicyById(Int64Value.of(lastCreatedPolicy.getId()));
        assertThat(policy).isNotNull()
            .extracting("name", "memo", "tagsList")
            .containsExactly(policyBuilder.getName(), policyBuilder.getMemo(), policyBuilder.getTagsList());
        assertThat(policy.getRulesList()).asList().hasSize(1)
            .extracting("name", "componentType")
            .containsExactly(Tuple.tuple(ruleBuilder.getName(), ruleBuilder.getComponentType()));
        assertThat(policy.getRulesList().get(0).getSnmpEventsList()).asList().hasSize(1)
            .extracting("triggerEvent", "count", "overtime", "overtimeUnit", "severity", "clearEvent")
            .containsExactly(Tuple.tuple(triggerBuilder.getTriggerEvent(), triggerBuilder.getCount(), triggerBuilder.getOvertime(),
                triggerBuilder.getOvertimeUnit(), triggerBuilder.getSeverity(), triggerBuilder.getClearEvent()));
    }

    @Then("List policy should contain {int}")
    public void listPolicyShouldContain(int count) {
        MonitorPolicyList list = grpcClient.getPolicyStub().listPolicies(Empty.getDefaultInstance());
        assertThat(list).isNotNull()
            .extracting(MonitorPolicyList::getPoliciesList).asList().hasSize(count);
    }

    @Then("The default monitoring policy exist with name {string} and all notification enabled")
    public void theDefaultMonitoringPolicyExistWithNameAndTag(String policyName) {
        defaultPolicy = grpcClient.getPolicyStub().getDefaultPolicy(Empty.getDefaultInstance());
        assertThat(defaultPolicy).isNotNull()
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
                .extracting(e -> e.getTriggerEvent().getName(), e -> e.getSeverity().name())
                .containsExactly(rows.get(i).get("triggerEventName"), rows.get(i).get("severity"));
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
            List<MonitorPolicyProto> messages = kafkaTestHelper.getConsumedMessages(background.getMonitoringPolicyTopic()).stream().map(messageBytes -> {
                try {
                    return MonitorPolicyProto.parseFrom(messageBytes.value());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }).filter(proto -> proto.getTenantId().equals(tenant)).toList();
            assertTrue(messages.size() >= 1);
        });
    }

    @Then("Verify valid monitoring policy ID is set in alert for the tenant")
    public void checkMonitoringPolicyIdSet() {
        checkMonitoringPolicyIdSet(tenantSteps.getTenantId());
    }

    @Then("Verify valid monitoring policy ID is set in alert for tenant {string}")
    public void checkMonitoringPolicyIdSet(String tenantId) {
        Awaitility.waitAtMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Alert> alerts = filterMessagesForTenant(tenantId, alert -> alert.getMonitoringPolicyIdCount() >= 1);
            assertEquals(1, alerts.size());

            assertNotNull(grpcClient.getPolicyStub().getPolicyById(Int64Value.of(alerts.get(0).getMonitoringPolicyId(0))));
        });
    }

    public List<Alert> filterMessagesForTenant(String tenant, Predicate<Alert> predicate) {
        return kafkaTestHelper.getConsumedMessages(background.getAlertTopic()).stream().map(b -> {
            try {
                return Alert.parseFrom(b.value());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }).filter(predicate).toList();

    }
}
