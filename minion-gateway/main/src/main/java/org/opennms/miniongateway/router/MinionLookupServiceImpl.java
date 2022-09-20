package org.opennms.miniongateway.router;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.horizon.shared.ignite.remoteasync.MinionLookupService;
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

        minionByIdCache = ignite.getOrCreateCache(MINIONS_BY_ID);
        minionByLocationCache = ignite.getOrCreateCache(MINIONS_BY_LOCATION);
    }

    @Override
    public UUID findGatewayNodeWithId(String id) {
        return minionByIdCache.get(id);
    }

    @Override
    public Queue<UUID> findGatewayNodeWithLocation(String location) {
        return minionByLocationCache.get(location);
    }

    @Override
    public void onMinionAdded(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.put(minionInfo.getId(), localUUID);

        Queue<UUID> existingMinions = minionByLocationCache.get(minionInfo.getLocation());
        if (existingMinions == null) {
            existingMinions = new ConcurrentLinkedQueue<>();
            minionByLocationCache.put(minionInfo.getLocation(), existingMinions);
        }
        //TODO: for now, seems we can modify in place and not have to put this back in.
        existingMinions.add(localUUID);

        int x = 0;
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
