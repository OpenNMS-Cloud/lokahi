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

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.opennms.horizon.db.dao.api.MinionDao;
import org.opennms.horizon.db.dao.api.MonitoringLocationDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsMinion;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.events.model.IParm;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.opennms.horizon.minion.echo.ipc.client.LocationAwareEchoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionRpcMonitorManager implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(MinionRpcMonitorManager.class);
    private final static int DEFAULT_MESSAGE_SIZE = 1024;
    private static final String[] LABEL_NAMES = {"instance", "location"};
    private static final int MONITOR_INITIAL_DELAY = 3_000;
    private static final int MONITOR_PERIOD = 30_000;
    private static final long ECHO_TIMEOUT = 120_000;

    private final CollectorRegistry collectorRegistry = new CollectorRegistry();

    private final Gauge responseTimeGauge = Gauge.build().name("minion_response_time").help("Response time of Minion RPC")
        .unit("msec").labelNames(LABEL_NAMES).register(collectorRegistry);

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
        eventSubscriptionService.addEventListener(this);
        List<OnmsMinion> minions = sessionUtils.withReadOnlyTransaction(minionDao::findAll);
        minionCache.addAll(minions);
        minionCache.forEach(minion -> {
            // TBD888: NEED TENANT ID
            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMinionMonitor("opennms-prime", minion.getLocation(), minion.getId()),
                MONITOR_INITIAL_DELAY, MONITOR_PERIOD, TimeUnit.MILLISECONDS);
        });
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
            IParm locationParm = event.getParm(EventConstants.PARAM_MONITORING_SYSTEM_LOCATION);
            String location = locationParm.getValue() != null ? locationParm.getValue().getContent()
                : MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID;
            IParm systemIdParm = event.getParm(EventConstants.PARAM_MONITORING_SYSTEM_ID);
            String systemId = systemIdParm.getValue() != null ? systemIdParm.getValue().getContent() : null;
            if (!Strings.isNullOrEmpty(systemId)) {
                // TBD888: NEED TENANT ID
                LOG.info("Received event for new Minion with Id {}", systemId);
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMinionMonitor("opennms-prime", location, systemId),
                    MONITOR_INITIAL_DELAY, MONITOR_PERIOD, TimeUnit.MILLISECONDS);
            }
        }

    }


    private void runMinionMonitor(String tenantId, String location, String minionId) {
         LOG.info("Minion RPC Monitor: executing check for minion: id={}; location={}", minionId, location);

         EchoRequest echoRequest = EchoRequest.newBuilder()
             .setTime(System.nanoTime())
             .setMessage(Strings.repeat("*", DEFAULT_MESSAGE_SIZE))
             .build();

         echoClient.execute(tenantId, minionId, location, ECHO_TIMEOUT, echoRequest).whenComplete((response, error) -> {
             if (error != null) {
                 LOG.warn("ECHO REQUEST failed", error);
                 LOG.error("Minion RPC Monitor: check for minion failed: id={}", minionId);
                 return;
             }
             long responseTime = (System.nanoTime() - response.getTime()) / 1000000;
             LOG.info("ECHO RESPONSE: node-id={}; node-location={}; duration={}ms", minionId, location, responseTime);
             updateMetrics(responseTime, new String[]{minionId, location});
         });
    }

     private void updateMetrics(long responseTime, String[] labelValues) {
         responseTimeGauge.labels(labelValues).set(responseTime);
         var groupingKey = IntStream.range(0, LABEL_NAMES.length).boxed()
             .collect(Collectors.toMap(i -> LABEL_NAMES[i], i -> labelValues[i]));
         metricsAdapter.pushMetrics(collectorRegistry, groupingKey);
     }

}
