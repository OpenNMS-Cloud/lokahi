package org.opennms.horizon.minion.plugin.api;

import lombok.Builder;
import lombok.Data;
import org.opennms.taskset.contract.MonitorType;

import java.util.Map;

@Builder
@Data
public class ServiceMonitorResponseImpl implements ServiceMonitorResponse {
    private Status status;
    private String reason;
    private String ipAddress;
    private double responseTime;
    private DeviceConfig deviceConfig;
    private Map<String, Number> properties;
    private MonitorType monitorType;
    private long nodeId;
    private long monitoredServiceId;

    private long timestamp;

    public static ServiceMonitorResponse unknown() { return builder().status(Status.Unknown).build();}
    public static ServiceMonitorResponse down() { return builder().status(Status.Down).build();}

    public static ServiceMonitorResponse up() { return builder().status(Status.Up).build();}
    public static ServiceMonitorResponse unresponsive() { return builder().status(Status.Unresponsive).build();}


}
