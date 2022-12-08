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

import com.google.common.collect.Sets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alarmservice.api.AlarmEntityListener;
import org.opennms.horizon.alarmservice.api.AlarmLifecycleListener;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.opennms.horizon.alarmservice.db.entity.Memo;
import org.opennms.horizon.alarmservice.db.entity.ReductionKeyMemo;
import org.opennms.horizon.alarmservice.db.repository.AlarmRepository;
import org.opennms.horizon.alarmservice.model.AlarmSeverity;
import org.opennms.horizon.alarmservice.utils.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class AlarmLifecycleListenerManager implements AlarmEntityListener {

    public static final String ALARM_SNAPSHOT_INTERVAL_MS_SYS_PROP = "org.opennms.alarms.snapshot.sync.ms";
    public static final long ALARM_SNAPSHOT_INTERVAL_MS = SystemProperties.getLong(ALARM_SNAPSHOT_INTERVAL_MS_SYS_PROP, TimeUnit.MINUTES.toMillis(2));

    private final Set<AlarmLifecycleListener> listeners = Sets.newConcurrentHashSet();
    private Timer timer;

    @Autowired
    private AlarmRepository alarmRepository;

    @PostConstruct
    public void start() {
        timer = new Timer("AlarmLifecycleListenerManager");
        // Use a fixed delay instead of a fixed interval so that snapshots are not constantly in progress
        // if they take a long time
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    doSnapshot();
                } catch (Exception e) {
                    log.error("Error while performing snapshot update.", e);
                }
            }
        }, 0, ALARM_SNAPSHOT_INTERVAL_MS);
    }

    @PreDestroy
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Transactional
    protected void doSnapshot() {
        if (listeners.size() < 1) {
            return;
        }

        final AtomicLong numAlarms = new AtomicLong(-1);
        final long systemMillisBeforeSnapshot = System.currentTimeMillis();
        final AtomicLong systemMillisAfterLoad = new AtomicLong(-1);
        try {
            forEachListener(AlarmLifecycleListener::preHandleAlarmSnapshot);
               // Load all of the alarms
               final List<Alarm> allAlarms = alarmRepository.findAll();
               numAlarms.set(allAlarms.size());
               // Save the timestamp after the load, so we can differentiate between how long it took
               // to load the alarms and how long it took to invoke the callbacks
               systemMillisAfterLoad.set(System.currentTimeMillis());
               forEachListener(l -> {
                   log.debug("Calling handleAlarmSnapshot on listener: {}", l);
                   l.handleAlarmSnapshot(allAlarms);
                   log.debug("Done calling listener.");
               });
        } finally {
            if (log.isDebugEnabled()) {
                final long now = System.currentTimeMillis();
                log.debug("Alarm snapshot for {} alarms completed. Spent {}ms loading the alarms. " +
                                "Snapshot processing took a total of of {}ms.",
                        numAlarms.get(),
                        systemMillisAfterLoad.get() - systemMillisBeforeSnapshot,
                        now - systemMillisBeforeSnapshot);
            }
            forEachListener(AlarmLifecycleListener::postHandleAlarmSnapshot);
        }
    }

    public void onNewOrUpdatedAlarm(Alarm alarm) {
        forEachListener(l -> l.handleNewOrUpdatedAlarm(alarm));
    }

    @Override
    public void onAlarmArchived(Alarm alarm, String previousReductionKey) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onAlarmDeleted(Alarm alarm) {
        forEachListener(l -> l.handleDeletedAlarm(alarm.getAlarmId(), alarm.getReductionKey()));
    }

    @Override
    public void onAlarmCreated(Alarm alarm) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onAlarmUpdatedWithReducedEvent(Alarm alarm) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onAlarmAcknowledged(Alarm alarm, String previousAckUser, Date previousAckTime) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onAlarmUnacknowledged(Alarm alarm, String previousAckUser, Date previousAckTime) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onAlarmSeverityUpdated(Alarm alarm, AlarmSeverity previousSeverity) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onStickyMemoUpdated(Alarm alarm, String previousBody, String previousAuthor, Date previousUpdated) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onReductionKeyMemoUpdated(Alarm alarm, String previousBody, String previousAuthor, Date previousUpdated) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onStickyMemoDeleted(Alarm alarm, Memo memo) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onReductionKeyMemoDeleted(Alarm alarm, ReductionKeyMemo memo) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onLastAutomationTimeUpdated(Alarm alarm, Date previousLastAutomationTime) {
        onNewOrUpdatedAlarm(alarm);
    }

    @Override
    public void onRelatedAlarmsUpdated(Alarm alarm, Set<Alarm> previousRelatedAlarms) {
        onNewOrUpdatedAlarm(alarm);
    }

//    @Override
//    public void onTicketStateChanged(Alarm alarm, TroubleTicketState previousState) {
//        onNewOrUpdatedAlarm(alarm);
//    }

    private void forEachListener(Consumer<AlarmLifecycleListener> callback) {
        for (AlarmLifecycleListener listener : listeners) {
            try {
                callback.accept(listener);
            } catch (Exception e) {
                log.error("Error occurred while invoking listener: {}. Skipping.", listener, e);
            }
        }
    }

    public void onListenerRegistered(final AlarmLifecycleListener listener, final Map<String,String> properties) {
        log.debug("onListenerRegistered: {} with properties: {}", listener, properties);
        if (listener!=null) { listeners.add(listener); }
    }

    public void setListener(final AlarmLifecycleListener listener) {
        if (listener!=null) { onListenerRegistered(listener, null); }
    }

    public void onListenerUnregistered(final AlarmLifecycleListener listener, final Map<String,String> properties) {
        log.debug("onListenerUnregistered: {} with properties: {}", listener, properties);
        if (listener!=null) { listeners.remove(listener); }
    }

}
