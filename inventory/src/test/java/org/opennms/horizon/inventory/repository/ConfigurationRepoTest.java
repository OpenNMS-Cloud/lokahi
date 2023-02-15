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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.ConfigType;
import org.opennms.horizon.inventory.model.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        configuration1.setKey("test-key1");
        configuration1.setValue(new ObjectMapper().readTree("{\"test\": \"value1\"}"));
        configuration1.setType(ConfigType.DISCOVERY);

        configuration2 = new Configuration();
        configuration2.setLocation("test-location1");
        configuration2.setTenantId(tenantId1);
        configuration2.setKey("test-key2");
        configuration2.setValue(new ObjectMapper().readTree("{\"test\": \"value2\"}"));
        configuration2.setType(ConfigType.DISCOVERY);

        configuration3 = new Configuration();
        configuration3.setLocation("test-location3");
        configuration3.setTenantId(new UUID(5, 6).toString());
        configuration3.setKey("test-key3");
        configuration3.setValue(new ObjectMapper().readTree("{\"test\": \"value3\"}"));
        configuration3.setType(ConfigType.DISCOVERY);

        configuration4 = new Configuration();
        configuration4.setLocation("test-location1");
        configuration4.setTenantId(tenantId2);
        configuration4.setKey("test-key1");
        configuration4.setValue(new ObjectMapper().readTree("{\"test\": \"value4\"}"));
        configuration4.setType(ConfigType.SNMP);

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
    void testFindByTypeNotExist() {
        List<Configuration> list = repository.findByTenantIdAndType(tenantId1, ConfigType.UNRECOGNIZED);
        assertThat(list).asList().hasSize(0);
    }

    @Test
    void testFindByTenantIdNotExist() {
        List<Configuration> list = repository.findByTenantIdAndType("some-tenant", ConfigType.DISCOVERY);
        assertThat(list).asList().hasSize(0);
    }

    @Test
    void testListByConfigType() {
        List<Configuration> list = repository.findByTenantIdAndType(tenantId1, ConfigType.DISCOVERY);
        assertThat(list).asList().hasSize(2)
            .extracting("key", "type")
            .contains(Tuple.tuple(configuration2.getKey(), ConfigType.DISCOVERY),
                Tuple.tuple(configuration1.getKey(), ConfigType.DISCOVERY));
    }

    @Test
    void testListByLocationAndType() {
        List<Configuration> list = repository.findByTenantIdAndLocationAndType(tenantId1, "test-location1", ConfigType.DISCOVERY);
        assertThat(list).asList().hasSize(2)
            .extracting("key", "type")
            .contains(Tuple.tuple(configuration2.getKey(), ConfigType.DISCOVERY),
                Tuple.tuple(configuration1.getKey(), ConfigType.DISCOVERY));
    }

    @Test
    void testListByWrongLocationAndType() {
        List<Configuration> list = repository.findByTenantIdAndLocationAndType(tenantId1, "somewhere", ConfigType.DISCOVERY);
        assertThat(list).asList().hasSize(0);
    }

    @Test
    void testGetByKeyAndType() {
        Optional<Configuration> result = repository.getByTenantIdAndKeyAndType(tenantId1, "test-key2", ConfigType.DISCOVERY);
        assertThat(result).isPresent().get()
            .extracting(Configuration::getKey, Configuration::getType)
            .containsExactly(configuration2.getKey(), ConfigType.DISCOVERY);

    }

    @Test
    void testGetByKeyAndTypeNotExit() {
        Optional<Configuration> result = repository.getByTenantIdAndKeyAndType(tenantId1, "test-key2", ConfigType.UNRECOGNIZED);
        assertThat(result).isEmpty();
    }

    @Test
    void testGetByKeyNotExit() {
        Optional<Configuration> result = repository.getByTenantIdAndKeyAndType(tenantId1, "test-key-new", ConfigType.DISCOVERY);
        assertThat(result).isEmpty();
    }

    @Test
    void testGetTenantIdNotExist() {
        Optional<Configuration> result = repository.getByTenantIdAndKeyAndType("new tenant", "test-key2", ConfigType.DISCOVERY);
        assertThat(result).isEmpty();
    }
}
