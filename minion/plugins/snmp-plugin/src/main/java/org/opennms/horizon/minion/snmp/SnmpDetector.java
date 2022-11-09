package org.opennms.horizon.minion.snmp;

import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorRequest;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponseImpl;
import org.opennms.horizon.shared.snmp.SnmpHelper;

import java.util.concurrent.CompletableFuture;

public class SnmpDetector implements ServiceDetector {
    private final SnmpHelper snmpHelper;

    public SnmpDetector(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    @Override
    public CompletableFuture<ServiceDetectorResponse> detect(ServiceDetectorRequest request) {
        return CompletableFuture.completedFuture(
            ServiceDetectorResponseImpl.up()
        );
    }
}
