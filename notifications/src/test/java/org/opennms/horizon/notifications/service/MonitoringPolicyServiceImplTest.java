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
package org.opennms.horizon.notifications.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.notifications.mapper.MonitoringPolicyMapper;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringPolicyServiceImplTest {

    @InjectMocks
    MonitoringPolicyService monitoringPolicyService;

    @Mock
    MonitoringPolicyRepository monitoringPolicyRepository;

    @Spy
    MonitoringPolicyMapper monitoringPolicyMapper = Mappers.getMapper(MonitoringPolicyMapper.class);

    @Test
    public void savePolicy() {
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder()
                .setId(1)
                .setTenantId("tenant")
                .setNotifyByPagerDuty(true)
                .setNotifyByEmail(false)
                .setNotifyByWebhooks(false)
                .build();
        monitoringPolicyService.saveMonitoringPolicy(proto);

        Mockito.verify(monitoringPolicyRepository, Mockito.times(1)).save(argThat((arg) -> {
            assertEquals(1, arg.getId());
            assertEquals("tenant", arg.getTenantId());
            assertTrue(arg.isNotifyByPagerDuty());
            assertFalse(arg.isNotifyByEmail());
            assertFalse(arg.isNotifyByWebhooks());

            return true;
        }));
    }
}
