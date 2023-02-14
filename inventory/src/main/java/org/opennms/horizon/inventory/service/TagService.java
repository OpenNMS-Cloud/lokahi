/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service;

import com.google.protobuf.Int64Value;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.TagMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository repository;
    private final NodeRepository nodeRepository;
    private final TagMapper mapper;

    @Transactional
    public List<TagDTO> addTags(String tenantId, TagCreateListDTO request) {
        if (request.getTagsList().isEmpty()) {
            return Collections.emptyList();
        }
        Node node = getNode(tenantId, request.getNodeId());

        return request.getTagsList().stream()
            .map(tagCreateDTO -> createTag(tenantId, node, tagCreateDTO))
            .toList();
    }

    @Transactional
    public void removeTags(String tenantId, TagRemoveListDTO request) {
        Node node = getNode(tenantId, request.getNodeId());

        request.getTagIdsList().stream()
            .map(Int64Value::getValue)
            .map(tagId -> getTag(tenantId, tagId))
            .toList()
            .forEach(tag -> removeTagFromNode(node, tag));
    }

    private TagDTO createTag(String tenantId, Node node, TagCreateDTO tagCreateDTO) {
        String tagName = tagCreateDTO.getName();

        Optional<Tag> tagOpt = repository
            .findByTenantIdNodeIdAndName(tenantId, node.getId(), tagName);

        if (tagOpt.isPresent()) {
            return mapper.modelToDTO(tagOpt.get());
        }

        tagOpt = repository.findByTenantIdAndName(tenantId, tagName);
        Tag tag = tagOpt.orElseGet(() -> mapCreateTag(tenantId, tagCreateDTO));

        tag.getNodes().add(node);
        tag = repository.save(tag);

        return mapper.modelToDTO(tag);
    }

    private void removeTagFromNode(Node node, Tag tag) {
        if (tag.getNodes().isEmpty()) {
            repository.delete(tag);
        } else {
            tag.getNodes().remove(node);
            if (tag.getNodes().isEmpty()) {
                repository.delete(tag);
            } else {
                repository.save(tag);
            }
        }
    }

    private Tag mapCreateTag(String tenantId, TagCreateDTO request) {
        Tag tag = mapper.createDtoToModel(request);
        tag.setTenantId(tenantId);
        return tag;
    }

    private Node getNode(String tenantId, long nodeId) {
        Optional<Node> nodeOpt = nodeRepository.findByIdAndTenantId(nodeId, tenantId);
        if (nodeOpt.isEmpty()) {
            throw new InventoryRuntimeException("Node not found for id: " + nodeId);
        }
        return nodeOpt.get();
    }

    private Tag getTag(String tenantId, long tagId) {
        Optional<Tag> tagOpt = repository.findByTenantIdAndId(tenantId, tagId);
        if (tagOpt.isEmpty()) {
            throw new InventoryRuntimeException("Tag not found for id: " + tagId);
        }
        return tagOpt.get();
    }

    public List<TagDTO> getTagsByNodeId(String tenantId, long nodeId) {
        return repository.findByTenantIdAndNodeId(tenantId, nodeId)
            .stream().map(mapper::modelToDTO).toList();
    }

    public List<TagDTO> getTags(String tenantId) {
        return repository.findByTenantId(tenantId)
            .stream().map(mapper::modelToDTO).toList();
    }
}
