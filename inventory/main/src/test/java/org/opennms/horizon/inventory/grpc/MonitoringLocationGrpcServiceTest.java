package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.service.ConfigUpdateService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringLocationGrpcServiceTest {
    @InjectMocks
    private MonitoringLocationGrpcService grpcService;

    @Mock
    private MonitoringLocationService service;

    @Mock
    private TenantLookup tenantLookup;

    @Mock
    private ConfigUpdateService configUpdateService;

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

    private static final Long INVALID_LOCATION_ID = 404L;

    private static final String TENANT_ID = "tenantId";

    @Test
    void testListLocations() {
        Empty request = Empty.getDefaultInstance();
        List<MonitoringLocationDTO> expectedLocations = new ArrayList<>();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.findByTenantId(anyString())).thenReturn(expectedLocations);

        grpcService.listLocations(request, listResponseObserver);

        verify(listResponseObserver).onNext(listResponseCaptor.capture());
        verify(listResponseObserver).onCompleted();
        MonitoringLocationList response = listResponseCaptor.getValue();
        assertEquals(expectedLocations, response.getLocationsList());
    }

    @Test
    void testGetLocationByName() {
        StringValue locationName = StringValue.newBuilder().setValue("locationName").build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.findByLocationAndTenantId(anyString(), anyString())).thenReturn(Optional.of(expectedLocation));

        grpcService.getLocationByName(locationName, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    void testGetLocationById() {
        Int64Value request = Int64Value.newBuilder().setValue(1L).build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.getByIdAndTenantId(anyLong(), anyString())).thenReturn(Optional.of(expectedLocation));

        grpcService.getLocationById(request, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    void testListLocationsByIds() {
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
    void testSearchLocations() {
        StringValue request = StringValue.newBuilder().setValue("searchString").build();
        List<MonitoringLocationDTO> expectedLocations = new ArrayList<>();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.searchLocationsByTenantId(anyString(), anyString())).thenReturn(expectedLocations);

        grpcService.searchLocations(request, listResponseObserver);

        verify(listResponseObserver).onNext(listResponseCaptor.capture());
        verify(listResponseObserver).onCompleted();
        MonitoringLocationList response = listResponseCaptor.getValue();
        assertEquals(expectedLocations, response.getLocationsList());
    }

    @Test
    void testCreateLocation() throws LocationNotFoundException {
        MonitoringLocationCreateDTO request = MonitoringLocationCreateDTO.newBuilder().build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.upsert(any())).thenReturn(expectedLocation);

        grpcService.createLocation(request, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    void testCreateLocationException() throws LocationNotFoundException {
        MonitoringLocationCreateDTO request = MonitoringLocationCreateDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.upsert(any())).thenThrow(new RuntimeException("test exception"));

        grpcService.createLocation(request, getResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(getResponseObserver).onError(throwableCaptor.capture());
        assertEquals("INTERNAL: test exception", throwableCaptor.getValue().getMessage());
    }

    @Test
    void testUpdateLocation() throws LocationNotFoundException {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().build();
        MonitoringLocationDTO expectedLocation = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.upsert(any())).thenReturn(expectedLocation);

        grpcService.updateLocation(request, getResponseObserver);

        verify(getResponseObserver).onNext(getResponseCaptor.capture());
        verify(getResponseObserver).onCompleted();
        MonitoringLocationDTO response = getResponseCaptor.getValue();
        assertEquals(expectedLocation, response);
    }

    @Test
    void testUpdateLocationException() throws LocationNotFoundException {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));
        when(service.upsert(any())).thenThrow(new RuntimeException("test exception"));

        grpcService.updateLocation(request, getResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(getResponseObserver).onError(throwableCaptor.capture());
        assertEquals("INTERNAL: test exception", throwableCaptor.getValue().getMessage());
    }

    @Test
    void testUpdateInvalidLocationException() throws LocationNotFoundException {
        MonitoringLocationDTO request = MonitoringLocationDTO.newBuilder().setId(INVALID_LOCATION_ID).build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));

        when(service.upsert(argThat((dto) -> INVALID_LOCATION_ID.equals(dto.getId()))))
            .thenThrow(new LocationNotFoundException("test exception"));

        grpcService.updateLocation(request, getResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(getResponseObserver).onError(throwableCaptor.capture());
        assertEquals("NOT_FOUND: test exception", throwableCaptor.getValue().getMessage());
    }

    @Test
    void testDeleteLocation() {
        Int64Value request = Int64Value.newBuilder().setValue(1L).build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));

        grpcService.deleteLocation(request, deleteResponseObserver);

        verify(configUpdateService).removeConfigsFromTaskSet(TENANT_ID, 1L);
        verify(deleteResponseObserver).onNext(deleteResponseCaptor.capture());
        verify(deleteResponseObserver).onCompleted();
        BoolValue response = deleteResponseCaptor.getValue();
        assertEquals(BoolValue.of(true), response);
    }

    @Test
    void testDeleteLocationException() throws LocationNotFoundException {
        Int64Value request = Int64Value.newBuilder().setValue(1L).build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));

        doThrow(new RuntimeException("test exception")).when(service).delete(any(), any());

        grpcService.deleteLocation(request, deleteResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(deleteResponseObserver).onError(throwableCaptor.capture());
        assertEquals("INTERNAL: test exception", throwableCaptor.getValue().getMessage());
    }

    @Test
    void testDeleteInvalidLocationException() throws LocationNotFoundException {
        Int64Value request =  Int64Value.newBuilder().setValue(INVALID_LOCATION_ID).build();
        when(tenantLookup.lookupTenantId(any())).thenReturn(Optional.of(TENANT_ID));

        doThrow(new LocationNotFoundException("test exception")).when(service).delete(INVALID_LOCATION_ID, TENANT_ID);

        grpcService.deleteLocation(request, deleteResponseObserver);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(deleteResponseObserver).onError(throwableCaptor.capture());
        assertEquals("NOT_FOUND: test exception", throwableCaptor.getValue().getMessage());
    }
}
