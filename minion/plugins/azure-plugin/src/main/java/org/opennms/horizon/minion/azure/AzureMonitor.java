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
                request.getClientId(), request.getClientSecret(), request.getTimeout());

            long startMs = System.currentTimeMillis();

            AzureInstanceView instanceView = client.getInstanceView(token, request.getSubscriptionId(),
                request.getResourceGroup(), request.getResource(), request.getTimeout());

            if (instanceView.isUp()) {

                future.complete(
                    ServiceMonitorResponseImpl.builder()
                        .monitorType(MonitorType.ICMP) // HACK: using ICMP because the UI makes a query with ICMP for status
                        .status(ServiceMonitorResponse.Status.Up)
                        .responseTime(System.currentTimeMillis() - startMs)
                        .nodeId(svc.getNodeId())
                        .ipAddress(request.getHost())
                        .build()
                );
            } else {
                future.complete(
                    ServiceMonitorResponseImpl.builder()
                        .monitorType(MonitorType.ICMP)
                        .status(ServiceMonitorResponse.Status.Down)
                        .nodeId(svc.getNodeId())
                        .ipAddress(request.getHost())
                        .build()
                );
            }

        } catch (Exception e) {
            log.error("Failed to monitor for azure resource", e);
            future.complete(
                ServiceMonitorResponseImpl.builder()
                    .reason("Failed to monitor for azure resource: " + e.getMessage())
                    .monitorType(MonitorType.ICMP)
                    .status(ServiceMonitorResponse.Status.Down)
                    .nodeId(svc.getNodeId())
                    .build()
            );
        }

        return future;
    }
}
