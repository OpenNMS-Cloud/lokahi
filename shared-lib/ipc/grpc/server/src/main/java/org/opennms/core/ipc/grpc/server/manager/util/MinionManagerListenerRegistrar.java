package org.opennms.core.ipc.grpc.server.manager.util;

import org.opennms.core.ipc.grpc.server.manager.MinionManager;
import org.opennms.core.ipc.grpc.server.manager.MinionManagerListener;

public class MinionManagerListenerRegistrar {
    private MinionManager minionManager;
    private MinionManagerListener listener;

    public MinionManager getMinionManager() {
        return minionManager;
    }

    public void setMinionManager(MinionManager minionManager) {
        this.minionManager = minionManager;
    }

    public MinionManagerListener getListener() {
        return listener;
    }

    public void setListener(MinionManagerListener listener) {
        this.listener = listener;
    }

    public void init() {
        minionManager.addMinionListener(listener);
    }

    public void shutdown() {
        minionManager.removeMinionListener(listener);
    }
}
