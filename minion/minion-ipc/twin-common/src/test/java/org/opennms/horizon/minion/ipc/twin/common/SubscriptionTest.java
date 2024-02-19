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
package org.opennms.horizon.minion.ipc.twin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.horizon.minion.ipc.twin.common.AbstractTwinSubscriber.Subscription;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.protobuf.marshalling.ProtoBufJsonSerializer;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;

public class SubscriptionTest {

    Subscription subscription = new LocalTwinSubscriberImpl(new IpcIdentity() {
        @Override
        public String getId() {
            return "blahId";
        }
    })
    .new Subscription("blahKey");

    @Before
    public void setUp() throws Exception {}

    @Test
    public void sendRpcRequest() {}

    @Ignore
    @Test
    public void accept() throws IOException {

        SimpleModule simpleModule = new SimpleModule();

        simpleModule.addSerializer(new ProtoBufJsonSerializer<>(TaskSet.class));

        TaskSet taskSet = TaskSet.newBuilder()
                .addTaskDefinition(
                        TaskDefinition.newBuilder().setType(TaskType.MONITOR).build())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(simpleModule);

        TwinUpdate twinUpdate = new TwinUpdate();
        twinUpdate.setVersion(1);
        twinUpdate.setPatch(false);
        twinUpdate.setSessionId("blahSessionId");
        byte[] objInBytes = objectMapper.writeValueAsBytes(taskSet);
        twinUpdate.setObject(objInBytes);
        twinUpdate.setKey("blahKey");

        subscription.update(twinUpdate);

        twinUpdate.setPatch(true);
        twinUpdate.setVersion(2);

        subscription.update(twinUpdate);
    }
}
