package org.opennms.horizon.inventory.mapper;

import org.mapstruct.Mapper;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.model.MonitoringSystem;

@Mapper(componentModel = "spring")
public interface MonitoringSystemMapper extends DateTimeMapper {
    MonitoringSystem dtoToModel(MonitoringSystemDTO dto);
    MonitoringSystemDTO modelToDTO(MonitoringSystem model);
}
