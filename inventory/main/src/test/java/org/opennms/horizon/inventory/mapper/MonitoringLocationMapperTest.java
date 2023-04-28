package org.opennms.horizon.inventory.mapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.model.Address;
import org.opennms.horizon.inventory.model.MonitoringLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringLocationMapperTest {

    @InjectMocks
    MonitoringLocationMapperImpl mapper;

    @Mock
    AddressMapper addressMapper;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(addressMapper);
    }

    @Test
    void testProtoToLocation() {
        when(addressMapper.dtoToModel(any(AddressDTO.class))).thenReturn(new Address());
        var proto = MonitoringLocationDTO.newBuilder().setId(1L).setTenantId("testTenantId").setLocation("testLocationName").setGeoLocation(GeoLocation.newBuilder().setLatitude(1.0).setLongitude(2.0).build()).setAddress(AddressDTO.newBuilder().setId(1L).build()).build();
        var result = mapper.dtoToModel(proto);
        assertEquals(1L, result.getId());
        assertEquals("testLocationName", result.getLocation());
        assertEquals(1.0, result.getLatitude());
        assertEquals(2.0, result.getLongitude());
        assertEquals("testTenantId", result.getTenantId());
        assertEquals(1L, result.getAddressId());
        verify(addressMapper, times(1)).dtoToModel(any(AddressDTO.class));
    }

    @Test
    void testLocationToProto() {
        var model = new MonitoringLocation();
        model.setId(1L);
        model.setLocation("testLocationName");
        model.setLatitude(1.0);
        model.setLongitude(2.0);
        model.setTenantId("testTenantId");
        var result = mapper.modelToDTO(model);
        assertEquals(1L, result.getId());
        assertEquals("testLocationName", result.getLocation());
        assertEquals(1.0, result.getGeoLocation().getLatitude());
        assertEquals(2.0, result.getGeoLocation().getLongitude());
        assertEquals("testTenantId", result.getTenantId());
    }

}
