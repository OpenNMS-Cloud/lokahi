/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.plugin.api.CollectionRequest;
import org.opennms.horizon.minion.plugin.api.CollectionSet;
import org.opennms.horizon.minion.plugin.api.CollectorRequestImpl;
import org.opennms.horizon.minion.plugin.api.ServiceCollector;
import org.opennms.horizon.minion.plugin.api.ServiceCollectorManager;
import org.opennms.horizon.minion.plugin.api.registries.CollectorRegistry;
import org.opennms.horizon.minion.scheduler.OpennmsScheduler;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.horizon.shared.logging.Logging;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskExecutorLocalCollectorServiceImpl implements TaskExecutorLocalService {

    private static final Logger LOG = LoggerFactory.getLogger(TaskExecutorLocalCollectorServiceImpl.class);
    private static final String LOG_PREFIX = "collector";

    private AtomicBoolean active = new AtomicBoolean(false);

    private TaskDefinition taskDefinition;
    private OpennmsScheduler scheduler;
    private TaskExecutionResultProcessor resultProcessor;
    private CollectorRegistry collectorRegistry;

    public TaskExecutorLocalCollectorServiceImpl(TaskDefinition taskDefinition,
                                                 OpennmsScheduler scheduler,
                                                 TaskExecutionResultProcessor resultProcessor,
                                                 CollectorRegistry collectorRegistry) {
        this.taskDefinition = taskDefinition;
        this.scheduler = scheduler;
        this.resultProcessor = resultProcessor;
        this.collectorRegistry = collectorRegistry;
    }

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
            if (LOG.isDebugEnabled()) {
                LOG.debug("error starting workflow {}", taskDefinition.getId(), exc);
            } else {
                LOG.warn("error starting workflow {}, message = {}", taskDefinition.getId(), exc.getMessage());
            }

        }
    }

    @Override
    public void cancel() {
        scheduler.cancelTask(taskDefinition.getId());
    }

    private void executeSerializedIteration() {
        // Verify it's not already active
        if (active.compareAndSet(false, true)) {
            LOG.trace("Executing iteration of task: workflow-uuid={}", taskDefinition.getId());
            try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(LOG_PREFIX)) {
                executeIteration();
            }
        } else {
            LOG.debug("Skipping iteration of task as prior iteration is still active: workflow-uuid={}", taskDefinition.getId());
        }
    }

    private void executeIteration() {
        try {
            ServiceCollector serviceCollector = lookupCollector(taskDefinition);

            if (serviceCollector != null) {
                CollectionRequest collectionRequest = configureCollectionRequest(taskDefinition);
                CompletableFuture<CollectionSet> future = serviceCollector.collect(collectionRequest, taskDefinition.getConfiguration());
                future.whenComplete(this::handleExecutionComplete);
            } else {
                LOG.info("Skipping service collector execution; collector not found: collector=" + taskDefinition.getPluginName());
            }
        } catch (Exception exc) {
            // TODO: throttle - we can get very large numbers of these in a short time
            if (LOG.isDebugEnabled()) {
                LOG.debug("error executing workflow {}", taskDefinition.getId(), exc);
            } else {
                LOG.warn("error executing workflow {}, message = {}",taskDefinition.getId(), exc.getMessage());
            }

        }
    }

    private CollectionRequest configureCollectionRequest(TaskDefinition taskDefinition) {
        return CollectorRequestImpl.builder().nodeId(taskDefinition.getNodeId()).build();
    }


    private void handleExecutionComplete(CollectionSet collectionSet, Throwable exc) {
        LOG.info("Completed execution: workflow-uuid={}", taskDefinition.getId());
        active.set(false);

        if (exc == null) {
            resultProcessor.queueSendResult(taskDefinition.getId(), collectionSet);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("error executing workflow; workflow-uuid= {}", taskDefinition.getId(), exc);
            } else {
                LOG.warn("error executing workflow; workflow-uuid= {}, message = {}", taskDefinition.getId(), exc.getMessage());
            }
        }
    }

    private ServiceCollector lookupCollector(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ServiceCollectorManager result = collectorRegistry.getService(pluginName);

        return result.create();
    }
}
