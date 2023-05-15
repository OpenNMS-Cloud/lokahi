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

package org.opennms.horizon.minion.azure;

import com.google.protobuf.Any;
import org.opennms.azure.contract.AzureMonitorRequest;
import org.opennms.horizon.minion.plugin.api.AbstractServiceMonitor;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class AzureMonitor extends AbstractServiceMonitor {
    private static final Logger log = LoggerFactory.getLogger(AzureMonitor.class);

    private final AzureHttpClient client;

    public AzureMonitor(AzureHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config) {

        CompletableFuture<ServiceMonitorResponse> future = new CompletableFuture<>();

        try {
            if (!config.is(AzureMonitorRequest.class)) {
                throw new IllegalArgumentException("configuration must be an AzureMonitorRequest; type-url=" + config.getTypeUrl());
            }

            AzureMonitorRequest request = config.unpack(AzureMonitorRequest.class);

            AzureOAuthToken token = client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), request.getTimeoutMs(), request.getRetries());

            long startMs = System.currentTimeMillis();

            AzureInstanceView instanceView = client.getInstanceView(token, request.getSubscriptionId(),
                request.getResourceGroup(), request.getResource(), request.getTimeoutMs(), request.getRetries());

            if (instanceView.isUp()) {

                future.complete(
                    ServiceMonitorResponseImpl.builder()
                        .monitorType(MonitorType.AZURE)
                        .status(ServiceMonitorResponse.Status.Up)
                        .responseTime(System.currentTimeMillis() - startMs)
                        .nodeId(svc.getNodeId())
                        .ipAddress("azure-node-" + svc.getNodeId())
                        .build()
                );
            } else {
                future.complete(
                    ServiceMonitorResponseImpl.builder()
                        .monitorType(MonitorType.AZURE)
                        .status(ServiceMonitorResponse.Status.Down)
                        .nodeId(svc.getNodeId())
                        .ipAddress("azure-node-" + svc.getNodeId())
                        .build()
                );
            }

        } catch (Exception e) {
            log.error("Failed to monitor for azure resource", e);
            future.complete(
                ServiceMonitorResponseImpl.builder()
                    .reason("Failed to monitor for azure resource: " + e.getMessage())
                    .monitorType(MonitorType.AZURE)
                    .status(ServiceMonitorResponse.Status.Down)
                    .nodeId(svc.getNodeId())
                    .build()
            );
        }

        return future;
    }
}
