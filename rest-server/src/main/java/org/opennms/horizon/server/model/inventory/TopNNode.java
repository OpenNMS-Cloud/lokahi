/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.server.model.inventory;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;

@Getter
@Setter
public class TopNNode {
    private String nodeLabel;
    private String nodeAlias;
    private String location;
    private double avgResponseTime;
    private double reachability;

    public static Comparator<TopNNode> getComparator(String fieldName, boolean sortByAscending) {
        Comparator<TopNNode> comparator = switch (fieldName) {
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
