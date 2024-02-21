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
package org.opennms.horizon.inventory.service.taskset;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.icmp.contract.PingSweepRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.taskset.contract.TaskDefinition;

@ExtendWith(MockitoExtension.class)
public class ScannerTaskSetServiceTest {
    @Mock
    private TaskSetPublisher mockPublisher;

    @Mock
    private SnmpConfigService configService;

    @InjectMocks
    private ScannerTaskSetService service;

    @Captor
    ArgumentCaptor<List<TaskDefinition>> taskListCaptor;

    private final String tenantId = "testTenant";
    private final Long locationId = 1020304050L;
    private NodeDTO.Builder nodeBuilder;
    private IpInterfaceDTO ipInterface1;
    private IpInterfaceDTO ipInterface2;

    @BeforeEach
    void prepareTest() {
        ipInterface1 = IpInterfaceDTO.newBuilder()
                .setIpAddress("127.0.0.1")
                .setSnmpPrimary(true)
                .build();
        ipInterface2 = IpInterfaceDTO.newBuilder().setIpAddress("127.0.0.1").build();
        nodeBuilder = NodeDTO.newBuilder().setId(1L);
    }

    @AfterEach
    void afterTest() {
        verifyNoMoreInteractions(mockPublisher);
    }

    @Test
    void testSendNodeScanWithTwoIpInterfaces() throws InvalidProtocolBufferException {
        NodeDTO node = nodeBuilder
                .addAllIpInterfaces(List.of(ipInterface1, ipInterface2))
                .build();
        service.sendNodeScannerTask(List.of(node), locationId, tenantId);
        verify(mockPublisher).publishNewTasks(eq(tenantId), eq(locationId), taskListCaptor.capture());
        List<TaskDefinition> tasks = taskListCaptor.getValue();
        assertThat(tasks).asList().hasSize(1).extracting("nodeId_").containsExactly(node.getId());
        NodeScanRequest request = tasks.get(0).getConfiguration().unpack(NodeScanRequest.class);
        assertThat(request)
                .extracting(NodeScanRequest::getNodeId, NodeScanRequest::getPrimaryIp)
                .containsExactly(node.getId(), ipInterface1.getIpAddress());
    }

    @Test
    void testSendNodeScanWithIpInterfaceNonPrimary() throws InvalidProtocolBufferException {
        NodeDTO node = nodeBuilder.addAllIpInterfaces(List.of(ipInterface2)).build();
        service.sendNodeScannerTask(List.of(node), locationId, tenantId);
        verify(mockPublisher).publishNewTasks(eq(tenantId), eq(locationId), taskListCaptor.capture());
        List<TaskDefinition> tasks = taskListCaptor.getValue();
        assertThat(tasks).asList().hasSize(1).extracting("nodeId_").containsExactly(node.getId());
        NodeScanRequest request = tasks.get(0).getConfiguration().unpack(NodeScanRequest.class);
        assertThat(request)
                .extracting(NodeScanRequest::getNodeId, NodeScanRequest::getPrimaryIp)
                .containsExactly(node.getId(), ipInterface2.getIpAddress());
    }

    @Test
    void testSendNodeScanWithoutIpInterfaces() {
        NodeDTO node = nodeBuilder.build();
        service.sendNodeScannerTask(List.of(node), locationId, tenantId);
        verifyNoInteractions(mockPublisher);
    }

    @ParameterizedTest
    @CsvSource({
        "192.168.34.0/24, 192.168.34.0, 192.168.34.255",
        "192.168.45.1-192.168.45.254, 192.168.45.1, 192.168.45.254",
        "192.168.2.45, 192.168.2.45, 192.168.2.45"
    })
    void testIpAddressParsing(String ipAddressNotation, String begin, String end)
            throws InvalidProtocolBufferException {

        var optional = service.createDiscoveryTask(List.of(ipAddressNotation), locationId, 1);
        Assertions.assertTrue(optional.isPresent());
        var ipRanges = optional.get().getConfiguration().unpack(PingSweepRequest.class);
        Assertions.assertEquals(begin, ipRanges.getIpRange(0).getBegin());
        Assertions.assertEquals(end, ipRanges.getIpRange(0).getEnd());
    }
}
