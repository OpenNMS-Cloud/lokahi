package org.opennms.horizon.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

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
            Optional<IpInterface> optional = modelRepo.findByIpAddressAndLocationAndTenantId(InetAddressUtils.getInetAddress(ipAddress), location, tenantId);
            return optional.map(mapper::modelToDTO);
    }

    public void creatUpdateFromScanResult(String tenantId, Node node, IpInterfaceResult result, Map<Integer, SnmpInterface> ifIndexSNMPMap) {
        modelRepo.findByNodeIdAndTenantIdAndIpAddress(node.getId(), tenantId, InetAddressUtils.getInetAddress(result.getIpAddress()))
            .ifPresentOrElse(ipInterface -> {
                ipInterface.setHostname(result.getIpHostName());
                ipInterface.setNetmask(result.getNetmask());
                var snmpInterface = ifIndexSNMPMap.get(result.getIfIndex());
                if(snmpInterface != null) {
                    ipInterface.setSnmpInterface(snmpInterface);
                }
                modelRepo.save(ipInterface);
            }, () -> {
                IpInterface ipInterface = mapper.fromScanResult(result);
                ipInterface.setNode(node);
                ipInterface.setTenantId(tenantId);
                ipInterface.setSnmpPrimary(false);
                ipInterface.setHostname(result.getIpHostName());
                var snmpInterface = ifIndexSNMPMap.get(result.getIfIndex());
                if(snmpInterface != null) {
                    ipInterface.setSnmpInterface(snmpInterface);
                }
                modelRepo.save(ipInterface);
            });
    }
}
