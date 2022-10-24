package org.opennms.horizon.minion.plugin.api;

import java.util.concurrent.CompletableFuture;

public interface ServiceDetector {

    CompletableFuture<ServiceDetectorResults> detect(ServiceDetectorRequest request);

}
