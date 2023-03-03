package org.opennms.horizon.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.opennms.horizon.inventory.model.SnmpInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnmpInterfaceRepository extends JpaRepository<SnmpInterface, Long> {
    List<SnmpInterface> findByTenantId(String tenantId);

    Optional<SnmpInterface> findByNodeIdAndTenantIdAndIfIndex(long nodeId, String tenantId, int ifIndex);
}
