/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.events.traps;

import org.opennms.horizon.config.api.EventBuilder;
import org.opennms.horizon.config.api.EventConfDao;
import org.opennms.horizon.config.xml.Event;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapIdentity;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.snmp.api.SnmpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Date;
import java.util.Optional;

import static org.opennms.horizon.config.EventConstants.OID_SNMP_IFINDEX_STRING;
import static org.opennms.horizon.shared.utils.InetAddressUtils.str;

public class EventCreator {

    private static final Logger LOG = LoggerFactory.getLogger(EventCreator.class);

    private static final SnmpObjId OID_SNMP_IFINDEX = SnmpObjId.get(OID_SNMP_IFINDEX_STRING);

    private final EventConfDao eventConfDao;
    private final SnmpHelper snmpHelper;

    public EventCreator(EventConfDao eventConfDao, SnmpHelper snmpHelper) {
        this.eventConfDao = eventConfDao;
        this.snmpHelper = snmpHelper;
    }

    public Event createEventFrom(final TrapDTO trapDTO, final String systemId, final String location, final InetAddress trapAddress) {
        LOG.debug("{} trap - trapInterface: {}", trapDTO.getVersion(), trapDTO.getAgentAddress());

        // Set event data
        final EventBuilder eventBuilder = new EventBuilder(null, "trapd");
        eventBuilder.setTime(new Date(trapDTO.getCreationTime()));
        eventBuilder.setCommunity(trapDTO.getCommunity());
        eventBuilder.setSnmpTimeStamp(trapDTO.getTimestamp());
        eventBuilder.setSnmpVersion(trapDTO.getVersion());
        eventBuilder.setSnmpHost(str(trapAddress));
        eventBuilder.setInterface(trapAddress);
        eventBuilder.setHost(trapDTO.getAgentAddress());

        // Handle trap identity
        final TrapIdentity trapIdentity = trapDTO.getTrapIdentity();
        LOG.debug("Trap Identity {}", trapIdentity);
        eventBuilder.setGeneric(trapIdentity.getGeneric());
        eventBuilder.setSpecific(trapIdentity.getSpecific());
        eventBuilder.setEnterpriseId(trapIdentity.getEnterpriseId());
        eventBuilder.setTrapOID(trapIdentity.getTrapOID());

        // Handle var bindings
        for (SnmpResult eachResult : trapDTO.getSnmpResultsList()) {
            final SnmpObjId name = SnmpObjId.get(eachResult.getBase());
            final SnmpValue value  = snmpHelper.getValueFactory().getValue(eachResult.getValue().getTypeValue(),
                eachResult.getValue().getValue().toByteArray());
            eventBuilder.addParam(SyntaxToEvent.processSyntax(name.toString(), value));
            if (OID_SNMP_IFINDEX.isPrefixOf(name)) {
                eventBuilder.setIfIndex(value.toInt());
            }
        }

        // Resolve Node id and set, if known by OpenNMS
        resolveNodeId(location, trapAddress)
            .ifPresent(eventBuilder::setNodeid);

        // Note: Filling in Location instead of SystemId. Do we really need to know about system id ?
        if (systemId != null) {
            eventBuilder.setDistPoller(location);
        }

        // Get event template and set uei, if unknown
        final Event event = eventBuilder.getEvent();
        final org.opennms.horizon.config.conf.xml.Event econf = eventConfDao.findByEvent(event);
        if (econf == null || econf.getUei() == null) {
            event.setUei("uei.opennms.org/default/trap");
        } else {
            event.setUei(econf.getUei());
        }
        return event;
    }

    private Optional<Integer> resolveNodeId(String location, InetAddress trapAddress) {
        // TODO: Query inventory service for node id
        return Optional.empty();
    }
}
