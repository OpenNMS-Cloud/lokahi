package org.opennms.horizon.inventory.repository.discovery.active;

import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActiveDiscoveryRepository extends JpaRepository<ActiveDiscovery, Long> {
    Optional<ActiveDiscovery> findByTenantIdAndId(String tenantId, long id);
}
