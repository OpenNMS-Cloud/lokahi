package org.opennms.horizon.notifications.repository;

import org.opennms.horizon.notifications.model.PagerDutyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagerDutyConfigRepository extends JpaRepository<PagerDutyConfig, Long> {
}
