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

package org.opennms.horizon.minion.icmp;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceDetectorResponseImpl;
import org.opennms.horizon.shared.icmp.EchoPacket;
import org.opennms.horizon.shared.icmp.PingConstants;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.ServiceResult;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

public class IcmpDetector implements ServiceDetector {

    private static final Logger LOG = LoggerFactory.getLogger(IcmpDetector.class);
    private final PingerFactory pingerFactory;

    private final Descriptors.FieldDescriptor allowFragmentationFieldDescriptor;
    private final Descriptors.FieldDescriptor dscpFieldDescriptor;
    private final Descriptors.FieldDescriptor packetSizeFieldDescriptor;
    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public IcmpDetector(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;

        Descriptors.Descriptor icmpDetectorRequestDescriptor = IcmpDetectorRequest.getDefaultInstance().getDescriptorForType();

        allowFragmentationFieldDescriptor = icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.ALLOW_FRAGMENTATION_FIELD_NUMBER);
        dscpFieldDescriptor = icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.DSCP_FIELD_NUMBER);
        packetSizeFieldDescriptor = icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.PACKET_SIZE_FIELD_NUMBER);
        retriesFieldDescriptor = icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor = icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.TIMEOUT_FIELD_NUMBER);
    }

    @Override
    public CompletableFuture<ServiceDetectorResponse> detect(Any config, long nodeId) {

        CompletableFuture<ServiceDetectorResponse> future = new CompletableFuture<>();
        String hostAddress = null;

        try {

            if (!config.is(IcmpDetectorRequest.class)) {
                throw new IllegalArgumentException("configuration must be an IcmpDetectorRequest; type-url=" + config.getTypeUrl());
            }
            IcmpDetectorRequest icmpDetectorRequest = config.unpack(IcmpDetectorRequest.class);
            IcmpDetectorRequest effectiveRequest = populateDefaultsAsNeeded(icmpDetectorRequest);

            hostAddress = effectiveRequest.getHost();
            InetAddress host = InetAddress.getByName(hostAddress);
            int dscp = effectiveRequest.getDscp();
            boolean allowFragmentation = effectiveRequest.getAllowFragmentation();

            Pinger pinger = pingerFactory.getInstance(dscp, allowFragmentation);

            pinger.ping(
                host,
                effectiveRequest.getTimeout(),
                effectiveRequest.getRetries(),
                effectiveRequest.getPacketSize(),
                new MyPingResponseCallback(future, nodeId)
            );
        } catch (Exception e) {
            future.complete(
                ServiceDetectorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .serviceDetected(false)
                    .reason(e.getMessage())
                    .ipAddress(hostAddress)
                    .nodeId(nodeId)
                    .build()
            );
        }

        return future;
    }

    @Override
    public CompletableFuture<ServiceResult> detect(String host, Any config) {
        CompletableFuture<ServiceResult> future = new CompletableFuture<>();

        try {

            if (!config.is(IcmpDetectorRequest.class)) {
                throw new IllegalArgumentException("configuration must be an IcmpDetectorRequest; type-url=" + config.getTypeUrl());
            }

            IcmpDetectorRequest icmpDetectorRequest = config.unpack(IcmpDetectorRequest.class);
            IcmpDetectorRequest effectiveRequest = populateDefaultsAsNeeded(icmpDetectorRequest);

            InetAddress hostAddress = InetAddress.getByName(host);
            int dscp = effectiveRequest.getDscp();
            boolean allowFragmentation = effectiveRequest.getAllowFragmentation();

            Pinger pinger = pingerFactory.getInstance(dscp, allowFragmentation);

            pinger.ping(
                hostAddress,
                effectiveRequest.getTimeout(),
                effectiveRequest.getRetries(),
                effectiveRequest.getPacketSize(),
                new PingResponseHandler(future)
            );
        } catch (Exception e) {
            future.complete(ServiceResult.newBuilder().setIpAddress(host).build());
        }

        return future;
    }

    private IcmpDetectorRequest populateDefaultsAsNeeded(IcmpDetectorRequest request) {
        IcmpDetectorRequest.Builder resultBuilder = IcmpDetectorRequest.newBuilder(request);

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

    private static class MyPingResponseCallback implements PingResponseCallback {
        private final CompletableFuture<ServiceDetectorResponse> future;

        public MyPingResponseCallback(CompletableFuture<ServiceDetectorResponse> future, long nodeId) {
            this.future = future;
        }

        @Override
        public void handleResponse(InetAddress inetAddress, EchoPacket response) {
            future.complete(
                ServiceDetectorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .serviceDetected(true)
                    .ipAddress(inetAddress.getHostAddress())
                    .build()
            );
        }

        @Override
        public void handleTimeout(InetAddress inetAddress, EchoPacket echoPacket) {
            future.complete(
                ServiceDetectorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .serviceDetected(false)
                    .reason(String.format("Timed out ICMP request for ip = %s", inetAddress.getHostAddress()))
                    .ipAddress(inetAddress.getHostAddress())
                    .build()
            );
        }

        @Override
        public void handleError(InetAddress inetAddress, EchoPacket echoPacket, Throwable throwable) {
            future.complete(
                ServiceDetectorResponseImpl.builder()
                    .monitorType(MonitorType.ICMP)
                    .serviceDetected(false)
                    .reason(throwable.getMessage())
                    .ipAddress(inetAddress.getHostAddress())
                    .build()
            );
        }
    }

    private static class PingResponseHandler implements PingResponseCallback {

        private final CompletableFuture<ServiceResult> future;

        private PingResponseHandler(CompletableFuture<ServiceResult> future) {
            this.future = future;
        }

        @Override
        public void handleResponse(InetAddress address, EchoPacket response) {
            future.complete(ServiceResult.newBuilder()
                .setIpAddress(InetAddressUtils.str(address))
                .setService(ServiceType.ICMP)
                .setStatus(true)
                .build());
        }

        @Override
        public void handleTimeout(InetAddress address, EchoPacket request) {
            future.complete(ServiceResult.newBuilder()
                .setIpAddress(InetAddressUtils.str(address))
                .setService(ServiceType.ICMP)
                .setStatus(false)
                .build());
        }

        @Override
        public void handleError(InetAddress address, EchoPacket request, Throwable t) {
            future.complete(ServiceResult.newBuilder()
                .setIpAddress(InetAddressUtils.str(address))
                .setService(ServiceType.ICMP)
                .setStatus(false)
                .build());
        }
    }
}
