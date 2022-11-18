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

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class MonitoringSystemGrpcTest extends GrpcTestBase {
    @Autowired
    private MonitoringSystemRepository systemRepo;
    @Autowired
    private MonitoringLocationRepository locationRepo;
    @Autowired
    private MonitoringSystemMapper mapper;
    private MonitoringSystem system1;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub serviceStub;

    private void initStub() {
        serviceStub = MonitoringSystemServiceGrpc.newBlockingStub(channel);
    }

    @BeforeEach
    public void setup() {
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
    }

    @AfterEach
    public void cleanup() {
        channel.shutdown();
        systemRepo.deleteAll();
        locationRepo.deleteAll();
    }

    @Test
    public void testListSystem() {
        setupGrpc();
        initStub();
        MonitoringSystemList systemList = serviceStub.listMonitoringSystem(Empty.newBuilder().build());
        assertThat(systemList).isNotNull();
        assertThat(systemList.getListList().size()).isEqualTo(2);
    }

    @Test
    public void testListSystemWithDifferentTenantId() {
        setupGrpcWithDifferentTenantID();
        initStub();
        MonitoringSystemList systemList = serviceStub.listMonitoringSystem(Empty.newBuilder().build());
        assertThat(systemList).isNotNull();
        assertThat(systemList.getListList().size()).isEqualTo(0);
    }

    @Test
    public void testGetBySystemId() {
        setupGrpc();
        initStub();
        MonitoringSystemDTO systemDTO = serviceStub.getMonitoringSystemById(StringValue.of(system1.getSystemId()));
        assertThat(systemDTO).isNotNull();
        assertThat(systemDTO).isEqualTo(mapper.modelToDTO(system1));
    }

    @Test
    public void testGetBySystemIdNotExist() {
        setupGrpc();
        initStub();
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, ()-> serviceStub.getMonitoringSystemById(StringValue.of("wrong systemId")));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    public void testGetBySystemIdWithWrongTenantId() {
        setupGrpcWithDifferentTenantID();
        initStub();
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, ()-> serviceStub.getMonitoringSystemById(StringValue.of(system1.getSystemId())));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    public void testListWithoutTenantId() {
        setupGrpcWithOutTenantID();
        initStub();
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> serviceStub.listMonitoringSystem(Empty.newBuilder().build()));
        assertThat(exception.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
    }
}
