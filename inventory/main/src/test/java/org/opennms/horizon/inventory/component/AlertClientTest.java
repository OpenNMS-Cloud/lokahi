/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group; Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group; Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group; Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License;
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful;
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not; see:
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
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AlertClientTest {
    public final static GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static AlertClient client;
    private static MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase mockMonitorPolicyService;

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockMonitorPolicyService = mock(MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase.class, delegatesTo(
            new MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase() {
                @Override
                public void getPolicyById(Int64Value request, StreamObserver<MonitorPolicyProto> responseObserver) {
                    responseObserver.onNext(MonitorPolicyProto.newBuilder()
                        .setId(request.getValue()).build());
                    responseObserver.onCompleted();
                }
            }));

        when(mockMonitorPolicyService.bindService()).thenCallRealMethod();

        grpcCleanUp.register(InProcessServerBuilder.forName("AlertClientTest")
            .addService(mockMonitorPolicyService)
            .directExecutor().build().start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("AlertClientTest").directExecutor().build());
        client = new AlertClient(channel, 5000);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockMonitorPolicyService);
        reset(mockMonitorPolicyService);
    }

    @Test
    void testGetPolicyById() {
        long policyId = 100L;
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        MonitorPolicyProto result = client.getPolicyById(policyId, "tenantId");
        assertThat(result).isNotNull();
        verify(mockMonitorPolicyService).bindService();
        verify(mockMonitorPolicyService).getPolicyById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(policyId);
    }
}
