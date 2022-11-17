package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {
    List<MonitoredService> findByTenantId(String tenantId);

    @Query("SELECT ms " +
        "FROM MonitoredService ms " +
        "WHERE ms.tenantId = :tenantId " +
        "AND ms.monitoredServiceType = :monitoredServiceType " +
        "AND ms.ipInterface = :ipInterface")
    Optional<MonitoredService> findByTenantIdTypeAndIpInterface(@Param("tenantId") String tenantId,
                                                                @Param("monitoredServiceType") MonitoredServiceType monitoredServiceType,
                                                                @Param("ipInterface") IpInterface ipInterface);
}
