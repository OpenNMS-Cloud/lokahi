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

package org.opennms.horizon.server.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.service.metrics.QueryService;

import java.util.HashMap;

import static org.opennms.horizon.server.service.metrics.normalization.Constants.QUERY_FOR_BW_IN_UTIL_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.QUERY_FOR_TOTAL_NETWORK_IN_BITS;

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
        Assertions.assertEquals("(irate(ifHCInOctets{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"}[4m])*8) / " +
            "(ifHighSpeed{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"} *1000000) * 100 " +
            "unless ifHighSpeed{if_name=\"en0\",monitor=\"SNMP\",node_id=\"5\"} == 0", bwUtilQuery);
    }

}
