package org.opennms.horizon.inventory.grpc.taskset;

import io.grpc.inprocess.InProcessChannelBuilder;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestTaskSetGrpcConfig {

    @Bean
    @Primary
    public TaskSetServiceGrpc.TaskSetServiceBlockingStub testTaskSetServiceBlockingStub() {
        return TaskSetServiceGrpc.newBlockingStub(
            InProcessChannelBuilder.forName(TaskSetServiceGrpc.SERVICE_NAME).directExecutor().build());
    }
}
