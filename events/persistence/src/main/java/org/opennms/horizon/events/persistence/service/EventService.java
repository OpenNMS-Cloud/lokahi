package org.opennms.horizon.events.persistence.service;

import org.opennms.horizon.events.persistence.mapper.EventMapper;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public EventService(EventRepository eventRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    public List<EventDTO> findEvents() {
        return eventRepository.findAll().stream()
            .map(eventMapper::modelToDtoWithParams)
            .collect(Collectors.toList());
    }

    public List<EventDTO> findEventsByNodeId(long nodeId) {
        return eventRepository.findAllByNodeId(nodeId).stream()
            .map(eventMapper::modelToDtoWithParams)
            .collect(Collectors.toList());
    }
}
