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
package org.opennms.horizon.alertservice.grpc;

import com.google.protobuf.Timestamp;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertError;
import org.opennms.horizon.alerts.proto.AlertRequest;
import org.opennms.horizon.alerts.proto.AlertResponse;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.CountAlertResponse;
import org.opennms.horizon.alerts.proto.DeleteAlertResponse;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.entity.Node;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.LocationRepository;
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertGrpcService extends AlertServiceGrpc.AlertServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(AlertGrpcService.class);
    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final String SORT_BY_DEFAULT = "id";
    public static final int DURATION = 24;
    public static final String TENANT_ID_NOT_FOUND = "Tenant Id not found";
    private final AlertMapper alertMapper;
    private final AlertRepository alertRepository;
    private final NodeRepository nodeRepository;
    private final LocationRepository locationRepository;
    private final AlertService alertService;
    private final TenantLookup tenantLookup;

    @Override
    public void listAlerts(ListAlertsRequest request, StreamObserver<ListAlertsResponse> responseObserver) {
        // Extract the page size, page and sort values from the request
        int pageSize = request.getPageSize() != 0 ? request.getPageSize() : PAGE_SIZE_DEFAULT;
        int page = request.getPage();
        String sortBy = !request.getSortBy().isEmpty() ? request.getSortBy() : SORT_BY_DEFAULT;
        boolean sortAscending = request.getSortAscending();

        // Create a PageRequest object based on the page size, next page, filter, and sort parameters
        Sort.Direction sortDirection = sortAscending ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(sortDirection, sortBy));

        // Get Filters
        List<Date> timeRange = new ArrayList<>();
        List<Severity> severities = new ArrayList<>();
        List<String> filterNodeIds = new ArrayList<>();
        getFilter(request, timeRange, severities, filterNodeIds);

        Optional<String> lookupTenantId = tenantLookup.lookupTenantId(Context.current());
        try {
            Page<org.opennms.horizon.alertservice.db.entity.Alert> alertPage;
            if (filterNodeIds.isEmpty()) {
                alertPage = lookupTenantId
                        .map(tenantId -> alertRepository.findBySeverityInAndLastEventTimeBetweenAndTenantId(
                                severities, timeRange.get(0), timeRange.get(1), pageRequest, tenantId))
                        .orElseThrow();
            } else {
                alertPage = lookupTenantId
                        .map(tenantId -> alertRepository
                                .findBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(
                                        severities,
                                        timeRange.get(0),
                                        timeRange.get(1),
                                        ManagedObjectType.NODE,
                                        filterNodeIds,
                                        pageRequest,
                                        tenantId))
                        .orElseThrow();
            }

            Set<String> nodeIds = getNodeIds(alertPage);
            var nodes = getNodeLabels(nodeIds, lookupTenantId.get());

            List<Alert> alerts = alertPage.getContent().stream()
                    .map(dbAlert -> getEnrichAlertProto(nodes, dbAlert))
                    .toList();

            ListAlertsResponse.Builder responseBuilder =
                    ListAlertsResponse.newBuilder().addAllAlerts(alerts);

            // If there is a next page, add the page number to the response's next_page_token field
            if (alertPage.hasNext()) {
                responseBuilder.setNextPage(alertPage.nextPageable().getPageNumber());
            }

            // Set last_page_token
            responseBuilder.setLastPage(alertPage.getTotalPages() - 1);

            // Set total alerts
            responseBuilder.setTotalAlerts(alertPage.getTotalElements());

            // Build the final ListAlertsResponse object and send it to the client using the responseObserver
            ListAlertsResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            responseObserver.onNext(ListAlertsResponse.newBuilder()
                    .addAllAlerts(Collections.emptyList())
                    .setError(AlertError.newBuilder()
                            .setError(TENANT_ID_NOT_FOUND)
                            .build())
                    .build());
            responseObserver.onCompleted();
        }
    }

    private Alert getEnrichAlertProto(Map<Long, Node> nodes, org.opennms.horizon.alertservice.db.entity.Alert dbAlert) {
        var alertBuilder = Alert.newBuilder(alertMapper.toProto(dbAlert));
        alertBuilder.addRuleName(dbAlert.getAlertCondition().getRule().getName());
        alertBuilder.addPolicyName(
                dbAlert.getAlertCondition().getRule().getPolicy().getName());

        if (ManagedObjectType.NODE.equals(dbAlert.getManagedObjectType())) {
            String strNodeId = dbAlert.getManagedObjectInstance();
            try {
                Long nodeId = Long.parseLong(strNodeId);
                if (nodes.containsKey(nodeId)) {
                    Node node = nodes.get(nodeId);
                    alertBuilder.setNodeName(node.getNodeLabel());
                    if (node.getMonitoringLocationId() != 0) {
                        locationRepository
                                .findByIdAndTenantId(node.getMonitoringLocationId(), node.getTenantId())
                                .ifPresent(l -> alertBuilder.setLocation(l.getLocationName()));
                    }
                }
            } catch (NumberFormatException ex) {
                // Just swallow
            }
        }
        return alertBuilder.build();
    }

    private Map<Long, Node> getNodeLabels(Set<String> nodeIds, String tenantId) {
        Map<Long, Node> nodes = new HashMap<>();
        for (String strNodeId : nodeIds) {
            try {
                long nodeId = Long.parseLong(strNodeId);
                Optional<Node> node = nodeRepository.findByIdAndTenantId(nodeId, tenantId);
                node.ifPresent(value -> nodes.put(nodeId, value));
            } catch (NumberFormatException ex) {
                // Just swallow this.
            }
        }
        return nodes;
    }

    private Set<String> getNodeIds(Page<org.opennms.horizon.alertservice.db.entity.Alert> alerts) {
        return alerts.getContent().stream()
                .filter(alert -> ManagedObjectType.NODE.equals(alert.getManagedObjectType()))
                .map(alert -> alert.getManagedObjectInstance())
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteAlert(AlertRequest request, StreamObserver<DeleteAlertResponse> responseObserver) {
        var deleteAlertResponse = DeleteAlertResponse.newBuilder();
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        request.getAlertIdList().forEach(alertId -> {
            boolean success = alertService.deleteByIdAndTenantId(alertId, tenantId);
            if (success) {
                deleteAlertResponse.addAlertId(alertId).build();
            } else {
                AlertError alertError = AlertError.newBuilder()
                        .setAlertId(alertId)
                        .setError("Couldn't delete alert")
                        .build();
                deleteAlertResponse.addAlertError(alertError);
            }
        });

        responseObserver.onNext(deleteAlertResponse.build());
        responseObserver.onCompleted();
    }

    @Override
    public void acknowledgeAlert(AlertRequest request, StreamObserver<AlertResponse> responseObserver) {
        var alertResponse = AlertResponse.newBuilder();
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        request.getAlertIdList().forEach(alertId -> {
            Optional<Alert> alert = alertService.acknowledgeByIdAndTenantId(alertId, tenantId);
            if (alert.isPresent()) {
                alertResponse.addAlert(alert.get());
            } else {
                AlertError alertError = AlertError.newBuilder()
                        .setAlertId(alertId)
                        .setError("Couldn't acknowledged alert")
                        .build();
                alertResponse.addAlertError(alertError);
            }
        });

        responseObserver.onNext(alertResponse.build());
        responseObserver.onCompleted();
    }

    @Override
    public void unacknowledgeAlert(AlertRequest request, StreamObserver<AlertResponse> responseObserver) {
        var alertResponse = AlertResponse.newBuilder();
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        request.getAlertIdList().forEach(alertId -> {
            Optional<Alert> alert = alertService.unacknowledgeByIdAndTenantId(alertId, tenantId);
            if (alert.isPresent()) {
                alertResponse.addAlert(alert.get());
            } else {
                AlertError alertError = AlertError.newBuilder()
                        .setAlertId(alertId)
                        .setError("Couldn't unacknowledged alert")
                        .build();
                alertResponse.addAlertError(alertError);
            }
        });

        responseObserver.onNext(alertResponse.build());
        responseObserver.onCompleted();
    }

    @Override
    public void clearAlert(AlertRequest request, StreamObserver<AlertResponse> responseObserver) {
        var alertResponse = AlertResponse.newBuilder();
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        request.getAlertIdList().forEach(alertId -> {
            Optional<Alert> alert = alertService.clearByIdAndTenantId(alertId, tenantId);
            if (alert.isPresent()) {
                alertResponse.addAlert(alert.get());
            } else {
                AlertError alertError = AlertError.newBuilder()
                        .setAlertId(alertId)
                        .setError("Couldn't clear alert")
                        .build();
                alertResponse.addAlertError(alertError);
            }
        });

        responseObserver.onNext(alertResponse.build());
        responseObserver.onCompleted();
    }

    @Override
    public void escalateAlert(AlertRequest request, StreamObserver<AlertResponse> responseObserver) {
        var alertResponse = AlertResponse.newBuilder();
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        request.getAlertIdList().forEach(alertId -> {
            Optional<Alert> alert = alertService.escalateByIdAndTenantId(alertId, tenantId);
            if (alert.isPresent()) {
                alertResponse.addAlert(alert.get());
            } else {
                AlertError alertError = AlertError.newBuilder()
                        .setAlertId(alertId)
                        .setError("Couldn't escalate alert")
                        .build();
                alertResponse.addAlertError(alertError);
            }
        });

        responseObserver.onNext(alertResponse.build());
        responseObserver.onCompleted();
    }

    @Override
    public void countAlerts(ListAlertsRequest request, StreamObserver<CountAlertResponse> responseObserver) {
        List<Date> timeRange = new ArrayList<>();
        List<Severity> severities = new ArrayList<>();
        List<String> nodeIds = new ArrayList<>();
        getFilter(request, timeRange, severities, nodeIds);

        try {
            int count;
            if (nodeIds.isEmpty()) {
                count = tenantLookup
                        .lookupTenantId(Context.current())
                        .map(tenantId -> alertRepository.countBySeverityInAndLastEventTimeBetweenAndTenantId(
                                severities, timeRange.get(0), timeRange.get(1), tenantId))
                        .orElseThrow();
            } else {
                count = tenantLookup
                        .lookupTenantId(Context.current())
                        .map(tenantId -> alertRepository
                                .countBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(
                                        severities,
                                        timeRange.get(0),
                                        timeRange.get(1),
                                        ManagedObjectType.NODE,
                                        nodeIds,
                                        tenantId))
                        .orElseThrow();
            }
            responseObserver.onNext(
                    CountAlertResponse.newBuilder().setCount(count).build());
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            responseObserver.onNext(CountAlertResponse.newBuilder()
                    .setCount(-1)
                    .setError(AlertError.newBuilder().setError(TENANT_ID_NOT_FOUND))
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void alertCounts(
            com.google.protobuf.Empty request,
            io.grpc.stub.StreamObserver<org.opennms.horizon.alerts.proto.AlertCount> responseObserver) {
        try {
            String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
            var alertCount = alertService.getAlertsCount(tenantId);
            responseObserver.onNext(alertCount);
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            var status = Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage(TENANT_ID_NOT_FOUND)
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    private void getFilter(
            ListAlertsRequest request, List<Date> timeRange, List<Severity> severities, List<String> nodeIds) {
        Optional<String> lookupTenantId = tenantLookup.lookupTenantId(Context.current());
        request.getFiltersList().forEach(filter -> {
            if (filter.hasSeverity()) {
                severities.add(Severity.valueOf(filter.getSeverity().name()));
            }
            if (filter.hasTimeRange()) {
                timeRange.add(convertTimestampToDate(filter.getTimeRange().getStartTime()));
                timeRange.add(convertTimestampToDate(filter.getTimeRange().getEndTime()));
            }
            if (filter.hasNodeLabel()) {
                List<Node> nodes = lookupTenantId
                        .map(tenantId -> nodeRepository.findAllByNodeLabelAndTenantId(filter.getNodeLabel(), tenantId))
                        .orElseThrow();

                for (Node node : nodes) {
                    nodeIds.add(String.valueOf(node.getId()));
                }
            }
        });

        if (timeRange.isEmpty()) {
            getDefaultTimeRange(timeRange);
        }

        if (severities.isEmpty()) {
            getAllSeverities(severities);
        }
    }

    private static void getAllSeverities(List<Severity> severities) {
        severities.addAll(Arrays.asList(Severity.values()));
    }

    private static void getDefaultTimeRange(List<Date> timeRange) {
        Calendar calendar = Calendar.getInstance();
        Date endTime = calendar.getTime();
        calendar.add(Calendar.HOUR_OF_DAY, -DURATION);
        Date startTime = calendar.getTime();
        timeRange.add(startTime);
        timeRange.add(endTime);
    }

    private static Date convertTimestampToDate(Timestamp timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return Date.from(instant);
    }
}
