package org.opennms.horizon.minion.plugin.api;

import lombok.Builder;
import lombok.Data;
import org.opennms.taskset.contract.MonitorType;

@Data
@Builder
public class ServiceDetectorResponseImpl implements ServiceDetectorResponse {
    private MonitorType monitorType;
    private boolean serviceDetected; // enum instead?
    private String reason;
    private String ipAddress;
    private long nodeId;

    @Override
    public MonitorType getMonitorType() {
        return monitorType;
    }

    @Override
    public boolean isServiceDetected() {
        return serviceDetected;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public String getIpAddress() {
        return ipAddress;
    }
}
