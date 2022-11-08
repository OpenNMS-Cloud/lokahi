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
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.InventoryApplication;
import org.opennms.horizon.inventory.dto.LocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringServiceGrpc;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.rpc.Code;
import com.google.rpc.Status;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = InventoryApplication.class)
@Testcontainers
public class GrpcIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("inventory").withUsername("inventory")
        .withPassword("password").withExposedPorts(5432);

    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("grpc.server.port", ()->6767);
    }

    private MonitoringLocationDTO location1;
    private MonitoringLocationDTO location2;

    @Autowired
    private MonitoringLocationRepository repo;
    @Autowired
    private MonitoringLocationMapper mapper;
    private ManagedChannel channel;
    private MonitoringServiceGrpc.MonitoringServiceBlockingStub serviceStub;

    @BeforeEach
    public void prepareData(){
        location1 = MonitoringLocationDTO.newBuilder()
            .setLocation("test-location")
            .setTenantId(new UUID(10, 10).toString())
            .build();
        repo.save(mapper.dtoToModel(location1));

        location2 = MonitoringLocationDTO.newBuilder()
            .setLocation("test-location2")
            .setTenantId(new UUID(10, 10).toString())
            .build();
        repo.save(mapper.dtoToModel(location2));
            channel = ManagedChannelBuilder.forAddress("localhost", 6767)
                .usePlaintext().build();
            serviceStub = MonitoringServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp(){
        repo.deleteAll();
        channel.shutdown();
    }

    @Test
    public void testListLocations () {
        LocationList locationList = serviceStub.listLocations(Empty.newBuilder().build());
        assertThat(locationList).isNotNull();
        List<MonitoringLocationDTO> list = locationList.getLocationsList();
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(0).getLocation()).isEqualTo(location1.getLocation());
        assertThat(list.get(1).getLocation()).isEqualTo(location2.getLocation());
        assertThat(list.get(0).getTenantId()).isEqualTo(location1.getTenantId());
        assertThat(list.get(1).getTenantId()).isEqualTo(location2.getTenantId());
        assertThat(list.get(0).getId()).isGreaterThan(0L);
        assertThat(list.get(1).getId()).isGreaterThan(0L);
    }

    @Test
    public void testFindLocationByName() {
        MonitoringLocationDTO locationDTO = serviceStub.getLocationByName(StringValue.of("test-location"));
        assertThat(locationDTO).isNotNull();
        assertThat(locationDTO.getId()).isGreaterThan(0L);
        assertThat(locationDTO.getLocation()).isEqualTo(location1.getLocation());
        assertThat(locationDTO.getTenantId()).isEqualTo(location1.getTenantId());
    }

    @Test()
    public void testFindLocationByNameNotFound() {
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.getLocationByName(StringValue.of("test-location3")));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.NOT_FOUND_VALUE);
    }

}
