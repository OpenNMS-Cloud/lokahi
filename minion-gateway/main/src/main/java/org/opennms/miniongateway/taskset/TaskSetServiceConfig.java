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
        taskSetStorage.addAllTwinPublisherSessionListener(taskSetStorageListener);

        return result;
    }
}
