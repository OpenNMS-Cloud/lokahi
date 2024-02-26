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
import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.minion.plugin.api.ServiceDetector;
import org.opennms.horizon.shared.icmp.EchoPacket;
import org.opennms.horizon.shared.icmp.PingConstants;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.ServiceResult;

public class IcmpDetector implements ServiceDetector {

    private final PingerFactory pingerFactory;

    private final Descriptors.FieldDescriptor allowFragmentationFieldDescriptor;
    private final Descriptors.FieldDescriptor dscpFieldDescriptor;
    private final Descriptors.FieldDescriptor packetSizeFieldDescriptor;
    private final Descriptors.FieldDescriptor retriesFieldDescriptor;
    private final Descriptors.FieldDescriptor timeoutFieldDescriptor;

    public IcmpDetector(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;

        Descriptors.Descriptor icmpDetectorRequestDescriptor =
                IcmpDetectorRequest.getDefaultInstance().getDescriptorForType();

        allowFragmentationFieldDescriptor =
                icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.ALLOW_FRAGMENTATION_FIELD_NUMBER);
        dscpFieldDescriptor = icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.DSCP_FIELD_NUMBER);
        packetSizeFieldDescriptor =
                icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.PACKET_SIZE_FIELD_NUMBER);
        retriesFieldDescriptor =
                icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.RETRIES_FIELD_NUMBER);
        timeoutFieldDescriptor =
                icmpDetectorRequestDescriptor.findFieldByNumber(IcmpDetectorRequest.TIMEOUT_FIELD_NUMBER);
    }

    @Override
    public CompletableFuture<ServiceResult> detect(String host, Any config) {
        CompletableFuture<ServiceResult> future = new CompletableFuture<>();

        try {

            if (!config.is(IcmpDetectorRequest.class)) {
                throw new IllegalArgumentException(
                        "configuration must be an IcmpDetectorRequest; type-url=" + config.getTypeUrl());
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
                    new PingResponseHandler(future));
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
