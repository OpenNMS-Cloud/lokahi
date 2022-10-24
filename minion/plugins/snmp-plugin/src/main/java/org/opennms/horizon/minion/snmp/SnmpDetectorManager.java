package org.opennms.horizon.minion.snmp;

import java.util.function.Consumer;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResults;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;

public class SnmpDetectorManager implements ServiceDetectorManager {

    @Override
    public ServiceDetector create(Consumer<ServiceDetectorResults> resultProcessor) {
        return new SnmpDetector();
    }
}
