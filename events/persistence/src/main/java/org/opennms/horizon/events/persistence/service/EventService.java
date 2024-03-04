/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.events.persistence.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opennms.horizon.events.persistence.mapper.EventMapper;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.persistence.util.SearchEventsSpecification;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventsSearchBy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public EventService(EventRepository eventRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    public List<Event> findEvents(String tenantId) {
        return eventRepository.findAllByTenantId(tenantId).stream()
                .map(eventMapper::modelToDtoWithParams)
                .collect(Collectors.toList());
    }

    public List<Event> findEventsByNodeId(String tenantId, long nodeId) {
        return eventRepository.findAllByTenantIdAndNodeId(tenantId, nodeId).stream()
                .map(eventMapper::modelToDtoWithParams)
                .collect(Collectors.toList());
    }


    public List<Event> searchEvents(String tenantId,EventsSearchBy searchBy)  {
       return eventRepository.findAll(Specification.where(SearchEventsSpecification.hasDescription(tenantId,searchBy.getNodeId(),searchBy.getSearchTerm()))
               .or(SearchEventsSpecification.hasLocationName(tenantId,searchBy.getNodeId(),searchBy.getSearchTerm()))
               .or(SearchEventsSpecification.hasLogMessage(tenantId,searchBy.getNodeId(),searchBy.getSearchTerm()))
               .or(SearchEventsSpecification.hasEventsUei(tenantId,searchBy.getNodeId(),searchBy.getSearchTerm()))
               .or(SearchEventsSpecification.hasIpAddress(tenantId,searchBy.getNodeId(),searchBy.getSearchTerm())))
           .stream().map(eventMapper::modelToDtoWithParams).filter(Objects::nonNull).collect(Collectors.toList());

    }

}
