/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.detector.client;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.opentracing.Span;
import org.opennms.configvars.FallbackScope;
import org.opennms.configvars.Interpolator;
import org.opennms.horizon.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.netmgt.provision.DetectRequest;
import org.opennms.netmgt.provision.DetectorRequestExecutor;
import org.opennms.netmgt.provision.PreDetectCallback;
import org.opennms.netmgt.provision.ServiceDetectorFactory;
import org.opennms.netmgt.provision.rpc.relocate.MetadataConstants;
import org.opennms.netmgt.provision.rpc.relocate.ParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DetectorRequestExecutorImpl implements DetectorRequestExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(DetectorRequestExecutorImpl.class);

    private static final String PORT = "port";

    private final LocationAwareDetectorClientRpcImpl client;

    private final String location;
    private final String systemId;
    private final String detectorName;
    private final InetAddress address;
    private final Integer nodeId;
    private final Map<String, String> attributes = new HashMap<>();
    private final Span span;
    private final PreDetectCallback preDetectCallback;

    public DetectorRequestExecutorImpl(
        LocationAwareDetectorClientRpcImpl client,
        String location,
        String systemId,
        String detectorName,
        InetAddress address,
        Integer nodeId,
        Span span,
        PreDetectCallback preDetectCallback) {

        this.client = client;
        this.location = location;
        this.systemId = systemId;
        this.detectorName = detectorName;
        this.address = address;
        this.nodeId = nodeId;
        this.span = span;
        this.preDetectCallback = preDetectCallback;
    }

    /**
     * Builds the {@link DetectorRequestDTO} and executes the requested detector
     * via the RPC client.
     */
    @Override
    public CompletableFuture<Boolean> execute() {
        if (address == null) {
            throw new IllegalArgumentException("Address is required.");
        } else if (detectorName == null) {
            throw new IllegalArgumentException("Detector class name is required.");
        }

        FallbackScope entityAttributeScope = new FallbackScope(
                client.getEntityScopeProvider().getScopeForNode(nodeId),
                client.getEntityScopeProvider().getScopeForInterface(nodeId, InetAddressUtils.toIpAddrString(address))
        );

        // Process the requested attributes for "${...}" replacements using the underlying entity attributes as values
        //  to fill in the replacements.
        Map<String, String> interpolatedAttributes = Interpolator.interpolateStrings(attributes, entityAttributeScope);

        // Retrieve the factory associated with the requested detector
        final ServiceDetectorFactory<?> factory = client.getRegistry().getDetectorFactoryByClassName(detectorName);
        if (factory == null) {
            // Fail immediately if no suitable factory was found
            throw new IllegalArgumentException("No factory found for detector with class name '" + detectorName + "'.");
        }

        // Store all of the request details in the DTO
        final DetectorRequestDTO detectorRequestDTO = new DetectorRequestDTO();
        detectorRequestDTO.setLocation(location);
        detectorRequestDTO.setSystemId(systemId);
        detectorRequestDTO.setClassName(detectorName);
        detectorRequestDTO.setAddress(address);
        // Update ttl from metadata
        String timeToLive = interpolatedAttributes.get(MetadataConstants.TTL);
        if (!Strings.isNullOrEmpty(timeToLive)) {
            Long ttlFromMetadata = ParameterMap.getLongValue(MetadataConstants.TTL, interpolatedAttributes.get(MetadataConstants.TTL), null);
            detectorRequestDTO.setTimeToLiveMs(ttlFromMetadata);
            //Remove ttl from attributes as it is not a detector attribute.
            interpolatedAttributes.remove(MetadataConstants.TTL);
        }
        detectorRequestDTO.addDetectorAttributes(interpolatedAttributes);
        detectorRequestDTO.addTracingInfo(RpcRequest.TAG_CLASS_NAME, detectorName);
        detectorRequestDTO.addTracingInfo(RpcRequest.TAG_IP_ADDRESS, InetAddressUtils.toIpAddrString(address));
        detectorRequestDTO.setSpan(span);
        detectorRequestDTO.setPreDetectCallback(preDetectCallback);
        // Attempt to extract the port from the list of attributes
        Integer port = null;
        final String portString = interpolatedAttributes.get(PORT);
        if (portString != null) {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException nfe) {
                LOG.warn("Failed to parse port as integer from: {}", portString);
            }
        }

        // Build the DetectRequest and store the runtime attributes in the DTO
        final DetectRequest request = factory.buildRequest(location, address, port, interpolatedAttributes);
        detectorRequestDTO.addRuntimeAttributes(request.getRuntimeAttributes());
        // Execute the request

        // TBD888: PROTOBUF BYTE-STRING passed over the wire (LATER)

        return client.getDelegate().execute(detectorRequestDTO)
            .thenApply(response -> {
                // Notify the factory that a request was successfully executed
                try {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("DETECTOR RESPONSE: {}", new Gson().toJson(response));
                    }

                    factory.afterDetect(request, response, nodeId);
                } catch (Throwable t) {
                    LOG.error("Error while processing detect callback.", t);
                }
                return response.isDetected();
            });
    }
}
