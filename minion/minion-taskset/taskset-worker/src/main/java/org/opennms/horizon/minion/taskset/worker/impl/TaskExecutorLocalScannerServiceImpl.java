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

import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.minion.taskset.worker.TaskExecutionResultProcessor;
import org.opennms.horizon.minion.taskset.worker.TaskExecutorLocalService;
import org.opennms.horizon.shared.logging.Logging;
import org.opennms.taskset.contract.TaskDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskExecutorLocalScannerServiceImpl implements TaskExecutorLocalService {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutorLocalScannerServiceImpl.class);
    private static final String LOG_PREFIX = "scanner";
    private final TaskDefinition taskDefinition;
    private final TaskExecutionResultProcessor resultProcessor;
    private final ScannerRegistry scannerRegistry;

    private CompletableFuture<ScanResultsResponse> future;

    public TaskExecutorLocalScannerServiceImpl(
            TaskDefinition taskDefinition,
            ScannerRegistry scannerRegistry,
            TaskExecutionResultProcessor resultProcessor) {
        this.taskDefinition = taskDefinition;
        this.resultProcessor = resultProcessor;
        this.scannerRegistry = scannerRegistry;
    }

    // ========================================
    // API
    // ----------------------------------------

    @Override
    public void start() throws Exception {
        try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(LOG_PREFIX)) {
            Scanner scanner = lookupScanner(taskDefinition);
            log.info("Create Scanner for {}", taskDefinition.getPluginName());
            if (scanner != null) {
                future = scanner.scan(taskDefinition.getConfiguration());
                future.whenComplete(this::handleExecutionComplete);
            }
        } catch (Exception exc) {
            if (log.isDebugEnabled()) {
                log.debug("error executing workflow = " + taskDefinition.getId(), exc);
            } else {
                log.warn("error executing workflow id = {}, message = {}" + taskDefinition.getId(), exc.getMessage());
            }
        }
    }

    @Override
    public void cancel() {
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
        future = null;
    }

    private void handleExecutionComplete(ScanResultsResponse response, Throwable exc) {
        log.trace("Completed execution: workflow-uuid = {}", taskDefinition.getId());

        if (exc == null) {
            resultProcessor.queueSendResult(taskDefinition.getId(), response);
        } else {
            log.warn("error executing workflow; workflow-uuid = " + taskDefinition.getId(), exc);
        }
    }

    private Scanner lookupScanner(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ScannerManager result = scannerRegistry.getService(pluginName);
        if (result != null) { // TODO: add node scanner plugin
            return result.create();
        }
        return null;
    }
}
