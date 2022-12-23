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

package org.opennms.horizon.alarmservice.drools;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.opennms.horizon.alarmservice.api.AlarmLifecycleListener;
import org.opennms.horizon.alarmservice.api.AlarmService;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.opennms.horizon.alarmservice.db.entity.AlarmAssociation;
import org.opennms.horizon.alarmservice.db.repository.AlarmRepository;
import org.opennms.horizon.alarmservice.service.AlarmServiceImpl;
import org.opennms.horizon.alarmservice.utils.SystemProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class maintains the Drools context used to manage the lifecycle of the alarms.
 *
 * We drive the facts in the Drools context using callbacks provided by the {@link AlarmLifecycleListener}.
 *
 * Atomic actions are used to update facts in working memory.
 *
 * @author jwhite
 */
@Slf4j
@Component
@Setter
public class DroolsAlarmContext extends ManagedDroolsContext implements AlarmLifecycleListener {

    private static final RateLimitedLog RATE_LIMITED_LOGGER = RateLimitedLog
            .withRateLimit(log)
            .maxRate(5)
            .every(Duration.ofSeconds(30))
            .build();

    private static final long MAX_NUM_ACTIONS_IN_FLIGHT = SystemProperties.getLong(
            "org.opennms.netmgt.alarmd.drools.max_num_actions_in_flight", 5000);

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AlarmRepository alarmRepository;

    private final AlarmCallbackStateTracker stateTracker = new AlarmCallbackStateTracker();

    private final Map<Long, AlarmAndFact> alarmsById = new HashMap<>();

    private final Map<Long, Map<Long, AlarmAssociationAndFact>> alarmAssociationById = new HashMap<>();

    private final CountDownLatch seedSubmittedLatch = new CountDownLatch(1);

    private final AtomicLong atomicActionsInFlight = new AtomicLong(-1);
    private final AtomicLong numAlarmsFromLastSnapshot = new AtomicLong(-1);
    private final AtomicLong numSituationsFromLastSnapshot = new AtomicLong(-1);
    private final Meter atomicActionsDropped = new Meter();
    private final Meter atomicActionsQueued = new Meter();

    public DroolsAlarmContext() {
        this(getRulesResourceNames());
    }

