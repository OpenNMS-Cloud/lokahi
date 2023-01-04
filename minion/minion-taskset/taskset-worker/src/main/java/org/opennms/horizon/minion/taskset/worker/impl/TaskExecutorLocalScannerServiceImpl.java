package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.minion.plugin.api.AzureScannerResponse;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class TaskExecutorLocalScannerServiceImpl implements TaskExecutorLocalService {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutorLocalScannerServiceImpl.class);

    private final TaskDefinition taskDefinition;
    private final TaskExecutionResultProcessor resultProcessor;
    private final ScannerRegistry scannerRegistry;

    private CompletableFuture<AzureScannerResponse> future;

    public TaskExecutorLocalScannerServiceImpl(TaskDefinition taskDefinition,
                                               ScannerRegistry scannerRegistry,
                                               TaskExecutionResultProcessor resultProcessor) {
        this.taskDefinition = taskDefinition;
        this.resultProcessor = resultProcessor;
        this.scannerRegistry = scannerRegistry;
    }

//========================================
// API
//----------------------------------------

    @Override
    public void start() throws Exception {
        try {
            Scanner scanner = loookupAzureScanner(taskDefinition);

            future = scanner.scan(taskDefinition.getConfiguration());
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

    private void handleExecutionComplete(AzureScannerResponse response, Throwable exc) {
        log.trace("Completed execution: workflow-uuid = {}", taskDefinition.getId());

        if (exc == null) {
            resultProcessor.queueSendResult(taskDefinition.getId(), response);
        } else {
            log.warn("error executing workflow; workflow-uuid = " + taskDefinition.getId(), exc);
        }
    }

    private Scanner loookupAzureScanner(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ScannerManager result = scannerRegistry.getService(pluginName);

        return result.create();
    }
}
