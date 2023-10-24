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
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.component.AlertClient;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.TagMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TagService {
    private final TagRepository repository;
    private final NodeRepository nodeRepository;
    private final ActiveDiscoveryRepository activeDiscoveryRepository;
    private final PassiveDiscoveryRepository passiveDiscoveryRepository;
    private final TagMapper mapper;
    private final TagPublisher tagPublisher;
    private final NodeService nodeService;

    private final AlertClient alertClient;

    public TagService(final TagRepository repository,
                      final NodeRepository nodeRepository,
                      final ActiveDiscoveryRepository activeDiscoveryRepository,
                      final PassiveDiscoveryRepository passiveDiscoveryRepository,
                      final TagMapper mapper,
                      final TagPublisher tagPublisher,
                      @Lazy final NodeService nodeService,
                      final AlertClient alertClient) {
        this.repository = Objects.requireNonNull(repository);
        this.nodeRepository = Objects.requireNonNull(nodeRepository);
        this.activeDiscoveryRepository = Objects.requireNonNull(activeDiscoveryRepository);
        this.passiveDiscoveryRepository = Objects.requireNonNull(passiveDiscoveryRepository);
        this.mapper = Objects.requireNonNull(mapper);
        this.tagPublisher = Objects.requireNonNull(tagPublisher);
        this.nodeService = Objects.requireNonNull(nodeService);
        this.alertClient = Objects.requireNonNull(alertClient);
    }

    @Transactional
    public List<TagDTO> addTags(String tenantId, TagCreateListDTO request) {
        if (request.getTagsList().isEmpty()) {
            return Collections.emptyList();
        }
        if (request.getEntityIdsList().isEmpty()) {
            return Collections.emptyList();
        }
        Set<TagDTO> tags = new LinkedHashSet<>();
        for (TagEntityIdDTO entityId : request.getEntityIdsList()) {
            tags.addAll(addTags(tenantId, entityId, request.getTagsList()));
        }
        return tags.stream().toList();
    }


    private List<TagDTO> addTags(String tenantId, TagEntityIdDTO entityId, List<TagCreateDTO> tagCreateList) {
        if (entityId.hasNodeId()) {
            Node node = getNode(tenantId, entityId.getNodeId());
            List<TagOperationProto> tagOpList = tagCreateList.stream().map(t -> TagOperationProto.newBuilder()
                .setOperation(Operation.ASSIGN_TAG)
                .setTagName(t.getName())
                .setTenantId(tenantId)
                .addNodeId(node.getId())
                .build()).toList();
            final var result = tagCreateList.stream()
                .map(tagCreateDTO -> addTagToNode(tenantId, node, tagCreateDTO))
                .toList();
            tagPublisher.publishTagUpdate(tagOpList);
            return result;
        } else if (entityId.hasActiveDiscoveryId()) {
            ActiveDiscovery discovery = getActiveDiscovery(tenantId, entityId.getActiveDiscoveryId());
            return tagCreateList.stream()
                .map(tagCreateDTO -> addTagToActiveDiscovery(tenantId, discovery, tagCreateDTO))
                .toList();
        } else if (entityId.hasPassiveDiscoveryId()) {
            PassiveDiscovery discovery = getPassiveDiscovery(tenantId, entityId.getPassiveDiscoveryId());
            return tagCreateList.stream()
                .map(tagCreateDTO -> addTagToPassiveDiscovery(tenantId, discovery, tagCreateDTO))
                .toList();
        } else if (entityId.hasMonitoringPolicyId()) {
            if (!policyExists(entityId.getMonitoringPolicyId(), tenantId)) {
                throw new InventoryRuntimeException("MonitoringPolicyId not found for id: " + entityId.getMonitoringPolicyId());
            }
            return tagCreateList.stream().map(tagCreateDTO ->
                    addTagsToMonitoringPolicy(tenantId, entityId.getMonitoringPolicyId(), tagCreateDTO))
                .collect(Collectors.toList());
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

        for (TagEntityIdDTO entityId : request.getEntityIdsList()) {
            removeTags(tenantId, entityId, tags);
        }
    }

    private void removeTags(String tenantId, TagEntityIdDTO entityId, List<Tag> tags) {
        if (entityId.hasNodeId()) {
            Node node = getNode(tenantId, entityId.getNodeId());
            tags.forEach(tag -> tag.getNodes().remove(node));
            List<TagOperationProto> tagOpList = tags.stream().map(t  -> TagOperationProto.newBuilder()
                .setTenantId(tenantId)
                .setOperation(Operation.REMOVE_TAG)
                .setTagName(t.getName())
                .addNodeId(node.getId())
                .build()).collect(Collectors.toList());
            tagPublisher.publishTagUpdate(tagOpList);
        } else if (entityId.hasActiveDiscoveryId()) {
            ActiveDiscovery activeDiscovery = getActiveDiscovery(tenantId, entityId.getActiveDiscoveryId());
            tags.forEach(tag -> {
                tag.getActiveDiscoveries().remove(activeDiscovery);
            });
        } else if (entityId.hasPassiveDiscoveryId()) {
            PassiveDiscovery discovery = getPassiveDiscovery(tenantId, entityId.getPassiveDiscoveryId());
            tags.forEach(tag -> {
                tag.getPassiveDiscoveries().remove(discovery);
            });
        }
    }

    public List<TagDTO> getTagsByEntityId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        TagEntityIdDTO entityId = listParams.getEntityId();
        if (entityId.hasNodeId()) {
            return getTagsByNodeId(tenantId, listParams);
        } else if (entityId.hasActiveDiscoveryId()) {
            return getTagsByActiveDiscoveryId(tenantId, listParams);
        } else if (entityId.hasPassiveDiscoveryId()) {
            return getTagsByPassiveDiscoveryId(tenantId, listParams);
        } else if (entityId.hasMonitoringPolicyId()) {
            if (!policyExists(entityId.getMonitoringPolicyId(), tenantId)) {
                throw new InventoryRuntimeException("MonitoringPolicyId not found for id: " + entityId.getMonitoringPolicyId());
            }
            return getTagsByMonitoryPolicyId(tenantId, listParams);
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

                final var nodes = List.copyOf(tag.getNodes());

                tag.getNodes().clear();
                tag.getActiveDiscoveries().clear();
                tag.getPassiveDiscoveries().clear();
                tag.getMonitorPolicyIds().clear();

                repository.delete(tag);

                for (final var node: nodes) {
                    this.nodeService.updateNodeMonitoredState(node.getId(),  node.getTenantId());
                }
            } else {
                throw new InventoryRuntimeException("Invalid Tag id: " + tagId.getValue());
            }
        }
    }

    @Transactional
    public void updateTags(String tenantId, TagCreateListDTO request) {
        if (request.getTagsList().isEmpty()) {
            return;
        }
        if (request.getEntityIdsList().isEmpty()) {
            return;
        }
        for (TagEntityIdDTO entityId : request.getEntityIdsList()) {
            removeEntityTagAssociations(tenantId, entityId);
        }
        addTags(tenantId, request);
    }

    private void removeEntityTagAssociations(String tenantId, TagEntityIdDTO entityId) {
        if (entityId.hasNodeId()) {
            Node node = getNode(tenantId, entityId.getNodeId());
            node.getTags().forEach(tag -> tag.getNodes().remove(node));
        } else if (entityId.hasActiveDiscoveryId()) {
            ActiveDiscovery activeDiscovery = getActiveDiscovery(tenantId, entityId.getActiveDiscoveryId());
            activeDiscovery.getTags().forEach(tag -> {
                tag.getActiveDiscoveries().remove(activeDiscovery);
            });
        } else if (entityId.hasPassiveDiscoveryId()) {
            PassiveDiscovery discovery = getPassiveDiscovery(tenantId, entityId.getPassiveDiscoveryId());
            discovery.getTags().forEach(tag -> {
                tag.getPassiveDiscoveries().remove(discovery);
            });
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

    private TagDTO addTagToActiveDiscovery(String tenantId, ActiveDiscovery discovery, TagCreateDTO tagCreateDTO) {
        String tagName = tagCreateDTO.getName();

        Optional<Tag> tagOpt = repository
            .findByTenantIdActiveDiscoveryIdAndName(tenantId, discovery.getId(), tagName);

        if (tagOpt.isPresent()) {
            return mapper.modelToDTO(tagOpt.get());
        }

        tagOpt = repository.findByTenantIdAndName(tenantId, tagName);
        Tag tag = tagOpt.orElseGet(() -> mapCreateTag(tenantId, tagCreateDTO));

        tag.getActiveDiscoveries().add(discovery);
        tag = repository.save(tag);

        return mapper.modelToDTO(tag);
    }

    private TagDTO addTagToPassiveDiscovery(String tenantId, PassiveDiscovery discovery, TagCreateDTO tagCreateDTO) {
        String tagName = tagCreateDTO.getName();

        Optional<Tag> tagOpt = repository
            .findByTenantIdPassiveDiscoveryIdAndName(tenantId, discovery.getId(), tagName);

        if (tagOpt.isPresent()) {
            return mapper.modelToDTO(tagOpt.get());
        }

        tagOpt = repository.findByTenantIdAndName(tenantId, tagName);
        Tag tag = tagOpt.orElseGet(() -> mapCreateTag(tenantId, tagCreateDTO));

        tag.getPassiveDiscoveries().add(discovery);
        tag = repository.save(tag);

        return mapper.modelToDTO(tag);
    }

    private TagDTO addTagsToMonitoringPolicy(String tenantId, long monitoringPolicyId, TagCreateDTO tagCreateDTO) {
        String tagName = tagCreateDTO.getName();
        var optional = repository.findByTenantIdAndName(tenantId, tagName);
        Tag tag = optional.orElseGet(() -> mapCreateTag(tenantId, tagCreateDTO));
        if (tag.getMonitorPolicyIds().stream().noneMatch(policyId -> policyId == monitoringPolicyId)) {
            tag.getMonitorPolicyIds().add(monitoringPolicyId);
        }
        tag = repository.save(tag);

        for (final var node: tag.getNodes()) {
            this.nodeService.updateNodeMonitoredState(node.getId(), node.getTenantId());
        }

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

    private ActiveDiscovery getActiveDiscovery(String tenantId, long credentialId) {
        Optional<ActiveDiscovery> discoveryOpt = activeDiscoveryRepository.findByTenantIdAndId(tenantId, credentialId);
        if (discoveryOpt.isEmpty()) {
            throw new InventoryRuntimeException("Active Discovery not found for id: " + credentialId);
        }
        return discoveryOpt.get();
    }

    private PassiveDiscovery getPassiveDiscovery(String tenantId, long trapdPassiveDiscoveryId) {
        Optional<PassiveDiscovery> passiveDiscoveryOpt = passiveDiscoveryRepository.findByTenantIdAndId(tenantId, trapdPassiveDiscoveryId);
        if (passiveDiscoveryOpt.isEmpty()) {
            throw new InventoryRuntimeException("Passive Discovery not found for id: " + trapdPassiveDiscoveryId);
        }
        return passiveDiscoveryOpt.get();
    }

    private List<TagDTO> getTagsByNodeId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        TagEntityIdDTO entityId = listParams.getEntityId();

        long nodeId = entityId.getNodeId();
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

    private List<TagDTO> getTagsByActiveDiscoveryId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        TagEntityIdDTO entityId = listParams.getEntityId();

        long activeDiscoveryId = entityId.getActiveDiscoveryId();
        if (listParams.hasParams()) {
            TagListParamsDTO params = listParams.getParams();
            String searchTerm = params.getSearchTerm();

            if (StringUtils.isNotEmpty(searchTerm)) {
                return repository.findByTenantIdAndActiveDiscoveryIdAndNameLike(tenantId, activeDiscoveryId, searchTerm)
                    .stream().map(mapper::modelToDTO).toList();
            }
        }
        return repository.findByTenantIdAndActiveDiscoveryId(tenantId, activeDiscoveryId)
            .stream().map(mapper::modelToDTO).toList();
    }

    private List<TagDTO> getTagsByPassiveDiscoveryId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        TagEntityIdDTO entityId = listParams.getEntityId();

        long passiveDiscoveryId = entityId.getPassiveDiscoveryId();
        if (listParams.hasParams()) {
            TagListParamsDTO params = listParams.getParams();
            String searchTerm = params.getSearchTerm();

            if (StringUtils.isNotEmpty(searchTerm)) {
                return repository.findByTenantIdAndPassiveDiscoveryIdAndNameLike(tenantId, passiveDiscoveryId, searchTerm)
                    .stream().map(mapper::modelToDTO).toList();
            }
        }
        return repository.findByTenantIdAndPassiveDiscoveryId(tenantId, passiveDiscoveryId)
            .stream().map(mapper::modelToDTO).toList();
    }

    private List<TagDTO> getTagsByMonitoryPolicyId(String tenantId, ListTagsByEntityIdParamsDTO listParams) {
        TagEntityIdDTO entityId = listParams.getEntityId();
        long monitoringPolicyId = entityId.getMonitoringPolicyId();
        List<Tag> tagList = new ArrayList<>();
        if (listParams.hasParams()) {
            TagListParamsDTO params = listParams.getParams();
            String searchTerm = params.getSearchTerm();
            if (StringUtils.isNotEmpty(searchTerm)) {
                tagList = repository.findByTenantIdAndNameLike(tenantId, searchTerm);
            }
        } else {
            tagList = repository.findByTenantId(tenantId);
        }
        if (tagList.isEmpty()) {
            return new ArrayList<>();
        }
        return tagList.stream().filter(tag -> tag.getMonitorPolicyIds().contains(monitoringPolicyId)).map(mapper::modelToDTO)
            .toList();
    }



    @Transactional
    public void insertOrUpdateTags(TagOperationList list) {
        list.getTagsList().forEach(tagOp -> {
            switch (tagOp.getOperation()) {
                case ASSIGN_TAG -> {
                    if (tagOp.getMonitoringPolicyIdList().isEmpty()) {
                        // Only handle tag operation updates with monitoring policies
                        return;
                    }
                    repository.findByTenantIdAndName(tagOp.getTenantId(), tagOp.getTagName())
                        .ifPresentOrElse(tag -> {
                            int oldSize = tag.getMonitorPolicyIds().size();
                            tagOp.getMonitoringPolicyIdList().forEach(id -> {
                                if (!tag.getMonitorPolicyIds().contains(id)) {
                                    tag.getMonitorPolicyIds().add(id);
                                }
                            });
                            var persisted = repository.save(tag);
                            log.info("added monitoring policyIds with data {} monitoring policy id size from {} to {}",
                                tagOp, oldSize, persisted.getMonitorPolicyIds().size());
                            for (final var node: persisted.getNodes()) {
                                this.nodeService.updateNodeMonitoredState(node.getId(), node.getTenantId());
                            }
                        }, () -> {
                            Tag tag = new Tag();
                            tag.setName(tagOp.getTagName());
                            tag.setTenantId(tagOp.getTenantId());
                            tag.setMonitorPolicyIds(tagOp.getMonitoringPolicyIdList());
                            var persisted = repository.save(tag);
                            log.info("inserted new tag with data {}", tagOp);
                            for (final var node: persisted.getNodes()) {
                                this.nodeService.updateNodeMonitoredState(node.getId(), node.getTenantId());
                            }
                        });
                }
                case REMOVE_TAG -> {

                    if (tagOp.getMonitoringPolicyIdList().isEmpty()) {
                        // Only handle tag operation updates with monitoring policies
                        return;
                    }
                    repository.findByTenantIdAndName(tagOp.getTenantId(), tagOp.getTagName())
                        .ifPresent(tag -> {
                            int oldSize = tag.getMonitorPolicyIds().size();
                            tagOp.getMonitoringPolicyIdList().forEach(id -> tag.getMonitorPolicyIds().remove(id));
                            if (tag.getMonitorPolicyIds().isEmpty() && tag.getNodes().isEmpty() &&
                                tag.getPassiveDiscoveries().isEmpty() && tag.getActiveDiscoveries().isEmpty()) {
                                repository.deleteById(tag.getId());
                                log.info("deleted tag {}", tagOp);
                            } else {
                                repository.save(tag);
                                log.info("removed monitoring policyIds for {} and monitoring policy size changed from {} to {}",
                                    tagOp, oldSize, tag.getMonitorPolicyIds().size());
                            }
                        });
                }
            }
        });
    }

    private boolean policyExists(long policyId, String tenantId) {
        try {
            var policy = alertClient.getPolicyById(policyId, tenantId);
            return policy != null;
        } catch (StatusRuntimeException ex) {
            log.error("Error during get policyId: {} tenantId: {}", policyId, tenantId);
            return false;
        }
    }
}
