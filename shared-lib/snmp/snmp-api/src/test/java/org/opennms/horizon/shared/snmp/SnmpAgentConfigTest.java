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
package org.opennms.horizon.shared.snmp;

import org.junit.Assert;
import org.junit.Test;

public class SnmpAgentConfigTest {

    @Test
    public void testEqualsAndHashCode() {
        SnmpAgentConfig config = new SnmpAgentConfig();
        SnmpAgentConfig config2 = new SnmpAgentConfig();

        Assert.assertEquals(config, config2);
        Assert.assertEquals(config.hashCode(), config2.hashCode());

        fillAll(config);
        Assert.assertFalse(config.equals(config2));
        Assert.assertFalse(config.hashCode() == config2.hashCode());

        fillAll(config2);
        Assert.assertEquals(config, config2);
        Assert.assertEquals(config.hashCode(), config2.hashCode());
    }

    private void fillAll(SnmpAgentConfig config) {
        config.setTimeout(12);
        config.setAuthPassPhrase("some random pass phrase");
        config.setAuthProtocol("some random protocol");
        config.setContextEngineId("some context engine id");
        config.setContextName("some context name");
        config.setEngineId("some engine id");
        config.setEnterpriseId("some enterprise id");
        config.setMaxRepetitions(34);
        config.setMaxRequestSize(56);
        config.setMaxVarsPerPdu(78);
        config.setPort(99);
        config.setPrivPassPhrase("some random private pass phrase");
        config.setPrivProtocol("some random private protocol");
        config.setReadCommunity("read community string");
        config.setWriteCommunity("write community string");
        config.setRetries(17);
        config.setSecurityLevel(3);
        config.setSecurityName("dummy");
        config.setVersion(3);
    }

    @Test
    public void canConvertToAndFromMap() {
        SnmpAgentConfig config = new SnmpAgentConfig();
        Assert.assertEquals(config, SnmpAgentConfig.fromMap(config.toMap()));
    }
}
