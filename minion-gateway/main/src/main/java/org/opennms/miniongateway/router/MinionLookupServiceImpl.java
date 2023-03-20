package org.opennms.miniongateway.router;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionLookupServiceImpl implements MinionLookupService {

    public static final String MINIONS_BY_ID = "minionsById";
    public static final String MINIONS_BY_LOCATION = "minionsByLocation";

    private final Logger logger = LoggerFactory.getLogger(MinionLookupServiceImpl.class);

    private Ignite ignite;

    private IgniteCache<TenantKey, UUID> minionByIdCache;
    private IgniteCache<TenantKey, Set<UUID>> minionByLocationCache;

    public MinionLookupServiceImpl(Ignite ignite) {
        logger.info("############ MINION ROUTER SERVICE INITIALIZED");

        this.ignite = ignite;

        // We need to be able to lock the caches when inserting new values, to insure that there is no race condition
        // with competing threads that may be trying to insert the same new location. So we will configure both caches
        // to be TRANSACTIONAL and be ready for locking.
        minionByIdCache = ignite.cache(MINIONS_BY_ID);
        minionByLocationCache = ignite.cache(MINIONS_BY_LOCATION); //getOrCreateCache(minionByLocationCacheConfig);
    }

    @Override
    public UUID findGatewayNodeWithId(String tenantId, String id) {
        return minionByIdCache.get(new TenantKey(tenantId, id));
    }

    @Override
    public List<UUID> findGatewayNodeWithLocation(String tenantId, String location) {
        // TODO consider different structure to retain node identifiers to avoid wrapping into list
        // result must be indexed to support balancing of requests sent onto location (see Routing Task)
        Set<UUID> uuids = minionByLocationCache.get(new TenantKey(tenantId, location));
        return uuids != null ? List.copyOf(uuids) : null;
    }

    @Override
    public void onMinionAdded(long sequence, MinionInfo minionInfo) {
        UUID localUUID = ignite.cluster().localNode().id();
        final TenantKey systemKey = new TenantKey(minionInfo.getTenantId(), minionInfo.getId());
        final TenantKey locationKey = new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation());

        minionByIdCache.put(systemKey, localUUID);

        lockCacheAndRun(locationKey, () -> {
            Set<UUID> existingMinions = minionByLocationCache.get(locationKey);
            if (existingMinions == null) {
                existingMinions = new LinkedHashSet<>();
                existingMinions.add(localUUID);
                minionByLocationCache.put(locationKey, existingMinions);
            } else {
                // make sure we push back updated collection to keep it in sync, regardless of ignite configuration
                // while its extra operation it should stay light in terms of traffic given that number of minions
                // per locations shall usually stay low
                existingMinions.add(localUUID);
                minionByLocationCache.put(locationKey, existingMinions);
            }
        });
    }

    @Override
    public void onMinionRemoved(long sequence, MinionInfo minionInfo) {
        UUID localUUID = ignite.cluster().localNode().id();
        final TenantKey systemKey = new TenantKey(minionInfo.getTenantId(), minionInfo.getId());
        final TenantKey locationKey = new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation());

        minionByIdCache.remove(systemKey);

        lockCacheAndRun(locationKey, () -> {
            Set<UUID> existingMinions = minionByLocationCache.get(locationKey);
            if (existingMinions != null) {
                if (existingMinions.remove(localUUID)) {
                    // collection state was changed, verify if we need to update or remove empty collection from cache
                    if (existingMinions.size() == 0) {
                        minionByLocationCache.remove(locationKey);
                    } else {
                        minionByLocationCache.put(locationKey, existingMinions);
                    }
                }
            }
        });
    }

    private void lockCacheAndRun(TenantKey tenantKey, Runnable runnable) {
        Lock lock = minionByLocationCache.lock(tenantKey);
        try {
            lock.lock();
            runnable.run();
        } catch (Exception e) {
            logger.error("Failed to complete operation on minionByLocationCache");
        }
        finally {
            lock.unlock();
        }
    }
}
