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
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.AlertConditionRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.events.proto.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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
    AlertConditionRepository alertConditionRepository;

    @Mock
    MeterRegistry registry;

    @Mock
    TagRepository tagRepository;

    @Mock
    TenantLookup tenantLookup;

    @Test
    public void generateAlert() {
        Event event = Event.newBuilder()
            .setTenantId("tenantA")
            .setUei("uei")
            .build();

        AlertCondition alertCondition = new AlertCondition();
        alertCondition.setTenantId("tenantA");
        alertCondition.setId(1L);
        alertCondition.setSeverity(Severity.MAJOR);
        alertCondition.setCount(1);
        alertCondition.setOvertime(0);

        AlertDefinition alertDefinition = new AlertDefinition();
        alertDefinition.setTenantId("tenantA");
        alertDefinition.setUei("uei");
        alertDefinition.setReductionKey("reduction");
        alertDefinition.setAlertConditionId(alertCondition.getId());

        MonitorPolicy monitorPolicy = new MonitorPolicy();
        monitorPolicy.setId(1L);
        monitorPolicy.setTenantId("tenantA");
        var tag = new Tag();
        tag.setPolicies(new HashSet<>(List.of(monitorPolicy)));
        var tags = new ArrayList<Tag>();
        tags.add(tag);


        Mockito.when(alertDefinitionRepository.findFirstByTenantIdAndUei(event.getTenantId(), event.getUei()))
            .thenReturn(Optional.of(alertDefinition));
        Mockito.when(alertConditionRepository.getReferenceById(alertCondition.getId()))
            .thenReturn(alertCondition);
        Mockito.when(tagRepository.findByTenantIdAndNodeId(Mockito.anyString(), Mockito.anyLong())).thenReturn(tags);

        Alert alert = processor.addOrReduceEventAsAlert(event);
        assertEquals("tenantA", alert.getTenantId());
        assertEquals(alertCondition.getSeverity(), alert.getSeverity());
        assertEquals(List.of(monitorPolicy.getId()), alert.getMonitoringPolicyId());
    }
}
