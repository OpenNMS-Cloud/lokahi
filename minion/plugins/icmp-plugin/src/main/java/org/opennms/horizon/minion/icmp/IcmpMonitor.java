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
package org.opennms.horizon.minion.icmp;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.opennms.horizon.minion.plugin.api.*;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.shared.icmp.EchoPacket;
import org.opennms.horizon.shared.icmp.PingConstants;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IcmpMonitor extends AbstractServiceMonitor {

    private final PingerFactory pingerFactory;

    private final Descriptors.FieldDescriptor allowFragmentationFieldDescriptor;
    private final Descriptors.FieldDescriptor dscpFieldDescriptor;
    private final Descriptors.FieldDescriptor hostFieldDescriptor;
    private final Descriptors.FieldDescriptor packetSizeFieldDescriptor;
    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public IcmpMonitor(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;

        Descriptors.Descriptor echoMonitorRequestDescriptor =
                IcmpMonitorRequest.getDefaultInstance().getDescriptorForType();

        allowFragmentationFieldDescriptor =
                echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.ALLOW_FRAGMENTATION_FIELD_NUMBER);
        dscpFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.DSCP_FIELD_NUMBER);
        hostFieldDescriptor = echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.HOST_FIELD_NUMBER);
        packetSizeFieldDescriptor =
                echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.PACKET_SIZE_FIELD_NUMBER);
        retriesFieldDescriptor =
                echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor =
                echoMonitorRequestDescriptor.findFieldByNumber(IcmpMonitorRequest.TIMEOUT_FIELD_NUMBER);
    }

    // ========================================
    //
    // ----------------------------------------

    @Override
    public ServiceMonitorResponse poll(MonitoredService monitoredService, Any config) {
        AtomicReference<ServiceMonitorResponse> responseHolder = new AtomicReference<>(null);

        try {
            if (!config.is(IcmpMonitorRequest.class)) {
                throw new IllegalArgumentException(
                        "configuration must be an IcmpMonitorRequest; type-url=" + config.getTypeUrl());
            }

            IcmpMonitorRequest icmpMonitorRequest = config.unpack(IcmpMonitorRequest.class);
            IcmpMonitorRequest effectiveRequest = populateDefaultsAsNeeded(icmpMonitorRequest);

            String hostString = effectiveRequest.getHost();
            InetAddress host = InetAddress.getByName(hostString);

            boolean allowFragmentation = effectiveRequest.getAllowFragmentation();

            ServiceMonitorCallback callback = new ServiceMonitorCallback() {
                @Override
                public void onResponse(ServiceMonitorResponse response) {
                    responseHolder.set(response);
                }
            };

            Pinger pinger = pingerFactory.getInstance(effectiveRequest.getDscp(), allowFragmentation);

            pinger.ping(
                    host,
                    effectiveRequest.getTimeout(),
                    effectiveRequest.getRetries(),
                    effectiveRequest.getPacketSize(),
                    new MyPingResponseCallback(
                            callback, monitoredService.getNodeId(), monitoredService.getMonitorServiceId()));
        } catch (Exception e) {
            responseHolder.set(ServiceMonitorResponseImpl.builder()
                    .reason("Failed to monitor for azure resource: " + e.getMessage())
                    .monitorType(MonitorType.AZURE)
                    .status(ServiceMonitorResponse.Status.Down)
                    .nodeId(monitoredService.getNodeId())
                    .build());
        }

        return responseHolder.get();
    }

    // ========================================
    // Internal Methods
    // ----------------------------------------

    private IcmpMonitorRequest populateDefaultsAsNeeded(IcmpMonitorRequest request) {
        IcmpMonitorRequest.Builder resultBuilder = request.newBuilder(request);

        if (!request.hasField(retriesFieldDescriptor)) {
            resultBuilder.setRetries(PingConstants.DEFAULT_RETRIES);
        }

        if ((!request.hasField(packetSizeFieldDescriptor)) || (request.getPacketSize() <= 0)) {
            resultBuilder.setPacketSize(PingConstants.DEFAULT_PACKET_SIZE);
        }

        if (!request.hasField(dscpFieldDescriptor)) {
            resultBuilder.setDscp(PingConstants.DEFAULT_DSCP);
        }

        if (!request.hasField(allowFragmentationFieldDescriptor)) {
            resultBuilder.setAllowFragmentation(PingConstants.DEFAULT_ALLOW_FRAGMENTATION);
        }

        if (!request.hasField(timeoutFieldDescriptor)) {
            resultBuilder.setTimeout(PingConstants.DEFAULT_TIMEOUT);
        }

        return resultBuilder.build();
    }

    // ========================================
    // Internal Classes
    // ----------------------------------------

    private static class MyPingResponseCallback implements PingResponseCallback {
        private final Logger logger = LoggerFactory.getLogger(MyPingResponseCallback.class);
        private ServiceMonitorResponse serviceMonitorResponse;
        private final long nodeId;
        private final long monitoredServiceId;
        final ServiceMonitorCallback serviceMonitorCallback;

        public MyPingResponseCallback(
                ServiceMonitorCallback serviceMonitorCallback, long nodeId, long monitoredServiceId) {
            this.serviceMonitorCallback = serviceMonitorCallback;
            this.nodeId = nodeId;
            this.monitoredServiceId = monitoredServiceId;
        }

        @Override
        public void handleResponse(InetAddress inetAddress, EchoPacket response) {
            double responseTimeMicros = Math.round(response.elapsedTime(TimeUnit.MICROSECONDS));
            double responseTimeMillis = responseTimeMicros / 1000.0;
            serviceMonitorResponse = ServiceMonitorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .status(Status.Up)
                    .responseTime(responseTimeMillis)
                    .nodeId(nodeId)
                    .monitoredServiceId(monitoredServiceId)
                    .ipAddress(inetAddress.getHostAddress())
                    .build();
            serviceMonitorCallback.onResponse(serviceMonitorResponse);
        }

        @Override
        public void handleTimeout(InetAddress inetAddress, EchoPacket echoPacket) {
            serviceMonitorResponse = ServiceMonitorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .status(Status.Unknown)
                    .nodeId(nodeId)
                    .monitoredServiceId(monitoredServiceId)
                    .reason("timeout")
                    .ipAddress(inetAddress.getHostAddress())
                    .build();
            serviceMonitorCallback.onResponse(serviceMonitorResponse);
        }

        @Override
        public void handleError(InetAddress inetAddress, EchoPacket echoPacket, Throwable throwable) {
            serviceMonitorResponse = ServiceMonitorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .status(Status.Down)
                    .nodeId(nodeId)
                    .reason(throwable.getMessage())
                    .monitoredServiceId(monitoredServiceId)
                    .ipAddress(inetAddress.getHostAddress())
                    .build();
            serviceMonitorCallback.onResponse(serviceMonitorResponse);
        }
    }
}
