package org.opennms.horizon.inventory.component;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.service.taskset.manager.TaskSetManager;
import org.opennms.horizon.inventory.service.taskset.response.DetectorResponseService;
import org.opennms.horizon.inventory.service.taskset.response.MonitorResponseService;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSetResultsConsumer {
    private final DetectorResponseService detectorResponseService;
    private final MonitorResponseService monitorResponseService;
    private final TaskSetManager taskSetManager;
    private final TaskSetPublisher taskSetPublisher;

    @KafkaListener(topics = "${kafka.topics.task-set-results}", concurrency = "1")
    public void receiveMessage(byte[] data) {
        try {
            TaskSetResults message = TaskSetResults.parseFrom(data);

            Set<String> locationSet = new HashSet<>();

            for (TaskResult taskResult : message.getResultsList()) {

                String location = taskResult.getLocation();
                locationSet.add(location);

                if (taskResult.hasDetectorResponse()) {
                    detectorResponseService.accept(location, taskResult.getDetectorResponse());
                } else if (taskResult.hasMonitorResponse()) {
                    monitorResponseService.accept(location, taskResult.getMonitorResponse());
                } else {
                    log.warn("Unknown task set response = {}", taskResult);
                }
            }

            for (String location : locationSet) {

                TaskSet taskSet = taskSetManager.getTaskSet(location);
                if (taskSet != null) {
                    taskSetPublisher.publishTaskSet(location, taskSet);
                }
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing task set results", e);
        }
    }
}
