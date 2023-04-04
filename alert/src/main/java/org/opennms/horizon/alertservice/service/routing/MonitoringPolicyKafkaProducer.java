package org.opennms.horizon.alertservice.service.routing;

import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostPersist;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MonitoringPolicyKafkaProducer {
    @Value("${kafka.topics.monitoring-policy}")
    private String topic;

    @Autowired
    @Qualifier("kafkaProducerTemplate")
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @PostUpdate
    @PostPersist
    public void sendMonitoringPolicy(MonitorPolicy monitorPolicy) {
        // Not all fields are included in this proto, since the Notification service doesn't care about all of them.
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder()
            .setId(monitorPolicy.getId())
            .setTenantId(monitorPolicy.getTenantId())
            .setNotifyByEmail(monitorPolicy.getNotifyByEmail())
            .setNotifyByWebhooks(monitorPolicy.getNotifyByWebhooks())
            .setNotifyByPagerDuty(monitorPolicy.getNotifyByPagerDuty())
            .build();

        var record = new ProducerRecord<String, byte[]>(topic, proto.toByteArray());
        kafkaTemplate.send(record);
    }
}

