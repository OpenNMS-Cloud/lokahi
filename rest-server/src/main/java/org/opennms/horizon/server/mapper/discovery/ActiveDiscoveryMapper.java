package org.opennms.horizon.server.mapper.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.server.model.inventory.discovery.active.ActiveDiscovery;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryMapper {
    private static final String AZURE_DISCOVERY_TYPE = "AZURE";
    private static final String ICMP_DISCOVERY_TYPE = "ICMP";
    private final IcmpActiveDiscoveryMapper icmpMapper;
    private final AzureActiveDiscoveryMapper azureMapper;
    private final ObjectMapper objectMapper;

    public ActiveDiscovery dtoToActiveDiscovery(ActiveDiscoveryDTO activeDiscoveryDTO) {
        ActiveDiscovery discovery = new ActiveDiscovery();
        if (activeDiscoveryDTO.hasAzure()) {
            discovery.setDetails(objectMapper.valueToTree(azureMapper.dtoToAzureActiveDiscovery(activeDiscoveryDTO.getAzure())));
            discovery.setDiscoveryType(AZURE_DISCOVERY_TYPE);
        } else if (activeDiscoveryDTO.hasIcmp()) {
            discovery.setDetails(objectMapper.valueToTree(icmpMapper.dtoToIcmpActiveDiscovery(activeDiscoveryDTO.getIcmp())));
            discovery.setDiscoveryType(ICMP_DISCOVERY_TYPE);
        } else {
            throw new RuntimeException("Invalid Active Discovery type returned");
        }
        return discovery;
    }
}
