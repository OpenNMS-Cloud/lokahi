package org.opennms.horizon.inventory.mapper;

import org.mapstruct.Mapper;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.Node;

@Mapper(componentModel = "spring")
public interface NodeMapper extends DateTimeMapper {
    Node dtoToModel(NodeDTO dto);
    NodeDTO modelToDTO(Node model);
}
