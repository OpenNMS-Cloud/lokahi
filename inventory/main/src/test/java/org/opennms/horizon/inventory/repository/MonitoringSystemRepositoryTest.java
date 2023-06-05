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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class MonitoringSystemRepositoryTest {
    @Autowired
    private MonitoringSystemRepository systemRepository;
    @Autowired
    private MonitoringLocationRepository locationRepository;

    private final String tenantId = new UUID(10, 10).toString();
    private MonitoringSystem system1;
    private MonitoringSystem system2;
    private MonitoringSystem system3;
    private MonitoringLocation location;

    @BeforeEach
    public void setUp() {
        location = new MonitoringLocation();
        location.setTenantId(tenantId);
        location.setLocation("test-location");
        locationRepository.save(location);

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
        system3.setTenantId(new UUID(5,6).toString());

        systemRepository.save(system1);
        systemRepository.save(system2);
        systemRepository.save(system3);
    }

    @AfterEach
    public void cleanUp(){
        systemRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    void testFindByTenantId() {
        List<MonitoringSystem> list = systemRepository.findByTenantId(tenantId);
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void testFindByLocationId() {
        List<MonitoringSystem> list = systemRepository.findByMonitoringLocationIdAndTenantId(system1.getMonitoringLocationId(), tenantId);
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    void testFindByRandomTenantId(){
        List<MonitoringSystem> list = systemRepository.findByTenantId(new UUID(5,7).toString());
        assertThat(list.size()).isZero();
    }

    @Test
    void testFindBySystemId() {
        Optional<MonitoringSystem> result = systemRepository.findBySystemId(system1.getSystemId());
        assertThat(result).isPresent();
    }

    @Test
    void testFindBySystemIdNotExist() {
        Optional<MonitoringSystem> result = systemRepository.findBySystemId("random id");
        assertThat(result).isNotPresent();
    }

    @Test
    void testFindBySystemIdAndTenantId() {
        Optional<MonitoringSystem> result = systemRepository.findBySystemIdAndTenantId(system1.getSystemId(), tenantId);
        assertThat(result).isPresent();
    }

    @Test
    void testFindByRandomSystemIdAndTenantId() {
        Optional<MonitoringSystem> result = systemRepository.findBySystemIdAndTenantId("random system id", tenantId);
        assertThat(result).isNotPresent();
    }

    @Test
    void testFindBySystemIdAndRandomTenantId() {
        Optional<MonitoringSystem> result = systemRepository.findBySystemIdAndTenantId(system1.getSystemId(), new UUID(5,8).toString());
        assertThat(result).isNotPresent();
    }
}
