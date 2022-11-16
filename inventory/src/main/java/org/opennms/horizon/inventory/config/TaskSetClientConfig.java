package org.opennms.horizon.inventory.config;

import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.client.TaskSetGrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskSetClientConfig {

    @Value("${grpc.client.task-set.host:localhost}")
    private String host;

    @Value("${grpc.client.task-set.port:8990}")
    private int port;

    @Value("${grpc.client.task-set.tlsEnabled:false}")
    private boolean tlsEnabled;

    @Value("${grpc.client.task-set.maxMessageSize:10485760}")
    private int maxMessageSize;

    @Bean
    public TaskSetPublisher grpcTaskSetPublisher() {
        TaskSetGrpcClient client = new TaskSetGrpcClient();
        client.setHost(host);
        client.setPort(port);
        client.setTlsEnabled(tlsEnabled);
        client.setMaxMessageSize(maxMessageSize);

        client.init();

        return client;
    }
}
