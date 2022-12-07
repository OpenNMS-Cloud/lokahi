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
import org.opennms.miniongateway.grpc.server.model.TenantKey;
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
    private Map<StreamObserver<RpcRequestProto>, TenantKey> locationByConnection = new IdentityHashMap<>();
    private Map<StreamObserver<RpcRequestProto>, TenantKey> minionIdByConnection = new IdentityHashMap<>();

    private Map<TenantKey, StreamObserver<RpcRequestProto>> connectionByMinionId = new HashMap<>();
    private Multimap<TenantKey, StreamObserver<RpcRequestProto>> connectionListByLocation = LinkedListMultimap.create();
    private Map<TenantKey, Iterator<StreamObserver<RpcRequestProto>>> rpcHandlerIteratorMap = new HashMap<>();

    /**
     * Semaphore per connection that is used to ensure thread-safe sending to each connection.
     */
    private Map<StreamObserver<RpcRequestProto>, Semaphore> semaphoreByConnection = new IdentityHashMap<>();

    @Override
    public boolean addConnection(String tenantId, String location, String minionId, StreamObserver<RpcRequestProto> connection) {
        boolean added = false;
        synchronized (lock) {
            TenantKey tenantMinionId = new TenantKey(tenantId, minionId);
            TenantKey tenantLocation = new TenantKey(tenantId, location);

            // Prevent duplicate registration
            if (! connectionListByLocation.containsEntry(tenantLocation, connection)) {
                log.debug("Registering connection: location={}; minionId={}", location, minionId);

                removePossibleExistingMinionConnectionLocked(minionId);

                connectionByMinionId.put(tenantMinionId, connection);
                connectionListByLocation.put(tenantLocation, connection);

                locationByConnection.put(connection, tenantLocation);
                minionIdByConnection.put(connection, tenantMinionId);

                semaphoreByConnection.put(connection, new Semaphore(1, true));

                updateIteratorLocked(tenantLocation);

                minionManager.addMinion(new MinionInfo(tenantId, minionId, location));

                added = true;
            } else {
                log.info("Ignoring duplicate registration of connection: location={}; minionId={}", location, minionId);
            }
        }

        return added;
    }

    @Override
    public StreamObserver<RpcRequestProto> lookupByMinionId(String tenantId, String minionId) {
        TenantKey tenantMinionId = new TenantKey(tenantId, minionId);

        synchronized (lock) {
            return connectionByMinionId.get(tenantMinionId);
        }
    }

    @Override
    public StreamObserver<RpcRequestProto> lookupByLocationRoundRobin(String tenantId, String locationId) {
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
            TenantKey tenantMinionId = minionIdByConnection.remove(connection);
            TenantKey tenantLocation = locationByConnection.remove(connection);

            if (tenantMinionId != null) {
                log.debug("removing connection for minion: location={}, minionId={}", tenantLocation, tenantMinionId);

                connectionByMinionId.remove(tenantMinionId);
                removedMinionInfo.setId(tenantMinionId.getKey());
            }

            if (tenantLocation != null) {
                log.debug("removing connection for location: location={}, minionId={}", tenantLocation, tenantMinionId);

                connectionListByLocation.remove(tenantLocation, connection);
                updateIteratorLocked(tenantLocation);

                removedMinionInfo.setLocation(tenantLocation.getKey());
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

    private void updateIteratorLocked(TenantKey tenantLocation) {
        Collection<StreamObserver<RpcRequestProto>> streamObservers = connectionListByLocation.get(tenantLocation);
        Iterator<StreamObserver<RpcRequestProto>> iterator = Iterables.cycle(streamObservers).iterator();

        rpcHandlerIteratorMap.put(tenantLocation, iterator);
    }
}
