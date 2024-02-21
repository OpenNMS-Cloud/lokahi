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
package org.opennms.horizon.protobuf.marshalling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;
import org.opennms.horizon.shared.protobuf.marshalling.ProtoBufJsonDeserializer;
import org.opennms.horizon.shared.protobuf.marshalling.ProtoBufJsonSerializer;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;

public class ProtoBufJsonSerializationTest {

    ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {

        objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(TaskSet.class, new ProtoBufJsonDeserializer<>(TaskSet.class));
        simpleModule.addSerializer(TaskSet.class, new ProtoBufJsonSerializer<>(TaskSet.class));
        objectMapper.registerModule(simpleModule);
    }

    @Test
    public void testRoundTrip() throws JsonProcessingException {

        TaskSet taskSet = TaskSet.newBuilder()
                .addTaskDefinition(
                        TaskDefinition.newBuilder().setType(TaskType.MONITOR).build())
                .build();

        String json = objectMapper.writeValueAsString(taskSet);

        TaskSet newTaskSet = objectMapper.readValue(json, TaskSet.class);

        assertNotNull(newTaskSet);
        assertEquals(taskSet, newTaskSet);
    }
}
