package org.opennms.horizon.inventory.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.model.Address;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.AddressRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringLocationServiceTest {

    @InjectMocks
    private MonitoringLocationService monitoringLocationService;

    @Mock
    private MonitoringLocationRepository modelRepo;

    @Mock
    private AddressRepository addressRepo;

    @Mock
    private MonitoringLocationMapper mapper;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(modelRepo, addressRepo, mapper);
    }

    @Test
    void testFindByTenantId() {
        // Mock data
        String tenantId = "testTenantId";
        List<MonitoringLocation> monitoringLocationList = new ArrayList<>();
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        when(modelRepo.findByTenantId(tenantId)).thenReturn(monitoringLocationList);
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(MonitoringLocationDTO.newBuilder().build());

        // Test
        List<MonitoringLocationDTO> result = monitoringLocationService.findByTenantId(tenantId);

        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(modelRepo, times(1)).findByTenantId(tenantId);
        verify(mapper, times(2)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testFindByLocationAndTenantId() {
        // Mock data
        String location = "testLocation";
        String tenantId = "testTenantId";
        MonitoringLocation monitoringLocation = new MonitoringLocation();
        when(modelRepo.findByLocationAndTenantId(location, tenantId)).thenReturn(Optional.of(monitoringLocation));
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(MonitoringLocationDTO.newBuilder().build());

        // Test
        Optional<MonitoringLocationDTO> result = monitoringLocationService.findByLocationAndTenantId(location, tenantId);

        // Assertions
        assertNotNull(result);
        assertTrue(result.isPresent());
        verify(modelRepo, times(1)).findByLocationAndTenantId(location, tenantId);
        verify(mapper, times(1)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testGetByIdAndTenantId() {
        // Mock data
        long id = 1L;
        String tenantId = "testTenantId";
        MonitoringLocation monitoringLocation = new MonitoringLocation();
        when(modelRepo.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(monitoringLocation));
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(MonitoringLocationDTO.newBuilder().build());

        // Test
        Optional<MonitoringLocationDTO> result = monitoringLocationService.getByIdAndTenantId(id, tenantId);

        // Assertions
        assertNotNull(result);
        assertTrue(result.isPresent());
        verify(modelRepo, times(1)).findByIdAndTenantId(id, tenantId);
        verify(mapper, times(1)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testFindByLocationIds() {
        // Mock data
        List<Long> ids = List.of(1L, 2L, 3L);
        List<MonitoringLocation> monitoringLocationList = new ArrayList<>();
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        when(modelRepo.findByIdIn(ids)).thenReturn(monitoringLocationList);
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(MonitoringLocationDTO.newBuilder().build());

        // Test
        List<MonitoringLocationDTO> result = monitoringLocationService.findByLocationIds(ids);

        // Assertions
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(modelRepo, times(1)).findByIdIn(ids);
        verify(mapper, times(3)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testFindAll() {
        // Mock data
        List<MonitoringLocation> monitoringLocationList = new ArrayList<>();
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        when(modelRepo.findAll()).thenReturn(monitoringLocationList);
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(MonitoringLocationDTO.newBuilder().build());

        // Test
        List<MonitoringLocationDTO> result = monitoringLocationService.findAll();

        // Assertions
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(modelRepo, times(1)).findAll();
        verify(mapper, times(3)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testSearchLocationsByTenantId() {
        // Mock data
        String tenantId = "testTenantId";
        String search = "testSearch";
        List<MonitoringLocation> monitoringLocationList = new ArrayList<>();
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        monitoringLocationList.add(new MonitoringLocation());
        when(modelRepo.findByLocationContainingIgnoreCaseAndTenantId(tenantId, search)).thenReturn(monitoringLocationList);
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(MonitoringLocationDTO.newBuilder().build());

        // Test
        List<MonitoringLocationDTO> result = monitoringLocationService.searchLocationsByTenantId(tenantId, search);

        // Assertions
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(modelRepo, times(1)).findByLocationContainingIgnoreCaseAndTenantId(tenantId, search);
        verify(mapper, times(3)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testUpsertAddressIsNull() {
        // Mock data
        MonitoringLocationDTO monitoringLocationDTO = MonitoringLocationDTO.newBuilder().build();
        MonitoringLocation monitoringLocation = new MonitoringLocation();
        when(mapper.dtoToModel(any(MonitoringLocationDTO.class))).thenReturn(monitoringLocation);
        when(modelRepo.save(monitoringLocation)).thenReturn(monitoringLocation);
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(monitoringLocationDTO);

        // Test
        MonitoringLocationDTO result = monitoringLocationService.upsert(monitoringLocationDTO);

        // Assertions
        assertNotNull(result);
        verify(mapper, times(1)).dtoToModel(any(MonitoringLocationDTO.class));
        verify(modelRepo, times(1)).save(monitoringLocation);
        verify(mapper, times(1)).modelToDTO(any(MonitoringLocation.class));
    }

    @Test
    void testUpsertAddressIsNotNull() {
        // Mock data
        MonitoringLocationDTO monitoringLocationDTO = MonitoringLocationDTO.newBuilder().build();
        MonitoringLocation monitoringLocation = new MonitoringLocation();
        Address address = new Address();
        address.setId(1L);
        monitoringLocation.setAddress(address);
        when(mapper.dtoToModel(any(MonitoringLocationDTO.class))).thenReturn(monitoringLocation);
        when(modelRepo.save(monitoringLocation)).thenReturn(monitoringLocation);
        when(mapper.modelToDTO(any(MonitoringLocation.class))).thenReturn(monitoringLocationDTO);
        when(addressRepo.findById(anyLong())).thenReturn(Optional.of(address));

        // Test
        MonitoringLocationDTO result = monitoringLocationService.upsert(monitoringLocationDTO);

        // Assertions
        assertNotNull(result);
        verify(mapper, times(1)).dtoToModel(any(MonitoringLocationDTO.class));
        verify(modelRepo, times(1)).save(monitoringLocation);
        verify(mapper, times(1)).modelToDTO(any(MonitoringLocation.class));
        verify(addressRepo, times(1)).findById(anyLong());
    }

    @Test
    void testDelete() {
        // Mock data
        long id = 1L;
        String tenantId = "testTenantId";
        MonitoringLocation monitoringLocation = new MonitoringLocation();
        when(modelRepo.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.of(monitoringLocation));

        // Test
        monitoringLocationService.delete(id, tenantId);

        // Assertions
        verify(modelRepo, times(1)).findByIdAndTenantId(id, tenantId);
        verify(modelRepo, times(1)).delete(monitoringLocation);
    }
}




