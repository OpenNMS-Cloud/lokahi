package org.opennms.horizon.db.model.mapper;

import org.opennms.horizon.db.model.OnmsAlarm;
import org.opennms.horizon.shared.dto.event.AlarmDTO;

public interface IAlarmMapper {
    AlarmDTO alarmToAlarmDTO(OnmsAlarm alarm);
}
