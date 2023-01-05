package org.opennms.horizon.minion.azure;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minion.azure.http.AzureHttpClient;
import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;

@RequiredArgsConstructor
public class AzureMonitorManager implements ServiceMonitorManager {
    @Override
    public ServiceMonitor create() {
        AzureHttpClient client = new AzureHttpClient();
        return new AzureMonitor(client);
    }
}
