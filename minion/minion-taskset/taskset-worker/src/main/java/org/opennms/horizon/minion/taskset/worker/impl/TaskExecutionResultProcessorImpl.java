package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.SyncDispatcher;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

public class TaskExecutionResultProcessorImpl implements TaskExecutionResultProcessor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskExecutionResultProcessorImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final SyncDispatcher<TaskSetResults> taskSetSinkDispatcher;
    private final IpcIdentity identity;

    public TaskExecutionResultProcessorImpl(SyncDispatcher<TaskSetResults> taskSetSinkDispatcher, IpcIdentity identity) {
        this.taskSetSinkDispatcher = taskSetSinkDispatcher;
        this.identity = identity;
    }


//========================================
// API
//----------------------------------------

    @Override
    public void queueSendResult(String id, ServiceMonitorResponse result) {
        log.debug("O-POLL STATUS: status={}; reason={}", result.getStatus(), result.getReason());

        TaskSetResults taskSetResults = formatTaskSetResults(id, result);

        taskSetSinkDispatcher.send(taskSetResults);
    }

//========================================
// Internals
//----------------------------------------

    private TaskSetResults formatTaskSetResults(String id, ServiceMonitorResponse result) {
        TaskResult taskResult =
            TaskResult.newBuilder()
                .setId(id)
                .setStatus(result.getStatus().toString())
                .setReason(Optional.of(result).map(ServiceMonitorResponse::getReason).orElse(TaskResult.getDefaultInstance().getReason()))
                .setIpAddress(result.getIpAddress())
                .setLocation(identity.getLocation())
                .setSystemId(identity.getId())
                .setResponseTime(result.getResponseTime())
                .putAllMetrics(Optional.of(result).map(ServiceMonitorResponse::getProperties).orElse(Collections.EMPTY_MAP))
                .build();

        TaskSetResults taskSetResults =
            TaskSetResults.newBuilder()
                .addResults(taskResult)
                .build();

        return taskSetResults;
    }
//                 .setReason(result.getReason() != null ?
//                                result.getReason() :
//                                TaskResult.getDefaultInstance().getReason())
}
