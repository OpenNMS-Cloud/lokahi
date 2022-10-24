package org.opennms.miniongateway.detector.api;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter which connects Ignite-executed operations to the server's internal processing.
 */
public interface LocalDetectorAdapter {
    CompletableFuture<Boolean> detect(
        String location,
        String systemId,
        String serviceName,
        String detectorName,
        InetAddress inetAddress,
        Integer nodeId
    );
}
