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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.snmp.api.SnmpConfiguration;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class SnmpConfigMatrixTest {


    @Test
    public void testConfigMatrix() {
        NodeScanner nodeScanner = new NodeScanner(Mockito.mock(SnmpHelper.class));
        List<SnmpConfiguration> configsFromRequest = new ArrayList<>();
        List<SnmpConfiguration> configurationsWithReadCommunity = new ArrayList<>();
        configurationsWithReadCommunity.add(SnmpConfiguration.newBuilder().setReadCommunity("snmp1").build());
        configurationsWithReadCommunity.add(SnmpConfiguration.newBuilder().setReadCommunity("snmp2").build());
        configurationsWithReadCommunity.add(SnmpConfiguration.newBuilder().setReadCommunity("snmp3").build());
        var list = nodeScanner.deriveSnmpConfigs(configurationsWithReadCommunity, InetAddress.getLoopbackAddress());
        // +1 for default config
        Assertions.assertEquals(configurationsWithReadCommunity.size() + 1 , list.size());
        List<SnmpConfiguration> configurationsWithPort = new ArrayList<>();
        configurationsWithPort.add(SnmpConfiguration.newBuilder().setPort(163).build());
        configurationsWithPort.add(SnmpConfiguration.newBuilder().setPort(165).build());
        configsFromRequest.addAll(configurationsWithReadCommunity);
        configsFromRequest.addAll(configurationsWithPort);
        list = nodeScanner.deriveSnmpConfigs(configsFromRequest, InetAddress.getLoopbackAddress());
        // +1 for default config
        Assertions.assertEquals((configurationsWithReadCommunity.size() + 1) * (configurationsWithPort.size() + 1) , list.size());
    }
}
