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
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.model.flows.TimeRange;
import org.opennms.horizon.server.service.grpc.InventoryClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class FlowClient {
    private final InventoryClient inventoryClient;

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
     * It only handles time ranges, tenantId and exporters.
     *
     * @param requestCriteria
     * @param tenantId
     * @return ListRequest.Builder
     */
    private ListRequest.Builder createBaseListRequest(RequestCriteria requestCriteria, String tenantId, String authHeader) {
        var listRequest = ListRequest.newBuilder()
            .setTenantId(tenantId).setLimit(requestCriteria.getCount());
        if (requestCriteria.getTimeRange() != null) {
            listRequest.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }
        if (requestCriteria.getExporter() != null) {
            requestCriteria.getExporter().forEach(e -> listRequest.addAllFilters(convertExporter(e, authHeader)));
        }
        return listRequest;
    }

    public List<Long> findExporters(RequestCriteria requestCriteria, String tenantId, String authHeader) {
        var listRequest = this.createBaseListRequest(requestCriteria, tenantId, authHeader);
        return exporterServiceStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getExporterInterfaces(listRequest.build())
            .getElementsList().stream().map(Long::parseLong).toList();
    }

    public List<String> findApplications(RequestCriteria requestCriteria, String tenantId, String authHeader) {
        var listRequest = this.createBaseListRequest(requestCriteria, tenantId, authHeader);
        if (requestCriteria.getApplications() != null) {
            requestCriteria.getApplications().forEach(a ->
                listRequest.addFilters(convertApplication(a))
            );
        }

        return applicationsServiceBlockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getApplications(listRequest.build())
            .getElementsList();
    }

    public Summaries getApplicationSummaries(RequestCriteria requestCriteria, String tenantId, String authHeader) {
        var summaryRequest = ApplicationSummariesRequest.newBuilder()
            .setTenantId(tenantId).setCount(requestCriteria.getCount());

        summaryRequest.setIncludeOther(requestCriteria.isIncludeOther());

        // request.applications (exact match) are difference than ApplicationFilter (wildcard)
        if (requestCriteria.getApplications() != null) {
            requestCriteria.getApplications().forEach(summaryRequest::addApplications);
        }

        if (requestCriteria.getExporter() != null) {
            requestCriteria.getExporter().forEach(e -> summaryRequest.addAllFilters(convertExporter(e, authHeader)));
        }

        if (requestCriteria.getTimeRange() != null) {
            summaryRequest.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }

        return applicationsServiceBlockingStub
            .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getApplicationSummaries(summaryRequest.build());
    }

    public Series getApplicationSeries(RequestCriteria requestCriteria, String tenantId, String authHeader) {
        var seriesRequest = ApplicationSeriesRequest.newBuilder()
            .setTenantId(tenantId).setStep(requestCriteria.getStep()).setCount(requestCriteria.getCount());

        if (requestCriteria.getApplications() != null) {
            requestCriteria.getApplications().forEach(seriesRequest::addApplications);
        }

        if (requestCriteria.getExporter() != null) {
            requestCriteria.getExporter().forEach(e -> seriesRequest.addAllFilters(convertExporter(e, authHeader)));
        }

        if (requestCriteria.getTimeRange() != null) {
            seriesRequest.addFilters(convertTimeRage(requestCriteria.getTimeRange()));
        }

        return applicationsServiceBlockingStub
            .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getApplicationSeries(seriesRequest.build());
    }

    private Filter getExporterFilter(long id){
        return Filter.newBuilder().setExporter(
            ExporterFilter.newBuilder().setExporter(
                org.opennms.dataplatform.flows.querier.v1.Exporter.newBuilder().setInterfaceId(id))).build();
    }

    private List<Filter> convertExporter(org.opennms.horizon.server.model.flows.ExporterFilter exporter, String authHeader) {
        List<Filter> exporterList = new ArrayList<>();
        if (exporter.getIpInterfaceId() != null) {
            exporterList.add(getExporterFilter(exporter.getIpInterfaceId()));
        } else if (exporter.getNodeId() != null) {
            var nodeDTO = inventoryClient.getNodeById(exporter.getNodeId(), authHeader);
            if (nodeDTO != null) {
                nodeDTO.getIpInterfacesList().forEach(i -> exporterList.add(getExporterFilter(i.getId())));
            }
        }

        return exporterList;
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
