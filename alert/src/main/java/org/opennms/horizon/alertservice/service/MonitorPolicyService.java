/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.alertservice.service;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.PolicyRuleProto;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.EventDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.SystemPolicyTag;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.PolicyRuleRepository;
import org.opennms.horizon.alertservice.db.repository.SystemPolicyTagRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.opennms.horizon.alertservice.service.routing.TagOperationProducer;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorPolicyService {
    public static final String SYSTEM_TENANT = "system-tenant";
    public static final String DEFAULT_POLICY = "default_policy";

    private final MonitorPolicyMapper policyMapper;
    private final MonitorPolicyRepository repository;
    private final SystemPolicyTagRepository systemPolicyTagRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final AlertDefinitionRepository definitionRepo;
    private final AlertRepository alertRepository;

    private final TagRepository tagRepository;
    private final TagOperationProducer tagOperationProducer;

    private void validatePolicyName(MonitorPolicyProto request, String tenantId) {
        if (StringUtils.isBlank(request.getName())) {
            throw new IllegalArgumentException("Policy name is Blank");
        }
        if (repository.findByNameAndTenantId(request.getName(), tenantId).isPresent()) {
            throw new IllegalArgumentException("Duplicate monitoring policy with name " + request.getName());
        }
    }

    private void validateRuleName(MonitorPolicyProto request) {
        var duplicatedRules =
                request.getRulesList().stream()
                        .collect(Collectors.groupingBy(PolicyRuleProto::getName))
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().size() > 1)
                        .toList();
        if (!duplicatedRules.isEmpty()) {
            throw new IllegalArgumentException("Duplicate monitoring rule with name "
                    + duplicatedRules.stream().map(Map.Entry::getKey).collect(Collectors.joining(", ")));
        }
        if (request.getRulesList().stream().anyMatch(rule -> StringUtils.isBlank(rule.getName()))) {
            throw new IllegalArgumentException("Rule name is blank");
        }
    }

    @Transactional
    public MonitorPolicyProto createPolicy(MonitorPolicyProto request, String tenantId) {
        if (tenantId.isEmpty()) {
            throw new IllegalArgumentException("Missing tenantId");
        }
        validateRuleName(request);
        if (request.hasField(
                MonitorPolicyProto.getDescriptor().findFieldByNumber(MonitorPolicyProto.ID_FIELD_NUMBER))) {
            Optional<MonitorPolicy> policy;
            if (DEFAULT_POLICY.equals(request.getName())) {
                policy = getDefaultPolicy(tenantId);
            } else {
                policy = repository.findByIdAndTenantId(request.getId(), tenantId);
            }
            if (policy.isEmpty()) {
                String message = String.format("policy not found by id %s for tenant %s", request.getId(), tenantId);
                log.warn(message);
                throw new IllegalArgumentException(message);
            } else if (!policy.get().getName().equals(request.getName())) {
                validatePolicyName(request, tenantId);
            }
        } else if (!DEFAULT_POLICY.equals(request.getName())) {
            validatePolicyName(request, tenantId);
        }

        MonitorPolicy policy = policyMapper.map(request);
        if (DEFAULT_POLICY.equals(request.getName())) {
            return handleDefaultTagOperationUpdate(policy.getTags(), tenantId);
        } else {
            updateData(policy, tenantId);
            MonitorPolicy newPolicy = repository.save(policy);
            createAlertDefinitionFromPolicy(newPolicy);
            var existingTags = tagRepository.findByTenantIdAndPolicyId(newPolicy.getTenantId(), newPolicy.getId());
            var tags = updateTags(newPolicy, policy.getTags());
            newPolicy.setTags(tags);
            handleTagOperationUpdate(existingTags, tags);
            return policyMapper.map(newPolicy);
        }
    }

    private MonitorPolicyProto handleDefaultTagOperationUpdate(Set<Tag> requestedNewTags, final String tenantId) {
        var optional = getDefaultPolicy(tenantId);
        if (optional.isEmpty()) {
            throw new NotFoundException("Default policy not found");
        }
        final var defaultPolicy = optional.get();
        final var existingTags = new ArrayList<>(defaultPolicy.getTags());
        final var newTags = new HashSet<Tag>();
        final var existingTagMap = existingTags.stream().collect(Collectors.toMap(Tag::getName, Function.identity()));

        requestedNewTags.forEach(tag -> {
            SystemPolicyTag defaultPolicyTag;
            var existingTag = tagRepository.findByTenantIdAndName(tenantId, tag.getName());
            var matchedTag = existingTagMap.get(tag.getName());
            if (matchedTag == null || !tenantId.equals(matchedTag.getTenantId())) {
                Tag updatedTag;
                if (existingTag.isEmpty()) {
                    tag.setPolicies(new HashSet<>());
                    tag.setTenantId(tenantId);
                    updatedTag = tagRepository.save(tag);
                } else {
                    updatedTag = existingTag.get();
                }
                defaultPolicyTag = new SystemPolicyTag(tenantId, defaultPolicy.getId(), updatedTag);
                defaultPolicyTag = systemPolicyTagRepository.save(defaultPolicyTag);
                defaultPolicy.getTags().add(defaultPolicyTag.getTag());
                newTags.add(updatedTag);
            } else {
                newTags.add(matchedTag);
            }
        });

        var removedTags = new HashSet<>(Sets.difference(defaultPolicy.getTags(), newTags));
        removedTags.forEach(tag -> {
            defaultPolicy.getTags().remove(tag);
            if (SYSTEM_TENANT.equals(tag.getTenantId())) {
                return;
            }
            systemPolicyTagRepository.deleteById(
                    new SystemPolicyTag.RelationshipId(tenantId, defaultPolicy.getId(), tag));
        });
        if (!newTags.isEmpty()) {
            systemPolicyTagRepository.deleteEmptyTagByTenantIdAndPolicyId(tenantId, defaultPolicy.getId());
        } else if (!removedTags.isEmpty()) {
            var systemPolicyTag = new SystemPolicyTag(tenantId, defaultPolicy.getId(), null);
            systemPolicyTagRepository.save(systemPolicyTag);
        }

        existingTags.forEach(t -> {
            if (SYSTEM_TENANT.equals(t.getTenantId())) {
                t.setTenantId(tenantId);
            }
        });
        handleTagOperationUpdate(existingTags, newTags);

        return policyMapper.map(defaultPolicy);
    }

    private void handleTagOperationUpdate(List<Tag> existingTags, Set<Tag> newTags) {
        var oldTags = new HashSet<>(existingTags);
        var removedTags = Sets.difference(oldTags, newTags);
        var addedTags = Sets.difference(newTags, oldTags);
        var tagOperationUpdates = TagOperationList.newBuilder();
        addedTags.forEach(tag -> {
            var tagAddOp = TagOperationProto.newBuilder()
                    .setOperation(Operation.ASSIGN_TAG)
                    .setTagName(tag.getName())
                    .setTenantId(tag.getTenantId());
            tag.getPolicies().forEach(monitorPolicy -> tagAddOp.addMonitoringPolicyId(monitorPolicy.getId()));
            tagOperationUpdates.addTags(tagAddOp.build());
        });
        removedTags.forEach(tag -> {
            var tagRemoveOp = TagOperationProto.newBuilder()
                    .setOperation(Operation.REMOVE_TAG)
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
            if (persistedTags.stream().noneMatch(persistedTag -> tag.getId().equals(persistedTag.getId()))) {
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
        if (optional.isPresent()) {
            tag = optional.get();
        }

        tag.getPolicies().add(newPolicy);
        return tagRepository.save(tag);
    }

    @Transactional(readOnly = true)
    public List<MonitorPolicyProto> listAll(String tenantId) {
        return repository.findAllByTenantId(tenantId).stream()
                .map(policyMapper::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MonitorPolicyProto> findById(Long id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(policyMapper::map);
    }

    @Transactional(readOnly = true)
    public Optional<MonitorPolicyProto> getDefaultPolicyProto(String tenantId) {
        return getDefaultPolicy(tenantId).map(policyMapper::map);
    }

    private Optional<MonitorPolicy> getDefaultPolicy(String tenantId) {
        return repository.findByNameAndTenantId(DEFAULT_POLICY, SYSTEM_TENANT).map(p -> {
            var systemPolicyTags = systemPolicyTagRepository.findByTenantIdAndPolicyId(tenantId, p.getId());
            var tags = systemPolicyTags.stream()
                    .map(systemPolicyTag -> {
                        if (systemPolicyTag == null) {
                            return null;
                        }
                        return systemPolicyTag.getTag();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!systemPolicyTags.isEmpty()) {
                p.setTags(tags);
            }
            return p;
        });
    }

    @Transactional
    public void deletePolicyById(long id, String tenantId) {
        if (SYSTEM_TENANT.equals(tenantId)) {
            throw new IllegalArgumentException(
                    String.format("Policy with tenantId %s is not allowed to delete.", SYSTEM_TENANT));
        }
        var alerts = alertRepository.findByPolicyIdAndTenantId(id, tenantId);
        if (alerts != null && !alerts.isEmpty()) {
            alertRepository.deleteAll(alerts);
        }
        repository.deleteByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public void deleteRuleById(long id, String tenantId) {
        if (SYSTEM_TENANT.equals(tenantId)) {
            throw new IllegalArgumentException(
                    String.format("Rule with tenantId %s is not allowed to delete.", SYSTEM_TENANT));
        }
        var alerts = alertRepository.findByRuleIdAndTenantId(id, tenantId);
        if (alerts != null && !alerts.isEmpty()) {
            alertRepository.deleteAll(alerts);
        }
        policyRuleRepository.deleteByIdAndTenantId(id, tenantId);
    }

    public long countAlertByPolicyId(long id, String tenantId) {
        return alertRepository.countByPolicyIdAndTenantId(id, tenantId);
    }

    public long countAlertByRuleId(long id, String tenantId) {
        return alertRepository.countByRuleIdAndTenantId(id, tenantId);
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

    private void createAlertDefinitionFromPolicy(MonitorPolicy policy) {
        policy.getRules().forEach(rule -> rule.getAlertConditions().forEach(this::createOrUpdateAlertDefinition));
    }

    private void createOrUpdateAlertDefinition(AlertCondition condition) {
        String uei = condition.getTriggerEvent().getEventUei();
        definitionRepo
                .findFirstByAlertConditionId(condition.getId())
                .ifPresentOrElse(
                        definition -> {
                            if (!uei.equals(definition.getUei())) {
                                log.info("update alert definition for event {} ", condition.getTriggerEvent());
                                definition.setReductionKey(
                                        condition.getTriggerEvent().getReductionKey());
                                definition.setUei(uei);
                                definition.setType(getAlertTypeFromEventDefinition(condition.getTriggerEvent()));
                                definition.setClearKey(
                                        condition.getTriggerEvent().getClearKey());
                                definitionRepo.save(definition);
                            }
                        },
                        () -> {
                            log.info("creating alert definition for event {}", condition.getTriggerEvent());
                            AlertDefinition definition = new AlertDefinition();
                            definition.setUei(uei);
                            definition.setTenantId(condition.getTenantId());
                            definition.setReductionKey(
                                    condition.getTriggerEvent().getReductionKey());
                            definition.setType(getAlertTypeFromEventDefinition(condition.getTriggerEvent()));
                            definition.setClearKey(condition.getTriggerEvent().getClearKey());
                            definition.setAlertCondition(condition);
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
