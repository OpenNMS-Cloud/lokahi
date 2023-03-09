package org.opennms.horizon.notifications.kafka;

import io.grpc.Context;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.grpc.config.TenantLookup;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.notifications.tenant.TenantContext;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class AlarmKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(AlarmKafkaConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
        topics = "${horizon.kafka.alarms.topic}",
        concurrency = "${horizon.kafka.alarms.concurrency}"
    )
    @TenantAwareKafkaListener(skipOnMissing = true)
    public void consume(@Payload AlarmDTO alarm, @Headers Map<String, Object> headers) {
        LOG.info("Received alarm from kafka {}", alarm);
        consumeAlarm(alarm);
    }

    public void consumeAlarm(AlarmDTO alarm){
        try {
            notificationService.postNotification(alarm);
        } catch (NotificationException e) {
            LOG.error("Exception sending alarm to PagerDuty.", e);
        }
    }
}
