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

package org.opennms.horizon.inventory.grpc;


import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.DiscoveryRequest;
import org.opennms.horizon.inventory.dto.DiscoveryServiceGrpc;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.icmp.contract.IcmpScannerRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.google.protobuf.InvalidProtocolBufferException;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.stub.MetadataUtils;


@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class DiscoveryGrpcItTest extends GrpcTestBase {

    private DiscoveryServiceGrpc.DiscoveryServiceBlockingStub serviceStub;
    private static TestTaskSetGrpcService testGrpcService;

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = DiscoveryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        testGrpcService.reset();
        afterTest();
    }

    @Test
    void testDiscoverServices() throws VerificationException, InvalidProtocolBufferException {
        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .discoverServices(DiscoveryRequest.newBuilder()
                .addIpAddresses("127.0.0.1")
                .addIpAddresses("127.0.0.2")
                .setLocation("Default")
                .setRequisitionName("requisition")
                .build());
        await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests().size(), Matchers.is(1));
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));

        List<TaskDefinition> taskDefinitions = testGrpcService.getRequests()
            .stream()
            .map(PublishTaskSetRequest::getTaskSet)
            .flatMap(taskSet -> taskSet.getTaskDefinitionList().stream())
            .filter(taskDefinition -> taskDefinition.getPluginName().equals("Discovery"))
            .collect(Collectors.toList());

        Assertions.assertThat(taskDefinitions)
            .hasSize(1)
            .extracting(taskDefinition -> taskDefinition.getConfiguration().unpack(IcmpScannerRequest.class))
            .extracting(icmpScannerRequest -> icmpScannerRequest.getHost(0),
                icmpScannerRequest -> icmpScannerRequest.getHost(1))
            .containsExactly(Tuple.tuple("127.0.0.1", "127.0.0.2"));

        assertThat(taskDefinitions)
            .extracting(td -> td.get(0).getId())
            .isEqualTo("requisition:requisition/ip=127.0.0.1/Default");
    }
}
