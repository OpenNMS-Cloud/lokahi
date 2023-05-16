package org.opennms.horizon.alertservice.service.routing;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alertservice.api.AlertLifecycleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.config.KafkaTopicProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;

@Component
public class AlertProducer implements AlertLifecycleListener {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private final AlertService alertService;

    private final String kafkaTopic;

    public AlertProducer(KafkaTemplate<String, byte[]> kafkaTemplate, AlertService alertService, KafkaTopicProperties kafkaTopicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.alertService = Objects.requireNonNull(alertService);
        this.kafkaTopic = kafkaTopicProperties.getAlert();
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
