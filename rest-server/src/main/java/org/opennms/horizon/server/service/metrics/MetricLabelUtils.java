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
package org.opennms.horizon.server.service.metrics;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MetricLabelUtils {
    public static final String METRIC_NAME_KEY = "__name__";
    public static final String MONITOR_KEY = "monitor";
    private static final String NODE_ID_KEY = "node_id";

    public Optional<Long> getNodeId(Map<String, String> metricLabels) {
        if (metricLabels.containsKey(NODE_ID_KEY)) {
            String nodeIdStr = metricLabels.get(NODE_ID_KEY);
            try {
                return Optional.of(Long.parseLong(nodeIdStr));
            } catch (NumberFormatException e) {
                throw new RuntimeException("Node ID is not a number");
            }
        }
        return Optional.empty();
    }
}
