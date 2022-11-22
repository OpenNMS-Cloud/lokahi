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

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NodeGrpcServiceTest {
    @InjectMocks
    NodeGrpcService nodeGrpcService;

    @Mock
    NodeService nodeService;

    @Mock
    IpInterfaceService ipInterfaceService;

    @Mock
    NodeMapper nodeMapper;

    @Mock
    TenantLookup tenantLookup;

    @Mock
    DetectorTaskSetService taskSetService;

    @Test
    public void createNode() {
        doReturn(Optional.of("ANY")).when(tenantLookup).lookupTenantId(any());

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .build();

        StreamObserver<NodeDTO> obs = mock(StreamObserver.class);

        nodeGrpcService.createNode(nodeCreateDTO, obs);

        verify(obs, times(0)).onError(any());
        verify(obs).onCompleted();

        verify(taskSetService, times(1)).sendDetectorTasks(any());
    }

    @Test
    public void createNodeBadIp() {
        doReturn(Optional.of("ANY")).when(tenantLookup).lookupTenantId(any());

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("BAD")
            .build();

        StreamObserver<NodeDTO> obs = mock(StreamObserver.class);

        nodeGrpcService.createNode(nodeCreateDTO, obs);

        verify(obs).onError(any());
        verify(obs, times(0)).onCompleted();
    }

    @Test
    public void createNodeDuplicateIp() {
        Optional<IpInterfaceDTO> interfaces = Optional.of(IpInterfaceDTO.newBuilder().build());

        doReturn(Optional.of("ANY")).when(tenantLookup).lookupTenantId(any());
        doReturn(interfaces).when(ipInterfaceService).findByIpAddressAndLocationAndTenantId(any(), any(), any());

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .build();

        StreamObserver<NodeDTO> obs = mock(StreamObserver.class);

        nodeGrpcService.createNode(nodeCreateDTO, obs);

        verify(obs).onError(any());
        verify(obs, times(0)).onCompleted();
    }
}
