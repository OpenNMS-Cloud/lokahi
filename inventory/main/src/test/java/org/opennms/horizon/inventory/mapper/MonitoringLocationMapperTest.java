package org.opennms.horizon.inventory.mapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
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

    @Test
    void testProtoToLocation() {
        var proto = MonitoringLocationDTO.newBuilder()
            .setId(1L).setTenantId("testTenantId").setLocation("testLocationName")
            .setGeoLocation(GeoLocation.newBuilder().setLatitude(1.0).setLongitude(2.0).build())
            .setAddress("address").build();
        var result = mapper.dtoToModel(proto);
        assertEquals(1L, result.getId());
        assertEquals("testLocationName", result.getLocation());
        assertEquals(1.0, result.getLatitude());
        assertEquals(2.0, result.getLongitude());
        assertEquals(proto.getTenantId(), result.getTenantId());
        assertEquals(proto.getAddress(), result.getAddress());
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
