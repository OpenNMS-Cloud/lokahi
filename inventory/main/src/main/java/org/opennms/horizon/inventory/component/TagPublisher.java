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

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class TagPublisher {
    private static final String DEFAULT_TOPIC = "tag-operation";
    @Value("${kafka.topics.tag-operation:"+DEFAULT_TOPIC +"}")
    private String tagTopic;
    private final TagRepository tagRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public TagPublisher(TagRepository tagRepository, KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.tagRepository = tagRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAllTags() {
        List<Tag> tags = tagRepository.findAll().stream().filter(t -> t.getNodes().size() > 0).toList();
        TagOperationList list = createTagOperationFromTag(tags, Operation.ASSIGN_TAG);
        sendTagMessage(list);
    }

    public void publishTagUpdate(List<TagOperationProto> opList) {
        sendTagMessage(TagOperationList.newBuilder().addAllTags(opList).build());
    }

    private TagOperationList createTagOperationFromTag(List<Tag> tags, Operation operation) {
        List<TagOperationProto> topList = tags.stream().map(t -> TagOperationProto.newBuilder()
            .setTenantId(t.getTenantId())
            .setTagName(t.getName())
            .setOperation(operation)
            .addAllNodeId(t.getNodes().stream().map(Node::getId).toList())
            .build()).collect(Collectors.toList());
        return TagOperationList.newBuilder()
            .addAllTags(topList).build();
    }

    private void sendTagMessage(TagOperationList tagData) {
        var rec = new ProducerRecord<String, byte[]>(tagTopic, tagData.toByteArray());
        kafkaTemplate.send(rec);
    }
}
