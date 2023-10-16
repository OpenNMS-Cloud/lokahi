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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.discovery.PassiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class PassiveDiscoveryServiceTest {
    PassiveDiscoveryService passiveDiscoveryService;
    private PassiveDiscoveryRepository passiveDiscoveryRepository;
    private TagService tagService;
    private NodeRepository nodeRepository;
    private ScannerTaskSetService scannerTaskSetService;

    private MonitoringLocationService monitoringLocationService;

    @BeforeEach
    void prepareTest() {
        PassiveDiscoveryMapper passiveDiscoveryMapper = Mappers.getMapper(PassiveDiscoveryMapper.class);
        passiveDiscoveryRepository = mock(PassiveDiscoveryRepository.class);
        tagService = mock(TagService.class);
        nodeRepository = mock(NodeRepository.class);
        monitoringLocationService = mock(MonitoringLocationService.class);
        passiveDiscoveryService = new PassiveDiscoveryService(passiveDiscoveryMapper,
            passiveDiscoveryRepository, tagService, nodeRepository, scannerTaskSetService, monitoringLocationService);
    }

    @Test
    public void validateCommunityStrings() {
        // No exception should be thrown..
        PassiveDiscoveryUpsertDTO valid = PassiveDiscoveryUpsertDTO
            .newBuilder().addCommunities("1.2.3.4").build();
        passiveDiscoveryService.validateCommunityStrings(valid);
    }

    @Test
    public void validateCommunityStringsLength() {
            Exception exception = assertThrows(InventoryRuntimeException.class, () -> {
            List<String> communities = new ArrayList<>();
            communities.add("123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
            PassiveDiscoveryUpsertDTO tooLong = PassiveDiscoveryUpsertDTO
                .newBuilder().addAllCommunities(communities)
                .build();
            passiveDiscoveryService.validateCommunityStrings(tooLong);
            });
            assertTrue(exception.getMessage().equals("Snmp communities string is too long"));
    }

    @Test
    public void validateCommunityStringsChars() {
        Exception exception = assertThrows(InventoryRuntimeException.class, () -> {
            List<String> communities = new ArrayList<>();
            communities.add("ÿ");
            PassiveDiscoveryUpsertDTO invalidChars = PassiveDiscoveryUpsertDTO
                .newBuilder().addAllCommunities(communities)
                .build();
            passiveDiscoveryService.validateCommunityStrings(invalidChars);
        });
        assertTrue(exception.getMessage().equals("All characters must be 7bit ascii"));
    }

    @Test
    public void validatePorts() {
        // No exception should be thrown..
        PassiveDiscoveryUpsertDTO valid = PassiveDiscoveryUpsertDTO
            .newBuilder().addPorts(12345).build();
        passiveDiscoveryService.validateSnmpPorts(valid);
    }
    @Test
    public void validatePortsRange() {
        Exception exception = assertThrows(InventoryRuntimeException.class, () -> {
            PassiveDiscoveryUpsertDTO invalid = PassiveDiscoveryUpsertDTO
                .newBuilder()
                .addPorts(Constants.SNMP_PORT_MAX+1)
                .addPorts(0)
                .build();
            passiveDiscoveryService.validateSnmpPorts(invalid);
        });
        assertTrue(exception.getMessage().contains("SNMP port is not in range"));
    }

    @Test
    void testCreateDiscoveryLocationNotFound() {
        final String tenantId = "test_tenant";
        final String locationId = "11";

        PassiveDiscoveryUpsertDTO upsertDTO = PassiveDiscoveryUpsertDTO.newBuilder()
            .setLocationId(locationId).build();
        var exception = assertThrows(LocationNotFoundException.class, () -> passiveDiscoveryService.createDiscovery(tenantId, upsertDTO));

        Assertions.assertEquals("Location not found", exception.getMessage());
    }

    @Test
    void testDeleteDiscovery() {
        String tenantId = "test_tenant";
        long discoveryId = 10L;
        PassiveDiscovery passiveDiscovery = mock(PassiveDiscovery.class);
        when(passiveDiscoveryRepository.findByTenantIdAndId(tenantId, discoveryId)).thenReturn(Optional.of(passiveDiscovery));

        passiveDiscoveryService.deleteDiscovery(tenantId, discoveryId);

        verify(passiveDiscoveryRepository, times(1)).delete(passiveDiscovery);
    }

    @Test
    void testDeleteDiscoveryNotFound() {
        String tenantId = "test_tenant";
        long discoveryId = 10L;

        when(passiveDiscoveryRepository.findByTenantIdAndId(tenantId, discoveryId)).thenReturn(Optional.empty());

        var exception = assertThrows(InventoryRuntimeException.class,
            () -> passiveDiscoveryService.deleteDiscovery(tenantId, discoveryId));

        Assertions.assertEquals("Discovery not found.", exception.getMessage());
    }
}
