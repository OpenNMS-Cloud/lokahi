package org.opennms.miniongateway.grpc.server.tasktresults;

import com.google.protobuf.Message;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Forwarder of TaskResults - received via GRPC and forwarded to Kafka.
 */
@Component
public class TaskResultsKafkaForwarder implements MessageConsumer<Message, Message> {

    public static final String DEFAULT_TASK_RESULTS_TOPIC = "task-set.results";

    private final Logger logger = LoggerFactory.getLogger(TaskResultsKafkaForwarder.class);

    @Autowired
    @Qualifier("kafkaByteArrayProducerTemplate")
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    @Override
    public SinkModule<Message, Message> getModule() {
        return new TaskResultsModule();
    }

    @Override
    public void handleMessage(Message messageLog) {
        logger.debug("Received results; sending to Kafka: kafka-topic={}, message={}", kafkaTopic, messageLog);

        byte[] rawContent = messageLog.toByteArray();
        this.kafkaTemplate.send(kafkaTopic, rawContent);
    }
}
