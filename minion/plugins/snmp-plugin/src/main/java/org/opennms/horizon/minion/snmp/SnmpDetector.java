package org.opennms.horizon.minion.snmp;

import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorRequest;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResults;

public class SnmpDetector implements ServiceDetector {

    @Override
    public CompletableFuture<ServiceDetectorResults> detect(ServiceDetectorRequest request) {
        return null;
    }
}
