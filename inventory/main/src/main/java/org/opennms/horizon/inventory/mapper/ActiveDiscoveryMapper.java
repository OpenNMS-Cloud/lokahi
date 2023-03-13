package org.opennms.horizon.inventory.mapper;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryMapper {
    private final IcmpActiveDiscoveryMapper icmpActiveDiscoveryMapper;
    private final AzureActiveDiscoveryMapper azureActiveDiscoveryMapper;

    public List<ActiveDiscoveryDTO> modelToDto(List<ActiveDiscovery> list) {
        return list.stream().map(this::modelToDto).toList();
    }

    public ActiveDiscoveryDTO modelToDto(ActiveDiscovery discovery) {
        ActiveDiscoveryDTO.Builder builder = ActiveDiscoveryDTO.newBuilder();
        if (discovery instanceof IcmpActiveDiscovery icmp) {
            builder.setIcmp(icmpActiveDiscoveryMapper.modelToDto(icmp));
        } else if (discovery instanceof AzureActiveDiscovery azure) {
            builder.setAzure(azureActiveDiscoveryMapper.modelToDto(azure));
        }
        return builder.build();
    }
}
