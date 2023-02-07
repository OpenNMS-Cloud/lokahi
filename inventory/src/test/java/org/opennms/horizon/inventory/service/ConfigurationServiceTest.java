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

package org.opennms.horizon.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.mapper.ConfigurationMapper;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigurationServiceTest {
    private ConfigurationRepository mockConfigurationRepo;
    private ConfigurationService service;

    private ConfigurationDTO testConfiguration;

    private final String location = "test location";

    private final String tenantId = "test-tenant";
    private final String key = "test-key";
    private final String value = "\"{\"test\":\"value\"}\"";

    @BeforeEach
    public void setUP() {
        mockConfigurationRepo = mock(ConfigurationRepository.class);
        ConfigurationMapper mapper = Mappers.getMapper(ConfigurationMapper.class);
        service = new ConfigurationService(mockConfigurationRepo, mapper);
        testConfiguration = ConfigurationDTO.newBuilder()
            .setLocation(location)
            .setTenantId(tenantId)
            .setKey(key)
            .setValue(value)
            .build();
    }

    @AfterEach
    public void postTest() {
        verifyNoMoreInteractions(mockConfigurationRepo);
    }

    @Test
    void testCreateSingle() throws JsonProcessingException {
        doReturn(Optional.empty()).when(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
        service.createSingle(testConfiguration);
        verify(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
        ArgumentCaptor<Configuration> configurationArgumentCaptor = ArgumentCaptor.forClass(Configuration.class);
        verify(mockConfigurationRepo).save(configurationArgumentCaptor.capture());
        Configuration result = configurationArgumentCaptor.getValue();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getLocation()).isEqualTo(location);
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getValue()).isEqualTo(new ObjectMapper().readTree(value));
    }

    @Test
    void testCreateSingleDuplicate() throws JsonProcessingException {
        Configuration configuration = new Configuration();
        configuration.setTenantId(tenantId);
        configuration.setLocation(location);
        configuration.setKey(key);
        configuration.setId(1L);
        configuration.setValue(new ObjectMapper().readTree(value));
        doReturn(Optional.of(configuration)).when(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
        Configuration result = service.createSingle(testConfiguration);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getLocation()).isEqualTo(location);
        assertThat(result.getKey()).isEqualTo(key);
        assertThat(result.getValue()).isEqualTo(new ObjectMapper().readTree(value));
        assertThat(result.getId()).isEqualTo(1L);
        verify(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
    }
}
