/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.alarmservice.db.entity;

import com.google.common.base.MoreObjects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p> Entity to store situations and their associated (related) alarms with other details like mappedTime </p>
 */
@Entity
//TODO:MMF need to fix this
//@Table(name = "alarm_association", uniqueConstraints={@UniqueConstraint(columnNames={"situation_alarm_id", "related_alarm_id"})})
@Table(name = "alarm_association")
@Getter
@Setter
public class AlarmAssociation extends TenantAwareEntity implements Serializable {

    private static final long serialVersionUID = 4115687014888009683L;

    @Id
    @SequenceGenerator(name="alarmAssociationSequence", sequenceName="alarmAssociationNxtId", allocationSize = 1)
    @GeneratedValue(generator="alarmAssociationSequence")
    @Column(nullable=false)
    private Long alarmAssociationId;

    @ManyToOne
    @JoinColumn(name="situation_alarm_id")
    private Alarm situationAlarmId;

    @OneToOne
    @JoinColumn(name="related_alarm_id")
    private Alarm relatedAlarmId;

    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date mappedTime;


    public AlarmAssociation() {
    }

    public AlarmAssociation(Alarm situationAlarmId, Alarm relatedAlarmId) {
        this(situationAlarmId, relatedAlarmId, new Date());
    }

    public AlarmAssociation(Alarm situationAlarmId, Alarm relatedAlarmId, Date mappedTime) {
        this.mappedTime = mappedTime;
        this.situationAlarmId = situationAlarmId;
        this.relatedAlarmId = relatedAlarmId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("situation", getSituationAlarmId().getAlarmId())
                .add("alarm", getRelatedAlarmId().getAlarmId())
                .add("time", getMappedTime())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlarmAssociation that = (AlarmAssociation) o;
        return Objects.equals(situationAlarmId, that.situationAlarmId) &&
                Objects.equals(relatedAlarmId, that.relatedAlarmId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(situationAlarmId, relatedAlarmId);
    }
}
