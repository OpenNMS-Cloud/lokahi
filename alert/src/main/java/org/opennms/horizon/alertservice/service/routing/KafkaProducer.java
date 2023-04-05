package org.opennms.horizon.alertservice.service.routing;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alertservice.api.AlertLifecyleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;

@Component
@PropertySource("classpath:application.yaml")
public class KafkaProducer implements AlertLifecyleListener {
    public static final String DEFAULT_ALARMS_TOPIC = "new-alerts";

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final AlertService alertService;

    @Value("${kafka.topics.new-alerts:" + DEFAULT_ALARMS_TOPIC + "}")
    private String kafkaTopic;

    @Autowired
    public KafkaProducer(@Qualifier("kafkaProducerTemplate") KafkaTemplate<String, byte[]> kafkaTemplate, AlertService alertService) {
        this.kafkaTemplate = kafkaTemplate;
        this.alertService = Objects.requireNonNull(alertService);
    }

    @PostConstruct
    public void init() {
        alertService.addListener(this);
    }

    @PreDestroy
    public void destroy() {
        alertService.removeListener(this);
    }

    @Override
    public void handleNewOrUpdatedAlert(Alert alert) {
        var producerRecord = new ProducerRecord<>(kafkaTopic, toKey(alert), alert.toByteArray());
        kafkaTemplate.send(producerRecord);
    }

    @Override
    public void handleDeletedAlert(Alert alert) {
        var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, toKey(alert), null);
        kafkaTemplate.send(producerRecord);
    }

    private String toKey(Alert alert) {
        return alert.getTenantId() + "-" + alert.getLocation();
    }
}
