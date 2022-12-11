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

    public static NamedSnmpVar[] elemList = new NamedSnmpVar[10];
    private final SnmpStore snmpStore;
    private Map<String, SnmpResult> snmpResultMap = new TreeMap<>();


    static {
        int ndx = 0;
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
    }

    public Mib2InterfacesTracker() {
        super(NamedSnmpVar.getTrackersFor(elemList));
        snmpStore = new SnmpStore(elemList);
    }

    @Override
    protected void storeResult(SnmpResult res) {

        final SnmpObjId base = res.getBase();
        final SnmpValue value = res.getValue();

        for (final NamedSnmpVar var : elemList) {
            if (base.equals(var.getSnmpObjId())) {
                if (value.isError()) {
                    LOG.error("storeResult: got an error for alias {} [{}].[{}]," +
                        " but we should only be getting non-errors: {}", var.getAlias(), base, res.getInstance(), value);
                } else if (value.isEndOfMib()) {
                    LOG.debug("storeResult: got endOfMib for alias {} [{}].[{}], not storing", var.getAlias(), base, res.getInstance());
                } else {
                    snmpResultMap.put(var.getAlias(), res);
                }
            }
        }
    }

    public SnmpStore getSnmpStore() {
        return snmpStore;
    }

    public Map<String, SnmpResult> getSnmpResultMap() {
        return snmpResultMap;
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
