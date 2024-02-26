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

import static org.mockito.ArgumentMatchers.isA;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.AzureInterfaceDTO;
import org.opennms.horizon.inventory.mapper.AzureInterfaceMapper;
import org.opennms.horizon.inventory.mapper.AzureInterfaceMapperImpl;
import org.opennms.horizon.inventory.mapper.EmptyStringMapperImpl;
import org.opennms.horizon.inventory.mapper.IpAddressMapperImpl;
import org.opennms.horizon.inventory.model.AzureInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.AzureInterfaceRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = {
            AzureInterfaceMapperImpl.class,
            EmptyStringMapperImpl.class,
            IpAddressMapperImpl.class,
            AzureInterface.class
        })
public class AzureInterfaceServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    final String interfaceName = "interfaceName";
    final String azureLocation = "eastus";
    final long azureInterfaceId = 1313L;
    final String publicIpId = "publicIpId";
    final String publicIp = "8.8.8.8";
    final String privateIpId = "privateIpId";

    private AzureInterfaceRepository mockAzureInterfaceRepository;

    @Autowired
    private AzureInterfaceMapper azureInterfaceMapper;

    private AzureInterfaceService target;

    @BeforeEach
    public void setUp() {
        mockAzureInterfaceRepository = Mockito.mock(AzureInterfaceRepository.class);
        target = new AzureInterfaceService(mockAzureInterfaceRepository, azureInterfaceMapper);
    }

    @Test
    void testGetByIdTenantId() {
        //
        // Setup Test Data and Interactions
        //
        var testAzureInterface = new AzureInterface();
        testAzureInterface.setId(azureInterfaceId);

        var testAzureInterfaceDTO =
                AzureInterfaceDTO.newBuilder().setId(azureInterfaceId).build();

        Mockito.when(mockAzureInterfaceRepository.findByIdAndTenantId(azureInterfaceId, TEST_TENANT_ID))
                .thenReturn(Optional.of(testAzureInterface));

        //
        // Execute
        //
        Optional<AzureInterfaceDTO> result = target.findByIdAndTenantId(azureInterfaceId, TEST_TENANT_ID);

        //
        // Verify the Results
        //
        Assertions.assertEquals(testAzureInterfaceDTO, result.get());
    }

    @Test
    void testCreateFromScanResult() {
        //
        // Setup Test Data and Interactions
        //
        Node node = new Node();
        node.setId(1);
        AzureScanNetworkInterfaceItem networkInterfaceItem = AzureScanNetworkInterfaceItem.newBuilder()
                .setInterfaceName(interfaceName)
                .setName(privateIpId)
                .setLocation(azureLocation)
                .setPublicIpAddress(AzureScanNetworkInterfaceItem.newBuilder()
                        .setName(publicIpId)
                        .setIpAddress(publicIp)
                        .setLocation(azureLocation))
                .build();

        Mockito.when(mockAzureInterfaceRepository.findByIdAndTenantId(azureInterfaceId, TEST_TENANT_ID))
                .thenReturn(Optional.empty());
        Mockito.when(mockAzureInterfaceRepository.save(isA(AzureInterface.class)))
                .thenAnswer(in -> in.getArguments()[0]);

        //
        // Execute
        //
        var result = target.createOrUpdateFromScanResult(TEST_TENANT_ID, node, networkInterfaceItem);

        //
        // Verify the Results
        //
        Assertions.assertEquals(interfaceName, result.getInterfaceName());
        Assertions.assertEquals(TEST_TENANT_ID, result.getTenantId());
        Assertions.assertEquals(azureLocation, result.getLocation());
        Assertions.assertEquals(publicIpId, result.getPublicIpId());
        Assertions.assertEquals(privateIpId, result.getPrivateIpId());
        Assertions.assertEquals(node.getId(), result.getNode().getId());
        Assertions.assertEquals(InetAddressUtils.getInetAddress(publicIp), result.getPublicIpAddress());
    }

    @Test
    void testUpdateFromScanResult() {
        //
        // Setup Test Data and Interactions
        //
        Node node = new Node();
        node.setId(1);
        AzureInterface azureInterface = new AzureInterface();
        azureInterface.setId(azureInterfaceId);
        azureInterface.setNode(node);
        azureInterface.setPrivateIpId(privateIpId);
        azureInterface.setTenantId(TEST_TENANT_ID);
        AzureScanNetworkInterfaceItem networkInterfaceItem = AzureScanNetworkInterfaceItem.newBuilder()
                .setInterfaceName(interfaceName)
                .setName(privateIpId)
                .setLocation(azureLocation)
                .setPublicIpAddress(AzureScanNetworkInterfaceItem.newBuilder()
                        .setName(publicIpId)
                        .setIpAddress(publicIp)
                        .setLocation(azureLocation))
                .build();

        Mockito.when(mockAzureInterfaceRepository.findByTenantIdAndPublicIpId(TEST_TENANT_ID, publicIpId))
                .thenReturn(Optional.of(azureInterface));
        Mockito.when(mockAzureInterfaceRepository.save(isA(AzureInterface.class)))
                .thenAnswer(in -> in.getArguments()[0]);

        //
        // Execute
        //
        var result = target.createOrUpdateFromScanResult(TEST_TENANT_ID, node, networkInterfaceItem);

        //
        // Verify the Results
        //
        Assertions.assertEquals(azureInterfaceId, result.getId());
        Assertions.assertEquals(interfaceName, result.getInterfaceName());
        Assertions.assertEquals(TEST_TENANT_ID, result.getTenantId());
        Assertions.assertEquals(azureLocation, result.getLocation());
        Assertions.assertEquals(publicIpId, result.getPublicIpId());
        Assertions.assertEquals(privateIpId, result.getPrivateIpId());
        Assertions.assertEquals(node.getId(), result.getNode().getId());
        Assertions.assertEquals(InetAddressUtils.getInetAddress(publicIp), result.getPublicIpAddress());
    }
}
