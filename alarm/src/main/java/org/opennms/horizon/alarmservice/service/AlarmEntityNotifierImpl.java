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
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.horizon.alarmservice.api.AlarmEntityNotifier;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.opennms.horizon.alarmservice.db.entity.Memo;
import org.opennms.horizon.alarmservice.db.entity.ReductionKeyMemo;
import org.opennms.horizon.alarmservice.model.AlarmDTO;
import org.opennms.horizon.alarmservice.model.AlarmSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@PropertySource("classpath:application.yaml")
public class AlarmEntityNotifierImpl implements AlarmEntityNotifier {

    public static final String DEFAULT_ALARMS_TOPIC = "new-alarms";

    @Autowired
    @Qualifier("kafkaAlarmProducerTemplate")
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.new-alarms:" + DEFAULT_ALARMS_TOPIC + "}")
    private String kafkaTopic;

//    private Set<AlarmEntityListener> listeners = Sets.newConcurrentHashSet();

    @Override
    public void didCreateAlarm(Alarm alarm) {
        AlarmDTO alarmDTO =  AlarmMapper.INSTANCE.alarmToAlarmDTO(alarm);
        kafkaTemplate.send(new ProducerRecord<>(kafkaTopic, alarmDTO.toString().getBytes()));
    }

    @Override
    public void didUpdateAlarmWithReducedEvent(Alarm alarm) {
//        forEachListener(l -> l.onAlarmUpdatedWithReducedEvent(alarm));
    }

    @Override
    public void didAcknowledgeAlarm(Alarm alarm, String previousAckUser, Date previousAckTime) {
//        forEachListener(l -> l.onAlarmAcknowledged(alarm, previousAckUser, previousAckTime));
    }

    @Override
    public void didUnacknowledgeAlarm(Alarm alarm, String previousAckUser, Date previousAckTime) {
//        forEachListener(l -> l.onAlarmUnacknowledged(alarm, previousAckUser, previousAckTime));
    }

    @Override
    public void didUpdateAlarmSeverity(Alarm alarm, AlarmSeverity previousSeverity) {
//        forEachListener(l -> l.onAlarmSeverityUpdated(alarm, previousSeverity));
    }

    @Override
    public void didArchiveAlarm(Alarm alarm, String previousReductionKey) {
//        forEachListener(l -> l.onAlarmArchived(alarm, previousReductionKey));
    }

    @Override
    public void didDeleteAlarm(Alarm alarm) {
//        forEachListener(l -> l.onAlarmDeleted(alarm));
    }

    @Override
    public void didUpdateStickyMemo(Alarm alarm, String previousBody, String previousAuthor, Date previousUpdated) {
//        forEachListener(l -> l.onStickyMemoUpdated(alarm, previousBody, previousAuthor, previousUpdated));
    }

    @Override
    public void didUpdateReductionKeyMemo(Alarm alarm, String previousBody, String previousAuthor, Date previousUpdated) {
//        forEachListener(l -> l.onReductionKeyMemoUpdated(alarm, previousBody, previousAuthor, previousUpdated));
    }

    @Override
    public void didDeleteStickyMemo(Alarm alarm, Memo memo) {
//        forEachListener(l -> l.onStickyMemoDeleted(alarm, memo));
    }

    @Override
    public void didDeleteReductionKeyMemo(Alarm alarm, ReductionKeyMemo memo) {
//        forEachListener(l -> l.onReductionKeyMemoDeleted(alarm, memo));
    }

    @Override
    public void didUpdateLastAutomationTime(Alarm alarm, Date previousLastAutomationTime) {
//        forEachListener(l -> l.onLastAutomationTimeUpdated(alarm, previousLastAutomationTime));
    }

    @Override
    public void didUpdateRelatedAlarms(Alarm alarm, Set<Alarm> previousRelatedAlarms) {
//        forEachListener(l -> l.onRelatedAlarmsUpdated(alarm, previousRelatedAlarms));
    }

//    @Override
//    public void didChangeTicketStateForAlarm(Alarm alarm, TroubleTicketState previousState) {
//        forEachListener(l -> l.onTicketStateChanged(alarm, previousState));
//    }

//    private void forEachListener(Consumer<AlarmEntityListener> callback) {
//        for (AlarmEntityListener listener : listeners) {
//            try {
//                callback.accept(listener);
//            } catch (Exception e) {
//                log.error("Error occurred while invoking listener: {}. Skipping.", listener, e);
//            }
//        }
//    }

//    public void onListenerRegistered(final AlarmEntityListener listener, final Map<String,String> properties) {
//        log.debug("onListenerRegistered: {} with properties: {}", listener, properties);
//        listeners.add(listener);
//    }
//
//    public void onListenerUnregistered(final AlarmEntityListener listener, final Map<String,String> properties) {
//        log.debug("onListenerUnregistered: {} with properties: {}", listener, properties);
//        listeners.remove(listener);
//    }

}
