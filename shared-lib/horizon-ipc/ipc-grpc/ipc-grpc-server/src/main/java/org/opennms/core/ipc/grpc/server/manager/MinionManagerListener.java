package org.opennms.core.ipc.grpc.server.manager;

public interface MinionManagerListener {
    /**
     * Called when a new minion is registered.  This call is synchronous, so implementations need to be fast and non-blocking.
     *
     * @param sequence monotonic value indicating the order of operations on the minion manager
     * @param minionInfo details for the added minion
     */
    void onMinionAdded(long sequence, MinionInfo minionInfo);

    /**
     * Called when a minion is unregistered.  This call is synchronous, so implementations need to be fast and non-blocking.
     *
     * @param sequence monotonic value indicating the order of operations on the minion manager
     * @param minionInfo details for the removed minion
     */
    void onMinionRemoved(long sequence, MinionInfo minionInfo);
}
