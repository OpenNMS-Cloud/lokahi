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

package org.opennms.horizon.inventory.repository;

import io.grpc.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.SnmpAgentConfig;
import org.opennms.horizon.inventory.model.SnmpConfig;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@AutoConfigureObservability
// Make sure to include Metrics (for some reason they are disabled by default in the integration grey-box test)
public class SnmpConfigRepositoryTest {

    private static final String tenantId = "tenant-1";

    @Autowired
    private SnmpConfigRepository snmpConfigRepository;

    @Test
    public void testSnmpConfigPersistence() {

        var snmpAgentConfig = new SnmpAgentConfig();
        snmpAgentConfig.setPort(1161);
        snmpAgentConfig.setReadCommunity("OpenNMS");
        snmpAgentConfig.setWriteCommunity("private");
        var snmpConfig = new SnmpConfig();
        snmpConfig.setTenantId(tenantId);
        snmpConfig.setLocation("MINION");
        snmpConfig.setIpAddress(InetAddressUtils.getInetAddress("192.168.1.1"));
        snmpConfig.setTenantId(tenantId);
        snmpConfig.setSnmpAgentConfig(snmpAgentConfig);
        var persisted = snmpConfigRepository.save(snmpConfig);
        Assertions.assertNotNull(persisted);
        Assertions.assertEquals("v1", snmpConfig.getSnmpAgentConfig().getVersion());
        Assertions.assertEquals("OpenNMS", snmpConfig.getSnmpAgentConfig().getReadCommunity());
        Assertions.assertEquals("private", snmpConfig.getSnmpAgentConfig().getWriteCommunity());
        Assertions.assertEquals(1161, snmpConfig.getSnmpAgentConfig().getPort());

        var optional = snmpConfigRepository
            .findByTenantIdAndLocationAndIpAddress(tenantId, "MINION", InetAddressUtils.getInetAddress("192.168.1.1"));

        Assertions.assertTrue(optional.isPresent());


    }

    @AfterEach
    public void destroy() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(() ->
        {
            snmpConfigRepository.deleteAll();
        });
    }
}
