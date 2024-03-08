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
package org.opennms.horizon.inventory.component;

import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
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
        ArgumentMatcher<ProducerRecord<String, byte[]>> matcher =
                argument -> nodeProducerRecordMatches(testNode, argument);
        Mockito.verify(mockKafkaTemplate).send(Mockito.argThat(matcher));
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private boolean nodeProducerRecordMatches(Node expectedNode, ProducerRecord<String, byte[]> producerRecord) {
        try {
            NodeDTO nodeDTO = NodeDTO.parseFrom(producerRecord.value());

            return ((TEST_TOPIC.equals(producerRecord.topic()))
                    && (Objects.equals(expectedNode.getId(), nodeDTO.getId()))
                    && (Objects.equals(expectedNode.getTenantId(), nodeDTO.getTenantId()))
                    && (Objects.equals(expectedNode.getMonitoringLocationId(), nodeDTO.getMonitoringLocationId()))
                    && (Objects.equals(expectedNode.getNodeLabel(), nodeDTO.getNodeLabel())));
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Unexpected test error", e);
            return false;
        }
    }
}
