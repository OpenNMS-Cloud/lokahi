package org.opennms.horizon.minion.plugin.api;

import java.util.function.Consumer;

public interface ServiceDetectorManager {
    ServiceDetector create(Consumer<ServiceDetectorResults> resultProcessor);
}
