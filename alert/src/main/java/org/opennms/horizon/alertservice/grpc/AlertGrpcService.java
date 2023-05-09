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

package org.opennms.horizon.alertservice.grpc;

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
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.service.AlertMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.google.protobuf.Timestamp;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertGrpcService extends AlertServiceGrpc.AlertServiceImplBase {
    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final String SORT_BY_DEFAULT = "id";
    public static final int DURATION = 24;
    public static final String TENANT_ID_NOT_FOUND = "Tenant Id not found";
    private final AlertMapper alertMapper;
    private final AlertRepository alertRepository;
    private final NodeRepository nodeRepository;
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
        getFilter(request, timeRange, severities);

        Optional<String> lookupTenantId = tenantLookup.lookupTenantId(Context.current());
        try {
            Page<org.opennms.horizon.alertservice.db.entity.Alert> alertPage = lookupTenantId
                .map(tenantId -> alertRepository.findBySeverityInAndLastEventTimeBetweenAndTenantId(severities, timeRange.get(0), timeRange.get(1), pageRequest, tenantId))
                .orElseThrow();

            Set<String> nodeIds = getNodeIds(alertPage);
            Map<Long, String> nodeLabels = getNodeLabels(nodeIds, lookupTenantId.get());
            insertNodeLabelsIntoAlerts(nodeLabels, alertPage);

            List<Alert> alerts = alertPage.getContent().stream()
                .map(alertMapper::toProto)
                .toList();

            ListAlertsResponse.Builder responseBuilder = ListAlertsResponse.newBuilder()
                .addAllAlerts(alerts);

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
            responseObserver.onNext(ListAlertsResponse.newBuilder().addAllAlerts(Collections.emptyList()).setError(AlertError.newBuilder().setError(TENANT_ID_NOT_FOUND).build()).build());
            responseObserver.onCompleted();
        }
    }

    private void insertNodeLabelsIntoAlerts(Map<Long, String> nodeLabels, Page<org.opennms.horizon.alertservice.db.entity.Alert> alerts) {
        for (org.opennms.horizon.alertservice.db.entity.Alert alert:alerts.getContent()) {
            if (ManagedObjectType.NODE.equals(alert.getManagedObjectType())) {
                String strNodeId = alert.getManagedObjectInstance();
                try {
                    Long nodeId = Long.parseLong(strNodeId);
                    if (nodeLabels.containsKey(nodeId)) {
                        String label = nodeLabels.get(nodeId);
                        alert.setNodeLabel(label);
                    }
                } catch (NumberFormatException ex) {
                    // Just swallow
                }
            }
        }
    }

    private Map<Long, String> getNodeLabels(Set<String> nodeIds, String tenantId) {
        Map<Long, String> nodeLabels = new HashMap<>();
        for (String strNodeId:nodeIds) {
            try {
                long nodeId = Long.parseLong(strNodeId);
                Optional<Node> node = nodeRepository.findByIdAndTenantId(nodeId, tenantId);
                node.ifPresent(value -> nodeLabels.put(nodeId, value.getNodeLabel()));
            } catch (NumberFormatException ex) {
                // Just swallow this.
            }
        }
        return nodeLabels;
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
        request.getAlertIdList().forEach(
            alertId -> {
                boolean success = alertService.deleteByIdAndTenantId(alertId, tenantId);
                if (success) {
                    deleteAlertResponse.addAlertId(alertId).build();
                } else {
                    AlertError alertError = AlertError.newBuilder().setAlertId(alertId).setError("Couldn't delete alert").build();
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
        request.getAlertIdList().forEach(
            alertId -> {
                Optional<Alert> alert = alertService.acknowledgeByIdAndTenantId(alertId, tenantId);
                if(alert.isPresent()) {
                    alertResponse.addAlert(alert.get());
                }
                else {
                    AlertError alertError = AlertError.newBuilder().setAlertId(alertId).setError("Couldn't acknowledged alert").build();
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
        request.getAlertIdList().forEach(
            alertId -> {
                Optional<Alert> alert = alertService.unacknowledgeByIdAndTenantId(alertId, tenantId);
                if(alert.isPresent()) {
                    alertResponse.addAlert(alert.get());
                }
                else {
                    AlertError alertError = AlertError.newBuilder().setAlertId(alertId).setError("Couldn't unacknowledged alert").build();
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
        request.getAlertIdList().forEach(
            alertId -> {
                Optional<Alert> alert = alertService.clearByIdAndTenantId(alertId, tenantId);
                if(alert.isPresent()) {
                    alertResponse.addAlert(alert.get());
                }
                else {
                    AlertError alertError = AlertError.newBuilder().setAlertId(alertId).setError("Couldn't clear alert").build();
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
        request.getAlertIdList().forEach(
            alertId -> {
                Optional<Alert> alert = alertService.escalateByIdAndTenantId(alertId, tenantId);
                if(alert.isPresent()) {
                    alertResponse.addAlert(alert.get());
                }
                else {
                    AlertError alertError = AlertError.newBuilder().setAlertId(alertId).setError("Couldn't escalate alert").build();
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
        getFilter(request, timeRange, severities);

        try {
            int count = tenantLookup.lookupTenantId(Context.current())
                .map(tenantId -> alertRepository.countBySeverityInAndLastEventTimeBetweenAndTenantId(severities, timeRange.get(0), timeRange.get(1), tenantId))
                .orElseThrow();
            responseObserver.onNext(CountAlertResponse.newBuilder().setCount(count).build());
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            responseObserver.onNext(CountAlertResponse.newBuilder().setCount(-1).setError(AlertError.newBuilder().setError(TENANT_ID_NOT_FOUND)).build());
            responseObserver.onCompleted();
        }
    }

    private static void getFilter(ListAlertsRequest request, List<Date> timeRange, List<Severity> severities) {
        request.getFiltersList().forEach(filter -> {
            if (filter.hasSeverity()) {
                severities.add(Severity.valueOf(filter.getSeverity().name()));
            }
            if (filter.hasTimeRange()) {
                timeRange.add(convertTimestampToDate(filter.getTimeRange().getStartTime()));
                timeRange.add(convertTimestampToDate(filter.getTimeRange().getEndTime()));
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
