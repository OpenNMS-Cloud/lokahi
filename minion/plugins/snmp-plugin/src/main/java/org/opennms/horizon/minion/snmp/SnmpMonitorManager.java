package org.opennms.horizon.minion.snmp;

import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.StrategyResolver;

public class SnmpMonitorManager implements ServiceMonitorManager {

    private final StrategyResolver strategyResolver;
    private final SnmpHelper snmpHelper;

    public SnmpMonitorManager(StrategyResolver strategyResolver, SnmpHelper snmpHelper) {
        this.strategyResolver = strategyResolver;
        this.snmpHelper = snmpHelper;
    }

    @Override
    public ServiceMonitor create() {
        return new SnmpMonitor(strategyResolver, snmpHelper);
    }
}
