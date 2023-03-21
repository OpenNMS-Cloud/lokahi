/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.server.service.flows;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.dataplatform.flows.querier.ApplicationsServiceGrpc;
import org.opennms.dataplatform.flows.querier.ExporterServiceGrpc;
import org.opennms.dataplatform.flows.querier.Querier;
import org.opennms.horizon.server.model.flows.Exporter;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.model.flows.TimeRange;
import org.opennms.horizon.server.model.inventory.IpInterface;
import org.opennms.horizon.server.model.inventory.Node;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class FlowClient {
    private final ManagedChannel channel;
    private final long deadlineMs;

    private ApplicationsServiceGrpc.ApplicationsServiceBlockingStub applicationsServiceBlockingStub;
    private ExporterServiceGrpc.ExporterServiceBlockingStub exporterServiceStub;


    protected void initialStubs() {
        applicationsServiceBlockingStub = ApplicationsServiceGrpc.newBlockingStub(channel);
        exporterServiceStub = ExporterServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public List<Exporter> findExporters(RequestCriteria requestCriteria, String tenantId) {
        List<Exporter> exporters = new ArrayList<>();
        Exporter exporter = new Exporter();
        exporters.add(exporter);
        Node node = new Node();
        IpInterface ipInterface = new IpInterface();
        List<IpInterface> ipInterfaces = new ArrayList<>();
        ipInterfaces.add(ipInterface);
        node.setId(1);
        node.setCreateTime(System.currentTimeMillis());
        node.setNodeLabel("node");
        node.setIpInterfaces(ipInterfaces);
        ipInterface.setTenantId(tenantId);
        ipInterface.setNodeId(node.getId());
        ipInterface.setHostname("hostname");
        ipInterface.setIpAddress("127.0.0.1");
        exporter.setNode(node);
        exporter.setIpInterface(ipInterface);
        return exporters;
    }

    public List<String> findApplications(RequestCriteria requestCriteria, String tenantId) {
        List<String> applications = new ArrayList<>();
        applications.add("https");
        applications.add("ssh");
        return applications;
    }

    public Querier.Summaries getApplicationSummaries(RequestCriteria requestCriteria, String tenantId) {
        var summaries = Querier.Summaries.newBuilder();
        for (int i = 0; i < 10; i++) {
            summaries.addSummaries(Querier.TrafficSummary.newBuilder().setApplication("app_" + i)
                .setBytesIn((long) (Math.random() * 100_000L))
                .setBytesOut((long) (Math.random() * 100_000L)));
        }
        return summaries.build();
    }

    public Querier.Series getApplicationSeries(RequestCriteria requestCriteria, String tenantId) {
        Querier.Series.Builder series = Querier.Series.newBuilder();
        int seriesSize = 100;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < seriesSize; j++) {
                series.addPoint(
                    Querier.FlowingPoint.newBuilder()
                        .setApplication("app_" + i)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() - ((seriesSize - j) * 500)))
                        .setValue(Math.random() * 100_1000L)
                        .setDirection(Querier.Direction.EGRESS));
            }
        }
        return series.build();
    }
}
