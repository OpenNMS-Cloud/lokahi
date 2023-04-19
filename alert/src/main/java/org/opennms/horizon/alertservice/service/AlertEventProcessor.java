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

import com.google.common.base.Strings;
import io.grpc.Context;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.OverTimeUnit;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.db.entity.AlertDefinition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.ThresholdedEvent;
import org.opennms.horizon.alertservice.db.entity.TriggerEvent;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.ThresholdedEventRepository;
import org.opennms.horizon.alertservice.db.repository.TriggerEventRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.events.proto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private final TriggerEventRepository triggerEventRepository;
    private final ThresholdedEventRepository thresholdedEventRepository;

    private final MonitorPolicyRepository monitorPolicyRepository;

    private final MeterRegistry registry;

    private Counter eventsWithoutAlertDataCounter;

    private final TenantLookup tenantLookup;

    @PostConstruct
    public void init() {
        eventsWithoutAlertDataCounter = registry.counter("events_without_alert_data_counter");
    }

    @Transactional
    public Optional<Alert> process(Event e) {
        LOG.trace("Processing event with UEI: {} for tenant id: {}", e.getUei(), e.getTenantId());
        org.opennms.horizon.alertservice.db.entity.Alert dbAlert = addOrReduceEventAsAlert(e);
        if (dbAlert == null) {
            LOG.debug("No alert returned from processing event with UEI: {} for tenant id: {}", e.getUei(), e.getTenantId());
            return Optional.empty();
        }
        return Optional.of(alertMapper.toProto(dbAlert));
    }

    private @Nullable AlertData getAlertData(Event event, AlertDefinition alertDefinition) {
        var reductionKey = String.format(alertDefinition.getReductionKey(), event.getTenantId(), event.getUei(), event.getNodeId());
        String clearKey = null;
        if (!Strings.isNullOrEmpty(alertDefinition.getClearKey())) {
            clearKey = String.format(alertDefinition.getClearKey(), event.getTenantId(), event.getNodeId());
        }
        TriggerEvent triggerEvent = triggerEventRepository.getReferenceById(alertDefinition.getTriggerEventId());
        // TODO HS-1485: An alert could match multiple monitoring policies, each having their own notifications.
        Optional<MonitorPolicy> policy = monitorPolicyRepository.findMonitoringPolicyByTriggerEvent(triggerEvent.getId());
        List<Long> policies = new ArrayList<>();
        policy.ifPresent(monitorPolicy -> policies.add(monitorPolicy.getId()));
        return new AlertData(
            reductionKey,
            clearKey,
            alertDefinition.getType(),
            triggerEvent.getSeverity(),
            triggerEvent.getCount(),
            triggerEvent.getOvertime(),
            triggerEvent.getOvertimeUnit(),
            policies
        );
    }

    protected @Nullable org.opennms.horizon.alertservice.db.entity.Alert addOrReduceEventAsAlert(Event event) {
        Optional<AlertDefinition> alertDefOpt = alertDefinitionRepository.findFirstByTenantIdAndUei(event.getTenantId(), event.getUei());
        if (alertDefOpt.isEmpty()) {
            alertDefOpt = alertDefinitionRepository.findFirstByTenantIdAndUei(MonitorPolicyService.SYSTEM_TENANT, event.getUei());
        }
        if (alertDefOpt.isEmpty()) {
            // No alert definition matching, no alert to create
            eventsWithoutAlertDataCounter.increment();
            return null;
        }
        AlertData alertData = getAlertData(event, alertDefOpt.get());

        Optional<org.opennms.horizon.alertservice.db.entity.Alert> optionalAlert = Optional.empty();
        if (alertData.clearKey() != null) {
            // If a clearKey is set, determine if there is an existing alert, and reduce onto that one
            optionalAlert = tenantLookup.lookupTenantId(Context.current())
                .map(tenantId -> alertRepository.findByReductionKeyAndTenantId(alertData.clearKey(), tenantId));
            if (optionalAlert.isEmpty()) {
                LOG.debug("No existing alert found with clear key: {}. This is possibly an out-of-order event: {}", alertData.clearKey(), event);
            }
        }
        if (optionalAlert.isEmpty()) {
            // If we didn't find an existing alert to reduce to with the clearKey, the lookup by reductionKey
            optionalAlert = tenantLookup.lookupTenantId(Context.current())
                .map(tenantId -> alertRepository.findByReductionKeyAndTenantId(alertData.reductionKey(), tenantId));
        }

        boolean thresholding = isThresholding(alertData);
        boolean thresholdMet = true;
        if (thresholding) {
            if (!AlertType.CLEAR.equals(alertData.type())) {
                // TODO: (Quote from Jose) We will have to add an option to auto close if rate is no longer met - that will be post FMA.
                // If we don't wish to use SQL, this can be done by passing the ThresholdedEvent to the AlertEngine,
                // using the tick() method to check for expiredEvents.
                // In AlertEngine, need id, tenant, expiryDate. Save in a TreeMap sorted by expiryDate.
                saveThresholdEvent(event.getUei(), alertData, event);
                thresholdMet = isThresholdMet(alertData, event.getTenantId());
            }
        }

        org.opennms.horizon.alertservice.db.entity.Alert alert = null;
        if (optionalAlert.isEmpty()) {
            // No existing alert found, create a new one
            if (thresholdMet) {
                alert = createNewAlert(event, alertData);
            } else {
                return null;
            }
        } else {
            // Existing alert found, update it
            alert = optionalAlert.get();
            alert.incrementCount();
            alert.setLastEventId(event.getDatabaseId());
            alert.setType(alertData.type());
            if (AlertType.CLEAR.equals(alert.getType())) {
                // Set the severity to CLEARED when reducing alerts
                alert.setSeverity(Severity.CLEARED);
            } else {
                alert.setSeverity(alertData.severity());
            }
        }
        alert.setMonitoringPolicyId(alertData.monitoringPolicyId());

        // FIXME: If the alert is going to be delete immediately, should we even bother creating it?
        alertRepository.save(alert);
        return alert;
    }

    private boolean isThresholdMet(AlertData alertData, String tenantId) {
        Date current = new Date();

        int currentCount = thresholdedEventRepository.countByReductionKeyAndTenantIdAndExpiryTimeGreaterThanEqual(alertData.reductionKey(), tenantId, current);
        return alertData.count() <= currentCount;
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
        if (alertData.overTime() == 0) {
            dur = Duration.ofDays(365 * 1000);
        } else {
            switch (alertData.overTimeUnit) {
                case HOUR -> dur = Duration.ofHours(alertData.overTime());
                case MINUTE -> dur = Duration.ofMinutes(alertData.overTime());
                case SECOND -> dur = Duration.ofSeconds(alertData.overTime());
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
        return (alertData.count() > 1 || alertData.overTime() > 0);
    }

    private org.opennms.horizon.alertservice.db.entity.Alert createNewAlert(Event event, AlertData alertData) {
        org.opennms.horizon.alertservice.db.entity.Alert alert = new org.opennms.horizon.alertservice.db.entity.Alert();
        alert.setTenantId(event.getTenantId());
        alert.setType(alertData.type());
        alert.setReductionKey(alertData.reductionKey());
        alert.setClearKey(alertData.clearKey());
        alert.setCounter(1L);
        if (event.getNodeId() > 0) {
            alert.setManagedObjectType(ManagedObjectType.NODE);
            alert.setManagedObjectInstance(Long.toString(event.getNodeId()));
        } else {
            alert.setManagedObjectType(ManagedObjectType.UNDEFINED);
        }
        // FIXME: We should be using the source time of the event and not the time at which it was produced
        alert.setLastEventTime(new Date(event.getProducedTimeMs()));
        alert.setSeverity(alertData.severity());
        alert.setEventUei(event.getUei());
        return alert;
    }

    private record AlertData(String reductionKey, String clearKey, AlertType type, Severity severity, Integer count,
                             Integer overTime, OverTimeUnit overTimeUnit, List<Long> monitoringPolicyId) {
    }
}
