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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import io.grpc.Context;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.SnmpInterfaceRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import jakarta.transaction.Transactional;


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

    @Autowired
    private SnmpInterfaceRepository snmpInterfaceRepository;

    @Autowired
    private NodeService nodeService;

    private static TestTaskSetGrpcService testGrpcService;

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterEach
    public void cleanUp() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            ipInterfaceRepository.deleteAll();
            nodeRepository.deleteAll();
            credentialRepository.deleteAll();
            locationRepository.deleteAll();
        });
        testGrpcService.reset();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    void testAzureAccept() throws Exception {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
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

            try {
                service.accept(TEST_TENANT_ID, TEST_LOCATION, response);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            // monitor and collect tasks
            assertEquals(2, testGrpcService.getRequests().size());

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
        });
    }

    @Test
    void testAcceptNodeScanResult() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            String managedIp = "127.0.0.1";
            Node node = createNode(managedIp);
            int ifIndex = 1;
            SnmpInterface snmpIf = createSnmpInterface(node, ifIndex);
            NodeScanResult result = createNodeScanResult(node.getId(), managedIp, ifIndex);

            try {
                service.accept(TEST_TENANT_ID, TEST_LOCATION, ScannerResponse.newBuilder().setResult(Any.pack(result)).build());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
            assertNodeSystemGroup(node, null);
            nodeRepository.findByIdAndTenantId(node.getId(), TEST_TENANT_ID).ifPresentOrElse(dbNode ->
                assertNodeSystemGroup(dbNode, result.getNodeInfo()), () -> fail("Node not found"));

            assertIpInterface(node.getIpInterfaces().get(0), null);
            List<IpInterface> ipIfList = ipInterfaceRepository.findByNodeId(node.getId());
            assertThat(ipIfList.get(0)).extracting(ipIf -> ipIf.getIpAddress().getHostAddress()).isEqualTo(managedIp);
            assertThat(ipIfList).asList().hasSize(result.getIpInterfacesList().size());
            IntStream.range(0, ipIfList.size())
                .forEach(i -> assertIpInterface(ipIfList.get(i), result.getIpInterfaces(i)));

            List<SnmpInterface> snmpInterfaceList = snmpInterfaceRepository.findByTenantId(TEST_TENANT_ID);
            assertThat(snmpInterfaceList).asList().hasSize(2);
            assertThat(snmpIf.getIfIndex()).isEqualTo(ifIndex);
            assertSnmpInterfaces(snmpIf, null);
            IntStream.range(0, snmpInterfaceList.size())
                .forEach(i -> assertSnmpInterfaces(snmpInterfaceList.get(i), result.getSnmpInterfaces(i)));
        });
    }

    private Node createNode(String ipAddress) {
        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp(ipAddress)
            .setLocation(TEST_LOCATION)
            .build();
        return nodeService.createNode(createDTO, TEST_TENANT_ID);
    }

    private SnmpInterface createSnmpInterface(Node node, int ifIndex) {
        SnmpInterface snmpIf = new SnmpInterface();
        snmpIf.setTenantId(TEST_TENANT_ID);
        snmpIf.setNode(node);
        snmpIf.setIfIndex(ifIndex);
        snmpInterfaceRepository.save(snmpIf);
        return snmpIf;
    }

    private NodeScanResult createNodeScanResult(long nodeId, String ipAddress, int ifIndex) {
        NodeInfoResult nodeInfo = NodeInfoResult.newBuilder()
            .setObjectId(".1.2.3.4.5")
            .setSystemName("Test System")
            .setSystemDescr("Test device")
            .setSystemLocation("Somewhere")
            .setSystemContact("admin@opennms.com")
            .build();
        IpInterfaceResult ipIf1 = IpInterfaceResult.newBuilder()
                .setIpAddress(ipAddress)
                .setIpHostName("hostname1")
                .setNetmask("255.255.255.0")
                .build();
        IpInterfaceResult ipIf2 = IpInterfaceResult.newBuilder()
                .setIpAddress("192.168.2.3")
                .setNetmask("255.255.0.0")
                .setIpHostName("hostname-2")
                .build();
        SnmpInterfaceResult snmpIf1 = SnmpInterfaceResult.newBuilder()
            .setIfIndex(ifIndex)
            .setIfDescr("SNMP Interface1")
            .setIfName("testIf1")
            .setIfSpeed(1000L)
            .setIfAdminStatus(1)
            .setIfOperatorStatus(2)
            .setIfAlias("alias1")
            .setIpAddress(ipAddress)
            .setPhysicalAddr("0sdfasdf")
            .build();
        SnmpInterfaceResult snmpIf2 = SnmpInterfaceResult.newBuilder()
            .setIfIndex(ifIndex+2)
            .setIfDescr("SNMP Interface2")
            .setIfName("testIf2")
            .setIfSpeed(2000L)
            .setIfAdminStatus(4)
            .setIfOperatorStatus(5)
            .setIfAlias("alias2")
            .setPhysicalAddr("owradfasdqwr00")
            .build();

        return NodeScanResult.newBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(nodeInfo)
            .addAllIpInterfaces(List.of(ipIf1, ipIf2))
            .addAllSnmpInterfaces(List.of(snmpIf1, snmpIf2))
            .build();
    }

    private AzureCredential createAzureCredential() {

        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(TEST_TENANT_ID);
        location = locationRepository.save(location);

        AzureCredential credential = new AzureCredential();
        credential.setTenantId(TEST_TENANT_ID);
        credential.setName("name");
        credential.setClientId("client-id");
        credential.setClientSecret("client-secret");
        credential.setDirectoryId("directory-id");
        credential.setSubscriptionId("sub-id");
        credential.setCreateTime(LocalDateTime.now());
        credential.setMonitoringLocation(location);

        return credentialRepository.save(credential);
    }

    private void assertNodeSystemGroup(Node node, NodeInfoResult nodeInfo) {
        if(nodeInfo != null) {
            assertThat(node)
                .extracting(Node::getObjectId,
                    Node::getSystemName,
                    Node::getSystemDescr,
                    Node::getSystemLocation,
                    Node::getSystemContact)
                .containsExactly(nodeInfo.getObjectId(),
                    nodeInfo.getSystemName(),
                    nodeInfo.getSystemDescr(),
                    nodeInfo.getSystemLocation(),
                    nodeInfo.getSystemContact());
        } else {
            assertThat(node)
                .extracting(Node::getObjectId,
                    Node::getSystemName,
                    Node::getSystemDescr,
                    Node::getSystemLocation,
                    Node::getSystemContact)
                .containsExactly(null,null, null, null, null);
        }
    }

    private void assertIpInterface(IpInterface ipInterface, IpInterfaceResult scanResult) {
        if(scanResult != null) {
            assertThat(ipInterface)
                .extracting(ipIf -> ipIf.getIpAddress().getHostAddress(), IpInterface::getHostname, IpInterface::getNetmask)
                .containsExactly(scanResult.getIpAddress(), scanResult.getIpHostName(), scanResult.getNetmask());
        } else {
            assertThat(ipInterface)
                .extracting(IpInterface::getHostname, IpInterface::getNetmask)
                .containsExactly(null, null);
        }
    }

   private void assertSnmpInterfaces(SnmpInterface snmpIf, SnmpInterfaceResult result) {
        if(result != null) {
            assertThat(snmpIf)
                .extracting(SnmpInterface::getIfIndex,
                    SnmpInterface::getIfName,
                    SnmpInterface::getIfDescr,
                    SnmpInterface::getIfType,
                    SnmpInterface::getIfSpeed,
                    SnmpInterface::getIfAdminStatus,
                    SnmpInterface::getIfOperatorStatus,
                    SnmpInterface::getIfAlias,
                    snmpInterface -> snmpInterface.getIpAddress() == null ? null: snmpInterface.getIpAddress().getHostAddress(),
                    SnmpInterface::getPhysicalAddr)
                .containsExactly(result.getIfIndex(),
                    result.getIfName(),
                    result.getIfDescr(),
                    result.getIfType(),
                    result.getIfSpeed(),
                    result.getIfAdminStatus(),
                    result.getIfOperatorStatus(),
                    result.getIfAlias(),
                    StringUtils.isEmpty(result.getIpAddress())? null: result.getIpAddress(),
                    result.getPhysicalAddr());
        } else {
            assertThat(snmpIf)
                .extracting(SnmpInterface::getIfName,
                    SnmpInterface::getIfDescr,
                    SnmpInterface::getIfType,
                    SnmpInterface::getIfSpeed,
                    SnmpInterface::getIfAdminStatus,
                    SnmpInterface::getIfOperatorStatus,
                    SnmpInterface::getIfAlias,
                    SnmpInterface::getIpAddress,
                    SnmpInterface::getPhysicalAddr)
                .containsExactly(null,null,0,0L,0,0,null,null,null);
        }
    }
}
