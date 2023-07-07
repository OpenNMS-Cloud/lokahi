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

package org.opennms.horizon.alertservice.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alerts.proto.AlertConditionProto;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled // For developer test only,
// comment out  @PostUpdate @PostPersist on MonitorPolicyProducer
// Also enable commented mapper impl in test
public class MonitoringPolicyRepositoryTest {

    @Autowired
    private MonitorPolicyRepository repository;

    @Autowired
    private TagRepository tagRepository;

    private MonitorPolicyMapper monitorPolicyMapper;



    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("alerts").withUsername("alerts")
        .withPassword("password").withExposedPorts(5432);
    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setup() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }

    @Test
    @Transactional
    public void testPersistence() {

        monitorPolicyMapper = new MonitorPolicyMapperImpl();
      /*  PolicyRuleMapper policyRuleMapper = new PolicyRuleMapperImpl();
        TriggerEventMapper triggerEventMapper = new TriggerEventMapperImpl();
        ReflectionTestUtils.setField(policyRuleMapper, "triggerEventMapper", triggerEventMapper);
        ReflectionTestUtils.setField(monitorPolicyMapper, "policyRuleMapper", policyRuleMapper);*/

        var policy = createNewPolicy(monitorPolicyMapper);
        MonitorPolicy policyCreated1 = repository.save(policy);
        Assertions.assertNotNull(policyCreated1);
        policy.getTags().forEach(tag -> {
            var optional = tagRepository.findByTenantIdAndName(policyCreated1.getTenantId(), tag.getName());
            if(optional.isPresent()) {
                tag = optional.get();
            }
            tag.getPolicies().add(policyCreated1);
            tagRepository.save(tag);
        });

        var policy2 = createNewPolicy(monitorPolicyMapper);
        MonitorPolicy policyCreated2 = repository.save(policy2);
        Assertions.assertNotNull(policyCreated2);

        Assertions.assertNotEquals(policyCreated1.getId(), policyCreated2.getId());
        policy2.getTags().forEach(tag -> {
            var optional = tagRepository.findByTenantIdAndName(policyCreated2.getTenantId(), tag.getName());
            if(optional.isPresent()) {
                tag = optional.get();
            }
            tag.getPolicies().add(policyCreated2);
            tagRepository.save(tag);
        });


        var optionalDefaultTag = tagRepository.findByTenantIdAndName("opennms-prime", "Default");
        Assertions.assertTrue(optionalDefaultTag.isPresent());
        var optionalExampleTag = tagRepository.findByTenantIdAndName("opennms-prime", "Example");
        Assertions.assertTrue(optionalExampleTag.isPresent());
    }

    MonitorPolicy createNewPolicy(MonitorPolicyMapper monitorPolicyMapper) {
        AlertConditionProto coldReboot = AlertConditionProto.newBuilder()
            .setTriggerEventType(EventType.SNMP_Cold_Start)
            .setCount(1)
            .setSeverity(Severity.CRITICAL)
            .build();
        AlertConditionProto warmReboot = AlertConditionProto.newBuilder()
            .setTriggerEventType(EventType.SNMP_Warm_Start)
            .setCount(1)
            .setSeverity(Severity.MAJOR)
            .build();
        PolicyRuleProto defaultRule = PolicyRuleProto.newBuilder()
            .setName("default")
            .setComponentType(ManagedObjectType.NODE)
            .addAllSnmpEvents(List.of(coldReboot, warmReboot))
            .build();
        MonitorPolicyProto defaultPolicy = MonitorPolicyProto.newBuilder()
            .setName("default_policy")
            .setMemo("Default SNMP event monitoring policy")
            .setNotifyByEmail(true)
            .setNotifyByPagerDuty(true)
            .setNotifyByWebhooks(true)
            .addRules(defaultRule)
            .addTags("Default")
            .addTags("Example")
            .setNotifyInstruction("This is default policy notification") //todo: changed to something from environment
            .build();
        MonitorPolicy policy = monitorPolicyMapper.map(defaultPolicy);
        String tenantId = "opennms-prime";
        policy.setTenantId(tenantId);
        policy.getRules().forEach(r -> {
            r.setTenantId(tenantId);
            r.setPolicy(policy);
            r.getAlertConditions().forEach(e -> {
                e.setTenantId(tenantId);
                e.setRule(r);
            });
        });
        policy.getTags().forEach( tag -> {
            tag.setTenantId(tenantId);
        });
        return policy;
    }
}
