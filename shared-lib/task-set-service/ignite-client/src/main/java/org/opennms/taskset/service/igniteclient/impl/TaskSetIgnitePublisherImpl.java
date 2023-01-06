/*
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
 */

package org.opennms.taskset.service.igniteclient.impl;

import org.apache.ignite.client.IgniteClient;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.model.LocatedTaskSet;

import java.util.List;
import java.util.function.Consumer;

@Deprecated
public class TaskSetIgnitePublisherImpl implements TaskSetPublisher {

    private IgniteClient igniteClient;

//========================================
// Getters and Setters
//----------------------------------------

    public IgniteClient getIgniteClient() {
        return igniteClient;
    }

    public void setIgniteClient(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
    }


//========================================
// Ignite Task Set Client API
//----------------------------------------

    @Override
    public void publishTaskSet(String tenantId, String location, TaskSet taskSet) {
        LocatedTaskSet locatedTaskSet = new LocatedTaskSet(tenantId, location, taskSet);

        igniteClient.services().serviceProxy(TASK_SET_PUBLISH_SERVICE, Consumer.class).accept(locatedTaskSet);
    }

    @Override
    public void publishNewTasks(String tenantId, String location, List<TaskDefinition> taskList) {

    }
}
