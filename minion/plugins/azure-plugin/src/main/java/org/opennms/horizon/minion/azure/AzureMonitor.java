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
package org.opennms.horizon.minion.azure;

import com.google.protobuf.Any;
import java.util.concurrent.CompletableFuture;
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
                throw new IllegalArgumentException(
                        "configuration must be an AzureMonitorRequest; type-url=" + config.getTypeUrl());
            }

            AzureMonitorRequest request = config.unpack(AzureMonitorRequest.class);

            AzureOAuthToken token = client.login(
                    request.getDirectoryId(),
                    request.getClientId(),
                    request.getClientSecret(),
                    request.getTimeoutMs(),
                    request.getRetries());

            long startMs = System.currentTimeMillis();

            AzureInstanceView instanceView = client.getInstanceView(
                    token,
                    request.getSubscriptionId(),
                    request.getResourceGroup(),
                    request.getResource(),
                    request.getTimeoutMs(),
                    request.getRetries());

            if (instanceView.isUp()) {

                future.complete(ServiceMonitorResponseImpl.builder()
                        .monitorType(MonitorType.AZURE)
                        .status(ServiceMonitorResponse.Status.Up)
                        .responseTime(System.currentTimeMillis() - startMs)
                        .nodeId(svc.getNodeId())
                        .ipAddress("azure-node-" + svc.getNodeId())
                        .build());
            } else {
                future.complete(ServiceMonitorResponseImpl.builder()
                        .monitorType(MonitorType.AZURE)
                        .status(ServiceMonitorResponse.Status.Down)
                        .nodeId(svc.getNodeId())
                        .ipAddress("azure-node-" + svc.getNodeId())
                        .build());
            }

        } catch (Exception e) {
            log.error("Failed to monitor for azure resource", e);
            future.complete(ServiceMonitorResponseImpl.builder()
                    .reason("Failed to monitor for azure resource: " + e.getMessage())
                    .monitorType(MonitorType.AZURE)
                    .status(ServiceMonitorResponse.Status.Down)
                    .nodeId(svc.getNodeId())
                    .build());
        }

        return future;
    }
}
