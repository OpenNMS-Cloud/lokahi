/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
 */

package org.opennms.core.ipc.grpc.server.manager.impl;

import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.core.ipc.grpc.server.manager.MinionManager;
import org.opennms.core.ipc.grpc.server.manager.MinionManagerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manager that tracks known minions that are connected to this server.
 */
public class MinionManagerImpl implements MinionManager {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private Map<String, MinionInfo> minionByIdMap = new HashMap<>();
    private List<MinionManagerListener> listeners = new LinkedList<>();

    private final Object lock = new Object();
    private long sequence = 0L;

    @Override
    public void addMinion(MinionInfo minionInfo) {
        log.info("Minion Manager: adding minion: id={}; location={}", minionInfo.getId(), minionInfo.getLocation());

        long opSeq;

        synchronized (lock) {
            if (minionByIdMap.containsKey(minionInfo.getId())) {
                log.warn("Attempt to register minion with duplicate id; ignoring: id=" + minionInfo.getId() + "; location=" + minionInfo.getLocation());
                return;
            }

            minionByIdMap.put(minionInfo.getId(), minionInfo);
            opSeq = sequence;
            sequence++;
        }

        foreachListener((listener) -> listener.onMinionAdded(opSeq, minionInfo));
    }

    @Override
    public void removeMinion(String minionId) {
        log.info("Minion Manager: removing minion: id={}", minionId);

        long opSeq;
        MinionInfo removedMinionInfo;

        synchronized (lock) {
            removedMinionInfo = minionByIdMap.remove(minionId);

            if (removedMinionInfo == null) {
                log.warn("Attempt to remove minion with unknown id; ignoring: id={}", minionId);
                return;
            }

            minionByIdMap.remove(minionId);

            opSeq = sequence;
            sequence++;
        }

        foreachListener((listener) -> listener.onMinionRemoved(opSeq, removedMinionInfo));
    }

    @Override
    public void addMinionListener(MinionManagerListener listener) {
        log.info("Adding minion manager listener at {}: class={}", System.identityHashCode(listener), listener.getClass().getName());
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeMinionListener(MinionManagerListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    /**
     * Returns a copy of the list of minions known by the manager.
     *
     * @return
     */
    @Override
    public List<MinionInfo> getMinions() {
        List<MinionInfo> result;
        synchronized (lock) {
            result = new LinkedList<>(minionByIdMap.values());
        }

        return result;
    }

//========================================
// Internals
//----------------------------------------

    private void foreachListener(Consumer<? super MinionManagerListener> action) {
        List<MinionManagerListener> listenerSnapshot;
        synchronized (lock) {
            listenerSnapshot = new LinkedList<>(listeners);
        }

        listenerSnapshot.forEach(action);
    }
}
