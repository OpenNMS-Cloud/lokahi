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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opennms.horizon.config.service.api.ConfigConstants;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.shared.snmp.traps.TrapdConfigBean;
import org.opennms.horizon.taskset.manager.TaskSetManager;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class TrapdConfigJsonTest {

    @Test
    public void testTrapdConfigInJson() throws IOException {
        TaskSetManager mockTaskSetManager = Mockito.mock(TaskSetManager.class);
        ConfigService mockConfigService = Mockito.mock(ConfigService.class);

        TrapSinkConsumer trapSinkConsumer = new TrapSinkConsumer();
        trapSinkConsumer.setConfigService(mockConfigService);
        trapSinkConsumer.setTaskSetManager(mockTaskSetManager);
        trapSinkConsumer.setTaskSetPublisher((tenantId, location, taskSet) -> {});
        trapSinkConsumer.initializeConfig();

        ArgumentCaptor<String> jsonStringCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockConfigService).addConfig(Mockito.eq(ConfigConstants.SNMP_TRAPS_CONFIG), jsonStringCaptor.capture(), Mockito.eq(TrapSinkConsumer.TRAPS_EVENT_SOURCE));

        String json = jsonStringCaptor.getValue();
        assertNotNull(json);

        ObjectMapper objectMapper = new ObjectMapper();
        TrapdConfigBean configBean = objectMapper.readValue(json, TrapdConfigBean.class);
        Assert.assertEquals(10162, configBean.getSnmpTrapPort());
        Assert.assertEquals("*", configBean.getSnmpTrapAddress());
        Assert.assertEquals(500, configBean.getBatchIntervalMs());
        Assert.assertEquals(10000, configBean.getQueueSize());
        Assert.assertTrue("snmpv3 users must be empty", configBean.getSnmpV3Users().isEmpty());
    }
}
