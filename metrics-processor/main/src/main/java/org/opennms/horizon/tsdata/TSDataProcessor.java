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

package org.opennms.horizon.tsdata;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TSDataProcessor {

    private final TaskSetResultProcessor taskSetResultProcessor;

    // NOTE: it might be better to split the asynchronous execution into a separate class to make testing here, and there,
    //  more straight-forward (i.e. more "Real Obvious").  Then the submission here would look something like this:
    //  `taskSetResultAsyncProcessor.submitTaskResultForProcessing(tenantId, result)`
    @Setter // Testability
    private Consumer<Runnable> submitForExecutionOp = this::defaultExecutionSubmissionOp;

    public TSDataProcessor(TaskSetResultProcessor taskSetResultProcessor) {
        this.taskSetResultProcessor = taskSetResultProcessor;
    }

    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(@Payload byte[] data) {
        try {
            TenantLocationSpecificTaskSetResults results = TenantLocationSpecificTaskSetResults.parseFrom(data);
            String tenantId = results.getTenantId();
            if (Strings.isBlank(tenantId)) {
                throw new RuntimeException("Missing tenant id");
            }

            String locationId = results.getLocationId();
            if (Strings.isBlank(locationId)) {
                throw new RuntimeException("Missing location");
            }

            results.getResultsList().forEach(
                result -> submitForExecutionOp.accept(() -> taskSetResultProcessor.processTaskResult(tenantId, locationId, result)));
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid data from kafka", e);
        }
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Default operation for submission of the given Supplier for execution which uses CompletableFuture's supplyAsync()
     *  method to schedule execution.
     *
     * @param runnable
     */
    private void defaultExecutionSubmissionOp(Runnable runnable) {
        CompletableFuture.supplyAsync(() -> {
            runnable.run();
            return null;    // Not doing anything with the Future, so the value is of no consequence
        });
    }
}
