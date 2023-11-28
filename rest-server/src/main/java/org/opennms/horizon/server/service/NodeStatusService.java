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
import org.opennms.horizon.server.model.inventory.MonitoredServiceStatusRequest;
import org.opennms.horizon.server.model.inventory.TopNNode;
import org.opennms.horizon.server.model.status.NodeReachability;
import org.opennms.horizon.server.model.status.NodeResponseTime;
import org.opennms.horizon.server.model.status.NodeStatus;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.metrics.Constants;
import org.opennms.horizon.server.service.metrics.TSDBMetricsService;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static org.opennms.horizon.server.service.metrics.Constants.AVG_RESPONSE_TIME;
import static org.opennms.horizon.server.service.metrics.Constants.AZURE_MONITOR_TYPE;
import static org.opennms.horizon.server.service.metrics.Constants.AZURE_SCAN_TYPE;
import static org.opennms.horizon.server.service.metrics.Constants.INSTANCE_KEY;
import static org.opennms.horizon.server.service.metrics.Constants.MONITOR_KEY;
import static org.opennms.horizon.server.service.metrics.Constants.NODE_ID_KEY;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class NodeStatusService {
    private static final Logger LOG = LoggerFactory.getLogger(NodeStatusService.class);
    private static final String RESPONSE_TIME_METRIC = "response_time_msec";
    private static final int TIME_RANGE_IN_SECONDS = 90;
    private final InventoryClient client;
    private final TSDBMetricsService tsdbMetricsService;
    private final ServerHeaderUtil headerUtil;

    public Mono<NodeStatus> getNodeStatus(long id, String monitorType, ResolutionEnvironment env) {
        NodeDTO node = client.getNodeById(id, headerUtil.getAuthHeader(env));

        if (AZURE_SCAN_TYPE.equals(node.getScanType())) {
            return getStatusMetric(id, "azure-node-" + id, AZURE_MONITOR_TYPE, env)
                .map(result -> getNodeStatus(id, result));
        } else {
            if (node.getIpInterfacesCount() > 0) {
                IpInterfaceDTO ipInterface = getPrimaryInterface(node);
                return getNodeStatusByInterface(id, monitorType, ipInterface, env);
            }
        }
        return Mono.just(new NodeStatus(id, false));
    }

    private IpInterfaceDTO getPrimaryInterface(NodeDTO node) {
        List<IpInterfaceDTO> ipInterfacesList = node.getIpInterfacesList();
        for (IpInterfaceDTO ipInterface : ipInterfacesList) {
            if (ipInterface.getSnmpPrimary()) {
                return ipInterface;
            }
        }
        return node.getIpInterfaces(0);
    }

    private Mono<NodeStatus> getNodeStatusByInterface(long id, String monitorType, IpInterfaceDTO ipInterface, ResolutionEnvironment env) {
        String ipAddress = ipInterface.getIpAddress();

        return getStatusMetric(id, ipAddress, monitorType, env)
            .map(result -> getNodeStatus(id, result));
    }

    private NodeStatus getNodeStatus(long id, TimeSeriesQueryResult result) {
        if (isNull(result)) {
            return new NodeStatus(id, false);
        }
        List<TSResult> tsResults = result.getData().getResult();

        if (isEmpty(tsResults)) {
            return new NodeStatus(id, false);
        }

        TSResult tsResult = tsResults.get(0);
        List<List<Double>> values = tsResult.getValues();

        if (isEmpty(values)) {
            return new NodeStatus(id, false);
        }

        List<Double> doubles = values.get(values.size() - 1);
        if (doubles.size() != 2) {
            return new NodeStatus(id, false);
        }

        Double responseTime = doubles.get(1);
        boolean status = responseTime > 0d;

        return new NodeStatus(id, status);
    }

    private Mono<TimeSeriesQueryResult> getStatusMetric(long id, String instance, String monitorType, ResolutionEnvironment env) {
        Map<String, String> labels = new HashMap<>();
        labels.put(NODE_ID_KEY, String.valueOf(id));
        labels.put(MONITOR_KEY, monitorType);
        labels.put(INSTANCE_KEY, instance);

        return tsdbMetricsService
            .getMetric(env, RESPONSE_TIME_METRIC, labels, TIME_RANGE_IN_SECONDS, TimeRangeUnit.SECOND);
    }

    public Mono<NodeReachability> getNodeReachability(NodeDTO node,
                                                Integer timeRange,
                                                TimeRangeUnit timeRangeUnit,
                                                ResolutionEnvironment env) {
        IpInterfaceDTO ipInterface = getPrimaryInterface(node);
        Map<String, String> labels = new HashMap<>();
        labels.put(NODE_ID_KEY, String.valueOf(node.getId()));
        labels.put(MONITOR_KEY, Constants.DEFAULT_MONITOR_TYPE);
        labels.put(INSTANCE_KEY, ipInterface.getIpAddress());

        var request = new MonitoredServiceStatusRequest();
        request.setNodeId(node.getId());
        request.setMonitorType(Constants.DEFAULT_MONITOR_TYPE);
        request.setIpAddress(ipInterface.getIpAddress());
        // Defaults to 24hr window for first observation time.
        long firstObservationTime = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli();
        try {
            var monitorStatusProto = client.getMonitorStatus(request, headerUtil.getAuthHeader(env));
            firstObservationTime = monitorStatusProto.getFirstObservationTime();
        } catch (Exception e) {
            LOG.warn("Exception while getting monitor status for request {}", request);
        }
        var optionalParams = Map.of(Constants.FIRST_OBSERVATION_TIME, String.valueOf(firstObservationTime));
        try {
            return tsdbMetricsService.
                getCustomMetric(env, Constants.REACHABILITY_PERCENTAGE, labels, timeRange, timeRangeUnit, optionalParams)
                .map(result -> transformToNodeReachability(node.getId(), result));
        } catch (Exception e) {
            LOG.warn("Failed to get reachability for node id {}", node.getId(), e);
        }
        return Mono.just(new NodeReachability(node.getId(), 0.0));
    }

    private NodeReachability transformToNodeReachability(long id, TimeSeriesQueryResult result) {
        if (isNull(result)) {
            return new NodeReachability(id, 0.0);
        }
        List<TSResult> tsResults = result.getData().getResult();

        if (isEmpty(tsResults)) {
            return new NodeReachability(id, 0.0);
        }

        for (TSResult tsResult : tsResults) {
            List<List<Double>> values = tsResult.getValues();
            List<Double> doubles;
            if (isEmpty(values)) {
                doubles = tsResult.getValue();
            } else {
                doubles = values.get(values.size() - 1);
            }
            if (doubles.size() != 2) {
                continue;
            }
            Double reachability = doubles.get(1);
            var roundedValue = Math.min(reachability, 100.0);
            return new NodeReachability(id, roundedValue);
        }
        return new NodeReachability(id, 0.0);
    }

    public Mono<NodeResponseTime> getNodeAvgResponseTime(NodeDTO node, Integer timeRange, TimeRangeUnit timeRangeUnit, ResolutionEnvironment env) {
        IpInterfaceDTO ipInterface = getPrimaryInterface(node);
        Map<String, String> labels = new HashMap<>();
        labels.put(NODE_ID_KEY, String.valueOf(node.getId()));
        labels.put(MONITOR_KEY, Constants.DEFAULT_MONITOR_TYPE);
        labels.put(INSTANCE_KEY, ipInterface.getIpAddress());

        try {
            return tsdbMetricsService.getMetric(env,
                AVG_RESPONSE_TIME, labels, timeRange, timeRangeUnit).map(result ->
                transformToNodeResponseTime(node.getId(), result));
        } catch (Exception e) {
            LOG.warn("Failed to get response time for node id {}", node.getId(), e);
        }
        return Mono.just(new NodeResponseTime(node.getId(), 0.0));
    }

    public Mono<TopNNode> getTopNNode(NodeDTO nodeDTO, Integer timeRange, TimeRangeUnit timeRangeUnit, ResolutionEnvironment env) {

        Mono<NodeReachability> nodeReachability = getNodeReachability(nodeDTO, timeRange, timeRangeUnit, env);
        Mono<NodeResponseTime> nodeResponseTime = getNodeAvgResponseTime(nodeDTO, timeRange, timeRangeUnit, env);
        Mono<Tuple2<NodeReachability, NodeResponseTime>> result = nodeReachability.zipWith(nodeResponseTime);

        return result.map(tuple -> {
            var topNNode = new TopNNode();
            topNNode.setNodeLabel(nodeDTO.getNodeLabel());
            topNNode.setLocation(nodeDTO.getLocation());
            // Round to 2 points after decimal
            topNNode.setReachability(Math.round(tuple.getT1().getReachability() * 100.0) / 100.0);
            topNNode.setAvgResponseTime(Math.round(tuple.getT2().getResponseTime() * 100.0) / 100.0);
            return topNNode;
        });
    }

    public Mono<NodeStatus> getNodeStatus(NodeDTO nodeDTO,  ResolutionEnvironment env) {
        return getNodeStatus(nodeDTO.getId(), Constants.DEFAULT_MONITOR_TYPE, env);
    }

    private NodeResponseTime transformToNodeResponseTime(long id, TimeSeriesQueryResult result) {
        if (isNull(result)) {
            return new NodeResponseTime(id, 0.0);
        }
        List<TSResult> tsResults = result.getData().getResult();

        if (isEmpty(tsResults)) {
            return new NodeResponseTime(id, 0.0);
        }

        for (TSResult tsResult : tsResults) {
            List<List<Double>> values = tsResult.getValues();

            List<Double> doubles;
            if (isEmpty(values)) {
                doubles = tsResult.getValue();
            } else {
                doubles = values.get(values.size() - 1);
            }
            if (doubles.size() != 2) {
                continue;
            }
            Double responseTime = doubles.get(1);
            return new NodeResponseTime(id, responseTime);
        }
        return new NodeResponseTime(id, 0.0);

    }

}
