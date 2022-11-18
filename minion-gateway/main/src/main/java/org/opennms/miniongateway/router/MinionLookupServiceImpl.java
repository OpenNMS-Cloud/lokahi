package org.opennms.miniongateway.router;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionLookupServiceImpl implements MinionLookupService {

    public static final String MINIONS_BY_ID = "minionsById";
    public static final String MINIONS_BY_LOCATION = "minionsByLocation";

    private final Logger logger = LoggerFactory.getLogger(MinionLookupServiceImpl.class);

    private Ignite ignite;

    private IgniteCache<TenantKey, UUID> minionByIdCache;
    private IgniteCache<TenantKey, Queue<UUID>> minionByLocationCache;

    public MinionLookupServiceImpl(Ignite ignite) {
        logger.info("############ MINION ROUTER SERVICE INITIALIZED");

        this.ignite = ignite;

        // We need to be able to lock the caches when inserting new values, to insure that there is no race condition
        // with competing threads that may be trying to insert the same new location. So we will configure both caches
        // to be TRANSACTIONAL and be ready for locking.
        CacheConfiguration<TenantKey, UUID> minionByIdCacheConfig =
            new CacheConfiguration<TenantKey, UUID>().
                setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).
                setName(MINIONS_BY_ID);
        minionByIdCache = ignite.getOrCreateCache(minionByIdCacheConfig);

        CacheConfiguration<TenantKey, Queue<UUID>> minionByLocationCacheConfig =
            new CacheConfiguration<TenantKey, Queue<UUID>>().
                setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).
                setName(MINIONS_BY_LOCATION);
            minionByLocationCache = ignite.getOrCreateCache(minionByLocationCacheConfig);
    }

    @Override
    public UUID findGatewayNodeWithId(String tenantId, String id) {
        return minionByIdCache.get(new TenantKey(tenantId, id));
    }

    @Override
    public List<UUID> findGatewayNodeWithLocation(String tenantId, String location) {
        // TODO consider different structure to retain node identifiers to avoid wrapping into list
        // result must be indexed to support balancing of requests sent onto location (see Routing Task)
        Queue<UUID> uuids = minionByLocationCache.get(new TenantKey(tenantId, location));
        return uuids != null ? List.copyOf(uuids) : null;
    }

    @Override
    public void onMinionAdded(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.put(new TenantKey(minionInfo.getTenantId(), minionInfo.getId()), localUUID);

        Lock lock = minionByLocationCache.lock(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
        try {
            lock.lock();

            Queue<UUID> existingMinions = minionByLocationCache.get(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
            if (existingMinions == null) {
                existingMinions = new ConcurrentLinkedQueue<>();
                minionByLocationCache.put(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()), existingMinions);
            }
            //TODO: for now, seems we can modify in place and not have to put this back in.
            existingMinions.add(localUUID);
        }
        finally {
            lock.unlock();
        }



    }

    @Override
    public void onMinionRemoved(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.remove(new TenantKey(minionInfo.getTenantId(), minionInfo.getId()));

        Queue<UUID> existingMinions = minionByLocationCache.get(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
        if (existingMinions != null) {
            existingMinions.remove(localUUID);
            if (existingMinions.size() == 0)
            {
                minionByLocationCache.remove(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
            }
        }
    }

//========================================
//
//----------------------------------------

    // TESTABILITY: public for test use
    public static class TenantKey implements Serializable {
        private final String tenantId;
        private final String key;

        public TenantKey(String tenantId, String key) {
            this.tenantId = tenantId;
            this.key = key;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TenantKey tenantKey = (TenantKey) o;
            return Objects.equals(tenantId, tenantKey.tenantId) &&
                Objects.equals(key, tenantKey.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, key);
        }
    }
}
