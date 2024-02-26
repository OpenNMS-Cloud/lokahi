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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.opennms.horizon.events.EventConstants;
import org.opennms.horizon.events.xml.Parm;
import org.opennms.horizon.events.xml.Value;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyntaxToEvent {

    private static final Logger LOG = LoggerFactory.getLogger(SyntaxToEvent.class);

    private static Pattern pattern = Pattern.compile(".*[Mm][Aa][Cc].*");

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
                if (pattern.matcher(name).matches()) {
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
