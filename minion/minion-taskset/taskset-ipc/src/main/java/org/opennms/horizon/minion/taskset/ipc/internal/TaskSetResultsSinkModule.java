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
package org.opennms.horizon.minion.taskset.ipc.internal;

import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.taskset.contract.TaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink Module for processing TaskSetResults.  Note this is used in the communication between the Minion and
 * Minion Gateway, so Tenant IDs are not explicitly handled here.
 */
public class TaskSetResultsSinkModule implements SinkModule<TaskSetResults, TaskSetResults> {

    public static final String MODULE_ID = "task-set-result";
    private final Logger logger = LoggerFactory.getLogger(TaskSetResultsSinkModule.class);

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 0;
    }

    @Override
    public byte[] marshal(TaskSetResults resultsMessage) {
        try {
            return resultsMessage.toByteArray();
        } catch (Exception e) {
            logger.warn("Error while marshalling message {}.", resultsMessage, e);
            return new byte[0];
        }
    }

    @Override
    public TaskSetResults unmarshal(byte[] bytes) {
        try {
            return TaskSetResults.parseFrom(bytes);
        } catch (Exception e) {
            logger.warn("Error while unmarshalling message.", e);
            return null;
        }
    }

    @Override
    public byte[] marshalSingleMessage(TaskSetResults resultsMessage) {
        return marshal(resultsMessage);
    }

    @Override
    public TaskSetResults unmarshalSingleMessage(byte[] bytes) {
        return unmarshal(bytes);
    }

    @Override
    public AggregationPolicy<TaskSetResults, TaskSetResults, ?> getAggregationPolicy() {
        return new IdentityAggregationPolicy<>();
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            public int getQueueSize() {
                return 10;
            }

            public int getNumThreads() {
                return 10;
            }
        };
    }
}
