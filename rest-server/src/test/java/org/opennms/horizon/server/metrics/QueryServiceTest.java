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
package org.opennms.horizon.server.metrics;

import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_BW_IN_UTIL_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_BITS_IN;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_BITS_OUT;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_IN_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_PREFIX;
import static org.opennms.horizon.server.service.metrics.Constants.TOTAL_NETWORK_BITS_IN;
import static org.opennms.horizon.server.service.metrics.Constants.TOTAL_NETWORK_BITS_OUT;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.service.metrics.Constants;
import org.opennms.horizon.server.service.metrics.QueryService;

public class QueryServiceTest {

    @Test
    public void testLabelsSubstitution() {

        QueryService queryService = new QueryService();
        var labels = new HashMap<String, String>();
        labels.put("if_name", "en0");
        labels.put("monitor", "SNMP");
        labels.put("node_id", "5");
        var labelQuery = queryService.getLabelsQueryString(labels);
        var query = String.format(QUERY_FOR_TOTAL_NETWORK_IN_BITS, labelQuery);
        Assertions.assertEquals("irate(ifHCInOctets{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"}[4m])*8", query);

        var bwUtilQuery = String.format(QUERY_FOR_BW_IN_UTIL_PERCENTAGE, labelQuery);
        Assertions.assertEquals(
                "(irate(ifHCInOctets{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"}[4m])*8) / "
                        + "(ifHighSpeed{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"} *1000000) * 100 "
                        + "unless ifHighSpeed{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"} == 0",
                bwUtilQuery);
    }

    @Test
    public void testCustomQuery() {
        QueryService queryService = new QueryService();
        var labels = new HashMap<String, String>();
        labels.put("instance", "192.168.1.1");
        labels.put("monitor", "ICMP");
        labels.put("system_id", "minion-standalone");
        var queryString = queryService.getQueryString(
                Optional.empty(), Constants.REACHABILITY_PERCENTAGE, labels, 24, TimeRangeUnit.HOUR);
        Assertions.assertEquals(
                "query=(count_over_time(response_time_msec{instance=\"192.168.1.1\","
                        + "system_id=\"minion-standalone\",monitor=\"ICMP\"}[24h])/1440)*100 or vector(0)",
                queryString);
    }

    @Test
    void testTotalQuery() {
        long end = System.currentTimeMillis() / 1000L;
        long start = end - Duration.ofHours(24).getSeconds();
        QueryService queryService = new QueryService();

        // We pass our 'end' value to ensure our start/end values match exactly
        var bitsInQuery = queryService.getQueryString(
                Optional.empty(), TOTAL_NETWORK_BITS_IN, new HashMap<>(), 24, TimeRangeUnit.HOUR, end);
        var inSplitQuery = bitsInQuery.split("&");
        Assertions.assertEquals(
                QUERY_PREFIX + URLEncoder.encode(QUERY_FOR_TOTAL_NETWORK_BITS_IN, StandardCharsets.UTF_8),
                inSplitQuery[0]);
        Assertions.assertEquals("start=" + start, inSplitQuery[1]);
        Assertions.assertEquals("end=" + end, inSplitQuery[2]);
        Assertions.assertEquals("step=2m", inSplitQuery[3]);

        // We pass our 'end' value to ensure our start/end values match exactly
        var bitsOutQuery = queryService.getQueryString(
                Optional.empty(), TOTAL_NETWORK_BITS_OUT, new HashMap<>(), 24, TimeRangeUnit.HOUR, end);
        var outSplitQuery = bitsOutQuery.split("&");
        Assertions.assertEquals(
                QUERY_PREFIX + URLEncoder.encode(QUERY_FOR_TOTAL_NETWORK_BITS_OUT, StandardCharsets.UTF_8),
                outSplitQuery[0]);
        Assertions.assertEquals("start=" + start, outSplitQuery[1]);
        Assertions.assertEquals("end=" + end, outSplitQuery[2]);
        Assertions.assertEquals("step=2m", outSplitQuery[3]);
    }
}
