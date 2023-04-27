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

package org.opennms.horizon.inventory.component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class NodeKafkaProducer {
    @Value("${kafka.topics.node}")
    private String topic;

    @Autowired
    @Qualifier("byteArrayTemplate")
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @PostUpdate
    @PostPersist
    public void sendNode(Node node) {
        // Not all fields are included in this proto, since the Alerts service doesn't care about all of them.
        NodeDTO proto = NodeDTO.newBuilder()
            .setId(node.getId())
            .setTenantId(node.getTenantId())
            .setNodeLabel(node.getNodeLabel())
            .build();

        var record = new ProducerRecord<String, byte[]>(topic, proto.toByteArray());
        kafkaTemplate.send(record);
    }
}
