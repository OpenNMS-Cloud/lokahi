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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;


@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class LocationGrpcItTest extends GrpcTestBase {
    private MonitoringLocation location1;
    private MonitoringLocation location2;
    @Autowired
    private MonitoringLocationRepository repo;

    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub serviceStub;

    @BeforeEach
    public void prepareData() throws VerificationException {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            location1 = new MonitoringLocation();
            location1.setLocation("test-location");
            repo.save(location1);

            location2 = new MonitoringLocation();
            location2.setLocation("test-location2");
            repo.save(location2);
        });
        prepareServer();
        serviceStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        afterTest();
    }

    @Test
    void testListLocations () {
        MonitoringLocationList locationList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listLocations(Empty.newBuilder().build());
        assertThat(locationList).isNotNull();
        List<MonitoringLocationDTO> list = locationList.getLocationsList();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getLocation()).isEqualTo(location1.getLocation());
        assertThat(list.get(1).getLocation()).isEqualTo(location2.getLocation());
        assertThat(list.get(0).getTenantId()).isEqualTo(location1.getTenantId());
        assertThat(list.get(1).getTenantId()).isEqualTo(location2.getTenantId());
        assertThat(list.get(0).getId()).isPositive();
        assertThat(list.get(1).getId()).isPositive();
    }

    @Test
    void testListLocationsWithWrongTenantId () {
        MonitoringLocationList locationList = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .listLocations(Empty.newBuilder().build());
        assertThat(locationList).isNotNull();
        List<MonitoringLocationDTO> list = locationList.getLocationsList();
        assertThat(list.size()).isZero();
    }

    @Test
    void testFindLocationByName() {
        MonitoringLocationDTO locationDTO = serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getLocationByName(StringValue.of("test-location"));
        assertThat(locationDTO).isNotNull();
        assertThat(locationDTO.getId()).isPositive();
        assertThat(locationDTO.getLocation()).isEqualTo(location1.getLocation());
        assertThat(locationDTO.getTenantId()).isEqualTo(location1.getTenantId());
    }

    @Test()
    void testFindLocationByNameNotFound() {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getLocationByName(StringValue.of("test-location3")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test()
    void testFindLocationByNameInvalidTenantId() {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader)))
            .getLocationByName(StringValue.of("test-location")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test()
    void testFindLocationByNameWithoutTenantId() {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
            .getLocationByName(StringValue.of("test-location")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Missing tenant id");
    }

    @Test()
    void testFindLocationByNameWithoutHeader() {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.getLocationByName(StringValue.of("test-location")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Invalid access token");
    }

}
