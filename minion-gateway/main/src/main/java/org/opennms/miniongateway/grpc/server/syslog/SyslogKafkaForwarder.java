package org.opennms.miniongateway.grpc.server.syslog;


import org.opennms.horizon.grpc.syslog.contract.TenantLocationSpecificSysLogDTO;
import org.opennms.horizon.grpc.syslog.contract.SyslogMessageLogDTO;
import org.opennms.horizon.shared.grpc.syslog.contract.mapper.TenantLocationSpecificSysLogDTOMapper;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisher;
import org.opennms.miniongateway.grpc.server.kafka.SinkMessageKafkaPublisherFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Forwarder of Trap messages - received via GRPC and forwarded to Kafka.
 */
@Component
public class SyslogKafkaForwarder implements MessageConsumer<SyslogMessageLogDTO, SyslogMessageLogDTO> {

    public static final String DEFAULT_TRAP_RESULTS_TOPIC = "syslog";

    private final SinkMessageKafkaPublisher<SyslogMessageLogDTO, TenantLocationSpecificSysLogDTO> kafkaPublisher;

    @Autowired
    public SyslogKafkaForwarder(
        SinkMessageKafkaPublisherFactory messagePublisherFactory,
        TenantLocationSpecificSysLogDTOMapper mapper,
        @Value("${syslog.results.kafka-topic:" + DEFAULT_TRAP_RESULTS_TOPIC + "}") String kafkaTopic) {
        this.kafkaPublisher = messagePublisherFactory.create(mapper::mapBareToTenanted, kafkaTopic);
    }

    @Override
    public SinkModule<SyslogMessageLogDTO, SyslogMessageLogDTO> getModule() {
        return new SyslogSinkModule();
    }

    @Override
    public void handleMessage(SyslogMessageLogDTO message) {
        this.kafkaPublisher.send(message);
    }
}
