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

import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.minion.plugin.api.ScannerManager;
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

    private CompletableFuture<ScanResultsResponse> future;

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
            Scanner scanner = lookupScanner(taskDefinition);
            //TODO: add node scanner
            log.info("Create Scanner for {}", taskDefinition.getPluginName());
            if(scanner != null) {
                future = scanner.scan(taskDefinition.getConfiguration());
                future.whenComplete(this::handleExecutionComplete);
            }
        } catch (Exception exc) {
            log.warn("error executing workflow = " + taskDefinition.getId(), exc);
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
            resultProcessor.queueSendResult(taskDefinition.getId(), taskDefinition.getContext(), response);
        } else {
            log.warn("error executing workflow; workflow-uuid = " + taskDefinition.getId(), exc);
        }
    }

    private Scanner lookupScanner(TaskDefinition taskDefinition) {
        String pluginName = taskDefinition.getPluginName();

        ScannerManager result = scannerRegistry.getService(pluginName);
        if(result != null) { //TODO: add node scanner plugin
            return result.create();
        }
        return null;
    }
}
