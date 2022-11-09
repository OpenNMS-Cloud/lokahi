package org.opennms.horizon.minion.snmp;

import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.shared.snmp.SnmpHelper;

public class SnmpDetectorManager implements ServiceDetectorManager {

    private final SnmpHelper snmpHelper;

    public SnmpDetectorManager(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    @Override
    public ServiceDetector create() {
        return new SnmpDetector(snmpHelper);
    }
}
