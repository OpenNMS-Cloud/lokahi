/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.traps.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.core.ipc.twin.api.TwinPublisher;
import org.opennms.horizon.config.service.api.ConfigConstants;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.shared.snmp.TrapListenerConfig;
import org.opennms.horizon.traps.config.TrapdConfigBean;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TrapdConfigJsonTest {

    @Test
    public void testTrapdConfigInJson() throws IOException {
        TrapSinkConsumer trapSinkConsumer = new TrapSinkConsumer();
        ConfigService configService = new MockConfigService();
        TwinPublisher twinPublisher = Mockito.mock(TwinPublisher.class);
        trapSinkConsumer.setTwinPublisher(twinPublisher);
        trapSinkConsumer.setConfigService(configService);
        trapSinkConsumer.setTwinSession(new TwinPublisher.Session<TrapListenerConfig>() {
            @Override
            public void publish(TrapListenerConfig obj) throws IOException {
                //Ignore
            }

            @Override
            public void close() throws IOException {

            }
        });
        trapSinkConsumer.initializeConfig();
        Optional<String> optionalConfig = configService.getConfig(ConfigConstants.SNMP_TRAPS_CONFIG);
        Assert.assertTrue("Config must be present", optionalConfig.isPresent());
        ObjectMapper objectMapper = new ObjectMapper();
        TrapdConfigBean configBean = objectMapper.readValue(optionalConfig.get(), TrapdConfigBean.class);
        Assert.assertEquals(10162, configBean.getSnmpTrapPort());
        Assert.assertEquals("*", configBean.getSnmpTrapAddress());
        Assert.assertEquals(500, configBean.getBatchIntervalMs());
        Assert.assertEquals(10000, configBean.getQueueSize());
        Assert.assertTrue("snmpv3 users must be empty", configBean.getSnmpV3Users().isEmpty());
    }

    static class MockConfigService implements ConfigService {

        Map<String, String> configs = new HashMap<>();

        @Override
        public void addConfig(String configName, String jsonConfig, String source) {
            configs.put(configName, jsonConfig);
        }

        @Override
        public void updateConfig(String configName, String jsonConfig, String source) {

        }

        @Override
        public Optional<String> getConfig(String configName) {
            return Optional.ofNullable(configs.get(configName));
        }

        @Override
        public List<String> getConfigNames() {
            return null;
        }
    }
}
