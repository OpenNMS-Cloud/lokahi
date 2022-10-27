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

package org.opennms.horizon.shared.snmp.traps;

import org.opennms.horizon.shared.snmp.SnmpV3User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TrapdConfigBean implements TrapdConfig, Serializable {

    private static final long serialVersionUID = 2L;

    private String snmpTrapAddress;
    private int snmpTrapPort;
    private boolean newSuspectOnTrap;
    private List<SnmpV3User> snmpV3Users= new ArrayList<>();
    private boolean includeRawMessage;
    private int batchIntervalInMs;
    private int batchSize;
    private int queueSize;
    private int numThreads;
    private boolean useAddressFromVarbind;

    public TrapdConfigBean() {

    }

    public TrapdConfigBean(TrapdConfig configToClone) {
        update(configToClone);
    }


    public void setSnmpTrapAddress(String snmpTrapAddress) {
        this.snmpTrapAddress = snmpTrapAddress;
    }

    public void setSnmpTrapPort(int snmpTrapPort) {
        this.snmpTrapPort = snmpTrapPort;
    }

    public void setNewSuspectOnTrap(boolean newSuspectOnTrap) {
        this.newSuspectOnTrap = newSuspectOnTrap;
    }

    @Override
    public String getSnmpTrapAddress() {
        return snmpTrapAddress;
    }

    @Override
    public int getSnmpTrapPort() {
        return snmpTrapPort;
    }

    public void setSnmpV3Users(List<SnmpV3User> snmpV3Users) {
        this.snmpV3Users = new ArrayList<>(Objects.requireNonNull(snmpV3Users));
    }

    @Override
    public boolean getNewSuspectOnTrap() {
        return newSuspectOnTrap;
    }

    @Override
    public List<SnmpV3User> getSnmpV3Users() {
        return Collections.unmodifiableList(snmpV3Users);
    }

    @Override
    public boolean isIncludeRawMessage() {
        return includeRawMessage;
    }

    public void setIncludeRawMessage(boolean includeRawMessage) {
        this.includeRawMessage = includeRawMessage;
    }

    @Override
    public int getNumThreads() {
        if (numThreads <= 0) {
            return Runtime.getRuntime().availableProcessors() * 2;
        }
        return numThreads;
    }

    @Override
    public int getQueueSize() {
        return queueSize;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public int getBatchIntervalMs() {
        return batchIntervalInMs;
    }

    @Override
    public void update(TrapdConfig config) {
        setSnmpTrapAddress(config.getSnmpTrapAddress());
        setSnmpTrapPort(config.getSnmpTrapPort());
        setNewSuspectOnTrap(config.getNewSuspectOnTrap());
        setIncludeRawMessage(config.isIncludeRawMessage());
        setBatchIntervalMs(config.getBatchIntervalMs());
        setBatchSize(config.getBatchSize());
        setQueueSize(config.getQueueSize());
        setNumThreads(config.getNumThreads());
        setSnmpV3Users(config.getSnmpV3Users());
    }

    public void setBatchIntervalMs(int batchIntervalInMs) {
        this.batchIntervalInMs = batchIntervalInMs;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }


    @Override
    public boolean shouldUseAddressFromVarbind() {
        return this.useAddressFromVarbind;
    }

    public void setUseAddressFromVarbind(boolean useAddressFromVarbind) {
        this.useAddressFromVarbind = useAddressFromVarbind;
    }
}

