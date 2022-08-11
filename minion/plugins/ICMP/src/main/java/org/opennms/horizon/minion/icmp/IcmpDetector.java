package org.opennms.horizon.minion.icmp;

import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorRequest;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResults;

@AllArgsConstructor
public class IcmpDetector implements ServiceDetector {
    @Setter
    public String sampleConfig;
    @Override
    public CompletableFuture<ServiceDetectorResults> detect(ServiceDetectorRequest request) {
        return null;
    }
}
