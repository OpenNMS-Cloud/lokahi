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

import org.opennms.horizon.shared.snmp.Mib2InterfacesTracker;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opennms.horizon.minion.snmp.SnmpCollectionSet.addResult;

public class InterfaceMetricsTracker extends Mib2InterfacesTracker {

    private final Logger LOG = LoggerFactory.getLogger(InterfaceMetricsTracker.class);
    private final Integer ifIndex;
    private final String ifName;
    private final SnmpResponseMetric.Builder builder;
    private final String ipAddress;

    public InterfaceMetricsTracker(Integer ifIndex, String ifName, String ipAddress, SnmpResponseMetric.Builder builder) {
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
