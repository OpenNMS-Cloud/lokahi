package org.opennms.horizon.events.persistence.mapper;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.proto.EventDTO;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper extends DateTimeMapper {

    @Mapping(source = "eventUei", target = "uei")
    EventDTO modelToDTO(Event event);

    default EventDTO modelToDtoWithParams(Event event) {
        EventDTO eventDTO = modelToDTO(event);

        EventDTO.Builder builder = EventDTO.newBuilder(eventDTO);

        EventParameters eventParams = event.getEventParameters();
        if (eventParams != null) {

            List<EventParameter> parameters = eventParams.getParameters();
            for (EventParameter param : parameters) {
                builder.addEventParams(dtoToModel(param));
            }
        }
        return builder.build();
    }

    org.opennms.horizon.events.proto.EventParameter dtoToModel(EventParameter param);

    default String map(Inet value) {
        if (value == null) {
            return "";
        } else {
            return value.getAddress();
        }
    }
}
