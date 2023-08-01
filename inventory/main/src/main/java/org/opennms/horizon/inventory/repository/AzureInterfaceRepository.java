package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.AzureInterface;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AzureInterfaceRepository extends JpaRepository<AzureInterface, Long> {
    List<AzureInterface> findByTenantId(String tenantId);

    Optional<AzureInterface> findByIdAndTenantId(long id, String tenantId);

    Optional<AzureInterface> findByTenantIdAndPublicIpId(String tenantId, String publicIpId);

}
