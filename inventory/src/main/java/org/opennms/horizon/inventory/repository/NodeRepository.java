package org.opennms.horizon.inventory.repository;

import org.opennms.horizon.inventory.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeRepository extends JpaRepository<Node, Long> {
    List<Node> findByTenantId(String tenantId);
    Optional<Node> findByIdAndTenantId(long id, String tenantID);
    List<Node> findByNodeLabel(String label);
}
