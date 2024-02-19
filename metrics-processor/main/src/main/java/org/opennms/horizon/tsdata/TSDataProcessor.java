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
package org.opennms.horizon.tsdata;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TSDataProcessor {

    private final TaskSetResultProcessor taskSetResultProcessor;

    // NOTE: it might be better to split the asynchronous execution into a separate class to make testing here, and
    // there,
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

            results.getResultsList()
                    .forEach(result -> submitForExecutionOp.accept(
                            () -> taskSetResultProcessor.processTaskResult(tenantId, locationId, result)));
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid data from kafka", e);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    /**
     * Default operation for submission of the given Supplier for execution which uses CompletableFuture's supplyAsync()
     *  method to schedule execution.
     *
     * @param runnable
     */
    private void defaultExecutionSubmissionOp(Runnable runnable) {
        CompletableFuture.supplyAsync(() -> {
            runnable.run();
            return null; // Not doing anything with the Future, so the value is of no consequence
        });
    }
}
