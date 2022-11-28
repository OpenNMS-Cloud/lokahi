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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemList;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class MonitoringSystemGrpcItTest extends GrpcTestBase {
    @Autowired
    private MonitoringSystemRepository systemRepo;
    @Autowired
    private MonitoringLocationRepository locationRepo;
    @Autowired
    private MonitoringSystemMapper mapper;
    private MonitoringSystem system1;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub serviceStub;

    @BeforeEach
    public void setup() throws VerificationException {
        MonitoringLocation location = new MonitoringLocation();
            location.setLocation("test-location");
            location.setTenantId(tenantId);
        locationRepo.save(location);

        system1 = new MonitoringSystem();
        system1.setSystemId("test-system-id-1");
        system1.setTenantId(tenantId);
        system1.setMonitoringLocation(location);
        system1.setMonitoringLocationId(location.getId());
        system1.setLabel("system1");
        system1.setLastCheckedIn(LocalDateTime.now());

        MonitoringSystem system2 = new MonitoringSystem();
        system2.setSystemId("test-system-id-2");
        system2.setTenantId(tenantId);
        system2.setMonitoringLocation(location);
        system2.setLabel("system2");
        system2.setLastCheckedIn(LocalDateTime.now());

        MonitoringSystem system3 = new MonitoringSystem();
        system3.setSystemId("test-system-id-3");
        system3.setTenantId(new UUID(5, 6).toString());
        system3.setMonitoringLocation(location);
        system3.setLabel("system3");
        system3.setLastCheckedIn(LocalDateTime.now());

        systemRepo.save(system1);
        systemRepo.save(system2);
        systemRepo.save(system3);
        prepareServer();
        serviceStub = MonitoringSystemServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanup() throws InterruptedException {
        systemRepo.deleteAll();
        locationRepo.deleteAll();
        afterTest();
    }

    @Test
    public void testListSystem() throws VerificationException {
        MonitoringSystemList systemList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listMonitoringSystem(Empty.newBuilder().build());
        assertThat(systemList).isNotNull();
        assertThat(systemList.getSystemsList().size()).isEqualTo(2);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    public void testListSystemWithDifferentTenantId() throws VerificationException {
        MonitoringSystemList systemList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .listMonitoringSystem(Empty.newBuilder().build());
        assertThat(systemList).isNotNull();
        assertThat(systemList.getSystemsList().size()).isEqualTo(0);
        verify(spyInterceptor).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    public void testGetBySystemId() throws VerificationException {
        MonitoringSystemDTO systemDTO = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getMonitoringSystemById(StringValue.of(system1.getSystemId()));
        assertThat(systemDTO).isNotNull();
        assertThat(systemDTO).isEqualTo(mapper.modelToDTO(system1));
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    public void testGetBySystemIdNotExist() throws VerificationException {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, ()-> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getMonitoringSystemById(StringValue.of("wrong systemId")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    public void testGetBySystemIdWithWrongTenantId() throws VerificationException {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, ()-> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .getMonitoringSystemById(StringValue.of(system1.getSystemId())));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        verify(spyInterceptor).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    public void testListWithoutTenantId() throws VerificationException {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
            .listMonitoringSystem(Empty.newBuilder().build()));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        verify(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }
}
