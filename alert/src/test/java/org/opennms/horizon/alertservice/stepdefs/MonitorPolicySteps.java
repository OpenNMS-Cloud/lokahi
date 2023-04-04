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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.InvalidProtocolBufferException;
import io.cucumber.java.BeforeAll;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.groups.Tuple;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.shared.alert.policy.ComponentType;
import org.opennms.horizon.shared.alert.policy.EventType;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyList;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;
import org.opennms.horizon.shared.alert.policy.OverTimeUnit;
import org.opennms.horizon.shared.alert.policy.PolicyRuleProto;
import org.opennms.horizon.shared.alert.policy.Severity;
import org.opennms.horizon.shared.alert.policy.TriggerEventProto;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@Slf4j
@RequiredArgsConstructor
public class MonitorPolicySteps {
    private final KafkaTestHelper kafkaTestHelper;
    private final BackgroundSteps background;
    private final AlertGrpcClientUtils grpcClient;
    private MonitorPolicyProto.Builder policyBuilder = MonitorPolicyProto.newBuilder();
    private PolicyRuleProto.Builder ruleBuilder = PolicyRuleProto.newBuilder();
    private TriggerEventProto.Builder triggerBuilder = TriggerEventProto.newBuilder();
    private Long policyId;

    private MonitorPolicyProto policy;

    @Given("Monitoring policy kafka consumer")
    public void setupMonitoringPolicyTopic() {
        kafkaTestHelper.setKafkaBootstrapUrl(background.getKafkaBootstrapUrl());
        kafkaTestHelper.startConsumerAndProducer(background.getMonitoringPolicyTopic(), background.getMonitoringPolicyTopic());
    }


    @Given("Tenant id {string}")
    public void defaultTenantId(String tenantId) {
        grpcClient.setTenantId(tenantId);
    }

    @Given("Monitor policy name {string} and memo {string}")
    public void monitorPolicyNameAndMemo(String name, String memo) {
        policyBuilder
            .setName(name)
            .setMemo(memo);
    }

    @Given("Policy tags")
    public void policyTags(io.cucumber.datatable.DataTable dataTable) {
        List<String> tags = dataTable.asList();
        policyBuilder.addAllTags(tags);
    }

    @Given("Notify by email {string}")
    public void notifyByEmail(String notifyByEmail) {
        policyBuilder.setNotifyByEmail(Boolean.parseBoolean(notifyByEmail));
    }

    @Given("Policy Rule name {string} and componentType {string}")
    public void policyRuleNameAndComponentType(String name, String type) {
        ruleBuilder
            .setName(name)
            .setComponentType(ComponentType.valueOf(type.toUpperCase()));
    }

    @Given("Trigger event {string}, count {int} overtime {int} {string}, severity {string}")
    public void triggerEventCountOvertimeSeverity(String event, Integer count, Integer overTime, String timeUnit, String severity) {
        triggerBuilder.setTriggerEvent(EventType.valueOf(event.toUpperCase()))
            .setCount(count)
            .setOvertime(overTime)
            .setOvertimeUnit(OverTimeUnit.valueOf(timeUnit.toUpperCase()))
            .setSeverity(Severity.valueOf(severity.toUpperCase()));
    }

    @Then("Create a new policy with give parameters")
    public void createANewPolicyWithGiveParameters() {
        MonitorPolicyProto policy = policyBuilder.addRules(
                ruleBuilder.addSnmpEvents(triggerBuilder.build()).build()
        ).build();
        MonitorPolicyProto dbPolicy = grpcClient.getPolicyStub().createPolicy(policy);
        policyId = dbPolicy.getId();
        log.info("Creating policy {}", policy);
        assertThat(dbPolicy).isNotNull();
    }

    @Then("Verify the new policy has been created")
    public void verifyTheNewPolicyHasBeenCreated() {
        MonitorPolicyProto policy = grpcClient.getPolicyStub().getPolicyById(Int64Value.of(policyId));
        assertThat(policy).isNotNull()
            .extracting("name", "memo", "tagsList")
            .containsExactly(policyBuilder.getName(), policyBuilder.getMemo(), policyBuilder.getTagsList());
        assertThat(policy.getRulesList()).asList().hasSize(1)
            .extracting("name", "componentType")
            .containsExactly(Tuple.tuple(ruleBuilder.getName(), ruleBuilder.getComponentType()));
        assertThat(policy.getRulesList().get(0).getSnmpEventsList()).asList().hasSize(1)
            .extracting("triggerEvent", "count", "overtime", "overtimeUnit", "severity", "clearEvent")
            .containsExactly(Tuple.tuple(triggerBuilder.getTriggerEvent(), triggerBuilder.getCount(), triggerBuilder.getOvertime(),
                triggerBuilder.getOvertimeUnit(), triggerBuilder.getSeverity(), EventType.UNKNOWN_EVENT));
    }

    @Then("List policy should contain {int}")
    public void listPolicyShouldContain(int count) {
        MonitorPolicyList list = grpcClient.getPolicyStub().listPolicies(Empty.getDefaultInstance());
        assertThat(list).isNotNull()
            .extracting(MonitorPolicyList::getPoliciesList).asList().hasSize(count);
    }

    @Then("The default monitoring policy exist with name {string} and tag {string} and all notification enabled")
    public void theDefaultMonitoringPolicyExistWithNameAndTag(String policyName, String tag) {
         policy = grpcClient.getPolicyStub().getDefaultPolicy(Empty.getDefaultInstance());
        assertThat(policy).isNotNull()
            .extracting("name", "tagsList", "notifyByEmail", "notifyByPagerDuty", "notifyByWebhooks")
            .containsExactly(policyName, List.of(tag), true, true, true);
    }

    @Then("Verify the default monitoring policy has the following data")
    public void verifyTheDefaultMonitoringPolicyHasTheFollowingData(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        List<TriggerEventProto> events = policy.getRulesList().get(0).getSnmpEventsList();
        assertThat(events).asList().hasSize(3);
        for(int i =0; i < events.size(); i++) {
            assertThat(events.get(i))
                .extracting(e -> e.getTriggerEvent().name(), e -> e.getSeverity().name())
                .containsExactly(rows.get(i).get("triggerEvent"), rows.get(i).get("severity"));
        }
    }

    @Then("Verify the default policy rule has name {string} and component type {string}")
    public void verifyTheDefaultPolicyRuleHasNameAndComponentType(String name, String type) {
        assertThat(policy.getRulesList()).asList().hasSize(1);
        PolicyRuleProto rule = policy.getRulesList().get(0);
        assertThat(rule)
            .extracting(r -> r.getName(), r->r.getComponentType().name())
            .containsExactly(name, type);

    }

    @Then("Verify monitoring policy for tenant {string} is sent to Kafka")
    public void verifyMonitoringPolicyTopic(String tenant) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MonitorPolicyProto> messages = kafkaTestHelper.getConsumedMessages(background.getMonitoringPolicyTopic()).stream().map(messageBytes -> {
                try {
                    return MonitorPolicyProto.parseFrom(messageBytes.value());
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }).filter(proto -> proto.getTenantId().equals(tenant)).toList();

            assertEquals(1, messages.size());
        });
    }
}
