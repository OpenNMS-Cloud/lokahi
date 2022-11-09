package org.opennms.horizon.minion.icmp;

import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;

public class IcmpDetectorManager implements ServiceDetectorManager {

    @Override
    public ServiceDetector create() {
        return new IcmpDetector();
    }
}
