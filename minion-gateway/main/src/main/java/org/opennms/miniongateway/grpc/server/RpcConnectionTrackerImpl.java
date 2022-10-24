package org.opennms.miniongateway.grpc.server;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.stub.StreamObserver;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RpcConnectionTrackerImpl implements RpcConnectionTracker {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcConnectionTrackerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final Object lock = new Object();

    @Autowired
    private MinionManager minionManager;

    // Remove location tracking here, keep it in the minion manager
    private Map<StreamObserver<RpcRequestProto>, String> locationByConnection = new IdentityHashMap<>();
    private Map<StreamObserver<RpcRequestProto>, String> minionIdByConnection = new IdentityHashMap<>();

    private Map<String, StreamObserver<RpcRequestProto>> connectionByMinionId = new HashMap<>();
    private Multimap<String, StreamObserver<RpcRequestProto>> connectionListByLocation = LinkedListMultimap.create();
    private Map<String, Iterator<StreamObserver<RpcRequestProto>>> rpcHandlerIteratorMap = new HashMap<>();

    /**
     * Semaphore per connection that is used to ensure thread-safe sending to each connection.
     */
    private Map<StreamObserver<RpcRequestProto>, Semaphore> semaphoreByConnection = new IdentityHashMap<>();

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

                semaphoreByConnection.put(connection, new Semaphore(1, true));

                updateIteratorLocked(location);

                minionManager.addMinion(new MinionInfo(minionId, location));

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

            semaphoreByConnection.remove(connection);
        }

        return removedMinionInfo;
    }

    @Override
    public Semaphore getConnectionSemaphore(StreamObserver<RpcRequestProto> connection) {
        synchronized (lock) {
            return semaphoreByConnection.get(connection);
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
            semaphoreByConnection.clear();
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
