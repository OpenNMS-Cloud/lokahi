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
package org.opennms.horizon.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.mapper.ConfigurationMapper;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;

public class ConfigurationServiceTest {
    private ConfigurationRepository mockConfigurationRepo;
    private ConfigurationService service;

    private ConfigurationDTO testConfiguration;

    private final String location = "test location";

    private final String tenantId = "test-tenant";
    private final ConfigKey key = ConfigKey.DISCOVERY;
    private final String value = "{\"test\":\"value\"}";
    private ArgumentCaptor<Configuration> configArgCaptor;

    @BeforeEach
    public void setUP() {
        mockConfigurationRepo = mock(ConfigurationRepository.class);
        ConfigurationMapper mapper = Mappers.getMapper(ConfigurationMapper.class);
        service = new ConfigurationService(mockConfigurationRepo, mapper) {};
        testConfiguration = ConfigurationDTO.newBuilder()
                .setLocation(location)
                .setTenantId(tenantId)
                .setKey(key)
                .setValue(value)
                .build();
        configArgCaptor = ArgumentCaptor.forClass(Configuration.class);
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
        verify(mockConfigurationRepo).save(configArgCaptor.capture());
        Configuration result = configArgCaptor.getValue();
        assertThat(result)
                .isNotNull()
                .extracting(
                        Configuration::getTenantId,
                        Configuration::getLocation,
                        Configuration::getKey,
                        Configuration::getValue)
                .containsExactly(tenantId, location, key, new ObjectMapper().readTree(value));
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
        assertThat(result)
                .isNotNull()
                .extracting(
                        Configuration::getId,
                        Configuration::getTenantId,
                        Configuration::getLocation,
                        Configuration::getKey,
                        Configuration::getValue)
                .containsExactly(1L, tenantId, location, key, new ObjectMapper().readTree(value));
        verify(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
    }

    @Test
    void testCreateOrUpdateNew() throws JsonProcessingException {
        doReturn(Optional.empty()).when(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
        service.createOrUpdate(testConfiguration);
        verify(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
        verify(mockConfigurationRepo).save(configArgCaptor.capture());
        Configuration result = configArgCaptor.getValue();
        assertThat(result)
                .isNotNull()
                .extracting(
                        Configuration::getTenantId,
                        Configuration::getLocation,
                        Configuration::getKey,
                        Configuration::getValue)
                .containsExactly(tenantId, location, key, new ObjectMapper().readTree(value));
    }

    @Test
    void testCreateOrUpdateExist() throws JsonProcessingException {
        String oldValue = "{\"old_value\": \"will be changed\"}";
        Configuration configuration = new Configuration();
        configuration.setTenantId(tenantId);
        configuration.setLocation(location);
        configuration.setKey(key);
        configuration.setId(1L);
        configuration.setValue(new ObjectMapper().readTree(oldValue));

        doReturn(Optional.of(configuration)).when(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
        service.createOrUpdate(testConfiguration);
        verify(mockConfigurationRepo).save(configArgCaptor.capture());
        Configuration arg = configArgCaptor.getValue();
        assertThat(arg)
                .isNotNull()
                .extracting(
                        Configuration::getId,
                        Configuration::getTenantId,
                        Configuration::getLocation,
                        Configuration::getKey,
                        Configuration::getValue)
                .containsExactly(
                        1L, tenantId, location, key, new ObjectMapper().readTree(value)); // updated with the new value;
        verify(mockConfigurationRepo).getByTenantIdAndKey(tenantId, key);
    }
}
