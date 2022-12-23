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

package org.opennms.horizon.alarmservice.service;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alarmservice.api.AlarmEntityNotifier;
import org.opennms.horizon.alarmservice.api.AlarmService;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.opennms.horizon.alarmservice.db.entity.Memo;
import org.opennms.horizon.alarmservice.db.repository.AlarmRepository;
import org.opennms.horizon.alarmservice.model.AlarmDTO;
import org.opennms.horizon.alarmservice.model.AlarmSeverity;
import org.opennms.horizon.alarmservice.utils.SystemProperties;
import org.opennms.horizon.events.proto.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AlarmServiceImpl implements AlarmService {

    protected static final Integer THREADS = SystemProperties.getInteger("org.opennms.alarmd.threads", 4);

    public static final String ALARM_RULES_NAME = "alarm";

    protected static final String DEFAULT_USER = "admin";

    @Autowired
    AlarmRepository alarmRepository;

    @Autowired
    private AlarmEntityNotifier alarmEntityNotifier;

    @Autowired
    private AlarmMapper alarmMapper;

    @Override
    @Transactional
    public AlarmDTO clearAlarm(Alarm alarm, Date now) {
            log.info("Clearing alarm with id: {} with current severity: {} at: {}", alarm.getAlarmId(), alarm.getSeverity(), now);
            final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarm.getAlarmId());
            if (maybeAlarmInTrans.isEmpty()) {
                log.warn("Alarm disappeared: {}. Skipping clear.", alarm);
                return null;
            }
            Alarm alarmInTrans = maybeAlarmInTrans.get();
            final AlarmSeverity previousSeverity = alarmInTrans.getSeverity();
            alarmInTrans.setSeverity(AlarmSeverity.CLEARED);
            updateAutomationTime(alarmInTrans, now);
            alarmRepository.save(alarmInTrans);

            List<Alarm> associatedAlarms = alarm.getAssociatedAlarms().stream().map(alarmAssociation -> alarmAssociation.getRelatedAlarmId()).collect(
                Collectors.toList());

            associatedAlarms.forEach(associatedAlarm -> clearAlarm(associatedAlarm, now));

            alarmEntityNotifier.didUpdateAlarmSeverity(alarmInTrans, previousSeverity);

            return alarmMapper.alarmToAlarmDTO(alarmInTrans);
    }

    @Override
    @Transactional
    public AlarmDTO clearAlarm(Long alarmId, Date now) {
        return clearAlarm(alarmRepository.getById(alarmId), now);
    }

    @Override
    @Transactional
    public AlarmDTO deleteAlarm(Alarm alarm) {
        return deleteAlarm(alarm.getAlarmId());
    }

    @Override
    @Transactional
    public AlarmDTO deleteAlarm(Long id) {
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(id);

        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm with Id {}  disappeared. Skipping clear.", id);
            return null;
        }
        Alarm alarmInTrans = maybeAlarmInTrans.get();

        log.info("Deleting alarm with id: {} with severity: {}", alarmInTrans.getAlarmId(), alarmInTrans.getSeverity());

        // If alarm was in Situation, calculate notifications for the Situation
            Map<Alarm, Set<Alarm>> priorRelatedAlarms = new HashMap<>();
            if (alarmInTrans.isPartOfSituation()) {
                for (Alarm situation : alarmInTrans.getRelatedSituations()) {
                    priorRelatedAlarms.put(situation, new HashSet<Alarm>(situation.getRelatedAlarms()));
                }
            }
            alarmRepository.delete(alarmInTrans);
            // fire notifications after alarm has been deleted
            for (Entry<Alarm, Set<Alarm>> entry : priorRelatedAlarms.entrySet()) {
                alarmEntityNotifier.didUpdateRelatedAlarms(entry.getKey(), entry.getValue());
            }
            alarmEntityNotifier.didDeleteAlarm(alarmInTrans);

            return alarmMapper.alarmToAlarmDTO(alarmInTrans);
    }

    @Override
    @Transactional
    public AlarmDTO unclearAlarm(Alarm alarm, Date now) {
            log.info("Un-clearing alarm with id: {} at: {}", alarm.getAlarmId(), now);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarm.getAlarmId());
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarm);
            return null;
        }
        return doAlarmUnclear(now, maybeAlarmInTrans.get());
    }

    @Override
    @Transactional
    public AlarmDTO unclearAlarm(Long alarmId, Date now) {
        log.info("Un-clearing alarm with id: {} at: {}", alarmId, now);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarmId);
            return null;
        }
        return doAlarmUnclear(now, maybeAlarmInTrans.get());
    }

    private AlarmDTO doAlarmUnclear(Date now, Alarm alarm) {
        final AlarmSeverity previousSeverity = alarm.getSeverity();
        alarm.setSeverity(alarm.getLastEventSeverity());
        updateAutomationTime(alarm, now);
        alarmRepository.save(alarm);
        alarmEntityNotifier.didUpdateAlarmSeverity(alarm, previousSeverity);

        return alarmMapper.alarmToAlarmDTO(alarm);

    }

    @Override
    @Transactional
    public AlarmDTO escalateAlarm(Alarm alarm, Date now) {
            log.info("Escalating alarm with id: {} at: {}", alarm.getAlarmId(), now);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarm.getAlarmId());
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarm);
            return null;
        }
        return doEscalateAlarm(now, maybeAlarmInTrans);
    }

    @Override
    @Transactional
    public AlarmDTO escalateAlarm(Long alarmId, Date now) {
        log.info("Escalating alarm with id: {} at: {}", alarmId, now);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarmId);
            return null;
        }
        return doEscalateAlarm(now, maybeAlarmInTrans);
    }

    private AlarmDTO doEscalateAlarm(Date now, Optional<Alarm> maybeAlarmInTrans) {
        Alarm alarmInTrans = maybeAlarmInTrans.get();
        final AlarmSeverity previousSeverity = alarmInTrans.getSeverity();
        alarmInTrans.setSeverity(AlarmSeverity.escalate(previousSeverity));
        updateAutomationTime(alarmInTrans, now);
        alarmRepository.save(alarmInTrans);
        alarmEntityNotifier.didUpdateAlarmSeverity(alarmInTrans, previousSeverity);

        return alarmMapper.alarmToAlarmDTO(alarmInTrans);
    }

    @Override
    @Transactional
    public AlarmDTO acknowledgeAlarm(Alarm alarm, Date now, String userId) {
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarm.getAlarmId());
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarm);
            return null;
        }

        return doAcknowledgeAlarm(userId, maybeAlarmInTrans.get());
    }

    @Override
    @Transactional
    public AlarmDTO acknowledgeAlarm(Long alarmId, Date now, String userId) {
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarmId);
            return null;
        }

        return doAcknowledgeAlarm(userId, maybeAlarmInTrans.get());
    }

    private AlarmDTO doAcknowledgeAlarm(String userId, Alarm alarm) {
        alarm.setAlarmAckTime(new Date());
        alarm.setAlarmAckUser(userId);
        alarmRepository.save(alarm);

        return alarmMapper.alarmToAlarmDTO(alarm);
    }

    @Override
    @Transactional
    public AlarmDTO unAcknowledgeAlarm(Long alarmId, Date now) {
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarmId);
            return null;
        }

        return doUnacknowledgeAlarm(maybeAlarmInTrans.get());
    }

    @Override
    @Transactional
    public AlarmDTO unAcknowledgeAlarm(Alarm alarm, Date now) {
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarm.getAlarmId());
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarm);
            return null;
        }

        return doUnacknowledgeAlarm(maybeAlarmInTrans.get());
    }

    private AlarmDTO doUnacknowledgeAlarm(Alarm alarm) {
        alarm.setAlarmAckTime(null);
        alarm.setAlarmAckUser(null);
        alarmRepository.save(alarm);

        return alarmMapper.alarmToAlarmDTO(alarm);
    }

    @Override
    @Transactional
    public AlarmDTO setSeverity(Alarm alarm, AlarmSeverity severity, Date now) {
            log.info("Updating severity {} on alarm with id: {}", severity, alarm.getAlarmId());
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarm.getAlarmId());
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarm);
            return null;
        }
        return doSetSeverity(severity, now, maybeAlarmInTrans.get());
    }

    @Override
    @Transactional
    public AlarmDTO setSeverity(Long alarmId, AlarmSeverity severity, Date now) {
        log.info("Updating severity to {} on alarm with id: {}", severity, alarmId);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping clear.", alarmId);
            return null;
        }
        return doSetSeverity(severity, now, maybeAlarmInTrans.get());
    }

    private AlarmDTO doSetSeverity(AlarmSeverity severity, Date now, Alarm alarmInTrans) {
        final AlarmSeverity previousSeverity = alarmInTrans.getSeverity();
        alarmInTrans.setSeverity(severity);
        updateAutomationTime(alarmInTrans, now);
        alarmRepository.save(alarmInTrans);
        alarmEntityNotifier.didUpdateAlarmSeverity(alarmInTrans, previousSeverity);

        return alarmMapper.alarmToAlarmDTO(alarmInTrans);
    }

    @Override
    public List<AlarmDTO> getAllAlarms(String tenantId) {
        List<Alarm> alarms = alarmRepository.findAll();

        List<AlarmDTO> dtoAlarmList =
            alarms
                .stream()
                .map(alarm -> alarmMapper.alarmToAlarmDTO(alarm))
                .collect(Collectors.toList());

        return dtoAlarmList;
    }

    @Override
    @Transactional
    public AlarmDTO process(Event event) {
        Objects.requireNonNull(event, "Cannot create alarm from null event.");

        log.debug("Processing Event: {}; nodeid: {}; ipaddr: {}", event.getUei(), event.getNodeId(), event.getIpAddress());

        Alarm alarm  = addOrReduceEventAsAlarm(event);

        return alarmMapper.alarmToAlarmDTO(alarm);
    }

    @Override
    @Transactional
    public AlarmDTO removeStickyMemo(long alarmId) {
        log.info("Removing sticky memo on alarm with id: {}", alarmId);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping sticky memo removal.", alarmId);
            return null;
        }

        Alarm targetAlarm = maybeAlarmInTrans.get();

        if (targetAlarm.getStickyMemo() != null) {
            targetAlarm.setStickyMemo(null);
            alarmRepository.save(targetAlarm);
        }

        return alarmMapper.alarmToAlarmDTO(targetAlarm);
    }

    @Override
    public AlarmDTO updateStickyMemo(Long alarmId, String body) {
        log.info("Updating sticky memo on alarm with id: {}", alarmId);
        final Optional<Alarm> maybeAlarmInTrans = alarmRepository.findById(alarmId);
        if (maybeAlarmInTrans.isEmpty()) {
            log.warn("Alarm disappeared: {}. Skipping sticky memo removal.", alarmId);
            return null;
        }

        Alarm targetAlarm = maybeAlarmInTrans.get();

        if (targetAlarm.getStickyMemo() == null) {
            Memo memo = new Memo();
            memo.setBody(body);
            memo.setUpdated(new Date());
            memo.setCreated(new Date());
            targetAlarm.setStickyMemo(memo);
            alarmRepository.save(targetAlarm);
        }

        return alarmMapper.alarmToAlarmDTO(targetAlarm);
    }

    protected Alarm addOrReduceEventAsAlarm(Event event) throws IllegalStateException {

//        String reductionKey = String.format("%s:%d:%s", event.getUei(), event.getNodeId(), "TODO:Need tenant id");
        String reductionKey = event.getAlarmData().getReductionKey();
        String clearKey = event.getAlarmData().getClearKey();

        log.debug("Looking for existing clearKey: {}", clearKey);

        Alarm alarm = alarmRepository.findByReductionKey(clearKey);

        if (alarm == null) {
            log.debug("looking for existing reductionKey: {}", reductionKey);
            alarm = alarmRepository.findByReductionKey(reductionKey);
        }

        if (alarm == null ) {
            log.debug("reductionKey or clearKey not found, instantiating new alarm");

            alarm = createNewAlarm(event);

            alarmEntityNotifier.didCreateAlarm(alarm);
        } else {
            log.debug("reductionKey or clearKey found, reducing event to existing alarm: {}", alarm.getAlarmId());

            alarm.incrementCount();

            alarmEntityNotifier.didUpdateAlarmWithReducedEvent(alarm);
        }

        alarmRepository.save(alarm);

        return alarm;
    }

    private static void updateAutomationTime(Alarm alarm, Date now) {
        if (alarm.getFirstAutomationTime() == null) {
            alarm.setFirstAutomationTime(now);
        }
        alarm.setLastAutomationTime(now);
    }

    public void setAlarmEntityNotifier(AlarmEntityNotifier alarmEntityNotifier) {
        this.alarmEntityNotifier = alarmEntityNotifier;
    }

    private Alarm createNewAlarm(Event event) {
        Date now = new Date();
        Alarm alarm = new Alarm();

        alarm.setAlarmType(1);
        alarm.setClearKey(event.getAlarmData().getClearKey());
        alarm.setCounter(1);
        alarm.setLastEventTime(now);
        alarm.setLastAutomationTime(now);
        //TODO:Not sure this is a robust mapping. Maybe merge the two enums?
        alarm.setSeverity(AlarmSeverity.get(event.getEventSeverity().getNumber()));
        alarm.setLastEventSeverity(alarm.getSeverity());
        alarm.setReductionKey(event.getAlarmData().getReductionKey());
        alarm.setEventUei(event.getUei() + event.getNodeId());
        alarm.setX733ProbableCause(1);
        alarm.setDetails(new HashMap<>());
        alarm.setRelatedAlarms(new HashSet<>());

        // Situations are denoted by the existance of related-reductionKeys
//        alarm.setRelatedAlarms(getRelatedAlarms(event.getParmCollection()), event.getTime());

//        alarm.setDescription(e.getEventDescr());
//        alarm.setDistPoller(e.getDistPoller());
//        alarm.setFirstEventTime(e.getEventTime());
//        alarm.setIfIndex(e.getIfIndex());
//        alarm.setIpAddr(e.getIpAddr());

//        alarm.setLastEvent(e);
//        alarm.setLogMsg(e.getEventLogMsg());
//        alarm.setMouseOverText(e.getEventMouseOverText());
//        alarm.setNode(e.getNode());
//        alarm.setOperInstruct(e.getEventOperInstruct());

//        alarm.setServiceType(e.getServiceType());
//        alarm.setSuppressedUntil(e.getEventTime()); //UI requires this be set
//        alarm.setSuppressedTime(e.getEventTime()); // UI requires this be set

//        if (event.getAlarmData().getManagedObject() != null) {
//            alarm.setManagedObjectType(event.getAlarmData().getManagedObject().getType());
//        }
//        e.setAlarm(alarm);
        
        return alarm;
    }

}
