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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.model.flows.FlowingPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class UnitConverterTest {
    private static final String INGRESS = "INGRESS";
    private static final String EGRESS = "EGRESS";
    private static final String LABEL = "LABEL";

    @Test
    void testSingleConvert() {
        List<FlowingPoint> pointList = new ArrayList<>();

        long startTime = 1694473114833L;
        pointList.add(getFlowingPoint(startTime, 100, LABEL, INGRESS));
        pointList.add(getFlowingPoint(startTime + 100, 200, LABEL, INGRESS));
        pointList.add(getFlowingPoint(startTime + 200, 300, LABEL, INGRESS));
        pointList.add(getFlowingPoint(startTime + 300, 400, LABEL, INGRESS));
        UnitConverter.convertToRate(pointList);

        Assertions.assertEquals(4, pointList.size());
        Assertions.assertEquals(8000, pointList.get(0).getValue());
        Assertions.assertEquals(16_000, pointList.get(1).getValue());
        Assertions.assertEquals(24_000, pointList.get(2).getValue());
        Assertions.assertEquals(32_000, pointList.get(3).getValue());
        for (int i = 0; i < pointList.size(); i++) {
            Assertions.assertEquals(startTime + 100L * i, pointList.get(i).getTimestamp().toEpochMilli());
            Assertions.assertEquals(INGRESS, pointList.get(i).getDirection());
        }
    }


    @Test
    void testInvalidList() {
        List<FlowingPoint> pointList = new ArrayList<>();
        UnitConverter.convertToRate(pointList);
        Assertions.assertEquals(0, pointList.size());

        // single data
        pointList.add(getFlowingPoint(System.currentTimeMillis(), 100, LABEL, INGRESS));
        UnitConverter.convertToRate(pointList);
        Assertions.assertEquals(1, pointList.size());

        pointList = null;
        UnitConverter.convertToRate(pointList);
        // null
        Assertions.assertNull(pointList);
    }

    @Test
    void testConvert() {
        List<FlowingPoint> pointList = new ArrayList<>();

        long startTime = 1694473114833L;
        pointList.add(getFlowingPoint(startTime, 100, LABEL, INGRESS));
        pointList.add(getFlowingPoint(startTime + 100, 200, LABEL, INGRESS));
        pointList.add(getFlowingPoint(startTime + 200, 300, LABEL, INGRESS));
        pointList.add(getFlowingPoint(startTime + 300, 400, LABEL, INGRESS));

        pointList.add(3, getFlowingPoint(startTime, 100, LABEL + "1", INGRESS));
        pointList.add(2, getFlowingPoint(startTime, 100, LABEL, EGRESS));
        pointList.add(1, getFlowingPoint(startTime + 100, 200, LABEL, EGRESS));

        UnitConverter.convert(pointList);
        var labelIngressList = pointList.stream().filter(p -> LABEL.equals(p.getLabel()) && INGRESS.equals(p.getDirection())).toList();
        Assertions.assertEquals(4, labelIngressList.size());
        Assertions.assertEquals(8000, labelIngressList.get(0).getValue());
        Assertions.assertEquals(16_000, labelIngressList.get(1).getValue());
        Assertions.assertEquals(24_000, labelIngressList.get(2).getValue());
        Assertions.assertEquals(32_000, labelIngressList.get(3).getValue());

        var label1IngressList = pointList.stream().filter(p -> (LABEL + "1").equals(p.getLabel()) && INGRESS.equals(p.getDirection())).toList();
        Assertions.assertEquals(1, label1IngressList.size());

        var labelEgressList = pointList.stream().filter(p -> LABEL.equals(p.getLabel()) && EGRESS.equals(p.getDirection())).toList();
        Assertions.assertEquals(2, labelEgressList.size());
    }

    private FlowingPoint getFlowingPoint(long timestamp, long value, String label, String direction) {
        var point = new FlowingPoint();
        point.setLabel(label);
        point.setTimestamp(Instant.ofEpochMilli(timestamp));
        point.setValue(value);
        point.setDirection(direction);
        return point;
    }
}
