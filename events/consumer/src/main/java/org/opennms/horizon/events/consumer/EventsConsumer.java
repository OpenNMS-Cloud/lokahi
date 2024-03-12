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
package org.opennms.horizon.events.consumer;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.opennms.horizon.events.consumer.metrics.TenantMetricsTracker;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.yml")
public class EventsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(EventsConsumer.class);

    private final EventRepository eventRepository;

    private final TenantMetricsTracker metricsTracker;

    @Autowired
    public EventsConsumer(EventRepository eventRepository, TenantMetricsTracker metricsTracker) {
        this.eventRepository = eventRepository;
        this.metricsTracker = metricsTracker;
    }

    @KafkaListener(topics = "${kafka.trap-events-topic}", concurrency = "1")
    public void consume(@Payload byte[] data) {

        try {
            EventLog eventLog = EventLog.parseFrom(data);
            LOG.trace("Received events from kafka {}", eventLog);
            if (Strings.isNullOrEmpty(eventLog.getTenantId())) {
                LOG.warn("TenantId is empty. Dropping events: {}", eventLog);
                return;
            }
            List<Event> eventList = mapEventsFromLog(eventLog);
            eventRepository.saveAll(eventList);
            metricsTracker.addTenantEventSampleCount(eventLog.getTenantId(), eventList.size());
            LOG.info("Persisted {} event(s) in database for tenant {}.", eventList.size(), eventLog.getTenantId());
        } catch (InvalidProtocolBufferException e) {
            LOG.error(
                    "Exception while parsing events from payload. Events will be dropped. Payload: {}",
                    Arrays.toString(data),
                    e);
        }
    }

    @KafkaListener(topics = "${kafka.internal-events-topic}", concurrency = "1")
    public void consumeInternalEvents(@Payload byte[] data) {

        try {
            EventLog eventLog = EventLog.parseFrom(data);
            LOG.info("Received internal events from kafka {}", eventLog);
            if (Strings.isNullOrEmpty(eventLog.getTenantId())) {
                LOG.warn("TenantId is empty. Dropping events: {}", eventLog);
                return;
            }
            List<Event> eventList = mapEventsFromLog(eventLog);
            eventRepository.saveAll(eventList);
            metricsTracker.addTenantEventSampleCount(eventLog.getTenantId(), eventList.size());
            LOG.info("Persisted {} event(s) in database for tenant {}.", eventList.size(), eventLog.getTenantId());
        } catch (InvalidProtocolBufferException e) {
            LOG.error(
                    "Exception while parsing events from payload. Events will be dropped. Payload: {}",
                    Arrays.toString(data),
                    e);
        }
    }

    List<Event> mapEventsFromLog(EventLog eventLog) {
        return eventLog.getEventsList().stream().map(this::mapEventFromProto).collect(Collectors.toList());
    }

    private Event mapEventFromProto(org.opennms.horizon.events.proto.Event eventProto) {
        var event = new Event();
        event.setTenantId(eventProto.getTenantId());
        event.setEventUei(eventProto.getUei());
        try {
            event.setIpAddress(InetAddressUtils.getInetAddress(eventProto.getIpAddress()));
        } catch (IllegalArgumentException ex) {
            LOG.warn(
                    "Failed to parse IP address: {} for event: {}. Field will not be set.",
                    eventProto.getIpAddress(),
                    eventProto);
        }
        event.setNodeId(eventProto.getNodeId());
        event.setProducedTime(LocalDateTime.now());
        var eventParameters = new EventParameters();
        var paramsList =
                eventProto.getParametersList().stream().map(this::mapEventParam).collect(Collectors.toList());
        eventParameters.setParameters(paramsList);
        event.setEventParameters(eventParameters);
        event.setEventInfo(eventProto.getInfo().toByteArray());
        event.setLocationName(eventProto.getLocationName());
        event.setDescription(eventProto.getDescription());
        event.setLogMessage(eventProto.getLogMessage());

        return event;
    }

    private EventParameter mapEventParam(org.opennms.horizon.events.proto.EventParameter eventParameter) {
        var param = new EventParameter();
        param.setEncoding(eventParameter.getEncoding());
        param.setName(eventParameter.getName());
        param.setType(eventParameter.getType());
        param.setValue(eventParameter.getValue());
        return param;
    }
}
