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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertCount;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.api.AlertLifecycleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.entity.Node;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.db.repository.SeverityCount;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.opennms.horizon.alertservice.mapper.NodeMapper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert =
                alertRepository.findByIdAndTenantId(id, tenantId);
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
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert =
                alertRepository.findByIdAndTenantId(id, tenantId);
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
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert =
                alertRepository.findByIdAndTenantId(id, tenantId);
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
        Optional<org.opennms.horizon.alertservice.db.entity.Alert> dbAlert =
                alertRepository.findByIdAndTenantId(id, tenantId);
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
        long allAlertsCount = alertRepository.countByTenantId(tenantId);
        long acknowledgedAlerts = alertRepository.countByTenantIdAndAcknowledged(tenantId);

        List<SeverityCount> severityCountList = alertRepository.countByTenantIdAndGroupBySeverity(tenantId);
        var countBuilder = AlertCount.newBuilder();
        countBuilder.setAcknowledgedCount(acknowledgedAlerts).setTotalAlertCount(allAlertsCount);
        severityCountList.forEach(severityCount ->
                countBuilder.putCountBySeverity(severityCount.getSeverity().name(), severityCount.getCount()));
        return countBuilder.build();
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

        optNode.ifPresentOrElse(
                node -> {
                    updateNode(node, nodeDTO);
                },
                () -> {
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
