package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.registries.DetectorRegistry;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class TaskExecutorLocalDetectorServiceImpl implements TaskExecutorLocalService {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutorLocalDetectorServiceImpl.class);

    private final TaskDefinition taskDefinition;
    private final TaskExecutionResultProcessor resultProcessor;
    private final DetectorRegistry detectorRegistry;

    private CompletableFuture<ServiceDetectorResponse> future;

    public TaskExecutorLocalDetectorServiceImpl(TaskDefinition taskDefinition,
                                                DetectorRegistry detectorRegistry,
                                                TaskExecutionResultProcessor resultProcessor) {
        this.taskDefinition = taskDefinition;
        this.resultProcessor = resultProcessor;
        this.detectorRegistry = detectorRegistry;
    }

//========================================
// API
//----------------------------------------

    @Override
    public void start() throws Exception {
        try {
            ServiceDetector detector = lookupDetector(taskDefinition);

            future = detector.detect(taskDefinition.getConfiguration(), taskDefinition.getContext().getNodeId());
            future.whenComplete(this::handleExecutionComplete);

        } catch (Exception exc) {
            log.warn("error executing workflow = " + taskDefinition.getId(), exc);
        }
    }

    @Override
    public void cancel() {
        if (future != null
            && !future.isCancelled()) {

            future.cancel(true);
        }
        future = null;
    }

    private void handleExecutionComplete(ServiceDetectorResponse serviceDetectorResponse, Throwable exc) {
        log.trace("Completed execution: workflow-uuid = {}", taskDefinition.getId());

        if (exc == null) {
            resultProcessor.queueSendResult(taskDefinition.getId(), taskDefinition.getContext(), serviceDetectorResponse);
        } else {
            log.warn("error executing workflow; workflow-uuid = " + taskDefinition.getId(), exc);
        }
    }

    private ServiceDetector lookupDetector(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ServiceDetectorManager result = detectorRegistry.getService(pluginName);

        return result.create();
    }
}
