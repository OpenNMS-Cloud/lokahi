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

import org.opennms.horizon.minion.plugin.api.registries.CollectorRegistry;
import org.opennms.horizon.minion.plugin.api.registries.DetectorRegistry;
import org.opennms.horizon.minion.plugin.api.registries.ListenerFactoryRegistry;
import org.opennms.horizon.minion.plugin.api.registries.MonitorRegistry;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.minion.scheduler.OpennmsScheduler;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalServiceFactory;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutorLocalServiceFactoryImpl implements TaskExecutorLocalServiceFactory {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskExecutorLocalServiceFactoryImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final OpennmsScheduler scheduler;
    private final TaskExecutionResultProcessor resultProcessor;
    private final ListenerFactoryRegistry listenerFactoryRegistry;
    private final DetectorRegistry detectorRegistry;
    private final MonitorRegistry monitorRegistry;
    private final CollectorRegistry collectorRegistry;
    private final ScannerRegistry scannerRegistry;

    // ========================================
    // Constructor
    // ----------------------------------------

    public TaskExecutorLocalServiceFactoryImpl(
            OpennmsScheduler scheduler,
            TaskExecutionResultProcessor resultProcessor,
            ListenerFactoryRegistry listenerFactoryRegistry,
            DetectorRegistry detectorRegistry,
            MonitorRegistry monitorRegistry,
            CollectorRegistry collectorRegistry,
            ScannerRegistry scannerRegistry) {

        this.scheduler = scheduler;
        this.resultProcessor = resultProcessor;
        this.listenerFactoryRegistry = listenerFactoryRegistry;
        this.detectorRegistry = detectorRegistry;
        this.monitorRegistry = monitorRegistry;
        this.collectorRegistry = collectorRegistry;
        this.scannerRegistry = scannerRegistry;
    }

    // ========================================
    // API
    // ----------------------------------------

    @Override
    public TaskExecutorLocalService create(TaskDefinition taskDefinition) {

        switch (taskDefinition.getType()) {
            case SCANNER:
                return new TaskExecutorLocalScannerServiceImpl(taskDefinition, scannerRegistry, resultProcessor);

            case MONITOR:
                return new TaskExecutorLocalMonitorServiceImpl(
                        scheduler, taskDefinition, resultProcessor, monitorRegistry);

            case LISTENER:
                TaskListenerRetryable listenerService =
                        new TaskListenerRetryable(taskDefinition, resultProcessor, listenerFactoryRegistry);
                return new TaskCommonRetryExecutor(scheduler, taskDefinition, resultProcessor, listenerService);

            case CONNECTOR:
                TaskConnectorRetryable connectorService = new TaskConnectorRetryable(taskDefinition, resultProcessor);
                return new TaskCommonRetryExecutor(scheduler, taskDefinition, resultProcessor, connectorService);

            case COLLECTOR:
                return new TaskExecutorLocalCollectorServiceImpl(
                        taskDefinition, scheduler, resultProcessor, collectorRegistry);

            default:
                throw new RuntimeException("unrecognized taskDefinition type " + taskDefinition.getType());
        }
    }
}
