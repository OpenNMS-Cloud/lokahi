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

package org.opennms.horizon.traps.config;

import org.opennms.horizon.shared.snmp.SnmpV3User;

import java.util.List;

public class SnmpTrapsConfig implements TrapdConfig {

    private TrapdConfig trapdConfig;

    @Override
    public String getSnmpTrapAddress() {
        return trapdConfig.getSnmpTrapAddress();
    }

    @Override
    public int getSnmpTrapPort() {
        return trapdConfig.getSnmpTrapPort();
    }

    @Override
    public boolean getNewSuspectOnTrap() {
        return trapdConfig.getNewSuspectOnTrap();
    }

    @Override
    public List<SnmpV3User> getSnmpV3Users() {
        return trapdConfig.getSnmpV3Users();
    }

    @Override
    public boolean isIncludeRawMessage() {
        return trapdConfig.isIncludeRawMessage();
    }

    @Override
    public int getNumThreads() {
        return trapdConfig.getNumThreads();
    }

    @Override
    public int getQueueSize() {
        return trapdConfig.getQueueSize();
    }

    @Override
    public int getBatchSize() {
        return trapdConfig.getBatchSize();
    }

    @Override
    public int getBatchIntervalMs() {
        return trapdConfig.getBatchIntervalMs();
    }

    @Override
    public void update(TrapdConfig config) {

    }

    @Override
    public boolean shouldUseAddressFromVarbind() {
        return trapdConfig.shouldUseAddressFromVarbind();
    }

    public TrapdConfig getTrapdConfig() {
        return trapdConfig;
    }

    public void setTrapdConfig(TrapdConfig trapdConfig) {
        this.trapdConfig = trapdConfig;
    }
}
