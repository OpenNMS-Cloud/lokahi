/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023  The OpenNMS Group, Inc.
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.isA;

@SpringBootTest(classes = {AzureInterfaceMapperImpl.class, EmptyStringMapperImpl.class, IpAddressMapperImpl.class,AzureInterface.class})
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
            AzureInterfaceDTO.newBuilder()
                .setId(azureInterfaceId)
                .build();

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
                .setIpAddress(publicIp).setLocation(azureLocation)).build();

        Mockito.when(mockAzureInterfaceRepository.findByIdAndTenantId(azureInterfaceId, TEST_TENANT_ID))
            .thenReturn(Optional.empty());
        Mockito.when(mockAzureInterfaceRepository.save(isA(AzureInterface.class))).thenAnswer(in -> in.getArguments()[0]);

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
                .setIpAddress(publicIp).setLocation(azureLocation)).build();


        Mockito.when(mockAzureInterfaceRepository.findByTenantIdAndPublicIpId(TEST_TENANT_ID, publicIpId))
            .thenReturn(Optional.of(azureInterface));
        Mockito.when(mockAzureInterfaceRepository.save(isA(AzureInterface.class))).thenAnswer(in -> in.getArguments()[0]);

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
