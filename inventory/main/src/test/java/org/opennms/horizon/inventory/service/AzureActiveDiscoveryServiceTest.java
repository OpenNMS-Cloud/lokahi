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

import java.util.List;
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
        azureActiveDiscoveryService = new AzureActiveDiscoveryService(
                client,
                azureActiveDiscoveryMapper,
                azureActiveDiscoveryRepository,
                activeDiscoveryRepository,
                scannerTaskSetService,
                monitoringLocationService,
                tagService);
    }

    @Test
    void testCreateDiscoveryLocationNotFound() {
        final String tenantId = "test_tenant";
        final String locationId = "11";

        AzureActiveDiscoveryCreateDTO createDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
                .setLocationId(locationId)
                .setName("not blank")
                .build();
        var exception = assertThrows(
                LocationNotFoundException.class,
                () -> azureActiveDiscoveryService.createActiveDiscovery(tenantId, createDTO));

        Assertions.assertEquals("Location not found with id 11", exception.getMessage());
    }

    @Test
    void testCreateDiscoveryDuplicateName() {
        final String tenantId = "test_tenant";
        final String name = "duplicate";
        var discovery = new IcmpActiveDiscovery();
        discovery.setId(1L);

        when(activeDiscoveryRepository.findByNameAndTenantId(name, tenantId)).thenReturn(List.of(discovery));

        AzureActiveDiscoveryCreateDTO createDTO =
                AzureActiveDiscoveryCreateDTO.newBuilder().setName(name).build();
        var exception = assertThrows(
                InventoryRuntimeException.class,
                () -> azureActiveDiscoveryService.createActiveDiscovery(tenantId, createDTO));
        Assertions.assertEquals("Duplicate active discovery with name duplicate", exception.getMessage());
    }
}
