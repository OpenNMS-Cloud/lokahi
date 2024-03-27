package org.opennms.horizon.minion.icmp.shell;

import com.google.protobuf.*;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.plugin.api.registries.MonitorRegistry;
import org.opennms.monitors.http.contract.HttpMonitorRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Command(scope = "opennms", name = "http-monitor", description = "Monitor list of ports.")
@Service
public class HttpMonitorCommand  implements Action {

    @Reference
    MonitorRegistry monitorRegistry;

    @Override
    public Object execute() {
        var httpMonitorManager = monitorRegistry.getService("HTTPMonitor");
        var httpMonitor = httpMonitorManager.create();
        Any configuration = Any.pack(HttpMonitorRequest.newBuilder()
                .setInetAddress("127.0.0.1")
            .build());

        Future future= httpMonitor.poll(null,configuration);
        return null;
    }


}
