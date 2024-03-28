/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.taskset.worker.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitor;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorManager;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.registries.MonitorRegistry;
import org.opennms.horizon.minion.scheduler.OpennmsScheduler;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.horizon.shared.logging.Logging;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local implementation of the service to execute a Monitor workflow.  This class runs "locally" only, so it is never
 *  serialized / deserialized; this enables the "ignite" service to be a thin implementation, reducing the chances of
 *  problems due to serialization/deserialization.
 */
public class TaskExecutorLocalMonitorServiceImpl implements TaskExecutorLocalService {
    private ExecutorService executor;
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskExecutorLocalMonitorServiceImpl.class);

    private Logger log = DEFAULT_LOGGER;
    private static final String LOG_PREFIX = "monitor";

    private TaskDefinition taskDefinition;
    private OpennmsScheduler scheduler;
    private TaskExecutionResultProcessor resultProcessor;
    private MonitorRegistry monitorRegistry;
    private ServiceMonitor monitor = null;

    private AtomicBoolean active = new AtomicBoolean(false);

    public TaskExecutorLocalMonitorServiceImpl(
            OpennmsScheduler scheduler,
            TaskDefinition taskDefinition,
            TaskExecutionResultProcessor resultProcessor,
            MonitorRegistry monitorRegistry) {
        this.taskDefinition = taskDefinition;
        this.scheduler = scheduler;
        this.resultProcessor = resultProcessor;
        this.monitorRegistry = monitorRegistry;
        this.executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                .setNameFormat("monitor-service-response-handler")
                .build());
    }

    // ========================================
    // API
    // ----------------------------------------

    @Override
    public void start() throws Exception {
        try {
            String whenSpec = taskDefinition.getSchedule().trim();

            // If the value is all digits, use it as periodic time in milliseconds
            if (whenSpec.matches("^\\d+$")) {
                long period = Long.parseLong(taskDefinition.getSchedule());

                scheduler.schedulePeriodically(
                        taskDefinition.getId(), period, TimeUnit.MILLISECONDS, this::executeSerializedIteration);
            } else {
                // Not a number, REQUIRED to be a CRON expression
                scheduler.scheduleTaskOnCron(taskDefinition.getId(), whenSpec, this::executeSerializedIteration);
            }

        } catch (Exception exc) {
            // TODO: throttle - we can get very large numbers of these in a short time
            if (log.isDebugEnabled()) {
                log.debug("error starting workflow {}", taskDefinition.getId(), exc);
            } else {
                log.warn("error starting workflow {}, message {}", taskDefinition.getId(), exc.getMessage());
            }
        }
    }

    @Override
    public void cancel() {
        scheduler.cancelTask(taskDefinition.getId());
    }

    // ========================================
    // Processing
    // ----------------------------------------

    private void executeSerializedIteration() {
        // Verify it's not already active
        if (active.compareAndSet(false, true)) {
            log.trace("Executing iteration of task: workflow-uuid={}", taskDefinition.getId());
            try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(LOG_PREFIX)) {
                executeIteration();
            }
        } else {
            log.debug(
                    "Skipping iteration of task as prior iteration is still active: workflow-uuid={}",
                    taskDefinition.getId());
        }
    }

    private void executeIteration() {
        try {
            if (monitor == null) {
                ServiceMonitor lazyMonitor = lookupMonitor(taskDefinition);
                if (lazyMonitor != null) {
                    this.monitor = lazyMonitor;
                }
            }
            if (monitor != null) {
                // TBD888: populate host, or stop?
                MonitoredService monitoredService = configureMonitoredService(taskDefinition);
                CompletableFuture.runAsync(
                        () -> {
                            monitor.poll(monitoredService, taskDefinition.getConfiguration())
                                    .whenCompleteAsync(this::handleExecutionComplete, executor);
                        },
                        executor);
            } else {
                log.info("Skipping service monitor execution; monitor not found: monitor="
                        + taskDefinition.getPluginName());
            }
        } catch (Exception exc) {
            // TODO: throttle - we can get very large numbers of these in a short time
            if (log.isDebugEnabled()) {
                log.debug("error executing workflow {}", taskDefinition.getId(), exc);
            } else {
                log.warn("error executing workflow {} , message = {}", taskDefinition.getId(), exc.getMessage());
            }
        }
    }

    private void handleExecutionComplete(ServiceMonitorResponse serviceMonitorResponse, Throwable exc) {
        log.trace("Completed execution: workflow-uuid={}", taskDefinition.getId());
        active.set(false);

        if (exc == null) {
            resultProcessor.queueSendResult(taskDefinition.getId(), serviceMonitorResponse);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("error executing workflow; workflow-uuid= {}", taskDefinition.getId(), exc);
            } else {
                log.warn(
                        "error executing workflow; workflow-uuid= {}, message = {}",
                        taskDefinition.getId(),
                        exc.getMessage());
            }
        }
    }

    private MonitoredService configureMonitoredService(TaskDefinition taskDefinition) {
        return new GeneralMonitoredService(
                "TBD", "TBD", taskDefinition.getNodeId(), "TBD", "TBD", null, taskDefinition.getMonitorServiceId());
    }

    private ServiceMonitor lookupMonitor(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ServiceMonitorManager result = monitorRegistry.getService(pluginName);

        return result.create();
    }
}
