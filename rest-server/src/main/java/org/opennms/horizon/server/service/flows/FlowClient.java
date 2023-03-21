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
import org.opennms.dataplatform.flows.querier.v1.ApplicationFilter;
import org.opennms.dataplatform.flows.querier.v1.ApplicationSeriesRequest;
import org.opennms.dataplatform.flows.querier.v1.ApplicationSummariesRequest;
import org.opennms.dataplatform.flows.querier.v1.ApplicationsServiceGrpc;
import org.opennms.dataplatform.flows.querier.v1.ExporterFilter;
import org.opennms.dataplatform.flows.querier.v1.ExporterServiceGrpc;
import org.opennms.dataplatform.flows.querier.v1.Filter;
import org.opennms.dataplatform.flows.querier.v1.ListRequest;
import org.opennms.dataplatform.flows.querier.v1.Series;
import org.opennms.dataplatform.flows.querier.v1.Summaries;
import org.opennms.dataplatform.flows.querier.v1.TimeRangeFilter;
import org.opennms.horizon.server.model.flows.Exporter;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.model.flows.TimeRange;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    public List<Long> findExporters(RequestCriteria requestCriteria, String tenantId) {
        var listRequest = ListRequest.newBuilder()
            .setTenantId(tenantId);
        if (requestCriteria.getTimeRange() != null) {
            listRequest.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }
        if (requestCriteria.getExporter() != null) {
            requestCriteria.getExporter().stream().forEach(e ->
                listRequest.addFilters(convertExporter(e))
            );
        }
        return exporterServiceStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getExporterInterfaces(listRequest.build())
            .getElementsList().stream().map(Long::parseLong).collect(Collectors.toList());
    }

    public List<String> findApplications(RequestCriteria requestCriteria, String tenantId) {
        var listRequest = ListRequest.newBuilder()
            .setTenantId(tenantId);
        if (requestCriteria.getTimeRange() != null) {
            listRequest.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }
        if (requestCriteria.getApplications() != null) {
            requestCriteria.getApplications().stream().forEach(a ->
                listRequest.addFilters(convertApplication(a))
            );
        }

        return applicationsServiceBlockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getApplications(listRequest.build())
            .getElementsList();
    }

    public Summaries getApplicationSummaries(RequestCriteria requestCriteria, String tenantId) {
        var request = ApplicationSummariesRequest.newBuilder()
            .setTenantId(tenantId);

        if (requestCriteria.getApplications() != null) {
            requestCriteria.getApplications().stream().forEach(a ->
                request.addFilters(convertApplication(a))
            );
        }

        if (requestCriteria.getExporter() != null) {
            requestCriteria.getExporter().stream().forEach(e ->
                request.addFilters(convertExporter(e))
            );
        }

        if (requestCriteria.getTimeRange() != null) {
            request.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }

        return applicationsServiceBlockingStub
            .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getApplicationSummaries(request.build());
    }

    public Series getApplicationSeries(RequestCriteria requestCriteria, String tenantId) {
        var request = ApplicationSeriesRequest.newBuilder()
            .setTenantId(tenantId);

        if (requestCriteria.getApplications() != null) {
            requestCriteria.getApplications().stream().forEach(a ->
                request.addFilters(convertApplication(a))
            );
        }

        if (requestCriteria.getExporter() != null) {
            requestCriteria.getExporter().stream().forEach(e ->
                request.addFilters(convertExporter(e))
            );
        }

        if (requestCriteria.getTimeRange() != null) {
            request.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }

        return applicationsServiceBlockingStub
            .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getApplicationSeries(request.build());
    }

    private Filter.Builder convertExporter(Exporter exporter) {
        var exporterRequest = org.opennms.dataplatform.flows.querier.v1.Exporter.newBuilder();
        if (exporter.getNode() != null) {
            exporterRequest.setNodeId(exporter.getNode().getId());
        }
        if (exporter.getIpInterface() != null) {
            exporterRequest.setInterfaceId(exporter.getIpInterface().getId());
        }

        return Filter.newBuilder().setExporter(
            ExporterFilter.newBuilder().setExporter(exporterRequest));
    }

    private Filter.Builder convertApplication(String application) {
        return Filter.newBuilder().setApplication(
            ApplicationFilter.newBuilder().setApplication(application));
    }

    private Filter.Builder convertTimeRage(TimeRange timeRange) {
        return Filter.newBuilder().setTimeRange(
            TimeRangeFilter.newBuilder()
                .setStartTime(Timestamp.newBuilder()
                    .setSeconds(timeRange.getStartTime().getEpochSecond())
                    .setNanos(timeRange.getStartTime().getNano()))
                .setEndTime(Timestamp.newBuilder()
                    .setSeconds(timeRange.getEndTime().getEpochSecond())
                    .setNanos(timeRange.getEndTime().getNano())));
    }
}
