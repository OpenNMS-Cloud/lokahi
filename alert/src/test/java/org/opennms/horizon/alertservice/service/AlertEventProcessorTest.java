package org.opennms.horizon.alertservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.AlertConditionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.events.proto.Event;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
    ReductionKeyService reductionKeyService;

    @Mock
    MeterRegistry registry;

    @Mock
    TagRepository tagRepository;

    @Mock
    TenantLookup tenantLookup;

    @Mock
    Counter counter;

    @BeforeEach
    void setUp() {
        when(registry.counter(any())).thenReturn(counter);

        processor.init();
    }

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
        alertDefinition.setAlertCondition(alertCondition);

        MonitorPolicy monitorPolicy = new MonitorPolicy();
        monitorPolicy.setId(1L);
        monitorPolicy.setTenantId("tenantA");

        var tag = new Tag();
        tag.getPolicies().add(monitorPolicy);

        when(alertDefinitionRepository.findByTenantIdAndUei(event.getTenantId(), event.getUei()))
            .thenReturn(List.of(alertDefinition));

        when(tagRepository.findByTenantIdAndNodeId(anyString(), anyLong())).thenReturn(List.of(tag));

        List<Alert> alerts = processor.addOrReduceEventAsAlert(event);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0))
            .returns("tenantA", Alert::getTenantId)
            .returns(alertCondition.getSeverity(), Alert::getSeverity)
            .returns(List.of(monitorPolicy.getId()), Alert::getMonitoringPolicyId);
    }
}
