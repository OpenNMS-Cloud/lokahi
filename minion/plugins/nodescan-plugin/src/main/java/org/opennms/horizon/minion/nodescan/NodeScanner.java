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

package org.opennms.horizon.minion.nodescan;

import java.util.concurrent.CompletableFuture;

import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.taskset.contract.ScannerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Any;

public class NodeScanner implements Scanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeScanner.class);
    private static final String DEFAULT_OBJECT_IDENTIFIER = ".1.3.6.1.2.1.1.2.0";
    private final SnmpHelper snmpHelper;

    public NodeScanner(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    @Override
    public CompletableFuture<ScanResultsResponse> scan(Any config) {
        LOGGER.info("Received node scan config {}", config);
        if(!config.is(NodeScanRequest.class)) {
            throw new IllegalArgumentException("Task config must be a NodeScanRequest, this is wrong type: " + config.getTypeUrl());
        }
        CompletableFuture<ScanResultsResponse> resultFuture = new CompletableFuture<>();
        try {
            NodeScanRequest scanRequest = config.unpack(NodeScanRequest.class);
            //TODO: how to create node scan response, add more detector tools
            ScannerResponse scannerResponse = ScannerResponse.newBuilder().build();
            resultFuture.complete(ScanResultsResponseImpl.builder().results(scannerResponse).build());
        } catch (Exception e) {
            LOGGER.error("Failed to scan node with task config: {}", config);
            resultFuture.complete(ScanResultsResponseImpl.builder()
                .reason("Failed to scan node with task config: "+ config + " " + e).build());
        }
        return resultFuture;
    }
}
