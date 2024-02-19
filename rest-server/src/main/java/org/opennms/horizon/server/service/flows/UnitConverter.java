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
package org.opennms.horizon.server.service.flows;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opennms.horizon.server.model.flows.FlowingPoint;

public final class UnitConverter {
    private UnitConverter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * It will split data by label & direction and convert byte > bps, beware it will directly modify value of flowingPoint
     *
     * @param input
     */
    public static void convert(List<FlowingPoint> input) {
        input.stream()
                .collect(Collectors.groupingBy(p -> p.getLabel() + p.getDirection()))
                .values()
                .forEach(UnitConverter::convertToRate);
    }

    /**
     * convert a grouped points value from byte > bps, beware it will directly modify value of flowingPoint
     */
    public static void convertToRate(final List<FlowingPoint> points) {
        if (points == null || points.size() < 2) {
            return;
        }
        var sorted = points.stream()
                .sorted(Comparator.comparing(FlowingPoint::getTimestamp))
                .toList();
        long lastTimestampMs = -1;
        for (final var point : sorted) {
            if (lastTimestampMs == -1) {
                // assume the first step size are the same as second one
                double firstStepSize = (double) sorted.get(1).getTimestamp().toEpochMilli()
                        - (double) sorted.get(0).getTimestamp().toEpochMilli();
                point.setValue(point.getValue() / firstStepSize * 1000d * 8);
            } else {
                point.setValue(point.getValue() / (point.getTimestamp().toEpochMilli() - lastTimestampMs) * 1000d * 8);
            }
            lastTimestampMs = point.getTimestamp().toEpochMilli();
        }
    }
}
