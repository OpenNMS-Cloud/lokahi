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
package org.opennms.horizon.inventory.mapper.discovery;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;

public class ActiveDiscoveryMapperTest {

    private IcmpActiveDiscoveryMapper mockIcmpActiveDiscoveryMapper;
    private AzureActiveDiscoveryMapper mockAzureActiveDiscoveryMapper;

    private ActiveDiscoveryMapper target;

    @BeforeEach
    public void setUp() {
        mockIcmpActiveDiscoveryMapper = Mockito.mock(IcmpActiveDiscoveryMapper.class);
        mockAzureActiveDiscoveryMapper = Mockito.mock(AzureActiveDiscoveryMapper.class);

        target = new ActiveDiscoveryMapper(mockIcmpActiveDiscoveryMapper, mockAzureActiveDiscoveryMapper);
    }

    @Test
    void testSingleIcmpModelToDto() {
        //
        // Setup Test Data and Interactions
        //
        var testIcmpActiveDiscovery = new IcmpActiveDiscovery();
        var testIcmpActiveDiscoveryDTO = IcmpActiveDiscoveryDTO.newBuilder().build();
        Mockito.when(mockIcmpActiveDiscoveryMapper.modelToDto(testIcmpActiveDiscovery))
                .thenReturn(testIcmpActiveDiscoveryDTO);

        //
        // Execute
        //
        var result = target.modelToDto(testIcmpActiveDiscovery);

        //
        // Verify the Results
        //
        assertTrue(result.hasIcmp());
        assertSame(testIcmpActiveDiscoveryDTO, result.getIcmp());
        assertFalse(result.hasAzure());
    }

    @Test
    void testSingleAzureModelToDto() {
        //
        // Setup Test Data and Interactions
        //
        var testAzureActiveDiscovery = new AzureActiveDiscovery();
        var testAzureActiveDiscoveryDTO = AzureActiveDiscoveryDTO.newBuilder().build();
        Mockito.when(mockAzureActiveDiscoveryMapper.modelToDto(testAzureActiveDiscovery))
                .thenReturn(testAzureActiveDiscoveryDTO);

        //
        // Execute
        //
        var result = target.modelToDto(testAzureActiveDiscovery);

        //
        // Verify the Results
        //
        assertTrue(result.hasAzure());
        assertSame(testAzureActiveDiscoveryDTO, result.getAzure());
        assertFalse(result.hasIcmp());
    }

    @Test
    void testListModelToDto() {
        //
        // Setup Test Data and Interactions
        //

        // One each of ICMP and Azure

        var testIcmpActiveDiscovery = new IcmpActiveDiscovery();
        var testIcmpActiveDiscoveryDTO = IcmpActiveDiscoveryDTO.newBuilder().build();
        Mockito.when(mockIcmpActiveDiscoveryMapper.modelToDto(testIcmpActiveDiscovery))
                .thenReturn(testIcmpActiveDiscoveryDTO);

        var testAzureActiveDiscovery = new AzureActiveDiscovery();
        var testAzureActiveDiscoveryDTO = AzureActiveDiscoveryDTO.newBuilder().build();
        Mockito.when(mockAzureActiveDiscoveryMapper.modelToDto(testAzureActiveDiscovery))
                .thenReturn(testAzureActiveDiscoveryDTO);

        //
        // Execute
        //
        var testList = List.of(testIcmpActiveDiscovery, testAzureActiveDiscovery);
        var result = target.modelToDto(testList);

        //
        // Verify the Results
        //
        assertEquals(2, result.size());
        assertSame(testIcmpActiveDiscoveryDTO, result.get(0).getIcmp());
        assertSame(testAzureActiveDiscoveryDTO, result.get(1).getAzure());
    }
}
