package org.opennms.horizon.notifications.kafka;

import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AlarmKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(AlarmKafkaConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(
        topics = "${horizon.kafka.alarms.topic}",
        concurrency = "${horizon.kafka.alarms.concurrency}"
    )
    public void consume(AlarmDTO alarm) {
        try {
            notificationService.postNotification(alarm);
        } catch (NotificationException e) {
            LOG.error("Exception sending alarm to PagerDuty.", e);
        }
    }
}
