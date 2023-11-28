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

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertCount;
import org.opennms.horizon.alerts.proto.AlertCountByType;
import org.opennms.horizon.alerts.proto.AlertCountType;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.api.AlertLifecycleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.entity.Node;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.opennms.horizon.alertservice.mapper.NodeMapper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertEventProcessor alertEventProcessor;
    private final AlertRepository alertRepository;
    private final NodeRepository nodeRepository;
    private final AlertListenerRegistry alertListenerRegistry;
    private final AlertMapper alertMapper;
    private final NodeMapper nodeMapper;

    @Override
    public List<Alert> reduceEvent(Event e) {
        List<Alert> alerts = alertEventProcessor.process(e);
        alerts.forEach(value -> alertListenerRegistry.forEachListener((l) -> l.handleNewOrUpdatedAlert(value)));
        return alerts;
    }

    @Override
    @Transactional
    public boolean deleteByIdAndTenantId(long id, String tenantId) {
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert = alertRepository.findById(id);
        if (dbAlert.isEmpty()) {
            return false;
        }

        alertRepository.deleteByIdAndTenantId(id, tenantId);
        Alert alert = alertMapper.toProto(dbAlert.get());
        alertListenerRegistry.forEachListener((l) -> l.handleDeletedAlert(alert));
        return true;
    }

    @Override
    @Transactional
    public void deleteByTenantId(Alert alert, String tenantId) {
        alertRepository.deleteByIdAndTenantId(alert.getDatabaseId(), tenantId);
        alertListenerRegistry.forEachListener((l) -> l.handleDeletedAlert(alert));
    }

    @Override
    @Transactional
    public Optional<Alert> acknowledgeByIdAndTenantId(long id, String tenantId) {
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert = alertRepository.findByIdAndTenantId(id, tenantId);
        if (dbAlert.isEmpty()) {
            return Optional.empty();
        }

        org.opennms.horizon.alertservice.db.entity.Alert alert = dbAlert.get();
        alert.setAcknowledgedAt(new Date());
        alert.setAcknowledgedByUser("me");
        alertRepository.save(alert);
        return Optional.of(alertMapper.toProto(alert));
    }

    @Override
    @Transactional
    public Optional<Alert> unacknowledgeByIdAndTenantId(long id, String tenantId) {
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert = alertRepository.findByIdAndTenantId(id, tenantId);
        if (dbAlert.isEmpty()) {
            return Optional.empty();
        }

        org.opennms.horizon.alertservice.db.entity.Alert alert = dbAlert.get();
        alert.setAcknowledgedAt(null);
        alert.setAcknowledgedByUser(null);
        alertRepository.save(alert);
        return Optional.of(alertMapper.toProto(alert));
    }

    @Override
    @Transactional
    public Optional<Alert> escalateByIdAndTenantId(long id, String tenantId) {
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert = alertRepository.findByIdAndTenantId(id, tenantId);
        if (dbAlert.isEmpty()) {
            return Optional.empty();
        }

        org.opennms.horizon.alertservice.db.entity.Alert alert = dbAlert.get();

        // Check if the current severity is below CRITICAL
        if (alert.getSeverity().ordinal() < Severity.CRITICAL.ordinal()) {
            // Increase severity level by one
            alert.setSeverity(Severity.values()[alert.getSeverity().ordinal() + 1]);
        }
        alertRepository.save(alert);
        return Optional.of(alertMapper.toProto(alert));
    }

    @Override
    @Transactional
    public Optional<Alert> clearByIdAndTenantId(long id, String tenantId) {
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert = alertRepository.findByIdAndTenantId(id, tenantId);
        if (dbAlert.isEmpty()) {
            return Optional.empty();
        }

        org.opennms.horizon.alertservice.db.entity.Alert alert = dbAlert.get();
        alert.setSeverity(Severity.CLEARED);
        alertRepository.save(alert);
        return Optional.of(alertMapper.toProto(alert));
    }

    @Override
    public AlertCount getAlertsCount(String tenantId) {
        long allAlerts = alertRepository.countByTenantId(tenantId);
        long acknowledgedAlerts = alertRepository.countByTenantIdAndAcknowledged(tenantId);
        long unacknowledgedAlerts = alertRepository.countByTenantIdAndUnAcknowledged(tenantId);
        long indeterminateAlerts = alertRepository.countByTenantIdAndSeverity(tenantId, Severity.INDETERMINATE);
        long clearedAlerts = alertRepository.countByTenantIdAndSeverity(tenantId, Severity.CLEARED);
        long alertsByNormal = alertRepository.countByTenantIdAndSeverity(tenantId, Severity.NORMAL);
        long alertCountByWarning = alertRepository.countByTenantIdAndSeverity(tenantId, Severity.WARNING);
        long alertCountByMinor =  alertRepository.countByTenantIdAndSeverity(tenantId, Severity.MINOR);
        long alertCountByMajor = alertRepository.countByTenantIdAndSeverity(tenantId, Severity.MAJOR);
        long alertCountByCritical = alertRepository.countByTenantIdAndSeverity(tenantId, Severity.CRITICAL);

        return AlertCount.newBuilder()
            .addAlertCount(AlertCountByType.newBuilder().setCount(allAlerts).setCountType(AlertCountType.COUNT_ALL).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(acknowledgedAlerts)
                .setCountType(AlertCountType.COUNT_ACKNOWLEDGED).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(unacknowledgedAlerts)
                .setCountType(AlertCountType.COUNT_UNACKNOWLEDGED).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(indeterminateAlerts)
                .setCountType(AlertCountType.COUNT_INDETERMINATE).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(clearedAlerts)
                .setCountType(AlertCountType.COUNT_CLEARED).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(alertsByNormal)
                .setCountType(AlertCountType.COUNT_NORMAL).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(alertCountByWarning)
                .setCountType(AlertCountType.COUNT_WARNING).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(alertCountByMinor)
                .setCountType(AlertCountType.COUNT_MINOR).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(alertCountByMajor)
                .setCountType(AlertCountType.COUNT_MAJOR).build())
            .addAlertCount(AlertCountByType.newBuilder().setCount(alertCountByCritical)
                .setCountType(AlertCountType.COUNT_CRITICAL).build())
            .build();
    }

    @Override
    public void addListener(AlertLifecycleListener listener) {
        alertListenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(AlertLifecycleListener listener) {
        alertListenerRegistry.removeListener(listener);
    }

    @Override
    public void saveNode(NodeDTO nodeDTO) {
        Optional<Node> optNode = nodeRepository.findByIdAndTenantId(nodeDTO.getId(), nodeDTO.getTenantId());

        optNode.ifPresentOrElse(node -> {
            updateNode(node, nodeDTO);
        }, () -> {
            createNode(nodeDTO);
        });
    }

    private void createNode(NodeDTO nodeDTO) {
        Node node = nodeMapper.map(nodeDTO);
        nodeRepository.save(node);
    }

    private void updateNode(Node node, NodeDTO nodeDTO) {
        node.setNodeLabel(nodeDTO.getNodeLabel());
        nodeRepository.save(node);
    }
}
