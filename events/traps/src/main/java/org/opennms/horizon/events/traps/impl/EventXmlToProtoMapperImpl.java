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
package org.opennms.horizon.events.traps.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.EventParameter;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.opennms.horizon.events.traps.EventXmlToProtoMapper;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Parm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventXmlToProtoMapperImpl implements EventXmlToProtoMapper {

    private static final Logger LOG = LoggerFactory.getLogger(EventXmlToProtoMapperImpl.class);

    public org.opennms.horizon.events.proto.Event convert(Event event, String tenantId) {
        org.opennms.horizon.events.proto.Event.Builder eventBuilder =
                org.opennms.horizon.events.proto.Event.newBuilder()
                        .setTenantId(tenantId)
                        .setUei(event.getUei())
                        .setProducedTimeMs(event.getCreationTime().getTime())
                        .setNodeId(event.getNodeid())
                        .setLocationId(event.getDistPoller())
                        .setIpAddress(event.getInterface());

        if (event.getDescr() != null) {
            eventBuilder.setDescription(event.getDescr());
        }
        if (event.getLogmsg() != null) {
            eventBuilder.setLogMessage(event.getLogmsg().getContent());
        }

        mapEventInfo(event, eventBuilder);

        List<EventParameter> eventParameters = mapEventParams(event);
        eventBuilder.addAllParameters(eventParameters);
        return eventBuilder.build();
    }

    private void mapEventInfo(Event event, org.opennms.horizon.events.proto.Event.Builder eventBuilder) {
        var snmp = event.getSnmp();
        if (snmp != null) {
            var eventInfo = EventInfo.newBuilder()
                    .setSnmp(SnmpInfo.newBuilder()
                            .setId(snmp.getId())
                            .setVersion(snmp.getVersion())
                            .setGeneric(snmp.getGeneric())
                            .setCommunity(snmp.getCommunity())
                            .setSpecific(snmp.getSpecific())
                            .setTrapOid(snmp.getTrapOID())
                            .build())
                    .build();
            eventBuilder.setInfo(eventInfo);
        }
    }

    private List<EventParameter> mapEventParams(Event event) {

        return event.getParmCollection().stream()
                .map(this::mapEventParm)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private EventParameter mapEventParm(Parm parm) {
        if (parm.isValid()) {
            var eventParm = EventParameter.newBuilder()
                    .setName(parm.getParmName())
                    .setType(parm.getValue().getType())
                    .setEncoding(parm.getValue().getEncoding())
                    .setValue(parm.getValue().getContent())
                    .build();
            return eventParm;
        }
        return null;
    }
}
