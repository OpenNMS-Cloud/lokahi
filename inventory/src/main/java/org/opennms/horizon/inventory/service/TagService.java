package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagRemoveDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.TagMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository repository;
    private final NodeRepository nodeRepository;
    private final TagMapper mapper;

    @Transactional
    public TagDTO createTag(String tenantId, TagCreateDTO request) {
        long nodeId = request.getNodeId();
        String tagName = request.getName();

        Optional<Tag> tagOpt = repository.find(tenantId, nodeId, tagName);
        if (tagOpt.isPresent()) {
            return mapper.modelToDTO(tagOpt.get());
        }

        Node node = getNode(tenantId, nodeId);

        tagOpt = repository.find(tenantId, tagName);
        Tag tag = tagOpt.orElseGet(() -> mapCreateTag(tenantId, request));

        tag.getNodes().add(node);
        tag = repository.save(tag);

        return mapper.modelToDTO(tag);
    }

    @Transactional
    public void removeTag(String tenantId, TagRemoveDTO request) {
        long tagId = request.getTagId();
        long nodeId = request.getNodeId();

        Tag tag = getTag(tenantId, tagId);

        if (tag.getNodes().isEmpty()) {
            repository.delete(tag);
        } else {
            Node node = getNode(tenantId, nodeId);
            tag.getNodes().removeIf(it -> it.getId() == node.getId());
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
}
