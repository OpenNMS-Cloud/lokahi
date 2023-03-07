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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.model.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class ConfigurationRepoTest {
    private final String tenantId1 = new UUID(10, 10).toString();
    private final String tenantId2 = new UUID(9, 9).toString();
    private Configuration configuration1, configuration2, configuration3, configuration4;

    @Autowired
    private ConfigurationRepository repository;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        configuration1 = new Configuration();
        configuration1.setLocation("test-location1");
        configuration1.setTenantId(tenantId1);
        configuration1.setKey(ConfigKey.DISCOVERY);
        configuration1.setValue(new ObjectMapper().readTree("{\"test\": \"value1\"}"));

        configuration2 = new Configuration();
        configuration2.setLocation("test-location1");
        configuration2.setTenantId(tenantId1);
        configuration2.setKey(ConfigKey.SNMP);
        configuration2.setValue(new ObjectMapper().readTree("{\"test\": \"value2\"}"));

        configuration3 = new Configuration();
        configuration3.setLocation("test-location3");
        configuration3.setTenantId(new UUID(5, 6).toString());
        configuration3.setKey(ConfigKey.SNMP);
        configuration3.setValue(new ObjectMapper().readTree("{\"test\": \"value3\"}"));

        configuration4 = new Configuration();
        configuration4.setLocation("test-location1");
        configuration4.setTenantId(tenantId2);
        configuration4.setKey(ConfigKey.DISCOVERY);
        configuration4.setValue(new ObjectMapper().readTree("{\"test\": \"value4\"}"));

        repository.save(configuration1);
        repository.save(configuration2);
        repository.save(configuration3);
        repository.save(configuration4);
    }

    @AfterEach
    public void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void testFindAll() {
        List<Configuration> result = repository.findAll();
        assertThat(result.size()).isEqualTo(4);
    }

    @Test
    void testFindByKey() {
        Optional<Configuration> result = repository.getByTenantIdAndKey(configuration1.getTenantId(), configuration1.getKey());
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getValue().toString()).isEqualTo("{\"test\":\"value1\"}");
    }

    @Test
    void testFindByKeyNotExist() {
        Optional<Configuration> result = repository.getByTenantIdAndKey(configuration1.getTenantId(), ConfigKey.UNRECOGNIZED);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void testFindByRandomTenantIdAndKey() {
        Optional<Configuration> result = repository.getByTenantIdAndKey(new UUID(5,8).toString(), configuration1.getKey());
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void testFindByTenantIdAndRandomKey() {
        Optional<Configuration> result = repository.getByTenantIdAndKey(configuration1.getTenantId(), ConfigKey.UNRECOGNIZED);
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void testFindByLocation() {
        List<Configuration> result = repository.findByTenantIdAndLocation(configuration1.getTenantId(), configuration1.getLocation());
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void testFindByLocationNotExist() {
        List<Configuration> result = repository.findByTenantIdAndLocation(configuration1.getTenantId(), "Invalid location");
        assertThat(result.size()).isZero();
    }

    @Test
    void testFindByRandomTenantIdAndLocation() {
        List<Configuration> result = repository.findByTenantIdAndLocation(new UUID(5,8).toString(), configuration1.getLocation());
        assertThat(result.size()).isZero();
    }

    @Test
    void testFindByRandomLocation() {
        List<Configuration> result = repository.findByTenantIdAndLocation(tenantId1, "Random location");
        assertThat(result.size()).isZero();
    }
}
