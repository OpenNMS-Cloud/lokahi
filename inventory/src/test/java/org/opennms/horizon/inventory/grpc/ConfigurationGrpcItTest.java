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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.dto.ConfigurationKeyAndLocation;
import org.opennms.horizon.inventory.dto.ConfigurationList;
import org.opennms.horizon.inventory.dto.ConfigurationServiceGrpc;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;

@SpringBootTest
@ComponentScan({"org.opennms.horizon.inventory"})
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class ConfigurationGrpcItTest extends GrpcTestBase {
    private Configuration configuration1;
    private Configuration configuration2;
    @Autowired
    private ConfigurationRepository repo;

    private ConfigurationServiceGrpc.ConfigurationServiceBlockingStub serviceStub;

    @BeforeEach
    public void prepareData() throws VerificationException, JsonProcessingException {
        configuration1 = new Configuration();
        configuration1.setLocation("test-location1");
        configuration1.setTenantId(tenantId);
        configuration1.setKey("test-key1");
        configuration1.setValue(new ObjectMapper().readTree("{\"test\": \"value1\"}"));
        repo.save(configuration1);

        configuration2 = new Configuration();
        configuration2.setLocation("test-location1");
        configuration2.setTenantId(tenantId);
        configuration2.setKey("test-key2");
        configuration2.setValue(new ObjectMapper().readTree("{\"test\": \"value2\"}"));
        repo.save(configuration2);
        prepareServer();
        serviceStub = ConfigurationServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        repo.deleteAll();
        afterTest();
    }

    @Test
    void testListConfigurations () throws VerificationException {
       ConfigurationList configurationList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listConfigurations(Empty.newBuilder().build());
        assertThat(configurationList).isNotNull();
        List<ConfigurationDTO> list = configurationList.getConfigurationsList();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getLocation()).isEqualTo(configuration1.getLocation());
        assertThat(list.get(1).getLocation()).isEqualTo(configuration2.getLocation());
        assertThat(list.get(0).getTenantId()).isEqualTo(configuration1.getTenantId());
        assertThat(list.get(1).getTenantId()).isEqualTo(configuration2.getTenantId());
        assertThat(list.get(0).getKey()).isEqualTo(configuration1.getKey());
        assertThat(list.get(1).getKey()).isEqualTo(configuration2.getKey());
        assertThat(list.get(0).getValue().toString()).isEqualTo("{\"test\":\"value1\"}");
        assertThat(list.get(1).getValue().toString()).isEqualTo("{\"test\":\"value2\"}");
        assertThat(list.get(0).getId()).isPositive();
        assertThat(list.get(1).getId()).isPositive();
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testListConfigurationsWithWrongTenantId () throws VerificationException {
        ConfigurationList configurationList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .listConfigurations(Empty.newBuilder().build());
        assertThat(configurationList).isNotNull();
        List<ConfigurationDTO> list = configurationList.getConfigurationsList();
        assertThat(list.size()).isZero();
        verify(spyInterceptor).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testFindConfigurationByKey() throws VerificationException {
        ConfigurationList configurationList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listConfigurationsByTenantIdAndKey(StringValue.of("test-key1"));
        assertThat(configurationList).isNotNull();
        List<ConfigurationDTO> list = configurationList.getConfigurationsList();
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).getLocation()).isEqualTo(configuration1.getLocation());
        assertThat(list.get(0).getTenantId()).isEqualTo(configuration1.getTenantId());
        assertThat(list.get(0).getValue().toString()).isEqualTo("{\"test\":\"value1\"}");
        assertThat(list.get(0).getId()).isPositive();
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyNotFound() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listConfigurationsByTenantIdAndKey(StringValue.of("test-key3")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyInvalidTenantId() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .listConfigurationsByTenantIdAndKey(StringValue.of("test-key1")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        verify(spyInterceptor).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyWithoutTenantId() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
            .listConfigurationsByTenantIdAndKey(StringValue.of("test-key1")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Missing tenant id");
        verify(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyWithoutHeader() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.listConfigurationsByTenantIdAndKey(StringValue.of("test-location")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Invalid access token");
        verify(spyInterceptor).verifyAccessToken(null);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }


    @Test
    void testFindConfigurationByKeyAndLocation() throws VerificationException {
        ConfigurationList configurationList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listConfigurationsByTenantIdAndKeyAndLocation(ConfigurationKeyAndLocation.newBuilder().setKey("test-key1").setLocation("test-location1").build());
        assertThat(configurationList).isNotNull();
        List<ConfigurationDTO> list = configurationList.getConfigurationsList();
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.get(0).getLocation()).isEqualTo(configuration1.getLocation());
        assertThat(list.get(0).getTenantId()).isEqualTo(configuration1.getTenantId());
        assertThat(list.get(0).getValue().toString()).isEqualTo("{\"test\":\"value1\"}");
        assertThat(list.get(0).getId()).isPositive();
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyAndLocationNotFound() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listConfigurationsByTenantIdAndKeyAndLocation(ConfigurationKeyAndLocation.newBuilder().setKey("test-key3").setLocation("test-location1").build()));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyAndLocationInvalidTenantId() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .listConfigurationsByTenantIdAndKeyAndLocation(ConfigurationKeyAndLocation.newBuilder().setKey("test-key1").setLocation("test-location1").build()));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        verify(spyInterceptor).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyAndLocationWithoutTenantId() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
            .listConfigurationsByTenantIdAndKeyAndLocation(ConfigurationKeyAndLocation.newBuilder().setKey("test-key1").setLocation("test-location1").build()));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Missing tenant id");
        verify(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test()
    void testFindConfigurationByKeyAndLocationWithoutHeader() throws VerificationException {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.listConfigurationsByTenantIdAndKeyAndLocation(ConfigurationKeyAndLocation.newBuilder().setKey("test-key1").setLocation("test-location1").build()));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Invalid access token");
        verify(spyInterceptor).verifyAccessToken(null);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }
}
