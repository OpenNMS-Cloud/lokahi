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

import io.grpc.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alertservice.api.AlertLifecycleListener;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A simple engine that stores alerts in memory and periodically scans the list to performs actions (i.e. delete if older than X).
 *
 * This is a very limited and temporary solution, to which we may re-introduce Drools or some alternative to perform.
 */
@Component
@RequiredArgsConstructor
public class AlertEngine implements AlertLifecycleListener {
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
        nextTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            tick();
                        } catch (RuntimeException e) {
                            LOG.error("Error happened in tick. Keeping on ticking...", e);
                        }
                    }
                },
                TimeUnit.SECONDS.toMillis(5),
                TimeUnit.SECONDS.toMillis(5));
        alertRepository.findAll().forEach(a -> handleNewOrUpdatedAlert(alertMapper.toProto(a)));
    }

    @PreDestroy
    public void destroy() {
        alertEntityNotifier.removeListener(this);
        nextTimer.cancel();
    }

    @Override
    public synchronized void handleNewOrUpdatedAlert(Alert alert) {
        alertsByReductionKeyByTenantId.compute(alert.getTenantId(), (k, v) -> {
            Map<String, Alert> alertsByReductionKey = (v == null) ? new ConcurrentHashMap<>() : v;
            alertsByReductionKey.put(alert.getReductionKey(), alert);
            return alertsByReductionKey;
        });
    }

    @Override
    public synchronized void handleDeletedAlert(Alert alert) {
        Map<String, Alert> alertsByReductionKey =
                alertsByReductionKeyByTenantId.getOrDefault(alert.getTenantId(), new HashMap<>());
        alertsByReductionKey.compute(alert.getReductionKey(), (k, v) -> {
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
            Context.current()
                    .withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId)
                    .run(() -> {
                        alertsByReductionKey.values().stream()
                                .filter(a -> a.getLastUpdateTimeMs()
                                        < (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DURATION)))
                                .forEach(this::deleteAlert);
                    });
        });
    }

    private void deleteAlert(Alert alert) {
        LOG.info("Delete alert with reduction key: {} for tenant id: {}", alert.getReductionKey(), alert.getTenantId());
        try {
            alertService.deleteByTenantId(alert, alert.getTenantId());
        } catch (EmptyResultDataAccessException ex) {
            LOG.warn(
                    "Could not find alert alert with reduction key: {} for tenant id: {}. Will stop tracking.",
                    alert.getReductionKey(),
                    alert.getTenantId());
        }
        // We expect the delete call to the AlertService to issue a callback to our listener, which will remove the
        // entry from the map
    }
}
