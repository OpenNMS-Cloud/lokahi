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

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.stub.StreamObserver;
import org.opennms.core.ipc.grpc.common.RpcRequestProto;
import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.core.ipc.grpc.server.manager.RpcConnectionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class RpcConnectionTrackerImpl implements RpcConnectionTracker {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcConnectionTrackerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final Object lock = new Object();

    private Map<StreamObserver<RpcRequestProto>, String> locationByConnection = new IdentityHashMap<>();
    private Map<StreamObserver<RpcRequestProto>, String> minionIdByConnection = new IdentityHashMap<>();

    private Map<String, StreamObserver<RpcRequestProto>> connectionByMinionId = new HashMap<>();
    private Multimap<String, StreamObserver<RpcRequestProto>> connectionListByLocation = LinkedListMultimap.create();
    private Map<String, Iterator<StreamObserver<RpcRequestProto>>> rpcHandlerIteratorMap = new HashMap<>();

    /**
     * Semaphore per connection that is used to ensure thread-safe sending to each connection.
     */
    private Map<StreamObserver<RpcRequestProto>, Semaphore> sempahoreByConnection = new IdentityHashMap<>();

    @Override
    public boolean addConnection(String location, String minionId, StreamObserver<RpcRequestProto> connection) {
        boolean added = false;
        synchronized (lock) {
            // Prevent duplicate registration
            if (! connectionListByLocation.containsEntry(location, connection)) {
                log.debug("Registering connection: location={}; minionId={}", location, minionId);

                removePossibleExistingMinionConnectionLocked(minionId);

                connectionByMinionId.put(minionId, connection);
                connectionListByLocation.put(location, connection);

                locationByConnection.put(connection, location);
                minionIdByConnection.put(connection, minionId);

                sempahoreByConnection.put(connection, new Semaphore(1, true));

                updateIteratorLocked(location);

                added = true;
            } else {
                log.info("Ignoring duplicate registration of connection: location={}; minionId={}", location, minionId);
            }
        }

        return added;
    }

    @Override
    public StreamObserver<RpcRequestProto> lookupByMinionId(String minionId) {
        synchronized (lock) {
            return connectionByMinionId.get(minionId);
        }
    }

    @Override
    public StreamObserver<RpcRequestProto> lookupByLocationRoundRobin(String locationId) {
        synchronized (lock) {
            Iterator<StreamObserver<RpcRequestProto>> iterator = rpcHandlerIteratorMap.get(locationId);

            if (iterator == null) {
                return null;
            }

            return iterator.next();
        }
    }

    @Override
    public MinionInfo removeConnection(StreamObserver<RpcRequestProto> connection) {
        MinionInfo removedMinionInfo = new MinionInfo();

        synchronized (lock) {
            String minionId = minionIdByConnection.remove(connection);
            String locationId = locationByConnection.remove(connection);

            if (minionId != null) {
                log.debug("removing connection for minion: location={}, minionId={}", locationId, minionId);

                connectionByMinionId.remove(minionId);
                removedMinionInfo.setId(minionId);
            }

            if (locationId != null) {
                log.debug("removing connection for location: location={}, minionId={}", locationId, minionId);

                connectionListByLocation.remove(locationId, connection);
                updateIteratorLocked(locationId);

                removedMinionInfo.setLocation(locationId);
            }

            sempahoreByConnection.remove(connection);
        }

        return removedMinionInfo;
    }

    @Override
    public Semaphore getConnectionSemaphore(StreamObserver<RpcRequestProto> connection) {
        synchronized (lock) {
            return sempahoreByConnection.get(connection);
        }
    }

    @Override
    public void clear() {
        log.info("Clearing all connections");

        synchronized (lock) {
            connectionByMinionId.clear();
            connectionListByLocation.clear();
            locationByConnection.clear();
            minionIdByConnection.clear();
            sempahoreByConnection.clear();
        }
    }

//========================================
// Internals
//----------------------------------------

    private void removePossibleExistingMinionConnectionLocked(String minionId) {
        StreamObserver<RpcRequestProto> obsoleteObserver = connectionByMinionId.get(minionId);

        if (obsoleteObserver != null) {
            log.info("replacing existing connection for minion: minion-id={}", minionId);
            connectionListByLocation.values().remove(obsoleteObserver);
        }
    }

    private void updateIteratorLocked(String location) {
        Collection<StreamObserver<RpcRequestProto>> streamObservers = connectionListByLocation.get(location);
        Iterator<StreamObserver<RpcRequestProto>> iterator = Iterables.cycle(streamObservers).iterator();

        rpcHandlerIteratorMap.put(location, iterator);
    }
}
