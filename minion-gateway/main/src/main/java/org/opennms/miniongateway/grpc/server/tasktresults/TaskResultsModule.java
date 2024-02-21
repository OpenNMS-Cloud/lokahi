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
package org.opennms.miniongateway.grpc.server.tasktresults;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.taskset.contract.TaskSetResults;

/**
 * Sink Module for TaskSetResults that is used for handling results sent by Minions.  Since the results are received
 * from Minions, there is no explicit Tenant ID in the structures.
 */
public class TaskResultsModule implements SinkModule<TaskSetResults, TaskSetResults> {

    public static final String MODULE_ID = "task-set-result";

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(TaskSetResults message) {
        return message.toByteArray();
    }

    @Override
    public TaskSetResults unmarshal(byte[] content) {
        try {
            return TaskSetResults.parseFrom(content);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] marshalSingleMessage(TaskSetResults message) {
        return marshal((TaskSetResults) message);
    }

    @Override
    public TaskSetResults unmarshalSingleMessage(byte[] message) {
        return unmarshal(message);
    }

    @Override
    public AggregationPolicy<TaskSetResults, TaskSetResults, ?> getAggregationPolicy() {
        return new IdentityAggregationPolicy<>();
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return 10;
            }

            @Override
            public int getNumThreads() {
                return 1;
            }
        };
    }
}
