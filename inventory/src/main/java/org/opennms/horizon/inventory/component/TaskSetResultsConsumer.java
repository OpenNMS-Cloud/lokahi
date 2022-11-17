package org.opennms.horizon.inventory.component;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.service.taskset.response.DetectorResponseService;
import org.opennms.horizon.inventory.service.taskset.response.MonitorResponseService;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSetResultsConsumer {
    private final DetectorResponseService detectorResponseService;
    private final MonitorResponseService monitorResponseService;

    @KafkaListener(topics = "${kafka.topics.task-set-results}", concurrency = "1")
    public void receiveMessage(byte[] data) {
        try {
            TaskSetResults message = TaskSetResults.parseFrom(data);

            for (TaskResult taskResult : message.getResultsList()) {

                if (taskResult.hasDetectorResponse()) {
                    detectorResponseService.accept(taskResult.getDetectorResponse());
                } else if (taskResult.hasMonitorResponse()) {
                    monitorResponseService.accept(taskResult.getMonitorResponse());
                } else {
                    log.warn("Unknown task set response = {}", taskResult);
                }
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing task set results", e);
        }
    }
}
