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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MonitoringSystemServiceTest {
    private MonitoringLocationRepository mockLocationRepo;
    private MonitoringSystemRepository mockMonitoringSystemRepo;
    private ConfigUpdateService mockConfigUpdateService;
    private MonitoringSystemService service;

    private MonitoringSystem testMonitoringSystem;
    private MonitoringLocation testLocation;

    private TenantLocationSpecificHeartbeatMessage heartbeatMessage;
    private final String location = "test location";
    private final String systemId = "test-monitoring-system-12345";

    private final String tenantId = "test-tenant";

    @BeforeEach
    public void setUP(){
        mockLocationRepo = mock(MonitoringLocationRepository.class);
        mockMonitoringSystemRepo = mock(MonitoringSystemRepository.class);
        mockConfigUpdateService = mock(ConfigUpdateService.class);
        MonitoringSystemMapper mapper = Mappers.getMapper(MonitoringSystemMapper.class);
        service = new MonitoringSystemService(mockMonitoringSystemRepo, mockLocationRepo, mapper, mockConfigUpdateService);
        testLocation = new MonitoringLocation();
        testLocation.setLocation(location);
        testLocation.setTenantId(tenantId);
        testMonitoringSystem = new MonitoringSystem();
        testMonitoringSystem.setLastCheckedIn(LocalDateTime.now());
        testMonitoringSystem.setTenantId(tenantId);
        testMonitoringSystem.setSystemId(systemId);
        testMonitoringSystem.setLabel(systemId);
        heartbeatMessage =
            TenantLocationSpecificHeartbeatMessage.newBuilder()
                .setTenantId(tenantId)
                .setLocation(location)
                .setIdentity(Identity.newBuilder()
                    .setSystemId(systemId).build()
                )
                .build();
    }

    @AfterEach
    public void postTest() {
        verifyNoMoreInteractions(mockLocationRepo);
        verifyNoMoreInteractions(mockMonitoringSystemRepo);
    }

    @Test
    void testReceiveMsgMonitorSystemExist() {
        doReturn(Optional.of(testMonitoringSystem)).when(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        service.addMonitoringSystemFromHeartbeat(heartbeatMessage);
        verify(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        verify(mockMonitoringSystemRepo).save(testMonitoringSystem);
    }

    @Test
    void testCreateNewMonitorSystemWithLocationExist() {
        doReturn(Optional.empty()).when(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        doReturn(Optional.of(testLocation)).when(mockLocationRepo).findByLocationAndTenantId(location, tenantId);
        service.addMonitoringSystemFromHeartbeat(heartbeatMessage);
        verify(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        verify(mockMonitoringSystemRepo).save(any(MonitoringSystem.class));
        verify(mockLocationRepo).findByLocationAndTenantId(location, tenantId);
    }

    @Test
    void testCreateNewMonitorSystemAndNewLocation() {
        doReturn(Optional.empty()).when(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        doReturn(Optional.empty()).when(mockLocationRepo).findByLocationAndTenantId(location, tenantId);
        doReturn(testLocation).when(mockLocationRepo).save(any(MonitoringLocation.class));
        service.addMonitoringSystemFromHeartbeat(heartbeatMessage);
        verify(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        verify(mockMonitoringSystemRepo).save(any(MonitoringSystem.class));
        verify(mockLocationRepo).findByLocationAndTenantId(location, tenantId);
        verify(mockLocationRepo).save(any(MonitoringLocation.class));
    }

    @Test
    void testFindBySystemIdWithStatus() {
        doReturn(Optional.of(testMonitoringSystem)).when(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
        var result = service.findBySystemId(systemId, tenantId);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getStatus()).isTrue();
        verify(mockMonitoringSystemRepo).findBySystemIdAndTenantId(systemId, tenantId);
    }

}
