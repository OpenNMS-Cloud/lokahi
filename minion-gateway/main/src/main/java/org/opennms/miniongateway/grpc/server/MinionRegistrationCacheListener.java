package org.opennms.miniongateway.grpc.server;

import static org.opennms.miniongateway.router.MinionLookupServiceImpl.MINIONS_BY_ID;
import static org.opennms.miniongateway.router.MinionLookupServiceImpl.MINIONS_BY_LOCATION;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.core.ipc.grpc.server.manager.MinionManagerListener;

@Deprecated
public class MinionRegistrationCacheListener implements MinionManagerListener {

    private Ignite ignite;

    IgniteCache<String, UUID> minionByIdCache;
    IgniteCache<String, Queue<UUID>> minionByLocationCache;

    public MinionRegistrationCacheListener(Ignite ignite) {
        this.ignite = ignite;

        minionByIdCache = ignite.cache(MINIONS_BY_ID);
        minionByLocationCache = ignite.cache(MINIONS_BY_LOCATION);
    }

    @Override
    public void onMinionAdded(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.put(minionInfo.getId(), localUUID);

        Queue<UUID> existingMinions = minionByLocationCache.get(minionInfo.getLocation());
        if (existingMinions.isEmpty()) {
            existingMinions = new ConcurrentLinkedDeque();
        }
        //TODO: for now, seems we can modify in place and not have to put this back in.
        existingMinions.add(localUUID);
    }

    @Override
    public void onMinionRemoved(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.remove(minionInfo.getId());
        minionByLocationCache.remove(minionInfo.getLocation());

        Queue<UUID> existingMinions = minionByLocationCache.get(minionInfo.getLocation());
        if (!existingMinions.isEmpty()) {
            existingMinions.remove(localUUID);
        }
    }
}
