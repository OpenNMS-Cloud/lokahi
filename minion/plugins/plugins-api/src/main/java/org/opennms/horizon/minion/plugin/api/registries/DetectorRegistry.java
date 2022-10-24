package org.opennms.horizon.minion.plugin.api.registries;

import java.util.Map;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;

public interface DetectorRegistry {

    ServiceDetectorManager getService(String type);
    int getServiceCount();
    Map<String, ServiceDetectorManager> getServices();
}
