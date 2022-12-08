package org.opennms.horizon.alarmservice.service;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.opennms.horizon.alarmservice.db.entity.AlarmAssociation;
import org.opennms.horizon.alarmservice.model.AlarmAssociationDTO;
import org.opennms.horizon.alarmservice.model.AlarmDTO;

@Mapper(componentModel = "spring")
public interface AlarmMapper {

    AlarmMapper INSTANCE = Mappers.getMapper( AlarmMapper.class );

    AlarmDTO alarmToAlarmDTO(Alarm alarm);
    Alarm alarmDTOToAlarm(AlarmDTO alarmDTO);

    AlarmAssociationDTO alarmAssociationToAlarmAssociationDTO(AlarmAssociation alarmAssociation);
    AlarmAssociation alarmAssociationDTOToAlarmAssociation(AlarmAssociationDTO alarmAssociationDTO);

}
