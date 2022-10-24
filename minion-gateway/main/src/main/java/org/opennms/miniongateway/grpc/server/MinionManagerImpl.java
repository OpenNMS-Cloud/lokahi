package org.opennms.miniongateway.grpc.server;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManagerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager that tracks known minions that are connected to this server.
 *
 *  1. For this location, which minions are connected locally
 *  2. Is Minion X connected locally, Is SystemId X connected locally
 *  3. Node singleton service will use this to route
 *  THIS bean is.... Spring Bean, NOT an Ignite service.
 *  Add node singleton as a listener to this
 *
 *  - Need a router (core entry point), top level guy, "send this to X (location or id)".
 *  -   - needs to ask "is this locally connected"
 */

public class MinionManagerImpl implements MinionManager {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private Map<String, MinionInfo> minionByIdMap = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<MinionManagerListener> listeners = new ConcurrentLinkedQueue<>();
    

    private long sequence = 0L;

    @Override
    public void addMinion(MinionInfo minionInfo) {
        log.info("Minion Manager: adding minion: id={}; location={}", minionInfo.getId(), minionInfo.getLocation());

        if (minionByIdMap.containsKey(minionInfo.getId())) {
            log.warn("Attempt to register minion with duplicate id; ignoring: id=" + minionInfo.getId() + "; location=" + minionInfo.getLocation());
            return;
        }

        minionByIdMap.put(minionInfo.getId(), minionInfo);

        listeners.forEach(listener -> listener.onMinionAdded(sequence++, minionInfo));
    }

    @Override
    public void removeMinion(String minionId) {
        log.info("Minion Manager: removing minion: id={}", minionId);

        MinionInfo removedMinionInfo;

        removedMinionInfo = minionByIdMap.remove(minionId);

        if (removedMinionInfo == null) {
            log.warn("Attempt to remove minion with unknown id; ignoring: id={}", minionId);
            return;
        }

        minionByIdMap.remove(minionId);

        listeners.forEach(listener -> listener.onMinionRemoved(sequence++, removedMinionInfo));
    }

    @Override
    public void addMinionListener(MinionManagerListener listener) {
        log.info("Adding minion manager listener at {}: class={}", System.identityHashCode(listener), listener.getClass().getName());

        listeners.add(listener);
    }

    @Override
    public void removeMinionListener(MinionManagerListener listener) {

        listeners.remove(listener);
    }

    /**
     * Returns a copy of the list of minions known by the manager.
     *
     * @return
     */
    @Override
    public List<MinionInfo> getMinions() {
        return new LinkedList<>(minionByIdMap.values());
    }
}
