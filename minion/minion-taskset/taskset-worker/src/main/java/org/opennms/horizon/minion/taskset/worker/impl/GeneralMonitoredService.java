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
package org.opennms.horizon.minion.taskset.worker.impl;

import java.net.InetAddress;
import org.opennms.horizon.minion.plugin.api.MonitoredService;

// TODO: why an interface - this seems like a straight-forward model?
public class GeneralMonitoredService implements MonitoredService {

    private final String svcName;
    private final String ipAddr;
    private final long nodeId;
    private final String nodeLabel;
    private final String nodeLocation;
    private final InetAddress address;
    private final long monitorServiceId;

    public GeneralMonitoredService(
            String svcName,
            String ipAddr,
            long nodeId,
            String nodeLabel,
            String nodeLocation,
            InetAddress address,
            long monitorServiceId) {
        this.svcName = svcName;
        this.ipAddr = ipAddr;
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.nodeLocation = nodeLocation;
        this.address = address;
        this.monitorServiceId = monitorServiceId;
    }

    @Override
    public String getSvcName() {
        return svcName;
    }

    @Override
    public String getIpAddr() {
        return ipAddr;
    }

    @Override
    public long getNodeId() {
        return nodeId;
    }

    @Override
    public String getNodeLabel() {
        return nodeLabel;
    }

    @Override
    public String getNodeLocation() {
        return nodeLocation;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public long getMonitorServiceId() {
        return monitorServiceId;
    }
}
