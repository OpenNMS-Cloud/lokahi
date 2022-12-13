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

package org.opennms.horizon.server.service;

import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.TSResult;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.isNull;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class NodeStatusService {
    private static final String RESPONSE_TIME_METRIC = "response_time_msec";
    private static final int TIME_RANGE_IN_SECONDS = 90;
    private static final String NODE_ID_KEY = "node_id";
    private static final String MONITOR_KEY = "monitor";
    private static final String INSTANCE_KEY = "instance";
    private static final long TIMEOUT_IN_SECONDS = 30L;
    private final InventoryClient client;
    private final PrometheusTSDBServiceImpl prometheusTSDBService;
    private final ServerHeaderUtil headerUtil;

    public boolean getNodeStatus(long id, String monitorType, ResolutionEnvironment env) {
        NodeDTO node = client.getNodeById(id, headerUtil.getAuthHeader(env));

        if (node.getIpInterfacesCount() > 0) {

            IpInterfaceDTO ipInterface = node.getIpInterfaces(0);
            return getNodeStatusByInterface(id, monitorType, ipInterface, env);
        }
        return false;
    }

    private boolean getNodeStatusByInterface(long id, String monitorType, IpInterfaceDTO ipInterface, ResolutionEnvironment env) {
        String ipAddress = ipInterface.getIpAddress();

        TimeSeriesQueryResult result = getStatusMetric(id, ipAddress, monitorType, env);
        if (isNull(result)) {
            return false;
        }
        List<TSResult> tsResults = result.getData().getResult();

        if (isEmpty(tsResults)) {
            return false;
        }

        TSResult tsResult = tsResults.get(0);
        List<List<Double>> values = tsResult.getValues();

        return !isEmpty(values);
    }

    private TimeSeriesQueryResult getStatusMetric(long id, String ipAddress, String monitorType, ResolutionEnvironment env) {
        Map<String, String> labels = new HashMap<>();
        labels.put(NODE_ID_KEY, String.valueOf(id));
        labels.put(MONITOR_KEY, monitorType);
        labels.put(INSTANCE_KEY, ipAddress);

        Mono<TimeSeriesQueryResult> result = prometheusTSDBService
            .getMetric(env, RESPONSE_TIME_METRIC, labels, TIME_RANGE_IN_SECONDS, TimeRangeUnit.SECOND);
        try {
            return result.toFuture().get(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to get metrics for node", e);
        }
    }
}
