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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.SnmpInterfaceRepository;
import org.opennms.node.scan.contract.SnmpInterfaceResult;

public class SnmpInterfaceServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";
    public static final String TEST_LOCATION = "x-location-x";

    private SnmpInterfaceRepository mockSnmpInterfaceRepository;
    private SnmpInterfaceMapper mockSnmpInterfaceMapper;

    private SnmpInterface testSnmpInterface1;
    private SnmpInterface testSnmpInterface2;
    private SnmpInterface testSnmpInterface3;

    private SnmpInterfaceDTO testSnmpInterfaceDTO1;
    private SnmpInterfaceDTO testSnmpInterfaceDTO2;
    private SnmpInterfaceDTO testSnmpInterfaceDTO3;
    private Node testNode;
    private SnmpInterfaceResult testSnmpInterfaceResult;

    private SnmpInterfaceService target;

    @BeforeEach
    public void setUp() {
        mockSnmpInterfaceRepository = Mockito.mock(SnmpInterfaceRepository.class);
        mockSnmpInterfaceMapper = Mockito.mock(SnmpInterfaceMapper.class);
        testSnmpInterface1 = new SnmpInterface();
        testSnmpInterface2 = new SnmpInterface();
        testSnmpInterface3 = new SnmpInterface();

        testSnmpInterfaceDTO1 = SnmpInterfaceDTO.newBuilder().build();
        testSnmpInterfaceDTO2 = SnmpInterfaceDTO.newBuilder().build();
        testSnmpInterfaceDTO3 = SnmpInterfaceDTO.newBuilder().build();

        testNode = new Node();
        testNode.setId(1313);
        testSnmpInterfaceResult = SnmpInterfaceResult.newBuilder().setIfIndex(1).build();

        target = new SnmpInterfaceService(mockSnmpInterfaceRepository, mockSnmpInterfaceMapper);
    }

    @Test
    void testFindByTenantId() {
        //
        // Setup Test Data and Interactions
        //
        var testSnmpInterfaceList = List.of(testSnmpInterface1, testSnmpInterface2, testSnmpInterface3);
        Mockito.when(mockSnmpInterfaceRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(testSnmpInterfaceList);
        Mockito.when(mockSnmpInterfaceMapper.modelToDTO(testSnmpInterface1)).thenReturn(testSnmpInterfaceDTO1);
        Mockito.when(mockSnmpInterfaceMapper.modelToDTO(testSnmpInterface2)).thenReturn(testSnmpInterfaceDTO2);
        Mockito.when(mockSnmpInterfaceMapper.modelToDTO(testSnmpInterface3)).thenReturn(testSnmpInterfaceDTO3);

        //
        // Execute
        //
        var result = target.findByTenantId(TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertEquals(3, result.size());
        assertSame(testSnmpInterfaceDTO1, result.get(0));
        assertSame(testSnmpInterfaceDTO2, result.get(1));
        assertSame(testSnmpInterfaceDTO3, result.get(2));
    }

    @Test
    void testCreateFromScanResult() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockSnmpInterfaceRepository.findByNodeIdAndTenantIdAndIfIndex(1313, TEST_TENANT_ID, 1))
                .thenReturn(Optional.of(testSnmpInterface1));

        //
        // Execute
        //
        var result = target.createOrUpdateFromScanResult(TEST_TENANT_ID, testNode, testSnmpInterfaceResult);

        //
        // Verify the Results
        //
        Mockito.verify(mockSnmpInterfaceMapper).updateFromScanResult(testSnmpInterfaceResult, result);
        Mockito.verify(mockSnmpInterfaceMapper).updateFromScanResult(testSnmpInterfaceResult, result);
        Mockito.verify(mockSnmpInterfaceRepository).save(result);
    }

    @Test
    void testCreateFromScanResultNotFound() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockSnmpInterfaceRepository.findByNodeIdAndTenantIdAndIfIndex(1313, TEST_TENANT_ID, 1))
                .thenReturn(Optional.empty());
        Mockito.when(mockSnmpInterfaceMapper.scanResultToModel(testSnmpInterfaceResult))
                .thenReturn(testSnmpInterface1);
        Mockito.when(mockSnmpInterfaceRepository.save(Mockito.argThat(argument -> ((argument == testSnmpInterface1)
                        && (argument.getNode() == testNode)
                        && (Objects.equals(TEST_TENANT_ID, argument.getTenantId()))))))
                .thenReturn(testSnmpInterface2);

        //
        // Execute
        //
        var result = target.createOrUpdateFromScanResult(TEST_TENANT_ID, testNode, testSnmpInterfaceResult);

        //
        // Verify the Results
        //
        Mockito.verify(mockSnmpInterfaceRepository).save(testSnmpInterface1);
        assertEquals(TEST_TENANT_ID, testSnmpInterface1.getTenantId());
    }
}
