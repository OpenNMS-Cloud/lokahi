/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.taskset.service.api.TaskSetPublisher;

public class FlowsConfigServiceTest {
    @Mock
    MonitoringLocationService monitoringLocationService;

    @Mock
    TaskSetPublisher taskSetPublisher;

    @Test
    public void canReadConfig() {
        FlowsConfigService service = new FlowsConfigService(monitoringLocationService, taskSetPublisher);
        FlowsConfig config = service.readFlowsConfig();
        Assert.assertNotNull(config);
        Assert.assertEquals("Netflow-5-UDP-8877", config.getListeners(0).getName());
        Assert.assertEquals(1, config.getListeners(0).getParsersList().size());
        Assert.assertEquals(1, config.getListeners(0).getParsers(0).getQueue()
            .getAdapters(0).getPackagesCount());
    }
}
