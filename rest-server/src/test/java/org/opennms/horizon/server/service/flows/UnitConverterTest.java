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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.model.flows.FlowingPoint;

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
            Assertions.assertEquals(
                    startTime + 100L * i, pointList.get(i).getTimestamp().toEpochMilli());
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
        var labelIngressList = pointList.stream()
                .filter(p -> LABEL.equals(p.getLabel()) && INGRESS.equals(p.getDirection()))
                .toList();
        Assertions.assertEquals(4, labelIngressList.size());
        Assertions.assertEquals(8000, labelIngressList.get(0).getValue());
        Assertions.assertEquals(16_000, labelIngressList.get(1).getValue());
        Assertions.assertEquals(24_000, labelIngressList.get(2).getValue());
        Assertions.assertEquals(32_000, labelIngressList.get(3).getValue());

        var label1IngressList = pointList.stream()
                .filter(p -> (LABEL + "1").equals(p.getLabel()) && INGRESS.equals(p.getDirection()))
                .toList();
        Assertions.assertEquals(1, label1IngressList.size());

        var labelEgressList = pointList.stream()
                .filter(p -> LABEL.equals(p.getLabel()) && EGRESS.equals(p.getDirection()))
                .toList();
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
