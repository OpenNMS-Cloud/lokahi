package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringSystemRepository extends JpaRepository<MonitoringSystem, Long> {
    List<MonitoringSystem> findByTenantId(String tenantId);
    Optional<MonitoringSystem> findBySystemIdAndTenantId(String systemId, String tenantId);
    List<MonitoringSystem> findByMonitoringLocationIdAndTenantId(long locationId, String tenantId);
    Optional<MonitoringSystem> findByMonitoringLocationIdAndSystemIdAndTenantId(long locationId, String systemId, String tenantId);
    @Query("SELECT ms " +
        "FROM MonitoringSystem ms " +
        "WHERE ms.systemId = :systemId " +
        "AND ms.monitoringLocation.location = :locationName " +
        "AND ms.tenantId = :tenantId ")
    Optional<MonitoringSystem> findByMonitoringLocationAndSystemIdAndTenantId(@Param("locationName") String locationName,
                                                                              @Param("systemId") String systemId,
                                                                              @Param("tenantId") String tenantId);

    Optional<MonitoringSystem> findByIdAndTenantId(long id, String tenantId);
}
