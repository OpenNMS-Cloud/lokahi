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
package org.opennms.horizon.inventory.service.taskset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
        when(monitorTaskSetService.getMonitorTask(any(), any(), anyLong(), anyLong(), any()))
                .thenReturn(TaskDefinition.newBuilder().getDefaultInstanceForType());
        service.sendMonitorTask(locationId, MonitorType.SNMP, ip1, 1L, 5L);
        verify(mockPublisher).publishNewTasks(eq(tenantId), eq(locationId), taskListCaptor.capture());
        service.sendMonitorTask(locationId, MonitorType.SNMP, ip2, 1L, 10L);
        verifyNoMoreInteractions(mockPublisher);
    }
}
