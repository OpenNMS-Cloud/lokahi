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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
        passiveDiscoveryService = new PassiveDiscoveryService(
                passiveDiscoveryMapper,
                passiveDiscoveryRepository,
                tagService,
                nodeRepository,
                scannerTaskSetService,
                monitoringLocationService);
    }

    @Test
    public void validateCommunityStrings() {
        // No exception should be thrown..
        PassiveDiscoveryUpsertDTO valid =
                PassiveDiscoveryUpsertDTO.newBuilder().addCommunities("1.2.3.4").build();
        passiveDiscoveryService.validateCommunityStrings(valid);
    }

    @Test
    public void validateCommunityStringsLength() {
        Exception exception = assertThrows(InventoryRuntimeException.class, () -> {
            List<String> communities = new ArrayList<>();
            communities.add(
                    "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
            PassiveDiscoveryUpsertDTO tooLong = PassiveDiscoveryUpsertDTO.newBuilder()
                    .addAllCommunities(communities)
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
            PassiveDiscoveryUpsertDTO invalidChars = PassiveDiscoveryUpsertDTO.newBuilder()
                    .addAllCommunities(communities)
                    .build();
            passiveDiscoveryService.validateCommunityStrings(invalidChars);
        });
        assertTrue(exception.getMessage().equals("All characters must be 7bit ascii"));
    }

    @Test
    public void validatePorts() {
        // No exception should be thrown..
        PassiveDiscoveryUpsertDTO valid =
                PassiveDiscoveryUpsertDTO.newBuilder().addPorts(12345).build();
        passiveDiscoveryService.validateSnmpPorts(valid);
    }

    @Test
    public void validatePortsRange() {
        Exception exception = assertThrows(InventoryRuntimeException.class, () -> {
            PassiveDiscoveryUpsertDTO invalid = PassiveDiscoveryUpsertDTO.newBuilder()
                    .addPorts(Constants.SNMP_PORT_MAX + 1)
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
                .setLocationId(locationId)
                .setName("not blank")
                .build();
        var exception = assertThrows(
                LocationNotFoundException.class, () -> passiveDiscoveryService.createDiscovery(tenantId, upsertDTO));

        Assertions.assertEquals("Location not found with location 11", exception.getMessage());
    }

    @Test
    void testCreateDiscoveryDuplicateName() {
        final String tenantId = "test_tenant";
        final String name = "duplicate";
        var discovery = new PassiveDiscovery();
        discovery.setId(1L);
        when(passiveDiscoveryRepository.findByTenantIdAndName(tenantId, name)).thenReturn(List.of(discovery));

        PassiveDiscoveryUpsertDTO upsertDTO =
                PassiveDiscoveryUpsertDTO.newBuilder().setName(name).build();
        var exception = assertThrows(
                InventoryRuntimeException.class, () -> passiveDiscoveryService.createDiscovery(tenantId, upsertDTO));

        Assertions.assertEquals("Duplicate passive discovery with name duplicate", exception.getMessage());
    }

    @Test
    void testDeleteDiscovery() {
        String tenantId = "test_tenant";
        long discoveryId = 10L;
        PassiveDiscovery passiveDiscovery = mock(PassiveDiscovery.class);
        when(passiveDiscoveryRepository.findByTenantIdAndId(tenantId, discoveryId))
                .thenReturn(Optional.of(passiveDiscovery));

        passiveDiscoveryService.deleteDiscovery(tenantId, discoveryId);

        verify(passiveDiscoveryRepository, times(1)).delete(passiveDiscovery);
    }

    @Test
    void testDeleteDiscoveryNotFound() {
        String tenantId = "test_tenant";
        long discoveryId = 10L;

        when(passiveDiscoveryRepository.findByTenantIdAndId(tenantId, discoveryId))
                .thenReturn(Optional.empty());

        var exception = assertThrows(
                InventoryRuntimeException.class, () -> passiveDiscoveryService.deleteDiscovery(tenantId, discoveryId));

        Assertions.assertEquals("Discovery not found.", exception.getMessage());
    }
}
