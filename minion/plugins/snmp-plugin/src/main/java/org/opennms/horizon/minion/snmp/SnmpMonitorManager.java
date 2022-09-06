package org.opennms.horizon.minion.snmp;

import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;
import org.opennms.horizon.shared.snmp.StrategyResolver;

import java.util.function.Consumer;

public class SnmpMonitorManager implements ServiceMonitorManager {

    private final StrategyResolver strategyResolver;

    public SnmpMonitorManager(StrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    @Override
    public ServiceMonitor create() {
        return new SnmpMonitor(strategyResolver);
    }
}
