package org.opennms.horizon.notifications.kafka;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.notifications.service.MonitoringPolicyService;
import org.opennms.horizon.notifications.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;

import java.util.Arrays;

@Service
public class MonitoringPolicyKafkaConsumer {
    private final Logger LOG = LoggerFactory.getLogger(MonitoringPolicyKafkaConsumer.class);

    @Autowired
    private MonitoringPolicyService monitoringPolicyService;

    @KafkaListener(
        topics = "${horizon.kafka.monitoring-policy.topic}",
        concurrency = "${horizon.kafka.monitoring-policy.concurrency}"
    )
    public void consume(@Payload byte[] data) {
        try {
            MonitorPolicyProto monitoringPolicyProto = MonitorPolicyProto.parseFrom(data);
            if (Strings.isNullOrEmpty(monitoringPolicyProto.getTenantId())) {
                LOG.warn("TenantId is empty, dropping alert {}", monitoringPolicyProto);
                return;
            }
            monitoringPolicyService.saveMonitoringPolicy(monitoringPolicyProto);
        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error while parsing Monitoring Policy. Payload: {}", Arrays.toString(data), e);
        }
    }
}
