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
package org.opennms.horizon.alertservice.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alerts.proto.*;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.repository.EventDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.mapper.EventDefinitionMapper;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled // For developer test only,
// comment out  @PostUpdate @PostPersist on MonitorPolicyProducer
// Also enable commented mapper impl in test
class MonitoringPolicyRepositoryTest {

    @Autowired
    private MonitorPolicyRepository repository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private MonitorPolicyMapper monitorPolicyMapper;

    @Autowired
    private EventDefinitionRepository eventDefinitionRepository;

    @Autowired
    private EventDefinitionMapper eventDefinitionMapper;

    private static final String COLD_START_TRAP_NAME = "SNMP Cold Start";
    private static final String WARM_START_TRAP_NAME = "SNMP Warm Start";

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
            .withDatabaseName("alerts")
            .withUsername("alerts")
            .withPassword("password")
            .withExposedPorts(5432);

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> String.format(
                        "jdbc:postgresql://localhost:%d/%s",
                        postgres.getFirstMappedPort(), postgres.getDatabaseName()));
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
    void testPersistence() {
        var policy = createNewPolicy(monitorPolicyMapper);
        MonitorPolicy policyCreated1 = repository.save(policy);
        Assertions.assertNotNull(policyCreated1);
        policy.getTags().forEach(tag -> {
            var optional = tagRepository.findByTenantIdAndName(policyCreated1.getTenantId(), tag.getName());
            if (optional.isPresent()) {
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
            if (optional.isPresent()) {
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
        AlertEventDefinitionProto coldStartTrap = eventDefinitionRepository
                .findByEventTypeAndName(EventType.SNMP_TRAP, COLD_START_TRAP_NAME)
                .map(eventDefinitionMapper::entityToProto)
                .orElseThrow();
        AlertEventDefinitionProto warmStartTrap = eventDefinitionRepository
                .findByEventTypeAndName(EventType.SNMP_TRAP, WARM_START_TRAP_NAME)
                .map(eventDefinitionMapper::entityToProto)
                .orElseThrow();
        AlertConditionProto coldReboot = AlertConditionProto.newBuilder()
                .setTriggerEvent(coldStartTrap)
                .setCount(1)
                .setSeverity(Severity.CRITICAL)
                .build();
        AlertConditionProto warmReboot = AlertConditionProto.newBuilder()
                .setTriggerEvent(warmStartTrap)
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
                .setNotifyInstruction(
                        "This is default policy notification") // todo: changed to something from environment
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
        policy.getTags().forEach(tag -> {
            tag.setTenantId(tenantId);
        });
        return policy;
    }
}
