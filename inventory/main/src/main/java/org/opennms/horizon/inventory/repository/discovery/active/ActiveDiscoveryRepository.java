package org.opennms.horizon.inventory.repository.discovery.active;

import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveDiscoveryRepository extends JpaRepository<ActiveDiscovery, Long> {
    List<ActiveDiscovery> findByTenantIdOrderById(String tenantId);
    Optional<ActiveDiscovery> findByTenantIdAndId(String tenantId, long id);
}
