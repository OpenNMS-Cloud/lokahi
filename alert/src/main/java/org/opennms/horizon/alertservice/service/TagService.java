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

package org.opennms.horizon.alertservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alert.tag.proto.TagProto;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.mapper.TagMapper;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final MonitorPolicyRepository monitorPolicyRepository;

    @Transactional
    public void insertOrUpdateTags(TagOperationList list) {
        list.getTagsList().forEach( tagOp -> {
            switch (tagOp.getOperation()) {
                case ASSIGN_TAG -> tagRepository.findByTenantIdAndName(tagOp.getTenantId(), tagOp.getTagName())
                    .ifPresentOrElse(tag -> {
                        int oldSize = tag.getNodeIds().size();
                        tagOp.getNodeIdList().forEach(id -> {
                            if(!tag.getNodeIds().contains(id)) {
                                tag.getNodeIds().add(id);
                            }
                        });
                        tagRepository.save(tag);
                        log.info("added nodeIds with data {} node id size from {} to {}", tagOp, oldSize, tag.getNodeIds().size());
                    }, () -> {
                        Tag tag = new Tag();
                        tag.setName(tagOp.getTagName());
                        tag.setTenantId(tagOp.getTenantId());
                        tag.setNodeIds(tagOp.getNodeIdList());
                        tagRepository.save(tag);
                        log.info("inserted new tag with data {}", tagOp);
                    });
                case REMOVE_TAG -> tagRepository.findByTenantIdAndName(tagOp.getTenantId(), tagOp.getTagName())
                    .ifPresent(tag -> {
                        int oldSize = tag.getNodeIds().size();
                        tagOp.getNodeIdList().forEach(id -> tag.getNodeIds().remove(id));
                        if(tag.getNodeIds().isEmpty() && tag.getPolicies().isEmpty()) {
                            tagRepository.deleteById(tag.getId());
                            log.info("deleted tag {}", tagOp);
                        } else {
                            tagRepository.save(tag);
                            log.info("removed nodeIds for {} and node ids size changed from {} to {}", tagOp, oldSize, tag.getNodeIds().size());
                        }
                    });
            }
        });
    }

    public List<TagProto> listAllTags(String tenantId) {
        return tagRepository.findByTenantId(tenantId)
            .stream().map(tagMapper::map).toList();
    }
}
