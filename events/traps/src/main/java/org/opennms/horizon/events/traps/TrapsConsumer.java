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
package org.opennms.horizon.events.traps;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Setter;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.shared.events.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:application.yml")
public class TrapsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TrapsConsumer.class);

    @Autowired
    @Setter
    private EventForwarder eventForwarder;

    @Autowired
    @Setter
    private EventLogXmlToProtoMapper eventLogXmlToProtoMapper;

    @Autowired
    @Setter
    private TrapLogProtoToEventLogXmlMapper trapLogProtoToXmlMapper;

    @KafkaListener(topics = "${kafka.raw-traps-topic}", concurrency = "1")
    public void consume(@Payload byte[] data) {

        try {
            TenantLocationSpecificTrapLogDTO tenantLocationSpecificTrapLogDTO =
                    TenantLocationSpecificTrapLogDTO.parseFrom(data);

            LOG.debug("Received trap {}", tenantLocationSpecificTrapLogDTO);

            String tenantId = tenantLocationSpecificTrapLogDTO.getTenantId();

            // Convert to Event
            // TODO: do we need to use this intermediate format?
            Log eventLog = trapLogProtoToXmlMapper.convert(tenantLocationSpecificTrapLogDTO);

            // Convert to events into protobuf format
            EventLog eventLogProto = eventLogXmlToProtoMapper.convert(eventLog, tenantId);

            // Send them to kafka
            eventForwarder.sendTrapEvents(eventLogProto);

            eventLogProto.getEventsList().stream()
                    .filter(event -> (event.getNodeId() <= 0))
                    .forEach(event -> sendNewSuspectEvent(event, tenantId));

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing traps", e);
        }
    }

    private void sendNewSuspectEvent(org.opennms.horizon.events.proto.Event event, String tenantId) {
        var newEvent = org.opennms.horizon.events.proto.Event.newBuilder()
                .setTenantId(tenantId)
                .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
                .setIpAddress(event.getIpAddress())
                .setLocationId(event.getLocationId())
                .setInfo(event.getInfo())
                .addAllParameters(event.getParametersList())
                .build();

        eventForwarder.sendInternalEvent(newEvent);

        LOG.info("Sent new suspect event for interface {}", event.getIpAddress());
    }
}
