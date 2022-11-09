package org.opennms.horizon.minion.icmp;

import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorRequest;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;

@AllArgsConstructor
public class IcmpDetector implements ServiceDetector {
    @Override
    public CompletableFuture<ServiceDetectorResponse> detect(ServiceDetectorRequest request) {
        return null;
    }
}
