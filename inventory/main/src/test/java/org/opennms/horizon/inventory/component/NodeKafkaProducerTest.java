package org.opennms.horizon.inventory.component;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class NodeKafkaProducerTest {

    public static final String TEST_TOPIC = "x-topic-x";

    private static final Logger LOG = LoggerFactory.getLogger(NodeKafkaProducerTest.class);

    private NodeKafkaProducer target;

    private KafkaTemplate<String, byte[]> mockKafkaTemplate;
    
    @BeforeEach
    public void setUp() {
        mockKafkaTemplate = Mockito.mock(KafkaTemplate.class);
        
        target = new NodeKafkaProducer();
        
        target.setTopic(TEST_TOPIC);
        target.setKafkaTemplate(mockKafkaTemplate);
    }

    @Test
    void testSendNode() {
        //
        // Setup Test Data and Interactions
        //
        Node testNode = new Node();
        testNode.setId(131313L);
        testNode.setTenantId("x-tenant-id-x");
        testNode.setNodeLabel("x-node-label-x");
        testNode.setMonitoringLocationId(10L);

        //
        // Execute
        //
        target.sendNode(testNode);

        //
        // Verify the Results
        //
        ArgumentMatcher<ProducerRecord<String, byte[]>> matcher = argument -> nodeProducerRecordMatches(testNode, argument);
        Mockito.verify(mockKafkaTemplate).send(Mockito.argThat(matcher));
    }
    
//========================================
// Internals
//----------------------------------------
    
    private boolean nodeProducerRecordMatches(Node expectedNode, ProducerRecord<String, byte[]> producerRecord) {
        try {
            NodeDTO nodeDTO = NodeDTO.parseFrom(producerRecord.value());

            return (
                    (TEST_TOPIC.equals(producerRecord.topic())) &&
                    (Objects.equals(expectedNode.getId(), nodeDTO.getId())) &&
                    (Objects.equals(expectedNode.getTenantId(), nodeDTO.getTenantId())) &&
                    (Objects.equals(expectedNode.getMonitoringLocationId(), nodeDTO.getMonitoringLocationId())) &&
                    (Objects.equals(expectedNode.getNodeLabel(), nodeDTO.getNodeLabel()))
                );
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Unexpected test error", e);
            return false;
        }
    }
}
