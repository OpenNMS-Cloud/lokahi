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

import com.google.protobuf.ByteString;
import org.opennms.horizon.shared.snmp.Collectable;
import org.opennms.horizon.shared.snmp.IfNumberTracker;
import org.opennms.horizon.shared.snmp.SnmpNodeTracker;
import org.opennms.horizon.shared.snmp.SnmpResult;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.SysUpTimeTracker;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpValueMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SnmpCollectionSet {

    private final Logger LOG = LoggerFactory.getLogger(SnmpCollectionSet.class);

    private final SnmpResponseMetric.Builder builder;
    private final List<Collectable> trackers = new ArrayList<>();

    public SnmpCollectionSet(SnmpResponseMetric.Builder builder) {
        this.builder = builder;
    }


    public List<Collectable> addDefaultTrackers() {
        IfNumberTracker ifNumberTracker = new IfNumberTracker() {
            @Override
            protected void storeResult(org.opennms.horizon.shared.snmp.SnmpResult res) {
                addResult(res, builder, "ifNumber");
            }
        };
        SysUpTimeTracker sysUpTimeTracker = new SysUpTimeTracker() {
            @Override
            protected void storeResult(org.opennms.horizon.shared.snmp.SnmpResult res) {
                addResult(res, builder, "sysUpTime");
            }
        };
        SnmpNodeTracker nodeTracker = new SnmpNodeTracker() {

            @Override
            protected void storeResult(org.opennms.horizon.shared.snmp.SnmpResult res) {
                var aliasOptional = getAlias(res);
                aliasOptional.ifPresent((alias) -> addResult(res, builder, alias));
            }
        };
        trackers.add(ifNumberTracker);
        trackers.add(sysUpTimeTracker);
        return trackers;
    }

    static void addResult(SnmpResult result, SnmpResponseMetric.Builder builder,
                          String alias, String ifName, String ipAddress) {
        builder.addResults(mapResult(result, alias, ifName, ipAddress));
    }

    static void addResult(SnmpResult result, SnmpResponseMetric.Builder builder, String alias) {
        builder.addResults(mapResult(result, alias));
    }

    private static SnmpResultMetric mapResult(org.opennms.horizon.shared.snmp.SnmpResult result, String alias) {
        return SnmpResultMetric.newBuilder()
            .setBase(result.getBase().toString())
            .setInstance(result.getInstance().toString())
            .setValue(mapValue(result.getValue()))
            .setAlias(alias)
            .build();
    }

    private static SnmpResultMetric mapResult(org.opennms.horizon.shared.snmp.SnmpResult result,
                                              String alias, String ifName, String ipAddress) {
        return SnmpResultMetric.newBuilder()
            .setBase(result.getBase().toString())
            .setInstance(result.getInstance().toString())
            .setValue(mapValue(result.getValue()))
            .setAlias(alias)
            .setIfName(ifName)
            .setIpAddress(ipAddress)
            .build();
    }

    private static org.opennms.horizon.snmp.api.SnmpValueMetric mapValue(SnmpValue value) {
        SnmpValueMetric.Builder builder = org.opennms.horizon.snmp.api.SnmpValueMetric.newBuilder();
        SnmpValueType valueType = SnmpValueType.forNumber(value.getType());
        builder.setType(valueType);
        switch (valueType) {
            case INT32:
                builder.setSint64(value.toLong());
                break;
            case OCTET_STRING:
            case OPAQUE:
                builder.setBytes(ByteString.copyFrom(value.getBytes()));
                break;
            case NULL:
                builder.setBytes(ByteString.EMPTY);
                break;
            case OBJECT_IDENTIFIER:
                builder.setString(value.toSnmpObjId().toString());
                break;
            case IPADDRESS:
                byte[] address = value.toInetAddress().getAddress();
                builder.setBytes(ByteString.copyFrom(address));
                break;
            case COUNTER32:
            case GAUGE32:
            case TIMETICKS:
                builder.setUint64(value.toLong());
                break;
            case COUNTER64:
                builder.setBytes(ByteString.copyFrom(value.toBigInteger().toByteArray()));
                break;
            case NO_SUCH_OBJECT:
            case NO_SUCH_INSTANCE:
            case END_OF_MIB:
                builder.setBytes(ByteString.EMPTY);
                builder.setBytes(ByteString.EMPTY);
                builder.setBytes(ByteString.EMPTY);

        }
        return builder.build();
    }

    public List<Collectable> getTrackers() {
        return trackers;
    }
}
