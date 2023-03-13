package org.opennms.horizon.inventory.repository.discovery.active;

import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IcmpActiveDiscoveryRepository extends JpaRepository<IcmpActiveDiscovery, Long> {
    List<IcmpActiveDiscovery> findByLocationAndTenantId(String location, String tenantId);

    Optional<IcmpActiveDiscovery> findByLocationAndName(String location, String name);

    List<IcmpActiveDiscovery> findByNameAndTenantId(String name, String tenantId);

    List<IcmpActiveDiscovery> findByTenantId(String tenantId);

    Optional<IcmpActiveDiscovery> findByIdAndTenantId(long id, String tenantId);
}
