/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service.taskset.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventParameter;
import org.opennms.horizon.inventory.component.InternalEventProducer;
import org.opennms.horizon.inventory.model.MonitoredServiceState;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceStateRepository;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorResponseService {

    private final MonitoredServiceStateRepository serviceStateRepository;

    private final MonitoredServiceRepository monitoredServiceRepository;

    private final InternalEventProducer eventProducer;


    public void updateMonitoredState(String tenantId, MonitorResponse monitorResponse) {

        if (monitorResponse.getMonitorType().equals(MonitorType.ECHO)) {
            // No need to handle Echo monitor response
            return;
        }
        long monitorServiceId = monitorResponse.getMonitorServiceId();
        var optionalService = monitoredServiceRepository.findByIdAndTenantId(monitorServiceId, tenantId);
        if (optionalService.isEmpty()) {
            return;
        }
        var monitoredService = optionalService.get();
        var optionalServiceState = serviceStateRepository.findByTenantIdAndMonitoredServiceId(tenantId, monitorServiceId);
        var previousState = Boolean.TRUE;
        Boolean statusFromMonitor = "Up".equalsIgnoreCase(monitorResponse.getStatus()) ? Boolean.TRUE : Boolean.FALSE;
        if (optionalServiceState.isPresent()) {
            previousState = optionalServiceState.get().getServiceState();
            var monitorServiceState = optionalServiceState.get();
            monitorServiceState.setServiceState(statusFromMonitor);
            serviceStateRepository.save(monitorServiceState);
        } else {
            MonitoredServiceState monitoredServiceState = new MonitoredServiceState();
            monitoredServiceState.setTenantId(tenantId);
            monitoredServiceState.setMonitoredService(monitoredService);
            monitoredServiceState.setServiceState(statusFromMonitor);
            serviceStateRepository.save(monitoredServiceState);
        }
        if (!Objects.equals(statusFromMonitor, previousState)) {
            // State changed, send event
            triggerEvent(tenantId, monitorResponse, statusFromMonitor);
        }
    }

    private void triggerEvent(String tenantId, MonitorResponse monitorResponse, Boolean statusFromMonitor) {
        var eventBuilder = Event.newBuilder();
        if (statusFromMonitor) {
            eventBuilder.setUei(EventConstants.SERVICE_RESTORED_EVENT_UEI);
        } else {
            eventBuilder.setUei(EventConstants.SERVICE_UNREACHABLE_EVENT_UEI);
        }
        eventBuilder.setIpAddress(monitorResponse.getIpAddress());
        eventBuilder.setTenantId(tenantId);
        eventBuilder.setNodeId(monitorResponse.getNodeId());
        eventBuilder.setProducedTimeMs(monitorResponse.getTimestamp());
        eventBuilder.setDescription(monitorResponse.getReason());
        var serviceNameParam = EventParameter.newBuilder().setName("serviceName")
            .setValue(monitorResponse.getMonitorType().name()).build();
        var serviceIdParam = EventParameter.newBuilder().setName("serviceId")
            .setValue(String.valueOf(monitorResponse.getMonitorServiceId())).build();
        eventBuilder.addParameters(serviceNameParam)
            .addParameters(serviceIdParam);
        var eventLog = EventLog.newBuilder().setTenantId(tenantId).addEvents(eventBuilder.build());
        eventProducer.sendEvent(eventLog.build());
    }
}
