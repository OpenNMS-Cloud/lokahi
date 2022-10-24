/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.provision.detector;

import static org.opennms.netmgt.provision.service.Provisioner.ERROR;

import io.opentracing.Span;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.db.model.OnmsMonitoringLocation;
import org.opennms.netmgt.provision.DetectorRequestExecutor;
import org.opennms.netmgt.provision.LocationAwareDetectorClient;
import org.opennms.netmgt.provision.persistence.dto.PluginConfigDTO;
import org.opennms.netmgt.provision.persistence.dto.PluginParameterDTO;
import org.opennms.netmgt.provision.rpc.relocate.Async;
import org.opennms.netmgt.provision.rpc.relocate.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectorRunner implements Async<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(DetectorRunner.class);

    private final LocationAwareDetectorClient locationAwareDetectorClient;
    private final PluginConfigDTO detectorConfig;
    private final Integer nodeId;
    private final InetAddress address;
    private final OnmsMonitoringLocation location;
    private final Span parentSpan;
    private Span span;

    public DetectorRunner(LocationAwareDetectorClient locationAwareDetectorClient, PluginConfigDTO detectorConfig, Integer nodeId, InetAddress address,
            OnmsMonitoringLocation location, Span span) {
        this.locationAwareDetectorClient = locationAwareDetectorClient;
        this.detectorConfig = detectorConfig;
        this.nodeId = nodeId;
        this.address = address;
        this.location = location;
        parentSpan = span;
    }

    /** {@inheritDoc} */
    @Override
    public void supplyAsyncThenAccept(final Callback<Boolean> cb) {
        try {
            LOG.info("Attemping to detect service {} on address {} at location {}", detectorConfig.getName(),
                    getHostAddress(), getLocationName());
            // Launch the detector
//            if(!LocationUtils.isDefaultLocationName(getLocationName())) {
//                startSpan();
//            }

            DetectorRequestExecutor detectorRequestExecutor =
                locationAwareDetectorClient.detect().withClassName(detectorConfig.getPluginClass())
                        .withAddress(address).withNodeId(nodeId).withLocation(getLocationName())
                        .withAttributes(detectorConfig.getParameters().stream()
                                .collect(Collectors.toMap(PluginParameterDTO::getKey, PluginParameterDTO::getValue)))
                        .withParentSpan(span)
    //                    .withPreDetectCallback(this::startSpan)
                        .build()
                    ;

            CompletableFuture<Boolean> completableFuture =
                detectorRequestExecutor.execute()
                            // After completion, run the callback
                            .whenComplete((res, ex) -> {
                                LOG.info("Completed detector execution for service {} on address {} at location {}",
                                        detectorConfig.getName(), getHostAddress(), getLocationName());
                                if (ex != null) {
                                    cb.handleException(ex);
                                    if(span != null) {
                                        span.log(ex.getMessage());
                                        span.setTag(ERROR, true);
                                    }
                                } else {
                                    cb.accept(res);
                                }
                                if(span != null) {
                                    span.finish();
                                }
                            });
        } catch (Throwable e) {
            LOG.warn("Detection failure", e);
            cb.handleException(e);
        }
    }

//    private void startSpan() {
//        if (parentSpan != null) {
//            span = m_service.buildAndStartSpan(detectorConfig.getName() + "-DetectRunner", parentSpan.context());
//            span.setTag(DETECTOR_NAME, detectorConfig.getName());
//            span.setTag(IP_ADDRESS, address.getHostAddress());
//            span.setTag(LOCATION, getLocationName());
//        }
//    }

    private String getHostAddress() {
        return InetAddressUtils.str(address);
    }

    private String getLocationName() {
        return location != null ? location.getLocationName() : null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Run detector %s on address %s", detectorConfig.getName(), getHostAddress());
    }


}
