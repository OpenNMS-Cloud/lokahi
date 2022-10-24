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

import io.opentracing.Span;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.opennms.netmgt.provision.DetectorRequestExecutor;
import org.opennms.netmgt.provision.DetectorRequestExecutorBuilder;
import org.opennms.netmgt.provision.PreDetectCallback;
import org.opennms.netmgt.provision.ServiceDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectorRequestBuilderImpl implements DetectorRequestExecutorBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(DetectorRequestBuilderImpl.class);

    private final LocationAwareDetectorClientRpcImpl client;

    private String location;
    private String systemId;
    private String detectorName;
    private InetAddress address;
    private Integer nodeId;
    private Map<String, String> attributes = new HashMap<>();
    private Span span;
    private PreDetectCallback preDetectCallback;

    public DetectorRequestBuilderImpl(LocationAwareDetectorClientRpcImpl client) {
        this.client = client;
    }

    @Override
    public DetectorRequestBuilderImpl withLocation(String location) {
        this.location = location;
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withSystemId(String systemId) {
        this.systemId = systemId;
        return this;
    }

    @Override
    @Deprecated
    public DetectorRequestBuilderImpl withClassName(String className) {
        this.detectorName = className;
        return this;
    }

    @Override
    public DetectorRequestExecutorBuilder withDetectorName(String detectorName) {
        this.detectorName = detectorName;
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withServiceName(String serviceName) {
        final ServiceDetector detector = client.getRegistry().getDetectorByServiceName(serviceName);
        if (detector == null) {
            throw new IllegalArgumentException("No detector found with service name '" + serviceName + "'.");
        }
        this.detectorName = client.getRegistry().getDetectorClassNameFromServiceName(serviceName);
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withAddress(InetAddress address) {
        this.address = address;
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withAttributes(Map<String, String> attributes) {
        this.attributes.putAll(attributes);
        return this;
    }

    @Override
    public DetectorRequestExecutorBuilder withRuntimeAttribute(String key, String value) {
        // TODO determine if we need this
        return this;
    }

    @Override
    public DetectorRequestExecutorBuilder withRuntimeAttributes(Map<String, String> attributes) {
        // TODO determine if we need this
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withNodeId(Integer nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    @Override
    public DetectorRequestBuilderImpl withParentSpan(Span span) {
        this.span = span;
        return this;
    }

    @Override
    public DetectorRequestExecutor build() {
        return new
            DetectorRequestExecutorImpl(
                client,
                location,
                systemId,
                detectorName,
                address,
                nodeId,
                span,
                preDetectCallback
        );
    }
}
