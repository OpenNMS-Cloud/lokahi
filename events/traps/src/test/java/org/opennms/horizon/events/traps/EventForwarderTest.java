package org.opennms.horizon.events.traps;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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
        Event testEvent =
            Event.newBuilder()
                .setNodeId(1)
                .build();
        EventLog testProtoEventLog =
            EventLog.newBuilder()
                .addEvents(testEvent)
                .build();

        eventForwarder.sendTrapEvents(testProtoEventLog);
        verify(kafkaTemplate).send(producerRecordCaptor.capture());

        ProducerRecord<String, byte[]> producerRecord = producerRecordCaptor.getValue();
        assertThat(producerRecord.topic()).isEqualTo(trapEventsTopic);
        assertThat(producerRecord.value()).isEqualTo(testProtoEventLog.toByteArray());
    }

    @Test
    void testSendInternalEvent() {
        Event testEvent =
            Event.newBuilder()
                .setNodeId(1)
                .build();

        eventForwarder.sendInternalEvent(testEvent);
        verify(kafkaTemplate).send(producerRecordCaptor.capture());

        ProducerRecord<String, byte[]> producerRecord = producerRecordCaptor.getValue();
        assertThat(producerRecord.topic()).isEqualTo(internalEventsTopic);
        assertThat(producerRecord.value()).isEqualTo(testEvent.toByteArray());
    }


}
