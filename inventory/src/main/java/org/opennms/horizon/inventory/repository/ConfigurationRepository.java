package org.opennms.horizon.inventory.repository;

import java.util.List;

import org.opennms.horizon.inventory.model.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    List<Configuration> findByTenantId(String tenantId);

    List<Configuration> findByKey(String key);

    List<Configuration> findByTenantIdAndKey(String tenantId, String key);

    List<Configuration> findByLocation(String location);

    List<Configuration> findByTenantIdAndLocation(String tenantId, String location);

    List<Configuration> findByKeyAndLocation(String key, String location);

    List<Configuration> findByTenantIdAndKeyAndLocation(String tenantId, String key, String location);

    List<Configuration> findAll();
}
