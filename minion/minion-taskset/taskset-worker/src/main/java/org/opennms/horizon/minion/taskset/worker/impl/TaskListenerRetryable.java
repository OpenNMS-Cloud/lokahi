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

import com.google.protobuf.Any;
import org.apache.ignite.resources.SpringResource;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.minion.plugin.api.ListenerFactory;
import org.opennms.horizon.minion.plugin.api.registries.ListenerFactoryRegistry;
import org.opennms.horizon.minion.taskset.worker.RetryableExecutor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core of the Task Definition Executor for LISTENERS which implements the RetryableExecutor, focusing the logic for starting
 *  and maintaining the listener.  Used with WorkflowCommonRetryExecutor for retry handling.
 *
 * NOTE: there currently is no mechanism by which a LISTENER plugin can notify of a lost listener.  If there is a need
 *  to trigger retries, a way for the Listener to notify back of the failure must be added.
 */
public class TaskListenerRetryable implements RetryableExecutor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskListenerRetryable.class);

    @SpringResource(resourceClass = ListenerFactoryRegistry.class)
    private ListenerFactoryRegistry listenerFactoryRegistry;

    private Logger log = DEFAULT_LOGGER;

    private TaskDefinition taskDefinition;
    private Listener listener;
    private TaskExecutionResultProcessor resultProcessor;

    private Runnable onDisconnect;

    public TaskListenerRetryable(
            TaskDefinition taskDefinition,
            TaskExecutionResultProcessor resultProcessor,
            ListenerFactoryRegistry listenerFactoryRegistry) {
        this.taskDefinition = taskDefinition;
        this.resultProcessor = resultProcessor;
        this.listenerFactoryRegistry = listenerFactoryRegistry;
    }

    // ========================================
    // API
    // ----------------------------------------

    @Override
    public void init(Runnable handleRetryNeeded) {
        onDisconnect = handleRetryNeeded;
    }

    @Override
    public void attempt(Any config) throws Exception {
        ListenerFactory listenerFactory = lookupListenerFactory(taskDefinition);

        if (listenerFactory != null) {
            log.info(
                    "Staring listener: plugin-name={}; workflow-id={}",
                    taskDefinition.getPluginName(),
                    taskDefinition.getId());

            listener = listenerFactory.create(taskDefinition.getConfiguration());

            listener.start();
        } else {
            log.warn(
                    "Listener plugin not registered; workflow will not run: plugin-name={}; workflow-id={}",
                    taskDefinition.getPluginName(),
                    taskDefinition.getId());

            throw new Exception("Listener plugin not registered: plugin-name=" + taskDefinition.getPluginName());
        }
    }

    @Override
    public void cancel() {
        if (listener != null) {
            listener.stop();
        }
    }

    // ========================================
    // Setup Internals
    // ----------------------------------------
    private ListenerFactory lookupListenerFactory(TaskDefinition workflow) {
        String pluginName = workflow.getPluginName();

        ListenerFactory result = listenerFactoryRegistry.getService(pluginName);

        return result;
    }
}
