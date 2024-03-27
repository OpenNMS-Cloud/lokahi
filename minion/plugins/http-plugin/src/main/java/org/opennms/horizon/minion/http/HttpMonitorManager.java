package org.opennms.horizon.minion.http;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;

@RequiredArgsConstructor
public class HttpMonitorManager  implements ServiceMonitorManager {


    @Override
    public ServiceMonitor create() {
        HttpMonitor httpMonitor = new HttpMonitor();

        return httpMonitor;
    }
}
