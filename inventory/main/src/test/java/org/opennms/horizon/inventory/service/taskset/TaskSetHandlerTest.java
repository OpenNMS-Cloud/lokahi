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

package org.opennms.horizon.inventory.service.taskset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskSetHandlerTest {

    @Mock
    private TaskSetPublisher mockPublisher;
    @Mock
    private SnmpConfigService configService;
    @Mock
    private MonitorTaskSetService monitorTaskSetService;

    @InjectMocks
    private TaskSetHandler service;
    @Captor
    ArgumentCaptor<List<TaskDefinition>> taskListCaptor;
    private final String tenantId = "testTenant";
    private final Long locationId = 1020304050L;


    @Test
    public void testMonitorTaskForPrimary() {
        var ip1 = new IpInterface();
        ip1.setSnmpPrimary(true);
        ip1.setTenantId(tenantId);
        ip1.setIpAddress(InetAddressUtils.addr("192.168.1.1"));
        var ip2 = new IpInterface();
        ip2.setSnmpPrimary(false);
        ip2.setTenantId(tenantId);
        ip2.setIpAddress(InetAddressUtils.addr("192.168.5.1"));
        when(monitorTaskSetService.getMonitorTask(any(), any(), anyLong(), anyLong(), any())).thenReturn(TaskDefinition.newBuilder().getDefaultInstanceForType());
        service.sendMonitorTask(locationId, MonitorType.SNMP, ip1, 1L, 5L);
        verify(mockPublisher).publishNewTasks(eq(tenantId), eq(locationId), taskListCaptor.capture());
        service.sendMonitorTask(locationId, MonitorType.SNMP, ip2, 1L, 10L);
        verifyNoMoreInteractions(mockPublisher);
    }
}
