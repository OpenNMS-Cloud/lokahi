package org.opennms.horizon.minion.plugin.api.registries;

import org.opennms.horizon.minion.plugin.api.ScannerManager;

import java.util.Map;

public interface ScannerRegistry {

    ScannerManager getService(String type);
    int getServiceCount();
    Map<String, ScannerManager> getServices();
}
