package org.opennms.miniongateway.grpc.server.traps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.grpc.traps.contract.mapper.TenantLocationSpecificTrapLogDTOMapper;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageMapper;

@RunWith(MockitoJUnitRunner.class)
public class TrapsKafkaForwarderTest {

    private final String kafkaTopic = "kafkaTopic";

    @Mock
    private SinkMessageKafkaPublisherFactory publisherFactory;
    @Mock
    private TenantLocationSpecificTrapLogDTOMapper mapper;
    @Mock
    private SinkMessageKafkaPublisher<Message, Message> publisher;

    private TrapsKafkaForwarder trapsKafkaForwarder;

    @Before
    public void setUp() {
        when(publisherFactory.create(any(SinkMessageMapper.class), eq(kafkaTopic))).thenReturn(publisher);
        trapsKafkaForwarder = new TrapsKafkaForwarder(publisherFactory, mapper, kafkaTopic);
    }

    @Test
    public void testForward() {
        var message = TrapLogDTO.newBuilder()
            .setIdentity(Identity.newBuilder().setSystemId("asdf").build())
            .addTrapDTO(TrapDTO.newBuilder().setCommunity("public").build())
            .build();

        trapsKafkaForwarder.handleMessage(message);
        Mockito.verify(publisher).send(message);
    }
}
