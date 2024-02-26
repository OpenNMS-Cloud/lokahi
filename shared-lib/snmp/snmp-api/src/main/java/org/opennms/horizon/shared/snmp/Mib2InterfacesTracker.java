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
package org.opennms.horizon.shared.snmp;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mib2InterfacesTracker extends AggregateTracker {

    private static final Logger LOG = LoggerFactory.getLogger(Mib2InterfacesTracker.class);

    private static final NamedSnmpVar[] elemList = new NamedSnmpVar[22];

    static {
        int ndx = 0;
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPOCTETSTRING, "ifDescr", ".1.3.6.1.2.1.2.2.1.2", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPGAUGE32, "ifSpeed", ".1.3.6.1.2.1.2.2.1.5", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifInOctets", ".1.3.6.1.2.1.2.2.1.10", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifInUcastpkts", ".1.3.6.1.2.1.2.2.1.11", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifInNUcastpkts", ".1.3.6.1.2.1.2.2.1.12", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifInDiscards", ".1.3.6.1.2.1.2.2.1.13", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifInErrors", ".1.3.6.1.2.1.2.2.1.14", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifOutOctets", ".1.3.6.1.2.1.2.2.1.16", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifOutUcastPkts", ".1.3.6.1.2.1.2.2.1.17", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifOutNUcastPkts", ".1.3.6.1.2.1.2.2.1.18", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifOutDiscards", ".1.3.6.1.2.1.2.2.1.19", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifOutErrors", ".1.3.6.1.2.1.2.2.1.20", 6);

        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPOCTETSTRING, "ifName", ".1.3.6.1.2.1.31.1.1.1.1", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCInOctets", ".1.3.6.1.2.1.31.1.1.1.6", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCInUcastPkts", ".1.3.6.1.2.1.31.1.1.1.7", 6);
        elemList[ndx++] =
                new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCInMulticastPkts", ".1.3.6.1.2.1.31.1.1.1.8", 6);
        elemList[ndx++] =
                new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCInBroadcastPkts", ".1.3.6.1.2.1.31.1.1.1.9", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutOctets", ".1.3.6.1.2.1.31.1.1.1.10", 6);
        elemList[ndx++] =
                new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutUcastPkts", ".1.3.6.1.2.1.31.1.1.1.11", 6);
        elemList[ndx++] =
                new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutMulticastPkt", ".1.3.6.1.2.1.31.1.1.1.12", 6);
        elemList[ndx++] =
                new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutBroadcastPkt", ".1.3.6.1.2.1.31.1.1.1.13", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPOCTETSTRING, "ifHighSpeed", ".1.3.6.1.2.1.31.1.1.1.15", 6);
    }

    public Mib2InterfacesTracker() {
        super(NamedSnmpVar.getTrackersFor(elemList));
    }

    public static Optional<String> getAlias(SnmpResult snmpResult) {
        final SnmpObjId base = snmpResult.getBase();
        final SnmpValue value = snmpResult.getValue();
        for (final NamedSnmpVar snmpVar : elemList) {
            if (base.equals(snmpVar.getSnmpObjId())) {
                if (value.isError() || value.isEndOfMib()) {
                    return Optional.empty();
                } else {
                    return Optional.of(snmpVar.getAlias());
                }
            }
        }
        return Optional.empty();
    }
}
