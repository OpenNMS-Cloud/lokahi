package org.opennms.horizon.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    List<Configuration> findByTenantId(String tenantId);

    Optional<Configuration> getByTenantIdAndKey(String tenantId, ConfigKey key);

    List<Configuration> findByTenantIdAndLocation(String tenantId, String location);

    List<Configuration> findAll();
}
