package org.opennms.horizon.minion.taskset.worker;

import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;

public interface TaskExecutionResultProcessor {
    /**
     * Queue the given result to be sent out.
     *
     * @param uuid
     * @param serviceMonitorResponse
     */
    void queueSendResult(String uuid, ServiceMonitorResponse serviceMonitorResponse);
}
