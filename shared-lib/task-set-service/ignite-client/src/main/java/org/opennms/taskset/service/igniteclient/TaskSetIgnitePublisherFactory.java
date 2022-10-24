package org.opennms.taskset.service.igniteclient;

import org.apache.ignite.client.IgniteClient;
import org.opennms.taskset.service.api.TaskSetPublisher;

public interface TaskSetIgnitePublisherFactory {
    TaskSetPublisher create(IgniteClient igniteClient);
}
