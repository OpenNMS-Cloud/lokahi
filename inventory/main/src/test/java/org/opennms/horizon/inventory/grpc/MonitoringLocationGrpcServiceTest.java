package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.service.MonitoringLocationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class MonitoringLocationGrpcServiceTest {
    @InjectMocks
    private MonitoringLocationGrpcService grpcService;

    @Mock
    private MonitoringLocationService service;

    @Mock
    private TenantLookup tenantLookup;

    @Mock
    private StreamObserver<MonitoringLocationList> listResponseObserver;

    @Mock
    private StreamObserver<MonitoringLocationDTO> getResponseObserver;

    @Mock
    private StreamObserver<BoolValue> deleteResponseObserver;

    @Captor
    private ArgumentCaptor<MonitoringLocationList> listResponseCaptor;

    @Captor
    private ArgumentCaptor<MonitoringLocationDTO> getResponseCaptor;

    @Captor
    private ArgumentCaptor<BoolValue> deleteResponseCaptor;

    @Test
    public void testListLocations() {
        Empty request = Empty.getDefaultInstance();
        List<MonitoringLocationDTO> expectedLocations = new ArrayList<>();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.findByTenantId(anyString())).thenReturn(expectedLocations);

        grpcService.listLocations(request, listResponseObserver);

        verify(listResponseObserver).onNext(listResponseCaptor.capture());
        verify(listResponseObserver).onCompleted();
        MonitoringLocationList response = listResponseCaptor.getValue();
        assertEquals(expectedLocations, response.getLocationsList());
    }

    @Test
    public void testGetLocationByName() {
        StringValue locationName = StringValue.newBuilder().setValue("locationName").build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.findByLocationAndTenantId(anyString(), anyString())).thenReturn(Optional.of(expectedLocation));

        grpcService.getLocationByName(locationName, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    public void testGetLocationById() {
        Int64Value request = Int64Value.newBuilder().setValue(1L).build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.getByIdAndTenantId(anyLong(), anyString())).thenReturn(Optional.of(expectedLocation));

        grpcService.getLocationById(request, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    public void testListLocationsByIds() {
        IdList request = IdList.newBuilder().addIds(Int64Value.newBuilder().setValue(1L).build()).build();
        List<MonitoringLocationDTO> expectedLocations = new ArrayList<>();
        when(service.findByLocationIds(anyList())).thenReturn(expectedLocations);

        grpcService.listLocationsByIds(request, listResponseObserver);

        verify(listResponseObserver).onNext(listResponseCaptor.capture());
        verify(listResponseObserver).onCompleted();
        MonitoringLocationList response = listResponseCaptor.getValue();
        assertEquals(expectedLocations, response.getLocationsList());
    }

    @Test
    public void testSearchLocations() {
        StringValue request = StringValue.newBuilder().setValue("searchString").build();
        List<MonitoringLocationDTO> expectedLocations = new ArrayList<>();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.searchLocationsByTenantId(anyString(), anyString())).thenReturn(expectedLocations);

        grpcService.searchLocations(request, listResponseObserver);

        verify(listResponseObserver).onNext(listResponseCaptor.capture());
        verify(listResponseObserver).onCompleted();
        MonitoringLocationList response = listResponseCaptor.getValue();
        assertEquals(expectedLocations, response.getLocationsList());
    }

    @Test
    public void testCreateLocation() {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.upsert(any())).thenReturn(expectedLocation);

        grpcService.createLocation(request, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    public void testCreateLocationException() {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.upsert(any())).thenThrow(new RuntimeException("test exception"));

        grpcService.createLocation(request, getResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(getResponseObserver).onError(throwableCaptor.capture());
        assertEquals("INTERNAL: Error while creating location with name " + request.getLocation(), throwableCaptor.getValue().getMessage());
    }

    @Test
    public void testUpdateLocation() {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.upsert(any())).thenReturn(expectedLocation);

        grpcService.updateLocation(request, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    public void testUpdateLocationException() {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        when(service.upsert(any())).thenThrow(new RuntimeException("test exception"));

        grpcService.updateLocation(request, getResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(getResponseObserver).onError(throwableCaptor.capture());
        assertEquals("INTERNAL: Error while updating location with ID 0", throwableCaptor.getValue().getMessage());
    }

    @Test
    public void testDeleteLocation() {
        Int64Value request = Int64Value.newBuilder().setValue(1L).build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));

        grpcService.deleteLocation(request, deleteResponseObserver);

        verify(deleteResponseObserver).onNext(deleteResponseCaptor.capture());
        verify(deleteResponseObserver).onCompleted();
        BoolValue response = deleteResponseCaptor.getValue();
        assertEquals(BoolValue.of(true), response);
    }

    @Test
    public void testDeleteLocationException() {
        Int64Value request = Int64Value.newBuilder().setValue(1L).build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of("tenantId"));
        doThrow(new RuntimeException("test exception")).when(service).delete(any(), any());

        grpcService.deleteLocation(request, deleteResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(deleteResponseObserver).onError(throwableCaptor.capture());
        assertEquals("INTERNAL: Error while deleting location with ID 1", throwableCaptor.getValue().getMessage());
    }
}
