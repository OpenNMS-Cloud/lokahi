/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
 */

package org.opennms.horizon.core.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.opennms.horizon.db.dao.api.MinionDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsMinion;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.events.model.IParm;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.opennms.horizon.minion.echo.ipc.client.LocationAwareEchoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class MinionRpcMonitorManager implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(MinionRpcMonitorManager.class);

    private final LocationAwareEchoClient echoClient;
    private final OnmsMetricsAdapter metricsAdapter;
    private final MinionDao minionDao;
    private final SessionUtils sessionUtils;
    private final EventSubscriptionService eventSubscriptionService;
    private final List<OnmsMinion> minionCache = new ArrayList<>();
    private final ThreadFactory monitorThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("minion-monitor-runner-%d")
        .build();
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        new ScheduledThreadPoolExecutor(10, monitorThreadFactory);

    public MinionRpcMonitorManager(LocationAwareEchoClient echoClient,
                                   OnmsMetricsAdapter metricsAdapter,
                                   SessionUtils sessionUtils,
                                   MinionDao minionDao,
                                   EventSubscriptionService eventSubscriptionService) {
        this.echoClient = echoClient;
        this.metricsAdapter = metricsAdapter;
        this.minionDao = minionDao;
        this.sessionUtils = sessionUtils;
        this.eventSubscriptionService = eventSubscriptionService;
    }

    public void init() {
        LOG.debug("echo client {} metrics adaptor {}", echoClient, metricsAdapter);
        eventSubscriptionService.addEventListener(this);
        List<OnmsMinion> minions = sessionUtils.withReadOnlyTransaction(minionDao::findAll);
        minionCache.addAll(minions);
        minionCache.forEach(minion -> LOG.debug("minion: {}", minion.getId()));
    }

    public void shutdown() {
        eventSubscriptionService.removeEventListener(this);
        scheduledThreadPoolExecutor.shutdown();
    }

    @Override
    public String getName() {
        return "Minion-Monitor-Manager";
    }

    @Override
    public void onEvent(IEvent event) {

        if (event.getUei().equals(EventConstants.MONITORING_SYSTEM_ADDED_UEI)) {
            IParm systemIdParm = event.getParm(EventConstants.PARAM_MONITORING_SYSTEM_ID);
            String systemId = systemIdParm.getValue() != null ? systemIdParm.getValue().getContent() : null;
            if (!Strings.isNullOrEmpty(systemId)) {
                LOG.info("Received event for new Minion with Id {}", systemId);
            }
        }

    }
}
