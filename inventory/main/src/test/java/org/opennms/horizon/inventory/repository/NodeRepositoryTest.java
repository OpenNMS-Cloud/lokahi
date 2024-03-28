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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.ScanType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Disabled
// For developer test only,
// comment out  @PostUpdate @PostPersist on MonitorPolicyProducer
public class NodeRepositoryTest {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;

    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;

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
        Node node = new Node();
        node.setTenantId("opennms-prime");
        node.setDiscoveryIds(new ArrayList<>(Arrays.asList(1L, 2L)));
        node.setNodeLabel("test");
        nodeRepository.save(node);
        var result = nodeRepository.findByTenantId("opennms-prime");
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    public void testAddNewRecordWithSameLocationAndIpAddressThrowException() {

        MonitoringLocation monitoringLocation = new MonitoringLocation();
        monitoringLocation.setTenantId("opennms-prime");
        monitoringLocation.setLocation("minion-default");
        monitoringLocationRepository.save(monitoringLocation);

        Node node = new Node();
        node.setTenantId("opennms-prime");
        node.setDiscoveryIds(Arrays.asList(1L, 2L));
        node.setNodeLabel("my-label");
        node.setDiscoveryIds(new ArrayList<>(Arrays.asList(1L, 2L)));
        node.setScanType(ScanType.NODE_SCAN);
        node.setCreateTime(LocalDateTime.now());
        node.setMonitoringLocation(monitoringLocation);
        nodeRepository.save(node);

        IpInterface existingIpInterface = new IpInterface();
        existingIpInterface.setNode(node);
        existingIpInterface.setTenantId("opennms-prime");
        existingIpInterface.setIpAddress(InetAddressUtils.getInetAddress("172.16.8.1"));
        existingIpInterface.setSnmpPrimary(true);
        existingIpInterface.setLocation(monitoringLocation);
        ipInterfaceRepository.save(existingIpInterface);

        IpInterface newIpInterface = new IpInterface();
        newIpInterface.setNode(node);
        newIpInterface.setTenantId("opennms-prime");
        newIpInterface.setIpAddress(InetAddressUtils.getInetAddress("172.16.8.1"));
        newIpInterface.setSnmpPrimary(true);
        newIpInterface.setLocation(monitoringLocation);

        Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
            ipInterfaceRepository.save(newIpInterface);
        });
    }
}
