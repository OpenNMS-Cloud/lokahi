package org.opennms.horizon.notifications.kafka;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AlertKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(AlertKafkaConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
        topics = "${horizon.kafka.alerts.topic}",
        concurrency = "${horizon.kafka.alerts.concurrency}"
    )
    public void consume(@Payload byte[] data) {
        try {
            Alert alert = Alert.parseFrom(data);
            if (Strings.isNullOrEmpty(alert.getTenantId())) {
                LOG.warn("TenantId is empty, dropping alert {}", alert);
                return;
            }
            notificationService.postNotification(alert);
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing Alert. Payload: {}", Arrays.toString(data), e);
        }
    }
}
