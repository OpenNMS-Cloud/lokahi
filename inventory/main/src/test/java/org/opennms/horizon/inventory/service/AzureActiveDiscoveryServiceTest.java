/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.discovery.AzureActiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.AzureActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.discovery.active.AzureActiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureActiveDiscoveryServiceTest {
    private AzureActiveDiscoveryService azureActiveDiscoveryService;
    private AzureHttpClient client;
    private AzureActiveDiscoveryRepository azureActiveDiscoveryRepository;
    private ActiveDiscoveryRepository activeDiscoveryRepository;
    private TagService tagService;

    private ScannerTaskSetService scannerTaskSetService;

    private MonitoringLocationService monitoringLocationService;

    @BeforeEach
    void prepareTest() {
        client = mock(AzureHttpClient.class);
        AzureActiveDiscoveryMapper azureActiveDiscoveryMapper = Mappers.getMapper(AzureActiveDiscoveryMapper.class);
        azureActiveDiscoveryRepository = mock(AzureActiveDiscoveryRepository.class);
        activeDiscoveryRepository = mock(ActiveDiscoveryRepository.class);

        tagService = mock(TagService.class);
        scannerTaskSetService = mock(ScannerTaskSetService.class);

        monitoringLocationService = mock(MonitoringLocationService.class);
        azureActiveDiscoveryService = new AzureActiveDiscoveryService(client, azureActiveDiscoveryMapper,
            azureActiveDiscoveryRepository, activeDiscoveryRepository, scannerTaskSetService, monitoringLocationService, tagService);
    }

    @Test
    void testCreateDiscoveryLocationNotFound() {
        final String tenantId = "test_tenant";
        final String locationId = "11";

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setLocationId(locationId)
            .setName("not blank").build();
        var exception = assertThrows(LocationNotFoundException.class, () -> azureActiveDiscoveryService.createActiveDiscovery(tenantId, createDTO));

        Assertions.assertEquals("Location not found with id 11", exception.getMessage());
    }

    @Test
    void testCreateDiscoveryDuplicateName() {
        final String tenantId = "test_tenant";
        final String name = "duplicate";
        var discovery = new IcmpActiveDiscovery();
        discovery.setId(1L);

        when(activeDiscoveryRepository.findByNameAndTenantId(name, tenantId)).thenReturn(List.of(discovery));

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder().setName(name).build();
        var exception = assertThrows(InventoryRuntimeException.class, () -> azureActiveDiscoveryService.createActiveDiscovery(tenantId, createDTO));
        Assertions.assertEquals("Duplicate active discovery with name duplicate", exception.getMessage());
    }

}
