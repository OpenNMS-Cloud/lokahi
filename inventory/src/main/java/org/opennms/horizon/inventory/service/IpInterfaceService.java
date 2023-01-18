package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpInterfaceService {
    private final IpInterfaceRepository modelRepo;

    private final IpInterfaceMapper mapper;

    public List<IpInterfaceDTO> findByTenantId(String tenantId) {
        List<IpInterface> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }

    public Optional<IpInterfaceDTO> findByIpAddressAndLocationAndTenantId(String ipAddress, String location, String tenantId) {
        try {
            Optional<IpInterface> optional = modelRepo.findByIpAddressAndLocationAndTenantId(InetAddress.getByName(ipAddress), location, tenantId);
            return optional.map(mapper::modelToDTO);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
