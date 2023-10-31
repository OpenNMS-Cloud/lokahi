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


package org.opennms.horizon.inventory.component;

import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class AlertClient {
    private final ManagedChannel channel;
    private final long deadline;

    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub monitorPolicyServiceBlockingStub;

    protected void initialStubs() {
        monitorPolicyServiceBlockingStub = MonitorPolicyServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public MonitorPolicyProto getPolicyById(long policyId, String tenantId){
        Metadata metadata = getMetadata(true, tenantId);

        return monitorPolicyServiceBlockingStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
            .getPolicyById(Int64Value.of(policyId));
    }

    private Metadata getMetadata(boolean bypassAuthorization, String tenantId){
        var metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(bypassAuthorization));
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        return metadata;
    }
}
