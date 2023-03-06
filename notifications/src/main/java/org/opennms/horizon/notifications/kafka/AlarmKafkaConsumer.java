package org.opennms.horizon.notifications.kafka;

import io.grpc.Context;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.service.NotificationService;
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
    public void consume(@Payload AlarmDTO alarm, @Headers Map<String, Object> headers) {

        LOG.info("Received alarm from kafka {}", alarm);
        Optional<String> tenantOptional = getTenantId(headers);
        if (tenantOptional.isEmpty()) {
           LOG.warn("TenantId is empty, dropping alarm {}", alarm);
           return;
        }
        String tenantId = tenantOptional.get();
        LOG.info("Received tenantIda {}", tenantId);

        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            consumeAlarm(alarm);
        });
    }

    public void consumeAlarm(AlarmDTO alarm){
        try {
            notificationService.postNotification(alarm);
        } catch (NotificationException e) {
            LOG.error("Exception sending alarm to PagerDuty.", e);
        }
    }

    private Optional<String> getTenantId(Map<String, Object> headers) {
        Object tenantId = headers.get(GrpcConstants.TENANT_ID_KEY);
        if (tenantId instanceof byte[]) {
            return Optional.of(new String((byte[]) tenantId));
        }
        return Optional.empty();
    }
}
