package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.TenantCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByTenantId(String tenantId);
    Optional<Node> findByIdAndTenantId(long id, String tenantID);
    List<Node> findByNodeLabel(String label);
    List<Node> findByTenantIdAndMonitoredStateEquals(String tenantId, MonitoredState monitoredState);

    @Query("SELECT n " +
        "FROM Node n " +
        "WHERE n.tenantId = :tenantId " +
        "AND n.monitoringLocation.location = :location " +
        "AND n.nodeLabel = :nodeLabel ")
    Optional<Node> findByTenantLocationAndNodeLabel(@Param("tenantId") String tenantId,
                                                    @Param("location") String location,
                                                    @Param("nodeLabel") String nodeLabel);
    List<Node> findByIdInAndTenantId(List<Long> ids, String tenantId);


    @Query("SELECT n " +
        "FROM Node n " +
        "WHERE n.tenantId = :tenantId " +
        "AND n.monitoringLocation.location = :location " +
        "AND n.monitoredState = :monitoredState ")
    List<Node> findByTenantIdLocationsAndMonitoredStateEquals(@Param("tenantId") String tenantId,
                                                              @Param("location") String location,
                                                              @Param("monitoredState") MonitoredState monitoredState);

    @Query("SELECT new org.opennms.horizon.inventory.model.TenantCount(n.tenantId, count(*)) " +
        "FROM Node n " +
        "GROUP BY n.tenantId"
    )
    List<TenantCount> countNodesByTenant();
}
