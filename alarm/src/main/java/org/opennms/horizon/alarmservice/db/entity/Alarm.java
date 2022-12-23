/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Formula;
import org.opennms.horizon.alarmservice.model.AlarmSeverity;

@Entity
@Table(name="alarm")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Alarm extends TenantAwareEntity implements Serializable {
    private static final long serialVersionUID = 7275548439687562161L;

    public static final int PROBLEM_TYPE = 1;
    public static final int RESOLUTION_TYPE = 2;
    public static final int PROBLEM_WITHOUT_RESOLUTION_TYPE = 3;

    public static final String ARCHIVED = "Archived";


    @Id
    @SequenceGenerator(name="alarmSequence", sequenceName="alarmNxtId", allocationSize = 1)
    @GeneratedValue(generator="alarmSequence")
    @Column(nullable=false)
    private Long alarmId;

    @Column(length=256, nullable=false)
    private String eventUei;

    @Column(unique=true)
    private String reductionKey;

    @Column
    private Integer alarmType;

    @Column
    private Integer ifIndex;

    @Column(nullable=false)
    private Integer counter;

    @Column(nullable=false)
    @Enumerated(EnumType.ORDINAL)
    private AlarmSeverity severity = AlarmSeverity.INDETERMINATE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date firstEventTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date lastEventTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date firstAutomationTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date lastAutomationTime;

    @Column(length=4000)
    private String description;

    @Column(length=1024)
    private String logMsg;

    @Column
    private String operInstruct;

    @Column(length=64)
    private String mouseOverText;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date suppressedUntil;

    @Column(length=256)
    private String suppressedUser;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date suppressedTime;

    @Column(length=256)
    private String alarmAckUser;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date alarmAckTime;

    @Column
    private Long lastEventId;

    @Column
    private String clearUei;

    @Column
    private String clearKey;

    @Column(length=512)
    private String managedObjectInstance;

    @Column(length=512)
    private String managedObjectType;

    @Column(length=512)
    private String applicationDn;

    @Column(length=512)
    private String ossPrimaryKey;

    @Column(name="x733_alarm_type", length=31)
    private String x733AlarmType;

    @Column(length=31)
    private String qosAlarmState;

    @Column(name="x733_probable_cause", nullable=false)
    private int x733ProbableCause = 0;

    @Column
    private AlarmSeverity lastEventSeverity;

    public AlarmSeverity getLastEventSeverity() {
        return lastEventSeverity;
    }

    //========== fields with cross table relationships =========

    @ElementCollection
    @JoinTable(name="alarm_attributes", joinColumns = @JoinColumn(name="alarm_id"))
    @MapKeyColumn(name="attribute_name")
    @Column(name="attribute_value", nullable=false)
    private Map<String, String> details;

    @OneToOne(cascade=CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="sticky_memo_id")
    @Exclude
    private Memo stickyMemo;

    @OneToMany(mappedBy = "situationAlarmId", orphanRemoval = true, cascade = CascadeType.ALL)
    @Exclude
    private Set<AlarmAssociation> associatedAlarms = new HashSet<>();

    // a situation is an alarm, but an alarm is not necessarily a situation
    // a situation may contain many alarms
    // a situation has a set of alarms
    // if an alarm is part of a situation, related situation will be non-empty
    @ElementCollection
    @JoinTable(name = "alarm_association", joinColumns = @JoinColumn(name = "related_alarm_id"),
        inverseJoinColumns = @JoinColumn(name = "situation_alarm_id"))
    @Column(name="alarm_id", nullable=false)
    private Set<Alarm> relatedSituations = new HashSet<>();

    @Formula(value = "(SELECT COUNT(*)>0 FROM ALARM_ASSOCIATION S WHERE S.SITUATION_ALARM_ID=ALARM_ID)")
    private boolean situation;

    @Formula(value = "(SELECT COUNT(*)>0 FROM ALARM_ASSOCIATION S WHERE S.RELATED_ALARM_ID=ALARM_ID)")
    private boolean partOfSituation;

    /**
     * minimal constructor
     *
     * @param alarmid a {@link Integer} object.
     * @param eventuei a {@link String} object.
     * @param counter a {@link Integer} object.
     * @param severity a {@link Integer} object.
     * @param firsteventtime a {@link Date} object.
     */
    public Alarm(Long alarmid, String eventuei, Integer counter, Integer severity, Date firsteventtime, Date lasteEventTime) {
        this.alarmId = alarmid;
        this.eventUei = eventuei;
        this.counter = counter;
        this.severity = AlarmSeverity.get(severity);
        this.firstEventTime = firsteventtime;
        setLastEventTime(lasteEventTime);
    }

    @Transient
    public void incrementCount() {
        counter++;
    }

    @Transient
    public String getSeverityLabel() {
        return this.severity.name();
    }

    public void setSeverityLabel(final String label) {
        severity = AlarmSeverity.get(label);
    }

    @Transient
    public Integer getSeverityId() {
        return this.severity.getId();
    }

    public void setSeverityId(final Integer severity) {
        this.severity = AlarmSeverity.get(severity);
    }

    @Transient
    public boolean isAcknowledged() {
        return getAlarmAckUser() != null;
    }

    //TODO: Maybe we DO need the lastEvent?
//    @Transient
//    @XmlElementWrapper(name="parameters")
//    @XmlElement(name="parameter")
//    public List<OnmsEventParameter> getEventParameters() {
//        return m_lastEvent != null ? m_lastEvent.getEventParameters() : null;
//    }

//    public Optional<OnmsEventParameter> findEventParameter(final String name) {
//        return this.getEventParameters().stream().filter(p -> Objects.equals(name, p.getName())).findAny();
//    }
//
//    public String getEventParameter(final String name) {
//        return this.getEventParameters().stream().filter(p -> Objects.equals(name, p.getName())).findAny().map(OnmsEventParameter::getValue).orElse(null);
//    }

    /**
     * <p>toString</p>
     *
     * @return a {@link String} object.
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("alarmid", getAlarmId())
            .add("uei", getEventUei())
            .add("severity", getSeverity())
            .add("lastEventTime",getLastEventTime())
            .add("counter", getCounter())
            .toString();
    }

    /**
     * This marks an alarm as archived and prevents it from being used again in during reduction.
     */
    public void archive() {
        qosAlarmState = ARCHIVED;
        severity = AlarmSeverity.CLEARED;
        reductionKey = getReductionKey() + ":ID:"+ getAlarmId();
    }

    // Alarms that are archived
    @Transient
    public boolean isArchived() {
        return ARCHIVED.equals(qosAlarmState);
    }

    /**
     * <p>getRelatedAlarms</p>
     *
     * @return a {@link Set} object.
     */
    @Transient
    
    public Set<Alarm> getRelatedAlarms() {
        return associatedAlarms.stream().map(AlarmAssociation::getRelatedAlarmId).collect(Collectors.toSet());
    }

    @Transient
    
    public Set<Long> getRelatedAlarmIds() {
        return getRelatedAlarms().stream()
                .map(Alarm::getAlarmId)
                .collect(Collectors.toSet());
    }

    
    @OneToMany(mappedBy = "situationAlarmId", orphanRemoval = true, cascade = CascadeType.ALL)
    public Set<AlarmAssociation> getAssociatedAlarms() {
        return associatedAlarms;
    }

    public void setAssociatedAlarms(Set<AlarmAssociation> alarms) {
        associatedAlarms = alarms;
        situation = !associatedAlarms.isEmpty();
    }

    public void setRelatedAlarms(Set<Alarm> alarms) {
        associatedAlarms.clear();
        alarms.forEach(relatedAlarm -> associatedAlarms.add(new AlarmAssociation(this, relatedAlarm)));
        situation = !associatedAlarms.isEmpty();
    }

    public void setRelatedAlarms(Set<Alarm> alarms, Date associationEventTime) {
        associatedAlarms.clear();
        alarms.forEach(relatedAlarm -> associatedAlarms.add(new AlarmAssociation(this, relatedAlarm, associationEventTime)));
        situation = !associatedAlarms.isEmpty();
    }

    public void addRelatedAlarm(Alarm alarm) {
        associatedAlarms.add(new AlarmAssociation(this, alarm));
        situation = !associatedAlarms.isEmpty();
    }

    public void removeRelatedAlarm(Alarm alarm) {
        associatedAlarms.removeIf(associatedAlarm -> associatedAlarm.getRelatedAlarmId().getAlarmId().equals(alarm.getAlarmId()));
        situation = !associatedAlarms.isEmpty();
    }

    public void removeRelatedAlarmWithId(Long relatedAlarmId) {
        associatedAlarms.removeIf(associatedAlarm -> associatedAlarm.getRelatedAlarmId().getAlarmId().equals(relatedAlarmId));
        situation = !associatedAlarms.isEmpty();
    }

    @Transient
    public Set<Long> getRelatedSituationIds() {
        return getRelatedSituations().stream()
                .map(Alarm::getAlarmId)
                .collect(Collectors.toSet());
    }

    public void setRelatedSituations(Set<Alarm> alarms) {
        relatedSituations = alarms;
        partOfSituation = !relatedSituations.isEmpty();
    }

    @Transient
    public Date getLastUpdateTime() {
        if (getLastAutomationTime() != null && getLastAutomationTime().compareTo(getLastEventTime()) > 0) {
            return getLastAutomationTime();
        }
        return getLastEventTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Alarm alarm = (Alarm) o;
        return alarmId != null && Objects.equals(alarmId, alarm.alarmId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
