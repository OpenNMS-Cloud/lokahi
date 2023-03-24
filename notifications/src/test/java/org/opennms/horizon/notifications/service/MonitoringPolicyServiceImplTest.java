package org.opennms.horizon.notifications.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.notifications.mapper.MonitoringPolicyMapper;
import org.opennms.horizon.notifications.repository.MonitoringPolicyRepository;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.argThat;

@RunWith(MockitoJUnitRunner.class)
public class MonitoringPolicyServiceImplTest {

    @InjectMocks
    MonitoringPolicyService monitoringPolicyService;

    @Mock
    MonitoringPolicyRepository monitoringPolicyRepository;

    @Spy
    MonitoringPolicyMapper monitoringPolicyMapper = Mappers.getMapper(MonitoringPolicyMapper.class);

    @Test
    public void savePolicy() {
        MonitorPolicyProto proto = MonitorPolicyProto.newBuilder()
            .setId(1)
            .setTenantId("tenant")
            .setNotifyByPagerDuty(true)
            .setNotifyByEmail(false)
            .setNotifyByWebhooks(false)
            .build();
        monitoringPolicyService.saveMonitoringPolicy(proto);

        Mockito.verify(monitoringPolicyRepository, Mockito.times(1)).save(argThat((arg) -> {
            assertEquals(1, arg.getId());
            assertEquals("tenant", arg.getTenantId());
            assertTrue(arg.isNotifyByPagerDuty());
            assertFalse(arg.isNotifyByEmail());
            assertFalse(arg.isNotifyByWebhooks());

            return true;
        }));
    }
}
