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

package org.opennms.horizon.server.service.flows;

import org.opennms.horizon.server.model.flows.FlowingPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
            .values().forEach(UnitConverter::convertToRate);
    }

    /**
     * convert a grouped points value from byte > bps, beware it will directly modify value of flowingPoint
     */
    public static void convertToRate(final List<FlowingPoint> points) {
        if (points == null || points.size() < 2) {
            return;
        }
        var sorted = points.stream().sorted(Comparator.comparing(FlowingPoint::getTimestamp)).toList();
        long lastTimestampMs = -1;
        for (final var point : sorted) {
            if (lastTimestampMs == -1) {
                // assume the first step size are the same as second one
                double firstStepSize = (double) sorted.get(1).getTimestamp().toEpochMilli() - (double) sorted.get(0).getTimestamp().toEpochMilli();
                point.setValue(point.getValue() / firstStepSize * 1000d * 8);
            } else {
                point.setValue(point.getValue() / (point.getTimestamp().toEpochMilli() - lastTimestampMs) * 1000d * 8);
            }
            lastTimestampMs = point.getTimestamp().toEpochMilli();
        }
    }
}
