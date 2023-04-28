package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.mapper.AddressMapper;
import org.opennms.horizon.inventory.repository.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressService {
    private static final Logger LOG = LoggerFactory.getLogger(AddressService.class);
    private final AddressRepository addressRepo;

    private final AddressMapper mapper;

    public AddressDTO create(AddressDTO dto) {
        LOG.info("Creating address dto: {}", dto);
        org.opennms.horizon.inventory.model.Address model = mapper.dtoToModel(dto);
        LOG.info("Creating address model: {}", model);
        return mapper.modelToDTO(addressRepo.save(model));
    }

    public AddressDTO update(AddressDTO dto) {
        org.opennms.horizon.inventory.model.Address model = mapper.dtoToModel(dto);
        return mapper.modelToDTO(addressRepo.save(model));
    }

    public void delete(Long id) {
        addressRepo.deleteById(id);
    }

    public List<AddressDTO> findAll() {
        return addressRepo.findAll()
            .stream().map(mapper::modelToDTO).toList();
    }

    public Optional<AddressDTO> findById(Long id) {
        return addressRepo.findById(id)
            .map(mapper::modelToDTO);
    }
}
