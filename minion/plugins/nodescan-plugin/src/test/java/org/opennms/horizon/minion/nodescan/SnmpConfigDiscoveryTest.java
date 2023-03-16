/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.nodescan;

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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        SnmpAgentConfig config1 = new SnmpAgentConfig(InetAddressUtils.getInetAddress("127.0.0.1"), SnmpConfiguration.DEFAULTS);
        SnmpAgentConfig config2 = new SnmpAgentConfig(InetAddressUtils.getInetAddress("192.168.1.1"), SnmpConfiguration.DEFAULTS);
        List<SnmpAgentConfig> configs = Arrays.asList(config1, config2);

        SnmpValue snmpValues1 = new Snmp4JValue(new Integer32(1));
        SnmpValue snmpValues2 = null;

        when(snmpHelper.get(config1, SnmpObjId.get(SnmpHelper.SYS_OBJECTID_INSTANCE)))
            .thenReturn(snmpValues1);
        when(snmpHelper.get(config2, SnmpObjId.get(SnmpHelper.SYS_OBJECTID_INSTANCE)))
            .thenReturn(snmpValues2);

        List<SnmpAgentConfig> detectedConfigs = snmpConfigDiscovery.getDiscoveredConfig(configs);

        assertEquals(1, detectedConfigs.size());
        assertEquals(config1, detectedConfigs.get(0));

        verify(snmpHelper, times(2)).get(any(), (SnmpObjId) any());
    }
}

