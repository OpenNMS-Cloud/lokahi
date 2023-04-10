/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.horizon.shared.ipc.grpc.server;

import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.Message;
import io.opentracing.Tracer;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.common.AbstractMessageDispatcherFactory;
import org.osgi.framework.BundleContext;

public class GrpcLocalDispatcherFactory extends AbstractMessageDispatcherFactory<Void> {

    static final String SINK_METRIC_PRODUCER_DOMAIN = "org.opennms.core.ipc.sink.producer";

    private OpennmsGrpcServer onmsGrpcServer;

    private MetricRegistry metrics;

    public GrpcLocalDispatcherFactory(OpennmsGrpcServer onmsGrpcServer) {
        this.onmsGrpcServer = onmsGrpcServer;
    }

    public GrpcLocalDispatcherFactory() {
    }

    @Override
    public <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, Void metadata, T message) {
        onmsGrpcServer.dispatch(module, message);
    }

    @Override
    public String getMetricDomain() {
        return SINK_METRIC_PRODUCER_DOMAIN;
    }

    @Override
    public Tracer getTracer() {
        return null;
    }

    @Override
    public MetricRegistry getMetrics() {
        if(metrics == null) {
            metrics = new MetricRegistry();
        }
        return metrics;
    }

    public void setMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
    }
}
