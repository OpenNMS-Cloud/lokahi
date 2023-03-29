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

import io.grpc.ManagedChannel;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.opennms.dataplatform.flows.querier.v1.ApplicationsServiceGrpc;
import org.opennms.dataplatform.flows.querier.v1.Direction;
import org.opennms.dataplatform.flows.querier.v1.Exporter;
import org.opennms.dataplatform.flows.querier.v1.ExporterServiceGrpc;
import org.opennms.dataplatform.flows.querier.v1.Filter;
import org.opennms.dataplatform.flows.querier.v1.FlowingPoint;
import org.opennms.dataplatform.flows.querier.v1.Series;
import org.opennms.dataplatform.flows.querier.v1.Summaries;
import org.opennms.dataplatform.flows.querier.v1.TrafficSummary;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.model.flows.TimeRange;
import org.opennms.horizon.server.model.inventory.IpInterface;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class FLowClientTest {
    private final ManagedChannel managedChannel = Mockito.mock(ManagedChannel.class);

    private FlowClient flowClient = new FlowClient(managedChannel, 600);
    private final ApplicationsServiceGrpc.ApplicationsServiceBlockingStub applicationsServiceBlockingStub
        = Mockito.mock(ApplicationsServiceGrpc.ApplicationsServiceBlockingStub.class);
    private final ExporterServiceGrpc.ExporterServiceBlockingStub exporterServiceStub
        = Mockito.mock(ExporterServiceGrpc.ExporterServiceBlockingStub.class);


    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        var applicationsServiceBlockingStubField = FlowClient.class.getDeclaredField("applicationsServiceBlockingStub");
        applicationsServiceBlockingStubField.setAccessible(true);
        applicationsServiceBlockingStubField.set(flowClient, applicationsServiceBlockingStub);

        var exporterServiceStubField = FlowClient.class.getDeclaredField("exporterServiceStub");
        exporterServiceStubField.setAccessible(true);
        exporterServiceStubField.set(flowClient, exporterServiceStub);

        Mockito.when(applicationsServiceBlockingStub.withDeadlineAfter(Mockito.anyLong(), Mockito.any())).thenReturn(applicationsServiceBlockingStub);
        Mockito.when(exporterServiceStub.withDeadlineAfter(Mockito.anyLong(), Mockito.any())).thenReturn(exporterServiceStub);
    }

    private final String tenantId = "testId";
    private long exporterInterfaceId = 1L;
    private String application = "http";
    private final int count = 20; // make sure it is not default value
    private final int bytesIn = 10;
    private final int bytesOut = 10;
    private final Instant startTime = Instant.now();
    private final Instant endTime = startTime.minus(1, ChronoUnit.HOURS);

    @Test
    public void testGetApplications() {
        var result = org.opennms.dataplatform.flows.querier.v1.List.newBuilder().addElements(application).build();
        Mockito.when(applicationsServiceBlockingStub.getApplications(ArgumentMatchers.argThat(request -> {
            this.checkTimeRangeFilters(request.getFiltersList());
            Assert.assertEquals(tenantId, request.getTenantId());
            Assert.assertEquals(count, request.getLimit());
            this.checkApplication(application, request.getFiltersList());
            return true;
        }))).thenReturn(result);

        RequestCriteria requestCriteria = this.getRequestCriteria();

        var list = flowClient.findApplications(requestCriteria, tenantId);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(application, list.get(0));
    }

    @Test
    public void testGetExporters() {
        var result = org.opennms.dataplatform.flows.querier.v1.List.newBuilder()
            .addElements(String.valueOf(exporterInterfaceId)).build();
        Mockito.when(exporterServiceStub.getExporterInterfaces(ArgumentMatchers.argThat(request -> {
            this.checkTimeRangeFilters(request.getFiltersList());
            Assert.assertEquals(tenantId, request.getTenantId());
            Assert.assertEquals(count, request.getLimit());
            this.checkExporter(Exporter.newBuilder().setInterfaceId(exporterInterfaceId).build(), request.getFiltersList());
            return true;
        }))).thenReturn(result);

        RequestCriteria requestCriteria = this.getRequestCriteria();

        var list = flowClient.findExporters(requestCriteria, tenantId);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(exporterInterfaceId, (long) list.get(0));
    }

    @Test
    public void testGetApplicationSummary() {
        var result = Summaries.newBuilder()
            .addSummaries(TrafficSummary.newBuilder().setApplication(application).setBytesIn(bytesIn).setBytesOut(bytesOut))
            .build();
        Mockito.when(applicationsServiceBlockingStub.getApplicationSummaries(ArgumentMatchers.argThat(request -> {
            this.checkTimeRangeFilters(request.getFiltersList());
            Assert.assertEquals(tenantId, request.getTenantId());
            Assert.assertEquals(count, request.getCount());
            this.checkApplication(application, request.getFiltersList());
            this.checkExporter(Exporter.newBuilder().setInterfaceId(exporterInterfaceId).build(), request.getFiltersList());
            return true;
        }))).thenReturn(result);

        RequestCriteria requestCriteria = this.getRequestCriteria();

        var summaries = flowClient.getApplicationSummaries(requestCriteria, tenantId);
        Assert.assertEquals(1, summaries.getSummariesCount());
        Assert.assertEquals(application, summaries.getSummaries(0).getApplication());
        Assert.assertEquals(bytesIn, summaries.getSummaries(0).getBytesIn());
        Assert.assertEquals(bytesOut, summaries.getSummaries(0).getBytesOut());
    }

    @Test
    public void testGetApplicationSeries() {
        var result = Series.newBuilder()
            .addPoint(FlowingPoint.newBuilder().setApplication(application).setValue(bytesIn).setDirection(Direction.INGRESS))
            .build();
        Mockito.when(applicationsServiceBlockingStub.getApplicationSeries(ArgumentMatchers.argThat(request -> {
            this.checkTimeRangeFilters(request.getFiltersList());
            Assert.assertEquals(tenantId, request.getTenantId());
            Assert.assertEquals(count, request.getCount());
            this.checkApplication(application, request.getFiltersList());
            this.checkExporter(Exporter.newBuilder().setInterfaceId(exporterInterfaceId).build(), request.getFiltersList());
            return true;
        }))).thenReturn(result);

        RequestCriteria requestCriteria = this.getRequestCriteria();

        var series = flowClient.getApplicationSeries(requestCriteria, tenantId);
        Assert.assertEquals(1, series.getPointCount());
        Assert.assertEquals(application, series.getPoint(0).getApplication());
        Assert.assertEquals(bytesIn, series.getPoint(0).getValue(), 0);
        Assert.assertEquals(Direction.INGRESS, series.getPoint(0).getDirection());
    }

    private RequestCriteria getRequestCriteria() {
        RequestCriteria requestCriteria = new RequestCriteria();
        var timeRage = new TimeRange();
        timeRage.setStartTime(startTime);
        timeRage.setEndTime(endTime);
        requestCriteria.setTimeRange(timeRage);
        requestCriteria.setCount(count);

        // application
        List<String> applications = new ArrayList<>();
        applications.add(application);
        requestCriteria.setApplications(applications);

        // exporter
        org.opennms.horizon.server.model.flows.Exporter exporter = new org.opennms.horizon.server.model.flows.Exporter();
        IpInterface ipInterface = new IpInterface();
        ipInterface.setId(exporterInterfaceId);
        exporter.setIpInterface(ipInterface);
        List<org.opennms.horizon.server.model.flows.Exporter> exporters = new ArrayList<>();
        exporters.add(exporter);
        requestCriteria.setExporter(exporters);

        return requestCriteria;
    }

    private void checkTimeRangeFilters(List<Filter> filters) {
        var timeRangeFilters = filters.stream().filter(f -> f.hasTimeRange()).toList();
        Assert.assertEquals(1, timeRangeFilters.size());

        var startTimestamp = timeRangeFilters.get(0).getTimeRange().getStartTime();
        Assert.assertEquals(startTime, Instant.ofEpochSecond(startTimestamp.getSeconds(), startTimestamp.getNanos()));

        var endTimestamp = timeRangeFilters.get(0).getTimeRange().getEndTime();
        Assert.assertEquals(endTime, Instant.ofEpochSecond(endTimestamp.getSeconds(), endTimestamp.getNanos()));
    }

    private void checkApplication(String application, List<Filter> filters) {
        var applicationFilter = filters.stream().filter(f -> f.hasApplication()).toList();

        Assert.assertEquals(1, applicationFilter.size());
        Assert.assertEquals(application, applicationFilter.get(0).getApplication().getApplication());
    }

    private void checkExporter(Exporter exporter, List<Filter> filters) {
        var exporterFilters = filters.stream().filter(f -> f.hasExporter()).toList();

        Assert.assertEquals(1, exporterFilters.size());
        Assert.assertEquals(exporter.getInterfaceId(), exporterFilters.get(0).getExporter().getExporter().getInterfaceId());
    }
}
