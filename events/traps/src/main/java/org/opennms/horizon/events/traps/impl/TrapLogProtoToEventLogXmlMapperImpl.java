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

import java.net.InetAddress;
import java.util.function.Function;
import lombok.Setter;
import org.opennms.horizon.events.traps.EventFactory;
import org.opennms.horizon.events.traps.TrapLogProtoToEventLogXmlMapper;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TrapLogProtoToEventLogXmlMapperImpl implements TrapLogProtoToEventLogXmlMapper {
    private static final Logger LOG = LoggerFactory.getLogger(TrapLogProtoToEventLogXmlMapperImpl.class);

    @Autowired
    @Setter
    private EventFactory eventFactory;

    // Testability
    @Setter
    private Function<String, InetAddress> inetAddressLookupFunction = InetAddressUtils::getInetAddress;

    @Override
    public Log convert(TenantLocationSpecificTrapLogDTO messageLog) {
        Log log = new Log();
        Events events = new Events();
        log.setEvents(events);

        String tenantId = messageLog.getTenantId();
        String locationId = messageLog.getLocationId();

        // TODO: Add metrics for Traps received/error/dropped.
        for (TrapDTO eachMessage : messageLog.getTrapDTOList()) {
            try {
                var event = eventFactory.createEventFrom(
                        eachMessage,
                        messageLog.getIdentity().getSystemId(),
                        locationId,
                        inetAddressLookupFunction.apply(messageLog.getTrapAddress()),
                        tenantId);

                if (event != null) {
                    events.addEvent(event);
                }
            } catch (Throwable e) {
                LOG.error("Unexpected error processing trap: {}", eachMessage, e);
            }
        }
        return log;
    }
}
