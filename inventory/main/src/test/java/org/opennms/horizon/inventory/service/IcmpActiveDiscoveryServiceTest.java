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
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.discovery.IcmpActiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.IcmpActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.discovery.active.IcmpActiveDiscoveryService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IcmpActiveDiscoveryServiceTest {
    IcmpActiveDiscoveryService icmpActiveDiscoveryService;
    private IcmpActiveDiscoveryRepository icmpActiveDiscoveryRepository;
    private ActiveDiscoveryRepository activeDiscoveryRepository;
    private TagService tagService;

    private MonitoringLocationService monitoringLocationService;

    @BeforeEach
    void prepareTest() {
        IcmpActiveDiscoveryMapper icmpActiveDiscoveryMapper = Mappers.getMapper(IcmpActiveDiscoveryMapper.class);
        icmpActiveDiscoveryRepository = mock(IcmpActiveDiscoveryRepository.class);
        activeDiscoveryRepository = mock(ActiveDiscoveryRepository.class);

        tagService = mock(TagService.class);

        monitoringLocationService = mock(MonitoringLocationService.class);
        icmpActiveDiscoveryService = new IcmpActiveDiscoveryService(
            icmpActiveDiscoveryRepository, activeDiscoveryRepository, monitoringLocationService, icmpActiveDiscoveryMapper, tagService);
    }

    @Test
    void testCreateDiscoveryLocationNotFound() {
        final String tenantId = "test_tenant";
        final String locationId = "11";

        IcmpActiveDiscoveryCreateDTO createDTO = IcmpActiveDiscoveryCreateDTO.newBuilder()
            .setLocationId(locationId)
            .setName("not blank")
            .build();
        var exception = assertThrows(LocationNotFoundException.class, () -> icmpActiveDiscoveryService.createActiveDiscovery(createDTO, tenantId));

        Assertions.assertEquals("Location not found with id 11", exception.getMessage());
    }

    @Test
    void testCreateDiscoveryDuplicateName() {
        final String tenantId = "test_tenant";
        final String name = "duplicate";
        List<ActiveDiscovery> discoveries = new ArrayList<>();
        discoveries.add(new IcmpActiveDiscovery());
        when(activeDiscoveryRepository.findByNameAndTenantId(name, tenantId)).thenReturn(discoveries);

        IcmpActiveDiscoveryCreateDTO createDTO = IcmpActiveDiscoveryCreateDTO.newBuilder().setName(name).build();
        var exception = assertThrows(InventoryRuntimeException.class, () -> icmpActiveDiscoveryService.createActiveDiscovery(createDTO, tenantId));

        Assertions.assertEquals("Duplicate active discovery with name duplicate", exception.getMessage());
    }

}
