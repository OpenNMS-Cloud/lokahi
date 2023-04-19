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
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alertservice.api.AlertLifecyleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A simple engine that stores alerts in memory and periodically scans the list to performs actions (i.e. delete if older than X).
 *
 * This is a very limited and temporary solution, to which we may re-introduce Drools or some alternative to perform.
 */
@Component
@RequiredArgsConstructor
public class AlertEngine implements AlertLifecyleListener {
    private static final Logger LOG = LoggerFactory.getLogger(AlertEngine.class);
    public static final int DURATION = 14;

    private final AlertService alertService;
    private final AlertMapper alertMapper;
    private final AlertRepository alertRepository;

    private final AlertListenerRegistry alertEntityNotifier;
    private final Map<String, Map<String, Alert>> alertsByReductionKeyByTenantId = new ConcurrentHashMap<>();
    private final Timer nextTimer = new Timer();

    @PostConstruct
    @Transactional
    public void init() {
        alertEntityNotifier.addListener(this);
        nextTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    tick();
                } catch (RuntimeException e) {
                    LOG.error("Error happened in tick. Keeping on ticking...", e);
                }
            }
        }, TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(5));
        alertRepository.findAll().forEach(a -> handleNewOrUpdatedAlert(alertMapper.toProto(a)));
    }

    @PreDestroy
    public void destroy() {
        alertEntityNotifier.removeListener(this);
        nextTimer.cancel();
    }

    @Override
    public synchronized void handleNewOrUpdatedAlert(Alert alert) {
        alertsByReductionKeyByTenantId.compute(alert.getTenantId(), (k,v) -> {
            Map<String, Alert> alertsByReductionKey = (v == null) ? new ConcurrentHashMap<>() : v;
            alertsByReductionKey.put(alert.getReductionKey(), alert);
            return alertsByReductionKey;
        });
    }

    @Override
    public synchronized void handleDeletedAlert(Alert alert) {
        Map<String,Alert> alertsByReductionKey = alertsByReductionKeyByTenantId.getOrDefault(alert.getTenantId(), new HashMap<>());
        alertsByReductionKey.compute(alert.getReductionKey(), (k,v) -> {
                if (v == null) {
                    LOG.error("Received delete for alert that was not present in the cache. Alert: {}", alert);
                }
                return null;
            });
        if (alertsByReductionKey.isEmpty()) {
            // Stop tracking tenant
            alertsByReductionKeyByTenantId.remove(alert.getTenantId());
        }
    }

    private synchronized void tick() {
        LOG.debug("Tick with: {}", alertsByReductionKeyByTenantId);
        // Delete alerts more than 2 weeks old
        alertsByReductionKeyByTenantId.forEach((tenantId, alertsByReductionKey) -> {
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()-> {
                alertsByReductionKey.values().stream()
                    .filter(a -> a.getLastUpdateTimeMs() < (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DURATION)))
                    .forEach(this::deleteAlert);
            });
        });
    }

    private void deleteAlert(Alert alert) {
        LOG.info("Delete alert with reduction key: {} for tenant id: {}", alert.getReductionKey(), alert.getTenantId());
        try {
            alertService.deleteByTenantId(alert, alert.getTenantId());
        } catch (EmptyResultDataAccessException ex) {
            LOG.warn("Could not find alert alert with reduction key: {} for tenant id: {}. Will stop tracking.", alert.getReductionKey(), alert.getTenantId());
        }
        // We expect the delete call to the AlertService to issue a callback to our listener, which will remove the entry from the map
    }
}
