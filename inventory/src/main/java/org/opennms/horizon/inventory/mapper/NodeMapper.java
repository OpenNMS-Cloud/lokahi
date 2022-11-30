package org.opennms.horizon.inventory.mapper;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.Node;

import java.util.List;

@Mapper(componentModel = "spring", uses = IpInterfaceMapper.class,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface NodeMapper extends DateTimeMapper {

    @Mapping(source = "ipInterfacesList", target = "ipInterfaces")
    Node dtoToModel(NodeDTO dto);

    @Mapping(source = "ipInterfaces", target = "ipInterfacesList")
    NodeDTO modelToDTO(Node model);
}
