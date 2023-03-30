package org.opennms.horizon.notifications.kafka;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.notifications.service.MonitoringPolicyService;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringPolicyKafkaConsumerTest {

    @InjectMocks
    MonitoringPolicyKafkaConsumer monitoringPolicyKafkaConsumer;

    @Mock
    MonitoringPolicyService monitoringPolicyService;

    @Test
    public void testDropsInvalidData() {
        monitoringPolicyKafkaConsumer.consume(new byte[10]);
        Mockito.verify(monitoringPolicyService, times(0)).saveMonitoringPolicy(any());
    }

    @Test
    public void testDropsWithoutTenantId() {
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder().build();

        monitoringPolicyKafkaConsumer.consume(proto.toByteArray());
        Mockito.verify(monitoringPolicyService, times(0)).saveMonitoringPolicy(any());
    }

    @Test
    public void testConsume() {
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder().setTenantId("tenant").build();

        monitoringPolicyKafkaConsumer.consume(proto.toByteArray());
        Mockito.verify(monitoringPolicyService, times(1)).saveMonitoringPolicy(eq(proto));
    }
}
