/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.core.monitor;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opennms.horizon.core.monitor.taskset.LocationBasedTaskSetManager;
import org.opennms.horizon.core.monitor.taskset.TaskSetManager;
import org.opennms.horizon.db.dao.api.IpInterfaceDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsIpInterface;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.proxy.LocationAwareSnmpClient;
import org.opennms.taskset.model.TaskSet;
import org.opennms.taskset.model.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * TBD888: Rework still needed for task-set definitions, and general completeness
 *
 *  1. list of nodes is only read once, in the init() method; it needs to be updated continually
 *  2. if there are other sources of task definitions, the management of task definitions needs to be extracted into
 *     its own source
 *  3. parameters for each task
 */
public class DeviceMonitorManager implements EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceMonitorManager.class);
    private static final String SYS_OBJECTID_INSTANCE = ".1.3.6.1.2.1.1.2.0";
    private static final Long INVALID_UP_TIME = -1L;
    private static final String DEFAULT_LOCATION = "Default";
    private final EventSubscriptionService eventSubscriptionService;
    private final NodeDao nodeDao;
    private final IpInterfaceDao ipInterfaceDao;
    private final SessionUtils sessionUtils;
    private final List<OnmsNode> nodeCache = new ArrayList<>();
    private final ThreadFactory monitorThreadFactory = new ThreadFactoryBuilder()
        .setNameFormat("device-monitor-runner-%d")
        .build();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(25, monitorThreadFactory);

    private final LocationBasedTaskSetManager locationBasedTaskSetManager = new LocationBasedTaskSetManager();
    private final TaskSetPublisher taskSetIgniteClient;

    public DeviceMonitorManager(EventSubscriptionService eventSubscriptionService,
                                NodeDao nodeDao,
                                IpInterfaceDao ipInterfaceDao,
                                SessionUtils sessionUtils,
                                TaskSetPublisher taskSetIgniteClient) {
        this.eventSubscriptionService = eventSubscriptionService;
        this.nodeDao = nodeDao;
        this.ipInterfaceDao = ipInterfaceDao;
        this.sessionUtils = sessionUtils;
        this.taskSetIgniteClient = taskSetIgniteClient;
    }

    // TODO: don't use a static snapshot of nodes at init-time; need to update as new nodes are discovered.
    public void init() {
        eventSubscriptionService.addEventListener(this);
        // Add all nodes currently present in the inventory to cache.
        sessionUtils.withReadOnlyTransaction(() -> nodeCache.addAll(nodeDao.findAll()));
        nodeCache.forEach(onmsNode -> {
            LOG.info("Starting device monitoring for device with ID {}", onmsNode.getId());

            scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMonitors(onmsNode), 5, 120, TimeUnit.SECONDS);
        });
    }


    private void runMonitors(OnmsNode onmsNode) {
        String locationName = DEFAULT_LOCATION;

        try {
            List<OnmsIpInterface> ipInterfaces = sessionUtils.withReadOnlyTransaction(() -> ipInterfaceDao.findInterfacesByNodeId(onmsNode.getId()));
            String location = sessionUtils.withReadOnlyTransaction(() -> onmsNode.getLocation().getLocationName());
            ipInterfaces.forEach(onmsIpInterface -> {
                LOG.info("Polling ICMP/SNMP Monitor for IPAddress {}", onmsIpInterface.getIpAddress());

                TaskSetManager taskSetManager = locationBasedTaskSetManager.getManagerForLocation(locationName);

                addPollIcmpTask(taskSetManager, onmsIpInterface.getIpAddress());
                addPollSnmpTask(taskSetManager, onmsIpInterface.getIpAddress(), onmsNode.getSnmpCommunityString());
            });
        } catch (Exception e) {
            LOG.error("Exception while running monitors for device with Id : {}", onmsNode.getId(), e);
        }

        TaskSet updatedTaskSet = locationBasedTaskSetManager.getManagerForLocation(locationName).getTaskSet();

        // TODO: reduce log level to debug
        LOG.info("Publishing task set for location: location={}; num-task={}",
            locationName,
            Optional.ofNullable(updatedTaskSet.getTaskDefinitionList()).map(Collection::size).orElse(0));

        taskSetIgniteClient.publishTaskSet(locationName, updatedTaskSet);
    }

    private void addPollIcmpTask(TaskSetManager taskSetManager, InetAddress inetAddress) {
        Map<String, String> parameters = makeParametersMap("host", inetAddress.getHostAddress(), "timeout", "60000");
        taskSetManager.addIpTask(inetAddress, "icmp-monitor", TaskType.MONITOR, "ICMPMonitor", "120", parameters);
    }

    private void addPollSnmpTask(TaskSetManager taskSetManager, InetAddress inetAddress, String snmpCommunityString) {
        Map<String, String> parameters =
            makeParametersMap(
                "iod", SYS_OBJECTID_INSTANCE,
                "timeout", "18000",
                "retries", "2"
            );

        taskSetManager.addIpTask(inetAddress, "snmp-monitor", TaskType.MONITOR, "SNMPMonitor", "120", parameters);
    }

    @Override
    public String getName() {
        return "Device-Monitor-Manager";
    }

    @Override
    public void onEvent(IEvent event) {
        if (event.getUei().equals(EventConstants.NODE_ADDED_EVENT_UEI)) {
            Long nodeId = event.getNodeid();
            if (nodeId != null) {
                OnmsNode node = sessionUtils.withReadOnlyTransaction(() -> nodeDao.get(nodeId.intValue()));
                scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> runMonitors(node), 5, 120, TimeUnit.SECONDS);
            }
        }
    }

    public void shutdown() {
        eventSubscriptionService.removeEventListener(this);
        scheduledThreadPoolExecutor.shutdown();
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Given any number of pairs of strings, one key and one value, create a map with contents key=value.
     *
     * @param keyValues alternating key and value strings.
     * @return a map with the key=value contents for the given keys and values.
     */
    private Map<String, String> makeParametersMap(String... keyValues) {
        Map<String, String> result = new HashMap<>();

        int cur = 0;
        while ( cur < ( keyValues.length - 1 ) ) {
            result.put(keyValues[cur], keyValues[cur + 1]);
            cur++;
        }

        // Probably not a normal use-case, but accept it
        if (cur < keyValues.length) {
            result.put(keyValues[cur], null);
        }

        return result;
    }
}
