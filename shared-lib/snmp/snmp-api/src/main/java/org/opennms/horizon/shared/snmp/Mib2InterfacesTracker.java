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

package org.opennms.horizon.shared.snmp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCInMulticastPkts", ".1.3.6.1.2.1.31.1.1.1.8", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCInBroadcastPkts", ".1.3.6.1.2.1.31.1.1.1.9", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutOctets", ".1.3.6.1.2.1.31.1.1.1.10", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutUcastPkts", ".1.3.6.1.2.1.31.1.1.1.11", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutMulticastPkt", ".1.3.6.1.2.1.31.1.1.1.12", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER64, "ifHCOutBroadcastPkt", ".1.3.6.1.2.1.31.1.1.1.13", 6);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPOCTETSTRING, "ifHighSpeed", ".1.3.6.1.2.1.31.1.1.1.15", 6);
    }

    public Mib2InterfacesTracker() {
        super(NamedSnmpVar.getTrackersFor(elemList));
    }


    public static Optional<String> getAlias(SnmpResult snmpResult) {
        final SnmpObjId base = snmpResult.getBase();
        final SnmpValue value = snmpResult.getValue();
        for (final NamedSnmpVar var : elemList) {
            if (base.equals(var.getSnmpObjId())) {
                if (value.isError() || value.isEndOfMib()) {
                    return Optional.empty();
                } else {
                    return Optional.of(var.getAlias());
                }
            }
        }
        return Optional.empty();
    }
}
