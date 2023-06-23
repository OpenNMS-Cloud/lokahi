package org.opennms.horizon.inventory.repository;

import java.util.List;
import java.util.Optional;

import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitoringLocationRepository extends JpaRepository<MonitoringLocation, Long> {
    Optional<MonitoringLocation> findByLocation(String location);

    List<MonitoringLocation> findByTenantId(String tenantId);

    Optional<MonitoringLocation> findByLocationAndTenantId(String locationName, String tenantId);

    Optional<MonitoringLocation> findByIdAndTenantId(long id, String tenantId);

    List<MonitoringLocation> findByIdIn(List<Long> ids);

    List<MonitoringLocation> findByLocationContainingIgnoreCaseAndTenantId(String location, String tenantId);

}
