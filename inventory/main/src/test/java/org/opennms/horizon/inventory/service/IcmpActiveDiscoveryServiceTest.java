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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.IcmpActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.discovery.active.IcmpActiveDiscoveryService;

class IcmpActiveDiscoveryServiceTest {
    IcmpActiveDiscoveryService icmpActiveDiscoveryService;
    private IcmpActiveDiscoveryRepository icmpActiveDiscoveryRepository;
    private NodeRepository nodeRepository;
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
                icmpActiveDiscoveryRepository,
                nodeRepository,
                activeDiscoveryRepository,
                monitoringLocationService,
                icmpActiveDiscoveryMapper,
                tagService);
    }

    @Test
    void testCreateDiscoveryLocationNotFound() {
        final String tenantId = "test_tenant";
        final String locationId = "11";

        IcmpActiveDiscoveryCreateDTO createDTO = IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setLocationId(locationId)
                .setName("not blank")
                .build();
        var exception = assertThrows(
                LocationNotFoundException.class,
                () -> icmpActiveDiscoveryService.createActiveDiscovery(createDTO, tenantId));

        Assertions.assertEquals("Location not found with id 11", exception.getMessage());
    }

    @Test
    void testCreateDiscoveryDuplicateName() {
        final String tenantId = "test_tenant";
        final String name = "duplicate";
        List<ActiveDiscovery> discoveries = new ArrayList<>();
        discoveries.add(new IcmpActiveDiscovery());
        when(activeDiscoveryRepository.findByNameAndTenantId(name, tenantId)).thenReturn(discoveries);

        IcmpActiveDiscoveryCreateDTO createDTO =
                IcmpActiveDiscoveryCreateDTO.newBuilder().setName(name).build();
        var exception = assertThrows(
                InventoryRuntimeException.class,
                () -> icmpActiveDiscoveryService.createActiveDiscovery(createDTO, tenantId));

        Assertions.assertEquals("Duplicate active discovery with name duplicate", exception.getMessage());
    }
}
