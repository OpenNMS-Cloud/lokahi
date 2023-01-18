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

package org.opennms.horizon.inventory.service.taskset.response;

import com.google.protobuf.Any;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class ScannerResponseServiceIntTest extends GrpcTestBase {
    private static final String TEST_LOCATION = "Default";
    private static final String TEST_TENANT_ID = "test-tenant-id";

    @Autowired
    private ScannerResponseService service;

    @Autowired
    private MonitoringLocationRepository locationRepository;

    @Autowired
    private AzureCredentialRepository credentialRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;

    private static TestTaskSetGrpcService testGrpcService;

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterEach
    @Transactional
    public void cleanUp() {
        ipInterfaceRepository.deleteAll();
        nodeRepository.deleteAll();
        credentialRepository.deleteAll();
        locationRepository.deleteAll();
        testGrpcService.reset();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    @Transactional
    void testAzureAccept() throws Exception {
        AzureCredential credential = createAzureCredential();

        AzureScanItem scanItem = AzureScanItem.newBuilder()
            .setId("/subscriptions/sub-id/resourceGroups/resource-group/providers/Microsoft.Compute/virtualMachines/vm-name")
            .setName("vm-name")
            .setResourceGroup("resource-group")
            .setCredentialId(credential.getId())
            .build();

        List<AzureScanItem> azureScanItems = Collections.singletonList(scanItem);
        AzureScanResponse azureScanResponse = AzureScanResponse.newBuilder().addAllResults(azureScanItems).build();
        ScannerResponse response = ScannerResponse.newBuilder().setResult(Any.pack(azureScanResponse)).build();

        service.accept(TEST_TENANT_ID, TEST_LOCATION, response);

        // monitor and collect tasks
        assertEquals(2, testGrpcService.getTimesCalled().get());

        List<Node> allNodes = nodeRepository.findAll();
        assertEquals(1, allNodes.size());

        Node node = allNodes.get(0);
        assertNotNull(node);
        assertEquals(TEST_TENANT_ID, node.getTenantId());
        assertEquals("vm-name (resource-group)", node.getNodeLabel());
        assertNotNull(node.getMonitoringLocation());

        List<IpInterface> allIpInterfaces = ipInterfaceRepository.findAll();
        assertEquals(1, allIpInterfaces.size());
        IpInterface ipInterface = allIpInterfaces.get(0);
        assertEquals(TEST_TENANT_ID, ipInterface.getTenantId());
        assertEquals("127.0.0.1", InetAddressUtils.toIpAddrString(ipInterface.getIpAddress()));
    }

    private AzureCredential createAzureCredential() {

        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(TEST_TENANT_ID);
        location = locationRepository.save(location);

        AzureCredential credential = new AzureCredential();
        credential.setTenantId(TEST_TENANT_ID);
        credential.setClientId("client-id");
        credential.setClientSecret("client-secret");
        credential.setDirectoryId("directory-id");
        credential.setSubscriptionId("sub-id");
        credential.setCreateTime(LocalDateTime.now());
        credential.setMonitoringLocation(location);

        return credentialRepository.save(credential);
    }

}
