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
package org.opennms.horizon.server.mapper.alert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alerts.proto.*;
import org.opennms.horizon.alerts.proto.AlertConditionProto;
import org.opennms.horizon.server.model.alerts.AlertCondition;
import org.opennms.horizon.server.model.alerts.AlertEventDefinition;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.server.model.alerts.PolicyRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MonitorPolicyMapperTest {
    @Autowired
    private MonitorPolicyMapper mapper;

    private MonitorPolicyProto policyProto;

    @BeforeEach
    void before() {
        AlertEventDefinitionProto triggerEvent = AlertEventDefinitionProto.newBuilder()
                .setId(1L)
                .setName("SNMP Warm Start")
                .setEventType(EventType.SNMP_TRAP)
                .build();
        AlertConditionProto alertCondition = AlertConditionProto.newBuilder()
                .setTriggerEvent(triggerEvent)
                .setCount(1)
                .setSeverity(Severity.CRITICAL)
                .build();
        PolicyRuleProto rule = PolicyRuleProto.newBuilder()
                .setName("test-rule")
                .setComponentType(ManagedObjectType.NODE)
                .setDetectionMethod(DetectionMethod.EVENT)
                .setEventType(EventType.SNMP_TRAP)
                .setThresholdMetricName("threshold-metric")
                .addSnmpEvents(alertCondition)
                .build();
        policyProto = MonitorPolicyProto.newBuilder()
                .setName("test-policy")
                .setMemo("test mapper")
                .setNotifyByEmail(true)
                .setNotifyInstruction("sample notify")
                .addTags("junit")
                .addTags("test")
                .addRules(rule)
                .build();
    }

    @Test
    void testProtoToEntity() {
        MonitorPolicy policy = mapper.map(policyProto);
        assertThat(policy)
                .isNotNull()
                .extracting(
                        MonitorPolicy::getName,
                        MonitorPolicy::getMemo,
                        p -> p.getTags().size(),
                        p -> p.getRules().size(),
                        MonitorPolicy::getNotifyByEmail,
                        MonitorPolicy::getNotifyByPagerDuty,
                        MonitorPolicy::getNotifyByWebhooks,
                        MonitorPolicy::getNotifyInstruction)
                .containsExactly(
                        policyProto.getName(),
                        policyProto.getMemo(),
                        policyProto.getTagsList().size(),
                        policyProto.getRulesList().size(),
                        policyProto.getNotifyByEmail(),
                        policyProto.getNotifyByPagerDuty(),
                        policyProto.getNotifyByWebhooks(),
                        policyProto.getNotifyInstruction());
        assertThat(policy.getTags()).isEqualTo(policyProto.getTagsList()); // the order doesn't matter here

        PolicyRule policyRule = policy.getRules().get(0);
        assertThat(policyRule)
                .extracting(
                        PolicyRule::getName,
                        PolicyRule::getComponentType,
                        PolicyRule::getDetectionMethod,
                        PolicyRule::getEventType,
                        PolicyRule::getThresholdMetricName,
                        r -> r.getAlertConditions().size())
                .containsExactly(
                        "test-rule",
                        ManagedObjectType.NODE,
                        DetectionMethod.EVENT,
                        EventType.SNMP_TRAP,
                        "threshold-metric",
                        1);

        AlertCondition alertCondition = policyRule.getAlertConditions().get(0);
        assertThat(alertCondition)
                .extracting(
                        AlertCondition::getCount,
                        AlertCondition::getOvertime,
                        AlertCondition::getOvertimeUnit,
                        AlertCondition::getSeverity,
                        AlertCondition::getClearEvent)
                .containsExactly(1, 0, OverTimeUnit.UNKNOWN_UNIT.name(), Severity.CRITICAL.name(), null);

        AlertEventDefinition triggerEvent = alertCondition.getTriggerEvent();
        assertThat(triggerEvent)
                .extracting(
                        AlertEventDefinition::getId, AlertEventDefinition::getName, AlertEventDefinition::getEventType)
                .containsExactly(1L, "SNMP Warm Start", EventType.SNMP_TRAP);
    }

    @Test
    void testEntityToProto() {
        MonitorPolicy entity = mapper.map(policyProto);
        MonitorPolicyProto result = mapper.map(entity);
        assertThat(result).usingRecursiveComparison().isEqualTo(policyProto);
    }
}
