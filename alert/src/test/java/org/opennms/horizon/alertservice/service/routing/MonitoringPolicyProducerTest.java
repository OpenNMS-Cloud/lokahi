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
package org.opennms.horizon.alertservice.service.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.PolicyRule;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringPolicyProducerTest {

    @InjectMocks
    MonitoringPolicyProducer producer;

    @Mock
    KafkaTemplate<String, byte[]> kafkaProducerTemplate;

    @Captor
    ArgumentCaptor<ProducerRecord<String, byte[]>> producerCaptor;

    String topic = "some-monitoring-policy-topic";

    @Before
    public void setup() {
        ReflectionTestUtils.setField(producer, "topic", topic);
    }

    @Test
    public void sendsUpdatedMonitoringPolicyToKafka() throws InvalidProtocolBufferException {
        MonitorPolicy policy = new MonitorPolicy();
        policy.setId(1L);
        policy.setTenantId("T1");
        policy.setNotifyByPagerDuty(true);
        policy.setNotifyByEmail(false);
        policy.setNotifyByWebhooks(false);

        // These fields aren't needed by the notification service, so we should avoid sending them.
        policy.setName("Testing Policy");
        policy.setMemo("Some memo");
        policy.setRules(List.of(mock(PolicyRule.class)));
        policy.setNotifyInstruction("Instructions");

        producer.sendMonitoringPolicy(policy);

        verify(kafkaProducerTemplate, times(1)).send(producerCaptor.capture());
        assertEquals(topic, producerCaptor.getValue().topic());
        MonitorPolicyProto sentProto =
                MonitorPolicyProto.parseFrom(producerCaptor.getValue().value());
        assertEquals(1L, sentProto.getId());
        assertEquals("T1", sentProto.getTenantId());
        assertTrue(sentProto.getNotifyByPagerDuty());
        assertFalse(sentProto.getNotifyByEmail());
        assertFalse(sentProto.getNotifyByWebhooks());

        // Check the unneeded fields are missing
        assertTrue(sentProto.getName().isEmpty());
        assertTrue(sentProto.getMemo().isEmpty());
        assertTrue(sentProto.getRulesList().isEmpty());
        assertTrue(sentProto.getTagsList().isEmpty());
        assertTrue(sentProto.getNotifyInstruction().isEmpty());
    }
}
