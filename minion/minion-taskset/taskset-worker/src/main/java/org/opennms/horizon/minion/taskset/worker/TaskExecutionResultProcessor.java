package org.opennms.horizon.minion.taskset.worker;

import org.opennms.horizon.minion.plugin.api.CollectionSet;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;

public interface TaskExecutionResultProcessor {
    /**
     * Queue the given scan result to be sent out.
     *
     * @param uuid
     * @param scanResultsResponse
     */
    void queueSendResult(String uuid, ScanResultsResponse scanResultsResponse);

    /**
     * Queue the given detector result to be sent out.
     *
     * @param uuid
     * @param serviceDetectorResponse
     */
    void queueSendResult(String uuid, ServiceDetectorResponse serviceDetectorResponse);

    /**
     * Queue the given monitor result to be sent out.
     *
     * @param uuid
     * @param serviceMonitorResponse
     */
    void queueSendResult(String uuid, ServiceMonitorResponse serviceMonitorResponse);


    void queueSendResult(String uuid, CollectionSet collectionSet);
}
