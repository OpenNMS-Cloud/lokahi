package org.opennms.core.ipc.grpc.server.manager.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.core.ipc.grpc.server.manager.MinionManager;
import org.opennms.core.ipc.grpc.server.manager.MinionManagerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