    public DroolsAlarmContext(List<String> rulesResourceNames) {
        super(rulesResourceNames, AlarmServiceImpl.ALARM_RULES_NAME, "DroolsAlarmContext");
        setOnNewKiewSessionCallback(kieSession -> {
            kieSession.setGlobal("alarmService", alarmService);

            // Rebuild the fact handle maps
            alarmsById.clear();
            alarmAssociationById.clear();
            for (FactHandle fact : kieSession.getFactHandles()) {
                final Object objForFact = kieSession.getObject(fact);
                if (objForFact instanceof Alarm) {
                    final Alarm alarmInSession = (Alarm)objForFact;
                    alarmsById.put(alarmInSession.getAlarmId(), new AlarmAndFact(alarmInSession, fact));
                } else if (objForFact instanceof AlarmAssociation) {
                    final AlarmAssociation associationInSession = (AlarmAssociation)objForFact;
                    final Long situationId = associationInSession.getSituationAlarmId().getAlarmId();
                    final Long alarmId = associationInSession.getRelatedAlarmId().getAlarmId();
                    final Map<Long, AlarmAssociationAndFact> associationFacts = alarmAssociationById.computeIfAbsent(situationId, (sid) -> new HashMap<>());
                    associationFacts.put(alarmId, new AlarmAssociationAndFact(associationInSession, fact));
                }
            }

            // Reset metrics
            atomicActionsInFlight.set(0L);
            numAlarmsFromLastSnapshot.set(-1L);
            numSituationsFromLastSnapshot.set(-1L);
        });

        // Register metrics
        getMetrics().register("atomicActionsInFlight", (Gauge<Long>) atomicActionsInFlight::get);
        getMetrics().register("numAlarmsFromLastSnapshot", (Gauge<Long>) numAlarmsFromLastSnapshot::get);
        getMetrics().register("numSituationsFromLastSnapshot", (Gauge<Long>) numSituationsFromLastSnapshot::get);
        getMetrics().register("atomicActionsDropped", atomicActionsDropped);
        getMetrics().register("atomicActionsQueued", atomicActionsQueued);
    }

//    public static File getDefaultRulesFolder() {
//        // FIXME: OOPS: Ugly
//        try {
//            Path rulesFolder = Files.createTempDirectory("rules");
//            Bundle bundle = FrameworkUtil.getBundle(DroolsAlarmContext.class);
//            copy(bundle.getResource("rules/alarmd.drl"), rulesFolder.resolve("alarmd.drl"));
//            copy(bundle.getResource("rules/situations.drl"), rulesFolder.resolve("situations.drl"));
//            return rulesFolder.toFile();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static List<String> getRulesResourceNames() {
        return Arrays.asList("/rules/alarm.drl", "/rules/situations.drl");
    }

    public static void copy(URL url, final Path target) throws IOException {
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    @Transactional
    public void onStart() {
        final Thread seedThread = new Thread(() -> {
            // Seed the engine with the current set of alarms asynchronously
            // We do this async since we don't want to block the whole system from starting up
            // while we wait on the database (particularly for systems with large amounts of alarms)
            try {
                preHandleAlarmSnapshot();
                    log.info("Loading all alarms to seed Drools context.");
                    final List<Alarm> allAlarms = alarmRepository.findAll();
                    log.info("Done loading {} alarms.", allAlarms.size());
                    // Leverage the existing snapshot processing function to see the engine
                    handleAlarmSnapshot(allAlarms);
                    // Seed was submitted as an atomic action
                    seedSubmittedLatch.countDown();
            } finally {
                postHandleAlarmSnapshot();
            }
        });
        seedThread.setName("DroolAlarmContext-InitialSeed");
        seedThread.start();
    }

    @Override
    public void preHandleAlarmSnapshot() {
        // Start tracking alarm callbacks via the state tracker
        stateTracker.startTrackingAlarms();
    }

    /**
     * When running in the context of a transaction, execute the given action
     * when the transaction is complete and has been successfully committed.
     *
     * If the transaction does not complete successfully, log a warning and drop the action.
     *
     * If we're not currently in a transaction, execute the action immediately.
     *
     * @param atomicAction action to consider
     */
    private void executeAtomicallyWhenTransactionComplete(KieSession.AtomicAction atomicAction) {
        // FIXME: OOPS
        throw new RuntimeException("FIXME: OOPS");
//
//        if (TransactionSynchronizationManager.isSynchronizationActive()) {
//            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//                @Override
//                public void afterCompletion(int status) {
//                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
//                        RATE_LIMITED_LOGGER.warn("A database transaction did not complete successfully. " +
//                                "The alarms facts in the session may be out of sync until the next snapshot.");
//                        return;
//                    }
//                    submitOrRun(atomicAction);
//                }
//            });
//        } else {
//            submitOrRun(atomicAction);
//        }
    }

    private void submitOrRun(KieSession.AtomicAction atomicAction) {
        if (fireThreadId.get() == Thread.currentThread().getId()) {
            // This is the fire thread! Let's execute the action immediately instead of deferring it.
            atomicAction.execute(getKieSession());
        } else {
            // Submit the action for execution
            // Track the number of atomic actions waiting to be executed
            final long numActionsInFlight = atomicActionsInFlight.incrementAndGet();
            if (numActionsInFlight > MAX_NUM_ACTIONS_IN_FLIGHT) {
                RATE_LIMITED_LOGGER.error("Dropping action - number of actions in flight exceed {}! " +
                        "Alarms in Drools context will not match those in the database until the next successful sync.", MAX_NUM_ACTIONS_IN_FLIGHT);
                atomicActionsDropped.mark();
                atomicActionsInFlight.decrementAndGet();
                return;
            }
            getKieSession().submit(kieSession -> {
                atomicAction.execute(kieSession);
                atomicActionsInFlight.decrementAndGet();
            });
            atomicActionsQueued.mark();
        }
    }

    @Override
    public void handleAlarmSnapshot(List<Alarm> alarms) {
        if (!isStarted()) {
            log.debug("Ignoring alarm snapshot. Drools session is stopped.");
            return;
        }

        log.debug("Handling snapshot for {} alarms.", alarms.size());
        final Map<Long, Alarm> alarmsInDbById = alarms.stream()
                .filter(a -> a.getAlarmId() != null)
                .collect(Collectors.toMap(Alarm::getAlarmId, a -> a));

        // Eagerly initialize the alarms
        for (Alarm alarm : alarms) {
            eagerlyInitializeAlarm(alarm);
        }

        // Track some stats
        final long numSituations = alarms.stream().filter(Alarm::isSituation).count();
        numAlarmsFromLastSnapshot.set(alarms.size() - numSituations);
        numSituationsFromLastSnapshot.set(numSituations);

        submitOrRun(kieSession -> {
            final Set<Long> alarmIdsInDb = alarmsInDbById.keySet();
            final Set<Long> alarmIdsInWorkingMem = alarmsById.keySet();

            final Set<Long> alarmIdsToAdd = Sets.difference(alarmIdsInDb, alarmIdsInWorkingMem).stream()
                    // The snapshot contains an alarm which we don't have in working memory.
                    // It is possible that the alarm was in fact deleted some time after the
                    // snapshot was processed. We should only add it, if we did not explicitly
                    // delete the alarm after the snapshot was taken.
                    .filter(alarmId -> !stateTracker.wasAlarmWithIdDeleted(alarmId))
                    .collect(Collectors.toSet());
            final Set<Long> alarmIdsToRemove = Sets.difference(alarmIdsInWorkingMem, alarmIdsInDb).stream()
                    // We have an alarm in working memory that is not contained in the snapshot.
                    // Only remove it from memory if the fact we have dates before the snapshot.
                    .filter(alarmId -> !stateTracker.wasAlarmWithIdUpdated(alarmId))
                    .collect(Collectors.toSet());
            final Set<Long> alarmIdsToUpdate = Sets.intersection(alarmIdsInWorkingMem, alarmIdsInDb).stream()
                    // This stream contains the set of all alarms which are both in the snapshot
                    // and in working memory
                    .filter(alarmId -> {
                        final AlarmAndFact alarmAndFact = alarmsById.get(alarmId);
                        // Don't bother updating the alarm in memory if the fact we have is more recent than the snapshot
                        if (stateTracker.wasAlarmWithIdUpdated(alarmId)) {
                            return false;
                        }
                        final Alarm alarmInMem = alarmAndFact.getAlarm();
                        final Alarm alarmInDb = alarmsInDbById.get(alarmId);
                        // Only update the alarms if they are different
                        return shouldUpdateAlarmForSnapshot(alarmInMem, alarmInDb);
                    })
                    .collect(Collectors.toSet());

            // Log details that help explain what actions are being performed, if any
            if (log.isDebugEnabled()) {
                if (!alarmIdsToAdd.isEmpty() || !alarmIdsToRemove.isEmpty() || !alarmIdsToUpdate.isEmpty()) {
                    log.debug("Adding {} alarms, removing {} alarms and updating {} alarms for snapshot.",
                            alarmIdsToAdd.size(), alarmIdsToRemove.size(), alarmIdsToUpdate.size());
                } else {
                    log.debug("No actions to perform for alarm snapshot.");
                }
                // When TRACE is enabled, include diagnostic information to help explain why
                // the alarms are being updated
                if (log.isTraceEnabled()) {
                    for (Long alarmIdToUpdate : alarmIdsToUpdate) {
                        log.trace("Updating alarm with id={}. Alarm from DB: {} vs Alarm from memory: {}",
                                alarmIdToUpdate,
                                alarmsInDbById.get(alarmIdToUpdate),
                                alarmsById.get(alarmIdToUpdate));
                    }
                }
            }

            for (Long alarmIdToRemove : alarmIdsToRemove) {
                handleDeletedAlarmForAtomic(kieSession, alarmIdToRemove, alarmsById.get(alarmIdToRemove).getAlarm().getReductionKey());
            }

            final Set<Alarm> alarmsToUpdate = Sets.union(alarmIdsToAdd, alarmIdsToUpdate).stream()
                    .map(alarmsInDbById::get)
                    .collect(Collectors.toSet());
//            for (Alarm alarm : alarmsToUpdate) {
//                handleNewOrUpdatedAlarmForAtomic(kieSession, alarm, acksByRefId.get(alarm.getId()));
//            }

            stateTracker.resetStateAndStopTrackingAlarms();
            log.debug("Done handling snapshot.");
        });
    }

    @Override
    public void postHandleAlarmSnapshot() {
        // pass
    }

    /**
     * Used to determine if an alarm that is presently in the working memory should be updated
     * with the given alarm, when handling alarm snapshots.
     *
     * @param alarmInMem the alarm that is currently in the working memory
     * @param alarmInDb the alarm that is currently in the database
     * @return true if the alarm in the working memory should be updated, false otherwise
     */
    protected static boolean shouldUpdateAlarmForSnapshot(Alarm alarmInMem, Alarm alarmInDb) {
        return !Objects.equals(alarmInMem.getLastEventTime(), alarmInDb.getLastEventTime());
    }

    @Override
    public void handleNewOrUpdatedAlarm(Alarm alarm) {
        if (!isStarted()) {
            log.debug("Ignoring new/updated alarm. Drools session is stopped.");
            return;
        }
        eagerlyInitializeAlarm(alarm);
    }


    private void eagerlyInitializeAlarm(Alarm alarm) {
        // Initialize any related objects that are needed for rule execution
        Hibernate.initialize(alarm.getAssociatedAlarms());
//        if (alarm.getLastEvent() != null) {
//            // The last event may be null in unit tests
//            try {
//                Hibernate.initialize(alarm.getLastEvent().getEventParameters());
//            } catch (ObjectNotFoundException ex) {
//                // This may be triggered if the event attached to the alarm entity is already gone
//                alarm.setLastEvent(null);
//            }
//        }
    }

    private void handleNewOrUpdatedAlarmForAtomic(KieSession kieSession, Alarm alarm) {
        final AlarmAndFact alarmAndFact = alarmsById.get(alarm.getAlarmId());
        if (alarmAndFact == null) {
            log.debug("Inserting alarm into session: {}", alarm);
            final FactHandle fact = kieSession.insert(alarm);
            alarmsById.put(alarm.getAlarmId(), new AlarmAndFact(alarm, fact));
        } else {
            // Updating the fact doesn't always give us to expected results so we resort to deleting it
            // and adding it again instead
            log.trace("Deleting alarm from session (for re-insertion): {}", alarm);
            kieSession.delete(alarmAndFact.getFact());
            // Reinsert
            log.trace("Re-inserting alarm into session: {}", alarm);
            final FactHandle fact = kieSession.insert(alarm);
            alarmsById.put(alarm.getAlarmId(), new AlarmAndFact(alarm, fact));
        }

        if (alarm.isSituation()) {
            final Alarm situation = alarm;
            final Map<Long, AlarmAssociationAndFact> associationFacts = alarmAssociationById.computeIfAbsent(situation.getAlarmId(), (sid) -> new HashMap<>());
            for (AlarmAssociation association : situation.getAssociatedAlarms()) {
                Long alarmId = association.getRelatedAlarmId().getAlarmId();
                AlarmAssociationAndFact associationFact = associationFacts.get(alarmId);
                if (associationFact == null) {
                    log.debug("Inserting alarm association into session: {}", association);
                    final FactHandle fact = kieSession.insert(association);
                    associationFacts.put(alarmId, new AlarmAssociationAndFact(association, fact));
                } else {
                    FactHandle fact = associationFact.getFact();
                    log.trace("Updating alarm association in session: {}", associationFact);
                    kieSession.update(fact, association);
                    associationFacts.put(alarmId, new AlarmAssociationAndFact(association, fact));
                }
            }
            // Remove Fact for any Alarms no longer in the Situation
            Set<Long> deletedAlarmIds = associationFacts.values().stream()
                    .map(fact -> fact.getAlarmAssociation().getRelatedAlarmId().getAlarmId())
                    .filter(alarmId -> !situation.getRelatedAlarmIds().contains(alarmId))
                    .collect(Collectors.toSet());
            deletedAlarmIds.forEach(alarmId -> {
                final AlarmAssociationAndFact associationAndFact = associationFacts.remove(alarmId);
                if (associationAndFact != null) {
                    log.debug("Deleting AlarmAssociationAndFact from session: {}", associationAndFact.getAlarmAssociation());
                    kieSession.delete(associationAndFact.getFact());
                }
            });
        }
    }

    @Override
    public void handleDeletedAlarm(long alarmId, String reductionKey) {
        if (!isStarted()) {
            log.debug("Ignoring deleted alarm. Drools session is stopped.");
            return;
        }

        executeAtomicallyWhenTransactionComplete(kieSession -> {
            handleDeletedAlarmForAtomic(kieSession, alarmId, reductionKey);
            stateTracker.trackDeletedAlarm(alarmId, reductionKey);
        });
    }

    private void handleDeletedAlarmForAtomic(KieSession kieSession, long alarmId, String reductionKey) {
        final AlarmAndFact alarmAndFact = alarmsById.remove(alarmId);
        if (alarmAndFact != null) {
            log.debug("Deleting alarm from session: {}", alarmAndFact.getAlarm());
            kieSession.delete(alarmAndFact.getFact());
        }

        final Map<Long, AlarmAssociationAndFact> associationFacts = alarmAssociationById.remove(alarmId);
        if (associationFacts == null) {
            return;
        }
        for (Long association : associationFacts.keySet()) {
            AlarmAssociationAndFact associationFact = associationFacts.get(association);
            if (associationFact != null) {
                log.debug("Deleting association from session: {}", associationFact.getAlarmAssociation());
                kieSession.delete(associationFact.getFact());
            }
        }
    }

    @VisibleForTesting
    public void waitForInitialSeedToBeSubmitted() throws InterruptedException {
        seedSubmittedLatch.await();
    }
}
