package org.opennms.horizon.minion.plugin.api;

import lombok.Builder;
import lombok.Data;
import org.opennms.taskset.contract.MonitorType;

import java.util.Map;

@Data
@Builder
public class ServiceDetectorResponseImpl implements ServiceDetectorResponse {
    private MonitorType monitorType;
    private boolean serviceDetected; // enum instead?
    private String reason;
    private String ipAddress;
}
