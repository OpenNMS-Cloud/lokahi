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

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.opennms.dataplatform.flows.querier.v1.ApplicationsServiceGrpc;
import org.opennms.dataplatform.flows.querier.v1.Direction;
import org.opennms.dataplatform.flows.querier.v1.ExporterServiceGrpc;
import org.opennms.dataplatform.flows.querier.v1.FlowingPoint;
import org.opennms.dataplatform.flows.querier.v1.Series;
import org.opennms.dataplatform.flows.querier.v1.Summaries;
import org.opennms.dataplatform.flows.querier.v1.TrafficSummary;
import org.opennms.horizon.server.model.flows.RequestCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake client, the real implementation will be in HS-907
 */
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

    /**
     * It returns a list of exporter interfaceIds
     *
     * @param requestCriteria
     * @param tenantId
     * @return
     */
    public List<Long> findExporters(RequestCriteria requestCriteria, String tenantId) {
        List<Long> exporters = new ArrayList<>();
        // fake interfaceId (long)
        exporters.add(1L);
        exporters.add(2L);
        return exporters;
    }

    public List<String> findApplications(RequestCriteria requestCriteria, String tenantId) {
        List<String> applications = new ArrayList<>();
        // fake applications
        applications.add("https");
        applications.add("ssh");
        return applications;
    }

    public Summaries getApplicationSummaries(RequestCriteria requestCriteria, String tenantId) {
        var summaries = Summaries.newBuilder();
        for (int i = 0; i < 10; i++) {
            summaries.addSummaries(TrafficSummary.newBuilder().setApplication("app_" + i)
                // fake in/out to make it looks like real
                .setBytesIn((getRandomBytes()))
                .setBytesOut((getRandomBytes())));
        }
        return summaries.build();
    }

    public Series getApplicationSeries(RequestCriteria requestCriteria, String tenantId) {
        Series.Builder series = Series.newBuilder();
        int seriesSize = 100;
        // generate 10 apps data with seriesSize of points, each step is 500ms
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < seriesSize; j++) {
                series.addPoint(
                    FlowingPoint.newBuilder()
                        .setApplication("app_" + i)
                        .setTimestamp(Timestamp.newBuilder().setSeconds(System.currentTimeMillis() - ((seriesSize - j) * 500)))
                        .setValue(getRandomBytes())
                        .setDirection(Direction.EGRESS));
            }
        }
        return series.build();
    }

    private long getRandomBytes() {
        return (long) (Math.random() * 100_000L);
    }
}
