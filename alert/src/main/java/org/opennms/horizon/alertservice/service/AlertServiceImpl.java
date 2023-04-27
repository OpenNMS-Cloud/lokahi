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

import java.util.Date;
import java.util.Optional;

import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.api.AlertLifecyleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.entity.Node;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.mapper.NodeMapper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

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
    public Optional<Alert> reduceEvent(Event e) {
        Optional<Alert> alert = alertEventProcessor.process(e);
        alert.ifPresent(value -> alertListenerRegistry.forEachListener((l) -> l.handleNewOrUpdatedAlert(value)));
        return alert;
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
    public void addListener(AlertLifecyleListener listener) {
        alertListenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(AlertLifecyleListener listener) {
        alertListenerRegistry.addListener(listener);
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
