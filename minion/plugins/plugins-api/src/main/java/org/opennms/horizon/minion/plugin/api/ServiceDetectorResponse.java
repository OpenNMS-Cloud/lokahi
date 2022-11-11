package org.opennms.horizon.minion.plugin.api;

import org.opennms.taskset.contract.MonitorType;

public interface ServiceDetectorResponse {

    /**
     * @return type of monitor that produced the response.
     */
    MonitorType getMonitorType();

    /**
     * @return true if the service was detected, false otherwise
     */
    boolean isServiceDetected();

    /**
     * @return reason behind the current detection status when the service is not detected
     */
    String getReason();

    /**
     * @return IP address that was under detection
     */
    String getIpAddress();
}
