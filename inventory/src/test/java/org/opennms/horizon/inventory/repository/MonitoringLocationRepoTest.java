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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class MonitoringLocationRepoTest {
    private final String tenantId = new UUID(10, 10).toString();
    private MonitoringLocation location1, location2, location3;

    @Autowired
    private MonitoringLocationRepository repository;

    @BeforeEach
    public void setUp() {
        location1 = new MonitoringLocation();
        location1.setLocation("test-location1");
        location1.setTenantId(tenantId);

        location2 = new MonitoringLocation();
        location2.setLocation("test-location2");
        location2.setTenantId(tenantId);

        location3 = new MonitoringLocation();
        location3.setLocation("test-location3");
        location3.setTenantId(new UUID(5,6).toString());

        repository.save(location1);
        repository.save(location2);
        repository.save(location3);
    }

    @AfterEach
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    public void testFindByTenantId() {
        List<MonitoringLocation> result = repository.findByTenantId(tenantId);
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void testFindByRandomTenantId() {
        List<MonitoringLocation> result = repository.findByTenantId( new UUID(5,7).toString());
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testFindByLocation(){
        Optional<MonitoringLocation> result = repository.findByLocation(location1.getLocation());
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    public void testFindByLocationNotExist(){
        Optional<MonitoringLocation> result = repository.findByLocation("random location");
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void testFindByLocationAndTenantId() {
        Optional<MonitoringLocation> result = repository.findByLocationAndTenantId(location1.getLocation(), tenantId);
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    public void testFindByRandomLocationAndTenantId() {
        Optional<MonitoringLocation> result = repository.findByLocationAndTenantId("invalid location", tenantId);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void testFindByLocationAndRandomTenantId() {
        Optional<MonitoringLocation> result = repository.findByLocationAndTenantId(location1.getLocation(), new UUID(5,8).toString());
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void testFindByIdList(){
        List<MonitoringLocation> result = repository.findByIdIn(Arrays.asList(location1.getId(),location2.getId(),location3.getId()));
        assertThat(result.size()).isEqualTo(3);
    }
}
