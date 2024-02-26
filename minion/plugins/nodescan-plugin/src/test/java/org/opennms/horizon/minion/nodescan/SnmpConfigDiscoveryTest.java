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
package org.opennms.horizon.minion.nodescan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JValue;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.snmp4j.smi.Integer32;

class SnmpConfigDiscoveryTest {
    @Mock
    private SnmpHelper snmpHelper;

    private SnmpConfigDiscovery snmpConfigDiscovery;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        snmpConfigDiscovery = new SnmpConfigDiscovery(snmpHelper);
    }

    @Test
    void detectSNMP() {
        SnmpAgentConfig config1 =
                new SnmpAgentConfig(InetAddressUtils.getInetAddress("127.0.0.1"), SnmpConfiguration.DEFAULTS);
        SnmpAgentConfig config2 =
                new SnmpAgentConfig(InetAddressUtils.getInetAddress("192.168.1.1"), SnmpConfiguration.DEFAULTS);
        List<SnmpAgentConfig> configs = Arrays.asList(config1, config2);

        SnmpValue[] snmpValues1 = new SnmpValue[] {new Snmp4JValue(new Integer32(1))};
        SnmpValue[] snmpValues2 = new SnmpValue[0];

        when(snmpHelper.getAsync(config1, new SnmpObjId[] {SnmpObjId.get(SnmpHelper.SYS_OBJECTID_INSTANCE)}))
                .thenReturn(CompletableFuture.completedFuture(snmpValues1));
        when(snmpHelper.getAsync(config2, new SnmpObjId[] {SnmpObjId.get(SnmpHelper.SYS_OBJECTID_INSTANCE)}))
                .thenReturn(CompletableFuture.completedFuture(snmpValues2));

        List<SnmpAgentConfig> detectedConfigs = snmpConfigDiscovery.getDiscoveredConfig(configs);

        assertEquals(1, detectedConfigs.size());
        assertEquals(config1, detectedConfigs.get(0));

        verify(snmpHelper, times(2)).getAsync(any(), any());
    }
}
