/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.server.service.metrics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {
    public static final String AZURE_MONITOR_TYPE = "AZURE";
    public static final String AZURE_SCAN_TYPE = "AZURE_SCAN";


    public static final String QUERY_PREFIX = "query=";
    public static final String NETWORK_IN_BITS = "network_in_bits";
    public static final String NETWORK_OUT_BITS = "network_out_bits";

    public static final String QUERY_FOR_TOTAL_NETWORK_IN_BITS = "irate(ifHCInOctets%s[4m])*8";
    public static final String QUERY_FOR_TOTAL_NETWORK_OUT_BITS = "irate(ifHCOutOctets%s[4m])*8";

    public static final String QUERY_FOR_AZURE_TOTAL_NETWORK_IN_BITS = "sum(sum_over_time(network_in_total_bytes%s[4m]))*8";
    public static final String QUERY_FOR_AZURE_TOTAL_NETWORK_OUT_BITS = "sum(sum_over_time(network_out_total_bytes%s[4m]))*8";

    public static final String BW_IN_PERCENTAGE = "bw_util_network_in";
    public static final String BW_OUT_PERCENTAGE = "bw_util_network_out";

    public static final String QUERY_FOR_BW_IN_UTIL_PERCENTAGE = "(irate(ifHCInOctets%1$s[4m])*8) " +
        "/ (ifHighSpeed%1$s *1000000) * 100 unless ifHighSpeed%1$s == 0";
    public static final String QUERY_FOR_BW_OUT_UTIL_PERCENTAGE = "(irate(ifHCOutOctets%1$s[4m])*8) " +
        "/ (ifHighSpeed%1$s *1000000) * 100 unless ifHighSpeed%1$s == 0";

    public static final String NETWORK_ERRORS_IN = "network_errors_in";
    public static final String NETWORK_ERRORS_OUT = "network_errors_out";

    public static final String QUERY_FOR_NETWORK_ERRORS_IN = "irate(ifInErrors%s[4m])";
    public static final String QUERY_FOR_NETWORK_ERRORS_OUT = "irate(ifOutErrors%s[4m])";

    // Total Network
    public static final String TOTAL_NETWORK_BYTES_IN = "total_network_bytes_in";
    public static final String TOTAL_NETWORK_BYTES_OUT = "total_network_bytes_out";
    public static final String QUERY_FOR_TOTAL_NETWORK_BYTES_IN = """
                sum(irate(ifHCInOctets[4m])) or vector(0) +
                sum(sum_over_time(network_in_total_bytes[4m])) or vector(0)
                    unless
                count(irate(ifHCInOctets[4m])) == 0 and
                count(sum_over_time(network_in_total_bytes[4m])) == 0
        """;

    public static final String QUERY_FOR_TOTAL_NETWORK_BYTES_OUT = """
                sum(irate(ifHCOutOctets[4m])) or vector(0) +
                sum(sum_over_time(network_out_total_bytes[4m])) or vector(0)
                    unless
                count(irate(ifHCOutOctets[4m])) == 0 and
                count(sum_over_time(network_out_total_bytes[4m])) == 0
        """;
}
