package org.opennms.miniongateway.router;

import java.util.List;
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

    private IgniteCache<String, UUID> minionByIdCache;
    private IgniteCache<String, Queue<UUID>> minionByLocationCache;

    public MinionLookupServiceImpl(Ignite ignite) {
        logger.info("############ MINION ROUTER SERVICE INITIALIZED");

        this.ignite = ignite;

        // We need to be able to lock the caches when inserting new values, to insure that there is no race condition
        // with competing threads that may be trying to insert the same new location. So we will configure both caches
        // to be TRANSACTIONAL and be ready for locking.
        CacheConfiguration<String, UUID> minionByIdCacheConfig = new CacheConfiguration<String, UUID>().
            setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).
            setName(MINIONS_BY_ID);
        minionByIdCache = ignite.getOrCreateCache(minionByIdCacheConfig);

        CacheConfiguration<String, Queue<UUID>> minionByLocationCacheConfig = new CacheConfiguration<String, Queue<UUID>>().
            setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).
            setName(MINIONS_BY_LOCATION);
        minionByLocationCache = ignite.getOrCreateCache(minionByLocationCacheConfig);
    }

    @Override
    public UUID findGatewayNodeWithId(String id) {
        return minionByIdCache.get(id);
    }

    @Override
    public List<UUID> findGatewayNodeWithLocation(String location) {
        // TODO consider different structure to retain node identifiers to avoid wrapping into list
        // result must be indexed to support balancing of requests sent onto location (see Routing Task)
        Queue<UUID> uuids = minionByLocationCache.get(location);
        return uuids != null ? List.copyOf(uuids) : null;
    }

    @Override
    public void onMinionAdded(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.put(minionInfo.getId(), localUUID);

        Lock lock = minionByLocationCache.lock(minionInfo.getLocation());
        try {
            lock.lock();

            Queue<UUID> existingMinions = minionByLocationCache.get(minionInfo.getLocation());
            if (existingMinions == null) {
                existingMinions = new ConcurrentLinkedQueue<>();
                minionByLocationCache.put(minionInfo.getLocation(), existingMinions);
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

        minionByIdCache.remove(minionInfo.getId());

        Queue<UUID> existingMinions = minionByLocationCache.get(minionInfo.getLocation());
        if (existingMinions != null) {
            existingMinions.remove(localUUID);
            if (existingMinions.size() == 0)
            {
                minionByLocationCache.remove(minionInfo.getLocation());
            }
        }
    }
}
