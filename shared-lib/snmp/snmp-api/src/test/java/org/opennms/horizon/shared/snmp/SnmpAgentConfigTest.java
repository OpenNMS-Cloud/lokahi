/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 2016 The OpenNMS Group, Inc.
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
