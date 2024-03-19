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
package org.opennms.horizon.minion.plugin.api;

import java.util.Map;
import org.opennms.taskset.contract.MonitorType;

public interface ServiceMonitorResponse {
    /**
     *
     * @return type of monitor that produced the response.
     */
    MonitorType getMonitorType();

    /**
     *
     * @return status whether service is Unknown/Up/Down/Unresponsive
     */
    Status getStatus();

    /**
     *
     * @return reason behind the current poll status when the service is not Up
     */
    String getReason();

    /**
     *
     * @return IP address that was monitored
     */
    String getIpAddress();

    long getNodeId();

    long getMonitoredServiceId();

    /**
     *  TODO: standardize the unit (ms or sec?)
     *
     * @return amount of time device took to respond to the monitor request
     */
    double getResponseTime();

    Map<String, Number> getAdditionalMetrics();

    /**
     * TECHDEBT: Was originally added to the monitor interface to take advantage of poller's scheduling and
     * configuration mechanism.
     */
    DeviceConfig getDeviceConfig();

    /**
     * Returns timestamp when response time was actually generated.
     *
     * @return Timestamp of a response.
     */
    long getTimestamp();

    interface DeviceConfig {

        byte[] getContent();

        String getFilename();
    }

    enum Status {
        /**
         * Was unable to determine the status.
         */
        Unknown,

        /**
         * Was in a normal state.
         */
        Up,

        /**
         * Not working normally.
         */
        Down,

        /**
         * Service that is up but is most likely suffering due to excessive load or latency
         * issues and because of that has not responded within the configured timeout period.
         */
        Unresponsive;
    }
}
