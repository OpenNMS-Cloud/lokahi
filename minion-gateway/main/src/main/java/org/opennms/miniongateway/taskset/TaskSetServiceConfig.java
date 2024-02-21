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

import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.taskset.service.TaskSetStorage;
import org.opennms.miniongateway.taskset.service.TaskSetStorageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskSetServiceConfig {

    @Autowired
    private TaskSetStorage taskSetStorage;

    private TaskSetStorageListener taskSetStorageListener;

    @Bean
    public TaskSetPublisher taskSetService(GrpcTwinPublisher publisher) {
        TaskSetPublisher result = new TaskSetPublisherImpl(publisher);

        // Wire the publisher to listen for updates from the Task Set Storage
        taskSetStorageListener = result::publishTaskSet;
        taskSetStorage.addTaskSetStorageListener(taskSetStorageListener);

        return result;
    }
}
