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

package org.opennms.netmgt.provision;

import io.opentracing.Span;

import java.net.InetAddress;
import java.util.Map;

public interface DetectorRequestExecutorBuilder {

    DetectorRequestExecutorBuilder withLocation(String location);

    DetectorRequestExecutorBuilder withSystemId(String systemId);

    DetectorRequestExecutorBuilder withServiceName(String serviceName);

    // Being removed - use detectorName
    @Deprecated
    DetectorRequestExecutorBuilder withClassName(String className);

    DetectorRequestExecutorBuilder withDetectorName(String detectorName);

    DetectorRequestExecutorBuilder withAddress(InetAddress address);

    DetectorRequestExecutorBuilder withAttribute(String key, String value);

    DetectorRequestExecutorBuilder withAttributes(Map<String, String> attributes);

    DetectorRequestExecutorBuilder withRuntimeAttribute(String key, String value);

    DetectorRequestExecutorBuilder withRuntimeAttributes(Map<String, String> attributes);

    DetectorRequestExecutorBuilder withNodeId(Integer nodeId);

    DetectorRequestExecutorBuilder withParentSpan(Span span);

    DetectorRequestExecutor build();

}
