package org.opennms.horizon.events.persistence.mapper;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.proto.EventDTO;

@Mapper(componentModel = "spring")
public interface EventMapper extends DateTimeMapper {

    @Mapping(source = "eventUei", target = "uei")
    EventDTO modelToDTO(Event event);

    default String map(Inet value) {
        if (value == null) {
            return "";
        } else {
            return value.getAddress();
        }
    }
}
