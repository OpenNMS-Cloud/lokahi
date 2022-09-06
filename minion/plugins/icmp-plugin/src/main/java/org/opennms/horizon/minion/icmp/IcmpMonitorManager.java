package org.opennms.horizon.minion.icmp;

import lombok.RequiredArgsConstructor;
import org.opennms.netmgt.icmp.PingerFactory;
import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;

@RequiredArgsConstructor
public class IcmpMonitorManager implements ServiceMonitorManager {
    private final PingerFactory pingerFactory;

    @Override
    public ServiceMonitor create() {
        IcmpMonitor icmpMonitor =  new IcmpMonitor(pingerFactory);

        return icmpMonitor;
    }
}
