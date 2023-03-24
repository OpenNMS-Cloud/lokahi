package org.opennms.horizon.notifications.repository;

import org.opennms.horizon.notifications.model.MonitoringPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringPolicyRepository extends JpaRepository<MonitoringPolicy, Long> {
    Optional<MonitoringPolicy> findByTenantIdAndId(String tenantId, long id);
}
