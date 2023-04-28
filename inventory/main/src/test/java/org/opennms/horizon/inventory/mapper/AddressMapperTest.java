package org.opennms.horizon.inventory.mapper;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.model.Address;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AddressMapperTest {

    private final AddressMapper addressMapper = new AddressMapperImpl();

    @Test
    void testProtoToAddress() {
        var dto = AddressDTO.newBuilder().setId(1L).setAddressLine1("addressLine1").setAddressLine2("addressLine2").setCity("testCity").setState("testState").setCountry("testCountry").setPostalCode("testPostalCode").build();
        var result = addressMapper.dtoToModel(dto);
        assertEquals(1L, result.getId());
        assertEquals("addressLine1", result.getAddressLine1());
        assertEquals("addressLine2", result.getAddressLine2());
        assertEquals("testCity", result.getCity());
        assertEquals("testState", result.getState());
        assertEquals("testCountry", result.getCountry());
        assertEquals("testPostalCode", result.getPostalCode());
    }

    @Test
    void testAddressToProto() {
        var model = new Address();
        model.setId(1L);
        model.setAddressLine1("addressLine1");
        model.setAddressLine2("addressLine2");
        model.setCity("testCity");
        model.setState("testState");
        model.setCountry("testCountry");
        model.setPostalCode("testPostalCode");
        var result = addressMapper.modelToDTO(model);
        assertEquals(1L, result.getId());
        assertEquals("addressLine1", result.getAddressLine1());
        assertEquals("addressLine2", result.getAddressLine2());
        assertEquals("testCity", result.getCity());
        assertEquals("testState", result.getState());
        assertEquals("testCountry", result.getCountry());
        assertEquals("testPostalCode", result.getPostalCode());
    }

    @Test
    void testAddressToProtoNull() {
        var model = new Address();
        model.setId(1L);
        var result = addressMapper.modelToDTO(model);
        assertEquals(1L, result.getId());
        assertEquals("", result.getAddressLine1());
        assertEquals("", result.getAddressLine2());
        assertEquals("", result.getCity());
        assertEquals("", result.getState());
        assertEquals("", result.getCountry());
        assertEquals("", result.getPostalCode());
    }

    @Test
    void testProtoToAddressNull() {
        var result = addressMapper.dtoToModel(AddressDTO.newBuilder().setId(1L).build());
        assertEquals(1L, result.getId());
        assertEquals("", result.getAddressLine1());
        assertEquals("", result.getAddressLine2());
        assertEquals("", result.getCity());
        assertEquals("", result.getState());
        assertEquals("", result.getCountry());
        assertEquals("", result.getPostalCode());
    }
}
