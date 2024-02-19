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

import org.apache.ignite.IgniteLogger;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.resources.SpringResource;
import org.apache.ignite.services.Service;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalServiceFactory;
import org.opennms.taskset.contract.TaskDefinition;

/**
 * Ignite version of the service to execute workflows.  Uses the "local" version of the service,
 *  WorkflowExecutorLocalService, which is never serialized/deserialized, reducing the challenges that introduces.
 */
public class TaskExecutorIgniteService implements Service {

    private TaskDefinition taskDefinition;

    @LoggerResource
    private IgniteLogger logger;

    @SpringResource(resourceClass = TaskExecutorLocalServiceFactory.class)
    private transient TaskExecutorLocalServiceFactory workflowExecutorLocalServiceFactory;

    private transient TaskExecutorLocalService localService;
    private transient boolean shutdown;

    private transient Object sync;

    public TaskExecutorIgniteService(TaskDefinition taskDefinition) {
        this.taskDefinition = taskDefinition;
    }

    // ========================================
    // Ignite Service API
    // ----------------------------------------

    @Override
    public void init() throws Exception {
        sync = new Object();
        shutdown = false;
    }

    @Override
    public void execute() throws Exception {
        if (shutdown) {
            logger.info("Skipping execution of workflow; appears to have been canceled already");
            return;
        }

        TaskExecutorLocalService newLocalService = workflowExecutorLocalServiceFactory.create(taskDefinition);
        synchronized (sync) {
            if (!shutdown) {
                localService = newLocalService;
                localService.start();
            } else {
                logger.info("Aborting execution of workflow; appears to have been canceled before fully started");
            }
        }
    }

    @Override
    public void cancel() {
        TaskExecutorLocalService shutdownService = null;

        synchronized (sync) {
            if (!shutdown) {
                shutdownService = localService;
            }
            shutdown = true;
            localService = null;
        }

        if (shutdownService != null) {
            shutdownService.cancel();
        }
    }
}
