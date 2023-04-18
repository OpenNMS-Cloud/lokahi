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

package org.opennms.horizon.server.mapper.alert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.OverTimeUnit;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.server.model.alerts.PolicyRule;
import org.opennms.horizon.server.model.alerts.TriggerEvent;
import org.opennms.horizon.alerts.proto.TriggerEventProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MonitorPolicyMapperTest {
    @Autowired
    private MonitorPolicyMapper mapper;
    private MonitorPolicyProto policyProto;

    @BeforeEach
    void before() {
        TriggerEventProto triggerEvent = TriggerEventProto.newBuilder()
            .setTriggerEvent(EventType.SNMP_Warm_Start)
            .setCount(1)
            .setSeverity(Severity.CRITICAL)
            .build();
        PolicyRuleProto rule = PolicyRuleProto.newBuilder()
            .setName("test-rule")
            .setComponentType(ManagedObjectType.NODE)
            .addSnmpEvents(triggerEvent)
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
        assertThat(policy).isNotNull()
            .extracting(MonitorPolicy::getName, MonitorPolicy::getMemo, p -> p.getTags().size(), p -> p.getRules().size(),
                MonitorPolicy::getNotifyByEmail, MonitorPolicy::getNotifyByPagerDuty, MonitorPolicy::getNotifyByWebhooks,
                MonitorPolicy::getNotifyInstruction)
            .containsExactly(policyProto.getName(), policyProto.getMemo(), policyProto.getTagsList().size(), policyProto.getRulesList().size(),
                policyProto.getNotifyByEmail(), policyProto.getNotifyByPagerDuty(), policyProto.getNotifyByWebhooks(), policyProto.getNotifyInstruction());
        assertThat(policy.getTags()).isEqualTo(policyProto.getTagsList()); //the order doesn't matter here
        assertThat(policy.getRules().get(0))
            .extracting(PolicyRule::getName, PolicyRule::getComponentType, r -> r.getTriggerEvents().size())
            .containsExactly("test-rule", ManagedObjectType.NODE.name(), 1);
        assertThat(policy.getRules().get(0).getTriggerEvents().get(0))
            .extracting(TriggerEvent::getTriggerEvent, TriggerEvent::getCount, TriggerEvent::getOvertime, TriggerEvent::getOvertimeUnit,
                TriggerEvent::getSeverity, TriggerEvent::getClearEvent)
            .containsExactly(EventType.SNMP_Warm_Start.name(), 1, 0, OverTimeUnit.UNKNOWN_UNIT.name(), Severity.CRITICAL.name(), EventType.UNKNOWN_EVENT.name());
    }

    @Test
    void testEntityToProto() {
        MonitorPolicy entity = mapper.map(policyProto);
        MonitorPolicyProto result = mapper.map(entity);
        assertThat(result).usingRecursiveComparison().isEqualTo(policyProto);
    }
}
