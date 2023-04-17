package org.opennms.horizon.alertservice.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.TriggerEvent;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.TriggerEventRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.events.proto.Event;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AlertEventProcessorTest {

    @InjectMocks
    AlertEventProcessor processor;

    @Mock
    AlertRepository alertRepository;

    @Mock
    AlertDefinitionRepository alertDefinitionRepository;

    @Mock
    TriggerEventRepository triggerEventRepository;

    @Mock
    MonitorPolicyRepository monitorPolicyRepository;

    @Mock
    MeterRegistry registry;

    @Mock
    TenantLookup tenantLookup;

    @Test
    public void generateAlert() {
        Event event = Event.newBuilder()
            .setTenantId("tenantA")
            .setUei("uei")
            .build();

        TriggerEvent triggerEvent = new TriggerEvent();
        triggerEvent.setTenantId("tenantA");
        triggerEvent.setId(1L);
        triggerEvent.setSeverity(Severity.MAJOR);
        triggerEvent.setCount(1);
        triggerEvent.setOvertime(0);

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setTenantId("tenantA");
        alertDefinition.setUei("uei");
        alertDefinition.setReductionKey("reduction");
        alertDefinition.setTriggerEventId(triggerEvent.getId());

        MonitorPolicy monitorPolicy = new MonitorPolicy();
        monitorPolicy.setId(1L);
        monitorPolicy.setTenantId("tenantA");

        Mockito.when(alertDefinitionRepository.findFirstByTenantIdAndUei(event.getTenantId(), event.getUei()))
            .thenReturn(Optional.of(alertDefinition));
        Mockito.when(triggerEventRepository.getReferenceById(triggerEvent.getId()))
            .thenReturn(triggerEvent);
        Mockito.when(monitorPolicyRepository.findMonitoringPolicyByTriggerEvent(triggerEvent.getId()))
            .thenReturn(Optional.of(monitorPolicy));

        Alert alert = processor.addOrReduceEventAsAlert(event);
        assertEquals("tenantA", alert.getTenantId());
        assertEquals(triggerEvent.getSeverity(), alert.getSeverity());
        assertEquals(List.of(monitorPolicy.getId()), alert.getMonitoringPolicyId());
    }
}
