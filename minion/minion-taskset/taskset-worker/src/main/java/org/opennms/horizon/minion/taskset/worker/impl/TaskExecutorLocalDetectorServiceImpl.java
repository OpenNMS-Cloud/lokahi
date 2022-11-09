package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorManager;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.registries.DetectorRegistry;
import org.opennms.horizon.minion.scheduler.OpennmsScheduler;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskExecutorLocalDetectorServiceImpl implements TaskExecutorLocalService {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutorLocalDetectorServiceImpl.class);

    private final TaskDefinition taskDefinition;
    private final OpennmsScheduler scheduler;
    private final TaskExecutionResultProcessor resultProcessor;
    private final DetectorRegistry detectorRegistry;
    private ServiceDetector detector = null;
    private AtomicBoolean active = new AtomicBoolean(false);

    public TaskExecutorLocalDetectorServiceImpl(OpennmsScheduler scheduler,
                                                TaskDefinition taskDefinition,
                                                DetectorRegistry detectorRegistry,
                                                TaskExecutionResultProcessor resultProcessor) {
        this.scheduler = scheduler;
        this.taskDefinition = taskDefinition;
        this.resultProcessor = resultProcessor;
        this.detectorRegistry = detectorRegistry;
    }

//========================================
// API
//----------------------------------------

    //todo: consider super class for start(), cancel(), executeSerializedIteration() as this is repeated code from TaskExecutorLocalMoitorServiceImpl
    @Override
    public void start() throws Exception {
        try {
            String whenSpec = taskDefinition.getSchedule().trim();

            // If the value is all digits, use it as periodic time in milliseconds
            if (whenSpec.matches("^\\d+$")) {
                long period = Long.parseLong(taskDefinition.getSchedule());

                scheduler.schedulePeriodically(taskDefinition.getId(), period, TimeUnit.MILLISECONDS, this::executeSerializedIteration);
            } else {
                // Not a number, REQUIRED to be a CRON expression
                scheduler.scheduleTaskOnCron(taskDefinition.getId(), whenSpec, this::executeSerializedIteration);
            }

        } catch (Exception exc) {
            // TODO: throttle - we can get very large numbers of these in a short time
            log.warn("error starting workflow {}", taskDefinition.getId(), exc);
        }
    }

    @Override
    public void cancel() {
        scheduler.cancelTask(taskDefinition.getId());
    }

    private void executeSerializedIteration() {
        System.out.println(">>>> tbigg - TaskExecutorLocalDetectorServiceImpl.executeSerializedIteration");
        // Verify it's not already active
        if (active.compareAndSet(false, true)) {
            log.trace("Executing iteration of task: workflow-uuid={}", taskDefinition.getId());
            executeIteration();
        } else {
            log.debug("Skipping iteration of task as prior iteration is still active: workflow-uuid={}", taskDefinition.getId());
        }
    }

    private void executeIteration() {
        System.out.println(">>>> tbigg - TaskExecutorLocalMonitorServiceImpl.executeIteration");
        try {
            if (detector == null) {
                ServiceDetector lazyDetector = lookupDetector(taskDefinition);
                if (lazyDetector != null) {
                    this.detector = lazyDetector;
                }
            }
            if (detector != null) {

                CompletableFuture<ServiceDetectorResponse> future = detector.detect(null);
                future.whenComplete(this::handleExecutionComplete);
            } else {
                log.info("Skipping service monitor execution; monitor not found: monitor=" + taskDefinition.getPluginName());
            }
        } catch (Exception exc) {
            // TODO: throttle - we can get very large numbers of these in a short time
            log.warn("error executing workflow " + taskDefinition.getId(), exc);
        }
    }

    private void handleExecutionComplete(ServiceDetectorResponse serviceDetectorResponse, Throwable exc) {
        System.out.println(">>>> tbigg - TaskExecutorLocalDetectorServiceImpl.handleExecutionComplete");
        System.out.println("serviceDetectorResponse = " + serviceDetectorResponse);
        System.out.println("exc = " + exc);
    }

    private ServiceDetector lookupDetector(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ServiceDetectorManager result = detectorRegistry.getService(pluginName);

        return result.create();
    }
}
