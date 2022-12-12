package org.opennms.horizon.datachoices.repository;

import org.opennms.horizon.datachoices.model.DataChoices;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataChoicesRepository extends JpaRepository<DataChoices, Long> {

    Optional<DataChoices> findByTenantId(String tenantId);
}
