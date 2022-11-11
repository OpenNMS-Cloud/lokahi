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

package org.opennms.horizon.alarmservice.db.impl.entity;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * <p> Entity to store situations and their associated (related) alarms with other details like mappedTime </p>
 */
@Entity
@Table(name = "alarm_situations", uniqueConstraints={@UniqueConstraint(columnNames={"situation_id", "related_alarm_id"})})
@Getter
@Setter
public class AlarmAssociation extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 4115687014888009683L;

    @Id
    @SequenceGenerator(name="alarmSequence", sequenceName="alarmsNxtId", allocationSize = 1)
    @GeneratedValue(generator="alarmSequence")
    @Column(name="id", nullable=false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "situation_id")
    private Alarm situationAlarm;

    @OneToOne
    @JoinColumn(name = "related_alarm_id")
    private Alarm relatedAlarm;

    @Column(name = "mapped_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date mappedTime;

    public AlarmAssociation() {
    }

    public AlarmAssociation(Alarm situationAlarm, Alarm relatedAlarm) {
        this(situationAlarm, relatedAlarm, new Date());
    }

    public AlarmAssociation(Alarm situationAlarm, Alarm relatedAlarm, Date mappedTime) {
        this.mappedTime = mappedTime;
        this.situationAlarm = situationAlarm;
        this.relatedAlarm = relatedAlarm;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("situation", getSituationAlarm().getId())
                .add("alarm", getRelatedAlarm().getId())
                .add("time", getMappedTime())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlarmAssociation that = (AlarmAssociation) o;
        return Objects.equals(situationAlarm, that.situationAlarm) &&
                Objects.equals(relatedAlarm, that.relatedAlarm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(situationAlarm, relatedAlarm);
    }
}
