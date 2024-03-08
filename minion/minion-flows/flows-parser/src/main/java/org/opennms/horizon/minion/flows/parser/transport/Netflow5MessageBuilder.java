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
package org.opennms.horizon.minion.flows.parser.transport;

import static org.opennms.horizon.minion.flows.parser.transport.MessageUtils.getLongValue;
import static org.opennms.horizon.minion.flows.parser.transport.MessageUtils.getUInt32Value;
import static org.opennms.horizon.minion.flows.parser.transport.MessageUtils.getUInt64Value;

import java.net.InetAddress;
import lombok.NoArgsConstructor;
import org.opennms.horizon.flows.document.Direction;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.NetflowVersion;
import org.opennms.horizon.flows.document.SamplingAlgorithm;
import org.opennms.horizon.minion.flows.parser.RecordEnrichment;
import org.opennms.horizon.minion.flows.parser.ie.Value;

@NoArgsConstructor
public class Netflow5MessageBuilder implements MessageBuilder {

    @Override
    public FlowDocument.Builder buildMessage(final Iterable<Value<?>> values, final RecordEnrichment enrichment) {
        final var builder = FlowDocument.newBuilder();

        Long unixSecs = null;
        Long unixNSecs = null;
        Long sysUpTime = null;
        Long first = null;
        Long last = null;
        InetAddress srcAddr = null;
        InetAddress dstAddr = null;
        InetAddress nextHop = null;

        for (Value<?> value : values) {
            switch (value.getName()) {
                case "@count":
                    getUInt32Value(value).ifPresent(builder::setNumFlowRecords);
                    break;
                case "@unixSecs":
                    unixSecs = getLongValue(value);
                    break;
                case "@unixNSecs":
                    unixNSecs = getLongValue(value);
                    break;
                case "@sysUptime":
                    sysUpTime = getLongValue(value);
                    break;
                case "@flowSequence":
                    getUInt64Value(value).ifPresent(builder::setFlowSeqNum);
                    break;
                case "@engineType":
                    getUInt32Value(value).ifPresent(builder::setEngineType);
                    break;
                case "@engineId":
                    getUInt32Value(value).ifPresent(builder::setEngineId);
                    break;
                case "@samplingAlgorithm":
                    Long saValue = getLongValue(value);
                    SamplingAlgorithm samplingAlgorithm = SamplingAlgorithm.UNDEFINED_SAMPLING_ALGORITHM;
                    if (saValue != null) {
                        switch (saValue.intValue()) {
                            case 1:
                                samplingAlgorithm = SamplingAlgorithm.SYSTEMATIC_COUNT_BASED_SAMPLING;
                                break;
                            case 2:
                                samplingAlgorithm = SamplingAlgorithm.RANDOM_N_OUT_OF_N_SAMPLING;
                                break;
                        }
                    }
                    builder.setSamplingAlgorithm(samplingAlgorithm);
                    break;
                case "@samplingInterval":
                    MessageUtils.getDoubleValue(value).ifPresent(builder::setSamplingInterval);
                    break;

                case "srcAddr":
                    srcAddr = MessageUtils.getInetAddress(value);
                    break;
                case "dstAddr":
                    dstAddr = MessageUtils.getInetAddress(value);
                    break;
                case "nextHop":
                    nextHop = MessageUtils.getInetAddress(value);
                    break;
                case "input":
                    getUInt32Value(value).ifPresent(builder::setInputSnmpIfindex);
                    break;
                case "output":
                    getUInt32Value(value).ifPresent(builder::setOutputSnmpIfindex);
                    break;
                case "dPkts":
                    getUInt64Value(value).ifPresent(builder::setNumPackets);
                    break;
                case "dOctets":
                    getUInt64Value(value).ifPresent(builder::setNumBytes);
                    break;
                case "first":
                    first = getLongValue(value);
                    break;
                case "last":
                    last = getLongValue(value);
                    break;
                case "srcPort":
                    getUInt32Value(value).ifPresent(builder::setSrcPort);
                    break;
                case "dstPort":
                    getUInt32Value(value).ifPresent(builder::setDstPort);
                    break;
                case "tcpFlags":
                    getUInt32Value(value).ifPresent(builder::setTcpFlags);
                    break;
                case "proto":
                    getUInt32Value(value).ifPresent(builder::setProtocol);
                    break;
                case "srcAs":
                    getUInt64Value(value).ifPresent(builder::setSrcAs);
                    break;
                case "dstAs":
                    getUInt64Value(value).ifPresent(builder::setDstAs);
                    break;
                case "tos":
                    getUInt32Value(value).ifPresent(builder::setTos);
                    break;
                case "srcMask":
                    getUInt32Value(value).ifPresent(builder::setSrcMaskLen);
                    break;
                case "dstMask":
                    getUInt32Value(value).ifPresent(builder::setDstMaskLen);
                    break;
                case "egress":
                    Boolean egress = MessageUtils.getBooleanValue(value);
                    Direction direction = egress ? Direction.EGRESS : Direction.INGRESS;
                    builder.setDirection(direction);
                    break;
            }
        }

        long timeStamp = unixSecs * 1000L + unixNSecs / 1000_000L;
        long bootTime = timeStamp - sysUpTime;

        builder.setNetflowVersion(NetflowVersion.V5);
        builder.setFirstSwitched(MessageUtils.setLongValue(bootTime + first));
        builder.setLastSwitched(MessageUtils.setLongValue(bootTime + last));
        builder.setTimestamp(timeStamp);

        if (srcAddr != null) {
            builder.setSrcAddress(srcAddr.getHostAddress());
            enrichment.getHostnameFor(srcAddr).ifPresent(builder::setSrcHostname);
        }
        if (dstAddr != null) {
            builder.setDstAddress(dstAddr.getHostAddress());
            enrichment.getHostnameFor(dstAddr).ifPresent(builder::setDstHostname);
        }
        if (nextHop != null) {
            builder.setNextHopAddress(nextHop.getHostAddress());
            enrichment.getHostnameFor(nextHop).ifPresent(builder::setNextHopHostname);
        }

        return builder;
    }
}
