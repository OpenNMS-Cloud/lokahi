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
package org.opennms.horizon.minion.snmp;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.List;
import org.opennms.horizon.shared.snmp.Collectable;
import org.opennms.horizon.shared.snmp.IfNumberTracker;
import org.opennms.horizon.shared.snmp.SnmpResult;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.SysUpTimeTracker;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpValueMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;

public class SnmpCollectionSet {

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
        trackers.add(ifNumberTracker);
        trackers.add(sysUpTimeTracker);
        return trackers;
    }

    static void addResult(
            SnmpResult result, SnmpResponseMetric.Builder builder, String alias, String ifName, String ipAddress) {
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

    private static SnmpResultMetric mapResult(
            org.opennms.horizon.shared.snmp.SnmpResult result, String alias, String ifName, String ipAddress) {
        return SnmpResultMetric.newBuilder()
                .setBase(result.getBase().toString())
                .setInstance(result.getInstance().toString())
                .setValue(mapValue(result.getValue()))
                .setAlias(alias)
                .setIfName(ifName)
                .setIpAddress(ipAddress)
                .build();
    }

    public static org.opennms.horizon.snmp.api.SnmpValueMetric mapValue(SnmpValue value) {
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
