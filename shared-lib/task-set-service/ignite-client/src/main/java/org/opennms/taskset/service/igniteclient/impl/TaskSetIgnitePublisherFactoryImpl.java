package org.opennms.taskset.service.igniteclient.impl;

import org.apache.ignite.client.IgniteClient;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.igniteclient.TaskSetIgnitePublisherFactory;

public class TaskSetIgnitePublisherFactoryImpl implements TaskSetIgnitePublisherFactory {
    @Override
    public TaskSetPublisher create(IgniteClient igniteClient) {
        TaskSetIgnitePublisherImpl result = new TaskSetIgnitePublisherImpl();
        result.setIgniteClient(igniteClient);

        return result;
    }
}
