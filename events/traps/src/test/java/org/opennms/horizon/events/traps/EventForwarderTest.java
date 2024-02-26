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
package org.opennms.horizon.events.traps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class EventForwarderTest {
    EventForwarder eventForwarder;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Captor
    ArgumentCaptor<ProducerRecord<String, byte[]>> producerRecordCaptor;

    private final String trapEventsTopic = "test-trap-events";

    private final String internalEventsTopic = "test-internal-events";

    @BeforeEach
    void setUp() {
        eventForwarder = new EventForwarder(kafkaTemplate, trapEventsTopic, internalEventsTopic);
    }

    @Test
    void testSendTrapEvent() {
        Event testEvent = Event.newBuilder().setNodeId(1).build();
        EventLog testProtoEventLog = EventLog.newBuilder().addEvents(testEvent).build();

        eventForwarder.sendTrapEvents(testProtoEventLog);
        verify(kafkaTemplate).send(producerRecordCaptor.capture());

        ProducerRecord<String, byte[]> producerRecord = producerRecordCaptor.getValue();
        assertThat(producerRecord.topic()).isEqualTo(trapEventsTopic);
        assertThat(producerRecord.value()).isEqualTo(testProtoEventLog.toByteArray());
    }

    @Test
    void testSendInternalEvent() {
        Event testEvent = Event.newBuilder().setNodeId(1).build();

        eventForwarder.sendInternalEvent(testEvent);
        verify(kafkaTemplate).send(producerRecordCaptor.capture());

        ProducerRecord<String, byte[]> producerRecord = producerRecordCaptor.getValue();
        assertThat(producerRecord.topic()).isEqualTo(internalEventsTopic);
        assertThat(producerRecord.value())
                .isEqualTo(EventLog.newBuilder().addEvents(testEvent).build().toByteArray());
    }
}
