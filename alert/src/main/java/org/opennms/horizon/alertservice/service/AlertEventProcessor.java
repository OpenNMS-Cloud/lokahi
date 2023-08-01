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

import io.grpc.Context;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.ThresholdedEvent;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.db.repository.ThresholdedEventRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.opennms.horizon.events.proto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Used to process/reduce events to alerts.
 */
@Service
@RequiredArgsConstructor
public class AlertEventProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AlertEventProcessor.class);

    private final AlertRepository alertRepository;

    private final AlertMapper alertMapper;

    private final AlertDefinitionRepository alertDefinitionRepository;

    private final ThresholdedEventRepository thresholdedEventRepository;

    private final ReductionKeyService reductionKeyService;

    private final TagRepository tagRepository;

    private final MeterRegistry registry;

    private final TenantLookup tenantLookup;
    private Counter eventsWithoutAlertDataCounter;


    @PostConstruct
    public void init() {
        eventsWithoutAlertDataCounter = registry.counter("events_without_alert_data_counter");
    }

    @Transactional
    public List<Alert> process(Event e) {
        LOG.trace("Processing event with UEI: {} for tenant id: {}", e.getUei(), e.getTenantId());
        List<org.opennms.horizon.alertservice.db.entity.Alert> dbAlerts = addOrReduceEventAsAlert(e);
        if (dbAlerts.isEmpty()) {
            LOG.debug("No alert returned from processing event with UEI: {} for tenant id: {}", e.getUei(), e.getTenantId());
            return Collections.emptyList();
        }
        return dbAlerts.stream().map(alertMapper::toProto).toList();
    }


    protected List<org.opennms.horizon.alertservice.db.entity.Alert> addOrReduceEventAsAlert(Event event) {
        List<AlertDefinition> alertDefinitions = alertDefinitionRepository.findByTenantIdAndUei(event.getTenantId(), event.getUei());
        if (alertDefinitions.isEmpty()) {
            alertDefinitions = alertDefinitionRepository.findByTenantIdAndUei(MonitorPolicyService.SYSTEM_TENANT, event.getUei());
        }
        if (alertDefinitions.isEmpty()) {
            // No alert definition matching, no alert to create
            eventsWithoutAlertDataCounter.increment();
            return Collections.emptyList();
        }

        List<org.opennms.horizon.alertservice.db.entity.Alert> alerts = alertDefinitions.stream()
            .map(alertDefinition -> getAlertData(event, alertDefinition))
            .map(alertData -> createOrUpdateAlert(event, alertData))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

        // FIXME: If the alert is going to be delete immediately, should we even bother creating it?
        alertRepository.saveAll(alerts);
        return alerts;
    }

    private AlertData getAlertData(Event event, AlertDefinition alertDefinition) {
        String reductionKey = reductionKeyService.renderReductionKey(event, alertDefinition);
        String clearKey = reductionKeyService.renderClearKey(event, alertDefinition);
        AlertCondition alertCondition = alertDefinition.getAlertCondition();
        var tags = tagRepository.findByTenantIdAndNodeId(event.getTenantId(), event.getNodeId());
        List<MonitorPolicy> matchingPolicies = new ArrayList<>();
        tags.forEach(tag -> matchingPolicies.addAll(tag.getPolicies().stream().toList()));
        var policies = matchingPolicies.stream().map(MonitorPolicy::getId).toList();
        return new AlertData(
            reductionKey,
            clearKey,
            alertDefinition.getType(),
            policies,
            alertCondition
        );
    }

    private Optional<org.opennms.horizon.alertservice.db.entity.Alert> createOrUpdateAlert(Event event, AlertData alertData) {
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> queryResult = Optional.empty();
        if (alertData.clearKey() != null) {
            // If a clearKey is set, determine if there is an existing alert, and reduce onto that one
            queryResult = tenantLookup.lookupTenantId(Context.current())
                .flatMap(tenantId -> alertRepository.findByReductionKeyAndTenantId(alertData.clearKey(), tenantId));
            if (queryResult.isEmpty()) {
                LOG.debug("No existing alert found with clear key: {}. This is possibly an out-of-order event: {}", alertData.clearKey(), event);
            }
        }
        if (queryResult.isEmpty()) {
            // If we didn't find an existing alert to reduce to with the clearKey, the lookup by reductionKey
            queryResult = tenantLookup.lookupTenantId(Context.current())
                .flatMap(tenantId -> alertRepository.findByReductionKeyAndTenantId(alertData.reductionKey(), tenantId));
        }

        boolean thresholdMet;
        if (isThresholding(alertData) && !AlertType.CLEAR.equals(alertData.type())) {
            // TODO: (Quote from Jose) We will have to add an option to auto close if rate is no longer met - that will be post FMA.
            // If we don't wish to use SQL, this can be done by passing the ThresholdedEvent to the AlertEngine,
            // using the tick() method to check for expiredEvents.
            // In AlertEngine, need id, tenant, expiryDate. Save in a TreeMap sorted by expiryDate.
            saveThresholdEvent(event.getUei(), alertData, event);
            thresholdMet = isThresholdMet(alertData, event.getTenantId());
        } else {
            thresholdMet = true;
        }

        if (queryResult.isEmpty() && thresholdMet) {
            var newAlert = createNewAlert(event, alertData);
            newAlert.setMonitoringPolicyId(alertData.monitoringPolicyId());
            return Optional.of(newAlert);
        }

        queryResult.ifPresent(alert -> {
            alert.incrementCount();
            if (event.hasField(Event.getDescriptor().findFieldByNumber(Event.DATABASE_ID_FIELD_NUMBER))) {
                alert.setLastEventId(event.getDatabaseId());
            }

            if (event.hasField(Event.getDescriptor().findFieldByNumber(Event.PRODUCED_TIME_MS_FIELD_NUMBER))) {
                alert.setLastEventTime(new Date(event.getProducedTimeMs()));
            } else {
                alert.setLastEventTime(new Date());
            }

            alert.setType(alertData.type());
            if (AlertType.CLEAR.equals(alert.getType())) {
                // Set the severity to CLEARED when reducing alerts
                alert.setSeverity(Severity.CLEARED);
            } else {
                alert.setSeverity(alertData.alertCondition().getSeverity());
            }
            alert.setMonitoringPolicyId(alertData.monitoringPolicyId());
        });

        return queryResult;
    }

    private boolean isThresholdMet(AlertData alertData, String tenantId) {
        Date current = new Date();

        int currentCount = thresholdedEventRepository.countByReductionKeyAndTenantIdAndExpiryTimeGreaterThanEqual(alertData.reductionKey(), tenantId, current);
        return alertData.alertCondition().getCount() <= currentCount;
    }

    private void saveThresholdEvent(String uei, AlertData alertData, Event event) {
        Date current = new Date(event.getProducedTimeMs());
        Date expired = calculateExpiry(current, alertData, true);

        ThresholdedEvent thresholdedEvent = new ThresholdedEvent();
        thresholdedEvent.setEventUei(uei);
        thresholdedEvent.setReductionKey(alertData.reductionKey());
        thresholdedEvent.setTenantId(event.getTenantId());
        thresholdedEvent.setCreateTime(current);
        thresholdedEvent.setExpiryTime(expired);

        thresholdedEventRepository.save(thresholdedEvent);
    }

    private Date calculateExpiry(Date current, AlertData alertData, boolean future) {
        Instant curr = current.toInstant();
        Duration dur = Duration.ZERO;
        if (alertData.alertCondition().getOvertime() == 0) {
            dur = Duration.ofDays(365 * 1000);
        } else {
            switch (alertData.alertCondition().getOvertimeUnit()) {
                case HOUR -> dur = Duration.ofHours(alertData.alertCondition().getOvertime());
                case MINUTE -> dur = Duration.ofMinutes(alertData.alertCondition().getOvertime());
                case SECOND -> dur = Duration.ofSeconds(alertData.alertCondition().getOvertime());
            }
        }

        Instant expiry;
        if (future) {
            expiry = curr.plus(dur);
        } else {
            expiry = curr.minus(dur);
        }
        return Date.from(expiry);
    }

    private boolean isThresholding(AlertData alertData) {
        return (alertData.alertCondition().getCount() > 1 || alertData.alertCondition().getOvertime() > 0);
    }

    private org.opennms.horizon.alertservice.db.entity.Alert createNewAlert(Event event, AlertData alertData) {
        org.opennms.horizon.alertservice.db.entity.Alert alert = new org.opennms.horizon.alertservice.db.entity.Alert();
        alert.setTenantId(event.getTenantId());
        alert.setType(alertData.type());
        alert.setReductionKey(alertData.reductionKey());
        alert.setClearKey(alertData.clearKey());
        alert.setCounter(1L);
        alert.setDescription(event.getDescription());
        alert.setLogMessage(event.getLogMessage());

        if (event.getNodeId() > 0) {
            alert.setManagedObjectType(ManagedObjectType.NODE);
            alert.setManagedObjectInstance(Long.toString(event.getNodeId()));
        } else {
            alert.setManagedObjectType(ManagedObjectType.UNDEFINED);
        }

        // FIXME: We should be using the source time of the event and not the time at which it was produced
        if (event.hasField(Event.getDescriptor().findFieldByNumber(Event.PRODUCED_TIME_MS_FIELD_NUMBER))) {
            alert.setFirstEventTime(new Date(event.getProducedTimeMs()));
        } else {
            alert.setFirstEventTime(new Date());
        }

        alert.setLastEventTime(alert.getFirstEventTime());

        if (event.hasField(Event.getDescriptor().findFieldByNumber(Event.DATABASE_ID_FIELD_NUMBER))) {
            alert.setLastEventId(event.getDatabaseId());
        }

        alert.setSeverity(alertData.alertCondition().getSeverity());
        alert.setEventUei(event.getUei());
        alert.setAlertCondition(alertData.alertCondition());
        return alert;
    }

    private record AlertData(
        String reductionKey,
        String clearKey,
        AlertType type,
        List<Long> monitoringPolicyId,
        AlertCondition alertCondition
    ) {
    }
}
