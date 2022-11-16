package org.opennms.horizon.inventory.service.taskset.publisher;

import org.opennms.taskset.contract.TaskSet;
import org.springframework.stereotype.Component;

@Component
public class DefaultTaskSetPublisher implements TaskSetPublisher {
    @Override
    public void publishTaskSet(String location, TaskSet taskSet) {
        System.out.println("DefaultTaskSetPublisher.publishTaskSet");
        System.out.println("location = " + location + ", taskSet = " + taskSet);
    }
}
