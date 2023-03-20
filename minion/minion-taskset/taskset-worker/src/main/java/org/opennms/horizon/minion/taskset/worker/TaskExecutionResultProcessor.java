package org.opennms.horizon.minion.taskset.worker;

import java.util.Map;
import org.opennms.horizon.minion.plugin.api.CollectionSet;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.taskset.contract.TaskContext;

public interface TaskExecutionResultProcessor {
    /**
     * Queue the given scan result to be sent out.
     *
     * @param taskId
     * @param scanResultsResponse
     */
    void queueSendResult(String taskId, TaskContext taskContext, ScanResultsResponse scanResultsResponse);

    /**
     * Queue the given detector result to be sent out.
     *
     * @param taskId
     * @param serviceDetectorResponse
     */
    void queueSendResult(String taskId, TaskContext taskContext, ServiceDetectorResponse serviceDetectorResponse);

    /**
     * Queue the given monitor result to be sent out.
     *
     * @param taskId
     * @param serviceMonitorResponse
     */
    void queueSendResult(String taskId, TaskContext taskContext, ServiceMonitorResponse serviceMonitorResponse);


    void queueSendResult(String taskId, TaskContext taskContext, CollectionSet collectionSet);
}
