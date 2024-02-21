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
package org.opennms.miniongateway.taskset;

import java.io.IOException;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinPublisher.Session;
import org.opennms.taskset.contract.TaskSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process task set updates, publishing them to downstream minions and storing the latest version to provide to minions
 *  on request.
 *
 *  This is the EGRESS part of task set management flow:
 *      1. (INGRESS) updates received from other services, such as inventory
 *      2. (STORE + AGGREGATE) task set updates made against the Task Set store
 *      3. (EGRESS) On storage events, updates pushed downstream to Minions via Twin
 */
public class TaskSetPublisherImpl implements TaskSetPublisher {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetPublisherImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private GrpcTwinPublisher publisher;

    // ========================================
    // Interface
    // ----------------------------------------

    public TaskSetPublisherImpl(GrpcTwinPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishTaskSet(String tenantId, String location, TaskSet taskSet) {
        try {
            Session<TaskSet> session = publisher.register("task-set", TaskSet.class, tenantId, location);
            session.publish(taskSet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
