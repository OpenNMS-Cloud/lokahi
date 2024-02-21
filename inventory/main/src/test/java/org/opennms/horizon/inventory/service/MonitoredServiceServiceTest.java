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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.mapper.MonitoredServiceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;

public class MonitoredServiceServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    private MonitoredServiceRepository mockMonitoredServiceRepository;
    private MonitoredServiceMapper mockMonitoredServiceMapper;

    private MonitoredServiceDTO testMonitoredServiceDTO1;
    private MonitoredServiceDTO testMonitoredServiceDTO2;
    private MonitoredServiceDTO testMonitoredServiceDTO3;
    private MonitoredService testMonitoredService1;
    private MonitoredService testMonitoredService2;
    private MonitoredService testMonitoredService3;
    private MonitoredServiceType testMonitoredServiceType;
    private IpInterface testIpInterface;
    private IpInterfaceRepository ipInterfaceRepository;

    private MonitoredServiceService target;

    @BeforeEach
    public void setUp() {
        mockMonitoredServiceRepository = Mockito.mock(MonitoredServiceRepository.class);
        mockMonitoredServiceMapper = Mockito.mock(MonitoredServiceMapper.class);
        ipInterfaceRepository = Mockito.mock(IpInterfaceRepository.class);

        testMonitoredServiceDTO1 = MonitoredServiceDTO.newBuilder()
                .setTenantId(TEST_TENANT_ID)
                .setId(1313)
                .build();

        testMonitoredServiceDTO2 = MonitoredServiceDTO.newBuilder()
                .setTenantId(TEST_TENANT_ID)
                .setId(1717)
                .build();

        testMonitoredServiceDTO3 = MonitoredServiceDTO.newBuilder()
                .setTenantId(TEST_TENANT_ID)
                .setId(1919)
                .build();

        testMonitoredServiceType = new MonitoredServiceType();
        testMonitoredServiceType.setServiceName("x-service-name-x"); // e.g. SSH

        testMonitoredService1 = new MonitoredService();
        testMonitoredService1.setId(1313);

        testMonitoredService2 = new MonitoredService();
        testMonitoredService2.setId(1717);

        testMonitoredService3 = new MonitoredService();
        testMonitoredService3.setId(1919);

        testIpInterface = new IpInterface();
        testIpInterface.setHostname("x-hostname-x");

        target = new MonitoredServiceService(
                mockMonitoredServiceRepository, mockMonitoredServiceMapper, ipInterfaceRepository);
    }

    @Test
    void testCreateSingle() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockMonitoredServiceRepository.findByTenantIdTypeAndIpInterface(
                        TEST_TENANT_ID, testMonitoredServiceType, testIpInterface))
                .thenReturn(Optional.empty());
        Mockito.when(mockMonitoredServiceMapper.dtoToModel(testMonitoredServiceDTO1))
                .thenReturn(testMonitoredService1);

        //
        // Execute
        //
        target.createSingle(testMonitoredServiceDTO1, testMonitoredServiceType, testIpInterface);

        //
        // Verify the Results
        //
        Mockito.verify(mockMonitoredServiceRepository).save(testMonitoredService1);
    }

    @Test
    void testCreateSingleAlreadyExists() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockMonitoredServiceRepository.findByTenantIdTypeAndIpInterface(
                        TEST_TENANT_ID, testMonitoredServiceType, testIpInterface))
                .thenReturn(Optional.of(testMonitoredService1));

        //
        // Execute
        //
        target.createSingle(testMonitoredServiceDTO1, testMonitoredServiceType, testIpInterface);

        //
        // Verify the Results
        //
        Mockito.verify(mockMonitoredServiceRepository, Mockito.times(0)).save(Mockito.any(MonitoredService.class));
    }

    @Test
    void testFindByTenantId() {
        //
        // Setup Test Data and Interactions
        //
        var testMonitoredServiceList = List.of(testMonitoredService1, testMonitoredService2, testMonitoredService3);

        Mockito.when(mockMonitoredServiceRepository.findByTenantId(TEST_TENANT_ID))
                .thenReturn(testMonitoredServiceList);
        Mockito.when(mockMonitoredServiceMapper.modelToDTO(testMonitoredService1))
                .thenReturn(testMonitoredServiceDTO1);
        Mockito.when(mockMonitoredServiceMapper.modelToDTO(testMonitoredService2))
                .thenReturn(testMonitoredServiceDTO2);
        Mockito.when(mockMonitoredServiceMapper.modelToDTO(testMonitoredService3))
                .thenReturn(testMonitoredServiceDTO3);

        //
        // Execute
        //
        var result = target.findByTenantId(TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertEquals(3, result.size());
        assertSame(testMonitoredServiceDTO1, result.get(0));
        assertSame(testMonitoredServiceDTO2, result.get(1));
        assertSame(testMonitoredServiceDTO3, result.get(2));
    }
}
