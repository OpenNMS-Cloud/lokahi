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
package org.opennms.horizon.server.service.grpc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.Filter;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alerts.proto.TimeRangeFilter;
import org.opennms.horizon.model.common.proto.Severity;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyProto;
import org.opennms.horizon.shared.alert.policy.MonitorPolicyServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import com.google.protobuf.Timestamp;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.UInt64Value;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlertsClient {
    public static final int DEFAULT_HOURS_DURATION = 24;
    private final ManagedChannel channel;
    private final long deadline;
    private final MonitorPolicyMapper policyMapper;

    private AlertServiceGrpc.AlertServiceBlockingStub alertStub;
    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub policyStub;

    protected void initialStubs() {
        alertStub = AlertServiceGrpc.newBlockingStub(channel);
        policyStub = MonitorPolicyServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public ListAlertsResponse listAlerts(int pageSize, String nextPage, List<String> severityFilters, long hours, String sortBy, boolean sortAscending, String accessToken) {
        Metadata metadata = getMetadata(accessToken);

        final var request = ListAlertsRequest.newBuilder();
        getTimeRangeFilter(hours, request);
        getSeverity(severityFilters, request);

        request.setPageSize(pageSize)
            .setNextPageToken(nextPage)
            .setSortBy(sortBy)
            .setSortAscending(sortAscending)
            .build();
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listAlerts(request.build());
    }

    public Alert acknowledgeAlert(long alertId, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).acknowledgeAlert(UInt64Value.of(alertId));
    }

    public Alert unacknowledgeAlert(long alertId, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).unacknowledgeAlert(UInt64Value.of(alertId));
    }

    public Alert clearAlert(long alertId, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).clearAlert(UInt64Value.of(alertId));
    }

    public Alert escalateAlert(long alertId, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).escalateAlert(UInt64Value.of(alertId));
    }

    public boolean deleteAlert(long alertId, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).deleteAlert(UInt64Value.of(alertId)).getValue();
    }

    public long countAlerts(List<String> severityFilter, long hours, String accessToken) {
        Metadata metadata = getMetadata(accessToken);

        ListAlertsRequest.Builder request = ListAlertsRequest.newBuilder();
        getTimeRangeFilter(hours, request);
        getSeverity(severityFilter, request);

        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).countAlerts(request
            .build()).getValue();
    }

    private static void getTimeRangeFilter(Long hours, ListAlertsRequest.Builder request) {
        long effectiveHours = hours != null ? hours : DEFAULT_HOURS_DURATION;

        Instant nowTime = Instant.now();
        Timestamp nowTimestamp = Timestamp.newBuilder()
            .setSeconds(nowTime.getEpochSecond())
            .setNanos(nowTime.getNano()).build();

        Instant thenTime = nowTime.minus(effectiveHours, ChronoUnit.HOURS);
        Timestamp thenTimestamp = Timestamp.newBuilder()
            .setSeconds(thenTime.getEpochSecond())
            .setNanos(thenTime.getNano()).build();

        request.addFilters(Filter.newBuilder().setTimeRange(TimeRangeFilter.newBuilder()
                .setStartTime(thenTimestamp)
                .setEndTime(nowTimestamp))
            .build());
    }

    private static void getSeverity(List<String> severityFilters, ListAlertsRequest.Builder request) {
        severityFilters.stream()
            .map(Severity::valueOf)
            .forEach(severity -> request.addFilters(Filter.newBuilder().setSeverity(severity).build()));
    }

    private static Metadata getMetadata(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return metadata;
    }

    public MonitorPolicy createMonitorPolicy(MonitorPolicy policy, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        MonitorPolicyProto newPolicy = policyStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).createPolicy(policyMapper.map(policy));
        return policyMapper.map(newPolicy);
    }

    public List<MonitorPolicy> listMonitorPolicies(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listPolicies(Empty.getDefaultInstance())
            .getPoliciesList().stream().map(policyMapper::map).toList();
    }

    public MonitorPolicy getMonitorPolicyById(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyMapper.map(policyStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getPolicyById(Int64Value.of(id)));
    }
}
