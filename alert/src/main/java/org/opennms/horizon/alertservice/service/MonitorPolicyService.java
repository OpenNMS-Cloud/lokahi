/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.alertservice.service;

import java.util.List;
import java.util.Optional;

import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alerts.proto.TriggerEventProto;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.TriggerEvent;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.TriggerEventRepository;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.opennms.horizon.alerts.proto.EventType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorPolicyService {
    protected static final String SYSTEM_TENANT = "system-tenant";
    private static final String DEFAULT_POLICY = "default_policy";
    private static final String DEFAULT_RULE = "default_rule";
    private static final String DEFAULT_TAG = "default";
    private static final String UEI_GENERIC_TEMPLATE = "uei.opennms.org/generic/traps/%s"; //%s is the event type
    private static final String REDUCTION_KEY_TEMPLATE = "%s:%s:%d";
    private final MonitorPolicyMapper policyMapper;
    private final MonitorPolicyRepository repository;
    private final AlertDefinitionRepository definitionRepo;
    private final TriggerEventRepository eventRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void defaultPolicies() {
        if(repository.findAllByTenantId(SYSTEM_TENANT).isEmpty()) {
            TriggerEventProto coldReboot = TriggerEventProto.newBuilder()
                .setTriggerEvent(EventType.SNMP_Cold_Start)
                .setCount(1)
                .setSeverity(Severity.CRITICAL)
                .build();
            TriggerEventProto warmReboot = TriggerEventProto.newBuilder()
                .setTriggerEvent(EventType.SNMP_Warm_Start)
                .setCount(1)
                .setSeverity(Severity.MAJOR)
                .build();
            PolicyRuleProto defaultRule = PolicyRuleProto.newBuilder()
                .setName(DEFAULT_RULE)
                .setComponentType(ManagedObjectType.NODE)
                .addAllSnmpEvents(List.of(coldReboot, warmReboot))
                .build();
            MonitorPolicyProto defaultPolicy = MonitorPolicyProto.newBuilder()
                .setName(DEFAULT_POLICY)
                .setMemo("Default SNMP event monitoring policy")
                .addTags(DEFAULT_TAG)
                .setNotifyByEmail(true)
                .setNotifyByPagerDuty(true)
                .setNotifyByWebhooks(true)
                .addRules(defaultRule)
                .setNotifyInstruction("This is default policy notification") //todo: changed to something from environment
                .build();
            createPolicy(defaultPolicy, SYSTEM_TENANT);
        } else {
            createOrUpdateAlertDefinition();
        }
    }

    public MonitorPolicyProto createPolicy(MonitorPolicyProto request, String tenantId) {
        MonitorPolicy policy = policyMapper.map(request);
        updateData(policy, tenantId);
        MonitorPolicy newPolicy = repository.save(policy);
        createAlertDefinitionFromPolicy(newPolicy);
        return policyMapper.entityToProto(newPolicy);
    }

    @Transactional(readOnly = true)
    public List<MonitorPolicyProto> listAll(String tenantId) {
        return repository.findAllByTenantId(tenantId)
            .stream().map(policyMapper::entityToProto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MonitorPolicyProto> findById(Long id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
            .map(policyMapper::entityToProto);
    }

    @Transactional(readOnly = true)
    public Optional<MonitorPolicyProto> getDefaultPolicy() {
        return repository.findByName(DEFAULT_POLICY)
            .map(policyMapper::entityToProto);
    }
    private void updateData(MonitorPolicy policy, String tenantId) {
        policy.setTenantId(tenantId);
        policy.getRules().forEach(r -> {
            r.setTenantId(tenantId);
            r.setPolicy(policy);
            r.getTriggerEvents().forEach(e -> {
                e.setTenantId(tenantId);
                e.setRule(r);
            });
        });
    }

    private void createOrUpdateAlertDefinition() {
        List<TriggerEvent> list =  eventRepository.findAll();
        log.info("found {} events", list.size());
        list.forEach(this::createOrUpdateAlertDefinition);
        definitionRepo.flush();
    }


    private void createAlertDefinitionFromPolicy(MonitorPolicy policy) {
        policy.getRules().forEach(rule -> rule.getTriggerEvents()
            .forEach(this::createOrUpdateAlertDefinition));
    }

    private void createOrUpdateAlertDefinition(TriggerEvent event) {
        String uei = getUeiFromEventType(event.getTriggerEvent());
        definitionRepo.findFirstByTriggerEventId(event.getId())
            .ifPresentOrElse(definition -> {
                if(!uei.equals(definition.getUei())) {
                    log.info("update alert definition for event {} ", event.getTriggerEvent());
                    definition.setUei(uei);
                    definition.setReductionKey(REDUCTION_KEY_TEMPLATE);
                    definition.setType(getAlertTypeFromEventType(event.getTriggerEvent()));
                    setClearKey(definition, event.getClearEvent());
                    definitionRepo.save(definition);
                }
            }, ()-> {
                log.info("creating alert definition for event {}", event.getTriggerEvent());
                AlertDefinition definition = new AlertDefinition();
                definition.setUei(uei);
                definition.setTenantId(event.getTenantId());
                definition.setReductionKey(REDUCTION_KEY_TEMPLATE);
                definition.setType(getAlertTypeFromEventType(event.getTriggerEvent()));
                definition.setTriggerEventId(event.getId());
                setClearKey(definition, event.getClearEvent());
                definitionRepo.save(definition);
            });
    }

    private void setClearKey(AlertDefinition definition, EventType clearType) {
        if(clearType != null && !clearType.equals(EventType.UNKNOWN_EVENT) && !clearType.equals(EventType.UNRECOGNIZED)) {
            definition.setClearKey("%s:" + getUeiFromEventType(clearType) + ":%d");
        }
    }

    private String getUeiFromEventType(EventType type) {
        return String.format(UEI_GENERIC_TEMPLATE, type.name());
    }

    private AlertType getAlertTypeFromEventType(EventType eventType) {
        return switch (eventType) {
            case SNMP_Cold_Start, SNMP_Warm_Start -> AlertType.PROBLEM_WITHOUT_CLEAR;
            case SNMP_Link_Down -> AlertType.PROBLEM_WITH_CLEAR;
            case SNMP_Link_Up -> AlertType.CLEAR;
            default -> AlertType.ALARM_TYPE_UNDEFINED;
        };
    }
}
