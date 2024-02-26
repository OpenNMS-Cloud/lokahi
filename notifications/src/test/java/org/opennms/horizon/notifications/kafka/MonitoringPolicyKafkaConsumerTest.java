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
package org.opennms.horizon.notifications.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.notifications.service.MonitoringPolicyService;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringPolicyKafkaConsumerTest {

    @InjectMocks
    MonitoringPolicyKafkaConsumer monitoringPolicyKafkaConsumer;

    @Mock
    MonitoringPolicyService monitoringPolicyService;

    @Test
    public void testDropsInvalidData() {
        monitoringPolicyKafkaConsumer.consume(new byte[10]);
        Mockito.verify(monitoringPolicyService, times(0)).saveMonitoringPolicy(any());
    }

    @Test
    public void testDropsWithoutTenantId() {
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder().build();

        monitoringPolicyKafkaConsumer.consume(proto.toByteArray());
        Mockito.verify(monitoringPolicyService, times(0)).saveMonitoringPolicy(any());
    }

    @Test
    public void testConsume() {
        MonitorPolicyProto proto =
                MonitorPolicyProto.newBuilder().setTenantId("tenant").build();

        monitoringPolicyKafkaConsumer.consume(proto.toByteArray());
        Mockito.verify(monitoringPolicyService, times(1)).saveMonitoringPolicy(eq(proto));
    }
}
