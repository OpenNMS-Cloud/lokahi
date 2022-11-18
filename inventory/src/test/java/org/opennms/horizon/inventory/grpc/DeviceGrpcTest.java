package org.opennms.horizon.inventory.grpc;

import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.inventory.dto.DeviceCreateDTO;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceGrpcTest {
    @InjectMocks
    DeviceGrpcService deviceGrpcService;

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
    public void createDevice() {
        doReturn(Optional.of("ANY")).when(tenantLookup).lookupTenantId(any());

        DeviceCreateDTO deviceCreateDTO = DeviceCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .build();

        StreamObserver<NodeDTO> obs = mock(StreamObserver.class);

        deviceGrpcService.createDevice(deviceCreateDTO, obs);

        verify(obs, times(0)).onError(any());
        verify(obs).onCompleted();

        verify(taskSetService, times(1)).sendDetectorTasks(any());
    }

    @Test
    public void createDeviceBadIp() {
        doReturn(Optional.of("ANY")).when(tenantLookup).lookupTenantId(any());

        DeviceCreateDTO deviceCreateDTO = DeviceCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("BAD")
            .build();

        StreamObserver<NodeDTO> obs = mock(StreamObserver.class);

        deviceGrpcService.createDevice(deviceCreateDTO, obs);

        verify(obs).onError(any());
        verify(obs, times(0)).onCompleted();
    }

    @Test
    public void createDeviceDuplicateIp() {
        List<IpInterfaceDTO> interfaces = Arrays.asList(IpInterfaceDTO.newBuilder().build());

        doReturn(Optional.of("ANY")).when(tenantLookup).lookupTenantId(any());
        doReturn(interfaces).when(ipInterfaceService).findByIpAddressAndLocationAndTenantId(any(), any(), any());

        DeviceCreateDTO deviceCreateDTO = DeviceCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .build();

        StreamObserver<NodeDTO> obs = mock(StreamObserver.class);

        deviceGrpcService.createDevice(deviceCreateDTO, obs);

        verify(obs).onError(any());
        verify(obs, times(0)).onCompleted();
    }
}
