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
package org.opennms.horizon.minion.snmp;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import org.junit.Test;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.snmp.api.SnmpConfiguration;

public class SnmpAgentConfigTest {

    @Test
    public void testAgentConfigDefaults() throws Exception {

        SnmpConfiguration snmpConfiguration = SnmpConfiguration.newBuilder().build();
        SnmpAgentConfig agentConfig = SnmpCollector.mapAgent(
                snmpConfiguration, InetAddress.getLocalHost().getHostAddress());
        assertEquals("public", agentConfig.getReadCommunity());
        assertEquals("private", agentConfig.getWriteCommunity());
        assertEquals(3000, agentConfig.getTimeout());
    }
}
