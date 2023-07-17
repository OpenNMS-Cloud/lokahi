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

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alerts.proto.AlertConditionProto;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionProto;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.EventDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.AlertConditionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.EventDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.mapper.EventDefinitionMapper;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.opennms.horizon.alertservice.service.routing.TagOperationProducer;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorPolicyService {
    protected static final String SYSTEM_TENANT = "system-tenant";
    private static final String DEFAULT_POLICY = "default_policy";
    private static final String DEFAULT_RULE = "default_rule";
    private static final String DEFAULT_TAG = "default";
    private static final String COLD_START_TRAP_NAME = "SNMP Cold Start";
    private static final String WARM_START_TRAP_NAME = "SNMP Warm Start";

    private final MonitorPolicyMapper policyMapper;
    private final MonitorPolicyRepository repository;
    private final AlertDefinitionRepository definitionRepo;
    private final EventDefinitionRepository eventDefinitionRepository;
    private final EventDefinitionMapper eventDefinitionMapper;
    private final AlertConditionRepository alertConditionRepository;
    private final TagRepository tagRepository;
    private final TagOperationProducer tagOperationProducer;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void defaultPolicies() {
        if (repository.findAllByTenantId(SYSTEM_TENANT).isEmpty()) {
            // TODO: Refactor how the EventDefinitions are found
            AlertEventDefinitionProto coldStartTrap = eventDefinitionRepository
                .findByEventTypeAndName(EventType.SNMP_TRAP, COLD_START_TRAP_NAME)
                .map(eventDefinitionMapper::entityToProto).orElseThrow();
            AlertEventDefinitionProto warmStartTrap = eventDefinitionRepository
                .findByEventTypeAndName(EventType.SNMP_TRAP, WARM_START_TRAP_NAME)
                .map(eventDefinitionMapper::entityToProto).orElseThrow();
            AlertConditionProto coldRebootCondition = AlertConditionProto.newBuilder()
                .setTriggerEvent(coldStartTrap)
                .setCount(1)
                .setSeverity(Severity.CRITICAL)
                .build();
            AlertConditionProto warmRebootCondition = AlertConditionProto.newBuilder()
                .setTriggerEvent(warmStartTrap)
                .setCount(1)
                .setSeverity(Severity.MAJOR)
                .build();
            PolicyRuleProto defaultRule = PolicyRuleProto.newBuilder()
                .setName(DEFAULT_RULE)
                .setComponentType(ManagedObjectType.NODE)
                .addAllSnmpEvents(List.of(coldRebootCondition, warmRebootCondition))
                .build();
            MonitorPolicyProto defaultPolicy = MonitorPolicyProto.newBuilder()
                .setName(DEFAULT_POLICY)
                .setMemo("Default SNMP event monitoring policy")
                .setNotifyByEmail(true)
                .setNotifyByPagerDuty(true)
                .setNotifyByWebhooks(true)
                .addRules(defaultRule)
                .addTags(DEFAULT_TAG)
                .setNotifyInstruction("This is default policy notification") //todo: changed to something from environment
                .build();
            createPolicy(defaultPolicy, SYSTEM_TENANT);
        } else {
            createOrUpdateAlertDefinition();
        }
    }

    @Transactional
    public MonitorPolicyProto createPolicy(MonitorPolicyProto request, String tenantId) {
        MonitorPolicy policy = policyMapper.map(request);
        updateData(policy, tenantId);
        MonitorPolicy newPolicy = repository.save(policy);
        createAlertDefinitionFromPolicy(newPolicy);
        var existingTags = tagRepository.findByTenantIdAndPolicyId(newPolicy.getTenantId(), newPolicy.getId());
        var tags = updateTags(newPolicy, policy.getTags());
        newPolicy.setTags(tags);
        handleTagOperationUpdate(existingTags, tags);
        return policyMapper.map(newPolicy);
    }

    private void handleTagOperationUpdate(List<Tag> existingTags, Set<Tag> newTags) {
        var oldTags = new HashSet<>(existingTags);
        var removedTags = Sets.difference(oldTags, newTags);
        var addedTags = Sets.difference(newTags, oldTags);
        var tagOperationUpdates = TagOperationList.newBuilder();
        addedTags.forEach(tag -> {
            var tagAddOp = TagOperationProto.newBuilder().setOperation(Operation.ASSIGN_TAG)
                .setTagName(tag.getName())
                .setTenantId(tag.getTenantId());
            tag.getPolicies().forEach(monitorPolicy -> tagAddOp.addMonitoringPolicyId(monitorPolicy.getId()));
            tagOperationUpdates.addTags(tagAddOp.build());
        });
        removedTags.forEach(tag -> {
            var tagRemoveOp = TagOperationProto.newBuilder().setOperation(Operation.REMOVE_TAG)
                .setTagName(tag.getName())
                .setTenantId(tag.getTenantId());
            tag.getPolicies().forEach(monitorPolicy -> tagRemoveOp.addMonitoringPolicyId(monitorPolicy.getId()));
            tagOperationUpdates.addTags(tagRemoveOp.build());
        });
        tagOperationProducer.sendTagUpdate(tagOperationUpdates.build());
    }

    private Set<Tag> updateTags(MonitorPolicy newPolicy, Set<Tag> tags) {
        Set<Tag> newTags = new HashSet<>();
        tags.forEach(tag -> newTags.add(updateTag(newPolicy, tag)));
        var existingTags = tagRepository.findByTenantIdAndPolicyId(newPolicy.getTenantId(), newPolicy.getId());
        updateExistingTags(existingTags, newTags);
        return newTags;
    }

    private void updateExistingTags(List<Tag> existingTags, Set<Tag> persistedTags) {
        existingTags.forEach(tag -> {
            if (persistedTags.stream().noneMatch(persistedTag ->
                tag.getId().equals(persistedTag.getId()))) {
                // Delete tag if it got removed and nodeIds are empty
                if (tag.getNodeIds().isEmpty()) {
                    tagRepository.deleteById(tag.getId());
                } else {
                    tag.setPolicies(new HashSet<>());
                    tagRepository.save(tag);
                }
            }
        });
    }

    private Tag updateTag(MonitorPolicy newPolicy, Tag tag) {

        var optional = tagRepository.findByTenantIdAndName(newPolicy.getTenantId(), tag.getName());
        if(optional.isPresent()) {
            tag = optional.get();
        }
        tag.getPolicies().add(newPolicy);
        return tagRepository.save(tag);
    }

    @Transactional(readOnly = true)
    public List<MonitorPolicyProto> listAll(String tenantId) {
        return repository.findAllByTenantId(tenantId)
            .stream().map(policyMapper::map).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MonitorPolicyProto> findById(Long id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
            .map(policyMapper::map);
    }

    @Transactional(readOnly = true)
    public Optional<MonitorPolicyProto> getDefaultPolicy() {
        return repository.findByName(DEFAULT_POLICY)
            .map(policyMapper::map);
    }
    private void updateData(MonitorPolicy policy, String tenantId) {
        policy.setTenantId(tenantId);
        policy.getRules().forEach(r -> {
            r.setTenantId(tenantId);
            r.setPolicy(policy);
            r.getAlertConditions().forEach(e -> {
                e.setTenantId(tenantId);
                e.setRule(r);
            });
        });
        policy.getTags().forEach(tag -> tag.setTenantId(tenantId));
    }

    private void createOrUpdateAlertDefinition() {
        List<AlertCondition> list =  alertConditionRepository.findAll();
        log.info("found {} alert conditions", list.size());
        list.forEach(this::createOrUpdateAlertDefinition);
        definitionRepo.flush();
    }


    private void createAlertDefinitionFromPolicy(MonitorPolicy policy) {
        policy.getRules().forEach(rule -> rule.getAlertConditions()
            .forEach(this::createOrUpdateAlertDefinition));
    }

    private void createOrUpdateAlertDefinition(AlertCondition condition) {
        String uei = condition.getTriggerEvent().getEventUei();
        definitionRepo.findFirstByAlertConditionId(condition.getId())
            .ifPresentOrElse(definition -> {
                if (!uei.equals(definition.getUei())) {
                    log.info("update alert definition for event {} ", condition.getTriggerEvent());
                    definition.setReductionKey(condition.getTriggerEvent().getReductionKey());
                    definition.setUei(uei);
                    definition.setType(getAlertTypeFromEventDefinition(condition.getTriggerEvent()));
                    definition.setClearKey(condition.getTriggerEvent().getClearKey());
                    definitionRepo.save(definition);
                }
            }, ()-> {
                log.info("creating alert definition for event {}", condition.getTriggerEvent());
                AlertDefinition definition = new AlertDefinition();
                definition.setUei(uei);
                definition.setTenantId(condition.getTenantId());
                definition.setReductionKey(condition.getTriggerEvent().getReductionKey());
                definition.setType(getAlertTypeFromEventDefinition(condition.getTriggerEvent()));
                definition.setAlertConditionId(condition.getId());
                definition.setClearKey(condition.getTriggerEvent().getClearKey());
                definitionRepo.save(definition);
            });
    }

    private AlertType getAlertTypeFromEventDefinition(EventDefinition eventDefinition) {
        if (eventDefinition.getClearKey() != null) {
            return AlertType.CLEAR;
        }
        return AlertType.ALARM_TYPE_UNDEFINED;
    }
}
