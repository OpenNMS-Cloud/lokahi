package org.opennms.horizon.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.opennms.horizon.inventory.dto.ConfigType;
import org.opennms.horizon.inventory.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    List<Configuration> findByTenantIdAndType(String tenantId, ConfigType type);

    List<Configuration> findByTenantIdAndLocationAndType(String tenantId, String location, ConfigType type);

    Optional<Configuration> getByTenantIdAndKeyAndType(String tenantId, String key, ConfigType type);
}
