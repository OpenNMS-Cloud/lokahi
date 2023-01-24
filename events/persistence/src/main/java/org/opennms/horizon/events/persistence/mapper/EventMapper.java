package org.opennms.horizon.events.persistence.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventInfoDTO;
import org.opennms.horizon.events.proto.EventParameterDTO;
import org.opennms.horizon.shared.utils.InetAddressUtils;

import java.net.InetAddress;
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
                builder.addEventParams(modelToDTO(param));
            }
        }
        return builder.build();
    }

    EventParameterDTO modelToDTO(EventParameter param);

    default EventInfoDTO map(byte[] value) {
        try {
            return EventInfoDTO.parseFrom(value);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    default String map(InetAddress value) {
        if (value == null) {
            return "";
        } else {
            return InetAddressUtils.toIpAddrString(value);
        }
    }
}
