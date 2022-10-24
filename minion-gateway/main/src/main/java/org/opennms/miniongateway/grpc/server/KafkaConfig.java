package org.opennms.miniongateway.grpc.server;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${KAFKA.BOOTSTRAP.SERVERS:kafka:9092}")
    private String bootstrapAddress;

    @Bean
    public ProducerFactory<String, byte[]> producerFactory() {
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

    @Bean("kafkaByteArrayProducerTemplate")
    public KafkaTemplate<String, byte[]> kafkaTemplate(
        @Autowired ProducerFactory<String, byte[]> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
