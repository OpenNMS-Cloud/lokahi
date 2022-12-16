package org.opennms.miniongateway.grpc.server.heartbeat;

import com.google.protobuf.Message;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
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
 * Forwarder of Heartbeat messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class HeartbeatKafkaForwarder implements MessageConsumer<Message, Message> {
    public static final String DEFAULT_TASK_RESULTS_TOPIC = "heartbeat";

    private final Logger logger = LoggerFactory.getLogger(HeartbeatKafkaForwarder.class);
    private final RateLimitedLog usingDefaultTenantIdLog =
        RateLimitedLog
            .withRateLimit(logger)
            .maxRate(1)
            .every(Duration.ofMinutes(1))
            .build();

    @Autowired
    @Qualifier("kafkaByteArrayProducerTemplate")
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${task.results.kafka-topic:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    @Autowired
    private TenantIDGrpcServerInterceptor tenantIDGrpcInterceptor;

    @Override
    public SinkModule<Message, Message> getModule() {
        return new HeartbeatModule();
    }

    @Override
    public void handleMessage(Message messageLog) {
        // Retrieve the Tenant ID from the TenantID GRPC Interceptor
        String tenantId = tenantIDGrpcInterceptor.readCurrentContextTenantId();
        logger.info("Received heartbeat; sending to Kafka: tenant-id: {}; kafka-topic={}; message={}", tenantId, kafkaTopic, messageLog);
        byte[] rawContent = messageLog.toByteArray();

        ProducerRecord<String, byte[]> producerRecord = formatProducerRecord(rawContent, tenantId);

        this.kafkaTemplate.send(producerRecord);
    }

//========================================
// INTERNALS
//----------------------------------------

    /**
     * Format the record to send to Kafka, with the needed content and the headers.
     *
     * @param rawContent content to include as the message payload.
     * @param tenantId Tenant ID to include in the message headers.
     * @return ProducerRecord to send to Kafka.
     */
    private ProducerRecord<String, byte[]> formatProducerRecord(byte[] rawContent, String tenantId) {
        List<Header> headers = new LinkedList<>();
        headers.add(new RecordHeader(GrpcConstants.TENANT_ID_KEY, tenantId.getBytes(StandardCharsets.UTF_8)));

        return new ProducerRecord<String, byte[]>(
            kafkaTopic,
            null,
            null,
            rawContent,
            headers
        );
    }
}
