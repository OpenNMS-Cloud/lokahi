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
import java.util.Arrays;
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

public class IPAddressTableTracker extends TableTracker {
    private static final Logger LOG = LoggerFactory.getLogger(IPAddressTableTracker.class);

    public static final SnmpObjId IP_ADDRESS_PREFIX_TABLE_ENTRY = SnmpObjId.get(".1.3.6.1.2.1.4.32.1");
    public static final SnmpObjId IP_ADDRESS_TABLE_ENTRY = SnmpObjId.get(".1.3.6.1.2.1.4.34.1");

    public static final SnmpObjId IP_ADDRESS_IF_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "3");
    public static final SnmpObjId IP_ADDRESS_TYPE_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "4");
    public static final SnmpObjId IP_ADDRESS_PREFIX_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "5");
    public static final SnmpObjId IP_ADDRESS_PREFIX_ORIGIN_INDEX = SnmpObjId.get(IP_ADDRESS_PREFIX_TABLE_ENTRY, "5");
    public static final SnmpObjId IP_ADDRESS_ORIGIN_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "6");
    public static final SnmpObjId IP_ADDRESS_STATUS_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "7");
    public static final SnmpObjId IP_ADDRESS_CREATED_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "8");
    public static final SnmpObjId IP_ADDRESS_LAST_CHANGED_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "9");
    public static final SnmpObjId IP_ADDRESS_ROW_STATUS_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "10");
    public static final SnmpObjId IP_ADDRESS_STORAGE_TYPE_INDEX = SnmpObjId.get(IP_ADDRESS_TABLE_ENTRY, "11");
    public static final int TYPE_IPV4 = 1;
    public static final int TYPE_IPV6 = 2;
    public static final int TYPE_IPV4Z = 3;
    public static final int TYPE_IPV6Z = 4;
    public static final int TYPE_DNS = 16;

    private static final int IP_ADDRESS_TYPE_UNICAST = 1;
    // private static final int IP_ADDRESS_TYPE_ANYCAST = 2;
    // private static final int IP_ADDRESS_TYPE_BROADCAST = 3;

    private static SnmpObjId[] s_tableColumns =
            new SnmpObjId[] {IP_ADDRESS_IF_INDEX, IP_ADDRESS_PREFIX_INDEX, IP_ADDRESS_TYPE_INDEX};

    public static class IPAddressRow extends SnmpRowResult {

        public IPAddressRow(final int columnCount, final SnmpInstId instance) {
            super(columnCount, instance);
            LOG.debug("column count = {}, instance = {}", columnCount, instance);
        }

        public Integer getIfIndex() {
            final SnmpValue value = getValue(IP_ADDRESS_IF_INDEX);
            return value.toInt();
        }

        public String getIpAddress() {
            final var result = getResult(IP_ADDRESS_IF_INDEX);
            if (result.isEmpty()) {
                LOG.warn("BAD AGENT: Device is missing IP-MIB::ipAddressIfIndex. Skipping.");
                return null;
            }

            SnmpInstId instance = result.get().getInstance();
            final int[] instanceIds = instance.getIds();

            final int addressType = instanceIds[0];
            int addressIndex = 2;
            int addressLength = instanceIds[1];
            // Begin NMS-4906 Lame Force 10 agent!
            if (addressType == TYPE_IPV4 && instanceIds.length != 6) {
                LOG.warn(
                        "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = instanceIds.length - 4;
                addressLength = 4;
            }
            if (addressType == TYPE_IPV6 && instanceIds.length != 18) {
                LOG.warn(
                        "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = instanceIds.length - 16;
                addressLength = 16;
            }
            // End NMS-4906 Lame Force 10 agent!

            // we ignore zones anyways, make sure we truncate to just the address part, since InetAddress doesn't know
            // how to parse zone bytes
            if (addressType == TYPE_IPV4Z) {
                addressLength = 4;
            } else if (addressType == TYPE_IPV6Z) {
                addressLength = 16;
            }

            if (addressIndex < 0 || addressIndex + addressLength > instanceIds.length) {
                LOG.warn(
                        "BAD AGENT: Returned instanceId {} does not enough bytes to contain address!. Skipping.",
                        instance);
                return null;
            }

            if (addressType == TYPE_IPV4 || addressType == TYPE_IPV6 || addressType == TYPE_IPV6Z) {
                try {
                    final InetAddress address =
                            InetAddressUtils.getInetAddress(instanceIds, addressIndex, addressLength);
                    return InetAddressUtils.str(address);
                } catch (final IllegalArgumentException e) {
                    LOG.warn(
                            "Failed to parse address: {}, index {}, length {}",
                            Arrays.toString(instanceIds),
                            addressIndex,
                            addressLength,
                            e);
                }
            }
            return null;
        }

        public Integer getType() {
            final SnmpValue value = getValue(IP_ADDRESS_TYPE_INDEX);
            return value.toInt();
        }

        private InetAddress getNetMask() {
            final SnmpValue value = getValue(IP_ADDRESS_PREFIX_INDEX);

            final SnmpInstId netmaskRef = value.toSnmpObjId().getInstance(IP_ADDRESS_PREFIX_ORIGIN_INDEX);

            // See bug NMS-5036
            // {@see http://issues.opennms.org/browse/NMS-5036}
            if (netmaskRef == null) {
                LOG.warn("BAD AGENT: Null netmask instanceId");
                return null;
            }

            final int[] rawIds = netmaskRef.getIds();
            final int addressType = rawIds[1];
            final int mask = rawIds[rawIds.length - 1];
            int addressLength = rawIds[2];
            int addressIndex = 3;

            // Begin NMS-4906 Lame Force 10 agent!
            if (addressType == TYPE_IPV4 && rawIds.length != 1 + 6 + 1) {
                LOG.warn(
                        "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = rawIds.length - (4 + 1);
                addressLength = 4;
            }
            if (addressType == TYPE_IPV6 && rawIds.length != 1 + 18 + 1) {
                LOG.warn(
                        "BAD AGENT: Does not conform to RFC 4001 Section 4.1 Table Indexing!!! Report them immediately.  Making a best guess!");
                addressIndex = rawIds.length - (16 + 1);
                addressLength = 16;
            }
            // End NMS-4906 Lame Force 10 agent!

            if (addressIndex < 0 || addressIndex + addressLength > rawIds.length) {
                LOG.warn(
                        "BAD AGENT: Returned instanceId {} does not enough bytes to contain address!. Skipping.",
                        netmaskRef);
                return null;
            }

            // final InetAddress address = getInetAddress(rawIds, addressIndex, addressLength);

            if (addressType == TYPE_IPV4) {
                return InetAddressUtils.convertCidrToInetAddressV4(mask);
            } else if (addressType == TYPE_IPV6) {
                return InetAddressUtils.convertCidrToInetAddressV6(mask);
            } else {
                LOG.warn("unknown address type, expected 1 (IPv4) or 2 (IPv6), but got {}", addressType);
                return null;
            }
        }

        public Optional<IpInterfaceResult> createInterfaceFromRow() {

            final Integer ifIndex = getIfIndex();
            final String ipAddr = getIpAddress();
            final Integer type = getType();
            final InetAddress netMask = getNetMask();

            LOG.debug(
                    "createInterfaceFromRow: ifIndex = {}, ipAddress = {}, type = {}, netmask = {}",
                    ifIndex,
                    ipAddr,
                    type,
                    netMask);

            if (type != IP_ADDRESS_TYPE_UNICAST || ipAddr == null) {
                return Optional.empty();
            }

            final InetAddress inetAddress = InetAddressUtils.addr(ipAddr);

            if (inetAddress.isAnyLocalAddress()
                    || inetAddress.isLoopbackAddress()
                    || inetAddress.isLinkLocalAddress()
                    || inetAddress.isMulticastAddress()) {
                return Optional.empty();
            }

            IpInterfaceResult.Builder ipInterfaceBuilder = IpInterfaceResult.newBuilder();
            ipInterfaceBuilder.setIpAddress(inetAddress.getHostAddress());
            ipInterfaceBuilder.setNetmask(netMask.getHostAddress());
            ipInterfaceBuilder.setIpHostName(inetAddress.getHostName());
            if (ifIndex != null) {
                ipInterfaceBuilder.setIfIndex(ifIndex);
            }
            return Optional.of(ipInterfaceBuilder.build());
        }
    }

    /**
     * <p>Constructor for IPInterfaceTableTracker.</p>
     */
    public IPAddressTableTracker() {
        super(s_tableColumns);
    }

    public IPAddressTableTracker(final RowCallback rowProcessor) {
        super(rowProcessor, s_tableColumns);
    }

    /** {@inheritDoc} */
    @Override
    public SnmpRowResult createRowResult(final int columnCount, final SnmpInstId instance) {
        return new IPAddressRow(columnCount, instance);
    }

    /** {@inheritDoc} */
    @Override
    public void rowCompleted(final SnmpRowResult row) {
        processIPAddressRow((IPAddressRow) row);
    }

    public void processIPAddressRow(final IPAddressRow row) {}
}
