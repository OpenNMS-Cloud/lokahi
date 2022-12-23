package org.opennms.horizon.alarmservice.db.repository;

import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    Alarm findByReductionKey(String reductionKey);
}
