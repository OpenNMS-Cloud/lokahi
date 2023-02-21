/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.grpc.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class MonitoringSystemRepositoryTest {
    @Autowired
    private MonitoringSystemRepository systemRepository;
    @Autowired
    private MonitoringLocationRepository locationRepository;

    private final String tenantId = new UUID(10, 10).toString();
    private final String otherTenantId = new UUID(5,6).toString();
    private MonitoringSystem system1;
    private MonitoringSystem system2;
    private MonitoringSystem system3;
    private MonitoringLocation location;

    @BeforeEach
    public void setUp() {
        location = new MonitoringLocation();
        location.setTenantId(tenantId);
        location.setLocation("test-location");

        system1 = new MonitoringSystem();
        system1.setLastCheckedIn(LocalDateTime.now());
        system1.setMonitoringLocationId(location.getId());
        system1.setMonitoringLocation(location);
        system1.setLabel("Test-Minion1");
        system1.setSystemId("test-system-id1");
        system1.setTenantId(tenantId);

        system2 = new MonitoringSystem();
        system2.setLastCheckedIn(LocalDateTime.now());
        system2.setMonitoringLocationId(location.getId());
        system2.setMonitoringLocation(location);
        system2.setLabel("Test-Minion2");
        system2.setSystemId("test-system-id2");
        system2.setTenantId(tenantId);

        system3 = new MonitoringSystem();
        system3.setLastCheckedIn(LocalDateTime.now());
        system3.setMonitoringLocationId(location.getId());
        system3.setMonitoringLocation(location);
        system3.setLabel("Test-Minion3");
        system3.setSystemId("test-system-id3");
        system3.setTenantId(otherTenantId);

        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            locationRepository.save(location);
            systemRepository.save(system1);
            systemRepository.save(system2);
        });
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, otherTenantId).run(()->
        {
            systemRepository.save(system3);
        });
    }

    @AfterEach
    public void cleanUp(){
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            systemRepository.deleteAll();
            locationRepository.deleteAll();
        });
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, otherTenantId).run(()->
        {
            systemRepository.deleteAll();
            locationRepository.deleteAll();
        });
    }

    @Test
    void testFindByTenantId() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            List<MonitoringSystem> list = systemRepository.findByTenantId(tenantId);
            assertThat(list.size()).isEqualTo(2);
        });
    }

    @Test
    void testFindByRandomTenantId(){
        final String tenant = new UUID(5,7).toString();
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenant).run(()->
        {
            List<MonitoringSystem> list = systemRepository.findByTenantId(tenant);
            assertThat(list.isEmpty());
        });
    }

    @Test
    void testFindBySystemId() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            Optional<MonitoringSystem> result = systemRepository.findBySystemId(system1.getSystemId());
            assertThat(result).isPresent();
        });
    }

    @Test
    void testFindBySystemIdNotExist() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            Optional<MonitoringSystem> result = systemRepository.findBySystemId("random id");
            assertThat(result).isNotPresent();
        });
    }

    @Test
    void testFindBySystemIdAndTenantId() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            Optional<MonitoringSystem> result = systemRepository.findBySystemIdAndTenantId(system1.getSystemId(), tenantId);
            assertThat(result).isPresent();
        });
    }

    @Test
    void testFindByRandomSystemIdAndTenantId() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            Optional<MonitoringSystem> result = systemRepository.findBySystemIdAndTenantId("random system id", tenantId);
            assertThat(result).isNotPresent();
        });
    }

    @Test
    void testFindBySystemIdAndRandomTenantId() {
        final String otherTenant = new UUID(5,8).toString();
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, otherTenant).run(()->
        {
            Optional<MonitoringSystem> result = systemRepository.findBySystemIdAndTenantId(system1.getSystemId(), otherTenant);
            assertThat(result).isNotPresent();
        });
    }
}
