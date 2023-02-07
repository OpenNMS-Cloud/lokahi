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

    public void createOrUpdateFromScanResult(String tenantId, Node node, SnmpInterfaceResult result) {
        modelRepo.findByNodeIdAndTenantIdAndIfIndex(node.getId(), tenantId, result.getIfIndex())
            .ifPresentOrElse(snmpIf -> {
                mapper.updateFromScanResult(result, snmpIf);
                modelRepo.save(snmpIf);
            }, () -> {
                SnmpInterface snmpIf = mapper.scanResultToModel(result);
                snmpIf.setNode(node);
                snmpIf.setTenantId(tenantId);
                modelRepo.save(snmpIf);
            });
    }
}
