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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertDefinition;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.model.common.proto.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Date;
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

    private final MeterRegistry registry;

    private Counter eventsWithoutAlertDataCounter;

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

    private AlertData getAlertData(Event event, AlertDefinition alertDefinition) {
        var reductionKey = String.format(alertDefinition.getReductionKey(), event.getTenantId(), event.getUei(), event.getNodeId());
        String clearKey = null;
        if (!Strings.isNullOrEmpty(alertDefinition.getClearKey())) {
            clearKey = String.format(alertDefinition.getClearKey(), event.getTenantId(), event.getNodeId());
        }
        return new AlertData(reductionKey, clearKey, alertDefinition.getType());
    }

    protected org.opennms.horizon.alertservice.db.entity.Alert addOrReduceEventAsAlert(Event event) {
        AlertDefinition alertDef = alertDefinitionRepository.getAlertDefinitionForEvent(event);
        if (alertDef == null) {
            // No alert definition matching, no alert to create
            eventsWithoutAlertDataCounter.increment();
            return null;
        }
        AlertData alertData = getAlertData(event, alertDef);

        org.opennms.horizon.alertservice.db.entity.Alert alert = null;
        if (alertData.clearKey() != null) {
            // If a clearKey is set, determine if there is an existing alert, and reduce onto that one
            alert = alertRepository.findByReductionKey(alertData.clearKey());
            if (alert == null) {
                LOG.debug("No existing alert found with clear key: {}. This is possibly an out-of-order event: {}", alertData.clearKey(), event);
            }
        }

        if (alert == null) {
            // If we didn't find an existing alert to reduce to with the clearKey, the lookup by reductionKey
            alert = alertRepository.findByReductionKey(alertData.reductionKey());
        }

        if (alert == null ) {
            // No existing alert found, create a new one
            alert = createNewAlert(event, alertData);
        } else {
            // Existing alert found, update it
            alert.incrementCount();
            alert.setLastEventId(event.getDatabaseId());
            alert.setType(alertData.type());
            if (AlertType.CLEAR.equals(alert.getType())) {
                // Set the severity to CLEARED when reducing alerts
                alert.setSeverity(Severity.CLEARED);
            } else {
                alert.setSeverity(event.getSeverity());
            }
        }

        // FIXME: If the alert is going to be delete immediately, should we even bother creating it?
        alertRepository.save(alert);
        return alert;
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
        alert.setSeverity(event.getSeverity());
        alert.setEventUei(event.getUei() );
        return alert;
    }

    private record AlertData(String reductionKey, String clearKey, AlertType type) { }

}
