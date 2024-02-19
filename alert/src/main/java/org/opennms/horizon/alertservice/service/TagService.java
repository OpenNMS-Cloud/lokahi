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
package org.opennms.horizon.alertservice.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alert.tag.proto.TagProto;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.mapper.TagMapper;
import org.opennms.horizon.alertservice.service.routing.TagOperationProducer;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final TagOperationProducer tagOperationProducer;

    /**
     * This publishes existing tags on monitoring policies to Kafka
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void publishExistingTags() {

        var tags = tagRepository.findAll();
        var tagOperationUpdates = TagOperationList.newBuilder();
        tags.forEach(tag -> {
            var tagAddOp = TagOperationProto.newBuilder()
                    .setOperation(Operation.ASSIGN_TAG)
                    .setTagName(tag.getName())
                    .setTenantId(tag.getTenantId());
            tag.getPolicies().forEach(monitorPolicy -> tagAddOp.addMonitoringPolicyId(monitorPolicy.getId()));
            tagOperationUpdates.addTags(tagAddOp.build());
        });
        tagOperationProducer.sendTagUpdate(tagOperationUpdates.build());
    }

    @Transactional
    public void insertOrUpdateTags(TagOperationList list) {
        list.getTagsList().forEach(tagOp -> {
            switch (tagOp.getOperation()) {
                case ASSIGN_TAG -> {
                    if (tagOp.getNodeIdList().isEmpty()) {
                        // Only handle tag operation updates with nodeIds
                        return;
                    }
                    tagRepository
                            .findByTenantIdAndName(tagOp.getTenantId(), tagOp.getTagName())
                            .ifPresentOrElse(
                                    tag -> {
                                        int oldSize = tag.getNodeIds().size();
                                        tagOp.getNodeIdList().forEach(id -> {
                                            if (!tag.getNodeIds().contains(id)) {
                                                tag.getNodeIds().add(id);
                                            }
                                        });
                                        tagRepository.save(tag);
                                        log.info(
                                                "added nodeIds with data {} node id size from {} to {}",
                                                tagOp,
                                                oldSize,
                                                tag.getNodeIds().size());
                                    },
                                    () -> {
                                        Tag tag = new Tag();
                                        tag.setName(tagOp.getTagName());
                                        tag.setTenantId(tagOp.getTenantId());
                                        tag.setNodeIds(tagOp.getNodeIdList());
                                        tagRepository.save(tag);
                                        log.info("inserted new tag with data {}", tagOp);
                                    });
                }
                case REMOVE_TAG -> {
                    if (tagOp.getNodeIdList().isEmpty()) {
                        // Only handle tag operation updates with nodeIds
                        return;
                    }
                    tagRepository
                            .findByTenantIdAndName(tagOp.getTenantId(), tagOp.getTagName())
                            .ifPresent(tag -> {
                                int oldSize = tag.getNodeIds().size();
                                tagOp.getNodeIdList()
                                        .forEach(id -> tag.getNodeIds().remove(id));
                                if (tag.getNodeIds().isEmpty()
                                        && tag.getPolicies().isEmpty()) {
                                    tagRepository.deleteById(tag.getId());
                                    log.info("deleted tag {}", tagOp);
                                } else {
                                    tagRepository.save(tag);
                                    log.info(
                                            "removed nodeIds for {} and node ids size changed from {} to {}",
                                            tagOp,
                                            oldSize,
                                            tag.getNodeIds().size());
                                }
                            });
                }
            }
        });
    }

    public List<TagProto> listAllTags(String tenantId) {
        return tagRepository.findByTenantId(tenantId).stream()
                .map(tagMapper::map)
                .toList();
    }
}
