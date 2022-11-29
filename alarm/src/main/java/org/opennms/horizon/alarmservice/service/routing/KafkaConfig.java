package org.opennms.horizon.alarmservice.service.routing;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    @Value("${KAFKA.BOOTSTRAP.SERVERS:kafka:9092}")
    private String bootstrapAddress;

    @Bean
    public ProducerFactory<String, Alarm> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapAddress);
        configProps.put(
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        configProps.put(
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            ByteArraySerializer.class);
        configProps.put(
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
            false); // FIXME disabled to work with KRaft, but should it stay disabled?
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean("kafkaAlarmProducerTemplate")
    public KafkaTemplate<String, Alarm> kafkaTemplate(
        @Autowired ProducerFactory<String, Alarm> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
