package org.opennms.miniongateway.taskset;

import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskSetServiceConfig {

    @Bean
    public TaskSetPublisher taskSetService(GrpcTwinPublisher publisher) {
        return new TaskSetPublisherImpl(publisher);
    }

}
