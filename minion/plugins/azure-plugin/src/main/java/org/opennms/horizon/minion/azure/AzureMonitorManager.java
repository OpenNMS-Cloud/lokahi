package org.opennms.horizon.minion.azure;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;

@RequiredArgsConstructor
public class AzureMonitorManager implements ServiceMonitorManager {
    @Override
    public ServiceMonitor create() {
        AzureHttpClient client = new AzureHttpClient();
        return new AzureMonitor(client);
    }
}
