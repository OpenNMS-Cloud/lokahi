package org.opennms.horizon.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.node.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IpInterfaceService {
    private final IpInterfaceRepository repository;

    private final IpInterfaceMapper mapper;

    public void saveIpInterface(String tenantId,  Node node, String ipAddress) {
        IpInterface ipInterface = new IpInterface();
        ipInterface.setNode(node);
        ipInterface.setTenantId(tenantId);
        ipInterface.setIpAddress(InetAddressUtils.getInetAddress(ipAddress));
        ipInterface.setSnmpPrimary(true);
        repository.save(ipInterface);
        node.getIpInterfaces().add(ipInterface);
    }

    public List<IpInterfaceDTO> findByTenantId(String tenantId) {
        List<IpInterface> all = repository.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public Optional<IpInterfaceDTO> getByIdAndTenantId(long id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public Optional<IpInterfaceDTO> findByIpAddressAndLocationAndTenantId(String ipAddress, String location, String tenantId) {
            Optional<IpInterface> optional = repository.findByIpAddressAndLocationAndTenantId(InetAddressUtils.getInetAddress(ipAddress), location, tenantId);
            return optional.map(mapper::modelToDTO);
    }

    public void creatUpdateFromScanResult(String tenantId, Node node, IpInterfaceResult result, Map<Integer, SnmpInterface> ifIndexSNMPMap) {
        repository.findByNodeIdAndTenantIdAndIpAddress(node.getId(), tenantId, InetAddressUtils.getInetAddress(result.getIpAddress()))
            .ifPresentOrElse(ipInterface -> {
                ipInterface.setHostname(result.getIpHostName());
                ipInterface.setNetmask(result.getNetmask());
                var snmpInterface = ifIndexSNMPMap.get(result.getIfIndex());
                if(snmpInterface != null) {
                    ipInterface.setSnmpInterface(snmpInterface);
                }
                repository.save(ipInterface);
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
                repository.save(ipInterface);
            });
    }
}
