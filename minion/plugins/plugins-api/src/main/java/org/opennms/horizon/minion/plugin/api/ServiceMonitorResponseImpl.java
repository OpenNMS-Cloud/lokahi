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
import lombok.Builder;
import lombok.Data;
import org.opennms.taskset.contract.MonitorType;

@Builder
@Data
public class ServiceMonitorResponseImpl implements ServiceMonitorResponse {
    private Status status;
    private String reason;
    private String ipAddress;
    private double responseTime;
    private Map<String, Number> additionalMetrics;
    private MonitorType monitorType;
    private long nodeId;
    private long monitoredServiceId;

    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    public static ServiceMonitorResponse unknown() {
        return builder().status(Status.Unknown).build();
    }

    public static ServiceMonitorResponse down() {
        return builder().status(Status.Down).build();
    }

    public static ServiceMonitorResponse up() {
        return builder().status(Status.Up).build();
    }

    public static ServiceMonitorResponse unresponsive() {
        return builder().status(Status.Unresponsive).build();
    }
}
