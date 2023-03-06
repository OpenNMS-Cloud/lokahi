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

//========================================
// Interface
//----------------------------------------

    public TaskSetPublisherImpl(GrpcTwinPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishTaskSet(String tenantId, String location, TaskSet taskSet) {
        try {
            Session<TaskSet> session = publisher.register("task-set", TaskSet.class, tenantId, location);
            session.publish(tenantId, taskSet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
