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
package org.opennms.horizon.shared.snmp.traps;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.opennms.horizon.shared.snmp.SnmpV3User;

public class TrapdConfigBean implements TrapdConfig, Serializable {

    private static final long serialVersionUID = 2L;

    private String snmpTrapAddress;
    private int snmpTrapPort;
    private boolean newSuspectOnTrap;
    private List<SnmpV3User> snmpV3Users = new ArrayList<>();
    private boolean includeRawMessage;
    private int batchIntervalInMs;
    private int batchSize;
    private int queueSize;
    private int numThreads;
    private boolean useAddressFromVarbind;

    public TrapdConfigBean() {}

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
