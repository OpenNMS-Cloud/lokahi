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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.shared.constants.GrpcConstants;

import com.google.protobuf.UInt64Value;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlertsClient {
    private final ManagedChannel channel;
    private final long deadline;

    private AlertServiceGrpc.AlertServiceBlockingStub alertStub;

    protected void initialStubs() {
        alertStub = AlertServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public List<Alert> listAlerts(int pageSize, String nextPage, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        ListAlertsRequest request = ListAlertsRequest.newBuilder().setPageSize(pageSize).setNextPageToken(nextPage).build();
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listAlerts(request).getAlertsList();
    }

    public Alert acknowledgeAlert(long alertId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).acknowledgeAlert(UInt64Value.of(alertId));
    }

    public Alert unacknowledgeAlert(long alertId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).unacknowledgeAlert(UInt64Value.of(alertId));
    }

    public Alert clearAlert(long alertId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).clearAlert(UInt64Value.of(alertId));
    }

    public Alert escalateAlert(long alertId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).escalateAlert(UInt64Value.of(alertId));
    }

    public boolean deleteAlert(long alertId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return alertStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).deleteAlert(UInt64Value.of(alertId)).getValue();
    }
}
