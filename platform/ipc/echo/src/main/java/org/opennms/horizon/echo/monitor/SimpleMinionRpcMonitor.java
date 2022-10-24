/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
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

package org.opennms.horizon.echo.monitor;

import com.google.common.base.Strings;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.echo.EchoRequest;
import org.opennms.horizon.echo.EchoResponse;
import org.opennms.horizon.echo.EchoRpcModule;
import org.opennms.horizon.ipc.rpc.api.RpcClient;
import org.opennms.horizon.ipc.rpc.api.RpcClientFactory;
import org.opennms.horizon.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleMinionRpcMonitor {

    private final static int DEFAULT_MESSAGE_SIZE = 1024;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMinionRpcMonitor.class);

    private static final String[] labelNames = {"instance", "location"};

    private final CollectorRegistry collectorRegistry = new CollectorRegistry();

    private final Gauge responseTimeGauge = Gauge.build().name("minion_response_time").help("Response time of Minion RPC")
        .unit("msec").labelNames(labelNames).register(collectorRegistry);

    private RpcClientFactory rpcClientFactory;

    private OnmsMetricsAdapter metricsAdapter;


    private String nodeId;
    private String ipAddr;
    private String nodeLabel;
    private String nodeLocation;
    private InetAddress address;
    private String foreignId;

//========================================
// Getters and Setters
//----------------------------------------

    public RpcClientFactory getRpcClientFactory() {
        return rpcClientFactory;
    }

    public void setRpcClientFactory(RpcClientFactory rpcClientFactory) {
        this.rpcClientFactory = rpcClientFactory;
    }

    public OnmsMetricsAdapter getMetricsAdapter() {
        return metricsAdapter;
    }

    public void setMetricsAdapter(OnmsMetricsAdapter metricsAdapter) {
        this.metricsAdapter = metricsAdapter;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }

    public String getNodeLocation() {
        return nodeLocation;
    }

    public void setNodeLocation(String nodeLocation) {
        this.nodeLocation = nodeLocation;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public String getForeignId() {
        return foreignId;
    }

    public void setForeignId(String foreignId) {
        this.foreignId = foreignId;
    }

//========================================
// Processing
//----------------------------------------

    public void process() {
        // Create the client
        RpcClient<EchoRequest, EchoResponse> client = rpcClientFactory.getClient(EchoRpcModule.INSTANCE);

        // Build the request
        EchoRequest request = new EchoRequest();
        request.setId(System.nanoTime() / 1000000L);
        request.setMessage(Strings.repeat("*", DEFAULT_MESSAGE_SIZE));
        request.setLocation(nodeLocation);
        request.setSystemId(foreignId);
        request.setTimeToLiveMs(null);
        request.addTracingInfo(RpcRequest.TAG_NODE_ID, nodeId);
        request.addTracingInfo(RpcRequest.TAG_NODE_LABEL, nodeLabel);
        request.addTracingInfo(RpcRequest.TAG_CLASS_NAME, SimpleMinionRpcMonitor.class.getCanonicalName());
        request.addTracingInfo(RpcRequest.TAG_IP_ADDRESS, InetAddressUtils.toIpAddrString(address));

        try {
            EchoResponse response = client.execute(request).get();
            long responseTime = ( System.nanoTime() / 1000000 ) - response.getId();
            LOG.info("ECHO RESPONSE: node-id={}; node-location={}; duration={}ms", nodeId, nodeLocation, responseTime);
            updateMetrics(responseTime, new String[]{nodeId, nodeLocation});
        } catch (InterruptedException|ExecutionException t) {
            LOG.warn("ECHO REQUEST failed", t);
        }
    }

    private void updateMetrics(long responseTime, String[] labelValues) {
        responseTimeGauge.labels(labelValues).set(responseTime);
        var groupingKey = IntStream.range(0, labelNames.length).boxed()
            .collect(Collectors.toMap(i -> labelNames[i], i -> labelValues[i]));
        metricsAdapter.pushMetrics(collectorRegistry, groupingKey);
    }
}
