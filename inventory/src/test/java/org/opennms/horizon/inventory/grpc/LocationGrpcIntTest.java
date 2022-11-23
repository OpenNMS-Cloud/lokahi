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
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class LocationGrpcIntTest extends GrpcTestBase {
    private MonitoringLocation location1;
    private MonitoringLocation location2;
    @Autowired
    private MonitoringLocationRepository repo;
    @Autowired
    private MonitoringLocationMapper mapper;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub serviceStub;
    @BeforeEach
    public void prepareData(){
        location1 = new MonitoringLocation();
        location1.setLocation("test-location");
        location1.setTenantId(tenantId);
        repo.save(location1);

        location2 = new MonitoringLocation();
        location2.setLocation("test-location2");
        location2.setTenantId(tenantId);
        repo.save(location2);
    }

    @AfterEach
    public void cleanUp(){
        repo.deleteAll();
        channel.shutdown();
    }

    private void initStub(){
        serviceStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void testListLocations () {
        setupGrpc();
        initStub();
        MonitoringLocationList locationList = serviceStub.listLocations(Empty.newBuilder().build());
        assertThat(locationList).isNotNull();
        List<MonitoringLocationDTO> list = locationList.getLocationsList();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getLocation()).isEqualTo(location1.getLocation());
        assertThat(list.get(1).getLocation()).isEqualTo(location2.getLocation());
        assertThat(list.get(0).getTenantId()).isEqualTo(location1.getTenantId().toString());
        assertThat(list.get(1).getTenantId()).isEqualTo(location2.getTenantId().toString());
        assertThat(list.get(0).getId()).isGreaterThan(0L);
        assertThat(list.get(1).getId()).isGreaterThan(0L);
    }

    @Test
    public void testListLocationsWithWrongTenantId () {
        setupGrpcWithDifferentTenantID();
        initStub();
        setupGrpcWithDifferentTenantID();
        MonitoringLocationList locationList = serviceStub.listLocations(Empty.newBuilder().build());
        assertThat(locationList).isNotNull();
        List<MonitoringLocationDTO> list = locationList.getLocationsList();
        assertThat(list.size()).isEqualTo(0);
    }

    @Test
    public void testFindLocationByName() {
        setupGrpc();
        initStub();
        MonitoringLocationDTO locationDTO = serviceStub.getLocationByName(StringValue.of("test-location"));
        assertThat(locationDTO).isNotNull();
        assertThat(locationDTO.getId()).isGreaterThan(0L);
        assertThat(locationDTO.getLocation()).isEqualTo(location1.getLocation());
        assertThat(locationDTO.getTenantId()).isEqualTo(location1.getTenantId().toString());
    }

    @Test()
    public void testFindLocationByNameNotFound() {
        setupGrpc();
        initStub();
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.getLocationByName(StringValue.of("test-location3")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test()
    public void testFindLocationByNameInvalidTenantId() {
        setupGrpcWithDifferentTenantID();
        initStub();
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.getLocationByName(StringValue.of("test-location")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test()
    public void testFindLocationByNameWithoutTenantId() {
        setupGrpcWithOutTenantID();
        initStub();
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.getLocationByName(StringValue.of("test-location")));
        assertThat(exception.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.UNAUTHENTICATED);
    }
}
