package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.Node;
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

    @Query("SELECT n " +
        "FROM Node n " +
        "WHERE n.tenantId = :tenantId " +
        "AND n.monitoringLocation.location = :location " +
        "AND n.nodeLabel = :nodeLabel ")
    Optional<Node> findByTenantLocationAndNodeLabel(@Param("tenantId") String tenantId,
                                                    @Param("location") String location,
                                                    @Param("nodeLabel") String nodeLabel);
    List<Node> findByIdInAndTenantId(List<Long> ids, String tenantId);
}
