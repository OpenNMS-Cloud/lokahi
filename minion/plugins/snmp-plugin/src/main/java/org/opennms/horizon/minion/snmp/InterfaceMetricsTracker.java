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

import static org.opennms.horizon.minion.snmp.SnmpCollectionSet.addResult;

import org.opennms.horizon.shared.snmp.Mib2InterfacesTracker;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceMetricsTracker extends Mib2InterfacesTracker {

    private final Logger LOG = LoggerFactory.getLogger(InterfaceMetricsTracker.class);
    private final Integer ifIndex;
    private final String ifName;
    private final SnmpResponseMetric.Builder builder;
    private final String ipAddress;

    public InterfaceMetricsTracker(
            Integer ifIndex, String ifName, String ipAddress, SnmpResponseMetric.Builder builder) {
        super();
        this.ifIndex = ifIndex;
        this.ifName = ifName;
        this.builder = builder;
        this.ipAddress = ipAddress;
    }

    @Override
    protected void storeResult(org.opennms.horizon.shared.snmp.SnmpResult res) {
        var aliasOptional = getAlias(res);
        try {
            if (res.getInstance() != null && ifIndex == res.getInstance().toInt()) {
                aliasOptional.ifPresent((alias) -> addResult(res, builder, alias, ifName, ipAddress));
            }
        } catch (Exception e) {
            LOG.warn("Exception while converting result from SnmpValue to proto", e);
        }
    }
}
