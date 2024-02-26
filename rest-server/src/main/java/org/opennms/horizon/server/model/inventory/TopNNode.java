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
package org.opennms.horizon.server.model.inventory;

import java.util.Comparator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopNNode {
    private String nodeLabel;
    private String nodeAlias;
    private String location;
    private double avgResponseTime;
    private double reachability;

    public static Comparator<TopNNode> getComparator(String fieldName, boolean sortByAscending) {
        Comparator<TopNNode> comparator =
                switch (fieldName) {
                    case "nodeLabel" -> Comparator.comparing(TopNNode::getNodeLabel);
                    case "nodeAlias" -> Comparator.comparing(TopNNode::getNodeAlias);
                    case "location" -> Comparator.comparing(TopNNode::getLocation);
                    case "avgResponseTime" -> Comparator.comparingDouble(TopNNode::getAvgResponseTime);
                    default -> Comparator.comparingDouble(TopNNode::getReachability);
                };
        // Apply ascending or descending sorting based on the flag
        return sortByAscending ? comparator : comparator.reversed();
    }
}
