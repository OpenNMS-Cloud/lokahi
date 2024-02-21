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

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class TagPublisher {
    private static final String DEFAULT_TOPIC = "tag-operation";

    @Value("${kafka.topics.tag-operation:" + DEFAULT_TOPIC + "}")
    private String tagTopic;

    private final TagRepository tagRepository;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public TagPublisher(TagRepository tagRepository, KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.tagRepository = tagRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAllTags() {
        List<Tag> tags = tagRepository.findAll().stream()
                .filter(t -> t.getNodes().size() > 0)
                .toList();
        TagOperationList list = createTagOperationFromTag(tags, Operation.ASSIGN_TAG);
        sendTagMessage(list);
    }

    public void publishTagUpdate(List<TagOperationProto> opList) {
        sendTagMessage(TagOperationList.newBuilder().addAllTags(opList).build());
    }

    private TagOperationList createTagOperationFromTag(List<Tag> tags, Operation operation) {
        List<TagOperationProto> topList = tags.stream()
                .map(t -> TagOperationProto.newBuilder()
                        .setTenantId(t.getTenantId())
                        .setTagName(t.getName())
                        .setOperation(operation)
                        .addAllNodeId(t.getNodes().stream().map(Node::getId).toList())
                        .build())
                .collect(Collectors.toList());
        return TagOperationList.newBuilder().addAllTags(topList).build();
    }

    private void sendTagMessage(TagOperationList tagData) {
        var rec = new ProducerRecord<String, byte[]>(tagTopic, tagData.toByteArray());
        kafkaTemplate.send(rec);
    }
}
