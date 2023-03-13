package org.opennms.horizon.inventory.service.discovery.active;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryService {

    private final ActiveDiscoveryRepository repository;



}
