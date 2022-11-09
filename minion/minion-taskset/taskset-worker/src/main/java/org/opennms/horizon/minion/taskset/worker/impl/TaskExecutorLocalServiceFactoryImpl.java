package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.plugin.api.registries.DetectorRegistry;
import org.opennms.horizon.minion.plugin.api.registries.ListenerFactoryRegistry;
import org.opennms.horizon.minion.plugin.api.registries.MonitorRegistry;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalServiceFactory;
import org.opennms.horizon.minion.scheduler.OpennmsScheduler;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutorLocalServiceFactoryImpl implements TaskExecutorLocalServiceFactory {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskExecutorLocalServiceFactoryImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final OpennmsScheduler scheduler;
    private final TaskExecutionResultProcessor resultProcessor;
    private final ListenerFactoryRegistry listenerFactoryRegistry;

//========================================
// Constructor
//----------------------------------------

    public TaskExecutorLocalServiceFactoryImpl(
        OpennmsScheduler scheduler,
        TaskExecutionResultProcessor resultProcessor,
        ListenerFactoryRegistry listenerFactoryRegistry
    ) {

        this.scheduler = scheduler;
        this.resultProcessor = resultProcessor;
        this.listenerFactoryRegistry = listenerFactoryRegistry;
    }

//========================================
// API
//----------------------------------------

    @Override
    public TaskExecutorLocalService create(TaskDefinition taskDefinition, DetectorRegistry detectorRegistry, MonitorRegistry monitorRegistry) {
        switch (taskDefinition.getType()) {
            case DETECTOR:
                return new TaskExecutorLocalDetectorServiceImpl(scheduler, taskDefinition, detectorRegistry, resultProcessor);

            case MONITOR:
                return new TaskExecutorLocalMonitorServiceImpl(scheduler, taskDefinition, resultProcessor, monitorRegistry);

            case LISTENER:
                TaskListenerRetryable listenerService = new TaskListenerRetryable(taskDefinition, resultProcessor, listenerFactoryRegistry);
                return new TaskCommonRetryExecutor(scheduler, taskDefinition, resultProcessor, listenerService);

            case CONNECTOR:
                TaskConnectorRetryable connectorService = new TaskConnectorRetryable(taskDefinition, resultProcessor);
                return new TaskCommonRetryExecutor(scheduler, taskDefinition, resultProcessor, connectorService);

            default:
                throw new RuntimeException("unrecognized taskDefinition type " + taskDefinition.getType());
        }
    }
}
