/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.mapper.AddressMapper;
import org.opennms.horizon.inventory.model.Address;
import org.opennms.horizon.inventory.repository.AddressRepository;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(addressRepository, addressMapper);
    }

    @Test
    void testCreateAddress() {
        AddressDTO inputDto = AddressDTO.newBuilder().build();
        Address savedAddress = new Address();

        when(addressMapper.dtoToModel(inputDto)).thenReturn(savedAddress);
        when(addressRepository.save(savedAddress)).thenReturn(savedAddress);
        when(addressMapper.modelToDTO(savedAddress)).thenReturn(inputDto);

        AddressDTO resultDto = addressService.create(inputDto);

        verify(addressMapper, times(1)).dtoToModel(inputDto);
        verify(addressRepository, times(1)).save(savedAddress);
        verify(addressMapper, times(1)).modelToDTO(savedAddress);
    }

    @Test
    void testUpdateAddress() {
        AddressDTO inputDto = AddressDTO.newBuilder().build();
        Address savedAddress = new Address();

        when(addressMapper.dtoToModel(inputDto)).thenReturn(savedAddress);
        when(addressRepository.save(savedAddress)).thenReturn(savedAddress);
        when(addressMapper.modelToDTO(savedAddress)).thenReturn(inputDto);

        AddressDTO resultDto = addressService.update(inputDto);

        verify(addressMapper, times(1)).dtoToModel(inputDto);
        verify(addressRepository, times(1)).save(savedAddress);
        verify(addressMapper, times(1)).modelToDTO(savedAddress);
    }

    @Test
    void testDeleteAddress() {
        addressService.delete(1L);
        verify(addressRepository, times(1)).deleteById(1L);
    }

    @Test
    void testFindAll() {
        List<Address> addressList = List.of(new Address());
        when(addressRepository.findAll()).thenReturn(addressList);
        when(addressMapper.modelToDTO(addressList.get(0))).thenReturn(AddressDTO.newBuilder().build());
        addressService.findAll();
        verify(addressRepository, times(1)).findAll();
        verify(addressMapper, times(1)).modelToDTO(addressList.get(0));
    }

    @Test
    void testFindById() {
        Optional<Address> optionalAddress = Optional.of(new Address());
        when(addressRepository.findById(1L)).thenReturn(optionalAddress);
        when(addressMapper.modelToDTO(optionalAddress.get())).thenReturn(AddressDTO.newBuilder().build());
        addressService.findById(1L);
        verify(addressRepository, times(1)).findById(1L);
        verify(addressMapper, times(1)).modelToDTO(optionalAddress.get());
    }

}
