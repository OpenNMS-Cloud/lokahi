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
package org.opennms.miniongateway.taskset.service;

import io.grpc.stub.StreamObserver;
import java.io.IOException;
import javax.annotation.PostConstruct;
import lombok.Setter;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * API Endpoints exposed to other services to perform operations on Task Sets, such as adding and removing Tasks from
 * a TaskSet.
 *
 *  This is the INGRESS part of task set management flow:
 *      1. (INGRESS) updates received from other services, such as inventory
 *      2. (STORE + AGGREGATE) task set updates made against the Task Set store
 *      3. (EGRESS) updates pushed downstream to Minions via Twin
 */
@Component
public class TaskSetGrpcService extends TaskSetServiceGrpc.TaskSetServiceImplBase {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TaskSetGrpcService.class);

    private Logger LOG = DEFAULT_LOGGER;

    @Autowired
    @Qualifier("internalGrpcIpcServer")
    @Setter // Testability
    private GrpcIpcServer grpcIpcServer;

    @Autowired
    @Setter // Testability
    private TaskSetStorage taskSetStorage;

    @Autowired
    @Setter // Testability
    private TaskSetGrpcServiceUpdateProcessorFactory taskSetGrpcServiceUpdateProcessorFactory;

    // ========================================
    // Lifecycle
    // ----------------------------------------

    @PostConstruct
    public void start() throws IOException {
        grpcIpcServer.startServer(this);
        LOG.info("Initiated TaskSet GRPC Service");
    }

    // ========================================
    // Service API
    // ----------------------------------------

    /**
     * Update the requested task set with the given list of task updates.  Note that removals are processed first followed
     * by additions.
     *
     * @param request
     * @param responseObserver
     */
    @Override
    public void updateTasks(UpdateTasksRequest request, StreamObserver<UpdateTasksResponse> responseObserver) {
        TaskSetGrpcServiceUpdateProcessor updateProcessor = taskSetGrpcServiceUpdateProcessorFactory.create(request);

        try {
            taskSetStorage.atomicUpdateTaskSetForLocation(
                    request.getTenantId(), request.getLocationId(), updateProcessor);
        } catch (RuntimeException rtExc) {
            // Log exceptions here that might otherwise get swallowed
            LOG.warn("error applying task set updates", rtExc);
            throw rtExc;
        }

        UpdateTasksResponse response = UpdateTasksResponse.newBuilder()
                .setNumNew(updateProcessor.getNumNew())
                .setNumReplaced(updateProcessor.getNumReplaced())
                .setNumRemoved(updateProcessor.getNumRemoved())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
