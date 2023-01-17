/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

public class InventoryClientDeadlineTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static InventoryClient client;
    private static MockServerInterceptor mockInterceptor;
    private static MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase mockLocationService;
    private final String accessToken = "test-token";

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockInterceptor = new MockServerInterceptor();

        mockLocationService = mock(MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase.class, delegatesTo(
            new MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase() {
                @Override
                public void listLocations(Empty request, StreamObserver<MonitoringLocationList> responseObserver) {
                    CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS).execute(() -> {
                        responseObserver.onNext(MonitoringLocationList.newBuilder().build());
                        responseObserver.onCompleted();
                    });
                }
            }
        ));

        grpcCleanUp.register(InProcessServerBuilder.forName("InventoryClientDeadlineTest").intercept(mockInterceptor)
            .addService(mockLocationService)
            .directExecutor()
            .build()
            .start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("InventoryClientDeadlineTest").directExecutor().build());
        client = new InventoryClient(channel, 1000);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockLocationService);
        reset(mockLocationService);
        mockInterceptor.reset();
    }

    @Test
    public void testListLocation() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<Empty> captor = ArgumentCaptor.forClass(Empty.class);
        StatusRuntimeException thrown = assertThrows(
            StatusRuntimeException.class,
            () -> client.listLocations(accessToken + methodName),
            "Expected listLocations() to throw, but it didn't"
        );
        assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        verify(mockLocationService).listLocations(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    private static class MockServerInterceptor implements ServerInterceptor {
        private String authHeader;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            authHeader = headers.get(GrpcConstants.AUTHORIZATION_METADATA_KEY);
            return next.startCall(call, headers);
        }

        public String getAuthHeader() {
            return authHeader;
        }

        public void reset() {
            authHeader = null;
        }
    }

}
