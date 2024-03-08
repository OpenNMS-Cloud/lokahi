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
package org.opennms.horizon.minion.nodescan;

import java.net.InetAddress;
import java.util.Optional;
import org.opennms.horizon.shared.snmp.RowCallback;
import org.opennms.horizon.shared.snmp.SnmpInstId;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpRowResult;
import org.opennms.horizon.shared.snmp.SnmpValue;
import org.opennms.horizon.shared.snmp.TableTracker;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IPAddrTracker extends TableTracker {
    private static final Logger LOG = LoggerFactory.getLogger(IPAddrTracker.class);

    /** Constant <code>IP_ADDR_TABLE_ENTRY</code> */
    public static final SnmpObjId IP_ADDR_TABLE_ENTRY = SnmpObjId.get(".1.3.6.1.2.1.4.20.1");

    /** Constant <code>IP_ADDR_ENT_ADDR</code> */
    public static final SnmpObjId IP_ADDR_ENT_ADDR = SnmpObjId.get(IP_ADDR_TABLE_ENTRY, "1");
    /** Constant <code>IP_ADDR_IF_INDEX</code> */
    public static final SnmpObjId IP_ADDR_IF_INDEX = SnmpObjId.get(IP_ADDR_TABLE_ENTRY, "2");
    /** Constant <code>IP_ADDR_ENT_NETMASK</code> */
    public static final SnmpObjId IP_ADDR_ENT_NETMASK = SnmpObjId.get(IP_ADDR_TABLE_ENTRY, "3");
    /** Constant <code>IP_ADDR_ENT_BCASTADDR</code> */
    public static final SnmpObjId IP_ADDR_ENT_BCASTADDR = SnmpObjId.get(IP_ADDR_TABLE_ENTRY, "4");

    private static SnmpObjId[] s_tableColumns =
            new SnmpObjId[] {IP_ADDR_ENT_ADDR, IP_ADDR_IF_INDEX, IP_ADDR_ENT_NETMASK, IP_ADDR_ENT_BCASTADDR};

    static class IPInterfaceRow extends SnmpRowResult {

        public IPInterfaceRow(int columnCount, SnmpInstId instance) {
            super(columnCount, instance);
        }

        public Integer getIfIndex() {
            SnmpValue value = getValue(IP_ADDR_IF_INDEX);
            return value == null ? null : value.toInt();
        }

        public String getIpAddress() {
            SnmpValue value = getValue(IP_ADDR_ENT_ADDR);
            if (value != null) {
                return InetAddressUtils.str(value.toInetAddress());
            } else {
                // instance for ipAddr Table it ipAddr
                SnmpInstId inst = getInstance();
                if (inst != null) {
                    final String addr = InetAddressUtils.normalize(inst.toString());
                    if (addr == null) {
                        throw new IllegalArgumentException("cannot convert " + inst + " to an InetAddress");
                    }
                    return addr;
                } else {
                    return null;
                }
            }
        }

        private InetAddress getNetMask() {
            SnmpValue value = getValue(IP_ADDR_ENT_NETMASK);
            return value == null ? null : value.toInetAddress();
        }

        public Optional<IpInterfaceResult> createInterfaceFromRow() {

            final Integer ifIndex = getIfIndex();
            final String ipAddr = getIpAddress();
            final InetAddress netMask = getNetMask();

            LOG.debug("createInterfaceFromRow: ifIndex = {}, ipAddress = {}, netmask = {}", ifIndex, ipAddr, netMask);

            if (ipAddr == null) {
                return Optional.empty();
            }

            final InetAddress inetAddress = InetAddressUtils.addr(ipAddr);

            if (inetAddress.isAnyLocalAddress()
                    || inetAddress.isLoopbackAddress()
                    || inetAddress.isLinkLocalAddress()
                    || inetAddress.isMulticastAddress()) {
                return Optional.empty();
            }

            IpInterfaceResult.Builder ipInterFaceBuilder = IpInterfaceResult.newBuilder();
            ipInterFaceBuilder.setIpAddress(inetAddress.getHostAddress());
            ipInterFaceBuilder.setNetmask(netMask.getHostAddress());
            ipInterFaceBuilder.setIpHostName(inetAddress.getHostName());
            if (ifIndex != null) {
                ipInterFaceBuilder.setIfIndex(ifIndex);
            }
            return Optional.of(ipInterFaceBuilder.build());
        }
    }

    /**
     * <p>Constructor for IPInterfaceTableTracker.</p>
     */
    public IPAddrTracker() {
        super(s_tableColumns);
    }

    /**
     * <p>Constructor for IPInterfaceTableTracker.</p>
     *
     *
     */
    public IPAddrTracker(RowCallback rowProcessor) {
        super(rowProcessor, s_tableColumns);
    }

    /** {@inheritDoc} */
    @Override
    public SnmpRowResult createRowResult(int columnCount, SnmpInstId instance) {
        return new IPInterfaceRow(columnCount, instance);
    }

    /** {@inheritDoc} */
    @Override
    public void rowCompleted(SnmpRowResult row) {
        processIPInterfaceRow((IPInterfaceRow) row);
    }

    /**
     * <p>processIPInterfaceRow</p>
     *
     *
     */
    public void processIPInterfaceRow(IPInterfaceRow row) {}
}
