package org.opennms.horizon.inventory.repository;

import java.util.List;
import org.opennms.horizon.inventory.model.SyntheticTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyntheticTestRepository extends JpaRepository<SyntheticTest, Long> {
    List<SyntheticTest> findByTenantIdAndSyntheticTransactionId(String tenant, long syntheticTransactionId);

    void deleteByTenantIdAndId(String tenant, long id);

}
