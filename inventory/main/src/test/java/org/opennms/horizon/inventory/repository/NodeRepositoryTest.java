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
package org.opennms.horizon.inventory.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.taskset.contract.ScanType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled
// Developer test only, comment out @PostUpdate , @PostPersist in NodeKafkaProducer to successfully run this
public class NodeRepositoryTest {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
            .withDatabaseName("inventory")
            .withUsername("inventory")
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
        public void testFindByTenantIdAndDiscoveryIdsContain() {
        MonitoringLocation monitoringLocation = new MonitoringLocation();
        monitoringLocation.setTenantId("opennms-prime");
        monitoringLocation.setLocation("minion");
        var location = monitoringLocationRepository.save(monitoringLocation);
        Node node = new Node();
        node.setTenantId("opennms-prime");
        node.setDiscoveryIds(new ArrayList<>(Arrays.asList(1L, 2L)));
        node.setNodeLabel("test");
        node.setScanType(ScanType.NODE_SCAN);
        node.setCreateTime(LocalDateTime.now());
        node.setMonitoringLocation(location);
        nodeRepository.save(node);
        var result = nodeRepository.findByTenantId("opennms-prime");
        Assertions.assertFalse(result.isEmpty());
        Assertions.assertFalse(result.get(0).getDiscoveryIds().isEmpty());
    }
}
