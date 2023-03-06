package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.SnmpInterfaceRepository;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SnmpInterfaceService {
    private final SnmpInterfaceRepository modelRepo;

    private final SnmpInterfaceMapper mapper;

    public List<SnmpInterfaceDTO> findByTenantId(String tenantId) {
        List<SnmpInterface> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public SnmpInterface createOrUpdateFromScanResult(String tenantId, Node node, SnmpInterfaceResult result) {
        return modelRepo.findByNodeIdAndTenantIdAndIfIndex(node.getId(), tenantId, result.getIfIndex())
            .map(snmp -> {
               mapper.updateFromScanResult(result, snmp);
               modelRepo.save(snmp);
               return snmp;
            }).orElseGet(() -> {
                SnmpInterface snmp = mapper.scanResultToModel(result);
                snmp.setNode(node);
                snmp.setTenantId(tenantId);
                return modelRepo.save(snmp);
            });
    }
}
