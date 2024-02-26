/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.server.service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionServiceGrpc;
import org.opennms.horizon.alerts.proto.AlertRequest;
import org.opennms.horizon.alerts.proto.AlertResponse;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.CountAlertResponse;
import org.opennms.horizon.alerts.proto.DeleteAlertResponse;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.Filter;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alerts.proto.TimeRangeFilter;
import org.opennms.horizon.server.mapper.alert.AlertEventDefinitionMapper;
import org.opennms.horizon.server.mapper.alert.AlertsCountMapper;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.model.alerts.AlertCount;
import org.opennms.horizon.server.model.alerts.AlertEventDefinition;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.shared.constants.GrpcConstants;

@RequiredArgsConstructor
public class AlertsClient {
    private final ManagedChannel channel;
    private final long deadline;
    private final MonitorPolicyMapper policyMapper;
    private final AlertEventDefinitionMapper alertEventDefinitionMapper;
    private final AlertsCountMapper alertsCountMapper;

    private AlertServiceGrpc.AlertServiceBlockingStub alertStub;
    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub policyStub;
    private AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceBlockingStub alertEventDefinitionStub;

    protected void initialStubs() {
        alertStub = AlertServiceGrpc.newBlockingStub(channel);
        policyStub = MonitorPolicyServiceGrpc.newBlockingStub(channel);
        alertEventDefinitionStub = AlertEventDefinitionServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    @SuppressWarnings("squid:S107")
    public ListAlertsResponse listAlerts(
            int pageSize,
            int page,
            List<String> severityFilters,
            TimeRange timeRange,
            String sortBy,
            boolean sortAscending,
            String nodeLabel,
            String accessToken) {
        Metadata metadata = getMetadata(accessToken);

        final var request = ListAlertsRequest.newBuilder();
        getTimeRangeFilter(timeRange, request);
        getSeverity(severityFilters, request);
        if (nodeLabel != null) {
            request.addFilters(Filter.newBuilder().setNodeLabel(nodeLabel).build());
        }

        request.setPageSize(pageSize)
                .setPage(page)
                .setSortBy(sortBy)
                .setSortAscending(sortAscending)
                .build();
        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .listAlerts(request.build());
    }

    public AlertResponse acknowledgeAlert(List<Long> ids, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .acknowledgeAlert(AlertRequest.newBuilder().addAllAlertId(ids).build());
    }

    public AlertResponse unacknowledgeAlert(List<Long> ids, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .unacknowledgeAlert(AlertRequest.newBuilder().addAllAlertId(ids).build());
    }

    public AlertResponse clearAlert(List<Long> ids, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .clearAlert(AlertRequest.newBuilder().addAllAlertId(ids).build());
    }

    public AlertResponse escalateAlert(List<Long> ids, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .escalateAlert(AlertRequest.newBuilder().addAllAlertId(ids).build());
    }

    public DeleteAlertResponse deleteAlert(List<Long> ids, String accessToken) {
        Metadata metadata = getMetadata(accessToken);
        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .deleteAlert(AlertRequest.newBuilder().addAllAlertId(ids).build());
    }

    public CountAlertResponse countAlerts(List<String> severityFilter, TimeRange timeRange, String accessToken) {
        Metadata metadata = getMetadata(accessToken);

        ListAlertsRequest.Builder request = ListAlertsRequest.newBuilder();
        getTimeRangeFilter(timeRange, request);
        getSeverity(severityFilter, request);

        return alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .countAlerts(request.build());
    }

    private static void getTimeRangeFilter(TimeRange timeRange, ListAlertsRequest.Builder request) {
        TimeRangeFilter.Builder filterBuilder = TimeRangeFilter.newBuilder();
        Timestamp.Builder startTimeBuilder = Timestamp.newBuilder();
        Timestamp.Builder endTimeBuilder = Timestamp.newBuilder();

        switch (timeRange) {
            case TODAY:
                startTimeBuilder.setSeconds(getStartTime(TimeRange.TODAY));
                endTimeBuilder.setSeconds(getEndTime());
                break;
            case SEVEN_DAYS:
                startTimeBuilder.setSeconds(getStartTime(TimeRange.SEVEN_DAYS));
                endTimeBuilder.setSeconds(getEndTime());
                break;
            case LAST_24_HOURS:
                startTimeBuilder.setSeconds(getStartTime(TimeRange.LAST_24_HOURS));
                endTimeBuilder.setSeconds(getEndTime());
                break;
            case ALL:
                startTimeBuilder.setSeconds(0);
                endTimeBuilder.setSeconds(System.currentTimeMillis() / 1000);
                break;
            default:
                throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        filterBuilder.setStartTime(startTimeBuilder.build());
        filterBuilder.setEndTime(endTimeBuilder.build());

        request.addFilters(
                Filter.newBuilder().setTimeRange(filterBuilder.build()).build());
    }

    private static void getSeverity(List<String> severityFilters, ListAlertsRequest.Builder request) {
        if (severityFilters == null || severityFilters.isEmpty()) {
            return;
        }
        severityFilters.stream()
                .map(Severity::valueOf)
                .forEach(severity -> request.addFilters(
                        Filter.newBuilder().setSeverity(severity).build()));
    }

    private static Metadata getMetadata(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return metadata;
    }

    public MonitorPolicy createMonitorPolicy(MonitorPolicy policy, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        MonitorPolicyProto newPolicy = policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .createPolicy(policyMapper.map(policy));
        return policyMapper.map(newPolicy);
    }

    public List<MonitorPolicy> listMonitorPolicies(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .listPolicies(Empty.getDefaultInstance())
                .getPoliciesList()
                .stream()
                .map(policyMapper::map)
                .toList();
    }

    public MonitorPolicy getMonitorPolicyById(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyMapper.map(policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .getPolicyById(Int64Value.of(id)));
    }

    public List<AlertEventDefinition> listAlertEventDefinitions(EventType eventType, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);

        var request = ListAlertEventDefinitionsRequest.newBuilder()
                .setEventType(eventType)
                .build();

        return alertEventDefinitionStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .listAlertEventDefinitions(request)
                .getAlertEventDefinitionsList()
                .stream()
                // TODO: Remove this limitation of having name in event def to use all event def in LOK-2288
                .filter(eventDefProto -> Strings.isNotBlank(eventDefProto.getName()))
                .map(alertEventDefinitionMapper::protoToAlertEventDefinition)
                .toList();
    }

    public static long getStartTime(TimeRange timeRange) {
        LocalDate today = LocalDate.now();
        return switch (timeRange) {
            case TODAY -> today.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            case SEVEN_DAYS -> today.minusDays(6).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            case LAST_24_HOURS -> Instant.now().minusSeconds(24L * 60L * 60L).getEpochSecond();
            case ALL -> 0;
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        };
    }

    public static long getEndTime() {
        return Instant.now().getEpochSecond();
    }

    public MonitorPolicy getDefaultPolicy(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyMapper.map(policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .getDefaultPolicy(Empty.getDefaultInstance()));
    }

    public boolean deletePolicyById(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .deletePolicyById(Int64Value.of(id))
                .getValue();
    }

    public boolean deleteRuleById(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .deleteRuleById(Int64Value.of(id))
                .getValue();
    }

    public long countAlertByPolicyId(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .countAlertByPolicyId(Int64Value.of(id))
                .getValue();
    }

    public long countAlertByRuleId(Long id, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return policyStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .countAlertByRuleId(Int64Value.of(id))
                .getValue();
    }

    public AlertCount countAlerts(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        var alertCountProto = alertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .alertCounts(Empty.getDefaultInstance());
        return alertsCountMapper.protoToAlertCount(alertCountProto);
    }
}
