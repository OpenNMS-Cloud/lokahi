package org.opennms.horizon.minion.icmp;

import java.util.function.Consumer;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResults;

public class IcmpDetectorManager implements ServiceDetectorManager {

    @Override
    public ServiceDetector create(Consumer<ServiceDetectorResults> resultProcessor) {
        return new IcmpDetector();
    }
}
