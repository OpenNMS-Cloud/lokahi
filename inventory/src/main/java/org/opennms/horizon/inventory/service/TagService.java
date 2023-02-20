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
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.TagMapper;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
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
    private final AzureCredentialRepository azureCredentialRepository;
    private final TagMapper mapper;

    @Transactional
    public List<TagDTO> addTags(String tenantId, TagCreateListDTO request) {
        if (request.getTagsList().isEmpty()) {
            return Collections.emptyList();
        }
        if (request.hasNodeId()) {
            Node node = getNode(tenantId, request.getNodeId());
            return request.getTagsList().stream()
                .map(tagCreateDTO -> addTagToNode(tenantId, node, tagCreateDTO))
                .toList();
        } else if (request.hasAzureCredentialId()) {
            AzureCredential credential = getAzureCredential(tenantId, request.getAzureCredentialId());
            return request.getTagsList().stream()
                .map(tagCreateDTO -> addTagToAzureCredential(tenantId, credential, tagCreateDTO))
                .toList();
        } else {
            throw new InventoryRuntimeException("Invalid ID provided");
        }
    }

    @Transactional
    public void removeTags(String tenantId, TagRemoveListDTO request) {
        List<Tag> tags = request.getTagIdsList().stream()
            .map(Int64Value::getValue)
            .map(tagId -> getTag(tenantId, tagId))
            .toList();

        if (request.hasNodeId()) {
            Node node = getNode(tenantId, request.getNodeId());
            tags.forEach(tag -> tag.getNodes().remove(node));
        } else if (request.hasAzureCredentialId()) {
            AzureCredential azureCredential = getAzureCredential(tenantId, request.getAzureCredentialId());
            tags.forEach(tag -> tag.getAzureCredentials().remove(azureCredential));
        }
    }

    public List<TagDTO> getTagsByEntityId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        if (listParams.hasNodeId()) {
            return getTagsByNodeId(tenantId, listParams);
        } else if (listParams.hasAzureCredentialId()) {
            return getTagsByAzureCredentialId(tenantId, listParams);
        } else {
            throw new InventoryRuntimeException("Invalid ID provided");
        }
    }

    public List<TagDTO> getTags(String tenantId, ListAllTagsParamsDTO listParams) {
        if (listParams.hasParams()) {
            TagListParamsDTO params = listParams.getParams();
            String searchTerm = params.getSearchTerm();

            if (StringUtils.isNotEmpty(searchTerm)) {
                return repository.findByTenantIdAndNameLike(tenantId, searchTerm)
                    .stream().map(mapper::modelToDTO).toList();
            }
        }
        return repository.findByTenantId(tenantId)
            .stream().map(mapper::modelToDTO).toList();
    }

    @Transactional
    public void deleteTags(String tenantId, DeleteTagsDTO request) {
        if (request.getTagIdsList().isEmpty()) {
            return;
        }
        for (Int64Value tagId : request.getTagIdsList()) {
            Optional<Tag> tagOpt = repository.findByTenantIdAndId(tenantId, tagId.getValue());
            if (tagOpt.isPresent()) {
                Tag tag = tagOpt.get();
                tag.getNodes().clear();
                tag.getAzureCredentials().clear();

                repository.delete(tag);
            }
        }
    }

    private TagDTO addTagToNode(String tenantId, Node node, TagCreateDTO tagCreateDTO) {
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

    private TagDTO addTagToAzureCredential(String tenantId, AzureCredential credential, TagCreateDTO tagCreateDTO) {
        String tagName = tagCreateDTO.getName();

        Optional<Tag> tagOpt = repository
            .findByTenantIdAzureCredentialIdAndName(tenantId, credential.getId(), tagName);

        if (tagOpt.isPresent()) {
            return mapper.modelToDTO(tagOpt.get());
        }

        tagOpt = repository.findByTenantIdAndName(tenantId, tagName);
        Tag tag = tagOpt.orElseGet(() -> mapCreateTag(tenantId, tagCreateDTO));

        tag.getAzureCredentials().add(credential);
        tag = repository.save(tag);

        return mapper.modelToDTO(tag);
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

    private AzureCredential getAzureCredential(String tenantId, long credentialId) {
        Optional<AzureCredential> azureCredentialOpt = azureCredentialRepository.findByTenantIdAndId(tenantId, credentialId);
        if (azureCredentialOpt.isEmpty()) {
            throw new InventoryRuntimeException("Azure Credential not found for id: " + credentialId);
        }
        return azureCredentialOpt.get();
    }

    private List<TagDTO> getTagsByNodeId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        long nodeId = listParams.getNodeId();
        if (listParams.hasParams()) {
            TagListParamsDTO params = listParams.getParams();
            String searchTerm = params.getSearchTerm();

            if (StringUtils.isNotEmpty(searchTerm)) {
                return repository.findByTenantIdAndNodeIdAndNameLike(tenantId, nodeId, searchTerm)
                    .stream().map(mapper::modelToDTO).toList();
            }
        }
        return repository.findByTenantIdAndNodeId(tenantId, nodeId)
            .stream().map(mapper::modelToDTO).toList();
    }

    private List<TagDTO> getTagsByAzureCredentialId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        long azureCredentialId = listParams.getAzureCredentialId();
        if (listParams.hasParams()) {
            TagListParamsDTO params = listParams.getParams();
            String searchTerm = params.getSearchTerm();

            if (StringUtils.isNotEmpty(searchTerm)) {
                return repository.findByTenantIdAndAzureCredentialIdAndNameLike(tenantId, azureCredentialId, searchTerm)
                    .stream().map(mapper::modelToDTO).toList();
            }
        }
        return repository.findByTenantIdAndAzureCredentialId(tenantId, azureCredentialId)
            .stream().map(mapper::modelToDTO).toList();
    }
}
