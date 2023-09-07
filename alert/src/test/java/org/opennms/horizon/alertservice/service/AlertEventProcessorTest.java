package org.opennms.horizon.alertservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.Location;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.Node;
import org.opennms.horizon.alertservice.db.entity.PolicyRule;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.LocationRepository;
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.opennms.horizon.alertservice.mapper.AlertMapperImpl;
import org.opennms.horizon.events.proto.Event;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertEventProcessorTest {

    @InjectMocks
    AlertEventProcessor processor;

    @Mock
    AlertRepository alertRepository;

    @Mock
    NodeRepository nodeRepository;
    @Mock
    LocationRepository locationRepository;

    @Mock
    AlertDefinitionRepository alertDefinitionRepository;

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

    @Spy // for InjectMocks
    AlertMapper alertMapper = new AlertMapperImpl();

    @BeforeEach
    void setUp() {
        when(registry.counter(any())).thenReturn(counter);

        processor.init();
    }

    @Test
    void generateAlert() {
        Event event = Event.newBuilder()
            .setTenantId("tenantA")
            .setUei("uei")
            .setDescription("desc")
            .setNodeId(10L)
            .setLocationId("11")
            .setLocationName("locationName")
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
        MonitorPolicy policy = new MonitorPolicy();
        policy.setName("policyName");
        PolicyRule rule = new PolicyRule();
        rule.setName("ruleName");
        rule.setPolicy(policy);
        alertCondition.setRule(rule);

        MonitorPolicy monitorPolicy = new MonitorPolicy();
        monitorPolicy.setId(1L);
        monitorPolicy.setTenantId("tenantA");

        var tag = new Tag();
        tag.getPolicies().add(monitorPolicy);

        when(alertDefinitionRepository.findByTenantIdAndUei(event.getTenantId(), event.getUei()))
            .thenReturn(List.of(alertDefinition));

        when(tagRepository.findByTenantIdAndNodeId(anyString(), anyLong())).thenReturn(List.of(tag));

        var node = new Node();
        node.setId(event.getNodeId());
        node.setNodeLabel("nodeLabel");
        when(nodeRepository.findByIdAndTenantId(event.getNodeId(), event.getTenantId())).thenReturn(Optional.of(node));

        List<Alert> alerts = processor.process(event);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0))
            .returns("tenantA", Alert::getTenantId)
            .returns("desc", Alert::getDescription)
            .returns(alertCondition.getSeverity(), Alert::getSeverity)
            .returns(List.of(monitorPolicy.getId()), Alert::getMonitoringPolicyIdList);

        ArgumentCaptor<Location> saveLocationArg = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository, times(1)).save(saveLocationArg.capture());

        Assert.assertEquals(saveLocationArg.getValue().getLocationName(), event.getLocationName());
        Assert.assertEquals(saveLocationArg.getValue().getId(), Long.parseLong(event.getLocationId()));
        Assert.assertEquals(saveLocationArg.getValue().getTenantId(), event.getTenantId());
    }
}
