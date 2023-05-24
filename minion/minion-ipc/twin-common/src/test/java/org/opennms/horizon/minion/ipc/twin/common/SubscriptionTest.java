package org.opennms.horizon.minion.ipc.twin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.Message;
import java.io.IOException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.opennms.horizon.minion.ipc.twin.common.AbstractTwinSubscriber.Subscription;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.protobuf.marshalling.ProtoBufJsonSerializer;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;

public class SubscriptionTest {

    Subscription subscription =  new LocalTwinSubscriberImpl(new IpcIdentity() {
        @Override
        public String getId() {
            return "blahId";
        }

    }).new Subscription("blahKey");

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void sendRpcRequest() {
    }

    @Ignore
    @Test
    public void accept() throws IOException {

        SimpleModule simpleModule = new SimpleModule();

        simpleModule.addSerializer(new ProtoBufJsonSerializer<>(TaskSet.class));

        TaskSet taskSet = TaskSet.newBuilder().addTaskDefinition(TaskDefinition.newBuilder().setType(TaskType.MONITOR).build()).build();

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
