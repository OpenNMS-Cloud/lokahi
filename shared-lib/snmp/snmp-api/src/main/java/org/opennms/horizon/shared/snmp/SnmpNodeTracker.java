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

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpNodeTracker extends AggregateTracker {

    private static final Logger LOG = LoggerFactory.getLogger(SnmpNodeTracker.class);

    private static final NamedSnmpVar[] elemList = new NamedSnmpVar[44];
    private final Map<String, SnmpResult> snmpResultMap = new TreeMap<>();

    public SnmpNodeTracker() {
        super(NamedSnmpVar.getTrackersFor(elemList));
    }

    static {
        int ndx = 0;
        // TCP
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpActiveOpens", ".1.3.6.1.2.1.6.5");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpPassiveOpens", ".1.3.6.1.2.1.6.6");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpAttemptFails", ".1.3.6.1.2.1.6.7");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpEstabResets", ".1.3.6.1.2.1.6.8");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPGAUGE32, "tcpCurrEstab", ".1.3.6.1.2.1.6.9");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpInSegs", ".1.3.6.1.2.1.6.10");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpOutSegs", ".1.3.6.1.2.1.6.11");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpRetransSegs", ".1.3.6.1.2.1.6.12");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpInErrors", ".1.3.6.1.2.1.6.14");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "tcpOutRsts", ".1.3.6.1.2.1.6.15");

        // ICMP
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInMsgs", ".1.3.6.1.2.1.5.1");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInErrors", ".1.3.6.1.2.1.5.2");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInDestUnreachs", ".1.3.6.1.2.1.5.3");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInTimeExcds", ".1.3.6.1.2.1.5.4");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInParmProbs", ".1.3.6.1.2.1.5.5");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInSrcQuenchs", ".1.3.6.1.2.1.5.6");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInRedirects", ".1.3.6.1.2.1.5.7");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInEchos", ".1.3.6.1.2.1.5.8");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInEchoReps", ".1.3.6.1.2.1.5.9");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInTimestamps", ".1.3.6.1.2.1.5.10");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInTimestampReps", ".1.3.6.1.2.1.5.11");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInAddrMasks", ".1.3.6.1.2.1.5.12");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpInAddrMaskReps", ".1.3.6.1.2.1.5.13");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutMsgs", ".1.3.6.1.2.1.5.14");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutErrors", ".1.3.6.1.2.1.5.15");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutDestUnreachs", ".1.3.6.1.2.1.5.16");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutTimeExcds", ".1.3.6.1.2.1.5.17");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutParmProbs", ".1.3.6.1.2.1.5.18");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutSrcQuenchs", ".1.3.6.1.2.1.5.19");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutRedirects", ".1.3.6.1.2.1.5.20");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutEchos", ".1.3.6.1.2.1.5.21");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutEchoReps", ".1.3.6.1.2.1.5.22");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutTimestamps", ".1.3.6.1.2.1.5.23");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutTimestmpReps", ".1.3.6.1.2.1.5.24");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutAddrMasks", ".1.3.6.1.2.1.5.25");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "icmpOutAddrMaskReps", ".1.3.6.1.2.1.5.26");

        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "hrSystemUptime", ".1.3.6.1.2.1.25.1.1");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "hrSystemNumUsers", ".1.3.6.1.2.1.25.1.5");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "hrSystemProcesses", ".1.3.6.1.2.1.25.1.6");

        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "hrMemorySize", ".1.3.6.1.2.1.25.2.2");

        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "udpInDatagrams", ".1.3.6.1.2.1.7.1");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "udpNoPorts", ".1.3.6.1.2.1.7.2");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "udpInErrors", ".1.3.6.1.2.1.7.3");
        elemList[ndx++] = new NamedSnmpVar(NamedSnmpVar.SNMPCOUNTER32, "udpOutDatagrams", ".1.3.6.1.2.1.7.4");
    }

    @Override
    protected void storeResult(SnmpResult res) {

        final SnmpObjId base = res.getBase();
        final SnmpValue value = res.getValue();

        for (final NamedSnmpVar snmpVar : elemList) {
            if (base.equals(snmpVar.getSnmpObjId())) {
                if (value.isError()) {
                    LOG.error(
                            "storeResult: got an error for alias {} [{}].[{}],"
                                    + " but we should only be getting non-errors: {}",
                            snmpVar.getAlias(),
                            base,
                            res.getInstance(),
                            value);
                } else if (value.isEndOfMib()) {
                    LOG.debug(
                            "storeResult: got endOfMib for alias {} [{}].[{}], not storing",
                            snmpVar.getAlias(),
                            base,
                            res.getInstance());
                } else {
                    snmpResultMap.put(snmpVar.getAlias(), res);
                }
            }
        }
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

    Map<String, SnmpResult> getSnmpResultMap() {
        return snmpResultMap;
    }
}
