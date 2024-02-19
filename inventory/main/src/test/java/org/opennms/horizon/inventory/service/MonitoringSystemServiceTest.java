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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;

class MonitoringSystemServiceTest {
    private MonitoringLocationRepository mockLocationRepo;
    private MonitoringSystemRepository mockMonitoringSystemRepo;
    private ConfigUpdateService mockConfigUpdateService;
    private MonitoringSystemService service;

    private MonitoringSystem testMonitoringSystem;
    private MonitoringLocation testLocation;

    private TenantLocationSpecificHeartbeatMessage heartbeatMessage;
    private final Long locationId = new Random().nextLong(1, Long.MAX_VALUE);
    private final String systemId = "test-monitoring-system-12345";

    private final String tenantId = "test-tenant";

    @BeforeEach
    public void setUP() {
        mockLocationRepo = mock(MonitoringLocationRepository.class);
        mockMonitoringSystemRepo = mock(MonitoringSystemRepository.class);
        mockConfigUpdateService = mock(ConfigUpdateService.class);
        MonitoringSystemMapper mapper = Mappers.getMapper(MonitoringSystemMapper.class);
        service = new MonitoringSystemService(
                mockMonitoringSystemRepo, mockLocationRepo, mapper, mockConfigUpdateService);
        testLocation = new MonitoringLocation();
        testLocation.setId(locationId);
        testLocation.setLocation("Location " + locationId);
        testLocation.setTenantId(tenantId);
        testMonitoringSystem = new MonitoringSystem();
        testMonitoringSystem.setLastCheckedIn(LocalDateTime.now());
        testMonitoringSystem.setTenantId(tenantId);
        testMonitoringSystem.setSystemId(systemId);
        testMonitoringSystem.setLabel(systemId);
        testMonitoringSystem.setMonitoringLocation(testLocation);
        testMonitoringSystem.setMonitoringLocationId(locationId);
        heartbeatMessage = TenantLocationSpecificHeartbeatMessage.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(String.valueOf(locationId))
                .setIdentity(Identity.newBuilder().setSystemId(systemId))
                .build();
    }

    @AfterEach
    public void postTest() {
        verifyNoMoreInteractions(mockLocationRepo);
        verifyNoMoreInteractions(mockMonitoringSystemRepo);
    }

    @Test
    void testFindByTenantId() {
        doReturn(Collections.singletonList(testMonitoringSystem))
                .when(mockMonitoringSystemRepo)
                .findByTenantId(tenantId);
        service.findByTenantId(tenantId);
        verify(mockMonitoringSystemRepo).findByTenantId(tenantId);
    }

    @Test
    void testFindByMonitoringLocationIdAndTenantId() {
        long locationId = 1L;
        doReturn(Collections.singletonList(testMonitoringSystem))
                .when(mockMonitoringSystemRepo)
                .findByMonitoringLocationIdAndTenantId(locationId, tenantId);
        doReturn(Optional.of(new MonitoringLocation()))
                .when(mockLocationRepo)
                .findByIdAndTenantId(locationId, tenantId);
        service.findByMonitoringLocationIdAndTenantId(locationId, tenantId);
        verify(mockMonitoringSystemRepo).findByMonitoringLocationIdAndTenantId(locationId, tenantId);
        verify(mockLocationRepo).findByIdAndTenantId(locationId, tenantId);
    }

    @Test
    void testFindByMonitoringLocationIdAndTenantIdLocationNotFound() {
        long locationId = 1L;
        var exception = Assert.assertThrows(LocationNotFoundException.class, () -> {
            service.findByMonitoringLocationIdAndTenantId(locationId, tenantId);
        });
        assertThat(exception.getMessage()).isEqualTo("Location not found for id: " + locationId);
        verify(mockLocationRepo).findByIdAndTenantId(locationId, tenantId);
    }

    @Test
    void testReceiveMsgMonitorSystemExist() throws LocationNotFoundException {
        doReturn(Optional.of(testMonitoringSystem))
                .when(mockMonitoringSystemRepo)
                .findByMonitoringLocationIdAndSystemIdAndTenantId(locationId, systemId, tenantId);
        doReturn(Optional.of(testLocation)).when(mockLocationRepo).findByIdAndTenantId(locationId, tenantId);
        service.addMonitoringSystemFromHeartbeat(heartbeatMessage);
        verify(mockMonitoringSystemRepo)
                .findByMonitoringLocationIdAndSystemIdAndTenantId(testLocation.getId(), systemId, tenantId);
        verify(mockMonitoringSystemRepo).save(testMonitoringSystem);
        verifyNoMoreInteractions(mockConfigUpdateService);
    }

    @Test
    void testCreateNewMonitorSystemWithLocationExist() throws LocationNotFoundException {
        doReturn(Optional.empty())
                .when(mockMonitoringSystemRepo)
                .findByMonitoringLocationIdAndSystemIdAndTenantId(locationId, systemId, tenantId);
        doReturn(Optional.of(testLocation)).when(mockLocationRepo).findByIdAndTenantId(locationId, tenantId);
        service.addMonitoringSystemFromHeartbeat(heartbeatMessage);
        verify(mockMonitoringSystemRepo)
                .findByMonitoringLocationIdAndSystemIdAndTenantId(testLocation.getId(), systemId, tenantId);
        verify(mockMonitoringSystemRepo).save(any(MonitoringSystem.class));
        verify(mockLocationRepo).findByIdAndTenantId(locationId, tenantId);
    }

    @Test
    void testFindBySystemIdWithStatus() {
        doReturn(Optional.of(testMonitoringSystem))
                .when(mockMonitoringSystemRepo)
                .findBySystemIdAndTenantId(systemId, tenantId);
        var result = service.findBySystemId(systemId, tenantId);
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isTrue();
        verify(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
    }
}
