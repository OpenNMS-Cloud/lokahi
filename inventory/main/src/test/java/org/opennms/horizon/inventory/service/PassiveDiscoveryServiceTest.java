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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.discovery.PassiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class PassiveDiscoveryServiceTest {
    PassiveDiscoveryService passiveDiscoveryService;
    private PassiveDiscoveryRepository passiveDiscoveryRepository;
    private TagService tagService;
    private NodeRepository nodeRepository;
    private ScannerTaskSetService scannerTaskSetService;

    private final String tenantId = "test-tenant";
    private final String location = "test-location";

    PassiveDiscoveryUpsertDTO passiveDiscoveryUpsertDTO;

    @BeforeEach
    void prepareTest() {
        PassiveDiscoveryMapper passiveDiscoveryMapper = Mappers.getMapper(PassiveDiscoveryMapper.class);
        passiveDiscoveryRepository = mock(PassiveDiscoveryRepository.class);
        tagService = mock(TagService.class);
        doNothing().when(tagService).updateTags(eq(tenantId),any());

        nodeRepository = mock(NodeRepository.class);
        passiveDiscoveryService = new PassiveDiscoveryService(passiveDiscoveryMapper,
            passiveDiscoveryRepository, tagService,nodeRepository,scannerTaskSetService);
        passiveDiscoveryUpsertDTO = PassiveDiscoveryUpsertDTO
            .newBuilder()
            .setId(0)
            .setLocation(location)
            .build();
    }
    @Test
    public void validatesDiscoveryDifferentLocation() {
        PassiveDiscovery passiveDiscovery = new PassiveDiscovery();
        passiveDiscovery.setId(1);
        passiveDiscovery.setLocation("other"); // different location
        passiveDiscovery.setCreateTime(LocalDateTime.now());
        passiveDiscovery.setToggle(true);

        doReturn(Optional.of(passiveDiscovery)).when(passiveDiscoveryRepository).findByTenantIdAndId(any(),anyLong());
        doReturn(Optional.of(passiveDiscovery)).when(passiveDiscoveryRepository).findByTenantIdAndLocation(any(),eq(location));
        doReturn(passiveDiscovery).when(passiveDiscoveryRepository).save(any());
        passiveDiscoveryService.updateDiscovery(tenantId,passiveDiscoveryUpsertDTO);
        // No exception thrown
    }

    @Test
    public void validatesDiscoverySameLocation() {
        PassiveDiscovery passiveDiscovery = new PassiveDiscovery();
        passiveDiscovery.setId(0);
        passiveDiscovery.setLocation("other"); // different location
        passiveDiscovery.setCreateTime(LocalDateTime.now());
        passiveDiscovery.setToggle(true);

        doReturn(Optional.of(passiveDiscovery)).when(passiveDiscoveryRepository).findByTenantIdAndId(any(),anyLong());
        doReturn(Optional.of(passiveDiscovery)).when(passiveDiscoveryRepository).findByTenantIdAndLocation(any(),eq(location));
        doReturn(passiveDiscovery).when(passiveDiscoveryRepository).save(any());

        Exception exception = assertThrows(InventoryRuntimeException.class, () -> {
            passiveDiscoveryService.updateDiscovery(tenantId,passiveDiscoveryUpsertDTO);
        });
        assertTrue(exception.getMessage().contains("Already a passive discovery with location"));
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
}
