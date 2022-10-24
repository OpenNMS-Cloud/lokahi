package org.opennms.netmgt.alarmd.notifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.horizon.alarms.api.DefaultAlarmEntityListener;
import org.opennms.horizon.db.model.OnmsAlarm;
import org.opennms.horizon.db.model.mapper.IAlarmMapper;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

public class NotificationForwarder extends DefaultAlarmEntityListener {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationForwarder.class);
    private static final RateLimitedLog RATE_LIMITED_LOG = RateLimitedLog
        .withRateLimit(LOG)
        .maxRate(5).every(Duration.ofSeconds(30))
        .build();

    private IAlarmMapper alarmMapper;

    private String kafkaBrokers;
    private String notificationKafkaTopic;

    public void setAlarmMapper(IAlarmMapper alarmMapper) {
        this.alarmMapper = alarmMapper;
    }

    public void setKafkaBrokers(String kafkaBrokers) {
        this.kafkaBrokers = kafkaBrokers;
    }

    public void setNotificationKafkaTopic(String notificationKafkaTopic) {
        this.notificationKafkaTopic = notificationKafkaTopic;
    }

    private KafkaProducer<String, String> producer;

    @Override
    public void onAlarmCreated(OnmsAlarm alarm) {
        AlarmDTO alarmDTO = alarmMapper.alarmToAlarmDTO(alarm);
        forwardAlarm(alarmDTO);
    }

    private void forwardAlarm(AlarmDTO alarmDTO) {
        try {
            initProducer();
            String alarm = convertDTO(alarmDTO);
            produceKafkaMessage(String.valueOf(alarmDTO.getId()), alarm);
        } catch (JsonProcessingException ex) {
            RATE_LIMITED_LOG.error("Json exception publishing alarm to notifications");
        }
    }

    private void produceKafkaMessage(String alarmId, String alarm) {
        final var record = new ProducerRecord<>(notificationKafkaTopic, alarmId, alarm);
        LOG.info("Sending alarm id:"+alarmId+" to notifications topic:"+notificationKafkaTopic);
        this.producer.send(record, (meta, ex) -> {
            if (ex != null) {
                RATE_LIMITED_LOG.error("Error publishing alarm to notifications", ex);
            }
        });
    }

    private String convertDTO(AlarmDTO dto) throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(dto);
    }

    public void initProducer() {
        if (producer == null) {
            final var kafkaConfig = new Properties();
            kafkaConfig.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
            kafkaConfig.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
            kafkaConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            kafkaConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
            kafkaConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
            kafkaConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
            kafkaConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
            kafkaConfig.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
            producer = Utils.runWithGivenClassLoader(() -> new KafkaProducer<>(kafkaConfig), KafkaProducer.class.getClassLoader());
        }
    }
}
