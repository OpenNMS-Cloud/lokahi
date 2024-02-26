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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.minion.plugin.api.registries.DetectorRegistry;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.snmp.api.SnmpConfiguration;

public class SnmpConfigMatrixTest {

    @Test
    public void testConfigMatrix() {
        NodeScanner nodeScanner = new NodeScanner(Mockito.mock(SnmpHelper.class), Mockito.mock(DetectorRegistry.class));
        List<SnmpConfiguration> configsFromRequest = new ArrayList<>();
        List<SnmpConfiguration> configurationsWithReadCommunity = new ArrayList<>();
        configurationsWithReadCommunity.add(
                SnmpConfiguration.newBuilder().setReadCommunity("snmp1").build());
        configurationsWithReadCommunity.add(
                SnmpConfiguration.newBuilder().setReadCommunity("snmp2").build());
        configurationsWithReadCommunity.add(
                SnmpConfiguration.newBuilder().setReadCommunity("snmp3").build());
        var list = nodeScanner.deriveSnmpConfigs(configurationsWithReadCommunity, InetAddress.getLoopbackAddress());
        // +1 for default config
        Assertions.assertEquals(configurationsWithReadCommunity.size() + 1, list.size());
        List<SnmpConfiguration> configurationsWithPort = new ArrayList<>();
        configurationsWithPort.add(SnmpConfiguration.newBuilder().setPort(163).build());
        configurationsWithPort.add(SnmpConfiguration.newBuilder().setPort(165).build());
        configsFromRequest.addAll(configurationsWithReadCommunity);
        configsFromRequest.addAll(configurationsWithPort);
        list = nodeScanner.deriveSnmpConfigs(configsFromRequest, InetAddress.getLoopbackAddress());
        // +1 for default config
        Assertions.assertEquals(
                (configurationsWithReadCommunity.size() + 1) * (configurationsWithPort.size() + 1), list.size());
    }
}
