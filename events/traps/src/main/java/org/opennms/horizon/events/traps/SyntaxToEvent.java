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


import org.opennms.horizon.config.EventConstants;
import org.opennms.horizon.config.xml.Parm;
import org.opennms.horizon.config.xml.Value;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SyntaxToEvent {

    private static final Logger LOG = LoggerFactory.getLogger(SyntaxToEvent.class);

    public static Map<Integer, String> syntaxToEventsMap = new HashMap<>();

    static {
        setup();
    }

    public static void setup() {
        syntaxToEventsMap.put(SnmpValue.SNMP_INT32, EventConstants.TYPE_SNMP_INT32);
        syntaxToEventsMap.put(SnmpValue.SNMP_NULL, EventConstants.TYPE_SNMP_NULL);
        syntaxToEventsMap.put(SnmpValue.SNMP_OBJECT_IDENTIFIER, EventConstants.TYPE_SNMP_OBJECT_IDENTIFIER);
        syntaxToEventsMap.put(SnmpValue.SNMP_IPADDRESS, EventConstants.TYPE_SNMP_IPADDRESS);
        syntaxToEventsMap.put(SnmpValue.SNMP_TIMETICKS, EventConstants.TYPE_SNMP_TIMETICKS);
        syntaxToEventsMap.put(SnmpValue.SNMP_COUNTER32, EventConstants.TYPE_SNMP_COUNTER32);
        syntaxToEventsMap.put(SnmpValue.SNMP_GAUGE32, EventConstants.TYPE_SNMP_GAUGE32);
        syntaxToEventsMap.put(SnmpValue.SNMP_OCTET_STRING, EventConstants.TYPE_SNMP_OCTET_STRING);
        syntaxToEventsMap.put(SnmpValue.SNMP_OPAQUE, EventConstants.TYPE_SNMP_OPAQUE);
        syntaxToEventsMap.put(SnmpValue.SNMP_COUNTER64, EventConstants.TYPE_SNMP_COUNTER64);
        syntaxToEventsMap.put(-1, EventConstants.TYPE_STRING);
    }

    public static Optional<Parm> processSyntax(final String name, final SnmpValue value) {
        final Value val = new Value();
        String type = syntaxToEventsMap.get(value.getType());
        String encoding = null;
        if (type != null) {
            val.setType(type);
            if (value.isDisplayable()) {
                if (name.matches(".*[Mm][Aa][Cc].*")) {
                    encoding = EventConstants.XML_ENCODING_MAC_ADDRESS;
                } else {
                    encoding = EventConstants.XML_ENCODING_TEXT;
                }
            } else {
                if (value.getBytes().length == 6) {
                    encoding = EventConstants.XML_ENCODING_MAC_ADDRESS;
                } else {
                    encoding = EventConstants.XML_ENCODING_BASE64;
                }
            }
            val.setEncoding(encoding);
            val.setContent(EventConstants.toString(encoding, value));
        } else {
            LOG.error("Couldn't match snmp value type {} to event type", value.getType());
            return Optional.empty();
        }

        final Parm parm = new Parm();
        parm.setParmName(name);
        parm.setValue(val);

        return Optional.of(parm);
    }
}

