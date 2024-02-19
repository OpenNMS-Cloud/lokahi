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
package org.opennms.horizon.inventory.service.taskset.response;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventParameter;
import org.opennms.horizon.inventory.component.InternalEventProducer;
import org.opennms.horizon.inventory.model.MonitoredServiceState;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceStateRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorResponseService {

    private final MonitoredServiceStateRepository serviceStateRepository;

    private final MonitoredServiceRepository monitoredServiceRepository;

    private final MonitoringLocationRepository monitoringLocationRepository;

    private final InternalEventProducer eventProducer;

    public void updateMonitoredState(String tenantId, String locationId, MonitorResponse monitorResponse) {
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
        var optionalServiceState =
                serviceStateRepository.findByTenantIdAndMonitoredServiceId(tenantId, monitorServiceId);
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
            monitoredServiceState.setFirstObservationTime(LocalDateTime.now());
            serviceStateRepository.save(monitoredServiceState);
        }
        if (!Objects.equals(statusFromMonitor, previousState)) {
            // State changed, send event
            triggerEvent(tenantId, locationId, monitorResponse, statusFromMonitor);
        }
    }

    private void triggerEvent(
            String tenantId, String locationId, MonitorResponse monitorResponse, boolean statusFromMonitor) {
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
        eventBuilder.setLocationId(locationId);
        monitoringLocationRepository
                .findByIdAndTenantId(Long.parseLong(locationId), tenantId)
                .ifPresent(l -> eventBuilder.setLocationName(l.getLocation()));
        var serviceNameParam = EventParameter.newBuilder()
                .setName("serviceName")
                .setValue(monitorResponse.getMonitorType().name())
                .build();
        var serviceIdParam = EventParameter.newBuilder()
                .setName("serviceId")
                .setValue(String.valueOf(monitorResponse.getMonitorServiceId()))
                .build();
        eventBuilder.addParameters(serviceNameParam).addParameters(serviceIdParam);
        var eventLog = EventLog.newBuilder().setTenantId(tenantId).addEvents(eventBuilder.build());
        eventProducer.sendEvent(eventLog.build());
    }
}
