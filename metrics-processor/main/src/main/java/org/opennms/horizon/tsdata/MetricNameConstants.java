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
package org.opennms.horizon.tsdata;

public interface MetricNameConstants {
    String METRICS_NAME_PREFIX_MONITOR = "monitor_";
    String METRICS_NAME_RESPONSE = "response_time_msec";
    String METRIC_NAME_LABEL = "__name__";

    String METRIC_INSTANCE_LABEL = "instance";
    // refer to AzureHttpClient.ResourcesType
    String METRIC_AZURE_NODE_TYPE = "node";
    String METRIC_AZURE_PUBLIC_IP_TYPE = "publicIPAddresses";
    String METRIC_AZURE_NETWORK_INTERFACE_TYPE = "networkInterfaces";

    String[] MONITOR_METRICS_LABEL_NAMES = {METRIC_INSTANCE_LABEL, "location_id", "system_id", "monitor", "node_id"};
}
