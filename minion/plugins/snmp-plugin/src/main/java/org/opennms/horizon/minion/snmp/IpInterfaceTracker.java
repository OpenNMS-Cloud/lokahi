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

package org.opennms.horizon.minion.snmp;

import org.opennms.horizon.shared.snmp.AggregateTracker;
import org.opennms.horizon.shared.snmp.Collectable;
import org.opennms.horizon.shared.snmp.NamedSnmpVar;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpResult;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public class IpInterfaceTracker extends AggregateTracker {

    private final Logger LOG = LoggerFactory.getLogger(IpInterfaceTracker.class);
    public static NamedSnmpVar[] elemList = new NamedSnmpVar[2];
    public static final String IP_ADDR_ENT_ADDR = "ipAdEntAddr";
    public static final String IP_ADDR_IF_INDEX = "ipAdEntIfIndex";
    private final InetAddress ipAddress;
    private Integer indexForIfIndex;
    private final Map<Integer, SnmpResult> ifIndexMap = new ConcurrentSkipListMap<>();

    public IpInterfaceTracker(InetAddress address) {
        super(NamedSnmpVar.getTrackersFor(elemList));
        ipAddress = address;
    }

    static {

        int ndx = 0;

        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPIPADDRESS, IP_ADDR_ENT_ADDR, ".1.3.6.1.2.1.4.20.1.1", 2);
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPINT32, IP_ADDR_IF_INDEX, ".1.3.6.1.2.1.4.20.1.2", 2);

    }

    @Override
    protected void storeResult(SnmpResult res) {
        Optional<String> optional = getAlias(res);

        optional.ifPresent(s -> LOG.info("result for alias {}, {}", s, res));

        if (optional.isPresent() && IP_ADDR_ENT_ADDR.equals(optional.get())) {
            if (this.ipAddress.equals(res.getValue().toInetAddress())) {
                indexForIfIndex = res.getInstance().toInt();
            }
        }
        if (optional.isPresent() && IP_ADDR_IF_INDEX.equals(optional.get())) {
            ifIndexMap.put(res.getInstance().toInt(), res);
        }
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


    public Integer getIfIndex() {
        if (indexForIfIndex != null) {
            SnmpResult result = ifIndexMap.get(indexForIfIndex);
            if (result != null) {
                return result.getValue().toInt();
            }
        }
        return null;
    }

}
